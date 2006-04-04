/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.refactoring.nls.search.CompilationUnitEntry;
import org.eclipse.jdt.internal.ui.refactoring.nls.search.FileEntry;
import org.eclipse.jdt.internal.ui.refactoring.nls.search.NLSSearchQuery;
import org.eclipse.jdt.internal.ui.refactoring.nls.search.NLSSearchResult;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public class NLSSearchTest extends TestCase {

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public NLSSearchTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new ProjectTestSetup(new TestSuite((NLSSearchTest.class)));
	}

	public void setUp() throws CoreException {
		fJProject1= ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack= fSourceFolder.createPackageFragment("org.eclipse.osgi.util", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package org.eclipse.osgi.util;\n");
		buf.append("public class NLS {\n");
		buf.append("public static void initializeMessages(String s, Class c) {}\n}\n");
		pack.createCompilationUnit("NLS.java", buf.toString(), false, null);
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	private IFile write(IFolder folder, final String content, final String fileName) throws IOException, CoreException {
		InputStream stream= new InputStream() {
			private Reader fReader= new StringReader(content);
			public int read() throws IOException {
				return fReader.read();
			}
		};
		IFile file= fJProject1.getProject().getFile(folder.getProjectRelativePath().append(fileName));
		file.create(stream, true, null);
		return file;
	}

	private NLSSearchResult searchProblems(ICompilationUnit accessor, IFile propertiesFile) {
		NLSSearchQuery query= new NLSSearchQuery((new IType[] {accessor.getType("Accessor")}), (new IFile[] {propertiesFile}), SearchEngine.createWorkspaceScope(), "");
		NewSearchUI.runQueryInForeground(new BusyIndicatorRunnableContext(), query);
		NLSSearchResult result= (NLSSearchResult)query.getSearchResult();
		return result;
	}

	private static void assertNumberResults(NLSSearchResult result, int expected) {
		int is= result.getElements().length;
		assertTrue("Expected number of results is " + expected + " but was " + is, is == expected);
	}

	private void assertResultHasUndefinedKey(String key, IFile file, NLSSearchResult result) throws IOException, CoreException {
		Match[] matches= result.getFileMatchAdapter().computeContainedMatches(result, file);

		for (int i= 0; i < matches.length; i++) {
			Match match= matches[i];
			if (match.getElement() instanceof ICompilationUnit) {
				ICompilationUnit unit= (ICompilationUnit)match.getElement();
				String field= unit.getSource().substring(match.getOffset(), match.getOffset() + match.getLength());
				if (field.indexOf(key) != -1)
					return;
			}
		}

		assertTrue("No undefined key problem found for " + key + " in " + file.getName(), false);		
	}

	private void assertResultHasUnusedKey(String key, IFile file, NLSSearchResult result) throws IOException, CoreException {
		Match[] matches= result.getFileMatchAdapter().computeContainedMatches(result, file);

		for (int i= 0; i < matches.length; i++) {
			Match match= matches[i];
			if (match.getElement() instanceof CompilationUnitEntry) {
				ICompilationUnit unit= ((CompilationUnitEntry)match.getElement()).getCompilationUnit();
				String field= unit.getSource().substring(match.getOffset(), match.getOffset() + match.getLength());
				if (field.indexOf(key) != -1)
					return;
			} else if (match.getElement() instanceof FileEntry) {
				FileEntry entry= (FileEntry)match.getElement();
				String content= getContent(entry.getPropertiesFile());
				String propkey= content.substring(match.getOffset(), match.getOffset() + match.getLength());
				if (propkey.indexOf(key) != -1)
					return;
			}
		}

		assertTrue("No unused key problem found for " + key + " in " + file.getName(), false);		
	}

	private void assertResultHasDuplicateKey(String key, IFile file, NLSSearchResult result) throws CoreException, IOException {
		Match[] matches= result.getFileMatchAdapter().computeContainedMatches(result, file);

		for (int i= 0; i < matches.length; i++) {
			Match match= matches[i];
			if (match.getElement() instanceof FileEntry) {
				FileEntry entry= (FileEntry)match.getElement();
				String content= getContent(entry.getPropertiesFile());
				int firstIndex= content.indexOf(key);
				if (firstIndex != -1 && content.indexOf(key, firstIndex + 1) != -1)
					return;
			}
		}

		assertTrue("No duplicate key problem found for " + key + " in " + file.getName(), false);
	}

	private static String getContent(IFile entry) throws CoreException, IOException {
		StringBuffer buf= new StringBuffer();
		InputStream contents= entry.getContents();
		try {
			char ch= (char)contents.read();
			int avilable= contents.available();
			while (avilable > 0 && ch != -1) {
				buf.append(ch);
				ch= (char)contents.read();
				avilable--;
			}
			return buf.toString();
		} finally {
			contents.close();
		}
	}

	public void test01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("import org.eclipse.osgi.util.NLS;\n");
		buf.append("public class Accessor extends NLS {\n");
		buf.append("    private Accessor() {}\n");
		buf.append("    private static final String BUNDLE_NAME = \"test.Accessor\"; //$NON-NLS-1$\n");
		buf.append("    static {NLS.initializeMessages(BUNDLE_NAME, Accessor.class);}\n");
		buf.append("}\n");
		ICompilationUnit accessor= pack1.createCompilationUnit("Accessor.java", buf.toString(), false, null);

		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), "", "Accessor.properties");

		NLSSearchResult result= searchProblems(accessor, propertiesFile);
		assertNumberResults(result, 0);
	}

	public void test02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("    public String s1= Accessor.Client_s1;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("Client_s1=s1\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchResult result= searchProblems(accessor, propertiesFile);
		assertNumberResults(result, 0);
	}

	public void test03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("    public String s1= Accessor.Client_s1;\n");
		buf.append("}\n");
		ICompilationUnit client= pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuffer();
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchResult result= searchProblems(accessor, propertiesFile);
		assertNumberResults(result, 2);

		assertResultHasUndefinedKey("Client_s1", (IFile)accessor.getCorrespondingResource(), result);
		assertResultHasUndefinedKey("Client_s1", (IFile)client.getCorrespondingResource(), result);
	}

	public void test04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("Client_s1=foo\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchResult result= searchProblems(accessor, propertiesFile);
		assertNumberResults(result, 2);

		assertResultHasUnusedKey("Client_s1", (IFile)accessor.getCorrespondingResource(), result);
		assertResultHasUnusedKey("Client_s1", propertiesFile, result);
	}

	public void test05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuffer();
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchResult result= searchProblems(accessor, propertiesFile);
		assertNumberResults(result, 2);

		assertResultHasUnusedKey("Client_s1", (IFile)accessor.getCorrespondingResource(), result);
		assertResultHasUndefinedKey("Client_s1", (IFile)accessor.getCorrespondingResource(), result);
	}

	public void test06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuffer buf= new StringBuffer();
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

		buf= new StringBuffer();
		buf.append("package test;\n");
		buf.append("public class Client {\n");
		buf.append("    public String s1= Accessor.Client_s1;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Client.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("Client_s1=s1\n");
		buf.append("Client_s1=s1\n");
		IFile propertiesFile= write((IFolder)pack1.getCorrespondingResource(), buf.toString(), "Accessor.properties");

		NLSSearchResult result= searchProblems(accessor, propertiesFile);
		assertNumberResults(result, 1);

		assertResultHasDuplicateKey("Client_s1", propertiesFile, result);
	}
}