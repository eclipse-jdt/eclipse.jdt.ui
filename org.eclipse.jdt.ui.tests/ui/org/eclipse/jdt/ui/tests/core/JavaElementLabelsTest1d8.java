/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class JavaElementLabelsTest1d8 extends CoreTests {
	@Rule
	public Java1d8ProjectTestSetup j18p= new Java1d8ProjectTestSetup();

	private IJavaProject fJProject1;

	@Before
	public void setUp() throws Exception {
		fJProject1= j18p.getProject();

		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		store.setValue(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES, false);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, j18p.getDefaultClasspath());
	}

	@Test
	public void testMethodLabelPolymorphicSignatureDeclaration() throws Exception {
		IType methodHandle= fJProject1.findType("java.lang.invoke.MethodHandle");
		IMethod invokeExact= methodHandle.getMethod("invokeExact", new String[] {
				Signature.createArraySignature(Signature.createTypeSignature("java.lang.Object", true), 1)
		});

		String lab= JavaElementLabels.getTextLabel(invokeExact, JavaElementLabels.ALL_DEFAULT);
		assertEqualString(lab, "invokeExact(Object...)");

		lab= JavaElementLabels.getTextLabel(invokeExact, JavaElementLabels.M_PARAMETER_NAMES);
		assertEqualString(lab, "invokeExact(arg0)");

		lab= JavaElementLabels.getTextLabel(invokeExact, JavaElementLabels.M_PARAMETER_TYPES);
		assertEqualString(lab, "invokeExact(Object...)");

		lab= JavaElementLabels.getTextLabel(invokeExact, JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PARAMETER_TYPES);
		assertEqualString(lab, "invokeExact(Object... arg0)");

		lab= JavaElementLabels.getTextLabel(invokeExact, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invokeExact(Object...)");
	}

	private IJavaElement createInvokeReference(String invocation) throws CoreException, JavaModelException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package org.test;\n");
		buf.append("import java.lang.invoke.MethodHandle;\n");
		buf.append("public class Test {\n");
		buf.append("    void foo(MethodHandle mh) throws Throwable {\n");
		buf.append("        " + invocation + ";\n");
		buf.append("    }\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", content, false, null);

		IJavaElement elem= cu.codeSelect(content.indexOf("invoke("), 0)[0];
		return elem;
	}

	private static void assertInvokeUnresolved(IJavaElement elem) {
		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT);
		assertEqualString(lab, "invoke(Object...)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_NAMES);
		assertEqualString(lab, "invoke(arg0)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES);
		assertEqualString(lab, "invoke(Object...)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PRE_RETURNTYPE);
		assertEqualString(lab, "Object invoke(Object... arg0)");
	}

	@Test
	public void testMethodLabelPolymorphicSignatureReference0() throws Exception {
		IJavaElement elem= createInvokeReference("mh.invoke()");

		assertInvokeUnresolved(elem);

		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke()");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke()");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "void invoke()");
	}

	@Test
	public void testMethodLabelPolymorphicSignatureReference0Ret() throws Exception {
		IJavaElement elem= createInvokeReference("String s= (String) mh.invoke()");

		assertInvokeUnresolved(elem);

		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke()");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke()");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "Object invoke()");
	}

	@Test
	public void testMethodLabelPolymorphicSignatureReference1() throws Exception {
		IJavaElement elem= createInvokeReference("mh.invoke(1)");

		assertInvokeUnresolved(elem);

		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke(int)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke(int)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "void invoke(int arg00)");
	}

	@Test
	public void testMethodLabelPolymorphicSignatureReference1Array() throws Exception {
		IJavaElement elem= createInvokeReference("mh.invoke(new Object[42])");

		assertInvokeUnresolved(elem);

		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke(Object[])");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke(Object[])");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "void invoke(Object[] arg00)");
	}

	@Test
	public void testMethodLabelPolymorphicSignatureReference2() throws Exception {
		IJavaElement elem= createInvokeReference("mh.invoke('a', new Integer[0][])");

		assertInvokeUnresolved(elem);

		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke(char, Integer[][])");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke(char, Integer[][])");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "void invoke(char arg00, Integer[][] arg01)");
	}

	@Test
	public void testMethodLabelPolymorphicSignatureReference3Ret() throws Exception {
		IJavaElement elem= createInvokeReference("long l= (long) mh.invoke('a', new java.util.ArrayList<String>(), null)");

		assertInvokeUnresolved(elem);

		String lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke(char, ArrayList, Void)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke(char, ArrayList, Void)");

		lab= JavaElementLabels.getTextLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "Object invoke(char arg00, ArrayList arg01, Void arg02)");
	}

	@Test
	public void testTypeLabelLambda1() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			import java.util.function.IntConsumer;
			public class C {
			    IntConsumer c = (i) -> { };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", content, false, null);

		IJavaElement[] elems= cu.codeSelect(content.lastIndexOf("i"), 1);
		IJavaElement i= elems[0];
		String lab= JavaElementLabels.getTextLabel(i, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED);
		assertEqualString(lab, "org.test.C.c.() -> {...} IntConsumer.accept(int).i");

		IJavaElement lambdaMethod= i.getParent();
		lab= JavaElementLabels.getTextLabel(lambdaMethod, JavaElementLabels.T_FULLY_QUALIFIED
				| JavaElementLabels.M_POST_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES);
		assertEqualString(lab, "accept(int i) - org.test.C.c.() -> {...} IntConsumer");

		IJavaElement lambdaType= lambdaMethod.getParent();
		lab= JavaElementLabels.getTextLabel(lambdaType, JavaElementLabels.T_POST_QUALIFIED);
		assertEqualString(lab, "() -> {...} IntConsumer - org.test.C.c");
	}

	@Test
	public void testTypeLabelLambda2() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		String content= """
			package org.test;
			import java.util.function.Consumer;
			public class C {
			    Consumer<String> c = (s) -> { };
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", content, false, null);

		IJavaElement[] elems= cu.codeSelect(content.lastIndexOf("s"), 1);
		IJavaElement i= elems[0];
		String lab= JavaElementLabels.getTextLabel(i, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED);
		assertEqualString(lab, "org.test.C.c.() -> {...} Consumer.accept(String).s");

		lab= JavaElementLabels.getTextLabel(i, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "org.test.C.c.() -> {...} Consumer.accept(String).s");

		IJavaElement lambdaMethod= i.getParent();
		lab= JavaElementLabels.getTextLabel(lambdaMethod, JavaElementLabels.T_FULLY_QUALIFIED
				| JavaElementLabels.M_POST_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES);
		assertEqualString(lab, "accept(String s) - org.test.C.c.() -> {...} Consumer");

		IJavaElement lambdaType= lambdaMethod.getParent();
		lab= JavaElementLabels.getTextLabel(lambdaType, JavaElementLabels.T_POST_QUALIFIED);
		assertEqualString(lab, "() -> {...} Consumer - org.test.C.c");
	}

}
