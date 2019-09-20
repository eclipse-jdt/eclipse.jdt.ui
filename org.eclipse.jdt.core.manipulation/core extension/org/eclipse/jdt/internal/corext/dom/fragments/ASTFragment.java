/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom.fragments;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * @see org.eclipse.jdt.internal.corext.dom.fragments.IASTFragment
 * @see org.eclipse.jdt.internal.corext.dom.fragments.ASTFragmentFactory
 */
abstract class ASTFragment implements IASTFragment {

	/**
	 * Tries to create or find as many fragments as possible
	 * such that each fragment f matches
	 * this fragment and f.getNode() is <code>node</code>
	 */
	abstract IASTFragment[] getMatchingFragmentsWithNode(ASTNode node);
}

