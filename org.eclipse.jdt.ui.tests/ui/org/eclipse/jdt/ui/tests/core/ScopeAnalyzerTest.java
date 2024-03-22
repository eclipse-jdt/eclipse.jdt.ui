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
		String str1= """
			package test1.ae;
			public class E {
			    int[] fGlobal;
			    public int goo(int param1, int param2) {
			        int count= 0;
			        fGlobal= new int[] { 1, 2, 3};
			        for (int i= 0; i < fGlobal.length; i++) {
			            int insideFor= 0;
			            count= insideFor + fGlobal[i];
			            return -1;
			        }
			        count++;
			        int count2= 0;
			        count+= count2;
			        return count;
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);

		{
			String str= "count+= count2;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "count", "count2", "fGlobal" });
		}

		{
			String str= "count++;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "count", "fGlobal" });
		}

		{
			String str= "return -1;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "count", "i", "insideFor", "fGlobal" });
		}

	}

	@Test
	public void testVariableDeclarations2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str1= """
			package test1.ae;
			import java.io.IOException;
			public class E {
			    public int goo(int param1) {
			        int count= 9, count2= 0;
			        try {
			            for (int i= 0, j= 0; i < 9; i++) {
			                System.out.println(i + j);
			                j++;
			                throw new IOException();
			            }
			            return 8;
			        } catch (IOException e) {
			           int k= 0;
			           return k;
			        } catch (Exception x) {
			           x= null;
			        };
			        return count;
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);

		{
			String str= "j++;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "count", "count2", "i", "j"});
		}

		{
			String str= "return 8;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "count", "count2"});
		}

		{
			String str= "return k;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "count", "count2", "e", "k" });
		}

		{
			String str= "x= null;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "count", "count2", "x" });
		}

		{
			String str= "return count;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "count", "count2" });
		}

	}

	@Test
	public void testVariableDeclarations3() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str1= """
			package test1.ae;
			public class E {
			    private int fVar1, fVar2;
			    public int goo(int param1) {
			        Runnable run= new Runnable() {
			            int fInner;
			            public void run() {
			                int k= 0;
			                fVar1= k;
			            }
			        };
			        int k= 0;
			        return k;
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);

		{
			String str= "fVar1= k;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "k", "fInner",  "param1", "run", "fVar1", "fVar2"});
		}

		{
			String str= "return k;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "k", "param1", "run", "fVar1", "fVar2"});
		}

	}

	@Test
	public void testVariableDeclarations4() throws Exception {
		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			
			public class Rectangle {
			  public int x;
			  public int y;
			}
			""";
		pack0.createCompilationUnit("Rectangle.java", str1, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str2= """
			package test1.ae;
			import pack1.Rectangle;
			public class E {
			    private int fVar1, fVar2;
			    public int goo(int param1) {
			        int k= 0;
			        Rectangle r= new Rectangle();
			        return r.x;
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "return r.x;";
			int offset= str2.indexOf(str) + "return r.".length();

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "x", "y"});
		}

	}

	@Test
	public void testVariableDeclarations5() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("pack1", false, null);
		String str1= """
			package pack1;
			
			public interface IConstants {
			  public final int CONST= 1;
			}
			""";
		pack0.createCompilationUnit("IConstants.java", str1, false, null);

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str2= """
			package test1.ae;
			public class E {
			    private int fVar1, fVar2;
			    private class A {
			        int fCount;
			        public int foo(int param1) {
			            return 1;
			        }
			    }
			    public int goo(int param0) {
			        int k= 0;
			        class B extends A implements pack1.IConstants {
			            int fCount2;
			            public int foo(int param1) {
			                return 2;
			            }
			        }
			        return 3;
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str2, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "return 1;";
			int offset= str2.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "fCount", "fVar1", "fVar2"});
		}

		{
			String str= "return 2;";
			int offset= str2.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "fCount2", "fCount", "k", "param0", "fVar1", "fVar2", "CONST"});
		}

	}

	@Test
	public void testVariableDeclarations6() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str1= """
			package test1.ae;
			public class E {
			    int[] fGlobal;
			    public int goo(int param1, int param2) {
			        switch (param1) {
			            case 1:
			;\
			                fGlobal= new int[] { 1, 2, 3};
			                int temp= 9;
			                break;
			            case 2:
			;\
			                do {
			                   int insideDo= 0;
			                   return -1;
			                } while (true);
			            case 3:
			;\
			                int temp2= 9;
			                Math.min(1.0f, 2.0f);
			                return 3;
			        }
			        return 4;
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);

		{
			String str= "break;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "temp", "fGlobal" });
		}

		{
			String str= "return -1;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "temp", "insideDo", "fGlobal" });
		}

		{
			String str= "Math";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "temp", "temp2", "fGlobal" });
		}

		{
			String str= "min";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "E", "PI" });
		}

		{
			String str= "return 4;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "param1", "param2", "fGlobal" });
		}

	}

	@Test
	public void testVariableDeclarations7() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str1= """
			package test1.ae;
			public class E {
			    public void goo() {
			        try {
			        } catch (Exception x) {
			        }
			        return;
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);

		{
			String str= "return;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] {});
		}
	}

	@Test
	public void testSwitchOnEnum() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str1= """
			package test1.ae;
			public enum E {
			    A, B, C;
			    public static final int X=1;
			}
			class A {
			    public void goo(E e) {
			        switch (e) {
			            case A:
			;\
			                break;
			        }
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "A:";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "A", "B", "C"});
		}
		{
			String str= "break;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] {"e"});
		}
	}

	@Test
	public void testDeclarationsAfter() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str1= """
			package test1.ae;
			public class E {
			    public int goo(final int param0) {
			        int k= 0;
			        try {
			            for (int i= 0; i < 10; i++) {
			                k += i;
			            }
			        } catch (Exception x) {
			           return 9;
			        };
			        Runnable run= new Runnable() {
			            int fInner;
			            public void run() {
			                int x1= 0;
			                x1 += param0;
			                {
			                    for (int i= 0, j= 0; i < 10; i++) {
			                        x1 += i;
			                        int x2= 0;
			                    }
			                }
			            }
			        };
			        return 3;
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "int k= 0;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsAfter(offset, flags);

			assertVariables(res, new String[] { "k", "i", "x", "run"});
		}

		{
			String str= "return 9;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsAfter(offset, flags);

			assertVariables(res, new String[] { });
		}

		{
			String str= "x1 += param0;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.VARIABLES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsAfter(offset, flags);

			assertVariables(res, new String[] { "i", "j", "x2" });
		}

	}

	@Test
	public void testTypeDeclarations1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str1= """
			package test1.ae;
			public class E {
			    public static class A {
			        public class A1 {
			            public int foo() {
			                return 1;
			            }
			        }
			        public class A2 {
			        }
			        public int foo() {
			            return 2;
			        }
			    }
			}
			class F {
			    public int goo(int param0) {
			        class C extends E.A {
			            A1 b;
			            public int foo() {
			                return 3;
			            }
			        }
			        return 4;
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "return 1;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "A1", "A", "E", "A2", "F"});
		}

		{
			String str= "return 2;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "A1", "A", "E", "A2", "F"});
		}

		{
			String str= "return 3;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "C", "F", "A1", "A2", "E"});
		}

		{
			String str= "return 4;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "C", "F", "E"});
		}

	}

	@Test
	public void testTypeDeclarations2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str1= """
			package test1.ae;
			public class E {
			    public static class E1 extends G {
			        public static class EE1 {
			        }
			        public static class EE2 {
			        }
			    }
			    public static class E2 {
			    }
			}
			class F extends E.E1{
			    F f1;
			}
			class G {
			    public static class G1 {
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "F f1;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "F", "EE1", "EE2", "G1", "G", "E"});
		}
	}

	@Test
	public void testTypeDeclarationsTypeParameters() throws Exception {

		IPackageFragment pack0= fSourceFolder.createPackageFragment("test0.ae", false, null);
		String str1= """
			package test1.ae;
			public class H<M> {
			}
			""";
		pack0.createCompilationUnit("H.java", str1, false, null);


		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str2= """
			package test1.ae;
			import test0.ae.H;
			public class G<X, Y> extends H<String> {
			    public <A, B> void foo() {
			        return;
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("G.java", str2, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "return;";
			int offset= str2.indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "A", "B", "G", "X", "Y"});
		}
	}

	@Test
	public void testClassInstanceCreation() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str1= """
			package test1.ae;
			public class E {
			    public Object foo(G g) {
			        return g.new G1();
			    }
			}
			class G extends H {
			    public class G1 {
			    }
			    public class G2 {
			    }
			}
			class H {
			    public class H1 {
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "G1()";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.TYPES;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertVariables(res, new String[] { "G1", "G2", "H1"});
		}
	}



	@Test
	public void testMethodDeclarations1() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str1= """
			package test1.ae;
			public class E {
			    public void foo() {
			    }
			    public void goo() {
			        return;
			    }
			    public String toString() {
			        return String.valueOf(1);
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "return;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.METHODS;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertMethods(res, new String[] { "goo", "foo" }, true);
		}

	}

	@Test
	public void testMethodDeclarations2() throws Exception {

		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1.ae", false, null);
		String str1= """
			package test1.ae;
			public class E {
			    int fVar1, fVar2;
			    public int goo(int param1) {
			        Runnable run= new Runnable() {
			            int fInner;
			            public void run() {
			                return;
			            }
			        };
			        int k= 0;
			        return k;
			    }
			    private class A extends E {
			        { // initializer
			           fVar1= 9;\s
			        }
			        public int foo(int param1) {
			            return 1;
			        }
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("E.java", str1, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);

		{
			String str= "return;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.METHODS;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertMethods(res, new String[] { "run", "goo"}, true);
		}

		{
			String str= "return k;";
			int offset= str1.indexOf(str);

			int flags= ScopeAnalyzer.METHODS;
			IBinding[] res= new ScopeAnalyzer(astRoot).getDeclarationsInScope(offset, flags);

			assertMethods(res, new String[] { "goo"}, true);
		}

		{
			String str= "return 1;";
			int offset= str1.indexOf(str);

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
		String str1= """
			package test;
			
			public enum TestEnum {
			    A(11);
			
			    TestEnum(int value) {}
			
			    private static int getDefaultValue() {
			        return -1;
			    }
			}
			""";
		ICompilationUnit compilationUnit= pack1.createCompilationUnit("TestEnum.java", str1, false, null);

		CompilationUnit astRoot= createAST(compilationUnit);
		assertNoProblems(astRoot);
		{
			String str= "11";
			int offset= str1.indexOf(str);

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
