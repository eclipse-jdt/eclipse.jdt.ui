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
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import org.eclipse.jface.text.rules.IWordDetector;

/**
 * An assignment word detector.
 */
public final class AssignmentDetector implements IWordDetector {
	
	/*
	 * @see IWordDetector#isWordStart
	 */
	public boolean isWordStart(char c) {
		return '=' == c;
	}
	
	/*
	 * @see IWordDetector#isWordPart
	 */
	public boolean isWordPart(char c) {
		return false;
	}
}
