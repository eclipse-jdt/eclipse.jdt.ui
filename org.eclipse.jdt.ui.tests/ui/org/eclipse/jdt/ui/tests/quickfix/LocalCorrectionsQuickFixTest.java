/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.LinkedNamesAssistProposal;

public class LocalCorrectionsQuickFixTest extends QuickFixTest {
	
	private static final Class THIS= LocalCorrectionsQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;


	public LocalCorrectionsQuickFixTest(String name) {
		super(name);
	}
	
	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}
	
	public static Test suite() {
		if (true) {
			return allTests();
		}
		return setUpTest(new AdvancedQuickAssistTest("testAssignToLocal1"));
	}


	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getFormatterOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, String.valueOf(99));
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		
		JavaCore.setOptions(options);			

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		
		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.CATCHBLOCK).setPattern("");
		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.CONSTRUCTORSTUB).setPattern("");
		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.METHODSTUB).setPattern("");

		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	
	public void testFieldAccessToStatic() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.File;\n");
		buf.append("public class E {\n");
		buf.append("    public char foo() {\n");
		buf.append("        return (new File(\"x.txt\")).separatorChar;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.File;\n");
		buf.append("public class E {\n");
		buf.append("    public char foo() {\n");
		buf.append("        return File.separatorChar;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testInheritedAccessOnStatic() throws Exception {
		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class A {\n");
		buf.append("    public static void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack0.createCompilationUnit("A.java", buf.toString(), false, null);		
		
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class B extends A {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("B.java", buf.toString(), false, null);		
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import pack.B;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(B b) {\n");
		buf.append("        b.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import pack.B;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(B b) {\n");
		buf.append("        B.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import pack.A;\n");		
		buf.append("import pack.B;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(B b) {\n");
		buf.append("        A.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();		

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });
	}	
	
	
	public void testQualifiedAccessToStatic() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Thread t) {\n");
		buf.append("        t.sleep(10);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Thread t) {\n");
		buf.append("        Thread.sleep(10);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}	
	
	public void testThisAccessToStatic() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static void goo() {\n");
		buf.append("    }\n");				
		buf.append("    public void foo() {\n");
		buf.append("        this.goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static void goo() {\n");
		buf.append("    }\n");				
		buf.append("    public void foo() {\n");
		buf.append("        E.goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void goo() {\n");
		buf.append("    }\n");				
		buf.append("    public void foo() {\n");
		buf.append("        this.goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testThisAccessToStaticField() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static int fCount;\n");
		buf.append("\n");				
		buf.append("    public void foo() {\n");
		buf.append("        this.fCount= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public static int fCount;\n");
		buf.append("\n");				
		buf.append("    public void foo() {\n");
		buf.append("        E.fCount= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int fCount;\n");
		buf.append("\n");				
		buf.append("    public void foo() {\n");
		buf.append("        this.fCount= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
		
	}	
	
	public void testCastMissingInVarDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        Thread th= o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        Thread th= (Thread) o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object o) {\n");
		buf.append("        Object th= o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Thread o) {\n");
		buf.append("        Thread th= o;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}
	
	public void testCastMissingInVarDecl2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");		
		buf.append("public class Container {\n");
		buf.append("    public List[] getLists() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Container.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");				
		buf.append("public class E {\n");
		buf.append("    public void foo(Container c) {\n");
		buf.append("         ArrayList[] lists= c.getLists();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");				
		buf.append("public class E {\n");
		buf.append("    public void foo(Container c) {\n");
		buf.append("         ArrayList[] lists= (ArrayList[]) c.getLists();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");					
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Container c) {\n");
		buf.append("         List[] lists= c.getLists();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");		
		buf.append("import java.util.List;\n");		
		buf.append("public class Container {\n");
		buf.append("    public ArrayList[] getLists() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		

	}
	
	public void testCastMissingInVarDecl3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Thread th= foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 0);
	}	
	
	public void testCastMissingInVarDecl4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");		
		buf.append("public class Container {\n");
		buf.append("    public List getLists()[] {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("Container.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");				
		buf.append("public class E extends Container {\n");
		buf.append("    public void foo() {\n");
		buf.append("         ArrayList[] lists= super.getLists();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");				
		buf.append("public class E extends Container {\n");
		buf.append("    public void foo() {\n");
		buf.append("         ArrayList[] lists= (ArrayList[]) super.getLists();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");					
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E extends Container {\n");
		buf.append("    public void foo() {\n");
		buf.append("         List[] lists= super.getLists();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");		
		buf.append("import java.util.List;\n");		
		buf.append("public class Container {\n");
		buf.append("    public ArrayList[] getLists() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		

	}
	
	
	public void testCastMissingInFieldDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int time= System.currentTimeMillis();\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    int time= (int) System.currentTimeMillis();\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    long time= System.currentTimeMillis();\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}	
	
	public void testCastMissingInAssignment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        String str;\n");
		buf.append("        str= iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        String str;\n");
		buf.append("        str= (String) iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        Object str;\n");
		buf.append("        str= iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		

	}
	
	public void testCastMissingInAssignment2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        String str, str2;\n");
		buf.append("        str= iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        String str, str2;\n");
		buf.append("        str= (String) iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Iterator;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo(Iterator iter) {\n");
		buf.append("        String str2;\n");
		buf.append("        Object str;\n");
		buf.append("        str= iter.next();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		

	}
	
	public void testCastMissingInExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");		
		buf.append("public class E {\n");
		buf.append("    public String[] foo(List list) {\n");
		buf.append("        return list.toArray(new List[list.size()]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 2); // 2nd is type safety warning for toArray
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public String[] foo(List list) {\n");
		buf.append("        return (String[]) list.toArray(new List[list.size()]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public Object[] foo(List list) {\n");
		buf.append("        return list.toArray(new List[list.size()]);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}
	
	public void testCastOnCastExpression() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(List list) {\n");
		buf.append("        ArrayList a= (Cloneable) list;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(List list) {\n");
		buf.append("        ArrayList a= (ArrayList) list;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(List list) {\n");
		buf.append("        Cloneable a= (Cloneable) list;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}
	

	public void testUncaughtException() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void goo() throws IOException {\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");			
		buf.append("public class E {\n");
		buf.append("    public void goo() throws IOException {\n");
		buf.append("    }\n");				
		buf.append("    public void foo() throws IOException {\n");
		buf.append("        goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		 
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");			
		buf.append("public class E {\n");
		buf.append("    public void goo() throws IOException {\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");						
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testUncaughtException2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");		
		buf.append("public class E {\n");
		buf.append("    public String goo() throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    /**\n");
		buf.append("     * Not much to say here.\n");
		buf.append("     */\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo().substring(2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");			
		buf.append("public class E {\n");
		buf.append("    public String goo() throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    /**\n");
		buf.append("     * Not much to say here.\n");
		buf.append("     * @throws IOException\n");
		buf.append("     */\n");
		buf.append("    public void foo() throws IOException {\n");
		buf.append("        goo().substring(2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		 
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");			
		buf.append("public class E {\n");
		buf.append("    public String goo() throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    /**\n");
		buf.append("     * Not much to say here.\n");
		buf.append("     */\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo().substring(2);\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");						
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testUncaughtException3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");	
		buf.append("import java.text.ParseException;\n");	
		buf.append("public class E {\n");
		buf.append("    public String goo() throws IOException, ParseException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    /**\n");
		buf.append("     * Not much to say here.\n");
		buf.append("     * @throws ParseException Parsing failed\n");
		buf.append("     */\n");
		buf.append("    public void foo() throws ParseException {\n");
		buf.append("        goo().substring(2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");	
		buf.append("import java.text.ParseException;\n");	
		buf.append("public class E {\n");
		buf.append("    public String goo() throws IOException, ParseException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    /**\n");
		buf.append("     * Not much to say here.\n");
		buf.append("     * @throws ParseException Parsing failed\n");
		buf.append("     * @throws IOException\n");
		buf.append("     */\n");
		buf.append("    public void foo() throws ParseException, IOException {\n");
		buf.append("        goo().substring(2);\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		 
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");	
		buf.append("import java.text.ParseException;\n");	
		buf.append("public class E {\n");
		buf.append("    public String goo() throws IOException, ParseException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    /**\n");
		buf.append("     * Not much to say here.\n");
		buf.append("     * @throws ParseException Parsing failed\n");
		buf.append("     */\n");
		buf.append("    public void foo() throws ParseException {\n");
		buf.append("        try {\n");		
		buf.append("            goo().substring(2);\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        } catch (ParseException e) {\n");
		buf.append("        }\n");							
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testUncaughtExceptionExtendedSelection() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");	
		buf.append("public class E {\n");
		buf.append("    public String goo(int i) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.out.println(goo(1));\n");
		buf.append("        System.out.println(goo(2));\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		String begin= "goo(1)", end= "goo(2));";
		
		int offset= buf.indexOf(begin);
		int length= buf.indexOf(end) + end.length() - offset;
		AssistContext context= getCorrectionContext(cu, offset, length);
		ArrayList proposals= collectCorrections(cu, astRoot, 2, context);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");	
		buf.append("public class E {\n");
		buf.append("    public String goo(int i) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    public void foo() throws IOException {\n");
		buf.append("        System.out.println(goo(1));\n");
		buf.append("        System.out.println(goo(2));\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		 
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");	
		buf.append("public class E {\n");
		buf.append("    public String goo(int i) throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            System.out.println(goo(1));\n");
		buf.append("            System.out.println(goo(2));\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");							
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}

	
	public void testUncaughtExceptionRemoveMoreSpecific() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.net.SocketException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void goo() throws IOException {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("    /**\n");
		buf.append("     * @throws SocketException Sockets are dangerous\n");
		buf.append("     * @since 3.0\n");
		buf.append("     */\n");
		buf.append("    public void foo() throws SocketException {\n");
		buf.append("        this.goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.net.SocketException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void goo() throws IOException {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("    /**\n");
		buf.append("     * @throws IOException\n");
		buf.append("     * @since 3.0\n");
		buf.append("     */\n");
		buf.append("    public void foo() throws IOException {\n");
		buf.append("        this.goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		 
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.net.SocketException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void goo() throws IOException {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("    /**\n");
		buf.append("     * @throws SocketException Sockets are dangerous\n");
		buf.append("     * @since 3.0\n");
		buf.append("     */\n");
		buf.append("    public void foo() throws SocketException {\n");
		buf.append("        try {\n");		
		buf.append("            this.goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");			
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testUncaughtExceptionToSurroundingTry() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    public static void goo() throws IOException, ParseException {\n");
		buf.append("        return;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");
		buf.append("            E.goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    public static void goo() throws IOException, ParseException {\n");
		buf.append("        return;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() throws ParseException {\n");
		buf.append("        try {\n");
		buf.append("            E.goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		 
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    public static void goo() throws IOException, ParseException {\n");
		buf.append("        return;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");
		buf.append("            try {\n");
		buf.append("                E.goo();\n");
		buf.append("            } catch (ParseException e1) {\n");
		buf.append("            }\n");		
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);
		 
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    public static void goo() throws IOException, ParseException {\n");
		buf.append("        return;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");
		buf.append("            E.goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        } catch (ParseException e) {\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		String expected3= buf.toString();		
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		
	}	
	
	public void testUncaughtExceptionOnSuper1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");		
		buf.append("public class E extends FileInputStream {\n");
		buf.append("    public E() {\n");
		buf.append("        super(\"x\");\n");
		buf.append("    }\n");		
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.FileInputStream;\n");		
		buf.append("import java.io.FileNotFoundException;\n");	
		buf.append("public class E extends FileInputStream {\n");
		buf.append("    public E() throws FileNotFoundException {\n");
		buf.append("        super(\"x\");\n");
		buf.append("    }\n");		
		buf.append("}\n");
		
		assertEqualString(preview, buf.toString());
	}
	
	public void testUncaughtExceptionOnSuper2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public A() throws Exception {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		pack1.createCompilationUnit("A.java", buf.toString(), false, null);		

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends A {\n");
		buf.append("    public E() {\n");
		buf.append("        super();\n");
		buf.append("    }\n");		
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends A {\n");
		buf.append("    public E() throws Exception {\n");
		buf.append("        super();\n");
		buf.append("    }\n");		
		buf.append("}\n");
		
		assertEqualString(preview, buf.toString());
	}	
	
	public void testUncaughtExceptionOnThis() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");		
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        this(null);\n");		
		buf.append("    }\n");
		buf.append("    public E(Object x) throws IOException {\n");
		buf.append("    }\n");				
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");		
		buf.append("public class E {\n");
		buf.append("    public E() throws IOException {\n");
		buf.append("        this(null);\n");		
		buf.append("    }\n");
		buf.append("    public E(Object x) throws IOException {\n");
		buf.append("    }\n");				
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}	
	
	
	boolean BUG_25417= true;
	
	public void testUncaughtExceptionDuplicate() throws Exception {
		if (BUG_25417) {
			return;
		}

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class MyException extends Exception {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("MyException.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void m1() throws IOException {\n");
		buf.append("        m2();\n");
		buf.append("    }\n");		
		buf.append("    public void m2() throws IOException, ParseException, MyException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 2); // 2 uncaught exceptions
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void m1() throws IOException, ParseException, MyException {\n");
		buf.append("        m2();\n");
		buf.append("    }\n");		
		buf.append("    public void m2() throws IOException, ParseException, MyException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		 
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void m1() throws IOException {\n");
		buf.append("        try {\n");
		buf.append("            m2();\n");
		buf.append("        } catch (ParseException e) {\n");
		buf.append("        } catch (MyException e) {\n");		
		buf.append("        }\n");
		buf.append("    }\n");		
		buf.append("    public void m2() throws IOException, ParseException, MyException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}	
	
	public void testMultipleUncaughtExceptions() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");						
		buf.append("public class E {\n");
		buf.append("    public void goo() throws IOException, ParseException {\n");
		buf.append("    }\n");				
		buf.append("    public void foo() {\n");
		buf.append("        goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 2); // 2 uncaught exceptions
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");						
		buf.append("public class E {\n");
		buf.append("    public void goo() throws IOException, ParseException {\n");
		buf.append("    }\n");				
		buf.append("    public void foo() throws IOException, ParseException {\n");
		buf.append("        goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		 
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void goo() throws IOException, ParseException {\n");
		buf.append("    }\n");				
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        } catch (ParseException e) {\n");		
		buf.append("        }\n");						
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}	
	
	public void testUnneededCatchBlock() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");				
		buf.append("public class E {\n");
		buf.append("    public void goo() throws IOException {\n");
		buf.append("    }\n");			
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        } catch (ParseException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");	
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");				
		buf.append("public class E {\n");
		buf.append("    public void goo() throws IOException {\n");
		buf.append("    }\n");			
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");				
		buf.append("public class E {\n");
		buf.append("    public void goo() throws IOException {\n");
		buf.append("    }\n");			
		buf.append("    public void foo() throws ParseException {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}
	
	public void testUnneededCatchBlockInInitializer() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.text.ParseException;\n");				
		buf.append("public class E {\n");
		buf.append("    static {\n");
		buf.append("        try {\n");		
		buf.append("            int x= 1;\n");
		buf.append("        } catch (ParseException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");	
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.text.ParseException;\n");				
		buf.append("public class E {\n");
		buf.append("    static {\n");
		buf.append("        int x= 1;\n");
		buf.append("    }\n");	
		buf.append("}\n");
		String expected1= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });	
	}
	
	public void testUnneededCatchBlockSingle() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void goo() {\n");
		buf.append("    }\n");			
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void goo() {\n");
		buf.append("    }\n");			
		buf.append("    public void foo() {\n");
		buf.append("        goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void goo() {\n");
		buf.append("    }\n");			
		buf.append("    public void foo() throws IOException {\n");
		buf.append("        goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}

	public void testUnneededCatchBlockBug47221() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public Object foo() {\n");
		buf.append("        try {\n");		
		buf.append("            Object o= null;\n");
		buf.append("            return o;\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public Object foo() {\n");
		buf.append("        Object o= null;\n");
		buf.append("        return o;\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public Object foo() throws IOException {\n");
		buf.append("        Object o= null;\n");
		buf.append("        return o;\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });	
	}

	
	public void testUnneededCatchBlockWithFinally() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void goo() {\n");
		buf.append("    }\n");			
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        } finally {\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
	
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void goo() {\n");
		buf.append("    }\n");			
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } finally {\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();

		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void goo() {\n");
		buf.append("    }\n");			
		buf.append("    public void foo() throws IOException {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } finally {\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		

	}
	
	public void testUnimplementedMethods() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Inter {\n");
		buf.append("    int getCount(Object[] o) throws IOException;\n");
		buf.append("}\n");
		pack2.createCompilationUnit("Inter.java", buf.toString(), false, null);

		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");		
		buf.append("public class E implements Inter{\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.Inter;\n");		
		buf.append("public abstract class E implements Inter{\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");		
		buf.append("import test2.Inter;\n");
		buf.append("public class E implements Inter{\n");
		buf.append("\n");
		buf.append("    public int getCount(Object[] o) throws IOException {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
		
	}
	
	public void testUnimplementedMethods2() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public interface Inter {\n");
		buf.append("    int getCount(Object[] o) throws IOException;\n");
		buf.append("}\n");
		pack2.createCompilationUnit("Inter.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public abstract class InterImpl implements Inter {\n");
		buf.append("    protected abstract int[] getMusic() throws IOException;\n");		
		buf.append("}\n");
		pack2.createCompilationUnit("InterImpl.java", buf.toString(), false, null);		

		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.InterImpl;\n");		
		buf.append("public class E extends InterImpl {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot, 2);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.InterImpl;\n");		
		buf.append("public abstract class E extends InterImpl {\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");		
		buf.append("import test2.InterImpl;\n");
		buf.append("public class E extends InterImpl {\n");
		buf.append("\n");
		buf.append("    protected int[] getMusic() throws IOException {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public int getCount(Object[] o) throws IOException {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}
	
	public void testUnimplementedMethods_bug62931() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public interface Inter {\n");
		buf.append("    int foo();\n");
		buf.append("}\n");
		pack2.createCompilationUnit("Inter.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class A {\n");
		buf.append("    int foo() { }\n");	// package visible	
		buf.append("}\n");
		pack2.createCompilationUnit("A.java", buf.toString(), false, null);		

		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.A;\n");	
		buf.append("import test2.Inter;\n");		
		buf.append("public class E extends A implements Inter {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.A;\n");	
		buf.append("import test2.Inter;\n");		
		buf.append("public class E extends A implements Inter {\n");
		buf.append("\n");
		buf.append("    public int foo() {\n");
		buf.append("        return 0;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.A;\n");	
		buf.append("import test2.Inter;\n");		
		buf.append("public abstract class E extends A implements Inter {\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

				
		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });
	}		
	
	public void testUnitializedVariable() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int s;\n");
		buf.append("        try {\n");
		buf.append("            s= 1;\n");
		buf.append("        } catch (Exception e) {\n");
		buf.append("            System.out.println(s);\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int s = 0;\n");
		buf.append("        try {\n");
		buf.append("            s= 1;\n");
		buf.append("        } catch (Exception e) {\n");
		buf.append("            System.out.println(s);\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testUndefinedConstructorInDefaultConstructor1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public F(Runnable runnable) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("F.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends F {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends F {\n");
		buf.append("\n");
		buf.append("    public E(Runnable runnable) {\n");
		buf.append("        super(runnable);\n");		
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUndefinedConstructorInDefaultConstructor2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class F {\n");
		buf.append("    public F(Runnable runnable) throws IOException {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public F(int i, Runnable runnable) {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		pack1.createCompilationUnit("F.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends F {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends F {\n");
		buf.append("\n");
		buf.append("    public E(int i, Runnable runnable) {\n");
		buf.append("        super(i, runnable);\n");		
		buf.append("    }\n");		
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("public class E extends F {\n");
		buf.append("\n");
		buf.append("    public E(Runnable runnable) throws IOException {\n");
		buf.append("        super(runnable);\n");				
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testUndefinedConstructorWithEnclosing1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public class SubF {\n");
		buf.append("        public SubF(int i) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("F.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public class SubE extends F.SubF {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public class SubE extends F.SubF {\n");
		buf.append("\n");
		buf.append("        public SubE(F f, int i) {\n");
		buf.append("            f.super(i);\n");		
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUndefinedConstructorWithEnclosing2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public static class SubF {\n");
		buf.append("        public SubF(int i) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("F.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public class SubE extends F.SubF {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public class SubE extends F.SubF {\n");
		buf.append("\n");
		buf.append("        public SubE(int i) {\n");
		buf.append("            super(i);\n");		
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	
	public void testUndefinedConstructorWithEnclosing3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public static class SubF {\n");
		buf.append("        public SubF(int i) {\n");
		buf.append("        }\n");
		buf.append("        public class SubF2 extends SubF {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("F.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public static class SubF {\n");
		buf.append("        public SubF(int i) {\n");
		buf.append("        }\n");
		buf.append("        public class SubF2 extends SubF {\n");
		buf.append("\n");
		buf.append("            public SubF2(int i) {\n");
		buf.append("                super(i);\n");		
		buf.append("            }\n");	
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");

		assertEqualString(preview, buf.toString());
	}
	
	public void testNotVisibleConstructorInDefaultConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    private F() {\n");
		buf.append("    }\n");
		buf.append("    public F(Runnable runnable) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("F.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends F {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends F {\n");
		buf.append("\n");
		buf.append("    public E(Runnable runnable) {\n");
		buf.append("        super(runnable);\n");		
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnhandledExceptionInDefaultConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class F {\n");
		buf.append("    public F() throws IOException{\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("F.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends F {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("public class E extends F {\n");
		buf.append("\n");
		buf.append("    public E() throws IOException {\n");
		buf.append("        super();\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnusedPrivateField() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int fCount;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}	

	public void testUnusedPrivateField1() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int fCount, fColor= fCount;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int fCount;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnusedPrivateField2() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int fCount= 0;\n");
		buf.append("    public void foo() {\n");
		buf.append("        fCount= 1 + 2;\n");		
		buf.append("    }\n");		
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnusedPrivateField3() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private E e= new E();\n");
		buf.append("    private int value;\n");
		buf.append("    public void foo() {\n");
		buf.append("        value= 0;\n");
		buf.append("        this.value= 0;\n");
		buf.append("        e.value= 0;\n");
		buf.append("        this.e.value= 0;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private E e= new E();\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	
	public void testUnusedVariable() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        boolean res= process();\n");
		buf.append("        res= (super.hashCode() == 1);\n");
		buf.append("    }\n");
		buf.append("    public boolean process() {\n");
		buf.append("        return true;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        res= (super.hashCode() == 1);\n");
		buf.append("    }\n");
		buf.append("    public boolean process() {\n");
		buf.append("        return true;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnusedVariable1() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private Object[] foo() {\n");
		buf.append("        Object[] i, j= new Object[0];\n");
		buf.append("        i= j = null;\n");
		buf.append("        i= (new Object[] { null, null });\n");
		buf.append("        return j;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private Object[] foo() {\n");
		buf.append("        Object[] j= new Object[0];\n");
		buf.append("        j = null;\n");
		buf.append("        return j;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}	

	public void testUnusedVariable2() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private void foo() {\n");
		buf.append("        for (int j= 0, i= 0; i < 3; i++) {\n");
		buf.append("             j= i;\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private void foo() {\n");
		buf.append("        for (int i= 0; i < 3; i++) {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnusedVariable4() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param i\n");
		buf.append("     */\n");
		buf.append("    private void foo(int i) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     */\n");
		buf.append("    private void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnusedParam() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_LOCAL, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PARAMETER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private void foo(Object str) {\n");
		buf.append("        {\n");
		buf.append("            str= toString();\n");
		buf.append("            str= new String[] { toString(), toString() };\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private void foo() {\n");
		buf.append("        {\n");
		buf.append("            str= toString();\n");
		buf.append("            str= new String[] { toString(), toString() };\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	
	public void testUnusedPrivateMethod() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int fCount;\n");
		buf.append(" \n");
		buf.append("    private void foo() {\n");
		buf.append("        fCount= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int fCount;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnusedPrivateConstructor() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append(" \n");
		buf.append("    private E(int i) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnusedPrivateType() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_PRIVATE_MEMBER, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private class F {\n");
		buf.append("    }\n");
		buf.append(" \n");
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}		
	
	public void testUnnecessaryCast1() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        int s = (int) i;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        int s = i;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnnecessaryCast2() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        String r = ((String) s);\n");
		buf.append("    }\n");		
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String s) {\n");
		buf.append("        String r = s;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnnecessaryCast3() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        int s = ((int) 1 + 2) * 3;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        int s = (1 + 2) * 3;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testSuperfluousSemicolon() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_EMPTY_STATEMENT, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        int s= 1;;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        int s= 1;\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testIndirectStaticAccess1() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment other= fSourceFolder.createPackageFragment("other", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package other;\n");
		buf.append("public class A {\n");
		buf.append("    public static final int CONST=1;\n");
		buf.append("}\n");
		other.createCompilationUnit("A.java", buf.toString(), false, null);		

		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack", false, null);
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class B extends other.A {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("B.java", buf.toString(), false, null);		
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import pack.B;\n");
		buf.append("public class E {\n");
		buf.append("    public int foo(B b) {\n");
		buf.append("        return B.CONST;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import other.A;\n");
		buf.append("import pack.B;\n");
		buf.append("public class E {\n");
		buf.append("    public int foo(B b) {\n");
		buf.append("        return A.CONST;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testIndirectStaticAccess2() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment other= fSourceFolder.createPackageFragment("other", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package other;\n");
		buf.append("public class A {\n");
		buf.append("    public static int foo() {\n");
		buf.append("        return 1;\n");	
		buf.append("    }\n");
		buf.append("}\n");
		other.createCompilationUnit("A.java", buf.toString(), false, null);		

		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack", false, null);
		buf= new StringBuffer();
		buf.append("package pack;\n");
		buf.append("public class B extends other.A {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("B.java", buf.toString(), false, null);		
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int foo() {\n");
		buf.append("        return pack.B.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import other.A;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    public int foo() {\n");
		buf.append("        return A.foo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testIndirectStaticAccess_bug40880() throws Exception {
		
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("class FileType {\n");
		buf.append("    public String extension;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("FileType.java", buf.toString(), false, null);		

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface ToolConfigurationSettingsConstants {\n");
		buf.append("     FileType FILE_TYPE = null;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("ToolConfigurationSettingsConstants.java", buf.toString(), false, null);		


		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("interface ToolUserSettingsConstants extends ToolConfigurationSettingsConstants {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("ToolUserSettingsConstants.java", buf.toString(), false, null);		
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        ToolUserSettingsConstants.FILE_TYPE.extension.toLowerCase();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
			

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        ToolConfigurationSettingsConstants.FILE_TYPE.extension.toLowerCase();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testIndirectStaticAccess_bug32022() throws Exception {
		
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_INDIRECT_STATIC_ACCESS, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class StaticField {\n");
		buf.append("    public boolean flag;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("StaticField.java", buf.toString(), false, null);		

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class ConstClass {\n");
		buf.append("     public static StaticField staticField = new StaticField();\n");
		buf.append("}\n");
		pack1.createCompilationUnit("ConstClass.java", buf.toString(), false, null);		

		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(ConstClass constclass) {\n");
		buf.append("        constclass.staticField.flag= true;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(ConstClass constclass) {\n");
		buf.append("        ConstClass.staticField.flag= true;\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class ConstClass {\n");
		buf.append("     public StaticField staticField = new StaticField();\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}
	
	
	public void testUnnecessaryInstanceof1() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(String b) {\n");
		buf.append("        return (b instanceof String);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(String b) {\n");
		buf.append("        return (b != null);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnnecessaryInstanceof2() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_TYPE_CHECK, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String b) {\n");
		buf.append("        if  (b instanceof String && b.getClass() != null) {\n");
		buf.append("            System.out.println();\n");		
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String b) {\n");
		buf.append("        if  (b != null && b.getClass() != null) {\n");
		buf.append("            System.out.println();\n");		
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testUnnecessaryThrownException1() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String b) throws IOException {\n");
		buf.append("        if  (b != null) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(String b) {\n");
		buf.append("        if  (b != null) {\n");
		buf.append("            System.out.println();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnnecessaryThrownException2() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    public E(int i) throws IOException, ParseException {\n");
		buf.append("        if  (i == 0) {\n");
		buf.append("            throw new IOException();\n");	
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    public E(int i) throws IOException {\n");
		buf.append("        if  (i == 0) {\n");
		buf.append("            throw new IOException();\n");	
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnnecessaryThrownException3() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNUSED_DECLARED_THROWN_EXCEPTION, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param i\n");
		buf.append("     * @throws IOException\n");
		buf.append("     * @throws ParseException\n");
		buf.append("     */\n");
		buf.append("    public void foo(int i) throws IOException, ParseException {\n");
		buf.append("        if  (i == 0) {\n");
		buf.append("            throw new IOException();\n");	
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * @param i\n");
		buf.append("     * @throws IOException\n");
		buf.append("     */\n");
		buf.append("    public void foo(int i) throws IOException {\n");
		buf.append("        if  (i == 0) {\n");
		buf.append("            throw new IOException();\n");	
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	
	public void testUnqualifiedFieldAccess1() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int count;\n");
		buf.append("    public E(int i) {\n");
		buf.append("        count= i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int count;\n");
		buf.append("    public E(int i) {\n");
		buf.append("        this.count= i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnqualifiedFieldAccess2() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public int count;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("F.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private F f= new F();\n");
		buf.append("    public E(int i) {\n");
		buf.append("        f.count= i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private F f= new F();\n");
		buf.append("    public E(int i) {\n");
		buf.append("        this.f.count= i;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnqualifiedFieldAccess3() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public void setCount(int i) {}\n");
		buf.append("}\n");
		pack1.createCompilationUnit("F.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private F f= new F();\n");
		buf.append("    public E(int i) {\n");
		buf.append("        f.setCount(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private F f= new F();\n");
		buf.append("    public E(int i) {\n");
		buf.append("        this.f.setCount(i);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnqualifiedFieldAccess4() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int count;\n");
		buf.append("    public E(int i) {\n");
		buf.append("        class Inner {\n");
		buf.append("            public void foo() {\n");
		buf.append("               count= 1;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int count;\n");
		buf.append("    public E(int i) {\n");
		buf.append("        class Inner {\n");
		buf.append("            public void foo() {\n");
		buf.append("               E.this.count= 1;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnqualifiedFieldAccess_bug50960() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNQUALIFIED_FIELD_ACCESS, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    private int count;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("F.java", buf.toString(), false, null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends F {\n");
		buf.append("    private int count;\n");
		buf.append("    public E(int i) {\n");
		buf.append("        count= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends F {\n");
		buf.append("    private int count;\n");
		buf.append("    public E(int i) {\n");
		buf.append("        this.count= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	
	
	public void testHidingVariable1() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int count;\n");
		buf.append("    public void foo() {\n");
		buf.append("       int count= 1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		assertTrue(proposals.get(0) instanceof LinkedNamesAssistProposal);
	}
	
	public void testHidingVariable2() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int count;\n");
		buf.append("    public void foo(int count) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		assertTrue(proposals.get(0) instanceof LinkedNamesAssistProposal);
	}	
	
	public void testHidingVariable3() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int count) {\n");
		buf.append("        class Inner {\n");
		buf.append("            private int count;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		assertTrue(proposals.get(0) instanceof LinkedNamesAssistProposal);
	}
	
	public void testHidingVariable4() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int count;\n");
		buf.append("    public void foo() {\n");
		buf.append("        class Inner {\n");
		buf.append("            private int count;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		assertTrue(proposals.get(0) instanceof LinkedNamesAssistProposal);
	}
	
	public void testHidingVariable5() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int count) {\n");
		buf.append("        class Inner {\n");
		buf.append("            public void foo() {\n");
		buf.append("                 int count;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		assertTrue(proposals.get(0) instanceof LinkedNamesAssistProposal);
	}
	
	public void testHidingVariable6() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int count) {\n");
		buf.append("        class Inner {\n");
		buf.append("            public void foo(int count) {\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		assertTrue(proposals.get(0) instanceof LinkedNamesAssistProposal);
	}
	
	public void testSetParenteses1() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object x) {\n");
		buf.append("        if (!x instanceof Runnable) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(Object x) {\n");
		buf.append("        if (!(x instanceof Runnable)) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
	}
	
	public void testSetParenteses2() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_LOCAL_VARIABLE_HIDING, JavaCore.ERROR);
		hashtable.put(JavaCore.COMPILER_PB_FIELD_HIDING, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(int x) {\n");
		buf.append("        return !x instanceof Runnable || true;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(int x) {\n");
		buf.append("        return !(x instanceof Runnable) || true;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
	}

	public void testUnnecessaryElse1() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_ELSE, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(int x) {\n");
		buf.append("        if (x == 9) {\n");
		buf.append("            return true;\n");
		buf.append("        } else {\n");
		buf.append("            return false;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(int x) {\n");
		buf.append("        if (x == 9) {\n");
		buf.append("            return true;\n");
		buf.append("        }\n");
		buf.append("        return false;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
	}
	public void testUnnecessaryElse2() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_ELSE, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(int x) {\n");
		buf.append("        if (x == 9) {\n");
		buf.append("            return true;\n");
		buf.append("        } else {\n");
		buf.append("            x= 9;\n");	
		buf.append("            return false;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(int x) {\n");
		buf.append("        if (x == 9) {\n");
		buf.append("            return true;\n");
		buf.append("        }\n");
		buf.append("        x= 9;\n");	
		buf.append("        return false;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
	}
	
	public void testUnnecessaryElse3() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_ELSE, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(int x) {\n");
		buf.append("        if (x == 9) {\n");
		buf.append("            return true;\n");
		buf.append("        } else\n");
		buf.append("            return false;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public boolean foo(int x) {\n");
		buf.append("        if (x == 9) {\n");
		buf.append("            return true;\n");
		buf.append("        }\n");
		buf.append("        return false;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
	}
	
	
	public void testInterfaceExtendsClass() throws Exception {
		Hashtable hashtable= JavaCore.getOptions();
		hashtable.put(JavaCore.COMPILER_PB_UNNECESSARY_ELSE, JavaCore.ERROR);
		JavaCore.setOptions(hashtable);
				
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E extends List {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public class E implements List {\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.List;\n");
		buf.append("public interface E extends List {\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });

		
	}
	
}
