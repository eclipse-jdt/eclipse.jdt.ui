/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
 *     Red Hat Inc. - Body moved from org.eclipse.jdt.internal.ui.text.correction.proposals.NewAbstractMethodCorrectionProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

public class NewAbstractMethodCorrectionProposalCore extends NewMethodCorrectionProposalCore {
	public NewAbstractMethodCorrectionProposalCore(String label, ICompilationUnit targetCU, ASTNode invocationNode, List<Expression> arguments, ITypeBinding binding, int relevance) {
		super(label, targetCU, invocationNode, arguments, binding, relevance);
	}

	@Override
	protected int evaluateModifiers(ASTNode targetTypeDecl) {
		return Modifier.ABSTRACT | Modifier.PROTECTED;
	}
}