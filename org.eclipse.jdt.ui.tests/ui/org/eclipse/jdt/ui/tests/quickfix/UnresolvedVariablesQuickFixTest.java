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
 *     Rabea Gransberger <rgransberger@gmx.de> - [quick fix] Fix several visibility issues - https://bugs.eclipse.org/394692
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.proposals.NewVariableCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RenameNodeCorrectionProposal;

public class UnresolvedVariablesQuickFixTest extends QuickFixTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		StubUtility.setCodeTemplate(CodeTemplateContextType.NEWTYPE_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.TYPECOMMENT_ID, "", null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testVarInAssignment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        iter= vec.iterator();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    private Iterator iter;

			    void foo(Vector vec) {
			        iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        Iterator iter = vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec, Iterator iter) {
			        iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testVarAssingmentInIfBody() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        if (vec != null)
			            iter= vec.iterator();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    private Iterator iter;

			    void foo(Vector vec) {
			        if (vec != null)
			            iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        Iterator iter;
			        if (vec != null)
			            iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec, Iterator iter) {
			        if (vec != null)
			            iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        if (vec != null) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testVarAssingmentInThenBody() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        if (vec == null) {
			        } else
			            iter= vec.iterator();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    private Iterator iter;

			    void foo(Vector vec) {
			        if (vec == null) {
			        } else
			            iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        Iterator iter;
			        if (vec == null) {
			        } else
			            iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec, Iterator iter) {
			        if (vec == null) {
			        } else
			            iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector vec) {
			        if (vec == null) {
			        } else {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testVarInAssignmentWithGenerics() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<String> vec) {
			        iter= vec.iterator();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    private Iterator<String> iter;

			    void foo(Vector<String> vec) {
			        iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    void foo(Vector<String> vec) {
			        Iterator<String> iter = vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    void foo(Vector<String> vec, Iterator<String> iter) {
			        iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<String> vec) {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testVarAssignedByWildcard1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<?> vec) {
			        elem = vec.get(0);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<?> vec) {
			        Object elem = vec.get(0);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testVarAssignedByWildcard2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? super Number> vec) {
			        elem = vec.get(0);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? super Number> vec) {
			        Object elem = vec.get(0);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testVarAssignedByWildcard3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? extends Number> vec) {
			        elem = vec.get(0);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? extends Number> vec) {
			        Number elem = vec.get(0);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testVarAssignedToWildcard1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? super Number> vec) {
			        vec.add(elem);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? super Number> vec, Number elem) {
			        vec.add(elem);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testVarAssignedToWildcard2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? extends Number> vec) {
			        vec.add(elem);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<? extends Number> vec, Object elem) {
			        vec.add(elem);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testVarAssignedToWildcard3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<?> vec) {
			        vec.add(elem);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<?> vec, Object elem) {
			        vec.add(elem);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testVarAssingmentInIfBodyWithGenerics() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<String> vec) {
			        if (vec != null)
			            iter= vec.iterator();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    private Iterator<String> iter;

			    void foo(Vector<String> vec) {
			        if (vec != null)
			            iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    void foo(Vector<String> vec) {
			        Iterator<String> iter;
			        if (vec != null)
			            iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    void foo(Vector<String> vec, Iterator<String> iter) {
			        if (vec != null)
			            iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<String> vec) {
			        if (vec != null) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testVarAssingmentInThenBodyWithGenerics() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<String> vec) {
			        if (vec == null) {
			        } else
			            iter= vec.iterator();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    private Iterator<String> iter;

			    void foo(Vector<String> vec) {
			        if (vec == null) {
			        } else
			            iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    void foo(Vector<String> vec) {
			        Iterator<String> iter;
			        if (vec == null) {
			        } else
			            iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    void foo(Vector<String> vec, Iterator<String> iter) {
			        if (vec == null) {
			        } else
			            iter= vec.iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			import java.util.Vector;
			public class E {
			    void foo(Vector<String> vec) {
			        if (vec == null) {
			        } else {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}


	@Test
	public void testVarInVarArgs1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			import java.util.Arrays;
			public class E {
			    public void foo() {
			        Arrays.<Number>asList(x);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[4];
		expected[0]= """
			package pack;
			import java.util.Arrays;
			public class E {
			    private Number x;

			    public void foo() {
			        Arrays.<Number>asList(x);
			    }
			}
			""";

		expected[1]= """
			package pack;
			import java.util.Arrays;
			public class E {
			    private static final Number x = null;

			    public void foo() {
			        Arrays.<Number>asList(x);
			    }
			}
			""";

		expected[2]= """
			package pack;
			import java.util.Arrays;
			public class E {
			    public void foo(Number x) {
			        Arrays.<Number>asList(x);
			    }
			}
			""";

		expected[3]= """
			package pack;
			import java.util.Arrays;
			public class E {
			    public void foo() {
			        Number x;
			        Arrays.<Number>asList(x);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testVarInVarArgs2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			import java.io.File;
			import java.util.Arrays;
			public class E {
			    public void foo(String name) {
			        Arrays.<File>asList( new File(name), new XXX(name) );
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		expected[0]= """
			package pack;
			import java.io.File;
			import java.util.Arrays;
			public class E {
			    public void foo(String name) {
			        Arrays.<File>asList( new File(name), new File(name) );
			    }
			}
			""";

		expected[1]= """
			package pack;

			import java.io.File;

			public class XXX extends File {

			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testVarInForInitializer() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        for (i= 0;;) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private int i;

			    void foo() {
			        for (i= 0;;) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    void foo() {
			        for (int i = 0;;) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    void foo(int i) {
			        for (i= 0;;) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testVarInForInitializer2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    /**
			     * @return Returns a number
			     */
			    int foo() {
			        for (i= new int[] { 1 };;) {
			        }
			        return 0;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private int[] i;

			    /**
			     * @return Returns a number
			     */
			    int foo() {
			        for (i= new int[] { 1 };;) {
			        }
			        return 0;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    /**
			     * @return Returns a number
			     */
			    int foo() {
			        for (int[] i = new int[] { 1 };;) {
			        }
			        return 0;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    /**
			     * @param i\s
			     * @return Returns a number
			     */
			    int foo(int[] i) {
			        for (i= new int[] { 1 };;) {
			        }
			        return 0;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}


	@Test
	public void testVarInInitializer() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int i= k;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private int k;
			    private int i= k;
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    private static final int k = 0;
			    private int i= k;
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testVarInOtherType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    void foo(E e) {
			         e.var2= 2;
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    protected int var1;
			}
			""";
		pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    protected int var1;
			    public int var2;
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class F {
			    void foo(E e) {
			         e.var1= 2;
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testVarInSuperFieldAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F extends E {
			    void foo() {
			         super.var2= 2;
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    protected int var1;
			}
			""";
		pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class F extends E {
			    void foo() {
			         super.var1= 2;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    protected int var1;
			    public int var2;
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testVarInSuper() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import test3.E;
			public class F extends E {
			    void foo() {
			         this.color= baz();
			    }
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", str, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str1= """
			package test2;
			public class E {
			}
			""";
		pack2.createCompilationUnit("E.java", str1, false, null);

		IPackageFragment pack3= fSourceFolder.createPackageFragment("test3", false, null);
		String str2= """
			package test3;
			public class E {
			    protected Object olor;
			    public test2.E baz() {
			        return null;
			    }
			}
			""";
		pack3.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import test3.E;
			public class F extends E {
			    void foo() {
			         this.olor= baz();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import test3.E;
			public class F extends E {
			    private test2.E color;

			    void foo() {
			         this.color= baz();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}


	@Test
	public void testVarInAnonymous() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int fcount) {
			        new Runnable() {
			            public void run() {
			                fCount= 7;
			            }
			        };
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 6);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo(int fcount) {
			        new Runnable() {
			            private int fCount;

			            public void run() {
			                fCount= 7;
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    protected int fCount;

			    public void foo(int fcount) {
			        new Runnable() {
			            public void run() {
			                fCount= 7;
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void foo(int fcount) {
			        new Runnable() {
			            public void run() {
			                int fCount = 7;
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			public class E {
			    public void foo(int fcount) {
			        new Runnable() {
			            public void run(int fCount) {
			                fCount= 7;
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(4);
		String preview5= getPreviewContent(proposal);

		String expected5= """
			package test1;
			public class E {
			    public void foo(int fcount) {
			        new Runnable() {
			            public void run() {
			                fcount= 7;
			            }
			        };
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(5);
		String preview6= getPreviewContent(proposal);

		String expected6= """
			package test1;
			public class E {
			    public void foo(int fcount) {
			        new Runnable() {
			            public void run() {
			            }
			        };
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4, preview5, preview6 }, new String[] { expected1, expected2, expected3, expected4, expected5, expected6 });
	}

	@Test
	public void testVarInAnnotation1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public @interface Annot {
			        String value();
			    }
			   \s
			    @Annot(x)
			    public void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			public class E {
			    public @interface Annot {
			        String value();
			    }

			    private static final String x = null;
			   \s
			    @Annot(x)
			    public void foo() {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testVarInAnnotation2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public @interface Annot {
			        float value();
			    }
			   \s
			    @Annot(value=x)
			    public void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			public class E {
			    public @interface Annot {
			        float value();
			    }

			    private static final float x = 0;
			   \s
			    @Annot(value=x)
			    public void foo() {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testVarInAnnotation3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public @interface Annot {
			        float[] value();
			    }
			   \s
			    @Annot(value={x})
			    class Inner {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package pack;
			public class E {
			    public @interface Annot {
			        float[] value();
			    }

			    private static final float x = 0;
			   \s
			    @Annot(value={x})
			    class Inner {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testStaticImportFavorite1() throws Exception {
		IPreferenceStore preferenceStore= PreferenceConstants.getPreferenceStore();
		preferenceStore.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "java.lang.Math.*");
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
			String str= """
				package pack;

				public class E {
				    private float foo() {
				        return PI;
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

			CompilationUnit astRoot= getASTRoot(cu);
			ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

			assertCorrectLabels(proposals);

			String[] expected= new String[1];
			expected[0]= """
				package pack;

				import static java.lang.Math.PI;

				public class E {
				    private float foo() {
				        return PI;
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			preferenceStore.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "");
		}
	}

	@Test
	public void testLongVarRef() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public int mash;
			    void foo(E e) {
			         e.var.hash= 2;
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    public F var;
			}
			""";
		pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class F {
			    public int mash;
			    private int hash;
			    void foo(E e) {
			         e.var.hash= 2;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class F {
			    public int mash;
			    void foo(E e) {
			         e.var.mash= 2;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2}, new String[] { expected1, expected2});
	}

	@Test
	public void testVarAndTypeRef() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.File;
			public class F {
			    void foo() {
			        char ch= Fixe.pathSeparatorChar;
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertCorrectLabels(proposals);

		int i= 0;
		String[] expected= new String[proposals.size()];

		expected[i++]= """
			package test1;
			import java.io.File;
			public class F {
			    private Object Fixe;

			    void foo() {
			        char ch= Fixe.pathSeparatorChar;
			    }
			}
			""";


		expected[i++]= """
			package test1;
			import java.io.File;
			public class F {
			    void foo() {
			        Object Fixe;
			        char ch= Fixe.pathSeparatorChar;
			    }
			}
			""";

		expected[i++]= """
			package test1;
			import java.io.File;
			public class F {
			    void foo(Object Fixe) {
			        char ch= Fixe.pathSeparatorChar;
			    }
			}
			""";

		expected[i++]= """
			package test1;
			import java.io.File;
			public class F {
			    private static final String Fixe = null;

			    void foo() {
			        char ch= Fixe.pathSeparatorChar;
			    }
			}
			""";

		expected[i++]= """
			package test1;

			public class Fixe {

			}
			""";

		expected[i++]= """
			package test1;

			public interface Fixe {

			}
			""";

		expected[i++]= """
			package test1;

			public enum Fixe {

			}
			""";

		expected[i++]= """
			package test1;
			import java.io.File;
			public class F {
			    void foo() {
			        char ch= File.pathSeparatorChar;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testVarWithGenericType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			public class F {
			    void foo(E e) {
			         e.var2= new ArrayList<String>();
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    protected int var1;
			}
			""";
		pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;

			import java.util.ArrayList;

			public class E {
			    protected int var1;
			    public ArrayList<String> var2;
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.ArrayList;
			public class F {
			    void foo(E e) {
			         e.var1= new ArrayList<String>();
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}


	@Test
	public void testSimilarVariableNames1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		String str= """
			package test3;
			public class E {
			    private static final short CON1= 1;
			    private static final float CON2= 1.0f;
			    private String bla;
			    private String cout;
			    public int foo() {
			        return count;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		for (int i= proposals.size() - 1; i >= 0; i--) {
			Object curr= proposals.get(i);
			if (!(curr instanceof RenameNodeCorrectionProposal)) {
				proposals.remove(i);
			}
		}

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test3;
			public class E {
			    private static final short CON1= 1;
			    private static final float CON2= 1.0f;
			    private String bla;
			    private String cout;
			    public int foo() {
			        return CON1;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test3;
			public class E {
			    private static final short CON1= 1;
			    private static final float CON2= 1.0f;
			    private String bla;
			    private String cout;
			    public int foo() {
			        return cout;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testSimilarVariableNames2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		String str= """
			package test3;
			public class E {
			    private static final short CON1= 1;
			    private static final float CON2= 1.0f;
			    private static short var1= 1;
			    private static float var2= 1.0f;
			    private String bla;
			    private String cout;
			    public void foo(int x) {
			        count= x;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		for (int i= proposals.size() - 1; i >= 0; i--) {
			Object curr= proposals.get(i);
			if (!(curr instanceof RenameNodeCorrectionProposal)) {
				proposals.remove(i);
			}
		}

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test3;
			public class E {
			    private static final short CON1= 1;
			    private static final float CON2= 1.0f;
			    private static short var1= 1;
			    private static float var2= 1.0f;
			    private String bla;
			    private String cout;
			    public void foo(int x) {
			        var2= x;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test3;
			public class E {
			    private static final short CON1= 1;
			    private static final float CON2= 1.0f;
			    private static short var1= 1;
			    private static float var2= 1.0f;
			    private String bla;
			    private String cout;
			    public void foo(int x) {
			        cout= x;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testSimilarVariableNamesMultipleOcc() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test3", false, null);
		String str= """
			package test3;
			public class E {
			    private int cout;
			    public void setCount(int x) {
			        count= x;
			        count++;
			    }
			    public int getCount() {
			        return count;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3);
		for (int i= proposals.size() - 1; i >= 0; i--) {
			Object curr= proposals.get(i);
			if (!(curr instanceof RenameNodeCorrectionProposal)) {
				proposals.remove(i);
			}
		}

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test3;
			public class E {
			    private int cout;
			    public void setCount(int x) {
			        cout= x;
			        cout++;
			    }
			    public int getCount() {
			        return cout;
			    }
			}
			""";

		assertEqualString( preview1, expected1);
	}

	@Test
	public void testUndeclaredPrimitiveVariable() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public void foo() {
			        for (int number : numbers) {
			        }
			    }
			}
			""";

		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		List<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		CUCorrectionProposal localProposal= null;
		for (IJavaCompletionProposal curr : proposals) {
			if (curr instanceof NewVariableCorrectionProposal && ((NewVariableCorrectionProposal) curr).getVariableKind() == NewVariableCorrectionProposal.LOCAL) {
				localProposal= (CUCorrectionProposal) curr;
			}
		}

		assertNotNull(localProposal);

		String preview= getPreviewContent(localProposal);
		String expected= """
			package test1;

			public class E {
			    public void foo() {
			        int[] numbers;
			        for (int number : numbers) {
			        }
			    }
			}
			""";

		assertEqualString(preview, expected);
	}

	@Test
	public void testUndeclaredObjectVariable() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;

			public class E {
			    public void foo() {
			        for (Integer number : numbers) {
			        }
			    }
			}
			""";

		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		List<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 1);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		CUCorrectionProposal localProposal= null;
		for (IJavaCompletionProposal curr : proposals) {
			if (curr instanceof NewVariableCorrectionProposal && ((NewVariableCorrectionProposal) curr).getVariableKind() == NewVariableCorrectionProposal.LOCAL) {
				localProposal= (CUCorrectionProposal) curr;
			}
		}

		assertNotNull(localProposal);

		String preview= getPreviewContent(localProposal);
		String expected= """
			package test1;

			public class E {
			    public void foo() {
			        Integer[] numbers;
			        for (Integer number : numbers) {
			        }
			    }
			}
			""";

		assertEqualString(preview, expected);
	}

	@Test
	public void testVarMultipleOccurences1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        for (i= 0; i > 9; i++) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal localProposal= null;
		for (IJavaCompletionProposal curr : proposals) {
			if (curr instanceof NewVariableCorrectionProposal && ((NewVariableCorrectionProposal) curr).getVariableKind() == NewVariableCorrectionProposal.LOCAL) {
				localProposal= (CUCorrectionProposal) curr;
			}
		}
		assertNotNull(localProposal);

		String preview= getPreviewContent(localProposal);
		String expected= """
			package test1;
			public class E {
			    void foo() {
			        for (int i = 0; i > 9; i++) {
			        }
			    }
			}
			""";

		assertEqualString(preview, expected);
	}

	@Test
	public void testVarMultipleOccurences2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        for (i= 0; i > 9;) {
			            i++;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal localProposal= null;
		for (IJavaCompletionProposal curr : proposals) {
			if (curr instanceof NewVariableCorrectionProposal && ((NewVariableCorrectionProposal) curr).getVariableKind() == NewVariableCorrectionProposal.LOCAL) {
				localProposal= (CUCorrectionProposal) curr;
			}
		}
		assertNotNull(localProposal);

		String preview= getPreviewContent(localProposal);
		String expected= """
			package test1;
			public class E {
			    void foo() {
			        for (int i = 0; i > 9;) {
			            i++;
			        }
			    }
			}
			""";

		assertEqualString(preview, expected);
	}

	@Test
	public void testVarMultipleOccurences3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        for (i = 0; i > 9;) {
			        }
			        i= 9;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal localProposal= null;
		for (IJavaCompletionProposal curr : proposals) {
			if (curr instanceof NewVariableCorrectionProposal && ((NewVariableCorrectionProposal) curr).getVariableKind() == NewVariableCorrectionProposal.LOCAL) {
				localProposal= (CUCorrectionProposal) curr;
			}
		}
		assertNotNull(localProposal);

		String preview= getPreviewContent(localProposal);
		String expected= """
			package test1;
			public class E {
			    void foo() {
			        int i;
			        for (i = 0; i > 9;) {
			        }
			        i= 9;
			    }
			}
			""";

		assertEqualString(preview, expected);
	}

	@Test
	public void testVarInArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo(Object[] arr) {
			        for (int i = 0; i > arr.lenght; i++) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    void foo(Object[] arr) {
			        for (int i = 0; i > arr.length; i++) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testVarInEnumSwitch() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public enum Colors {
			    RED
			}
			""";
		pack1.createCompilationUnit("Colors.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    void foo(Colors c) {
			        switch (c) {
			            case BLUE:
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public enum Colors {
			    RED, BLUE
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    void foo(Colors c) {
			        switch (c) {
			            case RED:
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testVarInMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void goo(String s) {
			    }
			    void foo() {
			        goo(x);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private String x;
			    void goo(String s) {
			    }
			    void foo() {
			        goo(x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    void goo(String s) {
			    }
			    void foo(String x) {
			        goo(x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    void goo(String s) {
			    }
			    void foo() {
			        String x;
			        goo(x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			public class E {
			    private static final String x = null;
			    void goo(String s) {
			    }
			    void foo() {
			        goo(x);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testVarInConstructurInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public E(String s) {
			    }
			    public E() {
			        this(x);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private static String x;
			    public E(String s) {
			    }
			    public E() {
			        this(x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public E(String s) {
			    }
			    public E(String x) {
			        this(x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    private static final String x = null;
			    public E(String s) {
			    }
			    public E() {
			        this(x);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testVarInSuperConstructurInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public F(String s) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E extends F {
			    public E() {
			        super(x);
			    }
			}
			""";
		cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E extends F {
			    private static String x;

			    public E() {
			        super(x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E extends F {
			    public E(String x) {
			        super(x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E extends F {
			    private static final String x = null;

			    public E() {
			        super(x);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testVarInClassInstanceCreation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class F {
			    public F(String s) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("F.java", str, false, null);

		String str1= """
			package test1;
			public class E {
			    public E() {
			        new F(x);
			    }
			}
			""";
		cu= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private String x;

			    public E() {
			        new F(x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public E(String x) {
			        new F(x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    private static final String x = null;

			    public E() {
			        new F(x);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			public class E {
			    public E() {
			        String x;
			        new F(x);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testVarInArrayAccess() throws Exception {
		// bug 194913
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;

			public class E {
			    void foo(int i) {
			        bar[0][i] = "bar";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[4];
		expected[0]= """
			package p;

			public class E {
			    private String[][] bar;

			    void foo(int i) {
			        bar[0][i] = "bar";
			    }
			}
			""";

		expected[1]= """
			package p;

			public class E {
			    private static final String[][] bar = null;

			    void foo(int i) {
			        bar[0][i] = "bar";
			    }
			}
			""";

		expected[2]= """
			package p;

			public class E {
			    void foo(int i, String[][] bar) {
			        bar[0][i] = "bar";
			    }
			}
			""";

		expected[3]= """
			package p;

			public class E {
			    void foo(int i) {
			        String[][] bar;
			        bar[0][i] = "bar";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testVarWithMethodName1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    int foo(String str) {
			        for (int i = 0; i > str.length; i++) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    int foo(String str) {
			        for (int i = 0; i > str.length(); i++) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testVarWithMethodName2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    int foo(String str) {
			        return length;
			    }
			    int getLength() {
			        return 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    int foo(String str) {
			        return getLength();
			    }
			    int getLength() {
			        return 1;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testSimilarVarsAndVisibility() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;

			public class E {
			    int var1;
			    static int var2;
			    public static void main(String[] var3) {
			        println(var);
			    }
			    public static void println(String[] s) {}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 6);

		String[] expected= new String[6];
		expected[0]= """
			package test;

			public class E {
			    int var1;
			    static int var2;
			    public static void main(String[] var3) {
			        println(var3);
			    }
			    public static void println(String[] s) {}
			}
			""";

		expected[1]= """
			package test;

			public class E {
			    int var1;
			    static int var2;
			    public static void main(String[] var3) {
			        println(var2);
			    }
			    public static void println(String[] s) {}
			}
			""";

		expected[2]= """
			package test;

			public class E {
			    int var1;
			    static int var2;
			    private static String[] var;
			    public static void main(String[] var3) {
			        println(var);
			    }
			    public static void println(String[] s) {}
			}
			""";

		expected[3]= """
			package test;

			public class E {
			    private static final String[] var = null;
			    int var1;
			    static int var2;
			    public static void main(String[] var3) {
			        println(var);
			    }
			    public static void println(String[] s) {}
			}
			""";

		expected[4]= """
			package test;

			public class E {
			    int var1;
			    static int var2;
			    public static void main(String[] var3, String[] var) {
			        println(var);
			    }
			    public static void println(String[] s) {}
			}
			""";

		expected[5]= """
			package test;

			public class E {
			    int var1;
			    static int var2;
			    public static void main(String[] var3) {
			        String[] var;
			        println(var);
			    }
			    public static void println(String[] s) {}
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testVarOfShadowedType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;

			public class E {
			    class Runnable { }
			    public void test() {
			        new Thread(myRunnable);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 4);

		String[] expected= new String[4];
		expected[0]= """
			package test1;

			public class E {
			    class Runnable { }
			    private java.lang.Runnable myRunnable;
			    public void test() {
			        new Thread(myRunnable);
			    }
			}
			""";

		expected[1]= """
			package test1;

			public class E {
			    class Runnable { }
			    private static final java.lang.Runnable myRunnable = null;
			    public void test() {
			        new Thread(myRunnable);
			    }
			}
			""";

		expected[2]= """
			package test1;

			public class E {
			    class Runnable { }
			    public void test(java.lang.Runnable myRunnable) {
			        new Thread(myRunnable);
			    }
			}
			""";

		expected[3]= """
			package test1;

			public class E {
			    class Runnable { }
			    public void test() {
			        java.lang.Runnable myRunnable;
			        new Thread(myRunnable);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}


	/**
	 * Wrong quick fixes when accessing protected field in subclass in different package.
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=280819#c2
	 *
	 * @throws Exception if anything goes wrong
	 * @since 3.9
	 */
	@Test
	public void testVarParameterAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);

		String str= """
			package test1;
			public class Base {
			    protected int myField;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Base.java", str, false, null);

		String str1= """
			package test2;
			import test1.Base;
			public class Child extends Base {
			    public void aMethod(Base parent) {
			        System.out.println(parent.myField);
			    }
			}
			""";
		cu = pack2.createCompilationUnit("Child.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String [] expected = new String[1];
		expected[0]= """
			package test1;
			public class Base {
			    public int myField;
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testBug547404() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			public class Y {
			    void f(X x) {
			         x.field= new ArrayList<String>();
			    }
			}
			""";

		ICompilationUnit cu1= pack1.createCompilationUnit("Y.java", str, false, null);

		String str1= """
			package test1;
			public class X {
			    class ArrayList{}
			}
			""";
		pack1.createCompilationUnit("X.java", str1, false, null);

		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String expected= """
			package test1;
			public class X {
			    class ArrayList{}

			    public java.util.ArrayList<String> field;
			}
			""";

		assertEquals(expected, preview);
	}

}
