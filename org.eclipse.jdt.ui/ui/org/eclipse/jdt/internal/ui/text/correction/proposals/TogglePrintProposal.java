/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - Initial API and Implementation
 *
 */
package org.eclipse.jdt.internal.ui.text.correction.proposals;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModelCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public class TogglePrintProposal extends CompilationUnitRewriteOperationsFixCore {

	public static final String SYSOUT = "System.out.print"; //$NON-NLS-1$
	public static final String IOPRT = "IO.print"; //$NON-NLS-1$

	public TogglePrintProposal(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static TogglePrintProposal togglePrint(CompilationUnit compilationUnit, ASTNode printStatement, String label) {
		ConvertPrintOperation refwfeop = new ConvertPrintOperation(printStatement);
		return new TogglePrintProposal(label, compilationUnit, refwfeop);
	}

  public static class ConvertPrintOperation extends CompilationUnitRewriteOperation {
		private ASTNode printStmt;
		private static final String IO = "IO"; //$NON-NLS-1$
		private static final String PRINT = "print"; //$NON-NLS-1$
		private static final String PRINTLN = "println"; //$NON-NLS-1$
		private static final String SYSTEM = "System"; //$NON-NLS-1$
		private static final String OUT = "out"; //$NON-NLS-1$


		public ConvertPrintOperation(ASTNode system) {
			this.printStmt= system;
		}

		@Override
	  public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
		  AST ast = cuRewrite.getRoot().getAST();
		  TextEditGroup group= createTextEditGroup(CorrectionMessages.QuickAssistProcessor_switch_Out_operation, cuRewrite);
		  ASTRewrite rewrite = cuRewrite.getASTRewrite();
	      Expression convertedOutExp = buildConvertedPrint(rewrite,ast);
	      ExpressionStatement convertedOutExpStatmnt = ast.newExpressionStatement(convertedOutExp);
	      ASTNodes.replaceButKeepComment(rewrite, printStmt, convertedOutExpStatmnt, group);
	  }

		public Expression buildConvertedPrint(ASTRewrite rewrite, AST ast) {
			ExpressionStatement currentNodeExpression= (ExpressionStatement) printStmt;
			MethodInvocation currentNode= (MethodInvocation) currentNodeExpression.getExpression();
			String start= currentNode.getExpression().toString();
			String print= currentNode.getName().getIdentifier();
			MethodInvocation newPrint= ast.newMethodInvocation();
			if (start.startsWith(SYSTEM)) {
				newPrint.setExpression(ast.newSimpleName(IO));
			} else {
				QualifiedName systemOut= ast.newQualifiedName(ast.newSimpleName(SYSTEM), ast.newSimpleName(OUT));
				newPrint.setExpression(systemOut);
			}
			if (print.endsWith("ln")) { //$NON-NLS-1$
				newPrint.setName(ast.newSimpleName(PRINTLN));
			} else {
				newPrint.setName(ast.newSimpleName(PRINT));
			}
			for (Object arg : currentNode.arguments()) {
				newPrint.arguments().add(rewrite.createCopyTarget((ASTNode) arg));
			}
			return newPrint;
		}

  }
}
