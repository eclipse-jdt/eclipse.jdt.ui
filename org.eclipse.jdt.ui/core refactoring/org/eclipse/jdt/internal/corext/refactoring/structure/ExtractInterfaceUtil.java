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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.Document;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
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
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.TokenScanner;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStringStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ASTCreator;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompositeOrTypeConstraint;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintCollector;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintOperator;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintVariableFactory;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.DeclaringTypeVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ExpressionVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.FullConstraintCreator;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ITypeConstraint;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ParameterTypeVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.RawBindingVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ReturnTypeVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.SimpleTypeConstraint;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.TypeConstraintFactory;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.TypeVariable;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;

class ExtractInterfaceUtil {

	private final ICompilationUnit fInputTypeWorkingCopy;
	private final ICompilationUnit fSupertypeWorkingCopy;//can be null
	private final WorkingCopyOwner fWorkingCopyOwner;
	private static ICompilationUnit fCu;
	private final IType fInputType;

	private ExtractInterfaceUtil(ICompilationUnit inputTypeWorkingCopy, ICompilationUnit supertypeWorkingCopy, WorkingCopyOwner workingCopyOwner, IType inputType){
		Assert.isNotNull(inputTypeWorkingCopy);
		fSupertypeWorkingCopy= supertypeWorkingCopy;
		fInputTypeWorkingCopy= inputTypeWorkingCopy;
		fWorkingCopyOwner= workingCopyOwner;
		fInputType= inputType;
	}
	
	private static ConstraintVariable[] getAllOfType(ITypeConstraint[] constraints, ITypeBinding binding){
		Set result= new HashSet();
		ITypeBinding typeBinding= binding;
		for (int i= 0; i < constraints.length; i++) {
			ITypeConstraint constraint= constraints[i];
			if (constraint.isSimpleTypeConstraint()){
				SimpleTypeConstraint simple= (SimpleTypeConstraint)constraint;
				
				ConstraintVariable left= simple.getLeft();
				ITypeBinding leftBinding= left.getBinding();
				ConstraintVariable right= simple.getRight();
				ITypeBinding rightBinding= right.getBinding();
				
				if (leftBinding != null) {
					if (Bindings.equals(leftBinding, typeBinding))
						result.add(left);
					if (leftBinding.isArray() && Bindings.equals(leftBinding.getElementType(), typeBinding))
						result.add(left);
				}
				if (rightBinding != null) {
					if (Bindings.equals(rightBinding, typeBinding))
						result.add(right);
					if (rightBinding.isArray() && Bindings.equals(rightBinding.getElementType(), typeBinding))
						result.add(right);
				}
			} else {
				CompositeOrTypeConstraint cotc= (CompositeOrTypeConstraint)constraint;
				result.addAll(Arrays.asList(getAllOfType(cotc.getConstraints(), binding)));
			}
		}
		return (ConstraintVariable[]) result.toArray(new ConstraintVariable[result.size()]);
	}

	private ConstraintVariable[] getUpdatableVariables(ITypeBinding inputTypeBinding, IType theType, IType theSupertype, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException{
		ITypeBinding interfaceBinding= getSuperTypeBinding(inputTypeBinding, theSupertype);
		ICompilationUnit[] referringCus= getCusToParse(theType, theSupertype, pm, status);
		checkCompileErrors(referringCus, status);
		if (status.hasFatalError())
			return new ConstraintVariable[0];
		ITypeConstraint[] constraints= getConstraints(referringCus);
		return getUpdatableVariables(constraints, inputTypeBinding, interfaceBinding);
	}

	private void checkCompileErrors(ICompilationUnit[] referringCus, RefactoringStatus status) throws JavaModelException {
		for (int i= 0; i < referringCus.length; i++) {
			ICompilationUnit unit= referringCus[i];
			String source= unit.getSource();
			CompilationUnit cuNode= getAST(unit);
			IProblem[] problems= ASTNodes.getProblems(cuNode, ASTNodes.INCLUDE_ALL_PARENTS, ASTNodes.ERROR);
			for (int j= 0; j < problems.length; j++) {
				IProblem problem= problems[j];
				if (problem.isError()) {
					RefactoringStatusContext context= new JavaStringStatusContext(source,  new SourceRange(problem));
					status.addFatalError(problem.getMessage(), context);
				}
			}
		}
	}

	private static ITypeBinding getSuperTypeBinding(ITypeBinding typeBinding, IType superType) {
		Set setOfAll= new HashSet();
		setOfAll.addAll(Arrays.asList(Bindings.getAllSuperTypes(typeBinding)));
		
		// otherwise does not contain java.lang.Object for interfaces
		setOfAll.add(ASTCreator.createAST(fCu, null).getAST().resolveWellKnownType("java.lang.Object")); //$NON-NLS-1$
		
		
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
		//cannot compare names of unnamed/default packages (one is "UNNAMED", other is "")
		if (typeBinding.getPackage().isUnnamed() != type.getPackageFragment().isDefaultPackage())
			return false;
		if (! typeBinding.getPackage().isUnnamed() && !type.getPackageFragment().isDefaultPackage()){
			if (! typeBinding.getPackage().getName().equals(type.getPackageFragment().getElementName()))
				return false;
		}
		return true;
	}

	public static CompilationUnitRange[] updateReferences(TextChangeManager manager, IType inputType, IType supertypeToUse, WorkingCopyOwner workingCopyOwner, boolean updateInputTypeCu, IProgressMonitor pm, RefactoringStatus status, CodeGenerationSettings settings) throws CoreException{
		ICompilationUnit typeWorkingCopy= inputType.getCompilationUnit();
		fCu= typeWorkingCopy;
		ExtractInterfaceUtil inst= new ExtractInterfaceUtil(typeWorkingCopy, supertypeToUse.getCompilationUnit(), workingCopyOwner, inputType);
		ITypeBinding inputTypeBinding= getTypeBinding(inputType, workingCopyOwner);
		ConstraintVariable[] updatableVars= inst.getUpdatableVariables(inputTypeBinding, inputType, supertypeToUse, pm, status);
		if (status.hasFatalError())
			return new CompilationUnitRange[0];
		String typeName= inputType.getElementName();
		CompilationUnitRange[] ranges= inst.getCompilationUnitRanges(updatableVars, inputType, inputTypeBinding);
		Set cus= new HashSet();
		Map cuToImportType= new HashMap();
		for (int i= 0; i < ranges.length; i++) {
			if (pm.isCanceled())
				throw new OperationCanceledException();
			CompilationUnitRange range= ranges[i];
			ICompilationUnit cu= range.getCompilationUnit();
			if (updateInputTypeCu || ! cu.equals(typeWorkingCopy)){
			    TextChange change= getTextChange(manager, cu);
			    if (!cus.contains(cu)) {
			        cus.add(cu);
			        ImportRewrite importRewrite= new ImportRewrite(cu);
			        cuToImportType.put(cu, importRewrite.addImport(supertypeToUse.getFullyQualifiedName()));
			        TextEdit importEdit= importRewrite.createEdit(new Document(cu.getSource()));
			        TextChangeCompatibility.addTextEdit(
			        	change, 
						RefactoringCoreMessages.getString("ExtractInterfaceUtil.update_imports"), importEdit); //$NON-NLS-1$
			    }
				TextChangeCompatibility.addTextEdit(change, 
					RefactoringCoreMessages.getString("ExtractInterfaceUtil.update_reference"),  //$NON-NLS-1$
					createTypeUpdateEdit(range.getSourceRange(), typeName, (String)cuToImportType.get(cu)));
			}
		}
		return ranges;
	}

	public static TextChange getTextChange(TextChangeManager manager, ICompilationUnit cu) throws CoreException {
		// workaround for bug 39630 CompilationUnitChange cannot handle in-memory-only compilation units 
		if (manager.containsChangesIn(cu) || cu.getResource().exists())
			return manager.get(cu);
		DocumentChange result= new DocumentChange(cu.getElementName(), new Document(cu.getSource()));
		manager.manage(cu, result);
		return result;
	}

	private static TextEdit createTypeUpdateEdit(ISourceRange sourceRange, String className, String interfaceName) {
		int offset= sourceRange.getOffset() + sourceRange.getLength() - className.length();
		return new ReplaceEdit(offset, className.length(), interfaceName);
	}
	
	private static ConstraintVariable[] getUpdatableVariables(ITypeConstraint[] constraints, ITypeBinding classBinding, ITypeBinding interfaceBinding){
		Set/*<ConstraintVariable>*/ allVariablesOfType= new HashSet(Arrays.asList(getAllOfType(constraints, classBinding)));
		Set/*<ConstraintVariable>*/ badVariables= new HashSet();
		Set/*<ITypeConstraint>*/ badConstraints= new HashSet(); // set is never read again!
		ConstraintVariable[] initialBad= getInitialBad(allVariablesOfType, badVariables, badConstraints, constraints, interfaceBinding);
		if (initialBad == null || initialBad.length == 0)//TODO to be removed after it's optimized
			return (ConstraintVariable[]) allVariablesOfType.toArray(new ConstraintVariable[allVariablesOfType.size()]);
		badVariables.addAll(Arrays.asList(initialBad)); //TODO: this is redundant
		boolean repeat= false;
		do{
			//TODO can optimize here - don't have to walk the whole constraints array, bad would be enough
			int sizeOfBad= badVariables.size();
			for (int i= 0; i < constraints.length; i++) {
				ITypeConstraint constraint= constraints[i];
				if(! constraint.isSimpleTypeConstraint())
					continue;
				SimpleTypeConstraint con= (SimpleTypeConstraint)constraint;
				ConstraintVariable left= con.getLeft();
				ConstraintVariable right= con.getRight();
				if (allVariablesOfType.contains(left) && badVariables.contains(right) && ! badVariables.contains(left))
					badVariables.add(left);
				if (con.isEqualsConstraint() || con.isDefinesConstraint()){
					if (allVariablesOfType.contains(right) && badVariables.contains(left) && ! badVariables.contains(right))
						badVariables.add(right);
				}
			}
			repeat= sizeOfBad < badVariables.size();
		} while(repeat);
		Set updatable= new HashSet(allVariablesOfType);
		updatable.removeAll(badVariables);
		return (ConstraintVariable[]) updatable.toArray(new ConstraintVariable[updatable.size()]);
	}
	
	private static ConstraintVariable[] getInitialBad(Set/*<ConstraintVariable>*/ setOfAll,
			Set/*<ConstraintVariable>*/ badVariables, Set/*<ITypeConstraint>*/ badConstraints,
			ITypeConstraint[] constraints, ITypeBinding interfaceBinding) {
		ConstraintVariable interfaceVariable= new RawBindingVariable(interfaceBinding);
		for (int i= 0; i < constraints.length; i++) {
			ITypeConstraint constraint= constraints[i];
			if (constraint.isSimpleTypeConstraint()){
				SimpleTypeConstraint simple= (SimpleTypeConstraint)constraint;
				if (simple.isSubtypeConstraint() && canAddLeftSideToInitialBadSet(simple, setOfAll, interfaceVariable)) {
					badVariables.add(simple.getLeft());
					badConstraints.add(simple);
				}
			} else if (constraint instanceof CompositeOrTypeConstraint){
				ITypeConstraint[] subConstraints= ((CompositeOrTypeConstraint)constraint).getConstraints();
				if (canAddLeftSideToInitialBadSet(subConstraints, setOfAll, interfaceVariable)) {
					badVariables.add(((SimpleTypeConstraint)subConstraints[0]).getLeft());
					badConstraints.add(subConstraints[0]);
					badConstraints.add(constraint);
				}
			}
		}
		return (ConstraintVariable[]) badVariables.toArray(new ConstraintVariable[badVariables.size()]);
	}
	
	private static boolean canAddLeftSideToInitialBadSet(SimpleTypeConstraint sc,
			Set/*<ConstraintVariable>*/ setOfAll, ConstraintVariable interfaceVariable) {
		Assert.isTrue(sc.isSubtypeConstraint());
		ConstraintVariable left= sc.getLeft();
		ConstraintVariable right= sc.getRight();
		if (! (left instanceof ExpressionVariable || left instanceof TypeVariable))
			return false;
		else if (! setOfAll.contains(left))	
			return false;
		else if (interfaceVariable.canBeAssignedTo(right))
			return false;
		//DeclaringTypeVariables are different - they can never be updated by this refactoring
		else if (setOfAll.contains(right) && ! (right instanceof DeclaringTypeVariable))
			return false;
		else if (right instanceof DeclaringTypeVariable && right.getBinding() == null)
			return false; // calls to [].length do not give rise to badness
		else
			return true;
	}

	private static boolean canAddLeftSideToInitialBadSet(ITypeConstraint[] subConstraints,
			Set/*<ConstraintVariable>*/ setOfAll, ConstraintVariable interfaceVariable) {
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
			if (right.canBeAssignedTo(interfaceVariable))
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
		ConstraintCollector collector= new ConstraintCollector(new ExtractInterfaceConstraintCreator(fInputType));
		
		for (int i= 0; i < referringCus.length; i++) {
			ICompilationUnit unit= referringCus[i];
			getAST(unit).accept(collector);
			result.addAll(Arrays.asList(collector.getConstraints()));
			collector.clear();
		}
		return (ITypeConstraint[]) result.toArray(new ITypeConstraint[result.size()]);
	}
	
	private CompilationUnit getAST(ICompilationUnit unit) {
		return ASTCreator.createAST(unit, fWorkingCopyOwner);
	}

	/*
	 * we need to parse not only the cus that reference the type directly but also those that 
	 * reference a field or a method that reference the type. this method is used to find these cus.
	 */
	private ICompilationUnit[] getCusToParse(IType theType, IType theSupertype, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException{
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			SearchPattern pattern= SearchPattern.createPattern(theType, IJavaSearchConstants.REFERENCES);
			IJavaSearchScope scope= RefactoringScopeFactory.create(theType);
			ICompilationUnit[] workingCopies= getWorkingCopies(theType.getCompilationUnit(), theSupertype.getCompilationUnit());
			if (workingCopies.length == 0)
				workingCopies= null;
			SearchResultGroup[] typeReferences= RefactoringSearchEngine.search(pattern, scope, new SubProgressMonitor(pm, 1), workingCopies, status);
			ICompilationUnit[] typeReferencingCus= getCus(typeReferences);
			ICompilationUnit[] fieldAndMethodReferencingCus= fieldAndMethodReferringCus(theType, typeReferences, workingCopies, new SubProgressMonitor(pm, 1), status);
			return merge(fieldAndMethodReferencingCus, typeReferencingCus);
		} finally{
			pm.done();
		}
	}

	private ASTNode[] getAstNodes(SearchResultGroup searchResultGroup) {
		ICompilationUnit cu= searchResultGroup.getCompilationUnit();
		if (cu == null)
			return new ASTNode[0];
		CompilationUnit cuNode= getAST(cu);
		return ASTNodeSearchUtil.getAstNodes(searchResultGroup.getSearchResults(), cuNode);
	}
	
	private ICompilationUnit[] fieldAndMethodReferringCus(IType theType, SearchResultGroup[] typeReferences, ICompilationUnit[] wcs, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		SearchPattern pattern= createPatternForReferencingFieldsAndMethods(typeReferences);
		if (pattern == null)
			return new ICompilationUnit[0];
		IJavaSearchScope scope= RefactoringScopeFactory.create(theType);
		ICompilationUnit[] units= RefactoringSearchEngine.findAffectedCompilationUnits(pattern, scope, pm, status);
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

	private SearchPattern createPatternForReferencingFieldsAndMethods(SearchResultGroup[] typeReferences) throws JavaModelException {
		return RefactoringSearchEngine.createOrPattern(getReferencingFieldsAndMethods(typeReferences), IJavaSearchConstants.ALL_OCCURRENCES);		
	}

	private IMethod[] getReferencingMethods(ASTNode[] typeReferenceNodes) throws JavaModelException {
		List result= new ArrayList();
		for (int i= 0; i < typeReferenceNodes.length; i++) {
			ASTNode node= typeReferenceNodes[i];
			IJavaProject scope= ASTCreator.getCu(node).getJavaProject();
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
			IJavaProject scope= ASTCreator.getCu(node).getJavaProject();
			result.addAll(Arrays.asList(getFields(node, scope)));
		}
		return (IField[]) result.toArray(new IField[result.size()]);
	}
	
	private IMember[] getReferencingFieldsAndMethods(SearchResultGroup[] typeReferences) throws JavaModelException {
		List result= new ArrayList();
		for (int i= 0; i < typeReferences.length; i++) {
			SearchResultGroup group= typeReferences[i];	
			ASTNode[] typeReferenceNodes= getAstNodes(group);
			result.addAll(Arrays.asList(getReferencingMethods(typeReferenceNodes)));
			result.addAll(Arrays.asList(getReferencingFields(typeReferenceNodes)));
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
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
		IBinding binding= fragment.getName().resolveBinding();
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
		if (JavaPlugin.USE_WORKING_COPY_OWNERS) {
			ArrayList result= new ArrayList(2);
			if (precedingWC1 != null && precedingWC1.isWorkingCopy()) {
				result.add(precedingWC1);
			}
			if (precedingWC2 != null && precedingWC2.isWorkingCopy()) {
				result.add(precedingWC2);
			}
			return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
		}
		
		
		// XXX: This is a layer breaker - should not access jdt.ui
		IWorkingCopy[] copies= JavaUI.getSharedWorkingCopiesOnClasspath();
		Set result= new HashSet(copies.length);
		ICompilationUnit original1= null, original2= null;
		if (precedingWC1 != null && precedingWC1.isWorkingCopy()) {
			result.add(precedingWC1);
			original1= precedingWC1.getPrimary();
		}
		if (precedingWC2 != null && precedingWC2.isWorkingCopy()) {
			result.add(precedingWC2);
			original2= precedingWC2.getPrimary();
		}
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

	private static ITypeBinding getTypeBinding(IType type, WorkingCopyOwner workingCopyOwner) throws JavaModelException {
		final CompilationUnit unit= ASTCreator.createAST(type.getCompilationUnit(), workingCopyOwner);
		AbstractTypeDeclaration decl= null;
		if (type.isAnnotation())
			decl= ASTNodeSearchUtil.getAnnotationTypeDeclarationNode(type, unit);
		else if (type.isEnum())
			decl= ASTNodeSearchUtil.getEnumDeclarationNode(type, unit);
		else
			decl= ASTNodeSearchUtil.getTypeDeclarationNode(type, unit);
		return decl.resolveBinding();
	}

	private CompilationUnitRange[] getCompilationUnitRanges(ConstraintVariable[] variables, IType inputType, ITypeBinding inputTypeBinding) throws CoreException {
		Set ranges= new HashSet();
		IJavaProject scope= inputType.getJavaProject(); //TODO check if scope is correct
		for (int i= 0; i < variables.length; i++) {
			CompilationUnitRange range= getRange(variables[i], scope, inputTypeBinding);
			if (range != null)
				ranges.add(range);
		}
		return (CompilationUnitRange[]) ranges.toArray(new CompilationUnitRange[ranges.size()]);		
	}
	
	private CompilationUnitRange getRange(ConstraintVariable variable, IJavaProject scope, ITypeBinding inputTypeBinding) throws CoreException {
		if (variable instanceof DeclaringTypeVariable)
			return null;
		else if (variable instanceof RawBindingVariable)
			return null;
		else if (variable instanceof ParameterTypeVariable)
			return getRange((ParameterTypeVariable)variable, scope);
		else if (variable instanceof ReturnTypeVariable)
			return getRange((ReturnTypeVariable)variable, scope);
		else if (variable instanceof TypeVariable)
			return getRange((TypeVariable)variable);
		else if (variable instanceof ExpressionVariable)
			return getRange((ExpressionVariable)variable, inputTypeBinding);
		else //TODO is this enough?
			return null;
	}

	private CompilationUnitRange getRange(ExpressionVariable variable, ITypeBinding inputTypeBinding) {
		if (inputTypeBinding == null) return null;
		if (variable.getExpressionType() == ASTNode.SIMPLE_NAME || variable.getExpressionType() == ASTNode.QUALIFIED_NAME) {
			if (Bindings.equals(inputTypeBinding, variable.getExpressionBinding()))
				return variable.getCompilationUnitRange();
		}
		return null;
	}

	private CompilationUnitRange getRange(TypeVariable variable) {
		return variable.getCompilationUnitRange();
	}

	private CompilationUnitRange getRange(ReturnTypeVariable variable, IJavaProject scope) throws CoreException {
		IMethodBinding methodBinding= variable.getMethodBinding();
		IMethod method= getMethod(methodBinding, scope);
		if (method == null)
			return null;
		return new CompilationUnitRange(method.getCompilationUnit(), getReturnTypeRange(method));
	}	
	
	private CompilationUnitRange getRange(ParameterTypeVariable variable, IJavaProject scope) throws CoreException {
		IMethodBinding methodBinding= variable.getMethodBinding();
		int paramIndex= variable.getParameterIndex();
		IMethod method= getMethod(methodBinding, scope);
		if (method == null)
			return null;
		return new CompilationUnitRange(method.getCompilationUnit(), getParameterTypeRange(method, paramIndex));
	}

	private static ISourceRange getReturnTypeRange(IMethod method) throws CoreException {
		IScanner scanner= ToolFactory.createScanner(false, false, false, false);
		scanner.setSource(method.getSource().toCharArray());
		TokenScanner tokenScanner= new TokenScanner(scanner);
		skipModifiers(tokenScanner);
		return new SourceRange(method.getSourceRange().getOffset() +  tokenScanner.getCurrentStartOffset(), tokenScanner.getCurrentLength());
	}

	private static void skipModifiers(TokenScanner scanner) throws CoreException {
		int token= scanner.readNext(true);
		while (token != ITerminalSymbols.TokenNameEOF) {
			if (!TokenScanner.isModifier(token))
				return;
			token= scanner.readNext(true);
		}
	}

	private static ISourceRange getParameterTypeRange(IMethod method, int paramIndex) throws CoreException {
		Assert.isTrue(0 <= paramIndex, "incorrect parameter"); //$NON-NLS-1$
		Assert.isTrue(paramIndex < method.getNumberOfParameters(), "too few method parameters"); //$NON-NLS-1$
		IScanner scanner= ToolFactory.createScanner(false, false, false, false);
		scanner.setSource(method.getSource().toCharArray());
		TokenScanner tokenScanner= new TokenScanner(scanner);
		tokenScanner.readToToken(ITerminalSymbols.TokenNameLPAREN);
		for (int i= 0; i < paramIndex; i++) {
			tokenScanner.readToToken(ITerminalSymbols.TokenNameCOMMA);
		}
		tokenScanner.readNext(true);
		return new SourceRange(method.getSourceRange().getOffset() + tokenScanner.getCurrentStartOffset(), tokenScanner.getCurrentLength());
	}

	private IMethod getMethod(IMethodBinding methodBinding, IJavaProject scope) throws JavaModelException {
		IMethod method= Bindings.findMethod(methodBinding, scope);
		IJavaElement e1= JavaModelUtil.findInCompilationUnit(fInputTypeWorkingCopy, method);
		if (e1 instanceof IMethod){
			method= (IMethod) e1;
		} else if (fSupertypeWorkingCopy != null){
			e1= JavaModelUtil.findInCompilationUnit(fSupertypeWorkingCopy, method);
			if (e1 instanceof IMethod)
				method= (IMethod) e1;
		}
		return method;			
	}
	
	private static class ExtractInterfaceConstraintCreator extends FullConstraintCreator{

		public ExtractInterfaceConstraintCreator(final IType inputType){
			super(new ConstraintVariableFactory(), new TypeConstraintFactory(){
				public boolean filter(ConstraintVariable v1, ConstraintVariable v2, ConstraintOperator o){
					ITypeBinding v1Binding= v1.getBinding();
					ITypeBinding v2Binding= v2.getBinding();
					if (v1Binding != null && v2Binding != null){
						String inputTypeName= inputType.getFullyQualifiedName();
						String v1Name= (!v1Binding.isArray()) ? v1Binding.getQualifiedName()
															  : v1Binding.getElementType().getQualifiedName();
						String v2Name= (!v2Binding.isArray()) ? v2Binding.getQualifiedName()
															  : v2Binding.getElementType().getQualifiedName();
						if (!v1Name.equals(inputTypeName) && !v2Name.equals(inputTypeName)){
							if (PRINT_STATS) fNrFiltered++;
							return true;
						}
					}
					return super.filter(v1, v2, o);
				}
			});
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintCreator#create(org.eclipse.jdt.core.dom.ArrayCreation)
		 */
		public ITypeConstraint[] create(ArrayCreation node) {
			ConstraintVariable arrayCreationVar= getConstraintVariableFactory().makeExpressionOrTypeVariable(node, getContext());
			ConstraintVariable typeVar= getConstraintVariableFactory().makeTypeVariable(node.getType());
			ITypeConstraint[] equals= getConstraintFactory().createEqualsConstraint(arrayCreationVar, typeVar);
			return equals;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintCreator#create(org.eclipse.jdt.core.dom.ArrayAccess)
		 */
		public ITypeConstraint[] create(ArrayAccess node) {
			Expression expression= node.getArray();
			ITypeConstraint[] equals= getConstraintFactory().createEqualsConstraint(getConstraintVariableFactory().makeExpressionOrTypeVariable(node, getContext()), getConstraintVariableFactory().makeExpressionOrTypeVariable(expression, getContext()));
			return equals;
		}
	
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintCreator#create(org.eclipse.jdt.core.dom.ArrayType)
		 */
		public ITypeConstraint[] create(ArrayType node) {
			ConstraintVariable component= getConstraintVariableFactory().makeTypeVariable(node.getComponentType());
			ITypeConstraint[] equals= getConstraintFactory().createEqualsConstraint(getConstraintVariableFactory().makeTypeVariable(node), component);
			return equals;
		}
	}
}
