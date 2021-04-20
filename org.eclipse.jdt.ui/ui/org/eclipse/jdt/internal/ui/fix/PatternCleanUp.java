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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.dom.VarDefinitionsUsesVisitor;
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
					+ "Pattern dateCheck= Pattern.compile(\"\\\\d{4}-\\\\d{2}-\\\\d{2}\");\n" //$NON-NLS-1$
					+ "dateCheck.matcher(\"2020-03-17\").matches();\n" //$NON-NLS-1$
					+ "dateCheck.matcher(\"2020-03-17\").dateCheckplaceFirst(\"0000-00-00\");\n" //$NON-NLS-1$
					+ "dateCheck.matcher(\"2020-03-17\").replaceAll(\"0000-00-00\");\n" //$NON-NLS-1$
					+ "dateCheck.split(\"A2020-03-17B\");\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "String dateCheck= \"\\\\d{4}-\\\\d{2}-\\\\d{2}\";\n" //$NON-NLS-1$
				+ "\"2020-03-17\".matches(dateCheck);\n" //$NON-NLS-1$
				+ "\"2020-03-17\".replaceFirst(dateCheck, \"0000-00-00\");\n" //$NON-NLS-1$
				+ "\"2020-03-17\".replaceAll(dateCheck, \"0000-00-00\");\n" //$NON-NLS-1$
				+ "\"A2020-03-17B\".split(dateCheck);\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.PRECOMPILE_REGEX) || !JavaModelUtil.is1d4OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();
		final Map<ASTNode, Set<String>> addedPatternFields= new HashMap<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final Block node) {
				RegExAndUsesVisitor regExAndUsesVisitor= new RegExAndUsesVisitor(node);
				node.accept(regExAndUsesVisitor);
				return true;
			}

			final class RegExAndUsesVisitor extends ASTVisitor {
				private final Block startNode;

				public RegExAndUsesVisitor(final Block startNode) {
					this.startNode= startNode;
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
					if (ASTNodes.hasType(type.resolveBinding(), STRING_CLASS_NAME)
							&& extraDimensions == 0
							&& initializer != null) {
						VarDefinitionsUsesVisitor varOccurrencesVisitor= new VarDefinitionsUsesVisitor(variableBinding,
						startNode, true);

						List<SimpleName> reads= varOccurrencesVisitor.getReads();
						List<SimpleName> writes= varOccurrencesVisitor.getWrites();

						if (writes.size() == 1 && reads.size() > 1) {
							for (SimpleName simpleName : reads) {
								if (!isRegExUse(simpleName, initializer)) {
									return true;
								}
							}
							String varName= variableBinding.getName();
							rewriteOperations.add(new PatternOperation(type, initializer, reads, varName, addedPatternFields));

							return false;
						}
					}

					return true;
				}

				private boolean isRegExUse(final SimpleName simpleName, final Expression initializer) {
					if (simpleName.getParent() instanceof MethodInvocation) {
						MethodInvocation methodInvocation= (MethodInvocation) simpleName.getParent();

						if (simpleName.getLocationInParent() == MethodInvocation.ARGUMENTS_PROPERTY
								&& !methodInvocation.arguments().isEmpty()
								&& simpleName.equals(methodInvocation.arguments().get(0))) {
							if (ASTNodes.usesGivenSignature(methodInvocation, STRING_CLASS_NAME, MATCHES_METHOD, STRING_CLASS_NAME)
									|| ASTNodes.usesGivenSignature(methodInvocation, STRING_CLASS_NAME, REPLACE_ALL_METHOD, STRING_CLASS_NAME, STRING_CLASS_NAME)
									|| ASTNodes.usesGivenSignature(methodInvocation, STRING_CLASS_NAME, REPLACE_FIRST_METHOD, STRING_CLASS_NAME, STRING_CLASS_NAME)) {
								return true;
							} else if (ASTNodes.usesGivenSignature(methodInvocation, STRING_CLASS_NAME, SPLIT_METHOD, STRING_CLASS_NAME)
									|| ASTNodes.usesGivenSignature(methodInvocation, STRING_CLASS_NAME, SPLIT_METHOD, STRING_CLASS_NAME, int.class.getCanonicalName())) {
								String value= null;
								Expression expression= ASTNodes.getUnparenthesedExpression(initializer);

								if (expression instanceof StringLiteral) {
									StringLiteral literal= (StringLiteral) expression;
									value= literal.getLiteralValue();
								} else if (expression instanceof Name) {
									Name name= (Name) expression;
									IBinding binding= name.resolveBinding();

									if (binding instanceof IVariableBinding) {
										ASTNode aSTNode= ASTNodes.findDeclaration(binding, startNode);

										if (aSTNode instanceof VariableDeclarationFragment) {
											Expression exp= ((VariableDeclarationFragment) aSTNode).getInitializer();

											if (exp != null) {
												exp= ASTNodes.getUnparenthesedExpression(exp);

												if (exp instanceof StringLiteral) {
													value= ((StringLiteral) exp).getLiteralValue();
												}
											}
										}
									}
								}

								if (value != null) {
									// Don't compile pattern for single character that doesn't use regex meta-chars
									// and don't compile pattern for double character where first char is back-slash
									// and second character isn't alpha-numeric
									if (value.length() == 1) {
										return ".$|()[{^?*+\\".contains(value); //$NON-NLS-1$
									} else if (value.length() == 2 && value.charAt(0) == '\\') {
										return Character.isLetterOrDigit(value.charAt(1));
									}
								}

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
		private final String varName;
		private final Map<ASTNode, Set<String>> addedPatternFields;

		public PatternOperation(final Type type, final Expression initializer, final List<SimpleName> regExUses,
				final String varName, final Map<ASTNode, Set<String>> addedPatternFields) {
			this.type= type;
			this.initializer= initializer;
			this.regExUses= regExUses;
			this.varName= varName;
			this.addedPatternFields= addedPatternFields;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			TextEditGroup group= createTextEditGroup(FixMessages.PatternFix_convert_string_to_pattern_object, cuRewrite);

			String patternNameText= importRewrite.addImport(Pattern.class.getCanonicalName());
			ASTNode replacement= ast.newSimpleType(ASTNodeFactory.newName(ast, patternNameText));
			ASTNodes.replaceButKeepComment(rewrite, type, replacement, group);

			Expression unparanthesedInitializer= ASTNodes.getUnparenthesedExpression(initializer);
			MethodInvocation newCompileMethod= ast.newMethodInvocation();
			newCompileMethod.setExpression(ASTNodeFactory.newName(ast, patternNameText));
			newCompileMethod.setName(ast.newSimpleName(COMPILE_METHOD));
			newCompileMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, unparanthesedInitializer));

			boolean isInStaticInitializer= ASTNodes.getFirstAncestorOrNull(initializer, Initializer.class) != null;
			ASTNode typeDecl= ASTNodes.getFirstAncestorOrNull(initializer, TypeDeclaration.class, RecordDeclaration.class);
			boolean isInInterface= typeDecl instanceof TypeDeclaration && ((TypeDeclaration)typeDecl).isInterface();
			boolean isInLocalType= typeDecl instanceof AbstractTypeDeclaration && ((AbstractTypeDeclaration)typeDecl).isLocalTypeDeclaration();
			boolean isInNonStaticInnerClass= typeDecl instanceof AbstractTypeDeclaration && ((AbstractTypeDeclaration)typeDecl).isMemberTypeDeclaration()
					&& (((AbstractTypeDeclaration)typeDecl).getModifiers() & Modifier.STATIC) != Modifier.STATIC;

			if (unparanthesedInitializer instanceof StringLiteral && typeDecl instanceof AbstractTypeDeclaration
					&& !isInStaticInitializer && !isInInterface && !isInLocalType && !isInNonStaticInnerClass) {
				VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
				Set<String> addedFields= addedPatternFields.get(typeDecl);
				if (addedFields == null) {
					addedFields= new HashSet<>();
					addedPatternFields.put(typeDecl, addedFields);
				}
				String newFieldName= getUniqueFieldName(addedFields);
				newFragment.setName(ast.newSimpleName(newFieldName));
				newFragment.setInitializer(newCompileMethod);
				FieldDeclaration newFieldDeclaration= ast.newFieldDeclaration(newFragment);
				newFieldDeclaration.setType(ast.newSimpleType(ASTNodeFactory.newName(ast, patternNameText)));
				newFieldDeclaration.modifiers().addAll(ASTNodeFactory.newModifiers(ast, Modifier.STATIC | Modifier.PRIVATE | Modifier.FINAL));
				int insertionIndex= findInsertionIndex((AbstractTypeDeclaration)typeDecl);
				int addedFieldCount= addedFields.size();
				rewrite.getListRewrite(typeDecl,
						ASTNodes.getBodyDeclarationsProperty(typeDecl)).insertAt(newFieldDeclaration, insertionIndex + addedFieldCount, group);
				addedFields.add(newFieldName);
				ASTNodes.replaceButKeepComment(rewrite, initializer, ast.newSimpleName(newFieldName), group);
			} else {
			    ASTNodes.replaceButKeepComment(rewrite, initializer, newCompileMethod, group);
			}

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

				ASTNodes.replaceButKeepComment(rewrite, oldMethodInvocation, newMethod, group);
			}
		}

		private String getUniqueFieldName(Set<String> usedPatternNames) {
			List<String> usedNames= getVisibleVariablesInScope(initializer);
			String newPatternName= varName + "_pattern"; //$NON-NLS-1$
			String newName= newPatternName;
			int i= 2;
			boolean finished= false;
			while (!finished) {
				finished= true;
				for (String usedName : usedNames) {
					if (usedName.equals(newName)) {
						newName= newPatternName + i++;
						finished= false;
						break;
					}
				}
				for (String patternName : usedPatternNames) {
					if (patternName.equals(newName)) {
						newName= newPatternName + i++;
						finished= false;
						break;
					}
				}
			}
			return newName;
		}

		private List<String> getVisibleVariablesInScope(ASTNode node) {
			List<String> variableNames= new ArrayList<>();
			CompilationUnit root= (CompilationUnit) node.getRoot();
			IBinding[] bindings= new ScopeAnalyzer(root).
					getDeclarationsInScope(node.getStartPosition(), ScopeAnalyzer.VARIABLES | ScopeAnalyzer.CHECK_VISIBILITY);
			for (IBinding binding : bindings) {
				variableNames.add(binding.getName());
			}
			return variableNames;
		}

		private int findInsertionIndex(AbstractTypeDeclaration typeDecl) {
			List<BodyDeclaration> decls= typeDecl.bodyDeclarations();
			boolean finished= false;
			BodyDeclaration bodyDecl= (BodyDeclaration)ASTNodes.getFirstAncestorOrNull(initializer, MethodDeclaration.class, FieldDeclaration.class);
			while (!finished) {
				for (int i= 0; i < decls.size(); ++i) {
					if (decls.get(i).equals(bodyDecl)) {
						return i;
					}
				}
				bodyDecl= (BodyDeclaration)ASTNodes.getFirstAncestorOrNull(bodyDecl, MethodDeclaration.class, FieldDeclaration.class);
				if (bodyDecl == null) {
					finished= true;
				}
			}
			return 0; // default to insert at 0
		}

	}
}
