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

package org.eclipse.jdt.ui.tests.refactoring.nls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.corext.refactoring.nls.AccessorClassReference;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHintHelper;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests the NLSHintHelper.
 *
 * @since 3.1
 */
public class NLSHintHelperTest {
	private IJavaProject fJProject;
	private IPackageFragmentRoot fLibrary;

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	@Before
	public void setUp() throws Exception {
		fJProject= pts.getProject();
		File lib= JavaTestPlugin.getDefault().getFileInPlugin(JavaProjectHelper.NLS_LIB);
		assertNotNull("lib does not exist", lib);
		assertTrue("lib does not exist", lib.exists());
		fLibrary= JavaProjectHelper.addLibrary(fJProject, Path.fromOSString(lib.getPath())); // add library to project
	}

	@Test
	public void findInJAR() {
		try {
			assertNotNull(NLSHintHelper.getResourceBundle(fLibrary, "pkg", "Messages.properties"));
		} catch (JavaModelException e) {
			fail();
		}
		IPackageFragment pkg= fLibrary.getPackageFragment("pkg");
		IClassFile classFile= pkg.getClassFile("Client.class");
		IRegion region= new Region(648, 4);
		CompilationUnit ast= SharedASTProviderCore.getAST(classFile, SharedASTProviderCore.WAIT_YES, null);
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

	@Test
	public void doNotFindInJAR() {
		try {
			assertNull(NLSHintHelper.getResourceBundle(fJProject, "pkg", "Messages.properties"));
		} catch (JavaModelException e) {
			fail();
		}
	}

	@Test
	public void findInDirtyBuffer() {
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

	@Test
	public void doNotFindDirtyBuffer() {
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
			assertNull(newValue);
		} finally {
			try {
				manager.disconnect(nonExistentPath, LocationKind.NORMALIZE, null);
			} catch (CoreException e1) {
				// ignore: test itself was already successful
			}
		}
	}

	@Test
	public void findInFile() {
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
			assertTrue(existentFile.exists());

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

	@Test
	public void doNotFindInFile() {
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
			assertTrue(existentFile.exists());

			Properties properties= NLSHintHelper.getProperties(existentFile);
			String newValue= properties.getProperty("nonExistingValue");
			assertNull(newValue);
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

	@Test
	public void findResourceBundleName1f() throws Exception {
	    String source=
			"""
			package test;
			public class TestMessages {
				private static final String BUNDLE_NAME = "test.test";
				public static String getString(String s) {\
					return "";
				}
			}
			""";


	    assertEquals("test.test", getResourceBundleName(source, "TestMessages", "test"));
	}

	@Test
	public void findResourceBundleName1s() throws Exception {
	    String source=
			"""
			package test;
			public class TestMessages {
				private static String BUNDLE_NAME;
			   static {
					BUNDLE_NAME= "test.test";
			   }
				public static String getString(String s) {\
					return "";
				}
			}
			""";


	    assertEquals("test.test", getResourceBundleName(source, "TestMessages", "test"));
	}

	@Test
	public void findResourceBundleName2f() throws Exception {
	    String source=
			"""
			package test;
			public class TestMessages {
				private static final String BUNDLE_NAME = TestMessages.class.getName();
				public static String getString(String s) {
					return "";
				}
			}
			""";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	@Test
	public void findResourceBundleName2s() throws Exception {
	    String source=
			"""
			package test;
			public class TestMessages {
				private static String BUNDLE_NAME;
			   static {
					BUNDLE_NAME = TestMessages.class.getName();
			   }
				public static String getString(String s) {
					return "";
				}
			}
			""";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	@Test
	public void findResourceBundleName3f() throws Exception {
	    String source=
			"""
			package test;
			import java.util.ResourceBundle;
			public class TestMessages {
				private static final ResourceBundle b= ResourceBundle.getBundle(TestMessages.class.getName());
				public static String getString(String s) {
					return "";
				}
			}
			""";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	@Test
	public void findResourceBundleName3s() throws Exception {
	    String source=
			"""
			package test;
			import java.util.ResourceBundle;
			public class TestMessages {
				private static ResourceBundle b;
			   static {
					b= ResourceBundle.getBundle(TestMessages.class.getName());
			   }
				public static String getString(String s) {
					return "";
				}
			}
			""";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	@Test
	public void findResourceBundleName4f() throws Exception {
	    String source=
			"""
			package test;
			import java.util.ResourceBundle;
			public class TestMessages {
				private static final ResourceBundle b= ResourceBundle.getBundle("test.test");
				public static String getString(String s) {
					return "";
				}
			}
			""";

	    assertEquals("test.test", getResourceBundleName(source, "TestMessages", "test"));
	}

	@Test
	public void findResourceBundleName4s() throws Exception {
	    String source=
			"""
			package test;
			import java.util.ResourceBundle;
			public class TestMessages {
				private static ResourceBundle b;
			   static {
					b= ResourceBundle.getBundle("test.test");
			   }
				public static String getString(String s) {
					return "";
				}
			}
			""";

	    assertEquals("test.test", getResourceBundleName(source, "TestMessages", "test"));
	}

	@Test
	public void findResourceBundleName5f() throws Exception {
	    String source=
			"""
			package test;
			import java.util.ResourceBundle;
			public class TestMessages {
				private static final String RESOURCE_BUNDLE= TestMessages.class.getName();
				private static final ResourceBundle b= ResourceBundle.getBundle(RESOURCE_BUNDLE);
				public static String getString(String s) {
					return "";
				}
			}
			""";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	@Test
	public void findResourceBundleName5s() throws Exception {
	    String source=
			"""
			package test;
			import java.util.ResourceBundle;
			public class TestMessages {
				private static final String RESOURCE_BUNDLE= TestMessages.class.getName();
				private static ResourceBundle b;
			   static {
					b= ResourceBundle.getBundle(RESOURCE_BUNDLE);
			   }
				public static String getString(String s) {
					return "";
				}
			}
			""";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	@Test
	public void findResourceBundleName6() throws Exception {
	    String source=
			"""
			package test;
			import java.util.ResourceBundle;
			public class TestMessages {
				private static final String RESOURCE_BUNDLE= TestMessages.class.getName();
				private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);
				public static String getString(String s) {
					return "";
				}
			}
			""";

	    assertEquals("test.TestMessages", getResourceBundleName(source, "TestMessages", "test"));
	}

	private String getResourceBundleName(String source, String className, String packageName) throws Exception {
		// Create CU
	    IPackageFragmentRoot sourceFolder = JavaProjectHelper.addSourceContainer(fJProject, "src");
        IPackageFragment pack = sourceFolder.createPackageFragment(packageName, false, null);
        ICompilationUnit  cu= pack.createCompilationUnit(className + ".java", source, false, null);

        // Get type binding
        CompilationUnit ast= SharedASTProviderCore.getAST(cu, SharedASTProviderCore.WAIT_YES, null);
        ASTNode node= NodeFinder.perform(ast, cu.getType(className).getSourceRange());
        ITypeBinding typeBinding= ((TypeDeclaration)node).resolveBinding();

        return  NLSHintHelper.getResourceBundleName(typeBinding);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject, pts.getDefaultClasspath());
	}
}
