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
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.NewCUCompletionUsingWizardProposal;
import org.eclipse.jdt.internal.ui.text.correction.NewVariableCompletionProposal;
import org.eclipse.jdt.internal.ui.text.correction.ProblemPosition;

public class UnresolvedVariablesQuickFixTest extends QuickFixTest {
	
	private static final Class THIS= UnresolvedVariablesQuickFixTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public UnresolvedVariablesQuickFixTest(String name) {
		super(name);
	}


	public static Test suite() {
		if (true) {
			return new TestSuite(THIS);
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new UnresolvedVariablesQuickFixTest("testTypeInFieldDecl"));
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

	
	public void testVarInAssignment() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    void foo(Vector vec) {\n");
		buf.append("        iter= vec.iterator();\n");
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
		
		boolean doField= true, doParam= true, doLocal= true;
		for (int i= 0; i < proposals.size(); i++) {
			NewVariableCompletionProposal proposal= (NewVariableCompletionProposal) proposals.get(i);
			String preview= proposal.getCompilationUnitChange().getPreviewContent();

			if (proposal.getVariableKind() == NewVariableCompletionProposal.FIELD) {
				assertTrue("2 field proposals", doField);
				doField= false;
				
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("import java.util.Iterator;\n");
				buf.append("import java.util.Vector;\n");
				buf.append("public class E {\n");
				buf.append("    private Iterator iter;\n");
				buf.append("    void foo(Vector vec) {\n");
				buf.append("        iter= vec.iterator();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else if (proposal.getVariableKind() == NewVariableCompletionProposal.LOCAL) {
				assertTrue("2 local proposals", doLocal);
				doLocal= false;
				
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("import java.util.Iterator;\n");
				buf.append("import java.util.Vector;\n");
				buf.append("public class E {\n");
				buf.append("    void foo(Vector vec) {\n");
				buf.append("        Iterator iter = vec.iterator();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else if (proposal.getVariableKind() == NewVariableCompletionProposal.PARAM) {
				assertTrue("2 param proposals", doParam);
				doParam= false;
				
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("import java.util.Iterator;\n");
				buf.append("import java.util.Vector;\n");
				buf.append("public class E {\n");
				buf.append("    void foo(Vector vec, Iterator iter) {\n");
				buf.append("        iter= vec.iterator();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
			} else {
				assertTrue("unknown type", false);
			}
		}
	}
	

}
