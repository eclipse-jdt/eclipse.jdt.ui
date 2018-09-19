/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Jens Reimann - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.List;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;

public class NewAbstractMethodCorrectionProposal extends NewMethodCorrectionProposal {

	public NewAbstractMethodCorrectionProposal(String label, ICompilationUnit targetCU, ASTNode invocationNode, List<Expression> arguments, ITypeBinding binding, int relevance, Image image) {
		super(label, targetCU, invocationNode, arguments, binding, relevance, image);
	}

	@Override
	protected int evaluateModifiers(ASTNode targetTypeDecl) {
		return Modifier.ABSTRACT | Modifier.PROTECTED;
	}

}
