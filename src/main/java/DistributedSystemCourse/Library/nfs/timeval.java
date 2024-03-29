/*
 * Automatically generated by jrpcgen 1.1.3 on 12/8/16 10:20 AM
 * jrpcgen is part of the "Remote Tea" ONC/RPC package for Java
 * See http://remotetea.sourceforge.net for details
 */
package DistributedSystemCourse.Library.nfs;
import org.acplt.oncrpc.*;
import java.io.IOException;

public class timeval implements XdrAble {
    public int seconds;
    public int useconds;

    public timeval() {
    }

    public timeval(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
           throws OncRpcException, IOException {
        xdr.xdrEncodeInt(seconds);
        xdr.xdrEncodeInt(useconds);
    }

    public void xdrDecode(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        seconds = xdr.xdrDecodeInt();
        useconds = xdr.xdrDecodeInt();
    }

}
// End of timeval.java
