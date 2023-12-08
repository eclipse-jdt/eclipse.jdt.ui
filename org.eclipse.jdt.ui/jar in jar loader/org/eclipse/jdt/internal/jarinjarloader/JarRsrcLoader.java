/*******************************************************************************
 * Copyright (c) 2008, 2023 IBM Corporation and others.
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
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This class will be compiled into the binary jar-in-jar-loader.zip. This ZIP is used for the
 * "Runnable JAR File Exporter".
 * Source has to comply to java 1.8 - see <a href="file:../../../../../../scripts/build_jar-in-jar-loader.xml">build_jar-in-jar-loader.xml</a>
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
			if (rsrcPath.endsWith(JIJConstants.PATH_SEPARATOR))
				rsrcUrls[i] = new URL(JIJConstants.INTERNAL_URL_PROTOCOL_WITH_COLON + rsrcPath);
			else
				rsrcUrls[i] = new URL(JIJConstants.JAR_INTERNAL_URL_PROTOCOL_WITH_COLON + rsrcPath + JIJConstants.JAR_INTERNAL_SEPARATOR);
		}
		@SuppressWarnings("resource")
		ClassLoader jceClassLoader = new URLClassLoader(rsrcUrls, getParentClassLoader());
		Thread.currentThread().setContextClassLoader(jceClassLoader);
		Class<?> c = Class.forName(mi.rsrcMainClass, true, jceClassLoader);
		Method main = c.getMethod(JIJConstants.MAIN_METHOD_NAME, args.getClass());
		main.invoke((Object) null, new Object[] {args});
	}

	private static ClassLoader getParentClassLoader() throws InvocationTargetException, IllegalAccessException {
		// On Java8, it is ok to use a null parent class loader, but, starting with Java 9,
		// we need to provide one that has access to the restricted list of packages that
		// otherwise would produce a SecurityException when loaded
		try {
			// We use reflection here because the method ClassLoader.getPlatformClassLoader()
			// is only present starting from Java 9
			Method platformClassLoader = ClassLoader.class.getMethod("getPlatformClassLoader", (Class[])null); //$NON-NLS-1$
			return (ClassLoader) platformClassLoader.invoke(null, (Object[]) null);
		} catch (NoSuchMethodException e) {
			// This is a safe value to be used on Java 8 and previous versions
			return null;
		}
	}

	private static ManifestInfo getManifestInfo() throws IOException {
		Enumeration<URL> resEnum;
		resEnum = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
		while (resEnum.hasMoreElements()) {
			try (InputStream is =  resEnum.nextElement().openStream()){
				if (is != null) {
					ManifestInfo result = new ManifestInfo();
					Manifest manifest = new Manifest(is);
					Attributes mainAttribs = manifest.getMainAttributes();
					result.rsrcMainClass = mainAttribs.getValue(JIJConstants.REDIRECTED_MAIN_CLASS_MANIFEST_NAME);
					String rsrcCP = mainAttribs.getValue(JIJConstants.REDIRECTED_CLASS_PATH_MANIFEST_NAME);
					if (rsrcCP == null)
						rsrcCP = JIJConstants.DEFAULT_REDIRECTED_CLASSPATH;
					result.rsrcClassPath = splitSpaces(rsrcCP);
					if ((result.rsrcMainClass != null) && !result.rsrcMainClass.trim().isEmpty())
							return result;
				}
			}
			catch (Exception e) {
				// Silently ignore wrong manifests on classpath?
			}
		}
		System.err.println("Missing attributes for JarRsrcLoader in Manifest ("+JIJConstants.REDIRECTED_MAIN_CLASS_MANIFEST_NAME+", "+JIJConstants.REDIRECTED_CLASS_PATH_MANIFEST_NAME+")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		List<String> result = new ArrayList<>();
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
		return result.toArray(new String[result.size()]);
	}

}
