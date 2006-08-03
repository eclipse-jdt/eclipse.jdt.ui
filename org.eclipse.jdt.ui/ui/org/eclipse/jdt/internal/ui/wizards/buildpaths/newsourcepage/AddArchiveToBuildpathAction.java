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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.BuildpathDelta;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.wizards.BuildPathDialogAccess;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

//SelectedElements iff enabled: IJavaProject && size==1
public class AddArchiveToBuildpathAction extends BuildpathModifierAction {

	private final IRunnableContext fContext;

	public AddArchiveToBuildpathAction(IWorkbenchSite site) {
		this(site, null, PlatformUI.getWorkbench().getProgressService());
	}
	
	public AddArchiveToBuildpathAction(IRunnableContext context, ISetSelectionTarget selectionTarget) {
		this(null, selectionTarget, context);
    }
	
	private AddArchiveToBuildpathAction(IWorkbenchSite site, ISetSelectionTarget selectionTarget, IRunnableContext context) {
		super(site, selectionTarget, BuildpathModifierAction.ADD_LIB_TO_BP);
		
		fContext= context;

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
		if (selected == null)
			return;
		
		try {
			run(selected, false);
		} catch (final InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				showExceptionDialog((CoreException)e.getCause(), NewWizardMessages.AddArchiveToBuildpathAction_ErrorTitle);
			} else {
				JavaPlugin.log(e);
			}
		} catch (CoreException e) {
			showExceptionDialog(e, NewWizardMessages.AddArchiveToBuildpathAction_ErrorTitle);
			JavaPlugin.log(e);
        }
	}
	
	public void run(final IPath[] absolutePaths, boolean headless) throws InvocationTargetException, CoreException {

		final IJavaProject javaProject= (IJavaProject)getSelectedElements().get(0);
		IPath[] duplicatePaths= getDuplicatePaths(absolutePaths, javaProject);
		final IPath[] paths;
		if (duplicatePaths.length > 0) {
			paths= new IPath[absolutePaths.length - duplicatePaths.length];
			int j= 0;
			for (int i= 0; i < absolutePaths.length; i++) {
	            if (!contains(duplicatePaths, absolutePaths[i])) {
	            	paths[j]= absolutePaths[i];
	            	j++;
	            }
            }
			
			if (!headless) {
				String message;
				if (duplicatePaths.length > 1) {
					StringBuffer buf= new StringBuffer();
					for (int i= 0; i < duplicatePaths.length; i++) {
		                buf.append('\n').append(duplicatePaths[i].lastSegment());
	                }
					message= Messages.format(NewWizardMessages.AddArchiveToBuildpathAction_DuplicateArchivesInfo_message, buf.toString());
				} else {
					message= Messages.format(NewWizardMessages.AddArchiveToBuildpathAction_DuplicateArchiveInfo_message, duplicatePaths[0].lastSegment());
				}
				MessageDialog.openInformation(getShell(), NewWizardMessages.AddArchiveToBuildpathAction_InfoTitle, message);
			}
		} else {
			paths= absolutePaths;
		}
		
		if (paths.length == 0)
			return;
		
		try {
			final IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						List result= addExternalJars(paths, javaProject, monitor);
						if (result != null && result.size() > 0)
							selectAndReveal(new StructuredSelection(result));
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			};
			fContext.run(false, false, runnable);
		} catch (final InterruptedException e) {
		}
	}

	private boolean contains(IPath[] paths, IPath path) {
		for (int i= 0; i < paths.length; i++) {
	        if (paths[i].equals(path))
	        	return true;
        }
	    return false;
    }

	private IPath[] getDuplicatePaths(final IPath[] absolutePaths, final IJavaProject javaProject) throws JavaModelException {
	    IClasspathEntry[] rawClasspath= javaProject.getRawClasspath();

		List duplicatePaths= new ArrayList();
		for (int j= 0; j < absolutePaths.length; j++) {
			for (int i= 0; i < rawClasspath.length; i++) {
		        if (absolutePaths[j].equals(rawClasspath[i].getPath())) {
		        	duplicatePaths.add(absolutePaths[j]);
		        }
	        }
		}
		return (IPath[])duplicatePaths.toArray(new IPath[duplicatePaths.size()]);
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
				ClasspathModifier.commitClassPath(existingEntries, project, new SubProgressMonitor(monitor, 1));

        		BuildpathDelta delta= new BuildpathDelta(getToolTipText());
        		delta.setNewEntries((CPListElement[])existingEntries.toArray(new CPListElement[existingEntries.size()]));
        		informListeners(delta);

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
