/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
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
 */
public class AddJARToClasspathAction extends SelectionDispatchAction {

	/**
	 * Creates a new <code>RefreshAction</code>. The action requires
	 * that the selection provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public AddJARToClasspathAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.getString("AddJARToClasspathAction.label")); //$NON-NLS-1$
		setToolTipText(ActionMessages.getString("AddJARToClasspathAction.toolTip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.ADDJAR_ACTION);
	}
	
	/* (non-Javadoc)
	 * Method declared in SelectionDispatchAction
	 */
	protected void selectionChanged(IStructuredSelection selection) {
		setEnabled(checkEnabled(selection));
	}
	
	private IFile getCandidate(IAdaptable element) throws JavaModelException {
		IResource resource= (IResource)((IAdaptable) element).getAdapter(IResource.class);
		if (resource instanceof IFile && ArchiveFileFilter.isArchivePath(resource.getFullPath())) {
			IJavaProject project= JavaCore.create(resource.getProject());
			if (project.findPackageFragmentRoot(resource.getFullPath()) == null) {
				return (IFile) resource;
			}
		}
		return null;
	}
	
	
	private boolean checkEnabled(IStructuredSelection selection) {
		if (selection.isEmpty())
			return false;
		try {
			for (Iterator iter= selection.iterator(); iter.hasNext();) {
				Object element= (Object) iter.next();
				if (element instanceof IAdaptable) {
					if (getCandidate((IAdaptable) element) == null) {
						return false;
					}
				} else {
					return false;
				}
			}
			return true;
		} catch (JavaModelException e) {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * Method declared in SelectionDispatchAction
	 */
	protected void run(final IStructuredSelection selection) {
		IWorkspaceRunnable operation= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IFile[] files= getJARFiles(selection);
				monitor.beginTask(ActionMessages.getString("AddJARToClasspathAction.progressMessage"), files.length); //$NON-NLS-1$
				for (int i= 0; i < files.length; i++) {
					monitor.subTask(files[i].getFullPath().toString());
					IJavaProject project= JavaCore.create(files[i].getProject());
					addToClassPath(project, files[i].getFullPath(), new SubProgressMonitor(monitor, 1));
				}
			}
			
			private void addToClassPath(IJavaProject project, IPath jarPath, IProgressMonitor monitor) throws JavaModelException {
				IClasspathEntry[] entries= project.getRawClasspath();
				IClasspathEntry[] newEntries= new IClasspathEntry[entries.length + 1];
				System.arraycopy(entries, 0, newEntries, 0, entries.length);
				newEntries[entries.length]= JavaCore.newLibraryEntry(jarPath, null, null, false);
				project.setRawClasspath(newEntries, monitor);
			}
		};
		
		try {
			new ProgressMonitorDialog(getShell()).run(true, true, new WorkbenchRunnableAdapter(operation));
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, getShell(), 
				ActionMessages.getString("AddJARToClasspathAction.error.title"),  //$NON-NLS-1$
				ActionMessages.getString("AddJARToClasspathAction.error.message")); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// canceled
		}
	}
	
	private IFile[] getJARFiles(IStructuredSelection selection) throws JavaModelException {
		ArrayList list= new ArrayList();
		for (Iterator iter= selection.iterator(); iter.hasNext();) {
			Object element= (Object) iter.next();
			if (element instanceof IAdaptable) {
				IFile file= getCandidate((IAdaptable) element);
				if (file != null) {
					list.add(file);
				}
			}
		}
		return (IFile[]) list.toArray(new IFile[list.size()]);
	}
}

