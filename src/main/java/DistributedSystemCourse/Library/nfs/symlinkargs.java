/*
 * Automatically generated by jrpcgen 1.1.3 on 12/8/16 10:20 AM
 * jrpcgen is part of the "Remote Tea" ONC/RPC package for Java
 * See http://remotetea.sourceforge.net for details
 */
package DistributedSystemCourse.Library.nfs;
import org.acplt.oncrpc.*;
import java.io.IOException;

public class symlinkargs implements XdrAble {
    public diropargs from;
    public path to;
    public sattr attributes;

    public symlinkargs() {
    }

    public symlinkargs(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
           throws OncRpcException, IOException {
        from.xdrEncode(xdr);
        to.xdrEncode(xdr);
        attributes.xdrEncode(xdr);
    }

    public void xdrDecode(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        from = new diropargs(xdr);
        to = new path(xdr);
        attributes = new sattr(xdr);
    }

}
// End of symlinkargs.java
