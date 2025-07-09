package leb.util.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

public class FileRemover {
    public static void safeDelete(String path) {

        try { Files.delete(Paths.get(path)); }
        catch(IOException e) { Prompt.warning("Failed to delete file: " + path); }
    }

    public static void safeDeleteDirectory(String path) {
        try { FileUtils.deleteDirectory(Paths.get(path).toFile()); }
        catch(IOException e) { Prompt.warning("Failed to delete directory: " + path); }
    }
}
