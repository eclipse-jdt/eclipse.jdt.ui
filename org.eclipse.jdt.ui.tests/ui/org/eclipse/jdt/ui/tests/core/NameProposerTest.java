/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;


public class NameProposerTest extends TestCase {

	private static final Class THIS= NameProposerTest.class;

	private IJavaProject fJProject1;

	public NameProposerTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new TestSuite(THIS);
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new NameProposerTest("testGetterSetterName"));
			return suite;
		}
	}

	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}


	public void testGetterSetterName() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("  int fCount;\n");
		buf.append("  static int fgSingleton;\n");
		buf.append("  int foo;\n");
		buf.append("\n");
		buf.append("  boolean fBlue;\n");
		buf.append("  boolean modified;\n");
		buf.append("  boolean isTouched;\n");
		buf.append("\n");
		buf.append("  static final int K_CONST;\n");
		buf.append("  static final boolean MY_CONST_ANT;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
		IType type= cu.getType("C");

		fJProject1.setOption(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		fJProject1.setOption(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "fg");
		fJProject1.setOption(JavaCore.CODEASSIST_STATIC_FINAL_FIELD_PREFIXES, "K_");

		String[] excluded= new String[0];
		IField f1= type.getField("fCount");
		IField f2= type.getField("fgSingleton");
		IField f3= type.getField("foo");
		IField f4= type.getField("fBlue");
		IField f5= type.getField("modified");
		IField f6= type.getField("isTouched");
		IField f7= type.getField("K_CONST");
		IField f8= type.getField("MY_CONST_ANT");

		assertEqualString("setCount", GetterSetterUtil.getSetterName(f1, excluded));
		assertEqualString("getCount", GetterSetterUtil.getGetterName(f1, excluded));
		assertEqualString("setSingleton", GetterSetterUtil.getSetterName(f2, excluded));
		assertEqualString("getSingleton", GetterSetterUtil.getGetterName(f2, excluded));
		assertEqualString("setFoo", GetterSetterUtil.getSetterName(f3, excluded));
		assertEqualString("getFoo", GetterSetterUtil.getGetterName(f3, excluded));

		assertEqualString("setBlue", GetterSetterUtil.getSetterName(f4, excluded));
		assertEqualString("isBlue", GetterSetterUtil.getGetterName(f4, excluded));
		assertEqualString("setModified", GetterSetterUtil.getSetterName(f5, excluded));
		assertEqualString("isModified", GetterSetterUtil.getGetterName(f5, excluded));
		assertEqualString("setTouched", GetterSetterUtil.getSetterName(f6, excluded));
		assertEqualString("isTouched", GetterSetterUtil.getGetterName(f6, excluded));
		
		assertEqualString("setConst", GetterSetterUtil.getSetterName(f7, excluded));
		assertEqualString("getConst", GetterSetterUtil.getGetterName(f7, excluded));
		assertEqualString("setMyConstAnt", GetterSetterUtil.getSetterName(f8, excluded));
		assertEqualString("isMyConstAnt", GetterSetterUtil.getGetterName(f8, excluded));
	}

	private void assertEqualString(String expected, String actual) {
		assertEquals("Expected '" + expected + "', is '" + actual + "'", expected, actual);
	}

}
