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
import org.eclipse.jdt.internal.ui.text.correction.CorrectionContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;

public class ModifierCorrectionsQuickFixTest extends QuickFixTest {
	
	private static final Class THIS= ModifierCorrectionsQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ModifierCorrectionsQuickFixTest(String name) {
		super(name);
	}


	public static Test suite() {
		if (true) {
			return new TestSuite(THIS);
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new ModifierCorrectionsQuickFixTest("testOuterLocalMustBeFinal"));
			return suite;
		}
	}


	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(JavaCore.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(JavaCore.FORMATTER_TAB_SIZE, "4");
		options.put(JavaCore.COMPILER_PB_STATIC_ACCESS_RECEIVER, JavaCore.ERROR);
		
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

	public void testStaticMethodRequestedInSameType1() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void xoo() {\n");
		buf.append("    }\n");		
		buf.append("    public static void foo() {\n");
		buf.append("        xoo();\n");
		buf.append("    }\n");
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
		buf.append("    public static void xoo() {\n");
		buf.append("    }\n");		
		buf.append("    public static void foo() {\n");
		buf.append("        xoo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}	
	
	public void testStaticMethodRequestedInSameType2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void xoo() {\n");
		buf.append("    }\n");		
		buf.append("    public static void foo() {\n");
		buf.append("        E.xoo();\n");
		buf.append("    }\n");
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
		buf.append("    public static void xoo() {\n");
		buf.append("    }\n");		
		buf.append("    public static void foo() {\n");
		buf.append("        E.xoo();\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testStaticMethodRequestedInOtherType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class X {\n");
		buf.append("    public void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("X.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("         X.xoo();\n");
		buf.append("    }\n");
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
		buf.append("public class X {\n");
		buf.append("    public static void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testInvisibleMethodRequestedInSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");	
		buf.append("public class C {\n");
		buf.append("    private void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("         xoo();\n");
		buf.append("    }\n");
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
		buf.append("public class C {\n");
		buf.append("    protected void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testInvisibleSuperMethodRequestedInSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    private void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("         super.xoo();\n");
		buf.append("    }\n");
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
		buf.append("public class C {\n");
		buf.append("    protected void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
		
	public void testInvisibleMethodRequestedInOtherPackage() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("import java.util.List;\n");		
		buf.append("public class C {\n");
		buf.append("    private void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import test2.C;\n");				
		buf.append("public class E {\n");
		buf.append("    public void foo(C c) {\n");
		buf.append("         c.xoo();\n");
		buf.append("    }\n");
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
		buf.append("package test2;\n");
		buf.append("import java.util.List;\n");		
		buf.append("public class C {\n");
		buf.append("    public void xoo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testInvisibleConstructorRequestedInOtherType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");	
		buf.append("public class C {\n");
		buf.append("    private C() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("         C c= new C();\n");
		buf.append("    }\n");
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
		buf.append("public class C {\n");
		buf.append("    C() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testInvisibleConstructorRequestedInSuperType() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");	
		buf.append("public class C {\n");
		buf.append("    private C() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public E() {\n");
		buf.append("         super();\n");
		buf.append("    }\n");
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
		buf.append("public class C {\n");
		buf.append("    protected C() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}	
	
	public void testInvisibleFieldRequestedInSamePackage1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    private int fXoo;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");	
		buf.append("public class E {\n");
		buf.append("    public void foo(C c) {\n");
		buf.append("         c.fXoo= 1;\n");
		buf.append("    }\n");
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
		buf.append("public class C {\n");
		buf.append("    int fXoo;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testInvisibleFieldRequestedInSamePackage2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    private int fXoo;\n");
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);

		buf= new StringBuffer();
		buf.append("package test1;\n");	
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("         fXoo= 1;\n");
		buf.append("    }\n");
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
		buf.append("public class C {\n");
		buf.append("    protected int fXoo;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}	
	
	
	public void testNonStaticMethodRequestedInConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int xoo() { return 1; };\n");
		buf.append("    public E(int val) {\n");		
		buf.append("    }\n");			
		buf.append("    public E() {\n");
		buf.append("         this(xoo());\n");
		buf.append("    }\n");
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
		buf.append("    private static int xoo() { return 1; };\n");
		buf.append("    public E(int val) {\n");		
		buf.append("    }\n");			
		buf.append("    public E() {\n");
		buf.append("         this(xoo());\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}		
	
	public void testNonStaticFieldRequestedInConstructor() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    private int fXoo= 1;\n");
		buf.append("    public E(int val) {\n");		
		buf.append("    }\n");			
		buf.append("    public E() {\n");
		buf.append("         this(fXoo);\n");
		buf.append("    }\n");
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
		buf.append("    private static int fXoo= 1;\n");
		buf.append("    public E(int val) {\n");		
		buf.append("    }\n");			
		buf.append("    public E() {\n");
		buf.append("         this(fXoo);\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testInvisibleTypeRequestedInDifferentPackage() throws Exception {
		if (false) {
			System.out.println("testInvisibleTypeRequestedInDifferentPackage: Waiting for release fo bug fix 24406");	
			return;
		}
		
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("class C {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("         test2.C c= null;\n");
		buf.append("    }\n");
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
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testInvisibleTypeRequestedFromSuperClass() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    private class CInner {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E extends C {\n");
		buf.append("    public void foo() {\n");
		buf.append("         CInner c= null;\n");
		buf.append("    }\n");
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
		buf.append("public class C {\n");
		buf.append("    class CInner {\n");
		buf.append("    }\n");		
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	
	public void testInvisibleImport() throws Exception {
		IPackageFragment pack2= fSourceFolder.createPackageFragment("test2", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test2;\n");
		buf.append("class C {\n");
		buf.append("}\n");
		pack2.createCompilationUnit("C.java", buf.toString(), false, null);
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("         test2.C c= null;\n");
		buf.append("    }\n");
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
		buf.append("package test2;\n");
		buf.append("public class C {\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testAbstractMethodWithBody() throws Exception {	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public abstract void foo() {\n");
		buf.append("    }\n");				
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
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");				
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public abstract class E {\n");
		buf.append("    public abstract void foo();\n");
		buf.append("}\n");
		String expected2= buf.toString();

		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		

	}
	
	public void testAbstractMethodInNonAbstractClass() throws Exception {	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public abstract int foo();\n");
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
		
		CUCorrectionProposal proposal= (CUCorrectionProposal) proposals.get(0);
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public int foo() {\n");
		buf.append("        return 0;\n");	
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");		
		buf.append("public abstract class E {\n");
		buf.append("    public abstract int foo();\n");
		buf.append("}\n");
		String expected2= buf.toString();
		
		assertEqualStringsIgnoreOrder(new String[] { preview1, preview2 }, new String[] { expected1, expected2 });		
	}
	
	public void testNativeMethodWithBody() throws Exception {	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public native void foo() {\n");
		buf.append("    }\n");
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
		String preview1= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		String expected1= buf.toString();
		
		/* no body
		proposal= (CUCorrectionProposal) proposals.get(1);
		String preview2= proposal.getCompilationUnitChange().getPreviewContent();

		buf= new StringBuffer();
		buf.append("package test1;\n");		
		buf.append("public abstract class E {\n");
		buf.append("    public abstract int foo();\n");
		buf.append("}\n");
		String expected2= buf.toString();
		*/
		
		assertEqualStringsIgnoreOrder(new String[] { preview1 }, new String[] { expected1 });		
	}
	
	public void testOuterLocalMustBeFinal() throws Exception {
	
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        int i= 9;\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                int x= i;\n");
		buf.append("            }\n");
		buf.append("        };\n");				
		buf.append("    }\n");
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
		buf.append("    public void foo() {\n");
		buf.append("        final int i= 9;\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                int x= i;\n");
		buf.append("            }\n");
		buf.append("        };\n");				
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testOuterParameterMustBeFinal() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo(int i) {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                int x= i;\n");
		buf.append("            }\n");
		buf.append("        };\n");				
		buf.append("    }\n");
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
		buf.append("    public void foo(final int i) {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            public void run() {\n");
		buf.append("                int x= i;\n");
		buf.append("            }\n");
		buf.append("        };\n");				
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testOuterForParamMustBeFinal() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("        for (int i= 1; true;) {\n");		
		buf.append("            Runnable run= new Runnable() {\n");
		buf.append("                public void run() {\n");
		buf.append("                    int x= i;\n");
		buf.append("                }\n");
		buf.append("            };\n");			
		buf.append("        }\n");				
		buf.append("    }\n");
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
		buf.append("    public void foo() {\n");
		buf.append("        for (final int i= 1; true;) {\n");		
		buf.append("            Runnable run= new Runnable() {\n");
		buf.append("                public void run() {\n");
		buf.append("                    int x= i;\n");
		buf.append("                }\n");
		buf.append("            };\n");			
		buf.append("        }\n");				
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}	
	
}
