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

import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.NewASTRewrite;

/**
  */
public class ASTRewritingTest extends TestCase {

	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(ASTRewritingExpressionsTest.allTests());
		suite.addTest(ASTRewritingInsertBoundTest.allTests());
		suite.addTest(ASTRewritingMethodDeclTest.allTests());
		suite.addTest(ASTRewritingMoveCodeTest.allTests());
		suite.addTest(ASTRewritingStatementsTest.allTests());
		suite.addTest(ASTRewritingTrackingTest.allTests());
		suite.addTest(ASTRewritingTypeDeclTest.allTests());
		suite.addTest(SourceModifierTest.allTests());
		suite.addTest(ASTRewritingCollapseTest.allTests());
		
		return new ProjectTestSetup(suite);
	}

	
	public ASTRewritingTest(String name) {
		super(name);
	}
	
	/**
	 * Returns the result of a rewrite.
	 */
	protected String evaluateRewrite(ICompilationUnit cu, NewASTRewrite rewrite) throws CoreException, MalformedTreeException, BadLocationException {
		Document document= new Document(cu.getSource());
		TextEdit res= rewrite.rewriteAST(document);
		
		res.apply(document);
		return document.get();
	}
	
	private static final int printRange= 6;
	
	public static void assertEqualString(String expected, String actual) {	
		int len1= Math.min(expected.length(), actual.length());
		
		int diffPos= -1;
		for (int i= 0; i < len1; i++) {
			if (expected.charAt(i) != actual.charAt(i)) {
				diffPos= i;
				break;
			}
		}
		if (diffPos == -1 && expected.length() != actual.length()) {
			diffPos= len1;
		}
		if (diffPos != -1) {
			int diffAhead= Math.max(0, diffPos - printRange);
			int diffAfter= Math.min(expected.length(), diffPos + printRange);
			
			String diffStr= expected.substring(diffAhead, diffPos) + '^' + expected.substring(diffPos, diffAfter);
			
			// use detailed message
			String message= "Content not as expected: is\n" + expected + "\nDiffers at pos " + diffPos + ": " + diffStr + "\nexpected:\n" + actual;  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			
			assertEquals(message, expected, actual);
		}
	}
	
	public static TypeDeclaration findTypeDeclaration(CompilationUnit astRoot, String simpleTypeName) {
		List types= astRoot.types();
		for (int i= 0; i < types.size(); i++) {
			TypeDeclaration elem= (TypeDeclaration) types.get(i);
			if (simpleTypeName.equals(elem.getName().getIdentifier())) {
				return elem;
			}
		}
		return null;
	}
	
	public static MethodDeclaration findMethodDeclaration(TypeDeclaration typeDecl, String methodName) {
		MethodDeclaration[] methods= typeDecl.getMethods();
		for (int i= 0; i < methods.length; i++) {
			if (methodName.equals(methods[i].getName().getIdentifier())) {
				return methods[i];
			}
		}
		return null;
	}
	
	public static SingleVariableDeclaration createNewParam(AST ast, String name) {
		SingleVariableDeclaration newParam= ast.newSingleVariableDeclaration();
		newParam.setType(ast.newPrimitiveType(PrimitiveType.FLOAT));
		newParam.setName(ast.newSimpleName(name));
		return newParam;
	}
	
	protected FieldDeclaration createNewField(AST ast, String name) {
		VariableDeclarationFragment frag= ast.newVariableDeclarationFragment();
		frag.setName(ast.newSimpleName(name));
		FieldDeclaration newFieldDecl= ast.newFieldDeclaration(frag);
		newFieldDecl.setModifiers(Modifier.PRIVATE);
		newFieldDecl.setType(ast.newPrimitiveType(PrimitiveType.DOUBLE));
		return newFieldDecl;
	}
	
	protected MethodDeclaration createNewMethod(AST ast, String name, boolean isAbstract) {
		MethodDeclaration decl= ast.newMethodDeclaration();
		decl.setName(ast.newSimpleName(name));
		decl.setReturnType(ast.newPrimitiveType(PrimitiveType.VOID));
		decl.setModifiers(isAbstract ? (Modifier.ABSTRACT | Modifier.PRIVATE) : Modifier.PRIVATE);
		SingleVariableDeclaration param= ast.newSingleVariableDeclaration();
		param.setName(ast.newSimpleName("str"));
		param.setType(ast.newSimpleType(ast.newSimpleName("String")));
		decl.parameters().add(param);
		decl.setBody(isAbstract ? null : ast.newBlock());
		return decl;
	}
	
	
	protected void clearRewrite(ASTRewrite rewrite) {
		rewrite.removeModifications();
	}
	

}
