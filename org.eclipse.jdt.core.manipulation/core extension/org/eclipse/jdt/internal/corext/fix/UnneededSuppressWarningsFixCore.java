/*******************************************************************************
 * Copyright (c) 2024 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * Remove unneeded SuppressWarnings annotations.
 */
public class UnneededSuppressWarningsFixCore extends CompilationUnitRewriteOperationsFixCore {

	public UnneededSuppressWarningsFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static IProposableFix createFix(CompilationUnit compilationUnit, StringLiteral origLiteral) {
		IProblem[] problems= compilationUnit.getProblems();
		List<IProblemLocation> locationsList= new ArrayList<>();
		for (int i= 0; i < problems.length; i++) {
			IProblemLocation location= new ProblemLocation(problems[i]);
			if (location.getProblemId() == IProblem.UnusedWarningToken) {
				ASTNode node= location.getCoveringNode(compilationUnit);
				if (node instanceof StringLiteral literal) {
					if (literal.getLiteralValue() == origLiteral.getLiteralValue()) {
						locationsList.add(location);
					}
				}
			}
		}
		IProblemLocation[] locations= locationsList.toArray(new IProblemLocation[0]);
		return createFix(compilationUnit, origLiteral, locations);
	}

	public static IProposableFix createFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		StringLiteral literal= (StringLiteral)problem.getCoveringNode(compilationUnit);
		return createFix(compilationUnit, literal, new IProblemLocation[] { problem });
	}

	public static IProposableFix createFix(CompilationUnit compilationUnit, StringLiteral literal, IProblemLocation[] problems) {
		ICompilationUnit cu= (ICompilationUnit)compilationUnit.getJavaElement();
		try {
			if (!cu.isStructureKnown())
				return null;
		} catch (JavaModelException e) {
			return null;
		}
		String label= Messages.format(CorrectionMessages.SuppressWarningsSubProcessor_remove_annotation_label, literal.getLiteralValue());
		return new UnneededSuppressWarningsFixCore(label, compilationUnit, new RemoveUnneededSuppressWarningsOperation(problems));
	}

	private static class RemoveUnneededSuppressWarningsOperation extends CompilationUnitRewriteOperation {

		private IProblemLocation[] fLocations;
		public RemoveUnneededSuppressWarningsOperation(IProblemLocation[] locations) {
			this.fLocations= locations;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {

			for (IProblemLocation location : fLocations) {
				ASTNode coveringNode= location.getCoveringNode(cuRewrite.getRoot());
				if (!(coveringNode instanceof StringLiteral))
					continue;

				if (coveringNode.getParent() instanceof MemberValuePair) {
					coveringNode= coveringNode.getParent();
				}

				ASTNode parent= coveringNode.getParent();
				ASTRewrite rewrite= cuRewrite.getASTRewrite();
				if (parent instanceof SingleMemberAnnotation) {
					rewrite.remove(parent, null);
				} else if (parent instanceof NormalAnnotation) {
					NormalAnnotation annot= (NormalAnnotation) parent;
					if (annot.values().size() == 1) {
						rewrite.remove(annot, null);
					} else {
						rewrite.remove(coveringNode, null);
					}
				} else if (parent instanceof ArrayInitializer) {
					rewrite.remove(coveringNode, null);
				}
			}

		}

	}

}
