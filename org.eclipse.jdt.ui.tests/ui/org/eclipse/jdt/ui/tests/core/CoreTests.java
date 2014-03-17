/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.StringAsserts;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;

import org.eclipse.jdt.ui.tests.core.source.SourceActionTests;


public class CoreTests extends TestCase {

	public static Test suite() {

		TestSuite suite= new TestSuite(CoreTests.class.getName());
		suite.addTest(AddImportTest.suite());
		suite.addTest(SourceActionTests.suite());
		suite.addTest(ASTNodesInsertTest.suite());
		suite.addTest(BindingsNameTest.suite());
		suite.addTest(CallHierarchyTest.suite());
		suite.addTest(ClassPathDetectorTest.suite());
		suite.addTest(CodeFormatterUtilTest.suite());
		suite.addTest(CodeFormatterTest.suite());
		suite.addTest(HierarchicalASTVisitorTest.suite());
		suite.addTest(ImportOrganizeTest.suite());
		suite.addTest(ImportOrganizeTest18.suite());
		suite.addTest(JavaElementLabelsTest.suite());
		suite.addTest(JavaElementLabelsTest18.suite());
		suite.addTest(JavaElementPropertyTesterTest.suite());
		suite.addTest(JavaModelUtilTest.suite());
		suite.addTest(MethodOverrideTest.suite());
		suite.addTest(MethodOverrideTest18.suite());
		suite.addTest(NameProposerTest.suite());
		suite.addTest(OverrideTest.suite());
		suite.addTest(PartialASTTest.suite());
		suite.addTest(ScopeAnalyzerTest.suite());
		suite.addTest(TemplateStoreTest.suite());
		suite.addTest(TypeHierarchyTest.suite());
		suite.addTest(TypeRulesTest.suite());
		suite.addTest(TypeInfoTest.suite());
		suite.addTest(StringsTest.suite());
		suite.addTest(IndentManipulationTest.suite());
		suite.addTest(SelectionHistoryTest.suite());
		suite.addTest(ASTProviderTest.suite());
		suite.addTest(JDTFlagsTest18.suite());

		return new ProjectTestSetup(suite);
	}

	public CoreTests(String name) {
		super(name);
	}

	public static void assertEqualString(String actual, String expected) {
		StringAsserts.assertEqualString(actual, expected);
	}

	public static void assertEqualStringIgnoreDelim(String actual, String expected) throws IOException {
		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

	public static void assertEqualStringsIgnoreOrder(String[] actuals, String[] expecteds) {
		StringAsserts.assertEqualStringsIgnoreOrder(actuals, expecteds);
	}

	public static void assertNumberOf(String name, int is, int expected) {
		assertTrue("Wrong number of " + name + ", is: " + is + ", expected: " + expected, is == expected);
	}

	protected ImportRewrite newImportsRewrite(ICompilationUnit cu, String[] order, int normalThreshold, int staticThreshold, boolean restoreExistingImports) throws CoreException {
		ImportRewrite rewrite= StubUtility.createImportRewrite(cu, restoreExistingImports);
		rewrite.setImportOrder(order);
		rewrite.setOnDemandImportThreshold(normalThreshold);
		rewrite.setStaticOnDemandImportThreshold(staticThreshold);
		return rewrite;
	}

	protected ImportRewrite newImportsRewrite(CompilationUnit cu, String[] order, int normalThreshold, int staticThreshold, boolean restoreExistingImports) {
		ImportRewrite rewrite= ImportRewrite.create(cu, restoreExistingImports);
		rewrite.setImportOrder(order);
		rewrite.setOnDemandImportThreshold(normalThreshold);
		rewrite.setStaticOnDemandImportThreshold(staticThreshold);
		return rewrite;
	}
}
