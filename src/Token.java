import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class Token implements UserToken, java.io.Serializable {

    private String issuer;
    private String subject;
    private List<String> groups;
    private byte[] signature;

    public Token(String issuer, String subject, List<String> groups) {
        this.issuer = issuer;
        this.subject = subject;
        this.groups = groups;
        this.signature = null;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getSubject() {
        return subject;
    }

    public List<String> getGroups() {
        return groups;
    }

    public byte[] getContents() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
            objectStream.writeObject(issuer);
            objectStream.writeObject(subject);
            objectStream.writeObject(groups);
            byte[] contents = outputStream.toByteArray();
            return contents;
        } catch (Exception e) {
            System.err.println("Failed to get contents of user token.");
            e.printStackTrace();
        }
        return null;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public byte[] getSignature() {
        return signature;
    }

}