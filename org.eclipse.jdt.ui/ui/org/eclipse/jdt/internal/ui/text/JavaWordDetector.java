/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text;


import org.eclipse.jface.text.rules.IWordDetector;

/**
 * A Java aware word detector.
 */
public class JavaWordDetector implements IWordDetector, IVersionDependent {

	private boolean fIsJLS3= false;
	
	/*
	 * @see IWordDetector#isWordStart
	 */
	public boolean isWordStart(char c) {
		return Character.isJavaIdentifierStart(c) || fIsJLS3 && c == '@';
	}
	
	/*
	 * @see IWordDetector#isWordPart
	 */
	public boolean isWordPart(char c) {
		return Character.isJavaIdentifierPart(c);
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.IVersionDependent#setCurrentVersion(java.lang.String)
	 */
	public void setCurrentVersion(String version) {
		fIsJLS3= "1.5".compareTo(version) <= 0; //$NON-NLS-1$
	}
}
