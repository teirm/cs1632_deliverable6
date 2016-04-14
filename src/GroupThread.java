import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

import java.lang.Thread;
import java.math.BigInteger;
import java.net.Socket;
import java.io.*;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class GroupThread extends Thread {

	private final Socket socket;
	private GroupServer groupServer;
	private AES_Machine cipher;
	private String user; // The username supplied during authentication
	private Cipher signCipher;
	private Cipher encryptCipher;
	private BigInteger counter;
	private Mac mac;
	private boolean allGroups;
	private String group;
	private PublicKey fsPublicKey;
	
	public GroupThread(Socket socket, GroupServer groupServer, Cipher signCipher) {
		this.socket = socket;
		this.groupServer = groupServer;
		this.signCipher = signCipher;
		this.allGroups = true;
	}
	
	public void run() {
		try {
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			counter = new BigInteger("0");
			
			// create the Diffie-Hellman object
			DiffieHellman dh = new DiffieHellman();
			// generate the Diffie-Hellman key pair
			dh.GenerateDHKeyPair();
			
			// get the client's public key
			Envelope envelope = (Envelope)input.readObject();
			
			// Get and check the counter
			try {
				BigInteger rec_counter = (BigInteger)envelope.getObjContents().get(1);
				if (rec_counter.equals(this.counter) == false) {
					System.err.println("Diffie Hellman exchange failed: Invalid Counter Received");
					return;
				}
			} catch(Exception e) {
				System.err.println("Diffie Hellman exchange failed: Invalid Counter Received");
				e.printStackTrace();
				return;
			}
						
			// client has commenced authentication by sending public key
			if (envelope.getMessage().equals("PUBLICKEY")) {
				// generate the AES shared key
				Object publicKey = envelope.getObjContents().get(0);
				dh.GenerateAESKey(publicKey);
				cipher = new AES_Machine(dh.GetAESKey());
				mac = Mac.getInstance("HmacSHA1");
				mac.init(dh.GetHMACKey());
			} else {
				throw new Exception("Diffie-Hellman exchange failed.");
			}
			// send the public key to the client
			Envelope env = new Envelope("OK");
			env.addObject(dh.GetDHPublicKey());
			env.addObject(signBytes(hashBytes(dh.GetDHPublicKey().getEncoded()))); // Add the signed hash of the key

			// Increment the counter and add to envelope
			this.counter = this.counter.add(BigInteger.ONE);
			env.addObject(this.counter);
			
			output.writeObject(env);
			
			
			// expecting to read the client's username and password
			envelope = decryptEnvelope(input.readObject());
			if (envelope == null) {
				System.err.println("Error decrypting envelope...exiting thread");
				return;
			}
			
			// terminate the connection if the first contact isn't the username and password
			if (envelope.getMessage().equals("USERPASSWORD") && verifyNotNull(envelope, 2)){
				String name = (String)envelope.getObjContents().get(0);
				String password = (String)envelope.getObjContents().get(1);
				if (authenticateUser(name, password)) {
					user = new String(name);
					env = new Envelope("OK");
					output.writeObject(encryptEnvelope(env));
				} else {
					env = new Envelope("FAIL");
					output.writeObject(encryptEnvelope(env));
					socket.close();
					return;
				}
			} else {
				env = new Envelope("FAIL");
				output.writeObject(encryptEnvelope(env));
				socket.close();
				return;
			}
			
			while (true) {
				Envelope message = decryptEnvelope(input.readObject());
				if (message == null) {
					System.err.println("Error decrypting envelope...exiting thread");
					return;
				}
				
				System.out.println("Request received: " + message.getMessage());
				Envelope response = new Envelope("FAIL");
				if (message.getMessage().equals("GET")) {
					if (verifyNotNull(message, 1)) {
						PublicKey publicKey = (PublicKey)message.getObjContents().get(0);

						if (fsPublicKey == null || !fsPublicKey.equals(publicKey)) {
							fsPublicKey = publicKey;
							encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
							fsPublicKey = publicKey;
							encryptCipher.init(Cipher.ENCRYPT_MODE, fsPublicKey);
						}
						UserToken yourToken = createToken();
						if (yourToken != null) {
							response = new Envelope("OK");
							byte[] a = encryptBytes(hashBytes(yourToken.getContents()));
							System.out.println(a.length);
							byte[] signature = signBytes(a);
							yourToken.setSignature(signature);
							response.addObject(yourToken);
						}
					}
				} else if (message.getMessage().equals("LATESTKEY")) {
					if (verifyNotNull(message, 1)) {
						String group = (String)message.getObjContents().get(0);
						if ((this.allGroups || group.equals(this.group)) && this.groupServer.groupList.hasGroup(group) &&
								this.groupServer.groupList.getGroup(group).hasMember(user)) {
							SecretKey key = this.groupServer.groupList.getGroup(group).getLatestKey();
							Integer index = this.groupServer.groupList.getGroup(group).getLatestIndex();
							if (key != null) {
								response = new Envelope("OK");
								response.addObject(key);
								response.addObject(index);
							}
						}
					}
				} else if (message.getMessage().equals("INDEXKEY")) {
					if (verifyNotNull(message, 2)) {
						String group = (String)message.getObjContents().get(0);
						Integer index = (Integer)message.getObjContents().get(1);
						if (this.allGroups || group.equals(this.group) && this.groupServer.groupList.hasGroup(group) &&
								this.groupServer.groupList.getGroup(group).hasMember(user)) {
							SecretKey key = this.groupServer.groupList.getGroup(group).getKey(user, index);
							if (key != null) {
								response = new Envelope("OK");
								response.addObject(key);
							}
						}
					}
				} else if (message.getMessage().equals("ALLGROUPS")) {
					this.allGroups = true;	
					response = new Envelope("OK");
				} else if (message.getMessage().equals("SETGROUP")) {
					if (verifyNotNull(message, 1)) {
						String group = (String)message.getObjContents().get(0);
						// Make sure the user is in the group
						if (groupServer.groupList.getGroup(group).getMembers().contains(this.user) == true) {
							this.allGroups = false;
							this.group = group;
							response = new Envelope("OK");
						}
						
					}
				} else if (message.getMessage().equals("CHANGEPASSWORD")) {
					if (verifyNotNull(message, 1)) {
						String password = (String)message.getObjContents().get(0);
						if (changePassword(user, password)) {
							response = new Envelope("OK");
						}
					}
				}else if (message.getMessage().equals("CUSER")) {
					if (verifyNotNull(message, 1)) {
						String username = (String)message.getObjContents().get(0);
						UserToken yourToken = createToken();
						// verify that the user was created successfully
						if (createUser(username, yourToken)) {
							response = new Envelope("OK");
						}
					}
				} else if (message.getMessage().equals("DUSER")) {
					if (verifyNotNull(message, 1)) {
						String username = (String)message.getObjContents().get(0);
						UserToken yourToken = createToken();
						// verify that the user was deleted successfully
						if (deleteUser(username, yourToken)) {
							response = new Envelope("OK");
						}
					}
				} else if (message.getMessage().equals("CGROUP")) {
					if (verifyNotNull(message, 1)) {
						String groupname = (String)message.getObjContents().get(0);
						UserToken yourToken = createToken();
						// verify that the group was created successfully
						if (createGroup(groupname, yourToken)) {
							response = new Envelope("OK");
						}
					}
				} else if (message.getMessage().equals("DGROUP")) {
					if (verifyNotNull(message, 1)) {
						String groupname = (String)message.getObjContents().get(0);
						UserToken yourToken = createToken();
						// verify that the group was deleted successfully
						if (deleteGroup(groupname, yourToken)) {
							response = new Envelope("OK");
						}
					}
				} else if (message.getMessage().equals("LMEMBERS")) {
					if (verifyNotNull(message, 1)) {
						String groupname = (String)message.getObjContents().get(0);
						UserToken yourToken = createToken();
						List<String> members = listMembers(groupname, yourToken);
						// verify that the list of members is successfully retrieved
						if (members != null) {
							response = new Envelope("OK");
							response.addObject(members);
						}
					}
				} else if (message.getMessage().equals("AUSERTOGROUP")) {
					if (verifyNotNull(message, 2)) {
						String username = (String)message.getObjContents().get(0);
						String groupname = (String)message.getObjContents().get(1);
						UserToken yourToken = createToken();
						// verify that the user was added to the group successfully
						if (addUserToGroup(username, groupname, yourToken)) {
							response = new Envelope("OK");
						}
					}
				} else if (message.getMessage().equals("RUSERFROMGROUP")) {
					if (verifyNotNull(message, 2)) {
						String username = (String)message.getObjContents().get(0);
						String groupname = (String)message.getObjContents().get(1);
						UserToken yourToken = createToken();
						// verify that the user was removed from the group successfully
						if (deleteUserFromGroup(username, groupname, yourToken)) {
							response = new Envelope("OK");
						}
					}
				} else if (message.getMessage().equals("DISCONNECT")) {
					socket.close();
					break; // break out of the loop
				}
				output.writeObject(encryptEnvelope(response));
			}
		} catch (Exception e) {
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

	private byte[] signBytes(byte[] bytes) {
		try {
			return signCipher.doFinal(bytes);
		} catch (Exception e) {
			System.err.println("Failed to encrypt the user token digest.");
			e.printStackTrace();
		}
		return null;
	}

	private UserToken createToken() {
		// verify that the user exists
		if (groupServer.userList.checkUser(user)) {
			// issue a new token with server's name, user's name, and user's groups
			UserToken yourToken = null;
			if (this.allGroups == true) yourToken = new Token(groupServer.name, user, groupServer.userList.getUserGroups(user));
			else {
				ArrayList<String> oneGroup = new ArrayList<String>();
				oneGroup.add(this.group);
				yourToken = new Token(groupServer.name, user, oneGroup);
			}
			return yourToken;
		}
		return null;
	}

	private boolean createUser(String username, UserToken yourToken) {
		String requester = yourToken.getSubject();
		// verify that the requester exists
		if (groupServer.userList.checkUser(requester)) {
			// get the requester's groups
			ArrayList<String> temp = groupServer.userList.getUserGroups(requester);
			// verify that the requester is an administrator
			if (temp.contains("ADMIN")) {
				// verify that the user doesn't already exist
				if (!groupServer.userList.checkUser(username)) {
					groupServer.userList.addUser(username);
					return true;
				}
			}
		}
		return false;
	}

	private boolean deleteUser(String username, UserToken yourToken) {
		String requester = yourToken.getSubject();
		// verify that the requester exists
		if (groupServer.userList.checkUser(requester)) {
			ArrayList<String> temp = groupServer.userList.getUserGroups(requester);
			// verify that the requester is an administrator and that the user exists
			if (temp.contains("ADMIN") && groupServer.userList.checkUser(username)) {
				List<String> groups = groupServer.userList.getUserGroups(username);
				// remove the user from all of the groups that they belong to
				for (int i = 0; i < groups.size(); i++) {
					String group = groups.get(i);
					groupServer.groupList.getGroup(group).removeMember(username);
				}
				List<String> ownerships = groupServer.userList.getUserOwnership(username);
				// remove the user from all of the groups that they own
				for (int i = 0; i < ownerships.size(); i++) {
					String ownership = ownerships.get(i);
					List<String> members = groupServer.groupList.getGroup(ownership).getMembers();
					// remove all of the users from the deleted user's owned groups
					for (int j = 0; j < members.size(); j++) {
						String member = members.get(i);
						groupServer.userList.removeGroup(member, ownership);
					}
					groupServer.groupList.deleteGroup(ownership);
				}
				// delete the user from the user list
				groupServer.userList.deleteUser(username);
				return true;
			}
		}
		return false;
	}

	private boolean createGroup(String groupname, UserToken yourToken) {
		String requester = yourToken.getSubject();
		// verify that the requester exists and that the group doesn't exist
		if (groupServer.userList.checkUser(requester) && !groupServer.groupList.hasGroup(groupname)) {
			// verify that the group was created successfully
			if (groupServer.groupList.createGroup(groupname, yourToken)) {
				groupServer.userList.addOwnership(requester, groupname);
				groupServer.userList.addGroup(requester, groupname);
				return true;
			}
		}
		return false;
	}

	private boolean deleteGroup(String groupname, UserToken yourToken) {
		String requester = yourToken.getSubject();
		// verify that the requester exists and that the group exists
		if (groupServer.userList.checkUser(requester) && groupServer.groupList.hasGroup(groupname)) {
			String owner = groupServer.groupList.getGroup(groupname).getOwner();
			// verify that the requester is the owner of the group
			if (owner.equals(requester)) {
				List<String> members = groupServer.groupList.getGroup(groupname).getMembers();
				// verify that the group was deleted successfully
				if (groupServer.groupList.deleteGroup(groupname)) {
					groupServer.userList.removeOwnership(requester, groupname);
					// remove the group from all of its members group lists
					for (int i = 0; i < members.size(); i++) {
						String member = members.get(i);
						if (groupServer.userList.checkUser(member)) {
							groupServer.userList.removeGroup(member, groupname);
						}
					}
					return true;
				}
			}
		}
		return false;
	}

	private boolean addUserToGroup(String username, String groupname, UserToken yourToken) {
		String requester = yourToken.getSubject();
		// verify that the requester exists and that the group exists
		if (groupServer.userList.checkUser(requester) && groupServer.groupList.hasGroup(groupname)) {
			String owner = groupServer.groupList.getGroup(groupname).getOwner();
			// verify that the requester is the owner of the group
			if (owner.equals(requester) && groupServer.userList.checkUser(username)) {
				groupServer.groupList.getGroup(groupname).addMember(username);
				groupServer.userList.addGroup(username, groupname);
				return true;
			}
		}
		return false;
	}

	private boolean deleteUserFromGroup(String username, String groupname, UserToken yourToken) {
		String requester = yourToken.getSubject();
		// verify that the requester exists and that the group exists
		if (groupServer.userList.checkUser(requester) && groupServer.userList.checkUser(username) &&
				groupServer.groupList.hasGroup(groupname) && groupServer.groupList.getGroup(groupname).hasMember(username)) {
			String owner = groupServer.groupList.getGroup(groupname).getOwner();
			// verify that the requester is the owner of the group
			if (owner.equals(requester)) {
				groupServer.groupList.getGroup(groupname).removeMember(username);
				groupServer.userList.removeGroup(username, groupname);
				return true;
			}
		}
		return false;
	}

	private List<String> listMembers(String groupname, UserToken yourToken) {
		String requester = yourToken.getSubject();
		// verify that the requester exists and that the group exists
		if (groupServer.userList.checkUser(requester) && groupServer.groupList.hasGroup(groupname)) {
			String owner = groupServer.groupList.getGroup(groupname).getOwner();
			// verify that the requester is the owner of the group
			if (owner.equals(requester)) {
				return groupServer.groupList.getGroup(groupname).getMembers();
			}
		}
		return null;
	}

	private boolean verifyNotNull(Envelope message, int values) {
		if (message.getObjContents().size() < values) {
			return false;
		}
		for (int i = 0; i < values; i++) {
			if (message.getObjContents().get(i) == null) {
				return false;
			}
		}
		return true;
	}
	
	private boolean authenticateUser(String username, String password) {
		return groupServer.userList.checkPassword(username, password);
	}
	
	private boolean changePassword(String username, String password) {
		return groupServer.userList.changePassword(username, password);
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
