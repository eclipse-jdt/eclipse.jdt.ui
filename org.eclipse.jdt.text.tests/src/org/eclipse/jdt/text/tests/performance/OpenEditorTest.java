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
import java.util.StringTokenizer;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.perfmsr.core.IPerformanceMonitor;
import org.eclipse.perfmsr.core.LoadValueConstants;
import org.eclipse.perfmsr.core.PerfMsrCorePlugin;
import org.eclipse.perfmsr.core.Upload;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class OpenEditorTest extends TestCase {
	
	private IPerformanceMonitor fPerformanceMonitor;
	
	private static final int N_OF_COPIES= 20;

	private static final String FILE_PREFIX= "org.eclipse.swt/Eclipse SWT/win32/org/eclipse/swt/graphics/TextLayout";

	private static final String FILE_SUFFIX= ".java";

	private static final String LOG_FILE= "timer-OpenEditorTest.xml";

	protected void setUp() {
		fPerformanceMonitor= PerfMsrCorePlugin.getPerformanceMonitor(false);
		fPerformanceMonitor.setLogFile(getLogFile());
		fPerformanceMonitor.setVar("50");

		runEventQueue(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
	}
	
	protected void tearDown() {
		Upload.Status status= fPerformanceMonitor.upload(null);
		System.out.println(status.message);
	}
	
	public void testOpenJavaEditor() throws PartInitException {
		try {
			IFile[] files= findFiles(FILE_PREFIX, FILE_SUFFIX, 0, N_OF_COPIES);
			for (int i= 0, n= files.length; i < n; i++) {
				fPerformanceMonitor.snapshot(1);
				openInEditor(files[i]);
				fPerformanceMonitor.snapshot(2);
				sleep(2000); // NOTE: runnables posted from other threads, while the main thread waits here, are executed and measured only in the next iteration
			}
		} finally {
			getActivePage().closeAllEditors(false);
		}
	}

	private void openInEditor(IFile file) throws PartInitException {
		IEditorPart part= IDE.openEditor(getActivePage(), file);
		runEventQueue(part.getSite().getShell());
	}

	private IFile[] findFiles(String prefix, String suffix, int i, int n) {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		List types= new ArrayList(n);
		for (int j= i; j < i + n; j++)
			types.add(root.getFile(new Path(root.getLocation().toString() + "/" + prefix + j + suffix)));
		return (IFile[]) types.toArray(new IFile[types.size()]);
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
	
	private String getLogFile() {
		String ctrl= System.getProperty(LoadValueConstants.ENV_PERF_CTRL);
		if (ctrl == null)
			return LOG_FILE;
		
		StringTokenizer st= new StringTokenizer(ctrl, ";");
		while(st.hasMoreTokens()) {
			String token= st.nextToken();
			int i= token.indexOf('=');
			if (i < 1)
				continue;
			String value= token.substring(i+1);
			String parm= token.substring(0,i);
			if (parm.equals(LoadValueConstants.PerfCtrl.log))
				return value + "/" + LOG_FILE;
		}
		return LOG_FILE;
	}	
}
