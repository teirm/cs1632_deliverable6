import java.math.BigInteger;
import java.security.*;
import java.util.Arrays;

import javax.crypto.*;
import javax.crypto.spec.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class DiffieHellman {
	private static final BigInteger p512 = new BigInteger("fca682ce8e12caba26efccf7110e526db078b05edecbcd1eb4a208f3ae1617ae01f35b91a47e6df63413c5e12ed0899bcd132acd50d99151bdc43ee737592e17", 16);
	private static final BigInteger g512 = new BigInteger("678471b27a9cf44ee91a49c5147db1a9aaf244f05a434d6486931d2d14271b9e35030b71fd73da179069b32e2935630e1c2062354d0da20a6c416e50be794ca4", 16);
	private DHParameterSpec dhParams;
	private KeyPair dhKeys;
	private SecretKey aesKey;
	private SecretKey hmacKey;
	
	public DiffieHellman() {
		// Add Bouncy Castle as the Security Provider if necessary
		if (Security.getProvider("BC") == null) {
			System.err.println("\n" + this + " adding Bouncy Castle as Security Provider\n");
			Security.addProvider(new BouncyCastleProvider());
		}
		// Create the diffie hellman parameter spec based on the p and g constants
		dhParams = new DHParameterSpec(p512, g512, 384);
	}
  
	public boolean GenerateDHKeyPair() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH", "BC");
			keyGen.initialize(dhParams, new SecureRandom());
			dhKeys = keyGen.generateKeyPair();
			return true;
		} catch(Exception e) {
			System.err.println("\n" + this + " GenerateDHKeyPair failed");
			e.printStackTrace();
			System.out.println("");
			return false;
		}
	}
	
	public SecretKey GenerateAESKey(Object pubKey) {
		try {
			KeyAgreement keyAgree = KeyAgreement.getInstance("DH", "BC");
			keyAgree.init(dhKeys.getPrivate());
			keyAgree.doPhase((PublicKey)pubKey, true);
			byte[] dh_shared_secret = keyAgree.generateSecret();
			
			aesKey = new SecretKeySpec(dh_shared_secret, 0, 16, "AES");
			hmacKey = new SecretKeySpec(dh_shared_secret, 16, 15, "HmacSHA1");
			return aesKey;
		} catch(Exception e) {
			System.err.println("\n" + this + " GenerateAESKey failed");
			e.printStackTrace();
			System.out.println("");
			return null;
		}
	}
	
	public SecretKey GetAESKey() {
		return aesKey;
	}
	
	public SecretKey GetHMACKey() {
		return hmacKey;
	}
	
	public PublicKey GetDHPublicKey() {
		return dhKeys.getPublic();
	}
}