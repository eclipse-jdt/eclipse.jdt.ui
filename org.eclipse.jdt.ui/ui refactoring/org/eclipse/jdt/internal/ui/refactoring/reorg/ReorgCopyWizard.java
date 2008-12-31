/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.core.resources.IResource;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgDestinationValidator;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaCopyProcessor;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgDestinationFactory;


public class ReorgCopyWizard extends RefactoringWizard {

	private final JavaCopyProcessor fProcessor;

	public ReorgCopyWizard(JavaCopyProcessor processor, Refactoring ref) {
		super(ref, DIALOG_BASED_USER_INTERFACE | NO_PREVIEW_PAGE);
		fProcessor= processor;
		setDefaultPageTitle(ReorgMessages.ReorgCopyWizard_1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	protected void addUserInputPages() {
		addPage(new CopyInputPage(fProcessor));
	}

	private static class CopyInputPage extends ReorgUserInputPage{

		private static final String PAGE_NAME= "CopyInputPage"; //$NON-NLS-1$

		private final JavaCopyProcessor fProcessor;

		public CopyInputPage(JavaCopyProcessor processor) {
			super(PAGE_NAME);
			fProcessor= processor;
		}

		private JavaCopyProcessor getCopyProcessor(){
			return fProcessor;
		}

		protected Object getInitiallySelectedElement() {
			return getCopyProcessor().getCommonParentForInputElements();
		}

		protected IJavaElement[] getJavaElements() {
			return getCopyProcessor().getJavaElements();
		}

		protected IResource[] getResources() {
			return getCopyProcessor().getResources();
		}

		protected IReorgDestinationValidator getDestinationValidator() {
			return getCopyProcessor();
		}

		protected RefactoringStatus verifyDestination(Object selected) throws JavaModelException{
			return getCopyProcessor().setDestination(ReorgDestinationFactory.createDestination(selected));
		}
	}
}
