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
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.OutputLocationDialog;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.OutputFolderQuery;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.OutputFolderValidator;

//SelectedElements iff enabled: (IPackageFragmentRoot || IJavaProject || CPListElementAttribute) && size == 1
public class EditOutputFolderAction extends BuildpathModifierAction {

	private final IRunnableContext fContext;
	private final IClasspathModifierListener fListener;
	private boolean fShowOutputFolders;

	public EditOutputFolderAction(final IWorkbenchSite site) {
		this(site, PlatformUI.getWorkbench().getProgressService(), null);
		
		fShowOutputFolders= true;
	}

	public EditOutputFolderAction(IWorkbenchSite site, IRunnableContext context, IClasspathModifierListener listener) {
		super(site, BuildpathModifierAction.EDIT_OUTPUT);
		
		fContext= context;
		fListener= listener;
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
			final int index= classpathEntries.indexOf(element);

			final OutputLocationDialog dialog= new OutputLocationDialog(shell, element, classpathEntries);
			if (dialog.open() != Window.OK)
				return;

			classpathEntries.add(index, element);

			final boolean removeProjectFromClasspath;
			final IPath defaultOutputLocation= javaProject.getOutputLocation().makeRelative();
			final IPath newDefaultOutputLocation;
			if (defaultOutputLocation.segmentCount() == 1) {
				//Project folder is output location
				final OutputFolderValidator outputFolderValidator= new OutputFolderValidator(null, javaProject) {
					public boolean validate(IPath outputLocation) {
						return true;
					}
				};
				final OutputFolderQuery outputFolderQuery= ClasspathModifierQueries.getDefaultFolderQuery(shell, defaultOutputLocation);
				if (outputFolderQuery.doQuery(true, outputFolderValidator, javaProject)) {
					newDefaultOutputLocation= outputFolderQuery.getOutputLocation();
					removeProjectFromClasspath= outputFolderQuery.removeProjectFromClasspath();
				} else {
					return;
				}
			} else {
				removeProjectFromClasspath= false;
				newDefaultOutputLocation= defaultOutputLocation;
			}
			
			try {
				final IRunnableWithProgress runnable= new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							setOutputLocation(element, dialog.getOutputLocation(), classpathEntries, newDefaultOutputLocation, removeProjectFromClasspath, javaProject, monitor);
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

	private void setOutputLocation(final CPListElement entry, final IPath outputLocation, final List existingEntries, final IPath defaultOutputLocation, final boolean removeProjectFromClasspath, final IJavaProject javaProject, final IProgressMonitor monitor) throws CoreException, InterruptedException {
		try {
			monitor.beginTask(NewWizardMessages.EditOutputFolderAction_ProgressMonitorDescription, 4);
			if (!defaultOutputLocation.equals(javaProject.getOutputLocation().makeRelative())) {
				javaProject.setOutputLocation(defaultOutputLocation, new SubProgressMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}
			
			if (removeProjectFromClasspath) {
				ClasspathModifier.removeFromClasspath(javaProject, existingEntries, new SubProgressMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}
	
			if (outputLocation != null) {
				ClasspathModifier.exclude(outputLocation, existingEntries, new ArrayList(), javaProject, new SubProgressMonitor(monitor, 1));
				entry.setAttribute(CPListElement.OUTPUT, outputLocation);
			} else {
				monitor.worked(1);
			}
			
			ClasspathModifier.commitClassPath(existingEntries, javaProject, fListener, new SubProgressMonitor(monitor, 1));
		} finally {
			monitor.done();
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