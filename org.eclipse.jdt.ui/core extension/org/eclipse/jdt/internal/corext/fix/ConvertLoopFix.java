/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
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
 *     Red Hat Inc. - modified to use ConvertLoopFixCore
 *******************************************************************************/
/**
 *
 **/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class ConvertLoopFix extends CompilationUnitRewriteOperationsFix {

	public static ICleanUpFix createCleanUp(CompilationUnit compilationUnit, boolean convertForLoops, boolean convertIterableForLoops,
			boolean makeFinal, boolean checkLoopVarUsed) {
		if (!JavaModelUtil.is50OrHigher(compilationUnit.getJavaElement().getJavaProject()))
			return null;

		if (!convertForLoops && !convertIterableForLoops)
			return null;

		List<ConvertLoopOperation> operations= new ArrayList<>();
		ConvertLoopFixCore.ControlStatementFinder finder= new ConvertLoopFixCore.ControlStatementFinder(convertForLoops, convertIterableForLoops, makeFinal,
				checkLoopVarUsed, operations);
		compilationUnit.accept(finder);

		if (operations.isEmpty())
			return null;

		CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[operations.size()]);
		return new ConvertLoopFix(FixMessages.ControlStatementsFix_change_name, compilationUnit, ops, null);
	}

	public static ConvertLoopFix createConvertForLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertLoopOperation convertForLoopOperation= new ConvertForLoopOperation(loop);
		if (!convertForLoopOperation.satisfiesPreconditions().isOK())
			return null;

		return new ConvertLoopFix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {convertForLoopOperation}, null);
	}

	public static ConvertLoopFix createConvertIterableLoopToEnhancedFix(CompilationUnit compilationUnit, ForStatement loop) {
		ConvertIterableLoopOperation loopConverter= new ConvertIterableLoopOperation(loop);
		IStatus status= loopConverter.satisfiesPreconditions();
		if (status.getSeverity() == IStatus.ERROR)
			return null;

		return new ConvertLoopFix(FixMessages.Java50Fix_ConvertToEnhancedForLoop_description, compilationUnit, new CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] {loopConverter}, status);
	}

	private final IStatus fStatus;

	protected ConvertLoopFix(String name, CompilationUnit compilationUnit, CompilationUnitRewriteOperationsFixCore.CompilationUnitRewriteOperation[] fixRewriteOperations, IStatus status) {
		super(name, compilationUnit, fixRewriteOperations);
		fStatus= status;
	}

	@Override
	public IStatus getStatus() {
		return fStatus;
	}

}
