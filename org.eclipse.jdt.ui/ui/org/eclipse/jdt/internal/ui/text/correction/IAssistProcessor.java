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

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;

/**
  */
public interface IAssistProcessor {
	
	
	/**
	 * Evluates if assist can be created for the given context. This evaluation must be precice.
	 */
	public boolean hasAssists(IAssistContext context) throws CoreException;
	
	
	/**
	 * Collects assists for the given context
	 * @param context Defines current compilation unit, position and a shared AST
	 * @param resultingCorrections The resulting proposals. The proposals must be of type
	 * <code>IJavaCompletionProposal</code>
	 */
	void process(IAssistContext context, IProblemLocation[] locations, Collection resultingCorrections) throws CoreException;
	
}
