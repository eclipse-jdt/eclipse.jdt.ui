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
package org.eclipse.jdt.internal.corext.refactoring.participants;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.ltk.internal.refactoring.core.DelegatingValidationStateChange;


public class DeleteRefactoring extends Refactoring {

	private DeleteProcessor fProcessor;
	private IDeleteParticipant[] fElementParticipants;
	private IDeleteParticipant[] fDerivedParticipants;
	private IRefactoringParticipant[] fMappedParticipants;

	/**
	 * Constructs a new delete refactoring for the given processor.
	 * 
	 * @param processor the delete processor
	 */
	public DeleteRefactoring(DeleteProcessor processor) {
		Assert.isNotNull(processor);
		fProcessor= processor;
	}
	
	public boolean isAvailable() throws CoreException {
		return fProcessor.isAvailable();
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
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(fProcessor.checkActivation());
		fElementParticipants= fProcessor.getElementParticipants();		
		for (int i= 0; i < fElementParticipants.length; i++) {
			IDeleteParticipant participant= fElementParticipants[i];
			result.merge(participant.checkActivation());
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		
		initParticipants();
		pm.beginTask("", 2 + fElementParticipants.length + fDerivedParticipants.length + fMappedParticipants.length); //$NON-NLS-1$
		
		result.merge(fProcessor.checkInput(new SubProgressMonitor(pm, 1)));
		if (result.hasFatalError())
			return result;
			
		
		for (int i= 0; i < fElementParticipants.length; i++) {
			IDeleteParticipant participant= fElementParticipants[i];
			result.merge(participant.checkInput(new SubProgressMonitor(pm, fElementParticipants.length)));
		}
		for (int i= 0; i < fDerivedParticipants.length; i++) {
			IDeleteParticipant participant= fDerivedParticipants[i];
			result.merge(participant.checkInput(new SubProgressMonitor(pm, fDerivedParticipants.length)));
		}
		for (int i= 0; i < fMappedParticipants.length; i++) {
			IRefactoringParticipant participant= fMappedParticipants[i];
			result.merge(participant.checkInput(new SubProgressMonitor(pm, fMappedParticipants.length)));
		}
		return result;		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", fElementParticipants.length + fDerivedParticipants.length + fMappedParticipants.length + 1); //$NON-NLS-1$
		List changes= new ArrayList();
		changes.add(fProcessor.createChange(new SubProgressMonitor(pm, 1)));
		
		for (int i= 0; i < fElementParticipants.length; i++) {
			IDeleteParticipant participant= fElementParticipants[i];
			changes.add(participant.createChange(new SubProgressMonitor(pm, fElementParticipants.length)));
		}
		for (int i= 0; i < fDerivedParticipants.length; i++) {
			IDeleteParticipant participant= fDerivedParticipants[i];
			changes.add(participant.createChange(new SubProgressMonitor(pm, fDerivedParticipants.length)));
		}
		for (int i= 0; i < fMappedParticipants.length; i++) {
			IRefactoringParticipant participant= fMappedParticipants[i];
			changes.add(participant.createChange(new SubProgressMonitor(pm, fMappedParticipants.length)));
		}
		return new DelegatingValidationStateChange((Change[]) changes.toArray(new Change[changes.size()]));		
	}

	public String getName() {
		return fProcessor.getProcessorName();
	}
	
	private void initParticipants() throws CoreException {
		fDerivedParticipants= fProcessor.getDerivedParticipants();
		fMappedParticipants= fProcessor.getMappedParticipants();
	}	
}
