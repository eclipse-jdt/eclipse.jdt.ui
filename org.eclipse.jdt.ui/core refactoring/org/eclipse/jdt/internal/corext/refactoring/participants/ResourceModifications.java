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
package org.eclipse.jdt.internal.corext.refactoring.participants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CreateArguments;
import org.eclipse.ltk.core.refactoring.participants.CreateParticipant;
import org.eclipse.ltk.core.refactoring.participants.DeleteArguments;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

/**
 * A data structure to collect resource modifications.
 * 
 * @since 3.0
 */
public class ResourceModifications {
	
	private List fCreate;
	private List fDelete;
	
	private List fMove;
	private List fMoveArguments;
	
	private IResource fRename;
	private RenameArguments fRenameArguments;
	
	/**
	 * Adds the given resource to the list of resources 
	 * to be created.
	 * 
	 * @param add the list of resource to be created
	 */
	public void addCreate(IResource create) {
		if (fCreate == null)
			fCreate= new ArrayList(2);
		fCreate.add(create);
	}
	
	/**
	 * Adds the given resource to the list of resources 
	 * to be deleted.
	 * 
	 * @param delete the resource to be deleted
	 */
	public void addDelete(IResource delete) {
		if (fDelete == null)
			fDelete= new ArrayList(2);
		fDelete.add(delete);
	}
	
	/**
	 * Adds the given resource to the list of resources
	 * to be moved.
	 * 
	 * @param move the resource to be moved
	 */
	public void addMove(IResource move, MoveArguments arguments) {
		if (fMove == null) {
			fMove= new ArrayList(2);
			fMoveArguments= new ArrayList(2);
		}
		fMove.add(move);
		fMoveArguments.add(arguments);
	}

	/**
	 * Sets the resource to be rename together with its
	 * new arguments.
	 * 
	 * @param rename the resource to be renamed
	 * @param arguments the arguments of the rename
	 */
	public void setRename(IResource rename, RenameArguments arguments) {
		Assert.isNotNull(rename);
		Assert.isNotNull(arguments);
		fRename= rename;
		fRenameArguments= arguments;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications#getParticipants(org.eclipse.jdt.internal.corext.refactoring.participants.IRefactoringProcessor)
	 */
	public RefactoringParticipant[] getParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) throws CoreException {
		List result= new ArrayList(5);
		if (fDelete != null) {
			DeleteArguments arguments= new DeleteArguments();
			for (Iterator iter= fDelete.iterator(); iter.hasNext();) {
				DeleteParticipant[] deletes= ParticipantManager.loadDeleteParticipants(status, 
					processor, iter.next(), 
					arguments, natures, shared);
				result.addAll(Arrays.asList(deletes));
			}
		}
		if (fCreate != null) {
			CreateArguments arguments= new CreateArguments();
			for (Iterator iter= fCreate.iterator(); iter.hasNext();) {
				CreateParticipant[] creates= ParticipantManager.loadCreateParticipants(status, 
					processor, iter.next(), 
					arguments, natures, shared);
				result.addAll(Arrays.asList(creates));
			}
		}
		if (fMove != null) {
			for (int i= 0; i < fMove.size(); i++) {
				Object element= fMove.get(i);
				MoveArguments arguments= (MoveArguments)fMoveArguments.get(i);
				MoveParticipant[] moves= ParticipantManager.loadMoveParticipants(status, 
					processor, element, 
					arguments, natures, shared);
				result.addAll(Arrays.asList(moves));
				
			}
		}
		/*
		if (fCopy != null) {
			for (int i= 0; i < fCopy.size(); i++) {
				Object element= fCopy.get(i);
				CopyArguments arguments= (CopyArguments)fCopyArguments.get(i);
				CopyParticipant[] copies= ParticipantManager.loadCopyParticipants(processor, 
					element, arguments, 
					natures, shared);
				result.addAll(Arrays.asList(copies));
			}
		}
		*/
		if (fRename != null) {
			RenameParticipant[] renames= ParticipantManager.loadRenameParticipants(status, 
				processor, fRename, 
				fRenameArguments, natures, shared);
			result.addAll(Arrays.asList(renames));
		}
		return (RefactoringParticipant[])result.toArray(new RefactoringParticipant[result.size()]);
	}
}
