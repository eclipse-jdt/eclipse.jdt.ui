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
package org.eclipse.jdt.ui.tests.astrewrite;

import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.dom.ListRewriter;
import org.eclipse.jdt.internal.corext.dom.NewASTRewrite;

public class ASTRewritingJavadocTest extends ASTRewritingTest {
	
	private static final Class THIS= ASTRewritingJavadocTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public ASTRewritingJavadocTest(String name) {
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
			suite.addTest(new ASTRewritingJavadocTest("testVariableDeclarationFragment"));
			return new ProjectTestSetup(suite);
		}
	}
	
	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);			
		
		fJProject1= ProjectTestSetup.getProject();
		
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}
	
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}
	
	public void testParamName() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @param name Hello World.\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		AST ast= astRoot.getAST();
		
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= methodDecl.getJavadoc();
			List tags= javadoc.tags();
			assertTrue("Has one tag", tags.size() == 1);
			
			TagElement tagElement= (TagElement) tags.get(0);
			List fragments= tagElement.fragments();
			assertTrue("Has fragments", !fragments.isEmpty());
			
			SimpleName name= (SimpleName) fragments.get(0);
			rewrite.markAsReplaced(name, ast.newSimpleName("newName"));
			}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @param newName Hello World.\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}

	public void testSeeTag1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @see String A String\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		AST ast= astRoot.getAST();
		
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= methodDecl.getJavadoc();
			List tags= javadoc.tags();
			assertTrue("Has one tag", tags.size() == 1);
			
			TagElement tagElement= (TagElement) tags.get(0);
			List fragments= tagElement.fragments();
			assertTrue("Has fragments", !fragments.isEmpty());
			
			SimpleName name= (SimpleName) fragments.get(0);
			rewrite.markAsReplaced(name, ast.newSimpleName("Vector"));
			}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @see Vector A String\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	public void testSeeTag2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @see #toString A String\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		AST ast= astRoot.getAST();
		
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= methodDecl.getJavadoc();
			List tags= javadoc.tags();
			assertTrue("Has one tag", tags.size() == 1);
			
			TagElement tagElement= (TagElement) tags.get(0);
			List fragments= tagElement.fragments();
			assertTrue("Has fragments", !fragments.isEmpty());
			
			MemberRef ref= (MemberRef) fragments.get(0);
			rewrite.markAsReplaced(ref.getName(), ast.newSimpleName("hashCode"));
			}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @see #hashCode A String\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	
	public void testSeeTag3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @see #toString A String\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		AST ast= astRoot.getAST();
		
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= methodDecl.getJavadoc();
			List tags= javadoc.tags();
			assertTrue("Has one tag", tags.size() == 1);
			
			TagElement tagElement= (TagElement) tags.get(0);
			List fragments= tagElement.fragments();
			assertTrue("Has fragments", !fragments.isEmpty());
			
			MemberRef ref= (MemberRef) fragments.get(0);
			rewrite.markAsInsert(ref, MemberRef.QUALIFIER_PROPERTY, ast.newSimpleName("E"), null);
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @see E#toString A String\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	public void testSeeTagParamInsert1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @see #toString() A String\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		AST ast= astRoot.getAST();
		
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= methodDecl.getJavadoc();
			List tags= javadoc.tags();
			assertTrue("Has one tag", tags.size() == 1);
			
			TagElement tagElement= (TagElement) tags.get(0);
			List fragments= tagElement.fragments();
			assertTrue("Has fragments", !fragments.isEmpty());
			
			MethodRef ref= (MethodRef) fragments.get(0);
			MethodRefParameter param= ast.newMethodRefParameter();
			param.setName(ast.newSimpleName("arg"));
			param.setType(ast.newPrimitiveType(PrimitiveType.INT));
			rewrite.getListRewrite(ref, MethodRef.PARAMETERS_PROPERTY).insertLast(param, null);
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @see #toString(int arg) A String\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}

	public void testSeeTagParamInsert2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * {@link #toString(int x) A String}\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		AST ast= astRoot.getAST();
		
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= methodDecl.getJavadoc();
			List tags= javadoc.tags();
			assertTrue("Has one tag", tags.size() == 1);
			
			TagElement topElement= (TagElement) tags.get(0);
			List fragments= topElement.fragments();
			assertTrue("Has fragments", !fragments.isEmpty());
			
			TagElement tagElement= (TagElement) fragments.get(0);
			fragments= tagElement.fragments();
			assertTrue("Has fragments", !fragments.isEmpty());
			
			MethodRef ref= (MethodRef) fragments.get(0);
			MethodRefParameter param= ast.newMethodRefParameter();
			param.setName(ast.newSimpleName("arg"));
			param.setType(ast.newPrimitiveType(PrimitiveType.INT));
			rewrite.getListRewrite(ref, MethodRef.PARAMETERS_PROPERTY).insertLast(param, null);
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * {@link #toString(int x, int arg) A String}\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	public void testTagInsert1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @param name\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		AST ast= astRoot.getAST();
		
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= methodDecl.getJavadoc();
			List tags= javadoc.tags();
			assertTrue("Has one tag", tags.size() == 1);
			
			TagElement newTag= ast.newTagElement();
			newTag.setTagName("@throws");
			newTag.fragments().add(ast.newSimpleName("Exception"));
			TextElement text= ast.newTextElement();
			text.setText("Thrown for no reason.");
			newTag.fragments().add(text);

			rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY).insertLast(newTag, null);
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @param name\n");
		buf.append("     * @throws Exception Thrown for no reason.\n");		
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	public void testTagInsert2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @param name\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		AST ast= astRoot.getAST();
		
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= methodDecl.getJavadoc();
			List tags= javadoc.tags();
			assertTrue("Has one tag", tags.size() == 1);
			
			TagElement newTag= ast.newTagElement();
			newTag.setTagName("@see");
			MemberRef ref= ast.newMemberRef();
			ref.setQualifier(ast.newSimpleName("Vector"));
			ref.setName(ast.newSimpleName("size"));
			newTag.fragments().add(ref);
			
			rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY).insertFirst(newTag, null);
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");
		buf.append("     * @see Vector#size\n");
		buf.append("     * @param name\n");		
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	public void testTagInsert3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		AST ast= astRoot.getAST();
		
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= methodDecl.getJavadoc();
			List tags= javadoc.tags();
			assertTrue("Has one tag", tags.size() == 0);
			
			TagElement newTag= ast.newTagElement();
			newTag.setTagName(null);
			
			TextElement text= ast.newTextElement();
			text.setText("Comment");
			newTag.fragments().add(text);
			
			TagElement nested= ast.newTagElement();
			nested.setTagName("@link");
			
			newTag.fragments().add(nested);
			
			MethodRef ref= ast.newMethodRef();
			ref.setQualifier(ast.newSimpleName("Vector"));
			ref.setName(ast.newSimpleName("size"));
			nested.fragments().add(ref);
			
			TextElement textNested= ast.newTextElement();
			textNested.setText("Link");
			nested.fragments().add(textNested);

			rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY).insertFirst(newTag, null);
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");
		buf.append("     * Comment {@link Vector#size() Link}\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testTagRemove1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @param name\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
		
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= methodDecl.getJavadoc();
			List tags= javadoc.tags();
			assertTrue("Has one tag", tags.size() == 1);
			
			rewrite.markAsRemoved((ASTNode) tags.get(0));
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}

	public void testTagRemove2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @param name1 The first name.\n");
		buf.append("     * @param name2 The second name.\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
	
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= methodDecl.getJavadoc();
			List tags= javadoc.tags();
			assertTrue("Has one tag", tags.size() == 2);
			
			rewrite.markAsRemoved((ASTNode) tags.get(0));
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @param name2 The second name.\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	public void testTagRemove3() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @param name1 The first name.\n");
		buf.append("     * @param name2 The second name.\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);

		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
	
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= methodDecl.getJavadoc();
			List tags= javadoc.tags();
			assertTrue("Has one tag", tags.size() == 2);
			
			rewrite.markAsRemoved((ASTNode) tags.get(1));
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @param name1 The first name.\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());
	}
	
	public void testTagRemoveInsert() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @param name1 The first name.\n");
		buf.append("     * @param name2 The second name.\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);

		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
	
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= methodDecl.getJavadoc();
			List tags= javadoc.tags();
			assertTrue("Has one tag", tags.size() == 2);
			
			ListRewriter listRewrite= rewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);
			listRewrite.remove((ASTNode) tags.get(1), null);
			
			AST ast= astRoot.getAST();
			TagElement element= ast.newTagElement();
			element.setTagName("@since");
			
			TextElement textElement= ast.newTextElement();
			textElement.setText("1.1");
			element.fragments().add(textElement);
			
			listRewrite.insertLast(element, null);
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @param name1 The first name.\n");
		buf.append("     * @since 1.1\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	public void testAddJavadoc() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		AST ast= astRoot.getAST();
		
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
	
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			MethodDeclaration methodDecl= findMethodDeclaration(type, "gee");
			
			Javadoc javadoc= ast.newJavadoc();
			TagElement element= ast.newTagElement();
			element.setTagName("@since");
			
			TextElement textElement= ast.newTextElement();
			textElement.setText("1.1");
			element.fragments().add(textElement);
			javadoc.tags().add(element);
			
			rewrite.markAsInsert(methodDecl, MethodDeclaration.JAVADOC_PROPERTY, javadoc, null);
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @since 1.1\n");
		buf.append("     */\n");
		buf.append("    public void gee(String name) {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}

	public void testAddJavadoc2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    public int count;\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		AST ast= astRoot.getAST();
		
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
	
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			FieldDeclaration fieldDecl= type.getFields()[0];
			
			Javadoc javadoc= ast.newJavadoc();
			TagElement element= ast.newTagElement();
			element.setTagName("@since");
			
			TextElement textElement= ast.newTextElement();
			textElement.setText("1.1");
			element.fragments().add(textElement);
			javadoc.tags().add(element);
			
			rewrite.markAsInsert(fieldDecl, FieldDeclaration.JAVADOC_PROPERTY, javadoc, null);
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @since 1.1\n");
		buf.append("     */\n");
		buf.append("    public int count;\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}
	
	public void testRemoveJavadoc() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    /**\n");			
		buf.append("     * @since 1.1\n");
		buf.append("     */\n");
		buf.append("    static {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
	
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			Initializer initializer= (Initializer) type.bodyDeclarations().get(0);
			rewrite.markAsRemoved(initializer.getJavadoc());
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("\n");
		buf.append("    static {\n");
		buf.append("    }\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}

	public void testRemoveJavadoc2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("/**\n");			
		buf.append(" * @since 1.1\n");
		buf.append(" */\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);			

		CompilationUnit astRoot= createAST(cu);
		
		NewASTRewrite rewrite= new NewASTRewrite(astRoot.getAST());
	
		{  // insert method at first position
			TypeDeclaration type= findTypeDeclaration(astRoot, "E");
			
			rewrite.markAsRemoved(type.getJavadoc());
		}

		String preview= evaluateRewrite(cu, rewrite);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("}\n");
		assertEqualString(preview, buf.toString());

	}


	
}
