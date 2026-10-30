package io.teaql.core.blob;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlobCommandServiceTest {
    @Test public void fingerprintIsDomainSeparatedAndInputIsDefensivelyCopied() {
        byte[] input="private payload".getBytes(StandardCharsets.UTF_8); var request=new BlobCommandService.Request("Asset.bind/v1","one",input);
        String fingerprint=request.fingerprint(); input[0]=0; request.canonicalInput()[0]=1;
        assertEquals(fingerprint,request.fingerprint()); assertEquals('p',request.canonicalInput()[0]);
        assertEquals(fingerprint,new BlobCommandService.Request("Asset.bind/v1","two","private payload".getBytes(StandardCharsets.UTF_8)).fingerprint());
        assertNotEquals(fingerprint,new BlobCommandService.Request("Asset.clear/v1","one","private payload".getBytes(StandardCharsets.UTF_8)).fingerprint());
        assertFalse(request.toString().contains("private payload"));
    }
    @Test public void receiptIsClosedSortedImmutableAndRejectsInvalidOrDuplicateRevisions() {
        var scope=new BlobScope("test","tenant"); String digest="a".repeat(64);
        var receipt=new BlobCommandService.Receipt(scope,"Asset.bind/v1","one",digest,Instant.EPOCH,List.of(new BlobCommandService.EntityRevision("Asset",43,-2),new BlobCommandService.EntityRevision("Asset",42,1)));
        assertEquals(42,receipt.entities().get(0).id()); assertThrows(UnsupportedOperationException.class,receipt.entities()::clear);
        assertThrows(BlobException.class,() -> new BlobCommandService.EntityRevision("Asset",42,Long.MIN_VALUE));
        assertThrows(BlobException.class,() -> new BlobCommandService.Request("bad operation","one",new byte[]{1}));
        assertThrows(BlobException.class,() -> new BlobCommandService.Request("Asset.bind/v1","one",new byte[1048577]));
        assertThrows(BlobException.class,() -> new BlobCommandService.Receipt(scope,"Asset.bind/v1","one",digest,Instant.EPOCH,List.of(new BlobCommandService.EntityRevision("Asset",42,1),new BlobCommandService.EntityRevision("Asset",42,2))));
    }
}
