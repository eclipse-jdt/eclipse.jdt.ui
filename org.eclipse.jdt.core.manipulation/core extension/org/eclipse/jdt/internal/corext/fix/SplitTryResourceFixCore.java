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

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;

public final class SplitTryResourceFixCore extends CompilationUnitRewriteOperationsFixCore {

	public SplitTryResourceFixCore(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation[] operations) {
		super(name, compilationUnit, operations);
	}

	public static boolean initialConditionsCheck(ICompilationUnit compilationUnit, ASTNode node) {
		if (!JavaModelUtil.is1d8OrHigher(compilationUnit.getJavaProject()))
			return false;
		ASTNode foundNode= ASTNodes.getFirstAncestorOrNull(node, VariableDeclarationExpression.class, Statement.class);
		if (!(foundNode instanceof VariableDeclarationExpression varDeclExp) || varDeclExp.getLocationInParent() != TryStatement.RESOURCES2_PROPERTY) {
			return false;
		}
		ASTNode parent= foundNode.getParent();
		if (!(parent instanceof TryStatement tryStatement) || tryStatement.resources().size() < 2) {
			return false;
		}
		return true;
	}

	public static SplitTryResourceFixCore createSplitVariableFix(CompilationUnit compilationUnit, ASTNode node) {
		ASTNode foundNode= ASTNodes.getFirstAncestorOrNull(node, VariableDeclarationExpression.class, Statement.class);
		if (!(foundNode instanceof VariableDeclarationExpression varDeclExp) || varDeclExp.getLocationInParent() != TryStatement.RESOURCES2_PROPERTY) {
			return null;
		}
		ASTNode parent= foundNode.getParent();
		if (!(parent instanceof TryStatement tryStatement) || tryStatement.resources().size() < 2) {
			return null;
		}
		return new SplitTryResourceFixCore(CorrectionMessages.QuickAssistProcessor_splittryresource_description, compilationUnit,
				new CompilationUnitRewriteOperation[] { new SplitTryResourceProposalOperation(tryStatement, varDeclExp) });
	}

	private static class SplitTryResourceProposalOperation extends CompilationUnitRewriteOperation {

		private final TryStatement tryStatement;

		private final VariableDeclarationExpression expression;

		public SplitTryResourceProposalOperation(TryStatement statement, VariableDeclarationExpression expression) {
			this.tryStatement= statement;
			this.expression= expression;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
			TextEditGroup group= null;
			final ASTRewrite rewrite= cuRewrite.getASTRewrite();
			final AST ast= cuRewrite.getAST();
			ICompilationUnit cu= cuRewrite.getCu();
			CompilationUnit root= (CompilationUnit) tryStatement.getRoot();
			IBuffer cuBuffer= cu.getBuffer();
			List<VariableDeclarationExpression> resources= tryStatement.resources();
			TryStatement newTryStatement= ast.newTryStatement();
			IJavaElement rootElement= cuRewrite.getRoot().getJavaElement();
			String fIndent= "\t"; //$NON-NLS-1$
			if (rootElement != null) {
				IJavaProject project= rootElement.getJavaProject();
				if (project != null) {
					String tab_option= project.getOption(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, true);
					if (JavaCore.SPACE.equals(tab_option)) {
						fIndent= ""; //$NON-NLS-1$
						for (int i= 0; i < CodeFormatterUtil.getTabWidth(project); ++i) {
							fIndent += " "; //$NON-NLS-1$
						}
					}
				}
			}
			boolean copyResources= false;
			String prefix= ""; //$NON-NLS-1$
			for (VariableDeclarationExpression resource : resources) {
				if (resource.equals(expression)) {
					copyResources= true;
				}
				if (copyResources) {
					int start= root.getExtendedStartPosition(resource);
					int length= root.getExtendedLength(resource);
					StringBuffer buffer= new StringBuffer(prefix);
					buffer.append(cuBuffer.getText(start, length));
					VariableDeclarationExpression newVarExpression= (VariableDeclarationExpression) rewrite.createStringPlaceholder(buffer.toString(), ASTNode.VARIABLE_DECLARATION_EXPRESSION);
					newTryStatement.resources().add(newVarExpression);
					rewrite.remove(resource, group);
					prefix= "\n" + fIndent + fIndent; //$NON-NLS-1$
				}
			}
			Block originalBlock= tryStatement.getBody();
			List<Statement> originalStatements= originalBlock.statements();
			ListRewrite originalBlockListRewrite= rewrite.getListRewrite(originalBlock, Block.STATEMENTS_PROPERTY);
			StringBuilder buf= new StringBuilder();
			buf.append("{\n"); //$NON-NLS-1$
			for (Statement s : originalStatements) {
				int start= root.getExtendedStartPosition(s);
				int length= root.getExtendedLength(s);
				String text= cuBuffer.getText(start, length);
				buf.append(fIndent).append(text).append("\n"); //$NON-NLS-1$
				rewrite.remove(s, group);
			}
			buf.append("}"); //$NON-NLS-1$
			Block newBlock= (Block) rewrite.createStringPlaceholder(buf.toString(), ASTNode.BLOCK);
			newTryStatement.setBody(newBlock);
			originalBlockListRewrite.insertFirst(newTryStatement, group);
		}
	}

}
