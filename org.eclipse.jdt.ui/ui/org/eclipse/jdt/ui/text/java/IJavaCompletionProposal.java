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

package org.eclipse.jdt.ui.text.java;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

/**
 * CompletionProposal with a relevance value.
 * The relevance value is used to sort the completion proposals. Proposals with higher relevance
 * should be listed before proposals with lower relevance.
 */
public interface IJavaCompletionProposal extends ICompletionProposal {
		
	/**
	 * Returns the relevance of the completion proposal. It is an integer in the
	 * range of [0, 100], which is used to determine if this proposal is more
	 * relevant than another proposal.
	 */
	int getRelevance();

}
