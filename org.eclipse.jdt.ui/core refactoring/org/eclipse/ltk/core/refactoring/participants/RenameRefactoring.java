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


public class RenameRefactoring extends Refactoring {

	private RenameProcessor fProcessor;
	private RenameParticipant[] fElementParticipants;
	private RefactoringParticipant[] fSecondaryParticipants;
	
	private CheckConditionsContext fContext;
	
	public RenameRefactoring(RenameProcessor processor) throws CoreException {
		Assert.isNotNull(processor);
		fProcessor= processor;
		fContext= new CheckConditionsContext();
		IConditionChecker checker= new ValidateEditChecker(null);
		fContext.add(checker);
	}
	
	public boolean isAvailable() throws CoreException {
		return fProcessor.isApplicable();
	}
		
	public RenameProcessor getProcessor() {
		return fProcessor;
	}
	
	public int getStyle() {
		return fProcessor.getStyle();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return fProcessor.getProcessorName();
	}
	
	/**
	 * {@inheritDoc}
	 */
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
			RenameParticipant participant= fElementParticipants[i];
			result.merge(participant.checkInitialConditions(new SubProgressMonitor(sm, 1), fContext));
		}
		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		
		pm.beginTask("", 3); //$NON-NLS-1$
		
		result.merge(fProcessor.checkFinalConditions(new SubProgressMonitor(pm, 1), fContext));
		if (result.hasFatalError())
			return result;
			
		IProgressMonitor sm= new SubProgressMonitor(pm, 1);
		sm.beginTask("", fElementParticipants.length); //$NON-NLS-1$
		for (int i= 0; i < fElementParticipants.length; i++) {
			RenameParticipant participant= fElementParticipants[i];
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
	
	/**
	 * {@inheritDoc}
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", fElementParticipants.length + fSecondaryParticipants.length + 1); //$NON-NLS-1$
		List changes= new ArrayList();
		changes.add(fProcessor.createChange(new SubProgressMonitor(pm, 1)));
		
		for (int i= 0; i < fElementParticipants.length; i++) {
			RenameParticipant participant= fElementParticipants[i];
			changes.add(participant.createChange(new SubProgressMonitor(pm, 1)));
		}
		for (int i= 0; i < fSecondaryParticipants.length; i++) {
			RefactoringParticipant participant= fSecondaryParticipants[i];
			changes.add(participant.createChange(new SubProgressMonitor(pm, 1)));
		}
		return new DelegatingValidationStateChange((Change[]) changes.toArray(new Change[changes.size()]));		
	}
	
	/**
	 * Adapts the refactoring to the given type. The adapter is resolved
	 * as follows:
	 * <ol>
	 *   <li>the refactoring itself is checked whether it is an instance
	 *       of the requested type.</li>
	 *   <li>its processor is checked whether it is an instance of the
	 *       requested type.</li>
	 *   <li>the request is delegated to the super class.</li>
	 * </ol>
	 * 
	 * @return the requested adapter or <code>null</code>if no adapter
	 *  exists. 
	 */
	public Object getAdapter(Class clazz) {
		if (clazz.isInstance(this))
			return this;
		if (clazz.isInstance(fProcessor))
			return fProcessor;
		return super.getAdapter(clazz);
	}
	
	/* non java-doc
	 * for debugging only
	 */
	public String toString() {
		return getName();
	}
}
