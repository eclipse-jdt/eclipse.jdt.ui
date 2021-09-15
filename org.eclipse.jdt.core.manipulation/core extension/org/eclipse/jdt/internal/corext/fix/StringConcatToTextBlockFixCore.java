/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
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

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class StringConcatToTextBlockFixCore extends CompilationUnitRewriteOperationsFixCore {

	private final static String JAVA_STRING= "java.lang.String"; //$NON-NLS-1$

	public static final class StringConcatFinder extends ASTVisitor {

		private final List<CompilationUnitRewriteOperation> fOperations;
		private final boolean fAllConcats;

		public StringConcatFinder(List<CompilationUnitRewriteOperation> operations, boolean allConcats) {
			super(true);
			fOperations= operations;
			fAllConcats= allConcats;
		}

		private boolean isStringType(Type type) {
			if (type instanceof ArrayType) {
				return false;
			}
			ITypeBinding typeBinding= type.resolveBinding();
			if (typeBinding == null || !typeBinding.getQualifiedName().equals(JAVA_STRING)) {
				return false;
			}
			return true;
		}

		@Override
		public boolean visit(final VariableDeclarationStatement visited) {
			Type type= visited.getType();
			if (!isStringType(type)) {
				return false;
			}
			return true;
		}

		@Override
		public boolean visit(final FieldDeclaration visited) {
			Type type= visited.getType();
			if (!isStringType(type)) {
				return false;
			}
			return true;
		}

		@Override
		public boolean visit(final Assignment visited) {
			ITypeBinding typeBinding= visited.resolveTypeBinding();
			if (!typeBinding.getQualifiedName().equals(JAVA_STRING)) {
				return false;
			}
			return true;
		}

		@Override
		public boolean visit(final InfixExpression visited) {
			if (visited.getOperator() != InfixExpression.Operator.PLUS
					|| visited.extendedOperands().isEmpty()) {
				return false;
			}
			ITypeBinding typeBinding= visited.resolveTypeBinding();
			if (typeBinding == null || !typeBinding.getQualifiedName().equals(JAVA_STRING)) {
				return false;
			}
			Expression leftHand= visited.getLeftOperand();
			if (!(leftHand instanceof StringLiteral)) {
				return false;
			}
			StringLiteral leftLiteral= (StringLiteral)leftHand;
			String literal= leftLiteral.getLiteralValue();
			if (!literal.isEmpty() && !fAllConcats && !literal.endsWith("\n")) { //$NON-NLS-1$
				return false;
			}
			Expression rightHand= visited.getRightOperand();
			if (!(rightHand instanceof StringLiteral)) {
				return false;
			}
			StringLiteral rightLiteral= (StringLiteral)leftHand;
			literal= rightLiteral.getLiteralValue();
			if (!literal.isEmpty() && !fAllConcats && !literal.endsWith("\n")) { //$NON-NLS-1$
				return false;
			}
			List<Expression> extendedOperands= visited.extendedOperands();
			if (extendedOperands.isEmpty()) {
				return false;
			}
			for (int i= 0; i < extendedOperands.size(); ++i) {
				Expression operand= extendedOperands.get(i);
				if (operand instanceof StringLiteral) {
					StringLiteral stringLiteral= (StringLiteral)operand;
					String string= stringLiteral.getLiteralValue();
					if (!string.isEmpty() && (fAllConcats || string.endsWith("\n") || i == extendedOperands.size() - 1)) { //$NON-NLS-1$
						continue;
					}
				}
				return false;
			}
			fOperations.add(new ChangeStringConcatToTextBlock(visited));
			return false;
		}

	}

	public static class ChangeStringConcatToTextBlock extends CompilationUnitRewriteOperation {

		private final InfixExpression fInfix;
		private final String fIndent;

		public ChangeStringConcatToTextBlock(final InfixExpression infix) {
			this.fInfix= infix;
			this.fIndent= "\t"; //$NON-NLS-1$
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.StringConcatToTextBlockCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			StringBuilder buf= new StringBuilder();
			buf.append("\"\"\"\n"); //$NON-NLS-1$
			StringLiteral left= (StringLiteral)fInfix.getLeftOperand();
			formTextBlockString(buf, left, false);
			StringLiteral right= (StringLiteral)fInfix.getRightOperand();
			formTextBlockString(buf, right, false);
			List<Expression> extraOperands= fInfix.extendedOperands();
			boolean hasNewLine= true;
			for (Expression extraOperand : extraOperands) {
				hasNewLine= formTextBlockString(buf, (StringLiteral)extraOperand, extraOperand == extraOperands.get(extraOperands.size() - 1));
			}
			if (hasNewLine) {
				buf.append(fIndent);
			}
			buf.append("\"\"\""); //$NON-NLS-1$
			TextBlock textBlock= (TextBlock) rewrite.createStringPlaceholder(buf.toString(), ASTNode.TEXT_BLOCK);
			rewrite.replace(fInfix, textBlock, group);
		}

		private boolean formTextBlockString(StringBuilder buf, StringLiteral literal, boolean lastLine) {
			String modifiedTextLine= literal.getLiteralValue();
			boolean hasNewLine= false;
			modifiedTextLine= modifiedTextLine.replaceAll("\"\"\"", "\\\\\"\"\""); //$NON-NLS-1$ //$NON-NLS-2$
			if (!modifiedTextLine.isEmpty()) {
				if (modifiedTextLine.endsWith("\n")) { //$NON-NLS-1$
					hasNewLine= true;
					modifiedTextLine= modifiedTextLine.substring(0, modifiedTextLine.length() - 1);
					int count= 0;
					for (int i= modifiedTextLine.length() - 1; i > 0; --i) {
						if (modifiedTextLine.charAt(i) == ' ') {
							++count;
						} else {
							break;
						}
					}
					if (count > 0) {
						modifiedTextLine= modifiedTextLine.substring(0, modifiedTextLine.length() - count);
						for (int i= 0; i < count; ++i) {
							modifiedTextLine += "\\s"; //$NON-NLS-1$
						}
					}
				}
				modifiedTextLine= modifiedTextLine.replaceAll("\n", "\\\\n"); //$NON-NLS-1$ //$NON-NLS-2$
				buf.append(fIndent).append(modifiedTextLine);
				if (hasNewLine) {
					buf.append("\n"); //$NON-NLS-1$
				} else if (!lastLine) {
					buf.append("\\\n"); //$NON-NLS-1$
				}
			}
			return hasNewLine;
		}
	}

	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		if (!JavaModelUtil.is15OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();

		StringConcatFinder finder= new StringConcatFinder(operations, true);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new StringBufferToStringBuilderFixCore(FixMessages.StringConcatToTextBlockFix_convert_msg, compilationUnit, ops);
	}

	public static StringConcatToTextBlockFixCore createStringConcatToTextBlockFix(ASTNode exp) {
		CompilationUnit root= (CompilationUnit) exp.getRoot();
		if (!JavaModelUtil.is15OrHigher(root.getJavaElement().getJavaProject()))
			return null;
		List<CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation> operations= new ArrayList<>();
		StringConcatFinder finder= new StringConcatFinder(operations, true);
		exp.accept(finder);
		if (operations.isEmpty())
			return null;
		return new StringConcatToTextBlockFixCore(FixMessages.StringConcatToTextBlockFix_convert_msg, root, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { operations.get(0) });
	}

	protected StringConcatToTextBlockFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
