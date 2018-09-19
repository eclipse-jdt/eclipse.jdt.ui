/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - Contribution for Bug 403917 - [1.8] Render TYPE_USE annotations in Javadoc hover/view
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;

import junit.framework.Test;
import junit.framework.TestSuite;

public class BindingLabels18Test extends AbstractBindingLabelsTest {

	private static final Class<BindingLabels18Test> THIS= BindingLabels18Test.class;

	public BindingLabels18Test(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	@Override
	protected void setUp() throws Exception {
		fJProject1= Java18ProjectTestSetup.getProject();

		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		store.setValue(PreferenceConstants.APPEARANCE_COMPRESS_PACKAGE_NAMES, false);
	}

	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java18ProjectTestSetup.getDefaultClasspath());
	}


	public void testMethodLabelPolymorphicSignatureDeclaration() throws Exception {
		IType methodHandle= fJProject1.findType("java.lang.invoke.MethodHandle");
		IMethod invokeExact= methodHandle.getMethod("invokeExact", new String[] {
				Signature.createArraySignature(Signature.createTypeSignature("java.lang.Object", true), 1)
		});
		
		String lab= getBindingLabel(invokeExact, JavaElementLabels.ALL_DEFAULT);
		assertLinkMatch(lab, "invokeExact({{java.lang|Object}}...)");
	
		lab= getBindingLabel(invokeExact, JavaElementLabels.M_PARAMETER_NAMES);
		assertEqualString(lab, "invokeExact(arg0)");
	
		lab= getBindingLabel(invokeExact, JavaElementLabels.M_PARAMETER_TYPES);
		assertLinkMatch(lab, "invokeExact({{java.lang|Object}}...)");
		
		lab= getBindingLabel(invokeExact, JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PARAMETER_TYPES);
		assertLinkMatch(lab, "invokeExact({{java.lang|Object}}... arg0)");
		
		lab= getBindingLabel(invokeExact, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "invokeExact({{java.lang|Object}}...)");
	}

	private IJavaElement createInvokeReference(String invocation) throws CoreException, JavaModelException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		
		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
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

	private void assertInvokeUnresolved(IJavaElement elem) {
		String lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT);
		assertLinkMatch(lab, "invoke({{java.lang|Object}}...)");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_NAMES);
		assertEqualString(lab, "invoke(arg0)");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES);
		assertLinkMatch(lab, "invoke({{java.lang|Object}}...)");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PRE_RETURNTYPE);
		assertLinkMatch(lab, "{{java.lang|Object}} invoke({{java.lang|Object}}... arg0)");
	}

	public void testMethodLabelPolymorphicSignatureReference0() throws Exception {
		IJavaElement elem= createInvokeReference("mh.invoke()");
		
		assertInvokeUnresolved(elem);
		
		String lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke()");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke()");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "void invoke()");
	}

	public void testMethodLabelPolymorphicSignatureReference0Ret() throws Exception {
		IJavaElement elem= createInvokeReference("String s= (String) mh.invoke()");
		
		assertInvokeUnresolved(elem);
		
		String lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke()");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke()");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "{{java.lang|Object}} invoke()");
	}
	
	public void testMethodLabelPolymorphicSignatureReference1() throws Exception {
		IJavaElement elem= createInvokeReference("mh.invoke(1)");
		
		assertInvokeUnresolved(elem);
		
		String lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke(int)");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertEqualString(lab, "invoke(int)");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "void invoke(int arg00)");
	}
	
	public void testMethodLabelPolymorphicSignatureReference1Array() throws Exception {
		IJavaElement elem= createInvokeReference("mh.invoke(new Object[42])");
		
		assertInvokeUnresolved(elem);
		
		String lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "invoke({{java.lang|Object}}[])");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "invoke({{java.lang|Object}}[])");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "void invoke({{java.lang|Object}}[] arg00)");
	}
	
	public void testMethodLabelPolymorphicSignatureReference2() throws Exception {
		IJavaElement elem= createInvokeReference("mh.invoke('a', new Integer[0][])");
		
		assertInvokeUnresolved(elem);
		
		String lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "invoke(char, {{java.lang|Integer}}[][])");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "invoke(char, {{java.lang|Integer}}[][])");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "void invoke(char arg00, {{java.lang|Integer}}[][] arg01)");
	}
	
	public void testMethodLabelPolymorphicSignatureReference3Ret() throws Exception {
		IJavaElement elem= createInvokeReference("long l= (long) mh.invoke('a', new java.util.ArrayList<String>(), null)");
		
		assertInvokeUnresolved(elem);
		
		String lab= getBindingLabel(elem, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "invoke(char, {{java.util|ArrayList}}<{{java.util.ArrayList|E}}>, {{java.lang|Void}})");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "invoke(char, {{java.util|ArrayList}}, {{java.lang|Void}})");
		
		lab= getBindingLabel(elem, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "{{java.lang|Object}} invoke(char arg00, {{java.util|ArrayList}} arg01, {{java.lang|Void}} arg02)");
	}
	
	public void testTypeLabelLambda1() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.util.function.IntConsumer;\n");
		buf.append("public class C {\n");
		buf.append("    IntConsumer c = (i) -> { };\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", content, false, null);

		IJavaElement[] elems= cu.codeSelect(content.lastIndexOf("i"), 1);
		IJavaElement i= elems[0];
		String lab= getBindingLabel(i, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED);
		assertLinkMatch(lab, "{{org.test.C}}.{{org.test.C|c}}.() -> {...} {{IntConsumer}}.{{java.util.function.IntConsumer|accept}}(int).i");

		IJavaElement lambdaMethod= i.getParent();
		lab= getBindingLabel(lambdaMethod, JavaElementLabels.T_FULLY_QUALIFIED
				| JavaElementLabels.M_POST_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES);
		assertLinkMatch(lab, "accept(int i) - {{org.test.C}}.{{org.test.C|c}}.() -> {...} {{java.util.function|IntConsumer}}");
		
		IJavaElement lambdaType= lambdaMethod.getParent();
		lab= getBindingLabel(lambdaType, JavaElementLabels.T_POST_QUALIFIED);
// Bindings don't have the split identity of a lambda as expected from JavaElementLabelsTest18
		assertLinkMatch(lab, "accept(...) - {{org.test.C}}.{{org.test.C|c}}.() -> {...} {{java.util.function|IntConsumer}}");
//		assertLinkMatch(lab, "() -> {...} IntConsumer - org.test.C.c");
	}

	public void testTypeLabelLambda2() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("public class C {\n");
		buf.append("    Consumer<String> c = (s) -> { };\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", content, false, null);

		IJavaElement[] elems= cu.codeSelect(content.lastIndexOf("s"), 1);
		IJavaElement i= elems[0];
		String lab= getBindingLabel(i, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED);
		assertLinkMatch(lab, "{{org.test.C}}.{{org.test.C|c}}.() -> {...} {{Consumer}}.{{java.util.function.Consumer|accept}}({{java.util.function.Consumer|T}}).s");

		lab= getBindingLabel(i, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "{{org.test.C}}.{{org.test.C|c}}.() -> {...} {{Consumer}}.{{java.util.function.Consumer<java.lang.String>|accept}}({{java.lang|String}}).s");

		IJavaElement lambdaMethod= i.getParent();
		lab= getBindingLabel(lambdaMethod, JavaElementLabels.T_FULLY_QUALIFIED
				| JavaElementLabels.M_POST_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES);
		assertLinkMatch(lab, "accept({{java.util.function.Consumer|T}} arg0) - {{org.test.C}}.{{org.test.C|c}}.() -> {...} {{java.util.function|Consumer}}");
		
		IJavaElement lambdaType= lambdaMethod.getParent();
		lab= getBindingLabel(lambdaType, JavaElementLabels.T_POST_QUALIFIED);
		assertLinkMatch(lab, "accept(...) - {{org.test.C}}.{{org.test.C|c}}.() -> {...} {{java.util.function|Consumer}}");
	}

	public void testAnonymousClassInLambda1() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("public class C {\n");
		buf.append("    Consumer<String> c = (s) -> {\n");
		buf.append("    	new Thread() { public void run() { } }.start();\n");
		buf.append("    };\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", content, false, null);

		IJavaElement thread= cu.getElementAt(content.lastIndexOf("Thread"));
		String lab= getBindingLabel(thread, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED);
		assertLinkMatch(lab, "{{org.test.C}}.{{org.test.C|c}}.() -> {...}.new Thread() {...}");

		lab= getBindingLabel(thread, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_POST_QUALIFIED);
		assertLinkMatch(lab, "new Thread() {...} - {{org.test.C}}.{{org.test.C|c}}.() -> {...}");
	}

	public void testLambdaInAnonymousClass1() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("public class C {\n");
		buf.append("    Thread t= new Thread() {\n");
		buf.append("    	public void run() {\n");
		buf.append("    		Consumer<String> c = (s) -> { };\n");
		buf.append("    	}\n");
		buf.append("    };\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", content, false, null);

		IJavaElement[] elems= cu.codeSelect(content.lastIndexOf("s)"), 1);
		IJavaElement thread= elems[0];
		String lab= getBindingLabel(thread, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.USE_RESOLVED);
		assertLinkMatch(lab, "{{org.test.C}}.{{org.test.C|t}}.{{org.test.C.t|new Thread() {...}}}.{{org.test.C.t.new Thread() {...}|run}}()." + 
								"() -> {...} {{java.util.function|Consumer}}.{{java.util.function.Consumer<java.lang.String>|accept}}({{java.lang|String}}).s");

		lab= getBindingLabel(thread, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.F_PRE_TYPE_SIGNATURE | JavaElementLabels.F_POST_QUALIFIED);
		assertLinkMatch(lab, "{{java.lang|String}} s - {{org.test.C}}.{{org.test.C|t}}.{{org.test.C.t|new Thread() {...}}}.{{org.test.C.t.new Thread() {...}|run}}()." + 
								"() -> {...} {{java.util.function|Consumer}}.{{java.util.function.Consumer|accept}}({{java.util.function.Consumer|T}})");
	}

	public void testLambdaInInstanceInitializer() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.util.function.Consumer;\n");
		buf.append("public class C {\n");
		buf.append("    {\n");
		buf.append("    	Consumer<String> c = (s) -> { System.out.print(s.toUpperCase()); };\n");
		buf.append("    };\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", content, false, null);

		IJavaElement[] elems= cu.codeSelect(content.lastIndexOf("s.toUpperCase"), 1);
		IJavaElement thread= elems[0];
		String lab= getBindingLabel(thread, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.USE_RESOLVED | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
		assertLinkMatch(lab, "{{java.lang|String}} {{org.test.C}}.{...}.() -> {...} {{java.util.function|Consumer}}.{{java.util.function.Consumer<java.lang.String>|accept}}({{java.lang|String}}).s");

		lab= getBindingLabel(thread, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.F_PRE_TYPE_SIGNATURE | JavaElementLabels.F_POST_QUALIFIED);
		assertLinkMatch(lab, "{{java.lang|String}} s - {{org.test.C}}.{...}.() -> {...} {{java.util.function|Consumer}}.{{java.util.function.Consumer|accept}}({{java.util.function.Consumer|T}})");
	}

	public void testAnnotatedArrayDimension1() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.lang.annotation.*;\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE) @interface TAnn {}\n");
		buf.append("\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    void test (String [] @TAnn[] argss) {\n");
		buf.append("    	String[] args = argss[0];\n");
		buf.append("    };\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", content, false, null);

		IJavaElement[] elems= cu.codeSelect(content.lastIndexOf("argss[0]"), "argss".length());
		IJavaElement thread= elems[0];
		String lab= getBindingLabel(thread, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.USE_RESOLVED | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
		assertLinkMatch(lab, "{{java.lang|String}}[] @{{org.test|TAnn}}[] {{org.test.C|test}}({{java.lang|String}}[] @{{org.test|TAnn}}[]).argss");

		lab= getBindingLabel(thread, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.F_PRE_TYPE_SIGNATURE | JavaElementLabels.F_POST_QUALIFIED);
		assertLinkMatch(lab, "{{java.lang|String}}[] @{{org.test|TAnn}}[] argss - {{org.test.C|test}}({{java.lang|String}}[][])");
	}

	public void testAnnotatedArrayDimension2() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("org.test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.test;\n");
		buf.append("import java.lang.annotation.*;\n");
		buf.append("\n");
		buf.append("@Target(ElementType.TYPE_USE) @interface TA1 {}\n");
		buf.append("@Target(ElementType.TYPE_USE) @interface TA2 {}\n");
		buf.append("@Target(ElementType.TYPE_USE) @interface TA3 {}\n");
		buf.append("\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    void test (@TA1 String @TA2[] @TA3[] argss) {\n");
		buf.append("    	String[] args = argss[0];\n");
		buf.append("    };\n");
		buf.append("}\n");
		String content= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", content, false, null);

		IJavaElement[] elems= cu.codeSelect(content.lastIndexOf("argss[0]"), "argss".length());
		IJavaElement thread= elems[0];
		String lab= getBindingLabel(thread, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.USE_RESOLVED | JavaElementLabels.F_PRE_TYPE_SIGNATURE);
		assertLinkMatch(lab, "@{{org.test|TA1}} {{java.lang|String}} @{{org.test|TA2}}[] @{{org.test|TA3}}[] {{org.test.C|test}}(@{{org.test|TA1}} {{java.lang|String}} @{{org.test|TA2}}[] @{{org.test|TA3}}[]).argss");

		lab= getBindingLabel(thread, JavaElementLabels.ALL_DEFAULT | JavaElementLabels.F_PRE_TYPE_SIGNATURE | JavaElementLabels.F_POST_QUALIFIED);
		assertLinkMatch(lab, "@{{org.test|TA1}} {{java.lang|String}} @{{org.test|TA2}}[] @{{org.test|TA3}}[] argss - {{org.test.C|test}}({{java.lang|String}}[][])");
	}
}
