/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.core.refactoring.tagging;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.refactoring.IRefactoring;import org.eclipse.jdt.core.refactoring.RefactoringStatus;

public interface IPreactivatedRefactoring extends IRefactoring{
	public RefactoringStatus checkPreactivation() throws JavaModelException;
}
