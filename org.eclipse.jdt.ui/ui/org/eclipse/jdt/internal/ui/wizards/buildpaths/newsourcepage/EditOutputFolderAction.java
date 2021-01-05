/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
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

	@Override
	public String getDetailedDescription() {
	    return NewWizardMessages.PackageExplorerActionGroup_FormText_EditOutputFolder;
	}


	public void showOutputFolders(boolean showOutputFolders) {
		fShowOutputFolders= showOutputFolders;
    }

	@Override
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

			final List<CPListElement> classpathEntries= ClasspathModifier.getExistingEntries(javaProject);
			final CPListElement element= ClasspathModifier.getClasspathEntry(classpathEntries, cpElement);

			final OutputLocationDialog dialog= new OutputLocationDialog(shell, element, classpathEntries, javaProject.getOutputLocation(), false);
			if (dialog.open() != Window.OK)
				return;

			final CPJavaProject cpProject= CPJavaProject.createFromExisting(javaProject);
        	final BuildpathDelta delta= ClasspathModifier.setOutputLocation(cpProject.getCPElement(element), dialog.getOutputLocation(), false, cpProject);

        	IFolder oldOutputFolder= getOldOutputFolder(delta);
        	final IFolder folderToDelete;
        	if (oldOutputFolder != null) {
        		String message= Messages.format(NewWizardMessages.EditOutputFolderAction_DeleteOldOutputFolderQuestion, BasicElementLabels.getPathLabel(oldOutputFolder.getFullPath(), false));
    	    	if (MessageDialog.openQuestion(getShell(), NewWizardMessages.OutputLocationDialog_title, message)) {
    	    		folderToDelete= oldOutputFolder;
    	    	} else {
    	    		folderToDelete= null;
    	    	}
        	} else {
        		folderToDelete= null;
        	}

			try {
				final IRunnableWithProgress runnable= monitor -> {
					try {
				    	monitor.beginTask(NewWizardMessages.EditOutputFolderAction_ProgressMonitorDescription, 50 + (folderToDelete == null?0:10));

				    	ClasspathModifier.commitClassPath(cpProject, new SubProgressMonitor(monitor, 50));
				    	if (folderToDelete != null)
				            folderToDelete.delete(true, new SubProgressMonitor(monitor, 10));

				    	informListeners(delta);
				    	selectAndReveal(new StructuredSelection(JavaCore.create(element.getResource())));
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} finally {
				    	monitor.done();
				    }
				};
				fContext.run(false, false, runnable);
			} catch (final InvocationTargetException e) {
				JavaPlugin.log(e);
			} catch (final InterruptedException e) {
			}

		} catch (final CoreException e) {
			showExceptionDialog(e, NewWizardMessages.EditOutputFolderAction_ErrorDescription);
		}
	}

	private IFolder getOldOutputFolder(final BuildpathDelta delta) {
	    List<IResource> existingFolders= new ArrayList<>();
		for (IResource deletedResource : delta.getDeletedResources()) {
			if (deletedResource instanceof IFolder && deletedResource.exists()) {
				existingFolders.add(deletedResource);
			}
		}
	    if (existingFolders.size() > 0) {
	    	if (existingFolders.size() > 1) {
	    		StringBuilder message= new StringBuilder("Found more then one existing folders:"); //$NON-NLS-1$
	    		for (IResource iResource : existingFolders) {
	                IFolder folder= (IFolder)iResource;
	                message.append("\n").append(folder.toString()); //$NON-NLS-1$
	            }
	    		Assert.isTrue(false, message.toString());
	    	}
	    	return (IFolder)existingFolders.get(0);
	    }
	    return null;
    }

	@Override
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
				if (!CPListElement.OUTPUT.equals(attribute.getKey()))
					return false;

				return true;
			}

		} catch (final JavaModelException e) {
			return false;
		}
		return false;
	}
}