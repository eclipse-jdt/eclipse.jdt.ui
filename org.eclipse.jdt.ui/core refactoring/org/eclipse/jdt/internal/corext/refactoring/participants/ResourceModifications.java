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
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CopyArguments;
import org.eclipse.ltk.core.refactoring.participants.CopyParticipant;
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
import org.eclipse.ltk.core.refactoring.participants.ReorgExecutionLog;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.internal.corext.Assert;

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
	
	private List fCopy;
	private List fCopyArguments;
	
	/**
	 * Adds the given resource to the list of resources 
	 * to be created.
	 * 
	 * @param create the resource to be add to the list of 
	 *  resources to be created
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
	 * Adds the given resource to the list of resources
	 * to be copied.
	 * 
	 * @param copy the resource to be copied
	 */
	public void addCopy(IResource copy, CopyArguments arguments) {
		if (fCopy == null) {
			fCopy= new ArrayList(2);
			fCopyArguments= new ArrayList(2);
		}
		fCopy.add(copy);
		fCopyArguments.add(arguments);
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
	
	public void addCopy(IPackageFragment pack, IPackageFragmentRoot destination, ReorgExecutionLog log) throws CoreException {
		IContainer container= (IContainer)pack.getResource();
		if (container == null) return;
		IContainer resourceDestination= (IContainer)destination.getResource();
		if (resourceDestination == null) return;
		
		IPath path= resourceDestination.getFullPath();
		path= path.append(pack.getElementName().replace('.', '/'));
		IFolder target= ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
		addCreate(target);
		
		CopyArguments args= new CopyArguments(target, log);
		IFile[] files= getPackageContent(pack);
		for (int i= 0; i < files.length; i++) {
			addCopy(files[i], args);
		}
	}
	
	private IFile[] getPackageContent(IPackageFragment pack) throws CoreException {
		List result= new ArrayList();
		IContainer container= (IContainer)pack.getResource();
		if (container != null) {
			IResource[] members= container.members();
			for (int m= 0; m < members.length; m++) {
				IResource member= members[m];
				if (member instanceof IFile) {
					IFile file= (IFile)member;
					if ("class".equals(file.getFileExtension()) && file.isDerived()) //$NON-NLS-1$
						continue;
					result.add(member);
				}
			}
		}
		return (IFile[])result.toArray(new IFile[result.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.participants.IResourceModifications#getParticipants(org.eclipse.jdt.internal.corext.refactoring.participants.IRefactoringProcessor)
	 */
	public RefactoringParticipant[] getParticipants(RefactoringStatus status, RefactoringProcessor processor, String[] natures, SharableParticipants shared) {
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
		if (fCopy != null) {
			for (int i= 0; i < fCopy.size(); i++) {
				Object element= fCopy.get(i);
				CopyArguments arguments= (CopyArguments)fCopyArguments.get(i);
				CopyParticipant[] copies= ParticipantManager.loadCopyParticipants(status,
					processor, element, 
					arguments, natures, shared);
				result.addAll(Arrays.asList(copies));
			}
		}
		if (fRename != null) {
			RenameParticipant[] renames= ParticipantManager.loadRenameParticipants(status, 
				processor, fRename, 
				fRenameArguments, natures, shared);
			result.addAll(Arrays.asList(renames));
		}
		return (RefactoringParticipant[])result.toArray(new RefactoringParticipant[result.size()]);
	}
}
