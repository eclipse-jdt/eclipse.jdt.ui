/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.tagging;

import java.util.Map;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;

public interface IMultiRenameRefactoring extends IRefactoring{

	/**
	 * Sets new names.
	 * The names are then validated in <code>checkNewNames</code>.
	 * @param renamings Map: String -> String (old name -> new name)
	 */
	public void setNewNames(Map renamings);
	
	/**
	 * Get the old names.
	 * @return Map: String -> String (old name -> new name)
	 */
	public Map getNewNames() throws JavaModelException;

	/**
	 * Checks if the new name (set in <code>setNewName</code>) is valid for
	 * the entity that this refactoring renames.
	 */
	public RefactoringStatus checkNewNames() throws JavaModelException;
	
}

