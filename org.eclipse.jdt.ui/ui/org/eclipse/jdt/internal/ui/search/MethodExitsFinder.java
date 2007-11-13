/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.Position;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.LocalVariableIndex;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowContext;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.FlowInfo;
import org.eclipse.jdt.internal.corext.refactoring.code.flow.InOutFlowAnalyzer;


public class MethodExitsFinder extends ASTVisitor {
	
	private AST fAST;
	private MethodDeclaration fMethodDeclaration;
	private List fResult;
	private List fCatchedExceptions;

	public String initialize(CompilationUnit root, int offset, int length) {
		return initialize(root, NodeFinder.perform(root, offset, length));
	}
	
	public String initialize(CompilationUnit root, ASTNode node) {
		fAST= root.getAST();
		
		if (node instanceof ReturnStatement) {
			fMethodDeclaration= (MethodDeclaration)ASTNodes.getParent(node, ASTNode.METHOD_DECLARATION);
			if (fMethodDeclaration == null)
				return SearchMessages.MethodExitsFinder_no_return_type_selected;
			return null;
			
		}
		
		Type type= null;
		if (node instanceof Type) {
			type= (Type)node;
		} else  if (node instanceof Name) {
			Name name= ASTNodes.getTopMostName((Name)node);
			if (name.getParent() instanceof Type) {
				type= (Type)name.getParent();
			}
		}
		if (type == null)
			return SearchMessages.MethodExitsFinder_no_return_type_selected; 
		type= ASTNodes.getTopMostType(type);
		if (!(type.getParent() instanceof MethodDeclaration))
			return SearchMessages.MethodExitsFinder_no_return_type_selected; 
		fMethodDeclaration= (MethodDeclaration)type.getParent();
		return null;
	}

	private void performSearch() {
		fResult= new ArrayList();
		markReferences();
		if (fResult.size() > 0) {
			Type returnType= fMethodDeclaration.getReturnType2();
			if (returnType != null)
				fResult.add(fMethodDeclaration.getReturnType2());
		}
	}

	public Position[] getOccurrencePositions() {
		performSearch();
		if (fResult.isEmpty())
			return null;

		Position[] positions= new Position[fResult.size()];
		for (int i= 0; i < fResult.size(); i++) {
			ASTNode node= (ASTNode) fResult.get(i);
			positions[i]= new Position(node.getStartPosition(), node.getLength());
		}
		return positions;
	}
	
	private void markReferences() {
		fCatchedExceptions= new ArrayList();
		boolean isVoid= true;
		Type returnType= fMethodDeclaration.getReturnType2();
		if (returnType != null) {
			ITypeBinding returnTypeBinding= returnType.resolveBinding();
			isVoid= returnTypeBinding != null && Bindings.isVoidType(returnTypeBinding);
		}
		fMethodDeclaration.accept(this);
		Block block= fMethodDeclaration.getBody();
		if (block != null) {
			List statements= block.statements();
			if (statements.size() > 0) {
				Statement last= (Statement)statements.get(statements.size() - 1);
				int maxVariableId= LocalVariableIndex.perform(fMethodDeclaration);
				FlowContext flowContext= new FlowContext(0, maxVariableId + 1);
				flowContext.setConsiderAccessMode(false);
				flowContext.setComputeMode(FlowContext.ARGUMENTS);
				InOutFlowAnalyzer flowAnalyzer= new InOutFlowAnalyzer(flowContext);
				FlowInfo info= flowAnalyzer.perform(new ASTNode[] {last});
				if (!info.isNoReturn() && !isVoid) {
					if (!info.isPartialReturn())
						return;
				}
			}
			SimpleName name= fAST.newSimpleName("x"); //$NON-NLS-1$
			name.setSourceRange(fMethodDeclaration.getStartPosition() + fMethodDeclaration.getLength() - 1, 1);
			fResult.add(name);
		}
	}

	public boolean visit(TypeDeclaration node) {
		// Don't dive into a local type.
		return false;
	}

	public boolean visit(AnonymousClassDeclaration node) {
		// Don't dive into a local type.
		return false;
	}

	public boolean visit(AnnotationTypeDeclaration node) {
		// Don't dive into a local type.
		return false;
	}

	public boolean visit(EnumDeclaration node) {
		// Don't dive into a local type.
		return false;
	}

	public boolean visit(ReturnStatement node) {
		fResult.add(node);
		return super.visit(node);
	}
	
	public boolean visit(TryStatement node) {
		int currentSize= fCatchedExceptions.size();
		List catchClauses= node.catchClauses();
		for (Iterator iter= catchClauses.iterator(); iter.hasNext();) {
			IVariableBinding variable= ((CatchClause)iter.next()).getException().resolveBinding();
			if (variable != null && variable.getType() != null) {
				fCatchedExceptions.add(variable.getType());
			}
		}
		node.getBody().accept(this);
		int toRemove= fCatchedExceptions.size() - currentSize;
		for(int i= toRemove; i > 0; i--) {
			fCatchedExceptions.remove(currentSize);
		}
		
		// visit catch and finally
		for (Iterator iter= catchClauses.iterator(); iter.hasNext(); ) {
			((CatchClause)iter.next()).accept(this);
		}
		if (node.getFinally() != null)
			node.getFinally().accept(this);
			
		// return false. We have visited the body by ourselves.	
		return false;
	}
	
	public boolean visit(ThrowStatement node) {
		ITypeBinding exception= node.getExpression().resolveTypeBinding();
		if (isExitPoint(exception)) {
			SimpleName name= fAST.newSimpleName("xxxxx"); //$NON-NLS-1$
			name.setSourceRange(node.getStartPosition(), 5);
			fResult.add(name);
		}	
		return true;
	}
	
	public boolean visit(MethodInvocation node) {
		if (isExitPoint(node.resolveMethodBinding())) {
			fResult.add(node.getName());
		}
		return true;
	}
	
	public boolean visit(SuperMethodInvocation node) {
		if (isExitPoint(node.resolveMethodBinding())) {
			fResult.add(node.getName());
		}
		return true;
	}
	
	public boolean visit(ClassInstanceCreation node) {
		if (isExitPoint(node.resolveConstructorBinding())) {
			fResult.add(node.getType());
		}
		return true;
	}
	
	public boolean visit(ConstructorInvocation node) {
		if (isExitPoint(node.resolveConstructorBinding())) {
			// mark this
			SimpleName name= fAST.newSimpleName("xxxx"); //$NON-NLS-1$
			name.setSourceRange(node.getStartPosition(), 4);
			fResult.add(name);
		}
		return true;
	}
	
	public boolean visit(SuperConstructorInvocation node) {
		if (isExitPoint(node.resolveConstructorBinding())) {
			SimpleName name= fAST.newSimpleName("xxxxx"); //$NON-NLS-1$
			name.setSourceRange(node.getStartPosition(), 5);
			fResult.add(name);
		}
		return true;
	}
	
	private boolean isExitPoint(ITypeBinding binding) {
		if (binding == null)
			return false;
		return !isCatched(binding);
	}
	
	private boolean isExitPoint(IMethodBinding binding) {
		if (binding == null)
			return false;
		ITypeBinding[] exceptions= binding.getExceptionTypes();
		for (int i= 0; i < exceptions.length; i++) {
			if (!isCatched(exceptions[i]))
				return true;
		}
		return false;
	}
	
	private boolean isCatched(ITypeBinding binding) {
		for (Iterator iter= fCatchedExceptions.iterator(); iter.hasNext();) {
			ITypeBinding catchException= (ITypeBinding)iter.next();
			if (catches(catchException, binding))
				return true;
		}
		return false;
	}
	
	private boolean catches(ITypeBinding catchTypeBinding, ITypeBinding throwTypeBinding) {
		while(throwTypeBinding != null) {
			if (throwTypeBinding == catchTypeBinding)
				return true;
			throwTypeBinding= throwTypeBinding.getSuperclass();	
		}
		return false;
	}	
}
