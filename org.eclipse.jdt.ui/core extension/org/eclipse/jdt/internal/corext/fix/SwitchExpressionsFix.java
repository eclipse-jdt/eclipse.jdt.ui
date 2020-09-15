/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
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
import org.eclipse.jdt.core.dom.SwitchStatement;

import org.eclipse.jdt.internal.corext.fix.SwitchExpressionsFixCore.SwitchExpressionsFixOperation;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class SwitchExpressionsFix extends CompilationUnitRewriteOperationsFix {

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit) {
		if (!JavaModelUtil.is14OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		List<SwitchExpressionsFixOperation> operations= new ArrayList<>();
		SwitchExpressionsFixCore.SwitchStatementsFinder finder= new SwitchExpressionsFixCore.SwitchStatementsFinder(operations);
		compilationUnit.accept(finder);
		if (operations.isEmpty())
			return null;

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[operations.size()]);
		return new SwitchExpressionsFix(FixMessages.SwitchExpressionsFix_convert_to_switch_expression, compilationUnit, ops);
	}

	public static SwitchExpressionsFix createConvertToSwitchExpressionFix(SwitchStatement switchStatement) {
		CompilationUnit root= (CompilationUnit) switchStatement.getRoot();
		if (!JavaModelUtil.is14OrHigher(root.getJavaElement().getJavaProject()))
			return null;

		List<SwitchExpressionsFixOperation> operations= new ArrayList<>();
		SwitchExpressionsFixCore.SwitchStatementsFinder finder= new SwitchExpressionsFixCore.SwitchStatementsFinder(operations);
		switchStatement.accept(finder);
		if (operations.isEmpty())
			return null;
		return new SwitchExpressionsFix(FixMessages.SwitchExpressionsFix_convert_to_switch_expression, root, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] { operations.get(0) });
	}

	protected SwitchExpressionsFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}

}
