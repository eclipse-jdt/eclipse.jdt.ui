/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at	
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.UpdateTypeReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeMappingManager;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;


public final class ExtractInterfaceUtil {

	private ExtractInterfaceUtil(){}
	
	private static ConstraintVariable[] getAllOfType(ITypeConstraint[] constraints, ITypeBinding binding){
		Set result= new HashSet();
		ITypeBinding typeBinding= binding;
		for (int i= 0; i < constraints.length; i++) {
			ITypeConstraint constraint= constraints[i];
			if (constraint.isSimpleTypeConstraint()){
				SimpleTypeConstraint simple= (SimpleTypeConstraint)constraint;
				if (simple.getLeft().isEqualBinding(typeBinding))
					result.add(simple.getLeft());
				if (simple.getRight().isEqualBinding(typeBinding))
					result.add(simple.getRight());
			} else {
				//TODO composite constraints
			}
		}
		return (ConstraintVariable[]) result.toArray(new ConstraintVariable[result.size()]);
	}

	private static ConstraintVariable[] getUpdatableVariables(IType theClass, IType theInterface, ASTNodeMappingManager astManager, IProgressMonitor pm) throws JavaModelException{
		ITypeBinding classBinding= getTypeBinding(theClass, astManager);
		ITypeBinding interfaceBinding= getSuperTypeBinding(classBinding, theInterface);
//		ITypeBinding interfaceBinding= getTypeBinding(theInterface, astManager);
		ICompilationUnit[] referringCus= getCusToParse(theClass, theInterface, pm, astManager);
		ITypeConstraint[] constraints= getConstraints(referringCus, astManager);
		return getUpdatableVariables(constraints, classBinding, interfaceBinding);
	}

//	for (int i= 0; i < constraints.length; i++) {
//		System.out.println("-----------------------------");
//		System.out.println("constraint: " + constraints[i].toString());
//		System.out.println("resolved:   " + constraints[i].toResolvedString());
//		System.out.println("satisfied:  " + constraints[i].isSatisfied());
//	}
	
	//TODO move to TypeBindings
	private static ITypeBinding getSuperTypeBinding(ITypeBinding typeBinding, IType superType) {
		Set setOfAll= TypeBindings.getSuperTypes(typeBinding);
		ITypeBinding[] all= (ITypeBinding[]) setOfAll.toArray(new ITypeBinding[setOfAll.size()]);
		for (int i= 0; i < all.length; i++) {
			ITypeBinding superTypeBinding= all[i];
			if (isBindingForType(superTypeBinding, superType))
				return superTypeBinding;
		}
		return null;
	}

	//TODO move to TypeBindings
	private static boolean isBindingForType(ITypeBinding typeBinding, IType type) {
		if (! typeBinding.getName().equals(type.getElementName()))
			return false;
		if (! typeBinding.getPackage().getName().equals(type.getPackageFragment().getElementName()))
			return false;
		return true;
	}

	//TODO to be deleted
	public 	static Set updateReferences(TextChangeManager manager, IType theClass, IType theInterface, ASTNodeMappingManager astManager, IProgressMonitor pm) throws CoreException{
		ConstraintVariable[] updatableVars= getUpdatableVariables(theClass, theInterface, astManager, pm);
		ASTNode[] nodes= getNodes(updatableVars, theClass.getJavaProject(), astManager);//XXX scope is bogus
		Set updatedCus= new HashSet(0);
		for (int i= 0; i < nodes.length; i++) {
			ASTNode node= nodes[i];
			ICompilationUnit cu= astManager.getCompilationUnit(node);
			manager.get(cu).addTextEdit("update", createTypeUpdateEdit(new SourceRange(node), theClass.getElementName(), theInterface.getElementName()));	 //$NON-NLS-1$
			updatedCus.add(cu);
		}
//		for (Iterator iter= updatedCus.iterator(); iter.hasNext();) {
//			ICompilationUnit cu= (ICompilationUnit) iter.next();
//			if (needsImport(cu))
//				addSupertypeImport(manager, cu);			
//		}
		return new HashSet(Arrays.asList(nodes));
	}

	//TODO to be deleted
	private static TextEdit createTypeUpdateEdit(ISourceRange sourceRange, String className, String interfaceName) {
		return new UpdateTypeReferenceEdit(sourceRange.getOffset(), sourceRange.getLength(), interfaceName, className);
	}
	
	public static ASTNode[] getUpdatableNodes(IType theClass, IType theInterface, IProgressMonitor pm) throws JavaModelException{
		ASTNodeMappingManager astManager= new ASTNodeMappingManager();
		ConstraintVariable[] updatableVars= getUpdatableVariables(theClass, theInterface, astManager, pm);
		return getNodes(updatableVars, theClass.getJavaProject(), astManager);//XXX scope is bogus
	}

	public static CompilationUnitRange[] getUpdatableRanges(IType theClass, IType theInterface, IProgressMonitor pm) throws JavaModelException{
		ASTNodeMappingManager astManager= new ASTNodeMappingManager();
		ConstraintVariable[] updatableVars= getUpdatableVariables(theClass, theInterface, astManager, pm);
		return getRanges(updatableVars, theClass.getJavaProject(), astManager);//XXX scope is bogus
	}

	private static ConstraintVariable[] getUpdatableVariables(ITypeConstraint[] constraints, ITypeBinding classBinding, ITypeBinding interfaceBinding){
		Set setOfAll= new HashSet(Arrays.asList(getAllOfType(constraints, classBinding)));
		ConstraintVariable[] initialBad= getInitialBad(setOfAll, constraints, interfaceBinding);
		Set bad= new HashSet();
		bad.addAll(Arrays.asList(initialBad));
		if (bad.isEmpty())//TODO to be removed after it's optimized
			return (ConstraintVariable[]) setOfAll.toArray(new ConstraintVariable[setOfAll.size()]);
		boolean repeat= false;
		do{
			//TODO can optimize here - don't have to walk the whole constraints array, bad would be enough
			int sizeOfBad= bad.size();
			for (int i= 0; i < constraints.length; i++) {
				ITypeConstraint constraint= constraints[i];
				if(! constraint.isSimpleTypeConstraint())
					continue;
				SimpleTypeConstraint con= (SimpleTypeConstraint)constraint;
				ConstraintVariable left= con.getLeft();
				ConstraintVariable right= con.getRight();
				if (setOfAll.contains(left) && bad.contains(right) && ! bad.contains(left))
					bad.add(left);
				if (con.isEqualsConstraint() || con.isDefinesConstraint()){
					if (setOfAll.contains(right) && bad.contains(left) && ! bad.contains(right))
						bad.add(right);
				}
			}
			repeat= sizeOfBad < bad.size();
		} while(repeat);
		Set updatable= new HashSet(setOfAll);
		updatable.removeAll(bad);
		return (ConstraintVariable[]) updatable.toArray(new ConstraintVariable[updatable.size()]);
	}
	
	private static ConstraintVariable[] getInitialBad(Set setOfAll, ITypeConstraint[] constraints, ITypeBinding interfaceBinding){
		ConstraintVariable interfaceVariable= new RawBindingVariable(interfaceBinding);
		Set result= new HashSet();
		for (int i= 0; i < constraints.length; i++) {
			ITypeConstraint constraint= constraints[i];
			if (constraint.isSimpleTypeConstraint()){
				SimpleTypeConstraint simple= (SimpleTypeConstraint)constraint;
				if (simple.isSubtypeConstraint() && canAddLeftSideToInitialBadSet(simple, setOfAll, interfaceVariable))
					result.add(simple.getLeft());
			} else if (constraint instanceof CompositeOrTypeConstraint){
				ITypeConstraint[] subConstraints= ((CompositeOrTypeConstraint)constraint).getConstraints();
				if (canAddLeftSideToInitialBadSet(subConstraints, setOfAll, interfaceVariable))
					result.add(((SimpleTypeConstraint)subConstraints[0]).getLeft());
			}
		}
		return (ConstraintVariable[]) result.toArray(new ConstraintVariable[result.size()]);
	}
	
	private static boolean canAddLeftSideToInitialBadSet(SimpleTypeConstraint sc, Set setOfAll, ConstraintVariable interfaceVariable) {
		ConstraintVariable left= sc.getLeft();
		ConstraintVariable right= sc.getRight();
		if (! (left instanceof ExpressionVariable))
			return false;
		else if (! setOfAll.contains(left))	
			return false;
		else if (interfaceVariable.isSubtypeOf(right))
			return false;
		else if ((right instanceof ExpressionVariable || right instanceof TypeVariable) && setOfAll.contains(right))
			return false;
		else if (! (right instanceof RawBindingVariable) && !( right instanceof DeclaringTypeVariable))
			return false;//TODO this case is very strange and thus probably bogus
		else
			return true;
	}

	private static boolean canAddLeftSideToInitialBadSet(ITypeConstraint[] subConstraints, Set setOfAll, ConstraintVariable interfaceVariable) {
		if (subConstraints.length == 0)
			return false;
		if (! allAreSimpleConstraints(subConstraints))
			return false;
		
		SimpleTypeConstraint[] simpleTypeConstraints= toSimpleConstraintArray(subConstraints);
		if (! allHaveSameLeftSide(simpleTypeConstraints))
			return false;
		
		ConstraintVariable left= (simpleTypeConstraints[0]).getLeft();
		if (! (left instanceof ExpressionVariable))
			return false;
		if (! setOfAll.contains(left))
			return false;
		if (rightSideIsSubtypeOf(simpleTypeConstraints, interfaceVariable))
			return false;
		return true;
	}

	private static boolean rightSideIsSubtypeOf(SimpleTypeConstraint[] simpleTypeConstraints, ConstraintVariable interfaceVariable) {
		for (int i= 0; i < simpleTypeConstraints.length; i++) {
			ConstraintVariable right= simpleTypeConstraints[i].getRight();
			if (right.isSubtypeOf(interfaceVariable))
				return true;
		}
		return false;
	}
	
	private static SimpleTypeConstraint[] toSimpleConstraintArray(ITypeConstraint[] subConstraints) {
		List result= Arrays.asList(subConstraints);
		return (SimpleTypeConstraint[]) result.toArray(new SimpleTypeConstraint[result.size()]);
	}

	private static boolean allAreSimpleConstraints(ITypeConstraint[] subConstraints) {
		for (int i= 0; i < subConstraints.length; i++) {
			ITypeConstraint constraint= subConstraints[i];
			if (! constraint.isSimpleTypeConstraint())
				return false;
		}
		return true;
	}

	private static boolean allHaveSameLeftSide(SimpleTypeConstraint[] constraints) {
		Assert.isTrue(constraints.length > 0);
		ConstraintVariable first= constraints[0].getLeft();
		for (int i= 1; i < constraints.length; i++) {
			if (! first.equals(constraints[i].getLeft()))
				return false;
		}
		return true;
	}

	private static ITypeConstraint[] getConstraints(ICompilationUnit[] referringCus, ASTNodeMappingManager astManager) {
		Set result= new HashSet();
		for (int i= 0; i < referringCus.length; i++) {
			ICompilationUnit unit= referringCus[i];
			ConstraintCollector collector= new ConstraintCollector();
			astManager.getAST(unit).accept(collector);
			result.addAll(Arrays.asList(collector.getConstraints()));
		}
		return (ITypeConstraint[]) result.toArray(new ITypeConstraint[result.size()]);
	}
	
	private static ICompilationUnit[] getCusToParse(IType theClass, IType theInterface, IProgressMonitor pm, ASTNodeMappingManager astManager) throws JavaModelException{
		try{
			pm.beginTask("", 2);
			ISearchPattern pattern= SearchEngine.createSearchPattern(theClass, IJavaSearchConstants.REFERENCES);
			IJavaSearchScope scope= RefactoringScopeFactory.create(theClass);
			ICompilationUnit[] workingCopies= getWorkingCopies(theClass.getCompilationUnit(), theInterface.getCompilationUnit());
			if (workingCopies.length == 0)
				workingCopies= null;
			SearchResultGroup[] groups= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), scope, pattern, workingCopies);
			ASTNode[] typeReferenceNodes= ASTNodeSearchUtil.getAstNodes(groups, astManager);
			ICompilationUnit[] typeReferecingCus= getCus(groups);
			ICompilationUnit[] fieldAndMethodReferringCus= fieldAndMethodReferringCus(theClass, typeReferenceNodes, astManager, new SubProgressMonitor(pm, 1));
			return merge(fieldAndMethodReferringCus, typeReferecingCus);
		} finally{
			pm.done();
		}
	}
	
	private static ICompilationUnit[] fieldAndMethodReferringCus(IType theClass, ASTNode[] typeReferenceNodes, ASTNodeMappingManager astManager, IProgressMonitor pm) throws JavaModelException {
		ISearchPattern pattern= createPatternForReferencingFieldsAndMethods(typeReferenceNodes, astManager);
		if (pattern == null)
			return new ICompilationUnit[0];
		IJavaSearchScope scope= RefactoringScopeFactory.create(theClass);
		return RefactoringSearchEngine.findAffectedCompilationUnits(pm, scope, pattern);
	}

	private static ISearchPattern createPatternForReferencingFieldsAndMethods(ASTNode[] typeReferenceNodes, ASTNodeMappingManager astManager) throws JavaModelException {
		IField[] fields= getReferencingFields(typeReferenceNodes, astManager);
		IMethod[] methods= getReferencingMethods(typeReferenceNodes, astManager);
		ISearchPattern fieldPattern= RefactoringSearchEngine.createSearchPattern(fields, IJavaSearchConstants.ALL_OCCURRENCES);
		ISearchPattern methodPattern= RefactoringSearchEngine.createSearchPattern(methods, IJavaSearchConstants.ALL_OCCURRENCES);
		if (fieldPattern == null)
			return methodPattern;
		if (methodPattern == null)
			return fieldPattern;
		return SearchEngine.createOrSearchPattern(fieldPattern, methodPattern);	
	}

	private static IMethod[] getReferencingMethods(ASTNode[] typeReferenceNodes, ASTNodeMappingManager astManager) throws JavaModelException {
		List result= new ArrayList();
		for (int i= 0; i < typeReferenceNodes.length; i++) {
			ASTNode node= typeReferenceNodes[i];
			IJavaProject scope= astManager.getCompilationUnit(node).getJavaProject();
			IMethod method= getMethod(node, scope);
			if (method != null)
				result.add(method);
		}
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}

	private static IField[] getReferencingFields(ASTNode[] typeReferenceNodes, ASTNodeMappingManager astManager) throws JavaModelException {
		List result= new ArrayList();
		for (int i= 0; i < typeReferenceNodes.length; i++) {
			ASTNode node= typeReferenceNodes[i];
			IJavaProject scope= astManager.getCompilationUnit(node).getJavaProject();
			IField[] fields= getFields(node, scope);
			result.addAll(Arrays.asList(fields));
		}
		return (IField[]) result.toArray(new IField[result.size()]);
	}

	private static IMethod getMethod(ASTNode node, IJavaProject scope) throws JavaModelException {
		if (node instanceof Type && node.getParent() instanceof MethodDeclaration){
			MethodDeclaration declaration= (MethodDeclaration)node.getParent();
			IMethodBinding binding= declaration.resolveBinding();
			if (binding != null)
				return Bindings.findMethod(binding, scope);
		} else if (node instanceof Type && isMethodParameter(node.getParent())){
			MethodDeclaration declaration= (MethodDeclaration)node.getParent().getParent();
			IMethodBinding binding= declaration.resolveBinding();
			if (binding != null)
				return Bindings.findMethod(binding, scope);
		}
		return null;
	}

	private static boolean isMethodParameter(ASTNode node){
		return (node instanceof VariableDeclaration) && 
			   (node.getParent() instanceof MethodDeclaration) &&
				((MethodDeclaration)node.getParent()).parameters().indexOf(node) != -1;
	}
	
	private static IField[] getFields(ASTNode node, IJavaProject scope) throws JavaModelException {
		if (node instanceof Type && node.getParent() instanceof FieldDeclaration){
			FieldDeclaration parent= (FieldDeclaration)node.getParent();
			if (parent.getType() == node){
				List result= new ArrayList(parent.fragments().size());
				for (Iterator iter= parent.fragments().iterator(); iter.hasNext();) {
					VariableDeclarationFragment fragment= (VariableDeclarationFragment) iter.next();
					IField field= getField(fragment, scope);
					if (field != null)
						result.add(field);
				}
				return (IField[]) result.toArray(new IField[result.size()]);
			}
		}  
		return new IField[0];
	}

	private static IField getField(VariableDeclarationFragment fragment, IJavaProject in) throws JavaModelException {
		IBinding binding= (IBinding)fragment.getName().resolveBinding();
		if (! (binding instanceof IVariableBinding))
			return null;
		IVariableBinding variableBinding= (IVariableBinding)binding;
		if (! variableBinding.isField())
			return null;
		return Bindings.findField(variableBinding, in);
	}

	private static ICompilationUnit[] merge(ICompilationUnit[] array1, ICompilationUnit[] array2){
		Set result= new HashSet();
		result.addAll(Arrays.asList(array1));
		result.addAll(Arrays.asList(array2));
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}
	
	private static ICompilationUnit[] getCus(SearchResultGroup[] groups) {
		List result= new ArrayList(groups.length);
		for (int i= 0; i < groups.length; i++) {
			SearchResultGroup group= groups[i];
			ICompilationUnit cu= group.getCompilationUnit();
			if (cu != null)
				result.add(WorkingCopyUtil.getWorkingCopyIfExists(cu));
		}
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}

	private static ICompilationUnit[] getWorkingCopies(ICompilationUnit precedingWC1, ICompilationUnit precedingWC2) {
		// XXX: This is a layer breaker - should not access jdt.ui
		IWorkingCopy[] copies= JavaUI.getSharedWorkingCopiesOnClasspath();
		Set result= new HashSet(copies.length);
		if (precedingWC1 != null && precedingWC1.isWorkingCopy())
			result.add(precedingWC1);
		if (precedingWC2 != null && precedingWC2.isWorkingCopy())
			result.add(precedingWC2);
		ICompilationUnit original1= WorkingCopyUtil.getOriginal(precedingWC1);
		ICompilationUnit original2= WorkingCopyUtil.getOriginal(precedingWC2);
		for (int i= 0; i < copies.length; i++) {
			IWorkingCopy copy= copies[i];
			if (copy.isWorkingCopy() && copy instanceof ICompilationUnit){
				IJavaElement original= copy.getOriginalElement();
				if (!original.equals(original1) && !original.equals(original2))
					result.add(copy);
			}
		}
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}

	private static ITypeBinding getTypeBinding(IType theType, ASTNodeMappingManager astManager) throws JavaModelException {
		return ASTNodeSearchUtil.getTypeDeclarationNode(theType, astManager).resolveBinding();
	}
	
	//-- which nodes to update ----//
	
	private static CompilationUnitRange[] getRanges(ConstraintVariable[] variables, IJavaProject scope, ASTNodeMappingManager astManager) throws JavaModelException {
		//TODO do it in a more lighweight way. we don't need ast nodes - we just ranges
		ASTNode[] nodes= getNodes(variables, scope, astManager);
		CompilationUnitRange[] result= new CompilationUnitRange[nodes.length];
		for (int i= 0; i < nodes.length; i++) {
			ASTNode node= nodes[i];
			ICompilationUnit cu= astManager.getCompilationUnit(node);
			result[i]= new CompilationUnitRange(cu, node);
		}
		return result;
	}
	
	private static ASTNode[] getNodes(ConstraintVariable[] variables, IJavaProject scope, ASTNodeMappingManager astManager) throws JavaModelException {
		Set nodes= new HashSet();
		for (int i= 0; i < variables.length; i++) {
			ConstraintVariable variable= variables[i];
			ASTNode node= getNode(variable, scope, astManager);
			if (node != null)
				nodes.add(node);
		}
		return (ASTNode[]) nodes.toArray(new ASTNode[nodes.size()]);
	}

	private static ASTNode getNode(ConstraintVariable variable, IJavaProject scope, ASTNodeMappingManager astManager) throws JavaModelException {
		if (variable instanceof DeclaringTypeVariable)
			return null;
		else if (variable instanceof RawBindingVariable)
			return null;
		else if (variable instanceof ParameterTypeVariable)
			return getNode((ParameterTypeVariable)variable, scope, astManager);
		else if (variable instanceof ReturnTypeVariable)
			return getNode((ReturnTypeVariable)variable, scope, astManager);
		else if (variable instanceof TypeVariable)
			return ((TypeVariable)variable).getType();
		else //TODO is this enough?
			return null;
	}
	
	private static ASTNode getNode(ReturnTypeVariable variable, IJavaProject scope, ASTNodeMappingManager astManager) throws JavaModelException {
		IMethodBinding methodBinding= variable.getMethodBinding();
		MethodDeclaration declaration= findMethodDeclaration(methodBinding, scope, astManager);
		if (declaration == null)
			return null;
		return declaration.getReturnType();
	}

	private static ASTNode getNode(ParameterTypeVariable variable, IJavaProject scope, ASTNodeMappingManager astManager) throws JavaModelException{
		IMethodBinding method= variable.getMethodBinding();
		int paramIndex= variable.getParameterIndex();
		MethodDeclaration declaration= findMethodDeclaration(method, scope, astManager);
		if (declaration == null)
			return null;
		return ((SingleVariableDeclaration)declaration.parameters().get(paramIndex)).getType();
	}

	private static MethodDeclaration findMethodDeclaration(IMethodBinding methodBinding, IJavaProject scope, ASTNodeMappingManager astManager) throws JavaModelException {
		IMethod method= Bindings.findMethod(methodBinding, scope);
		if (method == null)
			return null;
		return ASTNodeSearchUtil.getMethodDeclarationNode(method, astManager);
	}
}