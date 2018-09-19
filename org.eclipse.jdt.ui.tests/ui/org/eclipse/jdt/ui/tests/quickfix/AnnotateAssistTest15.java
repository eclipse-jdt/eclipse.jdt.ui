/*******************************************************************************
 * Copyright (c) 2015, 2017 GK Software AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.compiler.CompilationResult;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;
import org.eclipse.jdt.ui.tests.quickfix.JarUtil.ClassFileFilter;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AnnotateAssistTest15 extends AbstractAnnotateAssistTests {

	protected static final String ANNOTATION_PATH= "annots";
	
	protected static final Class<?> THIS= AnnotateAssistTest15.class;
	
	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}
	
	public AnnotateAssistTest15(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		fJProject1= ProjectTestSetup.getProject();
		fJProject1.getProject().getFolder(ANNOTATION_PATH).create(true, true, null);
		fJProject1.setOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
	}
	
	// === Tests ===

	/*
	 * Assert that the "Annotate" command can be invoked on a ClassFileEditor
	 */
	public void testAnnotateReturn() throws Exception {
		
		String MY_MAP_PATH= "pack/age/MyMap";
		String[] pathAndContents= new String[] {
					MY_MAP_PATH+".java",
					"package pack.age;\n" +
					"public interface MyMap<K,V> {\n" +
					"    public V get(K key);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5, null);
		IType type= fJProject1.findType(MY_MAP_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			SourceViewer viewer= (SourceViewer)javaEditor.getViewer();

			// invoke the full command and asynchronously collect the result:
			final ICompletionProposal[] proposalBox= new ICompletionProposal[1];
			viewer.getQuickAssistAssistant().addCompletionListener(new ICompletionListener() {
				@Override
				public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
					proposalBox[0]= proposal;
				}
				@Override
				public void assistSessionStarted(ContentAssistEvent event) { /* nop */ }
				@Override
				public void assistSessionEnded(ContentAssistEvent event) { /* nop */ }
			});

			int offset= pathAndContents[1].indexOf("V get");
			viewer.setSelection(new TextSelection(offset, 0));
			viewer.doOperation(JavaSourceViewer.ANNOTATE_CLASS_FILE);

			int count = 10;
			while (proposalBox[0] == null && count-- > 0)
				Thread.sleep(200);
			ICompletionProposal proposal= proposalBox[0];
			assertNotNull("should have a proposal", proposal);
			
			viewer.getQuickAssistAssistant().uninstall();
			JavaProjectHelper.emptyDisplayLoop();

			assertEquals("expect proposal", "Annotate as '@NonNull V'", proposal.getDisplayString());
			String expectedInfo=
					"<dl><dt>get</dt>" +
					"<dd>(TK;)TV;</dd>" +
					"<dd>(TK;)T<b>1</b>V;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append("pack/age/MyMap.eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/MyMap\n" +
					"get\n" +
					" (TK;)TV;\n" +
					" (TK;)T1V;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/*
	 * Assert two proposals ("@NonNull" and "@Nullable") on a simple return type (type variable).
	 * Apply the second proposal and check the effect.
	 */
	public void testAnnotateReturn2() throws Exception {
		
		String MY_MAP_PATH= "pack/age/MyMap";
		String[] pathAndContents= new String[] {
					MY_MAP_PATH+".java",
					"package pack.age;\n" +
					"public interface MyMap<K,V> {\n" +
					"    public V get(K key);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5, null);
		IType type= fJProject1.findType(MY_MAP_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("V get");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			
			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);
			
			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull V'", list);
			String expectedInfo=
					"<dl><dt>get</dt>" +
					"<dd>(TK;)TV;</dd>" +
					"<dd>(TK;)T<b>1</b>V;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable V'", list);
			expectedInfo=
					"<dl><dt>get</dt>" +
					"<dd>(TK;)TV;</dd>" +
					"<dd>(TK;)T<b>0</b>V;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(MY_MAP_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/MyMap\n" +
					"get\n" +
					" (TK;)TV;\n" +
					" (TK;)T0V;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/*
	 * Assert two proposals ("@NonNull" and "Remove") if annotation file already says "@Nullable".
	 * Apply the second proposal and check the effect.
	 */
	public void testAnnotateRemove() throws Exception {
		
		String MY_MAP_PATH= "pack/age/MyMap";

		String[] pathAndContents= new String[] {
				MY_MAP_PATH+".java",
				"package pack.age;\n" +
				"public interface MyMap<K,V> {\n" +
				"    public V get(K key);\n" +
				"}\n"
			};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5, null);
		IType type= fJProject1.findType(MY_MAP_PATH.replace('/', '.'));
		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(MY_MAP_PATH+".eea"));
		String initialContent=
				"class pack/age/MyMap\n" +
				"get\n" +
				" (TK;)TV;\n" +
				" (TK;)T0V;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);
		
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("V get");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			
			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);
			
			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull V'", list);
			String expectedInfo=
					"<dl><dt>get</dt>" +
					"<dd>(TK;)TV;</dd>" +
					"<dd>(TK;)T<b>1</b>V;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Remove nullness annotation from type 'V'", list);
			expectedInfo=
					"<dl><dt>get</dt>" +
					"<dd>(TK;)TV;</dd>" +
					"<dd>(TK;)T<del>0</del>V;</dd>" + // <= <strike>0</strike>
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			assertTrue("Annotation file should still exist", annotationFile.exists());

			String expectedContent=
					"class pack/age/MyMap\n" +
					"get\n" +
					" (TK;)TV;\n" +
					" (TK;)TV;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}
	

	/*
	 * Assert two proposals ("@NonNull" and "@Nullable") on an (outer) array type (in parameter position).
	 * The method already has a 2-line entry (i.e., not yet annotated).
	 * Apply the second proposal and check the effect.
	 */
	public void testAnnotateParameter_Array1() throws Exception {
		
		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public String test(int[][] ints, List<String> list);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);
		
		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"class pack/age/X\n" +
				"test\n" +
				" ([[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("[][] ints");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			
			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);
			
			ICompletionProposal proposal= findProposalByName("Annotate as 'int @NonNull [][]'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([<b>1</b>[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as 'int @Nullable [][]'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([<b>0</b>[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					"test\n" +
					" ([[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n" +
					" ([0[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/*
	 * Assert two proposals ("@NonNull" and "@Nullable") on the array representing the varargs ellipsis
	 * Apply the second proposal and check the effect.
	 * 
	 * Cf. {@link AnnotateAssistTest15#testAnnotateParameter_Array1()}
	 */
	public void testAnnotateParameter_Varargs1() throws Exception {
		
		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public String test(List<String> list, int ... ints);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5, null);
		
		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"class pack/age/X\n" +
				"test\n" +
				" (Ljava/util/List<Ljava/lang/String;>;[I)Ljava/lang/String;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("...");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			
			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);
			
			ICompletionProposal proposal= findProposalByName("Annotate as 'int @NonNull ...'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>(Ljava/util/List&lt;Ljava/lang/String;&gt;;[I)Ljava/lang/String;</dd>" +
					"<dd>(Ljava/util/List&lt;Ljava/lang/String;&gt;;[<b>1</b>I)Ljava/lang/String;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as 'int @Nullable ...'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>(Ljava/util/List&lt;Ljava/lang/String;&gt;;[I)Ljava/lang/String;</dd>" +
					"<dd>(Ljava/util/List&lt;Ljava/lang/String;&gt;;[<b>0</b>I)Ljava/lang/String;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					"test\n" +
					" (Ljava/util/List<Ljava/lang/String;>;[I)Ljava/lang/String;\n" +
					" (Ljava/util/List<Ljava/lang/String;>;[0I)Ljava/lang/String;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/*
	 * Assert two proposals ("@NonNull" and "@Nullable") on a parameter of a constructor.
	 * Apply the second proposal and check the effect.
	 */
	public void testAnnotateConstructorParameter() throws Exception {
		
		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public class X {\n" +
					"    public X(String p) {}\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5, null);
		
		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"class pack/age/X\n" +
				"<init>\n" +
				" (Ljava/lang/String;)V\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("String");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			
			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);
			
			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull String'", list);
			String expectedInfo=
					"<dl><dt>&lt;init&gt;</dt>" +
					"<dd>(Ljava/lang/String;)V</dd>" +
					"<dd>(L<b>1</b>java/lang/String;)V</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable String'", list);
			expectedInfo=
					"<dl><dt>&lt;init&gt;</dt>" +
					"<dd>(Ljava/lang/String;)V</dd>" +
					"<dd>(L<b>0</b>java/lang/String;)V</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					"<init>\n" +
					" (Ljava/lang/String;)V\n" +
					" (L0java/lang/String;)V\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/*
	 * Assert two proposals ("@NonNull" and "@Nullable") on a simple field type (type variable).
	 * Apply the second proposal and check the effect.
	 */
	public void testAnnotateField1() throws Exception {
		
		String NODE_PATH= "pack/age/Node";
		String[] pathAndContents= new String[] {
					NODE_PATH+".java",
					"package pack.age;\n" +
					"public class Node<V> {\n" +
					"    V value;\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5, null);
		IType type= fJProject1.findType(NODE_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("V value");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			
			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);
			
			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull V'", list);
			String expectedInfo=
					"<dl><dt>value</dt>" +
					"<dd>TV;</dd>" +
					"<dd>T<b>1</b>V;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable V'", list);
			expectedInfo=
					"<dl><dt>value</dt>" +
					"<dd>TV;</dd>" +
					"<dd>T<b>0</b>V;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(NODE_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/Node\n" +
					"value\n" +
					" TV;\n" +
					" T0V;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/*
	 * Assert two proposals ("@NonNull" and "@Nullable") on a parameterized field type.
	 * Apply the second proposal and check the effect.
	 */
	public void testAnnotateField2() throws Exception {
		
		String NODE_PATH= "pack/age/Node";
		String[] pathAndContents= new String[] {
					NODE_PATH+".java",
					"package pack.age;\n" +
					"public class Node<V> {\n" +
					"    Node<String> next;\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5, null);
		IType type= fJProject1.findType(NODE_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("Node<String> next");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			
			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);
			
			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull Node'", list);
			String expectedInfo=
					"<dl><dt>next</dt>" +
					"<dd>Lpack/age/Node&lt;Ljava/lang/String;&gt;;</dd>" +
					"<dd>L<b>1</b>pack/age/Node&lt;Ljava/lang/String;&gt;;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable Node'", list);
			expectedInfo=
					"<dl><dt>next</dt>" +
					"<dd>Lpack/age/Node&lt;Ljava/lang/String;&gt;;</dd>" +
					"<dd>L<b>0</b>pack/age/Node&lt;Ljava/lang/String;&gt;;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(NODE_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/Node\n" +
					"next\n" +
					" Lpack/age/Node<Ljava/lang/String;>;\n" +
					" L0pack/age/Node<Ljava/lang/String;>;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	public void testBug466232() throws Exception {
		final String MISSINGPATH= "pack/age/Missing";
		String CLASS2_PATH= "pack/age/Class2";
		String[] pathAndContents= new String[] {
					MISSINGPATH+".java",
					"package pack.age;\n" +
					"public class Missing {\n" +
					"    Missing foo() { return this; }\n" +
					"}\n",
					CLASS2_PATH+".java",
					"package pack.age;\n" +
					"import pack.age.Missing;\n" +
					"public class Class2 {\n" +
					"    void test(Missing c1) {\n" + // will get a RecoveredTypeBinding for "Missing"
					"        c1 = c1.foo();\n" +
					"    }\n" +
					"}\n"
				};
		
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5, new ClassFileFilter() {
			@Override
			public boolean include(CompilationResult unitResult) {
				return !new Path(MISSINGPATH + ".java").equals(new Path(String.valueOf(unitResult.getFileName())));
			}
		});
		IType type= fJProject1.findType(CLASS2_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		ILogListener logListener= null;
		ILog log= JavaPlugin.getDefault().getLog();
		try {
			int offset= pathAndContents[3].indexOf("Missing c1");

			// not expecting proposals, but a log message, due to incomplete AST (no binding information available).
			final IStatus[] resultingStatus= new IStatus[1];
			logListener= new ILogListener() {
				@Override
				public void logging(IStatus status, String plugin) {
					assertTrue("Only one status", resultingStatus[0] == null);
					assertEquals("Expected status message",
							"Error during computation of Annotate proposals: " +
							"Could not resolve type Missing",
							status.getMessage());
					resultingStatus[0] = status;
				}
			};
			log.log(new Status(IStatus.INFO, JavaUI.ID_PLUGIN, "Expecting an error message to be logged after this."));
			log.addLogListener(logListener);
			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			
			assertEquals("Expected number of proposals", 0, list.size());
			
			assertNotNull("Expected status", resultingStatus[0]);
		} finally {
			if (logListener != null) {
				log.removeLogListener(logListener);
			}
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}
}
