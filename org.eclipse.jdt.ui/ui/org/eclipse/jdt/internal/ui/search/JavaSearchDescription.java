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

import org.eclipse.jdt.core.IJavaElement;

public class JavaSearchDescription {
	private int fLimitTo;
	private IJavaElement fElementPattern;
	private String fStringPattern;
	private String fScopeDescription;

	public IJavaElement getElementPattern() {
		return fElementPattern;
	}

	public int getLimitTo() {
		return fLimitTo;
	}

	public String getStringPattern() {
		return fStringPattern;
	}

	public String getScopeDescription() {
		return fScopeDescription;
	}

	/**
	 * @param limitTo
	 * @param elementPattern
	 * @param stringPattern
	 * @param scopeDescription
	 */
	public JavaSearchDescription(
		int limitTo,
		IJavaElement elementPattern,
		String stringPattern,
		String scopeDescription) {
		super();
		fLimitTo= limitTo;
		fElementPattern= elementPattern;
		fStringPattern= stringPattern;
		fScopeDescription= scopeDescription;
	}
}
