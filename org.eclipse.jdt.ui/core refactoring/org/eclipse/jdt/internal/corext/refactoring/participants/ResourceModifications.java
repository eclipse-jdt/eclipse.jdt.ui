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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.ltk.core.refactoring.participants.CopyArguments;
import org.eclipse.ltk.core.refactoring.participants.CopyParticipant;
import org.eclipse.ltk.core.refactoring.participants.CreateArguments;
import org.eclipse.ltk.core.refactoring.participants.CreateParticipant;
import org.eclipse.ltk.core.refactoring.participants.DeleteArguments;
import org.eclipse.ltk.core.refactoring.participants.DeleteParticipant;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
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
	private MoveArguments fMoveArguments;
	
	private List fCopy;
	private CopyArguments fCopyArguments;
	
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
	 * Sets the copy arguments.
	 * 
	 * @param arguments the copy arguments
	 */
	public void setCopyArguments(CopyArguments arguments) {
		Assert.isNotNull(arguments);
		fCopyArguments= arguments;
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
	 * Sets the move arguments.
	 * 
	 * @param target the move arguments
	 */
	public void setMoveArguments(MoveArguments arguments) {
		Assert.isNotNull(arguments);
		fMoveArguments= arguments;
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
	public RefactoringParticipant[] getParticipants(RefactoringProcessor processor, String[] natures, SharableParticipants shared) throws CoreException {
		List result= new ArrayList(5);
		if (fDelete != null) {
			DeleteArguments arguments= new DeleteArguments();
			for (Iterator iter= fDelete.iterator(); iter.hasNext();) {
				DeleteParticipant[] deletes= ParticipantManager.getDeleteParticipants(processor, iter.next(), natures, shared);
				for (int i= 0; i < deletes.length; i++) {
					deletes[i].setArguments(arguments);
				}
				result.addAll(Arrays.asList(deletes));
			}
		}
		if (fCreate != null) {
			CreateArguments arguments= new CreateArguments();
			for (Iterator iter= result.iterator(); iter.hasNext();) {
				CreateParticipant[] creates= ParticipantManager.getCreateParticipants(processor, iter.next(), natures, shared);
				for (int i= 0; i < creates.length; i++) {
					creates[i].setArguments(arguments);
				}
				result.addAll(Arrays.asList(creates));
			}
		}
		if (fMove != null) {
			for (Iterator iter= result.iterator(); iter.hasNext();) {
				MoveParticipant[] moves= ParticipantManager.getMoveParticipants(processor, iter.next(), natures, shared);
				for (int i= 0; i < moves.length; i++) {
					moves[i].setArguments(fMoveArguments);
				}
				result.addAll(Arrays.asList(moves));
			}
		}
		if (fCopy != null) {
			for (Iterator iter= result.iterator(); iter.hasNext();) {
				CopyParticipant[] copies= ParticipantManager.getCopyParticipants(processor, iter.next(), natures, shared);
				for (int i= 0; i < copies.length; i++) {
					copies[i].setArguments(fCopyArguments);
				}
				result.addAll(Arrays.asList(copies));
			}
		}
		if (fRename != null) {
			RenameParticipant[] renames= ParticipantManager.getRenameParticipants(processor, fRename, natures, shared);
			for (int i= 0; i < renames.length; i++) {
				renames[i].setArguments(fRenameArguments);
			}
			result.addAll(Arrays.asList(renames));
		}
		return (RefactoringParticipant[])result.toArray(new RefactoringParticipant[result.size()]);
	}
}
