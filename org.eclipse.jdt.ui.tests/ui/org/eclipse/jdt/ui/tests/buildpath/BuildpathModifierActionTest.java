/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.buildpath;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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

public class BuildpathModifierActionTest {

    private static final String DEFAULT_OUTPUT_FOLDER_NAME= "bin";
	private static final String PROJECT_NAME= "P01";

	private IJavaProject fJavaProject;

	@AfterEach
	public void tearDown() throws Exception {
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
		for (IClasspathEntry entry : entries) {
			if (entry.getPath().equals(path)) {
				return;
			}
		}
		fail("Element with location " + path + " is not on buildpath");
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
	        	assertTrue(contains(createdFiles, path), "File " + createdResources[i] + " is unexpected created");
	        } else if (createdResources[i] instanceof IFolder) {
	        	assertTrue(contains(createdFolders, path), "Folder " + createdResources[i] + " is unexpected created");
	        } else {
	        	fail("Resource " + createdResources[i] + " is nor file nor folder.");
	        }
        }
		for (IPath createdFolder : createdFolders) {
			assertTrue(contains(createdPaths, createdFolder), "Folder at " + createdFolder + " was not created");
		}
		for (IPath createdFile : createdFiles) {
			assertTrue(contains(createdPaths, createdFile), "File at " + createdFile + " was not created");
		}

    	IResource[] deletedResources= delta.getDeletedResources();
    	IPath[] deletedPaths= new IPath[deletedResources.length];
    	for (int i= 0; i < deletedResources.length; i++) {
	        deletedPaths[i]= deletedResources[i].getFullPath();
        }

    	for (int i= 0; i < deletedPaths.length; i++) {
	        IPath path= deletedPaths[i];
			if (deletedResources[i] instanceof IFile) {
	        	assertTrue(contains(removedFiles, path), "File " + deletedResources[i] + " is unexpected removed");
	        } else if (deletedResources[i] instanceof IFolder) {
	        	assertTrue(contains(removedFolders, path), "Folder " + deletedResources[i] + " is unexpected removed");
	        } else {
	        	fail("Resource " + deletedResources[i] + " is nor file nor folder.");
	        }
        }
		for (IPath removedFolder : removedFolders) {
			assertTrue(contains(deletedPaths, removedFolder), "Folder at " + removedFolder + " was not removed");
		}
		for (IPath removedFile : removedFiles) {
			assertTrue(contains(deletedPaths, removedFile), "File at " + removedFile + " was not removed");
		}
    }

    private static void assertDeltaRemovedEntries(BuildpathDelta delta, IPath[] paths) {
    	List<CPListElement> removedEntries= delta.getRemovedEntries();
    	assertEquals(removedEntries.size(), paths.length, "Expected " + paths.length + " is " + removedEntries.size());
    	IPath[] removed= new IPath[removedEntries.size()];
    	int i= 0;
    	for (CPListElement element : removedEntries) {
	        removed[i]= element.getPath();
	        i++;
        }
		for (IPath path : paths) {
			assertTrue(contains(removed, path), "Entry " + path + " was not removed");
		}
		for (IPath removed1 : removed) {
			assertTrue(contains(paths, removed1), "Entry " + removed1 + " was removed");
		}
    }

    private static void assertDeltaAddedEntries(BuildpathDelta delta, IPath[] paths) {
    	List<CPListElement> addedEntries= delta.getAddedEntries();
    	assertEquals(addedEntries.size(), paths.length, "Expected " + paths.length + " is " + addedEntries.size());
    	IPath[] added= new IPath[addedEntries.size()];
    	int i= 0;
    	for (CPListElement element : addedEntries) {
	        added[i]= element.getPath();
	        i++;
        }
		for (IPath path : paths) {
			assertTrue(contains(added, path), "Entry " + path + " was not added");
		}
		for (IPath add : added) {
			assertTrue(contains(paths, add), "Entry " + add + " was added");
		}
    }

    private static boolean contains(IPath[] paths, IPath path) {
		for (IPath p : paths) {
			if (p.equals(path)) {
				return true;
			}
		}
	    return false;
    }

    private static void assertDeltaDefaultOutputFolder(BuildpathDelta delta, IPath expectedLocation) {
    	IPath location= delta.getDefaultOutputLocation();
		assertTrue(location.equals(expectedLocation), "Default output location is " + location + " expected was " + expectedLocation);
    }

    private static void assertNumberOfEntries(IClasspathEntry[] entries, int expected) {
    	assertEquals(expected, entries.length, "Expected count was " + expected + " is " + entries.length);
    }

    @Test
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

    @Test
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

    @Test
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
		assertEquals(IStatus.INFO, status.getSeverity(), status.getMessage());

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

    @Test
	public void testAddExternalJarBug132827() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);

		IPath[] jarPaths= new IPath[] {JavaProjectHelper.MYLIB.makeAbsolute()};

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		ClasspathModifier.addExternalJars(jarPaths, cpProject);
		ClasspathModifier.commitClassPath(cpProject, null);

		cpProject= CPJavaProject.createFromExisting(fJavaProject);
		IStatus status= ClasspathModifier.checkAddExternalJarsPrecondition(jarPaths, cpProject);
		assertEquals(IStatus.INFO, status.getSeverity());

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

    @Test
	public void testEditOutputFolder01SetOutputFolderForSourceFolder() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(fJavaProject, "src");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append("srcbin");

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertNotEquals(IStatus.ERROR, status.getSeverity(), status.getMessage());

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getOutputLocation());
		assertDeltaAddedEntries(delta, new IPath[0]);
		assertDeltaRemovedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertSame(src.getRawClasspathEntry(), entry);
		IPath location= entry.getOutputLocation();
		assertTrue(outputPath.equals(location), "Output path is " + location + " expected was " + outputPath);
	}

    @Test
	public void testEditOutputFolder02RemoveProjectAsSourceFolder() throws Exception {
		fJavaProject= createProject(null);

		// Use the old behavior in order to test the fallback code. Set to ERROR since 3.8.
		fJavaProject.setOption(JavaCore.CORE_OUTPUT_LOCATION_OVERLAPPING_ANOTHER_SOURCE, JavaCore.IGNORE);

		JavaProjectHelper.addSourceContainer(fJavaProject, null, new IPath[] {new Path("src/")});
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(fJavaProject, "src");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append("srcbin");

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertEquals(IStatus.INFO, status.getSeverity(), status.getMessage());

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[] {fJavaProject.getPath()});
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		assertTrue(fJavaProject.getOutputLocation().equals(fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME)), "Default output folder was not set to bin");

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertSame(src.getRawClasspathEntry(), entry);
		IPath location= entry.getOutputLocation();
		assertTrue(outputPath.equals(location), "Output path is " + location + " expected was " + outputPath);
	}

    @Test
	public void testEditOutputFolder03ExcludeOutputFolderSelf() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src1= JavaProjectHelper.addSourceContainer(fJavaProject, "src1");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append("src1").append(DEFAULT_OUTPUT_FOLDER_NAME);

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertNotEquals(IStatus.ERROR, status.getSeverity(), status.getMessage());

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertSame(src1.getRawClasspathEntry(), entry);
		IPath location= entry.getOutputLocation();
		assertTrue(outputPath.equals(location), "Output path is " + location + " expected was " + outputPath);
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertEquals(1, exclusionPatterns.length);
		assertEquals("bin/", exclusionPatterns[0].toString());
	}

    @Test
	public void testEditOutputFolder03ExcludeOutputFolderOther() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src1= JavaProjectHelper.addSourceContainer(fJavaProject, "src1");
		IPackageFragmentRoot src2= JavaProjectHelper.addSourceContainer(fJavaProject, "src2");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append("src2").append(DEFAULT_OUTPUT_FOLDER_NAME);

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertNotEquals(IStatus.ERROR, status.getSeverity(), status.getMessage());

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 3);
		IClasspathEntry entry= classpathEntries[1];
		assertSame(src1.getRawClasspathEntry(), entry);
		IPath location= entry.getOutputLocation();
		assertTrue(outputPath.equals(location), "Output path is " + location + " expected was " + outputPath);

		entry= classpathEntries[2];
		assertSame(src2.getRawClasspathEntry(), entry);
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertEquals(1, exclusionPatterns.length);
		assertEquals("bin/", exclusionPatterns[0].toString());
	}

    @Test
	public void testEditOutputFolder04RemoveProjectAndExcludeOutput() throws Exception {
		fJavaProject= createProject(null);

		// Use the old behavior in order to test the fallback code. Set to ERROR since 3.8.
		fJavaProject.setOption(JavaCore.CORE_OUTPUT_LOCATION_OVERLAPPING_ANOTHER_SOURCE, JavaCore.IGNORE);

		JavaProjectHelper.addSourceContainer(fJavaProject, null, new IPath[] {new Path("src1/"), new Path("src2/")});
		IPackageFragmentRoot src1= JavaProjectHelper.addSourceContainer(fJavaProject, "src1");
		IPackageFragmentRoot src2= JavaProjectHelper.addSourceContainer(fJavaProject, "src2");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append("src2").append(DEFAULT_OUTPUT_FOLDER_NAME);

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertEquals(IStatus.INFO, status.getSeverity(), status.getMessage());

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[] {fJavaProject.getPath()});
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		assertTrue(fJavaProject.getOutputLocation().equals(fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME)), "Default output folder was not set to bin");

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 3);
		IClasspathEntry entry= classpathEntries[1];
		assertSame(src1.getRawClasspathEntry(), entry);
		IPath location= entry.getOutputLocation();
		assertTrue(outputPath.equals(location), "Output path is " + location + " expected was " + outputPath);

		entry= classpathEntries[2];
		assertSame(src2.getRawClasspathEntry(), entry);
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertEquals(1, exclusionPatterns.length);
		assertEquals("bin/", exclusionPatterns[0].toString());
	}

    @Test
	public void testEditOutputFolder05CannotOutputToSource() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src1= JavaProjectHelper.addSourceContainer(fJavaProject, "src1");
		JavaProjectHelper.addSourceContainer(fJavaProject, "src2");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append("src2");

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src1.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertEquals(IStatus.ERROR, status.getSeverity(), status.getMessage());
	}

    @Test
	public void testEditOutputFolderBug153068() throws Exception {
		fJavaProject= createProject(DEFAULT_OUTPUT_FOLDER_NAME);
		IPackageFragmentRoot src= JavaProjectHelper.addSourceContainer(fJavaProject, "src");

		IPath projectPath= fJavaProject.getProject().getFullPath();
		IPath outputPath= projectPath.append(".");

		CPJavaProject cpProject= CPJavaProject.createFromExisting(fJavaProject);
		CPListElement element= cpProject.getCPElement(CPListElement.createFromExisting(src.getRawClasspathEntry(), fJavaProject));
		IStatus status= ClasspathModifier.checkSetOutputLocationPrecondition(element, outputPath, false, cpProject);
		assertEquals(IStatus.ERROR, status.getSeverity(), status.getMessage());
	}

    @Test
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
		assertTrue(status.isOK(), status.getMessage());

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[] {oldOutputPath}, new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertSame(src1.getRawClasspathEntry(), entry);
		IPath location= entry.getOutputLocation();
		assertTrue(outputPath.equals(location), "Output path is " + location + " expected was " + outputPath);
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertEquals(1, exclusionPatterns.length);
		assertEquals("sub/newBin/", exclusionPatterns[0].toString(), exclusionPatterns[0].toString());
	}

    @Test
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
		assertTrue(status.isOK(), status.getMessage());

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath());
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertSame(p01.getRawClasspathEntry(), entry);
		IPath location= entry.getOutputLocation();
		assertTrue(outputPath.equals(location), "Output path is " + location + " expected was " + outputPath);
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertEquals(1, exclusionPatterns.length);
		assertEquals("bin/", exclusionPatterns[0].toString(), exclusionPatterns[0].toString());
	}

    @Test
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
		assertTrue(status.isOK(), status.getMessage());

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, outputPath, false, cpProject);
		assertDeltaResources(delta, new IPath[] {outputPath}, new IPath[0], new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertSame(src1.getRawClasspathEntry(), entry);
		IPath location= entry.getOutputLocation();
		assertTrue(outputPath.equals(location), "Output path is " + location + " expected was " + outputPath);
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertEquals(0, exclusionPatterns.length);
	}

    @Test
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
		assertTrue(status.isOK(), status.getMessage());

		BuildpathDelta delta= ClasspathModifier.setOutputLocation(element, null, false, cpProject);
		assertDeltaResources(delta, new IPath[0], new IPath[] {oldOutputPath}, new IPath[0], new IPath[0]);
		assertDeltaDefaultOutputFolder(delta, fJavaProject.getPath().append(DEFAULT_OUTPUT_FOLDER_NAME));
		assertDeltaRemovedEntries(delta, new IPath[0]);
		assertDeltaAddedEntries(delta, new IPath[0]);

		ClasspathModifier.commitClassPath(cpProject, null);

		IClasspathEntry[] classpathEntries= fJavaProject.getRawClasspath();
		assertNumberOfEntries(classpathEntries, 2);
		IClasspathEntry entry= classpathEntries[1];
		assertSame(src1.getRawClasspathEntry(), entry);
		IPath location= entry.getOutputLocation();
		assertNull(location);
		IPath[] exclusionPatterns= entry.getExclusionPatterns();
		assertEquals(0, exclusionPatterns.length);
	}

    @Test
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

    @Test
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

    @Test
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
				delta.getAddedEntries().get(0),
				delta.getAddedEntries().get(1)
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

    @Test
	public void testRemoveFromBuildpathBug153299Src() throws Exception {
		fJavaProject= createProject(null);

		// Use the old behavior in order to test the fallback code. Set to ERROR since 3.8.
		fJavaProject.setOption(JavaCore.CORE_OUTPUT_LOCATION_OVERLAPPING_ANOTHER_SOURCE, JavaCore.IGNORE);

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

    @Test
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