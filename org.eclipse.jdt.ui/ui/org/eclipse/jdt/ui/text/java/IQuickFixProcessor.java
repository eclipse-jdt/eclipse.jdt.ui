/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * 
 *******************************************************************************/
package org.eclipse.jdt.ui.text.java;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;


/**
 * Interface to be implemented by contributors to the extension point
 * <code>org.eclipse.jdt.ui.quickFixProcessors</code>.
 *
 * @since 3.0
 */
public interface IQuickFixProcessor {

	/**
	 * Returns <code>true</code> if the processor has proposals for the given problem. This test should be an
	 * optimistic guess and be very cheap.
	 * 
	 * @param unit the compilation unit
	 * @param problemId the problem Id
	 * @return <code>true</code> if the processor has proposals for the given problem
	 */
	boolean hasCorrections(ICompilationUnit unit, int problemId);
	
	/**
	 * Collects corrections or code manipulations for the given context.
	 * 
	 * @param context Defines current compilation unit, position and a shared AST
	 * @param locations Problems are the current location.
	 * @return the corrections applicable at the location or <code>null</code> if no proposals
	 * 			can be offered
	 * @throws CoreException CoreException can be thrown if the operation fails
	 */
	IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations) throws CoreException;
	
}
