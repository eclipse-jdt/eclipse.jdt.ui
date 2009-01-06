/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ferenc Hechler - initial API and implementation
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 219530 [jar application] add Jar-in-Jar ClassLoader option
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackagerfat;

import java.io.IOException;
import java.net.URL;

/**
 * If this class is modified the binary jar-rsrc-loader.zip has to be rebuilt using the ANT script
 * build_jar-rsrc-loader.xml.
 * 
 * Handle URLs with protocol "rsrc". "rsrc:path/file.ext" identifies the content accessiblr as
 * classLoader.getResourceAsStream("path/file.ext"). "rsrc:path/" identifies a base-path for
 * resources to be searched. The spec "file.ext" is combined to "rsrc:path/file.ext".
 * 
 * @since 3.5
 */
public class RsrcURLStreamHandler extends java.net.URLStreamHandler {

	private ClassLoader classLoader;
	
	public RsrcURLStreamHandler(ClassLoader classLoader) {
    	this.classLoader = classLoader;
	}

	protected java.net.URLConnection openConnection(URL u) throws IOException {
    	return new RsrcURLConnection(u, classLoader);
    }

    protected void parseURL(URL url, String spec, int start, int limit) {
    	String file;
    	if (spec.startsWith("rsrc:"))  //$NON-NLS-1$
    		file = spec.substring(5);
    	else if (url.getFile().equals("./")) //$NON-NLS-1$
    		file = spec;
    	else if (url.getFile().endsWith("/")) //$NON-NLS-1$
    		file = url.getFile() + spec;
    	else 
    		file = spec;
    	setURL(url, "rsrc", "", -1, null, null, file, null, null);	 //$NON-NLS-1$ //$NON-NLS-2$
    }

}
