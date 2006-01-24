/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.text.java;

import org.eclipse.core.runtime.IConfigurationElement;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

/**
 * Abstract base class for filters contributed to the
 * <code>org.eclipse.jdt.ui.javaCompletionProposalFilters</code> extension point.
 * <p>
 * Subclasses need to implement {@link #filter(ICompletionProposal)} and may override
 * {@link #beginFiltering(ContentAssistInvocationContext) beginFiltering} and
 * {@link #endFiltering() endFiltering}.
 * </p>
 * 
 * @since 3.2
 */
public abstract class AbstractProposalFilter {
	
	/**
	 * Creates a new filter. Note that subclasses must provide a zero-argument constructor to be
	 * instantiatable via {@link IConfigurationElement#createExecutableExtension(String)}.
	 */
	protected AbstractProposalFilter() {
	}

	/**
	 * Called once before the content assist session.
	 * <p>
	 * Clients may override, the default implementation does nothing.
	 * </p>
	 * 
	 * @param context the context of the content assist invocation
	 */
	public void beginFiltering(ContentAssistInvocationContext context) {
	}
	
	/**
	 * Called once after the content assist session.
	 * <p>
	 * Clients may override, the default implementation does nothing.
	 * </p>
	 */
	public void endFiltering() {
	}
	
	/**
	 * Returns <code>true</code> if a proposal is filtered, i.e. should not be displayed,
	 * <code>false</code> if it passes the filter.
	 * 
	 * @param proposal the proposal to be filtered
	 * @return <code>true</code> to filter (remove) <code>proposal</code>, <code>false</code>
	 *         to let it pass
	 */
	public abstract boolean filter(ICompletionProposal proposal);

}
