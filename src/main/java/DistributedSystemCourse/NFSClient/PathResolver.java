package DistributedSystemCourse.NFSClient;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class PathResolver {
	private Path localRoot;
	private Path remoteRoot;
	
	public PathResolver(String localRoot, String remoteRoot) {
		this.localRoot = Paths.get(localRoot);
		this.remoteRoot = Paths.get(remoteRoot);
	}
	
	public Path toRemote(Path path) {
		return null;
	}
	
	public Path toLocal(Path path) {
		return null;
	}
	
	public Path relativizeLocal(Path path) throws IllegalArgumentException {
		return localRoot.relativize(path);
	}
	
	public Path relativizeRemote(Path path) throws IllegalArgumentException {
		return remoteRoot.relativize(path);
	}
	
	public Path resolveLocal(Path path) {
		return localRoot.resolve(path);
	}
	
	public Path resolveRemote(Path path) {
		return remoteRoot.resolve(path);
	}
	
	public boolean isRemoteAbsolute(Path p) {
		return p.startsWith(remoteRoot);
	}
	
	public boolean isLocalAbsolute(Path p) {
		return p.startsWith(localRoot);
	}
	
}
