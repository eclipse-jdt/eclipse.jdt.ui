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

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

/**
  */
public interface ICorrectionProcessor {

	/**
	 * Returns true if the processor has propsals for the given problem. This test should be an
	 * optimistic guess and be extremly cheap. 
	 */
	boolean hasCorrections(ICompilationUnit unit, int problemId);
	
	/**
	 * Collects corrections or code manipulations for the given context
	 * @param context Defines current compilation unit, position and the problem ID.
	 * @param locations Problems ar the current location
	 * @return Returns the correction applicable at the location or <code>null</code> if no proposals
	 * can be offered.
	 */
	IJavaCompletionProposal[] getCorrections(IAssistContext context, IProblemLocation[] locations) throws CoreException;
	
}
