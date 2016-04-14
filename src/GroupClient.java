import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.io.ObjectInputStream;
import java.math.BigInteger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class GroupClient extends Client implements GroupClientInterface {
 
	public boolean connect(final String server, final int port, Cipher c, String username, String password) {
		try {
			// Perform the normal connect stuff
			boolean result = super.connect(server, port, c);
			
			// Check the return value
			if (result == false) return result;
			
			// Send the Group Server the username and password to be verified
			// The username and password will be associated with the AES key
			// and, thus, the user will be authenticated
			Envelope env = new Envelope("USERPASSWORD");
			env.addObject(username);
			env.addObject(password);
			
			// Send the encrypted envelope
			output.writeObject(encryptEnvelope(env));
			
			// Get the Group Server's response to authentication
			Envelope response = decryptEnvelope(input.readObject());
			
			//Successful response
			if (response.getMessage().equals("OK")) {
				return true;
			}
			else {
				System.out.println("\nUser authentication failed: " + response.getMessage());
				return false;
			}
		} catch(Exception e) {
			System.err.println("\nConnection to Group Server Failed");
			e.printStackTrace();
			return false;
		}
	}
	
	public UserToken getToken(String username, PublicKey pk) {
		try {
			UserToken token = null;
			Envelope message = null, response = null;
			//Tell the server to return a token.
			message = new Envelope("GET");
			message.addObject(pk);
			output.writeObject(encryptEnvelope(message));
			
			//Get the response from the server
			response = decryptEnvelope(input.readObject());
			
			//Successful response
			if (response.getMessage().equals("OK")) {
				//If there is a token in the Envelope, return it
				ArrayList<Object> temp = null;
				temp = response.getObjContents();
				token = (UserToken)temp.get(0);
				return token;
			}
			return null;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public boolean createUser(String username) {
		try {
			Envelope message = null, response = null;
			//Tell the server to create a user
			message = new Envelope("CUSER");
			message.addObject(username); //Add user name string
			output.writeObject(encryptEnvelope(message));
			
			response = decryptEnvelope(input.readObject());
			
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean deleteUser(String username) {
		try {
			Envelope message = null, response = null;
			//Tell the server to delete a user
			message = new Envelope("DUSER");
			message.addObject(username); //Add user name
			output.writeObject(encryptEnvelope(message));
			
			// Decrypt the response
			response = decryptEnvelope(input.readObject());
			
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean createGroup(String groupname) {
		try {
			Envelope message = null, response = null;
			//Tell the server to create a group
			message = new Envelope("CGROUP");
			message.addObject(groupname); //Add the group name string
			output.writeObject(encryptEnvelope(message));
			
			response = decryptEnvelope(input.readObject());
			
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean deleteGroup(String groupname) {
		try {
			Envelope message = null, response = null;
			//Tell the server to delete a group
			message = new Envelope("DGROUP");
			message.addObject(groupname); //Add group name string
			output.writeObject(encryptEnvelope(message));
			
			response = decryptEnvelope(input.readObject());
			
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public List<String> listMembers(String group) {
		try {
			Envelope message = null, response = null;
			//Tell the server to return the member list
			message = new Envelope("LMEMBERS");
			message.addObject(group); //Add group name string
			output.writeObject(encryptEnvelope(message));
			
			response = decryptEnvelope(input.readObject());
			
			//If server indicates success, return the member list
			if (response.getMessage().equals("OK")) {
				return (List<String>)response.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
			}
			return null;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public boolean addUserToGroup(String username, String groupname) {
		try {
			Envelope message = null, response = null;
			//Tell the server to add a user to the group
			message = new Envelope("AUSERTOGROUP");
			message.addObject(username); //Add user name string
			message.addObject(groupname); //Add group name string
			output.writeObject(encryptEnvelope(message));
			
			response = decryptEnvelope(input.readObject());
			
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean deleteUserFromGroup(String username, String groupname) {
		try {
			Envelope message = null, response = null;
			//Tell the server to remove a user from the group
			message = new Envelope("RUSERFROMGROUP");
			message.addObject(username); //Add user name string
			message.addObject(groupname); //Add group name string
			output.writeObject(encryptEnvelope(message));
			
			response = decryptEnvelope(input.readObject());
			
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}
	
	public boolean changePassword(String password) {
		try {
			Envelope message = null, response = null;
			//Tell the server to change the user's password
			message = new Envelope("CHANGEPASSWORD");
			message.addObject(password); //Add password string
			output.writeObject(encryptEnvelope(message));
			
			response = decryptEnvelope(input.readObject());
			
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}
	
	public boolean setGroup(String group) {
		try {
			Envelope message = null, response = null;
			
			// This indicates allow file operations on all groups
			if (group == null) {
				message = new Envelope("ALLGROUPS");
				
			}// Otherwise restrict to one group
			else {
				message = new Envelope("SETGROUP");
				message.addObject(group); //Add group
			}
			
			// Write to the Server
			output.writeObject(encryptEnvelope(message));
			
			response = decryptEnvelope(input.readObject());
			
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return true;
			}
			return false;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}
	
	/**
	 * Gets the group's latest file encryption key and initializes the cipher accordingly
	 * @param group the group that the key belongs to
	 * @param c the cipher to be initialized with the key
	 * @return the index of the key on success, null on failure
	 */
	public KeyIndex getLatestKey(String group) {
		try {
			Envelope message = null, response = null;
			
			if (group == null) return null;
			
			message = new Envelope("LATESTKEY");
			message.addObject(group);
			
			// Write to the Server
			output.writeObject(encryptEnvelope(message));
			
			response = decryptEnvelope(input.readObject());
			
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				SecretKey aesKey = (SecretKey)response.getObjContents().get(0);
				Integer rec_index = (Integer)response.getObjContents().get(1);
				return new KeyIndex(aesKey, rec_index);
			}
			return null;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}
	
	/**
	 * Gets a specific file decryption key based on an index and initializes the cipher accordingly
	 * @param group the group that the key belongs to
	 * @param index the index of the key
	 * @return true on success, false on failure
	 */
	public SecretKey getIndexedKey(String group, Integer index) {
		try {
			Envelope message = null, response = null;
			
			if (group == null) return null;
			if (index == null || index.compareTo(new Integer(0)) == -1) return null;
			
			message = new Envelope("INDEXKEY");
			message.addObject(group);
			message.addObject(index);
			
			// Write to the Server
			output.writeObject(encryptEnvelope(message));
			
			response = decryptEnvelope(input.readObject());
			
			//If server indicates success, return true
			if (response.getMessage().equals("OK")) {
				return (SecretKey)response.getObjContents().get(0);
			}
			return null;
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}
}
