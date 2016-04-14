import java.security.PublicKey;
import java.util.List;

import javax.crypto.Cipher;

/**
 * Interface describing the operations that must be supported by the
 * client application used to talk with the group server.  All methods
 * must be implemented!
 *
 */
public interface GroupClientInterface
{
    /**
     * Connect to the specified group server.  No other methods should
     * work until the client is connected to a group server.
     *
     * @param server The IP address or hostname of the group server
     * @param port The port that the group server is listening on
     * @param c The cipher used to verify the server's signature
     *
     * @return true if the connection succeeds, false otherwise
     *
     */
    public boolean connect(final String server, final int port, final Cipher c);


    /**
     * Close down the connection to the group server.
     *
     */
    public void disconnect();


    /**
     * Method used to get a token from the group server.  Right now,
     * there are no security checks.
     *
     * @param username The user whose token is being requested
     * @param pk The public key of the File Server
     *
     * @return A UserToken describing the permissions of "username."
     *         If this user does not exist, a null value will be returned.
     *
     */
    public UserToken getToken(final String username, PublicKey pk);


    /**
     * Creates a new user.  This method should only succeed if the
     * user invoking it is a member of the special group "ADMIN".
     *
     * @param username The name of the user to create
     *
     * @return true if the new user was created, false otherwise
     *
     */
    public boolean createUser(final String username);


    /**
     * Deletes a user.  This method should only succeed if the user
     * invoking it is a member of the special group "ADMIN".  Deleting
     * a user should also remove him or her from all existing groups.
     *
     * @param username The name of the user to delete
     *
     * @return true if the user was deleted, false otherwise
     *
     */
    public boolean deleteUser(final String username);


    /**
     * Creates a new group.  Any user may create a group, provided
     * that it does not already exist.
     *
     * @param groupname The name of the group to create
     *
     * @return true if the new group was created, false otherwise
     *
     */
    public boolean createGroup(final String groupname);


    /**
     * Deletes a group.  This method should only succeed if the user
     * invoking it is the user that originally created the group.
     *
     * @param groupname The name of the group to delete
     *
     * @return true if the group was deleted, false otherwise
     *
     */
    public boolean deleteGroup(final String groupname);


    /**
     * Adds a user to some group.  This method should succeed if
     * the user invoking the operation is the owner of the group.
     *
     * @param user  The user to add
     * @param group The name of the group to which user should be added
     *
     * @return true if the user was added, false otherwise
     *
     */
    public boolean addUserToGroup(final String user, final String group);


    /**
     * Removes a user from some group.  This method should succeed if
     * the user invoking the operation is the owner of the group.
     *
     * @param user  The name of the user to remove
     * @param group The name of the group from which user should be removed
     *
     * @return true if the user was removed, false otherwise
     *
     */
    public boolean deleteUserFromGroup(final String user, final String group);



    /**
     * Lists the members of a group.  This method should only succeed
     * if the user invoking the operation is the owner of the
     * specified group.
     *
     * @param group The group whose membership list is requested
     *
     * @return A List of group members.  Note that an empty list means
     *         a group has no members, while a null return indicates
     *         an error.
     */
    public List<String> listMembers(final String group);

}   //-- end interface GroupClientInterface