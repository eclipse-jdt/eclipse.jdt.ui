package org.eclipse.jdt.ui.tests.refactoring;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpMethodRefactoring;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class PullUpMethodsTests extends RefactoringTest {

	private static final Class clazz= PullUpMethodsTests.class;
	
	private static final String REFACTORING_PATH= "PullUpMethods/";

	public PullUpMethodsTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	//-------------------
	private static IMethod[] getMethods(IType type, String[] names, String[][] signatures) throws JavaModelException{
		Set methods= new HashSet();
		for (int i = 0; i < names.length; i++) {
			IMethod method= type.getMethod(names[i], signatures[i]);
			if (method.exists())
				methods.add(method);
		}
		return (IMethod[]) methods.toArray(new IMethod[methods.size()]);	
	}
	
	private static PullUpMethodRefactoring createRefactoring(IMethod[] methods){
		return new PullUpMethodRefactoring(methods, JavaPreferencesSettings.getCodeGenerationSettings());
	}	
	
	private void helper1(String[] methodNames, String[][] signatures, boolean deleteAllInSourceType, boolean deleteAllMatchingMethods) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);
			PullUpMethodRefactoring ref= createRefactoring(methods);
			if (deleteAllInSourceType)
				ref.setMethodsToDelete(methods);
			if (deleteAllMatchingMethods)
				ref.setMethodsToDelete(ref.getMatchingMethods());
		
			RefactoringStatus result= performRefactoring(ref);
			assertEquals("precondition was supposed to pass", null, result);
			
			String expected= getFileContents(getOutputTestFileName("A"));
			String actual= cu.getSource();
			//assertEquals("incorrect lengths", expected.length(), actual.length());
			assertEquals("incorrect modification", expected, actual);
		} finally{
			cu.delete(false, null);
		}	
	}
	
	private void helper2(String[] methodNames, String[][] signatures, boolean deleteAllInSourceType, boolean deleteAllMatchingMethods) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);
			PullUpMethodRefactoring ref= createRefactoring(methods);
			if (deleteAllInSourceType)
				ref.setMethodsToDelete(methods);
			if (deleteAllMatchingMethods)
				ref.setMethodsToDelete(ref.getMatchingMethods());
		
			RefactoringStatus result= performRefactoring(ref);
			assertTrue("precondition was supposed to fail", result != null && ! result.isOK());
		} finally{
			cu.delete(false, null);
		}		
	}
	
	private void helper3(String[] methodNames, String[][] signatures, boolean deleteAllInSourceType, boolean deleteAllMatchingMethods) throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");
		try{
			IType type= getType(cuB, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);
			PullUpMethodRefactoring ref= createRefactoring(methods);
			if (deleteAllInSourceType)
				ref.setMethodsToDelete(methods);
			if (deleteAllMatchingMethods)
				ref.setMethodsToDelete(ref.getMatchingMethods());
			
			RefactoringStatus result= performRefactoring(ref);
			assertTrue("precondition was supposed to fail", result != null && ! result.isOK());
		} finally{
			cuA.delete(false, null);
			cuB.delete(false, null);
		}				
	}
	
	//------------------ tests -------------
	
	public void test0() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
	
	public void test1() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}

	public void test2() throws Exception{
		helper1(new String[]{"mmm", "n"}, new String[][]{new String[0], new String[0]}, true, false);
	}

	public void test3() throws Exception{
		helper1(new String[]{"mmm", "n"}, new String[][]{new String[0], new String[0]}, true, true);
	}

	public void test4() throws Exception{
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");
		
		try{
			String[] methodNames= new String[]{"m"};
			String[][] signatures= new String[][]{new String[]{"QList;"}};
			
			IType type= getType(cuB, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);
			PullUpMethodRefactoring ref= createRefactoring(methods);
			ref.setMethodsToDelete(ref.getMatchingMethods());
		
			RefactoringStatus result= performRefactoring(ref);
			assertEquals("precondition was supposed to pass", null, result);
			
			assertEquals("incorrect modification in A", getFileContents(getOutputTestFileName("A")), cuA.getSource());		
			assertEquals("incorrect modification in B", getFileContents(getOutputTestFileName("B")), cuB.getSource());		
		} finally{
			cuA.delete(false, null);
			cuB.delete(false, null);
		}					
	}

	public void test5() throws Exception{
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");
		
		try{
			String[] methodNames= new String[]{"m"};
			String[][] signatures= new String[][]{new String[0]};
			
			IType type= getType(cuB, "B");
			IMethod[] methods= getMethods(type, methodNames, signatures);
			PullUpMethodRefactoring ref= createRefactoring(methods);
			ref.setMethodsToDelete(ref.getMatchingMethods());
		
			RefactoringStatus result= performRefactoring(ref);
			assertEquals("precondition was supposed to pass", null, result);
			
			assertEquals("incorrect modification in A", getFileContents(getOutputTestFileName("A")), cuA.getSource());
			assertEquals("incorrect modification in B", getFileContents(getOutputTestFileName("B")), cuB.getSource());		
		} finally{
			cuA.delete(false, null);
			cuB.delete(false, null);
		}		
	}

	public void test6() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}

	public void test7() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
	
	public void test8() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
	
	public void test9() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}

	public void test10() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
	
	public void test11() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
	
	public void test12() throws Exception{
		printTestDisabledMessage("bug#6779 searchDeclarationsOfReferencedTyped - missing exception  types");
//		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
//		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");
//
//		try{
//			String[] methodNames= new String[]{"m"};
//			String[][] signatures= new String[][]{new String[0]};
//			
//			IType type= getType(cuB, "B");
//			IMethod[] methods= getMethods(type, methodNames, signatures);
//			PullUpMethodRefactoring ref= createRefactoring(methods);
//			ref.setMethodsToDelete(ref.getMatchingMethods());
//		
//			RefactoringStatus result= performRefactoring(ref);
//			assertEquals("precondition was supposed to pass", null, result);
//			
//			assertEquals("incorrect modification in A", getFileContents(getOutputTestFileName("A")), cuA.getSource());		
//			assertEquals("incorrect modification in B", getFileContents(getOutputTestFileName("B")), cuB.getSource());		
//		} finally{
//			cuA.delete(false, null);
//			cuB.delete(false, null);
//		}							
	}
	
	public void test13() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
	
	public void test14() throws Exception{
		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
	
	public void test15() throws Exception{
		printTestDisabledMessage("must fix - incorrect error");
//		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
	
	public void test16() throws Exception{
		printTestDisabledMessage("must fix - incorrect error");
//		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
	
	public void testFail0() throws Exception{
//		printTestDisabledMessage("6538: searchDeclarationsOf* incorrect");
		helper2(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
	
	public void testFail1() throws Exception{
		printTestDisabledMessage("overloading - current limitation");
//		helper2(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
	
	public void testFail2() throws Exception{
//		printTestDisabledMessage("6538: searchDeclarationsOf* incorrect");
		helper2(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}

	public void testFail3() throws Exception{
//		printTestDisabledMessage("6538: searchDeclarationsOf* incorrect");
		helper2(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}

	public void testFail4() throws Exception{
//		printTestDisabledMessage("6538: searchDeclarationsOf* incorrect");
		helper2(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}

	public void testFail5() throws Exception{
		helper2(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
	
	public void testFail6() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		
		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods);
	}
	
	public void testFail7() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods);
	}
	
	public void testFail8() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods);
	}
	
	public void testFail9() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods);
	}
	
	public void testFail10() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods);
	}

	public void testFail11() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods);
	}

	public void testFail12() throws Exception{
		printTestDisabledMessage("overloading - current limitation");
//		String[] methodNames= new String[]{"m"};
//		String[][] signatures= new String[][]{new String[0]};
//		boolean deleteAllInSourceType= true;
//		boolean deleteAllMatchingMethods= false;
//		helper3(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods);
	}
	
	public void testFail13() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		helper2(methodNames, signatures, deleteAllInSourceType, deleteAllMatchingMethods);
	}
	
}

