package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.jface.preference.IPreferenceStore;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.NewCUCompletionUsingWizardProposal;
import org.eclipse.jdt.internal.ui.text.correction.ProblemPosition;

public class UnresolvedTypesQuickFixTest extends QuickFixTest {
	
	private static final Class THIS= UnresolvedTypesQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public UnresolvedTypesQuickFixTest(String name) {
		super(name);
	}


	public static Test suite() {
		if (true) {
			return new TestSuite(THIS);
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new UnresolvedTypesQuickFixTest("testTypeInStatement"));
			return suite;
		}
	}


	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getOptions();
		options.put(JavaCore.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(JavaCore.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);
		
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN__FILE_COMMENTS, false);
		store.setValue(PreferenceConstants.CODEGEN__JAVADOC_STUBS, false);
				
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}
		
	public void testTypeInFieldDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    Vector1 vec;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
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
	
	public void testTypeInMethodDecl() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vect1or[] vec) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 3);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
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
		newCU.delete(true, null);

		newCUWizard= (NewCUCompletionUsingWizardProposal) proposals.get(2);
		newCUWizard.setShowDialog(false);
		newCUWizard.apply(null);
		
		newCU= pack1.getCompilationUnit("ArrayListist.java");
		assertTrue("Nothing created", newCU.exists());
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public interface ArrayListist {\n");
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
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 4);

		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
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
		preview= proposal.getCompilationUnitChange().getPreviewContent();
		
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

}
