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
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.NewCUCompletionUsingWizardProposal;

public class UnresolvedTypesQuickFixTest extends QuickFixTest {
	
	private static final Class THIS= UnresolvedTypesQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public UnresolvedTypesQuickFixTest(String name) {
		super(name);
	}


	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test suite() {
		if (false) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new UnresolvedTypesQuickFixTest("testInnerType"));
			return new ProjectTestSetup(suite);
		}
	}


	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getFormatterOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);
				
		fJProject1= ProjectTestSetup.getProject();

		String newFileTemplate= "${package_declaration}\n\n${type_declaration}";
		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.NEWTYPE).setPattern(newFileTemplate);
		JavaPlugin.getDefault().getCodeTemplateStore().findTemplate(CodeTemplateContextType.TYPECOMMENT).setPattern("");

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}
		
	public void testTypeInFieldDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    Vector1 vec;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    Vector vec;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
		NewCUCompletionUsingWizardProposal newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(1);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		ICompilationUnit newCU= pack1.getCompilationUnit("Vector1.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Vector1 {\n");
		buf.append("\n");
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
		JavaProjectHelper.performDummySearch();
		newCU.delete(true, null);

		newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(2);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		newCU= pack1.getCompilationUnit("Vector1.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface Vector1 {\n");
		buf.append("\n");
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
	}

	public void testTypeInMethodArguments() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vect1or[] vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector[] vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
		NewCUCompletionUsingWizardProposal newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(1);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		ICompilationUnit newCU= pack1.getCompilationUnit("Vect1or.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Vect1or {\n");
		buf.append("\n");
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
		JavaProjectHelper.performDummySearch();
		newCU.delete(true, null);

		newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(2);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		newCU= pack1.getCompilationUnit("Vect1or.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface Vect1or {\n");
		buf.append("\n");
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
	}
	
	public void testTypeInMethodReturnType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    Vect1or[] foo() {\n");
		buf.append("        return null;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    Vector[] foo() {\n");
		buf.append("        return null;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
		NewCUCompletionUsingWizardProposal newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(1);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		ICompilationUnit newCU= pack1.getCompilationUnit("Vect1or.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class Vect1or {\n");
		buf.append("\n");
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
		JavaProjectHelper.performDummySearch();
		newCU.delete(true, null);

		newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(2);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		newCU= pack1.getCompilationUnit("Vect1or.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface Vect1or {\n");
		buf.append("\n");
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
	}

	public void testTypeInExceptionType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() throws IOExcpetion {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void foo() throws IOException {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
		NewCUCompletionUsingWizardProposal newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(1);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		ICompilationUnit newCU= pack1.getCompilationUnit("IOExcpetion.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class IOExcpetion extends Exception {\n");
		buf.append("\n");
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
		JavaProjectHelper.performDummySearch();
		newCU.delete(true, null);
	}
	
	
	public void testTypeInStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");		
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        ArrayList v= new ArrayListist();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.ArrayList;\n");		
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        ArrayList v= new ArrayList();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
		
		NewCUCompletionUsingWizardProposal newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(1);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		ICompilationUnit newCU= pack1.getCompilationUnit("ArrayListist.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.util.ArrayList;\n");
		buf.append("\n");
		buf.append("public class ArrayListist extends ArrayList {\n");
		buf.append("\n");
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
	}	
		

	public void testArrayTypeInStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");		
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Serializable[] v= new ArrayListExtra[10];\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 4);
		assertCorrectLabels(proposals);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Serializable[] v= new Serializable[10];\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		preview= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.io.*;\n");
		buf.append("import java.util.ArrayList;\n");		
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Serializable[] v= new ArrayList[10];\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
		NewCUCompletionUsingWizardProposal newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(2);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		ICompilationUnit newCU= pack1.getCompilationUnit("ArrayListExtra.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("\n");
		buf.append("public class ArrayListExtra implements Serializable {\n");
		buf.append("\n");
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
		JavaProjectHelper.performDummySearch();
		newCU.delete(true, null);

		newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(3);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		newCU= pack1.getCompilationUnit("ArrayListExtra.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("\n");
		buf.append("public interface ArrayListExtra extends Serializable {\n");
		buf.append("\n");
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
	}
	
	public void testQualifiedType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        test2.Test t= null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 2);
		assertCorrectLabels(proposals);
		
		NewCUCompletionUsingWizardProposal newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(0);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		ICompilationUnit newCU= fSourceFolder.getPackageFragment("test2").getCompilationUnit("Test.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("\n");		
		buf.append("public class Test {\n");
		buf.append("\n");		
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
		JavaProjectHelper.performDummySearch();
		newCU.delete(true, null);

		newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(1);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		newCU= fSourceFolder.getPackageFragment("test2").getCompilationUnit("Test.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("\n");		
		buf.append("public interface Test {\n");
		buf.append("\n");		
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());		
		
	}
	
	public void testInnerType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Object object= new F.Inner() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("}\n");
		ICompilationUnit cu2= pack1.createCompilationUnit("F.java", buf.toString(), false, null);

		
		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 3);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        Object object= new Object() {\n");
		buf.append("        };\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
		
		NewCUCompletionUsingWizardProposal newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(1);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public class Inner {\n");
		buf.append("\n");				
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualStringIgnoreDelim(cu2.getSource(), buf.toString());

		cu2.getType("F").getType("Inner").delete(true, null);

		newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(2);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class F {\n");
		buf.append("    public interface Inner {\n");
		buf.append("\n");				
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualStringIgnoreDelim(cu2.getSource(), buf.toString());		
	
	}
	



	public void testTypeInCatchBlock() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo() {\n");
		buf.append("        try {\n");		
		buf.append("        } catch (XXX x) {\n");
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		NewCUCompletionUsingWizardProposal newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(0);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		ICompilationUnit newCU= pack1.getCompilationUnit("XXX.java");
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");			
		buf.append("public class XXX extends Exception {\n");
		buf.append("\n");	
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
	}
	
	public void testTypeInSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends XXX {\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		NewCUCompletionUsingWizardProposal newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(0);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		ICompilationUnit newCU= pack1.getCompilationUnit("XXX.java");
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");			
		buf.append("public class XXX {\n");
		buf.append("\n");	
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
	}
	
	public void testTypeInSuperInterface() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public interface E extends XXX {\n");
		buf.append("}\n");
		ICompilationUnit cu1= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu1);
		ArrayList proposals= collectCorrections(cu1, astRoot);
		assertNumberOfProposals(proposals, 1);
		assertCorrectLabels(proposals);
		
		NewCUCompletionUsingWizardProposal newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(0);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		ICompilationUnit newCU= pack1.getCompilationUnit("XXX.java");
				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");			
		buf.append("public interface XXX {\n");
		buf.append("\n");	
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
	}
	
	public void testPrimitiveTypeInFieldDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    floot vec= 1.0;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= getASTRoot(cu);
		ArrayList proposals= collectCorrections(cu, astRoot);
		assertNumberOfProposals(proposals, 5);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    double vec= 1.0;\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    Float vec= 1.0;\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(2);
		String preview3= getPreviewContent(proposal);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    float vec= 1.0;\n");
		buf.append("}\n");
		String expected3= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2, preview3 }, new String[] { expected1, expected2, expected3 });		

		
		
		NewCUCompletionUsingWizardProposal newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(3);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		ICompilationUnit newCU= pack1.getCompilationUnit("floot.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class floot {\n");
		buf.append("\n");
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());
		JavaProjectHelper.performDummySearch();
		newCU.delete(true, null);

		newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(4);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		newCU= pack1.getCompilationUnit("floot.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");		
		buf.append("public interface floot {\n");
		buf.append("\n");
		buf.append("}\n");
		assertEqualStringIgnoreDelim(newCU.getSource(), buf.toString());	
	}


	private void createSomeAmbiguity(boolean ifc, boolean isException) throws Exception {

		IPackageFragment pack3= fSourceFolder.createPackageFragment("test3", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public "); buf.append(ifc ? "interface" : "class");
		buf.append(" A "); buf.append(isException ? "extends Exception " : ""); buf.append("{\n");
		buf.append("}\n");
		pack3.createCompilationUnit("A.java", buf.toString(), false, null);
	
		buf= new StringBuffer();
		buf.append("package test3;\n");
		buf.append("public class B {\n");
		buf.append("}\n");
		pack3.createCompilationUnit("B.java", buf.toString(), false, null);
			
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public "); buf.append(ifc ? "interface" : "class");
		buf.append(" A "); buf.append(isException ? "extends Exception " : ""); buf.append("{\n");
		buf.append("}\n");
		pack2.createCompilationUnit("A.java", buf.toString(), false, null);
			
		buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);
	}


	public void testAmbiguousTypeInSuperClass() throws Exception {
		createSomeAmbiguity(false, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("public class E extends A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
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
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E extends A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("import test3.A;\n");
		buf.append("public class E extends A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testAmbiguousTypeInInterface() throws Exception {
		createSomeAmbiguity(true, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("public class E implements A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
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
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E implements A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("import test3.A;\n");
		buf.append("public class E implements A {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}

	public void testAmbiguousTypeInField() throws Exception {
		createSomeAmbiguity(true, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("public class E {\n");
		buf.append("    A a;\n");	
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
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
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    A a;\n");	
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    A a;\n");	
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}
	
	public void testAmbiguousTypeInArgument() throws Exception {
		createSomeAmbiguity(true, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("    public void foo(A a) {");
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
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("    public void foo(A a) {");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("    public void foo(A a) {");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}
	
	public void testAmbiguousTypeInReturnType() throws Exception {
		createSomeAmbiguity(false, false);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("    public A foo() {");
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
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("    public A foo() {");
		buf.append("        return null;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("    public A foo() {");
		buf.append("        return null;\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}
	
	public void testAmbiguousTypeInExceptionType() throws Exception {
		createSomeAmbiguity(false, true);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("    public void foo() throws A {");
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
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("    public void foo() throws A {");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("    public void foo() throws A {");
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}
	
	public void testAmbiguousTypeInCatchBlock() throws Exception {
		createSomeAmbiguity(false, true);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("    public void foo() {");
		buf.append("        try {\n");
		buf.append("        } catch (A e) {\n");
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
		buf.append("import test2.*;\n");
		buf.append("import test2.A;\n");
		buf.append("import test3.*;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("    public void foo() {");
		buf.append("        try {\n");
		buf.append("        } catch (A e) {\n");		
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= getPreviewContent(proposal);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.*;\n");
		buf.append("import test3.*;\n");		
		buf.append("import test3.A;\n");
		buf.append("public class E {\n");
		buf.append("    B b;\n");
		buf.append("    C c;\n");		
		buf.append("    public void foo() {");
		buf.append("        try {\n");
		buf.append("        } catch (A e) {\n");		
		buf.append("        }\n");		
		buf.append("    }\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });
	}	
	

}
