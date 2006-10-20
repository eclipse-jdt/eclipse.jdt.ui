/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.util.Arrays;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.java.FillArgumentNamesCompletionProposalCollector;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

public class CodeCompletionTest extends CoreTests {

	private final static boolean BUG_80782= true;

	private static final Class THIS= CodeCompletionTest.class;


	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new CodeCompletionTest("testSetterCompletion1"));
			return new ProjectTestSetup(suite);
		}
	}

	private IJavaProject fJProject1;

	public CodeCompletionTest(String name) {
		super(name);
	}

	private void assertAppliedProposal(String contents, IJavaCompletionProposal proposal, String completion) {
		IDocument doc= new Document(contents);
		proposal.apply(doc);
		int offset2= contents.indexOf("//here");
		String result= contents.substring(0, offset2) + completion + contents.substring(offset2);
		assertEqualString(doc.get(), result);
	}

	private void codeComplete(ICompilationUnit cu, int offset, CompletionProposalCollector collector) throws JavaModelException {
		cu.codeComplete(offset, collector);
	}

	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRTJar(fJProject1);
		JavaProjectHelper.addRequiredProject(fJProject1, ProjectTestSetup.getProject());

		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, true);
		store.setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, false);
		store.setValue(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, "/* (non-Javadoc)\n * ${see_to_overridden}\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.DELEGATECOMMENT_ID, "/* (non-Javadoc)\n * ${see_to_target}\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, "/**\n * Constructor.\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODCOMMENT_ID, "/**\n * Method.\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "//TODO\n${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.GETTERCOMMENT_ID, "/**\n * @return the ${bare_field_name}\n */", fJProject1);
		StubUtility.setCodeTemplate(CodeTemplateContextType.SETTERCOMMENT_ID, "/**\n * @param ${param} the ${bare_field_name} to set\n */", fJProject1);
	}

	protected void tearDown() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setToDefault(PreferenceConstants.CODEGEN_ADD_COMMENTS);
		store.setToDefault(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS);
		store.setToDefault(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS);
		closeAllEditors();
		JavaProjectHelper.delete(fJProject1);
	}
	
	public static void closeEditor(IEditorPart editor) {
		IWorkbenchPartSite site;
		IWorkbenchPage page;
		if (editor != null && (site= editor.getSite()) != null && (page= site.getPage()) != null)
			page.closeEditor(editor, false);
	}
	
	public static void closeAllEditors() {
		IWorkbenchWindow[] windows= PlatformUI.getWorkbench().getWorkbenchWindows();
		for (int i= 0; i < windows.length; i++) {
			IWorkbenchPage[] pages= windows[i].getPages();
			for (int j= 0; j < pages.length; j++) {
				IEditorReference[] editorReferences= pages[j].getEditorReferences();
				for (int k= 0; k < editorReferences.length; k++)
					closeEditor(editorReferences[k].getEditor(false));
			}
		}
	}

	public void testAnonymousTypeCompletion1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run= new Runnable(\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		String str= "Runnable run= new Runnable(";

		int offset= contents.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		buf= new StringBuffer();
		buf.append("package test1;\n" +
				"public class A {\n" +
				"    public void foo() {\n" +
				"        Runnable run= new Runnable(){\n" +
				"        \n" +
				"            public void run() {\n" +
				"                //TODO\n" +
				"                \n" +
				"            }\n" +
				"        \n" +
				"        };\n" +
				"    }\n" +
				"}\n" +
				"");
		assertEqualString(doc.get(), buf.toString());
	}

	public void testAnonymousTypeCompletion2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run= new Runnable();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		String str= "Runnable run= new Runnable(";

		int offset= contents.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		buf= new StringBuffer();
		buf.append("package test1;\n" +
				"public class A {\n" +
				"    public void foo() {\n" +
				"        Runnable run= new Runnable() {\n" +
				"        \n" +
				"            public void run() {\n" +
				"                //TODO\n" +
				"        \n" +
				"            }\n" +
				"        \n" +
				"        };\n" +
				"    }\n" +
				"}\n" +
				"");
		assertEqualString(doc.get(), buf.toString());
	}

	public void testConstructorCompletion() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.BufferedWriter;\n");
		buf.append("\n");
		buf.append("public class MyClass {\n");
		buf.append("    private BufferedWriter writer;\n");
		buf.append("    //here\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("MyClass.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		try {
			String str= "//here";

			int offset= contents.indexOf(str);

			CompletionProposalCollector collector= createCollector(cu, offset);
			collector.setReplacementLength(0);
	
			codeComplete(cu, offset, collector);

			IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

			IJavaCompletionProposal proposal= null;

			for (int i= 0; i < proposals.length; i++) {
				if (proposals[i].getDisplayString().startsWith("MyClass")) {
					proposal= proposals[i];
				}
			}
			assertNotNull("no proposal for MyClass()", proposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			proposal.apply(doc);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import java.io.BufferedWriter;\n");
			buf.append("\n");
			buf.append("public class MyClass {\n");
			buf.append("    private BufferedWriter writer;\n");
			buf.append("    /**\n");
			buf.append("     * Constructor.\n");
			buf.append("     */\n");
			buf.append("    public MyClass() {\n");
			buf.append("        //TODO\n");
			buf.append("\n");
			buf.append("    }//here\n");
			buf.append("}\n");
			assertEqualString(doc.get(), buf.toString());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	public void testEnumCompletions() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= "package test1;\n" +
				"\n" +
				"enum Natural {\n" +
				"	ONE,\n" +
				"	TWO,\n" +
				"	THREE\n" +
				"}\n" +
				"\n" +
				"public class Completion {\n" +
				"    \n" +
				"    void foomethod() {\n" +
				"        Natu//here\n" +
				"    }\n" +
				"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Completion.java", contents, false, null);

		String str= "//here";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= new FillArgumentNamesCompletionProposalCollector(createContext(offset, cu));
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		IJavaCompletionProposal proposal= null;
		for (int i= 0; i < proposals.length; i++) {
			if (proposals[i].getDisplayString().startsWith("Natural")) {
				proposal= proposals[i];
			}
		}
		assertNotNull("no proposal for enum Natural()", proposal);

		IDocument doc= new Document(contents);
		proposal.apply(doc);

		String result= "package test1;\n" +
				"\n" +
				"enum Natural {\n" +
				"	ONE,\n" +
				"	TWO,\n" +
				"	THREE\n" +
				"}\n" +
				"\n" +
				"public class Completion {\n" +
				"    \n" +
				"    void foomethod() {\n" +
				"        Natural//here\n" +
				"    }\n" +
				"}\n";

		assertEqualString(doc.get(), result);
	}
	
	private CompletionProposalCollector createCollector(ICompilationUnit cu, int offset) throws PartInitException, JavaModelException {
		CompletionProposalCollector collector= new CompletionProposalCollector(cu);
		collector.setInvocationContext(createContext(offset, cu));
		return collector;
	}

	private JavaContentAssistInvocationContext createContext(int offset, ICompilationUnit cu) throws PartInitException, JavaModelException {
		JavaEditor editor= (JavaEditor) JavaUI.openInEditor(cu);
		ISourceViewer viewer= editor.getViewer();
		return new JavaContentAssistInvocationContext(viewer, offset, editor);
	}

	public void testGetterCompletion1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.BufferedWriter;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    private BufferedWriter fWriter;\n");
		buf.append("    get//here\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		try {
			String str= "//here";

			int offset= contents.indexOf(str);

			CompletionProposalCollector collector= createCollector(cu, offset);
			collector.setReplacementLength(0);
	
			codeComplete(cu, offset, collector);

			IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

			IJavaCompletionProposal proposal= null;

			for (int i= 0; i < proposals.length; i++) {
				if (proposals[i].getDisplayString().startsWith("getWriter")) {
					proposal= proposals[i];
				}
			}
			assertNotNull("no proposal for getWriter()", proposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			proposal.apply(doc);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import java.io.BufferedWriter;\n");
			buf.append("\n");
			buf.append("public class A {\n");
			buf.append("    private BufferedWriter fWriter;\n");
			buf.append("    /**\n");
			buf.append("     * @return the writer\n");
			buf.append("     */\n");
			buf.append("    public BufferedWriter getWriter() {\n");
			buf.append("        return fWriter;\n");
			buf.append("    }//here\n");
			buf.append("}\n");
			assertEqualString(doc.get(), buf.toString());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	public void testMethodCompletion() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.BufferedWriter;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    private BufferedWriter writer;\n");
		buf.append("    foo//here\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		try {
			String str= "//here";

			int offset= contents.indexOf(str);

			CompletionProposalCollector collector= createCollector(cu, offset);
			collector.setReplacementLength(0);
	
			codeComplete(cu, offset, collector);

			IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
			IJavaCompletionProposal proposal= null;

			for (int i= 0; i < proposals.length; i++) {
				if (proposals[i].getDisplayString().startsWith("foo")) {
					proposal= proposals[i];
				}
			}
			assertNotNull("no proposal for foo()", proposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			proposal.apply(doc);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import java.io.BufferedWriter;\n");
			buf.append("\n");
			buf.append("public class A {\n");
			buf.append("    private BufferedWriter writer;\n");
			buf.append("    /**\n");
			buf.append("     * Method.\n");
			buf.append("     */\n");
			buf.append("    private void foo() {\n");
			buf.append("        //TODO\n");
			buf.append("\n");
			buf.append("    }//here\n");
			buf.append("}\n");
			assertEqualString(doc.get(), buf.toString());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	public void testNormalAllMethodCompletion() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= "package test1;\n" + 
				"\n" +
				"public class Completion {\n" +
				"    \n" +
				"    void foomethod() {\n" +
				"        Runnable run;\n" +
				"        run.//here\n" +
				"    }\n" +
				"}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Completion.java", contents, false, null);

		String str= "//here";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertEquals(12, proposals.length);
		CompletionProposalComparator comparator= new CompletionProposalComparator();
		comparator.setOrderAlphabetically(true);
		Arrays.sort(proposals, comparator);

		int i= 0;
		assertAppliedProposal(contents, proposals[i++], "clone()");
		assertAppliedProposal(contents, proposals[i++], "equals()");
		assertAppliedProposal(contents, proposals[i++], "finalize()");
		assertAppliedProposal(contents, proposals[i++], "getClass()");
		assertAppliedProposal(contents, proposals[i++], "hashCode()");
		assertAppliedProposal(contents, proposals[i++], "notify()");
		assertAppliedProposal(contents, proposals[i++], "notifyAll()");
		assertAppliedProposal(contents, proposals[i++], "run()");
		assertAppliedProposal(contents, proposals[i++], "toString()");
		assertAppliedProposal(contents, proposals[i++], "wait()");
		assertAppliedProposal(contents, proposals[i++], "wait()");
		assertAppliedProposal(contents, proposals[i++], "wait()");
	}

	public void testNormalAllMethodCompletionWithParametersGuessed() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, true);

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= "package test1;\n" +
				 "\n" +
				 "public class Completion {\n" +
				 "    \n" +
				 "    void foomethod() {\n" +
				 "        int intVal=5;\n" +
				 "        long longVal=3;\n" +
				 "        Runnable run;\n" +
				 "        run.//here\n" +
				 "    }\n" +
				 "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Completion.java", contents, false, null);

		String str= "//here";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= new FillArgumentNamesCompletionProposalCollector(createContext(offset, cu));
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		CompletionProposalComparator comparator= new CompletionProposalComparator();
		comparator.setOrderAlphabetically(true);
		Arrays.sort(proposals, comparator);

		int i= 0;
		assertAppliedProposal(contents, proposals[i++], "clone()");
		assertAppliedProposal(contents, proposals[i++], "equals(run)");
		assertAppliedProposal(contents, proposals[i++], "finalize()");
		assertAppliedProposal(contents, proposals[i++], "getClass()");
		assertAppliedProposal(contents, proposals[i++], "hashCode()");
		assertAppliedProposal(contents, proposals[i++], "notify()");
		assertAppliedProposal(contents, proposals[i++], "notifyAll()");
		assertAppliedProposal(contents, proposals[i++], "run()");
		assertAppliedProposal(contents, proposals[i++], "toString()");
		assertAppliedProposal(contents, proposals[i++], "wait()");
		assertAppliedProposal(contents, proposals[i++], "wait(longVal)");
		assertAppliedProposal(contents, proposals[i++], "wait(longVal, intVal)");

		assertEquals(i, proposals.length);
	}

	public void testNormalAllMethodCompletionWithParametersNames() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= "package test1;\n" +
				 "\n" +
				 "public class Completion {\n" +
				 "    \n" +
				 "    void foomethod() {\n" +
				 "        int i=5;\n" +
				 "        long l=3;\n" +
				 "        Runnable run;\n" +
				 "        run.//here\n" +
				 "    }\n" +
				 "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Completion.java", contents, false, null);

		String str= "//here";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= new FillArgumentNamesCompletionProposalCollector(createContext(offset, cu));
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		CompletionProposalComparator comparator= new CompletionProposalComparator();
		comparator.setOrderAlphabetically(true);
		Arrays.sort(proposals, comparator);

		int i= 0;
		assertAppliedProposal(contents, proposals[i++], "clone()");
		assertAppliedProposal(contents, proposals[i++], "equals(arg0)");
		assertAppliedProposal(contents, proposals[i++], "finalize()");
		assertAppliedProposal(contents, proposals[i++], "getClass()");
		assertAppliedProposal(contents, proposals[i++], "hashCode()");
		assertAppliedProposal(contents, proposals[i++], "notify()");
		assertAppliedProposal(contents, proposals[i++], "notifyAll()");
		assertAppliedProposal(contents, proposals[i++], "run()");
		assertAppliedProposal(contents, proposals[i++], "toString()");
		assertAppliedProposal(contents, proposals[i++], "wait()");
		assertAppliedProposal(contents, proposals[i++], "wait(arg0)");
		assertAppliedProposal(contents, proposals[i++], "wait(arg0, arg1)");

		assertEquals(i, proposals.length);
	}

	public void testNormalMethodCompletion() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= "package test1;\n" +
				 "\n" +
				 "public class Completion {\n" +
				 "    \n" +
				 "    void foomethod() {\n" +
				 "        this.foo//here\n" +
				 "    }\n" +
				 "}\n";
		ICompilationUnit cu= pack1.createCompilationUnit("Completion.java", contents, false, null);

		String str= "//here";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();
		IJavaCompletionProposal proposal= null;

		for (int i= 0; i < proposals.length; i++) {
			if (proposals[i].getDisplayString().startsWith("foo")) {
				proposal= proposals[i];
			}
		}
		assertNotNull("no proposal for foomethod()", proposal);

		IDocument doc= new Document(contents);
		proposal.apply(doc);

		String result= "package test1;\n" +
				 "\n" +
				 "public class Completion {\n" +
				 "    \n" +
				 "    void foomethod() {\n" +
				 "        this.foomethod()//here\n" +
				 "    }\n" +
				 "}\n";

		assertEqualString(doc.get(), result);
	}

	public void testOverrideCompletion1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.Writer;\n");
		buf.append("\n");
		buf.append("public class A extends Writer {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    //here\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		String str= "//here";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		IJavaCompletionProposal toStringProposal= null;

		for (int i= 0; i < proposals.length; i++) {
			if (proposals[i].getDisplayString().startsWith("toString()")) {
				toStringProposal= proposals[i];
			}
		}
		assertNotNull("no proposal for toString()", toStringProposal);

		IDocument doc= new Document(contents);
		toStringProposal.apply(doc);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.Writer;\n");
		buf.append("\n");
		buf.append("public class A extends Writer {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    /* (non-Javadoc)\n");
		buf.append("     * @see java.lang.Object#toString()\n");
		buf.append("     */\n");
		buf.append("    @Override\n");
		buf.append("    public String toString() {\n");
		buf.append("        //TODO\n");
		buf.append("        return super.toString();\n");
		buf.append("    }//here\n");
		buf.append("}\n");
		assertEqualString(doc.get(), buf.toString());
	}

	public void testOverrideCompletion2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.Writer;\n");
		buf.append("\n");
		buf.append("public class A extends Writer {\n" + 
				"    public void foo() {\n" + 
				"    }\n" + 
				"    //here\n" + 
				"}");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		String str= "//here";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		IJavaCompletionProposal closeProposal= null;

		for (int i= 0; i < proposals.length; i++) {
			if (proposals[i].getDisplayString().startsWith("close()")) {
				closeProposal= proposals[i];
			}
		}
		assertNotNull("no proposal for close()", closeProposal);

		IDocument doc= new Document(contents);
		closeProposal.apply(doc);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.io.Writer;\n");
		buf.append("\n");
		buf.append("public class A extends Writer {\n" + 
				"    public void foo() {\n" + 
				"    }\n" + 
				"    /* (non-Javadoc)\n" + 
				"     * @see java.io.Writer#close()\n" + 
				"     */\n" + 
				"    @Override\n" +		
				"    public void close() throws IOException {\n" + 
				"        //TODO\n" + 
				"        \n" + 
				"    }//here\n" + 
				"}");
		assertEqualString(doc.get(), buf.toString());
	}

	public void testOverrideCompletion3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.BufferedWriter;\n");
		buf.append("\n");
		buf.append("public class A extends BufferedWriter {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    //here\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		String str= "//here";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		IJavaCompletionProposal closeProposal= null;

		for (int i= 0; i < proposals.length; i++) {
			if (proposals[i].getDisplayString().startsWith("close()")) {
				closeProposal= proposals[i];
			}
		}
		assertNotNull("no proposal for close()", closeProposal);

		IDocument doc= new Document(contents);
		closeProposal.apply(doc);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.BufferedWriter;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("public class A extends BufferedWriter {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    /* (non-Javadoc)\n");
		buf.append("     * @see java.io.BufferedWriter#close()\n");
		buf.append("     */\n");
		buf.append("    @Override\n");
		buf.append("    public void close() throws IOException {\n");
		buf.append("        //TODO\n");
		buf.append("        super.close();\n");
		buf.append("    }//here\n");
		buf.append("}\n");
		assertEqualString(doc.get(), buf.toString());
	}

	public void testOverrideCompletion4() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface Inter {\n");
		buf.append("    public void foo();\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Inter.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class B extends A implements Inter {\n");
		buf.append("    foo//here\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", buf.toString(), false, null);

		String contents= buf.toString();

		String str= "//here";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		JavaModelUtil.reconcile(cu);
		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		IJavaCompletionProposal closeProposal= null;

		for (int i= 0; i < proposals.length; i++) {
			if (proposals[i].getDisplayString().startsWith("foo()")) {
				closeProposal= proposals[i];
			}
		}
		assertNotNull("no proposal for foo()", closeProposal);

		IDocument doc= new Document(contents);
		closeProposal.apply(doc);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class B extends A implements Inter {\n");
		buf.append("    /* (non-Javadoc)\n");
		buf.append("     * @see test1.A#foo()\n");
		buf.append("     */\n");
		buf.append("    @Override\n");		
		buf.append("    public void foo() {\n");
		buf.append("        //TODO\n");
		buf.append("        super.foo();\n");
		buf.append("    }//here\n");
		buf.append("}\n");
		assertEqualString(doc.get(), buf.toString());
	}

	public void testOverrideCompletion5() throws Exception {
		if (BUG_80782) {
			return;
		}

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            ru//here\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		String str= "//here";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		IJavaCompletionProposal toStringProposal= null;

		for (int i= 0; i < proposals.length; i++) {
			if (proposals[i].getDisplayString().startsWith("run()")) {
				toStringProposal= proposals[i];
			}
		}
		assertNotNull("no proposal for toString()", toStringProposal);

		IDocument doc= new Document(contents);
		toStringProposal.apply(doc);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Runnable() {\n");
		buf.append("            /* (non-Javadoc)\n");
		buf.append("             * @see java.lang.Runnable#run()\n");
		buf.append("             */\n");
		buf.append("            public void run() {\n");
		buf.append("                //TODO\n");
		buf.append("\n");
		buf.append("            }//here\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(doc.get(), buf.toString());
	}

	public void testSetterCompletion1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.BufferedWriter;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    private BufferedWriter writer;\n");
		buf.append("    se//here\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		try {
			String str= "//here";

			int offset= contents.indexOf(str);

			CompletionProposalCollector collector= createCollector(cu, offset);
			collector.setReplacementLength(0);
	
			codeComplete(cu, offset, collector);

			IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

			IJavaCompletionProposal proposal= null;

			for (int i= 0; i < proposals.length; i++) {
				if (proposals[i].getDisplayString().startsWith("setWriter")) {
					proposal= proposals[i];
				}
			}
			assertNotNull("no proposal for setWriter()", proposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			proposal.apply(doc);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import java.io.BufferedWriter;\n");
			buf.append("\n");
			buf.append("public class A {\n");
			buf.append("    private BufferedWriter writer;\n");
			buf.append("    /**\n");
			buf.append("     * @param writer the writer to set\n");
			buf.append("     */\n");
			buf.append("    public void setWriter(BufferedWriter writer) {\n");
			buf.append("        this.writer = writer;\n");
			buf.append("    }//here\n");
			buf.append("}\n");
			assertEqualString(doc.get(), buf.toString());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}
}
