/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

module org.openide.filesystems {
    requires java.base;
    requires java.compiler;
    requires java.logging;
    requires java.xml;
    requires java.rmi;
    requires java.management;
    requires java.desktop;
    requires org.openide.util;
    exports org.openide.filesystems;
    exports org.openide.filesystems.annotations;
    exports org.openide.filesystems.spi;
    uses org.openide.filesystems.FileSystem;
    uses org.openide.filesystems.Repository;
    uses org.openide.filesystems.URLMapper;
    uses org.openide.filesystems.Repository.LayerProvider;
    uses org.openide.filesystems.MIMEResolver;
    uses org.openide.filesystems.spi.ArchiveRootProvider;
    uses org.openide.filesystems.StatusDecorator;
    provides org.openide.filesystems.URLMapper with org.netbeans.modules.openide.filesystems.DefaultURLMapperProxy;
    provides org.openide.util.lookup.implspi.NamedServicesProvider with org.netbeans.modules.openide.filesystems.RecognizeInstanceFiles;
    provides javax.annotation.processing.Processor with org.netbeans.modules.openide.filesystems.declmime.MIMEResolverProcessor;
    provides org.openide.filesystems.URLMapper with org.openide.filesystems.MemoryFileSystem.Mapper;
}
