/* FileServer loads files from FileList.bin.  Stores files in shared_files directory. */

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

public class FileServer extends Server {
	
	public static final int SERVER_PORT = 4321;
	public static FileList fileList;
	private PublicKey groupServerPublicKey;
	private Cipher unsignCipher;
	private Cipher decryptCipher;
	private Cipher encryptCipher;
	private PublicKey publicKey;
	private PrivateKey privateKey;
	
	public FileServer() {
		super(SERVER_PORT, "FilePile");
	}

	public FileServer(String publicKeyFilename) {
		this(publicKeyFilename, SERVER_PORT);
	}

	public FileServer(String publicKeyFilename, int port) {
		super(port, "FilePile");
		// add BouncyCastle as a security provider if it hasn't been added yet
		if (Security.getProvider("BC") == null) {
			System.err.println("\n" + this + " adding Bouncy Castle as Security Provider\n");
			Security.addProvider(new BouncyCastleProvider());
		}
		try {
			//Create a RSA KeyPair
			try {
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
				KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
				generator.initialize(1024);
			    KeyPair keys = generator.genKeyPair();
			    publicKey = keys.getPublic();
			    privateKey = keys.getPrivate();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			FileInputStream stream = new FileInputStream(publicKeyFilename);
			byte[] key = new byte[stream.available()];
			stream.read(key);
			stream.close();
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(key);
			KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
			groupServerPublicKey = factory.generatePublic(keySpec);
			unsignCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
			unsignCipher.init(Cipher.DECRYPT_MODE, groupServerPublicKey);
			encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
			encryptCipher.init(Cipher.ENCRYPT_MODE, privateKey, new SecureRandom());
			decryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
			decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
			
			try {
                // write the public key to disk so that users can pass it to File Servers
                FileOutputStream stream_out = new FileOutputStream("publickey_fs.txt");
                byte[] keyBytes = publicKey.getEncoded();
                stream_out.write(keyBytes);
                stream_out.close();
            } catch (Exception e) {
                System.err.println("Failed to create the public key text file.");
                e.printStackTrace();
                System.exit(-1);
            }
			
		} catch (Exception e) {
			System.out.println("Failed to create PublicKey from passed public key string.");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public void start() {
		
		String fileFile = "FileList.bin";
		ObjectInputStream fileStream;
		
		//This runs a thread that saves the lists on program exit
		Runtime runtime = Runtime.getRuntime();
		Thread catchExit = new Thread(new ShutDownListenerFS());
		runtime.addShutdownHook(catchExit);
		
		//Open user file to get user list
		try
		{
			FileInputStream fis = new FileInputStream(fileFile);
			fileStream = new ObjectInputStream(fis);
			fileList = (FileList)fileStream.readObject();
		}
		catch(FileNotFoundException e)
		{
			System.out.println("FileList Does Not Exist. Creating FileList...");
			
			fileList = new FileList();
			
		}
		catch(IOException e)
		{
			System.out.println("Error reading from FileList file");
			System.exit(-1);
		}
		catch(ClassNotFoundException e)
		{
			System.out.println("Error reading from FileList file");
			System.exit(-1);
		}
		
		File file = new File("shared_files");
		 if (file.mkdir()) {
			 System.out.println("Created new shared_files directory");
		 }
		 else if (file.exists()){
			 System.out.println("Found shared_files directory");
		 }
		 else {
			 System.out.println("Error creating shared_files directory");				 
		 }
		
		//Autosave Daemon. Saves lists every 5 minutes
		AutoSaveFS aSave = new AutoSaveFS();
		aSave.setDaemon(true);
		aSave.start();
		
		
		boolean running = true;
		
		try
		{			
			final ServerSocket serverSock = new ServerSocket(port);
			System.out.printf("%s up and running\n", this.getClass().getName());
			
			Socket sock = null;
			Thread thread = null;
			
			while(running)
			{
				sock = serverSock.accept();
				thread = new FileThread(sock, encryptCipher, decryptCipher, unsignCipher, publicKey, privateKey);
				thread.start();
			}
			
			System.out.printf("%s shut down\n", this.getClass().getName());
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
}

//This thread saves user and group lists
class ShutDownListenerFS implements Runnable
{
	public void run()
	{
		System.out.println("Shutting down server");
		ObjectOutputStream outStream;

		try
		{
			outStream = new ObjectOutputStream(new FileOutputStream("FileList.bin"));
			outStream.writeObject(FileServer.fileList);
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
}

class AutoSaveFS extends Thread
{
	public void run()
	{
		do
		{
			try
			{
				Thread.sleep(300000); //Save group and user lists every 5 minutes
				System.out.println("Autosave file list...");
				ObjectOutputStream outStream;
				try
				{
					outStream = new ObjectOutputStream(new FileOutputStream("FileList.bin"));
					outStream.writeObject(FileServer.fileList);
				}
				catch(Exception e)
				{
					System.err.println("Error: " + e.getMessage());
					e.printStackTrace(System.err);
				}

			}
			catch(Exception e)
			{
				System.out.println("Autosave Interrupted");
			}
		}while(true);
	}
}
