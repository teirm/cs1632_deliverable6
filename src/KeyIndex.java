import javax.crypto.SecretKey;

public class KeyIndex {
	public SecretKey key;
	public Integer index;
	
	public KeyIndex(SecretKey k, Integer i) {
		key = k;
		index = i;
	}
}
