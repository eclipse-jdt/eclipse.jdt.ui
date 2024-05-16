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
		String contents= """
			package test1;
			public class A {
			    public void foo() {
			        Runnable run= new Runnable(
			    }
			}
			""";

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

		String str1= """
			package test1;
			public class A {
			    public void foo() {
			        Runnable run= new Runnable() {
			           \s
			            public void run() {
			                //TODO
			               \s
			            }
			        };
			    }
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testAnonymousTypeCompletion2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			public class A {
			    public void foo() {
			        Runnable run= new Runnable();
			    }
			}
			""";

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

		String str1= """
			package test1;
			public class A {
			    public void foo() {
			        Runnable run= new Runnable() {
			           \s
			            public void run() {
			                //TODO
			               \s
			            }
			        };
			    }
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testAnonymousTypeCompletion3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			public class A {
			    interface Inner {
			        void doIt();
			    }
			    public void foo() {
			        Inner inner= new Inner();
			    }
			}
			""";

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

		String str1= """
			package test1;
			public class A {
			    interface Inner {
			        void doIt();
			    }
			    public void foo() {
			        Inner inner= new Inner() {
			           \s
			            public void doIt() {
			                //TODO
			               \s
			            }
			        };
			    }
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testAnonymousTypeCompletion4() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			public class A {
			    public void foo() {
			        abstract class Local {
			            abstract void doIt();
			        }
			        Local loc= new Local();
			    }
			}
			""";

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

		String str1= """
			package test1;
			public class A {
			    public void foo() {
			        abstract class Local {
			            abstract void doIt();
			        }
			        Local loc= new Local() {
			           \s
			            @Override
			            void doIt() {
			                //TODO
			               \s
			            }
			        };
			    }
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testAnonymousTypeCompletion5() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			public class A {
			    abstract class Local<E> {
			        abstract E doIt();
			    }
			    public void foo() {
			        new Local<String>(
			    }
			}
			""";

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

		String str1= """
			package test1;
			public class A {
			    abstract class Local<E> {
			        abstract E doIt();
			    }
			    public void foo() {
			        new Local<String>() {
			           \s
			            @Override
			            String doIt() {
			                //TODO
			                return null;
			            }
			        };
			    }
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testAnonymousTypeCompletion6() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			import java.io.Serializable;
			//BUG
			public class A {
			    public void foo() {
			        Serializable run= new Serializable(
			    }
			}
			""";

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

		String str1= """
			package test1;
			import java.io.Serializable;
			//BUG
			public class A {
			    public void foo() {
			        Serializable run= new Serializable() {
			        };
			    }
			}
			""";
		assertEquals(str1, doc.get());
	}

	// same CU
	// @NonNullByDefault on class
	// -> don't insert redundant @NonNull
	@Test
	public void testAnonymousTypeCompletion7() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		NullTestUtils.prepareNullDeclarationAnnotations(sourceFolder);

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String before= """
			package test1;
			import annots.*;
			interface Ifc {
			    @NonNull Object test(@Nullable Object i1, @NonNull Object i2);
			}
			@NonNullByDefault
			public class A {
			    public void foo() {
			        Ifc ifc= new Ifc(""";
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
				"""
			package test1;
			import annots.*;
			public interface Ifc {
			    @NonNull Object test(@Nullable Object i1, @NonNull Object i2);
			}
			""";
		pack1.createCompilationUnit("Ifc.java", ifcContents, false, null);

		String before=
				"""
			package test1;
			import annots.*;
			public class A {
			    void bar(Ifc i) {}
			    @NonNullByDefault
			    public void foo() {
			        bar(new Ifc(""";
		String after=
				"""
			);
			    }
			}
			""";
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
				"""
			package test1;
			import annots.*;
			public interface Ifc {
			    @NonNull Object test(@Nullable Object i1, @NonNull Object i2);
			}
			""";
		pack1.createCompilationUnit("Ifc.java", ifcContents, false, null);

		String before=
				"""
			package test1;
			import annots.*;
			public class A {
			    @NonNullByDefault
			    Ifc ifc= new Ifc(""";
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
				"""
			package test1;
			import annots.*;
			public interface Ifc {
			    @NonNull Object test(@Nullable Object i1, @NonNull Object i2);
			}
			""";
		pack1.createCompilationUnit("Ifc.java", ifcContents, false, null);

		String before=
				"""
			package test1;
			import annots.*;
			@NonNullByDefault
			public class A {
			    {
			        Ifc ifc= new Ifc(""";
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
		String contents= """
			package test1;
			public class Try {
			    Object m() {
			        return new Run;
			    }
			}
			""";

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

		String str1= """
			package test1;
			public class Try {
			    Object m() {
			        return new Runnable() {
			           \s
			            public void run() {
			                //TODO
			               \s
			            }
			        };
			    }
			}
			""";
		assertEquals("", str1, doc.get());
	}

	@Test
	public void testAnonymousTypeCompletionBug324391() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			public class Try {
			    Object m() {
			        take(new Run, (String) o);
			    }
			}
			""";

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

		String str1= """
			package test1;
			public class Try {
			    Object m() {
			        take(new Runnable() {
			           \s
			            public void run() {
			                //TODO
			               \s
			            }
			        }, (String) o);
			    }
			}
			""";
		assertEquals("", str1, doc.get());
	}

	@Test
	public void testAnonymousTypeCompletionBug326377() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			public class Try {
			    Object m() {
			        take(new Run)
			    }
			}
			""";

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

		String str1= """
			package test1;
			public class Try {
			    Object m() {
			        take(new Runnable() {
			           \s
			            public void run() {
			                //TODO
			               \s
			            }
			        })
			    }
			}
			""";
		assertEquals("", str1, doc.get());
	}

	@Test
	public void testAnonymousTypeCompletionBug526615() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);

		pack1.createCompilationUnit("B.java",
				"package test1;\n" +
				"public abstract class B {}",
				false, null);

		String contents= """
			package test1;
			/**
			 * Lore ipsum dolor sit amet, consectetur adipisici elit,
			 * sed eiusmod tempor incidunt ut labore et dolore magna aliqua.
			 */
			@SuppressWarnings({"rawtypes", "unchecked"})
			public class A {
			    B run= new B(
			    static class C {}
			}
			""";

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

		String str1= """
			package test1;
			/**
			 * Lore ipsum dolor sit amet, consectetur adipisici elit,
			 * sed eiusmod tempor incidunt ut labore et dolore magna aliqua.
			 */
			@SuppressWarnings({"rawtypes", "unchecked"})
			public class A {
			    B run= new B() {
			    };
			    static class C {}
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testConstructorCompletion() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			import java.io.BufferedWriter;
			
			public class MyClass {
			    private BufferedWriter writer;
			    //here
			}
			""";

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

			String str1= """
				package test1;
				
				import java.io.BufferedWriter;
				
				public class MyClass {
				    private BufferedWriter writer;
				    /**
				     * Constructor.
				     */
				    public MyClass() {
				        //TODO
				
				    }//here
				}
				""";
			assertEquals(str1, doc.get());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	@Test
	public void testEnumCompletions() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			enum Natural {
				ONE,
				TWO,
				THREE
			}
			
			public class Completion {
			   \s
			    void foomethod() {
			        Natu//here
			    }
			}
			""";
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

		String result= """
			package test1;
			
			enum Natural {
				ONE,
				TWO,
				THREE
			}
			
			public class Completion {
			   \s
			    void foomethod() {
			        Natural//here
			    }
			}
			""";

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
		String contents= """
			package test1;
			
			import java.io.BufferedWriter;
			
			public class A {
			    private BufferedWriter fWriter;
			    get//here
			}
			""";

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

			String str1= """
				package test1;
				
				import java.io.BufferedWriter;
				
				public class A {
				    private BufferedWriter fWriter;
				    /**
				     * @return the writer
				     */
				    public BufferedWriter getWriter() {
				        return fWriter;
				    }//here
				}
				""";
			assertEquals(str1, doc.get());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	@Test
	public void testMethodCompletion() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			import java.io.BufferedWriter;
			
			public class A {
			    private BufferedWriter writer;
			    foo//here
			}
			""";

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

			String str1= """
				package test1;
				
				import java.io.BufferedWriter;
				
				public class A {
				    private BufferedWriter writer;
				    /**
				     * Method.
				     */
				    private void foo() {
				        //TODO
				
				    }//here
				}
				""";
			assertEquals(str1, doc.get());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}

	@Test
	public void testNormalAllMethodCompletion() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			public class Completion {
			   \s
			    void foomethod() {
			        Runnable run;
			        run.//here
			    }
			}
			""";
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
		String contents= """
			package test1;
			
			public class Completion {
			   \s
			    void foomethod() {
			        int intVal=5;
			        long longVal=3;
			        Runnable run;
			        run.//here
			    }
			}
			""";
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
		String contents= """
			package test1;
			
			public class Completion {
			   \s
			    void foomethod() {
			        int i=5;
			        long l=3;
			        Runnable run;
			        run.//here
			    }
			}
			""";
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
		String contents= """
			package test1;
			
			public class Completion {
			   \s
			    void foomethod() {
			        this.foo//here
			    }
			}
			""";
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

		String result= """
			package test1;
			
			public class Completion {
			   \s
			    void foomethod() {
			        this.foomethod();//here
			    }
			}
			""";

		assertEquals(result, doc.get());
	}

	@Test
	public void testOverrideCompletion1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			import java.io.Writer;
			
			public class A extends Writer {
			    public void foo() {
			    }
			    //here
			}
			""";

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

		String str1= """
			package test1;
			
			import java.io.Writer;
			
			public class A extends Writer {
			    public void foo() {
			    }
			    /* (non-Javadoc)
			     * @see java.lang.Object#toString()
			     */
			    @Override
			    public String toString() {
			        //TODO
			        return super.toString();
			    }//here
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testOverrideCompletion2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			import java.io.Writer;
			
			public class A extends Writer {
			    public void foo() {
			    }
			    //here
			}""";

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

		String str1= """
			package test1;
			
			import java.io.IOException;
			import java.io.Writer;
			
			public class A extends Writer {
			    public void foo() {
			    }
			    /* (non-Javadoc)
			     * @see java.io.Writer#close()
			     */
			    @Override
			    public void close() throws IOException {
			        //TODO
			       \s
			    }//here
			}""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testOverrideCompletion3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			import java.io.BufferedWriter;
			
			public class A extends BufferedWriter {
			    public void foo() {
			    }
			    //here
			}
			""";

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

		String str1= """
			package test1;
			
			import java.io.BufferedWriter;
			import java.io.IOException;
			
			public class A extends BufferedWriter {
			    public void foo() {
			    }
			    /* (non-Javadoc)
			     * @see java.io.BufferedWriter#close()
			     */
			    @Override
			    public void close() throws IOException {
			        //TODO
			        super.close();
			    }//here
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testOverrideCompletion4() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			
			public class A {
			    public void foo() {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		String str2= """
			package test1;
			
			public interface Inter {
			    public void foo();
			}
			""";
		pack1.createCompilationUnit("Inter.java", str2, false, null);

		String str3= """
			package test1;
			
			public class B extends A implements Inter {
			    foo//here
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str3, false, null);

		String contents= str3;

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

		String str4= """
			package test1;
			
			public class B extends A implements Inter {
			    /* (non-Javadoc)
			     * @see test1.A#foo()
			     */
			    @Override
			    public void foo() {
			        //TODO
			        super.foo();
			    }//here
			}
			""";
		assertEquals(str4, doc.get());
	}

	@Ignore("BUG_80782")
	@Test
	public void testOverrideCompletion5() throws Exception {

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			public class A {
			    public void foo() {
			        new Runnable() {
			            ru//here
			        }
			    }
			}
			""";

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

		String str1= """
			package test1;
			
			public class A {
			    public void foo() {
			        new Runnable() {
			            /* (non-Javadoc)
			             * @see java.lang.Runnable#run()
			             */
			            public void run() {
			                //TODO
			
			            }//here
			        }
			    }
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testOverrideCompletion6_bug157069() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			
			public class A {
			    public class Sub { }
			    public void foo(Sub sub) {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		String str2= """
			package test1;
			
			public class B extends A {
			    foo//here
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("B.java", str2, false, null);

		String contents= str2;

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

		String str3= """
			package test1;
			
			public class B extends A {
			    /* (non-Javadoc)
			     * @see test1.A#foo(test1.A.Sub)
			     */
			    @Override
			    public void foo(Sub sub) {
			        //TODO
			        super.foo(sub);
			    }//here
			}
			""";
		assertEquals(str3, doc.get());
	}

	@Test
	public void testOverrideCompletion7_bug355926() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=355926
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			interface Z<T> {}
			
			class A {
			    void foo(Z<?>... zs) {
			    }
			}
			class B extends A {
			    //here
			}
			""";

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

		String str1= """
			package test1;
			
			interface Z<T> {}
			
			class A {
			    void foo(Z<?>... zs) {
			    }
			}
			class B extends A {
			    /* (non-Javadoc)
			     * @see test1.A#foo(test1.Z[])
			     */
			    @Override
			    void foo(Z<?>... zs) {
			        //TODO
			        super.foo(zs);
			    }//here
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testOverrideCompletion8_bug355926() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=355926
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			interface Z<T> {}
			
			class A {
			    void foo(Z<?>[] zs) {
			    }
			}
			class B extends A {
			    //here
			}
			""";

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

		String str1= """
			package test1;
			
			interface Z<T> {}
			
			class A {
			    void foo(Z<?>[] zs) {
			    }
			}
			class B extends A {
			    /* (non-Javadoc)
			     * @see test1.A#foo(test1.Z[])
			     */
			    @Override
			    void foo(Z<?>[] zs) {
			        //TODO
			        super.foo(zs);
			    }//here
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testOverrideCompletion9_bug355926() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=355926
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			import java.util.List;
			
			interface Z<T, U> {}
			
			class A {
			    void foo(Z<String, List<String>> zs) {
			    }
			}
			class B extends A {
			    //here
			}
			""";

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

		String str1= """
			package test1;
			
			import java.util.List;
			
			interface Z<T, U> {}
			
			class A {
			    void foo(Z<String, List<String>> zs) {
			    }
			}
			class B extends A {
			    /* (non-Javadoc)
			     * @see test1.A#foo(test1.Z)
			     */
			    @Override
			    void foo(Z<String, List<String>> zs) {
			        //TODO
			        super.foo(zs);
			    }//here
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testOverrideCompletion10_bug377184() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=377184
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			class Super<T> {
			    void foo(T t) {
			    }
			}
			public class Impl<T2 extends Number> extends Super<T2> {
			    foo//here
			}
			""";

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

		String str1= """
			package test1;
			class Super<T> {
			    void foo(T t) {
			    }
			}
			public class Impl<T2 extends Number> extends Super<T2> {
			    /* (non-Javadoc)
			     * @see test1.Super#foo(java.lang.Object)
			     */
			    @Override
			    void foo(T2 t) {
			        //TODO
			        super.foo(t);
			    }//here
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testOverrideCompletionArrayOfTypeVariable() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=391265
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			class Super {
			    public <T extends Number> void foo(T[] t) {
			    }
			}
			public class Impl extends Super {
			    foo//here
			}
			""";

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

		String str1= """
			package test1;
			class Super {
			    public <T extends Number> void foo(T[] t) {
			    }
			}
			public class Impl extends Super {
			    /* (non-Javadoc)
			     * @see test1.Super#foo(java.lang.Number[])
			     */
			    @Override
			    public <T extends Number> void foo(T[] t) {
			        //TODO
			        super.foo(t);
			    }//here
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testSetterCompletion1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			
			import java.io.BufferedWriter;
			
			public class A {
			    private BufferedWriter writer;
			    se//here
			}
			""";

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

			String str1= """
				package test1;
				
				import java.io.BufferedWriter;
				
				public class A {
				    private BufferedWriter writer;
				    /**
				     * @param writer the writer to set
				     */
				    public void setWriter(BufferedWriter writer) {
				        this.writer = writer;
				    }//here
				}
				""";
			assertEquals(str1, doc.get());
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
		String str1= """
			package test1;
			
			public class A {
			    public static void foo() {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		String contents= """
			package test1;
			
			public class B {
			    public void bar() {
			        f//here
			    }
			}
			""";

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

			String str2= """
				package test1;
				
				import static test1.A.foo;
				
				public class B {
				    public void bar() {
				        foo();//here
				    }
				}
				""";
			assertEquals(str2, doc.get());
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
		String str1= """
			package test1;
			
			public class A {
			    public static void foo() {
			    }
			}
			""";
		pack1.createCompilationUnit("A.java", str1, false, null);

		IPackageFragment pack2= sourceFolder.createPackageFragment("test2", false, null);
		String contents= """
			package test2;
			
			public class B {
			    public void bar() {
			        f//here
			    }
			    public void foo(int x) {
			    }
			}
			""";

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

			String str2= """
				package test2;
				
				import test1.A;
				
				public class B {
				    public void bar() {
				        A.foo();//here
				    }
				    public void foo(int x) {
				    }
				}
				""";
			assertEquals(str2, doc.get());
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
		String contents= """
			package test1;
			import java.util.ArrayL; // here
			public class A {
			}
			""";
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

		String str1= """
			package test1;
			import java.util.ArrayList; // here
			public class A {
			}
			""";
		assertEquals(str1, doc.get());
	}

	/*
	 * Ensure no extra ';' is inserted, whereas a selected text part is correctly replaced
	 */
	@Test
	public void testImportReplacingSelection() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			import java.util.ArrayLWrong;
			public class A {
			}
			""";
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

		String str1= """
			package test1;
			import java.util.ArrayList;
			public class A {
			}
			""";
		assertEquals(str1, doc.get());
	}

	/*
	 * Ensure no extra ';' is inserted, whereas remain text is replaced as per the preference option
	 */
	@Test
	public void testImportReplacing() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);

		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			package test1;
			import java.util.ArrayLWrong;
			public class A {
			}
			""";
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

		String str1= """
			package test1;
			import java.util.ArrayList;
			public class A {
			}
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testConstructorCompletion_Bug336451() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		String contents= """
			public class EclipseTest {
			   private static interface InvokerIF{
			       public <T extends ArgIF, Y> T invoke(T arg) throws RuntimeException, IndexOutOfBoundsException;
			   }
			   private static class Invoker implements InvokerIF{       \s
			       public <T extends ArgIF, Y> T invoke(T arg){         \s
			           return arg;                                      \s
			       }                                                    \s
			   }                                                        \s
			                                                            \s
			   private static interface ArgIF{                          \s
			   }                                                        \s
			                                                            \s
			   private static interface ArgIF2<C> extends ArgIF{        \s
			                                                            \s
			   }                                                        \s
			   private static class ArgImpl<C> implements ArgIF2<C>{    \s
			       public ArgImpl() {                                   \s
			           super();                                         \s
			       }                                                    \s
			   }                                                        \s
			   public static void main(String[] args) throws Exception {\s
			       InvokerIF test = new Invoker();                      \s
			       test.invoke(new ArgImpl)                             \s
			   }                                                        \s
			}                                                            \s
			""";

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

		String str1= """
			public class EclipseTest {
			   private static interface InvokerIF{
			       public <T extends ArgIF, Y> T invoke(T arg) throws RuntimeException, IndexOutOfBoundsException;
			   }
			   private static class Invoker implements InvokerIF{       \s
			       public <T extends ArgIF, Y> T invoke(T arg){         \s
			           return arg;                                      \s
			       }                                                    \s
			   }                                                        \s
			                                                            \s
			   private static interface ArgIF{                          \s
			   }                                                        \s
			                                                            \s
			   private static interface ArgIF2<C> extends ArgIF{        \s
			                                                            \s
			   }                                                        \s
			   private static class ArgImpl<C> implements ArgIF2<C>{    \s
			       public ArgImpl() {                                   \s
			           super();                                         \s
			       }                                                    \s
			   }                                                        \s
			   public static void main(String[] args) throws Exception {\s
			       InvokerIF test = new Invoker();                      \s
			       test.invoke(new ArgImpl<C>())                             \s
			   }                                                        \s
			}                                                            \s
			""";
		assertEquals(str1, doc.get());
	}

	@Test
	public void testBug466252() throws CoreException {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack1= sourceFolder.createPackageFragment("p", false, null);
		String str1= """
			package p;
			
			public class C {
			    void foo() {
			        try {
			        }\s
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("CC.java", str1, false, null);


		String str= "}";
		int offset= str1.indexOf(str) + str.length();

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

			String str= """
				package annots;
				
				public enum DefaultLocation { PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS, TYPE_PARAMETER }
				""";
			pack0.createCompilationUnit("DefaultLocation.java", str, false, null);

			String str1= """
				package annots;
				
				import java.lang.annotation.*;
				import static annots.DefaultLocation.*;
				
				@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
				@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.LOCAL_VARIABLE })
				public @interface NonNullByDefault { DefaultLocation[] value() default {PARAMETER, RETURN_TYPE, FIELD, TYPE_BOUND, TYPE_ARGUMENT}; }
				""";
			pack0.createCompilationUnit("NonNullByDefault.java", str1, false, null);

			IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
			String contents= """
				@annots.NonNullByDefault({ARRAY})
				package test1;
				""";

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

			String expected= """
				@annots.NonNullByDefault({ARRAY_CONTENTS})
				package test1;
				
				import static annots.DefaultLocation.ARRAY_CONTENTS;
				""";
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
				"""
			package pack;
			class Fred {
				Object xyzObject;
			}
			public class Bar {
				Fred fred() { return new Fred(); }n\
				Bar foo() {
					return (Bar)(fred().xyz);
				}
			}
			""";
		ICompilationUnit cu= pack0.createCompilationUnit("Bar.java", contents, false, null);
		int offset = contents.indexOf("(fred().x") + "(fred().x".length();
		JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();
		List<ICompletionProposal> proposals= computer.computeCompletionProposals(createContext(offset, cu), null);
		assertEquals("expect 1 proposal", 1, proposals.size());

		IDocument doc= new Document(contents);
		proposals.get(0).apply(doc);

		String expected=
				"""
			package pack;
			class Fred {
				Object xyzObject;
			}
			public class Bar {
				Fred fred() { return new Fred(); }n	Bar foo() {
					return (Bar)(fred().xyzObject);
				}
			}
			""";
		assertEquals(expected, doc.get());
	}

	@Test
	public void testArgumentName() throws CoreException {
		// copy-adjusted from JDT/Core's CompletionRecoveryTest.test12() to validate replacement
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack0= sourceFolder.createPackageFragment("pack", false, null);
		String contents=
				"""
			package pack;
			class A  {
			
				public static void main(String[] argv
						new Member().f
						;
				}
				class Member {
					int foo()
					}
				}
			};
			""";
		ICompilationUnit cu= pack0.createCompilationUnit("A.java", contents, false, null);
		int offset = contents.indexOf("argv") + "argv".length();
		JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();
		List<ICompletionProposal> proposals= computer.computeCompletionProposals(createContext(offset, cu), null);
		assertEquals("expect 1 proposal", 1, proposals.size());

		IDocument doc= new Document(contents);
		proposals.get(0).apply(doc);

		String expected=
				"""
			package pack;
			class A  {
			
				public static void main(String[] argvStrings
						new Member().f
						;
				}
				class Member {
					int foo()
					}
				}
			};
			""";
		assertEquals(expected, doc.get());
	}

	@Test
	public void superCallInAnonymous() throws CoreException {
		// copy-adjusted from JDT/Core's CompletionParserTest2.test0137_Method() to validate replacement
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack0= sourceFolder.createPackageFragment("pack", false, null);
		String contents=
				"""
			package pack;\
			class MyObject {
				void zzzX() {}
			}
			public class X {
				void foo(){
					new MyObject(){
						void bar(){
							super.zzz();
						}
					};
				}
			}
			""";
		String completeBehind = "zzz(";
		int cursorLocation = contents.indexOf(completeBehind) + completeBehind.length() - 1;
		ICompilationUnit cu= pack0.createCompilationUnit("X.java", contents, false, null);
		JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();
		List<ICompletionProposal> proposals= computer.computeCompletionProposals(createContext(cursorLocation, cu), null);
		assertEquals("expect 1 proposal", 1, proposals.size());

		IDocument doc= new Document(contents);
		proposals.get(0).apply(doc);

		String expected=
				"""
			package pack;\
			class MyObject {
				void zzzX() {}
			}
			public class X {
				void foo(){
					new MyObject(){
						void bar(){
							super.zzzX();
						}
					};
				}
			}
			""";
		assertEquals(expected, doc.get());
	}

	@Test
	public void testInIfStatement() throws CoreException {
		// copy-adjusted from JDT/Core's MethodInvocationCompletionTest.testInIfStatement() to validate replacement
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack0= sourceFolder.createPackageFragment("pack", false, null);
		String contents =
				"""
			package pack;\
			class Bar {
				void freddy() {}
			}
			class X {
				void foo(Bar bar) {
					if (true) {
						bar.fred();
					}
				}
			}
			""";
		String completeBehind = "fred(";
		int cursorLocation = contents.indexOf(completeBehind) + completeBehind.length() - 1;
		ICompilationUnit cu= pack0.createCompilationUnit("X.java", contents, false, null);
		JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();
		List<ICompletionProposal> proposals= computer.computeCompletionProposals(createContext(cursorLocation, cu), null);
		assertEquals("expect 1 proposal", 1, proposals.size());

		IDocument doc= new Document(contents);
		proposals.get(0).apply(doc);

		String expected=
				"""
			package pack;\
			class Bar {
				void freddy() {}
			}
			class X {
				void foo(Bar bar) {
					if (true) {
						bar.freddy();
					}
				}
			}
			""";
		assertEquals(expected, doc.get());
	}

	@Test
	public void testWithExpressionReceiver() throws CoreException {
		// copy-adjusted from JDT/Core's MethodInvocationCompletionTest.testWithExpressionReceiver() to validate replacement
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack0= sourceFolder.createPackageFragment("pack", false, null);
		String contents =
				"""
			package pack;\
			class Bar {
				void freddy() {}
			}
			class X {
				Bar bar() { return new Bar(); }
				void foo() {
					if (true) {
						bar().fred();
					}
				}
			}
			""";
		String completeBehind = "fred(";
		int cursorLocation = contents.indexOf(completeBehind) + completeBehind.length() - 1;
		ICompilationUnit cu= pack0.createCompilationUnit("X.java", contents, false, null);
		JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();
		List<ICompletionProposal> proposals= computer.computeCompletionProposals(createContext(cursorLocation, cu), null);
		assertEquals("expect 1 proposal", 1, proposals.size());

		IDocument doc= new Document(contents);
		proposals.get(0).apply(doc);

		String expected=
				"""
			package pack;\
			class Bar {
				void freddy() {}
			}
			class X {
				Bar bar() { return new Bar(); }
				void foo() {
					if (true) {
						bar().freddy();
					}
				}
			}
			""";
		assertEquals(expected, doc.get());
	}

	@Test
	public void test0215_Method() throws CoreException {
		// copy-adjusted from JDT/Core's GenericsCompletionParserTest.test0215_Method() to validate replacement
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		IPackageFragment pack0= sourceFolder.createPackageFragment("pack", false, null);
		String contents =
				"""
			package pack;
			public class X {
				<T> void bar(T t) { }
				void foo() {
			      this.<X>bar();
			   }\
			}
			""";

		String completeBehind = "<X>bar(";
		int cursorLocation = contents.indexOf(completeBehind) + completeBehind.length() - 1;
		ICompilationUnit cu= pack0.createCompilationUnit("X.java", contents, false, null);
		JavaCompletionProposalComputer computer= new JavaNoTypeCompletionProposalComputer();
		List<ICompletionProposal> proposals= computer.computeCompletionProposals(createContext(cursorLocation, cu), null);
		assertEquals("expect 1 proposal", 1, proposals.size());

		IDocument doc= new Document(contents);
		proposals.get(0).apply(doc);

		String expected=
				"""
			package pack;
			public class X {
				<T> void bar(T t) { }
				void foo() {
			      this.<X>bar(t);
			   }\
			}
			""";
		assertEquals(expected, doc.get());
	}
}
