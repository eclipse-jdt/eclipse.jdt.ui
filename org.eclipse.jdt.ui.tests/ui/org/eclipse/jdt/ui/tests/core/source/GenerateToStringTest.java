/*******************************************************************************
 * Copyright (c) 2008, 2020 Mateusz Matela and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mateusz Matela <mateusz.matela@gmail.com> - [code manipulation] [dcr] toString() builder wizard - https://bugs.eclipse.org/bugs/show_bug.cgi?id=26070
 *     Mateusz Matela <mateusz.matela@gmail.com> - [toString] finish toString() builder wizard - https://bugs.eclipse.org/bugs/show_bug.cgi?id=267710
 *     Mateusz Matela <mateusz.matela@gmail.com> - [toString] toString wizard generates wrong code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=270462
 *     Mateusz Matela <mateusz.matela@gmail.com> - [toString] Wrong code generated with String concatenation - https://bugs.eclipse.org/bugs/show_bug.cgi?id=275360
 *     Mateusz Matela <mateusz.matela@gmail.com> - [toString] Generator uses wrong suffixes and prefixes - https://bugs.eclipse.org/bugs/show_bug.cgi?id=275370
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core.source;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.codeassist.impl.AssistOptions;
import org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration.GenerateToStringOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration.ToStringGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration.ToStringGenerationSettingsCore.CustomBuilderSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.tostringgeneration.ToStringTemplateParser;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class GenerateToStringTest extends SourceTestCase {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	protected ToStringGenerationSettings fSettings2;

	@Before
	public void before() throws CoreException {
		fSettings2= new ToStringGenerationSettings();
		fSettings.setSettings(fSettings2);
		fSettings2.stringFormatTemplate= ToStringTemplateParser.DEFAULT_TEMPLATE;
		fSettings2.toStringStyle= 0;
		fSettings2.skipNulls= false;
		fSettings2.createComments= false;
		fSettings2.customArrayToString= false;
		fSettings2.limitElements= false;
		fSettings2.limitValue= 10;
		fSettings2.useBlocks= true;
		setCompilerLevels(true, true);
		fSettings2.customBuilderSettings= new CustomBuilderSettings();
		fSettings2.customBuilderSettings.className= "com.pack.ToStringBuilder";
		fSettings2.customBuilderSettings.variableName= "builder";
		fSettings2.customBuilderSettings.appendMethod= "append";
		fSettings2.customBuilderSettings.resultMethod= "toString";
		fSettings2.customBuilderSettings.chainCalls= false;

		IPackageFragment packageFragment= fRoot.createPackageFragment("com.pack", true, null);
		ICompilationUnit compilationUnit= packageFragment.getCompilationUnit("ToStringBuilder.java");
		compilationUnit
				.createType(
						"package com.pack;\npublic class ToStringBuilder {\npublic ToStringBuilder(Object o){\n}\npublic ToStringBuilder append(String s, Object o){\nreturn null;\n}\npublic String toString(){\nreturn null;\n}\n}\n",
						null, true, null);

		packageFragment= fRoot.createPackageFragment("org.another.pack", true, null);
		compilationUnit= packageFragment.getCompilationUnit("AnotherToStringCreator.java");
		compilationUnit
				.createType(
						"package org.another.pack;\npublic class AnotherToStringCreator {\npublic AnotherToStringCreator(java.lang.Object o) {\n}\npublic AnotherToStringCreator addSth(Object o, String s) {\n return null;\n}\npublic String addSth(String s, int i){\nreturn null;\n}\npublic void addSth(boolean b, String s){\n}\npublic String getResult(){\nreturn null;\n}\n}\n",
						null, true, null);
	}

	private void setCompilerLevels(boolean is50orHigher, boolean is60orHigher) throws CoreException {
		fSettings2.is50orHigher= is50orHigher;
		fSettings2.is60orHigher= is60orHigher;
		IJavaProject jp= fRoot.getJavaProject();
		if (is60orHigher || ! is50orHigher) {
			ArrayList<IClasspathEntry> newCP= new ArrayList<>();
			for (IClasspathEntry cpe : jp.getRawClasspath()) {
				if (cpe.getEntryKind() != IClasspathEntry.CPE_LIBRARY) {
					newCP.add(cpe);
				}
			}
			jp.setRawClasspath(newCP.toArray(new IClasspathEntry[newCP.size()]), null);
		}
		if (is60orHigher) {
			JavaProjectHelper.addRTJar18(jp);
		}
		if (!is50orHigher) {
			JavaProjectHelper.addRTJar18(jp);
		}
	}

	public void runOperation(IType type, IMember[] members, IJavaElement insertBefore) throws CoreException {

		RefactoringASTParser parser= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);

		Object[] fKeys= new Object[members.length];
		for (int i= 0; i < members.length; i++) {
			Assert.assertTrue(members[i].exists());
			if (members[i] instanceof IField) {
				VariableDeclarationFragment frag= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField)members[i], unit);
				fKeys[i]= frag.resolveBinding();
				continue;
			}
			if (members[i] instanceof IMethod) {
				MethodDeclaration decl= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod)members[i], unit);
				fKeys[i]= decl.resolveBinding();
				continue;
			}
			Assert.fail("Member " + members[i] + " not a field nor a method");
		}

		AbstractTypeDeclaration decl= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(type, unit);
		ITypeBinding binding= decl.resolveBinding();
		GenerateToStringOperation op= GenerateToStringOperation.createOperation(binding, fKeys, unit, insertBefore, fSettings2, true, true);
		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(type.getCompilationUnit());
	}

	private IMember[] getMembers(IType type, String[] names) {
		IMember[] members= new IMember[names.length];
		for (int i= 0; i < members.length; i++) {
			members[i]= type.getField(names[i]);
			if (!members[i].exists())
				members[i]= type.getMethod(names[i], new String[0]);
			Assert.assertTrue(members[i].exists());
		}
		return members;
	}

	/**
	 * Compares source with expected and asserts that no new compile errors have been created.
	 *
	 * @param expected source
	 * @param cu compilation unit
	 * @param oldCUNode the old AST root
	 * @throws Exception if test failed if test failed
	 *
	 * @since 3.5
	 */
	private void compareSourceAssertCompilation(String expected, ICompilationUnit cu, CompilationUnit oldCUNode) throws Exception {
		compareSource(expected, cu.getSource());
		CompilationUnit newCUNode= getCUNode(cu);
		for (IProblem problem : RefactoringAnalyzeUtil.getIntroducedCompileProblems(newCUNode, oldCUNode)) {
			assertFalse(problem.toString(), problem.isError());
		}
	}

	private static CompilationUnit getCUNode(ICompilationUnit cu) throws Exception {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setSource(cu);
		return (CompilationUnit)parser.createAST(null);
	}

	/**
	 * string concatenation - basic functionality and comment
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatComment() throws Exception {

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
				int aFloatMethod() {\r
					return 3.3;\r
				}\r
				int aStringMethod() {\r
					return "";\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aBool", "aByte", "aChar", "anInt", "aDouble", "aFloat", "aLong", "aFloatMethod", "aStringMethod" });
		fSettings2.createComments= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
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
				int aFloatMethod() {\r
					return 3.3;\r
				}\r
				int aStringMethod() {\r
					return "";\r
				}\r
				@Override\r
				public String toString() {\r
					return "A [aBool=" + aBool + ", aByte=" + aByte + ", aChar=" + aChar + ", anInt=" + anInt + ", aDouble=" + aDouble + ", aFloat=" + aFloat + ", aLong=" + aLong + ", aFloatMethod()=" + aFloatMethod() + ", aStringMethod()=" + aStringMethod() + "]";\r
				}\r
			\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.skipNulls= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					return "A [" + (aStringMethod() != null ? "aStringMethod()=" + aStringMethod() + ", " : "") + "aFloatMethod()=" + aFloatMethod() + ", " + (anArrayMethod() != null ? "anArrayMethod()=" + anArrayMethod() + ", " : "") + "aBool=" + aBool + ", " + (aString != null ? "aString=" + aString + ", " : "") + "anInt=" + anInt + "]";\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom array toString without limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArray() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] anArrayMethod() {\r
					return new char[0];\r
				}\r
				java.util.List<Boolean> list;\r
			\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "intArray", "list", "object", "stringArray", "anArrayMethod" });
		fSettings2.customArrayToString= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] anArrayMethod() {\r
					return new char[0];\r
				}\r
				java.util.List<Boolean> list;\r
				@Override\r
				public String toString() {\r
					return "A [AArray=" + Arrays.toString(AArray) + ", aBool=" + aBool + ", anA=" + anA + ", floatArray=" + Arrays.toString(floatArray) + ", intArray=" + Arrays.toString(intArray) + ", list=" + list + ", object=" + object + ", stringArray=" + Arrays.toString(stringArray) + ", anArrayMethod()=" + Arrays.toString(anArrayMethod()) + "]";\r
				}\r
			\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - limit of elements but not in arrays
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.limitElements= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return "A [AArray=" + AArray + ", aBool=" + aBool + ", anA=" + anA + ", floatArray=" + floatArray + ", hashMap=" + (hashMap != null ? toString(hashMap.entrySet(), maxLen) : null) + ", intArray=" + intArray + ", integerCollection=" + (integerCollection != null ? toString(integerCollection, maxLen) : null) + ", list=" + (list != null ? toString(list, maxLen) : null) + ", object=" + object + ", stringArray=" + stringArray + ", wildCollection=" + (wildCollection != null ? toString(wildCollection, maxLen) : null) + ", charArrayMethod()=" + charArrayMethod() + ", floatArrayMethod()=" + floatArrayMethod() + "]";\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom array toString and limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return "A [AArray=" + (AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) : null) + ", aBool=" + aBool + ", anA=" + anA + ", floatArray=" + (floatArray != null ? Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) : null) + ", hashMap=" + (hashMap != null ? toString(hashMap.entrySet(), maxLen) : null) + ", intArray=" + (intArray != null ? Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) : null) + ", integerCollection=" + (integerCollection != null ? toString(integerCollection, maxLen) : null) + ", list=" + (list != null ? toString(list, maxLen) : null) + ", object=" + object + ", stringArray=" + (stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) : null) + ", wildCollection=" + (wildCollection != null ? toString(wildCollection, maxLen) : null) + ", charArrayMethod()="\r
							+ (charArrayMethod() != null ? Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) : null) + ", floatArrayMethod()=" + (floatArrayMethod() != null ? Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : null) + "]";\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom Array, limit elements, java 1.4 compatibility
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimit1_4() throws Exception {
		setCompilerLevels(false, false);
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				\r
			}\r
			""",
				true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return "A [AArray=" + (AArray != null ? arrayToString(AArray, AArray.length, maxLen) : null) + ", aBool=" + aBool + ", anA=" + anA + ", floatArray=" + (floatArray != null ? arrayToString(floatArray, floatArray.length, maxLen) : null) + ", hashMap=" + (hashMap != null ? toString(hashMap.entrySet(), maxLen) : null) + ", intArray=" + (intArray != null ? arrayToString(intArray, intArray.length, maxLen) : null) + ", integerCollection=" + (integerCollection != null ? toString(integerCollection, maxLen) : null) + ", list=" + (list != null ? toString(list, maxLen) : null) + ", object=" + object + ", stringArray=" + (stringArray != null ? arrayToString(stringArray, stringArray.length, maxLen) : null) + ", wildCollection=" + (wildCollection != null ? toString(wildCollection, maxLen) : null) + ", charArrayMethod()=" + (charArrayMethod() != null ? arrayToString(charArrayMethod(), charArrayMethod().length, maxLen) : null) + ", floatArrayMethod()="\r
							+ (floatArrayMethod() != null ? arrayToString(floatArrayMethod(), floatArrayMethod().length, maxLen) : null) + "]";\r
				}\r
				private String toString(Collection collection, int maxLen) {\r
					StringBuffer buffer = new StringBuffer();\r
					buffer.append("[");\r
					int i = 0;\r
					for (Iterator iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							buffer.append(", ");\r
						}\r
						buffer.append(iterator.next());\r
					}\r
					buffer.append("]");\r
					return buffer.toString();\r
				}\r
				private String arrayToString(Object array, int len, int maxLen) {\r
					StringBuffer buffer = new StringBuffer();\r
					len = Math.min(len, maxLen);\r
					buffer.append("[");\r
					for (int i = 0; i < len; i++) {\r
						if (i > 0) {\r
							buffer.append(", ");\r
						}\r
						if (array instanceof float[]) {\r
							buffer.append(((float[]) array)[i]);\r
						}\r
						if (array instanceof int[]) {\r
							buffer.append(((int[]) array)[i]);\r
						}\r
						if (array instanceof char[]) {\r
							buffer.append(((char[]) array)[i]);\r
						}\r
						if (array instanceof Object[]) {\r
							buffer.append(((Object[]) array)[i]);\r
						}\r
					}\r
					buffer.append("]");\r
					return buffer.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom Array, limit elements, java 1.4 compatibility, unique variable
	 * names needed
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimit1_4Unique() throws Exception {
		setCompilerLevels(false, false);
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				int[] intArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				Object builder;\r
				Object buffer;\r
				Object maxLen;\r
				Object len;\r
				Object collection;\r
				Object array;\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aBool", "intArray", "stringArray", "AArray", "list", "hashMap", "wildCollection", "integerCollection", "builder", "buffer",
				"maxLen", "len", "collection", "array" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				int[] intArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				Object builder;\r
				Object buffer;\r
				Object maxLen;\r
				Object len;\r
				Object collection;\r
				Object array;\r
				@Override\r
				public String toString() {\r
					final int maxLen2 = 10;\r
					return "A [aBool=" + aBool + ", intArray=" + (intArray != null ? arrayToString(intArray, intArray.length, maxLen2) : null) + ", stringArray=" + (stringArray != null ? arrayToString(stringArray, stringArray.length, maxLen2) : null) + ", AArray=" + (AArray != null ? arrayToString(AArray, AArray.length, maxLen2) : null) + ", list=" + (list != null ? toString(list, maxLen2) : null) + ", hashMap=" + (hashMap != null ? toString(hashMap.entrySet(), maxLen2) : null) + ", wildCollection=" + (wildCollection != null ? toString(wildCollection, maxLen2) : null) + ", integerCollection=" + (integerCollection != null ? toString(integerCollection, maxLen2) : null) + ", builder=" + builder + ", buffer=" + buffer + ", maxLen=" + maxLen + ", len=" + len + ", collection=" + collection + ", array=" + array + "]";\r
				}\r
				private String toString(Collection collection2, int maxLen2) {\r
					StringBuffer buffer2 = new StringBuffer();\r
					buffer2.append("[");\r
					int i = 0;\r
					for (Iterator iterator = collection2.iterator(); iterator.hasNext() && i < maxLen2; i++) {\r
						if (i > 0) {\r
							buffer2.append(", ");\r
						}\r
						buffer2.append(iterator.next());\r
					}\r
					buffer2.append("]");\r
					return buffer2.toString();\r
				}\r
				private String arrayToString(Object array2, int len2, int maxLen2) {\r
					StringBuffer buffer2 = new StringBuffer();\r
					len2 = Math.min(len2, maxLen2);\r
					buffer2.append("[");\r
					for (int i = 0; i < len2; i++) {\r
						if (i > 0) {\r
							buffer2.append(", ");\r
						}\r
						if (array2 instanceof int[]) {\r
							buffer2.append(((int[]) array2)[i]);\r
						}\r
						if (array2 instanceof Object[]) {\r
							buffer2.append(((Object[]) array2)[i]);\r
						}\r
					}\r
					buffer2.append("]");\r
					return buffer2.toString();\r
				}\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom Array, limit elements, java 1.5 compatibility
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimit1_5() throws Exception {
		setCompilerLevels(true, false);
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				\r
			}\r
			""",
				true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return "A [AArray=" + (AArray != null ? arrayToString(AArray, AArray.length, maxLen) : null) + ", aBool=" + aBool + ", anA=" + anA + ", floatArray=" + (floatArray != null ? arrayToString(floatArray, floatArray.length, maxLen) : null) + ", hashMap=" + (hashMap != null ? toString(hashMap.entrySet(), maxLen) : null) + ", intArray=" + (intArray != null ? arrayToString(intArray, intArray.length, maxLen) : null) + ", integerCollection=" + (integerCollection != null ? toString(integerCollection, maxLen) : null) + ", list=" + (list != null ? toString(list, maxLen) : null) + ", object=" + object + ", stringArray=" + (stringArray != null ? arrayToString(stringArray, stringArray.length, maxLen) : null) + ", wildCollection=" + (wildCollection != null ? toString(wildCollection, maxLen) : null) + ", charArrayMethod()=" + (charArrayMethod() != null ? arrayToString(charArrayMethod(), charArrayMethod().length, maxLen) : null) + ", floatArrayMethod()="\r
							+ (floatArrayMethod() != null ? arrayToString(floatArrayMethod(), floatArrayMethod().length, maxLen) : null) + "]";\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				private String arrayToString(Object array, int len, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					len = Math.min(len, maxLen);\r
					builder.append("[");\r
					for (int i = 0; i < len; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						if (array instanceof float[]) {\r
							builder.append(((float[]) array)[i]);\r
						}\r
						if (array instanceof int[]) {\r
							builder.append(((int[]) array)[i]);\r
						}\r
						if (array instanceof char[]) {\r
							builder.append(((char[]) array)[i]);\r
						}\r
						if (array instanceof Object[]) {\r
							builder.append(((Object[]) array)[i]);\r
						}\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom Array, limit elements, java 1.5 compatibility, unique names
	 * needed
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimit1_5Unique() throws Exception {
		setCompilerLevels(true, false);
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				int[] intArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				Object builder;\r
				Object buffer;\r
				Object maxLen;\r
				Object len;\r
				Object collection;\r
				Object array;\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aBool", "intArray", "stringArray", "AArray", "list", "hashMap", "wildCollection", "integerCollection", "builder", "buffer",
				"maxLen", "len", "collection", "array" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				int[] intArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				Object builder;\r
				Object buffer;\r
				Object maxLen;\r
				Object len;\r
				Object collection;\r
				Object array;\r
				@Override\r
				public String toString() {\r
					final int maxLen2 = 10;\r
					return "A [aBool=" + aBool + ", intArray=" + (intArray != null ? arrayToString(intArray, intArray.length, maxLen2) : null) + ", stringArray=" + (stringArray != null ? arrayToString(stringArray, stringArray.length, maxLen2) : null) + ", AArray=" + (AArray != null ? arrayToString(AArray, AArray.length, maxLen2) : null) + ", list=" + (list != null ? toString(list, maxLen2) : null) + ", hashMap=" + (hashMap != null ? toString(hashMap.entrySet(), maxLen2) : null) + ", wildCollection=" + (wildCollection != null ? toString(wildCollection, maxLen2) : null) + ", integerCollection=" + (integerCollection != null ? toString(integerCollection, maxLen2) : null) + ", builder=" + builder + ", buffer=" + buffer + ", maxLen=" + maxLen + ", len=" + len + ", collection=" + collection + ", array=" + array + "]";\r
				}\r
				private String toString(Collection<?> collection2, int maxLen2) {\r
					StringBuilder builder2 = new StringBuilder();\r
					builder2.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection2.iterator(); iterator.hasNext() && i < maxLen2; i++) {\r
						if (i > 0) {\r
							builder2.append(", ");\r
						}\r
						builder2.append(iterator.next());\r
					}\r
					builder2.append("]");\r
					return builder2.toString();\r
				}\r
				private String arrayToString(Object array2, int len2, int maxLen2) {\r
					StringBuilder builder2 = new StringBuilder();\r
					len2 = Math.min(len2, maxLen2);\r
					builder2.append("[");\r
					for (int i = 0; i < len2; i++) {\r
						if (i > 0) {\r
							builder2.append(", ");\r
						}\r
						if (array2 instanceof int[]) {\r
							builder2.append(((int[]) array2)[i]);\r
						}\r
						if (array2 instanceof Object[]) {\r
							builder2.append(((Object[]) array2)[i]);\r
						}\r
					}\r
					builder2.append("]");\r
					return builder2.toString();\r
				}\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom Array, limit elements to 0
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimitZero() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				\r
			}\r
			""",
				true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.limitValue= 0;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				@Override\r
				public String toString() {\r
					return "A [AArray=" + (AArray != null ? "[]" : null) + ", aBool=" + aBool + ", anA=" + anA + ", floatArray=" + (floatArray != null ? "[]" : null) + ", hashMap=" + (hashMap != null ? "[]" : null) + ", intArray=" + (intArray != null ? "[]" : null) + ", integerCollection=" + (integerCollection != null ? "[]" : null) + ", list=" + (list != null ? "[]" : null) + ", object=" + object + ", stringArray=" + (stringArray != null ? "[]" : null) + ", wildCollection=" + (wildCollection != null ? "[]" : null) + ", charArrayMethod()=" + (charArrayMethod() != null ? "[]" : null) + ", floatArrayMethod()=" + (floatArrayMethod() != null ? "[]" : null) + "]";\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom Array, limit elements to 0, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimitZeroNulls() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				\r
			}\r
			""",
				true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.limitValue= 0;
		fSettings2.skipNulls= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				@Override\r
				public String toString() {\r
					return "A [" + (AArray != null ? "AArray=[], " : "") + "aBool=" + aBool + ", " + (anA != null ? "anA=" + anA + ", " : "") + (floatArray != null ? "floatArray=[], " : "") + (hashMap != null ? "hashMap=[], " : "") + (intArray != null ? "intArray=[], " : "") + (integerCollection != null ? "integerCollection=[], " : "") + (list != null ? "list=[], " : "") + (object != null ? "object=" + object + ", " : "") + (stringArray != null ? "stringArray=[], " : "") + (wildCollection != null ? "wildCollection=[], " : "") + (charArrayMethod() != null ? "charArrayMethod()=[], " : "") + (floatArrayMethod() != null ? "floatArrayMethod()=[]" : "") + "]";\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - 'use keyword this' and no one-line blocks
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimitThisNoBlock() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.useBlocks= false;
		fSettings2.useKeywordThis= true;

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return "A [AArray=" + (this.AArray != null ? Arrays.asList(this.AArray).subList(0, Math.min(this.AArray.length, maxLen)) : null) + ", aBool=" + this.aBool + ", anA=" + this.anA + ", floatArray=" + (this.floatArray != null ? Arrays.toString(Arrays.copyOf(this.floatArray, Math.min(this.floatArray.length, maxLen))) : null) + ", hashMap=" + (this.hashMap != null ? this.toString(this.hashMap.entrySet(), maxLen) : null) + ", intArray=" + (this.intArray != null ? Arrays.toString(Arrays.copyOf(this.intArray, Math.min(this.intArray.length, maxLen))) : null) + ", integerCollection=" + (this.integerCollection != null ? this.toString(this.integerCollection, maxLen) : null) + ", list=" + (this.list != null ? this.toString(this.list, maxLen) : null) + ", object=" + this.object + ", stringArray=" + (this.stringArray != null ? Arrays.asList(this.stringArray).subList(0, Math.min(this.stringArray.length, maxLen)) : null) + ", wildCollection="\r
							+ (this.wildCollection != null ? this.toString(this.wildCollection, maxLen) : null) + ", charArrayMethod()=" + (this.charArrayMethod() != null ? Arrays.toString(Arrays.copyOf(this.charArrayMethod(), Math.min(this.charArrayMethod().length, maxLen))) : null) + ", floatArrayMethod()=" + (this.floatArrayMethod() != null ? Arrays.toString(Arrays.copyOf(this.floatArrayMethod(), Math.min(this.floatArrayMethod().length, maxLen))) : null) + "]";\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0)\r
							builder.append(", ");\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom array, limit elements, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimitNulls() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.skipNulls= true;

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return "A [" + (AArray != null ? "AArray=" + Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) + ", " : "") + "aBool=" + aBool + ", anInt=" + anInt + ", " + (anA != null ? "anA=" + anA + ", " : "") + (floatArray != null ? "floatArray=" + Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) + ", " : "") + (hashMap != null ? "hashMap=" + toString(hashMap.entrySet(), maxLen) + ", " : "") + (intArray != null ? "intArray=" + Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) + ", " : "") + (integerCollection != null ? "integerCollection=" + toString(integerCollection, maxLen) + ", " : "") + (list != null ? "list=" + toString(list, maxLen) + ", " : "") + (object != null ? "object=" + object + ", " : "") + (stringArray != null ? "stringArray=" + Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) + ", " : "")\r
							+ (wildCollection != null ? "wildCollection=" + toString(wildCollection, maxLen) + ", " : "") + (charArrayMethod() != null ? "charArrayMethod()=" + Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) + ", " : "") + (floatArrayMethod() != null ? "floatArrayMethod()=" + Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : "") + "]";\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - custom array, limit elements, no members require helper method
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatArrayLimitNoHelpers() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "intArray", "list", "object", "stringArray", "charArrayMethod",
				"floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.skipNulls= true;

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return "A [" + (AArray != null ? "AArray=" + Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) + ", " : "") + "aBool=" + aBool + ", anInt=" + anInt + ", " + (anA != null ? "anA=" + anA + ", " : "") + (floatArray != null ? "floatArray=" + Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) + ", " : "") + (intArray != null ? "intArray=" + Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) + ", " : "") + (list != null ? "list=" + list.subList(0, Math.min(list.size(), maxLen)) + ", " : "") + (object != null ? "object=" + object + ", " : "") + (stringArray != null ? "stringArray=" + Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) + ", " : "") + (charArrayMethod() != null ? "charArrayMethod()=" + Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) + ", " : "")\r
							+ (floatArrayMethod() != null ? "floatArrayMethod()=" + Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : "") + "]";\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - different template
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatTemplate() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.stringFormatTemplate= "ABCD${object.className}(${object.getClassName})\nEFG\n{\n\t${member.name} == ${member.value}\n\t${otherMembers}\n}(${object.className}|${object.hashCode}|${object.superToString}|${object.identityHashCode})\nGoodbye!";
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					return "ABCDA(" + getClass().getName() + ")\\nEFG\\n{\\n\\taStringMethod == " + aStringMethod() + "\\n\\taFloatMethod == " + aFloatMethod() + "\\n\\tanArrayMethod == " + anArrayMethod() + "\\n\\taBool == " + aBool + "\\n\\taString == " + aString + "\\n\\tanInt == " + anInt + "\\n}(A|" + hashCode() + "|" + super.toString() + "|" + System.identityHashCode(this) + ")\\nGoodbye!";\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string concatenation - replacing existing toString() and arrayToString(array,int), leaving
	 * toString(Collection)
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void concatReplace() throws Exception {
		setCompilerLevels(true, false);

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				A anA;\r
				float[] floatArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				@Override\r
				public String toString() {\r
					return "A []";\r
				}\r
				private String arrayToString(Object array, int len, int maxLen) {\r
					return array[0].toString();\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					return collection.toString();\r
				}\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "anA", "floatArray", "hashMap", "list", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				A anA;\r
				float[] floatArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return "A [anA=" + anA + ", floatArray=" + (floatArray != null ? arrayToString(floatArray, floatArray.length, maxLen) : null) + ", hashMap=" + (hashMap != null ? toString(hashMap.entrySet(), maxLen) : null) + ", list=" + (list != null ? toString(list, maxLen) : null) + ", charArrayMethod()=" + (charArrayMethod() != null ? arrayToString(charArrayMethod(), charArrayMethod().length, maxLen) : null) + ", floatArrayMethod()=" + (floatArrayMethod() != null ? arrayToString(floatArrayMethod(), floatArrayMethod().length, maxLen) : null) + "]";\r
				}\r
				private String arrayToString(Object array, int len, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					len = Math.min(len, maxLen);\r
					builder.append("[");\r
					for (int i = 0; i < len; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						if (array instanceof float[]) {\r
							builder.append(((float[]) array)[i]);\r
						}\r
						if (array instanceof char[]) {\r
							builder.append(((char[]) array)[i]);\r
						}\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					return collection.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 1;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A [");\r
					if (aStringMethod() != null) {\r
						builder.append("aStringMethod()=");\r
						builder.append(aStringMethod());\r
						builder.append(", ");\r
					}\r
					builder.append("aFloatMethod()=");\r
					builder.append(aFloatMethod());\r
					builder.append(", ");\r
					if (anArrayMethod() != null) {\r
						builder.append("anArrayMethod()=");\r
						builder.append(anArrayMethod());\r
						builder.append(", ");\r
					}\r
					builder.append("aBool=");\r
					builder.append(aBool);\r
					builder.append(", ");\r
					if (aString != null) {\r
						builder.append("aString=");\r
						builder.append(aString);\r
						builder.append(", ");\r
					}\r
					builder.append("anInt=");\r
					builder.append(anInt);\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array toString without limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArray() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] anArrayMethod() {\r
					return new char[0];\r
				}\r
				java.util.List<Boolean> list;\r
			\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "intArray", "list", "object", "stringArray", "anArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.toStringStyle= 1;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] anArrayMethod() {\r
					return new char[0];\r
				}\r
				java.util.List<Boolean> list;\r
				@Override\r
				public String toString() {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A [AArray=");\r
					builder.append(Arrays.toString(AArray));\r
					builder.append(", aBool=");\r
					builder.append(aBool);\r
					builder.append(", anA=");\r
					builder.append(anA);\r
					builder.append(", floatArray=");\r
					builder.append(Arrays.toString(floatArray));\r
					builder.append(", intArray=");\r
					builder.append(Arrays.toString(intArray));\r
					builder.append(", list=");\r
					builder.append(list);\r
					builder.append(", object=");\r
					builder.append(object);\r
					builder.append(", stringArray=");\r
					builder.append(Arrays.toString(stringArray));\r
					builder.append(", anArrayMethod()=");\r
					builder.append(Arrays.toString(anArrayMethod()));\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
			\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - limit of elements but not in arrays
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 1;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A [AArray=");\r
					builder.append(AArray);\r
					builder.append(", aBool=");\r
					builder.append(aBool);\r
					builder.append(", anA=");\r
					builder.append(anA);\r
					builder.append(", floatArray=");\r
					builder.append(floatArray);\r
					builder.append(", hashMap=");\r
					builder.append(hashMap != null ? toString(hashMap.entrySet(), maxLen) : null);\r
					builder.append(", intArray=");\r
					builder.append(intArray);\r
					builder.append(", integerCollection=");\r
					builder.append(integerCollection != null ? toString(integerCollection, maxLen) : null);\r
					builder.append(", list=");\r
					builder.append(list != null ? toString(list, maxLen) : null);\r
					builder.append(", object=");\r
					builder.append(object);\r
					builder.append(", stringArray=");\r
					builder.append(stringArray);\r
					builder.append(", wildCollection=");\r
					builder.append(wildCollection != null ? toString(wildCollection, maxLen) : null);\r
					builder.append(", charArrayMethod()=");\r
					builder.append(charArrayMethod());\r
					builder.append(", floatArrayMethod()=");\r
					builder.append(floatArrayMethod());\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array toString and limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 1;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A [AArray=");\r
					builder.append(AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) : null);\r
					builder.append(", aBool=");\r
					builder.append(aBool);\r
					builder.append(", anA=");\r
					builder.append(anA);\r
					builder.append(", floatArray=");\r
					builder.append(floatArray != null ? Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) : null);\r
					builder.append(", hashMap=");\r
					builder.append(hashMap != null ? toString(hashMap.entrySet(), maxLen) : null);\r
					builder.append(", intArray=");\r
					builder.append(intArray != null ? Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) : null);\r
					builder.append(", integerCollection=");\r
					builder.append(integerCollection != null ? toString(integerCollection, maxLen) : null);\r
					builder.append(", list=");\r
					builder.append(list != null ? toString(list, maxLen) : null);\r
					builder.append(", object=");\r
					builder.append(object);\r
					builder.append(", stringArray=");\r
					builder.append(stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) : null);\r
					builder.append(", wildCollection=");\r
					builder.append(wildCollection != null ? toString(wildCollection, maxLen) : null);\r
					builder.append(", charArrayMethod()=");\r
					builder.append(charArrayMethod() != null ? Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) : null);\r
					builder.append(", floatArrayMethod()=");\r
					builder.append(floatArrayMethod() != null ? Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : null);\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array, limit of elements, without java 5.0 compatibility
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimit1_4() throws Exception {
		setCompilerLevels(false, false);
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				\r
			}\r
			""",
				true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 1;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					StringBuffer buffer = new StringBuffer();\r
					buffer.append("A [AArray=");\r
					buffer.append(AArray != null ? arrayToString(AArray, AArray.length, maxLen) : null);\r
					buffer.append(", aBool=");\r
					buffer.append(aBool);\r
					buffer.append(", anA=");\r
					buffer.append(anA);\r
					buffer.append(", floatArray=");\r
					buffer.append(floatArray != null ? arrayToString(floatArray, floatArray.length, maxLen) : null);\r
					buffer.append(", hashMap=");\r
					buffer.append(hashMap != null ? toString(hashMap.entrySet(), maxLen) : null);\r
					buffer.append(", intArray=");\r
					buffer.append(intArray != null ? arrayToString(intArray, intArray.length, maxLen) : null);\r
					buffer.append(", integerCollection=");\r
					buffer.append(integerCollection != null ? toString(integerCollection, maxLen) : null);\r
					buffer.append(", list=");\r
					buffer.append(list != null ? toString(list, maxLen) : null);\r
					buffer.append(", object=");\r
					buffer.append(object);\r
					buffer.append(", stringArray=");\r
					buffer.append(stringArray != null ? arrayToString(stringArray, stringArray.length, maxLen) : null);\r
					buffer.append(", wildCollection=");\r
					buffer.append(wildCollection != null ? toString(wildCollection, maxLen) : null);\r
					buffer.append(", charArrayMethod()=");\r
					buffer.append(charArrayMethod() != null ? arrayToString(charArrayMethod(), charArrayMethod().length, maxLen) : null);\r
					buffer.append(", floatArrayMethod()=");\r
					buffer.append(floatArrayMethod() != null ? arrayToString(floatArrayMethod(), floatArrayMethod().length, maxLen) : null);\r
					buffer.append("]");\r
					return buffer.toString();\r
				}\r
				private String toString(Collection collection, int maxLen) {\r
					StringBuffer buffer = new StringBuffer();\r
					buffer.append("[");\r
					int i = 0;\r
					for (Iterator iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							buffer.append(", ");\r
						}\r
						buffer.append(iterator.next());\r
					}\r
					buffer.append("]");\r
					return buffer.toString();\r
				}\r
				private String arrayToString(Object array, int len, int maxLen) {\r
					StringBuffer buffer = new StringBuffer();\r
					len = Math.min(len, maxLen);\r
					buffer.append("[");\r
					for (int i = 0; i < len; i++) {\r
						if (i > 0) {\r
							buffer.append(", ");\r
						}\r
						if (array instanceof float[]) {\r
							buffer.append(((float[]) array)[i]);\r
						}\r
						if (array instanceof int[]) {\r
							buffer.append(((int[]) array)[i]);\r
						}\r
						if (array instanceof char[]) {\r
							buffer.append(((char[]) array)[i]);\r
						}\r
						if (array instanceof Object[]) {\r
							buffer.append(((Object[]) array)[i]);\r
						}\r
					}\r
					buffer.append("]");\r
					return buffer.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array, limit of elements, without java 5.0 compatibility, unique
	 * names needed
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimit1_4Unique() throws Exception {
		setCompilerLevels(false, false);
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				int[] intArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				Object builder;\r
				Object buffer;\r
				Object maxLen;\r
				Object len;\r
				Object collection;\r
				Object array;\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aBool", "intArray", "stringArray", "AArray", "list", "hashMap", "wildCollection", "integerCollection", "builder", "buffer",
				"maxLen", "len", "collection", "array" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 1;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				int[] intArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				Object builder;\r
				Object buffer;\r
				Object maxLen;\r
				Object len;\r
				Object collection;\r
				Object array;\r
				@Override\r
				public String toString() {\r
					final int maxLen2 = 10;\r
					StringBuffer buffer2 = new StringBuffer();\r
					buffer2.append("A [aBool=");\r
					buffer2.append(aBool);\r
					buffer2.append(", intArray=");\r
					buffer2.append(intArray != null ? arrayToString(intArray, intArray.length, maxLen2) : null);\r
					buffer2.append(", stringArray=");\r
					buffer2.append(stringArray != null ? arrayToString(stringArray, stringArray.length, maxLen2) : null);\r
					buffer2.append(", AArray=");\r
					buffer2.append(AArray != null ? arrayToString(AArray, AArray.length, maxLen2) : null);\r
					buffer2.append(", list=");\r
					buffer2.append(list != null ? toString(list, maxLen2) : null);\r
					buffer2.append(", hashMap=");\r
					buffer2.append(hashMap != null ? toString(hashMap.entrySet(), maxLen2) : null);\r
					buffer2.append(", wildCollection=");\r
					buffer2.append(wildCollection != null ? toString(wildCollection, maxLen2) : null);\r
					buffer2.append(", integerCollection=");\r
					buffer2.append(integerCollection != null ? toString(integerCollection, maxLen2) : null);\r
					buffer2.append(", builder=");\r
					buffer2.append(builder);\r
					buffer2.append(", buffer=");\r
					buffer2.append(buffer);\r
					buffer2.append(", maxLen=");\r
					buffer2.append(maxLen);\r
					buffer2.append(", len=");\r
					buffer2.append(len);\r
					buffer2.append(", collection=");\r
					buffer2.append(collection);\r
					buffer2.append(", array=");\r
					buffer2.append(array);\r
					buffer2.append("]");\r
					return buffer2.toString();\r
				}\r
				private String toString(Collection collection2, int maxLen2) {\r
					StringBuffer buffer2 = new StringBuffer();\r
					buffer2.append("[");\r
					int i = 0;\r
					for (Iterator iterator = collection2.iterator(); iterator.hasNext() && i < maxLen2; i++) {\r
						if (i > 0) {\r
							buffer2.append(", ");\r
						}\r
						buffer2.append(iterator.next());\r
					}\r
					buffer2.append("]");\r
					return buffer2.toString();\r
				}\r
				private String arrayToString(Object array2, int len2, int maxLen2) {\r
					StringBuffer buffer2 = new StringBuffer();\r
					len2 = Math.min(len2, maxLen2);\r
					buffer2.append("[");\r
					for (int i = 0; i < len2; i++) {\r
						if (i > 0) {\r
							buffer2.append(", ");\r
						}\r
						if (array2 instanceof int[]) {\r
							buffer2.append(((int[]) array2)[i]);\r
						}\r
						if (array2 instanceof Object[]) {\r
							buffer2.append(((Object[]) array2)[i]);\r
						}\r
					}\r
					buffer2.append("]");\r
					return buffer2.toString();\r
				}\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array, limit of elements, skip nulls, use keyword this, no one-line
	 * blocks
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimitNullsThisNoBlocks() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.skipNulls= true;
		fSettings2.limitElements= true;
		fSettings2.useBlocks= false;
		fSettings2.useKeywordThis= true;
		fSettings2.toStringStyle= 1;
		fSettings2.stringFormatTemplate= "${object.className}[ ${member.value}, ${otherMembers}]";

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A[ ");\r
					if (this.AArray != null) {\r
						builder.append(Arrays.asList(this.AArray).subList(0, Math.min(this.AArray.length, maxLen)));\r
						builder.append(", ");\r
					}\r
					builder.append(this.aBool);\r
					builder.append(", ");\r
					if (this.anA != null) {\r
						builder.append(this.anA);\r
						builder.append(", ");\r
					}\r
					if (this.floatArray != null) {\r
						builder.append(Arrays.toString(Arrays.copyOf(this.floatArray, Math.min(this.floatArray.length, maxLen))));\r
						builder.append(", ");\r
					}\r
					if (this.hashMap != null) {\r
						builder.append(this.toString(this.hashMap.entrySet(), maxLen));\r
						builder.append(", ");\r
					}\r
					if (this.intArray != null) {\r
						builder.append(Arrays.toString(Arrays.copyOf(this.intArray, Math.min(this.intArray.length, maxLen))));\r
						builder.append(", ");\r
					}\r
					if (this.integerCollection != null) {\r
						builder.append(this.toString(this.integerCollection, maxLen));\r
						builder.append(", ");\r
					}\r
					if (this.list != null) {\r
						builder.append(this.toString(this.list, maxLen));\r
						builder.append(", ");\r
					}\r
					if (this.object != null) {\r
						builder.append(this.object);\r
						builder.append(", ");\r
					}\r
					if (this.stringArray != null) {\r
						builder.append(Arrays.asList(this.stringArray).subList(0, Math.min(this.stringArray.length, maxLen)));\r
						builder.append(", ");\r
					}\r
					if (this.wildCollection != null) {\r
						builder.append(this.toString(this.wildCollection, maxLen));\r
						builder.append(", ");\r
					}\r
					if (this.charArrayMethod() != null) {\r
						builder.append(Arrays.toString(Arrays.copyOf(this.charArrayMethod(), Math.min(this.charArrayMethod().length, maxLen))));\r
						builder.append(", ");\r
					}\r
					if (this.floatArrayMethod() != null)\r
						builder.append(Arrays.toString(Arrays.copyOf(this.floatArrayMethod(), Math.min(this.floatArrayMethod().length, maxLen))));\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0)\r
							builder.append(", ");\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array, limit elements, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimitNulls() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 1;

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A [");\r
					if (AArray != null) {\r
						builder.append("AArray=");\r
						builder.append(Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)));\r
						builder.append(", ");\r
					}\r
					builder.append("aBool=");\r
					builder.append(aBool);\r
					builder.append(", anInt=");\r
					builder.append(anInt);\r
					builder.append(", ");\r
					if (anA != null) {\r
						builder.append("anA=");\r
						builder.append(anA);\r
						builder.append(", ");\r
					}\r
					if (floatArray != null) {\r
						builder.append("floatArray=");\r
						builder.append(Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))));\r
						builder.append(", ");\r
					}\r
					if (hashMap != null) {\r
						builder.append("hashMap=");\r
						builder.append(toString(hashMap.entrySet(), maxLen));\r
						builder.append(", ");\r
					}\r
					if (intArray != null) {\r
						builder.append("intArray=");\r
						builder.append(Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))));\r
						builder.append(", ");\r
					}\r
					if (integerCollection != null) {\r
						builder.append("integerCollection=");\r
						builder.append(toString(integerCollection, maxLen));\r
						builder.append(", ");\r
					}\r
					if (list != null) {\r
						builder.append("list=");\r
						builder.append(toString(list, maxLen));\r
						builder.append(", ");\r
					}\r
					if (object != null) {\r
						builder.append("object=");\r
						builder.append(object);\r
						builder.append(", ");\r
					}\r
					if (stringArray != null) {\r
						builder.append("stringArray=");\r
						builder.append(Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)));\r
						builder.append(", ");\r
					}\r
					if (wildCollection != null) {\r
						builder.append("wildCollection=");\r
						builder.append(toString(wildCollection, maxLen));\r
						builder.append(", ");\r
					}\r
					if (charArrayMethod() != null) {\r
						builder.append("charArrayMethod()=");\r
						builder.append(Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))));\r
						builder.append(", ");\r
					}\r
					if (floatArrayMethod() != null) {\r
						builder.append("floatArrayMethod()=");\r
						builder.append(Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))));\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array, limit elements, no members require helper methods
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimitNoHelpers() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "intArray", "list", "object", "stringArray", "charArrayMethod",
				"floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 1;

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A [AArray=");\r
					builder.append(AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) : null);\r
					builder.append(", aBool=");\r
					builder.append(aBool);\r
					builder.append(", anInt=");\r
					builder.append(anInt);\r
					builder.append(", anA=");\r
					builder.append(anA);\r
					builder.append(", floatArray=");\r
					builder.append(floatArray != null ? Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) : null);\r
					builder.append(", intArray=");\r
					builder.append(intArray != null ? Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) : null);\r
					builder.append(", list=");\r
					builder.append(list != null ? list.subList(0, Math.min(list.size(), maxLen)) : null);\r
					builder.append(", object=");\r
					builder.append(object);\r
					builder.append(", stringArray=");\r
					builder.append(stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) : null);\r
					builder.append(", charArrayMethod()=");\r
					builder.append(charArrayMethod() != null ? Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) : null);\r
					builder.append(", floatArrayMethod()=");\r
					builder.append(floatArrayMethod() != null ? Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : null);\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array, limit elements to 0
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimitZero() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.limitValue= 0;
		fSettings2.toStringStyle= 1;

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A [AArray=");\r
					builder.append(AArray != null ? "[]" : null);\r
					builder.append(", aBool=");\r
					builder.append(aBool);\r
					builder.append(", anInt=");\r
					builder.append(anInt);\r
					builder.append(", anA=");\r
					builder.append(anA);\r
					builder.append(", floatArray=");\r
					builder.append(floatArray != null ? "[]" : null);\r
					builder.append(", hashMap=");\r
					builder.append(hashMap != null ? "[]" : null);\r
					builder.append(", intArray=");\r
					builder.append(intArray != null ? "[]" : null);\r
					builder.append(", integerCollection=");\r
					builder.append(integerCollection != null ? "[]" : null);\r
					builder.append(", list=");\r
					builder.append(list != null ? "[]" : null);\r
					builder.append(", object=");\r
					builder.append(object);\r
					builder.append(", stringArray=");\r
					builder.append(stringArray != null ? "[]" : null);\r
					builder.append(", wildCollection=");\r
					builder.append(wildCollection != null ? "[]" : null);\r
					builder.append(", charArrayMethod()=");\r
					builder.append(charArrayMethod() != null ? "[]" : null);\r
					builder.append(", floatArrayMethod()=");\r
					builder.append(floatArrayMethod() != null ? "[]" : null);\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array, limit elements to 0, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimitZeroNulls() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.limitValue= 0;
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 1;

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A [");\r
					if (AArray != null) {\r
						builder.append("AArray=[], ");\r
					}\r
					builder.append("aBool=");\r
					builder.append(aBool);\r
					builder.append(", anInt=");\r
					builder.append(anInt);\r
					builder.append(", ");\r
					if (anA != null) {\r
						builder.append("anA=");\r
						builder.append(anA);\r
						builder.append(", ");\r
					}\r
					if (floatArray != null) {\r
						builder.append("floatArray=[], ");\r
					}\r
					if (hashMap != null) {\r
						builder.append("hashMap=[], ");\r
					}\r
					if (intArray != null) {\r
						builder.append("intArray=[], ");\r
					}\r
					if (integerCollection != null) {\r
						builder.append("integerCollection=[], ");\r
					}\r
					if (list != null) {\r
						builder.append("list=[], ");\r
					}\r
					if (object != null) {\r
						builder.append("object=");\r
						builder.append(object);\r
						builder.append(", ");\r
					}\r
					if (stringArray != null) {\r
						builder.append("stringArray=[], ");\r
					}\r
					if (wildCollection != null) {\r
						builder.append("wildCollection=[], ");\r
					}\r
					if (charArrayMethod() != null) {\r
						builder.append("charArrayMethod()=[], ");\r
					}\r
					if (floatArrayMethod() != null) {\r
						builder.append("floatArrayMethod()=[]");\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder, chained calls - skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedBuilderNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 2;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A [");\r
					if (aStringMethod() != null) {\r
						builder.append("aStringMethod()=").append(aStringMethod()).append(", ");\r
					}\r
					builder.append("aFloatMethod()=").append(aFloatMethod()).append(", ");\r
					if (anArrayMethod() != null) {\r
						builder.append("anArrayMethod()=").append(anArrayMethod()).append(", ");\r
					}\r
					builder.append("aBool=").append(aBool).append(", ");\r
					if (aString != null) {\r
						builder.append("aString=").append(aString).append(", ");\r
					}\r
					builder.append("anInt=").append(anInt).append("]");\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder, chained calls - custom array toString without limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedBuilderArray() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] anArrayMethod() {\r
					return new char[0];\r
				}\r
				java.util.List<Boolean> list;\r
			\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "intArray", "list", "object", "stringArray", "anArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.toStringStyle= 2;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] anArrayMethod() {\r
					return new char[0];\r
				}\r
				java.util.List<Boolean> list;\r
				@Override\r
				public String toString() {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A [AArray=").append(Arrays.toString(AArray)).append(", aBool=").append(aBool).append(", anA=").append(anA).append(", floatArray=").append(Arrays.toString(floatArray)).append(", intArray=").append(Arrays.toString(intArray)).append(", list=").append(list).append(", object=").append(object).append(", stringArray=").append(Arrays.toString(stringArray)).append(", anArrayMethod()=").append(Arrays.toString(anArrayMethod())).append("]");\r
					return builder.toString();\r
				}\r
			\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder, chained calls - custom array toString without limit of elements, unique names
	 * needed
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedBuilderArrayUnique() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				int[] intArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				Object builder;\r
				Object buffer;\r
				Object maxLen;\r
				Object len;\r
				Object collection;\r
				Object array;\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aBool", "intArray", "stringArray", "AArray", "list", "hashMap", "wildCollection", "integerCollection", "builder", "buffer",
				"maxLen", "len", "collection", "array" });
		fSettings2.customArrayToString= true;
		fSettings2.toStringStyle= 2;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				int[] intArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				Object builder;\r
				Object buffer;\r
				Object maxLen;\r
				Object len;\r
				Object collection;\r
				Object array;\r
				@Override\r
				public String toString() {\r
					StringBuilder builder2 = new StringBuilder();\r
					builder2.append("A [aBool=").append(aBool).append(", intArray=").append(Arrays.toString(intArray)).append(", stringArray=").append(Arrays.toString(stringArray)).append(", AArray=").append(Arrays.toString(AArray)).append(", list=").append(list).append(", hashMap=").append(hashMap).append(", wildCollection=").append(wildCollection).append(", integerCollection=").append(integerCollection).append(", builder=").append(builder).append(", buffer=").append(buffer).append(", maxLen=").append(maxLen).append(", len=").append(len).append(", collection=").append(collection).append(", array=").append(array).append("]");\r
					return builder2.toString();\r
				}\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder, chained calls - custom array, limit of elements, without java 5.0
	 * compatibility
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedBuilderArrayLimit1_4() throws Exception {
		setCompilerLevels(false, false);
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				\r
			}\r
			""",
				true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 2;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					StringBuffer buffer = new StringBuffer();\r
					buffer.append("A [AArray=").append(AArray != null ? arrayToString(AArray, AArray.length, maxLen) : null).append(", aBool=").append(aBool).append(", anA=").append(anA).append(", floatArray=").append(floatArray != null ? arrayToString(floatArray, floatArray.length, maxLen) : null).append(", hashMap=").append(hashMap != null ? toString(hashMap.entrySet(), maxLen) : null).append(", intArray=").append(intArray != null ? arrayToString(intArray, intArray.length, maxLen) : null).append(", integerCollection=").append(integerCollection != null ? toString(integerCollection, maxLen) : null).append(", list=").append(list != null ? toString(list, maxLen) : null).append(", object=").append(object).append(", stringArray=").append(stringArray != null ? arrayToString(stringArray, stringArray.length, maxLen) : null).append(", wildCollection=").append(wildCollection != null ? toString(wildCollection, maxLen) : null).append(", charArrayMethod()=")\r
							.append(charArrayMethod() != null ? arrayToString(charArrayMethod(), charArrayMethod().length, maxLen) : null).append(", floatArrayMethod()=").append(floatArrayMethod() != null ? arrayToString(floatArrayMethod(), floatArrayMethod().length, maxLen) : null).append("]");\r
					return buffer.toString();\r
				}\r
				private String toString(Collection collection, int maxLen) {\r
					StringBuffer buffer = new StringBuffer();\r
					buffer.append("[");\r
					int i = 0;\r
					for (Iterator iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							buffer.append(", ");\r
						}\r
						buffer.append(iterator.next());\r
					}\r
					buffer.append("]");\r
					return buffer.toString();\r
				}\r
				private String arrayToString(Object array, int len, int maxLen) {\r
					StringBuffer buffer = new StringBuffer();\r
					len = Math.min(len, maxLen);\r
					buffer.append("[");\r
					for (int i = 0; i < len; i++) {\r
						if (i > 0) {\r
							buffer.append(", ");\r
						}\r
						if (array instanceof float[]) {\r
							buffer.append(((float[]) array)[i]);\r
						}\r
						if (array instanceof int[]) {\r
							buffer.append(((int[]) array)[i]);\r
						}\r
						if (array instanceof char[]) {\r
							buffer.append(((char[]) array)[i]);\r
						}\r
						if (array instanceof Object[]) {\r
							buffer.append(((Object[]) array)[i]);\r
						}\r
					}\r
					buffer.append("]");\r
					return buffer.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}


	/**
	 * string builder, chained calls - custom array, JDK 1.5 compatybility
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedBuilderArray1_5() throws Exception {
		setCompilerLevels(true, false);
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.toStringStyle= 2;
		fSettings2.stringFormatTemplate= "${object.className}[ ${member.value}, ${otherMembers}]";

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A[ ").append(Arrays.toString(AArray)).append(", ").append(aBool).append(", ").append(anA).append(", ").append(Arrays.toString(floatArray)).append(", ").append(hashMap).append(", ").append(Arrays.toString(intArray)).append(", ").append(integerCollection).append(", ").append(list).append(", ").append(object).append(", ").append(Arrays.toString(stringArray)).append(", ").append(wildCollection).append(", ").append(Arrays.toString(charArrayMethod())).append(", ").append(Arrays.toString(floatArrayMethod())).append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder, chained calls - custom array, limit of elements, skip nulls, use keyword
	 * this, no one-line blocks
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedBuilderArrayLimitNullsThisNoBlocks() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.skipNulls= true;
		fSettings2.limitElements= true;
		fSettings2.useBlocks= false;
		fSettings2.useKeywordThis= true;
		fSettings2.toStringStyle= 2;
		fSettings2.stringFormatTemplate= "${object.className}[ ${member.value}, ${otherMembers}]";

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("A[ ");\r
					if (this.AArray != null)\r
						builder.append(Arrays.asList(this.AArray).subList(0, Math.min(this.AArray.length, maxLen))).append(", ");\r
					builder.append(this.aBool).append(", ");\r
					if (this.anA != null)\r
						builder.append(this.anA).append(", ");\r
					if (this.floatArray != null)\r
						builder.append(Arrays.toString(Arrays.copyOf(this.floatArray, Math.min(this.floatArray.length, maxLen)))).append(", ");\r
					if (this.hashMap != null)\r
						builder.append(this.toString(this.hashMap.entrySet(), maxLen)).append(", ");\r
					if (this.intArray != null)\r
						builder.append(Arrays.toString(Arrays.copyOf(this.intArray, Math.min(this.intArray.length, maxLen)))).append(", ");\r
					if (this.integerCollection != null)\r
						builder.append(this.toString(this.integerCollection, maxLen)).append(", ");\r
					if (this.list != null)\r
						builder.append(this.toString(this.list, maxLen)).append(", ");\r
					if (this.object != null)\r
						builder.append(this.object).append(", ");\r
					if (this.stringArray != null)\r
						builder.append(Arrays.asList(this.stringArray).subList(0, Math.min(this.stringArray.length, maxLen))).append(", ");\r
					if (this.wildCollection != null)\r
						builder.append(this.toString(this.wildCollection, maxLen)).append(", ");\r
					if (this.charArrayMethod() != null)\r
						builder.append(Arrays.toString(Arrays.copyOf(this.charArrayMethod(), Math.min(this.charArrayMethod().length, maxLen)))).append(", ");\r
					if (this.floatArrayMethod() != null)\r
						builder.append(Arrays.toString(Arrays.copyOf(this.floatArrayMethod(), Math.min(this.floatArrayMethod().length, maxLen))));\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0)\r
							builder.append(", ");\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - basic test
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void format() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.toStringStyle= 3;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					return String.format("A [aStringMethod()=%s, aFloatMethod()=%s, anArrayMethod()=%s, aBool=%s, aString=%s, anInt=%s]", aStringMethod(), aFloatMethod(), anArrayMethod(), aBool, aString, anInt);\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - custom array toString without limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void formatArray() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] anArrayMethod() {\r
					return new char[0];\r
				}\r
				java.util.List<Boolean> list;\r
			\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "intArray", "list", "object", "stringArray", "anArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.toStringStyle= 3;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] anArrayMethod() {\r
					return new char[0];\r
				}\r
				java.util.List<Boolean> list;\r
				@Override\r
				public String toString() {\r
					return String.format("A [AArray=%s, aBool=%s, anA=%s, floatArray=%s, intArray=%s, list=%s, object=%s, stringArray=%s, anArrayMethod()=%s]", Arrays.toString(AArray), aBool, anA, Arrays.toString(floatArray), Arrays.toString(intArray), list, object, Arrays.toString(stringArray), Arrays.toString(anArrayMethod()));\r
				}\r
			\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - limit of elements but not arrays
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void formatLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 3;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return String.format("A [AArray=%s, aBool=%s, anA=%s, floatArray=%s, hashMap=%s, intArray=%s, integerCollection=%s, list=%s, object=%s, stringArray=%s, wildCollection=%s, charArrayMethod()=%s, floatArrayMethod()=%s]", AArray, aBool, anA, floatArray, hashMap != null ? toString(hashMap.entrySet(), maxLen) : null, intArray, integerCollection != null ? toString(integerCollection, maxLen) : null, list != null ? toString(list, maxLen) : null, object, stringArray, wildCollection != null ? toString(wildCollection, maxLen) : null, charArrayMethod(), floatArrayMethod());\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - custom array toString and limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void formatArrayLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 3;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return String.format("A [AArray=%s, aBool=%s, anA=%s, floatArray=%s, hashMap=%s, intArray=%s, integerCollection=%s, list=%s, object=%s, stringArray=%s, wildCollection=%s, charArrayMethod()=%s, floatArrayMethod()=%s]", AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) : null, aBool, anA, floatArray != null ? Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) : null, hashMap != null ? toString(hashMap.entrySet(), maxLen) : null, intArray != null ? Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) : null, integerCollection != null ? toString(integerCollection, maxLen) : null, list != null ? toString(list, maxLen) : null, object, stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) : null, wildCollection != null ? toString(wildCollection, maxLen) : null,\r
							charArrayMethod() != null ? Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) : null, floatArrayMethod() != null ? Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : null);\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - custom array toString, limit of elements, JDK 1.5, no members require
	 * helper methods
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void formatArrayLimit1_5NoHelpers() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "list", "object", "stringArray" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 3;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return String.format("A [AArray=%s, aBool=%s, anA=%s, list=%s, object=%s, stringArray=%s]", AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) : null, aBool, anA, list != null ? list.subList(0, Math.min(list.size(), maxLen)) : null, object, stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) : null);\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - custom array toString and limit number of elements to 0
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void formatArrayLimitZero() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.limitValue= 0;
		fSettings2.toStringStyle= 3;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					return String.format("A [AArray=%s, aBool=%s, anA=%s, floatArray=%s, hashMap=%s, intArray=%s, integerCollection=%s, list=%s, object=%s, stringArray=%s, wildCollection=%s, charArrayMethod()=%s, floatArrayMethod()=%s]", AArray != null ? "[]" : null, aBool, anA, floatArray != null ? "[]" : null, hashMap != null ? "[]" : null, intArray != null ? "[]" : null, integerCollection != null ? "[]" : null, list != null ? "[]" : null, object, stringArray != null ? "[]" : null, wildCollection != null ? "[]" : null, charArrayMethod() != null ? "[]" : null, floatArrayMethod() != null ? "[]" : null);\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - custom array, limit of elements, 'use keyword this'
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void formatLimitThis() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.useKeywordThis= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 3;

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return String.format("A [AArray=%s, aBool=%s, anA=%s, floatArray=%s, hashMap=%s, intArray=%s, integerCollection=%s, list=%s, object=%s, stringArray=%s, wildCollection=%s, charArrayMethod()=%s, floatArrayMethod()=%s]", this.AArray, this.aBool, this.anA, this.floatArray, this.hashMap != null ? this.toString(this.hashMap.entrySet(), maxLen) : null, this.intArray, this.integerCollection != null ? this.toString(this.integerCollection, maxLen) : null, this.list != null ? this.toString(this.list, maxLen) : null, this.object, this.stringArray, this.wildCollection != null ? this.toString(this.wildCollection, maxLen) : null, this.charArrayMethod(), this.floatArrayMethod());\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * String.format() - custom array, limit of elements, jdk 1.4
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void formatLimit1_4() throws Exception {
		setCompilerLevels(false, false);
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 3;

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.text.MessageFormat;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					return MessageFormat.format("A [AArray={0}, aBool={1}, anA={2}, floatArray={3}, hashMap={4}, intArray={5}, integerCollection={6}, list={7}, object={8}, stringArray={9}, wildCollection={10}, charArrayMethod()={11}, floatArrayMethod()={12}]", new Object[]{AArray, new Boolean(aBool), anA, floatArray, hashMap != null ? toString(hashMap.entrySet(), maxLen) : null, intArray, integerCollection != null ? toString(integerCollection, maxLen) : null, list != null ? toString(list, maxLen) : null, object, stringArray, wildCollection != null ? toString(wildCollection, maxLen) : null, charArrayMethod(), floatArrayMethod()});\r
				}\r
				private String toString(Collection collection, int maxLen) {\r
					StringBuffer buffer = new StringBuffer();\r
					buffer.append("[");\r
					int i = 0;\r
					for (Iterator iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							buffer.append(", ");\r
						}\r
						buffer.append(iterator.next());\r
					}\r
					buffer.append("]");\r
					return buffer.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - basic case
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilder() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					builder.append("aStringMethod()", aStringMethod());\r
					builder.append("aFloatMethod()", aFloatMethod());\r
					builder.append("anArrayMethod()", anArrayMethod());\r
					builder.append("aBool", aBool);\r
					builder.append("aString", aString);\r
					builder.append("anInt", anInt);\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					if (aStringMethod() != null) {\r
						builder.append("aStringMethod()", aStringMethod());\r
					}\r
					builder.append("aFloatMethod()", aFloatMethod());\r
					if (anArrayMethod() != null) {\r
						builder.append("anArrayMethod()", anArrayMethod());\r
					}\r
					builder.append("aBool", aBool);\r
					if (aString != null) {\r
						builder.append("aString", aString);\r
					}\r
					builder.append("anInt", anInt);\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - custom array toString without limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderArray() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] anArrayMethod() {\r
					return new char[0];\r
				}\r
				java.util.List<Boolean> list;\r
			\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "intArray", "list", "object", "stringArray", "anArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] anArrayMethod() {\r
					return new char[0];\r
				}\r
				java.util.List<Boolean> list;\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					builder.append("AArray", Arrays.toString(AArray));\r
					builder.append("aBool", aBool);\r
					builder.append("anA", anA);\r
					builder.append("floatArray", Arrays.toString(floatArray));\r
					builder.append("intArray", Arrays.toString(intArray));\r
					builder.append("list", list);\r
					builder.append("object", object);\r
					builder.append("stringArray", Arrays.toString(stringArray));\r
					builder.append("anArrayMethod()", Arrays.toString(anArrayMethod()));\r
					return builder.toString();\r
				}\r
			\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - limit of elements but not arrays
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					builder.append("AArray", AArray);\r
					builder.append("aBool", aBool);\r
					builder.append("anA", anA);\r
					builder.append("floatArray", floatArray);\r
					builder.append("hashMap", hashMap != null ? toString(hashMap.entrySet(), maxLen) : null);\r
					builder.append("intArray", intArray);\r
					builder.append("integerCollection", integerCollection != null ? toString(integerCollection, maxLen) : null);\r
					builder.append("list", list != null ? toString(list, maxLen) : null);\r
					builder.append("object", object);\r
					builder.append("stringArray", stringArray);\r
					builder.append("wildCollection", wildCollection != null ? toString(wildCollection, maxLen) : null);\r
					builder.append("charArrayMethod()", charArrayMethod());\r
					builder.append("floatArrayMethod()", floatArrayMethod());\r
					return builder.toString();\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}


	/**
	 * Custom ToString() builder - custom array toString and limit of elements
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderArrayLimit() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					builder.append("AArray", AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)) : null);\r
					builder.append("aBool", aBool);\r
					builder.append("anA", anA);\r
					builder.append("floatArray", floatArray != null ? Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))) : null);\r
					builder.append("hashMap", hashMap != null ? toString(hashMap.entrySet(), maxLen) : null);\r
					builder.append("intArray", intArray != null ? Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))) : null);\r
					builder.append("integerCollection", integerCollection != null ? toString(integerCollection, maxLen) : null);\r
					builder.append("list", list != null ? toString(list, maxLen) : null);\r
					builder.append("object", object);\r
					builder.append("stringArray", stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)) : null);\r
					builder.append("wildCollection", wildCollection != null ? toString(wildCollection, maxLen) : null);\r
					builder.append("charArrayMethod()", charArrayMethod() != null ? Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))) : null);\r
					builder.append("floatArrayMethod()", floatArrayMethod() != null ? Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))) : null);\r
					return builder.toString();\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - custom array toString and limit of elements, unique names needed
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderArrayLimitUnique() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				int[] intArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				Object builder;\r
				Object buffer;\r
				Object maxLen;\r
				Object len;\r
				Object collection;\r
				Object array;\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aBool", "intArray", "stringArray", "AArray", "list", "hashMap", "wildCollection", "integerCollection", "builder", "buffer",
				"maxLen", "len", "collection", "array" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				int[] intArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				Object builder;\r
				Object buffer;\r
				Object maxLen;\r
				Object len;\r
				Object collection;\r
				Object array;\r
				@Override\r
				public String toString() {\r
					final int maxLen2 = 10;\r
					ToStringBuilder builder2 = new ToStringBuilder(this);\r
					builder2.append("aBool", aBool);\r
					builder2.append("intArray", intArray != null ? Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen2))) : null);\r
					builder2.append("stringArray", stringArray != null ? Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen2)) : null);\r
					builder2.append("AArray", AArray != null ? Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen2)) : null);\r
					builder2.append("list", list != null ? toString(list, maxLen2) : null);\r
					builder2.append("hashMap", hashMap != null ? toString(hashMap.entrySet(), maxLen2) : null);\r
					builder2.append("wildCollection", wildCollection != null ? toString(wildCollection, maxLen2) : null);\r
					builder2.append("integerCollection", integerCollection != null ? toString(integerCollection, maxLen2) : null);\r
					builder2.append("builder", builder);\r
					builder2.append("buffer", buffer);\r
					builder2.append("maxLen", maxLen);\r
					builder2.append("len", len);\r
					builder2.append("collection", collection);\r
					builder2.append("array", array);\r
					return builder2.toString();\r
				}\r
				private String toString(Collection<?> collection2, int maxLen2) {\r
					StringBuilder builder2 = new StringBuilder();\r
					builder2.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection2.iterator(); iterator.hasNext() && i < maxLen2; i++) {\r
						if (i > 0) {\r
							builder2.append(", ");\r
						}\r
						builder2.append(iterator.next());\r
					}\r
					builder2.append("]");\r
					return builder2.toString();\r
				}\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - skip nulls, use keyword this, no one-line blocks
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderNullsThisNoBlocks() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.skipNulls= true;
		fSettings2.useBlocks= false;
		fSettings2.useKeywordThis= true;
		fSettings2.toStringStyle= 4;

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					if (this.AArray != null)\r
						builder.append("AArray", this.AArray);\r
					builder.append("aBool", this.aBool);\r
					if (this.anA != null)\r
						builder.append("anA", this.anA);\r
					if (this.floatArray != null)\r
						builder.append("floatArray", this.floatArray);\r
					if (this.hashMap != null)\r
						builder.append("hashMap", this.hashMap);\r
					if (this.intArray != null)\r
						builder.append("intArray", this.intArray);\r
					if (this.integerCollection != null)\r
						builder.append("integerCollection", this.integerCollection);\r
					if (this.list != null)\r
						builder.append("list", this.list);\r
					if (this.object != null)\r
						builder.append("object", this.object);\r
					if (this.stringArray != null)\r
						builder.append("stringArray", this.stringArray);\r
					if (this.wildCollection != null)\r
						builder.append("wildCollection", this.wildCollection);\r
					if (this.charArrayMethod() != null)\r
						builder.append("charArrayMethod()", this.charArrayMethod());\r
					if (this.floatArrayMethod() != null)\r
						builder.append("floatArrayMethod()", this.floatArrayMethod());\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - custom array, limit elements, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void customBuilderArrayLimitNulls() throws Exception {
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "aBool", "anInt", "anA", "floatArray", "hashMap", "intArray", "integerCollection", "list", "object", "stringArray",
				"wildCollection", "charArrayMethod", "floatArrayMethod" });
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 4;

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Arrays;\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
			 int anInt;\r
				Object object;\r
				A anA;\r
				int[] intArray;\r
				float[] floatArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				float[] floatArrayMethod() {\r
					return null;\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int maxLen = 10;\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					if (AArray != null) {\r
						builder.append("AArray", Arrays.asList(AArray).subList(0, Math.min(AArray.length, maxLen)));\r
					}\r
					builder.append("aBool", aBool);\r
					builder.append("anInt", anInt);\r
					if (anA != null) {\r
						builder.append("anA", anA);\r
					}\r
					if (floatArray != null) {\r
						builder.append("floatArray", Arrays.toString(Arrays.copyOf(floatArray, Math.min(floatArray.length, maxLen))));\r
					}\r
					if (hashMap != null) {\r
						builder.append("hashMap", toString(hashMap.entrySet(), maxLen));\r
					}\r
					if (intArray != null) {\r
						builder.append("intArray", Arrays.toString(Arrays.copyOf(intArray, Math.min(intArray.length, maxLen))));\r
					}\r
					if (integerCollection != null) {\r
						builder.append("integerCollection", toString(integerCollection, maxLen));\r
					}\r
					if (list != null) {\r
						builder.append("list", toString(list, maxLen));\r
					}\r
					if (object != null) {\r
						builder.append("object", object);\r
					}\r
					if (stringArray != null) {\r
						builder.append("stringArray", Arrays.asList(stringArray).subList(0, Math.min(stringArray.length, maxLen)));\r
					}\r
					if (wildCollection != null) {\r
						builder.append("wildCollection", toString(wildCollection, maxLen));\r
					}\r
					if (charArrayMethod() != null) {\r
						builder.append("charArrayMethod()", Arrays.toString(Arrays.copyOf(charArrayMethod(), Math.min(charArrayMethod().length, maxLen))));\r
					}\r
					if (floatArrayMethod() != null) {\r
						builder.append("floatArrayMethod()", Arrays.toString(Arrays.copyOf(floatArrayMethod(), Math.min(floatArrayMethod().length, maxLen))));\r
					}\r
					return builder.toString();\r
				}\r
				private String toString(Collection<?> collection, int maxLen) {\r
					StringBuilder builder = new StringBuilder();\r
					builder.append("[");\r
					int i = 0;\r
					for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {\r
						if (i > 0) {\r
							builder.append(", ");\r
						}\r
						builder.append(iterator.next());\r
					}\r
					builder.append("]");\r
					return builder.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - chained calls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedCustomBuilder() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.chainCalls= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					builder.append("aStringMethod()", aStringMethod()).append("aFloatMethod()", aFloatMethod()).append("anArrayMethod()", anArrayMethod()).append("aBool", aBool).append("aString", aString).append("anInt", anInt);\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - chained calls, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedCustomBuilderNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aString", "aBool", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.chainCalls= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					if (aStringMethod() != null) {\r
						builder.append("aStringMethod()", aStringMethod());\r
					}\r
					builder.append("aFloatMethod()", aFloatMethod());\r
					if (anArrayMethod() != null) {\r
						builder.append("anArrayMethod()", anArrayMethod());\r
					}\r
					if (aString != null) {\r
						builder.append("aString", aString);\r
					}\r
					builder.append("aBool", aBool).append("anInt", anInt);\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom ToString() builder - chained calls, add comment
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedCustomBuilderComments() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aString", "aBool", "anInt" });
		fSettings2.createComments= true;
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.chainCalls= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import com.pack.ToStringBuilder;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					builder.append("aStringMethod()", aStringMethod()).append("aFloatMethod()", aFloatMethod()).append("anArrayMethod()", anArrayMethod()).append("aString", aString).append("aBool", aBool).append("anInt", anInt);\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom toString() builder - alternative class, basic case
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void alternativeCustomBuilder() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.className= "org.another.pack.AnotherToStringCreator";
		fSettings2.customBuilderSettings.variableName= "creator";
		fSettings2.customBuilderSettings.appendMethod= "addSth";
		fSettings2.customBuilderSettings.resultMethod= "getResult";
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import org.another.pack.AnotherToStringCreator;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					AnotherToStringCreator creator = new AnotherToStringCreator(this);\r
					creator.addSth(aStringMethod(), "aStringMethod()");\r
					creator.addSth(aFloatMethod(), "aFloatMethod()");\r
					creator.addSth(anArrayMethod(), "anArrayMethod()");\r
					creator.addSth(aBool, "aBool");\r
					creator.addSth(aString, "aString");\r
					creator.addSth("anInt", anInt);\r
					return creator.getResult();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom toString() builder - alternative class, unique names needed
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void alternativeCustomBuilderUnique() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				int[] intArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				Object builder;\r
				Object buffer;\r
				Object maxLen;\r
				Object len;\r
				Object collection;\r
				Object array;\r
				Object creator;\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aBool", "intArray", "stringArray", "AArray", "list", "hashMap", "wildCollection", "integerCollection", "builder", "buffer",
				"maxLen", "len", "collection", "array", "creator" });
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.className= "org.another.pack.AnotherToStringCreator";
		fSettings2.customBuilderSettings.variableName= "creator";
		fSettings2.customBuilderSettings.appendMethod= "addSth";
		fSettings2.customBuilderSettings.resultMethod= "getResult";
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			import org.another.pack.AnotherToStringCreator;\r
			\r
			public class A {\r
			\r
				boolean aBool;\r
				int[] intArray;\r
				String[] stringArray;\r
				A[] AArray;\r
				List list;\r
				HashMap hashMap;\r
				Collection wildCollection;\r
				Collection integerCollection;\r
				Object builder;\r
				Object buffer;\r
				Object maxLen;\r
				Object len;\r
				Object collection;\r
				Object array;\r
				Object creator;\r
				@Override\r
				public String toString() {\r
					AnotherToStringCreator creator2 = new AnotherToStringCreator(this);\r
					creator2.addSth(aBool, "aBool");\r
					creator2.addSth(intArray, "intArray");\r
					creator2.addSth(stringArray, "stringArray");\r
					creator2.addSth(AArray, "AArray");\r
					creator2.addSth(list, "list");\r
					creator2.addSth(hashMap, "hashMap");\r
					creator2.addSth(wildCollection, "wildCollection");\r
					creator2.addSth(integerCollection, "integerCollection");\r
					creator2.addSth(builder, "builder");\r
					creator2.addSth(buffer, "buffer");\r
					creator2.addSth(maxLen, "maxLen");\r
					creator2.addSth(len, "len");\r
					creator2.addSth(collection, "collection");\r
					creator2.addSth(array, "array");\r
					creator2.addSth(creator, "creator");\r
					return creator2.getResult();\r
				}\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom toString() builder - alternative class, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void alternativeCustomBuilderNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aString", "aBool", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.className= "org.another.pack.AnotherToStringCreator";
		fSettings2.customBuilderSettings.variableName= "creator";
		fSettings2.customBuilderSettings.appendMethod= "addSth";
		fSettings2.customBuilderSettings.resultMethod= "getResult";
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import org.another.pack.AnotherToStringCreator;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					AnotherToStringCreator creator = new AnotherToStringCreator(this);\r
					if (aStringMethod() != null) {\r
						creator.addSth(aStringMethod(), "aStringMethod()");\r
					}\r
					creator.addSth(aFloatMethod(), "aFloatMethod()");\r
					if (anArrayMethod() != null) {\r
						creator.addSth(anArrayMethod(), "anArrayMethod()");\r
					}\r
					if (aString != null) {\r
						creator.addSth(aString, "aString");\r
					}\r
					creator.addSth(aBool, "aBool");\r
					creator.addSth("anInt", anInt);\r
					return creator.getResult();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom toString() builder - alternative class, chained calls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedAlternativeCustomBuilderCreator() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "aFloatMethod", "anArrayMethod", "aBool", "aString", "anInt" });
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.className= "org.another.pack.AnotherToStringCreator";
		fSettings2.customBuilderSettings.variableName= "creator";
		fSettings2.customBuilderSettings.appendMethod= "addSth";
		fSettings2.customBuilderSettings.resultMethod= "getResult";
		fSettings2.customBuilderSettings.chainCalls= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import org.another.pack.AnotherToStringCreator;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					AnotherToStringCreator creator = new AnotherToStringCreator(this);\r
					creator.addSth(aStringMethod(), "aStringMethod()").addSth(aFloatMethod(), "aFloatMethod()").addSth(anArrayMethod(), "anArrayMethod()").addSth(aBool, "aBool");\r
					creator.addSth(aString, "aString").addSth("anInt", anInt);\r
					return creator.getResult();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * string builder - custom array, limit elements, JDK1.4, use prefixes and suffixes for local variables and parameters
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void builderArrayLimit1_4Prefixes() throws Exception {
		setCompilerLevels(false, false);
		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				int[] intArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				\r
			}\r
			""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "AArray", "hashMap", "intArray", "integerCollection", "list", "wildCollection", "charArrayMethod"});
		fSettings2.customArrayToString= true;
		fSettings2.limitElements= true;
		fSettings2.toStringStyle= 1;

		IJavaProject project= fRoot.getJavaProject();
		project.setOption(AssistOptions.OPTION_LocalPrefixes, "l_");
		project.setOption(AssistOptions.OPTION_LocalSuffixes, "_l");
		project.setOption(AssistOptions.OPTION_ArgumentPrefixes, "a_");
		project.setOption(AssistOptions.OPTION_ArgumentSuffixes, "_a");

		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import java.util.Collection;\r
			import java.util.HashMap;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			\r
			public class A {\r
			\r
				int[] intArray;\r
				A[] AArray;\r
				char[] charArrayMethod() {\r
					return new char[0];\r
				}\r
				List<Boolean> list;\r
				HashMap<Integer, String> hashMap;\r
				Collection<?> wildCollection;\r
				Collection<Integer> integerCollection;\r
				@Override\r
				public String toString() {\r
					final int l_maxLen_l = 10;\r
					StringBuffer l_buffer_l = new StringBuffer();\r
					l_buffer_l.append("A [AArray=");\r
					l_buffer_l.append(AArray != null ? arrayToString(AArray, AArray.length, l_maxLen_l) : null);\r
					l_buffer_l.append(", hashMap=");\r
					l_buffer_l.append(hashMap != null ? toString(hashMap.entrySet(), l_maxLen_l) : null);\r
					l_buffer_l.append(", intArray=");\r
					l_buffer_l.append(intArray != null ? arrayToString(intArray, intArray.length, l_maxLen_l) : null);\r
					l_buffer_l.append(", integerCollection=");\r
					l_buffer_l.append(integerCollection != null ? toString(integerCollection, l_maxLen_l) : null);\r
					l_buffer_l.append(", list=");\r
					l_buffer_l.append(list != null ? toString(list, l_maxLen_l) : null);\r
					l_buffer_l.append(", wildCollection=");\r
					l_buffer_l.append(wildCollection != null ? toString(wildCollection, l_maxLen_l) : null);\r
					l_buffer_l.append(", charArrayMethod()=");\r
					l_buffer_l.append(charArrayMethod() != null ? arrayToString(charArrayMethod(), charArrayMethod().length, l_maxLen_l) : null);\r
					l_buffer_l.append("]");\r
					return l_buffer_l.toString();\r
				}\r
				private String toString(Collection a_collection_a, int a_maxLen_a) {\r
					StringBuffer l_buffer_l = new StringBuffer();\r
					l_buffer_l.append("[");\r
					int l_i_l = 0;\r
					for (Iterator l_iterator_l = a_collection_a.iterator(); l_iterator_l.hasNext() && l_i_l < a_maxLen_a; l_i_l++) {\r
						if (l_i_l > 0) {\r
							l_buffer_l.append(", ");\r
						}\r
						l_buffer_l.append(l_iterator_l.next());\r
					}\r
					l_buffer_l.append("]");\r
					return l_buffer_l.toString();\r
				}\r
				private String arrayToString(Object a_array_a, int a_len_a, int a_maxLen_a) {\r
					StringBuffer l_buffer_l = new StringBuffer();\r
					a_len_a = Math.min(a_len_a, a_maxLen_a);\r
					l_buffer_l.append("[");\r
					for (int l_i_l = 0; l_i_l < a_len_a; l_i_l++) {\r
						if (l_i_l > 0) {\r
							l_buffer_l.append(", ");\r
						}\r
						if (a_array_a instanceof int[]) {\r
							l_buffer_l.append(((int[]) a_array_a)[l_i_l]);\r
						}\r
						if (a_array_a instanceof char[]) {\r
							l_buffer_l.append(((char[]) a_array_a)[l_i_l]);\r
						}\r
						if (a_array_a instanceof Object[]) {\r
							l_buffer_l.append(((Object[]) a_array_a)[l_i_l]);\r
						}\r
					}\r
					l_buffer_l.append("]");\r
					return l_buffer_l.toString();\r
				}\r
				\r
			}\r
			""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom toString() builder - alternative class, chained calls, skip nulls
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedAlternativeCustomBuilderNulls() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "anArrayMethod", "aString", "aFloatMethod", "aBool", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.toStringStyle= 4;
		fSettings2.customBuilderSettings.className= "org.another.pack.AnotherToStringCreator";
		fSettings2.customBuilderSettings.variableName= "creator";
		fSettings2.customBuilderSettings.appendMethod= "addSth";
		fSettings2.customBuilderSettings.resultMethod= "getResult";
		fSettings2.customBuilderSettings.chainCalls= true;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import org.another.pack.AnotherToStringCreator;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					AnotherToStringCreator creator = new AnotherToStringCreator(this);\r
					if (aStringMethod() != null) {\r
						creator.addSth(aStringMethod(), "aStringMethod()");\r
					}\r
					if (anArrayMethod() != null) {\r
						creator.addSth(anArrayMethod(), "anArrayMethod()");\r
					}\r
					if (aString != null) {\r
						creator.addSth(aString, "aString");\r
					}\r
					creator.addSth(aFloatMethod(), "aFloatMethod()").addSth(aBool, "aBool");\r
					creator.addSth("anInt", anInt);\r
					return creator.getResult();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}

	/**
	 * Custom toString() builder - alternative class with append method that takes only one argument
	 * for most of the types
	 *
	 * @throws Exception if test failed
	 */
	@Test
	public void chainedOneArgumentCustomBuilders() throws Exception {

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", """
			package p;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
			\r
			}""", true, null);
		CompilationUnit oldCUNode= getCUNode(a);

		IPackageFragment packageFragment= fRoot.createPackageFragment("com.simple.pack", true, null);
		ICompilationUnit compilationUnit= packageFragment.getCompilationUnit("ToStringBuilder.java");
		compilationUnit
				.createType(
						"package com.simple.pack;\npublic class ToStringBuilder {\npublic ToStringBuilder(Object o){\n}\npublic ToStringBuilder append(Object o){\nreturn null;\n}\npublic ToStringBuilder append(String s1, String s2) {\nreturn null;\n}\npublic String toString(){\nreturn null;\n}\n}\n",
						null, true, null);

		IMember[] members= getMembers(a.getType("A"), new String[] { "aStringMethod", "anArrayMethod", "aString", "aFloatMethod", "aBool", "anInt" });
		fSettings2.skipNulls= true;
		fSettings2.customBuilderSettings.className= "com.simple.pack.ToStringBuilder";
		fSettings2.toStringStyle= 4;
		runOperation(a.getType("A"), members, null);

		String expected= """
			package p;\r
			\r
			import com.simple.pack.ToStringBuilder;\r
			\r
			public class A {\r
				\r
				boolean aBool;\r
				int anInt;\r
				String aString;\r
				A anA;\r
				float aFloatMethod() {\r
					return 3.3f;\r
				}\r
				String aStringMethod() {\r
					return "";\r
				}\r
				int[] anArrayMethod() {\r
					return new int[0];\r
				}\r
				@Override\r
				public String toString() {\r
					ToStringBuilder builder = new ToStringBuilder(this);\r
					if (aStringMethod() != null) {\r
						builder.append("aStringMethod()", aStringMethod());\r
					}\r
					if (anArrayMethod() != null) {\r
						builder.append(anArrayMethod());\r
					}\r
					if (aString != null) {\r
						builder.append("aString", aString);\r
					}\r
					builder.append(aFloatMethod());\r
					builder.append(aBool);\r
					builder.append(anInt);\r
					return builder.toString();\r
				}\r
			\r
			}""";

		compareSourceAssertCompilation(expected, a, oldCUNode);
	}
}
