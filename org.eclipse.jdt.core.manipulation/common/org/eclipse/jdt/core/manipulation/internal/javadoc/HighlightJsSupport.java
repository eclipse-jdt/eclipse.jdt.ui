/*******************************************************************************
 * Copyright (c) 2026 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.core.manipulation.internal.javadoc;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import org.eclipse.core.runtime.FileLocator;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;

/**
 * Provides highlight.js integration for syntax highlighting of non-Java fenced code blocks
 * in markdown Javadoc comments. Downloads of highlight.js resources are managed via the
 * Maven build (see pom.xml).
 */
public class HighlightJsSupport {

	private static final String RESOURCE_SCRIPT= "/resources/highlight.min.js/highlight.min.js"; //$NON-NLS-1$
	private static final String RESOURCE_CSS_LIGHT= "/resources/highlight.min.js/styles/intellij-light.min.css"; //$NON-NLS-1$
	private static final String RESOURCE_CSS_DARK= "/resources/highlight.min.js/styles/vs2015.min.css"; //$NON-NLS-1$

	private static final String INIT_SCRIPT= "hljs.configure({cssSelector: 'pre code[class*=\"language-\"]'});hljs.highlightAll();"; //$NON-NLS-1$
	private static final String INIT_SCRIPT_HASH= computeSha256(INIT_SCRIPT);

	private HighlightJsSupport() {
	}

	static String fileUrlScript;
	static String fileUrlCss;
	static String fileUrlCssDark;
	static String hljsFileHash;
	static boolean UrlsInitialized;
	/**
	 * Returns the HTML snippet to inject into &lt;head&gt; for highlight.js support,
	 * including the CSS link, script tag, and highlightAll() invocation.
	 *
	 * @param isDark - true if the current theme is a dark theme
	 *
	 * @return the HTML snippet, or an empty string if resources cannot be resolved
	 */
	public static synchronized String getHighlightJsHeader(boolean isDark) {
		try {
			if (!UrlsInitialized) {
				Class<?> pluginClass= JavaManipulationPlugin.getDefault().getClass();
				URL urlScript= pluginClass.getResource(RESOURCE_SCRIPT);
				URL urlCss= pluginClass.getResource(RESOURCE_CSS_LIGHT);
				URL urlCssDark= pluginClass.getResource(RESOURCE_CSS_DARK);

				fileUrlScript= urlScript == null
						? null
								: new File(FileLocator.toFileURL(urlScript).getPath()).toURI().toASCIIString().replace("'", "%27"); //$NON-NLS-1$ //$NON-NLS-2$
				fileUrlCss= urlCss == null
						? null
								: new File(FileLocator.toFileURL(urlCss).getPath()).toURI().toASCIIString().replace("'", "%27"); //$NON-NLS-1$ //$NON-NLS-2$
				fileUrlCssDark= urlCssDark == null
						? null
								: new File(FileLocator.toFileURL(urlCssDark).getPath()).toURI().toASCIIString().replace("'", "%27"); //$NON-NLS-1$ //$NON-NLS-2$
				hljsFileHash= computeSha384(pluginClass, RESOURCE_SCRIPT);
				UrlsInitialized= true;
			}
			String urlCss= isDark ? fileUrlCssDark : fileUrlCss;
			if (fileUrlScript != null && urlCss != null && hljsFileHash != null) {
				String csp= "<meta http-equiv=\"Content-Security-Policy\" content=\"" //$NON-NLS-1$
						+ "default-src 'none'; " //$NON-NLS-1$
						+ "script-src 'sha384-" + hljsFileHash + "' 'sha256-" + INIT_SCRIPT_HASH + "'; " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ "style-src 'unsafe-inline' file:; img-src * data: file:; font-src file:\">"; //$NON-NLS-1$
				return csp
						+ "<link rel='stylesheet' href='" + urlCss + "'>" //$NON-NLS-1$ //$NON-NLS-2$
						+ "<style>.hljs{background:transparent;padding:0;}</style>" //$NON-NLS-1$
						+ "<script src='" + fileUrlScript + "' integrity='sha384-" + hljsFileHash + "'></script>" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ "<script>" + INIT_SCRIPT + "</script>"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (Exception e) {
			JavaManipulationPlugin.log(e);
		}
		return "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; script-src 'none'; style-src 'unsafe-inline' file:; img-src * data: file:; font-src file:\">"; //$NON-NLS-1$
	}

	private static String computeSha256(String text) {
		try {
			MessageDigest digest= MessageDigest.getInstance("SHA-256"); //$NON-NLS-1$
			return Base64.getEncoder().encodeToString(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			JavaManipulationPlugin.log(e);
			return null;
		}
	}

	private static String computeSha384(Class<?> pluginClass, String resourcePath) {
		try (InputStream is= pluginClass.getResourceAsStream(resourcePath)) {
			if (is != null) {
				MessageDigest digest= MessageDigest.getInstance("SHA-384"); //$NON-NLS-1$
				byte[] buf= new byte[8192];
				int n;
				while ((n= is.read(buf)) != -1) {
					digest.update(buf, 0, n);
				}
				return Base64.getEncoder().encodeToString(digest.digest());
			}
		} catch (Exception e) {
			JavaManipulationPlugin.log(e);
		}
		return null;
	}

}
