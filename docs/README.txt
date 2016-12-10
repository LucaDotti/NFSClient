GENERATING LIBRARY FILES
^^^^^^^^^^^^^^^^^^^^^^^^

The XDR files can be found in src/main/resources/xFiles. Both files were created following RFC1094. Note that some definitions
in RFC1094 need to be fixed: 

   - Some inner structs (all structs defined in the case where NFS_OK) were removed, defined outside and 
     created as a variable (ex. line 170). 
   - Some structs have a '*' before the name -> removed '*'

The jrpcgen jar used to generate the files is in /Library.

Mount files:

    java -jar src/main/java/DistributedSystemCourse/Library/jrpcgen.jar -p DistributedSystemCourse.Library.mount -nobackup -verbose -d src/main/java/DistributedSystemCourse/Library/mount src/main/resources/xFiles/mountDef.x 
    
NFS files:

    java -jar src/main/java/DistributedSystemCourse/Library/jrpcgen.jar -p DistributedSystemCourse.Library.nfs -nobackup -verbose -d src/main/java/DistributedSystemCourse/Library/nfs src/main/resources/xFiles/nfsDef.x
     
     TODO:
      - Restore
      - Split file