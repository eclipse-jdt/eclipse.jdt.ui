/*******************************************************************************
 * Copyright (c) 2020 Fabrice TIERCELIN and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.FixMessages;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that precompiles the regular expressions when they are used several times.
 * It uses <code>split()</code>, <code>replaceFirst()</code>, <code>replaceAll()</code> and <code>matches()</code> methods on a <code>java.util.regex.Pattern</code> object.
 * It only changes code inside one method.
 */
public class PatternCleanUp extends AbstractMultiFix {
	private static final String STRING_CLASS_NAME= String.class.getCanonicalName();

	private static final String SPLIT_METHOD= "split"; //$NON-NLS-1$
	private static final String REPLACE_FIRST_METHOD= "replaceFirst"; //$NON-NLS-1$
	private static final String REPLACE_ALL_METHOD= "replaceAll"; //$NON-NLS-1$
	private static final String MATCHER_METHOD= "matcher"; //$NON-NLS-1$
	private static final String MATCHES_METHOD= "matches"; //$NON-NLS-1$
	private static final String COMPILE_METHOD= "compile"; //$NON-NLS-1$

	public PatternCleanUp() {
		this(Collections.emptyMap());
	}

	public PatternCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.PRECOMPILE_REGEX);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.PRECOMPILE_REGEX)) {
			return new String[] { MultiFixMessages.PatternCleanup_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.PRECOMPILE_REGEX)) {
			return "" //$NON-NLS-1$
					+ "Pattern dateValidation= Pattern.compile(\"\\d{4}\\-\\d{2}\\-\\d{2}\");\n" //$NON-NLS-1$
					+ "dateValidation.matcher(\"2020-03-17\").matches();\n" //$NON-NLS-1$
					+ "dateValidation.matcher(\"2020-03-17\").replaceFirst(\"0000-00-00\");\n" //$NON-NLS-1$
					+ "dateValidation.matcher(\"2020-03-17\").replaceAll(\"0000-00-00\");\n" //$NON-NLS-1$
					+ "dateValidation.split(\"A2020-03-17B\");\n"; //$NON-NLS-1$
		} else {
			return "" //$NON-NLS-1$
					+ "String dateValidation= \"\\d{4}\\-\\d{2}\\-\\d{2}\";\n" //$NON-NLS-1$
					+ "\"2020-03-17\".matches(dateValidation);\n" //$NON-NLS-1$
					+ "\"2020-03-17\".replaceFirst(dateValidation, \"0000-00-00\");\n" //$NON-NLS-1$
					+ "\"2020-03-17\".replaceAll(dateValidation, \"0000-00-00\");\n" //$NON-NLS-1$
					+ "\"A2020-03-17B\".split(dateValidation);\n"; //$NON-NLS-1$
		}
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.PRECOMPILE_REGEX) || !JavaModelUtil.is1d4OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final Block node) {
				RegExAndUsesVisitor regExAndUsesVisitor= new RegExAndUsesVisitor(node);
				node.accept(regExAndUsesVisitor);
				return regExAndUsesVisitor.getResult();
			}

			final class RegExAndUsesVisitor extends ASTVisitor {
				private final Block startNode;

				private boolean result= true;

				public RegExAndUsesVisitor(final Block startNode) {
					this.startNode= startNode;
				}

				/**
				 * @return The result
				 */
				public boolean getResult() {
					return result;
				}

				@Override
				public boolean visit(final Block node) {
					return startNode == node;
				}

				@Override
				public boolean visit(final VariableDeclarationStatement node) {
					if (node.fragments().size() != 1) {
						return true;
					}

					VariableDeclarationFragment fragment= (VariableDeclarationFragment) node.fragments().get(0);
					return visitVariable(node.getType(), fragment.resolveBinding(), fragment.getExtraDimensions(), fragment.getInitializer());
				}

				@Override
				public boolean visit(final VariableDeclarationExpression node) {
					if (node.fragments().size() != 1) {
						return true;
					}

					VariableDeclarationFragment fragment= (VariableDeclarationFragment) node.fragments().get(0);
					return visitVariable(node.getType(), fragment.resolveBinding(), fragment.getExtraDimensions(), fragment.getInitializer());
				}

				@Override
				public boolean visit(final SingleVariableDeclaration node) {
					return visitVariable(node.getType(), node.resolveBinding(), node.getExtraDimensions(), node.getInitializer());
				}

				private boolean visitVariable(final Type type, final IVariableBinding variableBinding, final int extraDimensions, final Expression initializer) {
					if (getResult() && ASTNodes.hasType(type.resolveBinding(), STRING_CLASS_NAME)
							&& extraDimensions == 0
							&& initializer != null) {
						VarDefinitionsUsesVisitor varOccurrencesVisitor= new VarDefinitionsUsesVisitor(variableBinding,
								startNode, true).find();

						List<SimpleName> reads= varOccurrencesVisitor.getReads();
						List<SimpleName> writes= varOccurrencesVisitor.getWrites();

						if (writes.size() == 1 && reads.size() > 1) {
							for (SimpleName simpleName : reads) {
								if (!isRegExUse(simpleName)) {
									return true;
								}
							}
							rewriteOperations.add(new PatternOperation(type, initializer, reads));

							result= false;
							return false;
						}
					}

					return true;
				}

				private boolean isRegExUse(final SimpleName simpleName) {
					if (simpleName.getParent() instanceof MethodInvocation) {
						MethodInvocation methodInvocation= (MethodInvocation) simpleName.getParent();

						if (simpleName.getLocationInParent() == MethodInvocation.ARGUMENTS_PROPERTY
								&& !methodInvocation.arguments().isEmpty()
								&& simpleName.equals(methodInvocation.arguments().get(0))) {
							if (ASTNodes.usesGivenSignature(methodInvocation, STRING_CLASS_NAME, MATCHES_METHOD, STRING_CLASS_NAME)
									|| ASTNodes.usesGivenSignature(methodInvocation, STRING_CLASS_NAME, REPLACE_ALL_METHOD, STRING_CLASS_NAME, STRING_CLASS_NAME)
									|| ASTNodes.usesGivenSignature(methodInvocation, STRING_CLASS_NAME, REPLACE_FIRST_METHOD, STRING_CLASS_NAME, STRING_CLASS_NAME)
									|| ASTNodes.usesGivenSignature(methodInvocation, STRING_CLASS_NAME, SPLIT_METHOD, STRING_CLASS_NAME)
									|| ASTNodes.usesGivenSignature(methodInvocation, STRING_CLASS_NAME, SPLIT_METHOD, STRING_CLASS_NAME, int.class.getCanonicalName())) {
								return true;
							}
						}
					}

					return false;
				}
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.PatternCleanup_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, final IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class PatternOperation extends CompilationUnitRewriteOperation {
		private final Type type;
		private final Expression initializer;
		private final List<SimpleName> regExUses;

		public PatternOperation(final Type type, final Expression initializer, final List<SimpleName> regExUses) {
			this.type= type;
			this.initializer= initializer;
			this.regExUses= regExUses;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			TextEditGroup group= createTextEditGroup(FixMessages.PatternFix_convert_string_to_pattern_object, cuRewrite);

			String patternNameText= importRewrite.addImport(Pattern.class.getCanonicalName());
			rewrite.replace(type, ast.newSimpleType(newTypeName(ast, patternNameText)), group);

			MethodInvocation newCompileMethod= ast.newMethodInvocation();
			newCompileMethod.setExpression(newTypeName(ast, patternNameText));
			newCompileMethod.setName(ast.newSimpleName(COMPILE_METHOD));
			newCompileMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(initializer)));
			rewrite.replace(initializer, newCompileMethod, group);

			for (SimpleName oldRegExUse : regExUses) {
				MethodInvocation oldMethodInvocation= (MethodInvocation) oldRegExUse.getParent();
				MethodInvocation newMethod= ast.newMethodInvocation();
				newMethod.setExpression(ASTNodes.createMoveTarget(rewrite, oldRegExUse));

				if (ASTNodes.usesGivenSignature(oldMethodInvocation, STRING_CLASS_NAME, SPLIT_METHOD, STRING_CLASS_NAME)) {
					newMethod.setName(ast.newSimpleName(SPLIT_METHOD));
					newMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(oldMethodInvocation.getExpression())));
				} else if (ASTNodes.usesGivenSignature(oldMethodInvocation, STRING_CLASS_NAME, SPLIT_METHOD, STRING_CLASS_NAME, int.class.getCanonicalName())) {
					newMethod.setName(ast.newSimpleName(SPLIT_METHOD));
					newMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(oldMethodInvocation.getExpression())));
					newMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression((Expression) oldMethodInvocation.arguments().get(1))));
				} else {
					MethodInvocation newMatcherExpression= newMethod;
					newMethod= ast.newMethodInvocation();

					if (ASTNodes.usesGivenSignature(oldMethodInvocation, STRING_CLASS_NAME, MATCHES_METHOD, STRING_CLASS_NAME)) {
						newMethod.setName(ast.newSimpleName(MATCHES_METHOD));
					} else if (ASTNodes.usesGivenSignature(oldMethodInvocation, STRING_CLASS_NAME, REPLACE_ALL_METHOD, STRING_CLASS_NAME, STRING_CLASS_NAME)) {
						newMethod.setName(ast.newSimpleName(REPLACE_ALL_METHOD));
						newMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression((Expression) oldMethodInvocation.arguments().get(1))));
					} else if (ASTNodes.usesGivenSignature(oldMethodInvocation, STRING_CLASS_NAME, REPLACE_FIRST_METHOD, STRING_CLASS_NAME, STRING_CLASS_NAME)) {
						newMethod.setName(ast.newSimpleName(REPLACE_FIRST_METHOD));
						newMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression((Expression) oldMethodInvocation.arguments().get(1))));
					}

					newMatcherExpression.setName(ast.newSimpleName(MATCHER_METHOD));
					newMatcherExpression.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(oldMethodInvocation.getExpression())));

					newMethod.setExpression(newMatcherExpression);
				}

				rewrite.replace(oldMethodInvocation, newMethod, group);
			}
		}

		private Name newTypeName(AST ast, String patternNameText) {
			Name qualifiedName= null;

			for (String packageName : patternNameText.split("\\.")) { //$NON-NLS-1$
				if (qualifiedName == null) {
					qualifiedName= ast.newSimpleName(packageName);
				} else {
					qualifiedName= ast.newQualifiedName(qualifiedName, ast.newSimpleName(packageName));
				}
			}

			return qualifiedName;
		}
	}
}
