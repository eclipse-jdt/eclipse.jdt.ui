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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;


public class RenameRefactoring extends Refactoring implements IAdaptable, IRenameRefactoring {

	private Object fElement;
	private IRenameProcessor fProcessor;
	private IRenameParticipant[] fElementParticipants;
	private IRenameParticipant[] fDerivedParticipants;
	private IRefactoringParticipant[] fResourceParticipants;
	
	public RenameRefactoring(Object element) throws CoreException {
		Assert.isNotNull(element);
		
		fElement= element;
		fProcessor= RenameExtensionManager.getProcessor(fElement);
	}
	
	public boolean isAvailable() {
		return fProcessor != null;
	}
		
	public Object getAdapter(Class clazz) {
		if (clazz.isInstance(fProcessor))
			return fProcessor;
		return null;
	}
	
	public IRenameProcessor getProcessor() {
		return fProcessor;
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring#getNewName()
	 */
	public String getNewName() {
		return fProcessor.getNewName();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring#setNewName(java.lang.String)
	 */
	public void setNewName(String newName) {
		fProcessor.setNewName(newName);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring#getCurrentName()
	 */
	public String getCurrentName() {
		return fProcessor.getCurrentName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring#getNewElement()
	 */
	public Object getNewElement() throws JavaModelException {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring#checkNewName(java.lang.String)
	 */
	public RefactoringStatus checkNewName(String newName) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		try {
			result.merge(fProcessor.checkNewName(newName));
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
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		try {
			result.merge(fProcessor.checkActivation());
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
		try {
			fElementParticipants= RenameExtensionManager.getParticipants(
				fProcessor, new Object[] {fProcessor.getElement()});
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}		
		for (int i= 0; i < fElementParticipants.length; i++) {
			IRenameParticipant participant= fElementParticipants[i];
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
		pm.beginTask("", 10); //$NON-NLS-1$
		
		try {
			result.merge(fProcessor.checkInput(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;
				
			initParticipants(new SubProgressMonitor(pm, 2));
			IProgressMonitor sub= new SubProgressMonitor(pm, 6);
			sub.beginTask("", fElementParticipants.length + fDerivedParticipants.length + fResourceParticipants.length); //$NON-NLS-1$
			
			for (int i= 0; i < fElementParticipants.length; i++) {
				IRenameParticipant participant= fElementParticipants[i];
				participant.setNewName(fProcessor.getNewName());
				result.merge(participant.checkInput(new SubProgressMonitor(pm, 1)));
			}
			for (int i= 0; i < fDerivedParticipants.length; i++) {
				IRenameParticipant participant= fDerivedParticipants[i];
				participant.setNewName(fProcessor.getNewName());
				result.merge(participant.checkInput(new SubProgressMonitor(pm, 1)));
			}
			for (int i= 0; i < fResourceParticipants.length; i++) {
				IRefactoringParticipant participant= fResourceParticipants[i];
				result.merge(participant.checkInput(new SubProgressMonitor(pm, 1)));
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
		pm.beginTask("", fElementParticipants.length + fDerivedParticipants.length + fResourceParticipants.length + 1); //$NON-NLS-1$
		CompositeChange result= new CompositeChange();
		try {
			result.add(fProcessor.createChange(new SubProgressMonitor(pm, 1)));
			
			for (int i= 0; i < fElementParticipants.length; i++) {
				IRenameParticipant participant= fElementParticipants[i];
				participant.setNewName(fProcessor.getNewName());
				result.add(participant.createChange(new SubProgressMonitor(pm, 1)));
			}
			for (int i= 0; i < fDerivedParticipants.length; i++) {
				IRenameParticipant participant= fDerivedParticipants[i];
				participant.setNewName(fProcessor.getNewName());
				result.add(participant.createChange(new SubProgressMonitor(pm, 1)));
			}
			for (int i= 0; i < fResourceParticipants.length; i++) {
				IRefactoringParticipant participant= fResourceParticipants[i];
				result.add(participant.createChange(new SubProgressMonitor(pm, 1)));
			}
		} catch (JavaModelException e) {
			throw e;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		}
		return result;		
	}
	
	private void initParticipants(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		fDerivedParticipants= RenameExtensionManager.getParticipants(fProcessor, fProcessor.getDerivedElements());
		pm.worked(1);
		ResourceModifications resourceModifications= fProcessor.getResourceModifications();
		if (resourceModifications != null)
			fResourceParticipants= resourceModifications.getParticipants(fProcessor);
		else
			fResourceParticipants= new IRefactoringParticipant[0];
		pm.worked(2);
		pm.done();
	}
}
