/* FileClient provides all the client functionality regarding the file server */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class FileClient extends Client implements FileClientInterface {
	
	private Key hostkey ;
	String hostkeyInString="";
	Cipher RSACipher;
	public boolean connect(final String server, final int port, final Cipher c) {
		
		boolean fileServerSaved=false; //true if File Server's fingerprint is saved and correct
		boolean fileServerUntrusted=false; //true if something is wrong with this server fingerprint (same address but different keys)
		
		String serverName;
		if (server == null) serverName="null";
		else serverName = server;
		
		
		// diffie hellman handshake
		System.out.println("Performing handshaking protocol with FileServer...");
		return super.connect(server, port, c);
	}
	public boolean delete(String filename, UserToken token) {
		String remotePath;
		if (filename.charAt(0)=='/') {
			remotePath = filename.substring(1);
		}
		else {
			remotePath = filename;
		}

		Envelope env = new Envelope("DELETEF"); //Success
	    env.addObject(remotePath);
	    env.addObject(token);
	    try {
			output.writeObject(encryptEnvelope(env));
			
		    env = decryptEnvelope(input.readObject());
		    
			if (env.getMessage().compareTo("OK")==0) {
				System.out.printf("File %s deleted successfully\n", filename);				
			}
			else {
				System.out.printf("Error deleting file %s (%s)\n", filename, env.getMessage());
				return false;
			}			
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
	    	
		return true;
	}

	public boolean download(String sourceFile, String destFile, UserToken token, GroupClient groupClient) {
				if (sourceFile == null || destFile == null || token == null || groupClient == null) {
					System.err.println("A null pointer was passed to download()");
					return false;
				}
				
				if (sourceFile.charAt(0)=='/') {
					sourceFile = sourceFile.substring(1);
				}
		
				File file = new File(destFile);
			    try {
			    				
				
				    if (!file.exists()) {
				    	file.createNewFile();
					    FileOutputStream fos = new FileOutputStream(file);
					    
					    // Get the file metadata -- group and index
					    Envelope env = new Envelope("DOWNLOADF_META");
					    env.addObject(sourceFile);
					    env.addObject(token);
					    output.writeObject(encryptEnvelope(env)); 
					
					    env = decryptEnvelope(input.readObject());
					    
					    // Receive the group and index
					    if (env.getMessage().compareTo("METADATA") !=0) {
					    	System.err.println("Failed to receive File Metadata");
					    	return false;
					    }
					    
					    // First group name and then index
					    String group = (String)env.getObjContents().get(0);
					    Integer index = (Integer)env.getObjContents().get(1);
					    Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
					    
					    // Initialize the cipher with the indexed key from the Group Server
					    SecretKey aesKey = groupClient.getIndexedKey(group, index);
					    if (aesKey == null) {
					    	System.err.println("Could not get Decryption Key from the Group Server");
					    	return false;
					    }
					    
					    //System.out.println("download key is : " + (new BigInteger(aesKey.getEncoded()).toString(16)));
					    
					    // Get the actual file
					    env = new Envelope("DOWNLOADF"); //Success
					    env.addObject(sourceFile);
					    env.addObject(token);
					    output.writeObject(encryptEnvelope(env)); 
					    
					    env = decryptEnvelope(input.readObject());
					    
						while (env.getMessage().compareTo("CHUNK")==0) { 
							byte[] rec_bytes = (byte[])env.getObjContents().get(0);
							byte[] IV = Arrays.copyOfRange(rec_bytes, 0, 16);
							byte[] ciphertext = Arrays.copyOfRange(rec_bytes, 16, rec_bytes.length);
							
							//System.out.println("IV: " + (new BigInteger(IV)).toString(16) + ", " + IV.length);
							//System.out.println("ciphertext: " + (new BigInteger(ciphertext)).toString(16) + ", " + ciphertext.length);
							
							c.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(IV));
							byte[] file_bytes = c.doFinal(ciphertext);
							fos.write(file_bytes, 0, file_bytes.length);
							System.out.printf(".");
							env = new Envelope("DOWNLOADF"); //Success
							output.writeObject(encryptEnvelope(env));
							env = decryptEnvelope(input.readObject());									
						}										
						fos.close();
						
					    if(env.getMessage().compareTo("EOF")==0) {
					    	 fos.close();
								System.out.printf("\nTransfer successful file %s\n", sourceFile);
								env = new Envelope("OK"); //Success
								output.writeObject(encryptEnvelope(env));
						}
						else {
								System.out.printf("Error reading file %s (%s)\n", sourceFile, env.getMessage());
								file.delete();
								return false;								
						}
				    }    
					 
				    else {
						System.out.printf("Error couldn't create file %s\n", destFile);
						return false;
				    }
								
			
			    } catch (IOException e1) {
			    	
			    	System.out.printf("Error couldn't create file %s\n", destFile);
			    	return false;
			    
					
				}
			    catch (ClassNotFoundException e1) {
					e1.printStackTrace();
					return false;
				}
			    catch (Exception e) {
			    	System.err.println("download() has experienced an exception:");
			    	e.printStackTrace();
			    	return false;
			    }
				 return true;
	}

	@SuppressWarnings("unchecked")
	public List<String> listFiles(UserToken token) {
		 try
		 {
			 Envelope message = null, e = null;
			 //Tell the server to return the member list
			 message = new Envelope("LFILES");
			 message.addObject(token); //Add requester's token
			 output.writeObject(encryptEnvelope(message)); 
			 
			 e = decryptEnvelope(input.readObject());
			 
			 //If server indicates success, return the member list
			 if(e.getMessage().equals("OK"))
			 { 
				return (List<String>)e.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
			 }
				
			 return null;
			 
		 }
		 catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
				return null;
			}
	}

	public boolean upload(String sourceFile, String destFile, String group, UserToken token, SecretKey aesKey, Integer index) {
		if (sourceFile == null || destFile == null || token == null || aesKey == null || index == null) {
			System.err.println("A null pointer was passed to upload()");
			return false;
		}
		
		if (destFile.charAt(0)!='/') {
			 destFile = "/" + destFile;
		 }
		
		try
		 {
			 
			 Envelope message = null, env = null;
			 //Tell the server to return the member list
			 message = new Envelope("UPLOADF");
			 message.addObject(destFile);
			 message.addObject(group);
			 message.addObject(token); //Add requester's token
			 message.addObject(index); //Add the key index -- will be stored as metadata
			 output.writeObject(encryptEnvelope(message));
			
			 
			 FileInputStream fis = new FileInputStream(sourceFile);
			 
			 env = decryptEnvelope(input.readObject());
			 
			 //If server indicates success, return the member list
			 if(env.getMessage().equals("READY"))
			 { 
				System.out.printf("Meta data upload successful\n");
				
			}
			 else {
				
				 System.out.printf("Upload failed: %s\n", env.getMessage());
				 return false;
			 }
			 
			 //System.out.println("upload key is : " + (new BigInteger(aesKey.getEncoded()).toString(16)));
		 	
			 do {
				 byte[] buf = new byte[4096];
				 	if (env.getMessage().compareTo("READY")!=0) {
				 		System.out.printf("Server error: %s\n", env.getMessage());
				 		return false;
				 	}
				 	message = new Envelope("CHUNK");
					int n = fis.read(buf); //can throw an IOException
					if (n > 0) {
						System.out.printf(".");
					} else if (n < 0) {
						System.out.println("Read error");
						return false;
					}
					
					Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
					SecureRandom r = new SecureRandom();
					byte[] IV = new byte[16];
					// Generate the Initialization Vector and initialize the cipher
					r.nextBytes(IV);
					encryptCipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(IV));
					
					// Encrypt the file block with the supplied cipher
					byte[] ciphertext = encryptCipher.doFinal(buf);
					
					//System.out.println("IV: " + (new BigInteger(encryptCipher.getIV())).toString(16) + ", " + IV.length);
					//System.out.println("ciphertext: " + (new BigInteger(ciphertext)).toString(16) + ", " + ciphertext.length);
					
					// Store the IV and the ciphertext in the same array
					byte[] cipherBuf = new byte[ciphertext.length + 16]; // extra 16 for IV
					System.arraycopy(encryptCipher.getIV(), 0, cipherBuf, 0, 16);
					System.arraycopy(ciphertext, 0, cipherBuf, 16, ciphertext.length);
					
					// Send the encrypted block
					message.addObject(cipherBuf);
					// Send the length of the encrypted block
					message.addObject(new Integer(cipherBuf.length));
					
					output.writeObject(encryptEnvelope(message));
					
					
					env = decryptEnvelope(input.readObject());
										
			 }
			 while (fis.available()>0);		 
					 
			 //If server indicates success, return the member list
			 if(env.getMessage().compareTo("READY")==0)
			 { 
				
				message = new Envelope("EOF");
				output.writeObject(encryptEnvelope(message));
				
				env = decryptEnvelope(input.readObject());
				if(env.getMessage().compareTo("OK")==0) {
					System.out.printf("\nFile data upload successful\n");
				}
				else {
					
					 System.out.printf("\nUpload failed: %s\n", env.getMessage());
					 return false;
				 }
				
			}
			 else {
				
				 System.out.printf("Upload failed: %s\n", env.getMessage());
				 return false;
			 }
			 
		 }catch(Exception e1)
			{
				System.err.println("Error: " + e1.getMessage());
				e1.printStackTrace(System.err);
				return false;
				}
		 return true;
	}

}

