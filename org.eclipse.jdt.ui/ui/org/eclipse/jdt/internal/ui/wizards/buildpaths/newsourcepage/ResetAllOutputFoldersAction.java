/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.BuildpathDelta;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.Progress;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;

public class ResetAllOutputFoldersAction extends BuildpathModifierAction {

	private final IRunnableContext fContext;
	private final IJavaProject fJavaProject;


	public ResetAllOutputFoldersAction(IRunnableContext context, IJavaProject project, ISetSelectionTarget selectionTarget) {
		this(null, selectionTarget, context, project);
    }

	public ResetAllOutputFoldersAction(IWorkbenchSite site, ISetSelectionTarget selectionTarget, IRunnableContext context, IJavaProject javaProject) {
		super(site, selectionTarget, BuildpathModifierAction.RESET_ALL_OUTPUT_FOLDERS);

		fContext= context;
		fJavaProject= javaProject;

		setText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Reset_tooltip);
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Reset_tooltip);
    }

	@Override
	public String getDetailedDescription() {
		return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_ResetAllOutputFolders;
	}

	@Override
	public void run() {
		final IRunnableWithProgress runnable= monitor -> {
			try {
				resetOutputFolders(fJavaProject, monitor);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			}
		};
		try {
	        fContext.run(false, false, runnable);
        } catch (InvocationTargetException e) {
        	if (e.getCause() instanceof CoreException) {
				showExceptionDialog((CoreException)e.getCause(), NewWizardMessages.RemoveFromBuildpathAction_ErrorTitle);
			} else {
				JavaPlugin.log(e);
			}
        } catch (InterruptedException e) {
        }
	}

	private List<Object> resetOutputFolders(IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			IPackageFragmentRoot[] roots= project.getPackageFragmentRoots();
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_ResetOutputFolder, roots.length + 10);
			List<CPListElementAttribute> entries= new ArrayList<>();
			for (IPackageFragmentRoot root : roots) {
				monitor.worked(1);
				if (root.isArchive() || root.isExternal())
					continue;
				IClasspathEntry entry= root.getRawClasspathEntry();
				CPListElement element= CPListElement.createFromExisting(entry, project);
				CPListElementAttribute outputFolder= new CPListElementAttribute(element, CPListElement.OUTPUT, element.getAttribute(CPListElement.OUTPUT), true);
				entries.add(outputFolder);
			}
			return reset(entries, project, Progress.subMonitor(monitor, 10));
		} finally {
			monitor.done();
		}
	}

	private List<Object> reset(List<CPListElementAttribute> selection, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
	    if (monitor == null)
        	monitor= new NullProgressMonitor();
        try {
        	monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_Resetting, selection.size());
        	List<CPListElement> entries= ClasspathModifier.getExistingEntries(project);
        	List<Object> result= new ArrayList<>();
        	for (CPListElementAttribute element : selection) {
        		if (element instanceof IJavaElement) {
        			IJavaElement javaElement= (IJavaElement) element;
        			IPackageFragmentRoot root;
        			if (element instanceof IJavaProject)
        				root= project.getPackageFragmentRoot(project.getResource());
        			else
        				root= (IPackageFragmentRoot) element;
        			CPListElement entry= ClasspathModifier.getClasspathEntry(entries, root);
        			ClasspathModifier.resetFilters(javaElement, entry, project, Progress.subMonitor(monitor, 1));
        			result.add(javaElement);
        		} else {
        			CPListElement selElement= element.getParent();
        			CPListElement entry= ClasspathModifier.getClasspathEntry(entries, selElement);
        			CPListElementAttribute outputFolder= ClasspathModifier.resetOutputFolder(entry, project);
        			result.add(outputFolder);
        		}
        	}

        	ClasspathModifier.commitClassPath(entries, project, null);

        	BuildpathDelta delta= new BuildpathDelta(getToolTipText());
        	delta.setNewEntries(entries.toArray(new CPListElement[entries.size()]));
        	informListeners(delta);

        	return result;
        } finally {
        	monitor.done();
        }
    }

	@Override
	protected boolean canHandle(IStructuredSelection elements) {
		return true;
	}
}
