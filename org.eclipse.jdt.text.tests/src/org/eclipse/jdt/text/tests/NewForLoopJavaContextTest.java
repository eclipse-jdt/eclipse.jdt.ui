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
package org.eclipse.jdt.text.tests;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.template.java.JavaContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Tests the automatic bracket insertion feature of the CUEditor. Also tests
 * linked mode along the way.
 * 
 * @since 3.1
 */
public class NewForLoopJavaContextTest extends TestCase {

	private static final String PROJECT= "NewForLoopJavaContextTest";
	private static final String SRC= "src";
	private static final String CU_NAME= "A.java";

	private static final String CU_PREFIX=
		"package test;\n" +
		"\n" +
		"import java.io.Serializable;\n" +
		"import java.util.Collection;\n" +
		"import java.util.List;\n" +
		"\n" +
		"public class A<E extends Number> {\n" +
		"	private static class Inner {\n" +
		"	}\n" +
		"	\n" +
		"	private static abstract class Inner2<E> implements Iterable<E> {\n" +
		"	}\n" +
		"	\n" +
		"	private static abstract class Inner3 implements Iterable<Serializable> {\n" +
		"	}\n" +
		"	\n" +
		"	private static abstract class Inner4<T> implements Iterable<String> {\n" +
		"	}\n" +
		"	\n" +
		"	private static abstract class Transi1<T extends Collection> implements Iterable<T> {\n" +
		"	}\n" +
		"	\n" +
		"	private static abstract class Transi2<T extends List> extends Transi1<T> {\n" +
		"	}\n" +
		"	\n";
	private static final String CU_POSTFIX=
		" {\n" +
		"	\n" +
		"}\n" +
		"}\n";
	
	public static Test suite() {
		return new TestSuite(NewForLoopJavaContextTest.class);
	}
	
	private IJavaProject fProject;
	private ICompilationUnit fCU;

	protected void setUp() throws Exception {
		if (JavaCore.getPlugin() != null) {
			Hashtable options= JavaCore.getDefaultOptions();
			options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
			options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
//			options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_LENGTH, "4");
//			options.put(DefaultCodeFormatterConstants.FORMATTER_INDENTATION_SIZE, "4");
			JavaCore.setOptions(options);
		}
		setUpProject(JavaCore.VERSION_1_5);
	}
	
	private void setUpProject(String sourceLevel) throws CoreException, JavaModelException {
		fProject= JavaProjectHelper.createJavaProject(PROJECT, "bin");
		JavaProjectHelper.addRTJar(fProject);
		fProject.setOption(JavaCore.COMPILER_SOURCE, sourceLevel);
		IPackageFragmentRoot fragmentRoot= JavaProjectHelper.addSourceContainer(fProject, SRC);
		IPackageFragment fragment= fragmentRoot.createPackageFragment("test", true, new NullProgressMonitor());
		fCU= fragment.createCompilationUnit(CU_NAME, "", true, new NullProgressMonitor());
		fCU.becomeWorkingCopy(null);
	}

	protected void tearDown() throws Exception {
		fCU.discardWorkingCopy();
		JavaProjectHelper.delete(fProject);
		if (JavaCore.getPlugin() != null) {
			JavaCore.setOptions(JavaCore.getDefaultOptions());
		}
	}
	
	private Template getTemplate(String id) {
		TemplateStore store= JavaPlugin.getDefault().getTemplateStore();
		return store.getTemplateData(id).getTemplate();
	}
	
	private Template getForLoop() {
		return getTemplate("org.eclipse.jdt.ui.templates.for_iterable");
	}
	
	private String evaluateTemplateInMethod(String signature) throws BadLocationException, TemplateException, CoreException {
		fCU.getBuffer().setContents(CU_PREFIX + signature + CU_POSTFIX);
		int offset= CU_PREFIX.length() + signature.length() + 3;
		fCU.reconcile(ICompilationUnit.NO_AST, false, null, null);
		return JavaContext.evaluateTemplate(getForLoop(), fCU, offset);
	}
	
	public void testArray() throws Exception {
		String template= evaluateTemplateInMethod("void method(Number[] array)");
		assertEquals(
				"	for (Number number : array) {\n" +
				"		\n" +
				"	}", template);
	}
	
	public void testInnerArray() throws Exception {
		String template= evaluateTemplateInMethod("void array(Inner[] array)");
		assertEquals(
				"	for (Inner inner : array) {\n" +
				"		\n" +
				"	}", template);
	}
	

	public void testSuperList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<? super Number> list)");
		assertEquals(
				"	for (Object object : list) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testSuperArrayList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<? super Number[]> list)");
		assertEquals(
				"	for (Object object : list) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testMultiList() throws Exception {
		String template= evaluateTemplateInMethod("<T extends Comparable<T> & Iterable<? extends Number>> void method(List<T> list)");
		assertEquals(
				"	for (T t : list) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testInnerIterable() throws Exception {
		String template= evaluateTemplateInMethod("void method(Inner2<Number> inner)");
		assertEquals(
				"	for (Number number : inner) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testInnerIterable2() throws Exception {
		String template= evaluateTemplateInMethod("void method(Inner3 inner)");
		assertEquals(
				"	for (Serializable serializable : inner) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testInnerMixedParameters() throws Exception {
		String template= evaluateTemplateInMethod("void method(Inner4<Number> inner)");
		assertEquals(
				"	for (String string : inner) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testGenericList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<E> list)");
		assertEquals(
				"	for (E e : list) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testWildcardList() throws Exception {
		String template= evaluateTemplateInMethod("void method(Transi1<?> transi)");
		assertEquals(
				"	for (Collection collection : transi) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testConcreteList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<String> list)");
		assertEquals(
				"	for (String string : list) {\n" +
				"		\n" +
				"	}", template);
	}

	public void testUpperboundList() throws Exception {
		String template= evaluateTemplateInMethod("void method(List<? extends Number> list)");
		assertEquals(
				"	for (Number number : list) {\n" +
				"		\n" +
				"	}", template);
	}

}
