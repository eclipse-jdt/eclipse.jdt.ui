/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.text.tests.performance;


import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.manipulation.ConvertLineDelimitersOperation;
import org.eclipse.core.filebuffers.manipulation.FileBufferOperationRunner;

/**
 * Measures the time to convert line delimiters of a project.
 * 
 * @since 3.1
 */
public class ConvertLineDelimitersProjectTest extends TextPerformanceTestCase {
	
	private static final Class THIS= ConvertLineDelimitersProjectTest.class;
	
	private static final int WARM_UP_RUNS= 5;

	private static final int MEASURED_RUNS= 5;

	private static final ConvertLineDelimitersOperation[] OPERATIONS= new ConvertLineDelimitersOperation[] {
		new ConvertLineDelimitersOperation("\r"),
		new ConvertLineDelimitersOperation("\n"),
		new ConvertLineDelimitersOperation("\r\n"),
	};
	
	private int fOperationIndex= 0;
	
	public static Test suite() {
		return new DisableAutoBuildTestSetup(new TextPluginTestSetup(new TestSuite(THIS)));
	}

	protected void setUp() throws Exception {
		super.setUp();
		setWarmUpRuns(WARM_UP_RUNS);
		setMeasuredRuns(MEASURED_RUNS);
	}

	/**
	 * Measures the time to convert line delimiters of a project.
	 * 
	 * @throws Exception if measure fails
	 */
	public void test() throws Exception {
		measure(getNullPerformanceMeter(), getWarmUpRuns());
		measure(createPerformanceMeter(), getMeasuredRuns());
		commitAllMeasurements();
		assertAllPerformance();
	}

	private void measure(PerformanceMeter performanceMeter, int runs) throws Exception {
		for (int i= 0; i < runs; i++) {
			performanceMeter.start();
			runOperation();
			performanceMeter.stop();
		}
	}

	private void runOperation() throws CoreException {
		IFile[] files= EditorTestHelper.findFiles(ResourceTestHelper.getProject(TextPluginTestSetup.PROJECT));
		FileBufferOperationRunner runner= new FileBufferOperationRunner(FileBuffers.getTextFileBufferManager(), null);
		runner.execute(getLocations(files), OPERATIONS[fOperationIndex], null);
		fOperationIndex= (fOperationIndex + 1) % OPERATIONS.length;
	}

	private static IPath[] getLocations(IResource[] files) {
		IPath[] locations= new IPath[files.length];
		for (int i= 0; i < locations.length; i++)
			locations[i]= files[i].getFullPath();
		return locations;
	}
}
