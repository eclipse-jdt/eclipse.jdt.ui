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
package org.eclipse.jdt.ui.tests.refactoring;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractClassDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractClassDescriptor.Field;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ExtractClassTests extends RefactoringTest {

	private static final String REFACTORING_PATH= "ExtractClass/";
	private IPackageFragment fPack;
	private ExtractClassDescriptor fDescriptor;

	public ExtractClassTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(ExtractClassTests.class));
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringTestSetup(someTest);
	}

	private IType setupType() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), getCUName(), true);
		IType type= cu.getType(getCUName());
		assertNotNull(type);
		assertTrue(type.exists());
		return type;
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	private void checkAdditionalFile(String fileName) throws Exception, JavaModelException, IOException {
		ICompilationUnit cu= fPack.getCompilationUnit(fileName + ".java");
		assertNotNull(cu);
		assertTrue(cu.getPath() + " does not exist", cu.exists());
		String actual= cu.getSource();
		String expected= getFileContents(getOutputTestFileName(fileName));
		assertEqualLines(expected, actual);
	}

	private void createAdditionalFile(String fileName) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(fPack, fileName, true);
		assertNotNull(cu);
		assertTrue(cu.exists());
	}

	private String getCUName() {
		StringBuffer sb= new StringBuffer();
		String name= getName();
		if (name.startsWith("test"))
			name= name.substring(4);
		sb.append(Character.toUpperCase(name.charAt(0)) + name.substring(1));
		return sb.toString();
	}

	private String getCUFileName() {
		StringBuffer sb= new StringBuffer();
		sb.append(getCUName());
		sb.append(".java");
		return sb.toString();
	}

	private RefactoringStatus runRefactoring(boolean expectFailure) throws Exception {
		RefactoringStatus status= performRefactoring(fDescriptor);
		if (expectFailure) {
			assertNotNull(status + "", status);
			return status;
		} else {
			assertNull(status + "", status);
		}
		String expected= getFileContents(getOutputTestFileName(getCUName()));
		assertNotNull(expected);
		ICompilationUnit resultCU= fPack.getCompilationUnit(getCUFileName());
		assertNotNull(resultCU);
		assertTrue(resultCU.exists());
		String result= resultCU.getSource();
		assertNotNull(result);
		assertEqualLines(expected, result);
		if (fDescriptor.isCreateTopLevel() && !expectFailure) {
			String packageName= fDescriptor.getPackage();
			if (packageName != null)
				fPack= getRoot().getPackageFragment(packageName);
			assertNotNull(fPack);
			String parameterClassFile= fDescriptor.getClassName() + ".java";
			ICompilationUnit unit= fPack.getCompilationUnit(parameterClassFile);
			assertNotNull(unit);
			assertTrue(unit.exists());
			expected= getFileContents(getOutputTestFileName(fDescriptor.getClassName()));
			result= unit.getSource();
			assertNotNull(result);
			assertEqualLines(expected, result);
		}
		return null;
	}

	protected void setUp() throws Exception {
		super.setUp();
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		fDescriptor= new ExtractClassDescriptor();
		fPack= getPackageP();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		fDescriptor= null;
		fPack= null;
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setToDefault(PreferenceConstants.CODEGEN_ADD_COMMENTS);
	}

	public void testComplexExtract() throws Exception {
		fDescriptor.setType(setupType());
		runRefactoring(false);
	}

	public void testInitializerProblem() throws Exception {
		fDescriptor.setType(setupType());
		runRefactoring(false);
	}

	public void testMethodUpdate() throws Exception {
		fDescriptor.setType(setupType());
		runRefactoring(false);
	}

	public void testInheritanceUpdate() throws Exception {
		createAdditionalFile("InheritanceUpdateImpl");
		fDescriptor.setType(setupType());
		runRefactoring(false);
		checkAdditionalFile("InheritanceUpdateImpl");
	}

	public void testInheritanceUpdateGetterSetter() throws Exception {
		createAdditionalFile("InheritanceUpdateImplGetterSetter");
		fDescriptor.setType(setupType());
		fDescriptor.setCreateGetterSetter(true);
		runRefactoring(false);
		checkAdditionalFile("InheritanceUpdateImplGetterSetter");
	}
	
	public void testComplexExtractGetterSetter() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setCreateGetterSetter(true);
		runRefactoring(false);
	}

	public void testComplexExtractNested() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setCreateTopLevel(false);
		runRefactoring(false);
	}

	public void testStaticInstanceFields() throws Exception {
		fDescriptor.setType(setupType());
		RefactoringStatus status= runRefactoring(true);
		RefactoringStatusEntry[] entries= status.getEntries();
		//Warning for no IFields moved
		assertEquals(1, entries.length);
		for (int i= 0; i < entries.length; i++) {
			RefactoringStatusEntry refactoringStatusEntry= entries[i];
			assertEquals("Status was:"+refactoringStatusEntry, true, refactoringStatusEntry.isFatalError());
		}
	}
	
	public void testImportRemove() throws Exception {
		fDescriptor.setType(setupType());
		runRefactoring(false);
	}
	
	public void testSwitchCase() throws Exception {
		fDescriptor.setType(setupType());
		RefactoringStatus status= runRefactoring(true);
		RefactoringStatusEntry[] entries= status.getEntries();
		//Error for usage in Switch case
		assertEquals(1, entries.length);
		for (int i= 0; i < entries.length; i++) {
			RefactoringStatusEntry refactoringStatusEntry= entries[i];
			assertEquals(true, refactoringStatusEntry.isError());
		}
	}
	
	public void testCopyModifierAnnotations() throws Exception {
		fDescriptor.setType(setupType());
		RefactoringStatus status= runRefactoring(true);
		RefactoringStatusEntry[] entries= status.getEntries();
		//Warning for transient
		//Warning for volatile
		assertEquals(2, entries.length);
		for (int i= 0; i < entries.length; i++) {
			RefactoringStatusEntry refactoringStatusEntry= entries[i];
			assertEquals(true, refactoringStatusEntry.isWarning());
		}
	}
	
	public void testUFOGetter() throws Exception {
		fDescriptor.setType(setupType());
		Field[] fields= ExtractClassDescriptor.getFields(fDescriptor.getType());
		for (int i= 0; i < fields.length; i++) {
			Field field= fields[i];
			if ("homePlanet".equals(field.getFieldName()))
				field.setCreateField(false);
		}
		fDescriptor.setFields(fields);
		fDescriptor.setClassName("Position");
		fDescriptor.setFieldName("position");
		fDescriptor.setCreateGetterSetter(true);
		runRefactoring(false);
	}

}
