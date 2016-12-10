package DistributedSystemCourse.NFSClient;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class DirectoryWatcher implements Runnable {
	private WatchService watcher; 
	private Path root;
	private HashMap<Path, WatchKey> directories;
	private HashMap<WatchKey, Path> keys;
	private Queue<FSEvent> eventQueue;
	private volatile boolean running = true;
	private int currentTimestamp;
	
	public DirectoryWatcher(String directory, Queue<FSEvent> eventQueue) throws IOException {
		this.root = Paths.get(directory);
		this.watcher = FileSystems.getDefault().newWatchService();
		this.eventQueue = eventQueue;
		directories = new HashMap<Path, WatchKey>();
		keys = new HashMap<WatchKey, Path>();
		currentTimestamp = 0;
	}
	
	public void terminate() {
        running = false;
    }
	
	public Path getRoot() {
		return root;
	}
	
	public void setRoot(Path root) {
		this.root = root;
	}
	
	public void registerParent(Path directory) throws IOException {
		directories.clear();
		keys.clear();
		root = directory;
		registerDirectory(root);
	}
	
	public Map<Path, Boolean> registerDirectory(Path directory) throws IOException {
		Map<Path, Boolean> isDirectory = new HashMap<>();
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
				directories.put(dir, key);
				keys.put(key, dir);
				isDirectory.put(dir, true);
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				isDirectory.put(file, false);
				return FileVisitResult.CONTINUE;
			}
		});
		
		return isDirectory;
	}
	
	public HashMap<Path, WatchKey> getDirectoriesKeys() {
		return directories;
	}

	@Override
	public void run() {
		while(running) {
			// wait for key to be signaled
		    WatchKey key;
		    try {
		        key = watcher.take();
		    } catch (InterruptedException x) {
		    	System.out.println("Interrupted");
		        return;
		    }
		    
		    for (WatchEvent<?> event: key.pollEvents()) {
		        WatchEvent.Kind<?> kind = event.kind();
	
		        if (kind == OVERFLOW) {
		            continue;
		        }
	
		        WatchEvent<Path> ev = (WatchEvent<Path>) event;
		        Path path = keys.get(key).resolve(ev.context());
		        eventQueue.add(new FSEvent(path, ev, currentTimestamp));
		        currentTimestamp++;
		    }
	
		    boolean valid = key.reset();
		    if (!valid) {
		    	keys.remove(key);
		    	
//		        break;
		    }
		}
		System.out.println("STOPPED WATCHER");
	}
}