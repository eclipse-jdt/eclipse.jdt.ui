/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class DoWhileRatherThanWhileFix extends CompilationUnitRewriteOperationsFix {

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit) {
		List<CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation> operations= new ArrayList<>();
		DoWhileRatherThanWhileFixCore.DoWhileRatherThanWhileFinder finder= new DoWhileRatherThanWhileFixCore.DoWhileRatherThanWhileFinder(operations);
		compilationUnit.accept(finder);
		if (operations.isEmpty())
			return null;

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[operations.size()]);
		return new DoWhileRatherThanWhileFix(FixMessages.DoWhileRatherThanWhileFix_description, compilationUnit, ops);
	}

	public static DoWhileRatherThanWhileFix createDoWhileFix(WhileStatement switchStatement) {
		CompilationUnit root= (CompilationUnit) switchStatement.getRoot();
		List<CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation> operations= new ArrayList<>();
		DoWhileRatherThanWhileFixCore.DoWhileRatherThanWhileFinder finder= new DoWhileRatherThanWhileFixCore.DoWhileRatherThanWhileFinder(operations);
		switchStatement.accept(finder);
		if (operations.isEmpty())
			return null;
		return new DoWhileRatherThanWhileFix(FixMessages.DoWhileRatherThanWhileFix_description, root, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { operations.get(0) });
	}

	protected DoWhileRatherThanWhileFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
