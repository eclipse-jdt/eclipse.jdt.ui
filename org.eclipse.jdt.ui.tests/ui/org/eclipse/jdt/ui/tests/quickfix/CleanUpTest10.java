/*******************************************************************************
 * Copyright (c) 2020, 2022 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *     Christian Femers - Bug 579471
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java10ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests the cleanup features related to Java 10.
 */
public class CleanUpTest10 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java10ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testUseLocalVariableTypeInferenceOnPrimitive() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        int number = 0;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        var number = 0;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceOnLongWidening() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        long number = 0;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        var number = 0L;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceOnFloatWidening() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        float number = 0;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        var number = 0F;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceOnDoubleWidening() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        double number = 0;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        var number = 0D;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceOnHexaPrimitive() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        long number = 0x0;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        var number = 0x0L;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceOnParameterizedType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.ArrayList;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        ArrayList<String> parameterizedType = new ArrayList<String>();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.ArrayList;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        var parameterizedType = new ArrayList<String>();\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceParameterizedTypeWithDiamond() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        HashMap<Integer, String> parameterizedTypeWithDiamond = new HashMap<>();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        var parameterizedTypeWithDiamond = new HashMap<Integer, String>();\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotUseVarOnUninitializedVariable() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo(int doNotRefactorParameter) {\n" //
				+ "        int doNotRefactorUninitializedVariable;\n" //
				+ "        doNotRefactorUninitializedVariable = 0;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnLambdaType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private interface I1 {\n" //
				+ "        public void run(String s, int i, Boolean b);\n" //
				+ "    }\n" //
				+ "    public void foo(int doNotRefactorParameter) {\n" //
				+ "        I1 i1 = (String s, int i, Boolean b) -> { System.out.println(\"hello\"); };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnNarrowingType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    private int doNotRefactorField = 0;\n" //
				+ "\n" //
				+ "    public void foo(int doNotRefactorParameter) {\n" //
				+ "        short doNotRefactorNarrowingType = 0;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnDifferentTypes() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "import java.util.Map;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        Map<Integer, String> doNotRefactorDifferentTypes = new HashMap<Integer, String>();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        int doNotRefactorArray[] = new int[]{0};\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnDifferentTypeArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.ArrayList;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        ArrayList<? extends Integer> doNotRefactorDifferentTypeArguments = new ArrayList<Integer>();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnMultiDeclarations() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        double doNot = 0, refactor = .0, multiDeclarations = 1D;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnGenericMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.ArrayList;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        ArrayList<Integer> doNotRefactorGenericMethod = newInstance();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public <D> ArrayList<D> newInstance() {\n" //
				+ "        return new ArrayList<D>();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUseVarOnParameterizedMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.ArrayList;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        ArrayList<Integer> list = newParameterizedInstance();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public ArrayList<Integer> newParameterizedInstance() {\n" //
				+ "        return new ArrayList<Integer>();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.ArrayList;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        var list = newParameterizedInstance();\n" //
				+ "    }\n" //
				+ "\n" //
				+ "    public ArrayList<Integer> newParameterizedInstance() {\n" //
				+ "        return new ArrayList<Integer>();\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotUseVarOnInferedMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Collections;\n" //
				+ "import java.util.List;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        List<Integer> doNotRefactorInferedMethod = Collections.emptyList();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUseLocalVariableTypeInferenceParameterizedTypeFromCastExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo(Object o) {\n" //
				+ "        HashMap<Integer, String> parameterizedTypeFromCastExpression = (HashMap<Integer, String>) o;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo(Object o) {\n" //
				+ "        var parameterizedTypeFromCastExpression = (HashMap<Integer, String>) o;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceParameterizedTypeFromMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Collection;\n" //
				+ "import java.util.HashMap;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo(HashMap<Integer, String> m) {\n" //
				+ "        Collection<String> parameterizedTypeFromMethod = m.values();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Collection;\n" //
				+ "import java.util.HashMap;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo(HashMap<Integer, String> m) {\n" //
				+ "        var parameterizedTypeFromMethod = m.values();\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceParameterizedTypeFromSuperMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Collection;\n" //
				+ "import java.util.HashMap;\n" //
				+ "\n" //
				+ "public class E extends HashMap<Integer, String> {\n" //
				+ "    public void foo() {\n" //
				+ "        Collection<String> parameterizedTypeFromMethod = super.values();\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.Collection;\n" //
				+ "import java.util.HashMap;\n" //
				+ "\n" //
				+ "public class E extends HashMap<Integer, String> {\n" //
				+ "    public void foo() {\n" //
				+ "        var parameterizedTypeFromMethod = super.values();\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceParameterizedTypeFromVariable() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo(HashMap<Integer, String> m) {\n" //
				+ "        HashMap<Integer, String> parameterizedTypeFromVariable = m;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.HashMap;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo(HashMap<Integer, String> m) {\n" //
				+ "        var parameterizedTypeFromVariable = m;\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceIntoStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo(String[] array) {\n" //
				+ "        for (int i= 0; i < array.length; i++) {\n" //
				+ "            String arrayElement= array[i];\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo(String[] array) {\n" //
				+ "        for (var i= 0; i < array.length; i++) {\n" //
				+ "            var arrayElement= array[i];\n" //
				+ "        }\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotUseVarOnFromLambdaExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Function;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    public void foo() {\n" //
				+ "        Function<Integer, String> doNotUseVarOnFromLambdaExpression = i -> String.valueOf(i);\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnFromLambdaMethodReference() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "import java.util.function.Function;\n" //
				+ "\n" //
				+ "public class E1 {\n" //
				+ "    Function<String, Integer> field = String::length;\n" //
				+ "    public void foo() {\n" //
				+ "        Function<String, Integer> doNotUseVarOnFromLambdaMethodReference = String::length;\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUseLocalVariableTypeForArrays() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo(String[] array) {\n" //
				+ "        String[] a = array;\n" //
				+ "        String[] b = new String[] {\"a\", \"b\", \"c\"};\n" //
				+ "        String[][] c = new String[][] { {\"a\", \"b\", \"c\"}, {\"d\", \"e\", \"f\"} };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo(String[] array) {\n" //
				+ "        var a = array;\n" //
				+ "        var b = new String[] {\"a\", \"b\", \"c\"};\n" //
				+ "        var c = new String[][] { {\"a\", \"b\", \"c\"}, {\"d\", \"e\", \"f\"} };\n" //
				+ "    }\n" //
				+ "}\n";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotUseLocalVariableTypeForArrayInitialization() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        String[] a = {\"a\", \"b\", \"c\"};\n" //
				+ "        String[][] b = { {\"a\", \"b\", \"c\"}, {\"d\", \"e\", \"f\"} };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseCurlyBracesOnlyArrayInitializationForVar() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= "" //
				+ "package test1;\n" //
				+ "\n" //
				+ "public class E {\n" //
				+ "    public void foo() {\n" //
				+ "        var a = new String[0];\n" //
				+ "        var b = new String[][]{ {\"a\", \"b\", \"c\"}, {\"d\", \"e\", \"f\"} };\n" //
				+ "    }\n" //
				+ "}\n";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		/*
		 * As Array initialization requires and explicit target type, the code above must not change
		 * even if we activate the "Create array with curly if possible" cleanup.
		 */
		enable(CleanUpConstants.ARRAY_WITH_CURLY);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}
}
