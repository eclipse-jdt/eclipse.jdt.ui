package org.eclipse.jdt.ui.tests.core;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
  */
public class ASTRewritingTest extends TestCase {

	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTest(ASTRewritingMethodDeclTest.suite());
		suite.addTest(ASTRewritingStatementsTest.suite());
		suite.addTest(ASTRewritingTypeDeclTest.suite());
		return suite;
	}

	
	public ASTRewritingTest(String name) {
		super(name);
	}
	public static void assertEqualString(String str1, String str2) {	
		int len1= Math.min(str1.length(), str2.length());
		
		int diffPos= -1;
		for (int i= 0; i < len1; i++) {
			if (str1.charAt(i) != str2.charAt(i)) {
				diffPos= i;
				break;
			}
		}
		if (diffPos != -1) {
			int diffAhead= Math.max(0, diffPos - 3);
			int diffAfter= Math.min(str1.length(), diffPos + 3);
			
			String diffStr= str1.substring(diffAhead, diffPos) + '^' + str1.substring(diffPos, diffAfter);
			
			assertTrue("Content not as expected: is\n" + str1 + "\nDiffers at pos " + diffPos + ": " + diffStr + "\nexpected:\n" + str2, false);
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
	

}
