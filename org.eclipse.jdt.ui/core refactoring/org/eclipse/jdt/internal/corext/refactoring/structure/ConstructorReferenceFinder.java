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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

/**
 * This class is used to find references to construcotors.
 */
class ConstructorReferenceFinder {
	private final IType fType;
	private final IMethod[] fConstructors;
	private ASTNodeMappingManager fASTManager;
	
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

	private ConstructorReferenceFinder(IMethod constructor){
		fConstructors= new IMethod[]{constructor};
		fType= constructor.getDeclaringType();
	}
	
	public static ASTNode[] getConstructorReferenceNodes(IType type, ASTNodeMappingManager astManager, IProgressMonitor pm) throws JavaModelException{
		return new ConstructorReferenceFinder(type, astManager).getConstructorReferenceNodes(pm, IJavaSearchConstants.REFERENCES);
	}

	public static SearchResultGroup[] getConstructorOccurrences(IMethod constructor, IProgressMonitor pm) throws JavaModelException{
		Assert.isTrue(constructor.isConstructor());
		return new ConstructorReferenceFinder(constructor).getConstructorReferences(pm, IJavaSearchConstants.ALL_OCCURRENCES);
	}

	public static ASTNode[] getConstructorOccurrenceNodes(IMethod constructor, ASTNodeMappingManager astManager, IProgressMonitor pm) throws JavaModelException{
		Assert.isTrue(constructor.isConstructor());
		return new ConstructorReferenceFinder(constructor, astManager).getConstructorReferenceNodes(pm, IJavaSearchConstants.ALL_OCCURRENCES);
	}

	private SearchResultGroup[] getConstructorReferences(IProgressMonitor pm, int limitTo) throws JavaModelException{
		IJavaSearchScope scope= createSearchScope();
		ISearchPattern pattern= RefactoringSearchEngine.createSearchPattern(fConstructors, limitTo);
		if (pattern == null){
			if (fConstructors.length != 0)
				return new SearchResultGroup[0];
			return getImplicitConstructorReferences(pm);	
		}	
		return removeUnrealReferences(RefactoringSearchEngine.search(pm, scope, pattern));
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

	//XXX this method is a workaround for jdt core bug 27236
	private SearchResultGroup[] removeUnrealReferences(SearchResultGroup[] groups) {
		List result= new ArrayList(groups.length);
		for (int i= 0; i < groups.length; i++) {
			SearchResultGroup group= groups[i];
			ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(group.getCompilationUnit());
			if (cu == null)
				continue;
			CompilationUnit cuNode= AST.parseCompilationUnit(cu, false);
			SearchResult[] allSearchResults= group.getSearchResults();
			List realConstructorReferences= new ArrayList(Arrays.asList(allSearchResults));
			for (int j= 0; j < allSearchResults.length; j++) {
				SearchResult searchResult= allSearchResults[j];
				if (! isRealConstructorReferenceNode(ASTNodeSearchUtil.getAstNode(searchResult, cuNode)))
					realConstructorReferences.remove(searchResult);
			}
			if (! realConstructorReferences.isEmpty())
				result.add(new SearchResultGroup(group.getResource(), (SearchResult[]) realConstructorReferences.toArray(new SearchResult[realConstructorReferences.size()])));
		}
		return (SearchResultGroup[]) result.toArray(new SearchResultGroup[result.size()]);
	}

	//XXX this method is a workaround for jdt core bug 27236
	private ASTNode[] removeUnrealNodes(ASTNode[] nodes) {
		List realNodes= new ArrayList(nodes.length);
		for (int i= 0; i < nodes.length; i++) {
			if (isRealConstructorReferenceNode(nodes[i]))
				realNodes.add(nodes[i]);
		}
		return (ASTNode[]) realNodes.toArray(new ASTNode[realNodes.size()]);
	}
	
	//XXX this method is a workaround for jdt core bug 27236
	private boolean isRealConstructorReferenceNode(ASTNode node){
		String typeName= fConstructors[0].getDeclaringType().getElementName();
		if (node.getParent() instanceof TypeDeclaration)
			return false;
		if (node.getParent() instanceof MethodDeclaration){
			MethodDeclaration md= (MethodDeclaration)node.getParent();
			if (md.isConstructor() && ! md.getName().getIdentifier().equals(typeName))
				return false;
		}
		return true;
	}
	
	private IJavaSearchScope createSearchScope() throws JavaModelException{
		if (fConstructors.length == 0)
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

	private SearchResultGroup[] getImplicitConstructorReferences(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		List searchResults= new ArrayList();
		searchResults.addAll(getImplicitConstructorReferencesFromHierarchy(new SubProgressMonitor(pm, 1)));
		searchResults.addAll(getImplicitConstructorReferencesInClassCreations(new SubProgressMonitor(pm, 1)));
		pm.done();
		return RefactoringSearchEngine.groupByResource((SearchResult[]) searchResults.toArray(new SearchResult[searchResults.size()]));
	}
		
	private ASTNode[] getImplicitConstructorReferenceNodes(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		List result= new ArrayList();
		result.addAll(getImplicitConstructorReferenceNodesFromHierarchy(new SubProgressMonitor(pm, 1)));
		result.addAll(getImplicitConstructorReferenceNodesInClassCreations(new SubProgressMonitor(pm, 1)));
		pm.done();
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);
	}

	//List of SearchResults
	private List getImplicitConstructorReferencesInClassCreations(IProgressMonitor pm) throws JavaModelException {
		//XXX workaround for jdt core bug 23112
		Assert.isTrue(fASTManager == null);
		ISearchPattern pattern= SearchEngine.createSearchPattern(fType, IJavaSearchConstants.REFERENCES);
		IJavaSearchScope scope= RefactoringScopeFactory.create(fType);
		SearchResultGroup[] refs= RefactoringSearchEngine.search(pm, scope, pattern);
		List result= new ArrayList();
		for (int i= 0; i < refs.length; i++) {
			SearchResultGroup group= refs[i];
			ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(group.getCompilationUnit());
			if (cu == null)
				continue;
			CompilationUnit cuNode= AST.parseCompilationUnit(cu, false);
			SearchResult[] results= group.getSearchResults();
			for (int j= 0; j < results.length; j++) {
				SearchResult searchResult= results[j];
				ASTNode node= ASTNodeSearchUtil.getAstNode(searchResult, cuNode);
				if (isImplicitConstructorReferenceNodeInClassCreations(node))
					result.add(searchResult);
			}
		}
		return result;
	}
	
	//List of ASTNodes
	private List getImplicitConstructorReferenceNodesInClassCreations(IProgressMonitor pm) throws JavaModelException {
		//XXX workaround for jdt core bug 23112
		return getImplicitConstructorReferenceNodesInClassCreations(pm, fType, fASTManager);
	}
	
	//List of ASTNodes
	private static List getImplicitConstructorReferenceNodesInClassCreations(IProgressMonitor pm, IType type, ASTNodeMappingManager astManager) throws JavaModelException {
		ASTNode[] nodes= ASTNodeSearchUtil.findReferenceNodes(type, astManager, pm);
		List result= new ArrayList(2);
		for (int i= 0; i < nodes.length; i++) {
			ASTNode node= nodes[i];
			if (isImplicitConstructorReferenceNodeInClassCreations(node))
				result.add(node.getParent());
		}
		return result;
	}

	private static boolean isImplicitConstructorReferenceNodeInClassCreations(ASTNode node){
		if (node instanceof Name && node.getParent() instanceof ClassInstanceCreation){
			ClassInstanceCreation cic= (ClassInstanceCreation)node.getParent();
			return (node.equals(cic.getName()));
		}
		return false;
	}

	//List of SearchResults
	private List getImplicitConstructorReferencesFromHierarchy(IProgressMonitor pm) throws JavaModelException{
		IType[] subTypes= getNonBinarySubtypes(fType, pm);
		List result= new ArrayList(subTypes.length);
		for (int i= 0; i < subTypes.length; i++) {
			result.addAll(getAllSuperConstructorInvocations(subTypes[i]));
		}
		return result;
	}

	//List of ASTNodes
	private List getImplicitConstructorReferenceNodesFromHierarchy(IProgressMonitor pm) throws JavaModelException{
		IType[] subTypes= getNonBinarySubtypes(fType, pm);
		List result= new ArrayList(subTypes.length);
		for (int i= 0; i < subTypes.length; i++) {
			result.addAll(getAllSuperConstructorInvocationNodes(subTypes[i]));
		}
		return result;
	}
	
	private static IType[] getNonBinarySubtypes(IType type, IProgressMonitor pm) throws JavaModelException{
		ITypeHierarchy hierarchy= type.newTypeHierarchy(pm);
		IType[] subTypes= hierarchy.getAllSubtypes(type);
		List result= new ArrayList(subTypes.length);
		for (int i= 0; i < subTypes.length; i++) {
			if (! subTypes[i].isBinary())
				result.add(subTypes[i]);
		}
		return (IType[]) result.toArray(new IType[result.size()]);
	}

	//Collection of ASTNodes
	private Collection getAllSuperConstructorInvocationNodes(IType type) throws JavaModelException {
		IMethod[] constructors= JavaElementUtil.getAllConstructors(type);
		List result= new ArrayList(constructors.length);
		for (int i= 0; i < constructors.length; i++) {
			ASTNode superCall= getSuperConstructorCallNode(constructors[i]);
			if (superCall != null)
				result.add(superCall);
		}
		return result;
	}
	
	//Collection of SearchResults
	private static Collection getAllSuperConstructorInvocations(IType type) throws JavaModelException {
		IMethod[] constructors= JavaElementUtil.getAllConstructors(type);
		CompilationUnit cuNode= AST.parseCompilationUnit(type.getCompilationUnit(), false);
		List result= new ArrayList(constructors.length);
		for (int i= 0; i < constructors.length; i++) {
			ASTNode superCall= getSuperConstructorCallNode(constructors[i], cuNode);
			if (superCall != null)
				result.add(createSearchResult(superCall, constructors[i]));
		}
		return result;
	}

	private static SearchResult createSearchResult(ASTNode superCall, IMethod constructor) {
		return new SearchResult(constructor.getResource(), superCall.getStartPosition(), ASTNodes.getInclusiveEnd(superCall), constructor, IJavaSearchResultCollector.EXACT_MATCH);
	}

	private static SuperConstructorInvocation getSuperConstructorCallNode(IMethod constructor, CompilationUnit cuNode) throws JavaModelException {
		Assert.isTrue(constructor.isConstructor());
		MethodDeclaration constructorNode= ASTNodeSearchUtil.getMethodDeclarationNode(constructor, cuNode);
		Assert.isTrue(constructorNode.isConstructor());
		Block body= constructorNode.getBody();
		Assert.isNotNull(body);
		List statements= body.statements();
		if (! statements.isEmpty() && statements.get(0) instanceof SuperConstructorInvocation)
			return (SuperConstructorInvocation)statements.get(0);
		return null;
	}
	
	private SuperConstructorInvocation getSuperConstructorCallNode(IMethod constructor) throws JavaModelException {
		return getSuperConstructorCallNode(constructor, fASTManager.getAST(constructor.getCompilationUnit()));
	}
}