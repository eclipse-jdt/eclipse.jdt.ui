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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.CopyRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgDestinationValidator;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;


public class ReorgCopyWizard extends RefactoringWizard {

	public ReorgCopyWizard(CopyRefactoring ref) {
		super(ref, DIALOG_BASED_UESR_INTERFACE | NO_PREVIEW_PAGE); 
		setDefaultPageTitle(ReorgMessages.getString("ReorgCopyWizard.1")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	protected void addUserInputPages() {
		addPage(new CopyInputPage());
	}
	
	private static class CopyInputPage extends ReorgUserInputPage{

		private static final String PAGE_NAME= "CopyInputPage"; //$NON-NLS-1$

		public CopyInputPage() {
			super(PAGE_NAME);
		}

		private CopyRefactoring getCopyRefactoring(){
			return (CopyRefactoring) getRefactoring();
		}

		protected Object getInitiallySelectedElement() {
			return getCopyRefactoring().getCommonParentForInputElements();
		}

		protected IJavaElement[] getJavaElements() {
			return getCopyRefactoring().getJavaElements();
		}

		protected IResource[] getResources() {
			return getCopyRefactoring().getResources();
		}

		protected IReorgDestinationValidator getDestinationValidator() {
			return getCopyRefactoring();
		}
		
		protected RefactoringStatus verifyDestination(Object selected) throws JavaModelException{
			if (selected instanceof IJavaElement)
				return getCopyRefactoring().setDestination((IJavaElement)selected);
			if (selected instanceof IResource)
				return getCopyRefactoring().setDestination((IResource)selected);
			return RefactoringStatus.createFatalErrorStatus(ReorgMessages.getString("ReorgCopyWizard.2")); //$NON-NLS-1$
		}		
	}
}
