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

import java.io.File;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

public class OpenTextEditorTestSetup extends TestSetup {

	public OpenTextEditorTestSetup(Test test) {
		super(test);
	}

	protected void setUp() throws Exception {
		try {
			String workspacePath= ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/";
			String src= workspacePath + PerformanceTestSetup.PROJECT + OpenTextEditorTest.ORIG_FILE;
			String destPrefix= workspacePath + PerformanceTestSetup.PROJECT + OpenTextEditorTest.PATH + OpenTextEditorTest.FILE_PREFIX;
			for (int i= 0; i < OpenTextEditorTest.N_OF_COPIES; i++)
				FileTool.copy(new File(src), new File(destPrefix + i + OpenTextEditorTest.FILE_SUFFIX));

			ResourcesPlugin.getWorkspace().getRoot().getFolder(new Path(PerformanceTestSetup.PROJECT + OpenTextEditorTest.PATH)).refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	protected void tearDown() {
		// do nothing, the actual test runs in its own workbench
	}
}
