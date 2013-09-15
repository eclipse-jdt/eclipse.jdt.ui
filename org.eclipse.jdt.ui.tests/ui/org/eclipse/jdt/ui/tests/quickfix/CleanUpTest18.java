/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;

public class CleanUpTest18 extends CleanUpTestCase {

	private static final Class THIS= CleanUpTest18.class;

	public CleanUpTest18(String name) {
		super(name);
	}

	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}
	
	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	protected IJavaProject getProject() {
		return Java18ProjectTestSetup.getProject();
	}

	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return Java18ProjectTestSetup.getDefaultClasspath();
	}

	public void testConvertToLambda01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(){\n");
		buf.append("        Runnable r = new Runnable() {\n");
		buf.append("            @Override\n");
		buf.append("            public void run() {\n");
		buf.append("                System.out.println(\"do something\");\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    };\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.USE_LAMBDA);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(){\n");
		buf.append("        Runnable r = () -> System.out.println(\"do something\");\n");
		buf.append("    };\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

	public void testConvertToLambda02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(){\n");
		buf.append("        Runnable r1 = new Runnable() {\n");
		buf.append("            @Override\n");
		buf.append("            public void run() {\n");
		buf.append("                System.out.println(\"do something\");\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        Runnable r2 = new Runnable() {\n");
		buf.append("            @Override\n");
		buf.append("            public void run() {\n");
		buf.append("                System.out.println(\"do something\");\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    };\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		enable(CleanUpConstants.USE_LAMBDA);

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(){\n");
		buf.append("        Runnable r1 = () -> System.out.println(\"do something\");\n");
		buf.append("        Runnable r2 = () -> System.out.println(\"do something\");\n");
		buf.append("    };\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu1 }, new String[] { expected1 });
	}

}
