/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.util;

import org.osgi.framework.Bundle;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;


public class CoreUtility {
	
	/**
	 * Creates a folder and all parent folders if not existing.
	 * Project must exist.
	 * <code> org.eclipse.ui.dialogs.ContainerGenerator</code> is too heavy
	 * (creates a runnable)
	 */
	public static void createFolder(IFolder folder, boolean force, boolean local, IProgressMonitor monitor) throws CoreException {
		if (!folder.exists()) {
			IContainer parent= folder.getParent();
			if (parent instanceof IFolder) {
				createFolder((IFolder)parent, force, local, null);
			}
			folder.create(force, local, monitor);
		}
	}
	
	/**
	 * Creates an extension.  If the extension plugin has not
	 * been loaded a busy cursor will be activated during the duration of
	 * the load.
	 *
	 * @param element the config element defining the extension
	 * @param classAttribute the name of the attribute carrying the class
	 * @return the extension object
	 */
	public static Object createExtension(final IConfigurationElement element, final String classAttribute) throws CoreException {
		// If plugin has been loaded create extension.
		// Otherwise, show busy cursor then create extension.
		String pluginId = element.getNamespace();
		Bundle bundle = Platform.getBundle(pluginId);
		if (bundle != null && bundle.getState() == Bundle.ACTIVE ) {
			return element.createExecutableExtension(classAttribute);
		} else {
			final Object[] ret = new Object[1];
			final CoreException[] exc = new CoreException[1];
			BusyIndicator.showWhile(null, new Runnable() {
				public void run() {
					try {
						ret[0] = element.createExecutableExtension(classAttribute);
					} catch (CoreException e) {
						exc[0] = e;
					}
				}
			});
			if (exc[0] != null)
				throw exc[0];
			else
				return ret[0];
		}
	}	

	
	/**
	 * Starts a build in the background.
	 * @param project The project to build or <code>null</code> to build the workspace.
	 */
	public static void startBuildInBackground(final IProject project) {
		
		Job buildJob = new Job(JavaUIMessages.getString("CoreUtility.job.title")){  //$NON-NLS-1$
			/* (non-Javadoc)
			 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (project != null) {
						monitor.beginTask(JavaUIMessages.getFormattedString("CoreUtility.buildproject.taskname", project.getName()), 2); //$NON-NLS-1$
						project.build(IncrementalProjectBuilder.FULL_BUILD, new SubProgressMonitor(monitor,1));
						JavaPlugin.getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new SubProgressMonitor(monitor,1));
					} else {
						monitor.beginTask(JavaUIMessages.getString("CoreUtility.buildall.taskname"), 2); //$NON-NLS-1$
						JavaPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new SubProgressMonitor(monitor, 2));
					}
				} catch (CoreException e) {
					return e.getStatus();
				} catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				}
				finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
			public boolean belongsTo(Object family) {
				return ResourcesPlugin.FAMILY_MANUAL_BUILD == family;
			}
		};
		
		buildJob.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
		buildJob.setUser(true); 
		buildJob.schedule();
	}
}
