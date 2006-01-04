/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.text.java;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;


/**
 * Computes completions and context information displayed by the Java editor content assistant.
 * <p>
 * XXX this API is provisional and may change anytime during the course of 3.2
 * </p>
 * 
 * @since 3.2
 */
public interface IJavaCompletionProposalComputer {

	/**
	 * Returns a list of completion proposals valid at the given invocation context.
	 * 
	 * @param context the context of the content assist invocation
	 * @param monitor a progress monitor to report progress. The monitor is private to this
	 *        invocation, i.e. there is no need for the receiver to spawn a sub monitor.
	 * @return an array of completion proposals (element type: {@link ICompletionProposal})
	 */
	List computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor);

	/**
	 * Returns context information objects valid at the given invocation context.
	 * 
	 * @param context the context of the content assist invocation
	 * @param monitor a progress monitor to report progress. The monitor is private to this
	 *        invocation, i.e. there is no need for the receiver to spawn a sub monitor.
	 * @return an array of context information objects (element type: {@link IContextInformation})
	 */
	List computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor);

	/**
	 * Returns the reason why this computer was unable to produce any completion proposals or
	 * context information.
	 * 
	 * @return an error message or <code>null</code> if no error occurred
	 */
	String getErrorMessage();
}
