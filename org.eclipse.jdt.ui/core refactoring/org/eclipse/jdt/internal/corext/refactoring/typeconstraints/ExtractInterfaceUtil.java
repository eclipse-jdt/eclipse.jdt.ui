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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
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
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeMappingManager;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;


public final class ExtractInterfaceUtil {

	private ExtractInterfaceUtil(){}
	
	public static ConstraintVariable[] getAllOfType(ITypeConstraint[] constraints, ITypeBinding binding){
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
				//TODO
			}
		}
		return (ConstraintVariable[]) result.toArray(new ConstraintVariable[result.size()]);
	}

	
//---------------------------------------------------------//
	public static ConstraintVariable[] getUpdatable(IType theClass, IType theInterface, IProgressMonitor pm) throws JavaModelException{
		ASTNodeMappingManager astManager= new ASTNodeMappingManager();
		ITypeBinding classBinding= getTypeBinding(theClass, astManager);
		ITypeBinding interfaceBinding= getTypeBinding(theInterface, astManager);
		ICompilationUnit[] referringCus= getCusToParse(theClass, pm, astManager);
		ITypeConstraint[] constraints= getConstraints(referringCus, astManager);
//		for (int i= 0; i < constraints.length; i++) {
//			System.out.println("-----------------------------");
//			System.out.println("constraint: " + constraints[i].toString());
//			System.out.println("resolved:   " + constraints[i].toResolvedString());
//			System.out.println("satisfied:  " + constraints[i].isSatisfied());
//		}

		return getUpdatable(constraints, classBinding, interfaceBinding);
	}

	private static ConstraintVariable[] getUpdatable(ITypeConstraint[] constraints, ITypeBinding classBinding, ITypeBinding interfaceBinding){
		Set setOfAll= new HashSet(Arrays.asList(getAllOfType(constraints, classBinding)));
		ConstraintVariable[] initialBad= getInitialBad(setOfAll, constraints, interfaceBinding);
		Set bad= new HashSet();
		bad.addAll(Arrays.asList(initialBad));
		boolean repeat= false;
		do{
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
				if (constraint instanceof EqualsConstraint || constraint instanceof DefinesConstraint){
					if (setOfAll.contains(right) && bad.contains(left) && ! bad.contains(right))
						bad.add(right);
				}
			}
			repeat= sizeOfBad < bad.size();
		} while(repeat);
		System.out.println("\nall:" + setOfAll);
		System.out.println("bad:" + bad);
		Set updatable= new HashSet(setOfAll);
		updatable.removeAll(bad);
		System.out.println("good:"+ updatable);
		return (ConstraintVariable[]) updatable.toArray(new ConstraintVariable[updatable.size()]);
	}
	
	public static ConstraintVariable[] getInitialBad(Set setOfAll, ITypeConstraint[] constraints, ITypeBinding interfaceBinding){
		ConstraintVariable interfaceVariable= new RawBindingVariable(interfaceBinding);
		Set result= new HashSet();
		for (int i= 0; i < constraints.length; i++) {
			ITypeConstraint constraint= constraints[i];
			if (constraint instanceof SubtypeConstraint){
				SubtypeConstraint sc= (SubtypeConstraint)constraint;
				ConstraintVariable left= sc.getLeft();
				ConstraintVariable right= sc.getRight();
				if (left instanceof ExpressionVariable && right instanceof TypeVariable && setOfAll.contains(left) && ! setOfAll.contains(right) && ! interfaceVariable.isSubtypeOf(right))
					result.add(left);
				else if (left instanceof ExpressionVariable && right instanceof ExpressionVariable && setOfAll.contains(left) && ! setOfAll.contains(right) && ! interfaceVariable.isSubtypeOf(right)) 
					result.add(left);
				else if (left instanceof ExpressionVariable && right instanceof RawBindingVariable && setOfAll.contains(left) && ! interfaceVariable.isSubtypeOf(right))
					result.add(left);
				else if (left instanceof ExpressionVariable && right instanceof DeclaringTypeVariable && setOfAll.contains(left) && ! interfaceVariable.isSubtypeOf(right))
					result.add(left);
			} //TODO handle OR constraints
		}
		return (ConstraintVariable[]) result.toArray(new ConstraintVariable[result.size()]);
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
	
	private static ICompilationUnit[] getCusToParse(IType theClass, IProgressMonitor pm, ASTNodeMappingManager astManager) throws JavaModelException{
		try{
			pm.beginTask("", 2);
			ISearchPattern pattern= SearchEngine.createSearchPattern(theClass, IJavaSearchConstants.REFERENCES);
			IJavaSearchScope scope= RefactoringScopeFactory.create(theClass);
			ICompilationUnit[] workingCopies= getWorkingCopies();
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
		//TODO scope
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
				return Binding2JavaModel.find(binding, scope);
		} else if (node instanceof Type && isMethodParameter(node.getParent())){
			MethodDeclaration declaration= (MethodDeclaration)node.getParent().getParent();
			IMethodBinding binding= declaration.resolveBinding();
			if (binding != null)
				return Binding2JavaModel.find(binding, scope);
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
		return Binding2JavaModel.find(variableBinding, in);
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

	private static ICompilationUnit[] getWorkingCopies() {
		// XXX: This is a layer breaker - should not access jdt.ui
		IWorkingCopy[] copies= JavaUI.getSharedWorkingCopiesOnClasspath();
		List result= new ArrayList(copies.length);
		for (int i= 0; i < copies.length; i++) {
			IWorkingCopy copy= copies[i];
			if (copy instanceof ICompilationUnit)
				result.add(copy);
		}
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}

	private static ITypeBinding getTypeBinding(IType theType, ASTNodeMappingManager astManager) throws JavaModelException {
		return ASTNodeSearchUtil.getTypeDeclarationNode(theType, astManager).resolveBinding();
	}
}
