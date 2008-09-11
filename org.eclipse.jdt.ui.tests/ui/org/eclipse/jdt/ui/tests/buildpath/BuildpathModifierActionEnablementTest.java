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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ISetSelectionTarget;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.buildpath.ClasspathModifier;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.AddFolderToBuildpathAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.AddSelectedLibraryToBuildpathAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.BuildpathModifierAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.CreateLinkedSourceFolderAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.EditFilterAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.EditOutputFolderAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.ExcludeFromBuildpathAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.IncludeToBuildpathAction;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.newsourcepage.RemoveFromBuildpathAction;

public class BuildpathModifierActionEnablementTest extends TestCase {

    public static final Class THIS= BuildpathModifierActionEnablementTest.class;

    private BuildpathModifierAction[] fActions;

    private AddFolderToBuildpathAction fAddFolderToBuildpathAction;
	private RemoveFromBuildpathAction fRemoveFromBuildpathAction;
	private ExcludeFromBuildpathAction fExcludeFromBuildpathAction;
	private IncludeToBuildpathAction fIncludeToBuildpathAction;
	private EditFilterAction fEditFilterAction;
	private EditOutputFolderAction fEditOutputFolderAction;
	private CreateLinkedSourceFolderAction fCreateLinkedSourceFolderAction;

    private IJavaProject fProject;
    private IPackageFragmentRoot fSourceFolder;
    private IPackageFragment fDefaultPackage;
    private IFolder fFolder;
    private IFile fFile;
    private IPackageFragment fPackage;
    private IResource fExcludedPackage;
    private ICompilationUnit fCompilationUnit;
    private IResource fExcludedFile;
    private IPackageFragmentRoot fLibrary;
    private IFile fExcludedLibrary;

    /* ### Project Structure:
     * - DummyProject
     *        |- src
     *            |- default package
     *            |- pack1
     *                 |- A.java
     *                 |- B.java (excluded)
     *                 |- NormalFile
     *                 |- pack2 (excluded)
     *            |- archive.jar (on buildpath)
     *            |- archive.zip (excluded)
     *        |- NormalFolder
     */

    public BuildpathModifierActionEnablementTest() {
        super(THIS.getName());
    }

    protected void setUp() throws Exception {
    	fActions= createActions();
        fProject= createProject();
        assertFalse(fProject.isOnClasspath(fProject.getUnderlyingResource()));
    }

    private BuildpathModifierAction[] createActions() {
    	ISetSelectionTarget nullSelectionTarget= new ISetSelectionTarget() {
    		public void selectReveal(ISelection selection) {}
        };

        IRunnableContext context= PlatformUI.getWorkbench().getProgressService();

        fAddFolderToBuildpathAction= new AddFolderToBuildpathAction(context, nullSelectionTarget);
        fRemoveFromBuildpathAction= new RemoveFromBuildpathAction(context, nullSelectionTarget);
        fExcludeFromBuildpathAction= new ExcludeFromBuildpathAction(context, nullSelectionTarget);
        fIncludeToBuildpathAction= new IncludeToBuildpathAction(context, nullSelectionTarget);
        fEditFilterAction= new EditFilterAction(context, nullSelectionTarget);
        fEditOutputFolderAction= new EditOutputFolderAction(context, nullSelectionTarget);
        fCreateLinkedSourceFolderAction= new CreateLinkedSourceFolderAction(context, nullSelectionTarget);

        return new BuildpathModifierAction[] {
        		fAddFolderToBuildpathAction,
                fRemoveFromBuildpathAction,
                fExcludeFromBuildpathAction,
                fIncludeToBuildpathAction,
                fEditFilterAction,
                fEditOutputFolderAction,
                fCreateLinkedSourceFolderAction
        };
    }

	protected void tearDown() throws Exception {
        fProject.getProject().delete(true, true, null);
    }

    private void assertOnlyEnabled(IAction[] enabledActions) {
    	for (int i= 0; i < fActions.length; i++) {
	        if (fActions[i].isEnabled()) {
	        	assertTrue(fActions[i].getText() + " is enabled but should not be.", contains(enabledActions, fActions[i]));
	        } else {
	        	assertTrue(fActions[i].getText() + " is disabled but should not be.", !contains(enabledActions, fActions[i]));
	        }
        }
    }

	private boolean contains(IAction[] actions, IAction action) {
    	for (int i= 0; i < actions.length; i++) {
	        if (actions[i] == action)
	        	return true;
        }
	    return false;
    }

	private void assertAllDisabled() {
		for (int i= 0; i < fActions.length; i++) {
	        if (fActions[i].isEnabled())
	        	assertTrue(fActions[i].getText() + " is enabled but should not be.", false);
        }
    }

    public void testProjectWithOthers() {
        select(new Object[] {fProject});
        assertOnlyEnabled(new IAction[] {fAddFolderToBuildpathAction, fCreateLinkedSourceFolderAction});

        select(new Object[] {fProject, fSourceFolder});
        assertAllDisabled();

        select(new Object[] {fProject, fFolder});
        assertOnlyEnabled(new IAction[] {fAddFolderToBuildpathAction});

        select(new Object[] {fProject, fPackage});
        assertOnlyEnabled(new IAction[] {fAddFolderToBuildpathAction});

        select(new Object[] {fProject, fCompilationUnit});
        assertAllDisabled();

        select(new Object[] {fProject, fExcludedFile});
        assertAllDisabled();

        select(new Object[] {fProject, fFile});
        assertAllDisabled();

        select(new Object[] {fProject, fExcludedPackage});
        assertOnlyEnabled(new IAction[] {fAddFolderToBuildpathAction});

        select(new Object[] {fProject, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fProject, fLibrary});
        assertAllDisabled();

        select(new Object[] {fProject, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fProject, fSourceFolder, fFolder});
        assertAllDisabled();

        select(new Object[] {fProject, fSourceFolder, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fProject, fFolder, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fProject, fFolder, fLibrary});
        assertAllDisabled();

        select(new Object[] {fProject, fLibrary, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder});
        assertOnlyEnabled(new IAction[] {fAddFolderToBuildpathAction});

        select(new Object[] {fProject, fPackage, fFolder, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage});
        assertOnlyEnabled(new IAction[] {fAddFolderToBuildpathAction});

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fLibrary});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fCompilationUnit});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fLibrary});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fExcludedPackage, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fExcludedPackage, fLibrary});
        assertAllDisabled();

        select(new Object[] {fProject, fSourceFolder, fPackage, fFolder, fCompilationUnit});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fCompilationUnit});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fCompilationUnit, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fExcludedFile});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fExcludedFile, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fFile});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fFile, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fCompilationUnit, fExcludedFile});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fCompilationUnit, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fCompilationUnit, fFile});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fCompilationUnit, fFile, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fExcludedFile, fFile});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fExcludedFile, fFile, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fCompilationUnit, fFile, fExcludedFile});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fCompilationUnit, fFile, fExcludedFile, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fProject, fPackage, fFolder, fExcludedPackage, fCompilationUnit, fFile, fExcludedFile, fDefaultPackage, fLibrary, fExcludedLibrary});
        assertAllDisabled();
    }

    public void testSrcWithOthers() {
        select(new Object[] {fSourceFolder});
        assertOnlyEnabled(new IAction[] {fRemoveFromBuildpathAction, fEditFilterAction});

        select(new Object[] {fSourceFolder, fFolder});
        assertAllDisabled();

        select(new Object[] {fSourceFolder, fPackage});
        assertAllDisabled();

        select(new Object[] {fSourceFolder, fCompilationUnit});
        assertAllDisabled();

        select(new Object[] {fSourceFolder, fExcludedFile});
        assertAllDisabled();

        select(new Object[] {fSourceFolder, fFile});
        assertAllDisabled();

        select(new Object[] {fSourceFolder, fExcludedPackage});
        assertAllDisabled();

        select(new Object[] {fSourceFolder, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fSourceFolder, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fSourceFolder, fLibrary});
        assertOnlyEnabled(new IAction[] {fRemoveFromBuildpathAction});

        select(new Object[] {fSourceFolder, fLibrary, fFolder});
        assertAllDisabled();

        select(new Object[] {fSourceFolder, fLibrary, fPackage});
        assertAllDisabled();

        select(new Object[] {fSourceFolder, fLibrary, fExcludedPackage});
        assertAllDisabled();

        select(new Object[] {fSourceFolder, fFolder, fPackage, fCompilationUnit, fExcludedFile, fFile, fExcludedPackage, fDefaultPackage, fLibrary, fExcludedLibrary});
        assertAllDisabled();
    }

    public void testNormalFolderWithOthers() {
        select(new Object[] {fFolder});
        assertOnlyEnabled(new IAction[] {fAddFolderToBuildpathAction});

        select(new Object[] {fFolder, fPackage});
        assertOnlyEnabled(new IAction[] {fAddFolderToBuildpathAction});

        select(new Object[] {fFolder, fCompilationUnit});
        assertAllDisabled();

        select(new Object[] {fFolder, fExcludedFile});
        assertAllDisabled();

        select(new Object[] {fFolder, fFile});
        assertAllDisabled();

        select(new Object[] {fFolder, fExcludedPackage});
        assertOnlyEnabled(new IAction[] {fAddFolderToBuildpathAction});

        select(new Object[] {fFolder, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fFolder, fLibrary});
        assertAllDisabled();

        select(new Object[] {fFolder, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fFolder, fPackage, fCompilationUnit});
        assertAllDisabled();

        select(new Object[] {fFolder, fPackage, fFile});
        assertAllDisabled();

        select(new Object[] {fFolder, fPackage, fExcludedPackage});
        assertOnlyEnabled(new IAction[] {fAddFolderToBuildpathAction});

        select(new Object[] {fFolder, fPackage, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fFolder, fPackage, fLibrary});
        assertAllDisabled();

        select(new Object[] {fFolder, fPackage, fCompilationUnit, fFile});
        assertAllDisabled();

        select(new Object[] {fFolder, fPackage, fExcludedPackage, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fFolder, fPackage, fExcludedPackage, fExcludedFile});
        assertAllDisabled();

        select(new Object[] {fFolder, fPackage, fExcludedPackage, fExcludedFile});
        assertAllDisabled();

        select(new Object[] {fFolder, fPackage, fExcludedFile, fLibrary});
        assertAllDisabled();

        select(new Object[] {fFolder, fPackage, fExcludedFile, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fFolder, fPackage, fExcludedPackage, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fFolder, fPackage, fExcludedPackage, fLibrary});
        assertAllDisabled();
    }

    public void testPackageWithOthers() {
        select(new Object[] {fPackage});
        assertOnlyEnabled(new IAction[] {fAddFolderToBuildpathAction, fExcludeFromBuildpathAction});

        select(new Object[] {fPackage, fCompilationUnit});
        assertOnlyEnabled(new IAction[] {fExcludeFromBuildpathAction});

        select(new Object[] {fPackage, fExcludedFile});
        assertAllDisabled();

        select(new Object[] {fPackage, fFile});
        assertAllDisabled();

        select(new Object[] {fPackage, fExcludedPackage});
        assertOnlyEnabled(new IAction[] {fAddFolderToBuildpathAction});

        select(new Object[] {fPackage, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fPackage, fLibrary});
        assertAllDisabled();

        select(new Object[] {fPackage, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fPackage, fExcludedPackage, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fPackage, fExcludedPackage, fLibrary});
        assertAllDisabled();

        select(new Object[] {fPackage, fExcludedPackage, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fPackage, fExcludedPackage, fCompilationUnit, fFile, fExcludedFile});
        assertAllDisabled();

        select(new Object[] {fPackage, fExcludedPackage, fCompilationUnit, fFile, fLibrary});
        assertAllDisabled();

        select(new Object[] {fPackage, fExcludedPackage, fCompilationUnit, fFile, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fPackage, fExcludedPackage, fCompilationUnit, fFile, fExcludedFile, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fPackage, fExcludedPackage, fCompilationUnit, fFile, fExcludedFile, fDefaultPackage, fLibrary});
        assertAllDisabled();

        select(new Object[] {fPackage, fExcludedPackage, fCompilationUnit, fFile, fExcludedFile, fDefaultPackage, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fPackage, fExcludedPackage, fCompilationUnit, fFile, fExcludedFile, fDefaultPackage, fLibrary, fExcludedLibrary});
        assertAllDisabled();
    }

    public void testCUWithOthers() {
        select(new Object[] {fCompilationUnit});
        assertOnlyEnabled(new IAction[] {fExcludeFromBuildpathAction});

        select(new Object[] {fCompilationUnit, fPackage});
        assertOnlyEnabled(new IAction[] {fExcludeFromBuildpathAction});

        select(new Object[] {fCompilationUnit, fExcludedFile});
        assertAllDisabled();

        select(new Object[] {fCompilationUnit, fFile});
        assertAllDisabled();

        select(new Object[] {fCompilationUnit, fExcludedPackage});
        assertAllDisabled();

        select(new Object[] {fCompilationUnit, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fCompilationUnit, fLibrary});
        assertAllDisabled();

        select(new Object[] {fCompilationUnit, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fCompilationUnit, fExcludedPackage, fPackage});
        assertAllDisabled();

        select(new Object[] {fCompilationUnit, fExcludedPackage, fLibrary});
        assertAllDisabled();

        select(new Object[] {fCompilationUnit, fExcludedPackage, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fCompilationUnit, fPackage, fLibrary});
        assertAllDisabled();

        select(new Object[] {fCompilationUnit, fPackage, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fCompilationUnit,fExcludedFile, fFile, fExcludedPackage, fDefaultPackage, fLibrary, fExcludedLibrary});
        assertAllDisabled();
    }

    public void testExcludedFileWithOthers() {
        select(new Object[] {fExcludedFile});
        assertOnlyEnabled(new IAction[] {fIncludeToBuildpathAction});

        select(new Object[] {fExcludedFile, fExcludedPackage});
        assertOnlyEnabled(new IAction[] {fIncludeToBuildpathAction});

        select(new Object[] {fExcludedFile, fFile});
        assertAllDisabled();

        select(new Object[] {fExcludedFile, fExcludedPackage});
        assertOnlyEnabled(new IAction[] {fIncludeToBuildpathAction});

        select(new Object[] {fExcludedFile, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fExcludedFile, fLibrary});
        assertAllDisabled();

        select(new Object[] {fExcludedFile, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fExcludedFile, fExcludedPackage, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fExcludedFile, fExcludedPackage, fLibrary});
        assertAllDisabled();

        select(new Object[] {fExcludedFile, fExcludedPackage, fExcludedLibrary});
        assertAllDisabled();
    }

    public void testFileWithOthers() {
        select(new Object[] {fFile});
        assertAllDisabled();

        select(new Object[] {fFile, fExcludedPackage});
        assertAllDisabled();

        select(new Object[] {fFile, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fFile, fLibrary});
        assertAllDisabled();

        select(new Object[] {fFile, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fFile, fExcludedPackage, fLibrary, fExcludedLibrary});
        assertAllDisabled();
    }

    public void testExcludedPackWithOthers() {
        select(new Object[] {fExcludedPackage});
        assertOnlyEnabled(new IAction[] {fAddFolderToBuildpathAction, fIncludeToBuildpathAction});

        select(new Object[] {fExcludedPackage, fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fExcludedPackage, fLibrary});
        assertAllDisabled();

        select(new Object[] {fExcludedPackage, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fExcludedPackage, fDefaultPackage, fLibrary});
        assertAllDisabled();

        select(new Object[] {fExcludedPackage, fDefaultPackage, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fExcludedPackage, fLibrary, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fExcludedPackage, fDefaultPackage, fLibrary, fExcludedLibrary});
        assertAllDisabled();
    }

    public void testDefaultPackWithOthers() {
        select(new Object[] {fDefaultPackage});
        assertAllDisabled();

        select(new Object[] {fDefaultPackage, fLibrary});
        assertAllDisabled();

        select(new Object[] {fDefaultPackage, fExcludedLibrary});
        assertAllDisabled();

        select(new Object[] {fDefaultPackage, fLibrary, fExcludedLibrary});
        assertAllDisabled();
    }

    public void testDefaultJARWithOthers() {
        select(new Object[] {fLibrary});
        assertOnlyEnabled(new IAction[] {fRemoveFromBuildpathAction});

        select(new Object[] {fLibrary, fExcludedLibrary});
        assertAllDisabled();
    }

    public void testDefaultZipWithOthers() {
        select(new Object[] {fExcludedLibrary});
        assertAllDisabled();

        final IPackageFragmentRoot[] addedZipArchive= {null};
        AddSelectedLibraryToBuildpathAction add= new AddSelectedLibraryToBuildpathAction(PlatformUI.getWorkbench().getProgressService(), new ISetSelectionTarget() {
			public void selectReveal(ISelection selection) {
				addedZipArchive[0]= (IPackageFragmentRoot)((StructuredSelection)selection).getFirstElement();
            }
        });
        add.selectionChanged(new SelectionChangedEvent(new ISelectionProvider() {
			public void addSelectionChangedListener(ISelectionChangedListener listener) {}
			public ISelection getSelection() {return new StructuredSelection(fExcludedLibrary);}
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {}
			public void setSelection(ISelection selection) {}
        }, new StructuredSelection(fExcludedLibrary)));
        add.run();

        select(new StructuredSelection(new Object[] {addedZipArchive[0], fLibrary}));
        assertOnlyEnabled(new IAction[] {fRemoveFromBuildpathAction});
    }

	private IJavaProject createProject() throws CoreException {
        fProject= JavaProjectHelper.createJavaProject("Dummy project", "bin");
        IPath srcPath= new Path("src");
        IPath normalFolderPath= new Path("NormalFolder");
        IPath packagePath= srcPath.append("pack1");
        IPath filePath= packagePath.append("NormalFile");

        // src folder
        IFolder folder= fProject.getProject().getFolder(srcPath);
        CoreUtility.createFolder(folder, true, true, null);

        // one normal folder
        IFolder folder2= fProject.getProject().getFolder(normalFolderPath);
        CoreUtility.createFolder(folder, true, true, null);

        final IPath projectPath= fProject.getProject().getFullPath();

        // configure the classpath entries, including the default jre library.
        List cpEntries= new ArrayList();
        cpEntries.add(JavaCore.newSourceEntry(projectPath.append(srcPath)));
        cpEntries.addAll(Arrays.asList(PreferenceConstants.getDefaultJRELibrary()));
        IClasspathEntry[] entries= (IClasspathEntry[]) cpEntries.toArray(new IClasspathEntry[cpEntries.size()]);
        fProject.setRawClasspath(entries, null);

        // one package in src folder
        IPackageFragmentRoot root= fProject.findPackageFragmentRoot(fProject.getPath().append(srcPath));
        IPackageFragment pack1= root.createPackageFragment("pack1", true, null);
        IPackageFragment defaultPack= root.getPackageFragment("");

        IPath libraryPath= root.getPath().append("archive.jar");
        IPackageFragmentRoot jarRoot= JavaProjectHelper.addLibrary(fProject, libraryPath);
        assertFalse(ClasspathModifier.getClasspathEntryFor(jarRoot.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        libraryPath= root.getPath().append("archive.zip");
        IFile zipFile= fProject.getProject().getWorkspace().getRoot().getFile(libraryPath);
        zipFile.create(new ByteArrayInputStream(new byte[] {}), true, null);
        final IPackageFragmentRoot zipRoot= JavaProjectHelper.addLibrary(fProject, libraryPath);
        assertFalse(ClasspathModifier.getClasspathEntryFor(zipRoot.getPath(), fProject, IClasspathEntry.CPE_LIBRARY) == null);

        // two compilation units A and B in 'package'
        ICompilationUnit cuA= createICompilationUnit("A", pack1);
        final IResource excludedElements[]= {null, null};
        final IPackageFragment pack2= root.createPackageFragment("pack1.pack2", true, null);
        final ICompilationUnit cuB= createICompilationUnit("B", pack1);
        ExcludeFromBuildpathAction exclude= new ExcludeFromBuildpathAction(PlatformUI.getWorkbench().getProgressService(), new ISetSelectionTarget() {

			public void selectReveal(ISelection selection) {
				StructuredSelection ss= (StructuredSelection)selection;
				List list= ss.toList();
				excludedElements[0]= (IResource)list.get(0);
				excludedElements[1]= (IResource)list.get(1);
            }

        });
        exclude.selectionChanged(new SelectionChangedEvent(new ISelectionProvider() {
			public void addSelectionChangedListener(ISelectionChangedListener listener) {}
			public ISelection getSelection() {return new StructuredSelection(new Object[] {cuB, pack2});}
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {}
			public void setSelection(ISelection selection) {}
        }, new StructuredSelection(new Object[] {cuB, pack2})));
        exclude.run();

        IFile file= fProject.getProject().getFile(filePath);
        file.create(null, false, null);

        final IFile[] removedZipFile= {null};
        RemoveFromBuildpathAction remove= new RemoveFromBuildpathAction(PlatformUI.getWorkbench().getProgressService(), new ISetSelectionTarget() {

			public void selectReveal(ISelection selection) {
				removedZipFile[0]= (IFile)((StructuredSelection)selection).getFirstElement();
            }

        });
        remove.selectionChanged(new SelectionChangedEvent(new ISelectionProvider() {
			public void addSelectionChangedListener(ISelectionChangedListener listener) {}
			public ISelection getSelection() {return new StructuredSelection(zipRoot);}
			public void removeSelectionChangedListener(ISelectionChangedListener listener) {}
			public void setSelection(ISelection selection) {}
        }, new StructuredSelection(zipRoot)));
        remove.run();

        fSourceFolder= root;
        fFolder= folder2;
        fPackage= pack1;
        fCompilationUnit= cuA;
        fExcludedFile= excludedElements[0];
        fFile= file;
        fExcludedPackage= excludedElements[1];
        fDefaultPackage= defaultPack;
        fLibrary= jarRoot;
        fExcludedLibrary= removedZipFile[0];

        return fProject;
    }

    private void select(Object[] objs) {
        select(new StructuredSelection(objs));
    }

	private void select(final StructuredSelection selection) {
	    for (int i= 0; i < fActions.length; i++) {
	        fActions[i].selectionChanged(new SelectionChangedEvent(new ISelectionProvider(){
				public void addSelectionChangedListener(ISelectionChangedListener listener) {}
				public ISelection getSelection() {
	                return selection;
                }
				public void removeSelectionChangedListener(ISelectionChangedListener listener) {}
				public void setSelection(ISelection s) {}
	        }, selection));
        }
    }

	private ICompilationUnit createICompilationUnit(String className, IPackageFragment fragment) throws JavaModelException {
        String packString= fragment.getElementName().equals("") ? fragment.getElementName() : "package " + fragment.getElementName() +";\n";
        StringBuffer content= getFileContent(className, packString);
        return fragment.createCompilationUnit(className+".java", content.toString(), false, null);
    }

    private StringBuffer getFileContent(String className, String packageHeader) {
        StringBuffer buf= new StringBuffer();
        buf.append(packageHeader);
        buf.append("\n");
        buf.append("public class "+className+ " {\n");
        buf.append("    public void foo() {\n");
        buf.append("    }\n");
        buf.append("}\n");
        return buf;
    }

	public static Test suite() {
		return new TestSuite(BuildpathModifierActionEnablementTest.class);
	}
}
