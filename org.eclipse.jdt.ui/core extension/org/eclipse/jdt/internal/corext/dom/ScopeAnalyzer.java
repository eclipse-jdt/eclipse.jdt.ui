/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

/**
 * Evaluates all fields, methods and types available (declared) at a given offset
 * in a compilation unit (Code assist that returns IBindings)
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
	
	/**
	 * Flag to specify that only visible elements should be added.
	 */		
	public static final int CHECK_VISIBILITY= 16;
		
	private ArrayList fRequestor;
	private HashSet fNamesAdded;
	private HashSet fTypesVisited;
	
	private CompilationUnit fRoot;
	
	public ScopeAnalyzer(CompilationUnit root) {
		fRequestor= new ArrayList();
		fNamesAdded= new HashSet();
		fTypesVisited= new HashSet();
		fRoot= root;
	}
	
	protected void addResult(IBinding binding) {
		String signature= getSignature(binding);
		if (signature != null && fNamesAdded.add(signature)) { // avoid duplicated results from inheritance
			fRequestor.add(binding);
		}			
	}
	
	private void clearLists() {
		fRequestor.clear();
		fNamesAdded.clear();
		fTypesVisited.clear();
	}
	
	private static String getSignature(IBinding binding) {
		if (binding != null) {
			switch (binding.getKind()) {
				case IBinding.METHOD:
					StringBuffer buf= new StringBuffer();
					buf.append('M');
					buf.append(binding.getName()).append('(');
					ITypeBinding[] parameters= ((IMethodBinding) binding).getParameterTypes();
					for (int i= 0; i < parameters.length; i++) {
						if (i > 0) {
							buf.append(',');
						}
						buf.append(parameters[i].getQualifiedName());
					}
					buf.append(')');
					return buf.toString();
				case IBinding.VARIABLE:
					return 'V' + binding.getName();
				case IBinding.TYPE:
					return 'T' + binding.getName();			
			}
		}
		return null;
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
				addResult(variableBindings[i]);
			}
		}
		
		if (hasFlag(METHODS, flags)) {
			IMethodBinding[] methodBindings= binding.getDeclaredMethods();
			for (int i= 0; i < methodBindings.length; i++) {
				IMethodBinding curr= methodBindings[i];
				if (!curr.isSynthetic() && !curr.isConstructor()) {
					addResult(curr);		
				}
			}
		}

		if (hasFlag(TYPES, flags)) {
			ITypeBinding[] typeBindings= binding.getDeclaredTypes();
			for (int i= 0; i < typeBindings.length; i++) {
				ITypeBinding curr= typeBindings[i];
				addResult(curr);			
			}
		}		
		
		
		ITypeBinding superClass= binding.getSuperclass();
		if (superClass != null) {
			addInherited(superClass, flags); // recursive
		} else if (binding.isArray()) {
			addInherited(fRoot.getAST().resolveWellKnownType("java.lang.Object"), flags); //$NON-NLS-1$
		}
		
		if (hasFlag(TYPES | VARIABLES, flags)) {
			ITypeBinding[] interfaces= binding.getInterfaces();
			for (int i= 0; i < interfaces.length; i++) {
				addInherited(interfaces[i], flags & (TYPES | VARIABLES)); // recursive
			}			
		}
	}
		
	
	private void addTypeDeclarations(ITypeBinding binding, int flags) {
		if (hasFlag(TYPES, flags) && !binding.isAnonymous()) {
			addResult(binding);
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
						addResult(decl.resolveBinding());
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
			
			ITypeBinding parentTypeBinding= Bindings.getBindingOfParentType(node.getParent());
			if (parentTypeBinding != null) {
				addTypeDeclarations(parentTypeBinding, flags);
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
				MethodInvocation decl= (MethodInvocation) parent;
				if (selector == decl.getName()) {
					return getBinding(decl.getExpression());
				}
				return null;
			case ASTNode.QUALIFIED_NAME:
				QualifiedName qualifiedName= (QualifiedName) parent;
				if (selector == qualifiedName.getName()) {
					return getBinding(qualifiedName.getQualifier());
				}
				return null;
			case ASTNode.FIELD_ACCESS:
				FieldAccess fieldAccess= (FieldAccess) parent;
				if (selector == fieldAccess.getName()) {
					return getBinding(fieldAccess.getExpression());
				}
				return null;			
			case ASTNode.SUPER_FIELD_ACCESS:
			case ASTNode.SUPER_METHOD_INVOCATION:
				ITypeBinding curr= Bindings.getBindingOfParentType(parent);
				return curr.getSuperclass();
			default:
				return null;
		}
	}
	
	public IBinding[] getDeclarationsInScope(SimpleName selector, int flags) {
		try {
			ITypeBinding parentTypeBinding= Bindings.getBindingOfParentType(selector);
			
			ITypeBinding binding= getQualifier(selector);
			if (binding == null) {
				addLocalDeclarations(selector, flags);
				binding= parentTypeBinding;
			}
			if (binding != null) {
				addTypeDeclarations(binding, flags);
			}
			if (hasFlag(CHECK_VISIBILITY, flags)) {
				firterNonVisible(parentTypeBinding);
			}
			return (IBinding[]) fRequestor.toArray(new IBinding[fRequestor.size()]);
		} finally {
			clearLists();			
		}
	}		
	
	public IBinding[] getDeclarationsInScope(int offset, int flags) {
		NodeFinder finder= new NodeFinder(offset, 0);
		fRoot.accept(finder);
		ASTNode node= finder.getCoveringNode();
		if (node == null) {
			return null;
		}

		if (node instanceof SimpleName) {
			return getDeclarationsInScope((SimpleName) node, flags);
		}
		
		try {
			addLocalDeclarations(node, offset, flags);
			ITypeBinding binding= Bindings.getBindingOfParentType(node);				
			if (binding != null) {
				addTypeDeclarations(binding, flags);
			}
			
			if (hasFlag(CHECK_VISIBILITY, flags)) {
				firterNonVisible(binding);
			}
			return (IBinding[]) fRequestor.toArray(new IBinding[fRequestor.size()]);
		} finally {
			clearLists();			
		}
	}
	
	private void firterNonVisible(ITypeBinding binding) {
		// remove non-visible declarations
		for (int i= fRequestor.size() - 1; i >= 0; i--) {
			if (!isVisible((IBinding) fRequestor.get(i), binding)) {
				fRequestor.remove(i);
			}
		}
	}

	private static ITypeBinding getDeclaringType(IBinding binding) {
		switch (binding.getKind()) {
			case IBinding.VARIABLE:
				return ((IVariableBinding) binding).getDeclaringClass();
			case IBinding.METHOD:
				return ((IMethodBinding) binding).getDeclaringClass();
			case IBinding.TYPE:
				ITypeBinding typeBinding= (ITypeBinding) binding;
				if (typeBinding.getDeclaringClass() != null) {
					return typeBinding;
				}
				return typeBinding;
		}
		return null;
	}
	
	/**
	 * Evaluates if the declaration is visible in a certain context. 
	 * @param binding The binding of the declaration to examine
	 * @param context The context to test in
	 * @return Returns 
	 */
	public static boolean isVisible(IBinding binding, ITypeBinding context) {
		if (binding.getKind() == IBinding.VARIABLE && !((IVariableBinding) binding).isField()) {
			return true; // all local variables found are visible
		}
		ITypeBinding declaring= getDeclaringType(binding);
		if (declaring == null) {
			return false;
		}
	
		int modifiers= binding.getModifiers();
		if (Modifier.isPublic(modifiers) || declaring.isInterface()) {
			return true;
		} else if (Modifier.isProtected(modifiers) || !Modifier.isPrivate(modifiers)) {
			if (declaring != null && declaring.getPackage() == context.getPackage()) {
				return true;
			}
			return isTypeInScope(declaring, context, Modifier.isProtected(modifiers));
		}
		// private visibility
		return isTypeInScope(declaring, context, false);
	}
	
	private static boolean isTypeInScope(ITypeBinding declaring, ITypeBinding context, boolean includeHierarchy) {
		ITypeBinding curr= context;
		while (curr != null && curr != declaring) {
			if (includeHierarchy && Bindings.isSuperType(declaring, curr)) {
				return true;
			}
			curr= curr.getDeclaringClass();
		}
		return curr == declaring;
	}
	

	public IBinding[] getDeclarationsAfter(int offset, int flags) {
		try {		
			NodeFinder finder= new NodeFinder(offset, 0);
			fRoot.accept(finder);
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
			if (isInside(node)) {
				Block body= node.getBody();
				if (body != null) {
					body.accept(this);
				}
				visitBackwards(node.parameters());
				if (node.getAST().apiLevel() >= AST.JLS3) {
					visitBackwards(node.typeParameters());
				}
			}
			return false;
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
				visitBackwards(node.statements());
			}
			return false;
		}		
		
		public boolean visit(VariableDeclaration node) {
			if (hasFlag(VARIABLES, fFlags) && node.getStartPosition() < fPosition) {
				addResult(node.resolveBinding());					
			}
			return false;
		}
		
		public boolean visit(VariableDeclarationStatement node) {
			visitBackwards(node.fragments());
			return false;
		}		
		
		public boolean visit(VariableDeclarationExpression node) {
			visitBackwards(node.fragments());
			return false;
		}
	
		public boolean visit(CatchClause node) {
			if (isInside(node)) {
				node.getBody().accept(this);
				node.getException().accept(this);
			}
			return false;			
		}
		
		public boolean visit(ForStatement node) {
			if (isInside(node)) {
				node.getBody().accept(this);
				visitBackwards(node.initializers());
			}
			return false;
		}	
	
		public boolean visit(TypeDeclarationStatement node) {
			if (hasFlag(TYPES, fFlags) && node.getStartPosition() + node.getLength() < fPosition) {
				if (node.getAST().apiLevel() == AST.JLS2) {
					addResult(node.getTypeDeclaration().resolveBinding());
				} else {
					addResult(node.getDeclaration().getName().resolveBinding());
				}
				return false;
			}
			return isInside(node);
		}
		
		private void visitBackwards(List list) {
			for (int i= list.size() - 1; i >= 0; i--) {
				ASTNode curr= (ASTNode) list.get(i);
				if (curr.getStartPosition() <  fPosition) {
					curr.accept(this);
				}
			}			
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
				addResult(node.resolveBinding());		
			}
			return false;
		}
		
		public boolean visit(AnonymousClassDeclaration node) {
			return false;
		}

		public boolean visit(TypeDeclarationStatement node) {
			if (hasFlag(TYPES, fFlags) && fPosition < node.getStartPosition()) {
				addResult(node.getTypeDeclaration().resolveBinding());
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
