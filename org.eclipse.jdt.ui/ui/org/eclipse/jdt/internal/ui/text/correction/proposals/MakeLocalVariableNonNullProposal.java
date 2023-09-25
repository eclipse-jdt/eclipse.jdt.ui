/*******************************************************************************
 * Copyright (c) 2016 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class MakeLocalVariableNonNullProposal extends ASTRewriteCorrectionProposal {

	public MakeLocalVariableNonNullProposal(ICompilationUnit targetCU, IVariableBinding varBinding, CompilationUnit astRoot, int relevance, String nonNullAnnotationName) {
		super("", targetCU, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE)); //$NON-NLS-1$
		setDelegate(new MakeLocalVariableNonNullProposalCore(targetCU, varBinding, astRoot, relevance, nonNullAnnotationName));
	}

}
