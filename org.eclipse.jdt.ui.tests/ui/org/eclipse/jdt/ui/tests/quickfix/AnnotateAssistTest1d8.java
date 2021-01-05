/*******************************************************************************
 * Copyright (c) 2015, 2020 GK Software AG and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Those tests are made to run on Java Spider 1.8 .
 */
public class AnnotateAssistTest1d8 extends AbstractAnnotateAssistTests {

	@Rule
    public ProjectTestSetup projectSetup = new Java1d8ProjectTestSetup();

	protected static final String ANNOTATION_PATH= "annots";

	@Before
	public void setUp() throws Exception {
		fJProject1= projectSetup.getProject();
		fJProject1.getProject().getFolder(ANNOTATION_PATH).create(true, true, null);
		fJProject1.setOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on a type argument of a parameter.
	 * The parameterized type already has a @NonNull annotation.
	 * Apply the second proposal and check the effect.
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateParameter_TypeArgument() throws Exception {

		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public String test(int[] ints, List<String> list);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"class pack/age/X\n" +
				"test\n" +
				" ([ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n" +
				" ([IL1java/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("String> list");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull String'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([IL1java/util/List&lt;L<b>1</b>java/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable String'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([IL1java/util/List&lt;L<b>0</b>java/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);

			annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					"test\n" +
					" ([ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n" +
					" ([IL1java/util/List<L0java/lang/String;>;)Ljava/lang/String;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert NO proposals on the primitive leaf type of an array type.
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateParameter_ArrayOfPrimitive() throws Exception {

		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public String test(int[] ints, List<String> list);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);
		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("int[]");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertNumberOfProposals(list, 0);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on a wildcard bound inside a parameter type
	 * The parameterized type and the wildcard already has a @NonNull annotation.
	 * Annotation entry already exists, with @NonNull on the wildcard itself.
	 * Apply the second proposal and check the effect.
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateParameter_WildcardBound() throws Exception {

		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public String test(Object[] objects, List<? extends Number> list);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"class pack/age/X\n" +
				"test\n" +
				" ([Ljava/lang/Object;Ljava/util/List<+Ljava/lang/Number;>;)Ljava/lang/String;\n" +
				" ([Ljava/lang/Object;L1java/util/List<+1Ljava/lang/Number;>;)Ljava/lang/String;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("Number> list");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull Number'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([Ljava/lang/Object;Ljava/util/List&lt;+Ljava/lang/Number;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([Ljava/lang/Object;L1java/util/List&lt;+1L<b>1</b>java/lang/Number;&gt;;)Ljava/lang/String;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable Number'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([Ljava/lang/Object;Ljava/util/List&lt;+Ljava/lang/Number;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([Ljava/lang/Object;L1java/util/List&lt;+1L<b>0</b>java/lang/Number;&gt;;)Ljava/lang/String;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);

			annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					"test\n" +
					" ([Ljava/lang/Object;Ljava/util/List<+Ljava/lang/Number;>;)Ljava/lang/String;\n" +
					" ([Ljava/lang/Object;L1java/util/List<+1L0java/lang/Number;>;)Ljava/lang/String;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on an inner array type (in parameter position).
	 * A single line entry using this selector exists and will be amended.
	 * Apply the second proposal and check the effect.
	 *
	 * Cf. {@link AnnotateAssistTest1d5#testAnnotateParameter_Array1()}
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateParameter_Array2() throws Exception {

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
				"test\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("[] ints");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			ICompletionProposal proposal= findProposalByName("Annotate as 'int[] @NonNull []'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([[<b>1</b>ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as 'int[] @Nullable []'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([[<b>0</b>ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 0
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
					" ([[0ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on an inner array type (in parameter position).
	 * An entry with annotation on the outer array already exists and will be amended.
	 * Apply the second proposal and check the effect.
	 *
	 * Cf. {@link AnnotateAssistTest1d5#testAnnotateParameter_Array1()}
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateParameter_Array3() throws Exception {

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
				" ([[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n" +
				" ([1[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("[] ints");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			ICompletionProposal proposal= findProposalByName("Annotate as 'int[] @NonNull []'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([1[<b>1</b>ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as 'int[] @Nullable []'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([1[<b>0</b>ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 0
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
					" ([1[0ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on an inner array type - extra dimensions (in parameter position).
	 * An entry with annotation on the outer array already exists and will be amended.
	 * Apply the second proposal and check the effect.
	 *
	 * Cf. {@link AnnotateAssistTest1d8#testAnnotateParameter_Array3()}
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateParameter_Array4() throws Exception {

		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public String test(int ints[][], List<String> list);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"class pack/age/X\n" +
				"test\n" +
				" ([[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n" +
				" ([1[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("[],");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			ICompletionProposal proposal= findProposalByName("Annotate as 'int[] @NonNull []'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([1[<b>1</b>ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as 'int[] @Nullable []'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([1[<b>0</b>ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 0
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
					" ([1[0ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on an outer array type - extra dimensions (in parameter position).
	 * An entry with annotation on the inner array already exists and will be amended.
	 * Apply the second proposal and check the effect.
	 *
	 * Cf. {@link AnnotateAssistTest1d8#testAnnotateParameter_Array3()}
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateParameter_Array5() throws Exception {

		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public String test(int ints[][][], List<String> list);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"class pack/age/X\n" +
				"test\n" +
				" ([[[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n" +
				" ([[1[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("[][][],");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			ICompletionProposal proposal= findProposalByName("Annotate as 'int @NonNull [][][]'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([[[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([<b>1</b>[1[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as 'int @Nullable [][][]'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>([[[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" +
					"<dd>([<b>0</b>[1[ILjava/util/List&lt;Ljava/lang/String;&gt;;)Ljava/lang/String;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);

			annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					"test\n" +
					" ([[[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n" +
					" ([0[1[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on an array type in normal syntax.
	 * A two line entry using this selector & signature exists and will be amended.
	 * Apply the second proposal and check the effect.
	 *
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateMethod_Array1() throws Exception {

		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public String[] test();\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"class pack/age/X\n" +
				"test\n" +
				" ()[Ljava/lang/String;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("[] test");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			ICompletionProposal proposal= findProposalByName("Annotate as 'String @NonNull []'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>()[Ljava/lang/String;</dd>" +
					"<dd>()[<b>1</b>Ljava/lang/String;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as 'String @Nullable []'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>()[Ljava/lang/String;</dd>" +
					"<dd>()[<b>0</b>Ljava/lang/String;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);

			annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					"test\n" +
					" ()[Ljava/lang/String;\n" +
					" ()[0Ljava/lang/String;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on an array type in extra-dims syntax.
	 * A two line entry using this selector & signature exists and will be amended.
	 * Apply the second proposal and check the effect.
	 *
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateMethod_Array2() throws Exception {

		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public String test()[];\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"class pack/age/X\n" +
				"test\n" +
				" ()[Ljava/lang/String;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("[];");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			ICompletionProposal proposal= findProposalByName("Annotate as 'String @NonNull []'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>()[Ljava/lang/String;</dd>" +
					"<dd>()[<b>1</b>Ljava/lang/String;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as 'String @Nullable []'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>()[Ljava/lang/String;</dd>" +
					"<dd>()[<b>0</b>Ljava/lang/String;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);

			annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					"test\n" +
					" ()[Ljava/lang/String;\n" +
					" ()[0Ljava/lang/String;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on the array representing the varargs ellipsis
	 * Apply the second proposal and check the effect.
	 *
	 * Cf. {@link AnnotateAssistTest1d8#testAnnotateParameter_Array3()}
	 * @throws Exception multiple causes
	 */
	@Test
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
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

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

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on the array representing the varargs ellipsis
	 * An entry with annotation on the inner array already exists and will be amended.
	 * Apply the second proposal and check the effect.
	 *
	 * Cf. {@link AnnotateAssistTest1d8#testAnnotateParameter_Varargs1()}
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateParameter_Varargs2() throws Exception {

		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public String test(List<String> list, int[] ... ints);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"class pack/age/X\n" +
				"test\n" +
				" (Ljava/util/List<Ljava/lang/String;>;[[I)Ljava/lang/String;\n" +
				" (Ljava/util/List<Ljava/lang/String;>;[[1I)Ljava/lang/String;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("...");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			ICompletionProposal proposal= findProposalByName("Annotate as 'int[] @NonNull ...'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>(Ljava/util/List&lt;Ljava/lang/String;&gt;;[[I)Ljava/lang/String;</dd>" +
					"<dd>(Ljava/util/List&lt;Ljava/lang/String;&gt;;[<b>1</b>[1I)Ljava/lang/String;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as 'int[] @Nullable ...'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>(Ljava/util/List&lt;Ljava/lang/String;&gt;;[[I)Ljava/lang/String;</dd>" +
					"<dd>(Ljava/util/List&lt;Ljava/lang/String;&gt;;[<b>0</b>[1I)Ljava/lang/String;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);

			annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					"test\n" +
					" (Ljava/util/List<Ljava/lang/String;>;[[I)Ljava/lang/String;\n" +
					" (Ljava/util/List<Ljava/lang/String;>;[0[1I)Ljava/lang/String;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on the element type of a varargs type
	 * Apply the second proposal and check the effect.
	 *
	 * Cf. {@link AnnotateAssistTest1d8#testAnnotateParameter_Array3()}
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateParameter_Varargs3() throws Exception {

		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public String test(List<String> list, String ... strings);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"class pack/age/X\n" +
				"test\n" +
				" (Ljava/util/List<Ljava/lang/String;>;[Ljava/lang/String;)Ljava/lang/String;\n";
		ensureExists(annotationFile.getParent());
		annotationFile.create(new ByteArrayInputStream(initialContent.getBytes("UTF-8")), 0, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("String ...");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull String'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>(Ljava/util/List&lt;Ljava/lang/String;&gt;;[Ljava/lang/String;)Ljava/lang/String;</dd>" +
					"<dd>(Ljava/util/List&lt;Ljava/lang/String;&gt;;[L<b>1</b>java/lang/String;)Ljava/lang/String;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable String'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>(Ljava/util/List&lt;Ljava/lang/String;&gt;;[Ljava/lang/String;)Ljava/lang/String;</dd>" +
					"<dd>(Ljava/util/List&lt;Ljava/lang/String;&gt;;[L<b>0</b>java/lang/String;)Ljava/lang/String;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);

			annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					"test\n" +
					" (Ljava/util/List<Ljava/lang/String;>;[Ljava/lang/String;)Ljava/lang/String;\n" +
					" (Ljava/util/List<Ljava/lang/String;>;[L0java/lang/String;)Ljava/lang/String;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on a method's type parameter
	 * Apply the second proposal and check the effect.
	 * Then repeat for another type parameter (to check merging of changes)
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateMethod_TypeParameter1() throws Exception {

		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public <X, T extends List<X>> X test(T list);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("T extends");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull T extends List<X>'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>&lt;X:Ljava/lang/Object;T::Ljava/util/List&lt;TX;&gt;;&gt;(TT;)TX;</dd>" +
					"<dd>&lt;X:Ljava/lang/Object;<b>1</b>T::Ljava/util/List&lt;TX;&gt;;&gt;(TT;)TX;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable T extends List<X>'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>&lt;X:Ljava/lang/Object;T::Ljava/util/List&lt;TX;&gt;;&gt;(TT;)TX;</dd>" +
					"<dd>&lt;X:Ljava/lang/Object;<b>0</b>T::Ljava/util/List&lt;TX;&gt;;&gt;(TT;)TX;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);

			IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					"test\n" +
					" <X:Ljava/lang/Object;T::Ljava/util/List<TX;>;>(TT;)TX;\n" +
					" <X:Ljava/lang/Object;0T::Ljava/util/List<TX;>;>(TT;)TX;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);

			// add second annotation:
			offset= pathAndContents[1].indexOf("X,");

			list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			proposal= findProposalByName("Annotate as '@NonNull X'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>&lt;X:Ljava/lang/Object;T::Ljava/util/List&lt;TX;&gt;;&gt;(TT;)TX;</dd>" +
					"<dd>&lt;<b>1</b>X:Ljava/lang/Object;0T::Ljava/util/List&lt;TX;&gt;;&gt;(TT;)TX;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable X'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>&lt;X:Ljava/lang/Object;T::Ljava/util/List&lt;TX;&gt;;&gt;(TT;)TX;</dd>" +
					"<dd>&lt;<b>0</b>X:Ljava/lang/Object;0T::Ljava/util/List&lt;TX;&gt;;&gt;(TT;)TX;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);

			annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			expectedContent=
					"class pack/age/X\n" +
					"test\n" +
					" <X:Ljava/lang/Object;T::Ljava/util/List<TX;>;>(TT;)TX;\n" +
					" <0X:Ljava/lang/Object;0T::Ljava/util/List<TX;>;>(TT;)TX;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on a type's type parameter
	 * Apply the second proposal and check the effect.
	 * Then repeat for another type parameter (to check merging of changes)
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateMethod_TypeParameter2() throws Exception {

		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X <X, T extends List<X>> {\n" +
					"    public X test(T list);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("T extends");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull T extends List<X>'", list);
			String expectedInfo=
					"<dl><dt>class pack/age/X</dt>" +
					"<dd>&lt;X:Ljava/lang/Object;T::Ljava/util/List&lt;TX;&gt;;&gt;</dd>" +
					"<dd>&lt;X:Ljava/lang/Object;<b>1</b>T::Ljava/util/List&lt;TX;&gt;;&gt;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable T extends List<X>'", list);
			expectedInfo=
					"<dl><dt>class pack/age/X</dt>" +
					"<dd>&lt;X:Ljava/lang/Object;T::Ljava/util/List&lt;TX;&gt;;&gt;</dd>" +
					"<dd>&lt;X:Ljava/lang/Object;<b>0</b>T::Ljava/util/List&lt;TX;&gt;;&gt;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);

			IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					" <X:Ljava/lang/Object;T::Ljava/util/List<TX;>;>\n" +
					" <X:Ljava/lang/Object;0T::Ljava/util/List<TX;>;>\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);

			// add second annotation:
			offset= pathAndContents[1].indexOf("X,");

			list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			proposal= findProposalByName("Annotate as '@NonNull X'", list);
			expectedInfo=
					"<dl><dt>class pack/age/X</dt>" +
					"<dd>&lt;X:Ljava/lang/Object;T::Ljava/util/List&lt;TX;&gt;;&gt;</dd>" +
					"<dd>&lt;<b>1</b>X:Ljava/lang/Object;0T::Ljava/util/List&lt;TX;&gt;;&gt;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable X'", list);
			expectedInfo=
					"<dl><dt>class pack/age/X</dt>" +
					"<dd>&lt;X:Ljava/lang/Object;T::Ljava/util/List&lt;TX;&gt;;&gt;</dd>" +
					"<dd>&lt;<b>0</b>X:Ljava/lang/Object;0T::Ljava/util/List&lt;TX;&gt;;&gt;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);

			annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			expectedContent=
					"class pack/age/X\n" +
					" <X:Ljava/lang/Object;T::Ljava/util/List<TX;>;>\n" +
					" <0X:Ljava/lang/Object;0T::Ljava/util/List<TX;>;>\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on a complex field type (list of array)
	 * Apply the second proposal and check the effect.
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateField1() throws Exception {

		String NODE_PATH= "pack/age/Node";
		String[] pathAndContents= new String[] {
					NODE_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public class Node {\n" +
					"    List<Object[]> value;\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5, null);
		IType type= fJProject1.findType(NODE_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("[]> value");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);

			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);

			ICompletionProposal proposal= findProposalByName("Annotate as 'Object @NonNull []'", list);
			String expectedInfo=
					"<dl><dt>value</dt>" +
					"<dd>Ljava/util/List&lt;[Ljava/lang/Object;&gt;;</dd>" +
					"<dd>Ljava/util/List&lt;[<b>1</b>Ljava/lang/Object;&gt;;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as 'Object @Nullable []'", list);
			expectedInfo=
					"<dl><dt>value</dt>" +
					"<dd>Ljava/util/List&lt;[Ljava/lang/Object;&gt;;</dd>" +
					"<dd>Ljava/util/List&lt;[<b>0</b>Ljava/lang/Object;&gt;;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);

			IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(NODE_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/Node\n" +
					"value\n" +
					" Ljava/util/List<[Ljava/lang/Object;>;\n" +
					" Ljava/util/List<[0Ljava/lang/Object;>;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on a simple return type (type variable).
	 * Apply the second proposal and check the effect.
	 *
	 * Similar to AnnotateAssistTest1d5.testAnnotateReturn2() but annotating source not binary.
	 *
	 * @throws Exception Any exception
	 */
	@Test
	public void testAnnotateReturnInSourceFolder() throws Exception {
		String MY_MAP_PATH= "pack/age/MyMap";
		String[] pathAndContents= new String[] {
					MY_MAP_PATH+".java",
					"package pack.age;\n" +
					"public interface MyMap<K,V> {\n" +
					"    public V get(K key);\n" +
					"}\n"
				};
		JarUtil.createSourceDir(pathAndContents, fJProject1.getProject().getLocation()+"/src");
		fJProject1.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);

		IClasspathEntry[] rawClasspath= fJProject1.getRawClasspath();
		fJProject1.setRawClasspath(new IClasspathEntry[] {
				rawClasspath[0],
				JavaCore.newSourceEntry(fJProject1.getPath().append("src"))
			},
			null);
		IType type= fJProject1.findType(MY_MAP_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("V get");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			assertNumberOfProposals(list, 0); // no annotation path defined

			fJProject1.setRawClasspath(new IClasspathEntry[] {
					rawClasspath[0],
					JavaCore.newSourceEntry(fJProject1.getPath().append("src"), null, null, null,
							new IClasspathAttribute[] {
									JavaCore.newClasspathAttribute(IClasspathAttribute.EXTERNAL_ANNOTATION_PATH, ANNOTATION_PATH)
					})
				},
				null);

			list= collectAnnotateProposals(javaEditor, offset);
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

			IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(MY_MAP_PATH + ".eea"));
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

}
