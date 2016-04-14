
import java.math.BigInteger;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

/**
 * Interface describing the operations that must be supported by the
 * client application used to talk with the file servers.  All methods
 * must be implemented!
 *
 */
public interface FileClientInterface
{
    /**
     * Connect to the specified file server.  No other methods should
     * work until the client is connected to a file server.
     *
     * @param server The IP address or hostname of the file server
     * @param port The port that the file server is listening on
     *
     * @return true if the connection succeeds, false otherwise
     *
     */
    public boolean connect(final String server, final int port, final Cipher c);


    /**
     * Close down the connection to the file server.
     *
     */
    public void disconnect();


    /**
     * Retrieves a list of files that are allowed to be displayed
     * members of the groups encoded in the supplied user token.
     *
     * @param token The UserToken object assigned to the user invoking this operation
     *
     * @return A list of filenames
     *
     */
    public List<String> listFiles(final UserToken token);


    /**
     * Uploads a file to the server to be shared with members of the
     * specified group.  This method should only succeed if the
     * uploader is a member of the group that the file will be shared
     * with.
     *
     * @param sourceFile Path to the local file to upload
     * @param destFile   The filename to use on the server
     * @param group      The group to share this file with
     * @param token      The token of the user uploading the file
     * @param encryptCipher	The cipher used to encrypt the file blocks
     * @param index      The index of the key used for encryption
     *
     * @return true on success, false on failure
     *
     */
    public boolean upload(final String sourceFile, final String destFile, final String group, final UserToken token, final SecretKey aesKey, final Integer index);


    /**
     * Downloads a file from the server.  The user must be a member of
     * the group with which this file is shared.
     *
     * @param sourceFile The filename used on the server
     * @param destFile   The filename to use locally
     * @param token      The token of the user uploading the file
     * @param groupClient Used to get the decryption key
     *
     * @return true on success, false on failure
     *
     */
    public boolean download(final String sourceFile, final String destFile, final UserToken token, final GroupClient groupClient);


    /**
     * Deletes a file from the server.  The user must be a member of
     * the group with which this file is shared.
     *
     * @param filename The file to delete
     * @param token    The token of the user requesting the delete
     *
     * @return true on success, false on failure
     *
     */
    public boolean delete(final String filename, final UserToken token);


}  //-- end interface FileClientInterface