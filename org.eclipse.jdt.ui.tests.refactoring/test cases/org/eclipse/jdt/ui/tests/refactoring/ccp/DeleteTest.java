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
package org.eclipse.jdt.ui.tests.refactoring.ccp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.commands.operations.IOperationHistory;
import org.eclipse.core.commands.operations.IUndoContext;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.operations.IWorkbenchOperationSupport;

import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.DeleteRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.reorg.IReorgQueries;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaDeleteProcessor;

import org.eclipse.jdt.ui.tests.refactoring.GenericRefactoringTest;
import org.eclipse.jdt.ui.tests.refactoring.ParticipantTesting;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

//Last tests need to delete package p. Make sure they are really last to run:
// Tests starting with "test_END_DeletePackageSub" should run last
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DeleteTest extends GenericRefactoringTest {

	private static final String REFACTORING_PATH= "Delete/";

	public DeleteTest() {
		rts= new RefactoringTestSetup();
	}

	@Before
	public void setUp() throws Exception {
		fIsPreDeltaTest= true;
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void verifyDisabled(Object[] elements) throws CoreException {
		JavaDeleteProcessor processor= new JavaDeleteProcessor(elements);
		DeleteRefactoring ref= new DeleteRefactoring(processor);
		assertFalse("delete should be disabled", ref.isApplicable());
	}

	private void verifyEnabled(Object[] elements) throws CoreException {
		JavaDeleteProcessor processor= new JavaDeleteProcessor(elements);
		DeleteRefactoring ref= new DeleteRefactoring(processor);
		assertTrue("delete should be enabled", ref.isApplicable());
	}

	private IPackageFragmentRoot getArchiveRoot() throws JavaModelException, Exception {
		IPackageFragmentRoot archive= null;
		for (IPackageFragmentRoot root : rts.getProject().getPackageFragmentRoots()) {
			if (root.isArchive() && root.isExternal())
				archive= root;
		}
		return archive;
	}

	private ICompilationUnit fCuA;
	private static final String CU_NAME= "A";

	private void loadFileSetup() throws Exception{
		fCuA= createCUfromTestFile(getPackageP(), CU_NAME);
		assertTrue("A.java does not exist", fCuA.exists());
	}

	private void checkDelete(IJavaElement[] elems, boolean deleteCu) throws JavaModelException, Exception {
		DeleteRefactoring refactoring= createRefactoring(elems);
		assertNotNull(refactoring);
		RefactoringStatus status= performRefactoring(refactoring, true);
		assertNull("precondition was supposed to pass", status);

		ICompilationUnit newCuA= getPackageP().getCompilationUnit(CU_NAME + ".java");
		assertEquals("A.java does not exist", newCuA.exists(), !deleteCu);
		if (! deleteCu)
			assertEqualLines("incorrect content of A.java", getFileContents(getOutputTestFileName(CU_NAME)), newCuA.getSource());
	}

	private DeleteRefactoring createRefactoring(Object[] elements) {
		JavaDeleteProcessor processor= new JavaDeleteProcessor(elements);
		DeleteRefactoring result= new DeleteRefactoring(processor);
		processor.setQueries(createReorgQueries());
		return result;
	}

	// package helpers

	private IPackageFragment[] createPackagePath(int no) throws JavaModelException {
		IPackageFragment[] frags= new IPackageFragment[no];
		for (int i=0; i<no; i++) {
			frags[i]= getRoot().createPackageFragment(getPackageName(i), true, new NullProgressMonitor());
		}
		return frags;
	}

	private String getPackageName(int i) {
		StringBuilder buf= new StringBuilder();
		for (int j= 0; j <= i; j++) {
			if (j>0)
				buf.append(".");
			buf.append("a");
			buf.append(j);
		}
		return buf.toString();
	}

	private void executeDeletePackage(Object[] markedForDelete, IPackageFragment[] packsToBeDeleted, Object[] othersToBeDeleted) throws CoreException, Exception {
		executeDeletePackage(markedForDelete, packsToBeDeleted, othersToBeDeleted, false);
	}

	/**
	 * Execute a package delete.
	 * @param markedForDelete The elements selected for deletion ("in the UI")
	 * @param packsToBeDeleted First half of elements which must be deleted after the refactoring
	 * @param othersToBeDeleted Second half (halfs will be merged).
	 * @param deleteSubs true if subpackages should be deleted as well.
	 * @throws CoreException xxx
	 * @throws Exception xxx
	 */
	private void executeDeletePackage(Object[] markedForDelete, IPackageFragment[] packsToBeDeleted, Object[] othersToBeDeleted, boolean deleteSubs) throws CoreException, Exception {

		List<Object> allList= new ArrayList<>();
		allList.addAll(Arrays.asList(packsToBeDeleted));
		allList.addAll(Arrays.asList(othersToBeDeleted));

		Object[] all= allList.toArray();

		ParticipantTesting.reset();
		String[] deleteHandles= ParticipantTesting.createHandles(all);

		verifyEnabled(markedForDelete);
		mustPerformDummySearch();
		DeleteRefactoring ref= createRefactoring(markedForDelete);
		((JavaDeleteProcessor)ref.getProcessor()).setDeleteSubPackages(deleteSubs);
		RefactoringStatus status= performRefactoring(ref, true);
		assertNull("expected to pass", status);

		// assure participants got notified of everything.
		ParticipantTesting.testDelete(deleteHandles);
	}

	private void doTestUndoRedo(Object[] dontExist, Object[] exist) throws CoreException {
		assertExist(exist, true);
		assertExist(dontExist, false);

		IUndoManager undoManager= RefactoringCore.getUndoManager();
		undoManager.performUndo(null, new NullProgressMonitor());
		assertExist(exist, true);
		assertExist(dontExist, true);

		undoManager.performRedo(null, new NullProgressMonitor());
		assertExist(exist, true);
		assertExist(dontExist, false);
	}

	private void assertExist(Object[] resourceOrElements, boolean exists) {
		if (resourceOrElements != null) {
			for (Object resourceOrElement : resourceOrElements) {
				assertExists(resourceOrElement, exists);
			}
		}
	}

	private void assertExists(Object resourceOrElement, boolean exists) {
		if (resourceOrElement instanceof IResource) {
			IResource resource= (IResource) resourceOrElement;
			if (exists) {
				assertTrue("expected to exist: " + resource.getFullPath(), resource.exists());
			} else {
				assertFalse("expected NOT to exist: " + resource.getFullPath(), resource.exists());
			}
		} else 	if (resourceOrElement instanceof IJavaElement) {
			IJavaElement javaElement= (IJavaElement) resourceOrElement;
			if (exists) {
				assertTrue("expected to exist: " + javaElement.getHandleIdentifier(), javaElement.exists());
			} else {
				assertFalse("expected NOT to exist: " + javaElement.getHandleIdentifier(), javaElement.exists());
				IResource resource= javaElement.getResource();
				assertFalse("expected NOT to exist: " + resource.getFullPath(), resource.exists());
			}
		}
	}

	//---- tests

	private IReorgQueries createReorgQueries() {
		return new MockReorgQueries();
	}

	@Test
	public void testDisabled_emptySelection() throws Exception{
		verifyDisabled(new Object[] {});
	}

	@Test
	public void testDisabled_projectAndNonProject() throws Exception{
		IJavaElement[] javaElements= {rts.getProject(), getPackageP()};
		verifyDisabled(javaElements);
	}

	@Test
	public void testDisabled_nonExistingResource() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();
		IFile file= folder.getFile("a.txt");

		IResource[] resources= {file};
		verifyDisabled(resources);
	}

	@Test
	public void testDisabled_nonExistingJavaElement() throws Exception{
		IJavaElement notExistingCu= getPackageP().getCompilationUnit("V.java");

		IJavaElement[] javaElements= {notExistingCu};
		verifyDisabled(javaElements);
	}

	@Test
	public void testDisabled_nullResource() throws Exception{
		Object[] elements= {rts.getProject(), null};
		verifyDisabled(elements);
	}

	@Test
	public void testDisabled_nullJavaElement() throws Exception{
		Object[] elements= {getPackageP(), null};
		verifyDisabled(elements);
	}

	@Test
	public void testDisabled_archiveElement() throws Exception{
		IPackageFragmentRoot archive= getArchiveRoot();
		assertNotNull(archive);

		Object[] elements= archive.getChildren();
		verifyDisabled(elements);
	}

	@Test
	public void testDisabled_externalArchive() throws Exception{
		IPackageFragmentRoot archive= getArchiveRoot();
		assertNotNull(archive);

		Object[] elements= {archive};
		verifyDisabled(elements);
	}

	@Test
	public void testDisabled_archiveFromAnotherProject() throws Exception{
		//TODO implement me
	}

	@Test
	public void testDisabled_binaryMember() throws Exception{
		//TODO implement me
	}

	@Test
	public void testDisabled_javaProject() throws Exception{
		Object[] elements= {rts.getProject()};
		verifyDisabled(elements);
	}

	@Test
	public void testDisabled_defaultPackage() throws Exception {
		IPackageFragment defaultPackage= getRoot().getPackageFragment("");
		defaultPackage.createCompilationUnit("A.java", "", false, new NullProgressMonitor());

		Object[] elements= {defaultPackage};
		verifyDisabled(elements);
	}

	@Test
	public void testDisabled_simpleProject() throws Exception{
		Object[] elements= {rts.getProject().getProject()};
		verifyDisabled(elements);
	}

	@Test
	public void testEnabled_cu() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "", false, new NullProgressMonitor());

		Object[] elements= {cu};
		verifyEnabled(elements);
	}

	@Test
	public void testEnabled_sourceReferences1() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "", false, new NullProgressMonitor());
		IJavaElement importD= cu.createImport("java.lang.*", null, new NullProgressMonitor());
		IJavaElement packageD= cu.createPackageDeclaration("p", new NullProgressMonitor());
		IJavaElement type= cu.createType("class A{}", null, false, new NullProgressMonitor());

		Object[] elements= {packageD, importD, type};
		verifyEnabled(elements);
	}

	@Test
	public void testEnabled_sourceReferences2() throws Exception{
		ICompilationUnit cu= getPackageP().createCompilationUnit("A.java", "", false, new NullProgressMonitor());
		IType type= cu.createType("class A{}", null, false, new NullProgressMonitor());
		IJavaElement field= type.createField("int i;", null, false, new NullProgressMonitor());
		IJavaElement method= type.createMethod("void f(){}", null, false, new NullProgressMonitor());
		IJavaElement initializer= type.createInitializer("{ int k= 0;}", null, new NullProgressMonitor());
		IJavaElement innerType= type.createType("class Inner{}", null, false,  new NullProgressMonitor());

		Object[] elements= {field, method, initializer, innerType};
		verifyEnabled(elements);
	}


	@Test
	public void testEnabled_file() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();
		IFile file= folder.getFile("a.txt");
		file.create(getStream("123"), true, null);
		Object[] elements= {file};
		verifyEnabled(elements);
	}

	@Test
	public void testEnabled_folder() throws Exception{
		IFolder folder= (IFolder)getPackageP().getResource();

		Object[] elements= {folder};
		verifyEnabled(elements);
	}

	@Test
	public void testEnabled_readOnlyCu() throws Exception{
		//TODO implement me
	}

	@Test
	public void testEnabled_readOnlyFile() throws Exception{
		//TODO implement me
	}

	@Test
	public void testEnabled_package() throws Exception{
		Object[] elements= {getPackageP()};
		verifyEnabled(elements);
	}

	@Test
	public void testEnabled_sourceFolder() throws Exception{
		Object[] elements= {getRoot()};
		verifyEnabled(elements);
	}

	@Test
	public void testEnabled_linkedFile() throws Exception{
		//TODO implement me
	}

	@Test
	public void testEnabled_linkedFolder() throws Exception{
		//TODO implement me
	}

	@Test
	public void testEnabled_linkedPackage() throws Exception{
		//TODO implement me
	}

	@Test
	public void testEnabled_linkedSourceFolder() throws Exception{
		//TODO implement me
	}

	@Test
	public void testDeleteWithinCu0() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("i");
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);
		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu1() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A");
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(fCuA, elem0, fCuA.getResource());

		checkDelete(elems, true);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu2() throws Exception{
		loadFileSetup();
		ParticipantTesting.reset();
		IJavaElement elem0= fCuA.getType("A").getField("i");
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu3() throws Exception{
		loadFileSetup();
		ParticipantTesting.reset();
		IJavaElement elem0= fCuA.getType("A").getField("i");
		IJavaElement elem1= fCuA.getType("A").getField("j");
		IJavaElement[] elems= new IJavaElement[]{elem0, elem1};
		String[] handles= ParticipantTesting.createHandles(elem0, elem1);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu4() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("i");
		IJavaElement elem1= fCuA.getType("A").getField("k");
		IJavaElement[] elems= new IJavaElement[]{elem0, elem1};
		String[] handles= ParticipantTesting.createHandles(elem0, elem1);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu5() throws Exception{
		loadFileSetup();
		ParticipantTesting.reset();
		IJavaElement elem0= fCuA.getType("A").getField("j");
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu6() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("j");
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu7() throws Exception{
		//exposes bug#9381 IPackageDeclaration is not ISourceManipulation
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getPackageDeclaration("p");
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu8() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getMethod("m", new String[0]);
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu9() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getInitializer(1);
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu10() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getInitializer(1);
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu11() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getImport("java.util.List");
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu12() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getType("B");
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu13() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getType("B");
		IJavaElement elem1= fCuA.getType("A");
		IJavaElement[] elems= new IJavaElement[]{elem0, elem1};
		String[] handles= ParticipantTesting.createHandles(fCuA, fCuA.getTypes()[0], fCuA.getResource());

		checkDelete(elems, true);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu14() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getType("B");
		IJavaElement elem1= fCuA.getType("A");
		IJavaElement elem2= fCuA.getPackageDeclaration("p");
		IJavaElement[] elems= new IJavaElement[]{elem0, elem1, elem2};
		String[] handles= ParticipantTesting.createHandles(fCuA, fCuA.getTypes()[0], fCuA.getResource());

		checkDelete(elems, true);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu15() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("field");
		IJavaElement getter= fCuA.getType("A").getMethod("getField", new String[] {});
		IJavaElement setter= fCuA.getType("A").getMethod("setField", new String[] {"I"});
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0, getter, setter);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu16() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("Test");
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu17() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getMethod("f", new String[0]);
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu18() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getMethod("fs", new String[0]);
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu19() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getImportContainer();
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu20() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("fEmpty");
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu21() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("A").getField("var11");
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu22() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IJavaElement elem0= fCuA.getType("B");
		IJavaElement[] elems= new IJavaElement[]{elem0};
		String[] handles= ParticipantTesting.createHandles(elem0);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu23() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();
		IType typeA= fCuA.getType("A");
		IJavaElement[] elems= new IJavaElement[]{
				typeA.getField("nestingDepth"), typeA.getField("openOnRun"),
				typeA.getMethod("getNestingDepth", new String[0]), typeA.getMethod("getOpenOnRun", new String[0])
		};
		String[] handles= ParticipantTesting.createHandles(elems);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu24() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();

		int indexOf= fCuA.getSource().indexOf("Thread");
		IType type= (IType) fCuA.getElementAt(indexOf);
		IJavaElement[] elems= new IJavaElement[] { type };

		String[] handles= ParticipantTesting.createHandles(elems);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu25() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();

		int indexOf= fCuA.getSource().indexOf("Thread");
		IType type= (IType) fCuA.getElementAt(indexOf);
		IJavaElement[] elems= new IJavaElement[] { type };

		String[] handles= ParticipantTesting.createHandles(elems);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteWithinCu26() throws Exception{
		ParticipantTesting.reset();
		loadFileSetup();

		int indexOf= fCuA.getSource().indexOf("Y");
		IType type= (IType) fCuA.getElementAt(indexOf);
		IJavaElement[] elems= new IJavaElement[] { type };

		String[] handles= ParticipantTesting.createHandles(elems);

		checkDelete(elems, false);
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteFile() throws Exception{
		ParticipantTesting.reset();
		IFolder folder= (IFolder)getPackageP().getResource();
		String content= "123";
		IFile file= folder.getFile("a.txt");
		file.create(getStream(content), true, null);
		assertTrue("file does not exist", file.exists());
		Object[] elem= {file};
		verifyEnabled(elem);
		mustPerformDummySearch();

		String[] handles= ParticipantTesting.createHandles(file);

		DeleteRefactoring ref= createRefactoring(elem);
		RefactoringStatus status= performRefactoring(ref, true);
		assertNull("expected to pass", status);
		assertFalse("file not deleted", file.exists());
		ParticipantTesting.testDelete(handles);

		IUndoManager undoManager= RefactoringCore.getUndoManager();
		assertFalse(undoManager.anythingToRedo());
		assertTrue(undoManager.anythingToUndo());
		undoManager.performUndo(null, new NullProgressMonitor());
		assertTrue(file.exists());
		assertEquals(content, getContents(file));

		assertTrue(undoManager.anythingToRedo());
		undoManager.performRedo(null, new NullProgressMonitor());
		assertFalse(file.exists());
	}

	@Test
	public void testDeleteFolder() throws Exception{
		ParticipantTesting.reset();
		IFolder folder= (IFolder)getPackageP().getResource();
		IFolder subFolder= folder.getFolder("subFolder");
		subFolder.create(true, true, null);

		assertTrue("folder does not exist", subFolder.exists());
		Object[] elements= {subFolder};
		verifyEnabled(elements);
		mustPerformDummySearch();

		String[] handles= ParticipantTesting.createHandles(subFolder);
		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, true);
		assertNull("expected to pass", status);
		assertFalse("folder not deleted", subFolder.exists());
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteNestedFolders() throws Exception{
		ParticipantTesting.reset();
		IFolder folder= (IFolder)getPackageP().getResource();
		IFolder subFolder= folder.getFolder("subFolder");
		subFolder.create(true, true, null);
		IFolder subsubFolder= subFolder.getFolder("subSubFolder");
		subsubFolder.create(true, true, null);

		assertTrue("folder does not exist", subFolder.exists());
		assertTrue("folder does not exist", subsubFolder.exists());
		Object[] elements= {subFolder, subsubFolder};
		verifyEnabled(elements);
		mustPerformDummySearch();

		String[] handles= ParticipantTesting.createHandles(subFolder);
		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, true);
		assertNull("expected to pass", status);
		assertFalse("folder not deleted", subFolder.exists());
		assertFalse("folder not deleted", subsubFolder.exists());
		ParticipantTesting.testDelete(handles);

		IUndoManager undoManager= RefactoringCore.getUndoManager();
		assertFalse(undoManager.anythingToRedo());
		assertTrue(undoManager.anythingToUndo());
		undoManager.performUndo(null, new NullProgressMonitor());
		assertTrue(subFolder.exists());
		assertTrue(subsubFolder.exists());

		assertTrue(undoManager.anythingToRedo());
		undoManager.performRedo(null, new NullProgressMonitor());
		assertFalse(subFolder.exists());
		assertFalse(subsubFolder.exists());
	}

	@Test
	public void testDeleteCu() throws Exception{
		ParticipantTesting.reset();
		final String contents= "package p; class X{}";
		ICompilationUnit newCU= getPackageP().createCompilationUnit("X.java", contents, true, new NullProgressMonitor());
		assertTrue("cu not created", newCU.exists());

		Object[] elements= {newCU};
		String[] handles= ParticipantTesting.createHandles(newCU, newCU.getTypes()[0], newCU.getResource());

		verifyEnabled(elements);
		mustPerformDummySearch();

		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, true);
		assertNull("expected to pass", status);
		assertFalse("cu not deleted", newCU.exists());
		ParticipantTesting.testDelete(handles);

		IWorkbenchOperationSupport operationSupport= PlatformUI.getWorkbench().getOperationSupport();
		IOperationHistory operationHistory= operationSupport.getOperationHistory();
		IUndoContext undoContext= operationSupport.getUndoContext();

		assertFalse(operationHistory.canRedo(undoContext));
		assertTrue(operationHistory.canUndo(undoContext));
		operationHistory.undo(undoContext, null, null);
		assertTrue(newCU.exists());
		assertEquals(contents, newCU.getSource());

		assertTrue(operationHistory.canRedo(undoContext));
		operationHistory.redo(undoContext, null, null);
		assertFalse(newCU.exists());
	}

	@Test
	public void testDeleteSourceFolder() throws Exception{
		ParticipantTesting.reset();
		IPackageFragmentRoot fredRoot= JavaProjectHelper.addSourceContainer(rts.getProject(), "fred");
		assertTrue("not created", fredRoot.exists());

		Object[] elements= {fredRoot};
		verifyEnabled(elements);
		mustPerformDummySearch();
		String[] handles= ParticipantTesting.createHandles(fredRoot, fredRoot.getResource());
		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, true);
		assertNull("expected to pass", status);
		assertFalse("not deleted", fredRoot.exists());
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteInternalJAR() throws Exception{
		ParticipantTesting.reset();
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB);
		assertTrue("lib does not exist",  lib.exists());
		IPackageFragmentRoot internalJAR= JavaProjectHelper.addLibraryWithImport(rts.getProject(), Path.fromOSString(lib.getPath()), null, null);

		Object[] elements= {internalJAR};
		verifyEnabled(elements);
		mustPerformDummySearch();
		String[] handles= ParticipantTesting.createHandles(internalJAR, internalJAR.getResource());

		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, true);
		assertNull("expected to pass", status);
		assertFalse("not deleted", internalJAR.exists());
		ParticipantTesting.testDelete(handles);
	}

	@Test
	public void testDeleteClassFile() throws Exception{
		//TODO implement me - how do i get a handle to a class file?
	}

	@Test
	public void testDeletePackage() throws Exception{
		// newPackage    <- delete
		// newPackage.A
		// newPackage.file
		// all three items must be deleted.
		// Notification of package delete and folder delete
		ParticipantTesting.reset();
		IPackageFragment newPackage= getRoot().createPackageFragment("newPackage", true, new NullProgressMonitor());
		assertTrue("package not created", newPackage.exists());
		String cuContents= "public class A {}";
		ICompilationUnit cu= newPackage.createCompilationUnit("A.java", cuContents, false, null);
		IFile file= ((IContainer)newPackage.getResource()).getFile(new Path("Z.txt"));
		file.create(getStream("123"), true, null);

		Object[] elements= {newPackage};
		verifyEnabled(elements);
		mustPerformDummySearch();
		String[] deleteHandles= ParticipantTesting.createHandles(newPackage, newPackage.getResource());

		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, true);
		assertNull("expected to pass", status);
		assertFalse("package not deleted", newPackage.exists());

		ParticipantTesting.testDelete(deleteHandles);

		IUndoManager undoManager= RefactoringCore.getUndoManager();
		undoManager.performUndo(null, new NullProgressMonitor());
		assertTrue(newPackage.exists());
		assertTrue(file.exists());
		assertEquals(cuContents, cu.getSource());
		undoManager.performRedo(null, new NullProgressMonitor());
		assertFalse(newPackage.exists());
	}

	@Test
	public void testDeletePackage2() throws Exception{
		// p1   <- delete
		// p1.A
		// p1.file
		// p1.p2
		// this tests cleaning of packages (p2 is not deleted)
		ParticipantTesting.reset();
		IPackageFragment p1= getRoot().createPackageFragment("p1", true, new NullProgressMonitor());
		IPackageFragment p1p2= getRoot().createPackageFragment("p1.p2", true, new NullProgressMonitor());
		assertTrue("package not created", p1.exists());
		String cuContents= "public class A {}";
		ICompilationUnit cu= p1.createCompilationUnit("A.java", cuContents, false, null);
		IFile file= ((IContainer)p1.getResource()).getFile(new Path("Z.txt"));
		file.create(getStream("123"), true, null);

		Object[] elements= {p1};
		verifyEnabled(elements);
		mustPerformDummySearch();
		String[] deleteHandles= ParticipantTesting.createHandles(p1, cu.getResource(), file);

		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, true);
		assertNull("expected to pass", status);
		//Package is not delete since it had sub packages
		assertTrue("package deleted", p1.exists());
		assertEquals(0, p1.getChildren().length);
		assertTrue(p1p2.exists());
		assertFalse(file.exists());
		assertFalse(cu.exists());

		ParticipantTesting.testDelete(deleteHandles);

		IUndoManager undoManager= RefactoringCore.getUndoManager();
		undoManager.performUndo(null, new NullProgressMonitor());
		assertTrue(p1.exists());
		assertTrue(p1p2.exists());
		assertTrue(file.exists());
		assertEquals(cuContents, cu.getSource());
		undoManager.performRedo(null, new NullProgressMonitor());
		assertTrue(p1p2.exists());
		assertFalse(file.exists());
		assertFalse(cu.exists());
	}

	@Test
	public void testDeletePackage3() throws Exception {
		// a0.a1.a2.a3 <- delete
		// a0.a1.a2.a3.A
		// all packages must be removed; folder a0 must be removed.
		IPackageFragment[] frags= createPackagePath(4);
		ICompilationUnit a= frags[3].createCompilationUnit("A.java", "public class A {}", false, null);
		executeDeletePackage(new Object[] { frags[3] }, frags, new Object[] {  frags[0].getResource() } );
		Object[] deleted= new Object[]{frags[0], frags[1], frags[2], frags[3], a};
		doTestUndoRedo(deleted, null);
	}

	@Test
	public void testDeletePackage4() throws Exception {
		// a0.a1.a2.a3 <- delete
		// a0.a1.a2.a3.A <- delete
		// all packages must be removed; folder a0 must be removed.
		IPackageFragment[] frags= createPackagePath(4);
		ICompilationUnit a= frags[3].createCompilationUnit("A.java", "public class A {}", false, null);
		executeDeletePackage(new Object[] { frags[3], a }, frags, new Object[] {  frags[0].getResource() } );
		Object[] deleted= new Object[]{frags[0], frags[1], frags[2], frags[3], a};
		doTestUndoRedo(deleted, null);
	}

	@Test
	public void testDeletePackage5() throws Exception {
		// a0.a1.a2.A <- not deleted
		// a0.a1.a2.a3.a4.a5 <- delete
		// only a3, a4, a5 are to be deleted; folder a3 must be removed.
		IPackageFragment[] frags= createPackagePath(6);
		ICompilationUnit a= frags[2].createCompilationUnit("A.java", "public class A {}", false, null);
		executeDeletePackage(new Object[] { frags[5] }, new IPackageFragment[] { frags[5], frags[4], frags[3] }, new Object[] { frags[3].getResource() });
		Object[] deleted= new Object[]{frags[5], frags[4], frags[3]};
		Object[] exist= new Object[]{frags[2], frags[1], frags[0], a};
		doTestUndoRedo(deleted, exist);
	}

	@Test
	public void testDeletePackage6() throws Exception {
		// a0.a1.a2.anotherPackage
		// a0.a1.a2.a3.a4.a5 <- delete
		// only a3, a4, a5 are to be deleted; folder a3 must be removed
		IPackageFragment[] frags= createPackagePath(6);
		IPackageFragment another= getRoot().createPackageFragment("a0.a1.a2.anotherPackage", true, null);
		executeDeletePackage(new Object[] { frags[5] }, new IPackageFragment[] { frags[5], frags[4], frags[3] }, new Object[] { frags[3].getResource() });
		Object[] deleted= new Object[]{frags[5], frags[4], frags[3]};
		Object[] exist= new Object[]{frags[2], frags[1], frags[0], another};
		doTestUndoRedo(deleted, exist);
	}

	@Test
	public void testDeletePackage7() throws Exception {
		// a0.a1.a2.A <- delete
		// a0.a1.a2.a3.a4.a5 <- delete
		// all packages must be deleted; folder a0 must be removed
		IPackageFragment[] frags= createPackagePath(6);
		ICompilationUnit a= frags[2].createCompilationUnit("A.java", "public class A {}", false, null);
		executeDeletePackage(new Object[] { frags[5], a }, frags, new Object[] { frags[0].getResource() });
		Object[] deleted= new Object[]{frags[5], frags[4], frags[3], frags[2], frags[1], frags[0], a};
		Object[] exist= null;
		doTestUndoRedo(deleted, exist);
	}

	@Test
	public void testDeletePackage8() throws Exception {
		// a0.a1.a2.A <- delete
		// a0.a1.a2.a3.Z <- don't delete
		// a0.a1.a2.a3.a4.a5 <- delete
		// only someFile, a4, and a5 are to be deleted; notification about a4, a5, A
		IPackageFragment[] frags= createPackagePath(6);
		ICompilationUnit a= frags[2].createCompilationUnit("A.java", "public class A {}", false, null);
		IFile file= ((IContainer)frags[3].getResource()).getFile(new Path("Z.txt"));
		file.create(getStream("123"), true, null);
		executeDeletePackage(new Object[] { frags[5], a }, new IPackageFragment[] { frags[5], frags[4] }, new Object[] { frags[4].getResource(), a.getResource(), a, a.getType("A") });
		Object[] deleted= new Object[]{frags[5], frags[4], a};
		Object[] exist= new Object[]{frags[3], frags[2], frags[1], frags[0], file};
		doTestUndoRedo(deleted, exist);
	}

	@Test
	public void testDeletePackage9() throws Exception {
		// a0.a1.a2.A <- delete
		// a0.a1.a2.a3.Z <- delete
		// a0.a1.a2.a3.a4.a5 <- delete
		// all packages must be removed; folder a0 must be removed
		IPackageFragment[] frags= createPackagePath(6);
		ICompilationUnit a= frags[2].createCompilationUnit("A.java", "public class A {}", false, null);
		IFile file= ((IContainer)frags[3].getResource()).getFile(new Path("Z.txt"));
		file.create(getStream("123"), true, null);
		executeDeletePackage(new Object[] { frags[5], a, file }, frags, new Object[] {  frags[0].getResource() });
		Object[] deleted= new Object[]{frags[5], frags[4], frags[3], frags[2], frags[1], frags[0], a, file};
		Object[] exist= null;
		doTestUndoRedo(deleted, exist);
	}

	@Test
	public void testDeletePackage10() throws Exception {
		// a0.a1.a2 <- delete
		// a0.a1.a2.A <- delete
		// a0.a1.a2.a3 <- do not delete
		// only A must be removed
		// This tests "cleaning" of packages -> folder a0.a1.a2 does NOT get removed.
		IPackageFragment[] frags= createPackagePath(4);
		ICompilationUnit a= frags[2].createCompilationUnit("A.java", "public class A {}", false, null);

		ParticipantTesting.reset();
		final Object[] markedForDelete= new Object[] { frags[2], a.getResource() };
		String[] deleteHandles= ParticipantTesting.createHandles(markedForDelete);

		verifyEnabled(markedForDelete);
		mustPerformDummySearch();
		DeleteRefactoring ref= createRefactoring(markedForDelete);
		RefactoringStatus status= performRefactoring(ref, true);
		assertNull("expected to pass", status);

		// test handles (!! only the package, not the resource)
		ParticipantTesting.testDelete(deleteHandles);
		// Package is not deleted since it had sub packages
		Object[] deleted= new Object[]{a};
		Object[] exist= new Object[]{frags[3], frags[2], frags[1], frags[0]};
		doTestUndoRedo(deleted, exist);
	}

	@Test
	public void testDeletePackage12() throws Exception {
		// a0		<- delete
		// a0.a1	<- delete
		// a0.a1.a2
		// a0 and a1 are to be cleaned, do not report any folder deletions
		IPackageFragment[] frags= createPackagePath(3);
		executeDeletePackage(new Object[] { frags[0], frags[1] }, new IPackageFragment[] { frags[0], frags[1] }, new Object[] { });
		Object[] deleted= null;
		Object[] exist= new Object[]{frags[2], frags[1], frags[0]};
		doTestUndoRedo(deleted, exist);
	}

	@Test
	public void testDeletePackageAndFolder() throws Exception {
		// folder    <- delete
		// and
		// newPackage    <- delete
		// newPackage.A
		// newPackage.file
		// Both the package and the folder must be deleted.
		ParticipantTesting.reset();
		IProject project= rts.getProject().getProject();
		IFolder folder= project.getFolder("folder");
		folder.create(true, true, null);
		IPackageFragment newPackage= getRoot().createPackageFragment("newPackage", true, new NullProgressMonitor());
		assertTrue("folder does not exist", folder.exists());
		assertTrue("package not created", newPackage.exists());
		String cuContents= "public class A {}";
		ICompilationUnit cu= newPackage.createCompilationUnit("A.java", cuContents, false, null);
		IFile file= ((IContainer)newPackage.getResource()).getFile(new Path("Z.txt"));
		file.create(getStream("123"), true, null);

		Object[] elements= { folder, newPackage };
		verifyEnabled(elements);
		mustPerformDummySearch();
		String[] deleteHandles= ParticipantTesting.createHandles(folder, newPackage, newPackage.getResource());

		DeleteRefactoring ref= createRefactoring(elements);
		RefactoringStatus status= performRefactoring(ref, true);
		assertNull("expected to pass", status);
		assertFalse("folder not deleted", folder.exists());
		assertFalse("package not deleted", newPackage.exists());

		ParticipantTesting.testDelete(deleteHandles);

		IUndoManager undoManager= RefactoringCore.getUndoManager();
		undoManager.performUndo(null, new NullProgressMonitor());
		assertTrue(folder.exists());
		assertTrue(newPackage.exists());
		assertTrue(file.exists());
		assertEquals(cuContents, cu.getSource());
		undoManager.performRedo(null, new NullProgressMonitor());
		assertFalse(folder.exists());
		assertFalse(newPackage.exists());
	}

	/* Don't rename! See @FixMethodOrder(MethodSorters.NAME_ASCENDING) */
	@Test
	public void test_END_DeletePackageSub1() throws Exception {
		// a0.a1.a2 <-delete with subs
		// a0.a1.a2.a3
		// a0.a1.a2.a3.file
		// a0.a1.a2.a3.A
		// a0.a1.a2.a3.a4
		// expected: everything deleted
		IPackageFragment[] frags= createPackagePath(5);
		ICompilationUnit a= frags[3].createCompilationUnit("A.java", "public class A {}", false, null);
		IFile file= ((IContainer)frags[3].getResource()).getFile(new Path("Z.txt"));
		file.create(getStream("123"), true, null);
		executeDeletePackage(new Object[] { frags[2] }, frags, new Object[] {  frags[0].getResource() }, true);
		Object[] deleted= new Object[]{frags[3], frags[2], frags[1], frags[0], a, file};
		Object[] exist= null;
		doTestUndoRedo(deleted, exist);
	}

	/* Don't rename! See @FixMethodOrder(MethodSorters.NAME_ASCENDING) */
	@Test
	public void test_END_DeletePackageSub4() throws Exception {
		// (default)
		// a0 <- delete
		// a0.a1
		// expected: everything deleted; notification about deletion of:
		// PackageFragments: a0, a0.a1
		// Folders: a0 (NOT the folder of the default package)
		IPackageFragment[] frags= createPackagePath(2);
		IPackageFragment p= getRoot().getPackageFragment("p");
		if (p.exists()) p.delete(true, null);
		executeDeletePackage(new Object[] { frags[0] }, frags, new Object[] { frags[0].getResource() } , true);
		Object[] deleted= new Object[]{frags[1], frags[0]};
		Object[] exist= null;
		doTestUndoRedo(deleted, exist);
	}

}
