/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.actions;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.ui.tests.refactoring.ExtractMethodTests;
import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import org.eclipse.jdt.internal.corext.refactoring.SourceRange;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.ui.refactoring.actions.structureselection.StructureSelectionAction;public class StructureSelectionActionTests extends RefactoringTest{
	
	private static final Class clazz= StructureSelectionActionTests.class;
	private static final String REFACTORING_PATH= "StructureSelectionAction/";
	
	public StructureSelectionActionTests(String name){
		super(name);
	}
	
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(clazz));
	}
	
	private String getSimpleTestFileName(boolean input){
		String fileName = "A_" + getName();
		fileName += input ? "": "_out";
		fileName +=  input ? ".java": ".txt"; 
		return fileName;
	}
	
	private String getTestFileName(boolean input){
		return TEST_PATH_PREFIX + getRefactoringPath() + getSimpleTestFileName(input);
	}
	
	private String getPassingTestFileName(boolean input){
		return getTestFileName(input);
	}
	
	//------------
	protected ICompilationUnit createCUfromTestFile(IPackageFragment pack, boolean input) throws Exception {
		return createCU(pack, getSimpleTestFileName(input), getFileContents(getTestFileName(input)));
	}
	
	private ISourceRange getSelection(ICompilationUnit cu) throws Exception{
		String source= cu.getSource();
		int offset= source.indexOf(ExtractMethodTests.SQUARE_BRACKET_OPEN);
		int end= source.indexOf(ExtractMethodTests.SQUARE_BRACKET_CLOSE);
		return new SourceRange(offset, end - offset);
	}
	
	private ISourceRange getSelection(ICompilationUnit cu, int startLine, int startColumn, int endLine, int endColumn) throws Exception{
		int offset= getOffset(cu, startLine, startColumn);
		int end= getOffset(cu, endLine, endColumn);
		return new SourceRange(offset, end - offset);
	}
	
	private void helper1() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		ISourceRange selection= getSelection(cu);

		ISourceRange newRange= new StructureSelectionAction().getNewSelectionRange(selection, cu);
		
		String expected= getFileContents(getTestFileName(false));
		String actual= cu.getSource().substring(newRange.getOffset(), newRange.getOffset() + newRange.getLength());
		assertEquals("selection incorrect length", expected.length(), actual.length());
		assertEquals("selection incorrect", expected, actual);
	}	
	
	private void helper1(int startLine, int startColumn, int endLine, int endColumn) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		ISourceRange selection= getSelection(cu, startLine, startColumn, endLine, endColumn);

		ISourceRange newRange= new StructureSelectionAction().getNewSelectionRange(selection, cu);
		
		String expected= getFileContents(getTestFileName(false));
		String actual= cu.getSource().substring(newRange.getOffset(), newRange.getOffset() + newRange.getLength());
		assertEquals("selection incorrect length", expected.length(), actual.length());
		assertEquals("selection incorrect", expected, actual);
	}	
		
	private void helperZeroLength(int line, int column) throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		ISourceRange selection= new SourceRange(getOffset(cu, line, column), 1);
			
		//DebugUtils.dump(name() + ":<" + cu.getSource().substring(selection.getOffset()) + "/>");
		
		ISourceRange newRange= new StructureSelectionAction().getNewSelectionRange(selection, cu);
		
		String expected= getFileContents(getTestFileName(false));
		String actual= cu.getSource().substring(newRange.getOffset(), newRange.getOffset() + newRange.getLength());
		assertEquals("selection incorrect length", expected.length(), actual.length());
		assertEquals("selection incorrect", expected, actual);		
	}
	
	private void offsetTest(int line, int column, int expected) throws Exception{
		String filePath= TEST_PATH_PREFIX + getRefactoringPath() + "OffsetTest.java";
		ICompilationUnit cu= createCU(getPackageP(), "OffsetTest.java", getFileContents(filePath));
		assertEquals("incorrect offset", expected, getOffset(cu, line, column));
	}
	
	private static int getOffset(ICompilationUnit cu, int line, int column) throws Exception{
		//String s= cu.getSource();
		TextBuffer tb= TextBuffer.create(cu.getSource());
		int r= tb.getLineInformation(line - 1).getOffset();
		
		int lineTabCount= calculateTabCountInLine(tb.getLineContent(line - 1), column);		
		r += (column - 1) - (lineTabCount * getTabWidth()) + lineTabCount;
		return r ;
	}
	
	private static final int getTabWidth(){
		return 4;
	}
	
	private static int calculateTabCountInLine(String lineSource, int lastCharOffset){
		int acc= 0;
		int charCount= 0;
		for(int i= 0; charCount < lastCharOffset - 1; i++){
			if ('\t' == lineSource.charAt(i)){
				acc++;
				charCount += getTabWidth();
			}	else
				charCount += 1;
		}
		return acc;
	}

	// ---- tests --- 
	
	public void test0() throws Exception{
		helper1();
	}
	
	public void test1() throws Exception{
		helper1();
	}

	public void test2() throws Exception{
		helper1();
	}

	public void test3() throws Exception{
		helper1();
	}

	public void test4() throws Exception{
		helper1();
	}
	
	public void test5() throws Exception{
		helper1();
	}
	
	public void test6() throws Exception{
		helper1();
	}

	public void test7() throws Exception{
		helper1();
	}

	public void test8() throws Exception{
		helper1();
	}

	public void test9() throws Exception{
		helper1();
	}
	
	public void test10() throws Exception{
		helper1(4, 18, 4, 21);
	}

	public void test11() throws Exception{
		helper1(4, 20, 4, 21);
	}

	public void test12() throws Exception{
		helper1(4, 16, 4, 19);
	}
	
	public void test13() throws Exception{
		helper1(4, 13, 4, 16);
	}
	
	public void test14() throws Exception{
		helper1(4, 16, 4, 21);
	}
	
	public void test15() throws Exception{
		helper1(3, 10, 3, 11);
	}
	
	public void test16() throws Exception{
		helper1(3, 16, 3, 17);
	}
	
	public void test17() throws Exception{
		helper1(3, 5, 7, 6);
	}
	
	public void test18() throws Exception{
		helper1(3, 5, 4, 6);
	}
	
	public void test19() throws Exception{
		helper1(7, 14, 7, 16);
	}
	
	public void test20() throws Exception{
		helper1(4, 18, 4, 19);
	}
	
	public void testZeroLength0() throws Exception{
		//printTestDisabledMessage("");
		helperZeroLength(4, 20);
	}
	
	public void testWholeCu() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP(), true);
		ISourceRange selection= cu.getSourceRange();

		ISourceRange newRange= new StructureSelectionAction().getNewSelectionRange(selection, cu);
		
		String expected= getFileContents(getTestFileName(false));
		String actual= cu.getSource().substring(newRange.getOffset(), newRange.getOffset() + newRange.getLength());
		assertEquals("selection incorrect length", expected.length(), actual.length());
		assertEquals("selection incorrect", expected, actual);
	}

	//--- offset calculation tests
	
	public void testOffset0() throws Exception{
		offsetTest(4, 20, 47);
	}	
	
	public void testOffset1() throws Exception{
		offsetTest(5, 9, 53);
	}	
	
	public void testOffset2() throws Exception{
		offsetTest(7, 13, 81);
	}	
	
	public void testTabCount0(){
		int t= calculateTabCountInLine("\t\t1", 9);
		assertEquals(2, t);
	}
	
	public void testTabCount1(){
		int t= calculateTabCountInLine("\t\tint i= 1 + 1;", 20);
		assertEquals(2, t);
	}
	
	public void testTabCount2(){
		int t= calculateTabCountInLine("\t\t\treturn;", 13);
		assertEquals(3, t);
	}
	
	public void testTabCount3(){
		int t= calculateTabCountInLine("\tvoid m(){m();", 18);
		assertEquals(1, t);
	}
	
}