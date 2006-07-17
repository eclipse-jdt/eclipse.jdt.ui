/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

//SelectedElements iff enabled: IPackageFragment || ICompilationUnit
public class ExcludeFromBuildpathAction extends BuildpathModifierAction {
 
	private final IRunnableContext fContext;
	private final IClasspathModifierListener fListener;
	
	public ExcludeFromBuildpathAction(IWorkbenchSite site) {
		this(site, PlatformUI.getWorkbench().getProgressService(), null);
	}
	
	public ExcludeFromBuildpathAction(IWorkbenchSite site, IRunnableContext context, IClasspathModifierListener listener) {
		super(site);
		
		fContext= context;
		fListener= listener;
		
		setText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Exclude_label);
		setImageDescriptor(JavaPluginImages.DESC_ELCL_EXCLUDE_FROM_BUILDPATH);
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Exclude_tooltip);
		setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_EXCLUDE_FROM_BUILDPATH);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void run() {
        final IJavaProject project;
        Object object= getSelectedElements().get(0);
        if (object instanceof ICompilationUnit) {
        	project= ((ICompilationUnit)object).getJavaProject();
        } else {
        	project= ((IPackageFragment)object).getJavaProject();
        }
        
        try {
			final IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
				        List result= exclude(getSelectedElements(), project, monitor);
				        selectAndReveal(new StructuredSelection(result));
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			fContext.run(false, false, runnable);
		} catch (final InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				showExceptionDialog((CoreException)e.getCause(), NewWizardMessages.ExcludeFromBuildathAction_ErrorTitle);
			} else {
				JavaPlugin.log(e);
			}
		} catch (final InterruptedException e) {
		}
	}
	
	private List exclude(List javaElements, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_Excluding, javaElements.size() + 4); 

			List existingEntries= ClasspathModifier.getExistingEntries(project);
			List resources= new ArrayList();
			for (int i= 0; i < javaElements.size(); i++) {
				IJavaElement javaElement= (IJavaElement) javaElements.get(i);
				IPackageFragmentRoot root= (IPackageFragmentRoot) javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				CPListElement entry= ClasspathModifier.getClasspathEntry(existingEntries, root);

				IResource resource= ClasspathModifier.exclude(javaElement, entry, project, new SubProgressMonitor(monitor, 1));
				if (resource != null) {
					resources.add(resource);
				}
			}

			ClasspathModifier.commitClassPath(existingEntries, project, fListener, new SubProgressMonitor(monitor, 4));
			return resources;
		} finally {
			monitor.done();
		}
	}

	protected boolean canHandle(IStructuredSelection elements) {
        if (elements.size() == 0)
            return false;
        try {
	        for (Iterator iter= elements.iterator(); iter.hasNext();) {
				Object element= iter.next();
				if (element instanceof IPackageFragment) {
					int type= DialogPackageExplorerActionGroup.getType(element, ((IPackageFragment)element).getJavaProject());
					if (type != DialogPackageExplorerActionGroup.INCLUDED_FOLDER && type != DialogPackageExplorerActionGroup.PACKAGE_FRAGMENT)
						return false;
				} else if (element instanceof ICompilationUnit) {
				} else {
					return false;
				}	
			}
			return true;
        } catch (CoreException e) {
        }
    	return false;
	}

}
