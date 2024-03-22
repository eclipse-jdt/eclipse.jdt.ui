/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Red Hat Inc - separate core logic from UI images
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Proposals for 'Assign to variable' quick assist - Assign an expression from an
 * ExpressionStatement to a local or field - Assign single or all parameter(s) to field(s)
 */
public class AssignToVariableAssistProposal extends LinkedCorrectionProposal {

	public static final int LOCAL= 1;

	public static final int FIELD= 2;

	public static final int TRY_WITH_RESOURCES= 3;

	static final String KEY_NAME= "name"; //$NON-NLS-1$

	static final String KEY_TYPE= "type"; //$NON-NLS-1$

	static final String GROUP_EXC_TYPE= "exc_type"; //$NON-NLS-1$

	static final String GROUP_EXC_NAME= "exc_name"; //$NON-NLS-1$

	static final String VAR_TYPE= "var"; //$NON-NLS-1$

	public AssignToVariableAssistProposal(ICompilationUnit cu, int variableKind, ExpressionStatement node, ITypeBinding typeBinding, int relevance) {
		super("", cu, null, relevance, null, new AssignToVariableAssistProposalCore(cu, variableKind, node, typeBinding, relevance, false)); //$NON-NLS-1$
		if (variableKind == LOCAL) {
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL));
		} else if (variableKind == FIELD) {
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE));
		} else {
			setImage(JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE));
		}
	}

	public AssignToVariableAssistProposal(ICompilationUnit cu, SingleVariableDeclaration parameter, VariableDeclarationFragment existingFragment, ITypeBinding typeBinding, int relevance) {
		super("", cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE), new AssignToVariableAssistProposalCore(cu, parameter, existingFragment, typeBinding, relevance, false)); //$NON-NLS-1$
	}

	public AssignToVariableAssistProposal(ICompilationUnit cu, List<SingleVariableDeclaration> parameters, int relevance) {
		super("", cu, null, relevance, JavaPluginImages.get(JavaPluginImages.IMG_FIELD_PRIVATE), new AssignToVariableAssistProposalCore(cu, parameters, relevance, false)); //$NON-NLS-1$
	}

	protected LinkedProposalModelCore createProposalModel() {
		return new LinkedProposalModelCore();
	}

	public int getVariableKind() {
		return ((AssignToVariableAssistProposalCore) getDelegate()).getVariableKind();
	}

}
