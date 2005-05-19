/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 package org.eclipse.jdt.internal.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.ui.progress.UIJob;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.TypeInfoHistory;

public class InitializeAfterLoadJob extends UIJob {
	public InitializeAfterLoadJob() {
		super(JavaUIMessages.InitializeAfterLoadJob_starter_job_name);
		setSystem(true);
	}
	public IStatus runInUIThread(IProgressMonitor monitor) {
		Job realJob= new Job(JavaUIMessages.InitializeAfterLoadJob_real_job_name) {
			protected IStatus run(IProgressMonitor pm) {
				pm.beginTask(JavaUIMessages.TypeSelectionDialog_progress_consistency, 100);
				try {
					JavaCore.initializeAfterLoad(new SubProgressMonitor(pm, 90));
					TypeInfoHistory.getInstance().checkConsistency(new SubProgressMonitor(pm, 10));
				} catch (CoreException e) {
					JavaPlugin.log(e);
					return e.getStatus();
				} finally {
					pm.done();
				}
				return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
			}
		};
		realJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
		// wait another two seconds
		realJob.schedule(2 * 1000);
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
	}
}
