/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.LocalVariableIndex;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.InputFlowAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;

public class CallInliner {

	private ICompilationUnit fCUnit;
	private TextBuffer fBuffer;
	private SourceProvider fSourceProvider;
	
	private MethodInvocation fInvocation;
	private List fUsedNames;
	private ASTRewrite fRewriter;
	private List fStatements;
	private int fInsertionIndex;
	private ASTNode fTargetNode;
	private FlowContext fFlowContext;
	private FlowInfo fFlowInfo;

	private static class NameCollector extends GenericVisitor {
		List names= new ArrayList();
		private Selection fSelection;
		public NameCollector(ASTNode node) {
			fSelection= Selection.createFromStartLength(node.getStartPosition(), node.getLength());
		}
		protected boolean visitNode(ASTNode node) {
			if (node.getStartPosition() > fSelection.getInclusiveEnd())
				return true;
			if (fSelection.coveredBy(node))
				return true;
			return false;
		}
		public boolean visit(SimpleName node) {
			names.add(node.getIdentifier());
			return super.visit(node);
		}
		public boolean visit(VariableDeclarationStatement node) {
			return true;
		}
		public boolean visit(VariableDeclarationFragment node) {
			boolean result= super.visit(node);
			if (!result)
				names.add(node.getName().getIdentifier());
			return result;
		}
		public boolean visit(SingleVariableDeclaration node) {
			boolean result= super.visit(node);
			if (!result)
				names.add(node.getName().getIdentifier());
			return result;
		}
		public boolean visit(TypeDeclarationStatement node) {
			names.add(node.getTypeDeclaration().getName().getIdentifier());
			return false;
		}
	}

	public CallInliner(ICompilationUnit unit, SourceProvider sourceContext) throws CoreException {
		super();
		fCUnit= unit;
		fBuffer= TextBuffer.acquire(getFile(fCUnit));
		fSourceProvider= sourceContext;
	}

	public void dispose() {
		TextBuffer.release(fBuffer);
	}
	
	public RefactoringStatus initialize(MethodInvocation invocation) {
		RefactoringStatus result= new RefactoringStatus();
		fInvocation= invocation;
		initializeState(fSourceProvider.getNumberOfStatements());
		int nodeType= fTargetNode.getNodeType();
		if (nodeType == ASTNode.EXPRESSION_STATEMENT) {
			if (fSourceProvider.isExecutionFlowInterrupted()) {
				result.addFatalError(
					"Can't inline call. Return statement in method declaration interrupts execution flow.", 
					JavaSourceContext.create(fSourceProvider.getCompilationUnit(), fSourceProvider.getDeclaration()));	
			}
		} else if (nodeType == ASTNode.METHOD_INVOCATION) {
			if (!(isValidParent(fTargetNode.getParent()) || fSourceProvider.getNumberOfStatements() == 1)) {
				result.addFatalError(
					"Can only inline simple functions (consisting of a return statement) or functions used in an assignment.",
					JavaSourceContext.create(fCUnit, fInvocation));
			}
		}
		return result;
	}
	
	private boolean isValidParent(ASTNode parent) {
		int nodeType= parent.getNodeType();
		if (nodeType == ASTNode.ASSIGNMENT)
			return true;
		if (nodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			parent= parent.getParent();
			if (parent.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				VariableDeclarationStatement vs= (VariableDeclarationStatement)parent;
				return vs.fragments().size() == 1;
			}
		}
		return false;
	}
	
	public TextEdit perform() throws CoreException {
		List arguments= fInvocation.arguments();
		String[] newArgs= new String[arguments.size()];
		for (int i= 0; i < arguments.size(); i++) {
			Expression expression= (Expression)arguments.get(i);
			ParameterData parameter= fSourceProvider.getParameterData(i);
			if ((ASTNodes.isLiteral(expression) && parameter.isReadOnly()) || canInline(expression)) {
				newArgs[i]= getContent(expression);
			} else {
				String name= proposeName(parameter);
				newArgs[i]= name;
				addLocalDeclaration(parameter, name, expression);
			}
		}
		CallContext context= new CallContext(newArgs, fRewriter, fUsedNames, fTargetNode.getNodeType());
		ASTNode[] nodes= fSourceProvider.getInlineNodes(context);
		if (nodes.length == 0) {
			fRewriter.markAsRemoved(fTargetNode);
		} else {
			for (int i= 0; i < nodes.length - 1; i++) {
				ASTNode node= nodes[i];
				fRewriter.markAsInserted(node);
				fStatements.add(fInsertionIndex++, node);
			}
			ASTNode last= nodes[nodes.length - 1];
			fRewriter.markAsReplaced(fTargetNode, last);
		}
		MultiTextEdit result= new MultiTextEdit();
		fRewriter.rewriteNode(fBuffer, result, null);
		return result;
	}
	
	public void performed() {
		fRewriter.removeModifications();
	}
	
	private void addLocalDeclaration(ParameterData parameter, String name, Expression initializer) {
		VariableDeclarationStatement decl= (VariableDeclarationStatement)ASTNodeFactory.newStatement(
			fInvocation.getAST(), parameter.getTypeName() + " " + name + ";");
		fRewriter.markAsInserted(decl);
		((VariableDeclarationFragment)decl.fragments().get(0)).setInitializer((Expression)fRewriter.createCopy(initializer));
		fStatements.add(fInsertionIndex++, decl);
	}

	private  void initializeState(int sourceStatements) {
		fRewriter= new ASTRewrite(ASTNodes.getParent(fInvocation, ASTNode.BLOCK));
		fUsedNames= collectUsedNames();
		ASTNode parent= fInvocation.getParent();
		if (parent.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			fTargetNode= parent;
		} else {
			fTargetNode= fInvocation;
		}
		Statement enclosingStatement= (Statement)ASTNodes.getParent(fInvocation, Statement.class);
		// We have to consider for/while/if... here
		fStatements= ((Block)enclosingStatement.getParent()).statements();
		fInsertionIndex= fStatements.indexOf(enclosingStatement);
		
		MethodDeclaration decl= (MethodDeclaration)ASTNodes.getParent(fInvocation, ASTNode.METHOD_DECLARATION);
		int numberOfLocals= LocalVariableIndex.perform(decl);
		fFlowContext= new FlowContext(0, numberOfLocals + 1);
		fFlowContext.setConsiderAccessMode(true);
		fFlowContext.setComputeMode(FlowContext.ARGUMENTS);
		Selection selection= Selection.createFromStartLength(fInvocation.getStartPosition(), fInvocation.getLength());
		fFlowInfo= new InputFlowAnalyzer(fFlowContext, selection).perform(decl);
	}

	private boolean canInline(Expression expression) {
		if (expression instanceof Name) {
			IBinding binding= ((Name)expression).resolveBinding();
			if (binding instanceof IVariableBinding) {
				IVariableBinding vb= (IVariableBinding)binding;
				if (!vb.isField()) {
					return fFlowInfo.hasAccessMode(fFlowContext, vb, FlowInfo.UNUSED | FlowInfo.WRITE);
				}
			}
			return true;
		} else if (expression instanceof FieldAccess) {
			return canInline(((FieldAccess)expression).getExpression());
		} else if (expression instanceof ThisExpression) {
			return canInline(((ThisExpression)expression).getQualifier());
		} else if (expression instanceof SuperFieldAccess) {
			return canInline(((SuperFieldAccess)expression).getQualifier());
		}
		return false;
	}
	
	private String proposeName(ParameterData parameter) {
		String result= parameter.getName();
		int i= 1;
		while (fUsedNames.contains(result)) {
			result= result + i++;
		}
		return result;
	}

	private String getContent(ASTNode node) {
		return fBuffer.getContent(node.getStartPosition(), node.getLength());
	}

	private static IFile getFile(ICompilationUnit cu) throws CoreException {
		return (IFile)WorkingCopyUtil.getOriginal(cu).getCorrespondingResource();
	}
	
	private List collectUsedNames() {
		BodyDeclaration decl= (BodyDeclaration)ASTNodes.getParent(fInvocation, BodyDeclaration.class);
		NameCollector collector= new NameCollector(fInvocation);
		decl.accept(collector);
		return collector.names;
	}
}
