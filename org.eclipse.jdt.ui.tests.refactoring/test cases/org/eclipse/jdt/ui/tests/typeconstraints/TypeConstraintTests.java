/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.tests.typeconstraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.tests.refactoring.MySetup;
import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintCollector;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ExtractInterfaceUtil;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ITypeConstraint;

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
		return new MySetup(new TestSuite(clazz));
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
		return AST.parseCompilationUnit(cu, true);
	}

	private static void assertAllSatisfied(ITypeConstraint[] constraints){
		for (int i= 0; i < constraints.length; i++) {
			assertTrue(constraints[i].toString(), constraints[i].isSatisfied());
		}
	}
	
	private void numberHelper(int number) throws Exception {
		CompilationUnit cuNode= getCuNode();
		ConstraintCollector collector= new ConstraintCollector();
		cuNode.accept(collector);
		ITypeConstraint[] constraints= collector.getConstraints();
		assertEquals(Arrays.asList(constraints).toString(), number, constraints.length);
		assertAllSatisfied(constraints);
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

	private static List allToStrings(Object[] elements) {
		String[] strings= new String[elements.length];
		for (int i= 0; i < elements.length; i++) {
			strings[i]= elements[i].toString();
		}
		return new ArrayList(Arrays.asList(strings));//want to be able to remove stuff from it
	}
	
	private void testConstraints(String[] constraintStrings) throws Exception{
		CompilationUnit cuNode= getCuNode();
		ConstraintCollector collector= new ConstraintCollector();
		cuNode.accept(collector);
		ITypeConstraint[] constraints= collector.getConstraints();
		
		List externals= allToStrings(constraints);
		assertEquals("length", constraintStrings.length, constraints.length);
		for (int i= 0; i < constraintStrings.length; i++) {
			assertTrue("missing constraint:" + constraintStrings[i], externals.remove(constraintStrings[i]));
		}
		assertAllSatisfied(constraints);
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
//
//	public void testConstraints4() throws Exception{
//		String[] strings= {};
//		testConstraints(strings);
//	}	
//
//	public void testConstraints5() throws Exception{
//		String[] strings= {};
//		testConstraints(strings);
//	}	

	public void testConstraints6() throws Exception{
		String[] strings= {"Decl(A:f()) =^= p.A", "Decl(A:A(A)) =^= p.A", "[new A(a0)] =^= p.A", "[a0] <= [Parameter(0,A:A(A))]", "[a1] =^= A", "[a0] =^= A", "[null] <= [a0]", "[new A(a0)] <= [a1]", "[Parameter(0,A:A(A))] =^= [a]"};
		testConstraints(strings);
	}	

	public void testConstraints7() throws Exception{
		String[] strings= {"Decl(A:A()) =^= p.A", "Decl(A:A(A)) =^= p.A", "[null] <= [Parameter(0,A:A(A))]", "[Parameter(0,A:A(A))] =^= [a]"};
		testConstraints(strings);
	}	

//	public void testConstraints8() throws Exception{
//		String[] strings= {};
//		testConstraints(strings);
//	}	
//
	public void testConstraints9() throws Exception{
		String[] strings= {"Decl(A:f()) =^= p.A", "[a] =^= A", "[x] =^= boolean", "[a instanceof A] <= [x]", "[null] <= [a]", "[a] <= A or A <= [a]"};
		testConstraints(strings);
	}	
//
//	public void testConstraints10() throws Exception{
//		String[] strings= {};
//		testConstraints(strings);
//	}	
//
//	public void testConstraints11() throws Exception{
//		String[] strings= {};
//		testConstraints(strings);
//	}	

//	public void testConstraints12() throws Exception{
//		String[] strings= {};
//		testConstraints(strings);
//	}	
//
//	public void testConstraints13() throws Exception{
//		String[] strings= {};
//		testConstraints(strings);
//	}	
//
//	public void testConstraints14() throws Exception{
//		String[] strings= {};
//		testConstraints(strings);
//	}	
//
//	public void testConstraints15() throws Exception{
//		String[] strings= {};
//		testConstraints(strings);
//	}	

	public void testConstraints16() throws Exception{
		String[] strings= {"Decl(A:aField) =^= p.A", "Decl(A:f()) =^= p.A", "[this] =^= p.A", "[this] =^= p.A", "[this] <= [aTemp]", "[this] <= [aField]", "[this] <= [a]", "[a] =^= A", "[aField] =^= A", "[aTemp] =^= A", "[this] =^= p.A"};
		testConstraints(strings);
	}	

	public void testConstraints17() throws Exception{
		String[] strings= {"Decl(A:f()) =^= p.A", "[null] <= [a]", "[A:f()]_returnType =^= A", "[a] =^= A", "[a] <= [A:f()]_returnType"};
		testConstraints(strings);
	}	

//	public void testConstraints18() throws Exception{
//		String[] strings= {};
//		testConstraints(strings);
//	}	
//
//	public void testConstraints19() throws Exception{
//		String[] strings= {};
//		testConstraints(strings);
//	}	

	public void testConstraints20() throws Exception{
		String[] strings= {"Decl(B:aField) =^= p.B", "Decl(A:aField) =^= p.A", "Decl(B:aField) < Decl(A:aField)", "[aField] =^= A", "[aField] =^= A"};
		testConstraints(strings);
	}	
	
//	private void testBadVars(String[] badVars, String[] initiallyBadVars) throws Exception {
//		CompilationUnit cuNode= getCuNode();
//		ConstraintCollector collector= new ConstraintCollector();
//		cuNode.accept(collector);
//		ITypeConstraint[] constraints= collector.getConstraints();
//		assertAllSatisfied(constraints);
//		ITypeBinding classBinding= ((TypeDeclaration)cuNode.types().get(0)).resolveBinding();
//		assertTrue("isClass", classBinding.isClass());
//		ITypeBinding interfaceBinding= ((TypeDeclaration)cuNode.types().get(1)).resolveBinding();
//		assertTrue("isInterface", interfaceBinding.isInterface());
//
//		Set setOfAll= new HashSet(Arrays.asList(ExtractInterfaceUtil.getAllOfType(constraints, classBinding)));
//		ConstraintVariable[] initiallyBad= ExtractInterfaceUtil.getInitialBad(setOfAll, constraints, interfaceBinding);
//		List initiallyBadStrings= allToStrings(initiallyBad);
//		assertEquals(initiallyBadStrings.toString(), initiallyBadVars.length, initiallyBadStrings.size());
//		for (int i= 0; i < initiallyBadVars.length; i++) {
//			assertTrue(initiallyBadVars[i], initiallyBadStrings.contains(initiallyBadVars[i]));
//		}
//
//		//TODO fix		
////		ConstraintVariable[] bad= ExtractInterfaceUtil.getBad(constraints, classBinding, interfaceBinding);
////		List badStrings= allToStrings(bad);
////		assertEquals(badStrings.toString(), badVars.length, badStrings.size());
////		for (int i= 0; i < badVars.length; i++) {
////			assertTrue(badVars[i], badStrings.contains(badVars[i]));
////		}
//	}
	
//	public void testBad0() throws Exception{
////		String[] badVars= {"[a2]"};
////		testBadVars(badVars);
//	}
//
//	public void testBad1() throws Exception{
//		String[] initialBad= {"[a2]"};
//		String[] badVars= {"[a2]"};
//		testBadVars(badVars, initialBad);
//	}
//
//	public void testBad2() throws Exception{
//		String[] initialBad= {"[a2]"};
//		String[] badVars= {"[a1]", "[a2]"};
//		testBadVars(badVars, initialBad);
//	}
//
//	public void testBad3() throws Exception{
//		String[] initialBad= {"[a2]"};
//		String[] badVars= {"[a1]", "[a2]"};
//		testBadVars(badVars, initialBad);
//	}
//
//	public void testBad4() throws Exception{
//		String[] initialBad= {"[a2]"};
//		String[] badVars= {"[a2]", "[(A)a1]"};
//		testBadVars(badVars, initialBad);
//	}

	public void testUpdatableExtractInterfaceRanges0() throws Exception{
		ICompilationUnit cu= createCUfromTestFile(getPackageP());
		IType theInterface= cu.getType("Bag");
		IType theClass= cu.getType("A");
		CompilationUnitRange[] ranges= ExtractInterfaceUtil.getUpdatableRanges(theClass, theInterface, new NullProgressMonitor());		
		assertEquals(10, ranges.length);
	}
}
