package DistributedSystemCourse.NFSClient;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.acplt.oncrpc.OncRpcException;

public class LocalSynchronizer implements Runnable {
	
	private Queue<FSEvent> events;
	private DirectoryWatcher dirWatcher;
	private Thread dirWatcherThread;
	private ServerHandler handler;
	private String sharedDir;
	private volatile boolean running = true;
	private Map<Path, Boolean> isDirectory;

	
	public LocalSynchronizer(String sharedDir, ServerHandler handler) throws IOException {
		this.handler = handler;
		isDirectory = new HashMap<>();
		events = new ArrayBlockingQueue<>(50);
		dirWatcher = new DirectoryWatcher(sharedDir, events);
		isDirectory.putAll(dirWatcher.registerDirectory(Paths.get(sharedDir)));
		dirWatcherThread = new Thread(dirWatcher);
		dirWatcherThread.start();
		this.sharedDir = sharedDir;
		
	}

	public void terminate() {
		dirWatcher.terminate();
        running = false;
    }
	
	public boolean isDirectory(Path path) {
		return isDirectory.get(path);
	}
		
	@Override
	public void run() {
		while(running) {
			FSEvent event = events.poll();
			if(event != null) {
				System.out.println("GOT");
				WatchEvent.Kind kind = event.getEvent().kind();
				if (kind == ENTRY_CREATE) {
					if(Files.isDirectory(event.getFile(), NOFOLLOW_LINKS)) {
						try {
							System.out.println("Created local dir " + event.getFile().toString());
							handler.processCreateDir(event.getFile().toString());
						} catch (OncRpcException | IOException | NFSOperationException e) {
							System.out.println(e.getMessage());
						}
					} else {
						System.out.println("Created local file " + event.getFile().toString());
						try {
							handler.processCreateFile(event.getFile().toString());
						} catch (OncRpcException | IOException | NFSOperationException e) {
							System.out.println(e.getMessage());
						}
					}
					
					try {
						isDirectory.putAll(dirWatcher.registerDirectory(event.getFile()));
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					isDirectory.put(event.getFile(), Files.isDirectory(event.getFile()));
					
		        } else if (kind == ENTRY_DELETE) {
		        	try {
		        		if(isDirectory.get(event.getFile())) {
		        			handler.processRemoveDir(event.getFile().toString());
		        		} else {
		        			handler.processRemoveFile(event.getFile().toString());
		        		}
		        		
		        		isDirectory.remove(event.getFile());
						
					} catch (OncRpcException | IOException | NFSOperationException e) {
						System.out.println(e.getMessage());
					}
		        } else if (kind == ENTRY_MODIFY) {
		        	try {
						handler.processWriteFile(event.getFile().toString(), new String(Files.readAllBytes(event.getFile())));
					} catch (OncRpcException | IOException | NFSOperationException e) {
						System.out.println(e.getMessage());
					}
		        } else {
		        	
		        }
			}
		}
		System.out.println("STOPPED SYNCH");
	}		
}

