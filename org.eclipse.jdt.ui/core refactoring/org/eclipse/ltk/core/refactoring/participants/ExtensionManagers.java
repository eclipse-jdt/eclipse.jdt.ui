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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;

import org.eclipse.core.expressions.EvaluationContext;

/**
 * Facade to access the rename, move, delete, create and copy participant
 * extension point provided by the org.eclipse.ltk.core.refactoring plug-in.
 * 
 * @since 3.0
 */
public class ExtensionManagers {
	
	public static EvaluationContext createStandardEvaluationContext(RefactoringProcessor processor, Object[] elements, IProject[] affectedProjects) throws CoreException {
		return createStandardEvaluationContext(processor, elements, getNatures(affectedProjects));
	}
	
	public static EvaluationContext createStandardEvaluationContext(RefactoringProcessor processor, Object[] elements, String[] affectedNatures) throws CoreException {
		List ce= Arrays.asList(elements);
		EvaluationContext result= new EvaluationContext(null, ce);
		result.addVariable("elements", ce); //$NON-NLS-1$
		result.addVariable("affectedNatures", Arrays.asList(affectedNatures)); //$NON-NLS-1$
		result.addVariable("processorIdentifier", processor.getIdentifier()); //$NON-NLS-1$
		return result;
	}
	
	//---- Move participants ----------------------------------------------------------------
	
	private static final String MOVE_PARTICIPANT_EXT_POINT= "moveParticipants"; //$NON-NLS-1$
	private static ExtensionManager fgMoveExtensions= new ExtensionManager("Move", MOVE_PARTICIPANT_EXT_POINT); //$NON-NLS-1$

	public static MoveParticipant[] getMoveParticipants(RefactoringProcessor processor, Object[] elements, IProject[] affectedProjects, SharableParticipants shared) throws CoreException {
		EvaluationContext evalContext= createStandardEvaluationContext(processor, elements, affectedProjects);
		return getMoveParticipants(processor, elements, evalContext, shared);
	}
	
	public static MoveParticipant[] getMoveParticipants(RefactoringProcessor processor, Object[] elements, EvaluationContext evalContext, SharableParticipants shared) throws CoreException {
		RefactoringParticipant[] participants= fgMoveExtensions.getParticipants(processor, elements, evalContext, shared);
		MoveParticipant[] result= new MoveParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}

	//---- Copy participants ----------------------------------------------------------------
	
	private static final String COPY_PARTICIPANT_EXT_POINT= "copyParticipants"; //$NON-NLS-1$
	private static ExtensionManager fgCopyInstance= new ExtensionManager("Copy", COPY_PARTICIPANT_EXT_POINT); //$NON-NLS-1$
	
	public static CopyParticipant[] getCopyParticipants(RefactoringProcessor processor, Object[] elements, IProject[] affectedProjects, SharableParticipants shared) throws CoreException {
		EvaluationContext evalContext= createStandardEvaluationContext(processor, elements, affectedProjects);
		return getCopyParticipants(processor, elements, evalContext, shared);
	}
	
	public static CopyParticipant[] getCopyParticipants(RefactoringProcessor processor, Object[] elements, EvaluationContext evalContext, SharableParticipants shared) throws CoreException {
		RefactoringParticipant[] participants= fgCopyInstance.getParticipants(processor, elements, evalContext, shared);
		CopyParticipant[] result= new CopyParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}

	//---- Delete participants ----------------------------------------------------------------
	
	private static final String DELETE_PARTICIPANT_EXT_POINT= "deleteParticipants"; //$NON-NLS-1$
	private static ExtensionManager fgDeleteInstance= new ExtensionManager("Delete", DELETE_PARTICIPANT_EXT_POINT); //$NON-NLS-1$
	
	public static DeleteParticipant[] getDeleteParticipants(RefactoringProcessor processor, Object[] elements, IProject[] affectedProjects, SharableParticipants shared) throws CoreException {
		EvaluationContext evalContext= createStandardEvaluationContext(processor, elements, affectedProjects);
		return getDeleteParticipants(processor, elements, evalContext, shared);
	}
	
	public static DeleteParticipant[] getDeleteParticipants(RefactoringProcessor processor, Object[] elements, EvaluationContext evalContext, SharableParticipants shared) throws CoreException {
		RefactoringParticipant[] participants= fgDeleteInstance.getParticipants(processor, elements, evalContext, shared);
		DeleteParticipant[] result= new DeleteParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}

	//---- Create participants ----------------------------------------------------------------
	
	private static final String CREATE_PARTICIPANT_EXT_POINT= "createParticipants"; //$NON-NLS-1$
	private static ExtensionManager fgCreateInstance= new ExtensionManager("Create", CREATE_PARTICIPANT_EXT_POINT); //$NON-NLS-1$
	
	public static CreateParticipant[] getCreateParticipants(RefactoringProcessor processor, Object[] elements, IProject[] affectedProjects, SharableParticipants shared) throws CoreException {
		EvaluationContext evalContext= createStandardEvaluationContext(processor, elements, affectedProjects);
		return getCreateParticipants(processor, elements, evalContext, shared);
	}
	
	public static CreateParticipant[] getCreateParticipants(RefactoringProcessor processor, Object[] elements, EvaluationContext evalContext, SharableParticipants shared) throws CoreException {
		RefactoringParticipant[] participants= fgCreateInstance.getParticipants(processor, elements, evalContext, shared);
		CreateParticipant[] result= new CreateParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}

	//---- Rename participants ----------------------------------------------------------------
	
	private static final String RENAME_PARTICIPANT_EXT_POINT= "renameParticipants"; //$NON-NLS-1$
	private static ExtensionManager fgRenameInstance= new ExtensionManager("Rename", RENAME_PARTICIPANT_EXT_POINT); //$NON-NLS-1$
	
	public static RenameParticipant[] getRenameParticipants(RefactoringProcessor processor, Object[] elements, IProject[] affectedProjects, SharableParticipants shared) throws CoreException {
		EvaluationContext evalContext= createStandardEvaluationContext(processor, elements, affectedProjects);
		return getRenameParticipants(processor, elements, evalContext, shared);
	}
	
	public static RenameParticipant[] getRenameParticipants(RefactoringProcessor processor, Object[] elements, EvaluationContext evalContext, SharableParticipants shared) throws CoreException {
		RefactoringParticipant[] participants= fgRenameInstance.getParticipants(processor, elements, evalContext, shared);
		RenameParticipant[] result= new RenameParticipant[participants.length];
		System.arraycopy(participants, 0, result, 0, participants.length);
		return result;
	}
	
	//---- Helper methods ------------------------------------------------------------------
	
	private static String[] getNatures(IProject[] affectedProjects) throws CoreException {
		Set result= new HashSet();
		for (int i= 0; i < affectedProjects.length; i++) {
			String[] pns= affectedProjects[i].getDescription().getNatureIds();
			for (int p = 0; p < pns.length; p++) {
				result.add(pns[p]);
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}	
}
