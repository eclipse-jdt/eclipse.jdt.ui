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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.IProcessorBasedRefactoring;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.internal.core.refactoring.DelegatingValidationStateChange;


public class RenameRefactoring extends Refactoring implements IProcessorBasedRefactoring, IRenameRefactoring {

	private RenameProcessor fProcessor;
	private RenameParticipant[] fElementParticipants;
	private RefactoringParticipant[] fSecondaryParticipants;
	
	public RenameRefactoring(RenameProcessor processor) throws CoreException {
		Assert.isNotNull(processor);
		fProcessor= processor;
	}
	
	public boolean isAvailable() throws CoreException {
		return fProcessor.isAvailable();
	}
		
	public Object getAdapter(Class clazz) {
		if (clazz.isInstance(fProcessor))
			return fProcessor;
		return super.getAdapter(clazz);
	}
	
	public RefactoringProcessor getProcessor() {
		return fProcessor;
	}
	
	public int getStyle() {
		return fProcessor.getStyle();
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring#getNewName()
	 */
	public String getNewName() {
		return fProcessor.getNewElementName();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring#setNewName(java.lang.String)
	 */
	public void setNewName(String newName) {
		fProcessor.setNewElementName(newName);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring#getCurrentName()
	 */
	public String getCurrentName() {
		return fProcessor.getCurrentElementName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring#getNewElement()
	 */
	public Object getNewElement() throws JavaModelException {
		try {
			return fProcessor.getNewElement();
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring#checkNewName(java.lang.String)
	 */
	public RefactoringStatus checkNewName(String newName) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		try {
			result.merge(fProcessor.checkNewElementName(newName));
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
		return result;
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return fProcessor.getProcessorName();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		result.merge(fProcessor.checkActivation());
		fElementParticipants= fProcessor.getElementParticipants();		
		for (int i= 0; i < fElementParticipants.length; i++) {
			RenameParticipant participant= fElementParticipants[i];
			result.merge(participant.checkActivation());
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		
		pm.beginTask("", 3); //$NON-NLS-1$
		
		result.merge(fProcessor.checkInput(new SubProgressMonitor(pm, 1)));
		if (result.hasFatalError())
			return result;
			
		IProgressMonitor sm= new SubProgressMonitor(pm, 1);
		sm.beginTask("", fElementParticipants.length); //$NON-NLS-1$
		for (int i= 0; i < fElementParticipants.length; i++) {
			RenameParticipant participant= fElementParticipants[i];
			fProcessor.setArgumentsTo(participant);
			result.merge(participant.checkInput(new SubProgressMonitor(sm, 1)));
		}
		if (result.hasFatalError())
			return result;
		fSecondaryParticipants= fProcessor.getSecondaryParticipants();
		sm= new SubProgressMonitor(pm, 1);
		sm.beginTask("", fSecondaryParticipants.length); //$NON-NLS-1$
		for (int i= 0; i < fSecondaryParticipants.length; i++) {
			RefactoringParticipant participant= fSecondaryParticipants[i];
			result.merge(participant.checkInput(new SubProgressMonitor(sm, 1)));
		}
		return result;		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
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
	
	/* non java-doc
	 * for debugging only
	 */
	public String toString() {
		return getName();
	}
}
