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

import java.util.List;

import org.eclipse.core.runtime.CoreException;

/**
  */
public interface IAssistProcessor {
	
	/**
	 * Returns true if the processor has propsals for the given location. This test can used the passed AST
	 * and should be precice (to a guess). 
	 */
	boolean hasAssists(IAssistContext context) throws CoreException;

	/**
	 * Collects assists for the given context
	 * @param context Defines current compilation unit, position and a shared AST
	 * @param resultingCollections 
	 */
	void process(IAssistContext context, List resultingCollections) throws CoreException;
	
}
