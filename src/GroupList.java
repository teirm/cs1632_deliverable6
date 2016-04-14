import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.SecureRandom;
import java.util.*;

public class GroupList implements java.io.Serializable {

    private Hashtable<String, Group> groupList;

    public GroupList() {
        this.groupList = new Hashtable<String, Group>();
    }

    public synchronized boolean hasGroup(String groupname) {
        return groupList.containsKey(groupname);
    }

    public synchronized Group getGroup(String groupname) {
        return groupList.get(groupname);
    }

    public synchronized boolean createGroup(String groupname, UserToken token) {
        if (!hasGroup(groupname)) {
            String owner = token.getSubject();
            Group newGroup = new Group(groupname, owner);
            groupList.put(groupname, newGroup);
            return true;
        }
        return false;
    }

    public synchronized boolean deleteGroup(String groupname) {
        if (hasGroup(groupname)) {
            groupList.remove(groupname);
            return true;
        }
        return false;
    }

    class Group implements java.io.Serializable {

        private String groupname;
        private String owner;
        private Map<String, Integer> members;
        private List<SecretKey> keys;

        public Group(String groupname, String owner) {
            this.groupname = groupname;
            this.owner = owner;
            this.members = new HashMap<String, Integer>();
            this.keys = new ArrayList<SecretKey>();
            generateKey();
            addMember(owner);
        }

        public String getGroupname() {
            return groupname;
        }

        public String getOwner() {
            return owner;
        }

        public List<String> getMembers() {
            List<String> memberList = new ArrayList<String>();
            for (String member : members.keySet()) {
                memberList.add(member);
            }
            return memberList;
        }

        public void addMember(String member) {
            if (members.isEmpty() || !members.containsKey(member)) {
                members.put(member, keys.size() - 1);
            }
        }

        public void removeMember(String member) {
            if (!members.isEmpty() && members.containsKey(member)) {
                members.remove(member);
                generateKey();
            }
        }

        public boolean hasMember(String member) {
            return members.containsKey(member);
        }

        public SecretKey getLatestKey() {
            return !keys.isEmpty() ? keys.get(keys.size() - 1) : null;
        }

        public int getLatestIndex() {
            return keys.size() - 1;
        }

        public SecretKey getKey(String member, Integer index) {
            Integer memberIndex = hasMember(member) ? members.get(member) : null;
            if (index == null | memberIndex == null || index < memberIndex || keys.isEmpty() || index < 0 || index > keys.size() - 1) {
                return null;
            }
            return keys.get(index);
        }

        private void generateKey() {
            SecureRandom random = new SecureRandom();
            byte[] bytes = new byte[16];
            random.nextBytes(bytes);
            SecretKeySpec key = new SecretKeySpec(bytes, "AES");
            keys.add(key);
        }

    }

}