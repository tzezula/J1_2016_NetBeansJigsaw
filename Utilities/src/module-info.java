/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

module org.openide.util {
    requires java.desktop;
    requires java.compiler;
    requires java.logging;
    requires java.prefs;
    exports org.openide.util;
    exports org.openide.util.io;
    exports org.openide.util.lookup;    
    exports org.openide.util.lookup.implspi;
    exports org.openide.util.spi;
    exports org.openide.xml;
    uses org.openide.util.Lookup;
    uses org.openide.util.Lookup.Provider;
    provides javax.annotation.processing.Processor with org.netbeans.modules.openide.util.ServiceProviderProcessor;
    provides javax.annotation.processing.Processor with org.netbeans.modules.openide.util.NamedServiceProcessor;
}
