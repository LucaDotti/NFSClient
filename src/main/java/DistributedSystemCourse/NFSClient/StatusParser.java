package DistributedSystemCourse.NFSClient;

import java.util.HashMap;
import java.util.Map;

import DistributedSystemCourse.Library.nfs.stat;

public class StatusParser {
	static Map<Integer, String> map = new HashMap<>();
	
	static {
		map.put(stat.NFSERR_PERM, "Not file owner");
		map.put(stat.NFSERR_NOENT, "No such file or directory");
		map.put(stat.NFSERR_IO, "IO error");
		map.put(stat.NFSERR_NXIO, "No such device or address");
		map.put(stat.NFSERR_ACCES, "Permission denied");
		map.put(stat.NFSERR_EXIST, "File already exists");
		map.put(stat.NFSERR_NODEV, "No such device");
		map.put(stat.NFSERR_NOTDIR, "Cannot perform this operation on file");
		map.put(stat.NFSERR_ISDIR, "Cannot perform this operation on directory");
		map.put(stat.NFSERR_FBIG, "File too large");
		map.put(stat.NFSERR_NOSPC, "No space left on device");
		map.put(stat.NFSERR_ROFS, "Read-only file system");
		map.put(stat.NFSERR_NAMETOOLONG, "File name too long");
		map.put(stat.NFSERR_NOTEMPTY, "Cannot remove not empty directory");
		map.put(stat.NFSERR_DQUOT, "Disk quota exceeded");
		map.put(stat.NFSERR_STALE, "Invalid file handle");
		map.put(stat.NFSERR_WFLUSH, "The cache was flushed on disk");
	}
	
	public static String parse(int status) {
		return map.get(status);
	}

}
