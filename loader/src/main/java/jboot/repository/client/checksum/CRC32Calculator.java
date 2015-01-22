package jboot.repository.client.checksum;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class CRC32Calculator implements IChecksumCalculator {
    @Override
    public String getChecksum(File file) throws Exception {
        if (file != null) {
        	BufferedInputStream in = null;
        	try {
	            FileInputStream inputStream = new FileInputStream(file);
	            CheckedInputStream check = new CheckedInputStream(inputStream, new CRC32());
	            in = new BufferedInputStream(check);
	            while (in.read() != -1) {
	            }
	            return Long.toHexString(check.getChecksum().getValue()).toUpperCase();
        	} finally {
        		if (in != null) {
        			in.close();
        		}
        	}
        }
        return null;
    }
}
