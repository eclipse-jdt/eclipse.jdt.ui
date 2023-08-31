/*******************************************************************************
 * Copyright (c) 2021, 2023 Red Hat Inc. and others.
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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
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
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSElement;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSLine;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class StringConcatToTextBlockFixCore extends CompilationUnitRewriteOperationsFixCore {

	private final static String JAVA_STRING= "java.lang.String"; //$NON-NLS-1$
	private final static String JAVA_STRINGBUFFER= "java.lang.StringBuffer"; //$NON-NLS-1$
	private final static String JAVA_STRINGBUILDER= "java.lang.StringBuilder"; //$NON-NLS-1$
	private final static String NLSComment= "$NON-NLS-1$"; //$NON-NLS-1$

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
			StringLiteral rightLiteral= (StringLiteral)leftHand;
			hasComments= hasComments || !ASTNodes.getTrailingComments(rightLiteral).isEmpty();
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
			hasComments= hasComments || !ASTNodes.getCommentsForRegion(cUnit, leftLiteral.getStartPosition(), endPosition - leftLiteral.getStartPosition()).isEmpty();
			lineNo= getLineOfOffset(cUnit, rightLiteral.getStartPosition());
			endPosition= getLineOffset(cUnit, lineNo + 1) == -1 ? cUnit.getLength() : getLineOffset(cUnit, lineNo + 1);
			hasComments= hasComments || !ASTNodes.getCommentsForRegion(cUnit, rightLiteral.getStartPosition(), endPosition - rightLiteral.getStartPosition()).isEmpty();
			for (int i= 0; i < extendedOperands.size(); ++i) {
				Expression operand= extendedOperands.get(i);
				if (operand instanceof StringLiteral) {
					StringLiteral stringLiteral= (StringLiteral)operand;
					lineNo= getLineOfOffset(cUnit, stringLiteral.getStartPosition());
					endPosition= getLineOffset(cUnit, lineNo + 1) == -1 ? cUnit.getLength() : getLineOffset(cUnit, lineNo + 1);
					hasComments= hasComments || !ASTNodes.getCommentsForRegion(cUnit, stringLiteral.getStartPosition(), endPosition - stringLiteral.getStartPosition()).isEmpty();
					String string= stringLiteral.getLiteralValue();
					if (!string.isEmpty() && (fAllConcats || string.endsWith("\n") || i == extendedOperands.size() - 1)) { //$NON-NLS-1$
						continue;
					}
				}
				return false;
			}
			boolean isTagged= false;
			if (hasComments) {
				// we must ensure that NLS comments are consistent for all string literals in concatenation
				ICompilationUnit cu= (ICompilationUnit)((CompilationUnit)leftHand.getRoot()).getJavaElement();
				try {
				   NLSLine nlsLine= NLSUtil.scanCurrentLine(cu, leftHand.getStartPosition());
				   isTagged= nlsLine.getElements()[0].hasTag();
				   if (!isConsistent(nlsLine, isTagged)) {
					   return false;
				   }
				   nlsLine= NLSUtil.scanCurrentLine(cu, rightHand.getStartPosition());
				   if (!isConsistent(nlsLine, isTagged)) {
					   return false;
				   }
				   for (int i= 0; i < extendedOperands.size(); ++i) {
					   Expression operand= extendedOperands.get(i);
					   nlsLine= NLSUtil.scanCurrentLine(cu, operand.getStartPosition());
					   if (!isConsistent(nlsLine, isTagged)) {
						   return false;
					   }
				   }
				} catch (JavaModelException e) {
					return false;
				}
			}
			fOperations.add(new ChangeStringConcatToTextBlock(visited, isTagged));
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
		private final String fIndent;
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
			if (!isTagged) {
				rewrite.replace(fInfix, textBlock, group);
			} else {
				Statement stmt= ASTNodes.getFirstAncestorOrNull(fInfix, Statement.class);
				ICompilationUnit cu= (ICompilationUnit)((CompilationUnit)fInfix.getRoot()).getJavaElement();
				StringBuilder buffer= new StringBuilder();
				buffer.append(cu.getBuffer().getText(stmt.getStartPosition(), fInfix.getStartPosition() - stmt.getStartPosition()));
				buffer.append(buf.toString());
				buffer.append(cu.getBuffer().getText(fInfix.getStartPosition() + fInfix.getLength(), stmt.getStartPosition() + stmt.getLength() - fInfix.getStartPosition() - fInfix.getLength()));
//				buffer.append(" //$NON-NLS-1$"); //$NON-NLS-1$
				Statement newStmt= (Statement) rewrite.createStringPlaceholder(buffer.toString(), stmt.getNodeType());
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
	private static List<String> unescapeBlock(String escapedText) {
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

	public static final class StringBufferFinder extends ASTVisitor {

		private final List<CompilationUnitRewriteOperation> fOperations;
		private List<StringLiteral> fLiterals= new ArrayList<>();
		private List<Statement> statementList= new ArrayList<>();
		private SimpleName originalVarName;
		private Map<ExpressionStatement, ChangeStringBufferToTextBlock> conversions= new HashMap<>();
		private static final String APPEND= "append"; //$NON-NLS-1$
		private static final String TO_STRING= "toString"; //$NON-NLS-1$
		private final Set<String> fExcludedNames;

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
							} else {
								valid= false;
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
			boolean nonNLS= false;
			CompilationUnit cu= (CompilationUnit) node.getRoot();
			ICompilationUnit icu= (ICompilationUnit) cu.getJavaElement();
			if (args.size() == 1 && args.get(0) instanceof StringLiteral) {
				fLiterals.add((StringLiteral)args.get(0));
				nonNLS= hasNLSMarker(statement, icu);
			}
			Block block= (Block) statement.getParent();
			List<Statement> stmtList= block.statements();
			int startIndex= stmtList.indexOf(statement);
			int i= startIndex;
			while (++i < stmtList.size()) {
				Statement s= stmtList.get(i);
				if (s instanceof ExpressionStatement) {
					Expression exp= ((ExpressionStatement)s).getExpression();
					if (exp instanceof MethodInvocation) {
						MethodInvocation method= (MethodInvocation)exp;
						Expression invocationExp= method.getExpression();
						if (invocationExp instanceof SimpleName &&
								((SimpleName)invocationExp).getFullyQualifiedName().equals(originalVarName.getFullyQualifiedName()) &&
								method.getName().getFullyQualifiedName().equals(APPEND)) {
							Expression arg= (Expression) method.arguments().get(0);
							if (arg instanceof StringLiteral) {
								if (nonNLS != hasNLSMarker(s, icu)) {
									nonNLS= !nonNLS;
									if (i - startIndex > 1 || args.size() == 1) {
										return false;
									}
								}
								fLiterals.add((StringLiteral)arg);
								statementList.add(s);
							} else {
								return false;
							}
						} else {
							break;
						}
					} else {
						break;
					}
				} else {
					break;
				}
			}
			Statement endStatement= stmtList.get(i - 1);
			int lastStatementEnd= endStatement.getStartPosition() + endStatement.getLength();
			IBinding varBinding= originalVarName.resolveBinding();
			if (varBinding == null) {
				return false;
			}
			CheckValidityVisitor checkValidityVisitor= new CheckValidityVisitor(lastStatementEnd, varBinding);
			try {
				block.accept(checkValidityVisitor);
			} catch (AbortSearchException e) {
				// do nothing
			}
			if (!checkValidityVisitor.isValid() || checkValidityVisitor.getToStringList().size() == 0) {
				statementList.clear();
				fLiterals.clear();
				return false;
			}
			ExpressionStatement assignmentToConvert= checkValidityVisitor.getNextAssignment();
			List<Statement> statements= new ArrayList<>(statementList);
			List<StringLiteral> literals= new ArrayList<>(fLiterals);
			List<MethodInvocation> toStringList= new ArrayList<>(checkValidityVisitor.getToStringList());
			ChangeStringBufferToTextBlock operation= new ChangeStringBufferToTextBlock(toStringList, statements, literals, assignmentToConvert, fExcludedNames, nonNLS);
			fOperations.add(operation);
			conversions.put(assignmentToConvert, operation);
			statementList.clear();
			fLiterals.clear();
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

	private static boolean hasNLSMarker(Statement statement, ICompilationUnit cu) {
		boolean hasNLS= false;
		List<Comment> commentList= ASTNodes.getTrailingComments(statement);
		if (commentList.size() > 0) {
			if (commentList.get(0) instanceof LineComment) {
				LineComment lineComment= (LineComment) commentList.get(0);
				try {
					String comment= cu.getBuffer().getText(lineComment.getStartPosition() + 2, lineComment.getLength() - 2).trim();
					hasNLS= comment.equals(NLSComment);
				} catch (IndexOutOfBoundsException | JavaModelException e) {
					return false;
				}
			}
		}
		return hasNLS;
	}

	public static class ChangeStringBufferToTextBlock extends CompilationUnitRewriteOperation {

		private final List<MethodInvocation> fToStringList;
		private final List<Statement> fStatements;
		private final List<StringLiteral> fLiterals;
		private final String fIndent;
		private final Set<String> fExcludedNames;
		private final boolean fNonNLS;
		private ExpressionStatement fAssignmentToConvert;

		public ChangeStringBufferToTextBlock(final List<MethodInvocation> toStringList, List<Statement> statements,
				List<StringLiteral> literals, ExpressionStatement assignmentToConvert, Set<String> excludedNames, boolean nonNLS) {
			this.fToStringList= toStringList;
			this.fStatements= statements;
			this.fLiterals= literals;
			this.fAssignmentToConvert= assignmentToConvert;
			this.fIndent= "\t"; //$NON-NLS-1$
			this.fExcludedNames= excludedNames;
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

			List<String> parts= new ArrayList<>();
			fLiterals.stream().forEach((t) -> { String value= t.getEscapedValue(); parts.addAll(unescapeBlock(value.substring(1, value.length() - 1))); });


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
			MethodInvocation firstToStringCall= fToStringList.get(0);
			AST ast= firstToStringCall.getAST();
			if (fToStringList.size() == 1 &&
					!fNonNLS &&
					ASTNodes.getLeadingComments(fStatements.get(0)).size() == 0 &&
					(firstToStringCall.getLocationInParent() == Assignment.RIGHT_HAND_SIDE_PROPERTY ||
					firstToStringCall.getLocationInParent() == VariableDeclarationFragment.INITIALIZER_PROPERTY)) {
				// if we only have one use of buffer.toString() and it is assigned to another string variable,
				// put the text block in place of the buffer.toString() call and delete the whole StringBuffer/StringBuilder
				TextBlock textBlock= (TextBlock) rewrite.createStringPlaceholder(buf.toString(), ASTNode.TEXT_BLOCK);
				rewrite.replace(firstToStringCall, textBlock, group);
				for (Statement statement : fStatements) {
					rewrite.remove(statement, group);
				}
			} else {
				// otherwise, create a new text block variable and use the variable name in every place the buffer.toString()
				// call is found before the variable is reused to form a new StringBuilder/StringBuffer
				ImportRewrite importRewriter= cuRewrite.getImportRewrite();
				ITypeBinding binding= firstToStringCall.resolveTypeBinding();
				if (binding != null) {
					Set<String> excludedNames= new HashSet<>(ASTNodes.getVisibleLocalVariablesInScope(fToStringList.get(fToStringList.size() - 1)));
					excludedNames.addAll(fExcludedNames);
					String newVarName= DEFAULT_NAME;
					if (excludedNames.contains(DEFAULT_NAME)) {
						newVarName= createName(DEFAULT_NAME, excludedNames);
					}
					fExcludedNames.add(newVarName);
					Type t= importRewriter.addImport(binding, ast);
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

	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit, boolean includeStringBufferBuilder) {
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
			StringBufferFinder finder2= new StringBufferFinder(operations, new HashSet<String>());
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
