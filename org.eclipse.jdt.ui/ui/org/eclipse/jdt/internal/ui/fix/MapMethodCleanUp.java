/*******************************************************************************
 * Copyright (c) 2019 Fabrice TIERCELIN and others.
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

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
 * A fix that uses Map method instead of using the keyset or values:
 * <ul>
 * <li>Use containsKey() or containsValue() rather than contains(),</li>
 * <li>Uses remove(), clear(), size() and isEmpty() on map directly.</li>
 * </ul>
 */
public class MapMethodCleanUp extends AbstractMultiFix {
	public MapMethodCleanUp() {
		this(Collections.emptyMap());
	}

	public MapMethodCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.USE_DIRECTLY_MAP_METHOD);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.USE_DIRECTLY_MAP_METHOD)) {
			return new String[] { MultiFixMessages.UseDirectlyMapMethodCleanup_description };
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		if (isEnabled(CleanUpConstants.USE_DIRECTLY_MAP_METHOD)) {
			bld.append("int x = map.size();\n"); //$NON-NLS-1$
			bld.append("if (map.containsKey(\"hello\")) {\n"); //$NON-NLS-1$
			bld.append("    map.remove(\"hello\");\n"); //$NON-NLS-1$
			bld.append("}\n"); //$NON-NLS-1$
			bld.append("map.clear();\n"); //$NON-NLS-1$
			bld.append("map.clear();\n"); //$NON-NLS-1$
			bld.append("if (map.isEmpty()) {\n"); //$NON-NLS-1$
		} else {
			bld.append("int x = map.keySet().size();\n"); //$NON-NLS-1$
			bld.append("if (map.keySet().contains(\"hello\")) {\n"); //$NON-NLS-1$
			bld.append("    map.keySet().remove(\"hello\");\n"); //$NON-NLS-1$
			bld.append("}\n"); //$NON-NLS-1$
			bld.append("map.keySet().clear();\n"); //$NON-NLS-1$
			bld.append("map.values().clear();\n"); //$NON-NLS-1$
			bld.append("if (map.keySet().isEmpty()) {\n"); //$NON-NLS-1$
		}
		bld.append("    x++;\n"); //$NON-NLS-1$
		bld.append("}\n"); //$NON-NLS-1$

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.USE_DIRECTLY_MAP_METHOD)) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
		    @Override
		    public boolean visit(MethodInvocation mi) {
		        MethodInvocation miExpression= ASTNodes.as(mi.getExpression(), MethodInvocation.class);

				if (miExpression != null && miExpression.getExpression() != null && ASTNodes.as(miExpression.getExpression(), ThisExpression.class) == null) {
					String subAggregateClass;

					if (ASTNodes.usesGivenSignature(miExpression, Map.class.getCanonicalName(), "keySet")) { //$NON-NLS-1$
						subAggregateClass= Set.class.getCanonicalName();

						if (ASTNodes.usesGivenSignature(mi, subAggregateClass, "remove", Object.class.getCanonicalName()) //$NON-NLS-1$
								// If parent is not an expression statement, the MethodInvocation must return a
								// boolean.
								// In that case, we cannot replace because `Map.removeKey(key) != null`
								// is not strictly equivalent to `Map.keySet().remove(key)`
								&& mi.getParent().getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
							rewriteOperations.add(new MapMethodOperation(miExpression, mi, "remove")); //$NON-NLS-1$
							return false;
						}
					} else if (ASTNodes.usesGivenSignature(miExpression, Map.class.getCanonicalName(), "values")) { //$NON-NLS-1$
						subAggregateClass= Collection.class.getCanonicalName();
					} else {
						return true;
					}

					if (ASTNodes.usesGivenSignature(mi, subAggregateClass, "clear")) { //$NON-NLS-1$
						rewriteOperations.add(new MapMethodOperation(miExpression, mi, "clear")); //$NON-NLS-1$
						return false;
					}

					if (ASTNodes.usesGivenSignature(mi, subAggregateClass, "size")) { //$NON-NLS-1$
						rewriteOperations.add(new MapMethodOperation(miExpression, mi, "size")); //$NON-NLS-1$
						return false;
					}

					if (ASTNodes.usesGivenSignature(mi, subAggregateClass, "isEmpty")) { //$NON-NLS-1$
						rewriteOperations.add(new MapMethodOperation(miExpression, mi, "isEmpty")); //$NON-NLS-1$
						return false;
					}

					if (ASTNodes.usesGivenSignature(mi, subAggregateClass, "contains", Object.class.getCanonicalName())) { //$NON-NLS-1$
						String methodName= ASTNodes.usesGivenSignature(miExpression, Map.class.getCanonicalName(), "keySet") ? "containsKey" : "containsValue"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						rewriteOperations.add(new MapMethodOperation(miExpression, mi, methodName));
						return false;
					}
		        }

		        return true;
		    }
		});

		if (rewriteOperations.isEmpty()) {
			return null;
		}

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.UseDirectlyMapMethodCleanup_description, unit,
				rewriteOperations.toArray(new CompilationUnitRewriteOperation[rewriteOperations.size()]));
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		return null;
	}

	private static class MapMethodOperation extends CompilationUnitRewriteOperation {
		private final MethodInvocation subAggregateMi;
		private final MethodInvocation globalMi;
		private final String methodName;

		public MapMethodOperation(MethodInvocation mapKeySetMi, MethodInvocation actualMi,
				String methodName) {
			this.subAggregateMi= mapKeySetMi;
			this.globalMi= actualMi;
			this.methodName= methodName;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			AST ast= cuRewrite.getRoot().getAST();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.UseDirectlyMapMethodCleanup_description, cuRewrite);

			MethodInvocation newMethodInvocation= ast.newMethodInvocation();
			newMethodInvocation.setName(ast.newSimpleName(methodName));

			if (subAggregateMi.getExpression() != null) {
				newMethodInvocation.setExpression((Expression) rewrite.createMoveTarget(subAggregateMi.getExpression()));
			}

			for (Object expression : globalMi.arguments()) {
				newMethodInvocation.arguments().add(rewrite.createMoveTarget((Expression) expression));
			}

			ASTNodes.replaceButKeepComment(rewrite, this.globalMi, newMethodInvocation, group);
		}
	}
}
