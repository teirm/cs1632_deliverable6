import javax.crypto.Cipher;
import java.security.MessageDigest;

public class SecureMachine {

    private Cipher encryptCipher;
    private Cipher decryptCipher;
    private MessageDigest sha1;

    public SecureMachine(Cipher encryptCipher, Cipher decryptCipher) {
        this.encryptCipher = encryptCipher;
        this.decryptCipher = decryptCipher;
        try {
            this.sha1 = MessageDigest.getInstance("SHA1", "BC");
        } catch (Exception e) {
            System.err.println("SHA-1 algorithm not available.");
            e.printStackTrace();
            this.sha1 = null;
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
        if (encryptCipher == null) {
            System.err.println("Failed to encrypt bytes. Missing encryption cipher.");
            return null;
        }
        try {
            return encryptCipher.doFinal(bytes);
        } catch (Exception e) {
            System.err.println("Failed to encrypt bytes.");
            e.printStackTrace();
        }
        return null;
    }

    private byte[] decryptBytes(byte[] bytes) {
        if (decryptCipher == null) {
            System.err.println("Failed to decrypt bytes. Missing decryption cipher.");
            return null;
        }
        try {
            return decryptCipher.doFinal(bytes);
        } catch (Exception e) {
            System.err.println("Failed to decrypt bytes.");
            e.printStackTrace();
        }
        return null;
    }

}