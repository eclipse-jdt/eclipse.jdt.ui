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
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.Progress;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ArchiveFileFilter;

/**
 * Action to add a JAR to the classpath of its parent project.
 * Action is applicable to selections containing archives (JAR or zip)
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.1
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class AddToClasspathAction extends SelectionDispatchAction {

	/**
	 * Creates a new <code>AddToClasspathAction</code>. The action requires that
	 * the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 *
	 * @param site the site providing context information for this action
	 */
	public AddToClasspathAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.AddToClasspathAction_label);
		setToolTipText(ActionMessages.AddToClasspathAction_toolTip);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ADD_TO_CLASSPATH_ACTION);
	}

	@Override
	public void selectionChanged(IStructuredSelection selection) {
		try {
			setEnabled(checkEnabled(selection));
		} catch (JavaModelException e) {
			// http://bugs.eclipse.org/bugs/show_bug.cgi?id=19253
			if (JavaModelUtil.isExceptionToBeLogged(e))
				JavaPlugin.log(e);
			setEnabled(false);
		}
	}

	private static boolean checkEnabled(IStructuredSelection selection) throws JavaModelException {
		if (selection.isEmpty())
			return false;
		for (Object name : selection) {
			if (! canBeAddedToBuildPath(name))
				return false;
		}
		return true;
	}

	private static boolean canBeAddedToBuildPath(Object element) throws JavaModelException{
		return (element instanceof IAdaptable) && getCandidate((IAdaptable) element) != null;
	}

	private static IFile getCandidate(IAdaptable element) throws JavaModelException {
		IResource resource= element.getAdapter(IResource.class);
		if (! (resource instanceof IFile) || ! ArchiveFileFilter.isArchivePath(resource.getFullPath(), true))
			return null;

		IJavaProject project= JavaCore.create(resource.getProject());
		if (project != null && project.exists() && (project.findPackageFragmentRoot(resource.getFullPath()) == null))
			return (IFile) resource;
		return null;
	}

	@Override
	public void run(IStructuredSelection selection) {
		try {
			final IFile[] files= getJARFiles(selection);

			IWorkspaceRunnable operation= new IWorkspaceRunnable() {
				@Override
				public void run(IProgressMonitor monitor) throws CoreException {
					monitor.beginTask(ActionMessages.AddToClasspathAction_progressMessage, files.length);
					for (IFile file : files) {
						monitor.subTask(BasicElementLabels.getPathLabel(file.getFullPath(), false));
						IJavaProject project= JavaCore.create(file.getProject());
						addToClassPath(project, file.getFullPath(), Progress.subMonitor(monitor, 1));
					}
				}

				private void addToClassPath(IJavaProject project, IPath jarPath, IProgressMonitor monitor) throws JavaModelException {
					if (monitor.isCanceled())
						throw new OperationCanceledException();
					IClasspathEntry[] entries= project.getRawClasspath();
					IClasspathEntry[] newEntries= new IClasspathEntry[entries.length + 1];
					System.arraycopy(entries, 0, newEntries, 0, entries.length);
					newEntries[entries.length]= JavaCore.newLibraryEntry(jarPath, null, null, false);
					project.setRawClasspath(newEntries, monitor);
				}
			};

			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(true, true, new WorkbenchRunnableAdapter(operation));
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(),
				ActionMessages.AddToClasspathAction_error_title,
				ActionMessages.AddToClasspathAction_error_message);
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, getShell(),
					ActionMessages.AddToClasspathAction_error_title,
					ActionMessages.AddToClasspathAction_error_message);
		} catch (InterruptedException e) {
			// canceled
		}

	}

	private static IFile[] getJARFiles(IStructuredSelection selection) throws JavaModelException {
		ArrayList<IFile> list= new ArrayList<>();
		for (Object element : selection) {
			if (element instanceof IAdaptable) {
				IFile file= getCandidate((IAdaptable) element);
				if (file != null) {
					list.add(file);
				}
			}
		}
		return list.toArray(new IFile[list.size()]);
	}
}

