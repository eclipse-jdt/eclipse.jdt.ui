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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;


public class DeleteRefactoring extends Refactoring {

	private Object[] fElements;
	private DeleteProcessor fProcessor;
	private IDeleteParticipant[] fElementParticipants;
	private IDeleteParticipant[] fDerivedParticipants;
	private IRefactoringParticipant[] fMappedParticipants;

	/**
	 * Constructs a new delete refactoring for the given set
	 * of elements.
	 * 
	 * @param elements the elements to be deleted
	 */
	public DeleteRefactoring(Object[] elements) throws CoreException {
		Assert.isTrue(elements != null);
		
		fElements= elements;
		fProcessor= DeleteExtensionManager.getProcessor(fElements);
	}
	
	public boolean isAvailable() {
		return fProcessor != null;
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
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		try {
			result.merge(fProcessor.checkActivation());
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
		try {
			fElementParticipants= fProcessor.getElementParticipants();
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}		
		for (int i= 0; i < fElementParticipants.length; i++) {
			IDeleteParticipant participant= fElementParticipants[i];
			try {
				result.merge(participant.checkActivation());
			} catch (JavaModelException e) {
				throw e;
			} catch (CoreException e) {
				throw new JavaModelException(e);
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		
		try {
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
		} catch (JavaModelException e) {
			throw e;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
		return result;		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", fElementParticipants.length + fDerivedParticipants.length + fMappedParticipants.length + 1); //$NON-NLS-1$
		CompositeChange result= new CompositeChange();
		try {
			result.add(fProcessor.createChange(new SubProgressMonitor(pm, 1)));
			
			for (int i= 0; i < fElementParticipants.length; i++) {
				IDeleteParticipant participant= fElementParticipants[i];
				result.add(participant.createChange(new SubProgressMonitor(pm, fElementParticipants.length)));
			}
			for (int i= 0; i < fDerivedParticipants.length; i++) {
				IDeleteParticipant participant= fDerivedParticipants[i];
				result.add(participant.createChange(new SubProgressMonitor(pm, fDerivedParticipants.length)));
			}
			for (int i= 0; i < fMappedParticipants.length; i++) {
				IRefactoringParticipant participant= fMappedParticipants[i];
				result.add(participant.createChange(new SubProgressMonitor(pm, fMappedParticipants.length)));
			}
		} catch (JavaModelException e) {
			throw e;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
		return result;		
	}

	public String getName() {
		return fProcessor.getProcessorName();
	}
	
	private void initParticipants() throws CoreException {
		fDerivedParticipants= fProcessor.getDerivedParticipants();
		fMappedParticipants= fProcessor.getMappedParticipants();
	}	
}
