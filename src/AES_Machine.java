import java.math.BigInteger;
import java.security.*;
import java.util.Arrays;

import javax.crypto.*;
import javax.crypto.spec.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

public class AES_Machine {
	private SecretKey aesKey;
	private byte[] IV;
	
	public AES_Machine(Object key) {
		// Add Bouncy Castle as the Security Provider if necessary
		if (Security.getProvider("BC") == null) {
			System.err.println("\n" + this + " adding Bouncy Castle as Security Provider\n");
			Security.addProvider(new BouncyCastleProvider());
		}
		
		aesKey = (SecretKey)key;
		IV = new byte[16];
	}
	
	public byte[] Encrypt(Envelope plaintext) {
		try {
			Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			SecureRandom r = new SecureRandom();
			
			// Generate the Initialization Vector and initialize the cipher
			r.nextBytes(IV);
			aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(IV));
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(out);
			os.writeObject(plaintext);
			
			// Encrypt message and return the array of bytes
			return aesCipher.doFinal(out.toByteArray());
		} catch(Exception e) {
			System.err.println("\n" + this + " Encrypt failed");
			e.printStackTrace();
			System.out.println("");
			return null;
		}
	}
	
	public byte[] GetIV() {
		try {
			return Arrays.copyOf(IV, 16);
		} catch(Exception e) {
			System.err.println("\n" + this + " GetIV failed");
			e.printStackTrace();
			System.out.println("");
			return null;
		}
	}
	
	public Envelope Decrypt(byte[] initVector, byte[] ciphertext) {
		try {
			Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			aesCipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(initVector));
					    
			// Decrypt message and return the envelope
			ByteArrayInputStream in = new ByteArrayInputStream(aesCipher.doFinal(ciphertext));
			ObjectInputStream is = new ObjectInputStream(in);
			return (Envelope)is.readObject();
		} catch(Exception e) {
			System.err.println("\n" + this + " Decrypt failed");
			e.printStackTrace();
			System.out.println("");
			return null;
		}
	}
}