/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperationWithSourceRange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.structure.ImportRemover;

class ReplaceNullableWithNonNullOperation extends CompilationUnitRewriteOperationWithSourceRange {

	private final Annotation fAnnotation;

	public ReplaceNullableWithNonNullOperation(Annotation annotation) {
		this.fAnnotation= annotation;
	}

	@Override
	public void rewriteASTInternal(CompilationUnitRewrite cuRewrite, LinkedProposalModelCore linkedModel) throws CoreException {
		AST ast= cuRewrite.getAST();
		ASTRewrite rewrite= cuRewrite.getASTRewrite();
		ImportRewrite importRewrite= cuRewrite.getImportRewrite();
		ImportRemover importRemover= cuRewrite.getImportRemover();

		Annotation newAnnotation= ast.newMarkerAnnotation();
		newAnnotation.setTypeName(ast.newSimpleName(NullAnnotationsFixCore.getNonNullAnnotationName(cuRewrite.getCu(), true)));
		importRemover.registerRemovedNode(fAnnotation);
		importRewrite.addImport(NullAnnotationsFixCore.getNonNullAnnotationName(cuRewrite.getCu(), false));
		rewrite.replace(fAnnotation, newAnnotation, null);
	}

}