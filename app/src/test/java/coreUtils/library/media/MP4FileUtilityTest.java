package coreUtils.library.media;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

public class MP4FileUtilityTest {

    @Test
    public void testContainsMoovAtomAtStart_Valid() {
        // Create a fake MP4 structure: [size][ftyp]...[size][moov]
        byte[] ftypBox = createFakeBox("ftyp", 20);
        byte[] moovBox = createFakeBox("moov", 50);
        
        byte[] data = new byte[100];
        System.arraycopy(ftypBox, 0, data, 0, ftypBox.length);
        System.arraycopy(moovBox, 0, data, ftypBox.length, moovBox.length);
        
        assertTrue("Should detect moov atom at the start", 
                MP4FileUtility.containsMoovAtomAtStart(data));
    }

    @Test
    public void testContainsMoovAtomAtStart_Invalid() {
        // Create a fake structure where moov is missing
        byte[] ftypBox = createFakeBox("ftyp", 20);
        byte[] mdatBox = createFakeBox("mdat", 50);
        
        byte[] data = new byte[100];
        System.arraycopy(ftypBox, 0, data, 0, ftypBox.length);
        System.arraycopy(mdatBox, 0, data, ftypBox.length, mdatBox.length);
        
        assertFalse("Should not detect moov atom if it's missing", 
                MP4FileUtility.containsMoovAtomAtStart(data));
    }

    @Test
    public void testContainsMoovAtomAtStart_MoovTooFar() {
        // Create a structure where moov is beyond 1024 bytes
        byte[] ftypBox = createFakeBox("ftyp", 20);
        byte[] mdatBox = createFakeBox("mdat", 1100);
        byte[] moovBox = createFakeBox("moov", 50);
        
        byte[] data = new byte[1200];
        System.arraycopy(ftypBox, 0, data, 0, ftypBox.length);
        System.arraycopy(mdatBox, 0, data, ftypBox.length, mdatBox.length);
        System.arraycopy(moovBox, 0, data, ftypBox.length + mdatBox.length, moovBox.length);
        
        assertFalse("Should return false if moov is beyond 1024 bytes", 
                MP4FileUtility.containsMoovAtomAtStart(data));
    }

    private byte[] createFakeBox(String type, int size) {
        byte[] box = new byte[size];
        box[0] = (byte) ((size >> 24) & 0xFF);
        box[1] = (byte) ((size >> 16) & 0xFF);
        box[2] = (byte) ((size >> 8) & 0xFF);
        box[3] = (byte) (size & 0xFF);
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(typeBytes, 0, box, 4, 4);
        return box;
    }
}
