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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;

public class ASTNodeSearchUtil {

	private ASTNodeSearchUtil() {
	}

	public static ASTNode[] findReferenceNodes(IJavaElement[] elements, ASTNodeMappingManager astManager, IProgressMonitor pm, IJavaSearchScope scope) throws JavaModelException{
		ISearchPattern pattern= RefactoringSearchEngine.createSearchPattern(elements, IJavaSearchConstants.REFERENCES);
		return searchNodes(scope, pattern, astManager, pm);
	}
	
//	public static ASTNode[] findOccurrenceNodes(IJavaElement[] elements, ASTNodeMappingManager astManager, IProgressMonitor pm, IJavaSearchScope scope) throws JavaModelException{
//		ISearchPattern pattern= RefactoringSearchEngine.createSearchPattern(elements, IJavaSearchConstants.ALL_OCCURRENCES);
//		return searchNodes(scope, pattern, astManager, pm);
//	}
	
	public static ASTNode[] findReferenceNodes(IJavaElement element, ASTNodeMappingManager astManager, IProgressMonitor pm) throws JavaModelException{
		ISearchPattern pattern= SearchEngine.createSearchPattern(element, IJavaSearchConstants.REFERENCES);
		IJavaSearchScope scope= RefactoringScopeFactory.create(element);
		return searchNodes(scope, pattern, astManager, pm);
	}
	
	public static ASTNode[] searchNodes(IJavaSearchScope scope, ISearchPattern pattern, ASTNodeMappingManager astManager, IProgressMonitor pm) throws JavaModelException{
		SearchResultGroup[] searchResultGroups= RefactoringSearchEngine.search(pm, scope, pattern);
		return getAstNodes(searchResultGroups, astManager);	
	}

	public static ASTNode[] getAstNodes(SearchResultGroup[] searchResultGroups, ASTNodeMappingManager astManager) {
		List result= new ArrayList();
		for (int i= 0; i < searchResultGroups.length; i++) {
			ICompilationUnit referencedCu= searchResultGroups[i].getCompilationUnit();
			if (referencedCu == null)
				continue;
			result.addAll(Arrays.asList(getAstNodes(searchResultGroups[i].getSearchResults(), astManager.getAST(referencedCu))));
		}
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);	
	}	
	
	public static ASTNode[] getAstNodes(SearchResult[] searchResults, CompilationUnit cuNode) {
		List result= new ArrayList(searchResults.length);
		for (int i= 0; i < searchResults.length; i++) {
			ASTNode node= getAstNode(searchResults[i], cuNode);
			if (node != null)
				result.add(node);
		}
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);
	}

	public static ASTNode getAstNode(SearchResult searchResult, CompilationUnit cuNode) {
		ASTNode selectedNode= getAstNode(cuNode, searchResult.getStart(), searchResult.getEnd() - searchResult.getStart());
		if (selectedNode == null)
			return null;
		if (selectedNode.getParent() == null)
			return null;
		return selectedNode;
	}

	private static ASTNode getAstNode(CompilationUnit cuNode, int start, int length){
		SelectionAnalyzer analyzer= new SelectionAnalyzer(Selection.createFromStartLength(start, length), true);
		cuNode.accept(analyzer);
		//XXX workaround for jdt core feature 23527
		ASTNode node= analyzer.getFirstSelectedNode();
		if (node == null && analyzer.getLastCoveringNode() instanceof SuperConstructorInvocation)
			node= analyzer.getLastCoveringNode().getParent();
		else if (node == null && analyzer.getLastCoveringNode() instanceof ConstructorInvocation)
			node= analyzer.getLastCoveringNode().getParent();
		
		if (node == null)	
			return null;
		
		ASTNode parentNode= node.getParent();

		if (parentNode instanceof MethodDeclaration){
			MethodDeclaration md= (MethodDeclaration)parentNode;
			if (!(node instanceof SimpleName)
				&& md.isConstructor()
			    && md.getBody() != null
			    && md.getBody().statements().size() > 0 
			    &&(md.getBody().statements().get(0) instanceof ConstructorInvocation || md.getBody().statements().get(0) instanceof SuperConstructorInvocation)
			    &&((ASTNode)md.getBody().statements().get(0)).getLength() == length + 1)
			return (ASTNode)md.getBody().statements().get(0);
		}

		if (parentNode instanceof SuperConstructorInvocation){
			if (parentNode.getLength() == length + 1)
				return parentNode;
		}
		if (parentNode instanceof ConstructorInvocation){
			if (parentNode.getLength() == length + 1)
				return parentNode;
		}
		return node;
	}

	public static MethodDeclaration getMethodDeclarationNode(IMethod iMethod, CompilationUnit cuNode) throws JavaModelException {
		return (MethodDeclaration)getDeclarationNode(iMethod, cuNode);
	}

	public static MethodDeclaration getMethodDeclarationNode(IMethod iMethod, ASTNodeMappingManager astManager) throws JavaModelException {
		return getMethodDeclarationNode(iMethod, astManager.getAST(iMethod.getCompilationUnit()));
	}

	public static VariableDeclarationFragment getFieldDeclarationFragmentNode(IField iField, CompilationUnit cuNode) throws JavaModelException {
		ASTNode node= getNameNode(iField, cuNode);
		if (node instanceof VariableDeclarationFragment)
			return  (VariableDeclarationFragment)node;
		return (VariableDeclarationFragment)ASTNodes.getParent(node, VariableDeclarationFragment.class);
	}
		
	public static VariableDeclarationFragment getFieldDeclarationFragmentNode(IField iField, ASTNodeMappingManager astManager) throws JavaModelException {
		ASTNode node= getNameNode(iField, astManager);
		if (node instanceof VariableDeclarationFragment)
			return  (VariableDeclarationFragment)node;
		return (VariableDeclarationFragment)ASTNodes.getParent(node, VariableDeclarationFragment.class);
	}
	
	public static FieldDeclaration getFieldDeclarationNode(IField iField, ASTNodeMappingManager astManager) throws JavaModelException {
		return getFieldDeclarationNode(iField, astManager.getAST(iField.getCompilationUnit()));
	}

	public static FieldDeclaration getFieldDeclarationNode(IField iField, CompilationUnit cuNode) throws JavaModelException {
		return (FieldDeclaration)getDeclarationNode(iField, cuNode);
	}

	public static TypeDeclaration getTypeDeclarationNode(IType iType, CompilationUnit cuNode) throws JavaModelException {
		return (TypeDeclaration)getDeclarationNode(iType, cuNode);
	}

	public static TypeDeclaration getTypeDeclarationNode(IType iType, ASTNodeMappingManager astManager) throws JavaModelException {
		return getTypeDeclarationNode(iType, astManager.getAST(iType.getCompilationUnit()));
	}
	
	public static ASTNode getDeclarationNode(IMember iMember, CompilationUnit cuNode) throws JavaModelException {
		Assert.isTrue(! (iMember instanceof IInitializer));
		ASTNode node= getNameNode(iMember, cuNode);
		if (iMember instanceof IField)
			return (FieldDeclaration)ASTNodes.getParent(node, FieldDeclaration.class);
		if (iMember instanceof IType)
			return (TypeDeclaration)ASTNodes.getParent(node, TypeDeclaration.class);
		if (iMember instanceof IMethod)
			return (MethodDeclaration)ASTNodes.getParent(node, MethodDeclaration.class);
		Assert.isTrue(false);	
		return node;
	}

	private static ASTNode getNameNode(IMember iMember, CompilationUnit cuNode) throws JavaModelException {
		Selection selection= Selection.createFromStartLength(iMember.getNameRange().getOffset(), iMember.getNameRange().getLength());
		SelectionAnalyzer selectionAnalyzer= new SelectionAnalyzer(selection, true);
		cuNode.accept(selectionAnalyzer);
		ASTNode node= selectionAnalyzer.getFirstSelectedNode();
		if (node == null)
			node= selectionAnalyzer.getLastCoveringNode();
		return node;
	}

	private static ASTNode getNameNode(IMember iMember, ASTNodeMappingManager astManager) throws JavaModelException {
		return getNameNode(iMember, astManager.getAST(iMember.getCompilationUnit()));
	}

	public static PackageDeclaration getPackageDeclarationNode(IPackageDeclaration reference, CompilationUnit cuNode) throws JavaModelException {
		return (PackageDeclaration) findNode(reference.getSourceRange(), cuNode);
	}

	public static ImportDeclaration getImportDeclarationNode(IImportDeclaration reference, CompilationUnit cuNode) throws JavaModelException {
		return (ImportDeclaration) findNode(reference.getSourceRange(), cuNode);
	}

	public static ASTNode[] getImportNodes(IImportContainer reference, CompilationUnit cuNode) throws JavaModelException {
		IJavaElement[] imps= reference.getChildren();
		ASTNode[] result= new ASTNode[imps.length];
		for (int i= 0; i < imps.length; i++) {
			result[i]= getImportDeclarationNode((IImportDeclaration)imps[i], cuNode);
		}
		return result;
	}

	public static Initializer getInitializerNode(IInitializer initializer, CompilationUnit cuNode) throws JavaModelException {
		ASTNode node= findNode(initializer.getSourceRange(), cuNode);
		if (node instanceof Initializer)
			return (Initializer) node;
		if (node instanceof Block && node.getParent() instanceof Initializer)
			return (Initializer) node.getParent();
		return null;
	}
	
	private static ASTNode findNode(ISourceRange range, CompilationUnit cuNode){
		NodeFinder nodeFinder= new NodeFinder(range.getOffset(), range.getLength());
		cuNode.accept(nodeFinder);
		ASTNode coveredNode= nodeFinder.getCoveredNode();
		if (coveredNode != null)
			return coveredNode;
		else
			return nodeFinder.getCoveringNode();		
	}
}
