/*******************************************************************************
 * Copyright (c) 2018 itemis AG (http://www.itemis.eu) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Karsten Thoms (itemis) - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.LinkedProposalModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that removes redundant modifiers:
 * <ul>
 * <li>Within an interface modifiers <code>public</code>, <code>static</code> and <code>final</code>
 * are redundant for field declarations.</li>
 * <li>Within an interface modifier <code>public</code> and
 * <code>abstract</code are redundant for method declarations.</li>
 * <li>Within a final class the <code>final</code> modifier is redundant for method
 * declarations.</li>
 * <li>For nested interfaces the <code>static</code> modifier is redundant.</li>
 * </ul>
 */
public class RedundantModifiersCleanUp extends AbstractMultiFix {
	public RedundantModifiersCleanUp() {
		this(Collections.emptyMap());
	}

	public RedundantModifiersCleanUp(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS);
		Map<String, String> requiredOptions= null;
		return new CleanUpRequirements(requireAST, false, false, requiredOptions);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS)) {
			return new String[] { MultiFixMessages.RedundantModifiersCleanup_description };
		}
		return new String[0];
	}

	@SuppressWarnings("nls")
	@Override
	public String getPreview() {
		StringBuffer buf= new StringBuffer();
		buf.append("\n");
		buf.append("public interface IFoo {\n");
		if (isEnabled(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS)) {
			buf.append("  int MAGIC_NUMBER = 646;\n");
			buf.append("  int foo ();\n");
			buf.append("  int bar (int bazz);\n");
		} else {
			buf.append("  public static final int MAGIC_NUMBER = 646;\n");
			buf.append("  public abstract int foo ();\n");
			buf.append("  public int bar (int bazz);\n");
		}
		buf.append("}\n");
		buf.append("\n");
		buf.append("public final class Sealed {\n");
		if (isEnabled(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS)) {
			buf.append("  public void foo () {};\n");
			buf.append("  \n");
			buf.append("  interface INested {\n");
			buf.append("  }\n");

		} else {
			buf.append("  public final void foo () {};\n");
			buf.append("  \n");
			buf.append("  static interface INested {\n");
			buf.append("  }\n");
		}
		buf.append("}\n");


		return buf.toString();
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.REMOVE_REDUNDANT_MODIFIERS)) {
			return null;
		}
		final List<CompilationUnitRewriteOperation> rewriteOperations= new ArrayList<>();

		unit.accept(new ASTVisitor() {
			@Override
			public boolean visit(FieldDeclaration node) {
				TypeDeclaration typeDecl= ASTNodes.getParent(node, TypeDeclaration.class);
				if (typeDecl != null && typeDecl.isInterface()) {
					final int excluded= Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
					if ((node.getModifiers() & excluded) > 0) {
						rewriteOperations.add(new RemoveModifiersOperation(node, excluded));
					}
				}
				return true;
			}

			@Override
			public boolean visit(MethodDeclaration node) {
				TypeDeclaration typeDecl= ASTNodes.getParent(node, TypeDeclaration.class);
				if (typeDecl != null && typeDecl.isInterface()) {
					rewriteOperations.add(new RemoveModifiersOperation(node, Modifier.ABSTRACT));
					if (!AnonymousClassDeclaration.class.isInstance(node.getParent()) && !EnumDeclaration.class.isInstance(node.getParent())) {
						rewriteOperations.add(new RemoveModifiersOperation(node, Modifier.PUBLIC));
					}
				} else if (typeDecl != null && Modifier.isFinal(typeDecl.getModifiers()) && Modifier.isFinal(node.getModifiers())) {
					rewriteOperations.add(new RemoveModifiersOperation(node, Modifier.FINAL));
				}
				return true;
			}

			@Override
			public boolean visit(TypeDeclaration node) {
				TypeDeclaration typeDecl= ASTNodes.getParent(node, TypeDeclaration.class);
				if (typeDecl != null && node.isInterface() && Modifier.isStatic(node.getModifiers())) {
					rewriteOperations.add(new RemoveModifiersOperation(node, Modifier.STATIC));
				}
				return true;
			}
			
			@Override
			public boolean visit(EnumDeclaration node) {
				TypeDeclaration typeDecl= ASTNodes.getParent(node, TypeDeclaration.class);
				if (typeDecl != null && Modifier.isStatic(node.getModifiers())) {
					rewriteOperations.add(new RemoveModifiersOperation(node, Modifier.STATIC));
				}
				return true;
			}

		});
		if (rewriteOperations.isEmpty()) {
			return null;
		}
		return new CompilationUnitRewriteOperationsFix(MultiFixMessages.RedundantModifiersCleanup_description, unit,
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

	private static class RemoveModifiersOperation extends CompilationUnitRewriteOperation {
		private final ASTNode node;

		private final int excludedModifiers;

		public RemoveModifiersOperation(ASTNode node, int excludedModifiers) {
			this.node= node;
			this.excludedModifiers= excludedModifiers;
		}

		@Override
		public void rewriteAST(CompilationUnitRewrite cuRewrite, LinkedProposalModel linkedModel) throws CoreException {
			ModifierRewrite rewrite= ModifierRewrite.create(cuRewrite.getASTRewrite(), node);
			rewrite.setModifiers(Modifier.NONE, excludedModifiers, null);
		}
	}
}
