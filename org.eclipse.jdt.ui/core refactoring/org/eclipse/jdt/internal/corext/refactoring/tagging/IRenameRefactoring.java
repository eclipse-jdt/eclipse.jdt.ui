/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.tagging;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

/**
 * Represents a refactoring that renames an <code>IJavaElement</code>.
 */
public interface IRenameRefactoring extends IRefactoring{
	
	/**
	 * Sets new name for the entity that this refactoring is working on.
	 */
	public void setNewName(String newName);
	
	/**
	 * Get the name for the entity that this refactoring is working on.
	 */
	public String getNewName();

	/**
	 * Gets the current name of the entity that this refactoring is working on.
	 */
	public String getCurrentName();
	
	/**
	 * Gets the element after renaming.
	 */	
	public Object getNewElement() throws JavaModelException;

	/**
	 * Checks if the new name is valid for the entity that this refactoring renames.
	 */
	public RefactoringStatus checkNewName(String newName) throws JavaModelException;
}