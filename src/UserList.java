import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class UserList implements java.io.Serializable {

	private static final long serialVersionUID = 7600343803563417992L;
	private Hashtable<String, User> list = new Hashtable<String, User>();

	public synchronized void addUser(String username) {
		User newUser = new User();
		list.put(username, newUser);
	}

	public synchronized void deleteUser(String username) {
		list.remove(username);
	}

	public synchronized boolean checkUser(String username) {
		return list.containsKey(username);
	}
	
	public synchronized boolean checkPassword(String username, String password) {
		// Make sure that the username is in the list
		if (list.containsKey(username) == true) {
			return list.get(username).verifyPassword(password);
		} // Otherwise return false
		else return false;
	}
	
	public synchronized boolean changePassword(String username, String password) {
		// Make sure that the username is in the list
		if (list.containsKey(username) == true) {
			return list.get(username).addPassword(password);
		} // Otherwise return false
		else return false;
	}

	public synchronized ArrayList<String> getUserGroups(String username) {
		return list.get(username).getGroups();
	}

	public synchronized ArrayList<String> getUserOwnership(String username) {
		return list.get(username).getOwnership();
	}

	public synchronized void addGroup(String user, String groupname) {
		list.get(user).addGroup(groupname);
	}

	public synchronized void removeGroup(String user, String groupname) {
		list.get(user).removeGroup(groupname);
	}

	public synchronized void addOwnership(String user, String groupname) {
		list.get(user).addOwnership(groupname);
	}

	public synchronized void removeOwnership(String user, String groupname) {
		list.get(user).removeOwnership(groupname);
	}
	
	class User implements java.io.Serializable {

		private static final long serialVersionUID = -6699986336399821598L;
		private ArrayList<String> groups;
		private ArrayList<String> ownership;
		private byte[] passwordHash;
		private byte[] passwordSalt;
		
		public User() {
			groups = new ArrayList<String>();
			ownership = new ArrayList<String>();
		}
		
		public ArrayList<String> getGroups() {
			return groups;
		}
		
		public ArrayList<String> getOwnership() {
			return ownership;
		}
		
		public void addGroup(String group) {
			groups.add(group);
		}
		
		public void removeGroup(String group) {
			if (!groups.isEmpty()) {
				if (groups.contains(group)) {
					groups.remove(groups.indexOf(group));
				}
			}
		}
		
		public void addOwnership(String group) {
			ownership.add(group);
		}
		
		public void removeOwnership(String group) {
			if (!ownership.isEmpty() && ownership.contains(group)) {
				ownership.remove(ownership.indexOf(group));
			}
		}
		
		public boolean addPassword(String password) {
			try {
				SecureRandom random = new SecureRandom();
				
				// Generate a random salt and store it
				passwordSalt = new byte[16];
				random.nextBytes(passwordSalt);
				
				// This method of hashing and salting came from the first answer to
				// http://stackoverflow.com/questions/2860943/how-can-i-hash-a-password-in-java
				// 65536 iterations, 128 bit key length
				KeySpec spec = new PBEKeySpec(password.toCharArray(), passwordSalt, 65536, 128);
				SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
				passwordHash = f.generateSecret(spec).getEncoded();
				return true;
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		public boolean verifyPassword(String password) {
			try {
				// The case where the user does not have a password -- add the password and return true
				if (passwordHash == null || passwordSalt == null) {
					
					return addPassword(password);
				} 
				else {
					KeySpec spec = new PBEKeySpec(password.toCharArray(), passwordSalt, 65536, 128);
					SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
					byte[] tempHash = f.generateSecret(spec).getEncoded();
					
					if (Arrays.equals(passwordHash, tempHash) == true) return true;
					else return false;
				}
			} catch(Exception e) {
				System.err.println("\nUserList verifyPassword() failed");
				e.printStackTrace();
			}
			return false;
		}
	}
	
}	
