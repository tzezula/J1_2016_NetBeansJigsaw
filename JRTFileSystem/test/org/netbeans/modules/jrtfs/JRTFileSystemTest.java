package org.netbeans.modules.jrtfs;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Tomas Zezula
 */
public class JRTFileSystemTest {
    
    public JRTFileSystemTest() {
    }
    
    @Test
    public void testFileSystemCreated() throws IOException {
        final JRTFileSystem fs = JRTFileSystem.getInstance();
        assertNotNull("FileSystem non null", fs);   //NOI18N
    }
    
    @Test
    public void testListModules() throws IOException {
        final JRTFileSystem fs = JRTFileSystem.getInstance();
        final FileObject modules = fs.getRoot().getFileObject("modules"); //NOI18N
        assertNotNull("Modles exists", modules);    //NOI18N
        assertNotNull("java.base exists", modules.getFileObject("java.base"));  //NOI18N
    }
}
