/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests.performance;

import java.io.File;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.filebuffers.tests.ResourceHelper;

import junit.framework.Test;


/**
 * Performance tests for file buffers.
 *
 * @since 3.3
 */
@SuppressWarnings("restriction") // org.eclipse.core.filebuffers.tests
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

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fProject= ResourceHelper.createProject("project");
		fManager= FileBuffers.getTextFileBufferManager();
		fProject= ResourceHelper.createProject("project");
		fPath= createPath(fProject);
		fNonExistingPath= new Path("project/folderA/folderB/noFile");
		ITextFileBuffer buffer= fManager.getTextFileBuffer(fPath, LocationKind.NORMALIZE);
		fTempFile= File.createTempFile("measureGetOldExternal", "txt");
		fTempFilePath= new Path(fTempFile.getAbsolutePath());
		assertNull(buffer);
	}

	@Override
	protected void tearDown() throws Exception {
		ResourceHelper.deleteProject("project");
		fTempFile.delete();
		super.tearDown();
	}

	protected IPath createPath(IProject project) throws Exception {
		IFolder folder= ResourceHelper.createFolder(project.getName() + "/folderA/folderB/");
		IFile file= ResourceHelper.createFile(folder, "WorkspaceFile", "content");
		return file.getFullPath();
	}

	public void measureConnectOld(PerformanceMeter meter) throws CoreException {
		meter.start();
		for (int i= 0; i < LONG_REPEAT_COUNT; i++)
			fManager.connect(fPath, LocationKind.IFILE, null);
		meter.stop();

		for (int i= 0; i < LONG_REPEAT_COUNT; i++)
			fManager.disconnect(fPath, LocationKind.IFILE, null);

		assertNull(fManager.getTextFileBuffer(fPath, LocationKind.IFILE));
	}

	public void measureGetOld(PerformanceMeter meter) throws CoreException {
		fManager.connect(fPath, LocationKind.IFILE, null);
		meter.start();
		for (int i= 0; i < LONG_REPEAT_COUNT; i++)
			fManager.getTextFileBuffer(fPath, LocationKind.IFILE);
		meter.stop();
		fManager.disconnect(fPath, LocationKind.IFILE, null);

		assertNull(fManager.getTextFileBuffer(fPath, LocationKind.IFILE));
	}

	public void measureGetOldNonExist(PerformanceMeter meter) throws CoreException {
		fManager.connect(fNonExistingPath, LocationKind.NORMALIZE, null);
		meter.start();
		for (int i= 0; i < LONG_REPEAT_COUNT; i++)
			fManager.getTextFileBuffer(fNonExistingPath, LocationKind.NORMALIZE);
		meter.stop();
		fManager.disconnect(fNonExistingPath, LocationKind.NORMALIZE, null);

		assertNull(fManager.getTextFileBuffer(fNonExistingPath, LocationKind.NORMALIZE));
	}

	public void measureGetOldExternal(PerformanceMeter meter) throws Exception {
		fManager.connect(fTempFilePath, LocationKind.LOCATION, null);
		meter.start();
		for (int i= 0; i < LONG_REPEAT_COUNT; i++)
			fManager.getTextFileBuffer(fTempFilePath, LocationKind.LOCATION);
		meter.stop();
		fManager.disconnect(fTempFilePath, LocationKind.LOCATION, null);
		assertNull(fManager.getTextFileBuffer(fTempFilePath, LocationKind.LOCATION));
	}

	public void measureAllOld(PerformanceMeter meter) throws CoreException {
		meter.start();
		for (int i= 0; i < ALL_REPEAT_COUNT; i++) {
			fManager.connect(fPath, LocationKind.IFILE, null);
			fManager.getTextFileBuffer(fPath, LocationKind.IFILE);
			fManager.disconnect(fPath, LocationKind.IFILE, null);
		}
		meter.stop();

		assertNull(fManager.getTextFileBuffer(fPath, LocationKind.IFILE));
	}

	public void measureGetUnconnectedOld(PerformanceMeter meter) {
		meter.start();
		for (int i= 0; i < UNCONNECTED_REPEAT_COUNT; i++)
			fManager.getTextFileBuffer(fPath, LocationKind.IFILE);
		meter.stop();

		assertNull(fManager.getTextFileBuffer(fPath, LocationKind.IFILE));
	}

	public void measureGetUnconnectedOldNonExist(PerformanceMeter meter) {
		meter.start();
		for (int i= 0; i < UNCONNECTED_REPEAT_COUNT; i++)
			fManager.getTextFileBuffer(fNonExistingPath, LocationKind.NORMALIZE);
		meter.stop();

		assertNull(fManager.getTextFileBuffer(fNonExistingPath, LocationKind.NORMALIZE));
	}

	public void measureGetUnconnectedOldExternal(PerformanceMeter meter) throws Exception {
		meter.start();
		for (int i= 0; i < UNCONNECTED_REPEAT_COUNT; i++)
			fManager.getTextFileBuffer(fTempFilePath, LocationKind.LOCATION);
		meter.stop();
		assertNull(fManager.getTextFileBuffer(fTempFilePath, LocationKind.LOCATION));
	}

}