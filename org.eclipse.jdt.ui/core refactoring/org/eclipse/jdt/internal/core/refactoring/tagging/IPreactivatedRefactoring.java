/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.tagging;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.IRefactoring;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

public interface IPreactivatedRefactoring extends IRefactoring{
	public RefactoringStatus checkPreactivation() throws JavaModelException;
}
