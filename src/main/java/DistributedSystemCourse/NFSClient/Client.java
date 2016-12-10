package DistributedSystemCourse.NFSClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observer;
import java.util.Scanner;

import org.acplt.oncrpc.OncRpcException;

public class Client {
	private Scanner scanner;
	// the shared folder
	private String sharedFolder;
	// the servers
	private Map<String, ServerInferface> servers;
	private String mountDir;

	public Client() {
		scanner = new Scanner(System.in);
		servers = new HashMap<>();
	}

	public static void main(String[] args) {
		Client c = new Client();
		c.UI();
	}

	/**
	 * Run the client UI.
	 */
	private void UI() {
		boolean quit = false;
		String cmd = null;
		printWelcome();

		while (!quit) {
			System.out.print("> ");

			cmd = scanner.nextLine();
			quit = processCmd(cmd);
		}
	}

	/**
	 * Prints the available commands.
	 */
	private void printWelcome() {
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++ NFS Client ++++++++++++++++++++++++++++++++++++++++++++++");
		System.out.println("|                                                                                                      |");
		System.out.println("|  Commands:                                                                                           |");
		System.out.println("|                                                                                                      |");
		System.out.println("|\t - mount <server>:IP <replica>:IP <uid>:int <gid>:int <remoteRoot>: String <localRoot>: String |");
		System.out.println("|\t     Mount the directory (<remoteRoot>) on the given server (<server>) with the given          |");
		System.out.println("|\t     user id (<uid>) and group id (<gid>) credentials and <localRoot> as shared                |");
		System.out.println("|\t     directory.                                                                                |");
		System.out.println("|                                                                                                      |");
		System.out.println("|\t - list                                                                                        |");
		System.out.println("|\t     List all servers and their mounts                                                         |");
		System.out.println("|                                                                                                      |");
		System.out.println("|\t - replicate <server> IP <uid>:int <gid>:int <remoteRoot>: String                              |");
		System.out.println("|\t     Replicate the operations on the given server.                                             |");
		System.out.println("|                                                                                                      |");
		System.out.println("|\t - openMount <server>:IP                                                                       |");
		System.out.println("|\t     Open the command line for the given server                                                |");
		System.out.println("|                                                                                                      |");
		System.out.println("|\t - help                                                                                        |");
		System.out.println("|                                                                                                      |");
		System.out.println("|\t - quit                                                                                        |");
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	}

	/**
	 * Process the given command.
	 * 
	 * @param cmd
	 * @return
	 */
	private boolean processCmd(String cmd) {
		String[] args = cmd.split(" ");

		// process quit
		if (args[0].equals("quit")) {
			System.out.println("Quitting nfs client...");
			return true;
		}

		// process help
		if (args[0].equals("help")) {
			printWelcome();
			return false;
		}

		// process mount
		if (args[0].equals("mount")) {
			processMount(args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), args[4], args[5]);
			return false;
		}
		
		// process replicate
		if (args[0].equals("replicate")) {
			processReplicate(args[1], args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]), args[5], args[6]);
			return false;
		}

		// process list
		if (args[0].equals("list")) {
			processList();
			return false;
		}

		// process openMount
		if (args[0].equals("openMount")) {
			processOpenMount(args[1]);
			return false;

		}
		
		if (args[0].equals("debugReplica")) {
			processMount("35.161.84.168", 50, 1000, "/exports", "/Users/usi/Desktop/watcherTest");
			processReplicate("35.161.84.168", "212.47.245.249", 50, 1000, "/exports", "/Users/usi/Desktop/watcherTest" );
			processOpenMount("35.161.84.168");
			return false;

		}
		
		if (args[0].equals("debug")) {
			processMount("35.161.84.168", 50, 1000, "/exports", "/Users/usi/Desktop/watcherTest");
			processOpenMount("35.161.84.168");
			return false;

		}

		// the given command does not exist
		System.out.println("> Command not recognized!");
		return false;

	}

	/**
	 * Process the mount command. It create a server interface containing a
	 * server handler and put it in the servers list.
	 * 
	 * @param server
	 * @param uid
	 * @param gid
	 */
	private void processMount(String server, int uid, int gid, String rootDir, String localDir) {
		mountDir = rootDir;
		sharedFolder = localDir;
		try {
			if(sharedFolder != null) {
				servers.put(server, new ServerInferface(server, sharedFolder, mountDir, uid, gid));
			} else {
				System.out.println("> No shared directory!");
			}
		} catch (IOException | OncRpcException | MountException e) {
			e.printStackTrace();
		}
	}
	
	private void processReplicate(String server, String replica, int uid, int gid, String rootDir, String localDir) {
		try {
			Observer r = new Replicator(new ServerHandler(replica, rootDir, uid, gid, new PathResolver(localDir, rootDir), true));
			servers.get(server).addObserver(r);
		} catch (IOException | OncRpcException | MountException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Process the list command.
	 * 
	 */
	private void processList() {
		System.out.println("> List of all servers: ");
		for (Entry<String, ServerInferface> entry : servers.entrySet()) {
			System.out.println(">   " + entry.getKey());
		}
	}

	/**
	 * Process the openMount command. It will get the given ServerInterface and
	 * run its UI.
	 * 
	 * @param server
	 */
	private void processOpenMount(String server) {
		servers.get(server).runUI();
	}
}
