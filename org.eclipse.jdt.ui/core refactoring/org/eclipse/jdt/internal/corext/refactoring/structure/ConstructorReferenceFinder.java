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
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

class ConstructorReferenceFinder {
	private final IType fType;
	private final IMethod[] fConstructors;
	private final ASTNodeMappingManager fASTManager;
	
	private ConstructorReferenceFinder(IType type, ASTNodeMappingManager astManager) throws JavaModelException{
		fConstructors= JavaElementUtil.getAllConstructors(type);
		fASTManager= astManager;
		fType= type;
	}
	
	private ConstructorReferenceFinder(IMethod constructor, ASTNodeMappingManager astManager){
		fConstructors= new IMethod[]{constructor};
		fASTManager= astManager;
		fType= constructor.getDeclaringType();
	}
	
	public static ASTNode[] getConstructorReferenceNodes(IType type, ASTNodeMappingManager astManager, IProgressMonitor pm) throws JavaModelException{
		return new ConstructorReferenceFinder(type, astManager).getConstructorReferenceNodes(pm, IJavaSearchConstants.REFERENCES);
	}

	public static ASTNode[] getConstructorOccurrenceNodes(IMethod constructor, ASTNodeMappingManager astManager, IProgressMonitor pm) throws JavaModelException{
		Assert.isTrue(constructor.isConstructor());
		return new ConstructorReferenceFinder(constructor, astManager).getConstructorReferenceNodes(pm, IJavaSearchConstants.ALL_OCCURRENCES);
	}
	
	private ASTNode[] getConstructorReferenceNodes(IProgressMonitor pm, int limitTo) throws JavaModelException{
		IJavaSearchScope scope= createSearchScope();
		ISearchPattern pattern= RefactoringSearchEngine.createSearchPattern(fConstructors, limitTo);
		if (pattern == null){
			if (fConstructors.length != 0)
				return new ASTNode[0];
			return getImplicitConstructorReferenceNodes(pm);	
		}	
		return removeUnrealNodes(ASTNodeSearchUtil.searchNodes(scope, pattern, fASTManager, pm));
	}

	//XXX this method is a work around for bug 27236
	private ASTNode[] removeUnrealNodes(ASTNode[] nodes) {
		List realNodes= new ArrayList(nodes.length);
		String typeName= fConstructors[0].getDeclaringType().getElementName();
		for (int i= 0; i < nodes.length; i++) {
			if (nodes[i].getParent() instanceof TypeDeclaration)
				continue;
			if (nodes[i].getParent() instanceof MethodDeclaration){
				MethodDeclaration md= (MethodDeclaration)nodes[i].getParent();
				if (md.isConstructor() && ! md.getName().getIdentifier().equals(typeName))
					continue;
			}	
			realNodes.add(nodes[i]);	
		}
		return (ASTNode[]) realNodes.toArray(new ASTNode[realNodes.size()]);
	}
	
	private IJavaSearchScope createSearchScope() throws JavaModelException{
		if (fConstructors.length ==0)
			return RefactoringScopeFactory.create(fType);
		return RefactoringScopeFactory.create(getMostVisibleConstructor());
	}
	
	private IMethod getMostVisibleConstructor() throws JavaModelException {
		Assert.isTrue(fConstructors.length > 0);
		IMethod candidate= fConstructors[0];
		int visibility= JdtFlags.getVisibilityCode(fConstructors[0]);
		for (int i= 1; i < fConstructors.length; i++) {
			IMethod constructor= fConstructors[i];
			if (JdtFlags.isHigherVisibility(JdtFlags.getVisibilityCode(constructor), visibility))
				candidate= constructor;
		}
		return candidate;
	}
	
	private ASTNode[] getImplicitConstructorReferenceNodes(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		List result= new ArrayList();
		result.addAll(getImplicitConstructorReferenceNodesFromHierarchy(new SubProgressMonitor(pm, 1)));
		result.addAll(getImplicitConstructorReferencesInClassCreations(new SubProgressMonitor(pm, 1)));
		pm.done();
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);
	}
	
	//List of ASTNodes
	private List getImplicitConstructorReferencesInClassCreations(IProgressMonitor pm) throws JavaModelException {
		//XXX workaround for bug 23112
		ASTNode[] nodes= ASTNodeSearchUtil.findReferenceNodes(fType, fASTManager, pm);
		List result= new ArrayList(2);
		for (int i= 0; i < nodes.length; i++) {
			ASTNode node= nodes[i];
			if (node instanceof Name && node.getParent() instanceof ClassInstanceCreation){
				ClassInstanceCreation cic= (ClassInstanceCreation)node.getParent();
				if (node.equals(cic.getName()))
					result.add(cic);
			}
		}
		return result;
	}

	//List of ASTNodes
	private List getImplicitConstructorReferenceNodesFromHierarchy(IProgressMonitor pm) throws JavaModelException{
		ITypeHierarchy hierarchy= fType.newTypeHierarchy(pm);
		IType[] subTypes= hierarchy.getAllSubtypes(fType);
		List result= new ArrayList(subTypes.length);
		for (int i= 0; i < subTypes.length; i++) {
			if (! subTypes[i].isBinary())
				result.addAll(getAllSuperConstructorInvocations(subTypes[i]));
		}
		return result;
	}

	//Collection of ASTNodes
	private Collection getAllSuperConstructorInvocations(IType type) throws JavaModelException {
		IMethod[] constructors= JavaElementUtil.getAllConstructors(type);
		List result= new ArrayList(constructors.length);
		for (int i= 0; i < constructors.length; i++) {
			ASTNode superCall= getSuperConstructorCall(constructors[i]);
			if (superCall != null)
				result.add(superCall);
		}
		return result;
	}

	private ASTNode getSuperConstructorCall(IMethod constructor) throws JavaModelException {
		Assert.isTrue(constructor.isConstructor());
		MethodDeclaration constructorNode= ASTNodeSearchUtil.getMethodDeclarationNode(constructor, fASTManager);
		Assert.isTrue(constructorNode.isConstructor());
		Block body= constructorNode.getBody();
		Assert.isNotNull(body);
		List statements= body.statements();
		if (! statements.isEmpty() && statements.get(0) instanceof SuperConstructorInvocation)
			return (SuperConstructorInvocation)statements.get(0);
		return null;
	}

}
