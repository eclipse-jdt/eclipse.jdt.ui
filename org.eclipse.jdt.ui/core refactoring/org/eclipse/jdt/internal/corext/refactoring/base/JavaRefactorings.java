/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.SourceRange;

/**
 * Helper method to code Java refactorings
 */
public class JavaRefactorings {

	public static RefactoringStatusEntry createStatusEntry(IProblem problem, String newWcSource) {
		RefactoringStatusContext context= new JavaStringStatusContext(newWcSource, new SourceRange(problem));
		int severity= problem.isError() ? RefactoringStatus.ERROR: RefactoringStatus.WARNING;
		return new RefactoringStatusEntry(severity, problem.getMessage(), context);
	}
}
