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
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

/**
 * A Java string context can be used to annotate a </code>RefactoringStatusEntry<code> 
 * with detailed information about an error detected in Java source code represented
 * by a string.
 */
public class JavaStringStatusContext extends RefactoringStatusContext {
	
	private String fSource;
	private ISourceRange fSourceRange;

	/**
	 * Creates a new <code>JavaStringStatusContext</code>.
	 * 
	 * @param source the source code containing the error
	 * @param range a source range inside <code>source</code> or
	 *  <code>null</code> if no special source range is known.
	 */	
	public JavaStringStatusContext(String source, ISourceRange range){
		Assert.isNotNull(source);
		fSource= source;
		fSourceRange= range;
	}
	
	public String getSource() {
		return fSource;
	}

	public ISourceRange getSourceRange() {
		return fSourceRange;
	}
	
	/* (non-Javadoc)
	 * @see RefactoringStatusContext#getCorrespondingElement()
	 */
	public Object getCorrespondingElement() {
		return null;
	}
}
