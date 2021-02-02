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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
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
 * A fix that makes inner <code>class</code> static:
 * <ul>
 * <li>It should not use top level <code>class</code> members</li>
 * </ul>
 */
public class StaticInnerClassCleanUp extends AbstractMultiFix {
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
				private boolean isTopLevelClassMemberUsed;

				public TopLevelClassMemberVisitor(final TypeDeclaration innerClass) {
					this.innerClass= innerClass;
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
			}

			@Override
			public boolean visit(final TypeDeclaration visited) {
				if (!visited.isInterface()) {
					TypeDeclaration parent= ASTNodes.getTypedAncestor(visited, TypeDeclaration.class);
					TypeDeclaration topLevelClass= null;

					while (parent != null) {
						topLevelClass= parent;
						parent= ASTNodes.getTypedAncestor(topLevelClass, TypeDeclaration.class);

						if (parent != null && !Modifier.isStatic(topLevelClass.getModifiers())) {
							return true;
						}
					}

					if (topLevelClass != null && !Modifier.isStatic(visited.getModifiers())) {
						TopLevelClassMemberVisitor topLevelClassMemberVisitor= new TopLevelClassMemberVisitor(visited);
						topLevelClassMemberVisitor.traverseNodeInterruptibly(visited);

						if (!topLevelClassMemberVisitor.isTopLevelClassMemberUsed()) {
							rewriteOperations.add(new StaticInnerClassOperation(visited));
							return false;
						}
					}
				}

				return true;
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

		public StaticInnerClassOperation(final TypeDeclaration visited) {
			this.visited= visited;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			ListRewrite listRewrite= rewrite.getListRewrite(visited, TypeDeclaration.MODIFIERS2_PROPERTY);
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.StaticInnerClassCleanUp_description, cuRewrite);

			List<?> modifiers= visited.modifiers();
			Modifier static0= ast.newModifier(ModifierKeyword.STATIC_KEYWORD);

			if (modifiers.isEmpty()) {
				listRewrite.insertFirst(static0, group);
			} else {
				IExtendedModifier lastModifier= (IExtendedModifier) modifiers.get(modifiers.size() - 1);

				if (lastModifier.isModifier() && ((Modifier) lastModifier).isFinal()) {
					listRewrite.insertBefore(static0, (ASTNode) lastModifier, group);
				} else {
					listRewrite.insertLast(static0, group);
				}
			}
		}
	}
}
