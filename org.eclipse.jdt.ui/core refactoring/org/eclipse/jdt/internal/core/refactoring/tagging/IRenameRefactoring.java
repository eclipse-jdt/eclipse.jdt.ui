/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.tagging;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

/**
 * Represents a refactoring that renames an <code>IJavaElement</code>.
 */
public interface IRenameRefactoring {
	
	/**
	 * Sets new name for the entity that this refactoring is working on.
	 * This name is then validated in <code>checkNewName</code>.
	 */
	public void setNewName(String newName);

	/**
	 * Gets the current name of the entity that this refactoring is working on.
	 */
	public String getCurrentName();
	
	/**
	 * Checks if the new name (set in <code>setNewName</code>) is valid for
	 * the entity that this refactoring renames.
	 */
	public RefactoringStatus checkNewName() throws JavaModelException;
	
	/**
	 * Checks if this refactoring object is capable of updating references to the renamed element.
	 */
	public boolean canEnableUpdateReferences();

	/**
	 * If <code>canUpdateReferences</code> returns <code>true</code>, then this method is used to
	 * inform the refactoring object whether references should be updated.
	 * This call can be ignored if  <code>canUpdateReferences</code> returns <code>false</code>.
	 */	
	public void setUpdateReferences(boolean update);

	/**
	 * If <code>canUpdateReferences</code> returns <code>true</code>, then this method is used to
	 * ask the refactoring object whether references should be updated.
	 * This call can be ignored if  <code>canUpdateReferences</code> returns <code>false</code>.
	 */		
	public boolean getUpdateReferences();
}