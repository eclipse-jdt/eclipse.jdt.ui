package org.eclipse.jdt.ui.tests.quickfix;

import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.swt.graphics.Point;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
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
			suite.addTest(new AssistQuickFixTest("testUnimplementedMethods2"));
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
		store.setValue(PreferenceConstants.CODEGEN_GETTERSETTER_PREFIX, "f");
		store.setValue(PreferenceConstants.CODEGEN_GETTERSETTER_SUFFIX, "_m");
		
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}

	
	public void testAssignToLocal() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_USE_GETTERSETTER_PREFIX, true);
		store.setValue(PreferenceConstants.CODEGEN_USE_GETTERSETTER_SUFFIX, false);		
		
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
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
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
				buf.append("    private Class fClass;\n");
				buf.append("\n");
				buf.append("    public void foo() {\n");
				buf.append("        fClass = getClass();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
				
				Point selection= proposal.getSelection(new Document(preview));
				assertEquals("wrong selection", "fClass", preview.substring(selection.x, selection.x + selection.y));	

			} else if (proposal.getVariableKind() == AssignToVariableAssistProposal.LOCAL) {
				assertTrue("same proposal kind", doLocal);
				doLocal= false;
				String preview= proposal.getCompilationUnitChange().getPreviewContent();
		
				buf= new StringBuffer();
				buf.append("package test1;\n");
				buf.append("public class E {\n");
				buf.append("    public void foo() {\n");
				buf.append("        Class c = getClass();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
				
				Point selection= proposal.getSelection(new Document(preview));
				assertEquals("wrong selection", "c", preview.substring(selection.x, selection.x + selection.y));	
			}
		}
	}
	
	public void testAssignToLocal2() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_USE_GETTERSETTER_PREFIX, true);
		store.setValue(PreferenceConstants.CODEGEN_USE_GETTERSETTER_SUFFIX, false);			
		
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
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
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
				buf.append("    private Iterator fIterator;\n");	
				buf.append("    public Vector goo() {\n");
				buf.append("        return null;\n");
				buf.append("    }\n");		
				buf.append("    public void foo() {\n");
				buf.append("        fIterator = goo().iterator();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
				
				Point selection= proposal.getSelection(new Document(preview));
				assertEquals("wrong selection", "fIterator", preview.substring(selection.x, selection.x + selection.y));	
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

				Point selection= proposal.getSelection(new Document(preview));
				assertEquals("wrong selection", "iterator", preview.substring(selection.x, selection.x + selection.y));	
			}
		}
	}
	
	public void testAssignToLocal2CursorAtEnd() throws Exception {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_USE_GETTERSETTER_PREFIX, false);
		store.setValue(PreferenceConstants.CODEGEN_USE_GETTERSETTER_SUFFIX, true);	
		
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
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
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
				buf.append("    private Object[] objects_m;\n");	
				buf.append("    public Vector goo() {\n");
				buf.append("        return null;\n");
				buf.append("    }\n");		
				buf.append("    public void foo() {\n");
				buf.append("        objects_m = goo().toArray();\n");
				buf.append("    }\n");
				buf.append("}\n");
				assertEqualString(preview, buf.toString());
				
				Point selection= proposal.getSelection(new Document(preview));
				assertEquals("wrong selection", "objects_m", preview.substring(selection.x, selection.x + selection.y));						
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
				
				Point selection= proposal.getSelection(new Document(preview));
				assertEquals("wrong selection", "objects", preview.substring(selection.x, selection.x + selection.y));	
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
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
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
		
		JavaCorrectionProcessor.collectCorrections(context,  proposals);
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
	
}
