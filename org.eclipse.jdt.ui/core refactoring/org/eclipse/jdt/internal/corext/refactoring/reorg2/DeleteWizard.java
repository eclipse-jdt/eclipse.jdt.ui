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
package org.eclipse.jdt.internal.corext.refactoring.reorg2;

import java.text.MessageFormat;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.refactoring.*;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

public class DeleteWizard extends RefactoringWizard{
	
	public DeleteWizard(DeleteRefactoring2 ref) {
		super(ref, "Confirm Delete");
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
		if (refactoring instanceof DeleteRefactoring2) {
			DeleteRefactoring2 dr= (DeleteRefactoring2)refactoring;
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

		private DeleteRefactoring2 getDeleteRefactoring(){
			return (DeleteRefactoring2)getRefactoring();
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
				return "Internal error. See log for details.";
			}
		}

		private String getNameOfSingleSelectedElement() throws JavaModelException{
			if (getSingleSelectedResource() != null)
				return ReorgUtils2.getName(getSingleSelectedResource());
			else
				return ReorgUtils2.getName(getSingleSelectedJavaElement());
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
					return "Are you sure you want to delete linked resource ''{0}''?\nOnly the workspace link will be deleted. Link target will remain unchanged.";
	
				if (! isLinkedResource(element))
					return "Are you sure you want to delete {0}?";
		
				if (! isLinkedPackageOrPackageFragmentRoot(element))	
					return "Are you sure you want to delete linked resource ''{0}''?\nOnly the workspace link will be deleted. Link target will remain unchanged.";

				//XXX workaround for jcore bugs - linked packages or source folders cannot be deleted properly		
				return "Are you sure you want to delete linked resource ''{0}''?\nOnly the workspace link will be deleted. Link target will remain unchanged.\n\nNote that all subelements of the selected linked packages and package fragment roots will be removed from the workspace as well.";
			} else {
				if (isLinked(getSelectedResources()[0]))//checked before that this will work
					return "Are you sure you want to delete linked resource ''{0}''?\nOnly the workspace link will be deleted. Link target will remain unchanged.";				
				else
					return "Are you sure you want to delete {0}?";
			}
		}

		private String createConfirmationStringForManyElements() throws JavaModelException {
			IResource[] resources= getSelectedResources();
			IJavaElement[] javaElements= getSelectedJavaElements();
			if (! containsLinkedResources(resources, javaElements))
				return "Are you sure you want to delete these {0} elements?";

			if (! containsLinkedPackagesOrPackageFragmentRoots(javaElements))	
				return "Are you sure you want to delete these {0} elements?\n\nSelection contains linked resources.\nOnly the workspace links will be deleted. Link targets will remain unchanged.";

			//XXX workaround for jcore bugs - linked packages or source folders cannot be deleted properly
			return "Are you sure you want to delete these {0} elements?\n\nSelection contains linked packages.\nOnly the workspace links will be deleted. Link targets will remain unchanged.\n\nNote that all subelements of linked packages and package fragment roots will be removed from the workspace as well.";
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
			return isLinked(ReorgUtils2.getResource(element));
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
