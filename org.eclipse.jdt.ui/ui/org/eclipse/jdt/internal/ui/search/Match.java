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
package org.eclipse.jdt.internal.ui.search;


/**
 * This class is a wrapper for {@link org.eclipse.search.ui.text.Match}
 * in order to prevent the loading of the Search plug-in when the VM
 * verifies some JDT UI code. Use it only when needed in those cases.
 * 
 * @since 3.1
 */
class Match {

	private org.eclipse.search.ui.text.Match fMatch;

	/**
	 * Returns the wrapped search matches.
	 */
	static org.eclipse.search.ui.text.Match[] convert(Match[] jdtMatches) {
		if (jdtMatches == null)
			return null;
		
		int length= jdtMatches.length;
		org.eclipse.search.ui.text.Match[] matches= new org.eclipse.search.ui.text.Match[length];
		for (int i= 0; i < length; i++)
			matches[i]= jdtMatches[i].fMatch;
		return matches;
	}

	/*
	 * @see org.eclipse.search.ui.text.Match#Match(Object, int, int, int)
	 */
	Match(Object element, int unit, int offset, int length) {
		fMatch= new org.eclipse.search.ui.text.Match(element, unit, offset, length);
	}
	
	/*
	 * @see org.eclipse.search.ui.text.Match#Match(Object, int, int)
	 */
	Match(Object element, int offset, int length) {
		fMatch= new org.eclipse.search.ui.text.Match(element, offset, length);
	}
}
