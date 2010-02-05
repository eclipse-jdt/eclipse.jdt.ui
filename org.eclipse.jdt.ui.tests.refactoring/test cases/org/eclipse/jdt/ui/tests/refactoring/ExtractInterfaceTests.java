/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.ProcessorBasedRefactoring;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractInterfaceProcessor;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class ExtractInterfaceTests extends RefactoringTest {

	private static final Class clazz= ExtractInterfaceTests.class;
	private static final String REFACTORING_PATH= "ExtractInterface/";
    private Hashtable fOldOptions;
    private boolean fGenerateAnnotations= false;

	public ExtractInterfaceTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringTestSetup(someTest);
	}

	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	protected void setUp() throws Exception {
		super.setUp();
		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID,
			"${package_declaration}" +
				System.getProperty("line.separator", "\n") +
			"${"+ CodeTemplateContextType.TYPE_COMMENT+"}" +
			System.getProperty("line.separator", "\n") +
			"${type_declaration}", null);

		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "/** typecomment template*/", null);

		fOldOptions= JavaCore.getOptions();

	    Hashtable options= TestOptions.getDefaultOptions();
	    options.put(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR, DefaultCodeFormatterConstants.TRUE);
	    options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
	    options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);

	    JavaCore.setOptions(options);
	}

    protected void tearDown() throws Exception {
    	super.tearDown();
    	JavaCore.setOptions(fOldOptions);
    	fOldOptions= null;
    }

	private static String getTopLevelTypeName(String typeQualifiedTyperName){
		int dotIndex= typeQualifiedTyperName.indexOf('.');
		if (dotIndex == -1)
			return typeQualifiedTyperName;
		return typeQualifiedTyperName.substring(0, dotIndex);
	}

	private IType getClassFromTestFile(IPackageFragment pack, String className) throws Exception{
		return getType(createCUfromTestFile(pack, getTopLevelTypeName(className)), className);
	}

	private void validatePassingTest(String className, String[] cuNames, String newInterfaceName, boolean replaceOccurrences, String[] extractedMethodNames, String[][] extractedSignatures, String[] extractedFieldNames) throws Exception {
		IType clas= getClassFromTestFile(getPackageP(), className);

		ExtractInterfaceProcessor processor= new ExtractInterfaceProcessor(clas, JavaPreferencesSettings.getCodeGenerationSettings(clas.getJavaProject()));
		Refactoring ref= new ProcessorBasedRefactoring(processor);

		processor.setTypeName(newInterfaceName);
		assertEquals("interface name should be accepted", RefactoringStatus.OK, processor.checkTypeName(newInterfaceName).getSeverity());

		ICompilationUnit[] cus= new ICompilationUnit[cuNames.length];
		for (int i= 0; i < cuNames.length; i++) {
			if (cuNames[i].equals(clas.getCompilationUnit().findPrimaryType().getElementName()))
				cus[i]= clas.getCompilationUnit();
			else
				cus[i]= createCUfromTestFile(clas.getPackageFragment(), cuNames[i]);
		}
		processor.setReplace(replaceOccurrences);
		processor.setAnnotations(fGenerateAnnotations);
		IMethod[] extractedMethods= getMethods(clas, extractedMethodNames, extractedSignatures);
	    IField[] extractedFields= getFields(clas, extractedFieldNames);
		processor.setExtractedMembers(merge(extractedMethods, extractedFields));
		assertEquals("was supposed to pass", null, performRefactoring(ref));

		for (int i= 0; i < cus.length; i++) {
			String expected= getFileContents(getOutputTestFileName(cuNames[i]));
			String actual= cus[i].getSource();
			assertEqualLines("(" + cus[i].getElementName() +")", expected, actual);
		}

		ICompilationUnit interfaceCu= clas.getPackageFragment().getCompilationUnit(newInterfaceName + ".java");
		assertEqualLines("(interface cu)", getFileContents(getOutputTestFileName(newInterfaceName)), interfaceCu.getSource());
	}

	private void validatePassingTest(String className, String newInterfaceName, boolean extractAll, boolean replaceOccurrences) throws Exception {
		IType clas= getClassFromTestFile(getPackageP(), className);
		ICompilationUnit cu= clas.getCompilationUnit();
		IPackageFragment pack= (IPackageFragment)cu.getParent();

		ExtractInterfaceProcessor processor= new ExtractInterfaceProcessor(clas, JavaPreferencesSettings.getCodeGenerationSettings(clas.getJavaProject()));
		Refactoring ref= new ProcessorBasedRefactoring(processor);

		processor.setTypeName(newInterfaceName);
		assertEquals("interface name should be accepted", RefactoringStatus.OK, processor.checkTypeName(newInterfaceName).getSeverity());

		if (extractAll)
			processor.setExtractedMembers(processor.getExtractableMembers());
		processor.setReplace(replaceOccurrences);
		processor.setAnnotations(fGenerateAnnotations);
		assertEquals("was supposed to pass", null, performRefactoring(ref));
		assertEqualLines("incorrect changes in " + className,
			getFileContents(getOutputTestFileName(className)),
			cu.getSource());

		ICompilationUnit interfaceCu= pack.getCompilationUnit(newInterfaceName + ".java");
		assertEqualLines("incorrect interface created",
			getFileContents(getOutputTestFileName(newInterfaceName)),
			interfaceCu.getSource());
	}

	private void validateFailingTest(String className, String newInterfaceName, boolean extractAll, int expectedSeverity) throws Exception {
		IType clas= getClassFromTestFile(getPackageP(), className);
		ExtractInterfaceProcessor processor= new ExtractInterfaceProcessor(clas, JavaPreferencesSettings.getCodeGenerationSettings(clas.getJavaProject()));
		Refactoring ref= new ProcessorBasedRefactoring(processor);
		processor.setTypeName(newInterfaceName);
		if (extractAll)
			processor.setExtractedMembers(processor.getExtractableMembers());
		assertTrue("was not supposed to pass", performRefactoring(ref) != null);
		assertEquals("was not supposed to fail with different severity", expectedSeverity, performRefactoring(ref).getSeverity());
	}

	private void standardPassingTest() throws Exception{
		String[] names= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, null);
	}
	//---------------tests ----------------------

	public void test0() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test1() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test2() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test3() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test4() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test5() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test6() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test7() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test8() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test9() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test10() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test11() throws Exception{
		validatePassingTest("A", "I", true, false);
	}

	public void test12() throws Exception{
		validatePassingTest("A", "I", true, true);
	}

	public void test13() throws Exception{
		validatePassingTest("A", "I", true, true);
	}

	public void test14() throws Exception{
		standardPassingTest();
	}

	public void test15() throws Exception{
		String[] names= new String[]{"m", "m1"};
		String[][] signatures= new String[][]{new String[0], new String[0]};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, null);
	}

	public void test16() throws Exception{
		standardPassingTest();
	}

	public void test17() throws Exception{
		standardPassingTest();
	}

	public void test18() throws Exception{
		standardPassingTest();
	}

	public void test19() throws Exception{
		standardPassingTest();
	}

	public void test20() throws Exception{
		String[] names= new String[]{"m", "m1"};
		String[][] signatures= new String[][]{new String[0], new String[0]};
		validatePassingTest("A", new String[]{"A"},"I", true, names, signatures, null);
	}

	public void test21() throws Exception{
		validatePassingTest("A", "I", true, true);
	}

	public void test22() throws Exception{
		validatePassingTest("A", "I", true, true);
	}

	public void test23() throws Exception{
		validatePassingTest("A", "I", true, true);
	}

	public void test24() throws Exception{
		standardPassingTest();
	}

	public void test25() throws Exception{
		validatePassingTest("A", "I", true, true);
	}

	public void test26() throws Exception{
		standardPassingTest();
	}

	public void test27() throws Exception{
		standardPassingTest();
	}

	public void test28() throws Exception{
		standardPassingTest();
	}

	public void test29() throws Exception{
		standardPassingTest();
	}

	public void test30() throws Exception{
		standardPassingTest();
	}

	public void test31() throws Exception{
		standardPassingTest();
	}

	public void test32() throws Exception{
		standardPassingTest();
	}

	public void test33() throws Exception{
		standardPassingTest();
	}

	public void test34() throws Exception{
		standardPassingTest();
	}

	public void test35() throws Exception{
		standardPassingTest();
	}

	public void test36() throws Exception{
		standardPassingTest();
	}

	public void test37() throws Exception{
		standardPassingTest();
	}

	public void test38() throws Exception{
		standardPassingTest();
	}

	public void test39() throws Exception{
		standardPassingTest();
	}

	public void test40() throws Exception{
		standardPassingTest();
	}

	public void test41() throws Exception{
		standardPassingTest();
	}

	public void test42() throws Exception{
		standardPassingTest();
	}

	public void test43() throws Exception{
		standardPassingTest();
	}

	public void test44() throws Exception{
		standardPassingTest();
	}

	public void test45() throws Exception{
		standardPassingTest();
	}

	public void test46() throws Exception{
		standardPassingTest();
	}

	public void test47() throws Exception{
		standardPassingTest();
	}

	public void test48() throws Exception{
		standardPassingTest();
	}

	public void test49() throws Exception{
		standardPassingTest();
	}

	public void test50() throws Exception{
		standardPassingTest();
	}

	public void test51() throws Exception{
		standardPassingTest();
	}

	public void test52() throws Exception{
		standardPassingTest();
	}

	public void test53() throws Exception{
		standardPassingTest();
	}

	public void test54() throws Exception{
		String[] names= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A", "A1"}, "I", true, names, signatures, null);
	}

	public void test55() throws Exception{
		String[] names= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A", "A1"}, "I", true, names, signatures, null);
	}

	public void test56() throws Exception{
		String[] names= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A", "A1"}, "I", true, names, signatures, null);
	}

	public void test57() throws Exception{
		String[] names= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A", "A1"}, "I", true, names, signatures, null);
	}

	public void test58() throws Exception{
		standardPassingTest();
	}

	public void test59() throws Exception{
//		printTestDisabledMessage("bug 22946 ");
		standardPassingTest();
	}

	public void test60() throws Exception{
		standardPassingTest();
	}

	public void test61() throws Exception{
		standardPassingTest();
	}

	public void test62() throws Exception{
		standardPassingTest();
	}

	public void test63() throws Exception{
		standardPassingTest();
	}

	public void test64() throws Exception{
//		printTestDisabledMessage("test for 23105");
		String[] names= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A", "Inter"}, "I", true, names, signatures, null);
	}

	public void test65() throws Exception{
//		printTestDisabledMessage("test for 23105");
		standardPassingTest();
	}

	public void test66() throws Exception{
		standardPassingTest();
	}

	public void test67() throws Exception{
//		printTestDisabledMessage("test for 23105");
		String[] names= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A", "Outer", "Inter"}, "I", true, names, signatures, null);
	}

	public void test68() throws Exception{
		String[] names= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A", "As"}, "I", true, names, signatures, null);
	}

	public void test69() throws Exception{
		String[] names= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A", "As"}, "I", true, names, signatures, null);
	}

	public void test70() throws Exception{
		standardPassingTest();
	}

	public void test71() throws Exception{
		String[] names= new String[]{"m"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A", "As"}, "I", true, names, signatures, null);
	}

	public void test72() throws Exception{
//		printTestDisabledMessage("bug 23705");
		String[] names= new String[]{"m"};
		String[][] signatures= new String[][]{new String[]{"QA;"}};
		validatePassingTest("A", new String[]{"A", "As"}, "I", true, names, signatures, null);
	}

	public void test73() throws Exception{
//		printTestDisabledMessage("bug 23953");
		String[] names= new String[]{"amount"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A", "B", "OldInterface"}, "I", true, names, signatures, null);
	}

	public void test74() throws Exception{
//		printTestDisabledMessage("bug 23953");
		String[] names= new String[]{"amount"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A", "B", "OldInterface"}, "I", true, names, signatures, null);
	}

	public void test75() throws Exception{
//		printTestDisabledMessage("bug 23953");
		String[] names= new String[]{"amount"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A", "B", "C"}, "I", true, names, signatures, null);
	}

	public void test76() throws Exception{
//		printTestDisabledMessage("bug 23953");
		String[] names= new String[]{"amount"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A", new String[]{"A", "B", "C"}, "I", true, names, signatures, null);
	}

	public void test77() throws Exception{
//		printTestDisabledMessage("Waiting for new type constraints infrastructure");
		String[] names= new String[]{"amount"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("A.Inner", new String[]{"A", "B"}, "I", true, names, signatures, null);
	}

	public void test78() throws Exception{
//		printTestDisabledMessage("bug 23705");
		String[] names= new String[]{"m"};
		String[][] signatures= new String[][]{new String[]{"QA;"}};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, null);
	}

    public void test79() throws Exception{
//		printTestDisabledMessage("bug 23697");
        String[] names= new String[]{"getFoo", "foo"};
        String[][] signatures= new String[][]{new String[0], new String[]{"QA;"}};
        validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, null);
    }

	public void test80() throws Exception{
//		printTestDisabledMessage("bug 33223");
		String[] names= new String[]{"f", "fz", "f1", "f1z", "f11", "f2"};
		String[][] signatures= new String[][]{new String[0], new String[0], new String[0], new String[0], new String[0], new String[0]};
		String[] fieldNames= {"I1", "I1z", "I2", "I2z", "I3", "I4"};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test81() throws Exception{
//		printTestDisabledMessage("bug 33878 extract interface: incorrect handling of arrays ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test82() throws Exception{
//		printTestDisabledMessage("bug 33878 extract interface: incorrect handling of arrays ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test83() throws Exception{
//		printTestDisabledMessage("bug 33878 extract interface: incorrect handling of arrays ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test84() throws Exception{
//		printTestDisabledMessage("bug 34931 Extract Interface does not update all references ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test85() throws Exception{
//		printTestDisabledMessage("bug 34931 Extract Interface does not update all references ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test86() throws Exception{
//		printTestDisabledMessage("bug 34931 Extract Interface does not update all references ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test87() throws Exception{
//		printTestDisabledMessage("bug 34931 Extract Interface does not update all references ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test88() throws Exception{
//		printTestDisabledMessage("bug 34931 Extract Interface does not update all references ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test89() throws Exception{
//		printTestDisabledMessage("bug 34931 Extract Interface does not update all references ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test90() throws Exception{
//		printTestDisabledMessage("bug 34931 Extract Interface does not update all references ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test91() throws Exception{
//		printTestDisabledMessage("bug 34931 Extract Interface does not update all references ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test92() throws Exception{
//		printTestDisabledMessage("bug 34931 Extract Interface does not update all references ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test93() throws Exception{
//		printTestDisabledMessage("bug 34931 Extract Interface does not update all references ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test94() throws Exception{
//		printTestDisabledMessage("bug 34931 Extract Interface does not update all references ");
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test95() throws Exception{
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test96() throws Exception{
		String[] names= {};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test97() throws Exception{
		//printTestDisabledMessage("bug 40373");
		String[] names= {"foo"};
		String[][] signatures= {{}};
		String[] fieldNames= {};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void test98() throws Exception{
		//test for 41464
		String[] names= new String[]{"foo"};
		String[][] signatures= new String[][]{new String[0]};
		validatePassingTest("Foo", new String[]{"Foo", "Bar"}, "IFoo", true, names, signatures, null);
	}

	public void test99() throws Exception{
		String[] names= new String[]{};
		String[][] signatures= new String[][]{};
		validatePassingTest("C", new String[]{"A", "B", "C"}, "I", true, names, signatures, null);
	}

	public void test100() throws Exception{
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=47785
		validatePassingTest("A", "I", true, false);
	}

	public void test101() throws Exception{
		String[] names= new String[]{};
		String[][] signatures= new String[][]{};
		validatePassingTest("C", new String[]{"A", "B", "C"}, "I", true, names, signatures, null);
	}

	public void test102() throws Exception{
		String[] names= new String[]{};
		String[][] signatures= new String[][]{};

		validatePassingTest("C", new String[]{"A", "B", "C"}, "I", true, names, signatures, null);
	}

	public void test103() throws Exception{
		String[] names= new String[]{};
		String[][] signatures= new String[][]{};

		validatePassingTest("C", new String[]{"A", "B", "C"}, "I", true, names, signatures, null);
	}

	public void test104() throws Exception {
		// bug 195817
		String[] names= new String[]{ "m1" };
		String[][] signatures= new String[][]{ new String[0] };

		validatePassingTest("A", new String[]{"A", "B"}, "I", true, names, signatures, null);
	}

	public void test105() throws Exception{
		// bug 195817
		String[] names= new String[]{ "m2" };
		String[][] signatures= new String[][]{ new String[0] };

		validatePassingTest("A", new String[]{"A", "B"}, "I", true, names, signatures, null);
	}

	public void test106() throws Exception {
		// bug 195817
		String[] names= new String[]{ "m1" };
		String[][] signatures= new String[][]{ new String[0] };

		validatePassingTest("A", new String[]{"A", "B"}, "I", true, names, signatures, null);
	}

	public void test107() throws Exception{
		// bug 195817
		String[] names= new String[]{ "m2" };
		String[][] signatures= new String[][]{ new String[0] };

		validatePassingTest("A", new String[]{"A", "B"}, "I", true, names, signatures, null);
	}

	public void test108() throws Exception{
		// bug 195817
		fGenerateAnnotations= true; // should not generate because project is 1.5
		standardPassingTest();
	}
	
	public void test109() throws Exception{
		// Generate @Override in 1.6 project
		fGenerateAnnotations= true;
		RefactoringTestSetup refactoringTestSetup= new RefactoringTestSetup(null);
		try {
			JavaProjectHelper.addRTJar16(getRoot().getJavaProject());
			
			standardPassingTest();
			
			refactoringTestSetup.tearDown();
		} finally {
			refactoringTestSetup.setUp();
		}
	}
	
	public void testPaperExample0() throws Exception{
		String[] names= new String[]{"add", "addAll", "iterator"};
		String[][] signatures= new String[][]{new String[]{"QComparable;"}, new String[]{"QA;"}, new String[0]};
		String[] fieldNames= null;
		validatePassingTest("A", new String[]{"A"}, "Bag", true, names, signatures, fieldNames);
	}

	public void testPaperExample1() throws Exception{
		String[] names= new String[]{"add", "addAll", "iterator"};
		String[][] signatures= new String[][]{new String[]{"QComparable;"}, new String[]{"QA;"}, new String[0]};
		String[] fieldNames= null;
		validatePassingTest("A", new String[]{"A"}, "Bag", true, names, signatures, fieldNames);
	}

	public void testPaperExampleSimplified0() throws Exception{
		String[] names= new String[]{};
		String[][] signatures= {{}};
		String[] fieldNames= null;
		validatePassingTest("A", new String[]{"A"}, "Bag", true, names, signatures, fieldNames);
	}


	public void testConditional1() throws Exception {
		String[] names= new String[]{};
		String[][] signatures= {{}};
		String[] fieldNames= null;
		validatePassingTest("X", new String[]{"A", "X"}, "I", true, names, signatures, fieldNames);
	}

	public void testConditional2() throws Exception {
		String[] names= new String[]{ "dot" };
		String[][] signatures= {new String[]{"QX;"}};
		String[] fieldNames= null;
		validatePassingTest("X", new String[]{"A", "X"}, "I", true, names, signatures, fieldNames);
	}

    public void testConstant80() throws Exception{
        String[] names= null;
        String[][] signatures= null;
        String[] fieldNames= {"X"};
        validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
    }

    public void testConstant81() throws Exception{
        String[] names= null;
        String[][] signatures= null;
        String[] fieldNames= {"X"};
        validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
    }

    public void testConstant82() throws Exception{
        String[] names= null;
        String[][] signatures= null;
        String[] fieldNames= {"X"};
        validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
    }

    public void testConstant83() throws Exception{
        String[] names= null;
        String[][] signatures= null;
        String[] fieldNames= {"X"};
        validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
    }

	public void testConstant84() throws Exception{
		String[] names= null;
		String[][] signatures= null;
		String[] fieldNames= {"X"};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void testConstant85() throws Exception{
		String[] names= null;
		String[][] signatures= null;
		String[] fieldNames= {"X"};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void testConstant86() throws Exception{
		String[] names= null;
		String[][] signatures= null;
		String[] fieldNames= {"X", "Y"};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void testConstant87() throws Exception{
		String[] names= null;
		String[][] signatures= null;
		String[] fieldNames= {"X", "Y"};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void testConstant88() throws Exception{
		String[] names= null;
		String[][] signatures= null;
		String[] fieldNames= {"X", "Y"};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void testInterface0() throws Exception{
		String[] names= {"m"};
		String[][] signatures= {new String[0]};
		String[] fieldNames= null;
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void testInterface1() throws Exception{
		String[] names= {"m"};
		String[][] signatures= {new String[0]};
		String[] fieldNames= null;
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void testInterface2() throws Exception{
		String[] names= {"m"};
		String[][] signatures= {new String[0]};
		String[] fieldNames= {"i", "j"};
		validatePassingTest("A", new String[]{"A"}, "I", true, names, signatures, fieldNames);
	}

	public void testInterface3() throws Exception{
		String[] methodNames= {"m", "m1", "m2", "m4", "m5"};
		String[][] signatures= {new String[0], new String[0], new String[0], new String[0], new String[0]};
		String[] fieldNames= {"I", "I1", "I2", "I4", "I5"};
		validatePassingTest("A", new String[]{"A"}, "I", true, methodNames, signatures, fieldNames);
	}

	public void testInterface4() throws Exception{
//		printTestDisabledMessage("cannot yet update references (in methods) to itself if it's an interface");
		String[] methodNames= {"a"};
		String[][] signatures= {{"QA;", "QA;"}};
		String[] fieldNames= null;
		validatePassingTest("A", new String[]{"A"}, "I", true, methodNames, signatures, fieldNames);
	}

	public void testInterface5() throws Exception{
		String[] methodNames= {"a"};
		String[][] signatures= {new String[0]};
		String[] fieldNames= null;
		validatePassingTest("A", new String[]{"A"}, "I", true, methodNames, signatures, fieldNames);
	}

	public void testInterface6() throws Exception{
		String[] methodNames= {"foo0", "foo1", "foo2", "foo3"};
		String[][] signatures= {new String[0], new String[0], new String[0], new String[0]};
		String[] fieldNames= null;
		validatePassingTest("A", new String[]{"A"}, "I", true, methodNames, signatures, fieldNames);
	}

	public void testFail1() throws Exception{
		validateFailingTest("A", "I", true, RefactoringStatus.FATAL);
	}


}
