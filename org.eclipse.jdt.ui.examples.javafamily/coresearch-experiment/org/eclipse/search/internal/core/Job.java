/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.search.internal.core;

import org.eclipse.core.runtime.IProgressMonitor;

public abstract class Job {

	/**
	 * Answer true if the job belongs to a given family (tag)
	 */
	public abstract boolean belongsTo(Object jobFamily);
	/**
	 * Asks this job to cancel its execution. The cancellation
	 * can take an undertermined amount of time.
	 */
	public abstract void cancel();
	/**
	 * Answer whether the job is ready to run.
	 */
	public abstract boolean isReadyToRun();
	/**
	 * Execute the current job, answer whether it was successful.
	 */
	public abstract boolean run(IProgressMonitor monitor);
}
