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

package org.eclipse.jdt.text.tests.performance;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.PartInitException;

public abstract class OpenEditorTest extends TestCase {
	private PerformanceMeterFactory fPerformanceMeterFactory= Performance.createPerformanceMeterFactory();

	protected void measureOpenInEditor(IFile[] files) throws PartInitException {
		PerformanceMeter performanceMeter= fPerformanceMeterFactory.createPerformanceMeter(this);
		try {
			for (int i= 0, n= files.length; i < n; i++) {
				performanceMeter.start();
				EditorTestHelper.openInEditor(files[i], true);
				performanceMeter.stop();
				sleep(2000); // NOTE: runnables posted from other threads, while the main thread waits here, are executed and measured only in the next iteration
			}
		} finally {
			EditorTestHelper.closeAllEditors();
			performanceMeter.commit();
		}
	}

	private synchronized void sleep(int time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
		}
	}
	
	/**
	 * Make sure the project exists and is opened. This is needed to circumvent
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=71362
	 * 
	 * It does not restore the java perspective!
	 * 
	 * @throws CoreException
	 */
	protected void ensureTestProjectOpened() throws CoreException {
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IProject project= workspace.getRoot().getProject(PerformanceTestSetup.PROJECT);
		IProjectDescription description= workspace.newProjectDescription(PerformanceTestSetup.PROJECT);
		description.setLocation(null);

		project.open(null);
	}
}
