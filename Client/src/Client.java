import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Client {
    static String ipAdress;
    static int port;
    private static Socket socket;
    static boolean closeCommunication = false;
    static boolean serverActive = false;
    static DataInputStream dataRecived;
    static DataOutputStream dataSend;
    static String command = "";
    static String response = "";
    static Scanner scanner = new Scanner(System.in);

    private static boolean ipValidator(String ip) throws Exception {

        String[] ipChunks = ip.split(Pattern.quote("."));
        if (ipChunks.length != 4)
            throw new Exception("IP address requires four bytes.");

        for (String chunk : ipChunks) {
            try {
                int ipChunk = Integer.parseInt(chunk);
                if (0 > ipChunk || ipChunk > 255)
                    throw new Exception("The numbers must be between 0 and 255.");
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
            if (5050 < port || port < 5000)
                throw new Exception("The numbers must be between 5000 and 5050.");
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

    private static void socketConnexion() {
        try {
            socket = new Socket(ipAdress, port);
            System.out.println(socket.getRemoteSocketAddress().toString());
            dataRecived = new DataInputStream(socket.getInputStream());
            dataSend = new DataOutputStream(socket.getOutputStream());
        } catch (java.net.ConnectException e) {
            System.out.println("The connection to the server could not be established.");
            scanner.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            scanner.close();
        }
        return;
    }

    private static void execution() throws Exception {
        final String cd = "cd.*";
        final String ls = "ls";
        final String exit = "exit";
        final String mkdir = "mkdir.*";
        final String upload = "upload.*";
        final String download = "download.*";

        command = scanner.nextLine();

        if (command.matches(cd))
            dataSend.writeUTF(command);
        else if (command.matches(ls))
            dataSend.writeUTF(command);
        else if (command.matches(exit))
            closeCommunication = true;
        else if (command.matches(mkdir))
            dataSend.writeUTF(command);
        else if (command.matches(upload)) {
            final File file = new File(System.getProperty("user.dir") + File.separator + command.split(" ")[1]);
            if (file.exists() && file.isFile() && file.canRead()) {
                try {
                    uploadToServer(file);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            } else {
                System.out.println("Error while handling the file");
            }
        } else if (command.matches(download)) {
            try {
                downloadFromServer();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void uploadToServer(File file) throws Exception {
  
		byte[] data = Files.readAllBytes(file.toPath());
		
		dataSend.writeUTF("upload");
		
		dataSend.writeUTF(file.getName());
		
		dataSend.writeInt(data.length);
		
		dataSend.write(data);
		dataSend.flush();
    }

    private static void downloadFromServer() throws Exception {
        final String FileName = dataRecived.readUTF();
        final int FileSize = Integer.parseInt(dataRecived.readUTF());
        byte[] bytesRecived = new byte[FileSize];
        int bytesRead = 0;

        while (bytesRead < FileSize) {
            bytesRead += dataRecived.read(bytesRecived, bytesRead, FileSize - bytesRead);
        }

        FileOutputStream file = new FileOutputStream(FileName);
        file.write(bytesRecived);
        file.close();
    }

    public static void main(String[] args) {
        userInput();
        System.out.format("\nIP:port -> %s:%s\n", ipAdress, port);
        socketConnexion();
        try {
            while (!closeCommunication) {
                String serverResponse = dataRecived.readUTF();
                if (serverResponse.equals("Active"))
                    execution();
                else {
                    System.out.println(serverResponse);
                }


            }
            System.out.println("\nYou have been successfully disconnected.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        scanner.close();
    }
}
