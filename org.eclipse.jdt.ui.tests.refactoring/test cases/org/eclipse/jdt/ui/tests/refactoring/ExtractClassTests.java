/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractClassDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.ExtractClassDescriptor.Field;

import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;

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

	private void createAdditionalFile(String subDir, String fileName) throws Exception {
		IPackageFragment pack= getSubPackage(subDir);
		ICompilationUnit cu= createCUfromTestFile(pack, fileName, true);
		assertNotNull(cu);
		assertTrue(cu.exists());
	}

	private IPackageFragment getSubPackage(String subDir) throws Exception {
		IPackageFragment pack= getPackageP();
		if (subDir != null) {
			String packageName= pack.getElementName() + "." + subDir;
			pack= getRoot().getPackageFragment(packageName);
			if (!pack.exists()) {
				IPackageFragment create= getRoot().createPackageFragment(packageName, true, new NullProgressMonitor());
				assertNotNull(create);
				assertTrue(create.exists());
				return create;
			}
		}
		return pack;
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

	private RefactoringStatus runRefactoring(boolean expectError) throws Exception {
		RefactoringStatus status= performRefactoring(fDescriptor);
		if (expectError) {
			assertNotNull(status + "", status);
			return status;
		} else {
			if (status!=null)
				assertTrue(status+"",status.getEntryWithHighestSeverity().getSeverity() <= RefactoringStatus.WARNING);
		}
		String expected= getFileContents(getOutputTestFileName(getCUName()));
		assertNotNull(expected);
		ICompilationUnit resultCU= fPack.getCompilationUnit(getCUFileName());
		assertNotNull(resultCU);
		assertTrue(resultCU.exists());
		String result= resultCU.getSource();
		assertNotNull(result);
		assertEqualLines(expected, result);
		if (fDescriptor.isCreateTopLevel() && !expectError) {
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
		return status;
	}

	protected void setUp() throws Exception {
		super.setUp();
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		fDescriptor= RefactoringSignatureDescriptorFactory.createExtractClassDescriptor();
		fDescriptor.setFieldName("parameterObject");
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
		fDescriptor.setClassName("ComplexExtractParameter");
		runRefactoring(false);
	}

	public void testInitializerProblem() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setClassName("InitializerProblemParameter");
		runRefactoring(false);
	}

	public void testMethodUpdate() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setClassName("MethodUpdateParameter");
		runRefactoring(false);
	}

	public void testInheritanceUpdate() throws Exception {
		createAdditionalFile("InheritanceUpdateImpl");
		fDescriptor.setType(setupType());
		fDescriptor.setClassName("InheritanceUpdateParameter");
		runRefactoring(false);
		checkAdditionalFile("InheritanceUpdateImpl");
	}

	public void testInheritanceUpdateGetterSetter() throws Exception {
		createAdditionalFile("InheritanceUpdateImplGetterSetter");
		fDescriptor.setType(setupType());
		fDescriptor.setCreateGetterSetter(true);
		fDescriptor.setClassName("InheritanceUpdateGetterSetterParameter");
		runRefactoring(false);
		checkAdditionalFile("InheritanceUpdateImplGetterSetter");
	}

	public void testComplexExtractGetterSetter() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setCreateGetterSetter(true);
		fDescriptor.setClassName("ComplexExtractGetterSetterParameter");
		runRefactoring(false);
	}

	public void testComplexExtractNested() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setCreateTopLevel(false);
		fDescriptor.setClassName("ComplexExtractNestedParameter");
		runRefactoring(false);
	}

	public void testStaticInstanceFields() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setClassName("StaticInstanceFieldsParameter");
		RefactoringStatus status= runRefactoring(true);
		RefactoringStatusEntry[] entries= status.getEntries();
		//Warning for no IFields moved
		assertEquals(1, entries.length);
		for (int i= 0; i < entries.length; i++) {
			RefactoringStatusEntry refactoringStatusEntry= entries[i];
			assertEquals("Status was:" + refactoringStatusEntry, true, refactoringStatusEntry.isFatalError());
		}
	}

	public void testImportRemove() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setClassName("ImportRemoveParameter");
		runRefactoring(false);
	}

	public void testSwitchCase() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setClassName("SwitchCaseParameter");
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
		fDescriptor.setClassName("CopyModifierAnnotationsParameter");
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

	public void testControlBodyUpdates() throws Exception {
		IJavaProject javaProject= getRoot().getJavaProject();
		Map originalOptions= javaProject.getOptions(true);
		try {
			HashMap newOptions= new HashMap(originalOptions);
			newOptions.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
			javaProject.setOptions(newOptions);
			fDescriptor.setType(setupType());
			fDescriptor.setCreateGetterSetter(true);
			fDescriptor.setClassName("ControlBodyUpdatesParameter");
			runRefactoring(false);
		} finally {
			javaProject.setOptions(originalOptions);
		}
	}

	public void testArrayInitializer() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setCreateGetterSetter(true);
		fDescriptor.setClassName("ArrayInitializerParameter");
		runRefactoring(false);
	}

	public void testVariableDeclarationInitializer() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setCreateGetterSetter(true);
		fDescriptor.setClassName("VariableDeclarationInitializerParameter");
		runRefactoring(false);
	}

	public void testUpdateSimpleName() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setCreateGetterSetter(true);
		fDescriptor.setClassName("UpdateSimpleNameParameter");
		runRefactoring(false);
	}

	public void testArrayLengthAccess() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setCreateGetterSetter(true);
		fDescriptor.setClassName("ArrayLengthAccessParameter");
		runRefactoring(false);
	}

	public void testInnerDocumentedClass() throws Exception {
		IType outer= setupType();
		IType inner= outer.getType("InnerClass");
		assertTrue(inner.exists());
		fDescriptor.setType(inner);
		fDescriptor.setCreateGetterSetter(true);
		fDescriptor.setClassName("InnerClassParameter");
		runRefactoring(false);
	}

	public void testPackageReferences() throws Exception {
		createAdditionalFile("subPack","PackEx");
		fDescriptor.setType(setupType());
		fDescriptor.setCreateGetterSetter(true);
		fDescriptor.setClassName("PackageReferencesParameter");
		RefactoringStatus status= runRefactoring(true);
		RefactoringStatusEntry[] entries= status.getEntries();
		//Error for privateInner reference
		//Error for OtherPackageProteced reference
		assertEquals(2, entries.length);
		for (int i= 0; i < entries.length; i++) {
			RefactoringStatusEntry refactoringStatusEntry= entries[i];
			assertEquals(true, refactoringStatusEntry.isError());
		}
	}

	public void testDuplicateParamName() throws Exception {
		IJavaProject javaProject= getRoot().getJavaProject();
		Map originalOptions= javaProject.getOptions(true);
		try {
			HashMap newOptions= new HashMap(originalOptions);
			newOptions.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
			javaProject.setOptions(newOptions);
			fDescriptor.setType(setupType());
			fDescriptor.setCreateGetterSetter(true);
			fDescriptor.setClassName("DuplicateParamNameParameter");
			runRefactoring(false);
		} finally {
			javaProject.setOptions(originalOptions);
		}
	}

	public void testLowestVisibility() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setCreateGetterSetter(true);
		fDescriptor.setClassName("LowestVisibilityParameter");
		runRefactoring(false);
	}

	public void testSwitchCaseUpdates() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setCreateGetterSetter(true);
		fDescriptor.setClassName("SwitchCaseUpdatesParameter");
		runRefactoring(false);
	}

	public void testFieldsWithJavadoc() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setFieldName("data");
		fDescriptor.setCreateGetterSetter(true);
		runRefactoring(false);
	}

	public void testQualifiedIncrements() throws Exception {
		fDescriptor.setType(setupType());
		fDescriptor.setCreateGetterSetter(true);
		fDescriptor.setClassName("QualifiedIncrementsParameter");
		RefactoringStatus status= runRefactoring(false);
		RefactoringStatusEntry[] entries= status.getEntries();
		//3*Warning for semantic change
		assertEquals(3, entries.length);
		for (int i= 0; i < entries.length; i++) {
			RefactoringStatusEntry refactoringStatusEntry= entries[i];
			assertEquals(true, refactoringStatusEntry.isWarning());
		}
	}

}
