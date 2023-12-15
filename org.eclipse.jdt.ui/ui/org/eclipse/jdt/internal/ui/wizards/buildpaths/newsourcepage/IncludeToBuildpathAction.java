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
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.buildpath.BuildpathDelta;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.Progress;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

//SelectedElements iff enabled: IResource
public class IncludeToBuildpathAction extends BuildpathModifierAction {

	private final IRunnableContext fContext;

	public IncludeToBuildpathAction(IWorkbenchSite site) {
		this(site, null, PlatformUI.getWorkbench().getProgressService());
	}

	public IncludeToBuildpathAction(IRunnableContext context, ISetSelectionTarget selectionTarget) {
		this(null, selectionTarget, context);
    }

	private IncludeToBuildpathAction(IWorkbenchSite site, ISetSelectionTarget selectionTarget, IRunnableContext context) {
		super(site, selectionTarget, BuildpathModifierAction.INCLUDE);

		fContext= context;

		setText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Unexclude_label);
		setImageDescriptor(JavaPluginImages.DESC_ELCL_INCLUDE_ON_BUILDPATH);
		setToolTipText(NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_Unexclude_tooltip);
		setDisabledImageDescriptor(JavaPluginImages.DESC_DLCL_INCLUDE_ON_BUILDPATH);
	}

	@Override
	public String getDetailedDescription() {
		if (!isEnabled())
			return null;

		if (getSelectedElements().size() != 1)
			return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_Unexclude;

		IResource resource= (IResource) getSelectedElements().get(0);
        String name= ClasspathModifier.escapeSpecialChars(BasicElementLabels.getResourceName(resource));

        if (resource instanceof IContainer) {
        	return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_UnexcludeFolder, name);
        } else if (resource instanceof IFile) {
        	return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_UnexcludeFile, name);
        }

        return null;
	}

	@Override
	public void run() {
		IResource resource= (IResource)getSelectedElements().get(0);
		final IJavaProject project= JavaCore.create(resource.getProject());

		try {
			final IRunnableWithProgress runnable= monitor -> {
				try {
					List<?> result= unExclude(getSelectedElements(), project, monitor);
					selectAndReveal(new StructuredSelection(result));
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			};
			fContext.run(false, false, runnable);
		} catch (final InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				showExceptionDialog((CoreException)e.getCause(), NewWizardMessages.IncludeToBuildpathAction_ErrorTitle);
			} else {
				JavaPlugin.log(e);
			}
		} catch (final InterruptedException e) {
		}
	}

	protected List<?> unExclude(List<?> elements, IJavaProject project, IProgressMonitor monitor) throws JavaModelException {
		if (monitor == null)
			monitor= new NullProgressMonitor();
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_Including, 2 * elements.size());

			List<CPListElement> entries= ClasspathModifier.getExistingEntries(project);
			for (Object element : elements) {
				IResource resource= (IResource) element;
				IPackageFragmentRoot root= ClasspathModifier.getFragmentRoot(resource, project, Progress.subMonitor(monitor, 1));
				if (root != null) {
					CPListElement entry= ClasspathModifier.getClasspathEntry(entries, root);
					ClasspathModifier.unExclude(resource, entry, project, Progress.subMonitor(monitor, 1));
				}
			}

			ClasspathModifier.commitClassPath(entries, project, Progress.subMonitor(monitor, 4));

        	BuildpathDelta delta= new BuildpathDelta(getToolTipText());
        	delta.setNewEntries(entries.toArray(new CPListElement[entries.size()]));
        	informListeners(delta);

			List<?> resultElements= ClasspathModifier.getCorrespondingElements(elements, project);
			return resultElements;
		} finally {
			monitor.done();
		}
	}

	@Override
	protected boolean canHandle(IStructuredSelection elements) {
		if (elements.size() == 0)
			return false;

		try {
			for (Object element : elements) {
				if (element instanceof IResource) {
					IResource resource= (IResource)element;
					IJavaProject project= JavaCore.create(resource.getProject());
					if (project == null || !project.exists())
						return false;

					if (!ClasspathModifier.isExcluded(resource, project))
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
}
