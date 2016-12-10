package DistributedSystemCourse.NFSClient;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import org.acplt.oncrpc.OncRpcException;

public class Replicator implements Observer {

	private ServerHandler handler;
	
	public Replicator(ServerHandler handler) {
		this.handler = handler;
	}
	
	@Override
	public void update(Observable o, Object arg) {
		System.out.println("GOT EVENT");
		NFSEvent event = (NFSEvent) arg;
		try {
			if(event.getAction().equals("mount")) {
				handler.mount(event.getPath());
			} else if(event.getAction().equals("setFileAttrs")) {
				handler.processSetFileAttrs(event.getPath());
			} else if(event.getAction().equals("createDir")) {
				handler.processCreateDir(event.getPath());
			} else if(event.getAction().equals("remove")) {
				handler.processRemove(event.getPath());
			} else if(event.getAction().equals("createFile")) {
				handler.processCreateFile(event.getPath());
			} else if(event.getAction().equals("writeFile")) {
				handler.processWriteFile(event.getPath(), event.getContent());
			} 
		} catch (OncRpcException | IOException | MountException | NFSOperationException e) {
			// TODO Auto-generated catch block
			System.out.println("Replicate server: " + e.getMessage());
		}
	}

}
