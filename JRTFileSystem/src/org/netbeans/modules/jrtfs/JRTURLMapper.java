package org.netbeans.modules.jrtfs;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.URLMapper;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Zezula
 */
@ServiceProvider(service=URLMapper.class)
public class JRTURLMapper extends URLMapper {

    private static final Logger LOG = Logger.getLogger(JRTURLMapper.class.getName());

    @Override
    public URL getURL(FileObject fo, int type) {
        if (type == NETWORK)
            return null;

        try {
            if (fo.getFileSystem() instanceof JRTFileSystem) {
                final Object path = fo.getAttribute("java.nio.file.Path");  //NOI18N
                if (path instanceof Path) {
                    return ((Path)path).toUri().toURL();
                }
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, null, ex);
        }
        return null;
    }

    @Override
    public FileObject[] getFileObjects(URL url) {
        if (JRTFileSystem.PROTOCOL.equals(url.getProtocol())) {
            try {
                final String pathInImage = url.getPath();
                FileSystem fs = JRTFileSystem.getInstance();
                if (fs != null) {
                    final FileObject fo = fs.getRoot().getFileObject(pathInImage);
                    if (fo != null) {
                        return new FileObject[]{
                            fo
                        };
                    }
                }
            } catch (IOException ioe) {
                LOG.log(Level.WARNING, null, ioe);
            }
        }
        return null;
    }

}
