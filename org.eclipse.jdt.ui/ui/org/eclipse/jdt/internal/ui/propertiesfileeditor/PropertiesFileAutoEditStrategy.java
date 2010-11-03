/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * Auto edit strategy that escapes characters if they cannot be encoded in the .properties file's
 * encoding.
 * 
 * <p>
 * Backslashes are escaped only when the pasted text is not perfectly correct for the .properties
 * file, i.e. if the text contains
 * <ul>
 * <li>a single backslash that is not followed by <code>t,n,f,r,u,\</code></li>
 * <li>a character which requires Unicode escapes</li>
 * </ul>
 * Escaping of backslash is controlled by the preference
 * {@link PreferenceConstants#PROPERTIES_FILE_WHEN_PASTING_ESCAPE_BACKSLASH_IF_REQUIRED}
 * </p>
 * 
 * @since 3.7
 */
public class PropertiesFileAutoEditStrategy implements IAutoEditStrategy {

	private final IPreferenceStore fPreferenceStore;
	private final IFile fFile;
	private String fCharsetName;
	private CharsetEncoder fCharsetEncoder;

	public PropertiesFileAutoEditStrategy(IPreferenceStore preferenceStore, IFile file) {
		fPreferenceStore= preferenceStore;
		fFile= file;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.IAutoEditStrategy#customizeDocumentCommand(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.DocumentCommand)
	 */
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
		try {
			String charsetName= fFile.getCharset();
			if (!charsetName.equals(fCharsetName)) {
				fCharsetName= charsetName;
				fCharsetEncoder= Charset.forName(fCharsetName).newEncoder();
			}
		} catch (CoreException e) {
			return;
		}

		String text= command.text;
		boolean escapeUnicodeChars= !fCharsetEncoder.canEncode(text);
		boolean escapeSlash= (text.length() > 1)
				&& fPreferenceStore.getBoolean(PreferenceConstants.PROPERTIES_FILE_WHEN_PASTING_ESCAPE_BACKSLASH_IF_REQUIRED) && (escapeUnicodeChars || shouldEscapeSlashes(text));

		if (!escapeUnicodeChars && !escapeSlash)
			return;

		command.text= escape(text, escapeUnicodeChars, escapeSlash);
	}

	/**
	 * Tests if the backslashes should be escaped in the given text.
	 * 
	 * @param text the text
	 * @return <code>true</code> if backslashes need to be escaped, <code>false</code> otherwise
	 */
	private boolean shouldEscapeSlashes(String text) {
		int length= text.length();
		for (int i= 0; i < length; i++) {
			char c= text.charAt(i);
			if (c == '\\') {
				if (i < length - 1) {
					char nextC= text.charAt(i + 1);
					switch (nextC) {
						case 't':
						case 'n':
						case 'f':
						case 'r':
						case 'u':
							break;
						case '\\':
							i++;
							break;
						default:
							return true;
					}
				}else {
					return true;
				}
			}
		}
		return false;
	}

	private static String escape(String s, boolean escapeUnicodeChars, boolean escapeSlash) {
		StringBuffer sb= new StringBuffer(s.length());
		int length= s.length();
		for (int i= 0; i < length; i++) {
			char c= s.charAt(i);
			sb.append(escape(c, escapeUnicodeChars, escapeSlash));
		}
		return sb.toString();
	}

	private static String escape(char c, boolean escapeUnicodeChars, boolean escapeSlash) {
		switch (c) {
			case '\t':
				return "\t";//$NON-NLS-1$
			case '\n':
				return "\n";//$NON-NLS-1$
			case '\f':
				return "\f";//$NON-NLS-1$
			case '\r':
				return "\r";//$NON-NLS-1$
			case '\\':
				return escapeSlash ? "\\\\" : "\\"; //$NON-NLS-1$ //$NON-NLS-2$
			default:
				return escapeUnicodeChars ? PropertiesFileEscapes.escape(c) : String.valueOf(c);
		}
	}
}
