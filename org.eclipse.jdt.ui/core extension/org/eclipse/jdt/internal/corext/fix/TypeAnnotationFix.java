/*******************************************************************************
 * Copyright (c) 2016 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.fix.TypeAnnotationRewriteOperations.MoveTypeAnnotationRewriteOperation;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class TypeAnnotationFix extends CompilationUnitRewriteOperationsFix {
	public TypeAnnotationFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperation operation) {
		super(name, compilationUnit, operation);
	}

	public static TypeAnnotationFix createMoveAnnotationsToTypeAnnotationsFix(CompilationUnit compilationUnit, IProblemLocation problem) {
		MoveTypeAnnotationRewriteOperation operation= new MoveTypeAnnotationRewriteOperation(compilationUnit, problem);
		return new TypeAnnotationFix(operation.isMove() ? FixMessages.TypeAnnotationFix_move : FixMessages.TypeAnnotationFix_remove, compilationUnit, operation);
	}

}
