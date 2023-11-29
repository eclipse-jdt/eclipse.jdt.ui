/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.fail;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;

import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class ScopeAnalyzerTest extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {

		fJProject1= pts.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(JavaCore.COMPILER_PB_HIDDEN_CATCH_BLOCK, JavaCore.IGNORE);
		options.put(JavaCore.COMPILER_PB_INCOMPLETE_ENUM_SWITCH, JavaCore.IGNORE);

		JavaCore.setOptions(options);
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	@Test
	public void testVariableDeclarations1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuilder buf= new StringBuilder();
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

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);

		{
			String str= "count+= count2;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "count", "count2", "fGlobal" });
		}

		{
			String str= "count++;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "count", "fGlobal" });
		}

		{
			String str= "return -1;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "count", "i", "insideFor", "fGlobal" });
		}

	}

	@Test
	public void testVariableDeclarations2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1.ae;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("public class E {\n");
		buf.append("    public int goo(int param1) {\n");
		buf.append("        int count= 9, count2= 0;\n");
		buf.append("        try {\n");
		buf.append("            for (int i= 0, j= 0; i < 9; i++) {\n");
		buf.append("                System.out.println(i + j);\n");
		buf.append("                j++;\n");
		buf.append("                throw new IOException();\n");
		buf.append("            }\n");
		buf.append("            return 8;\n");
		buf.append("        } catch (IOException e) {\n");
		buf.append("           int k= 0;\n");
		buf.append("           return k;\n");
		buf.append("        } catch (Exception x) {\n");
		buf.append("           x= null;\n");
		buf.append("        };\n");
		buf.append("        return count;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);

		{
			String str= "j++;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "count", "count2", "i", "j"});
		}

		{
			String str= "return 8;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "count", "count2"});
		}

		{
			String str= "return k;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "count", "count2", "e", "k" });
		}

		{
			String str= "x= null;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "count", "count2", "x" });
		}

		{
			String str= "return count;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "count", "count2" });
		}

	}

	@Test
	public void testVariableDeclarations3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1.ae;\n");
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

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);

		{
			String str= "fVar1= k;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "k", "fInner",  "param1", "run", "fVar1", "fVar2"});
		}

		{
			String str= "return k;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "k", "param1", "run", "fVar1", "fVar2"});
		}

	}

	@Test
	public void testVariableDeclarations4() throws Exception {
		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public class Rectangle {\n");
		buf.append("  public int x;\n");
		buf.append("  public int y;\n");
		buf.append("}\n");
		pack0.createCompilationUnit("Rectangle.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		buf= new StringBuilder();
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

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "return r.x;";
			int offset= buf.toString().indexOf(str) + "return r.".length();

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "x", "y"});
		}

	}

	@Test
	public void testVariableDeclarations5() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack1", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package pack1;\n");
		buf.append("\n");
		buf.append("public interface IConstants {\n");
		buf.append("  public final int CONST= 1;\n");
		buf.append("}\n");
		pack0.createCompilationUnit("IConstants.java", buf.toString(), false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		buf= new StringBuilder();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    private int fVar1, fVar2;\n");

		buf.append("    private class A {\n");
		buf.append("        int fCount;\n");
		buf.append("        public int foo(int param1) {\n");
		buf.append("            return 1;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public int goo(int param0) {\n");
		buf.append("        int k= 0;\n");
		buf.append("        class B extends A implements pack1.IConstants {\n");
		buf.append("            int fCount2;\n");
		buf.append("            public int foo(int param1) {\n");
		buf.append("                return 2;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("        return 3;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "return 1;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "fCount", "fVar1", "fVar2"});
		}

		{
			String str= "return 2;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "fCount2", "fCount", "k", "param0", "fVar1", "fVar2", "CONST"});
		}

	}

	@Test
	public void testVariableDeclarations6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    int[] fGlobal;\n");
		buf.append("    public int goo(int param1, int param2) {\n");
		buf.append("        switch (param1) {\n");
		buf.append("            case 1:\n;");
		buf.append("                fGlobal= new int[] { 1, 2, 3};\n");
		buf.append("                int temp= 9;\n");
		buf.append("                break;\n");
		buf.append("            case 2:\n;");
		buf.append("                do {\n");
		buf.append("                   int insideDo= 0;\n");
		buf.append("                   return -1;\n");
		buf.append("                } while (true);\n");
		buf.append("            case 3:\n;");
		buf.append("                int temp2= 9;\n");
		buf.append("                Math.min(1.0f, 2.0f);\n");
		buf.append("                return 3;\n");
		buf.append("        }\n");
		buf.append("        return 4;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);

		{
			String str= "break;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "temp", "fGlobal" });
		}

		{
			String str= "return -1;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "temp", "insideDo", "fGlobal" });
		}

		{
			String str= "Math";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "temp", "temp2", "fGlobal" });
		}

		{
			String str= "min";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "E", "PI" });
		}

		{
			String str= "return 4;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "fGlobal" });
		}

	}

	@Test
	public void testVariableDeclarations7() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    public void goo() {\n");
		buf.append("        try {\n");
		buf.append("        } catch (Exception x) {\n");
		buf.append("        }\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);

		{
			String str= "return;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] {});
		}
	}

	@Test
	public void testSwitchOnEnum() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1.ae;\n");
		buf.append("public enum E {\n");
		buf.append("    A, B, C;\n");
		buf.append("    public static final int X=1;\n");
		buf.append("}\n");
		buf.append("class A {\n");
		buf.append("    public void goo(E e) {\n");
		buf.append("        switch (e) {\n");
		buf.append("            case A:\n;");
		buf.append("                break;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "A:";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "A", "B", "C"});
		}
		{
			String str= "break;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] {"e"});
		}
	}

	@Test
	public void testDeclarationsAfter() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    public int goo(final int param0) {\n");
		buf.append("        int k= 0;\n");
		buf.append("        try {\n");
		buf.append("            for (int i= 0; i < 10; i++) {\n");
		buf.append("                k += i;\n");
		buf.append("            }\n");
		buf.append("        } catch (Exception x) {\n");
		buf.append("           return 9;\n");
		buf.append("        };\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            int fInner;\n");
		buf.append("            public void run() {\n");
		buf.append("                int x1= 0;\n");
		buf.append("                x1 += param0;\n");
		buf.append("                {\n");
		buf.append("                    for (int i= 0, j= 0; i < 10; i++) {\n");
		buf.append("                        x1 += i;\n");
		buf.append("                        int x2= 0;\n");
		buf.append("                    }\n");
		buf.append("                }\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        return 3;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "int k= 0;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsAfter(offset, flags);

			assertVariables(res, new String[] { "k", "i", "x", "run"});
		}

		{
			String str= "return 9;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsAfter(offset, flags);

			assertVariables(res, new String[] { });
		}

		{
			String str= "x1 += param0;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsAfter(offset, flags);

			assertVariables(res, new String[] { "i", "j", "x2" });
		}

	}

	@Test
	public void testTypeDeclarations1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    public static class A {\n");
		buf.append("        public class A1 {\n");
		buf.append("            public int foo() {\n");
		buf.append("                return 1;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("        public class A2 {\n");
		buf.append("        }\n");
		buf.append("        public int foo() {\n");
		buf.append("            return 2;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class F {\n");
		buf.append("    public int goo(int param0) {\n");
		buf.append("        class C extends E.A {\n");
		buf.append("            A1 b;\n");
		buf.append("            public int foo() {\n");
		buf.append("                return 3;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("        return 4;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "return 1;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "A1", "A", "E", "A2", "F"});
		}

		{
			String str= "return 2;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "A1", "A", "E", "A2", "F"});
		}

		{
			String str= "return 3;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "C", "F", "A1", "A2", "E"});
		}

		{
			String str= "return 4;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "C", "F", "E"});
		}

	}

	@Test
	public void testTypeDeclarations2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    public static class E1 extends G {\n");
		buf.append("        public static class EE1 {\n");
		buf.append("        }\n");
		buf.append("        public static class EE2 {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    public static class E2 {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class F extends E.E1{\n");
		buf.append("    F f1;\n");
		buf.append("}\n");
		buf.append("class G {\n");
		buf.append("    public static class G1 {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "F f1;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "F", "EE1", "EE2", "G1", "G", "E"});
		}
	}

	@Test
	public void testTypeDeclarationsTypeParameters() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("test0.ae", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1.ae;\n");
		buf.append("public class H<M> {\n");
		buf.append("}\n");
		pack0.createCompilationUnit("H.java", buf.toString(), false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		buf= new StringBuilder();
		buf.append("package test1.ae;\n");
		buf.append("import test0.ae.H;\n");
		buf.append("public class G<X, Y> extends H<String> {\n");
		buf.append("    public <A, B> void foo() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");
		buf.append("}\n");

		ICompilationUnit compilationUnit= pack1.createCompilationUnit("G.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "return;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "A", "B", "G", "X", "Y"});
		}
	}

	@Test
	public void testClassInstanceCreation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    public Object foo(G g) {\n");
		buf.append("        return g.new G1();\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class G extends H {\n");
		buf.append("    public class G1 {\n");
		buf.append("    }\n");
		buf.append("    public class G2 {\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("class H {\n");
		buf.append("    public class H1 {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "G1()";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "G1", "G2", "H1"});
		}
	}



	@Test
	public void testMethodDeclarations1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");

		buf.append("    public void goo() {\n");
		buf.append("        return;\n");
		buf.append("    }\n");

		buf.append("    public String toString() {\n");
		buf.append("        return String.valueOf(1);\n");
		buf.append("    }\n");

		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "return;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.METHODS;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertMethods(res, new String[] { "goo", "foo" }, true);
		}

	}

	@Test
	public void testMethodDeclarations2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test1.ae;\n");
		buf.append("public class E {\n");
		buf.append("    int fVar1, fVar2;\n");
		buf.append("    public int goo(int param1) {\n");
		buf.append("        Runnable run= new Runnable() {\n");
		buf.append("            int fInner;\n");
		buf.append("            public void run() {\n");
		buf.append("                return;\n");
		buf.append("            }\n");
		buf.append("        };\n");
		buf.append("        int k= 0;\n");
		buf.append("        return k;\n");
		buf.append("    }\n");
		buf.append("    private class A extends E {\n");
		buf.append("        { // initializer\n");
		buf.append("           fVar1= 9; \n");
		buf.append("        }\n");
		buf.append("        public int foo(int param1) {\n");
		buf.append("            return 1;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);

		{
			String str= "return;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.METHODS;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertMethods(res, new String[] { "run", "goo"}, true);
		}

		{
			String str= "return k;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.METHODS;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertMethods(res, new String[] { "goo"}, true);
		}

		{
			String str= "return 1;";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.METHODS;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertMethods(res, new String[] { "foo", "goo"}, true);
		}

	}


	private static final String[] OBJ_METHODS= new String[] { "getClass",
		"hashCode", "equals", "clone", "toString", "notify", "notifyAll", "wait", "wait",
		"wait", "finalize" };

	private void assertMethods(IBinding[] res, String[] expectedNames, boolean addObjectMethods) {
		String[] names= new String[res.length];
		for (int i= 0; i < res.length; i++) {
			names[i]= res[i].getName();
		}
		String[] expected= expectedNames;

		if (addObjectMethods) {
			expected= new String[expectedNames.length + OBJ_METHODS.length];
			System.arraycopy(OBJ_METHODS, 0, expected, 0, OBJ_METHODS.length);
			System.arraycopy(expectedNames, 0, expected, OBJ_METHODS.length, expectedNames.length);
		}

		assertEqualStringsIgnoreOrder(names, expected);
	}

	@Test
	public void testEnumConstantDeclaration1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test", false, null);
		StringBuilder buf= new StringBuilder();
		buf.append("package test;\n");
		buf.append("\n");
		buf.append("public enum TestEnum {\n");
		buf.append("    A(11);\n");
		buf.append("\n");
		buf.append("    TestEnum(int value) {}\n");
		buf.append("\n");
		buf.append("    private static int getDefaultValue() {\n");
		buf.append("        return -1;\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("TestEnum.java", buf.toString(), false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "11";
			int offset= buf.toString().indexOf(str);

			int flags= ScopeAnalyzer.METHODS;
			for (IBinding binding : new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags)) {
				if ("getDefaultValue".equals(binding.getName())) {
					return;
				}
			}
			fail("getDefaultValue not found");
		}

	}

	private void assertVariables(IBinding[] res, String[] expectedNames) {
		String[] names= new String[res.length];
		for (int i= 0; i < res.length; i++) {
			names[i]= res[i].getName();
		}
		assertEqualStringsIgnoreOrder(names, expectedNames);
	}

	private void assertNoProblems(CompilationUnit astRoot) {
		IProblem[] problems= astRoot.getProblems();
		if (problems.length > 0) {
			StringBuilder buf= new StringBuilder();
			for (IProblem problem : problems) {
				buf.append(problem.getMessage()).append('\n');
			}
			fail(buf.toString());
		}
	}

	private CompilationUnit createAST(ICompilationUnit compilationUnit) {
		ASTParser parser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
		parser.setSource(compilationUnit);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
	}


}
