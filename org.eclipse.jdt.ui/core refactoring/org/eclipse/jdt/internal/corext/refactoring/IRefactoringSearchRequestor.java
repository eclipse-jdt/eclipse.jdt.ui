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
package org.eclipse.jdt.internal.corext.refactoring;

import org.eclipse.jdt.core.search.SearchMatch;

/**
 * Interface for search requestors used in conjunction with {@link org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2}.
 * 
 * @since 3.1
 */
public interface IRefactoringSearchRequestor {

	/**
	 * Can the search match be accepted?
	 * 
	 * @param match the search match to test
	 * @return <code>true</code> if the match could be accepted, <code>false</code> otherwise
	 */
	public boolean acceptSearchMatch(SearchMatch match);
}