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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;

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
 * A fix that replaces creating a new Collection, then invoking Collection.addAll() on it, by creating the new Collection with the other Collection as parameter.
 */
public class CollectionCloningCleanUp extends AbstractMultiFix {
	public CollectionCloningCleanUp() {
		this(Collections.emptyMap());
	}

	public CollectionCloningCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.COLLECTION_CLONING);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.COLLECTION_CLONING)) {
			return new String[] { MultiFixMessages.CollectionCloningCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.COLLECTION_CLONING)) {
			return "List<Integer> output = new ArrayList<>(collection);\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "List<Integer> output = new ArrayList<>();\n" //$NON-NLS-1$
				+ "output.addAll(collection);\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.COLLECTION_CLONING)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final Block node) {
				NewAndAddAllMethodVisitor newAndAddAllMethodVisitor= new NewAndAddAllMethodVisitor(node);
				node.accept(newAndAddAllMethodVisitor);
				return newAndAddAllMethodVisitor.result;
			}

			final class NewAndAddAllMethodVisitor extends ASTVisitor {
				private final Block startNode;
				private boolean result= true;

				public NewAndAddAllMethodVisitor(final Block startNode) {
					this.startNode= startNode;
				}

				@Override
				public boolean visit(final Block node) {
					return startNode == node;
				}

				@Override
				public boolean visit(final ExpressionStatement node) {
					MethodInvocation methodInvocation= ASTNodes.asExpression(node, MethodInvocation.class);

					if (result && ASTNodes.usesGivenSignature(methodInvocation, Collection.class.getCanonicalName(), "addAll", Collection.class.getCanonicalName())) { //$NON-NLS-1$
						Expression arg0= (Expression) methodInvocation.arguments().get(0);
						Statement previousStatement= ASTNodes.getPreviousSibling(node);
						Assignment as= ASTNodes.asExpression(previousStatement, Assignment.class);
						VariableDeclarationFragment fragment= ASTNodes.getUniqueFragment(previousStatement);

						if (ASTNodes.hasOperator(as, Assignment.Operator.ASSIGN)) {
							SimpleName lhs= ASTNodes.as(as.getLeftHandSide(), SimpleName.class);

							if (lhs != null && ASTNodes.isSameLocalVariable(lhs, methodInvocation.getExpression())) {
								return maybeReplaceInitializer(as.getRightHandSide(), arg0, node);
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
						rewriteOperations.add(new CollectionCloningOperation(nodeToRemove, cic, arg0));

						result= false;
						return false;
					}

					return true;
				}

				private boolean canReplaceInitializer(final ClassInstanceCreation cic, final Expression sourceCollection) {
					if (cic == null || cic.getAnonymousClassDeclaration() != null) {
						return false;
					}

					if (cic.arguments().isEmpty()) {
						return ASTNodes.hasType(cic,
								ConcurrentLinkedDeque.class.getCanonicalName(),
								ArrayList.class.getCanonicalName(),
								HashSet.class.getCanonicalName(),
								LinkedHashSet.class.getCanonicalName(),
								LinkedList.class.getCanonicalName(),
								TreeSet.class.getCanonicalName(),
								Vector.class.getCanonicalName(),
								ConcurrentLinkedQueue.class.getCanonicalName(),
								ConcurrentSkipListSet.class.getCanonicalName(),
								CopyOnWriteArrayList.class.getCanonicalName(),
								CopyOnWriteArraySet.class.getCanonicalName(),
								DelayQueue.class.getCanonicalName(),
								LinkedBlockingDeque.class.getCanonicalName(),
								LinkedBlockingQueue.class.getCanonicalName(),
								LinkedTransferQueue.class.getCanonicalName(),
								PriorityBlockingQueue.class.getCanonicalName(),
								ArrayDeque.class.getCanonicalName(),
								PriorityQueue.class.getCanonicalName());
					}

					return isValidCapacityParameter(sourceCollection, cic.arguments())
							&& ASTNodes.hasType(cic,
									ArrayList.class.getCanonicalName(),
									HashSet.class.getCanonicalName(),
									LinkedHashSet.class.getCanonicalName(),
									LinkedBlockingDeque.class.getCanonicalName(),
									LinkedBlockingQueue.class.getCanonicalName(),
									PriorityBlockingQueue.class.getCanonicalName(),
									ArrayDeque.class.getCanonicalName(),
									PriorityQueue.class.getCanonicalName(),
									Vector.class.getCanonicalName());
				}

				private boolean isValidCapacityParameter(final Expression sourceCollection, final List<Expression> args) {
					if (args.size() == 1 && ASTNodes.isPrimitive(args.get(0), int.class.getSimpleName())) {
						Long constant= ASTNodes.getIntegerLiteral(args.get(0));

						if (constant != null) {
							return Long.valueOf(0).equals(constant);
						}

						MethodInvocation methodInvocation= ASTNodes.as(args.get(0), MethodInvocation.class);

						return ASTNodes.usesGivenSignature(methodInvocation, Collection.class.getCanonicalName(), "size") //$NON-NLS-1$
								&& ASTNodes.match(methodInvocation.getExpression(), sourceCollection);
					}

					return false;
				}
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.CollectionCloningCleanUp_description, unit,
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

	private static class CollectionCloningOperation extends CompilationUnitRewriteOperation {
		private final ExpressionStatement nodeToRemove;
		private final ClassInstanceCreation classInstanceCreation;
		private final Expression arg0;

		public CollectionCloningOperation(final ExpressionStatement nodeToRemove, final ClassInstanceCreation classInstanceCreation, final Expression arg0) {
			this.nodeToRemove= nodeToRemove;
			this.classInstanceCreation= classInstanceCreation;
			this.arg0= arg0;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			ListRewrite listRewrite= rewrite.getListRewrite(classInstanceCreation, ClassInstanceCreation.ARGUMENTS_PROPERTY);
			TextEditGroup group= createTextEditGroup(MultiFixMessages.CollectionCloningCleanUp_description, cuRewrite);

			if (classInstanceCreation.arguments() == null || classInstanceCreation.arguments().isEmpty()) {
				listRewrite.insertFirst(ASTNodes.createMoveTarget(rewrite, arg0), group);
			} else {
				listRewrite.replace((ASTNode) classInstanceCreation.arguments().get(0), ASTNodes.createMoveTarget(rewrite, arg0), group);
			}

			rewrite.remove(nodeToRemove, group);
		}
	}
}
