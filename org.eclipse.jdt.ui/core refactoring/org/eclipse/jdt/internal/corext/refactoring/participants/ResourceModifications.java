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
 * A default implementation of <code>IResourceModifications</code>.
 * 
 * @since 3.0
 */
public class ResourceModifications implements IResourceModifications {
	
	private List fCreate;
	private List fDelete;
	
	private List fMove;
	private IContainer fMoveTarget;
	
	private List fCopy;
	private IContainer fCopyTarget;
	
	private IResource fRename;
	private String fNewName;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications#getCreate()
	 */
	public List getCreate() {
		return fCreate;
	}

	/**
	 * Adds the given resource to the list of resorces 
	 * to be created.
	 * 
	 * @param add the list of resource to be created
	 */
	public void addCreate(IResource create) {
		if (fCreate == null)
			fCreate= new ArrayList(2);
		fCreate.add(create);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications#getDelete()
	 */
	public List getDelete() {
		return fDelete;
	}

	/**
	 * Adds the given resource to the list of resorces 
	 * to be deleted.
	 * 
	 * @param delete the resource to be deleted
	 */
	public void addDelete(IResource delete) {
		if (fDelete == null)
			fDelete= new ArrayList(2);
		fDelete.add(delete);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications#getCopy()
	 */
	public List getCopy() {
		return fCopy;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications#getCopyTarget()
	 */
	public IContainer getCopyTarget() {
		return fCopyTarget;
	}

	/**
	 * Adds the given resource to the list of resources
	 * to be copied.
	 * 
	 * @param copy the resource to be copied
	 */
	public void addCopy(IResource copy) {
		if (fCopy == null)
			fCopy= new ArrayList(2);
		fCopy.add(copy);
	}

	/**
	 * Sets the copy target.
	 * 
	 * @param target the copy target
	 */
	public void setCopyTarget(IContainer target) {
		fCopyTarget= target;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications#getMove()
	 */
	public List getMove() {
		return fMove;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications#getMoveTarget()
	 */
	public IContainer getMoveTarget() {
		return fMoveTarget;
	}

	/**
	 * Adds the given resource to the list of resources
	 * to be moved.
	 * 
	 * @param move the resource to be moved
	 */
	public void addMove(IResource move) {
		if (fMove == null)
			fMove= new ArrayList(2);
		fMove.add(move);
	}

	/**
	 * Sets the move target.
	 * 
	 * @param target the move target
	 */
	public void setMoveTarget(IContainer target) {
		fMoveTarget= target;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications#getRename()
	 */
	public IResource getRename() {
		return fRename;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications#getNewName()
	 */
	public String getNewName() {
		return fNewName;
	}
	
	/**
	 * Sets the resource to be rename together with its
	 * new name.
	 * 
	 * @param rename the resource to be renamed
	 * @param newName the new name of the resource
	 */
	public void setRename(IResource rename, String newName) {
		fRename= rename;
		fNewName= newName;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications#getParticipants(org.eclipse.jdt.internal.corext.refactoring.participants.IRefactoringProcessor)
	 */
	public IRefactoringParticipant[] getParticipants(IRefactoringProcessor processor, SharableParticipants shared) throws CoreException {
		List result= new ArrayList(5);
		if (fDelete != null) {
			IDeleteParticipant[] deletes= DeleteExtensionManager.getParticipants(processor, fDelete.toArray(), shared);
			result.addAll(Arrays.asList(deletes));
		}
		if (fCreate != null) {
			ICreateParticipant[] creates= CreateExtensionManager.getParticipants(processor, fCreate.toArray());
			result.addAll(Arrays.asList(creates));
		}
		if (fMove != null) {
			IMoveParticipant[] moves= MoveExtensionManager.getParticipants(processor, fMove.toArray());
			for (int i= 0; i < moves.length; i++) {
				moves[i].setTarget(fMoveTarget);
			}
			result.addAll(Arrays.asList(moves));
		}
		if (fCopy != null) {
			ICopyParticipant[] copies= CopyExtensionManager.getParticipants(processor, fCopy.toArray());
			for (int i= 0; i < copies.length; i++) {
				copies[i].setTarget(fCopyTarget);
			}
			result.addAll(Arrays.asList(copies));
		}
		return (IRefactoringParticipant[])result.toArray(new IRefactoringParticipant[result.size()]);
	}
}
