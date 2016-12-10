package DistributedSystemCourse.NFSClient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer;
import java.util.Scanner;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import org.acplt.oncrpc.OncRpcException;
import org.javatuples.Pair;

import DistributedSystemCourse.Library.nfs.entry;
import DistributedSystemCourse.Library.nfs.fattr;

public class ServerInferface {
	private Scanner scanner;
	private ServerHandler handler;
	private LocalSynchronizer synch;
	private Thread localSynchThread;
	private Path localRoot;
	private PathResolver pr;
	private String currentDir;
	
	public ServerInferface(String server, String sharedDir, String mountDir, int uid, int gid) throws IOException, OncRpcException, MountException {
		pr = new PathResolver(sharedDir, mountDir);
		handler = new ServerHandler(server, mountDir, uid, gid, pr, false);
		scanner = new Scanner(System.in);
		watchDirectory(sharedDir);
		localRoot = Paths.get(sharedDir);
		currentDir = mountDir;
	}
	
	private Path resolvePath(String path) {
		return localRoot.resolve(path);
	}
	public ServerHandler getHandler() {
		return handler;
	}
		
	private void watchDirectory(String dir) throws IOException {
		synch = new LocalSynchronizer(dir, handler);
		localSynchThread = new Thread(synch);
		localSynchThread.start();
	}
	
	public void addObserver(Observer o) {
		handler.addObserver(o);
	}
	public void runUI() {
		boolean quit = false;
		printServerUI();
		String cmd = null;
		while(!quit) {
			System.out.print(currentDir + " >> ");
			
			cmd = scanner.nextLine();
			
			quit = processCmd(cmd);
		}
	}
	
	private boolean processCmd(String cmd) {
		String[] args = cmd.split(" ");
		
		if(args[0].equals("createDir")) {
			try {
				if(args.length == 3) {
					processCreateDir(args[1], true);
				} else {
					processCreateDir(args[1], false);
				}
			} catch (OncRpcException | IOException | NFSOperationException e) {
//				System.err.println(e.getMessage());
				e.printStackTrace();
			}
			
			return false;
		}
		
		if(args[0].equals("listDir")) {
			try {
				List<entry> dirContent;
				if(args.length <= 1) {
					dirContent = handler.processListDir(null);
				} else {
					dirContent = handler.processListDir(args[1]);
				}
				for(entry e: dirContent) {
					if(e != null) {
						System.out.println(e.name.value);
					}
				}
			} catch (OncRpcException | IOException | NFSOperationException e) {
				System.err.println(e.getMessage());
			}
			
			return false;
		}
				
		if(args[0].equals("createFile")) {
			try {
				if(args.length == 3) {
					processCreateFile(args[1], true);
				} else {
					processCreateFile(args[1], false);
				}
			} catch (OncRpcException | IOException | NFSOperationException e) {
				System.err.println(e.getMessage());
			}
			
			return false;
		}
		
		if(args[0].equals("readFile")) {
			try {
				processReadFile(args[1]);
			} catch (OncRpcException | IOException | NFSOperationException e) {
				System.err.println(e.getMessage());
			}
			
			return false;
		}
		
		if(args[0].equals("writeFile")) {
			try {
				String content = "";
				for(int i = 2; i < args.length; i++) {
					content += args[i] + " ";
				}
				if(args.length == 3) {
					processWriteFile(args[1], content, true);
				} else {
					processWriteFile(args[1], content, false);
				}
			} catch (OncRpcException | IOException | NFSOperationException e) {
				System.err.println(e.getMessage());
			}
			
			return false;
		}
		
		if(args[0].equals("setFileAttrs")) {
			try {
				processSetFileAttrs(args[1]);
			} catch (OncRpcException | IOException | NFSOperationException e) {
				System.err.println(e.getMessage());
			}
			
			return false;
		}
		
		if(args[0].equals("readFileAttrs")) {
			try {
				processReadFileAttrs(args[1]);
			} catch (OncRpcException | IOException | NFSOperationException e) {
				System.err.println(e.getMessage());
			}
			return false;
		}
		
		if(args[0].equals("restore")) {
			processRestore(args[1]);
			return false;
		}
		
		if(args[0].equals("remove")) {
			try {
				if(args.length == 3) {
					processRemove(args[1], true);
				} else {
					processRemove(args[1], false);
				}
			} catch (OncRpcException | IOException | NFSOperationException e) {
				System.err.println(e.getMessage());
			}
		
			return false;
		}
		
		if(args[0].equals("cd")) {
			try {
				processCd(args[1]);
			} catch (OncRpcException | IOException | NFSOperationException e) {
				System.err.println(e.getMessage());
			}
			
			return false;
		}
		
		if(args[0].equals("help")) {
			printServerUI();
			return false;
		}
		
		if(args[0].equals("quit")) {
			System.out.println("Quitting server interface...");
			synch.terminate();
			return true;
		}
		
		System.err.println("Command not recognized!");
		return false;
	}
	
	private void processWriteFile(String path, String content, boolean noLocal) throws IllegalArgumentException, OncRpcException, IOException, NFSOperationException {
		Path p = Paths.get(path);
		if(!pr.isLocalAbsolute(p)) {
			p = pr.relativizeRemote(Paths.get(currentDir).resolve(p));
			p = pr.resolveLocal(p);
		}
		
		if(noLocal) {
			handler.processWriteFile(p.toString(), content);
		} else {
			Files.write(p, content.getBytes());
		}
	}
	
	private void processReadFile(String path) throws OncRpcException, IOException, NFSOperationException {
		byte[] content = handler.processReadFile(path);
		System.out.println(new String(content));
	}
	
	private void processCd(String path) throws OncRpcException, IOException, NFSOperationException {
		currentDir = handler.processCd(path);
	}
	
	private void processSetFileAttrs(String path) throws OncRpcException, IOException, NFSOperationException {
		Path p = Paths.get(path);
		if(!pr.isLocalAbsolute(p)) {
			p = pr.relativizeRemote(Paths.get(currentDir).resolve(p));
			p = pr.resolveLocal(p);
		}
		
		handler.processSetFileAttrs(p.toString());
	}
	
	private void processReadFileAttrs(String path) throws OncRpcException, IOException, NFSOperationException {
		fattr attrs = handler.processReadFileAttrs(path);
		System.out.println("type: " + attrs.type);
		System.out.println("mode: " + attrs.mode);
		System.out.println("nLink: " + attrs.nlink);
		System.out.println("uid: " + attrs.uid);
		System.out.println("gid: " + attrs.gid);
		System.out.println("size: " + attrs.size);
		System.out.println("blockSize: " + attrs.blocksize);
		System.out.println("rDev: " + attrs.rdev);
		System.out.println("blocks: " + attrs.blocks);
		System.out.println("fsid: " + attrs.fsid);
		System.out.println("fileid: " + attrs.fileid);
		System.out.println("atime: " + attrs.atime.seconds + " s, " + attrs.atime.useconds + " us");
		System.out.println("mtime: " + attrs.mtime.seconds + " s, " + attrs.mtime.useconds + " us");
		System.out.println("ctime: " + attrs.ctime.seconds + " s, " + attrs.ctime.useconds + " us");
	}
	
	private void processCreateFile(String path, boolean noLocal) throws OncRpcException, IOException, NFSOperationException {
		Path p = Paths.get(path);
		if(!pr.isLocalAbsolute(p)) {
			p = pr.relativizeRemote(Paths.get(currentDir).resolve(p));
			p = pr.resolveLocal(p);
		}
		
		if(noLocal) {
			handler.processCreateFile(p.toString());
		} else {
			Files.createFile(p);
		}
	}
	
	private void processCreateDir(String path, boolean noLocal) throws OncRpcException, IOException, NFSOperationException {
		Path p = Paths.get(path);
		if(!pr.isLocalAbsolute(p)) {
			p = pr.relativizeRemote(Paths.get(currentDir).resolve(p));
			p = pr.resolveLocal(p);
		}
		
		if(noLocal) {
			handler.processCreateDir(p.toString());
		} else {
			Files.createDirectory(p);
		}
	}
	
	private void processRemove(String path, boolean noLocal) throws OncRpcException, IOException, NFSOperationException {
		Path p = Paths.get(path);
		
		if(!pr.isLocalAbsolute(p)) {
			p = pr.relativizeRemote(Paths.get(currentDir).resolve(p));
			p = pr.resolveLocal(p);
		}
		
		if(Files.exists(p)) {
			if(Files.isDirectory(p)) {
				if(p.toFile().list().length == 0) {
					if(noLocal) {
						handler.processRemoveDir(p.toString());
					} else {
						Files.delete(p);
					}
				} else {
					if(noLocal) {
						handler.processRemoveDir(p.toString());
					} else {System.out.println("AA");
						deleteRec(p);
					}
				}
			} else {
				if(noLocal) {
					handler.processRemoveFile(p.toString());
				} else {
					Files.delete(p);
				}
			}
		} else {
			handler.remove(pr.relativizeLocal(p).toString());
		}
		
	}
	
	//FIXME
	private void processRestore(String path) {
		Path p = Paths.get(path);
		
		if(!pr.isLocalAbsolute(p)) {
			p = pr.relativizeRemote(Paths.get(currentDir).resolve(p));
			p = pr.resolveLocal(p);
		}
		
		if(Files.exists(p)) {
			if(Files.isDirectory(p)) {
			} else {
				try {
					byte[] content = handler.processReadFile(path);
					fattr attrs = handler.processReadFileAttrs(path);
					System.out.println(new String(content));
					Charset charset = Charset.forName("US-ASCII");
					try (BufferedWriter w = Files.newBufferedWriter(p, charset)) {
						PrintWriter writer = new PrintWriter(w);
						writer.print(new String(content));
						
						BasicFileAttributeView fattrs = Files.getFileAttributeView(p, BasicFileAttributeView.class);
						FileTime mtime = FileTime.fromMillis(attrs.mtime.seconds*1000 + attrs.mtime.useconds/1000);
						FileTime atime = FileTime.fromMillis(attrs.atime.seconds*1000 + attrs.atime.useconds/1000);
						FileTime ctime = FileTime.fromMillis(attrs.ctime.seconds*1000 + attrs.ctime.useconds/1000);
						fattrs.setTimes(mtime, atime, ctime);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (OncRpcException | IOException | NFSOperationException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("File does not exist.");
		}
	}
	
	private void deleteRec(Path root) throws IOException {
		if(!Files.isDirectory(root)) {
			Files.delete(root);
		} else {
			String[] content = root.toFile().list();
			for(String file: content) {
				deleteRec(root.resolve(Paths.get(file)));
			}
			
			Files.delete(root);
		}
	}
	
	private void printServerUI() {
		System.out.println("+++++++++++++++++++++++++++++++ Server interface +++++++++++++++++++++++++++++++");
		System.out.println("|                                                                              |");
		System.out.println("| Server: " + handler.getServerAddress());
		System.out.println("| Mount: " + handler.getRoot());
		System.out.println("| Local root: " + localRoot);
		System.out.println("|                                                                              |");
		System.out.println("| Commands:                                                                    |");
		System.out.println("|\t - cd <path>                                                           |");
		System.out.println("|\t     Enter the directory at the given path                             |");
		System.out.println("|\t - createDir <path>                                                    |");
		System.out.println("|\t     Create a directory at the given path                              |");
		System.out.println("|\t - listDir <path>                                                      |");
		System.out.println("|\t     List the content of the directory at the given path               |");
		System.out.println("|\t - createFile <path>                                                   |");
		System.out.println("|\t     Create a file at the given path                                   |");
		System.out.println("|\t - readFile <path>                                                     |");
		System.out.println("|\t     Read content of the file at the given path                        |");
		System.out.println("|\t - writeFile <path> <content>                                          |");
		System.out.println("|\t     Write the given content into the file at the given path           |");
		System.out.println("|\t - setFileAttrs <path>                                                 |");
		System.out.println("|\t     Set attributes to the file at the given path                      |");
		System.out.println("|\t - readFileAttrs <path>                                                |");
		System.out.println("|\t     Read attributes to the file at the given path                     |");
		System.out.println("|\t - restore <path>                                                      |");
		System.out.println("|\t     Restore the file at the given path from the remote file system    |");
		System.out.println("|\t - help                                                                |");
		System.out.println("|\t - quit                						       |");
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

	}
}