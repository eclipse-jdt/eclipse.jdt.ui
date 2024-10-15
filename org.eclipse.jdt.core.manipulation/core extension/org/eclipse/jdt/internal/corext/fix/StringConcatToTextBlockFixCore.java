/*******************************************************************************
 * Copyright (c) 2021, 2024 Red Hat Inc. and others.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jface.text.BadLocationException;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSElement;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSLine;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSScanner;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class StringConcatToTextBlockFixCore extends CompilationUnitRewriteOperationsFixCore {

	private final static String JAVA_STRING= "java.lang.String"; //$NON-NLS-1$
	private final static String JAVA_STRINGBUFFER= "java.lang.StringBuffer"; //$NON-NLS-1$
	private final static String JAVA_STRINGBUILDER= "java.lang.StringBuilder"; //$NON-NLS-1$
	private final static String NLSComment= "$NON-NLS-1$"; //$NON-NLS-1$
	private final static String NLSCommentPrefix= "$NON-NLS-"; //$NON-NLS-1$

	public static final class StringConcatFinder extends ASTVisitor {

		private final List<CompilationUnitRewriteOperation> fOperations;
		private final boolean fAllConcats;

		public StringConcatFinder(List<CompilationUnitRewriteOperation> operations, boolean allConcats) {
			super(true);
			fOperations= operations;
			fAllConcats= allConcats;
		}

		@Override
		public boolean visit(final InfixExpression visited) {
			if (visited.getOperator() != InfixExpression.Operator.PLUS
					|| visited.extendedOperands().isEmpty()) {
				return false;
			}
			if (visited.getLocationInParent() == InfixExpression.LEFT_OPERAND_PROPERTY ||
					visited.getLocationInParent() == InfixExpression.RIGHT_OPERAND_PROPERTY) {
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
			boolean hasComments= false;
			StringLiteral leftLiteral= (StringLiteral)leftHand;
			CompilationUnit cUnit= (CompilationUnit)leftLiteral.getRoot();
			String literal= leftLiteral.getLiteralValue();
			if (!literal.isEmpty() && !fAllConcats && !literal.endsWith("\n")) { //$NON-NLS-1$
				return false;
			}
			Expression rightHand= visited.getRightOperand();
			if (!(rightHand instanceof StringLiteral)) {
				return false;
			}
			StringLiteral rightLiteral= (StringLiteral)rightHand;
			ICompilationUnit cu= (ICompilationUnit)cUnit.getJavaElement();
			hasComments= hasComments || hasNLS(ASTNodes.getTrailingComments(rightLiteral), cu);
			literal= rightLiteral.getLiteralValue();
			if (!literal.isEmpty() && !fAllConcats && !literal.endsWith("\n")) { //$NON-NLS-1$
				return false;
			}
			List<Expression> extendedOperands= visited.extendedOperands();
			if (extendedOperands.isEmpty()) {
				return false;
			}
			int lineNo= getLineOfOffset(cUnit, leftLiteral.getStartPosition());
			int endPosition= getLineOffset(cUnit, lineNo + 1) == -1 ? cUnit.getLength() : getLineOffset(cUnit, lineNo + 1);
			hasComments= hasComments || hasNLS(ASTNodes.getCommentsForRegion(cUnit, leftLiteral.getStartPosition(), endPosition - leftLiteral.getStartPosition()), cu);
			lineNo= getLineOfOffset(cUnit, rightLiteral.getStartPosition());
			endPosition= getLineOffset(cUnit, lineNo + 1) == -1 ? cUnit.getLength() : getLineOffset(cUnit, lineNo + 1);
			hasComments= hasComments || hasNLS(ASTNodes.getCommentsForRegion(cUnit, rightLiteral.getStartPosition(), endPosition - rightLiteral.getStartPosition()), cu);
			for (int i= 0; i < extendedOperands.size(); ++i) {
				Expression operand= extendedOperands.get(i);
				if (operand instanceof StringLiteral) {
					StringLiteral stringLiteral= (StringLiteral)operand;
					lineNo= getLineOfOffset(cUnit, stringLiteral.getStartPosition());
					endPosition= getLineOffset(cUnit, lineNo + 1) == -1 ? cUnit.getLength() : getLineOffset(cUnit, lineNo + 1);
					hasComments= hasComments || hasNLS(ASTNodes.getCommentsForRegion(cUnit, stringLiteral.getStartPosition(), endPosition - stringLiteral.getStartPosition()), cu);
					String string= stringLiteral.getLiteralValue();
					if (string.isEmpty() || fAllConcats || string.endsWith("\n") || i == extendedOperands.size() - 1) { //$NON-NLS-1$
						continue;
					}
				}
				return false;
			}
			boolean isTagged= false;
			// if there are any block comments or non-empty line comments and we aren't NLS, abandon change
			if (!hasComments) {
				List<Comment> comments= ASTNodes.getCommentsForRegion(cUnit, visited.getStartPosition(), visited.getLength());
				if (!comments.isEmpty()) {
					IBuffer buffer;
					try {
						buffer= cu.getBuffer();
						for (Comment comment : comments) {
							if (!(comment instanceof LineComment)) {
								return false;
							}
							if (!buffer.getText(comment.getStartPosition() + 2, comment.getLength() - 2).trim().isEmpty()) {
								return false;
							}
						}
					} catch (JavaModelException e) {
						// fall through
					}
				}
			} else if (ASTNodes.getFirstAncestorOrNull(visited, Annotation.class) == null) {
				NLSLine nlsLine= scanCurrentLine(cu, leftHand);
				if (nlsLine == null) {
					return false;
				}
				isTagged= nlsLine.getElements()[0].hasTag();
				if (!isConsistent(nlsLine, isTagged)) {
					return false;
				}
				nlsLine= scanCurrentLine(cu, rightHand);
				if (nlsLine == null || !isConsistent(nlsLine, isTagged)) {
					return false;
				}
				for (int i= 0; i < extendedOperands.size(); ++i) {
					Expression operand= extendedOperands.get(i);
					nlsLine= scanCurrentLine(cu, operand);
					if (nlsLine == null || !isConsistent(nlsLine, isTagged)) {
						return false;
					}
				}
			}
			// check if we are untagged or else if tagged, make sure we have a Statement or FieldDeclaration
			// ancestor so we can recreate with proper single NLS tag
			if (!isTagged || ASTNodes.getFirstAncestorOrNull(visited, Statement.class, FieldDeclaration.class) != null) {
				fOperations.add(new ChangeStringConcatToTextBlock(visited, isTagged));
			}
			return false;
		}

		private NLSLine scanCurrentLine(ICompilationUnit cu, Expression exp) {
			CompilationUnit cUnit= (CompilationUnit)exp.getRoot();
			int startLine= cUnit.getLineNumber(exp.getStartPosition());
			int endOfLine= cUnit.getPosition(startLine + 1, 0);
			NLSLine[] lines;
			try {
				lines= NLSScanner.scan(cu.getBuffer().getText(exp.getStartPosition(), endOfLine - exp.getStartPosition()));
				if (lines.length > 0) {
					return lines[0];
				}
			} catch (IndexOutOfBoundsException | JavaModelException | InvalidInputException | BadLocationException e) {
				// fall-through
			}
			return null;
		}

		private boolean hasNLS(List<Comment> trailingComments, ICompilationUnit cu) {
			if (!trailingComments.isEmpty() && cu != null) {
				IBuffer buffer;
				try {
					buffer= cu.getBuffer();
					for (Comment comment : trailingComments) {
						if (comment instanceof LineComment) {
							if (buffer.getText(comment.getStartPosition(), comment.getLength()).contains("$NON-NLS")) { //$NON-NLS-1$
								return true;
							}
						}
					}
				} catch (JavaModelException e) {
					// fall through
				}
			}
			return false;
		}

		private int getLineOfOffset(CompilationUnit astRoot, int offset) {
			return astRoot.getLineNumber(offset) - 1;
		}

		private int getLineOffset(CompilationUnit astRoot, int line) {
			return astRoot.getPosition(line + 1, 0);
		}

		private boolean isConsistent(NLSLine nlsLine, boolean isTagged) {
			NLSElement[] elements= nlsLine.getElements();
			for (NLSElement element : elements) {
				if (element.hasTag() != isTagged) {
					return false;
				}
			}
			return true;
		}
	}

	public static class ChangeStringConcatToTextBlock extends CompilationUnitRewriteOperation {

		private final InfixExpression fInfix;
		private String fIndent;
		private final boolean isTagged;

		public ChangeStringConcatToTextBlock(final InfixExpression infix, boolean isTagged) {
			this.fInfix= infix;
			this.fIndent= "\t"; //$NON-NLS-1$
			this.isTagged= isTagged;
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

			IJavaElement root= cuRewrite.getRoot().getJavaElement();
			if (root != null) {
				IJavaProject project= root.getJavaProject();
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

			StringBuilder buf= new StringBuilder();

			Stream<Expression> expressions= Stream.concat(Stream.of(fInfix.getLeftOperand(), fInfix.getRightOperand()), ((List<Expression>) fInfix.extendedOperands()).stream());

			List<String> parts= new ArrayList<>();

			expressions.forEach(new Consumer<Expression>() {
				@Override
				public void accept(Expression t) {
					StringLiteral literal= (StringLiteral)t;
					if (!literal.getLiteralValue().equals("\"\"")) { //$NON-NLS-1$
						String value= literal.getEscapedValue();
						parts.addAll(unescapeBlock(value.substring(1, value.length() - 1)));
					}
				}
			});

			buf.append("\"\"\"\n"); //$NON-NLS-1$
			boolean newLine= false;
			boolean allWhiteSpaceStart= true;
			boolean allEmpty= true;
			for (String part : parts) {
				if (buf.length() > 4) {// the first part has been added after the text block delimiter and newline
					if (!newLine) {
						// no line terminator in this part: merge the line by emitting a line continuation escape
						buf.append("\\").append(System.lineSeparator()); //$NON-NLS-1$
					}
				}
				newLine= part.endsWith(System.lineSeparator());
				allWhiteSpaceStart= allWhiteSpaceStart && (part.isEmpty() || Character.isWhitespace(part.charAt(0)));
				allEmpty= allEmpty && part.isEmpty();
				buf.append(fIndent).append(part);
			}

			if (newLine || allEmpty) {
				buf.append(fIndent);
			} else if (allWhiteSpaceStart) {
				buf.append("\\").append(System.lineSeparator()); //$NON-NLS-1$
				buf.append(fIndent);
			} else {
				// Replace trailing un-escaped quotes with escaped quotes before adding text block end
				int i= buf.length() - 1;
				int count= 0;
				while (i >= 0 && buf.charAt(i) == '"' && count <= 3) {
					--i;
					++count;
				}
				if (i >= 0 && buf.charAt(i) == '\\') {
					--count;
				}
				for (i= count; i > 0; --i) {
					buf.deleteCharAt(buf.length() - 1);
				}
				for (i= count; i > 0; --i) {
					buf.append("\\\""); //$NON-NLS-1$
				}
				i= buf.length() - 1;
				if (buf.charAt(i) == ' ') {
					buf.deleteCharAt(i);
					buf.append("\\s"); //$NON-NLS-1$
				} else if (buf.charAt(i) == '\t') {
					buf.deleteCharAt(i);
					buf.append("\\t"); //$NON-NLS-1$
				}
			}
			buf.append("\"\"\""); //$NON-NLS-1$
			if (!isTagged) {
				TextBlock textBlock= (TextBlock) rewrite.createStringPlaceholder(buf.toString(), ASTNode.TEXT_BLOCK);
				rewrite.replace(fInfix, textBlock, group);
			} else {
				ASTNode stmt= ASTNodes.getFirstAncestorOrNull(fInfix, Statement.class, FieldDeclaration.class);
				ICompilationUnit cu= (ICompilationUnit)((CompilationUnit)fInfix.getRoot()).getJavaElement();
				StringBuilder buffer= new StringBuilder();
				buffer.append(cu.getBuffer().getText(stmt.getStartPosition(), fInfix.getStartPosition() - stmt.getStartPosition()));
				buffer.append(buf.toString());
				buffer.append(cu.getBuffer().getText(fInfix.getStartPosition() + fInfix.getLength(), stmt.getStartPosition() + stmt.getLength() - fInfix.getStartPosition() - fInfix.getLength()));
				ASTNode newStmt= rewrite.createStringPlaceholder(buffer.toString(), stmt.getNodeType());
				ASTNodes.replaceButKeepComment(rewrite, stmt, newStmt, group);
			}
		}

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
	public static List<String> unescapeBlock(String escapedText) {
		StringBuilder transformed= new StringBuilder();
		int readIndex= 0;
		int bsIndex= 0;

		List<String> parts= new ArrayList<>();

		while ((bsIndex= escapedText.indexOf("\\", readIndex)) >= 0) { //$NON-NLS-1$ "\"
			if (escapedText.startsWith("\\n", bsIndex) || escapedText.startsWith("\\u005cn", bsIndex)) { //$NON-NLS-1$ //$NON-NLS-2$ "\n"
				transformed.append(escapedText.substring(readIndex, bsIndex));
				parts.add(escapeTrailingWhitespace(transformed.toString())+ System.lineSeparator());
				transformed= new StringBuilder();
				readIndex= bsIndex + (escapedText.startsWith("\\n", bsIndex) ? 2 : 7); //$NON-NLS-1$
			} else if (escapedText.startsWith("\\\"", bsIndex) || escapedText.startsWith("\\u005c\"", bsIndex)) { //$NON-NLS-1$ //$NON-NLS-2$ "\""
				// if there are more than three quotes in a row, escape the first quote of every triplet to
				// avoid it being interpreted as a text block terminator. This code would be much simpler if
				// we could escape the third quote of each triplet, but the text block spec recommends this way.

				transformed.append(escapedText.substring(readIndex, bsIndex));
				int quoteCount= 1;
				int index= (escapedText.startsWith("\\\"", bsIndex) ? 2 : 7); //$NON-NLS-1$
				while (escapedText.startsWith("\\\"", bsIndex + index) || escapedText.startsWith("\\u005c\"", bsIndex + index)) { //$NON-NLS-1$ //$NON-NLS-2$
					quoteCount++;
					index += (escapedText.startsWith("\\\"", bsIndex + index) ? 2 : 7); //$NON-NLS-1$
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
				readIndex= bsIndex + index;
			} else if (escapedText.startsWith("\\t", bsIndex) || escapedText.startsWith("\\u005ct", bsIndex)) { //$NON-NLS-1$ //$NON-NLS-2$ "\t"
				transformed.append(escapedText.substring(readIndex, bsIndex));
				transformed.append("\t"); //$NON-NLS-1$
				readIndex= bsIndex + (escapedText.startsWith("\\t", bsIndex) ? 2 : 7); //$NON-NLS-1$
			} else if (escapedText.startsWith("\\'", bsIndex) || escapedText.startsWith("\\u005c'", bsIndex)) { //$NON-NLS-1$ //$NON-NLS-2$
				transformed.append(escapedText.substring(readIndex, bsIndex));
				transformed.append("'"); //$NON-NLS-1$
				readIndex= bsIndex + (escapedText.startsWith("\\'", bsIndex) ? 2 : 7); //$NON-NLS-1$
			} else {
				transformed.append(escapedText.substring(readIndex, bsIndex));
				transformed.append("\\").append(escapedText.charAt(bsIndex + 1)); //$NON-NLS-1$
				readIndex= bsIndex + 2;
			}
		}
		if (readIndex < escapedText.length()) {
			// there is text at the end of the string that is not followed by a newline
			transformed.append(escapedText.substring(readIndex));
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
		if (unescaped.charAt(whitespaceStart) == ' ') {
			--whitespaceStart;
			trailingWhitespace.append("\\s"); //$NON-NLS-1$
		} else if (unescaped.charAt(whitespaceStart) == '\t') {
			--whitespaceStart;
			trailingWhitespace.append("\\t"); //$NON-NLS-1$
		}

		return unescaped.substring(0, whitespaceStart + 1) + trailingWhitespace;
	}

	public static final class StringBufferFinder extends ASTVisitor {

		private final List<CompilationUnitRewriteOperation> fOperations;
		private List<StringLiteral> fLiterals= new ArrayList<>();
		private List<Statement> statementList= new ArrayList<>();
		private Set<Statement> ignoredConstructorStatements= new HashSet<>();
		private SimpleName originalVarName;
		private Map<ExpressionStatement, ChangeStringBufferToTextBlock> conversions= new HashMap<>();
		private static final String APPEND= "append"; //$NON-NLS-1$
		private static final String TO_STRING= "toString"; //$NON-NLS-1$
		private static final String INDEX_OF= "indexOf"; //$NON-NLS-1$
		private BodyDeclaration fLastBodyDecl;
		private final Set<String> fExcludedNames;
		private final Set<IBinding> fRemovedDeclarations= new HashSet<>();

		public StringBufferFinder(List<CompilationUnitRewriteOperation> operations, Set<String> excludedNames) {
			super(true);
			fOperations= operations;
			fExcludedNames= excludedNames;
		}

		private class CheckValidityVisitor extends ASTVisitor {
			private boolean valid= true;
			private ExpressionStatement nextAssignment= null;
			private int lastStatementEnd;
			private IBinding varBinding;
			private List<MethodInvocation> toStringList= new ArrayList<>();
			private List<MethodInvocation> indexOfList= new ArrayList<>();
			private List<SimpleName> argList= new ArrayList<>();

			public CheckValidityVisitor(int lastStatementEnd, IBinding varBinding) {
				this.lastStatementEnd= lastStatementEnd;
				this.varBinding= varBinding;
			}

			public boolean isValid() {
				return valid;
			}

			public ExpressionStatement getNextAssignment() {
				return nextAssignment;
			}

			public List<MethodInvocation> getToStringList() {
				return toStringList;
			}

			public List<MethodInvocation> getIndexOfList() {
				return indexOfList;
			}

			public List<SimpleName> getArgList() {
				return argList;
			}

			@Override
			public boolean visit(SimpleName name) {
				if (name.getStartPosition() > lastStatementEnd) {
					IBinding binding= name.resolveBinding();
					if (varBinding.isEqualTo(binding)) {
						valid= false;
						if (name.getLocationInParent() == Assignment.LEFT_HAND_SIDE_PROPERTY) {
							Assignment assignment= (Assignment) name.getParent();
							ASTNode parent= assignment.getParent();
							if (parent instanceof ExpressionStatement &&
									(assignment.getRightHandSide() instanceof ClassInstanceCreation ||
											assignment.getRightHandSide() instanceof NullLiteral)) {
								valid=true;
								nextAssignment= (ExpressionStatement)parent;
							}
						} else if (name.getLocationInParent() == MethodInvocation.EXPRESSION_PROPERTY) {
							MethodInvocation invocation= (MethodInvocation) name.getParent();
							if (invocation.getName().getFullyQualifiedName().equals(TO_STRING)) {
								valid= true;
								toStringList.add(invocation);
								return true;
							} else if (invocation.getName().getFullyQualifiedName().equals(INDEX_OF)) {
								valid= true;
								indexOfList.add(invocation);
								return true;
							} else {
								valid= false;
							}
						} else if (name.getLocationInParent() == MethodInvocation.ARGUMENTS_PROPERTY) {
							MethodInvocation invocation= (MethodInvocation) name.getParent();
							IMethodBinding methodBinding= invocation.resolveMethodBinding();
							if (methodBinding != null) {
								List<Expression> args= invocation.arguments();
								int index= -1;
								for (int i= 0; i < args.size(); ++i) {
									if (args.get(i) == name) {
										index= i;
										break;
									}
								}
								ITypeBinding[] paramTypes= methodBinding.getParameterTypes();
								ITypeBinding paramType= paramTypes[index];
								if (paramType.getQualifiedName().equals("java.lang.String") || //$NON-NLS-1$
										paramType.getQualifiedName().equals("java.lang.CharSequence")) { //$NON-NLS-1$
									argList.add(name);
									valid= true;
								} else {
									valid= false;
								}
							}
						} else {
							valid= false;
						}
						throw new AbortSearchException();
					}
				}
				return true;
			}
		}

		public Map<ExpressionStatement, ChangeStringBufferToTextBlock> getConversionMap() {
			return conversions;
		}

		private boolean isStringBufferType(Type type) {
			if (type instanceof ArrayType) {
				return false;
			}
			ITypeBinding typeBinding= type.resolveBinding();
			if (typeBinding == null || (!typeBinding.getQualifiedName().equals(JAVA_STRINGBUFFER) && !typeBinding.getQualifiedName().equals(JAVA_STRINGBUILDER))) {
				return false;
			}
			return true;
		}

		@Override
		public boolean visit(ClassInstanceCreation node) {
			if (!isStringBufferType(node.getType())) {
				return false;
			}
			List<Expression> args= node.arguments();
			Statement statement= ASTNodes.getFirstAncestorOrNull(node, Statement.class);
			if (statement.getLocationInParent() != Block.STATEMENTS_PROPERTY ||
					!(statement instanceof VariableDeclarationStatement) &&
					!(statement instanceof ExpressionStatement)) {
				return false;
			}
			if (ignoredConstructorStatements.contains(statement)) {
				statementList.remove(statement);
				return false;
			}
			boolean nonNLS= false;
			CompilationUnit cu= (CompilationUnit) node.getRoot();
			ICompilationUnit icu= (ICompilationUnit) cu.getJavaElement();
			if (args.size() == 1 && args.get(0) instanceof StringLiteral) {
				fLiterals.add((StringLiteral)args.get(0));
			} else if (args.size() > 0) {
				if (args.get(0) instanceof InfixExpression infix) {
					if (!processInfixExpression(infix)) {
						return failure();
					}
				} else if (!(args.get(0) instanceof NumberLiteral)) {
					statementList.remove(statement);
					return false;
				}
			}
			try {
				extractConcatenatedAppends(node);
			} catch (AbortSearchException e) {
				return failure();
			}
			Block block= (Block) statement.getParent();
			List<Statement> stmtList= block.statements();
			int startIndex= stmtList.indexOf(statement);
			int i= startIndex;
			Statement previousStatement= null;
			while (++i < stmtList.size()) {
				Statement s= stmtList.get(i);
				if (s instanceof ExpressionStatement) {
					Expression exp= ((ExpressionStatement)s).getExpression();
					if (exp instanceof MethodInvocation) {
						class MethodVisitor extends ASTVisitor {
							private boolean valid= false;
							private boolean indexOfSeen= false;
							public boolean isValid() {
								return valid;
							}
							@Override
							public boolean visit(SimpleName simpleName) {
								if (simpleName.getFullyQualifiedName().equals(APPEND) && simpleName.getLocationInParent() == MethodInvocation.NAME_PROPERTY) {
									return true;
								}
								if (simpleName.getLocationInParent() == MethodInvocation.EXPRESSION_PROPERTY && simpleName.getFullyQualifiedName().equals(originalVarName.getFullyQualifiedName())) {
									if (((MethodInvocation)simpleName.getParent()).getName().getFullyQualifiedName().equals(APPEND)) {
										if (!indexOfSeen) {
											extractConcatenatedAppends(simpleName);
											valid= true;
										} else {
											valid= false;
										}
										return false;
									} else if (((MethodInvocation)simpleName.getParent()).getName().getFullyQualifiedName().equals(INDEX_OF)) {
										indexOfSeen= true;
										return false;
									}
								}
								return true;
							}
						}
						MethodVisitor methodVisitor= new MethodVisitor();
						try {
							s.accept(methodVisitor);
							if (!methodVisitor.isValid()) {
								break;
							}
							statementList.add(s);
						} catch (AbortSearchException e) {
							return failure();
						}
					} else if (exp instanceof Assignment assignment && assignment.getLeftHandSide() instanceof SimpleName name
							&& name.getFullyQualifiedName().equals(originalVarName.getFullyQualifiedName())
							&& (i - startIndex == 1 || ignoredConstructorStatements.contains(previousStatement))) {
						Expression rightSide= ASTNodes.getUnparenthesedExpression(assignment.getRightHandSide());
						if (rightSide instanceof ClassInstanceCreation cic && isStringBufferType(cic.getType())) {
							List<Expression> cicArgs= cic.arguments();
							ignoredConstructorStatements.add(s);
							statementList.add(s);
							if (!cicArgs.isEmpty() || !fLiterals.isEmpty()) {
								return failure();
							}
						}
					} else {
						break;
					}
				} else {
					break;
				}
				previousStatement= s;
			}
			Statement endStatement= stmtList.get(i - 1);
			int lastStatementEnd= endStatement.getStartPosition() + endStatement.getLength();
			IBinding varBinding= null;
			if (originalVarName == null || (varBinding= originalVarName.resolveBinding()) == null) {
				return failure();
			}
			// verify NLS markers are consistent for all string literals
			if (!fLiterals.isEmpty()) {
				try {
					nonNLS= hasNLSMarker(fLiterals.get(0), icu);
					for (int j= 1; j < fLiterals.size(); ++j) {
						if (nonNLS != hasNLSMarker(fLiterals.get(j), icu)) {
							return failure();
						}
					}
				} catch (AbortSearchException e) {
					return failure();
				}
			}
			CheckValidityVisitor checkValidityVisitor= new CheckValidityVisitor(lastStatementEnd, varBinding);
			try {
				block.accept(checkValidityVisitor);
			} catch (AbortSearchException e) {
				// do nothing
			}
			if (!checkValidityVisitor.isValid() || (checkValidityVisitor.getToStringList().isEmpty() && checkValidityVisitor.getArgList().isEmpty())) {
				return failure();
			}
			ExpressionStatement assignmentToConvert= checkValidityVisitor.getNextAssignment();
			List<Statement> statements= new ArrayList<>(statementList);
			List<StringLiteral> literals= new ArrayList<>(fLiterals);
			List<MethodInvocation> toStringList= new ArrayList<>(checkValidityVisitor.getToStringList());
			List<MethodInvocation> indexOfList= new ArrayList<>(checkValidityVisitor.getIndexOfList());
			List<SimpleName> argList= new ArrayList<>(checkValidityVisitor.getArgList());
			BodyDeclaration bodyDecl= ASTNodes.getFirstAncestorOrNull(node, BodyDeclaration.class);
			if (statements.get(0) instanceof VariableDeclarationStatement) {
				fRemovedDeclarations.add(originalVarName.resolveBinding());
			}
			ChangeStringBufferToTextBlock operation= new ChangeStringBufferToTextBlock(toStringList, indexOfList, argList, statements, literals,
					fRemovedDeclarations.contains(originalVarName.resolveBinding()) ? assignmentToConvert : null, fExcludedNames, fLastBodyDecl, nonNLS);
			fLastBodyDecl= bodyDecl;
			fOperations.add(operation);
			conversions.put(assignmentToConvert, operation);
			statementList.clear();
			fLiterals.clear();
			return true;
		}

		private void extractConcatenatedAppends(ASTNode node) throws AbortSearchException {
			ASTNode parent= node.getParent();
			while (!(parent instanceof Statement)) {
				if (parent instanceof MethodInvocation methodCall) {
					if (!methodCall.getName().getFullyQualifiedName().equals(APPEND)) {
						throw new AbortSearchException();
					}
					Expression arg= (Expression) methodCall.arguments().get(0);
					if (arg instanceof StringLiteral) {
						fLiterals.add((StringLiteral)arg);
					} else if (arg instanceof InfixExpression infix) {
						if (!processInfixExpression(infix)) {
							throw new AbortSearchException();
						}
					} else {
						throw new AbortSearchException();
					}
				}
				parent= parent.getParent();
			}
		}

		private boolean failure() {
			statementList.clear();
			fLiterals.clear();
			return false;
		}

		private boolean processInfixExpression(InfixExpression infix) {
			if (infix.getOperator() == Operator.PLUS) {
				Expression left= infix.getLeftOperand();
				Expression right= infix.getRightOperand();
				List<Expression> extendedOps= infix.extendedOperands();
				if (left instanceof StringLiteral && right instanceof StringLiteral) {
					List<StringLiteral> extendedLiterals= new ArrayList<>();
					for (Expression extendedOp : extendedOps) {
						if (extendedOp instanceof StringLiteral) {
							extendedLiterals.add((StringLiteral)extendedOp);
						} else {
							return false;
						}

					}
					fLiterals.add((StringLiteral)left);
					fLiterals.add((StringLiteral)right);
					fLiterals.addAll(extendedLiterals);
				} else {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean visit(final VariableDeclarationStatement visited) {
			Type type= visited.getType();
			if (!isStringBufferType(type) || visited.fragments().size() != 1) {
				return false;
			}
			return true;
		}

		@Override
		public boolean visit(VariableDeclarationFragment node) {
			// TODO Auto-generated method stub
			VariableDeclarationStatement varDeclStmt= ASTNodes.getFirstAncestorOrNull(node, VariableDeclarationStatement.class);
			if (varDeclStmt == null || varDeclStmt.fragments().size() != 1) {
				return false;
			}
			originalVarName= node.getName();
			statementList.clear();
			statementList.add(varDeclStmt);
			return true;
		}

		@Override
		public boolean visit(final Assignment visited) {
			ITypeBinding typeBinding= visited.resolveTypeBinding();
			if (typeBinding == null || (!typeBinding.getQualifiedName().equals(JAVA_STRINGBUFFER) && !typeBinding.getQualifiedName().equals(JAVA_STRINGBUILDER))) {
				return false;
			}
			if (!(visited.getLeftHandSide() instanceof SimpleName)) {
				return false;
			}
			ExpressionStatement statement= ASTNodes.getFirstAncestorOrNull(visited, ExpressionStatement.class);
			if (statement == null) {
				return false;
			}
			originalVarName= ((SimpleName)visited.getLeftHandSide());
			statementList.add(statement);
			return true;
		}

	}

	private static boolean hasNLSMarker(ASTNode node, ICompilationUnit cu) throws AbortSearchException {
		boolean hasNLS= false;
		List<Comment> commentList= ASTNodes.getTrailingComments(node);
		if (commentList.isEmpty()) {
			ASTNode n= node;
			while (!(n instanceof Statement) && commentList.isEmpty()) {
				n= n.getParent();
				commentList= ASTNodes.getTrailingComments(n);
			}
		}
		if (commentList.size() > 0) {
			if (commentList.get(0) instanceof LineComment) {
				LineComment lineComment= (LineComment) commentList.get(0);
				try {
					String comment= cu.getBuffer().getText(lineComment.getStartPosition() + 2, lineComment.getLength() - 2).trim();
					hasNLS= comment.startsWith(NLSComment);
					if (comment.substring(NLSComment.length()).contains(NLSCommentPrefix)) {
						throw new AbortSearchException();
					}
				} catch (IndexOutOfBoundsException | JavaModelException e) {
					throw new AbortSearchException();
				}
			}
		}
		return hasNLS;
	}

	public static class ChangeStringBufferToTextBlock extends CompilationUnitRewriteOperation {

		private final List<MethodInvocation> fToStringList;
		private final List<MethodInvocation> fIndexOfList;
		private final List<SimpleName> fArgList;
		private final List<Statement> fStatements;
		private final List<StringLiteral> fLiterals;
		private String fIndent;
		private final Set<String> fExcludedNames;
		private final BodyDeclaration fLastBodyDecl;
		private final boolean fNonNLS;
		private ExpressionStatement fAssignmentToConvert;

		public ChangeStringBufferToTextBlock(final List<MethodInvocation> toStringList, final List<MethodInvocation> indexOfList, final List<SimpleName> argList,
				List<Statement> statements, List<StringLiteral> literals, ExpressionStatement assignmentToConvert, Set<String> excludedNames, BodyDeclaration lastBodyDecl, boolean nonNLS) {
			this.fToStringList= toStringList;
			this.fIndexOfList= indexOfList;
			this.fArgList= argList;
			this.fStatements= statements;
			this.fLiterals= literals;
			this.fAssignmentToConvert= assignmentToConvert;
			this.fIndent= "\t"; //$NON-NLS-1$
			this.fExcludedNames= excludedNames;
			this.fLastBodyDecl= lastBodyDecl;
			this.fNonNLS= nonNLS;
		}

		public List<Statement> getStatements() {
			return fStatements;
		}

		public void resetAssignmentToConvert() {
			fAssignmentToConvert= null;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			String DEFAULT_NAME= "str"; //$NON-NLS-1$
			BodyDeclaration bodyDecl= ASTNodes.getFirstAncestorOrNull(fToStringList.isEmpty() ? fArgList.get(0) : fToStringList.get(0), BodyDeclaration.class);
			if (bodyDecl != null && bodyDecl != fLastBodyDecl) {
				fExcludedNames.clear();
			}
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			IJavaElement root= cuRewrite.getRoot().getJavaElement();
			if (root != null) {
				IJavaProject project= root.getJavaProject();
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

			List<String> parts= new ArrayList<>();
			fLiterals.stream().forEach((t) -> { String value= t.getEscapedValue(); parts.addAll(unescapeBlock(value.substring(1, value.length() - 1))); });


			buf.append("\"\"\"\n"); //$NON-NLS-1$
			boolean newLine= false;
			boolean allWhiteSpaceStart= true;
			boolean allEmpty= true;
			for (String part : parts) {
				if (buf.length() > 4) {// the first part has been added after the text block delimiter and newline
					if (!newLine) {
						// no line terminator in this part: merge the line by emitting a line continuation escape
						buf.append("\\").append(System.lineSeparator()); //$NON-NLS-1$
					}
				}
				newLine= part.endsWith(System.lineSeparator());
				allWhiteSpaceStart= allWhiteSpaceStart && (part.isEmpty() || Character.isWhitespace(part.charAt(0)));
				allEmpty= allEmpty && part.isEmpty();
				buf.append(fIndent).append(part);
			}

			if (newLine || allEmpty) {
				buf.append(fIndent);
			} else if (allWhiteSpaceStart) {
				buf.append("\\").append(System.lineSeparator()); //$NON-NLS-1$
				buf.append(fIndent);
			} else {
				// Replace trailing un-escaped quotes with escaped quotes before adding text block end
				int readIndex= buf.length() - 1;
				int count= 0;
				while (readIndex >= 0 && buf.charAt(readIndex) == '"' && count <= 3) {
					--readIndex;
					++count;
				}
				if (readIndex >= 0 && buf.charAt(readIndex) == '\\') {
					--count;
				}
				for (int i= count; i > 0; --i) {
					buf.deleteCharAt(buf.length() - 1);
				}
				for (int i= count; i > 0; --i) {
					buf.append("\\\""); //$NON-NLS-1$
				}
			}
			buf.append("\"\"\""); //$NON-NLS-1$
			AST ast= fStatements.get(0).getAST();
			if (fToStringList.size() == 1 &&
					fIndexOfList.isEmpty() &&
					!fNonNLS &&
					ASTNodes.getLeadingComments(fStatements.get(0)).size() == 0 &&
					(fToStringList.get(0).getLocationInParent() == Assignment.RIGHT_HAND_SIDE_PROPERTY ||
					fToStringList.get(0).getLocationInParent() == VariableDeclarationFragment.INITIALIZER_PROPERTY)) {
				// if we only have one use of buffer.toString() and it is assigned to another string variable,
				// put the text block in place of the buffer.toString() call and delete the whole StringBuffer/StringBuilder
				TextBlock textBlock= (TextBlock) rewrite.createStringPlaceholder(buf.toString(), ASTNode.TEXT_BLOCK);
				rewrite.replace(fToStringList.get(0), textBlock, group);
				for (Statement statement : fStatements) {
					rewrite.remove(statement, group);
				}
			} else {
				// otherwise, create a new text block variable and use the variable name in every place the buffer.toString()
				// call is found before the variable is reused to form a new StringBuilder/StringBuffer
				ImportRewrite importRewriter= cuRewrite.getImportRewrite();
				Set<String> excludedNames= new HashSet<>(ASTNodes.getAllLocalVariablesInBlock(fStatements.get(0)));
				excludedNames.addAll(fExcludedNames);
				String newVarName= DEFAULT_NAME;
				if (excludedNames.contains(DEFAULT_NAME)) {
					newVarName= createName(DEFAULT_NAME, excludedNames);
				}
				fExcludedNames.add(newVarName);
				Type t= importRewriter.addImportFromSignature("QString;", ast); //$NON-NLS-1$
				if (!fNonNLS || hasNLSMarker(fStatements.get(0), cuRewrite.getCu())) {
					TextBlock textBlock= (TextBlock) rewrite.createStringPlaceholder(buf.toString(), ASTNode.TEXT_BLOCK);
					VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
					newFragment.setName(ast.newSimpleName(newVarName));
					newFragment.setInitializer(textBlock);
					VariableDeclarationStatement newStmt= ast.newVariableDeclarationStatement(newFragment);
					newStmt.setType(t);
					ASTNodes.replaceButKeepComment(rewrite, fStatements.get(0), newStmt, group);
				} else {
					StringBuilder builder= new StringBuilder();
					builder.append("String "); //$NON-NLS-1$
					builder.append(newVarName);
					builder.append(" = "); //$NON-NLS-1$
					builder.append(buf.toString());
					builder.append("; //$NON-NLS-1$"); //$NON-NLS-1$
					VariableDeclarationStatement newStmt= (VariableDeclarationStatement) rewrite.createStringPlaceholder(builder.toString(), ASTNode.VARIABLE_DECLARATION_STATEMENT);
					ASTNodes.replaceButKeepComment(rewrite, fStatements.get(0), newStmt, group);
				}
				for (int i= 1; i < fStatements.size(); ++i) {
					rewrite.remove(fStatements.get(i), group);
				}
				for (MethodInvocation toStringCall : fToStringList) {
					SimpleName name= ast.newSimpleName(newVarName);
					rewrite.replace(toStringCall, name, group);
				}
				for (MethodInvocation indexOfCall : fIndexOfList) {
					MethodInvocation newCall= ast.newMethodInvocation();
					newCall.setName(ast.newSimpleName("indexOf")); //$NON-NLS-1$
					SimpleName caller= ast.newSimpleName(newVarName);
					newCall.setExpression(caller);
					List<Expression> arguments= indexOfCall.arguments();
					newCall.arguments().add(rewrite.createCopyTarget(arguments.get(0)));
					rewrite.replace(indexOfCall, newCall, group);
				}
				for (SimpleName arg : fArgList) {
					SimpleName name= ast.newSimpleName(newVarName);
					rewrite.replace(arg, name, group);
				}
			}
			// convert any reuse of the StringBuffer/StringBuilder variable that assigns a new StringBuffer/StringBuilder so
			// it will now declare the variable as the original declaration will be deleted
			if (fAssignmentToConvert != null) {
				ImportRewrite importRewriter= cuRewrite.getImportRewrite();
				Assignment assignment= (Assignment) fAssignmentToConvert.getExpression();
				ITypeBinding binding= assignment.resolveTypeBinding();
				if (binding != null) {
					Type t= importRewriter.addImport(binding, ast);
					VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
					newFragment.setName(ast.newSimpleName(((SimpleName)assignment.getLeftHandSide()).getFullyQualifiedName()));
					newFragment.setInitializer((Expression)rewrite.createCopyTarget(assignment.getRightHandSide()));
					VariableDeclarationStatement newStmt= ast.newVariableDeclarationStatement(newFragment);
					newStmt.setType(t);
					ASTNodes.replaceButKeepComment(rewrite, fAssignmentToConvert, newStmt, group);
				}
			}
		}

		private static String createName(final String nameRoot, final Set<String> excludedNames) {
			int i= 1;
			String candidate;

			do {
				candidate= nameRoot + i++;
			} while (excludedNames.remove(candidate));

			return candidate;
		}

	}

	public static ICleanUpFix createCleanUp(final CompilationUnit compilationUnit, boolean includeStringBufferBuilder) {
		if (!JavaModelUtil.is15OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();

		StringConcatFinder finder= new StringConcatFinder(operations, true);
		compilationUnit.accept(finder);

		if (includeStringBufferBuilder) {
			Set<String> excludedNames= new HashSet<>();
			StringBufferFinder finder2= new StringBufferFinder(operations, excludedNames);
			compilationUnit.accept(finder2);
			Map<ExpressionStatement, ChangeStringBufferToTextBlock> conversions= finder2.getConversionMap();
			for (CompilationUnitRewriteOperation operation : operations) {
				if (operation instanceof ChangeStringBufferToTextBlock) {
					for (Statement s : ((ChangeStringBufferToTextBlock)operation).getStatements()) {
						if (conversions.containsKey(s)) {
							ChangeStringBufferToTextBlock op= conversions.get(s);
							op.resetAssignmentToConvert();
						}
					}
				}
			}
		}

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
		if (operations.isEmpty()) {
			StringBufferFinder finder2= new StringBufferFinder(operations, new HashSet<>());
			exp.accept(finder2);
		}
		if (operations.isEmpty()) {
			return null;
		}
		return new StringConcatToTextBlockFixCore(FixMessages.StringConcatToTextBlockFix_convert_msg, root, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { operations.get(0) });
	}

	protected StringConcatToTextBlockFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
