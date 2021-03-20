/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial version based on SurroundWithTryCatchAnalyzer
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.surround;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.util.SurroundWithAnalyzer;

public class SurroundWithTryWithResourcesAnalyzer extends SurroundWithAnalyzer {

	private ITypeBinding[] fExceptions;
	private ASTNode fEnclosingNode;
	private CompilationUnit fCompilationUnit;

	public SurroundWithTryWithResourcesAnalyzer(ICompilationUnit unit, Selection selection) throws CoreException {
		super(unit, selection, true);
	}

	public ITypeBinding[] getExceptions(Selection selection) {
		if (fEnclosingNode != null && !getStatus().hasFatalError()) {
			fExceptions= ExceptionAnalyzer.perform(fEnclosingNode, selection, false);
			if (fExceptions == null || fExceptions.length == 0) {
				if (fEnclosingNode instanceof MethodReference) {
					invalidSelection(RefactoringCoreMessages.SurroundWithTryCatchAnalyzer_doesNotContain);
				} else {
					List<ASTNode> autoClosableNodes= getCoveredAutoClosableNodes();
					if (autoClosableNodes.isEmpty()) {
						fExceptions= new ITypeBinding[] { fCompilationUnit.getAST().resolveWellKnownType("java.lang.Exception") }; //$NON-NLS-1$
					} else {
						fExceptions= new ITypeBinding[0];
					}
				}
			}
		}
		return fExceptions;
	}

	public ITypeBinding[] getCaughtExceptions() {
		List<ITypeBinding> exceptions= new ArrayList<>();
		TryStatement enclosingTry= (TryStatement)ASTResolving.findAncestor(getFirstSelectedNode(), ASTNode.TRY_STATEMENT);
		while (enclosingTry != null) {
			List<CatchClause> catchClauses= enclosingTry.catchClauses();
			for (CatchClause catchClause : catchClauses) {
				SingleVariableDeclaration sdv= catchClause.getException();
				Type type= sdv.getType();
				if (type instanceof UnionType) {
					UnionType unionType= (UnionType)type;
					List<Type> types= unionType.types();
					for (Type t : types) {
						ITypeBinding binding= t.resolveBinding();
						if (binding == null) {
							break;
						}
						exceptions.add(binding);
					}
				} else {
					ITypeBinding binding= type.resolveBinding();
					if (binding != null) {
						exceptions.add(binding);
					}
				}
			}
			enclosingTry= (TryStatement)ASTResolving.findAncestor(enclosingTry.getParent(), ASTNode.TRY_STATEMENT);
		}
		return exceptions.toArray(new ITypeBinding[0]);
	}

	public ITypeBinding[] getThrownExceptions() {
		List<ITypeBinding> exceptions= new ArrayList<>();
		if (fEnclosingNode.getNodeType() == ASTNode.METHOD_DECLARATION) {
			List<Type> thrownExceptions= ((MethodDeclaration) fEnclosingNode).thrownExceptionTypes();
			for (Type type : thrownExceptions) {
				ITypeBinding thrownException= type.resolveBinding();
				if (thrownException != null) {
					exceptions.add(thrownException);
				}
			}
		} else {
			ITypeBinding typeBinding= null;
			if (fEnclosingNode.getLocationInParent() == LambdaExpression.BODY_PROPERTY) {
				typeBinding= ((LambdaExpression) fEnclosingNode.getParent()).resolveTypeBinding();
			} else if (fEnclosingNode instanceof MethodReference) {
				typeBinding= ((MethodReference) fEnclosingNode).resolveTypeBinding();
			}
			if (typeBinding != null) {
				IMethodBinding methodBinding= typeBinding.getFunctionalInterfaceMethod();
				if (methodBinding != null) {
					Collections.addAll(exceptions, methodBinding.getExceptionTypes());
				}
			}
		}
		return exceptions.toArray(new ITypeBinding[0]);
	}

	public List<ITypeBinding> calculateCatchesAndRethrows(List<ITypeBinding> exceptions, List<ITypeBinding> mustRethrowList) {
		List<ITypeBinding> exceptionList= new ArrayList<>(exceptions);
		ITypeBinding[] caughtExceptions= getCaughtExceptions();
		if (caughtExceptions.length > 0) {
			for (Iterator<ITypeBinding> iter= exceptionList.iterator(); iter.hasNext();) {
				ITypeBinding binding= iter.next();
				for (ITypeBinding caughtException : caughtExceptions) {
					if (binding.isAssignmentCompatible(caughtException)) {
						iter.remove();
						break;
					}
				}
			}
			for (ITypeBinding binding : exceptionList) {
				for (ITypeBinding caughtException : caughtExceptions) {
					if (caughtException.isAssignmentCompatible(binding)) {
						mustRethrowList.add(caughtException);
						break;
					}
				}
			}
		}
		ITypeBinding[] thrownExceptions= getThrownExceptions();
		for (Iterator<ITypeBinding> iter= exceptionList.iterator(); iter.hasNext();) {
			ITypeBinding binding= iter.next();
			for (ITypeBinding thrownException : thrownExceptions) {
				if (binding.isAssignmentCompatible(thrownException)) {
					iter.remove();
					break;
				}
			}
		}
		for (ITypeBinding binding : exceptionList) {
			for (ITypeBinding thrownException : thrownExceptions) {
				if (thrownException.isAssignmentCompatible(binding)) {
					if (!mustRethrowList.contains(thrownException))
						mustRethrowList.add(thrownException);
					break;
				}
			}
		}
		return exceptionList;
	}

	public ASTNode getEnclosingNode() {
		return fEnclosingNode;
	}

	@Override
	public void endVisit(CompilationUnit node) {
		fEnclosingNode= null;
		fCompilationUnit= node;
		if (!getStatus().hasFatalError() && hasSelectedNodes())
			fEnclosingNode= SurroundWithAnalyzer.getEnclosingNode(getFirstSelectedNode());

		super.endVisit(node);
	}

	/**
	 * Return the first auto closable nodes. When a node that isn't Autoclosable is found the method
	 * returns.
	 *
	 * @return List of the first AutoClosable nodes found
	 */
	public List<ASTNode> getCoveredAutoClosableNodes() {
		ASTNode[] astNodes= getSelectedNodes();
		List<ASTNode> autoClosableNodes= new ArrayList<>();
		for (ASTNode astNode : astNodes) {
			if (isAutoClosable(astNode)) {
				autoClosableNodes.add(astNode);
			} else {
				return autoClosableNodes;
			}
		}
		return autoClosableNodes;
	}

	private boolean isAutoClosable(ASTNode astNode) {
		Map<SimpleName, IVariableBinding> simpleNames= getVariableStatementBinding(astNode);
		for (Entry<SimpleName, IVariableBinding> entry : simpleNames.entrySet()) {
			ITypeBinding typeBinding= null;
			switch (entry.getKey().getParent().getNodeType()) {
				case ASTNode.VARIABLE_DECLARATION_FRAGMENT:
				case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				case ASTNode.ASSIGNMENT:
					typeBinding= entry.getValue().getType();
					break;
				default:
					continue;
			}
			if (typeBinding == null) {
				continue;
			}
			for (ITypeBinding superType : Bindings.getAllSuperTypes(typeBinding)) {
				if ("java.lang.AutoCloseable".equals(superType.getQualifiedName())) { //$NON-NLS-1$
					return true;
				}
			}
		}
		return false;
	}

	public Map<SimpleName, IVariableBinding> getVariableStatementBinding(ASTNode astNode) {
		Map<SimpleName, IVariableBinding> variableBindings= new HashMap<>();
		astNode.accept(new ASTVisitor() {
			@Override
			public boolean visit(VariableDeclarationStatement node) {
				for (Object o : node.fragments()) {
					if (o instanceof VariableDeclarationFragment) {
						VariableDeclarationFragment vdf= (VariableDeclarationFragment) o;
						SimpleName name= vdf.getName();
						IBinding binding= name.resolveBinding();
						if (binding instanceof IVariableBinding) {
							variableBindings.put(name, (IVariableBinding) binding);
							break;
						}
					}
				}
				return false;
			}
		});
		return variableBindings;
	}

}
