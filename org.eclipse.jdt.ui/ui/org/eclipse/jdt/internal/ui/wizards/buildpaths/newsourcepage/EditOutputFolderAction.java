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
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.BuildpathDelta;
import org.eclipse.jdt.internal.corext.buildpath.CPJavaProject;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.OutputLocationDialog;

//SelectedElements iff enabled: (IPackageFragmentRoot || IJavaProject || CPListElementAttribute) && size == 1
public class EditOutputFolderAction extends BuildpathModifierAction {

	private final IRunnableContext fContext;
	private boolean fShowOutputFolders;

	public EditOutputFolderAction(final IWorkbenchSite site) {
		this(site, null, PlatformUI.getWorkbench().getProgressService());
		
		fShowOutputFolders= true;
	}
	
	public EditOutputFolderAction(IRunnableContext context, ISetSelectionTarget selectionTarget) {
		this(null, selectionTarget, context);
    }

	private EditOutputFolderAction(IWorkbenchSite site, ISetSelectionTarget selectionTarget, IRunnableContext context) {
		super(site, selectionTarget, BuildpathModifierAction.EDIT_OUTPUT);
		
		fContext= context;
		fShowOutputFolders= false;
		
		setText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_EditOutput_label);
		setImageDescriptor(JavaPluginImages.DESC_ELCL_CONFIGURE_OUTPUT_FOLDER);
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_EditOutput_tooltip);
		setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_CONFIGURE_OUTPUT_FOLDER);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDetailedDescription() {
	    return NewWizardMessages.PackageExplorerActionGroup_FormText_EditOutputFolder;
	}
	

	public void showOutputFolders(boolean showOutputFolders) {
		fShowOutputFolders= showOutputFolders;
    }

	/**
	 * {@inheritDoc}
	 */
	public void run() {
		try {

			final Shell shell= getShell();
			
			final IJavaProject javaProject;
			CPListElement cpElement= null;
			Object firstElement= getSelectedElements().get(0);
			if (firstElement instanceof IJavaProject) {
				javaProject= (IJavaProject)firstElement;
				
				final IClasspathEntry entry= ClasspathModifier.getClasspathEntryFor(javaProject.getPath(), javaProject, IClasspathEntry.CPE_SOURCE);
				cpElement= CPListElement.createFromExisting(entry, javaProject);
			} else if (firstElement instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot)firstElement;
				
				javaProject= root.getJavaProject();
								
				final IClasspathEntry entry= ClasspathModifier.getClasspathEntryFor(root.getPath(), javaProject, IClasspathEntry.CPE_SOURCE);
				cpElement= CPListElement.createFromExisting(entry, javaProject);
			} else if (firstElement instanceof CPListElementAttribute) {
				CPListElementAttribute attribute= (CPListElementAttribute)firstElement;

				cpElement= attribute.getParent();
				javaProject= cpElement.getJavaProject();
			} else {
				return;
			}

			final List classpathEntries= ClasspathModifier.getExistingEntries(javaProject);
			final CPListElement element= ClasspathModifier.getClasspathEntry(classpathEntries, cpElement);

			final OutputLocationDialog dialog= new OutputLocationDialog(shell, element, classpathEntries, javaProject.getOutputLocation(), false);
			if (dialog.open() != Window.OK)
				return;

			try {
				final IRunnableWithProgress runnable= new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							try {
                            	monitor.beginTask(NewWizardMessages.EditOutputFolderAction_ProgressMonitorDescription, 2);
                            	
                            	CPJavaProject cpProject= CPJavaProject.createFromExisting(javaProject);
                            	BuildpathDelta delta= ClasspathModifier.setOutputLocation(cpProject.getCPElement(element), dialog.getOutputLocation(), false, cpProject, new SubProgressMonitor(monitor, 1));
                            	ClasspathModifier.commitClassPath(cpProject, new SubProgressMonitor(monitor, 1));
                            	informListeners(delta);
                            	selectAndReveal(new StructuredSelection(JavaCore.create(element.getResource())));
                            } finally {
                            	monitor.done();
                            }
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
					}
				};
				fContext.run(false, false, runnable);
			} catch (final InvocationTargetException e) {
				if (e.getCause() instanceof CoreException) {
					showExceptionDialog((CoreException)e.getCause(), NewWizardMessages.EditOutputFolderAction_ErrorDescription);
				} else {
					JavaPlugin.log(e);
				}
			} catch (final InterruptedException e) {
			}
			
		} catch (final CoreException e) {
			showExceptionDialog(e, NewWizardMessages.EditOutputFolderAction_ErrorDescription);
		}
	}

	protected boolean canHandle(final IStructuredSelection elements) {
		if (!fShowOutputFolders)
			return false;
		
		if (elements.size() != 1)
			return false;

		final Object element= elements.getFirstElement();
		try {
			if (element instanceof IPackageFragmentRoot) {
				final IPackageFragmentRoot root= (IPackageFragmentRoot)element;
				if (root.getKind() != IPackageFragmentRoot.K_SOURCE)
					return false;
					
				IJavaProject javaProject= root.getJavaProject();
				if (javaProject == null)
					return false;
				
				final IClasspathEntry entry= ClasspathModifier.getClasspathEntryFor(root.getPath(), javaProject, IClasspathEntry.CPE_SOURCE);
				if (entry == null)
					return false;
				
				return true;
			} else if (element instanceof IJavaProject) {
				IJavaProject project= (IJavaProject)element;
				if (!(ClasspathModifier.isSourceFolder(project)))
					return false;
				
				return true;
			} else if (element instanceof CPListElementAttribute) {
				CPListElementAttribute attribute= (CPListElementAttribute)element;
				if (attribute.getKey() != CPListElement.OUTPUT)
					return false;
				
				return true;
			}

		} catch (final JavaModelException e) {
			return false;
		}
		return false;
	}
}