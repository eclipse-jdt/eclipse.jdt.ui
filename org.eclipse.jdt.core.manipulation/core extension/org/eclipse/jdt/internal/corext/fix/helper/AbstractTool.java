/*******************************************************************************
 * Copyright (c) 2021, 2022 Carsten Hammer.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix.helper;

import java.util.Collection;
import java.util.Set;

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Name;

import org.eclipse.jdt.internal.corext.dom.AbortSearchException;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.UseIteratorToForLoopFixCore;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;

/**
 * @param <T> Type found in Visitor
 */
public abstract class AbstractTool<T> {

	protected static boolean isOfType(ITypeBinding typeBinding, String typename) {
		if (typeBinding == null) {
			throw new AbortSearchException();
		}
		if (typeBinding.isArray()) {
			typeBinding= typeBinding.getElementType();
		}
		return typeBinding.getQualifiedName().equals(typename);
	}

	public abstract void find(UseIteratorToForLoopFixCore fixcore, CompilationUnit compilationUnit, Set<CompilationUnitRewriteOperation> operations,
			Set<ASTNode> nodesprocessed, boolean createForIfVarNotUsed);

	public abstract void rewrite(UseIteratorToForLoopFixCore useExplicitEncodingFixCore, T holder, CompilationUnitRewrite cuRewrite,
			TextEditGroup group);

	/**
	 * Adds an import to the class. This method should be used for every class reference added to
	 * the generated code.
	 *
	 * @param typeName a fully qualified name of a type
	 * @param cuRewrite CompilationUnitRewrite
	 * @param ast AST
	 * @return simple name of a class if the import was added and fully qualified name if there was
	 *         a conflict
	 */
	protected Name addImport(String typeName, final CompilationUnitRewrite cuRewrite, AST ast) {
		String importedName= cuRewrite.getImportRewrite().addImport(typeName);
		return ast.newName(importedName);
	}


	public abstract String getPreview(boolean afterRefactoring);

	public static Collection<String> getUsedVariableNames(ASTNode node) {
		CompilationUnit root= (CompilationUnit) node.getRoot();
		Collection<String> res= (new ScopeAnalyzer(root)).getUsedVariableNames(node.getStartPosition(), node.getLength());
		return res;
	}
}
