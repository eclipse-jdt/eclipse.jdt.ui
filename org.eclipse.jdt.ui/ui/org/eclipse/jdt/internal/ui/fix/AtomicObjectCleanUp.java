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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
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
 * A fix that replaces an array with one index by an atomic object.
 */
public class AtomicObjectCleanUp extends AbstractMultiFix {
	public AtomicObjectCleanUp() {
		this(Collections.emptyMap());
	}

	public AtomicObjectCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.ATOMIC_OBJECT);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.ATOMIC_OBJECT)) {
			return new String[] { MultiFixMessages.CodeStyleCleanUp_AtomicObject_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.ATOMIC_OBJECT)) {
			return "AtomicBoolean booleanRef= new AtomicBoolean();\n" //$NON-NLS-1$
					+ "Runnable runnable = () -> booleanRef.set(true);\n" //$NON-NLS-1$
					+ "runnable.run();\n" //$NON-NLS-1$
					+ "boolean b = booleanRef.get();\n"; //$NON-NLS-1$
		}

		return "boolean[] booleanRef= new boolean[1];\n" //$NON-NLS-1$
				+ "Runnable runnable = () -> booleanRef[0] = true;\n" //$NON-NLS-1$
				+ "runnable.run();\n" //$NON-NLS-1$
				+ "boolean b = booleanRef[0];\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.ATOMIC_OBJECT) || !JavaModelUtil.is50OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final Block node) {
				ArrayOccurrencesVisitor arrayOccurrencesVisitor= new ArrayOccurrencesVisitor(node);
				node.accept(arrayOccurrencesVisitor);
				return arrayOccurrencesVisitor.result;
			}

			final class ArrayOccurrencesVisitor extends ASTVisitor {
				private final Block startNode;
				private boolean result= true;

				public ArrayOccurrencesVisitor(final Block startNode) {
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
					return visitVariable(node.getType(), fragment.resolveBinding(), fragment.extraDimensions(), fragment.getName(), fragment.getInitializer());
				}

				@Override
				public boolean visit(final VariableDeclarationExpression node) {
					if (node.fragments().size() != 1) {
						return true;
					}

					VariableDeclarationFragment fragment= (VariableDeclarationFragment) node.fragments().get(0);
					return visitVariable(node.getType(), fragment.resolveBinding(), fragment.extraDimensions(), fragment.getName(), fragment.getInitializer());
				}

				@Override
				public boolean visit(final SingleVariableDeclaration node) {
					return visitVariable(node.getType(), node.resolveBinding(), node.extraDimensions(), node.getName(), node.getInitializer());
				}

				private boolean visitVariable(final Type type, final IVariableBinding variableBinding, final List<?> variableDimensions, final SimpleName declaration, final Expression initializer) {
					ArrayCreation arrayCreation= ASTNodes.as(initializer, ArrayCreation.class);

					if (result
							&& arrayCreation != null
							&& (arrayCreation.getInitializer() != null
									? arrayCreation.getInitializer().expressions().size() == 1
									: arrayCreation.dimensions().size() == 1 && Long.valueOf(1L).equals(ASTNodes.getIntegerLiteral((Expression) arrayCreation.dimensions().get(0))))
							&& type != null
							&& type.resolveBinding() != null
							&& (type.resolveBinding().isArray()
									? variableDimensions.isEmpty() && type.resolveBinding().getDimensions() == 1 && equalNotNull(type.resolveBinding().getElementType(), arrayCreation.getType().getElementType().resolveBinding())
											: variableDimensions.size() == 1 && equalNotNull(type.resolveBinding(), arrayCreation.getType().getElementType().resolveBinding()))
							&& !ASTNodes.hasType(arrayCreation.getType().getElementType().resolveBinding(),
									double.class.getCanonicalName(),
									float.class.getCanonicalName(),
									short.class.getCanonicalName(),
									char.class.getCanonicalName(),
									byte.class.getCanonicalName())) {
						VarDefinitionsUsesVisitor varOccurrencesVisitor= new VarDefinitionsUsesVisitor(variableBinding,
								startNode, true).find();

						List<SimpleName> reads= varOccurrencesVisitor.getReads();
						List<SimpleName> writes= varOccurrencesVisitor.getWrites();
						writes.remove(declaration);

						if (writes.isEmpty()) {
							Set<Assignment> assignmentReads= new HashSet<>();
							Set<ArrayAccess> accessReads= new HashSet<>();

							for (SimpleName simpleName : reads) {
								if (!isReadValid(simpleName, assignmentReads, accessReads)) {
									return true;
								}
							}

							boolean hasOneWriteInDynamicCode= false;

							for (Assignment assignmentRead : assignmentReads) {
								ASTNode dynamicCode= ASTNodes.getFirstAncestorOrNull(assignmentRead, LambdaExpression.class, AnonymousClassDeclaration.class);

								if (dynamicCode != null && ASTNodes.isParent(dynamicCode, startNode)) {
									hasOneWriteInDynamicCode= true;
									break;
								}
							}

							if (hasOneWriteInDynamicCode) {
								rewriteOperations.add(new AtomicObjectOperation(type, variableDimensions, arrayCreation, assignmentReads, accessReads));

								result= false;
								return false;
							}
						}
					}

					return true;
				}

				private boolean equalNotNull(ITypeBinding elementType, ITypeBinding resolveBinding) {
					return elementType != null && Objects.equals(elementType, resolveBinding);
				}

				private boolean isReadValid(final SimpleName simpleName, final Set<Assignment> assignmentReads, final Set<ArrayAccess> accessReads) {
					if (simpleName.getParent() instanceof ArrayAccess
							&& simpleName.getLocationInParent() == ArrayAccess.ARRAY_PROPERTY) {
						ArrayAccess arrayAccess= (ArrayAccess) simpleName.getParent();

						if (Long.valueOf(0L).equals(ASTNodes.getIntegerLiteral(arrayAccess.getIndex()))) {
							if (arrayAccess.getParent() instanceof Assignment
									&& arrayAccess.getLocationInParent() == Assignment.LEFT_HAND_SIDE_PROPERTY) {
								Assignment assignment= (Assignment) arrayAccess.getParent();

								if (ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)
										&& (assignment.getParent() instanceof ExpressionStatement
										|| assignment.getParent() instanceof LambdaExpression && assignment.getLocationInParent() == LambdaExpression.BODY_PROPERTY)) {
									assignmentReads.add(assignment);
									return true;
								}
							} else if ((!(arrayAccess.getParent() instanceof PrefixExpression)
									|| !ASTNodes.hasOperator((PrefixExpression) arrayAccess.getParent(), PrefixExpression.Operator.INCREMENT, PrefixExpression.Operator.DECREMENT))
									&& (!(arrayAccess.getParent() instanceof PostfixExpression)
											|| !ASTNodes.hasOperator((PostfixExpression) arrayAccess.getParent(), PostfixExpression.Operator.INCREMENT, PostfixExpression.Operator.DECREMENT))) {
								accessReads.add(arrayAccess);
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

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.CodeStyleCleanUp_AtomicObject_description, unit,
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

	private static class AtomicObjectOperation extends CompilationUnitRewriteOperation {
		private final Type type;
		private final List<?> variableDimensions;
		private final ArrayCreation arrayCreation;
		private final Set<Assignment> assignmentReads;
		private final Set<ArrayAccess> accessReads;

		public AtomicObjectOperation(final Type type, final List<?> variableDimensions, final ArrayCreation arrayCreation, final Set<Assignment> assignmentReads, final Set<ArrayAccess> accessReads) {
			this.type= type;
			this.variableDimensions= variableDimensions;
			this.arrayCreation= arrayCreation;
			this.assignmentReads= assignmentReads;
			this.accessReads= accessReads;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CodeStyleCleanUp_AtomicObject_description, cuRewrite);

			Class<?> atomicClass;
			Type objectClass= null;
			if (ASTNodes.hasType(arrayCreation.getType().getElementType().resolveBinding(), boolean.class.getCanonicalName())) {
				atomicClass= AtomicBoolean.class;
			} else if (ASTNodes.hasType(arrayCreation.getType().getElementType().resolveBinding(), int.class.getCanonicalName())) {
				atomicClass= AtomicInteger.class;
			} else if (ASTNodes.hasType(arrayCreation.getType().getElementType().resolveBinding(), long.class.getCanonicalName())) {
				atomicClass= AtomicLong.class;
			} else {
				atomicClass= AtomicReference.class;
				objectClass= arrayCreation.getType().getElementType();
			}

			String atomicClassNameText= importRewrite.addImport(atomicClass.getCanonicalName());
			Type atomicInstance= ast.newSimpleType(ASTNodeFactory.newName(ast, atomicClassNameText));

			if (objectClass != null) {
				ParameterizedType newParameterizedType= ast.newParameterizedType(atomicInstance);

				if (!JavaModelUtil.is17OrHigher(((CompilationUnit) type.getRoot()).getJavaElement().getJavaProject())) {
					newParameterizedType.typeArguments().add(rewrite.createCopyTarget(objectClass));
				}

				atomicInstance= newParameterizedType;
			}

			ClassInstanceCreation newAtomicObject= ast.newClassInstanceCreation();
			newAtomicObject.setType(atomicInstance);

			if (arrayCreation.getInitializer() != null) {
				List<Expression> arguments= newAtomicObject.arguments();
				arguments.add(ASTNodes.createMoveTarget(rewrite, (Expression) arrayCreation.getInitializer().expressions().get(0)));
			}

			ASTNodes.replaceButKeepComment(rewrite, arrayCreation, newAtomicObject, group);

			for (Object variableDimension : variableDimensions) {
				rewrite.remove((ASTNode) variableDimension, group);
			}

			Type atomicType= ast.newSimpleType(ASTNodeFactory.newName(ast, atomicClassNameText));

			if (objectClass != null) {
				ParameterizedType newParameterizedType= ast.newParameterizedType(atomicType);
				newParameterizedType.typeArguments().add(rewrite.createCopyTarget(objectClass));
				atomicInstance= newParameterizedType;
			}

			ASTNodes.replaceButKeepComment(rewrite, type, atomicInstance, group);

			for (ArrayAccess accessRead : accessReads) {
				MethodInvocation newMethodInvocation= ast.newMethodInvocation();
				newMethodInvocation.setExpression(ASTNodes.createMoveTarget(rewrite, accessRead.getArray()));
				newMethodInvocation.setName(ast.newSimpleName("get")); //$NON-NLS-1$
				ASTNodes.replaceButKeepComment(rewrite, accessRead, newMethodInvocation, group);
			}

			for (Assignment assignmentRead : assignmentReads) {
				MethodInvocation newMethodInvocation= ast.newMethodInvocation();
				newMethodInvocation.setExpression(ASTNodes.createMoveTarget(rewrite, ((ArrayAccess) assignmentRead.getLeftHandSide()).getArray()));
				newMethodInvocation.setName(ast.newSimpleName("set")); //$NON-NLS-1$
				newMethodInvocation.arguments().add(ASTNodes.createMoveTarget(rewrite, assignmentRead.getRightHandSide()));
				ASTNodes.replaceButKeepComment(rewrite, assignmentRead, newMethodInvocation, group);
			}
		}
	}
}
