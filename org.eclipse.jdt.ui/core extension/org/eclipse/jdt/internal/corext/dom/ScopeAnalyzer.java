/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

/**
 * Evaluates all fields, methods and types available (declared) at a given offset
 * in a compilation unit.
 */
public class ScopeAnalyzer {
	
	/**
	 * Flag to specify that method should be reported.
	 */
	public static final int METHODS= 1;
	
	/**
	 * Flag to specify that variables should be reported.
	 */	
	public static final int VARIABLES= 2;
	
	/**
	 * Flag to specify that types should be reported.
	 */		
	public static final int TYPES= 4;
	
	private ArrayList fRequestor;
	private HashSet fNamesAdded;
	private HashSet fTypesVisited;
	
	private CompilationUnit fRoot;
	
	public ScopeAnalyzer() {
		fRequestor= new ArrayList();
		fNamesAdded= new HashSet();
		fTypesVisited= new HashSet();
	}
	
	private void clearLists() {
		fRequestor.clear();
		fNamesAdded.clear();
		fTypesVisited.clear();
		
		fRoot= null;
	}
	
	private static String getVariableSignature(IVariableBinding binding) {
		return 'V' + binding.getName();
	}

	private static String getTypeSignature(ITypeBinding binding) {
		return 'T' + binding.getName();
	}
	
	
	private static String getMethodSignature(IMethodBinding binding) {
		StringBuffer buf= new StringBuffer();
		buf.append('M');
		buf.append(binding.getName()).append('(');
		ITypeBinding[] parameters= binding.getParameterTypes();
		for (int i= 0; i < parameters.length; i++) {
			if (i > 0) {
				buf.append(',');
			}
			buf.append(parameters[i].getQualifiedName());
		}
		buf.append(')');
		return buf.toString();
	}
	
	private boolean hasFlag(int property, int flags) {
		return (flags & property) != 0;
	}
	
	
	private void addInherited(ITypeBinding binding, int flags) {
		if (!fTypesVisited.add(binding)) {
			return;
		}
		if (hasFlag(VARIABLES, flags)) {
			IVariableBinding[] variableBindings= binding.getDeclaredFields();
			for (int i= 0; i < variableBindings.length; i++) {
				IVariableBinding curr= variableBindings[i];
				if (fNamesAdded.add(getVariableSignature(curr))) { // avoid duplicated results from inheritance
					fRequestor.add(curr);
				}
			}
		}
		
		if (hasFlag(METHODS, flags)) {
			IMethodBinding[] methodBindings= binding.getDeclaredMethods();
			for (int i= 0; i < methodBindings.length; i++) {
				IMethodBinding curr= methodBindings[i];
				if (!curr.isSynthetic() && !curr.isConstructor()) {
					String signature= getMethodSignature(curr);
					if (fNamesAdded.add(signature)) { // avoid duplicated results from inheritance
						fRequestor.add(curr);
					}			
				}
			}
		}

		if (hasFlag(TYPES, flags)) {
			ITypeBinding[] typeBindings= binding.getDeclaredTypes();
			for (int i= 0; i < typeBindings.length; i++) {
				ITypeBinding curr= typeBindings[i];
				if (fNamesAdded.add(getTypeSignature(curr))) { // avoid duplicated results from inheritance
					fRequestor.add(curr);
				}				
			}
		}		
		
		
		ITypeBinding superClass= binding.getSuperclass();
		if (superClass != null) {
			addInherited(superClass, flags); // recursive
		}
		
		if (hasFlag(TYPES | VARIABLES, flags)) {
			ITypeBinding[] interfaces= binding.getInterfaces();
			for (int i= 0; i < interfaces.length; i++) {
				addInherited(interfaces[i], flags & (TYPES | VARIABLES)); // recursive
			}			
		}
	}
		
	
	
	private void addTypeDeclarations(ITypeBinding binding, int flags) {
		if (hasFlag(TYPES, flags)) {
			if (!binding.isAnonymous()) {
				fRequestor.add(binding);
			}
		}
		
		addInherited(binding, flags); // add inherited 
		
		if (binding.isLocal()) {
			addOuterDeclarationsForLocalType(binding, flags);
		} else {
			ITypeBinding declaringClass= binding.getDeclaringClass();
			if (declaringClass != null) {
				addTypeDeclarations(declaringClass, flags); // recursivly add inherited 
			} else if (hasFlag(TYPES, flags)) {
				if (fRoot.findDeclaringNode(binding) != null) {
					List types= fRoot.types();
					for (int i= 0; i < types.size(); i++) {
						TypeDeclaration decl= (TypeDeclaration) types.get(i);
						ITypeBinding curr= decl.resolveBinding();
						if (curr != null && fNamesAdded.add(getTypeSignature(curr))) { // avoid duplicated results from inheritance
							fRequestor.add(curr);
						}	
					}
				}
			}
		}
	}
	
	private void addOuterDeclarationsForLocalType(ITypeBinding localBinding, int flags) {
		ASTNode node= fRoot.findDeclaringNode(localBinding);
		if (node == null) {
			return;
		}
		
		if (node instanceof TypeDeclaration || node instanceof AnonymousClassDeclaration) {
			addLocalDeclarations(node.getParent(), flags);
			
			ITypeBinding parentTypeBidning= ASTResolving.getBindingOfParentType(node.getParent());
			if (parentTypeBidning != null) {
				addTypeDeclarations(parentTypeBidning, flags);
			}
			
		}
	}
	
	private static ITypeBinding getBinding(Expression node) {
		if (node != null) {
			return node.resolveTypeBinding();
		}
		return null;
	}
		
	private static ITypeBinding getQualifier(SimpleName selector) {
		ASTNode parent= selector.getParent();
		switch (parent.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return getBinding(((MethodInvocation) parent).getExpression());
			case ASTNode.QUALIFIED_NAME:
				return getBinding(((QualifiedName) parent).getQualifier());
			case ASTNode.FIELD_ACCESS:
				return getBinding(((FieldAccess) parent).getExpression());
			case ASTNode.SUPER_FIELD_ACCESS:
			case ASTNode.SUPER_METHOD_INVOCATION:
				ITypeBinding curr= ASTResolving.getBindingOfParentType(parent);
				return curr.getSuperclass();
			default:
				return null;
		}
	}
	
	public IBinding[] getDeclarationsInScope(CompilationUnit root, int offset, int flags) {
		fRoot= root;
		try {
			NodeFinder finder= new NodeFinder(offset, 0);
			root.accept(finder);
			ASTNode node= finder.getCoveringNode();
			if (node == null) {
				return null;
			}
			ITypeBinding binding= null;
			if (node instanceof SimpleName) {
				SimpleName selector= (SimpleName) node;
				
				binding= getQualifier(selector);
				if (binding == null) {
					addLocalDeclarations(selector, flags);
					binding= ASTResolving.getBindingOfParentType(selector);
				}	
			} else {
				addLocalDeclarations(node, offset, flags);
				binding= ASTResolving.getBindingOfParentType(node);				
			}

			if (binding != null) {
				addTypeDeclarations(binding, flags);
			}
		
			return (IBinding[]) fRequestor.toArray(new IBinding[fRequestor.size()]);
		} finally {
			clearLists();			
		}
	}
	
	public IBinding[] getDeclarationsAfter(CompilationUnit root, int offset, int flags) {
		fRoot= root;
		try {		
			NodeFinder finder= new NodeFinder(offset, 0);
			root.accept(finder);
			ASTNode node= finder.getCoveringNode();
			if (node == null) {
				return null;
			}
			
			ASTNode declaration= ASTResolving.findParentStatement(node);
			while (declaration instanceof Statement && declaration.getNodeType() != ASTNode.BLOCK) {
				declaration= declaration.getParent();
			}

			if (declaration instanceof Block) {
				DeclarationsAfterVisitor visitor= new DeclarationsAfterVisitor(node.getStartPosition(), flags);
				declaration.accept(visitor);
			}
			return (IBinding[]) fRequestor.toArray(new IBinding[fRequestor.size()]);
		} finally {
			clearLists();			
		}
	}
	
	
	private class ScopeAnalyzerVisitor extends HierarchicalASTVisitor {
		
		private int fPosition;
		private int fFlags;
		
		public ScopeAnalyzerVisitor(int position, int flags) {
			fPosition= position;
			fFlags= flags;
		}
		
		private boolean isInside(ASTNode node) {
			int start= node.getStartPosition();
			int end= start + node.getLength();
					
			return start <= fPosition && fPosition < end;
		}
		
		public boolean visit(MethodDeclaration node) {
			return isInside(node);
		}
		
		public boolean visit(Initializer node) {
			return isInside(node);
		}		
		
		public boolean visit(Statement node) {
			return isInside(node);
		}
		
		public boolean visit(ASTNode node) {
			return false;
		}
		
		public boolean visit(Block node) {
			if (isInside(node)) {
				List list= node.statements();
				for (int i= 0; i < list.size(); i++) {
					ASTNode curr= (ASTNode) list.get(i);
					if (curr.getStartPosition() <  fPosition) {
						curr.accept(this);
					}
				}
			}
			return false;
		}		
		
		public boolean visit(VariableDeclaration node) {
			if (hasFlag(VARIABLES, fFlags) && node.getStartPosition() < fPosition) {
				IVariableBinding binding= node.resolveBinding();
				if (binding != null && fNamesAdded.add(getVariableSignature(binding))) { // only if not already defined
					fRequestor.add(binding);
				}							
			}
			return false;
		}
		
		public boolean visit(VariableDeclarationStatement node) {
			return true;
		}		
		
		public boolean visit(VariableDeclarationExpression node) {
			return true;
		}

		public boolean visit(CatchClause node) {
			return isInside(node);
		}

		public boolean visit(TypeDeclarationStatement node) {
			if (hasFlag(TYPES, fFlags) && node.getStartPosition() + node.getLength() < fPosition) {
				ITypeBinding binding= node.getTypeDeclaration().resolveBinding();
				if (binding != null && fNamesAdded.add(getTypeSignature(binding))) {
					fRequestor.add(binding);
				}
				return false;
			}
			return isInside(node);
		}
	}
	
	private class DeclarationsAfterVisitor extends HierarchicalASTVisitor {
		private int fPosition;
		private int fFlags;
		
		public DeclarationsAfterVisitor(int position, int flags) {
			fPosition= position;
			fFlags= flags;
		}
		
		public boolean visit(ASTNode node) {
			return true;
		}
		
		public boolean visit(VariableDeclaration node) {
			if (hasFlag(VARIABLES, fFlags) && fPosition < node.getStartPosition()) {
				IVariableBinding binding= node.resolveBinding();
				if (binding != null && fNamesAdded.add(getVariableSignature(binding))) {
					fRequestor.add(binding);
				}				
			}
			return false;
		}
		
		public boolean visit(AnonymousClassDeclaration node) {
			return false;
		}

		public boolean visit(TypeDeclarationStatement node) {
			if (hasFlag(TYPES, fFlags) && fPosition < node.getStartPosition()) {
				ITypeBinding binding= node.getTypeDeclaration().resolveBinding();
				if (binding != null && fNamesAdded.add(getTypeSignature(binding))) {
					fRequestor.add(binding);
				}
			}
			return false;
		}
	}
	
	private void addLocalDeclarations(ASTNode node, int flags) {
		addLocalDeclarations(node, node.getStartPosition(), flags);
	}
	
	
	private void addLocalDeclarations(ASTNode node, int offset, int flags) {
		if (hasFlag(VARIABLES, flags) || hasFlag(TYPES, flags)) {
			BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(node);
			if (declaration instanceof MethodDeclaration || declaration instanceof Initializer) {		
				ScopeAnalyzerVisitor visitor= new ScopeAnalyzerVisitor(offset, flags);
				declaration.accept(visitor);
			}
		}
	}
}
