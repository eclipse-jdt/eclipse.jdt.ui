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

import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;

/**
 * Auto edit strategy that escapes non ISO-8859-1 characters.
 * 
 * @since 3.7
 */
public class PropertiesFileAutoEditStratergy implements IAutoEditStrategy {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.text.IAutoEditStrategy#customizeDocumentCommand(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.DocumentCommand)
	 */
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
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
			case '\b':
				return "\b";//$NON-NLS-1$
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
