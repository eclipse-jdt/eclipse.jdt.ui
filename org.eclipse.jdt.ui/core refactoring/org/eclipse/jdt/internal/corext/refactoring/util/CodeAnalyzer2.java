/*
 * (c) Copyright 2002 IBM Corporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.CompilationUnitBuffer;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

public class CodeAnalyzer2 extends StatementAnalyzer {

	public CodeAnalyzer2(ICompilationUnit cunit, Selection selection, boolean traverseSelectedNode) throws JavaModelException {
		super(cunit, selection, traverseSelectedNode);
	}
	
	protected final void checkSelectedNodes() {
		super.checkSelectedNodes();
		RefactoringStatus status= getStatus();
		if (status.hasFatalError())
			return;
		ASTNode node= getFirstSelectedNode();
		if (node instanceof ArrayInitializer) {
			status.addFatalError("Operation not applicable to an array initializer.", JavaSourceContext.create(fCUnit, node));
		}
	}
}
