/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

module org.netbeans.modules.jrtfs {
    requires java.base;
    requires java.logging;
    requires org.netbeans.api.annotations.common;
    requires org.openide.util;
    requires org.openide.filesystems;
    exports org.netbeans.modules.jrtfs.api;
    provides org.openide.filesystems.URLMapper with org.netbeans.modules.jrtfs.JRTURLMapper;
}
