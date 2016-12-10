/*
 * The maximum number of bytes of data in a READ or WRITE
 * request.
 */
const MAXDATA = 8192;

/* The maximum number of bytes in a pathname argument. */
const MAXPATHLEN = 1024;

/* The maximum number of bytes in a file name argument. */
const MAXNAMLEN = 255;

/* The size in bytes of the opaque "cookie" passed by READDIR. */
const COOKIESIZE  = 4;

/* The size in bytes of the opaque file handle. */
const FHSIZE = 32;
typedef opaque nfsdata<MAXDATA>;
typedef opaque nfscookie[COOKIESIZE];
/*
* Stat
*/
enum stat {
   NFS_OK = 0,
   NFSERR_PERM=1,
   NFSERR_NOENT=2,
   NFSERR_IO=5,
   NFSERR_NXIO=6,
   NFSERR_ACCES=13,
   NFSERR_EXIST=17,
   NFSERR_NODEV=19,
   NFSERR_NOTDIR=20,
   NFSERR_ISDIR=21,
   NFSERR_FBIG=27,
   NFSERR_NOSPC=28,
   NFSERR_ROFS=30,
   NFSERR_NAMETOOLONG=63,
   NFSERR_NOTEMPTY=66,
   NFSERR_DQUOT=69,
   NFSERR_STALE=70,
   NFSERR_WFLUSH=99
};

/*
* ftype
*/
enum ftype {
   NFNON = 0,
   NFREG = 1,
   NFDIR = 2,
   NFBLK = 3,
   NFCHR = 4,
   NFLNK = 5
};

/*
* fhandle
*/
typedef opaque fhandle[FHSIZE];

/*
* timeval
*/
struct timeval {
   unsigned int seconds;
   unsigned int useconds;
};

/*
* fattr
*/
struct fattr {
   ftype        type;
   unsigned int mode;
   unsigned int nlink;
   unsigned int uid;
   unsigned int gid;
   unsigned int size;
   unsigned int blocksize;
   unsigned int rdev;
   unsigned int blocks;
   unsigned int fsid;
   unsigned int fileid;
   timeval      atime;
   timeval      mtime;
   timeval      ctime;
};

/*
* sattr
*/
struct sattr {
   unsigned int mode;
   unsigned int uid;
   unsigned int gid;
   unsigned int size;
   timeval      atime;
   timeval      mtime;
};

/*
* filename
*/
typedef string filename<MAXNAMLEN>;

/*
* path
*/
typedef string path<MAXPATHLEN>;

/*
* attrstat
*/
union attrstat switch (stat status) {
   case NFS_OK:
      fattr attributes;
   default:
      void;
};

/*
* diropargs
*/
struct diropargs {
   fhandle  dir;
   filename name;
};

/*
* diropres
*/
union diropres switch (stat status) {
   case NFS_OK:
   	  diropresOk diropok;
   default:
      void;
};

struct diropresOk {
    fhandle file;
    fattr   attributes;
};

struct sattrargs {
   fhandle file;
   sattr attributes;
};

/*
* Read From Symbolic Link
*/
union readlinkres switch (stat status) {
   case NFS_OK:
      path data;
   default:
      void; 
};

/*
* Read From File
*/
struct readargs {
   fhandle file;
   unsigned offset;
   unsigned count;
   unsigned totalcount;
};

union readres switch (stat status) {
   case NFS_OK:
      readresOk readRes;
   default:
      void;
};

struct readresOk {
    fattr attributes;
    nfsdata data;
};

/*
* Write to File
*/
struct writeargs {
   fhandle file;
   unsigned beginoffset;
   unsigned offset;
   unsigned totalcount;
   nfsdata data;
};

/*
* Create File
*/
struct createargs {
   diropargs where;
   sattr attributes;
};

/*
* Rename File
*/
struct renameargs {
   diropargs from;
   diropargs to;
};

/*
* Create Link to File
*/
struct linkargs {
   fhandle from;
   diropargs to;
};

/*
* Create Symbolic Link
*/
struct symlinkargs {
   diropargs from;
   path to;
   sattr attributes;
};

/*
* Read From Directory
*/
struct readdirargs {
   fhandle dir;
   nfscookie cookie;
   unsigned count;
};

struct entry {
   unsigned fileid;
   filename name;
   nfscookie cookie;
   entry *nextentry;
};

union readdirres switch (stat status) {
   case NFS_OK:
      readdirOk readdirok;
   default:
      void;
};

struct readdirOk {
    entry *entries;
    bool eof;
};

/*
* Get Filesystem Attributes
*/
union statfsres switch (stat status) {
   case NFS_OK:
      statfsresOk info;
   default:
      void;
};

struct statfsresOk {
	unsigned tsize;
	unsigned bsize;
	unsigned blocks;
	unsigned bfree;
	unsigned bavail;
};

/*
* Remote file service routines
*/
program NFS_PROGRAM {
   version NFS_VERSION {
      void
      NFSPROC_NULL(void)              = 0;
      attrstat
      NFSPROC_GETATTR(fhandle)        = 1;
      attrstat
      NFSPROC_SETATTR(sattrargs)      = 2;
      void
      NFSPROC_ROOT(void)              = 3;
      diropres
      NFSPROC_LOOKUP(diropargs)       = 4;
      readlinkres
      NFSPROC_READLINK(fhandle)       = 5;
      readres
      NFSPROC_READ(readargs)          = 6;
      void
      NFSPROC_WRITECACHE(void)        = 7;
      attrstat
      NFSPROC_WRITE(writeargs)        = 8;
      diropres
      NFSPROC_CREATE(createargs)      = 9;
      stat
      NFSPROC_REMOVE(diropargs)       = 10;
      stat
      NFSPROC_RENAME(renameargs)      = 11;
      stat
      NFSPROC_LINK(linkargs)          = 12;
      stat
      NFSPROC_SYMLINK(symlinkargs)    = 13;
      diropres
      NFSPROC_MKDIR(createargs)       = 14;
      stat
      NFSPROC_RMDIR(diropargs)        = 15;
      readdirres
      NFSPROC_READDIR(readdirargs)    = 16;
      statfsres
      NFSPROC_STATFS(fhandle)         = 17;
       } = 2;
} = 100003;