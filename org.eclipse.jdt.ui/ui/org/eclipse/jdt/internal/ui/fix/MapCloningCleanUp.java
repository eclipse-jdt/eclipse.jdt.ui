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
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that replaces creating a new Map, then invoking Map.putAll() on it, by creating the new Map with the other Map as parameter.
 */
public class MapCloningCleanUp extends AbstractMultiFix {
	public MapCloningCleanUp() {
		this(Collections.emptyMap());
	}

	public MapCloningCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.MAP_CLONING);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.MAP_CLONING)) {
			return new String[] { MultiFixMessages.MapCloningCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.MAP_CLONING)) {
			return "Map<String, String> output = new HashMap<String, String>(map);\n\n"; //$NON-NLS-1$
		}

		return "Map<String, String> output = new HashMap<String, String>();\n" //$NON-NLS-1$
				+ "output.putAll(map);\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.MAP_CLONING)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final Block node) {
				NewAndPutAllMethodVisitor newAndPutAllMethodVisitor= new NewAndPutAllMethodVisitor(node);
				node.accept(newAndPutAllMethodVisitor);
				return newAndPutAllMethodVisitor.result;
			}

			final class NewAndPutAllMethodVisitor extends ASTVisitor {
				private final Block startNode;
				private boolean result= true;

				public NewAndPutAllMethodVisitor(final Block startNode) {
					this.startNode= startNode;
				}

				@Override
				public boolean visit(final Block node) {
					return startNode == node;
				}

				@Override
				public boolean visit(final ExpressionStatement node) {
					MethodInvocation methodInvocation= ASTNodes.asExpression(node, MethodInvocation.class);

					if (result && ASTNodes.usesGivenSignature(methodInvocation, Map.class.getCanonicalName(), "putAll", Map.class.getCanonicalName())) { //$NON-NLS-1$
						Expression arg0= (Expression) methodInvocation.arguments().get(0);
						Statement previousStatement= ASTNodes.getPreviousSibling(node);
						VariableDeclarationFragment fragment= ASTNodes.getUniqueFragment(previousStatement);

						Assignment assignment= ASTNodes.asExpression(previousStatement, Assignment.class);
						if (ASTNodes.hasOperator(assignment, Assignment.Operator.ASSIGN)) {
							SimpleName leftHandSide= ASTNodes.as(assignment.getLeftHandSide(), SimpleName.class);

							if (leftHandSide != null && ASTNodes.isSameLocalVariable(leftHandSide, methodInvocation.getExpression())) {
								return maybeReplaceInitializer(assignment.getRightHandSide(), arg0, node);
							}
						} else if (fragment != null && ASTNodes.isSameLocalVariable(fragment, methodInvocation.getExpression())) {
							return maybeReplaceInitializer(fragment.getInitializer(), arg0, node);
						}
					}

					return true;
				}

				private boolean maybeReplaceInitializer(final Expression nodeToReplace, final Expression arg0,
						final ExpressionStatement nodeToRemove) {
					ClassInstanceCreation cic= ASTNodes.as(nodeToReplace, ClassInstanceCreation.class);

					if (canReplaceInitializer(cic, arg0) && ASTNodes.isCastCompatible(nodeToReplace, arg0)) {
						rewriteOperations.add(new MapCloningOperation(nodeToRemove, cic, arg0));
						result= false;
						return false;
					}

					return true;
				}

				private boolean canReplaceInitializer(final ClassInstanceCreation cic, final Expression sourceMap) {
					if (cic == null || cic.getAnonymousClassDeclaration() != null) {
						return false;
					}

					if (cic.arguments().isEmpty()) {
						return ASTNodes.hasType(cic,
								ConcurrentHashMap.class.getCanonicalName(),
								ConcurrentSkipListMap.class.getCanonicalName(),
								Hashtable.class.getCanonicalName(),
								HashMap.class.getCanonicalName(),
								IdentityHashMap.class.getCanonicalName(),
								LinkedHashMap.class.getCanonicalName(),
								TreeMap.class.getCanonicalName(),
								WeakHashMap.class.getCanonicalName());
					}

					return isValidCapacityParameter(sourceMap, cic.arguments())
							&& ASTNodes.hasType(cic,
									ConcurrentHashMap.class.getCanonicalName(),
									Hashtable.class.getCanonicalName(),
									HashMap.class.getCanonicalName(),
									IdentityHashMap.class.getCanonicalName(),
									LinkedHashMap.class.getCanonicalName(),
									WeakHashMap.class.getCanonicalName());
				}

				private boolean isValidCapacityParameter(final Expression sourceMap, final List<Expression> args) {
					if (args.size() == 1 && ASTNodes.isPrimitive(args.get(0), int.class.getSimpleName())) {
						Long zero= ASTNodes.getIntegerLiteral(args.get(0));

						if (zero != null) {
							return Long.valueOf(0).equals(zero);
						}

						MethodInvocation methodInvocation= ASTNodes.as(args.get(0), MethodInvocation.class);

						return ASTNodes.usesGivenSignature(methodInvocation, Map.class.getCanonicalName(), "size") && ASTNodes.match(methodInvocation.getExpression(), sourceMap); //$NON-NLS-1$
					}

					return false;
				}
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.MapCloningCleanUp_description, unit,
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

	private static class MapCloningOperation extends CompilationUnitRewriteOperation {
		private final ExpressionStatement nodeToRemove;
		private final ClassInstanceCreation classInstanceCreation;
		private final Expression arg0;

		public MapCloningOperation(final ExpressionStatement nodeToRemove, final ClassInstanceCreation classInstanceCreation, final Expression arg0) {
			this.nodeToRemove= nodeToRemove;
			this.classInstanceCreation= classInstanceCreation;
			this.arg0= arg0;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			ListRewrite listRewrite= rewrite.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
			TextEditGroup group= createTextEditGroup(MultiFixMessages.MapCloningCleanUp_description, cuRewrite);

			if (classInstanceCreation.arguments() == null || classInstanceCreation.arguments().isEmpty()) {
				listRewrite.insertFirst(ASTNodes.createMoveTarget(rewrite, arg0), group);
			} else {
				listRewrite.replace((ASTNode) classInstanceCreation.arguments().get(0), ASTNodes.createMoveTarget(rewrite, arg0), group);
			}

			rewrite.remove(nodeToRemove, group);
		}
	}
}
