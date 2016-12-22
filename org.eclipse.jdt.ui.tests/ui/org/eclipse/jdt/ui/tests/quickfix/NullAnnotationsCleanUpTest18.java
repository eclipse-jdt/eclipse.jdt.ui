/*******************************************************************************
 * Copyright (c) 2016 Till Brychcy and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     TIll Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;

import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;

import org.eclipse.jdt.internal.ui.fix.NullAnnotationsCleanUp;

import junit.framework.Test;
import junit.framework.TestSuite;

public class NullAnnotationsCleanUpTest18 extends CleanUpTestCase {

	private static final Class<NullAnnotationsCleanUpTest18> THIS= NullAnnotationsCleanUpTest18.class;

	public NullAnnotationsCleanUpTest18(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	@Override
	protected IJavaProject getProject() {
		return Java18ProjectTestSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return Java18ProjectTestSetup.getDefaultClasspath();
	}

	public void testMoveTypeAnnotation() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=468457
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE) @interface X {}\n");
		buf.append("@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE) @interface Y {}\n");
		buf.append("public class Test {\n");
		buf.append("    void test(@X java.lang.@Y String param) {\n");
		buf.append("        @X @Y java.lang.String f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", original, false, null);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE) @interface X {}\n");
		buf.append("@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE) @interface Y {}\n");
		buf.append("public class Test {\n");
		buf.append("    void test(java.lang.@Y @X String param) {\n");
		buf.append("        java.lang.@X @Y String f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		ICleanUp[] cleanUps= { new NullAnnotationsCleanUp(new HashMap<>(), IProblem.TypeAnnotationAtQualifiedName) };
		performRefactoring(new CleanUpRefactoring(), new ICompilationUnit[] { cu1 }, cleanUps);

		assertEqualStringsIgnoreOrder(new String[] { cu1.getBuffer().getContents() }, new String[] { expected1 });
	}

}
