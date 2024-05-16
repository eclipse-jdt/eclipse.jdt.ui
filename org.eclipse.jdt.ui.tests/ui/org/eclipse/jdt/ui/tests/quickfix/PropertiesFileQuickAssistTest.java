/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesAssistContext;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileEditor;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesFileEditorMessages;
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertiesQuickAssistProcessor;

public class PropertiesFileQuickAssistTest {

	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private IJavaProject fJProject;
	private IPackageFragmentRoot fSourceFolder;

	private final String REMOVE_KEY= PropertiesFileEditorMessages.PropertiesCorrectionProcessor_remove_property_label;
	private final String REMOVE_KEYS= PropertiesFileEditorMessages.PropertiesCorrectionProcessor_remove_properties_label;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");

		JavaCore.setOptions(options);

		setPreferences();

		fJProject= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject, "src");

		IPath osgiJar= new Path("testresources/org.eclipse.osgi_3_7_stub.jar");
		JavaProjectHelper.addLibrary(fJProject, JavaProjectHelper.findRtJar(osgiJar)[0]);
	}

	@Deprecated
	private void setPreferences() {
		Preferences corePrefs= JavaPlugin.getJavaCorePluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_SUFFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, "");
	}


	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject, projectSetup.getDefaultClasspath());
	}

	private static IFile createPropertyFile(IPackageFragment pack, String name, String content) throws UnsupportedEncodingException, CoreException {
		ByteArrayInputStream is= new ByteArrayInputStream(content.getBytes("8859_1"));
		IFile file= ((IFolder)pack.getResource()).getFile(name);
		file.create(is, false, null);
		return file;
	}

	private static void checkContentOfCu(String message, ICompilationUnit cu, String content) throws Exception {
		assertEqualLines(message, content, cu.getBuffer().getContents());
	}

	private static void checkContentOfFile(String message, IFile file, String content) throws Exception {
		try (InputStream in= file.getContents()) {
			assertEqualLines(message, content, new String(in.readAllBytes()));
		}
	}

	private static void assertEqualLines(String message, String expected, String actual) {
		String[] expectedLines= Strings.convertIntoLines(expected);
		String[] actualLines= Strings.convertIntoLines(actual);

		String expected2= (expectedLines == null ? null : Strings.concatenate(expectedLines, "\n"));
		String actual2= (actualLines == null ? null : Strings.concatenate(actualLines, "\n"));
		assertEquals(message, expected2, actual2);
	}

	private static List<ICompletionProposal> collectAssists(PropertiesAssistContext context) throws Exception {
		ICompletionProposal[] assists= PropertiesQuickAssistProcessor.collectAssists(context);
		if (assists == null)
			return Collections.EMPTY_LIST;
		if (assists.length > 0) {
			assertTrue("should be marked as 'has assist'", PropertiesQuickAssistProcessor.hasAssists(context));
		}
		return Arrays.asList(assists);
	}

	private static IEditorPart openEditor(IFile file) throws Exception {
		IWorkbenchPage page= JavaPlugin.getActivePage();
		if (page == null)
			throw new Exception();

		return IDE.openEditor(page, file, true);
	}

	private PropertiesAssistContext createAssistContext(IFile file, int offset, int length) throws Exception {
		IEditorPart editorPart= openEditor(file);
		ISourceViewer sourceViewer= (ISourceViewer)editorPart.getAdapter(ITextOperationTarget.class);
		IType accessorType= ((PropertiesFileEditor)editorPart).getAccessorType();
		return new PropertiesAssistContext(sourceViewer, offset, length, file, sourceViewer.getDocument(), accessorType);
	}

	@Test
	public void testCreateFieldInAccessor1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		String str= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Accessor.java", str, false, null);

		// property file
		String str1= """
			Test_1=Hello1
			Test_2=Hello2
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str1);

		int offset= str1.indexOf("est_1");
		PropertiesAssistContext context= createAssistContext(file, offset, 0);
		List<ICompletionProposal> proposals= collectAssists(context);

		QuickFixTest.assertNumberOfProposals(proposals, 2);
		QuickFixTest.assertCorrectLabels(proposals);

		ICompletionProposal proposal= proposals.get(0);
		proposal.apply(context.getDocument());

		// Accessor class
		String str2= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			}
			""";
		checkContentOfCu("nls file", cu, str2);
	}

	@Test
	public void testCreateFieldInAccessor2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		String str= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Accessor.java", str, false, null);

		// property file
		String str1= """
			Test_1=Hello1
			Test_2=Hello2
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str1);

		int offset1= str1.indexOf("Test_1");
		int offset2= str1.indexOf("Hello2");
		PropertiesAssistContext context= createAssistContext(file, offset1, offset2 - offset1);
		List<ICompletionProposal> proposals= collectAssists(context);

		QuickFixTest.assertNumberOfProposals(proposals, 2);
		QuickFixTest.assertCorrectLabels(proposals);

		ICompletionProposal proposal= proposals.get(0);
		proposal.apply(context.getDocument());

		// Accessor class
		String str2= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			    public static String Test_2;
			}
			""";
		checkContentOfCu("nls file", cu, str2);
	}

	@Test
	public void testCreateFieldInAccessor3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		String str= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			    public static String Test_2;
			    public static String Test_5;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Accessor.java", str, false, null);

		// property file
		String str1= """
			Test_1=Hello1
			Test_2=Hello2
			Test_3=Hello3
			Test_4=Hello4
			Test_5=Hello5
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str1);

		int offset= str1.indexOf("Test_3");
		PropertiesAssistContext context= createAssistContext(file, offset, 0);
		List<ICompletionProposal> proposals= collectAssists(context);

		QuickFixTest.assertNumberOfProposals(proposals, 2);
		QuickFixTest.assertCorrectLabels(proposals);

		ICompletionProposal proposal= proposals.get(0);
		proposal.apply(context.getDocument());

		// Accessor class
		String str2= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			    public static String Test_2;
			    public static String Test_3;
			    public static String Test_5;
			}
			""";
		checkContentOfCu("nls file", cu, str2);
	}

	@Test
	public void testCreateFieldInAccessor4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		String str= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			    public static String Test_2;
			    public static String Test_5;
			    public static String Test_6;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Accessor.java", str, false, null);

		// property file
		String str1= """
			Test_1=Hello1
			Test_2=Hello2
			Test_3=Hello3
			Test_4=Hello4
			Test_5=Hello5
			Test_6=Hello6
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str1);

		int offset1= str1.indexOf("Test_3");
		int offset2= str1.indexOf("Hello4");
		PropertiesAssistContext context= createAssistContext(file, offset1, offset2 - offset1);
		List<ICompletionProposal> proposals= collectAssists(context);

		QuickFixTest.assertNumberOfProposals(proposals, 2);
		QuickFixTest.assertCorrectLabels(proposals);

		ICompletionProposal proposal= proposals.get(0);
		proposal.apply(context.getDocument());

		// Accessor class
		String str2= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			    public static String Test_2;
			    public static String Test_3;
			    public static String Test_4;
			    public static String Test_5;
			    public static String Test_6;
			}
			""";
		checkContentOfCu("nls file", cu, str2);
	}

	@Test
	public void testCreateFieldInAccessor5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		String str= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			    public static String Test_3;
			    public static String Test_4;
			    public static String Test_6;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Accessor.java", str, false, null);

		// property file
		String str1= """
			Test_1=Hello1
			Test_3=Hello3
			Test_4=Hello4
			Test_6=Hello6
			Test_2=Hello2
			Test_5=Hello5
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str1);

		int offset1= str1.indexOf("Test_2");
		int offset2= str1.indexOf("Hello5");
		PropertiesAssistContext context= createAssistContext(file, offset1, offset2 - offset1);
		List<ICompletionProposal> proposals= collectAssists(context);

		QuickFixTest.assertNumberOfProposals(proposals, 2);
		QuickFixTest.assertCorrectLabels(proposals);

		ICompletionProposal proposal= proposals.get(0);
		proposal.apply(context.getDocument());

		// Accessor class
		String str2= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			    public static String Test_2;
			    public static String Test_3;
			    public static String Test_4;
			    public static String Test_5;
			    public static String Test_6;
			}
			""";
		checkContentOfCu("nls file", cu, str2);
	}

	@Test
	public void testCreateFieldInAccessor6() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=361535
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		String str= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class RandomName extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, RandomName.class);
			    }
			    private RandomName() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("RandomName.java", str, false, null);

		// property file
		String str1= """
			Test_1=Hello1
			Test_2=Hello2
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str1);

		int offset= str1.indexOf("est_1");
		PropertiesAssistContext context= createAssistContext(file, offset, 0);
		List<ICompletionProposal> proposals= collectAssists(context);

		QuickFixTest.assertNumberOfProposals(proposals, 2);
		QuickFixTest.assertCorrectLabels(proposals);

		ICompletionProposal proposal= proposals.get(0);
		proposal.apply(context.getDocument());

		// Accessor class
		String str2= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class RandomName extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, RandomName.class);
			    }
			    private RandomName() {
			    }
			    public static String Test_1;
			}
			""";
		checkContentOfCu("nls file", cu, str2);
	}

	@Test
	public void testRemoveProperty1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		String str= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			}
			""";
		pack1.createCompilationUnit("Accessor.java", str, false, null);

		// property file
		String str1= """
			Test_1=Hello1
			Test_2=Hello2
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str1);

		int offset= str1.indexOf("est_1");
		PropertiesAssistContext context= createAssistContext(file, offset, 0);
		List<ICompletionProposal> proposals= collectAssists(context);

		QuickFixTest.assertNumberOfProposals(proposals, 2);
		QuickFixTest.assertCorrectLabels(proposals);

		ICompletionProposal proposal= QuickFixTest.findProposalByName(REMOVE_KEY, proposals);
		proposal.apply(context.getDocument());

		// property file
		String str2= """
			Test_2=Hello2
			""";
		checkContentOfFile("property file", file, str2);
	}

	@Test
	public void testRemoveProperty2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		String str= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			}
			""";
		pack1.createCompilationUnit("Accessor.java", str, false, null);

		// property file
		String str1= """
			Test_1=Hello1
			Test_2=Hello2
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str1);

		int offset1= str1.indexOf("Test_1");
		int offset2= str1.indexOf("Hello2");
		PropertiesAssistContext context= createAssistContext(file, offset1, offset2 - offset1);
		List<ICompletionProposal> proposals= collectAssists(context);

		QuickFixTest.assertNumberOfProposals(proposals, 2);
		QuickFixTest.assertCorrectLabels(proposals);

		ICompletionProposal proposal= QuickFixTest.findProposalByName(REMOVE_KEYS, proposals);
		proposal.apply(context.getDocument());

		// property file
		String str2= """
			""";
		checkContentOfFile("property file", file, str2);
	}

	@Test
	public void testRemoveProperty3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		String str= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			    public static String Test_2;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Accessor.java", str, false, null);

		// property file
		String str1= """
			Test_1=Hello1
			Test_2=Hello2
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str1);

		int offset= str1.indexOf("est_1");
		PropertiesAssistContext context= createAssistContext(file, offset, 0);
		List<ICompletionProposal> proposals= collectAssists(context);

		QuickFixTest.assertNumberOfProposals(proposals, 2);
		QuickFixTest.assertCorrectLabels(proposals);

		ICompletionProposal proposal= QuickFixTest.findProposalByName(REMOVE_KEY, proposals);
		proposal.apply(context.getDocument());

		// Accessor class
		String str2= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_2;
			}
			""";
		checkContentOfCu("nls file", cu, str2);

		// property file
		String str3= """
			Test_2=Hello2
			""";
		checkContentOfFile("property file", file, str3);
	}

	@Test
	public void testRemoveProperty4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		String str= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			    public static String Test_2;
			    public static String Test_3;
			    public static String Test_4;
			    public static String Test_5;
			    public static String Test_6;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Accessor.java", str, false, null);

		// property file
		String str1= """
			Test_1=Hello1
			Test_2=Hello2
			Test_3=Hello3
			Test_4=Hello4
			Test_5=Hello5
			Test_6=Hello6
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str1);

		int offset1= str1.indexOf("Test_3");
		int offset2= str1.indexOf("Hello4");
		PropertiesAssistContext context= createAssistContext(file, offset1, offset2 - offset1);
		List<ICompletionProposal> proposals= collectAssists(context);

		QuickFixTest.assertNumberOfProposals(proposals, 1);
		QuickFixTest.assertCorrectLabels(proposals);

		ICompletionProposal proposal= QuickFixTest.findProposalByName(REMOVE_KEYS, proposals);
		proposal.apply(context.getDocument());

		// Accessor class
		String str2= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			    public static String Test_2;
			    public static String Test_5;
			    public static String Test_6;
			}
			""";
		checkContentOfCu("nls file", cu, str2);

		// property file
		String str3= """
			Test_1=Hello1
			Test_2=Hello2
			Test_5=Hello5
			Test_6=Hello6
			""";
		checkContentOfFile("property file", file, str3);
	}

	@Test
	public void testNoProposals1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		String str= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			    public static String Test_2;
			}
			""";
		pack1.createCompilationUnit("Accessor.java", str, false, null);

		// property file
		String str1= """
			Test_1=Hello1
			Test_2=Hello2
			   \s
			   \s
			Test_3=Hello3
			Test_4=Hello4
			Test_5=Hello5
			   \s
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str1);

		int offset= str1.indexOf("Test_3");
		PropertiesAssistContext context= createAssistContext(file, offset - 5, 0);
		List<ICompletionProposal> proposals= collectAssists(context);
		QuickFixTest.assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testNoProposals2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		String str= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			    public static String Test_2;
			}
			""";
		pack1.createCompilationUnit("Accessor.java", str, false, null);

		// property file
		String str1= """
			Test_1=Hello1
			Test_2=Hello2
			   \s
			   \s
			Test_3=Hello3
			Test_4=Hello4
			Test_5=Hello5
			   \s
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str1);

		int offset= str1.indexOf("lo2");
		PropertiesAssistContext context= createAssistContext(file, offset + 5, 5);
		List<ICompletionProposal> proposals= collectAssists(context);
		QuickFixTest.assertNumberOfProposals(proposals, 0);
	}

	@Test
	public void testNoProposals3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);

		// Accessor class
		String str= """
			package test;
			import org.eclipse.osgi.util.NLS;
			public class Accessor extends NLS {
			    private static final String BUNDLE_NAME = "test.Accessor";//$NON-NLS-1$
			    static {
			        NLS.initializeMessages(BUNDLE_NAME, Accessor.class);
			    }
			    private Accessor() {
			    }
			    public static String Test_1;
			    public static String Test_2;
			}
			""";
		pack1.createCompilationUnit("Accessor.java", str, false, null);

		// property file
		String str1= """
			Test_1=Hello1
			Test_2=Hello2
			   \s
			   \s
			Test_3=Hello3
			Test_4=Hello4
			Test_5=Hello5
			   \s
			""";
		IFile file= createPropertyFile(pack1, "Accessor.properties", str1);

		int offset= str1.indexOf("lo5");
		PropertiesAssistContext context= createAssistContext(file, offset + 5, 1);
		List<ICompletionProposal> proposals= collectAssists(context);
		QuickFixTest.assertNumberOfProposals(proposals, 0);
	}
}
