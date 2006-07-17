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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier.IClasspathModifierListener;

import org.eclipse.jdt.ui.wizards.BuildPathDialogAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

//SelectedElements iff enabled: IJavaProject && size==1
public class AddArchiveToBuildpathAction extends BuildpathModifierAction {

	private final IRunnableContext fContext;
	private final IClasspathModifierListener fListener;

	public AddArchiveToBuildpathAction(IWorkbenchSite site) {
		this(site, PlatformUI.getWorkbench().getProgressService(), null);
	}
	
	public AddArchiveToBuildpathAction(IWorkbenchSite site, IRunnableContext context, IClasspathModifierListener listener) {
		super(site);
		
		fContext= context;
		fListener= listener;

		setText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddJarCP_label);
		setImageDescriptor(JavaPluginImages.DESC_OBJS_EXTJAR);
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddJarCP_tooltip);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getDetailedDescription() {
		return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_toBuildpath_archives;
	}

	/**
	 * {@inheritDoc}
	 */
	public void run() {

		final Shell shell= getShell();
		final IPath[] selected= BuildPathDialogAccess.chooseExternalJAREntries(shell);

		try {
			final IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						List result= addExternalJars(selected == null?new IPath[0]:selected, (IJavaProject)getSelectedElements().get(0), monitor);
						if (result != null && result.size() > 0)
							selectAndReveal(new StructuredSelection(result));
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			fContext.run(false, false, runnable);
		} catch (final InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				showExceptionDialog((CoreException)e.getCause(), NewWizardMessages.AddArchiveToBuildpathAction_ErrorTitle);
			} else {
				JavaPlugin.log(e);
			}
		} catch (final InterruptedException e) {
		}
	}

	protected List addExternalJars(IPath[] jarPaths, IJavaProject project, IProgressMonitor monitor) throws CoreException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		List addedEntries= new ArrayList();
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_AddToBuildpath, 4); 
			if (jarPaths != null) {
				for (int i= 0; i < jarPaths.length; i++) {
					addedEntries.add(new CPListElement(project, IClasspathEntry.CPE_LIBRARY, jarPaths[i], null));
				}
				monitor.worked(1);

				List existingEntries= ClasspathModifier.getExistingEntries(project);
				ClasspathModifier.setNewEntry(existingEntries, addedEntries, project, new SubProgressMonitor(monitor, 1));
				ClasspathModifier.commitClassPath(existingEntries, project, fListener, new SubProgressMonitor(monitor, 1));

				List result= new ArrayList(addedEntries.size());
				for (int i= 0; i < addedEntries.size(); i++) {
					IClasspathEntry entry= ((CPListElement) addedEntries.get(i)).getClasspathEntry();
					IJavaElement elem= project.findPackageFragmentRoot(entry.getPath());
					if (elem != null) {
						result.add(elem);
					}
				}
				monitor.worked(1);
				return result;
			}
		} finally {
			monitor.done();
		}
		return null;
	}

	protected boolean canHandle(IStructuredSelection selection) {
		if (selection.size() != 1)
			return false;

		Object first= selection.getFirstElement();
		if (!(first instanceof IJavaProject))
			return false;

		return true;
	}
}
