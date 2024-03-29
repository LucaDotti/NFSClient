/*
 * Automatically generated by jrpcgen 1.1.3 on 12/8/16 10:20 AM
 * jrpcgen is part of the "Remote Tea" ONC/RPC package for Java
 * See http://remotetea.sourceforge.net for details
 */
package DistributedSystemCourse.Library.nfs;
import org.acplt.oncrpc.*;
import java.io.IOException;

public class statfsresOk implements XdrAble {
    public int tsize;
    public int bsize;
    public int blocks;
    public int bfree;
    public int bavail;

    public statfsresOk() {
    }

    public statfsresOk(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
           throws OncRpcException, IOException {
        xdr.xdrEncodeInt(tsize);
        xdr.xdrEncodeInt(bsize);
        xdr.xdrEncodeInt(blocks);
        xdr.xdrEncodeInt(bfree);
        xdr.xdrEncodeInt(bavail);
    }

    public void xdrDecode(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        tsize = xdr.xdrDecodeInt();
        bsize = xdr.xdrDecodeInt();
        blocks = xdr.xdrDecodeInt();
        bfree = xdr.xdrDecodeInt();
        bavail = xdr.xdrDecodeInt();
    }

}
// End of statfsresOk.java
