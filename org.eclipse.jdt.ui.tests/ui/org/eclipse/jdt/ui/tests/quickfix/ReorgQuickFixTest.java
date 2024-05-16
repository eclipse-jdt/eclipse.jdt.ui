/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ltk.core.refactoring.PerformChangeOperation;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.ClasspathFixProcessor.ClasspathFixProposal;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.ClasspathFixProcessorDescriptor;
import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CorrectMainTypeNameProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.CorrectPackageDeclarationProposal;

public class ReorgQuickFixTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.ERROR);
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testUnusedImports() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		Object p1= proposals.get(0);
		if (!(p1 instanceof CUCorrectionProposal)) {
			p1= proposals.get(1);
		}

		CUCorrectionProposal proposal= (CUCorrectionProposal) p1;
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnusedImportsInDefaultPackage() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		String str= """
			import java.util.Vector;
			public class E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		Object p1= proposals.get(0);
		if (!(p1 instanceof CUCorrectionProposal)) {
			p1= proposals.get(1);
		}

		CUCorrectionProposal proposal= (CUCorrectionProposal) p1;
		String preview= getPreviewContent(proposal);

		String str1= """
			
			public class E {
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testUnusedImportOnDemand() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			import java.util.Vector;
			import java.net.*;
			
			public class E {
			    Vector v;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		Object p1= proposals.get(0);
		if (!(p1 instanceof CUCorrectionProposal)) {
			p1= proposals.get(1);
		}

		CUCorrectionProposal proposal= (CUCorrectionProposal) p1;
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			
			import java.util.Vector;
			
			public class E {
			    Vector v;
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testCollidingImports() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			import java.util.Date;
			import java.sql.Date;
			import java.util.Vector;
			
			public class E {
			    Date d;
			    Vector v;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		Object p1= proposals.get(0);
		if (!(p1 instanceof CUCorrectionProposal)) {
			p1= proposals.get(1);
		}

		CUCorrectionProposal proposal= (CUCorrectionProposal) p1;
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			
			import java.util.Date;
			import java.util.Vector;
			
			public class E {
			    Date d;
			    Vector v;
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testWrongPackageStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test2;
			
			public class E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean hasRename= true, hasMove= true;

		for (IJavaCompletionProposal proposal2 : proposals) {
			ChangeCorrectionProposal curr= (ChangeCorrectionProposal) proposal2;
			if (curr instanceof CorrectPackageDeclarationProposal) {
				assertTrue("Duplicated proposal", hasRename);
				hasRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) curr;
				String preview= getPreviewContent(proposal);
				String str1= """
					package test1;
					
					public class E {
					}
					""";
				assertEqualString(preview, str1);
			} else {
				assertTrue("Duplicated proposal", hasMove);
				hasMove= false;
				curr.apply(null);

				IPackageFragment pack2= fSourceFolder.getPackageFragment("test2");
				ICompilationUnit cu2= pack2.getCompilationUnit("E.java");
				assertTrue("CU does not exist", cu2.exists());
				String str2= """
					package test2;
					
					public class E {
					}
					""";
				assertEqualStringIgnoreDelim(cu2.getSource(), str2);
			}
		}
	}

	@Test
	public void testWrongPackageStatementInEnum() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test2;
			
			public enum E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean hasRename= true, hasMove= true;

		for (IJavaCompletionProposal proposal2 : proposals) {
			ChangeCorrectionProposal curr= (ChangeCorrectionProposal) proposal2;
			if (curr instanceof CorrectPackageDeclarationProposal) {
				assertTrue("Duplicated proposal", hasRename);
				hasRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) curr;
				String preview= getPreviewContent(proposal);
				String str1= """
					package test1;
					
					public enum E {
					}
					""";
				assertEqualString(preview, str1);
			} else {
				assertTrue("Duplicated proposal", hasMove);
				hasMove= false;
				curr.apply(null);

				IPackageFragment pack2= fSourceFolder.getPackageFragment("test2");
				ICompilationUnit cu2= pack2.getCompilationUnit("E.java");
				assertTrue("CU does not exist", cu2.exists());
				String str2= """
					package test2;
					
					public enum E {
					}
					""";
				assertEqualStringIgnoreDelim(cu2.getSource(), str2);
			}
		}
	}

	@Test
	public void testWrongPackageStatementFromDefault() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		String str= """
			package test2;
			
			public class E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean hasRename= true, hasMove= true;

		for (IJavaCompletionProposal proposal2 : proposals) {
			ChangeCorrectionProposal curr= (ChangeCorrectionProposal) proposal2;
			if (curr instanceof CorrectPackageDeclarationProposal) {
				assertTrue("Duplicated proposal", hasRename);
				hasRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) curr;
				String preview= getPreviewContent(proposal);
				String str1= """
					
					
					public class E {
					}
					""";
				assertEqualString(preview, str1);
			} else {
				assertTrue("Duplicated proposal", hasMove);
				hasMove= false;
				curr.apply(null);

				IPackageFragment pack2= fSourceFolder.getPackageFragment("test2");
				ICompilationUnit cu2= pack2.getCompilationUnit("E.java");
				assertTrue("CU does not exist", cu2.exists());
				String str2= """
					package test2;
					
					public class E {
					}
					""";
				assertEqualStringIgnoreDelim(cu2.getSource(), str2);
			}
		}
	}

	@Test
	public void testWrongDefaultPackageStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test2", false, null);
		String str= """
			public class E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean hasRename= true, hasMove= true;

		for (IJavaCompletionProposal proposal2 : proposals) {
			ChangeCorrectionProposal curr= (ChangeCorrectionProposal) proposal2;
			if (curr instanceof CorrectPackageDeclarationProposal) {
				assertTrue("Duplicated proposal", hasRename);
				hasRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) curr;
				String preview= getPreviewContent(proposal);
				String str1= """
					package test2;
					
					public class E {
					}
					""";
				assertEqualString(preview, str1);
			} else {
				assertTrue("Duplicated proposal", hasMove);
				hasMove= false;
				curr.apply(null);

				IPackageFragment pack2= fSourceFolder.getPackageFragment("");
				ICompilationUnit cu2= pack2.getCompilationUnit("E.java");
				assertTrue("CU does not exist", cu2.exists());
				String str2= """
					public class E {
					}
					""";
				assertEqualStringIgnoreDelim(cu2.getSource(), str2);
			}
		}
	}

	@Test
	public void testWrongPackageStatementButColliding() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf.append("package test2;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		String str= """
			package test1;
			
			public class E {
			}
			""";
		assertEqualString(preview, str);
	}

	@Test
	public void testWrongTypeName() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			public class E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean hasRename= true, hasMove= true;

		for (IJavaCompletionProposal proposal2 : proposals) {
			ChangeCorrectionProposal curr= (ChangeCorrectionProposal) proposal2;
			if (curr instanceof CorrectMainTypeNameProposal) {
				assertTrue("Duplicated proposal", hasRename);
				hasRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) curr;
				String preview= getPreviewContent(proposal);
				String str1= """
					package test1;
					
					public class X {
					}
					""";
				assertEqualString(preview, str1);
			} else {
				assertTrue("Duplicated proposal", hasMove);
				hasMove= false;
				curr.apply(null);

				ICompilationUnit cu2= pack1.getCompilationUnit("E.java");
				assertTrue("CU does not exist", cu2.exists());
				String str2= """
					package test1;
					
					public class E {
					}
					""";
				assertEqualStringIgnoreDelim(cu2.getSource(), str2);
			}
		}
	}

	@Test
	public void testWrongTypeName_bug180330() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			public class \\u0042 {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		boolean hasRename= true, hasMove= true;

		for (IJavaCompletionProposal proposal2 : proposals) {
			ChangeCorrectionProposal curr= (ChangeCorrectionProposal) proposal2;
			if (curr instanceof CorrectMainTypeNameProposal) {
				assertTrue("Duplicated proposal", hasRename);
				hasRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) curr;
				String preview= getPreviewContent(proposal);
				String str1= """
					package p;
					public class C {
					}
					""";
				assertEqualString(preview, str1);
			} else {
				assertTrue("Duplicated proposal", hasMove);
				hasMove= false;
				curr.apply(null);

				ICompilationUnit cu2= pack1.getCompilationUnit("B.java");
				assertTrue("CU does not exist", cu2.exists());
				String str2= """
					package p;
					public class \\u0042 {
					}
					""";
				assertEqualStringIgnoreDelim(cu2.getSource(), str2);
			}
		}
	}

	@Test
	public void testWrongTypeNameButColliding() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		String preview= getPreviewContent(proposal);
		String str= """
			package test1;
			
			public class E {
			}
			""";
		assertEqualString(preview, str);
	}

	@Test
	public void testWrongTypeNameWithConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("    public X() {\n");
		buf.append("        X other;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		String preview= getPreviewContent(proposal);
		String str= """
			package test1;
			
			public class E {
			    public E() {
			        E other;
			    }
			}
			""";
		assertEqualString(preview, str);
	}

	@Test
	public void testWrongTypeNameInEnum() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public enum X {\n");
		buf.append("    A;\n");
		buf.append("    X() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class X {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		String preview= getPreviewContent(proposal);
		String str= """
			package test1;
			
			public enum E {
			    A;
			    E() {
			    }
			}
			""";
		assertEqualString(preview, str);
	}

	@Test
	public void testWrongTypeNameInAnnot() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public @interface X {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public @interface X {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);

		String preview= getPreviewContent(proposal);
		String str= """
			package test1;
			
			public @interface E {
			}
			""";
		assertEqualString(preview, str);
	}

	@Test
	public void testTodoTasks1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        // TODO: XXX
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "TODO: XXX";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		ProblemLocation problem= new ProblemLocation(str1.indexOf(str), str.length(), IProblem.Task, new String[0], true, IJavaModelMarker.TASK_MARKER);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(context, problem);



		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testTodoTasks2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        // Some other text TODO: XXX
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "TODO: XXX";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		ProblemLocation problem= new ProblemLocation(str1.indexOf(str), str.length(), IProblem.Task, new String[0], true, IJavaModelMarker.TASK_MARKER);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(context, problem);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        // Some other text\s
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testTodoTasks3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        /* TODO: XXX */
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "TODO: XXX";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		ProblemLocation problem= new ProblemLocation(str1.indexOf(str), str.length(), IProblem.Task, new String[0], true, IJavaModelMarker.TASK_MARKER);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(context, problem);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testTodoTasks4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        /**
			        TODO: XXX
			        */
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "TODO: XXX";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		ProblemLocation problem= new ProblemLocation(str1.indexOf(str), str.length(), IProblem.Task, new String[0], true, IJavaModelMarker.TASK_MARKER);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(context, problem);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testTodoTasks5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        /**
			        Some other text: TODO: XXX
			        */
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "TODO: XXX";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		ProblemLocation problem= new ProblemLocation(str1.indexOf(str), str.length(), IProblem.Task, new String[0], true, IJavaModelMarker.TASK_MARKER);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(context, problem);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        /**
			        Some other text:\s
			        */
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testTodoTasks6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        ;// TODO: XXX
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "TODO: XXX";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		ProblemLocation problem= new ProblemLocation(str1.indexOf(str), str.length(), IProblem.Task, new String[0], true, IJavaModelMarker.TASK_MARKER);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(context, problem);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        ;
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testTodoTasks7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        /* TODO: XXX*/;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "TODO: XXX";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		ProblemLocation problem= new ProblemLocation(str1.indexOf(str), str.length(), IProblem.Task, new String[0], true, IJavaModelMarker.TASK_MARKER);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(context, problem);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        ;
			    }
			}
			""";
		assertEqualString(preview, str2);
	}


	@Test
	public void testAddToClasspathSourceFolder() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import mylib.Foo;
			public class E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		IClasspathEntry[] prevClasspath= cu.getJavaProject().getRawClasspath();

		IJavaProject otherProject= JavaProjectHelper.createJavaProject("other", "bin");
		try {
			IPackageFragmentRoot otherRoot= JavaProjectHelper.addSourceContainer(otherProject, "src");
			IPackageFragment otherPack= otherRoot.createPackageFragment("mylib", false, null);
			String str1= """
				package mylib;
				public class Foo {
				}
				""";
			otherPack.createCompilationUnit("Foo.java", str1, false, null);

			MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.OK, "", null);
			ClasspathFixProposal[] proposals= ClasspathFixProcessorDescriptor.getProposals(cu.getJavaProject(), "mylib.Foo", status);
			assertEquals(1, proposals.length);
			assertTrue(status.isOK());

			assertAddedClassPathEntry(proposals[0], otherProject.getPath(), cu, prevClasspath);

		} finally {
			JavaProjectHelper.delete(otherProject);
		}
	}

	@Test
	public void testAddToClasspathIntJAR() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import mylib.Foo;
			public class E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		IClasspathEntry[] prevClasspath= cu.getJavaProject().getRawClasspath();

		IJavaProject otherProject= JavaProjectHelper.createJavaProject("other", "bin");
		try {
			File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB);
			assertNotNull("lib does not exist", lib);
			assertTrue("lib does not exist", lib.exists());
			IPackageFragmentRoot otherRoot= JavaProjectHelper.addLibraryWithImport(otherProject, Path.fromOSString(lib.getPath()), null, null);

			MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.OK, "", null);
			ClasspathFixProposal[] proposals= ClasspathFixProcessorDescriptor.getProposals(cu.getJavaProject(), "mylib.Foo", status);
			assertEquals(1, proposals.length);
			assertTrue(status.isOK());

			assertAddedClassPathEntry(proposals[0], otherRoot.getPath(), cu, prevClasspath);
		} finally {
			JavaProjectHelper.delete(otherProject);
		}
	}

	private void assertAddedClassPathEntry(ClasspathFixProposal curr, IPath addedPath, ICompilationUnit cu, IClasspathEntry[] prevClasspath) throws CoreException {
		new PerformChangeOperation(curr.createChange(null)).run(null);

		IClasspathEntry[] newClasspath= cu.getJavaProject().getRawClasspath();
		assertEquals(prevClasspath.length + 1, newClasspath.length);
		assertEquals(addedPath, newClasspath[prevClasspath.length].getPath());
	}


	@Test
	public void testAddToClasspathExportedExtJAR() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import mylib.Foo;
			public class E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		IClasspathEntry[] prevClasspath= cu.getJavaProject().getRawClasspath();

		IJavaProject otherProject= JavaProjectHelper.createJavaProject("other", "bin");
		try {
			File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB);
			assertNotNull("lib does not exist", lib);
			assertTrue("lib does not exist", lib.exists());

			IPath path= Path.fromOSString(lib.getPath());

			// exported external JAR
			IClasspathEntry entry= JavaCore.newLibraryEntry(path, null, null, true);
			JavaProjectHelper.addToClasspath(otherProject, entry);

			MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.OK, "", null);
			ClasspathFixProposal[] proposals= ClasspathFixProcessorDescriptor.getProposals(cu.getJavaProject(), "mylib.Foo", status);
			assertEquals(2, proposals.length);
			assertTrue(status.isOK());

			assertAddedClassPathEntry(proposals[0], otherProject.getPath(), cu, prevClasspath);
			assertAddedClassPathEntry(proposals[1], path, cu, prevClasspath);

		} finally {
			JavaProjectHelper.delete(otherProject);
		}
	}

	@Test
	public void testAddToClasspathContainer() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import mylib.Foo;
			public class E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);
		IClasspathEntry[] prevClasspath= cu.getJavaProject().getRawClasspath();

		IJavaProject otherProject= JavaProjectHelper.createJavaProject("other", "bin");
		try {
			File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.MYLIB);
			assertNotNull("lib does not exist", lib);
			assertTrue("lib does not exist", lib.exists());
			IPath path= Path.fromOSString(lib.getPath());
			final IClasspathEntry[] entries= { JavaCore.newLibraryEntry(path, null, null) };
			final IPath containerPath= new Path(JavaCore.USER_LIBRARY_CONTAINER_ID).append("MyUserLibrary");


			IClasspathContainer newContainer= new IClasspathContainer() {
				@Override
				public IClasspathEntry[] getClasspathEntries() {
					return entries;
				}

				@Override
				public String getDescription() {
					return "MyUserLibrary";
				}

				@Override
				public int getKind() {
					return IClasspathContainer.K_APPLICATION;
				}

				@Override
				public IPath getPath() {
					return containerPath;
				}
			};
			ClasspathContainerInitializer initializer= JavaCore.getClasspathContainerInitializer(JavaCore.USER_LIBRARY_CONTAINER_ID);
			initializer.requestClasspathContainerUpdate(containerPath, otherProject, newContainer);

			IClasspathEntry entry= JavaCore.newContainerEntry(containerPath);
			JavaProjectHelper.addToClasspath(otherProject, entry);

			MultiStatus status= new MultiStatus(JavaUI.ID_PLUGIN, IStatus.OK, "", null);
			ClasspathFixProposal[] proposals= ClasspathFixProcessorDescriptor.getProposals(cu.getJavaProject(), "mylib.Foo", status);
			assertEquals(1, proposals.length);
			assertTrue(status.isOK());

			assertAddedClassPathEntry(proposals[0], containerPath, cu, prevClasspath);
		} finally {
			JavaProjectHelper.delete(otherProject);
		}
	}




}
