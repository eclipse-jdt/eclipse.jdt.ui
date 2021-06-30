/*******************************************************************************
 * Copyright (c) 2017 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.testplugin;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class NullTestUtils {
	// note: use disableAnnotationBasedNullAnalysis, if the project is reused between test cases
	public static void prepareNullDeclarationAnnotations(IPackageFragmentRoot sourceFolder) throws CoreException {
		Map<String, String> options= sourceFolder.getJavaProject().getOptions(true);
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_NONNULL_ANNOTATION_NAME, "annots.NonNull");
		options.put(JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME, "annots.Nullable");
		options.put(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME, "annots.NonNullByDefault");
		sourceFolder.getJavaProject().setOptions(options);

		IPackageFragment pack0= sourceFolder.createPackageFragment("annots", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package annots;\n");
		buf.append("\n");
		buf.append("@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)\n");
		buf.append("public @interface NonNull {}\n");
		pack0.createCompilationUnit("NonNull.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package annots;\n");
		buf.append("\n");
		buf.append("@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)\n");
		buf.append("public @interface Nullable {}\n");
		pack0.createCompilationUnit("Nullable.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package annots;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.*;\n");
		buf.append("@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })\n");
		buf.append("@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)\n");
		buf.append("public @interface NonNullByDefault { boolean value() default true; }\n");
		pack0.createCompilationUnit("NonNullByDefault.java", buf.toString(), false, null);
	}

	// note: use disableAnnotationBasedNullAnalysis, if the project is reused between test cases
	public static void prepareNullTypeAnnotations(IPackageFragmentRoot sourceFolder) throws JavaModelException {
		IJavaProject project=sourceFolder.getJavaProject();
		Map<String, String> options= project.getOptions(false);
		options.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_NONNULL_ANNOTATION_NAME, "annots.NonNull");
		options.put(JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME, "annots.Nullable");
		options.put(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME, "annots.NonNullByDefault");
		project.setOptions(options);

		IPackageFragment pack0= sourceFolder.createPackageFragment("annots", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package annots;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.*;\n");
		buf.append("\n");
		buf.append("@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)\n");
		buf.append("@Target({ ElementType.TYPE_USE })\n");
		buf.append("public @interface NonNull {}\n");
		pack0.createCompilationUnit("NonNull.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package annots;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.*;\n");
		buf.append("\n");
		buf.append("@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)\n");
		buf.append("@Target({ ElementType.TYPE_USE })\n");
		buf.append("public @interface Nullable {}\n");
		pack0.createCompilationUnit("Nullable.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package annots;\n");
		buf.append("\n");
		buf.append("public enum DefaultLocation { PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS, TYPE_PARAMETER }\n");
		pack0.createCompilationUnit("DefaultLocation.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package annots;\n");
		buf.append("\n");
		buf.append("import java.lang.annotation.*;\n");
		buf.append("import static annots.DefaultLocation.*;\n");
		buf.append("\n");
		buf.append("@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)\n");
		buf.append("@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.LOCAL_VARIABLE })\n");
		buf.append("public @interface NonNullByDefault { DefaultLocation[] value() default {PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT}; }\n");
		pack0.createCompilationUnit("NonNullByDefault.java", buf.toString(), false, null);
	}

	// for test classes where the project is not deleted for each test case
	public static void disableAnnotationBasedNullAnalysis(IPackageFragmentRoot sourceFolder) {
		IJavaProject project= sourceFolder.getJavaProject();
		Map<String, String> options= project.getOptions(false);
		options.remove(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS);
		options.remove(JavaCore.COMPILER_NONNULL_ANNOTATION_NAME);
		options.remove(JavaCore.COMPILER_NULLABLE_ANNOTATION_NAME);
		options.remove(JavaCore.COMPILER_NONNULL_BY_DEFAULT_ANNOTATION_NAME);
		project.setOptions(options);
	}
}
