/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.openide.util.lookup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.Lookup;
import org.openide.util.lookup.implspi.SharedClassObjectBridge;

/**
 * @author Jaroslav Tulach, Jesse Glick
 * @see Lookups#metaInfServices(ClassLoader,String)
 * @see "#14722"
 */
final class MetaInfServicesLookup extends AbstractLookup {
    static final Logger LOGGER = Logger.getLogger(MetaInfServicesLookup.class.getName());
    private static final MetaInfCache CACHE = new MetaInfCache(512);
    private static Reference<Executor> RP = new WeakReference<Executor>(null);
    static synchronized Executor getRP() {
        Executor res = RP.get();
        if (res == null) {
            try {
                Class<?> seek = Class.forName("org.openide.util.RequestProcessor");
                res = (Executor)seek.newInstance();
            } catch (Throwable t) {
                try {
                    res = Executors.newSingleThreadExecutor();
                } catch (Throwable t2) {
                    res = new Executor() {
                        @Override
                        public void execute(Runnable command) {
                            command.run();
                        }
                    };
                }
            }
            RP = new SoftReference<Executor>(res);
        }
        return res;
    }

    /** A set of all requested classes.
     * Note that classes that we actually succeeded on can never be removed
     * from here because we hold a strong reference to the loader.
     * However we also hold classes which are definitely not loadable by
     * our loader.
     */
    private final Map<Class<?>,Object> classes = new WeakHashMap<Class<?>,Object>();

    /** class loader to use */
    private final ClassLoader loader;
    /** prefix to prepend */
    private final String prefix;

    /** Create a lookup reading from a specified classloader.
     */
    @SuppressWarnings("LeakingThisInConstructor")
    public MetaInfServicesLookup(ClassLoader loader, String prefix) {
        this.loader = loader;
        this.prefix = prefix;

        LOGGER.log(Level.FINE, "Created: {0}", this);
    }

    @Override
    public String toString() {
        return "MetaInfServicesLookup[" + loader + "]"; // NOI18N
    }

    /** Initialize soon, before result's listeners are activated
     */
    @Override
    void beforeLookupResult(Template<?> template) {
        beforeLookup(template);
    }

    /* Tries to load appropriate resources from manifest files.
     */
    @Override
    protected final void beforeLookup(Lookup.Template<?> t) {
        Class<?> c = t.getType();

        Collection<AbstractLookup.Pair<?>> toAdd = null;
        synchronized (this) {
            if (classes.get(c) == null) { // NOI18N
                toAdd = new ArrayList<Pair<?>>();
            } else {
                // ok, nothing needs to be done
                return;
            }
        }
        if (toAdd != null) {
            Set<Class<?>> all = new HashSet<Class<?>>();
            for (Class type : allSuper(c, all)) {
                search(type, toAdd);
            }
        }
        HashSet<R> listeners = null;
        synchronized (this) {
            if (classes.put(c, "") == null) { // NOI18N
                // Added new class, search for it.
                LinkedHashSet<AbstractLookup.Pair<?>> lhs = getPairsAsLHS();
                List<Item> arr = new ArrayList<Item>();
                for (Pair<?> lh : lhs) {
                    arr.add((Item)lh);
                }
                for (Pair<?> p : toAdd) {
                    insertItem((Item) p, arr);
                }
                listeners = setPairsAndCollectListeners(arr);
            }
        }
        if (listeners != null) {
            notifyIn(getRP(), listeners);
        }
    }
    
    private Set<Class<?>> allSuper(Class<?> clazz, Set<Class<?>> all) {
        all.add(clazz);
        Class<?> sup = clazz.getSuperclass();
        if (sup != null && sup != Object.class) {
            all.add(sup);
        }
        for (Class<?> c : clazz.getInterfaces()) {
            allSuper(c, all);
        }
        return all;
    }

    /** Finds all pairs and adds them to the collection.
     *
     * @param clazz class to find
     * @param result collection to add Pair to
     */
    private void search(Class<?> clazz, Collection<AbstractLookup.Pair<?>> result) {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER, "Searching for {0} in {1} from {2}", new Object[] {clazz.getName(), clazz.getClassLoader(), this});
        }
        getClass().getModule().addUses(clazz);
        List<Item> foundClasses = new ArrayList<Item>();
        for (Object o :  ServiceLoader.load(clazz)) {
            Item currentItem = new Item(o);
            result.add(currentItem);
        }
    }
    private static String clazzToString(Class<?> clazz) {
        String loc = null;
        try {
            if (clazz.getProtectionDomain() != null && clazz.getProtectionDomain().getCodeSource() != null) {
                loc = clazz.getProtectionDomain().getCodeSource().getLocation().toString();
            }
        } catch (Throwable ex) {
            loc = ex.getMessage();
        }
        return clazz.getName() + "@" + clazz.getClassLoader() + ":" + loc; // NOI18N
    }

    /**
     * Insert item to the list according to item.position value.
     */
    private void insertItem(Item item, List<Item> list) {
        // no position? -> add it to the end
        if (item.position == -1) {
            if (!list.contains(item)) {
                list.add(item);
            }

            return;
        }

        int foundIndex = -1;
        int index = -1;
        for (Item i : list) {
            if (i.equals(item)) {
                return;
            }
            index++;

            if (foundIndex < 0) {
                if (i.position == -1 || i.position > item.position) {
                    foundIndex = index;
                }
            }
        }
        if (foundIndex < 0) {
            list.add(item);             // add to the end
        } else {
            list.add(foundIndex, item); // insert at found index
        }
    }

    static Item createPair(Class<?> clazz) {
        return new Item(clazz);
    }

    /** Pair that holds name of a class and maybe the instance.
     */
    private static final class Item extends AbstractLookup.Pair<Object> {
        /** May be one of three things:
         * 1. The implementation class which was named in the services file.
         * 2. An instance of it.
         * 3. Null, if creation of the instance resulted in an error.
         */
        private Object object;
        private int position = -1;

        public Item(Object instance) {
            this.object = instance;
        }

        @Override
        public String toString() {
            return "MetaInfServicesLookup.Item[" + clazz().getName() + "]"; // NOI18N
        }
        
        /** Finds the class.
         */
        private Class<? extends Object> clazz() {
            Object o = object;
            if (o != null) {
                return o.getClass();
            } else {
                // Broken.
                return Object.class;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Item) {
                return ((Item) o).clazz().equals(clazz());
            }

            return false;
        }

        @Override
        public int hashCode() {
            return clazz().hashCode();
        }

        protected @Override boolean instanceOf(Class<?> c) {
            return c.isAssignableFrom(clazz());
        }

        public @Override Class<?> getType() {
            return clazz();
        }

        public @Override Object getInstance() {
            Object o = object; // keeping local copy to avoid another
            return o;
        }

        public @Override String getDisplayName() {
            return clazz().getName();
        }

        public @Override String getId() {
            return clazz().getName();
        }

        protected @Override boolean creatorOf(Object obj) {
            return obj == object;
        }

    }
    private static final class CantInstantiate {
        final Class<?> clazz;

        public CantInstantiate(Class<?> clazz) {
            assert clazz != null;
            this.clazz = clazz;
        }
    }
}
