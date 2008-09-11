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
package org.eclipse.jdt.ui.tests.buildpath;

import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.buildpath.BuildpathDelta;
import org.eclipse.jdt.internal.corext.buildpath.CPJavaProject;
import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;

import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;

public class BuildpathModifierActionTest extends TestCase {

    private static final String DEFAULT_OUTPUT_FOLDER_NAME= "bin";
	private static final String PROJECT_NAME= "P01";

	private IJavaProject fJavaProject;

	public BuildpathModifierActionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(BuildpathModifierActionTest.class);
	}

	protected void setUp() throws Exception {
	}

	protected void tearDown() throws Exception {
		if (fJavaProject != null) {
			JavaProjectHelper.delete(fJavaProject);
			fJavaProject= null;
		}
	}

	private static IJavaProject createProject(String defaultOutputFolder) throws CoreException {
    	IJavaProject result= JavaProjectHelper.createJavaProject(PROJECT_NAME, defaultOutputFolder);
    	IPath[] rtJarPath= JavaProjectHelper.findRtJar(JavaProjectHelper.RT_STUBS_15);
    	result.setRawClasspath(new IClasspathEntry[] {  JavaCore.newLibraryEntry(rtJarPath[0], rtJarPath[1], rtJarPath[2], true) }, null);
    	return result;
    }

	private static void assertIsOnBuildpath(IClasspathEntry[] entries, IPath path) {
		for (int i= 0; i < entries.length; i++) {
	        if (entries[i].getPath().equals(path))
	        	return;
        }
		assertTrue("Element with location " + path + " is not on buildpath", false);
    }

    private static void assertDeltaResources(BuildpathDelta delta, IPath[] createdFolders, IPath[] removedFolders, IPath[] createdFiles, IPath[] removedFiles) {
    	IResource[] createdResources= delta.getCreatedResources();
    	IPath[] createdPaths= new IPath[createdResources.length];
    	for (int i= 0; i < createdResources.length; i++) {
	        createdPaths[i]= createdResources[i].getFullPath();
        }

    	for (int i= 0; i < createdPaths.length; i++) {
	        IPath path= createdPaths[i];
			if (createdResources[i] instanceof IFile) {
	        	assertTrue("File " + createdResources[i] + " is unexpected created", contains(createdFiles, path));
	        } else if (createdResources[i] instanceof IFolder) {
	        	assertTrue("Folder " + createdResources[i] + " is unexpected created", contains(createdFolders, path));
	        } else {
	        	assertTrue("Resource " + createdResources[i] + " is nor file nor folder.", false);
	        }
        }
    	for (int i= 0; i < createdFolders.length; i++) {
	        assertTrue("Folder at " + createdFolders[i] + " was not created", contains(createdPaths, createdFolders[i]));
        }
    	for (int i= 0; i < createdFiles.length; i++) {
    		assertTrue("File at " + createdFiles[i] + " was not created", contains(createdPaths, createdFiles[i]));
        }

    	IResource[] deletedResources= delta.getDeletedResources();
    	IPath[] deletedPaths= new IPath[deletedResources.length];
    	for (int i= 0; i < deletedResources.length; i++) {
	        deletedPaths[i]= deletedResources[i].getFullPath();
        }

    	for (int i= 0; i < deletedPaths.length; i++) {
	        IPath path= deletedPaths[i];
			if (deletedResources[i] instanceof IFile) {
	        	assertTrue("File " + deletedResources[i] + " is unexpected removed", contains(removedFiles, path));
	        } else if (deletedResources[i] instanceof IFolder) {
	        	assertTrue("Folder " + deletedResources[i] + " is unexpected removed", contains(removedFolders, path));
	        } else {
	        	assertTrue("Resource " + deletedResources[i] + " is nor file nor folder.", false);
	        }
        }
    	for (int i= 0; i < removedFolders.length; i++) {
	        assertTrue("Folder at " + removedFolders[i] + " was not removed", contains(deletedPaths, removedFolders[i]));
        }
    	for (int i= 0; i < removedFiles.length; i++) {
    		assertTrue("File at " + removedFiles[i] + " was not removed", contains(deletedPaths, removedFiles[i]));
        }
    }

    private static void assertDeltaRemovedEntries(BuildpathDelta delta, IPath[] paths) {
    	List removedEntries= delta.getRemovedEntries();
    	assertTrue("Expected " + paths.length + " is " + removedEntries.size(), removedEntries.size() == paths.length);
    	IPath[] removed= new IPath[removedEntries.size()];
    	int i= 0;
    	for (Iterator iterator= removedEntries.iterator(); iterator.hasNext();) {
	        CPListElement element= (CPListElement)iterator.next();
	        removed[i]= element.getPath();
	        i++;
        }
    	for (int j= 0; j < paths.length; j++) {
	        assertTrue("Entry " + paths[j] + " was not removed", contains(removed, paths[j]));
        }
    	for (int j= 0; j < removed.length; j++) {
	        assertTrue("Entry " + removed[j] + " was removed", contains(paths, removed[j]));
        }
    }

    private static void assertDeltaAddedEntries(BuildpathDelta delta, IPath[] paths) {
    	List addedEntries= delta.getAddedEntries();
    	assertTrue("Expected " + paths.length + " is " + addedEntries.size(), addedEntries.size() == paths.length);
    	IPath[] added= new IPath[addedEntries.size()];
    	int i= 0;
    	for (Iterator iterator= addedEntries.iterator(); iterator.hasNext();) {
	        CPListElement element= (CPListElement)iterator.next();
	        added[i]= element.getPath();
	        i++;
        }
    	for (int j= 0; j < paths.length; j++) {
	        assertTrue("Entry " + paths[j] + " was not added", contains(added, paths[j]));
        }
    	for (int j= 0; j < added.length; j++) {
	        assertTrue("Entry " + added[j] + " was added", contains(paths, added[j]));
        }
    }

    private static boolean contains(IPath[] paths, IPath path) {
    	for (int i= 0; i < paths.length; i++) {
	        if (paths[i].equals(path))
	        	return true;
        }
	    return false;
    }

    private static void assertDeltaDefaultOutputFolder(BuildpathDelta delta, IPath expectedLocation) {
    	IPath location= delta.getDefaultOutputLocation();
		assertTrue("Default output location is " + location + " expected was " + expectedLocation, location.equals(expectedLocation));
    }

    private static void assertNumberOfEntries(IClasspathEntry[] entries, int expected) {
    	assertTrue("Expected count was " + expected + " is " + entries.length, expected == entries.length);
    }

	public void testAddExternalJar01AddMyLib() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);

		IPath[] jarPaths= new IPath[] {JavaProjectHelper.MYLIB.makeAbsolute()};

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		IStatus status= ClasspathModifier.checkAddExternalJarsPrecondition(jarPaths, cpProject);
		assertTrue(status.isOK());

		BuildpathDelta delta= ClasspathModifier.addExternalJars(jarPaths, cpProject);
		assertDeltaResources(delta, new IPath[0], new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getOutputLocation());
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, jarPaths);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		assertIsOnBuildpath(classpathEntries, JavaProjectHelper.MYLIB.makeAbsolute());
	}

	public void testAddExternalJar02AddMuiltiLibs() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);

		IPath[] jarPaths= new IPath[] {
				JavaProjectHelper.MYLIB.makeAbsolute(),
				JavaProjectHelper.NLS_LIB.makeAbsolute()
		};

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		IStatus status= ClasspathModifier.checkAddExternalJarsPrecondition(jarPaths, cpProject);
		assertTrue(status.isOK());

		BuildpathDelta delta= ClasspathModifier.addExternalJars(jarPaths, cpProject);
		assertDeltaResources(delta, new IPath[0], new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getOutputLocation());
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, jarPaths);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 3);
		assertIsOnBuildpath(classpathEntries, JavaProjectHelper.MYLIB.makeAbsolute());
		assertIsOnBuildpath(classpathEntries, JavaProjectHelper.NLS_LIB.makeAbsolute());
	}

	public void testAddExternalJar02AddMuiltiLibsTwice() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);

		IPath[] jarPaths= new IPath[] {JavaProjectHelper.MYLIB.makeAbsolute()};

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		ClasspathModifier.addExternalJars(jarPaths, cpProject);
		ClasspathModifier.commitClassPath(cpProject, null);

		jarPaths= new IPath[] {
				JavaProjectHelper.MYLIB.makeAbsolute(),
				JavaProjectHelper.NLS_LIB.makeAbsolute()
		};

		cpProject= CPJavaProject.createFromExisting(fJavaProject);
		IStatus status= ClasspathModifier.checkAddExternalJarsPrecondition(jarPaths, cpProject);
		assertTrue(status.getMessage(), status.getSeverity() == IStatus.INFO);

		BuildpathDelta delta= ClasspathModifier.addExternalJars(jarPaths, cpProject);
		assertDeltaResources(delta, new IPath[0], new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getOutputLocation());
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[] {JavaProjectHelper.NLS_LIB.makeAbsolute()});

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 3);
		assertIsOnBuildpath(classpathEntries, JavaProjectHelper.MYLIB.makeAbsolute());
		assertIsOnBuildpath(classpathEntries, JavaProjectHelper.NLS_LIB.makeAbsolute());
	}

	public void testAddExternalJarBug132827() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);

		IPath[] jarPaths= new IPath[] {JavaProjectHelper.MYLIB.makeAbsolute()};

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		ClasspathModifier.addExternalJars(jarPaths, cpProject);
		ClasspathModifier.commitClassPath(cpProject, null);

		cpProject= CPJavaProject.createFromExisting(fJavaProject);
		IStatus status= ClasspathModifier.checkAddExternalJarsPrecondition(jarPaths, cpProject);
		assertTrue(status.getSeverity() == IStatus.INFO);

		BuildpathDelta delta= ClasspathModifier.addExternalJars(jarPaths, cpProject);
		assertDeltaResources(delta, new IPath[0], new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getOutputLocation());
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		assertIsOnBuildpath(classpathEntries, JavaProjectHelper.MYLIB.makeAbsolute());
	}

	public void testEditOutputFolder01SetOutputFolderForSourceFolder() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(fJavaProject, "src");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append("srcbin");

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertTrue(status.getMessage(), status.getSeverity() != IStatus.ERROR);

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getOutputLocation());
		assertDeltaAddedEntries(delta, new IPath[0]);
		assertDeltaRemovedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertTrue(src.getRawClasspathEntry() == entry);
		IPath location= entry.getOutputLocation();
		assertTrue("Output path is " + location + " expected was " + outputPath, outputPath.equals(location));
	}

	public void testEditOutputFolder02RemoveProjectAsSourceFolder() throws Exception {
		fJavaProject= createProject(null);
		JavaProjectHelper.addSourceContainer(fJavaProject, null, new IPath[] {new Path("src/")});
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(fJavaProject, "src");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append("srcbin");

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertTrue(status.getMessage(), status.getSeverity() == IStatus.INFO);

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[] {fJavaProject.getPath()});
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		assertTrue("Default output folder was not set to bin", fJavaProject.getOutputLocation().equals(fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME)));

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertTrue(src.getRawClasspathEntry() == entry);
		IPath location= entry.getOutputLocation();
		assertTrue("Output path is " + location + " expected was " + outputPath, outputPath.equals(location));
	}

	public void testEditOutputFolder03ExcludeOutputFolderSelf() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src1= JavaProjectHelper.addSourceContainer(fJavaProject, "src1");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append("src1").append(DEFAULT_OUTPUT_FOLDER_NAME);

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertTrue(status.getMessage(), status.getSeverity() != IStatus.ERROR);

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertTrue(src1.getRawClasspathEntry() == entry);
		IPath location= entry.getOutputLocation();
		assertTrue("Output path is " + location + " expected was " + outputPath, outputPath.equals(location));
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertTrue(exclusionPatterns.length == 1);
		assertTrue(exclusionPatterns[0].toString().equals("bin/"));
	}

	public void testEditOutputFolder03ExcludeOutputFolderOther() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src1= JavaProjectHelper.addSourceContainer(fJavaProject, "src1");
		IPackageFragmentRoot src2= JavaProjectHelper.addSourceContainer(fJavaProject, "src2");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append("src2").append(DEFAULT_OUTPUT_FOLDER_NAME);

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertTrue(status.getMessage(), status.getSeverity() != IStatus.ERROR);

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 3);
		IClasspathEntry entry= classpathEntries[1];
		assertTrue(src1.getRawClasspathEntry() == entry);
		IPath location= entry.getOutputLocation();
		assertTrue("Output path is " + location + " expected was " + outputPath, outputPath.equals(location));

		entry= classpathEntries[2];
		assertTrue(src2.getRawClasspathEntry() == entry);
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertTrue(exclusionPatterns.length == 1);
		assertTrue(exclusionPatterns[0].toString().equals("bin/"));
	}

	public void testEditOutputFolder04RemoveProjectAndExcludeOutput() throws Exception {
		fJavaProject= createProject(null);
		JavaProjectHelper.addSourceContainer(fJavaProject, null, new IPath[] {new Path("src1/"), new Path("src2/")});
		IPackageFragmentRoot src1= JavaProjectHelper.addSourceContainer(fJavaProject, "src1");
		IPackageFragmentRoot src2= JavaProjectHelper.addSourceContainer(fJavaProject, "src2");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append("src2").append(DEFAULT_OUTPUT_FOLDER_NAME);

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertTrue(status.getMessage(), status.getSeverity() == IStatus.INFO);

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[] {fJavaProject.getPath()});
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		assertTrue("Default output folder was not set to bin", fJavaProject.getOutputLocation().equals(fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME)));

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 3);
		IClasspathEntry entry= classpathEntries[1];
		assertTrue(src1.getRawClasspathEntry() == entry);
		IPath location= entry.getOutputLocation();
		assertTrue("Output path is " + location + " expected was " + outputPath, outputPath.equals(location));

		entry= classpathEntries[2];
		assertTrue(src2.getRawClasspathEntry() == entry);
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertTrue(exclusionPatterns.length == 1);
		assertTrue(exclusionPatterns[0].toString().equals("bin/"));
	}

	public void testEditOutputFolder05CannotOutputToSource() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src1= JavaProjectHelper.addSourceContainer(fJavaProject, "src1");
		JavaProjectHelper.addSourceContainer(fJavaProject, "src2");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append("src2");

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertTrue(status.getMessage(), status.getSeverity() == IStatus.ERROR);
	}

	public void testEditOutputFolderBug153068() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(fJavaProject, "src");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append(".");

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertTrue(status.getMessage(), status.getSeverity() == IStatus.ERROR);
	}

	public void testEditOutputFolderBug154044() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src1= JavaProjectHelper.addSourceContainer(fJavaProject, "src1");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath oldOutputPath= projectPath.append("src1").append("sub").append(DEFAULT_OUTPUT_FOLDER_NAME);

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		ClasspathModifier.setOutputLocation(element, oldOutputPath, false, cpProject);
		ClasspathModifier.commitClassPath(cpProject, null);

		IPath outputPath= projectPath.append("src1").append("sub").append("newBin");

		cpProject= CPJavaProject.createFromExisting(fJavaProject);
		element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertTrue(status.getMessage(), status.isOK());

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[] {oldOutputPath}, new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertTrue(src1.getRawClasspathEntry() == entry);
		IPath location= entry.getOutputLocation();
		assertTrue("Output path is " + location + " expected was " + outputPath, outputPath.equals(location));
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertTrue(exclusionPatterns.length == 1);
		assertTrue(exclusionPatterns[0].toString(), exclusionPatterns[0].toString().equals("sub/newBin/"));
	}

	public void testEditOutputFolderBug154044OldIsProject() throws Exception {
		fJavaProject= createProject(null);
		IPackageFragmentRoot p01= JavaProjectHelper.addSourceContainer(fJavaProject, null);

		IPath projectPath= fJavaProject.getProject().getFullPath();

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(p01.getRawClasspathEntry(), fJavaProject));
		ClasspathModifier.setOutputLocation(element, projectPath, false, cpProject);
		ClasspathModifier.commitClassPath(cpProject, null);

		IPath outputPath= projectPath.append(DEFAULT_OUTPUT_FOLDER_NAME);

		cpProject= CPJavaProject.createFromExisting(fJavaProject);
		element= cpProject.getCPElement(CPListElement.createFromExisting(p01.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertTrue(status.getMessage(), status.isOK());

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath());
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertTrue(p01.getRawClasspathEntry() == entry);
		IPath location= entry.getOutputLocation();
		assertTrue("Output path is " + location + " expected was " + outputPath, outputPath.equals(location));
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertTrue(exclusionPatterns.length == 1);
		assertTrue(exclusionPatterns[0].toString(), exclusionPatterns[0].toString().equals("bin/"));
	}

	public void testEditOutputFolderBug154044DonotDeleteDefaultOutputFolder() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src1= JavaProjectHelper.addSourceContainer(fJavaProject, "src1");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath oldOutputPath= projectPath.append(DEFAULT_OUTPUT_FOLDER_NAME);

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		ClasspathModifier.setOutputLocation(element, oldOutputPath, false, cpProject);
		ClasspathModifier.commitClassPath(cpProject, null);

		IPath outputPath= projectPath.append("bin1");

		cpProject= CPJavaProject.createFromExisting(fJavaProject);
		element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertTrue(status.getMessage(), status.isOK());

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertTrue(src1.getRawClasspathEntry() == entry);
		IPath location= entry.getOutputLocation();
		assertTrue("Output path is " + location + " expected was " + outputPath, outputPath.equals(location));
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertTrue(exclusionPatterns.length == 0);
	}

	public void testEditOutputFolderBug154196() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src1= JavaProjectHelper.addSourceContainer(fJavaProject, "src1");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath oldOutputPath= projectPath.append("binary");

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		ClasspathModifier.setOutputLocation(element, oldOutputPath, false, cpProject);
		ClasspathModifier.commitClassPath(cpProject, null);

		cpProject= CPJavaProject.createFromExisting(fJavaProject);
		element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, null, false, cpProject);
		assertTrue(status.getMessage(), status.isOK());

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, null, false, cpProject);
		assertDeltaResources(delta, new IPath[0], new IPath[] {oldOutputPath}, new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertTrue(src1.getRawClasspathEntry() == entry);
		IPath location= entry.getOutputLocation();
		assertTrue(location == null);
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertTrue(exclusionPatterns.length == 0);
	}

	public void testRemoveFromBuildpath01() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src1= JavaProjectHelper.addSourceContainer(fJavaProject, "src1");
		IPackageFragmentRoot src2= JavaProjectHelper.addSourceContainer(fJavaProject, "src2");

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement[] toRemove= new CPListElement[2];
		toRemove[0]= CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject);
		toRemove[1]= CPListElement.createFromExisting(src2.getRawClasspathEntry(), fJavaProject);

		BuildpathDelta delta= ClasspathModifier.removeFromBuildpath(toRemove, cpProject);
		assertDeltaResources(delta, new IPath[0], new IPath[] {src1.getPath(), src2.getPath()}, new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[] {src1.getPath(), src2.getPath()});
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 1);
	}

	public void testRemoveFromBuildpath01RemoveProject() throws Exception {
		fJavaProject= createProject(null);
		IPackageFragmentRoot p01= JavaProjectHelper.addSourceContainer(fJavaProject, null);

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);

		BuildpathDelta delta= ClasspathModifier.removeFromBuildpath(new CPListElement[] {CPListElement.createFromExisting(p01.getRawClasspathEntry(), fJavaProject)}, cpProject);
		assertDeltaResources(delta, new IPath[0], new IPath[] {}, new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath());
		assertDeltaRemovedEntries(delta, new IPath[] {p01.getPath()});
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 1);
	}

	public void testRemoveFromBuildpath01RemoveLibs() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);

		IPath[] jarPaths= new IPath[] {
				JavaProjectHelper.MYLIB.makeAbsolute(),
				JavaProjectHelper.JUNIT_SRC_381.makeAbsolute()
		};

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		BuildpathDelta delta= ClasspathModifier.addExternalJars(jarPaths, cpProject);
		ClasspathModifier.commitClassPath(cpProject, null);

		CPListElement[] toRemove = {
				(CPListElement)delta.getAddedEntries().get(0),
				(CPListElement)delta.getAddedEntries().get(1)
		};

		delta= ClasspathModifier.removeFromBuildpath(toRemove, cpProject);
		assertDeltaResources(delta, new IPath[0], new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[] {toRemove[0].getPath(), toRemove[1].getPath()});
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 1);
	}

	public void testRemoveFromBuildpathBug153299Src() throws Exception {
		fJavaProject= createProject(null);
		IPackageFragmentRoot p01= JavaProjectHelper.addSourceContainer(fJavaProject, null, new IPath[] {new Path("src1/")});
		JavaProjectHelper.addSourceContainer(fJavaProject, "src1");

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);

		BuildpathDelta delta= ClasspathModifier.removeFromBuildpath(new CPListElement[] {CPListElement.createFromExisting(p01.getRawClasspathEntry(), fJavaProject)}, cpProject);
		assertDeltaResources(delta, new IPath[0], new IPath[] {}, new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[] {p01.getPath()});
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
	}

	public void testRemoveFromBuildpathBug153299Lib() throws Exception {
		fJavaProject= createProject(null);
		IPackageFragmentRoot p01= JavaProjectHelper.addSourceContainer(fJavaProject, null, new IPath[] {new Path("src1/")});

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		IPath[] jarPaths= new IPath[] {JavaProjectHelper.MYLIB.makeAbsolute()};

		ClasspathModifier.addExternalJars(jarPaths, cpProject);
		ClasspathModifier.commitClassPath(cpProject, null);

		cpProject= CPJavaProject.createFromExisting(fJavaProject);

		BuildpathDelta delta= ClasspathModifier.removeFromBuildpath(new CPListElement[] {CPListElement.createFromExisting(p01.getRawClasspathEntry(), fJavaProject)}, cpProject);
		assertDeltaResources(delta, new IPath[0], new IPath[] {}, new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath());
		assertDeltaRemovedEntries(delta, new IPath[] {p01.getPath()});
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
	}
}