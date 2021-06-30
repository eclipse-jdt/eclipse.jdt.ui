/*******************************************************************************
 * Copyright (c) 2017, 2020 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     TIll Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;

import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;

import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.fix.NullAnnotationsCleanUp;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class NullAnnotationsCleanUpTest1d8 extends CleanUpTestCase {

	@Rule
    public ProjectTestSetup projectSetup = new Java1d8ProjectTestSetup();

	private String ANNOTATION_JAR_PATH;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		Hashtable<String, String> options= JavaCore.getOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_HASHCODE_METHOD, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_PB_NULL_SPECIFICATION_VIOLATION, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_NULL_REFERENCE, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_POTENTIAL_NULL_REFERENCE, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_ANNOTATION_INFERENCE_CONFLICT, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_NULL_CHECK, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_NULL_UNCHECKED_CONVERSION, JavaCore.WARNING);

		JavaCore.setOptions(options);

		if (this.ANNOTATION_JAR_PATH == null) {
			String version= "[2.0.0,3.0.0)"; // tests run at 1.8, need the "new" null annotations
			Bundle[] bundles= Platform.getBundles("org.eclipse.jdt.annotation", version);
			File bundleFile= FileLocator.getBundleFile(bundles[0]);
			if (bundleFile.isDirectory())
				this.ANNOTATION_JAR_PATH= bundleFile.getPath() + "/bin";
			else
				this.ANNOTATION_JAR_PATH= bundleFile.getPath();
		}
		JavaProjectHelper.addLibrary(getProject(), new Path(ANNOTATION_JAR_PATH));
		}

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testMoveTypeAnnotation() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=468457
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
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

		buf= new StringBuilder();
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
		performRefactoring(new CleanUpRefactoring(), new ICompilationUnit[] { cu1 }, cleanUps, null);

		assertEqualStringsIgnoreOrder(new String[] { cu1.getBuffer().getContents() }, new String[] { expected1 });
	}

	@Test
	public void testBug528222() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNullByDefault;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault\n");
		buf.append("abstract class Test {\n");
		buf.append("    abstract void f(String x);\n");
		buf.append("\n");
		buf.append("    void g() {\n");
		buf.append("        f(null);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    Object h() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("");
		String original= buf.toString();
		ICompilationUnit cu1= pack1.createCompilationUnit("Test.java", original, false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.annotation.NonNullByDefault;\n");
		buf.append("import org.eclipse.jdt.annotation.Nullable;\n");
		buf.append("\n");
		buf.append("@NonNullByDefault\n");
		buf.append("abstract class Test {\n");
		buf.append("    abstract void f(String x);\n");
		buf.append("\n");
		buf.append("    void g() {\n");
		buf.append("        f(null);\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    @Nullable\n");
		buf.append("    Object h() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("");
		String expected1= buf.toString();

		ICleanUp[] cleanUps= { new NullAnnotationsCleanUp(new HashMap<>(), IProblem.RequiredNonNullButProvidedNull) };
		performRefactoring(new CleanUpRefactoring(), new ICompilationUnit[] { cu1 }, cleanUps, null);

		assertEqualStringsIgnoreOrder(new String[] { cu1.getBuffer().getContents() }, new String[] { expected1 });
	}

}
