/*******************************************************************************
 * Copyright (c) 2015 GK Software AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephan Herrmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.io.ByteArrayInputStream;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class AnnotateAssistTest18 extends AbstractAnnotateAssistTests {

	protected static final String ANNOTATION_PATH= "annots";

	protected static final Class<?> THIS= AnnotateAssistTest18.class;
	
	public static Test suite() {
		return setUpTest(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}
	public AnnotateAssistTest18(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		fJProject1= Java18ProjectTestSetup.getProject();
		fJProject1.getProject().getFolder(ANNOTATION_PATH).create(true, true, null);
		fJProject1.setOption(JavaCore.COMPILER_ANNOTATION_NULL_ANALYSIS, JavaCore.ENABLED);
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on a type argument of a parameter.
	 * The parameterized type already has a @NonNull annotation.
	 * Apply the second proposal and check the effect.
	 * @throws Exception
	 */
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
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8);
		
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
	 * @throws Exception
	 */
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
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8);
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
	 * @throws Exception
	 */
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
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8);
		
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
	 * Cf. {@link AnnotateAssistTest15#testAnnotateParameter_Array1()}
	 * @throws Exception
	 */
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
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8);
		
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
	 * Cf. {@link AnnotateAssistTest15#testAnnotateParameter_Array1()}
	 * @throws Exception
	 */
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
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8);
		
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
	 * Assert two proposals ("@NonNull" and "@Nullable") on a method's type parameter
	 * The parameterized type and the wildcard already has a @NonNull annotation.
	 * Annotation entry already exists, with @NonNull on the wildcard itself.
	 * Apply the second proposal and check the effect.
	 * @throws Exception
	 */
	// FIXME(stephan): enable once implemented
	public void _testAnnotateParameter_TypeParameter() throws Exception {
		
		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] { 
					X_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public interface X {\n" +
					"    public <X, T extends List<X>> X test(T list);\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8);

		IType type= fJProject1.findType(X_PATH.replace('/', '.'));
		JavaEditor javaEditor= (JavaEditor) JavaUI.openInEditor(type);

		try {
			int offset= pathAndContents[1].indexOf("T extends");

			List<ICompletionProposal> list= collectAnnotateProposals(javaEditor, offset);
			
			assertCorrectLabels(list);
			assertNumberOfProposals(list, 2);
			
			ICompletionProposal proposal= findProposalByName("Annotate as '@NonNull T'", list);
			String expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>&lt;X:Ljava/lang/Object;T:Ljava/util/List&lt;TX;&gt;&gt;(TT;)TX;</dd>" +
					"<dd>&lt;X:Ljava/lang/Object;1T:Ljava/util/List&lt;TX;&gt;&gt;(TT;)TX;</dd>" + // <= 1
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			proposal= findProposalByName("Annotate as '@Nullable Number'", list);
			expectedInfo=
					"<dl><dt>test</dt>" +
					"<dd>&lt;X:Ljava/lang/Object;T:Ljava/util/List&lt;TX;&gt;&gt;(TT;)TX;</dd>" +
					"<dd>&lt;X:Ljava/lang/Object;0T:Ljava/util/List&lt;TX;&gt;&gt;(TT;)TX;</dd>" + // <= 0
					"</dl>";
			assertEquals("expect detail", expectedInfo, proposal.getAdditionalProposalInfo());

			IDocument document= javaEditor.getDocumentProvider().getDocument(javaEditor.getEditorInput());
			proposal.apply(document);
			
			IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
			assertTrue("Annotation file should have been created", annotationFile.exists());

			String expectedContent=
					"class pack/age/X\n" +
					"test\n" +
					" <X:Ljava/lang/Object;T:Ljava/util/List<;TX;>>(TT;)TX;\n" +
					" <X:Ljava/lang/Object;0T:Ljava/util/List<;TX;>>(TT;)TX;\n";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on a complex field type (list of array)
	 * Apply the second proposal and check the effect.
	 * @throws Exception
	 */
	// FIXME(stephan): enable once implemented
	public void _testAnnotateField2() throws Exception {
		
		String NODE_PATH= "pack/age/Node";
		String[] pathAndContents= new String[] { 
					NODE_PATH+".java",
					"package pack.age;\n" +
					"import java.util.List;\n" +
					"public class Node {\n" +
					"    List<Object[]> value;\n" +
					"}\n"
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_5);
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
}
