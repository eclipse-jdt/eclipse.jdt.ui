/*******************************************************************************
 * Copyright (c) 2026 Microsoft Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Microsoft Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.junit.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.After;
import org.junit.Test;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.ui.tests.quickfix.JarUtil;

import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;

public class JUnitStandaloneDetectionTest {

	private static final String[] JUNIT_JUPITER_API_STUBS= {
			"org/junit/jupiter/api/Test.java",
			"""
			package org.junit.jupiter.api;

			public @interface Test {
			}
			""",
			"org/junit/jupiter/api/TestTemplate.java",
			"""
			package org.junit.jupiter.api;

			public @interface TestTemplate {
			}
			""",
			"org/junit/jupiter/api/ClassTemplate.java",
			"""
			package org.junit.jupiter.api;

			public @interface ClassTemplate {
			}
			""",
			"org/junit/platform/commons/annotation/Testable.java",
			"""
			package org.junit.platform.commons.annotation;

			public @interface Testable {
			}
			"""
	};

	private IJavaProject javaProject;

	@After
	public void tearDown() throws Exception {
		if (javaProject != null) {
			JavaProjectHelper.delete(javaProject);
		}
	}

	@Test
	public void testDetectJUnit5FromPlatformConsoleStandaloneJar() throws Exception {
		javaProject= createProjectWithStandaloneJar("junit-platform-console-standalone-1.13.4.jar", "1.13.4", "5.13.4");

		assertThat(CoreTestSearchEngine.hasJUnit5TestAnnotation(javaProject)).isTrue();
		assertThat(CoreTestSearchEngine.hasJUnit6TestAnnotation(javaProject)).isFalse();
	}

	@Test
	public void testDetectJUnit6FromPlatformConsoleStandaloneJar() throws Exception {
		javaProject= createProjectWithStandaloneJar("junit-platform-console-standalone-6.0.3.jar", "6.0.3", "6.0.3");

		assertThat(CoreTestSearchEngine.hasJUnit5TestAnnotation(javaProject)).isFalse();
		assertThat(CoreTestSearchEngine.hasJUnit6TestAnnotation(javaProject)).isTrue();
	}

	private static IJavaProject createProjectWithStandaloneJar(String jarName, String specificationVersion, String jupiterVersion) throws Exception {
		IJavaProject project= JavaProjectHelper.createJavaProject("JUnitStandaloneDetectionTest", "bin");
		JavaProjectHelper.addRTJar_17(project, false);
		File jar= new File(project.getProject().getLocation().toFile(), jarName);
		String rtStubs= JavaProjectHelper.findRtJar(JavaProjectHelper.RT_STUBS17)[0].toOSString();
		JarUtil.createJar(JUNIT_JUPITER_API_STUBS, new String[] {
				"META-INF/MANIFEST.MF",
				"""
				Manifest-Version: 1.0
				Automatic-Module-Name: org.junit.platform.console.standalone
				Multi-Release: true
				Specification-Version: %s
				Engine-Version-junit-jupiter: %s

				""".formatted(specificationVersion, jupiterVersion)
		}, jar.getAbsolutePath(), new String[] { rtStubs }, "17", null, null);
		JavaProjectHelper.addLibrary(project, Path.fromOSString(jar.getAbsolutePath()));
		return project;
	}
}
