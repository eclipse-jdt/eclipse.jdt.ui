/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.tagging;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Represents processors in the JDT space the rename elements.
 */
public interface INameUpdating {
	
	/**
	 * Sets new name for the entity that this refactoring is working on.
	 */
	public void setNewElementName(String newName);
	
	/**
	 * Get the name for the entity that this refactoring is working on.
	 */
	public String getNewElementName();

	/**
	 * Gets the current name of the entity that this refactoring is working on.
	 */
	public String getCurrentElementName();
	
	/**
	 * Gets the element after renaming.
	 */	
	public Object getNewElement() throws CoreException;

	/**
	 * Checks if the new name is valid for the entity that this refactoring renames.
	 */
	public RefactoringStatus checkNewElementName(String newName) throws CoreException;
}
