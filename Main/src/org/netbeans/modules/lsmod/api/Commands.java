/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2016 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.lsmod.api;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.netbeans.api.annotations.common.NonNull;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.util.NbCollections;
import org.openide.util.Parameters;

/**
 *
 * @author Tomas Zezula
 */
public final class Commands {
    private Commands() {
        throw new IllegalStateException("No instance allowed"); //NOI18N
    }
    
    @NonNull
    public static Stream<FileObject> listModules() throws IOException {
        return getModules()
                .map((fo) -> Arrays.stream(fo.getChildren()))
                .orElse(Stream.empty());
        
    }
    
    @NonNull
    public static Stream<? extends FileObject> listModule(@NonNull final String moduleName) throws IOException {
        Parameters.notNull("moduleName", moduleName);   //NOI18N
        return getModules()
                .map((fo) -> {
                    return Arrays.stream(fo.getChildren())
                            .filter((mod) -> moduleName.equals(mod.getNameExt()))
                            .flatMap((mod) -> StreamSupport.stream(
                                Spliterators.spliteratorUnknownSize(EnumIt.of(mod.getChildren(true)),0),
                                false)
                                    .filter((f)-> f.isData()));
                })
                .orElse(Stream.empty());
    }
    
    @NonNull
    private static Optional<? extends FileObject> getModules() throws IOException {
        return Optional.ofNullable(URLMapper.findFileObject(new URL("jrt:///")))    //NOI18N
                .map((root) -> root.getFileObject("modules"));   //NOI18N        
    }
    
    private static final class EnumIt<T> implements Iterator<T> {
        private final Enumeration<? extends T> enumeration;
        
        private EnumIt(@NonNull final Enumeration<? extends T> enumeration) {
            Parameters.notNull("enumeration", enumeration); //NOI18N
            this.enumeration = enumeration;
        }

        @Override
        public boolean hasNext() {
            return enumeration.hasMoreElements();
        }

        @Override
        public T next() {
            return enumeration.nextElement();
        }
        
        @NonNull
        static <T> EnumIt<T> of (Enumeration<? extends T> enumeration) {
            return new EnumIt(enumeration);
        }
    }
}
