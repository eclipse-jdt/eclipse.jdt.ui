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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.fix.ConvertForLoopOperation;
import org.eclipse.jdt.internal.corext.fix.ConvertLoopOperation;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessor;
import org.eclipse.jdt.internal.ui.text.correction.proposals.FixCorrectionProposal;

public class ConvertForLoopQuickFixTest extends QuickFixTest {

	@Rule
	public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject fJProject1;
	IPackageFragmentRoot fSourceFolder;
	private FixCorrectionProposal fConvertLoopProposal;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		fConvertLoopProposal= null;
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
		fJProject1= null;
		fSourceFolder= null;
		fConvertLoopProposal= null;
	}

	@Test
	public void testSimplestSmokeCase() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
			    public void foo() {
					int[] array = {1,2,3,4};
					for (int element : array) {
					}
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNotEqualsComparison() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("public class B {\n");
		bld.append("    public void foo() {\n");
		bld.append("        int[] array = {5, 6, 7, 8};\n");
		bld.append("        for (int i = 0; i != array.length; i++) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);
		String expected= """
			package test1;
			
			public class B {
			    public void foo() {
			        int[] array = {5, 6, 7, 8};
			        for (int element : array) {
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testLengthVariable() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("public class C {\n");
		bld.append("    public void foo() {\n");
		bld.append("        int[] array = {5, 6, 7, 8};\n");
		bld.append("        for (int i = 0, len = array.length; i < len; i++) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);
		String expected= """
			package test1;
			
			public class C {
			    public void foo() {
			        int[] array = {5, 6, 7, 8};
			        for (int element : array) {
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testLengthVariableNotEquals() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("public class D {\n");
		bld.append("    public void foo() {\n");
		bld.append("        int[] array = {5, 6, 7, 8};\n");
		bld.append("        for (int i = 0, len = array.length; i != len; i++) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("D.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);
		String expected= """
			package test1;
			
			public class D {
			    public void foo() {
			        int[] array = {5, 6, 7, 8};
			        for (int element : array) {
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testWrongComparison() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("public class E {\n");
		bld.append("    public void foo() {\n");
		bld.append("        int[] array = {5, 6, 7, 8};\n");
		bld.append("        for (int i = 0, len = array.length; i > len; i++) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", bld.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCaptureList() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("import java.util.List;\n");
		bld.append("\n");
		bld.append("public class F {\n");
		bld.append("    public void foo(List<?> wildcardList) {\n");
		bld.append("        for (int j = 0; j < wildcardList.size(); j++) {\n");
		bld.append("            Object element = wildcardList.get(j);\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("F.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);
		String expected= """
			package test1;
			
			import java.util.List;
			
			public class F {
			    public void foo(List<?> wildcardList) {
			        for (Object element : wildcardList) {
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testWildcardList() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("import java.util.List;\n");
		bld.append("\n");
		bld.append("public class G {\n");
		bld.append("    public void foo(List<? extends String> wildcardList) {\n");
		bld.append("        for (int j = 0; j < wildcardList.size(); j++) {\n");
		bld.append("            String element = wildcardList.get(j);\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("G.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);
		String expected= """
			package test1;
			
			import java.util.List;
			
			public class G {
			    public void foo(List<? extends String> wildcardList) {
			        for (String element : wildcardList) {
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testUpperBoundList() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("import java.util.List;\n");
		bld.append("\n");
		bld.append("public class H {\n");
		bld.append("    public void foo(List<? super String> wildcardList) {\n");
		bld.append("        for (int j = 0; j < wildcardList.size(); j++) {\n");
		bld.append("            Object element = wildcardList.get(j);\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("H.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);
		String expected= """
			package test1;
			
			import java.util.List;
			
			public class H {
			    public void foo(List<? super String> wildcardList) {
			        for (Object element : wildcardList) {
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testCollection() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		StringBuilder bld= new StringBuilder();
		bld.append("package test1;\n");
		bld.append("\n");
		bld.append("import java.util.Collection;\n");
		bld.append("\n");
		bld.append("public class I {\n");
		bld.append("    public void foo(Collection<? super String> wildcardCollection) {\n");
		bld.append("        for (int q = 0; q < wildcardCollection.size(); q++) {\n");
		bld.append("        }\n");
		bld.append("    }\n");
		bld.append("}\n");

		ICompilationUnit cu= pack1.createCompilationUnit("I.java", bld.toString(), false, null);
		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(bld, cu);
		assertNotNull(fConvertLoopProposal);
		assertCorrectLabels(proposals);
		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			
			import java.util.Collection;
			
			public class I {
			    public void foo(Collection<? super String> wildcardCollection) {
			        for (Object element : wildcardCollection) {
			        }
			    }
			}
			""";

		assertEqualString(preview1, expected);
	}

	@Test
	public void testNameDetectionChildren() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] children = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < children.length; i++){\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
				public void foo() {
					int[] children = {1,2,3,4};
					for (int child : children) {
					}
				}
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNameDetectionS() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] locations = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < locations.length; i++){\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
				public void foo() {
					int[] locations = {1,2,3,4};
					for (int location : locations) {
					}
				}
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNameDetectionlocationEntries() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] locationEntries = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < locationEntries.length; i++){\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
				public void foo() {
					int[] locationEntries = {1,2,3,4};
					for (int locationEntry : locationEntries) {
					}
				}
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNameDetectionEntries() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] Entries = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < Entries.length; i++){\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
				public void foo() {
					int[] Entries = {1,2,3,4};
					for (int entry : Entries) {
					}
				}
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNameDetectionProxies() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		int[] proxies = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < proxies.length; i++){\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
				public void foo() {
					int[] proxies = {1,2,3,4};
					for (int proxy : proxies) {
					}
				}
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNameDetectionAllChildren() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("	public void foo() {\n");
		buf.append("		String[] AllChildren = {\"aaa\",\"bbb\",\"ccc\"};\n");
		buf.append("		for (int i = 0; i < AllChildren.length; i++){\n");
		buf.append("			System.out.println(AllChildren[i]);\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
				public void foo() {
					String[] AllChildren = {"aaa","bbb","ccc"};
					for (String child : AllChildren) {
						System.out.println(child);
					}
				}
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testInferPrimitiveTypeElement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		char[] array = {'1','2'};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
			    public void foo() {
					char[] array = {'1','2'};
					for (char element : array) {
					}
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testInferTypeElement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = {\"1\",\"2\"};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
			    public void foo() {
					String[] array = {"1","2"};
					for (String element : array) {
					}
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testSimplestClean() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = {\"1\",\"2\"};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(array[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
			    public void foo() {
					String[] array = {"1","2"};
					for (String element : array) {
						System.out.println(element);
					}
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testLotsOfRefereces() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = {\"1\",\"2\"};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			if (array[i].equals(\"2\"))\n");
		buf.append("				System.out.println(array[i]);\n");
		buf.append("			else if ((array[i] + 2) == \"4\"){\n");
		buf.append("				int k = Integer.parseInt(array[i]) - 2;\n");
		buf.append("			}\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
			    public void foo() {
					String[] array = {"1","2"};
					for (String element : array) {
						if (element.equals("2"))
							System.out.println(element);
						else if ((element + 2) == "4"){
							int k = Integer.parseInt(element) - 2;
						}
					}
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testInferCollectionFromInitializers() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = {\"1\",\"2\"};\n");
		buf.append("		for (int i = 0, max = array.length; i < max; i++){\n");
		buf.append("			if (array[i].equals(\"2\"))\n");
		buf.append("				System.out.println(array[i]);\n");
		buf.append("			else if ((array[i] + 2) == \"4\"){\n");
		buf.append("				int k = Integer.parseInt(array[i]) - 2;\n");
		buf.append("			}\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
			    public void foo() {
					String[] array = {"1","2"};
					for (String element : array) {
						if (element.equals("2"))
							System.out.println(element);
						else if ((element + 2) == "4"){
							int k = Integer.parseInt(element) - 2;
						}
					}
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNiceReduction() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Weirdy{}\n");
		buf.append("private Weirdy[] weirdies;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo(){\n");
		buf.append("		for (int i = 0, length = weirdies.length; i < length; i++){\n");
		buf.append("			System.out.println();\n");
		buf.append("		    Weirdy p = weirdies[i];\n");
		buf.append("		    if (p != null){\n");
		buf.append("				System.out.println(p);\n");
		buf.append("	    	}\n");
		buf.append("	    }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			class Weirdy{}
			private Weirdy[] weirdies;
			public class A {
			    public void foo(){
					for (Weirdy p : weirdies) {
						System.out.println();
					    if (p != null){
							System.out.println(p);
				    	}
				    }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testNiceReductionArrayIsField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Weirdy{}\n");
		buf.append("public class A {\n");
		buf.append("	private Weirdy[] weirdies;\n");
		buf.append("    public void foo(){\n");
		buf.append("		for (int i = 0, length = weirdies.length; i < length; i++){\n");
		buf.append("			System.out.println();\n");
		buf.append("		    Weirdy p = weirdies[i];\n");
		buf.append("		    if (p != null){\n");
		buf.append("				System.out.println(p);\n");
		buf.append("	    	}\n");
		buf.append("	    }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			class Weirdy{}
			public class A {
				private Weirdy[] weirdies;
			    public void foo(){
					for (Weirdy p : weirdies) {
						System.out.println();
					    if (p != null){
							System.out.println(p);
				    	}
				    }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testArrayIsQualifiedByThis() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Weirdy{}\n");
		buf.append("public class A {\n");
		buf.append("	private Weirdy[] weirdies;\n");
		buf.append("    public void foo(){\n");
		buf.append("		for (int i = 0, length = this.weirdies.length; i < length; i++){\n");
		buf.append("			System.out.println();\n");
		buf.append("		    Weirdy p = this.weirdies[i];\n");
		buf.append("		    if (p != null){\n");
		buf.append("				System.out.println(p);\n");
		buf.append("	    	}\n");
		buf.append("	    }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			class Weirdy{}
			public class A {
				private Weirdy[] weirdies;
			    public void foo(){
					for (Weirdy p : this.weirdies) {
						System.out.println();
					    if (p != null){
							System.out.println(p);
				    	}
				    }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testArrayIsAccessedByMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Weirdy{}\n");
		buf.append("public class A {\n");
		buf.append("	private Weirdy[] weirdies;\n");
		buf.append("	private Weirdy[] getArray(){\n");
		buf.append("		return weirdies;\n");
		buf.append("	}\n");
		buf.append("    public void foo(){\n");
		buf.append("		for (int i = 0, length = this.weirdies.length; i < length; i++){\n");
		buf.append("			System.out.println();\n");
		buf.append("		    Weirdy p = getArray()[i];\n");
		buf.append("		    if (p != null){\n");
		buf.append("				System.out.println(p);\n");
		buf.append("	    	}\n");
		buf.append("	    }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testArrayIsAccessedByMethodInvocation2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Weirdy{}\n");
		buf.append("public class A {\n");
		buf.append("	private Weirdy[] weirdies;\n");
		buf.append("	private Weirdy[] getArray(){\n");
		buf.append("		return weirdies;\n");
		buf.append("	}\n");
		buf.append("    public void foo(){\n");
		buf.append("		for (int i = 0, length = getArray().length; i < length; i++){\n");
		buf.append("			System.out.println();\n");
		buf.append("		    Weirdy p = getArray()[i];\n");
		buf.append("		    if (p != null){\n");
		buf.append("				System.out.println(p);\n");
		buf.append("	    	}\n");
		buf.append("	    }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testMatrix() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[][] matrix = {{1,2},{3,4}};\n");
		buf.append("		for (int i = 0; i < matrix.length; i++){\n");
		buf.append("			System.out.println(matrix[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
			    public void foo() {
					int[][] matrix = {{1,2},{3,4}};
					for (int[] element : matrix) {
						System.out.println(element);
					}
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testMatrix2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[][] matrix = {{1,2},{3,4}};\n");
		buf.append("		for (int i = 0; i < matrix.length; i++){\n");
		buf.append("			for(int j = 0; j < matrix[i].length; j++){\n");
		buf.append("				System.out.println(matrix[i][j]);\n");
		buf.append("			}\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
			    public void foo() {
					int[][] matrix = {{1,2},{3,4}};
					for (int[] element : matrix) {
						for(int j = 0; j < element.length; j++){
							System.out.println(element[j]);
						}
					}
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testArrayIsAssigned() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			array[i]=0;\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testArrayIsAssigned2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			++array[i];\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testArrayCannotBeInferred() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < 4; i++){\n");
		buf.append("			System.out.println(array[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexBruteModified() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(array[i]);\n");
		buf.append("			i++;\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexBruteModified2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			i = array.lenght;\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexReadOutsideArrayAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			if (i == 1){};\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexReadOutsideArrayAccess_StringConcatenation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(i + array[i]);");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexReadOutsideInferredArrayAccess() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		int[] array2 = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(array[i] + array2[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexReadOutsideInferredArrayAccess2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public int get(int i) {\n");
		buf.append("        return i; \n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("		String[] array = null;\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(array[get(i)]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testReverseTraversalIsNotAllowed() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = array.length; i > 0; --i){\n");
		buf.append("			System.out.println(array[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCollectionIsNotArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		java.util.List list = new ArrayList();\n");
		buf.append("		list.add(null);\n");
		buf.append("		for (int i = 0; i < list.size(); i++) {\n");
		buf.append("			System.out.println(list.get(i));\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
			    public void foo() {
					java.util.List list = new ArrayList();
					list.add(null);
					for (Object element : list) {
						System.out.println(element);
					}
			    }
			}
			""";
		assertEqualString(preview1, expected);

		assertCorrectLabels(proposals);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testAdditionalLocalIsNotReferenced() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0, j = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(array[i] + j++);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testTwoIndexesNotAllowed() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0, j = 0; i < array.length; i++, j++){\n");
		buf.append("			System.out.println(array[i] + j);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testAdditionalLocalIsNotReferenced2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		int i,j;\n");
		buf.append("		for (i = 0, j = 1; i < array.length; i++){\n");
		buf.append("			System.out.println(array[i] + j++);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCollectionTypeBindingIsNull() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		in[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < array.length; i++){\n");
		buf.append("			System.out.println(array[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCollectionBindingIsNull() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("		for (int i = 0; i < arra.length; i++){\n");
		buf.append("			System.out.println(array[i]);\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCollectionsAccepted() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List strings= new ArrayList();\n");
		buf.append("		for (int i= 0; i < strings.size(); i++);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			import java.util.List;
			import java.util.ArrayList;
			public class A {
			    public void foo() {
					List strings= new ArrayList();
					for (Object string : strings);
			    }
			}
			""";
		assertEqualString(preview1, expected);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCollectionsAccepted2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		List<String> strings= new ArrayList<>();\n");
		buf.append("		for (int i= 0; i < strings.size(); i++);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			import java.util.List;
			import java.util.ArrayList;
			public class A {
			    public void foo() {
					List<String> strings= new ArrayList<>();
					for (String string : strings);
			    }
			}
			""";
		assertEqualString(preview1, expected);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testCollectionsAccepted3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Set;\n");
		buf.append("import java.util.TreeSet;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		Set<String> strings= new TreeSet<>();\n");
		buf.append("		for (int i= 0; i < strings.size(); i++);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			import java.util.Set;
			import java.util.TreeSet;
			public class A {
			    public void foo() {
					Set<String> strings= new TreeSet<>();
					for (String string : strings);
			    }
			}
			""";
		assertEqualString(preview1, expected);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testMapCollectionsNotAccepted() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.Map;\n");
		buf.append("import java.util.HashMap;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo(Map<String, String> map) {\n");
		buf.append("		for (int i= 0; i < map.size(); i++) {\n");
		buf.append("			String x= map.get(\"\" + i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testMultipleCollectionsNotAccepted() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo(Map<String, String> map) {\n");
        buf.append("        List<File> a = new ArrayList<>();\n");
        buf.append("        List<File> b = new ArrayList<>();\n");
        buf.append("        for (int i = 0; i < a.size(); i++) {\n");
        buf.append("            System.out.print(a.get(i) + \" \" + b.get(i));\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug550726() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.File;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class A {\n");
		buf.append("    public static void main(String[] args) {\n");
		buf.append("        List<File> a = new ArrayList<>();\n");
		buf.append("        for (int i = 0; i < a.size(); i++) {\n");
		buf.append("            System.out.print(a);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			import java.io.File;
			import java.util.ArrayList;
			import java.util.List;
			public class A {
			    public static void main(String[] args) {
			        List<File> a = new ArrayList<>();
			        for (File element : a) {
			            System.out.print(a);
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testArrayAccessWithCollections() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array = {1,2,3,4};\n");
		buf.append("        List<String> list = ArrayList.asList(\"a\", \"b\", \"c\", \"d\");\n");
		buf.append("		for (int i = 0, i < list.size(); i++){\n");
		buf.append("			System.out.println(array[i] + list.get(i));\n");
		buf.append("		}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testIndexDoesNotStartFromZero() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("		int[] array= null;\n");
		buf.append("		for (int i= 1; i < array.length; i++);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug127346() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int[] arr= new int[7]; 1 < arr.length;) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug130139_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String[] strings) {\n");
		buf.append("        int x= 1;\n");
		buf.append("        for (int i= x; i < strings.length; i++) {\n");
		buf.append("            System.out.println(strings[i]);\n");
		buf.append("        }  \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug130139_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String[] strings) {\n");
		buf.append("        for (int i= x(); i < strings.length; i++) {\n");
		buf.append("            System.out.println(strings[i]);\n");
		buf.append("        }  \n");
		buf.append("    }\n");
		buf.append("    private int x(){\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug130293_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    private int[] arr;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < this.arr.length; i++) {\n");
		buf.append("            System.out.println(this.arr[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
			    private int[] arr;
			    public void foo() {
			        for (int element : this.arr) {
			            System.out.println(element);
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug130293_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    private class E1Sub {\n");
		buf.append("        public int[] array;\n");
		buf.append("    }\n");
		buf.append("    private E1Sub e1sub;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < this.e1sub.array.length; i++) {\n");
		buf.append("            System.out.println(this.e1sub.array[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class A {
			    private class E1Sub {
			        public int[] array;
			    }
			    private E1Sub e1sub;
			    public void foo() {
			        for (int element : this.e1sub.array) {
			            System.out.println(element);
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug138353_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    private class Bar {\n");
		buf.append("        public int[] getBar() {\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        Bar bar1= null;\n");
		buf.append("        Bar bar2= null;\n");
		buf.append("        for (int i = 0; i < bar1.getBar().length; i++) {\n");
		buf.append("            System.out.println(bar1.getBar()[i]);\n");
		buf.append("            System.out.println(bar2.getBar()[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug138353_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    private class Bar {\n");
		buf.append("        public int[] getBar() {\n");
		buf.append("            return null;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        Bar bar1= null;\n");
		buf.append("        for (int i = 0; i < bar1.getBar().length; i++) {\n");
		buf.append("            System.out.println(bar1.getBar()[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug148419() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int[] ints;\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i = 0; i < this.ints.length; i++) {\n");
		buf.append("            this.ints[i]= 0;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug149797() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    private int ba r() {return 0;}\n");
		buf.append("    public void foo(int[] ints) {\n");
		buf.append("        for (int i = 0, max = ints.length, b= bar(); i < max; i++) {\n");
		buf.append("            System.out.println(ints[i] + b);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug163050_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object[] x) {\n");
		buf.append("        int i = 0;\n");
		buf.append("        for (int j = 0; j < x.length; j++) {\n");
		buf.append("            System.out.println(x[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test;
			public class E {
			    void foo(Object[] x) {
			        int i = 0;
			        for (Object element : x) {
			            System.out.println(x[i]);
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug163050_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Object[] x) {\n");
		buf.append("        for (int j = 0; j < x.length; j++) {\n");
		buf.append("            System.out.println(x[0]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test;
			public class E {
			    void foo(Object[] x) {
			        for (Object element : x) {
			            System.out.println(x[0]);
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug163121() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    void foo(Object[] x, Object[] y) {\n");
		buf.append("        for (int i= 0; i < y.length; i++)\n");
		buf.append("            for (Object element : x)\n");
		buf.append("                System.out.println(y[i]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class E1 {
			    void foo(Object[] x, Object[] y) {
			        for (Object element2 : y)
			            for (Object element : x)
			                System.out.println(element2);
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	private List<IJavaCompletionProposal> fetchConvertingProposal(StringBuilder buf, ICompilationUnit cu) throws Exception {
		int offset= buf.toString().indexOf("for");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		fConvertLoopProposal= (FixCorrectionProposal)findProposalByCommandId(QuickAssistProcessor.CONVERT_FOR_LOOP_ID, proposals);
		return proposals;
	}

	private List<IJavaCompletionProposal> fetchConvertingProposal2(StringBuilder buf, ICompilationUnit cu) throws Exception {
		int offset= buf.toString().indexOf("for");
		offset= buf.toString().indexOf("for", offset+3);
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		fConvertLoopProposal= (FixCorrectionProposal)findProposalByCommandId(QuickAssistProcessor.CONVERT_FOR_LOOP_ID, proposals);
		return proposals;
	}

	@Test
	public void testInitializerPrecondition01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0; i < x.length; i++) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testInitializerPrecondition02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 1; i < x.length; i++) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testInitializerPrecondition03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        int i;\
			        for (i = 0; i < x.length; i++) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testInitializerPrecondition04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        int i, f;
			        for (i = 0, f= 0; i < x.length; i++) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testInitializerPrecondition05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0, length= x.length; i < x.length; i++) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testInitializerPrecondition06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test1;
			public class E1 {
			    void foo(Object[] x) {
			        for (int j = 0, a = init(); j < x.length; j++) {
			            System.out.println(x[j]);
			        }
			    }
			    private int init() {return 0;}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0; x.length > i; i++) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0; x.length <= i; i++) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0; x.length < j; i++) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    private static class MyClass {
			        public int length;
			    }
			    public void foo(MyClass x) {
			        for (int i = 0; i < x.length; i++) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    Object[] x;
			    public void foo() {
			        for (int i = 0; i < x.length; i++) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    Object[] x;
			    public void foo() {
			        for (int i = 0; i < this.x.length; i++) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0, length= x.length; i < length; i++) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testExpressionPrecondition08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0, length= x.length; length > i; i++) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testUpdatePrecondition01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0; i < x.length; i+= 1) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testUpdatePrecondition02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0; i < x.length; i= 1 + i) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testUpdatePrecondition03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0; i < x.length; i= i + 1) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testUpdatePrecondition04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0; i < x.length; i= i + 2) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testUpdatePrecondition06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
					int j= 0\
			        for (int i = 0; i < x.length; i= j + 1) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testUpdatePrecondition07() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=349782
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0; i < x.length; ++i) {}
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0; i < x.length; i++) {
			            System.out.println(x[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    Object[] x;
			    public void foo() {
			        for (int i = 0; i < x.length; i++) {
			            System.out.println(x[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    Object[] x;
			    public void foo() {
			        for (int i = 0; i < x.length; i++) {
			            System.out.println(this.x[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    Object[] x;
			    public void foo() {
			        for (int i = 0; i < x.length; i++) {
						i++;\
			            System.out.println(this.x[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    Object[] x;
			    public void foo() {
			        for (int i = 0; i < x.length; i++) {
						System.out.println(i);\
			            System.out.println(this.x[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    Object[] x;
			    public void foo() {
			        for (int i = 0; i < x.length; i++) {
			            this.x= null;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0; i < x.length; i++) {
			            x= null;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    Object[] x;
			    public void foo() {
			        for (int i = 0; i < x.length; i++) {
			            x= null;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0; i < x.length; i++) {
			            x[i]= null;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPreconditio10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(int[] x) {
			        for (int i = 0; i < x.length; i++) {
			            --x[i];
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(int[] x) {
			        for (int i = 0; i < x.length; i++) {
			            x[i]++;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Object[] x) {
			        for (int i = 0, length= x.length; length > i; i++) {
			            System.out.println(length);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition13() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			import java.util.Iterator;
			import java.util.List;
			public class E {
			    void foo(List<String> data) {
			        for (Iterator<String> iterator = data.iterator(); iterator.hasNext();) {
			            String row = iterator.next();
			            row.equals(iterator.hasNext());
			        }
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_1() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    Object[] x;
			    public void foo() {
			        for (int i = 0; i < this.x.length; i++) {
			            System.out.println(x[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_2() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    Object[] x;
			    public void foo() {
			        for (int i = 0; i < this.x.length; i++) {
			            System.out.println(this.x[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_3() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    Object[] x;
			    public void foo(Object obj) {
			        for (int i = 0; i < ((E) obj).x.length; i++) {
			            System.out.println(((E) obj).x[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertTrue(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_4() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E implements Comparable<Object> {
			    private int[] tokens;
			    public int compareTo(Object obj) {
			        for (int i = 0; i < tokens.length; i++) {
			            int v = compare(tokens[i], ((E) obj).tokens[i]);
			            if (v != 0)
			                return v;
			        }
			        return 0;
			    }
			    private int compare(int i, int j) {
			        return i < j ? -1 : i == j ? 0 : 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_5() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E implements Comparable<Object> {
			    private int[] tokens;
			    public int compareTo(Object obj) {
			        for (int i = 0; i < this.tokens.length; i++) {
			            int v = compare(tokens[i], ((E) obj).tokens[i]);
			            if (v != 0)
			                return v;
			        }
			        return 0;
			    }
			    private int compare(int i, int j) {
			        return i < j ? -1 : i == j ? 0 : 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_6() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E implements Comparable<Object> {
			    private int[] tokens;
			    public int compareTo(Object obj) {
			        for (int i = 0; i < ((E) obj).tokens.length; i++) {
			            int v = compare(((E) obj).tokens[i], tokens[i]);
			            if (v != 0)
			                return v;
			        }
			        return 0;
			    }
			    private int compare(int i, int j) {
			        return i < j ? -1 : i == j ? 0 : 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_7() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E implements Comparable<Object> {
			    private int[] tokens;
			    public int compareTo(Object obj) {
			        for (int i = 0; i < ((E) obj).tokens.length; i++) {
			            int v = compare(((E) obj).tokens[i], this.tokens[i]);
			            if (v != 0)
			                return v;
			        }
			        return 0;
			    }
			    private int compare(int i, int j) {
			        return i < j ? -1 : i == j ? 0 : 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_8() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E implements Comparable<E> {
			    private int[] tokens;
			    public int compareTo(E obj) {
			        for (int i = 0; i < obj.tokens.length; i++) {
			            int v = compare(obj.tokens[i], this.tokens[i]);
			            if (v != 0)
			                return v;
			        }
			        return 0;
			    }
			    private int compare(int i, int j) {
			        return i < j ? -1 : i == j ? 0 : 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBodyPrecondition344674_9() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=344674
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    String[] tokens;
			    E other;
			    private E get(E a) {
			        return a;
			    }
			    public void foo(E arg) {
			        for (int i = 0; i < get(other).tokens.length; i++) {
			            E other = this; // local var shadows field
			            System.out.println(get(other).tokens[i]);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBug110599() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void a(int[] ints) {\n");
		buf.append("        //Comment\n");
		buf.append("        for (int i = 0; i < ints.length; i++) {\n");
		buf.append("            System.out.println(ints[i]);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class E1 {
			    public void a(int[] ints) {
			        //Comment
			        for (int j : ints) {
			            System.out.println(j);
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug175827() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void a(int[] ints) {\n");
		buf.append("        //Comment\n");
		buf.append("        for (int i = 0; i < ints.length; i++) {\n");
		buf.append("            System.out.println(ints[i]);\n");
		buf.append("        }\n");
		buf.append("        //Comment\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class E1 {
			    public void a(int[] ints) {
			        //Comment
			        for (int j : ints) {
			            System.out.println(j);
			        }
			        //Comment
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug214340_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E1 {
			    int[] array = new int[3];
			
			    boolean same(E1 that) {
			        for (int i = 0; i < array.length; i++) {
			            if (this.array[i] != that.array[i])
			                return false;
			        }
			        return true;
			    }
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBug214340_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E1 {
			    int[] array = new int[3];
			    static boolean same(E1 one, E1 two) {
			        for (int i = 0; i < one.array.length; i++) {
			            if (one.array[i] != two.array[i])
			                return false;
			        }
			        return true;
			    }
			}
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBug214340_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    int[] array = new int[3];\n");
		buf.append("    static boolean same(E1 one, E1 two) {\n");
		buf.append("        for (int i = 0; i < one.array.length; i++) {\n");
		buf.append("            System.out.println(one.array[i]);\n");
		buf.append("        }\n");
		buf.append("        return true;\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class E1 {
			    int[] array = new int[3];
			    static boolean same(E1 one, E1 two) {
			        for (int element : one.array) {
			            System.out.println(element);
			        }
			        return true;
			    }
			}
			
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug231575_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E1 {
			    private Object[] array;
			    public void method(E1 copy) {
			        for (int i = 0; i < copy.array.length; i++) {
			            array[i].equals(null);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", str, false, null);

		assertFalse(satisfiesPrecondition(cu));
	}

	@Test
	public void testBug510758_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();

		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private Object[] array;\n");
		buf.append("    public boolean isNull(Object object ) {\n");
		buf.append("        boolean isNull= (object != null) ? false : true;\n");
		buf.append("        return isNull; \n");
		buf.append("    }\n");
		buf.append("    public void method() {\n");
		buf.append("        for (int i = 0; i < array.length; i++) {\n");
		buf.append("            if (!isNull(array[i])) {\n");
		buf.append("                System.out.println(array[i].toString()) ");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class E1 {
			    private Object[] array;
			    public boolean isNull(Object object ) {
			        boolean isNull= (object != null) ? false : true;
			        return isNull;\s
			    }
			    public void method() {
			        for (Object element : array) {
			            if (!isNull(element)) {
			                System.out.println(element.toString()) \
			            }
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug510758_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();

		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private Object[] array;\n");
		buf.append("    public boolean isNull(Object object ) {\n");
		buf.append("        boolean isNull= (object != null) ? false : true;\n");
		buf.append("        return isNull; \n");
		buf.append("    }\n");
		buf.append("    public void method() {\n");
		buf.append("        for (int i = 0; i < array.length; i++) {\n");
		buf.append("            if (!isNull(array[i+1])) {\n");
		buf.append("                System.out.println(array[i+1].toString()) ");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug510758_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();

		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private Object[] array;\n");
		buf.append("    public boolean isNull(Object object ) {\n");
		buf.append("        boolean isNull= (object != null) ? false : true;\n");
		buf.append("        return isNull; \n");
		buf.append("    }\n");
		buf.append("    public void method() {\n");
		buf.append("        for (int i = 0; i < array.length; i++) {\n");
		buf.append("            if (~isNull(array[i])) {\n");
		buf.append("                System.out.println(array[i].toString()) ");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);

		assertFalse(satisfiesPrecondition(cu));

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);
	}

	@Test
	public void testBug542936() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder orig= new StringBuilder();

		orig.append("package test1;\n");
		orig.append("public class E1 {\n");
		orig.append("    private Object[] array1;\n");
		orig.append("    private Object[] array2;\n");
		orig.append("    public void method() {\n");
		orig.append("        outer:\n");
		orig.append("        for (int i = 0; i < array1.length; i++) {\n");
		orig.append("            Object o1= array1[i];\n");
		orig.append("            for (int j = 0; j < array2.length; j++) {\n");
		orig.append("                Object o2= array2[j];\n");
		orig.append("                if (o2.equals(o1)) {\n");
		orig.append("                    continue outer;\n");
		orig.append("                }\n");
		orig.append("            }\n");
		orig.append("        }\n");
		orig.append("    }\n");
		orig.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E1.java", orig.toString(), false, null);

		assertTrue(satisfiesPrecondition(cu));

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(orig, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			public class E1 {
			    private Object[] array1;
			    private Object[] array2;
			    public void method() {
			        outer:
			        for (Object o1 : array1) {
			            for (int j = 0; j < array2.length; j++) {
			                Object o2= array2[j];
			                if (o2.equals(o1)) {
			                    continue outer;
			                }
			            }
			        }
			    }
			}
			""";
		assertEqualString(preview1, expected);

		proposals= fetchConvertingProposal2(orig, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview2= getPreviewContent(fConvertLoopProposal);

		expected= """
			package test1;
			public class E1 {
			    private Object[] array1;
			    private Object[] array2;
			    public void method() {
			        outer:
			        for (int i = 0; i < array1.length; i++) {
			            Object o1= array1[i];
			            for (Object o2 : array2) {
			                if (o2.equals(o1)) {
			                    continue outer;
			                }
			            }
			        }
			    }
			}
			""";
		assertEqualString(preview2, expected);
	}

	@Test
	public void testBug562291_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append(
				"""
					package test1;
					import java.io.Serializable;
					public class A {
						public void test() {
							Object[] foo= new Object[5];
							for (int i= 0; i < foo.length; i++) {
								if (!(foo[i] instanceof Serializable)) {
								}
							}
						}
					}
					""");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			import java.io.Serializable;
			public class A {
				public void test() {
					Object[] foo= new Object[5];
					for (Object element : foo) {
						if (!(element instanceof Serializable)) {
						}
					}
				}
			}
			""";
		assertEqualString(preview1, expected);
	}

	@Test
	public void testBug562291_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append(
				"""
					package test1;
					import java.io.Serializable;
					public class A {
						public void test() {
							boolean[] foo= new boolean[5];
							for (int i= 0; i < foo.length; i++) {
								if (!foo[i]) {
								}
							}
						}
					}
					""");
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		List<IJavaCompletionProposal> proposals= fetchConvertingProposal(buf, cu);

		assertNotNull(fConvertLoopProposal);

		assertCorrectLabels(proposals);

		String preview1= getPreviewContent(fConvertLoopProposal);

		String expected= """
			package test1;
			import java.io.Serializable;
			public class A {
				public void test() {
					boolean[] foo= new boolean[5];
					for (boolean element : foo) {
						if (!element) {
						}
					}
				}
			}
			""";
		assertEqualString(preview1, expected);
	}

	private boolean satisfiesPrecondition(ICompilationUnit cu) {
		ForStatement statement= getForStatement(cu);
		ConvertLoopOperation op= new ConvertForLoopOperation(statement);
		return op.satisfiesPreconditions().isOK();
	}

	private static ForStatement getForStatement(ICompilationUnit cu) {
		CompilationUnit ast= SharedASTProviderCore.getAST(cu, SharedASTProviderCore.WAIT_YES, new NullProgressMonitor());

		final ForStatement[] statement= new ForStatement[1];
		ast.accept(new GenericVisitor() {
			@Override
			protected boolean visitNode(ASTNode node) {
				if (node instanceof ForStatement) {
					statement[0]= (ForStatement)node;
					return false;
				} else {
					return super.visitNode(node);
				}
			}
		});

		return statement[0];
	}
}
