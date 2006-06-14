/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.source;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
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

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		return allTests();
	}

	public void runOperation(IType type, IField[] fields, IJavaElement insertBefore, boolean createComments, boolean useInstanceof) throws CoreException {

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
		GenerateHashCodeEqualsOperation op= new GenerateHashCodeEqualsOperation(binding, fKeys, unit, insertBefore, fSettings, useInstanceof, false, true, true);

		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(type.getCompilationUnit());
	}

	public void runOperation(IType type, IField[] fields, boolean useInstanceof) throws CoreException {
		runOperation(type, fields, null, true, useInstanceof);
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
	 * Test non-reference types in a direct subclass of Object
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
				"\r\n" + 
				"}", true, null);
		
		IField[] fields= getFields(a.getType("A"), new String[] {"aBool", "aByte", "aChar", "anInt", "aDouble", "aFloat", "aLong" });
		runOperation(a.getType("A"), fields, false);
		
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
				"		final int PRIME = 31;\r\n" + 
				"		int result = 1;\r\n" + 
				"		result = PRIME * result + (aBool ? 1231 : 1237);\r\n" + 
				"		result = PRIME * result + aChar;\r\n" + 
				"		result = PRIME * result + anInt;\r\n" + 
				"		long temp;\r\n" + 
				"		temp = Double.doubleToLongBits(aDouble);\r\n" + 
				"		result = PRIME * result + (int) (temp ^ (temp >>> 32));\r\n" + 
				"		result = PRIME * result + Float.floatToIntBits(aFloat);\r\n" + 
				"		result = PRIME * result + (int) (aLong ^ (aLong >>> 32));\r\n" + 
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
				"		final A other = (A) obj;\r\n" + 
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
		runOperation(a.getType("A"), fields, false);
		
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
				"		final int PRIME = 31;\r\n" + 
				"		int result = 1;\r\n" + 
				"		result = PRIME * result + (aBool ? 1231 : 1237);\r\n" + 
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
				"		final A other = (A) obj;\r\n" + 
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
		runOperation(a.getType("A"), fields, false);
		
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
				"		final int PRIME = 31;\r\n" + 
				"		int result = 1;\r\n" + 
				"		result = PRIME * result + ((anA == null) ? 0 : anA.hashCode());\r\n" + 
				"		result = PRIME * result + ((aB == null) ? 0 : aB.hashCode());\r\n" + 
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
				"		final A other = (A) obj;\r\n" + 
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
		runOperation(a.getType("A"), fields, false);
		
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
				"		final int PRIME = 31;\r\n" + 
				"		int result = 1;\r\n" + 
				"		result = PRIME * result + ((anA == null) ? 0 : anA.hashCode());\r\n" + 
				"		result = PRIME * result + ((aB == null) ? 0 : aB.hashCode());\r\n" + 
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
				"		final A other = (A) obj;\r\n" + 
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
		runOperation(a.getType("A"), fields, false);
		
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
				"		final int PRIME = 31;\r\n" + 
				"		int result = 1;\r\n" + 
				"		result = PRIME * result + Arrays.hashCode(someAs);\r\n" + 
				"		result = PRIME * result + Arrays.hashCode(someInts);\r\n" + 
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
				"		final A other = (A) obj;\r\n" + 
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
		runOperation(a.getType("A"), fields, a.getType("A").getMethod("bar", new String[0]), true, false);
		
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
				"		final int PRIME = 31;\r\n" + 
				"		int result = 1;\r\n" + 
				"		result = PRIME * result + someInt;\r\n" + 
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
				"		final A other = (A) obj;\r\n" + 
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
		runOperation(a.getType("A"), fields, false);
		
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
				"		final int PRIME = 31;\r\n" + 
				"		int result = 1;\r\n" + 
				"		result = PRIME * result + ((intList == null) ? 0 : intList.hashCode());\r\n" + 
				"		result = PRIME * result + ((intBoolHashMap == null) ? 0 : intBoolHashMap.hashCode());\r\n" + 
				"		result = PRIME * result + ((someEnum == null) ? 0 : someEnum.hashCode());\r\n" + 
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
				"		final A other = (A) obj;\r\n" + 
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
				"		if (someEnum == null) {\r\n" + 
				"			if (other.someEnum != null)\r\n" + 
				"				return false;\r\n" + 
				"		} else if (!someEnum.equals(other.someEnum))\r\n" + 
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
		runOperation(a.getType("A"), fields, false);
		
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
				"		final int PRIME = 31;\r\n" + 
				"		int result = 1;\r\n" + 
				"		long temp;\r\n" + 
				"		temp = Double.doubleToLongBits(d1);\r\n" + 
				"		result = PRIME * result + (int) (temp ^ (temp >>> 32));\r\n" + 
				"		temp = Double.doubleToLongBits(d2);\r\n" + 
				"		result = PRIME * result + (int) (temp ^ (temp >>> 32));\r\n" + 
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
				"		final A other = (A) obj;\r\n" + 
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
		runOperation(a.getType("A"), fields, false);
		
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
				"		final int PRIME = 31;\r\n" + 
				"		int result = 1;\r\n" + 
				"		long temp;\r\n" + 
				"		temp = Double.doubleToLongBits(this.temp);\r\n" + 
				"		result = PRIME * result + (int) (temp ^ (temp >>> 32));\r\n" + 
				"		result = PRIME * result + (this.result ? 1231 : 1237);\r\n" + 
				"		result = PRIME * result + ((obj == null) ? 0 : obj.hashCode());\r\n" + 
				"		temp = Double.doubleToLongBits(someOther);\r\n" + 
				"		result = PRIME * result + (int) (temp ^ (temp >>> 32));\r\n" + 
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
				"		final A other = (A) obj;\r\n" + 
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
		runOperation(a.getType("A"), fields, true);
		
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
				"		final int PRIME = 31;\r\n" + 
				"		int result = 1;\r\n" + 
				"		result = PRIME * result + (aBool ? 1231 : 1237);\r\n" + 
				"		result = PRIME * result + aChar;\r\n" + 
				"		result = PRIME * result + anInt;\r\n" + 
				"		long temp;\r\n" + 
				"		temp = Double.doubleToLongBits(aDouble);\r\n" + 
				"		result = PRIME * result + (int) (temp ^ (temp >>> 32));\r\n" + 
				"		result = PRIME * result + Float.floatToIntBits(aFloat);\r\n" + 
				"		result = PRIME * result + (int) (aLong ^ (aLong >>> 32));\r\n" + 
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
				"		final A other = (A) obj;\r\n" + 
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
		runOperation(a.getType("A"), fields, true);
		
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
				"		final int PRIME = 31;\r\n" + 
				"		int result = 1;\r\n" + 
				"		result = PRIME * result + (aBool ? 1231 : 1237);\r\n" + 
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
				"		final A other = (A) obj;\r\n" + 
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
		runOperation(a.getType("A"), fields, true);
		
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
				"		final int PRIME = 31;\r\n" + 
				"		int result = 1;\r\n" + 
				"		result = PRIME * result + ((anA == null) ? 0 : anA.hashCode());\r\n" + 
				"		result = PRIME * result + ((aB == null) ? 0 : aB.hashCode());\r\n" + 
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
				"		final A other = (A) obj;\r\n" + 
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
	
}
