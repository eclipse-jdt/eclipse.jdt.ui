/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import java.io.File;

import junit.framework.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.tests.ResourceHelper;

import org.eclipse.test.performance.PerformanceMeter;


/**
 * Performance tests for file buffers.
 * 
 * @since 3.3
 */
public class FileBufferPerformanceTest extends TextPerformanceTestCase2 {
	
	private static final int LONG_REPEAT_COUNT= 1000000;
	private static final int UNCONNECTED_REPEAT_COUNT= 100000;
	private static final int ALL_REPEAT_COUNT= 5000;

	private IProject fProject;
	private ITextFileBufferManager fManager;
	private IPath fPath;
	private File fTempFile;
	private IPath fTempFilePath;
	private IPath fNonExistingPath;
	

	public static Test suite() {
		return new PerfTestSuite(FileBufferPerformanceTest.class);
	}

	public static Test setUpTest(Test test) {
		return new PerformanceTestSetup(test);
	}
	
	
	protected void setUp() throws Exception {
		fProject= ResourceHelper.createProject("project");
		fManager= FileBuffers.getTextFileBufferManager();
		fProject= ResourceHelper.createProject("project");
		fPath= createPath(fProject);
		fNonExistingPath= new Path("project/folderA/folderB/noFile");
		ITextFileBuffer buffer= fManager.getTextFileBuffer(fPath);
		fTempFile= File.createTempFile("measureGetOldExternal", "txt");
		fTempFilePath= new Path(fTempFile.getAbsolutePath());
		assertTrue(buffer == null);
	}
	
	protected void tearDown() throws Exception {
		ResourceHelper.deleteProject("project");
		fTempFile.delete();
	}

	protected IPath createPath(IProject project) throws Exception {
		IFolder folder= ResourceHelper.createFolder("project/folderA/folderB/");
		IFile file= ResourceHelper.createFile(folder, "WorkspaceFile", "content");
		return file.getFullPath();
	}
	
	public void measureConnectOld(PerformanceMeter meter) throws CoreException {
		meter.start();
		for (int i= 0; i < LONG_REPEAT_COUNT; i++)
			fManager.connect(fPath, null);
		meter.stop();
		
		for (int i= 0; i < LONG_REPEAT_COUNT; i++)
			fManager.disconnect(fPath, null);
		
		assertNull(fManager.getTextFileBuffer(fPath));
	}
	
	public void measureGetOld(PerformanceMeter meter) throws CoreException {
		fManager.connect(fPath, null);
		meter.start();
		for (int i= 0; i < LONG_REPEAT_COUNT; i++)
			fManager.getTextFileBuffer(fPath);
		meter.stop();
		fManager.disconnect(fPath, null);
		
		assertNull(fManager.getTextFileBuffer(fPath));
	}
	
	public void measureGetOldNonExist(PerformanceMeter meter) throws CoreException {
		fManager.connect(fNonExistingPath, null);
		meter.start();
		for (int i= 0; i < LONG_REPEAT_COUNT; i++)
			fManager.getTextFileBuffer(fNonExistingPath);
		meter.stop();
		fManager.disconnect(fNonExistingPath, null);
		
		assertNull(fManager.getTextFileBuffer(fNonExistingPath));
	}
	
	public void measureGetOldExternal(PerformanceMeter meter) throws Exception {
		fManager.connect(fTempFilePath, null);
		meter.start();
		for (int i= 0; i < LONG_REPEAT_COUNT; i++)
			fManager.getTextFileBuffer(fTempFilePath);
		meter.stop();
		fManager.disconnect(fTempFilePath, null);
		assertNull(fManager.getTextFileBuffer(fTempFilePath));
	}
	
	public void measureAllOld(PerformanceMeter meter) throws CoreException {
		meter.start();
		for (int i= 0; i < ALL_REPEAT_COUNT; i++) {
			fManager.connect(fPath, null);
			fManager.getTextFileBuffer(fPath);
			fManager.disconnect(fPath, null);
		}
		meter.stop();
		
		assertNull(fManager.getTextFileBuffer(fPath));
	}
	
	public void measureGetUnconnectedOld(PerformanceMeter meter) throws CoreException {
		meter.start();
		for (int i= 0; i < UNCONNECTED_REPEAT_COUNT; i++)
			fManager.getTextFileBuffer(fPath);
		meter.stop();
		
		assertNull(fManager.getTextFileBuffer(fPath));
	}
	
	public void measureGetUnconnectedOldNonExist(PerformanceMeter meter) throws CoreException {
		meter.start();
		for (int i= 0; i < UNCONNECTED_REPEAT_COUNT; i++)
			fManager.getTextFileBuffer(fNonExistingPath);
		meter.stop();
		
		assertNull(fManager.getTextFileBuffer(fNonExistingPath));
	}

	public void measureGetUnconnectedOldExternal(PerformanceMeter meter) throws Exception {
		meter.start();
		for (int i= 0; i < UNCONNECTED_REPEAT_COUNT; i++)
			fManager.getTextFileBuffer(fTempFilePath);
		meter.stop();
		assertNull(fManager.getTextFileBuffer(fTempFilePath));
	}

}
