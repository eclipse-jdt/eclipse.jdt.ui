/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> - Bug 37432 getInvertEqualsProposal
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.LinkedNodeFinder;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
  */
public class QuickAssistProcessor implements IQuickAssistProcessor {
	
	public QuickAssistProcessor() {
		super();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.IAssistProcessor#hasAssists(org.eclipse.jdt.internal.ui.text.correction.IAssistContext)
	 */
	public boolean hasAssists(IInvocationContext context) throws CoreException {
		ASTNode coveringNode= getCoveringNode(context);
		if (coveringNode != null) {
			return getCatchClauseToThrowsProposals(context, coveringNode, null) 
				|| getRenameLocalProposals(context, coveringNode, null, null)
				|| getAssignToVariableProposals(context, coveringNode, null)
				|| getUnWrapProposals(context, coveringNode, null)
				|| getAssignParamToFieldProposals(context, coveringNode, null)
				|| getJoinVariableProposals(context, coveringNode, null)
				|| getAddFinallyProposals(context, coveringNode, null)
				|| getAddElseProposals(context, coveringNode, null)
				|| getSplitVariableProposals(context, coveringNode, null)
				|| getAddBlockProposals(context, coveringNode, null)
				|| getInvertEqualsProposal(context, coveringNode, null);
		}
		return false;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.correction.IAssistProcessor#getAssists(org.eclipse.jdt.internal.ui.text.correction.IAssistContext, org.eclipse.jdt.internal.ui.text.correction.IProblemLocation[])
	 */
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations) throws CoreException {
		ASTNode coveringNode= getCoveringNode(context);
		if (coveringNode != null) {
			ArrayList resultingCollections= new ArrayList();
			// quick assists that show up also if there is an error/warning
			getRenameLocalProposals(context, coveringNode, locations, resultingCollections);
		
			if (noErrorsAtLocation(locations)) {
				getCatchClauseToThrowsProposals(context, coveringNode, resultingCollections);
				getAssignToVariableProposals(context, coveringNode, resultingCollections);
				getAssignParamToFieldProposals(context, coveringNode, resultingCollections);
				getUnWrapProposals(context, coveringNode, resultingCollections);
				getSplitVariableProposals(context, coveringNode, resultingCollections);
				getJoinVariableProposals(context, coveringNode, resultingCollections);
				getAddFinallyProposals(context, coveringNode, resultingCollections);
				getAddElseProposals(context, coveringNode, resultingCollections);
				getAddBlockProposals(context, coveringNode, resultingCollections);
				getInvertEqualsProposal(context, coveringNode, resultingCollections);
			}
			return (IJavaCompletionProposal[]) resultingCollections.toArray(new IJavaCompletionProposal[resultingCollections.size()]);
		}
		return null;
	}
	
	private boolean noErrorsAtLocation(IProblemLocation[] locations) {
		if (locations != null) {
			for (int i= 0; i < locations.length; i++) {
				if (locations[i].isError()) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean getJoinVariableProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		ASTNode parent= node.getParent();
		if (!(parent instanceof VariableDeclarationFragment)) {
			return false;
		}
		VariableDeclarationFragment fragment= (VariableDeclarationFragment) parent;
		
		IVariableBinding binding= fragment.resolveBinding();
		if (fragment.getInitializer() != null || binding == null || binding.isField()) {
			return false;
		}
		
		if (!(fragment.getParent() instanceof VariableDeclarationStatement)) {
			return false;
		}
		VariableDeclarationStatement statement= (VariableDeclarationStatement) fragment.getParent();
		
		SimpleName[] names= LinkedNodeFinder.findByBinding(statement.getParent(), binding);
		if (names.length <= 1 || names[0] != fragment.getName()) {
			return false;
		}
		
		SimpleName firstAccess= names[1];
		if (!(firstAccess.getParent() instanceof Assignment)) { 
			return false;
		}
		Assignment assignment= (Assignment) firstAccess.getParent();
		if (assignment.getLeftHandSide() != firstAccess) {
			return false;
		}
		
		ASTNode assignParent= assignment.getParent();
		if (!(assignParent instanceof ExpressionStatement || assignParent instanceof ForStatement && ((ForStatement) assignParent).initializers().contains(assignment))) {
			return false;
		}
				
		if (resultingCollections == null) {
			return true;
		}

		AST ast= statement.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		
		String label= CorrectionMessages.getString("QuickAssistProcessor.joindeclaration.description"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);

		Expression placeholder= (Expression) rewrite.createMoveTarget(assignment.getRightHandSide());
		rewrite.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, placeholder, null);
		
		if (assignParent instanceof ExpressionStatement) {
			int statementParent= assignParent.getParent().getNodeType();
			if (statementParent == ASTNode.IF_STATEMENT || statementParent == ASTNode.WHILE_STATEMENT || statementParent == ASTNode.DO_STATEMENT
				|| statementParent == ASTNode.FOR_STATEMENT) {
				
				Block block= ast.newBlock();
				rewrite.replace(assignParent, block, null);
			} else {
				rewrite.remove(assignParent, null);	
			}	
		} else {
			rewrite.remove(assignment, null);
		}		
		
		proposal.setEndPosition(rewrite.track(fragment.getName()));
		resultingCollections.add(proposal);
		return true;
		
	}
	
	private boolean getSplitVariableProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		ASTNode parent= node.getParent();
		if (!(parent instanceof VariableDeclarationFragment)) {
			return false;
		}
		VariableDeclarationFragment fragment= (VariableDeclarationFragment) parent;
		
		if (fragment.getInitializer() == null) {
			return false;
		}

		Statement statement;
		ASTNode fragParent= fragment.getParent();
		if (fragParent instanceof VariableDeclarationStatement) {
			statement= (VariableDeclarationStatement) fragParent;
		} else if (fragParent instanceof VariableDeclarationExpression) {
			statement= (Statement) fragParent.getParent();
		} else {
			return false;
		}
		// statement is ForStatement or VariableDeclarationStatement
		
		ASTNode statementParent= statement.getParent();
		StructuralPropertyDescriptor property= statement.getLocationInParent();
		if (!property.isChildListProperty()) {
			return false;
		}

		List list= (List) statementParent.getStructuralProperty(property);
		
		if (resultingCollections == null) {
			return true;
		}

		AST ast= statement.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		
		String label= CorrectionMessages.getString("QuickAssistProcessor.splitdeclaration.description"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_LOCAL);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);

		Statement newStatement;
		int insertIndex= list.indexOf(statement);

		Expression placeholder= (Expression) rewrite.createMoveTarget(fragment.getInitializer());
		Assignment assignment= ast.newAssignment();
		assignment.setRightHandSide(placeholder);
		assignment.setLeftHandSide(ast.newSimpleName(fragment.getName().getIdentifier()));

		if (statement instanceof VariableDeclarationStatement) {
			newStatement= ast.newExpressionStatement(assignment);
			insertIndex+= 1; // add after declaration
			
			int modifiers= ((VariableDeclarationStatement) statement).getModifiers();
			if (Modifier.isFinal(modifiers)) {
				rewrite.set(statement, VariableDeclarationStatement.MODIFIERS_PROPERTY, new Integer(modifiers & ~Modifier.FINAL), null);
			}
		} else {
			rewrite.replace(fragment.getParent(), assignment, null);
			VariableDeclarationFragment newFrag= ast.newVariableDeclarationFragment();
			newFrag.setName(ast.newSimpleName(fragment.getName().getIdentifier()));
			newFrag.setExtraDimensions(fragment.getExtraDimensions());
			
			VariableDeclarationExpression oldVarDecl= (VariableDeclarationExpression) fragParent;
			
			VariableDeclarationStatement newVarDec= ast.newVariableDeclarationStatement(newFrag);
			newVarDec.setType((Type) ASTNode.copySubtree(ast, oldVarDecl.getType()));
			newVarDec.setModifiers(oldVarDecl.getModifiers() & ~Modifier.FINAL);
			newStatement= newVarDec;
		}
		
		ListRewrite listRewriter= rewrite.getListRewrite(statementParent, (ChildListPropertyDescriptor) property);
		listRewriter.insertAt(newStatement, insertIndex, null);

		resultingCollections.add(proposal);
		return true;
	}

	private ASTNode getCoveringNode(IInvocationContext context) {
		NodeFinder finder= new NodeFinder(context.getSelectionOffset(), context.getSelectionLength());
		context.getASTRoot().accept(finder);
		return finder.getCoveringNode();	
	}
	
	
	private boolean getAssignToVariableProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		Statement statement= ASTResolving.findParentStatement(node);
		if (!(statement instanceof ExpressionStatement)) {
			return false;
		}
		ExpressionStatement expressionStatement= (ExpressionStatement) statement;

		Expression expression= expressionStatement.getExpression();
		if (expression.getNodeType() == ASTNode.ASSIGNMENT) {
			return false; // too confusing and not helpful
		}
		
		ITypeBinding typeBinding= expression.resolveTypeBinding();
		typeBinding= Bindings.normalizeTypeBinding(typeBinding);
		if (typeBinding == null) {
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}
		
		ICompilationUnit cu= context.getCompilationUnit();
		
		AssignToVariableAssistProposal localProposal= new AssignToVariableAssistProposal(cu, AssignToVariableAssistProposal.LOCAL, expressionStatement, typeBinding, 2);
		resultingCollections.add(localProposal);	
		
		ASTNode type= ASTResolving.findParentType(expression);
		if (type != null) {
			AssignToVariableAssistProposal fieldProposal= new AssignToVariableAssistProposal(cu, AssignToVariableAssistProposal.FIELD, expressionStatement, typeBinding, 1);
			resultingCollections.add(fieldProposal);
		}
		return false;
		
	}
	
	private boolean getAssignParamToFieldProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		ASTNode parent= node.getParent();
		if (!(parent instanceof SingleVariableDeclaration) || !(parent.getParent() instanceof MethodDeclaration)) {
			return false;
		}
		SingleVariableDeclaration paramDecl= (SingleVariableDeclaration) parent;
		IVariableBinding binding= paramDecl.resolveBinding();
		
		MethodDeclaration methodDecl= (MethodDeclaration) parent.getParent();		
		if (binding == null || methodDecl.getBody() == null) {
			return false;
		}
		ITypeBinding typeBinding= binding.getType();
		if (typeBinding == null) {
			return false;
		}
		
		if (resultingCollections != null) {
			AssignToVariableAssistProposal fieldProposal= new AssignToVariableAssistProposal(context.getCompilationUnit(), paramDecl, typeBinding, 1);
			resultingCollections.add(fieldProposal);
		}
		return true;				
	}
	
	private boolean getAddFinallyProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		TryStatement tryStatement= ASTResolving.findParentTryStatement(node);
		if (tryStatement == null || tryStatement.getFinally() != null) {
			return false;	
		}
		Statement statement= ASTResolving.findParentStatement(node);
		if (tryStatement != statement && tryStatement.getBody() != statement) {
			return false; // an node inside a catch or finally block		
		}
		
		if (resultingCollections == null) {
			return true;
		}
		
		AST ast= tryStatement.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		Block finallyBody= ast.newBlock();

		rewrite.set(tryStatement, TryStatement.FINALLY_PROPERTY, finallyBody, null);
				
		String label= CorrectionMessages.getString("QuickAssistProcessor.addfinallyblock.description"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}
	
	private boolean getAddElseProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		Statement statement= ASTResolving.findParentStatement(node);
		if (!(statement instanceof IfStatement)) {
			return false;
		}
		IfStatement ifStatement= (IfStatement) statement;
		if (ifStatement.getElseStatement() != null) {
			return false;
		}
		
		if (resultingCollections == null) {
			return true;
		}
		
		AST ast= statement.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);
		Block body= ast.newBlock();

		rewrite.set(ifStatement, IfStatement.ELSE_STATEMENT_PROPERTY, body, null);
				
		String label= CorrectionMessages.getString("QuickAssistProcessor.addelseblock.description"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_ADD);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}	

	public static boolean getCatchClauseToThrowsProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		CatchClause catchClause= (CatchClause) ASTResolving.findAncestor(node, ASTNode.CATCH_CLAUSE);
		if (catchClause == null) {
			return false;
		}
		
		Statement statement= ASTResolving.findParentStatement(node);
		if (statement != catchClause.getParent() && statement != catchClause.getBody()) {
			return false; // selection is in a statement inside the body
		}
		
		Type type= catchClause.getException().getType();
		if (!type.isSimpleType()) {
			return false;
		}
		
		BodyDeclaration bodyDeclaration= ASTResolving.findParentBodyDeclaration(catchClause);
		if (!(bodyDeclaration instanceof MethodDeclaration)) {
			return false;
		}
		
		if (resultingCollections == null) {
			return true;
		}
		
		AST ast= bodyDeclaration.getAST();
		MethodDeclaration methodDeclaration= (MethodDeclaration) bodyDeclaration;
		{		
			ASTRewrite rewrite= ASTRewrite.create(ast);
			
			removeCatchBlock(rewrite, methodDeclaration, catchClause);
	
			ITypeBinding binding= type.resolveBinding();
			if (binding == null || isNotYetThrown(binding, methodDeclaration.thrownExceptions())) {
				Name name= ((SimpleType) type).getName();
				Name newName= (Name) ASTNode.copySubtree(catchClause.getAST(), name);
				
				ListRewrite listRewriter= rewrite.getListRewrite(methodDeclaration, MethodDeclaration.THROWN_EXCEPTIONS_PROPERTY);
				listRewriter.insertLast(newName, null);
			}
		
			String label= CorrectionMessages.getString("QuickAssistProcessor.catchclausetothrows.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 4, image);
			resultingCollections.add(proposal);
		}
		{
			ASTRewrite rewrite= ASTRewrite.create(ast);
			
			removeCatchBlock(rewrite, methodDeclaration, catchClause);
			String label= CorrectionMessages.getString("QuickAssistProcessor.removecatchclause.description"); //$NON-NLS-1$
			Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 5, image);
			resultingCollections.add(proposal);
		}
		
		return true;
	}
	
	private static void removeCatchBlock(ASTRewrite rewrite, MethodDeclaration methodDeclaration, CatchClause catchClause) {
		TryStatement tryStatement= (TryStatement) catchClause.getParent();
		if (tryStatement.catchClauses().size() > 1 || tryStatement.getFinally() != null) {
			rewrite.remove(catchClause, null);
		} else {
			Block block= tryStatement.getBody();
			List statements= block.statements();
			if (statements.size() > 0) {			
				ListRewrite listRewrite= rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
				ASTNode first= (ASTNode) statements.get(0);
				ASTNode last= (ASTNode) statements.get(statements.size() - 1);
				ASTNode placeholder= listRewrite.createCopyTarget(first, last);
				rewrite.replace(tryStatement, placeholder, null);
			} else {
				rewrite.remove(tryStatement, null);
			}
		}
	}

	private static boolean isNotYetThrown(ITypeBinding binding, List thrownExcpetions) {
		for (int i= 0; i < thrownExcpetions.size(); i++) {
			Name name= (Name) thrownExcpetions.get(i);
			ITypeBinding elem= (ITypeBinding) name.resolveBinding();
			if (elem != null) {
				if (Bindings.isSuperType(elem, binding)) { // existing exception is base class of new
					return false;
				}
			}
		}
		return true;
	}
	
	
	private boolean getRenameLocalProposals(IInvocationContext context, ASTNode node, IProblemLocation[] locations, Collection resultingCollections) {
		if (!(node instanceof SimpleName)) {
			return false;
		}
		SimpleName name= (SimpleName) node;
		IBinding binding= name.resolveBinding();
		if (binding != null && binding.getKind() == IBinding.PACKAGE) {
			return false;
		}

		if (locations != null) {
			for (int i= 0; i < locations.length; i++) {
				switch (locations[i].getProblemId()) {
					case IProblem.LocalVariableHidingLocalVariable:
					case IProblem.LocalVariableHidingField:
					case IProblem.FieldHidingLocalVariable:
					case IProblem.FieldHidingField:
					case IProblem.ArgumentHidingLocalVariable:
					case IProblem.ArgumentHidingField:
						return false;
				}
			}
		}
		
		if (resultingCollections == null) {
			return true;
		}
				
		LinkedNamesAssistProposal proposal= new LinkedNamesAssistProposal(context.getCompilationUnit(), name);
		resultingCollections.add(proposal);
		return true;
	}
	
	private ASTNode getCopyOfInner(ASTRewrite rewrite, ASTNode statement) {
		if (statement.getNodeType() == ASTNode.BLOCK) {
			Block block= (Block) statement;
			List innerStatements= block.statements();
			int nStatements= innerStatements.size();
			if (nStatements == 1) {
				return rewrite.createCopyTarget(((ASTNode) innerStatements.get(0)));
			} else if (nStatements > 1) {
				ListRewrite listRewrite= rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
				ASTNode first= (ASTNode) innerStatements.get(0);
				ASTNode last= (ASTNode) innerStatements.get(nStatements - 1);
				return listRewrite.createCopyTarget(first, last);
			}
			return null;
		} else {
			return rewrite.createCopyTarget(statement);
		}
	}
	
	
	private boolean getUnWrapProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		ASTNode outer= node;
			
		Block block= null;
		if (outer.getNodeType() == ASTNode.BLOCK) {
			block= (Block) outer;
			outer= block.getParent();
		}
		
		ASTNode body= null;
		String label= null;
		if (outer instanceof IfStatement) {
			IfStatement ifStatement= (IfStatement) outer;
			Statement elseBlock= ifStatement.getElseStatement();
			if (elseBlock == null || ((elseBlock instanceof Block) && ((Block) elseBlock).statements().isEmpty())) {
				body= ifStatement.getThenStatement();
			}
			label= CorrectionMessages.getString("QuickAssistProcessor.unwrap.ifstatement");	 //$NON-NLS-1$
		} else if (outer instanceof WhileStatement) {
			body=((WhileStatement) outer).getBody();
			label= CorrectionMessages.getString("QuickAssistProcessor.unwrap.whilestatement");	 //$NON-NLS-1$
		} else if (outer instanceof ForStatement) {
			body=((ForStatement) outer).getBody();
			label= CorrectionMessages.getString("QuickAssistProcessor.unwrap.forstatement");	 //$NON-NLS-1$
		} else if (outer instanceof DoStatement) {
			body=((DoStatement) outer).getBody();
			label= CorrectionMessages.getString("QuickAssistProcessor.unwrap.dostatement");	 //$NON-NLS-1$
		} else if (outer instanceof TryStatement) {
			TryStatement tryStatement= (TryStatement) outer;
			if (tryStatement.catchClauses().isEmpty()) {
				body= tryStatement.getBody();
			}
			label= CorrectionMessages.getString("QuickAssistProcessor.unwrap.trystatement");	 //$NON-NLS-1$
		} else if (outer instanceof AnonymousClassDeclaration) {
			List decls= ((AnonymousClassDeclaration) outer).bodyDeclarations();
			for (int i= 0; i < decls.size(); i++) {
				ASTNode elem= (ASTNode) decls.get(i);
				if (elem instanceof MethodDeclaration) {
					Block curr= ((MethodDeclaration) elem).getBody();
					if (curr != null && !curr.statements().isEmpty()) {
						if (body != null) {
							return false;
						}
						body= curr;
					}
				} else if (elem instanceof TypeDeclaration) {
					return false;
				}
			}
			label= CorrectionMessages.getString("QuickAssistProcessor.unwrap.anonymous");	 //$NON-NLS-1$
			outer= ASTResolving.findParentStatement(outer);
		} else if (outer instanceof Block) {
			//	-> a block in a block
			body= block;
			outer= block;
			label= CorrectionMessages.getString("QuickAssistProcessor.unwrap.block");	 //$NON-NLS-1$
		} else if (outer instanceof ParenthesizedExpression) {
			ParenthesizedExpression expression= (ParenthesizedExpression) outer;
			body= expression.getExpression();
			label= CorrectionMessages.getString("QuickAssistProcessor.unwrap.parenthesis");	 //$NON-NLS-1$
		} else if (outer instanceof MethodInvocation) {
			MethodInvocation invocation= (MethodInvocation) outer;
			if (invocation.arguments().size() == 1) {
				body= (ASTNode) invocation.arguments().get(0);
				if (invocation.getParent().getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
					int kind= body.getNodeType();
					if (kind != ASTNode.ASSIGNMENT && kind != ASTNode.PREFIX_EXPRESSION && kind != ASTNode.POSTFIX_EXPRESSION
							&& kind != ASTNode.METHOD_INVOCATION && kind != ASTNode.SUPER_METHOD_INVOCATION) {
						body= null;
					}
				}
				label= CorrectionMessages.getString("QuickAssistProcessor.unwrap.methodinvocation");	 //$NON-NLS-1$
			}
		}
		if (body == null) {
			return false; 
		}
		ASTRewrite rewrite= ASTRewrite.create(outer.getAST());
		ASTNode inner= getCopyOfInner(rewrite, body);
		if (inner == null) {
			return false;
		}
		if (resultingCollections == null) {
			return true;
		}
			
		rewrite.replace(outer, inner, null);
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
		ASTRewriteCorrectionProposal proposal= new ASTRewriteCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;
	}

	private static boolean isControlStatementWithBlock(ASTNode node) {
		switch (node.getNodeType()) {
			case ASTNode.IF_STATEMENT:
			case ASTNode.WHILE_STATEMENT:
			case ASTNode.FOR_STATEMENT:
			case ASTNode.DO_STATEMENT:
				return true;
			default:
				return false;
		}
	}
	
	
	private boolean getAddBlockProposals(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		Statement statement= ASTResolving.findParentStatement(node);
		if (statement == null) {
			return false;
		}

		if (!isControlStatementWithBlock(statement)) {
			if (!isControlStatementWithBlock(statement.getParent())) {
				return false;
			}
			int statementStart= statement.getStartPosition();
			int statementEnd= statementStart + statement.getLength();
			
			int offset= context.getSelectionOffset();
			int length= context.getSelectionLength();
			if (length == 0) {
				if (offset != statementEnd) { // cursor at end
					return false;
				}
			} else {
				if (offset > statementStart || offset + length < statementEnd) { // statement selected
					return false;
				}
			}
			statement= (Statement) statement.getParent();
		}
		
		StructuralPropertyDescriptor childProperty= null;
		ASTNode child= null;
		switch (statement.getNodeType()) {
			case ASTNode.IF_STATEMENT:
				int selectionStart= context.getSelectionOffset();
				int selectionEnd= context.getSelectionOffset() + context.getSelectionLength();
				ASTNode then= ((IfStatement) statement).getThenStatement();
				if (selectionEnd <= then.getStartPosition() + then.getLength()) {
					if (!(then instanceof Block)) {
						childProperty= IfStatement.THEN_STATEMENT_PROPERTY;
						child= then;
					}
				} else if (selectionStart >=  then.getStartPosition() + then.getLength()) {
					ASTNode elseStatement= ((IfStatement) statement).getElseStatement();
					if (!(elseStatement instanceof Block)) {
						childProperty= IfStatement.ELSE_STATEMENT_PROPERTY;
						child= elseStatement;
					}
				}
				break;
			case ASTNode.WHILE_STATEMENT:
				ASTNode whileBody= ((WhileStatement) statement).getBody();
				if (!(whileBody instanceof Block)) {
					childProperty= WhileStatement.BODY_PROPERTY;
					child= whileBody;
				}
				break;
			case ASTNode.FOR_STATEMENT:
				ASTNode forBody= ((ForStatement) statement).getBody();
				if (!(forBody instanceof Block)) {
					childProperty= ForStatement.BODY_PROPERTY;
					child= forBody;
				}
				break;
			case ASTNode.DO_STATEMENT:
				ASTNode doBody= ((DoStatement) statement).getBody();
				if (!(doBody instanceof Block)) {
					childProperty= DoStatement.BODY_PROPERTY;
					child= doBody;
				}
				break;
			default:
		}
		if (child == null) {
			return false;
		}

		if (resultingCollections == null) {
			return true;
		}
		AST ast= statement.getAST();
		ASTRewrite rewrite= ASTRewrite.create(ast);

		ASTNode childPlaceholder= rewrite.createMoveTarget(child);
		Block replacingBody= ast.newBlock();
		replacingBody.statements().add(childPlaceholder);
		rewrite.set(statement, childProperty, replacingBody, null);

		String label;
		if (childProperty == IfStatement.THEN_STATEMENT_PROPERTY) {
			label = CorrectionMessages.getString("QuickAssistProcessor.replacethenwithblock.description");//$NON-NLS-1$
		} else if (childProperty == IfStatement.ELSE_STATEMENT_PROPERTY) {
			label = CorrectionMessages.getString("QuickAssistProcessor.replaceelsewithblock.description");//$NON-NLS-1$
		} else {
			label = CorrectionMessages.getString("QuickAssistProcessor.replacebodywithblock.description");//$NON-NLS-1$
		}
		
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, 10, image);
		proposal.setEndPosition(rewrite.track(child));
		resultingCollections.add(proposal);
		return true;
	}
	
	private boolean getInvertEqualsProposal(IInvocationContext context, ASTNode node, Collection resultingCollections) {
		ASTNode parent= node.getParent();
		if (!(parent instanceof MethodInvocation)) {
			return false;
		}
		MethodInvocation method= (MethodInvocation) parent;
		if (!"equals".equals(method.getName().getIdentifier())) { //$NON-NLS-1$
			return false;
		}
		List arguments= method.arguments();
		if (arguments.size() != 1) { //overloaded equals w/ more than 1 arg
			return false;
		}
		Expression right= (Expression) arguments.get(0);
		ITypeBinding binding = right.resolveTypeBinding();
		if (!(binding.isClass() || binding.isInterface())) { //overloaded equals w/ non-class/interface arg or null
			return false;
		}
		Expression left= method.getExpression();

		AST ast= method.getAST();
		ASTRewrite rewrite;
		if (left == null) { // equals(x) -> x.equals(this)
			rewrite= ASTRewrite.create(ast);
			MethodInvocation replacement= ast.newMethodInvocation();
			replacement.setName((SimpleName) rewrite.createCopyTarget(method.getName()));
			replacement.arguments().add(ast.newThisExpression());
			replacement.setExpression((Expression) rewrite.createCopyTarget(right));
			rewrite.replace(method, replacement, null);
		} else if (right instanceof ThisExpression) { // x.equals(this) -> equals(x)
			rewrite= ASTRewrite.create(ast);
			MethodInvocation replacement= rewrite.getAST().newMethodInvocation();
			replacement.setName((SimpleName) rewrite.createCopyTarget(method.getName()));
			replacement.arguments().add(rewrite.createCopyTarget(left));
			rewrite.replace(method, replacement, null);
		} else {
			rewrite= ASTRewrite.create(ast);
			if (left instanceof ParenthesizedExpression) {
				Expression ex= ((ParenthesizedExpression) left).getExpression();
				rewrite.replace(right, rewrite.createCopyTarget(ex), null);
			} else {
				rewrite.replace(right, rewrite.createCopyTarget(left), null);
			}
			if ((right instanceof CastExpression)
				|| (right instanceof Assignment)
				|| (right instanceof ConditionalExpression)
				|| (right instanceof InfixExpression)) {
				ParenthesizedExpression paren= rewrite.getAST().newParenthesizedExpression();
				paren.setExpression((Expression) rewrite.createCopyTarget(right));
				rewrite.replace(left, paren, null);
			} else {
				rewrite.replace(left, rewrite.createCopyTarget(right), null);
			}
		}
		
		String label= CorrectionMessages.getString("QuickAssistProcessor.invertequals.description"); //$NON-NLS-1$
		Image image= JavaPluginImages.get(JavaPluginImages.IMG_CORRECTION_CHANGE);
		
		LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, context.getCompilationUnit(), rewrite, 1, image);
		resultingCollections.add(proposal);
		return true;	
	}
}
