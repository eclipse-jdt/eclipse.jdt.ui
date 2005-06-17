/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IColorManager;

import org.eclipse.jdt.internal.ui.text.AbstractJavaScanner;
import org.eclipse.jdt.internal.ui.text.JavaWhitespaceDetector;


/**
 * A rule based property value scanner.
 *
 * @since 3.1
 */
public final class PropertyValueScanner extends AbstractJavaScanner {

	public class AssignmentDetector implements IWordDetector {

		/*
		 * @see IWordDetector#isWordStart
		 */
		public boolean isWordStart(char c) {
			if ('=' != c && ':' != c || fDocument == null)
				return false;

			try {
				// check whether it is the first '='
				IRegion lineInfo= fDocument.getLineInformationOfOffset(fOffset);
				int offset= lineInfo.getOffset();
				String line= fDocument.get(offset, lineInfo.getLength());
				int i= line.indexOf(c);
				return i != -1 && i + lineInfo.getOffset() + 1 == fOffset;
			} catch (BadLocationException ex) {
				return false;
			}
		}

		/*
		 * @see IWordDetector#isWordPart
		 */
		public boolean isWordPart(char c) {
			return false;
		}
	}


	private static String[] fgTokenProperties= {
		PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE,
		PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT,
		PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT
	};


	/**
	 * Creates a property value code scanner
	 *
	 * @param manager	the color manager
	 * @param store		the preference store
	 */
	public PropertyValueScanner(IColorManager manager, IPreferenceStore store) {
		super(manager, store);
		initialize();
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.AbstractJavaScanner#getTokenProperties()
	 */
	protected String[] getTokenProperties() {
		return fgTokenProperties;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.AbstractJavaScanner#createRules()
	 */
	protected List createRules() {
		setDefaultReturnToken(getToken(PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE));
		List rules= new ArrayList();

		// Add rule for arguments.
		IToken token= getToken(PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT);
		rules.add(new ArgumentRule(token));

		// Add word rule for assignment operator.
		token= getToken(PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT);
		WordRule wordRule= new WordRule(new AssignmentDetector(), token);
		rules.add(wordRule);

		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new JavaWhitespaceDetector()));

		return rules;
	}
}
