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
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.core.dom.*;

import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;


public class ImportReferencesCollector extends GenericVisitor {

	private Region fSubRange;
	private Collection/*<Name>*/ fTypeImports;
	private Collection/*<Name>*/ fStaticImports;

	public ImportReferencesCollector(IJavaProject project, Region rangeLimit, Collection resultingTypeImports, Collection resultingStaticImports) {
		super(true);
		fTypeImports= resultingTypeImports;
		fStaticImports= resultingStaticImports;
		fSubRange= rangeLimit;
		if (project == null || !JavaCore.VERSION_1_5.equals(project.getOption(JavaCore.COMPILER_SOURCE, true))) {
			fStaticImports= null; // do not collect
		}
	}
	
	private boolean isAffected(ASTNode node) {
		if (fSubRange == null) {
			return true;
		}
		int nodeStart= node.getStartPosition();
		return nodeStart + node.getLength() > fSubRange.getOffset() && (fSubRange.getOffset() + fSubRange.getLength()) >  nodeStart;
	}
	
	
	private void addReference(SimpleName name) {
		if (isAffected(name)) {
			fTypeImports.add(name);
		}
	}			
	
	private void typeRefFound(Name node) {
		if (node != null) {
			while (node.isQualifiedName()) {
				node= ((QualifiedName) node).getQualifier();
			}
			addReference((SimpleName) node);
		}
	}

	private void possibleTypeRefFound(Name node) {
		while (node.isQualifiedName()) {
			node= ((QualifiedName) node).getQualifier();
		}
		IBinding binding= node.resolveBinding();
		if (binding == null || binding.getKind() == IBinding.TYPE) {
			// if the binding is null, we cannot determine if 
			// we have a type binding or not, so we will assume
			// we do.
			addReference((SimpleName) node);
		}
	}
	
	private void possibleStaticImportFound(Name name) {
		if (fStaticImports == null) {
			return;
		}
		
		while (name.isQualifiedName()) {
			name= ((QualifiedName) name).getQualifier();
		}
		if (!isAffected(name)) {
			return;
		}
		
		IBinding binding= name.resolveBinding();
		if (binding == null || !Modifier.isStatic(binding.getModifiers()) || ((SimpleName) name).isDeclaration()) {
			return;
		}
		
		int flags= 0;
		ITypeBinding declaringClass= null;
		if (binding instanceof IVariableBinding) {
			IVariableBinding varBinding= (IVariableBinding) binding;
			if (varBinding.isField()) {
				declaringClass= varBinding.getDeclaringClass();
				flags= ScopeAnalyzer.VARIABLES;
			}
		} else if (binding instanceof IMethodBinding) {
			IMethodBinding methodBinding= (IMethodBinding) binding;
			declaringClass= methodBinding.getDeclaringClass();
			flags= ScopeAnalyzer.METHODS;
		}
		if (declaringClass != null && !declaringClass.isLocal()) {
			IBinding[] declarationsInScope= new ScopeAnalyzer((CompilationUnit) name.getRoot()).getDeclarationsInScope(name.getStartPosition(), flags);
			for (int i= 0; i < declarationsInScope.length; i++) {
				if (declarationsInScope[i] == binding) {
					return;
				}
			}
			fStaticImports.add(name);
		}
	}
	
	private void doVisitChildren(List elements) {
		int nElements= elements.size();
		for (int i= 0; i < nElements; i++) {
			((ASTNode) elements.get(i)).accept(this);
		}
	}
	
	private void doVisitNode(ASTNode node) {
		if (node != null) {
			node.accept(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.dom.GenericVisitor#visitNode(org.eclipse.jdt.core.dom.ASTNode)
	 */
	protected boolean visitNode(ASTNode node) {
		return isAffected(node);
	}
	
	/*
	 * @see ASTVisitor#visit(ArrayType)
	 */
	public boolean visit(ArrayType node) {
		doVisitNode(node.getElementType());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SimpleType)
	 */
	public boolean visit(SimpleType node) {
		typeRefFound(node.getName());
		return false;
	}
	
	/*
	 * @see ASTVisitor#visit(QualifiedType)
	 */
	public boolean visit(QualifiedType node) {
		// nothing to do here, let the qualifier be visited
		return true;
	}
	
	/*
	 * @see ASTVisitor#visit(QualifiedName)
	 */
	public boolean visit(QualifiedName node) {
		possibleTypeRefFound(node); // possible ref
		possibleStaticImportFound(node);
		return false;
	}		

	/*
	 * @see ASTVisitor#visit(ImportDeclaration)
	 */
	public boolean visit(ImportDeclaration node) {
		return false;
	}
	
	/*
	 * @see ASTVisitor#visit(PackageDeclaration)
	 */
	public boolean visit(PackageDeclaration node) {
		return false;
	}				

	/*
	 * @see ASTVisitor#visit(ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		typeRefFound(node.getQualifier());
		return false;
	}

	private void evalQualifyingExpression(Expression expr, Name selector) {
		if (expr != null) {
			if (expr instanceof Name) {
				Name name= (Name) expr;
				possibleTypeRefFound(name);
				possibleStaticImportFound(name);
			} else {
				expr.accept(this);
			}
		} else if (selector != null) {
			possibleStaticImportFound(selector);
		}
	}			

	/*
	 * @see ASTVisitor#visit(ClassInstanceCreation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		if (node.getAST().apiLevel() == AST.JLS2) {
			typeRefFound(node.getName());
		} else {
			doVisitNode(node.getType());
		}
		evalQualifyingExpression(node.getExpression(), null);
		if (node.getAnonymousClassDeclaration() != null) {
			node.getAnonymousClassDeclaration().accept(this);
		}
		doVisitChildren(node.arguments());
		return false;
	}

	/*
	 * @see ASTVisitor#endVisit(MethodInvocation)
	 */
	public boolean visit(MethodInvocation node) {
		evalQualifyingExpression(node.getExpression(), node.getName());
		doVisitChildren(node.arguments());
		return false;
	}

	/*
	 * @see ASTVisitor#visit(SuperConstructorInvocation)
	 */		
	public boolean visit(SuperConstructorInvocation node) {
		if (!isAffected(node)) {
			return false;
		}
		
		evalQualifyingExpression(node.getExpression(), null);
		doVisitChildren(node.arguments());
		return false;	
	}		

	/*
	 * @see ASTVisitor#visit(FieldAccess)
	 */
	public boolean visit(FieldAccess node) {
		evalQualifyingExpression(node.getExpression(), node.getName());
		return false;
	}
	
	/*
	 * @see ASTVisitor#visit(SimpleName)
	 */
	public boolean visit(SimpleName node) {
		// if the call gets here, it can only be a variable reference
		possibleStaticImportFound(node);
		return false;
	}

	/*
	 * @see ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		if (!isAffected(node)) {
			return false;
		}
		if (node.getAST().apiLevel() >= AST.JLS3) {
			return true;
		}
		// in JLS2 super class and interfaces are names
		doVisitNode(node.getJavadoc());
		
		typeRefFound(node.getSuperclass());

		Iterator iter= node.superInterfaces().iterator();
		while (iter.hasNext()) {
			typeRefFound((Name) iter.next());
		}
		doVisitChildren(node.bodyDeclarations());
		return false;
	}
	
	/*
	 * @see ASTVisitor#visit(MethodDeclaration)
	 */
	public boolean visit(MethodDeclaration node) {
		if (!isAffected(node)) {
			return false;
		}
		doVisitNode(node.getJavadoc());
		
		if (!node.isConstructor()) {
			if (node.getAST().apiLevel() == AST.JLS2) {
				doVisitNode(node.getReturnType());
			} else {
				doVisitNode(node.getReturnType2());
			}
		}
		doVisitChildren(node.parameters());
		Iterator iter=node.thrownExceptions().iterator();
		while (iter.hasNext()) {
			typeRefFound((Name) iter.next());
		}
		doVisitNode(node.getBody());
		return false;
	}
	
	public boolean visit(TagElement node) {
		String tagName= node.getTagName();
		List list= node.fragments();
		int idx= 0;
		if (tagName != null && !list.isEmpty()) {
			Object first= list.get(0);
			if (first instanceof Name) {
				if ("@throws".equals(tagName) || "@exception".equals(tagName)) {  //$NON-NLS-1$//$NON-NLS-2$
					typeRefFound((Name) first);
				} else if ("@see".equals(tagName) || "@link".equals(tagName) || "@linkplain".equals(tagName)) {  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					Name name= (Name) first;
					possibleTypeRefFound(name);
					possibleStaticImportFound(name);
				}
				idx++;
			}
		}
		for (int i= idx; i < list.size(); i++) {
			doVisitNode((ASTNode) list.get(i));
		}
		return false;
	}
	
	public boolean visit(MemberRef node) {
		Name qualifier= node.getQualifier();
		if (qualifier != null) {
			typeRefFound(qualifier);
		} else {
			SimpleName name= node.getName();
			if (name != null) {
				possibleStaticImportFound(name);
			}
		}
		return false;
	}
	
	public boolean visit(MethodRef node) {
		Name qualifier= node.getQualifier();
		if (qualifier != null) {
			typeRefFound(qualifier);
		} else {
			SimpleName name= node.getName();
			if (name != null) {
				possibleStaticImportFound(name);
			}
		}
		List list= node.parameters();
		if (list != null) {
			doVisitChildren(list); // visit MethodRefParameter with Type
		}
		return false;
	}
}