/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import java.util.Collection;

import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

public interface IOccurrencesFinder {
	
	public String initialize(CompilationUnit root, int offset, int length);
	
	public String initialize(CompilationUnit root, ASTNode node);

	public String getJobLabel();

	/**
	 * Returns the plural label for this finder with 3 placeholders:
	 * <ul>
	 * <li>{0} for the {@link #getElementName() element name}</li>
	 * <li>{1} for the number of results found</li>
	 * <li>{2} for the scope (name of the compilation unit)</li>
	 *  </ul>
	 * @return the unformatted label
	 */
	public String getUnformattedPluralLabel();
	
	/**
	 * Returns the singular label for this finder with 2 placeholders:
	 * <ul>
	 * <li>{0} for the {@link #getElementName() element name}</li>
	 * <li>{1} for the scope (name of the compilation unit)</li>
	 *  </ul>
	 * @return the unformatted label
	 */
	public String getUnformattedSingularLabel();
	
	/**
	 * Returns the name of the element to look for or <code>null</code> if the finder hasn't
	 * been initialized yet.
	 * @return the name of the element
	 */
	public String getElementName();

	/**
	 * Collects matches for all occurrences. API avoids search plugin activation
	 * 
	 * @param resultingMatches the resulting matches of type {@link Match}
	 */
	public void collectMatches(Collection resultingMatches);
		
	/**
	 * Returns the id of this finder.
	 * @return returns the id of this finder.
	 */
	public String getID();

}
