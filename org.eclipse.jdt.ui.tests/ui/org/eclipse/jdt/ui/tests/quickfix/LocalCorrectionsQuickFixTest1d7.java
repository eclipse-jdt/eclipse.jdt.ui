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

import java.util.ArrayList;
import java.util.Hashtable;

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
import org.eclipse.jdt.ui.tests.core.rules.Java1d7ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Those tests should run on Java Dolphin 1.7 .
 */
public class LocalCorrectionsQuickFixTest1d7 extends QuickFixTest {
	@Rule
	public ProjectTestSetup projectSetup= new Java1d7ProjectTestSetup();

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_UNCHECKED_TYPE_OPERATION, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_MISSING_HASHCODE_METHOD, JavaCore.WARNING);
		options.put(JavaCore.COMPILER_PB_REDUNDANT_TYPE_ARGUMENTS, JavaCore.WARNING);

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.CATCHBLOCK_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "", null);

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testUncaughtExceptionUnionType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			import java.io.FileNotFoundException;
			import java.io.InterruptedIOException;
			import java.text.ParseException;
			
			public class E {
			    void foo(int a) {
			        try {
			            if (a < 10)
			                throw new FileNotFoundException();
			            else if (a < 20)
			                throw new InterruptedIOException();
			            else
			                throw new ParseException("bar", 0);
			        } catch (FileNotFoundException | InterruptedIOException ex) {
			            ex.printStackTrace();
			        }\s
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			
			import java.io.FileNotFoundException;
			import java.io.InterruptedIOException;
			import java.text.ParseException;
			
			public class E {
			    void foo(int a) throws ParseException {
			        try {
			            if (a < 10)
			                throw new FileNotFoundException();
			            else if (a < 20)
			                throw new InterruptedIOException();
			            else
			                throw new ParseException("bar", 0);
			        } catch (FileNotFoundException | InterruptedIOException ex) {
			            ex.printStackTrace();
			        }\s
			    }
			}
			""";

		String expected2= """
			package test1;
			
			import java.io.FileNotFoundException;
			import java.io.InterruptedIOException;
			import java.text.ParseException;
			
			public class E {
			    void foo(int a) {
			        try {
			            if (a < 10)
			                throw new FileNotFoundException();
			            else if (a < 20)
			                throw new InterruptedIOException();
			            else
			                throw new ParseException("bar", 0);
			        } catch (FileNotFoundException | InterruptedIOException ex) {
			            ex.printStackTrace();
			        } catch (ParseException e) {
			        }\s
			    }
			}
			""";

		String expected3= """
			package test1;
			
			import java.io.FileNotFoundException;
			import java.io.InterruptedIOException;
			import java.text.ParseException;
			
			public class E {
			    void foo(int a) {
			        try {
			            if (a < 10)
			                throw new FileNotFoundException();
			            else if (a < 20)
			                throw new InterruptedIOException();
			            else
			                throw new ParseException("bar", 0);
			        } catch (FileNotFoundException | InterruptedIOException | ParseException ex) {
			            ex.printStackTrace();
			        }\s
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testUncaughtSuperException() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.io.FileNotFoundException;
			import java.io.IOException;
			
			public class E {
			    void foo(int a) {
			        try {
			            if (a < 10)
			                throw new FileNotFoundException();
			            else
			                throw new IOException();
			        } catch (FileNotFoundException ex) {
			            ex.printStackTrace();
			        }\s
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", sample, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		sample= """
			package test1;
			
			import java.io.FileNotFoundException;
			import java.io.IOException;
			
			public class E {
			    void foo(int a) throws IOException {
			        try {
			            if (a < 10)
			                throw new FileNotFoundException();
			            else
			                throw new IOException();
			        } catch (FileNotFoundException ex) {
			            ex.printStackTrace();
			        }\s
			    }
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			
			import java.io.FileNotFoundException;
			import java.io.IOException;
			
			public class E {
			    void foo(int a) {
			        try {
			            if (a < 10)
			                throw new FileNotFoundException();
			            else
			                throw new IOException();
			        } catch (FileNotFoundException ex) {
			            ex.printStackTrace();
			        } catch (IOException e) {
			        }\s
			    }
			}
			""";
		String expected2= sample;

		sample= """
			package test1;
			
			import java.io.FileNotFoundException;
			import java.io.IOException;
			
			public class E {
			    void foo(int a) {
			        try {
			            if (a < 10)
			                throw new FileNotFoundException();
			            else
			                throw new IOException();
			        } catch (IOException ex) {
			            ex.printStackTrace();
			        }\s
			    }
			}
			""";
		String expected3= sample;

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testUncaughtSuperExceptionUnionType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			import java.io.FileNotFoundException;
			import java.io.IOException;
			import java.io.InterruptedIOException;
			
			public class E {
			    void foo(int a) {
			        try {
			            if (a < 10)
			                throw new FileNotFoundException();
			            else if (a < 20)
			                throw new InterruptedIOException();
			            else
			                throw new IOException();
			        } catch (FileNotFoundException | InterruptedIOException ex) {
			            ex.printStackTrace();
			        }\s
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", sample, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		sample= """
			package test1;
			
			import java.io.FileNotFoundException;
			import java.io.IOException;
			import java.io.InterruptedIOException;
			
			public class E {
			    void foo(int a) throws IOException {
			        try {
			            if (a < 10)
			                throw new FileNotFoundException();
			            else if (a < 20)
			                throw new InterruptedIOException();
			            else
			                throw new IOException();
			        } catch (FileNotFoundException | InterruptedIOException ex) {
			            ex.printStackTrace();
			        }\s
			    }
			}
			""";
		String expected1= sample;

		sample= """
			package test1;
			
			import java.io.FileNotFoundException;
			import java.io.IOException;
			import java.io.InterruptedIOException;
			
			public class E {
			    void foo(int a) {
			        try {
			            if (a < 10)
			                throw new FileNotFoundException();
			            else if (a < 20)
			                throw new InterruptedIOException();
			            else
			                throw new IOException();
			        } catch (FileNotFoundException | InterruptedIOException ex) {
			            ex.printStackTrace();
			        } catch (IOException e) {
			        }\s
			    }
			}
			""";
		String expected2= sample;

		sample= """
			package test1;
			
			import java.io.FileNotFoundException;
			import java.io.IOException;
			import java.io.InterruptedIOException;
			
			public class E {
			    void foo(int a) {
			        try {
			            if (a < 10)
			                throw new FileNotFoundException();
			            else if (a < 20)
			                throw new InterruptedIOException();
			            else
			                throw new IOException();
			        } catch (IOException | InterruptedIOException ex) {
			            ex.printStackTrace();
			        }\s
			    }
			}
			""";
		String expected3= sample;

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testUncaughtExceptionTryWithResources1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.FileInputStream;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar(int n) throws IllegalArgumentException, MyException {
			        if (n == 1)
			            throw new IllegalArgumentException();
			        else
			            throw new MyException();
			    }
			    void foo(String name, boolean b) {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            bar(1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3, 0); //quick fix on 1st problem
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar(int n) throws IllegalArgumentException, MyException {
			        if (n == 1)
			            throw new IllegalArgumentException();
			        else
			            throw new MyException();
			    }
			    void foo(String name, boolean b) throws FileNotFoundException, IOException {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            bar(1);
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar(int n) throws IllegalArgumentException, MyException {
			        if (n == 1)
			            throw new IllegalArgumentException();
			        else
			            throw new MyException();
			    }
			    void foo(String name, boolean b) {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            bar(1);
			        } catch (FileNotFoundException e) {
			        } catch (IOException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUncaughtExceptionTryWithResources2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.FileInputStream;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar(int n) throws IllegalArgumentException, MyException {
			        if (n == 1)
			            throw new IllegalArgumentException();
			        else
			            throw new MyException();
			    }
			    void foo(String name, boolean b) {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            bar(1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3, 1); //quick fix on 2nd problem
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar(int n) throws IllegalArgumentException, MyException {
			        if (n == 1)
			            throw new IllegalArgumentException();
			        else
			            throw new MyException();
			    }
			    void foo(String name, boolean b) throws FileNotFoundException, IOException {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            bar(1);
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar(int n) throws IllegalArgumentException, MyException {
			        if (n == 1)
			            throw new IllegalArgumentException();
			        else
			            throw new MyException();
			    }
			    void foo(String name, boolean b) {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            bar(1);
			        } catch (FileNotFoundException e) {
			        } catch (IOException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUncaughtExceptionTryWithResources3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.FileInputStream;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar(int n) throws IllegalArgumentException, MyException {
			        if (n == 1)
			            throw new IllegalArgumentException();
			        else
			            throw new MyException();
			    }
			    void foo(String name, boolean b) {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            bar(1);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 3, 2); //quick fix on 3rd problem
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.FileInputStream;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar(int n) throws IllegalArgumentException, MyException {
			        if (n == 1)
			            throw new IllegalArgumentException();
			        else
			            throw new MyException();
			    }
			    void foo(String name, boolean b) throws IllegalArgumentException, MyException {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            bar(1);
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.FileInputStream;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar(int n) throws IllegalArgumentException, MyException {
			        if (n == 1)
			            throw new IllegalArgumentException();
			        else
			            throw new MyException();
			    }
			    void foo(String name, boolean b) {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            bar(1);
			        } catch (IllegalArgumentException e) {
			        } catch (MyException e) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.io.FileInputStream;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar(int n) throws IllegalArgumentException, MyException {
			        if (n == 1)
			            throw new IllegalArgumentException();
			        else
			            throw new MyException();
			    }
			    void foo(String name, boolean b) {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            bar(1);
			        } catch (IllegalArgumentException | MyException e) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			import java.io.FileInputStream;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar(int n) throws IllegalArgumentException, MyException {
			        if (n == 1)
			            throw new IllegalArgumentException();
			        else
			            throw new MyException();
			    }
			    void foo(String name, boolean b) {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            try {
			                bar(1);
			            } catch (IllegalArgumentException e) {
			            } catch (MyException e) {
			            }
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(4);
		String preview5= getPreviewContent(proposal);

		String expected5= """
			package test1;
			import java.io.FileInputStream;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar(int n) throws IllegalArgumentException, MyException {
			        if (n == 1)
			            throw new IllegalArgumentException();
			        else
			            throw new MyException();
			    }
			    void foo(String name, boolean b) {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            try {
			                bar(1);
			            } catch (IllegalArgumentException | MyException e) {
			            }
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4, preview5 }, new String[] { expected1, expected2, expected3, expected4, expected5 });
	}

	@Test
	public void testUncaughtExceptionTryWithResources4() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=351464
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.FileInputStream;
			import java.io.IOException;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar() throws MyException {
			        throw new MyException();
			    }
			    void foo(String name, boolean b) throws IOException {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            bar();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.FileInputStream;
			import java.io.IOException;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar() throws MyException {
			        throw new MyException();
			    }
			    void foo(String name, boolean b) throws IOException, MyException {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            bar();
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.FileInputStream;
			import java.io.IOException;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar() throws MyException {
			        throw new MyException();
			    }
			    void foo(String name, boolean b) throws IOException {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            bar();
			        } catch (MyException e) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.io.FileInputStream;
			import java.io.IOException;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void bar() throws MyException {
			        throw new MyException();
			    }
			    void foo(String name, boolean b) throws IOException {
			        try (FileInputStream fis = new FileInputStream(name)) {
			            try {
			                bar();
			            } catch (MyException e) {
			            }
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}

	@Test
	public void testUncaughtExceptionTryWithResources5() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=139231
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.FileInputStream;
			public class E {
			    void foo(String name, boolean b) {
			        String e;
			        try (FileInputStream fis = new FileInputStream(name)) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 2, 0); //quick fix on 1st problem
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			public class E {
			    void foo(String name, boolean b) throws FileNotFoundException, IOException {
			        String e;
			        try (FileInputStream fis = new FileInputStream(name)) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.FileInputStream;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			public class E {
			    void foo(String name, boolean b) {
			        String e;
			        try (FileInputStream fis = new FileInputStream(name)) {
			        } catch (FileNotFoundException e1) {
			        } catch (IOException e1) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUncaughtExceptionTryWithResources6() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=478714
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			
			import java.io.ByteArrayInputStream;
			import java.io.IOException;
			import java.io.InputStream;
			
			public class E {
			    public static void main(String[] args) {
			        try (InputStream foo = new ByteArrayInputStream("foo".getBytes("UTF-8"))) {
			            String bla = new String(ByteStreams.toByteArray(foo), "UTF-8");
			        }
			    }
			}
			
			class ByteStreams {
			    public static byte[] toByteArray(InputStream foo) throws IOException, ArithmeticException {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot, 4, 2);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			
			import java.io.ByteArrayInputStream;
			import java.io.IOException;
			import java.io.InputStream;
			import java.io.UnsupportedEncodingException;
			
			public class E {
			    public static void main(String[] args) throws UnsupportedEncodingException, ArithmeticException, IOException {
			        try (InputStream foo = new ByteArrayInputStream("foo".getBytes("UTF-8"))) {
			            String bla = new String(ByteStreams.toByteArray(foo), "UTF-8");
			        }
			    }
			}
			
			class ByteStreams {
			    public static byte[] toByteArray(InputStream foo) throws IOException, ArithmeticException {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.io.ByteArrayInputStream;
			import java.io.IOException;
			import java.io.InputStream;
			import java.io.UnsupportedEncodingException;
			
			public class E {
			    public static void main(String[] args) {
			        try (InputStream foo = new ByteArrayInputStream("foo".getBytes("UTF-8"))) {
			            String bla = new String(ByteStreams.toByteArray(foo), "UTF-8");
			        } catch (UnsupportedEncodingException e) {
			        } catch (ArithmeticException e) {
			        } catch (IOException e) {
			        }
			    }
			}
			
			class ByteStreams {
			    public static byte[] toByteArray(InputStream foo) throws IOException, ArithmeticException {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			
			import java.io.ByteArrayInputStream;
			import java.io.IOException;
			import java.io.InputStream;
			
			public class E {
			    public static void main(String[] args) {
			        try (InputStream foo = new ByteArrayInputStream("foo".getBytes("UTF-8"))) {
			            String bla = new String(ByteStreams.toByteArray(foo), "UTF-8");
			        } catch (ArithmeticException | IOException e) {
			        }
			    }
			}
			
			class ByteStreams {
			    public static byte[] toByteArray(InputStream foo) throws IOException, ArithmeticException {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			
			import java.io.ByteArrayInputStream;
			import java.io.IOException;
			import java.io.InputStream;
			
			public class E {
			    public static void main(String[] args) {
			        try (InputStream foo = new ByteArrayInputStream("foo".getBytes("UTF-8"))) {
			            try {
			                String bla = new String(ByteStreams.toByteArray(foo), "UTF-8");
			            } catch (ArithmeticException | IOException e) {
			            }
			        }
			    }
			}
			
			class ByteStreams {
			    public static byte[] toByteArray(InputStream foo) throws IOException, ArithmeticException {
			        return null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(4);
		String preview5= getPreviewContent(proposal);

		String expected5= """
			package test1;
			
			import java.io.ByteArrayInputStream;
			import java.io.IOException;
			import java.io.InputStream;
			import java.io.UnsupportedEncodingException;
			
			public class E {
			    public static void main(String[] args) {
			        try (InputStream foo = new ByteArrayInputStream("foo".getBytes("UTF-8"))) {
			            try {
			                String bla = new String(ByteStreams.toByteArray(foo), "UTF-8");
			            } catch (UnsupportedEncodingException e) {
			            } catch (ArithmeticException e) {
			            } catch (IOException e) {
			            }
			        }
			    }
			}
			
			class ByteStreams {
			    public static byte[] toByteArray(InputStream foo) throws IOException, ArithmeticException {
			        return null;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4, preview5 }, new String[] { expected1, expected2, expected3, expected4, expected5 });
	}

	@Test
	public void testUnneededCaughtException1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			public class E {
			    public void foo() {
			        try {
			            throw new FileNotFoundException();
			        } catch (FileNotFoundException | IOException e) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			public class E {
			    public void foo() {
			        try {
			            throw new FileNotFoundException();
			        } catch (IOException e) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			public class E {
			    public void foo() throws FileNotFoundException {
			        try {
			            throw new FileNotFoundException();
			        } catch (IOException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnneededCaughtException2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			public class E {
			    public void foo() {
			        try {
			            throw new FileNotFoundException();
			        } catch (java.io.FileNotFoundException | java.io.IOException e) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			public class E {
			    public void foo() {
			        try {
			            throw new FileNotFoundException();
			        } catch (java.io.IOException e) {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.FileNotFoundException;
			import java.io.IOException;
			public class E {
			    public void foo() throws java.io.FileNotFoundException {
			        try {
			            throw new FileNotFoundException();
			        } catch (java.io.IOException e) {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testUnneededCatchBlockTryWithResources() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.io.FileReader;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void foo() throws Exception {
			        try (FileReader reader1 = new FileReader("file")) {
			            int ch;
			            while ((ch = reader1.read()) != -1) {
			                System.out.println(ch);
			            }
			        } catch (MyException e) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);


		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.FileReader;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void foo() throws Exception {
			        try (FileReader reader1 = new FileReader("file")) {
			            int ch;
			            while ((ch = reader1.read()) != -1) {
			                System.out.println(ch);
			            }
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.FileReader;
			class MyException extends Exception {
			    static final long serialVersionUID = 1L;
			}
			public class E {
			    void foo() throws Exception {
			        try (FileReader reader1 = new FileReader("file")) {
			            int ch;
			            while ((ch = reader1.read()) != -1) {
			                System.out.println(ch);
			            }
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testRemoveRedundantTypeArguments1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    void foo() {
			        List<String> a = new ArrayList<java.lang.String>();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			public class E {
			    void foo() {
			        List<String> a = new ArrayList<>();
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testRemoveRedundantTypeArguments2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.HashMap;
			import java.util.Map;
			public class E {
			    void foo() {
			        Map<String,String> a = new HashMap<String,String>();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.HashMap;
			import java.util.Map;
			public class E {
			    void foo() {
			        Map<String,String> a = new HashMap<>();
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testRemoveRedundantTypeArguments3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			import java.util.Map;
			public class E {
			    void foo() {
			        List<Map<String, String>> a = new ArrayList<Map<String, String>>();;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.ArrayList;
			import java.util.List;
			import java.util.Map;
			public class E {
			    void foo() {
			        List<Map<String, String>> a = new ArrayList<>();;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testRemoveRedundantTypeArguments4() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.HashMap;
			import java.util.Map;
			public class E {
			    void foo() {
			        Map<Map<String, String>, Map<String, String>> a = new HashMap<Map<String, String>, Map<String, String>>();;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList<IJavaCompletionProposal> proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			import java.util.HashMap;
			import java.util.Map;
			public class E {
			    void foo() {
			        Map<Map<String, String>, Map<String, String>> a = new HashMap<>();;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

}
