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
package org.eclipse.jdt.ui.tests.core;

import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal;
import org.eclipse.jdt.internal.ui.text.java.ResultCollector;

public class CodeCompletionTest extends CoreTests {
	
	private static final Class THIS= CodeCompletionTest.class;
	
	private IJavaProject fJProject1;

	public CodeCompletionTest(String name) {
		super(name);
	}
	
	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new CodeCompletionTest("test1"));
			return new ProjectTestSetup(suite);
		}	
	}


	protected void setUp() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		JavaProjectHelper.addRequiredProject(fJProject1, ProjectTestSetup.getProject());

		Hashtable options= JavaCore.getDefaultOptions();  
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(JavaCore.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(JavaCore.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, true);

		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.OVERRIDECOMMENT).setPattern("/* (non-Javadoc)\n * ${see_to_overridden}\n */");	
		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.METHODSTUB).setPattern("//TODO\n${body_statement}");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}
	
	
	public void testAnonymousTypeCompletion1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run= new Runnable(\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();
		
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		IEditorPart part= EditorUtility.openInEditor(cu);
		try {
			String str= "Runnable run= new Runnable(";
	
			int offset= contents.indexOf(str) + str.length();
	
			ResultCollector collector= new ResultCollector();
			collector.reset(offset, cu.getJavaProject(), cu);
			collector.setViewer(null);
			collector.setReplacementLength(0);
			collector.setPreventEating(true);
			
			cu.codeComplete(offset, collector);
			
			JavaCompletionProposal[] proposals= collector.getResults();
			
			assertNumberOf("proposals", proposals.length, 1);
			
			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());

			
			proposals[0].apply(doc);
	
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class A {\n");
			buf.append("    public void foo() {\n");
			buf.append("        Runnable run= new Runnable() {\n");
			buf.append("            public void run() {\n");
			buf.append("                //TODO\n");
			buf.append("\n");
			buf.append("            }\n");
			buf.append("        }\n");		
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(doc.get(), buf.toString());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}
	
	public void testAnonymousTypeCompletion2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class A {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run= new Runnable();\n");
		buf.append("    }\n");
		buf.append("}\n");
		String contents= buf.toString();
		
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);
		
		IEditorPart part= EditorUtility.openInEditor(cu);
		try {
			String str= "Runnable run= new Runnable(";
	
			int offset= contents.indexOf(str) + str.length();
	
			ResultCollector collector= new ResultCollector();
			collector.reset(offset, cu.getJavaProject(), cu);
			collector.setViewer(null);
			collector.setReplacementLength(0);
			collector.setPreventEating(true);
			
			cu.codeComplete(offset, collector);
			
			JavaCompletionProposal[] proposals= collector.getResults();
			
			assertNumberOf("proposals", proposals.length, 1);
			
			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			
			proposals[0].apply(doc);
	
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class A {\n");
			buf.append("    public void foo() {\n");
			buf.append("        Runnable run= new Runnable() {\n");
			buf.append("            public void run() {\n");
			buf.append("                //TODO\n");
			buf.append("\n");
			buf.append("            }\n");
			buf.append("        };\n");		
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(doc.get(), buf.toString());
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
	}	


	public void testMethodStubCompletion1() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.Writer;\n");
		buf.append("\n");
		buf.append("public class A extends Writer {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    //here\n");
		buf.append("}\n");
		String contents= buf.toString();
		
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		IEditorPart part= EditorUtility.openInEditor(cu);
		try {
			String str= "//here";

			int offset= contents.indexOf(str);

			ResultCollector collector= new ResultCollector();
			collector.reset(offset, cu.getJavaProject(), cu);
			collector.setViewer(null);
			collector.setReplacementLength(0);
			collector.setPreventEating(true);

			cu.codeComplete(offset, collector);

			JavaCompletionProposal[] proposals= collector.getResults();

			JavaCompletionProposal toStringProposal= null;

			for (int i= 0; i < proposals.length; i++) {
				if (proposals[i].getDisplayString().startsWith("toString()")) {
					toStringProposal= proposals[i];
				}
			}
			assertNotNull("no proposal for toString()", toStringProposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());

			toStringProposal.apply(doc);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import java.io.Writer;\n");
			buf.append("\n");
			buf.append("public class A extends Writer {\n");
			buf.append("    public void foo() {\n");
			buf.append("    }\n");
			buf.append("    /* (non-Javadoc)\n");
			buf.append("     * @see java.lang.Object#toString()\n");
			buf.append("     */\n");
			buf.append("    public String toString() {\n");
			buf.append("        //TODO\n");
			buf.append("        return super.toString();\n");
			buf.append("    }//here\n");
			buf.append("}\n");
			assertEqualString(doc.get(), buf.toString());
			
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}
		
	}
	
	public void testMethodStubCompletion2() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.Writer;\n");
		buf.append("\n");
		buf.append("public class A extends Writer {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    //here\n");
		buf.append("}\n");
		String contents= buf.toString();
		
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		IEditorPart part= EditorUtility.openInEditor(cu);
		try {
			String str= "//here";

			int offset= contents.indexOf(str);

			ResultCollector collector= new ResultCollector();
			collector.reset(offset, cu.getJavaProject(), cu);
			collector.setViewer(null);
			collector.setReplacementLength(0);
			collector.setPreventEating(true);

			cu.codeComplete(offset, collector);

			JavaCompletionProposal[] proposals= collector.getResults();

			JavaCompletionProposal closeProposal= null;

			for (int i= 0; i < proposals.length; i++) {
				if (proposals[i].getDisplayString().startsWith("close()")) {
					closeProposal= proposals[i];
				}
			}
			assertNotNull("no proposal for close()", closeProposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());

			closeProposal.apply(doc);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import java.io.IOException;\n");
			buf.append("import java.io.Writer;\n");
			buf.append("\n");
			buf.append("public class A extends Writer {\n");
			buf.append("    public void foo() {\n");
			buf.append("    }\n");
			buf.append("    /* (non-Javadoc)\n");
			buf.append("     * @see java.io.Writer#close()\n");
			buf.append("     */\n");
			buf.append("    public void close() throws IOException {\n");
			buf.append("        //TODO\n");
			buf.append("\n");
			buf.append("    }//here\n");
			buf.append("}\n");
			assertEqualString(doc.get(), buf.toString()); 
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}		
	}
	
	public void testMethodStubCompletion3() throws Exception {
		IPackageFragmentRoot sourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		IPackageFragment pack1= sourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.BufferedWriter;\n");
		buf.append("\n");
		buf.append("public class A extends BufferedWriter {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("    //here\n");
		buf.append("}\n");
		String contents= buf.toString();
		
		ICompilationUnit cu= pack1.createCompilationUnit("A.java", contents, false, null);

		IEditorPart part= EditorUtility.openInEditor(cu);
		try {
			String str= "//here";

			int offset= contents.indexOf(str);

			ResultCollector collector= new ResultCollector();
			collector.reset(offset, cu.getJavaProject(), cu);
			collector.setViewer(null);
			collector.setReplacementLength(0);
			collector.setPreventEating(true);

			cu.codeComplete(offset, collector);

			JavaCompletionProposal[] proposals= collector.getResults();

			JavaCompletionProposal closeProposal= null;

			for (int i= 0; i < proposals.length; i++) {
				if (proposals[i].getDisplayString().startsWith("close()")) {
					closeProposal= proposals[i];
				}
			}
			assertNotNull("no proposal for close()", closeProposal);

			IDocument doc= JavaUI.getDocumentProvider().getDocument(part.getEditorInput());
			closeProposal.apply(doc);

			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("\n");
			buf.append("import java.io.BufferedWriter;\n");
			buf.append("import java.io.IOException;\n");
			buf.append("\n");
			buf.append("public class A extends BufferedWriter {\n");
			buf.append("    public void foo() {\n");
			buf.append("    }\n");
			buf.append("    /* (non-Javadoc)\n");
			buf.append("     * @see java.io.BufferedWriter#close()\n");
			buf.append("     */\n");
			buf.append("    public void close() throws IOException {\n");
			buf.append("        //TODO\n");
			buf.append("        super.close();\n");
			buf.append("    }//here\n");
			buf.append("}\n");
			assertEqualString(doc.get(), buf.toString()); 
		} finally {
			part.getSite().getPage().closeAllEditors(false);
		}		
	}	
}
