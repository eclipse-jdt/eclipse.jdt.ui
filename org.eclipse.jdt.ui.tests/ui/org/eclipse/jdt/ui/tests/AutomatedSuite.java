/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.ui.tests.core.ASTRewritingMethodDeclTest;
import org.eclipse.jdt.ui.tests.core.ASTRewritingStatementsTest;
import org.eclipse.jdt.ui.tests.core.ASTRewritingTypeDeclTest;
import org.eclipse.jdt.ui.tests.core.AddUnimplementedMethodsTest;
import org.eclipse.jdt.ui.tests.core.AllTypesCacheTest;
import org.eclipse.jdt.ui.tests.core.ImportOrganizeTest;
import org.eclipse.jdt.ui.tests.core.JavaModelUtilTest;
import org.eclipse.jdt.ui.tests.core.TextBufferTest;
import org.eclipse.jdt.ui.tests.core.TypeHierarchyTest;
import org.eclipse.jdt.ui.tests.core.TypeInfoTest;
import org.eclipse.jdt.ui.tests.text.HTML2TextReaderTester;
import org.eclipse.jdt.ui.tests.text.JavaDoc2HTMLTextReaderTester;
import org.eclipse.jdt.ui.tests.wizardapi.NewJavaProjectWizardTest;


/**
 * Test all areas of the UI.
 */
public class AutomatedSuite extends TestSuite {

	/**
	 * Returns the suite.  This is required to
	 * use the JUnit Launcher.
	 */
	public static Test suite() {
		return new AutomatedSuite();
	}

	/**
	 * Construct the test suite.
	 */
	public AutomatedSuite() {
		addTest(TypeInfoTest.suite());
		addTest(AddUnimplementedMethodsTest.suite());
		addTest(ImportOrganizeTest.suite());
		addTest(JavaModelUtilTest.suite());
		addTest(TextBufferTest.suite());
		addTest(TypeHierarchyTest.suite());
		addTest(HTML2TextReaderTester.suite());
		addTest(JavaDoc2HTMLTextReaderTester.suite());
		
		addTest(ASTRewritingMethodDeclTest.suite());
		addTest(ASTRewritingStatementsTest.suite());
		addTest(ASTRewritingTypeDeclTest.suite());

		addTest(AllTypesCacheTest.suite());
		addTest(NewJavaProjectWizardTest.suite());
		
	}
	
}

