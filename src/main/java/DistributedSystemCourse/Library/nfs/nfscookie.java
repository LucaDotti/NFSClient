/*
 * Automatically generated by jrpcgen 1.1.3 on 12/8/16 10:20 AM
 * jrpcgen is part of the "Remote Tea" ONC/RPC package for Java
 * See http://remotetea.sourceforge.net for details
 */
package DistributedSystemCourse.Library.nfs;
import org.acplt.oncrpc.*;
import java.io.IOException;

public class nfscookie implements XdrAble {

    public byte [] value;

    public nfscookie() {
    }

    public nfscookie(byte [] value) {
        this.value = value;
    }

    public nfscookie(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        xdrDecode(xdr);
    }

    public void xdrEncode(XdrEncodingStream xdr)
           throws OncRpcException, IOException {
        xdr.xdrEncodeOpaque(value, nfsDef.COOKIESIZE);
    }

    public void xdrDecode(XdrDecodingStream xdr)
           throws OncRpcException, IOException {
        value = xdr.xdrDecodeOpaque(nfsDef.COOKIESIZE);
    }

}
// End of nfscookie.java