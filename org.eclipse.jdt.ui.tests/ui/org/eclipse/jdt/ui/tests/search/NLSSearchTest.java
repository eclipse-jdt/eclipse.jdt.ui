/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.search;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.NewSearchUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.refactoring.nls.search.NLSSearchQuery;

public class NLSSearchTest {

	@Rule
	public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws CoreException {
		fJProject1= projectSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack= fSourceFolder.createPackageFragment("org.eclipse.osgi.util", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package org.eclipse.osgi.util;\n");
		buf.append("public class NLS {\n");
		buf.append("public static void initializeMessages(String s, Class c) {}\n}\n");
		pack.createCompilationUnit("NLS.java", buf.toString(), false, null);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
		ISearchResultViewPart searchResultView= NewSearchUI.getSearchResultView();
		if (searchResultView != null) {
			searchResultView.getSite().getPage().hideView(searchResultView);
		}
	}

	private IFile write(IFolder folder, final String content, final String fileName) throws CoreException {
		ByteArrayInputStream stream= new ByteArrayInputStream(content.getBytes(Charset.defaultCharset()));
		IFile file= fJProject1.getProject().getFile(folder.getProjectRelativePath().append(fileName));
		file.create(stream, true, null);
		return file;
	}

	@Test
	public void test01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import org.eclipse.osgi.util.NLS;\n");
		buf.append("public class Accessor extends NLS {\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    static {NLS.initializeMessages(BUNDLE_NAME, Accessor.class);}\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), "", "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 0);
	}

	@Test
	public void test02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import org.eclipse.osgi.util.NLS;\n");
		buf.append("public class Accessor extends NLS {\n");
		buf.append("\n");
		buf.append("    public static String Client_s1;\n");
		buf.append("\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    static {NLS.initializeMessages(BUNDLE_NAME, Accessor.class);}\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("    public String s1= Accessor.Client_s1;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("Client_s1=s1\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 0);
	}

	@Test
	public void test03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import org.eclipse.osgi.util.NLS;\n");
		buf.append("public class Accessor extends NLS {\n");
		buf.append("\n");
		buf.append("    public static String Client_s1;\n");
		buf.append("\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    static {NLS.initializeMessages(BUNDLE_NAME, Accessor.class);}\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("    public String s1= Accessor.Client_s1;\n");
		buf.append("}\n");
		ICompilationUnit client= pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 2);

		NLSSearchTestHelper.assertHasUndefinedKey(accessor, propertiesFile, "Client_s1", (IFile)accessor.getCorrespondingResource(), true);
		NLSSearchTestHelper.assertHasUndefinedKey(accessor, propertiesFile, "Client_s1", (IFile)client.getCorrespondingResource(), false);
	}

	@Test
	public void test04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import org.eclipse.osgi.util.NLS;\n");
		buf.append("public class Accessor extends NLS {\n");
		buf.append("\n");
		buf.append("    public static String Client_s1;\n");
		buf.append("\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    static {NLS.initializeMessages(BUNDLE_NAME, Accessor.class);}\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("Client_s1=foo\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 2);

		NLSSearchTestHelper.assertHasUnusedKey(accessor, propertiesFile, "Client_s1", (IFile)accessor.getCorrespondingResource(), true);
		NLSSearchTestHelper.assertHasUnusedKey(accessor, propertiesFile, "Client_s1", propertiesFile, false);
	}

	@Test
	public void test05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import org.eclipse.osgi.util.NLS;\n");
		buf.append("public class Accessor extends NLS {\n");
		buf.append("\n");
		buf.append("    public static String Client_s1;\n");
		buf.append("\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    static {NLS.initializeMessages(BUNDLE_NAME, Accessor.class);}\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 2);

		NLSSearchTestHelper.assertHasUnusedKey(accessor, propertiesFile, "Client_s1", (IFile)accessor.getCorrespondingResource(), true);
		NLSSearchTestHelper.assertHasUndefinedKey(accessor, propertiesFile, "Client_s1", (IFile)accessor.getCorrespondingResource(), true);
	}

	@Test
	public void test06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import org.eclipse.osgi.util.NLS;\n");
		buf.append("public class Accessor extends NLS {\n");
		buf.append("\n");
		buf.append("    public static String Client_s1;\n");
		buf.append("\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    static {NLS.initializeMessages(BUNDLE_NAME, Accessor.class);}\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("    public String s1= Accessor.Client_s1;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("Client_s1=s1\n");
		buf.append("Client_s1=s1\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 1);

		NLSSearchTestHelper.assertHasDuplicateKey(accessor, propertiesFile, "Client_s1", propertiesFile);
	}

	@Test
	public void test07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import org.eclipse.osgi.util.NLS;\n");
		buf.append("public class Accessor extends NLS {\n");
		buf.append("\n");
		buf.append("    public static String Client_s1;\n");
		buf.append("    public static String Client_s2;\n");
		buf.append("\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    static {NLS.initializeMessages(BUNDLE_NAME, Accessor.class);}\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("Client_s1=foo\n");
		buf.append("Client_s2=foo2\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		// mark Client_s1 as used so it won't be flagged
		buf= new StringBuilder();
		buf.append("Client_s1=foo\n");
		write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor" + NLSSearchQuery.NLS_USED_PROPERTIES_EXT);

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 2);

		NLSSearchTestHelper.assertHasUnusedKey(accessor, propertiesFile, "Client_s2", (IFile)accessor.getCorrespondingResource(), true);
		NLSSearchTestHelper.assertHasUnusedKey(accessor, propertiesFile, "Client_s2", propertiesFile, false);
	}

	@Test
	public void testBug152604() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import org.eclipse.osgi.util.NLS;\n");
		buf.append("public class Accessor extends NLS {\n");
		buf.append("\n");
		buf.append("    public static String Client_s1;\n");
		buf.append("\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    static {NLS.initializeMessages(BUNDLE_NAME, Accessor.class);}\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("    public int length= Accessor.Client_s1.length();\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("Client_s1=s1\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 0);
	}

	@Test
	public void testBug133810() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import org.eclipse.osgi.util.NLS;\n");
		buf.append("public class Accessor extends NLS {\n");
		buf.append("\n");
		buf.append("    public static String Client_s1;\n");
		buf.append("\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    static {NLS.initializeMessages(BUNDLE_NAME, Accessor.class);}\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("    public String s1= Accessor.Client_s1;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("Client_s2=s1\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(propertiesFile.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
			ITextFileBuffer buffer= manager.getTextFileBuffer(propertiesFile.getFullPath(), LocationKind.IFILE);
			buffer.getDocument().replace(8, 1, "1");

			NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 0);
		} finally {
			manager.disconnect(propertiesFile.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
		}
	}

	@Test
	public void testBug185178() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.MissingResourceException;\n");
		buf.append("import java.util.ResourceBundle;\n");
		buf.append("public class Accessor {\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    public static String getString(String key) {\n");
		buf.append("        try {\n");
		buf.append("            return RESOURCE_BUNDLE.getString(key);\n");
		buf.append("        } catch (MissingResourceException e) {\n");
		buf.append("            return '!' + key + '!';\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("    public String s1= Accessor\n");
		buf.append("                            .getString(\"Client.0\"); //$NON-NLS-1$\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("Client.0=s1\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 0);
	}

	@Test
	public void testBug247012_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.MissingResourceException;\n");
		buf.append("import java.util.ResourceBundle;\n");
		buf.append("public class Accessor {\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    public static String getString(String key) {\n");
		buf.append("        try {\n");
		buf.append("            return RESOURCE_BUNDLE.getString(key);\n");
		buf.append("        } catch (MissingResourceException e) {\n");
		buf.append("            return '!' + key + '!';\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("	private static final String X =\"Main.indirect\";//$NON-NLS-1$ \n");
		buf.append("	public static void main(String[] args) { \n");
		buf.append("		System.out.println(Accessor.getString(\"Main.direct\")); //$NON-NLS-1$ \n");
		buf.append("		System.out.println(Accessor.getString(X)); \n");
		buf.append("	}\n");
		buf.append("}\n");

		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("Main.direct=Main.direct\n");
		buf.append("Main.indirect=Main.indirect\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 0);
	}

	@Test
	public void testBug247012_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.MissingResourceException;\n");
		buf.append("import java.util.ResourceBundle;\n");
		buf.append("public class Accessor {\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    public static String getString(String key) {\n");
		buf.append("        try {\n");
		buf.append("            return RESOURCE_BUNDLE.getString(key);\n");
		buf.append("        } catch (MissingResourceException e) {\n");
		buf.append("            return '!' + key + '!';\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("	private static final String X = Accessor.getString(\"Main.indirect\");//$NON-NLS-1$ \n");
		buf.append("	public static void main(String[] args) { \n");
		buf.append("		System.out.println(Accessor.getString(\"Main.direct\")); //$NON-NLS-1$ \n");
		buf.append("		System.out.println(Accessor.getString(X)); \n");
		buf.append("	}\n");
		buf.append("}\n");

		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("Main.direct=Main.direct\n");
		buf.append("Main.indirect=Main.indirect\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 1);
	}

	@Test
	public void testBug247012_3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.MissingResourceException;\n");
		buf.append("import java.util.ResourceBundle;\n");
		buf.append("public class Accessor {\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    public static String getString(int i) {\n");
		buf.append("            return \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("	private static final int i = 10; \n");
		buf.append("	public static void main(String[] args) { \n");
		buf.append("		System.out.println(Accessor.getString(i)); \n");
		buf.append("	}\n");
		buf.append("}\n");

		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 0);
	}

	@Test
	public void testBug247012_4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.MissingResourceException;\n");
		buf.append("import java.util.ResourceBundle;\n");
		buf.append("public class Accessor {\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    public static String getString(String key) {\n");
		buf.append("        try {\n");
		buf.append("            return RESOURCE_BUNDLE.getString(key);\n");
		buf.append("        } catch (MissingResourceException e) {\n");
		buf.append("            return '!' + key + '!';\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("	public static void main(String[] args) { \n");
		buf.append("		System.out.println(Accessor.getString(\"Main.undefined\")); //$NON-NLS-1$ \n");
		buf.append("	}\n");
		buf.append("}\n");

		ICompilationUnit client= pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 1);
		NLSSearchTestHelper.assertHasUndefinedKey(accessor, propertiesFile, "Main.undefined", (IFile)client.getCorrespondingResource(), false);
	}

	@Test
	public void testBug295040() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.MissingResourceException;\n");
		buf.append("import java.util.ResourceBundle;\n");
		buf.append("public class Accessor {\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    public static String getString(String key) {\n");
		buf.append("        try {\n");
		buf.append("            return RESOURCE_BUNDLE.getString(key);\n");
		buf.append("        } catch (MissingResourceException e) {\n");
		buf.append("            return '!' + key + '!';\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public static ResourceBundle getResourceBundle() {\n");
		buf.append("        return RESOURCE_BUNDLE;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("	public static void main(String[] args) { \n");
		buf.append("		System.out.println(Accessor.getString(\"Client_s1\")); //$NON-NLS-1$ \n");
		buf.append("		Accessor.getResourceBundle(); \n");
		buf.append("	}\n");
		buf.append("}\n");

		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("Client_s1=s1\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 0);
	}

	@Test
	public void testBug306168_1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.MissingResourceException;\n");
		buf.append("import java.util.ResourceBundle;\n");
		buf.append("public class Accessor {\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    public static String getString(String key) {\n");
		buf.append("        try {\n");
		buf.append("            return RESOURCE_BUNDLE.getString(key);\n");
		buf.append("        } catch (MissingResourceException e) {\n");
		buf.append("            return '!' + key + '!';\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("	private static final String X =\"Main.indirect\";//$NON-NLS-1$ \n");
		buf.append("	public static void main(String[] args) { \n");
		buf.append("		Accessor.getString(test.Client.X); \n");
		buf.append("	}\n");
		buf.append("}\n");

		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("Main.indirect=Main.indirect\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 0);
	}

	@Test
	public void testBug306168_2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("import java.util.MissingResourceException;\n");
		buf.append("import java.util.ResourceBundle;\n");
		buf.append("public class Accessor {\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    public static String getString(String key) {\n");
		buf.append("        try {\n");
		buf.append("            return RESOURCE_BUNDLE.getString(key);\n");
		buf.append("        } catch (MissingResourceException e) {\n");
		buf.append("            return '!' + key + '!';\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("	private static final String X =\"Main.undefined\";//$NON-NLS-1$ \n");
		buf.append("	public static void main(String[] args) { \n");
		buf.append("		Accessor.getString(test.Client.X); \n");
		buf.append("	}\n");
		buf.append("}\n");

		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuilder();
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchTestHelper.assertNumberOfProblems(accessor, propertiesFile, 1);
	}

}
