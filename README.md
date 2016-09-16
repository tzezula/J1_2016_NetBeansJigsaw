#Java One 2016: Java 9 Module System Support in the NetBeans IDE
This repository contains a demo projects to demonstrare a NetBeans support for Java 9 module system.

The NetBeans IDE with Java 9 support can be downloaded [NetBeans 9 Continuos Build](http://bits.netbeans.org/netbeans/nb9-for-jdk9_jigsaw/daily/latest/)

The project uses a [NetBeans FileSystem API](http://bits.netbeans.org/7.4/javadoc/org-openide-filesystems/org/openide/filesystems/doc-files/api.html) to implement a [NetBeans FileSystem](http://bits.netbeans.org/7.4/javadoc/org-openide-filesystems/org/openide/filesystems/FileSystem.html) to access jrt image.

##Content
*__modules__ binaries of modularised NetBeans FileSystem API and dependent modules
*__JRTFileSystem__ a project providing a NetBeans FileSystem for jrt image. Library and white box unit tests.
*__Main__ an application project using the __JRTFileSystem__ to list all modules in active JDK 9.
