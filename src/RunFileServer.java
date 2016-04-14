/* Driver program for FileSharing File Server */

public class RunFileServer {
	
	public static void main(String[] args) {
		if (args.length > 0) {
			FileServer server;
			String publicKeyFilename = args[0];
			if (args.length > 1) {
				try {
					int port = Integer.parseInt(args[1]);
					server = new FileServer(publicKeyFilename, port);
				} catch (NumberFormatException e) {
					System.out.printf("Enter a valid port number or pass no arguments to use the default port (%d)\n", FileServer.SERVER_PORT);
					return;
				}
			} else {
				server = new FileServer(publicKeyFilename);
			}
			server.start();
		} else {
			System.out.println("You must pass in the public key and IV padding of the group server.");
		}
	}

}
