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
package org.eclipse.jdt.ui.search;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchScope;

/**
 * <p>
 * Describes a search query by giving the <code>IJavaElement</code> to search
 * for.
 * </p>
 * <p>
 * This class isn't intended to be instantiated to subclassed by clients.
 * </p>
 * 
 * @see org.eclipse.jdt.ui.search.QuerySpecification
 *
 * @since 3.0
 */
public class ElementQuerySpecification extends QuerySpecification {
	private IJavaElement fElement;

	/**
	 * A constructor.
	 * @param javaElement The java element the query should search for.
	 * @param limitTo		  The kind of occurrence the query should search for
	 * @param scope		  The scope to search in
	 * @param scopeDescription A human readable description of the search scope
	 */
	public ElementQuerySpecification(IJavaElement javaElement, int limitTo, IJavaSearchScope scope, String scopeDescription) {
		super(limitTo, scope, scopeDescription);
		fElement= javaElement;
	}
	
	/**
	 * Returns the element to be searched for.
	 * @return the element to search for
	 */
	public IJavaElement getElement() {
		return fElement;
	}
}
