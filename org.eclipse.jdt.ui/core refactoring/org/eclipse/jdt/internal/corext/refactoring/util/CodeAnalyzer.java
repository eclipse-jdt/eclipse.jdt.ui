/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;

import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class CodeAnalyzer extends StatementAnalyzer {

	public CodeAnalyzer(ICompilationUnit cunit, Selection selection, boolean traverseSelectedNode) throws JavaModelException {
		super(cunit, selection, traverseSelectedNode);
	}
	
	protected final void checkSelectedNodes() {
		super.checkSelectedNodes();
		RefactoringStatus status= getStatus();
		if (status.hasFatalError())
			return;
		ASTNode node= getFirstSelectedNode();
		if (node instanceof ArrayInitializer) {
			status.addFatalError(RefactoringCoreMessages.getString("CodeAnalyzer.array_initializer"), JavaStatusContext.create(fCUnit, node)); //$NON-NLS-1$
		}
	}
}
