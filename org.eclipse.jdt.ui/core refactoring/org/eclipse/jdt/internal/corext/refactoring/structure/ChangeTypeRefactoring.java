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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.RippleMethodFinder;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ASTCreator;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompositeOrTypeConstraint;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintCollector;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ExpressionVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.FullConstraintCreator;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ITypeConstraint;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ParameterTypeVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.ReturnTypeVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.SimpleTypeConstraint;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.TypeBindings;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.TypeVariable;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.GroupDescription;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.text.edits.MultiTextEdit;

/**
 * @author tip
 */
public class ChangeTypeRefactoring extends Refactoring {

	// ------------------------------------------------------------------------------------------------- //
	// Fields

	private final Map/*<ICompilationUnit, Collection<ITypeConstraint>>*/ fConstraintCache;
	/**
	 * Offset of the selected text area.
	 */
	private final int fSelectionStart;

	/**
	 * Length of the selected text area.
	 */
	private final int fSelectionLength;
	
	/**
	 * Offset of the effective selection
	 */
	private int fEffectiveSelectionStart;
	
	/**
	 * Length of the effective selection
	 */
	private int fEffectiveSelectionLength;
	
	/**
	 * ICompilationUnit containing the selection.
	 */
	private ICompilationUnit fCu;
	
	/**
	 * If the selection corresponds to a method parameter/return type, this field stores
	 * a reference to its IMethodBinding, otherwise this field remains null. Used during 
	 * search for references in other CUs, and for determining the ConstraintVariable
	 * that corresponds to the selection
	 */
	private IMethodBinding fMethodBinding;
	
	/**
	 * If the selection corresponds to a method parameter, this field stores the parameter
	 * index (0 = first parameter for static methods, 0 = this for nonstatic methods). The
	 * value -1 is stored in the field if the selection corresponds to a method return type.
	 */
	private int fParamIndex;
	
	/**
	 * If the selection corresponds to a field, this field stores a reference to its IVariableBinding,
	 * otherwise this field remains null. Used during search for references in other CUs.
	 */
	private IVariableBinding fFieldBinding;

	/**
	 * The compilation units that contain constraint variables related to the selection
	 */
	private ICompilationUnit[] fAffectedUnits;

	/**
	 * The constraint variables that are of interest to this refactoring. This includes
	 * the constraint var. corresponding to the text selection, and possibly additional
	 * elements due to method overriding, method calls, etc.
	 */
	private Collection/*<ConstraintVariable>*/ fRelevantVars;

	/**
	 * The set of types (other than the original type) that can be given to
	 * the selected ASTNode.
	 */
	private final Collection/*<IType>*/ fValidTypes;
	
	/**
	 * The type constraints that are related to the selected ASTNode.
	 */
	private Collection/*<ITypeConstraint>*/ fRelevantConstraints;

	/**
	 * All type constraints in affected compilation units.
	 */
	private Collection/*<ITypeConstraint>*/ fAllConstraints;

	/**
	 * The new type of the selected declaration.
	 */
	private IType fSelectedType;
	
	/**
	 * Organizes SearchResults by CompilationUnit
	 */
	private Map/*<ICompilationUnit,SearchResultGroup>*/ fCuToSearchResultGroup= new HashMap();
	
	
	/**
	 * The type hierarchy of the original type.
	 */
	private ITypeHierarchy fTypeHierarchy;

	/**
	 * Type of the original selection.
	 */
	private IType fOriginalTypeOfSelection;

	/**
	 * Control debugging output.
	 */
	private static final boolean DEBUG= false;

	/**
	 * Enumeration: the types considered when determining possible replacement types. 
	 */
	private short fTypesToConsider= SUPERTYPES_ONLY;

	public static final short SUPERTYPES_ONLY= 1;
	public static final short SUPERTYPES_AND_SUBTYPES= 2;
	private ConstraintVariable fCv;
	
	public static boolean isAvailable(IJavaElement element) throws JavaModelException {
		if (element == null || ! element.exists())
			return false;
		String returnType= null;
		if (element instanceof IMethod) {
			returnType= ((IMethod)element).getReturnType();
		} else if (element instanceof IField) {
			returnType= ((IField)element).getTypeSignature();
		} else if (element instanceof ILocalVariable) {
			// be optimistic
			return true;
		} else if (element instanceof IType) {
			// be optimistic.
			return true;
		}
		if (PrimitiveType.toCode(Signature.toString(returnType)) != null)
			return false;
		return true;
	}
	
	public static ChangeTypeRefactoring create(ICompilationUnit cu, int selectionStart, int selectionLength){
		return new ChangeTypeRefactoring(cu, selectionStart, selectionLength);
	}
	
	public static ChangeTypeRefactoring create(ICompilationUnit cu, int selectionStart, int selectionLength, String selectedTypeName){
		return new ChangeTypeRefactoring(cu, selectionStart, selectionLength, selectedTypeName);
	}

	private ChangeTypeRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
		this(cu, selectionStart, selectionLength, null);
	}

	/**
	 * Constructor for ChangeTypeRefactoring (invoked from tests only)
	 */
	private ChangeTypeRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength, String selectedTypeName) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());

		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;

		fEffectiveSelectionStart= selectionStart;
		fEffectiveSelectionLength= selectionLength;
		
		fCu= cu;

		if (selectedTypeName != null)
			setSelectedType(selectedTypeName);
		
		fConstraintCache= new HashMap();
		fValidTypes= new HashSet();
	}

	// ------------------------------------------------------------------------------------------------- //
	// Overridden methods from superclass:
	//  - checkActivation(), checkSelection(), checkInput(), createChange()

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try {
			pm.beginTask("", 1); //$NON-NLS-1$
			if (fCu == null || !fCu.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ChangeTypeRefactoring.invalidSelection")); //$NON-NLS-1$
			return checkSelection(new SubProgressMonitor(pm, 1));
		} catch (JavaModelException e){
			throw e;
		} finally {
			pm.done();
		}
	}

	/**
	 * Check if the right type of AST Node is selected. Create the TypeHierarchy needed to
	 * bring up the wizard.
	 */
	private RefactoringStatus checkSelection(IProgressMonitor pm) throws JavaModelException {
		try {
			pm.beginTask("", 5); //$NON-NLS-1$

			ASTNode node= getTargetNode(fCu, fSelectionStart, fSelectionLength);
			if (DEBUG) {
				System.out.println(
					"selection: [" //$NON-NLS-1$
						+ fSelectionStart
						+ "," //$NON-NLS-1$
						+ (fSelectionStart + fSelectionLength)
						+ "] in " //$NON-NLS-1$
						+ fCu.getElementName());
				System.out.println("node= " + node + ", type= " + node.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
			}

			String selectionValid= determineSelection(node);
			if (selectionValid != null){
				if (DEBUG){
					System.out.println("invalid selection: " + selectionValid); //$NON-NLS-1$
				}
				return RefactoringStatus.createFatalErrorStatus(selectionValid);
			}

			pm.worked(1);

			RefactoringStatus result= new RefactoringStatus();

			// need to construct the type hierarchy for the selection so the wizard can display it
			
			fCv= findConstraintVariableForSelectedNode(new SubProgressMonitor(pm, 3));
			if (DEBUG) System.out.println("selected CV: " + fCv +  //$NON-NLS-1$
										  " (" + fCv.getClass().getName() +  //$NON-NLS-1$
										  ")");  //$NON-NLS-1$

			ITypeBinding typeBinding= fCv.getBinding();
			
			// produce error message if array or primitive type is selected
			if (typeBinding.isArray()){
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ChangeTypeRefactoring.arraysNotSupported")); //$NON-NLS-1$
			}
			if (typeBinding.isPrimitive()){
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ChangeTypeRefactoring.primitivesNotSupported")); //$NON-NLS-1$
			}
			if (checkOverriddenBinaryMethods())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ChangeTypeRefactoring.notSupportedOnBinary")); //$NON-NLS-1$
			
			if (typeBinding.isLocal()){
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ChangeTypeRefactoring.localTypesNotSupported")); //$NON-NLS-1$
			}
			
			if (fFieldBinding != null && fFieldBinding.getDeclaringClass().isLocal()){
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ChangeTypeRefactoring.localTypesNotSupported")); //$NON-NLS-1$
			}
			
			fOriginalTypeOfSelection= Bindings.findType(fCv.getBinding(), fCu.getJavaProject());
			
			fTypeHierarchy= getTypeHierarchy(fOriginalTypeOfSelection, pm, fCu.getJavaProject());

			pm.worked(1);

			if (fSelectedType != null){ // if invoked from unit test, compute valid types here
				computeValidTypes(new NullProgressMonitor());
			}
			
			return result;
		} finally {
			pm.done();
		}
	}
	
	private boolean checkOverriddenBinaryMethods() throws JavaModelException {
		if (fMethodBinding != null){
			Set declaringSupertypes= getDeclaringSuperTypes(fMethodBinding);
			for (Iterator iter= declaringSupertypes.iterator(); iter.hasNext();) {
				ITypeBinding superType= (ITypeBinding) iter.next();
				IMethodBinding overriddenMethod= findMethod(fMethodBinding, superType);
				Assert.isNotNull(overriddenMethod);//because we asked for declaring types
				IMethod iMethod= Bindings.findMethod(overriddenMethod, fCu.getJavaProject());
				if (iMethod.isBinary()){
					return true;
				}
			}
		}	
		return false;
	}
	
	// copied from FullConstraintCreator
	private static IMethodBinding findMethod(IMethodBinding methodBinding, ITypeBinding type) {
		  if (methodBinding.getDeclaringClass().equals(type))
			  return methodBinding;
		  return Bindings.findMethodInType(type, methodBinding.getName(), methodBinding.getParameterTypes());
	}

	// copied from FullConstraintCreator
	private static Set getDeclaringSuperTypes(IMethodBinding methodBinding) {
		Set allSuperTypes= TypeBindings.getSuperTypes(methodBinding.getDeclaringClass());
		Set result= new HashSet();
		for (Iterator iter= allSuperTypes.iterator(); iter.hasNext();) {
			ITypeBinding type= (ITypeBinding) iter.next();
			if (findMethod(methodBinding, type) != null)
				result.add(type);
		}
		return result;
	}	
	
	/**
	 * Do the actual work of computing allowable types. Invoked by the wizard when
	 * "compute" button is pressed
	 */
	public Collection/*<IType>*/ computeValidTypes(IProgressMonitor pm) {
		
		pm.beginTask(RefactoringCoreMessages.getString("ChangeTypeRefactoring.checking_preconditions"), 100); //$NON-NLS-1$

		try {
			
			fRelevantVars= findRelevantConstraintVars(fCv, new SubProgressMonitor(pm, 50));
			
			if (DEBUG)
				printCollection("relevant vars:", fRelevantVars); //$NON-NLS-1$
	
			fRelevantConstraints= findRelevantConstraints(fRelevantVars, new SubProgressMonitor(pm, 30));
		
			
			fValidTypes.addAll(computeValidTypes(fOriginalTypeOfSelection, fRelevantVars, 
												 fRelevantConstraints, new SubProgressMonitor(pm, 20)));
	
			if (DEBUG)
				printCollection("valid types:", getValidTypeNames()); //$NON-NLS-1$
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
		
		pm.done();
		
		return fValidTypes;
	}
	
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) {
		pm.beginTask(RefactoringCoreMessages.getString("ChangeTypeRefactoring.checking_preconditions"), 1); //$NON-NLS-1$	
			
		RefactoringStatus result= Checks.validateModifiesFiles(ResourceUtil.getFiles(fAffectedUnits));
		
		pm.done();
		return result;
	}

// TODO: do sanity check somewhere if the refactoring changes any files.
//
//	private IFile[] getAllFilesToModify(){
//		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
//	}
//	
//	private RefactoringStatus validateModifiesFiles(){
//		return Checks.validateModifiesFiles(getAllFilesToModify());
//	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.getString("ChangeTypeMessages.CreateChangesForChangeType"), 1); //$NON-NLS-1$
		try {
			Map/*<ICompilationUnit,Set<ConstraintVariable>>*/ relevantVarsByUnit=
			  new HashMap/*<ICompilationUnit,HashSet<ConstraintVariable>>*/();
			groupChangesByCompilationUnit(relevantVarsByUnit);
			
			CompositeChange	topLevelChange= new CompositeChange(RefactoringCoreMessages.getString("ChangeTypeRefactoring.allChanges"));  //$NON-NLS-1$
			for (Iterator/*<ICompilationUnit>*/ it= relevantVarsByUnit.keySet().iterator(); it.hasNext(); ){
				ICompilationUnit icu= (ICompilationUnit)it.next();
				Set/*<ConstraintVariable>*/ cVars = (Set)relevantVarsByUnit.get(icu);
				CompilationUnitChange cuChange= new CompilationUnitChange(getName(), icu);
				addAllChangesFor(icu, cVars, cuChange);
				topLevelChange.add(cuChange);
				pm.worked(1);
			}
			return topLevelChange;
		} catch (JavaModelException e){
			throw e;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} finally {
			pm.done();
		}
	}

	/**
	 * Apply all changes related to a single ICompilationUnit
	 */
	private void addAllChangesFor(ICompilationUnit icu, Set vars, CompilationUnitChange unitChange) throws CoreException {
		CompilationUnit	unit=  ASTCreator.createAST(icu, null);
		ASTRewrite unitRewriter= new ASTRewrite(unit);
		TextBuffer buffer= null;
		MultiTextEdit root= new MultiTextEdit();
		 
		try {
			unitChange.setEdit(root); // Adam sez don't need this, but then unitChange.addGroupDescription() fails an assertion!
			buffer= TextBuffer.acquire((IFile) icu.getResource());
			String typeName= updateImports(icu, buffer, root);
			updateCu(unit, vars, unitChange, unitRewriter, typeName);
			unitRewriter.rewriteNode(buffer, root);
		} finally {
			if (buffer != null)
				TextBuffer.release(buffer);
		}
	}

	private void updateCu(CompilationUnit unit, Set vars, CompilationUnitChange unitChange, 
						  ASTRewrite unitRewriter, String typeName) throws JavaModelException {
		for (Iterator it=vars.iterator(); it.hasNext(); ){
			ConstraintVariable cv = (ConstraintVariable)it.next();
			ASTNode decl= findDeclaration(unit, cv);
			if (decl instanceof SimpleName && cv instanceof ExpressionVariable) {
				ASTNode gp= decl.getParent().getParent();
				if (gp.getNodeType() == ASTNode.METHOD_DECLARATION){ // method parameter
					if (decl.getParent().getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION) {
						updateType(unit, getType(decl.getParent()), unitChange, unitRewriter, typeName);
					}
				} else{ 
					updateType(unit, getType(gp), unitChange, unitRewriter, typeName); // local variable, field
				}
			} else if (decl instanceof MethodDeclaration) {
				updateType(unit, getType(decl), unitChange, unitRewriter, typeName); // method return
			}
		}	
	}

	private void updateType(CompilationUnit cu, Type oldType, CompilationUnitChange unitChange, 
							ASTRewrite unitRewriter, String typeName) {
		
		String oldName= oldType.resolveBinding().getName();
		String description= 
		  RefactoringCoreMessages.getString("ChangeTypeRefactoring.typeChange") //$NON-NLS-1$ 
		  + oldName + RefactoringCoreMessages.getString("ChangeTypeRefactoring.to") + typeName;  //$NON-NLS-1$ //$NON-NLS-2$
		GroupDescription gd= new GroupDescription(description); 
		AST	ast= cu.getAST();
		//TODO handle types other than simple (e.g., arrays)
		Type newType= ast.newSimpleType(ast.newSimpleName(typeName)); 
		unitRewriter.markAsReplaced(oldType, newType, gd);
		unitChange.addGroupDescription(gd);
	}	
	
	private void groupChangesByCompilationUnit(Map relevantVarsByUnit) throws JavaModelException {
		for (Iterator it= fRelevantVars.iterator(); it.hasNext();) {
			ConstraintVariable cv= (ConstraintVariable) it.next();
			if (!(cv instanceof ExpressionVariable) && !(cv instanceof ReturnTypeVariable)){
				continue;
			} 
			ICompilationUnit icu = null;
			if (cv instanceof ExpressionVariable) {
				ExpressionVariable ev = (ExpressionVariable)cv;
				icu = ev.getCompilationUnitRange().getCompilationUnit();
			} else if (cv instanceof ReturnTypeVariable){
				ReturnTypeVariable rtv = (ReturnTypeVariable)cv;
				IMethodBinding mb= rtv.getMethodBinding();
				icu= Bindings.findMethod(mb, fCu.getJavaProject()).getCompilationUnit();
			}
			if (!relevantVarsByUnit.containsKey(icu)){
				relevantVarsByUnit.put(icu, new HashSet/*<ConstraintVariable>*/());
			}
			((Set)relevantVarsByUnit.get(icu)).add(cv);
		}
	}

	private ASTNode findDeclaration(CompilationUnit root, ConstraintVariable cv) throws JavaModelException {
		if (cv instanceof ExpressionVariable){
			for (Iterator iter= fAllConstraints.iterator(); iter.hasNext();) {
				ITypeConstraint constraint= (ITypeConstraint) iter.next();
				if (constraint.isSimpleTypeConstraint()){
					SimpleTypeConstraint stc= (SimpleTypeConstraint)constraint;
					if (stc.isDefinesConstraint() && stc.getLeft().equals(cv)){
						ConstraintVariable right= stc.getRight();
						if (right instanceof TypeVariable){
							TypeVariable typeVariable= (TypeVariable)right;
							return NodeFinder.perform(root, typeVariable.getCompilationUnitRange().getSourceRange());
						}
					}
				}
			}
		} else if (cv instanceof ReturnTypeVariable) {
			ReturnTypeVariable rtv= (ReturnTypeVariable) cv;
			IMethodBinding mb= rtv.getMethodBinding();
			IMethod im= Bindings.findMethod(mb, fCu.getJavaProject());
			return ASTNodeSearchUtil.getMethodDeclarationNode(im, root);
		}
		return null;
	}
	
	private static Type getType(ASTNode node) {
		switch(node.getNodeType()){
			case ASTNode.SINGLE_VARIABLE_DECLARATION:
				return ((SingleVariableDeclaration) node).getType();
			case ASTNode.FIELD_DECLARATION:
				return ((FieldDeclaration) node).getType();
			case ASTNode.VARIABLE_DECLARATION_STATEMENT:
				return ((VariableDeclarationStatement) node).getType();
			case ASTNode.METHOD_DECLARATION:
				return ((MethodDeclaration)node).getReturnType();
			default:
				Assert.isTrue(false);
				return null;
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("ChangeTypeRefactoring.name"); //$NON-NLS-1$
	}

	// ------------------------------------------------------------------------------------------------- //
	// Method for examining if a suitable kind of ASTNode was selected. Information about this node and
	// its parents in the AST are stored in fields fBinding, theMethod, and theField

	/**
	 * Determines what kind of ASTNode has been selected. A non-null String containing an error message
	 * is returned if the ChangeTypeRefactoring refactoring cannot be applied to the selected ASTNode.
	 * A return value of null indicates a valid selection.  
	 */
	private String determineSelection(ASTNode node) {
		if (node == null) {
			return RefactoringCoreMessages.getString("ChangeTypeRefactoring.invalidSelection"); //$NON-NLS-1$
		} else {
			switch (node.getNodeType()) {
				case ASTNode.SIMPLE_NAME :
					return simpleNameSelected((SimpleName)node);
				case ASTNode.VARIABLE_DECLARATION_STATEMENT :
					return variableDeclarationStatementSelected((VariableDeclarationStatement) node);
				case ASTNode.FIELD_DECLARATION :
					return fieldDeclarationSelected((FieldDeclaration) node);
				case ASTNode.SINGLE_VARIABLE_DECLARATION :
					return singleVariableDeclarationSelected((SingleVariableDeclaration) node);
				default :
					return nodeTypeNotSupported();
			}
		}
	}
	/**
	 * The selection corresponds to an ASTNode on which "ChangeTypeRefactoring" is not defined.
	 */
	private static String nodeTypeNotSupported() {
		return RefactoringCoreMessages.getString("ChangeTypeRefactoring.notSupportedOnNodeType"); //$NON-NLS-1$
	}

	/**
	  * The selection corresponds to a SingleVariableDeclaration
	  */
	private String singleVariableDeclarationSelected(SingleVariableDeclaration svd) {
		fEffectiveSelectionStart= svd.getName().getStartPosition();
		fEffectiveSelectionLength= svd.getName().getLength();
		return simpleNameSelected(svd.getName());
	}

	/**
	 * The selection corresponds to a VariableDeclarationStatement
	 */
	private String variableDeclarationStatementSelected(VariableDeclarationStatement vds) {
		if (vds.fragments().size() != 1) {
			return RefactoringCoreMessages.getString("ChangeTypeRefactoring.multiDeclarationsNotSupported"); //$NON-NLS-1$
		} else {
			ASTNode elem= (ASTNode) vds.fragments().iterator().next();
			if (elem.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
				simpleNameSelected(((VariableDeclarationFragment) elem).getName());
				fEffectiveSelectionStart= ((VariableDeclarationFragment) elem).getName().getStartPosition(); 
				fEffectiveSelectionLength= ((VariableDeclarationFragment) elem).getName().getLength();
			} else{
				return nodeTypeNotSupported();
			}
		}
		return null;
	}

	/**
	 * The selection corresponds to a FieldDeclaration
	 */
	private String fieldDeclarationSelected(FieldDeclaration fieldDeclaration) {
		if (fieldDeclaration.fragments().size() != 1) {
			return RefactoringCoreMessages.getString("ChangeTypeRefactoring.multiDeclarationsNotSupported"); //$NON-NLS-1$
		} else {
			VariableDeclarationFragment elem= (VariableDeclarationFragment) fieldDeclaration.fragments().iterator().next();
			SimpleName name= elem.getName();
			if (elem.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
				fFieldBinding= elem.resolveBinding();
				fEffectiveSelectionStart= name.getStartPosition();
				fEffectiveSelectionLength= name.getLength(); 
				return simpleNameSelected(name);
			} else{
				return nodeTypeNotSupported();
			}
		}
	}

	/**
	 * The selection corresponds to a SimpleName
	 */
	private String simpleNameSelected(SimpleName node) {
		ASTNode parent= node.getParent();
		ASTNode grandParent= parent.getParent();
		
		if (DEBUG) System.out.println("parent nodeType= " + parent.getClass().getName()); //$NON-NLS-1$
		if (DEBUG) System.out.println("GrandParent nodeType= " + grandParent.getClass().getName()); //$NON-NLS-1$
		
		if (parent.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT){
			VariableDeclarationStatement vds= (VariableDeclarationStatement)parent;
			if (vds.fragments().size() > 1){
				return RefactoringCoreMessages.getString("ChangeTypeRefactoring.multiDeclarationsNotSupported"); //$NON-NLS-1$
			}	
		} else if (parent.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			if (grandParent.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT){
				VariableDeclarationStatement vds= (VariableDeclarationStatement)grandParent;
				if (vds.fragments().size() > 1){
					return RefactoringCoreMessages.getString("ChangeTypeRefactoring.multiDeclarationsNotSupported"); //$NON-NLS-1$
				}	
				fEffectiveSelectionStart= node.getStartPosition();
				fEffectiveSelectionLength= node.getLength();
			} else if (grandParent.getNodeType() == ASTNode.FIELD_DECLARATION) {
				FieldDeclaration fd= (FieldDeclaration)grandParent;
				fFieldBinding= ((VariableDeclarationFragment)parent).resolveBinding();
				if (fd.fragments().size() > 1){
					return RefactoringCoreMessages.getString("ChangeTypeRefactoring.multiDeclarationsNotSupported"); //$NON-NLS-1$
				}
			}					
		} else if (parent.getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION || parent.getNodeType() == ASTNode.VARIABLE_DECLARATION_FRAGMENT) {
			if ((grandParent.getNodeType() == ASTNode.METHOD_DECLARATION)) {
				fMethodBinding= ((MethodDeclaration)grandParent).resolveBinding();
				fParamIndex= ((MethodDeclaration)grandParent).parameters().indexOf(parent);
			}
		} else if (parent.getNodeType() == ASTNode.SIMPLE_TYPE && (grandParent.getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION)) {
			ASTNode greatGrandParent= grandParent.getParent();
			if (greatGrandParent != null && greatGrandParent.getNodeType() == ASTNode.METHOD_DECLARATION) {
				fMethodBinding= ((MethodDeclaration)greatGrandParent).resolveBinding();
				fParamIndex= ((MethodDeclaration)greatGrandParent).parameters().indexOf(grandParent);
			}
			fEffectiveSelectionStart= node.getStartPosition();
			fEffectiveSelectionLength= node.getLength();
		} else if (parent.getNodeType() == ASTNode.SIMPLE_TYPE && grandParent.getNodeType() == ASTNode.METHOD_DECLARATION) {
			fMethodBinding= ((MethodDeclaration)grandParent).resolveBinding();
			fParamIndex= -1;
		} else if (parent.getNodeType() == ASTNode.METHOD_DECLARATION && 
				   grandParent.getNodeType() == ASTNode.TYPE_DECLARATION) {
			MethodDeclaration methodDeclaration= (MethodDeclaration)parent;
			if (methodDeclaration.getName().equals(node)){
				return RefactoringCoreMessages.getString("ChangeTypeRefactoring.notSupportedOnNodeType"); //$NON-NLS-1$
			}
			fMethodBinding= ((MethodDeclaration)parent).resolveBinding();
			fParamIndex= -1;
		} else if (
			parent.getNodeType() == ASTNode.SIMPLE_TYPE && (grandParent.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT)) {
			return variableDeclarationStatementSelected((VariableDeclarationStatement) grandParent);
		} else if (parent.getNodeType() == ASTNode.CAST_EXPRESSION) {
			ASTNode decl= findDeclaration(parent.getRoot(), fSelectionStart, fSelectionLength+1);
			VariableDeclarationFragment fragment= (VariableDeclarationFragment)decl;
			fEffectiveSelectionStart= fragment.getName().getStartPosition(); 
			fEffectiveSelectionLength= fragment.getName().getLength();
		} else if (parent.getNodeType() == ASTNode.SIMPLE_TYPE && 
			       grandParent.getNodeType() == ASTNode.FIELD_DECLARATION) {
			return fieldDeclarationSelected((FieldDeclaration) grandParent);
		} else if (parent.getNodeType() == ASTNode.SIMPLE_TYPE && 
			       grandParent.getNodeType() == ASTNode.ARRAY_TYPE){
			return RefactoringCoreMessages.getString("ChangeTypeRefactoring.arraysNotSupported"); //$NON-NLS-1$
		} else {
			return RefactoringCoreMessages.getString("ChangeTypeRefactoring.notSupportedOnNodeType"); //$NON-NLS-1$
		}
		return null;
	}

	// ------------------------------------------------------------------------------------------------- //
    // Methods for examining & solving type constraints. This includes:
    //  (1) locating the ConstraintVariable corresponding to the selected ASTNode
    //  (2) finding all ConstraintVariables "related" to (1) via overriding, method calls, field access
    //  (3) find all ITypeConstraints of interest that mention ConstraintVariables in (2)
    //  (4) determining all ITypes for which the ITypeConstraints in (3) are satisfied
 
	/**
	 * Find a ConstraintVariable that corresponds to the selected ASTNode.
	 */
	private ConstraintVariable findConstraintVariableForSelectedNode(IProgressMonitor pm) {
		pm.beginTask(RefactoringCoreMessages.getString("ChangeTypeRefactoring.analyzingMessage"), 100);  //$NON-NLS-1$
		ICompilationUnit[] cus= { fCu }; // only search in CU containing selection
		
		if (DEBUG){
			System.out.println("Effective selection: " + fEffectiveSelectionStart + "/" + fEffectiveSelectionLength); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		Collection/*<ITypeConstraint>*/ allConstraints= getConstraints(cus, new SubProgressMonitor(pm, 50));
		
		IProgressMonitor subMonitor= new SubProgressMonitor(pm, 50);
		subMonitor.beginTask(RefactoringCoreMessages.getString("ChangeTypeRefactoring.analyzingMessage"), allConstraints.size());  //$NON-NLS-1$
		for (Iterator it= allConstraints.iterator(); it.hasNext(); ) {
			subMonitor.worked(1);
			ITypeConstraint tc= (ITypeConstraint)it.next();
			if (! (tc instanceof SimpleTypeConstraint))
				continue;
			SimpleTypeConstraint stc= (SimpleTypeConstraint) tc;
			if (matchesSelection(stc.getLeft()))
				return stc.getLeft();
			if (matchesSelection(stc.getRight()))
				return stc.getRight();
		}
		subMonitor.done();
		pm.done();
		throw new Error(RefactoringCoreMessages.getString("ChangeTypeRefactoring.noMatchingConstraintVariable")); //$NON-NLS-1$
	}

	/**
	 * Determine if a given ConstraintVariable matches the selected ASTNode.
	 */
	private boolean matchesSelection(ConstraintVariable cv){
		if (cv instanceof ExpressionVariable){
			ExpressionVariable eLeft= (ExpressionVariable)cv;
			CompilationUnitRange cRange= eLeft.getCompilationUnitRange();
			if (cRange.getSourceRange().getOffset() == fEffectiveSelectionStart &&
				cRange.getSourceRange().getLength() == fEffectiveSelectionLength){
				return true;
			}
		} else if (cv instanceof ParameterTypeVariable){
			ParameterTypeVariable ptv = (ParameterTypeVariable)cv;
			if (fMethodBinding != null && Bindings.equals(ptv.getMethodBinding(), fMethodBinding) &&
				ptv.getParameterIndex() == fParamIndex){
				return true;
			}
		} else if (cv instanceof ReturnTypeVariable){
			ReturnTypeVariable rtv = (ReturnTypeVariable)cv;
			if (fMethodBinding != null && Bindings.equals(rtv.getMethodBinding(), fMethodBinding) &&
				fParamIndex == -1){
				return true;
			}
		}		
		return false;
	}
		
	/**
	 * Determine the set of constraint variables related to the selected
	 * expression. In addition to the expression itself, this consists of
	 * any expression that is defines-equal to it, and any expression equal
	 * to it. 
	 */
	private Collection/*<ConstraintVariable>*/ findRelevantConstraintVars(ConstraintVariable cv, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.getString("ChangeTypeRefactoring.analyzingMessage"), 150); //$NON-NLS-1$
		Collection/*<ConstraintVariable>*/ result= new HashSet();
		result.add(cv);
		ICompilationUnit[] cus= collectAffectedUnits(new SubProgressMonitor(pm, 50));
		Collection/*<ITypeConstraint>*/ allConstraints= getConstraints(cus, new SubProgressMonitor(pm, 50));

		List/*<ConstraintVariable>*/ workList= new ArrayList(result);
		while(! workList.isEmpty()){
			
			pm.worked(10);
			
			ConstraintVariable first= (ConstraintVariable)workList.remove(0);
			for (Iterator iter= allConstraints.iterator(); iter.hasNext();) {
				pm.worked(1);
				ITypeConstraint typeConstraint= (ITypeConstraint) iter.next();
				if (! typeConstraint.isSimpleTypeConstraint())
					continue;
				SimpleTypeConstraint stc= (SimpleTypeConstraint)typeConstraint;
				if (! stc.isDefinesConstraint() && ! stc.isEqualsConstraint())
					continue;
				ConstraintVariable match= match(first, stc.getLeft(), stc.getRight());
				if (match instanceof ExpressionVariable
				|| match instanceof ParameterTypeVariable
				|| match instanceof ReturnTypeVariable){
					if (! result.contains(match)){
						workList.add(match);
						result.add(match);
					}	
				}	
			}
		}
		
		pm.done();
		
		return result;
	}

	private static ConstraintVariable match(ConstraintVariable matchee, ConstraintVariable left, ConstraintVariable right) {
		if (matchee.equals(left))
			return right;
		if (matchee.equals(right))
			return left;
		return null;
	}

	/**
	 * Select the type constraints that involve the selected ASTNode.
	 */
	private Collection/*<ITypeConstraint>*/ findRelevantConstraints(Collection/*<ConstraintVariable>*/ relevantConstraintVars, 
																	IProgressMonitor pm) throws JavaModelException {

		ICompilationUnit[] cus= collectAffectedUnits(new SubProgressMonitor(pm, 100));
		
		fAllConstraints= getConstraints(cus, new SubProgressMonitor(pm, 900));
		
		pm.beginTask(RefactoringCoreMessages.getString("ChangeTypeRefactoring.analyzingMessage"), 1000 + fAllConstraints.size()); //$NON-NLS-1$
		

		if (DEBUG) printCollection("type constraints: ", fAllConstraints); //$NON-NLS-1$
		Collection/*<ITypeConstraint>*/ result= new ArrayList();
		for (Iterator it= fAllConstraints.iterator(); it.hasNext(); ) {
			ITypeConstraint tc= (ITypeConstraint)it.next();
			if (tc.isSimpleTypeConstraint()) {
				SimpleTypeConstraint stc= (SimpleTypeConstraint) tc;
				if (stc.isDefinesConstraint() || stc.isEqualsConstraint())
					continue;
				if (stc.getLeft().equals(stc.getRight())) 
					continue;
				if (isNull(stc.getLeft()))
					continue;
				if (relevantConstraintVars.contains(stc.getLeft()) || relevantConstraintVars.contains(stc.getRight()))
					result.add(tc);
			} else {
				CompositeOrTypeConstraint cotc= (CompositeOrTypeConstraint) tc;
				ITypeConstraint[] components= cotc.getConstraints();
				for (int i= 0; i < components.length; i++) {
					ITypeConstraint component= components[i];
					SimpleTypeConstraint simpleComponent= (SimpleTypeConstraint) component;
					if (relevantConstraintVars.contains(simpleComponent.getLeft()))
						result.add(tc);
				}
			}
			pm.worked(1);
		}
		if (DEBUG)
			printCollection("selected constraints: ", result); //$NON-NLS-1$
		pm.done();
		return result;
	}
	
	/**
	 * Finds the declaration of the ASTNode in a given AST at a specified offset and with a specified length
	 */
	private static ASTNode findDeclaration(final ASTNode root, final int start, final int length){
		ASTNode node= NodeFinder.perform(root, start, length);
		Assert.isTrue(node instanceof SimpleName, String.valueOf(node.getNodeType()));
		Assert.isTrue(root instanceof CompilationUnit, String.valueOf(root.getNodeType()));
		return ((CompilationUnit)root).findDeclaringNode(((SimpleName)node).resolveBinding());
	}	
	
	/**
	 * Determines the set of types for which a set of type constraints is satisfied.
	 */
	private Collection/*<IType>*/ computeValidTypes(IType originalType, 
													Collection/*<ConstraintVariable>*/ relevantVars,
													Collection/*<ITypeConstraint>*/ relevantConstraints,
													IProgressMonitor pm) throws JavaModelException {
		
		Collection/*<IType>*/ result= new HashSet();
		IJavaProject project= fCu.getJavaProject();

		Collection/*<IType>*/ allTypes = new HashSet/*<IType>*/();
		if (fTypesToConsider == SUPERTYPES_ONLY){ 
			allTypes.addAll(Arrays.asList(fTypeHierarchy.getAllSupertypes(originalType))); 
		} else {
			allTypes.addAll(Arrays.asList(fTypeHierarchy.getAllTypes()));
		}
		
		// Object is not considered a supertype of any interface type
		// (see ITypeHierarchy.getAllSuperTypes())
		if (fOriginalTypeOfSelection.isInterface()){
			IType object= JavaModelUtil.findType(fCu.getJavaProject(), "java.lang.Object"); //$NON-NLS-1$
			allTypes.add(object);
		}
		
		pm.beginTask(RefactoringCoreMessages.getString("ChangeTypeRefactoring.analyzingMessage"), allTypes.size()); //$NON-NLS-1$

		for (Iterator it= allTypes.iterator(); it.hasNext(); ) {
			IType type= (IType)it.next();
			if (isValid(project, type, relevantVars, relevantConstraints, new SubProgressMonitor(pm, 1))) {
				result.add(type);
			}
		} 		
		// "changing" to the original type is a no-op
		result.remove(originalType);

		// TODO: remove all types that are not visible --- need to check visibility in the CUs for
		//       all relevant constraint variables
		
		pm.done();
		
		return result;
	}
	
	/**
	 * Finds a type hierarchy that contains all possible replacement types. For most types, this
	 * simply gets the TypeHierarchy. However, for class Object we attempt to find a tighter
	 * bound by examining the constraints because the TypeHierarchy for Object contains 10000+
	 * types, which makes performance unacceptably slow.
	 */
	private ITypeHierarchy getTypeHierarchy(IType originalType, IProgressMonitor pm, IJavaProject project) throws JavaModelException {
		if (!originalType.getFullyQualifiedName().equals("java.lang.Object")) //$NON-NLS-1$
			return originalType.newTypeHierarchy(project, pm);
		for (Iterator it= fRelevantConstraints.iterator(); it.hasNext(); ){
			ITypeConstraint tc= (ITypeConstraint)it.next();
			if (tc instanceof SimpleTypeConstraint){
				SimpleTypeConstraint stc= (SimpleTypeConstraint)tc;
				ConstraintVariable left= stc.getLeft();
				ConstraintVariable right= stc.getRight();
				if (left.equals(fCv) && stc.isSubtypeConstraint()){
					String typeName= right.getBinding().getQualifiedName();
					IType type= JavaModelUtil.findType(project, typeName);
					return type.newTypeHierarchy(project, pm);
				}
				if (right.equals(fCv) && stc.isSubtypeConstraint()){
					String typeName= left.getBinding().getQualifiedName();
					IType type= JavaModelUtil.findType(project, typeName);
					return type.newTypeHierarchy(project, pm);
				}
			}
		}
		return originalType.newTypeHierarchy(project, pm);
	}

	/**
	 * Determines if a given type satisfies a set of type constraints.
	 */
	private boolean isValid(IJavaProject project,
							IType type,
						    Collection/*<ConstraintVariable>*/ relevantVars, 
						    Collection/*<ITypeConstraint>*/ constraints,
							IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.getString("ChangeTypeRefactoring.analyzingMessage"), constraints.size()); //$NON-NLS-1$
		for (Iterator it= constraints.iterator(); it.hasNext(); ) {
			ITypeConstraint tc= (ITypeConstraint)it.next();
			if (tc instanceof SimpleTypeConstraint) {
				if (!(isValidSimpleConstraint(project, type,  relevantVars, (SimpleTypeConstraint) tc)))
					return false;
			} else if (tc instanceof CompositeOrTypeConstraint) {
				if (!(isValidOrConstraint(project, type,  relevantVars, (CompositeOrTypeConstraint) tc)))
					return false;
			} 
			pm.worked(1);
		}
		pm.done();
		return true;
	}
	
	private boolean isValidSimpleConstraint(IJavaProject project, IType type, 
											Collection/*<ConstraintVariable>*/ relevantVars,
											SimpleTypeConstraint stc) throws JavaModelException{
		if (relevantVars.contains(stc.getLeft())) { // upper bound
			if (!isSubTypeOf(type, findType(project, stc.getRight()))) {
				return false;
			}
		}
		if (relevantVars.contains(stc.getRight())) { // lower bound
			if (!isSubTypeOf(findType(project, stc.getLeft()), type)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean isValidOrConstraint(IJavaProject project, IType type, 
										Collection/*<ConstraintVariable>*/ relevantVars,
										CompositeOrTypeConstraint cotc) throws JavaModelException{
		ITypeConstraint[] components= cotc.getConstraints();
		for (int i= 0; i < components.length; i++) {
			if (components[i] instanceof SimpleTypeConstraint) {
				SimpleTypeConstraint sc= (SimpleTypeConstraint) components[i];
				if (relevantVars.contains(sc.getLeft())) { // upper bound
					if (isSubTypeOf(type, findType(project, sc.getRight())))
						return true;
				} else if (relevantVars.contains(sc.getRight())) { // lower bound
					if (isSubTypeOf(findType(project, sc.getLeft()), type))
						return true;
				}
			} 
		}
		return false;
	}
	
	
	private IType findType(IJavaProject project, ConstraintVariable cv) throws JavaModelException {
		return JavaModelUtil.findType(project, cv.getBinding().getQualifiedName());
	}

	/**
	 * Gather constraints associated with a set of compilation units.
	 */
	private Collection/*<ITypeConstraint>*/ getConstraints(ICompilationUnit[] referringCus, IProgressMonitor pm) {
		pm.beginTask("Analyzing...", referringCus.length); //$NON-NLS-1$
		Collection/*<ITypeConstraint>*/ result= new ArrayList();
		for (int i= 0; i < referringCus.length; i++) {
			result.addAll(getConstraints(referringCus[i]));
			pm.worked(1);
		}
		pm.done();
		return result;
	}

	private List/*<ITypeConstraint>*/ getConstraints(ICompilationUnit unit) {
		if (fConstraintCache.containsKey(unit))
			return (List) fConstraintCache.get(unit);
		ConstraintCollector collector= new ConstraintCollector(new FullConstraintCreator());
		
		CompilationUnit cu= ASTCreator.createAST(unit, null);
		
		// only generate type constraints for relevant MethodDeclaration subtrees 
		if (fMethodBinding != null && fCuToSearchResultGroup.containsKey(unit)){
			SearchResultGroup group= (SearchResultGroup) fCuToSearchResultGroup.get(unit);
			ASTNode[] nodes= ASTNodeSearchUtil.getAstNodes(group.getSearchResults(), cu);
			for (int i=0; i < nodes.length; i++){
				ASTNode node = nodes[i];
				if (fMethodBinding != null){
					// find MethodDeclaration above it in the tree
					ASTNode n= node;
					while (!(n instanceof MethodDeclaration)){
						n = n.getParent();
					}
					MethodDeclaration md = (MethodDeclaration)n;
					md.accept(collector);
				}
			}
		} else {
			cu.accept(collector);
		}
		List/*<ITypeConstraint>*/ constraints= Arrays.asList(collector.getConstraints());
		fConstraintCache.put(unit, constraints);
		return constraints;
	}
	
	/**
	 * update a CompilationUnit's imports after changing the type of declarations
	 */
	private String updateImports(ICompilationUnit icu, TextBuffer buffer, MultiTextEdit rootEdit) throws CoreException{	
		ImportRewrite rewrite= new ImportRewrite(icu, JavaPreferencesSettings.getCodeGenerationSettings());
		String typeName= rewrite.addImport(fSelectedType.getFullyQualifiedName());
		rewrite.rewrite(buffer, rootEdit);
		return typeName;
	}

	//	------------------------------------------------------------------------------------------------- //
	// Miscellaneous helper methods

	/**
	 * Returns the Collection<IType> of types that can be given to the selected declaration.
	 */
	public Collection/*<IType>*/ getValidTypes() {
		return fValidTypes;
	}
	
	public IType getOriginalType(){
		return fOriginalTypeOfSelection;
	}
	
	public ITypeHierarchy getTypeHierarchy(){
		return fTypeHierarchy;
	}
	
	/**
	 * Returns the Collection<String> of names of types that can be given to the selected declaration.
	 * (used in tests only)
	 */
	public Collection/*<String>*/ getValidTypeNames() {
		Collection/*<String>*/ typeNames= new ArrayList();
		for (Iterator it= fValidTypes.iterator(); it.hasNext();) {
			IType type= (IType) it.next();
			typeNames.add(type.getFullyQualifiedName());
		}

		return typeNames;
	}

	/**
	 * Find the ASTNode for the given source text selection, if it is a type
	 * declaration, or null otherwise.
	 * @param unit The compilation unit in which the selection was made 
	 * @param offset
	 * @param length
	 * @return ASTNode
	 */
	private ASTNode getTargetNode(ICompilationUnit unit, int offset, int length) {
		CompilationUnit root= ASTCreator.createAST(unit, null);
		ASTNode node= NodeFinder.perform(root, offset, length);
		return node;
	}

	/**
	 * Determines the set of compilation units that may give rise to type constraints that
	 * we are interested in. This involves searching for overriding/overridden methods,
	 * method calls, field accesses.
	 */
	private ICompilationUnit[] collectAffectedUnits(IProgressMonitor pm) throws JavaModelException {
		// BUG: currently, no type constraints are generated for methods that are related
		// but that do not override each other. As a result, we may miss certain relevant
		// variables
		
		pm.beginTask(RefactoringCoreMessages.getString("ChangeTypeRefactoring.analyzingMessage"), 100); //$NON-NLS-1$
		
		if (fAffectedUnits != null) {
			if (DEBUG) printCollection("affected units: ", Arrays.asList(fAffectedUnits)); //$NON-NLS-1$
			pm.worked(100);
			return fAffectedUnits;
		}
		if (fMethodBinding != null) {
			IJavaProject project= fCu.getJavaProject();
			if (fMethodBinding != null) {
				
				
				IMethod selectedMethod= Bindings.findMethod(fMethodBinding, project);
				if (selectedMethod == null){
					throw new Error(RefactoringCoreMessages.getString("ChangeTypeAction.exception")); //$NON-NLS-1$
				}
				
				// the following code fragment appears to be the source of a memory leak, when
				// GT is repeatedly applied
						
				IMethod root= selectedMethod;
				if (! root.getDeclaringType().isInterface() && MethodChecks.isVirtual(root)) {
					IMethod inInterface= MethodChecks.isDeclaredInInterface(root, new SubProgressMonitor(pm, 5));
					if (inInterface != null && !inInterface.equals(root))
						root= inInterface;
				}

				// end code fragment
				
				IMethod[] rippleMethods= RippleMethodFinder.getRelatedMethods(
						root, new SubProgressMonitor(pm, 15), new IWorkingCopy[0]);
				ISearchPattern pattern= RefactoringSearchEngine.createSearchPattern(
					rippleMethods,
					IJavaSearchConstants.ALL_OCCURRENCES);

				// To compute the scope we have to use the selected method. Otherwise we
				// might start from the wrong project.
				IJavaSearchScope scope= RefactoringScopeFactory.create(selectedMethod);
				ICompilationUnit[] workingCopies= null;
				SearchResultGroup[] groups= RefactoringSearchEngine.search(
					new SubProgressMonitor(pm, 80),
					scope,
					pattern,
					workingCopies);
				
				fAffectedUnits= getCus(groups);
			}
		} else if (fFieldBinding != null) {
			try {
				IField iField= Bindings.findField(fFieldBinding, fCu.getJavaProject());
				if (iField == null){
					throw new Error(RefactoringCoreMessages.getString("ChangeTypeAction.exception")); //$NON-NLS-1$
				}
				ISearchPattern pattern=
					SearchEngine.createSearchPattern(iField,
													 IJavaSearchConstants.ALL_OCCURRENCES);
				IJavaSearchScope scope= RefactoringScopeFactory.create(iField);
				ICompilationUnit[] workingCopies= null;
				SearchResultGroup[] groups=
					RefactoringSearchEngine.search(new SubProgressMonitor(pm, 100), scope, pattern, workingCopies);
				fAffectedUnits= getCus(groups);
			} catch (JavaModelException e) {
				throw new Error(RefactoringCoreMessages.getString("ChangeTypeRefactoring.unhandledSearchException") + e); //$NON-NLS-1$
			}
		} else {
			// otherwise, selection was a local variable and we only have to search the CU
			// containing the selection
			fAffectedUnits= new ICompilationUnit[] { fCu };
		}
		if (DEBUG) {
			System.out.println("Determining affected CUs:"); //$NON-NLS-1$
			for (int i= 0; i < fAffectedUnits.length; i++) {
				System.out.println("  affected CU: " + fAffectedUnits[i].getElementName()); //$NON-NLS-1$
			}
		}
		pm.done();	
		return fAffectedUnits;
	}

	/**
	 * Store the selected new type (invoked from tests only)
	 */
	public void setSelectedType(String fullyQualifiedName) {
		try {
			fSelectedType= JavaModelUtil.findType(fCu.getJavaProject(), fullyQualifiedName);
		} catch (JavaModelException e) {
			throw new Error(RefactoringCoreMessages.getString("ChangeTypeRefactoring.failedToSelectType") + fullyQualifiedName); //$NON-NLS-1$
		}
	}

	/**
	 * Store the selected new type.
	 */
	public void setSelectedType(IType type) {
		fSelectedType= type;
	}

	//	-------------------------------------------------------------------------------------------- //
	// TODO The following utility methods should probably be moved to another class

	/**
	 * Determines if a constraint variable corresponds to the constant "null".
	 */
	private static boolean isNull(ConstraintVariable cv) {
		return cv instanceof ExpressionVariable && ((ExpressionVariable)cv).getExpressionType() == ASTNode.NULL_LITERAL;
	}


	/**
	 * For debugging.
	 */
	void printCollection(String title, Collection/*<Object>*/ l) {
		System.out.println(l.size() + " " + title); //$NON-NLS-1$
		for (Iterator it= l.iterator(); it.hasNext();) {
			System.out.println("  " + it.next()); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the compilation units that contain the search results.
	 */
	private ICompilationUnit[] getCus(SearchResultGroup[] groups) {
		List result= new ArrayList(groups.length);
		for (int i= 0; i < groups.length; i++) {
			SearchResultGroup group= groups[i];
			ICompilationUnit cu= group.getCompilationUnit();
			if (cu != null)
				result.add(WorkingCopyUtil.getWorkingCopyIfExists(cu));
				fCuToSearchResultGroup.put(cu, group);
		}
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}

	/**
	 * Returns true if and only if type1 is a direct or indirect subtype of type2.
     */
    public boolean isSubTypeOf(IType type1, IType type2) {
    	if (type2.getFullyQualifiedName().equals("java.lang.Object")) return true; //$NON-NLS-1$
		if (type1.equals(type2))
			return true;	
		if (type2.getFullyQualifiedName().equals(RefactoringCoreMessages.getString("ChangeTypeRefactoring.javaLangObject"))) // workaround for bug #46052 //$NON-NLS-1$
			return true;
		
		IType[] superTypes= fTypeHierarchy.getAllSupertypes(type1);
		for (int i= 0; i < superTypes.length; i++) {
			if (superTypes[i].equals(type2))
				return true;
		}
		return false;
	}
    
    public IJavaProject getProject(){
    	return fCu.getJavaProject();
    }
    
}
