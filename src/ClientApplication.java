import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import javax.crypto.Cipher;
import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class ClientApplication {
	private FileClient 		fileClient;
	private GroupClient		groupClient;
	private MenuEnum		menuLevel;
	private MTL_Strings		mtlCommand;
	private GTL_Strings		gtlCommand;
	private FTL_Strings		ftlCommand;
	private UserToken		userToken;
	private String 			userName;
	private BufferedReader	inputReader;
	private String			groupServerName;
	private String			fileServerName;
	private int				groupServerPort;
	private int				fileServerPort;
	private boolean			connectToFileServer;
	private String 			password;
	private PublicKey		fileServerPublicKey;
	private Cipher			gsRSADecryptCipher;
	private Cipher			fsRSADecryptCipher;
	
	/**
	 * @param args
	 * One Required argument should be supplied: username
	 * Two pairs of Optional arguments: [<Group Server Name> <Group Server Port> <Group Server Key File>] [<File Server Name> <File Server Port> <File Server Key File>]
	 * 		You must supply either the Group Server Name and Port or All Four in addition to the username
	 */
	public static void main(String[] args) {
		if (args.length == 1) {
			ClientApplication clientApp = new ClientApplication(args[0]);
			clientApp.runApplication();
		}
		else if (args.length == 4) {
			ClientApplication clientApp;
			try {
				clientApp = new ClientApplication(args[0], args[1], Integer.parseInt(args[2]), args[3]);
			} catch(NumberFormatException e) {
				e.printStackTrace();
				System.out.println("\n\nUsage:\n\tjava ClientApplication <username> [ [<Group Server Name> <Group Server Port> <Group Server Key File>] [<File Server Name> <File Server Port> <File Server Key File>] ]");
				return;
			}
			clientApp.runApplication();
		}
		else if (args.length == 7) {
			ClientApplication clientApp;
			try {
				clientApp = new ClientApplication(args[0], args[1], Integer.parseInt(args[2]), args[3], args[4], Integer.parseInt(args[5]), args[6]);
			} catch(NumberFormatException e) {
				e.printStackTrace();
				System.out.println("\n\nUsage:\n\tjava ClientApplication <username> [ [<Group Server Name> <Group Server Port> <Group Server Key File>] [<File Server Name> <File Server Port> <File Server Key File>] ]");
				return;
			}
			clientApp.runApplication();
			
		}
		else {
			System.out.println("Usage:\n\tjava ClientApplication <username> [ [<Group Server Name> <Group Server Port> <Group Server Key File>] [<File Server Name> <File Server Port> <File Server Key File>] ]");
		}
	}
	
	/**
	 * Enumeration of the different menu levels. Will be used for selecting the 
	 * correct menu to display later on in the program.
	 */
	private enum MenuEnum {
		MAIN_TOP_LEVEL, GROUP_TOP_LEVEL, FILE_TOP_LEVEL
	}
	
	/**
	 * Enumeration of the correct strings that the user should be inputting for the 
	 * MAIN_TOP_LEVEL menu
	 */
	private enum MTL_Strings {
		GROUP("group", "Manage Groups and Users"),
		FILE("file", "Manage Files"),
		EXIT("exit", "Exit the application");

		
		private String command;
		private String description;
		
		private MTL_Strings(String c, String d) {
			command = c;
			description = d;
		}
		
		public String getCommand() {
			return this.command;
		}
		
		public String getDescription() {
			return this.description;
		}
	}
	
	/**
	 * Enumeration of the correct strings that the user should be inputting for the 
	 * GROUP_TOP_LEVEL menu
	 */
	private enum GTL_Strings {
		CREATE_USER("create user", "Create a New User"),
		DELETE_USER("delete user", "Delete an Existing User"),
		CREATE_GROUP("create group", "Create a New Group"),
		DELETE_GROUP("delete group", "Delete and Existing Group"),
		ADD_USER_TO_GROUP("add member", "Add an Existing User to an Existing Group"),
		DELETE_USER_FROM_GROUP("remove member", "Remove a User from a Group"),
		LIST_MEMBERS("list members", "List the Users who are in a particular Group"),
		CHANGE_PASSWORD("change password", "Change your login Password"),
		SET_GROUP("set group", "Restrict file operations to only One of your Groups"),
		ALL_GROUPS("all groups", "Allow file operations in All your Groups"),
		BACK("back", "Return to the Main Menu"),
		EXIT("exit", "Exit the application");
		
		
		private String command;
		private String description;
		
		private GTL_Strings(String c, String d) {
			command = c;
			description = d;
		}
		
		public String getCommand() {
			return this.command;
		}
		
		public String getDescription() {
			return this.description;
		}
	}
	
	/**
	 * Enumeration of the correct strings that the user should be inputting for the 
	 * FILE_TOP_LEVEL menu
	 */
	private enum FTL_Strings {
		LIST_FILES("list files", "List all files that can be accessed by your groups"),
		UPLOAD_FILE("upload", "Share a local file with one of your groups"),
		DOWNLOAD_FILE("download", "Download a file belonging to one of your groups"),
		DELETE_FILE("delete", "Delete a file belonging to one of your groups"),
		CHANGE_SERVER("change server", "Connect to a different File Server"),
		BACK("back", "Return to Main Menu"),
		EXIT("exit", "Exit the application");
		
		private String command;
		private String description;
		
		private FTL_Strings(String c, String d) {
			command = c;
			description = d;
		}
		
		public String getCommand() {
			return this.command;
		}
		
		public String getDescription() {
			return this.description;
		}
	}
	
	/**
	 * Constructor for the ClientApplication class
	 */
	public ClientApplication(String uName) {
		userName	= uName;
		fileClient 	= new FileClient();
		groupClient	= new GroupClient();
		menuLevel	= MenuEnum.MAIN_TOP_LEVEL;
		inputReader	= new BufferedReader(new InputStreamReader(System.in));
		groupServerName	= null;
		fileServerName	= null;
		groupServerPort	= 8765;
		fileServerPort	= 4321;
		connectToFileServer = true;
	}
	
	public ClientApplication(String uName, String gServerName, int gServerPort, String gServerKeyFile) {
		userName		= uName;
		fileClient 		= new FileClient();
		groupClient		= new GroupClient();
		menuLevel		= MenuEnum.MAIN_TOP_LEVEL;
		inputReader		= new BufferedReader(new InputStreamReader(System.in));
		groupServerName	= gServerName;
		fileServerName	= null;
		groupServerPort	= gServerPort;
		fileServerPort	= 4321;
		connectToFileServer = true;
		
		try {
			// Add Bouncy Castle as the Security Provider if necessary
			if (Security.getProvider("BC") == null) {
				System.err.println("\n" + this + " adding Bouncy Castle as Security Provider\n");
				Security.addProvider(new BouncyCastleProvider());
			}
			
			FileInputStream stream = new FileInputStream(gServerKeyFile);
			byte[] key = new byte[stream.available()];
			stream.read(key);
			stream.close();
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
			KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
			PublicKey groupServerPublicKey = factory.generatePublic(keySpec);
			gsRSADecryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
			gsRSADecryptCipher.init(Cipher.DECRYPT_MODE, groupServerPublicKey);
		} catch(Exception e) {
			System.out.println("Creating the Group Server's RSA Decrypt Cipher failed\n");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public ClientApplication(String uName, String gServerName, int gServerPort, String gServerKeyFile, String fServerName, int fServerPort, String fServerKeyFile) {
		userName		= uName;
		fileClient 		= new FileClient();
		groupClient		= new GroupClient();
		menuLevel		= MenuEnum.MAIN_TOP_LEVEL;
		inputReader		= new BufferedReader(new InputStreamReader(System.in));
		groupServerName	= gServerName;
		fileServerName	= fServerName;
		groupServerPort	= gServerPort;
		fileServerPort	= fServerPort;
		connectToFileServer = true;
		
		try {
			// Add Bouncy Castle as the Security Provider if necessary
			if (Security.getProvider("BC") == null) {
				System.err.println("\n" + this + " adding Bouncy Castle as Security Provider\n");
				Security.addProvider(new BouncyCastleProvider());
			}
						
			FileInputStream stream = new FileInputStream(gServerKeyFile);
			byte[] key = new byte[stream.available()];
			stream.read(key);
			stream.close();
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
			KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
			PublicKey groupServerPublicKey = factory.generatePublic(keySpec);
			gsRSADecryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
			gsRSADecryptCipher.init(Cipher.DECRYPT_MODE, groupServerPublicKey);
			
			stream = new FileInputStream(fServerKeyFile);
			key = new byte[stream.available()];
			stream.read(key);
			stream.close();
			keySpec = new X509EncodedKeySpec(key);
			factory = KeyFactory.getInstance("RSA", "BC");
			fileServerPublicKey = factory.generatePublic(keySpec);
			fsRSADecryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
			fsRSADecryptCipher.init(Cipher.DECRYPT_MODE, fileServerPublicKey);
		} catch(Exception e) {
			System.out.println("Creating the either Group Server's or the File Server's RSA Decrypt Cipher failed\n");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Contains the main loop for receiving user input.
	 */
	private void runApplication() {
		boolean keepGoing = true;
		boolean validUser = false;
		
		// Get the user's password
		System.out.println("Please enter your password: ");
		password = getUserString();
		
		// Get the group server name and port if the user did not enter it as a command line argument
		if (groupServerName == null) getServerInformation(true);
		
		// Get the file server name and port if the user did not enter it as a command line argument
		// and they want to connect to a file server
		if (fileServerName == null) {
			String temp = null;
			System.out.println("Do you want to connect to a File Server? [yes | no]: ");
			temp = getUserString();
			
			// Get the file server information only if they say yes
			if (temp.equalsIgnoreCase("yes")) {
				
				getServerInformation(false);

				fileClient.connect(fileServerName, fileServerPort, fsRSADecryptCipher);
				
				if (!fileClient.isConnected()) {
					System.out.println("Could not connect to the specified File Server\n\n");
					return;
				}
				
				connectToFileServer = true;
			}
			else connectToFileServer = false;
		}
		
		
		// Make sure it is a valid user
		
		
		// This loop will only exit if the user gives the command for the 
		// application to exit.
		do {
			validUser = validateUser(userName, password);
			if (!validUser) {
				break;
			}
			keepGoing = executeCommands();
		}while(keepGoing);
		
		groupClient.disconnect();
		fileClient.disconnect();
	}
	
	/**
	 * gets the server name and port from the user
	 * @param serverType : true for group server, false for file server
	 */
	private void getServerInformation(boolean serverType) {
		String server;
		String name;
		String port;
		String keyFileName;
		
		if (serverType) server = "Group";
		else server = "File";
		
		System.out.println("Enter the name of the " + server + " Server (or press enter for localhost):");
		name = getUserString();
		System.out.println("Enter the port to connect to the " + server + " Server (or press enter for the default port)");
		port = getUserString();
		System.out.println("Enter the name of the public key file for the " + server + " Server");
		keyFileName = getUserString();
		
		// Use local host, which is already set as the default
		if (name == null || name.equals("")){}
		// Otherwise assign the name to the right server
		else {
			if (serverType) groupServerName = new String(name);
			else fileServerName = new String(name);
		}
		
		// Use the default port, which is already set as the default
		if (port == null || port.equals("")) {}
		// Otherwise assign the port to the right server
		else {
			if (serverType) groupServerPort = Integer.parseInt(port);
			else fileServerPort = Integer.parseInt(port);
		}
		
		// Make sure they entered a good file name
		if (keyFileName == null || keyFileName.equals("")) {
			System.err.println("You must enter the name of the public key file.\n");
			System.exit(1);
		}
		// Otherwise assign the key file to the right server
		else {
			if (serverType) {
				
				try {
					// Add Bouncy Castle as the Security Provider if necessary
					if (Security.getProvider("BC") == null) {
						System.err.println("\n" + this + " adding Bouncy Castle as Security Provider\n");
						Security.addProvider(new BouncyCastleProvider());
					}
					
					FileInputStream stream = new FileInputStream(keyFileName);
					byte[] key = new byte[stream.available()];
					stream.read(key);
					stream.close();
					X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
					KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
					PublicKey groupServerPublicKey = factory.generatePublic(keySpec);
					gsRSADecryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
					gsRSADecryptCipher.init(Cipher.DECRYPT_MODE, groupServerPublicKey);
				} catch(Exception e) {
					System.out.println("Creating the Group Server's RSA Decrypt Cipher failed\n");
					e.printStackTrace();
					System.exit(1);
				}
					
			}
			else {
				
				try {
					FileInputStream stream = new FileInputStream(keyFileName);
					byte[] key = new byte[stream.available()];
					stream.read(key);
					stream.close();
					X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
					KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
					fileServerPublicKey = factory.generatePublic(keySpec);
					fsRSADecryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
					fsRSADecryptCipher.init(Cipher.DECRYPT_MODE, fileServerPublicKey);
				} catch(Exception e) {
					System.out.println("Creating the File Server's RSA Decrypt Cipher failed\n");
					e.printStackTrace();
					System.exit(1);
				}
				
			}
		}
		
	}
	
	/**
	 * Make sure that the user exists and retrieve the user's token from the Group Server if a connection has not already been established
	 * @param userName the user name supplied as a command line argument
	 * @return true for a valid user and false for an invalid user
	 */
	private boolean validateUser(String userName, String password) {

		if (!groupClient.isConnected()) {
			groupClient.connect(groupServerName, groupServerPort, gsRSADecryptCipher, userName, password);
			if (groupClient.isConnected() == false) {
				System.out.println("Could not connect to Group Server");
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Shows a menu to the user, gets an input string from the user, validates 
	 * the user string, and then executes the command if the string is a valid 
	 * command. If the string is invalid, it will inform the user, and the menu 
	 * level will not change, so the same menu will be shown again.
	 * 
	 * @return false if the user wants to exit; otherwise, true for continue
	 */
	private boolean executeCommands() {
		String	userInput;
		boolean	valid;
		
		showMenu();
		userInput	= getUserString();
		valid 		= validateString(userInput);
		
		clearScreen();
		
		if (valid) {
			return executeString(userInput);
		}
		else {
			System.out.println(userInput +" is not a valid option. Please try again.\n");
			return true;
		}
	}
	
	/**
	 * From http://stackoverflow.com/questions/2979383/java-clear-the-console
	 * See the comment with "You can use following code to clear command line console:"
	 */
	private static void clearScreen() {
		System.out.print("\033[H\033[2J");
		System.out.flush();
    }  
	
	/**
	 * Shows the correct menu to the user based on the menuLevel
	 */
	private void showMenu() {
		switch(menuLevel) {
			case MAIN_TOP_LEVEL:
			{
				displayMTL();
				break;
			}
			case GROUP_TOP_LEVEL:
			{
				displayGTL();
				break;
			}
			case FILE_TOP_LEVEL:
			{
				// If the user wants to do file operations but has not specified a file server name and port,
				// we need to acquire those
				if (!connectToFileServer) {
					getServerInformation(false);
				}
				if (fileClient.isConnected() == false) {
					fileClient.connect(fileServerName, fileServerPort, fsRSADecryptCipher);
					
					if (!fileClient.isConnected()) {
						System.out.println("Could not connect to the specified File Server\n\n");
						return;
					}
					
					this.userToken = groupClient.getToken(this.userName, this.fileServerPublicKey);
					if (this.userToken == null) {
						System.err.println("Could not obtain token for File Operation");
						System.exit(1);
					}
					
					connectToFileServer = true;
					clearScreen();
				}
				displayFTL();
				break;
			}
		
		}
	}
	
	/**
	 * Print out the MAIN_TOP_LEVEL menu
	 */
	private void displayMTL() {
		System.out.println("Welcome to the secure distributed file sharing application.");
		System.out.println("Created by Alex P., Glenn S., and Tri N.");
		System.out.println("Type one of the following choices:");
		for (MTL_Strings s : MTL_Strings.values()) {
			System.out.println("\t" + s.getCommand() + ": " + s.getDescription());
		}
	}
	
	/**
	 * Print out the GROUP_TOP_LEVEL menu
	 */
	private void displayGTL() {
		System.out.println("Manage Groups and Users");
		System.out.println("Type one of the following choices:");
		for (GTL_Strings s : GTL_Strings.values()) {
			System.out.println("\t" + s.getCommand() + ": " + s.getDescription());
		}
	}
	
	/**
	 * Print out the FILE_TOP_LEVEL menu
	 */
	private void displayFTL() {
		System.out.println("Manage Files");
		System.out.println("Type one of the following choices:");
		for (FTL_Strings s : FTL_Strings.values()) {
			System.out.println("\t" + s.getCommand() + ": " + s.getDescription());
		}
	}
	
	/**
	 * Gets a line of input from the user
	 * 
	 * @return the string that the user enters in response to the menu
	 * or the exit string if an IOException occurs
	 */
	private String getUserString() {
		try {
			return inputReader.readLine();
		} catch (IOException e) {
			e.printStackTrace(); 
			return MTL_Strings.EXIT.toString();
		}
	}
	
	/**
	 * Make sure that the user has entered a response string that is valid for 
	 * the current menu
	 * 
	 * @param input the user input string
	 * @return true if valid, and false if invalid
	 */
	private boolean validateString(String input) {
		switch(menuLevel) {
			case MAIN_TOP_LEVEL:
			{
				for (MTL_Strings s : MTL_Strings.values()) {
					if ((s.getCommand()).compareToIgnoreCase(input) == 0) {
						mtlCommand = s;
						return true;
					}
				}
				break;
			}
			case GROUP_TOP_LEVEL:
			{
				for (GTL_Strings s : GTL_Strings.values()) {
					if ((s.getCommand()).compareToIgnoreCase(input) == 0) {
						gtlCommand = s;
						return true;
					}
				}
				break;
			}
			case FILE_TOP_LEVEL:
			{
				for (FTL_Strings s : FTL_Strings.values()) {
					if ((s.getCommand()).compareToIgnoreCase(input) == 0) {
						ftlCommand = s;
						return true;
					}
				}
				break;
			}
		}
		return false;
	}
	
	/**
	 * Perform the command based on the current menu level and the command for that 
	 * menu level, which was set in the validateString method
	 * 
	 * @return false if the user wants to exit; otherwise, returns true
	 */
	private boolean executeString(String input) {
		boolean commandSuccess = false;
		
		switch(menuLevel) {
			case MAIN_TOP_LEVEL:
			{
				switch(mtlCommand) {
					case GROUP:
					{						
						menuLevel = MenuEnum.GROUP_TOP_LEVEL;
						break;
					}
					case FILE:
					{
						menuLevel = MenuEnum.FILE_TOP_LEVEL;
						break;
					}
					case EXIT:
					{
						return false;
					}
				}
				break;
			}
			case GROUP_TOP_LEVEL:
			{
				if (!groupClient.isConnected()) {
					System.out.println("Group Client is not connected. Trying to establish a new connection...");
					groupClient = new GroupClient();
					groupClient.connect(groupServerName, groupServerPort, gsRSADecryptCipher, userName, password);
				}
				if (groupClient.isConnected()){
					switch(gtlCommand) {
						case CREATE_USER:
						{
							commandSuccess = createUser();
							break;
						}
						case DELETE_USER:
						{
							commandSuccess = deleteUser();
							break;
						}
						case CREATE_GROUP:
						{
							commandSuccess = createGroup();
							break;
						}
						case DELETE_GROUP:
						{
							commandSuccess = deleteGroup();
							break;
						}
						case ADD_USER_TO_GROUP:
						{
							commandSuccess = addUserToGroup();
							break;
						}
						case DELETE_USER_FROM_GROUP:
						{
							commandSuccess = deleteUserFromGroup();
							break;
						}
						case LIST_MEMBERS:
						{
							commandSuccess = listMembers();
							break;
						}
						case CHANGE_PASSWORD:
						{
							commandSuccess = changePassword();
							break;
						}
						case SET_GROUP:
						{
							commandSuccess = setGroup();
							break;
						}
						case ALL_GROUPS:
						{
							commandSuccess = allGroups();
							break;
						}
						case BACK:
						{
							menuLevel = MenuEnum.MAIN_TOP_LEVEL;
							commandSuccess = true;
							break;
						}
						case EXIT:
						{
							return false;
						}
					}
					
					if (commandSuccess == false) System.out.println("Command was unsuccessful\n\n");
				}
				else System.out.println("Connection to Group Server Failed. Command could not be executed.\n");
				break;
			}
			case FILE_TOP_LEVEL:
			{
				if (fileClient.isConnected()) {
					this.userToken = groupClient.getToken(this.userName, this.fileServerPublicKey);
					if (this.userToken == null) {
						System.err.println("Could not obtain token for File Operation");
						return false;
					}
					
					switch(ftlCommand) {
						case LIST_FILES:
						{
							commandSuccess = listFiles();
							break;
						}
						case UPLOAD_FILE:
						{
							commandSuccess = uploadFile();
							break;
						}
						case DOWNLOAD_FILE:
						{
							commandSuccess = downloadFile();
							break;
						}
						case DELETE_FILE:
						{
							commandSuccess = deleteFile();
							break;
						}
						case CHANGE_SERVER:
						{
							commandSuccess = changeFileServer();
							break;
						}
						case BACK:
						{
							menuLevel = MenuEnum.MAIN_TOP_LEVEL;
							commandSuccess = true;
							break;
						}
						case EXIT:
						{
							return false;
						}
					}
					
					if (commandSuccess == false) System.out.println("Command was unsuccessful\n\n");
				}
				else System.out.println("Connection to File Server Failed. Command could not be executed.");
				break;
			}
		}
		return true;
	}
	
	private boolean createUser() {
		String userName;
		
		System.out.println("Enter the username to create:");
		userName = getUserString();
		
		try {
			return groupClient.createUser(userName);
		}catch(Exception e) {
			clearScreen();
			System.out.println("Create User failed");
			e.printStackTrace();
			System.out.println("");
		}
		return false;
	}
	
	private boolean deleteUser() {
		String userName;
		
		System.out.println("Enter the username to delete:");
		userName = getUserString();
		
		try {
			return groupClient.deleteUser(userName);
		}catch(Exception e) {
			clearScreen();
			System.out.println("Delete User failed");
			e.printStackTrace();
			System.out.println("");
		}
		return false;
	}

	private boolean createGroup() {
		String group;
		
		System.out.println("Enter the group to be created");
		group = getUserString();
		
		try {
			return groupClient.createGroup(group);
		}catch(Exception e) {
			clearScreen();
			System.out.println("Create Group failed");
			e.printStackTrace();
			System.out.println("");
		}
		return false;
	}

	private boolean deleteGroup() {
		String group;
		
		System.out.println("Enter the group to be deleted");
		group = getUserString();
		
		try {
			return groupClient.deleteGroup(group);
		}catch(Exception e) {
			clearScreen();
			System.out.println("Delete Group failed");
			e.printStackTrace();
			System.out.println("");
		}
		return false;
	}

	private boolean addUserToGroup() {
		String userName;
		String group;
		
		System.out.println("Enter the name of the group:");
		group = getUserString();
		
		System.out.println("Enter the username to be added to the group:");
		userName = getUserString();
		
		try {
			return groupClient.addUserToGroup(userName, group);
		}catch(Exception e) {
			clearScreen();
			System.out.println("Add Member failed");
			e.printStackTrace();
			System.out.println("");
		}
		return false;
	}

	private boolean deleteUserFromGroup() {
		String userName;
		String group;
		
		System.out.println("Enter the name of the group:");
		group = getUserString();
		
		System.out.println("Enter the username to be removed from the group:");
		userName = getUserString();
		
		try {
			return groupClient.deleteUserFromGroup(userName, group);
		}catch(Exception e) {
			clearScreen();
			System.out.println("Remove Member failed");
			e.printStackTrace();
			System.out.println("");
		}
		return false;
	}
	
	private boolean listMembers() {
		List<String> members = null;
		String group;
		
		System.out.println("Enter the name of the group:");
		group = getUserString();
		try {
			members = groupClient.listMembers(group);
			if (members == null) return false;
		}catch(Exception e) {
			clearScreen();
			System.out.println("List Files failed");
			e.printStackTrace();
			System.out.println("");
			return false;
		}
		
		if (members != null) {
			System.out.println(members.size() + " users in " + group + ": ");
			for (String user : members) {
				System.out.println(user);
			}
		}
		
		return true;
	}
	
	private boolean changePassword() {
		String tempPassword;
		
		System.out.println("Enter your new pasword:");
		tempPassword = getUserString();
		try {
			if (groupClient.changePassword(tempPassword) == true) {
				this.password = new String(tempPassword);
				return true;
			}
			else return false;
		}catch(Exception e) {
			clearScreen();
			System.out.println("Change Password failed");
			e.printStackTrace();
			System.out.println("");
			return false;
		}
	}
	
	private boolean setGroup() {
		String group;
		
		System.out.println("Enter the name of the group:");
		group = getUserString();
		try {
			if (groupClient.setGroup(group) == true) {
				return true;
			}
			else return false;
		}catch(Exception e) {
			clearScreen();
			System.out.println("Set Group failed");
			e.printStackTrace();
			System.out.println("");
			return false;
		}
	}
	
	private boolean allGroups() {
		String tempPassword;
		
		try {
			if (groupClient.setGroup(null) == true) {
				return true;
			}
			else return false;
		}catch(Exception e) {
			clearScreen();
			System.out.println("All Groups failed");
			e.printStackTrace();
			System.out.println("");
			return false;
		}
	}

	private boolean listFiles() {
		List<String> files = null;
		
		try {
			files = fileClient.listFiles(userToken);
		}catch(Exception e) {
			clearScreen();
			System.out.println("List Files failed");
			e.printStackTrace();
			System.out.println("");
			return false;
		}
		
		if (files != null) {
			for (String file : files) {
				System.out.println(file);
			}
		}
		
		return true;
	}

	private boolean uploadFile() {
		String localFile;
		String remoteFile;
		String group;
		
		
		System.out.println("Enter the local file to upload:");
		localFile = getUserString();
		
		System.out.println("Enter a name for the file to be shared by:");
		remoteFile = getUserString();
		
		System.out.println("Enter the group you want to share the file with:");
		group = getUserString();
		
		try {
			KeyIndex pair = groupClient.getLatestKey(group);
			
			if (pair == null) {
				System.err.println("Upload failed: could not obtain Group Key");
				return false;
			}
				
			return fileClient.upload(localFile, remoteFile, group, userToken, pair.key, pair.index);
		}catch(Exception e) {
			clearScreen();
			System.out.println("Upload failed");
			e.printStackTrace();
			System.out.println("");
		}
		
		return false;
	}

	private boolean downloadFile() {
		String localFile;
		String remoteFile;
		
		System.out.println("Enter the remote file you want to download:");
		remoteFile = getUserString();
		
		System.out.println("Enter a local pathname for the file:");
		localFile = getUserString();
		
		try {
			return fileClient.download(remoteFile, localFile, userToken, groupClient);
		}catch(Exception e) {
			clearScreen();
			System.out.println("Download failed");
			e.printStackTrace();
			System.out.println("");
		}
		
		return false;
	}

	private boolean deleteFile() {
		String remoteFile;
		
		System.out.println("Enter the name of the remote file you want to delete:");
		remoteFile = getUserString();
		
		try {
			return fileClient.delete(remoteFile, userToken);
		}catch(Exception e)	{
			clearScreen();
			System.out.println("Delete failed");
			e.printStackTrace();
			System.out.println("");
		}
		
		return false;
	}
	
	private boolean changeFileServer() {
		getServerInformation(false);
		fileClient.connect(fileServerName, fileServerPort, fsRSADecryptCipher);
		
		if (!fileClient.isConnected()) {
			System.out.println("Could not connect to the specified File Server\n\n");
			return false;
		}
		
		connectToFileServer = true;
		return true;
	}
}
