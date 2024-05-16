/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Pierre-Yves B. <pyvesdev@gmail.com> - Generation of equals and hashcode with java 7 Objects.equals and Objects.hashcode - https://bugs.eclipse.org/424214
 *     Pierre-Yves B. <pyvesdev@gmail.com> - Different behaviour when generating hashCode and equals - https://bugs.eclipse.org/539589
 *     Pierre-Yves B. <pyvesdev@gmail.com> - Confusing name when generating hashCode and equals with outer type - https://bugs.eclipse.org/539872
 *     Pierre-Yves B. <pyvesdev@gmail.com> - Allow hashCode and equals generation when no fields but a super/enclosing class that implements them - https://bugs.eclipse.org/539901
 *     Pierre-Yves B. <pyvesdev@gmail.com> - [hashcode/equals] Redundant null check when instanceof is used - https://bugs.eclipse.org/545424
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.codemanipulation.GenerateHashCodeEqualsOperation;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests generation of delegate methods
 */
public class GenerateHashCodeEqualsTest extends SourceTestCase {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	public void runOperation(IType type, IField[] fields, IJavaElement insertBefore, boolean createComments, boolean useInstanceof, boolean useJ7HashEquals, boolean useBlocks, boolean force) throws CoreException {

		RefactoringASTParser parser= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);

		IVariableBinding[] fKeys= new IVariableBinding[fields.length];
		for (int i= 0; i < fields.length; i++) {
			assertTrue(fields[i].exists());
			VariableDeclarationFragment frag= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(fields[i], unit);
			fKeys[i]= frag.resolveBinding();
		}

		fSettings.createComments= createComments;

		AbstractTypeDeclaration decl= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(type, unit);
		ITypeBinding binding= decl.resolveBinding();
		GenerateHashCodeEqualsOperation op= new GenerateHashCodeEqualsOperation(binding, fKeys, unit, insertBefore, fSettings, useInstanceof, useJ7HashEquals, force, true, true);
		op.setUseBlocksForThen(useBlocks);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(type.getCompilationUnit());
	}

	public void runOperation(IType type, IField[] fields, boolean useInstanceof, boolean force) throws CoreException {
		runOperation(type, fields, null, true, useInstanceof, false, false, force);
	}

	public void runJ7Operation(IType type, IField[] fields, boolean useInstanceof) throws CoreException {
		runOperation(type, fields, null, true, useInstanceof, true, false, false);
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
	 */
	@Test
	public void test01() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				byte aByte;\r
				char aChar;\r
				int anInt;\r
				double aDouble;\r
				float aFloat;\r
				long aLong;\r
				java.lang.annotation.ElementType anEnum;\r
			\r
			}""", true, null);

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
	 */
	@Test
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
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + (aBool ? 1231 : 1237);\r\n" +
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
	 */
	@Test
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
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + ((anA == null) ? 0 : anA.hashCode());\r\n" +
				"		result = prime * result + ((aB == null) ? 0 : aB.hashCode());\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
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
	 */
	@Test
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
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + ((anA == null) ? 0 : anA.hashCode());\r\n" +
				"		result = prime * result + ((aB == null) ? 0 : aB.hashCode());\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
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
	 */
	@Test
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
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + Arrays.hashCode(someAs);\r\n" +
				"		result = prime * result + Arrays.hashCode(someInts);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
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
	 */
	@Test
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
		runOperation(a.getType("A"), fields, a.getType("A").getMethod("bar", new String[0]), true, false, false, false, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	int someInt;\r\n" +
				"	\r\n" +
				"	public void foo() {}\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + someInt;\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
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
	 */
	@Test
	public void test07() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
				List<Integer> intList;\r
				HashMap<Integer, List<Boolean>> intBoolHashMap;\r
				E someEnum;\r
			}\r
			\r
			enum E {\r
			}""", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"intList", "intBoolHashMap", "someEnum" });
		runOperation(a.getType("A"), fields, false, false);

		String expected= """
			package p;\r
			\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
				List<Integer> intList;\r
				HashMap<Integer, List<Boolean>> intBoolHashMap;\r
				E someEnum;\r
				@Override\r
				public int hashCode() {\r
					final int prime = 31;\r
					int result = 1;\r
					result = prime * result + ((intList == null) ? 0 : intList.hashCode());\r
					result = prime * result + ((intBoolHashMap == null) ? 0 : intBoolHashMap.hashCode());\r
					result = prime * result + ((someEnum == null) ? 0 : someEnum.hashCode());\r
					return result;\r
				}\r
				@Override\r
				public boolean equals(Object obj) {\r
					if (this == obj)\r
						return true;\r
					if (obj == null)\r
						return false;\r
					if (getClass() != obj.getClass())\r
						return false;\r
					A other = (A) obj;\r
					if (intList == null) {\r
						if (other.intList != null)\r
							return false;\r
					} else if (!intList.equals(other.intList))\r
						return false;\r
					if (intBoolHashMap == null) {\r
						if (other.intBoolHashMap != null)\r
							return false;\r
					} else if (!intBoolHashMap.equals(other.intBoolHashMap))\r
						return false;\r
					if (someEnum != other.someEnum)\r
						return false;\r
					return true;\r
				}\r
			}\r
			\r
			enum E {\r
			}""";

		compareSource(expected, a.getSource());
	}

	/**
	 * Two double fields
	 */
	@Test
	public void test08() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				double d1;\r
				double d2;\r
			}""", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"d1", "d2" });
		runOperation(a.getType("A"), fields, false, false);

		String expected= """
			package p;\r
			\r
			public class A {\r
				double d1;\r
				double d2;\r
				@Override\r
				public int hashCode() {\r
					final int prime = 31;\r
					int result = 1;\r
					long temp;\r
					temp = Double.doubleToLongBits(d1);\r
					result = prime * result + (int) (temp ^ (temp >>> 32));\r
					temp = Double.doubleToLongBits(d2);\r
					result = prime * result + (int) (temp ^ (temp >>> 32));\r
					return result;\r
				}\r
				@Override\r
				public boolean equals(Object obj) {\r
					if (this == obj)\r
						return true;\r
					if (obj == null)\r
						return false;\r
					if (getClass() != obj.getClass())\r
						return false;\r
					A other = (A) obj;\r
					if (Double.doubleToLongBits(d1) != Double.doubleToLongBits(other.d1))\r
						return false;\r
					if (Double.doubleToLongBits(d2) != Double.doubleToLongBits(other.d2))\r
						return false;\r
					return true;\r
				}\r
			}""";

		compareSource(expected, a.getSource());
	}

	/**
	 * "this" qualification for fields with the
	 * same name as a newly introduced temporary
	 */
	@Test
	public void test09() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				double temp;\r
				boolean result;\r
				String obj;\r
				double someOther;\r
			}""", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"temp", "result", "obj", "someOther" });
		runOperation(a.getType("A"), fields, false, false);

		String expected= """
			package p;\r
			\r
			public class A {\r
				double temp;\r
				boolean result;\r
				String obj;\r
				double someOther;\r
				@Override\r
				public int hashCode() {\r
					final int prime = 31;\r
					int result = 1;\r
					long temp;\r
					temp = Double.doubleToLongBits(this.temp);\r
					result = prime * result + (int) (temp ^ (temp >>> 32));\r
					result = prime * result + (this.result ? 1231 : 1237);\r
					result = prime * result + ((obj == null) ? 0 : obj.hashCode());\r
					temp = Double.doubleToLongBits(someOther);\r
					result = prime * result + (int) (temp ^ (temp >>> 32));\r
					return result;\r
				}\r
				@Override\r
				public boolean equals(Object obj) {\r
					if (this == obj)\r
						return true;\r
					if (obj == null)\r
						return false;\r
					if (getClass() != obj.getClass())\r
						return false;\r
					A other = (A) obj;\r
					if (Double.doubleToLongBits(temp) != Double.doubleToLongBits(other.temp))\r
						return false;\r
					if (result != other.result)\r
						return false;\r
					if (this.obj == null) {\r
						if (other.obj != null)\r
							return false;\r
					} else if (!this.obj.equals(other.obj))\r
						return false;\r
					if (Double.doubleToLongBits(someOther) != Double.doubleToLongBits(other.someOther))\r
						return false;\r
					return true;\r
				}\r
			}""";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test non-reference types in a direct subclass of Object, using 'instanceof' comparison
	 */
	@Test
	public void test10() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				byte aByte;\r
				char aChar;\r
				int anInt;\r
				double aDouble;\r
				float aFloat;\r
				long aLong;\r
			\r
			}""", true, null);

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
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
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
	 */
	@Test
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
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + (aBool ? 1231 : 1237);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
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
	 */
	@Test
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
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + ((anA == null) ? 0 : anA.hashCode());\r\n" +
				"		result = prime * result + ((aB == null) ? 0 : aB.hashCode());\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
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
	 */
	@Test
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
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + ((anA == null) ? 0 : anA.hashCode());\r\n" +
				"		result = prime * result + ((aB == null) ? 0 : aB.hashCode());\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
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
	 * Test with J7+ Objects.hash and Objects.equals method calls
	 * Using non-array instance variables and Enum
	 */
	@Test
	public void hashCodeEqualsIn17() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"import java.util.List;\r\n" +
				"import java.lang.annotation.ElementType;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	ElementType anEnum;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] { "aBool", "aByte", "aChar", "anInt", "aDouble", "aFloat", "aLong", "aString", "aListOfStrings", "anEnum" });
		runJ7Operation(a.getType("A"), fields, false);

		String expected= "package p;\r\n" +
				"import java.util.List;\r\n" +
				"import java.util.Objects;\r\n" +
				"import java.lang.annotation.ElementType;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	ElementType anEnum;\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		return Objects.hash(aBool, aByte, aChar, anInt, aDouble, aFloat, aLong, aString, aListOfStrings, anEnum);\r\n" +
				"	}\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		return aBool == other.aBool && aByte == other.aByte && aChar == other.aChar && anInt == other.anInt && Double.doubleToLongBits(aDouble) == Double.doubleToLongBits(other.aDouble) && Float.floatToIntBits(aFloat) == Float.floatToIntBits(other.aFloat) && aLong == other.aLong && Objects.equals(aString, other.aString) && Objects.equals(aListOfStrings, other.aListOfStrings) && anEnum == other.anEnum;\r\n"
				+
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test with J7+ Objects.hash and Objects.equals method calls
	 * Using unique non-array instance variables
	 */
	@Test
	public void hashCodeEqualsUniqueFieldIn17() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"public class A {\r\n" +
				"	String aString;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] { "aString" });
		runJ7Operation(a.getType("A"), fields, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"import java.util.Objects;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	String aString;\r\n" +
				"\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		return Objects.hash(aString);\r\n" +
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
				"		A other = (A) obj;\r\n" +
				"		return Objects.equals(aString, other.aString);\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test with J7+ Objects.hash and Objects.equals method calls
	 * Using non-array instance variables with 'instanceof' comparison
	 */
	@Test
	public void hashCodeEqualsInstanceOfIn17() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"import java.util.List;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] { "aBool", "aByte", "aChar", "anInt", "aDouble", "aFloat", "aLong", "aString", "aListOfStrings" });
		runJ7Operation(a.getType("A"), fields, true);

		String expected= "package p;\r\n" +
				"import java.util.List;\r\n" +
				"import java.util.Objects;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		return Objects.hash(aBool, aByte, aChar, anInt, aDouble, aFloat, aLong, aString, aListOfStrings);\r\n" +
				"	}\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (!(obj instanceof A))\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		return aBool == other.aBool && aByte == other.aByte && aChar == other.aChar && anInt == other.anInt && Double.doubleToLongBits(aDouble) == Double.doubleToLongBits(other.aDouble) && Float.floatToIntBits(aFloat) == Float.floatToIntBits(other.aFloat) && aLong == other.aLong && Objects.equals(aString, other.aString) && Objects.equals(aListOfStrings, other.aListOfStrings);\r\n"
				+
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test with J7+ Objects.hash and Objects.equals method calls
	 * Using 1-dim array amongst other instance variables
	 */
	@Test
	public void hashCodeEqualsArrayIn17() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"import java.util.List;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	int[] anArrayOfInts;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] { "aBool", "aByte", "aChar", "anInt", "aDouble", "aFloat", "aLong", "aString", "aListOfStrings", "anArrayOfInts" });
		runJ7Operation(a.getType("A"), fields, false);

		String expected= "package p;\r\n" +
				"import java.util.Arrays;\r\n" +
				"import java.util.List;\r\n" +
				"import java.util.Objects;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	int[] anArrayOfInts;\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + Arrays.hashCode(anArrayOfInts);\r\n" +
				"		result = prime * result + Objects.hash(aBool, aByte, aChar, anInt, aDouble, aFloat, aLong, aString, aListOfStrings);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		return aBool == other.aBool && aByte == other.aByte && aChar == other.aChar && anInt == other.anInt && Double.doubleToLongBits(aDouble) == Double.doubleToLongBits(other.aDouble) && Float.floatToIntBits(aFloat) == Float.floatToIntBits(other.aFloat) && aLong == other.aLong && Objects.equals(aString, other.aString) && Objects.equals(aListOfStrings, other.aListOfStrings) && Arrays.equals(anArrayOfInts, other.anArrayOfInts);\r\n"
				+
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test with J7+ Objects.hash and Objects.equals method calls
	 * Using 1-dim Cloneable array amongst other instance variables
	 */
	@Test
	public void hashCodeEqualsCloneableArrayIn17() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"import java.util.List;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	Cloneable[] anArrayOfCloneables;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] { "aBool", "aByte", "aChar", "anInt", "aDouble", "aFloat", "aLong", "aString", "aListOfStrings", "anArrayOfCloneables" });
		runJ7Operation(a.getType("A"), fields, false);

		String expected= "package p;\r\n" +
				"import java.util.Arrays;\r\n" +
				"import java.util.List;\r\n" +
				"import java.util.Objects;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	Cloneable[] anArrayOfCloneables;\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + Arrays.deepHashCode(anArrayOfCloneables);\r\n" +
				"		result = prime * result + Objects.hash(aBool, aByte, aChar, anInt, aDouble, aFloat, aLong, aString, aListOfStrings);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		return aBool == other.aBool && aByte == other.aByte && aChar == other.aChar && anInt == other.anInt && Double.doubleToLongBits(aDouble) == Double.doubleToLongBits(other.aDouble) && Float.floatToIntBits(aFloat) == Float.floatToIntBits(other.aFloat) && aLong == other.aLong && Objects.equals(aString, other.aString) && Objects.equals(aListOfStrings, other.aListOfStrings) && Arrays.deepEquals(anArrayOfCloneables, other.anArrayOfCloneables);\r\n"
				+
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test with J7+ Objects.hash and Objects.equals method calls
	 * Using 1-dim Serializable array amongst other instance variables
	 */
	@Test
	public void hashCodeEqualsSerializableArrayIn17() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"import java.util.List;\r\n" +
				"import java.io.Serializable;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	Serializable[] anArrayOfSerializables;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] { "aBool", "aByte", "aChar", "anInt", "aDouble", "aFloat", "aLong", "aString", "aListOfStrings", "anArrayOfSerializables" });
		runJ7Operation(a.getType("A"), fields, false);

		String expected= "package p;\r\n" +
				"import java.util.Arrays;\r\n" +
				"import java.util.List;\r\n" +
				"import java.util.Objects;\r\n" +
				"import java.io.Serializable;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	Serializable[] anArrayOfSerializables;\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + Arrays.deepHashCode(anArrayOfSerializables);\r\n" +
				"		result = prime * result + Objects.hash(aBool, aByte, aChar, anInt, aDouble, aFloat, aLong, aString, aListOfStrings);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		return aBool == other.aBool && aByte == other.aByte && aChar == other.aChar && anInt == other.anInt && Double.doubleToLongBits(aDouble) == Double.doubleToLongBits(other.aDouble) && Float.floatToIntBits(aFloat) == Float.floatToIntBits(other.aFloat) && aLong == other.aLong && Objects.equals(aString, other.aString) && Objects.equals(aListOfStrings, other.aListOfStrings) && Arrays.deepEquals(anArrayOfSerializables, other.anArrayOfSerializables);\r\n"
				+
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test with J7+ Objects.hash and Objects.equals method calls
	 * Using 1-dim Object array amongst other instance variables
	 */
	@Test
	public void hashCodeEqualsObjectArrayIn17() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"import java.util.List;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	Object[] anArrayOfObjects;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] { "aBool", "aByte", "aChar", "anInt", "aDouble", "aFloat", "aLong", "aString", "aListOfStrings", "anArrayOfObjects" });
		runJ7Operation(a.getType("A"), fields, false);

		String expected= "package p;\r\n" +
				"import java.util.Arrays;\r\n" +
				"import java.util.List;\r\n" +
				"import java.util.Objects;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	Object[] anArrayOfObjects;\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + Arrays.deepHashCode(anArrayOfObjects);\r\n" +
				"		result = prime * result + Objects.hash(aBool, aByte, aChar, anInt, aDouble, aFloat, aLong, aString, aListOfStrings);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		return aBool == other.aBool && aByte == other.aByte && aChar == other.aChar && anInt == other.anInt && Double.doubleToLongBits(aDouble) == Double.doubleToLongBits(other.aDouble) && Float.floatToIntBits(aFloat) == Float.floatToIntBits(other.aFloat) && aLong == other.aLong && Objects.equals(aString, other.aString) && Objects.equals(aListOfStrings, other.aListOfStrings) && Arrays.deepEquals(anArrayOfObjects, other.anArrayOfObjects);\r\n"
				+
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test with J7+ Objects.hash and Objects.equals method calls
	 * Using 1-dim type variable arrays extending Serializable and Number
	 */
	@Test
	public void hashCodeEqualsTypeVariableArrayIn17() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"import java.io.Serializable;\r\n" +
				"public class A <S extends Serializable, N extends Number> {\r\n" +
				"	S[] anArrayOfS;\r\n" +
				"	N[] anArrayOfN;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] { "anArrayOfS", "anArrayOfN" });
		runJ7Operation(a.getType("A"), fields, false);

		String expected= "package p;\r\n" +
				"import java.io.Serializable;\r\n" +
				"import java.util.Arrays;\r\n" +
				"public class A <S extends Serializable, N extends Number> {\r\n" +
				"	S[] anArrayOfS;\r\n" +
				"	N[] anArrayOfN;\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + Arrays.deepHashCode(anArrayOfS);\r\n" +
				"		result = prime * result + Arrays.hashCode(anArrayOfN);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		return Arrays.deepEquals(anArrayOfS, other.anArrayOfS) && Arrays.equals(anArrayOfN, other.anArrayOfN);\r\n"
				+
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test with J7+ Objects.hash and Objects.equals method calls
	 * Using multidimensional array amongst other instance variables
	 */
	@Test
	public void hashCodeEqualsMultiArrayIn17() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"import java.util.List;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	int[][] anArrayOfInts;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] { "aBool", "aByte", "aChar", "anInt", "aDouble", "aFloat", "aLong", "aString", "aListOfStrings", "anArrayOfInts" });
		runJ7Operation(a.getType("A"), fields, false);

		String expected= "package p;\r\n" +
				"import java.util.Arrays;\r\n" +
				"import java.util.List;\r\n" +
				"import java.util.Objects;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	int[][] anArrayOfInts;\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + Arrays.deepHashCode(anArrayOfInts);\r\n" +
				"		result = prime * result + Objects.hash(aBool, aByte, aChar, anInt, aDouble, aFloat, aLong, aString, aListOfStrings);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		return aBool == other.aBool && aByte == other.aByte && aChar == other.aChar && anInt == other.anInt && Double.doubleToLongBits(aDouble) == Double.doubleToLongBits(other.aDouble) && Float.floatToIntBits(aFloat) == Float.floatToIntBits(other.aFloat) && aLong == other.aLong && Objects.equals(aString, other.aString) && Objects.equals(aListOfStrings, other.aListOfStrings) && Arrays.deepEquals(anArrayOfInts, other.anArrayOfInts);\r\n"
				+
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test with J7+ Objects.hash and Objects.equals method calls
	 * Using both multidimensional and 1-dimensional primitive arrays amongst other instance variables
	 */
	@Test
	public void hashCodeEqualsVariousArraysIn17() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"import java.util.List;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	int[] anArrayOfInts;\r\n" +
				"	String[][] anArrayOfStrings;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] { "aBool", "aByte", "aChar", "anInt", "aDouble", "aFloat", "aLong", "aString", "aListOfStrings", "anArrayOfInts", "anArrayOfStrings" });
		runJ7Operation(a.getType("A"), fields, false);

		String expected= "package p;\r\n" +
				"import java.util.Arrays;\r\n" +
				"import java.util.List;\r\n" +
				"import java.util.Objects;\r\n" +
				"public class A {\r\n" +
				"	boolean aBool;\r\n" +
				"	byte aByte;\r\n" +
				"	char aChar;\r\n" +
				"	int anInt;\r\n" +
				"	double aDouble;\r\n" +
				"	float aFloat;\r\n" +
				"	long aLong;\r\n" +
				"	String aString;\r\n" +
				"	List<String> aListOfStrings;\r\n" +
				"	int[] anArrayOfInts;\r\n" +
				"	String[][] anArrayOfStrings;\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + Arrays.hashCode(anArrayOfInts);\r\n" +
				"		result = prime * result + Arrays.deepHashCode(anArrayOfStrings);\r\n" +
				"		result = prime * result + Objects.hash(aBool, aByte, aChar, anInt, aDouble, aFloat, aLong, aString, aListOfStrings);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		return aBool == other.aBool && aByte == other.aByte && aChar == other.aChar && anInt == other.anInt && Double.doubleToLongBits(aDouble) == Double.doubleToLongBits(other.aDouble) && Float.floatToIntBits(aFloat) == Float.floatToIntBits(other.aFloat) && aLong == other.aLong && Objects.equals(aString, other.aString) && Objects.equals(aListOfStrings, other.aListOfStrings) && Arrays.equals(anArrayOfInts, other.anArrayOfInts) && Arrays.deepEquals(anArrayOfStrings, other.anArrayOfStrings);\r\n"
				+
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test with J7+ Objects.hash and Objects.equals method calls
	 * Using ONLY multidimensional and 1-dimensional arrays as instance variables
	 */
	@Test
	public void hashCodeEqualsOnlyArraysIn17() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"public class A {\r\n" +
				"	int[] anArrayOfInts;\r\n" +
				"	String[][] anArrayOfStrings;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] { "anArrayOfInts", "anArrayOfStrings" });
		runJ7Operation(a.getType("A"), fields, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"import java.util.Arrays;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	int[] anArrayOfInts;\r\n" +
				"	String[][] anArrayOfStrings;\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + Arrays.hashCode(anArrayOfInts);\r\n" +
				"		result = prime * result + Arrays.deepHashCode(anArrayOfStrings);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (obj == null)\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		return Arrays.equals(anArrayOfInts, other.anArrayOfInts) && Arrays.deepEquals(anArrayOfStrings, other.anArrayOfStrings);\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test member types
	 */
	@Test
	public void enclosingInstance() throws Exception {

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
				"		@Override\r\n" +
				"		public int hashCode() {\r\n" +
				"			final int prime = 31;\r\n" +
				"			int result = 1;\r\n" +
				"			result = prime * result + getEnclosingInstance().hashCode();\r\n" +
				"			result = prime * result + x;\r\n" +
				"			return result;\r\n" +
				"		}\r\n" +
				"\r\n" +
				"		@Override\r\n" +
				"		public boolean equals(Object obj) {\r\n" +
				"			if (this == obj)\r\n" +
				"				return true;\r\n" +
				"			if (!(obj instanceof Inner))\r\n" +
				"				return false;\r\n" +
				"			Inner other = (Inner) obj;\r\n" +
				"			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))\r\n" +
				"				return false;\r\n" +
				"			if (x != other.x)\r\n" +
				"				return false;\r\n" +
				"			return true;\r\n" +
				"		}\r\n" +
				"\r\n" +
				"		private A getEnclosingInstance() {\r\n" +
				"			return A.this;\r\n" +
				"		}\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test member types with J7+ Objects.hash and Objects.equals method calls
	 */
	@Test
	public void enclosingInstanceIn17() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	public class Inner {\r\n" +
				"		int x;\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"", true, null);

		IType type= a.getType("A").getType("Inner");
		IField[] fields= getFields(type, new String[] { "x" });
		runJ7Operation(type, fields, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"import java.util.Objects;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	public class Inner {\r\n" +
				"		int x;\r\n" +
				"\r\n" +
				"		@Override\r\n" +
				"		public int hashCode() {\r\n" +
				"			final int prime = 31;\r\n" +
				"			int result = 1;\r\n" +
				"			result = prime * result + getEnclosingInstance().hashCode();\r\n" +
				"			result = prime * result + Objects.hash(x);\r\n" +
				"			return result;\r\n" +
				"		}\r\n" +
				"\r\n" +
				"		@Override\r\n" +
				"		public boolean equals(Object obj) {\r\n" +
				"			if (this == obj)\r\n" +
				"				return true;\r\n" +
				"			if (obj == null)\r\n" +
				"				return false;\r\n" +
				"			if (getClass() != obj.getClass())\r\n" +
				"				return false;\r\n" +
				"			Inner other = (Inner) obj;\r\n" +
				"			if (!getEnclosingInstance().equals(other.getEnclosingInstance()))\r\n" +
				"				return false;\r\n" +
				"			return x == other.x;\r\n" +
				"		}\r\n" +
				"\r\n" +
				"		private A getEnclosingInstance() {\r\n" +
				"			return A.this;\r\n" +
				"		}\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test non-reference types in a direct subclass of Object
	 */
	@Test
	public void thenWithBlocks() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				Object obj;\r
			\r
			}""", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] {"aBool", "obj" });
		runOperation(a.getType("A"), fields, null, true, false, false, true, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	\r\n" +
				"	boolean aBool;\r\n" +
				"	Object obj;\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = 1;\r\n" +
				"		result = prime * result + (aBool ? 1231 : 1237);\r\n" +
				"		result = prime * result + ((obj == null) ? 0 : obj.hashCode());\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
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
	 */
	@Test
	public void subTypeAndArraysIn14() throws Exception {
		IJavaProject javaProject= fPackageP.getJavaProject();
		Map<String, String> oldOptions= javaProject.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(oldOptions);
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
					"	public int hashCode() {\r\n" +
					"		final int prime = 31;\r\n" +
					"		int result = super.hashCode();\r\n" +
					"		result = prime * result + A.hashCode(anArray);\r\n" +
					"		result = prime * result + A.hashCode(anDArray);\r\n" +
					"		return result;\r\n" +
					"	}\r\n" +
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

	/**
	 * Test with J7+ Objects.hash and Objects.equals method calls Using sub-type
	 */
	@Test
	public void subTypeIn17() throws Exception {
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
				"	String aString;\r\n" +
				"\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] { "aString" });
		runJ7Operation(a.getType("A"), fields, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"import java.util.Objects;\r\n" +
				"\r\n" +
				"public class A extends B {\r\n" +
				"\r\n" +
				"	String aString;\r\n" +
				"\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		final int prime = 31;\r\n" +
				"		int result = super.hashCode();\r\n" +
				"		result = prime * result + Objects.hash(aString);\r\n" +
				"		return result;\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (!super.equals(obj))\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		A other = (A) obj;\r\n" +
				"		return Objects.equals(aString, other.aString);\r\n" +
				"	}\r\n" +
				"\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test with J7+ Objects.hash and Objects.equals method calls with class with "other" field
	 * (https://bugs.eclipse.org/bugs/show_bug.cgi?id=561517)
	 */
	public void otherFieldIn17() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	private String other;\r\n" +
				"}\r\n" +
				"", true, null);

		IField[] fields= getFields(a.getType("A"), new String[] { "other" });
		runJ7Operation(a.getType("A"), fields, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"import java.util.Objects;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	private String other;\r\n" +
				"\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		return Objects.hash(other);\r\n" +
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
				"		A other = (A) obj;\r\n" +
				"		return Objects.equals(this.other, other.other);\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	/**
	 * Test implementation based only on super class
	 */
	@Test
	public void subTypeNoFields() throws Exception {
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
				"}\r\n" +
				"", true, null);

		runOperation(a.getType("A"), new IField[0], false, false);

		String expected= "package p;\r\n" +
				"\r\n" +
				"public class A extends B {\r\n" +
				"\r\n" +
				"	@Override\r\n" +
				"	public int hashCode() {\r\n" +
				"		return super.hashCode();\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	@Override\r\n" +
				"	public boolean equals(Object obj) {\r\n" +
				"		if (this == obj)\r\n" +
				"			return true;\r\n" +
				"		if (!super.equals(obj))\r\n" +
				"			return false;\r\n" +
				"		if (getClass() != obj.getClass())\r\n" +
				"			return false;\r\n" +
				"		return true;\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"";

		compareSource(expected, a.getSource());
	}

	@Test
	public void insertAt() throws Exception {
		String originalContent= """
			package p;
			
			public class A  {
				Runnable x;
			\t
				A() {
				}
			\t
				void foo() {
				}
			\t
				{
				}
			\t
				static {
				}
			\t
				class Inner {
				}
			}""";

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

				runOperation(type, new IField[] { foo }, insertBefore, false, false, false, false, false);

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
	@Test
	public void abstractSuperMethods() throws Exception {

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
		runOperation(a.getType("Sub"), fields, null, false, false, false, false, false);

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

	/**
	 * Test that generated equals() should use Arrays.deepEquals() instead of Arrays.equals() for projects with compliance >= 1.5
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=304176
	 *
	 * @throws Exception rarely
	 */
	@Test
	public void arraysDeepEqualsIn15() throws Exception {
		IJavaProject javaProject= fPackageP.getJavaProject();
		Map<String, String> oldOptions= javaProject.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(oldOptions);
		JavaModelUtil.setComplianceOptions(newOptions, JavaCore.VERSION_1_5);
		javaProject.setOptions(newOptions);
		try {
			ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
					"\r\n" +
					"public class A {\r\n" +
					"\r\n" +
					"	 int[][][] a = new int[][][] {{null}};\r\n" +
					"	 int[][][] b = new int[][][] {{null}};\r\n" +
					"\r\n" +
					"}\r\n" +
					"", true, null);

			IField[] fields= getFields(a.getType("A"), new String[] {"a", "b"});
			runOperation(a.getType("A"), fields, false, false);

			String expected= "package p;\r\n" +
					"\r\n" +
					"import java.util.Arrays;\r\n" +
					"\r\n" +
					"public class A {\r\n" +
					"\r\n" +
					"	 int[][][] a = new int[][][] {{null}};\r\n" +
					"	 int[][][] b = new int[][][] {{null}};\r\n" +
					"	@Override\r\n" +
					"	public int hashCode() {\r\n" +
					"		final int prime = 31;\r\n" +
					"		int result = 1;\r\n" +
					"		result = prime * result + Arrays.deepHashCode(a);\r\n" +
					"		result = prime * result + Arrays.deepHashCode(b);\r\n" +
					"		return result;\r\n" +
					"	}\r\n" +
					"	@Override\r\n" +
					"	public boolean equals(Object obj) {\r\n" +
					"		if (this == obj)\r\n" +
					"			return true;\r\n" +
					"		if (obj == null)\r\n" +
					"			return false;\r\n" +
					"		if (getClass() != obj.getClass())\r\n" +
					"			return false;\r\n" +
					"		A other = (A) obj;\r\n" +
					"		if (!Arrays.deepEquals(a, other.a))\r\n" +
					"			return false;\r\n" +
					"		if (!Arrays.deepEquals(b, other.b))\r\n" +
					"			return false;\r\n" +
					"		return true;\r\n" +
					"	}\r\n" +
					"\r\n" +
					"}" +
					"";
			compareSource(expected, a.getSource());
		} finally {
			javaProject.setOptions(oldOptions);
		}
	}
}