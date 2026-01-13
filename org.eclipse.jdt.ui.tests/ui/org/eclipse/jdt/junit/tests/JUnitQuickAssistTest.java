/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.jdt.junit.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.tests.quickfix.QuickFixTest;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;

/**
 * Tests for JUnit Quick Assist Processor that adds/removes @Disabled and @Ignore annotations.
 */
public class JUnitQuickAssistTest extends QuickFixTest {

	@Rule
	public ProjectTestSetup projectSetup = new Java1d8ProjectTestSetup();

	private IJavaProject fJProject;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		fJProject = projectSetup.getProject();
		fSourceFolder = JavaProjectHelper.addSourceContainer(fJProject, "src");

		JavaProjectHelper.addRTJar(fJProject);
		IClasspathEntry cpe= JavaCore.newContainerEntry(JUnitCore.JUNIT5_CONTAINER_PATH);
		JavaProjectHelper.addToClasspath(fJProject, cpe);
		JavaProjectHelper.set18CompilerOptions(fJProject);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testAddDisabledAnnotationToJUnit5Test() throws Exception {
		// Test adding @Disabled to a JUnit 5 test
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		String original = """
			package test1;

			import org.junit.jupiter.api.Test;

			public class MyTest {
			    @Test
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", original, false, null);
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(cu);
		String str = "testMethod";
		AssistContext context = getCorrectionContext(cu, original.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals = collectAssists(context, false);

		// Should have at least one proposal
		assertTrue("Should have proposals", proposals.size() > 0);

		// Find the "Disable test with @Disabled" proposal
		IJavaCompletionProposal disableProposal = findProposalByName(proposals, "Disable test with @Disabled");
		assertNotNull("Should have 'Disable test with @Disabled' proposal", disableProposal);

		IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
		// Apply the proposal
		disableProposal.apply(document);

		String expected = """
package test1;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class MyTest {
    @Disabled
	@Test
    public void testMethod() {
        // test code
    }
}
""";

		assertEqualString(cu.getSource(), expected);
	}

	@Test
	public void testRemoveDisabledAnnotationFromJUnit5Test() throws Exception {
		// Test removing @Disabled from a JUnit 5 test
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		String original = """
			package test1;

			import org.junit.jupiter.api.Disabled;
			import org.junit.jupiter.api.Test;

			public class MyTest {
			    @Disabled
			    @Test
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", original, false, null);

		String str = "testMethod";
		AssistContext context = getCorrectionContext(cu, original.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals = collectAssists(context, false);

		// Should have at least one proposal
		assertTrue("Should have proposals", proposals.size() > 0);

		// Find the "Enable test (remove @Disabled)" proposal
		IJavaCompletionProposal enableProposal = findProposalByName(proposals, "Enable test (remove @Disabled)");
		assertNotNull("Should have 'Enable test (remove @Disabled)' proposal", enableProposal);

		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(cu);
		IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
		// Apply the proposal
		enableProposal.apply(document);

		String expected = """
package test1;

import org.junit.jupiter.api.Test;

public class MyTest {
    @Test
    public void testMethod() {
        // test code
    }
}
""";

		assertEqualString(cu.getSource(), expected);
	}

	@Test
	public void testAddIgnoreAnnotationToJUnit4Test() throws Exception {
		// Test adding @Ignore to a JUnit 4 test
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		String original = """
			package test1;

			import org.junit.Test;

			public class MyTest {
			    @Test
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", original, false, null);

		String str = "testMethod";
		AssistContext context = getCorrectionContext(cu, original.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals = collectAssists(context, false);

		// Should have at least one proposal
		assertTrue("Should have proposals", proposals.size() > 0);

		// Find the "Disable test with @Ignore" proposal
		IJavaCompletionProposal disableProposal = findProposalByName(proposals, "Disable test with @Ignore");
		assertNotNull("Should have 'Disable test with @Ignore' proposal", disableProposal);

		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(cu);
		IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
		// Apply the proposal
		disableProposal.apply(document);

		String expected = """
			package test1;

			import org.junit.Ignore;
			import org.junit.Test;

			public class MyTest {
			    @Ignore
			    @Test
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		assertEqualString(cu.getSource(), expected);
	}

	@Test
	public void testRemoveIgnoreAnnotationFromJUnit4Test() throws Exception {
		// Test removing @Ignore from a JUnit 4 test
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		String original = """
			package test1;

			import org.junit.Ignore;
			import org.junit.Test;

			public class MyTest {
			    @Ignore
			    @Test
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", original, false, null);

		String str = "testMethod";
		AssistContext context = getCorrectionContext(cu, original.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals = collectAssists(context, false);

		// Should have at least one proposal
		assertTrue("Should have proposals", proposals.size() > 0);

		// Find the "Enable test (remove @Ignore)" proposal
		IJavaCompletionProposal enableProposal = findProposalByName(proposals, "Enable test (remove @Ignore)");
		assertNotNull("Should have 'Enable test (remove @Ignore)' proposal", enableProposal);

		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(cu);
		IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
		// Apply the proposal
		enableProposal.apply(document);

		String expected = """
			package test1;

			import org.junit.Ignore;
			import org.junit.Test;

			public class MyTest {
			    @Test
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		assertEqualString(cu.getSource(), expected);
	}

	@Test
	public void testNoProposalForNonTestMethod() throws Exception {
		// Test that no proposal is offered for non-test methods
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		String original = """
			package test1;

			public class MyTest {
			    public void normalMethod() {
			        // not a test
			    }
			}
			""";

		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", original, false, null);

		String str = "normalMethod";
		AssistContext context = getCorrectionContext(cu, original.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals = collectAssists(context, false);

		// Should not have the disable/enable proposals
		IJavaCompletionProposal disableProposal = findProposalByName(proposals, "Disable test with @Disabled");
		IJavaCompletionProposal ignoreProposal = findProposalByName(proposals, "Disable test with @Ignore");

		assertEquals("Should not have 'Disable test with @Disabled' proposal for non-test method", null, disableProposal);
		assertEquals("Should not have 'Disable test with @Ignore' proposal for non-test method", null, ignoreProposal);
	}

	@Test
	public void testAddDisabledAnnotationToParameterizedTest() throws Exception {
		// Test adding @Disabled to a JUnit 5 parameterized test
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		String original = """
			package test1;

			import org.junit.jupiter.params.ParameterizedTest;
			import org.junit.jupiter.params.provider.ValueSource;

			public class MyTest {
			    @ParameterizedTest
			    @ValueSource(strings = {"test1", "test2"})
			    public void testMethod(String param) {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", original, false, null);

		String str = "testMethod";
		AssistContext context = getCorrectionContext(cu, original.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals = collectAssists(context, false);

		// Should have at least one proposal
		assertTrue("Should have proposals", proposals.size() > 0);

		// Find the "Disable test with @Disabled" proposal
		IJavaCompletionProposal disableProposal = findProposalByName(proposals, "Disable test with @Disabled");
		assertNotNull("Should have 'Disable test with @Disabled' proposal for parameterized test", disableProposal);

		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(cu);
		IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
		// Apply the proposal
		disableProposal.apply(document);

		String expected = """
			package test1;

			import org.junit.jupiter.api.Disabled;
			import org.junit.jupiter.params.ParameterizedTest;
			import org.junit.jupiter.params.provider.ValueSource;

			public class MyTest {
			    @Disabled
			    @ParameterizedTest
			    @ValueSource(strings = {"test1", "test2"})
			    public void testMethod(String param) {
			        // test code
			    }
			}
			""";

		assertEqualString(cu.getSource(), expected);
	}

	@Test
	public void testAddDisabledAnnotationToRepeatedTest() throws Exception {
		// Test adding @Disabled to a JUnit 5 repeated test
		IPackageFragment pack1 = fSourceFolder.createPackageFragment("test1", false, null);
		String original = """
			package test1;

			import org.junit.jupiter.api.RepeatedTest;

			public class MyTest {
			    @RepeatedTest(5)
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		ICompilationUnit cu = pack1.createCompilationUnit("MyTest.java", original, false, null);

		String str = "testMethod";
		AssistContext context = getCorrectionContext(cu, original.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals = collectAssists(context, false);

		// Should have at least one proposal
		assertTrue("Should have proposals", proposals.size() > 0);

		// Find the "Disable test with @Disabled" proposal
		IJavaCompletionProposal disableProposal = findProposalByName(proposals, "Disable test with @Disabled");
		assertNotNull("Should have 'Disable test with @Disabled' proposal for repeated test", disableProposal);

		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(cu);
		IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());

		// Apply the proposal
		disableProposal.apply(document);

		String expected = """
			package test1;

			import org.junit.jupiter.api.Disabled;
			import org.junit.jupiter.api.RepeatedTest;

			public class MyTest {
			    @Disabled
			    @RepeatedTest(5)
			    public void testMethod() {
			        // test code
			    }
			}
			""";

		assertEqualString(cu.getSource(), expected);
	}

	/**
	 * Helper method to find a proposal by its display name.
	 */
	private IJavaCompletionProposal findProposalByName(List<IJavaCompletionProposal> proposals, String name) {
		for (IJavaCompletionProposal proposal : proposals) {
			if (name.equals(proposal.getDisplayString())) {
				return proposal;
			}
		}
		return null;
	}
}
