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

package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.dom.ASTNodeConstants;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;

public class NewVariableCompletionProposal extends LinkedCorrectionProposal {

	public static final int LOCAL= 1;
	public static final int FIELD= 2;
	public static final int PARAM= 3;
	
	public static final int CONST_FIELD= 4;

	private static final String KEY_NAME= "name"; //$NON-NLS-1$
	private static final String KEY_TYPE= "type"; //$NON-NLS-1$
	private static final String KEY_INITIALIZER= "initializer"; //$NON-NLS-1$

	private int  fVariableKind;
	private SimpleName fOriginalNode;
	private ITypeBinding fSenderBinding;
	
	public NewVariableCompletionProposal(String label, ICompilationUnit cu, int variableKind, SimpleName node, ITypeBinding senderBinding, int relevance, Image image) {
		super(label, cu, null, relevance, image);
	
		fVariableKind= variableKind;
		fOriginalNode= node;
		fSenderBinding= senderBinding;
	}
		
	protected ASTRewrite getRewrite() throws CoreException {

		CompilationUnit cu= ASTResolving.findParentCompilationUnit(fOriginalNode);
		if (fVariableKind == PARAM) {
			return doAddParam(cu);
		} else if (fVariableKind == FIELD || fVariableKind ==CONST_FIELD) {
			return doAddField(cu);
		} else { // LOCAL
			return doAddLocal(cu);
		}
	}

	private ASTRewrite doAddParam(CompilationUnit cu) throws CoreException {
		AST ast= cu.getAST();
		SimpleName node= fOriginalNode;

		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(node);
		if (decl instanceof MethodDeclaration) {
			ASTRewrite rewrite= new ASTRewrite(decl);
			
			SingleVariableDeclaration newDecl= ast.newSingleVariableDeclaration();
			newDecl.setType(evaluateVariableType(ast));
			newDecl.setName(ast.newSimpleName(node.getIdentifier()));
			
			rewrite.markAsInsertBeforeOriginal(decl, ASTNodeConstants.PARAMETERS, newDecl, null, null);
			
			markAsLinked(rewrite, newDecl.getType(), false, KEY_TYPE);
			markAsLinked(rewrite, node, true, KEY_NAME);
			markAsLinked(rewrite, newDecl.getName(), false, KEY_NAME);
			
			return rewrite;
		}
		return null;
	}
	
	private boolean isAssigned(Statement statement, SimpleName name) {
		if (statement instanceof ExpressionStatement) {
			ExpressionStatement exstat= (ExpressionStatement) statement;
			if (exstat.getExpression() instanceof Assignment) {
				Assignment assignment= (Assignment) exstat.getExpression();
				return assignment.getLeftHandSide() == name;
			}
		}
		return false;
	}
	
	private boolean isForStatementInit(Statement statement, SimpleName name) {
		if (statement instanceof ForStatement) {
			ForStatement forStatement= (ForStatement) statement;
			List list = forStatement.initializers();
			if (list.size() == 1 && list.get(0) instanceof Assignment) {
				Assignment assignment= (Assignment) list.get(0);
				return assignment.getLeftHandSide() == name;
			}
		}		
		return false;
	}
	

	private ASTRewrite doAddLocal(CompilationUnit cu) throws CoreException {
		AST ast= cu.getAST();
		
		Block body;
		BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(fOriginalNode);
		if (decl instanceof MethodDeclaration) {
			body= (((MethodDeclaration) decl).getBody());
		} else if (decl instanceof Initializer) {
			body= (((Initializer) decl).getBody());
		} else {
			return null;
		}
		ASTRewrite rewrite= new ASTRewrite(decl);
		
		SimpleName[] names= getAllReferences(cu, body);
		ASTNode dominant= getDominantNode(names);
		
		Statement dominantStatement= ASTResolving.findParentStatement(dominant);
		SimpleName node= names[0];
		
		if (isAssigned(dominantStatement, node)) {
			// x = 1; -> int x = 1;
			Assignment assignment= (Assignment) node.getParent();
			
			VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
			VariableDeclarationStatement newDecl= ast.newVariableDeclarationStatement(newDeclFrag);
			newDecl.setType(evaluateVariableType(ast));
			
			Expression placeholder= (Expression) rewrite.createCopy(assignment.getRightHandSide());
			newDeclFrag.setInitializer(placeholder);
			newDeclFrag.setName(ast.newSimpleName(node.getIdentifier()));
			rewrite.markAsReplaced(dominantStatement, newDecl);
			
			markAsLinked(rewrite, newDecl.getType(), false, KEY_TYPE);
			markAsLinked(rewrite, newDeclFrag.getName(), true, KEY_NAME);
			
			markAsSelection(rewrite, newDecl); // end position

			return rewrite;
		} else if (isForStatementInit(dominantStatement, node)) {
			//	for (x = 1;;) ->for (int x = 1;;)
			
			Assignment assignment= (Assignment) node.getParent();
			
			VariableDeclarationFragment frag= ast.newVariableDeclarationFragment();
			VariableDeclarationExpression expression= ast.newVariableDeclarationExpression(frag);
			frag.setName(ast.newSimpleName(node.getIdentifier()));
			Expression placeholder= (Expression) rewrite.createCopy(assignment.getRightHandSide());
			frag.setInitializer(placeholder);
			expression.setType(evaluateVariableType(ast));
			
			rewrite.markAsReplaced(assignment, expression);
			
			markAsLinked(rewrite, expression.getType(), false, KEY_TYPE); 
			markAsLinked(rewrite, frag.getName(), true, KEY_NAME); 
			
			markAsSelection(rewrite, expression); // end position
			
			return rewrite;
		}
		//	foo(x) -> int x= 0; foo(x)
		
		VariableDeclarationFragment newDeclFrag= ast.newVariableDeclarationFragment();
		VariableDeclarationStatement newDecl= ast.newVariableDeclarationStatement(newDeclFrag);

		newDeclFrag.setName(ast.newSimpleName(node.getIdentifier()));
		newDecl.setType(evaluateVariableType(ast));
		newDeclFrag.setInitializer(ASTNodeFactory.newDefaultExpression(ast, newDecl.getType(), 0));

		markAsLinked(rewrite, newDecl.getType(), false, KEY_TYPE);
		markAsLinked(rewrite, node, true, KEY_NAME); 
		markAsLinked(rewrite, newDeclFrag.getName(), false, KEY_NAME);
		
		Statement statement= dominantStatement;
		List list= ASTNodes.getContainingList(statement);
		while (list == null && statement.getParent() instanceof Statement) { // parent must be if, for or while
			statement= (Statement) statement.getParent();
			list= ASTNodes.getContainingList(statement);
		}
		if (list != null) {
			list.add(list.indexOf(statement), newDecl);
			rewrite.markAsInserted(newDecl);
			return rewrite;					
		}
		return rewrite;
	}
	
	private SimpleName[] getAllReferences(CompilationUnit cu, Block body) {
		IProblem[] problems= cu.getProblems();
		
		ArrayList res= new ArrayList();
		
		int bodyStart= body.getStartPosition();
		int bodyEnd= bodyStart + body.getLength();
		
		String name= fOriginalNode.getIdentifier();

		for (int i= 0; i < problems.length; i++) {
			IProblem curr= problems[i];
			int probStart= curr.getSourceStart();
			int probEnd= curr.getSourceEnd() + 1;
			
			if (curr.getID() == IProblem.UndefinedName &&  probStart > bodyStart && probEnd < bodyEnd) {
				if (name.equals(curr.getArguments()[0])) {
					ASTNode node= NodeFinder.perform(body, probStart, probEnd - probStart);
					if (node instanceof SimpleName) {
						res.add(node);
					}
				}
			}
		}
		if (res.isEmpty()) {
			res.add(fOriginalNode); // bug 48617 should fix that
		}
		
		
		SimpleName[] names= (SimpleName[]) res.toArray(new SimpleName[res.size()]);
		if (res.size() > 0) {
			Arrays.sort(names, new Comparator() {
				public int compare(Object o1, Object o2) {
					return ((SimpleName) o1).getStartPosition() - ((SimpleName) o2).getStartPosition();
				}
	
				public boolean equals(Object obj) {
					return false;
				}
			});
		}
		return names;
	}
	
	
	private ASTNode getDominantNode(SimpleName[] names) {
		ASTNode dominator= names[0]; //ASTResolving.findParentStatement(names[0]);
		for (int i= 1; i < names.length; i++) {
			ASTNode curr= names[i];// ASTResolving.findParentStatement(names[i]);
			if (curr != dominator) {
				ASTNode parent= getCommonParent(curr, dominator);
				
				if (curr.getStartPosition() < dominator.getStartPosition()) {
					dominator= curr;
				}
				while (dominator.getParent() != parent) {
					dominator= dominator.getParent();
				}
			}
		}
		int parentKind= dominator.getParent().getNodeType();
		if (parentKind != ASTNode.BLOCK && parentKind != ASTNode.FOR_STATEMENT) {
			return dominator.getParent();
		}
		return dominator;
	}
	
	private ASTNode getCommonParent(ASTNode node1, ASTNode node2) {
		ASTNode parent= node1.getParent();
		while (parent != null && !ASTNodes.isParent(node2, parent)) {
			parent= parent.getParent();
		}
		return parent;
	}	

	private ASTRewrite doAddField(CompilationUnit astRoot) throws CoreException {
		SimpleName node= fOriginalNode;
		boolean isInDifferentCU= false;
		
		ASTNode newTypeDecl= astRoot.findDeclaringNode(fSenderBinding);
		if (newTypeDecl == null) {
			astRoot= AST.parseCompilationUnit(getCompilationUnit(), true);
			newTypeDecl= astRoot.findDeclaringNode(fSenderBinding.getKey());
			isInDifferentCU= true;
		}
		
		if (newTypeDecl != null) {
			ASTRewrite rewrite= new ASTRewrite(astRoot);
			
			AST ast= newTypeDecl.getAST();
			VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
			fragment.setName(ast.newSimpleName(node.getIdentifier()));
			
			Type type= evaluateVariableType(ast);
			
			FieldDeclaration newDecl= ast.newFieldDeclaration(fragment);
			newDecl.setType(type);
						
			newDecl.setModifiers(evaluateFieldModifiers(newTypeDecl));
			if (fSenderBinding.isInterface() || fVariableKind == CONST_FIELD) {
				fragment.setInitializer(ASTNodeFactory.newDefaultExpression(ast, type, 0));
			}
		
			boolean isAnonymous= newTypeDecl.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION;
			List decls= isAnonymous ?  ((AnonymousClassDeclaration) newTypeDecl).bodyDeclarations() :  ((TypeDeclaration) newTypeDecl).bodyDeclarations();
							
			int insertIndex= findFieldInsertIndex(decls, node.getStartPosition());
			rewrite.markAsInsertInOriginal(newTypeDecl, ASTNodeConstants.BODY_DECLARATIONS, newDecl, insertIndex, null);
			
			markAsLinked(rewrite, newDecl.getType(), false, KEY_TYPE);
			if (!isInDifferentCU) {
				markAsLinked(rewrite, node, true, KEY_NAME);
			}
			markAsLinked(rewrite, fragment.getName(), false, KEY_NAME);
			
			if (fragment.getInitializer() != null) {
				markAsLinked(rewrite, fragment.getInitializer(), false, KEY_INITIALIZER);
			}

			return rewrite;
		}
		return null;
	}
	
	private int findFieldInsertIndex(List decls, int currPos) {
		for (int i= decls.size() - 1; i >= 0; i--) {
			ASTNode curr= (ASTNode) decls.get(i);
			if (curr instanceof FieldDeclaration) {
				if (currPos > curr.getStartPosition() + curr.getLength()) {
					return i + 1;
				}
			}
		}
		return 0;
	}
		
	private Type evaluateVariableType(AST ast) throws CoreException {
		ITypeBinding binding= ASTResolving.guessBindingForReference(fOriginalNode);
		if (binding != null) {
			if (isVariableAssigned()) {
				ITypeBinding[] typeProposals= ASTResolving.getRelaxingTypes(ast, binding);
				for (int i= 0; i < typeProposals.length; i++) {
					addLinkedModeProposal(KEY_TYPE, typeProposals[i]);
				}
			}
			String typeName= addImport(binding);
			return ASTNodeFactory.newType(ast, typeName);			
		}
		Type type= ASTResolving.guessTypeForReference(ast, fOriginalNode);
		if (type != null) {
			return type;
		}
		if (fVariableKind == CONST_FIELD) {
			return ast.newSimpleType(ast.newSimpleName("String")); //$NON-NLS-1$
		}
		return ast.newSimpleType(ast.newSimpleName("Object")); //$NON-NLS-1$
	}
	
	private boolean isVariableAssigned() {
		ASTNode parent= fOriginalNode.getParent();
		return (parent instanceof Assignment) && (fOriginalNode == ((Assignment) parent).getLeftHandSide());
	}
	
	
	private int evaluateFieldModifiers(ASTNode newTypeDecl) {
		if (fSenderBinding.isInterface()) {
			// for interface members copy the modifiers from an existing field
			FieldDeclaration[] fieldDecls= ((TypeDeclaration) newTypeDecl).getFields();
			if (fieldDecls.length > 0) {
				return fieldDecls[0].getModifiers();
			}
			return 0;
		}
		int modifiers= 0;

		if (fVariableKind == CONST_FIELD) {
			modifiers |= Modifier.FINAL | Modifier.STATIC;
		} else {
			ASTNode parent= fOriginalNode.getParent();	
			if (parent instanceof QualifiedName) {
				IBinding qualifierBinding= ((QualifiedName)parent).getQualifier().resolveBinding();
				if (qualifierBinding instanceof ITypeBinding) {
					modifiers |= Modifier.STATIC;
				}			
			} else if (ASTResolving.isInStaticContext(fOriginalNode)) {
				modifiers |= Modifier.STATIC;
			}
		}
		ASTNode node= ASTResolving.findParentType(fOriginalNode);
		if (newTypeDecl.equals(node)) {
			modifiers |= Modifier.PRIVATE;
		} else if (node instanceof AnonymousClassDeclaration) {
			modifiers |= Modifier.PROTECTED;
		} else {
			modifiers |= Modifier.PUBLIC;
		}
		
		return modifiers;
	}	


	/**
	 * Returns the variable kind.
	 * @return int
	 */
	public int getVariableKind() {
		return fVariableKind;
	}

}
