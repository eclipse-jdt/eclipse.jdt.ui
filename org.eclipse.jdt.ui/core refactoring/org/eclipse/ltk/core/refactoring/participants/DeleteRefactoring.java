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
package org.eclipse.ltk.core.refactoring.participants;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.internal.core.refactoring.Assert;
import org.eclipse.ltk.internal.core.refactoring.DelegatingValidationStateChange;

public class DeleteRefactoring extends Refactoring {

	private DeleteProcessor fProcessor;
	private DeleteParticipant[] fElementParticipants;
	private RefactoringParticipant[] fSecondaryParticipants;

	private CheckConditionsContext fContext;
	
	/**
	 * Constructs a new delete refactoring for the given processor.
	 * 
	 * @param processor the delete processor
	 */
	public DeleteRefactoring(DeleteProcessor processor) throws CoreException {
		Assert.isNotNull(processor);
		fProcessor= processor;
		fContext= new CheckConditionsContext();
		IConditionChecker checker= new ValidateEditChecker(null);
		fContext.add(checker);
	}
	
	public boolean isAvailable() throws CoreException {
		return fProcessor.isApplicable();
	}

	public int getStyle() {
		return fProcessor.getStyle();
	}
	
	public Object getAdapter(Class clazz) {
		if (clazz.isInstance(fProcessor))
			return fProcessor;
		return super.getAdapter(clazz);
	}
	
	/**
	 * Returns the delete processor.
	 * 
	 * @return the delete processor;
	 */
	public DeleteProcessor getProcessor() {
		return fProcessor;
	}
		
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 4); //$NON-NLS-1$
		result.merge(fProcessor.checkInitialConditions(new SubProgressMonitor(pm, 3), fContext));
		if (result.hasFatalError())
			return result;
		
		fElementParticipants= fProcessor.loadElementParticipants();
		IProgressMonitor sm= new SubProgressMonitor(pm, 1);
		sm.beginTask("", fElementParticipants.length); //$NON-NLS-1$
		for (int i= 0; i < fElementParticipants.length; i++) {
			DeleteParticipant participant= fElementParticipants[i];
			result.merge(participant.checkInitialConditions(new SubProgressMonitor(sm, 1), fContext));
		}
		return result;
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 3); //$NON-NLS-1$
		
		result.merge(fProcessor.checkFinalConditions(new SubProgressMonitor(pm, 1), fContext));
		if (result.hasFatalError())
			return result;
			
		IProgressMonitor sm= new SubProgressMonitor(pm, 1);
		sm.beginTask("", fElementParticipants.length); //$NON-NLS-1$
		for (int i= 0; i < fElementParticipants.length; i++) {
			DeleteParticipant participant= fElementParticipants[i];
			fProcessor.setArgumentsTo(participant);
			result.merge(participant.checkFinalConditions(new SubProgressMonitor(sm, 1), fContext));
		}
		if (result.hasFatalError())
			return result;
		fSecondaryParticipants= fProcessor.loadDerivedParticipants();
		sm= new SubProgressMonitor(pm, 1);
		sm.beginTask("", fSecondaryParticipants.length); //$NON-NLS-1$
		for (int i= 0; i < fSecondaryParticipants.length; i++) {
			RefactoringParticipant participant= fSecondaryParticipants[i];
			result.merge(participant.checkFinalConditions(new SubProgressMonitor(sm, 1), fContext));
		}
		return result;		
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", fElementParticipants.length + fSecondaryParticipants.length + 1); //$NON-NLS-1$
		List changes= new ArrayList();
		changes.add(fProcessor.createChange(new SubProgressMonitor(pm, 1)));
		
		for (int i= 0; i < fElementParticipants.length; i++) {
			DeleteParticipant participant= fElementParticipants[i];
			changes.add(participant.createChange(new SubProgressMonitor(pm, 1)));
		}
		for (int i= 0; i < fSecondaryParticipants.length; i++) {
			RefactoringParticipant participant= fSecondaryParticipants[i];
			changes.add(participant.createChange(new SubProgressMonitor(pm, 1)));
		}
		return new DelegatingValidationStateChange((Change[]) changes.toArray(new Change[changes.size()]));		
	}

	public String getName() {
		return fProcessor.getProcessorName();
	}
}
