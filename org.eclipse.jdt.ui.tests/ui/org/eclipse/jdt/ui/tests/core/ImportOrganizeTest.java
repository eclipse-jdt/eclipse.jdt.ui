/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     John Glassmyer <jogl@google.com> - import group sorting is broken - https://bugs.eclipse.org/430303
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class ImportOrganizeTest extends CoreTests {

	private IJavaProject fJProject1;

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	@Before
	public void setUp() throws Exception {
		fJProject1= pts.getProject();

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		JavaCore.setOptions(options);
	}

	@After
	public void tearDown() throws Exception {
		setOrganizeImportSettings(null, 99, 99, fJProject1);
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	protected IChooseImportQuery createQuery(final String name, final String[] choices, final int[] nEntries) {
		return (openChoices, ranges) -> {
			assertEquals(name + "-query-nchoices1", choices.length, openChoices.length);
			assertEquals(name + "-query-nchoices2", nEntries.length, openChoices.length);
			for (int i1= 0; i1 < nEntries.length; i1++) {
				assertEquals(name + "-query-cnt" + i1, openChoices[i1].length, nEntries[i1]);
			}
			TypeNameMatch[] res= new TypeNameMatch[openChoices.length];
			for (int i2= 0; i2 < openChoices.length; i2++) {
				TypeNameMatch[] selection= openChoices[i2];
				assertNotNull(name + "-query-setset" + i2, selection);
				assertTrue(name + "-query-setlen" + i2, selection.length > 0);
				TypeNameMatch found= null;
				for (TypeNameMatch s : selection) {
					if (s.getFullyQualifiedName().equals(choices[i2])) {
						found= s;
					}
				}
				assertNotNull(name + "-query-notfound" + i2, found);
				res[i2]= found;
			}
			return res;
		};
	}

	protected void assertImports(ICompilationUnit cu, String[] imports) throws Exception {
		IImportDeclaration[] desc= cu.getImports();
		assertEquals(cu.getElementName() + "-count", imports.length, desc.length);
		for (int i= 0; i < imports.length; i++) {
			assertEquals(cu.getElementName() + "-cmpentries" + i, desc[i].getElementName(), imports[i]);
		}
	}

	@Test
	public void test1() throws Exception {
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
		assertNotNull("junit src not found", junitSrcArchive);
		assertTrue("junit src not found", junitSrcArchive.exists());

		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);

		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/runner/BaseTestRunner.java"));
		assertNotNull("BaseTestRunner.java", cu);

		IPackageFragmentRoot root= (IPackageFragmentRoot)cu.getParent().getParent();
		IPackageFragment pack= root.createPackageFragment("mytest", true, null);

		ICompilationUnit colidingCU= pack.getCompilationUnit("TestListener.java");
		colidingCU.createType("public abstract class TestListener {\n}\n", null, true, null);


		String[] order= new String[0];
		IChooseImportQuery query= createQuery("BaseTestRunner", new String[] { "junit.framework.TestListener" }, new int[] { 2 });

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {
			"java.io.BufferedReader",
			"java.io.File",
			"java.io.FileInputStream",
			"java.io.FileOutputStream",
			"java.io.IOException",
			"java.io.InputStream",
			"java.io.PrintWriter",
			"java.io.StringReader",
			"java.io.StringWriter",
			"java.lang.reflect.InvocationTargetException",
			"java.lang.reflect.Method",
			"java.lang.reflect.Modifier",
			"java.text.NumberFormat",
			"java.util.Properties",
			"junit.framework.AssertionFailedError",
			"junit.framework.Test",
			"junit.framework.TestListener",
			"junit.framework.TestSuite"
		});
	}

	@Test
	public void test1WithOrder() throws Exception {
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
		assertNotNull("junit src not found", junitSrcArchive);
		assertTrue("junit src not found", junitSrcArchive.exists());

		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);

		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/runner/BaseTestRunner.java"));
		assertNotNull("BaseTestRunner.java", cu);

		IPackageFragmentRoot root= (IPackageFragmentRoot)cu.getParent().getParent();
		IPackageFragment pack= root.createPackageFragment("mytest", true, null);

		ICompilationUnit colidingCU= pack.getCompilationUnit("TestListener.java");
		colidingCU.createType("public abstract class TestListener {\n}\n", null, true, null);


		String[] order= new String[] { "junit", "java.text", "java.io", "java" };
		IChooseImportQuery query= createQuery("BaseTestRunner", new String[] { "junit.framework.TestListener" }, new int[] { 2 });

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {
			"junit.framework.AssertionFailedError",
			"junit.framework.Test",
			"junit.framework.TestListener",
			"junit.framework.TestSuite",
			"java.text.NumberFormat",
			"java.io.BufferedReader",
			"java.io.File",
			"java.io.FileInputStream",
			"java.io.FileOutputStream",
			"java.io.IOException",
			"java.io.InputStream",
			"java.io.PrintWriter",
			"java.io.StringReader",
			"java.io.StringWriter",
			"java.lang.reflect.InvocationTargetException",
			"java.lang.reflect.Method",
			"java.lang.reflect.Modifier",
			"java.util.Properties"
		});
	}


	@Test
	public void test2() throws Exception {
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
		assertNotNull("junit src not found", junitSrcArchive);
		assertTrue("junit src not found", junitSrcArchive.exists());

		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);

		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/runner/LoadingTestCollector.java"));
		assertNotNull("LoadingTestCollector.java", cu);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("LoadingTestCollector", new String[] { }, new int[] { });

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {
			"java.lang.reflect.Modifier",
			"junit.framework.Test",
			"junit.framework.TestSuite",
		});
	}


	@Test
	public void test3() throws Exception {
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
		assertNotNull("junit src not found", junitSrcArchive);
		assertTrue("junit src not found", junitSrcArchive.exists());

		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);

		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/runner/TestCaseClassLoader.java"));
		assertNotNull("TestCaseClassLoader.java", cu);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("TestCaseClassLoader", new String[] { }, new int[] { });

		OrganizeImportsOperation op= createOperation(cu, order, 3, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {
			"java.io.*",
			"java.net.URL",
			"java.util.*",
			"java.util.zip.ZipEntry",
			"java.util.zip.ZipFile",
		});
	}

	@Test
	public void test4() throws Exception {
		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC_381);
		assertNotNull("junit src not found", junitSrcArchive);
		assertTrue("junit src not found", junitSrcArchive.exists());

		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", junitSrcArchive, JavaProjectHelper.JUNIT_SRC_ENCODING);

		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/textui/TestRunner.java"));
		assertNotNull("TestRunner.java", cu);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("TestRunner", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {
			"java.io.PrintStream",
			"junit.framework.Test",
			"junit.framework.TestResult",
			"junit.framework.TestSuite",
			"junit.runner.BaseTestRunner",
			"junit.runner.StandardTestSuiteLoader",
			"junit.runner.TestSuiteLoader",
			"junit.runner.Version"
		});
	}

	@Test
	public void testVariousTypeReferences() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack= sourceFolder.createPackageFragment("test", false, null);
		for (int ch= 'A'; ch < 'M'; ch++) {
			String name= String.valueOf((char) ch);
			ICompilationUnit cu= pack.getCompilationUnit(name + ".java");
			String content= "public class " + name + " {}";
			cu.createType(content, null, false, null);
		}
		for (int ch= 'A'; ch < 'M'; ch++) {
			String name= "I" + String.valueOf((char) ch);
			ICompilationUnit cu= pack.getCompilationUnit(name + ".java");
			String content= "public interface " + name + " {}";
			cu.createType(content, null, false, null);
		}

		String str= """
			public class ImportTest extends A implements IA, IB {
			  private B fB;
			  private Object fObj= new C();
			  public IB foo(IC c, ID d) throws IOException {
			   Object local= (D) fObj;
			   if (local instanceof E) {};
			   return null;
			  }
			}
			""";
		pack= sourceFolder.createPackageFragment("other", false, null);
		ICompilationUnit cu= pack.getCompilationUnit("ImportTest.java");
		cu.createType(str, null, false, null);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("ImportTest", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {
			"java.io.IOException",
			"test.A",
			"test.B",
			"test.C",
			"test.D",
			"test.E",
			"test.IA",
			"test.IB",
			"test.IC",
			"test.ID",
		});
	}

	@Test
	public void testInnerClassVisibility() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class C {
			  protected static class C1 {
			    public static class C2 {
			    }
			  }
			}
			""";
		pack1.createCompilationUnit("C.java", str, false, null);

		IPackageFragment pack2= sourceFolder.createPackageFragment("test2", false, null);

		String str1= """
			package test2;
			import test2.A.A1;
			import test2.A.A1.A2;
			import test2.A.A1.A2.A3;
			import test2.A.B1;
			import test2.A.B1.B2;
			import test1.C;
			import test1.C.C1.C2;
			public class A {
			    public static class A1 {
			        public static class A2 {
			            public static class A3 {
			            }
			        }
			    }
			    public static class B1 {
			        public static class B2 {
			        }
			        public static class B3 {
			            public static class B4 extends C {
			                B4 b4;
			                B3 b3;
			                B2 b2;
			                B1 b1;
			                A1 a1;
			                A2 a2;
			                A3 a3;
			                C1 c1;
			                C2 c2;
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu2= pack2.createCompilationUnit("A.java", str1, false, null);


		String[] order= new String[0];
		IChooseImportQuery query= createQuery("A", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu2, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu2, new String[] {
			"test1.C",
			"test1.C.C1.C2",
			"test2.A.A1.A2",
			"test2.A.A1.A2.A3"
		});
	}


	@Test
	public void testClearImports() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class C {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[0];
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package test1;
			
			public class C {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testNewImports() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class C extends Vector {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[0];
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package test1;
			
			import java.util.Vector;
			
			public class C extends Vector {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testReplaceImports() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			import java.util.Set;
			
			public class C extends Vector {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[0];
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package test1;
			
			import java.util.Vector;
			
			public class C extends Vector {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testRestoreExistingImports() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			import java.util.Properties;
			import java.io.File;
			import java.io.FileInputStream;
			
			public class C extends Vector {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[0];
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query, true);
		op.run(null);

		String str1= """
			package test1;
			
			import java.util.Properties;
			import java.util.Vector;
			import java.io.File;
			import java.io.FileInputStream;
			
			public class C extends Vector {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testClearImportsNoPackage() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.getPackageFragment("");
		String str= """
			import java.util.Vector;
			public class C {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[0];
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			public class C {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testNewImportsNoPackage() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.getPackageFragment("");
		String str= """
			public class C extends Vector {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[0];
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			import java.util.Vector;
			
			public class C extends Vector {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testReplaceImportsNoPackage() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.getPackageFragment("");
		String str= """
			import java.util.Set;
			
			public class C extends Vector {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[0];
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			import java.util.Vector;
			
			public class C extends Vector {
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testCommentAfterImport() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;\r
			\r
			import x;\r
			import java.util.Vector; // comment\r
			\r
			public class C {\r
			    Vector v;\r
			}\r
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[0];
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;\r
			\r
			import java.util.Vector; // comment\r
			\r
			public class C {\r
			    Vector v;\r
			}\r
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testImportToStar() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class List {
			}
			""";
		pack2.createCompilationUnit("List.java", str, false, null);


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			
			import java.util.Set;
			import java.util.Vector;
			import java.util.Map;
			
			import pack.List;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List v4;
			    String v5;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str1, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 2, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;
			
			import java.util.*;
			
			import pack.List;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List v4;
			    String v5;
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testImportToStarWithComments() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class List {
			}
			""";
		pack2.createCompilationUnit("List.java", str, false, null);


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			
			// comment 1
			/*lead 1*/import java.util.Set;//test1
			/*lead 2*/ import java.util.Vector;/*test2*/
			/**lead 3*/import java.util.Map; //test3
			/**comment 2*/
			import pack.List;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List v4;
			    String v5;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str1, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 2, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;
			
			// comment 1
			/*lead 1*/
			//test1
			/*lead 2*/
			/*test2*/
			/**lead 3*/
			//test3
			import java.util.*;
			
			/**comment 2*/
			import pack.List;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List v4;
			    String v5;
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testImportToStarWithExplicit() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class List {
			}
			""";
		pack2.createCompilationUnit("List.java", str, false, null);

		String str1= """
			package pack;
			public class List2 {
			}
			""";
		pack2.createCompilationUnit("List2.java", str1, false, null);

		String str2= """
			package pack;
			public class List3 {
			}
			""";
		pack2.createCompilationUnit("List3.java", str2, false, null);


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str3= """
			package pack1;
			
			import java.util.Set;
			import java.util.Vector;
			import java.util.Map;
			
			import pack.List;
			import pack.List2;
			import pack.List3;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List v4;
			    List2 v5;
			    List3 v6;
			    String v7;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str3, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		// Setting on-demand threshold to 2 ensures that the 2 reducible imports (pack.List2 and
		// pack.List3) will be reduced into an on-demand import.
		OrganizeImportsOperation op= createOperation(cu, order, 2, false, true, true, query);
		op.run(null);

		String str4= """
			package pack1;
			
			import java.util.*;
			
			import pack.*;
			import pack.List;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List v4;
			    List2 v5;
			    List3 v6;
			    String v7;
			}
			""";
		assertEqualString(cu.getSource(), str4);
	}

	@Test
	public void testImportToStarWithExplicit2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class List {
			}
			""";
		pack2.createCompilationUnit("List.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			
			import java.util.Set;
			import java.util.Vector;
			import java.util.Map;
			
			import pack.List;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List v4;
			    String v6;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str1, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 1, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;
			
			import java.util.*;
			
			import pack.List;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List v4;
			    String v6;
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testImportToStarWithExplicit3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class List {
			}
			""";
		pack2.createCompilationUnit("List.java", str, false, null);

		String str1= """
			package pack;
			public class Set {
			}
			""";
		pack2.createCompilationUnit("Set.java", str1, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str2= """
			package pack1;
			
			import java.util.Set;
			import java.util.Vector;
			import java.util.Map;
			
			import pack.List;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List v4;
			    String v6;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str2, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 1, false, true, true, query);
		op.run(null);

		String str3= """
			package pack1;
			
			import java.util.*;
			import java.util.Set;
			
			import pack.List;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List v4;
			    String v6;
			}
			""";
		assertEqualString(cu.getSource(), str3);
	}

	@Test
	public void testImportToStarWithExplicit4() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class List {
			}
			""";
		pack2.createCompilationUnit("List.java", str, false, null);

		IPackageFragment pack3= sourceFolder.createPackageFragment("pack3", false, null);
		String str1= """
			package pack3;
			public class List {
			}
			""";
		pack3.createCompilationUnit("List.java", str1, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str2= """
			package pack1;
			
			import java.util.Set;
			import java.util.Vector;
			import java.util.Map;
			
			import pack.List;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List v4;
			    String v6;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str2, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 1, false, true, true, query);
		op.run(null);

		String str3= """
			package pack1;
			
			import java.util.*;
			
			import pack.List;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List v4;
			    String v6;
			}
			""";
		assertEqualString(cu.getSource(), str3);
	}



	@Test
	public void testImportToStarWithExplicit5() throws Exception {


		// unrelated project, to fill the all types cache
		IJavaProject project2 = JavaProjectHelper.createJavaProject("TestProject2", "bin");
		try {
			assertNotNull("rt not found", JavaProjectHelper.addRTJar(project2));
			IPackageFragmentRoot sourceFolder2= JavaProjectHelper.addSourceContainer(project2, "src");

			IPackageFragment pack22= sourceFolder2.createPackageFragment("packx", false, null);
			String str= """
				package pack;
				public class Vector {
				}
				""";
			pack22.createCompilationUnit("List.java", str, false, null);

			String str1= """
				package pack;
				public class Set {
				}
				""";
			pack22.createCompilationUnit("Set.java", str1, false, null);

			IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

			IPackageFragment pack2= sourceFolder.createPackageFragment("pack", false, null);
			String str2= """
				package pack;
				public class List {
				}
				""";
			pack2.createCompilationUnit("List.java", str2, false, null);

			IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
			String str3= """
				package pack1;
				
				import java.util.Set;
				import java.util.Vector;
				import java.util.Map;
				
				import pack.List;
				
				public class C {
				    Vector v;
				    Set v2;
				    Map v3;
				    List v4;
				    String v6;
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("C.java", str3, false, null);


			String[] order= new String[] { "java", "pack" };
			IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

			OrganizeImportsOperation op= createOperation(cu, order, 1, false, true, true, query);
			op.run(null);

			String str4= """
				package pack1;
				
				import java.util.*;
				
				import pack.List;
				
				public class C {
				    Vector v;
				    Set v2;
				    Map v3;
				    List v4;
				    String v6;
				}
				""";
			assertEqualString(cu.getSource(), str4);
		} finally {
			JavaProjectHelper.delete(project2);
		}
	}


	@Test
	public void testImportFromDefault() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("", false, null);
		String str= """
			public class List1 {
			}
			""";
		pack2.createCompilationUnit("List1.java", str, false, null);


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			
			import java.util.Set;
			import java.util.Vector;
			import java.util.Map;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List1 v4;
			    String v5;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str1, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 2, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;
			
			import java.util.*;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List1 v4;
			    String v5;
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testImportFromDefaultWithStar() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("", false, null);
		String str= """
			public class List1 {
			}
			""";
		pack2.createCompilationUnit("List1.java", str, false, null);

		String str1= """
			public class List2 {
			}
			""";
		pack2.createCompilationUnit("List2.java", str1, false, null);


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str2= """
			package pack1;
			
			import java.util.Set;
			import java.util.Vector;
			import java.util.Map;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List1 v4;
			    List2 v5;
			    String v6;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str2, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 2, false, true, true, query);
		op.run(null);

		String str3= """
			package pack1;
			
			import java.util.*;
			
			public class C {
			    Vector v;
			    Set v2;
			    Map v3;
			    List1 v4;
			    List2 v5;
			    String v6;
			}
			""";
		assertEqualString(cu.getSource(), str3);
	}

	@Test
	public void testImportOfMemberFromLocal() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			public class C {
			    public void foo() {
			        class Local {
			            class LocalMember {
			            }
			            LocalMember x;
			            Vector v;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			import java.util.Vector;
			
			public class C {
			    public void foo() {
			        class Local {
			            class LocalMember {
			            }
			            LocalMember x;
			            Vector v;
			        }
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testGroups1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			public class List1 {
			}
			""";
		pack2.createCompilationUnit("List1.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			
			public class C {
			    File f;
			    IOException f1;
			    RandomAccessFile f2;
			    ArrayList f3;
			    List1 f4;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str1, false, null);


		String[] order= new String[] { "java.io", "java.util" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;
			
			import java.io.File;
			import java.io.IOException;
			import java.io.RandomAccessFile;
			
			import java.util.ArrayList;
			
			import pack0.List1;
			
			public class C {
			    File f;
			    IOException f1;
			    RandomAccessFile f2;
			    ArrayList f3;
			    List1 f4;
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testBaseGroups1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			public class List1 {
			}
			""";
		pack2.createCompilationUnit("List1.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			
			public class C {
			    File f;
			    IOException f1;
			    RandomAccessFile f2;
			    ArrayList f3;
			    List1 f4;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str1, false, null);


		String[] order= new String[] { "java", "java.io" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;
			
			import java.util.ArrayList;
			
			import java.io.File;
			import java.io.IOException;
			import java.io.RandomAccessFile;
			
			import pack0.List1;
			
			public class C {
			    File f;
			    IOException f1;
			    RandomAccessFile f2;
			    ArrayList f3;
			    List1 f4;
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testVisibility_bug26746() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			public interface MyInterface {
				public interface MyInnerInterface {
				}
			}
			""";
		pack2.createCompilationUnit("MyInterface.java", str, false, null);

		String str1= """
			package pack0;
			
			import pack0.MyInterface.MyInnerInterface;
			public class MyClass implements MyInterface {
				public MyInnerInterface myMethod() {
					return null;
				}
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("MyClass.java", str1, false, null);


		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack0;
			
			public class MyClass implements MyInterface {
				public MyInnerInterface myMethod() {
					return null;
				}
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testVisibility_bug37299a() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public class ClusterSingletonStepped {
				public interface SingletonStep {
				}
			}
			""";
		pack1.createCompilationUnit("ClusterSingletonStepped.java", str, false, null);

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack0", false, null);
		String str1= """
			package pack0;
			
			import pack1.ClusterSingletonStepped;
			import pack1.ClusterSingletonStepped.SingletonStep;
			
			public class TestFile extends ClusterSingletonStepped implements SingletonStep {
			    SingletonStep step;
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("TestFile.java", str1, false, null);


		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("TestFile", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack0;
			
			import pack1.ClusterSingletonStepped;
			import pack1.ClusterSingletonStepped.SingletonStep;
			
			public class TestFile extends ClusterSingletonStepped implements SingletonStep {
			    SingletonStep step;
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testVisibility_bug37299b() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public class ClusterSingletonStepped {
				public interface SingletonStep {
				}
			}
			""";
		pack1.createCompilationUnit("ClusterSingletonStepped.java", str, false, null);

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack0", false, null);
		String str1= """
			package pack0;
			
			import pack1.ClusterSingletonStepped;
			import pack1.ClusterSingletonStepped.SingletonStep;
			
			public class TestFile extends ClusterSingletonStepped {
			    SingletonStep step;
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("TestFile.java", str1, false, null);


		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("TestFile", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack0;
			
			import pack1.ClusterSingletonStepped;
			
			public class TestFile extends ClusterSingletonStepped {
			    SingletonStep step;
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testVisibility_bug56704() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			public class A {
				public class AX {
				}
			}
			""";
		pack2.createCompilationUnit("A.java", str, false, null);

		String str1= """
			package pack0;
			
			import pack0.A.AX;
			public class B extends A {
				public class BX extends AX {
				}
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("B.java", str1, false, null);


		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack0;
			
			public class B extends A {
				public class BX extends AX {
				}
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testVisibility_bug67644() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public class A {
				public class AX {
				}
			}
			""";
		pack1.createCompilationUnit("A.java", str, false, null);

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack2", false, null);

		String str1= """
			package pack2;
			
			import pack1.A;
			import pack1.AX;
			public class B {
				public void foo() {
				  Object x= new A().new AX();
				}
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("B.java", str1, false, null);


		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("testVisibility_bug67644", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack2;
			
			import pack1.A;
			public class B {
				public void foo() {
				  Object x= new A().new AX();
				}
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testVisibility_bug85831() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack2", false, null);

		String str= """
			package pack2;
			
			class A {
				public class AX {
				}
			}
			public class B {
				Object x= new A().new AX();
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("B.java", str, false, null);


		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("testVisibility_bug85831", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack2;
			
			class A {
				public class AX {
				}
			}
			public class B {
				Object x= new A().new AX();
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}


	@Test
	public void testVisibility_bug79174() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			public interface A<X> {
				public interface AX<Y> {
				}
			}
			""";
		pack1.createCompilationUnit("A.java", str, false, null);

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack2", false, null);

		String str1= """
			package pack2;
			
			import pack1.A;
			import pack1.AX;
			public class B implements A<String> {
				public void foo(AX<String> a) {
				}
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("B.java", str1, false, null);


		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("testVisibility_bug79174", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack2;
			
			import pack1.A;
			public class B implements A<String> {
				public void foo(AX<String> a) {
				}
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}


	@Test
	public void testVisibility_bug131305() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment packUtil= sourceFolder.createPackageFragment("util", false, null);
		String str= """
			package util;
			
			public interface Map\s
			        public static interface Entry {
			        }
			}
			""";
		packUtil.createCompilationUnit("Map.java", str, false, null);

		String str1= """
			package util;
			
			public interface HashMap implements Map {
			        private static interface Entry {
			        }
			}
			""";
		packUtil.createCompilationUnit("HashMap.java", str1, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str2= """
			package pack1;
			
			import util.HashMap;
			import util.Map;
			import util.Map.Entry;
			
			public class A extends HashMap {
			        public A(Map m, Entry e) {
			        }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str2, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("testVisibility_bug131305", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertEqualString(cu.getSource(), str2); // no changes, import for Entry is required
	}

	@Test
	public void testVisibility_bug159638() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			public abstract class Parent<E> {
			    public static class Inner {
			    }
			    public @interface Tag{
			        String value();
			    }
			}
			""";
		pack0.createCompilationUnit("Map.java", str, false, null);


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			
			import pack0.Parent;
			import pack0.Parent.Inner;
			import pack0.Parent.Tag;
			
			@Tag("foo")
			public class Child extends Parent<Inner> {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str1, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("testVisibility_bug159638", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertEqualString(cu.getSource(), str1); // no changes, imports for Inner and tag are required
	}


	@Test
	public void test5() throws Exception {

		String[] types= new String[] {
			"org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader",
			"org.eclipse.core.resources.IContainer",
			"org.eclipse.core.runtime.IPath",
			"org.eclipse.core.runtime.CoreException",
			"org.eclipse.core.resources.IResource",
			"org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer"
		};
		String[] order= new String[] { "org.eclipse.jdt", "org.eclipse" };

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		for (String type : types) {
			String pack= Signature.getQualifier(type);
			String name= Signature.getSimpleName(type);
			IPackageFragment pack2= sourceFolder.createPackageFragment(pack, false, null);
			StringBuilder buf= new StringBuilder();
			buf.append("package ").append(pack).append(";\n");
			buf.append("public class ").append(name).append(" {\n");
			buf.append("}\n");
			pack2.createCompilationUnit(name + ".java", buf.toString(), false, null);
		}

		StringBuilder body= new StringBuilder();
		body.append("public class C {\n");
		for (int i= 0; i < types.length; i++) {
			String name= Signature.getSimpleName(types[i]);
			body.append(name); body.append(" a"); body.append(i); body.append(";\n");
		}
		body.append("}\n");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append(body.toString());

		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;\n");
		buf.append("import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;\n");
		buf.append("\n");
		buf.append("import org.eclipse.core.resources.IContainer;\n");
		buf.append("import org.eclipse.core.resources.IResource;\n");
		buf.append("import org.eclipse.core.runtime.CoreException;\n");
		buf.append("import org.eclipse.core.runtime.IPath;\n");
		buf.append("\n");
		buf.append(body.toString());

		assertEqualString(cu.getSource(), buf.toString());
	}

	@Test
	public void test_bug25773() throws Exception {

		String[] types= new String[] {
			"java.util.Vector",
			"java.util.Map",
			"java.util.Set",
			"org.eclipse.gef.X1",
			"org.eclipse.gef.X2",
			"org.eclipse.gef.X3",
			"org.eclipse.core.runtime.IAdaptable",
			"org.eclipse.draw2d.IFigure",
			"org.eclipse.draw2d.LayoutManager",
			"org.eclipse.draw2d.geometry.Point",
			"org.eclipse.draw2d.geometry.Rectangle",
			"org.eclipse.swt.accessibility.ACC",
			"org.eclipse.swt.accessibility.AccessibleControlEvent"
		};

		String[] order= new String[] { "java", "org.eclipse", "org.eclipse.gef", "org.eclipse.draw2d", "org.eclipse.gef.examples" };
		int threshold= 3;

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		for (String type : types) {
			String pack= Signature.getQualifier(type);
			if (!pack.startsWith("java.")) {
				String name= Signature.getSimpleName(type);
				IPackageFragment pack2= sourceFolder.createPackageFragment(pack, false, null);
				StringBuilder buf= new StringBuilder();
				buf.append("package ").append(pack).append(";\n");
				buf.append("public class ").append(name).append(" {\n");
				buf.append("}\n");
				pack2.createCompilationUnit(name + ".java", buf.toString(), false, null);
			}
		}

		StringBuilder body= new StringBuilder();
		body.append("public class C {\n");
		for (int i= 0; i < types.length; i++) {
			String name= Signature.getSimpleName(types[i]);
			body.append(name); body.append(" a"); body.append(i); body.append(";\n");
		}
		body.append("}\n");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append(body.toString());

		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, threshold, false, true, true, query);
		op.run(null);

		buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import java.util.*;\n");
		buf.append("\n");
		buf.append("import org.eclipse.core.runtime.IAdaptable;\n");
		buf.append("import org.eclipse.swt.accessibility.ACC;\n");
		buf.append("import org.eclipse.swt.accessibility.AccessibleControlEvent;\n");
		buf.append("\n");
		buf.append("import org.eclipse.gef.*;\n");
		buf.append("\n");
		buf.append("import org.eclipse.draw2d.IFigure;\n");
		buf.append("import org.eclipse.draw2d.LayoutManager;\n");
		buf.append("import org.eclipse.draw2d.geometry.Point;\n");
		buf.append("import org.eclipse.draw2d.geometry.Rectangle;\n");
		buf.append("\n");
		buf.append(body.toString());
		assertEqualString(cu.getSource(), buf.toString());
	}

	@Test
	public void test_bug25113() throws Exception {

		String[] types= new String[] {
			"com.mycompany.Class1",
			"com.foreigncompany.Class2",
			"com.foreigncompany.Class3",
			"com.mycompany.Class4",
			"com.misc.Class5"
		};

		String[] order= new String[] { "com", "com.foreigncompany", "com.mycompany" };
		int threshold= 99;

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		for (String type : types) {
			String pack= Signature.getQualifier(type);
			if (!pack.startsWith("java.")) {
				String name= Signature.getSimpleName(type);
				IPackageFragment pack2= sourceFolder.createPackageFragment(pack, false, null);
				StringBuilder buf= new StringBuilder();
				buf.append("package ").append(pack).append(";\n");
				buf.append("public class ").append(name).append(" {\n");
				buf.append("}\n");
				pack2.createCompilationUnit(name + ".java", buf.toString(), false, null);
			}
		}

		StringBuilder body= new StringBuilder();
		body.append("public class C {\n");
		for (int i= 0; i < types.length; i++) {
			String name= Signature.getSimpleName(types[i]);
			body.append(name); body.append(" a"); body.append(i); body.append(";\n");
		}
		body.append("}\n");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append(body.toString());

		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, threshold, false, true, true, query);
		op.run(null);

		buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("import com.misc.Class5;\n");
		buf.append("\n");
		buf.append("import com.foreigncompany.Class2;\n");
		buf.append("import com.foreigncompany.Class3;\n");
		buf.append("\n");
		buf.append("import com.mycompany.Class1;\n");
		buf.append("import com.mycompany.Class4;\n");
		buf.append("\n");
		buf.append(body.toString());
		assertEqualString(cu.getSource(), buf.toString());
	}

	@Test
	public void testStaticImports1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import static java.lang.System.out;
			
			public class C {
			    public int foo() {
			        out.print(File.separator);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[] { "java", "pack", "#java" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			import java.io.File;
			
			import static java.lang.System.out;
			
			public class C {
			    public int foo() {
			        out.print(File.separator);
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testStaticImports2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import static java.io.File.*;
			
			public class C {
			    public String foo() {
			        return pathSeparator + separator + File.separator;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[] { "#java.io.File", "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			import static java.io.File.pathSeparator;
			import static java.io.File.separator;
			
			import java.io.File;
			
			public class C {
			    public String foo() {
			        return pathSeparator + separator + File.separator;
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testStaticImports_bug78585() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			public class Test1 {
				public static final <T> void assertNotEquals(final String msg, final T expected, final T toCheck) {
				}
			}
			""";
		pack0.createCompilationUnit("Test1.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			
			import pack0.Test1;
			import java.util.List;
			
			public class Test2 extends Test1 {
				public void testMe() {
				    assertNotEquals("A", "B", "C");
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test2.java", str1, false, null);


		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;
			
			import pack0.Test1;
			
			public class Test2 extends Test1 {
				public void testMe() {
				    assertNotEquals("A", "B", "C");
				}
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testStaticImports_bug90556() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			public class BasePanel<T extends Number> {
				public static void add2panel(String... s) {
				}
			}
			""";
		pack0.createCompilationUnit("Test1.java", str, false, null);

		String str1= """
			package pack0;
			
			public class ManufacturerMainPanel<T extends Number> extends BasePanel<T>{
				public void testMe() {
				    add2panel(null, null);
				}
			}
			""";
		ICompilationUnit cu= pack0.createCompilationUnit("ManufacturerMainPanel.java", str1, false, null);


		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("ManufacturerMainPanel", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack0;
			
			public class ManufacturerMainPanel<T extends Number> extends BasePanel<T>{
				public void testMe() {
				    add2panel(null, null);
				}
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testStaticImports_bug113770() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package test;
			
			public abstract class Test<M>
			{
			        private static Map<Object, Object[]> facetMap;
			
			        public void getFacets() {
			                facetMap.get(null);
			        }
			}
			""";
		ICompilationUnit cu= pack0.createCompilationUnit("Test.java", str, false, null);


		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("Test", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package test;
			
			import java.util.Map;
			
			public abstract class Test<M>
			{
			        private static Map<Object, Object[]> facetMap;
			
			        public void getFacets() {
			                facetMap.get(null);
			        }
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testStaticImports_bug81589() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			public enum E {
				A, B, C;
			}
			""";
		pack0.createCompilationUnit("E.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			
			import pack0.E;
			import static pack0.E.A;
			
			public class Test2 {
				public void testMe(E e) {
				    switch (e) {
				      case A:
				    }
				}
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test2.java", str1, false, null);


		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;
			
			import pack0.E;
			
			public class Test2 {
				public void testMe(E e) {
				    switch (e) {
				      case A:
				    }
				}
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testStaticImports_bug159424() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			
			import java.util.List;
			
			public abstract class B {
			    private static List logger;
			}
			""";
		pack0.createCompilationUnit("B.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);

		String str1= """
			package pack1;
			
			import java.util.List;
			import pack0.B;
			
			public abstract class A {
			    private static List logger;
			
			    protected class BSubClass extends B {
			        public void someMethod() {
			            logger.toString();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str1, false, null);


		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("testStaticImports_bug159424", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertEqualString(cu.getSource(), str1); // no changes, don't add 'logger' as static import
	}

	@Test
	public void testStaticImports_bug175498() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("p", false, null);

		String str= """
			package p;
			public class Test<T> {
			        public static enum TestEnum {
			                V1,
			                V2
			        }
			
			        public void test(final TestEnum value) {
			                switch (value) {
			                        case V1:
			                        case V2:
			                }
			        }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("testStaticImports_bug175498", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertEqualString(cu.getSource(), str); // no changes, don't add 'V1' and 'V2' as static import
	}

	@Test
	public void testStaticImports_bug181895() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package test;
			
			import static java.lang.Math.max;
			
			public class Test {
			        /**
			         * @see #max
			         */
			        public void doFoo() {
			        }
			}
			""";
		ICompilationUnit cu= pack0.createCompilationUnit("Test.java", str, false, null);


		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("Test", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package test;
			
			public class Test {
			        /**
			         * @see #max
			         */
			        public void doFoo() {
			        }
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testStaticImports_bug187004a() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("b", false, null);
		String str= """
			package b;
			
			abstract public class Parent<T> {
			        protected static final int CONSTANT = 42;
			}
			""";
		pack0.createCompilationUnit("Parent.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("a", false, null);
		String str1= """
			package a;
			
			import b.Parent;
			
			public class Child extends Parent<String> {
			        public Child() {
			                System.out.println(CONSTANT);
			        }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Child.java", str1, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("Child", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package a;
			
			import b.Parent;
			
			public class Child extends Parent<String> {
			        public Child() {
			                System.out.println(CONSTANT);
			        }
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testStaticImports_bug187004b() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("b", false, null);
		String str= """
			package b;
			
			abstract public class Parent<T> {
			        protected static final int CONSTANT() { return 42; }
			}
			""";
		pack0.createCompilationUnit("Parent.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("a", false, null);
		String str1= """
			package a;
			
			import b.Parent;
			
			public class Child extends Parent<String> {
			        public Child() {
			                System.out.println(CONSTANT());
			        }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Child.java", str1, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("Child", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package a;
			
			import b.Parent;
			
			public class Child extends Parent<String> {
			        public Child() {
			                System.out.println(CONSTANT());
			        }
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void testStaticImports_bug230067() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("a", false, null);
		String str= """
			package a;
			
			class Test<T> {
			    private static String TEST = "constant";
			
			    static class Inner extends Test<String> {
			        public void test() {
			            TEST.concat("access");
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test.java", str, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("Test", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package a;
			
			class Test<T> {
			    private static String TEST = "constant";
			
			    static class Inner extends Test<String> {
			        public void test() {
			            TEST.concat("access");
			        }
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testStaticImports_bug562641() throws Exception {
		IPreferenceStore preferenceStore= PreferenceConstants.getPreferenceStore();
		preferenceStore.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "java.lang.Math.max");
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			public class C {
			    private int foo() {
			        return max(1, 2);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("Test", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			import static java.lang.Math.max;
			
			public class C {
			    private int foo() {
			        return max(1, 2);
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testImportCountAddNew() throws Exception {
	    IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			import java.util.ArrayList;
			import java.util.HashMap;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);

		assertEquals(2, op.getNumberOfImportsAdded());
		assertEquals(0, op.getNumberOfImportsRemoved());
	}

	@Test
	public void testImportCountAddandRemove() throws Exception {
	    IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.util.*;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			import java.util.ArrayList;
			import java.util.HashMap;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);

		assertEquals(2, op.getNumberOfImportsAdded());
		assertEquals(1, op.getNumberOfImportsRemoved());
	}

	@Test
	public void testImportCountAddandRemoveWithComments() throws Exception {
	    IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			/**comment1*/
			/*lead1*/import java.util.*;// trail 1
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			/**comment1*/
			/*lead1*/
			// trail 1
			import java.util.ArrayList;
			import java.util.HashMap;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);

		assertEquals(2, op.getNumberOfImportsAdded());
		assertEquals(1, op.getNumberOfImportsRemoved());
	}

	@Test
	public void testImportCountKeepOne() throws Exception {
	    IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.util.HashMap;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			import java.util.ArrayList;
			import java.util.HashMap;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);

		assertEquals(1, op.getNumberOfImportsAdded());
		assertEquals(0, op.getNumberOfImportsRemoved());
	}

	@Test
	public void testImportCountKeepStar() throws Exception {
	    IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.util.*;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			        Collection c;
			        Socket s;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);

		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 2, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			import java.net.Socket;
			import java.util.*;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			        Collection c;
			        Socket s;
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);

		assertEquals(1, op.getNumberOfImportsAdded());
		assertEquals(0, op.getNumberOfImportsRemoved());
	}

	@Test
	public void testImportCountAddTwoRemoveOne() throws Exception {
	    IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.util.BitSet;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			import java.util.ArrayList;
			import java.util.HashMap;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);

		assertEquals(2, op.getNumberOfImportsAdded());
		assertEquals(1, op.getNumberOfImportsRemoved());
	}

	@Test
	public void testImportCountReplaceStar() throws Exception {
	    IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.util.BitSet;
			import java.util.Calendar;
			import java.util.*;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			import java.util.ArrayList;
			import java.util.HashMap;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);

		assertEquals(2, op.getNumberOfImportsAdded());
		assertEquals(3, op.getNumberOfImportsRemoved());
	}

	@Test
	public void testImportCountRemoveStatic() throws Exception {
	    IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.util.BitSet;
			// some comment;
			import java.util.Calendar; /*another comment*/
			import static java.io.File.pathSeparator;
			import java.util.*;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[] { "java", "pack" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			import java.util.ArrayList;
			import java.util.HashMap;
			
			public class C {
			    public void foo() {
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);

		assertEquals(2, op.getNumberOfImportsAdded());
		assertEquals(4, op.getNumberOfImportsRemoved());
	}

	@Test
	public void testImportCountKeepStatic() throws Exception {
	    IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			import java.util.BitSet;
			// some comment;
			import java.util.Calendar; /*another comment*/
			import static java.io.File.pathSeparator;
			import java.util.*;
			
			public class C {
			    public void foo() {
			        String s= pathSeparator;
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[] { "java", "pack", "#" };
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			import java.util.ArrayList;
			import java.util.HashMap;
			
			import static java.io.File.pathSeparator;
			
			public class C {
			    public void foo() {
			        String s= pathSeparator;
			        HashMap m;
			        ArrayList l;
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);

		assertEquals(2, op.getNumberOfImportsAdded());
		assertEquals(3, op.getNumberOfImportsRemoved());
	}

	@Test
	public void test_bug78397() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			import java.util.Collection;
			public class A {
			    Collection<java.sql.Date> foo;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack0;
			import java.util.Collection;
			public class A {
			    Collection<java.sql.Date> foo;
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void test_bug78533() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			public class A {
			    public <T extends Collection> void method1() { }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack0;
			
			import java.util.Collection;
			
			public class A {
			    public <T extends Collection> void method1() { }
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void test_bug78716() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			public enum MyEnum {
				A, B, C
			}
			""";
		pack0.createCompilationUnit("MyEnum.java", str, false, null);

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			
			import pack0.MyEnum;
			import static pack0.MyEnum.*;
			
			public class Test2 {
				MyEnum e= A;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test2.java", str1, false, null);

		String[] order= new String[] { "", "#"};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pack1;
			
			import pack0.MyEnum;
			
			import static pack0.MyEnum.A;
			
			public class Test2 {
				MyEnum e= A;
			}
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void test_bug135122() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			public class Foo extends Bar {
			  public static final int MYCONSTANT= 9;
			
			  public void anotherMethod() {
			    super.testMethod(MYCONSTANT);
			  }
			}
			
			class Bar {
			    public void testMethod(int something) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Foo.java", str, false, null);

		String[] order= new String[] { "", "#"};
		IChooseImportQuery query= createQuery("Foo", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			public class Foo extends Bar {
			  public static final int MYCONSTANT= 9;
			
			  public void anotherMethod() {
			    super.testMethod(MYCONSTANT);
			  }
			}
			
			class Bar {
			    public void testMethod(int something) {
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testIssue853() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			
			import static test.StaticImportBug.Test.*;
			
			public class StaticImportBug {
			    static public void methodWithBreakOuter () {
			        outer:
			        while (true)
			            break outer;
			    }
			
			    static public void main (String[] args) throws Throwable {
			        System.out.println(field);
			    }
			
			    static public class Test {
			        static public boolean field;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("StaticImportBug.java", str, false, null);

		String[] order= new String[] { "", "#"};
		IChooseImportQuery query= createQuery("StaticImportBug", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 1, false, true, true, query);
		op.run(null);

		String str1= """
			package test;
			
			import static test.StaticImportBug.Test.*;
			
			public class StaticImportBug {
			    static public void methodWithBreakOuter () {
			        outer:
			        while (true)
			            break outer;
			    }
			
			    static public void main (String[] args) throws Throwable {
			        System.out.println(field);
			    }
			
			    static public class Test {
			        static public boolean field;
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void test_PackageInfoBug157541a() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			@Foo
			package pack1;""";
		ICompilationUnit cu= pack1.createCompilationUnit("package-info.java", str, false, null);

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack2", false, null);
		String str1= """
			package pack2;
			public @interface Foo {
			}
			""";
		pack2.createCompilationUnit("Foo.java", str1, false, null);

		String[] order= new String[] { "", "#" };
		IChooseImportQuery query= createQuery("Foo", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			@Foo
			package pack1;
			
			import pack2.Foo;
			""";
		assertEqualString(cu.getSource(), str2);
	}

	@Test
	public void test_PackageInfoBug157541b() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			@Foo @Bar
			package pack1;
			
			import pack2.Foo;
			
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("package-info.java", str, false, null);

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack2", false, null);
		String str1= """
			package pack2;
			public @interface Foo {
			}
			""";
		pack2.createCompilationUnit("Foo.java", str1, false, null);

		String str2= """
			package pack2;
			public @interface Bar {
			}
			""";
		pack2.createCompilationUnit("Bar.java", str2, false, null);

		String[] order= new String[] { "", "#" };
		IChooseImportQuery query= createQuery("Foo", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str3= """
			@Foo @Bar
			package pack1;
			
			import pack2.Bar;
			import pack2.Foo;
			
			""";
		assertEqualString(cu.getSource(), str3);
	}

	@Test
	public void test_PackageInfoBug216432() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			/**
			 * @see Bar
			 */
			@Foo
			package pack1;""";
		ICompilationUnit cu= pack1.createCompilationUnit("package-info.java", str, false, null);

		IPackageFragment pack2= sourceFolder.createPackageFragment("pack2", false, null);
		String str1= """
			package pack2;
			public @interface Foo {
			}
			""";
		pack2.createCompilationUnit("Foo.java", str1, false, null);

		String str2= """
			package pack2;
			public @interface Bar {
			}
			""";
		pack2.createCompilationUnit("Bar.java", str2, false, null);

		String[] order= new String[] { "", "#" };
		IChooseImportQuery query= createQuery("test_PackageInfoBug216432", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str3= """
			/**
			 * @see Bar
			 */
			@Foo
			package pack1;
			
			import pack2.Foo;
			""";
		assertEqualString(cu.getSource(), str3);
	}


	@Test
	public void testTypeArgumentImports() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str= """
			package pack1;
			
			public class B {
				   public B() {
			        <File> this(null);
			    }
			    public <T> B(T t) {
			    }
			    public <T> void foo(T t) {
			        this.<Socket> foo(null);
			        new<URL> B(null);
			    }
			    class C extends B {
			        public C() {
			            <Vector> super(null);
			            super.<HashMap> foo(null);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str, false, null);

		String[] order= new String[] { "", "#"};
		IChooseImportQuery query= createQuery("B", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package pack1;
			
			import java.io.File;
			import java.net.Socket;
			import java.net.URL;
			import java.util.HashMap;
			import java.util.Vector;
			
			public class B {
				   public B() {
			        <File> this(null);
			    }
			    public <T> B(T t) {
			    }
			    public <T> void foo(T t) {
			        this.<Socket> foo(null);
			        new<URL> B(null);
			    }
			    class C extends B {
			        public C() {
			            <Vector> super(null);
			            super.<HashMap> foo(null);
			        }
			    }
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testAnnotationImports1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			public @interface MyAnnot1 {
			}
			""";
		pack0.createCompilationUnit("MyAnnot1.java", str, false, null);

		String str1= """
			package pack0;
			public @interface MyAnnot2 {
			    int value();
			}
			""";
		pack0.createCompilationUnit("MyAnnot2.java", str1, false, null);

		String str2= """
			package pack0;
			public @interface MyAnnot3 {
			}
			""";
		pack0.createCompilationUnit("MyAnnot3.java", str2, false, null);


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str3= """
			package pack1;
			
			@MyAnnot3 public class Test2 {
			    @MyAnnot1 Object e;
			    @MyAnnot2(1) void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test2.java", str3, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str4= """
			package pack1;
			
			import pack0.MyAnnot1;
			import pack0.MyAnnot2;
			import pack0.MyAnnot3;
			
			@MyAnnot3 public class Test2 {
			    @MyAnnot1 Object e;
			    @MyAnnot2(1) void foo() {
			    }
			}
			""";
		assertEqualString(cu.getSource(), str4);
	}

	@Test
	public void testAnnotationImports2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("pack0", false, null);
		String str= """
			package pack0;
			public @interface MyAnnot1 {
			}
			""";
		pack0.createCompilationUnit("MyAnnot1.java", str, false, null);

		String str1= """
			package pack0;
			public @interface MyAnnot2 {
			    char value();
			}
			""";
		pack0.createCompilationUnit("MyAnnot2.java", str1, false, null);


		IPackageFragment pack1= sourceFolder.createPackageFragment("pack1", false, null);
		String str2= """
			package pack1;
			
			@MyAnnot1()
			@MyAnnot2(File.separatorChar)
			public @interface Test2 {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Test2.java", str2, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("MyClass", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str3= """
			package pack1;
			
			import java.io.File;
			import pack0.MyAnnot1;
			import pack0.MyAnnot2;
			
			@MyAnnot1()
			@MyAnnot2(File.separatorChar)
			public @interface Test2 {
			}
			""";
		assertEqualString(cu.getSource(), str3);
	}

	@Test
	public void testJavadocImports_bug319860() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack0= sourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			import p.Main.I;
			
			/**
			 * {@link I}.
			 * @see C
			 */
			public class Main {
			    public interface I {
			    }
			    public class C {}
			}
			""";
		ICompilationUnit cu= pack0.createCompilationUnit("Main.java", str, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("JavadocImports_bug319860", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package p;
			
			/**
			 * {@link I}.
			 * @see C
			 */
			public class Main {
			    public interface I {
			    }
			    public class C {}
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void test_bug450858() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("bug", false, null);
		String str= """
			package bug;
			
			class S {
			    public final int f = 0;
			}
			
			class X {
			    class C extends S {
			        public void foo() {
			            System.out.println(C.super.f);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("S.java", str, false, null);

		String[] order= new String[] {};
		IChooseImportQuery query= createQuery("test_bug450858", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		assertImports(cu, new String[] {});
	}

	private void expectUnresolvableImportsArePreserved(
			CharSequence classContents, CharSequence resultantImports) throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment packageFragment= sourceFolder.createPackageFragment("pack", false, null);

		StringBuilder cuContents= new StringBuilder();
		cuContents.append("package pack;\n");
		cuContents.append("\n");
		cuContents.append("import static com.notfound.namesoftypes.NotFound1;\n");
		cuContents.append("import static com.notfound.namesoftypes.NotFound2;\n");
		cuContents.append("import static com.notfound.Type.*;\n");
		cuContents.append("import static com.notfound.Type.FIELD1;\n");
		cuContents.append("import static com.notfound.Type.FIELD2;\n");
		cuContents.append("import static com.notfound.Type.method1;\n");
		cuContents.append("import static com.notfound.Type.method2;\n");
		cuContents.append("\n");
		cuContents.append("import com.notfound.*;\n");
		cuContents.append("import com.notfound.NamesOfStatics.FIELD1;\n");
		cuContents.append("import com.notfound.NamesOfStatics.FIELD1;\n");
		cuContents.append("import com.notfound.NamesOfStatics.method1;\n");
		cuContents.append("import com.notfound.NamesOfStatics.method2;\n");
		cuContents.append("import com.notfound.NotFound1;\n");
		cuContents.append("import com.notfound.NotFound2;\n");
		cuContents.append("import com.notfound.OuterClass.*;\n");
		cuContents.append("import com.notfound.OuterClass.Inner1;\n");
		cuContents.append("import com.notfound.OuterClass.Inner2;\n");
		cuContents.append("\n");
		cuContents.append("public class Cu {\n");
		cuContents.append(classContents);
		cuContents.append("}\n");
		ICompilationUnit cu= packageFragment.createCompilationUnit("Cu.java", cuContents.toString(), false, null);

		createOperation(cu, new String[] {}, 99, true, true, true, null).run(null);

		StringBuilder expected= new StringBuilder();
		expected.append("package pack;\n");
		expected.append("\n");
		if (resultantImports.length() > 0) {
			expected.append(resultantImports);
			expected.append("\n");
		}
		expected.append("public class Cu {\n");
		expected.append(classContents);
		expected.append("}\n");
		assertEqualString(cu.getSource(), expected.toString());
	}

	@Test
	public void testPreserveUnresolvableTypeSingleImports() throws Exception {
		StringBuilder classContents= new StringBuilder();
		classContents.append("NotFound1 nf1;");
		classContents.append("Inner1 i1;");

		StringBuilder resultantImports= new StringBuilder();
		resultantImports.append("import com.notfound.NotFound1;\n");
		resultantImports.append("import com.notfound.OuterClass.Inner1;\n");

		expectUnresolvableImportsArePreserved(classContents, resultantImports);
	}

	@Test
	public void testPreserveUnresolvableTypeOnDemandImports() throws Exception {
		StringBuilder classContents= new StringBuilder();
		classContents.append("NotFound3 nf3;");

		StringBuilder resultantImports= new StringBuilder();
		resultantImports.append("import com.notfound.*;\n");
		resultantImports.append("import com.notfound.OuterClass.*;\n");

		expectUnresolvableImportsArePreserved(classContents, resultantImports);
	}

	@Test
	public void testPreserveUnresolvableStaticSingleImports() throws Exception {
		StringBuilder classContents= new StringBuilder();
		classContents.append("{\n");
		classContents.append("    int a= FIELD1;\n");
		classContents.append("    method1();\n");
		classContents.append("}\n");

		StringBuilder resultantImports= new StringBuilder();
		resultantImports.append("import static com.notfound.Type.FIELD1;\n");
		resultantImports.append("import static com.notfound.Type.method1;\n");

		expectUnresolvableImportsArePreserved(classContents, resultantImports);
	}

	@Test
	public void testPreserveUnresolvableStaticOnDemandImportDueToFieldReference() throws Exception {
		StringBuilder classContents= new StringBuilder();
		classContents.append("{\n");
		classContents.append("    int a= FIELD3;\n");
		classContents.append("}\n");

		StringBuilder resultantImports= new StringBuilder();
		resultantImports.append("import static com.notfound.Type.*;\n");

		expectUnresolvableImportsArePreserved(classContents, resultantImports);
	}

	@Test
	public void testPreserveUnresolvableStaticOnDemandImportDueToMethodReference() throws Exception {
		StringBuilder classContents= new StringBuilder();
		classContents.append("{\n");
		classContents.append("    method3();\n");
		classContents.append("}\n");

		StringBuilder resultantImports= new StringBuilder();
		resultantImports.append("import static com.notfound.Type.*;\n");

		expectUnresolvableImportsArePreserved(classContents, resultantImports);
	}

	@Test
	public void testPreserveUnresolvableImportRatherThanAddNewImport() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment notImportedPackage= sourceFolder.createPackageFragment("com.notimported", false, null);
		String fromEitherPackageContents= "pack com.notimported; public class FromEitherPackage {}";
		notImportedPackage.createCompilationUnit("FromEitherPackage.java", fromEitherPackageContents, false, null);
		String fromNotImportedOnlyContents= "pack com.notimported; public class FromNotImportedOnly {}";
		notImportedPackage.createCompilationUnit("FromNotImportedOnly.java", fromNotImportedOnlyContents, false, null);

		IPackageFragment packageFragment= sourceFolder.createPackageFragment("pack", false, null);

		String str= """
			package pack;
			
			import com.notfound.FromEitherPackage;
			
			public class Cu {
			    FromEitherPackage fep;
			    FromNotImportedOnly fnipo;
			}
			""";
		ICompilationUnit cu= packageFragment.createCompilationUnit("Cu.java", str, false, null);

		createOperation(cu, new String[] {}, 99, true, true, true, null).run(null);

		// FromEitherPackage is imported from com.notimported, instead of preserving the existing
		// unresolvable import from com.notfound.
		String str1= """
			package pack;
			
			import com.notimported.FromEitherPackage;
			import com.notimported.FromNotImportedOnly;
			
			public class Cu {
			    FromEitherPackage fep;
			    FromNotImportedOnly fnipo;
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testDealWithBrokenStaticImport() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			import static Broken;
			
			public class C{
			    int i = Broken;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", str, false, null);


		String[] order= new String[0];
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu, order, 99, false, true, true, query);
		op.run(null);

		String str1= """
			package test1;
			
			public class C{
			    int i = Broken;
			}
			""";
		assertEqualString(cu.getSource(), str1);
	}

	@Test
	public void testBug508660() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface ISomeInterface {
			    class FirstInnerClass {}
			    public class SecondInnerClass {}
			}
			""";
		pack1.createCompilationUnit("ISomeInterface.java", str, false, null);

		IPackageFragment pack2= sourceFolder.createPackageFragment("test2", false, null);
		String str1= """
			package test2;
			public class Test {
			    private FirstInnerClass first;
			    private SecondInnerClass second;
			}
			""";
		ICompilationUnit cu2= pack2.createCompilationUnit("Test.java", str1, false, null);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("C", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu2, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package test2;
			
			import test1.ISomeInterface.FirstInnerClass;
			import test1.ISomeInterface.SecondInnerClass;
			
			public class Test {
			    private FirstInnerClass first;
			    private SecondInnerClass second;
			}
			""";
		assertEqualString(cu2.getSource(), str2);
	}

	@Test
	public void testBug530193() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragmentRoot testSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src-tests", new Path[0], new Path[0], "bin-tests",
				new IClasspathAttribute[] { JavaCore.newClasspathAttribute(IClasspathAttribute.TEST, "true") });

		IPackageFragment pack1= sourceFolder.createPackageFragment("pp", false, null);
		String str= """
			package pp;
			public class C1 {
			    Tests at=new Tests();
			}
			""";
		ICompilationUnit cu1= pack1.createCompilationUnit("C1.java", str, false, null);

		IPackageFragment pack2= testSourceFolder.createPackageFragment("pt", false, null);
		String str1= """
			package pt;
			public class Tests {
			}
			""";
		pack2.createCompilationUnit("Tests.java", str1, false, null);

		String[] order= new String[0];
		IChooseImportQuery query= createQuery("T", new String[] {}, new int[] {});

		OrganizeImportsOperation op= createOperation(cu1, order, 99, false, true, true, query);
		op.run(null);

		String str2= """
			package pp;
			public class C1 {
			    Tests at=new Tests();
			}
			""";
		assertEqualString(cu1.getSource(), str2);
	}

	protected OrganizeImportsOperation createOperation(ICompilationUnit cu, String[] order, int threshold, boolean ignoreLowerCaseNames, boolean save, boolean allowSyntaxErrors, IChooseImportQuery chooseImportQuery) {
		setOrganizeImportSettings(order, threshold, threshold, cu.getJavaProject());
		return new OrganizeImportsOperation(cu, null, ignoreLowerCaseNames, save, allowSyntaxErrors, chooseImportQuery);
	}

	protected OrganizeImportsOperation createOperation(ICompilationUnit cu, String[] order, int threshold, boolean ignoreLowerCaseNames, boolean save, boolean allowSyntaxErrors, IChooseImportQuery chooseImportQuery, boolean restoreExistingImports) {
		setOrganizeImportSettings(order, threshold, threshold, cu.getJavaProject());
		return new OrganizeImportsOperation(cu, null, ignoreLowerCaseNames, save, allowSyntaxErrors, chooseImportQuery, restoreExistingImports);
	}

	protected void setOrganizeImportSettings(String[] order, int threshold, int staticThreshold, IJavaProject project) {
		IEclipsePreferences scope= new ProjectScope(project.getProject()).getNode(JavaUI.ID_PLUGIN);
		if (order == null) {
			scope.remove(PreferenceConstants.ORGIMPORTS_IMPORTORDER);
			scope.remove(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD);
		} else {
			StringBuilder buf= new StringBuilder();
			for (String o : order) {
				buf.append(o);
				buf.append(';');
			}
			scope.put(PreferenceConstants.ORGIMPORTS_IMPORTORDER, buf.toString());
			scope.put(PreferenceConstants.ORGIMPORTS_ONDEMANDTHRESHOLD, String.valueOf(threshold));
			scope.put(PreferenceConstants.ORGIMPORTS_STATIC_ONDEMANDTHRESHOLD, String.valueOf(staticThreshold));
		}
	}

}
