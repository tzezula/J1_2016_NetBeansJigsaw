package org.netbeans.modules.jrtfs.api;

import java.io.IOException;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.jrtfs.JRTFileSystem;
import org.openide.filesystems.FileSystem;

/**
 *
 * @author Tomas Zezula
 */
public final class Factory {
    private Factory() {
        throw new IllegalStateException("No instance allowed");
    }
    
    @NonNull
    public static FileSystem createJRTFileSystem() throws IOException {
        return JRTFileSystem.getInstance();
    }
}
