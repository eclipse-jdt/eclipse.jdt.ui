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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.core.ClasspathAttribute;
import org.eclipse.jdt.internal.core.ClasspathEntry;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.examples.MyClasspathContainerInitializer;
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


	/**
	 * Initializer for a container that may provide customized content specified with source and external annotations.
	 * (Similar to org.eclipse.jdt.core.tests.model.ExternalAnnotations18Test.TestCustomContainerInitializer)
	 */
	static class TestCustomContainerInitializer extends ClasspathContainerInitializer {

		List<String> allEntries;
		Map<String,String> sourceEntries;
		Map<String,String> elementAnnotationPaths;

		/**
		 * @param elementsWithSourcesAndAnnotationPaths each triplet of entries in this array defines one classpath entry:
		 * <ul>
		 * 	<li>1st string specifies the entry path,
		 *  <li>2nd string specifies source attachment
		 *  <li>if 3nd string is "self" than the entry is "self-annotating".
		 *  	 {@code null} is a legal value to signal "not self-annotating"
		 *  </ul>
		 */
		public TestCustomContainerInitializer(String... elementsWithSourcesAndAnnotationPaths) {
			this.allEntries = new ArrayList<>();
			this.sourceEntries = new HashMap<>();
			this.elementAnnotationPaths = new HashMap<>();
			for (int i = 0; i < elementsWithSourcesAndAnnotationPaths.length; i+=3) {
				String entryPath = elementsWithSourcesAndAnnotationPaths[i];
				this.allEntries.add(entryPath);
				this.sourceEntries.put(entryPath, elementsWithSourcesAndAnnotationPaths[i+1]);
				String annotsPath = elementsWithSourcesAndAnnotationPaths[i+2];
				if ("self".equals(annotsPath))
					this.elementAnnotationPaths.put(entryPath, entryPath);
				else if (annotsPath != null)
					this.elementAnnotationPaths.put(entryPath, annotsPath);
			}
		}

		static class TestContainer implements IClasspathContainer {
			IPath path;
			IClasspathEntry[] entries;
			TestContainer(IPath path, IClasspathEntry[] entries){
				this.path = path;
				this.entries = entries;
			}
			@Override public IPath getPath() { return this.path; }
			@Override public IClasspathEntry[] getClasspathEntries() { return this.entries;	}
			@Override public String getDescription() { return this.path.toString(); 	}
			@Override public int getKind() { return 0; }
		}

		@Override
		public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
			List<IClasspathEntry> entries = new ArrayList<>();
			for (String entryPath : this.allEntries) {
				String elementAnnotationPath = this.elementAnnotationPaths.get(entryPath);

				IClasspathAttribute[] extraAttributes;
				if (elementAnnotationPath != null)
					extraAttributes = externalAnnotationExtraAttributes(elementAnnotationPath);
				else
					extraAttributes = ClasspathEntry.NO_EXTRA_ATTRIBUTES;

				String sourcePath= this.sourceEntries.get(entryPath);
				IPath sourceAttachment = sourcePath != null ? new Path(sourcePath) : null;
				entries.add(JavaCore.newLibraryEntry(new Path(entryPath), sourceAttachment, null,
						ClasspathEntry.NO_ACCESS_RULES, extraAttributes, false/*not exported*/));
			}
			JavaCore.setClasspathContainer(
					containerPath,
					new IJavaProject[]{ project },
					new IClasspathContainer[] { new TestContainer(containerPath, entries.toArray(IClasspathEntry[]::new)) },
					null);
		}
	}

	static IClasspathAttribute[] externalAnnotationExtraAttributes(String path) {
		return new IClasspathAttribute[] {
				new ClasspathAttribute(IClasspathAttribute.EXTERNAL_ANNOTATION_PATH, path)
		};
	}

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
					"""
						package pack.age;
						import java.util.List;
						public interface X {
						    public String test(int[] ints, List<String> list);
						}
						"""
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"""
			class pack/age/X
			test
			 ([ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
			 ([IL1java/util/List<Ljava/lang/String;>;)Ljava/lang/String;
			""";
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
					"""
				class pack/age/X
				test
				 ([ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
				 ([IL1java/util/List<L0java/lang/String;>;)Ljava/lang/String;
				""";
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
					"""
						package pack.age;
						import java.util.List;
						public interface X {
						    public String test(int[] ints, List<String> list);
						}
						"""
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
					"""
						package pack.age;
						import java.util.List;
						public interface X {
						    public String test(Object[] objects, List<? extends Number> list);
						}
						"""
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"""
			class pack/age/X
			test
			 ([Ljava/lang/Object;Ljava/util/List<+Ljava/lang/Number;>;)Ljava/lang/String;
			 ([Ljava/lang/Object;L1java/util/List<+1Ljava/lang/Number;>;)Ljava/lang/String;
			""";
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
					"""
				class pack/age/X
				test
				 ([Ljava/lang/Object;Ljava/util/List<+Ljava/lang/Number;>;)Ljava/lang/String;
				 ([Ljava/lang/Object;L1java/util/List<+1L0java/lang/Number;>;)Ljava/lang/String;
				""";
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
					"""
						package pack.age;
						import java.util.List;
						public interface X {
						    public String test(int[][] ints, List<String> list);
						}
						"""
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
					"""
				class pack/age/X
				test
				 ([[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
				 ([[0ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
				""";
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
					"""
						package pack.age;
						import java.util.List;
						public interface X {
						    public String test(int[][] ints, List<String> list);
						}
						"""
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"""
			class pack/age/X
			test
			 ([[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
			 ([1[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
			""";
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
					"""
				class pack/age/X
				test
				 ([[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
				 ([1[0ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
				""";
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
					"""
						package pack.age;
						import java.util.List;
						public interface X {
						    public String test(int ints[][], List<String> list);
						}
						"""
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"""
			class pack/age/X
			test
			 ([[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
			 ([1[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
			""";
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
					"""
				class pack/age/X
				test
				 ([[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
				 ([1[0ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
				""";
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
					"""
						package pack.age;
						import java.util.List;
						public interface X {
						    public String test(int ints[][][], List<String> list);
						}
						"""
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"""
			class pack/age/X
			test
			 ([[[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
			 ([[1[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
			""";
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
					"""
				class pack/age/X
				test
				 ([[[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
				 ([0[1[ILjava/util/List<Ljava/lang/String;>;)Ljava/lang/String;
				""";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on an array type in normal syntax.
	 * A two line entry using this selector and signature exists and will be amended.
	 * Apply the second proposal and check the effect.
	 *
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateMethod_Array1() throws Exception {

		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"""
						package pack.age;
						import java.util.List;
						public interface X {
						    public String[] test();
						}
						"""
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"""
			class pack/age/X
			test
			 ()[Ljava/lang/String;
			""";
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
					"""
				class pack/age/X
				test
				 ()[Ljava/lang/String;
				 ()[0Ljava/lang/String;
				""";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	/**
	 * Assert two proposals ("@NonNull" and "@Nullable") on an array type in extra-dims syntax.
	 * A two line entry using this selector and signature exists and will be amended.
	 * Apply the second proposal and check the effect.
	 *
	 * @throws Exception multiple causes
	 */
	@Test
	public void testAnnotateMethod_Array2() throws Exception {

		String X_PATH= "pack/age/X";
		String[] pathAndContents= new String[] {
					X_PATH+".java",
					"""
						package pack.age;
						import java.util.List;
						public interface X {
						    public String test()[];
						}
						"""
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"""
			class pack/age/X
			test
			 ()[Ljava/lang/String;
			""";
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
					"""
				class pack/age/X
				test
				 ()[Ljava/lang/String;
				 ()[0Ljava/lang/String;
				""";
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
					"""
						package pack.age;
						import java.util.List;
						public interface X {
						    public String test(List<String> list, int ... ints);
						}
						"""
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"""
			class pack/age/X
			test
			 (Ljava/util/List<Ljava/lang/String;>;[I)Ljava/lang/String;
			""";
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
					"""
				class pack/age/X
				test
				 (Ljava/util/List<Ljava/lang/String;>;[I)Ljava/lang/String;
				 (Ljava/util/List<Ljava/lang/String;>;[0I)Ljava/lang/String;
				""";
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
					"""
						package pack.age;
						import java.util.List;
						public interface X {
						    public String test(List<String> list, int[] ... ints);
						}
						"""
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"""
			class pack/age/X
			test
			 (Ljava/util/List<Ljava/lang/String;>;[[I)Ljava/lang/String;
			 (Ljava/util/List<Ljava/lang/String;>;[[1I)Ljava/lang/String;
			""";
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
					"""
				class pack/age/X
				test
				 (Ljava/util/List<Ljava/lang/String;>;[[I)Ljava/lang/String;
				 (Ljava/util/List<Ljava/lang/String;>;[0[1I)Ljava/lang/String;
				""";
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
					"""
						package pack.age;
						import java.util.List;
						public interface X {
						    public String test(List<String> list, String ... strings);
						}
						"""
				};
		addLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, ANNOTATION_PATH, JavaCore.VERSION_1_8, null);

		IFile annotationFile= fJProject1.getProject().getFile(new Path(ANNOTATION_PATH).append(X_PATH+".eea"));
		String initialContent=
				"""
			class pack/age/X
			test
			 (Ljava/util/List<Ljava/lang/String;>;[Ljava/lang/String;)Ljava/lang/String;
			""";
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
					"""
				class pack/age/X
				test
				 (Ljava/util/List<Ljava/lang/String;>;[Ljava/lang/String;)Ljava/lang/String;
				 (Ljava/util/List<Ljava/lang/String;>;[L0java/lang/String;)Ljava/lang/String;
				""";
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
					"""
						package pack.age;
						import java.util.List;
						public interface X {
						    public <X, T extends List<X>> X test(T list);
						}
						"""
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
					"""
				class pack/age/X
				test
				 <X:Ljava/lang/Object;T::Ljava/util/List<TX;>;>(TT;)TX;
				 <X:Ljava/lang/Object;0T::Ljava/util/List<TX;>;>(TT;)TX;
				""";
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
					"""
						class pack/age/X
						test
						 <X:Ljava/lang/Object;T::Ljava/util/List<TX;>;>(TT;)TX;
						 <0X:Ljava/lang/Object;0T::Ljava/util/List<TX;>;>(TT;)TX;
						""";
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
					"""
						package pack.age;
						import java.util.List;
						public interface X <X, T extends List<X>> {
						    public X test(T list);
						}
						"""
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
					"""
				class pack/age/X
				 <X:Ljava/lang/Object;T::Ljava/util/List<TX;>;>
				 <X:Ljava/lang/Object;0T::Ljava/util/List<TX;>;>
				""";
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
					"""
						class pack/age/X
						 <X:Ljava/lang/Object;T::Ljava/util/List<TX;>;>
						 <0X:Ljava/lang/Object;0T::Ljava/util/List<TX;>;>
						""";
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
					"""
						package pack.age;
						import java.util.List;
						public class Node {
						    List<Object[]> value;
						}
						"""
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
					"""
				class pack/age/Node
				value
				 Ljava/util/List<[Ljava/lang/Object;>;
				 Ljava/util/List<[0Ljava/lang/Object;>;
				""";
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
					"""
						package pack.age;
						public interface MyMap<K,V> {
						    public V get(K key);
						}
						"""
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
					"""
				class pack/age/MyMap
				get
				 (TK;)TV;
				 (TK;)T0V;
				""";
			checkContentOfFile("annotation file content", annotationFile, expectedContent);
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
		}
	}

	@Test
	public void testAnnotationsInProjectReferencedViaContainer() throws Exception {
		ClasspathContainerInitializer prev = MyClasspathContainerInitializer.initializerDelegate;

		// container providing an unannotated library and a dedicated annotation project in the workspace:
		String eeaProjectName = "my.eeas";
		MyClasspathContainerInitializer.setInitializer(new TestCustomContainerInitializer(
				"/TestSetupProject1d8/lib.jar", "/TestSetupProject1d8/lib.zip", null,
				'/'+eeaProjectName, null, null));

		IJavaProject eeaProject = null;
		try {
			// create the annotation project:
			eeaProject = JavaProjectHelper.createJavaProject(eeaProjectName, "");

			// create the library
			String MY_MAP_PATH= "pack/age/MyMap";
			String[] pathAndContents= new String[] {
						MY_MAP_PATH+".java",
						"""
							package pack.age;
							public interface MyMap<K,V> {
							    public V get(K key);
							}
							"""
					};
			createLibrary(fJProject1, "lib.jar", "lib.zip", pathAndContents, JavaCore.VERSION_1_8, null);

			// set the classpath:
			IClasspathEntry[] rawClasspath= fJProject1.getRawClasspath();
			fJProject1.setRawClasspath(new IClasspathEntry[] {
					rawClasspath[0], // assumed to be rtstubs.jar
					JavaCore.newSourceEntry(fJProject1.getPath().append("src")),
					JavaCore.newContainerEntry(
							new Path(MyClasspathContainerInitializer.CONTAINER_NAME), null/*access rules*/,
							externalAnnotationExtraAttributes(MyClasspathContainerInitializer.CONTAINER_NAME+"/my.eeas"),
							false/*exported*/)
				},
				null);

			//  START of actual assist test:
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

				IFile annotationFile= eeaProject.getProject().getFile(new Path(MY_MAP_PATH + ".eea"));
				assertTrue("Annotation file should have been created", annotationFile.exists());

				String expectedContent=
						"""
					class pack/age/MyMap
					get
					 (TK;)TV;
					 (TK;)T0V;
					""";
				checkContentOfFile("annotation file content", annotationFile, expectedContent);
			} finally {
				JavaPlugin.getActivePage().closeAllEditors(false);
			}
		} finally {
			MyClasspathContainerInitializer.setInitializer(prev);
			if (eeaProject != null && eeaProject.exists())
				eeaProject.getProject().delete(true, null);
		}
	}

}
