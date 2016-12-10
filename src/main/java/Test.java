import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import DistributedSystemCourse.NFSClient.LocalSynchronizer;
import junit.framework.TestCase;

public class Test extends TestCase {
	
	public void test() {
//		DirectoryWatcher dw = null;
//		try {
//			 dw = new DirectoryWatcher("/Users/usi/Desktop/watcherTest", null);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		
//		new Thread(dw).start();
		
//		LocalSynchronizer s = null;
//		try {
//			s = new LocalSynchronizer("/Users/usi/Desktop/watcherTest", null);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		new Thread(s).start();
//		while(true) {
//			
//		}
		
//		Path p = Paths.get("/Users/usi/Desktop/watcherTest/alpha");
//		Iterator<Path> it = p.iterator();
//		while(it.hasNext()) {
//			System.out.println(((Path)it.next()).toString());
//		}
//		Path p = Paths.get("/Exports");
//		Path p2 = Paths.get("/Exports/a/b/a.txt");
//		try {
//			p.resolve("/Users/usi/Desktop/watcherTest/no");
//			p.relativize(p2);
//			System.out.println(p.relativize(p2));
//			System.out.println(p2.getFileName());
//		} catch(Exception e) {
//			System.out.println("ERROR");
//		}
		
//		Path a = Paths.get("a/b/c");
//		System.out.println(a.getParent());
//		Path a = Paths.get("/Users/usi/Desktop/watcherTest2");
//		try {
//			deleteRec(a);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}
	
	private static void deleteRec(Path root) throws IOException {
		if(!Files.isDirectory(root)) {
			Files.delete(root);
			System.out.println("DELETED " + root);
		} else {
			String[] content = root.toFile().list();
			for(String file: content) {
				
				deleteRec(root.resolve(Paths.get(file)));
			}
			
			Files.delete(root);
			System.out.println("DELETED " + root);
		}
	}

}

