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
package org.eclipse.ltk.core.refactoring.participants;

import org.eclipse.core.runtime.CoreException;



public class ExtensionManagers {

	//---- Move participants ----------------------------------------------------------------
	
	private static final String MOVE_PARTICIPANT_EXT_POINT= "moveParticipants"; //$NON-NLS-1$
	private static ExtensionManager fgMoveExtensions= new ExtensionManager("Move", MOVE_PARTICIPANT_EXT_POINT); //$NON-NLS-1$

	public static MoveParticipant[] getMoveParticipants(RefactoringProcessor processor, Object[] elements, SharableParticipants shared) throws CoreException {
		RefactoringParticipant[] participants= fgMoveExtensions.getParticipants(processor, elements, 
			fgMoveExtensions.createParticipantEvaluationContext(elements, processor), shared);
		MoveParticipant[] result= new MoveParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}

	//---- Copy participants ----------------------------------------------------------------
	
	private static final String COPY_PARTICIPANT_EXT_POINT= "copyParticipants"; //$NON-NLS-1$
	private static ExtensionManager fgCopyInstance= new ExtensionManager("Copy", COPY_PARTICIPANT_EXT_POINT); //$NON-NLS-1$
	
	public static CopyParticipant[] getCopyParticipants(RefactoringProcessor processor, Object[] elements, SharableParticipants shared) throws CoreException {
		RefactoringParticipant[] participants= fgCopyInstance.getParticipants(processor, elements, 
			fgCopyInstance.createParticipantEvaluationContext(elements, processor), shared);
		CopyParticipant[] result= new CopyParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}

	//---- Delete participants ----------------------------------------------------------------
	
	private static final String DELETE_PARTICIPANT_EXT_POINT= "deleteParticipants"; //$NON-NLS-1$
	private static ExtensionManager fgDeleteInstance= new ExtensionManager("Delete", DELETE_PARTICIPANT_EXT_POINT); //$NON-NLS-1$
	
	public static DeleteParticipant[] getDeleteParticipants(RefactoringProcessor processor, Object[] elements, SharableParticipants shared) throws CoreException {
		RefactoringParticipant[] participants= fgDeleteInstance.getParticipants(processor, elements, 
			fgDeleteInstance.createParticipantEvaluationContext(elements, processor), shared);
		DeleteParticipant[] result= new DeleteParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}

	//---- Create participants ----------------------------------------------------------------
	
	private static final String CREATE_PARTICIPANT_EXT_POINT= "createParticipants"; //$NON-NLS-1$
	private static ExtensionManager fgCreateInstance= new ExtensionManager("Create", CREATE_PARTICIPANT_EXT_POINT); //$NON-NLS-1$
	
	public static CreateParticipant[] getCreateParticipants(RefactoringProcessor processor, Object[] elements, SharableParticipants shared) throws CoreException {
		RefactoringParticipant[] participants= fgCreateInstance.getParticipants(processor, elements, 
			fgCreateInstance.createParticipantEvaluationContext(elements, processor), shared);
		CreateParticipant[] result= new CreateParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}

	//---- Rename participants ----------------------------------------------------------------
	
	private static final String RENAME_PARTICIPANT_EXT_POINT= "renameParticipants"; //$NON-NLS-1$
	private static ExtensionManager fgRenameInstance= new ExtensionManager("Rename", RENAME_PARTICIPANT_EXT_POINT); //$NON-NLS-1$
	
	public static RenameParticipant[] getRenameParticipants(RefactoringProcessor processor, Object[] elements, SharableParticipants shared) throws CoreException {
		RefactoringParticipant[] participants= fgRenameInstance.getParticipants(
			processor, elements, fgRenameInstance.createParticipantEvaluationContext(elements, processor), 
			shared);
		RenameParticipant[] result= new RenameParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}
}
