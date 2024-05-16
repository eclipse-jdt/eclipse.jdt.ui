/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - Split the tests
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;

import org.eclipse.ui.IEditorInput;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.tests.core.rules.Java1d5ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.fix.IMultiFix;
import org.eclipse.jdt.internal.ui.fix.Java50CleanUp;
import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMarkerResolutionGenerator;

/**
 * Tests the cleanup features related to Java 5 (i.e. Tiger).
 */
public class CleanUpTest1d5 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java1d5ProjectTestSetup();

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testAddOverride1d5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			interface I {
			    void m();
			    boolean equals(Object obj);
			}
			interface J extends I {
			    void m(); // @Override error in 1.5, not in 1.6
			}
			class X implements J {
			    public void m() {} // @Override error in 1.5, not in 1.6
			    public int hashCode() { return 0; }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("I.java", given, false, null);

		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE);
		enable(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION);

		String expected= """
			package test1;
			interface I {
			    void m();
			    boolean equals(Object obj);
			}
			interface J extends I {
			    void m(); // @Override error in 1.5, not in 1.6
			}
			class X implements J {
			    public void m() {} // @Override error in 1.5, not in 1.6
			    @Override
			    public int hashCode() { return 0; }
			}
			""";

		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] {cu}, new String[] {expected}, null);
	}

	@Test
	public void testAddAll() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			import java.util.ArrayList;
			import java.util.Collection;
			import java.util.Iterator;
			import java.util.List;
			import java.util.Map;
			import java.util.Set;
			
			public class E1 extends ArrayList<java.util.Date> {
			    private java.util.Date[] innerArray = new java.util.Date[10];
			
			    private List<java.util.Date> innerList = new ArrayList<java.util.Date>();
			
			    public Collection<? super java.util.Date> replaceAddWithForLoopByCollectionsAddAll(
			            List<? super java.util.Date> output, java.util.Date[] elems1, java.sql.Date[] elems2) {
			        // Keep this comment
			        for (int i = 0; i < elems1.length; i++) {
			            output.add(elems1[i]);
			        }
			        for (int i = 0; i < elems2.length; i++) {
			            output.add(elems2[i]);
			        }
			
			        return output;
			    }
			
			    public Collection<? super java.util.Date> replaceUsingVariableForEnd(
			            List<? super java.util.Date> output, java.util.Date[] elements1, java.sql.Date[] elements2) {
			        // Keep this comment
			        for (int i = 0, len = elements1.length; i < len; i++) {
			            output.add(elements1[i]);
			        }
			        for (int i = 0, len = elements2.length; i < len; i++) {
			            output.add(elements2[i]);
			        }
			
			        return output;
			    }
			
			    public Collection<? super java.util.Date> replaceStartingWithVariableForEnd(
			            List<? super java.util.Date> output, java.util.Date[] elems1, java.sql.Date[] elems2) {
			        // Keep this comment
			        for (int len = elems1.length, i = 0; i < len; i++) {
			            output.add(elems1[i]);
			        }
			        for (int len = elems2.length, i = 0; i < len; i++) {
			            output.add(elems2[i]);
			        }
			
			        return output;
			    }
			
			    public Collection<? super java.util.Date> replaceBackwardLoopOnSet(
			            Set<? super java.util.Date> output, java.util.Date[] elems1, java.sql.Date[] elems2) {
			        // Keep this comment
			        for (int i = elems1.length - 1; i >= 0; i--) {
			            output.add(elems1[i]);
			        }
			        for (int i = elems2.length - 1; 0 <= i; i--) {
			            output.add(elems2[i]);
			        }
			
			        return output;
			    }
			
			    public void replaceAddWithNotEqualOperator(Collection<? super java.util.Date> output, java.util.Date[] dates) {
			        // Keep this comment
			        for (int i = 0; i != dates.length; i++) {
			            output.add(dates[i]);
			        }
			    }
			
			    public void replaceAddWithForLoopByCollectionsAddAll(Collection<? super java.util.Date> output, java.util.Date[] dates) {
			        // Keep this comment
			        for (int i = 0; i < dates.length; i++) {
			            output.add(dates[i]);
			        }
			    }
			
			    public void replaceLoopWithFieldArray(Collection<? super java.util.Date> output) {
			        // Keep this comment
			        for (int i = 0; i < innerArray.length; i++) {
			            output.add(innerArray[i]);
			        }
			    }
			
			    public void replaceForeachWithFieldArray(Collection<? super java.util.Date> output) {
			        // Keep this comment
			        for (java.util.Date d : this.innerArray) {
			            output.add(d);
			        }
			    }
			
			    public void replaceLoopWithFieldList(Collection<? super java.util.Date> output) {
			        // Keep this comment
			        for (int i = 0; i < this.innerList.size(); i++) {
			            output.add(this.innerList.get(i));
			        }
			    }
			
			    public void replaceForeachWithFieldList(Collection<? super java.util.Date> output) {
			        // Keep this comment
			        for (java.util.Date d : innerList) {
			            output.add(d);
			        }
			    }
			
			    public Collection replaceAddWithForEachByCollectionsAddAll(
			            List<? super java.util.Date> output, java.util.Date[] elems1, java.sql.Date[] elems2) {
			        // Keep this comment
			        for (java.util.Date d : elems1) {
			            output.add(d);
			        }
			        for (java.sql.Date d : elems2) {
			            output.add(d);
			        }
			
			        return output;
			    }
			
			    public void replaceAddWithForEachByCollectionsAddAll(Collection<? super java.util.Date> output, java.util.Date[] dates) {
			        // Keep this comment
			        for (java.util.Date date : dates) {
			            output.add(date);
			        }
			    }
			
			    public Map<String, List<String>> replaceLoopOnCollectionAsExpressionWithArray(
			            Map<String, List<String>> mapToFill, String[] inputList) {
			        // Keep this comment
			        for (String input : inputList) {
			            mapToFill.get("foo").add(input);
			        }
			
			        return mapToFill;
			    }
			
			    public Collection replaceLoopOnRawCollectionWithArray(
			            List colToFill, String[] inputList) {
			        // Keep this comment
			        for (String input : inputList) {
			            colToFill.add(input);
			        }
			
			        return colToFill;
			    }
			
			    public Map<String, List<String>> replaceLoopOnCollectionAsExpressionWithList(
			            Map<String, List<String>> mapToFill, List<String> inputList) {
			        // Keep this comment
			        for (String input : inputList) {
			            mapToFill.get("foo").add(input);
			        }
			
			        return mapToFill;
			    }
			
			    public Collection replaceLoopOnRawCollectionWithList(
			            List colToFill, List<String> inputList) {
			        // Keep this comment
			        for (String input : inputList) {
			            colToFill.add(input);
			        }
			
			        return colToFill;
			    }
			
			    public Collection<String> replaceAddWithForLoopByAddAll(List<String> col, List<String> output) {
			        // Keep this comment
			        for (int i = 0; i < col.size(); i++) {
			            output.add(col.get(i));
			        }
			
			        return output;
			    }
			
			    public Collection<String> replaceAddWithForEachByAddAll(Collection<String> col, List<String> output) {
			        // Keep this comment
			        for (String s : col) {
			            output.add(s);
			        }
			
			        return output;
			    }
			
			    private String doSomething(String s) {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", given, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_ADD_ALL);

		String expected= """
			package test1;
			
			import java.util.ArrayList;
			import java.util.Arrays;
			import java.util.Collection;
			import java.util.Collections;
			import java.util.Iterator;
			import java.util.List;
			import java.util.Map;
			import java.util.Set;
			
			public class E1 extends ArrayList<java.util.Date> {
			    private java.util.Date[] innerArray = new java.util.Date[10];
			
			    private List<java.util.Date> innerList = new ArrayList<java.util.Date>();
			
			    public Collection<? super java.util.Date> replaceAddWithForLoopByCollectionsAddAll(
			            List<? super java.util.Date> output, java.util.Date[] elems1, java.sql.Date[] elems2) {
			        // Keep this comment
			        Collections.addAll(output, elems1);
			        Collections.addAll(output, elems2);
			
			        return output;
			    }
			
			    public Collection<? super java.util.Date> replaceUsingVariableForEnd(
			            List<? super java.util.Date> output, java.util.Date[] elements1, java.sql.Date[] elements2) {
			        // Keep this comment
			        Collections.addAll(output, elements1);
			        Collections.addAll(output, elements2);
			
			        return output;
			    }
			
			    public Collection<? super java.util.Date> replaceStartingWithVariableForEnd(
			            List<? super java.util.Date> output, java.util.Date[] elems1, java.sql.Date[] elems2) {
			        // Keep this comment
			        Collections.addAll(output, elems1);
			        Collections.addAll(output, elems2);
			
			        return output;
			    }
			
			    public Collection<? super java.util.Date> replaceBackwardLoopOnSet(
			            Set<? super java.util.Date> output, java.util.Date[] elems1, java.sql.Date[] elems2) {
			        // Keep this comment
			        Collections.addAll(output, elems1);
			        Collections.addAll(output, elems2);
			
			        return output;
			    }
			
			    public void replaceAddWithNotEqualOperator(Collection<? super java.util.Date> output, java.util.Date[] dates) {
			        // Keep this comment
			        Collections.addAll(output, dates);
			    }
			
			    public void replaceAddWithForLoopByCollectionsAddAll(Collection<? super java.util.Date> output, java.util.Date[] dates) {
			        // Keep this comment
			        Collections.addAll(output, dates);
			    }
			
			    public void replaceLoopWithFieldArray(Collection<? super java.util.Date> output) {
			        // Keep this comment
			        Collections.addAll(output, innerArray);
			    }
			
			    public void replaceForeachWithFieldArray(Collection<? super java.util.Date> output) {
			        // Keep this comment
			        Collections.addAll(output, this.innerArray);
			    }
			
			    public void replaceLoopWithFieldList(Collection<? super java.util.Date> output) {
			        // Keep this comment
			        output.addAll(this.innerList);
			    }
			
			    public void replaceForeachWithFieldList(Collection<? super java.util.Date> output) {
			        // Keep this comment
			        output.addAll(innerList);
			    }
			
			    public Collection replaceAddWithForEachByCollectionsAddAll(
			            List<? super java.util.Date> output, java.util.Date[] elems1, java.sql.Date[] elems2) {
			        // Keep this comment
			        Collections.addAll(output, elems1);
			        Collections.addAll(output, elems2);
			
			        return output;
			    }
			
			    public void replaceAddWithForEachByCollectionsAddAll(Collection<? super java.util.Date> output, java.util.Date[] dates) {
			        // Keep this comment
			        Collections.addAll(output, dates);
			    }
			
			    public Map<String, List<String>> replaceLoopOnCollectionAsExpressionWithArray(
			            Map<String, List<String>> mapToFill, String[] inputList) {
			        // Keep this comment
			        Collections.addAll(mapToFill.get("foo"), inputList);
			
			        return mapToFill;
			    }
			
			    public Collection replaceLoopOnRawCollectionWithArray(
			            List colToFill, String[] inputList) {
			        // Keep this comment
			        colToFill.addAll(Arrays.asList(inputList));
			
			        return colToFill;
			    }
			
			    public Map<String, List<String>> replaceLoopOnCollectionAsExpressionWithList(
			            Map<String, List<String>> mapToFill, List<String> inputList) {
			        // Keep this comment
			        mapToFill.get("foo").addAll(inputList);
			
			        return mapToFill;
			    }
			
			    public Collection replaceLoopOnRawCollectionWithList(
			            List colToFill, List<String> inputList) {
			        // Keep this comment
			        colToFill.addAll(inputList);
			
			        return colToFill;
			    }
			
			    public Collection<String> replaceAddWithForLoopByAddAll(List<String> col, List<String> output) {
			        // Keep this comment
			        output.addAll(col);
			
			        return output;
			    }
			
			    public Collection<String> replaceAddWithForEachByAddAll(Collection<String> col, List<String> output) {
			        // Keep this comment
			        output.addAll(col);
			
			        return output;
			    }
			
			    private String doSomething(String s) {
			        return null;
			    }
			}
			""";

		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.AddAllCleanup_description)));
	}

	@Test
	public void testDoNotAddAll() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.ArrayList;
			import java.util.Collection;
			import java.util.Iterator;
			import java.util.List;
			import java.util.Map;
			import java.util.Set;
			
			public class E1 extends ArrayList<java.util.Date> {
			    private List<java.util.Date> innerList = new ArrayList<java.util.Date>();
			
			    @Override
			    public boolean addAll(Collection<? extends java.util.Date> doNotRefactorWithCyclicCalls) {
			        for (java.util.Date doNotRefactorWithCyclicCall : doNotRefactorWithCyclicCalls) {
			            add(doNotRefactorWithCyclicCall);
			        }
			        return true;
			    }
			
			    public List<? super java.util.Date>[] doNotReplaceWithUsesVariableForEnd(
			            List<? super java.util.Date>[] output, java.util.Date[] elems1, java.util.Date[] elems2) {
			        for (int i = 0, len = elems1.length; i < len; i++) {
			            output[len].add(elems1[i]);
			        }
			
			        return output;
			    }
			
			    public Collection<? super java.util.Date> doNotReplaceBackwardLoopOnCollection(
			            Collection<? super java.util.Date> output, java.util.Date[] elems1, java.sql.Date[] elems2) {
			        for (int i = elems1.length - 1; i >= 0; i--) {
			            output.add(elems1[i]);
			        }
			        for (int i = elems2.length - 1; 0 <= i; i--) {
			            output.add(elems2[i]);
			        }
			
			        return output;
			    }
			
			    public boolean doNotRefactorInsideImplementation(Collection<? extends java.util.Date> dates) {
			        for (java.util.Date date : dates) {
			            this.add(date);
			        }
			        return true;
			    }
			
			    public void doNotReplaceLoopWithFieldList(Collection<? super java.util.Date> output, List<java.util.Date> input) {
			        for (int i = 0; i < input.size(); i++) {
			            output.add(innerList.get(i));
			        }
			    }
			
			    public Map<String, List<String>> doNotRefactorForEachWithListUsingLoopVariable(
			            Map<String, List<String>> mapToFill, List<String> inputList) {
			        for (String input : inputList) {
			            mapToFill.get(input).add(input);
			        }
			
			        return mapToFill;
			    }
			
			    public Map<String, List<String>> doNotRefactorForLoopWithListUsingLoopIndex(
			            Map<String, List<String>> mapToFill, List<String> inputList) {
			        for (int i = 0; i < inputList.size(); i++) {
			            mapToFill.get(inputList.get(i)).add(inputList.get(i));
			        }
			
			        return mapToFill;
			    }
			
			    public Map<String, List<String>> doNotRefactorForLoopWithListUsingLoopIterator(
			            Map<String, List<String>> mapToFill, List<String> inputList) {
			        String input = null;
			        for (Iterator<String> it = inputList.iterator(); it.hasNext(); input = it.next()) {
			            mapToFill.get(input).add(input);
			        }
			
			        return mapToFill;
			    }
			
			    public void doNotRefactorForLoopWithListUsingLoopIterator(List<String> col) {
			        for (Iterator<String> it = col.iterator(); it.hasNext();) {
			            System.out.println(it.next());
			        }
			    }
			
			    public Map<String, List<String>> doNotRefactorForEachWithArrayUsingLoopVariable(
			            Map<String, List<String>> mapToFill, String[] inputArray) {
			        for (String input : inputArray) {
			            mapToFill.get(input).add(input);
			        }
			
			        return mapToFill;
			    }
			
			    public Map<String, List<String>> doNotRefactorForLoopWithArrayUsingLoopIndex(
			            Map<String, List<String>> mapToFill, String[] inputArray) {
			        for (int i = 0; i < inputArray.length; i++) {
			            mapToFill.get(inputArray[i]).add(inputArray[i]);
			        }
			
			        return mapToFill;
			    }
			
			    public Collection<String> doNotRefactorForLoopAddMethodResult(List<String> output, String[] elems) {
			        for (int i = 0; i < elems.length; i++) {
			            output.add(doSomething(elems[i]));
			        }
			
			        return output;
			    }
			
			    public Collection<String> doNotRefactorForEachAddMethodResult(List<String> output, String[] elems) {
			        for (String s : elems) {
			            output.add(doSomething(s));
			        }
			
			        return output;
			    }
			
			    public Collection<String> doNotRefactorForLoopAddMethodResult(List<String> output, List<String> col) {
			        for (int i = 0; i < col.size(); i++) {
			            output.add(doSomething(col.get(i)));
			        }
			
			        return output;
			    }
			
			    public Collection<String> doNotRefactorForEachAddMethodResult(List<String> output, List<String> col) {
			        for (String s : col) {
			            output.add(doSomething(s));
			        }
			
			        return output;
			    }
			
			    private String doSomething(String s) {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_ADD_ALL);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testJava50ForLoop01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.ArrayList;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo() {
			        List<E1> list= new ArrayList<E1>();
			        // Keep this comment
			        for (Iterator<E1> iter = list.iterator(); iter.hasNext();) {
			            E1 e = iter.next();
			            System.out.println(e);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E1 {
			    public void foo() {
			        List<E1> list= new ArrayList<E1>();
			        // Keep this comment
			        for (E1 e : list) {
			            System.out.println(e);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.ArrayList;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo() {
			        List<E1> list1= new ArrayList<E1>();
			        List<E1> list2= new ArrayList<E1>();
			        for (Iterator<E1> iter = list1.iterator(); iter.hasNext();) {
			            E1 e1 = iter.next();
			            for (Iterator iterator = list2.iterator(); iterator.hasNext();) {
			                E1 e2 = (E1) iterator.next();
			                System.out.println(e2);
			            }
			            System.out.println(e1);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E1 {
			    public void foo() {
			        List<E1> list1= new ArrayList<E1>();
			        List<E1> list2= new ArrayList<E1>();
			        for (E1 e1 : list1) {
			            for (Object element : list2) {
			                E1 e2 = (E1) element;
			                System.out.println(e2);
			            }
			            System.out.println(e1);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int[] array={1,2,3,4};
			        for (int i=0;i<array.length;i++) {
			            String[] strs={"1", "2"};
			            for (int j = 1 - 1; j < strs.length; j++) {
			                System.out.println(array[i]+strs[j]);
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int[] array={1,2,3,4};
			        for (int element : array) {
			            String[] strs={"1", "2"};
			            for (String str : strs) {
			                System.out.println(element+str);
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int[] array= new int[10];
			        for (int i = 0; i < array.length; i++) {
			            for (int j = 0; j < array.length; j++) {
			                for (int k = 0; k < array.length; k++) {
			                }
			                for (int k = 0; k < array.length; k++) {
			                }
			            }
			            for (int j = 0; j < array.length; j++) {
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int[] array= new int[10];
			        for (int element : array) {
			            for (int element2 : array) {
			                for (int element3 : array) {
			                }
			                for (int element3 : array) {
			                }
			            }
			            for (int element2 : array) {
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int[] array= null;
			        for (int i = 0; --i < array.length;) {}
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testJava50ForLoop06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int a= 0;
			        for (a=0;a>0;a++) {}
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testJava50ForLoop07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int a= 0;
			        for (a=0;;a++) {}
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testJava50ForLoop08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int[] array= null;
			        int a= 0;
			        for (;a<array.length;a++) {}
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testJava50ForLoop09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int[] array= null;
			        for (int i = 0; i < array.length; i++) {
			            final int element= array[i];
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int[] array= null;
			        for (final int element : array) {
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int[] array= null;
			        int i;
			        for (i = 0; i < array.length; i++) {}
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testJava50ForLoop11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    private class E1Sub {
			        public int[] array;
			    }
			    private E1Sub e1sub;
			    public void foo() {
			        for (int i = 0; i < this.e1sub.array.length; i++) {
			            System.out.println(this.e1sub.array[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    private class E1Sub {
			        public int[] array;
			    }
			    private E1Sub e1sub;
			    public void foo() {
			        for (int element : this.e1sub.array) {
			            System.out.println(element);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public int[] array;
			    public void foo() {
			        for (int i = 0; i < this.array.length; i++) {
			            System.out.println(this.array[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    public int[] array;
			    public void foo() {
			        for (int element : this.array) {
			            System.out.println(element);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop13() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public int[] array1, array2;
			    public void foo() {
			        for (int i = array1.length - array2.length; i < 1; i++) {
			            System.out.println(1);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testJava50ForLoop14() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import test2.E3;
			public class E1 {
			    public void foo() {
			        E2 e2= new E2();
			        e2.foo();
			        E3 e3= new E3();
			        for (int i = 0; i < e3.array.length;i++) {
			            System.out.println(e3.array[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		sample= """
			package test1;
			public class E2 {
			    public void foo() {};
			}
			""";
		pack1.createCompilationUnit("E2.java", sample, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		sample= """
			package test2;
			public class E2 {}
			""";
		pack2.createCompilationUnit("E2.java", sample, false, null);

		sample= """
			package test2;
			public class E3 {
			    public E2[] array;
			}
			""";
		pack2.createCompilationUnit("E3.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import test2.E3;
			public class E1 {
			    public void foo() {
			        E2 e2= new E2();
			        e2.foo();
			        E3 e3= new E3();
			        for (test2.E2 element : e3.array) {
			            System.out.println(element);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop15() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class ForeachTest {
			    void foo(Object list) {
			        for (Iterator<String> iter= ((List<String>) list).iterator(); iter.hasNext(); ) {
			            String element = iter.next();
			            System.out.println(element);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.List;
			public class ForeachTest {
			    void foo(Object list) {
			        for (String element : ((List<String>) list)) {
			            System.out.println(element);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoopBug548002() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.List;
			public class ForeachTest {
			    void foo(List list) {
			        for (int i= 0; i < list.size(); ++i);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.List;
			public class ForeachTest {
			    void foo(List list) {
			        for (Object element : list);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoopBug550334() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.List;
			public class ForeachTest {
			    void foo(List list) {
			        String[] a= new String[]{"a", "b", "c"});
			        for (int i= 0; i < list.size(); ++i) {
			            list.get(i).append(a[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.List;
			public class ForeachTest {
			    void foo(List list) {
			        String[] a= new String[]{"a", "b", "c"});
			        for (int i= 0; i < list.size(); ++i) {
			            list.get(i).append(a[i]);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoopBug550672() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.List;
			import java.util.ArrayList;
			public class ForeachTest {
			    void foo(List list) {
			        List<File> a = new ArrayList<>();
			        List<File> b = new ArrayList<>();
			        for (int i = 0; i < a.size(); i++) {
			            System.out.print(a.get(i) + " " + b.get(i));
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.List;
			import java.util.ArrayList;
			public class ForeachTest {
			    void foo(List list) {
			        List<File> a = new ArrayList<>();
			        List<File> b = new ArrayList<>();
			        for (int i = 0; i < a.size(); i++) {
			            System.out.print(a.get(i) + " " + b.get(i));
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForBug560431_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.ArrayList;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo() {
			        List<E1> list= new ArrayList<E1>();
			        for (Iterator<E1> iter = list.iterator(); iter.hasNext();) {
			            E1 e = iter.next();
			            System.out.println("here");
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testJava50ForBug560431_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo() {
			        int[] array={1,2,3,4};
			        for (int i=0;i<array.length;i++) {
			            String[] strs={"1","2"};
			            for (int j = 0; j < strs.length; j++) {
			                System.out.println("here");
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_ONLY_IF_LOOP_VAR_USED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testJava50ForLoopBug578910() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.Map;
			
			public class E1 {
			
			    public void foo(Map<String, String> extensionMap) {
				       for (Iterator<String> iterator = extensionMap.keySet().iterator(); iterator.hasNext();) {
				   	       try {
				               String expression = iterator.next();
				               System.out.println(expression);
				           } catch (Exception e) {
				           }
				       }
				       int j = 7;
				       for (Iterator<String> iterator = extensionMap.keySet().iterator(); iterator.hasNext();) {
				           do {
				               String expression = iterator.next();
				               System.out.println(expression);
				           } while (j-- > 0);
				       }
				       for (Iterator<String> iterator = extensionMap.keySet().iterator(); iterator.hasNext();) {
				           String expression = null;
				           if (extensionMap != null) {
				               expression = iterator.next();
			            }
				           System.out.println(expression);
				       }
			    }
			}""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.Map;
			
			public class E1 {
			
			    public void foo(Map<String, String> extensionMap) {
				       for (String expression : extensionMap.keySet()) {
				   	       try {
				               System.out.println(expression);
				           } catch (Exception e) {
				           }
				       }
				       int j = 7;
				       for (String expression : extensionMap.keySet()) {
				           do {
				               System.out.println(expression);
				           } while (j-- > 0);
				       }
				       for (String expression : extensionMap.keySet()) {
				           if (extensionMap != null) {
			            }
				           System.out.println(expression);
				       }
			    }
			}""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

    @Test
    public void testJava50ForLoopIssue109() throws Exception {
            IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
            String sample= """
				package test1;
				import java.util.List;
				import java.util.ArrayList;
				public class E1 {
				    public void foo() {
				        List<String> list1 = new ArrayList();
				        for (int i = 0; i < list1.size(); i++) {
				            String s1 = list1.get(i);
				            String s2 = list1.get(i);
				            System.out.println(s1 + "," + s2); //$NON-NLS-1
				        }
				        for (int i = 0; i < list1.size(); i++) {
				            System.out.println(list1.get(i));
				            System.out.println(list1.get(i));
				        }
				    }
				}
				""";

            ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

            enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

            sample= """
				package test1;
				import java.util.List;
				import java.util.ArrayList;
				public class E1 {
				    public void foo() {
				        List<String> list1 = new ArrayList();
				        for (String s1 : list1) {
				            String s2 = s1;
				            System.out.println(s1 + "," + s2); //$NON-NLS-1
				        }
				        for (String element : list1) {
				            System.out.println(element);
				            System.out.println(element);
				        }
				    }
				}
				""";
            String expected1= sample;

    		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
    }

    @Test
	public void testBug550726() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.io.File;
			import java.util.ArrayList;
			import java.util.List;
			public class A {
			    public static void main(String[] args) {
			        List<File> a = new ArrayList<>();
			        for (int i = 0; i < a.size(); i++) {
			            System.out.print(a);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.io.File;
			import java.util.ArrayList;
			import java.util.List;
			public class A {
			    public static void main(String[] args) {
			        List<File> a = new ArrayList<>();
			        for (File element : a) {
			            System.out.print(a);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoopBug154939() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<Integer> list) {
			       for (Iterator<Integer> iter = list.iterator(); iter.hasNext() && false;) {
			            Integer id = iter.next();
			       }\s
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testJava50ForLoop160218() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    void bar(List<Number> x) {
			        if (true) {
			            for (Iterator<Number> i = x.iterator(); i.hasNext();)
			                System.out.println(i.next());
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= """
			package test1;
			import java.util.List;
			public class E1 {
			    void bar(List<Number> x) {
			        if (true)
			            for (Number number : x)
			                System.out.println(number);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop159449() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(Object[] objs) {
			        if (objs != null)
			            for (int i = 0; i < objs.length; i++) {
			                System.out.println(objs[i]);
			            }
			    }
			    public void bar(List<Object> objs) {
			        if (objs != null)
			            for (Iterator<Object> i = objs.iterator(); i.hasNext();) {
			                System.out.println(i.next());
			            }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= """
			package test1;
			import java.util.List;
			public class E1 {
			    public void foo(Object[] objs) {
			        if (objs != null) {
			            for (Object obj : objs) {
			                System.out.println(obj);
			            }
			        }
			    }
			    public void bar(List<Object> objs) {
			        if (objs != null) {
			            for (Object obj : objs) {
			                System.out.println(obj);
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop160283_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    void foo(Object[] x) {
			        for (int i = 0; i < x.length; i++) {
			            System.out.println(x[i]);
			        }
			    }
			    void bar(List<Object> x) {
			        for (Iterator<Object> i = x.iterator(); i.hasNext();) {
			            System.out.println(i.next());
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_NEVER);

		sample= """
			package test1;
			import java.util.List;
			public class E1 {
			    void foo(Object[] x) {
			        for (Object element : x)
			            System.out.println(element);
			    }
			    void bar(List<Object> x) {
			        for (Object object : x)
			            System.out.println(object);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop160283_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    void foo(Object[] x) {
			        for (int i = 0; i < x.length; i++)
			            System.out.println(x[i]);
			    }
			    void bar(List<Object> x) {
			        for (Iterator<Object> i = x.iterator(); i.hasNext();)
			            System.out.println(i.next());
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= """
			package test1;
			import java.util.List;
			public class E1 {
			    void foo(Object[] x) {
			        for (Object element : x) {
			            System.out.println(element);
			        }
			    }
			    void bar(List<Object> x) {
			        for (Object object : x) {
			            System.out.println(object);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop160312() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    void foo(Object[] x, Object[] y) {
			        for (int i = 0; i < y.length; i++)
			            for (int j = 0; j < x.length; j++)
			                System.out.println(x[j]);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);

		sample= """
			package test1;
			public class E1 {
			    void foo(Object[] x, Object[] y) {
			        for (Object element : y) {
			            for (Object element2 : x) {
			                System.out.println(element2);
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop160270() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    void foo(List<Object> y) {
			        for (Iterator<Object> it = y.iterator(); it.hasNext();) {
			            System.out.println(it.next());
			        }
			       \s
			        int j= 0;
			        for (Iterator<Object> it = y.iterator(); it.hasNext(); j++) {
			            System.out.println(it.next());
			        }
			       \s
			        for (Iterator<Object> it = y.iterator(); it.hasNext(); bar()) {
			            System.out.println(it.next());
			        }
			    }
			
			    private void bar() {}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    void foo(List<Object> y) {
			        for (Object object : y) {
			            System.out.println(object);
			        }
			       \s
			        int j= 0;
			        for (Iterator<Object> it = y.iterator(); it.hasNext(); j++) {
			            System.out.println(it.next());
			        }
			       \s
			        for (Iterator<Object> it = y.iterator(); it.hasNext(); bar()) {
			            System.out.println(it.next());
			        }
			    }
			
			    private void bar() {}
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop163122_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    void foo(Object[] x, Object[] y) {
			        for (int i = 0; i < y.length; i++)
			            for (int j = 0; j < x.length; j++)
			                System.out.println(y[i]);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    void foo(Object[] x, Object[] y) {
			        for (Object element : y) {
			            for (Object element2 : x) {
			                System.out.println(element);
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop163122_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    void foo(Object[] x, Object[] y) {
			        for (int i = 0; i < y.length; i++)
			            for (int j = 0; j < x.length; j++)
			                System.out.println(y[i]);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    void foo(Object[] x, Object[] y) {
			        for (Object element : y)
			            for (Object element2 : x)
			                System.out.println(element);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop163122_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    void foo(Object[] x, Object[] y) {
			        for (int i = 0; i < y.length; i++)
			            for (int j = 0; j < x.length; j++)
			                System.out.println(x[i]);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    void foo(Object[] x, Object[] y) {
			        for (int i = 0; i < y.length; i++) {
			            for (Object element : x) {
			                System.out.println(x[i]);
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop163122_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    void foo(Object[] x, Object[] y) {
			        for (int i = 0; i < y.length; i++)
			            for (int j = 0; j < x.length; j++)
			                System.out.println(x[i]);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    void foo(Object[] x, Object[] y) {
			        for (int i = 0; i < y.length; i++)
			            for (Object element : x)
			                System.out.println(x[i]);
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop163122_5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    void foo(Object[] x, Object[] y) {
			        for (int i = 0; i < y.length; i++)
			            for (int j = 0; j < x.length; j++)
			                System.out.println(x[j]);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_USE_BLOCKS_ALWAYS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    void foo(Object[] x, Object[] y) {
			        for (Object element : y) {
			            for (Object element2 : x) {
			                System.out.println(element2);
			            }
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop110599() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void a(int[] i, List<String> l) {
			        //Comment
			        for (int j = 0; j < i.length; j++) {
			            System.out.println(i[j]);
			        }
			        //Comment
			        for (Iterator<String> iterator = l.iterator(); iterator.hasNext();) {
			            String str = iterator.next();
			            System.out.println(str);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.List;
			public class E1 {
			    public void a(int[] i, List<String> l) {
			        //Comment
			        for (int element : i) {
			            System.out.println(element);
			        }
			        //Comment
			        for (String str : l) {
			            System.out.println(str);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop269595() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void a(int[] array) {
			        for (int i = 0; i < array.length; i++) {
			            final int value = array[i];
			            System.out.println(value);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
		enable(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS);

		sample= """
			package test1;
			public class E1 {
			    public void a(final int[] array) {
			        for (final int value : array) {
			            System.out.println(value);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop264421() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public void foo(String[] src) {
			        for (int i = 0; i < src.length; i++) {
			            String path = src[i];
			            String output = path;
			            if (output.length() == 1) {
			                output = output + "-XXX";
			            }
			            System.err.println("path="+ path + ",output="+output);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    public void foo(String[] src) {
			        for (String path : src) {
			            String output = path;
			            if (output.length() == 1) {
			                output = output + "-XXX";
			            }
			            System.err.println("path="+ path + ",output="+output);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop274199() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public static void main(String[] args) {
			        for (int i = 0; i < args.length; i++) {
			            String output = args[i];
			            if (output.length() == 1) {
			                output = output + "-XXX";
			            }
			
			            String s = "path=" + args[i] + ",output=" + output;
			        }
			       \s
			        for (int i = 0; i < args.length; i++) {
			            String output = args[i];
			            String output1 = output;
			            if (output1.length() == 1) {
			                output1 = output1 + "-XXX";
			            }
			
			            String s = "path=" + args[i] + ",output=" + output1;
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    public static void main(String[] args) {
			        for (String arg : args) {
			            String output = arg;
			            if (output.length() == 1) {
			                output = output + "-XXX";
			            }
			
			            String s = "path=" + arg + ",output=" + output;
			        }
			       \s
			        for (String output : args) {
			            String output1 = output;
			            if (output1.length() == 1) {
			                output1 = output1 + "-XXX";
			            }
			
			            String s = "path=" + output + ",output=" + output1;
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop349782() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=349782
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public int[] array;
			    public void foo() {
			        for (int i = 0; i < this.array.length; ++i) {
			            System.out.println(this.array[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    public int[] array;
			    public void foo() {
			        for (int element : this.array) {
			            System.out.println(element);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop344674() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 {
			    public int[] array;
			    public void foo(Object obj) {
			        for (int i = 0; i < ((E1) obj).array.length; i++) {
			            System.out.println(((E1) obj).array[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			public class E1 {
			    public int[] array;
			    public void foo(Object obj) {
			        for (int element : ((E1) obj).array) {
			            System.out.println(element);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testJava50ForLoop374264() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=374264
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.Iterator;
			import java.util.List;
			public class E1 {
			    public void foo(List<String> list) {
			        for (Iterator<String> iterator = list.iterator(); iterator.hasNext();) {
			            removeSecond(iterator);
			        }
			        System.out.println(list);
			    }
			    private static void removeSecond(Iterator<String> iterator) {
			        if ("second".equals(iterator.next())) {
			            iterator.remove();
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testAutoboxing() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			public class E {
			    public static void bar() {
			        // Keep this comment
			        Character c = Character.valueOf('*');
			        Byte by = Byte.valueOf((byte) 0);
			        Boolean bo = Boolean.valueOf(true);
			        Integer i = Integer.valueOf(42);
			        Long l1 = Long.valueOf(42L);
			        Long l2 = Long.valueOf(42);
			        Short s = Short.valueOf((short) 42);
			        Float f = Float.valueOf(42.42F);
			        Double d = Double.valueOf(42.42);
			    }
			
			    public static void removeUnnecessaryValueOfCallsInPrimitiveDeclaration() {
			        char c = Character.valueOf('*');
			        byte by = Byte.valueOf((byte) 0);
			        boolean bo = Boolean.valueOf(true);
			        int i = Integer.valueOf(42);
			        long l1 = Long.valueOf(42L);
			        long l2 = Long.valueOf(42);
			        short s = Short.valueOf((short) 42);
			        float f = Float.valueOf(42.42F);
			        double d = Double.valueOf(42.42);
			    }
			
			    public static void directlyReturnWrapperParameter(Character c, Byte by, Boolean bo, Integer i, Long l, Short s,
			            Float f, Double d) {
			        Object myObject = null;
			
			        // Keep this comment
			        myObject = Character.valueOf(c);
			        myObject = Byte.valueOf(by);
			        myObject = Boolean.valueOf(bo);
			        myObject = Integer.valueOf(i);
			        myObject = Long.valueOf(l);
			        myObject = Short.valueOf(s);
			        myObject = Float.valueOf(f);
			        myObject = Double.valueOf(d);
			    }
			
			    public static void useAutoboxingOnAssignment() {
			        // Keep this comment
			        Character c;
			        c = Character.valueOf('*');
			        Byte by;
			        by = Byte.valueOf((byte) 0);
			        Boolean bo1;
			        bo1 = Boolean.valueOf(true);
			        Integer i;
			        i = Integer.valueOf(42);
			        Long l1;
			        l1 = Long.valueOf(42L);
			        Long l2;
			        l2 = Long.valueOf(42);
			        Short s;
			        s = Short.valueOf((short) 42);
			        Float f;
			        f = Float.valueOf(42.42F);
			        Double d;
			        d = Double.valueOf(42.42);
			    }
			
			    public static void removeUnnecessaryValueOfCallsInPrimitiveAssignment() {
			        // Keep this comment
			        char c;
			        c = Character.valueOf('*');
			        byte by;
			        by = Byte.valueOf((byte) 0);
			        boolean bo1;
			        bo1 = Boolean.valueOf(true);
			        int i;
			        i = Integer.valueOf(42);
			        long l1;
			        l1 = Long.valueOf(42L);
			        long l2;
			        l2 = Long.valueOf(42);
			        short s;
			        s = Short.valueOf((short) 42);
			        float f;
			        f = Float.valueOf(42.42F);
			        double d;
			        d = Double.valueOf(42.42);
			    }
			
			    public static Character removeUnnecessaryValueOfCallsInCharacterWrapper() {
			        // Keep this comment
			        return Character.valueOf('*');
			    }
			
			    public static Byte removeUnnecessaryValueOfCallsInByteWrapper() {
			        // Keep this comment
			        return Byte.valueOf((byte) 0);
			    }
			
			    public static Boolean removeUnnecessaryValueOfCallsInBooleanWrapper() {
			        // Keep this comment
			        return Boolean.valueOf(true);
			    }
			
			    public static Integer removeUnnecessaryValueOfCallsInIntegerWrapper() {
			        // Keep this comment
			        return Integer.valueOf(42);
			    }
			
			    public static Long removeUnnecessaryValueOfCallsInLongWrapper() {
			        // Keep this comment
			        return Long.valueOf(42L);
			    }
			
			    public static Short removeUnnecessaryValueOfCallsInShortWrapper() {
			        // Keep this comment
			        return Short.valueOf((short) 42);
			    }
			
			    public static Float removeUnnecessaryValueOfCallsInFloatWrapper() {
			        // Keep this comment
			        return Float.valueOf(42.42F);
			    }
			
			    public static Double removeUnnecessaryValueOfCallsInDoubleWrapper() {
			        // Keep this comment
			        return Double.valueOf(42.42);
			    }
			
			    public static char removeUnnecessaryValueOfCallsInCharacterPrimitive() {
			        // Keep this comment
			        return Character.valueOf('*');
			    }
			
			    public static byte removeUnnecessaryValueOfCallsInBytePrimitive() {
			        // Keep this comment
			        return Byte.valueOf((byte) 0);
			    }
			
			    public static boolean removeUnnecessaryValueOfCallsInBooleanPrimitive() {
			        // Keep this comment
			        return Boolean.valueOf(true);
			    }
			
			    public static int removeUnnecessaryValueOfCallsInIntegerPrimitive() {
			        // Keep this comment
			        return Integer.valueOf(42);
			    }
			
			    public static long removeUnnecessaryValueOfCallsInLongPrimitive() {
			        // Keep this comment
			        return Long.valueOf(42L);
			    }
			
			    public static short removeUnnecessaryValueOfCallsInShortPrimitive() {
			        // Keep this comment
			        return Short.valueOf((short) 42);
			    }
			
			    public static float removeUnnecessaryValueOfCallsInFloatPrimitive() {
			        // Keep this comment
			        return Float.valueOf(42.42F);
			    }
			
			    public static double removeUnnecessaryValueOfCallsInDoublePrimitive() {
			        // Keep this comment
			        return Double.valueOf(42.42);
			    }
			
			    public static Object doNotUseAutoboxingReturningObject() {
			        return Character.valueOf('a');
			    }
			}
			""";

		String expected= """
			package test1;
			
			public class E {
			    public static void bar() {
			        // Keep this comment
			        Character c = '*';
			        Byte by = (byte) 0;
			        Boolean bo = true;
			        Integer i = 42;
			        Long l1 = 42L;
			        Long l2 = (long) 42;
			        Short s = (short) 42;
			        Float f = 42.42F;
			        Double d = 42.42;
			    }
			
			    public static void removeUnnecessaryValueOfCallsInPrimitiveDeclaration() {
			        char c = '*';
			        byte by = (byte) 0;
			        boolean bo = true;
			        int i = 42;
			        long l1 = 42L;
			        long l2 = 42;
			        short s = (short) 42;
			        float f = 42.42F;
			        double d = 42.42;
			    }
			
			    public static void directlyReturnWrapperParameter(Character c, Byte by, Boolean bo, Integer i, Long l, Short s,
			            Float f, Double d) {
			        Object myObject = null;
			
			        // Keep this comment
			        myObject = c;
			        myObject = by;
			        myObject = bo;
			        myObject = i;
			        myObject = l;
			        myObject = s;
			        myObject = f;
			        myObject = d;
			    }
			
			    public static void useAutoboxingOnAssignment() {
			        // Keep this comment
			        Character c;
			        c = '*';
			        Byte by;
			        by = (byte) 0;
			        Boolean bo1;
			        bo1 = true;
			        Integer i;
			        i = 42;
			        Long l1;
			        l1 = 42L;
			        Long l2;
			        l2 = (long) 42;
			        Short s;
			        s = (short) 42;
			        Float f;
			        f = 42.42F;
			        Double d;
			        d = 42.42;
			    }
			
			    public static void removeUnnecessaryValueOfCallsInPrimitiveAssignment() {
			        // Keep this comment
			        char c;
			        c = '*';
			        byte by;
			        by = (byte) 0;
			        boolean bo1;
			        bo1 = true;
			        int i;
			        i = 42;
			        long l1;
			        l1 = 42L;
			        long l2;
			        l2 = 42;
			        short s;
			        s = (short) 42;
			        float f;
			        f = 42.42F;
			        double d;
			        d = 42.42;
			    }
			
			    public static Character removeUnnecessaryValueOfCallsInCharacterWrapper() {
			        // Keep this comment
			        return '*';
			    }
			
			    public static Byte removeUnnecessaryValueOfCallsInByteWrapper() {
			        // Keep this comment
			        return (byte) 0;
			    }
			
			    public static Boolean removeUnnecessaryValueOfCallsInBooleanWrapper() {
			        // Keep this comment
			        return true;
			    }
			
			    public static Integer removeUnnecessaryValueOfCallsInIntegerWrapper() {
			        // Keep this comment
			        return 42;
			    }
			
			    public static Long removeUnnecessaryValueOfCallsInLongWrapper() {
			        // Keep this comment
			        return 42L;
			    }
			
			    public static Short removeUnnecessaryValueOfCallsInShortWrapper() {
			        // Keep this comment
			        return (short) 42;
			    }
			
			    public static Float removeUnnecessaryValueOfCallsInFloatWrapper() {
			        // Keep this comment
			        return 42.42F;
			    }
			
			    public static Double removeUnnecessaryValueOfCallsInDoubleWrapper() {
			        // Keep this comment
			        return 42.42;
			    }
			
			    public static char removeUnnecessaryValueOfCallsInCharacterPrimitive() {
			        // Keep this comment
			        return '*';
			    }
			
			    public static byte removeUnnecessaryValueOfCallsInBytePrimitive() {
			        // Keep this comment
			        return (byte) 0;
			    }
			
			    public static boolean removeUnnecessaryValueOfCallsInBooleanPrimitive() {
			        // Keep this comment
			        return true;
			    }
			
			    public static int removeUnnecessaryValueOfCallsInIntegerPrimitive() {
			        // Keep this comment
			        return 42;
			    }
			
			    public static long removeUnnecessaryValueOfCallsInLongPrimitive() {
			        // Keep this comment
			        return 42L;
			    }
			
			    public static short removeUnnecessaryValueOfCallsInShortPrimitive() {
			        // Keep this comment
			        return (short) 42;
			    }
			
			    public static float removeUnnecessaryValueOfCallsInFloatPrimitive() {
			        // Keep this comment
			        return 42.42F;
			    }
			
			    public static double removeUnnecessaryValueOfCallsInDoublePrimitive() {
			        // Keep this comment
			        return 42.42;
			    }
			
			    public static Object doNotUseAutoboxingReturningObject() {
			        return Character.valueOf('a');
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.USE_AUTOBOXING);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.AutoboxingCleanup_description)));
	}

	@Test
	public void testDoNotUseAutoboxing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.List;
			
			public class E1 {
			    public static int dummyMethod(Byte byObject) {
			        return 1;
			    }
			
			    public static int dummyMethod(byte byPrimitive) {
			        return 2;
			    }
			
			    public static void doNotCleanupOnConflictingMethod(byte byPrimitive) {
			        dummyMethod(Byte.valueOf(byPrimitive));
			    }
			
			    public static void doNotCleanupOnOverloadedMethod(List<Integer> integers, int notAnIndex) {
			        integers.remove(Integer.valueOf(notAnIndex));
			    }
			
			    public static void doNotUseAutoboxingOnString() {
			        Integer i = Integer.valueOf("1");
			        Long l = Long.valueOf("1");
			        Short s = Short.valueOf("1");
			        Float f = Float.valueOf("1");
			        Double d = Double.valueOf("1");
			    }
			
			    public static void doNotUseAutoboxingWithObjectDeclaration() {
			        Object c = Character.valueOf('*');
			        Object by = Byte.valueOf((byte) 0);
			        Object bo = Boolean.valueOf(true);
			        Object i = Integer.valueOf(42);
			        Object l1 = Long.valueOf(42L);
			        Object l2 = Long.valueOf(42);
			        Object s = Short.valueOf((short) 42);
			        Object f = Float.valueOf(42.42F);
			        Object d = Double.valueOf(42.42);
			    }
			
			    public static void doNotUseAutoboxingWithObjectAssignment() {
			        Object c;
			        c = Character.valueOf('*');
			        Object by;
			        by = Byte.valueOf((byte) 0);
			        Object bo1;
			        bo1 = Boolean.valueOf(true);
			        Object i;
			        i = Integer.valueOf(42);
			        Object l1;
			        l1 = Long.valueOf(42L);
			        Object l2;
			        l2 = Long.valueOf(42);
			        Object s;
			        s = Short.valueOf((short) 42);
			        Object f;
			        f = Float.valueOf(42.42F);
			        Object d;
			        d = Double.valueOf(42.42);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_AUTOBOXING);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUseUnboxing() throws Exception {
		// Given
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			public class E {
			    public static void useUnboxingOnPrimitiveDeclaration(Character cObject, Byte byObject, Boolean boObject,
			            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {
			        // Keep this comment
			        char c = cObject.charValue();
			        byte by = byObject.byteValue();
			        boolean bo = boObject.booleanValue();
			        int i = iObject.intValue();
			        short s = sObject.shortValue();
			        long l = lObject.longValue();
			        float f = fObject.floatValue();
			        double d = dObject.doubleValue();
			    }
			
			    public static void reuseWrapper(Character cObject, Byte byObject, Boolean boObject,
			            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {
			        // Keep this comment
			        Character c = cObject.charValue();
			        Byte by = byObject.byteValue();
			        Boolean bo = boObject.booleanValue();
			        Integer i = iObject.intValue();
			        Short s = sObject.shortValue();
			        Long l = lObject.longValue();
			        Float f = fObject.floatValue();
			        Double d = dObject.doubleValue();
			    }
			
			    public static void useUnboxingOnPrimitiveAssignment(Character cObject, Byte byObject, Boolean boObject,
			            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {
			        // Keep this comment
			        char c;
			        c = cObject.charValue();
			        byte by;
			        by = byObject.byteValue();
			        boolean bo;
			        bo = boObject.booleanValue();
			        int i;
			        i = iObject.intValue();
			        short s;
			        s = sObject.shortValue();
			        long l;
			        l = lObject.longValue();
			        float f;
			        f = fObject.floatValue();
			        double d;
			        d = dObject.doubleValue();
			    }
			
			    public static char useUnboxingOnPrimitiveReturn(Character cObject) {
			        // Keep this comment
			        return cObject.charValue();
			    }
			
			    public static byte useUnboxingOnPrimitiveReturn(Byte byObject) {
			        // Keep this comment
			        return byObject.byteValue();
			    }
			
			    public static boolean useUnboxingOnPrimitiveReturn(Boolean boObject) {
			        // Keep this comment
			        return boObject.booleanValue();
			    }
			
			    public static int useUnboxingOnPrimitiveReturn(Integer iObject) {
			        // Keep this comment
			        return iObject.intValue();
			    }
			
			    public static short useUnboxingOnPrimitiveReturn(Short sObject) {
			        // Keep this comment
			        return sObject.shortValue();
			    }
			
			    public static long useUnboxingOnPrimitiveReturn(Long lObject) {
			        // Keep this comment
			        return lObject.longValue();
			    }
			
			    public static float useUnboxingOnPrimitiveReturn(Float fObject) {
			        // Keep this comment
			        return fObject.floatValue();
			    }
			
			    public static double useUnboxingOnPrimitiveReturn(Double dObject) {
			        // Keep this comment
			        return dObject.doubleValue();
			    }
			
			    public static String useUnboxingOnArrayAccess(String[] strings, Integer i) {
			        // Keep this comment
			        return strings[i.intValue()];
			    }
			}
			""";

		String expected= """
			package test1;
			
			public class E {
			    public static void useUnboxingOnPrimitiveDeclaration(Character cObject, Byte byObject, Boolean boObject,
			            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {
			        // Keep this comment
			        char c = cObject;
			        byte by = byObject;
			        boolean bo = boObject;
			        int i = iObject;
			        short s = sObject;
			        long l = lObject;
			        float f = fObject;
			        double d = dObject;
			    }
			
			    public static void reuseWrapper(Character cObject, Byte byObject, Boolean boObject,
			            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {
			        // Keep this comment
			        Character c = cObject;
			        Byte by = byObject;
			        Boolean bo = boObject;
			        Integer i = iObject;
			        Short s = sObject;
			        Long l = lObject;
			        Float f = fObject;
			        Double d = dObject;
			    }
			
			    public static void useUnboxingOnPrimitiveAssignment(Character cObject, Byte byObject, Boolean boObject,
			            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {
			        // Keep this comment
			        char c;
			        c = cObject;
			        byte by;
			        by = byObject;
			        boolean bo;
			        bo = boObject;
			        int i;
			        i = iObject;
			        short s;
			        s = sObject;
			        long l;
			        l = lObject;
			        float f;
			        f = fObject;
			        double d;
			        d = dObject;
			    }
			
			    public static char useUnboxingOnPrimitiveReturn(Character cObject) {
			        // Keep this comment
			        return cObject;
			    }
			
			    public static byte useUnboxingOnPrimitiveReturn(Byte byObject) {
			        // Keep this comment
			        return byObject;
			    }
			
			    public static boolean useUnboxingOnPrimitiveReturn(Boolean boObject) {
			        // Keep this comment
			        return boObject;
			    }
			
			    public static int useUnboxingOnPrimitiveReturn(Integer iObject) {
			        // Keep this comment
			        return iObject;
			    }
			
			    public static short useUnboxingOnPrimitiveReturn(Short sObject) {
			        // Keep this comment
			        return sObject;
			    }
			
			    public static long useUnboxingOnPrimitiveReturn(Long lObject) {
			        // Keep this comment
			        return lObject;
			    }
			
			    public static float useUnboxingOnPrimitiveReturn(Float fObject) {
			        // Keep this comment
			        return fObject;
			    }
			
			    public static double useUnboxingOnPrimitiveReturn(Double dObject) {
			        // Keep this comment
			        return dObject;
			    }
			
			    public static String useUnboxingOnArrayAccess(String[] strings, Integer i) {
			        // Keep this comment
			        return strings[i];
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);
		enable(CleanUpConstants.USE_UNBOXING);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.UnboxingCleanup_description)));
	}

	@Test
	public void testDoNotUseUnboxing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E1 {
			    public static int dummyMethod(Byte byObject) {
			        return 1;
			    }
			
			    public static int dummyMethod(byte byPrimitive) {
			        return 2;
			    }
			
			    public static void doNotCleanupOnConflictingMethod(Byte byObject) {
			        dummyMethod(byObject.byteValue());
			    }
			
			    public static void doNotCleanupOnOverloadedMethod(StringBuilder builder, Character optimizedObject) {
			        builder.append(optimizedObject.charValue());
			    }
			
			    public static void doNotUseUnboxingOnNarrowingType(Character cObject, Byte byObject,
			            Integer iObject, Short sObject, Float fObject) {
			        int c = cObject.charValue();
			        int by = byObject.byteValue();
			        long i = iObject.intValue();
			        int s = sObject.shortValue();
			        double f = fObject.floatValue();
			    }
			
			    public static void doNotUseUnboxingOnCastCalls(Character cObject, Byte byObject,
			            Integer iObject, Short sObject, Float fObject, Object unknown) {
			        int c = (int)cObject.charValue();
			        int by = (int)byObject.byteValue();
			        long i = (long)iObject.intValue();
			        int s = (int)sObject.shortValue();
			        double f = (double)fObject.floatValue();
			        byte b = (byte)((Integer)unknown).intValue();
			    }
			
			    public static void doNotUseUnboxingWhenTypesDontMatch(Byte byObject,
			            Integer iObject, Short sObject, Long lObject, Float fObject, Double dObject) {
			        short by = byObject.shortValue();
			        short i = iObject.shortValue();
			        byte s = sObject.byteValue();
			        short l = lObject.shortValue();
			        short f = fObject.shortValue();
			        short d = dObject.shortValue();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);

		enable(CleanUpConstants.USE_UNBOXING);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testBooleanLiteral() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;
			
			public class E {
			    public static boolean replaceUselessUnboxing() {
			        // Keep this comment
			        boolean bo1 = Boolean.TRUE;
			        boolean bo2 = Boolean.FALSE;
			        bo1 = Boolean.TRUE;
			        if (Boolean.TRUE) {
			            bo2 = Boolean.FALSE;
			        }
			        return bo1 && bo2;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", input, false, null);

		enable(CleanUpConstants.PREFER_BOOLEAN_LITERAL);

		String output= """
			package test1;
			
			public class E {
			    public static boolean replaceUselessUnboxing() {
			        // Keep this comment
			        boolean bo1 = true;
			        boolean bo2 = false;
			        bo1 = true;
			        if (true) {
			            bo2 = false;
			        }
			        return bo1 && bo2;
			    }
			}
			""";
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { output },
				new HashSet<>(Arrays.asList(MultiFixMessages.BooleanLiteralCleanup_description)));
	}

	@Test
	public void testDoNotUseBooleanLiteral() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public static boolean doNotCreateUselessAutoboxing() {
			        Boolean bo = Boolean.TRUE;
			        bo = Boolean.FALSE;
			        return bo;
			    }
			}
			""";
		ICompilationUnit cu1= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.PREFER_BOOLEAN_LITERAL);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUnnecessaryArrayBug550129() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String sample= """
			package test;
			
			public class X {
			  public int foo(String x, String ...y) { return y.length + 1; }
			  public int bar() {
			      return foo
			          (/* first */ "a", new String[] {"b", "c", "d"});
			  };
			  public int bar2() {
			      return foo("a", "b", new String[] {"c", "d"});
			  };
			  public int foo2(String[] ...x) { return x.length; }
			  public int bar3() {
			      return foo2(new String[][] { new String[] {"a", "b"}});
			  };
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("X.java", sample, false, null);

		sample= """
			package test;
			
			public class X {
			  public int foo(String x, String ...y) { return y.length + 1; }
			  public int bar() {
			      return foo
			          (/* first */ "a", "b", "c", "d");
			  };
			  public int bar2() {
			      return foo("a", "b", new String[] {"c", "d"});
			  };
			  public int foo2(String[] ...x) { return x.length; }
			  public int bar3() {
			      return foo2(new String[][] { new String[] {"a", "b"}});
			  };
			}
			""";
		String expected1= sample;

		sample= """
			package test;
			
			import java.util.Arrays;
			
			public final class X2 {
			  public static class Y {
			      public int foo(String x, String ...y) { return y.length + 1; }
			  }
			  public static class Z extends Y {
			      public int foo2() {
			          List<String> list= Arrays.asList(new String[] {"one"/* 1 */
			              + "one", "two"/* 2 */
			              + "two", "three"/* 3 */
			              + "three"});
			          return super.foo("x", new String[] {"y", "z"});
			      }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("X2.java", sample, false, null);

		sample= """
			package test;
			
			import java.util.Arrays;
			
			public final class X2 {
			  public static class Y {
			      public int foo(String x, String ...y) { return y.length + 1; }
			  }
			  public static class Z extends Y {
			      public int foo2() {
			          List<String> list= Arrays.asList("one"/* 1 */
			              + "one", "two"/* 2 */
			          + "two", "three"/* 3 */
			          + "three");
			          return super.foo("x", "y", "z");
			      }
			}
			""";
		String expected2= sample;

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2 }, new String[] { expected1, expected2 }, null);
	}

	@Test
	public void testUnnecessaryArrayBug564983_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    public void foo(Object... elementsOrTreePaths) {
			        return;
			    }
			    public void foo(Object elementsOrTreePaths) {
			        foo(new Object[] {elementsOrTreePaths});
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testUnnecessaryArrayBug564983_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    public class B {
			        public void foo(Object elementsOrTreePaths, Integer obj, Integer obj2) {
			            return;
			        }
			    }
			    public class C extends B {
			        public void foo(Object... elementsOrTreePaths) {
			            return;
			        }
			        public void foo(Object elementsOrTreePaths, Integer obj) {
			            foo(new Object[] {elementsOrTreePaths, obj});
			            foo(new Object[] {elementsOrTreePaths, elementsOrTreePaths});
			            foo(new Object[] {elementsOrTreePaths, obj, obj});
			            foo(new Object[] {elementsOrTreePaths, obj, elementsOrTreePaths});
			        }
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		sample= """
			package test1;
			
			public class A {
			    public class B {
			        public void foo(Object elementsOrTreePaths, Integer obj, Integer obj2) {
			            return;
			        }
			    }
			    public class C extends B {
			        public void foo(Object... elementsOrTreePaths) {
			            return;
			        }
			        public void foo(Object elementsOrTreePaths, Integer obj) {
			            foo(new Object[] {elementsOrTreePaths, obj});
			            foo(elementsOrTreePaths, elementsOrTreePaths);
			            foo(new Object[] {elementsOrTreePaths, obj, obj});
			            foo(elementsOrTreePaths, obj, elementsOrTreePaths);
			        }
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.UnusedCodeFix_RemoveUnnecessaryArrayCreation_description)));
	}

	@Test
	public void testUnnecessaryArray_default_package() throws Exception {
		try {
			// Given
			IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
			String given= """
				public class A {
				    public class B {
				        public void foo(Object elementsOrTreePaths, Integer obj, Integer obj2) {
				            return;
				        }
				    }
				
				    public class C extends B {
				        public void foo(Object... elementsOrTreePaths) {
				            return;
				        }
				
				        public void foo(Object elementsOrTreePaths, Integer obj) {
				            foo(new Object[] {elementsOrTreePaths, obj});
				            foo(new Object[] {elementsOrTreePaths, elementsOrTreePaths});
				            foo(new Object[] {elementsOrTreePaths, obj, obj});
				            foo(new Object[] {elementsOrTreePaths, obj, elementsOrTreePaths});
				        }
				    }
				}
				""";

			String expected= """
				public class A {
				    public class B {
				        public void foo(Object elementsOrTreePaths, Integer obj, Integer obj2) {
				            return;
				        }
				    }
				
				    public class C extends B {
				        public void foo(Object... elementsOrTreePaths) {
				            return;
				        }
				
				        public void foo(Object elementsOrTreePaths, Integer obj) {
				            foo(new Object[] {elementsOrTreePaths, obj});
				            foo(elementsOrTreePaths, elementsOrTreePaths);
				            foo(new Object[] {elementsOrTreePaths, obj, obj});
				            foo(elementsOrTreePaths, obj, elementsOrTreePaths);
				        }
				    }
				}
				""";

			// When
			ICompilationUnit cu= pack1.createCompilationUnit("A.java", given, false, null);
			enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

			// Then
			assertNotEquals("The class must be changed", given, expected);
			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
					new HashSet<>(Arrays.asList(FixMessages.UnusedCodeFix_RemoveUnnecessaryArrayCreation_description)));
		} catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            fail(sw.toString());
		}
	}

	@Test
	public void testUnnecessaryArrayNLS() throws Exception { // https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1213
		try {
			// Given
			IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
			String given= """
				import java.util.Arrays;
				import java.util.List;
				
				public class NLS {
				    private static final List<String> WHITELISTED_IDS = Arrays
				            .asList(new String[] { "org.eclipse.search.text.FileSearchResultPage", //$NON-NLS-1$
				                    "org.eclipse.jdt.ui.JavaSearchResultPage", //$NON-NLS-1$
				                    "org.eclipse.jdt.ui.CodeGeneration",\s
				                    "org.eclipse.jdt.ui.ISharedImages", //$NON-NLS-1$
				                    "org.eclipse.jdt.ui.IWorkingCopyManager" }); //$NON-NLS-1$
				}
				"""; //
			String expected= """
				import java.util.Arrays;
				import java.util.List;
				
				public class NLS {
				    private static final List<String> WHITELISTED_IDS = Arrays
				                .asList("org.eclipse.search.text.FileSearchResultPage", //$NON-NLS-1$
				                        "org.eclipse.jdt.ui.JavaSearchResultPage", //$NON-NLS-1$
				                        "org.eclipse.jdt.ui.CodeGeneration",\s
				                        "org.eclipse.jdt.ui.ISharedImages", //$NON-NLS-1$
				                        "org.eclipse.jdt.ui.IWorkingCopyManager"); //$NON-NLS-1$
				}
				"""; //

			// When
			ICompilationUnit cu= pack1.createCompilationUnit("A.java", given, false, null);
			enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

			// Then
			assertNotEquals("The class must be changed", given, expected);
			assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
					new HashSet<>(Arrays.asList(FixMessages.UnusedCodeFix_RemoveUnnecessaryArrayCreation_description)));
		} catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            fail(sw.toString());
		}
	}

	@Test
	public void testUnnecessaryEmptyArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    public static void foo(Object... elementsOrTreePaths) {
			        return;
			    }
			
			    public static void bar() {
			        foo(new Object[] {});
			        foo(new Object[0]);
			        foo(new Object[0 + 0]);
			        foo(new Object[1 - 1]);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		sample= """
			package test1;
			
			public class A {
			    public static void foo(Object... elementsOrTreePaths) {
			        return;
			    }
			
			    public static void bar() {
			        foo();
			        foo();
			        foo();
			        foo();
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 },
				new HashSet<>(Arrays.asList(FixMessages.UnusedCodeFix_RemoveUnnecessaryArrayCreation_description)));
	}

	@Test
	public void testUnnecessaryArrayIncompatibleParameters() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			public class A {
			    public static void foo(int i, String text, String... elementsOrTreePaths) {
			        return;
			    }
			
			    public static void foo(String i, int text) {
			        return;
			    }
			
			    public static void foo(String i, int text, String anotherParameter) {
			        return;
			    }
			
			    public static void bar() {
			        foo(0, "bar", new String[0]);
			        foo(0, "bar", new String[] {"bar"});
			    }
			}
			""";

		String expected= """
			package test1;
			
			public class A {
			    public static void foo(int i, String text, String... elementsOrTreePaths) {
			        return;
			    }
			
			    public static void foo(String i, int text) {
			        return;
			    }
			
			    public static void foo(String i, int text, String anotherParameter) {
			        return;
			    }
			
			    public static void bar() {
			        foo(0, "bar");
			        foo(0, "bar", "bar");
			    }
			}
			""";

		// When
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", given, false, null);
		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		// Then
		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(FixMessages.UnusedCodeFix_RemoveUnnecessaryArrayCreation_description)));
	}

	@Test
	public void testUnnecessaryArrayOnConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    public A(Object... elements) {
			    }
			
			    public static A foo() {
			        return new A(new Object[] {"a", "b"});
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		sample= """
			package test1;
			
			public class A {
			    public A(Object... elements) {
			    }
			
			    public static A foo() {
			        return new A("a", "b");
			    }
			}
			""";
		String expected= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(FixMessages.UnusedCodeFix_RemoveUnnecessaryArrayCreation_description)));
	}

	@Test
	public void testUnnecessaryArrayBug565374() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    public class B {
			        public void foo(Object elementsOrTreePaths, Integer obj, Integer obj2) {
			            return;
			        }
			    }
			    public class C extends B {
			        public void foo(Object... elementsOrTreePaths) {
			            return;
			        }
			        public void foo(Object elementsOrTreePaths, Integer obj) {
			            foo(new Object[] {elementsOrTreePaths, obj});
			            foo(new Object[] {elementsOrTreePaths, elementsOrTreePaths});
			            foo(new Object[] {elementsOrTreePaths, obj, obj});
			            foo(new Object[] {elementsOrTreePaths, obj, elementsOrTreePaths});
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		String expected= """
			package test1;
			
			public class A {
			    public class B {
			        public void foo(Object elementsOrTreePaths, Integer obj, Integer obj2) {
			            return;
			        }
			    }
			    public class C extends B {
			        public void foo(Object... elementsOrTreePaths) {
			            return;
			        }
			        public void foo(Object elementsOrTreePaths, Integer obj) {
			            foo(new Object[] {elementsOrTreePaths, obj});
			            foo(elementsOrTreePaths, elementsOrTreePaths);
			            foo(new Object[] {elementsOrTreePaths, obj, obj});
			            foo(elementsOrTreePaths, obj, elementsOrTreePaths);
			        }
			    }
			}
			""";

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(FixMessages.UnusedCodeFix_RemoveUnnecessaryArrayCreation_description)));
	}

	@Test
	public void testUnnecessaryArrayBug567988() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    public static void main(String[] args) {
			        someMethod(new byte[]{42});
			    }
			    private static void someMethod(byte[]... byteArrays) {}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUnnecessaryArrayNotCompilingParameter() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.util.Date;
			
			public class A {
			    public static void doNotRefactorOnNotCompilingMethod() {
			        bar(undeclaredVariable, new String[] {"b"});
			    }
			
			    public static void bar(Object boss, String... elements) {
			    }
			
			    public static void bar(Integer boss, String parameter) {
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { sample }, null);
	}

	@Test
	public void testUnnecessaryArrayUndeclaredVariable() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    public static void doNotRefactorOnNotCompilingMethod() {
			        bar(undeclaredVariable, new String[] {"c"});
			    }
			
			    public static void bar(Object parameter, String... elements) {
			    }
			
			    public static void bar(Integer parameter, String text) {
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { sample }, null);
	}

	@Test
	public void testUnnecessaryArrayBug572656_ConstructorConflict() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    public static A doNotChangeConstructorDispatch() {
			        return new A(new Object[] {"d"});
			    }
			
			    public A(Object... elements) {
			    }
			
			    public A(Object element) {
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUnnecessaryArrayBug572656_MethodConflict() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    public static void doNotChangeMethodDispatch() {
			        bar(new Object[] {"e"});
			    }
			
			    public static void bar(Object... elements) {
			    }
			
			    public static void bar(Object element) {
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUnnecessaryArrayBug572656_SelfMethodConflict() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    public static void doNotChangeToSelfMethod(Object element) {
			        doNotChangeToSelfMethod(new Object[] {"f"});
			    }
			
			    public static void doNotChangeToSelfMethod(Object... elements) {
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUnnecessaryArrayBug572656_ConflictingStaticMethodImport() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import static java.util.Date.parse;
			
			public class A {
			    public void doNotCallAnotherMethod(String text) {
			        parse(new String[]{text});
			    }
			
			    public void parse(String... texts) {
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUnnecessaryArrayBug572656_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    public class ChildClass extends A {
			        public void overloadedMethod(int number) {
			        }
			
			        public void doNotCallAnotherMethod(int number) {
			            overloadedMethod(new int[]{number});
			        }
			    }
			
			    public void overloadedMethod(int... numbers) {
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUnnecessaryArrayBug572656_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    private void overloadedMethod(int number) {
			    }
			    public class ChildClass {
			
			        public void doNotCallAnotherMethod(int number) {
			            overloadedMethod(new int[]{number});
			        }
			    }
			
			    public void overloadedMethod(int... numbers) {
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUnnecessaryArrayBug572656_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String sample= """
			package test1;
			
			public class A extends C {
			    @SuppressWarnings("unused")
			    private void func(Object a) {
			    }
			
			    protected void func(Object ...objs) {
			    }
			
			    public void foo() {
			        new B().func(new Object[] {this});
			        new B().func(new Object[] {this, this});
			        B b = new B();
			        b.func(new Object[] {this});
			        b.func(new Object[] {this, this});
			        this.func(new Object[] {this});
			        this.func(new Object[] {this, this});
			        A a = new A();
			        a.func(new Object[] {this});
			        a.func(new Object[] {this, this});
			        super.func(new Object[] {this});
			        super.func(new Object[] {this, this, this});
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		String sample2= """
			package test1;
			
			public class B {
			    public void func(Object ...objs) {
			    }
			
			    protected void func(Object a, Object b) {
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("B.java", sample2, false, null);

		String sample3= """
			package test1;
			
			public class C {
			    protected void func(Object ...objs) {
			    }
			
			    protected void func(Object a, Object b, Object c) {
			    }
			}
			""";
		ICompilationUnit cu3= pack1.createCompilationUnit("C.java", sample3, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		sample= """
			package test1;
			
			public class A extends C {
			    @SuppressWarnings("unused")
			    private void func(Object a) {
			    }
			
			    protected void func(Object ...objs) {
			    }
			
			    public void foo() {
			        new B().func(this);
			        new B().func(new Object[] {this, this});
			        B b = new B();
			        b.func(this);
			        b.func(new Object[] {this, this});
			        this.func(new Object[] {this});
			        this.func(this, this);
			        A a = new A();
			        a.func(new Object[] {this});
			        a.func(this, this);
			        super.func(this);
			        super.func(new Object[] {this, this, this});
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2, cu3 }, new String[] { expected1, sample2, sample3 }, null);
	}

	@Test
	public void testUnnecessaryArrayBug572656_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String sample= """
			package test1;
			import static test1.B.func;
			
			public class A {
			    public void foo() {
			        func(new Object[] {this, this});
			        func(new Object[] {this});
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		String sample2= """
			package test1;
			
			public class B {
			    public static void func(Object ...objects) {
			    }
			
			    public static void func(Object a) {
			    }
			}
			""";
		ICompilationUnit cu2= pack1.createCompilationUnit("B.java", sample2, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		sample= """
			package test1;
			import static test1.B.func;
			
			public class A {
			    public void foo() {
			        func(this, this);
			        func(new Object[] {this});
			    }
			}
			""";
		String expected1= sample;
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1, cu2 }, new String[] { expected1, sample2 }, null);
    }

	@Test
	public void testUnnecessaryArrayBug568082() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    public static void main(String[] args) {
			        someMethod(new byte[]{42});
			        someMethod2(new char[]{42});
			        someMethod3(new short[]{42});
			    }
			    private static void someMethod(byte... bytes) {}
			    private static void someMethod2(char... chars) {}
			    private static void someMethod3(short... shorts) {}
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUnnecessaryArrayBug572131_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class A {
			    void doError(String a, Object b) {}
			    private void doError(String a, Object b, Object c) {}
			    public void doError(String a, Object b, Object c, Object d) {}
			    public void doError(String a, Object ...objects) {}
			    public void foo() {
			        doError("a", new Object[] {"b"});
			        doError("a", new Object[] {"b", "c"});
			        doError("a", new Object[] {"b", "c", "d"});
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testUnnecessaryArrayBug572131_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			private class B {
			    void doError(String a, Object b) {}
			    private void doError(String a, Object b, Object c) {}
			    protected void doError(String a, Object b, Object c, Object d) {};
			}
			public class A extends B {
			    public void doError(String a, Object ...objects);
			    public void foo() {
			        doError("a", new Object[] {"b"});
			        doError("a", new Object[] {"b", "c"});
			        doError("a", new Object[] {"b", "c", "d"});
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		sample= """
			package test1;
			
			private class B {
			    void doError(String a, Object b) {}
			    private void doError(String a, Object b, Object c) {}
			    protected void doError(String a, Object b, Object c, Object d) {};
			}
			public class A extends B {
			    public void doError(String a, Object ...objects);
			    public void foo() {
			        doError("a", new Object[] {"b"});
			        doError("a", "b", "c");
			        doError("a", new Object[] {"b", "c", "d"});
			    }
			}
			""";
		String expected= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected },
				new HashSet<>(Arrays.asList(FixMessages.UnusedCodeFix_RemoveUnnecessaryArrayCreation_description)));
	}

	@Test
	public void testKeepArrayWithSingleArrayElement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.lang.reflect.Method;
			
			public class A {
			  public void foo() throws Throwable {
			    Method method= A.class.getMethod("bah", A.class);
			    method.invoke(this, new Object[] {new Object[] {"bar"}});
			  }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testUnnecessaryArrayBug562091() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.lang.reflect.Method;
			
			public class A {
			  public void foo() throws Throwable {
			    Method method= A.class.getMethod("bah", A.class);
			    method.invoke(this, new Object[] {null});
			  }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.REMOVE_UNNECESSARY_ARRAY_CREATION);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}

	@Test
	public void testOrganizeImportsBug573629() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			import java.util.List;
			public class TestOrganizeImports {
			    public void foo(int a, List<String> list) {
			        label_1:
			        switch (a) {
			            case 0:
			                while (true) {
			                    int len = 0;
			                    for (int i = 0; i < list.size(); ++i) {
			                        String s = list.get(i);
			                        len += s.length();
			                    }
			                    if (len < 100) {
			                        break label_1;
			                    }
			                    break;
			                }
			                break;
			            default:
			        }
			    }
			}
			"""; //
		ICompilationUnit cu1= pack1.createCompilationUnit("TestOrganizeImports.java", sample, false, null);

		enable(CleanUpConstants.ORGANIZE_IMPORTS);
		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		sample= """
			package test1;
			import java.util.List;
			public class TestOrganizeImports {
			    public void foo(int a, List<String> list) {
			        label_1:
			        switch (a) {
			            case 0:
			                while (true) {
			                    int len = 0;
			                    for (String s : list) {
			                        len += s.length();
			                    }
			                    if (len < 100) {
			                        break label_1;
			                    }
			                    break;
			                }
			                break;
			            default:
			        }
			    }
			}
			"""; //
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
	}

	@Test
	public void testStringBufferToStringBuilderForLocals() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String sample0= """
			package test1;
			
			public class SuperClass {
			    public StringBuffer field0;
			    public void method0(StringBuffer parm) {
			        System.out.println(parm.toString());
			    }
			}""";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= """
			package test1;
			
			public class TestStringBuilderCleanup extends SuperClass {
			    StringBuffer field1;
			    StringBuffer field2;
			    public void changeForSimpleCase() {
			        StringBuffer x = new StringBuffer();
			        x.append("abc");
			        System.out.println(x.toString());
			    }
			    public void changeWithSafeParmUse(StringBuffer a) {
			        StringBuffer x = new StringBuffer();
			        x.append(a.toString());
			        System.out.println(x.toString());
			    }
			    public void changeWithArrayAndForLoop() {
			        StringBuffer[] j = new StringBuffer[14];
			        for (StringBuffer sb : j) {
			            StringBuffer k = sb.append("abc");
			            System.out.println(k.toString());
			        }
			    }
			    public void changeConstructors() {
			        StringBuffer x = null;
			        x = new StringBuffer();
			        x.append(new StringBuffer("abc"));
			    }
			    public void changeWithConditional(int x) {
			        StringBuffer a = new StringBuffer();
			        StringBuffer b = null;
			        b = x < 0 ? new StringBuffer() : a;
			    }
			    private void someMethod(Object a) {}
			    public void changeWithValidMethodCall() {
			        StringBuffer a = new StringBuffer();
			        someMethod(a.toString());
			    }
			    private void varArgMethod(StringBuffer ...a) {}
			    public void changeWithVarArgMethodWithSafeUse(StringBuffer parm) {
			        StringBuffer a = new StringBuffer();
			        varArgMethod(field1, super.field0, parm);
			    }
			    public void changeWithVarArgMethodWithSafeUse2() {
			        StringBuffer a = new StringBuffer();
			        varArgMethod();
			    }
			    public StringBuffer changeWithSafeFieldAndParmUse(StringBuffer parm) {
			        StringBuffer a = new StringBuffer();
			        StringBuffer b = new StringBuffer("abc");
			        a.append(b);
			        a.append(field1.toString());
			        b.append(parm.toString());
			        field1 = field2;
			        super.field0 = field1;
			        changeWithSafeParmUse(parm);
			        super.method0(parm);
			        field2 = parm.append("def");
			        return field2;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS);

		sample= """
			package test1;
			
			public class TestStringBuilderCleanup extends SuperClass {
			    StringBuffer field1;
			    StringBuffer field2;
			    public void changeForSimpleCase() {
			        StringBuilder x = new StringBuilder();
			        x.append("abc");
			        System.out.println(x.toString());
			    }
			    public void changeWithSafeParmUse(StringBuffer a) {
			        StringBuilder x = new StringBuilder();
			        x.append(a.toString());
			        System.out.println(x.toString());
			    }
			    public void changeWithArrayAndForLoop() {
			        StringBuilder[] j = new StringBuilder[14];
			        for (StringBuilder sb : j) {
			            StringBuilder k = sb.append("abc");
			            System.out.println(k.toString());
			        }
			    }
			    public void changeConstructors() {
			        StringBuilder x = null;
			        x = new StringBuilder();
			        x.append(new StringBuffer("abc"));
			    }
			    public void changeWithConditional(int x) {
			        StringBuilder a = new StringBuilder();
			        StringBuilder b = null;
			        b = x < 0 ? new StringBuilder() : a;
			    }
			    private void someMethod(Object a) {}
			    public void changeWithValidMethodCall() {
			        StringBuilder a = new StringBuilder();
			        someMethod(a.toString());
			    }
			    private void varArgMethod(StringBuffer ...a) {}
			    public void changeWithVarArgMethodWithSafeUse(StringBuffer parm) {
			        StringBuilder a = new StringBuilder();
			        varArgMethod(field1, super.field0, parm);
			    }
			    public void changeWithVarArgMethodWithSafeUse2() {
			        StringBuilder a = new StringBuilder();
			        varArgMethod();
			    }
			    public StringBuffer changeWithSafeFieldAndParmUse(StringBuffer parm) {
			        StringBuilder a = new StringBuilder();
			        StringBuilder b = new StringBuilder("abc");
			        a.append(b);
			        a.append(field1.toString());
			        b.append(parm.toString());
			        field1 = field2;
			        super.field0 = field1;
			        changeWithSafeParmUse(parm);
			        super.method0(parm);
			        field2 = parm.append("def");
			        return field2;
			    }
			}
			""";
		String expected1= sample;

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu0, cu1 }, new String[] { sample0, expected1 }, null);
	}

	@Test
	public void testDoNotConvertStringBufferToStringBuilderForLocals() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample0= """
			package test1;
			
			public class SuperClass {
			    public StringBuffer field0;
			    public void method0(StringBuffer parm) {
			        System.out.println(parm.toString());
			    }
			}""";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= """
			package test1;
			
			public class TestStringBuilderCleanup extends SuperClass {
			    StringBuffer field1;
			    StringBuffer field2;
			    public void doNotChangeForParmAssignment(StringBuffer parm) {
			        StringBuffer x = parm;
			        System.out.println(x.toString());
			    }
			    public void doNotChangeForFieldAssignment(StringBuffer a) {
			        StringBuffer x = field1.append("abc");
			        System.out.println(x.toString());
			    }
			    public void doNotChangeForSuperFieldAssignment(StringBuffer a) {
			        StringBuffer x = super.field0.append("abc");
			        System.out.println(x.toString());
			    }
			    public void doNotChangeWithParmForLoop(StringBuffer[] parm) {
			        for (StringBuffer sb : parm) {
			            StringBuffer k = sb.append("abc");
			            System.out.println(k.toString());
			        }
			    }
			    public void doNotChangeWithParmCast(Object o) {
			        StringBuffer k= (StringBuffer)o;
			    }
			    public StringBuffer doNotChangeWithReturn() {
			        StringBuffer a = new StringBuffer();
			        return a;
			    }\
			    public void doNotChangeWithMethodReturnAssignment() {
			        StringBuffer a = doNotChangeWithReturn();
			    }
			    public void doNotChangeWhenMethodParm() {
			        StringBuffer a = new StringBuffer();
			        doNotChangeForFieldAssignment(a);
			    }
			    public void doNotChangeWhenSuperMethodParm() {
			        StringBuffer a = new StringBuffer();
			        super.method0(a);
			    }
			    private void someMethod(Object a, Object b) {}
			    public void doNotChangeWhenMethodParmPassedAsObject() {
			        StringBuffer a = new StringBuffer();
			        someMethod(a.toString(), a);
			    }
			    private void varArgMethod(StringBuffer ...a) {}
			    public void doNotChangeWhenPassedAsVarArg() {
			        StringBuffer a = new StringBuffer();
			        varArgMethod(a);
			    }
			    public void doNotChangeWhenPassedAsVarArg2() {
			        StringBuffer a = new StringBuffer();
			        varArgMethod(field1, a);
			    }
			    private void varArgObjectMethod(Object ...a) {}
			    public void doNotChangeWhenPassedAsObjectVarArg() {
			        StringBuffer a = new StringBuffer();
			        varArgObjectMethod(a);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu0, cu1 });
	}

	@Test
	public void testDoNotConvertStringBufferToStringBuilder() throws Exception {
		// test bug 574588 NPE on private constructor
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String sample= """
			package test1;
			
			public class TestStringBuilderCleanup {
			    private TestStringBuilderCleanup(){
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu1 });
	}

	@Test
	public void testConvertStringBufferToStringBuilderAll() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample0= """
			package test1;
			
			public class SuperClass {
			    public StringBuffer field0;
			    private SuperClass(StringBuffer a) {
			        this.field0 = a;
			    }
			    public void method0(StringBuffer parm) {
			        System.out.println(parm.toString());
			    }
			}""";
		ICompilationUnit cu0= pack1.createCompilationUnit("SuperClass.java", sample0, false, null);

		String sample= """
			package test1;
			
			import java.util.Map;
			public class TestStringBuilderCleanup extends SuperClass {
			    StringBuffer field1;
			    StringBuffer field2;
			    /**
			     * {@link StringBuffer}
			     */
			    public void changeForParmAssignment(StringBuffer parm) {
			        StringBuffer x = (parm instanceof StringBuffer) ? parm : null;
			        System.out.println(x.toString());
			    }
			    /**
			     * @see StringBuffer#append(StringBuffer)
			     */
			    public void changeForFieldAssignment(StringBuffer a) {
			        StringBuffer x = field1.append("abc");
			        Map<String, StringBuffer> map= null;
			        System.out.println(x.toString());
			    }
			    public void changeForSuperFieldAssignment(StringBuffer a) {
			        StringBuffer x = super.field0.append("abc");
			        System.out.println(x.toString());
			    }
			    public void changeWithParmForLoop(StringBuffer[] parm) {
			        for (StringBuffer sb : parm) {
			            StringBuffer k = sb.append("abc");
			            System.out.println(k.toString());
			        }
			    }
			    public void changeWithParmCast(Object o) {
			        StringBuffer k= (StringBuffer)o;
			    }
			    public StringBuffer changeWithReturn() {
			        StringBuffer a = new StringBuffer();
			        return a;
			    }\
			    public void changeWithMethodReturnAssignment() {
			        StringBuffer a = changeWithReturn();
			    }
			    public void changeWhenMethodParm() {
			        StringBuffer a = new StringBuffer();
			        changeForFieldAssignment(a);
			    }
			    public void changeWhenSuperMethodParm() {
			        StringBuffer a = new StringBuffer();
			        super.method0(a);
			    }
			    private void someMethod(Object a, Object b) {}
			    public void changeWhenMethodParmPassedAsObject() {
			        StringBuffer a = new StringBuffer();
			        someMethod(a.toString(), a);
			    }
			    private void varArgMethod(StringBuffer ...a) {}
			    public void changeWhenPassedAsVarArg() {
			        StringBuffer a = new StringBuffer();
			        varArgMethod(a);
			    }
			    public void changeWhenPassedAsVarArg2() {
			        StringBuffer a = new StringBuffer();
			        varArgMethod(field1, a);
			    }
			    private void varArgObjectMethod(Object ...a) {}
			    public void changeWhenPassedAsObjectVarArg() {
			        StringBuffer a = new StringBuffer();
			        varArgObjectMethod(a);
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("TestStringBuilderCleanup.java", sample, false, null);

		String expected0= """
			package test1;
			
			public class SuperClass {
			    public StringBuilder field0;
			    private SuperClass(StringBuilder a) {
			        this.field0 = a;
			    }
			    public void method0(StringBuilder parm) {
			        System.out.println(parm.toString());
			    }
			}""";

		String expected1= """
			package test1;
			
			import java.util.Map;
			public class TestStringBuilderCleanup extends SuperClass {
			    StringBuilder field1;
			    StringBuilder field2;
			    /**
			     * {@link StringBuilder}
			     */
			    public void changeForParmAssignment(StringBuilder parm) {
			        StringBuilder x = (parm instanceof StringBuilder) ? parm : null;
			        System.out.println(x.toString());
			    }
			    /**
			     * @see StringBuilder#append(StringBuilder)
			     */
			    public void changeForFieldAssignment(StringBuilder a) {
			        StringBuilder x = field1.append("abc");
			        Map<String, StringBuilder> map= null;
			        System.out.println(x.toString());
			    }
			    public void changeForSuperFieldAssignment(StringBuilder a) {
			        StringBuilder x = super.field0.append("abc");
			        System.out.println(x.toString());
			    }
			    public void changeWithParmForLoop(StringBuilder[] parm) {
			        for (StringBuilder sb : parm) {
			            StringBuilder k = sb.append("abc");
			            System.out.println(k.toString());
			        }
			    }
			    public void changeWithParmCast(Object o) {
			        StringBuilder k= (StringBuilder)o;
			    }
			    public StringBuilder changeWithReturn() {
			        StringBuilder a = new StringBuilder();
			        return a;
			    }\
			    public void changeWithMethodReturnAssignment() {
			        StringBuilder a = changeWithReturn();
			    }
			    public void changeWhenMethodParm() {
			        StringBuilder a = new StringBuilder();
			        changeForFieldAssignment(a);
			    }
			    public void changeWhenSuperMethodParm() {
			        StringBuilder a = new StringBuilder();
			        super.method0(a);
			    }
			    private void someMethod(Object a, Object b) {}
			    public void changeWhenMethodParmPassedAsObject() {
			        StringBuilder a = new StringBuilder();
			        someMethod(a.toString(), a);
			    }
			    private void varArgMethod(StringBuilder ...a) {}
			    public void changeWhenPassedAsVarArg() {
			        StringBuilder a = new StringBuilder();
			        varArgMethod(a);
			    }
			    public void changeWhenPassedAsVarArg2() {
			        StringBuilder a = new StringBuilder();
			        varArgMethod(field1, a);
			    }
			    private void varArgObjectMethod(Object ...a) {}
			    public void changeWhenPassedAsObjectVarArg() {
			        StringBuilder a = new StringBuilder();
			        varArgObjectMethod(a);
			    }
			}
			""";

		enable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
		disable(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS);

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu0, cu1 }, new String[] { expected0, expected1 },
				new HashSet<>(Arrays.asList(MultiFixMessages.StringBufferToStringBuilderCleanUp_description)));
	}

	@Test
	public void testOverrideMultiFix() throws Exception {
		Hashtable<String, String> opts= JavaCore.getOptions();
		opts.put(JavaCore.COMPILER_PB_MISSING_OVERRIDE_ANNOTATION, JavaCore.ERROR);
		JavaCore.setOptions(opts);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			public class E1 extends MyAbstract {
			    public void run() {};
			    public int compareTo(String o) { return -1; };
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", sample, false, null);
		sample= """
			package test1;
			public abstract class MyAbstract {
			    public void run();
			    public int compareTo(String o);
			}""";
		pack1.createCompilationUnit("MyAbstract.java", sample, false, null);

		cu1.getJavaProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);

		sample= """
			package test1;
			public class E1 extends MyAbstract {
			    @Override
			    public void run() {};
			    @Override
			    public int compareTo(String o) { return -1; };
			}
			""";
		String expected1= sample;

		// Two error markers due to missing override annotations
		IMarker[] markers= cu1.getResource().findMarkers(null, true, IResource.DEPTH_INFINITE);
		assertEquals(2, markers.length);

		IEditorInput input= EditorUtility.getEditorInput(cu1);
		IProblemLocation location1= CorrectionMarkerResolutionGenerator.findProblemLocation(input, markers[0]);
		IProblemLocation location2= CorrectionMarkerResolutionGenerator.findProblemLocation(input, markers[1]);

		CleanUpOptions cleanUpOptions= new CleanUpOptions();
		cleanUpOptions.setOption(CleanUpConstants.ADD_MISSING_ANNOTATIONS, CleanUpOptions.TRUE);
		cleanUpOptions.setOption(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE, CleanUpOptions.TRUE);
		cleanUpOptions.setOption(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION, CleanUpOptions.TRUE);

		Java50CleanUp cleanUp = new Java50CleanUp();
		cleanUp.setOptions(cleanUpOptions);

		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setResolveBindings(true);
		parser.setProject(getProject());
		parser.setSource(cu1);
		CompilationUnit ast= (CompilationUnit)parser.createAST(null);

		ICleanUpFix cleanUpFix= cleanUp.createFix(new IMultiFix.MultiFixContext(cu1, ast, new IProblemLocation[] {location1, location2}));
		CompilationUnitChange change= cleanUpFix.createChange(new NullProgressMonitor());
		change.perform(new NullProgressMonitor());
		assertEquals(expected1, cu1.getBuffer().getContents());

	}

	@Test
	public void testRemoveThisBug536138() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String sample= """
			package test1;
			
			public class A<K> {
			    private int val;
			    public A(int val) {
			        this.val = val;
			    }
			    public void foo() {
			        this.val = 7;
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("A.java", sample, false, null);

		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS);
		enable(CleanUpConstants.MEMBER_ACCESSES_NON_STATIC_FIELD_USE_THIS_IF_NECESSARY);

		sample= """
			package test1;
			
			public class A<K> {
			    private int val;
			    public A(int val) {
			        this.val = val;
			    }
			    public void foo() {
			        val = 7;
			    }
			}
			""";
		String expected1= sample;
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 }, null);
    }

}
