/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

import java.util.Hashtable;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;

import org.eclipse.jdt.ui.tests.core.rules.Java9ProjectTestSetup;

public class CodeFormatterTest9 extends CodeFormatterTest {

	@Rule
	public Java9ProjectTestSetup j9p= new Java9ProjectTestSetup();

	@Override
	@Before
	public void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRequiredProject(fJProject1, j9p.getProject());

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "999");
		options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_9);
		JavaCore.setOptions(options);
	}

	@Test
	public void testFormatModule() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String original= "module     pack { requires java   .something   ; }  \n ";
		ICompilationUnit cu= pack1.createCompilationUnit("module-info.java", original, false, null);

		String formatted= format(cu, 0, 0);

		String expected=
			"""
			module pack {
			    requires java.something;
			}
			""";
		assertEqualString(formatted, expected);
	}

	@Test
	public void testFormatModuleInWrongFile() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String original= "module     pack { requires java   .something   ; }  \n ";
		ICompilationUnit cu= pack1.createCompilationUnit("SomeClass.java", original, false, null);

		String formatted= format(cu, 0, 0);

		String expected= "module     pack { requires java   .something   ; }\n";
		assertEqualString(formatted, expected);
	}
}
