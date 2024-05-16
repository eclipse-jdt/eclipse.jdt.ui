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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTFlattener;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.MethodOverrideTester;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

public class MethodOverrideTest extends CoreTests {
	/**
	 * See bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=111093
	 */

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private static final boolean DEBUG_SHOWRESULTS= true;

	private IJavaProject fJProject1;

	@Before
	public void setUp() throws Exception {
		fJProject1= pts.getProject();
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		JavaCore.setOptions(options);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	@Test
	public void testOverride0() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A<S> {
			    public A() {}
			    public void o0_foo(S t) {}
			}
			class B<T> extends A<T> {
			    public B() {}
			    @Override public void o0_foo(T t) {}
			}
			class C extends B<String> {
			    public C() {}
			    @Override public void o0_foo(String t) {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		doOverrideTests(cu, 2, 2, 1); // C and B
		doOverrideTests(cu, 2, 2, 0); // C and A
		doOverrideTests(cu, 2, 1, 0); // B and A
	}

	@Test
	public void testOverride1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A<T> {
			    public A() {}
			    public void o1_foo1(T t) {}
			    public void o1_foo2(int i, String s) {}
			    public void o1_foo3(String s) {}
			    public void o1_foo4(T... t) {}
			    public void o1_foo5(A<T> s) {}
			    public void o1_foo6(A<? super T> s) {}
			    public void o1_foo7(T... t) {}
			    public void o1_xoo1(T[] t) {}
			    public void o1_xoo2(A<?> s) {}
			    public void o1_xoo3(A<? extends T> s) {}
			}
			class B<S> extends A<S> {
			    public B() {}
			    @Override public void o1_foo1(S t) {}
			    @Override public void o1_foo2(int i, String s) {}
			    @Override public void o1_foo3(String s) {}
			    @Override public void o1_foo4(S... t) {}
			    @Override public void o1_foo5(A<S> s) {}
			    @Override public void o1_foo6(A<? super S> s) {}
			    @Override public void o1_foo7(S[] t) {}
			    @Override public void o1_xoo1(S[][] t) {}
			    @Override public void o1_xoo2(A<Object> s) {}
			    @Override public void o1_xoo3(A<? super S> s) {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		doOverrideTests(cu, 1, 1, 0); // B and A
	}

	@Test
	public void testOverride2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			import java.util.Map;
			public class A<S, T> {
			    public A() {}
			    public void o2_foo1(Map<S, T> t) {}
			    public void o2_xoo1(List<? extends T> t) {}
			}
			class B<V, W> extends A<W, V> {
			    public B() {}
			    @Override public void o2_foo1(Map<W, V> t) {}
			    @Override public void o2_xoo1(List<? extends W> t) {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		doOverrideTests(cu, 1, 1, 0); // B and A
	}

	@Test
	public void testOverride3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class A {
			    public A() {}
			    public void o3_foo1(List<Object> t) {}
			    public void o3_xoo1(List t) {}
			}
			class B extends A {
			    public B() {}
			    @Override public void o3_foo1(List t) {}
			    @Override public void o3_xoo1(List<Object> t) {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		doOverrideTests(cu, 1, 1, 0); // B and A
	}

	@Test
	public void testOverride4() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class A<T> {
			    public A() {}
			    public void o4_foo1(T[] t) {}
			    public void o4_foo2(T t) {}
			    public void o4_foo3(T t) {}
			    public void o4_xoo1(T t) {}
			}
			class B extends A<List<String>> {
			    public B() {}
			    @Override public void o4_foo1(List<String>[] t) {}
			    @Override public void o4_foo2(List<String> t) {}
			    @Override public void o4_foo3(List t) {}
			    @Override public void o4_xoo1(List<?> t) {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		doOverrideTests(cu, 1, 1, 0); // B and A
	}

	@Test
	public void testOverrideMethodTypeParams1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		// ATTENTION: Method names in this test must be in alphabetic order!
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A<S> {
			    public A() {}
			    public <T extends Enum<T>> T getEnum(String name, Class<T> clazz) { return null; }
			    public <X> void tp1_foo1(S s, X x) {}
			    public <X, Y> void tp1_foo2(S s, X x, Y y) {}
			    public <X, Y> void tp1_foo3(X x, Y y) {}
			    public <X extends Number> void tp1_foo4(X x) {}
			    public <X extends Object> void tp1_foo5(X x) {}
			    public <X, Y> void tp1_xoo1(S s, X x, Y y) {}
			    public void tp1_xoo2() {}
			    public <X, Y> void tp1_xoo3(X x, Y y) {}
			    public <X extends Number> void tp1_xoo5(X x) {}
			}
			class B extends A<String> {
			    public B() {}
			    @Override public <E extends Enum<E>> E getEnum(String name, Class<E> clazz) { return null; }
			    @Override public <X> void tp1_foo1(String s, X x) {}
			    @Override public void tp1_foo2(String s, Object x, Object y) {}
			    @Override public <V, W> void tp1_foo3(V x, W y) {}
			    @Override public <X extends Number> void tp1_foo4(X x) {}
			    @Override public <X> void tp1_foo5(X x) {}
			    @Override public <X> void tp1_xoo1(String s, X x, Object y) {}
			    @Override public <X> void tp1_xoo2() {}
			    @Override public <W, V> void tp1_xoo3(V x, W y) {}
			    @Override public <X> void tp1_xoo5(Number x) {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		doOverrideTests(cu, 1, 1, 0); // B and A
	}

	@Test
	public void testOverrideRaw1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A<S> {
			    public A() {}
			    public void r1_foo1(S s) {}
			    public void r1_foo2(A<S> s) {}
			}
			class B extends A {
			    public B() {}
			    @Override public void r1_foo1(Object s) {}
			    @Override public void r1_foo2(A s) {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		doOverrideTests(cu, 1, 1, 0); // B and A
	}

	protected void doOverrideTests(ICompilationUnit cu, int focusIndex, int overridingIndex, int overriddenIndex) throws JavaModelException {
		CompilationUnit root= assertNoCompilationError(cu);

		IType[] types= cu.getTypes();
		ITypeBinding[] typeBindings= new ITypeBinding[types.length];

		typeBindings[types.length - 1]= ((TypeDeclaration) root.types().get(types.length - 1)).resolveBinding();
		for (int i= types.length - 2; i >= 0; i--) {
			typeBindings[i]= typeBindings[i + 1].getSuperclass();
		}

		IType focusType= types[focusIndex];

		IType overridingType= types[overridingIndex];
		ITypeBinding overridingTypeBinding= typeBindings[overridingIndex];
		assertSameType(overridingType, overridingTypeBinding);

		IType overriddenType= types[overriddenIndex];
		ITypeBinding overriddenTypeBinding= typeBindings[overriddenIndex];
		assertSameType(overriddenType, overriddenTypeBinding);

		doOverrideTests(root, focusType, overridingType, overridingTypeBinding, overriddenType, overriddenTypeBinding);
	}

	protected void doOverrideTests(CompilationUnit root, IType focusType, IType overridingType, ITypeBinding overridingTypeBinding, IType overriddenType, ITypeBinding overriddenTypeBinding)
			throws JavaModelException {
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

			boolean overrideAnnotationResult= overrideAnnotation;
			boolean testerOverrides= tester.isSubsignature(overriding, overriddenMethod);
			boolean uiBindingsIsSubsignature= Bindings.isSubsignature(overridingBinding, overriddenBinding);

			if (DEBUG_SHOWRESULTS) {
				boolean bindingOverrides= overridingBinding.getMethodDeclaration().overrides(overriddenBinding.getMethodDeclaration());
				boolean bindingIsSubsignature= overridingBinding.isSubsignature(overriddenBinding);

				if (testerOverrides != overrideAnnotationResult || testerOverrides != bindingOverrides
						|| testerOverrides != bindingIsSubsignature || testerOverrides != uiBindingsIsSubsignature) {

					System.out.println();
					System.out.println("====================================");
					System.out.println("getName()");
					System.out.println("====================================");

					System.out.println("IMethodBinding.overrides(): " +  String.valueOf(bindingOverrides));
					System.out.println("IMethodBinding.isSubsignature(): " +  String.valueOf(bindingIsSubsignature));
					System.out.println("MethodOverrideTester.isSubsignature(): " +  String.valueOf(testerOverrides));
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
		StringBuilder buf= new StringBuilder();
		buf.append(BindingLabelProvider.getBindingLabel(overriding, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.T_TYPE_PARAMETERS));
		buf.append(" - ");
		buf.append(BindingLabelProvider.getBindingLabel(overriddenMethod, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_FULLY_QUALIFIED  | JavaElementLabels.T_TYPE_PARAMETERS));
		buf.append(" (");
		buf.append(BindingLabelProvider.getBindingLabel(overriddenMethod.getMethodDeclaration(), JavaElementLabels.M_PARAMETER_TYPES  | JavaElementLabels.T_TYPE_PARAMETERS));
		buf.append(")");
		return buf.toString();
	}

	private static String getCodeString(IMethodBinding overriding,  IMethodBinding overriddenMethod, CompilationUnit root) {
		StringBuilder buf= new StringBuilder();

		buf.append("// Overridden: ----------------------------------\n");
		buf.append(getCode(overriddenMethod, root)).append('\n');

		buf.append("// Overriding: ----------------------------------\n");
		buf.append(getCode(overriding, root)).append('\n');
		return buf.toString();
	}

	private static String getCode(IMethodBinding binding, CompilationUnit root) {

		final MethodDeclaration method=  (MethodDeclaration) root.findDeclaringNode(binding.getMethodDeclaration());

		ASTFlattener flattener= new ASTFlattener() {
			@Override
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

	protected CompilationUnit assertNoCompilationError(ICompilationUnit cu) {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setSource(cu);
		parser.setResolveBindings(true);
		CompilationUnit root= (CompilationUnit) parser.createAST(null);
		IProblem[] problems= root.getProblems();
		boolean hasProblems= false;
		for (IProblem prob : problems) {
			if (prob.isWarning() || prob.isInfo()) {
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
			StringBuilder buf= new StringBuilder();
			buf.append(cu.getElementName() + " has compilation problems: \n");
			for (IProblem prob : problems) {
				buf.append(prob.getMessage()).append('\n');
			}
			fail(buf.toString());
		}


		return root;
	}

	private Boolean isOverrideAnnotationOK(CompilationUnit root, IMethodBinding overriding) {
		ASTNode node= root.findDeclaringNode(overriding);
		if (node instanceof MethodDeclaration && hasOverrideAnnotation((MethodDeclaration) node)) {
			int start= node.getStartPosition();
			int end= start + node.getLength();

			for (IProblem prob : root.getProblems()) {
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
		List<IExtendedModifier> list= declaration.modifiers();
		for (IExtendedModifier element : list) {
			if (element instanceof Annotation) {
				return "Override".equals(((Annotation) element).getTypeName().getFullyQualifiedName());
			}
		}
		return false;
	}


}
