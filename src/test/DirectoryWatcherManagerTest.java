import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import junit.framework.TestCase;

public class DirectoryWatcherManagerTest extends TestCase {
	private Path root;
		
	@Test
	public void test() {
		DirectoryWatcherManager dwm;
		Path dir = Paths.get("/Users/usi/Desktop/watcherTest");
		root = Paths.get("/Users/usi/Desktop/watcherTest");
		createDir(dir);
		
		dwm = new DirectoryWatcherManager();
		
		try {
			dwm.watchDirectory(dir);
			assertEquals(1, dwm.getRoots().size());
			assertEquals(1, dwm.getRoots().get(dir).getDirectoriesKeys().size());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		dir = Paths.get("/Users/usi/Desktop/watcherTest/a");
		createDir(dir);
		
		try {
			dwm.watchDirectory(dir);
			assertEquals(1, dwm.getRoots().size());
			assertEquals(2, dwm.getRoots().get(root).getDirectoriesKeys().size());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		dir = Paths.get("/Users/usi/Desktop/watcherTest/b");
		createDir(dir);
		createDir(Paths.get("/Users/usi/Desktop/watcherTest/b/c"));
		
		try {
			dwm.watchDirectory(dir);
			assertEquals(1, dwm.getRoots().size());
			assertEquals(4, dwm.getRoots().get(root).getDirectoriesKeys().size());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		dir = Paths.get("/Users/usi/Desktop/watcherTest2");
		createDir(dir);
		
		try {
			dwm.watchDirectory(dir);
			assertEquals(2, dwm.getRoots().size());
			assertEquals(4, dwm.getRoots().get(root).getDirectoriesKeys().size());
			assertEquals(1, dwm.getRoots().get(dir).getDirectoriesKeys().size());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Path dir2 = Paths.get("/Users/usi/Desktop/watcherTest2/a");
		createDir(dir2);
		
		try {
			dwm.watchDirectory(dir2);
			assertEquals(2, dwm.getRoots().size());
			assertEquals(4, dwm.getRoots().get(root).getDirectoriesKeys().size());
			assertEquals(2, dwm.getRoots().get(dir).getDirectoriesKeys().size());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		dir2 = Paths.get("/Users/usi/Desktop/root");
		createDir(dir2);
		createDir(Paths.get("/Users/usi/Desktop/root/watcherTest"));
		createDir(Paths.get("/Users/usi/Desktop/root/watcherTest2"));
		
		try {
			FileUtils.copyDirectory(new File("/Users/usi/Desktop/watcherTest"), new File("/Users/usi/Desktop/root/watcherTest"));
			FileUtils.copyDirectory(new File("/Users/usi/Desktop/watcherTest2"), new File("/Users/usi/Desktop/root/watcherTest2"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			dwm.watchDirectory(dir2);
			assertEquals(1, dwm.getRoots().size());
			assertEquals(5, dwm.getRoots().get(dir2).getDirectoriesKeys().size());
//			assertEquals(2, dwm.getRoots().get(dir).getDirectoriesKeys().size());
		} catch (IOException e) {
			e.printStackTrace();
		}
//		clearDirs(dir);
		
//		clearDirs(root);
		
	}
	
	private void clearDirs(Path path) {
		try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void createDir(Path path) {
		try {
			Files.createDirectory(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
