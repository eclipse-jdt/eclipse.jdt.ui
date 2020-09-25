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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

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
 * A fix that uses Unboxing:
 * <ul>
 * <li>As of Java 5, intValue() call can be replaced the Integer wrapper expression directly.
 * And it is the case for all the primitive wrappers. The method call is automatically added at compile time.</li>
 * </ul>
 */
public class UnboxingCleanUp extends AbstractMultiFix {
	private static final String DOUBLE_VALUE= "doubleValue"; //$NON-NLS-1$
	private static final String FLOAT_VALUE= "floatValue"; //$NON-NLS-1$
	private static final String LONG_VALUE= "longValue"; //$NON-NLS-1$
	private static final String INT_VALUE= "intValue"; //$NON-NLS-1$
	private static final String SHORT_VALUE= "shortValue"; //$NON-NLS-1$
	private static final String CHAR_VALUE= "charValue"; //$NON-NLS-1$
	private static final String BYTE_VALUE= "byteValue"; //$NON-NLS-1$
	private static final String BOOLEAN_VALUE= "booleanValue"; //$NON-NLS-1$

	public UnboxingCleanUp() {
		this(Collections.emptyMap());
	}

	public UnboxingCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.USE_UNBOXING);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.USE_UNBOXING)) {
			return new String[] { MultiFixMessages.UnboxingCleanup_description };
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		bld.append("Integer integerObject = Integer.MAX_VALUE;\n"); //$NON-NLS-1$
		bld.append("Character cObject = Character.MAX_VALUE;\n"); //$NON-NLS-1$
		bld.append("\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.USE_UNBOXING)) {
			bld.append("int i = integerObject;\n"); //$NON-NLS-1$
			bld.append("char c = cObject;\n"); //$NON-NLS-1$
		} else {
			bld.append("int i = integerObject.intValue();\n"); //$NON-NLS-1$
			bld.append("char c = cObject.charValue();\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.USE_UNBOXING) || !JavaModelUtil.is50OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				if (node.getExpression() != null) {
					ITypeBinding nodeBinding= node.getExpression().resolveTypeBinding();
					if (nodeBinding != null	&& nodeBinding.isClass()
							&& (ASTNodes.usesGivenSignature(node, Boolean.class.getCanonicalName(), BOOLEAN_VALUE)
									|| ASTNodes.usesGivenSignature(node, Byte.class.getCanonicalName(), BYTE_VALUE)
									|| ASTNodes.usesGivenSignature(node, Character.class.getCanonicalName(), CHAR_VALUE)
									|| ASTNodes.usesGivenSignature(node, Short.class.getCanonicalName(), SHORT_VALUE)
									|| ASTNodes.usesGivenSignature(node, Integer.class.getCanonicalName(), INT_VALUE)
									|| ASTNodes.usesGivenSignature(node, Long.class.getCanonicalName(), LONG_VALUE)
									|| ASTNodes.usesGivenSignature(node, Float.class.getCanonicalName(), FLOAT_VALUE)
									|| ASTNodes.usesGivenSignature(node, Double.class.getCanonicalName(), DOUBLE_VALUE))) {
						final ITypeBinding actualResultType= ASTNodes.getTargetType(node);

						if (actualResultType != null && actualResultType.isAssignmentCompatible(node.resolveTypeBinding())) {
							rewriteOperations.add(new UnboxingOperation(node));
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

		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.UnboxingCleanup_description, unit,
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

	private static class UnboxingOperation extends CompilationUnitRewriteOperation {
		private final MethodInvocation node;

		public UnboxingOperation(MethodInvocation node) {
			this.node= node;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException {
			ASTRewrite rewrite= cuRewrite.getASTRewrite();
			TextEditGroup group= createTextEditGroup(MultiFixMessages.UnboxingCleanup_description, cuRewrite);
			Expression copyOfWrapper= (Expression) rewrite.createCopyTarget(node.getExpression());
			ASTNodes.replaceButKeepComment(rewrite, node, copyOfWrapper, group);
		}
	}
}