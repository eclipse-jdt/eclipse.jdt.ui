package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.preference.IPreferenceStore;

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
import org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.CorrectMainTypeNameProposal;
import org.eclipse.jdt.internal.ui.text.correction.CorrectPackageDeclarationProposal;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;

public class ReorgQuickFixTest extends QuickFixTest {
	
	private static final Class THIS= ReorgQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ReorgQuickFixTest(String name) {
		super(name);
	}


	public static Test suite() {
		if (true) {
			return new TestSuite(THIS);
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new ReorgQuickFixTest("testMethodWithConstructorName"));
			return suite;
		}
	}


	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(JavaCore.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(JavaCore.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_UNUSED_IMPORT, JavaCore.ERROR);
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

	
	public void testUnusedImports() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnusedImportsInDefaultPackage() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("public class E {\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testUnusedImportOnDemand() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");				
		buf.append("import java.util.Vector;\n");
		buf.append("import java.net.*;\n");
		buf.append("\n");				
		buf.append("public class E {\n");
		buf.append("    Vector v;\n");		
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");				
		buf.append("import java.util.Vector;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    Vector v;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testCollidingImports() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");				
		buf.append("import java.security.Permission;\n");
		buf.append("import java.security.acl.Permission;\n");
		buf.append("import java.util.Vector;\n");		
		buf.append("\n");				
		buf.append("public class E {\n");
		buf.append("    Permission p;\n");
		buf.append("    Vector v;\n");		
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");				
		buf.append("import java.security.Permission;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("\n");				
		buf.append("public class E {\n");
		buf.append("    Permission p;\n");
		buf.append("    Vector v;\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testWrongPackageStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("\n");				
		buf.append("public class E {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		boolean hasRename= true, hasMove= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			ChangeCorrectionProposal curr= (ChangeCorrectionProposal) proposals.get(i);
			if (curr instanceof CorrectPackageDeclarationProposal) {
				assertTrue("Duplicated proposal", hasRename);
				hasRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) curr;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();				
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("\n");				
				buf.append("public class E {\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());			
			} else {
				assertTrue("Duplicated proposal", hasMove);
				hasMove= false;				
				curr.apply(null);
				
				IPackageFragment pack2= fSourceFolder.getPackageFragment("test2");
				ICompilationUnit cu2= pack2.getCompilationUnit("E.java");
				assertTrue("CU does not exist", cu2.exists());
				buf= new StringBuffer();
				buf.append("package test2;\n");
				buf.append("\n");				
				buf.append("public class E {\n");
				buf.append("}\n");
				assertEqualStringIgnoreDelim(cu2.getSource(), buf.toString());					
			}
		}
	}
	
	public void testWrongPackageStatementFromDefault() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("\n");				
		buf.append("public class E {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		boolean hasRename= true, hasMove= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			ChangeCorrectionProposal curr= (ChangeCorrectionProposal) proposals.get(i);
			if (curr instanceof CorrectPackageDeclarationProposal) {
				assertTrue("Duplicated proposal", hasRename);
				hasRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) curr;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();				
				buf= new StringBuffer();
				buf.append("\n");
				buf.append("\n");
				buf.append("public class E {\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());			
			} else {
				assertTrue("Duplicated proposal", hasMove);
				hasMove= false;				
				curr.apply(null);
				
				IPackageFragment pack2= fSourceFolder.getPackageFragment("test2");
				ICompilationUnit cu2= pack2.getCompilationUnit("E.java");
				assertTrue("CU does not exist", cu2.exists());
				buf= new StringBuffer();
				buf.append("package test2;\n");
				buf.append("\n");				
				buf.append("public class E {\n");
				buf.append("}\n");
				assertEqualStringIgnoreDelim(cu2.getSource(), buf.toString());					
			}
		}
	}		
	
	public void testWrongDefaultPackageStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("public class E {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		boolean hasRename= true, hasMove= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			ChangeCorrectionProposal curr= (ChangeCorrectionProposal) proposals.get(i);
			if (curr instanceof CorrectPackageDeclarationProposal) {
				assertTrue("Duplicated proposal", hasRename);
				hasRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) curr;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();				
				buf= new StringBuffer();
				buf.append("package test2;\n");
				buf.append("\n");
				buf.append("public class E {\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());			
			} else {
				assertTrue("Duplicated proposal", hasMove);
				hasMove= false;				
				curr.apply(null);
				
				IPackageFragment pack2= fSourceFolder.getPackageFragment("");
				ICompilationUnit cu2= pack2.getCompilationUnit("E.java");
				assertTrue("CU does not exist", cu2.exists());
				buf= new StringBuffer();			
				buf.append("public class E {\n");
				buf.append("}\n");
				assertEqualStringIgnoreDelim(cu2.getSource(), buf.toString());					
			}
		}
	}
	
	public void testWrongPackageStatementButColliding() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("\n");		
		buf.append("public class E {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		buf.append("package test2;\n");
		buf.append("\n");		
		buf.append("public class E {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CorrectPackageDeclarationProposal curr= (CorrectPackageDeclarationProposal) proposals.get(0);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) curr;
		String preview= proposal.getCompilationUnitChange().getPreviewContent();				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());			
	}
	
	public void testWrongTypeName() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");				
		buf.append("public class E {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("X.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		boolean hasRename= true, hasMove= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			ChangeCorrectionProposal curr= (ChangeCorrectionProposal) proposals.get(i);
			if (curr instanceof CorrectMainTypeNameProposal) {
				assertTrue("Duplicated proposal", hasRename);
				hasRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) curr;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();				
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("\n");				
				buf.append("public class X {\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());					
			} else {						
				assertTrue("Duplicated proposal", hasMove);
				hasMove= false;				
				curr.apply(null);
				
				ICompilationUnit cu2= pack1.getCompilationUnit("E.java");
				assertTrue("CU does not exist", cu2.exists());
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("\n");				
				buf.append("public class E {\n");
				buf.append("}\n");
				assertEqualStringIgnoreDelim(cu2.getSource(), buf.toString());
			}
		}
	}
	
	public void testWrongTypeNameButColliding() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");		
		buf.append("public class X {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		buf.append("package test1;\n");
		buf.append("\n");		
		buf.append("public class X {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		
		String preview= proposal.getCompilationUnitChange().getPreviewContent();				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());			
	}
	
	public void testWrongTypeNameWithConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");		
		buf.append("public class X {\n");
		buf.append("    public X() {\n");
		buf.append("        X other;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		buf.append("package test1;\n");
		buf.append("\n");		
		buf.append("public class X {\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		CorrectionContext context= getCorrectionContext(cu, problems[0]);
		assertCorrectContext(context);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		
		String preview= proposal.getCompilationUnitChange().getPreviewContent();				
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");		
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        E other;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());			
	}		
	
	
	
}
