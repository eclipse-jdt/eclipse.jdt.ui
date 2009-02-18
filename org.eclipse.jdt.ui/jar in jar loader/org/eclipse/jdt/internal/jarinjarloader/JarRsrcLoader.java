/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ferenc Hechler - initial API and implementation
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 219530 [jar application] add Jar-in-Jar ClassLoader option
 *     Ferenc Hechler, ferenc_hechler@users.sourceforge.net - 262746 [jar exporter] Create a builder for jar-in-jar-loader.zip
 *******************************************************************************/
package org.eclipse.jdt.internal.jarinjarloader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * This class will be compiled into the binary jar-in-jar-loader.zip. This ZIP is used for the
 * "Runnable JAR File Exporter"
 * 
 * @since 3.5
 */
public class JarRsrcLoader {
 
	private static class ManifestInfo {
		String rsrcMainClass;
		String[] rsrcClassPath;
	}
	
	public static void main(String[] args) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException, IOException {
		ManifestInfo mi = getManifestInfo();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL.setURLStreamHandlerFactory(new RsrcURLStreamHandlerFactory(cl));
		URL[] rsrcUrls = new URL[mi.rsrcClassPath.length];
		for (int i = 0; i < mi.rsrcClassPath.length; i++) {
			String rsrcPath = mi.rsrcClassPath[i];
			if (rsrcPath.endsWith("/")) //$NON-NLS-1$
				rsrcUrls[i] = new URL("rsrc:" + rsrcPath); //$NON-NLS-1$
			else
				rsrcUrls[i] = new URL("jar:rsrc:" + rsrcPath+ "!/");  //$NON-NLS-1$//$NON-NLS-2$
		}
		ClassLoader jceClassLoader = new URLClassLoader(rsrcUrls, null);
		Thread.currentThread().setContextClassLoader(jceClassLoader);
		Class c = Class.forName(mi.rsrcMainClass, true, jceClassLoader);
		Method main = c.getMethod("main", new Class[]{args.getClass()}); //$NON-NLS-1$
		main.invoke((Object)null, new Object[]{args});
	}

	private static ManifestInfo getManifestInfo() throws IOException {
		Enumeration resEnum;
		resEnum = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		while (resEnum.hasMoreElements()) {
			try {
				URL url = (URL)resEnum.nextElement();
				InputStream is = url.openStream();
				if (is != null) {
					ManifestInfo result = new ManifestInfo();
					Manifest manifest = new Manifest(is);
					Attributes mainAttribs = manifest.getMainAttributes();
					result.rsrcMainClass = mainAttribs.getValue("Rsrc-Main-Class"); //$NON-NLS-1$
					String rsrcCP = mainAttribs.getValue("Rsrc-Class-Path"); //$NON-NLS-1$
					if (rsrcCP == null)
						rsrcCP = ""; //$NON-NLS-1$
					result.rsrcClassPath = splitSpaces(rsrcCP);
					if ((result.rsrcMainClass != null) && !result.rsrcMainClass.trim().equals(""))  //$NON-NLS-1$
							return result;
				}
			}
			catch (Exception e) {
				// Silently ignore wrong manifests on classpath?
			}
		}
		System.err.println("Missing attributes for RsrcLoader in Manifest (Rsrc-Main-Class, Rsrc-Class-Path)"); //$NON-NLS-1$
		return null;
	}

	/**
	 * JDK 1.3.1 does not support String.split(), so we have to do it manually. Skip all spaces
	 * (tabs are not handled)
	 * 
	 * @param line the line to split
	 * @return array of strings
	 */
	private static String[] splitSpaces(String line) {
		if (line == null) 
			return null;
		List result = new ArrayList();
		int firstPos = 0;
		while (firstPos < line.length()) {
			int lastPos = line.indexOf(' ', firstPos);
			if (lastPos == -1)
				lastPos = line.length();
			if (lastPos > firstPos) {
				result.add(line.substring(firstPos, lastPos));
			}
			firstPos = lastPos+1; 
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

}
