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
import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.ProblemPosition;

public class ReturnTypeQuickFixTest extends QuickFixTest {
	
	private static final Class THIS= ReturnTypeQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ReturnTypeQuickFixTest(String name) {
		super(name);
	}


	public static Test suite() {
		if (true) {
			return new TestSuite(THIS);
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new ReturnTypeQuickFixTest("testMethodWithConstructorName"));
			return suite;
		}
	}


	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
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

	
	public void testSimpleTypeReturnDeclMissing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        return (new Vector()).elements();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		boolean addReturnType= true, doRename= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			Object elem= proposals.get(i);
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;		
		
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("import java.util.Enumeration;\n");		
				buf.append("import java.util.Vector;\n");
				buf.append("public class E {\n");
				buf.append("    public Enumeration foo() {\n");
				buf.append("        return (new Vector()).elements();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("import java.util.Vector;\n");				
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        return (new Vector()).elements();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}
				
				
	}
	
	public void testVoidTypeReturnDeclMissing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        //do nothing\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		boolean addReturnType= true, doRename= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			Object elem= proposals.get(i);
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;		
		
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public void foo() {\n");
				buf.append("        //do nothing\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        //do nothing\n");	
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}	
		
	}
	
	public void testVoidTypeReturnDeclMissing2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        if (true) {\n");
		buf.append("           return;\n");
		buf.append("        }\n");
		buf.append("        return;\n");				
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		boolean addReturnType= true, doRename= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			Object elem= proposals.get(i);
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;		
		
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public void foo() {\n");
				buf.append("        if (true) {\n");
				buf.append("           return;\n");
				buf.append("        }\n");
				buf.append("        return;\n");				
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        if (true) {\n");
				buf.append("           return;\n");
				buf.append("        }\n");
				buf.append("        return;\n");				
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}	
	}
	
	public void testVoidMissingInAnonymousType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("            }\n");
		buf.append("            public foo() {\n");
		buf.append("            }\n");		
		buf.append("        };\n");				
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);

		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);

		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("            }\n");
		buf.append("            public void foo() {\n");
		buf.append("            }\n");		
		buf.append("        };\n");				
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	
	public void testNullTypeReturnDeclMissing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		boolean addReturnType= true, doRename= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			Object elem= proposals.get(i);
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;		
		
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public Object foo() {\n");
				buf.append("        return null;\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        return null;\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}	
		}		
	}
	
	public void testArrayTypeReturnDeclMissing() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public foo() {\n");
		buf.append("        return new int[][] { { 1, 2 }, { 2, 3 } };\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		boolean addReturnType= true, doRename= true;
		
		for (int i= 0; i < proposals.size(); i++) {
			Object elem= proposals.get(i);
			if (elem instanceof ASTRewriteCorrectionProposal) {
				ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) elem;
				assertTrue("duplicated entries", addReturnType);
				addReturnType= false;

				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public int[][] foo() {\n");
				buf.append("        return new int[][] { { 1, 2 }, { 2, 3 } };\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("duplicated entries", doRename);
				doRename= false;
				
				CUCorrectionProposal proposal= (CUCorrectionProposal) elem;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public E() {\n");
				buf.append("        return new int[][] { { 1, 2 }, { 2, 3 } };\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			}
		}	
	}
	
	public void testVoidMethodReturnsStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    Vector fList= new Vector();\n");
		buf.append("    public void elements() {\n");
		buf.append("        return fList.toArray();\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
			
		{	
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
			String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("public class E {\n");
			buf.append("    Vector fList= new Vector();\n");
			buf.append("    public Object[] elements() {\n");
			buf.append("        return fList.toArray();\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}
		{	
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
			String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.util.Vector;\n");
			buf.append("public class E {\n");
			buf.append("    Vector fList= new Vector();\n");
			buf.append("    public void elements() {\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}		
	}
	
	public void testVoidMethodReturnsAnonymClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void getOperation() {\n");
		buf.append("        return new Runnable() {\n");
		buf.append("            public void run() {}\n");
		buf.append("        };\n");				
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
			String preview= proposal.getCompilationUnitChange().getPreviewContent();
	
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E {\n");
			buf.append("    public Runnable getOperation() {\n");
			buf.append("        return new Runnable() {\n");
			buf.append("            public void run() {}\n");
			buf.append("        };\n");				
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}
		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
			String preview= proposal.getCompilationUnitChange().getPreviewContent();
	
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E {\n");
			buf.append("    public void getOperation() {\n");		
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}		
	}
	
	public void testConstructorReturnsValue() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Arrays;\n");		
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("        return Arrays.asList(null);\n");		
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 2);
		assertCorrectLabels(proposals);
		
		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
			String preview= proposal.getCompilationUnitChange().getPreviewContent();
	
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.util.Arrays;\n");
			buf.append("import java.util.List;\n");					
			buf.append("public class E {\n");
			buf.append("    public List E() {\n");
			buf.append("        return Arrays.asList(null);\n");		
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}
		{
			ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(1);
			String preview= proposal.getCompilationUnitChange().getPreviewContent();
	
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("import java.util.Arrays;\n");		
			buf.append("public class E {\n");
			buf.append("    public E() {\n");
			buf.append("    }\n");
			buf.append("}\n");
			assertEqualString(preview, buf.toString());
		}		
	}
	
	
	public void testCorrectReturnStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public Runnable getOperation() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public Runnable getOperation() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testCorrectReturnStatementForArray() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] getArray() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] getArray() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testMethodWithConstructorName() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] E() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public E() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}	
	
	/*
	only a compiler error, not a reconciled problem
	
	public void testMissingReturnStatement() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] getArray() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, true);
		IProblem[] problems= astRoot.getProblems();
		assertNumberOf("problems", problems.length, 1);
		
		ProblemPosition problemPos= new ProblemPosition(problems[0], cu);
		ArrayList proposals= new ArrayList();
		
		JavaCorrectionProcessor.collectCorrections(problemPos,  proposals);
		assertNumberOf("proposals", proposals.size(), 1);
		assertCorrectLabels(proposals);
		
		ASTRewriteCorrectionProposal proposal= (ASTRewriteCorrectionProposal) proposals.get(0);
		String preview= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int[][] getArray() {\n");
		buf.append("        return null;\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	*/
	
	
}
