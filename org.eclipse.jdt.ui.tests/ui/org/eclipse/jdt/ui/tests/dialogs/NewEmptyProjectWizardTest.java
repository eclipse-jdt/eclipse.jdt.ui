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
package org.eclipse.jdt.ui.tests.dialogs;


import java.lang.reflect.InvocationTargetException;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.BuildpathModifierAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.OutputFolderValidator;

public class NewEmptyProjectWizardTest extends NewProjectWizardTest {
    private IPath defaultOutputFolder;
    public static final Class THIS= NewEmptyProjectWizardTest.class;

    public NewEmptyProjectWizardTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        fProject= fTestSetup.getWorkspaceProject();
        defaultOutputFolder= fProject.getOutputLocation().append(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME));
        testProjectIsOnClasspath(true);
    }

    public void testCreateNormalFolderOnProject() throws CoreException, InvocationTargetException, InterruptedException {
        super.testCreateNormalFolderOnProject();
        IFolder folder= getNormalFolderCreationQuery().getCreatedFolder();
        assertTrue(ClasspathModifier.isExcluded(folder, fProject));
    }

    public void testCreateSourceFolderOnProjectWithProjAsRoot() throws CoreException, InvocationTargetException, InterruptedException {
        ClasspathModifierQueries.OutputFolderQuery outputFolderQuery= getOutputFolderQueryToKeepProjAsRoot();
        ClasspathModifierQueries.ICreateFolderQuery folderQuery= getSourceFolderCreationQuery();
        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.CREATE_FOLDER, null, outputFolderQuery, null, folderQuery, null);

        assertTrue(root.getUnderlyingResource().exists());
        assertTrue(root.getElementName().equals(fSubFolder));
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        testProjectIsOnClasspath(true);
        assertTrue(ClasspathModifier.isExcluded(fProject.getProject().findMember(root.getPath().removeFirstSegments(1)), fProject));

        validateClasspath();
    }

    public void testCreateSourceFolderOnProject() throws CoreException, InvocationTargetException, InterruptedException {
        // ... and remove project as root
        super.testCreateSourceFolderOnProject();
        IFolder folder= getSourceFolderCreationQuery().getCreatedFolder();
        assertFalse(ClasspathModifier.getClasspathEntryFor(folder.getFullPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        testProjectIsOnClasspath(false);
    }

    public void testCreateSourceFolderOnFragRootWithProjAsRoot() throws CoreException, InvocationTargetException, InterruptedException {
        ClasspathModifierQueries.OutputFolderQuery outputFolderQuery= getOutputFolderQueryToKeepProjAsRoot();
        IPackageFragmentRoot parentRoot= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, getFolderHandle(new Path(fNormalFolder)), outputFolderQuery, null, null, null);
        assertTrue(parentRoot != null);
        testProjectIsOnClasspath(true);
        ClasspathModifierQueries.ICreateFolderQuery folderQuery= new ClasspathModifierQueries.ICreateFolderQuery() {

            public boolean doQuery() {
                return true;
            }

            public boolean isSourceFolder() {
                return true;
            }

            public IFolder getCreatedFolder() {
                return getFolderHandle(new Path(fNormalFolder).append(fSubFolder));
            }

        };
        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.CREATE_FOLDER, null, outputFolderQuery, null, folderQuery, null);
        testProjectIsOnClasspath(true);
        assertTrue(root.getUnderlyingResource().exists());
        assertTrue(root.getParent().equals(fProject));
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);

        validateClasspath();
    }

    public void testCreateSourceFolderOnFragRoot() throws CoreException, InvocationTargetException, InterruptedException {
        // ... and remove project as root
        // first add a source folder, but keep project as root
        ClasspathModifierQueries.OutputFolderQuery outputFolderQuery= getOutputFolderQueryToKeepProjAsRoot();
        IPackageFragmentRoot parentRoot= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, getFolderHandle(new Path(fNormalFolder)), outputFolderQuery, null, null, null);
        testProjectIsOnClasspath(true);

        // now create a child of this source folder and remove the project as root
        outputFolderQuery= getOutputFolderQueryInternal(fProject.getPath()); // To be able to remove the project, we have to pretend that our
        // desired output location for the project is the project root itself, because the output location already changed when
        // executing adding to the buildpath (it is not possible to have a source folder if the output location is equal to the project folder).
        ClasspathModifierQueries.ICreateFolderQuery folderQuery= new ClasspathModifierQueries.ICreateFolderQuery() {

            public boolean doQuery() {
                return true;
            }

            public boolean isSourceFolder() {
                return true;
            }

            public IFolder getCreatedFolder() {
                return getFolderHandle(new Path(fNormalFolder).append(fSubFolder));
            }

        };
        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.CREATE_FOLDER, null, outputFolderQuery, null, folderQuery, null);
        assertTrue(root.getUnderlyingResource().exists());
        assertTrue(root.getUnderlyingResource().getParent().equals(parentRoot.getUnderlyingResource()));
        assertTrue(root.getParent().equals(fProject));
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        testProjectIsOnClasspath(false);

        validateClasspath();
    }

    public void testCreateNormalFolderOnFragRootWithProjAsRoot() throws CoreException, InvocationTargetException, InterruptedException {
        ClasspathModifierQueries.OutputFolderQuery outputFolderQuery= getOutputFolderQueryToKeepProjAsRoot();
        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, getFolderHandle(new Path(fNormalFolder)), outputFolderQuery, null, null, null);
        ClasspathModifierQueries.ICreateFolderQuery folderQuery= new ClasspathModifierQueries.ICreateFolderQuery() {

            public boolean doQuery() {
                return true;
            }

            public boolean isSourceFolder() {
                return false;
            }

            public IFolder getCreatedFolder() {
                return getFolderHandle(new Path(fNormalFolder).append(fSubFolder));
            }

        };
        IFolder folder= (IFolder)executeOperation(BuildpathModifierAction.CREATE_FOLDER, null, outputFolderQuery, null, folderQuery, null);
        assertTrue(folder.getParent().equals(root.getUnderlyingResource()));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testCreateSourceFolderOnFrag() throws CoreException, InvocationTargetException, InterruptedException {
        // ... and remove project as root
        final IPath srcPath= new Path("src");
        ClasspathModifierQueries.OutputFolderQuery outputFolderQuery= getOutputFolderQueryToKeepProjAsRoot();
        IPackageFragmentRoot parentRoot= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, getFolderHandle(srcPath), outputFolderQuery, null, null, null);
        IFolder fragmentFolder= getFolderHandle(srcPath.append(fNormalFolder));
        assertTrue(fragmentFolder.getParent().equals(parentRoot.getUnderlyingResource()));
        testProjectIsOnClasspath(true);

        ClasspathModifierQueries.ICreateFolderQuery folderQuery= new ClasspathModifierQueries.ICreateFolderQuery() {

            public boolean doQuery() {
                return true;
            }

            public boolean isSourceFolder() {
                return true;
            }

            public IFolder getCreatedFolder() {
                return getFolderHandle(srcPath.append(fNormalFolder).append(fSubFolder));
            }

        };
        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.CREATE_FOLDER, null, getOutputFolderQueryInternal(fProject.getPath()), null, folderQuery, null);
        assertTrue(root.getUnderlyingResource().exists());
        assertTrue(root.getParent().equals(fProject));
        assertTrue(root.getUnderlyingResource().getParent().equals(fragmentFolder));
        testProjectIsOnClasspath(false);

        validateClasspath();
    }

    public void testCreateSourceFolderOnFragWithProjAsRoot() throws CoreException, InvocationTargetException, InterruptedException {
        super.testCreateSourceFolderOnFrag();
        testProjectIsOnClasspath(false);
    }

    public void testCreateNormalFolderOnFrag() throws CoreException, InvocationTargetException, InterruptedException {
        super.testCreateNormalFolderOnFrag();
        testProjectIsOnClasspath(false);
    }

    public void testCreateNormalFolderOnFragWithProjAsRoot() throws CoreException, InvocationTargetException, InterruptedException {
        final IPath srcPath= new Path("src");
        ClasspathModifierQueries.OutputFolderQuery outputFolderQuery= getOutputFolderQueryToKeepProjAsRoot();
        IPackageFragmentRoot parentRoot= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, getFolderHandle(srcPath), outputFolderQuery, null, null, null);
        IFolder fragmentFolder= getFolderHandle(srcPath.append(fNormalFolder));
        assertTrue(fragmentFolder.getParent().equals(parentRoot.getUnderlyingResource()));
        testProjectIsOnClasspath(true);

        ClasspathModifierQueries.ICreateFolderQuery folderQuery= new ClasspathModifierQueries.ICreateFolderQuery() {

            public boolean doQuery() {
                return true;
            }

            public boolean isSourceFolder() {
                return false;
            }

            public IFolder getCreatedFolder() {
                return getFolderHandle(srcPath.append(fNormalFolder).append(fSubFolder));
            }

        };

        IFolder folder= (IFolder)executeOperation(BuildpathModifierAction.CREATE_FOLDER, null, getOutputFolderQueryToKeepProjAsRoot(), null, folderQuery, null);
        assertTrue(folder.getParent().equals(fragmentFolder));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    // Test adding/removing to classpath
    public void testAddProjectToCPAndKeepDefaultOutputLocation() throws CoreException, InvocationTargetException, InterruptedException {
        // first we need to remove the project from the classpath
        testRemoveProjectToCPAndKeepDefaultOutputLocation();

        // then we add it again
        IPath[] paths= getPaths();
        assertFalse(contains(fProject.getPath(), paths, null));

        IJavaProject project= (IJavaProject)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, fProject, getOutputFolderQueryInternal(fProject.getOutputLocation()), null, null, null);

        paths= getPaths();
        assertTrue(contains(fProject.getPath(), paths, null));
        assertTrue(project.equals(fProject));

        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testAddNormalFolderToCP() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        // ... and remove project as root
        int numberOfEntries= fProject.getRawClasspath().length;

        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));

        int newNumberOfEntries= fProject.getRawClasspath().length;
        // the number remains equal because we removed the project
        // as root and added another src folder
        assertTrue(numberOfEntries == newNumberOfEntries);
        assertTrue(root.getParent().equals(fProject));
        testProjectIsOnClasspath(false);

        validateClasspath();
    }

    public void testAddNormalFolderToCPWithProjAsRoot() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        int numberOfEntries= fProject.getRawClasspath().length;
        IFolder folder= getFolderHandle(new Path(fNormalFolder));

        IPath[] paths= getPaths();
        assertFalse(contains(folder.getFullPath(), paths, null));
        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, folder, getOutputFolderQueryToKeepProjAsRoot(), null, null, null);

        paths= getPaths();
        assertTrue(contains(folder.getFullPath(), getPaths(), null));

        int newNumberOfEntries= fProject.getRawClasspath().length;
        assertTrue(numberOfEntries + 1 == newNumberOfEntries);
        assertTrue(root.getParent().equals(fProject));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testAddNestedNormalFolderToCP() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IFolder cpFolder= getFolderHandle(new Path(fNormalFolder));
        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, cpFolder, getOutputFolderQueryToKeepProjAsRoot(), null, null, null);

        IFolder folder= getFolderHandle(cpFolder.getProjectRelativePath().append(fSubFolder));
        IPackageFragment fragment= root.getPackageFragment(folder.getName());

        IClasspathEntry entry= root.getRawClasspathEntry();
        int nrExcluded= entry.getExclusionPatterns().length;
        folder= (IFolder)executeOperation(BuildpathModifierAction.EXCLUDE, fragment, null, null, null, null);
        assertTrue(folder.getFullPath().equals(fragment.getPath()));

        entry= root.getRawClasspathEntry();
        IPath[] exclusionPatterns= entry.getExclusionPatterns();
        assertTrue(nrExcluded + 1 == exclusionPatterns.length);
        assertTrue(contains(new Path(fragment.getElementName()), exclusionPatterns, null));

        IPackageFragmentRoot newRoot= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, folder, getOutputFolderQueryInternal(defaultOutputFolder), null, null, null);
        assertTrue(newRoot.getPath().equals(folder.getFullPath()));

        entry= root.getRawClasspathEntry();
        assertTrue(contains(new Path(folder.getName()), entry.getExclusionPatterns(), null));
        assertFalse(ClasspathModifier.getClasspathEntryFor(folder.getFullPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testAddNestedNormalFolderToCPWithProjAsRoot() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        // ... and remove project as root
        IFolder cpFolder= getFolderHandle(new Path(fNormalFolder));
        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, cpFolder, getOutputFolderQueryToKeepProjAsRoot(), null, null, null);

        IFolder folder= getFolderHandle(cpFolder.getProjectRelativePath().append(fSubFolder));
        IPackageFragment fragment= root.getPackageFragment(folder.getName());

        IClasspathEntry entry= root.getRawClasspathEntry();
        int nrExcluded= entry.getExclusionPatterns().length;
        executeOperation(BuildpathModifierAction.EXCLUDE, fragment, null, null, null, null);

        entry= root.getRawClasspathEntry();
        IPath[] exclusionPatterns= entry.getExclusionPatterns();
        assertTrue(nrExcluded + 1 == exclusionPatterns.length);
        assertTrue(contains(new Path(fragment.getElementName()), exclusionPatterns, null));

        executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, folder, getOutputFolderQueryInternal(fProject.getPath()), null, null, null);

        entry= root.getRawClasspathEntry();
        assertTrue(contains(new Path(folder.getName()), entry.getExclusionPatterns(), null));
        assertFalse(ClasspathModifier.getClasspathEntryFor(folder.getFullPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        testProjectIsOnClasspath(false);

        validateClasspath();
    }

    public void testAddPackageToCP() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        // ... and remove project as root
        IPackageFragmentRoot parentRoot= createFragmentRootAndKeepProjAsRoot();
        getFolderHandle(parentRoot.getPath().removeFirstSegments(1).append(fSubFolder)); // because add to buildpath requires the fragments underlying resource to exist
        IPackageFragment fragment= parentRoot.getPackageFragment(fSubFolder);
        IClasspathEntry entry= parentRoot.getRawClasspathEntry();

        int nrExclusions= entry.getExclusionPatterns().length;
        assertFalse(contains(new Path(fragment.getElementName()), entry.getExclusionPatterns(), null));

        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, fragment, getOutputFolderQueryInternal(fProject.getPath()), null, null, null);

        entry= parentRoot.getRawClasspathEntry();
        assertTrue(contains(new Path(fragment.getElementName()), entry.getExclusionPatterns(), null));
        assertTrue(entry.getExclusionPatterns().length - 1 == nrExclusions);
        assertTrue(root.getParent().equals(fProject));
        testProjectIsOnClasspath(false);

        validateClasspath();
    }

    public void testAddPackageToCPWithProjAsRoot() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot parentRoot= createFragmentRootAndKeepProjAsRoot();
        getFolderHandle(parentRoot.getPath().removeFirstSegments(1).append(fSubFolder)); // because add to buildpath requires the fragments underlying resource to exist
        IPackageFragment fragment= parentRoot.getPackageFragment(fSubFolder);
        IClasspathEntry entry= parentRoot.getRawClasspathEntry();

        int nrExclusions= entry.getExclusionPatterns().length;
        assertFalse(contains(new Path(fragment.getElementName()), entry.getExclusionPatterns(), null));

        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, fragment, getOutputFolderQueryToKeepProjAsRoot(), null, null, null);

        entry= parentRoot.getRawClasspathEntry();
        assertTrue(contains(new Path(fragment.getElementName()), entry.getExclusionPatterns(), null));
        assertTrue(entry.getExclusionPatterns().length - 1 == nrExclusions);
        assertTrue(root.getParent().equals(fProject));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    // TODO refine + tests for project as root
	public void testAddJarFileToCP() throws InvocationTargetException, InterruptedException, CoreException {
        super.testAddJarFileToCP();
        testProjectIsOnClasspath(false);
    }

    public void testAddJarFileToCPWithProjAsRoot() throws InvocationTargetException, InterruptedException, CoreException {
        // create root parent for jar file
        IPackageFragmentRoot parentRoot= createFragmentRootAndKeepProjAsRoot();
        IPath libraryPath= parentRoot.getPath().append("archive.jar");
        IPackageFragmentRoot root= JavaProjectHelper.addLibrary(fProject, libraryPath);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        // after creation, the jar file is on the buildpath --> remove it first
        IFile jarFile= (IFile)executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, root, null, null, null, null);
        jarFile.create(null, false, null); // underlying resource must exist --> create
        assertTrue(jarFile.getFileExtension().equals("jar"));
        assertTrue(ClasspathModifier.isArchive(jarFile, fProject));
        assertTrue(ClasspathModifier.getClasspathEntryFor(jarFile.getFullPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        // now it can be added and tested
        root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_LIB_TO_BP, jarFile, null, null, null, null);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testAddJarFileToCPWithProjAsRootAndParent() throws InvocationTargetException, InterruptedException, CoreException {
        // create root parent for jar file
        IPackageFragmentRoot parentRoot= ClasspathModifier.getFragmentRoot(fProject.getUnderlyingResource(), fProject, null);
        IPath libraryPath= parentRoot.getPath().append("archive.jar");
        IPackageFragmentRoot root= JavaProjectHelper.addLibrary(fProject, libraryPath);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        assertTrue(root.getParent().equals(fProject));

        // after creation, the jar file is on the buildpath --> remove it first
        IFile jarFile= (IFile)executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, root, null, null, null, null);
        jarFile.create(null, false, null); // underlying resource must exist --> create
        assertTrue(jarFile.getFileExtension().equals("jar"));
        assertTrue(ClasspathModifier.isArchive(jarFile, fProject));
        assertTrue(ClasspathModifier.getClasspathEntryFor(jarFile.getFullPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        // now it can be added and tested
        root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_LIB_TO_BP, jarFile, null, null, null, null);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        //testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testAddJarFileToCPWithProjWithProjAsParentButRemovedAsRoot() throws InvocationTargetException, InterruptedException, CoreException {
        // create root parent for jar file
        IPackageFragmentRoot parentRoot= ClasspathModifier.getFragmentRoot(fProject.getUnderlyingResource(), fProject, null);
        IPath libraryPath= parentRoot.getPath().append("archive.jar");
        IPackageFragmentRoot root= JavaProjectHelper.addLibrary(fProject, libraryPath);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        assertTrue(root.getParent().equals(fProject));

        // after creation, the jar file is on the buildpath --> remove it first
        IFile jarFile= (IFile)executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, root, null, null, null, null);
        jarFile.create(null, false, null); // underlying resource must exist --> create
        assertTrue(jarFile.getFileExtension().equals("jar"));
        assertTrue(ClasspathModifier.isArchive(jarFile, fProject));
        assertTrue(ClasspathModifier.getClasspathEntryFor(jarFile.getFullPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        // now it can be added and tested
        root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_LIB_TO_BP, jarFile, null, null, null, null);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        //testProjectIsOnClasspath(false);

        validateClasspath();
    }

    public void testAddZipFileToCP() throws InvocationTargetException, InterruptedException, CoreException {
        super.testAddZipFileToCP();
        testProjectIsOnClasspath(false);
    }

    public void testAddZipFileToCPWithProjAsRoot() throws InvocationTargetException, InterruptedException, CoreException {
        // create root parent for jar file
        IPackageFragmentRoot parentRoot= createFragmentRootAndKeepProjAsRoot();
        IPath libraryPath= parentRoot.getPath().append("archive.zip");
        IPackageFragmentRoot root= JavaProjectHelper.addLibrary(fProject, libraryPath);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        // after creation, the jar file is on the buildpath --> remove it first
        IFile jarFile= (IFile)executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, root, null, null, null, null);
        jarFile.create(null, false, null); // underlying resource must exist --> create
        assertTrue(jarFile.getFileExtension().equals("zip"));
        assertTrue(ClasspathModifier.isArchive(jarFile, fProject));
        assertTrue(ClasspathModifier.getClasspathEntryFor(jarFile.getFullPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        // now it can be added and tested
        root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_LIB_TO_BP, jarFile, null, null, null, null);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testAddZipFileToCPWithProjAsRootAndParent() throws InvocationTargetException, InterruptedException, CoreException {
        // create root parent for jar file
        IPackageFragmentRoot parentRoot= ClasspathModifier.getFragmentRoot(fProject.getUnderlyingResource(), fProject, null);
        IPath libraryPath= parentRoot.getPath().append("archive.zip");
        IPackageFragmentRoot root= JavaProjectHelper.addLibrary(fProject, libraryPath);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        assertTrue(root.getParent().equals(fProject));

        // after creation, the jar file is on the buildpath --> remove it first
        IFile jarFile= (IFile)executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, root, null, null, null, null);
        jarFile.create(null, false, null); // underlying resource must exist --> create
        assertTrue(jarFile.getFileExtension().equals("zip"));
        assertTrue(ClasspathModifier.isArchive(jarFile, fProject));
        assertTrue(ClasspathModifier.getClasspathEntryFor(jarFile.getFullPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        // now it can be added and tested
        root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_LIB_TO_BP, jarFile, null, null, null, null);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testAddZipFileToCPWithProjWithProjAsParentButRemovedAsRoot() throws InvocationTargetException, InterruptedException, CoreException {
        // create root parent for jar file
        IPackageFragmentRoot parentRoot= ClasspathModifier.getFragmentRoot(fProject.getUnderlyingResource(), fProject, null);
        IPath libraryPath= parentRoot.getPath().append("archive.zip");
        IPackageFragmentRoot root= JavaProjectHelper.addLibrary(fProject, libraryPath);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        assertTrue(root.getParent().equals(fProject));

        // after creation, the jar file is on the buildpath --> remove it first
        IFile jarFile= (IFile)executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, root, null, null, null, null);
        jarFile.create(null, false, null); // underlying resource must exist --> create
        assertTrue(jarFile.getFileExtension().equals("zip"));
        assertTrue(ClasspathModifier.isArchive(jarFile, fProject));
        assertTrue(ClasspathModifier.getClasspathEntryFor(jarFile.getFullPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        // now it can be added and tested
        root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_LIB_TO_BP, jarFile, null, null, null, null);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        //testProjectIsOnClasspath(false);

        validateClasspath();
    }

    public void testAddJREToCP() throws InvocationTargetException, InterruptedException, CoreException {
        super.testAddJREToCP();
        testProjectIsOnClasspath(true);
    }

    public void testAddIncludedPackageToCP() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        // ... and remove project as root
        IPackageFragmentRoot parentRoot= includePackageAndKeepProjAsRoot();
        IPackageFragment fragment= parentRoot.getPackageFragment(fSubFolder);

        IClasspathEntry entry= parentRoot.getRawClasspathEntry();

        int nrInclusions= entry.getInclusionPatterns().length;
        int nrExclusions= entry.getExclusionPatterns().length;
        assertTrue(contains(new Path(fragment.getElementName()), entry.getInclusionPatterns(), null));
        assertFalse(contains(new Path(fragment.getElementName()), entry.getExclusionPatterns(), null));

        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, fragment, getOutputFolderQueryInternal(fProject.getPath()), null, null, null);

        entry= parentRoot.getRawClasspathEntry();
        assertFalse(contains(new Path(root.getElementName()), entry.getInclusionPatterns(), null));
        assertTrue(contains(new Path(root.getElementName()), entry.getExclusionPatterns(), null));
        assertTrue(entry.getInclusionPatterns().length + 1 == nrInclusions);
        assertTrue(entry.getExclusionPatterns().length - 1 == nrExclusions);
        assertTrue(root.getParent().equals(fProject));
        testProjectIsOnClasspath(false);

        validateClasspath();
    }

    public void testAddIncludedPackageToCPWithProjAsRoot() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot parentRoot= includePackageAndKeepProjAsRoot();
        IPackageFragment fragment= parentRoot.getPackageFragment(fSubFolder);

        IClasspathEntry entry= parentRoot.getRawClasspathEntry();

        int nrInclusions= entry.getInclusionPatterns().length;
        int nrExclusions= entry.getExclusionPatterns().length;
        assertTrue(contains(new Path(fragment.getElementName()), entry.getInclusionPatterns(), null));
        assertFalse(contains(new Path(fragment.getElementName()), entry.getExclusionPatterns(), null));

        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, fragment, getOutputFolderQueryToKeepProjAsRoot(), null, null, null);

        entry= parentRoot.getRawClasspathEntry();
        assertFalse(contains(new Path(root.getElementName()), entry.getInclusionPatterns(), null));
        assertTrue(contains(new Path(root.getElementName()), entry.getExclusionPatterns(), null));
        assertTrue(entry.getInclusionPatterns().length + 1 == nrInclusions);
        assertTrue(entry.getExclusionPatterns().length - 1 == nrExclusions);
        assertTrue(root.getParent().equals(fProject));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testAddExcludedPackageToCP() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        // ... and remove project as root
        IPackageFragmentRoot parentRoot= excludePackageAndKeepProjAsRoot();
        IPackageFragment fragment= parentRoot.getPackageFragment(fSubFolder);

        IClasspathEntry entry= parentRoot.getRawClasspathEntry();
        assertTrue(contains(new Path(fragment.getElementName()), entry.getExclusionPatterns(), null));

        IPath[] paths= getPaths();
        assertFalse(contains(fragment.getPath(), paths, null));

        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, fragment, getOutputFolderQueryInternal(fProject.getPath()), null, null, null);

        paths= getPaths();
        assertTrue(contains(fragment.getPath(), paths, null));

        parentRoot= fProject.findPackageFragmentRoot(parentRoot.getPath());
        entry= parentRoot.getRawClasspathEntry();

        assertTrue(contains(new Path(root.getElementName()), entry.getExclusionPatterns(), null));
        testProjectIsOnClasspath(false);

        validateClasspath();
    }

    public void testAddExcludedPackageToCPWithProjAsRoot() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot parentRoot= excludePackageAndKeepProjAsRoot();
        IPackageFragment fragment= parentRoot.getPackageFragment(fSubFolder);

        IClasspathEntry entry= parentRoot.getRawClasspathEntry();
        assertTrue(contains(new Path(fragment.getElementName()), entry.getExclusionPatterns(), null));

        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, fragment, getOutputFolderQueryToKeepProjAsRoot(), null, null, null);

        parentRoot= fProject.findPackageFragmentRoot(parentRoot.getPath());
        entry= parentRoot.getRawClasspathEntry();

        assertTrue(contains(new Path(root.getElementName()), entry.getExclusionPatterns(), null));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testRemoveProjectToCPAndKeepDefaultOutputLocation() throws CoreException, InvocationTargetException, InterruptedException {
        executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, fProject, null, null, null, null);
        testProjectIsOnClasspath(false);

        validateClasspath();
    }

    public void testRemoveFromCP() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        // project is not root
        // add folder
        int before= fProject.getRawClasspath().length;
        IFolder folder= getFolderHandle(new Path(fNormalFolder));
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));

        // and remove it

        executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, root, null, null, null, null);
        assertFalse(contains(folder.getFullPath(), getPaths(), null));
        int after= fProject.getRawClasspath().length;
        assertTrue(before - 1 == after);
        // the minus one is correct because:
        // first the project was the root and had an cp entry
        // then a src folder was added and the cp entry from the
        // project was removed.
        // at last, the entry for the folder was removed.
        // It follows that the number of cp entries has decreased by one

        validateClasspath();
        testProjectIsOnClasspath(false);
    }

    public void testRemoveFromCPWithProjAsRoot() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        // add folder
        int before= fProject.getRawClasspath().length;
        IPackageFragmentRoot root= createFragmentRootAndKeepProjAsRoot();

        // and remove it
        IFolder folder= (IFolder)executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, root, null, null, null, null);
        assertFalse(contains(folder.getFullPath(), getPaths(), null));
        int after= fProject.getRawClasspath().length;
        assertTrue(before == after);
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testRemoveZipFileFromCP() throws InvocationTargetException, InterruptedException, CoreException {
        super.testRemoveZipFileFromCP();
        testProjectIsOnClasspath(false);
    }

	public void testRemoveJarFileFromCP() throws InvocationTargetException, InterruptedException, CoreException {
        super.testRemoveJarFileFromCP();
        testProjectIsOnClasspath(false);
    }

    public void testRemoveJREFromCP() throws InvocationTargetException, InterruptedException, CoreException {
        super.testRemoveJREFromCP();
        testProjectIsOnClasspath(true);
    }

    // Test include, exclude, uninclude, unexclude, ...

    // Note that include and exclude does not have any impact whether
    // the project is on the classpath or not as long as the included/excluded
    // element was not a direct child of the project!
    // So the default testing is done by the super class while we have to
    // test only these special cases.
    public void testIncludePackageOnProject() throws JavaModelException, InvocationTargetException, InterruptedException {
        IPackageFragment fragment= createFragmentOnProject();
        IPackageFragmentRoot root= getProjectRoot(fragment.getUnderlyingResource());

        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));
        fragment= (IPackageFragment)executeOperation(BuildpathModifierAction.INCLUDE, fragment, null, null, null, null);
        assertTrue(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testExcludePackageOnProject() throws JavaModelException, InvocationTargetException, InterruptedException {
        IPackageFragment fragment= createFragmentOnProject();
        IPackageFragmentRoot root= getProjectRoot(fragment.getUnderlyingResource());

        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getExclusionPatterns(), null));
        IFolder excludedFolder= (IFolder) executeOperation(BuildpathModifierAction.EXCLUDE, fragment, null, null, null, null);
        assertTrue(contains(excludedFolder.getProjectRelativePath(), root.getRawClasspathEntry().getExclusionPatterns(), null));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testExcludeIncludedPackageOnProject() throws JavaModelException, InvocationTargetException, InterruptedException {
        IPackageFragment fragment= createFragmentOnProject();
        IPackageFragmentRoot root= getProjectRoot(fragment.getUnderlyingResource());

        // include
        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));
        fragment= (IPackageFragment) executeOperation(BuildpathModifierAction.INCLUDE, fragment, null, null, null, null);
        assertTrue(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));

        IClasspathEntry entry= root.getRawClasspathEntry();
        int nrIncluded= entry.getInclusionPatterns().length;
        int nrExcluded= entry.getExclusionPatterns().length;

        // exclude
        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getExclusionPatterns(), null));
        IFolder excludedFolder= (IFolder) executeOperation(BuildpathModifierAction.EXCLUDE, fragment, null, null, null, null);
        assertTrue(contains(excludedFolder.getProjectRelativePath(), root.getRawClasspathEntry().getExclusionPatterns(), null));
        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));

        assertTrue(root.getRawClasspathEntry().getInclusionPatterns().length + 1 == nrIncluded);
        assertTrue(root.getRawClasspathEntry().getExclusionPatterns().length - 1 == nrExcluded);
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testIncludeExcludedFolderOnProject() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPackageFragment fragment= createFragmentOnProject();
        IPackageFragmentRoot root= getProjectRoot(fragment.getUnderlyingResource());

        // exclude
        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getExclusionPatterns(), null));
        IFolder excludedFolder= (IFolder) executeOperation(BuildpathModifierAction.EXCLUDE, fragment, null, null, null, null);
        assertTrue(contains(excludedFolder.getProjectRelativePath(), root.getRawClasspathEntry().getExclusionPatterns(), null));

        IClasspathEntry entry= root.getRawClasspathEntry();
        int nrIncluded= entry.getInclusionPatterns().length;
        int nrExcluded= entry.getExclusionPatterns().length;

        // include
        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));
        fragment= (IPackageFragment) executeOperation(BuildpathModifierAction.INCLUDE, excludedFolder, null, null, null, null);
        assertTrue(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));
        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getExclusionPatterns(), null));

        assertTrue(root.getRawClasspathEntry().getInclusionPatterns().length - 1 == nrIncluded);
        assertTrue(root.getRawClasspathEntry().getExclusionPatterns().length + 1 == nrExcluded);
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testRemoveInclusionOnProject() throws JavaModelException, InvocationTargetException, InterruptedException {
        IPackageFragment fragment= createFragmentOnProject();
        IPackageFragmentRoot root= getProjectRoot(fragment.getUnderlyingResource());

        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));
        fragment= (IPackageFragment)executeOperation(BuildpathModifierAction.INCLUDE, fragment, null, null, null, null);
        assertTrue(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));

        // remove inclusion
        fragment= (IPackageFragment)executeOperation(BuildpathModifierAction.UNINCLUDE, fragment, null, null, null, null);
        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testRemoveExclusionOnProject() throws JavaModelException, InvocationTargetException, InterruptedException {
        IPackageFragment fragment= createFragmentOnProject();
        IPackageFragmentRoot root= getProjectRoot(fragment.getUnderlyingResource());

        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getExclusionPatterns(), null));
        IFolder excludedFolder= (IFolder)executeOperation(BuildpathModifierAction.EXCLUDE, fragment, null, null, null, null);
        assertTrue(contains(excludedFolder.getProjectRelativePath(), root.getRawClasspathEntry().getExclusionPatterns(), null));

        // remove exclusion
        fragment= (IPackageFragment)executeOperation(BuildpathModifierAction.UNEXCLUDE, excludedFolder, null, null, null, null);
        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getExclusionPatterns(), null));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testEditFiltersOnProject() throws JavaModelException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= getProjectRoot(fProject.getUnderlyingResource());
        final IPackageFragment includedPackage= root.getPackageFragment(fSubFolder);
        final IPackageFragment excludedPackage= root.getPackageFragment(fSubFolder + "2");

        assertFalse(contains(new Path(includedPackage.getElementName()).addTrailingSeparator(), root.getRawClasspathEntry().getInclusionPatterns(), null));
        assertFalse(contains(new Path(excludedPackage.getElementName()).addTrailingSeparator(), root.getRawClasspathEntry().getExclusionPatterns(), null));

        ClasspathModifierQueries.IInclusionExclusionQuery query= new ClasspathModifierQueries.IInclusionExclusionQuery() {

            public boolean doQuery(CPListElement element, boolean focusOnExcluded) {
                return true;
            }

            public IPath[] getInclusionPattern() {
                return new IPath[] {new Path(includedPackage.getElementName()).addTrailingSeparator()};
            }

            public IPath[] getExclusionPattern() {
                return new IPath[] {new Path(excludedPackage.getElementName()).addTrailingSeparator()};
            }

        };
        IJavaProject jProject= (IJavaProject)executeOperation(BuildpathModifierAction.EDIT_FILTERS, fProject, null, null, null, query);
        assertTrue(jProject.equals(fProject));

        root= getProjectRoot(fProject.getUnderlyingResource());
        assertTrue(contains(new Path(includedPackage.getElementName()).addTrailingSeparator(), root.getRawClasspathEntry().getInclusionPatterns(), null));
        assertTrue(contains(new Path(excludedPackage.getElementName()).addTrailingSeparator(), root.getRawClasspathEntry().getExclusionPatterns(), null));

        validateClasspath();
    }

    public void testResetFiltersOnProject() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= createFragmentRootAndKeepProjAsRoot();

        IPackageFragment includedPackage= root.getPackageFragment(fSubFolder);
        IPackageFragment excludedPackage= root.getPackageFragment(fSubFolder + "2");
        IFolder subSrcFolder= getFolderHandle(root.getPath().removeFirstSegments(1).append(fSubFolder + "3"));

        executeOperation(BuildpathModifierAction.INCLUDE, includedPackage, null, null, null, null);
        executeOperation(BuildpathModifierAction.INCLUDE, excludedPackage, null, null, null, null);
        executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, subSrcFolder, getOutputFolderQueryToKeepProjAsRoot(), null, null, null);
        int numberOnCP= fProject.getRawClasspath().length;

        executeOperation(BuildpathModifierAction.RESET, root, null, null, null, null);

        IPackageFragmentRoot projectRoot= getProjectRoot(fProject.getUnderlyingResource());
        IClasspathEntry entry= projectRoot.getRawClasspathEntry();
        assertTrue(entry.getInclusionPatterns().length == 0);
        // one has to be left because it is a source folder
        assertTrue(entry.getExclusionPatterns().length == 1);
        assertTrue(contains(root.getPath(), getPaths(), null));
        assertTrue(contains(subSrcFolder.getFullPath(), getPaths(), null));
        assertTrue(fProject.getRawClasspath().length == numberOnCP);
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    // Test output folder manipulations (create, edit, reset)
    public void testCreateOutputFolder() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPath oldOutputPath= fProject.getPath();
        IPath newOutputPath= oldOutputPath.append(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME) + "2");
        createOutputFolder(newOutputPath);
        testProjectIsOnClasspath(false);

        validateClasspath();
    }

    public void testCreateOutputFolderWithProjAsRootCancel() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        // Creation of the output folder is cancelled
        IPath oldOutputPath= fProject.getPath();
        IPath newOutputPath= oldOutputPath.append(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME) + "2");
        IPackageFragmentRoot root= createFragmentRootAndKeepProjAsRoot();

        IFolder outputFolder= fProject.getProject().getFolder(newOutputPath);
        assertFalse(outputFolder.exists());

        ClasspathModifierQueries.IOutputLocationQuery query= new ClasspathModifierQueries.IOutputLocationQuery() {

            public boolean doQuery(CPListElement element) {
                return false; // cancel
            }

            public IPath getOutputLocation() {
                // not important here
                return null;
            }

            public ClasspathModifierQueries.OutputFolderQuery getOutputFolderQuery(IPath path) {
                return new ClasspathModifierQueries.OutputFolderQuery(null) {

                    public boolean doQuery(boolean b, OutputFolderValidator validator, IJavaProject project) {
                        return false;
                    }

                    public IPath getOutputLocation() {
                        return null;
                    }

                    public boolean removeProjectFromClasspath() {
                        return true;
                    }

                };
            }

        };
        CPListElementAttribute outputAttribute= (CPListElementAttribute)executeOperation(BuildpathModifierAction.CREATE_OUTPUT, root, null, query, null, null);
        assertTrue(outputAttribute == null);
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testCreateOutputFolderWithProjAsRoot() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        // Creation of the output folder is accepted and project is removed as root
        IPath oldOutputPath= fProject.getPath();
        IPath newOutputPath= oldOutputPath.append(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME) + "2");
        IPackageFragmentRoot root= createFragmentRootAndKeepProjAsRoot();

        IFolder outputFolder= fProject.getProject().getFolder(newOutputPath);
        assertFalse(outputFolder.exists());

        ClasspathModifierQueries.IOutputLocationQuery query= getOutputLocationQuery();
        CPListElementAttribute outputAttribute= (CPListElementAttribute)executeOperation(BuildpathModifierAction.CREATE_OUTPUT, root, null, query, null, null);
        root= fProject.findPackageFragmentRoot(root.getPath());
        CPListElement elem= CPListElement.createFromExisting(root.getRawClasspathEntry(), fProject);

        assertTrue(((IPath)outputAttribute.getValue()).equals(newOutputPath));
        assertTrue(((IPath)outputAttribute.getValue()).equals(elem.getAttribute(CPListElement.OUTPUT)));
        testProjectIsOnClasspath(true); // the project has still a classpath entry, but the output location has changed
        assertFalse(fProject.getOutputLocation().equals(fProject.getPath()));

        validateClasspath();
    }

    public void testEditOutputFolder() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        // ... and remove project as root
        IPackageFragmentRoot root= createFragmentRootAndKeepProjAsRoot();
        IPath oldOutputPath= fProject.getPath();
        CPListElement elem= CPListElement.createFromExisting(root.getRawClasspathEntry(), fProject);
        CPListElementAttribute outputFolder= new CPListElementAttribute(elem, CPListElement.OUTPUT,
                elem.getAttribute(CPListElement.OUTPUT), true);

        final IPath editedOutputPath= oldOutputPath.append(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME) + "3");
        ClasspathModifierQueries.IOutputLocationQuery query= new ClasspathModifierQueries.IOutputLocationQuery() {

            public boolean doQuery(CPListElement element) {
                return true;
            }

            public IPath getOutputLocation() {
                return editedOutputPath;
            }

            public ClasspathModifierQueries.OutputFolderQuery getOutputFolderQuery(IPath path) throws JavaModelException {
                return NewEmptyProjectWizardTest.this.getOutputFolderQueryInternal(defaultOutputFolder);
            }
        };
        outputFolder= (CPListElementAttribute)executeOperation(BuildpathModifierAction.EDIT_OUTPUT, outputFolder, null, query, null, null);
        root= fProject.findPackageFragmentRoot(root.getPath());
        elem= CPListElement.createFromExisting(root.getRawClasspathEntry(), fProject);

        assertTrue(((IPath)outputFolder.getValue()).equals(editedOutputPath));
        assertTrue(((IPath)outputFolder.getValue()).equals(elem.getAttribute(CPListElement.OUTPUT)));
        testProjectIsOnClasspath(true);
        assertFalse(fProject.getOutputLocation().equals(fProject.getPath()));

        validateClasspath();
    }

    public void testEditOutputFolderWithProjAsRoot() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        // Editing of the output folder is cancelled
        IPackageFragmentRoot root= createFragmentRootAndKeepProjAsRoot();
        IPath oldOutputPath= fProject.getPath();
        CPListElement elem= CPListElement.createFromExisting(root.getRawClasspathEntry(), fProject);
        CPListElementAttribute outputFolder= new CPListElementAttribute(elem, CPListElement.OUTPUT,
                elem.getAttribute(CPListElement.OUTPUT), true);

        final IPath editedOutputPath= oldOutputPath.append(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME) + "3");
        ClasspathModifierQueries.IOutputLocationQuery query= new ClasspathModifierQueries.IOutputLocationQuery() {

            public boolean doQuery(CPListElement element) {
                return false; // cancel the operation
            }

            public IPath getOutputLocation() {
                return editedOutputPath;
            }

            public ClasspathModifierQueries.OutputFolderQuery getOutputFolderQuery(IPath path) {
                return new ClasspathModifierQueries.OutputFolderQuery(null) {

                    public boolean doQuery(boolean b, OutputFolderValidator validator, IJavaProject project) {
                        // cancel the operation
                        return false;
                    }

                    public IPath getOutputLocation() {
                        return null;
                    }

                    public boolean removeProjectFromClasspath() {
                        return true;
                    }

                };
            }
        };
        outputFolder= (CPListElementAttribute)executeOperation(BuildpathModifierAction.EDIT_OUTPUT, outputFolder, null, query, null, null);
        assertTrue(outputFolder == null);
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testEditOutputFolderWithNullReturn() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
      // here we have to set the future output folder to /WorkspaceProject/bin2 because
      // createOutputFolder() adds a new source folder to the project and removes the project as root
      // therefore the default output for the project changes to .../bin and editing changes it again to
      // .../bin2.
      final CPListElementAttribute attribute= createOutputFolder(fProject.getOutputLocation().append("bin2"));
      IPackageFragmentRoot root= fProject.findPackageFragmentRoot(fProject.getPath().append(fNormalFolder));

      assertTrue(root.getRawClasspathEntry().getOutputLocation() != null);

      ClasspathModifierQueries.IOutputLocationQuery query= new ClasspathModifierQueries.IOutputLocationQuery() {

          public boolean doQuery(CPListElement element) {
              return true;
          }

          public IPath getOutputLocation() {
              return null;
          }

          public ClasspathModifierQueries.OutputFolderQuery getOutputFolderQuery(IPath path) throws JavaModelException {
              return getOutputFolderQueryInternal(defaultOutputFolder);
          }
      };
      CPListElementAttribute newAttribute= (CPListElementAttribute)executeOperation(BuildpathModifierAction.EDIT_OUTPUT, attribute, null, query, null, null);

      assertTrue(root.getRawClasspathEntry().getOutputLocation() == null);
      assertTrue(newAttribute.getValue() == null);

      validateClasspath();
    }

    public void testEditOutputFolderWithNullReturnAndProjAsRoot() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= createFragmentRootAndKeepProjAsRoot();
        final IPath path= fProject.getPath().append("bin2");
        ClasspathModifierQueries.IOutputLocationQuery query= new ClasspathModifierQueries.IOutputLocationQuery() {
            public boolean doQuery(CPListElement element) {
                return true;
            }

            public IPath getOutputLocation() {
                return path;
            }

            public ClasspathModifierQueries.OutputFolderQuery getOutputFolderQuery(IPath path2) throws JavaModelException {
                return getOutputFolderQueryInternal(defaultOutputFolder);
            }
        };
        CPListElementAttribute attribute= (CPListElementAttribute)executeOperation(BuildpathModifierAction.CREATE_OUTPUT, root, null, query, null, null);

        root= fProject.findPackageFragmentRoot(root.getPath());
        assertTrue(root.getRawClasspathEntry().getOutputLocation().equals(path));
        assertTrue(fProject.getOutputLocation().segmentCount() > 1);

        query= new ClasspathModifierQueries.IOutputLocationQuery() {

            public boolean doQuery(CPListElement element) {
                return true;
            }

            public IPath getOutputLocation() {
                return null;
            }

            public ClasspathModifierQueries.OutputFolderQuery getOutputFolderQuery(IPath path2) throws JavaModelException {
                return getOutputFolderQueryToKeepProjAsRoot();
            }
        };
        CPListElementAttribute newAttribute= (CPListElementAttribute)executeOperation(BuildpathModifierAction.EDIT_OUTPUT, attribute, null, query, null, null);

        assertTrue(root.getRawClasspathEntry().getOutputLocation() == null);
        assertTrue(newAttribute.getValue() == null);
        testProjectIsOnClasspath(true);

        validateClasspath();
      }

    public void testResetOutputFolder() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPath oldOutputPath= fProject.getPath();
        IPath newOutputPath= oldOutputPath.append(PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME) + "2");
        CPListElementAttribute outputFolder= createOutputFolder(newOutputPath);
        outputFolder= (CPListElementAttribute)executeOperation(BuildpathModifierAction.RESET, outputFolder, null, null, null, null);
        assertTrue(outputFolder.getValue() == null);
        testProjectIsOnClasspath(false);

        validateClasspath();
    }

    // Test file manipulations (include, exclude, ...)
    // Note that include and exclude does not have any impact whether
    // the project is on the classpath or not as long as the included/excluded
    // element was not a direct child of the project!
    // So the default testing is done by the super class while we have to
    // test only these special cases for files.
    public void testIncludeFileOnProject() throws JavaModelException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot projectRoot= getProjectRoot(fProject.getCorrespondingResource());
        IPackageFragment fragment= projectRoot.createPackageFragment("", false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 0);

        cu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu, null, null, null, null);

        assertTrue(contains(cu.getPath().removeFirstSegments(1), projectRoot.getRawClasspathEntry().getInclusionPatterns(), null));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testExcludeFileOnProject() throws JavaModelException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot projectRoot= getProjectRoot(fProject.getCorrespondingResource());
        IPackageFragment fragment= projectRoot.createPackageFragment("", false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        assertTrue(projectRoot.getRawClasspathEntry().getExclusionPatterns().length == 0);

        IFile excludedFile= (IFile)executeOperation(BuildpathModifierAction.EXCLUDE, cu, null, null, null, null);

        assertTrue(contains(excludedFile.getFullPath().removeFirstSegments(1), projectRoot.getRawClasspathEntry().getExclusionPatterns(), null));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testIncludeExcludedFileOnProject() throws JavaModelException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot projectRoot= getProjectRoot(fProject.getCorrespondingResource());
        IPackageFragment fragment= projectRoot.createPackageFragment("", false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 0);
        assertTrue(projectRoot.getRawClasspathEntry().getExclusionPatterns().length == 0);

        IFile excludedFile= (IFile)executeOperation(BuildpathModifierAction.EXCLUDE, cu, null, null, null, null);

        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 0);
        assertTrue(contains(excludedFile.getFullPath().removeFirstSegments(1), projectRoot.getRawClasspathEntry().getExclusionPatterns(), null));

        cu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, excludedFile, null, null, null, null);

        assertTrue(projectRoot.getRawClasspathEntry().getExclusionPatterns().length == 0);
        assertTrue(contains(cu.getPath().removeFirstSegments(1), projectRoot.getRawClasspathEntry().getInclusionPatterns(), null));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testExcludeIncludedFileOnProject() throws JavaModelException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot projectRoot= getProjectRoot(fProject.getCorrespondingResource());
        IPackageFragment fragment= projectRoot.createPackageFragment("", false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 0);
        assertTrue(projectRoot.getRawClasspathEntry().getExclusionPatterns().length == 0);

        cu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu, null, null, null, null);

        assertTrue(projectRoot.getRawClasspathEntry().getExclusionPatterns().length == 0);
        assertTrue(contains(cu.getPath().removeFirstSegments(1), projectRoot.getRawClasspathEntry().getInclusionPatterns(), null));

        IFile excludedFile= (IFile)executeOperation(BuildpathModifierAction.EXCLUDE, cu, null, null, null, null);

        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 0);
        assertTrue(contains(excludedFile.getFullPath().removeFirstSegments(1), projectRoot.getRawClasspathEntry().getExclusionPatterns(), null));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testUnincludeFileOnProject() throws JavaModelException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot projectRoot= getProjectRoot(fProject.getCorrespondingResource());
        IPackageFragment fragment= projectRoot.createPackageFragment("", false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 0);
        cu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu, null, null, null, null);
        assertTrue(contains(cu.getPath().removeFirstSegments(1), projectRoot.getRawClasspathEntry().getInclusionPatterns(), null));

        cu= (ICompilationUnit)executeOperation(BuildpathModifierAction.UNINCLUDE, cu, null, null, null, null);

        assertFalse(contains(cu.getPath().removeFirstSegments(1), projectRoot.getRawClasspathEntry().getInclusionPatterns(), null));
        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testUnexcludeFileOnProject() throws JavaModelException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot projectRoot= getProjectRoot(fProject.getCorrespondingResource());
        IPackageFragment fragment= projectRoot.createPackageFragment("", false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        assertTrue(projectRoot.getRawClasspathEntry().getExclusionPatterns().length == 0);
        IFile excludedFile= (IFile)executeOperation(BuildpathModifierAction.EXCLUDE, cu, null, null, null, null);
        assertTrue(contains(excludedFile.getFullPath().removeFirstSegments(1), projectRoot.getRawClasspathEntry().getExclusionPatterns(), null));

        cu= (ICompilationUnit)executeOperation(BuildpathModifierAction.UNEXCLUDE, excludedFile, null, null, null, null);
        assertFalse(contains(excludedFile.getFullPath().removeFirstSegments(1), projectRoot.getRawClasspathEntry().getExclusionPatterns(), null));

        validateClasspath();
    }

    public void testIncludeFileWithIncludedFragment() throws JavaModelException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot projectRoot= getProjectRoot(fProject.getCorrespondingResource());
        IPackageFragment fragment= createFragmentOnProject();
        IPackageFragmentRoot root= getProjectRoot(fragment.getUnderlyingResource());

        // first include the fragment
        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 0);
        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));
        fragment= (IPackageFragment)executeOperation(BuildpathModifierAction.INCLUDE, fragment, null, null, null, null);
        assertTrue(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));

        // then include the file
        IPackageFragment defaultFragment= projectRoot.createPackageFragment("", false, null);
        ICompilationUnit cu= createICompilationUnit("C", defaultFragment);

        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 1);
        cu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu, null, null, null, null);
        assertTrue(contains(cu.getPath().removeFirstSegments(1), projectRoot.getRawClasspathEntry().getInclusionPatterns(), null));
        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 2);

        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testExcludeIncludedFileWithIncludedFragment() throws JavaModelException, InvocationTargetException, InterruptedException {
        // Important here is that the return value must be of type IFile and not
        // ICompilation unit because the fragment is still included
        IPackageFragmentRoot projectRoot= getProjectRoot(fProject.getCorrespondingResource());
        IPackageFragment fragment= createFragmentOnProject();
        IPackageFragmentRoot root= getProjectRoot(fragment.getUnderlyingResource());

        // first include the fragment
        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 0);
        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));
        fragment= (IPackageFragment)executeOperation(BuildpathModifierAction.INCLUDE, fragment, null, null, null, null);
        assertTrue(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));

        // then include the file
        IPackageFragment defaultFragment= projectRoot.createPackageFragment("", false, null);
        ICompilationUnit cu= createICompilationUnit("C", defaultFragment);

        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 1);
        cu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu, null, null, null, null);
        assertTrue(contains(cu.getPath().removeFirstSegments(1), projectRoot.getRawClasspathEntry().getInclusionPatterns(), null));
        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 2);

        // exclude the file
        IFile excludedFile= (IFile)executeOperation(BuildpathModifierAction.EXCLUDE, cu, null, null, null, null);
        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 1);
        assertFalse(contains(excludedFile.getProjectRelativePath(), projectRoot.getRawClasspathEntry().getInclusionPatterns(), null));
        assertTrue(contains(excludedFile.getProjectRelativePath(), projectRoot.getRawClasspathEntry().getExclusionPatterns(), null));

        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    public void testUnincludeIncludedFileWithIncludedFragment() throws JavaModelException, InvocationTargetException, InterruptedException {
        // Important here is that the return value must be of type IFile and not
        // ICompilation unit because the fragment is still included
        IPackageFragmentRoot projectRoot= getProjectRoot(fProject.getCorrespondingResource());
        IPackageFragment fragment= createFragmentOnProject();
        IPackageFragmentRoot root= getProjectRoot(fragment.getUnderlyingResource());

        // first include the fragment
        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 0);
        assertFalse(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));
        fragment= (IPackageFragment)executeOperation(BuildpathModifierAction.INCLUDE, fragment, null, null, null, null);
        assertTrue(contains(fragment.getPath().removeFirstSegments(1), root.getRawClasspathEntry().getInclusionPatterns(), null));

        // then include the file
        IPackageFragment defaultFragment= projectRoot.createPackageFragment("", false, null);
        ICompilationUnit cu= createICompilationUnit("C", defaultFragment);

        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 1);
        cu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu, null, null, null, null);
        assertTrue(contains(cu.getPath().removeFirstSegments(1), projectRoot.getRawClasspathEntry().getInclusionPatterns(), null));
        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 2);

        // uninclude the file
        IFile file= (IFile)executeOperation(BuildpathModifierAction.UNINCLUDE, cu, null, null, null, null);
        assertTrue(projectRoot.getRawClasspathEntry().getInclusionPatterns().length == 1);
        assertFalse(contains(file.getProjectRelativePath(), projectRoot.getRawClasspathEntry().getInclusionPatterns(), null));
        assertTrue(projectRoot.getRawClasspathEntry().getExclusionPatterns().length == 0);

        testProjectIsOnClasspath(true);

        validateClasspath();
    }

    protected IPackageFragmentRoot createFragmentRootAndKeepProjAsRoot() throws InvocationTargetException, InterruptedException {
        ClasspathModifierQueries.ICreateFolderQuery folderQuery= new ClasspathModifierQueries.ICreateFolderQuery() {

            public boolean doQuery() {
                return true;
            }

            public boolean isSourceFolder() {
                return true;
            }

            public IFolder getCreatedFolder() {
                return getFolderHandle(new Path(fSubFolder));
            }

        };
        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.CREATE_FOLDER, null, getOutputFolderQueryToKeepProjAsRoot(), null, folderQuery, null);
        return root;
    }

    protected IPackageFragment createFragmentOnProject() throws JavaModelException {
        IFolder fragmentFolder= getFolderHandle(new Path(fNormalFolder));
        IPackageFragmentRoot root= getProjectRoot(fragmentFolder);
        IPackageFragment fragment= root.getPackageFragment(fragmentFolder.getName());
        assertTrue(fragment.exists());
        return fragment;
    }

    protected IPackageFragmentRoot includePackageAndKeepProjAsRoot() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= createFragmentRootAndKeepProjAsRoot();

        IFolder folder= getFolderHandle(root.getPath().removeFirstSegments(1).append(fSubFolder));

        IClasspathEntry entry= root.getRawClasspathEntry();
        int before= entry.getInclusionPatterns().length;

        // include
        executeOperation(BuildpathModifierAction.INCLUDE, folder, null, null, null, null);

        entry= root.getRawClasspathEntry();
        IPath[] inclusionPatterns= entry.getInclusionPatterns();
        int after= inclusionPatterns.length;
        assertTrue(contains(new Path(folder.getName()), inclusionPatterns, null));
        assertTrue(before + 1 == after);
        return root;
    }

    protected IPackageFragmentRoot excludePackageAndKeepProjAsRoot() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= createFragmentRootAndKeepProjAsRoot();

        IFolder folder= getFolderHandle(root.getPath().removeFirstSegments(1).append(fSubFolder));

        IClasspathEntry entry= root.getRawClasspathEntry();
        int before= entry.getExclusionPatterns().length;

        // include
        folder= (IFolder)executeOperation(BuildpathModifierAction.EXCLUDE, root.getPackageFragment(folder.getName()), null, null, null, null);

        entry= root.getRawClasspathEntry();
        IPath[] exclusionPatterns= entry.getExclusionPatterns();
        int after= exclusionPatterns.length;
        assertTrue(contains(new Path(folder.getName()), exclusionPatterns, null));
        assertTrue(before + 1 == after);
        return root;
    }

    protected ClasspathModifierQueries.OutputFolderQuery getOutputFolderQueryToKeepProjAsRoot() {
        return new ClasspathModifierQueries.OutputFolderQuery(defaultOutputFolder) {
            public boolean doQuery(boolean b, OutputFolderValidator validator, IJavaProject project) {
                return true;
            }

            public IPath getOutputLocation() {
                return defaultOutputFolder;
            }

            public boolean removeProjectFromClasspath() {
                return false;
            }
        };
    }

    protected void testProjectIsOnClasspath(boolean isOnClasspath) throws JavaModelException {
        assertTrue((ClasspathModifier.getClasspathEntryFor(fProject.getPath(), fProject, IClasspathEntry.CPE_SOURCE) != null) == isOnClasspath);
    }
}
