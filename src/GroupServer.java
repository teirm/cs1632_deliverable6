import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.*;
import org.bouncycastle.util.encoders.Base64;

public class GroupServer extends Server {

	public static final int SERVER_PORT = 8765;

	public UserList userList;
	public GroupList groupList;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private Cipher signCipher;

    public GroupServer() {
		super(SERVER_PORT, "ALPHA");
	}

	public GroupServer(int _port) {
		super(_port, "ALPHA");
	}

	public void start() {
        // add BouncyCastle as a security provider if it hasn't been added yet
        if (Security.getProvider("BC") == null) {
            System.err.println("\n" + this + " adding Bouncy Castle as Security Provider\n");
            Security.addProvider(new BouncyCastleProvider());
        }
		String userFile = "UserList.bin";
		String groupFile = "GroupList.bin";
		Scanner console = new Scanner(System.in);
        FileInputStream fileStream;
		ObjectInputStream inputStream;
		Runtime runtime = Runtime.getRuntime();
		runtime.addShutdownHook(new ShutDownListener(this));
		try {
            // read in the previously created user list
			fileStream = new FileInputStream(userFile);
			inputStream = new ObjectInputStream(fileStream);
			userList = (UserList)inputStream.readObject();
            // read in the previously created group list
			fileStream = new FileInputStream(groupFile);
			inputStream = new ObjectInputStream(fileStream);
			groupList = (GroupList)inputStream.readObject();
		} catch (FileNotFoundException e) {
            // prompt user for administrator account username since no users exist
			System.out.println("User lists or group list doesn't exist. Creating...");
			System.out.println("No users currently exist. Your account will be the administrator.");
			System.out.print("Enter your username: ");
			String username = console.next();
			// create a new user list
			userList = new UserList();
            // add current user as administrator
			userList.addUser(username);
			userList.addGroup(username, "ADMIN");
			userList.addOwnership(username, "ADMIN");
            // create a new group list
			groupList = new GroupList();
            // create the administrator group that this user is now a part of
			groupList.createGroup("ADMIN", new Token(name, username, null));
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Failed to read from the user or group list file.");
			System.exit(-1);
		}
        try {
            // create a key pair generator instance
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
            // initialize with 2048 bits
            generator.initialize(2048);
            // generate the public/private key pair
            KeyPair key = generator.generateKeyPair();
            publicKey = key.getPublic();
            privateKey = key.getPrivate();
            // create an instance of the RSA cipher for encryption/decryption
            signCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
            // initialize with the public/private key pair
            signCipher.init(Cipher.ENCRYPT_MODE, privateKey, new SecureRandom());
            try {
                // write the public key to disk so that users can pass it to File Servers
                FileOutputStream stream = new FileOutputStream("publickey_gs.txt");
                byte[] keyBytes = publicKey.getEncoded();
                stream.write(keyBytes);
                stream.close();
            } catch (Exception e) {
                System.err.println("Failed to create the public key text file.");
                e.printStackTrace();
                System.exit(-1);
            }
        } catch (Exception e) {
            System.err.println("Failed to generate RSA key pairs.");
            e.printStackTrace();
            System.exit(-1);
        }
		// automatically saves the user and group list every 5 minutes
		AutoSave aSave = new AutoSave(this);
		aSave.setDaemon(true);
		aSave.start();
		try {
            // create a server socket on the specified port
			final ServerSocket serverSocket = new ServerSocket(port);
			Socket socket = null;
			GroupThread thread =  null;
			while (true) {
                // create a new thread for every new connection
				socket = serverSocket.accept();
				thread = new GroupThread(socket, this, signCipher);
				thread.start();
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
}

class ShutDownListener extends Thread {

	public GroupServer groupServer;

	public ShutDownListener(GroupServer groupServer) {
		this.groupServer = groupServer;
	}

	public void run() {
		System.out.println("Shutting down group server.");
		ObjectOutputStream outStream;
		try {
			outStream = new ObjectOutputStream(new FileOutputStream("UserList.bin"));
			outStream.writeObject(groupServer.userList);
			outStream = new ObjectOutputStream(new FileOutputStream("GroupList.bin"));
			outStream.writeObject(groupServer.groupList);
		} catch(Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
}

class AutoSave extends Thread {

	public GroupServer groupServer;

	public AutoSave(GroupServer groupServer) {
		this.groupServer = groupServer;
	}

	public void run() {
		do {
			try {
				Thread.sleep(300000); // 5 minutes
				System.out.println("Saving group and user lists...");
				ObjectOutputStream outStream;
				try {
					outStream = new ObjectOutputStream(new FileOutputStream("UserList.bin"));
					outStream.writeObject(groupServer.userList);
					outStream = new ObjectOutputStream(new FileOutputStream("GroupList.bin"));
					outStream.writeObject(groupServer.groupList);
				} catch (Exception e) {
					System.err.println("Error: " + e.getMessage());
					e.printStackTrace(System.err);
				}
			} catch (Exception e) {
				System.out.println("Failed to autosave due to interruption.");
			}
		} while (true);
	}
}
