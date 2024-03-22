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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
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
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class PartialASTTest extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	@Before
	public void setUp() throws Exception {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);

		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(PreferenceConstants.CODEGEN_ADD_COMMENTS, false);

		fJProject1= pts.getProject();

		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, pts.getDefaultClasspath());
	}

	@Test
	public void testPartialCU1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String existing= """
			package test1;
			import java.util.Vector;
			public class E {
			    private int fField1;
			    private int fField2;
			    public void foo1() {
			        fField1 = fField2;
			    }
			    public int foo1(int i) {
			        return i;
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", existing, false, null);

		String statement= "fField1 = fField2;";
		int offset= existing.indexOf(statement);

		CompilationUnit astRoot= getPartialCompilationUnit(cu, offset);
		String string= ASTNodes.asFormattedString(astRoot, 0, String.valueOf('\n'), null);

		String expected= """
			package test1;
			import java.util.Vector;
			public class E {
			    private int fField1;
			    private int fField2;
			    public void foo1() {
			        fField1 = fField2;
			    }
			    public int foo1(int i) {
			    }
			}""";

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
		List<AbstractTypeDeclaration> list= astRoot.types();
		for (AbstractTypeDeclaration element : list) {
			TypeDeclaration decl= (TypeDeclaration) element;
			assertNotNull(decl.resolveBinding());

			if (!decl.isInterface() && decl.getSuperclassType() != null) {
				assertNotNull(decl.getSuperclassType().resolveBinding());
			}
			List<Type> interfaces= decl.superInterfaceTypes();
			for (Type interface1 : interfaces) {
				assertNotNull(interface1.resolveBinding());
			}

			for (MethodDeclaration meth : decl.getMethods()) {
				assertNotNull(meth.resolveBinding());
				List<SingleVariableDeclaration> params= meth.parameters();
				for (SingleVariableDeclaration arg : params) {
					assertNotNull(arg.resolveBinding());
				}
				if (!meth.isConstructor()) {
					assertNotNull(meth.getReturnType2().resolveBinding());
				}
			}
		}


	}


	@Test
	public void testPartialCU2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String existing= """
			package test1;
			import java.util.Vector;
			public class E {
			    private class EInner {
			        public int inner(int i) {
			            return i;
			        }
			    }
			    private int fField1;
			    private int fField2;
			    public void foo1() {
			        fField1 = fField2;
			        if (fField1 == 0) {
			            fField2++;
			        }
			        EInner inner = new EInner();
			    }
			    public int foo1(int i) {
			        private class Local {
			            public int local(int i) {
			                return i;
			            }
			        }
			        Local local = new Local();
			        return i;
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", existing, false, null);

		int offset= existing.indexOf("fField1 = fField2;");

		CompilationUnit astRoot= getPartialCompilationUnit(cu, offset);
		String string= ASTNodes.asFormattedString(astRoot, 0, String.valueOf('\n'), null);

		String expected= """
			package test1;
			import java.util.Vector;
			public class E {
			    private class EInner {
			        public int inner(int i) {
			        }
			    }
			    private int fField1;
			    private int fField2;
			    public void foo1() {
			        fField1 = fField2;
			        if (fField1 == 0) {
			            fField2++;
			        }
			        EInner inner = new EInner();
			    }
			    public int foo1(int i) {
			    }
			}""";

		assertEqualString(string, expected);
		assertAllBindings(astRoot);
	}

	@Test
	public void testPartialCU3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String existing= """
			package test1;
			import java.util.Vector;
			import java.io.Serializable;
			public class E {
			    private class EInner implements Serializable {
			        public int inner(int i) {
			            return i;
			        }
			    }
			    private int fField1;
			    private int fField2;
			    public void foo1() {
			        fField1 = fField2;
			        if (fField1 == 0) {
			            fField2++;
			        }
			        EInner inner = new EInner();
			    }
			    public int foo1(int i) {
			        private class Local {
			            public int local(int i) {
			                return 1;
			            }
			        }
			        Local local = new Local();
			        return i;
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", existing, false, null);

		int offset= existing.indexOf("return 1;");

		CompilationUnit astRoot= getPartialCompilationUnit(cu, offset);
		String string= ASTNodes.asFormattedString(astRoot, 0, String.valueOf('\n'), null);

		String expected= """
			package test1;
			import java.util.Vector;
			import java.io.Serializable;
			public class E {
			    private class EInner implements Serializable {
			        public int inner(int i) {
			        }
			    }
			    private int fField1;
			    private int fField2;
			    public void foo1() {
			    }
			    public int foo1(int i) {
			        private class Local {
			            public int local(int i) {
			                return 1;
			            }
			        }
			        Local local = new Local();
			        return i;
			    }
			}""";

		assertEqualString(string, expected);
		assertAllBindings(astRoot);
	}

	@Test
	public void testPartialCU4() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String existing= """
			package test1;
			import java.util.Vector;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    private class EInner {
			        public int inner(int i) {
			            return 0;
			        }
			    }
			    private int fField1;
			    private int fField2;
			    public void foo1() throws IOException, ParseException {
			        fField1 = fField2;
			        if (fField1 == 0) {
			            fField2++;
			        }
			        EInner inner = new EInner();
			    }
			    public int foo1(int i) {
			        private class Local {
			            public int local(int i) {
			                return 1;
			            }
			        }
			        Local local = new Local();
			        return i;
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", existing, false, null);

		int offset= existing.indexOf("return 0;");

		CompilationUnit astRoot= getPartialCompilationUnit(cu, offset);
		String string= ASTNodes.asFormattedString(astRoot, 0, String.valueOf('\n'), null);

		String expected= """
			package test1;
			import java.util.Vector;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    private class EInner {
			        public int inner(int i) {
			            return 0;
			        }
			    }
			    private int fField1;
			    private int fField2;
			    public void foo1() throws IOException, ParseException {
			    }
			    public int foo1(int i) {
			    }
			}""";

		assertEqualString(string, expected);
		assertAllBindings(astRoot);
	}

	@Test
	public void testPartialCUPositionNotInMethod1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String existing= """
			package test1;
			import java.util.Vector;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    private class EInner {
			        public int inner(int i) {
			            return 0;
			        }
			    }
			    private int fField1;
			    private int fField2;
			    public void foo1() throws IOException, ParseException {
			        fField1 = fField2;
			        if (fField1 == 0) {
			            fField2++;
			        }
			        EInner inner = new EInner();
			    }
			    public int foo2(int i) {
			        private class Local {
			            public int local(int i) {
			                return 1;
			            }
			        }
			        Local local = new Local();
			        return i;
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", existing, false, null);

		int offset= existing.indexOf("private int fField1;");

		CompilationUnit astRoot= getPartialCompilationUnit(cu, offset);
		String string= ASTNodes.asFormattedString(astRoot, 0, String.valueOf('\n'), null);

		String expected= """
			package test1;
			import java.util.Vector;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    private class EInner {
			        public int inner(int i) {
			        }
			    }
			    private int fField1;
			    private int fField2;
			    public void foo1() throws IOException, ParseException {
			    }
			    public int foo2(int i) {
			    }
			}""";

		assertEqualString(string, expected);
		assertAllBindings(astRoot);
	}

	@Test
	public void testPartialCUPositionNotInMethod2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		String existing= """
			package test1;
			import java.util.Vector;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    private class EInner {
			        {
			            System.out.println();
			        }
			    }
			    private int fField1;
			    private int fField2;
			    public void foo1() throws IOException, ParseException {
			        fField1 = fField2;
			        if (fField1 == 0) {
			            fField2++;
			        }
			        EInner inner = new EInner();
			    }
			    public int foo2(int i) {
			        private class Local {
			            private int fField3;
			            public int local(int i) {
			                return 1;
			            }
			        }
			        Local local = new Local();
			        return i;
			    }
			}""";
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", existing, false, null);

		int offset= existing.indexOf("private int fField3;");

		CompilationUnit astRoot= getPartialCompilationUnit(cu, offset);
		String string= ASTNodes.asFormattedString(astRoot, 0, String.valueOf('\n'), null);

		String expected= """
			package test1;
			import java.util.Vector;
			import java.io.IOException;
			import java.text.ParseException;
			public class E {
			    private class EInner {
			        {
			        }
			    }
			    private int fField1;
			    private int fField2;
			    public void foo1() throws IOException, ParseException {
			    }
			    public int foo2(int i) {
			        private class Local {
			            private int fField3;
			            public int local(int i) {
			                return 1;
			            }
			        }
			        Local local = new Local();
			        return i;
			    }
			}""";

		assertEqualString(string, expected);
		assertAllBindings(astRoot);
	}

	private CompilationUnit getPartialCompilationUnit(ICompilationUnit cu, int offset) {
		ASTParser p= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);
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
