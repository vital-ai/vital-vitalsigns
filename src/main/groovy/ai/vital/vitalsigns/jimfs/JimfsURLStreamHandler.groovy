package ai.vital.vitalsigns.jimfs

import java.io.IOException
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler

/**
 * SOURCE: https://github.com/google/jimfs/issues/13
 */
public class JimfsURLStreamHandler extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        return new JimfsUrlConnection(url);
    }
}