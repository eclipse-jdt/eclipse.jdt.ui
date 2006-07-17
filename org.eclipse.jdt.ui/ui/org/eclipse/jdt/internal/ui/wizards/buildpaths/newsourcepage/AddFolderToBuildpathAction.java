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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathBasePage;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.OutputFolderQuery;

public class AddFolderToBuildpathAction extends Action implements ISelectionChangedListener {

	private final IWorkbenchSite fSite;
	private final List fSelectedElements; //IJavaProject || IPackageFrament || IFolder
	private final IRunnableContext fContext;
	private final IClasspathModifierListener fListener;

	public AddFolderToBuildpathAction(IWorkbenchSite site) {
		this(site, PlatformUI.getWorkbench().getProgressService(), null);
	}
	
	protected AddFolderToBuildpathAction(IWorkbenchSite site, IRunnableContext context, IClasspathModifierListener listener) {
		super(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelSFToCP_label, JavaPluginImages.DESC_ELCL_ADD_AS_SOURCE_FOLDER);
		
		fSite= site;
		fContext= context;
		fListener= listener;
		fSelectedElements= new ArrayList();
		
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelSFToCP_tooltip);
	}

	/**
	 * {@inheritDoc}
	 */
	public void run() {

		try {
			final IJavaProject project;
			Object object= fSelectedElements.get(0);
			if (object instanceof IJavaProject) {
				project= (IJavaProject)object;
			} else if (object instanceof IPackageFragment) {
				project= ((IPackageFragment)object).getJavaProject();
			} else {
				IFolder folder= (IFolder)object;
				project= JavaCore.create(folder.getProject());
				if (project == null)
					return;
			}

			final Shell shell= getShell();

			final boolean removeProjectFromClasspath;
			IPath outputLocation= project.getOutputLocation();
			final IPath defaultOutputLocation= outputLocation.makeRelative();
			final IPath newDefaultOutputLocation;
			final boolean removeOldClassFiles;
			IPath projPath= project.getProject().getFullPath();
			if (!(fSelectedElements.size() == 1 && fSelectedElements.get(0) instanceof IJavaProject) && //if only the project should be added, then the query does not need to be executed 
					(outputLocation.equals(projPath) || defaultOutputLocation.segmentCount() == 1)) {


				final OutputFolderQuery outputFolderQuery= ClasspathModifierQueries.getDefaultFolderQuery(shell, defaultOutputLocation);
				if (outputFolderQuery.doQuery(true, ClasspathModifier.getValidator(fSelectedElements, project), project)) {
					newDefaultOutputLocation= outputFolderQuery.getOutputLocation();
					removeProjectFromClasspath= outputFolderQuery.removeProjectFromClasspath();

					if (BuildPathsBlock.hasClassfiles(project.getProject()) && outputLocation.equals(projPath)) {
						String title= NewWizardMessages.BuildPathsBlock_RemoveBinariesDialog_title; 
						String message= Messages.format(NewWizardMessages.BuildPathsBlock_RemoveBinariesDialog_description, projPath.toString()); 
						MessageDialog dialog= new MessageDialog(shell, title, null, message, MessageDialog.QUESTION, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
						int answer= dialog.open();
						if (answer == 0) {
							removeOldClassFiles= true;
						} else if (answer == 1) {
							removeOldClassFiles= false;
						} else {
							return;
						}
					} else {
						removeOldClassFiles= false;
					}
				} else {
					return;
				}
			} else {
				removeProjectFromClasspath= false;
				removeOldClassFiles= false;
				newDefaultOutputLocation= defaultOutputLocation;
			}

			try {
				final IRunnableWithProgress runnable= new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							List result= addToClasspath(fSelectedElements, project, newDefaultOutputLocation.makeAbsolute(), removeProjectFromClasspath, removeOldClassFiles, monitor);
							selectAndReveal(new StructuredSelection(result));
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
					}
				};
				fContext.run(false, false, runnable);
			} catch (final InvocationTargetException e) {
				if (e.getCause() instanceof CoreException) {
					showExceptionDialog((CoreException)e.getCause());
				} else {
					JavaPlugin.log(e);
				}
			} catch (final InterruptedException e) {
			}
		} catch (CoreException e) {
			showExceptionDialog(e);
		}
	}

	private Shell getShell() {
		if (fSite == null)
			return JavaPlugin.getActiveWorkbenchShell();
		
	    return fSite.getShell() != null ? fSite.getShell() : JavaPlugin.getActiveWorkbenchShell();
    }

	private List addToClasspath(List elements, IJavaProject project, IPath outputLocation, boolean removeProjectFromClasspath, boolean removeOldClassFiles, IProgressMonitor monitor) throws OperationCanceledException, CoreException {
		if (!project.getProject().hasNature(JavaCore.NATURE_ID)) {
			StatusInfo rootStatus= new StatusInfo();
			rootStatus.setError(NewWizardMessages.ClasspathModifier_Error_NoNatures); 
			throw new CoreException(rootStatus);
		}
		
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_AddToBuildpath, elements.size() + 4); 
			IWorkspaceRoot workspaceRoot= JavaPlugin.getWorkspace().getRoot();
			
			if (removeOldClassFiles) {
				IResource res= workspaceRoot.findMember(project.getOutputLocation());
				if (res instanceof IContainer && BuildPathsBlock.hasClassfiles(res)) {
					BuildPathsBlock.removeOldClassfiles(res);
				}
			}

			if (!project.getOutputLocation().equals(outputLocation)) {
				project.setOutputLocation(outputLocation, new SubProgressMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}

			List existingEntries= ClasspathModifier.getExistingEntries(project);
			if (removeProjectFromClasspath) {
				ClasspathModifier.removeFromClasspath(project, existingEntries, new SubProgressMonitor(monitor, 1));
			} else {
				monitor.worked(1);
			}

			List newEntries= new ArrayList();
			for (int i= 0; i < elements.size(); i++) {
				Object element= elements.get(i);
				CPListElement entry;
				if (element instanceof IResource)
					entry= ClasspathModifier.addToClasspath((IResource) element, existingEntries, newEntries, project, new SubProgressMonitor(monitor, 1));
				else
					entry= ClasspathModifier.addToClasspath((IJavaElement) element, existingEntries, newEntries, project, new SubProgressMonitor(monitor, 1));
				newEntries.add(entry);
			}

			Set modifiedSourceEntries= new HashSet();
			BuildPathBasePage.fixNestingConflicts((CPListElement[])newEntries.toArray(new CPListElement[newEntries.size()]), (CPListElement[])existingEntries.toArray(new CPListElement[existingEntries.size()]), modifiedSourceEntries);

			ClasspathModifier.setNewEntry(existingEntries, newEntries, project, new SubProgressMonitor(monitor, 1));

			ClasspathModifier.commitClassPath(existingEntries, project, fListener, new SubProgressMonitor(monitor, 1));

			List result= new ArrayList();
			for (int i= 0; i < newEntries.size(); i++) {
				IClasspathEntry entry= ((CPListElement) newEntries.get(i)).getClasspathEntry();
				IJavaElement root;
				if (entry.getPath().equals(project.getPath()))
					root= project;
				else
					root= project.findPackageFragmentRoot(entry.getPath());
				if (root != null) {
					result.add(root);
				}
			}

			return result;
		} finally {
			monitor.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void selectionChanged(final SelectionChangedEvent event) {
		final ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection) {
			setEnabled(canHandle((IStructuredSelection) selection));
		} else {
			setEnabled(canHandle(StructuredSelection.EMPTY));
		}
	}

	private boolean canHandle(IStructuredSelection elements) {
		if (elements.size() == 0)
			return false;
		try {
			fSelectedElements.clear();
			for (Iterator iter= elements.iterator(); iter.hasNext();) {
				Object element= iter.next();
				fSelectedElements.add(element);
				if (element instanceof IJavaProject) {
					if (ClasspathModifier.isSourceFolder((IJavaProject)element))
						return false;
				} else if (element instanceof IPackageFragment) {
					int type= DialogPackageExplorerActionGroup.getType(element, ((IPackageFragment)element).getJavaProject());
					if (type != DialogPackageExplorerActionGroup.PACKAGE_FRAGMENT && type != DialogPackageExplorerActionGroup.INCLUDED_FOLDER)
						return false;
				} else if (element instanceof IFolder) {
					IProject project= ((IFolder)element).getProject();
					IJavaProject javaProject= JavaCore.create(project);
					if (javaProject == null || !javaProject.exists())
						return false;
				} else {
					return false;
				}
			}
			return true;
		} catch (CoreException e) {
		}
		return false;
	}

	private void showExceptionDialog(CoreException exception) {
		showError(exception, getShell(), NewWizardMessages.AddSourceFolderToBuildpathAction_ErrorTitle, exception.getMessage());
	}

	private void showError(CoreException e, Shell shell, String title, String message) {
		IStatus status= e.getStatus();
		if (status != null) {
			ErrorDialog.openError(shell, message, title, status);
		} else {
			MessageDialog.openError(shell, title, message);
		}
	}
	
	protected void selectAndReveal(final ISelection selection) {
		// validate the input
		IWorkbenchPage page= fSite.getPage();
		if (page == null)
			return;

		// get all the view and editor parts
		List parts= new ArrayList();
		IWorkbenchPartReference refs[]= page.getViewReferences();
		for (int i= 0; i < refs.length; i++) {
			IWorkbenchPart part= refs[i].getPart(false);
			if (part != null)
				parts.add(part);
		}
		refs= page.getEditorReferences();
		for (int i= 0; i < refs.length; i++) {
			if (refs[i].getPart(false) != null)
				parts.add(refs[i].getPart(false));
		}

		Iterator itr= parts.iterator();
		while (itr.hasNext()) {
			IWorkbenchPart part= (IWorkbenchPart) itr.next();

			// get the part's ISetSelectionTarget implementation
			ISetSelectionTarget target= null;
			if (part instanceof ISetSelectionTarget)
				target= (ISetSelectionTarget) part;
			else
				target= (ISetSelectionTarget) part.getAdapter(ISetSelectionTarget.class);

			if (target != null) {
				// select and reveal resource
				final ISetSelectionTarget finalTarget= target;
				page.getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						finalTarget.selectReveal(selection);
					}
				});
			}
		}
	}
	
	protected List getSelectedElements() {
		return fSelectedElements;
	}
}
