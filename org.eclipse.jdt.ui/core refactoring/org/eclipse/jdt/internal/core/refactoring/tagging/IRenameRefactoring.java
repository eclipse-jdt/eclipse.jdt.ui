/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.tagging;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;

/**
 * Represents a refactoring that renames an <code>IJavaElement</code>.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
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
	
}