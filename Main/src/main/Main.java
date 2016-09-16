package main;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;

/**
 *
 * @author Tomas Zezula
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws MalformedURLException {
        final FileObject jrtRoot = URLMapper.findFileObject(new URL("jrt:///"));    //NOI18N
        final FileObject modules = jrtRoot.getFileObject("modules");    //NOI18N
        Arrays.stream(modules.getChildren())
                .map ((fo) -> fo.getNameExt())
                .forEach(System.out::println);
    }
    
}
