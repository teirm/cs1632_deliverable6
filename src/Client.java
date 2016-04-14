import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.Arrays;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.crypto.Cipher;
import javax.crypto.Mac;

public abstract class Client {

	protected Socket sock;
	protected ObjectOutputStream output;
	protected ObjectInputStream input;
	protected AES_Machine cipher;
	protected BigInteger counter;
	protected Mac mac; 
	
	public boolean connect(final String server, final int port, final Cipher c) {
		System.out.println("attempting to connect");
		try 
		{
			// The counter stargs out at 0
			counter = new BigInteger("0");
			
			sock = new Socket(server,port);
			System.out.println("Connected to " + server + " on port " + port);
			output = new ObjectOutputStream(sock.getOutputStream());
			System.out.println("Acquired output stream");
			output.flush();
			input = new ObjectInputStream(sock.getInputStream());
			System.out.println("Acquired input stream");
			
			/*** 		Diffie-Hellman exchange			***/
			// Create the DH object and generate the keys
			DiffieHellman dh = new DiffieHellman();
			dh.GenerateDHKeyPair();
			
			// Send the public key -- this is the last unencrypted Envelope to be sent
			Envelope env = new Envelope("PUBLICKEY");
			env.addObject(dh.GetDHPublicKey());
			
			// Add the counter at the end
			env.addObject(counter);
			
			output.writeObject(env);
			
			// Get the Server's public key
			Envelope response = (Envelope)input.readObject();
			
			//Successful response
			if (response.getMessage().equals("OK")) {
				byte [] receivedHash = c.doFinal((byte [])response.getObjContents().get(1));
				PublicKey dhKey = (PublicKey)response.getObjContents().get(0);
				byte [] computedHash = MessageDigest.getInstance("SHA1", "BC").digest(dhKey.getEncoded());
				
				if (Arrays.equals(computedHash,receivedHash) != true) {
					System.err.println("Diffie Hellman exchange failed: Signature Invalid\nExiting...\n");
					System.exit(1);
				}
				
				// Get the counter from the end
				BigInteger rec_counter = (BigInteger)response.getObjContents().get(2);
				// Increment our counter to stay in syc
				counter = counter.add(BigInteger.ONE);
				
				// Make sure the counters are in sync
				if (rec_counter.equals(counter) == false) {
					System.err.println("Diffie Hellman exchange failed: Invalid Counter Received");
					System.exit(1);
				}
					
				dh.GenerateAESKey(dhKey);
				cipher = new AES_Machine(dh.GetAESKey());
				mac = Mac.getInstance("HmacSHA1");
				mac.init(dh.GetHMACKey());
			}
			else throw new Exception("Diffie Hellman exchange failed");
		} 
		catch(Exception e){
		    System.err.println("Error: " + e.getMessage());
		    e.printStackTrace(System.err);
		    return false;
		}

		return true;
	}

	public boolean isConnected() {
		if (sock == null || !sock.isConnected()) {
			return false;
		} else {
			return true;
		}
	}

	public void disconnect() {
		if (isConnected()) {
			try {
				Envelope cipherMessage = encryptEnvelope(new Envelope("DISCONNECT"));
				output.writeObject(cipherMessage);
			} catch (Exception e) {
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
			}
		}
	}
	
	protected Envelope encryptEnvelope(Envelope message) {
		// Increment the counter and add at the end
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
	
	protected Envelope decryptEnvelope(Object response) {
		Envelope encryptedResponse = (Envelope)response;
		byte[] rec_IV = null;
		byte[] ciphertext = null;
		
		// Check the HMAC before decrypting the ciphertext
		// There should only be IV, ciphertext, and HMAC in the outer envelope
		if (encryptedResponse.getObjContents().size() != 3) {
			System.err.println("Encrypted Envelope has an invalid number of objects");
			System.exit(1);
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
				System.exit(1);
			}
			
		}
		
		// Decrypt the ciphertext
		Envelope plaintext = cipher.Decrypt( rec_IV, ciphertext);
		
		int size = plaintext.getObjContents().size();
		if (size == 0) {
			System.err.println("No Counter Received");
			System.exit(1);
		}
		
		BigInteger rec_counter = (BigInteger)plaintext.getObjContents().get(size - 1);
		this.counter = this.counter.add(BigInteger.ONE);
		if (rec_counter.equals(this.counter) == false) {
			System.err.println("Invalid Counter Received");
			System.exit(1);
		}
		
		return plaintext;
	}
}
