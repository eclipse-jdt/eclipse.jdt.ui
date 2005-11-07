/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.fix.CleanUpRefactoring;
import org.eclipse.jdt.internal.corext.fix.IFix;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.fix.CodeStyleMultiFix;
import org.eclipse.jdt.internal.ui.fix.IMultiFix;
import org.eclipse.jdt.internal.ui.fix.Java50MultiFix;
import org.eclipse.jdt.internal.ui.fix.StringMultiFix;
import org.eclipse.jdt.internal.ui.fix.UnusedCodeMultiFix;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public class CleanUpTest extends QuickFixTest {
	
	private static final Class THIS= CleanUpTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public CleanUpTest(String name) {
		super(name);
	}
	
	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test suite() {
		return allTests();
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}
	
	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		
		
		JavaCore.setOptions(options);			

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, false);
		
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, "//TODO\n${body_statement}", null);
		
		Preferences corePrefs= JavaCore.getPlugin().getPluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_SUFFIXES, "");
		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, "");		
		
		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}
	
	protected static void assertNumberOfChanges(CompilationUnitChange[] changes, int expectedChanges) {
		if (changes.length != expectedChanges) {
			StringBuffer buf= new StringBuffer();
			buf.append("Wrong number of changes, is: ").append(changes.length). append(", expected: ").append(expectedChanges).append('\n');
			for (int i= 0; i < changes.length; i++) {
				CompilationUnitChange curr= changes[i];
				buf.append(" - ").append(curr.getName()).append(" in ").append(curr.getCompilationUnit().getElementName()).append('\n');
			}
			assertTrue(buf.toString(), false);
		}
	}
	
	public static void assertNumberOfProblems(CompilationUnit unit, int expected) {
		IProblem[] problems= unit.getProblems();
		if (problems.length != expected) {
			StringBuffer buf= new StringBuffer();
			buf.append("Wrong number of problems, is: ").append(problems.length). append(", expected: ").append(expected).append('\n');
			for (int i= 0; i < problems.length; i++) {
				buf.append(" - ").append(problems[i].getMessage()).append('\n');
			}
			assertTrue(buf.toString(), false);
		}
	}
	
	private CompilationUnit[] compile(ICompilationUnit[] units) {
		CompilationUnit[] result= new CompilationUnit[units.length];
		for (int i= 0; i < units.length; i++) {
			result[i]= compile(units[i]);
		}
		return result;
	}
	
	private CompilationUnit compile(ICompilationUnit compilationUnit) {
		CompilationUnit result= JavaPlugin.getDefault().getASTProvider().getAST(compilationUnit, ASTProvider.WAIT_YES, null);
		if (result == null) {
			// see bug 63554
			ASTParser parser= ASTParser.newParser(ASTProvider.AST_LEVEL);
			parser.setSource(compilationUnit);
			parser.setResolveBindings(true);
			result= (CompilationUnit) parser.createAST(null);
		}
		return result;
	}
	
	private void setOptions(IMultiFix fix) {
		setOptions(new IMultiFix[] {fix});
	}
	
	private void setOptions(IMultiFix[] fixes) {
		Hashtable options= JavaCore.getOptions();
		for (Iterator iter= options.keySet().iterator(); iter.hasNext();) {
			String key= (String)iter.next();
			String value= (String)options.get(key);
			if ("error".equals(value) || "warning".equals(value)) {  
				options.put(key, "ignore"); 
			}
		}
		
		for (int i= 0; i < fixes.length; i++) {
			IMultiFix fix= fixes[i];
			Map fixOptions= fix.getRequiredOptions();
			if (fixOptions != null) {
				options.putAll(fixOptions);
			}
		}

		JavaCore.setOptions(options);
	}
	
	private void assertRefactoringResultAsExpected(CleanUpRefactoring refactoring, String[] expected) throws CoreException {
		CompositeChange change= (CompositeChange)refactoring.createChange(null);
		Change[] children= change.getChildren();
		String[] previews= new String[children.length]; 
		for (int i= 0; i < children.length; i++) {
			previews[i]= ((TextChange)children[i]).getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, expected);
	}
	
	private void assertRefactoringHasNoChange(CleanUpRefactoring refactoring) throws CoreException {
		CompositeChange change= (CompositeChange)refactoring.createChange(null);
		Change[] children= change.getChildren();
		StringBuffer buf= new StringBuffer();
		buf.append("Refactoring should generate no changes but does change:\n");
		for (int i= 0; i < children.length; i++) {
			buf.append(((TextChange)children[i]).getPreviewContent(null));
		}
		
		assertTrue(buf.toString(), change.getChildren().length == 0);
	}
	
	public void testAddNLSTag01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s= \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public String s1 = \"\";\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s2 = \"\";\n");
		buf.append("        String s3 = s2 + \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);

		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static final String s= \"\";\n");
		buf.append("    public static String bar(String s1, String s2) {\n");
		buf.append("        bar(\"\", \"\");\n");
		buf.append("        return \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);
		
		IMultiFix multiFix= new StringMultiFix(true, false);
		
		setOptions(multiFix);
		
		CompilationUnit[] units= compile(new ICompilationUnit[] {cu1, cu2, cu3});
		assertNumberOfProblems(units[0], 1);
		assertNumberOfProblems(units[1], 3);
		assertNumberOfProblems(units[2], 4);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s= \"\"; //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public String s1 = \"\"; //$NON-NLS-1$\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s2 = \"\"; //$NON-NLS-1$\n");
		buf.append("        String s3 = s2 + \"\"; //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static final String s= \"\"; //$NON-NLS-1$\n");
		buf.append("    public static String bar(String s1, String s2) {\n");
		buf.append("        bar(\"\", \"\"); //$NON-NLS-1$ //$NON-NLS-2$\n");
		buf.append("        return \"\"; //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		int numberOfFixes= 3;
		int offset= 0;
		String[] previews= new String[numberOfFixes];
		for (int i= offset; i < numberOfFixes; i++) {
			IFix fix= multiFix.createFix(units[i]);
			assertNotNull("There are problems but no fix", fix);
			TextChange change= fix.createChange();
			assertNotNull("Null change for an existing fix", change);
			previews[i - offset]= change.getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2, expected3});	
	}
	
	public void testRemoveNLSTag01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s= null; //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public String s1 = null; //$NON-NLS-1$\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s2 = null; //$NON-NLS-1$\n");
		buf.append("        String s3 = s2 + s2; //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static final String s= null; //$NON-NLS-1$\n");
		buf.append("    public static String bar(String s1, String s2) {\n");
		buf.append("        bar(s2, s1); //$NON-NLS-1$ //$NON-NLS-2$\n");
		buf.append("        return s1; //$NON-NLS-1$\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);

		IMultiFix multiFix= new StringMultiFix(false, true);
		
		setOptions(multiFix);
		
		CompilationUnit[] units= compile(new ICompilationUnit[] {cu1, cu2, cu3});
		assertNumberOfProblems(units[0], 1);
		assertNumberOfProblems(units[1], 3);
		assertNumberOfProblems(units[2], 4);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s= null; \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public String s1 = null; \n");
		buf.append("    public void foo() {\n");
		buf.append("        String s2 = null; \n");
		buf.append("        String s3 = s2 + s2; \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static final String s= null; \n");
		buf.append("    public static String bar(String s1, String s2) {\n");
		buf.append("        bar(s2, s1); \n");
		buf.append("        return s1; \n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		int numberOfFixes= 3;
		int offset= 0;
		String[] previews= new String[numberOfFixes];
		for (int i= offset; i < numberOfFixes; i++) {
			IFix fix= multiFix.createFix(units[i]);
			assertNotNull("There are problems but no fix", fix);
			TextChange change= fix.createChange();
			assertNotNull("Null change for an existing fix", change);
			previews[i - offset]= change.getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2, expected3});	
	}

	public void testUnusedCode01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.*;\n");
		buf.append("public class E2 {\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import java.util.HashMap;\n");
		buf.append("import test1.E2;\n");
		buf.append("import java.io.StringReader;\n");
		buf.append("import java.util.HashMap;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);
		
		IMultiFix multiFix= new UnusedCodeMultiFix(true, false, false, false, false, false);
		
		setOptions(multiFix);
		
		CompilationUnit[] units= compile(new ICompilationUnit[] {cu1, cu2, cu3});
		assertNumberOfProblems(units[0], 1);
		assertNumberOfProblems(units[1], 2);
		assertNumberOfProblems(units[2], 3);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("}\n");
		String expected3= buf.toString();

		int numberOfFixes= 3;
		int offset= 0;
		String[] previews= new String[numberOfFixes];
		for (int i= offset; i < numberOfFixes; i++) {
			IFix fix= multiFix.createFix(units[i]);
			assertNotNull("There are problems but no fix", fix);
			TextChange change= fix.createChange();
			assertNotNull("Null change for an existing fix", change);
			previews[i - offset]= change.getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2, expected3});	
	}
	
	public void testUnusedCode02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private void foo() {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private void foo() {}\n");
		buf.append("    private void bar() {}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    private class E3Inner {\n");
		buf.append("        private void foo() {}\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {}\n");
		buf.append("            private void foo() {};\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);
		
		IMultiFix multiFix= new UnusedCodeMultiFix(false, true, false, false, false, false);
		
		setOptions(multiFix);
		
		CompilationUnit[] units= compile(new ICompilationUnit[] {cu1, cu2, cu3});
		assertNumberOfProblems(units[0], 1);
		assertNumberOfProblems(units[1], 2);
		assertNumberOfProblems(units[2], 3);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    private class E3Inner {\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable r= new Runnable() {\n");
		buf.append("            public void run() {};\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		int numberOfFixes= 3;
		int offset= 0;
		String[] previews= new String[numberOfFixes];
		for (int i= offset; i < numberOfFixes; i++) {
			IFix fix= multiFix.createFix(units[i]);
			assertNotNull("There are problems but no fix", fix);
			TextChange change= fix.createChange();
			assertNotNull("Null change for an existing fix", change);
			previews[i - offset]= change.getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2, expected3});	
	}
	
	public void testUnusedCode03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private E1(int i) {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public E2() {}\n");
		buf.append("    private E2(int i) {}\n");
		buf.append("    private E2(String s) {}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public class E3Inner {\n");
		buf.append("        private E3Inner(int i) {}\n");
		buf.append("    }\n");
		buf.append("    private void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);
		
		IMultiFix multiFix= new UnusedCodeMultiFix(false, false, true, false, false, false);
		
		setOptions(multiFix);
		
		CompilationUnit[] units= compile(new ICompilationUnit[] {cu1, cu2, cu3});
		assertNumberOfProblems(units[0], 1);
		assertNumberOfProblems(units[1], 2);
		assertNumberOfProblems(units[2], 2);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public E2() {}\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public class E3Inner {\n");
		buf.append("    }\n");
		buf.append("    private void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		int numberOfFixes= 3;
		int offset= 0;
		String[] previews= new String[numberOfFixes];
		for (int i= offset; i < numberOfFixes; i++) {
			IFix fix= multiFix.createFix(units[i]);
			assertNotNull("There are problems but no fix", fix);
			TextChange change= fix.createChange();
			assertNotNull("Null change for an existing fix", change);
			previews[i - offset]= change.getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2, expected3});	
	}
	
	public void testUnusedCode04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private int i= 10;\n");
		buf.append("    private int j= 10;\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    private int i;\n");
		buf.append("    private int j;\n");
		buf.append("    private void foo() {\n");
		buf.append("        i= 10;\n");
		buf.append("        i= 20;\n");
		buf.append("        i= j;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);
		
		IMultiFix multiFix= new UnusedCodeMultiFix(false, false, false, true, false, false);
		
		setOptions(multiFix);
		
		CompilationUnit[] units= compile(new ICompilationUnit[] {cu1, cu2, cu3});
		assertNumberOfProblems(units[0], 1);
		assertNumberOfProblems(units[1], 2);
		assertNumberOfProblems(units[2], 2);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    private int j;\n");
		buf.append("    private void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		int numberOfFixes= 3;
		int offset= 0;
		String[] previews= new String[numberOfFixes];
		for (int i= offset; i < numberOfFixes; i++) {
			IFix fix= multiFix.createFix(units[i]);
			assertNotNull("There are problems but no fix", fix);
			TextChange change= fix.createChange();
			assertNotNull("Null change for an existing fix", change);
			previews[i - offset]= change.getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2, expected3});	
	}
	
	public void testUnusedCode05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    private class E1Inner{}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private class E2Inner1 {}\n");
		buf.append("    private class E2Inner2 {}\n");
		buf.append("    public class E2Inner3 {}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public class E3Inner {\n");
		buf.append("        private class E3InnerInner {}\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);
		
		IMultiFix multiFix= new UnusedCodeMultiFix(false, false, false, false, true, false);
		
		setOptions(multiFix);
		
		CompilationUnit[] units= compile(new ICompilationUnit[] {cu1, cu2, cu3});
		assertNumberOfProblems(units[0], 1);
		assertNumberOfProblems(units[1], 2);
		assertNumberOfProblems(units[2], 1);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public class E2Inner3 {}\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public class E3Inner {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		int numberOfFixes= 3;
		int offset= 0;
		String[] previews= new String[numberOfFixes];
		for (int i= offset; i < numberOfFixes; i++) {
			IFix fix= multiFix.createFix(units[i]);
			assertNotNull("There are problems but no fix", fix);
			TextChange change= fix.createChange();
			assertNotNull("Null change for an existing fix", change);
			previews[i - offset]= change.getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2, expected3});	
	}
	
	public void testUnusedCode06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 10;\n");
		buf.append("        int j= 10;\n");
		buf.append("    }\n");
		buf.append("    private void bar() {\n");
		buf.append("        int i= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public class E3Inner {\n");
		buf.append("        public void foo() {\n");
		buf.append("            int i= 10;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 10;\n");
		buf.append("        int j= i;\n");
		buf.append("        j= 10;\n");
		buf.append("        j= 20;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);
		
		IMultiFix multiFix= new UnusedCodeMultiFix(false, false, false, false, false, true);
		
		setOptions(multiFix);
		
		CompilationUnit[] units= compile(new ICompilationUnit[] {cu1, cu2, cu3});
		assertNumberOfProblems(units[0], 1);
		assertNumberOfProblems(units[1], 3);
		assertNumberOfProblems(units[2], 2);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    private void bar() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class E3 {\n");
		buf.append("    public class E3Inner {\n");
		buf.append("        public void foo() {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 10;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();

		int numberOfFixes= 3;
		int offset= 0;
		String[] previews= new String[numberOfFixes];
		for (int i= offset; i < numberOfFixes; i++) {
			IFix fix= multiFix.createFix(units[i]);
			assertNotNull("There are problems but no fix", fix);
			TextChange change= fix.createChange();
			assertNotNull("Null change for an existing fix", change);
			previews[i - offset]= change.getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2, expected3});	
	}
	
	public void testAddDeprecatedAnnotation01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private int field= 1;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private int field1= 1;\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private int field2= 2;\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		IMultiFix multiFix= new Java50MultiFix(false, true);
		
		setOptions(multiFix);
		
		CompilationUnit[] units= compile(new ICompilationUnit[] {cu1, cu2});
		assertNumberOfProblems(units[0], 1);
		assertNumberOfProblems(units[1], 2);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private int field= 1;\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private int field1= 1;\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private int field2= 2;\n");
		buf.append("}\n");
		String expected2= buf.toString();

		int numberOfFixes= 2;
		int offset= 0;
		String[] previews= new String[numberOfFixes];
		for (int i= offset; i < numberOfFixes; i++) {
			IFix fix= multiFix.createFix(units[i]);
			assertNotNull("There are problems but no fix", fix);
			TextChange change= fix.createChange();
			assertNotNull("Null change for an existing fix", change);
			previews[i - offset]= change.getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2});	
	}
	
	public void testAddDeprecatedAnnotation02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private int f() {return 1;}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private int f1() {return 1;}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private int f2() {return 2;}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		IMultiFix multiFix= new Java50MultiFix(false, true);
		
		setOptions(multiFix);
		
		CompilationUnit[] units= compile(new ICompilationUnit[] {cu1, cu2});
		assertNumberOfProblems(units[0], 1);
		assertNumberOfProblems(units[1], 2);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private int f() {return 1;}\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private int f1() {return 1;}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private int f2() {return 2;}\n");
		buf.append("}\n");
		String expected2= buf.toString();

		int numberOfFixes= 2;
		int offset= 0;
		String[] previews= new String[numberOfFixes];
		for (int i= offset; i < numberOfFixes; i++) {
			IFix fix= multiFix.createFix(units[i]);
			assertNotNull("There are problems but no fix", fix);
			TextChange change= fix.createChange();
			assertNotNull("Null change for an existing fix", change);
			previews[i - offset]= change.getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2});	
	}
	
	public void testAddDeprecatedAnnotation03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * @deprecated\n");
		buf.append(" */\n");
		buf.append("public class E1 {\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private class E2Sub1 {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    private class E2Sub2 {}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		IMultiFix multiFix= new Java50MultiFix(false, true);
		
		setOptions(multiFix);
		
		CompilationUnit[] units= compile(new ICompilationUnit[] {cu1, cu2});
		assertNumberOfProblems(units[0], 1);
		assertNumberOfProblems(units[1], 2);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");
		buf.append(" * @deprecated\n");
		buf.append(" */\n");
		buf.append("@Deprecated\n");
		buf.append("public class E1 {\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private class E2Sub1 {}\n");
		buf.append("    /**\n");
		buf.append("     * @deprecated\n");
		buf.append("     */\n");
		buf.append("    @Deprecated\n");
		buf.append("    private class E2Sub2 {}\n");
		buf.append("}\n");
		String expected2= buf.toString();

		int numberOfFixes= 2;
		int offset= 0;
		String[] previews= new String[numberOfFixes];
		for (int i= offset; i < numberOfFixes; i++) {
			IFix fix= multiFix.createFix(units[i]);
			assertNotNull("There are problems but no fix", fix);
			TextChange change= fix.createChange();
			assertNotNull("Null change for an existing fix", change);
			previews[i - offset]= change.getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2});
	}
	
	public void testCodeStyle01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    String s = \"\"; //$NON-NLS-1$\n");
		buf.append("    String t = \"\";  //$NON-NLS-1$\n");
		buf.append("    \n");
		buf.append("    public void foo() {\n");
		buf.append("        s = \"\"; //$NON-NLS-1$\n");
		buf.append("        s = s + s;\n");
		buf.append("        s = t + s;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    int i = 10;\n");
		buf.append("    \n");
		buf.append("    public class E2Inner {\n");
		buf.append("        public void bar() {\n");
		buf.append("            int j = i;\n");
		buf.append("            String k = s + t;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    public void fooBar() {\n");
		buf.append("        String k = s;\n");
		buf.append("        int j = i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		
		IMultiFix multiFix= new CodeStyleMultiFix(true, false);
		refactoring.addMultiFix(multiFix);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    String s = \"\"; //$NON-NLS-1$\n");
		buf.append("    String t = \"\";  //$NON-NLS-1$\n");
		buf.append("    \n");
		buf.append("    public void foo() {\n");
		buf.append("        this.s = \"\"; //$NON-NLS-1$\n");
		buf.append("        this.s = this.s + this.s;\n");
		buf.append("        this.s = this.t + this.s;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1{\n");
		buf.append("    int i = 10;\n");
		buf.append("    \n");
		buf.append("    public class E2Inner {\n");
		buf.append("        public void bar() {\n");
		buf.append("            int j = E2.this.i;\n");
		buf.append("            String k = E2.this.s + E2.this.t;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    \n");
		buf.append("    public void fooBar() {\n");
		buf.append("        String k = this.s;\n");
		buf.append("        int j = this.i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2});
	}
	
	public void testCodeStyle02() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int i= 0;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private E1 e1;\n");
		buf.append("    \n");
		buf.append("    public void foo() {\n");
		buf.append("        e1= new E1();\n");
		buf.append("        int j= e1.i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		
		IMultiFix multiFix= new CodeStyleMultiFix(true, true);
		refactoring.addMultiFix(multiFix);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    private E1 e1;\n");
		buf.append("    \n");
		buf.append("    public void foo() {\n");
		buf.append("        this.e1= new E1();\n");
		buf.append("        int j= E1.i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}
	
	public void testCodeStyle03() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int f;\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= this.f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public static String s = \"\";\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(this.s);\n");
		buf.append("        E1 e1= new E1();\n");
		buf.append("        int i= e1.f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static int g;\n");
		buf.append("    {\n");
		buf.append("        this.g= (new E1()).f;\n");
		buf.append("    }\n");
		buf.append("    public static int f= E1.f;\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);
		
		IMultiFix multiFix= new CodeStyleMultiFix(false, true);
		
		setOptions(multiFix);
		
		CompilationUnit[] units= compile(new ICompilationUnit[] {cu1, cu2, cu3});
		assertNumberOfProblems(units[0], 1);
		assertNumberOfProblems(units[1], 2);
		assertNumberOfProblems(units[2], 2);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int f;\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= E1.f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public static String s = \"\";\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E2.s);\n");
		buf.append("        E1 e1= new E1();\n");
		buf.append("        int i= E1.f;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static int g;\n");
		buf.append("    {\n");
		buf.append("        E3.g= E1.f;\n");
		buf.append("    }\n");
		buf.append("    public static int f= E1.f;\n");
		buf.append("}\n");
		String expected3= buf.toString();

		int numberOfFixes= 3;
		int offset= 0;
		String[] previews= new String[numberOfFixes];
		for (int i= offset; i < numberOfFixes; i++) {
			IFix fix= multiFix.createFix(units[i]);
			assertNotNull("There are problems but no fix", fix);
			TextChange change= fix.createChange();
			assertNotNull("Null change for an existing fix", change);
			previews[i - offset]= change.getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, new String[] { expected1, expected2, expected3});
	}
	
	public void testCodeStyle04() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int f() {return 1;}\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= this.f();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public static String s() {return \"\";}\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(this.s());\n");
		buf.append("        E1 e1= new E1();\n");
		buf.append("        int i= e1.f();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static int g;\n");
		buf.append("    {\n");
		buf.append("        this.g= (new E1()).f();\n");
		buf.append("    }\n");
		buf.append("    public static int f= E1.f();\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		IMultiFix multiFix= new CodeStyleMultiFix(false, true);
		refactoring.addMultiFix(multiFix);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int f() {return 1;}\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= E1.f();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 {\n");
		buf.append("    public static String s() {return \"\";}\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E2.s());\n");
		buf.append("        E1 e1= new E1();\n");
		buf.append("        int i= E1.f();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 extends E2 {\n");
		buf.append("    public static int g;\n");
		buf.append("    {\n");
		buf.append("        E3.g= E1.f();\n");
		buf.append("    }\n");
		buf.append("    public static int f= E1.f();\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1, expected2, expected3});

	}
	
	public void testCodeStyle05() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public String s= \"\";\n");
		buf.append("    public E2 e2;\n");
		buf.append("    public static int i= 10;\n");
		buf.append("    public void foo() {}\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E2 extends E1 {\n");
		buf.append("    public int i = 10;\n");
		buf.append("    public E1 e1;\n");
		buf.append("    public void fooBar() {}\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("E2.java", buf.toString(), false, null);
		
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 {\n");
		buf.append("    private E1 e1;    \n");
		buf.append("    public void foo() {\n");
		buf.append("        e1= new E1();\n");
		buf.append("        int j= e1.i;\n");
		buf.append("        String s= e1.s;\n");
		buf.append("        e1.foo();\n");
		buf.append("        e1.e2.fooBar();\n");
		buf.append("        int k= e1.e2.e2.e2.i;\n");
		buf.append("        int h= e1.e2.e2.e1.e2.e1.i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu3= pack2.createCompilationUnit("E3.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		refactoring.addCompilationUnit(cu2);
		refactoring.addCompilationUnit(cu3);
		
		IMultiFix multiFix= new CodeStyleMultiFix(true, true);
		refactoring.addMultiFix(multiFix);
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import test1.E1;\n");
		buf.append("import test1.E2;\n");
		buf.append("public class E3 {\n");
		buf.append("    private E1 e1;    \n");
		buf.append("    public void foo() {\n");
		buf.append("        this.e1= new E1();\n");
		buf.append("        int j= E1.i;\n");
		buf.append("        String s= this.e1.s;\n");
		buf.append("        this.e1.foo();\n");
		buf.append("        this.e1.e2.fooBar();\n");
		buf.append("        int k= this.e1.e2.e2.e2.i;\n");
		buf.append("        int h= E1.i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		String[] expected= new String[] {expected1};
		
		assertRefactoringResultAsExpected(refactoring, expected);
	}
	
	public void testCodeStyle06() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public String s= \"\";\n");
		buf.append("    public E1 create() {\n");
		buf.append("        return new E1();\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        create().s= \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		IMultiFix multiFix= new CodeStyleMultiFix(true, true);
		refactoring.addMultiFix(multiFix);
		
		assertRefactoringHasNoChange(refactoring);
	}
	
	public void testCodeStyle07() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public static int i = 10;\n");
		buf.append("    private static int j = i + 10 * i;\n");
		buf.append("    public void foo() {\n");
		buf.append("        String s= i + \"\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		IMultiFix multiFix= new CodeStyleMultiFix(true, true);
		refactoring.addMultiFix(multiFix);
		
		assertRefactoringHasNoChange(refactoring);
	}
	
	public void testCodeStyle08() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public final static int i = 1;\n");
		buf.append("    public final int j = 2;\n");
		buf.append("    private final int k = 3;\n");
		buf.append("    public void foo() {\n");
		buf.append("        switch (3) {\n");
		buf.append("        case i: break;\n");
		buf.append("        case j: break;\n");
		buf.append("        case k: break;\n");
		buf.append("        default: break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		IMultiFix multiFix= new CodeStyleMultiFix(true, true);
		refactoring.addMultiFix(multiFix);
		
		assertRefactoringHasNoChange(refactoring);
	}
	
	public void testCodeStyle09() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public abstract class E1Inner1 {\n");
		buf.append("        protected int n;\n");
		buf.append("        public abstract void foo();\n");
		buf.append("    }\n");
		buf.append("    public abstract class E1Inner2 {\n");
		buf.append("        public abstract void run();\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        E1Inner1 inner= new E1Inner1() {\n");
		buf.append("            public void foo() {\n");
		buf.append("                E1Inner2 inner2= new E1Inner2() {\n");
		buf.append("                    public void run() {\n");
		buf.append("                        System.out.println(n);\n");
		buf.append("                    }\n");
		buf.append("                };\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		IMultiFix multiFix= new CodeStyleMultiFix(true, true);
		refactoring.addMultiFix(multiFix);
		
		assertRefactoringHasNoChange(refactoring);
	}

	public void testCodeStyleBug114544() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(new E1().i);\n");
		buf.append("    }\n");
		buf.append("    public static int i= 10;\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		IMultiFix multiFix= new CodeStyleMultiFix(false, true);
		
		setOptions(multiFix);
		
		CompilationUnit[] units= compile(new ICompilationUnit[] {cu1});
		assertNumberOfProblems(units[0], 1);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E1 {\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(E1.i);\n");
		buf.append("    }\n");
		buf.append("    public static int i= 10;\n");
		buf.append("}\n");
		String expected1= buf.toString();

		int numberOfFixes= 1;
		int offset= 0;
		String[] previews= new String[numberOfFixes];
		for (int i= offset; i < numberOfFixes; i++) {
			IFix fix= multiFix.createFix(units[i]);
			assertNotNull("There are problems but no fix", fix);
			TextChange change= fix.createChange();
			assertNotNull("Null change for an existing fix", change);
			previews[i - offset]= change.getPreviewContent(null);
		}

		assertEqualStringsIgnoreOrder(previews, new String[] { expected1});
	}
	
	public void testCombination01() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class E1 {\n");
		buf.append("    private int i= 10;\n");
		buf.append("    private int j= 20;\n");
		buf.append("    \n");
		buf.append("    public void foo() {\n");
		buf.append("        i= j;\n");
		buf.append("        i= 20;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E1.java", buf.toString(), false, null);
		
		CleanUpRefactoring refactoring= new CleanUpRefactoring();
		refactoring.addCompilationUnit(cu1);
		
		IMultiFix multiFix1= new CodeStyleMultiFix(true, false);
		IMultiFix multiFix2= new UnusedCodeMultiFix(false, false, false, true, false, false);
		refactoring.addMultiFix(multiFix1);
		refactoring.addMultiFix(multiFix2);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class E1 {\n");
		buf.append("    private int j= 20;\n");
		buf.append("    \n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertRefactoringResultAsExpected(refactoring, new String[] {expected1});
	}
	
}
