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

package org.netbeans.modules.openide.filesystems.declmime;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.openide.filesystems.*;
import org.openide.util.*;
import org.openide.xml.*;


/**
 * Implements default interruptible silent parser behaviour.
 * Errors can be tested by quering parser state.
 *
 * @author  Petr Kuzel
 * @version 1.0
 */
abstract class DefaultParser  extends DefaultHandler {

    protected FileObject fo;
    private Locator locator = null;

    protected short state = INIT;

    protected static final short PARSED = 1000;
    protected static final short ERROR = -1;
    protected static final short INIT = 0;
    protected static final short LOAD = 1;

    private static final Logger LOG = Logger.getLogger(DefaultParser.class.getName());
    private static boolean xercesErrorInfoLogged = false;

    protected DefaultParser() {        
    }
    
    protected DefaultParser(FileObject fo) {
        this.fo = fo;
    }

    /**
     * Preconfigure parser and return it.
     */
    protected XMLReader createXMLReader() throws IOException, SAXException {
        return XMLUtil.createXMLReader(false);
    }

    /**
     * Check if the given exception is one thrown from the handler
     * for stopping the parser.
     */
    protected boolean isStopException(Throwable e) {
        return false;
    }

    /**
     * @return current parser state
     */
    protected short getState() {
        return state;
    }

    protected final Locator getLocator() {
        return locator;
    }
    
    /**
     * Parser content workarounding known parser implementation
     * problems.
     */
    protected final void parse(FileObject fo) {
        state = INIT; // #15672
        InputStream is = null;
        this.fo = fo;
        try {
            XMLReader parser = createXMLReader();
            parser.setEntityResolver(this);
            parser.setErrorHandler(this);
            parser.setContentHandler(this);

            
            try {
                // ignore wrong encoding, for example
                parser.setFeature("http://apache.org/xml/features/continue-after-fatal-error", 
                        true);  //NOI18N
                parser.setProperty(
                        "http://apache.org/xml/properties/input-buffer-size", //NOI18N
                        new Integer(2048)); // #230305
            } catch (SAXException ignore) {
                // parsing may be slower :-(
            }

            InputSource in = new InputSource();                
            is = fo.getInputStream();
            in.setByteStream(is);
            in.setSystemId(fo.toURL().toExternalForm());
            customizeInputSource(in);
            
            parser.parse(in);

        } catch (IOException io) {
            if (!isStopException(io)) {
                if (fo.isValid() && fo.canRead()) {
                    Exceptions.attachMessage(io, "While parsing: " + fo); // NOI18N
                    LOG.log(Level.INFO, null, io);
                    state = ERROR;
                }
            }
        } catch (SAXException sex) {
            if (!isStopException(sex)) {
                Exceptions.attachMessage(sex, "While parsing: " + fo); // NOI18N
                LOG.log(Level.INFO, null, sex);
                state = ERROR;
            }
        } catch (InternalError ie) {
            // Sometimes thrown on not really valid sources
            if(!isStopException(ie)) {
                state = ERROR;
            }
        } catch (NullPointerException npe) {
            if(!isStopException(npe)) {
                state = ERROR;
                logNPE(npe, fo);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    // already closed
                }
            }
        }                        
    }

    /**
     * Log NullPointerException thrown by a parser.
     *
     * Use Level FINE for NPEs thrown in com.sun.org.apache.xerces.internal.impl
     * package or its subpackages. See bug 126496 and bug 126496 and
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6743605.
     */
    private void logNPE(NullPointerException npe, FileObject fo) {
        StackTraceElement[] elements = npe.getStackTrace();
        if (elements.length > 0 && elements[0].getClassName().startsWith(
                "com.sun.org.apache.xerces.internal.impl")) { //NOI18N
            if (!xercesErrorInfoLogged && !LOG.isLoggable(Level.FINE)) {
                xercesErrorInfoLogged = true;
                LOG.log(Level.INFO, "Some problem occurred during" //NOI18N
                        + " parsing. Please set logging level for {0}" //NOI18N
                        + " to FINE for more details.", LOG.getName()); //NOI18N
            }
            LOG.log(Level.FINE, "While parsing: {0}", fo);              //NOI18N
            LOG.log(Level.FINE, null, npe);
        } else {
            // report all other NPEs
            Exceptions.attachMessage(npe, "While parsing: " + fo);      //NOI18N
            LOG.log(Level.INFO, null, npe);
        }
    }

    protected void customizeInputSource(InputSource in) {
    }
    
    /**
     * Parser default file object
     */
    protected final void parse() {
        if (fo == null) throw new NullPointerException();
        parse(fo);
    }

    /** Report error occured during custom validation. */
    protected void error() throws SAXException {
        String reason = org.openide.util.NbBundle.getMessage(DefaultParser.class, "Invalid_XML_document");
        error(reason);
    }

    /** Report error occured during custom validation. */
    protected void error(String reason) throws SAXException {
        StringBuffer buf = new StringBuffer (reason).append(": ").append(fo.toString());//NOI18N
        if (locator != null) {
            buf.append(" line: ").append(locator.getLineNumber());//NOI18N
            buf.append(" column: ").append(locator.getColumnNumber());//NOI18N
        }
        String msg = buf.toString();  //NOI18N
        SAXException sex = new SAXException(msg);
        throw sex;
    }

    public void error(SAXParseException exception) throws SAXException {
        throw exception;
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        throw exception;
    }

    public void endDocument() throws SAXException {
        state = PARSED;
    }

    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    public InputSource resolveEntity (String publicID, String systemID) {
        // Read nothing whatsoever.
        return new InputSource (new ByteArrayInputStream (new byte[] { }));
    }
    
}
