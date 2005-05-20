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

import org.eclipse.ui.progress.UIJob;

import org.eclipse.jdt.core.JavaCore;

public class InitializeAfterLoadJob extends UIJob {
	public InitializeAfterLoadJob() {
		super(JavaUIMessages.InitializeAfterLoadJob_starter_job_name);
		setSystem(true);
	}
	public IStatus runInUIThread(IProgressMonitor monitor) {
		try {
			JavaCore.initializeAfterLoad(monitor);
		} catch (CoreException e) {
			JavaPlugin.log(e);
			return e.getStatus();
		}
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
	}
}
