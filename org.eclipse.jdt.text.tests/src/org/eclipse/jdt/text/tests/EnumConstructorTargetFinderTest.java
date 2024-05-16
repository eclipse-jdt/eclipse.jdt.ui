/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.core.manipulation.search.IOccurrencesFinder.OccurrenceLocation;

import org.eclipse.jdt.ui.tests.core.rules.Java1d8ProjectTestSetup;

import org.eclipse.jdt.internal.ui.javaeditor.JavaElementHyperlinkDetector;

/**
 * Tests for the BreakContinueTargerFinder class.
 *
 * @since 3.22
 */
public class EnumConstructorTargetFinderTest {
	@Rule
	public Java1d8ProjectTestSetup f18p= new Java1d8ProjectTestSetup();

	private ASTParser fASTParser;

	private IJavaProject fJavaProject;

	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		fASTParser= ASTParser.newParser(AST.getJLSLatest());
		fJavaProject= f18p.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJavaProject, f18p.getDefaultClasspath());
	}

	private CompilationUnit createCompilationUnit(String source) throws JavaModelException {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", source, true, null);
		fASTParser.setSource(cu);
		fASTParser.setResolveBindings(true);
		return (CompilationUnit) fASTParser.createAST(null);
	}

	private OccurrenceLocation getTarget(CompilationUnit root, int offset, int length) throws Exception {
		CompilationUnit astRoot= SharedASTProviderCore.getAST(root.getTypeRoot(), SharedASTProviderCore.WAIT_YES, null);
		return JavaElementHyperlinkDetector.findEnumConstructorTarget(astRoot, new Region(offset, length));
	}

	private void checkSelection(CompilationUnit root, int offset, int length, OccurrenceLocation expected) throws Exception {
		OccurrenceLocation selectedNode= getTarget(root, offset, length);
		assertNotNull("Selection null", selectedNode);
		assertEquals("Offset\n" + expected.getDescription(), expected.getOffset(), selectedNode.getOffset());
		assertEquals("Length\n" + expected.getDescription(), expected.getLength(), selectedNode.getLength());
	}

	@Test
	public void testEnumConstructorFinder_1() throws Exception {
		String s= """
			package test1;
			class E {
			   public enum A {
			      A1,
			      A2;
			   }
			}
			""";

		CompilationUnit root= createCompilationUnit(s);

		{
			int offset= s.indexOf("A1,");
			int targetOffset= s.indexOf("A1,");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 2 /* 'A1' */, 0, "A1"));
		}
	}

	@Test
	public void testEnumConstructorFinder_2() throws Exception {
		String s= """
			package test1;
			class E {
			   public enum A {
			      A1(B.B1),
			      A2(1, B.B2),
			      A3(B.B2, 2);
			      A(B b) {
			      }
			      A(B b, int i) {
			      }
			      A(int i, B b) {
			      }
			   }
			   public enum B {
			      B1,
			      B2;
			   }
			}
			""";

		CompilationUnit root= createCompilationUnit(s);

		{
			int offset= s.indexOf("A1(B.B1)");
			int targetOffset= s.indexOf("A(B b)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(B b)"));
		}
		{
			int offset= s.indexOf("A2(1, B.B2)");
			int targetOffset= s.indexOf("A(int i, B b)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(int i, B b)"));
		}
		{
			int offset= s.indexOf("A3(B.B2, 2)");
			int targetOffset= s.indexOf("A(B b, int i)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(B b, int i)"));
		}
	}

	@Test
	public void testEnumConstructorFinder_3() throws Exception {
		String s= """
			package test1;
			class E {
			   public enum A {
			      A1(1, 2),
			      A2(1, 2.0),
			      A3(2.0, 1);
			      A(int i1, int i2) {
			      }
			      A(double d, int i) {
			      }
			      A(int i, double d) {
			      }
			   }
			}
			""";

		CompilationUnit root= createCompilationUnit(s);

		{
			int offset= s.indexOf("A1(1, 2)");
			int targetOffset= s.indexOf("A(int i1, int i2)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(int i1, int i2)"));
		}
		{
			int offset= s.indexOf("A2(1, 2.0)");
			int targetOffset= s.indexOf("A(int i, double d)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(int i, double d)"));
		}
		{
			int offset= s.indexOf("A3(2.0, 1)");
			int targetOffset= s.indexOf("A(double d, int i)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(double d, int i)"));
		}
	}

	@Test
	public void testEnumConstructorFinder_4() throws Exception {
		String s= """
			package test1;
			class E {
			   public enum A {
			      A1(B.B1),
			      A2(B.B2, 1),
			      A3(1, B.B2, 2),
			      A4(B.B1, 3),
			      A5(B.B1, Double.valueOf(5.0)),
			      A6("string");
			      void foo(String s) {
			      }
			      A(B b) {
			      }
			      A(B b, int i) {
			      }
			      A(B b, Double d) {
			      }
			      A(int i1, B b, int i2) {
			      }
			      A(String s) {
			      }
			   }
			   public enum B {
			      B1,
			      B2;
			   }
			}
			""";

		CompilationUnit root= createCompilationUnit(s);

		{
			int offset= s.indexOf("A1(B.B1)");
			int targetOffset= s.indexOf("A(B b)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(B b)"));
		}
		{
			int offset= s.indexOf("A2(B.B2, 1)");
			int targetOffset= s.indexOf("A(B b, int i)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(B b, int i)"));
		}
		{
			int offset= s.indexOf("A3(1, B.B2, 2)");
			int targetOffset= s.indexOf("A(int i1, B b, int i2)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(int i1, B b, int i2)"));
		}
		{
			int offset= s.indexOf("A4(B.B1, 3)");
			int targetOffset= s.indexOf("A(B b, int i)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(B b, int i)"));
		}
		{
			int offset= s.indexOf("A5(B.B1, Double.valueOf(5.0))");
			int targetOffset= s.indexOf("A(B b, Double d)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(B b, Double d)"));
		}
		{
			int offset= s.indexOf("A6(\"string\")");
			int targetOffset= s.indexOf("A(String s)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(String s)"));
		}
	}

	@Test
	public void testEnumConstructorFinder_5() throws Exception {
		String s= """
			package test1;
			class E {
			   public enum A {
			      A1(""),
			      A2("","");
			      A(String ... strings) {
			      }
			      A(String s) {
			      }
			   }
			}
			""";

		CompilationUnit root= createCompilationUnit(s);

		{
			int offset= s.indexOf("A1(\"\")");
			int targetOffset= s.indexOf("A(String s)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1, 0, "A(String s)"));
		}
		{
			int offset= s.indexOf("A2(\"\",\"\")");
			int targetOffset= s.indexOf("A(String ... strings)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1, 0, "A(String ... strings)"));
		}
	}

	@Test
	public void testEnumConstructorFinder_6() throws Exception {
		String s= """
			package test1;
			class E {
			   public enum A {
			      A1(""),
			      A2(new Object());
			      A(String s) {
			      }
			      A(Object o) {
			      }
			   }
			}
			""";

		CompilationUnit root= createCompilationUnit(s);

		{
			int offset= s.indexOf("A1");
			int targetOffset= s.indexOf("A(String");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(String"));
		}
		{
			int offset= s.indexOf("A2");
			int targetOffset= s.indexOf("A(Object");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1 /* 'A' */, 0, "A(Object"));
		}
	}

	@Test
	public void testEnumConstructorFinder_7() throws Exception {
		String s= """
			package test1;
			class E {
			   public enum A {
			      A1(1L),
			      A2(new Long(1)),
			      A3(new Long(1), 1L),
			      A4(Long.valueOf(1)),
			      A5(Long.parseLong("1")),
			      A6(1L, 2.0),
			      A7(Long.valueOf(1), Double.valueOf(2.0));
			      A(long l) {
			      }
			      A(Long l) {
			      }
			      A(long l, double d) {
			      }
			      A(Long l, Double d) {
			      }
			   }
			}
			""";

		CompilationUnit root= createCompilationUnit(s);

		{
			int offset= s.indexOf("A1");
			int targetOffset= s.indexOf("A(long l)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1, 0, "A(long l)"));
		}
		{
			int offset= s.indexOf("A2");
			int targetOffset= s.indexOf("A(Long l)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1, 0, "A(Long l)"));
		}
		{
			int offset= s.indexOf("A3");
			int targetOffset= s.indexOf("A(long l, double d)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1, 0, "A(long l, double d)"));
		}
		{
			int offset= s.indexOf("A4");
			int targetOffset= s.indexOf("A(Long l)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1, 0, "A(Long l)"));
		}
		{
			int offset= s.indexOf("A5");
			int targetOffset= s.indexOf("A(long l)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1, 0, "A(long l)"));
		}
		{
			int offset= s.indexOf("A6");
			int targetOffset= s.indexOf("A(long l, double d)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1, 0, "A(long l, double d)"));
		}
		{
			int offset= s.indexOf("A7");
			int targetOffset= s.indexOf("A(Long l, Double d)");
			checkSelection(root, offset, 0, new OccurrenceLocation(targetOffset, 1, 0, "A(Long l, Double d)"));
		}
	}
}
