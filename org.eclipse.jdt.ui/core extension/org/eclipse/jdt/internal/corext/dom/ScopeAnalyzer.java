/*
 * Created on 10.04.2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.eclipse.jdt.internal.corext.dom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

/**
 * @author aes
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
public class ScopeAnalyzer {
	
	public static final int METHODS= 1;
	public static final int VARIABLES= 2;
	public static final int TYPES= 4;
	
	private ArrayList fRequestor;
	private HashSet fMethodAdded;
	private HashSet fTypesVisited;
	
	private int fFlags;
	private CompilationUnit fRoot;
	
	public ScopeAnalyzer() {
		fRequestor= new ArrayList();
		fMethodAdded= new HashSet();
		fTypesVisited= new HashSet();	
	}
	
	private void clearLists() {
		fRequestor.clear();
		fMethodAdded.clear();
		fTypesVisited.clear();
		
		fRoot= null;
	}
	
	private static String getMethodSignature(IMethodBinding binding) {
		StringBuffer buf= new StringBuffer();
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
	
	private boolean hasFlag(int flag) {
		return (fFlags & flag) != 0;
	}
	
	
	private void addTypeDeclarations(ITypeBinding binding) {
		if (!fTypesVisited.add(binding)) {
			return;
		}
		
		if (hasFlag(VARIABLES)) {
			IVariableBinding[] variableBindings= binding.getDeclaredFields();
			for (int i= 0; i < variableBindings.length; i++) {
				fRequestor.add(variableBindings[i]);
			}
		}
		
		if (hasFlag(METHODS)) {
			IMethodBinding[] methodBindings= binding.getDeclaredMethods();
			for (int i= 0; i < methodBindings.length; i++) {
				IMethodBinding curr= methodBindings[i];
				String signature= getMethodSignature(curr);
				if (fMethodAdded.add(signature)) {
					fRequestor.add(curr);
				}
			}
		}
		
		if (hasFlag(TYPES)) {
			ITypeBinding[] typeBindings= binding.getDeclaredTypes();
			for (int i= 0; i < typeBindings.length; i++) {
				fRequestor.add(typeBindings[i]);
			}
		}
	
		ITypeBinding superClass= binding.getSuperclass();
		if (superClass != null) {
			addTypeDeclarations(superClass); // recursive
		}
		
		ITypeBinding[] interfaces= binding.getInterfaces();
		for (int i= 0; i < interfaces.length; i++) {
			addTypeDeclarations(interfaces[i]); // recursive
		}
		
		if (binding.isLocal()) {
			addOuterDeclarationsForLocalType(binding);
		} else {
			ITypeBinding declaringClass= binding.getDeclaringClass();
			if (declaringClass != null) {
				addTypeDeclarations(declaringClass); // recursive
			}			
		}
	}
	
	private void addOuterDeclarationsForLocalType(ITypeBinding localBinding) {
		ASTNode node= fRoot.findDeclaringNode(localBinding);
		if (node == null) {
			return;
		}
		
		if (node instanceof TypeDeclaration || node instanceof AnonymousClassDeclaration) {
			addLocalDeclarations(node.getParent());
			
			ITypeBinding parentTypeBidning= ASTResolving.getBindingOfParentType(node.getParent());
			if (parentTypeBidning != null) {
				addTypeDeclarations(parentTypeBidning);
			}
			
		}
	}
	
		
	private static Expression getQualifier(SimpleName selector) {
		ASTNode parent= selector.getParent();
		switch (parent.getNodeType()) {
			case ASTNode.METHOD_INVOCATION:
				return ((MethodInvocation) parent).getExpression();
			case ASTNode.QUALIFIED_NAME:
				return ((QualifiedName) parent).getName();
			case ASTNode.FIELD_ACCESS:
				return ((FieldAccess) parent).getName();
			case ASTNode.SUPER_FIELD_ACCESS:
				return ((SuperFieldAccess) parent).getName();
			case ASTNode.SUPER_METHOD_INVOCATION:
				return ((SuperMethodInvocation) parent).getName();				
			default:
				return null;
		}
	}
	
	public IBinding[] getDeclarationsInScope(CompilationUnit root, int offset, int flags) {
		fFlags= flags;
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
				
				Expression qualifier= getQualifier(selector);
				if (qualifier != null) {
					binding= qualifier.resolveTypeBinding();
				} else {
					addLocalDeclarations(selector);
					binding= ASTResolving.getBindingOfParentType(selector);
				}	
			} else {
				addLocalDeclarations(node);
				binding= ASTResolving.getBindingOfParentType(node);				
			}

			if (binding != null) {
				addTypeDeclarations(binding);
			}
		
			return (IBinding[]) fRequestor.toArray(new IBinding[fRequestor.size()]);
		} finally {
			clearLists();			
		}
	}
	
	
	private class ScopeAnalyzerVisitor extends HierarchicalASTVisitor {
		
		private int fPosition; 
		
		public ScopeAnalyzerVisitor(int position) {
			fPosition= position;
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
			if (hasFlag(VARIABLES) && node.getStartPosition() < fPosition) {
				IVariableBinding binding= node.resolveBinding();
				if (binding != null) {
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
			if (hasFlag(TYPES) && node.getStartPosition() + node.getLength() < fPosition) {
				ITypeBinding binding= node.getTypeDeclaration().resolveBinding();
				if (binding != null && fTypesVisited.add(binding)) {
					fRequestor.add(binding);
				}
				return false;
			}
			return isInside(node);
		}

	}	
	
	private void addLocalDeclarations(ASTNode node) {
		if (hasFlag(VARIABLES) || hasFlag(TYPES)) {
			BodyDeclaration declaration= ASTResolving.findParentBodyDeclaration(node);
			if (declaration instanceof MethodDeclaration || declaration instanceof Initializer) {		
				ScopeAnalyzerVisitor visitor= new ScopeAnalyzerVisitor(node.getStartPosition());
				declaration.accept(visitor);
			}
		}
	}
}
