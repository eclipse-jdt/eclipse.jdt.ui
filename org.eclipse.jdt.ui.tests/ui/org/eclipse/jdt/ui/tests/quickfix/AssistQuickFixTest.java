/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Sebastian Davids <sdavids@gmx.de> - testInvertEquals1-23
 *     Lukas Hanke <hanke@yatta.de> - Bug 241696 [quick fix] quickfix to iterate over a collection - https://bugs.eclipse.org/bugs/show_bug.cgi?id=241696
 *     Lukas Hanke <hanke@yatta.de> - Bug 430818 [1.8][quick fix] Quick fix for "for loop" is not shown for bare local variable/argument/field - https://bugs.eclipse.org/bugs/show_bug.cgi?id=430818
 *     Mateusz Matela <mateusz.matela@gmail.com> - [formatter] Formatter does not format Java code correctly, especially when max line width is set
 *     Jens Reimann <jens.reimann@ibh-systems.com>, Fabian Pfaff <fabian.pfaff@vogella.com> - Bug 197850: [quick assist] Add import static field/method - https://bugs.eclipse.org/bugs/show_bug.cgi?id=197850
 *     Pierre-Yves B. <pyvesdev@gmail.com> - [inline] Allow inlining of local variable initialized to null. - https://bugs.eclipse.org/93850
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;
import org.eclipse.osgi.util.NLS;

import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.FixMessages;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.correction.CUCorrectionProposal;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.text.correction.NewJUnitTestCaseProposal;
import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessor;
import org.eclipse.jdt.internal.ui.text.correction.proposals.AssignToVariableAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.proposals.RenameRefactoringProposal;

public class AssistQuickFixTest extends QuickFixTest {
	@Rule
    public ProjectTestSetup projectSetup = new ProjectTestSetup();

	private static final String CHANGE_MODIFIER_TO_FINAL= FixMessages.VariableDeclarationFix_changeModifierOfUnknownToFinal_description;
	private static final String EXTRACT_TO_CONSTANT= CorrectionMessages.QuickAssistProcessor_extract_to_constant_description;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");

		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, false);

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);

		Preferences corePrefs= JavaPlugin.getJavaCorePluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_SUFFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, "");

		fJProject1= projectSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, projectSetup.getDefaultClasspath());
	}

	@Test
	public void testAssignToLocal() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        getClass();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("getClass()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private Class<? extends E> class1;
			
			    public void foo() {
			        class1 = getClass();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        Class<? extends E> class1 = getClass();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testAssignToLocal2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    public Vector goo() {
			        return null;
			    }
			    public void foo() {
			        goo().iterator();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("goo().iterator()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    private Iterator iterator;
			    public Vector goo() {
			        return null;
			    }
			    public void foo() {
			        iterator = goo().iterator();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    public Vector goo() {
			        return null;
			    }
			    public void foo() {
			        Iterator iterator = goo().iterator();
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignToLocal3() throws Exception {
		// test prefixes and this qualification

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);
		Preferences corePrefs= JavaPlugin.getJavaCorePluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		corePrefs.setValue(JavaCore.CODEASSIST_LOCAL_PREFIXES, "_");


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			
			    private int fCount;
			
			    public void foo() {
			        System.getSecurityManager();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("System");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			
			    private int fCount;
			    private SecurityManager fSecurityManager;
			
			    public void foo() {
			        this.fSecurityManager = System.getSecurityManager();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			
			    private int fCount;
			
			    public void foo() {
			        SecurityManager _securityManager = System.getSecurityManager();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignToLocal4() throws Exception {
		// test name conflict

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			
			    private int f;
			
			    public void foo() {
			        Math.min(1.0f, 2.0f);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("Math");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			
			    private int f;
			    private float min;
			
			    public void foo() {
			        min = Math.min(1.0f, 2.0f);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			
			    private int f;
			
			    public void foo() {
			        float min = Math.min(1.0f, 2.0f);
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignToLocal5() throws Exception {
		// test prefixes and this qualification on static method

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);
		Preferences corePrefs= JavaPlugin.getJavaCorePluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "fg");
		corePrefs.setValue(JavaCore.CODEASSIST_LOCAL_PREFIXES, "_");


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			
			    private int fCount;
			
			    public static void foo() {
			        System.getSecurityManager();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("System");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			
			    private int fCount;
			    private static SecurityManager fgSecurityManager;
			
			    public static void foo() {
			        E.fgSecurityManager = System.getSecurityManager();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			
			    private int fCount;
			
			    public static void foo() {
			        SecurityManager _securityManager = System.getSecurityManager();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignToLocal6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    static {
			        getClass(); // comment
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("getClass()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private static Class<? extends E> class1;
			
			    static {
			        class1 = getClass(); // comment
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    static {
			        Class<? extends E> class1 = getClass(); // comment
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignToLocal7() throws Exception {
		// test name conflict: name used later

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    public Vector goo() {
			        return null;
			    }
			    public void foo() {
			        goo().iterator();
			        Object iterator= null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("goo().iterator()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    private Iterator iterator2;
			    public Vector goo() {
			        return null;
			    }
			    public void foo() {
			        iterator2 = goo().iterator();
			        Object iterator= null;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Iterator;
			import java.util.Vector;
			public class E {
			    public Vector goo() {
			        return null;
			    }
			    public void foo() {
			        Iterator iterator2 = goo().iterator();
			        Object iterator= null;
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignToLocal8() throws Exception {
		// assign to local of field access

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public class MyLayout {
			        int indent;
			    }
			    public void foo() {
			        new MyLayout().indent;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("new MyLayout().indent;");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		int numberOfProposals= 5;
		assertNumberOfProposals(proposals, numberOfProposals);
		assertCorrectLabels(proposals);

		ArrayList<String> previews= new ArrayList<>();
		ArrayList<String> expecteds= new ArrayList<>();

		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public class MyLayout {\n");
		buf.append("        int indent;\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        int indent = new MyLayout().indent;\n");
		buf.append("    }\n");
		buf.append("}\n");
		addPreviewAndExpected(proposals, buf, expecteds, previews);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int indent;\n");
		buf.append("    public class MyLayout {\n");
		buf.append("        int indent;\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        indent = new MyLayout().indent;\n");
		buf.append("    }\n");
		buf.append("}\n");
		addPreviewAndExpected(proposals, buf, expecteds, previews);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public class MyLayout {\n");
		buf.append("        int indent;\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        MyLayout myLayout = new MyLayout();\n");
		buf.append("        myLayout.indent;\n");
		buf.append("    }\n");
		buf.append("}\n");
		addPreviewAndExpected(proposals, buf, expecteds, previews);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public class MyLayout {\n");
		buf.append("        int indent;\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        MyLayout myLayout = new MyLayout();\n");
		buf.append("        myLayout.indent;\n");
		buf.append("    }\n");
		buf.append("}\n");
		addPreviewAndExpected(proposals, buf, expecteds, previews);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private static final MyLayout MY_LAYOUT = new MyLayout();\n");
		buf.append("    public class MyLayout {\n");
		buf.append("        int indent;\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        MY_LAYOUT.indent;\n");
		buf.append("    }\n");
		buf.append("}\n");
		addPreviewAndExpected(proposals, buf, expecteds, previews);

		assertEqualStringsIgnoreOrder(previews, expecteds);
	}

	@Test
	public void testAssignToLocal9() throws Exception {
		// assign to local of field access

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int[] fField;
			    public void foo() {
			        fField[0];
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String string= "fField[0];";
		int offset= str.indexOf(string);
		AssistContext context= getCorrectionContext(cu, offset, string.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expected= new String[3];

		expected[0]= """
			package test1;
			public class E {
			    private int[] fField;
			    public void foo() {
			        int i = fField[0];
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    private int[] fField;
			    private int i;
			    public void foo() {
			        i = fField[0];
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class E {
			    private int[] fField;
			    public void foo() {
			        extracted();
			    }
			    private void extracted() {
			        fField[0];
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testAssignToLocal10() throws Exception {
		// assign to local with recovered statement

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo() {
			        System.getProperties()
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("System.getProperties()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			
			import java.util.Properties;
			
			public class E {
			    public void foo() {
			        Properties properties = System.getProperties();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.util.Properties;
			
			public class E {
			    private Properties properties;
			
			    public void foo() {
			        properties = System.getProperties();
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignToLocal11() throws Exception {
		// assign to statement in if body with no brackets

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        if (i == 0)
			            System.getProperties();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("System.getProperties()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			
			import java.util.Properties;
			
			public class E {
			    public void foo(int i) {
			        if (i == 0) {
			            Properties properties = System.getProperties();
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.util.Properties;
			
			public class E {
			    private Properties properties;
			
			    public void foo(int i) {
			        if (i == 0)
			            properties = System.getProperties();
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignToLocal12() throws Exception {
		// assign to recovered statement in if body with no brackets

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int i) {
			        if (i == 0)
			           i++
			        else
			            System.getProperties()
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("System.getProperties()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			
			import java.util.Properties;
			
			public class E {
			    public void foo(int i) {
			        if (i == 0)
			           i++
			        else {
			            Properties properties = System.getProperties();
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			
			import java.util.Properties;
			
			public class E {
			    private Properties properties;
			
			    public void foo(int i) {
			        if (i == 0)
			           i++
			        else
			            properties = System.getProperties();
			    }
			}
			""";


		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignToLocal13() throws Exception {
		// assign to local in context that requires fully qualified type, https://bugs.eclipse.org/bugs/show_bug.cgi?id=239735

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class Timer {
			    public static void main(String[] args) {
			        new java.util.Timer();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("Timer.java", str, false, null);

		int offset= str.indexOf("new java.util.Timer()");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		String[] expecteds= new String[5];

		expecteds[0]= """
			package test1;
			public class Timer {
			    public static void main(String[] args) {
			        java.util.Timer timer = new java.util.Timer();
			    }
			}
			""";

		expecteds[1]= """
			package test1;
			public class Timer {
			    private static java.util.Timer timer;
			
			    public static void main(String[] args) {
			        timer = new java.util.Timer();
			    }
			}
			""";

		expecteds[2]= """
			package test1;
			public class Timer {
			    public static void main(String[] args) {
			        java.util.Timer timer = new java.util.Timer();
			    }
			}
			""";

		expecteds[3]= """
			package test1;
			public class Timer {
			    public static void main(String[] args) {
			        java.util.Timer timer = new java.util.Timer();
			    }
			}
			""";

		expecteds[4]= """
			package test1;
			public class Timer {
			    private static final java.util.Timer TIMER = new java.util.Timer();
			
			    public static void main(String[] args) {
			        new java.util.Timer();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expecteds);
	}

	// bug 217984
	@Test
	public void testAssignToLocal14() throws Exception {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str1= """
				package test1;
				import java.util.ArrayList;
				import java.util.List;
				import java.util.RandomAccess;
				
				class Gen<E extends List<String> & RandomAccess> extends ArrayList<E> {
				    void foo() {
				        Gen<?> g = new Gen<>();
				        g.get(0);
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("Gen.java", str1, false, null);

			String str= "g.get(0);";
			AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);
			assertNumberOfProposals(proposals, 5);
			assertCorrectLabels(proposals);

			String expected1= """
				package test1;
				import java.util.ArrayList;
				import java.util.List;
				import java.util.RandomAccess;
				
				class Gen<E extends List<String> & RandomAccess> extends ArrayList<E> {
				    void foo() {
				        Gen<?> g = new Gen<>();
				        List<String> list = g.get(0);
				    }
				}
				""";

			String expected2= """
				package test1;
				import java.util.ArrayList;
				import java.util.List;
				import java.util.RandomAccess;
				
				class Gen<E extends List<String> & RandomAccess> extends ArrayList<E> {
				    private List<String> list;
				
				    void foo() {
				        Gen<?> g = new Gen<>();
				        list = g.get(0);
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });

		}

	// bug 217984
	@Test
	public void testAssignToLocal15() throws Exception {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str1= """
				package test1;
				import java.util.ArrayList;
				import java.util.List;
				import java.util.RandomAccess;
				
				class Gen<E extends List<String> & RandomAccess> extends ArrayList<E> {
				    void foo() {
				        Gen<? extends Cloneable> ge = new Gen<>();
				        ge.get(0);
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("Gen.java", str1, false, null);

			String str= "ge.get(0)";
			AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);
			assertNumberOfProposals(proposals, 7);
			assertCorrectLabels(proposals);

			String expected1= """
				package test1;
				import java.util.ArrayList;
				import java.util.List;
				import java.util.RandomAccess;
				
				class Gen<E extends List<String> & RandomAccess> extends ArrayList<E> {
				    void foo() {
				        Gen<? extends Cloneable> ge = new Gen<>();
				        Cloneable cloneable = ge.get(0);
				    }
				}
				""";

			String expected2= """
				package test1;
				import java.util.ArrayList;
				import java.util.List;
				import java.util.RandomAccess;
				
				class Gen<E extends List<String> & RandomAccess> extends ArrayList<E> {
				    private Cloneable cloneable;
				
				    void foo() {
				        Gen<? extends Cloneable> ge = new Gen<>();
				        cloneable = ge.get(0);
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
		}

	// bug 217984
	@Test
	public void testAssignToLocal16() throws Exception {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str1= """
				package test1;
				import java.util.ArrayList;
				import java.util.List;
				import java.util.RandomAccess;
				import java.util.Vector;
				
				class Gen<E extends List<String> & RandomAccess> extends ArrayList<E> {
				    void foo() {
				        Gen<? super Vector<String>> gs = new Gen<>();
				        gs.get(0);
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("Gen.java", str1, false, null);

			String str= "gs.get(0)";
			AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);
			assertNumberOfProposals(proposals, 7);
			assertCorrectLabels(proposals);

			String expected1= """
				package test1;
				import java.util.ArrayList;
				import java.util.List;
				import java.util.RandomAccess;
				import java.util.Vector;
				
				class Gen<E extends List<String> & RandomAccess> extends ArrayList<E> {
				    void foo() {
				        Gen<? super Vector<String>> gs = new Gen<>();
				        List<String> list = gs.get(0);
				    }
				}
				""";

			String expected2= """
				package test1;
				import java.util.ArrayList;
				import java.util.List;
				import java.util.RandomAccess;
				import java.util.Vector;
				
				class Gen<E extends List<String> & RandomAccess> extends ArrayList<E> {
				    private List<String> list;
				
				    void foo() {
				        Gen<? super Vector<String>> gs = new Gen<>();
				        list = gs.get(0);
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
		}

	// bug 506799
	@Test
	public void testAssignToLocal17() throws Exception {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str1= """
				package test1;
				
				interface WorkItem { }
				enum RebaseWorkItem implements WorkItem {
				    PREPARE, APPLY_COMMIT
				}
				
				public class Snippet {
				    void foo(Class<? extends WorkItem> workItemType) throws Exception {
				        workItemType.getEnumConstants();
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("Snippet.java", str1, false, null);

			String str= "workItemType.getEnumConstants();";
			AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);
			assertNumberOfProposals(proposals, 4);
			assertCorrectLabels(proposals);

			String expected1= """
				package test1;
				
				interface WorkItem { }
				enum RebaseWorkItem implements WorkItem {
				    PREPARE, APPLY_COMMIT
				}
				
				public class Snippet {
				    void foo(Class<? extends WorkItem> workItemType) throws Exception {
				        WorkItem[] enumConstants = workItemType.getEnumConstants();
				    }
				}
				""";

			String expected2= """
				package test1;
				
				interface WorkItem { }
				enum RebaseWorkItem implements WorkItem {
				    PREPARE, APPLY_COMMIT
				}
				
				public class Snippet {
				    private WorkItem[] enumConstants;
				
				    void foo(Class<? extends WorkItem> workItemType) throws Exception {
				        enumConstants = workItemType.getEnumConstants();
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignToLocal18() throws Exception { // https://bugs.eclipse.org/bugs/show_bug.cgi?id=287377
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str1= """
			package p;
			
			public class E {
			
			    private E other;
			    boolean b;
			
			    public void foo(boolean newB) {
			        /*1*/other.b = newB;
			        other.other.b = newB;
			        other.other.other.b = newB;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "/*1*/other";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package p;
			
			public class E {
			
			    private E other;
			    boolean b;
			
			    public void foo(boolean newB) {
			        /*1*/E other2 = other;
			        other2.b = newB;
			        other2.other.b = newB;
			        other2.other.other.b = newB;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testAssignParamToField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public  E(int count) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "count";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    private int count;
			
			    public  E(int count) {
			        this.count = count;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1 });
	}

	@Test
	public void testAssignParamToField2() throws Exception {
		Preferences corePrefs= JavaPlugin.getJavaCorePluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    public  E(int count, Vector vec[]) {
			        super();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "vec";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			import java.util.Vector;
			public class E {
			    private Vector[] fVec;
			
			    public  E(int count, Vector vec[]) {
			        super();
			        fVec = vec;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1 });
	}

	@Test
	public void testAssignParamToField3() throws Exception {
		Preferences corePrefs= JavaPlugin.getJavaCorePluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "fg");

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E {
			    private int fgVec;
			
			    public static void foo(int count, Vector vec[]) {
			        count++;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "vec";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			import java.util.Vector;
			public class E {
			    private int fgVec;
			    private static Vector[] fgVec2;
			
			    public static void foo(int count, Vector vec[]) {
			        E.fgVec2 = vec;
			        count++;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1 });
	}

	@Test
	public void testAssignParamToField4() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private long count;
			
			    public void foo(int count) {
			        count++;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("int count");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private long count;
			    private int count2;
			
			    public void foo(int count) {
			        this.count2 = count;
			        count++;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    private long count;
			
			    public void foo(int count) {
			        this.count = count;
			        count++;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignParamToField5() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int p1;
			
			    public void foo(int p1, int p2) {
			        this.p1 = p1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("int p2");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private int p1;
			    private int p2;
			
			    public void foo(int p1, int p2) {
			        this.p1 = p1;
			        this.p2 = p2;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    private int p1;
			
			    public void foo(int p1, int p2) {
			        this.p1 = p1;
			        this.p1 = p2;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignParamToField6() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private Float p1;
			    private Number p2;
			
			    public void foo(Float p1, Integer p2) {
			        this.p1 = p1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("Integer p2");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private Float p1;
			    private Number p2;
			
			    public void foo(Float p1, Integer p2) {
			        this.p1 = p1;
			        this.p2 = p2;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    private Float p1;
			    private Number p2;
			    private Integer p22;
			
			    public void foo(Float p1, Integer p2) {
			        this.p1 = p1;
			        this.p22 = p2;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testBug538832() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			
			    public void foo(Float p1, Integer p2) {
			    }
			    private Float p1;
			    private Number p2;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("Float p1");
		AssistContext context= getCorrectionContext(cu, offset, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			
			    public void foo(Float p1, Integer p2) {
			        this.p1 = p1;
			    }
			    private Float p1;
			    private Number p2;
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			
			    private Float p12;
			    public void foo(Float p1, Integer p2) {
			        this.p12 = p1;
			    }
			    private Float p1;
			    private Number p2;
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignAllParamsToFields1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public E(int count, long size, boolean state) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "count";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private int count;
			
			    public E(int count, long size, boolean state) {
			        this.count = count;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    private int count;
			    private long size;
			    private boolean state;
			
			    public E(int count, long size, boolean state) {
			        this.count = count;
			        this.size = size;
			        this.state = state;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignAllParamsToFields2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int count, long size, boolean state) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "count";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    private int count;
			
			    public void foo(int count, long size, boolean state) {
			        this.count = count;
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    private int count;
			    private long size;
			    private boolean state;
			
			    public void foo(int count, long size, boolean state) {
			        this.count = count;
			        this.size = size;
			        this.state = state;
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testAssignParamToFieldInGeneric() throws Exception {
		Preferences corePrefs= JavaPlugin.getJavaCorePluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Vector;
			public class E<T> {
			    public  E(int count, Vector<String>[] vec) {
			        super();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "vec";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			import java.util.Vector;
			public class E<T> {
			    private Vector<String>[] fVec;
			
			    public  E(int count, Vector<String>[] vec) {
			        super();
			        fVec = vec;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1 });
	}

	@Test
	public void testAssignToLocal2CursorAtEnd() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import java.util.Vector;
			public class E {
			    public Vector goo() {
			        return null;
			    }
			    public void foo() {
			        goo().toArray();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "goo().toArray();";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.util.Vector;
			public class E {
			    private Object[] array;
			    public Vector goo() {
			        return null;
			    }
			    public void foo() {
			        array = goo().toArray();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.util.Vector;
			public class E {
			    public Vector goo() {
			        return null;
			    }
			    public void foo() {
			        Object[] array = goo().toArray();
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	@Test
	public void testExtractToLocalVariable1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int d = a + b;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "a + b";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int i = a + b;
			        int d = i;
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int i = a + b;
			        int d = i;
			    }
			}
			""";

		String ex3= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int d = extracted(a, b);
			    }
			
			    private int extracted(int a, int b) {
			        return a + b;
			    }
			}
			""";

		String ex4= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int d = (a + b);
			    }
			}
			""";

		String ex5= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int d = b + a;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2, ex3, ex4, ex5 });
	}

	@Test
	public void testExtractToLocalVariable2() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=276467
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int c = 1;
			        int d = a + b + c;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "b + c";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int c = 1;
			        int i = b + c;
			        int d = a + i;
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int c = 1;
			        int i = b + c;
			        int d = a + i;
			    }
			}
			""";

		String ex3= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int c = 1;
			        int d = c + a + b;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2, ex3 });
	}

	@Test
	public void testExtractToLocalVariable3() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=276467
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int c = 1;
			        int d = a + b + c;
			        int e = a + b + c;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "b + c";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int c = 1;
			        int i = b + c;
			        int d = a + i;
			        int e = a + i;
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int c = 1;
			        int i = b + c;
			        int d = a + i;
			        int e = a + b + c;
			    }
			}
			""";

		String ex3= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int c = 1;
			        int d = c + a + b;
			        int e = a + b + c;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2, ex3 });
	}

	@Test
	public void testExtractToLocalVariable4() throws Exception {
		//bug 457547
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "1";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String ex1= """
			package test;
			public class E {
			    public E() {
			        int i = 1;
			        int a = i;
			        int b = i;
			    }
			}
			""";

		String ex2= """
			package test;
			public class E {
			    public E() {
			        int i = 1;
			        int a = i;
			        int b = 1;
			    }
			}
			""";

		String ex3= """
			package test;
			public class E {
			    private static final int _1 = 1;
			
			    public E() {
			        int a = _1;
			        int b = 1;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2, ex3 });
	}
	@Test
	public void testExtractToLocalVariable5() throws Exception { //https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1176
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			    Object parent;
			    Object elementName;
			   \s
			    public Object getElementName() {
			        return elementName;
			    }
			
			    private class UtilClass {
			        public static int combineHashCodes(int a, int b) {
			            return a + b;
			        }
			    }
			
			    @Override
			    public int hashCode() {
			        int k = this.parent == null ? super.hashCode() :
			        UtilClass.combineHashCodes(getElementName().hashCode(), this.parent.hashCode());
			        return k;
			    }
			   \s
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "UtilClass.combineHashCodes(getElementName().hashCode(), this.parent.hashCode())";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertProposalDoesNotExist(proposals, EXTRACT_TO_CONSTANT);
	}

	@Test
	public void testExtractAnonymousToLocalVariable1() throws Exception { //https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/1063
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			public class E {
			
			    public interface K {
			        String getName();
			        String getValue();
			    }
			
			    public void foo(K k) {
			        System.out.println(k.getName());
			        System.out.println(k.getValue());
			    }
			
			    public void foo2() {
			        foo(new K() {
			            @Override
			            public String getName() {
			                return "abc";
			            }
			            @Override
			            public String getValue() {
			                return "def";
			            }
			        });
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "\"abc\"";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		String ex1= """
			package test;
			public class E {
			
			    public interface K {
			        String getName();
			        String getValue();
			    }
			
			    public void foo(K k) {
			        System.out.println(k.getName());
			        System.out.println(k.getValue());
			    }
			
			    public void foo2() {
			        K k = new K() {
			            @Override
			            public String getName() {
			                return "abc";
			            }
			            @Override
			            public String getValue() {
			                return "def";
			            }
			        };
			        foo(k);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1 });
	}

	@Test
	public void testExtractToConstant1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			
			class E {
			    public final static E instance= new E();
			   \s
			    int s;
			
			    final static int f() {
			        System.out.println(E.instance.s + 1);
			        return 1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "E.instance.s + 1";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, EXTRACT_TO_CONSTANT);
	}

	@Test
	public void testExtractToConstant2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		String str= """
			package test;
			
			class E {
			    public final static E instance= new E();
			
			    static final int t = 5;
			
			    int f1() {
			        return 23 * E.t; \s
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "23 * E.t";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String ex1= """
			package test;
			
			class E {
			    public final static E instance= new E();
			
			    static final int t = 5;
			
			    private static final int INT = 23 * E.t;
			
			    int f1() {
			        return INT; \s
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1 });
	}

	@Test
	public void testExtractToMethod1() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=41302
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int d = a + b;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "a + b";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int d = extracted(a, b);
			    }
			
			    private int extracted(int a, int b) {
			        return a + b;
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int i = a + b;
			        int d = i;
			    }
			}
			""";

		String ex3= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int i = a + b;
			        int d = i;
			    }
			}
			""";

		String ex4= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int d = (a + b);
			    }
			}
			""";

		String ex5= """
			package test1;
			public class E {
			    public E() {
			        int a = 1;
			        int b = 1;
			        int d = b + a;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2, ex3, ex4, ex5 });
	}

	@Test
	public void testExtractToMethod2() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=41302
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        int a = 1;
			        int b = 1;
			        int d = a + b;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("int b = 1;");
		int offset2= str.indexOf("a + b;") + 6;
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    void foo() {
			        int a = 1;
			        extracted(a);
			    }
			
			    private void extracted(int a) {
			        int b = 1;
			        int d = a + b;
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    void foo() {
			        int a = 1;
			        final int b = 1;
			        final int d = a + b;
			    }
			}
			""";

		String ex3= null; // Wrap in buf.append() (to clipboard)

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2, ex3 });
	}

	@Test
	public void testExtractToMethod3() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=41302
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        int a = 1;
			        int b = 1;
			        int d = a + b;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("int a = 1;");
		int offset2= str.indexOf("a + b;") + 6;
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    void foo() {
			        extracted();
			    }
			
			    private void extracted() {
			        int a = 1;
			        int b = 1;
			        int d = a + b;
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    void foo() {
			        final int a = 1;
			        final int b = 1;
			        final int d = a + b;
			    }
			}
			""";

		String ex3= null; // Wrap in buf.append() (to clipboard)

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2, ex3 });
	}

	@Test
	public void testExtractToMethod4() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=41302
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo() {
			        int i = 0;
			        for (; true;)
			            i++;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "i++;";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());

		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    void foo() {
			        int i = 0;
			        for (; true;)
			            i = extracted(i);
			    }
			
			    private int extracted(int i) {
			        i++;
			        return i;
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    void foo() {
			        int i = 0;
			        for (; true;) {
			            int j = i++;
			        }
			    }
			}
			""";

		String ex3= """
			package test1;
			public class E {
			    private int j;
			
			    void foo() {
			        int i = 0;
			        for (; true;)
			            j = i++;
			    }
			}
			""";

		String ex4= """
			package test1;
			public class E {
			    void foo() {
			        int i = 0;
			        for (; true;) {
			            i++;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2, ex3, ex4 });
	}

	@Test
	public void testReplaceCatchClauseWithThrowsWithFinally() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void foo() {
			        try {
			            goo();
			        } catch (IOException e) {
			        } finally {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "(IOException e)";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void foo() throws IOException {
			        try {
			            goo();
			        } finally {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void foo() {
			        try {
			            goo();
			        } finally {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

	}

	@Test
	public void testReplaceSingleCatchClauseWithThrows() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void foo() {
			        try {
			            goo();
			        } catch (IOException e) {
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "(IOException e)";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void foo() throws IOException {
			        goo();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void foo() {
			        goo();
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			import java.io.IOException;
			public class E {
			    public void foo() {
			        try {
			            goo();
			        } catch (IOException e) {
			        } finally {
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });

	}

	@Test
	public void testUnwrapForLoop() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        for (int i= 0; i < 3; i++) {
			            goo();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "for";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        goo();
			    }
			}
			""";
		assertEqualString(preview, str2);

		proposal= (CUCorrectionProposal)proposals.get(1);
		preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			public class E {
			    public void foo() {
			        for (int i= 0; i < 3; i++)
			            goo();
			    }
			}
			""";
		assertEqualString(preview, str3);
	}

	@Test
	public void testUnwrapDoStatement() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        do {
			            goo();
			            goo();
			            goo();
			        } while (true);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "do";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        goo();
			        goo();
			        goo();
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testUnwrapWhileLoop2Statements() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        while (true) {
			            goo();
			            System.out.println();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "while";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        goo();
			        System.out.println();
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testUnwrapIfStatement() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (1+ 3 == 6) {
			            StringBuffer buf= new StringBuffer();
			            buf.append(1);
			            buf.append(2);
			            buf.append(3);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "if";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        StringBuffer buf= new StringBuffer();
			        buf.append(1);
			        buf.append(2);
			        buf.append(3);
			    }
			}
			""";

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        if (1+ 3 == 6) {
			            StringBuffer buf= new StringBuffer();
			            buf.append(1);
			            buf.append(2);
			            buf.append(3);
			        } else {
			        }
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(2);
		String preview3= getPreviewContent(proposal);

		String expected3= """
			package test1;
			public class E {
			    public void foo() {
			        if (1+ 3 != 6)
			            return;
			        StringBuffer buf= new StringBuffer();
			        buf.append(1);
			        buf.append(2);
			        buf.append(3);
			    }
			}
			""";

		proposal= (CUCorrectionProposal)proposals.get(3);
		String preview4= getPreviewContent(proposal);

		String expected4= """
			package test1;
			public class E {
			    public void foo() {
			        switch (6) {
			            case 1+ 3 :
			                StringBuffer buf= new StringBuffer();
			                buf.append(1);
			                buf.append(2);
			                buf.append(3);
			                break;
			        }
			    }
			}
			""";

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3, preview4 }, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testUnwrapTryStatement() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        try {
			            StringBuffer buf= new StringBuffer();
			            buf.append(1);
			            buf.append(2);
			            buf.append(3);
			        } finally {
			            return;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "try";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        StringBuffer buf= new StringBuffer();
			        buf.append(1);
			        buf.append(2);
			        buf.append(3);
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testUnwrapAnonymous() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        Runnable run= new Runnable() {
			            public void run() {\s
			                throw new NullPointerException();
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "};";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        throw new NullPointerException();
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testUnwrapBlock() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        {
			            {\s
			                throw new NullPointerException();
			            }//comment
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "}//comment";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        {
			            throw new NullPointerException();
			        }
			    }
			}
			""";
		assertEqualString(preview, str2);
	}


	@Test
	public void testUnwrapMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public int foo() {
			        return Math.abs(9+ 8);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "Math.abs(9+ 8)";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public int foo() {
			        return 9+ 8;
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public int foo() {
			        int abs = Math.abs(9+ 8);
			        return abs;
			    }
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    public int foo() {
			        int abs = Math.abs(9+ 8);
			        return abs;
			    }
			}
			""";

		String expected4= """
			package test1;
			public class E {
			    private static final int ABS = Math.abs(9+ 8);
			
			    public int foo() {
			        return ABS;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testSplitDeclaration1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        int i = 9;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "=";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        int i;
			        i = 9;
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testSplitDeclaration2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        for (int i = 0; i < 9; i++) {
			       }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "=";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        int i;
			        for (i = 0; i < 9; i++) {
			       }
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testSplitDeclaration3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        final int i[] = null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "i[]";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    public void foo() {
			        final int i[];
			        i = null;
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    public void foo() {
			    }
			}
			""";

		String ex3= """
			package test1;
			public class E {
			    private int is[];
			
			    public void foo() {
			        is = null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2, ex3 });
	}

	@Test
	public void testSplitDeclaration4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package e;
			public class Test {
			    public void test() {
			        String[] test = new String[0];
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "=";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package e;
			public class Test {
			    public void test() {
			        String[] test;
			        test = new String[0];
			    }
			}""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testSplitDeclaration5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package e;
			public class Test {
			    public void test() {
			        String[] test = { null };
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "=";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package e;
			public class Test {
			    public void test() {
			        String[] test;
			        test = new String[]{ null };
			    }
			}""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testSplitDeclaration6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package e;
			public class Test {
			    public void test() {
			        String[] test = { "a" };
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "=";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package e;
			public class Test {
			    public void test() {
			        String[] test;
			        test = new String[]{ "a" };
			    }
			}""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testSplitDeclaration7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package e;
			public class Test {
			    public void test() {
			        String[] test = x;
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "=";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package e;
			public class Test {
			    public void test() {
			        String[] test;
			        test = x;
			    }
			}""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testSplitDeclaration8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        for (long i = 2, j = 1; i < j; i++) {
			       }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "=";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        long i, j;
			        for (i = 2, j = 1; i < j; i++) {
			       }
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testSplitDeclaration9() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        for (/*1*/long i = 2/*2*/, j/*3*/; i < 1; i++) {
			       }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "=";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        /*1*/long i, j;
			        for (i = 2/*2*/, j/*3*/ = 0; i < 1; i++) {
			       }
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testSplitDeclaration10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        for (/*1*/boolean i = true/*2*/, j/*3*/; i != true; ) {
			       }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "=";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        /*1*/boolean i, j;
			        for (i = true/*2*/, j/*3*/ = false; i != true; ) {
			       }
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testSplitDeclaration11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        for (/*1*/Object i = null/*2*/, j/*3*/; i == null; ) {
			       }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "=";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo() {
			        /*1*/Object i, j;
			        for (i = null/*2*/, j/*3*/ = null; i == null; ) {
			       }
			    }
			}
			""";
		assertEqualString(preview, str2);
	}

	@Test
	public void testJoinDeclaration1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        int var[];
			        foo();
			        var = null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "var[]";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    public void foo() {
			        int var[] = null;
			        foo();
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    private int vars[];
			
			    public void foo() {
			        foo();
			        vars = null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2 });
	}


	@Test
	public void testJoinDeclaration2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        int var[];
			        foo();
			        var = null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "var = ";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        foo();
			        int var[] = null;
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    private int vars[];
			
			    public void foo() {
			        foo();
			        vars = null;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testJoinDeclaration3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        int var[] = null;
			        foo();
			        var = new int[10];
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "var[]";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    public void foo() {
			        int var[] = new int[10];
			        foo();
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    public void foo() {
			        int var[];
			        var = null;
			        foo();
			        var = new int[10];
			    }
			}
			""";

		String ex3= """
			package test1;
			public class E {
			    private int vars[];
			
			    public void foo() {
			        vars = null;
			        foo();
			        vars = new int[10];
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2, ex3 });
	}

	@Test
	public void testJoinDeclaration4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        // 1;
			       \s
			        String message;
			       \s
			        // 2;
			       \s
			        message = "";
			       \s
			        // 3;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "message;";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    public void foo() {
			        // 1;
			       \s
			        String message = "";
			       \s
			        // 2;
			
			       \s
			        // 3;
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    private String message;
			
			    public void foo() {
			        // 1;
			       \s
			       \s
			       \s
			        // 2;
			       \s
			        message = "";
			       \s
			        // 3;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2 });
	}

	@Test
	public void testJoinDeclaration5() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        // 1;
			       \s
			        String message;
			       \s
			        // 2;
			       \s
			        message = "";
			       \s
			        // 3;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "message =";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    public void foo() {
			        // 1;
			       \s
			       \s
			       \s
			        // 2;
			       \s
			        String message = "";
			       \s
			        // 3;
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    private String message;
			
			    public void foo() {
			        // 1;
			       \s
			       \s
			       \s
			        // 2;
			       \s
			        message = "";
			       \s
			        // 3;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2 });
	}

	@Test
	public void testJoinDeclaration6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo(int x) {
			        String var;
			        if (x == 1)
			           var = "abc";
			        else
			           var = "";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "var";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    public void foo(int x) {
			        String var = x == 1 ? "abc" : "";
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    private String var;
			
			    public void foo(int x) {
			        if (x == 1)
			           var = "abc";
			        else
			           var = "";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2 });
	}

	@Test
	public void testJoinDeclaration7() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo(int x) {
			        String var;
			        if (x == 1)
			           var = "abc";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "var";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    private String var;
			
			    public void foo(int x) {
			        if (x == 1)
			           var = "abc";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1 });
	}

	@Test
	public void testJoinDeclaration8() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo(int x) {
			        String var;
			        if (x == 1) {
			           var = "abc";
			        } else {
			           var = "def";
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "var";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    public void foo(int x) {
			        String var = x == 1 ? "abc" : "def";
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    private String var;
			
			    public void foo(int x) {
			        if (x == 1) {
			           var = "abc";
			        } else {
			           var = "def";
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2 });
	}

	@Test
	public void testJoinDeclaration9() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo(int x) {
			        String var;
			        if (x == 1) {
			           var = "abc";
			        } else {
			           var = "def";
			           foo(3);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "var";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    private String var;
			
			    public void foo(int x) {
			        if (x == 1) {
			           var = "abc";
			        } else {
			           var = "def";
			           foo(3);
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1 });
	}

	@Test
	public void testJoinDeclaration10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo(int x) {
			        String var;
			        switch (x) {
			        case 1:
			           var = "abc";
			           break;
			        default:
			           var = "def";
			           break;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "var";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String ex1= """
			package test1;
			public class E {
			    private String var;
			
			    public void foo(int x) {
			        switch (x) {
			        case 1:
			           var = "abc";
			           break;
			        default:
			           var = "def";
			           break;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1 });
	}

	private static final Class<?>[] FILTER_EQ= { LinkedNamesAssistProposal.class, RenameRefactoringProposal.class, AssignToVariableAssistProposal.class, NewJUnitTestCaseProposal.class };

	@Test
	public void testInvertEquals1() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    public void foo() {
			        "a".equals("b");
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        String str2= """
			package test1;
			public class E {
			    public void foo() {
			        "b".equals("a");
			    }
			}
			""";
        assertEqualString(preview, str2);

        cu= pack1.createCompilationUnit("E.java", str2, true, null);
        context= getCorrectionContext(cu, str2.indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        String str3= """
			package test1;
			public class E {
			    public void foo() {
			        "a".equals("b");
			    }
			}
			""";
        assertEqualString(preview, str3);
    }

	@Test
    public void testInvertEquals2() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    public void foo() {
			        String s= "a";
			        s.equals("a");
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "s.equals(\"a\")";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), str.length());
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 4);
        assertCorrectLabels(proposals);

        String ex1= """
			package test1;
			public class E {
			    public void foo() {
			        String s= "a";
			        "a".equals(s);
			    }
			}
			""";

		String ex2= """
			package test1;
			public class E {
			    public void foo() {
			        String s= "a";
			        boolean equals = s.equals("a");
			    }
			}
			""";

		String ex3= """
			package test1;
			public class E {
			    public void foo() {
			        String s= "a";
			        boolean equals = s.equals("a");
			    }
			}
			""";

		String ex4= """
			package test1;
			public class E {
			    public void foo() {
			        String s= "a";
			        extracted(s);
			    }
			
			    private boolean extracted(String s) {
			        return s.equals("a");
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2, ex3, ex4 });

		cu= pack1.createCompilationUnit("E.java", ex1, true, null);
        str= "\"a\".equals(s)";
		context= getCorrectionContext(cu, ex1.indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

		assertNumberOfProposals(proposals, 5);
        assertCorrectLabels(proposals);

        ex1= """
			package test1;
			public class E {
			    public void foo() {
			        String s= "a";
			        s.equals("a");
			    }
			}
			""";

		ex2= """
			package test1;
			public class E {
			    public void foo() {
			        String string = "a";
			        String s= string;
			        string.equals(s);
			    }
			}
			""";

		ex3= """
			package test1;
			public class E {
			    public void foo() {
			        String s= "a";
			        String string = "a";
			        string.equals(s);
			    }
			}
			""";

		ex4= """
			package test1;
			public class E {
			    private static final String A = "a";
			
			    public void foo() {
			        String s= A;
			        A.equals(s);
			    }
			}
			""";

		String ex5= """
			package test1;
			public class E {
			    public void foo() {
			        String s= "a";
			        "A".equals(s);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2, ex3, ex4, ex5 });
    }

	@Test
    public void testInvertEquals3() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    private String a= "a";
			    private String b= "b";
			    public void foo() {
			        a.equals(b);
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), str.length());
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        String str2= """
			package test1;
			public class E {
			    private String a= "a";
			    private String b= "b";
			    public void foo() {
			        b.equals(a);
			    }
			}
			""";
        assertEqualString(preview, str2);

        cu= pack1.createCompilationUnit("E.java", str2, true, null);
        context= getCorrectionContext(cu, str2.indexOf(str), str.length());
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        String str3= """
			package test1;
			public class E {
			    private String a= "a";
			    private String b= "b";
			    public void foo() {
			        a.equals(b);
			    }
			}
			""";
        assertEqualString(preview, str3);
    }

	@Test
    public void testInvertEquals4() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			class S {
			    protected String sup= "a";
			}
			public class E extends S {
			    private String a= "a";
			    public void foo() {
			        sup.equals(this.a);
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        String str2= """
			package test1;
			class S {
			    protected String sup= "a";
			}
			public class E extends S {
			    private String a= "a";
			    public void foo() {
			        this.a.equals(sup);
			    }
			}
			""";
        assertEqualString(preview, str2);

        cu= pack1.createCompilationUnit("E.java", str2, true, null);
        context= getCorrectionContext(cu, str2.indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        String str3= """
			package test1;
			class S {
			    protected String sup= "a";
			}
			public class E extends S {
			    private String a= "a";
			    public void foo() {
			        sup.equals(this.a);
			    }
			}
			""";
        assertEqualString(preview, str3);
    }

	@Test
    public void testInvertEquals5() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			class A {
			    static String A= "a";
			}
			public class E {
			    public void foo() {
			        "a".equals(A.A);
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        String str2= """
			package test1;
			class A {
			    static String A= "a";
			}
			public class E {
			    public void foo() {
			        A.A.equals("a");
			    }
			}
			""";
        assertEqualString(preview, str2);

        cu= pack1.createCompilationUnit("E.java", str2, true, null);
        context= getCorrectionContext(cu, str2.indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        String str3= """
			package test1;
			class A {
			    static String A= "a";
			}
			public class E {
			    public void foo() {
			        "a".equals(A.A);
			    }
			}
			""";
        assertEqualString(preview, str3);
    }

	@Test
    public void testInvertEquals6() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			class A {
			    static String get() {
			        return "a";
			    }
			}
			public class E {
			    public void foo() {
			        "a".equals(A.get());
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        cu= pack1.createCompilationUnit("E.java", str1, true, null);
        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        String str2= """
			package test1;
			class A {
			    static String get() {
			        return "a";
			    }
			}
			public class E {
			    public void foo() {
			        A.get().equals("a");
			    }
			}
			""";
        assertEqualString(preview, str2);

        cu= pack1.createCompilationUnit("E.java", str2, true, null);
        context= getCorrectionContext(cu, str2.indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        String str3= """
			package test1;
			class A {
			    static String get() {
			        return "a";
			    }
			}
			public class E {
			    public void foo() {
			        "a".equals(A.get());
			    }
			}
			""";
        assertEqualString(preview, str3);
    }

	@Test
    public void testInvertEquals7() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    public void foo() {
			        "a".getClass().equals(String.class);
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        String str2= """
			package test1;
			public class E {
			    public void foo() {
			        String.class.equals("a".getClass());
			    }
			}
			""";
        assertEqualString(preview, str2);

        cu= pack1.createCompilationUnit("E.java", str2, true, null);
        context= getCorrectionContext(cu, str2.indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        String str3= """
			package test1;
			public class E {
			    public void foo() {
			        "a".getClass().equals(String.class);
			    }
			}
			""";
        assertEqualString(preview, str3);
    }

	@Test
    public void testInvertEquals8() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    public void foo() {
			        boolean x = false && "a".equals(get());
			    }
			    String get() {
			        return "a";
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

		assertNumberOfProposals(proposals, 2);
        assertCorrectLabels(proposals);

        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        String ex1= """
			package test1;
			public class E {
			    public void foo() {
			        boolean x = false && get().equals("a");
			    }
			    String get() {
			        return "a";
			    }
			}
			""";
		assertEqualString(preview, ex1);

		proposal= (CUCorrectionProposal)proposals.get(1);
		preview= getPreviewContent(proposal);

		String ex2= """
			package test1;
			public class E {
			    public void foo() {
			        boolean x = (false && "a".equals(get()));
			    }
			    String get() {
			        return "a";
			    }
			}
			""";
		assertEqualString(preview, ex2);

		cu= pack1.createCompilationUnit("E.java", ex1, true, null);
		context= getCorrectionContext(cu, ex1.indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

		assertNumberOfProposals(proposals, 2);
        assertCorrectLabels(proposals);

        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        String str2= """
			package test1;
			public class E {
			    public void foo() {
			        boolean x = false && "a".equals(get());
			    }
			    String get() {
			        return "a";
			    }
			}
			""";
        assertEqualString(preview, str2);

		proposal= (CUCorrectionProposal)proposals.get(1);
		preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			public class E {
			    public void foo() {
			        boolean x = (false && get().equals("a"));
			    }
			    String get() {
			        return "a";
			    }
			}
			""";
		assertEqualString(preview, str3);
    }

	@Test
    public void testInvertEquals9() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    public void foo() {
			        equals(new E());
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        String str2= """
			package test1;
			public class E {
			    public void foo() {
			        new E().equals(this);
			    }
			}
			""";
        assertEqualString(preview, str2);

        cu= pack1.createCompilationUnit("E.java", str2, true, null);
        context= getCorrectionContext(cu, str2.indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        String str3= """
			package test1;
			public class E {
			    public void foo() {
			        equals(new E());
			    }
			}
			""";
        assertEqualString(preview, str3);
    }

	@Test
    public void testInvertEquals10() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    public void foo() {
			        "a".equals(null);
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }

	@Test
    public void testInvertEquals11() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    boolean equals(Object o, boolean a) {
			        return false;
			    }
			    public void foo() {
			        new E().equals("a", false);
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "E().equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);

        context= getCorrectionContext(cu, str1.lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }

	@Test
    public void testInvertEquals12() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    boolean equals(boolean b) {
			        return false;
			    }
			    public void foo() {
			        new E().equals(false);
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "E().equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);

        context= getCorrectionContext(cu, str1.lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }

	@Test
    public void testInvertEquals13() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    boolean equals(boolean b) {
			        return false;
			    }
			    public void foo() {
			        new E().equals(true ? true : false);
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "E().equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);

        context= getCorrectionContext(cu, str1.lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }

	@Test
    public void testInvertEquals14() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			class Super {
			    protected boolean sBool= false;
			}
			public class E extends Super {
			    boolean equals(boolean b) {
			        return false;
			    }
			    public void foo() {
			        new E().equals(sBool);
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "E().equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);

        context= getCorrectionContext(cu, str1.lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }

	@Test
    public void testInvertEquals15() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    boolean equals(int i) {
			        return false;
			    }
			    public void foo() {
			        new E().equals(1 + 1);
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "E().equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);

        context= getCorrectionContext(cu, str1.lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }

	@Test
    public void testInvertEquals16() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    boolean equals(int i) {
			        return false;
			    }
			    public void foo() {
			        int i= 1;
			        new E().equals(i + i);
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "E().equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);

        context= getCorrectionContext(cu, str1.lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }

	@Test
    public void testInvertEquals17() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    public void foo() {
			       "a".equals(null);
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }

	@Test
    public void testInvertEquals18() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    public boolean equals(Object o) {
			       return super.equals(o);
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals(o)";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);

        context= getCorrectionContext(cu, str1.lastIndexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 0);
        assertCorrectLabels(proposals);
    }

	@Test
    public void testInvertEquals19() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    private String a= "a";
			    public void foo() {
			        a.equals((Object) "a");
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        String str2= """
			package test1;
			public class E {
			    private String a= "a";
			    public void foo() {
			        ((Object) "a").equals(a);
			    }
			}
			""";
        assertEqualString(preview, str2);

        cu= pack1.createCompilationUnit("E.java", str2, true, null);
        context= getCorrectionContext(cu, str2.indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        String str3= """
			package test1;
			public class E {
			    private String a= "a";
			    public void foo() {
			        a.equals((Object) "a");
			    }
			}
			""";
        assertEqualString(preview, str3);
    }

	@Test
    public void testInvertEquals20() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    public void foo() {
			        String s= null;
			        "a".equals(s = "a");
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        String str2= """
			package test1;
			public class E {
			    public void foo() {
			        String s= null;
			        (s = "a").equals("a");
			    }
			}
			""";
        assertEqualString(preview, str2);

        cu= pack1.createCompilationUnit("E.java", str2, true, null);
        context= getCorrectionContext(cu, str2.indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        String str3= """
			package test1;
			public class E {
			    public void foo() {
			        String s= null;
			        "a".equals(s = "a");
			    }
			}
			""";
        assertEqualString(preview, str3);
    }

	@Test
    public void testInvertEquals21() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    public void foo() {
			        "aaa".equals("a" + "a" + "a");
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        String str2= """
			package test1;
			public class E {
			    public void foo() {
			        ("a" + "a" + "a").equals("aaa");
			    }
			}
			""";
        assertEqualString(preview, str2);

        cu= pack1.createCompilationUnit("E.java", str2, true, null);
        context= getCorrectionContext(cu, str2.indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        String str3= """
			package test1;
			public class E {
			    public void foo() {
			        "aaa".equals("a" + "a" + "a");
			    }
			}
			""";
        assertEqualString(preview, str3);
    }

	@Test
    public void testInvertEquals22() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    public void foo() {
			        "a".equals(true ? "a" : "b");
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        String str2= """
			package test1;
			public class E {
			    public void foo() {
			        (true ? "a" : "b").equals("a");
			    }
			}
			""";
        assertEqualString(preview, str2);

        cu= pack1.createCompilationUnit("E.java", str2, true, null);
        context= getCorrectionContext(cu, str2.indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        String str3= """
			package test1;
			public class E {
			    public void foo() {
			        "a".equals(true ? "a" : "b");
			    }
			}
			""";
        assertEqualString(preview, str3);
    }

	@Test
    public void testInvertEquals23() throws Exception {
        IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
        String str1= """
			package test1;
			public class E {
			    public void foo() {
			        "a".equals((("a")));
			    }
			}
			""";
        ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

        String str= "equals";
        AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
        List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
        String preview= getPreviewContent(proposal);

        String str2= """
			package test1;
			public class E {
			    public void foo() {
			        (("a")).equals("a");
			    }
			}
			""";
        assertEqualString(preview, str2);

        cu= pack1.createCompilationUnit("E.java", str2, true, null);
        context= getCorrectionContext(cu, str2.indexOf(str), 0);
        proposals= collectAssists(context, FILTER_EQ);

        assertNumberOfProposals(proposals, 1);
        assertCorrectLabels(proposals);

        proposal= (CUCorrectionProposal) proposals.get(0);
        preview= getPreviewContent(proposal);

        String str3= """
			package test1;
			public class E {
			    public void foo() {
			        "a".equals("a");
			    }
			}
			""";
        assertEqualString(preview, str3);
    }

	@Test
	public void testInvertEquals24() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=385389
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo(Enum e) {
			        e.equals(Enum.e1);
			    }
			}
			enum Enum {
			    e1, e2;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "equals";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, FILTER_EQ);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal)proposals.get(0);
		String preview= getPreviewContent(proposal);

		String str2= """
			package test1;
			public class E {
			    public void foo(Enum e) {
			        Enum.e1.equals(e);
			    }
			}
			enum Enum {
			    e1, e2;
			}
			""";
		assertEqualString(preview, str2);

		cu= pack1.createCompilationUnit("E.java", str2, true, null);
		context= getCorrectionContext(cu, str2.indexOf(str), 0);
		proposals= collectAssists(context, FILTER_EQ);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		proposal= (CUCorrectionProposal)proposals.get(0);
		preview= getPreviewContent(proposal);

		String str3= """
			package test1;
			public class E {
			    public void foo(Enum e) {
			        e.equals(Enum.e1);
			    }
			}
			enum Enum {
			    e1, e2;
			}
			""";
		assertEqualString(preview, str3);
	}

	@Test
	public void testAddTypeToArrayInitializer() throws Exception {

			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str1= """
				package test1;
				public class E {
				    public void foo() {
				        int[][] numbers= {{ 1, 2 }, { 3, 4 }, { 4, 5 }};
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

			String str= "{{";
			AssistContext context= getCorrectionContext(cu, str1.indexOf(str) + str.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 2);
			assertCorrectLabels(proposals);

			ArrayList<String> previews= new ArrayList<>();
			ArrayList<String> expecteds= new ArrayList<>();

			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E {\n");
			buf.append("    public void foo() {\n");
			buf.append("        int[][] numbers= new int[][]{{ 1, 2 }, { 3, 4 }, { 4, 5 }};\n");
			buf.append("    }\n");
			buf.append("}\n");
			addPreviewAndExpected(proposals, buf, expecteds, previews);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E {\n");
			buf.append("    private static final int[] INTS = { 1, 2 };\n");
			buf.append("\n");
			buf.append("    public void foo() {\n");
			buf.append("        int[][] numbers= {INTS, { 3, 4 }, { 4, 5 }};\n");
			buf.append("    }\n");
			buf.append("}\n");
			addPreviewAndExpected(proposals, buf, expecteds, previews);

			assertEqualStringsIgnoreOrder(previews, expecteds);
		}

	@Test
	public void testCreateInSuper() throws Exception {

			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str1= """
				package test1;
				public class A {
				}
				""";
			pack1.createCompilationUnit("A.java", str1, false, null);

			String str2= """
				package test1;
				public interface IB {
				}
				""";
			pack1.createCompilationUnit("IB.java", str2, false, null);

			String str3= """
				package test1;
				import java.io.IOException;
				import java.util.Vector;
				public class E extends A implements IB {
				    public Vector foo(int count) throws IOException {
				        return null;
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str3, false, null);

			String str= "foo";
			AssistContext context= getCorrectionContext(cu, str3.indexOf(str) + str.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 2);
			assertCorrectLabels(proposals);

			CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
			String preview1= getPreviewContent(proposal);

			String expected1= """
				package test1;
				
				import java.io.IOException;
				import java.util.Vector;
				
				public interface IB {
				
				    Vector foo(int count) throws IOException;
				}
				""";

			proposal= (CUCorrectionProposal) proposals.get(1);
			String preview2= getPreviewContent(proposal);

			String expected2= """
				package test1;
				
				import java.io.IOException;
				import java.util.Vector;
				
				public class A {
				
				    public Vector foo(int count) throws IOException {
				        //TODO
				        return null;
				    }
				}
				""";

			assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

		}

	@Test
	public void testCreateInSuperInGeneric() throws Exception {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str1= """
				package test1;
				public class A<T> {
				}
				""";
			pack1.createCompilationUnit("A.java", str1, false, null);

			String str2= """
				package test1;
				public interface IB<T> {
				}
				""";
			pack1.createCompilationUnit("IB.java", str2, false, null);

			String str3= """
				package test1;
				import java.io.IOException;
				import java.util.Vector;
				public class E extends A<String> implements IB<String> {
					/**
					 * Always return NULL
					 * @param count
					 * @return NULL
					 * @throws IOException
					 */
				    public Vector<String> foo(int count) throws IOException {
				        return null;
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str3, false, null);

			String str= "foo";
			AssistContext context= getCorrectionContext(cu, str3.indexOf(str) + str.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 2);
			assertCorrectLabels(proposals);

			CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
			String preview1= getPreviewContent(proposal);

			String expected1= """
				package test1;
				
				import java.io.IOException;
				import java.util.Vector;
				
				public interface IB<T> {
				
				    /**
				     * Always return NULL
				     * @param count
				     * @return NULL
				     * @throws IOException
				     */
				    Vector<String> foo(int count) throws IOException;
				}
				""";

			proposal= (CUCorrectionProposal) proposals.get(1);
			String preview2= getPreviewContent(proposal);

			String expected2= """
				package test1;
				
				import java.io.IOException;
				import java.util.Vector;
				
				public class A<T> {
				
				    /**
				     * Always return NULL
				     * @param count
				     * @return NULL
				     * @throws IOException
				     */
				    public Vector<String> foo(int count) throws IOException {
				        //TODO
				        return null;
				    }
				}
				""";

			assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
		}

	@Test
	public void testChangeIfStatementToBlock() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)\s
			            ;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "if (";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        }
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)\s
			            ;
			        else {
			        }
			    }
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    public void foo() {
			        ;
			    }
			}
			""";

		String expected4= """
			package test1;
			public class E {
			    public void foo() {
			        if (false)
			            return;
			        ;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testChangeElseStatementToBlock() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;;
			        } else
			            ;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "else";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;;
			        } else {
			            ;
			        }
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        if (false)
			            ;
			        else {
			            ;;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2});
	}

	@Test
	public void testChangeIfWithElseStatementToBlock() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else {
			            ;;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "if (";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else {
			            ;;
			        }
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        if (false) {
			            ;;
			        } else
			            ;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2});
	}

	@Test
	public void testChangeIfAndElseStatementToBlock1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else
			            ;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "if (";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else
			            ;
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else {
			            ;
			        }
			    }
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    public void foo() {
			        if (false)
			            ;
			        else
			            ;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testChangeIfAndElseStatementToBlock2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else
			            ;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "else";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else {
			            ;
			        }
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else {
			            ;
			        }
			    }
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    public void foo() {
			        if (false)
			            ;
			        else
			            ;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testChangeIfAndElseIfStatementToBlock() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else if (true)
			            ;
			        else if (false)
			            ;
			        else
			            ;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "else if (";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else {
			            if (true)
			                ;
			            else if (false)
			                ;
			            else
			                ;
			        }
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else if (true) {
			            ;
			        } else if (false) {
			            ;
			        } else {
			            ;
			        }
			    }
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    public void foo() {
			        if (false) {
			            if (true)
			                ;
			            else if (false)
			                ;
			            else
			                ;
			        } else
			            ;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testChangeIfAndElseIfStatementWithBlockToBlock() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else if (true) {
			            ;
			        } else if (false)
			            ;
			        else
			            ;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "else if (";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else {
			            if (true) {
			                ;
			            } else if (false)
			                ;
			            else
			                ;
			        }
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else if (true) {
			            ;
			        } else if (false) {
			            ;
			        } else {
			            ;
			        }
			    }
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    public void foo() {
			        if (false) {
			            if (true) {
			                ;
			            } else if (false)
			                ;
			            else
			                ;
			        } else
			            ;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testRemoveIfBlock01() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "if (";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else {
			        }
			    }
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    public void foo() {
			        ;
			    }
			}
			""";

		String expected4= """
			package test1;
			public class E {
			    public void foo() {
			        if (false)
			            return;
			        ;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4 });
	}

	@Test
	public void testRemoveIfBlock02() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else {
			            ;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "if (";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else
			            ;
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else {
			            ;
			        }
			    }
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else
			            ;
			    }
			}
			""";

		String expected4= """
			package test1;
			public class E {
			    public void foo() {
			        if (false) {
			            ;
			        } else {
			            ;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3, expected4});
	}

	@Test
	public void testRemoveIfBlock03() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else {
			            ;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "{\n            ;";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else
			            ;
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else {
			            ;
			        }
			    }
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else
			            ;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testRemoveIfBlock04() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public int foo() {
			        if (true)
			            return 1; /* comment*/
			        else
			            return 2;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "/* comment*/";
		int indexOf= str1.indexOf(str) + str.length();
		AssistContext context= getCorrectionContext(cu, indexOf, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public int foo() {
			        if (true) {
			            return 1; /* comment*/
			        } else
			            return 2;
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public int foo() {
			        if (true) {
			            return 1; /* comment*/
			        } else {
			            return 2;
			        }
			    }
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    public int foo() {
			        if (false)
			            return 2;
			        else
			            return 1; /* comment*/
			    }
			}
			""";

		String expected4= """
			package test1;
			public class E {
			    public int foo() {
			        return true ? 1 : 2;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3, expected4});
	}


	@Test
	public void testRemoveIfBlockBug128843() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else if (false) {
			            ;
			        } else {
			            ;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= " (false) {";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else if (false)
			            ;
			        else {
			            ;
			        }
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else if (false) {
			            ;
			        } else
			            ;
			    }
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else if (false)
			            ;
			        else
			            ;
			    }
			}
			""";

		String expected4= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            ;
			        } else if (true) {
			            ;
			        } else {
			            ;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3, expected4});
	}

	@Test
	public void testRemoveIfBlockBug138628() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (true) {
			            if (true)
			                ;
			        } else
			            ;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= " (true) {";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (false)
			            ;
			        else {
			            if (true)
			                ;
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testRemoveIfBlockBug149990_1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test;
			public class E {
			    public void foo() {
			        if (false) {
			            while (true)
			                if (false) {
			                    ;
			                }
			        } else
			            ;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= " (false) {";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String expected1= """
			package test;
			public class E {
			    public void foo() {
			        if (true)
			            ;
			        else {
			            while (true)
			                if (false) {
			                    ;
			                }
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testRemoveIfBlockBug139675() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        if (false)
			            if (true) {
			                ;
			            } else if (false) {
			                ;
			            } else {
			                ;
			            }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= " (true) {";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        if (false)
			            if (true)
			                ;
			            else if (false) {
			                ;
			            } else {
			                ;
			            }
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        if (false)
			            if (true)
			                ;
			            else if (false)
			                ;
			            else
			                ;
			    }
			}
			""";

		String expected3= """
			package test1;
			public class E {
			    public void foo() {
			        if (false)
			            if (false) {
			                if (false) {
			                    ;
			                } else {
			                    ;
			                }
			            } else {
			                ;
			            }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2, expected3});
	}

	@Test
	public void testRemoveIfBlockBug149990_2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test;
			public class E {
			    public void foo() {
			        if (false)
			            while (true)
			                while (true) {
			                    while (true)
			                        if (false)
			                            ;
			                }
			        else
			            ;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= " (true) {";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test;
			public class E {
			    public void foo() {
			        if (false)
			            while (true)
			                while (true)
			                    if (false)
			                        ;
			        else
			            ;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});

	}

	@Test
	public void testRemoveWhileBlock01() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        while (true) {
			            ;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "while (";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        while (true);
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        ;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2});
	}

	@Test
	public void testRemoveForBlock01() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        for (;;) {
			            ;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "for (";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        for (;;);
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        ;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2});
	}

	@Test
	public void testRemoveDoBlock01() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void foo() {
			        do {
			            ;
			        } while (true);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "do {";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void foo() {
			        do; while (true);
			    }
			}
			""";

		String expected2= """
			package test1;
			public class E {
			    public void foo() {
			        ;
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1, expected2});
	}

	@Test
	public void testMakeFinal01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    private int i= 0;
			    private void foo() {
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("public");
		int offset2= str.lastIndexOf("}");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test;
			public class E {
			    private final int i= 0;
			    private void foo() {
			        System.out.println(i);
			    }
			}
			""";

		String expected2= null; // Wrap in buf.append() (to clipboard)

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testMakeFinal02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    private final int i= 0;
			    private void foo() {
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("public");
		int offset2= str.lastIndexOf("}");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, CHANGE_MODIFIER_TO_FINAL);

		String expected1= null; // Wrap in buf.append() (to clipboard)
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testMakeFinal03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    public int i= 0;
			    private void foo() {
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, CHANGE_MODIFIER_TO_FINAL);

		String expected1= """
			package test;
			public class E {
			    private int i= 0;
			    private void foo() {
			        System.out.println(getI());
			    }
			    public int getI() {
			        return i;
			    }
			    public void setI(int i) {
			        this.i = i;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { expected1 });
	}

	@Test
	public void testMakeFinal04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    private void foo() {
			        int i= 0, j= 0;
			        System.out.println(i);
			        System.out.println(j);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("int i= 0");
		int length= "int i= 0".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str1= """
			package test;
			public class E {
			    private void foo() {
			        final int i= 0;
			        int j= 0;
			        System.out.println(i);
			        System.out.println(j);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str1});
	}

	@Test
	public void testMakeFinal05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    private void foo(int i, int j) {
			        System.out.println(i);
			        System.out.println(j);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("int i");
		int length= "int i".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str1= """
			package test;
			public class E {
			    private void foo(final int i, int j) {
			        System.out.println(i);
			        System.out.println(j);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str1});
	}

	@Test
	public void testMakeFinal06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    public void foo() {
			        int i= 0;
			        i= 1;
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("public");
		int offset2= str.lastIndexOf("}");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, CHANGE_MODIFIER_TO_FINAL);

		assertExpectedExistInProposals(proposals, new String[] { null }); // Wrap in buf.append() (to clipboard)
	}

	@Test
	public void testMakeFinal07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    private int i= 0;
			    public void foo() {
			        System.out.println(i);
			    }
			    public void set(int i) {
			        this.i= i;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("private int i= 0");
		int length= "private int i= 0".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
		assertProposalDoesNotExist(proposals, CHANGE_MODIFIER_TO_FINAL);
	}

	@Test
	public void testMakeFinal08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    private int i= 0;
			    public void foo() {
			        System.out.println(i);
			    }
			    public void reset() {
			        i= 0;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("public");
		int offset2= str.lastIndexOf("}");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, CHANGE_MODIFIER_TO_FINAL);

		assertExpectedExistInProposals(proposals, new String[] { null }); // Wrap in buf.append() (to clipboard)
	}

	@Test
	public void testMakeFinal09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    private int i= 0;
			    public void foo() {
			        System.out.println(i);
			    }
			    public void reset() {
			        i--;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("public");
		int offset2= str.lastIndexOf("}");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, CHANGE_MODIFIER_TO_FINAL);

		assertExpectedExistInProposals(proposals, new String[] { null }); // Wrap in buf.append() (to clipboard)
	}

	@Test
	public void testMakeFinal10() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    private int i= 0;
			    public void foo() {
			        System.out.println(i);
			    }
			    public void reset() {
			        this.i++;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("public");
		int offset2= str.lastIndexOf("}");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, CHANGE_MODIFIER_TO_FINAL);

		assertExpectedExistInProposals(proposals, new String[] { null }); // Wrap in buf.append() (to clipboard)
	}

	@Test
	public void testMakeFinal11() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    public void foo() {
			        for (int j= 0, i= 0; j < (new int[0]).length; j++) {
			            System.out.println(i);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("public");
		int offset2= str.lastIndexOf("}");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, CHANGE_MODIFIER_TO_FINAL);

		assertExpectedExistInProposals(proposals, new String[] { null }); // Wrap in buf.append() (to clipboard)
	}

	@Test
	public void testMakeFinal12() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    public void foo() {
			        int i= 1, j= i + 1, h= j + 1;
			        System.out.println(i);
			        System.out.println(j);
			        System.out.println(h);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("int i= 1");
		int length= "int i= 1".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str1= """
			package test;
			public class E {
			    public void foo() {
			        final int i= 1;
			        int j= i + 1, h= j + 1;
			        System.out.println(i);
			        System.out.println(j);
			        System.out.println(h);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str1});
	}

	@Test
	public void testMakeFinal13() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    public void foo() {
			        int i= 1, j= i + 1, h= j + 1;
			        System.out.println(i);
			        System.out.println(j);
			        System.out.println(h);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("j= i + 1");
		int length= "j= i + 1".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String ex1= """
			package test;
			public class E {
			    public void foo() {
			        int i= 1;
			        final int j= i + 1;
			        int h= j + 1;
			        System.out.println(i);
			        System.out.println(j);
			        System.out.println(h);
			    }
			}
			""";

		String ex2= """
			package test;
			public class E {
			    public void foo() {
			        int i= 1, j, h= j + 1;
			        j = i + 1;
			        System.out.println(i);
			        System.out.println(j);
			        System.out.println(h);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2 });
	}

	@Test
	public void testMakeFinal14() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    public void foo() {
			        int i= 1, j= i + 1, h= j + 1;
			        System.out.println(i);
			        System.out.println(j);
			        System.out.println(h);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("h= j + 1");
		int length= "h= j + 1".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String ex1= """
			package test;
			public class E {
			    public void foo() {
			        int i= 1, j= i + 1;
			        final int h= j + 1;
			        System.out.println(i);
			        System.out.println(j);
			        System.out.println(h);
			    }
			}
			""";

		String ex2= """
			package test;
			public class E {
			    public void foo() {
			        int i= 1, j= i + 1, h;
			        h = j + 1;
			        System.out.println(i);
			        System.out.println(j);
			        System.out.println(h);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { ex1, ex2 });
	}

	@Test
	public void testMakeFinal15() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			import java.io.Serializable;
			public class E {
			    public void foo() {
			        Serializable ser= new Serializable() {
			            private int i= 0;
			            Serializable ser2= new Serializable() {
			                public void foo() {
			                    System.out.println(i);
			                }
			            };
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset1= str.indexOf("public");
		int offset2= str.lastIndexOf("}");
		AssistContext context= getCorrectionContext(cu, offset1, offset2 - offset1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String expected1= """
			package test;
			import java.io.Serializable;
			public class E {
			    public void foo() {
			        final Serializable ser= new Serializable() {
			            private final int i= 0;
			            Serializable ser2= new Serializable() {
			                public void foo() {
			                    System.out.println(i);
			                }
			            };
			        };
			    }
			}
			""";

		String expected2= null; // Wrap in buf.append() (to clipboard)

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2 });
	}

	@Test
	public void testMakeFinal16() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    public void foo() {
			        int i= 0;
			        Integer in= Integer.valueOf(i++);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("int i= 0");
		int length= "int i= 0".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
		assertProposalDoesNotExist(proposals, CHANGE_MODIFIER_TO_FINAL);
	}

	@Test
	public void testMakeFinal17() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    private int i= 0;
			    private void foo() {
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("i=");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalDoesNotExist(proposals, CHANGE_MODIFIER_TO_FINAL);

		String str1= """
			package test;
			public class E {
			    private int i= 0;
			    private void foo() {
			        System.out.println(getI());
			    }
			    public int getI() {
			        return i;
			    }
			    public void setI(int i) {
			        this.i = i;
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] { str1 });
	}

	@Test
	public void testMakeFinal18() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    private int i= 0;
			    private void foo() {
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("E");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertProposalExists(proposals, NLS.bind(CorrectionMessages.AddGetterSetter_creategetterssettersfortype_description, "E"));
		assertProposalExists(proposals, NLS.bind(CorrectionMessages.AddHashCodeEquals_createhashcodeequalsfortype_description, "E"));
		assertProposalExists(proposals, NLS.bind(CorrectionMessages.AddToString_createtostringfortype_description, "E"));
		assertProposalDoesNotExist(proposals, CHANGE_MODIFIER_TO_FINAL);
	}

	@Test
	public void testMakeFinal19() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    private void foo() {
			        int i= 0;
			        System.out.println(i);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("foo");
		int length= "foo".length();
		AssistContext context= getCorrectionContext(cu, offset, length);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
		assertProposalDoesNotExist(proposals, CHANGE_MODIFIER_TO_FINAL);
	}

	@Test
	public void testMakeFinalBug148373() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test;
			public class E {
			    public void foo(Integer i) {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String selection= "public void foo(Integer i)";
		int offset= str.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str1= """
			package test;
			public class E {
			    public void foo(final Integer i) {
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str1});
	}
	@Test
	public void testConvertAnonymousToNested1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			public class E {
			    public Object foo(final String name) {
			        return new Runnable() {
			            public void run() {
			                foo(name);
			            }
			        };
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("Runnable");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str1= """
			package pack;
			public class E {
			    private final class RunnableImplementation implements Runnable {
			        private final String name;
			        private RunnableImplementation(String name) {
			            this.name = name;
			        }
			        public void run() {
			            foo(name);
			        }
			    }
			
			    public Object foo(final String name) {
			        return new RunnableImplementation(name);
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str1});
	}

	@Test
	public void testConvertAnonymousToNested2() throws Exception {
		Preferences corePrefs= JavaPlugin.getJavaCorePluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		corePrefs.setValue(JavaCore.CODEASSIST_LOCAL_PREFIXES, "l");
		corePrefs.setValue(JavaCore.CODEASSIST_ARGUMENT_PREFIXES, "p");

		IPackageFragment pack1= fSourceFolder.createPackageFragment("pack", false, null);
		String str= """
			package pack;
			import java.util.ArrayList;
			public class E {
			    public Object foo(final String pName) {
			        int lVar= 8;
			        return new ArrayList(lVar) {
			            String fExisting= pName;
			            public void run() {
			                foo(fExisting);
			            }
			        };
			    }
			}
			class ArrayListExtension {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		int offset= str.indexOf("ArrayList(lVar)");
		AssistContext context= getCorrectionContext(cu, offset, 1);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		String str1= """
			package pack;
			import java.util.ArrayList;
			public class E {
			    private final class ArrayListExtension2 extends ArrayList {
			        private final String fName;
			        String fExisting;
			        private ArrayListExtension2(int pArg0, String pName) {
			            super(pArg0);
			            fName = pName;
			            fExisting = fName;
			        }
			        public void run() {
			            foo(fExisting);
			        }
			    }
			
			    public Object foo(final String pName) {
			        int lVar= 8;
			        return new ArrayListExtension2(lVar, pName);
			    }
			}
			class ArrayListExtension {
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {str1});
	}

	@Test
	public void testConvertToStringBuffer1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo() {
			        String strX = "foo"+"bar"+"baz"+"biz";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("\"+\""), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 7);
		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class A {
			    public void foo() {
			        StringBuilder stringBuilder = new StringBuilder();
			        stringBuilder.append("foo");
			        stringBuilder.append("bar");
			        stringBuilder.append("baz");
			        stringBuilder.append("biz");
			        String strX = stringBuilder.toString();
			    }
			}
			""";

		String expected2= """
			package test1;
			public class A {
			    public void foo() {
			        String string = "foo";
			        String strX = string+"bar"+"baz"+"biz";
			    }
			}
			""";

		String expected3= """
			package test1;
			public class A {
			    public void foo() {
			        String string = "foo";
			        String strX = string+"bar"+"baz"+"biz";
			    }
			}
			""";

		String expected4= """
			package test1;
			public class A {
			    private static final String FOO = "foo";
			
			    public void foo() {
			        String strX = FOO+"bar"+"baz"+"biz";
			    }
			}
			""";

		String expected5= """
			package test1;
			public class A {
			    public void foo() {
			        String strX = ("foo"+"bar"+"baz"+"biz");
			    }
			}
			""";

		String expected6= """
			package test1;
			public class A {
			    public void foo() {
			        String strX = "foobarbazbiz";
			    }
			}
			""";

		String expected7= """
			package test1;
			public class A {
			    public void foo() {
			        String strX = "FOO"+"bar"+"baz"+"biz";
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] { expected1, expected2, expected3, expected4, expected5, expected6, expected7 });
	}

	@Test
	public void testConvertToStringBufferStringAndVar() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo() {
			        String foo = "foo";
			        String fuu = "fuu";
			        String strX = foo+"bar"+fuu;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("strX ="), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class A {
			    public void foo() {
			        String foo = "foo";
			        String fuu = "fuu";
			        StringBuilder stringBuilder = new StringBuilder();
			        stringBuilder.append(foo);
			        stringBuilder.append("bar");
			        stringBuilder.append(fuu);
			        String strX = stringBuilder.toString();
			    }
			}
			""";
		assertProposalPreviewEquals(str1, NLS.bind(CorrectionMessages.QuickAssistProcessor_convert_to_string_buffer_description, "StringBuilder"), proposals);
	}

	@Test
	public void testConvertToStringBufferNLS() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo() {
			        String strX = "foo"+"bar"+"baz"+"biz"; //a comment //$NON-NLS-1$ //$NON-NLS-3$
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("\"+\""), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class A {
			    public void foo() {
			        StringBuilder stringBuilder = new StringBuilder();
			        stringBuilder.append("foo"); //$NON-NLS-1$
			        stringBuilder.append("bar");
			        stringBuilder.append("baz"); //$NON-NLS-1$
			        stringBuilder.append("biz");
			        String strX = stringBuilder.toString(); //a comment
			    }
			}
			""";
		assertProposalPreviewEquals(str1, NLS.bind(CorrectionMessages.QuickAssistProcessor_convert_to_string_buffer_description, "StringBuilder"), proposals);
	}

	@Test
	public void testConvertToStringBufferNoFixWithoutString() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo() {
			        int strX = 5+1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("strX ="), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCommandIdDoesNotExist(proposals, QuickAssistProcessor.CONVERT_TO_STRING_BUFFER_ID);
	}

	@Test
	public void testConvertToStringBufferNoFixWithoutString2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo() {
			        int strX;
			        strX = 5+1;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("strX ="), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCommandIdDoesNotExist(proposals, QuickAssistProcessor.CONVERT_TO_STRING_BUFFER_ID);
	}

	@Test
	public void testConvertToStringBufferNoFixOutsideMethod() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    String strX = "foo"+"bar"
			    public void foo() {
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("strX ="), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCommandIdDoesNotExist(proposals, QuickAssistProcessor.CONVERT_TO_STRING_BUFFER_ID);
	}

	@Test
	public void testConvertToStringBufferDupVarName() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo() {
			        int stringBuilder = 5;
			        String stringBuilder2;
			        StringBuilder stringBuilder3 = null;
			        String strX = "foo"+"bar";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("strX ="), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class A {
			    public void foo() {
			        int stringBuilder = 5;
			        String stringBuilder2;
			        StringBuilder stringBuilder3 = null;
			        StringBuilder stringBuilder4 = new StringBuilder();
			        stringBuilder4.append("foo");
			        stringBuilder4.append("bar");
			        String strX = stringBuilder4.toString();
			    }
			}
			""";
		assertProposalPreviewEquals(str1, NLS.bind(CorrectionMessages.QuickAssistProcessor_convert_to_string_buffer_description, "StringBuilder"), proposals);
	}

	@Test
	public void testConvertToStringBufferInIfStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo() {
			        String strX;
			        if(true) strX = "foo"+"bar";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("\"+\""), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class A {
			    public void foo() {
			        String strX;
			        if(true) {
			            StringBuilder stringBuilder = new StringBuilder();
			            stringBuilder.append("foo");
			            stringBuilder.append("bar");
			            strX = stringBuilder.toString();
			        }
			    }
			}
			""";
		assertProposalPreviewEquals(str1, NLS.bind(CorrectionMessages.QuickAssistProcessor_convert_to_string_buffer_description, "StringBuilder"), proposals);
	}

	@Test
	public void testConvertToStringBufferAsParamter() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo() {
			        System.out.println("foo"+"bar");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("\"+\""), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class A {
			    public void foo() {
			        StringBuilder stringBuilder = new StringBuilder();
			        stringBuilder.append("foo");
			        stringBuilder.append("bar");
			        System.out.println(stringBuilder.toString());
			    }
			}
			""";
		assertProposalPreviewEquals(str1, NLS.bind(CorrectionMessages.QuickAssistProcessor_convert_to_string_buffer_description, "StringBuilder"), proposals);
	}

	@Test
	public void testConvertToStringBufferJava14() throws Exception {

		Map<String, String> oldOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(oldOptions);
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_4, newOptions);
		fJProject1.setOptions(newOptions);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				public class A {
				    public void foo() {
				        System.out.println("foo"+"bar");
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

			AssistContext context= getCorrectionContext(cu, str.indexOf("\"+\""), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertCorrectLabels(proposals);

			String str1= """
				package test1;
				public class A {
				    public void foo() {
				        StringBuffer stringBuffer = new StringBuffer();
				        stringBuffer.append("foo");
				        stringBuffer.append("bar");
				        System.out.println(stringBuffer.toString());
				    }
				}
				""";
			assertProposalPreviewEquals(str1, NLS.bind(CorrectionMessages.QuickAssistProcessor_convert_to_string_buffer_description, "StringBuffer"), proposals);
		} finally {
			fJProject1.setOptions(oldOptions);
		}
	}

	@Test
	public void testConvertToStringBufferExisting1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo() {
			        StringBuffer buf= new StringBuffer();
			        buf.append("high" + 5);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("\" + 5"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class A {
			    public void foo() {
			        StringBuffer buf= new StringBuffer();
			        buf.append("high");
			        buf.append(5);
			    }
			}
			""";
		assertProposalPreviewEquals(str1, NLS.bind(CorrectionMessages.QuickAssistProcessor_convert_to_string_buffer_description, "buf"), proposals);
	}

	@Test
	public void testConvertToStringBufferExisting2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo() {
			        StringBuilder sb= new StringBuilder();
			        sb.append("high" + 5 + " ho");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("\" + 5"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class A {
			    public void foo() {
			        StringBuilder sb= new StringBuilder();
			        sb.append("high");
			        sb.append(5);
			        sb.append(" ho");
			    }
			}
			""";
		assertProposalPreviewEquals(str1, NLS.bind(CorrectionMessages.QuickAssistProcessor_convert_to_string_buffer_description, "sb"), proposals);
	}

	@Test
	public void testConvertToMessageFormat14() throws Exception {

		Map<String, String> oldOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(oldOptions);
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_4, newOptions);
		fJProject1.setOptions(newOptions);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				public class A {
				    public void foo(Object o1, Object o2) {
				        System.out.println("foo" + o1 + " \\"bar\\" " + o2);
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

			AssistContext context= getCorrectionContext(cu, str.indexOf("\" + o1 + \""), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertCorrectLabels(proposals);

			String str1= """
				package test1;
				
				import java.text.MessageFormat;
				
				public class A {
				    public void foo(Object o1, Object o2) {
				        System.out.println(MessageFormat.format("foo{0} \\"bar\\" {1}", new Object[]{o1, o2}));
				    }
				}
				""";
			assertProposalPreviewEquals(str1, CorrectionMessages.QuickAssistProcessor_convert_to_message_format, proposals);
		} finally {
			fJProject1.setOptions(oldOptions);
		}
	}

	@Test
	public void testConvertToMessageFormatStringConcat() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println("foo" + "" + " \\"bar\\" ");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("\" + \"\" + \""), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		assertCommandIdDoesNotExist(proposals, QuickAssistProcessor.CONVERT_TO_MESSAGE_FORMAT_ID);
	}

	@Test
	public void testConvertToMessageFormatStringBoxing14() throws Exception {
		Map<String, String> oldOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(oldOptions);
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_4, newOptions);
		fJProject1.setOptions(newOptions);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				public class A {
				    public void foo(Object o1, Object o2) {
				        System.out.println("foo" + 1 + " \\"bar\\" ");
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

			AssistContext context= getCorrectionContext(cu, str.indexOf("1 + \""), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertCorrectLabels(proposals);

			String str1= """
				package test1;
				
				import java.text.MessageFormat;
				
				public class A {
				    public void foo(Object o1, Object o2) {
				        System.out.println(MessageFormat.format("foo{0} \\"bar\\" ", new Object[]{new Integer(1)}));
				    }
				}
				""";
			assertProposalPreviewEquals(str1, CorrectionMessages.QuickAssistProcessor_convert_to_message_format, proposals);
		} finally {
			fJProject1.setOptions(oldOptions);
		}
	}

	@Test
	public void testConvertToMessageFormatStringBoxing15() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println("foo" + 1 + " \\"bar\\" ");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("\" + 1 + \""), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			
			import java.text.MessageFormat;
			
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println(MessageFormat.format("foo{0} \\"bar\\" ", 1));
			    }
			}
			""";
		assertProposalPreviewEquals(str1, CorrectionMessages.QuickAssistProcessor_convert_to_message_format, proposals);
	}

	@Test
	public void testConvertToMessageFormat15() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println("foo" + o1 + " \\"bar\\" " + o2);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("\" + o1 + \""), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			
			import java.text.MessageFormat;
			
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println(MessageFormat.format("foo{0} \\"bar\\" {1}", o1, o2));
			    }
			}
			""";
		assertProposalPreviewEquals(str1, CorrectionMessages.QuickAssistProcessor_convert_to_message_format, proposals);
	}

	@Test
	public void testConvertToMessageFormatApostrophe() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println("foo'" + o1 + "' \\"bar\\" " + o2);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("\" + o1 + \""), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			
			import java.text.MessageFormat;
			
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println(MessageFormat.format("foo''{0}'' \\"bar\\" {1}", o1, o2));
			    }
			}
			""";
		assertProposalPreviewEquals(str1, CorrectionMessages.QuickAssistProcessor_convert_to_message_format, proposals);
	}

	@Test
	public void testConvertToMessageFormatExtendedOperands() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo() {
			        String s2= "a" + "b" + 3L + "c" + (4-2) + "d" + "e" + "f";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf(" + "), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			
			import java.text.MessageFormat;
			
			public class A {
			    public void foo() {
			        String s2= MessageFormat.format("ab{0}c{1}def", 3L, (4-2));
			    }
			}
			""";
		assertProposalPreviewEquals(str1, CorrectionMessages.QuickAssistProcessor_convert_to_message_format, proposals);
	}

	@Test
	public void testConvertToStringFormat14() throws Exception {

		Map<String, String> oldOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(oldOptions);
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_4, newOptions);
		fJProject1.setOptions(newOptions);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				public class A {
				    public void foo(Object o1, Object o2) {
				        System.out.println("foo" + o1 + " \\"bar\\" " + o2);
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

			AssistContext context= getCorrectionContext(cu, str.indexOf('+'), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertCorrectLabels(proposals);

			assertCommandIdDoesNotExist(proposals, QuickAssistProcessor.CONVERT_TO_STRING_FORMAT_ID);
		} finally {
			fJProject1.setOptions(oldOptions);
		}
	}

	@Test
	public void testConvertToMessageFormatNLS() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println("foo" + o1 + " \\"bar\\" " + o2); //a comment //$NON-NLS-1$ //$NON-NLS-2$
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf('+'), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			
			import java.text.MessageFormat;
			
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println(MessageFormat.format("foo{0} \\"bar\\" {1}", o1, o2)); //a comment //$NON-NLS-1$
			    }
			}
			""";
		assertProposalPreviewEquals(str1, CorrectionMessages.QuickAssistProcessor_convert_to_message_format, proposals);
	}

	@Test
	public void testConvertToMessageFormatNLSInvalid() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println("foo" + o1 + " \\"bar\\" " + o2); //a comment //$NON-NLS-1$
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf('+'), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_message_format);
	}

	@Test
	public void testConvertToStringFormatStringConcat() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println("foo" + "" + " \\"bar\\" ");
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf('+'), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		assertCommandIdDoesNotExist(proposals, QuickAssistProcessor.CONVERT_TO_STRING_FORMAT_ID);
	}

	@Test
	public void testConvertToStringFormat() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println("foo" + o1 + " \\"bar\\" " + o2);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf('+'), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println(String.format("foo%s \\"bar\\" %s", o1, o2));
			    }
			}
			""";
		assertProposalPreviewEquals(str1, CorrectionMessages.QuickAssistProcessor_convert_to_string_format, proposals);
	}

	@Test
	public void testConvertToStringFormatNLS() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println("foo" + o1 + " \\"bar\\" " + o2); //a comment //$NON-NLS-1$ //$NON-NLS-2$
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf('+'), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println(String.format("foo%s \\"bar\\" %s", o1, o2)); //a comment //$NON-NLS-1$
			    }
			}
			""";
		assertProposalPreviewEquals(str1, CorrectionMessages.QuickAssistProcessor_convert_to_string_format, proposals);
	}

	@Test
	public void testConvertToStringFormatNLSInvalid() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo(Object o1, Object o2) {
			        System.out.println("foo" + o1 + " \\"bar\\" " + o2); //a comment //$NON-NLS-1$
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf('+'), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_string_format);
	}

	@Test
	public void testConvertToStringFormatExtendedOperands() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public void foo() {
			        String s2= "a" + "b" + 3L + "c" + (4-2) + "d" + "e" + "f";
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf('+'), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class A {
			    public void foo() {
			        String s2= String.format("ab%dc%ddef", 3L, (4-2));
			    }
			}
			""";
		assertProposalPreviewEquals(str1, CorrectionMessages.QuickAssistProcessor_convert_to_string_format, proposals);
	}

	@Test
	public void testConvertToStringFormatPrimitives() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    public String foo(byte b, short s, int i, long l, float f, double d, boolean bb, char c) {
			        return "abc" + b + s + i + l + f + d + bb + c;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, String.valueOf(200));
		try {
			fJProject1.setOptions(newOptions);
			AssistContext context= getCorrectionContext(cu, str.indexOf('+'), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertCorrectLabels(proposals);

			String str1= """
				package test1;
				public class A {
				    public String foo(byte b, short s, int i, long l, float f, double d, boolean bb, char c) {
				        return String.format("abc%d%d%d%d%f%f%s%c", b, s, i, l, f, d, bb, c);
				    }
				}
				""";
			assertProposalPreviewEquals(str1, CorrectionMessages.QuickAssistProcessor_convert_to_string_format, proposals);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testMissingEnumConstantsInCase1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			       \s
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("switch"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[3];
		expected[0]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            default :
			                break;
			       \s
			        }
			    }
			}
			""";

		expected[1]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            case X2 :
			                break;
			            case X3 :
			                break;
			            default :
			                break;
			       \s
			        }
			    }
			}
			""";

		expected[2]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingEnumConstantsInCase2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			       \s
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("break;") + 7, 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 3);

		String[] expected= new String[3];
		expected[0]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            default :
			                break;
			       \s
			        }
			    }
			}
			""";

		expected[1]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            case X2 :
			                break;
			            case X3 :
			                break;
			            default :
			                break;
			       \s
			        }
			    }
			}
			""";

		expected[2]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        if (x == MyEnum.X1) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingEnumConstantsInCase3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			       \s
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("case"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            default :
			                break;
			       \s
			        }
			    }
			}
			""";

		expected[1]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            case X2 :
			                break;
			            case X3 :
			                break;
			            default :
			                break;
			       \s
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingEnumConstantsInCase4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            default :
			                break;
			       \s
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("default"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 1);

		String[] expected= new String[1];
		expected[0]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            case X2 :
			            case X3 :
			            default :
			                break;
			       \s
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testMissingEnumConstantsInCase5() throws Exception {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=372840
		IPackageFragment pack1= fSourceFolder.createPackageFragment("p", false, null);
		String str= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            case X2 :
			                break;
			            case X3 :
			                break;
			       \s
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("switch"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String[] expected= new String[2];
		expected[0]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        switch (x) {
			            case X1 :
			                break;
			            case X2 :
			                break;
			            case X3 :
			                break;
			            default :
			                break;
			       \s
			        }
			    }
			}
			""";

		expected[1]= """
			package p;
			
			public class E {
			    enum MyEnum {
			        X1, X2, X3
			    }
			   \s
			    public void foo(MyEnum x) {
			        if (x == MyEnum.X1) {
			        } else if (x == MyEnum.X2) {
			        } else if (x == MyEnum.X3) {
			        }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testConvertEnhancedForArray01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public static void main(String... args) {
			        for (final @Deprecated String arg : args) {
			            System.out.print(arg);
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("for"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expected= new String[3];
		expected[0]= """
			package test1;
			public class E {
			    public static void main(String... args) {
			        for (int i = 0; i < args.length; i++) {
			            final @Deprecated String arg = args[i];
			            System.out.print(arg);
			        }
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    public static void main(String... args) {
			        System.out.print(arg);
			    }
			}
			""";

		expected[2]= """
			package test1;
			public class E {
			    public static void main(String... args) {
			        for (final @Deprecated String arg : args)
			            System.out.print(arg);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testConvertEnhancedForArray02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    public void foo(int[][][] ints) {
			        outer: for (int[] is[] : ints.clone ()) {
			            //convert this
			            for (int i : is) {
			                System.out.print(i);
			                System.out.print(", ");
			            }
			            System.out.println();
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("for"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String[] expected= new String[2];
		expected[0]= """
			package test1;
			public class E {
			    public void foo(int[][][] ints) {
			        int[][][] clone = ints.clone ();
			        outer: for (int j = 0; j < clone.length; j++) {
			            int[] is[] = clone[j];
			            //convert this
			            for (int i : is) {
			                System.out.print(i);
			                System.out.print(", ");
			            }
			            System.out.println();
			        }
			    }
			}
			""";

		expected[1]= """
			package test1;
			public class E {
			    public void foo(int[][][] ints) {
			        outer: //convert this
			        for (int i : is) {
			            System.out.print(i);
			            System.out.print(", ");
			        }
			        System.out.println();
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testConvertEnhancedForList01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Arrays;
			import java.util.List;
			public class E {
			    void foo() {
			        for (Number number : getNums()) {
			            System.out.println(number.doubleValue());
			        }
			    }
			    private List<? extends Number> getNums() {
			        return Arrays.asList(1, 2.34, 0xFFFF);
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf(":"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		String[] expected= new String[4];
		expected[0]= """
			package test1;
			import java.util.Arrays;
			import java.util.List;
			public class E {
			    void foo() {
			        List<? extends Number> nums = getNums();
			        for (int i = 0; i < nums.size(); i++) {
			            Number number = nums.get(i);
			            System.out.println(number.doubleValue());
			        }
			    }
			    private List<? extends Number> getNums() {
			        return Arrays.asList(1, 2.34, 0xFFFF);
			    }
			}
			""";

		expected[1]= """
			package test1;
			import java.util.Arrays;
			import java.util.Iterator;
			import java.util.List;
			public class E {
			    void foo() {
			        for (Iterator<? extends Number> iterator = getNums()
			                .iterator(); iterator.hasNext();) {
			            Number number = iterator.next();
			            System.out.println(number.doubleValue());
			        }
			    }
			    private List<? extends Number> getNums() {
			        return Arrays.asList(1, 2.34, 0xFFFF);
			    }
			}
			""";

		expected[2]= """
			package test1;
			import java.util.Arrays;
			import java.util.List;
			public class E {
			    void foo() {
			        System.out.println(number.doubleValue());
			    }
			    private List<? extends Number> getNums() {
			        return Arrays.asList(1, 2.34, 0xFFFF);
			    }
			}
			""";

		expected[3]= """
			package test1;
			import java.util.Arrays;
			import java.util.List;
			public class E {
			    void foo() {
			        for (Number number : getNums())
			            System.out.println(number.doubleValue());
			    }
			    private List<? extends Number> getNums() {
			        return Arrays.asList(1, 2.34, 0xFFFF);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testConvertEnhancedForCollection01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collection;
			import java.util.List;
			public class E {
			    void foo(Collection<? extends List<? extends Number>> allNums) {
			        for (List<? extends Number> nums : allNums) {
			            for (Number number : nums) {
			                System.out.println(number.doubleValue());
			            }
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		AssistContext context= getCorrectionContext(cu, str.indexOf("for"), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String[] expected= new String[3];
		expected[0]= """
			package test1;
			import java.util.Collection;
			import java.util.Iterator;
			import java.util.List;
			public class E {
			    void foo(Collection<? extends List<? extends Number>> allNums) {
			        for (Iterator<? extends List<? extends Number>> iterator = allNums
			                .iterator(); iterator.hasNext();) {
			            List<? extends Number> nums = iterator.next();
			            for (Number number : nums) {
			                System.out.println(number.doubleValue());
			            }
			        }
			    }
			}
			""";

		expected[1]= """
			package test1;
			import java.util.Collection;
			import java.util.List;
			public class E {
			    void foo(Collection<? extends List<? extends Number>> allNums) {
			        for (Number number : nums) {
			            System.out.println(number.doubleValue());
			        }
			    }
			}
			""";

		expected[2]= """
			package test1;
			import java.util.Collection;
			import java.util.List;
			public class E {
			    void foo(Collection<? extends List<? extends Number>> allNums) {
			        for (List<? extends Number> nums : allNums)
			            for (Number number : nums) {
			                System.out.println(number.doubleValue());
			            }
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, expected);
	}

	@Test
	public void testGenerateForSimple() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collection;
			public class E {
			    void foo(Collection<String> collection) {
			        collection
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "collection";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 6);
			assertCorrectLabels(proposals);

			String[] expected= new String[2];
			expected[0]= """
				package test1;
				import java.util.Collection;
				public class E {
				    void foo(Collection<String> collection) {
				        for (String string : collection) {
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				import java.util.Collection;
				import java.util.Iterator;
				public class E {
				    void foo(Collection<String> collection) {
				        for (Iterator<String> iterator = collection.iterator(); iterator
				                .hasNext();) {
				            String string = iterator.next();
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForWithSemicolon() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collection;
			public class E {
			    void foo(Collection<String> collection) {
			        collection;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "collection;";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 4);
			assertCorrectLabels(proposals);

			String[] expected= new String[2];
			expected[0]= """
				package test1;
				import java.util.Collection;
				public class E {
				    void foo(Collection<String> collection) {
				        for (String string : collection) {
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				import java.util.Collection;
				import java.util.Iterator;
				public class E {
				    void foo(Collection<String> collection) {
				        for (Iterator<String> iterator = collection.iterator(); iterator
				                .hasNext();) {
				            String string = iterator.next();
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Map;
			public class E {
			    void foo(Map<String, String> map) {
			        map.keySet()
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "keySet()";
			AssistContext context= getCorrectionContext(cu, str.indexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 6);
			assertCorrectLabels(proposals);

			String[] expected= new String[2];
			expected[0]= """
				package test1;
				import java.util.Map;
				public class E {
				    void foo(Map<String, String> map) {
				        for (String string : map.keySet()) {
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				import java.util.Iterator;
				import java.util.Map;
				public class E {
				    void foo(Map<String, String> map) {
				        for (Iterator<String> iterator = map.keySet().iterator(); iterator
				                .hasNext();) {
				            String string = iterator.next();
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForComplexParametrization() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.LinkedList;
			public class E {
			    void foo(MySecondOwnIterable collection) {
			        collection
			    }
			private class MyFirstOwnIterable<T, K> extends LinkedList<K>{}\
			private class MySecondOwnIterable extends MyFirstOwnIterable<Integer, String>{}\
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "collection";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 7);
			assertCorrectLabels(proposals);

			String[] expected= new String[3];
			expected[0]= """
				package test1;
				import java.util.LinkedList;
				public class E {
				    void foo(MySecondOwnIterable collection) {
				        for (String string : collection) {
				           \s
				        }
				    }
				private class MyFirstOwnIterable<T, K> extends LinkedList<K>{}\
				private class MySecondOwnIterable extends MyFirstOwnIterable<Integer, String>{}\
				}
				""";

			expected[1]= """
				package test1;
				import java.util.Iterator;
				import java.util.LinkedList;
				public class E {
				    void foo(MySecondOwnIterable collection) {
				        for (Iterator<String> iterator = collection.iterator(); iterator
				                .hasNext();) {
				            String string = iterator.next();
				           \s
				        }
				    }
				private class MyFirstOwnIterable<T, K> extends LinkedList<K>{}\
				private class MySecondOwnIterable extends MyFirstOwnIterable<Integer, String>{}\
				}
				""";

			expected[2]= """
				package test1;
				import java.util.LinkedList;
				public class E {
				    void foo(MySecondOwnIterable collection) {
				        for (int i = 0; i < collection.size(); i++) {
				            String string = collection.get(i);
				           \s
				        }
				    }
				private class MyFirstOwnIterable<T, K> extends LinkedList<K>{}\
				private class MySecondOwnIterable extends MyFirstOwnIterable<Integer, String>{}\
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForGenerics() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collection;
			import java.util.Date;
			public class E {
			    void <T extends Date> foo(Collection<T> collection) {
			        collection
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "collection";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 6);
			assertCorrectLabels(proposals);

			String[] expected= new String[2];
			expected[0]= """
				package test1;
				import java.util.Collection;
				import java.util.Date;
				public class E {
				    void <T extends Date> foo(Collection<T> collection) {
				        for (T t : collection) {
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				import java.util.Collection;
				import java.util.Date;
				import java.util.Iterator;
				public class E {
				    void <T extends Date> foo(Collection<T> collection) {
				        for (Iterator<T> iterator = collection.iterator(); iterator.hasNext();) {
				            T t = iterator.next();
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForComplexGenerics() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class E {
			    void foo() {
			        getIterable()
			    }
			    <T extends Iterable<? super Number> & Comparable<Number>> Iterable<T> getIterable() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "getIterable()";
			AssistContext context= getCorrectionContext(cu, str.indexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 6);
			assertCorrectLabels(proposals);

			String[] expected= new String[2];
			expected[0]= """
				package test1;
				import java.util.List;
				public class E {
				    void foo() {
				        for (Iterable<? super Number> iterable : getIterable()) {
				           \s
				        }
				    }
				    <T extends Iterable<? super Number> & Comparable<Number>> Iterable<T> getIterable() {
				        return null;
				    }
				}
				""";

			expected[1]= """
				package test1;
				import java.util.Iterator;
				import java.util.List;
				public class E {
				    void foo() {
				        for (Iterator<? extends Iterable<? super Number>> iterator = getIterable()
				                .iterator(); iterator.hasNext();) {
				            Iterable<? super Number> iterable = iterator.next();
				           \s
				        }
				    }
				    <T extends Iterable<? super Number> & Comparable<Number>> Iterable<T> getIterable() {
				        return null;
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForUpperboundWildcard() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Date;
			import java.util.List;
			public class E {
			    void foo(List<? extends Date> list) {
			        list
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "list";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 7);
			assertCorrectLabels(proposals);

			String[] expected= new String[3];
			expected[0]= """
				package test1;
				import java.util.Date;
				import java.util.List;
				public class E {
				    void foo(List<? extends Date> list) {
				        for (Date date : list) {
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				import java.util.Date;
				import java.util.Iterator;
				import java.util.List;
				public class E {
				    void foo(List<? extends Date> list) {
				        for (Iterator<? extends Date> iterator = list.iterator(); iterator
				                .hasNext();) {
				            Date date = iterator.next();
				           \s
				        }
				    }
				}
				""";

			expected[2]= """
				package test1;
				import java.util.Date;
				import java.util.List;
				public class E {
				    void foo(List<? extends Date> list) {
				        for (int i = 0; i < list.size(); i++) {
				            Date date = list.get(i);
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForLowerboundWildcard() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Date;
			import java.util.List;
			public class E {
			    void foo(List<? super Date> list) {
			        list
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "list";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 7);
			assertCorrectLabels(proposals);

			String[] expected= new String[3];
			expected[0]= """
				package test1;
				import java.util.Date;
				import java.util.List;
				public class E {
				    void foo(List<? super Date> list) {
				        for (Object object : list) {
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				import java.util.Date;
				import java.util.Iterator;
				import java.util.List;
				public class E {
				    void foo(List<? super Date> list) {
				        for (Iterator<? super Date> iterator = list.iterator(); iterator
				                .hasNext();) {
				            Object object = iterator.next();
				           \s
				        }
				    }
				}
				""";

			expected[2]= """
				package test1;
				import java.util.Date;
				import java.util.List;
				public class E {
				    void foo(List<? super Date> list) {
				        for (int i = 0; i < list.size(); i++) {
				            Object object = list.get(i);
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForComplexInnerLowerboundWildcard() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.List;
			public class E {
			    private abstract class Inner<T, K> implements Iterable<K>{}
			    void foo() {
			        getList()
			    }
			    Inner<? super List<Number>, ? super List<List<Number>>> getList() {
			        return null;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "getList()";
			AssistContext context= getCorrectionContext(cu, str.indexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 6);
			assertCorrectLabels(proposals);

			String[] expected= new String[2];
			expected[0]= """
				package test1;
				import java.util.List;
				public class E {
				    private abstract class Inner<T, K> implements Iterable<K>{}
				    void foo() {
				        for (Object object : getList()) {
				           \s
				        }
				    }
				    Inner<? super List<Number>, ? super List<List<Number>>> getList() {
				        return null;
				    }
				}
				""";

			expected[1]= """
				package test1;
				import java.util.Iterator;
				import java.util.List;
				public class E {
				    private abstract class Inner<T, K> implements Iterable<K>{}
				    void foo() {
				        for (Iterator<? super List<List<Number>>> iterator = getList()
				                .iterator(); iterator.hasNext();) {
				            Object object = iterator.next();
				           \s
				        }
				    }
				    Inner<? super List<Number>, ? super List<List<Number>>> getList() {
				        return null;
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForMissingParametrization() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collection;
			public class E {
			    void foo(Collection collection) {
			        collection
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "collection";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 6);
			assertCorrectLabels(proposals);

			String[] expected= new String[2];
			expected[0]= """
				package test1;
				import java.util.Collection;
				public class E {
				    void foo(Collection collection) {
				        for (Object object : collection) {
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				import java.util.Collection;
				import java.util.Iterator;
				public class E {
				    void foo(Collection collection) {
				        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
				            Object object = iterator.next();
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForLowVersion() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collection;
			public class E {
			    void foo(Collection collection) {
			        collection
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_4, newOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);

			String selection= "collection";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 5);
			assertProposalDoesNotExist(proposals, CorrectionMessages.QuickAssistProcessor_generate_enhanced_for_loop);
			assertCorrectLabels(proposals);

			String[] expected= new String[1];

			// no generics should be added to iterator since the version is too low
			String str1= """
				package test1;
				import java.util.Collection;
				import java.util.Iterator;
				public class E {
				    void foo(Collection collection) {
				        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
				            Object object = iterator.next();
				           \s
				        }
				    }
				}
				""";
			expected[0]= str1;

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo(String[] array) {
			        array
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "array";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 6);
			assertCorrectLabels(proposals);

			String[] expected= new String[2];
			expected[0]= """
				package test1;
				public class E {
				    void foo(String[] array) {
				        for (int i = 0; i < array.length; i++) {
				            String string = array[i];
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				public class E {
				    void foo(String[] array) {
				        for (String string : array) {
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForMultiDimensionalArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    void foo(String[][] array) {
			        array
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "array";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 6);
			assertCorrectLabels(proposals);

			String[] expected= new String[2];
			expected[0]= """
				package test1;
				public class E {
				    void foo(String[][] array) {
				        for (int i = 0; i < array.length; i++) {
				            String[] name = array[i];
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				public class E {
				    void foo(String[][] array) {
				        for (String[] name : array) {
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForNameClash() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int[] nums;
			    void foo() {
			        nums
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "nums";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 7);
			assertCorrectLabels(proposals);

			String[] expected= new String[2];
			expected[0]= """
				package test1;
				public class E {
				    private int[] nums;
				    void foo() {
				        for (int i = 0; i < nums.length; i++) {
				            int j = nums[i];
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				public class E {
				    private int[] nums;
				    void foo() {
				        for (int i : nums) {
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForImportsAndFormat1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    class Iterator {}
			    void foo() {
			        B.get( /*important: empty*/ );
			    }
			}
			""";
		String str1= """
			package test1;
			import java.util.ArrayList;
			import java.util.Date;
			import java.util.Set;
			public class B {
			    static ArrayList<Date> get() {
			        return new ArrayList<Date>();
			    }
			    static Set raw(int i) {
			        return java.util.Collections.emptySet();
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);
		pack1.createCompilationUnit("B.java", str1, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_5, newOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);

			String selection= "B.get( /*important: empty*/ );";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 5);
			assertCorrectLabels(proposals);

			String[] expected= new String[3];
			expected[0]= """
				package test1;
				
				import java.util.Date;
				
				public class A {
				    class Iterator {}
				    void foo() {
				        for (Date date : B.get( /*important: empty*/ )) {
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				
				import java.util.Date;
				
				public class A {
				    class Iterator {}
				    void foo() {
				        for (java.util.Iterator<Date> iterator = B.get( /*important: empty*/ ).iterator(); iterator
				                .hasNext();) {
				            Date date = iterator.next();
				           \s
				        }
				    }
				}
				""";

			expected[2]= """
				package test1;
				
				import java.util.Date;
				
				public class A {
				    class Iterator {}
				    void foo() {
				        for (int i = 0; i < B.get( /*important: empty*/ ).size(); i++) {
				            Date date = B.get( /*important: empty*/ ).get(i);
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForImportsAndFormat2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    class Object {}
			    class Iterator {}
			    void foo() {
			        B.raw(1+ 2);
			    }
			}
			""";
		String str1= """
			package test1;
			import java.util.ArrayList;
			import java.util.Date;
			import java.util.Set;
			public class B {
			    static ArrayList<Date> get() {
			        return new ArrayList<Date>();
			    }
			    static Set raw(int i) {
			        return java.util.Collections.emptySet();
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);
		pack1.createCompilationUnit("B.java", str1, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_5, newOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);

			String selection= "B.raw(1+ 2);";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 4);
			assertCorrectLabels(proposals);

			String[] expected= new String[2];
			expected[0]= """
				package test1;
				public class A {
				    class Object {}
				    class Iterator {}
				    void foo() {
				        for (java.lang.Object object : B.raw(1+ 2)) {
				           \s
				        }
				    }
				}
				""";

			expected[1]= """
				package test1;
				public class A {
				    class Object {}
				    class Iterator {}
				    void foo() {
				        for (java.util.Iterator iterator = B.raw(1+ 2).iterator(); iterator
				                .hasNext();) {
				            java.lang.Object object = iterator.next();
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testGenerateForImportsArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class A {
			    class Date {}
			    void foo() {
			        B.get();
			    }
			}
			""";
		String str1= """
			package test1;
			import java.util.Date;
			public class B {
			    static Date[] get() {
			        return new Date[1];
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", str, false, null);
		pack1.createCompilationUnit("B.java", str1, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_5, newOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);

			String selection= "B.get();";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 4);
			assertCorrectLabels(proposals);

			String[] expected= new String[1];
			expected[0]= """
				package test1;
				public class A {
				    class Date {}
				    void foo() {
				        for (java.util.Date date : B.get()) {
				           \s
				        }
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}
	@Test
	public void testGenerateForQualified() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collection;
			public class E {
			    Collection<String> collection;
			    void foo(E e) {
			        e.collection
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "collection";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 5);
			assertCorrectLabels(proposals);

			String str1= """
				package test1;
				import java.util.Collection;
				public class E {
				    Collection<String> collection;
				    void foo(E e) {
				        for (String string : e.collection) {
				           \s
				        }
				    }
				}
				""";
			assertProposalPreviewEquals(str1, "Create enhanced 'for' loop", proposals);

			String str2= """
				package test1;
				import java.util.Collection;
				import java.util.Iterator;
				public class E {
				    Collection<String> collection;
				    void foo(E e) {
				        for (Iterator<String> iterator = e.collection.iterator(); iterator
				                .hasNext();) {
				            String string = iterator.next();
				           \s
				        }
				    }
				}
				""";
			assertProposalPreviewEquals(str2, "Create 'for' loop using Iterator", proposals);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}
	@Test
	public void testGenerateForThis() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			import java.util.Collection;
			public class E {
			    Collection<String> collection;
			    void foo() {
			        this.collection
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "collection";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 2);
			assertCorrectLabels(proposals);

			String str1= """
				package test1;
				import java.util.Collection;
				public class E {
				    Collection<String> collection;
				    void foo() {
				        for (String string : this.collection) {
				           \s
				        }
				    }
				}
				""";
			assertProposalPreviewEquals(str1, "Create enhanced 'for' loop", proposals);

			String str2= """
				package test1;
				import java.util.Collection;
				import java.util.Iterator;
				public class E {
				    Collection<String> collection;
				    void foo() {
				        for (Iterator<String> iterator = this.collection.iterator(); iterator
				                .hasNext();) {
				            String string = iterator.next();
				           \s
				        }
				    }
				}
				""";
			assertProposalPreviewEquals(str2, "Create 'for' loop using Iterator", proposals);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testConvertQualifiedNameToStaticImport() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
				static String str;
				static <V> void doIt(V o) {};
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);

		String str1= """
			package test1;
			public class S {
				{
					System.out.println(T.str);
				}
			}
			""";
		ICompilationUnit cu = pack1.createCompilationUnit("S.java", str1, false, null);

		String selection= "str";
		int offset= str1.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			
			import static test1.T.str;
			
			public class S {
				{
					System.out.println(str);
				}
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to static import", proposals);
		assertProposalPreviewEquals(str2, "Convert to static import (replace all occurrences)", proposals);

		selection= "T";
		offset= str1.indexOf(selection);
		context= getCorrectionContext(cu, offset, selection.length());
		proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, "Convert to static import");
		assertProposalDoesNotExist(proposals, "Convert to static import (replace all occurrences)");

		selection= "ystem";
		offset= str1.indexOf(selection);
		context= getCorrectionContext(cu, offset, selection.length());
		proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, "Convert to static import");
		assertProposalDoesNotExist(proposals, "Convert to static import (replace all occurrences)");
	}

	@Test
	public void testConvertMethodInvocationWithTypeToStaticImport() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
				static String str;
				static <V> void doIt(V o) {};
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);

		String str1= """
			package test1;
			public class S {
				{
					T.<String>doIt("");
					T.doIt("");
				}
			}
			""";
		ICompilationUnit cu = pack1.createCompilationUnit("S.java", str1, false, null);

		String selection= "It";
		int offset= str1.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			
			import static test1.T.doIt;
			
			public class S {
				{
					doIt("");
					T.doIt("");
				}
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to static import", proposals);

		String str3= """
			package test1;
			
			import static test1.T.doIt;
			
			public class S {
				{
					doIt("");
					doIt("");
				}
			}
			""";
		assertProposalPreviewEquals(str3, "Convert to static import (replace all occurrences)", proposals);

		selection= "T";
		offset= str1.indexOf(selection);
		context= getCorrectionContext(cu, offset, selection.length());
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
		assertProposalDoesNotExist(proposals, "Convert to static import");
		assertProposalDoesNotExist(proposals, "Convert to static import (replace all occurrences)");
	}

	@Test
	public void testConvertConstantToStaticImport() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
				public static final String FOO = "BAR";
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);

		String str1= """
			package test1;
			public class S {
				{
					public String foo = T.FOO;
				}
			}
			""";
		ICompilationUnit cu = pack1.createCompilationUnit("S.java", str1, false, null);

		String selection= "FOO";
		int offset= str1.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String str2= """
			package test1;
			
			import static test1.T.FOO;
			
			public class S {
				{
					public String foo = FOO;
				}
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to static import", proposals);
		assertProposalPreviewEquals(str2, "Convert to static import (replace all occurrences)", proposals);

		selection= "T";
		offset= str1.indexOf(selection);
		context= getCorrectionContext(cu, offset, selection.length());
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
		assertProposalDoesNotExist(proposals, "Convert to static import");
		assertProposalDoesNotExist(proposals, "Convert to static import (replace all occurrences)");
	}

	@Test
	public void testConvertToStaticImportDoesNotAddImportWhenInScope() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
				static String str;
				String str2 = T.str;
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("T.java", str, false, null);

		String selection= "str";
		int offset= str.lastIndexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String str1= """
			package test1;
			public class T {
				static String str;
				String str2 = str;
			}
			""";
		assertProposalPreviewEquals(str1, "Convert to static import", proposals);
		assertProposalPreviewEquals(str1, "Convert to static import (replace all occurrences)", proposals);

		selection= "T";
		offset= str.lastIndexOf(selection);
		context= getCorrectionContext(cu, offset, selection.length());
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
		assertProposalDoesNotExist(proposals, "Convert to static import");
		assertProposalDoesNotExist(proposals, "Convert to static import (replace all occurrences)");
	}

	@Test
	public void testConvertToStaticImportDoesRemoveUnusedImport() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
				public static final String FOO = "BAR";
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str1= """
			package test2;
			import test1.T
			public class S {
				{
					public String foo = T.FOO;
				}
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("S.java", str1, false, null);

		String selection= "FOO";
		int offset= str1.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String str2= """
			package test2;
			import static test1.T.FOO;
			public class S {
				{
					public String foo = FOO;
				}
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to static import", proposals);

		String str3= """
			package test2;
			import static test1.T.FOO;
			public class S {
				{
					public String foo = FOO;
				}
			}
			""";
		assertProposalPreviewEquals(str3, "Convert to static import (replace all occurrences)", proposals);

		selection= "T";
		offset= str1.indexOf(selection);
		context= getCorrectionContext(cu, offset, selection.length());
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
		assertProposalDoesNotExist(proposals, "Convert to static import");
		assertProposalDoesNotExist(proposals, "Convert to static import (replace all occurrences)");
	}

	@Test
	public void testConvertToStaticImportDoesntRemoveImportWhenReferencedByDifferentReferenceType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
				public static final String FOO = "BAR";
			    public static void bar() {};
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str1= """
			package test2;
			import test1.T
			public class S {
			    public S() {
			        String foo = T.FOO;
			        T.bar();
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("S.java", str1, false, null);

		String selection= "FOO";
		int offset= str1.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String str2= """
			package test2;
			import static test1.T.FOO;
			
			import test1.T
			public class S {
			    public S() {
			        String foo = FOO;
			        T.bar();
			    }
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to static import", proposals);

		String str3= """
			package test2;
			import static test1.T.FOO;
			
			import test1.T
			public class S {
			    public S() {
			        String foo = FOO;
			        T.bar();
			    }
			}
			""";
		assertProposalPreviewEquals(str3, "Convert to static import (replace all occurrences)", proposals);

		selection= "bar";
		offset= str1.indexOf(selection);
		context= getCorrectionContext(cu, offset, selection.length());
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String str4= """
			package test2;
			import static test1.T.bar;
			
			import test1.T
			public class S {
			    public S() {
			        String foo = T.FOO;
			        bar();
			    }
			}
			""";
		assertProposalPreviewEquals(str4, "Convert to static import", proposals);

		String str5= """
			package test2;
			import static test1.T.bar;
			
			import test1.T
			public class S {
			    public S() {
			        String foo = T.FOO;
			        bar();
			    }
			}
			""";
		assertProposalPreviewEquals(str5, "Convert to static import (replace all occurrences)", proposals);

		selection= "T";
		offset= str1.indexOf(selection);
		context= getCorrectionContext(cu, offset, selection.length());
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
		assertProposalDoesNotExist(proposals, "Convert to static import");
		assertProposalDoesNotExist(proposals, "Convert to static import (replace all occurrences)");
	}

	@Test
	public void testConvertToStaticImportDoesntRemoveImportWhenReferencedBySameReferenceType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
				public static final String FOO = "BAR";
				public static final String ZIP = "ZAP";
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str1= """
			package test2;
			import test1.T
			public class S {
			    public S() {
			        String foo = T.FOO;
			        String zip = T.ZIP;
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("S.java", str1, false, null);

		String selection= "FOO";
		int offset= str1.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String str2= """
			package test2;
			import static test1.T.FOO;
			
			import test1.T
			public class S {
			    public S() {
			        String foo = FOO;
			        String zip = T.ZIP;
			    }
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to static import", proposals);

		String str3= """
			package test2;
			import static test1.T.FOO;
			
			import test1.T
			public class S {
			    public S() {
			        String foo = FOO;
			        String zip = T.ZIP;
			    }
			}
			""";
		assertProposalPreviewEquals(str3, "Convert to static import (replace all occurrences)", proposals);

		selection= "T";
		offset= str1.indexOf(selection);
		context= getCorrectionContext(cu, offset, selection.length());
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
		assertProposalDoesNotExist(proposals, "Convert to static import");
		assertProposalDoesNotExist(proposals, "Convert to static import (replace all occurrences)");

		IPackageFragment pack3= fSourceFolder.createPackageFragment("test3", false, null);
		String str4= """
			package test3;
			import test1.T
			public class G {
			    public G() {
			        String foo = T.FOO;
			        String zip = T.FOO;
			    }
			}
			""";
		cu= pack3.createCompilationUnit("G.java", str4, false, null);

		selection= "FOO";
		offset= str4.indexOf(selection);
		context= getCorrectionContext(cu, offset, selection.length());
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		String str5= """
			package test3;
			import static test1.T.FOO;
			
			import test1.T
			public class G {
			    public G() {
			        String foo = FOO;
			        String zip = T.FOO;
			    }
			}
			""";
		assertProposalPreviewEquals(str5, "Convert to static import", proposals);

		String str6= """
			package test3;
			import static test1.T.FOO;
			public class G {
			    public G() {
			        String foo = FOO;
			        String zip = FOO;
			    }
			}
			""";
		assertProposalPreviewEquals(str6, "Convert to static import (replace all occurrences)", proposals);

		selection= "T";
		offset= str4.indexOf(selection);
		context= getCorrectionContext(cu, offset, selection.length());
		proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 0);
		assertProposalDoesNotExist(proposals, "Convert to static import");
		assertProposalDoesNotExist(proposals, "Convert to static import (replace all occurrences)");
	}

	@Test
	public void testCanConvertToStaticImportWhenClassContainsMethodInvocationWithoutExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
			    public static String bar() { return ""; };
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str1= """
			package test2;
			import static test1.T.bar;
			
			import test1.T
			public class S {
			    public S() {
			        String foo1 = T.bar();
			        String foo2 = bar();
			        String foo3 = T.bar();
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("S.java", str1, false, null);

		String selection= "bar";
		int offset= str1.lastIndexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String str2= """
			package test2;
			import static test1.T.bar;
			
			import test1.T
			public class S {
			    public S() {
			        String foo1 = T.bar();
			        String foo2 = bar();
			        String foo3 = bar();
			    }
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to static import", proposals);

		String str3= """
			package test2;
			import static test1.T.bar;
			public class S {
			    public S() {
			        String foo1 = bar();
			        String foo2 = bar();
			        String foo3 = bar();
			    }
			}
			""";
		assertProposalPreviewEquals(str3, "Convert to static import (replace all occurrences)", proposals);

		selection= "T";
		offset= str1.indexOf(selection);
		context= getCorrectionContext(cu, offset, selection.length());
		proposals= collectAssists(context, false);

		assertProposalDoesNotExist(proposals, "Convert to static import");
		assertProposalDoesNotExist(proposals, "Convert to static import (replace all occurrences)");
	}

	@Test
	public void testDoesntRemoveImportWithReferenceFromClassInstanceCreation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
			    public static void foo() { };
			    public void bar() { };
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str1= """
			package test2;
			
			import test1.T;
			public class S {
			    public S() {
			        T.foo();
			        T.foo();
			        new T().foo();
			        new T().bar();
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("S.java", str1, false, null);

		String selection= "foo";
		int offset= str1.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		String str2= """
			package test2;
			
			import static test1.T.foo;
			
			import test1.T;
			public class S {
			    public S() {
			        foo();
			        T.foo();
			        new T().foo();
			        new T().bar();
			    }
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to static import", proposals);

		String str3= """
			package test2;
			
			import static test1.T.foo;
			
			import test1.T;
			public class S {
			    public S() {
			        foo();
			        foo();
			        new T().foo();
			        new T().bar();
			    }
			}
			""";
		assertProposalPreviewEquals(str3, "Convert to static import (replace all occurrences)", proposals);
	}

	@Test
	public void testDoesntOfferConvertToStaticImportForImportDeclarations() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
			    public static int bar = 1;
			    public static void foo() { };
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str1= """
			package test2;
			
			import static test1.T.bar;
			import static test1.T.foo;
			public class S {
			    public S() {
			        foo();
			        System.out.println(bar);
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("S.java", str1, false, null);

		String selection= "bar";
		int offset= str1.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, "Convert to static import");
		assertProposalDoesNotExist(proposals, "Convert to static import (replace all occurrences)");

		selection= "foo";
		offset= str1.indexOf(selection);
		context= getCorrectionContext(cu, offset, selection.length());
		proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);
		assertProposalDoesNotExist(proposals, "Convert to static import");
		assertProposalDoesNotExist(proposals, "Convert to static import (replace all occurrences)");
	}

	@Test
	public void testConvertToStaticImportFromReferenceToSubclass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
			    public static void foo() { };
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);

		String str1= """
			package test1;
			public class TSub extends T {
			}
			""";
		pack1.createCompilationUnit("TSub.java", str1, false, null);


		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str2= """
			package test2;
			
			import test1.TSub;
			public class S {
			    public S() {
			        TSub.foo();
			        TSub.foo();
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("S.java", str2, false, null);

		String selection= "foo";
		int offset= str2.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_static_import);
		assertProposalExists(proposals, CorrectionMessages.QuickAssistProcessor_convert_to_static_import_replace_all);
		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);

		String preview1= """
			package test2;
			
			import static test1.T.foo;
			
			import test1.TSub;
			public class S {
			    public S() {
			        foo();
			        TSub.foo();
			    }
			}
			""";

		String preview2= """
			package test2;
			
			import static test1.T.foo;
			public class S {
			    public S() {
			        foo();
			        foo();
			    }
			}
			""";
		assertExpectedExistInProposals(proposals, new String[] {preview1, preview2});
	}

	@Test
	public void testCreateJUnitTestCase() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String string= "E";
		int offset= str.indexOf(string);
		AssistContext context= getCorrectionContext(cu, offset, string.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 1);
		assertProposalExists(proposals, NLS.bind(CorrectionMessages.QuickAssistProcessor_create_new_junit_test_case, "E.java"));
	}

	@Test
	public void testAssignParameterInnerStatic() throws Exception {
		// assign parameter to field inside inner static nested class, https://bugs.eclipse.org/bugs/show_bug.cgi?id=539476
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class E {
			    private int value;
			    public static class Inner {
			        public Inner (int value) {
			        }
			    }
			    public E (int value) {
			        this.value = value;
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		Map<String, String> saveOptions= fJProject1.getOptions(false);
		Map<String, String> newOptions= new HashMap<>(saveOptions);
		newOptions.put(DefaultCodeFormatterConstants.FORMATTER_PUT_EMPTY_STATEMENT_ON_NEW_LINE, "true");
		try {
			fJProject1.setOptions(newOptions);
			String selection= "Inner (int ";
			AssistContext context= getCorrectionContext(cu, str.lastIndexOf(selection) + selection.length(), 0);
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			String[] expected= new String[1];
			expected[0]= """
				package test1;
				public class E {
				    private int value;
				    public static class Inner {
				        private int value;
				
				        public Inner (int value) {
				            this.value = value;
				        }
				    }
				    public E (int value) {
				        this.value = value;
				    }
				}
				""";

			assertExpectedExistInProposals(proposals, expected);
		} finally {
			fJProject1.setOptions(saveOptions);
		}
	}

	@Test
	public void testCreateNewImplementation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String string= "E";
		int offset= str.indexOf(string);
		AssistContext context= getCorrectionContext(cu, offset, string.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertProposalExists(proposals, NLS.bind(CorrectionMessages.QuickAssistProcessor_create_new_impl, "E.java"));
	}

	@Test
	public void testCreateNewInterfaceImplementation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public interface E {
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str, false, null);

		String string= "E";
		int offset= str.indexOf(string);
		AssistContext context= getCorrectionContext(cu, offset, string.length());
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertNumberOfProposals(proposals, 3);
		assertProposalExists(proposals, NLS.bind(CorrectionMessages.QuickAssistProcessor_create_new_interface_impl, "E.java"));
	}

	@Test
	public void testDoWhileRatherThanWhile1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void replaceWhileByDoWhile(int i) {
			        // Keep this comment
			        while (true) {
			            // Keep this comment too
			            if (i > 100) {
			                return;
			            }
			            i *= 2;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "while (";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void replaceWhileByDoWhile(int i) {
			        // Keep this comment
			        do {
			            // Keep this comment too
			            if (i > 100) {
			                return;
			            }
			            i *= 2;
			        } while (true);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testDoWhileRatherThanWhile2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void replaceWithInitedBoolean(int i) {
			        boolean isInitedToTrue= true;
			
			        // Keep this comment
			        while (isInitedToTrue) {
			           ); Keep this comment too
			            if (i > 100) {
			                isInitedToTrue= false;
			            }
			            i *= 2;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "while (";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void replaceWithInitedBoolean(int i) {
			        boolean isInitedToTrue= true;
			
			        // Keep this comment
			        do {
			           ); Keep this comment too
			            if (i > 100) {
			                isInitedToTrue= false;
			            }
			            i *= 2;
			        } while (isInitedToTrue);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testDoWhileRatherThanWhile3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void replaceWithInitedBooleanAndInteger(int i) {
			        int j= 1000;
			        boolean isInitedToTrue= true;
			
			        // Keep this comment
			        while (isInitedToTrue && j > 0) {
			            // Keep this comment too
			            if (i > 100) {
			                isInitedToTrue= false;
			            }
			            i *= 2;
			            j--;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "while (";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void replaceWithInitedBooleanAndInteger(int i) {
			        int j= 1000;
			        boolean isInitedToTrue= true;
			
			        // Keep this comment
			        do {
			            // Keep this comment too
			            if (i > 100) {
			                isInitedToTrue= false;
			            }
			            i *= 2;
			            j--;
			        } while (isInitedToTrue && j > 0);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testDoWhileRatherThanWhile4() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void replaceWithReassignment(int i) {
			        int j= 1000;
			        int k= -1000;
			        boolean isInitedToTrue= false;
			        isInitedToTrue= k < 0;
			
			        // Keep this comment
			        while (isInitedToTrue && j > 0) {
			            // Keep this comment too
			            if (i > 100) {
			                isInitedToTrue= false;
			            }
			            i *= 2;
			            j--;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "while (";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void replaceWithReassignment(int i) {
			        int j= 1000;
			        int k= -1000;
			        boolean isInitedToTrue= false;
			        isInitedToTrue= k < 0;
			
			        // Keep this comment
			        do {
			            // Keep this comment too
			            if (i > 100) {
			                isInitedToTrue= false;
			            }
			            i *= 2;
			            j--;
			        } while (isInitedToTrue && j > 0);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testDoWhileRatherThanWhile5() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str1= """
			package test1;
			public class E {
			    public void replaceWithInnerWhile(int i) {
			        int j= 1000;
			        int k= -1000;
			        boolean isInitedToTrue= false;
			        isInitedToTrue= k < 0;
			
			        // Keep this comment
			        while (isInitedToTrue && j > 0) {
			            // Keep this comment too
			            while (i < 50 || isInitedToTrue) {
			                ++i;
			            }
			            if (i > 100) {
			                isInitedToTrue= false;
			            }
			            i *= 2;
			            j--;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String str= "while (is";
		AssistContext context= getCorrectionContext(cu, str1.indexOf(str), 0);
		List<IJavaCompletionProposal> proposals= collectAssists(context, false);

		assertCorrectLabels(proposals);

		String expected1= """
			package test1;
			public class E {
			    public void replaceWithInnerWhile(int i) {
			        int j= 1000;
			        int k= -1000;
			        boolean isInitedToTrue= false;
			        isInitedToTrue= k < 0;
			
			        // Keep this comment
			        do {
			            // Keep this comment too
			            while (i < 50 || isInitedToTrue) {
			                ++i;
			            }
			            if (i > 100) {
			                isInitedToTrue= false;
			            }
			            i *= 2;
			            j--;
			        } while (isInitedToTrue && j > 0);
			    }
			}
			""";

		assertExpectedExistInProposals(proposals, new String[] {expected1});
	}

	@Test
	public void testAddStaticFavoritesImportBothMemberAndType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
			    public static void foo() { };
			    public static void bar() { };
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);

		String str1= """
			package test1;
			import static test1.T.foo;\
			public class E {
			    public void x() {
			        foo();
			    }
			}
			""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

		String string= "import";
		int offset= str1.indexOf(string);

		IEditorPart part= JavaUI.openInEditor(cu);
		JavaEditor javaEditor= (JavaEditor) part;
		ISourceViewer viewer= javaEditor.getViewer();
		AssistContext context= new AssistContext(cu, viewer, offset, string.length());

		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		String orig= store.getString(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);

		try {
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);
			assertNumberOfProposals(proposals, 2);
			assertProposalExists(proposals, NLS.bind(CorrectionMessages.QuickAssistProcessor_modify_favorites, "test1.T.foo"));
			assertProposalExists(proposals, NLS.bind(CorrectionMessages.QuickAssistProcessor_modify_favorites, "test1.T.*"));

			assertFalse(orig.contains("test1.T"));

			IJavaCompletionProposal prop= proposals.get(0);
			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			prop.apply(doc);
			String newValue= store.getString(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);

			assertTrue(newValue.contains("test1.T.foo"));
			assertFalse(newValue.contains("test1.T.*"));

			prop= proposals.get(1);
			prop.apply(context.getSourceViewer().getDocument());
			newValue= store.getString(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);

			assertTrue(newValue.contains("test1.T.foo"));
			assertTrue(newValue.contains("test1.T.*"));
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
			store.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, orig);
		}
	}

	@Test
	public void testAddStaticFavoritesMemberAlreadyImported() throws Exception {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		store.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "test1.T.foo");

		String orig= store.getString(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);

		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				public class T {
				    public static void foo() { };
				    public static void bar() { };
				}
				""";
			pack1.createCompilationUnit("T.java", str, false, null);

			String str1= """
				package test1;
				import static test1.T.foo;\
				public class E {
				    public void x() {
				        foo();
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

			IEditorPart part= JavaUI.openInEditor(cu);
			JavaEditor javaEditor= (JavaEditor) part;
			ISourceViewer viewer= javaEditor.getViewer();

			String string= "import";
			int offset= str1.indexOf(string);
			AssistContext context= new AssistContext(cu, viewer, offset, string.length());
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertNumberOfProposals(proposals, 1);
			assertProposalExists(proposals, NLS.bind(CorrectionMessages.QuickAssistProcessor_modify_favorites, "test1.T.*"));

			assertFalse(orig.contains("test1.T.*"));

			IJavaCompletionProposal prop= proposals.get(0);
			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			prop.apply(doc);
			String newValue= store.getString(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);

			assertTrue(newValue.contains("test1.T.*"));
		} finally {
			JavaPlugin.getActivePage().closeAllEditors(false);
			store.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, orig);
		}
	}

	@Test
	public void testAddStaticFavoritesNoNeedToImport() throws Exception {
		IPreferenceStore store= PreferenceConstants.getPreferenceStore();
		store.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS, "test1.T.*");
		try {
			IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
			String str= """
				package test1;
				public class T {
				    public static void foo() { };
				    public static void bar() { };
				}
				""";
			pack1.createCompilationUnit("T.java", str, false, null);

			String str1= """
				package test1;
				import static test1.T.foo;\
				public class E {
				    public void x() {
				        foo();
				    }
				}
				""";
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", str1, false, null);

			String string= "import";
			int offset= str1.indexOf(string);
			AssistContext context= getCorrectionContext(cu, offset, string.length());
			List<IJavaCompletionProposal> proposals= collectAssists(context, false);

			assertProposalDoesNotExist(proposals, NLS.bind(CorrectionMessages.QuickAssistProcessor_modify_favorites, "test1.T.*"));
			assertProposalDoesNotExist(proposals, NLS.bind(CorrectionMessages.QuickAssistProcessor_modify_favorites, "test1.T.foo"));
		} finally {
			store.setToDefault(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);
		}
	}

	@Test
	public void testDoesntRemoveImportWhenClassReferenceIsPresent() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
			    public static void foo() { };
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str1= """
			package test2;
			
			import test1.T;
			
			public class S {
			    public S() {
			        T.foo();
			        System.out.println(T.class);
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("S.java", str1, false, null);
		String selection= "foo";
		int offset= str1.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);
		String str2= """
			package test2;
			
			import static test1.T.foo;
			
			import test1.T;
			
			public class S {
			    public S() {
			        foo();
			        System.out.println(T.class);
			    }
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to static import", proposals);
		assertProposalPreviewEquals(str2, "Convert to static import (replace all occurrences)", proposals);
	}

	@Test
	public void testDoesntRemoveImportWithClassReferenceInSeparateClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String str= """
			package test1;
			public class T {
			    public static void foo() { };
			}
			""";
		pack1.createCompilationUnit("T.java", str, false, null);
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		String str1= """
			package test2;
			
			import test1.T;
			
			public class S {
			    public S() {
			        T.foo();
			    }
			class C {
			    {
			        System.out.println(T.class);
			    }
			}
			""";
		ICompilationUnit cu= pack2.createCompilationUnit("S.java", str1, false, null);
		String selection= "foo";
		int offset= str1.indexOf(selection);
		AssistContext context= getCorrectionContext(cu, offset, selection.length());
		ArrayList<IJavaCompletionProposal> proposals= collectAssists(context, false);
		assertCorrectLabels(proposals);
		assertNumberOfProposals(proposals, 2);
		String str2= """
			package test2;
			
			import static test1.T.foo;
			
			import test1.T;
			
			public class S {
			    public S() {
			        foo();
			    }
			class C {
			    {
			        System.out.println(T.class);
			    }
			}
			""";
		assertProposalPreviewEquals(str2, "Convert to static import", proposals);
		assertProposalPreviewEquals(str2, "Convert to static import (replace all occurrences)", proposals);
	}

}
