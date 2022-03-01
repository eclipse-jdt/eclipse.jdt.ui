/*******************************************************************************
 * Copyright (c) 2020, 2021 Fabrice TIERCELIN and others.
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
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.InterruptibleVisitor;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that makes inner class <code>static</code>:
 * <ul>
 * <li>It should not use top level class members</li>
 * <li>Inner class instantiation from top level class object are rewritten without top level class object access</li>
 * <li>The top level class should not be inheritable or the inner class must be <code>private</code></li>
 * </ul>
 */
public class StaticInnerClassCleanUp extends AbstractMultiFix {

	private final String JUPITER_NESTED= "org.junit.jupiter.api.Nested"; //$NON-NLS-1$

	public StaticInnerClassCleanUp() {
		this(Collections.emptyMap());
	}

	public StaticInnerClassCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.STATIC_INNER_CLASS);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.STATIC_INNER_CLASS)) {
			return new String[] { MultiFixMessages.StaticInnerClassCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		if (isEnabled(CleanUpConstants.STATIC_INNER_CLASS)) {
			bld.append("public static class InnerClass {\n"); //$NON-NLS-1$
		} else {
			bld.append("public class InnerClass {\n"); //$NON-NLS-1$
		}
		bld.append("    int i;\n"); //$NON-NLS-1$
		bld.append("\n"); //$NON-NLS-1$
		bld.append("    public boolean anotherMethod() {\n"); //$NON-NLS-1$
		bld.append("        return true;\n"); //$NON-NLS-1$
		bld.append("    }\n"); //$NON-NLS-1$
		bld.append("}\n"); //$NON-NLS-1$

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.STATIC_INNER_CLASS)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			class TopLevelClassMemberVisitor extends InterruptibleVisitor {
				private final TypeDeclaration innerClass;
				private final Set<ITypeBinding> genericityTypes;
				private boolean isTopLevelClassMemberUsed;

				public TopLevelClassMemberVisitor(final TypeDeclaration innerClass, final Set<ITypeBinding> genericityTypes) {
					this.innerClass= innerClass;
					this.genericityTypes= genericityTypes;
				}

				public boolean isTopLevelClassMemberUsed() {
					return isTopLevelClassMemberUsed;
				}

				@Override
				public boolean visit(final SimpleName node) {
					if (innerClass.getName() == node
							|| node.getLocationInParent() == QualifiedName.NAME_PROPERTY
							|| node.getLocationInParent() == FieldAccess.NAME_PROPERTY
							|| node.getLocationInParent() == SuperFieldAccess.NAME_PROPERTY) {
						return true;
					}

					if (node.getLocationInParent() == MethodInvocation.NAME_PROPERTY) {
						MethodInvocation methodInvocation= (MethodInvocation) node.getParent();

						if (methodInvocation.getExpression() != null) {
							// The expression will be evaluated instead
							return true;
						}
					}

					IBinding binding= node.resolveBinding();
					ASTNode root= node.getRoot();

					if (binding == null || !(root instanceof CompilationUnit)) {
						isTopLevelClassMemberUsed= true;
						return interruptVisit();
					}

					if (!Modifier.isStatic(binding.getModifiers())
							&& binding.getKind() != IBinding.ANNOTATION
							&& binding.getKind() != IBinding.MEMBER_VALUE_PAIR
							&& binding.getKind() != IBinding.MODULE
							&& binding.getKind() != IBinding.PACKAGE
							&& binding.getKind() != IBinding.TYPE) {
						ASTNode declaration= ((CompilationUnit) root).findDeclaringNode(binding);

						if (declaration == null || !ASTNodes.isParent(declaration, innerClass)) {
							isTopLevelClassMemberUsed= true;
							return interruptVisit();
						}
					}

					return true;
				}

				@Override
				public boolean visit(final ThisExpression node) {
					if (node.getQualifier() == null
							|| ASTNodes.isSameVariable(innerClass.getName(), node.getQualifier())) {
						return true;
					}

					isTopLevelClassMemberUsed= true;
					return interruptVisit();
				}

				@Override
				public boolean visit(final ClassInstanceCreation node) {
					if (node.getExpression() != null) {
						// The expression will be evaluated instead
						return true;
					}

					ITypeBinding type= node.resolveTypeBinding();

					if (type != null
							&& (type.isTopLevel() || Modifier.isStatic(type.getModifiers()))) {
						return true;
					}

					isTopLevelClassMemberUsed= true;
					return interruptVisit();
				}

				@Override
				public boolean visit(final SimpleType node) {
					if (node.resolveBinding() == null || genericityTypes.contains(node.resolveBinding())) {
						isTopLevelClassMemberUsed= true;
						return interruptVisit();
					}

					return true;
				}
			}

			class DynamicClassInstanceCreationVisitor extends ASTVisitor {
				private final ITypeBinding innerClass;
				private final List<ClassInstanceCreation> dynamicClassInstanceCreations = new ArrayList<>();

				public DynamicClassInstanceCreationVisitor(final ITypeBinding innerClass) {
					this.innerClass= innerClass;
				}

				public List<ClassInstanceCreation> getDynamicClassInstanceCreations() {
					return dynamicClassInstanceCreations;
				}

				@Override
				public boolean visit(final ClassInstanceCreation node) {
					if (innerClass.getErasure().isEqualTo(node.getType().resolveBinding().getErasure())
							&& node.getExpression() != null) {
						dynamicClassInstanceCreations.add(node);
					}

					return true;
				}

				@Override
				public boolean visit(final TypeDeclaration node) {
					return !innerClass.getErasure().isEqualTo(node.resolveBinding().getErasure());
				}
			}

			@Override
			public boolean visit(final TypeDeclaration visited) {
				if (!visited.isInterface()
						&& !Modifier.isStatic(visited.getModifiers())
						&& visited.resolveBinding() != null) {
					Set<ITypeBinding> genericityTypes= new HashSet<>();
					ASTNode enclosingType= ASTNodes.getFirstAncestorOrNull(visited, TypeDeclaration.class, MethodDeclaration.class);

					if (enclosingType instanceof TypeDeclaration && ((TypeDeclaration) enclosingType).isInterface()) {
						// An inner class in an interface is static by default so the keyword is redundant
						return true;
					}

					List<IExtendedModifier> modifiers= visited.modifiers();
					for (IExtendedModifier modifier : modifiers) {
						if (modifier.isAnnotation()) {
							Annotation annotation= (Annotation)modifier;
							if (annotation.isMarkerAnnotation()) {
								Name name= annotation.getTypeName();
								ITypeBinding nameBinding= name.resolveTypeBinding();
								if (nameBinding != null && nameBinding.getQualifiedName().equals(JUPITER_NESTED)) {
									return true;
								}
							}
						}
					}

					TypeDeclaration topLevelClass= null;

					while (enclosingType instanceof TypeDeclaration) {
						topLevelClass= (TypeDeclaration) enclosingType;

						if (!Modifier.isPrivate(visited.getModifiers()) && !Modifier.isFinal(topLevelClass.getModifiers())) {
							// An inherited class could use this syntax:
							// this.new InnerClass()
							return true;
						}

						ITypeBinding topLevelClassBinding= topLevelClass.resolveBinding();

						if (topLevelClassBinding == null) {
							return true;
						}

						Collections.addAll(genericityTypes, topLevelClassBinding.getTypeParameters());
						enclosingType= ASTNodes.getTypedAncestor(topLevelClass, TypeDeclaration.class);

						if (enclosingType != null && !Modifier.isStatic(topLevelClass.getModifiers())) {
							return true;
						}
					}

					if (topLevelClass != null && !hasInnerDynamicMotherType(visited)) {
						TopLevelClassMemberVisitor topLevelClassMemberVisitor= new TopLevelClassMemberVisitor(visited, genericityTypes);
						topLevelClassMemberVisitor.traverseNodeInterruptibly(visited);

						if (!topLevelClassMemberVisitor.isTopLevelClassMemberUsed()) {
							DynamicClassInstanceCreationVisitor dynamicClassInstanceCreationVisitor= new DynamicClassInstanceCreationVisitor(visited.resolveBinding());
							topLevelClass.accept(dynamicClassInstanceCreationVisitor);

							rewriteOperations.add(new StaticInnerClassOperation(visited, dynamicClassInstanceCreationVisitor.getDynamicClassInstanceCreations()));
							return false;
						}
					}
				}

				return true;
			}

			private boolean hasInnerDynamicMotherType(final TypeDeclaration visited) {
				if (visited.resolveBinding() == null) {
					return true;
				}

				ITypeBinding motherType= visited.resolveBinding().getSuperclass();

				while (motherType != null) {
					if (motherType.getDeclaringClass() != null && !Modifier.isStatic(motherType.getModifiers())) {
						return true;
					}

					motherType= motherType.getSuperclass();
				}

				return false;
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.StaticInnerClassCleanUp_description, unit,
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

	private static class StaticInnerClassOperation extends CompilationUnitRewriteOperation {
		private final TypeDeclaration visited;
		private final List<ClassInstanceCreation> dynamicClassInstanceCreations;

		public StaticInnerClassOperation(final TypeDeclaration visited, final List<ClassInstanceCreation> dynamicClassInstanceCreations) {
			this.visited= visited;
			this.dynamicClassInstanceCreations= dynamicClassInstanceCreations;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			ListRewrite listRewrite= rewrite.getListRewrite(visited, TypeDeclaration.MODIFIERS2_PROPERTY);
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.StaticInnerClassCleanUp_description, cuRewrite);

			for (ClassInstanceCreation dynamicClassInstanceCreation : dynamicClassInstanceCreations) {
				if (!dynamicClassInstanceCreation.getType().isQualifiedType()
						&& dynamicClassInstanceCreation.getType().resolveBinding() != null) {
					ThisExpression thisExpression= ASTNodes.as(dynamicClassInstanceCreation.getExpression(), ThisExpression.class);

					if (thisExpression != null) {
						if (thisExpression.getQualifier() != null) {
							SimpleName newSimpleName= ast.newSimpleName(dynamicClassInstanceCreation.getType().resolveBinding().getName());
							QualifiedName newQualifiedName= ast.newQualifiedName(ASTNodes.createMoveTarget(rewrite, thisExpression.getQualifier()), newSimpleName);
							Type newType= ast.newSimpleType(newQualifiedName);
							rewrite.replace(dynamicClassInstanceCreation.getType(), newType, group);
						}
					} else {
						String qualifiedName= dynamicClassInstanceCreation.getExpression().resolveTypeBinding().getErasure().getQualifiedName();
						String[] qualifiedNameTokens= qualifiedName.split("\\."); //$NON-NLS-1$
						Name newQualifiedName= null;

						for (String qualifiedNameToken : qualifiedNameTokens) {
							if (newQualifiedName == null) {
								newQualifiedName= ast.newSimpleName(qualifiedNameToken);
							} else {
								newQualifiedName= ast.newQualifiedName(newQualifiedName, ast.newSimpleName(qualifiedNameToken));
							}
						}

						SimpleName newSimpleName= ast.newSimpleName(dynamicClassInstanceCreation.getType().resolveBinding().getName());
						newQualifiedName= ast.newQualifiedName(newQualifiedName, newSimpleName);
						Type newType= ast.newSimpleType(newQualifiedName);
						rewrite.replace(dynamicClassInstanceCreation.getType(), newType, group);
					}
				}

				rewrite.set(dynamicClassInstanceCreation, ClassInstanceCreation.EXPRESSION_PROPERTY, null, group);
			}

			List<IExtendedModifier> modifiers= visited.modifiers();
			Modifier static0= ast.newModifier(ModifierKeyword.STATIC_KEYWORD);

			if (modifiers.isEmpty()) {
				listRewrite.insertFirst(static0, group);
			} else {
				IExtendedModifier lastModifier= modifiers.get(modifiers.size() - 1);

				if (lastModifier.isModifier() && ((Modifier) lastModifier).isFinal()) {
					listRewrite.insertBefore(static0, (ASTNode) lastModifier, group);
				} else {
					listRewrite.insertLast(static0, group);
				}
			}
		}
	}
}
