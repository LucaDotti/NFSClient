/*
 * Automatically generated by jrpcgen 1.1.3 on 12/8/16 10:20 AM
 * jrpcgen is part of the "Remote Tea" ONC/RPC package for Java
 * See http://remotetea.sourceforge.net for details
 */
package DistributedSystemCourse.Library.nfs;
import org.acplt.oncrpc.*;
import java.io.IOException;

public class diropres implements XdrAble {
    public int status;
    public diropresOk diropok;

    public diropres() {
    }

    public diropres(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
           throws OncRpcException, IOException {
        xdr.xdrEncodeInt(status);
        switch ( status ) {
        case stat.NFS_OK:
            diropok.xdrEncode(xdr);
            break;
        default:
            break;
        }
    }

    public void xdrDecode(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        status = xdr.xdrDecodeInt();
        switch ( status ) {
        case stat.NFS_OK:
            diropok = new diropresOk(xdr);
            break;
        default:
            break;
        }
    }

}
// End of diropres.java
