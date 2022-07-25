/*******************************************************************************
 * Copyright (c) 2021 Carsten Hammer.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix.helper;

import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.SimplifyPlatformStatusFixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

/**
 * @param <T> Type found in Visitor
 */
public abstract class AbstractSimplifyPlatformStatus<T extends ASTNode> {
	String methodname;
	String istatus;

	public AbstractSimplifyPlatformStatus(String methodname, String istatus) {
		this.methodname= methodname;
		this.istatus= istatus;
	}

	/**
	 * Adds an import to the class. This method should be used for every class
	 * reference added to the generated code.
	 *
	 * @param typeName  a fully qualified name of a type
	 * @param cuRewrite CompilationUnitRewrite
	 * @param ast       AST
	 * @return simple name of a class if the import was added and fully qualified
	 *         name if there was a conflict
	 */
	protected Name addImport(String typeName, final CompilationUnitRewrite cuRewrite, AST ast) {
		String importedName= cuRewrite.getImportRewrite().addImport(typeName);
		return ast.newName(importedName);
	}

	public abstract String getPreview(boolean afterRefactoring);

	public void find(SimplifyPlatformStatusFixCore fixcore, CompilationUnit compilationUnit,
			Set<CompilationUnitRewriteOperation> operations, Set<ASTNode> nodesprocessed) throws CoreException {
		try {
			compilationUnit.accept(new ASTVisitor() {
				@Override
				public boolean visit(final ClassInstanceCreation visited) {
					if (nodesprocessed.contains(visited) || ((visited.arguments().size() != 3)
							&& (visited.arguments().size() != 4) && (visited.arguments().size() != 5))) {
						return false;
					}

					ITypeBinding binding= visited.resolveTypeBinding();
					if ((binding != null) && (Status.class.getSimpleName().equals(binding.getName()))) {
						List<Expression> arguments= visited.arguments();
						if (istatus.equals(arguments.get(0).toString())) {
							operations.add(fixcore.rewrite(visited));
							nodesprocessed.add(visited);
							return false;
						}
					}

					return true;
				}
			});
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, "sandbox_platform_helper", "Problem in find", e)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	public void rewrite(SimplifyPlatformStatusFixCore upp, final ClassInstanceCreation visited,
			final CompilationUnitRewrite cuRewrite, TextEditGroup group) {
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		AST ast= cuRewrite.getRoot().getAST();
		/**
		 * Add call to Status.warning(),Status.error() and Status.info()
		 */
		MethodInvocation staticCall= ast.newMethodInvocation();
		staticCall.setExpression(ASTNodeFactory.newName(ast, Status.class.getSimpleName()));
		staticCall.setName(ast.newSimpleName(methodname));
		List<ASTNode> arguments= visited.arguments();
		List<ASTNode> staticCallArguments= staticCall.arguments();
		int positionmessage= arguments.size() == 5 ? 3 : 2;
		staticCallArguments.add(ASTNodes.createMoveTarget(rewrite,
				ASTNodes.getUnparenthesedExpression(arguments.get(positionmessage))));
		switch (arguments.size()) {
		case 4:
			ASTNode node= arguments.get(3);
			if (!node.toString().equals("null")) { //$NON-NLS-1$
				staticCallArguments.add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(node)));
			}
			break;
		case 5:
			ASTNode node2= arguments.get(4);
			if (!node2.toString().equals("null")) { //$NON-NLS-1$
				staticCallArguments.add(ASTNodes.createMoveTarget(rewrite, ASTNodes.getUnparenthesedExpression(node2)));
			}
			break;
		case 3:
		default:
			break;
		}
		ASTNodes.replaceButKeepComment(rewrite, visited, staticCall, group);
	}
}
