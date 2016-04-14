/* File worker thread handles the business of uploading, downloading, and removing files for clients with valid tokens */

import javax.crypto.Cipher;
import javax.crypto.Mac;

import java.io.*;
import java.lang.Thread;
import java.math.BigInteger;
import java.net.Socket;
import java.security.Key;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileThread extends Thread
{
	private final Socket socket;
	private AES_Machine cipher;
	private Key publicKey;
	private Key privateKey;
	private Cipher encryptCipher;
	private Cipher decryptCipher;
	private Cipher unsignCipher;
	private BigInteger counter;
	private Mac mac;

	public FileThread(Socket socket, Cipher encryptCipher, Cipher decryptCipher, Cipher unsignCipher,
					  PublicKey publicKey, PrivateKey privateKey) {
		this.socket = socket;
		this.decryptCipher = decryptCipher;
		this.encryptCipher = encryptCipher;
		this.unsignCipher = unsignCipher;
		this.publicKey = publicKey;
		this.privateKey = privateKey;
	}

	public void run()
	{
		boolean proceed = true;
		try
		{
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			Envelope response;
			counter = new BigInteger("0");
			
			/*** 		Diffie-Hellman exchange			***/
			// Create the DH object and generate the keys
			DiffieHellman dh = new DiffieHellman();
			dh.GenerateDHKeyPair();
			
			// Get the Client's public key
			response = (Envelope)input.readObject();
			
			// Get and check the counter
			try {
				BigInteger rec_counter = (BigInteger)response.getObjContents().get(1);
				if (rec_counter.equals(this.counter) == false) {
					System.err.println("Diffie Hellman exchange failed: Invalid Counter Received");
					return;
				}
			} catch(Exception e) {
				System.err.println("Diffie Hellman exchange failed: Invalid Counter Received");
				e.printStackTrace();
				return;
			}
			
			//Successful response
			if (response.getMessage().equals("PUBLICKEY")) {
				dh.GenerateAESKey( (response.getObjContents()).get(0) );
				cipher = new AES_Machine(dh.GetAESKey());
				mac = Mac.getInstance("HmacSHA1");
				mac.init(dh.GetHMACKey());
			}
			else throw new Exception("Diffie Hellman exchange failed");
			
			// Send the public key -- this is the last unencrypted Envelope to be sent
			Envelope env = new Envelope("OK");
			env.addObject(dh.GetDHPublicKey());
			env.addObject(encryptBytes(hashBytes(dh.GetDHPublicKey().getEncoded()))); // Add the signed hash of the key
			
			// Increment the counter and add to envelope
			this.counter = this.counter.add(BigInteger.ONE);
			env.addObject(this.counter);
			
			output.writeObject(env);
			

			do
			{
				Envelope e = decryptEnvelope(input.readObject());
				if (e == null) {
					System.err.println("Error decrypting envelope...exiting thread");
					return;
				}
				
				System.out.println("Request received: " + e.getMessage());

				// Handler to list files that this user is allowed to see
				if(e.getMessage().equals("LFILES"))
				{
					UserToken yourToken = (UserToken)e.getObjContents().get(0);
					if (yourToken==null)
					{
						response = new Envelope("FAIL-BADTOKEN");
					}
					else
					{
						List<String> allowedList = new ArrayList<String>();
						
						ArrayList<ShareFile> filesList = FileServer.fileList.getFiles();
						byte[] digest = decryptBytes(unsignBytes(yourToken.getSignature()));
						if (Arrays.equals(digest, hashBytes(yourToken.getContents()))) {
							List<String> groupsList = yourToken.getGroups();
							for (int i = 0; i < filesList.size(); i++) {
								if (groupsList.contains(filesList.get(i).getGroup())) {
									allowedList.add(filesList.get(i).getPath());
								}
							}
							response = new Envelope("OK");
							response.addObject(allowedList);
						} else {
							response = new Envelope("FAIL-TOKENSIGNATURE");
						}
					}
					output.writeObject(encryptEnvelope(response));
				}
				if(e.getMessage().equals("UPLOADF"))
				{

					if(e.getObjContents().size() < 4)
					{
						response = new Envelope("FAIL-BADCONTENTS");
					}
					else
					{
						if(e.getObjContents().get(0) == null) {
							response = new Envelope("FAIL-BADPATH");
						}
						else if(e.getObjContents().get(1) == null) {
							response = new Envelope("FAIL-BADGROUP");
						}
						else if(e.getObjContents().get(2) == null) {
							response = new Envelope("FAIL-BADTOKEN");
						}
						else if(e.getObjContents().get(3) == null) {
							response = new Envelope("FAIL-BADINDEX");
						}
						else {
							String remotePath = (String)e.getObjContents().get(0);
							String group = (String)e.getObjContents().get(1);
							UserToken yourToken = (UserToken)e.getObjContents().get(2);
							Integer index = (Integer)e.getObjContents().get(3);
							
							byte[] digest = decryptBytes(unsignBytes(yourToken.getSignature()));
							if (Arrays.equals(digest, hashBytes(yourToken.getContents()))) {
								if (FileServer.fileList.checkFile(remotePath)) {
									System.out.printf("Error: file already exists at %s\n", remotePath);
									response = new Envelope("FAIL-FILEEXISTS"); //Success
								} else if (!yourToken.getGroups().contains(group)) {
									System.out.printf("Error: user missing valid token for group %s\n", group);
									response = new Envelope("FAIL-UNAUTHORIZED"); //Success
								} else {
									File file = new File("shared_files/" + remotePath.replace('/', '_'));
									file.createNewFile();
									FileOutputStream fos = new FileOutputStream(file);
									System.out.printf("Successfully created file %s\n", remotePath.replace('/', '_'));

									response = new Envelope("READY"); //Success
									output.writeObject(encryptEnvelope(response));

									e = decryptEnvelope(input.readObject());
									if (e == null) {
										System.err.println("Error decrypting envelope...exiting thread");
										return;
									}
									
									while (e.getMessage().compareTo("CHUNK") == 0) {
										fos.write((byte[]) e.getObjContents().get(0), 0, (Integer) e.getObjContents().get(1));
										response = new Envelope("READY"); //Success
										output.writeObject(encryptEnvelope(response));
										e = decryptEnvelope(input.readObject());
										if (e == null) {
											System.err.println("Error decrypting envelope...exiting thread");
											return;
										}
									}

									if (e.getMessage().compareTo("EOF") == 0) {
										System.out.printf("Transfer successful file %s\n", remotePath);
										FileServer.fileList.addFile(yourToken.getSubject(), group, remotePath, index);
										response = new Envelope("OK"); //Success
									} else {
										System.out.printf("Error reading file %s from client\n", remotePath);
										response = new Envelope("ERROR-TRANSFER"); //Success
									}
									fos.close();
								}
							} else {
								response = new Envelope("FAIL-TOKENSIGNATURE");
							}
						}
					}

					output.writeObject(encryptEnvelope(response));
				}
				else if (e.getMessage().compareTo("DOWNLOADF_META")==0) {
					String remotePath = (String)e.getObjContents().get(0);
					UserToken yourToken = (UserToken)e.getObjContents().get(1);
				    
					byte[] digest = decryptBytes(unsignBytes(yourToken.getSignature()));
					if (Arrays.equals(digest, hashBytes(yourToken.getContents()))) 
					{
						ShareFile sf = FileServer.fileList.getFile("/"+remotePath);
						if (sf == null) {
							System.out.printf("Error: File %s doesn't exist\n", remotePath);
							e = new Envelope("ERROR_FILEMISSING");
							output.writeObject(encryptEnvelope(e));
	
						}
						else if (!yourToken.getGroups().contains(sf.getGroup())){
							System.out.printf("Error user %s doesn't have permission\n", yourToken.getSubject());
							e = new Envelope("ERROR_PERMISSION");
							output.writeObject(encryptEnvelope(e));
						}
						else {
							// Get the metadata from the file -- group and index
							String group = sf.getGroup();
							Integer index = sf.getIndex();
							
							e = new Envelope("METADATA");
							e.addObject(group);
							e.addObject(index);
							output.writeObject(encryptEnvelope(e));
						}
					}
				}
				else if (e.getMessage().compareTo("DOWNLOADF")==0) {

					String remotePath = (String)e.getObjContents().get(0);
					UserToken yourToken = (UserToken)e.getObjContents().get(1);
					byte[] digest = decryptBytes(unsignBytes(yourToken.getSignature()));
					if (Arrays.equals(digest, hashBytes(yourToken.getContents()))) 
					{
						ShareFile sf = FileServer.fileList.getFile("/"+remotePath);
						if (sf == null) {
							System.out.printf("Error: File %s doesn't exist\n", remotePath);
							e = new Envelope("ERROR_FILEMISSING");
							output.writeObject(encryptEnvelope(e));
	
						}
						else if (!yourToken.getGroups().contains(sf.getGroup())){
							System.out.printf("Error user %s doesn't have permission\n", yourToken.getSubject());
							e = new Envelope("ERROR_PERMISSION");
							output.writeObject(encryptEnvelope(e));
						}
						else {
	
							try
							{
								File f = new File("shared_files/_"+remotePath.replace('/', '_'));
							if (!f.exists()) {
								System.out.printf("Error file %s missing from disk\n", "_"+remotePath.replace('/', '_'));
								e = new Envelope("ERROR_NOTONDISK");
								output.writeObject(encryptEnvelope(e));
	
							}
							else {
								FileInputStream fis = new FileInputStream(f);
	
								do {
									byte[] buf = new byte[4096 + 16 + 16];
									if (e.getMessage().compareTo("DOWNLOADF")!=0) {
										System.out.printf("Server error: %s\n", e.getMessage());
										break;
									}
									e = new Envelope("CHUNK");
									int n = fis.read(buf); //can throw an IOException
									if (n > 0) {
										System.out.printf(".");
									} else if (n < 0) {
										System.out.println("Read error");
	
									}
	
	
									e.addObject(buf);
									e.addObject(new Integer(n));
	
									output.writeObject(encryptEnvelope(e));
	
									e = decryptEnvelope(input.readObject());
									if (e == null) {
										System.err.println("Error decrypting envelope...exiting thread");
										return;
									}
	
	
								}
								while (fis.available()>0);
	
								//If server indicates success, return the member list
								if(e.getMessage().compareTo("DOWNLOADF")==0)
								{
	
									e = new Envelope("EOF");
									output.writeObject(encryptEnvelope(e));
	
									e = decryptEnvelope(input.readObject());
									if (e == null) {
										System.err.println("Error decrypting envelope...exiting thread");
										return;
									}
									
									if(e.getMessage().compareTo("OK")==0) {
										System.out.printf("File data upload successful\n");
									}
									else {
	
										System.out.printf("Upload failed: %s\n", e.getMessage());
	
									}
	
								}
								else {
	
									System.out.printf("Upload failed: %s\n", e.getMessage());
	
								}
							}
							}
							catch(Exception e1)
							{
								System.err.println("Error: " + e.getMessage());
								e1.printStackTrace(System.err);
	
							}
						}
					}
				}
				else if (e.getMessage().compareTo("DELETEF")==0) {

					String remotePath = (String)e.getObjContents().get(0);
					Token t = (Token)e.getObjContents().get(1);
					ShareFile sf = FileServer.fileList.getFile("/"+remotePath);
					if (sf == null) {
						System.out.printf("Error: File %s doesn't exist\n", remotePath);
						e = new Envelope("ERROR_DOESNTEXIST");
					}
					else if (!t.getGroups().contains(sf.getGroup())){
						System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
						e = new Envelope("ERROR_PERMISSION");
					}
					else {

						try
						{


							File f = new File("shared_files/"+"_"+remotePath.replace('/', '_'));

							if (!f.exists()) {
								System.out.printf("Error file %s missing from disk\n", "_"+remotePath.replace('/', '_'));
								e = new Envelope("ERROR_FILEMISSING");
							}
							else if (f.delete()) {
								System.out.printf("File %s deleted from disk\n", "_"+remotePath.replace('/', '_'));
								FileServer.fileList.removeFile("/"+remotePath);
								e = new Envelope("OK");
							}
							else {
								System.out.printf("Error deleting file %s from disk\n", "_"+remotePath.replace('/', '_'));
								e = new Envelope("ERROR_DELETE");
							}


						}
						catch(Exception e1)
						{
							System.err.println("Error: " + e1.getMessage());
							e1.printStackTrace(System.err);
							e = new Envelope(e1.getMessage());
						}
					}
					output.writeObject(encryptEnvelope(e));

				}
				else if(e.getMessage().equals("DISCONNECT"))
				{
					socket.close();
					proceed = false;
				}
			} while(proceed);
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	private byte[] hashBytes(byte[] bytes) {
		try {
			MessageDigest sha1 = MessageDigest.getInstance("SHA1", "BC");
			return sha1.digest(bytes);
		} catch (Exception e) {
			System.err.println("Failed to hash the given bytes.");
			e.printStackTrace();
		}
		return null;
	}
	
	private byte[] encryptBytes(byte[] bytes) {
		try {
			return encryptCipher.doFinal(bytes);
		} catch (Exception e) {
			System.err.println("Failed to encrypt the user token digest.");
			e.printStackTrace();
		}
		return null;
	}

	private byte[] unsignBytes(byte[] bytes) {
		try {
			return unsignCipher.doFinal(bytes);
		} catch (Exception e) {
			System.err.println("Failed to decrypt the user token digest.");
			e.printStackTrace();
		}
		return null;
	}

	private byte[] decryptBytes(byte[] bytes) {
		try {
			return decryptCipher.doFinal(bytes);
		} catch (Exception e) {
			System.err.println("Failed to decrypt the user token digest.");
			e.printStackTrace();
		}
		return null;
	}
	
	private Envelope encryptEnvelope(Envelope message) {
		// Increment the counter and add to envelope
		this.counter = this.counter.add(BigInteger.ONE);
		message.addObject(this.counter);
		
		byte[] ciphertext = cipher.Encrypt(message);
		
		Envelope cipherMessage = new Envelope("");
		cipherMessage.addObject(cipher.GetIV());
		cipherMessage.addObject(ciphertext);
		
		// Add the HMAC
		int textlen = ciphertext.length;
		int ivlen = cipher.GetIV().length;
		
		byte[] textAndIV = new byte[textlen + ivlen];
		System.arraycopy(cipher.GetIV(), 0, textAndIV, 0, ivlen);
		System.arraycopy(ciphertext, 0, textAndIV, ivlen, textlen);
		
		cipherMessage.addObject(mac.doFinal(textAndIV));
		
		return cipherMessage;
	}
	
	private Envelope decryptEnvelope(Object response) {
		Envelope encryptedResponse = (Envelope)response;
		byte[] rec_IV = null;
		byte[] ciphertext = null;
		
		// Check the HMAC before decrypting the ciphertext
		// There should only be IV, ciphertext, and HMAC in the outer envelope
		if (encryptedResponse.getObjContents().size() != 3) {
			System.err.println("Encrypted Envelope has an invalid number of objects");
			return null;
		} // Check the HMAC
		else {
			rec_IV = (byte [])encryptedResponse.getObjContents().get(0);
			ciphertext = (byte [])encryptedResponse.getObjContents().get(1);
			
			byte[] rec_hmac = (byte[])encryptedResponse.getObjContents().get(2);
			
			int textlen = ciphertext.length;
			int ivlen = rec_IV.length;
			
			byte[] textAndIV = new byte[textlen + ivlen];
			System.arraycopy(rec_IV, 0, textAndIV, 0, ivlen);
			System.arraycopy(ciphertext, 0, textAndIV, ivlen, textlen);
			
			byte [] computed_hmac = mac.doFinal(textAndIV);
			
			// not equal
			if (Arrays.equals(computed_hmac, rec_hmac) == false) {
				System.err.println("Invalid HMAC received");
				return null;
			}
			
		}
				
		// Decrypt the ciphertext
		Envelope plaintext = cipher.Decrypt( (byte[])(encryptedResponse.getObjContents()).get(0), (byte [])(encryptedResponse.getObjContents()).get(1));
		
		int size = plaintext.getObjContents().size();
		if (size == 0) {
			System.err.println("No Counter Received");
			return null;
		}
		
		BigInteger rec_counter = (BigInteger)plaintext.getObjContents().get(size - 1);
		this.counter = this.counter.add(BigInteger.ONE);
		if (rec_counter.equals(this.counter) == false) {
			System.err.println("Invalid Counter Received");
			return null;
		}
		
		return plaintext;
	}

}
