/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.SourceRange;

/**
 * Helper methods for Java refactorings.
 */
public final class JavaRefactorings {

	/**
	 * Constant describing the importable flag (value: 65536)
	 * <p>
	 * Clients should set this flag to indicate that the refactoring can be
	 * imported from a JAR file.
	 * </p>
	 */
	public static final int JAR_IMPORTABLE= 1 << 16;

	public static RefactoringStatusEntry createStatusEntry(IProblem problem, String newWcSource) {
		RefactoringStatusContext context= new JavaStringStatusContext(newWcSource, new SourceRange(problem));
		int severity= problem.isError() ? RefactoringStatus.ERROR : RefactoringStatus.WARNING;
		return new RefactoringStatusEntry(severity, problem.getMessage(), context);
	}

	private JavaRefactorings() {
		// Not for instantiation
	}
}