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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.text.MessageFormat;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.MessageInputPage;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

public class DeleteWizard extends RefactoringWizard{
	
	public DeleteWizard(DeleteRefactoring ref) {
		super(ref, RefactoringMessages.getString("DeleteWizard.1")); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard#addUserInputPages()
	 */
	protected void addUserInputPages(){
		addPage(new DeleteInputPage());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard#getMessageLineWidthInChars()
	 */
	protected int getMessageLineWidthInChars() {
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard#hasPreviewPage()
	 */
	public boolean hasPreviewPage() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard#yesNoStyle()
	 */
	protected boolean yesNoStyle() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#needsProgressMonitor()
	 */
	public boolean needsProgressMonitor() {
		Refactoring refactoring= getRefactoring();
		if (refactoring instanceof DeleteRefactoring) {
			DeleteRefactoring dr= (DeleteRefactoring)refactoring;
			IResource[] resources= dr.getResourcesToDelete();
			if (resources != null && resources.length > 0)
				return true;
			IJavaElement[] jElements= dr.getJavaElementsToDelete();
			if (jElements != null) {
				for (int i= 0; i < jElements.length; i++) {
					int type= jElements[i].getElementType();
					if (type <= IJavaElement.CLASS_FILE)
						return true;
				}
			}
			
		}
		return false;
	}
	
	private static class DeleteInputPage extends MessageInputPage{
		private static final String PAGE_NAME= "DeleteInputPage"; //$NON-NLS-1$

		public DeleteInputPage() {
			super(PAGE_NAME, true, MessageInputPage.STYLE_QUESTION);
		}

		private DeleteRefactoring getDeleteRefactoring(){
			return (DeleteRefactoring)getRefactoring();
		}
		
		protected String getMessageString() {
			try {
				if (1 == numberOfSelectedElements()){
					String pattern= createConfirmationStringForOneElement();
					String name= getNameOfSingleSelectedElement();
					return MessageFormat.format(pattern, new String[]{name});
				} else {
					String pattern= createConfirmationStringForManyElements();
					return MessageFormat.format(pattern, new String[]{String.valueOf(numberOfSelectedElements())});
				}
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
				setPageComplete(false);
				return RefactoringMessages.getString("DeleteWizard.2"); //$NON-NLS-1$
			}
		}

		private String getNameOfSingleSelectedElement() throws JavaModelException{
			if (getSingleSelectedResource() != null)
				return ReorgUtils.getName(getSingleSelectedResource());
			else
				return ReorgUtils.getName(getSingleSelectedJavaElement());
		}

		private IJavaElement getSingleSelectedJavaElement() {
			IJavaElement[] elements= getSelectedJavaElements();
			return elements.length == 1 ? elements[0]: null;
		}

		private IResource getSingleSelectedResource() {
			IResource[] resources= getSelectedResources();
			return resources.length == 1 ? resources[0]: null;
		}

		private int numberOfSelectedElements() {
			return getSelectedJavaElements().length + getSelectedResources().length;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.refactoring.RefactoringWizardPage#performFinish()
		 */
		protected boolean performFinish() {
			return super.performFinish() || getDeleteRefactoring().wasCanceled(); //close the dialog if canceled
		}

		private String createConfirmationStringForOneElement() throws JavaModelException {
			IJavaElement[] elements= getSelectedJavaElements();
			if (elements.length == 1){
				IJavaElement element= elements[0];
				if (isDefaultPackageWithLinkedFiles(element))	
					return RefactoringMessages.getString("DeleteWizard.3"); //$NON-NLS-1$
	
				if (! isLinkedResource(element))
					return RefactoringMessages.getString("DeleteWizard.4"); //$NON-NLS-1$
		
				if (! isLinkedPackageOrPackageFragmentRoot(element))	
					return RefactoringMessages.getString("DeleteWizard.5"); //$NON-NLS-1$

				//XXX workaround for jcore bugs - linked packages or source folders cannot be deleted properly		
				return RefactoringMessages.getString("DeleteWizard.6"); //$NON-NLS-1$
			} else {
				if (isLinked(getSelectedResources()[0]))//checked before that this will work
					return RefactoringMessages.getString("DeleteWizard.7");				 //$NON-NLS-1$
				else
					return RefactoringMessages.getString("DeleteWizard.8"); //$NON-NLS-1$
			}
		}

		private String createConfirmationStringForManyElements() throws JavaModelException {
			IResource[] resources= getSelectedResources();
			IJavaElement[] javaElements= getSelectedJavaElements();
			if (! containsLinkedResources(resources, javaElements))
				return RefactoringMessages.getString("DeleteWizard.9"); //$NON-NLS-1$

			if (! containsLinkedPackagesOrPackageFragmentRoots(javaElements))	
				return RefactoringMessages.getString("DeleteWizard.10"); //$NON-NLS-1$

			//XXX workaround for jcore bugs - linked packages or source folders cannot be deleted properly
			return RefactoringMessages.getString("DeleteWizard.11"); //$NON-NLS-1$
		}

		private static boolean isLinkedPackageOrPackageFragmentRoot(IJavaElement element) {
			if ((element instanceof IPackageFragment) || (element instanceof IPackageFragmentRoot))
				return isLinkedResource(element);
			else
				return false;	
		}
		
		private static boolean containsLinkedPackagesOrPackageFragmentRoots(IJavaElement[] javaElements) {
			for (int i= 0; i < javaElements.length; i++) {
				IJavaElement element= javaElements[i];
				if (isLinkedPackageOrPackageFragmentRoot(element))
					return true;				
			}
			return false;
		}

		private static boolean containsLinkedResources(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
			for (int i= 0; i < javaElements.length; i++) {
				IJavaElement element= javaElements[i];
				if (isLinkedResource(element))
					return true;
				if (isDefaultPackageWithLinkedFiles(element))
					return true;
			}
			for (int i= 0; i < resources.length; i++) {
				IResource resource= resources[i];
				if (isLinked(resource))
					return true;
			}
			return false;
		}

		private static boolean isDefaultPackageWithLinkedFiles(Object firstElement) throws JavaModelException {
			if (! JavaElementUtil.isDefaultPackage(firstElement))
				return false;
			IPackageFragment defaultPackage= (IPackageFragment)firstElement;
			ICompilationUnit[] cus= defaultPackage.getCompilationUnits();
			for (int i= 0; i < cus.length; i++) {
				if (isLinkedResource(cus[i]))
					return true;
			}
			return false;
		}

		private static boolean isLinkedResource(IJavaElement element) {
			return isLinked(ReorgUtils.getResource(element));
		}
		
		private static boolean isLinked(IResource resource){
			return resource != null && resource.isLinked();
		}	
		
		private IJavaElement[] getSelectedJavaElements(){
			return getDeleteRefactoring().getJavaElementsToDelete();
		}

		private IResource[] getSelectedResources(){
			return getDeleteRefactoring().getResourcesToDelete();
		}
	}
}
