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
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;

import junit.framework.Test;

public class ExtractClassTests18 extends ExtractClassTests {

	private static final String REFACTORING_PATH= "ExtractClass18/";

	public ExtractClassTests18(String name) {
		super(name);
	}

	public static Test suite() {
		return new Java18Setup(new NoSuperTestsSuite(ExtractClassTests18.class));
	}

	public static Test setUpTest(Test someTest) {
		return new Java18Setup(someTest);
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	/* Test that @NonNull annotations ARE created if no @NonNullByDefault is in effect for the target location */  
	public void testNoRedundantNonNull1() throws Exception {
		IJavaProject javaProject= getRoot().getJavaProject();
		Map<String, String> originalOptions= javaProject.getOptions(false);
		try {
			Hashtable<String, String> newOptions= new Hashtable<>(originalOptions);
			newOptions.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
			javaProject.setOptions(newOptions);
			JavaProjectHelper.addLibrary(javaProject, new Path(Java18ProjectTestSetup.getJdtAnnotations20Path()));

			fDescriptor.setType(setupType());
			fDescriptor.setCreateGetterSetter(true);
			fDescriptor.setClassName("NoRedundantNonNull1Data");
			fDescriptor.setFieldName("data");
			fDescriptor.setCreateTopLevel(true);
			runRefactoring(false);
		} finally {
			javaProject.setOptions(originalOptions);
		}
	}

	/* Test that @NonNull annotations ARE NOT created if @NonNullByDefault is in effect for the target location */  
	public void testNoRedundantNonNull2() throws Exception {
		IJavaProject javaProject= getRoot().getJavaProject();
		Map<String, String> originalOptions= javaProject.getOptions(false);
		try {
			Hashtable<String, String> newOptions= new Hashtable<>(originalOptions);
			newOptions.put(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
			javaProject.setOptions(newOptions);
			JavaProjectHelper.addLibrary(javaProject, new Path(Java18ProjectTestSetup.getJdtAnnotations20Path()));

			fDescriptor.setType(setupType());
			fDescriptor.setCreateGetterSetter(true);
			fDescriptor.setClassName("NoRedundantNonNull2Data");
			fDescriptor.setFieldName("data");
			fDescriptor.setCreateTopLevel(false);
			runRefactoring(false);
		} finally {
			javaProject.setOptions(originalOptions);
		}
	}
}
