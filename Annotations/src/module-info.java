/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

module org.netbeans.api.annotations.common {
    requires java.base;
    requires java.compiler;
    exports org.netbeans.api.annotations.common;
    provides javax.annotation.processing.Processor with org.netbeans.api.annotations.common.proc.StaticResourceProcessor;
}
