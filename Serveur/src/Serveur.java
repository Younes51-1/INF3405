/*
 *  l’adresse IP et le port du serveur doivent être entrés manuellement
 *  par l’utilisateur, Les données entrées doivent passer les tests exécutés
 *  dans ipValidator() et portValidator() avant de pouvoir établir une
 *  connexion avec le client.
 *  Pour chaque nouveau client, notre serveur crée un thread,
 *  de cette manière plusieurs clients peuvent se connecter
 *  et communiquer avec notre serveur.
 *  Chaque thread gère la réception des commandes venant du client et
 *  renvoie une réponse afin d’indiquer le statut d’exécution de la commande.
 *  À partir du client, le thread associe un gestionnaire d’exécution pour
 *  chaque commande sur le serveur.
 *  Le gestionnaire de la commande cd (cdHandler()) permet à l’utilisateur
 *  de se déplacer à travers la hiérarchie des répertoires.
 *  Nous avons créé un dossier Stockage pour accueillir les fichiers du client.
 *  Le client ne peut pas sortir de ce dossier pour des raisons de sécurité.
 *  Voici un bref résumé du rôle des autres gestionnaires :
 *  
·	lsHandler() : permet d’énumérer les fichiers et dossiers du répertoire courant du serveur
·	mkdirHandler() : permet de créer un répertoire sur le serveur
·	uploadHandler() : permet de téléverser un fichier du client vers le serveur
·	downloadHandler() : permet de télécharger un fichier du serveur vers le client
 * */

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
        if (ipChunks.length != 4) throw new Exception("L'adresse IP doit être sur 4 octets.");

        for (String chunk : ipChunks) {
            try {
                int ipChunk = Integer.parseInt(chunk);
                if (0 > ipChunk || ipChunk > 255) throw new Exception("Les nombres doivent être compris entre 0 et 255.");
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
            if (5050 < port || port < 5000) throw new Exception("Les nombres doivent être compris entre 5000 et 5050.");
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

    public static void main(String[] args) throws Exception {
        userInput();
        scanner.close();

        System.out.format("\nIP:port -> %s:%s\n", ipAdress, port);
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(ipAdress, port));
            System.out.format("\nServeur crée -> %s:%d%n\n", ipAdress, port);

            while (true) new ClientHandler(serverSocket.accept(), ++clientNumber).start();
        } catch (Exception e) {
            System.out.println("\nUn problème a été rencontré lors de la création du serveur.\n");
        }
        serverSocket.close();
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private int clientNumber;
        static String[] commands;
        private static Path currentPath;

        public ClientHandler(Socket socket, int clientNumber) {
            this.socket = socket;
            this.clientNumber = clientNumber;
            Path StockageFolder = Paths.get("Stockage").toAbsolutePath();
            if (!StockageFolder.toFile().isDirectory()) {
            	try {
                    Path directory = Paths.get("Stockage");
                    Path combinedPath = currentPath.resolve(directory);
                    File file = combinedPath.toFile();
                    file.mkdir();
     
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
            }
            currentPath = Paths.get("Stockage").toAbsolutePath();
            System.out.println("\nLa connexion a été établie avec le client. #" + clientNumber + " (" + socket + ")\n");
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
            	Path serverPath = Paths.get("").toAbsolutePath();        
            	if(newFolder.toString().equals(serverPath.toString())) {
                	dataSend.writeUTF(String.format("\nVous ne pouvez pas sortir de \\Stockage"));
                } else {
                	currentPath = newFolder;
                	dataSend.writeUTF(String.format("\nVous êtes dans %s\n", currentPath.toString().replace(serverPath.toString(), "")));
                }
            } else {
                dataSend.writeUTF(String.format("\nLe dossier <%s> n'est pas présent dans le répertoire.\n", directoryStr));
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
                    dataSend.writeUTF(String.format("\n<%s> existe déjà.\n", directoryStr));
                } else {
                    file.mkdirs();
                    dataSend.writeUTF(String.format("\n<%s> a été crée avec succès.\n", directoryStr));
                }
            }
        }

        private static void uploadHandler() throws Exception {
            String fileName = dataRecived.readUTF();
            if (fileName.equals("Fichier introuvable"))
            	return;
            
            System.out.println(String.format("\nReception du fichier : <%s>\n", fileName));
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
            dataSend.writeUTF(String.format("\n<%s> a été téléversé avec succès.\n", fileName));
        }

        private static void downloadHandler() throws Exception {
            String fileName = dataRecived.readUTF();
            Path relativeFilePath = Paths.get(fileName);
            Path absoluteFilePath = currentPath.resolve(relativeFilePath);

            if (!Files.exists(absoluteFilePath)) {
                dataSend.writeUTF("\nLe fichier " + fileName + " n'a pas été trouvé.\n");
                return;
            }

            dataSend.writeUTF("\nLe fichier " + fileName + " existe.\n");
            System.out.println("\nEnvoi du fichier : " + fileName);
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
                dataSend.writeUTF("Bienvenue sur notre serveur! Nous sommes heureux de vous avoir parmi nous, client #" + clientNumber + " !\n" + "\n\nVeuillez choisir une commande dans la liste ci-dessous:\n\n" + "- cd <Nom du répertoire>\n" + "- ls\n" + "- mkdir <Nom du nouveau dossier>\n" + "- upload <Nom du fichier>\n" + "- download <Nom du fichier>\n" + "- exit\n");
                dataRecived = new DataInputStream(socket.getInputStream());
                boolean openCommunication = true;

                while (openCommunication) {
                    dataSend.writeUTF("\nVotre commande: ");
                    dataSend.flush();
                    dataSend.writeUTF(ACTIVE_SERVER);
                    command = dataRecived.readUTF();
                    System.out.format("\n\n[%s - %s] : %s", socket.getRemoteSocketAddress().toString().substring(1), new SimpleDateFormat(SERVER_OUTPUT_FORMAT).format(Calendar.getInstance().getTime()), command);
                    commands = command.trim().split(" ");
                    
                    if (commands.length == 2 || commands.length == 1) {
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
                            case "exit":
                            	this.socket.close();
                            	openCommunication = false;
                            	System.out.format("\n\n Client n%s s'est déconnecté\n\n", this.clientNumber);
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
                                dataSend.writeUTF("\nCommande non répertoriée");
                        }
                    } else dataSend.writeUTF("\nCommande non répertoriée");
                }
            } catch (Exception e) {
                System.out.println("\nERREUR! Impossible d'établir la connexion avec le client #" + clientNumber + ". (" + e + ")");
            }
        }
    }
}
