/*******************************************************************************
 * Copyright (c) 2007, 2025 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.IAdvancedUndoableOperation;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;

import org.eclipse.jface.internal.text.reconciler.ReconcilerJobFamilies;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.CopyOnWriteTextStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.editors.quickdiff.LastSaveReferenceProvider;
import org.eclipse.ui.internal.texteditor.quickdiff.DocumentLineDiffer;
import org.eclipse.ui.part.FileEditorInput;

import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPostSaveListener;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.tests.util.TestUtils;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

@RunWith(Parameterized.class)
public class SaveParticipantTest extends CleanUpTestCase {
	@Parameters(name= "{0}")
	public static Object[][] retry() {
		return new Object[2][0];
	}

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	@BeforeClass
	public static void enableDebugTraces() {
		setDebugTracesEnabled(true);
	}

	@AfterClass
	public static void disableDebugTraces() {
		setDebugTracesEnabled(false);
	}

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		IEclipsePreferences node= InstanceScope.INSTANCE.getNode(JavaUI.ID_PLUGIN);
		node.putBoolean("editor_save_participant_" + CleanUpPostSaveListener.POSTSAVELISTENER_ID, true);
		node.put(CleanUpPreferenceUtil.SAVE_PARTICIPANT_KEY_PREFIX + CleanUpConstants.CLEANUP_ON_SAVE_ADDITIONAL_OPTIONS, CleanUpOptions.TRUE);
		TestUtils.waitForIndexer();
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		TestUtils.waitForEditorJobs(60_000L, true);
	}

	@SuppressWarnings("restriction")
	private static void editCUInEditor(ICompilationUnit cu, String newContent) throws Exception {
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu);
		Job.getJobManager().join(ReconcilerJobFamilies.FAMILY_RECONCILER, null);
		Job.getJobManager().join(DocumentLineDiffer.QUICKDIFF_INITIALIZE_FAMILY, null);
		Job.getJobManager().join(LastSaveReferenceProvider.JOB_FAMILY, null);
		TestUtils.cancelDecorationJob();

		cu.getBuffer().setContents(newContent);
		editor.doSave(null);
	}

	/**
	 * Sets the given content and saves the compilation unit through the Java document provider from a
	 * thread other than the UI thread, without opening an editor.
	 */
	private static void saveCUInBackgroundThread(ICompilationUnit cu, String newContent) throws Exception {
		IDocumentProvider provider= JavaUI.getDocumentProvider();
		IEditorInput input= new FileEditorInput((IFile) cu.getResource());
		provider.connect(input);
		try {
			IDocument document= provider.getDocument(input);
			document.set(newContent);

			CompletableFuture<Void> save= CompletableFuture.runAsync(() -> {
				try {
					provider.saveDocument(new NullProgressMonitor(), input, document, true);
				} catch (CoreException e) {
					throw new CompletionException(e);
				}
			});

			// Keep the UI thread responsive in case the save needs it
			Display display= Display.getCurrent();
			while (!save.isDone()) {
				if (!display.readAndDispatch()) {
					Thread.sleep(10);
				}
			}
			save.join();
		} finally {
			provider.disconnect(input);
		}
	}

	/**
	 * Returns whether the last save actions can be undone, without undoing them.
	 */
	private static IStatus computeUndoableStatusOfSaveActions() throws ExecutionException {
		IUndoableOperation operation= OperationHistoryFactory.getOperationHistory().getUndoOperation(PlatformUI.getWorkbench().getOperationSupport().getUndoContext());
		assertNotNull(operation);
		assertEquals(FixMessages.CleanUpPostSaveListener_SaveAction_ChangeName, operation.getLabel());
		return ((IAdvancedUndoableOperation) operation).computeUndoableStatus(new NullProgressMonitor());
	}

	@Test
	public void testFormatAll01() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {
			    public void foo( Object o ) {
			        String s= (String)o;
			    }
			}""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {
			    public void foo( Object o ) {
			        String s    = (String)o;
			    }
			}""";

		String expected1= """
			package test1;
			public class E1 {
			    public void foo(Object o) {
			        String s = (String) o;
			    }
			}""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testFormatChanges01() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {
			    public void foo( Object o ) {
			        String s= (String)o;
			    }
			}""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {
			    public void foo( Object o ) {
			        String s    = (String)o;
			    }
			}""";

		String expected1= """
			package test1;
			public class E1 {
			    public void foo( Object o ) {
			        String s = (String) o;
			    }
			}""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testFormatChanges02() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {
			    public void foo( Object o ) {
			        Object s= (String)o;
			}}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {
			    public void foo( Object o ) {
			        Object s       = (String)o;
			}}
			""";

		String expected1= """
			package test1;
			public class E1 {
			    public void foo( Object o ) {
			        Object s = o;
			}}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
		enable(CleanUpConstants.REMOVE_UNNECESSARY_CASTS);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testFormatChangesBug205177() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {
			    int        a= 1;
			    int        b= 2;
			    int        c= 3;
			    int        d= 4;
			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {
			    int        a= 1;
			    int        b= 2;//
			    int        c= 3;
			    int        d= 4;
			}
			""";

		String expected1= """
			package test1;
			public class E1 {
			    int        a= 1;
			    int b = 2;//
			    int        c= 3;
			    int        d= 4;
			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testFormatChangesBug205308() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {
			    int        a= 1;
			    int        b= 2;
			    int        c= 3;
			    int        d= 4;
			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {
			    int         a= 1;
			    int        b= 2;
			}
			""";

		String expected1= """
			package test1;
			public class E1 {
			    int a = 1;
			    int        b= 2;
			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testFormatChangesBug205301() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;

			public class E1 {
			    /**
			     * adsfdas
			     * dafs
			     */
			    int a = 2;

			    /**
			     * adsfasd\s
			     * asd
			     */
			    int b = 2;
			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;

			public class E1 {
			    /**
			     * adsfdas
			     * dafs\s
			     */
			    int a = 2;

			    /**
			     * adsfasd\s
			     * asd
			     */
			    int b = 2;
			}
			""";

		String expected1= """
			package test1;

			public class E1 {
			    /**
			     * adsfdas dafs
			     */
			    int a = 2;

			    /**
			     * adsfasd\s
			     * asd
			     */
			    int b = 2;
			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testFormatChangesBug207965() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {
			       protected String foo(String string) { \s
			          int i = 10;
			          return ("" + string + "") + ""; \s
			    }
			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {
			       protected String foo(String string) { \s
			          int i = 10;
			          return  ("" + string + "") + ""; \s
			    }
			}
			""";

		String expected1= """
			package test1;
			public class E1 {
			    protected String foo(String string) {
			        int i = 10;
			        return ("" + string + "") + "";
			    }
			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testFormatChangesBug207965_2() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {
			    public int i = 10;
			   \s
			    public int j = 10;
			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {
			    public int i= 10;
			   \s
			    public int j= 10;
			}
			""";

		String expected1= """
			package test1;
			public class E1 {
			    public int i = 10;

			    public int j = 10;
			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testFormatChangesBug208568() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {
			    public int i = 10;   \s

			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {
			    public  int i= 10;   \s

			}
			""";

		String expected1= """
			package test1;
			public class E1 {
			    public int i = 10;

			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testBug213248_1() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {

			    public int field;
			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {
			\s
			    public int field;
			}
			""";

		String expected1= """
			package test1;
			public class E1 {

			    public int field;
			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testBug213248_2() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {

			    public int field;

			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {

			    public int field;
			\s
			}
			""";

		String expected1= """
			package test1;
			public class E1 {

			    public int field;

			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testBug213248_3() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {

			    public int field;
			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {
			\s
			    public int field;
			}
			""";

		String expected1= """
			package test1;
			public class E1 {

			    public int field;
			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
		enable(CleanUpConstants.FORMAT_CORRECT_INDENTATION);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testBug213248_4() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {

			    public int field;

			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {

			    public int field;
			\s
			}
			""";

		String expected1= """
			package test1;
			public class E1 {

			    public int field;

			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
		enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testBug228659() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package a;
			public class Test {
			    /**
			     */
			    public void foo() {
			        String s1 = "";
			    }
			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package a;
			public class Test {
			    /**
			     */
			    public void foo() {
			        String s1  = "";
			    }
			}
			""";

		String expected1= """
			package a;
			public class Test {
			    /**
			     */
			    public void foo() {
			        String s1 = "";
			    }
			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testBug232768_1() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {

			    /**
			     * A Java comment on
			     * two lines
			     */

			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {

			    /**
			     * A Java comment on
			     *  two lines
			     */

			}
			""";

		String expected1= """
			package test1;
			public class E1 {

			    /**
			     * A Java comment on two lines
			     */

			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testBug232768_2() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {

			    /*
			     * A block comment on
			     * two lines
			     */

			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {

			    /*
			     * A block comment on
			     *  two lines
			     */

			}
			""";

		String expected1= """
			package test1;
			public class E1 {

			    /*
			     * A block comment on two lines
			     */

			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testBug232768_3() throws Exception {
		// Given
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {

			    //long long long long long long long long long long long long long long long long

			}
			""";
		ICompilationUnit cu1= pack2.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {

			    // long long long long long long long long long long long long long long long long

			}
			""";

		String expected1= """
			package test1;
			public class E1 {

			    // long long long long long long long long long long long long long long
			    // long long

			}
			""";

		enable(CleanUpConstants.FORMAT_SOURCE_CODE);
		enable(CleanUpConstants.FORMAT_SOURCE_CODE_CHANGES_ONLY);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testFormatChangeBug488229_1() throws Exception {
		Hashtable<String, String> oldOptions= JavaCore.getOptions();

		try {
			// Given
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String fileOnDisk= """
				package test1;
				public class E1 {
				    /**
				     * Method foo
				     * @param a - integer input
				     * @return integer
				     */
				    public int foo( int a ) {
				        return 0;
				    }
				}""";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

			String fileOnEditor= """
				package test1;
				public class E1 {
				    /**
				     * Method foo \s
				     * @param a - integer input \s
				     * @return integer \s
				     */
				    public int foo( int a ) {
				        return 0;
				    }
				}""";

			String expected1= """
				package test1;
				public class E1 {
					/**
					 * Method foo
					 *
					 * @param a
					 *            - integer input
					 * @return integer
					 */
					public int foo(int a) {
						return 0;
					}
				}""";

			enable(CleanUpConstants.FORMAT_SOURCE_CODE);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);
			Hashtable<String, String> coreOptions= new Hashtable<>();
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, DefaultCodeFormatterConstants.TRUE);
			JavaCore.setOptions(coreOptions);

			// When
			editCUInEditor(cu1, fileOnEditor);

			assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
		} finally {
			JavaCore.setOptions(oldOptions);
		}
	}

	@Test
	public void testFormatChangeBug488229_2() throws Exception {
		Hashtable<String,String> oldOptions= JavaCore.getOptions();

		try {
			// Given
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String fileOnDisk= """
				package test1;
				public class E1 {
				    /**
				     * Method foo
				     * @param a - integer input
				     * @return integer
				     */
				    public int foo( int a ) {
				        return 0;
				    }
				}""";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

			String fileOnEditor= """
				package test1;
				public class E1 {
				    /**
				     * Method foo \s
				     * @param a - integer input \s
				     * @return integer \s
				     */
				    public int foo( int a ) {
				        return 0;
				    }
				}""";

			String expected1= """
				package test1;
				public class E1 {
					/**
					 * Method foo
					 *
					 * @param a
					 *            - integer input
					 *
					 * @return integer
					 */
					public int foo(int a) {
						return 0;
					}
				}""";

			enable(CleanUpConstants.FORMAT_SOURCE_CODE);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);
			Hashtable<String, String> coreOptions= new Hashtable<>();
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BETWEEN_DIFFERENT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, DefaultCodeFormatterConstants.TRUE);
			JavaCore.setOptions(coreOptions);

			// When
			editCUInEditor(cu1, fileOnEditor);

			// Then
			assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
		} finally {
			JavaCore.setOptions(oldOptions);
		}
	}

	private void assertChangedFromTo(ICompilationUnit cu, String from, String fileOnEditor, String expected) throws JavaModelException {
		String contents= cu.getBuffer().getContents();
		if (!expected.equals(from)) {
			assertNotEquals("contents was not changed", from, contents);
		}
		if (!expected.equals(fileOnEditor)) {
			assertNotEquals("contents was not formated", fileOnEditor, contents);
		}
		assertEquals("contents was wrong formated", expected, contents);
	}

	@Test
	public void testFormatChangeBug488229_3() throws Exception {
		Hashtable<String,String> oldOptions= JavaCore.getOptions();

		try {
			// Given
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String fileOnDisk= """
				package test1;
				public class E1 {
				    /**
				     * Method foo
				     *	          @param a - integer input
				     * @return integer
				     */
				    public int foo( int a ) {
				        return 0;
				    }
				}""";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

			String fileOnEditor= """
				package test1;
				public class E1 {
				    /**
				     * Method foo \s
				     *	          @param a - integer input \s
				     * @return integer \s
				     */
				    public int foo( int a ) {
				        return 0;
				    }
				}""";

			String expected1= """
				package test1;
				public class E1 {
					/**
					 * Method foo
					 *
					 * @param a
					 *            - integer input
					 *
					 * @return integer
					 */
					public int foo(int a) {
						return 0;
					}
				}""";

			enable(CleanUpConstants.FORMAT_SOURCE_CODE);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);
			Hashtable<String, String> coreOptions= new Hashtable<>();
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BETWEEN_DIFFERENT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, DefaultCodeFormatterConstants.TRUE);
			JavaCore.setOptions(coreOptions);

			// When
			editCUInEditor(cu1, fileOnEditor);

			assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
		} finally {
			JavaCore.setOptions(oldOptions);
		}
	}
	@Test
	public void testFormatChangeBug561164() throws Exception {
		Hashtable<String, String> oldOptions= JavaCore.getOptions();

		try {
			// Given
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String fileOnDisk= """
				package test1;
				public class E1 {
				    /**
				     * Method foo with a really long description that will wrap lines on save operation
				     *	          @param a - integer input
				     * @return integer
				     */
				    public int foo( int a ) {
				        return 0;
				    }
				}""";
			ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

			String fileOnEditor= """
				package test1;
				public class E1 {
				    /**
				     * Method foo with a really long description that will wrap lines on save operation \s
				     *	          @param a - integer input \s
				     * @return integer \s
				     */
				    public int foo( int a ) {
				        return 0;
				    }
				}""";

			String expected1= """
				package test1;
				public class E1 {
					/**
					 * Method foo with a really long description that will wrap lines on save
					 * operation
					 *
					 * @param a
					 *            - integer input
					 *
					 * @return integer
					 */
					public int foo(int a) {
						return 0;
					}
				}""";

			enable(CleanUpConstants.FORMAT_SOURCE_CODE);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES);
			enable(CleanUpConstants.FORMAT_REMOVE_TRAILING_WHITESPACES_ALL);
			Hashtable<String, String> coreOptions= new Hashtable<>();
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BEFORE_ROOT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_INSERT_EMPTY_LINE_BETWEEN_DIFFERENT_TAGS, JavaCore.INSERT);
			coreOptions.put(DefaultCodeFormatterConstants.FORMATTER_COMMENT_FORMAT_JAVADOC_COMMENT, DefaultCodeFormatterConstants.TRUE);
			JavaCore.setOptions(coreOptions);

			// When
			editCUInEditor(cu1, fileOnEditor);

			assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
		} finally {
			JavaCore.setOptions(oldOptions);
		}
	}

	@Test
	public void testFormatChangeBug560429() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test;\r
			import java.util.ArrayList;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			public class A {\r
				public A() {\r
					List<List<Integer>> mylistlist=new ArrayList<>();\r
					for (Iterator<List<Integer>> mylistlistiterator= mylistlist.iterator(); mylistlistiterator.hasNext(); ) {\r
						for (Iterator<Integer> mylistiterator= mylistlistiterator.next().iterator(); mylistiterator.hasNext(); ) {\r
							int foo= mylistiterator.next().intValue();\r
						}\r
					}\r
				}\r
			}""";
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test;\r
			import java.util.ArrayList;\r
			import java.util.Iterator;\r
			import java.util.List;\r
			public class A {\r
				public A() {\r
					List<List<Integer>> mylistlist=new ArrayList<>();\r
					for (Iterator<List<Integer>> mylistlistiterator= mylistlist.iterator(); mylistlistiterator.hasNext(); ) {\r
						for (Iterator<Integer> mylistiterator= mylistlistiterator.next().iterator(); mylistiterator.hasNext(); ) {\r
							int foo= mylistiterator.next().intValue();\r
						}\r
					}\r
				}\r
			}""";

		String expected1= """
			package test;\r
			import java.util.ArrayList;\r
			import java.util.List;\r
			public class A {\r
				public A() {\r
					List<List<Integer>> mylistlist=new ArrayList<>();\r
					for (List<Integer> list : mylistlist) {\r
						for (Integer integer : list) {\r
							int foo= integer.intValue();\r
						}\r
					}\r
				}\r
			}""";

		enable(CleanUpConstants.CONTROL_STATEMENTS_CONVERT_FOR_LOOP_TO_ENHANCED);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testIssue313_1() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {

			    private void m1(Object p1) {
			    }
			}
			"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {

			    private void m1(Object p1) {
			    }
			}
			"""; //

		String expected1= """
			package test1;
			public class E1 {

			    private void m1() {
			    }
			}
			"""; //

		enable(CleanUpConstants.REMOVE_UNUSED_CODE_METHOD_PARAMETERS);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testIssue313_2() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			public class E1 {

			    private void m1(Object p1) {
			    }
			}
			"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			public class E1 {

			    private void m1(Object p1) {
			    }
			}
			"""; //

		String expected1= """
			package test1;
			public class E1 {

			    private void m1(Object p1) {
			    }
			}
			"""; //

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testSaveInBackgroundThread() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
				package test1;
				import java.util.ArrayList;
				import java.util.List;

				public class E1 {
				    private void m1() {
				    }
				}
				"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
				package test1;
				import java.util.ArrayList;
				import java.util.List;

				public class E1 {
				    private void m1() {
				        List<String> myList = new ArrayList<String>();
				    }
				}
				"""; //

		String expected1= """
				package test1;
				import java.util.ArrayList;
				import java.util.List;

				public class E1 {
				    private void m1() {
				        List<String> myList = new ArrayList<>();
				    }
				}
				"""; //

		// This clean up requires an AST
		enable(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS);

		// When
		saveCUInBackgroundThread(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);
	}

	@Test
	public void testEditDocumentWhileCalculatingChanges() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
				package test1;
				public class E1 {
				}
				"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
				package test1;
				public class E1 {
				    int toReplace;
				}
				"""; //

		String edit= "// edited while saving\n";

		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu1);
		try {
			Job.getJobManager().join(ReconcilerJobFamilies.FAMILY_RECONCILER, null);
			Job.getJobManager().join(DocumentLineDiffer.QUICKDIFF_INITIALIZE_FAMILY, null);
			Job.getJobManager().join(LastSaveReferenceProvider.JOB_FAMILY, null);
			TestUtils.cancelDecorationJob();
			cu1.getBuffer().setContents(fileOnEditor);

			// The document is edited (e.g. by a syncExec() of another thread) after the clean up calculated its offsets
			IDocument document= editor.getDocumentProvider().getDocument(editor.getEditorInput());
			PausingCleanUp.arm("toReplace", "replaced", () -> {
				try {
					document.replace(0, 0, edit);
				} catch (BadLocationException e) {
					throw new IllegalStateException(e);
				}
			});

			// When
			editor.doSave(null);

			// Then: the outdated clean up is not applied
			assertEquals(edit + fileOnEditor, document.get());
		} finally {
			PausingCleanUp.disarm();
			editor.close(false);
		}
	}

	@Test
	public void testShortenDocumentWhileCalculatingChanges() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
				package test1;
				public class E1 {
				}
				"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
				package test1;
				public class E1 {
				    int toReplace;
				}
				"""; //

		String afterEdit= """
				package test1;
				"""; //

		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu1);
		try {
			Job.getJobManager().join(ReconcilerJobFamilies.FAMILY_RECONCILER, null);
			Job.getJobManager().join(DocumentLineDiffer.QUICKDIFF_INITIALIZE_FAMILY, null);
			Job.getJobManager().join(LastSaveReferenceProvider.JOB_FAMILY, null);
			TestUtils.cancelDecorationJob();
			cu1.getBuffer().setContents(fileOnEditor);

			// The text at the offsets of the clean up is deleted, so reading it fails
			IDocument document= editor.getDocumentProvider().getDocument(editor.getEditorInput());
			PausingCleanUp.arm("toReplace", "replaced", () -> document.set(afterEdit));

			// When
			editor.doSave(null);

			// Then: the edit is not reverted
			assertEquals(afterEdit, document.get());
		} finally {
			PausingCleanUp.disarm();
			editor.close(false);
		}
	}

	@Test
	public void testRestoreModificationStampWhileCalculatingChanges() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
				package test1;
				public class E1 {
				}
				"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
				package test1;
				public class E1 {
				    int toReplace;
				}
				"""; //

		String edit= "// edited while saving\n";

		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu1);
		try {
			Job.getJobManager().join(ReconcilerJobFamilies.FAMILY_RECONCILER, null);
			Job.getJobManager().join(DocumentLineDiffer.QUICKDIFF_INITIALIZE_FAMILY, null);
			Job.getJobManager().join(LastSaveReferenceProvider.JOB_FAMILY, null);
			TestUtils.cancelDecorationJob();
			cu1.getBuffer().setContents(fileOnEditor);

			// Like an undo followed by a redo, the edits restore both the text and the modification stamp
			IDocument document= editor.getDocumentProvider().getDocument(editor.getEditorInput());
			IDocumentExtension4 documentExtension= (IDocumentExtension4) document;
			PausingCleanUp.arm("toReplace", "replaced", () -> {
				try {
					long stamp= documentExtension.getModificationStamp();
					document.replace(0, 0, edit);
					documentExtension.replace(0, edit.length(), "", stamp);
				} catch (BadLocationException e) {
					throw new IllegalStateException(e);
				}
			});

			// When
			editor.doSave(null);

			// Then: the clean up is not applied although the document looks unchanged
			assertEquals(fileOnEditor, document.get());
		} finally {
			PausingCleanUp.disarm();
			editor.close(false);
		}
	}

	@Test
	public void testUserInputIsBlockedWhileCalculatingChanges() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
				package test1;
				public class E1 {
				}
				"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
				package test1;
				public class E1 {
				    int toReplace;
				}
				"""; //

		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu1);
		try {
			Job.getJobManager().join(ReconcilerJobFamilies.FAMILY_RECONCILER, null);
			Job.getJobManager().join(DocumentLineDiffer.QUICKDIFF_INITIALIZE_FAMILY, null);
			Job.getJobManager().join(LastSaveReferenceProvider.JOB_FAMILY, null);
			TestUtils.cancelDecorationJob();
			cu1.getBuffer().setContents(fileOnEditor);

			// While the calculation is paused, check whether the user could still interact with the workbench window.
			// Either the window is disabled, or (after 800 ms) it is enabled again right before the modal progress dialog opens.
			Shell windowShell= editor.getSite().getShell();
			AtomicBoolean inputBlocked= new AtomicBoolean();
			PausingCleanUp.arm("toReplace", "replaced", () -> {
				boolean progressDialogOpen= Arrays.stream(windowShell.getDisplay().getShells())
						.anyMatch(shell -> shell.getData() instanceof ProgressMonitorDialog && shell.isVisible());
				inputBlocked.set(!windowShell.isEnabled() || progressDialogOpen);
			});

			// When
			editor.doSave(null);

			// Then: the user input was blocked during the calculation and the window is enabled again afterwards
			assertTrue(inputBlocked.get());
			assertTrue(windowShell.isEnabled());
		} finally {
			PausingCleanUp.disarm();
			editor.close(false);
		}
	}

	@Test
	public void testCloseEditorWhileCalculatingChanges() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
				package test1;
				public class E1 {
				}
				"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
				package test1;
				public class E1 {
				    int toReplace;
				}
				"""; //

		IUndoManager undoManager= RefactoringCore.getUndoManager();
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu1);
		AtomicBoolean closed= new AtomicBoolean();
		try {
			Job.getJobManager().join(ReconcilerJobFamilies.FAMILY_RECONCILER, null);
			Job.getJobManager().join(DocumentLineDiffer.QUICKDIFF_INITIALIZE_FAMILY, null);
			Job.getJobManager().join(LastSaveReferenceProvider.JOB_FAMILY, null);
			TestUtils.cancelDecorationJob();
			cu1.getBuffer().setContents(fileOnEditor);
			undoManager.flush();

			// The editor is closed (programmatically) while the clean ups are calculated
			PausingCleanUp.arm("toReplace", "replaced", () -> closed.set(editor.getSite().getPage().closeEditor(editor, false)));

			// When
			editor.doSave(null);

			// Then: the file is saved, the clean up is not applied to the closed document and nothing is registered for undo
			assertTrue(closed.get());
			assertEquals(fileOnEditor, cu1.getSource());
			assertFalse(undoManager.anythingToUndo());
		} finally {
			PausingCleanUp.disarm();
			undoManager.flush();
			if (!closed.get()) {
				editor.close(false);
			}
		}
	}

	@Test
	public void testCloseEditorWhileCalculatingChangesOfSecondPass() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
				package test1;
				public class E1 {
				}
				"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
				package test1;
				public class E1 {
				    int first;
				    int second;
				}
				"""; //

		IUndoManager undoManager= RefactoringCore.getUndoManager();
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu1);
		AtomicBoolean closed= new AtomicBoolean();
		try {
			Job.getJobManager().join(ReconcilerJobFamilies.FAMILY_RECONCILER, null);
			Job.getJobManager().join(DocumentLineDiffer.QUICKDIFF_INITIALIZE_FAMILY, null);
			Job.getJobManager().join(LastSaveReferenceProvider.JOB_FAMILY, null);
			TestUtils.cancelDecorationJob();
			cu1.getBuffer().setContents(fileOnEditor);
			undoManager.flush();

			// The editor is closed (programmatically) while the clean ups of the second pass are calculated
			PausingCleanUp.armInSeparatePasses(
					new PausingCleanUp.Replacement("first", "replacedInFirstPass", null),
					new PausingCleanUp.Replacement("second", "replacedInSecondPass", () -> closed.set(editor.getSite().getPage().closeEditor(editor, false))));

			// When
			editor.doSave(null);

			// Then: the changes of the first pass were not saved, so nothing is registered for undo
			assertTrue(closed.get());
			assertEquals(fileOnEditor, cu1.getSource());
			assertFalse(undoManager.anythingToUndo());
		} finally {
			PausingCleanUp.disarm();
			undoManager.flush();
			if (!closed.get()) {
				editor.close(false);
			}
		}
	}

	@Test
	public void testProgressStaysIndeterminateWhileCalculatingChanges() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
				package test1;
				public class E1 {
				}
				"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
				package test1;
				public class E1 {
				    int toReplace;
				}
				"""; //

		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu1);
		try {
			Job.getJobManager().join(ReconcilerJobFamilies.FAMILY_RECONCILER, null);
			Job.getJobManager().join(DocumentLineDiffer.QUICKDIFF_INITIALIZE_FAMILY, null);
			Job.getJobManager().join(LastSaveReferenceProvider.JOB_FAMILY, null);
			TestUtils.cancelDecorationJob();
			cu1.getBuffer().setContents(fileOnEditor);

			// The AST is parsed, and the progress bar is checked while the changes are calculated
			AtomicReference<Boolean> indeterminate= new AtomicReference<>();
			PausingCleanUp.armInSeparatePasses(new PausingCleanUp.Replacement("toReplace", "replaced", () -> {
				for (Shell shell : Display.getCurrent().getShells()) {
					ProgressIndicator indicator= shell.getData() instanceof ProgressMonitorDialog ? findProgressIndicator(shell) : null;
					if (indicator != null) {
						Control shown= ((StackLayout) indicator.getLayout()).topControl;
						indeterminate.set(shown instanceof ProgressBar bar && (bar.getStyle() & SWT.INDETERMINATE) != 0);
					}
				}
			}));
			PausingCleanUp.forceParsingAst();

			// When
			editor.doSave(null);

			// Then: the progress of unknown duration was not replaced by a determinate one when parsing the AST
			assertEquals("No progress dialog found, or its progress is not indeterminate", Boolean.TRUE, indeterminate.get());
		} finally {
			PausingCleanUp.disarm();
			editor.close(false);
		}
	}

	private static ProgressIndicator findProgressIndicator(Control control) {
		if (control instanceof ProgressIndicator indicator) {
			return indicator;
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				ProgressIndicator found= findProgressIndicator(child);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	@Test
	public void testEditDocumentWhileCalculatingNoChangeOfFirstPass() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
				package test1;
				public class E1 {
				}
				"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
				package test1;
				public class E1 {
				    int second;
				}
				"""; //

		String edit= "// edited while saving\n";

		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu1);
		try {
			Job.getJobManager().join(ReconcilerJobFamilies.FAMILY_RECONCILER, null);
			Job.getJobManager().join(DocumentLineDiffer.QUICKDIFF_INITIALIZE_FAMILY, null);
			Job.getJobManager().join(LastSaveReferenceProvider.JOB_FAMILY, null);
			TestUtils.cancelDecorationJob();
			cu1.getBuffer().setContents(fileOnEditor);

			// The first pass finds nothing to change and defers the second clean up to the next pass, the document is
			// edited while the first pass is calculated
			IDocument document= editor.getDocumentProvider().getDocument(editor.getEditorInput());
			PausingCleanUp.armInSeparatePasses(
					new PausingCleanUp.Replacement("notThere", "unused", () -> {
						try {
							document.replace(0, 0, edit);
						} catch (BadLocationException e) {
							throw new IllegalStateException(e);
						}
					}),
					new PausingCleanUp.Replacement("second", "replacedInSecondPass", null));

			// When
			editor.doSave(null);

			// Then: the clean ups deferred while the document was outdated are not run
			assertEquals(edit + fileOnEditor, document.get());
		} finally {
			PausingCleanUp.disarm();
			editor.close(false);
		}
	}

	@Test
	public void testEditDocumentWhileCalculatingChangesOfSecondPass() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
				package test1;
				public class E1 {
				}
				"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
				package test1;
				public class E1 {
				    int first;
				    int second;
				}
				"""; //

		String edit= "// edited while saving\n";

		String expected= edit + """
				package test1;
				public class E1 {
				    int replacedInFirstPass;
				    int second;
				}
				"""; //

		IUndoManager undoManager= RefactoringCore.getUndoManager();
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu1);
		try {
			Job.getJobManager().join(ReconcilerJobFamilies.FAMILY_RECONCILER, null);
			Job.getJobManager().join(DocumentLineDiffer.QUICKDIFF_INITIALIZE_FAMILY, null);
			Job.getJobManager().join(LastSaveReferenceProvider.JOB_FAMILY, null);
			TestUtils.cancelDecorationJob();
			cu1.getBuffer().setContents(fileOnEditor);
			undoManager.flush();

			// The document is edited after the second pass calculated its offsets
			IDocument document= editor.getDocumentProvider().getDocument(editor.getEditorInput());
			PausingCleanUp.armInSeparatePasses(
					new PausingCleanUp.Replacement("first", "replacedInFirstPass", null),
					new PausingCleanUp.Replacement("second", "replacedInSecondPass", () -> {
						try {
							document.replace(0, 0, edit);
						} catch (BadLocationException e) {
							throw new IllegalStateException(e);
						}
					}));

			// When
			editor.doSave(null);

			// Then: the first pass is kept, the outdated second pass is not applied
			assertEquals(expected, document.get());

			// Then: the first pass is registered for undo, but undoing it is rejected since its undo edits are outdated
			assertEquals(IStatus.ERROR, computeUndoableStatusOfSaveActions().getSeverity());
		} finally {
			PausingCleanUp.disarm();
			undoManager.flush();
			editor.close(false);
		}
	}

	@Test
	public void testUndoSeveralPasses() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
				package test1;
				public class E1 {
				}
				"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
				package test1;
				public class E1 {
				    int first;
				    int second;
				}
				"""; //

		String expected= """
				package test1;
				public class E1 {
				    int replacedInFirstPass;
				    int replacedInSecondPass;
				}
				"""; //

		IUndoManager undoManager= RefactoringCore.getUndoManager();
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu1);
		try {
			Job.getJobManager().join(ReconcilerJobFamilies.FAMILY_RECONCILER, null);
			Job.getJobManager().join(DocumentLineDiffer.QUICKDIFF_INITIALIZE_FAMILY, null);
			Job.getJobManager().join(LastSaveReferenceProvider.JOB_FAMILY, null);
			TestUtils.cancelDecorationJob();
			cu1.getBuffer().setContents(fileOnEditor);
			undoManager.flush();

			IDocument document= editor.getDocumentProvider().getDocument(editor.getEditorInput());
			PausingCleanUp.armInSeparatePasses(
					new PausingCleanUp.Replacement("first", "replacedInFirstPass", null),
					new PausingCleanUp.Replacement("second", "replacedInSecondPass", null));

			// When
			editor.doSave(null);

			// Then
			assertEquals(expected, document.get());
			assertTrue(computeUndoableStatusOfSaveActions().isOK());
		} finally {
			PausingCleanUp.disarm();
			undoManager.flush();
			editor.close(false);
		}
	}

	@Test
	public void testIssue2288() throws Exception {
		// Given
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String fileOnDisk= """
			package test1;
			import java.util.List;
			import java.util.ArrayList;

			@SuppressWarnings("removal")
			public class E1 {

			    private void m1() {
					List<String> myList = new ArrayList<String>();
			    }
			}
			"""; //

		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", fileOnDisk, false, null);

		String fileOnEditor= """
			package test1;
			import java.util.List;
			import java.util.ArrayList;

			@SuppressWarnings("removal")
			public class E1 {

			    private void m1() {
					List<String> myList = new ArrayList<String>();
			    }
			}
			"""; //

		String expected1= """
			package test1;
			import java.util.List;
			import java.util.ArrayList;

			public class E1 {

			    private void m1() {
					List<String> myList = new ArrayList<>();
			    }
			}
			"""; //

		enable(CleanUpConstants.REMOVE_UNNECESSARY_SUPPRESS_WARNINGS);
		enable(CleanUpConstants.REMOVE_REDUNDANT_TYPE_ARGUMENTS);

		// When
		editCUInEditor(cu1, fileOnEditor);

		assertChangedFromTo(cu1, fileOnDisk, fileOnEditor, expected1);

	}

	// Use debug tracing for: https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/79
	private static void setDebugTracesEnabled(boolean enable) {
		TestUtils.setDebugEnabled(CopyOnWriteTextStore.class, enable);
		TestUtils.setDebugEnabled(JavaCore.getPlugin().getBundle(), enable,
				"/debug", "/debug/buffermanager", "/debug/javamodel");
	}
}
