package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;

class ConstructorReferenceFinder {
	private final IType fType;
	private final ASTNodeMappingManager fASTManager;
	
	private ConstructorReferenceFinder(IType type, ASTNodeMappingManager astManager){
		fType= type;
		fASTManager= astManager;
	}
	
	public static ASTNode[] getConstructorReferenceNodes(IType type, ASTNodeMappingManager astManager, IProgressMonitor pm) throws JavaModelException{
		return new ConstructorReferenceFinder(type, astManager).getConstructorReferenceNodes(pm);
	}
	
	private ASTNode[] getConstructorReferenceNodes(IProgressMonitor pm) throws JavaModelException{
		IJavaSearchScope scope= RefactoringScopeFactory.create(fType);
		ISearchPattern pattern= createConstructorSearchPattern(fType, IJavaSearchConstants.REFERENCES);
		if (pattern == null){
			if (JavaElementUtil.getAllConstructors(fType).length != 0)
				return new ASTNode[0];
			return getImplicitConstructorReferenceNodes(pm);	
		}	
		return ASTNodeSearchUtil.searchNodes(scope, pattern, fASTManager, pm);
	}
	
	private static ISearchPattern createConstructorSearchPattern(IType type, int limitTo) throws JavaModelException {
		return RefactoringSearchEngine.createSearchPattern(JavaElementUtil.getAllConstructors(type), limitTo);
	}

	private ASTNode[] getImplicitConstructorReferenceNodes(IProgressMonitor pm) throws JavaModelException {
		ITypeHierarchy hierarchy= fType.newTypeHierarchy(pm);
		IType[] subTypes= hierarchy.getAllSubtypes(fType);
		List result= new ArrayList(subTypes.length);
		for (int i= 0; i < subTypes.length; i++) {
			if (! subTypes[i].isBinary())
				result.addAll(getAllSuperConstructorInvocations(subTypes[i]));
		}
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);
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
		MethodDeclaration constructorNode= getMethodDeclarationNode(constructor);
		Assert.isTrue(constructorNode.isConstructor());
		Block body= constructorNode.getBody();
		Assert.isNotNull(body);
		List statements= body.statements();
		if (! statements.isEmpty() && statements.get(0) instanceof SuperConstructorInvocation)
			return (SuperConstructorInvocation)statements.get(0);
		return null;
	}

	private MethodDeclaration getMethodDeclarationNode(IMethod iMethod) throws JavaModelException {
		Selection selection= Selection.createFromStartLength(iMethod.getNameRange().getOffset(), iMethod.getNameRange().getLength());
		SelectionAnalyzer selectionAnalyzer= new SelectionAnalyzer(selection, true);
		fASTManager.getAST(iMethod.getCompilationUnit()).accept(selectionAnalyzer);
		ASTNode node= selectionAnalyzer.getFirstSelectedNode();
		if (node == null)
			node= selectionAnalyzer.getLastCoveringNode();
		if (node == null)	
			return null;
		return (MethodDeclaration)ASTNodes.getParent(node, MethodDeclaration.class);
	}
	
}
