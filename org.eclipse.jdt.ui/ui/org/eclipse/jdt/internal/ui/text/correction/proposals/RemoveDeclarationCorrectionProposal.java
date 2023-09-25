/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import java.util.ArrayList;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;

import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;

public class RemoveDeclarationCorrectionProposal extends ASTRewriteCorrectionProposal {

	static class SideEffectFinder extends ASTVisitor {

		private ArrayList<Expression> fSideEffectNodes;

		public SideEffectFinder(ArrayList<Expression> res) {
			fSideEffectNodes= res;
		}

		@Override
		public boolean visit(Assignment node) {
			fSideEffectNodes.add(node);
			return false;
		}

		@Override
		public boolean visit(PostfixExpression node) {
			fSideEffectNodes.add(node);
			return false;
		}

		@Override
		public boolean visit(PrefixExpression node) {
			Object operator= node.getOperator();
			if (operator == PrefixExpression.Operator.INCREMENT || operator == PrefixExpression.Operator.DECREMENT) {
				fSideEffectNodes.add(node);
			}
			return false;
		}

		@Override
		public boolean visit(MethodInvocation node) {
			fSideEffectNodes.add(node);
			return false;
		}

		@Override
		public boolean visit(ClassInstanceCreation node) {
			fSideEffectNodes.add(node);
			return false;
		}

		@Override
		public boolean visit(SuperMethodInvocation node) {
			fSideEffectNodes.add(node);
			return false;
		}
	}



	public RemoveDeclarationCorrectionProposal(ICompilationUnit cu, SimpleName name, int relevance) {
		super("", cu, null, relevance, PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE)); //$NON-NLS-1$
		setDelegate(new RemoveDeclarationCorrectionProposalCore(cu, name, relevance));
		super.getDisplayString();
	}
}
