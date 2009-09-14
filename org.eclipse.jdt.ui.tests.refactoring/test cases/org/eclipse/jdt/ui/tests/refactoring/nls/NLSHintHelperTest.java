/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.refactoring.nls;

import java.io.File;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.refactoring.nls.AccessorClassReference;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHintHelper;

import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;


/**
 * Tests the NLSHintHelper.
 *
 * @since 3.1
 */
public class NLSHintHelperTest extends TestCase {

	private IJavaProject fJProject;
	private IPackageFragmentRoot fLibrary;


	public static Test suite() {
		return new ProjectTestSetup(new TestSuite(NLSHintHelperTest.class));
	}


	protected void setUp() throws Exception {
		fJProject= ProjectTestSetup.getProject();
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.NLS_LIB);
		assertTrue("lib does not exist",  lib != null && lib.exists());
		fLibrary= JavaProjectHelper.addLibrary(fJProject, Path.fromOSString(lib.getPath())); // add library to project
	}

	public void testFindInJAR() {
		try {
			assertNotNull(NLSHintHelper.getResourceBundle(fLibrary, "pkg", "Messages.properties"));
		} catch (JavaModelException e) {
			fail();
		}
		IPackageFragment pkg= fLibrary.getPackageFragment("pkg");
		IClassFile classFile= pkg.getClassFile("Client.class");
		IRegion region= new Region(648, 4);
		CompilationUnit ast= SharedASTProvider.getAST(classFile, SharedASTProvider.WAIT_YES, null);
		AccessorClassReference accessor= NLSHintHelper.getAccessorClassReference(ast, region);
		assertNotNull(accessor);
		Properties properties= NLSHintHelper.getProperties(fJProject, accessor);
		assertNotNull(properties);
		assertEquals("Hello World", properties.get("test"));
		try {
			assertNotNull(NLSHintHelper.getResourceBundle(fJProject, accessor));
		} catch (JavaModelException e1) {
			fail();
		}

	}

	public void testDoNotFindInJAR() {
		try {
			assertNull(NLSHintHelper.getResourceBundle(fJProject, "pkg", "Messages.properties"));
		} catch (JavaModelException e) {
			fail();
		}
	}

	public void testFindInDirtyBuffer() {
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		assertNotNull(manager);

		IPath nonExistentPath= fJProject.getProject().getFullPath().append("" + System.currentTimeMillis());
		try {
			manager.connect(nonExistentPath, LocationKind.NORMALIZE, null);
		} catch (CoreException e) {
			fail();
		}
		try {
			ITextFileBuffer buffer= manager.getTextFileBuffer(nonExistentPath, LocationKind.NORMALIZE);
			buffer.getDocument().set("newKey= newValue");

			IFile nonExistentFile= ResourcesPlugin.getWorkspace().getRoot().getFile(nonExistentPath);

			Properties properties= NLSHintHelper.getProperties(nonExistentFile);
			String newValue= properties.getProperty("newKey");
			assertEquals("newValue", newValue);
		} finally {
			try {
				manager.disconnect(nonExistentPath, LocationKind.NORMALIZE, null);
			} catch (CoreException e1) {
				// ignore: test itself was already successful
			}
		}
	}

	public void testDoNotFindDirtyBuffer() {
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		assertNotNull(manager);

		IPath nonExistentPath= fJProject.getProject().getFullPath().append("" + System.currentTimeMillis());
		try {
			manager.connect(nonExistentPath, LocationKind.NORMALIZE, null);
		} catch (CoreException e) {
			fail();
		}
		try {
			ITextFileBuffer buffer= manager.getTextFileBuffer(nonExistentPath, LocationKind.NORMALIZE);
			buffer.getDocument().set("newKey= newValue");

			IFile nonExistentFile= ResourcesPlugin.getWorkspace().getRoot().getFile(nonExistentPath);

			Properties properties= NLSHintHelper.getProperties(nonExistentFile);
			String newValue= properties.getProperty("nonExistingValue");
			assertEquals(newValue, null);
		} finally {
			try {
				manager.disconnect(nonExistentPath, LocationKind.NORMALIZE, null);
			} catch (CoreException e1) {
				// ignore: test itself was already successful
			}
		}
	}

	public void testFindInFile() {
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		assertNotNull(manager);

		String fileName= "" + System.currentTimeMillis();
		IPath nonExistentPath= fJProject.getProject().getFullPath().append(fileName);
		IPath nonExistentLocation= fJProject.getProject().getLocation().append(fileName);

		try {
			manager.connect(nonExistentLocation, LocationKind.NORMALIZE, null);
		} catch (CoreException e) {
			fail();
		}
		try {
			ITextFileBuffer buffer= manager.getTextFileBuffer(nonExistentLocation, LocationKind.NORMALIZE);
			buffer.getDocument().set("newKey= newValue");
			buffer.commit(null, false);

			fJProject.getProject().refreshLocal(IResource.DEPTH_ONE, null);

			IFile existentFile= ResourcesPlugin.getWorkspace().getRoot().getFile(nonExistentPath);
			assertEquals(true, existentFile.exists());

			Properties properties= NLSHintHelper.getProperties(existentFile);
			String newValue= properties.getProperty("newKey");
			assertEquals("newValue", newValue);
		} catch (CoreException ex) {
			fail();
		} finally {
			try {
				manager.disconnect(nonExistentPath, LocationKind.NORMALIZE, null);
			} catch (CoreException e1) {
				// ignore: test itself was already successful
			}
		}
	}

	public void testDoNotFindInFile() {
		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		assertNotNull(manager);

		String fileName= "" + System.currentTimeMillis();
		IPath nonExistentPath= fJProject.getProject().getFullPath().append(fileName);
		IPath nonExistentLocation= fJProject.getProject().getLocation().append(fileName);
		try {
			manager.connect(nonExistentLocation, LocationKind.NORMALIZE, null);
		} catch (CoreException e) {
			fail();
		}
		try {
			ITextFileBuffer buffer= manager.getTextFileBuffer(nonExistentLocation, LocationKind.NORMALIZE);
			buffer.getDocument().set("newKey= newValue");
			buffer.commit(null, false);

			fJProject.getProject().refreshLocal(IResource.DEPTH_ONE, null);

			IFile existentFile= ResourcesPlugin.getWorkspace().getRoot().getFile(nonExistentPath);
			assertEquals(true, existentFile.exists());

			Properties properties= NLSHintHelper.getProperties(existentFile);
			String newValue= properties.getProperty("nonExistingValue");
			assertEquals(newValue, null);
		} catch (CoreException ex) {
			fail();
		} finally {
			try {
				manager.disconnect(nonExistentPath, LocationKind.NORMALIZE, null);
			} catch (CoreException e1) {
				// ignore: test itself was already successful
			}
		}
	}

	public void testFindResourceBundleName1f() throws Exception {
	    String source=
			"package test;\n" +
			"public class TestMessages {\n" +
			"	private static final String BUNDLE_NAME = \"test.test\";\n" +
			"	public static String getString(String s) {" +
			"		return \"\";\n" +
			"	}\n" +
			"}\n";


	    assertEquals("test.test", getResourceBundleName(source, "TestMessages", "test"));
	}

	public void testFindResourceBundleName1s() throws Exception {
	    String source=
			"package test;\n" +
			"public class TestMessages {\n" +
			"	private static String BUNDLE_NAME;\n" +
			"   static {\n" +
			"		BUNDLE_NAME= \"test.test\";\n" +
			"   }\n" +
			"	public static String getString(String s) {" +
			"		return \"\";\n" +
			"	}\n" +
			"}\n";


	    assertEquals("test.test", getResourceBundleName(source, "TestMessages", "test"));
	}

	public void testFindResourceBundleName2f() throws Exception {
	    String source=
			"package test;\n" +
			"public class TestMessages {\n" +
			"	private static final String BUNDLE_NAME = TestMessages.class.getName();\n" +
			"	public static String getString(String s) {\n" +
			"		return \"\";\n" +
			"	}\n" +
			"}\n";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	public void testFindResourceBundleName2s() throws Exception {
	    String source=
			"package test;\n" +
			"public class TestMessages {\n" +
			"	private static String BUNDLE_NAME;\n" +
			"   static {\n" +
			"		BUNDLE_NAME = TestMessages.class.getName();\n" +
			"   }\n" +
			"	public static String getString(String s) {\n" +
			"		return \"\";\n" +
			"	}\n" +
			"}\n";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	public void testFindResourceBundleName3f() throws Exception {
	    String source=
			"package test;\n" +
			"import java.util.ResourceBundle;\n" +
			"public class TestMessages {\n" +
			"	private static final ResourceBundle b= ResourceBundle.getBundle(TestMessages.class.getName());\n" +
			"	public static String getString(String s) {\n" +
			"		return \"\";\n" +
			"	}\n" +
			"}\n";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	public void testFindResourceBundleName3s() throws Exception {
	    String source=
			"package test;\n" +
			"import java.util.ResourceBundle;\n" +
			"public class TestMessages {\n" +
			"	private static ResourceBundle b;\n" +
			"   static {\n" +
			"		b= ResourceBundle.getBundle(TestMessages.class.getName());\n" +
			"   }\n" +
			"	public static String getString(String s) {\n" +
			"		return \"\";\n" +
			"	}\n" +
			"}\n";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	public void testFindResourceBundleName4f() throws Exception {
	    String source=
			"package test;\n" +
			"import java.util.ResourceBundle;\n" +
			"public class TestMessages {\n" +
			"	private static final ResourceBundle b= ResourceBundle.getBundle(\"test.test\");\n" +
			"	public static String getString(String s) {\n" +
			"		return \"\";\n" +
			"	}\n" +
			"}\n";

	    assertEquals("test.test", getResourceBundleName(source, "TestMessages", "test"));
	}

	public void testFindResourceBundleName4s() throws Exception {
	    String source=
			"package test;\n" +
			"import java.util.ResourceBundle;\n" +
			"public class TestMessages {\n" +
			"	private static ResourceBundle b;\n" +
			"   static {\n" +
			"		b= ResourceBundle.getBundle(\"test.test\");\n" +
			"   }\n" +
			"	public static String getString(String s) {\n" +
			"		return \"\";\n" +
			"	}\n" +
			"}\n";

	    assertEquals("test.test", getResourceBundleName(source, "TestMessages", "test"));
	}

	public void testFindResourceBundleName5f() throws Exception {
	    String source=
			"package test;\n" +
			"import java.util.ResourceBundle;\n" +
			"public class TestMessages {\n" +
			"	private static final String RESOURCE_BUNDLE= TestMessages.class.getName();\n" +
			"	private static final ResourceBundle b= ResourceBundle.getBundle(RESOURCE_BUNDLE);\n" +
			"	public static String getString(String s) {\n" +
			"		return \"\";\n" +
			"	}\n" +
			"}\n";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	public void testFindResourceBundleName5s() throws Exception {
	    String source=
			"package test;\n" +
			"import java.util.ResourceBundle;\n" +
			"public class TestMessages {\n" +
			"	private static final String RESOURCE_BUNDLE= TestMessages.class.getName();\n" +
			"	private static ResourceBundle b;\n" +
			"   static {\n" +
			"		b= ResourceBundle.getBundle(RESOURCE_BUNDLE);\n" +
			"   }\n" +
			"	public static String getString(String s) {\n" +
			"		return \"\";\n" +
			"	}\n" +
			"}\n";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	public void testFindResourceBundleName6() throws Exception {
	    String source=
			"package test;\n" +
			"import java.util.ResourceBundle;\n" +
			"public class TestMessages {\n" +
			"	private static final String RESOURCE_BUNDLE= TestMessages.class.getName();\n" +
			"	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);\n" +
			"	public static String getString(String s) {\n" +
			"		return \"\";\n" +
			"	}\n" +
			"}\n";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	private String getResourceBundleName(String source, String className, String packageName) throws Exception {
		// Create CU
	    IPackageFragmentRoot sourceFolder = JavaProjectHelper.addSourceContainer(fJProject, "src");
        IPackageFragment pack = sourceFolder.createPackageFragment(packageName, false, null);
        ICompilationUnit  cu= pack.createCompilationUnit(className + ".java", source, false, null);

        // Get type binding
        CompilationUnit ast= SharedASTProvider.getAST(cu, SharedASTProvider.WAIT_YES, null);
        ASTNode node= NodeFinder.perform(ast, cu.getType(className).getSourceRange());
        ITypeBinding typeBinding= ((TypeDeclaration)node).resolveBinding();

        return  NLSHintHelper.getResourceBundleName(typeBinding);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject, ProjectTestSetup.getDefaultClasspath());
	}
}
