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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;

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
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ClasspathContainerWizard;

public class AddLibraryToBuildpathAction extends Action implements ISelectionChangedListener {

	private IJavaProject fSelectedProject;
	private final IWorkbenchSite fSite;

	public AddLibraryToBuildpathAction(IWorkbenchSite site) {
		super(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddLibCP_label, JavaPluginImages.DESC_OBJS_LIBRARY);
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddLibCP_tooltip);
		fSite= site;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run() {
		final IJavaProject project= fSelectedProject;

		Shell shell= fSite.getShell();
		if (shell == null) {
			shell= JavaPlugin.getActiveWorkbenchShell();
		}

		IClasspathEntry[] classpath;
		try {
			classpath= project.getRawClasspath();
		} catch (JavaModelException e1) {
			showExceptionDialog(e1);
			return;
		}

		ClasspathContainerWizard wizard= new ClasspathContainerWizard((IClasspathEntry) null, project, classpath) {

			/**
			 * {@inheritDoc}
			 */
			public boolean performFinish() {
				if (super.performFinish()) {
					IWorkspaceRunnable op= new IWorkspaceRunnable() {
						public void run(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
							try {
								finishPage(monitor);
							} catch (InterruptedException e) {
								throw new OperationCanceledException(e.getMessage());
							}
						}
					};
					try {
						ISchedulingRule rule= null;
						Job job= Platform.getJobManager().currentJob();
						if (job != null)
							rule= job.getRule();
						IRunnableWithProgress runnable= null;
						if (rule != null)
							runnable= new WorkbenchRunnableAdapter(op, rule, true);
						else
							runnable= new WorkbenchRunnableAdapter(op, ResourcesPlugin.getWorkspace().getRoot());
						getContainer().run(false, true, runnable);
					} catch (InvocationTargetException e) {
						JavaPlugin.log(e);
						return false;
					} catch  (InterruptedException e) {
						return false;
					}
					return true;
				} 
				return false;
			}

			private void finishPage(IProgressMonitor pm) throws InterruptedException {
				IClasspathEntry[] selected= getNewEntries();
				if (selected != null) {
					try {
						pm.beginTask(NewWizardMessages.ClasspathModifier_Monitor_AddToBuildpath, 4); 

						List addedEntries= new ArrayList();
						for (int i= 0; i < selected.length; i++) {
							addedEntries.add(new CPListElement(project, IClasspathEntry.CPE_CONTAINER, selected[i].getPath(), null));
						}

						pm.worked(1);
						if (pm.isCanceled())
							throw new InterruptedException();

						List existingEntries= ClasspathModifier.getExistingEntries(project);
						ClasspathModifier.setNewEntry(existingEntries, addedEntries, project, new SubProgressMonitor(pm, 1));
						if (pm.isCanceled())
							throw new InterruptedException();

						ClasspathModifier.commitClassPath(existingEntries, project, new SubProgressMonitor(pm, 1));
						if (pm.isCanceled())
							throw new InterruptedException();

						List result= new ArrayList(addedEntries.size());
						for (int i= 0; i < addedEntries.size(); i++) {
							result.add(new ClassPathContainer(project, selected[i]));
						}
						selectAndReveal(new StructuredSelection(result));

						pm.worked(1);
					} catch (CoreException e) {
						showExceptionDialog(e);
					} finally {
						pm.done();
					}
				}
			}
		};
		wizard.setNeedsProgressMonitor(true);

		WizardDialog dialog= new WizardDialog(shell, wizard);
		PixelConverter converter= new PixelConverter(shell);
		dialog.setMinimumPageSize(converter.convertWidthInCharsToPixels(70), converter.convertHeightInCharsToPixels(20));
		dialog.create();
		dialog.open();
	}

	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection) {
			setEnabled(canHandle((IStructuredSelection) selection));
		} else {
			setEnabled(canHandle(StructuredSelection.EMPTY));
		}
	}

	public boolean canHandle(IStructuredSelection selection) {
		if (selection.size() == 1 && selection.getFirstElement() instanceof IJavaProject) {
			fSelectedProject= (IJavaProject)selection.getFirstElement();
			return true;
		}
		return false;
	}

	private void showExceptionDialog(CoreException exception) {
		showError(exception, fSite.getShell(), NewWizardMessages.AddLibraryToBuildpathAction_ErrorTitle, exception.getMessage());
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
