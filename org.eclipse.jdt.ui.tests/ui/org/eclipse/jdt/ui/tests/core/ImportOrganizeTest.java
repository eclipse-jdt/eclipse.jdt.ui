/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.core;

import java.io.File;
import java.util.zip.ZipFile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.internal.corext.util.TypeInfo;

public class ImportOrganizeTest extends TestCase {
	
	private static final Class THIS= ImportOrganizeTest.class;
	
	private IJavaProject fJProject1;

	public ImportOrganizeTest(String name) {
		super(name);
	}

	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), THIS, args);
	}


	public static Test suite() {
		return new TestSuite(THIS);
	}


	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");

		assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);

		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.JUNIT_SRC);
		assertTrue("junit src not found", junitSrcArchive != null && junitSrcArchive.exists());
		ZipFile zipfile= new ZipFile(junitSrcArchive);
		JavaProjectHelper.addSourceContainerWithImport(fJProject1, "src", zipfile);
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}
	
	private IChooseImportQuery createQuery(final String name, final String[] choices, final int[] nEntries) {
		return new IChooseImportQuery() {
			public TypeInfo[] chooseImports(TypeInfo[][] openChoices, ISourceRange[] ranges) {
				assertTrue(name + "-query-nchoices1", choices.length == openChoices.length);
				assertTrue(name + "-query-nchoices2", nEntries.length == openChoices.length);
				if (nEntries != null) {
					for (int i= 0; i < nEntries.length; i++) {
						assertTrue(name + "-query-cnt" + i, openChoices[i].length == nEntries[i]);
					}
				}
				TypeInfo[] res= new TypeInfo[openChoices.length];
				for (int i= 0; i < openChoices.length; i++) {
					TypeInfo[] selection= openChoices[i];
					assertNotNull(name + "-query-setset" + i, selection);
					assertTrue(name + "-query-setlen" + i, selection.length > 0);
					TypeInfo found= null;
					for (int k= 0; k < selection.length; k++) {
						if (selection[k].getFullyQualifiedName().equals(choices[i])) {
							found= selection[k];
						}
					}
					assertNotNull(name + "-query-notfound" + i, found);
					res[i]= found;
				}
				return res;
			}
		};
	}
	
	private void assertImports(ICompilationUnit cu, String[] imports) throws Exception {
		IImportDeclaration[] desc= cu.getImports();
		assertTrue(cu.getElementName() + "-count", desc.length == imports.length);
		for (int i= 0; i < imports.length; i++) {
			assertEquals(cu.getElementName() + "-cmpentries" + i, desc[i].getElementName(), imports[i]);
		}
	}
	
	public void test1() throws Exception {
		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/runner/BaseTestRunner.java"));
		assertNotNull("BaseTestRunner.java", cu);
		
		IPackageFragmentRoot root= (IPackageFragmentRoot)cu.getParent().getParent();
		IPackageFragment pack= root.createPackageFragment("mytest", true, null);
		
		ICompilationUnit colidingCU= pack.getCompilationUnit("TestListener.java");
		colidingCU.createType("public abstract class TestListener {\n}\n", null, true, null);
		
		
		String[] order= new String[0];
		IChooseImportQuery query= createQuery("BaseTestRunner", new String[] { "junit.framework.TestListener" }, new int[] { 2 });
		
		OrganizeImportsOperation op= new OrganizeImportsOperation(cu, order, 99, false, true, query);
		op.run(null);
		
		assertImports(cu, new String[] {
			"java.io.BufferedReader",
			"java.io.File",
			"java.io.FileInputStream",
			"java.io.IOException",
			"java.io.InputStream",
			"java.io.PrintWriter",
			"java.io.StringReader",
			"java.io.StringWriter",
			"java.lang.reflect.InvocationTargetException",
			"java.lang.reflect.Method",
			"java.text.NumberFormat",
			"java.util.Properties",
			"junit.framework.Test",
			"junit.framework.TestListener",
			"junit.framework.TestSuite"
		});
	}
		
	public void test2() throws Exception {
		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/runner/LoadingTestCollector.java"));
		assertNotNull("LoadingTestCollector.java", cu);
		
		String[] order= new String[0];
		IChooseImportQuery query= createQuery("LoadingTestCollector", new String[] { }, new int[] { });
		
		OrganizeImportsOperation op= new OrganizeImportsOperation(cu, order, 99, false, true, query);
		op.run(null);
		
		assertImports(cu, new String[] {
			"java.lang.reflect.Constructor",
			"java.lang.reflect.Method",
			"java.lang.reflect.Modifier",
			"junit.framework.Test"	
		});	
	}
	
	
	public void test3() throws Exception {
		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/runner/TestCaseClassLoader.java"));
		assertNotNull("TestCaseClassLoader.java", cu);
		
		String[] order= new String[0];
		IChooseImportQuery query= createQuery("TestCaseClassLoader", new String[] { }, new int[] { });
		
		OrganizeImportsOperation op= new OrganizeImportsOperation(cu, order, 3, false, true, query);
		op.run(null);
		
		assertImports(cu, new String[] {
			"java.io.*",
			"java.net.URL",
			"java.util.*",
			"java.util.zip.ZipEntry",
			"java.util.zip.ZipFile",
		});	
	}
		
	public void test4() throws Exception {
		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/textui/TestRunner.java"));
		assertNotNull("TestRunner.java", cu);
		
		String[] order= new String[0];
		IChooseImportQuery query= createQuery("TestRunner", new String[] {}, new int[] {});
		
		OrganizeImportsOperation op= new OrganizeImportsOperation(cu, order, 99, false, true, query);
		op.run(null);
		
		assertImports(cu, new String[] {
			"java.io.PrintStream",
			"java.util.Enumeration",
			"junit.framework.AssertionFailedError",
			"junit.framework.Test",
			"junit.framework.TestFailure",
			"junit.framework.TestResult",
			"junit.framework.TestSuite",
			"junit.runner.BaseTestRunner",
			"junit.runner.StandardTestSuiteLoader",
			"junit.runner.TestSuiteLoader",
			"junit.runner.Version"
		});		
	}

}
