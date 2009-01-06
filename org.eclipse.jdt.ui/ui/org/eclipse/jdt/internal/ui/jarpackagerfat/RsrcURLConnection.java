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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * If this class is modified the binary jar-rsrc-loader.zip has to be rebuilt using the ANT script
 * build_jar-rsrc-loader.xml.
 * 
 * @since 3.5
 */
public class RsrcURLConnection extends URLConnection {

	private ClassLoader classLoader;

	public RsrcURLConnection(URL url, ClassLoader classLoader) {
		super(url);
		this.classLoader= classLoader;
	}

	public void connect() throws IOException {
	}

	public InputStream getInputStream() throws IOException {
		String file= url.getFile();
		InputStream result= classLoader.getResourceAsStream(file);
		if (result == null) {
			throw new MalformedURLException("Could not open InputStream for URL '" + url + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}


}
