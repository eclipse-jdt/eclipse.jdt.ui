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
import java.util.function.Consumer;
import java.util.stream.Stream;

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
			if (typeBinding == null || !typeBinding.getQualifiedName().equals(JAVA_STRING)) {
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

			Stream<Expression> expressions= Stream.concat(Stream.of(fInfix.getLeftOperand(), fInfix.getRightOperand()), ((List<Expression>) fInfix.extendedOperands()).stream());

			List<String> parts= new ArrayList<>();

			expressions.forEach(new Consumer<Expression>() {
				@Override
				public void accept(Expression t) {
					String value= ((StringLiteral) t).getEscapedValue();
					parts.addAll(unescapeBlock(value.substring(1, value.length() - 1)));
				}
			});

			buf.append("\"\"\"\n"); //$NON-NLS-1$
			boolean newLine= false;
			for (String part : parts) {
				if (buf.length() > 4) {// the first part has been added after the text block delimiter and newline
					if (!newLine) {
						// no line terminator in this part: merge the line by emitting a line continuation escape
						buf.append("\\").append(System.lineSeparator()); //$NON-NLS-1$
					}
				}
				newLine= part.endsWith(System.lineSeparator());
				buf.append(fIndent).append(part);
			}

			if (newLine) {
				buf.append(fIndent);
			}
			buf.append("\"\"\""); //$NON-NLS-1$
			TextBlock textBlock= (TextBlock) rewrite.createStringPlaceholder(buf.toString(), ASTNode.TEXT_BLOCK);
			rewrite.replace(fInfix, textBlock, group);
		}

		/*
		 * Split a given string into parts of a text block. Transformations undertaken will be:
		 *
		 * 1. Split the text at newline boundaries. The newline will be replaced at the end
		 * of the first line being split with System.lineTerminator()
		 * 2. Transform any sequence of three or more double quotes in such that it's not interpreted as "end of text block"
		 * 3. Transform any trailing spaces into \s escapes
		 * 4. Transform any non-trailing \t characters into tab characters
		 */
		private List<String> unescapeBlock(String escapedText) {
			StringBuilder transformed= new StringBuilder();
			int readIndex= 0;
			int bsIndex= 0;

			List<String> parts= new ArrayList<>();

			while ((bsIndex= escapedText.indexOf("\\", readIndex)) >= 0) { //$NON-NLS-1$ "\"
				if (escapedText.startsWith("\\n", bsIndex)) { //$NON-NLS-1$ "\n"
					transformed.append(escapedText.substring(readIndex, bsIndex));
					parts.add(escapeTrailingWhitespace(transformed.toString())+ System.lineSeparator());
					transformed= new StringBuilder();
					readIndex= bsIndex + 2;
				} else if (escapedText.startsWith("\\\"", bsIndex)) { //$NON-NLS-1$ "\""
					// if there are more than three quotes in a row, escape the first quote of every triplet to
					// avoid it being interpreted as a text block terminator. This code would be much simpler if
					// we could escape the third quote of each triplet, but the text block spec recommends this way.

					transformed.append(escapedText.substring(readIndex, bsIndex));
					int quoteCount= 1;
					while (escapedText.startsWith("\\\"", bsIndex + 2 * quoteCount)) { //$NON-NLS-1$
						quoteCount++;
					}
					int i= 0;
					while (i < quoteCount / 3) {
						transformed.append("\\\"\"\""); //$NON-NLS-1$
						i++;
					}

					if (i > 0 && quoteCount % 3 != 0) {
						transformed.append("\\"); //$NON-NLS-1$
					}
					for (int j = 0; j < quoteCount % 3; j++) {
						transformed.append("\""); //$NON-NLS-1$
					}

					readIndex= bsIndex + 2 * quoteCount;
				} else if (escapedText.startsWith("\\t", bsIndex)) { //$NON-NLS-1$ "\t"
					transformed.append(escapedText.substring(readIndex, bsIndex));
					transformed.append("\t"); //$NON-NLS-1$
					readIndex= bsIndex+2;
				} else {
					transformed.append(escapedText.substring(readIndex, bsIndex));
					transformed.append("\\").append(escapedText.charAt(bsIndex + 1)); //$NON-NLS-1$
					readIndex= bsIndex + 2;
				}
			}
			if (readIndex < escapedText.length()) {
				// there is text at the end of the string that is not followed by a newline
				transformed.append(escapeTrailingWhitespace(escapedText.substring(readIndex)));
			}
			if (transformed.length() > 0) {
				parts.add(transformed.toString());
			}
			return parts;
		}

		/*
		 * Escape spaces and tabs at the end of a line, because they would be trimmed from a text block
		 */
		private static String escapeTrailingWhitespace(String unescaped) {
			if (unescaped.length() == 0) {
				return ""; //$NON-NLS-1$
			}
			int whitespaceStart= unescaped.length()-1;
			StringBuilder trailingWhitespace= new StringBuilder();
			while (whitespaceStart > 0) {
				if (unescaped.charAt(whitespaceStart) == ' ') {
					whitespaceStart--;
					trailingWhitespace.append("\\s"); //$NON-NLS-1$
				} else if (unescaped.charAt(whitespaceStart) == '\t') {
					whitespaceStart--;
					trailingWhitespace.append("\\t"); //$NON-NLS-1$
				} else {
					break;
				}
			}

			return unescaped.substring(0, whitespaceStart + 1) + trailingWhitespace;
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
