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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.RippleMethodFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempOccurrenceFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.UpdateTypeReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

class UseSupertypeWherePossibleUtil {

	private final TextChangeManager fManager;
	private final Set fExtractedMemberSet;
	private final String fSuperTypeName;
	private final IType fInputClass;
	private final IType fSuperTypeToUse;
	private final CodeGenerationSettings fCodeGenerationSettings;
	private final ASTNodeMappingManager fASTMappingManager;
	private final Set fBadVarSet;
	private final Map fReferenceNodeCache;//Members[] -> ASTNode[]
	private final Map fRippleMethodCache;//IMethod -> IMethod[]
	private final Set fSuperTypeSet;
	private final boolean fUpdateInstanceOf; 

	private UseSupertypeWherePossibleUtil(TextChangeManager manager, IMember[] extractedMembers, String superTypeName, IType inputClass, CodeGenerationSettings codeGenerationSettings, ASTNodeMappingManager astManager, IType supertypeToUse, boolean updateInstanceOf) throws JavaModelException {
		fManager= manager;
		fExtractedMemberSet= toWorkingCopyMembersSet(new HashSet(Arrays.asList(extractedMembers)));
		fSuperTypeName= superTypeName;
		fInputClass= (IType)WorkingCopyUtil.getWorkingCopyIfExists(inputClass);
		fCodeGenerationSettings= codeGenerationSettings;
		fASTMappingManager= astManager;
		fBadVarSet= new HashSet(0);
		fReferenceNodeCache= new HashMap();
		fRippleMethodCache= new HashMap();
		fSuperTypeToUse= (IType)WorkingCopyUtil.getWorkingCopyIfExists(supertypeToUse);
		fSuperTypeSet= createSuperTypeSet(fSuperTypeToUse, fInputClass.getJavaProject());
		fUpdateInstanceOf= updateInstanceOf;
	}

	/*
	 * Fills the <code>TextChangeManager</code> with edits, returns the nodes that are updated.
	 */
	static Set updateReferences(TextChangeManager manager, IMember[] extractedMembers, String superTypeName, IType inputClass, CodeGenerationSettings codeGenerationSettings, ASTNodeMappingManager astManager, IProgressMonitor pm, IType supertypeToUse, boolean updateInstanceOf) throws CoreException{
		UseSupertypeWherePossibleUtil inst= new UseSupertypeWherePossibleUtil(manager, extractedMembers, superTypeName, inputClass, codeGenerationSettings, astManager, supertypeToUse, updateInstanceOf);
		pm.beginTask("", 4);//$NON-NLS-1$
		SearchResultGroup[] referenceGroups= getMemberReferences(inputClass, new SubProgressMonitor(pm, 1));
		Set replacedNodes= inst.addReferenceUpdatesAndImports(manager, new SubProgressMonitor(pm, 3), referenceGroups);
		pm.done();
		return replacedNodes;
	}

	/*
	 * Fills the <code>TextChangeManager</code> with edits, returns the nodes that are updated.
	 */
	static Set updateReferences(TextChangeManager manager, IMember[] extractedMembers, String superTypeName, IType inputClass, CodeGenerationSettings codeGenerationSettings, ASTNodeMappingManager astManager, IProgressMonitor pm) throws CoreException{
	    return updateReferences(manager, extractedMembers, superTypeName, inputClass, codeGenerationSettings, astManager, pm, null, true);
	}

	private static Set createSuperTypeSet(IType type, IJavaProject project) throws JavaModelException {
		if (type == null){
			Set result= new HashSet(1);
			result.add(project.findType("java.lang.Object")); //$NON-NLS-1$
			return result;
		}	
		return toWorkingCopyMembersSet(new HashSet(Arrays.asList(JavaModelUtil.getAllSuperTypes(type, new NullProgressMonitor()))));
	}
	
	private static Set toWorkingCopyMembersSet(Set members) throws JavaModelException{
		Set result= new HashSet(members.size());
		for (Iterator iter= members.iterator(); iter.hasNext();) {
            result.add(WorkingCopyUtil.getWorkingCopyIfExists((IMember) iter.next()));
        }
        return result;
	}
	
	private static SearchResultGroup[] getMemberReferences(IMember member, IProgressMonitor pm) throws JavaModelException{
		ISearchPattern pattern= SearchEngine.createSearchPattern(member, IJavaSearchConstants.REFERENCES);
		IJavaSearchScope scope= RefactoringScopeFactory.create(member);
		return RefactoringSearchEngine.search(pm, scope, pattern);
	}
	
	//returns the set of replaced ast nodes
	private Set addReferenceUpdatesAndImports(TextChangeManager manager, IProgressMonitor pm, SearchResultGroup[] resultGroups) throws CoreException {
		Set nodeSet= getNodesToUpdate(resultGroups, pm); //ASTNodes
		Set updatedCus= new HashSet(0);
		for (Iterator iter= nodeSet.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			ICompilationUnit cu= getCompilationUnit(node);
			manager.get(cu).addTextEdit(RefactoringCoreMessages.getString("UseSupertypeWherePossibleUtil.update_reference"), createTypeUpdateEdit(new SourceRange(node)));	 //$NON-NLS-1$
			updatedCus.add(cu);
		}
		for (Iterator iter= updatedCus.iterator(); iter.hasNext();) {
			ICompilationUnit cu= (ICompilationUnit) iter.next();
			if (needsImport(cu))
				addSupertypeImport(manager, cu);			
		}
		return nodeSet;
	}

	private boolean needsImport(ICompilationUnit cu) {
		IPackageFragment superTypePackage= getSuperTypePackage();
		if (superTypePackage.getElementName().equals("java.lang")) //$NON-NLS-1$
			return false;
		return ! superTypePackage.equals(cu.getParent());
	}

	private IPackageFragment getSuperTypePackage() {
		if (fSuperTypeToUse != null)
			return fSuperTypeToUse.getPackageFragment();
		return getInputClassPackage();
	}

	//Set of ASTNodes
	private Set getNodesToUpdate(SearchResultGroup[] resultGroups, IProgressMonitor pm) throws JavaModelException {
		Set nodeSet= new HashSet();
		for (int i= 0; i < resultGroups.length; i++) {
			nodeSet.addAll(Arrays.asList(getAstNodes(resultGroups[i])));
		}
		retainUpdatableNodes(nodeSet, pm);
		return nodeSet;
	}

	private void retainUpdatableNodes(Set nodeSet, IProgressMonitor pm) throws JavaModelException {
		Collection nodesToRemove= computeNodesToRemove(nodeSet, pm);
		for (Iterator iter= nodesToRemove.iterator(); iter.hasNext();) {
			nodeSet.remove(iter.next());
		}
	}
	
	private Collection computeNodesToRemove(Set nodeSet, IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1);  //$NON-NLS-1$
		IProgressMonitor subPm1= new SubProgressMonitor(pm, 1);
		subPm1.beginTask("", nodeSet.size()); //$NON-NLS-1$
		Collection nodesToRemove= new HashSet(0);
		for (Iterator iter= nodeSet.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			if (hasDirectProblems(node, new SubProgressMonitor(subPm1, 1)))
				nodesToRemove.add(node);	
		}
		subPm1.done();
		if (nodesToRemove.isEmpty())
			return nodesToRemove;

		IProgressMonitor subPm2= new SubProgressMonitor(pm, 1);
		subPm2.beginTask("", nodeSet.size() - nodesToRemove.size()); //$NON-NLS-1$
		boolean reiterate;
		do {
			reiterate= false;
			for (Iterator iter= nodeSet.iterator(); iter.hasNext();) {
				ASTNode node= (ASTNode) iter.next();
				if (! nodesToRemove.contains(node) && hasIndirectProblems(node, nodesToRemove, new SubProgressMonitor(subPm2, 1))){
					reiterate= true;
					nodesToRemove.add(node);			
				}
			}
		} while (reiterate);	
		subPm2.done();
		pm.done();
		return nodesToRemove;
	}

	private boolean hasIndirectProblems(ASTNode node, Collection nodesToRemove, IProgressMonitor pm) throws JavaModelException {
		try{
			ASTNode parentNode= node.getParent();
			if (parentNode instanceof VariableDeclarationStatement){
				VariableDeclarationStatement vds= (VariableDeclarationStatement)parentNode;
				if (vds.getType() != node)
					return false; 
				VariableDeclarationFragment[] vdfs= getVariableDeclarationFragments(vds);	
				pm.beginTask("", vdfs.length); //$NON-NLS-1$
				for (int i= 0; i < vdfs.length; i++) {
					if (hasIndirectProblems(vdfs[i], nodesToRemove, new SubProgressMonitor(pm, 1)))
						return true;	
				}
			} else if (parentNode instanceof FieldDeclaration){
				FieldDeclaration fd= (FieldDeclaration)parentNode;
				if (fd.getType() != node)
					return false; 
				VariableDeclarationFragment[] vdfs= getVariableDeclarationFragments(fd);	
				pm.beginTask("", vdfs.length); //$NON-NLS-1$
				for (int i= 0; i < vdfs.length; i++) {
					if (hasIndirectProblems(vdfs[i], nodesToRemove, new SubProgressMonitor(pm, 1)))
						return true;	
				}
					
			} else if (parentNode instanceof VariableDeclaration){
				pm.beginTask("", 1); //$NON-NLS-1$
				if (isMethodParameter(parentNode)){
					MethodDeclaration methodDeclaration= (MethodDeclaration)parentNode.getParent();	
					int parameterIndex= methodDeclaration.parameters().indexOf(parentNode);
					IMethod[] methods= getAllRippleMethods(methodDeclaration, new SubProgressMonitor(pm, 1));
					if (methods == null){ //XXX this can be null because of bug 22883
						SingleVariableDeclaration svd= getParameterDeclarationNode(parameterIndex, methodDeclaration);
						nodesToRemove.add(svd.getType());
						addToBadVarSet(svd);
						return true;
					}
					if (isAnyParameterDeclarationExcluded(methods, parameterIndex, nodesToRemove)){
						for (int i= 0; i < methods.length; i++) {
							SingleVariableDeclaration svd= getParameterDeclarationNode(parameterIndex, methods[i]);
							if (svd != null){
								nodesToRemove.add(svd.getType());
								addToBadVarSet(svd);
							}	
						}
						return true;
					}
				} 
				if (hasIndirectProblems((VariableDeclaration)parentNode, nodesToRemove, new SubProgressMonitor(pm, 1)))
					return true;
			} else if (parentNode instanceof CastExpression){
				pm.beginTask("", 1); //$NON-NLS-1$
				if (! isReferenceUpdatable(parentNode, nodesToRemove))
					return true;	
			} else if (parentNode instanceof MethodDeclaration){
				pm.beginTask("", 3); //$NON-NLS-1$
				MethodDeclaration methodDeclaration= (MethodDeclaration)parentNode;
				if (methodDeclaration.getReturnType() == node){
					IMethod[] methods= getAllRippleMethods(methodDeclaration, new SubProgressMonitor(pm, 1));
					if (methods == null){ //XXX this can be null because of bug 22883
						nodesToRemove.add(methodDeclaration.getReturnType());
						return true;
					}	
					if (isAnyMethodReturnTypeNodeExcluded(methods, nodesToRemove)){
						nodesToRemove.addAll(getAllReturnTypeNodes(methods));
						return true;
					}	
					ASTNode[] referenceNodes= getReferenceNodes(methods, new SubProgressMonitor(pm, 1));
					for (int i= 0; i < referenceNodes.length; i++) {
						if (! isReferenceUpdatable(referenceNodes[i], nodesToRemove)){
							nodesToRemove.addAll(getAllReturnTypeNodes(methods));
							return true;
						}
					}
				}		
			} else if (parentNode instanceof ArrayType){
				return hasIndirectProblems(parentNode, nodesToRemove, pm);
			} 
			return false;
		} finally{
			pm.done();
		}	
	}

	private static boolean isMethodParameter(ASTNode node){
		return (node instanceof VariableDeclaration) && 
		          (node.getParent() instanceof MethodDeclaration) &&
		          ((MethodDeclaration)node.getParent()).parameters().indexOf(node) != -1;
	}
	
	private IMethod[] getAllRippleMethods(MethodDeclaration methodDeclaration, IProgressMonitor pm) throws JavaModelException{
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			IMethodBinding methodBinding= methodDeclaration.resolveBinding();
			if (methodBinding == null)
				return new IMethod[0];
			IMethod method= Binding2JavaModel.find(methodBinding, getCompilationUnit(methodDeclaration).getJavaProject());
			if (method == null)
				return null; //XXX this can be null because of bug 22883
			method= (IMethod)WorkingCopyUtil.getWorkingCopyIfExists(method);	
			return getAllRippleMethods(pm, getTopMethod(method, new SubProgressMonitor(pm, 1)));
		} finally{
			pm.done();
		}	
	}
	private IMethod[] getAllRippleMethods(IProgressMonitor pm, IMethod topMethod) throws JavaModelException {
		if (fRippleMethodCache.containsKey(topMethod))
			return (IMethod[])fRippleMethodCache.get(topMethod);
		IMethod[] methods= RippleMethodFinder.getRelatedMethods(topMethod, new SubProgressMonitor(pm, 1), new ICompilationUnit[0]);	
		fRippleMethodCache.put(topMethod, getWorkingCopyMethods(methods));
		return methods;
	}

	private static IMethod[] getWorkingCopyMethods(IMethod[] methods) throws JavaModelException{
		IMethod[] result= new IMethod[methods.length];
		for (int i = 0; i < methods.length; i++) {
			result[i]= (IMethod)WorkingCopyUtil.getWorkingCopyIfExists(methods[i]);
		}
		return result;
	}
	
	private boolean isAnyParameterDeclarationExcluded(IMethod[] methods, int parameterIndex, Collection nodesToRemove) throws JavaModelException{
		for (int i= 0; i < methods.length; i++) {
			SingleVariableDeclaration paramDecl= getParameterDeclarationNode(parameterIndex, methods[i]);
			if (paramDecl == null)
				return true;
			if (fBadVarSet.contains(paramDecl))
				return true;
			if (nodesToRemove.contains(paramDecl.getType()))
				return true;							
		}
		return false;
	}

	private SingleVariableDeclaration getParameterDeclarationNode(int parameterIndex, IMethod method) throws JavaModelException {
		return getParameterDeclarationNode(parameterIndex, getMethodDeclarationNode(method));
	}
	
	private SingleVariableDeclaration getParameterDeclarationNode(int parameterIndex, MethodDeclaration md){
		if (md == null)
			return null;
		return (SingleVariableDeclaration)md.parameters().get(parameterIndex);
	}
	
	private Collection getAllReturnTypeNodes(IMethod[] methods) throws JavaModelException {
		List result= new ArrayList(methods.length);
		for (int i= 0; i < methods.length; i++) {
			MethodDeclaration methodDeclarationNode= getMethodDeclarationNode(methods[i]);
			if (methodDeclarationNode != null)
				result.add(methodDeclarationNode.getReturnType());
		}
		return result;
	}

	private boolean isAnyMethodReturnTypeNodeExcluded(IMethod[] methods, Collection nodesToRemove) throws JavaModelException{
		for (int i= 0; i < methods.length; i++) {
			MethodDeclaration methodDeclarationNode= getMethodDeclarationNode(methods[i]);
			if (methodDeclarationNode == null || nodesToRemove.contains(methodDeclarationNode.getReturnType()))
				return true;
		}
		return false;
	}
	
	private static IMethod getTopMethod(IMethod method, IProgressMonitor pm) throws JavaModelException {
		Assert.isNotNull(method);
		pm.beginTask("", 3); //$NON-NLS-1$
		IMethod top= method;
		IMethod oldTop;
		do {
			oldTop = top;
			top= MethodChecks.overridesAnotherMethod(top, new SubProgressMonitor(pm, 1)); 
		} while(top != null);
		pm.done();
		return (IMethod)WorkingCopyUtil.getWorkingCopyIfExists(oldTop);
	}
	
	private boolean hasIndirectProblems(VariableDeclaration varDeclaration, Collection nodesToRemove, IProgressMonitor pm) throws JavaModelException{
		ASTNode[] references= getVariableReferenceNodes(varDeclaration, pm);
		for (int i= 0; i < references.length; i++) {
			if (! isReferenceUpdatable(references[i], nodesToRemove)){
				addToBadVarSet(varDeclaration);
				return true;
			}	
		}
		return false;
	}
	
	private ASTNode[] getVariableReferenceNodes(VariableDeclaration varDeclaration, IProgressMonitor pm) throws JavaModelException{
		IVariableBinding vb= varDeclaration.resolveBinding();
		if (vb == null)
			return new ASTNode[0];
		if (vb.isField())
			return getFieldReferenceNodes(varDeclaration, pm);
		return TempOccurrenceFinder.findTempOccurrenceNodes(varDeclaration, true, false);
	}

	private ASTNode[] getFieldReferenceNodes(VariableDeclaration varDeclaration, IProgressMonitor pm) throws JavaModelException{
		Assert.isTrue(varDeclaration.resolveBinding().isField());
		IField field= Binding2JavaModel.find(varDeclaration.resolveBinding(), getCompilationUnit(varDeclaration).getJavaProject());
		if (field == null)
			return new ASTNode[0];
		field= (IField)WorkingCopyUtil.getWorkingCopyIfExists(field);	
		return getReferenceNodes(field, pm);	
	}
	
	private ASTNode[] getReferenceNodes(IMember member, IProgressMonitor pm) throws JavaModelException{
		ASTNode[] cached= getReferencesFromCache(member);
		if (cached != null)
			return cached;
		ASTNode[] nodes= ASTNodeSearchUtil.findReferenceNodes(member, fASTMappingManager, pm);
		putToCache(member, nodes);
		return nodes;
	}

	private ASTNode[] getReferenceNodes(IMember[] members, IProgressMonitor pm) throws JavaModelException{
		try{
			if (members == null || members.length ==0)
				return new ASTNode[0];
			if (members.length == 1)
				return getReferenceNodes(members[0], pm);	
			IJavaSearchScope scope= RefactoringScopeFactory.create(members[0]);
			return ASTNodeSearchUtil.findReferenceNodes(members, fASTMappingManager, pm, scope);
		} finally {
			pm.done();
		}	
	}
	
	private ASTNode[] getReferencesFromCache(IMember member){
		return (ASTNode[])fReferenceNodeCache.get(member);
	}
	
	private void putToCache(IMember member, ASTNode[] nodes){
		fReferenceNodeCache.put(member, nodes);
	}
		
	private boolean isReferenceUpdatable(ASTNode node, Collection nodesToRemove) throws JavaModelException{
		ASTNode parent= node.getParent();
		if (node instanceof SimpleName && parent instanceof ArrayInitializer){
			//there are only 2 places where ArrayInitializers can be used
			ArrayInitializer initializer= (ArrayInitializer)parent;
			if (initializer.getParent() instanceof VariableDeclarationFragment)
				return isReferenceUpdatable(initializer, nodesToRemove);
			if (initializer.getParent() instanceof ArrayCreation){
				ArrayCreation arrayCreation= (ArrayCreation)initializer.getParent();
				return ! nodesToRemove.contains(arrayCreation.getType().getComponentType());
			}	
		}
		if (parent instanceof VariableDeclarationFragment){
			VariableDeclarationFragment r1= (VariableDeclarationFragment)parent;
			if (node == r1.getInitializer()){
				IVariableBinding vb= r1.resolveBinding();
				if (vb != null && fBadVarSet.contains(getCompilationUnitNode(node).findDeclaringNode(vb)))
					return false;
			}
		} else if (parent instanceof Assignment){
			Assignment assmnt= (Assignment)parent;
			if (node == assmnt.getRightHandSide()){
				Expression lhs= assmnt.getLeftHandSide();
				if (lhs instanceof SimpleName){
					IBinding binding= ((SimpleName)lhs).resolveBinding();
					if (binding != null && fBadVarSet.contains(getCompilationUnitNode(lhs).findDeclaringNode(binding)))
						return false;
				} else if (lhs instanceof FieldAccess){
					IBinding binding= ((FieldAccess)lhs).getName().resolveBinding();
					if (binding != null && fBadVarSet.contains(getCompilationUnitNode(lhs).findDeclaringNode(binding)))
						return false;
				}
			}
		} else if (isInvocation(parent)) {
			return isReferenceInInvocationUpdatable(node, nodesToRemove);
		} else if (parent instanceof ReturnStatement){
			MethodDeclaration md= (MethodDeclaration)ASTNodes.getParent(parent, MethodDeclaration.class);
			if (nodesToRemove.contains(md.getReturnType()))
				return false;
		} 
		return true;
	}
	
	private boolean isReferenceInInvocationUpdatable(ASTNode node, Collection nodesToRemove) throws JavaModelException{
		ASTNode parent= node.getParent();
		Assert.isTrue(isInvocation(parent));
		                   
		int argumentIndex= getArgumentIndexInInvocation(node);
		if (argumentIndex == -1 )
			return isReferenceUpdatable(parent, nodesToRemove);
		IMethodBinding bin= resolveMethodBindingInInvocation(node);
		if (bin == null)
			return false;
		IMethod method= Binding2JavaModel.find(bin, fInputClass.getJavaProject());
		if (method == null)
			return false;
		method= (IMethod)WorkingCopyUtil.getWorkingCopyIfExists(method);	
		
		if (method.getCompilationUnit() == null) {
			if (fSuperTypeToUse == null)
				return false;
			IType type= JavaModelUtil.findType(fInputClass.getJavaProject(), Signature.toString(method.getParameterTypes()[argumentIndex]));
			if (type == null)
				return false;
			return isTypeOkToUseAsSuperType(type);
		}	
		MethodDeclaration methodDeclarationNode= getMethodDeclarationNode(method);
		if (method == null)
			return false;
		SingleVariableDeclaration parDecl= (SingleVariableDeclaration)methodDeclarationNode.parameters().get(argumentIndex);
		return ! fBadVarSet.contains(parDecl);
	}
	
	private static int getArgumentIndexInInvocation(ASTNode node){
		ASTNode parent= node.getParent();
		Assert.isTrue(isInvocation(parent));
		if (parent instanceof MethodInvocation)
			return ((MethodInvocation)parent).arguments().indexOf(node);
		else if (parent instanceof ConstructorInvocation)	
			return ((ConstructorInvocation)parent).arguments().indexOf(node);
		else if (parent instanceof ClassInstanceCreation)
			return ((ClassInstanceCreation)parent).arguments().indexOf(node);
		else	
			return ((SuperConstructorInvocation)parent).arguments().indexOf(node);
	}
	
	private static IMethodBinding resolveMethodBindingInInvocation(ASTNode node){
		ASTNode parent= node.getParent();
		Assert.isTrue(isInvocation(parent));
		if (parent instanceof MethodInvocation){
			IBinding bin= ((MethodInvocation)parent).getName().resolveBinding();
			if (! (bin instanceof IMethodBinding))
				return null;
			return (IMethodBinding)bin;
		} else if (parent instanceof ConstructorInvocation)	
			return ((ConstructorInvocation)parent).resolveConstructorBinding();
		else if (parent instanceof ClassInstanceCreation)
			return ((ClassInstanceCreation)parent).resolveConstructorBinding();
		else	
			return ((SuperConstructorInvocation)parent).resolveConstructorBinding();
	}
	
	private static boolean isInvocation(ASTNode node){
		return
			node instanceof MethodInvocation ||
		    node instanceof ConstructorInvocation ||
		    node instanceof SuperConstructorInvocation ||
		    node instanceof ClassInstanceCreation;
	}
	
	private static CompilationUnit getCompilationUnitNode(ASTNode node) {
		return (CompilationUnit)ASTNodes.getParent(node, CompilationUnit.class);
	}

	private static ASTNode getUnparenthesizedParent(ASTNode node){
		if (! (node.getParent() instanceof ParenthesizedExpression))
			return node.getParent();
		ASTNode parent= (ParenthesizedExpression)node.getParent();
		while(parent instanceof ParenthesizedExpression){
			parent= parent.getParent();
		}
		return parent;
	}
	
	private boolean hasDirectProblems(ASTNode node, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			ASTNode parentNode= getUnparenthesizedParent(node);
			if (parentNode instanceof TypeLiteral)
				return true;
			if (parentNode instanceof MethodInvocation)	
				return true;
			if (parentNode instanceof FieldAccess)	
				return true;
			if (parentNode instanceof ThisExpression)	
				return true;
			if (parentNode instanceof SuperMethodInvocation)	
				return true;
			if (parentNode instanceof ImportDeclaration)	
				return true;	

		    if (parentNode instanceof QualifiedName){
		    	QualifiedName qn= (QualifiedName)parentNode;
		    	IBinding binding= qn.resolveBinding();
		    	if (! (binding instanceof IVariableBinding))
			        return true;
		        IVariableBinding vb= (IVariableBinding)binding;
		        if (! vb.isField())
			        return true;
			    IField field= Binding2JavaModel.find(vb, getCompilationUnit(qn).getJavaProject());
		        if (field != null)
		            field= (IField)WorkingCopyUtil.getWorkingCopyIfExists(field);
				
				if (! fExtractedMemberSet.contains(field))
			        return true;
				IBinding b1= qn.getQualifier().resolveBinding();
		        if (! (b1 instanceof ITypeBinding))
			        return true;
		        return false;
		    }    

			if (parentNode instanceof InstanceofExpression)
				return ! fUpdateInstanceOf;
				
			if (parentNode instanceof ClassInstanceCreation){
				if (node == ((ClassInstanceCreation)parentNode).getName())
					return true;
			}
			if (parentNode instanceof ArrayType && parentNode.getParent() instanceof ArrayCreation){
				if (parentNode == ((ArrayCreation)parentNode.getParent()).getType())
					return true;
			}
			if (parentNode instanceof ArrayType)
				return hasDirectProblems(parentNode, pm);
				
			if (parentNode instanceof MethodDeclaration){
				MethodDeclaration md= (MethodDeclaration)parentNode;
				if (md.thrownExceptions().contains(node))
					return true;
				if (node == md.getReturnType()){
					ICompilationUnit cu= getCompilationUnit(node);
					IMethodBinding binding= md.resolveBinding();
					if (binding == null)
						return true; //XXX
					IMethod method= Binding2JavaModel.find(binding, cu.getJavaProject());
					if (method != null){
						method= (IMethod)WorkingCopyUtil.getWorkingCopyIfExists(method); 
						if (anyReferenceHasDirectProblems(method, new SubProgressMonitor(pm, 1)))
							return true;	
					}	
				}
			}	
						
			if (parentNode instanceof SingleVariableDeclaration && parentNode.getParent() instanceof CatchClause)
				return true;
				
			if (parentNode instanceof TypeDeclaration){
				if(node == ((TypeDeclaration)parentNode).getSuperclass())
				 	return true;
				 if (((TypeDeclaration)parentNode).superInterfaces().contains(node))
				 	return true;
			}

			if (parentNode instanceof FieldDeclaration){
				FieldDeclaration fd= (FieldDeclaration)parentNode;
				if (fd.getType() == node && ! canReplaceTypeInFieldDeclaration(fd, new SubProgressMonitor(pm, 1))){
					addAllToBadVarSet(getVariableDeclarationFragments(fd));
					return true;						
				}
			}
							
			if (parentNode instanceof VariableDeclarationStatement){
				VariableDeclarationStatement vds= (VariableDeclarationStatement)parentNode;
				if (vds.getType() == node && ! canReplaceTypeInVariableDeclarationStatement(vds, new SubProgressMonitor(pm, 1))){
					addAllToBadVarSet(getVariableDeclarationFragments(vds));
					return true; 
				}	
			}	
	
			if (parentNode instanceof SingleVariableDeclaration){
				if (anyVariableReferenceHasDirectProblems((SingleVariableDeclaration)parentNode, new SubProgressMonitor(pm, 1))){
					if (! isMethodParameter(parentNode)){
						addToBadVarSet((SingleVariableDeclaration)parentNode);
						return true;
					}	
					
					MethodDeclaration methodDeclaration= (MethodDeclaration)parentNode.getParent();	
					int parameterIndex= methodDeclaration.parameters().indexOf(parentNode);
					IMethod[] methods= getAllRippleMethods(methodDeclaration, new SubProgressMonitor(pm, 1));
					
					if (methods == null){ //XXX this can be null because of bug 22883
						SingleVariableDeclaration svd= getParameterDeclarationNode(parameterIndex, methodDeclaration);
						addToBadVarSet(svd);
						return true;
					}
					for (int i= 0; i < methods.length; i++) {
						SingleVariableDeclaration svd= getParameterDeclarationNode(parameterIndex, methods[i]);
						if (svd != null)
							addToBadVarSet(svd);
					}
					return true;
				}	
			} 
			if (parentNode instanceof CastExpression){
				if (isNotUpdatableReference(parentNode, new SubProgressMonitor(pm, 1)))
					return true;
			}
			return false;	
		} finally {
			pm.done();
		}	
	}

	private ASTNode[] getAstNodes(SearchResultGroup searchResultGroup){
		Set nodeSet= new HashSet();
		ICompilationUnit cu= searchResultGroup.getCompilationUnit();
		if (cu == null)
			return new ASTNode[0];
		ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
		ASTNode[] nodes= ASTNodeSearchUtil.getAstNodes(searchResultGroup.getSearchResults(), getAST(wc));
		for (int i= 0; i < nodes.length; i++) {
			nodeSet.add(nodes[i]);
		}
		return (ASTNode[]) nodeSet.toArray(new ASTNode[nodeSet.size()]);	
	}
		
	private void addSupertypeImport(TextChangeManager manager, ICompilationUnit cu) throws CoreException {
		ImportEdit importEdit= new ImportEdit(cu, fCodeGenerationSettings);
		importEdit.addImport(getFullyQualifiedSupertypeName());
		String[] keys= {getFullyQualifiedSupertypeName()};
		String editName= RefactoringCoreMessages.getFormattedString("UseSupertypeWherePossibleUtil.adding_import", keys); //$NON-NLS-1$
		manager.get(cu).addTextEdit(editName, importEdit);
	}

	private String getFullyQualifiedSupertypeName() {
		if (fSuperTypeToUse != null)
			return JavaModelUtil.getFullyQualifiedName(fSuperTypeToUse);
		return getInputClassPackage().getElementName() + "." + fSuperTypeName; //$NON-NLS-1$
	}

	private IPackageFragment getInputClassPackage() {
		return fInputClass.getPackageFragment();
	}

	private TextEdit createTypeUpdateEdit(ISourceRange sourceRange) {
		return new UpdateTypeReferenceEdit(sourceRange.getOffset(), sourceRange.getLength(), fSuperTypeName, fInputClass.getElementName());
	}
		
	private boolean canReplaceTypeInDeclarationFragments(VariableDeclarationFragment[] fragments, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", fragments.length); //$NON-NLS-1$
		try{
			for (int i= 0; i < fragments.length; i++) {
				if (anyVariableReferenceHasDirectProblems(fragments[i], new SubProgressMonitor(pm, 1)))
					return false;
			}
			return true;
		} finally {
			pm.done();
		}	
	}

	private boolean canReplaceTypeInFieldDeclaration(FieldDeclaration fd, IProgressMonitor pm) throws JavaModelException {
		return canReplaceTypeInDeclarationFragments(getVariableDeclarationFragments(fd), pm);
	}
	
	private boolean canReplaceTypeInVariableDeclarationStatement(VariableDeclarationStatement vds, IProgressMonitor pm) throws JavaModelException{
		return canReplaceTypeInDeclarationFragments(getVariableDeclarationFragments(vds), pm);
	}

	private void addToBadVarSet(VariableDeclaration variableDeclaration) {
		fBadVarSet.add(variableDeclaration);
	}
	
	private void addAllToBadVarSet(VariableDeclaration[] variableDeclarations) {
		for (int i= 0; i < variableDeclarations.length; i++) {
			addToBadVarSet(variableDeclarations[i]);
		}
	}

	private boolean anyVariableReferenceHasDirectProblems(VariableDeclaration varDeclaration, IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 2); //$NON-NLS-1$
		try{
			if (isInterfaceMethodParameterDeclaration(varDeclaration))
				return true;
			return anyReferenceNodeHasDirectProblems(getVariableReferenceNodes(varDeclaration, new SubProgressMonitor(pm, 1)), new SubProgressMonitor(pm, 1));
		} finally{
			pm.done();
		}	
	}
	
	private static boolean isInterfaceMethodParameterDeclaration(VariableDeclaration varDeclaration){
		if (! (varDeclaration.getParent() instanceof MethodDeclaration))
			return false;
		if (! (varDeclaration.getParent().getParent() instanceof TypeDeclaration))	
			return false;
		return (((TypeDeclaration)varDeclaration.getParent().getParent()).isInterface());
	}
	
	private boolean anyReferenceHasDirectProblems(IMember member, IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 2); //$NON-NLS-1$
		try{
			return anyReferenceNodeHasDirectProblems(getReferenceNodes(member, new SubProgressMonitor(pm, 1)), new SubProgressMonitor(pm, 1));
		} finally{
			pm.done();
		}	
	}
	
	private boolean anyReferenceNodeHasDirectProblems(ASTNode[] referenceNodes, IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", referenceNodes.length); //$NON-NLS-1$
		try{
			for (int i= 0; i < referenceNodes.length; i++) {
				if (isNotUpdatableReference(referenceNodes[i], new SubProgressMonitor(pm, 1)))
					return true;
			}
			return false;
		} finally{
			pm.done();
		}	
	}

	//XXX needs better name
	private boolean isNotUpdatableReference(ASTNode parentNode, IProgressMonitor pm) throws JavaModelException{
		ASTNode unparenthesizedParent= getUnparenthesizedParent(parentNode);
		if (unparenthesizedParent instanceof ArrayAccess){
			ArrayAccess arrayAccess= (ArrayAccess)unparenthesizedParent;
			if (parentNode.equals(arrayAccess.getArray()))
				return isNotUpdatableReference(arrayAccess, pm);
			if (ASTNodes.isParent(parentNode, arrayAccess.getArray()))
				return isNotUpdatableReference(arrayAccess, pm);
		}
		if (unparenthesizedParent instanceof FieldAccess){
			if (fSuperTypeToUse == null)
				return true;
			IBinding binding= ((FieldAccess)unparenthesizedParent).getName().resolveBinding();
			if (binding instanceof IVariableBinding)
				return !(isVariableBindingOk((IVariableBinding)binding));
			return true;	
		}	
			
		if (unparenthesizedParent instanceof QualifiedName){
			IBinding binding= ((QualifiedName)unparenthesizedParent).resolveBinding();
			if (binding instanceof IVariableBinding)
				return !(isVariableBindingOk((IVariableBinding)binding));
			return true;	
		}	
		
		if (unparenthesizedParent instanceof MethodInvocation){
			MethodInvocation mi= (MethodInvocation)unparenthesizedParent;
			//XXX
			if (parentNode == mi.getExpression() && ! isMethodInvocationOk(mi, pm)) 
				return true;
			if (mi.getExpression() != null && ASTNodes.isParent(parentNode, mi.getExpression()) && ! isMethodInvocationOk(mi, pm))
				return true;
			
			int argumentIndex= mi.arguments().indexOf(parentNode);
			if (argumentIndex != -1){
				IBinding bin= mi.getName().resolveBinding();
				if (! (bin instanceof IMethodBinding))
					return true;
				IMethod method= Binding2JavaModel.find((IMethodBinding)bin, fInputClass.getJavaProject());
				if (method == null)
					return true;
				IType paramType= getMethodParameterType(method, argumentIndex);
				if (! isTypeOkToUseAsSuperType(paramType))
					return true;
			}
		}
		if (unparenthesizedParent instanceof Assignment){
			Assignment assign= (Assignment)unparenthesizedParent;
			if (parentNode == assign.getRightHandSide()){
				IType type= findType(assign.getLeftHandSide().resolveTypeBinding());
				if (type == null)
					return true;
				if (! isTypeOkToUseAsSuperType(type))
					return true;	
			}	
		}
		if (unparenthesizedParent instanceof VariableDeclaration){
			VariableDeclaration vd= (VariableDeclaration)unparenthesizedParent;
			if (parentNode == vd.getInitializer()){
				IType type= findType(vd.getName().resolveTypeBinding());
				if (type == null)
					return true;
				if (! isTypeOkToUseAsSuperType(type))
					return true;
			}
		}
		return false;
	}

	private boolean isTypeOkToUseAsSuperType(IType type) throws JavaModelException{
		type= (IType)WorkingCopyUtil.getWorkingCopyIfExists(type);
		if (type.equals(fInputClass))
			return true;
		if (fSuperTypeToUse != null && fSuperTypeToUse.equals(type))
			return true;	
		if ( fSuperTypeSet.contains(type))
			return true;
		return false;
	}
	
	//maybe generally useful
	private static IType getMethodParameterType(IMethod method, int parameterIndex) throws JavaModelException{
		Assert.isTrue(parameterIndex >=0);
		if (method.getNumberOfParameters() < parameterIndex)
			return null;
		String fqn= JavaModelUtil.getResolvedTypeName(method.getParameterTypes()[parameterIndex], method.getDeclaringType());
		if (fqn == null)
			return null;
		return JavaModelUtil.findType(method.getJavaProject(), fqn);	
	}
	
	private boolean isVariableBindingOk(IVariableBinding vb) throws JavaModelException{
		if (! vb.isField())
			return false;
		IField field= Binding2JavaModel.find(vb, fInputClass.getJavaProject());
		if (field == null)
			return false;
		field= (IField)WorkingCopyUtil.getWorkingCopyIfExists(field);	
		return fExtractedMemberSet.contains(field);
	}
	
	private boolean isMethodInvocationOk(MethodInvocation mi, IProgressMonitor pm) throws JavaModelException{
		IBinding miBinding= mi.getName().resolveBinding();
		if (miBinding == null || miBinding.getKind() != IBinding.METHOD)
			return false;
		IMethod method;
		if (fSuperTypeToUse != null) //XXX	
			method= Binding2JavaModel.findIncludingSupertypes((IMethodBinding)miBinding, fSuperTypeToUse, pm);
		else
			method= Binding2JavaModel.find((IMethodBinding)miBinding, fInputClass);
		if (method == null)
			return false;
		method= (IMethod)WorkingCopyUtil.getWorkingCopyIfExists(method);				
		if (! fExtractedMemberSet.contains(method))
			return false;	
		if (VisibilityChecker.isVisibleFrom(method, mi, fASTMappingManager.getCompilationUnit(mi)))
			return true;
		return false;	
	}
	
	private CompilationUnit getAST(ICompilationUnit cu){
		return fASTMappingManager.getAST(cu);
	}
	
	private ICompilationUnit getCompilationUnit(ASTNode node) {
		return fASTMappingManager.getCompilationUnit(node);
	}

	private MethodDeclaration getMethodDeclarationNode(IMethod iMethod) throws JavaModelException{
		return ASTNodeSearchUtil.getMethodDeclarationNode(iMethod, fASTMappingManager);
	}
	
	private static VariableDeclarationFragment[] getVariableDeclarationFragments(VariableDeclarationStatement vds){
		return (VariableDeclarationFragment[]) vds.fragments().toArray(new VariableDeclarationFragment[vds.fragments().size()]);
	}

	private static VariableDeclarationFragment[] getVariableDeclarationFragments(FieldDeclaration fd){
		return (VariableDeclarationFragment[]) fd.fragments().toArray(new VariableDeclarationFragment[fd.fragments().size()]);
	}
		
	private IType findType(ITypeBinding tb) throws JavaModelException{
		if (tb == null)
			return null;
		IType result= Binding2JavaModel.find(tb, fInputClass.getJavaProject());
		if (result == null)
			return result;
		return (IType)WorkingCopyUtil.getWorkingCopyIfExists(result);	
	}
}
