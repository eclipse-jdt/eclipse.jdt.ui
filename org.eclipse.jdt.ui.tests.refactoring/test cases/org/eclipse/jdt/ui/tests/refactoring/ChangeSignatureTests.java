/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.refactoring.ExceptionInfo;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class ChangeSignatureTests extends RefactoringTest {
	private static final Class clazz= ChangeSignatureTests.class;
	private static final String REFACTORING_PATH= "ChangeSignature/";
	
	private static final boolean RUN_CONSTRUCTOR_TEST = true;

	public ChangeSignatureTests(String name) {
		super(name);
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static Test suite() {
		if (true) {
			return new MySetup(new TestSuite(clazz));
		} else {
			System.err.println("*** Running only parts of " + clazz.getName() + "!");
			TestSuite suite= new TestSuite();
			suite.addTest(new ChangeSignatureTests("testAll58"));
			return new MySetup(suite);
		}
	}
	
	public static Test setUpTest(Test someTest) {
		return new MySetup(someTest);
	}
	
	private String getSimpleTestFileName(boolean canReorder, boolean input){
		String fileName = "A_" + getName();
		if (canReorder)
			fileName += input ? "_in": "_out";
		return fileName + ".java"; 
	}
	
	private String getTestFileName(boolean canReorder, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName += (canReorder ? "canModify/": "cannotModify/");
		return fileName + getSimpleTestFileName(canReorder, input);
	}
		
	//---helpers 
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canRename, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canRename, input), getFileContents(getTestFileName(canRename, input)));
	}

	private static ParameterInfo[] createNewParamInfos(String[] newTypes, String[] newNames, String[] newDefaultValues) {
		if (newTypes == null)
			return new ParameterInfo[0];
		ParameterInfo[] result= new ParameterInfo[newTypes.length];
		for (int i= 0; i < newDefaultValues.length; i++) {
			result[i]= ParameterInfo.createInfoForAddedParameter();
			result[i].setNewName(newNames[i]);
			result[i].setNewTypeName(newTypes[i]);
			result[i].setDefaultValue(newDefaultValues[i]);
		}
		return result;
	}

	private static void addInfos(List list, ParameterInfo[] newParamInfos, int[] newIndices) {
		if (newParamInfos == null || newIndices == null)
			return;
		for (int i= newIndices.length - 1; i >= 0; i--) {
			list.add(newIndices[i], newParamInfos[i]);
		}
	}
		
	private void helperAdd(String[] signature, ParameterInfo[] newParamInfos, int[] newIndices) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		IMethod method = classA.getMethod("m", signature);
		assertTrue("method does not exist", method.exists());
		ChangeSignatureRefactoring ref= ChangeSignatureRefactoring.create(method, JavaPreferencesSettings.getCodeGenerationSettings());
		addInfos(ref.getParameterInfos(), newParamInfos, newIndices);
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		String expectedFileContents= getFileContents(getTestFileName(true, false));
		assertEqualLines("invalid renaming", expectedFileContents, newcu.getSource());
	}

	private void helperDoAll(String typeName, 
								String methodName, 
							  	String[] signature, 
							  	ParameterInfo[] newParamInfos, 
							  	int[] newIndices, 
							  	String[] oldParamNames, 
							  	String[] newParamNames, 
							  	String[] newParameterTypeNames, 
							  	int[] permutation,
							  	int newVisibility,
							  	int[] deleted, String returnTypeName)  throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, typeName);
		IMethod method = classA.getMethod(methodName, signature);
		assertTrue("method " + methodName +" does not exist", method.exists());
		ChangeSignatureRefactoring ref= ChangeSignatureRefactoring.create(method, JavaPreferencesSettings.getCodeGenerationSettings());
		if (returnTypeName != null)
			ref.setNewReturnTypeName(returnTypeName);
		markAsDeleted(ref.getParameterInfos(), deleted);	
		modifyInfos(ref.getParameterInfos(), newParamInfos, newIndices, oldParamNames, newParamNames, newParameterTypeNames, permutation);
		if (newVisibility != JdtFlags.VISIBILITY_CODE_INVALID)
			ref.setVisibility(newVisibility);
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		String expectedFileContents= getFileContents(getTestFileName(true, false));
		assertEqualLines(expectedFileContents, newcu.getSource());
	}
	
	private void markAsDeleted(List list, int[] deleted) {
		if (deleted == null)
			return;
		for (int i= 0; i < deleted.length; i++) {
			((ParameterInfo)list.get(i)).markAsDeleted();
		}
	}

	private void helper1(String[] newOrder, String[] signature) throws Exception{
		helper1(newOrder, signature, null, null);
	}
	
	private void helper1(String[] newOrder, String[] signature, String[] oldNames, String[] newNames) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		IMethod method = classA.getMethod("m", signature);
		assertTrue("method does not exist", method.exists());
		ChangeSignatureRefactoring ref= ChangeSignatureRefactoring.create(method, JavaPreferencesSettings.getCodeGenerationSettings());
		modifyInfos(ref.getParameterInfos(), newOrder, oldNames, newNames);
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		String expectedFileContents= getFileContents(getTestFileName(true, false));
//		assertEquals("invalid renaming", expectedFileContents, newcu.getSource());
		assertEqualLines(expectedFileContents, newcu.getSource());
	}

	private void modifyInfos(List infos, ParameterInfo[] newParamInfos, int[] newIndices, String[] oldParamNames, String[] newParamNames, String[] newParamTypeNames, int[] permutation) {
		addInfos(infos, newParamInfos, newIndices);
		List swapped= new ArrayList(infos.size());
		List oldNameList= Arrays.asList(oldParamNames);
		List newNameList= Arrays.asList(newParamNames);
		for (int i= 0; i < permutation.length; i++) {
			if (((ParameterInfo)infos.get(i)).isAdded())
				continue;
			if (! swapped.contains(new Integer(i))){
				swapped.add(new Integer(permutation[i]));

				ParameterInfo infoI= (ParameterInfo)infos.get(i);
				infoI.setNewName((String)newNameList.get(oldNameList.indexOf(infoI.getOldName())));
				if (newParamTypeNames != null)
					infoI.setNewTypeName(newParamTypeNames[oldNameList.indexOf(infoI.getOldName())]);

				ParameterInfo infoI1= (ParameterInfo)infos.get(permutation[i]);
				infoI1.setNewName((String)newNameList.get(oldNameList.indexOf(infoI1.getOldName())));
				if (newParamTypeNames != null)
					infoI1.setNewTypeName(newParamTypeNames[oldNameList.indexOf(infoI1.getOldName())]);

				swap(infos, i, permutation[i]);
			}	
		}
	}

	private static void modifyInfos(List infos, String[] newOrder, String[] oldNames, String[] newNames) {
		int[] permutation= createPermutation(infos, newOrder);
		List swapped= new ArrayList(infos.size());
		if (oldNames == null || newNames == null){
			ParameterInfo[] newInfos= new  ParameterInfo[infos.size()];
			for (int i= 0; i < permutation.length; i++) {
				newInfos[i]= (ParameterInfo)infos.get(permutation[i]);
			}
			infos.clear();
			for (int i= 0; i < newInfos.length; i++) {
				infos.add(newInfos[i]);
			}
			return;
		} else {
			List oldNameList= Arrays.asList(oldNames);
			List newNameList= Arrays.asList(newNames);
			for (int i= 0; i < permutation.length; i++) {
				if (! swapped.contains(new Integer(i))){
					swapped.add(new Integer(permutation[i]));
					ParameterInfo infoI= (ParameterInfo)infos.get(i);
					infoI.setNewName((String)newNameList.get(oldNameList.indexOf(infoI.getOldName())));
					ParameterInfo infoI1= (ParameterInfo)infos.get(permutation[i]);
					infoI1.setNewName((String)newNameList.get(oldNameList.indexOf(infoI1.getOldName())));
					swap(infos, i, permutation[i]);
				}				
			}
		}
	}

	private static void swap(List infos, int i, int i1) {
		Object o= infos.get(i);
		infos.set(i, infos.get(i1));
		infos.set(i1, o);
	}

	private static int[] createPermutation(List infos, String[] newOrder) {
		int[] result= new int[infos.size()];
		for (int i= 0; i < result.length; i++) {
			result[i]= indexOfOldName(infos, newOrder[i]);
		}
		return result;
	}

	private static int indexOfOldName(List infos, String string) {
		for (Iterator iter= infos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (info.getOldName().equals(string))
				return infos.indexOf(info);
		}
		assertTrue(false);
		return -1;
	}

	private void helperFail(String[] newOrder, String[] signature, int expectedSeverity) throws Exception{
		IType classA= getType(createCUfromTestFile(getPackageP(), false, false), "A");
		ChangeSignatureRefactoring ref= ChangeSignatureRefactoring.create(classA.getMethod("m", signature), JavaPreferencesSettings.getCodeGenerationSettings());
		modifyInfos(ref.getParameterInfos(), newOrder, null, null);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);		
		assertEquals("Severity:", expectedSeverity, result.getSeverity());
	}

	private void helperAddFail(String[] signature, ParameterInfo[] newParamInfos, int[] newIndices, int expectedSeverity) throws Exception{
		IType classA= getType(createCUfromTestFile(getPackageP(), false, false), "A");
		ChangeSignatureRefactoring ref= ChangeSignatureRefactoring.create(classA.getMethod("m", signature), JavaPreferencesSettings.getCodeGenerationSettings());
		addInfos(ref.getParameterInfos(), newParamInfos, newIndices);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);		
		assertEquals("Severity:" + result.getFirstMessage(result.getSeverity()), expectedSeverity, result.getSeverity());
	}
	
	private void helperDoAllFail(String methodName, 
								String[] signature, 
							  	ParameterInfo[] newParamInfos, 
							  	int[] newIndices, 
							  	String[] oldParamNames, 
							  	String[] newParamNames, 
							  	int[] permutation, 
							  	int newVisibility,
							  	int[] deleted,
							  	int expectedSeverity)  throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, false);
		IType classA= getType(cu, "A");
		IMethod method = classA.getMethod(methodName, signature);
		assertTrue("method does not exist", method.exists());
		ChangeSignatureRefactoring ref= ChangeSignatureRefactoring.create(method, JavaPreferencesSettings.getCodeGenerationSettings());
		markAsDeleted(ref.getParameterInfos(), deleted);	
		modifyInfos(ref.getParameterInfos(), newParamInfos, newIndices, oldParamNames, newParamNames, null, permutation);
		if (newVisibility != JdtFlags.VISIBILITY_CODE_INVALID)
			ref.setVisibility(newVisibility);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);	
		assertEquals("Severity:" + result.getFirstMessage(result.getSeverity()), expectedSeverity, result.getSeverity());		
	}
	
	private void helperException(String[] signature, String[] removeExceptions, String[] addExceptions) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		IMethod method = classA.getMethod("m", signature);
		assertTrue("method does not exist", method.exists());
		ChangeSignatureRefactoring ref= ChangeSignatureRefactoring.create(method, JavaPreferencesSettings.getCodeGenerationSettings());

		// from RefactoringTest#performRefactoring():
		RefactoringStatus status= ref.checkActivation(new NullProgressMonitor());
		assertTrue("checkActivation was supposed to pass", status.isOK());

		mangleExceptions(ref.getExceptionInfos(), removeExceptions, addExceptions, method.getCompilationUnit());

		status= ref.checkInput(new NullProgressMonitor());
		assertTrue("checkInput was supposed to pass", status.isOK());
		IChange change= ref.createChange(new NullProgressMonitor());
		performChange(change);
		// XXX: this should be done by someone else
		Refactoring.getUndoManager().addUndo(ref.getName(), change.getUndoChange());
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		String expectedFileContents= getFileContents(getTestFileName(true, false));
		assertEqualLines("invalid renaming", expectedFileContents, newcu.getSource());
	}
	
	
	private void mangleExceptions(List list, String[] removeExceptions, String[] addExceptions, ICompilationUnit cu) throws Exception {
		for (Iterator iter= list.iterator(); iter.hasNext(); ) {
			ExceptionInfo info= (ExceptionInfo) iter.next();
			String name= JavaModelUtil.getFullyQualifiedName(info.getType());
			for (int i= 0; i < removeExceptions.length; i++) {
				if (name.equals(removeExceptions[i]))
					iter.remove();
			}
		}
		for (int i= 0; i < addExceptions.length; i++) {
//			IType type= JavaModelUtil.findTypeInCompilationUnit(cu, addExceptions[i]);
			IType type= JavaModelUtil.findType(cu.getJavaProject(), addExceptions[i]);
			list.add(ExceptionInfo.createInfoForAddedException(type));
		}
	}

	//------- tests 
	
	public void testFail0() throws Exception{
		helperFail(new String[]{"j", "i"}, new String[]{"I", "I"}, RefactoringStatus.ERROR);
	}
	
	public void testFail1() throws Exception{
		//TODO: is duplicate of testFail0()
		helperFail(new String[]{"j", "i"}, new String[]{"I", "I"}, RefactoringStatus.ERROR);
	}

	public void testFailAdd2() throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};
		helperAddFail(signature, newParamInfo, newIndices, RefactoringStatus.ERROR);
	}

	public void testFailAdd3() throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"not good"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};
		helperAddFail(signature, newParamInfo, newIndices, RefactoringStatus.FATAL);
	}

	public void testFailAdd4() throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"x"};
		String[] newTypes= {"not a type"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};
		helperAddFail(signature, newParamInfo, newIndices, RefactoringStatus.FATAL);
	}
	
	public void testFailDoAll5()throws Exception{
		String[] signature= {"I"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"i", "j"};
		String[] newParamNames= {"i", "j"};
		int[] permutation= {0};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.NONE;
		int expectedSeverity= RefactoringStatus.ERROR;
		helperDoAllFail("m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, permutation, newVisibility, deletedIndices, expectedSeverity);
	}	
	
	public void testFailDoAll6()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"a"};
		String[] newTypes= {"Certificate"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};
		
		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.NONE;
		int expectedSeverity= RefactoringStatus.ERROR;
		helperDoAllFail("m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, permutation, newVisibility, deletedIndices, expectedSeverity);
	}	

	public void testFailDoAll7()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"a"};
		String[] newTypes= {"Fred"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};
		
		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.NONE;
		int expectedSeverity= RefactoringStatus.ERROR;
		helperDoAllFail("m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, permutation, newVisibility, deletedIndices, expectedSeverity);
	}	
	
	//---------
	public void test0() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test1() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test2() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test3() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test4() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test5() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test6() throws Exception{
		helper1(new String[]{"k", "i", "j"}, new String[]{"I", "I", "I"});
	}

	public void test7() throws Exception{
		helper1(new String[]{"i", "k", "j"}, new String[]{"I", "I", "I"});
	}

	public void test8() throws Exception{
		helper1(new String[]{"k", "j", "i"}, new String[]{"I", "I", "I"});
	}
	
	public void test9() throws Exception{
		helper1(new String[]{"j", "i", "k"}, new String[]{"I", "I", "I"});
	}

	public void test10() throws Exception{
		helper1(new String[]{"j", "k", "i"}, new String[]{"I", "I", "I"});
	}

	public void test11() throws Exception{
		helper1(new String[]{"j", "k", "i"}, new String[]{"I", "I", "I"});
	}

	public void test12() throws Exception{
		helper1(new String[]{"j", "k", "i"}, new String[]{"I", "I", "I"});
	}

	public void test13() throws Exception{
		helper1(new String[]{"j", "k", "i"}, new String[]{"I", "I", "I"});
	}
	
	public void test14() throws Exception{
		helper1(new String[]{"j", "i"}, new String[]{"I", "I"});
	}
	
	public void test15() throws Exception{
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"});
	}
	
	public void test16() throws Exception{
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"});
	}
	
	public void test17() throws Exception{
		//exception because of bug 11151
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"});
	}
	
	public void test18() throws Exception{
		//exception because of bug 11151
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"});
	}
	
	public void test19() throws Exception{
//		printTestDisabledMessage("bug 7274 - reorder parameters: incorrect when parameters have more than 1 modifiers");
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"});
	}
	public void test20() throws Exception{
//		printTestDisabledMessage("bug 18147");
		helper1(new String[]{"b", "a"}, new String[]{"I", "[I"});
	}

//constructor tests
	public void test21() throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}
		String[] signature= {"I", "I"};
		ParameterInfo[] newParamInfo= null;
		int[] newIndices= null;
		
		String[] oldParamNames= {"a", "b"};
		String[] newParamNames= {"a", "b"};
		int[] permutation= {1, 0};
		int newVisibility= JdtFlags.VISIBILITY_CODE_INVALID;//retain
		int[] deleted= null;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deleted, newReturnTypeName);
	}
	public void test22() throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}
		String[] signature= {"I", "I"};
		ParameterInfo[] newParamInfo= null;
		int[] newIndices= null;
		
		String[] oldParamNames= {"a", "b"};
		String[] newParamNames= {"a", "b"};
		int[] permutation= {1, 0};
		int newVisibility= JdtFlags.VISIBILITY_CODE_INVALID;//retain
		int[] deleted= null;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deleted, newReturnTypeName);
	}
	public void test23() throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}
		String[] signature= {"I", "I"};
		ParameterInfo[] newParamInfo= null;
		int[] newIndices= null;
		
		String[] oldParamNames= {"a", "b"};
		String[] newParamNames= {"a", "b"};
		int[] permutation= {1, 0};
		int newVisibility= JdtFlags.VISIBILITY_CODE_INVALID;//retain
		int[] deleted= null;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deleted, newReturnTypeName);
	}
	public void test24() throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}
//		if (true){
//			printTestDisabledMessage("Bug 24230");
//			return;
//		}	
		String[] signature= {"I", "I"};
		ParameterInfo[] newParamInfo= null;
		int[] newIndices= null;
		
		String[] oldParamNames= {"a", "b"};
		String[] newParamNames= {"a", "b"};
		int[] permutation= {1, 0};
		int newVisibility= JdtFlags.VISIBILITY_CODE_INVALID;//retain
		int[] deleted= null;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deleted, newReturnTypeName);
	}
	public void test25() throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}
		String[] signature= {"I", "I"};
		ParameterInfo[] newParamInfo= null;
		int[] newIndices= null;
		
		String[] oldParamNames= {"a", "b"};
		String[] newParamNames= {"a", "b"};
		int[] permutation= {1, 0};
		int newVisibility= JdtFlags.VISIBILITY_CODE_INVALID;//retain
		int[] deleted= null;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deleted, newReturnTypeName);
	}
	public void test26() throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}
		String[] signature= {"I", "I"};
		ParameterInfo[] newParamInfo= null;
		int[] newIndices= null;
		
		String[] oldParamNames= {"a", "b"};
		String[] newParamNames= {"a", "b"};
		int[] permutation= {1, 0};
		int newVisibility= JdtFlags.VISIBILITY_CODE_INVALID;//retain
		int[] deleted= null;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deleted, newReturnTypeName);
	}

	public void testRenameReorder26() throws Exception{
		helper1(new String[]{"a", "y"}, new String[]{"Z", "I"}, new String[]{"y", "a"}, new String[]{"zzz", "bb"});
	}
	
	public void testRenameReorder27() throws Exception{
		helper1(new String[]{"a", "y"}, new String[]{"Z", "I"}, new String[]{"y", "a"}, new String[]{"yyy", "a"});
	}

	public void testAdd28()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};
		helperAdd(signature, newParamInfo, newIndices);
	}

	public void testAdd29()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};
		helperAdd(signature, newParamInfo, newIndices);
	}

	public void testAdd30()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};
		helperAdd(signature, newParamInfo, newIndices);
	}
	
	public void testAdd31()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};
		helperAdd(signature, newParamInfo, newIndices);
	}

	public void testAdd32()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};
		helperAdd(signature, newParamInfo, newIndices);
	}

	public void testAdd33()throws Exception{
		String[] signature= {};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};
		helperAdd(signature, newParamInfo, newIndices);
	}

	public void testAddReorderRename34()throws Exception{
		String[] signature= {"I", "Z"};
		String[] newNames= {"x"};
		String[] newTypes= {"Object"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};
		
		String[] oldParamNames= {"iii", "j"};
		String[] newParamNames= {"i", "jj"};
		int[] permutation= {2, -1, 0};
		int[] deletedIndices= null;
		int newVisibility= JdtFlags.VISIBILITY_CODE_INVALID;//retain
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	

	public void testAll35()throws Exception{
		String[] signature= {"I", "Z"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"iii", "j"};
		String[] newParamNames= oldParamNames;
		int[] permutation= {0, 1};
		int[] deletedIndices= null;
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	

	public void testAll36()throws Exception{
		String[] signature= {"I", "Z"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"iii", "j"};
		String[] newParamNames= oldParamNames;
		int[] permutation= {0, 1};
		int[] deletedIndices= null;
		int newVisibility= Modifier.PRIVATE;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	

	public void testAll37()throws Exception{
		String[] signature= {"I", "Z"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"iii", "j"};
		String[] newParamNames= oldParamNames;
		int[] permutation= {0, 1};
		int[] deletedIndices= null;
		int newVisibility= Modifier.PROTECTED;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	

	public void testAll38()throws Exception{
		String[] signature= {"I", "Z"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"iii", "j"};
		String[] newParamNames= oldParamNames;
		int[] permutation= {0, 1};
		int[] deletedIndices= null;
		int newVisibility= Modifier.PROTECTED;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	

	public void testAll39()throws Exception{
		String[] signature= {"I", "Z"};
		String[] newNames= {"x"};
		String[] newTypes= {"Object"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};
		
		String[] oldParamNames= {"iii", "j"};
		String[] newParamNames= {"i", "jj"};
		int[] permutation= {2, -1, 0};
		int[] deletedIndices= null;
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	

	public void testAll40()throws Exception{
		String[] signature= {"I", "Z"};
		String[] newNames= {"x"};
		String[] newTypes= {"int[]"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};
		
		String[] oldParamNames= {"iii", "j"};
		String[] newParamNames= {"i", "jj"};
		int[] permutation= {2, -1, 0};
		int[] deletedIndices= null;
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	

	public void testAll41()throws Exception{
		String[] signature= {"I"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"i"};
		String[] newParamNames= {"i"};
		int[] permutation= {0};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	

	public void testAll42()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"i"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};
		
		String[] oldParamNames= {"i"};
		String[] newParamNames= {"i"};
		int[] permutation= {0, -1};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	

	public void testAll43()throws Exception{
		String[] signature= {"I", "I"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"i", "j"};
		String[] newParamNames= {"i", "j"};
		int[] permutation= {1, 0};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	

	public void testAll44()throws Exception{
		if (true){
			printTestDisabledMessage("need to decide how to treat compile errors");
			return;
		}
		String[] signature= {"I", "I"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"i", "j"};
		String[] newParamNames= {"i", "j"};
		int[] permutation= {0, 1};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= "boolean";
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	

	public void testAll45()throws Exception{
		if (true){
			printTestDisabledMessage("need to decide how to treat compile errors");
			return;
		}
		
		String[] signature= {"I", "I"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"i", "j"};
		String[] newParamNames= {"i", "j"};
		String[] newParamTypeNames= {"int", "boolean"};
		int[] permutation= {0, 1};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParamTypeNames, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	
	
	public void testAll46()throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}

		String[] signature= {};
		String[] newNames= {"i"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"1"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= null;
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testAll47()throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}

		String[] signature= {};
		String[] newNames= {"i"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"1"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= null;
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testAll48()throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}

		String[] signature= {};
		String[] newNames= {"i"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"1"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= null;
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testAll49()throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}

		String[] signature= {};
		String[] newNames= {"i"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"1"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= null;
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testAll50()throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}

		String[] signature= {};
		String[] newNames= {"i"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"1"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= null;
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testAll51()throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}

		String[] signature= {};
		String[] newNames= {"i"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"1"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= null;
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testAll52()throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}

		String[] signature= {};
		String[] newNames= {"i"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"1"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= null;
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testAll53()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"a"};
		String[] newTypes= {"HashSet"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testAll54()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"a"};
		String[] newTypes= {"List"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testAll55()throws Exception{
//		printTestDisabledMessage("test for bug 32654 [Refactoring] Change method signature with problems");
		String[] signature= {"[QObject;", "I", "Z"};
		String[] newNames= {"e"};
		String[] newTypes= {"boolean"};
		String[] newDefaultValues= {"true"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {2};
		helperAdd(signature, newParamInfo, newIndices);

	}

	public void testAll56()throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}
			
//		printTestDisabledMessage("test for 38366 ArrayIndexOutOfBoundsException in change signeture [refactoring] ");
		String[] signature= {"QEvaViewPart;", "I"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {};

		String[] oldParamNames= {"part", "title"};
		String[] newParamNames= {"part", "title"};
		int[] permutation= {0, 1};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("HistoryFrame", "HistoryFrame", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testAll57()throws Exception{
//		printTestDisabledMessage("test for 39633 classcast exception when refactoring change method signature [refactoring]");
//		if (true)
//			return;
		String[] signature= {"I", "QString;", "QString;"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;

		String[] oldParamNames= {"i", "hello", "goodbye"};
		String[] newParamNames= oldParamNames;
		int[] permutation= {0, 2, 1};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("TEST.X", "method", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testAll58()throws Exception{
		String[] signature= {"I", "[[[QString;"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;

		String[] oldParamNames= {"a", "b"};
		String[] newParamNames= {"abb", "bbb"};
		int[] permutation= {1, 0};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testAddRecursive1()throws Exception{
		printTestDisabledMessage("strange NPE: method binding for super.m(1) in class Super " +
				"is not resolved in JUnit Plugin Test (but is OK in Eclipse Runtime...).");
//		String[] signature= {"I"};
//		String[] newNames= {"bool"};
//		String[] newTypes= {"boolean"};
//		String[] newDefaultValues= {"true"};
//		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
//		int[] newIndices= {1};
//		helperAdd(signature, newParamInfo, newIndices);
	}
	
	public void testException01() throws Exception {
		String[] signature= {"J"};
		String[] remove= {};
		String[] add= {"java.util.zip.ZipException"};
		helperException(signature, remove, add);
	}
	
}
