package DistributedSystemCourse.NFSClient;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public class FSEvent {
	private Path file;
	private WatchEvent<Path> event;
	private int timestamp;
	
	public FSEvent(Path file, WatchEvent<Path> event, int timestamp) {
		this.file = file;
		this.event = event;
		this.timestamp = timestamp;
	}
	
	public WatchEvent<Path> getEvent() {
		return event;
	}
	
	public Path getFile() {
		return file;
	}
	
	public int getTimestamp() {
		return timestamp;
	}
}
