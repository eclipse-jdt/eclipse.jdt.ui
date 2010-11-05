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

import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;

/**
 * Auto edit strategy that escapes a character if it cannot be encoded in the .properties file's
 * encoding.
 * 
 * @since 3.7
 */
public class PropertiesFileAutoEditStrategy implements IAutoEditStrategy {

	private final IFile fFile;
	private String fCharsetName;
	private CharsetEncoder fCharsetEncoder;

	public PropertiesFileAutoEditStrategy(IFile file) {
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

		if (fCharsetEncoder.canEncode(command.text))
			return;

		command.text= escape(command.text);
	}

	private static String escape(String s) {
		StringBuffer sb= new StringBuffer(s.length());
		int length= s.length();
		for (int i= 0; i < length; i++) {
			char c= s.charAt(i);
			sb.append(escape(c));
		}
		return sb.toString();
	}

	private static String escape(char c) {
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
				return "\\";//$NON-NLS-1$

			default:
				return PropertiesFileEscapes.escape(c);
		}
	}
}
