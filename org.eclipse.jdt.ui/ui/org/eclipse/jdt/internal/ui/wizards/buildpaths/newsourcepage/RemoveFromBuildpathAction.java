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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.IRemoveLinkedFolderQuery;

public class RemoveFromBuildpathAction extends Action implements ISelectionChangedListener {

	private final IWorkbenchSite fSite;
	private List fSelectedElements; //IPackageFramgentRoot || IJavaProject || ClassPathContainer iff isEnabled()

	public RemoveFromBuildpathAction(IWorkbenchSite site) {
		super(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_RemoveFromCP_label, JavaPluginImages.DESC_ELCL_REMOVE_FROM_BP);
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_RemoveFromCP_tooltip);
		fSite= site;
		fSelectedElements= new ArrayList();
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
			} else if (object instanceof IPackageFragmentRoot) {
				IPackageFragmentRoot root= (IPackageFragmentRoot)object;
				project= root.getJavaProject();
			} else {
				ClassPathContainer container= (ClassPathContainer)object;
				project= container.getJavaProject();
			}

			final List elementsToRemove= new ArrayList();
			final List foldersToDelete= new ArrayList();
			queryToRemoveLinkedFolders(elementsToRemove, foldersToDelete);
		
			final IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_RemoveFromBuildpath, elementsToRemove.size() + foldersToDelete.size());
						List result= removeFromClasspath(elementsToRemove, project, new SubProgressMonitor(monitor, elementsToRemove.size()));
						result.removeAll(foldersToDelete);
						deleteFolders(foldersToDelete, new SubProgressMonitor(monitor, foldersToDelete.size()));
						if (result.size() == 0)
							result.add(project);
						selectAndReveal(new StructuredSelection(result));
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				}
			};
			PlatformUI.getWorkbench().getProgressService().run(true, false, runnable);
			
		} catch (CoreException e) {
			showExceptionDialog(e);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				showExceptionDialog((CoreException)e.getCause());
			} else {
				JavaPlugin.log(e);
			}
		} catch (InterruptedException e) {
		}
	}

	private void deleteFolders(List folders, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_RemoveFromBuildpath, folders.size()); 

			for (Iterator iter= folders.iterator(); iter.hasNext();) {
				IFolder folder= (IFolder)iter.next();
				folder.delete(true, true, new SubProgressMonitor(monitor, 1));
			}
		} finally {
			monitor.done();
		}
	}

	private List removeFromClasspath(List elements, IJavaProject project, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_RemoveFromBuildpath, elements.size() + 1); 

			List existingEntries= ClasspathModifier.getExistingEntries(project);
			List result= new ArrayList();
			
			for (int i= 0; i < elements.size(); i++) {
				Object element= elements.get(i);

				if (element instanceof IJavaProject) {
					Object res= ClasspathModifier.removeFromClasspath((IJavaProject)element, existingEntries, new SubProgressMonitor(monitor, 1));
					result.add(res);

				} else if (element instanceof IPackageFragmentRoot) {
					Object res= ClasspathModifier.removeFromClasspath((IPackageFragmentRoot)element, existingEntries, project, new SubProgressMonitor(monitor, 1));
					if (res != null)
						result.add(res);
				} else {
					existingEntries.remove(CPListElement.createFromExisting(((ClassPathContainer)element).getClasspathEntry(), project));
				}
			}

			ClasspathModifier.commitClassPath(existingEntries, project, new SubProgressMonitor(monitor, 1));

			return result;
		} finally {
			monitor.done();
		}
	}
	
	private void queryToRemoveLinkedFolders(final List elementsToRemove, final List foldersToDelete) throws JavaModelException {
		final Shell shell= fSite.getShell() != null ? fSite.getShell() : JavaPlugin.getActiveWorkbenchShell();
		for (Iterator iter= fSelectedElements.iterator(); iter.hasNext();) {
			Object element= iter.next();
			if (element instanceof IPackageFragmentRoot) {
				IFolder folder= getLinkedSourceFolder((IPackageFragmentRoot)element);
				if (folder != null) {
					RemoveLinkedFolderDialog dialog= new RemoveLinkedFolderDialog(shell, folder);

					final int result= dialog.open() == Window.OK?dialog.getRemoveStatus():IRemoveLinkedFolderQuery.REMOVE_CANCEL;
					
					if (result != IRemoveLinkedFolderQuery.REMOVE_CANCEL) {
						if (result == IRemoveLinkedFolderQuery.REMOVE_BUILD_PATH) {
							elementsToRemove.add(element);
						} else if (result == IRemoveLinkedFolderQuery.REMOVE_BUILD_PATH_AND_FOLDER) {
							elementsToRemove.add(element);
							foldersToDelete.add(folder);
						}
					}
				} else {
					elementsToRemove.add(element);
				}
			} else {
				elementsToRemove.add(element);
			}
		}
	}

	private IFolder getLinkedSourceFolder(IPackageFragmentRoot root) throws JavaModelException {
		if (root.getKind() != IPackageFragmentRoot.K_SOURCE)
			return null;

		final IResource resource= root.getCorrespondingResource();
		if (!(resource instanceof IFolder))
			return null;

		final IFolder folder= (IFolder) resource;
		if (!folder.isLinked())
			return null;

		return folder;
	}

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
				if (!(element instanceof IPackageFragmentRoot || element instanceof IJavaProject || element instanceof ClassPathContainer))
					return false;

				if (element instanceof IJavaProject) {
					IJavaProject project= (IJavaProject)element;
					if (!ClasspathModifier.isSourceFolder(project))
						return false;

				} else if (element instanceof IPackageFragmentRoot) {
					IClasspathEntry entry= ((IPackageFragmentRoot) element).getRawClasspathEntry();
					if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
						return false;
					}
				}
			}
			return true;
		} catch (JavaModelException e) {
		}
		return false;
	}

	private void showExceptionDialog(CoreException exception) {
		showError(exception, fSite.getShell(), NewWizardMessages.RemoveFromBuildpathAction_ErrorTitle, exception.getMessage());
	}

	private void showError(CoreException e, Shell shell, String title, String message) {
		IStatus status= e.getStatus();
		if (status != null) {
			ErrorDialog.openError(shell, message, title, status);
		} else {
			MessageDialog.openError(shell, title, message);
		}
	}
	
	private void selectAndReveal(final ISelection selection) {
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

}
