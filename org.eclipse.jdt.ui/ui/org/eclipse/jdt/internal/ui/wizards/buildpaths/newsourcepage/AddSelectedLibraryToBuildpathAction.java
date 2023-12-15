/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
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
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ModuleEncapsulationDetail;

//SelectedElements iff enabled: IFile
public class AddSelectedLibraryToBuildpathAction extends BuildpathModifierAction {

	private final IRunnableContext fContext;
	private boolean fForTestOnly;

	public AddSelectedLibraryToBuildpathAction(IWorkbenchSite site, boolean forTestOnly) {
		this(site, null, PlatformUI.getWorkbench().getProgressService(), forTestOnly);
	}

	public AddSelectedLibraryToBuildpathAction(IRunnableContext context, ISetSelectionTarget selectionTarget) {
		this(null, selectionTarget, context, false);
    }

	private AddSelectedLibraryToBuildpathAction(IWorkbenchSite site, ISetSelectionTarget selectionTarget, IRunnableContext context, boolean forTestOnly) {
		super(site, selectionTarget, forTestOnly ? BuildpathModifierAction.ADD_SEL_LIB_TO_TEST_BP : BuildpathModifierAction.ADD_SEL_LIB_TO_BP);

		fContext= context;
		fForTestOnly= forTestOnly;

		setText(forTestOnly ? NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelLibToTestCP_label: NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelLibToCP_label);
		setImageDescriptor(forTestOnly ? JavaPluginImages.DESC_OBJS_EXTJAR_TEST : JavaPluginImages.DESC_OBJS_EXTJAR);
		setToolTipText(forTestOnly ? NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelLibToTestCP_tooltip : NewWizardMessages.NewSourceContainerWorkbookPage_ToolBar_AddSelLibToCP_tooltip);
    }

	@Override
	public String getDetailedDescription() {
		if (!isEnabled())
			return null;

		IFile file= (IFile)getSelectedElements().get(0);
        IJavaProject project= JavaCore.create(file.getProject());

        try {
	        if (ClasspathModifier.isArchive(file, project)) {
	            String name= ClasspathModifier.escapeSpecialChars(BasicElementLabels.getResourceName(file));
	            return Messages.format(NewWizardMessages.PackageExplorerActionGroup_FormText_ArchiveToBuildpath, name);
	        }
        } catch (JavaModelException e) {
	        JavaPlugin.log(e);
        }

        return NewWizardMessages.PackageExplorerActionGroup_FormText_Default_toBuildpath;
	}

	@Override
	public void run() {
		try {
			final IFile[] files= getSelectedElements().toArray(new IFile[getSelectedElements().size()]);

			final IRunnableWithProgress runnable= monitor -> {
				try {
			        IJavaProject project= JavaCore.create(files[0].getProject());
			        List<IJavaElement> result= addLibraryEntries(files, project, monitor);
					selectAndReveal(new StructuredSelection(result));
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			};
			fContext.run(false, false, runnable);
		} catch (final InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				showExceptionDialog((CoreException)e.getCause(), NewWizardMessages.AddSelectedLibraryToBuildpathAction_ErrorTitle);
			} else {
				JavaPlugin.log(e);
			}
		} catch (final InterruptedException e) {
		}
	}

	private List<IJavaElement> addLibraryEntries(IFile[] resources, IJavaProject project, IProgressMonitor monitor) throws CoreException {
		List<CPListElement> addedEntries= new ArrayList<>();
		try {
			monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_AddToBuildpath, 4);
			for (IFile res : resources) {
				CPListElement cpListElement= new CPListElement(project, IClasspathEntry.CPE_LIBRARY, res.getFullPath(), res);
				if(fForTestOnly) {
					cpListElement.setAttribute(IClasspathAttribute.TEST, "true"); //$NON-NLS-1$
				} else {
					if(project.getModuleDescription() != null) {
						cpListElement.setAttribute(IClasspathAttribute.MODULE, new ModuleEncapsulationDetail[0]);
					}
				}
				addedEntries.add(cpListElement);
			}
			monitor.worked(1);

			List<CPListElement> existingEntries= ClasspathModifier.getExistingEntries(project);
			ClasspathModifier.setNewEntry(existingEntries, addedEntries, project, Progress.subMonitor(monitor, 1));
			ClasspathModifier.commitClassPath(existingEntries, project, Progress.subMonitor(monitor, 1));

        	BuildpathDelta delta= new BuildpathDelta(getToolTipText());
        	delta.setNewEntries(existingEntries.toArray(new CPListElement[existingEntries.size()]));
        	informListeners(delta);

			List<IJavaElement> result= new ArrayList<>(addedEntries.size());
			for (IResource res : resources) {
				IJavaElement elem= project.getPackageFragmentRoot(res);
				if (elem != null) {
					result.add(elem);
				}
			}

			monitor.worked(1);
			return result;
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
				if (element instanceof IFile) {
					IFile file= (IFile)element;
					IJavaProject project= JavaCore.create(file.getProject());
					if (project == null)
						return false;
					if (!ClasspathModifier.isArchive(file, project))
						return false;
					if (fForTestOnly) {
						if (!Arrays.stream(project.getRawClasspath()).anyMatch(e -> e.getEntryKind() == IClasspathEntry.CPE_SOURCE && e.isTest())) {
							return false;
						}
					}
				} else {
					return false;
				}
			}
		} catch (CoreException e) {
			return false;
		}
		return true;
	}
}
