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
package org.eclipse.jdt.internal.corext.refactoring.tagging;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Represents a refactoring that renames an <code>IJavaElement</code>.
 */
public interface IRenameRefactoring {
	
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
