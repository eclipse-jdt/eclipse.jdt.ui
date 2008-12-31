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
package org.eclipse.jdt.ui.tests.refactoring.typeconstraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ASTCreator;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintCollector;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintOperator;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintVariableFactory;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.FullConstraintCreator;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ITypeConstraint;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.TypeConstraintFactory;

import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTestSetup;

public class TypeConstraintTests extends RefactoringTest {

	private static final Class clazz= TypeConstraintTests.class;
	private static final String PATH= "TypeConstraints/";

	public TypeConstraintTests(String name) {
		super(name);
	}

	protected String getRefactoringPath() {
		return PATH;
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringTestSetup(someTest);
	}

	private String getSimpleTestFileName(){
		return "A_" + getName() + ".java";
	}

	private String getTestFileName(){
		return TEST_PATH_PREFIX + getRefactoringPath() + getSimpleTestFileName();
	}

	private ICompilationUnit createCUfromTestFile(IPackageFragment pack) throws Exception {
		return createCU(pack, getSimpleTestFileName(), getFileContents(getTestFileName()));
	}

	private CompilationUnit getCuNode() throws Exception {
		ICompilationUnit cu= createCUfromTestFile(getPackageP());
		return ASTCreator.createAST(cu, null);
	}

	private void numberHelper(int number) throws Exception {
		CompilationUnit cuNode= getCuNode();
		ConstraintCollector collector= getCollector();
		cuNode.accept(collector);
		ITypeConstraint[] constraints= collector.getConstraints();
		assertEquals(Arrays.asList(constraints).toString(), number, constraints.length);
	}

	public void testNumber0() throws Exception{
		numberHelper(2);
	}

	public void testNumber1() throws Exception{
		numberHelper(3);
	}

	public void testNumber2() throws Exception{
		numberHelper(10);
	}

	private ConstraintCollector getCollector() {
		TypeConstraintFactory factory = new TypeConstraintFactory(){
			public boolean filter(ConstraintVariable v1, ConstraintVariable v2, ConstraintOperator o){
				return false;
			}
		};
		ConstraintCollector collector= new ConstraintCollector(new FullConstraintCreator(new ConstraintVariableFactory(), factory));
		return collector;
	}

	private static List allToStrings(Object[] elements) {
		String[] strings= new String[elements.length];
		for (int i= 0; i < elements.length; i++) {
			strings[i]= elements[i].toString();
		}
		return new ArrayList(Arrays.asList(strings));//want to be able to remove stuff from it
	}

	private void testConstraints(String[] constraintStrings) throws Exception{
		CompilationUnit cuNode= getCuNode();
		ConstraintCollector collector= getCollector();
		cuNode.accept(collector);
		ITypeConstraint[] constraints= collector.getConstraints();

		List externals= allToStrings(constraints);
		assertEquals("length", constraintStrings.length, constraints.length);
		for (int i= 0; i < constraintStrings.length; i++) {
			assertTrue("missing constraint:" + constraintStrings[i], externals.remove(constraintStrings[i]));
		}
	}

	public void testConstraints0() throws Exception{
		String[] strings= {"[null] <= [a0]", "[a0] <= [a1]", "[a0] =^= A", "[a1] =^= A", "Decl(A:f()) =^= p.A"};
		testConstraints(strings);
	}

	public void testConstraints1() throws Exception{
		String[] strings= {"[null] <= [a0]", "[a0] == [a1]", "[a0] =^= A", "[a1] =^= A", "Decl(A:a0) =^= p.A", "Decl(A:a1) =^= p.A"};
		testConstraints(strings);
	}

	public void testConstraints2() throws Exception{
		String[] strings= {"[null] <= [a0]", "[(A)a0] =^= A", "[(A)a0] <= [a1]", "[a0] <= [(A)a0] or [(A)a0] <= [a0]", "[a0] =^= A", "[a1] =^= A", "Decl(A:f()) =^= p.A"};
		testConstraints(strings);
	}

	public void testConstraints3() throws Exception{
		String[] strings= {"[null] <= [a]", "[null] <= [b]", "[a] == [b]", "[a] =^= A", "[b] =^= A", "Decl(A:f()) =^= p.A"};
		testConstraints(strings);
	}

	public void testConstraints4() throws Exception{
		String[] strings= {"[as0] =^= A[]", "[a0] <= A", "[{a0}] <= [as0]", "Decl(A:f()) =^= p.A", "[null] <= [a0]", "[a0] =^= A"};
		testConstraints(strings);
	}

	public void testConstraints5() throws Exception{
		String[] strings= {"[as0] =^= A[]", "Decl(A:f()) =^= p.A", "[a0] <= A", "[a0] =^= A", "[null] <= [a0]", "[new A[]{a0}] <= [as0]"};
		testConstraints(strings);
	}

	public void testConstraints6() throws Exception{
		String[] strings= {"Decl(A:f()) =^= p.A", "Decl(A:A(A)) =^= p.A", "[new A(a0)] =^= p.A", "[a0] <= [Parameter(0,A:A(A))]", "[a1] =^= A", "[a] =^= A", "[a0] =^= A", "[null] <= [a0]", "[new A(a0)] <= [a1]", "[Parameter(0,A:A(A))] =^= [a]"};
		testConstraints(strings);
	}

	public void testConstraints7() throws Exception{
		String[] strings= {"Decl(A:A()) =^= p.A", "Decl(A:A(A)) =^= p.A", "[null] <= [Parameter(0,A:A(A))]", "[Parameter(0,A:A(A))] =^= [a]", "[a] =^= A"};
		testConstraints(strings);
	}

	public void testConstraints8() throws Exception{
		String[] strings= {"Decl(A:x) =^= p.A", "Decl(A:f()) =^= p.A", "[x] =^= java.lang.Object", "Decl(A:aField) =^= p.A", "[this] <= [x]", "[aField] =^= A", "[x] =^= Object", "[this] =^= p.A", "[aField] <= Decl(A:x)"};
		testConstraints(strings);
	}

	public void testConstraints9() throws Exception{
		String[] strings= {"Decl(A:f()) =^= p.A", "[a] =^= A", "[x] =^= boolean", "[a instanceof A] <= [x]", "[null] <= [a]", "[a] <= A or A <= [a]"};
		testConstraints(strings);
	}

	public void testConstraints10() throws Exception{
		String[] strings= {"[null] <= [A:f1()]_returnType", "Decl(A:f1()) =^= p.A", "[A:f1()]_returnType =^= A[]"};
		testConstraints(strings);
	}

	public void testConstraints11() throws Exception{
		String[] strings= {"[null] <= [A:f(A, Object)]_returnType", "[Parameter(1,B:f(A, Object))] =^= [a4]",
							"[B:f(A, Object)]_returnType =^= A", "Decl(B:f(A, Object)) < Decl(A:f(A, Object))",
							"[Parameter(1,A:f(A, Object))] == [Parameter(1,B:f(A, Object))]",
							"[Parameter(0,A:f(A, Object))] == [Parameter(0,B:f(A, Object))]",
							"[A:f(A, Object)]_returnType == [B:f(A, Object)]_returnType",
							"[Parameter(0,B:f(A, Object))] =^= [a3]",
							"[Parameter(0,A:f(A, Object))] =^= [a0]", "Decl(A:f(A, Object)) =^= p.A",
							"[null] <= [B:f(A, Object)]_returnType", "[Parameter(1,A:f(A, Object))] =^= [a1]",
							"[A:f(A, Object)]_returnType =^= A", "Decl(B:f(A, Object)) =^= p.B",
							"[a3] =^= A", "[a0] =^= A", "[a1] =^= Object", "[a4] =^= Object"};
		testConstraints(strings);
	}

	public void testConstraints12() throws Exception{
		String[] strings= { "Decl(B:f(A, Object)) =^= p.B", "[Parameter(1,B:f(A, Object))] =^= [a4]", "[B:f(A, Object)]_returnType =^= A", "[null] <= [B:f(A, Object)]_returnType", "[A:f(A, Object)]_returnType =^= A", "[Parameter(1,A:f(A, Object))] =^= [a1]", "Decl(A:f(A, Object)) =^= p.A", "[Parameter(0,B:f(A, Object))] =^= [a3]", "[null] <= [A:f(A, Object)]_returnType", "[Parameter(0,A:f(A, Object))] =^= [a0]", "[a3] =^= A", "[a0] =^= A", "[a1] =^= Object", "[a4] =^= Object" };
		testConstraints(strings);
	}

	public void testConstraints13() throws Exception{
		String[] strings= {"Decl(B:f(A, Object)) =^= p.B", "[Parameter(1,B:f(A, Object))] =^= [a4]", "[B:f(A, Object)]_returnType =^= A", "[null] <= [B:f(A, Object)]_returnType", "[A:f(A, Object)]_returnType =^= A", "[Parameter(1,A:f(A, Object))] =^= [a1]", "Decl(A:f(A, Object)) =^= p.A", "[Parameter(0,B:f(A, Object))] =^= [a3]", "[null] <= [A:f(A, Object)]_returnType", "[Parameter(0,A:f(A, Object))] =^= [a0]", "[a3] =^= A", "[a0] =^= A", "[a1] =^= Object", "[a4] =^= Object"};
		testConstraints(strings);
	}

	public void testConstraints14() throws Exception{
		String[] strings= {"[A:f(A)]_returnType == [B:f(A)]_returnType", "[Parameter(0,A:f(A))] == [Parameter(0,B:f(A))]", "[B:f(A)]_returnType =^= A", "Decl(B:f(A)) < Decl(A:f(A))", "[A:f(A)]_returnType =^= A", "Decl(B:f(A)) < Decl(I:f(A))", "[null] <= [A:f(A)]_returnType", "[Parameter(0,A:f(A))] =^= [a0]", "[I:f(A)]_returnType =^= A", "Decl(A:f(A)) =^= p.A", "[ax] =^= B", "[Parameter(0,B:f(A))] =^= [a3]", "[null] <= [B:f(A)]_returnType", "[a3] <= [Parameter(0,B:f(A))]", "[ax.f(a3)] =^= [B:f(A)]_returnType", "[ax] <= Decl(I:f(A)) or [ax] <= Decl(A:f(A))", "Decl(B:f(A)) =^= p.B", "[I:f(A)]_returnType == [B:f(A)]_returnType", "[Parameter(0,I:f(A))] == [Parameter(0,B:f(A))]", "[null] <= [ax]", "[Parameter(0,I:f(A))] =^= [ai]", "Decl(I:f(A)) =^= p.I", "[a3] =^= A", "[a0] =^= A", "[ai] =^= A"};
		testConstraints(strings);
	}

	public void testConstraints15() throws Exception{
		String[] strings= {"[Parameter(0,A:f(A))] =^= [a0]", "[I:f(A)]_returnType == [B:f(A)]_returnType", "[Parameter(0,I:f(A))] == [Parameter(0,B:f(A))]", "[null] <= [A:f(A)]_returnType", "[A:f(A)]_returnType == [B:f(A)]_returnType", "[Parameter(0,A:f(A))] == [Parameter(0,B:f(A))]", "Decl(B:f(A)) < Decl(A:f(A))", "[super.f(a3)] =^= [A:f(A)]_returnType", "Decl(A:f(A)) =^= p.A", "[null] <= [B:f(A)]_returnType", "[Parameter(0,I:f(A))] =^= [ai]", "[Parameter(0,B:f(A))] =^= [a3]", "[A:f(A)]_returnType =^= A", "Decl(B:f(A)) =^= p.B", "Decl(I:f(A)) =^= p.I", "Decl(B:f(A)) < Decl(I:f(A))", "[a3] <= [Parameter(0,A:f(A))]", "[I:f(A)]_returnType =^= A", "[B:f(A)]_returnType =^= A", "[a3] =^= A", "[a0] =^= A", "[ai] =^= A"};
		testConstraints(strings);
	}

	public void testConstraints16() throws Exception{
		String[] strings= {"Decl(A:aField) =^= p.A", "Decl(A:f()) =^= p.A", "[this] =^= p.A", "[this] =^= p.A", "[this] <= [aTemp]", "[this] <= [aField]", "[this] <= [a]", "[a] =^= A", "[aField] =^= A", "[aTemp] =^= A", "[this] =^= p.A"};
		testConstraints(strings);
	}

	public void testConstraints17() throws Exception{
		String[] strings= {"Decl(A:f()) =^= p.A", "[null] <= [a]", "[A:f()]_returnType =^= A", "[a] =^= A", "[a] <= [A:f()]_returnType"};
		testConstraints(strings);
	}

	public void testConstraints18() throws Exception{
		String[] strings= {"[Parameter(0,B:B(A))] =^= [a1]", "Decl(B:B(A)) =^= p.B", "[Parameter(0,A:A(A))] =^= [a0]", "Decl(A:A(A)) =^= p.A", "[a1] <= [Parameter(0,A:A(A))]", "[a1] =^= A", "[a0] =^= A"};
		testConstraints(strings);
	}

	public void testConstraints19() throws Exception{
		String[] strings= {"[aField] =^= p.A", "[a] =^= A", "Decl(B:f()) =^= p.B", "[a] <= [aField]", "[null] <= [a]", "Decl(A:aField) =^= p.A", "[aField] =^= A"};
		testConstraints(strings);
	}

	public void testConstraints20() throws Exception{
		String[] strings= {"Decl(B:aField) =^= p.B", "Decl(A:aField) =^= p.A", "Decl(B:aField) < Decl(A:aField)", "[aField] =^= A", "[aField] =^= A"};
		testConstraints(strings);
	}

	public void testConstraints21() throws Exception{
		String[] strings= {"Decl(A:f2(A[])) =^= p.A", "[Parameter(0,A:f2(A[]))] =^= [as]", "[as] =^= A[]"};
		testConstraints(strings);
	}

	public void testConstraints22() throws Exception{
		String[] strings= {"[null] <= [A:f(A, Object)]_returnType", "[Parameter(0,A:f(A, Object))] =^= [a0]", "[Parameter(1,A:f(A, Object))] =^= [a1]", "[A:f(A, Object)]_returnType =^= A", "Decl(A:f(A, Object)) =^= p.A", "[a1] =^= Object", "[a0] =^= A"};
		testConstraints(strings);
	}

	public void testConstraints23() throws Exception{
		//test for bug 41271 NullPointerException dumping set of ITypeConstraints to System.out
		String[] strings= {"[args.length] =^= int", "[0] <= [i]", "[i] =^= int", "[args] <= Decl((array type):length)", "[args] =^= String[]", "[Parameter(0,Test1:main(String[]))] =^= [args]", "Decl(Test1:main(String[])) =^= p.Test1"};
		testConstraints(strings);
	}
}
