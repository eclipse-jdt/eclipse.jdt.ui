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

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public abstract class OpenEditorTest extends TestCase {
	
	private PerformanceMeterFactory fPerformanceMeterFactory= new OSPerformanceMeterFactory();

	protected void setUp() {
		runEventQueue(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
	}
	
	protected void measureOpenInEditor(IFile[] files) throws PartInitException {
		PerformanceMeter performanceMeter= fPerformanceMeterFactory.createPerformanceMeter(this);
		try {
			for (int i= 0, n= files.length; i < n; i++) {
				performanceMeter.start();
				openInEditor(files[i]);
				performanceMeter.stop();
				sleep(2000); // NOTE: runnables posted from other threads, while the main thread waits here, are executed and measured only in the next iteration
			}
		} finally {
			getActivePage().closeAllEditors(false);
			performanceMeter.commit();
		}
	}

	protected IFile[] findFiles(String prefix, String suffix, int i, int n) {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		List types= new ArrayList(n);
		for (int j= i; j < i + n; j++)
			types.add(root.getFile(new Path(root.getLocation().toString() + "/" + prefix + j + suffix)));
		return (IFile[]) types.toArray(new IFile[types.size()]);
	}

	private void openInEditor(IFile file) throws PartInitException {
		IEditorPart part= IDE.openEditor(getActivePage(), file);
		runEventQueue(part.getSite().getShell());
	}

	private void runEventQueue(Shell shell) {
		while (shell.getDisplay().readAndDispatch());
	}

	private IWorkbenchPage getActivePage() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	private synchronized void sleep(int time) {
		try {
			wait(time);
		} catch (InterruptedException e) {
		}
	}
}
