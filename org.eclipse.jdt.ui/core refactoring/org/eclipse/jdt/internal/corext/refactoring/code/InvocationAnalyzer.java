/*******************************************************************************
 * Copyright (c) 2000, 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Common Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/cpl-v10.
 * html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry;

class InvocationAnalyzer {

	private SourceProvider fSourceProvider;
	
	// temporary fields
	private ICompilationUnit fCUnit;
	private ASTNode fInvocation;
	private ASTNode fTargetNode;
	private int fSeverity;
	
	public InvocationAnalyzer(SourceProvider sourceProvider) {
		fSourceProvider= sourceProvider;
	}
	
	public RefactoringStatus perform(ICompilationUnit unit, ASTNode invocation, ASTNode targetNode, int severity) {
		RefactoringStatus result= new RefactoringStatus();
		fCUnit= unit;
		fInvocation= invocation;
		fSeverity= severity;
		fTargetNode= targetNode;
		checkIfUsedInDeclaration(result);
		if (result.getSeverity() >= fSeverity)
			return result;
		checkInvocationContext(result);
		if (result.getSeverity() >= fSeverity)
			return result;		
		return result;
	}
	
	private void checkInvocationContext(RefactoringStatus result) {
		if (fInvocation.getNodeType() == ASTNode.METHOD_INVOCATION) {
			Expression exp= ((MethodInvocation)fInvocation).getExpression();
			if (exp != null && exp.resolveTypeBinding() == null) {
				addEntry(result, RefactoringCoreMessages.getString("TargetProvider.receiver_type"), //$NON-NLS-1$
					RefactoringStatusCodes.INLINE_METHOD_NULL_BINDING);
			}
		}
		int nodeType= fTargetNode.getNodeType();
		if (nodeType == ASTNode.EXPRESSION_STATEMENT) {
			if (fSourceProvider.isExecutionFlowInterrupted()) {
				addEntry(result,
					RefactoringCoreMessages.getString("CallInliner.execution_flow"),  //$NON-NLS-1$
					fSeverity,
					JavaSourceContext.create(fSourceProvider.getCompilationUnit(), fSourceProvider.getDeclaration()),
					RefactoringStatusCodes.INLINE_METHOD_EXECUTION_FLOW);
			}
		} else if (nodeType == ASTNode.METHOD_INVOCATION) {
			ASTNode parent= fTargetNode.getParent();
			if (parent.getNodeType() == ASTNode.ASSIGNMENT || isSingleDeclaration(parent)) {
				// this is ok
			} else if (isMultiDeclarationFragment(parent)) {
				if (!fSourceProvider.isSimpleFunction()) {
					addEntry(result,
						RefactoringCoreMessages.getString("InvocationAnalyzer.multiDeclaration"), //$NON-NLS-1$
						RefactoringStatusCodes.INLINE_METHOD_INITIALIZER_IN_FRAGEMENT);
				}
			} else if (fSourceProvider.getNumberOfStatements() > 1 ) {
				addEntry(result,
					RefactoringCoreMessages.getString("CallInliner.simple_functions"), //$NON-NLS-1$
					RefactoringStatusCodes.INLINE_METHOD_ONLY_SIMPLE_FUNCTIONS);
			} else if (!fSourceProvider.isSimpleFunction()) {
				addEntry(result,
					RefactoringCoreMessages.getString("CallInliner.execution_flow"),  //$NON-NLS-1$
					fSeverity,
					JavaSourceContext.create(fSourceProvider.getCompilationUnit(), fSourceProvider.getDeclaration()),
					RefactoringStatusCodes.INLINE_METHOD_EXECUTION_FLOW);
			}
		}		
	}

	private void checkIfUsedInDeclaration(RefactoringStatus result) {
		// we don't have support for field initializers yet. Skipping.
		BodyDeclaration decl= (BodyDeclaration)ASTNodes.getParent(fInvocation, BodyDeclaration.class);
		if (decl instanceof FieldDeclaration) {
			addEntry(result, RefactoringCoreMessages.getString("TargetProvider.field_initializer"), //$NON-NLS-1$
				RefactoringStatusCodes.INLINE_METHOD_FIELD_INITIALIZER);
			return;
		}
		if (fSourceProvider.isExecutionFlowInterrupted()) {
			VariableDeclaration vDecl= (VariableDeclaration)ASTNodes.getParent(fInvocation, VariableDeclaration.class);
			if (vDecl != null) {
				addEntry(result, RefactoringCoreMessages.getString("CallInliner.execution_flow"),  //$NON-NLS-1$
					RefactoringStatusCodes.INLINE_METHOD_LOCAL_INITIALIZER);
			}
		}
	}
	
	private static boolean isMultiDeclarationFragment(ASTNode node) {
		int nodeType= node.getNodeType();
		if (nodeType == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			node= node.getParent();
			if (node.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				VariableDeclarationStatement vs= (VariableDeclarationStatement)node;
				return vs.fragments().size() > 1;
			}
		}
		return false;
	}
	
	private static boolean isSingleDeclaration(ASTNode node) {
		int type= node.getNodeType();
		if (type == ASTNode.SINGLE_VARIABLE_DECLARATION)
			return true;
		if (type == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			node= node.getParent();
			if (node.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				VariableDeclarationStatement vs= (VariableDeclarationStatement)node;
				return vs.fragments().size() == 1;
			}
		}
		return false;
	}
	
	private void addEntry(RefactoringStatus result, String message, int code) {
		result.addEntry(new RefactoringStatusEntry(
			message, fSeverity, 
			JavaSourceContext.create(fCUnit, fInvocation),
			null, code));
	}
	
	private static void addEntry(RefactoringStatus result, String message, int severity, Context context, int code) {
		result.addEntry(new RefactoringStatusEntry(
			message, severity,
			context, 
			null, code));
	}	
}
