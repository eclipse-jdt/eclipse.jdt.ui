/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationMessages;
import org.eclipse.jdt.internal.corext.dom.Selection;

public class CodeAnalyzer extends StatementAnalyzer {

	public CodeAnalyzer(ICompilationUnit cunit, Selection selection, boolean traverseSelectedNode) throws CoreException {
		super(cunit, selection, traverseSelectedNode);
	}

	@Override
	protected final void checkSelectedNodes() {
		super.checkSelectedNodes();
		RefactoringStatus status= getStatus();
		if (status.hasFatalError())
			return;
		ASTNode node= getFirstSelectedNode();
		if (node instanceof ArrayInitializer) {
			status.addFatalError(JavaManipulationMessages.CodeAnalyzer_array_initializer, JavaStatusContext.create(fCUnit, node));
		}
	}
}
