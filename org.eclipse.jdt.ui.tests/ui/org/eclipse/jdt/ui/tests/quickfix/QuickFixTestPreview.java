/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Map;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.tests.core.rules.Java16ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

public class QuickFixTestPreview extends QuickFixTest {

    @Rule
    public ProjectTestSetup projectsetup = new Java16ProjectTestSetup(true);

    private IJavaProject fJProject1;

    private IPackageFragmentRoot fSourceFolder;

	@After
	public void tearDown() throws Exception {
		if (fJProject1 != null) {
			JavaProjectHelper.delete(fJProject1);
		}
	}

	@Test
	public void testAddSealedMissingClassModifierProposal() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, true);

		Map<String, String> options= fJProject1.getOptions(false);
		options.put(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES, JavaCore.IGNORE);
		fJProject1.setOptions(options);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test= "" +
					"package test;\n" +
					"\n" +
					"public sealed class Shape permits Square {}\n" +
					"\n" +
					"class Square extends Shape {}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Shape.java",test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= "" +
						"package test;\n" +
						"\n" +
						"public sealed class Shape permits Square {}\n" +
						"\n" +
						"final class Square extends Shape {}\n";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= "" +
						"package test;\n" +
						"\n" +
						"public sealed class Shape permits Square {}\n" +
						"\n" +
						"non-sealed class Square extends Shape {}\n";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= "" +
						"package test;\n" +
						"\n" +
						"public sealed class Shape permits Square {}\n" +
						"\n" +
						"sealed class Square extends Shape {}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });

	}

	@Test
	public void testAddSealedMissingInterfaceModifierProposal() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		fJProject1.setRawClasspath(projectsetup.getDefaultClasspath(), null);
		JavaProjectHelper.set16CompilerOptions(fJProject1, true);

		Map<String, String> options= fJProject1.getOptions(false);
		options.put(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES, JavaCore.IGNORE);
		fJProject1.setOptions(options);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		String test = "" +
					"package test;\n" +
					"\n" +
					"public sealed interface Shape permits Square {}\n" +
					"\n" +
					"interface Square extends Shape {}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Shape.java", test, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= "" +
						"package test;\n" +
						"\n" +
						"public sealed interface Shape permits Square {}\n" +
						"\n" +
						"sealed interface Square extends Shape {}\n";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= "" +
						"package test;\n" +
						"\n" +
						"public sealed interface Shape permits Square {}\n" +
						"\n" +
						"non-sealed interface Square extends Shape {}\n";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}


}
