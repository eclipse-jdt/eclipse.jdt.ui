package org.eclipse.jdt.ui.tests.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.ModifyParametersRefactoring;

public class ReorderParametersTests extends RefactoringTest {
	private static final Class clazz= ReorderParametersTests.class;
	private static final String REFACTORING_PATH= "ReorderParameters/";
	
	/**
	 * Constructor for ReorderParametersTests
	 */
	public ReorderParametersTests(String name) {
		super(name);
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}
	
	private String getSimpleTestFileName(boolean canReorder, boolean input){
		String fileName = "A_" + getName();
		if (canReorder)
			fileName += input ? "_in": "_out";
		return fileName + ".java"; 
	}
	
	private String getTestFileName(boolean canReorder, boolean input){
		String fileName= TEST_PATH_PREFIX + getRefactoringPath();
		fileName += (canReorder ? "canReorder/": "cannotReorder/");
		return fileName + getSimpleTestFileName(canReorder, input);
	}
	
	private String getPassingTestFileName(boolean input){
		return getTestFileName(true, input);
	}	
	
	//---helpers 
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean canRename, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(canRename, input), getFileContents(getTestFileName(canRename, input)));
	}

	private static ParameterInfo[] createNewParamInfos(String[] newTypes, String[] newNames, String[] newDefaultValues) {
		ParameterInfo[] result= new ParameterInfo[newTypes.length];
		for (int i= 0; i < newDefaultValues.length; i++) {
			result[i]= ParameterInfo.createInfoForAddedParameter();
			result[i].setNewName(newNames[i]);
			result[i].setType(newTypes[i]);
			result[i].setDefaultValue(newDefaultValues[i]);
		}
		return result;
	}

	private static void addInfos(List list, ParameterInfo[] newParamInfos, int[] newIndices) {
		for (int i= newIndices.length - 1; i >= 0; i--) {
			list.add(newIndices[i], newParamInfos[i]);
		}
	}
		
	private void helperAdd(String[] signature, ParameterInfo[] newParamInfos, int[] newIndices) throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		IMethod method = classA.getMethod("m", signature);
		assertTrue("method does not exist", method.exists());
		ModifyParametersRefactoring ref= new ModifyParametersRefactoring(method);
		addInfos(ref.getParameterInfos(), newParamInfos, newIndices);
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEquals("invalid renaming", getFileContents(getTestFileName(true, false)), newcu.getSource());
	}

	private void helperAddReorderRename(String[] signature, ParameterInfo[] newParamInfos, int[] newIndices, String[] oldParamNames, String[] newParamNames, int[] permutation)  throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		IMethod method = classA.getMethod("m", signature);
		assertTrue("method does not exist", method.exists());
		ModifyParametersRefactoring ref= new ModifyParametersRefactoring(method);
		modifyInfos(ref.getParameterInfos(), newParamInfos, newIndices, oldParamNames, newParamNames, permutation);
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEquals("invalid renaming", getFileContents(getTestFileName(true, false)), newcu.getSource());
	}

	private void helper1(String[] newOrder, String[] signature) throws Exception{
		helper1(newOrder, signature, null, null);
	}
	
	private void helper1(String[] newOrder, String[] signature, String[] oldNames, String[] newNames) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true, true);
		IType classA= getType(cu, "A");
		IMethod method = classA.getMethod("m", signature);
		assertTrue("method does not exist", method.exists());
		ModifyParametersRefactoring ref= new ModifyParametersRefactoring(method);
		modifyInfos(ref.getParameterInfos(), newOrder, oldNames, newNames);
		RefactoringStatus result= performRefactoring(ref);
		assertEquals("precondition was supposed to pass", null, result);
		
		IPackageFragment pack= (IPackageFragment)cu.getParent();
		String newCuName= getSimpleTestFileName(true, true);
		ICompilationUnit newcu= pack.getCompilationUnit(newCuName);
		assertTrue(newCuName + " does not exist", newcu.exists());
		assertEquals("invalid renaming", getFileContents(getTestFileName(true, false)), newcu.getSource());
	}

	private void modifyInfos(List infos, ParameterInfo[] newParamInfos, int[] newIndices, String[] oldParamNames, String[] newParamNames, int[] permutation) {
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
				ParameterInfo infoI1= (ParameterInfo)infos.get(permutation[i]);
				infoI1.setNewName((String)newNameList.get(oldNameList.indexOf(infoI1.getOldName())));
				swap(infos, i, permutation[i]);
			}	
		}
	}

	private static void modifyInfos(List infos, String[] newOrder, String[] oldNames, String[] newNames) {
		int[] permutation= createPermutation(infos, newOrder);
		List swapped= new ArrayList(infos.size());
		if (oldNames == null || newNames == null){
			for (int i= 0; i < permutation.length; i++) {
				if (! swapped.contains(new Integer(i))){
					swapped.add(new Integer(permutation[i]));
					swap(infos, i, permutation[i]);
				}	
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
		ModifyParametersRefactoring ref= new ModifyParametersRefactoring(classA.getMethod("m", signature));
		modifyInfos(ref.getParameterInfos(), newOrder, null, null);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);		
		assertEquals("Severity:", expectedSeverity, result.getSeverity());
	}

	private void helperAddFail(String[] signature, ParameterInfo[] newParamInfos, int[] newIndices, int expectedSeverity) throws Exception{
		IType classA= getType(createCUfromTestFile(getPackageP(), false, false), "A");
		ModifyParametersRefactoring ref= new ModifyParametersRefactoring(classA.getMethod("m", signature));
		addInfos(ref.getParameterInfos(), newParamInfos, newIndices);
		RefactoringStatus result= performRefactoring(ref);
		assertNotNull("precondition was supposed to fail", result);		
		assertEquals("Severity:" + result.getFirstMessage(result.getSeverity()), expectedSeverity, result.getSeverity());
	}
	
	//------- tests 
	
	public void testFail0() throws Exception{
		helperFail(new String[]{"j", "i"}, new String[]{"I", "I"}, RefactoringStatus.ERROR);
	}
	
	public void testFail1() throws Exception{
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
//	public void test21() throws Exception{
//		helper1(new String[]{"b", "a"}, new String[]{"I", "I"});
//	}
//	public void test22() throws Exception{
//		helper1(new String[]{"b", "a"}, new String[]{"I", "I"});
//	}
//	public void test23() throws Exception{
//		helper1(new String[]{"b", "a"}, new String[]{"I", "I"});
//	}
//	public void test24() throws Exception{
//		helper1(new String[]{"b", "a"}, new String[]{"I", "I"});
//	}
//	public void test25() throws Exception{
//		helper1(new String[]{"b", "a"}, new String[]{"I", "I"});
//	}

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
		helperAddReorderRename(signature, newParamInfo, newIndices, oldParamNames, newParamNames, permutation);
	}	
}

