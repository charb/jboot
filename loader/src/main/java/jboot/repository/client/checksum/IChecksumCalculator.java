package jboot.repository.client.checksum;

import java.io.File;

public interface IChecksumCalculator {
    public String getChecksum(File file) throws Exception;
}
