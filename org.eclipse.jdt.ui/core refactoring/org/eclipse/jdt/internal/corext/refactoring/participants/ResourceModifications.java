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
package org.eclipse.jdt.internal.corext.refactoring.participants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

/**
 * A data structure describing the resource modification resulting from 
 * applying a ceratin refactoring.
 * 
 * @since 3.0
 */
public class ResourceModifications {
	
	private List fCreate;
	private List fAdd;
	
	private List fMove;
	private IContainer fMoveTarget;
	
	private List fCopy;
	private IContainer fCopyTarget;
	
	private IResource fRename;
	private String fNewName;
	
	/**
	 * Returns the list of resources to be added.
	 * 
	 * @return the list of resources to be added
	 */
	public List getAdd() {
		return fAdd;
	}

	/**
	 * Returns the list of resources to be deleted.
	 * 
	 * @return the list of resources to be deleted
	 */
	public List getCreate() {
		return fCreate;
	}

	/**
	 * Returns the list of resources to be copied.
	 * 
	 * @return the list of resources to be copied
	 */
	public List getCopy() {
		return fCopy;
	}

	/**
	 * Returns the copy target.
	 * 
	 * @return the copy target
	 */
	public IContainer getCopyTarget() {
		return fCopyTarget;
	}

	/**
	 * Returns the list of resources to be moved.
	 * 
	 * @return the list of resources to be moved
	 */
	public List getMove() {
		return fMove;
	}

	/**
	 * Returns the move target
	 * 
	 * @return the move target
	 */
	public IContainer getMoveTarget() {
		return fMoveTarget;
	}

	/**
	 * Returns the resource to be renamed
	 * 
	 * @return the resourcr to be renamed
	 */
	public IResource getRename() {
		return fRename;
	}

	/**
	 * Returns the new name of the resource to be renamed
	 * 
	 * @return the new resource name
	 */
	public String getNewName() {
		return fNewName;
	}
	
	void setCreate(List deleted) {
		fCreate= deleted;
	}
	
	void setAdd(List added) {
		fAdd= added;
	}
	
	void setCopy(List copy, IContainer target) {
		fCopy= copy;
		fCopyTarget= target;
	}
	
	void setMove(List move, IContainer target) {
		fMove= move;
		fMoveTarget= target;
	}
	
	void setRename(IResource rename, String newName) {
		fRename= rename;
		fNewName= newName;
	}
	
	IRefactoringParticipant[] getParticipants(IRefactoringProcessor processor) throws CoreException {
		List result= new ArrayList(5);
		if (fMove != null) {
			IMoveParticipant[] moves= MoveExtensionManager.getParticipants(processor, fMove.toArray());
			for (int i= 0; i < moves.length; i++) {
				moves[i].setTarget(fMoveTarget);
			}
			result.addAll(Arrays.asList(moves));
		}
		return (IRefactoringParticipant[])result.toArray(new IRefactoringParticipant[result.size()]);
	}
}
