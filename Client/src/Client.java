import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Client {
	static String ipAdresse;
	static int port;
	private static Socket socket;
	static DataInputStream 	donneesRecues;	
	static DataOutputStream 	donneesEnvoyees;
	static String 				commande 		= "";
	static String				reponse 		= "";
	static Scanner scanner = new Scanner(System.in);
	
	private static boolean ipValidate(String ip) throws Exception {

		String[] sousIp = ip.split(Pattern.quote("."));
		if (sousIp.length != 4) 
			throw new Exception(" 4 morceaux");
		
		for (String s : sousIp) {
			try {
				int ipChunk = Integer.parseInt(s);
				if (0 < ipChunk || ipChunk < 255)
					throw new Exception("Les nombres doivent etre 0 et 255");
			} catch (NumberFormatException e) {
				throw new NumberFormatException("des nombers entiers");
			}
		}
		return true;
	}

	private static boolean  portValidate(String portInput) throws Exception {
		try {
			port = Integer.parseInt(portInput);
			if (5050 < port || port < 5000)
				throw new Exception("Les nombres doivent etre 5000 et 5050");
		} catch (NumberFormatException e) {
			throw new NumberFormatException("des nombers entiers");
		}

		return true; 
	}





	private static void userInput() {
		boolean ipcorrect = false;
		boolean portcorrect = false;
		while(!ipcorrect){
			System.out.println("Veuillez entrez votre ipAdresse:");
			String ipAdresse = scanner.nextLine();
			try {
				ipcorrect = ipValidate(ipAdresse);
			}catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}

		while(!portcorrect){
			System.out.println("Veuillez entre votre port");
			String portUser = scanner.nextLine();
			try {
				portcorrect = portValidate(portUser);
			}catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		
	}			
	
	private static void socketConnexion(){
		try {
			socket = new Socket(ipAdresse, port);
			System.out.println(socket.getRemoteSocketAddress().toString());
			donneesRecues 	= new DataInputStream(socket.getInputStream());
			donneesEnvoyees = new DataOutputStream(socket.getOutputStream());
		}
		catch (java.net.ConnectException e) {
			System.out.println("La connexion au serveur n'a pas pu être établie. Arrêt.");
			scanner.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			scanner.close();
		}
		return;
	}
	
	private static void exection() throws Exception {
		final String cd = "cd";
		final String ls = "ls";
		final String exit = "exit";
		final String mkdir = "mkdir";
		final String upload = "upload";
		final String download = "download";
		
		reponse = donneesRecues.readUTF();
		
		switch(reponse) {
			case cd:
				// TODO
			case ls:
				//TODO
			case exit:
				//TODO
			case mkdir:
				//TODO
			case upload:
				final String 	FileName = donneesRecues.readUTF();
				final File file = new File(System.getProperty("user.dir") + File.separator + FileName);
				if (file.exists() && file.isFile() && file.canRead())
					sendServer(upload);
				break;
			case download:
				//TODO
		}
	}
	
	private static void sendServer(String command) {
		// TODO
	}
	
	public static void main(String[] args) {
		userInput();
		System.out.format("\nIP:port du serveur -> %s:%s", ipAdresse, port);
		System.out.println("\nEssayons d'établir la connexion. . . ");
		socketConnexion();
		try {
			exection();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		
		
		
		
		System.out.println("\nVous avez été déconnecté avec succès. Fin du programme.");
		scanner.close();
	}
}
