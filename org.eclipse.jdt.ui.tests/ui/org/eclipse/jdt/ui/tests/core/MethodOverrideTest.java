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

import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTFlattener;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.MethodOverrideTester;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

public class MethodOverrideTest extends CoreTests {


	/**
	 * See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=111093
	 */

	private static final Class THIS= MethodOverrideTest.class;
	private static final boolean DEBUG_SHOWRESULTS= false;

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		return allTests();
	}

	private IJavaProject fJProject1;

	public MethodOverrideTest(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		fJProject1= ProjectTestSetup.getProject();

		Hashtable options= TestOptions.getDefaultOptions();
		JavaCore.setOptions(options);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	public void testOverride0() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A<S> {\n");
		buf.append("    public A() {}\n");
		buf.append("    public void o0_foo(S t) {}\n");
		buf.append("}\n");
		buf.append("class B<T> extends A<T> {\n");
		buf.append("    public B() {}\n");
		buf.append("    @Override public void o0_foo(T t) {}\n");
		buf.append("}\n");
		buf.append("class C extends B<String> {\n");
		buf.append("    public C() {}\n");
		buf.append("    @Override public void o0_foo(String t) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		doOverrideTests(cu, 2, 2, 1); // C and B
		doOverrideTests(cu, 2, 2, 0); // C and A
		doOverrideTests(cu, 2, 1, 0); // B and A
	}

	public void testOverride1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A<T> {\n");
		buf.append("    public A() {}\n");
		buf.append("    public void o1_foo1(T t) {}\n");
		buf.append("    public void o1_foo2(int i, String s) {}\n");
		buf.append("    public void o1_foo3(String s) {}\n");
		buf.append("    public void o1_foo4(T... t) {}\n");
		buf.append("    public void o1_foo5(A<T> s) {}\n");
		buf.append("    public void o1_foo6(A<? super T> s) {}\n");
		buf.append("    public void o1_foo7(T... t) {}\n");
		buf.append("    public void o1_xoo1(T[] t) {}\n");
		buf.append("    public void o1_xoo2(A<?> s) {}\n");
		buf.append("    public void o1_xoo3(A<? extends T> s) {}\n");
		buf.append("}\n");
		buf.append("class B<S> extends A<S> {\n");
		buf.append("    public B() {}\n");
		buf.append("    @Override public void o1_foo1(S t) {}\n");
		buf.append("    @Override public void o1_foo2(int i, String s) {}\n");
		buf.append("    @Override public void o1_foo3(String s) {}\n");
		buf.append("    @Override public void o1_foo4(S... t) {}\n");
		buf.append("    @Override public void o1_foo5(A<S> s) {}\n");
		buf.append("    @Override public void o1_foo6(A<? super S> s) {}\n");
		buf.append("    @Override public void o1_foo7(S[] t) {}\n");
		buf.append("    @Override public void o1_xoo1(S[][] t) {}\n");
		buf.append("    @Override public void o1_xoo2(A<Object> s) {}\n");
		buf.append("    @Override public void o1_xoo3(A<? super S> s) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		doOverrideTests(cu, 1, 1, 0); // B and A
	}

	public void testOverride2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.Map;\n");
		buf.append("public class A<S, T> {\n");
		buf.append("    public A() {}\n");
		buf.append("    public void o2_foo1(Map<S, T> t) {}\n");
		buf.append("    public void o2_xoo1(List<? extends T> t) {}\n");
		buf.append("}\n");
		buf.append("class B<V, W> extends A<W, V> {\n");
		buf.append("    public B() {}\n");
		buf.append("    @Override public void o2_foo1(Map<W, V> t) {}\n");
		buf.append("    @Override public void o2_xoo1(List<? extends W> t) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		doOverrideTests(cu, 1, 1, 0); // B and A
	}

	public void testOverride3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public A() {}\n");
		buf.append("    public void o3_foo1(List<Object> t) {}\n");
		buf.append("    public void o3_xoo1(List t) {}\n");
		buf.append("}\n");
		buf.append("class B extends A {\n");
		buf.append("    public B() {}\n");
		buf.append("    @Override public void o3_foo1(List t) {}\n");
		buf.append("    @Override public void o3_xoo1(List<Object> t) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		doOverrideTests(cu, 1, 1, 0); // B and A
	}

	public void testOverride4() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A<T> {\n");
		buf.append("    public A() {}\n");
		buf.append("    public void o4_foo1(T[] t) {}\n");
		buf.append("    public void o4_foo2(T t) {}\n");
		buf.append("    public void o4_foo3(T t) {}\n");
		buf.append("    public void o4_xoo1(T t) {}\n");

		buf.append("}\n");
		buf.append("class B extends A<List<String>> {\n");
		buf.append("    public B() {}\n");
		buf.append("    @Override public void o4_foo1(List<String>[] t) {}\n");
		buf.append("    @Override public void o4_foo2(List<String> t) {}\n");
		buf.append("    @Override public void o4_foo3(List t) {}\n");
		buf.append("    @Override public void o4_xoo1(List<?> t) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		doOverrideTests(cu, 1, 1, 0); // B and A
	}

	public void testOverrideMethodTypeParams1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A<S> {\n");
		buf.append("    public A() {}\n");
		buf.append("    public <X> void tp1_foo1(S s, X x) {}\n");
		buf.append("    public <X, Y> void tp1_foo2(S s, X x, Y y) {}\n");
		buf.append("    public <X, Y> void tp1_foo3(X x, Y y) {}\n");
		buf.append("    public <X extends Number> void tp1_foo4(X x) {}\n");
		buf.append("    public <X extends Object> void tp1_foo5(X x) {}\n");
		buf.append("    public <X, Y> void tp1_xoo1(S s, X x, Y y) {}\n");
		buf.append("    public void tp1_xoo2() {}\n");
		buf.append("    public <X, Y> void tp1_xoo3(X x, Y y) {}\n");
//		buf.append("    public <T extends Number & Runnable> void tp1_xoo4() {}\n");
		buf.append("    public <X extends Number> void tp1_xoo5(X x) {}\n");
		buf.append("}\n");
		buf.append("class B extends A<String> {\n");
		buf.append("    public B() {}\n");
		buf.append("    @Override public <X> void tp1_foo1(String s, X x) {}\n");
		buf.append("    @Override public void tp1_foo2(String s, Object x, Object y) {}\n");
		buf.append("    @Override public <V, W> void tp1_foo3(V x, W y) {}\n");
		buf.append("    @Override public <X extends Number> void tp1_foo4(X x) {}\n");
		buf.append("    @Override public <X> void tp1_foo5(X x) {}\n");
		buf.append("    @Override public <X> void tp1_xoo1(String s, X x, Object y) {}\n");
		buf.append("    @Override public <X> void tp1_xoo2() {}\n");
		buf.append("    @Override public <W, V> void tp1_xoo3(V x, W y) {}\n");
//		buf.append("    @Override public <T extends Number> void tp1_xoo4() {}\n");   // jdt.core bug, need to compare all bounds
		buf.append("    @Override public <X> void tp1_xoo5(Number x) {}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		doOverrideTests(cu, 1, 1, 0); // B and A
	}

	public void testOverrideRaw1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A<S> {\n");
		buf.append("    public A() {}\n");
		buf.append("    public void r1_foo1(S s) {}\n");
		buf.append("    public void r1_foo2(A<S> s) {}\n");
//		buf.append("    public void r1_xoo1(A<S> s) {}\n");
		buf.append("}\n");
		buf.append("class B extends A {\n");
		buf.append("    public B() {}\n");
		buf.append("    @Override public void r1_foo1(Object s) {}\n");
		buf.append("    @Override public void r1_foo2(A s) {}\n");
//		buf.append("    @Override public void r1_xoo1(A<Object> s) {}\n");  // bug in our implementation: extended raw type has all types raw
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		doOverrideTests(cu, 1, 1, 0); // B and A
	}


	private void doOverrideTests(ICompilationUnit cu, int focusIndex, int overridingIndex, int overriddenIndex) throws JavaModelException {
		CompilationUnit root= assertNoCompilationError(cu);

		IType[] types= cu.getTypes();
		ITypeBinding[] typeBindings= new ITypeBinding[types.length];

		typeBindings[types.length - 1]= ((TypeDeclaration) root.types().get(types.length - 1)).resolveBinding();
		for (int i= types.length - 2; i >= 0; i--) {
			typeBindings[i]= typeBindings[i + 1].getSuperclass();
		}

		IType overridingType= types[overridingIndex];
		ITypeBinding overridingTypeBinding= typeBindings[overridingIndex];
		assertSameType(overridingType, overridingTypeBinding);

		IType overriddenType= types[overriddenIndex];
 		ITypeBinding overriddenTypeBinding= typeBindings[overriddenIndex];
		assertSameType(overriddenType, overriddenTypeBinding);

		IType focusType= types[focusIndex];
		ITypeHierarchy hierarchy= focusType.newTypeHierarchy(null);
		MethodOverrideTester tester= new MethodOverrideTester(focusType, hierarchy);

		IMethod[] overridingMethods= overridingType.getMethods();
		IMethod[] overriddenMethods= overriddenType.getMethods();

		IMethodBinding[] overridingBindings= overridingTypeBinding.getDeclaredMethods();
		IMethodBinding[] overriddenBindings= overriddenTypeBinding.getDeclaredMethods();

		for (int i= 0; i < overridingMethods.length; i++) {

			IMethod overriding= overridingMethods[i];
			IMethodBinding overridingBinding= overridingBindings[i];

			IMethod overriddenMethod= overriddenMethods[i];
			IMethodBinding overriddenBinding= overriddenBindings[i];

			Boolean overrideAnnotation= isOverrideAnnotationOK(root, overridingBinding);
			if (overrideAnnotation == null) {
				continue; // only look at methods with the override annotation
			}

			boolean overrideAnnotationResult= overrideAnnotation.booleanValue();
			boolean testerOverrides= tester.isSubsignature(overriding, overriddenMethod);
			boolean uiBindingsIsSubsignature= Bindings.isSubsignature(overridingBinding, overriddenBinding);

			if (DEBUG_SHOWRESULTS) {
				boolean bindingOverrides= overridingBinding.getMethodDeclaration().overrides(overriddenBinding.getMethodDeclaration());
				boolean bindingIsSubsignature= overridingBinding.isSubsignature(overriddenBinding);

				if (testerOverrides != overrideAnnotationResult || testerOverrides != bindingOverrides
						|| testerOverrides != bindingIsSubsignature || testerOverrides != uiBindingsIsSubsignature) {

					System.out.println();
					System.out.println("====================================");
					System.out.println(getName());
					System.out.println("====================================");

					System.out.println("IMethodBinding.overrides(): " +  String.valueOf(bindingOverrides));
					System.out.println("IMethodBinding.isSubsignature(): " +  String.valueOf(bindingIsSubsignature));
					//System.out.println("MethodOverrideTester.isSubsignature(): " +  String.valueOf(testerOverrides));
					System.out.println("Bindings.isSubsignature(): " +  String.valueOf(uiBindingsIsSubsignature));
					System.out.println("Override Annotation: " +  String.valueOf(overrideAnnotationResult));
					System.out.println();
					System.out.println(getCodeString(overridingBinding, overriddenBinding, root));
					System.out.println();
				}
//				else {
//					System.out.println(getDebugString(overridingBinding, overriddenBinding));
//					System.out.println("    " +  String.valueOf(overrideAnnotationResult));
//				}
			}
			if (overrideAnnotationResult != testerOverrides) {
				assertEquals(getDebugString(overridingBinding, overriddenBinding), overrideAnnotationResult, testerOverrides);
			}
			/*if (overrideAnnotationResult != uiBindingsIsSubsignature) {
				assertEquals(getDebugString(overridingBinding, overriddenBinding), overrideAnnotationResult, uiBindingsIsSubsignature);
			}*/

		}
	}

	private static String getDebugString(IMethodBinding overriding,  IMethodBinding overriddenMethod) {
		StringBuffer buf= new StringBuffer();
		buf.append(BindingLabelProvider.getBindingLabel(overriding, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.T_TYPE_PARAMETERS));
		buf.append(" - ");
		buf.append(BindingLabelProvider.getBindingLabel(overriddenMethod, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_FULLY_QUALIFIED  | JavaElementLabels.T_TYPE_PARAMETERS));
		buf.append(" (");
		buf.append(BindingLabelProvider.getBindingLabel(overriddenMethod.getMethodDeclaration(), JavaElementLabels.M_PARAMETER_TYPES  | JavaElementLabels.T_TYPE_PARAMETERS));
		buf.append(")");
		return buf.toString();
	}

	private static String getCodeString(IMethodBinding overriding,  IMethodBinding overriddenMethod, CompilationUnit root) {
		StringBuffer buf= new StringBuffer();

		buf.append("// Overridden: ----------------------------------\n");
		buf.append(getCode(overriddenMethod, root)).append('\n');

		buf.append("// Overriding: ----------------------------------\n");
		buf.append(getCode(overriding, root)).append('\n');
		return buf.toString();
	}

	private static String getCode(IMethodBinding binding, CompilationUnit root) {

		final MethodDeclaration method=  (MethodDeclaration) root.findDeclaringNode(binding.getMethodDeclaration());

		ASTFlattener flattener= new ASTFlattener() {
			public boolean visit(MethodDeclaration node) {
				if (node == method) {
					super.visit(node);
				}
				return false;
			}
		};
		method.getParent().accept(flattener);
		String unformatted= flattener.getResult();

		TextEdit edit= CodeFormatterUtil.format2(method.getParent(), unformatted, 0, "\n", null);
		if (edit != null) {
			Document document= new Document(unformatted);
			try {
				edit.apply(document, TextEdit.NONE);
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			}
			return document.get();
		}
		return unformatted; // unknown node
	}


	private void assertSameType(IType type, ITypeBinding binding) throws JavaModelException {
		assertNotNull(binding);
		assertEquals(type.getElementName(), binding.getTypeDeclaration().getName());

		IMethod[] methods= type.getMethods();
		IMethodBinding[] bindings= binding.getDeclaredMethods();
		assertEquals(methods.length, bindings.length);
	}

	private CompilationUnit assertNoCompilationError(ICompilationUnit cu) {
		ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit root= (CompilationUnit) parser.createAST(null);
		IProblem[] problems= root.getProblems();
		boolean hasProblems= false;
		for (int i= 0; i < problems.length; i++) {
			IProblem prob= problems[i];
			if (prob.isWarning()) {
				continue;
			}
			switch (prob.getID()) {
				// ignored problems
				case IProblem.MethodNameClash:
					continue;
				case IProblem.MethodMustOverride:
					continue;
				default:
					hasProblems= true;
					break;

			}
		}
		if (hasProblems) {
			StringBuffer buf= new StringBuffer();
			buf.append(cu.getElementName() + " has compilation problems: \n");
			for (int i= 0; i < problems.length; i++) {
				buf.append(problems[i].getMessage()).append('\n');
			}
			assertTrue(buf.toString(), false);
		}


		return root;
	}

	private Boolean isOverrideAnnotationOK(CompilationUnit root, IMethodBinding overriding) {
		ASTNode node= root.findDeclaringNode(overriding);
		if (node instanceof MethodDeclaration && hasOverrideAnnotation((MethodDeclaration) node)) {
			int start= node.getStartPosition();
			int end= start + node.getLength();

			IProblem[] problems= root.getProblems();
			for (int i= 0; i < problems.length; i++) {
				IProblem prob= problems[i];
				if (prob.getID() == IProblem.MethodMustOverride) {
					int pos= prob.getSourceStart();
					if (start <= pos && pos < end) {
						return Boolean.FALSE;
					}
				}
			}
			return Boolean.TRUE;
		}
		return null;
	}

	private boolean hasOverrideAnnotation(MethodDeclaration declaration) {
		List list= declaration.modifiers();
		for (int i= 0; i < list.size(); i++) {
			if (list.get(i) instanceof Annotation) {
				return "Override".equals(((Annotation) list.get(i)).getTypeName().getFullyQualifiedName());
			}
		}
		return false;
	}


}
