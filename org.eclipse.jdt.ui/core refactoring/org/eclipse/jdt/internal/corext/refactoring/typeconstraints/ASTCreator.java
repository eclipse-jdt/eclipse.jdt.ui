/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;


public class ASTCreator {

	public static final String CU_PROPERTY= "org.eclipse.jdt.ui.refactoring.cu"; //$NON-NLS-1$

	private ASTCreator() {
	}
	
	public static CompilationUnit createAST(ICompilationUnit cu, WorkingCopyOwner workingCopyOwner) {
		ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		CompilationUnit cuNode= getCuNode(workingCopyOwner, wc);
		cuNode.setProperty(CU_PROPERTY, wc);
		return cuNode;
	}

	private static CompilationUnit getCuNode(WorkingCopyOwner workingCopyOwner, ICompilationUnit wc) {
		if (workingCopyOwner == null)
			return AST.parseCompilationUnit(wc, true);
		return AST.parseCompilationUnit(wc, true, workingCopyOwner, null);
	}

	public static ICompilationUnit getCu(ASTNode node) {
		Object property= node.getRoot().getProperty(CU_PROPERTY);
		if (property instanceof ICompilationUnit)
			return (ICompilationUnit)property;
		return null;
	}
}
