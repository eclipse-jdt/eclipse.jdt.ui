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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that avoids to create primitive wrapper when parsing a string:
 * <ul>
 * <li>The object should be used as a primitive and not as a wrapper.</li>
 * </ul>
 */
public class PrimitiveParsingCleanUp extends AbstractMultiFix {
	private static final String VALUE_OF_METHOD= "valueOf"; //$NON-NLS-1$
	private static final Class<?>[] WRAPPER_CLASSES= { Integer.class, Boolean.class, Long.class, Double.class, Character.class, Float.class, Short.class, Byte.class };

	public PrimitiveParsingCleanUp() {
		this(Collections.emptyMap());
	}

	public PrimitiveParsingCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.PRIMITIVE_PARSING);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.PRIMITIVE_PARSING)) {
			return new String[] { MultiFixMessages.PrimitiveParsingCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.PRIMITIVE_PARSING)) {
			return "" //$NON-NLS-1$
					+ "int number = Integer.parseInt(\"42\", 8);\n" //$NON-NLS-1$
					+ "Double.parseDouble(\"42.42\");\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "int number = Integer.valueOf(\"42\", 8);\n" //$NON-NLS-1$
				+ "new Double(\"42.42\").doubleValue();\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.PRIMITIVE_PARSING)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final MethodInvocation visited) {
				if (visited.getExpression() == null) {
					return true;
				}

				ITypeBinding destinationTypeBinding= ASTNodes.getTargetType(visited);
				String methodName= visited.getName().getIdentifier();

				if (destinationTypeBinding != null
						&& destinationTypeBinding.isPrimitive()
						&& VALUE_OF_METHOD.equals(methodName)) {
					for (Class<?> wrapperClass : WRAPPER_CLASSES) {
						String canonicalName= wrapperClass.getCanonicalName();
						String primitiveType= Bindings.getUnboxedTypeName(canonicalName);

						if (ASTNodes.usesGivenSignature(visited, canonicalName, VALUE_OF_METHOD, primitiveType)) {
							rewriteOperations.add(new PrimitiveParsingWithTheSingleArgumentOperation(visited));
							return false;
						}

						String parsingMethodName= getParsingMethodName(canonicalName, wrapperClass.getSimpleName(), visited);

						if (parsingMethodName != null
								&& isValueOfString(visited, canonicalName)) {
							rewriteOperations.add(new PrimitiveParsingMethodNameOperation(visited, parsingMethodName));
							return false;
						}
					}
				}

				ITypeBinding typeBinding= visited.getExpression().resolveTypeBinding();

				if (typeBinding != null
						&& visited.arguments().isEmpty()) {
					String primitiveValueMethodName= getPrimitiveValueMethodName(typeBinding.getQualifiedName());
					String parsingMethodName= getParsingMethodName(typeBinding.getQualifiedName(), typeBinding.getName(), visited);

					if (primitiveValueMethodName != null
							&& primitiveValueMethodName.equals(methodName)
							&& parsingMethodName != null) {
						ClassInstanceCreation classInstanceCreation= ASTNodes.as(visited.getExpression(), ClassInstanceCreation.class);
						MethodInvocation methodInvocation= ASTNodes.as(visited.getExpression(), MethodInvocation.class);

						if (classInstanceCreation != null) {
							List<Expression> classInstanceCreationArguments= classInstanceCreation.arguments();

							if (classInstanceCreationArguments.size() == 1
									&& ASTNodes.hasType(classInstanceCreationArguments.get(0), String.class.getCanonicalName())) {
								rewriteOperations.add(new PrimitiveParsingReplaceByParsingOperation(visited, typeBinding, parsingMethodName, classInstanceCreationArguments));
								return false;
							}
						} else if (methodInvocation != null
								&& isValueOfString(methodInvocation, typeBinding.getQualifiedName())) {
							rewriteOperations.add(new PrimitiveParsingReplaceByParsingOperation(visited, typeBinding, parsingMethodName, methodInvocation.arguments()));
							return false;
						}
					}
				}

				return true;
			}

			private boolean isValueOfString(final MethodInvocation visited, final String declaringTypeQualifiedName) {
				return ASTNodes.usesGivenSignature(visited, declaringTypeQualifiedName, VALUE_OF_METHOD, String.class.getCanonicalName())
						|| (Integer.class.getCanonicalName().equals(declaringTypeQualifiedName)
								|| Long.class.getCanonicalName().equals(declaringTypeQualifiedName)
								|| Short.class.getCanonicalName().equals(declaringTypeQualifiedName)
								|| Byte.class.getCanonicalName().equals(declaringTypeQualifiedName))
						&& ASTNodes.usesGivenSignature(visited, declaringTypeQualifiedName, VALUE_OF_METHOD, String.class.getCanonicalName(), int.class.getSimpleName());
			}

			private String getPrimitiveValueMethodName(final String wrapperFullyQualifiedName) {
				String primitiveTypeName= Bindings.getUnboxedTypeName(wrapperFullyQualifiedName);

				if (primitiveTypeName != null) {
					return primitiveTypeName + "Value"; //$NON-NLS-1$
				}

				return null;
			}

			private String getParsingMethodName(final String wrapperFullyQualifiedName, final String wrapperSimpleName, final MethodInvocation visited) {
				if (Integer.class.getCanonicalName().equals(wrapperFullyQualifiedName)) {
					return "parseInt"; //$NON-NLS-1$
				}

				IJavaProject javaProject= ((CompilationUnit) visited.getRoot()).getJavaElement().getJavaProject();

				if ((Boolean.class.getCanonicalName().equals(wrapperFullyQualifiedName) && JavaModelUtil.is50OrHigher(javaProject))
						|| Long.class.getCanonicalName().equals(wrapperFullyQualifiedName)
						|| (Double.class.getCanonicalName().equals(wrapperFullyQualifiedName) && JavaModelUtil.is1d2OrHigher(javaProject))
						|| (Float.class.getCanonicalName().equals(wrapperFullyQualifiedName) && JavaModelUtil.is1d2OrHigher(javaProject))
						|| Short.class.getCanonicalName().equals(wrapperFullyQualifiedName)
						|| Byte.class.getCanonicalName().equals(wrapperFullyQualifiedName)) {
					return "parse" + wrapperSimpleName; //$NON-NLS-1$
				}

				return null;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.PrimitiveParsingCleanUp_description, unit,
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

	private static class PrimitiveParsingWithTheSingleArgumentOperation extends CompilationUnitRewriteOperation {
		private final MethodInvocation visited;

		public PrimitiveParsingWithTheSingleArgumentOperation(final MethodInvocation visited) {
			this.visited= visited;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.PrimitiveParsingCleanUp_description, cuRewrite);

			ASTNodes.replaceButKeepComment(rewrite, visited, ASTNodes.createMoveTarget(rewrite, (Expression) visited.arguments().get(0)), group);
		}
	}

	private static class PrimitiveParsingMethodNameOperation extends CompilationUnitRewriteOperation {
		private final MethodInvocation visited;
		private final String methodName;

		public PrimitiveParsingMethodNameOperation(final MethodInvocation visited, final String parsingMethodName) {
			this.visited= visited;
			this.methodName= parsingMethodName;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.PrimitiveParsingCleanUp_description, cuRewrite);

			rewrite.set(visited, MethodInvocation.NAME_PROPERTY, ast.newSimpleName(methodName), group);
		}
	}

	private static class PrimitiveParsingReplaceByParsingOperation extends CompilationUnitRewriteOperation {
		private final MethodInvocation visited;
		private final ITypeBinding typeBinding;
		private final String methodName;
		private final List<Expression> arguments;

		public PrimitiveParsingReplaceByParsingOperation(final MethodInvocation visited, final ITypeBinding typeBinding, final String methodName,
				final List<Expression> arguments) {
			this.visited= visited;
			this.typeBinding= typeBinding;
			this.methodName= methodName;
			this.arguments= arguments;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.PrimitiveParsingCleanUp_description, cuRewrite);

			MethodInvocation newMethodInvocation= ast.newMethodInvocation();
			newMethodInvocation.setExpression(ast.newSimpleName(typeBinding.getName()));
			newMethodInvocation.setName(ast.newSimpleName(methodName));

			for (Expression argument : arguments) {
				newMethodInvocation.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(argument)));
			}

			ASTNodes.replaceButKeepComment(rewrite, visited, newMethodInvocation, group);
		}
	}
}
