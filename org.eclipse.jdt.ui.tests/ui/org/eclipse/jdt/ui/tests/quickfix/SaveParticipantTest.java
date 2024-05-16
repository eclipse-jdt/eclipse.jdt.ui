/*******************************************************************************
 * Copyright (c) 2007, 2023 IBM Corporation and others.
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

import java.util.Hashtable;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.ui.PartInitException;

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

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class SaveParticipantTest extends CleanUpTestCase {
	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

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
	}

	private static void editCUInEditor(ICompilationUnit cu, String newContent) throws JavaModelException, PartInitException {
		JavaEditor editor= (JavaEditor) EditorUtility.openInEditor(cu);

		cu.getBuffer().setContents(newContent);
		editor.doSave(null);
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

			// Then
			assertEquals(expected1, cu1.getBuffer().getContents());
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
			assertEquals(expected1, cu1.getBuffer().getContents());
		} finally {
			JavaCore.setOptions(oldOptions);
		}
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

			// Then
			assertEquals(expected1, cu1.getBuffer().getContents());
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

			// Then
			assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
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

		// Then
		assertEquals(expected1, cu1.getBuffer().getContents());
	}
}
