/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.core;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.ZipFile;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;
import org.eclipse.jdt.testplugin.TestPluginLauncher;

import org.eclipse.jdt.internal.ui.codemanipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.internal.corext.codegeneration.OrganizeImportsOperation;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.util.TypeInfo;

public class ImportOrganizeTest extends TestCase {
	
	private static final Class THIS= ImportOrganizeTest.class;
	
	private IJavaProject fJProject1;

	private static final IPath SOURCES= new Path("testresources/junit32-noUI.zip");

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

		IPackageFragmentRoot jdk= JavaProjectHelper.addRTJar(fJProject1);
		assertTrue("jdk not found", jdk != null);

		File junitSrcArchive= JavaTestPlugin.getDefault().getFileInPlugin(SOURCES);
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
		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/tests/TestTest.java"));
		assertNotNull("TestTest.java", cu);
		
		String[] order= new String[0];
		IChooseImportQuery query= createQuery("TestTest", new String[] { "junit.tests.TestTest.TornDown" }, new int[] { 2 });
		
		OrganizeImportsOperation op= new OrganizeImportsOperation(cu, order, 99, true, query);
		op.run(null);
		
		assertImports(cu, new String[] {
			"junit.framework.AssertionFailedError",
			"junit.framework.TestCase",
			"junit.framework.TestResult",
			"junit.util.StringUtil"
		});
	}
		
	public void test2() throws Exception {
		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/util/TestCaseClassLoader.java"));
		assertNotNull("TestCaseClassLoader.java", cu);
		
		String[] order= new String[0];
		IChooseImportQuery query= createQuery("TestCaseClassLoader", new String[] { }, new int[] { });
		
		OrganizeImportsOperation op= new OrganizeImportsOperation(cu, order, 99, true, query);
		op.run(null);
		
		assertImports(cu, new String[] {
			"java.io.File",
			"java.io.FileInputStream",
			"java.io.FileNotFoundException",
			"java.io.IOException",
			"java.io.InputStream",
			"java.util.Enumeration",
			"java.util.Properties",
			"java.util.StringTokenizer",
			"java.util.Vector"		
		});	
	}
	
	
	public void test3() throws Exception {
		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/util/TestCaseClassLoader.java"));
		assertNotNull("TestCaseClassLoader.java", cu);
		
		String[] order= new String[0];
		IChooseImportQuery query= createQuery("TestCaseClassLoader", new String[] { }, new int[] { });
		
		OrganizeImportsOperation op= new OrganizeImportsOperation(cu, order, 3, true, query);
		op.run(null);
		
		assertImports(cu, new String[] {
			"java.io.*",
			"java.util.*",	
		});	
	}	
	
	public void test4() throws Exception {
		ICompilationUnit cu= (ICompilationUnit) fJProject1.findElement(new Path("junit/textui/TestRunner.java"));
		assertNotNull("TestRunner.java", cu);
		
		String[] order= new String[0];
		IChooseImportQuery query= createQuery("TestRunner", new String[] {}, new int[] {});
		
		OrganizeImportsOperation op= new OrganizeImportsOperation(cu, order, 99, true, query);
		op.run(null);
		
		assertImports(cu, new String[] {
			"java.io.PrintStream",
			"java.lang.reflect.Method", 
			"java.util.Enumeration",
			"junit.framework.Test",
			"junit.framework.TestFailure",
			"junit.framework.TestListener",
			"junit.framework.TestResult",
			"junit.framework.TestSuite",
			"junit.util.StringUtil",
			"junit.util.Version"
		});		
	}	
	
	
}
