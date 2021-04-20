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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ForLoops;
import org.eclipse.jdt.internal.corext.dom.VarDefinitionsUsesVisitor;
import org.eclipse.jdt.internal.corext.dom.ForLoops.ForLoopContent;
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
 * A fix that replaces a loop on elements by Collection.addAll(), Collection.addAll(Arrays.asList()) or Collections.addAll().
 * If the source is an array, the list is raw and the JVM is Java 1.5 or higher, we use Arrays.asList() to handle the erasure type.
 * It doesn't decrease the performance.
 */
public class AddAllCleanUp extends AbstractMultiFix implements ICleanUpFix {
	public AddAllCleanUp() {
		this(Collections.emptyMap());
	}

	public AddAllCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_ADD_ALL);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_ADD_ALL)) {
			return new String[] { MultiFixMessages.AddAllCleanup_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_ADD_ALL)) {
			return "outputList.addAll(inputList);\n\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "for (int i = 0; i < inputList.size(); i++) {\n" //$NON-NLS-1$
				+ "    outputList.add(inputList.get(i));\n" //$NON-NLS-1$
				+ "}\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.CONTROL_STATEMENTS_USE_ADD_ALL)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(final EnhancedForStatement node) {
				MethodInvocation methodInvocation= ASTNodes.asExpression(node.getBody(), MethodInvocation.class);
				IVariableBinding foreachVariable= node.getParameter().resolveBinding();

				// We should remove all the loop variable occurrences
				// As we replace only one, there should be no more than one occurrence
				if (methodInvocation != null
						&& methodInvocation.arguments().size() == 1
						&& getVariableUseCount(foreachVariable, node.getBody()) == 1) {
					Expression iterable= node.getExpression();
					Expression argument= (Expression) methodInvocation.arguments().get(0);

					if (ASTNodes.instanceOf(iterable, Collection.class.getCanonicalName())) {
						if (ASTNodes.isSameLocalVariable(node.getParameter(), argument)) {
							return maybeReplaceForCollection(node, methodInvocation, iterable);
						}
					} else if (ASTNodes.isArray(iterable) && ASTNodes.isSameLocalVariable(foreachVariable, argument)) {
						return maybeReplaceForArray(node, iterable, methodInvocation);
					}
				}

				return true;
			}

			@Override
			public boolean visit(final ForStatement node) {
				ForLoopContent loopContent= ForLoops.iterateOverContainer(node);
				MethodInvocation methodInvocation= ASTNodes.asExpression(node.getBody(), MethodInvocation.class);

				if (loopContent != null
						&& loopContent.getLoopVariable() != null
						&& methodInvocation != null
						&& methodInvocation.arguments().size() == 1) {
					Name loopVariable= loopContent.getLoopVariable();
					IVariableBinding loopVariableName= (IVariableBinding) loopVariable.resolveBinding();

					// We should remove all the loop variable occurrences
					// As we replace only one, there should be no more than one occurrence
					if (getVariableUseCount(loopVariableName, node.getBody()) == 1
							&& (loopContent.isLoopingForward()
									|| (methodInvocation.resolveMethodBinding() != null
									&& ASTNodes.hasType(methodInvocation.resolveMethodBinding().getDeclaringClass(), Set.class.getCanonicalName())))) {
						Expression addArg0= (Expression) methodInvocation.arguments().get(0);

						switch (loopContent.getContainerType()) {
						case COLLECTION:
							MethodInvocation getMI= ASTNodes.as(addArg0, MethodInvocation.class);

							if (getMI != null && getMI.arguments().size() == 1 && isSameVariable(loopContent, getMI)) {
								return maybeReplaceForCollection(node, methodInvocation, getMI.getExpression());
							}
							break;

						case ARRAY:
							ArrayAccess arrayAccess= ASTNodes.as(addArg0, ArrayAccess.class);

							if (isSameVariable(loopContent, arrayAccess)) {
								return maybeReplaceForArray(node, loopContent.getContainerVariable(), methodInvocation);
							}
							break;

						default:
							break;
						}
					}
				}

				return true;
			}

			private boolean maybeReplaceForArray(final Statement node, final Expression iterable, final MethodInvocation addMethod) {
				IMethodBinding methodBinding= addMethod.resolveMethodBinding();

				if (methodBinding != null
						&& addMethod.getExpression() != null
						&& !ASTNodes.is(addMethod.getExpression(), ThisExpression.class)
						&& ASTNodes.usesGivenSignature(addMethod, Collection.class.getCanonicalName(), "add", Object.class.getCanonicalName()) //$NON-NLS-1$
						&& areTypeCompatible(methodBinding.getDeclaringClass(), iterable.resolveTypeBinding())) {
					rewriteOperations.add(new AddOrRemoveAllForArrayOperation(node, addMethod.getExpression(), iterable));
					return false;
				}

				return true;
			}

			private int getVariableUseCount(final IVariableBinding variableBinding, final Statement toVisit) {
				if (variableBinding != null) {
					VarDefinitionsUsesVisitor variableUseVisitor= new VarDefinitionsUsesVisitor(variableBinding,
					toVisit, true);
					return variableUseVisitor.getReads().size();
				}

				return 0;
			}

			private boolean isSameVariable(final ForLoopContent loopContent, final ArrayAccess arrayAccess) {
				return arrayAccess != null && ASTNodes.isSameVariable(arrayAccess.getArray(), loopContent.getContainerVariable())
						&& ASTNodes.isSameLocalVariable(arrayAccess.getIndex(), loopContent.getLoopVariable());
			}

			private boolean areTypeCompatible(final ITypeBinding colTypeBinding, final ITypeBinding arrayTypeBinding) {
				if (arrayTypeBinding != null && colTypeBinding != null) {
					ITypeBinding jucTypeBinding= ASTNodes.findImplementedType(colTypeBinding, Collection.class.getCanonicalName());

					if (jucTypeBinding.isRawType()) {
						return true;
					}

					ITypeBinding componentType= arrayTypeBinding.getComponentType();
					ITypeBinding colTypeArgument= jucTypeBinding.getTypeArguments()[0];
					return componentType.isSubTypeCompatible(colTypeArgument);
				}

				return false;
			}

			private boolean maybeReplaceForCollection(final Statement node, final MethodInvocation addOrRemoveMethod,
					final Expression data) {
				if (addOrRemoveMethod.getExpression() == null
						|| ASTNodes.is(addOrRemoveMethod.getExpression(), ThisExpression.class)) {
					return true;
				}

				if (ASTNodes.usesGivenSignature(addOrRemoveMethod, Collection.class.getCanonicalName(), "add", Object.class.getCanonicalName())) { //$NON-NLS-1$
					rewriteOperations.add(new AddAllForCollectionOperation(node, addOrRemoveMethod.getExpression(), data));
					return false;
				}

				return true;
			}

			private boolean isSameVariable(final ForLoopContent loopContent, final MethodInvocation getMI) {
				Expression methodExpression= getMI.getExpression();
				return (methodExpression instanceof Name || methodExpression instanceof FieldAccess || methodExpression instanceof SuperFieldAccess)
						&& ASTNodes.usesGivenSignature(getMI, List.class.getCanonicalName(), "get", int.class.getSimpleName()) //$NON-NLS-1$
						&& ASTNodes.isSameLocalVariable((Expression) getMI.arguments().get(0), loopContent.getLoopVariable())
						&& ASTNodes.isSameVariable(loopContent.getContainerVariable(), methodExpression);
			}
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.AddAllCleanup_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[0]));
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		return null;
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, final IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class AddOrRemoveAllForArrayOperation extends CompilationUnitRewriteOperation {
		private final Statement toReplace;
		private final Expression affectedCollection;
		private final Expression addedData;

		public AddOrRemoveAllForArrayOperation(final Statement toReplace, final Expression affectedCollection, final Expression addedData) {
			this.toReplace= toReplace;
			this.affectedCollection= affectedCollection;
			this.addedData= addedData;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.AddAllCleanup_description, cuRewrite);
			ImportRewrite importRewrite= cuRewrite.getImportRewrite();

			if (JavaModelUtil.is50OrHigher(((CompilationUnit) toReplace.getRoot()).getJavaElement().getJavaProject())
					&& affectedCollection.resolveTypeBinding() != null
					&& affectedCollection.resolveTypeBinding().isRawType()) {
				String arraysNameText= importRewrite.addImport(Arrays.class.getCanonicalName());

				MethodInvocation asListMethod= ast.newMethodInvocation();
				asListMethod.setExpression(ASTNodeFactory.newName(ast, arraysNameText));
				asListMethod.setName(ast.newSimpleName("asList")); //$NON-NLS-1$
				asListMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(addedData)));

				MethodInvocation addAllMethod= ast.newMethodInvocation();
				addAllMethod.setExpression(ASTNodes.createMoveTarget(rewrite, affectedCollection));
				addAllMethod.setName(ast.newSimpleName("addAll")); //$NON-NLS-1$
				addAllMethod.arguments().add(asListMethod);

				ASTNodes.replaceButKeepComment(rewrite, toReplace, ast.newExpressionStatement(addAllMethod), group);
			} else {
				String collectionsNameText= importRewrite.addImport(Collections.class.getCanonicalName());

				MethodInvocation newMethod= ast.newMethodInvocation();
				newMethod.setExpression(ASTNodeFactory.newName(ast, collectionsNameText));
				newMethod.setName(ast.newSimpleName("addAll")); //$NON-NLS-1$
				newMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(affectedCollection)));
				newMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(addedData)));

				ExpressionStatement expressionStatement= ast.newExpressionStatement(newMethod);

				ASTNodes.replaceButKeepComment(rewrite, toReplace, expressionStatement, group);
			}
		}
	}

	private static class AddAllForCollectionOperation extends CompilationUnitRewriteOperation {
		private final Statement toReplace;
		private final Expression affectedCollection;
		private final Expression addedData;

		public AddAllForCollectionOperation(final Statement toReplace, final Expression affectedCollection, final Expression addedData) {
			this.toReplace= toReplace;
			this.affectedCollection= affectedCollection;
			this.addedData= addedData;
		}

		@Override
		public void rewriteAST(final CompilationUnitRewrite cuRewrite, final LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.AddAllCleanup_description, cuRewrite);

			MethodInvocation newMethod= ast.newMethodInvocation();
			newMethod.setExpression(ASTNodes.createMoveTarget(rewrite, affectedCollection));
			newMethod.setName(ast.newSimpleName("addAll")); //$NON-NLS-1$
			newMethod.arguments().add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(addedData)));

			ASTNodes.replaceButKeepComment(rewrite, toReplace, ast.newExpressionStatement(newMethod), group);
		}
	}
}
