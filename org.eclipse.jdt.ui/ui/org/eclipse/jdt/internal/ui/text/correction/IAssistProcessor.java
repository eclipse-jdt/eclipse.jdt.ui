/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

/**
  */
public interface IAssistProcessor {
	
	
	/**
	 * Evluates if assist can be created for the given context. This evaluation must be precise.
	 */
	public boolean hasAssists(IAssistContext context) throws CoreException;
	
	
	/**
	 * Collects assists for the given context
	 * @param context Defines current compilation unit, position and a shared AST
	 * @param location the locations of problems at the invoked offset. The processor can descide to only
	 * add assists when there no problems at the selection offset.
	 * @return Returns the assist applicable at the location or <code>null</code> if no proposals
	 * can be offered.
	 */
	IJavaCompletionProposal[] getAssists(IAssistContext context, IProblemLocation[] locations) throws CoreException;
	
}
