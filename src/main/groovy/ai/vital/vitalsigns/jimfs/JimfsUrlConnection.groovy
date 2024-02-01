package ai.vital.vitalsigns.jimfs

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * SOURCE: https://github.com/google/jimfs/issues/13
 */

public class JimfsUrlConnection extends URLConnection {
    private Path path;

    public JimfsUrlConnection(URL url) {
        super(url);
    }

    @Override
    public void connect() throws IOException {
        try {
            FileSystem fileSystem = FileSystems.getFileSystem(
                    URI.create("jimfs://" + getURL().getAuthority()));
            path = fileSystem.getPath(getURL().getPath());
        } catch (FileSystemNotFoundException e) {
            throw new IOException(e);
        } catch(InvalidPathException e) {
            throw new IOException(e);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        // Preconditions.checkState(path != null);
        FileSystem fileSystem = FileSystems.getFileSystem(
                URI.create("jimfs://" + getURL().getAuthority()));
        path = fileSystem.getPath(getURL().getPath());
        return Files.newInputStream(path);
    }
}