/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class PartialASTTest extends CoreTests {

	private static final Class THIS= PartialASTTest.class;

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public PartialASTTest(String name) {
		super(name);
	}


	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new PartialASTTest("testPartialCU1"));
			return new ProjectTestSetup(suite);
		}
	}


	protected void setUp() throws Exception {
		Hashtable options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= ProjectTestSetup.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}


	public void testPartialCU1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private int fField1;\n");
		buf.append("    private int fField2;\n");
		buf.append("    public void foo1() {\n");
		buf.append("        fField1 = fField2;\n");
		buf.append("    }\n");
		buf.append("    public int foo1(int i) {\n");
		buf.append("        return i;\n");
		buf.append("    }\n");
		buf.append("}");
		String existing= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", existing, false, null);

		String statement= "fField1 = fField2;";
		int offset= existing.indexOf(statement);

		CompilationUnit astRoot= getPartialCompilationUnit(cu, offset);
		String string= ASTNodes.asFormattedString(astRoot, 0, String.valueOf('\n'), null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private int fField1;\n");
		buf.append("    private int fField2;\n");
		buf.append("    public void foo1() {\n");
		buf.append("        fField1 = fField2;\n");
		buf.append("    }\n");
		buf.append("    public int foo1(int i) {\n");
		buf.append("    }\n");
		buf.append("}");
		String expected= buf.toString();

		assertEqualString(string, expected);

		offset= expected.indexOf(statement);

		ASTNode node= NodeFinder.perform(astRoot, offset, statement.length());
		Assignment assignment= (Assignment) ((ExpressionStatement) node).getExpression();
		Expression e1= assignment.getLeftHandSide();
		Expression e2= assignment.getRightHandSide();
		assertNotNull(e1.resolveTypeBinding());
		assertNotNull(e2.resolveTypeBinding());

		assertTrue(((SimpleName) e1).resolveBinding() instanceof IVariableBinding);
		assertTrue(((SimpleName) e2).resolveBinding() instanceof IVariableBinding);

		assertAllBindings(astRoot);
	}

	private void assertAllBindings(CompilationUnit astRoot) {
		List list= astRoot.types();
		for (int i= 0; i < list.size(); i++) {
			TypeDeclaration decl= (TypeDeclaration) list.get(i);
			assertTrue(decl.resolveBinding() != null);

			if (!decl.isInterface() && decl.getSuperclassType() != null) {
				assertTrue(decl.getSuperclassType().resolveBinding() != null);
			}
			List interfaces= decl.superInterfaceTypes();
			for (int j= 0; j < interfaces.size(); j++) {
				assertTrue(((Type) interfaces.get(j)).resolveBinding() != null);
			}

			MethodDeclaration[] declarations= decl.getMethods();
			for (int k= 0; k < declarations.length; k++) {
				MethodDeclaration meth= declarations[k];
				assertTrue(meth.resolveBinding() != null);
				List params= meth.parameters();
				for (int n= 0; n < params.size(); n++) {
					SingleVariableDeclaration arg= (SingleVariableDeclaration) params.get(n);
					assertTrue(arg.resolveBinding() != null);
				}
				if (!meth.isConstructor()) {
					assertTrue(meth.getReturnType2().resolveBinding() != null);
				}
			}
		}


	}


	public void testPartialCU2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private class EInner {\n");
		buf.append("        public int inner(int i) {\n");
		buf.append("            return i;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private int fField1;\n");
		buf.append("    private int fField2;\n");
		buf.append("    public void foo1() {\n");
		buf.append("        fField1 = fField2;\n");
		buf.append("        if (fField1 == 0) {\n");
		buf.append("            fField2++;\n");
		buf.append("        }\n");
		buf.append("        EInner inner = new EInner();\n");
		buf.append("    }\n");
		buf.append("    public int foo1(int i) {\n");
		buf.append("        private class Local {\n");
		buf.append("            public int local(int i) {\n");
		buf.append("                return i;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("        Local local = new Local();\n");
		buf.append("        return i;\n");
		buf.append("    }\n");
		buf.append("}");
		String existing= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", existing, false, null);

		int offset= existing.indexOf("fField1 = fField2;");

		CompilationUnit astRoot= getPartialCompilationUnit(cu, offset);
		String string= ASTNodes.asFormattedString(astRoot, 0, String.valueOf('\n'), null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("public class E {\n");
		buf.append("    private class EInner {\n");
		buf.append("        public int inner(int i) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private int fField1;\n");
		buf.append("    private int fField2;\n");
		buf.append("    public void foo1() {\n");
		buf.append("        fField1 = fField2;\n");
		buf.append("        if (fField1 == 0) {\n");
		buf.append("            fField2++;\n");
		buf.append("        }\n");
		buf.append("        EInner inner = new EInner();\n");
		buf.append("    }\n");
		buf.append("    public int foo1(int i) {\n");
		buf.append("    }\n");
		buf.append("}");
		String expected= buf.toString();

		assertEqualString(string, expected);
		assertAllBindings(astRoot);
	}

	public void testPartialCU3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class E {\n");
		buf.append("    private class EInner implements Serializable {\n");
		buf.append("        public int inner(int i) {\n");
		buf.append("            return i;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private int fField1;\n");
		buf.append("    private int fField2;\n");
		buf.append("    public void foo1() {\n");
		buf.append("        fField1 = fField2;\n");
		buf.append("        if (fField1 == 0) {\n");
		buf.append("            fField2++;\n");
		buf.append("        }\n");
		buf.append("        EInner inner = new EInner();\n");
		buf.append("    }\n");
		buf.append("    public int foo1(int i) {\n");
		buf.append("        private class Local {\n");
		buf.append("            public int local(int i) {\n");
		buf.append("                return 1;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("        Local local = new Local();\n");
		buf.append("        return i;\n");
		buf.append("    }\n");
		buf.append("}");
		String existing= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", existing, false, null);

		int offset= existing.indexOf("return 1;");

		CompilationUnit astRoot= getPartialCompilationUnit(cu, offset);
		String string= ASTNodes.asFormattedString(astRoot, 0, String.valueOf('\n'), null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.io.Serializable;\n");
		buf.append("public class E {\n");
		buf.append("    private class EInner implements Serializable {\n");
		buf.append("        public int inner(int i) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private int fField1;\n");
		buf.append("    private int fField2;\n");
		buf.append("    public void foo1() {\n");
		buf.append("    }\n");
		buf.append("    public int foo1(int i) {\n");
		buf.append("        private class Local {\n");
		buf.append("            public int local(int i) {\n");
		buf.append("                return 1;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("        Local local = new Local();\n");
		buf.append("        return i;\n");
		buf.append("    }\n");
		buf.append("}");
		String expected= buf.toString();

		assertEqualString(string, expected);
		assertAllBindings(astRoot);
	}

	public void testPartialCU4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    private class EInner {\n");
		buf.append("        public int inner(int i) {\n");
		buf.append("            return 0;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private int fField1;\n");
		buf.append("    private int fField2;\n");
		buf.append("    public void foo1() throws IOException, ParseException {\n");
		buf.append("        fField1 = fField2;\n");
		buf.append("        if (fField1 == 0) {\n");
		buf.append("            fField2++;\n");
		buf.append("        }\n");
		buf.append("        EInner inner = new EInner();\n");
		buf.append("    }\n");
		buf.append("    public int foo1(int i) {\n");
		buf.append("        private class Local {\n");
		buf.append("            public int local(int i) {\n");
		buf.append("                return 1;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("        Local local = new Local();\n");
		buf.append("        return i;\n");
		buf.append("    }\n");
		buf.append("}");
		String existing= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", existing, false, null);

		int offset= existing.indexOf("return 0;");

		CompilationUnit astRoot= getPartialCompilationUnit(cu, offset);
		String string= ASTNodes.asFormattedString(astRoot, 0, String.valueOf('\n'), null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    private class EInner {\n");
		buf.append("        public int inner(int i) {\n");
		buf.append("            return 0;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private int fField1;\n");
		buf.append("    private int fField2;\n");
		buf.append("    public void foo1() throws IOException, ParseException {\n");
		buf.append("    }\n");
		buf.append("    public int foo1(int i) {\n");
		buf.append("    }\n");
		buf.append("}");
		String expected= buf.toString();

		assertEqualString(string, expected);
		assertAllBindings(astRoot);
	}

	public void testPartialCUPositionNotInMethod1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    private class EInner {\n");
		buf.append("        public int inner(int i) {\n");
		buf.append("            return 0;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private int fField1;\n");
		buf.append("    private int fField2;\n");
		buf.append("    public void foo1() throws IOException, ParseException {\n");
		buf.append("        fField1 = fField2;\n");
		buf.append("        if (fField1 == 0) {\n");
		buf.append("            fField2++;\n");
		buf.append("        }\n");
		buf.append("        EInner inner = new EInner();\n");
		buf.append("    }\n");
		buf.append("    public int foo2(int i) {\n");
		buf.append("        private class Local {\n");
		buf.append("            public int local(int i) {\n");
		buf.append("                return 1;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("        Local local = new Local();\n");
		buf.append("        return i;\n");
		buf.append("    }\n");
		buf.append("}");
		String existing= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", existing, false, null);

		int offset= existing.indexOf("private int fField1;");

		CompilationUnit astRoot= getPartialCompilationUnit(cu, offset);
		String string= ASTNodes.asFormattedString(astRoot, 0, String.valueOf('\n'), null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    private class EInner {\n");
		buf.append("        public int inner(int i) {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private int fField1;\n");
		buf.append("    private int fField2;\n");
		buf.append("    public void foo1() throws IOException, ParseException {\n");
		buf.append("    }\n");
		buf.append("    public int foo2(int i) {\n");
		buf.append("    }\n");
		buf.append("}");
		String expected= buf.toString();

		assertEqualString(string, expected);
		assertAllBindings(astRoot);
	}

	public void testPartialCUPositionNotInMethod2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    private class EInner {\n");
		buf.append("        {\n");
		buf.append("            System.out.println();\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private int fField1;\n");
		buf.append("    private int fField2;\n");
		buf.append("    public void foo1() throws IOException, ParseException {\n");
		buf.append("        fField1 = fField2;\n");
		buf.append("        if (fField1 == 0) {\n");
		buf.append("            fField2++;\n");
		buf.append("        }\n");
		buf.append("        EInner inner = new EInner();\n");
		buf.append("    }\n");
		buf.append("    public int foo2(int i) {\n");
		buf.append("        private class Local {\n");
		buf.append("            private int fField3;\n");
		buf.append("            public int local(int i) {\n");
		buf.append("                return 1;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("        Local local = new Local();\n");
		buf.append("        return i;\n");
		buf.append("    }\n");
		buf.append("}");
		String existing= buf.toString();
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", existing, false, null);

		int offset= existing.indexOf("private int fField3;");

		CompilationUnit astRoot= getPartialCompilationUnit(cu, offset);
		String string= ASTNodes.asFormattedString(astRoot, 0, String.valueOf('\n'), null);

		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("import java.util.Vector;\n");
		buf.append("import java.io.IOException;\n");
		buf.append("import java.text.ParseException;\n");
		buf.append("public class E {\n");
		buf.append("    private class EInner {\n");
		buf.append("        {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("    private int fField1;\n");
		buf.append("    private int fField2;\n");
		buf.append("    public void foo1() throws IOException, ParseException {\n");
		buf.append("    }\n");
		buf.append("    public int foo2(int i) {\n");
		buf.append("        private class Local {\n");
		buf.append("            private int fField3;\n");
		buf.append("            public int local(int i) {\n");
		buf.append("                return 1;\n");
		buf.append("            }\n");
		buf.append("        }\n");
		buf.append("        Local local = new Local();\n");
		buf.append("        return i;\n");
		buf.append("    }\n");
		buf.append("}");
		String expected= buf.toString();

		assertEqualString(string, expected);
		assertAllBindings(astRoot);
	}

	private CompilationUnit getPartialCompilationUnit(ICompilationUnit cu, int offset) {
		ASTParser p= ASTParser.newParser(AST.JLS3);
		p.setSource(cu);
		p.setFocalPosition(offset);
		p.setResolveBindings(true);
		return (CompilationUnit) p.createAST(null);
	}

	/*
	private static class PartialVisitor extends ASTVisitor {

		private int fOffset;

		public PartialVisitor(int offset) {
			fOffset= offset;
		}

		public boolean visit(Block node) {
			ASTNode parent= node.getParent();
			if (parent instanceof MethodDeclaration || parent instanceof Initializer) {
				int start= node.getStartPosition();
				int end= start + node.getLength();

				if (start <= fOffset && fOffset < end) {
					return true;
				}
				node.statements().clear();
				return false;
			}
			return true;
		}
	}*/


}
