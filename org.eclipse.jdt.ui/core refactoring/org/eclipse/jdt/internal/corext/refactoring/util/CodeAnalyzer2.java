/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.jdt.core.dom.ArrayInitializer;

import org.eclipse.jdt.internal.corext.dom.CompilationUnitBuffer;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

public class CodeAnalyzer2 extends StatementAnalyzer {

	public CodeAnalyzer2(CompilationUnitBuffer buffer, Selection selection, boolean traverseSelectedNode) {
		super(buffer, selection, traverseSelectedNode);
	}
	
	protected final void checkSelectedNodes() {
		super.checkSelectedNodes();
		RefactoringStatus status= getStatus();
		if (status.hasFatalError())
			return;
		if (getFirstSelectedNode() instanceof ArrayInitializer) {
			status.addFatalError("Operation not applicable to an array initializer.");
		}
	}
}
