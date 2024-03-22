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

import static org.junit.Assert.assertTrue;

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

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ReturnTypeQuickFixTest extends QuickFixTest {
	@Rule
    public ProjectTestSetup projectSetup= new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
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
	public void testReturnTypeMissingWithSimpleType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    public foo() {
			        return (new Vector()).elements();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean addReturnType= true, doRename= true;

		for (IJavaCompletionProposal elem : proposals) {
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;

				String preview= getPreviewContent(proposal);

				String str1= """
					package test1;
					import java.util.Enumeration;
					import java.util.Vector;
					public class E {
					    public Enumeration foo() {
					        return (new Vector()).elements();
					    }
					}
					""";
				assertEqualString(preview, str1);
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				String str2= """
					package test1;
					import java.util.Vector;
					public class E {
					    public E() {
					        return (new Vector()).elements();
					    }
					}
					""";
				assertEqualString(preview, str2);
			}
		}
	}


	@Test
	public void testReturnTypeMissingWithVoid() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public foo() {
			        //do nothing
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean addReturnType= true, doRename= true;

		for (IJavaCompletionProposal elem : proposals) {
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;

				String preview= getPreviewContent(proposal);

				String str1= """
					package test1;
					public class E {
					    public void foo() {
					        //do nothing
					    }
					}
					""";
				assertEqualString(preview, str1);
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				String str2= """
					package test1;
					public class E {
					    public E() {
					        //do nothing
					    }
					}
					""";
				assertEqualString(preview, str2);
			}
		}

	}


	@Test
	public void testReturnTypeMissing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface E {
			    public foo();
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public interface E {
			    public void foo();
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}


	@Test
	public void testReturnTypeMissingWithVoid2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public foo() {
			        if (true) {
			           return;
			        }
			        return;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean addReturnType= true, doRename= true;

		for (IJavaCompletionProposal elem : proposals) {
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;

				String preview= getPreviewContent(proposal);

				String str1= """
					package test1;
					public class E {
					    public void foo() {
					        if (true) {
					           return;
					        }
					        return;
					    }
					}
					""";
				assertEqualString(preview, str1);
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				String str2= """
					package test1;
					public class E {
					    public E() {
					        if (true) {
					           return;
					        }
					        return;
					    }
					}
					""";
				assertEqualString(preview, str2);
			}
		}
	}

	@Test
	public void testVoidMissingInAnonymousType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        Runnable run= new Runnable() {
			            public void run() {
			            }
			            public foo() {
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);

		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        Runnable run= new Runnable() {
			            public void run() {
			            }
			            public void foo() {
			            }
			        };
			    }
			}
			""";
		assertEqualString(preview, str1);
	}


	@Test
	public void testReturnTypeMissingWithNull() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public foo() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean addReturnType= true, doRename= true;

		for (IJavaCompletionProposal elem : proposals) {
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;

				String preview= getPreviewContent(proposal);

				String str1= """
					package test1;
					public class E {
					    public Object foo() {
					        return null;
					    }
					}
					""";
				assertEqualString(preview, str1);
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				String str2= """
					package test1;
					public class E {
					    public E() {
					        return null;
					    }
					}
					""";
				assertEqualString(preview, str2);
			}
		}
	}

	@Test
	public void testReturnTypeMissingWithArrayType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public foo() {
			        return new int[][] { { 1, 2 }, { 2, 3 } };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		boolean addReturnType= true, doRename= true;

		for (IJavaCompletionProposal elem : proposals) {
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;

				String preview= getPreviewContent(proposal);

				String str1= """
					package test1;
					public class E {
					    public int[][] foo() {
					        return new int[][] { { 1, 2 }, { 2, 3 } };
					    }
					}
					""";
				assertEqualString(preview, str1);
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;

				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= getPreviewContent(proposal);
				String str2= """
					package test1;
					public class E {
					    public E() {
					        return new int[][] { { 1, 2 }, { 2, 3 } };
					    }
					}
					""";
				assertEqualString(preview, str2);
			}
		}
	}

	@Test
	public void testVoidMethodReturnsStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    Vector fList= new Vector();
			    public void elements() {
			        return fList.toArray();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
			String preview= getPreviewContent(proposal);

			String str1= """
				package test1;
				import java.util.Vector;
				public class E {
				    Vector fList= new Vector();
				    public Object[] elements() {
				        return fList.toArray();
				    }
				}
				""";
			assertEqualString(preview, str1);
		}
		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
			String preview= getPreviewContent(proposal);

			String str2= """
				package test1;
				import java.util.Vector;
				public class E {
				    Vector fList= new Vector();
				    public void elements() {
				        return;
				    }
				}
				""";
			assertEqualString(preview, str2);
		}
	}

	@Test
	public void testVoidMethodReturnsAnonymClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void getOperation() {
			        return new Runnable() {
			            public void run() {}
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public Runnable getOperation() {
			        return new Runnable() {
			            public void run() {}
			        };
			    }
			}
			""";

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void getOperation() {
			        return;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testReturnTypeMissingWithWildcardSuper() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public x(ArrayList<? super Number> b) {
			        return b.get(0);
			    }
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
			import java.util.ArrayList;
			public class E {
			    public Object x(ArrayList<? super Number> b) {
			        return b.get(0);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public E(ArrayList<? super Number> b) {
			        return b.get(0);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testReturnTypeMissingWithWildcardExtends() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public x() {
			        ArrayList<? extends Number> b= null;
			        return b.get(0);
			    }
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
			import java.util.ArrayList;
			public class E {
			    public Number x() {
			        ArrayList<? extends Number> b= null;
			        return b.get(0);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public E() {
			        ArrayList<? extends Number> b= null;
			        return b.get(0);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testConstructorReturnsValue() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public E() {
			        return System.getProperties();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
			String preview= getPreviewContent(proposal);

			String str1= """
				package test1;
				
				import java.util.Properties;
				
				public class E {
				    public Properties E() {
				        return System.getProperties();
				    }
				}
				""";
			assertEqualString(preview, str1);
		}
		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
			String preview= getPreviewContent(proposal);

			String str2= """
				package test1;
				public class E {
				    public E() {
				        return;
				    }
				}
				""";
			assertEqualString(preview, str2);
		}
	}

	@Test
	public void testVoidMethodReturnsWildcardSuper() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public void getIt(ArrayList<? super Number> b) {
			        return b.get(0);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public Object getIt(ArrayList<? super Number> b) {
			        return b.get(0);
			    }
			}
			""";

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public void getIt(ArrayList<? super Number> b) {
			        return;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testVoidMethodReturnsWildcardExtends() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public void getIt(ArrayList<? extends Number> b) {
			        return b.get(0);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public Number getIt(ArrayList<? extends Number> b) {
			        return b.get(0);
			    }
			}
			""";

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.ArrayList;
			public class E {
			    public void getIt(ArrayList<? extends Number> b) {
			        return;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}


	@Test
	public void testCorrectReturnStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public Runnable getOperation() {
			        return;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public Runnable getOperation() {
			        return null;
			    }
			}
			""";

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void getOperation() {
			        return;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testCorrectReturnStatementInConditional() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String input= """
			package test1;
			
			import java.util.Map;
			
			public class E {
			    <K1, K2, V> V foo(K1 key1, Map<K1, Map<K2, V>> map) {
			        Map<K2, V> map2 = map.get(key1);
			        return map2 == null ? null : map2.entrySet();
			    }
			}
			"""; //
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", input, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		List<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			
			import java.util.Map;
			
			public class E {
			    <K1, K2, V> V foo(K1 key1, Map<K1, Map<K2, V>> map) {
			        Map<K2, V> map2 = map.get(key1);
			        return (V) (map2 == null ? null : map2.entrySet());
			    }
			}
			"""; //

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.util.Map;
			import java.util.Map.Entry;
			import java.util.Set;
			
			public class E {
			    <K1, K2, V> Set<Entry<K2, V>> foo(K1 key1, Map<K1, Map<K2, V>> map) {
			        Map<K2, V> map2 = map.get(key1);
			        return map2 == null ? null : map2.entrySet();
			    }
			}
			"""; //

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testCorrectReturnStatementInChainedConditional() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);

		String input= """
			package test1;
			
			import java.util.Map;
			
			public class E {
			    <K1, K2, V> V foo(K1 key1, Map<K1, Map<K2, V>> aMap) {
			        Map<K2, V> newMap = aMap.get(key1);
			        return newMap == null ? null : aMap == null ? null : newMap.entrySet();
			    }
			}
			"""; //
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", input, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		List<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			
			import java.util.Map;
			
			public class E {
			    <K1, K2, V> V foo(K1 key1, Map<K1, Map<K2, V>> aMap) {
			        Map<K2, V> newMap = aMap.get(key1);
			        return (V) (newMap == null ? null : aMap == null ? null : newMap.entrySet());
			    }
			}
			"""; //

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.util.Map;
			import java.util.Map.Entry;
			import java.util.Set;
			
			public class E {
			    <K1, K2, V> Set<Entry<K2, V>> foo(K1 key1, Map<K1, Map<K2, V>> aMap) {
			        Map<K2, V> newMap = aMap.get(key1);
			        return newMap == null ? null : aMap == null ? null : newMap.entrySet();
			    }
			}
			"""; //

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testReturnVoid() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    /**
			     * Does it.
			     */
			    public void foo() {
			    }
			    public int goo() {
			        return foo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    /**
			     * Does it.
			     * @return\s
			     */
			    public int foo() {
			    }
			    public int goo() {
			        return foo();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testCorrectReturnStatementForArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int[][] getArray() {
			        return;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public int[][] getArray() {
			        return null;
			    }
			}
			""";

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void getArray() {
			        return;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMethodWithConstructorName() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int[][] E() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str1= """
			package test1;
			public class E {
			    public E() {
			        return null;
			    }
			}
			""";
		assertEqualString(preview, str1);
	}

	@Test
	public void testMissingReturnStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public int[][] getArray() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public int[][] getArray() {
			        return null;
			    }
			}
			""";

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void getArray() {
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMissingReturnStatementWithCode() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int[] fArray;
			    public int getArray()[] {
			        if (true) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private int[] fArray;
			    public int getArray()[] {
			        if (true) {
			        }
			        return fArray;
			    }
			}
			""";

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    private int[] fArray;
			    public void getArray()[] {
			        if (true) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMissingReturnStatementWithCode2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public boolean isVisible() {
			        boolean res= false;
			        if (true) {
			            res= false;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public boolean isVisible() {
			        boolean res= false;
			        if (true) {
			            res= false;
			        }
			        return res;
			    }
			}
			""";

		proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void isVisible() {
			        boolean res= false;
			        if (true) {
			            res= false;
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testMissingReturnStatementWithCode3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public String getDebugInfo() {
			        getClass().getName();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public String getDebugInfo() {
			        return getClass().getName();
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testMissingReturnStatementWithCode4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public String getDebugInfo() {
			        getClass().notify();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public String getDebugInfo() {
			        getClass().notify();
			        return null;
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}

	@Test
	public void testMethodInEnum_bug239887() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			enum Bug {X;
			        wrap(){}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Bug.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package p;
			enum Bug {X;
			        void wrap(){}
			}
			""";

		expected[1]= """
			package p;
			enum Bug {X;
			        Bug(){}
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}
}
