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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ListRewrite;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.ExceptionInfo;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaRefactorings;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringAnalyzeUtil;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.RippleMethodFinder;
import org.eclipse.jdt.internal.corext.refactoring.rename.TempOccurrenceAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.AllTypesCache;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.TypeInfo;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;

public class ChangeSignatureRefactoring extends Refactoring {
	
	private final List fParameterInfos;

	private CompilationUnit fCU;
	private List fExceptionInfos;
	private TextChangeManager fChangeManager;
	private IMethod fMethod;
	private IMethod[] fRippleMethods;
	private SearchResultGroup[] fOccurrences;
	private String fReturnTypeName;
	private String fMethodName;
	private int fVisibility;
	private static final String CONST_CLASS_DECL = "class A{";//$NON-NLS-1$
	private static final String CONST_ASSIGN = " i=";		//$NON-NLS-1$
	private static final String CONST_CLOSE = ";}";			//$NON-NLS-1$
//	private static final String DEFAULT_NEW_PARAM_TYPE= "int";//$NON-NLS-1$
//	private static final String DEFAULT_NEW_PARAM_VALUE= "0"; //$NON-NLS-1$

	private ChangeSignatureRefactoring(IMethod method) throws JavaModelException{
		Assert.isNotNull(method);
		fMethod= method;
		fParameterInfos= createParameterInfoList(method);
		//fExceptionInfos is created in checkActivation
		fReturnTypeName= getInitialReturnTypeName();
		fMethodName= getInitialMethodName();
		fVisibility= getInitialMethodVisibility();
	}
	
	public static ChangeSignatureRefactoring create(IMethod method) throws JavaModelException{
		if (! isAvailable(method))
			return null;
		return new ChangeSignatureRefactoring(method);
	}
	
	public static boolean isAvailable(IMethod method) throws JavaModelException {
		if (method == null)
			return false;
		return Checks.isAvailable(method);
	}	
	
	private String getInitialReturnTypeName() throws JavaModelException{
		return Signature.toString(Signature.getReturnType(fMethod.getSignature()));
	}
	
	private String getInitialMethodName() {
		return fMethod.getElementName();
	}

	private int getInitialMethodVisibility() throws JavaModelException{
		return JdtFlags.getVisibilityCode(fMethod);
	}
	
	private static List createParameterInfoList(IMethod method) {
		try {
			String[] typeNames= method.getParameterTypes();
			String[] oldNames= method.getParameterNames();
			List result= new ArrayList(typeNames.length);
			for (int i= 0; i < oldNames.length; i++){
				result.add(new ParameterInfo(Signature.toString(typeNames[i]), oldNames[i], i));
			}
			return result;
		} catch(JavaModelException e) {
			JavaPlugin.log(e);
			return new ArrayList(0);
		}		
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("ChangeSignatureRefactoring.modify_Parameters"); //$NON-NLS-1$
	}
	
	public IMethod getMethod() {
		return fMethod;
	}
	
	public String getMethodName() {
		return fMethodName;
	}
	
	public void setNewMethodName(String newMethodName){
		Assert.isNotNull(newMethodName);
		fMethodName= newMethodName;
	}
	
	public void setNewReturnTypeName(String newReturnTypeName){
		Assert.isNotNull(newReturnTypeName);
		fReturnTypeName= newReturnTypeName;
	}
	
	public boolean canChangeNameAndReturnType(){
		try {
			return ! fMethod.isConstructor();
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			return false;
		}
	}
	
	/*
	 * @see JdtFlags
	 */
	public int getVisibility(){
		return fVisibility;
	}

	/*
	 * @see JdtFlags
	 */	
	public void setVisibility(int visibility){
		Assert.isTrue(	visibility == Modifier.PUBLIC ||
		            	visibility == Modifier.PROTECTED ||
		            	visibility == Modifier.NONE ||
		            	visibility == Modifier.PRIVATE);  
		fVisibility= visibility;            	
	}
	
	/*
	 * @see JdtFlags
	 */	
	public int[] getAvailableVisibilities() throws JavaModelException{
		if (fMethod.getDeclaringType().isInterface())
			return new int[]{Modifier.PUBLIC};
		else 	
			return new int[]{	Modifier.PUBLIC,
								Modifier.PROTECTED,
								Modifier.NONE,
								Modifier.PRIVATE};
	}
	
	/**
	 * 
	 * @return List of <code>ParameterInfo</code> objects.
	 */
	public List getParameterInfos(){
		return fParameterInfos;
	}
	
	/**
	 * @return List of <code>ExceptionInfo</code> objects.
	 */
	public List getExceptionInfos(){
		return fExceptionInfos;
	}
	
	public RefactoringStatus checkSignature() {
		RefactoringStatus result= new RefactoringStatus();
		checkForDuplicateParameterNames(result);
		if (result.hasFatalError())
			return result;
		checkParameters(result);
		if (result.hasFatalError())
			return result;
		checkReturnType(result);
		if (result.hasFatalError())
			return result;
		checkMethodName(result);
		//exceptions are ok
		return result;
	}
    
	public boolean isSignatureSameAsInitial() throws JavaModelException {
		if (! isVisibilitySameAsInitial())
			return false;
		if (! isMethodNameSameAsInitial())
			return false;
		if (! isReturnTypeSameAsInitial())
			return false;
		if (! areExceptionsSameAsInitial())
			return false;
		
		if (fMethod.getNumberOfParameters() == 0 && fParameterInfos.isEmpty())
			return true;
		
		if (areNamesSameAsInitial() && isOrderSameAsInitial() && areParameterTypesSameAsInitial())
			return true;
		
		return false;
	}

	private boolean areParameterTypesSameAsInitial() {
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (! info.isAdded() && ! info.isDeleted() && info.isTypeNameChanged())
				return false;
		}
		return true;
	}
	
	private boolean isReturnTypeSameAsInitial() throws JavaModelException {
		return fReturnTypeName.equals(getInitialReturnTypeName());
	}
	
	private boolean isMethodNameSameAsInitial() {
		return fMethodName.equals(getInitialMethodName());
	}
	
	private boolean areExceptionsSameAsInitial() {
		for (Iterator iter= fExceptionInfos.iterator(); iter.hasNext();) {
			ExceptionInfo info= (ExceptionInfo) iter.next();
			if (! info.isOld())
				return false;
		}
		return true;
	}
	
	private void checkParameters(RefactoringStatus result) {
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (info.isDeleted())
				continue;
			checkParameterType(result, info);
			if (result.hasFatalError())
				return;
			result.merge(Checks.checkTempName(info.getNewName()));
			if (result.hasFatalError())
				return;
			if (info.isAdded())	
				checkParameterDefaultValue(result, info);
		}
	}
	
	private void checkReturnType(RefactoringStatus result) {
		if (! isValidTypeName(fReturnTypeName, true)){
			String msg= RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.invalid_return_type", new String[]{fReturnTypeName}); //$NON-NLS-1$
			result.addFatalError(msg);
		}	
	}

	private void checkMethodName(RefactoringStatus result) {
		if (isMethodNameSameAsInitial() || ! canChangeNameAndReturnType())
			return;
		result.merge(Checks.checkMethodName(fMethodName));
		if (fMethodName.equals(fMethod.getDeclaringType().getElementName())) {
			String msg= RefactoringCoreMessages.getString("ChangeSignatureRefactoring.constructor_name"); //$NON-NLS-1$
			result.addFatalError(msg);
		}
	}

	private void checkParameterDefaultValue(RefactoringStatus result, ParameterInfo info) {
		if (info.getDefaultValue().trim().equals("")){ //$NON-NLS-1$
			String msg= RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.default_value", new String[]{info.getNewName()}); //$NON-NLS-1$
			result.addFatalError(msg);
			return;
		}	
		if (! isValidExpression(info.getDefaultValue())){
			String msg= RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.invalid_expression", new String[]{info.getDefaultValue()}); //$NON-NLS-1$
			result.addFatalError(msg);
		}	
	}

	private void checkParameterType(RefactoringStatus result, ParameterInfo info) {
		if (! info.isAdded() && ! info.isTypeNameChanged())
			return;
		if (info.getNewTypeName().trim().equals("")){ //$NON-NLS-1$
			String msg= RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.parameter_type", new String[]{info.getNewName()}); //$NON-NLS-1$
			if (info.isAdded() && info.getNewName().trim().equals("")) //$NON-NLS-1$
				msg= RefactoringCoreMessages.getString("ChangeSignatureRefactoring.new_parameter"); //$NON-NLS-1$
			result.addFatalError(msg);
			return;
		}	
		if (! isValidTypeName(info.getNewTypeName(), false)){
			String msg= RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.invalid_type_name", new String[]{info.getNewTypeName()}); //$NON-NLS-1$
			result.addFatalError(msg);
		}	
	}

	private static boolean isValidTypeName(String string, boolean isVoidAllowed){
		if ("".equals(string.trim())) //speed up for a common case //$NON-NLS-1$
			return false;
		if (! string.trim().equals(string))
			return false;
		if (PrimitiveType.toCode(string) == PrimitiveType.VOID)
			return isVoidAllowed;
		if (! Checks.checkTypeName(string).hasFatalError())
			return true;
		if (isPrimitiveTypeName(string))
			return true;
		StringBuffer cuBuff= new StringBuffer();
		cuBuff.append(CONST_CLASS_DECL);
		int offset= cuBuff.length();
		cuBuff.append(string)
			  .append(CONST_ASSIGN)
			  .append("null") //$NON-NLS-1$
			  .append(CONST_CLOSE);
		ASTParser p= ASTParser.newParser(AST.LEVEL_2_0);
		p.setSource(cuBuff.toString().toCharArray());
		CompilationUnit cu= (CompilationUnit) p.createAST(null);
		Selection selection= Selection.createFromStartLength(offset, string.length());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(selection, false);
		cu.accept(analyzer);
		ASTNode selected= analyzer.getFirstSelectedNode();
		if (!(selected instanceof Type))
			return false;
		Type type= (Type)selected;
		if (isVoidArrayType(type))
			return false;
		return string.equals(cuBuff.substring(type.getStartPosition(), ASTNodes.getExclusiveEnd(type)));
	}

	public static boolean isValidParameterTypeName(String string){
		return isValidTypeName(string, false);
	}
	
	private static boolean isVoidArrayType(Type type){
		if (! type.isArrayType())
			return false;
		
		ArrayType arrayType= (ArrayType)type;
		if (! arrayType.getComponentType().isPrimitiveType())
			return false;
		PrimitiveType primitiveType= (PrimitiveType)arrayType.getComponentType();
		return (primitiveType.getPrimitiveTypeCode() == PrimitiveType.VOID);
	}
	
	private static boolean isValidExpression(String string){
		String trimmed= string.trim();
		if ("".equals(trimmed)) //speed up for a common case //$NON-NLS-1$
			return false;
		StringBuffer cuBuff= new StringBuffer();
		cuBuff.append(CONST_CLASS_DECL)
			  .append("Object") //$NON-NLS-1$
			  .append(CONST_ASSIGN);
		int offset= cuBuff.length();
		cuBuff.append(trimmed)
			  .append(CONST_CLOSE);
		ASTParser p= ASTParser.newParser(AST.LEVEL_2_0);
		p.setSource(cuBuff.toString().toCharArray());
		CompilationUnit cu= (CompilationUnit) p.createAST(null);
		Selection selection= Selection.createFromStartLength(offset, trimmed.length());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(selection, false);
		cu.accept(analyzer);
		ASTNode selected= analyzer.getFirstSelectedNode();
		return (selected instanceof Expression) && 
				trimmed.equals(cuBuff.substring(selected.getStartPosition(), ASTNodes.getExclusiveEnd(selected)));
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
			RefactoringStatus result= Checks.checkIfCuBroken(fMethod);
			if (result.hasFatalError())
				return result;
			IMethod orig= (IMethod)WorkingCopyUtil.getOriginal(fMethod);
			if (orig == null || ! orig.exists()){
				String message= RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.method_deleted", getCu().getElementName());//$NON-NLS-1$
				return RefactoringStatus.createFatalErrorStatus(message);
			}
			fMethod= orig;
			
			if (MethodChecks.isVirtual(fMethod) && !fMethod.getDeclaringType().isInterface()){
				result.merge(MethodChecks.checkIfComesFromInterface(getMethod(), new SubProgressMonitor(pm, 1)));
				if (result.hasFatalError())
					return result;	
				
				result.merge(MethodChecks.checkIfOverridesAnother(getMethod(), new SubProgressMonitor(pm, 1)));
				if (result.hasFatalError())
					return result;			
			} 
			if (fMethod.getDeclaringType().isInterface()){
				result.merge(MethodChecks.checkIfOverridesAnother(getMethod(), new SubProgressMonitor(pm, 1)));
				if (result.hasFatalError())
					return result;
			}
			fCU= new RefactoringASTParser(AST.LEVEL_2_0).parse(getCu(), true);
			result.merge(createExceptionInfoList());
			
			return result;
		} finally{
			pm.done();
		}
	}

	private RefactoringStatus createExceptionInfoList() {
		fExceptionInfos= new ArrayList(0);
		try {
			IJavaProject project= fMethod.getJavaProject();
			ASTNode nameNode= NodeFinder.perform(fCU, fMethod.getNameRange());
			if (nameNode == null || ! (nameNode instanceof Name) || ! (nameNode.getParent() instanceof MethodDeclaration))
				return null;
			MethodDeclaration methodDeclaration= (MethodDeclaration) nameNode.getParent();
			List exceptions= methodDeclaration.thrownExceptions();
			List result= new ArrayList(exceptions.size());
			for (int i= 0; i < exceptions.size(); i++){
				Name name= (Name) exceptions.get(i);
				ITypeBinding typeBinding= name.resolveTypeBinding();
				if (typeBinding == null)
					return RefactoringStatus.createFatalErrorStatus(
							RefactoringCoreMessages.getString("ChangeSignatureRefactoring.no_exception_binding")); //$NON-NLS-1$
				IType type= Bindings.findType(typeBinding, project);
				result.add(ExceptionInfo.createInfoForOldException(type, typeBinding));
			}
			fExceptionInfos= result;
			return null;
		} catch(JavaModelException e) {
			JavaPlugin.log(e);
			return null;
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("ChangeSignatureRefactoring.checking_preconditions"), 8); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			clearManagers();

			if (isSignatureSameAsInitial())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ChangeSignatureRefactoring.unchanged")); //$NON-NLS-1$
			result.merge(checkSignature());
			if (result.hasFatalError())
				return result;

			fRippleMethods= RippleMethodFinder.getRelatedMethods(fMethod, new SubProgressMonitor(pm, 1), null);
			fOccurrences= findOccurrences(new SubProgressMonitor(pm, 1));
			
			result.merge(checkVisibilityChanges());
			
			//TODO:
			// We need a common way of dealing with possible compilation errors for all occurrences,
			// including visibility problems, shadowing and missing throws declarations.
			
			if (! isOrderSameAsInitial())
				result.merge(checkReorderings(new SubProgressMonitor(pm, 1)));
			else
				pm.worked(1);
			
			//TODO (bug 58616): check whether changed signature already exists somewhere in the ripple,
			// - error if exists
			// - warn if exists with different parameter types (may cause overloading)
			
			if (! areNamesSameAsInitial())
				result.merge(checkRenamings(new SubProgressMonitor(pm, 1)));
			else
				pm.worked(1);
			if (result.hasFatalError())
				return result;
			
			result.merge(checkAndResolveTypes(new SubProgressMonitor(pm, 1)));

			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1), result);

			if (mustAnalyzeAstOfDeclaringCu()) 
				result.merge(checkCompilationofDeclaringCu()); //TODO: should also check in ripple methods (move into createChangeManager)
			if (result.hasFatalError())
				return result;

			result.merge(validateModifiesFiles());
			return result;
		} finally {
			pm.done();
		}
	}

	private void clearManagers() {
		fChangeManager= null;
	}

	private RefactoringStatus checkAndResolveTypes(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		List notDeletedParams= getNotDeletedInfos();
		pm.beginTask("", notDeletedParams.size() + 1); //$NON-NLS-1$
		for (Iterator iter= notDeletedParams.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (! info.isTypeNameChanged() && ! info.isAdded())
				continue;
			String typeName= getElementTypeName(info.getNewTypeName());
			if (isPrimitiveTypeName(typeName))
				continue;
			String qualifiedTypeName= resolveType(typeName, result, new SubProgressMonitor(pm, 1));
			info.setNewTypeName(getFullTypeName(info.getNewTypeName(), qualifiedTypeName));
		}
		if (! isReturnTypeSameAsInitial()) {
			String typeName= getElementTypeName(fReturnTypeName);
			if (! isPrimitiveTypeName(typeName)) {
				String qualifiedTypeName= resolveType(typeName, result, new SubProgressMonitor(pm, 1));
				fReturnTypeName= getFullTypeName(fReturnTypeName, qualifiedTypeName);
			}
		}
		pm.done();
		return result;
	}
	
	private String resolveType(String elementTypeName, RefactoringStatus status, IProgressMonitor pm) throws CoreException {
		String[][] fqns= getMethod().getDeclaringType().resolveType(elementTypeName);
		if (fqns != null) {
			if (fqns.length == 1) {
				return JavaModelUtil.concatenateName(fqns[0][0], fqns[0][1]);
			} else if (fqns.length > 1){
				String[] keys= {elementTypeName, String.valueOf(fqns.length)};
				String msg= RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.ambiguous", keys); //$NON-NLS-1$
				status.addError(msg);
				return elementTypeName;
			}
		}
		
		List typeRefsFound= findTypeInfos(elementTypeName, pm);
		if (typeRefsFound.size() == 0){
			String[] keys= {elementTypeName};
			String msg= RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.not_unique", keys); //$NON-NLS-1$
			status.addError(msg);
			return elementTypeName;
		} else if (typeRefsFound.size() == 1){
			TypeInfo typeInfo= (TypeInfo) typeRefsFound.get(0);
			return typeInfo.getFullyQualifiedName();
		} else {
			Assert.isTrue(typeRefsFound.size() > 1);
			String[] keys= {elementTypeName, String.valueOf(typeRefsFound.size())};
			String msg= RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.ambiguous", keys); //$NON-NLS-1$
			status.addError(msg);
			return elementTypeName;
		}
	}

	private String getFullTypeName(String typeName, String qualifiedName) {
		int dimStart= typeName.indexOf('[');
		if (dimStart != -1)
			return qualifiedName + typeName.substring(dimStart);
		return qualifiedName;
	}

	private List findTypeInfos(String typeName, IProgressMonitor pm) throws JavaModelException {
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaProject[]{getMethod().getJavaProject()}, true);
		IPackageFragment currPackage= getMethod().getDeclaringType().getPackageFragment();
		TypeInfo[] infos= AllTypesCache.getTypesForName(typeName, scope, pm);
		List typeRefsFound= new ArrayList();
		for (int i= 0; i < infos.length; i++) {
			TypeInfo curr= infos[i];
			IType type= curr.resolveType(scope);
			if (type != null && JavaModelUtil.isVisible(type, currPackage)) {
				typeRefsFound.add(curr);
			}
		}
		return typeRefsFound;
	}
	
	private static boolean isPrimitiveTypeName(String typeName){
		return PrimitiveType.toCode(typeName) != null;
	}
	
	private static String getElementTypeName(String typeName) {
		if (typeName.indexOf('[') != -1)
			typeName= typeName.substring(0, typeName.indexOf('['));
		return typeName.trim();
	}
	
	private RefactoringStatus checkVisibilityChanges() throws JavaModelException {
		if (isVisibilitySameAsInitial())
			return null;
	    if (fRippleMethods.length == 1)
	    	return null;
	    Assert.isTrue(getInitialMethodVisibility() != Modifier.PRIVATE);
	    if (fVisibility == Modifier.PRIVATE)
	    	return RefactoringStatus.createWarningStatus(RefactoringCoreMessages.getString("ChangeSignatureRefactoring.non-virtual")); //$NON-NLS-1$
		return null;
	}
	
	public String getMethodSignaturePreview() throws JavaModelException{
		StringBuffer buff= new StringBuffer();
		
		buff.append(getPreviewOfVisibityString());
		if (Flags.isStatic(getMethod().getFlags()))
			buff.append("static "); //$NON-NLS-1$
		if (! getMethod().isConstructor())
			buff.append(getReturnTypeString())
				.append(' ');

		buff.append(getMethodName())
			.append(Signature.C_PARAM_START)
			.append(getMethodParameters())
			.append(Signature.C_PARAM_END);
		
		buff.append(getMethodThrows());
		
		return buff.toString();
	}

	private String getPreviewOfVisibityString() {
		String visibilityString= JdtFlags.getVisibilityString(fVisibility);
		if ("".equals(visibilityString)) //$NON-NLS-1$
			return visibilityString;
		return visibilityString + ' ';
	}

	private String getMethodThrows() {
		final String throwsString= " throws "; //$NON-NLS-1$
		StringBuffer buff= new StringBuffer(throwsString);
		for (Iterator iter= fExceptionInfos.iterator(); iter.hasNext(); ) {
			ExceptionInfo info= (ExceptionInfo) iter.next();
			if (! info.isDeleted()) {
				buff.append(info.getType().getElementName());
				buff.append(", "); //$NON-NLS-1$
			}
		}
		if (buff.length() == throwsString.length())
			return ""; //$NON-NLS-1$
		buff.delete(buff.length() - 2, buff.length());
		return buff.toString();
	}

	
	private void checkForDuplicateParameterNames(RefactoringStatus result){
		Set found= new HashSet();
		Set doubled= new HashSet();
		for (Iterator iter = getNotDeletedInfos().iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo)iter.next();
			String newName= info.getNewName();
			if (found.contains(newName) && !doubled.contains(newName)){
				result.addFatalError(RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.duplicate_name", newName));//$NON-NLS-1$	
				doubled.add(newName);
			} else {
				found.add(newName);
			}	
		}
	}
	
	private ICompilationUnit getCu() {
		return fMethod.getCompilationUnit();
	}
	
	private boolean mustAnalyzeAstOfDeclaringCu() throws JavaModelException{
		if (JdtFlags.isAbstract(getMethod()))
			return false;
		else if (JdtFlags.isNative(getMethod()))
			return false;
		else if (getMethod().getDeclaringType().isInterface())
			return false;
		else 
			return true;
	}
	
	private RefactoringStatus checkCompilationofDeclaringCu() throws CoreException {
		ICompilationUnit cu= getCu();
		TextChange change= fChangeManager.get(cu);
		String newCuSource= change.getPreviewContent();
		ASTParser p= ASTParser.newParser(AST.LEVEL_2_0);
		p.setSource(newCuSource.toCharArray());
		p.setUnitName(cu.getElementName());
		p.setProject(cu.getJavaProject());
		p.setCompilerOptions(RefactoringASTParser.getCompilerOptions(cu));
		CompilationUnit newCUNode= (CompilationUnit) p.createAST(null);
		IProblem[] problems= RefactoringAnalyzeUtil.getIntroducedCompileProblems(newCUNode, fCU);
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < problems.length; i++) {
			IProblem problem= problems[i];
			if (shouldReport(problem))
				result.addEntry(JavaRefactorings.createStatusEntry(problem, newCuSource));
		}
		return result;
	}
		
	private static boolean shouldReport(IProblem problem) {
		if (! problem.isError())
			return false;
		if (problem.getID() == IProblem.ArgumentTypeNotFound) //reported when trying to import
			return false;
		return true;	
	}

	public String getReturnTypeString() {
		return fReturnTypeName;
	}	

	private String getMethodParameters() {
		StringBuffer buff= new StringBuffer();
		int i= 0;
		for (Iterator iter= getNotDeletedInfos().iterator(); iter.hasNext(); i++) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (i != 0 )
				buff.append(", ");  //$NON-NLS-1$
			buff.append(createDeclarationString(info));
		}
		return buff.toString();
	}
		
	private List getDeletedInfos(){
		List result= new ArrayList(1);
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (info.isDeleted())
				result.add(info);
		}
		return result;
	}
	
	private List getNotDeletedInfos(){
		List all= new ArrayList(fParameterInfos);
		all.removeAll(getDeletedInfos());
		return all;
	}
	
	private boolean areNamesSameAsInitial() {
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (info.isRenamed())
				return false;
		}
		return true;
	}

	private boolean isOrderSameAsInitial(){
		int i= 0;
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext(); i++) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (info.getOldIndex() != i) // includes info.isAdded()
				return false;
			if (info.isDeleted())
				return false;
		}
		return true;
	}

	private RefactoringStatus checkReorderings(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("ChangeSignatureRefactoring.checking_preconditions"), 1); //$NON-NLS-1$
			return checkNativeMethods();
		} finally{
			pm.done();
		}	
	}
	
	private RefactoringStatus checkRenamings(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("ChangeSignatureRefactoring.checking_preconditions"), 1); //$NON-NLS-1$
			return checkParameterNamesInRippleMethods();
		} finally{
			pm.done();
		}	
	}
	
	private RefactoringStatus checkParameterNamesInRippleMethods() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		Set newParameterNames= getNewParameterNamesList();
		for (int i= 0; i < fRippleMethods.length; i++) {
			String[] paramNames= fRippleMethods[i].getParameterNames();
			for (int j= 0; j < paramNames.length; j++) {
				if (newParameterNames.contains(paramNames[j])){
					String[] args= new String[]{JavaElementUtil.createMethodSignature(fRippleMethods[i]), paramNames[j]};
					String msg= RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.already_has", args); //$NON-NLS-1$
					RefactoringStatusContext context= JavaStatusContext.create(fRippleMethods[i].getCompilationUnit(), fRippleMethods[i].getNameRange());
					result.addError(msg, context);
				}	
			}
		}
		return result;
	}
	
	private Set getNewParameterNamesList() {
		Set oldNames= getOriginalParameterNames();
		Set currentNames= getNamesOfNotDeletedParameters();
		currentNames.removeAll(oldNames);
		return currentNames;
	}
	
	private Set getNamesOfNotDeletedParameters() {
		Set result= new HashSet();
		for (Iterator iter= getNotDeletedInfos().iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			result.add(info.getNewName());
		}
		return result;
	}
	
	private Set getOriginalParameterNames() {
		Set result= new HashSet();
		for (Iterator iter= fParameterInfos.iterator(); iter.hasNext();) {
			ParameterInfo info= (ParameterInfo) iter.next();
			if (! info.isAdded())
				result.add(info.getOldName());
		}
		return result;
	}
	
	private RefactoringStatus checkNativeMethods() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fRippleMethods.length; i++) {
			if (JdtFlags.isNative(fRippleMethods[i])){
				String message= RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.native", //$NON-NLS-1$
					new String[]{JavaElementUtil.createMethodSignature(fRippleMethods[i]), JavaModelUtil.getFullyQualifiedName(fRippleMethods[i].getDeclaringType())});
				result.addError(message, JavaStatusContext.create(fRippleMethods[i]));			
			}								
		}
		return result;
	}

	private IFile[] getAllFilesToModify(){
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles(){
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}

	//--  changes ----
	public Change createChange(IProgressMonitor pm) {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			return new DynamicValidationStateChange(RefactoringCoreMessages.getString("ChangeSignatureRefactoring.restructure_parameters"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally{
			pm.done();
			clearManagers();
		}	
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm, RefactoringStatus result) throws CoreException {
		pm.beginTask(RefactoringCoreMessages.getString("ChangeSignatureRefactoring.preview"), 2); //$NON-NLS-1$
		TextChangeManager manager= new TextChangeManager();
		boolean isNoArgConstructor= isNoArgConstructor();
		Map namedSubclassMapping= null;
		if (isNoArgConstructor){
			//create only when needed;
			namedSubclassMapping= createNamedSubclassMapping(new SubProgressMonitor(pm, 1));
		}else{
			pm.worked(1);
		}
		for (int i= 0; i < fOccurrences.length; i++) {
			SearchResultGroup group= fOccurrences[i];
			ICompilationUnit cu= group.getCompilationUnit();
			if (cu == null)
				continue;
			CompilationUnit cuNode;
			if (cu.equals(getCu()))
				cuNode= fCU;
			else
				cuNode= new RefactoringASTParser(AST.LEVEL_2_0).parse(cu, true);
			OldASTRewrite rewrite= new OldASTRewrite(cuNode);
			ImportRewrite importRewrite= new ImportRewrite(cu);
			ASTNode[] nodes= ASTNodeSearchUtil.findNodes(group.getSearchResults(), cuNode);
			for (int j= 0; j < nodes.length; j++) {
				createOccurrenceUpdate(nodes[j], rewrite, importRewrite, result).updateNode();
			}
			if (isNoArgConstructor && namedSubclassMapping.containsKey(cu)){
				//only non-anonymous subclasses may have noArgConstructors to modify - see bug 43444
				Set subtypes= (Set)namedSubclassMapping.get(cu);
				for (Iterator iter= subtypes.iterator(); iter.hasNext();) {
					IType subtype= (IType) iter.next();
					TypeDeclaration subtypeNode= ASTNodeSearchUtil.getTypeDeclarationNode(subtype, cuNode);
					if (subtypeNode != null)
						modifyImplicitCallsToNoArgConstructor(subtypeNode, rewrite);
				}
			}
			addTextEditFromRewrite(manager, cu, rewrite);
			addImportEdit(manager, cu, importRewrite);
		}
		
		pm.done();
		return manager;
	}

	//Map<ICompilationUnit, Set<IType>>
	private Map createNamedSubclassMapping(IProgressMonitor pm) throws JavaModelException{
		IType[] subclasses= getSubclasses(pm);
		Map result= new HashMap();
		for (int i= 0; i < subclasses.length; i++) {
			IType subclass= subclasses[i];
			if (subclass.isAnonymous())
				continue;
			ICompilationUnit cu= subclass.getCompilationUnit();
			if (! result.containsKey(cu))
				result.put(cu, new HashSet());
			((Set)result.get(cu)).add(subclass);
		}
		return result;
	}
	
	private void modifyImplicitCallsToNoArgConstructor(TypeDeclaration subclass, OldASTRewrite rewrite) {
		MethodDeclaration[] constructors= getAllConstructors(subclass);
		if (constructors.length == 0){
			addNewConstructorToSubclass(subclass, rewrite);
		} else {
			for (int i= 0; i < constructors.length; i++) {
				if (! containsImplicitCallToSuperConstructor(constructors[i]))
					continue;
				SuperConstructorInvocation superCall= addExplicitSuperConstructorCall(constructors[i], rewrite);
				rewrite.markAsInserted(superCall);
			}
		}
	}
	
	private SuperConstructorInvocation addExplicitSuperConstructorCall(MethodDeclaration constructor, OldASTRewrite rewrite) {
		SuperConstructorInvocation superCall= constructor.getAST().newSuperConstructorInvocation();
		addArgumentsToNewSuperConstructorCall(superCall, rewrite);
		constructor.getBody().statements().add(0, superCall);
		return superCall;
	}
	
	private void addArgumentsToNewSuperConstructorCall(SuperConstructorInvocation superCall, OldASTRewrite rewrite) {
		int i= 0;
		for (Iterator iter= getNotDeletedInfos().iterator(); iter.hasNext(); i++) {
			ParameterInfo info= (ParameterInfo) iter.next();
			superCall.arguments().add(i, createNewExpression(rewrite, info));
		}
	}
	
	private static boolean containsImplicitCallToSuperConstructor(MethodDeclaration constructor) {
		Assert.isTrue(constructor.isConstructor());
		Block body= constructor.getBody();
		if (body == null)
			return false;
		if (body.statements().size() == 0)
			return true;
		if (body.statements().get(0) instanceof ConstructorInvocation)
			return false;
		if (body.statements().get(0) instanceof SuperConstructorInvocation)
			return false;
		return true;
	}
	
	private void addNewConstructorToSubclass(TypeDeclaration subclass, OldASTRewrite rewrite) {
		AST ast= subclass.getAST();
		MethodDeclaration newConstructor= ast.newMethodDeclaration();
		newConstructor.setName(ast.newSimpleName(subclass.getName().getIdentifier()));
		newConstructor.setConstructor(true);
		newConstructor.setBody(ast.newBlock());
		newConstructor.setExtraDimensions(0);
		newConstructor.setJavadoc(null);
		newConstructor.setModifiers(getAccessModifier(subclass));
		newConstructor.setReturnType(ast.newPrimitiveType(PrimitiveType.VOID));
		
		addExplicitSuperConstructorCall(newConstructor, rewrite);
		subclass.bodyDeclarations().add(0, newConstructor); // ok to add as first ???
		rewrite.markAsInserted(newConstructor);
	}
	
	private static int getAccessModifier(TypeDeclaration subclass) {
		int modifiers= subclass.getModifiers();
		if (Modifier.isPublic(modifiers))
			return Modifier.PUBLIC;
		else if (Modifier.isProtected(modifiers))
			return Modifier.PROTECTED;
		else if (Modifier.isPrivate(modifiers))
			return Modifier.PRIVATE;
		else
			return Modifier.NONE;
	}
	
	private MethodDeclaration[] getAllConstructors(TypeDeclaration typeDeclaration) {
		MethodDeclaration[] methods= typeDeclaration.getMethods();
		List result= new ArrayList(1);
		for (int i= 0; i < methods.length; i++) {
			if (methods[i].isConstructor())
				result.add(methods[i]);
		}
		return (MethodDeclaration[]) result.toArray(new MethodDeclaration[result.size()]);
	}
	
	private IType[] getSubclasses(IProgressMonitor pm) throws JavaModelException {
		return fMethod.getDeclaringType().newTypeHierarchy(pm).getSubclasses(fMethod.getDeclaringType());
	}
	
	private boolean isNoArgConstructor() throws JavaModelException {
		return fMethod.isConstructor() && fMethod.getNumberOfParameters() == 0;
	}
	
	private void addTextEditFromRewrite(TextChangeManager manager, ICompilationUnit cu, OldASTRewrite rewrite) throws CoreException {
		TextBuffer textBuffer= TextBuffer.create(cu.getBuffer().getContents());
		TextEdit resultingEdits= new MultiTextEdit();
		rewrite.rewriteNode(textBuffer, resultingEdits);

		if (resultingEdits.hasChildren()) {
		    TextChange textChange= manager.get(cu);
		    TextChangeCompatibility.addTextEdit(textChange, RefactoringCoreMessages.getString("ChangeSignatureRefactoring.modify_parameters"), resultingEdits); //$NON-NLS-1$
		}
		rewrite.removeModifications();
	}
	
	private void addImportEdit(TextChangeManager manager, ICompilationUnit cu, ImportRewrite importRewrite) throws CoreException {
		TextBuffer textBuffer= TextBuffer.create(cu.getBuffer().getContents());
		TextEdit resultingEdits= importRewrite.createEdit(textBuffer.getDocument());
		if (! importRewrite.isEmpty()) {
		    TextChange textChange= manager.get(cu);
		    TextChangeCompatibility.addTextEdit(textChange, RefactoringCoreMessages.getString("ChangeSignatureRefactoring.add_imports"), resultingEdits); //$NON-NLS-1$
		}
	}
	
	private static Expression createNewExpression(OldASTRewrite rewrite, ParameterInfo info) {
		return (Expression) rewrite.createStringPlaceholder(info.getDefaultValue(), ASTNode.METHOD_INVOCATION);
	}

	private boolean isVisibilitySameAsInitial() throws JavaModelException {
		return fVisibility == JdtFlags.getVisibilityCode(fMethod);
	}
	
	private IJavaSearchScope createRefactoringScope()  throws JavaModelException{
		return RefactoringScopeFactory.create(fMethod);
	}
	
	private SearchResultGroup[] findOccurrences(IProgressMonitor pm) throws JavaModelException{
		if (fMethod.isConstructor()){
			// workaround for bug 27236:
			return ConstructorReferenceFinder.getConstructorOccurrences(fMethod, pm);
		}else{	
			ISearchPattern pattern= RefactoringSearchEngine.createSearchPattern(fRippleMethods, IJavaSearchConstants.ALL_OCCURRENCES);
			return RefactoringSearchEngine.search(pm, createRefactoringScope(), pattern);
		}
	}
	
	private static String createDeclarationString(ParameterInfo info) {
		return info.getNewTypeName() + " " + info.getNewName(); //$NON-NLS-1$
	}

	private OccurrenceUpdate createOccurrenceUpdate(ASTNode node, OldASTRewrite rewrite, ImportRewrite importRewrite, RefactoringStatus result) {
		if (isReferenceNode(node))
			return new ReferenceUpdate(node, rewrite, importRewrite, result);
		
		else if (node instanceof SimpleName && node.getParent() instanceof MethodDeclaration)
			return new DeclarationUpdate((MethodDeclaration) node.getParent(), rewrite, importRewrite, result);
		
		else if (node instanceof SimpleName && 
				(node.getParent() instanceof MemberRef || node.getParent() instanceof MethodRef))
			return new DocReferenceUpdate(node.getParent(), rewrite, importRewrite, result);
		
		else
			return new NullOccurrenceUpdate(node, rewrite, importRewrite, result);
	}

	private static boolean isReferenceNode(ASTNode node){
		switch (node.getNodeType()) {
			case ASTNode.METHOD_INVOCATION :
			case ASTNode.SUPER_METHOD_INVOCATION :
			case ASTNode.CLASS_INSTANCE_CREATION :
			case ASTNode.CONSTRUCTOR_INVOCATION :
			case ASTNode.SUPER_CONSTRUCTOR_INVOCATION :
				return true;

			default :
				return false;
		}
	}

	abstract class OccurrenceUpdate {
		protected OldASTRewrite fRewrite;
		protected ImportRewrite fImportRewrite;
		protected OccurrenceUpdate(OldASTRewrite rewrite, ImportRewrite importRewrite) {
			fRewrite= rewrite;
			fImportRewrite= importRewrite;
		}
		
		protected final ICompilationUnit getOccurrenceCu() {
			return fImportRewrite.getCompilationUnit();
		}
		
		public abstract void updateNode() throws JavaModelException;
		
		protected final void reshuffleElements() {
			List paramguments= getParamguments();
			ASTNode[] nodes= (ASTNode[]) paramguments.toArray(new ASTNode[paramguments.size()]);
			
			// remove surplus nodes in rewrite (from end):
			deleteExcessiveElements(nodes);
			
			List nonDeletedInfos= getNotDeletedInfos();
			ASTNode[] newPermutation= new ASTNode[nonDeletedInfos.size()];
			
			// fill newPermutation with new / copied nodes:
			for (int i=0; i < newPermutation.length; i++) {
				ParameterInfo info= (ParameterInfo) nonDeletedInfos.get(i);
				
				if (info.isAdded()) {
					newPermutation[i]= createNewParamgument(info);
				} else if (info.getOldIndex() != i) {
					if (fRewrite.isReplaced(nodes[info.getOldIndex()])) // if actual argument already changed ...
						newPermutation[i]= fRewrite.getReplacingNode(nodes[info.getOldIndex()]);
				 	else
						newPermutation[i]= fRewrite.createCopyTarget(nodes[info.getOldIndex()]);
				} else {
					newPermutation[i]= nodes[i];
				}
			}
			
			// replace changing nodes with the ones from newPermutation:
			for (int i= 0; i < Math.min(nodes.length, newPermutation.length); i++) {
				if (nodes[i] != newPermutation[i])
					fRewrite.replace(nodes[i], newPermutation[i], null);
			}
			
			// add fresh nodes at end:
			for (int i= nodes.length; i < newPermutation.length; i++) {
				ParameterInfo info= (ParameterInfo) nonDeletedInfos.get(i);
				if (info.isAdded()){
					//TODO: Bug 54362: m() -> m(int) adds parameter twice!
					ASTNode newElement= createNewParamgument(info);
					paramguments.add(i, newElement);
					fRewrite.markAsInserted(newElement);
				} else {
					paramguments.add(i, newPermutation[i]);
					fRewrite.markAsInserted(newPermutation[i]);
				}	
			}
		}
		
		/** @return List of parameters or arguments */
		protected abstract List getParamguments();

		protected final void changeParamguments() {
			for (Iterator iter= getParameterInfos().iterator(); iter.hasNext();) {
				ParameterInfo info= (ParameterInfo) iter.next();
				if (info.isAdded() || info.isDeleted())
					continue;
				
				if (info.isRenamed())
					changeParamgumentName(info);
		
				if (info.isTypeNameChanged())
					changeParamgumentType(info);
			}
		}

		protected void changeParamgumentName(ParameterInfo info) {
			// no-op
		}

		protected void changeParamgumentType(ParameterInfo info) {
			// no-op
		}

		protected final void replaceTypeNode(Type typeNode, String newTypeName){
			Type newTypeNode= createNewTypeNode(newTypeName);
			fRewrite.replace(typeNode, newTypeNode, null);
		}
	
		private void deleteExcessiveElements(ASTNode[] nodes) {
			for (int i= getNotDeletedInfos().size(); i < nodes.length; i++) {
				fRewrite.remove(nodes[i], null);
			}
		}

		protected abstract ASTNode createNewParamgument(ParameterInfo info);

		protected abstract SimpleName getMethodNameNode();

		protected final void changeMethodName() {
			if (! isMethodNameSameAsInitial()) {
				SimpleName nameNode= getMethodNameNode();
				SimpleName newNameNode= nameNode.getAST().newSimpleName(fMethodName);
				fRewrite.replace(nameNode, newNameNode, null);
			}
		}

		protected final Type createNewTypeNode(String newTypeName) {
			String elementTypeName= getElementTypeName(newTypeName);
			String importedTypeName= fImportRewrite.addImport(elementTypeName);
			int dimensions= getArrayDimensions(newTypeName);
			
			Type newTypeNode= (Type) fRewrite.createStringPlaceholder(importedTypeName, ASTNode.SIMPLE_TYPE);
			if (dimensions != 0)
				newTypeNode= fRewrite.getAST().newArrayType(newTypeNode, dimensions);
			return newTypeNode;
		}

		private int getArrayDimensions(String typeName) {
			int dims= 0;
			for (int i= 0; i < typeName.length(); i++) {
				if (typeName.charAt(i) == '[')
					dims++;
			}
			return dims;
		}
	}
	
	class ReferenceUpdate extends OccurrenceUpdate {
		/** isReferenceNode(fNode) */
		private ASTNode fNode;

		protected ReferenceUpdate(ASTNode node, OldASTRewrite rewrite, ImportRewrite importRewrite, RefactoringStatus result) {
			super(rewrite, importRewrite);
			fNode= node; //holds: Assert.isTrue(isReferenceNode(node));
		}

		public void updateNode() {
			reshuffleElements();
			changeMethodName();
		}

		protected List getParamguments() {
			if (fNode instanceof MethodInvocation)	
				return ((MethodInvocation) fNode).arguments();
				
			if (fNode instanceof SuperMethodInvocation)	
				return ((SuperMethodInvocation) fNode).arguments();
				
			if (fNode instanceof ClassInstanceCreation)	
				return ((ClassInstanceCreation) fNode).arguments();
				
			if (fNode instanceof ConstructorInvocation)	
				return ((ConstructorInvocation) fNode).arguments();
				
			if (fNode instanceof SuperConstructorInvocation)	
				return ((SuperConstructorInvocation) fNode).arguments();
				
			return new ArrayList(0);
		}
		
		protected ASTNode createNewParamgument(ParameterInfo info) {
			if (isRecursiveReference())
				return createNewExpressionRecursive(info);
			else
				return createNewExpression(fRewrite, info);
		}

		private Expression createNewExpressionRecursive(ParameterInfo info) {
			return (Expression) fRewrite.createStringPlaceholder(info.getNewName(), ASTNode.METHOD_INVOCATION);
		}

		protected SimpleName getMethodNameNode() {
			if (fNode instanceof MethodInvocation)	
				return ((MethodInvocation)fNode).getName();
				
			if (fNode instanceof SuperMethodInvocation)	
				return ((SuperMethodInvocation)fNode).getName();
				
			return null;	
		}
		
		private boolean isRecursiveReference() {
			MethodDeclaration enclosingMethodDeclaration= (MethodDeclaration) ASTNodes.getParent(fNode, MethodDeclaration.class);
			if (enclosingMethodDeclaration == null)
				return false;
			
			IMethodBinding enclosingMethodBinding= enclosingMethodDeclaration.resolveBinding();
		
			if (fNode instanceof MethodInvocation)	
				return enclosingMethodBinding == ((MethodInvocation)fNode).resolveMethodBinding();
				
			if (fNode instanceof SuperMethodInvocation) {
				IMethodBinding methodBinding= ((SuperMethodInvocation)fNode).resolveMethodBinding();
				return isSameMethod(methodBinding, enclosingMethodBinding);
			}
				
			if (fNode instanceof ClassInstanceCreation)	
				return enclosingMethodBinding == ((ClassInstanceCreation)fNode).resolveConstructorBinding();
				
			if (fNode instanceof ConstructorInvocation)	
				return enclosingMethodBinding == ((ConstructorInvocation)fNode).resolveConstructorBinding();
				
			if (fNode instanceof SuperConstructorInvocation) {
				return false; //Constructors don't override -> enclosing has not been changed -> no recursion
			}

			Assert.isTrue(false);
			return false;
		}
		
		/**
		 * @return true iff
		 * 		<ul><li>the methods are both constructors with same argument types, or</li>
		 *	 		<li>the methods have the same name and the same argument types</li></ul>
		 */
		private boolean isSameMethod(IMethodBinding m1, IMethodBinding m2) {
			if (m1.isConstructor()) {
				if (! m2.isConstructor())
					return false;
			} else {
				if (! m1.getName().equals(m2.getName()))
					return false;
			}
			return Bindings.equals(m1.getParameterTypes(), m2.getParameterTypes());
		}

	}

	class DeclarationUpdate extends OccurrenceUpdate {
		private MethodDeclaration fMethDecl;
		private RefactoringStatus fResult;

		protected DeclarationUpdate(MethodDeclaration decl, OldASTRewrite rewrite, ImportRewrite importRewrite, RefactoringStatus result) {
			super(rewrite, importRewrite);
			fMethDecl= decl;
			fResult= result;
		}

		public void updateNode() throws JavaModelException {
			changeParamguments();
			
			if (canChangeNameAndReturnType()) {
				changeMethodName();
				changeReturnType();
			}
					
			if (needsVisibilityUpdate())
				changeVisibility();
			reshuffleElements();
			changeExceptions();
			
			changeJavadocTags();
			
			checkIfDeletedParametersUsed();
		}
	
		protected List getParamguments() {
			return fMethDecl.parameters();
		}

		protected void changeParamgumentName(ParameterInfo info) {
			SingleVariableDeclaration param= (SingleVariableDeclaration) fMethDecl.parameters().get(info.getOldIndex());
			if (! info.getOldName().equals(param.getName().getIdentifier()))
				return; //don't change if original parameter name != name in rippleMethod

			TempOccurrenceAnalyzer analyzer= new TempOccurrenceAnalyzer(param, false);
			analyzer.perform();
			SimpleName[] paramOccurrences= analyzer.getReferenceAndDeclarationNodes(); // @param tags are updated in changeJavaDocTags()
			for (int j= 0; j < paramOccurrences.length; j++) {
				SimpleName occurence= paramOccurrences[j];
				SimpleName newName= occurence.getAST().newSimpleName(info.getNewName());
				fRewrite.replace(occurence, newName, null);
			}
		}
		
		protected void changeParamgumentType(ParameterInfo info) {
			SingleVariableDeclaration oldParam= (SingleVariableDeclaration) fMethDecl.parameters().get(info.getOldIndex());
			replaceTypeNode(oldParam.getType(), info.getNewTypeName());
			removeExtraDimensions(oldParam);
		}

		private void changeReturnType() throws JavaModelException {
		    if (! isReturnTypeSameAsInitial())
		        replaceTypeNode(fMethDecl.getReturnType(), fReturnTypeName);
		    	//TODO: Remove expression from return statement when changed to void?
		    	//      Add return statement with default value and //TODO automatically generated ...
		}
	
		private void removeExtraDimensions(SingleVariableDeclaration oldParam) {
			if (oldParam.getExtraDimensions() != 0) {		
				fRewrite.set(oldParam, SingleVariableDeclaration.EXTRA_DIMENSIONS_PROPERTY, new Integer(0), null);
			}
		}
	
		private boolean needsVisibilityUpdate() throws JavaModelException {
			if (isVisibilitySameAsInitial())
				return false;
			if (isIncreasingVisibility())
				return JdtFlags.isHigherVisibility(fVisibility, JdtFlags.getVisibilityCode(fMethDecl));
			else
				return JdtFlags.isHigherVisibility(JdtFlags.getVisibilityCode(fMethDecl), fVisibility);
		}
		
		private boolean isIncreasingVisibility() throws JavaModelException{
			return JdtFlags.isHigherVisibility(fVisibility, JdtFlags.getVisibilityCode(fMethod));
		}
		
		private void changeVisibility() {
			int newModifiers= JdtFlags.clearAccessModifiers(fMethDecl.getModifiers()) | fVisibility;
			fRewrite.set(fMethDecl, MethodDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
		}
	
		private void changeExceptions() {
			for (Iterator iter= fExceptionInfos.iterator(); iter.hasNext();) {
				ExceptionInfo info= (ExceptionInfo) iter.next();
				if (info.isOld())
					continue;
				if (info.isDeleted())
					removeExceptionFromNodeList(info, fMethDecl.thrownExceptions());
				else
					addExceptionToNodeList(info, fMethDecl.thrownExceptions());
			}
		}
		
		private void removeExceptionFromNodeList(ExceptionInfo toRemove, List exceptionsNodeList) {
			ITypeBinding typeToRemove= toRemove.getTypeBinding();
			for (Iterator iter= exceptionsNodeList.iterator(); iter.hasNext(); ) {
				Name currentName= (Name) iter.next();
				ITypeBinding currentType= currentName.resolveTypeBinding();
				/* Maybe remove all subclasses of typeToRemove too.
				 * Problem:
				 * - B extends A;
				 * - A.m() throws IOException, Exception;
				 * - B.m() throws IOException, AWTException;
				 * Removing Exception should remove AWTException,
				 * but NOT remove IOException (or a subclass of JavaModelException). */
				 // if (Bindings.isSuperType(typeToRemove, currentType))
				if (currentType == null)
					continue; // newly added or unresolvable type
				if (Bindings.equals(typeToRemove, currentType))
					fRewrite.remove(currentName, null);
			}
		}
	
		private void addExceptionToNodeList(ExceptionInfo exceptionInfo, List exceptionsNodeList) {
			String fullyQualified= JavaModelUtil.getFullyQualifiedName(exceptionInfo.getType());
			for (Iterator iter= exceptionsNodeList.iterator(); iter.hasNext(); ) {
				Name exName= (Name) iter.next();
				//XXX: existing superclasses of the added exception are redundant and could be removed
				ITypeBinding typeBinding= exName.resolveTypeBinding();
				if (typeBinding == null)
					continue; // newly added or unresolvable type
				if (typeBinding.getQualifiedName().equals(fullyQualified))
					return; // don't add it again
			}
			String simple= fImportRewrite.addImport(JavaModelUtil.getFullyQualifiedName(exceptionInfo.getType()));
			ASTNode exNode= fRewrite.createStringPlaceholder(simple, ASTNode.SIMPLE_NAME);
			exceptionsNodeList.add(exNode);
			fRewrite.markAsInserted(exNode);
		}
		
		private void changeJavadocTags() throws JavaModelException {
			//update tags in javadoc: @param, @return, @exception, @throws, ...
			Javadoc javadoc= fMethDecl.getJavadoc();
			if (javadoc == null)
				return;

			ITypeBinding typeBinding= Bindings.getBindingOfParentType(fMethDecl);
			if (typeBinding == null)
				return;
			IMethodBinding methodBinding= fMethDecl.resolveBinding();
			if (methodBinding == null)
				return;
				
			boolean isTopOfRipple= null == Bindings.findDeclarationInHierarchy(typeBinding, methodBinding.getName(), methodBinding.getParameterTypes());
			//add tags: only iff top of ripple; change and remove: always.
			//TODO: should have preference for adding tags in (overriding) methods (with template: todo, inheritDoc, ...)
			
			List tags= javadoc.tags(); // List of TagElement
			ListRewrite tagsRewrite= fRewrite.getListRewrite(javadoc, Javadoc.TAGS_PROPERTY);

			if (! isReturnTypeSameAsInitial()) {
				if (PrimitiveType.VOID.toString().equals(fReturnTypeName)) {
					for (int i = 0; i < tags.size(); i++) {
						TagElement tag= (TagElement) tags.get(i);
						if (TagElement.TAG_RETURN.equals(tag.getTagName()))
							fRewrite.remove(tag, null);
					}
				} else if (isTopOfRipple && Signature.SIG_VOID.equals(fMethod.getReturnType())){
					TagElement returnNode= createReturnTag();
					TagElement previousTag= findTagElementToInsertAfter(tags, TagElement.TAG_RETURN);
					insertTag(returnNode, previousTag, tagsRewrite);
					tags= tagsRewrite.getRewrittenList();
				}
			}
			
			if (! (areNamesSameAsInitial() && isOrderSameAsInitial())) {
				ArrayList paramTags= new ArrayList(); // <TagElement>, only not deleted tags with simpleName
				// delete & rename:
				for (Iterator iter = tags.iterator(); iter.hasNext(); ) {
					TagElement tag = (TagElement) iter.next();
					String tagName= tag.getTagName();
					List fragments= tag.fragments();
					if (! (TagElement.TAG_PARAM.equals(tagName) && fragments.size() > 0 && fragments.get(0) instanceof SimpleName))
						continue;
					SimpleName simpleName= (SimpleName) fragments.get(0);
					String identifier= simpleName.getIdentifier();
					boolean removed= false;
					for (int i= 0; i < fParameterInfos.size(); i++) {
						ParameterInfo info= (ParameterInfo) fParameterInfos.get(i);
						if (identifier.equals(info.getOldName())) {
							if (info.isDeleted()) {
								fRewrite.remove(tag, null);
								removed= true;
							} else if (info.isRenamed()) {
								SimpleName newName= simpleName.getAST().newSimpleName(info.getNewName());
								fRewrite.replace(simpleName, newName, null);
							}
							break;
						}
					}
					if (! removed)
						paramTags.add(tag);
				}
				tags= tagsRewrite.getRewrittenList();

				if (! isOrderSameAsInitial()) {
					// reshuffle (sort in declaration sequence) & add (only add to top of ripple):
					TagElement previousTag= findTagElementToInsertAfter(tags, TagElement.TAG_PARAM);
					// reshuffle:
					for (Iterator infoIter= fParameterInfos.iterator(); infoIter.hasNext();) {
						ParameterInfo info= (ParameterInfo) infoIter.next();
						String oldName= info.getOldName();
						String newName= info.getNewName();
						if (info.isAdded()) {
							if (! isTopOfRipple)
								continue;
							TagElement paramNode= createParamTag(newName);
							insertTag(paramNode, previousTag, tagsRewrite);
							previousTag= paramNode;
						} else {
							for (Iterator tagIter= paramTags.iterator(); tagIter.hasNext();) {
								TagElement tag= (TagElement) tagIter.next();
								SimpleName tagName= (SimpleName) tag.fragments().get(0);
								if (oldName.equals(tagName.getIdentifier())) {
									tagIter.remove();
									TagElement movedTag= (TagElement) fRewrite.createMoveTarget(tag);
									insertTag(movedTag, previousTag, tagsRewrite);
									previousTag= movedTag;
								}
							}
						}
					}
					// params with bad names:
					for (Iterator iter= paramTags.iterator(); iter.hasNext();) {
						TagElement tag= (TagElement) iter.next();
						TagElement movedTag= (TagElement) fRewrite.createMoveTarget(tag);
						insertTag(movedTag, previousTag, tagsRewrite);
						previousTag= movedTag;
					}
				}
				tags= tagsRewrite.getRewrittenList();
			}
			
			if (! areExceptionsSameAsInitial()) {
				// collect exceptionTags and remove deleted:
				ArrayList exceptionTags= new ArrayList(); // <TagElement>, only not deleted tags with name
				for (int i= 0; i < tags.size(); i++) {
					TagElement tag= (TagElement) tags.get(i);
					if (! TagElement.TAG_THROWS.equals(tag.getTagName()) && ! TagElement.TAG_EXCEPTION.equals(tag.getTagName()))
						continue;
					if (! (tag.fragments().size() > 0 && tag.fragments().get(0) instanceof Name))
						continue;
					Name name= (Name) tag.fragments().get(0);
					for (int j= 0; j < fExceptionInfos.size(); j++) {
						ExceptionInfo info= (ExceptionInfo) fExceptionInfos.get(j);
						if (info.isDeleted() && Bindings.equals(info.getTypeBinding(), name.resolveTypeBinding())) {
							fRewrite.remove(tag, null);
							break;
						}
						exceptionTags.add(tag);
						break;
					}
				}
				// reshuffle:
				tags= tagsRewrite.getRewrittenList();
				TagElement previousTag= findTagElementToInsertAfter(tags, TagElement.TAG_THROWS);
				for (Iterator infoIter= fExceptionInfos.iterator(); infoIter.hasNext();) {
					ExceptionInfo info= (ExceptionInfo) infoIter.next();
					if (info.isAdded()) {
						if (! isTopOfRipple)
							continue;
						TagElement excptNode= createExceptionTag(info.getType().getElementName());
						insertTag(excptNode, previousTag, tagsRewrite);
						previousTag= excptNode;
					} else {
						for (Iterator tagIter= exceptionTags.iterator(); tagIter.hasNext();) {
							TagElement tag= (TagElement) tagIter.next();
							Name tagName= (Name) tag.fragments().get(0);
							if (Bindings.equals(info.getTypeBinding(), tagName.resolveTypeBinding())) {
								tagIter.remove();
								TagElement movedTag= (TagElement) fRewrite.createMoveTarget(tag);
								insertTag(movedTag, previousTag, tagsRewrite);
								previousTag= movedTag;
							}
						}
					}
				}
				// exceptions with bad names:
				for (Iterator iter= exceptionTags.iterator(); iter.hasNext();) {
					TagElement tag= (TagElement) iter.next();
					TagElement movedTag= (TagElement) fRewrite.createMoveTarget(tag);
					insertTag(movedTag, previousTag, tagsRewrite);
					previousTag= movedTag;
				}
			}
		}

		private TagElement createReturnTag() {
			TagElement returnNode= fRewrite.getAST().newTagElement();
			returnNode.setTagName(TagElement.TAG_RETURN);
			
			TextElement textElement= fRewrite.getAST().newTextElement();
			String text= StubUtility.getTodoTaskTag(getOccurrenceCu().getJavaProject());
			if (text != null)
				textElement.setText(text); //TODO: use template with {@todo} ...
			returnNode.fragments().add(textElement);
			
			return returnNode;
		}

		private TagElement createParamTag(String name) {
			TagElement paramNode= fRewrite.getAST().newTagElement();
			paramNode.setTagName(TagElement.TAG_PARAM);

			SimpleName simpleName= fRewrite.getAST().newSimpleName(name);
			paramNode.fragments().add(simpleName);

			TextElement textElement= fRewrite.getAST().newTextElement();
			String text= StubUtility.getTodoTaskTag(getOccurrenceCu().getJavaProject());
			if (text != null)
				textElement.setText(text); //TODO: use template with {@todo} ...
			paramNode.fragments().add(textElement);
			
			return paramNode;
		}

		private TagElement createExceptionTag(String simpleName) {
			TagElement excptNode= fRewrite.getAST().newTagElement();
			excptNode.setTagName(TagElement.TAG_THROWS);

			SimpleName nameNode= fRewrite.getAST().newSimpleName(simpleName);
			excptNode.fragments().add(nameNode);

			TextElement textElement= fRewrite.getAST().newTextElement();
			String text= StubUtility.getTodoTaskTag(getOccurrenceCu().getJavaProject());
			if (text != null)
				textElement.setText(text); //TODO: use template with {@todo} ...
			excptNode.fragments().add(textElement);
			
			return excptNode;
		}

		private void insertTag(TagElement tag, TagElement previousTag, ListRewrite tagsRewrite) {
			if (previousTag == null)
				tagsRewrite.insertFirst(tag, null);
			else
				tagsRewrite.insertAfter(tag, previousTag, null);
		}

		/**
		 * @return the <code>TagElement<code> just before a new <code>TagElement</code> with name <code>tagName</code>,
		 *   or <code>null</code>.
		 */
		private TagElement findTagElementToInsertAfter(List tags, String tagName) {
			List tagOrder= Arrays.asList(new String[] {
					TagElement.TAG_AUTHOR,
					TagElement.TAG_VERSION,
					TagElement.TAG_PARAM,
					TagElement.TAG_RETURN,
					TagElement.TAG_THROWS,
					TagElement.TAG_EXCEPTION,
					TagElement.TAG_SEE,
					TagElement.TAG_SINCE,
					TagElement.TAG_SERIAL,
					TagElement.TAG_SERIALFIELD,
					TagElement.TAG_SERIALDATA,
					TagElement.TAG_DEPRECATED,
					TagElement.TAG_VALUE
			});
			int goalOrdinal= tagOrder.indexOf(tagName);
			if (goalOrdinal == -1) // unknown tag -> to end
				return (tags.size() == 0) ? null : (TagElement) tags.get(tags.size());
			for (int i= 0; i < tags.size(); i++) {
				int tagOrdinal= tagOrder.indexOf(((TagElement) tags.get(i)).getTagName());
				if (tagOrdinal >= goalOrdinal)
					return (i == 0) ? null : (TagElement) tags.get(i-1);
			}
			return (tags.size() == 0) ? null : (TagElement) tags.get(tags.size()-1);
		}

		//TODO: already reported as compilation error -> don't report there?
		private void checkIfDeletedParametersUsed() {
			String typeName= getFullTypeName(fMethDecl);
			for (Iterator iter= getDeletedInfos().iterator(); iter.hasNext();) {
				ParameterInfo info= (ParameterInfo) iter.next();
				SingleVariableDeclaration paramDecl= (SingleVariableDeclaration) fMethDecl.parameters().get(info.getOldIndex());
				TempOccurrenceAnalyzer analyzer= new TempOccurrenceAnalyzer(paramDecl, false);
				analyzer.perform();
				SimpleName[] paramRefs= analyzer.getReferenceNodes();

				if (paramRefs.length > 0){
					RefactoringStatusContext context= JavaStatusContext.create(getOccurrenceCu(), paramRefs[0]);
					Object[] keys= new String[]{paramDecl.getName().getIdentifier(),
												fMethDecl.getName().getIdentifier(),
												typeName};
					String msg= RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.parameter_used", keys); //$NON-NLS-1$
					fResult.addError(msg, context);
				}
			}	
		}
		
		private String getFullTypeName(MethodDeclaration decl) {
			TypeDeclaration typeDecl= (TypeDeclaration) ASTNodes.getParent(decl, TypeDeclaration.class);
			AnonymousClassDeclaration anonymous= (AnonymousClassDeclaration) ASTNodes.getParent(decl, AnonymousClassDeclaration.class);
			if (anonymous != null && ASTNodes.isParent(typeDecl, anonymous)){
				ClassInstanceCreation cic= (ClassInstanceCreation) ASTNodes.getParent(decl, ClassInstanceCreation.class);
				return RefactoringCoreMessages.getFormattedString("ChangeSignatureRefactoring.anonymous_subclass", new String[]{ASTNodes.asString(cic.getName())}); //$NON-NLS-1$
			} else 
				return typeDecl.getName().getIdentifier();
		}
		
		protected ASTNode createNewParamgument(ParameterInfo info) {
			return createNewSingleVariableDeclaration(info);	
		}
	
		private SingleVariableDeclaration createNewSingleVariableDeclaration(ParameterInfo info) {
			SingleVariableDeclaration newP= fRewrite.getAST().newSingleVariableDeclaration();
			newP.setName(fRewrite.getAST().newSimpleName(info.getNewName()));
			newP.setType(createNewTypeNode(info.getNewTypeName()));
			return newP;
		}
		
		protected SimpleName getMethodNameNode() {
			return fMethDecl.getName();
		}
	
	}

	class DocReferenceUpdate extends OccurrenceUpdate {
		/** instanceof MemberRef || MethodRef */
		private ASTNode fNode;

		protected DocReferenceUpdate(ASTNode node, OldASTRewrite rewrite, ImportRewrite importRewrite, RefactoringStatus result) {
			super(rewrite, importRewrite);
			fNode= node;
		}

		public void updateNode() {
			changeParamguments();
			
			if (canChangeNameAndReturnType())
				changeMethodName();
					
			reshuffleElements();
		}
		
		protected ASTNode createNewParamgument(ParameterInfo info) {
			return createNewMethodRefParameter(info);
		}
		
		private MethodRefParameter createNewMethodRefParameter(ParameterInfo info) {
			MethodRefParameter newP= fRewrite.getAST().newMethodRefParameter();
			
			// only add name iff first parameter already has a name:
			List parameters= getParamguments();
			if (parameters.size() > 0)
				if (((MethodRefParameter) parameters.get(0)).getName() != null)
					newP.setName(fRewrite.getAST().newSimpleName(info.getNewName()));
			
			newP.setType(createNewTypeNode(info.getNewTypeName()));
			return newP;
		}

		protected SimpleName getMethodNameNode() {
			if (fNode instanceof MemberRef)
				return ((MemberRef) fNode).getName();
			
			if (fNode instanceof MethodRef)
				return ((MethodRef) fNode).getName();
			
			return null;	
		}

		protected List getParamguments() {
			if (fNode instanceof MethodRef)
				return ((MethodRef) fNode).parameters();
			
			return new ArrayList(0); // (fNode instanceof MemberRef) or error
		}
		
		protected void changeParamgumentName(ParameterInfo info) {
			if (! (fNode instanceof MethodRef))
				return;

			MethodRefParameter oldParam= (MethodRefParameter) ((MethodRef) fNode).parameters().get(info.getOldIndex());
			SimpleName oldParamName= oldParam.getName();
			if (oldParamName == null)
				return;
			
			SimpleName newName= oldParamName.getAST().newSimpleName(info.getNewName());
			fRewrite.replace(oldParamName, newName, null);
		}
		
		protected void changeParamgumentType(ParameterInfo info) {
			if (! (fNode instanceof MethodRef))
				return;
			
			MethodRefParameter oldParam= (MethodRefParameter) ((MethodRef) fNode).parameters().get(info.getOldIndex());
			replaceTypeNode(oldParam.getType(), info.getNewTypeName());
		}
	}
	
	class NullOccurrenceUpdate extends OccurrenceUpdate {
		private ASTNode fNode;
		private RefactoringStatus fResult;
		protected NullOccurrenceUpdate(ASTNode node, OldASTRewrite rewrite, ImportRewrite importRewrite, RefactoringStatus result) {
			super(rewrite, importRewrite);
			fNode= node;
			fResult= result;
		}
		public void updateNode() throws JavaModelException {
			int start= fNode.getStartPosition();
			int length= fNode.getLength();
			String msg= "Cannot update found node: nodeType=" + fNode.getNodeType() + "; "  //$NON-NLS-1$//$NON-NLS-2$
					+ fNode.toString() + "[" + start + ", " + length + "]";  //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			JavaPlugin.log(new Exception(msg + ":\n" + getOccurrenceCu().getSource().substring(start, start + length))); //$NON-NLS-1$
			fResult.addError(msg, JavaStatusContext.create(getOccurrenceCu(), fNode));
		}
		protected List getParamguments() {
			return new ArrayList(0);
		}
		protected ASTNode createNewParamgument(ParameterInfo info) {
			return null;
		}
		protected SimpleName getMethodNameNode() {
			return null;
		}
	}
}

