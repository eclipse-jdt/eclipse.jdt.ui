/*******************************************************************************
 * Copyright (c) 2020, 2023 Fabrice TIERCELIN and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java10ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.junit.Rule;
import org.junit.Test;

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
		String sample= """
			package test1;
			
			public class E {
			    public void foo() {
			        int number = 0;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			public class E {
			    public void foo() {
			        var number = 0;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceOnLongWidening() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public void foo() {
			        long number = 0;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			public class E {
			    public void foo() {
			        var number = 0L;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceOnFloatWidening() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public void foo() {
			        float number = 0;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			public class E {
			    public void foo() {
			        var number = 0F;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceOnDoubleWidening() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public void foo() {
			        double number = 0;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			public class E {
			    public void foo() {
			        var number = 0D;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceOnHexaPrimitive() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public void foo() {
			        long number = 0x0;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			public class E {
			    public void foo() {
			        var number = 0x0L;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceOnParameterizedType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.ArrayList;
			
			public class E {
			    public void foo() {
			        ArrayList<String> parameterizedType = new ArrayList<String>();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			import java.util.ArrayList;
			
			public class E {
			    public void foo() {
			        var parameterizedType = new ArrayList<String>();
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceParameterizedTypeWithDiamond() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.HashMap;
			
			public class E {
			    public void foo() {
			        HashMap<Integer, String> parameterizedTypeWithDiamond = new HashMap<>();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			import java.util.HashMap;
			
			public class E {
			    public void foo() {
			        var parameterizedTypeWithDiamond = new HashMap<Integer, String>();
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceRemoveUnusedImport() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/573
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.Date;
			
			public class E {
			    public void foo() {
			        Date x = E2.value;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		String sample2= """
			package test1;
			
			import java.util.Date;
			
			public class E2 {
			    public static Date value = null;
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", sample2, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			public class E {
			    public void foo() {
			        var x = E2.value;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2 }, new String[] { expected, sample2 }, null);
	}

	@Test
	public void testDoNotUseVarOnUninitializedVariable() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    public void foo(int doNotRefactorParameter) {
			        int doNotRefactorUninitializedVariable;
			        doNotRefactorUninitializedVariable = 0;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnLambdaType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    private interface I1 {
			        public void run(String s, int i, Boolean b);
			    }
			    public void foo(int doNotRefactorParameter) {
			        I1 i1 = (String s, int i, Boolean b) -> { System.out.println("hello"); };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnNarrowingType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    private int doNotRefactorField = 0;
			
			    public void foo(int doNotRefactorParameter) {
			        short doNotRefactorNarrowingType = 0;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnDifferentTypes() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.HashMap;
			import java.util.Map;
			
			public class E1 {
			    public void foo() {
			        Map<Integer, String> doNotRefactorDifferentTypes = new HashMap<Integer, String>();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    public void foo() {
			        int doNotRefactorArray[] = new int[]{0};
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnDifferentTypeArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.ArrayList;
			
			public class E1 {
			    public void foo() {
			        ArrayList<? extends Integer> doNotRefactorDifferentTypeArguments = new ArrayList<Integer>();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnMultiDeclarations() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    public void foo() {
			        double doNot = 0, refactor = .0, multiDeclarations = 1D;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnGenericMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.ArrayList;
			
			public class E1 {
			    public void foo() {
			        ArrayList<Integer> doNotRefactorGenericMethod = newInstance();
			    }
			
			    public <D> ArrayList<D> newInstance() {
			        return new ArrayList<D>();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUseVarOnParameterizedMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.ArrayList;
			
			public class E1 {
			    public void foo() {
			        ArrayList<Integer> list = newParameterizedInstance();
			    }
			
			    public ArrayList<Integer> newParameterizedInstance() {
			        return new ArrayList<Integer>();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			import java.util.ArrayList;
			
			public class E1 {
			    public void foo() {
			        var list = newParameterizedInstance();
			    }
			
			    public ArrayList<Integer> newParameterizedInstance() {
			        return new ArrayList<Integer>();
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotUseVarOnInferedMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.Collections;
			import java.util.List;
			
			public class E1 {
			    public void foo() {
			        List<Integer> doNotRefactorInferedMethod = Collections.emptyList();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUseLocalVariableTypeInferenceParameterizedTypeFromCastExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.HashMap;
			
			public class E {
			    public void foo(Object o) {
			        HashMap<Integer, String> parameterizedTypeFromCastExpression = (HashMap<Integer, String>) o;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			import java.util.HashMap;
			
			public class E {
			    public void foo(Object o) {
			        var parameterizedTypeFromCastExpression = (HashMap<Integer, String>) o;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceParameterizedTypeFromMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.Collection;
			import java.util.HashMap;
			
			public class E {
			    public void foo(HashMap<Integer, String> m) {
			        Collection<String> parameterizedTypeFromMethod = m.values();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			import java.util.HashMap;
			
			public class E {
			    public void foo(HashMap<Integer, String> m) {
			        var parameterizedTypeFromMethod = m.values();
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceParameterizedTypeFromSuperMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.Collection;
			import java.util.HashMap;
			
			public class E extends HashMap<Integer, String> {
			    public void foo() {
			        Collection<String> parameterizedTypeFromMethod = super.values();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			import java.util.HashMap;
			
			public class E extends HashMap<Integer, String> {
			    public void foo() {
			        var parameterizedTypeFromMethod = super.values();
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceParameterizedTypeFromVariable() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.HashMap;
			
			public class E {
			    public void foo(HashMap<Integer, String> m) {
			        HashMap<Integer, String> parameterizedTypeFromVariable = m;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			import java.util.HashMap;
			
			public class E {
			    public void foo(HashMap<Integer, String> m) {
			        var parameterizedTypeFromVariable = m;
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testUseLocalVariableTypeInferenceIntoStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public void foo(String[] array) {
			        for (int i= 0; i < array.length; i++) {
			            String arrayElement= array[i];
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			public class E {
			    public void foo(String[] array) {
			        for (var i= 0; i < array.length; i++) {
			            var arrayElement= array[i];
			        }
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotUseVarOnFromLambdaExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.function.Function;
			
			public class E1 {
			    public void foo() {
			        Function<Integer, String> doNotUseVarOnFromLambdaExpression = i -> String.valueOf(i);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseVarOnFromLambdaMethodReference() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.function.Function;
			
			public class E1 {
			    Function<String, Integer> field = String::length;
			    public void foo() {
			        Function<String, Integer> doNotUseVarOnFromLambdaMethodReference = String::length;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUseLocalVariableTypeForArrays() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public void foo(String[] array) {
			        String[] a = array;
			        String[] b = new String[] {"a", "b", "c"};
			        String[][] c = new String[][] { {"a", "b", "c"}, {"d", "e", "f"} };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		String expected= """
			package test1;
			
			public class E {
			    public void foo(String[] array) {
			        var a = array;
			        var b = new String[] {"a", "b", "c"};
			        var c = new String[][] { {"a", "b", "c"}, {"d", "e", "f"} };
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected }, null);
	}

	@Test
	public void testDoNotUseLocalVariableTypeForArrayInitialization() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public void foo() {
			        String[] a = {"a", "b", "c"};
			        String[][] b = { {"a", "b", "c"}, {"d", "e", "f"} };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_VAR);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testDoNotUseCurlyBracesOnlyArrayInitializationForVar() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public void foo() {
			        var a = new String[0];
			        var b = new String[][]{ {"a", "b", "c"}, {"d", "e", "f"} };
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", sample, false, null);

		/*
		 * As Array initialization requires and explicit target type, the code above must not change
		 * even if we activate the "Create array with curly if possible" cleanup.
		 */
		enable(CleanUpConstants.ARRAY_WITH_CURLY);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}
}
