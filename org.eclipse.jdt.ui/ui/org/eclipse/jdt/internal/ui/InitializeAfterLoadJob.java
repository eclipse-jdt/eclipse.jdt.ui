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
 package org.eclipse.jdt.internal.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.ui.progress.UIJob;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaUI;

public class InitializeAfterLoadJob extends UIJob {

	private final static class RealJob extends Job {
		public RealJob(String name) {
			super(name);
		}
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			SubMonitor subMonitor= SubMonitor.convert(monitor, 10);
			try {
				JavaCore.initializeAfterLoad(subMonitor.split(6));
				JavaPlugin.initializeAfterLoad(subMonitor.split(4));
			} catch (CoreException e) {
				JavaPlugin.log(e);
				return e.getStatus();
			}
			return Status.OK_STATUS;
		}
		@Override
		public boolean belongsTo(Object family) {
			return JavaUI.ID_PLUGIN.equals(family);
		}
	}

	public InitializeAfterLoadJob() {
		super(JavaUIMessages.InitializeAfterLoadJob_starter_job_name);
		setSystem(true);
	}
	@Override
	public IStatus runInUIThread(IProgressMonitor monitor) {
		Job job = new RealJob(JavaUIMessages.JavaPlugin_initializing_ui);
		job.setPriority(Job.SHORT);
		job.schedule();
		return Status.OK_STATUS;
	}
}