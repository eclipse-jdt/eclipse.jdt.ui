/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ferenc Hechler - initial API and implementation
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 219530 [jar application] add Jar-in-Jar ClassLoader option
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 262746 [jar exporter] Create a builder for jar-in-jar-loader.zip
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 262748 [jar exporter] extract constants for string literals in JarRsrcLoader et al.
 *******************************************************************************/
package org.eclipse.jdt.internal.jarinjarloader;

import java.io.IOException;
import java.net.URL;

/**
 * This class will be compiled into the binary jar-in-jar-loader.zip. This ZIP is used for the
 * "Runnable JAR File Exporter"
 *
 * Handle URLs with protocol "rsrc". "rsrc:path/file.ext" identifies the content accessible as
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

	@Override
	protected java.net.URLConnection openConnection(URL u) throws IOException {
    	return new RsrcURLConnection(u, classLoader);
    }

	@Override
    protected void parseURL(URL url, String spec, int start, int limit) {
    	String file;
    	if (spec.startsWith(JIJConstants.INTERNAL_URL_PROTOCOL_WITH_COLON))
    		file = spec.substring(5);
    	else if (JIJConstants.CURRENT_DIR.equals(url.getFile()))
    		file = spec;
    	else if (url.getFile().endsWith(JIJConstants.PATH_SEPARATOR))
    		file = url.getFile() + spec;
		else if (JIJConstants.RUNTIME.equals(spec))
    		file = url.getFile();
    	else
    		file = spec;
    	setURL(url, JIJConstants.INTERNAL_URL_PROTOCOL, "", -1, null, null, file, null, null);	 //$NON-NLS-1$
    }

}
