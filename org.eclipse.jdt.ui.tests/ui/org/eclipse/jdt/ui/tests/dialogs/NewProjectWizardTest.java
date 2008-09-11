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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElementAttribute;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.BuildpathModifierAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ClasspathModifierQueries.OutputFolderValidator;

public class NewProjectWizardTest extends TestCase {

    private static final Class THIS= NewProjectWizardTest.class;

    protected IJavaProject fProject;
    protected static NewProjectTestSetup fTestSetup;
    protected String fNormalFolder= "NormalFolder";
    protected String fSubFolder= "SubFolder";

    public NewProjectWizardTest(String name) {
        super(name);
    }

    public static Test allTests() {
        return setUpTest(new TestSuite(THIS));
    }

    public static Test setUpTest(Test test) {
    	fTestSetup= new NewProjectTestSetup(test);
        return fTestSetup;
    }

    public static Test suite() {
        TestSuite suite= new TestSuite(THIS);
        fTestSetup= new NewProjectTestSetup(suite);
        suite.addTestSuite(NewEmptyProjectWizardTest.THIS);
        return fTestSetup;
    }

    protected void setUp() throws Exception {
        fProject= fTestSetup.getWorkspaceProjectWithSrc();
        assertFalse(fProject.isOnClasspath(fProject.getUnderlyingResource()));
    }

    protected void tearDown() throws Exception {
        fProject.getProject().delete(true, true, null);
    }

    // Test folder creation (on project, on fragment root, on fragment; as source folder, as normal folder)
    public void testCreateNormalFolderOnProject() throws CoreException, InvocationTargetException, InterruptedException {
        IFolder folder= (IFolder)executeOperation(BuildpathModifierAction.CREATE_FOLDER, null, getOutputFolderQueryInternal(fProject.getOutputLocation()), null, getNormalFolderCreationQuery(), null);

        assertTrue(folder.exists());
        assertTrue(folder.getName().equals(fNormalFolder));
        assertTrue(ClasspathModifier.getClasspathEntryFor(folder.getFullPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);

        validateClasspath();
    }

    public void testCreateSourceFolderOnProject() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.CREATE_FOLDER, null, getOutputFolderQueryInternal(fProject.getOutputLocation()), null, getSourceFolderCreationQuery(), null);

        assertTrue(root.getUnderlyingResource().exists());
        assertTrue(root.getElementName().equals(fSubFolder));
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);

        validateClasspath();
    }

    public void testCreateNormalFolderOnFragRoot() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot parentRoot= addToClasspath(new Path(fSubFolder));
        ClasspathModifierQueries.ICreateFolderQuery folderQuery= new ClasspathModifierQueries.ICreateFolderQuery() {

            public boolean doQuery() {
                return true;
            }

            public boolean isSourceFolder() {
                return false;
            }

            public IFolder getCreatedFolder() {
                return getFolderHandle(new Path(fSubFolder).append(fNormalFolder));
            }

        };
        IFolder folder= (IFolder)executeOperation(BuildpathModifierAction.CREATE_FOLDER, parentRoot, getOutputFolderQueryInternal(fProject.getOutputLocation()), null, folderQuery, null);

        assertTrue(folder.exists());
        assertTrue(folder.getName().equals(fNormalFolder));
        assertTrue(ClasspathModifier.getClasspathEntryFor(new Path(fNormalFolder), fProject, IClasspathEntry.CPE_SOURCE) == null);
        assertTrue(contains(new Path(fNormalFolder), parentRoot.getRawClasspathEntry().getExclusionPatterns(), null));

        validateClasspath();
    }

    public void testCreateSourceFolderOnFragRoot() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot parentRoot= addToClasspath(new Path(fNormalFolder));
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
        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.CREATE_FOLDER, parentRoot, getOutputFolderQueryInternal(fProject.getOutputLocation()), null, folderQuery, null);

        assertTrue(root.getUnderlyingResource().exists());
        assertTrue(root.getElementName().equals(fSubFolder));
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        assertTrue(contains(new Path(fSubFolder), parentRoot.getRawClasspathEntry().getExclusionPatterns(), null));

        validateClasspath();
    }

    public void testCreateNormalFolderOnFrag() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        final IPath nfFolder= new Path(fNormalFolder).append(fSubFolder).append("nfFolder");
        IFolder fragment= getFolderHandle(new Path(fNormalFolder).append(fSubFolder));
        ClasspathModifierQueries.ICreateFolderQuery folderQuery= new ClasspathModifierQueries.ICreateFolderQuery() {

            public boolean doQuery() {
                return true;
            }

            public boolean isSourceFolder() {
                return false;
            }

            public IFolder getCreatedFolder() {
                return getFolderHandle(nfFolder);
            }

        };
        IFolder folder= (IFolder)executeOperation(BuildpathModifierAction.CREATE_FOLDER, fragment, getOutputFolderQueryInternal(fProject.getOutputLocation()), null, folderQuery, null);

        assertTrue(folder.exists());
        assertTrue(folder.getParent().equals(fragment));
        assertTrue(folder.getName().equals(nfFolder.lastSegment()));
        assertTrue(ClasspathModifier.getClasspathEntryFor(folder.getFullPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        assertTrue(contains(folder.getFullPath().removeFirstSegments(2), root.getRawClasspathEntry().getExclusionPatterns(), null));

        validateClasspath();
    }

    public void testCreateSourceFolderOnFrag() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot parentRoot= addToClasspath(new Path(fNormalFolder));
        IFolder fragment= getFolderHandle(new Path(fNormalFolder).append(fSubFolder));
        final IPath srcFolder= new Path(fNormalFolder).append(fSubFolder).append("srcFolder");

        ClasspathModifierQueries.ICreateFolderQuery folderQuery= new ClasspathModifierQueries.ICreateFolderQuery() {

            public boolean doQuery() {
                return true;
            }

            public boolean isSourceFolder() {
                return true;
            }

            public IFolder getCreatedFolder() {
                return getFolderHandle(srcFolder);
            }

        };
        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.CREATE_FOLDER, fragment, getOutputFolderQueryInternal(fProject.getOutputLocation()), null, folderQuery, null);

        assertTrue(root.getUnderlyingResource().exists());
        assertTrue(root.getUnderlyingResource().getParent().equals(fragment));
        assertTrue(root.getElementName().equals(srcFolder.lastSegment()));
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        assertTrue(contains(root.getPath().removeFirstSegments(2), parentRoot.getRawClasspathEntry().getExclusionPatterns(), null));

        validateClasspath();
    }

    public void testAddNormalFolderToCP() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        int numberOfEntries= fProject.getRawClasspath().length;

        addToClasspath(new Path(fNormalFolder));

        int newNumberOfEntries= fProject.getRawClasspath().length;
        assertTrue(numberOfEntries + 1 == newNumberOfEntries);

        validateClasspath();
    }

    public void testAddNestedNormalFolderToCP() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IFolder cpFolder= excludePackage();

        IPackageFragmentRoot root= fProject.findPackageFragmentRoot(cpFolder.getFullPath());
        IFolder folder= getFolderHandle(cpFolder.getProjectRelativePath().append(fSubFolder));

        IClasspathEntry entry= root.getRawClasspathEntry();
        assertTrue(contains(new Path(folder.getName()), entry.getExclusionPatterns(), null));

        addToClasspath(folder.getProjectRelativePath());

        entry= root.getRawClasspathEntry();
        assertTrue(contains(new Path(folder.getName()), entry.getExclusionPatterns(), null));

        validateClasspath();
    }

    public void testAddPackageToCP() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        getFolderHandle(new Path(fNormalFolder).append(fSubFolder));
        IPackageFragment fragment= root.getPackageFragment(fSubFolder);

        IClasspathEntry entry= root.getRawClasspathEntry();

        int nrExclusions= entry.getExclusionPatterns().length;
        assertFalse(contains(new Path(fragment.getElementName()), entry.getExclusionPatterns(), null));

        addToClasspath(fragment);

        entry= root.getRawClasspathEntry();
        assertTrue(contains(new Path(fragment.getElementName()), entry.getExclusionPatterns(), null));
        assertTrue(entry.getExclusionPatterns().length - 1 == nrExclusions);

        validateClasspath();
    }

    public void testAddIncludedPackageToCP() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IFolder cpFolder= includePackage();

        IPackageFragmentRoot root= fProject.findPackageFragmentRoot(cpFolder.getFullPath());
        IPackageFragment fragment= root.getPackageFragment(fSubFolder);

        IClasspathEntry entry= root.getRawClasspathEntry();

        int nrInclusions= entry.getInclusionPatterns().length;
        int nrExclusions= entry.getExclusionPatterns().length;
        assertTrue(contains(new Path(fragment.getElementName()), entry.getInclusionPatterns(), null));
        assertFalse(contains(new Path(fragment.getElementName()), entry.getExclusionPatterns(), null));

        addToClasspath(fragment);

        entry= root.getRawClasspathEntry();
        assertFalse(contains(new Path(fragment.getElementName()), entry.getInclusionPatterns(), null));
        assertTrue(contains(new Path(fragment.getElementName()), entry.getExclusionPatterns(), null));
        assertTrue(entry.getInclusionPatterns().length + 1 == nrInclusions);
        assertTrue(entry.getExclusionPatterns().length - 1 == nrExclusions);

        validateClasspath();
    }

    public void testAddExcludedPackageToCP() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IFolder cpFolder= excludePackage();
        IPackageFragmentRoot root= fProject.findPackageFragmentRoot(cpFolder.getFullPath());
        IPackageFragment element= root.getPackageFragment(fSubFolder);

        IClasspathEntry entry= root.getRawClasspathEntry();
        assertTrue(contains(new Path(element.getElementName()), entry.getExclusionPatterns(), null));

        addToClasspath(element);

        root= fProject.findPackageFragmentRoot(element.getParent().getPath());
        entry= root.getRawClasspathEntry();

        assertTrue(contains(new Path(element.getElementName()), entry.getExclusionPatterns(), null));

        validateClasspath();
    }

    public void testAddProjectToCP() throws CoreException, InvocationTargetException, InterruptedException {
        // Situation: Project wich one source folder and one normal folder as
        // direct childs --> adding the project to the CP should convert the folder into
        // a package and the .java file into a compilation unit
        IPath srcPath= new Path("src2");
        IPackageFragmentRoot root= addToClasspath(srcPath);
        IFolder normalFolder= getFolderHandle(new Path(fNormalFolder));
        IPackageFragmentRoot projectRoot= getProjectRoot(fProject.getCorrespondingResource());
        IPackageFragment fragment= projectRoot.createPackageFragment("", false, null);

        assertTrue(ClasspathModifier.getClasspathEntryFor(fProject.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        assertTrue(fProject.findPackageFragment(normalFolder.getFullPath()) == null);
        assertTrue(fProject.findPackageFragment(fragment.getPath()) == null);

        IJavaProject project= (IJavaProject)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, fProject, getOutputFolderQueryInternal(fProject.getOutputLocation()), null, null, null);
        assertTrue(project.equals(fProject));

        // project is on classpath
        assertFalse(ClasspathModifier.getClasspathEntryFor(fProject.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        // root is on classpath and excluded on the project
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        assertTrue(ClasspathModifier.isExcluded(root.getUnderlyingResource(), fProject));
        assertFalse(fProject.findPackageFragment(normalFolder.getFullPath()) == null);
        assertFalse(fProject.findPackageFragment(fragment.getPath()) == null);

        validateClasspath();
    }

    public void testAddJarFileToCP() throws InvocationTargetException, InterruptedException, CoreException {
        IPath libraryPath= fProject.getPath().append("src2").append("archive.jar");
        testRemoveJarFileFromCP();
        IFile jarFile= fProject.getProject().getFile(libraryPath.removeFirstSegments(1));
        assertTrue(jarFile.getFileExtension().equals("jar"));

        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_LIB_TO_BP, jarFile, getOutputFolderQueryInternal(fProject.getOutputLocation()), null, null, null);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        validateClasspath();
    }

    public void testAddZipFileToCP() throws InvocationTargetException, InterruptedException, CoreException {
        IPath libraryPath= fProject.getPath().append("src2").append("archive.zip");
        testRemoveZipFileFromCP();
        IFile zipFile= fProject.getProject().getFile(libraryPath.removeFirstSegments(1));
        assertTrue(zipFile.getFileExtension().equals("zip"));

        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_LIB_TO_BP, zipFile, getOutputFolderQueryInternal(fProject.getOutputLocation()), null, null, null);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        validateClasspath();
    }

    public void testAddJREToCP() throws InvocationTargetException, InterruptedException, CoreException {
        IClasspathEntry[] entries= fProject.getRawClasspath();
        IClasspathEntry entry= null;
        for(int i= 0; i < entries.length; i++) {
            if(entries[i].getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                entry= entries[i];
                break;
            }
        }
        assertTrue(entry != null);
        assertFalse(ClasspathModifier.getClasspathEntryFor(entry.getPath(), fProject, IClasspathEntry.CPE_CONTAINER) == null);
        testRemoveJREFromCP();
        ClassPathContainer container= (ClassPathContainer)executeOperation(BuildpathModifierAction.ADD_LIB_TO_BP, entry, null, null, null, null);
        assertTrue(container.getClasspathEntry().equals(entry));
        assertFalse(ClasspathModifier.getClasspathEntryFor(entry.getPath(), fProject, IClasspathEntry.CPE_CONTAINER) == null);

        validateClasspath();
    }

    public void testRemoveFromCP() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        // add folder
        int before= fProject.getRawClasspath().length;
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));

        // and remove it
        IFolder folder= (IFolder)executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, root, null, null, null, null);

        assertTrue(folder.getFullPath().equals(root.getPath()));
        assertFalse(contains(new Path(fNormalFolder), getPaths(), null));
        int after= fProject.getRawClasspath().length;
        assertTrue(before == after);

        validateClasspath();
    }

    public void removeProjectFromCP() throws CoreException, InvocationTargetException, InterruptedException {
        IPath srcPath= new Path("src2");
        IPackageFragmentRoot root= addToClasspath(srcPath);
        IFolder normalFolder= getFolderHandle(new Path(fNormalFolder));
        IPackageFragmentRoot projectRoot= getProjectRoot(fProject.getCorrespondingResource());
        IPackageFragment fragment= projectRoot.createPackageFragment("", false, null);

        // add project to class path
        projectRoot= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, fProject, getOutputFolderQueryInternal(fProject.getOutputLocation()), null, null, null);
        assertTrue(projectRoot.equals(getProjectRoot(fProject.getCorrespondingResource())));

        // project is on classpath
        assertFalse(ClasspathModifier.getClasspathEntryFor(fProject.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        // root is on classpath
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        assertFalse(fProject.findPackageFragment(normalFolder.getFullPath()) == null);
        assertFalse(fProject.findPackageFragment(fragment.getPath()) == null);

        IJavaProject jProject= (IJavaProject)executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, fProject, null, null, null, null);

        assertTrue(jProject.equals(fProject));
        assertTrue(ClasspathModifier.getClasspathEntryFor(fProject.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_SOURCE) == null);
        assertTrue(fProject.findPackageFragment(normalFolder.getFullPath()) == null);
        assertTrue(fProject.findPackageFragment(fragment.getPath()) == null);

        projectRoot= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, fProject, getOutputFolderQueryInternal(fProject.getOutputLocation()), null, null, null);
        assertTrue(projectRoot.equals(getProjectRoot(fProject.getCorrespondingResource())));

        validateClasspath();
    }

    // Test include, exclude, uninclude, unexclude, ...
    public void testIncludePackage() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        includePackage();

        validateClasspath();
    }

    public void testExcludePackage() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        excludePackage();

        validateClasspath();
    }

    public void testExcludeIncludedPackage() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IFolder cpFolder= includePackage();
        IPackageFragmentRoot root= fProject.findPackageFragmentRoot(cpFolder.getFullPath());
        IPackageFragment fragment= root.getPackageFragment(fSubFolder);

        IClasspathEntry entry= root.getRawClasspathEntry();
        int nrIncluded= entry.getInclusionPatterns().length;
        int nrExcluded= entry.getExclusionPatterns().length;

        IFolder folder= (IFolder)executeOperation(BuildpathModifierAction.EXCLUDE, fragment, null, null, null, null);

        entry= root.getRawClasspathEntry();
        IPath[] inclusionPatterns= entry.getInclusionPatterns();
        IPath[] exclusionPatterns= entry.getExclusionPatterns();
        assertTrue(inclusionPatterns.length + 1 == nrIncluded);
        assertTrue(exclusionPatterns.length - 1 == nrExcluded);
        assertFalse(contains(new Path(fragment.getElementName()), inclusionPatterns, null));
        assertTrue(contains(new Path(fragment.getElementName()), exclusionPatterns, null));
        assertTrue(folder.getFullPath().equals(fragment.getPath()));

        validateClasspath();
    }

    public void testIncludeExcludedFolder() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IFolder cpFolder= excludePackage();

        IPackageFragmentRoot root= fProject.findPackageFragmentRoot(cpFolder.getFullPath());
        IFolder folder= getFolderHandle(cpFolder.getProjectRelativePath().append(fSubFolder));

        IClasspathEntry entry= root.getRawClasspathEntry();
        int nrIncluded= entry.getInclusionPatterns().length;
        int nrExcluded= entry.getExclusionPatterns().length;

        IPackageFragment fragment= (IPackageFragment)executeOperation(BuildpathModifierAction.INCLUDE, folder, null, null, null, null);

        entry= root.getRawClasspathEntry();
        IPath[] inclusionPatterns= entry.getInclusionPatterns();
        IPath[] exclusionPatterns= entry.getExclusionPatterns();
        assertTrue(inclusionPatterns.length - 1 == nrIncluded);
        assertTrue(exclusionPatterns.length + 1 == nrExcluded);
        assertTrue(contains(new Path(folder.getName()), inclusionPatterns, null));
        assertFalse(contains(new Path(folder.getName()), exclusionPatterns, null));
        assertTrue(fragment.getPath().equals(folder.getFullPath()));

        validateClasspath();
    }

    public void testRemoveInclusion() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IFolder cpFolder= includePackage();

        IPackageFragmentRoot root= fProject.findPackageFragmentRoot(cpFolder.getFullPath());
        IPackageFragment fragment= root.getPackageFragment(fSubFolder);

        IClasspathEntry entry= root.getRawClasspathEntry();
        int nrIncluded= entry.getInclusionPatterns().length;
        int nrExcluded= entry.getExclusionPatterns().length;

        IPackageFragment frag= (IPackageFragment)executeOperation(BuildpathModifierAction.UNINCLUDE, fragment, null, null, null, null);

        entry= root.getRawClasspathEntry();
        IPath[] inclusionPatterns= entry.getInclusionPatterns();
        IPath[] exclusionPatterns= entry.getExclusionPatterns();
        assertTrue(inclusionPatterns.length + 1 == nrIncluded);
        assertTrue(exclusionPatterns.length == nrExcluded);
        assertFalse(contains(new Path(fragment.getElementName()), inclusionPatterns, null));
        assertFalse(contains(new Path(fragment.getElementName()), exclusionPatterns, null));
        assertTrue(frag.getPath().equals(fragment.getPath()));

        validateClasspath();
    }

    public void testRemoveExclusion() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IFolder cpFolder= excludePackage();

        IPackageFragmentRoot root= fProject.findPackageFragmentRoot(cpFolder.getFullPath());
        IFolder folder= getFolderHandle(cpFolder.getProjectRelativePath().append(fSubFolder));

        IClasspathEntry entry= root.getRawClasspathEntry();
        int nrIncluded= entry.getInclusionPatterns().length;
        int nrExcluded= entry.getExclusionPatterns().length;

        IPackageFragment fragment= (IPackageFragment)executeOperation(BuildpathModifierAction.UNEXCLUDE, folder, null, null, null, null);

        entry= root.getRawClasspathEntry();
        IPath[] inclusionPatterns= entry.getInclusionPatterns();
        IPath[] exclusionPatterns= entry.getExclusionPatterns();
        assertTrue(inclusionPatterns.length == nrIncluded);
        assertTrue(exclusionPatterns.length + 1 == nrExcluded);
        assertFalse(contains(new Path(folder.getName()), inclusionPatterns, null));
        assertFalse(contains(new Path(folder.getName()), exclusionPatterns, null));
        assertTrue(fragment.getPath().equals(folder.getFullPath()));

        validateClasspath();
    }

    public void testRemoveJarFileFromCP() throws InvocationTargetException, InterruptedException, CoreException {
        IPath srcPath= new Path("src2");
        IPackageFragmentRoot parentRoot= addToClasspath(srcPath);
        IPath libraryPath= parentRoot.getPath().append("archive.jar");
        IPackageFragmentRoot root= JavaProjectHelper.addLibrary(fProject, libraryPath, null, null);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, root, null, null, null, null);
        //assertTrue(jarFile.getFileExtension().equals("jar"));
        //assertTrue(ClasspathModifier.isArchive(jarFile, fProject));
        //assertTrue(ClasspathModifier.getClasspathEntryFor(jarFile.getFullPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        validateClasspath();
    }

    public void testRemoveZipFileFromCP() throws InvocationTargetException, InterruptedException, CoreException {
        IPath srcPath= new Path("src2");
        IPackageFragmentRoot parentRoot= addToClasspath(srcPath);
        IPath libraryPath= parentRoot.getPath().append("archive.zip");
        IPackageFragmentRoot root= JavaProjectHelper.addLibrary(fProject, libraryPath);
        assertFalse(ClasspathModifier.getClasspathEntryFor(root.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);
        executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, root, null, null, null, null);
        //zipFile.create(null, false, null);
       // assertTrue(zipFile.getFileExtension().equals("zip"));
        //assertTrue(ClasspathModifier.isArchive(zipFile, fProject));
        //assertTrue(ClasspathModifier.getClasspathEntryFor(zipFile.getFullPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        validateClasspath();
    }

    public void testRemoveJREFromCP() throws InvocationTargetException, InterruptedException, CoreException {
        IClasspathEntry[] entries= fProject.getRawClasspath();
        IClasspathEntry entry= null;
        for(int i= 0; i < entries.length; i++) {
            if(entries[i].getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                entry= entries[i];
                break;
            }
        }
        assertTrue(entry != null);
        assertFalse(ClasspathModifier.getClasspathEntryFor(entry.getPath(), fProject, IClasspathEntry.CPE_CONTAINER) == null);

        ClassPathContainer container= new ClassPathContainer(fProject, entry);
        IJavaProject project= (IJavaProject) executeOperation(BuildpathModifierAction.REMOVE_FROM_BP, container, null, null, null, null);
        assertTrue(project.equals(fProject));
        assertTrue(ClasspathModifier.getClasspathEntryFor(entry.getPath(), fProject, IClasspathEntry.CPE_CONTAINER) == null);

        validateClasspath();
    }

    public void testEditFilters() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
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
        root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.EDIT_FILTERS, root, null, null, null, query);

        assertTrue(contains(new Path(includedPackage.getElementName()).addTrailingSeparator(), root.getRawClasspathEntry().getInclusionPatterns(), null));
        assertTrue(contains(new Path(excludedPackage.getElementName()).addTrailingSeparator(), root.getRawClasspathEntry().getExclusionPatterns(), null));

        validateClasspath();
    }

    public void testResetFilters() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IFolder folder= getFolderHandle(new Path(fNormalFolder));

        IPackageFragment includedPackage= root.getPackageFragment(fSubFolder);
        IPackageFragment excludedPackage= root.getPackageFragment(fSubFolder + "2");

        IClasspathEntry entry= root.getRawClasspathEntry();

        executeOperation(BuildpathModifierAction.INCLUDE, includedPackage, null, null, null, null);
        executeOperation(BuildpathModifierAction.EXCLUDE, excludedPackage, null, null, null, null);
        addToClasspath(folder.getProjectRelativePath().append(fSubFolder + "3"));
        IFolder subSrcFolder= getFolderHandle(folder.getProjectRelativePath().append(fSubFolder + "3"));
        int numberOnCP= fProject.getRawClasspath().length;

        IPackageFragmentRoot editedRoot= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.RESET, root, null, null, null, null);

        entry= editedRoot.getRawClasspathEntry();
        assertTrue(entry.getInclusionPatterns().length == 0);
        // one has to be left because it is a source folder
        assertTrue(entry.getExclusionPatterns().length == 1);
        assertTrue(contains(folder.getFullPath(), getPaths(), null));
        assertTrue(contains(subSrcFolder.getFullPath(), getPaths(), null));
        assertTrue(fProject.getRawClasspath().length == numberOnCP);
        assertTrue(editedRoot.getPath().equals(root.getPath()));

        validateClasspath();
    }

    // Test output folder manipulations (create, edit, reset)
    public void testCreateOutputFolder() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPath oldOutputPath= fProject.getOutputLocation();
        IPath newOutputPath= new Path(oldOutputPath.toString() + "2");
        createOutputFolder(newOutputPath);

        validateClasspath();
    }

    public CPListElementAttribute createOutputFolder(IPath newOutputPath) throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));

        IFolder outputFolder= fProject.getProject().getFolder(newOutputPath);
        assertFalse(outputFolder.exists());

        ClasspathModifierQueries.IOutputLocationQuery query= getOutputLocationQuery();

        CPListElementAttribute outputAttribute= (CPListElementAttribute)executeOperation(BuildpathModifierAction.CREATE_OUTPUT, root, null, query, null, null);

        root= fProject.findPackageFragmentRoot(root.getPath());
        CPListElement elem= CPListElement.createFromExisting(root.getRawClasspathEntry(), fProject);

        assertTrue(((IPath)outputAttribute.getValue()).equals(newOutputPath));
        assertTrue(((IPath)outputAttribute.getValue()).equals(elem.getAttribute(CPListElement.OUTPUT)));

        validateClasspath();
        return outputAttribute;
    }

    public void testEditOutputFolder() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPath oldOutputPath= fProject.getOutputLocation();
        IPath newOutputPath= new Path(oldOutputPath.toString() + "2");
        CPListElementAttribute attribute= createOutputFolder(newOutputPath);

        ClasspathModifierQueries.IOutputLocationQuery query= new ClasspathModifierQueries.IOutputLocationQuery() {

            public boolean doQuery(CPListElement element) {
                return true;
            }

            public IPath getOutputLocation() {
                IPath path= null;
                try {
                    path= fProject.getOutputLocation();
                } catch (JavaModelException e) {}
                return new Path(path.toString() + "3");
            }

            public ClasspathModifierQueries.OutputFolderQuery getOutputFolderQuery(IPath path) throws JavaModelException {
                return getOutputFolderQueryInternal(fProject.getOutputLocation());
            }
        };
        CPListElementAttribute newAttribute= (CPListElementAttribute)executeOperation(BuildpathModifierAction.EDIT_OUTPUT, attribute, null, query, null, null);

        IPath editedOutputPath= new Path(oldOutputPath.toString() + "3");

        IFolder folder= getFolderHandle(new Path (fNormalFolder));
        IPackageFragmentRoot root= fProject.findPackageFragmentRoot(folder.getFullPath());
        CPListElement elem= CPListElement.createFromExisting(root.getRawClasspathEntry(), fProject);

        assertTrue(((IPath)newAttribute.getValue()).equals(editedOutputPath));
        assertTrue(((IPath)newAttribute.getValue()).equals(elem.getAttribute(CPListElement.OUTPUT)));

        validateClasspath();
    }

    public void testEditOutputFolderWithNullReturn() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        CPListElementAttribute attribute= createOutputFolder(getOutputLocationQuery().getOutputLocation());
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
                return NewProjectWizardTest.this.getOutputFolderQueryInternal(fProject.getOutputLocation());
            }
        };
        CPListElementAttribute newAttribute= (CPListElementAttribute)executeOperation(BuildpathModifierAction.EDIT_OUTPUT, attribute, null, query, null, null);

        assertTrue(root.getRawClasspathEntry().getOutputLocation() == null);
        assertTrue(newAttribute.getValue() == null);

        validateClasspath();
    }

    public void testResetOutputFolder() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPath oldOutputPath= fProject.getOutputLocation();
        IPath newOutputPath= new Path(oldOutputPath.toString() + "2");
        CPListElementAttribute attribute= createOutputFolder(newOutputPath);

        CPListElementAttribute newAttribute= (CPListElementAttribute)executeOperation(BuildpathModifierAction.RESET, attribute, null, null, null, null);

        assertTrue(((IPath)newAttribute.getValue()) == null);

        validateClasspath();
    }

    // Test file manipulations (include, exclude, ...)
    public void testIncludeCompilationUnit() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IPackageFragment fragment= root.createPackageFragment(fSubFolder, false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        assertTrue(root.getRawClasspathEntry().getInclusionPatterns().length == 0);

        ICompilationUnit newCu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu, null, null, null, null);

        assertTrue(contains(newCu.getPath().removeFirstSegments(2), root.getRawClasspathEntry().getInclusionPatterns(), null));
        assertTrue(newCu.getPath().equals(cu.getPath()));

        validateClasspath();
    }

    public void testExcludeCompilationUnit() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IPackageFragment fragment= root.createPackageFragment(fSubFolder, false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        assertTrue(root.getRawClasspathEntry().getExclusionPatterns().length == 0);

        IFile excludedFile=(IFile)executeOperation(BuildpathModifierAction.EXCLUDE, cu, null, null, null, null);

        assertTrue(contains(excludedFile.getFullPath().removeFirstSegments(2), root.getRawClasspathEntry().getExclusionPatterns(), null));
        assertTrue(excludedFile.getFullPath().equals(cu.getPath()));

        validateClasspath();
    }

    public void testIncludeExcludedFile() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IPackageFragment fragment= root.createPackageFragment(fSubFolder, false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        assertTrue(root.getRawClasspathEntry().getInclusionPatterns().length == 0);
        assertTrue(root.getRawClasspathEntry().getExclusionPatterns().length == 0);

        IFile excludedFile= (IFile)executeOperation(BuildpathModifierAction.EXCLUDE, cu, null, null, null, null);

        assertTrue(root.getRawClasspathEntry().getInclusionPatterns().length == 0);
        assertTrue(contains(excludedFile.getFullPath().removeFirstSegments(2), root.getRawClasspathEntry().getExclusionPatterns(), null));

        ICompilationUnit newCu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu, null, null, null, null);

        assertTrue(root.getRawClasspathEntry().getExclusionPatterns().length == 0);
        assertTrue(contains(newCu.getPath().removeFirstSegments(2), root.getRawClasspathEntry().getInclusionPatterns(), null));
        assertTrue(excludedFile.getFullPath().equals(newCu.getPath()));

        validateClasspath();
    }

    public void testExcludeIncludedFile() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IPackageFragment fragment= root.createPackageFragment(fSubFolder, false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        assertTrue(root.getRawClasspathEntry().getInclusionPatterns().length == 0);
        assertTrue(root.getRawClasspathEntry().getExclusionPatterns().length == 0);

        ICompilationUnit newCu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu, null, null, null, null);

        assertTrue(root.getRawClasspathEntry().getExclusionPatterns().length == 0);
        assertTrue(contains(cu.getPath().removeFirstSegments(2), root.getRawClasspathEntry().getInclusionPatterns(), null));

        IFile excludedFile= (IFile)executeOperation(BuildpathModifierAction.EXCLUDE, newCu, null, null, null, null);

        assertTrue(root.getRawClasspathEntry().getInclusionPatterns().length == 0);
        assertTrue(contains(excludedFile.getFullPath().removeFirstSegments(2), root.getRawClasspathEntry().getExclusionPatterns(), null));
        assertTrue(excludedFile.getFullPath().equals(newCu.getPath()));

        validateClasspath();
    }

    public void testUnincludeFile() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IPackageFragment fragment= root.createPackageFragment(fSubFolder, false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        assertTrue(root.getRawClasspathEntry().getInclusionPatterns().length == 0);
        ICompilationUnit newCu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu, null, null, null, null);
        assertTrue(contains(newCu.getPath().removeFirstSegments(2), root.getRawClasspathEntry().getInclusionPatterns(), null));

        newCu= (ICompilationUnit)executeOperation(BuildpathModifierAction.UNINCLUDE, newCu, null, null, null, null);

        assertFalse(contains(newCu.getPath().removeFirstSegments(2), root.getRawClasspathEntry().getInclusionPatterns(), null));
        assertTrue(newCu.getPath().equals(cu.getPath()));

        validateClasspath();
    }

    public void testUnexcludeFile() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IPackageFragment fragment= root.createPackageFragment(fSubFolder, false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        assertTrue(root.getRawClasspathEntry().getExclusionPatterns().length == 0);
        IFile excludedFile= (IFile)executeOperation(BuildpathModifierAction.EXCLUDE, cu, null, null, null, null);

        assertTrue(contains(excludedFile.getFullPath().removeFirstSegments(2), root.getRawClasspathEntry().getExclusionPatterns(), null));
        assertTrue(excludedFile.getFullPath().equals(cu.getPath()));

        ICompilationUnit newCu= (ICompilationUnit)executeOperation(BuildpathModifierAction.UNEXCLUDE, excludedFile, null, null, null, null);

        assertFalse(contains(excludedFile.getFullPath().removeFirstSegments(2), root.getRawClasspathEntry().getExclusionPatterns(), null));
        assertTrue(newCu.getPath().equals(cu.getPath()));

        validateClasspath();
    }

    public void testIncludeFileOnFolder() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IPackageFragment fragment2= root.createPackageFragment(fSubFolder+"2", false, null);
        executeOperation(BuildpathModifierAction.INCLUDE, fragment2, null, null, null, null);

        IPackageFragment fragment= root.createPackageFragment(fSubFolder, false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        // Note: the parent of cu is a folder which is not excluded! This is important as
        // 'include' on a cu which parent folder is explicitly excluded is not possible!
        // Therefore fragment2 had to be included to test this case!
        assertTrue(fProject.findElement(cu.getPath().makeRelative()) == null);

        ICompilationUnit newCu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu.getUnderlyingResource(), null, null, null, null);

        assertTrue(contains(newCu.getPath().removeFirstSegments(2), root.getRawClasspathEntry().getInclusionPatterns(), null));
        assertTrue(newCu.getPath().equals(cu.getPath()));

        validateClasspath();
    }

    public void testExcludeFileOnFolder() throws CoreException, InvocationTargetException, InterruptedException {
        // Special case: there are 2 packages fSubFolder and fSubFolder2 where the fSubFolder2 is
        // included. Now we include the compilation unit from fSubFolder and then exlude it.
        // After inclusion, the returned object must be of type ICompilation unit,
        // after exclusion, the returned object must be of type IFile (because
        // only fSubFolder2 is included. We only test that the return type is correct because
        // the correctness of the filters has been tested before.
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IPackageFragment fragment2= root.createPackageFragment(fSubFolder+"2", false, null);
        IPackageFragment fragment= root.createPackageFragment(fSubFolder, false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        // include fragment2
        executeOperation(BuildpathModifierAction.INCLUDE, fragment2, null, null, null, null);

        // Check that the compilation unit cannot be found (because now its only
        // a normal file and not a CU).
        assertTrue(fProject.findElement(cu.getPath().makeRelative()) == null);
        // include the cu --> if cast fails, then include is broken
        ICompilationUnit newCu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu.getUnderlyingResource(), null, null, null, null);

        // exclude the file --> if cast fails, then exclude is broken
        IFile excludedFile= (IFile)executeOperation(BuildpathModifierAction.EXCLUDE, newCu, null, null, null, null);

        assertTrue(excludedFile.getFullPath().equals(newCu.getPath()));

        validateClasspath();
    }

    public void testUnincludeFileOnFolder() throws CoreException, InvocationTargetException, InterruptedException {
        // Same situation as in testExcludeFileOnFolder, but this time, we use
        // uninclude instead of exclude
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IPackageFragment fragment2= root.createPackageFragment(fSubFolder+"2", false, null);
        IPackageFragment fragment= root.createPackageFragment(fSubFolder, false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        // include fragment2
        executeOperation(BuildpathModifierAction.INCLUDE, fragment2, null, null, null, null);

        // Check that the compilation unit cannot be found (because now its only
        // a normal file and not a CU).
        assertTrue(fProject.findElement(cu.getPath().makeRelative()) == null);
        // include the cu --> if cast fails, then include is broken
        ICompilationUnit newCu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu.getUnderlyingResource(), null, null, null, null);

        // uninclude the file --> if cast fails, then uninclude is broken
        IFile file= (IFile)executeOperation(BuildpathModifierAction.UNINCLUDE, newCu, null, null, null, null);

        assertTrue(file.getFullPath().equals(cu.getPath()));
        validateClasspath();
    }

    public void testIncludeFileOnIncludedFragment() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IPackageFragment fragment= root.createPackageFragment(fSubFolder, false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        fragment= (IPackageFragment)executeOperation(BuildpathModifierAction.INCLUDE, fragment, null, null, null, null);
        assertTrue(root.getRawClasspathEntry().getInclusionPatterns().length == 1);

        cu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu, null, null, null, null);

        assertTrue(root.getRawClasspathEntry().getInclusionPatterns().length == 2);
        assertTrue(contains(cu.getPath().removeFirstSegments(2), root.getRawClasspathEntry().getInclusionPatterns(), null));

        validateClasspath();
    }

    public void testExcludeFileOnIncludedFragment() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IPackageFragment fragment= root.createPackageFragment(fSubFolder, false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        fragment= (IPackageFragment)executeOperation(BuildpathModifierAction.INCLUDE, fragment, null, null, null, null);
        assertTrue(root.getRawClasspathEntry().getInclusionPatterns().length == 1);

        IFile excludedFile= (IFile)executeOperation(BuildpathModifierAction.EXCLUDE, cu, null, null, null, null);

        assertTrue(root.getRawClasspathEntry().getInclusionPatterns().length == 1);
        assertFalse(contains(excludedFile.getFullPath().removeFirstSegments(2), root.getRawClasspathEntry().getInclusionPatterns(), null));
        assertTrue(contains(excludedFile.getFullPath().removeFirstSegments(2), root.getRawClasspathEntry().getExclusionPatterns(), null));

        validateClasspath();
    }

    public void testUnincludeOnIncludedFragment() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IPackageFragment fragment= root.createPackageFragment(fSubFolder, false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        fragment= (IPackageFragment)executeOperation(BuildpathModifierAction.INCLUDE, fragment, null, null, null, null);
        cu= (ICompilationUnit)executeOperation(BuildpathModifierAction.INCLUDE, cu, null, null, null, null);

        cu= (ICompilationUnit)executeOperation(BuildpathModifierAction.UNINCLUDE, cu, null, null, null, null);
        assertFalse(contains(cu.getPath().removeFirstSegments(2), root.getRawClasspathEntry().getInclusionPatterns(), null));

        validateClasspath();
    }

    public void testUnexcludeOnIncludedFragment() throws CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IPackageFragment fragment= root.createPackageFragment(fSubFolder, false, null);
        ICompilationUnit cu= createICompilationUnit("C", fragment);

        fragment= (IPackageFragment)executeOperation(BuildpathModifierAction.INCLUDE, fragment, null, null, null, null);
        IFile excludedFile= (IFile)executeOperation(BuildpathModifierAction.EXCLUDE, cu, null, null, null, null);

        cu= (ICompilationUnit)executeOperation(BuildpathModifierAction.UNEXCLUDE, excludedFile, null, null, null, null);
        assertFalse(contains(cu.getPath().removeFirstSegments(2), root.getRawClasspathEntry().getExclusionPatterns(), null));
        assertTrue(excludedFile.getFullPath().equals(cu.getPath()));

        validateClasspath();
    }

    // Helper methods
    protected Object executeOperation(int type, final Object selection, final ClasspathModifierQueries.OutputFolderQuery outputQuery, final ClasspathModifierQueries.IOutputLocationQuery locationQuery, final ClasspathModifierQueries.ICreateFolderQuery creationQuery, final ClasspathModifierQueries.IInclusionExclusionQuery inclQuery) throws InvocationTargetException, InterruptedException {
        return null;
    }

    protected IPackageFragmentRoot addToClasspath(IPath path) throws CoreException, InvocationTargetException, InterruptedException {
        IPath[] paths= getPaths();
        assertFalse(contains(path, paths, null));

        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, getFolderHandle(path), getOutputFolderQueryInternal(fProject.getOutputLocation()), null, null, null);

        paths= getPaths();
        assertTrue(contains(root.getPath(), getPaths(), null));
        return root;
    }

    protected IPackageFragmentRoot addToClasspath(IJavaElement element) throws CoreException, InvocationTargetException, InterruptedException {
        IPath[] paths= getPaths();
        assertFalse(contains(element.getPath(), paths, null));

        IPackageFragmentRoot root= (IPackageFragmentRoot)executeOperation(BuildpathModifierAction.ADD_SEL_SF_TO_BP, element, getOutputFolderQueryInternal(fProject.getOutputLocation()), null, null, null);

        paths= getPaths();
        assertTrue(contains(element.getPath(), paths, null));
        return root;
    }

    protected IFolder includePackage() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IFolder cpFolder= getFolderHandle(new Path(fNormalFolder));

        IFolder folder= getFolderHandle(cpFolder.getProjectRelativePath().append(fSubFolder));

        IClasspathEntry entry= root.getRawClasspathEntry();
        int before= entry.getInclusionPatterns().length;

        // include
        executeOperation(BuildpathModifierAction.INCLUDE, folder, null, null, null, null);

        entry= root.getRawClasspathEntry();
        IPath[] inclusionPatterns= entry.getInclusionPatterns();
        int after= inclusionPatterns.length;
        assertTrue(contains(new Path(folder.getName()), inclusionPatterns, null));
        assertTrue(before + 1 == after);
        return cpFolder;
    }

    protected IFolder excludePackage() throws JavaModelException, CoreException, InvocationTargetException, InterruptedException {
        IPackageFragmentRoot root= addToClasspath(new Path(fNormalFolder));
        IFolder cpFolder= getFolderHandle(new Path(fNormalFolder));

        IFolder folder= getFolderHandle(cpFolder.getProjectRelativePath().append(fSubFolder));
        IPackageFragment fragment= root.getPackageFragment(folder.getName());

        IClasspathEntry entry= root.getRawClasspathEntry();
        int nrExcluded= entry.getExclusionPatterns().length;
        executeOperation(BuildpathModifierAction.EXCLUDE, fragment, null, null, null, null);

        entry= root.getRawClasspathEntry();
        IPath[] exclusionPatterns= entry.getExclusionPatterns();
        assertTrue(nrExcluded + 1 == exclusionPatterns.length);
        assertTrue(contains(new Path(fragment.getElementName()), exclusionPatterns, null));
        return cpFolder;
    }

    protected IFolder getFolderHandle(IPath path) {
        IFolder folder= fProject.getProject().getFolder(path);
        try {
            if (!folder.exists())
                folder.create(true, false, null);
        } catch (CoreException e) {
        }
        return folder;
    }

    protected ICompilationUnit createICompilationUnit(String className, IPackageFragment fragment) throws JavaModelException {
        String packString= fragment.getElementName().equals("") ? fragment.getElementName() : "package " + fragment.getElementName() +";\n";
        StringBuffer content= getFileContent(className, packString);
        return fragment.createCompilationUnit(className+".java", content.toString(), false, null);
    }

    protected StringBuffer getFileContent(String className, String packageHeader) {
        StringBuffer buf= new StringBuffer();
        buf.append(packageHeader);
        buf.append("\n");
        buf.append("public class "+className+ " {\n");
        buf.append("    public void foo() {\n");
        buf.append("    }\n");
        buf.append("}\n");
        return buf;
    }

    public ClasspathModifierQueries.OutputFolderQuery getOutputFolderQueryInternal(IPath desiredOutputLocation){
        return new ClasspathModifierQueries.OutputFolderQuery(desiredOutputLocation) {
            public boolean doQuery(boolean b, OutputFolderValidator validator, IJavaProject project) {
                return true;
            }

            public IPath getOutputLocation() {
                IPath newOutputFolder= null;
                try {
                    if (fProject.isOnClasspath(fProject.getUnderlyingResource())) {
                        String outputFolderName= PreferenceConstants.getPreferenceStore().getString(PreferenceConstants.SRCBIN_BINNAME);
                        newOutputFolder= fProject.getPath().append(outputFolderName);
                        return newOutputFolder;
                    }
                } catch (JavaModelException e) {
                    fail();
                }
                return null;
            }

            public boolean removeProjectFromClasspath() {
                return true;
            }
        };
    }

    public ClasspathModifierQueries.IOutputLocationQuery getOutputLocationQuery() {
        return new ClasspathModifierQueries.IOutputLocationQuery() {

            public boolean doQuery(CPListElement element) {
                return true;
            }

            public IPath getOutputLocation() {
                IPath oldOutputPath= null;
                try {
                    oldOutputPath= fProject.getOutputLocation();
                } catch (JavaModelException e) {}
                return new Path(oldOutputPath.toString() + "2");
            }

            public ClasspathModifierQueries.OutputFolderQuery getOutputFolderQuery(IPath path) throws JavaModelException {
                return NewProjectWizardTest.this.getOutputFolderQueryInternal(fProject.getOutputLocation());
            }
        };
    }

    protected ClasspathModifierQueries.ICreateFolderQuery getSourceFolderCreationQuery() {
        return new ClasspathModifierQueries.ICreateFolderQuery() {

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
    }

    protected ClasspathModifierQueries.ICreateFolderQuery getNormalFolderCreationQuery() {
        return new ClasspathModifierQueries.ICreateFolderQuery() {

            public boolean doQuery() {
                return true;
            }

            public boolean isSourceFolder() {
                return false;
            }

            public IFolder getCreatedFolder() {
                return getFolderHandle(new Path(fNormalFolder));
            }

        };
    }

    protected IPath[] getPaths() throws JavaModelException {
        IClasspathEntry[] entries= fProject.getRawClasspath();
        IPath[] paths= new IPath[entries.length];
        for(int i= 0; i < entries.length; i++) {
            paths[i]= entries[i].getPath();
        }
        return paths;
    }

    protected void validateClasspath() throws JavaModelException {
        IJavaModelStatus status= JavaConventions.validateClasspath(fProject, fProject.getRawClasspath(), fProject.getOutputLocation());
        assertFalse(status.getSeverity() == IStatus.ERROR);
    }

    protected IPackageFragmentRoot getProjectRoot(IResource resource) throws JavaModelException {
        return ClasspathModifier.getFragmentRoot(resource, fProject, null);
    }

    protected static boolean contains(IPath path, IPath[] paths, IProgressMonitor monitor) {
        if (monitor == null)
            monitor= new NullProgressMonitor();
        if (path == null)
            return false;
        try {
            monitor.beginTask(NewWizardMessages.ClasspathModifier_Monitor_ComparePaths, paths.length);
            if (path.getFileExtension() == null)
				path= new Path(completeName(path.toString()));
            for (int i=0; i < paths.length; i++) {
                if (paths[i].equals(path))
                    return true;
                monitor.worked(1);
            }
        } finally {
           monitor.done();
       }
       return false;
    }

    /**
     * Add a '/' at the end of the name if
     * it does not end with '.java'.
     *
     * @param name append '/' at the end if
     * necessary
     * @return modified string
     */
    private static String completeName(String name) {
        if (!name.endsWith(".java")) { //$NON-NLS-1$
            name= name + "/"; //$NON-NLS-1$
            name= name.replace('.', '/');
            return name;
        }
        return name;
    }
}
