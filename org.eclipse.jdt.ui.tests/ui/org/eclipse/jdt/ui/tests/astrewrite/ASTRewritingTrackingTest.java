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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

import org.eclipse.jface.text.IRegion;

import org.eclipse.jdt.internal.corext.dom.ASTNodeConstants;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;

import org.eclipse.jdt.internal.ui.text.correction.ASTRewriteCorrectionProposal;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

public class ASTRewritingTrackingTest extends ASTRewritingTest {

	private static final Class THIS= ASTRewritingTrackingTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	
	

	public ASTRewritingTrackingTest(String name) {
		super(name);
	}
	
	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test suite() {
		if (false) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new ASTRewritingTrackingTest("testNamesWithPlaceholder"));
			return new ProjectTestSetup(suite);
		}
	}
	
	protected void setUp() throws Exception {
		Hashtable options= JavaCore.getDefaultOptions();
		options.put(JavaCore.FORMATTER_TAB_CHAR, JavaCore.SPACE);
		options.put(JavaCore.FORMATTER_TAB_SIZE, "4");
		JavaCore.setOptions(options);			
		
		fJProject1= ProjectTestSetup.getProject();
		
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}
	
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}
	
	private GroupDescription getDescription(List all, String name) {
		GroupDescription desc= new GroupDescription(name);
		all.add(desc);
		return desc;
	}
	
	
	public void testNamesWithDelete() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            i--;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");		
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		ArrayList gd= new ArrayList();
		
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), getDescription(gd, "C"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), getDescription(gd, "foo"));
		
		FieldDeclaration field= (FieldDeclaration) decls.get(0);
		rewrite.markAsRemoved(field);
						
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		proposal.getCompilationUnitChange().setKeepExecutedTextEdits(true);
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            i--;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(cu.getSource(), expected);
		
		CompilationUnitChange change= proposal.getCompilationUnitChange();

		
		GroupDescription[] descriptions= (GroupDescription[]) gd.toArray(new GroupDescription[gd.size()]);
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			IRegion range= change.getNewTextRange(descriptions[i].getTextEdits());
			String string= expected.substring(range.getOffset(), range.getOffset() + range.getLength());
			assertEqualString(string, name);
		}
		clearRewrite(rewrite);
	}
	
	public void testNamesWithInsert() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            i--;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		ArrayList gd= new ArrayList();
		
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), getDescription(gd, "C"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), getDescription(gd, "foo"));
		
		FieldDeclaration field= (FieldDeclaration) decls.get(0);
		List fragments= field.fragments();
		VariableDeclarationFragment frag1= (VariableDeclarationFragment) fragments.get(0);
		rewrite.markAsTracked(frag1.getName(),  getDescription(gd, "x1"));
		
		VariableDeclarationFragment newFrag= ast.newVariableDeclarationFragment();
		newFrag.setName(ast.newSimpleName("newVariable"));
		newFrag.setExtraDimensions(2);
		rewrite.markAsInserted(newFrag);
		fragments.add(0, newFrag);
						
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		proposal.getCompilationUnitChange().setKeepExecutedTextEdits(true);
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public int newVariable[][], x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            i--;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(cu.getSource(), expected);
		
		CompilationUnitChange change= proposal.getCompilationUnitChange();
		
		GroupDescription[] descriptions= (GroupDescription[]) gd.toArray(new GroupDescription[gd.size()]);
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			IRegion range= change.getNewTextRange(descriptions[i].getTextEdits());
			String string= expected.substring(range.getOffset(), range.getOffset() + range.getLength());
			assertEqualString(string, name);
		}
		clearRewrite(rewrite);
	}
	
	public void testNamesWithReplace() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            ++i;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		ArrayList gd= new ArrayList();
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		SimpleName newName= ast.newSimpleName("XX");
		rewrite.markAsReplaced(typeC.getName(), newName);
		rewrite.markAsTracked(newName,  getDescription(gd, "XX"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), getDescription(gd, "foo"));
		
		WhileStatement whileStatement= (WhileStatement) method.getBody().statements().get(0);
		PrefixExpression prefixExpression= (PrefixExpression) ((ExpressionStatement) ((Block) whileStatement.getBody()).statements().get(0)).getExpression();
		rewrite.markAsTracked(prefixExpression.getOperand(),  getDescription(gd, "i"));
				
		FieldDeclaration field= (FieldDeclaration) decls.get(0);
		List fragments= field.fragments();
		VariableDeclarationFragment frag1= (VariableDeclarationFragment) fragments.get(0);
		rewrite.markAsTracked(frag1.getName(),  getDescription(gd, "x1"));
		
		// change modifier
		int newModifiers= Modifier.STATIC | Modifier.TRANSIENT | Modifier.PRIVATE;
		rewrite.markAsReplaced(field, ASTNodeConstants.MODIFIERS, new Integer(newModifiers), null);
								
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		proposal.getCompilationUnitChange().setKeepExecutedTextEdits(true);
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class XX {\n");
		buf.append("\n");
		buf.append("    private static transient int x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            ++i;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(cu.getSource(), expected);
		
		CompilationUnitChange change= proposal.getCompilationUnitChange();
		
		GroupDescription[] descriptions= (GroupDescription[]) gd.toArray(new GroupDescription[gd.size()]);
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			IRegion range= change.getNewTextRange(descriptions[i].getTextEdits());
			String string= expected.substring(range.getOffset(), range.getOffset() + range.getLength());
			assertEqualString(string, name);
		}
		clearRewrite(rewrite);
	}
	
	public void testNamesWithMove1() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            ++i;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		ArrayList gd= new ArrayList();
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), getDescription(gd, "C"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), getDescription(gd, "foo"));
		
		WhileStatement whileStatement= (WhileStatement) method.getBody().statements().get(0);
		PrefixExpression prefixExpression= (PrefixExpression) ((ExpressionStatement) ((Block) whileStatement.getBody()).statements().get(0)).getExpression();
		rewrite.markAsTracked(prefixExpression.getOperand(),  getDescription(gd, "i"));
					
		FieldDeclaration field= (FieldDeclaration) decls.get(0);
		List fragments= field.fragments();
		VariableDeclarationFragment frag1= (VariableDeclarationFragment) fragments.get(0);
		rewrite.markAsTracked(frag1.getName(),  getDescription(gd, "x1"));

		// move method before field
		ASTNode placeHolder= rewrite.createMove(method);
		rewrite.markAsInserted(placeHolder);
		decls.add(0, placeHolder);				
								
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		proposal.getCompilationUnitChange().setKeepExecutedTextEdits(true);
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            ++i;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(cu.getSource(), expected);
		
		CompilationUnitChange change= proposal.getCompilationUnitChange();
		
		GroupDescription[] descriptions= (GroupDescription[]) gd.toArray(new GroupDescription[gd.size()]);
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			IRegion range= change.getNewTextRange(descriptions[i].getTextEdits());
			String string= expected.substring(range.getOffset(), range.getOffset() + range.getLength());
			assertEqualString(string, name);
		}
		clearRewrite(rewrite);
	}
	
	public void testNamesWithMove2() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        while (i == 0) {\n");
		buf.append("            ++i;\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		ArrayList gd= new ArrayList();
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), getDescription(gd, "C"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(0);
		rewrite.markAsTracked(method.getName(), getDescription(gd, "foo"));
		
		WhileStatement whileStatement= (WhileStatement) method.getBody().statements().get(0);
		PrefixExpression prefixExpression= (PrefixExpression) ((ExpressionStatement) ((Block) whileStatement.getBody()).statements().get(0)).getExpression();
		rewrite.markAsTracked(prefixExpression.getOperand(),  getDescription(gd, "i"));
					

		// move method before field
		ASTNode placeHolder= rewrite.createMove(whileStatement);
		
		TryStatement tryStatement= ast.newTryStatement();
		tryStatement.getBody().statements().add(placeHolder);
		tryStatement.setFinally(ast.newBlock());
		rewrite.markAsReplaced(whileStatement, tryStatement);
								
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		proposal.getCompilationUnitChange().setKeepExecutedTextEdits(true);
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("        try {\n");
		buf.append("            while (i == 0) {\n");
		buf.append("                ++i;\n");
		buf.append("            }\n");
		buf.append("        } finally {\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(cu.getSource(), expected);
		
		CompilationUnitChange change= proposal.getCompilationUnitChange();
		
		GroupDescription[] descriptions= (GroupDescription[]) gd.toArray(new GroupDescription[gd.size()]);
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			IRegion range= change.getNewTextRange(descriptions[i].getTextEdits());
			String string= expected.substring(range.getOffset(), range.getOffset() + range.getLength());
			assertEqualString(string, name);
		}
		clearRewrite(rewrite);
	}	
	
	public void testNamesWithMove3() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		ArrayList gd= new ArrayList();
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), getDescription(gd, "C"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(1);
		rewrite.markAsTracked(method.getName(), getDescription(gd, "foo"));
		
		// move method before field
		ASTNode placeHolder= rewrite.createMove(method);
		rewrite.markAsInserted(placeHolder);
		decls.add(0, placeHolder);				
								
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		proposal.getCompilationUnitChange().setKeepExecutedTextEdits(true);
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("\n");
		buf.append("    public void foo(String s, int i) {\n");
		buf.append("    }\n");
		buf.append("\n");
		buf.append("    public int x1;\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(cu.getSource(), expected);
		
		CompilationUnitChange change= proposal.getCompilationUnitChange();
		
		GroupDescription[] descriptions= (GroupDescription[]) gd.toArray(new GroupDescription[gd.size()]);
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			IRegion range= change.getNewTextRange(descriptions[i].getTextEdits());
			String string= expected.substring(range.getOffset(), range.getOffset() + range.getLength());
			assertEqualString(string, name);
		}
		clearRewrite(rewrite);
	}
	public void testNamesWithPlaceholder() throws Exception {
		
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    public String foo(Object s) {\n");
		buf.append("        return s;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		ICompilationUnit cu= pack1.createCompilationUnit("C.java", buf.toString(), false, null);
	
		CompilationUnit astRoot= AST.parseCompilationUnit(cu, false);
		AST ast= astRoot.getAST();
		ASTRewrite rewrite= new ASTRewrite(astRoot);
		
		ArrayList gd= new ArrayList();
		
		// change type name
		TypeDeclaration typeC= findTypeDeclaration(astRoot, "C");
		rewrite.markAsTracked(typeC.getName(), getDescription(gd, "C"));
		
		List decls= typeC.bodyDeclarations();
		
		MethodDeclaration method= (MethodDeclaration) decls.get(0);
		rewrite.markAsTracked(method.getName(), getDescription(gd, "foo"));
		
		ReturnStatement returnStatement= (ReturnStatement) method.getBody().statements().get(0);
		
		CastExpression castExpression= ast.newCastExpression();
		Type type= (Type) rewrite.createPlaceholder("String", ASTRewrite.TYPE);
		Expression expression= (Expression) rewrite.createMove(returnStatement.getExpression());
		castExpression.setType(type);
		castExpression.setExpression(expression);
		
		rewrite.markAsReplaced(returnStatement.getExpression(), castExpression);
		
		rewrite.markAsTracked(type, getDescription(gd, "String"));
		rewrite.markAsTracked(expression, getDescription(gd, "s"));
		
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal("", cu, rewrite, 10, null);
		proposal.getCompilationUnitChange().setSave(true);
		proposal.getCompilationUnitChange().setKeepExecutedTextEdits(true);
		proposal.apply(null);
		
		buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class C {\n");
		buf.append("    public String foo(Object s) {\n");
		buf.append("        return (String) s;\n");
		buf.append("    }\n");
		buf.append("}\n");	
		String expected= buf.toString();
		assertEqualString(cu.getSource(), expected);
		
		CompilationUnitChange change= proposal.getCompilationUnitChange();
		
		GroupDescription[] descriptions= (GroupDescription[]) gd.toArray(new GroupDescription[gd.size()]);
		for (int i= 0; i < descriptions.length; i++) {
			String name= descriptions[i].getName();
			IRegion range= change.getNewTextRange(descriptions[i].getTextEdits());
			String string= expected.substring(range.getOffset(), range.getOffset() + range.getLength());
			assertEqualString(string, name);
		}
		clearRewrite(rewrite);
	}	

	
}



