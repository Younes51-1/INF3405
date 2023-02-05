import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;

public class Serveur {
    static String ipAdress;
    static int port;
    static boolean closeCommunication = false;
    static boolean serverActive = false;
    static DataInputStream dataRecived;
    static DataOutputStream dataSend;
    static String command = "";
    static String response = "";
    static Scanner scanner = new Scanner(System.in);
    static ServerSocket serverSocket;
    static int clientNumber = 0;

    private static boolean ipValidator(String ip) throws Exception {

        String[] ipChunks = ip.split(Pattern.quote("."));
        if (ipChunks.length != 4) throw new Exception("IP address requires four bytes.");

        for (String chunk : ipChunks) {
            try {
                int ipChunk = Integer.parseInt(chunk);
                if (0 > ipChunk || ipChunk > 255) throw new Exception("The numbers must be between 0 and 255.");
            } catch (NumberFormatException e) {
                throw new NumberFormatException("The numbers must be integers.");
            }
        }
        ipAdress = ip;
        return true;
    }

    private static boolean portValidator(String portInput) throws Exception {
        try {
            port = Integer.parseInt(portInput);
            if (5050 < port || port < 5000) throw new Exception("The numbers must be between 5000 and 5050.");
        } catch (NumberFormatException e) {
            throw new NumberFormatException("The numbers must be integers.");
        }
        return true;
    }


    private static void userInput() {
        boolean ipValid = false;
        boolean portValid = false;
        while (!ipValid) {
            System.out.println("Please input the server's IP address: ");
            String ipAdresseUser = scanner.nextLine();
            try {
                ipValid = ipValidator(ipAdresseUser);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        while (!portValid) {
            System.out.println("Please input the server's Port: ");
            String portUser = scanner.nextLine();
            try {
                portValid = portValidator(portUser);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

    }

    public static void main(String[] args) throws Exception {
        userInput();
        scanner.close();

        System.out.format("\nIP:port -> %s:%s\n", ipAdress, port);
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);

            serverSocket.bind(new InetSocketAddress(ipAdress, port));

            System.out.format("\nServer created -> %s:%d%n\n", ipAdress, port);

            while (true) new ClientHandler(serverSocket.accept(), ++clientNumber).start();
        } catch (Exception e) {
            System.out.println("\nThere was a problem when trying to create the server.\n");
        }
        serverSocket.close();
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private int clientNumber;
        static String[] commands;
        private static Path currentPath = Paths.get("").toAbsolutePath();

        public ClientHandler(Socket socket, int clientNumber) {
            this.socket = socket;
            this.clientNumber = clientNumber;
            System.out.println("\nConnection established with client. #" + clientNumber + " (" + socket + ")\n");
        }


        private static void cdHandler() throws Exception {
            String[] parts = command.split(" ", 2);
            String directoryStr = parts[1];

            Path newFolder = currentPath;

            if (directoryStr.equals("..")) {
                Path parentDirectory = currentPath.getParent();
                if (parentDirectory != null) newFolder = parentDirectory;

            } else if (directoryStr.equals(".")) {
                // Do nothing
            } else {
                Path directory = Paths.get(directoryStr);
                newFolder = currentPath.resolve(directory);
            }

            if (newFolder.toFile().isDirectory()) {
                currentPath = newFolder;
                dataSend.writeUTF(String.format("\nThe folder <%s> is not present within the directory.\n", newFolder.toAbsolutePath().toString()));
            } else {
                dataSend.writeUTF(String.format("\nThe folder <%s> is not present within the directory.\n", directoryStr));
            }
        }

        private static void lsHandler() throws Exception {
            File folder = new File(currentPath.toString());
            File[] files = folder.listFiles();
            for (File file : files) {
                if (file.isFile()) {
                    dataSend.writeUTF("[File] " + file.getName());
                } else if (file.isDirectory()) {
                    dataSend.writeUTF("[Folder] " + file.getName());
                }
            }
        }

        private static void mkdirHandler() throws Exception {
            String[] parts = command.split(" ", 2);
            String directoryStr = parts[1];
            if (!directoryStr.equals("..") && !directoryStr.equals(".")) {
                Path directory = Paths.get(directoryStr);
                Path combinedPath = currentPath.resolve(directory);

                File file = combinedPath.toFile();
                if (file.isDirectory()) {
                    dataSend.writeUTF(String.format("\n<%s> already exists.\n", directoryStr));
                } else {
                    file.mkdirs();
                    dataSend.writeUTF(String.format("\n<%s> was successfully created.\n", directoryStr));
                }
            }
        }

        private static void uploadHandler() throws Exception {
            String fileName = dataRecived.readUTF();
            System.out.println(String.format("\nReceiving file named : <%s>\n", fileName));

            int fileSize = dataRecived.readInt();

            Path filePath = currentPath.resolve(Paths.get(fileName));
            FileOutputStream fos = new FileOutputStream(filePath.toFile());

            byte[] buffer = new byte[8192];
            int bytesToRead = fileSize;
            while (bytesToRead > 0) {
                int min = Math.min(bytesToRead, buffer.length);
                int read = dataRecived.read(buffer, 0, min);
                fos.write(buffer, 0, read);
                bytesToRead -= read;
            }

            fos.flush();
            fos.close();

            dataSend.writeUTF(String.format("\n<%s> was successfully uploaded.\n", fileName));
        }

        private static void downloadHandler() throws Exception {
            String fileName = dataRecived.readUTF();
            Path relativeFilePath = Paths.get(fileName);
            Path absoluteFilePath = currentPath.resolve(relativeFilePath);
            if (Files.exists(absoluteFilePath) == false) {
                System.out.println("The file with name " + fileName + " does not exist.");
                return;
            }

            System.out.println("Sending file named : " + fileName);

            byte[] data = Files.readAllBytes(absoluteFilePath);

            dataSend.writeInt(data.length);

            dataSend.write(data);
            dataSend.flush();

            dataSend.writeUTF("Le fichier " + fileName + " a bien été téléchargé.");
        }

        public void run() {
            try {
                final String ACTIVE_SERVER = "Active";
                final String SERVER_OUTPUT_FORMAT = "yyyy-MM-dd@HH:mm:ss";
                dataSend = new DataOutputStream(socket.getOutputStream());
                dataRecived = new DataInputStream(socket.getInputStream());

                dataSend.writeUTF("Welcome to our server! We are glad to have you here, client #" + clientNumber + " !\n" + "\n\nPlease choose a commande from the list below:\n\n" + "- cd <Folder name>\n" + "- ls\n" + "- mkdir <Name of the new Folder>\n" + "- upload <File name>\n" + "- download <File name>\n" + "- exit\n");

                dataRecived = new DataInputStream(socket.getInputStream());

                while (true) {
                    dataSend.writeUTF("\nYour command: ");
                    dataSend.flush();

                    dataSend.writeUTF(ACTIVE_SERVER);

                    command = dataRecived.readUTF();

                    System.out.format("\n\n[%s - %s] : %s", socket.getRemoteSocketAddress().toString().substring(1), new SimpleDateFormat(SERVER_OUTPUT_FORMAT).format(Calendar.getInstance().getTime()), command);
                    commands = command.split(" ");
                    if (commands.length > 0 && commands.length < 2) {
                        switch (commands[0]) {
                            case "cd":
                                try {
                                    cdHandler();
                                } catch (Exception e) {
                                    System.out.println(e.getMessage());
                                }
                                break;
                            case "ls":
                                try {
                                    lsHandler();
                                } catch (Exception e) {
                                    System.out.println(e.getMessage());
                                }
                                break;
                            case "mkdir":
                                try {
                                    mkdirHandler();
                                } catch (Exception e) {
                                    System.out.println(e.getMessage());
                                }
                                break;
                            case "upload":
                                try {
                                    uploadHandler();
                                } catch (Exception e) {
                                    System.out.println(e.getMessage());
                                }
                                break;
                            case "download":

                                try {
                                    downloadHandler();
                                } catch (Exception e) {
                                    System.out.println(e.getMessage());
                                }
                                break;

                            default:
                                dataSend.writeUTF("\nUnkown command");
                        }
                    } else dataSend.writeUTF("\nUnkown command");
                }
            } catch (Exception e) {
                System.out.println("\nERROR! Unable to establish connection with the client #" + clientNumber + ". (" + e + ")");
            }
        }
    }
}