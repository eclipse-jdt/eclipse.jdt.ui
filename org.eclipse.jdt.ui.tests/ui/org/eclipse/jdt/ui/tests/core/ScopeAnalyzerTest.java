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

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;

import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;

/**
  */
public class ScopeAnalyzerTest extends CoreTests {

	private static final Class THIS= ScopeAnalyzerTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ScopeAnalyzerTest(String name) {
		super(name);
	}

	public static Test suite() {
		if (false) {
			return new TestSuite(THIS);
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new ScopeAnalyzerTest("testVariableDeclarations4"));
			return suite;
		}
	}
	
	protected void setUp() throws Exception {
		
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		assertTrue("rt not found", JavaProjectHelper.addRTJar(fJProject1) != null);

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(JavaCore.COMPILER_PB_HIDDEN_CATCH_BLOCK, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_UNREACHABLE_CODE, JavaCore.IGNORE);
		
		JavaCore.setOptions(options);		
	}

	protected void tearDown() throws Exception {
		JavaProjectHelper.delete(fJProject1);
	}
	
	public void testVariableDeclarations1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    int[] fGlobal;\n");
		buf.append("    public int goo(int param1, int param2) {\n");
		buf.append("        int count= 0;\n");
		buf.append("        fGlobal= new int[] { 1, 2, 3};\n");
		buf.append("        for (int i= 0; i < fGlobal.length; i++) {\n");
		buf.append("            int insideFor= 0;\n");
		buf.append("            count= insideFor + fGlobal[i];\n");
		buf.append("            return -1;\n");		
		buf.append("        }\n");
		buf.append("        count++;\n");
		buf.append("        int count2= 0;\n");
		buf.append("        count+= count2;\n");
		buf.append("        return count;\n");		
		buf.append("    }\n");			
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		
		
		CompilationUnit astRoot= AST.parseCompilationUnit(compilationUnit, true);
		IProblem[] problems= astRoot.getProblems();
		assertTrue(problems.length == 0);
		
		{
			String str= "count+= count2;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer().getDeclarationsInScope(astRoot, offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "count", "count2", "fGlobal" });
		}
		
		{
			String str= "count++;";
			int offset= buf.toString().indexOf(str);
			
			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer().getDeclarationsInScope(astRoot, offset, flags);
			
			assertVariables(res, new String[] { "param1", "param2", "count", "fGlobal" });
		}

		{
			String str= "return -1;";
			int offset= buf.toString().indexOf(str);
			
			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer().getDeclarationsInScope(astRoot, offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "count", "i", "insideFor", "fGlobal" });
		}		
		
	}

	public void testVariableDeclarations2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1.ae;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public int goo(int param1) {\n");
		buf.append("        int count= 9, count2= 0;\n");
		buf.append("        try {\n");
		buf.append("            for (int i= 0, j= 0; i < 9; i++) {\n");
		buf.append("                System.out.println(i + j);\n");
		buf.append("                j++;\n");
		buf.append("            }\n");
		buf.append("            return 8;\n");			
		buf.append("        } catch (IOException e) {\n");
		buf.append("           int k= 0;\n");
		buf.append("           return k;\n");
		buf.append("        } catch (Exception x) {\n");
		buf.append("           return 9;\n");					 
		buf.append("        };\n");
		buf.append("        return count;\n");		
		buf.append("    }\n");			
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		
		
		CompilationUnit astRoot= AST.parseCompilationUnit(compilationUnit, true);
		IProblem[] problems= astRoot.getProblems();
		assertTrue(problems.length == 0);
		
		{
			String str= "j++;";
			int offset= buf.toString().indexOf(str);
	
			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer().getDeclarationsInScope(astRoot, offset, flags);
			
			assertVariables(res, new String[] { "param1", "count", "count2", "i", "j"});
		}
		
		{
			String str= "return 8;";
			int offset= buf.toString().indexOf(str);
	
			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer().getDeclarationsInScope(astRoot, offset, flags);
			
			assertVariables(res, new String[] { "param1", "count", "count2"});
		}		
		
		{
			String str= "return k;";
			int offset= buf.toString().indexOf(str);
			
			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer().getDeclarationsInScope(astRoot, offset, flags);

			assertVariables(res, new String[] { "param1", "count", "count2", "e", "k" });
		}
		
		{
			String str= "return 9;";
			int offset= buf.toString().indexOf(str);
			
			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer().getDeclarationsInScope(astRoot, offset, flags);
			
			assertVariables(res, new String[] { "param1", "count", "count2", "x" });
		}		
	
		{
			String str= "return count;";
			int offset= buf.toString().indexOf(str);
			
			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer().getDeclarationsInScope(astRoot, offset, flags);
			
			assertVariables(res, new String[] { "param1", "count", "count2" });
		}		
		
	}
	
	public void testVariableDeclarations3() throws Exception {
		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class Rectangle {\n");
		buf.append("  public int x;\n");
		buf.append("  public int y;\n");
		buf.append("}\n");
		pack0.createCompilationUnit("Rectangle.java", buf.toString(), false, null);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		buf= new StringBuffer();
		buf.append("package test1.ae;\n");
		buf.append("import pack1.Rectangle;\n");
		buf.append("public class E {\n");
		buf.append("    private int fVar1, fVar2;\n");
		buf.append("    public int goo(int param1) {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            int fInner;\n");		
		buf.append("            public void run() {\n");
		buf.append("                int k= 0;\n");
		buf.append("                fVar1= k;\n");
		buf.append("            }\n");		
		buf.append("        };\n");
		buf.append("        int k= 0;\n");
		buf.append("        return k;\n");
		buf.append("    }\n");			
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		
		
		CompilationUnit astRoot= AST.parseCompilationUnit(compilationUnit, true);
		IProblem[] problems= astRoot.getProblems();
		assertTrue(problems.length == 0);
		
		{
			String str= "fVar1= k;";
			int offset= buf.toString().indexOf(str);
	
			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer().getDeclarationsInScope(astRoot, offset, flags);
			
			assertVariables(res, new String[] { "k", "fInner",  "param1", "run", "fVar1", "fVar2"});
		}
		
		{
			String str= "return k;";
			int offset= buf.toString().indexOf(str);
	
			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer().getDeclarationsInScope(astRoot, offset, flags);
			
			assertVariables(res, new String[] { "k", "param1", "run", "fVar1", "fVar2"});
		}

	}
	
	public void testVariableDeclarations4() throws Exception {
		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class Rectangle {\n");
		buf.append("  public int x;\n");
		buf.append("  public int y;\n");
		buf.append("}\n");
		pack0.createCompilationUnit("Rectangle.java", buf.toString(), false, null);
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		buf= new StringBuffer();
		buf.append("package test1.ae;\n");
		buf.append("import pack1.Rectangle;\n");
		buf.append("public class E {\n");
		buf.append("    private int fVar1, fVar2;\n");
		buf.append("    public int goo(int param1) {\n");
		buf.append("        int k= 0;\n");
		buf.append("        Rectangle r= new Rectangle();\n");		
		buf.append("        return r.x;\n");
		buf.append("    }\n");			
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);		
		
		CompilationUnit astRoot= AST.parseCompilationUnit(compilationUnit, true);
		IProblem[] problems= astRoot.getProblems();
		assertTrue(problems.length == 0);
		{
			String str= "return r.x;";
			int offset= buf.toString().indexOf(str) + "return r.".length();
	
			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer().getDeclarationsInScope(astRoot, offset, flags);
			
			assertVariables(res, new String[] { "x", "y"});
		}			
			
	}	
		
	

	private void assertVariables(IBinding[] res, String[] expectedNames) {
		String[] names= new String[res.length];		
		for (int i= 0; i < res.length; i++) {
			names[i]= res[i].getName();
		}
		assertEqualStringsIgnoreOrder(names, expectedNames);
	}

	

}
