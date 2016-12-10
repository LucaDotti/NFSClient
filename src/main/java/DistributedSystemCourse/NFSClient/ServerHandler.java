package DistributedSystemCourse.NFSClient;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.TimeUnit;

import org.acplt.oncrpc.OncRpcClientAuthUnix;
import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.OncRpcProtocols;

import DistributedSystemCourse.Library.mount.dirpath;
import DistributedSystemCourse.Library.mount.fhstatus;
import DistributedSystemCourse.Library.mount.mountDefClient;
import DistributedSystemCourse.Library.nfs.attrstat;
import DistributedSystemCourse.Library.nfs.createargs;
import DistributedSystemCourse.Library.nfs.diropargs;
import DistributedSystemCourse.Library.nfs.diropres;
import DistributedSystemCourse.Library.nfs.entry;
import DistributedSystemCourse.Library.nfs.fattr;
import DistributedSystemCourse.Library.nfs.fhandle;
import DistributedSystemCourse.Library.nfs.filename;
import DistributedSystemCourse.Library.nfs.ftype;
import DistributedSystemCourse.Library.nfs.nfsDefClient;
import DistributedSystemCourse.Library.nfs.nfscookie;
import DistributedSystemCourse.Library.nfs.nfsdata;
import DistributedSystemCourse.Library.nfs.readargs;
import DistributedSystemCourse.Library.nfs.readdirargs;
import DistributedSystemCourse.Library.nfs.readdirres;
import DistributedSystemCourse.Library.nfs.readres;
import DistributedSystemCourse.Library.nfs.sattr;
import DistributedSystemCourse.Library.nfs.sattrargs;
import DistributedSystemCourse.Library.nfs.stat;
import DistributedSystemCourse.Library.nfs.timeval;
import DistributedSystemCourse.Library.nfs.writeargs;

public class ServerHandler extends Observable{
	//the server address
	private InetAddress server;
	//the file handle of the remote root
	private fhandle fhRoot;
	//the nfsClient that performs the requests
	private nfsDefClient nfsClient;
	//the mountClient
	private mountDefClient mountClient;
	//the authenticationClient
	private OncRpcClientAuthUnix authClient;
	// TODO ask for these info
	private int uid;
	private int gid;
	//the string path to the root
	private Path root;
	//the path resolver to manipulate the paths
	private PathResolver pr;
	//current working directory
	private fhandle currentDir;
	private Path currentDirPath;
	private boolean isReplica;

	/**
	 * Constructor for ServerHandler
	 * 
	 * @param server IP
	 * @param mountDir remoteRoot
	 * @param uid 
	 * @param gid
	 * @param pr PathResolver
	 * 
	 * @throws IOException
	 * @throws OncRpcException
	 * @throws MountException
	 */
	public ServerHandler(String server, String mountDir, int uid, int gid, PathResolver pr, boolean isReplica)
			throws IOException, OncRpcException, MountException {
		this.isReplica = isReplica;
		this.server = InetAddress.getByName(server);
		this.uid = uid;
		this.gid = gid;
		this.pr = pr;

		//mount the remote directory
		mount(mountDir);
		//get the authentication client
		authClient = new OncRpcClientAuthUnix(getServerName(), uid, gid);
		//get the nfs client
		nfsClient = new nfsDefClient(this.server, OncRpcProtocols.ONCRPC_TCP);
		//set the authentication
		nfsClient.getClient().setAuth(authClient);
		setChanged();
	}

	/**
	 * Get the server address.
	 * 
	 * @return address
	 */
	public String getServerAddress() {
		return server.getHostAddress();
	}

	/**
	 * Get server name.
	 * 
	 * @return name
	 */
	public String getServerName() {
		return server.getHostName();
	}

	/**
	 * Get the string of the root.
	 * 
	 * @return root
	 */
	public String getRoot() {
		return root.toString();
	}
	
	/**
	 * Mount the given remote directory.
	 * 
	 * @param dir
	 * @throws OncRpcException
	 * @throws IOException
	 * @throws MountException
	 */
	public void mount(String dir) throws OncRpcException, IOException, MountException {
		root = Paths.get(dir);
		//get the mountClient
		mountClient = new mountDefClient(this.server, OncRpcProtocols.ONCRPC_TCP);
		//mount the directory
		fhstatus status = mountClient.MOUNTPROC_MNT_1(new dirpath(dir));
		//check that the mount is ok
		if (status.status == stat.NFS_OK) {
			//get the root file handler
			fhRoot = new fhandle(status.directory.value);
			currentDir = fhRoot;
			currentDirPath = root;
			//set the authenticationClient to the mountClient
			mountClient.getClient().setAuth(authClient);
			if(!isReplica) {
				notifyObservers(new NFSEvent(dir, "mount", null));
			}
		} else {
			String msg = "Something went wrong while mounting file.";
			if(isReplica) {
				msg = "Replica: " + msg;
			} 
			
			throw new MountException(msg);
		}
	}

	/**
	 * Get the filehandle of the given file. Assume that the path is either remote absolute (/exports/path-to-file) or
	 * relative to the remote root (/path-to-file).
	 * 
	 * @param path
	 * @return
	 * @throws OncRpcException
	 * @throws IOException
	 * @throws NFSOperationException
	 */
	private fhandle getFileHandle(fhandle from, Path path) throws OncRpcException, IOException, NFSOperationException {
		//if the path is the root, return the root filehandle
		if (root.equals(path)) {
			return fhRoot;
		}
		
		fhandle currDir;
		Path currPath;
		Iterator<Path> it = path.iterator();
		Path absRemoteDir;
		if(pr.isRemoteAbsolute(path)) {
			// skip root: we already have the fhandle for that
			if (it.hasNext()) {
				it.next();
			} 
			//path of the current directory
			currPath = root;
			absRemoteDir = path;
		} else {
			//path of the current directory
			currPath = currentDirPath;
			absRemoteDir = currentDirPath.resolve(path);
		}
		
		currDir = from;
		
		while (it.hasNext()) {
			Path currPathElem = (Path) it.next();
			
			diropargs doa = new diropargs();
			doa.dir = currDir;
			doa.name = new filename(currPathElem.toString());
			
			//lookup filehandle
			diropres result = nfsClient.NFSPROC_LOOKUP_2(doa);
			
			currPath = currPath.resolve(currPathElem);
			
			//check result status
			if (result.status == stat.NFS_OK) {
				//if the current path is equal to the given one: found the file handle -> return it
				//else go to next file
				if (currPath.equals(absRemoteDir)) {
					return result.diropok.file;
				} else {
					currDir = result.diropok.file;
				}
			} else {
				String msg = "Something went wrong while getting the file handle for file " + currPath.toString() 
					+ ".\n Server response: " + StatusParser.parse(result.status);
				if(isReplica) {
					msg = "Replica: " + msg;
				} 
			
				throw new NFSOperationException(msg);	
			}
			
		}
		throw new NFSOperationException("The file handle for file " + path.toString() + " does not exist.");
	}

	/**
	 * Get the content of the given directory.
	 * 
	 * @param path
	 * @return
	 * @throws OncRpcException
	 * @throws IOException
	 * @throws NFSOperationException
	 */
	public List<entry> processListDir(String path) throws OncRpcException, IOException, NFSOperationException {
		Path p;
		//if no path is given, list the root
		if(path == null) {
			p = currentDirPath;
		} else {
			p = Paths.get(path);
		}
		
		List<entry> dirContent = new ArrayList<>();
		fhandle fh;
		
		if(pr.isRemoteAbsolute(p)) {
			fh = getFileHandle(fhRoot, p);
		} else {
			fh = getFileHandle(currentDir, p);
		}
		
		// zero cookie to get entries starting at the beginning of the directory
		byte[] zeroCookie = {0, 0, 0, 0};
		// readdirargs struct to call READDIR procedure
		readdirargs rda = new readdirargs();
		rda.dir = fh;
		rda.cookie = new nfscookie(zeroCookie);
		rda.count = 1000;

		// get the result
		readdirres result = nfsClient.NFSPROC_READDIR_2(rda);

		// if the result is ok, extract the entries and add them in the
		// dirContent list, else throw a NFSOperationException
		if (result.status == stat.NFS_OK) {
			entry currEntry = result.readdirok.entries;
			while (currEntry != null) {
				currEntry = currEntry.nextentry;
				dirContent.add(currEntry);
			}
			return dirContent;
		} else {
			throw new NFSOperationException("Something went wrong while listing directory " + path.toString() 
				+ ".\n Server response: " + StatusParser.parse(result.status));
		}
	}

	/**
	 * Set the attributes to the given remote file.
	 * 
	 * @param path
	 * @throws OncRpcException
	 * @throws IOException
	 * @throws NFSOperationException
	 */
	public void processSetFileAttrs(String path) throws OncRpcException, IOException, NFSOperationException {
		Path p = Paths.get(path);
		
		//absolute local path "/path-to-shared-directory/..."
		Path absRemoteDir;
		Path absLocalDir;
		
		fhandle fh;
		
		if(pr.isRemoteAbsolute(p)) {
			fh = getFileHandle(fhRoot, p);
			absRemoteDir = p;
		} else {
			fh = getFileHandle(currentDir, p);
			absRemoteDir = currentDirPath.resolve(p);
		}
		
		absLocalDir = pr.resolveLocal(pr.relativizeRemote(absRemoteDir));
		
		sattrargs saa = new sattrargs();
		saa.file = fh;

		sattr sa = new sattr();
		//fill the file attributes read from the local file
		fillFileAttrbiutes(sa, absLocalDir, isDirectory(absRemoteDir.toString()));

		saa.attributes = sa;

		//set attribute request
		attrstat result = nfsClient.NFSPROC_SETATTR_2(saa);

		if (result.status == stat.NFS_OK) {
			System.out.println("Successfully set attributes of file " + absRemoteDir.toString());
			notifyObservers(new NFSEvent(path, "setFileAttrs", null));
		} else {
			String msg = "Something went wrong while setting attributes to file " + absRemoteDir.toString()
				+ ".\n Server response: " + StatusParser.parse(result.status);
			if(isReplica) {
				msg = "Replica: " + msg;
			}
			throw new NFSOperationException(msg);
		}
	}

	/**
	 * Read file attributes from the given remote file.
	 * 
	 * @param path
	 * @return attributes
	 * @throws OncRpcException
	 * @throws IOException
	 * @throws NFSOperationException
	 */
	public fattr processReadFileAttrs(String path) throws OncRpcException, IOException, NFSOperationException {
		Path p = Paths.get(path);
		Path absRemoteDir;
		fhandle fh;
		
		if(pr.isRemoteAbsolute(p)) {
			fh = getFileHandle(fhRoot, p);
			absRemoteDir = p;
		} else {
			fh = getFileHandle(currentDir, p);
			absRemoteDir = currentDirPath.resolve(p);
		}
		
		attrstat result = nfsClient.NFSPROC_GETATTR_2(fh);

		if (result.status == stat.NFS_OK) {
			return result.attributes;
		} else {
			throw new NFSOperationException("Something went wrong while reading attributes of file " + absRemoteDir.toString()
				+ ".\n Server response: " + StatusParser.parse(result.status));
		}
	}

	/**
	 * Fill the struct attrs with the attributes of the given file.
	 * 
	 * @param attrs
	 * @param file
	 * @throws IOException
	 */
	private void fillFileAttrbiutes(sattr attrs, Path file, boolean isDirectory) throws IOException {
		if(Files.exists(file)) {
			// http://stackoverflow.com/questions/10824027/get-the-metadata-of-a-file
			// https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#getAttribute(java.nio.file.Path,%20java.lang.String,%20java.nio.file.LinkOption...)
			// does this exists?
			//add mode
			attrs.mode = (int) Files.getAttribute(file, "unix:mode");
			//add uid
			attrs.uid = uid;
			//add gid
			attrs.gid = gid;
			//add size
			attrs.size = (int) file.toFile().length();
			
			BasicFileAttributes fileAttrs = Files.readAttributes(file, BasicFileAttributes.class);
			
			//add access time
			timeval access = new timeval();
			access.seconds = (int) fileAttrs.lastAccessTime().to(TimeUnit.SECONDS);
			access.useconds = (int) fileAttrs.lastAccessTime().to(TimeUnit.MICROSECONDS);
			attrs.atime = access;
			//add modification time
			timeval mod = new timeval();
			mod.seconds = (int) fileAttrs.lastModifiedTime().to(TimeUnit.SECONDS);
			mod.useconds = (int) fileAttrs.lastModifiedTime().to(TimeUnit.MICROSECONDS);
			attrs.mtime = mod;
			
		} else {
			if(isDirectory ) {
				attrs.mode = 16877;
			} else {
				attrs.mode = 33188;
			}
			
			attrs.uid = uid;
			attrs.gid = gid;
			attrs.size = (int) file.toFile().length();
			timeval access = new timeval();
			access.seconds = (int) System.currentTimeMillis()/1000;
			access.useconds = 0;
			attrs.atime = access;
			//add modification time
			timeval mod = new timeval();
			mod.seconds = (int) System.currentTimeMillis()/1000;
			mod.useconds = 0;
			attrs.mtime = mod;
		}
		
	}

	/**
	 * Create directory with the given name at the given path.
	 * 
	 * @param path
	 * @param fileName
	 * @throws NFSOperationException
	 * @throws IOException
	 * @throws OncRpcException
	 */
	public void processCreateDir(String path) throws OncRpcException, IOException, NFSOperationException {
		Path p = Paths.get(path);
		
		fhandle fh;
		Path absRemoteDir;
		Path absLocalDir;
		Path absRemotePathToDir;
		if(pr.isRemoteAbsolute(p)) {
			absRemoteDir = p;
			absLocalDir = pr.resolveLocal(pr.relativizeRemote(absRemoteDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(fhRoot, absRemotePathToDir);
		} else if(pr.isLocalAbsolute(p)){
			absLocalDir = p;
			absRemoteDir = pr.resolveRemote(pr.relativizeLocal(absLocalDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(fhRoot, absRemotePathToDir);
		} else {
			absRemoteDir = currentDirPath.resolve(p);
			absLocalDir = pr.resolveLocal(pr.relativizeRemote(absRemoteDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(currentDir, absRemotePathToDir);
		}
		
		Path fileName = p.getFileName();
		
		diropargs doa = new diropargs();
		doa.dir = fh;
		doa.name = new filename(fileName.toString());

		sattr attrs = new sattr();
		
		//fill the attributes with the local directory attributes
		fillFileAttrbiutes(attrs, absLocalDir, true);

		createargs ca = new createargs();
		ca.where = doa;
		ca.attributes = attrs;
		diropres result = nfsClient.NFSPROC_MKDIR_2(ca);

		if (result.status == stat.NFS_OK) {
			if(!isReplica) {
				System.out.println("Created directory " + absRemoteDir.toString());
				notifyObservers(new NFSEvent(path, "createDir", null));
			}
		} else {
			String msg = "Something went wrong while creating directory " + absRemoteDir.toString() 
				+ ".\n Server response: " + StatusParser.parse(result.status);
			if(isReplica) {
				msg = "Replica: " + msg;
			}
			throw new NFSOperationException(msg);
		}
	}
	
	/**
	 * Remove the given remote file.
	 * 
	 * @param path
	 * @throws OncRpcException
	 * @throws IOException
	 * @throws NFSOperationException
	 */
	public void processRemoveDir(String path) throws OncRpcException, IOException, NFSOperationException {
		Path p = Paths.get(path);
		
		fhandle fh;
		Path absRemoteDir;
		Path absLocalDir;
		Path absRemotePathToDir;
		
		if(pr.isRemoteAbsolute(p)) {
			absRemoteDir = p;
			absLocalDir = pr.resolveLocal(pr.relativizeRemote(absRemoteDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(fhRoot, absRemotePathToDir);
		} else if(pr.isLocalAbsolute(p)){
			absLocalDir = p;
			absRemoteDir = pr.resolveRemote(pr.relativizeLocal(absLocalDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(fhRoot, absRemotePathToDir);
		} else {
			absRemoteDir = currentDirPath.resolve(p);
			absLocalDir = pr.resolveLocal(pr.relativizeRemote(absRemoteDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(currentDir, absRemotePathToDir);
		}

		Path fileName = p.getFileName();
		
		diropargs doa = new diropargs();
		doa.dir = fh;
		doa.name = new filename(fileName.toString());
		
		int result = nfsClient.NFSPROC_RMDIR_2(doa);
		
		if(result == stat.NFS_OK) {
			if(!isReplica) {
				System.out.println("Removed file " + absRemoteDir.toString());
				notifyObservers(new NFSEvent(path, "remove", null));
			}
		} else {
			String msg = "Something went wrong while removing directory " + absRemoteDir.toString() 
				+ ".\nServer response: " + StatusParser.parse(result);
			if(isReplica) {
				msg = "Replica: " + msg;
			}
			throw new NFSOperationException(msg);
		}
	}
	
	/**
	 * Remove the given remote file.
	 * 
	 * @param path
	 * @throws OncRpcException
	 * @throws IOException
	 * @throws NFSOperationException
	 */
	public void processRemoveFile(String path) throws OncRpcException, IOException, NFSOperationException {
		Path p = Paths.get(path);
		
		fhandle fh;
		Path absRemoteDir;
		Path absLocalDir;
		Path absRemotePathToDir;
		
		if(pr.isRemoteAbsolute(p)) {
			absRemoteDir = p;
			absLocalDir = pr.resolveLocal(pr.relativizeRemote(absRemoteDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(fhRoot, absRemotePathToDir);
		} else if(pr.isLocalAbsolute(p)){
			absLocalDir = p;
			absRemoteDir = pr.resolveRemote(pr.relativizeLocal(absLocalDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(fhRoot, absRemotePathToDir);
		} else {
			absRemoteDir = currentDirPath.resolve(p);
			absLocalDir = pr.resolveLocal(pr.relativizeRemote(absRemoteDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(currentDir, absRemotePathToDir);
		}

		Path fileName = p.getFileName();
		
		diropargs doa = new diropargs();
		doa.dir = fh;
		doa.name = new filename(fileName.toString());
		
		int result = nfsClient.NFSPROC_REMOVE_2(doa);
		
		if(result == stat.NFS_OK) {
			if(!isReplica) {
				System.out.println("Removed file " + absRemoteDir.toString());
				notifyObservers(new NFSEvent(path, "remove", null));
			}
		} else {
			String msg = "Something went wrong while removing file " + absRemoteDir.toString() 
				+ ".\nServer response: " + StatusParser.parse(result);
			if(isReplica) {
				msg = "Replica: " + msg;
			}
			throw new NFSOperationException(msg);
		}
	}
	
	/**
	 * Create the remote file.
	 * 
	 * @param path
	 * @throws OncRpcException
	 * @throws IOException
	 * @throws NFSOperationException
	 */
	public void processCreateFile(String path) throws OncRpcException, IOException, NFSOperationException {
		Path p = Paths.get(path);
		
		fhandle fh;
		Path absRemoteDir;
		Path absLocalDir;
		Path absRemotePathToDir;
		if(pr.isRemoteAbsolute(p)) {
			absRemoteDir = p;
			absLocalDir = pr.resolveLocal(pr.relativizeRemote(absRemoteDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(fhRoot, absRemotePathToDir);
		} else if(pr.isLocalAbsolute(p)){
			absLocalDir = p;
			absRemoteDir = pr.resolveRemote(pr.relativizeLocal(absLocalDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(fhRoot, absRemotePathToDir);
		} else {
			absRemoteDir = currentDirPath.resolve(p);
			absLocalDir = pr.resolveLocal(pr.relativizeRemote(absRemoteDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(currentDir, absRemotePathToDir);
		}

		Path fileName = p.getFileName();

		diropargs doa = new diropargs();
		doa.dir = fh;
		doa.name = new filename(fileName.toString());

		sattr attrs = new sattr();

		fillFileAttrbiutes(attrs, absLocalDir, false);

		createargs ca = new createargs();
		ca.where = doa;
		ca.attributes = attrs;

		diropres result = nfsClient.NFSPROC_CREATE_2(ca);

		if (result.status == stat.NFS_OK) {
			if(!isReplica) {
				System.out.println("Created file " + absRemoteDir.toString());
				notifyObservers(new NFSEvent(path, "createFile", null));
			}
		} else {
			String msg = "Something went wrong while creating file " + absRemoteDir.toString()
				+ ".\n Server response: " + StatusParser.parse(result.status);
			if(isReplica) {
				msg = "Replica: " + msg;
			}
			throw new NFSOperationException(msg);
		}
	}

	/**
	 * Read the remote file.
	 * 
	 * @param path
	 * @return content
	 * @throws OncRpcException
	 * @throws IOException
	 * @throws NFSOperationException
	 */
	public byte[] processReadFile(String path) throws OncRpcException, IOException, NFSOperationException {
		Path p = Paths.get(path);
		
		fhandle fh;
		Path absRemoteDir;
		if(pr.isRemoteAbsolute(p)) {
			fh = getFileHandle(fhRoot, p);
			absRemoteDir = p;
		} else {
			fh = getFileHandle(currentDir, p);
			absRemoteDir = currentDirPath.resolve(p);
		}
		
		readargs ra = new readargs();
		ra.file = fh;
		ra.count = getSize(absRemoteDir.toString());
		ra.offset = 0;

		readres result = nfsClient.NFSPROC_READ_2(ra);

		if (result.status == stat.NFS_OK) {
			return result.readRes.data.value;
		} else {
			throw new NFSOperationException("Something went wrong while reading file " + absRemoteDir.toString()
				+ ".\n Server response: " + StatusParser.parse(result.status));
		}
	}

	/**
	 * Write to the remote file.
	 * 
	 * @param path
	 * @param content
	 * @throws OncRpcException
	 * @throws IOException
	 * @throws NFSOperationException
	 * @throws IllegalArgumentException
	 */
	public void processWriteFile(String path, String content) throws OncRpcException, IOException, NFSOperationException, IllegalArgumentException {
		Path p = Paths.get(path);
		
		fhandle fh;
		Path absRemoteDir;
		Path absLocalDir;
		Path absRemotePathToDir;
		if(pr.isRemoteAbsolute(p)) {
			absRemoteDir = p;
			absLocalDir = pr.resolveLocal(pr.relativizeRemote(absRemoteDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(fhRoot, absRemotePathToDir);
		} else if(pr.isLocalAbsolute(p)){
			absLocalDir = p;
			absRemoteDir = pr.resolveRemote(pr.relativizeLocal(absLocalDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(fhRoot, absRemotePathToDir);
		} else {
			absRemoteDir = currentDirPath.resolve(p);
			absLocalDir = pr.resolveLocal(pr.relativizeRemote(absRemoteDir));
			absRemotePathToDir = absRemoteDir.getParent();
			fh = getFileHandle(currentDir, absRemotePathToDir);
		}
						
		nfsdata data = new nfsdata();
		
		writeargs wa = new writeargs();
		wa.file = fh;
		wa.offset = 0;
		
		data.value = content.getBytes();
		wa.data = data;
		
		attrstat result = nfsClient.NFSPROC_WRITE_2(wa);

		if (result.status == stat.NFS_OK) {
			if(!isReplica) {
				System.out.println("Successfully wrote to file " + absRemoteDir.toString());
				notifyObservers(new NFSEvent(path, "writeFile", content));
			}
		} else {
			String msg = "Something went wrong while writing to file " + absRemoteDir.toString()
				+ ".\n Server response: " + StatusParser.parse(result.status);
			if(isReplica) {
				msg = "Replica: " + msg;
			}
			throw new NFSOperationException(msg);
		}
	}
		
	public String processCd(String path) throws OncRpcException, IOException, NFSOperationException {
		Path p = Paths.get(path);
		
		if(path.equals("..")) {
			if(!currentDirPath.equals(root)) {
				fhandle fh = getFileHandle(fhRoot, currentDirPath.getParent());
				currentDir = fh;
				currentDirPath = currentDirPath.getParent();
			} 
			return currentDirPath.toString();
		} else if(pr.isRemoteAbsolute(p)) {
			fhandle fh;
			if(p.getNameCount() == 1) {
				fh = fhRoot;
			} else {
				fh = getFileHandle(fhRoot, p);
			}
			
			currentDir = fh;
			currentDirPath = p;
			return currentDirPath.toString();
		} else {
			fhandle fh = getFileHandle(currentDir, p);
			currentDir = fh;
			currentDirPath = currentDirPath.resolve(p);
			return currentDirPath.toString();
		} 
	}
	
	public void remove(String path) throws OncRpcException, IOException, NFSOperationException {
		if(isDirectory(path)) {
			processRemoveDir(path);
		} else {
			processRemoveFile(path);
		}
	}
	
	/**
	 * Get file size.
	 * 
	 * @param p
	 * @return
	 * @throws OncRpcException
	 * @throws IOException
	 * @throws NFSOperationException
	 */
	private int getSize(String p) throws OncRpcException, IOException, NFSOperationException {
		fattr attrs = processReadFileAttrs(p);
		return attrs.size;
	}
	
	private boolean isDirectory(String p) throws OncRpcException, IOException, NFSOperationException {
		fattr attrs = processReadFileAttrs(p);
		if(attrs.type == ftype.NFDIR) {
			return true;
		} else {
			return false;
		}
	}
}

class MountException extends Exception {
	private static final long serialVersionUID = 1L;

	public MountException(String message) {
		super(message);
	}
}

class NFSOperationException extends Exception {
	private static final long serialVersionUID = 1L;

	public NFSOperationException(String message) {
		super(message);
	}
}

class NFSEvent {
	private String path;
	private String action;
	private String content;
	
	public NFSEvent(String path, String action, String content) {
		this.path = path;
		this.action = action;
		this.content = content;
	}
	
	public String getAction() {
		return action;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getContent() {
		return content;
	}
}