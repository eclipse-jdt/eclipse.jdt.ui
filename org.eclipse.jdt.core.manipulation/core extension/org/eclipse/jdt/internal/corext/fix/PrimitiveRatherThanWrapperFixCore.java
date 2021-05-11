/*******************************************************************************
 * Copyright (c) 2021 Fabrice TIERCELIN and others.
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
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

public class PrimitiveRatherThanWrapperFixCore extends CompilationUnitRewriteOperationsFixCore {
	public static ICleanUpFixCore createCleanUp(final CompilationUnit compilationUnit) {
		List<CompilationUnitRewriteOperation> operations= new ArrayList<>();
		compilationUnit.accept(new PrimitiveBooleanRatherThanWrapperFinder(operations));
		compilationUnit.accept(new PrimitiveCharRatherThanWrapperFinder(operations));
		compilationUnit.accept(new PrimitiveByteRatherThanWrapperFinder(operations));
		compilationUnit.accept(new PrimitiveShortRatherThanWrapperFinder(operations));
		compilationUnit.accept(new PrimitiveIntRatherThanWrapperFinder(operations));
		compilationUnit.accept(new PrimitiveLongRatherThanWrapperFinder(operations));
		compilationUnit.accept(new PrimitiveFloatRatherThanWrapperFinder(operations));
		compilationUnit.accept(new PrimitiveDoubleRatherThanWrapperFinder(operations));

		if (operations.isEmpty()) {
			return null;
		}

		CompilationUnitRewriteOperation[] ops= operations.toArray(new CompilationUnitRewriteOperation[0]);
		return new PrimitiveRatherThanWrapperFixCore(FixMessages.PrimitiveRatherThanWrapperFix_description, compilationUnit, ops);
	}

	protected PrimitiveRatherThanWrapperFixCore(final String name, final CompilationUnit compilationUnit, final CompilationUnitRewriteOperation[] fixRewriteOperations) {
		super(name, compilationUnit, fixRewriteOperations);
	}
}
