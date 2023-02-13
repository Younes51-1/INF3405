import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            throw new Exception("L'adresse IP doit être sur 4 octets.");

        for (String chunk : ipChunks) {
            try {
                int ipChunk = Integer.parseInt(chunk);
                if (0 > ipChunk || ipChunk > 255)
                    throw new Exception("Les nombres doivent être compris entre 0 et 255.");
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Les nombres doivent être des entiers.");
            }
        }
        ipAdress = ip;
        return true;
    }

    private static boolean portValidator(String portInput) throws Exception {
        try {
            port = Integer.parseInt(portInput);
            if (5050 < port || port < 5000)
                throw new Exception("Les nombres doivent être compris entre 5000 et 5050.");
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Les nombres doivent être des entiers.");
        }
        return true;
    }


    private static void userInput() {
        boolean ipValid = false;
        boolean portValid = false;
        while (!ipValid) {
            System.out.println("Veuillez entrer l'adresse IP du serveur: ");
            String ipAdresseUser = scanner.nextLine();
            try {
                ipValid = ipValidator(ipAdresseUser);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        while (!portValid) {
            System.out.println("Veuillez entrer le port du serveur: ");
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
            System.out.println("La connexion avec le serveur n'a pas pu être établie.");
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
        else if (command.matches(exit)) {
        	dataSend.writeUTF(exit);
            closeCommunication = true;
        } else if (command.matches(mkdir))
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
            	dataSend.writeUTF("Fichier introuvable");
                System.out.println("Erreur lors du traitement du fichier ");
            }
        } else if (command.matches(download)) {
            try {
            	final String FileName = command.split(" ")[1];
                downloadFromServer(FileName);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        else {
        	 dataSend.writeUTF(command);
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

    private static void downloadFromServer(String fileName) throws Exception {
    	dataSend.writeUTF("download");
        dataSend.writeUTF(fileName);
        
        String fileExist = dataRecived.readUTF();
        if(!fileExist.contains("exist")){
        	System.out.println(fileExist);
        	return;
        }
       
        byte[] size = new byte[4];
        dataRecived.read(size);
        
        int fileSize = ByteBuffer.wrap(size).asIntBuffer().get();
        Path filePath = Paths.get(fileName);
        FileOutputStream file = new FileOutputStream(filePath.toFile());
        int maxFileRead = 8192;
        byte[] buffer = new byte[maxFileRead];
		int bytesToRead = fileSize;
		
		while (bytesToRead > 0) {
			int min = Math.min(bytesToRead, buffer.length);
			int read = dataRecived.read(buffer, 0, min);
			file.write(buffer, 0, read);
			bytesToRead -= read;
		}

		file.flush();
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
            System.out.println("\nVous avez été déconnecté.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        scanner.close();
    }
}
