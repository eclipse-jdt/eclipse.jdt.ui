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

import java.net.URL;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;

/**
 * Provides highlight.js integration for syntax highlighting of non-Java fenced code blocks
 * in markdown Javadoc comments. Downloads of highlight.js resources are managed via the
 * Maven build (see pom.xml).
 */
public class HighlightJsSupport {

	private static final String THEME_PLUGIN_ID= "org.eclipse.e4.ui.css.swt.theme"; //$NON-NLS-1$
	private static final String THEME_PREF_KEY= "themeid"; //$NON-NLS-1$
	private static final String DARK_THEME_TOKEN= "dark"; //$NON-NLS-1$

	private static final String RESOURCE_SCRIPT= "/resources/highlight.min.js/highlight.min.js"; //$NON-NLS-1$
	private static final String RESOURCE_CSS_LIGHT= "/resources/highlight.min.js/styles/intellij-light.min.css"; //$NON-NLS-1$
	private static final String RESOURCE_CSS_DARK= "/resources/highlight.min.js/styles/vs2015.min.css"; //$NON-NLS-1$

	private static final Pattern SCRIPT_TAG= Pattern.compile(
			"<script[^>]*>.*?</script>|<script[^>]*/>", //$NON-NLS-1$
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private static final Pattern EVENT_HANDLER= Pattern.compile(
			"\\bon\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]*)", //$NON-NLS-1$
			Pattern.CASE_INSENSITIVE);

	private static final Pattern JAVASCRIPT_URI= Pattern.compile(
			"(href|src|action)\\s*=\\s*([\"'])\\s*javascript:", //$NON-NLS-1$
			Pattern.CASE_INSENSITIVE);

	private HighlightJsSupport() {
	}

	/**
	 * Returns the HTML snippet to inject into &lt;head&gt; for highlight.js support,
	 * including the CSS link, script tag, and highlightAll() invocation.
	 *
	 * @return the HTML snippet, or an empty string if resources cannot be resolved
	 */
	public static String getHighlightJsHeader() {
		try {
			Class<?> pluginClass= JavaManipulationPlugin.getDefault().getClass();
			URL urlScript= pluginClass.getResource(RESOURCE_SCRIPT);
			String cssPath= isDarkTheme() ? RESOURCE_CSS_DARK : RESOURCE_CSS_LIGHT;
			URL urlCss= pluginClass.getResource(cssPath);

			URL fileUrlScript= urlScript == null ? null : FileLocator.toFileURL(urlScript);
			URL fileUrlCss= urlCss == null ? null : FileLocator.toFileURL(urlCss);

			if (fileUrlScript != null && fileUrlCss != null) {
				return "<link rel='stylesheet' href='" + fileUrlCss + "'>" + //$NON-NLS-1$ //$NON-NLS-2$
						"<style>.hljs{background:transparent;}</style>" + //$NON-NLS-1$
						"<script src='" + fileUrlScript + "'></script>" + //$NON-NLS-1$ //$NON-NLS-2$
						"<script>hljs.highlightAll();</script>"; //$NON-NLS-1$
			}
		} catch (Exception e) {
			JavaManipulationPlugin.log(e);
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Sanitizes HTML content by removing script tags, event handler attributes,
	 * and javascript: URIs. This ensures that only the highlight.js scripts
	 * injected via {@link #getHighlightJsHeader()} can execute.
	 *
	 * @param html the HTML content to sanitize
	 * @return the sanitized HTML
	 */
	public static String sanitizeScripts(String html) {
		String result= SCRIPT_TAG.matcher(html).replaceAll(""); //$NON-NLS-1$
		result= EVENT_HANDLER.matcher(result).replaceAll(""); //$NON-NLS-1$
		result= JAVASCRIPT_URI.matcher(result).replaceAll("$1=$2"); //$NON-NLS-1$
		return result;
	}

	private static boolean isDarkTheme() {
		try {
			String themeId= Platform.getPreferencesService().getString(
					THEME_PLUGIN_ID, THEME_PREF_KEY, "", null); //$NON-NLS-1$
			return themeId.toLowerCase().contains(DARK_THEME_TOKEN);
		} catch (Exception e) {
			return false;
		}
	}
}
