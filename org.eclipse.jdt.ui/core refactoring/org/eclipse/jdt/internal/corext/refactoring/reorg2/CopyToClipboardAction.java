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

import java.util.List;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;


public class CopyToClipboardAction extends SelectionDispatchAction{

	public CopyToClipboardAction(IWorkbenchSite site) {
		super(site);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		try {
			List elements= selection.toList();
			IResource[] resources= ReorgUtils2.getResources(elements);
			IJavaElement[] javaElements= ReorgUtils2.getJavaElements(elements);
			if (elements.size() != resources.length + javaElements.length)
				setEnabled(false);
			else
				setEnabled(canEnable(resources, javaElements));
		} catch (JavaModelException e) {
			//no ui here - this happens on selection changes
			setEnabled(false);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#run(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void run(IStructuredSelection selection) {
		// TODO implement me
		super.run(selection);
	}

	private boolean canEnable(IResource[] resources, IJavaElement[] javaElements) throws JavaModelException {
		return new CopyToClipboardEnablementPolicy(resources, javaElements).canEnable();
	}
	
	private static class CopyToClipboardEnablementPolicy implements IReorgEnablementPolicy{
		private final IResource[] fResources;
		private final IJavaElement[] fJavaElements;
		CopyToClipboardEnablementPolicy(IResource[] resources, IJavaElement[] javaElements){
			Assert.isNotNull(resources);
			Assert.isNotNull(javaElements);
			fResources= resources;
			fJavaElements= javaElements;
		}

		public boolean canEnable() throws JavaModelException{
			if (fResources.length + fJavaElements.length == 0)
				return false;
			if (hasProjects() && hasNonProjects())
				return false;
			if (! canCopyAllToClipboard())
				return false;
			if (! new ParentChecker(fResources, fJavaElements).haveCommonParent())
				return false;
			return true;
		}

		private boolean canCopyAllToClipboard() throws JavaModelException {
			for (int i= 0; i < fResources.length; i++) {
				if (! canCopyToClipboard(fResources[i])) return false;
			}
			for (int i= 0; i < fJavaElements.length; i++) {
				if (! canCopyToClipboard(fJavaElements[i])) return false;
			}
			return true;
		}

		private static boolean canCopyToClipboard(IJavaElement element) throws JavaModelException {
			if (element == null)
				return false;
				
			if (! element.exists())
				return false;
				
			if (element instanceof IJavaModel)
				return false;
				
			if (JavaElementUtil.isDefaultPackage(element))		
				return false;
			
			if (element instanceof IMember && ! ReorgUtils2.hasSourceAvailable((IMember)element))
				return false;
			
			if (element instanceof IMember){
				/* feature in jdt core - initializers from class files are not binary but have no cus
				 * see bug 37199
				 * we just say 'no' to them
				 */
				IMember member= (IMember)element;
				if (! member.isBinary() && ReorgUtils2.getCompilationUnit(member) == null)
					return false;
			}
			
			if (ReorgUtils2.isDeletedFromEditor(element))
				return false;

			return true;
		}

		private static boolean canCopyToClipboard(IResource resource) {
			return 	resource != null && 
					resource.exists() &&
					! resource.isPhantom() &&
					resource.getType() != IResource.ROOT;
		}

		private boolean hasProjects() {
			for (int i= 0; i < fResources.length; i++) {
				if (ReorgUtils2.isProject(fResources[i])) return true;
			}
			for (int i= 0; i < fJavaElements.length; i++) {
				if (ReorgUtils2.isProject(fJavaElements[i])) return true;
			}
			return false;
		}

		private boolean hasNonProjects() {
			for (int i= 0; i < fResources.length; i++) {
				if (! ReorgUtils2.isProject(fResources[i])) return true;
			}
			for (int i= 0; i < fJavaElements.length; i++) {
				if (! ReorgUtils2.isProject(fJavaElements[i])) return true;
			}
			return false;
		}
	}
}
