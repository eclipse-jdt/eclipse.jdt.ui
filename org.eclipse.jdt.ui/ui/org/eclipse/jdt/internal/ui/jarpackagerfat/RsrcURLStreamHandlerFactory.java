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

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * If this class is modified the binary jar-rsrc-loader.zip has to be rebuilt using the ANT script
 * build_jar-rsrc-loader.xml.
 * 
 * @since 3.5
 */
public class RsrcURLStreamHandlerFactory implements URLStreamHandlerFactory {

	private ClassLoader classLoader;
	private URLStreamHandlerFactory chainFac;
	
	public RsrcURLStreamHandlerFactory(ClassLoader cl) {
		this.classLoader = cl;
	}

	public URLStreamHandler createURLStreamHandler(String protocol) {
		if ("rsrc".equals(protocol)) //$NON-NLS-1$
			return new RsrcURLStreamHandler(classLoader);
		if (chainFac != null)
			return chainFac.createURLStreamHandler(protocol);
		return null;
	}
	
	/**
	 * Allow one other URLStreamHandler to be added.
	 * URL.setURLStreamHandlerFactory does not allow
	 * multiple factories to be added.
	 * The chained factory is called for all other protocols,
	 * except "rsrc". Use null to clear previously set Handler. 
	 * @param fac another factory to be chained with ours.
	 */
	public void setURLStreamHandlerFactory(URLStreamHandlerFactory fac) {
		chainFac = fac;
	}
	
}
