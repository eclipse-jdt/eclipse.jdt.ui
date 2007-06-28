/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;

import org.eclipse.jdt.internal.corext.refactoring.ExceptionInfo;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.structure.ChangeSignatureRefactoring;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

/**
 * @see org.eclipse.jdt.core.Signature for encoding of signature strings.
 */
public class ChangeSignatureTests extends RefactoringTest {
	private static final Class clazz= ChangeSignatureTests.class;
	private static final String REFACTORING_PATH= "ChangeSignature/";
	
	private static final boolean BUG_83691_CORE_JAVADOC_REF= true;
	
	private static final boolean RUN_CONSTRUCTOR_TEST= true;

	public ChangeSignatureTests(String name) {
		super(name);
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static Test suite() {
		if (true) {
			return new RefactoringTestSetup(new TestSuite(clazz));
		} else {
			System.err.println("*** Running only parts of " + clazz.getName() + "!");
			TestSuite suite= new TestSuite();
			suite.addTest(new ChangeSignatureTests("testDelegate05"));
			return new RefactoringTestSetup(suite);
		}
	}
	
	public static Test setUpTest(Test someTest) {
		return new RefactoringTestSetup(someTest);
	}
	
	private String getSimpleTestFileName(boolean canReorder, boolean input){
		String fileName = "A_" + getName();
		if (canReorder)
			fileName += input ? "_in": "_out";
		return fileName + ".java"; 
	}
	
	private String getTestFileName(boolean canReorder, boolean input){
		String fileName= getTestFolderPath(canReorder);
		return fileName + getSimpleTestFileName(canReorder, input);
	}

	private String getTestFolderPath(boolean canModify) {
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName += (canModify ? "canModify/": "cannotModify/");
		return fileName;
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
			result[i]= ParameterInfo.createInfoForAddedParameter(newTypes[i], newNames[i], newDefaultValues[i]);
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
		helperAdd(signature, newParamInfos, newIndices, false);
	}
	
	private void helperAdd(String[] signature, ParameterInfo[] newParamInfos, int[] newIndices, boolean createDelegate) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		IMethod method = classA.getMethod("m", signature);
		assertTrue("method does not exist", method.exists());
		ChangeSignatureRefactoring ref= (RefactoringAvailabilityTester.isChangeSignatureAvailable(method) ? new ChangeSignatureRefactoring(method) : null);
		ref.setDelegateUpdating(createDelegate);
		addInfos(ref.getParameterInfos(), newParamInfos, newIndices);
		RefactoringStatus initialConditions= ref.checkInitialConditions(new NullProgressMonitor());
		assertTrue("precondition was supposed to pass:"+initialConditions.getEntryWithHighestSeverity(), initialConditions.isOK());
		JavaRefactoringDescriptor descriptor= ref.createDescriptor();
		RefactoringStatus result= performRefactoring(descriptor);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		String expectedFileContents= getFileContents(getTestFileName(true, false));
		assertEqualLines("invalid renaming", expectedFileContents, newcu.getSource());
	}
	
	/*
	 * Rename method 'A.m(signature)' to 'A.newMethodName(signature)'
	 */
	private void helperRenameMethod(String[] signature, String newMethodName, boolean createDelegate) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		IMethod method = classA.getMethod("m", signature);
		assertTrue("method m does not exist in A", method.exists());
		ChangeSignatureRefactoring ref= (RefactoringAvailabilityTester.isChangeSignatureAvailable(method) ? new ChangeSignatureRefactoring(method) : null);
		ref.setNewMethodName(newMethodName);
		ref.setDelegateUpdating(createDelegate);
		ref.checkInitialConditions(new NullProgressMonitor());
		JavaRefactoringDescriptor descriptor= ref.createDescriptor();
		RefactoringStatus result= performRefactoring(descriptor);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		String expectedFileContents= getFileContents(getTestFileName(true, false));
		assertEqualLines("invalid change of method name", expectedFileContents, newcu.getSource());
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
							  	int[] deleted, String returnTypeName, boolean createDelegate)  throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, typeName);
		IMethod method = classA.getMethod(methodName, signature);
		assertTrue("method " + methodName +" does not exist", method.exists());
		ChangeSignatureRefactoring ref= (RefactoringAvailabilityTester.isChangeSignatureAvailable(method) ? new ChangeSignatureRefactoring(method) : null);
		if (returnTypeName != null)
			ref.setNewReturnTypeName(returnTypeName);
		ref.setDelegateUpdating(createDelegate);
		markAsDeleted(ref.getParameterInfos(), deleted);	
		modifyInfos(ref.getParameterInfos(), newParamInfos, newIndices, oldParamNames, newParamNames, newParameterTypeNames, permutation);
		if (newVisibility != JdtFlags.VISIBILITY_CODE_INVALID)
			ref.setVisibility(newVisibility);
		RefactoringStatus initialConditions= ref.checkInitialConditions(new NullProgressMonitor());
		assertTrue(initialConditions.isOK());
		JavaRefactoringDescriptor descriptor= ref.createDescriptor();
		RefactoringStatus result= performRefactoring(descriptor);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		String expectedFileContents= getFileContents(getTestFileName(true, false));
		assertEqualLines(expectedFileContents, newcu.getSource());
	}
	
	private void helperDoAll(String typeName, String methodName, String[] signature, ParameterInfo[] newParamInfos, int[] newIndices,
			String[] oldParamNames, String[] newParamNames, String[] newParameterTypeNames, int[] permutation, int newVisibility, int[] deleted,
			String returnTypeName) throws Exception {
		helperDoAll(typeName, methodName, signature, newParamInfos, newIndices, oldParamNames, newParamNames, newParameterTypeNames, permutation,
				newVisibility, deleted, returnTypeName, false);
	}
	
	private void markAsDeleted(List list, int[] deleted) {
		if (deleted == null)
			return;
		for (int i= 0; i < deleted.length; i++) {
			((ParameterInfo)list.get(deleted[i])).markAsDeleted();
		}
	}

	private void helper1(String[] newOrder, String[] signature) throws Exception{
		helper1(newOrder, signature, null, null);
	}
	
	private void helper1(String[] newOrder, String[] signature, boolean createDelegate) throws Exception{
		helper1(newOrder, signature, null, null, createDelegate);
	}
	
	private void helper1(String[] newOrder, String[] signature, String[] oldNames, String[] newNames) throws Exception{
		helper1(newOrder, signature, oldNames, newNames, false);
	}
	
	private void helper1(String[] newOrder, String[] signature, String[] oldNames, String[] newNames, boolean createDelegate) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		IMethod method = classA.getMethod("m", signature);
		assertTrue("method does not exist", method.exists());
		ChangeSignatureRefactoring ref= (RefactoringAvailabilityTester.isChangeSignatureAvailable(method) ? new ChangeSignatureRefactoring(method) : null);
		ref.setDelegateUpdating(createDelegate);
		modifyInfos(ref.getParameterInfos(), newOrder, oldNames, newNames);
		ref.checkInitialConditions(new NullProgressMonitor());
		JavaRefactoringDescriptor descriptor= ref.createDescriptor();
		RefactoringStatus result= performRefactoring(descriptor);
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
		IMethod method= classA.getMethod("m", signature);
		ChangeSignatureRefactoring ref= (RefactoringAvailabilityTester.isChangeSignatureAvailable(method) ? new ChangeSignatureRefactoring(method) : null);
		modifyInfos(ref.getParameterInfos(), newOrder, null, null);
		ref.checkInitialConditions(new NullProgressMonitor());
		RefactoringStatus result= ref.checkInitialConditions(new NullProgressMonitor());
		if (result.isOK()) {
			JavaRefactoringDescriptor descriptor= ref.createDescriptor();
			result= performRefactoring(descriptor, true);
		}
		assertNotNull("precondition was supposed to fail", result);		
		assertEquals("Severity:", expectedSeverity, result.getSeverity());
	}

	private void helperAddFail(String[] signature, ParameterInfo[] newParamInfos, int[] newIndices, int expectedSeverity) throws Exception{
		IType classA= getType(createCUfromTestFile(getPackageP(), false, false), "A");
		IMethod method= classA.getMethod("m", signature);
		ChangeSignatureRefactoring ref= (RefactoringAvailabilityTester.isChangeSignatureAvailable(method) ? new ChangeSignatureRefactoring(method) : null);
		addInfos(ref.getParameterInfos(), newParamInfos, newIndices);
		RefactoringStatus result= ref.checkInitialConditions(new NullProgressMonitor());
		if (result.isOK()) {
			JavaRefactoringDescriptor descriptor= ref.createDescriptor();
			result= performRefactoring(descriptor, true);
		}
		assertNotNull("precondition was supposed to fail", result);
		assertEquals("Severity:" + result.getMessageMatchingSeverity(result.getSeverity()), expectedSeverity, result.getSeverity());
	}
	
	private void helperDoAllFail(String methodName, 
								String[] signature, 
							  	ParameterInfo[] newParamInfos, 
							  	int[] newIndices, 
							  	String[] oldParamNames, 
							  	String[] newParamNames, 
							  	String[] newParameterTypeNames, 
							  	int[] permutation,
							  	int newVisibility,
							  	int[] deleted, int expectedSeverity)  throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), false, false);
		IType classA= getType(cu, "A");
		IMethod method = classA.getMethod(methodName, signature);
		assertTrue("method does not exist", method.exists());
		ChangeSignatureRefactoring ref= (RefactoringAvailabilityTester.isChangeSignatureAvailable(method) ? new ChangeSignatureRefactoring(method) : null);
		markAsDeleted(ref.getParameterInfos(), deleted);	
		modifyInfos(ref.getParameterInfos(), newParamInfos, newIndices, oldParamNames, newParamNames, newParameterTypeNames, permutation);
		if (newVisibility != JdtFlags.VISIBILITY_CODE_INVALID)
			ref.setVisibility(newVisibility);
		RefactoringStatus result= ref.checkInitialConditions(new NullProgressMonitor());
		if (result.isOK()) {
			JavaRefactoringDescriptor descriptor= ref.createDescriptor();
			result= performRefactoring(descriptor);
		}
		assertNotNull("precondition was supposed to fail", result);	
		assertEquals("Severity:" + result.getMessageMatchingSeverity(result.getSeverity()), expectedSeverity, result.getSeverity());		
	}
	
	private void helperDoAllWithExceptions(String typeName, 
			String methodName, 
		  	String[] signature, 
		  	ParameterInfo[] newParamInfos, 
		  	int[] newIndices, 
		  	String[] oldParamNames, 
		  	String[] newParamNames, 
		  	String[] newParameterTypeNames, 
		  	int[] permutation,
		  	int newVisibility,
		  	int[] deleted,
			String returnTypeName,
			String [] removeExceptions,
			String[] addExceptions) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, typeName);
		IMethod method = classA.getMethod(methodName, signature);
		assertTrue("method " + methodName +" does not exist", method.exists());
		ChangeSignatureRefactoring ref= (RefactoringAvailabilityTester.isChangeSignatureAvailable(method) ? new ChangeSignatureRefactoring(method) : null);
		if (returnTypeName != null)
		ref.setNewReturnTypeName(returnTypeName);
		markAsDeleted(ref.getParameterInfos(), deleted);	
		modifyInfos(ref.getParameterInfos(), newParamInfos, newIndices, oldParamNames, newParamNames, newParameterTypeNames, permutation);
		if (newVisibility != JdtFlags.VISIBILITY_CODE_INVALID)
		ref.setVisibility(newVisibility);

		// from RefactoringTest#performRefactoring():
		RefactoringStatus status= ref.checkInitialConditions(new NullProgressMonitor());
		assertTrue("checkActivation was supposed to pass", status.isOK());
	
		mangleExceptions(ref.getExceptionInfos(), removeExceptions, addExceptions, method.getCompilationUnit());
	
		status= ref.checkFinalConditions(new NullProgressMonitor());
		assertTrue("checkInput was supposed to pass", status.isOK());
		Change undo= performChange(ref, true);
		assertNotNull(undo);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		String expectedFileContents= getFileContents(getTestFileName(true, false));
		assertEqualLines(expectedFileContents, newcu.getSource());
	}

	
	private void helperException(String[] signature, String[] removeExceptions, String[] addExceptions) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		IMethod method = classA.getMethod("m", signature);
		assertTrue("method does not exist", method.exists());
		ChangeSignatureRefactoring ref= (RefactoringAvailabilityTester.isChangeSignatureAvailable(method) ? new ChangeSignatureRefactoring(method) : null);
	
		// from RefactoringTest#performRefactoring():
		RefactoringStatus status= ref.checkInitialConditions(new NullProgressMonitor());
		assertTrue("checkActivation was supposed to pass", status.isOK());
	
		mangleExceptions(ref.getExceptionInfos(), removeExceptions, addExceptions, method.getCompilationUnit());
	
		status= ref.checkFinalConditions(new NullProgressMonitor());
		assertTrue("checkInput was supposed to pass", status.isOK());
		Change undo= performChange(ref, true);
		assertNotNull(undo);
		
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
					info.markAsDeleted();
			}
		}
		for (int i= 0; i < addExceptions.length; i++) {
			IType type= JavaModelUtil.findType(cu.getJavaProject(), addExceptions[i]);
			list.add(ExceptionInfo.createInfoForAddedException(type));
		}
	}

	//------- tests 
	
	public void testFail0() throws Exception{
		helperFail(new String[]{"j", "i"}, new String[]{"I", "I"}, RefactoringStatus.ERROR);
	}
	
	public void testFail1() throws Exception{
		helperFail(new String[0], new String[0], RefactoringStatus.FATAL);
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
		helperDoAllFail("m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, expectedSeverity);
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
		helperDoAllFail("m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, expectedSeverity);
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
		helperDoAllFail("m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, expectedSeverity);
	}	
	
	public void testFailDoAll8()throws Exception{
		String[] signature= {"I"};
		ParameterInfo[] newParamInfo= null;
		int[] newIndices= {0};
		
		String[] oldParamNames= {"I"};
		String[] newParamNames= {};
		int[] permutation= {};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.NONE;
		int expectedSeverity= RefactoringStatus.ERROR;
		helperDoAllFail("run", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, expectedSeverity);
	}

	public void testFailAnnotation1() throws Exception{
		IType classA= getType(createCUfromTestFile(getPackageP(), false, false), "A");
		IMethod method= classA.getMethod("name", new String[0]);
		assertNotNull(method);
		ChangeSignatureRefactoring ref= (RefactoringAvailabilityTester.isChangeSignatureAvailable(method) ? new ChangeSignatureRefactoring(method) : null);
		assertNull(ref);
	}
	
	public void testFailVararg01() throws Exception {
		//cannot change m(int, String...) to m(String..., int)
		String[] signature= {"I", "[QString;"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"i", "names"};
		String[] newParamNames= {"i", "names"};
		int[] permutation= {1, 0};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		int expectedSeverity= RefactoringStatus.FATAL;
		helperDoAllFail("m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, expectedSeverity);
	}

	public void testFailVararg02() throws Exception {
		//cannot introduce vararg in non-last position
		String[] signature= {"I", "[QString;"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"i", "names"};
		String[] newParamNames= {"i", "names"};
		String[] newParamTypeNames= {"int...", "String[]"};
		int[] permutation= {0, 1};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		int expectedSeverity= RefactoringStatus.FATAL;
		helperDoAllFail("m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParamTypeNames, permutation, newVisibility, deletedIndices, expectedSeverity);
	}

	public void testFailVararg03() throws Exception {
		//cannot change parameter type which is vararg in overriding method
		String[] signature= {"I", "[QString;"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"i", "names"};
		String[] newParamNames= {"i", "names"};
		String[] newParamTypeNames= {"int", "Object[]"};
		int[] permutation= {1, 0};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		int expectedSeverity= RefactoringStatus.FATAL;
		helperDoAllFail("m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParamTypeNames, permutation, newVisibility, deletedIndices, expectedSeverity);
	}

	public void testFailVararg04() throws Exception {
		//cannot change vararg to non-vararg
		String[] signature= {"I", "[QString;"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"i", "names"};
		String[] newParamNames= {"i", "names"};
		String[] newParamTypeNames= {"int", "String[]"};
		int[] permutation= {0, 1};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		int expectedSeverity= RefactoringStatus.FATAL;
		helperDoAllFail("m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParamTypeNames, permutation, newVisibility, deletedIndices, expectedSeverity);
	}

	public void testFailVararg05() throws Exception {
		//cannot move parameter which is vararg in ripple method
		String[] signature= {"I", "[QString;"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"i", "names"};
		String[] newParamNames= {"i", "names"};
		int[] permutation= {1, 0};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		int expectedSeverity= RefactoringStatus.FATAL;
		helperDoAllFail("m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, expectedSeverity);
	}

	public void testFailGenerics01() throws Exception {
		//type variable name may not be available in related methods
		String[] signature= {"QE;"};
		String[] newNames= {"e2"};
		String[] newTypes= {"E"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};
		
		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {};
		int[] deletedIndices= {};
		int newVisibility= Modifier.NONE;
		int expectedSeverity= RefactoringStatus.ERROR;
		helperDoAllFail("m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, expectedSeverity);
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
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"}, true);
	}
	
	public void test16() throws Exception{
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"}, true);
	}
	
	public void test17() throws Exception{
		//exception because of bug 11151
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"}, true);
	}
	
	public void test18() throws Exception{
		//exception because of bug 11151
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"}, true);
	}
	
	public void test19() throws Exception{
//		printTestDisabledMessage("bug 7274 - reorder parameters: incorrect when parameters have more than 1 modifiers");
		helper1(new String[]{"b", "i"}, new String[]{"I", "Z"}, true);
	}
	public void test20() throws Exception{
//		printTestDisabledMessage("bug 18147");
		helper1(new String[]{"b", "a"}, new String[]{"I", "[I"}, true);
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
	
	public void test27() throws Exception{
		if (! RUN_CONSTRUCTOR_TEST){
			printTestDisabledMessage("disabled for constructors for now");
			return;
		}
		String[] signature= {"QString;", "QObject;", "I"};
		ParameterInfo[] newParamInfo= createNewParamInfos(new String[]{"Object"}, new String[]{"newParam"}, new String[]{"null"});
		int[] newIndices= { 3 };
		
		String[] oldParamNames= {"msg", "xml", "id"};
		String[] newParamNames= {"msg", "xml", "id"};
		int[] permutation= {0, 1, 2};
		int newVisibility= JdtFlags.VISIBILITY_CODE_INVALID;//retain
		int[] deleted= null;
		String newReturnTypeName= null;
		helperDoAll("Query.PoolMessageEvent", "PoolMessageEvent", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deleted, newReturnTypeName, true);
	}

	public void testRenameReorder26() throws Exception{
		helper1(new String[]{"a", "y"}, new String[]{"Z", "I"}, new String[]{"y", "a"}, new String[]{"zzz", "bb"}, true);
	}
	
	public void testRenameReorder27() throws Exception{
		helper1(new String[]{"a", "y"}, new String[]{"Z", "I"}, new String[]{"y", "a"}, new String[]{"yyy", "a"}, true);
	}

	public void testAdd28()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};
		helperAdd(signature, newParamInfo, newIndices, true);
	}

	public void testAdd29()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};
		helperAdd(signature, newParamInfo, newIndices, true);
	}

	public void testAdd30()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};
		helperAdd(signature, newParamInfo, newIndices, true);
	}
	
	public void testAdd31()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};
		helperAdd(signature, newParamInfo, newIndices, true);
	}

	public void testAdd32()throws Exception{
		String[] signature= {"I"};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};
		helperAdd(signature, newParamInfo, newIndices, true);
	}

	public void testAdd33()throws Exception{
		String[] signature= {};
		String[] newNames= {"x"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};
		helperAdd(signature, newParamInfo, newIndices, true);
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
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName, true);
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
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName, true);
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
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName, true);
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

	public void testAll59() throws Exception{
		String[] signature= {"I", "J"};
		String[] newNames= {"really"};
		String[] newTypes= {"boolean"};
		String[] newDefaultValues= {"true"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {"from", "to"};
		String[] newParamNames= {"f", "t"};
		String[] newParameterTypeNames= {"int", "char"};
		int[] permutation= {0, 1, 2};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= "java.util.List";
		helperDoAll("A", "getList", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParameterTypeNames, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}
	
	public void testAll60() throws Exception{
		String[] signature= {"I", "J"};
		String[] newNames= {"l"};
		String[] newTypes= {"java.util.List"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};

		String[] oldParamNames= {"from", "to"};
		String[] newParamNames= {"to", "tho"};
		String[] newParameterTypeNames= {"int", "long"};
		int[] permutation= {2, 1, 0};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= "java.util.List";
		String[] removeExceptions= {"java.io.IOException"};
		String[] addExceptions= {"java.lang.Exception"};
		helperDoAllWithExceptions("I", "getList", signature, newParamInfo, newIndices,
				oldParamNames, newParamNames, newParameterTypeNames, permutation, newVisibility,
				deletedIndices, newReturnTypeName, removeExceptions, addExceptions);
	}

	public void testAll61()throws Exception{ //bug 51634
		String[] signature= {};
		ParameterInfo[] newParamInfo= null;
		int[] newIndices= null;
		
		String[] oldParamNames= {};
		String[] newParamNames= oldParamNames;
		int[] permutation= {};
		int[] deletedIndices= null;
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= "Object";
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	

	public void testAll62()throws Exception{ //bug 
		String[] signature= {"QBigInteger;", "QBigInteger;", "QBigInteger;"};
		ParameterInfo[] newParamInfo= null;
		int[] newIndices= null;
		String[] newParamTypeNames= {"long", "long", "long"};
		String[] oldParamNames= {"a", "b", "c"};
		String[] newParamNames= {"x", "y", "z"};
		int[] permutation= {0, 1, 2};
		int[] deletedIndices= null;
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= "void";
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParamTypeNames, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	
	
	public void testAll63()throws Exception{ //bug 
		String[] signature= {};
		ParameterInfo[] newParamInfo= null;
		int[] newIndices= null;
		String[] newParamTypeNames= {};
		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {};
		int[] deletedIndices= null;
		int newVisibility= Modifier.PROTECTED;
		String newReturnTypeName= "void";
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParamTypeNames, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}	
	
	public void testAddSyntaxError01()throws Exception{ // https://bugs.eclipse.org/bugs/show_bug.cgi?id=191349
		String refNameIn= "A_testAddSyntaxError01_Ref_in.java";
		String refNameOut= "A_testAddSyntaxError01_Ref_out.java";
		ICompilationUnit refCu= createCU(getPackageP(), refNameIn, getFileContents(getTestFolderPath(true) + refNameIn));
		
		String[] signature= {"QString;"};
		String[] newNames= {"newParam"};
		String[] newTypes= {"Object"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfos= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= { 1 };
		helperAdd(signature, newParamInfos, newIndices);
		
		String expectedRefContents= getFileContents(getTestFolderPath(true) + refNameOut);
		assertEqualLines(expectedRefContents, refCu.getSource());
	}	
	
	public void testAddRecursive1()throws Exception{ //bug 42100
		String[] signature= {"I"};
		String[] newNames= {"bool"};
		String[] newTypes= {"boolean"};
		String[] newDefaultValues= {"true"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};
		helperAdd(signature, newParamInfo, newIndices, true);
	}
	
	public void testException01() throws Exception {
		String[] signature= {"J"};
		String[] remove= {};
		String[] add= {"java.util.zip.ZipException"};
		helperException(signature, remove, add);
	}
	
	public void testException02() throws Exception {
		String[] add= new String[] {"java.lang.RuntimeException"};
		helperException(new String[0], new String[0], add);
	}

	public void testException03() throws Exception { //bug 52091
		String[] remove= new String[] {"java.lang.RuntimeException"};
		helperException(new String[0], remove, new String[0]);
	}

	public void testException04() throws Exception { //bug 52058
		String[] add= new String[] {"java.io.IOException", "java.lang.ClassNotFoundException"};
		helperException(new String[0], new String[0], add);
	}
	
	public void testException05() throws Exception { //bug 56132
		String[] remove= new String[] {"java.lang.IllegalArgumentException", "java.io.IOException"};
		helperException(new String[0], remove, new String[0]);
	}

	public void testInStatic01() throws Exception { //bug 47062
		String[] signature= {"QString;", "QString;"};
		ParameterInfo[] newParamInfo= null;
		int[] newIndices= null;
		
		String[] oldParamNames= {"arg1", "arg2"};
		String[] newParamNames= {"a", "b"};
		int[] permutation= {1, 0};
		int newVisibility= JdtFlags.VISIBILITY_CODE_INVALID;//retain
		int[] deleted= null;
		String newReturnTypeName= null;
		helperDoAll("Example", "Example", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deleted, newReturnTypeName);
	}

	public void testInStatic02() throws Exception { //bug 47062
		String[] signature= {"QString;", "QString;"};
		ParameterInfo[] newParamInfo= null;
		int[] newIndices= null;
		
		String[] oldParamNames= {"arg1", "arg2"};
		String[] newParamNames= {"a", "b"};
		int[] permutation= {1, 0};
		int newVisibility= JdtFlags.VISIBILITY_CODE_INVALID;//retain
		int[] deleted= null;
		String newReturnTypeName= null;
		helperDoAll("Example", "getExample", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deleted, newReturnTypeName);
	}
	
	public void testName01() throws Exception {
		String[] signature= {"QString;"};
		helperRenameMethod(signature, "newName", false);
	}

	public void testName02() throws Exception {
		String[] signature= {"QString;"};
		helperRenameMethod(signature, "newName", false);
	}
	
	public void testFailImport01() throws Exception {
		String[] signature= {};
		String[] newTypes= {"Permission"};
		String[] newNames= {"p"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};
		helperAddFail(signature, newParamInfo, newIndices, RefactoringStatus.ERROR);
	}

	public void testImport01() throws Exception {
		String[] signature= {};
		String[] newTypes= {"java.security.acl.Permission", "Permission"};
		String[] newNames= {"acl", "p"};
		String[] newDefaultValues= {"null", "perm"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0, 0};
		helperAdd(signature, newParamInfo, newIndices);
	}

	public void testImport02() throws Exception {
		String[] signature= {};
		String[] newTypes= {"Permission", "java.security.acl.Permission"};
		String[] newNames= {"p", "acl"};
		String[] newDefaultValues= {"null", "null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0, 0};
		helperAdd(signature, newParamInfo, newIndices);
	}

	public void testImport03() throws Exception {
		String[] signature= {};
		String[] newTypes= {"java.security.acl.Permission", "java.security.Permission"};
		String[] newNames= {"p", "pp"};
		String[] newDefaultValues= {"0", "0"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0, 0};
		helperAdd(signature, newParamInfo, newIndices);
	}

	public void testImport04() throws Exception {
		String[] signature= {};
		String[] newTypes= {"Object"};
		String[] newNames= {"o"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};
		helperAdd(signature, newParamInfo, newIndices);
	}
	
	public void testImport05() throws Exception {
		// printTestDisabledMessage("49772: Change method signature: remove unused imports [refactoring]");
		String[] signature= {};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {};
		int[] deletedIndices= null;
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= "Object";
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testImport06() throws Exception {
		String[] signature= {"QPermission;", "Qjava.security.acl.Permission;"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {};

		String[] oldParamNames= {"perm", "acl"};
		String[] newParamNames= {"xacl", "xperm"};
		String[] newParamTypeNames= {"java.security.acl.Permission [] []", "java.security.Permission"};
		int[] permutation= {1, 0};
		int[] deletedIndices= null;
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= "java.security.acl.Permission";
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParamTypeNames, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testImport07() throws Exception {
		// printTestDisabledMessage("49772: Change method signature: remove unused imports [refactoring]");
		String[] signature= {"QList;"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {};

		String[] oldParamNames= {"list"};
		String[] newParamNames= oldParamNames;
		String[] newParamTypeNames= null;
		int[] permutation= {0};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParamTypeNames, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}
	
	public void testImport08() throws Exception {
		// printTestDisabledMessage("68504: Refactor -> Change Method Signature removes import [refactoring]");
		String[] signature= {"QString;", "QVector;"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {};

		String[] oldParamNames= {"text", "v"};
		String[] newParamNames= oldParamNames;
		String[] newParamTypeNames= null;
		int[] permutation= {1, 0};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "textContains", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParamTypeNames, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}
	
	public void testEnum01() throws Exception {
		if (BUG_83691_CORE_JAVADOC_REF) {
			printTestDisabledMessage("BUG_83691_CORE_JAVADOC_REF");
			return;
		}
		String[] signature= {"I"};
		String[] newNames= {"a"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"17"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};

		String[] oldParamNames= {"i"};
		String[] newParamNames= {"i"};
		int[] permutation= {0};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PRIVATE;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}
	
	public void testEnum02() throws Exception {
		String[] signature= {"I"};
		String[] newNames= {"a"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"17"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};

		String[] oldParamNames= {"i"};
		String[] newParamNames= {"i"};
		int[] permutation= {0};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PRIVATE;
		String newReturnTypeName= null;
		helperDoAll("A_testEnum02_in", "A_testEnum02_in", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testEnum03() throws Exception {
		if (BUG_83691_CORE_JAVADOC_REF) {
			printTestDisabledMessage("BUG_83691_CORE_JAVADOC_REF");
			return;
		}
		String[] signature= {};
		String[] newNames= {"obj"};
		String[] newTypes= {"Object"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {};
		int[] deletedIndices= {};
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "A", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}
	
	public void testEnum04() throws Exception {
		String[] signature= {};
		String[] newNames= {"forward"};
		String[] newTypes= {"boolean"};
		String[] newDefaultValues= {"true"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "getNext", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}
	
	public void testStaticImport01() throws Exception {
		helperRenameMethod(new String[0], "abc", false);
	}
	
	public void testStaticImport02() throws Exception {
		String[] signature= {"QInteger;"};
		String[] newTypes= {"Object"};
		String[] newNames= {"o"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1};
		helperAdd(signature, newParamInfo, newIndices);
	}
	
	public void testVararg01() throws Exception {
		String[] signature= {"I", "[QString;"};
		String[] newNames= {};
		String[] newTypes= {};
		String[] newDefaultValues= {};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {};

		String[] oldParamNames= {"i", "names"};
		String[] newParamNames= {"i", "strings"};
		int[] permutation= {0, 1};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}
	
	public void testVararg02() throws Exception {
		String[] signature= {"I", "[QString;"};
		String[] newNames= {"o"};
		String[] newTypes= {"Object"};
		String[] newDefaultValues= {"new Object()"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {"i", "names"};
		String[] newParamNames= oldParamNames;
		int[] permutation= {0, 1, 2};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName, true);
	}
	
	public void testVararg03() throws Exception {
		String[] signature= {"[QString;"};
		String[] newNames= {};
		String[] newTypes= {};
		String[] newDefaultValues= {};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {};

		String[] oldParamNames= {"args"};
		String[] newParamNames= oldParamNames;
		String[] newParameterTypeNames= {"Object..."};
		int[] permutation= {0};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "use", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParameterTypeNames, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}
	
	public void testVararg04() throws Exception {
		String[] signature= {"[QString;"};
		String[] newNames= {"i"};
		String[] newTypes= {"int"};
		String[] newDefaultValues= {"1"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {"args"};
		String[] newParamNames= {"args"};
		int[] permutation= {};
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "use", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}
	
	public void testVararg05() throws Exception {
		String[] signature= {"QObject;", "[QString;"};
		String[] newNames= {};
		String[] newTypes= {};
		String[] newDefaultValues= {};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {};

		String[] oldParamNames= {"first", "args"};
		String[] newParamNames= {"arg", "invalid name"};
		int[] permutation= {0, 1};
		int[] deletedIndices= {1};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "use", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName, true);
	}
	
	public void testVararg06() throws Exception {
		String[] signature= {"I", "[QString;"};
		String[] newNames= {};
		String[] newTypes= {};
		String[] newDefaultValues= {};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {};

		String[] oldParamNames= {"i", "names"};
		String[] newParamNames= {"i", "names"};
		String[] newParameterTypeNames= {"int", "String..."};
		int[] permutation= {0, 1};
		int[] deletedIndices= { };
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParameterTypeNames, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testVararg07() throws Exception {
		//can remove parameter which is vararg in ripple method
		String[] signature= {"I", "[QString;"};
		String[] newNames= {"j", "k"};
		String[] newTypes= {"String", "Integer"};
		String[] newDefaultValues= {"\"none\"", "17"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {1, 2};

		String[] oldParamNames= {"i", "names"};
		String[] newParamNames= {"i", "names"};
		int[] permutation= {0, 1, 2, 3};
		int[] deletedIndices= { 1 };
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName, true);
	}
	
	public void testVararg08() throws Exception {
		//can add vararg parameter with empty default value
		String[] signature= {};
		String[] newNames= {"args"};
		String[] newTypes= {"String ..."};
		String[] newDefaultValues= {""};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= { };
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}
	
	public void testVararg09() throws Exception {
		//can add vararg parameter with one-expression default value
		String[] signature= {};
		String[] newNames= {"args"};
		String[] newTypes= {"String ..."};
		String[] newDefaultValues= {"\"Hello\""};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= { };
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}
	
	public void testVararg10() throws Exception {
		//can add vararg parameter with multiple-expressions default value
		String[] signature= {};
		String[] newNames= {"args"};
		String[] newTypes= {"String ..."};
		String[] newDefaultValues= {"\"Hello\", new String()"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {0};

		String[] oldParamNames= {};
		String[] newParamNames= {};
		int[] permutation= {0};
		int[] deletedIndices= { };
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}
	
	public void testGenerics01() throws Exception {
		String[] signature= {"QInteger;", "QE;"};
		String[] newNames= {};
		String[] newTypes= {};
		String[] newDefaultValues= {};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {};

		String[] oldParamNames= {"i", "e"};
		String[] newParamNames= {"integer", "e"};
		String[] newParameterTypeNames= null;
		int[] permutation= {1, 0};
		int[] deletedIndices= { };
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParameterTypeNames, permutation, newVisibility, deletedIndices, newReturnTypeName, true);
	}

	public void testGenerics02() throws Exception {
		String[] signature= {"QT;", "QE;"};
		String[] newNames= {"maps"};
		String[] newTypes= {"java.util.List<HashMap>"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {2};

		String[] oldParamNames= {"e", "t"};
		String[] newParamNames= {"e", "t"};
		String[] newParameterTypeNames= null;
		int[] permutation= {1, 0, 2};
		int[] deletedIndices= { };
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParameterTypeNames, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testGenerics03() throws Exception {
		String[] signature= {"QT;", "QE;"};
		String[] newNames= {"maps"};
		String[] newTypes= {"java.util.List<HashMap>"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {2};

		String[] oldParamNames= {"e", "t"};
		String[] newParamNames= {"e", "t"};
		String[] newParameterTypeNames= null;
		int[] permutation= {1, 0, 2};
		int[] deletedIndices= { };
		int newVisibility= Modifier.NONE;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParameterTypeNames, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}

	public void testGenerics04() throws Exception {
		String[] signature= {"QList<QInteger;>;", "QA<QString;>;"};
		String[] newNames= {"li"};
		String[] newTypes= {"List<Integer>"};
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {2};

		String[] oldParamNames= {"li", "as"};
		String[] newParamNames= {"li", "as"};
		String[] newParameterTypeNames= null;
		int[] permutation= {1, 2, 0};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParameterTypeNames, permutation, newVisibility, deletedIndices, newReturnTypeName);
	}
	
	public void testGenerics05() throws Exception {
		String[] signature= { "QClass;" };
		String[] newNames= {};
		String[] newTypes= {};
		String[] newDefaultValues= {};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= {};

		String[] oldParamNames= { "arg" };
		String[] newParamNames= { "arg" };
		String[] newParameterTypeNames= { "Class<?>" };
		int[] permutation= { 0 };
		int[] deletedIndices= {};
		int newVisibility= Modifier.PUBLIC;
		String newReturnTypeName= null;
		helperDoAll("I", "test", signature, newParamInfo, newIndices, oldParamNames, newParamNames, newParameterTypeNames, permutation,
				newVisibility, deletedIndices, newReturnTypeName);
	}	
		
	public void testDelegate01() throws Exception {
		// simple reordering with delegate
		helper1(new String[]{"j", "i"}, new String[]{"I", "QString;"}, null, null, true);
	}
	
	public void testDelegate02() throws Exception {
		// add a parameter -> import it
		String[] signature= {};
		String[] newTypes= {"java.util.List" };
		String[] newNames= {"list" };
		String[] newDefaultValues= {"null"};
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= { 0 };
		helperAdd(signature, newParamInfo, newIndices, true);
	}
	
	public void testDelegate03() throws Exception {
		// reordering with imported type in body => don't remove import
		helper1(new String[]{"j", "i"}, new String[]{"I", "QString;"}, null, null, true);
	}
	
	public void testDelegate04() throws Exception {
		// delete a parameter => import stays
		String[] signature= {"QList;"};
		String[] newNames= null;
		String[] newTypes= null;
		String[] newDefaultValues= null;
		ParameterInfo[] newParamInfo= createNewParamInfos(newTypes, newNames, newDefaultValues);
		int[] newIndices= null;
		
		String[] oldParamNames= {"l"};
		String[] newParamNames= {"l"};
		int[] permutation= {};
		int[] deletedIndices= {0};
		int newVisibility= Modifier.PRIVATE;
		String newReturnTypeName= null;
		helperDoAll("A", "m", signature, newParamInfo, newIndices, oldParamNames, newParamNames, null, permutation, newVisibility, deletedIndices, newReturnTypeName, true);	
	}
	
	public void testDelegate05() throws Exception {
		// bug 138320
		String[] signature= {};
		helperRenameMethod(signature, "renamed", true);
	}
	
}

