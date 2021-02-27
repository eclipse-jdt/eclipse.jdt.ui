/*******************************************************************************
 * Copyright (c) 2021 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *     Holger Voormann - Handles Pattern.quote(), Matcher.quoteReplacement(), lone chars and escaped texts
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

public class PlainReplacementFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static final class PlainReplacementFinder extends ASTVisitor {
		private List<PlainReplacementFixOperation> fResult;

		public PlainReplacementFinder(List<PlainReplacementFixOperation> ops) {
			fResult= ops;
		}

		@Override
		public boolean visit(final MethodInvocation visited) {
			if (ASTNodes.usesGivenSignature(visited, String.class.getCanonicalName(), "replaceAll", String.class.getCanonicalName(), String.class.getCanonicalName()) //$NON-NLS-1$
					&& isPlainStringArgument(visited, true, 0, Pattern.class, "quote") //$NON-NLS-1$
					&& isPlainStringArgument(visited, false, 1, Matcher.class, "quoteReplacement")) { //$NON-NLS-1$
				fResult.add(new PlainReplacementFixOperation(visited));

				if (visited.getExpression() != null) {
					visited.getExpression().accept(this);
				}

				return false;
			}

			return true;
		}

		private boolean isPlainStringArgument(final MethodInvocation visited, final boolean isRegex, final int index, final Class<?> klass, final String methodName) {
			Expression argument= (Expression) visited.arguments().get(index);

			if (argument instanceof MethodInvocation
					&& ASTNodes.usesGivenSignature((MethodInvocation) argument, klass.getCanonicalName(), methodName, String.class.getCanonicalName())) {
				return true;
			}

			Object argumentResolved= argument.resolveConstantExpressionValue();
			return argumentResolved instanceof String && toPlainString((String) argumentResolved, isRegex, argument instanceof StringLiteral) != null;
		}

	}

	public static class PlainReplacementFixOperation extends CompilationUnitRewriteOperation {
		private final MethodInvocation visited;

		public PlainReplacementFixOperation(final MethodInvocation visited) {
			this.visited= visited;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModelCore linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.PlainReplacementCleanUp_description, cuRewrite);
			rewrite.setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				@Override
				public SourceRange computeSourceRange(final ASTNode nodeWithComment) {
					if (Boolean.TRUE.equals(nodeWithComment.getProperty(ASTNodes.UNTOUCH_COMMENT))) {
						return new SourceRange(nodeWithComment.getStartPosition(), nodeWithComment.getLength());
					}

					return super.computeSourceRange(nodeWithComment);
				}
			});

			rewrite.set(visited, MethodInvocation.NAME_PROPERTY, ast.newSimpleName("replace"), group); //$NON-NLS-1$
			String arg1= tryToSimplifyStringLiteralArgument(true, ast, rewrite, group);
			String arg2= tryToSimplifyStringLiteralArgument(false, ast, rewrite, group);

			if (arg1 != null
					&& arg2 != null
					&& arg1.length() == 1
					&& arg2.length() == 1) {
				convertStringLiteralArgumentToChar(true, arg1, ast, rewrite, group);
				convertStringLiteralArgumentToChar(false, arg2, ast, rewrite, group);
			} else {
				useQuotedArgumentWhenPossible(true, rewrite, group);
				useQuotedArgumentWhenPossible(false, rewrite, group);
			}
		}

		private String tryToSimplifyStringLiteralArgument(boolean isRegex, final AST ast, final ASTRewrite rewrite, final TextEditGroup group) {
			int index= isRegex ? 0 : 1;
			Expression argument= (Expression) visited.arguments().get(index);

			if (!(argument instanceof StringLiteral)) {
				return null;
			}

			String argumentResolved= (String) argument.resolveConstantExpressionValue();
			String asPlainString= toPlainString(argumentResolved, isRegex, true);

			if (asPlainString == null) {
				return null;
			} else if (argumentResolved.equals(asPlainString)) {
				return argumentResolved;
			}

			// rewrite String literal
			StringLiteral dummyStringLiteral= ast.newStringLiteral();
			dummyStringLiteral.setLiteralValue(asPlainString);
			rewrite.set(((StringLiteral) visited.arguments().get(index)), StringLiteral.ESCAPED_VALUE_PROPERTY, dummyStringLiteral.getEscapedValue(), group);
			return asPlainString;
		}

		private void convertStringLiteralArgumentToChar(final boolean isRegex, final String string, final AST ast, final ASTRewrite rewrite, final TextEditGroup group) {
			int index= isRegex ? 0 : 1;
			CharacterLiteral characterLiteral= ast.newCharacterLiteral();
			characterLiteral.setCharValue(string.charAt(0));
			ASTNodes.replaceButKeepComment(rewrite, (ASTNode) visited.arguments().get(index), characterLiteral, group);
		}

		private void useQuotedArgumentWhenPossible(final boolean isRegex, final ASTRewrite rewrite, final TextEditGroup group) {
			int index= isRegex ? 0 : 1;
			Expression argument= (Expression) visited.arguments().get(index);

			if (argument instanceof MethodInvocation) {
				Expression quoteExpr= (Expression) ((MethodInvocation) argument).arguments().get(0);
				ASTNodes.replaceButKeepComment(rewrite, argument, ASTNodes.createMoveTarget(rewrite, quoteExpr), group);
			}
		}

	}

	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<PlainReplacementFixOperation> operations= new ArrayList<>();
		PlainReplacementFinder finder= new PlainReplacementFinder(operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[0]);
		return new PlainReplacementFixCore(FixMessages.PlainReplacementFix_use_plain_text, compilationUnit, ops);
	}

	protected PlainReplacementFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

	/**
	 * @param regexOrReplacement the regular expression or the replacement to convert to a plain String
	 * @param isRegex {@code true} to convert a regular expression, {@code false} to convert a replacement
	 * @param doUnescape {@code true} if and only if removing of escaping to convert to a plain String is allowed
	 * @return the plain String or {@code null} if the given regular expression cannot be converted to a plain String
	 *         or when in doubt (for the sake of simplicity, e.g. {@code null} is also returned for {@code "a[b]c"}
	 *         even if that would be {@code "abc"})
	 */
	private static String toPlainString(final String regexOrReplacement, final boolean isRegex, final boolean doUnescape) {
		boolean isEscaped= false;
		StringBuilder plainString= null;

		for (int i= 0; i < regexOrReplacement.length(); i++) {
			char c= regexOrReplacement.charAt(i);

			if (Character.isSurrogate(c)) {
				// See https://bugs.openjdk.java.net/browse/JDK-8149446
				return null;
			} else if (isMetaChar(c, isRegex)) {
				if (isEscaped) {
					if (plainString == null) {
						if (!doUnescape) {
							return null;
						}

						plainString= new StringBuilder();

						if (i > 1) {
							plainString.append(regexOrReplacement, 0, i - 1);
						}
					}

					plainString.append(c);
				} else if (c != '\\') {
					return null;
				}

			// in regex, escaped non-meta chars might have special meaning, e.g. \s, \d, \W, etc.
			// in replacement, escaped non-meta chars do not have a special meaning (escaping is here unnecessary and will be ignored)
			} else if (isEscaped) {
				if (isRegex || (!doUnescape && !isMetaChar(c, false))) {
					return null;
				}

				if (!isMetaChar(c, false)) {
					if (plainString == null) {
						plainString= new StringBuilder();

						if (i > 1) {
							plainString.append(regexOrReplacement, 0, i - 1);
						}
					}

					plainString.append(c);
				}
			} else if (plainString != null) {
				plainString.append(c);
			}

			isEscaped= !isEscaped && c == '\\';
		}

		return isEscaped ? null : (plainString == null ? regexOrReplacement : plainString.toString());
	}

	private static boolean isMetaChar(char c, boolean isRegex) {
		if (isRegex) {
			return c == '.'
					|| c == '$'
					|| c == '|'
					|| c == '('
					|| c == ')'
					|| c == '['
					|| c == ']'
					|| c == '{'
					|| c == '}'
					|| c == '^'
					|| c == '?'
					|| c == '*'
					|| c == '+'
					|| c == '\\';
		}

		return c == '$' || c == '\\';
	}
}
