package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class PullUpTests extends RefactoringTest {

	private static final Class clazz= PullUpTests.class;
	
	private static final String REFACTORING_PATH= "PullUp/";

	public PullUpTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	//-------------------
	
	private static PullUpRefactoring createRefactoring(IMember[] methods){
		return new PullUpRefactoring(methods, JavaPreferencesSettings.getCodeGenerationSettings());
	}
	
	private void fieldMethodHelper1(String[] fieldNames, String[] methodNames, String[][] signatures, boolean deleteAllInSourceType, boolean deleteAllMatchingMethods) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "B");
			IField[] fields= TestUtil.getFields(type, fieldNames);
			IMethod[] methods= TestUtil.getMethods(type, methodNames, signatures);

			PullUpRefactoring ref= createRefactoring(TestUtil.merge(methods, fields));

			if (deleteAllInSourceType)
				ref.setMethodsToDelete(methods);
			if (deleteAllMatchingMethods)
				ref.setMethodsToDelete(getMethods(ref.getMatchingElements(new NullProgressMonitor())));
						
		
			RefactoringStatus result= performRefactoring(ref);
			assertEquals("precondition was supposed to pass", null, result);
			
			String expected= getFileContents(getOutputTestFileName("A"));
			String actual= cu.getSource();
			assertEquals("incorrect modification", expected, actual);
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}	
	}

	private void fieldHelper1(String[] fieldNames, boolean deleteAllInSourceType, boolean deleteAllMatchingFields) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "B");
			IField[] fields= TestUtil.getFields(type, fieldNames);
			
			PullUpRefactoring ref= createRefactoring(fields);
		
			RefactoringStatus result= performRefactoring(ref);
			assertEquals("precondition was supposed to pass", null, result);
			
			String expected= getFileContents(getOutputTestFileName("A"));
			String actual= cu.getSource();
			assertEquals("incorrect modification", expected, actual);
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}	
	}
	
	private void fieldHelper2(String[] fieldNames) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "B");
			IField[] fields= TestUtil.getFields(type, fieldNames);
			PullUpRefactoring ref= createRefactoring(fields);
		
			RefactoringStatus result= performRefactoring(ref);
			assertTrue("precondition was supposed to fail", result != null && ! result.isOK());
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}		
	}
	
	private static IMethod[] getMethods(IMember[] members){
		List l= Arrays.asList(JavaElementUtil.getElementsOfType(members, IJavaElement.METHOD));
		return (IMethod[]) l.toArray(new IMethod[l.size()]);
	}
	
	private void helper1(String[] methodNames, String[][] signatures, boolean deleteAllInSourceType, boolean deleteAllMatchingMethods) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "B");
			IMethod[] methods= TestUtil.getMethods(type, methodNames, signatures);
			PullUpRefactoring ref= createRefactoring(methods);
			if (deleteAllInSourceType)
				ref.setMethodsToDelete(methods);
			if (deleteAllMatchingMethods)
				ref.setMethodsToDelete(getMethods(ref.getMatchingElements(new NullProgressMonitor())));
		
			RefactoringStatus result= performRefactoring(ref);
			assertEquals("precondition was supposed to pass", null, result);
			
			String expected= getFileContents(getOutputTestFileName("A"));
			String actual= cu.getSource();
			//assertEquals("incorrect lengths", expected.length(), actual.length());
			assertEquals("incorrect modification", expected, actual);
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}	
	}
	
	private void helper2(String[] methodNames, String[][] signatures, boolean deleteAllInSourceType, boolean deleteAllMatchingMethods) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "B");
			IMethod[] methods= TestUtil.getMethods(type, methodNames, signatures);
			PullUpRefactoring ref= createRefactoring(methods);
			if (deleteAllInSourceType)
				ref.setMethodsToDelete(methods);
			if (deleteAllMatchingMethods)
				ref.setMethodsToDelete(getMethods(ref.getMatchingElements(new NullProgressMonitor())));
		
			RefactoringStatus result= performRefactoring(ref);
			assertTrue("precondition was supposed to fail", result != null && ! result.isOK());
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}		
	}
	
	private void helper3(String[] methodNames, String[][] signatures, boolean deleteAllInSourceType, boolean deleteAllMatchingMethods) throws Exception {
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");
		try{
			IType type= getType(cuB, "B");
			IMethod[] methods= TestUtil.getMethods(type, methodNames, signatures);
			PullUpRefactoring ref= createRefactoring(methods);
			if (deleteAllInSourceType)
				ref.setMethodsToDelete(methods);
			if (deleteAllMatchingMethods)
				ref.setMethodsToDelete(getMethods(ref.getMatchingElements(new NullProgressMonitor())));
			
			RefactoringStatus result= performRefactoring(ref);
			assertTrue("precondition was supposed to fail", result != null && ! result.isOK());
		} finally{
			performDummySearch();
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
			IMethod[] methods= TestUtil.getMethods(type, methodNames, signatures);
			PullUpRefactoring ref= createRefactoring(methods);
			ref.setMethodsToDelete(getMethods(ref.getMatchingElements(new NullProgressMonitor())));
		
			RefactoringStatus result= performRefactoring(ref);
			assertEquals("precondition was supposed to pass", null, result);
			
			assertEquals("incorrect modification in A", getFileContents(getOutputTestFileName("A")), cuA.getSource());		
			assertEquals("incorrect modification in B", getFileContents(getOutputTestFileName("B")), cuB.getSource());		
		} finally{
			performDummySearch();
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
			IMethod[] methods= TestUtil.getMethods(type, methodNames, signatures);
			PullUpRefactoring ref= createRefactoring(methods);
			ref.setMethodsToDelete(getMethods(ref.getMatchingElements(new NullProgressMonitor())));
		
			RefactoringStatus result= performRefactoring(ref);
			assertEquals("precondition was supposed to pass", null, result);
			
			assertEquals("incorrect modification in A", getFileContents(getOutputTestFileName("A")), cuA.getSource());
			assertEquals("incorrect modification in B", getFileContents(getOutputTestFileName("B")), cuB.getSource());		
		} finally{
			performDummySearch();
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
		//printTestDisabledMessage("bug#6779 searchDeclarationsOfReferencedTyped - missing exception  types");
		ICompilationUnit cuA= createCUfromTestFile(getPackageP(), "A");
		ICompilationUnit cuB= createCUfromTestFile(getPackageP(), "B");

		try{
			String[] methodNames= new String[]{"m"};
			String[][] signatures= new String[][]{new String[0]};
			
			IType type= getType(cuB, "B");
			IMethod[] methods= TestUtil.getMethods(type, methodNames, signatures);
			PullUpRefactoring ref= createRefactoring(methods);
			//ref.setMethodsToDelete(ref.getMatchingMethods());
			ref.setMethodsToDelete(getMethods(ref.getMatchingElements(new NullProgressMonitor())));
		
			RefactoringStatus result= performRefactoring(ref);
			assertEquals("precondition was supposed to pass", null, result);
			
			assertEquals("incorrect modification in A", getFileContents(getOutputTestFileName("A")), cuA.getSource());		
			assertEquals("incorrect modification in B", getFileContents(getOutputTestFileName("B")), cuB.getSource());		
		} finally{
			performDummySearch();
			cuA.delete(false, null);
			cuB.delete(false, null);
		}							
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
	
	public void test17() throws Exception{
		printTestDisabledMessage("must fix - incorrect error with static method access");
//		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}

	public void test18() throws Exception{
		printTestDisabledMessage("must fix - incorrect error with static field access");
//		helper1(new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
	
	public void test19() throws Exception{
//		printTestDisabledMessage("bug 18438");
		printTestDisabledMessage("bug 23324 ");
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
	
	public void testFail14() throws Exception{
		String[] methodNames= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		boolean deleteAllInSourceType= true;
		boolean deleteAllMatchingMethods= false;
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), "A");
		try{
			IType type= getType(cu, "A");
			IMethod[] methods= TestUtil.getMethods(type, methodNames, signatures);
			IMember[] members= TestUtil.merge(methods, new IMember[]{type.getType("Quux")});
			PullUpRefactoring ref= createRefactoring(members);
			if (deleteAllInSourceType)
				ref.setMethodsToDelete(methods);
			if (deleteAllMatchingMethods)
				ref.setMethodsToDelete(getMethods(ref.getMatchingElements(new NullProgressMonitor())));
		
			RefactoringStatus result= performRefactoring(ref);
			assertTrue("precondition was supposed to fail", result != null && ! result.isOK());
		} finally{
			performDummySearch();
			cu.delete(false, null);
		}		

	}

	//----------------------------------------------------------
	public void testField0() throws Exception{
		fieldHelper1(new String[]{"i"}, true, false);
	}
	
	public void testFieldFail0() throws Exception{
		fieldHelper2(new String[]{"x"});
	}
	
	public void testFieldFail1() throws Exception{
		fieldHelper2(new String[]{"x"});
	}

	//---------------------------------------------------------
	public void testFieldMethod0() throws Exception{
		printTestDisabledMessage("bug 23324 ");
//		fieldMethodHelper1(new String[]{"f"}, new String[]{"m"}, new String[][]{new String[0]}, true, false);
	}
}

