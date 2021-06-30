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
 *     Stephan Herrmann - Contribution for Bug 463360 - [override method][null] generating method override should not create redundant null annotations
 *******************************************************************************/
package org.eclipse.jdt.text.tests.contentassist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.NullTestUtils;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.java.JavaContextType;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.java.CompletionProposalCollector;
import org.eclipse.jdt.ui.text.java.CompletionProposalComparator;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.java.AbstractJavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.FillArgumentNamesCompletionProposalCollector;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProcessor;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposalComputer;
import org.eclipse.jdt.internal.ui.text.java.JavaNoTypeCompletionProposalComputer;
import org.eclipse.jdt.internal.ui.text.java.JavaTypeCompletionProposalComputer;

//predictable order for https://bugs.eclipse.org/bugs/show_bug.cgi?id=423416
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CodeCompletionTest extends AbstractCompletionTest {

	private IJavaProject fJProject1;

	private void assertAppliedProposal(String contents, IJavaCompletionProposal proposal, String completion) {
		IDocument doc= new Document(contents);
		proposal.apply(doc);
		int offset2= contents.indexOf("//here");
		String result= contents.substring(0, offset2) + completion + contents.substring(offset2);
		assertEquals(doc.get(), result);
	}

	private void codeComplete(ICompilationUnit cu, int offset, CompletionProposalCollector collector) throws JavaModelException {
		// logging for https://bugs.eclipse.org/bugs/show_bug.cgi?id=423416
		System.out.println();
		System.out.println("---- " + getClass().getName() + "#" + getName() + " ----");
		System.out.println("offset: " + offset);
		System.out.println("cu: " + cu);
		IBuffer buffer= cu.getBuffer();
		System.out.println("buffer: " + buffer);
		System.out.println("source: |" + buffer.getContents() + "|");

		System.out.print("file contents: |");
		File file= cu.getResource().getLocation().toFile();
		try {
			try (BufferedReader reader= new BufferedReader(new FileReader(file))) {
				String line;
				while ((line= reader.readLine()) != null) {
					System.out.println(line);
				}
				System.out.println("|");
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println();

		IJavaProject project= cu.getJavaProject();
		System.out.println(project);
		for (IPackageFragmentRoot root : project.getAllPackageFragmentRoots()) {
			if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
				for (IJavaElement pack : root.getChildren()) {
					((IPackageFragment) pack).getChildren(); // side-effect: opens the package
					System.out.println(pack);
				}
			}
		}
		System.out.println();

		cu.codeComplete(offset, collector, new NullProgressMonitor());
	}

	@Override
	public void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRTJar(fJProject1);

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, true);
		store.setValue(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS, false);
		store.setValue(PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS, false);
		store.setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, true);

		StubUtility.setCodeTemplate(CodeTemplateContextType.OVERRIDECOMMENT_ID, "/* (non-Javadoc)\n * ${see_to_overridden}\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.DELEGATECOMMENT_ID, "/* (non-Javadoc)\n * ${see_to_target}\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, "/**\n * Constructor.\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODCOMMENT_ID, "/**\n * Method.\n */", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, "//TODO\n${body_statement}", null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.GETTERCOMMENT_ID, "/**\n * @return the ${bare_field_name}\n */", fJProject1);
		StubUtility.setCodeTemplate(CodeTemplateContextType.SETTERCOMMENT_ID, "/**\n * @param ${param} the ${bare_field_name} to set\n */", fJProject1);
	}

	@Override
	public void tearDown() throws Exception {
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
		if (editor != null && (site= editor.getSite()) != null && (page= site.getPage()) != null) {
			page.closeEditor(editor, false);
		}
	}

	public static void closeAllEditors() {
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editorReference : page.getEditorReferences()) {
					closeEditor(editorReference.getEditor(false));
				}
			}
		}
	}

	@Test
	public void testAnonymousTypeCompletion1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

		buf= new StringBuilder();
		buf.append("package test1;\n" +
				"public class A {\n" +
				"    public void foo() {\n" +
				"        Runnable run= new Runnable() {\n" +
				"            \n" +
				"            public void run() {\n" +
				"                //TODO\n" +
				"                \n" +
				"            }\n" +
				"        };\n" +
				"    }\n" +
				"}\n" +
				"");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testAnonymousTypeCompletion2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

		buf= new StringBuilder();
		buf.append("package test1;\n" +
				"public class A {\n" +
				"    public void foo() {\n" +
				"        Runnable run= new Runnable() {\n" +
				"            \n" +
				"            public void run() {\n" +
				"                //TODO\n" +
				"                \n" +
				"            }\n" +
				"        };\n" +
				"    }\n" +
				"}\n" +
				"");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testAnonymousTypeCompletion3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    interface Inner {\n");
		buf.append("        void doIt();\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        Inner inner= new Inner();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		String str= "Inner inner= new Inner(";

		int offset= contents.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    interface Inner {\n");
		buf.append("        void doIt();\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        Inner inner= new Inner() {\n");
		buf.append("            \n");
		buf.append("            public void doIt() {\n");
		buf.append("                //TODO\n");
		buf.append("                \n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testAnonymousTypeCompletion4() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        abstract class Local {\n");
		buf.append("            abstract void doIt();\n");
		buf.append("        }\n");
		buf.append("        Local loc= new Local();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		String str= "Local loc= new Local(";

		int offset= contents.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        abstract class Local {\n");
		buf.append("            abstract void doIt();\n");
		buf.append("        }\n");
		buf.append("        Local loc= new Local() {\n");
		buf.append("            \n");
		buf.append("            @Override\n");
		buf.append("            void doIt() {\n");
		buf.append("                //TODO\n");
		buf.append("                \n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testAnonymousTypeCompletion5() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    abstract class Local<E> {\n");
		buf.append("        abstract E doIt();\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");

		buf.append("        new Local<String>(\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		String str= "new Local<String>(";

		int offset= contents.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    abstract class Local<E> {\n");
		buf.append("        abstract E doIt();\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        new Local<String>() {\n");
		buf.append("            \n");
		buf.append("            @Override\n");
		buf.append("            String doIt() {\n");
		buf.append("                //TODO\n");
		buf.append("                return null;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testAnonymousTypeCompletion6() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("//BUG\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Serializable run= new Serializable(\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		String str= "Serializable run= new Serializable(";

		int offset= contents.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n" +
				"import java.io.Serializable;\n"+
				"//BUG\n"+
				"public class A {\n" +
				"    public void foo() {\n" +
				"        Serializable run= new Serializable() {\n" +
				"        };\n" +
				"    }\n" +
				"}\n" +
				"");
		assertEquals(buf.toString(), doc.get());
	}

	// same CU
	// @NonNullByDefault on class
	// -> don't insert redundant @NonNull
	@Test
	public void testAnonymousTypeCompletion7() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		NullTestUtils.prepareNullDeclarationAnnotations(sourceFolder);

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String before= "package test1;\n" +
				"import annots.*;\n" +
				"interface Ifc {\n" +
				"    @NonNull Object test(@Nullable Object i1, @NonNull Object i2);\n" +
				"}\n" +
				"@NonNullByDefault\n" +
				"public class A {\n" +
				"    public void foo() {\n" +
				"        Ifc ifc= new Ifc(";
		String after= "    }\n" +
				"}\n";
		String contents= before + '\n' + after;

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		int offset= before.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		String expected= before
				+
				") {\n" +
				"            \n" +
				"            public Object test(@Nullable Object i1, Object i2) {\n" +
				"                //TODO\n" +
				"                return null;\n" +
				"            }\n" +
				"        };\n"
				+ after;
		assertEquals(expected, doc.get());
	}

	// not same CU
	// @NonNullByDefault on method
	// anonymous class is argument in a method invocation.
	// -> don't insert redundant @NonNull
	@Test
	public void testAnonymousTypeCompletion8() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		NullTestUtils.prepareNullDeclarationAnnotations(sourceFolder);

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);

		String ifcContents=
				"package test1;\n" +
				"import annots.*;\n" +
				"public interface Ifc {\n" +
				"    @NonNull Object test(@Nullable Object i1, @NonNull Object i2);\n" +
				"}\n";
		pack1.createCompilationUnit("Ifc.java", ifcContents, false, null);

		String before=
				"package test1;\n" +
				"import annots.*;\n" +
				"public class A {\n" +
				"    void bar(Ifc i) {}\n" +
				"    @NonNullByDefault\n" +
				"    public void foo() {\n" +
				"        bar(new Ifc(";
		String after=
				");\n" +
				"    }\n" +
				"}\n";
		String contents= before + '\n' + after;

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		int offset= before.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		String expected= before
				+
				") {\n" +
				"            \n" +
				"            public Object test(@Nullable Object i1, Object i2) {\n" +
				"                //TODO\n" +
				"                return null;\n" +
				"            }\n" +
				"        };\n"
				+ after;
		assertEquals(expected, doc.get());
	}

	// not same CU
	// @NonNullByDefault on field
	// -> don't insert redundant @NonNull
	@Test
	public void testAnonymousTypeCompletion9() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		NullTestUtils.prepareNullDeclarationAnnotations(sourceFolder);

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);

		String ifcContents=
				"package test1;\n" +
				"import annots.*;\n" +
				"public interface Ifc {\n" +
				"    @NonNull Object test(@Nullable Object i1, @NonNull Object i2);\n" +
				"}\n";
		pack1.createCompilationUnit("Ifc.java", ifcContents, false, null);

		String before=
				"package test1;\n" +
				"import annots.*;\n" +
				"public class A {\n" +
				"    @NonNullByDefault\n" +
				"    Ifc ifc= new Ifc(";
		String after=
				"}\n";
		String contents= before + '\n' + after;

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		int offset= before.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		String expected= before
				+
				") {\n" +
				"        \n" +
				"        public Object test(@Nullable Object i1, Object i2) {\n" +
				"            //TODO\n" +
				"            return null;\n" +
				"        }\n" +
				"    };\n"
				+ after;
		assertEquals(expected, doc.get());
	}

	// not same CU
	// @NonNullByDefault on class, completion in instance initializer
	// -> don't insert redundant @NonNull
	@Test
	public void testAnonymousTypeCompletion10() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		NullTestUtils.prepareNullDeclarationAnnotations(sourceFolder);

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);

		String ifcContents=
				"package test1;\n" +
				"import annots.*;\n" +
				"public interface Ifc {\n" +
				"    @NonNull Object test(@Nullable Object i1, @NonNull Object i2);\n" +
				"}\n";
		pack1.createCompilationUnit("Ifc.java", ifcContents, false, null);

		String before=
				"package test1;\n" +
				"import annots.*;\n" +
				"@NonNullByDefault\n" +
				"public class A {\n" +
				"    {\n" +
				"        Ifc ifc= new Ifc(";
		String after=
				"    }\n" +
				"}\n";
		String contents= before + '\n' + after;

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		int offset= before.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		String expected= before
				+
				") {\n" +
				"            \n" +
				"            public Object test(@Nullable Object i1, Object i2) {\n" +
				"                //TODO\n" +
				"                return null;\n" +
				"            }\n" +
				"        };\n"
				+ after;
		assertEquals(expected, doc.get());
	}

	@Test
	public void testAnonymousTypeCompletionBug280801() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class Try {\n");
		buf.append("    Object m() {\n");
		buf.append("        return new Run;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("Try.java", contents, false, null);

		String str= "new Run";
		int offset= contents.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		collector.setAllowsRequiredProposals(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);

		cu.codeComplete(offset, collector, new NullProgressMonitor());

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class Try {\n");
		buf.append("    Object m() {\n");
		buf.append("        return new Runnable() {\n");
		buf.append("            \n");
		buf.append("            public void run() {\n");
		buf.append("                //TODO\n");
		buf.append("                \n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEquals("", buf.toString(), doc.get());
	}

	@Test
	public void testAnonymousTypeCompletionBug324391() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class Try {\n");
		buf.append("    Object m() {\n");
		buf.append("        take(new Run, (String) o);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("Try.java", contents, false, null);

		String str= "new Run";
		int offset= contents.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		collector.setAllowsRequiredProposals(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);

		cu.codeComplete(offset, collector, new NullProgressMonitor());

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class Try {\n");
		buf.append("    Object m() {\n");
		buf.append("        take(new Runnable() {\n");
		buf.append("            \n");
		buf.append("            public void run() {\n");
		buf.append("                //TODO\n");
		buf.append("                \n");
		buf.append("            }\n");
		buf.append("        }, (String) o);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEquals("", buf.toString(), doc.get());
	}

	@Test
	public void testAnonymousTypeCompletionBug326377() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class Try {\n");
		buf.append("    Object m() {\n");
		buf.append("        take(new Run)\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("Try.java", contents, false, null);

		String str= "new Run";
		int offset= contents.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);
		collector.setAllowsRequiredProposals(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);

		cu.codeComplete(offset, collector, new NullProgressMonitor());

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("public class Try {\n");
		buf.append("    Object m() {\n");
		buf.append("        take(new Runnable() {\n");
		buf.append("            \n");
		buf.append("            public void run() {\n");
		buf.append("                //TODO\n");
		buf.append("                \n");
		buf.append("            }\n");
		buf.append("        })\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEquals("", buf.toString(), doc.get());
	}

	@Test
	public void testAnonymousTypeCompletionBug526615() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);

		pack1.createCompilationUnit("B.java",
				"package test1;\n" +
				"public abstract class B {}",
				false, null);

		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * Lore ipsum dolor sit amet, consectetur adipisici elit,\n");
		buf.append(" * sed eiusmod tempor incidunt ut labore et dolore magna aliqua.\n");
		buf.append(" */\n");
		buf.append("@SuppressWarnings({\"rawtypes\", \"unchecked\"})\n");
		buf.append("public class A {\n");
		buf.append("    B run= new B(\n");
		buf.append("    static class C {}\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		String str= "B run= new B(";

		int offset= contents.indexOf(str) + str.length();

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * Lore ipsum dolor sit amet, consectetur adipisici elit,\n");
		buf.append(" * sed eiusmod tempor incidunt ut labore et dolore magna aliqua.\n");
		buf.append(" */\n");
		buf.append("@SuppressWarnings({\"rawtypes\", \"unchecked\"})\n");
		buf.append("public class A {\n");
		buf.append("    B run= new B() {\n");
		buf.append("    };\n");
		buf.append("    static class C {}\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testConstructorCompletion() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

			IJavaCompletionProposal proposal= null;

			for (IJavaCompletionProposal p : collector.getJavaCompletionProposals()) {
				if (p.getDisplayString().startsWith("MyClass")) {
					proposal= p;
				}
			}
			assertNotNull("no proposal for MyClass()", proposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			proposal.apply(doc);

			buf= new StringBuilder();
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
			assertEquals(buf.toString(), doc.get());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	@Test
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
		collector.setIgnored(CompletionProposal.TYPE_REF, false);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal proposal= null;
		for (IJavaCompletionProposal p : collector.getJavaCompletionProposals()) {
			if (p.getDisplayString().startsWith("Natural")) {
				proposal= p;
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

		assertEquals(result, doc.get());
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

	@Test
	public void testGetterCompletion1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

			IJavaCompletionProposal proposal= null;

			for (IJavaCompletionProposal p : collector.getJavaCompletionProposals()) {
				if (p.getDisplayString().startsWith("getWriter")) {
					proposal= p;
				}
			}
			assertNotNull("no proposal for getWriter()", proposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			proposal.apply(doc);

			buf= new StringBuilder();
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
			assertEquals(buf.toString(), doc.get());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	@Test
	public void testMethodCompletion() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

			IJavaCompletionProposal proposal= null;

			for (IJavaCompletionProposal p : collector.getJavaCompletionProposals()) {
				if (p.getDisplayString().startsWith("foo")) {
					proposal= p;
				}
			}
			assertNotNull("no proposal for foo()", proposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			proposal.apply(doc);

			buf= new StringBuilder();
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
			assertEquals(buf.toString(), doc.get());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	@Test
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
		assertAppliedProposal(contents, proposals[i++], "finalize();");
		assertAppliedProposal(contents, proposals[i++], "getClass()");
		assertAppliedProposal(contents, proposals[i++], "hashCode()");
		assertAppliedProposal(contents, proposals[i++], "notify();");
		assertAppliedProposal(contents, proposals[i++], "notifyAll();");
		assertAppliedProposal(contents, proposals[i++], "run();");
		assertAppliedProposal(contents, proposals[i++], "toString()");
		assertAppliedProposal(contents, proposals[i++], "wait();");
		assertAppliedProposal(contents, proposals[i++], "wait();");
		assertAppliedProposal(contents, proposals[i++], "wait();");
	}

	@Test
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
		collector.setIgnored(CompletionProposal.METHOD_REF, false);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		CompletionProposalComparator comparator= new CompletionProposalComparator();
		comparator.setOrderAlphabetically(true);
		Arrays.sort(proposals, comparator);

		int i= 0;
		assertAppliedProposal(contents, proposals[i++], "clone()");
		assertAppliedProposal(contents, proposals[i++], "equals(run)");
		assertAppliedProposal(contents, proposals[i++], "finalize();");
		assertAppliedProposal(contents, proposals[i++], "getClass()");
		assertAppliedProposal(contents, proposals[i++], "hashCode()");
		assertAppliedProposal(contents, proposals[i++], "notify();");
		assertAppliedProposal(contents, proposals[i++], "notifyAll();");
		assertAppliedProposal(contents, proposals[i++], "run();");
		assertAppliedProposal(contents, proposals[i++], "toString()");
		assertAppliedProposal(contents, proposals[i++], "wait();");
		assertAppliedProposal(contents, proposals[i++], "wait(longVal);");
		assertAppliedProposal(contents, proposals[i++], "wait(longVal, intVal);");

		assertEquals(i, proposals.length);
	}

	@Test
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
		collector.setIgnored(CompletionProposal.METHOD_REF, false);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		CompletionProposalComparator comparator= new CompletionProposalComparator();
		comparator.setOrderAlphabetically(true);
		Arrays.sort(proposals, comparator);

		int i= 0;
		assertAppliedProposal(contents, proposals[i++], "clone()");
		assertAppliedProposal(contents, proposals[i++], "equals(arg0)");
		assertAppliedProposal(contents, proposals[i++], "finalize();");
		assertAppliedProposal(contents, proposals[i++], "getClass()");
		assertAppliedProposal(contents, proposals[i++], "hashCode()");
		assertAppliedProposal(contents, proposals[i++], "notify();");
		assertAppliedProposal(contents, proposals[i++], "notifyAll();");
		assertAppliedProposal(contents, proposals[i++], "run();");
		assertAppliedProposal(contents, proposals[i++], "toString()");
		assertAppliedProposal(contents, proposals[i++], "wait();");
		assertAppliedProposal(contents, proposals[i++], "wait(arg0);");
		assertAppliedProposal(contents, proposals[i++], "wait(arg0, arg1);");

		assertEquals(i, proposals.length);
	}

	@Test
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

		IJavaCompletionProposal proposal= null;

		for (IJavaCompletionProposal p : collector.getJavaCompletionProposals()) {
			if (p.getDisplayString().startsWith("foo")) {
				proposal= p;
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
				"        this.foomethod();//here\n" +
				 "    }\n" +
				 "}\n";

		assertEquals(result, doc.get());
	}

	@Test
	public void testOverrideCompletion1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

		for (IJavaCompletionProposal proposal : proposals) {
			if (proposal.getDisplayString().startsWith("toString()")) {
				toStringProposal= proposal;
			}
		}
		assertNotNull("no proposal for toString()", toStringProposal);

		IDocument doc= new Document(contents);
		toStringProposal.apply(doc);

		buf= new StringBuilder();
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
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testOverrideCompletion2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

		for (IJavaCompletionProposal proposal : proposals) {
			if (proposal.getDisplayString().startsWith("close()")) {
				closeProposal= proposal;
			}
		}
		assertNotNull("no proposal for close()", closeProposal);

		IDocument doc= new Document(contents);
		closeProposal.apply(doc);

		buf= new StringBuilder();
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
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testOverrideCompletion3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

		for (IJavaCompletionProposal proposal : proposals) {
			if (proposal.getDisplayString().startsWith("close()")) {
				closeProposal= proposal;
			}
		}
		assertNotNull("no proposal for close()", closeProposal);

		IDocument doc= new Document(contents);
		closeProposal.apply(doc);

		buf= new StringBuilder();
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
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testOverrideCompletion4() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface Inter {\n");
		buf.append("    public void foo();\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Inter.java", buf.toString(), false, null);

		buf= new StringBuilder();
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

		for (IJavaCompletionProposal proposal : proposals) {
			if (proposal.getDisplayString().startsWith("foo()")) {
				closeProposal= proposal;
			}
		}
		assertNotNull("no proposal for foo()", closeProposal);

		IDocument doc= new Document(contents);
		closeProposal.apply(doc);

		buf= new StringBuilder();
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
		assertEquals(buf.toString(), doc.get());
	}

	@Ignore("BUG_80782")
	@Test
	public void testOverrideCompletion5() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

		for (IJavaCompletionProposal proposal : proposals) {
			if (proposal.getDisplayString().startsWith("run()")) {
				toStringProposal= proposal;
			}
		}
		assertNotNull("no proposal for toString()", toStringProposal);

		IDocument doc= new Document(contents);
		toStringProposal.apply(doc);

		buf= new StringBuilder();
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
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testOverrideCompletion6_bug157069() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    public class Sub { }\n");
		buf.append("    public void foo(Sub sub) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class B extends A {\n");
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

		for (IJavaCompletionProposal proposal : proposals) {
			if (proposal.getDisplayString().startsWith("foo(")) {
				closeProposal= proposal;
			}
		}
		assertNotNull("no proposal for foo(Sub)", closeProposal);

		IDocument doc= new Document(contents);
		closeProposal.apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class B extends A {\n");
		buf.append("    /* (non-Javadoc)\n");
		buf.append("     * @see test1.A#foo(test1.A.Sub)\n");
		buf.append("     */\n");
		buf.append("    @Override\n");
		buf.append("    public void foo(Sub sub) {\n");
		buf.append("        //TODO\n");
		buf.append("        super.foo(sub);\n");
		buf.append("    }//here\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testOverrideCompletion7_bug355926() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=355926
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface Z<T> {}\n");
		buf.append("\n");
		buf.append("class A {\n");
		buf.append("    void foo(Z<?>... zs) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class B extends A {\n");
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

		for (IJavaCompletionProposal proposal : proposals) {
			if (proposal.getDisplayString().startsWith("foo")) {
				toStringProposal= proposal;
			}
		}
		assertNotNull("no proposal for foo(...)", toStringProposal);

		IDocument doc= new Document(contents);
		toStringProposal.apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface Z<T> {}\n");
		buf.append("\n");
		buf.append("class A {\n");
		buf.append("    void foo(Z<?>... zs) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class B extends A {\n");
		buf.append("    /* (non-Javadoc)\n");
		buf.append("     * @see test1.A#foo(test1.Z[])\n");
		buf.append("     */\n");
		buf.append("    @Override\n");
		buf.append("    void foo(Z<?>... zs) {\n");
		buf.append("        //TODO\n");
		buf.append("        super.foo(zs);\n");
		buf.append("    }//here\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testOverrideCompletion8_bug355926() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=355926
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface Z<T> {}\n");
		buf.append("\n");
		buf.append("class A {\n");
		buf.append("    void foo(Z<?>[] zs) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class B extends A {\n");
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

		for (IJavaCompletionProposal proposal : proposals) {
			if (proposal.getDisplayString().startsWith("foo")) {
				toStringProposal= proposal;
			}
		}
		assertNotNull("no proposal for foo(...)", toStringProposal);

		IDocument doc= new Document(contents);
		toStringProposal.apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("interface Z<T> {}\n");
		buf.append("\n");
		buf.append("class A {\n");
		buf.append("    void foo(Z<?>[] zs) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class B extends A {\n");
		buf.append("    /* (non-Javadoc)\n");
		buf.append("     * @see test1.A#foo(test1.Z[])\n");
		buf.append("     */\n");
		buf.append("    @Override\n");
		buf.append("    void foo(Z<?>[] zs) {\n");
		buf.append("        //TODO\n");
		buf.append("        super.foo(zs);\n");
		buf.append("    }//here\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testOverrideCompletion9_bug355926() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=355926
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("interface Z<T, U> {}\n");
		buf.append("\n");
		buf.append("class A {\n");
		buf.append("    void foo(Z<String, List<String>> zs) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class B extends A {\n");
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

		for (IJavaCompletionProposal proposal : proposals) {
			if (proposal.getDisplayString().startsWith("foo")) {
				toStringProposal= proposal;
			}
		}
		assertNotNull("no proposal for foo(...)", toStringProposal);

		IDocument doc= new Document(contents);
		toStringProposal.apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.List;\n");
		buf.append("\n");
		buf.append("interface Z<T, U> {}\n");
		buf.append("\n");
		buf.append("class A {\n");
		buf.append("    void foo(Z<String, List<String>> zs) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class B extends A {\n");
		buf.append("    /* (non-Javadoc)\n");
		buf.append("     * @see test1.A#foo(test1.Z)\n");
		buf.append("     */\n");
		buf.append("    @Override\n");
		buf.append("    void foo(Z<String, List<String>> zs) {\n");
		buf.append("        //TODO\n");
		buf.append("        super.foo(zs);\n");
		buf.append("    }//here\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testOverrideCompletion10_bug377184() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=377184
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Super<T> {\n");
		buf.append("    void foo(T t) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("public class Impl<T2 extends Number> extends Super<T2> {\n");
		buf.append("    foo//here\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("Impl.java", contents, false, null);

		String str= "//here";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal toStringProposal= null;
		for (IJavaCompletionProposal proposal : collector.getJavaCompletionProposals()) {
			if (proposal.getDisplayString().startsWith("foo")) {
				toStringProposal= proposal;
			}
		}
		assertNotNull("no proposal for foo(...)", toStringProposal);

		IDocument doc= new Document(contents);
		toStringProposal.apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Super<T> {\n");
		buf.append("    void foo(T t) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("public class Impl<T2 extends Number> extends Super<T2> {\n");
		buf.append("    /* (non-Javadoc)\n");
		buf.append("     * @see test1.Super#foo(java.lang.Object)\n");
		buf.append("     */\n");
		buf.append("    @Override\n");
		buf.append("    void foo(T2 t) {\n");
		buf.append("        //TODO\n");
		buf.append("        super.foo(t);\n");
		buf.append("    }//here\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testOverrideCompletionArrayOfTypeVariable() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=391265
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Super {\n");
		buf.append("    public <T extends Number> void foo(T[] t) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("public class Impl extends Super {\n");
		buf.append("    foo//here\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("Impl.java", contents, false, null);

		String str= "//here";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal toStringProposal= null;
		for (IJavaCompletionProposal proposal : collector.getJavaCompletionProposals()) {
			if (proposal.getDisplayString().startsWith("foo")) {
				toStringProposal= proposal;
			}
		}
		assertNotNull("no proposal for foo(...)", toStringProposal);

		IDocument doc= new Document(contents);
		toStringProposal.apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("class Super {\n");
		buf.append("    public <T extends Number> void foo(T[] t) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("public class Impl extends Super {\n");
		buf.append("    /* (non-Javadoc)\n");
		buf.append("     * @see test1.Super#foo(java.lang.Number[])\n");
		buf.append("     */\n");
		buf.append("    @Override\n");
		buf.append("    public <T extends Number> void foo(T[] t) {\n");
		buf.append("        //TODO\n");
		buf.append("        super.foo(t);\n");
		buf.append("    }//here\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testSetterCompletion1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
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

			IJavaCompletionProposal proposal= null;

			for (IJavaCompletionProposal p : collector.getJavaCompletionProposals()) {
				if (p.getDisplayString().startsWith("setWriter")) {
					proposal= p;
				}
			}
			assertNotNull("no proposal for setWriter()", proposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			proposal.apply(doc);

			buf= new StringBuilder();
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
			assertEquals(buf.toString(), doc.get());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	@Test
	public void testStaticImports1() throws Exception {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		store.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "test1.A.foo");

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    public static void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class B {\n");
		buf.append("    public void bar() {\n");
		buf.append("        f//here\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("B.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		try {
			String str= "//here";

			int offset= contents.indexOf(str);

			ISourceViewer viewer= ((JavaEditor) part).getViewer();

			JavaContentAssistInvocationContext context= new JavaContentAssistInvocationContext(viewer, offset, part);
			JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();

			// make sure we get an import rewrite context
			SharedASTProviderCore.getAST(cu, SharedASTProviderCore.WAIT_YES, null);

			ICompletionProposal proposal= null;

			for (ICompletionProposal curr : computer.computeCompletionProposals(context, null)) {
				if (curr.getDisplayString().startsWith("foo")) {
					proposal= curr;
				}
			}
			assertNotNull("no proposal for foo()", proposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			proposal.apply(doc);

			buf= new StringBuilder();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import static test1.A.foo;\n");
			buf.append("\n");
			buf.append("public class B {\n");
			buf.append("    public void bar() {\n");
			buf.append("        foo();//here\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEquals(buf.toString(), doc.get());
		} finally {
			store.setToDefault(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);
			part.getSite().getPage().closeAllEditors(false);

		}
	}

	@Test
	public void testStaticImports2() throws Exception {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		store.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "test1.A.foo");

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class A {\n");
		buf.append("    public static void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);

		IPackageFragment pack2= sourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuilder();
		buf.append("package test2;\n");
		buf.append("\n");
		buf.append("public class B {\n");
		buf.append("    public void bar() {\n");
		buf.append("        f//here\n");
		buf.append("    }\n");
		buf.append("    public void foo(int x) {\n"); // conflicting method, no static import possible
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();

		ICompilationUnit cu= pack2.createCompilationUnit("B.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		try {
			String str= "//here";

			int offset= contents.indexOf(str);

			ISourceViewer viewer= ((JavaEditor) part).getViewer();

			JavaContentAssistInvocationContext context= new JavaContentAssistInvocationContext(viewer, offset, part);
			JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();

			// make sure we get an import rewrite context
			SharedASTProviderCore.getAST(cu, SharedASTProviderCore.WAIT_YES, null);

			List<ICompletionProposal> proposals= computer.computeCompletionProposals(context, null);

			ICompletionProposal proposal= null;

			for (ICompletionProposal curr : proposals) {
				if (curr.getDisplayString().startsWith("foo()")) {
					proposal= curr;
				}
			}
			assertNotNull("no proposal for foo()", proposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			proposal.apply(doc);

			buf= new StringBuilder();
			buf.append("package test2;\n");
			buf.append("\n");
			buf.append("import test1.A;\n");
			buf.append("\n");
			buf.append("public class B {\n");
			buf.append("    public void bar() {\n");
			buf.append("        A.foo();//here\n");
			buf.append("    }\n");
			buf.append("    public void foo(int x) {\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEquals(buf.toString(), doc.get());
		} finally {
			store.setToDefault(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);
			part.getSite().getPage().closeAllEditors(false);

		}
	}

	/*
	 * Ensure no extra ';' is inserted
	 */
	@Test
	public void testImport() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayL; // here\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		String contents= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		String str= "; // here";
		int offset= contents.indexOf(str);

		IEditorPart part= JavaUI.openInEditor(cu);
		ISourceViewer viewer= ((JavaEditor) part).getViewer();
		JavaContentAssistInvocationContext context= new JavaContentAssistInvocationContext(viewer, offset, part);
		JavaCompletionProposalComputer computer= new JavaTypeCompletionProposalComputer();

		// make sure we get an import rewrite context
		SharedASTProviderCore.getAST(cu, SharedASTProviderCore.WAIT_YES, null);

		List<ICompletionProposal> proposals= computer.computeCompletionProposals(context, null);
		assertEquals("Expecting 1 proposal", 1, proposals.size());

		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals.get(0).apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList; // here\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	/*
	 * Ensure no extra ';' is inserted, whereas a selected text part is correctly replaced
	 */
	@Test
	public void testImportReplacingSelection() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayLWrong;\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		String contents= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		String str= "Wrong";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setAllowsRequiredProposals(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);
		collector.setReplacementLength("Wrong".length());

		codeComplete(cu, offset, collector);
		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertEquals("expect 1 proposal", 1, proposals.length);

		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[0].apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	/*
	 * Ensure no extra ';' is inserted, whereas remain text is replaced as per the preference option
	 */
	@Test
	public void testImportReplacing() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayLWrong;\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		String contents= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		IEditorPart part= JavaUI.openInEditor(cu);
		String str= "Wrong";

		int offset= contents.indexOf(str);

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setAllowsRequiredProposals(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);

		codeComplete(cu, offset, collector);
		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertEquals("expect 1 proposal", 1, proposals.length);

		IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
		proposals[0].apply(doc);

		buf= new StringBuilder();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("public class A {\n");
		buf.append("}\n");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testConstructorCompletion_Bug336451() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("public class EclipseTest {\n");
		buf.append("   private static interface InvokerIF{\n");
		buf.append("       public <T extends ArgIF, Y> T invoke(T arg) throws RuntimeException, IndexOutOfBoundsException;\n");
		buf.append("   }\n");
		buf.append("   private static class Invoker implements InvokerIF{        \n");
		buf.append("       public <T extends ArgIF, Y> T invoke(T arg){          \n");
		buf.append("           return arg;                                       \n");
		buf.append("       }                                                     \n");
		buf.append("   }                                                         \n");
		buf.append("                                                             \n");
		buf.append("   private static interface ArgIF{                           \n");
		buf.append("   }                                                         \n");
		buf.append("                                                             \n");
		buf.append("   private static interface ArgIF2<C> extends ArgIF{         \n");
		buf.append("                                                             \n");
		buf.append("   }                                                         \n");
		buf.append("   private static class ArgImpl<C> implements ArgIF2<C>{     \n");
		buf.append("       public ArgImpl() {                                    \n");
		buf.append("           super();                                          \n");
		buf.append("       }                                                     \n");
		buf.append("   }                                                         \n");
		buf.append("   public static void main(String[] args) throws Exception { \n");
		buf.append("       InvokerIF test = new Invoker();                       \n");
		buf.append("       test.invoke(new ArgImpl)                              \n");
		buf.append("   }                                                         \n");
		buf.append("}                                                             \n");
		String contents= buf.toString();

		ICompilationUnit cu= pack1.createCompilationUnit("EclipseTest.java", contents, false, null);

		String str= "test.invoke(new ArgImpl)";

		int offset= contents.indexOf(str) + str.length() - 1;

		CompletionProposalCollector collector= createCollector(cu, offset);
		collector.setAllowsRequiredProposals(CompletionProposal.CONSTRUCTOR_INVOCATION, CompletionProposal.TYPE_REF, true);

		collector.setReplacementLength(0);

		codeComplete(cu, offset, collector);

		IJavaCompletionProposal[] proposals= collector.getJavaCompletionProposals();

		assertNumberOf("proposals", proposals.length, 1);

		IDocument doc= new Document(contents);

		proposals[0].apply(doc);

		buf= new StringBuilder();
		buf.append("public class EclipseTest {\n");
		buf.append("   private static interface InvokerIF{\n");
		buf.append("       public <T extends ArgIF, Y> T invoke(T arg) throws RuntimeException, IndexOutOfBoundsException;\n");
		buf.append("   }\n");
		buf.append("   private static class Invoker implements InvokerIF{        \n");
		buf.append("       public <T extends ArgIF, Y> T invoke(T arg){          \n");
		buf.append("           return arg;                                       \n");
		buf.append("       }                                                     \n");
		buf.append("   }                                                         \n");
		buf.append("                                                             \n");
		buf.append("   private static interface ArgIF{                           \n");
		buf.append("   }                                                         \n");
		buf.append("                                                             \n");
		buf.append("   private static interface ArgIF2<C> extends ArgIF{         \n");
		buf.append("                                                             \n");
		buf.append("   }                                                         \n");
		buf.append("   private static class ArgImpl<C> implements ArgIF2<C>{     \n");
		buf.append("       public ArgImpl() {                                    \n");
		buf.append("           super();                                          \n");
		buf.append("       }                                                     \n");
		buf.append("   }                                                         \n");
		buf.append("   public static void main(String[] args) throws Exception { \n");
		buf.append("       InvokerIF test = new Invoker();                       \n");
		buf.append("       test.invoke(new ArgImpl<C>())                              \n");
		buf.append("   }                                                         \n");
		buf.append("}                                                             \n");
		assertEquals(buf.toString(), doc.get());
	}

	@Test
	public void testBug466252() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("p", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package p;\n");
		buf.append("\n");
		buf.append("public class C {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");
		buf.append("        } \n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("CC.java", buf.toString(), false, null);


		String str= "}";
		int offset= buf.toString().indexOf(str) + str.length();

		TemplateStore templateStore= JavaPlugin.getDefault().getTemplateStore();
		int tokenLocation= createContext(offset, cu).getCoreContext().getTokenLocation();
		assertNotEquals(0, tokenLocation & CompletionContext.TL_STATEMENT_START);

		Template[] templates= templateStore.getTemplates(JavaContextType.ID_STATEMENTS);

		boolean finallyTemplateFound= false;
		for (Template template : templates) {
			if ("finally".equals(template.getName())) {
				finallyTemplateFound= true;
				break;
			}
		}
		assertTrue(finallyTemplateFound);
	}

	@Test
	public void testCompletionInPackageInfo() throws Exception {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		store.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "annots.DefaultLocation.*");
		try {
			IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
			IPackageFragment pack0= sourceFolder.createPackageFragment("annots", false, null);

			StringBuilder buf= new StringBuilder();
			buf.append("package annots;\n");
			buf.append("\n");
			buf.append("public enum DefaultLocation { PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS, TYPE_PARAMETER }\n");
			pack0.createCompilationUnit("DefaultLocation.java", buf.toString(), false, null);

			buf= new StringBuilder();
			buf.append("package annots;\n");
			buf.append("\n");
			buf.append("import java.lang.annotation.*;\n");
			buf.append("import static annots.DefaultLocation.*;\n");
			buf.append("\n");
			buf.append("@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)\n");
			buf.append("@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.LOCAL_VARIABLE })\n");
			buf.append("public @interface NonNullByDefault { DefaultLocation[] value() default {PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT}; }\n");
			pack0.createCompilationUnit("NonNullByDefault.java", buf.toString(), false, null);

			IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
			buf= new StringBuilder();
			buf.append("@annots.NonNullByDefault({ARRAY})\n");
			buf.append("package test1;\n");
			String contents= buf.toString();

			ICompilationUnit cu= pack1.createCompilationUnit("package-info.java", contents, false, null);

			int offset= contents.indexOf("{ARRAY}") + 6;

			JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();

			// make sure we get an import rewrite context
			SharedASTProviderCore.getAST(cu, SharedASTProviderCore.WAIT_YES, null);

			List<ICompletionProposal> proposals= computer.computeCompletionProposals(createContext(offset, cu), null);


			ICompletionProposal proposal= null;

			for (ICompletionProposal curr : proposals) {
				if (curr instanceof AbstractJavaCompletionProposal) {
					AbstractJavaCompletionProposal javaProposal= (AbstractJavaCompletionProposal) curr;
					if ("ARRAY_CONTENTS".equals(javaProposal.getReplacementString())) {
						proposal= curr;
						break;
					}
				}
			}
			assertNotNull("proposal not found", proposal);

			IDocument doc= new Document(contents);
			proposal.apply(doc);

			buf= new StringBuilder();
			buf.append("@annots.NonNullByDefault({ARRAY_CONTENTS})\n");
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import static annots.DefaultLocation.ARRAY_CONTENTS;\n");
			String expected= buf.toString();
			assertEquals(expected, doc.get());
		} finally {
			store.setToDefault(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);
		}
	}

	private static void assertNumberOf(String name, int is, int expected) {
		assertEquals("Wrong number of " + name + ", is: " + is + ", expected: " + expected, expected, is);
	}

	@Test
	public void testComputeCompletionInNonUIThread() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		ICompilationUnit cu= pack1.createCompilationUnit("Blah.java", "", true, new NullProgressMonitor());
		JavaEditor part= (JavaEditor) JavaUI.openInEditor(cu);
		ContentAssistant assistant= new ContentAssistant();
		assistant.setDocumentPartitioning(IJavaPartitions.JAVA_PARTITIONING);
		JavaCompletionProcessor javaProcessor= new JavaCompletionProcessor(part, assistant, getContentType());
		AtomicReference<Throwable> exception = new AtomicReference<>();
		List<IStatus> errors = new ArrayList<>();
		JavaPlugin.getDefault().getLog().addLogListener((status, plugin) -> {
			if (status.getSeverity() >= IStatus.WARNING) {
				errors.add(status);
			}
		});
		Thread thread = new Thread(() -> {
			try {
				javaProcessor.computeCompletionProposals(part.getViewer(), 0);
				// a popup can be shown and block the thread in case of error
			} catch (Exception e) {
				exception.set(e);
			}
		});
		thread.start();
		thread.join();
		if (exception.get() != null) {
			exception.get().printStackTrace();
		}
		assertNull(exception.get());
		assertEquals(Collections.emptyList(), errors);
	}

	@Test
	public void testCastExpressionUnaryExpression() throws CoreException {
		// copy-adjusted from JDT/Core's FieldAccessCompletionTest.testCastExpressionUnaryExpression() to validate replacement
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack0= sourceFolder.createPackageFragment("pack", false, null);
		String contents=
				"package pack;\n" +
				"class Fred {\n" +
				"	Object xyzObject;\n" +
				"}\n" +
				"public class Bar {\n" +
				"	Fred fred() { return new Fred(); }n" +
				"	Bar foo() {\n" +
				"		return (Bar)(fred().xyz);\n" +
				"	}\n" +
				"}\n";
		ICompilationUnit cu= pack0.createCompilationUnit("Bar.java", contents, false, null);
		int offset = contents.indexOf("(fred().x") + "(fred().x".length();
		JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();
		List<ICompletionProposal> proposals= computer.computeCompletionProposals(createContext(offset, cu), null);
		assertEquals("expect 1 proposal", 1, proposals.size());

		IDocument doc= new Document(contents);
		proposals.get(0).apply(doc);

		String expected=
				"package pack;\n" +
				"class Fred {\n" +
				"	Object xyzObject;\n" +
				"}\n" +
				"public class Bar {\n" +
				"	Fred fred() { return new Fred(); }n	Bar foo() {\n" +
				"		return (Bar)(fred().xyzObject);\n" +
				"	}\n" +
				"}\n";
		assertEquals(expected, doc.get());
	}

	@Test
	public void testArgumentName() throws CoreException {
		// copy-adjusted from JDT/Core's CompletionRecoveryTest.test12() to validate replacement
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack0= sourceFolder.createPackageFragment("pack", false, null);
		String contents=
				"package pack;\n"+
				"class A  {\n"+
				"\n"+
				"	public static void main(String[] argv\n"+
				"			new Member().f\n"+
				"			;\n"+
				"	}\n"+
				"	class Member {\n"+
				"		int foo()\n"+
				"		}\n"+
				"	}\n"+
				"};\n";
		ICompilationUnit cu= pack0.createCompilationUnit("A.java", contents, false, null);
		int offset = contents.indexOf("argv") + "argv".length();
		JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();
		List<ICompletionProposal> proposals= computer.computeCompletionProposals(createContext(offset, cu), null);
		assertEquals("expect 1 proposal", 1, proposals.size());

		IDocument doc= new Document(contents);
		proposals.get(0).apply(doc);

		String expected=
				"package pack;\n"+
				"class A  {\n"+
				"\n"+
				"	public static void main(String[] argvStrings\n"+
				"			new Member().f\n"+
				"			;\n"+
				"	}\n"+
				"	class Member {\n"+
				"		int foo()\n"+
				"		}\n"+
				"	}\n"+
				"};\n";
		assertEquals(expected, doc.get());
	}

	@Test
	public void superCallInAnonymous() throws CoreException {
		// copy-adjusted from JDT/Core's CompletionParserTest2.test0137_Method() to validate replacement
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack0= sourceFolder.createPackageFragment("pack", false, null);
		String contents=
				"package pack;" +
				"class MyObject {\n" +
				"	void zzzX() {}\n" +
				"}\n" +
				"public class X {\n" +
				"	void foo(){\n" +
				"		new MyObject(){\n" +
				"			void bar(){\n" +
				"				super.zzz();\n" +
				"			}\n" +
				"		};\n" +
				"	}\n" +
				"}\n";
		String completeBehind = "zzz(";
		int cursorLocation = contents.indexOf(completeBehind) + completeBehind.length() - 1;
		ICompilationUnit cu= pack0.createCompilationUnit("X.java", contents, false, null);
		JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();
		List<ICompletionProposal> proposals= computer.computeCompletionProposals(createContext(cursorLocation, cu), null);
		assertEquals("expect 1 proposal", 1, proposals.size());

		IDocument doc= new Document(contents);
		proposals.get(0).apply(doc);

		String expected=
				"package pack;" +
				"class MyObject {\n" +
				"	void zzzX() {}\n" +
				"}\n" +
				"public class X {\n" +
				"	void foo(){\n" +
				"		new MyObject(){\n" +
				"			void bar(){\n" +
				"				super.zzzX();\n" +
				"			}\n" +
				"		};\n" +
				"	}\n" +
				"}\n";
		assertEquals(expected, doc.get());
	}

	@Test
	public void testInIfStatement() throws CoreException {
		// copy-adjusted from JDT/Core's MethodInvocationCompletionTest.testInIfStatement() to validate replacement
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack0= sourceFolder.createPackageFragment("pack", false, null);
		String contents =
				"package pack;" +
				"class Bar {\n" +
				"	void freddy() {}\n" +
				"}\n" +
				"class X {\n" +
				"	void foo(Bar bar) {\n" +
				"		if (true) {\n" +
				"			bar.fred();\n" +
				"		}\n" +
				"	}\n" +
				"}\n";
		String completeBehind = "fred(";
		int cursorLocation = contents.indexOf(completeBehind) + completeBehind.length() - 1;
		ICompilationUnit cu= pack0.createCompilationUnit("X.java", contents, false, null);
		JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();
		List<ICompletionProposal> proposals= computer.computeCompletionProposals(createContext(cursorLocation, cu), null);
		assertEquals("expect 1 proposal", 1, proposals.size());

		IDocument doc= new Document(contents);
		proposals.get(0).apply(doc);

		String expected=
				"package pack;" +
				"class Bar {\n" +
				"	void freddy() {}\n" +
				"}\n" +
				"class X {\n" +
				"	void foo(Bar bar) {\n" +
				"		if (true) {\n" +
				"			bar.freddy();\n" +
				"		}\n" +
				"	}\n" +
				"}\n";
		assertEquals(expected, doc.get());
	}

	@Test
	public void testWithExpressionReceiver() throws CoreException {
		// copy-adjusted from JDT/Core's MethodInvocationCompletionTest.testWithExpressionReceiver() to validate replacement
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack0= sourceFolder.createPackageFragment("pack", false, null);
		String contents =
				"package pack;" +
				"class Bar {\n" +
				"	void freddy() {}\n" +
				"}\n" +
				"class X {\n" +
				"	Bar bar() { return new Bar(); }\n" +
				"	void foo() {\n" +
				"		if (true) {\n" +
				"			bar().fred();\n" +
				"		}\n" +
				"	}\n" +
				"}\n";
		String completeBehind = "fred(";
		int cursorLocation = contents.indexOf(completeBehind) + completeBehind.length() - 1;
		ICompilationUnit cu= pack0.createCompilationUnit("X.java", contents, false, null);
		JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();
		List<ICompletionProposal> proposals= computer.computeCompletionProposals(createContext(cursorLocation, cu), null);
		assertEquals("expect 1 proposal", 1, proposals.size());

		IDocument doc= new Document(contents);
		proposals.get(0).apply(doc);

		String expected=
				"package pack;" +
				"class Bar {\n" +
				"	void freddy() {}\n" +
				"}\n" +
				"class X {\n" +
				"	Bar bar() { return new Bar(); }\n" +
				"	void foo() {\n" +
				"		if (true) {\n" +
				"			bar().freddy();\n" +
				"		}\n" +
				"	}\n" +
				"}\n";
		assertEquals(expected, doc.get());
	}

	@Test
	public void test0215_Method() throws CoreException {
		// copy-adjusted from JDT/Core's GenericsCompletionParserTest.test0215_Method() to validate replacement
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack0= sourceFolder.createPackageFragment("pack", false, null);
		String contents =
				"package pack;\n" +
				"public class X {\n" +
				"	<T> void bar(T t) { }\n" +
				"	void foo() {\n" +
				"      this.<X>bar();\n" +
				"   }" +
				"}\n";

		String completeBehind = "<X>bar(";
		int cursorLocation = contents.indexOf(completeBehind) + completeBehind.length() - 1;
		ICompilationUnit cu= pack0.createCompilationUnit("X.java", contents, false, null);
		JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();
		List<ICompletionProposal> proposals= computer.computeCompletionProposals(createContext(cursorLocation, cu), null);
		assertEquals("expect 1 proposal", 1, proposals.size());

		IDocument doc= new Document(contents);
		proposals.get(0).apply(doc);

		String expected=
				"package pack;\n" +
				"public class X {\n" +
				"	<T> void bar(T t) { }\n" +
				"	void foo() {\n" +
				"      this.<X>bar(t);\n" +
				"   }" +
				"}\n";
		assertEquals(expected, doc.get());
	}
}
