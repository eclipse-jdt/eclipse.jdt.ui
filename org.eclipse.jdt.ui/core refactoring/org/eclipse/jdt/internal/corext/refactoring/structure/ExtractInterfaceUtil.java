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
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextBufferChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.UpdateTypeReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompositeOrTypeConstraint;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintCollector;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.DeclaringTypeVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ExpressionVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.FullConstraintCreator;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ITypeConstraint;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ParameterTypeVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.RawBindingVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ReturnTypeVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.SimpleTypeConstraint;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.TypeBindings;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.TypeVariable;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

class ExtractInterfaceUtil {

	private final ICompilationUnit fInputTypeWorkingCopy;
	private final ICompilationUnit fSupertypeWorkingCopy;//can be null
	private final ASTNodeMappingManager fASTManager;
	
	private ExtractInterfaceUtil(ICompilationUnit inputTypeWorkingCopy, ICompilationUnit supertypeWorkingCopy, ASTNodeMappingManager astManager){
		Assert.isNotNull(inputTypeWorkingCopy);
		fSupertypeWorkingCopy= supertypeWorkingCopy;
		fInputTypeWorkingCopy= inputTypeWorkingCopy;
		fASTManager= astManager;
	}
	
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

				if (simple.getRight().getBinding() != null && simple.getRight().getBinding().isArray() && Bindings.equals(simple.getRight().getBinding().getElementType(), typeBinding))
					result.add(simple.getRight());
				if (simple.getLeft().getBinding() != null && simple.getLeft().getBinding().isArray() && Bindings.equals(simple.getLeft().getBinding().getElementType(), typeBinding))
					result.add(simple.getLeft());
					
			} else {
				CompositeOrTypeConstraint cotc= (CompositeOrTypeConstraint)constraint;
				result.addAll(Arrays.asList(getAllOfType(cotc.getConstraints(), binding)));
			}
		}
		return (ConstraintVariable[]) result.toArray(new ConstraintVariable[result.size()]);
	}

	private ConstraintVariable[] getUpdatableVariables(IType theClass, IType theInterface, IProgressMonitor pm) throws JavaModelException{
		ITypeBinding typeBinding= getTypeBinding(theClass);
		ITypeBinding interfaceBinding= getSuperTypeBinding(typeBinding, theInterface);
		ICompilationUnit[] referringCus= getCusToParse(theClass, theInterface, pm);
		ITypeConstraint[] constraints= getConstraints(referringCus);
		return getUpdatableVariables(constraints, typeBinding, interfaceBinding);
	}

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

	private static boolean isBindingForType(ITypeBinding typeBinding, IType type) {
		if (! typeBinding.getName().equals(type.getElementName()))
			return false;
		if (! typeBinding.getPackage().getName().equals(type.getPackageFragment().getElementName()))
			return false;
		return true;
	}

	public static CompilationUnitRange[] updateReferences(TextChangeManager manager, IType inputType, IType supertypeToUse, WorkingCopyOwner workingCopyOwner, boolean updateInputTypeCu, IProgressMonitor pm) throws CoreException{
		ASTNodeMappingManager astManager= new ASTNodeMappingManager(workingCopyOwner);
		ICompilationUnit typeWorkingCopy= inputType.getCompilationUnit();
		ExtractInterfaceUtil inst= new ExtractInterfaceUtil(typeWorkingCopy, supertypeToUse.getCompilationUnit(), astManager);
		ConstraintVariable[] updatableVars= inst.getUpdatableVariables(inputType, supertypeToUse, pm);
		ASTNode[] nodes= inst.getNodes(updatableVars, inputType);
		String typeName= inputType.getElementName();
		String superTypeName= supertypeToUse.getElementName();
		for (int i= 0; i < nodes.length; i++) {
			ASTNode node= nodes[i];
			ICompilationUnit cu= astManager.getCompilationUnit(node);
			if (updateInputTypeCu || ! cu.equals(typeWorkingCopy)){
				getTextChange(manager, cu).addTextEdit("update", createTypeUpdateEdit(new SourceRange(node), typeName, superTypeName));	 //$NON-NLS-1$
			}
		}
		CompilationUnitRange[] ranges= new CompilationUnitRange[nodes.length];
		for (int i= 0; i < nodes.length; i++) {
			ranges[i]= new CompilationUnitRange(astManager.getCompilationUnit(nodes[i]), nodes[i]);
		}
		return ranges;
	}

	public static TextChange getTextChange(TextChangeManager manager, ICompilationUnit cu) throws CoreException {
		//XXX workaround for bug 39630 CompilationUnitChange cannot handle in-memory-only compilation units 
		if (manager.containsChangesIn(cu) || cu.getResource().exists())
			return manager.get(cu);
		TextBuffer textBuffer= TextBuffer.create(cu.getSource()); 
		TextChange textChange= new TextBufferChange(cu.getElementName(), textBuffer);
		manager.manage(cu, textChange);
		return textChange;
	}

	//TODO to be deleted
	private static TextEdit createTypeUpdateEdit(ISourceRange sourceRange, String className, String interfaceName) {
		return new UpdateTypeReferenceEdit(sourceRange.getOffset(), sourceRange.getLength(), interfaceName, className);
	}
	
	private static ConstraintVariable[] getUpdatableVariables(ITypeConstraint[] constraints, ITypeBinding classBinding, ITypeBinding interfaceBinding){
		Set setOfAll= new HashSet(Arrays.asList(getAllOfType(constraints, classBinding)));
		ConstraintVariable[] initialBad= getInitialBad(setOfAll, constraints, interfaceBinding);
		if (initialBad == null || initialBad.length == 0)//TODO to be removed after it's optimized
			return (ConstraintVariable[]) setOfAll.toArray(new ConstraintVariable[setOfAll.size()]);
		Set bad= new HashSet();
		bad.addAll(Arrays.asList(initialBad));
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
		//DeclaringTypeVariables are different - they can never be updated by this refactoring
		else if (setOfAll.contains(right) && ! (right instanceof DeclaringTypeVariable))
			return false;
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
			if (! subConstraints[i].isSimpleTypeConstraint())
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

	private ITypeConstraint[] getConstraints(ICompilationUnit[] referringCus) {
		Set result= new HashSet();
		for (int i= 0; i < referringCus.length; i++) {
			ICompilationUnit unit= referringCus[i];
			ConstraintCollector collector= new ConstraintCollector(new ExtractInterfaceConstraintCreator());
			fASTManager.getAST(unit).accept(collector);
			result.addAll(Arrays.asList(collector.getConstraints()));
		}
		return (ITypeConstraint[]) result.toArray(new ITypeConstraint[result.size()]);
	}
	
	private ICompilationUnit[] getCusToParse(IType theClass, IType theInterface, IProgressMonitor pm) throws JavaModelException{
		try{
			pm.beginTask("", 2);
			ISearchPattern pattern= SearchEngine.createSearchPattern(theClass, IJavaSearchConstants.REFERENCES);
			IJavaSearchScope scope= RefactoringScopeFactory.create(theClass);
			ICompilationUnit[] workingCopies= getWorkingCopies(theClass.getCompilationUnit(), theInterface.getCompilationUnit());
			if (workingCopies.length == 0)
				workingCopies= null;
			SearchResultGroup[] groups= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), scope, pattern, workingCopies);
			ASTNode[] typeReferenceNodes= getAstNodes(groups);
			ICompilationUnit[] typeReferecingCus= getCus(groups);
			ICompilationUnit[] fieldAndMethodReferringCus= fieldAndMethodReferringCus(theClass, typeReferenceNodes, workingCopies, new SubProgressMonitor(pm, 1));
			return merge(fieldAndMethodReferringCus, typeReferecingCus);
		} finally{
			pm.done();
		}
	}

	private ASTNode[] getAstNodes(SearchResultGroup[] searchResultGroups) {
		List result= new ArrayList();
		for (int i= 0; i < searchResultGroups.length; i++) {
			ICompilationUnit referencedCu= searchResultGroups[i].getCompilationUnit();
			if (referencedCu == null)
				continue;
			result.addAll(Arrays.asList(ASTNodeSearchUtil.getAstNodes(searchResultGroups[i].getSearchResults(), fASTManager.getAST(referencedCu))));
		}
		return (ASTNode[]) result.toArray(new ASTNode[result.size()]);	
	}	
	
	private ICompilationUnit[] fieldAndMethodReferringCus(IType theClass, ASTNode[] typeReferenceNodes, ICompilationUnit[] wcs, IProgressMonitor pm) throws JavaModelException {
		ISearchPattern pattern= createPatternForReferencingFieldsAndMethods(typeReferenceNodes);
		if (pattern == null)
			return new ICompilationUnit[0];
		IJavaSearchScope scope= RefactoringScopeFactory.create(theClass);
		ICompilationUnit[] units= RefactoringSearchEngine.findAffectedCompilationUnits(pm, scope, pattern);
		Set result= new HashSet(units.length);
		for (int i= 0; i < units.length; i++) {
			result.add(getUnproceededElement(units[i], wcs));
		}
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}

	private static ICompilationUnit getUnproceededElement(ICompilationUnit unit, ICompilationUnit[] wcs) {
		if (wcs == null) 
			return unit;
		for (int i= 0; i < wcs.length; i++) {
			if (proceeds(wcs[i], unit))	
				return wcs[i];
		}
		return unit;
	}

	private static boolean proceeds(ICompilationUnit wc, ICompilationUnit unit) {
		return wc.getResource() == null || wc.getResource().equals(unit.getResource());			
	}

	private ISearchPattern createPatternForReferencingFieldsAndMethods(ASTNode[] typeReferenceNodes) throws JavaModelException {
		IField[] fields= getReferencingFields(typeReferenceNodes);
		IMethod[] methods= getReferencingMethods(typeReferenceNodes);
		ISearchPattern fieldPattern= RefactoringSearchEngine.createSearchPattern(fields, IJavaSearchConstants.ALL_OCCURRENCES);
		ISearchPattern methodPattern= RefactoringSearchEngine.createSearchPattern(methods, IJavaSearchConstants.ALL_OCCURRENCES);
		if (fieldPattern == null)
			return methodPattern;
		if (methodPattern == null)
			return fieldPattern;
		return SearchEngine.createOrSearchPattern(fieldPattern, methodPattern);	
	}

	private IMethod[] getReferencingMethods(ASTNode[] typeReferenceNodes) throws JavaModelException {
		List result= new ArrayList();
		for (int i= 0; i < typeReferenceNodes.length; i++) {
			ASTNode node= typeReferenceNodes[i];
			IJavaProject scope= fASTManager.getCompilationUnit(node).getJavaProject();
			IMethod method= getMethod(node, scope);
			if (method != null)
				result.add(method);
		}
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}

	private IField[] getReferencingFields(ASTNode[] typeReferenceNodes) throws JavaModelException {
		List result= new ArrayList();
		for (int i= 0; i < typeReferenceNodes.length; i++) {
			ASTNode node= typeReferenceNodes[i];
			IJavaProject scope= fASTManager.getCompilationUnit(node).getJavaProject();
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
				((MethodDeclaration)node.getParent()).parameters().contains(node);
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

	private ITypeBinding getTypeBinding(IType theType) throws JavaModelException {
		return getTypeDeclarationNode(theType).resolveBinding();
	}

	private TypeDeclaration getTypeDeclarationNode(IType iType) throws JavaModelException {
		return ASTNodeSearchUtil.getTypeDeclarationNode(iType, fASTManager.getAST(iType.getCompilationUnit()));
	}

	private MethodDeclaration getMethodDeclarationNode(IMethod iMethod) throws JavaModelException {
		return ASTNodeSearchUtil.getMethodDeclarationNode(iMethod, fASTManager.getAST(iMethod.getCompilationUnit()));
	}

	//-- which nodes to update ----//
	
	private ASTNode[] getNodes(ConstraintVariable[] variables, IType inputType) throws JavaModelException {
		Set nodes= new HashSet();
		IJavaProject scope= inputType.getJavaProject();//XXX scope is bogus
		ITypeBinding inputTypeBinding= getTypeBinding(inputType);
		for (int i= 0; i < variables.length; i++) {
			ASTNode node= getNode(variables[i], scope, inputTypeBinding);
			if (node != null)
				nodes.add(node);
		}
		return (ASTNode[]) nodes.toArray(new ASTNode[nodes.size()]);
	}

	//TODO Should this be on ConstraintVariable?
	private ASTNode getNode(ConstraintVariable variable, IJavaProject scope, ITypeBinding inputTypeBinding) throws JavaModelException {
		if (variable instanceof DeclaringTypeVariable)
			return null;
		else if (variable instanceof RawBindingVariable)
			return null;
		else if (variable instanceof ParameterTypeVariable)
			return getNode((ParameterTypeVariable)variable, scope);
		else if (variable instanceof ReturnTypeVariable)
			return getNode((ReturnTypeVariable)variable, scope);
		else if (variable instanceof TypeVariable)
			return ASTNodes.getElementType(((TypeVariable)variable).getType());
		else if (variable instanceof ExpressionVariable)
			return getNode((ExpressionVariable)variable, inputTypeBinding);
		else //TODO is this enough?
			return null;
	}
	
	private static ASTNode getNode(ExpressionVariable variable, ITypeBinding inputTypeBinding) {
		if (inputTypeBinding == null) return null;
		Expression expression= variable.getExpression();
		if (expression instanceof Name){
			if (Bindings.equals(inputTypeBinding, ((Name)expression).resolveBinding()))
				return expression;
		}
		return null;
	}

	private Type getNode(ReturnTypeVariable variable, IJavaProject scope) throws JavaModelException {
		IMethodBinding methodBinding= variable.getMethodBinding();
		MethodDeclaration declaration= findMethodDeclaration(methodBinding, scope);
		if (declaration == null)
			return null;
		return ASTNodes.getElementType(declaration.getReturnType());
	}
	
	private Type getNode(ParameterTypeVariable variable, IJavaProject scope) throws JavaModelException{
		IMethodBinding method= variable.getMethodBinding();
		int paramIndex= variable.getParameterIndex();
		MethodDeclaration declaration= findMethodDeclaration(method, scope);
		if (declaration == null)
			return null;
		return ASTNodes.getElementType(((SingleVariableDeclaration)declaration.parameters().get(paramIndex)).getType());
	}

	private MethodDeclaration findMethodDeclaration(IMethodBinding methodBinding, IJavaProject scope) throws JavaModelException {
		IMethod method= Bindings.findMethod(methodBinding, scope);
		IJavaElement e1= JavaModelUtil.findInCompilationUnit(fInputTypeWorkingCopy, method);
		if (e1 instanceof IMethod){
			method= (IMethod) e1;
		} else if (fSupertypeWorkingCopy != null){
			e1= JavaModelUtil.findInCompilationUnit(fSupertypeWorkingCopy, method);
			if (e1 instanceof IMethod)
				method= (IMethod) e1;
		}
			
		if (method == null)
			return null;
		return getMethodDeclarationNode(method);
	}
	
	private static class ExtractInterfaceConstraintCreator extends FullConstraintCreator{
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintCreator#create(org.eclipse.jdt.core.dom.ArrayCreation)
		 */
		//TODO bogus ?
		public ITypeConstraint[] create(ArrayCreation node) {
			ConstraintVariable arrayCreationVar= new ExpressionVariable(node);
			ConstraintVariable typeVar= new TypeVariable(node.getType());
			ITypeConstraint equals= SimpleTypeConstraint.createEqualsConstraint(arrayCreationVar, typeVar);
			return new ITypeConstraint[]{equals};
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintCreator#create(org.eclipse.jdt.core.dom.ArrayAccess)
		 */
		 //TODO bogus ?
		public ITypeConstraint[] create(ArrayAccess node) {
			Expression expression= node.getArray();
			ITypeConstraint equals= SimpleTypeConstraint.createEqualsConstraint(new ExpressionVariable(node), new ExpressionVariable(expression));
			return new ITypeConstraint[]{equals};
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintCreator#create(org.eclipse.jdt.core.dom.ArrayType)
		 */
		//TODO bogus ?
		public ITypeConstraint[] create(ArrayType node) {
			ConstraintVariable component= new TypeVariable(node.getComponentType());
			ITypeConstraint equals= SimpleTypeConstraint.createEqualsConstraint(new TypeVariable(node), component);
			return new ITypeConstraint[]{equals};
		}
	}
	private static class ASTNodeMappingManager {

		private final Map fCUsToCuNodes;//Map<ICompilationUnit, CompilationUnit>
		private final Map fCuNodesToCus;//Map<CompilationUnit, ICompilationUnit>
		private final WorkingCopyOwner fWorkingCopyOwner;
	
		public ASTNodeMappingManager(WorkingCopyOwner workingCopyOwner) {
			fCUsToCuNodes= new HashMap();
			fCuNodesToCus= new HashMap();
			fWorkingCopyOwner= workingCopyOwner;
		}

		public CompilationUnit getAST(ICompilationUnit cu){
			ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
			if (fCUsToCuNodes.containsKey(wc)){
				return (CompilationUnit)fCUsToCuNodes.get(wc);
			}	
			CompilationUnit cuNode;
			if (fWorkingCopyOwner != null)
				cuNode= AST.parseCompilationUnit(wc, true, fWorkingCopyOwner);
			else
				cuNode= AST.parseCompilationUnit(wc, true);
			fCUsToCuNodes.put(wc, cuNode);
			fCuNodesToCus.put(cuNode, wc);
			return cuNode;	
		}
	
		public ICompilationUnit getCompilationUnit(ASTNode node) {
			CompilationUnit cuNode= getCompilationUnitNode(node);
			Assert.isTrue(fCuNodesToCus.containsKey(cuNode)); //the cu node must've been created before
			return (ICompilationUnit)fCuNodesToCus.get(cuNode);
		}
	
		public static CompilationUnit getCompilationUnitNode(ASTNode node){
			if (node instanceof CompilationUnit)
				return (CompilationUnit)node;
			return (CompilationUnit)ASTNodes.getParent(node, CompilationUnit.class);
		}

		public ICompilationUnit[] getAllCompilationUnits(){
			return (ICompilationUnit[]) fCUsToCuNodes.keySet().toArray(new ICompilationUnit[fCUsToCuNodes.keySet().size()]);
		}
	
		public void clear(){
			fCuNodesToCus.clear();
			fCUsToCuNodes.clear();
		}
	}
}