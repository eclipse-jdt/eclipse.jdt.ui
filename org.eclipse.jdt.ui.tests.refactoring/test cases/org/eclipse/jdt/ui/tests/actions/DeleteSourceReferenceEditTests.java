package org.eclipse.jdt.ui.tests.actions;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

public class DeleteSourceReferenceEditTests extends RefactoringTest {

	private static final Class clazz= DeleteSourceReferenceEditTests.class;
	private static final String REFACTORING_PATH= "DeleteSourceReferenceEdit/";
	
	public DeleteSourceReferenceEditTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	private static ICompilationUnit getCompilationUnit(Object o){
		if (o instanceof IMember)
			return ((IMember)o).getCompilationUnit();
		else if (o instanceof IImportDeclaration){
				IImportDeclaration im= (IImportDeclaration)o;
				return (ICompilationUnit)im.getParent().getParent();
		} else		
			return (ICompilationUnit)((IJavaElement)o).getParent();
	}
	
	private static TextEdit createDeleteEdit(ISourceReference ref) throws JavaModelException{
		return new DeleteSourceReferenceEdit(ref, getCompilationUnit(ref));
	}
	
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(input), getFileContents(getTestFileName(input)));
	}
	
	private String getTestFileName(boolean input){
		return TEST_PATH_PREFIX + getRefactoringPath() + getSimpleTestFileName(input);
	}
	
	private String getSimpleTestFileName(boolean input){
		String fileName = "A_" + getName();
		fileName += input ? "_in": "_out";
		return fileName + ".java"; 
	}

	private void helper(ICompilationUnit cu, ISourceReference[] elems) throws Exception {
		IFile file= (IFile)cu.getUnderlyingResource();
		TextBuffer tb= TextBuffer.acquire(file);
		try{
			TextBufferEditor tbe= new TextBufferEditor(tb);
			for (int i= 0; i < elems.length; i++) {
				ISourceReference iSourceReference= elems[i];
				tbe.add(createDeleteEdit(iSourceReference));	
			}
			assertTrue("cannot perform", tbe.canPerformEdits().isOK());
			
			tbe.performEdits(new NullProgressMonitor());	
			TextBuffer.commitChanges(tb, false, new NullProgressMonitor());
		} finally{	
			if (tb != null)
				TextBuffer.release(tb);
		}	
		String expected= getFileContents(getTestFileName(false));
		assertEquals("incorrect modification", expected, tb.getContent());	
	}
	
	private void methodHelper(String[] methodNames, String[][] signatures) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		IType classA= getType(cu, "A");
		
		ISourceReference[] elems= new ISourceReference[methodNames.length];
		for (int i= 0; i < methodNames.length; i++) {
			elems[i]= classA.getMethod(methodNames[i], signatures[i]);
		}
		helper(cu, elems);	
	}
	
	private void fieldHelper(String[] fieldNames) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		IType classA= getType(cu, "A");
		
		ISourceReference[] elems= new ISourceReference[fieldNames.length];
		for (int i= 0; i < fieldNames.length; i++) {
			elems[i]= classA.getField(fieldNames[i]);
		}
		helper(cu, elems);	
	}
	
	//---- tests 
	
	public void test0() throws Exception{
		methodHelper(new String[]{"m"}, new String[][]{new String[0]});
	}
	
	public void test1() throws Exception{
		methodHelper(new String[]{"m"}, new String[][]{new String[0]});
	}	
	
	public void test2() throws Exception{
		methodHelper(new String[]{"m", "n"}, new String[][]{new String[0], new String[0]});
	}	

	public void test3() throws Exception{
		methodHelper(new String[]{"m"}, new String[][]{new String[0]});
	}	

	public void test4() throws Exception{
		methodHelper(new String[]{"m"}, new String[][]{new String[0]});
	}	

	public void test5() throws Exception{
		methodHelper(new String[]{"m"}, new String[][]{new String[0]});
	}
	
	public void test6() throws Exception{
		methodHelper(new String[]{"m"}, new String[][]{new String[0]});
	}

	public void test7() throws Exception{
		methodHelper(new String[]{"m"}, new String[][]{new String[0]});
	}
	
	public void testField0() throws Exception{
		fieldHelper(new String[]{"f"});
	}
	
	public void testField1() throws Exception{
		fieldHelper(new String[]{"f"});
	}	
	
	public void testField2() throws Exception{
		fieldHelper(new String[]{"f", "ff"});
	}	

	public void testField3() throws Exception{
		fieldHelper(new String[]{"f"});
	}	

	public void testField4() throws Exception{
		fieldHelper(new String[]{"f"});
	}	
}

