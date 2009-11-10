/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.source;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.codemanipulation.GenerateHashCodeEqualsOperation;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

/**
 * Tests generation of delegate methods
 *
 */
public class GenerateHashCodeEqualsTest extends SourceTestCase {

	static final Class THIS= GenerateHashCodeEqualsTest.class;

	public GenerateHashCodeEqualsTest(String name) {
		super(name);
	}

	public static Test setupTest(Test test) {
		return new ProjectTestSetup(test);
	}
	
	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		return allTests();
	}

	public void runOperation(IType type, IField[] fields, IJavaElement insertBefore, boolean createComments, boolean useInstanceof, boolean useBlocks, boolean force) throws CoreException {

		RefactoringASTParser parser= new RefactoringASTParser(AST.JLS3);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);

		IVariableBinding[] fKeys= new IVariableBinding[fields.length];
		for (int i= 0; i < fields.length; i++) {
			Assert.assertTrue(fields[i].exists());
			VariableDeclarationFragment frag= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(fields[i], unit);
			fKeys[i]= frag.resolveBinding();
		}

		fSettings.createComments= createComments;

		AbstractTypeDeclaration decl= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(type, unit);
		ITypeBinding binding= decl.resolveBinding();
		GenerateHashCodeEqualsOperation op= new GenerateHashCodeEqualsOperation(binding, fKeys, unit, insertBefore, fSettings, useInstanceof, force, true, true);
		op.setUseBlocksForThen(useBlocks);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(type.getCompilationUnit());
	}

	public void runOperation(IType type, IField[] fields, boolean useInstanceof, boolean force) throws CoreException {
		runOperation(type, fields, null, true, useInstanceof, false, force);
	}

	private IField[] getFields(IType type, String[] fieldNames) {
		IField[] fields= new IField[fieldNames.length];
		for (int i= 0; i < fields.length; i++) {
			fields[i]= type.getField(fieldNames[i]);
		}
		return fields;
	}

	// ------------- Actual tests

	/**
	 * Test non-reference types (and Enum) in a direct subclass of Object
	 *
	 * @throws Exception
	 */
	public void test01() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	java.lang.annotation.ElementType anEnum;\r\n" +
				"\r\n" +
				"}", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"aBool", "aByte", "aChar", "anInt", "aDouble", "aFloat", "aLong", "anEnum" });
		runOperation(a.getType("A"), fields, false, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	java.lang.annotation.ElementType anEnum;\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + (aBool ? 1231 : 1237);\r\n" +
				"		result = prime * result + aByte;\r\n" +
				"		result = prime * result + aChar;\r\n" +
				"		result = prime * result + anInt;\r\n" +
				"		long temp;\r\n" +
				"		temp = Double.doubleToLongBits(aDouble);\r\n" +
				"		result = prime * result + (int) (temp ^ (temp >>> 32));\r\n" +
				"		result = prime * result + Float.floatToIntBits(aFloat);\r\n" +
				"		result = prime * result + (int) (aLong ^ (aLong >>> 32));\r\n" +
				"		result = prime * result + ((anEnum == null) ? 0 : anEnum.hashCode());\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (aBool != other.aBool)\r\n" +
				"			return false;\r\n" +
				"		if (aByte != other.aByte)\r\n" +
				"			return false;\r\n" +
				"		if (aChar != other.aChar)\r\n" +
				"			return false;\r\n" +
				"		if (anInt != other.anInt)\r\n" +
				"			return false;\r\n" +
				"		if (Double.doubleToLongBits(aDouble) != Double.doubleToLongBits(other.aDouble))\r\n" +
				"			return false;\r\n" +
				"		if (Float.floatToIntBits(aFloat) != Float.floatToIntBits(other.aFloat))\r\n" +
				"			return false;\r\n" +
				"		if (aLong != other.aLong)\r\n" +
				"			return false;\r\n" +
				"		if (anEnum != other.anEnum)\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test non-reference types in an indrect subclass of Object
	 *
	 * @throws Exception
	 */
	public void test02() throws Exception {

		fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A extends B {\r\n" +
				"	\r\n" +
				"	boolean aBool;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"aBool" });
		runOperation(a.getType("A"), fields, false, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A extends B {\r\n" +
				"	\r\n" +
				"	boolean aBool;\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + (aBool ? 1231 : 1237);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (aBool != other.aBool)\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test reference types in a direct subclass of Object
	 *
	 * @throws Exception
	 */
	public void test03() throws Exception {

		fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	A anA;\r\n" +
				"	B aB;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"anA", "aB" });
		runOperation(a.getType("A"), fields, false, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	A anA;\r\n" +
				"	B aB;\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + ((anA == null) ? 0 : anA.hashCode());\r\n" +
				"		result = prime * result + ((aB == null) ? 0 : aB.hashCode());\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (anA == null) {\r\n" +
				"			if (other.anA != null)\r\n" +
				"				return false;\r\n" +
				"		} else if (!anA.equals(other.anA))\r\n" +
				"			return false;\r\n" +
				"		if (aB == null) {\r\n" +
				"			if (other.aB != null)\r\n" +
				"				return false;\r\n" +
				"		} else if (!aB.equals(other.aB))\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test reference types in an indirect subclass of Object
	 *
	 * @throws Exception
	 */
	public void test04() throws Exception {

		fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A extends B {\r\n" +
				"	\r\n" +
				"	A anA;\r\n" +
				"	B aB;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"anA", "aB" });
		runOperation(a.getType("A"), fields, false, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A extends B {\r\n" +
				"	\r\n" +
				"	A anA;\r\n" +
				"	B aB;\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + ((anA == null) ? 0 : anA.hashCode());\r\n" +
				"		result = prime * result + ((aB == null) ? 0 : aB.hashCode());\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (anA == null) {\r\n" +
				"			if (other.anA != null)\r\n" +
				"				return false;\r\n" +
				"		} else if (!anA.equals(other.anA))\r\n" +
				"			return false;\r\n" +
				"		if (aB == null) {\r\n" +
				"			if (other.aB != null)\r\n" +
				"				return false;\r\n" +
				"		} else if (!aB.equals(other.aB))\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test arrays
	 *
	 * @throws Exception
	 */
	public void test05() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	A[] someAs;\r\n" +
				"	int[] someInts;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"someAs", "someInts" });
		runOperation(a.getType("A"), fields, false, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"import java.util.Arrays;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	A[] someAs;\r\n" +
				"	int[] someInts;\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + Arrays.hashCode(someAs);\r\n" +
				"		result = prime * result + Arrays.hashCode(someInts);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (!Arrays.equals(someAs, other.someAs))\r\n" +
				"			return false;\r\n" +
				"		if (!Arrays.equals(someInts, other.someInts))\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test insertion in-between two methods
	 *
	 * @throws Exception
	 */
	public void test06() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	int someInt;\r\n" +
				"	\r\n" +
				"	public void foo() {}\r\n" +
				"	public void bar() {}\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"someInt" });
		runOperation(a.getType("A"), fields, a.getType("A").getMethod("bar", new String[0]), true, false, false, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	int someInt;\r\n" +
				"	\r\n" +
				"	public void foo() {}\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + someInt;\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (someInt != other.someInt)\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"	public void bar() {}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Some enums and generic field types...
	 *
	 * @throws Exception
	 */
	public void test07() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"import java.util.HashMap;\r\n" +
				"import java.util.List;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	List<Integer> intList;\r\n" +
				"	HashMap<Integer, List<Boolean>> intBoolHashMap;\r\n" +
				"	E someEnum;\r\n" +
				"}\r\n" +
				"\r\n" +
				"enum E {\r\n" +
				"}", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"intList", "intBoolHashMap", "someEnum" });
		runOperation(a.getType("A"), fields, false, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"import java.util.HashMap;\r\n" +
				"import java.util.List;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	List<Integer> intList;\r\n" +
				"	HashMap<Integer, List<Boolean>> intBoolHashMap;\r\n" +
				"	E someEnum;\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + ((intList == null) ? 0 : intList.hashCode());\r\n" +
				"		result = prime * result + ((intBoolHashMap == null) ? 0 : intBoolHashMap.hashCode());\r\n" +
				"		result = prime * result + ((someEnum == null) ? 0 : someEnum.hashCode());\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (intList == null) {\r\n" +
				"			if (other.intList != null)\r\n" +
				"				return false;\r\n" +
				"		} else if (!intList.equals(other.intList))\r\n" +
				"			return false;\r\n" +
				"		if (intBoolHashMap == null) {\r\n" +
				"			if (other.intBoolHashMap != null)\r\n" +
				"				return false;\r\n" +
				"		} else if (!intBoolHashMap.equals(other.intBoolHashMap))\r\n" +
				"			return false;\r\n" +
				"		if (someEnum != other.someEnum)\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"\r\n" +
				"enum E {\r\n" +
				"}";

		compareSource(expected, a.getSource());
	}

	/**
	 * Two double fields
	 *
	 * @throws Exception
	 */
	public void test08() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	double d1;\r\n" +
				"	double d2;\r\n" +
				"}", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"d1", "d2" });
		runOperation(a.getType("A"), fields, false, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	double d1;\r\n" +
				"	double d2;\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		long temp;\r\n" +
				"		temp = Double.doubleToLongBits(d1);\r\n" +
				"		result = prime * result + (int) (temp ^ (temp >>> 32));\r\n" +
				"		temp = Double.doubleToLongBits(d2);\r\n" +
				"		result = prime * result + (int) (temp ^ (temp >>> 32));\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (Double.doubleToLongBits(d1) != Double.doubleToLongBits(other.d1))\r\n" +
				"			return false;\r\n" +
				"		if (Double.doubleToLongBits(d2) != Double.doubleToLongBits(other.d2))\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"}";

		compareSource(expected, a.getSource());
	}

	/**
	 * "this" qualification for fields with the
	 * same name as a newly introduced temporary
	 *
	 * @throws Exception
	 */
	public void test09() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	double temp;\r\n" +
				"	boolean result;\r\n" +
				"	String obj;\r\n" +
				"	double someOther;\r\n" +
				"}", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"temp", "result", "obj", "someOther" });
		runOperation(a.getType("A"), fields, false, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	double temp;\r\n" +
				"	boolean result;\r\n" +
				"	String obj;\r\n" +
				"	double someOther;\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		long temp;\r\n" +
				"		temp = Double.doubleToLongBits(this.temp);\r\n" +
				"		result = prime * result + (int) (temp ^ (temp >>> 32));\r\n" +
				"		result = prime * result + (this.result ? 1231 : 1237);\r\n" +
				"		result = prime * result + ((obj == null) ? 0 : obj.hashCode());\r\n" +
				"		temp = Double.doubleToLongBits(someOther);\r\n" +
				"		result = prime * result + (int) (temp ^ (temp >>> 32));\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (Double.doubleToLongBits(temp) != Double.doubleToLongBits(other.temp))\r\n" +
				"			return false;\r\n" +
				"		if (result != other.result)\r\n" +
				"			return false;\r\n" +
				"		if (this.obj == null) {\r\n" +
				"			if (other.obj != null)\r\n" +
				"				return false;\r\n" +
				"		} else if (!this.obj.equals(other.obj))\r\n" +
				"			return false;\r\n" +
				"		if (Double.doubleToLongBits(someOther) != Double.doubleToLongBits(other.someOther))\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"}";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test non-reference types in a direct subclass of Object, using 'instanceof' comparison
	 *
	 * @throws Exception
	 */
	public void test10() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"\r\n" +
				"}", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"aBool", "aByte", "aChar", "anInt", "aDouble", "aFloat", "aLong" });
		runOperation(a.getType("A"), fields, true, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + (aBool ? 1231 : 1237);\r\n" +
				"		result = prime * result + aByte;\r\n" +
				"		result = prime * result + aChar;\r\n" +
				"		result = prime * result + anInt;\r\n" +
				"		long temp;\r\n" +
				"		temp = Double.doubleToLongBits(aDouble);\r\n" +
				"		result = prime * result + (int) (temp ^ (temp >>> 32));\r\n" +
				"		result = prime * result + Float.floatToIntBits(aFloat);\r\n" +
				"		result = prime * result + (int) (aLong ^ (aLong >>> 32));\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (!(obj instanceof A))\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (aBool != other.aBool)\r\n" +
				"			return false;\r\n" +
				"		if (aByte != other.aByte)\r\n" +
				"			return false;\r\n" +
				"		if (aChar != other.aChar)\r\n" +
				"			return false;\r\n" +
				"		if (anInt != other.anInt)\r\n" +
				"			return false;\r\n" +
				"		if (Double.doubleToLongBits(aDouble) != Double.doubleToLongBits(other.aDouble))\r\n" +
				"			return false;\r\n" +
				"		if (Float.floatToIntBits(aFloat) != Float.floatToIntBits(other.aFloat))\r\n" +
				"			return false;\r\n" +
				"		if (aLong != other.aLong)\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test non-reference types in an indrect subclass of Object, using 'instanceof' comparison
	 *
	 * @throws Exception
	 */
	public void test11() throws Exception {

		fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A extends B {\r\n" +
				"	\r\n" +
				"	boolean aBool;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"aBool" });
		runOperation(a.getType("A"), fields, true, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A extends B {\r\n" +
				"	\r\n" +
				"	boolean aBool;\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + (aBool ? 1231 : 1237);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (!(obj instanceof A))\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (aBool != other.aBool)\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test reference types in a direct subclass of Object, using 'instanceof' comparison
	 *
	 * @throws Exception
	 */
	public void test12() throws Exception {

		fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	A anA;\r\n" +
				"	B aB;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"anA", "aB" });
		runOperation(a.getType("A"), fields, true, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	A anA;\r\n" +
				"	B aB;\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + ((anA == null) ? 0 : anA.hashCode());\r\n" +
				"		result = prime * result + ((aB == null) ? 0 : aB.hashCode());\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (!(obj instanceof A))\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (anA == null) {\r\n" +
				"			if (other.anA != null)\r\n" +
				"				return false;\r\n" +
				"		} else if (!anA.equals(other.anA))\r\n" +
				"			return false;\r\n" +
				"		if (aB == null) {\r\n" +
				"			if (other.aB != null)\r\n" +
				"				return false;\r\n" +
				"		} else if (!aB.equals(other.aB))\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test that multiple applications yield same result (without super calls)
	 * (https://bugs.eclipse.org/bugs/show_bug.cgi?id=154417)
	 *
	 * @throws Exception
	 */
	public void test13() throws Exception {

		fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
				"\r\n" +
				"public class B {\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	A anA;\r\n" +
				"	B aB;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"anA", "aB" });
		runOperation(a.getType("A"), fields, true, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	A anA;\r\n" +
				"	B aB;\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + ((anA == null) ? 0 : anA.hashCode());\r\n" +
				"		result = prime * result + ((aB == null) ? 0 : aB.hashCode());\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (!(obj instanceof A))\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (anA == null) {\r\n" +
				"			if (other.anA != null)\r\n" +
				"				return false;\r\n" +
				"		} else if (!anA.equals(other.anA))\r\n" +
				"			return false;\r\n" +
				"		if (aB == null) {\r\n" +
				"			if (other.aB != null)\r\n" +
				"				return false;\r\n" +
				"		} else if (!aB.equals(other.aB))\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());

		runOperation(a.getType("A"), fields, true, true);
		compareSource(expected, a.getSource());
	}

	/**
	 * Test member types
	 *
	 * @throws Exception
	 */
	public void testMemberType() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	public class Inner {\r\n" +
				"		int x;\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"", true, null);

		IType type= a.getType("A").getType("Inner");
		IField[] fields= getFields(type, new String[] {"x" });
		runOperation(type, fields, true, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	public class Inner {\r\n" +
				"		int x;\r\n" +
				"\r\n" +
				"		/* (non-Javadoc)\r\n" +
				"		 * @see java.lang.Object#hashCode()\r\n" +
				"		 */\r\n" +
				"		@Override\r\n" +
				"		public int hashCode() {\r\n" +
				"			final int prime = 31;\r\n" +
				"			int result = 1;\r\n" +
				"			result = prime * result + getOuterType().hashCode();\r\n" +
				"			result = prime * result + x;\r\n" +
				"			return result;\r\n" +
				"		}\r\n" +
				"\r\n" +
				"		/* (non-Javadoc)\r\n" +
				"		 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"		 */\r\n" +
				"		@Override\r\n" +
				"		public boolean equals(Object obj) {\r\n" +
				"			if (this == obj)\r\n" +
				"				return true;\r\n" +
				"			if (obj == null)\r\n" +
				"				return false;\r\n" +
				"			if (!(obj instanceof Inner))\r\n" +
				"				return false;\r\n" +
				"			Inner other = (Inner) obj;\r\n" +
				"			if (!getOuterType().equals(other.getOuterType()))\r\n" +
				"				return false;\r\n" +
				"			if (x != other.x)\r\n" +
				"				return false;\r\n" +
				"			return true;\r\n" +
				"		}\r\n" +
				"\r\n" +
				"		private A getOuterType() {\r\n" +
				"			return A.this;\r\n" +
				"		}\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test non-reference types in a direct subclass of Object
	 *
	 * @throws Exception
	 */
	public void testThenWithBlocks() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	boolean aBool;\r\n" +
				"	Object obj;\r\n" +
				"\r\n" +
				"}", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"aBool", "obj" });
		runOperation(a.getType("A"), fields, null, true, false, true, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	boolean aBool;\r\n" +
				"	Object obj;\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#hashCode()\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + (aBool ? 1231 : 1237);\r\n" +
				"		result = prime * result + ((obj == null) ? 0 : obj.hashCode());\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
				"	 */\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj) {\r\n" +
				"			return true;\r\n" +
				"		}\r\n" +
				"		if (obj == null) {\r\n" +
				"			return false;\r\n" +
				"		}\r\n" +
				"		if (getClass() != obj.getClass()) {\r\n" +
				"			return false;\r\n" +
				"		}\r\n" +
				"		A other = (A) obj;\r\n" +
				"		if (aBool != other.aBool) {\r\n" +
				"			return false;\r\n" +
				"		}\r\n" +
				"		if (this.obj == null) {\r\n" +
				"			if (other.obj != null) {\r\n" +
				"				return false;\r\n" +
				"			}\r\n" +
				"		} else if (!this.obj.equals(other.obj)) {\r\n" +
				"			return false;\r\n" +
				"		}\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test reference types in an subclass
	 *
	 * @throws Exception
	 */
	public void testSubTypeAndArraysIn14() throws Exception {
		IJavaProject javaProject= fPackageP.getJavaProject();
		Map oldOptions= javaProject.getOptions(false);
		Map newOptions= new HashMap(oldOptions);
		JavaModelUtil.setComplianceOptions(newOptions, JavaCore.VERSION_1_4);
		javaProject.setOptions(newOptions);
		try {
			fPackageP.createCompilationUnit("B.java", "package p;\r\n" +
					"\r\n" +
					"public class B {\r\n" +
					"	public int hashCode() {\r\n" +
					"		return 1;\r\n" +
					"	}\r\n" +
					"	public boolean equals(Object obj) {\r\n" +
					"		return obj instanceof B;\r\n" +
					"	}\r\n" +
					"\r\n" +
					"}\r\n" +
					"", true, null);

			ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
					"\r\n" +
					"public class A extends B {\r\n" +
					"\r\n" +
					"	A[] anArray;\r\n" +
					"	double[] anDArray;\r\n" +
					"\r\n" +
					"}\r\n" +
					"", true, null);

			IField[] fields= getFields(a.getType("A"), new String[] {"anArray", "anDArray"});
			runOperation(a.getType("A"), fields, false, false);

			String expected= "package p;\r\n" +
					"\r\n" +
					"import java.util.Arrays;\r\n" +
					"\r\n" +
					"public class A extends B {\r\n" +
					"\r\n" +
					"	/**\r\n" +
					"	 * Returns a hash code value for the array\r\n" +
					"	 * @param array the array to create a hash code value for\r\n" +
					"	 * @return a hash code value for the array\r\n" +
					"	 */\r\n" +
					"	private static int hashCode(double[] array) {\r\n" +
					"		int prime = 31;\r\n" +
					"		if (array == null)\r\n" +
					"			return 0;\r\n" +
					"		int result = 1;\r\n" +
					"		for (int index = 0; index < array.length; index++) {\r\n" +
					"			long temp = Double.doubleToLongBits(array[index]);\r\n" +
					"			result = prime * result + (int) (temp ^ (temp >>> 32));\r\n" +
					"		}\r\n" +
					"		return result;\r\n" +
					"	}\r\n" +
					"	/**\r\n" +
					"	 * Returns a hash code value for the array\r\n" +
					"	 * @param array the array to create a hash code value for\r\n" +
					"	 * @return a hash code value for the array\r\n" +
					"	 */\r\n" +
					"	private static int hashCode(Object[] array) {\r\n" +
					"		int prime = 31;\r\n" +
					"		if (array == null)\r\n" +
					"			return 0;\r\n" +
					"		int result = 1;\r\n" +
					"		for (int index = 0; index < array.length; index++) {\r\n" +
					"			result = prime * result + (array[index] == null ? 0 : array[index].hashCode());\r\n" +
					"		}\r\n" +
					"		return result;\r\n" +
					"	}\r\n" +
					"	A[] anArray;\r\n" +
					"	double[] anDArray;\r\n" +
					"	/* (non-Javadoc)\r\n" +
					"	 * @see java.lang.Object#hashCode()\r\n" +
					"	 */\r\n" +
					"	public int hashCode() {\r\n" +
					"		final int prime = 31;\r\n" +
					"		int result = super.hashCode();\r\n" +
					"		result = prime * result + A.hashCode(anArray);\r\n" +
					"		result = prime * result + A.hashCode(anDArray);\r\n" +
					"		return result;\r\n" +
					"	}\r\n" +
					"	/* (non-Javadoc)\r\n" +
					"	 * @see java.lang.Object#equals(java.lang.Object)\r\n" +
					"	 */\r\n" +
					"	public boolean equals(Object obj) {\r\n" +
					"		if (this == obj)\r\n" +
					"			return true;\r\n" +
					"		if (!super.equals(obj))\r\n" +
					"			return false;\r\n" +
					"		if (getClass() != obj.getClass())\r\n" +
					"			return false;\r\n" +
					"		A other = (A) obj;\r\n" +
					"		if (!Arrays.equals(anArray, other.anArray))\r\n" +
					"			return false;\r\n" +
					"		if (!Arrays.equals(anDArray, other.anDArray))\r\n" +
					"			return false;\r\n" +
					"		return true;\r\n" +
					"	}\r\n" +
					"\r\n" +
					"}\r\n" +
					"";

			compareSource(expected, a.getSource());
		} finally {
			javaProject.setOptions(oldOptions);
		}
	}

	public void testInsertAt() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class A  {\n");
		buf.append("	Runnable x;\n");
		buf.append("	\n");
		buf.append("	A() {\n");
		buf.append("	}\n");
		buf.append("	\n");
		buf.append("	void foo() {\n");
		buf.append("	}\n");
		buf.append("	\n");
		buf.append("	{\n"); // initializer
		buf.append("	}\n");
		buf.append("	\n");
		buf.append("	static {\n"); // static initializer
		buf.append("	}\n");
		buf.append("	\n");
		buf.append("	class Inner {\n"); // inner class
		buf.append("	}\n");
		buf.append("}");
		String originalContent= buf.toString();

		final int NUM_MEMBERS= 6;

		// try to insert the new methods after every member and at the end
		for (int i= 0; i < NUM_MEMBERS + 1; i++) {

			ICompilationUnit unit= null;
			try {
				unit= fPackageP.createCompilationUnit("A.java", originalContent, true, null);

				IType type= unit.findPrimaryType();
				IJavaElement[] children= type.getChildren();
				IField foo= (IField) children[0];
				assertEquals(NUM_MEMBERS, children.length);

				IJavaElement insertBefore= i < NUM_MEMBERS ? children[i] : null;

				runOperation(type, new IField[] { foo }, insertBefore, false, false, false, false);

				IJavaElement[] newChildren= type.getChildren();
				assertEquals(NUM_MEMBERS + 2, newChildren.length);

				assertEquals("hashCode", newChildren[i].getElementName());
				assertEquals("equals", newChildren[i + 1].getElementName());
			} finally {
				if (unit != null)
					JavaProjectHelper.delete(unit);
			}
		}
	}

	/**
	 * Test with abstract methods in superclass,
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=291924
	 *
	 * @throws Exception rarely
	 */
	public void testAbstractSuperMethods() throws Exception {
	
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" + 
				"\r\n" + 
				"abstract class Super {\r\n" + 
				"	public abstract int hashCode();\r\n" + 
				"	public abstract boolean equals(Object other);\r\n" + 
				"}\r\n" + 
				"\r\n" + 
				"class Sub extends Super {\r\n" + 
				"	String name;\r\n" + 
				"}" +
				"", true, null);
	
		IField[] fields= getFields(a.getType("Sub"), new String[] {"name" });
		runOperation(a.getType("Sub"), fields, null, false, false, false, false);
	
		String expected= "package p;\r\n" + 
				"\r\n" + 
				"abstract class Super {\r\n" + 
				"	public abstract int hashCode();\r\n" + 
				"	public abstract boolean equals(Object other);\r\n" + 
				"}\r\n" + 
				"\r\n" + 
				"class Sub extends Super {\r\n" + 
				"	String name;\r\n" + 
				"\r\n" + 
				"	@Override\r\n" + 
				"	public int hashCode() {\r\n" + 
				"		final int prime = 31;\r\n" + 
				"		int result = 1;\r\n" + 
				"		result = prime * result + ((name == null) ? 0 : name.hashCode());\r\n" + 
				"		return result;\r\n" + 
				"	}\r\n" + 
				"\r\n" + 
				"	@Override\r\n" + 
				"	public boolean equals(Object obj) {\r\n" + 
				"		if (this == obj)\r\n" + 
				"			return true;\r\n" + 
				"		if (obj == null)\r\n" + 
				"			return false;\r\n" + 
				"		if (getClass() != obj.getClass())\r\n" + 
				"			return false;\r\n" + 
				"		Sub other = (Sub) obj;\r\n" + 
				"		if (name == null) {\r\n" + 
				"			if (other.name != null)\r\n" + 
				"				return false;\r\n" + 
				"		} else if (!name.equals(other.name))\r\n" + 
				"			return false;\r\n" + 
				"		return true;\r\n" + 
				"	}\r\n" + 
				"}" +
				"";
	
		compareSource(expected, a.getSource());
	}

}
