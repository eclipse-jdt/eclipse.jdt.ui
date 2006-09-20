package org.eclipse.jdt.ui.examples;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.Document;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.launching.JavaRuntime;

public class ASTRewriteSnippet extends TestCase {

	public void testASTRewriteExample() throws Exception {
		IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject("Test");
		project.create(null);
		project.open(null);
		try {
			IProjectDescription description = project.getDescription();
			description.setNatureIds(new String[] { JavaCore.NATURE_ID } );
			project.setDescription(description, null);
			
			IJavaProject javaProject= JavaCore.create(project);
			
			IClasspathEntry[] cpentry= new IClasspathEntry[] {
					JavaCore.newSourceEntry(javaProject.getPath()),
					JavaRuntime.getDefaultJREContainerEntry()
			};
			javaProject.setRawClasspath(cpentry, javaProject.getPath(), null);
			Map options= new HashMap();
			options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
			options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
			javaProject.setOptions(options);
			
			IPackageFragmentRoot root= javaProject.getPackageFragmentRoot(project);
			IPackageFragment pack1= root.createPackageFragment("test1", false, null);
			StringBuffer buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E {\n");
			buf.append("    public void foo() {\n");
			buf.append("        while (i == j) {\n");
			buf.append("            System.beep();\n");
			buf.append("        }\n");		
			buf.append("    }\n");
			buf.append("}\n");	
			ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
			
			ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setSource(cu);
			parser.setResolveBindings(false);
			CompilationUnit astRoot= (CompilationUnit) parser.createAST(null);
			AST ast= astRoot.getAST();
			
			ASTRewrite rewrite= ASTRewrite.create(ast);
	
			TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);
			MethodDeclaration methodDecl= typeDecl.getMethods()[0];
			Block block= methodDecl.getBody();
			
			MethodInvocation newInv1= ast.newMethodInvocation();
			newInv1.setName(ast.newSimpleName("bar1"));
			Statement newStatement1= ast.newExpressionStatement(newInv1);
			
			MethodInvocation newInv2= ast.newMethodInvocation();
			newInv2.setName(ast.newSimpleName("bar2"));
			Statement newStatement2= ast.newExpressionStatement(newInv2);
			
			ListRewrite listRewrite= rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
			listRewrite.insertFirst(newStatement1, null);
			listRewrite.insertLast(newStatement2, null);
			
			TextEdit res= rewrite.rewriteAST();
			
			Document document= new Document(cu.getSource());
			res.apply(document);
			cu.getBuffer().setContents(document.get());
	
			String preview= cu.getSource();
			
			buf= new StringBuffer();
			buf.append("package test1;\n");
			buf.append("public class E {\n");
			buf.append("    public void foo() {\n");
			buf.append("        bar1();\n");
			buf.append("        while (i == j) {\n");
			buf.append("            System.beep();\n");
			buf.append("        }\n");
			buf.append("        bar2();\n");
			buf.append("    }\n");
			buf.append("}\n");	
			assertEquals(preview, buf.toString());
		} finally {
			project.delete(true, null);
		}
	}
}
