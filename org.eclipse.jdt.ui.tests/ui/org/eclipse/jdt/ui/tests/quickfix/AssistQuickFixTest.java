/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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

import org.eclipse.core.runtime.Preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.AssignToVariableAssistProposal;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;

public class AssistQuickFixTest extends QuickFixTest {
	
	private static final Class THIS= AssistQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;


	public AssistQuickFixTest(String name) {
		super(name);
	}


	public static Test suite() {
		if (true) {
			return new TestSuite(THIS);
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new AssistQuickFixTest("testAssignToLocal4"));
			return suite;
		}
	}


	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(JavaCore.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(JavaCore.FORMATTER_TAB_SIZE, "4");
		
		JavaCore.setOptions(options);			

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, false);
		
//		Preferences corePrefs= JavaCore.getPlugin().getPluginPreferences();
	
//		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
//		corePrefs.setValue(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, "fg");
		
		
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}

	
	public void testAssignToLocal() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        getClass();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		int offset= buf.toString().indexOf("getClass()");
		CorrectionContext context= getCorrectionContext(cu, offset, 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
		assertCorrectLabels(proposals);
		
		boolean doField= true, doLocal= true;
		for (int i= 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (!(curr instanceof AssignToVariableAssistProposal)) {
				continue;
			}
			AssignToVariableAssistProposal proposal= (AssignToVariableAssistProposal) curr;
			if (proposal.getVariableKind() == AssignToVariableAssistProposal.FIELD) {
				assertTrue("same proposal kind", doField);
				doField= false;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    private Class class1;\n");
				buf.append("\n");
				buf.append("    public void foo() {\n");
				buf.append("        class1 = getClass();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
				
			} else if (proposal.getVariableKind() == AssignToVariableAssistProposal.LOCAL) {
				assertTrue("same proposal kind", doLocal);
				doLocal= false;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public void foo() {\n");
				buf.append("        Class class1 = getClass();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}
	}
	
	public void testAssignToLocal2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");		
		buf.append("public class E {\n");
		buf.append("    public Vector goo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        goo().iterator();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		int offset= buf.toString().indexOf("goo().iterator()");
		CorrectionContext context= getCorrectionContext(cu, offset, 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
		assertCorrectLabels(proposals);

		boolean doField= true, doLocal= true;
		for (int i= 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (!(curr instanceof AssignToVariableAssistProposal)) {
				continue;
			}			
			AssignToVariableAssistProposal proposal= (AssignToVariableAssistProposal) curr;
			if (proposal.getVariableKind() == AssignToVariableAssistProposal.FIELD) {
				assertTrue("same proposal kind", doField);
				doField= false;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("import java.util.Iterator;\n");
				buf.append("import java.util.Vector;\n");				
				buf.append("public class E {\n");
				buf.append("    private Iterator iterator;\n");
				buf.append("    public Vector goo() {\n");
				buf.append("        return null;\n");
				buf.append("    }\n");		
				buf.append("    public void foo() {\n");
				buf.append("        iterator = goo().iterator();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
				
			} else if (proposal.getVariableKind() == AssignToVariableAssistProposal.LOCAL) {
				assertTrue("same proposal kind", doLocal);
				doLocal= false;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("import java.util.Iterator;\n");
				buf.append("import java.util.Vector;\n");				
				buf.append("public class E {\n");
				buf.append("    public Vector goo() {\n");
				buf.append("        return null;\n");
				buf.append("    }\n");		
				buf.append("    public void foo() {\n");
				buf.append("        Iterator iterator = goo().iterator();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());

			}
		}
	}
	
	public void testAssignToLocal3() throws Exception {
		// test prefixes and this qualification
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_KEYWORD_THIS, true);
		Preferences corePrefs= JavaCore.getPlugin().getPluginPreferences();
		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "f");
		corePrefs.setValue(JavaCore.CODEASSIST_LOCAL_PREFIXES, "_");
			
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    private int fCount;\n");
		buf.append("\n");
		buf.append("    public void foo() {\n");
		buf.append("        System.getSecurityManager();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		int offset= buf.toString().indexOf("System");
		CorrectionContext context= getCorrectionContext(cu, offset, 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
		assertCorrectLabels(proposals);

		boolean doField= true, doLocal= true;
		for (int i= 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (!(curr instanceof AssignToVariableAssistProposal)) {
				continue;
			}			
			AssignToVariableAssistProposal proposal= (AssignToVariableAssistProposal) curr;
			if (proposal.getVariableKind() == AssignToVariableAssistProposal.FIELD) {
				assertTrue("same proposal kind", doField);
				doField= false;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("\n");
				buf.append("    private int fCount;\n");
				buf.append("    private SecurityManager fManager;\n");
				buf.append("\n");				
				buf.append("    public void foo() {\n");
				buf.append("        this.fManager = System.getSecurityManager();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
				
			} else if (proposal.getVariableKind() == AssignToVariableAssistProposal.LOCAL) {
				assertTrue("same proposal kind", doLocal);
				doLocal= false;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("\n");
				buf.append("    private int fCount;\n");
				buf.append("\n");
				buf.append("    public void foo() {\n");
				buf.append("        SecurityManager _manager = System.getSecurityManager();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());

			}
		}
	}
	
	public void testAssignToLocal4() throws Exception {
		// test name conflict
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    private int f;\n");
		buf.append("\n");
		buf.append("    public void foo() {\n");
		buf.append("        Math.min(1.0f, 2.0f);\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
				
		int offset= buf.toString().indexOf("Math");
		CorrectionContext context= getCorrectionContext(cu, offset, 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
		assertCorrectLabels(proposals);

		boolean doField= true, doLocal= true;
		for (int i= 0; i < proposals.size(); i++) {
			Object curr= proposals.get(i);
			if (!(curr instanceof AssignToVariableAssistProposal)) {
				continue;
			}			
			AssignToVariableAssistProposal proposal= (AssignToVariableAssistProposal) curr;
			if (proposal.getVariableKind() == AssignToVariableAssistProposal.FIELD) {
				assertTrue("same proposal kind", doField);
				doField= false;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("\n");
				buf.append("    private int f;\n");
				buf.append("    private float g;\n");
				buf.append("\n");				
				buf.append("    public void foo() {\n");
				buf.append("        g = Math.min(1.0f, 2.0f);\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
				
			} else if (proposal.getVariableKind() == AssignToVariableAssistProposal.LOCAL) {
				assertTrue("same proposal kind", doLocal);
				doLocal= false;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("\n");
				buf.append("    private int f;\n");
				buf.append("\n");
				buf.append("    public void foo() {\n");
				buf.append("        float g = Math.min(1.0f, 2.0f);\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());

			}
		}
	}	
	
	
	public void testAssignToLocal2CursorAtEnd() throws Exception {
//		Preferences corePrefs= JavaCore.getPlugin().getPluginPreferences();
//		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_PREFIXES, "");
//		corePrefs.setValue(JavaCore.CODEASSIST_FIELD_SUFFIXES, "_m");
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");		
		buf.append("public class E {\n");
		buf.append("    public Vector goo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");		
		buf.append("    public void foo() {\n");
		buf.append("        goo().toArray();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "goo().toArray();";
		CorrectionContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		boolean doField= true, doLocal= true;
		for (int i= 0; i < proposals.size(); i++) {
			AssignToVariableAssistProposal proposal= (AssignToVariableAssistProposal) proposals.get(i);
			if (proposal.getVariableKind() == AssignToVariableAssistProposal.FIELD) {
				assertTrue("same proposal kind", doField);
				doField= false;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("import java.util.Vector;\n");				
				buf.append("public class E {\n");
				buf.append("    private Object[] objects;\n");
				buf.append("    public Vector goo() {\n");
				buf.append("        return null;\n");
				buf.append("    }\n");		
				buf.append("    public void foo() {\n");
				buf.append("        objects = goo().toArray();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
				
			} else if (proposal.getVariableKind() == AssignToVariableAssistProposal.LOCAL) {
				assertTrue("same proposal kind", doLocal);
				doLocal= false;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("import java.util.Vector;\n");				
				buf.append("public class E {\n");
				buf.append("    public Vector goo() {\n");
				buf.append("        return null;\n");
				buf.append("    }\n");		
				buf.append("    public void foo() {\n");
				buf.append("        Object[] objects = goo().toArray();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
				
			}
		}
	}
	
	public void testReplaceCatchClauseWithThrowsWithFinally() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");	
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        } finally {\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "(IOException e)";
		CorrectionContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo() throws IOException {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } finally {\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testReplaceSingleCatchClauseWithThrows() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");	
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            goo();\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "(IOException e)";
		CorrectionContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.IOException;\n");		
		buf.append("public class E {\n");
		buf.append("    public void foo() throws IOException {\n");
		buf.append("        goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testUnwrapForLoop() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i= 0; i < 3; i++) {\n");		
		buf.append("            goo();\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "for";
		CorrectionContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testUnwrapDoStatement() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        do {\n");		
		buf.append("            goo();\n");
		buf.append("            goo();\n");
		buf.append("            goo();\n");
		buf.append("        } while (true);\n");	
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "do";
		CorrectionContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo();\n");
		buf.append("        goo();\n");
		buf.append("        goo();\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}	
	
	public void testUnwrapWhileLoop2Statements() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        while (true) {\n");		
		buf.append("            goo();\n");
		buf.append("            System.out.println();\n");		
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "while";
		CorrectionContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        goo();\n");
		buf.append("        System.out.println();\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testUnwrapIfStatement() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        if (1+ 3 == 6) {\n");		
		buf.append("            StringBuffer buf= new StringBuffer();\n");
		buf.append("            buf.append(1);\n");
		buf.append("            buf.append(2);\n");
		buf.append("            buf.append(3);\n");
		buf.append("        }\n");	
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "if";
		CorrectionContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        StringBuffer buf= new StringBuffer();\n");
		buf.append("        buf.append(1);\n");
		buf.append("        buf.append(2);\n");
		buf.append("        buf.append(3);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testUnwrapTryStatement() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        try {\n");		
		buf.append("            StringBuffer buf= new StringBuffer();\n");
		buf.append("            buf.append(1);\n");
		buf.append("            buf.append(2);\n");
		buf.append("            buf.append(3);\n");
		buf.append("        } finally {\n");
		buf.append("            return;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "try";
		CorrectionContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        StringBuffer buf= new StringBuffer();\n");
		buf.append("        buf.append(1);\n");
		buf.append("        buf.append(2);\n");
		buf.append("        buf.append(3);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testUnwrapAnonymous() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() { \n");
		buf.append("                throw new NullPointerException();\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "};";
		CorrectionContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        throw new NullPointerException();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testUnwrapBlock() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        {\n");
		buf.append("            { \n");
		buf.append("                throw new NullPointerException();\n");
		buf.append("            }//comment\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "}//comment";
		CorrectionContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        {\n");
		buf.append("            throw new NullPointerException();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testUnwrapParanthesis() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= (9+ 8);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "(9+ 8)";
		CorrectionContext context= getCorrectionContext(cu, buf.toString().indexOf(str), 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 9+ 8;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}
	
	public void testUnwrapMethodInvocation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= Math.abs(9+ 8);\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		String str= "Math.abs(9+ 8)";
		CorrectionContext context= getCorrectionContext(cu, buf.toString().indexOf(str) + str.length(), 0);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectAssists(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 9+ 8;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());	
	}	
	
	
}
