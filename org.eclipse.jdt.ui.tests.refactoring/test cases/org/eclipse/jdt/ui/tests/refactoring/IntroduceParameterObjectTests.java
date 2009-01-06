/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
import java.util.Hashtable;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.descriptors.IntroduceParameterObjectDescriptor;
import org.eclipse.jdt.core.refactoring.descriptors.IntroduceParameterObjectDescriptor.Parameter;

import org.eclipse.jdt.internal.core.refactoring.descriptors.RefactoringSignatureDescriptorFactory;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class IntroduceParameterObjectTests extends RefactoringTest {

	private static final Class CLAZZ= IntroduceParameterObjectTests.class;
	private static final String DEFAULT_SUB_DIR= "sub";
	private static final String REFACTORING_PATH= "IntroduceParameterObject/";

	public static Test setUpTest(Test someTest) {
		return new RefactoringTestSetup(someTest);
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(CLAZZ));
	}

	private IPackageFragment fPack;
	private IntroduceParameterObjectDescriptor fDescriptor;

	public IntroduceParameterObjectTests(String name) {
		super(name);
	}

	private void checkAdditionalFile(String subdir, String fileName) throws Exception, JavaModelException, IOException {
		IPackageFragment pack= getSubPackage(subdir);
		ICompilationUnit cu= pack.getCompilationUnit(fileName+".java");
		assertNotNull(cu);
		assertTrue(cu.getPath() + " does not exist", cu.exists());
		String actual= cu.getSource();
		String expected= getFileContents(getOutputTestFileName(fileName));
		assertEqualLines(expected, actual);
	}

	private void checkCaller(String subdir) throws Exception {
		checkAdditionalFile(subdir, getCUName(true));
	}

	private void createAdditionalFile(String subDir, String fileName) throws Exception {
		IPackageFragment pack= getSubPackage(subDir);
		ICompilationUnit cu= createCUfromTestFile(pack, fileName, true);
		assertNotNull(cu);
		assertTrue(cu.exists());
	}

	private void createCaller(String subDir) throws Exception {
		createAdditionalFile(subDir, getCUName(true));
	}

	private String getCUFileName(boolean caller) {
		StringBuffer sb= new StringBuffer();
		sb.append(getCUName(caller));
		sb.append(".java");
		return sb.toString();
	}

	private String getCUName(boolean caller) {
		StringBuffer sb= new StringBuffer();
		sb.append(Character.toUpperCase(getName().charAt(0)) + getName().substring(1));
		if (caller)
			sb.append("Caller");
		return sb.toString();
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
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

	private void runRefactoring(boolean expectError, boolean useSuggestedMethod) throws Exception {
		RefactoringStatus status= performRefactoring(fDescriptor);
		if (expectError) {
			assertNotNull(status);
			if (useSuggestedMethod){
				final RefactoringStatusEntry entry= status.getEntryMatchingSeverity(RefactoringStatus.FATAL);
				if (entry.getCode() == RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD || entry.getCode() == RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE) {
					final Object element= entry.getData();
					fDescriptor.setMethod((IMethod) element);
					status= performRefactoring(fDescriptor);
				}
			} else {
				return;
			}
		}
		assertNull(status+"",status);
		String expected= getFileContents(getOutputTestFileName(getCUName(false)));
		assertNotNull(expected);
		ICompilationUnit resultCU= fPack.getCompilationUnit(getCUFileName(false));
		assertNotNull(resultCU);
		assertTrue(resultCU.exists());
		String result= resultCU.getSource();
		assertNotNull(result);
		assertEqualLines(expected, result);
		if (fDescriptor.isTopLevel()){
			String packageName= fDescriptor.getPackageName();
			if (packageName!=null)
				fPack=getRoot().getPackageFragment(packageName);
			assertNotNull(fPack);
			String parameterClassFile= fDescriptor.getClassName()+".java";
			ICompilationUnit unit= fPack.getCompilationUnit(parameterClassFile);
			assertNotNull(unit);
			assertTrue(unit.exists());
			expected=getFileContents(getOutputTestFileName(fDescriptor.getClassName()));
			result=unit.getSource();
			assertNotNull(result);
			assertEqualLines(expected, result);
		}

		assertParticipant(fDescriptor.getMethod().getDeclaringType());
	}

	private void assertParticipant(IType typeOfMethod) throws JavaModelException {
		TestChangeMethodSignaturParticipant.testParticipant(typeOfMethod);
	}

	protected void setUp() throws Exception {
		super.setUp();
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		fDescriptor= RefactoringSignatureDescriptorFactory.createIntroduceParameterObjectDescriptor();
		fPack= getPackageP();
	}

	private IMethod setupMethod() throws Exception, JavaModelException {
		ICompilationUnit cu= createCUfromTestFile(fPack, getCUName(false), true);
		IType type= cu.getType(getCUName(false));
		assertNotNull(type);
		assertTrue(type.exists());
		IMethod fooMethod= null;
		IMethod[] methods= type.getMethods();
		for (int i= 0; i < methods.length; i++) {
			IMethod method= methods[i];
			if ("foo".equals(method.getElementName())) {
				fooMethod= method;
			}
		}
		assertNotNull(fooMethod);
		assertTrue(fooMethod.exists());
		return fooMethod;
	}

	private void setupPackage(String inputPackage) throws JavaModelException {
		fPack= getRoot().createPackageFragment(inputPackage,true,null);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		fDescriptor=null;
		fPack=null;
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setToDefault(PreferenceConstants.CODEGEN_ADD_COMMENTS);
	}

	public void testBodyUpdate() throws Exception {
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setTopLevel(false);
		runRefactoring(false, true);
	}

	public void testDefaultPackagePoint() throws Exception {
		setupPackage("");
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setClassName("ArrayList");
		fDescriptor.setTopLevel(false);
		runRefactoring(false, true);
	}

	public void testDefaultPackagePointTopLevel() throws Exception {
		setupPackage("");
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setClassName("ArrayList");
		fDescriptor.setTopLevel(true);
		runRefactoring(false, true);
	}

	public void testDelegateCreation() throws Exception {
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setGetters(true);
		fDescriptor.setTopLevel(false);
		fDescriptor.setSetters(true);
		fDescriptor.setDelegate(true);

		Parameter[] parameters= IntroduceParameterObjectDescriptor.createParameters(fDescriptor.getMethod());
		parameters[1].setFieldName("newA");
		parameters[2].setFieldName("newB");
		parameters[3].setFieldName("newD");
		fDescriptor.setParameters(parameters);
		runRefactoring(false, true);
	}

	public void testDelegateCreationCodeStyle() throws Exception {
		IJavaProject javaProject= getRoot().getJavaProject();
		Map originalOptions= javaProject.getOptions(false);
		try {
			Hashtable newOptions= new Hashtable();
			newOptions.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
			newOptions.put(JavaCore.CODEASSIST_FIELD_SUFFIXES, "G");
			javaProject.setOptions(newOptions);

			fDescriptor.setMethod(setupMethod());
			fDescriptor.setGetters(true);
			fDescriptor.setSetters(true);
			fDescriptor.setDelegate(true);
			fDescriptor.setTopLevel(false);

			Parameter[] parameters= IntroduceParameterObjectDescriptor.createParameters(fDescriptor.getMethod());
			parameters[3].setFieldName("newD");
			fDescriptor.setParameters(parameters);
			runRefactoring(false, true);
		} finally {
			javaProject.setOptions(originalOptions);
		}
	}

	public void testImportAddEnclosing() throws Exception {
		createCaller(null);
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setTopLevel(false);

		Parameter[] parameters= IntroduceParameterObjectDescriptor.createParameters(fDescriptor.getMethod());
		parameters[1].setFieldName("permissions");
		fDescriptor.setParameters(parameters);
		runRefactoring(false, true);
		checkCaller(null);
	}

	public void testImportAddTopLevel() throws Exception {
		createCaller(DEFAULT_SUB_DIR);
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setTopLevel(true);
		fDescriptor.setClassName("TestImportAddTopLevelParameter");
		fDescriptor.setPackageName("p.parameters");
		runRefactoring(false, true);
		checkCaller(DEFAULT_SUB_DIR);
	}

	public void testImportNameSimple() throws Exception {
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setClassName("ArrayList");
		fDescriptor.setParameterName("p");
		fDescriptor.setTopLevel(true);
		runRefactoring(false, true);
	}

	public void testInlineRename() throws Exception {
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setTopLevel(false);

		Parameter[] parameters= IntroduceParameterObjectDescriptor.createParameters(fDescriptor.getMethod());
		parameters[3].setCreateField(false);
		fDescriptor.setParameters(parameters);
		runRefactoring(false, true);
	}
	
	public void testSubclassInCU() throws Exception {
		// test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=259095
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setTopLevel(true);
		fDescriptor.setClassName("FooParameter");
		
		runRefactoring(false, true);
	}

	public void testInterfaceMethod() throws Exception {
		createAdditionalFile(null, "TestInterfaceMethod2Impl");
		createAdditionalFile(null, "ITestInterfaceMethod");
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setTopLevel(true);
		fDescriptor.setClassName("FooParameter");
		runRefactoring(true, false);
		runRefactoring(true, true);
		checkAdditionalFile(null, "ITestInterfaceMethod");
		checkAdditionalFile(null, "TestInterfaceMethod2Impl");
	}

	public void testRecursiveReordered() throws Exception {
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setTopLevel(false);

		Parameter[] parameters= IntroduceParameterObjectDescriptor.createParameters(fDescriptor.getMethod());
		Parameter temp=parameters[1];
		parameters[1]=parameters[2];
		parameters[2]=temp;
		fDescriptor.setParameters(parameters);
		runRefactoring(false, true);
	}

	public void testRecursiveSimple() throws Exception {
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setTopLevel(false);
		runRefactoring(false, true);
	}

	public void testRecursiveSimpleReordered() throws Exception {
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setTopLevel(false);
		runRefactoring(false, true);
	}


	public void testReorderGetter() throws Exception{
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setGetters(true);
		fDescriptor.setTopLevel(false);
		Parameter[] parameters= IntroduceParameterObjectDescriptor.createParameters(fDescriptor.getMethod());
		Parameter temp=parameters[3];
		parameters[3]=parameters[2];
		parameters[2]=parameters[1];
		parameters[1]=temp;
		fDescriptor.setParameters(parameters);
		runRefactoring(false, true);
	}

	public void testSimpleEnclosing() throws Exception{
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setTopLevel(false);
		runRefactoring(false, true);
	}

	public void testSimpleEnclosingCodeStyle() throws Exception {
		IJavaProject javaProject= getRoot().getJavaProject();
		Map originalOptions= javaProject.getOptions(false);
		try {
			Hashtable newOptions= new Hashtable();
			newOptions.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
			newOptions.put(JavaCore.CODEASSIST_FIELD_SUFFIXES, "G");
			newOptions.put(JavaCore.CODEASSIST_ARGUMENT_PREFIXES, "a");
			newOptions.put(JavaCore.CODEASSIST_ARGUMENT_SUFFIXES, "M");
			javaProject.setOptions(newOptions);

			fDescriptor.setMethod(setupMethod());
			fDescriptor.setTopLevel(false);
			runRefactoring(false, true);
		} finally {
			javaProject.setOptions(originalOptions);
		}
	}

	public void testVarArgsNotReordered() throws Exception{
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setTopLevel(false);
		runRefactoring(false, true);
	}

	public void testVarArgsReordered() throws Exception{
		fDescriptor.setMethod(setupMethod());
		fDescriptor.setTopLevel(false);

		Parameter[] parameters= IntroduceParameterObjectDescriptor.createParameters(fDescriptor.getMethod());
		Parameter temp=parameters[1];
		parameters[1]=parameters[2];
		parameters[2]=temp;
		fDescriptor.setParameters(parameters);
		runRefactoring(false, true);
	}
}
