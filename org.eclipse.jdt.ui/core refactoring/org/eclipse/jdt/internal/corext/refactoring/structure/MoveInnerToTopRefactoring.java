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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.changes.ValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.ui.CodeGeneration;

public class MoveInnerToTopRefactoring extends Refactoring{
	
	private static final String THIS_KEYWORD= "this"; //$NON-NLS-1$
	private final ImportRewriteManager fImportManager;
	private final CodeGenerationSettings fCodeGenerationSettings;
	private IType fType;
	private TextChangeManager fChangeManager;
	private String fEnclosingInstanceFieldName;
	private boolean fMarkInstanceFieldAsFinal;
	private boolean fCreateInstanceField;
	private String fNewSourceOfInputType;
	private CompilationUnit fDeclaringCuNode;
	private final boolean fIsInstanceFieldCreationPossible;
	private final boolean fIsInstanceFieldCreationMandatory;
	
	private MoveInnerToTopRefactoring(IType type, CodeGenerationSettings codeGenerationSettings) throws JavaModelException{
		Assert.isNotNull(type);
		Assert.isNotNull(codeGenerationSettings);
		fType= type;
		fCodeGenerationSettings= codeGenerationSettings;
		fImportManager= new ImportRewriteManager(codeGenerationSettings);
		fEnclosingInstanceFieldName= getInitialNameForEnclosingInstanceField();
		fMarkInstanceFieldAsFinal= true; //default
		fDeclaringCuNode= AST.parseCompilationUnit(getDeclaringCu(), true);
		fIsInstanceFieldCreationPossible= !JdtFlags.isStatic(type);
		fIsInstanceFieldCreationMandatory= fIsInstanceFieldCreationPossible && isInstanceFieldCreationMandatory();
		fCreateInstanceField= fIsInstanceFieldCreationMandatory;
	}

	public static MoveInnerToTopRefactoring create(IType type, CodeGenerationSettings codeGenerationSettings) throws JavaModelException{
		if (! isAvailable(type))
			return null;
		return new MoveInnerToTopRefactoring(type, codeGenerationSettings);
	}
	
	public static boolean isAvailable(IType type) throws JavaModelException{
		return Checks.isAvailable(type) && ! Checks.isTopLevel(type) && !type.isLocal();
	}
	
	public boolean isInstanceFieldMarkedFinal(){
		return fMarkInstanceFieldAsFinal;
	}
	
	public void setMarkInstanceFieldAsFinal(boolean mark){
		fMarkInstanceFieldAsFinal= mark;
	}

	public boolean isCreatingInstanceFieldPossible(){
		return fIsInstanceFieldCreationPossible;
	}

	public boolean isCreatingInstanceFieldMandatory(){
		return fIsInstanceFieldCreationMandatory;
	}
	
	public boolean getCreateInstanceField(){
		return fCreateInstanceField;
	}
	
	public void setCreateInstanceField(boolean create){
		Assert.isTrue(fIsInstanceFieldCreationPossible);
		Assert.isTrue(! fIsInstanceFieldCreationMandatory);
		fCreateInstanceField= create;
	}
	
	private String getInitialNameForEnclosingInstanceField() {
		IType enclosingType= getEnclosingType();
		if (enclosingType == null)
			return ""; //$NON-NLS-1$
		String qualifiedTypeName= getTypeOfEnclosingInstanceField();
		String packageName = enclosingType.getPackageFragment().getElementName();
		String[] suggestedNames= NamingConventions.suggestFieldNames(enclosingType.getJavaProject(), packageName, qualifiedTypeName, 0, getEnclosingInstanceAccessModifiers(), getFieldNames(fType));
		if (suggestedNames.length > 0)
			return suggestedNames[0];
		String name= enclosingType.getElementName();
		if (name.equals("")) //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}
	
	private static String[] getFieldNames(IType type) {
		try {
			IField[] fields = type.getFields();
			List result= new ArrayList(fields.length);
			for (int i = 0; i < fields.length; i++) {
				result.add(fields[i].getElementName());
			}
			return (String[]) result.toArray(new String[result.size()]);
		} catch (JavaModelException e) {
			return null;
		}
	}

	public IType getInputType(){
		return fType;
	}
	
	private IType getEnclosingType(){
		return fType.getDeclaringType();
	}
	
	public String getEnclosingInstanceName(){
		return fEnclosingInstanceFieldName;
	}
	
	public RefactoringStatus checkEnclosingInstanceName(String name){
		if (! fCreateInstanceField)
			return new RefactoringStatus();
		RefactoringStatus result= Checks.checkFieldName(name);
		if (! Checks.startsWithLowerCase(name))
			result.addWarning(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.names_start_lowercase"));  //$NON-NLS-1$
			
		if (fType.getField(name).exists()){
			Object[] keys= new String[]{name, fType.getElementName()};
			String msg= RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.already_declared", keys); //$NON-NLS-1$
			result.addError(msg, JavaStatusContext.create(fType.getField(name)));
		}	
		return result;	
	}
	
	public void setEnclosingInstanceName(String name){
		Assert.isNotNull(name);
		fEnclosingInstanceFieldName= name;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws CoreException {
		IType orig= (IType)WorkingCopyUtil.getOriginal(fType);
		if (orig == null || ! orig.exists()){
			
			String message= RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.deleted", new String[]{getInputTypeCu().getElementName()}); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fType= orig;
		
		return Checks.checkIfCuBroken(fType);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2);//$NON-NLS-1$
		try {
			RefactoringStatus result= new RefactoringStatus();		

			if (isInputTypeStatic())
				result.merge(checkEnclosingInstanceName(fEnclosingInstanceFieldName));
			
			if (getInputTypePackage().getCompilationUnit(getNameForNewCu()).exists()){
				String message= RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.compilation_Unit_exists", new String[]{getNameForNewCu(), getInputTypePackage().getElementName()}); //$NON-NLS-1$
				result.addFatalError(message);
			}	
			result.merge(checkEnclosingInstanceName(fEnclosingInstanceFieldName));
			result.merge(Checks.checkCompilationUnitName(getNameForNewCu()));
			result.merge(checkConstructorParameterNames());
			result.merge(checkTypeNameInPackage());
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} finally {
			pm.done();
		}	
	}

	private RefactoringStatus checkTypeNameInPackage() throws JavaModelException {
		IType type= Checks.findTypeInPackage(getInputTypePackage(), fType.getElementName());
		if (type == null || ! type.exists())
			return null;
		String message= RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.type_exists", new String[]{fType.getElementName(), getInputTypePackage().getElementName()}); //$NON-NLS-1$
		return RefactoringStatus.createErrorStatus(message);
	}

	private RefactoringStatus checkConstructorParameterNames(){
		RefactoringStatus result= new RefactoringStatus();
		CompilationUnit cuNode= AST.parseCompilationUnit(getInputTypeCu(), false);
		TypeDeclaration type= findTypeDeclaration(fType, cuNode);
		MethodDeclaration[] nodes= getConstructorDeclarationNodes(type);
		for (int i= 0; i < nodes.length; i++) {
			MethodDeclaration constructor= nodes[i];
			for (Iterator iter= constructor.parameters().iterator(); iter.hasNext();) {
				SingleVariableDeclaration param= (SingleVariableDeclaration) iter.next();
				if (fEnclosingInstanceFieldName.equals(param.getName().getIdentifier())){
					String msg= RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.name_used", new String[]{param.getName().getIdentifier(), fType.getElementName()}); //$NON-NLS-1$
					result.addError(msg, JavaStatusContext.create(getInputTypeCu(), param));
				}
			}
		}
		return result;
	}
	
	private boolean isInputTypeStatic() throws JavaModelException {
		return JdtFlags.isStatic(fType);
	}

	private IPackageFragment getInputTypePackage() {
		return fType.getPackageFragment();
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.name"); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.creating_preview"), 1); //$NON-NLS-1$
			final ValidationStateChange result= new ValidationStateChange(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.move_to_Top")); //$NON-NLS-1$
			result.addAll(fChangeManager.getAllChanges());
			result.add(createCompilationUnitForMovedType(new SubProgressMonitor(pm, 1)));
			return result;
		} finally {
			fImportManager.clear();	
		}
	}
	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		pm.beginTask(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.29"), 2); //$NON-NLS-1$
		TextChangeManager manager= new TextChangeManager();
		Map typeReferences= createTypeReferencesMapping(new SubProgressMonitor(pm, 1));	//Map<ICompilationUnit, SearchResult[]>
		Map constructorReferences;	//Map<ICompilationUnit, SearchResult[]>
		if (isInputTypeStatic()){
			constructorReferences= new HashMap(0);
			pm.worked(1);
		} else {
			constructorReferences= createConstructorReferencesMapping(new SubProgressMonitor(pm, 1));
		}
		ICompilationUnit declaringCu= getDeclaringCu();
		for (Iterator iter= getMergedSet(typeReferences.keySet(), constructorReferences.keySet()).iterator(); iter.hasNext();) {
			ICompilationUnit processedCu= (ICompilationUnit) iter.next();
			ASTRewrite rewrite= createRewrite(typeReferences, constructorReferences, declaringCu, processedCu, false);
			if(processedCu.equals(declaringCu)) {	
				fNewSourceOfInputType= getNewSourceForInputType(processedCu, rewrite);	
				rewrite.removeModifications();
				rewrite= createRewrite(typeReferences, constructorReferences, declaringCu, processedCu, true);
			}
			addTextEditFromRewrite(manager, processedCu, rewrite);
		}
		pm.done();
		return manager;
	}
	
	private ICompilationUnit getDeclaringCu() {
		return WorkingCopyUtil.getWorkingCopyIfExists(fType.getCompilationUnit());
	}

	private String getNewSourceForInputType(ICompilationUnit processedCu, ASTRewrite rewrite) throws CoreException, JavaModelException {
		TextChange ch= new CompilationUnitChange("", processedCu); //$NON-NLS-1$
		TextEdit edit= getRewriteTextEdit(processedCu, rewrite);
		TextChangeCompatibility.addTextEdit(ch, "", edit);
		String newSource= ch.getPreviewContent();
		CompilationUnit cuNode= AST.parseCompilationUnit(newSource.toCharArray());
		TypeDeclaration td= findTypeDeclaration(fType, cuNode);
		return newSource.substring(td.getStartPosition(), ASTNodes.getExclusiveEnd(td));
	}

	private ASTRewrite createRewrite(Map typeReferences, Map constructorReferences, ICompilationUnit declaringCu, ICompilationUnit processedCu, boolean removeTypeDeclaration) throws CoreException {
		CompilationUnit cuNode= getAST(processedCu);
		ASTRewrite rewrite= new ASTRewrite(cuNode);
		if (processedCu.equals(declaringCu)){
			TypeDeclaration td= findTypeDeclaration(fType, cuNode);
			if (! removeTypeDeclaration && ! isInputTypeStatic() && fCreateInstanceField) {
				if (typeHasNoConstructors())
					createConstructor(td, rewrite);
				else
					modifyConstructors(td, rewrite);
				
				addEnclosingInstanceDeclaration(td, rewrite);
			}
			modifyAccessesToMembersFromEnclosingInstance(td, rewrite);
			if (removeTypeDeclaration)
				rewrite.markAsRemoved(td, null);
			else 
				removeUnusedTypeModifiers(td, rewrite);
		}
		ASTNode[] typeRefs= getReferenceNodesIn(cuNode, typeReferences, processedCu);
		for (int i= 0; i < typeRefs.length; i++) {
			updateTypeReference(typeRefs[i], rewrite, processedCu);
		}
		ASTNode[] constructorsRefs= getReferenceNodesIn(cuNode, constructorReferences, processedCu);
		for (int i= 0; i < constructorsRefs.length; i++) {
			updateConstructorReference(constructorsRefs[i], rewrite, processedCu);
		}
		return rewrite;
	}
	
	private CompilationUnit getAST(ICompilationUnit processedCu) {		
		if (processedCu.equals(getDeclaringCu()))
			return fDeclaringCuNode;
		return AST.parseCompilationUnit(processedCu, true);
	}

	private boolean typeHasNoConstructors() throws JavaModelException {
		return JavaElementUtil.getAllConstructors(fType).length == 0;
	}
	
	private boolean isInstanceFieldCreationMandatory() {
		TypeDeclaration td= findTypeDeclaration(fType, fDeclaringCuNode);
		MemberAccessNodeCollector collector= new MemberAccessNodeCollector(getEnclosingType());
		td.accept(collector);
		return containsNonStatic(collector.getFieldAccesses()) || containsNonStatic(collector.getMethodInvocations()) || containsNonStatic(collector.getSimpleFieldNames());
	}

	private static boolean containsNonStatic(SimpleName[] fieldNames) {
		for (int i= 0; i < fieldNames.length; i++) {
			if (! isStaticFieldName(fieldNames[i]))
				return true;
		}
		return false;
	}

	private static boolean containsNonStatic(MethodInvocation[] invocations) {
		for (int i= 0; i < invocations.length; i++) {
			if (! isStatic(invocations[i]))
				return true;
		}
		return false;
	}

	private static boolean containsNonStatic(FieldAccess[] accesses) {
		for (int i= 0; i < accesses.length; i++) {
			if (! isStatic(accesses[i]))
				return true;
		}
		return false;
	}

	private static boolean isStatic(MethodInvocation invocation) {
		IMethodBinding methodBinding= invocation.resolveMethodBinding();
		if (methodBinding == null)
			return false;
		return JdtFlags.isStatic(methodBinding);
	}

	private static boolean isStatic(FieldAccess access) {
		IVariableBinding fieldBinding= access.resolveFieldBinding();
		if (fieldBinding == null)
			return false;
		return JdtFlags.isStatic(fieldBinding);
	}

	private static boolean isStaticFieldName(SimpleName name) {
		IBinding binding= name.resolveBinding();
		if (! (binding instanceof IVariableBinding))
			return false;
		IVariableBinding variableBinding= (IVariableBinding)binding;
		if (! variableBinding.isField())
			return false;
		return JdtFlags.isStatic(variableBinding);			
	}

	private static TypeDeclaration findTypeDeclaration(IType type, CompilationUnit cuNode) {
		List types= getDeclaringTypes(type);
		types.add(type);
		TypeDeclaration[] declarations= (TypeDeclaration[]) cuNode.types().toArray(new TypeDeclaration[cuNode.types().size()]);
		TypeDeclaration td= null;
		for (Iterator iter= types.iterator(); iter.hasNext();) {
			IType enclosing= (IType) iter.next();
			td= findTypeDeclaration(enclosing, declarations);
			Assert.isNotNull(td);
			declarations= td.getTypes();
		}
		Assert.isNotNull(td);
		return td;
	}

	private static TypeDeclaration findTypeDeclaration(IType enclosing, TypeDeclaration[] declarations) {
		String typeName= enclosing.getElementName();
		for (int i= 0; i < declarations.length; i++) {
			TypeDeclaration declaration= declarations[i];
			if (declaration.getName().getIdentifier().equals(typeName))
				return declaration;
		}
		return null;
	}

	//List of ITypes
	private static List getDeclaringTypes(IType type) {
		IType declaringType= type.getDeclaringType();
		if (declaringType == null)
			return new ArrayList(0);
		List result= getDeclaringTypes(declaringType);
		result.add(declaringType);
		return result;
	}
	
	private static Set getMergedSet(Set s1, Set s2){
		Set result= new HashSet();
		result.addAll(s1);
		result.addAll(s2);
		return result;
	}
	
	//Map<ICompilationUnit, SearchResult[]>
	private Map createTypeReferencesMapping(IProgressMonitor pm) throws JavaModelException {
		ISearchPattern pattern= SearchEngine.createSearchPattern(fType, IJavaSearchConstants.ALL_OCCURRENCES);
		IJavaSearchScope scope= RefactoringScopeFactory.create(fType);
		SearchResultGroup[] groups= RefactoringSearchEngine.search(pm, scope, pattern);
		return createSearchResultMapping(groups);
	}

	//Map<ICompilationUnit, SearchResult[]>
	private Map createConstructorReferencesMapping(IProgressMonitor pm) throws JavaModelException {
		SearchResultGroup[] groups= ConstructorReferenceFinder.getConstructorReferences(fType, pm);
		return createSearchResultMapping(groups);
	}

	//Map<ICompilationUnit, SearchResult[]>
	private static Map createSearchResultMapping(SearchResultGroup[] groups){
		Map result= new HashMap();
		for (int i= 0; i < groups.length; i++) {
			SearchResultGroup group= groups[i];
			ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(group.getCompilationUnit());
			if (cu == null)
				continue;
			result.put(cu, group.getSearchResults());
		}
		return result;
	}

	private static ASTNode[] getReferenceNodesIn(CompilationUnit cuNode, Map references, ICompilationUnit cu){
		SearchResult[] results= (SearchResult[])references.get(cu);
		if (results == null)
			return new ASTNode[0];
		return ASTNodeSearchUtil.getAstNodes(results, cuNode);
	}

	private void addTextEditFromRewrite(TextChangeManager manager, ICompilationUnit cu, ASTRewrite rewrite) throws CoreException {
		TextChange textChange= manager.get(cu);
		TextEdit resultingEdit= getRewriteTextEdit(cu, rewrite);
		TextChangeCompatibility.addTextEdit(textChange, RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.30"), resultingEdit);
		rewrite.removeModifications();
	}

	private TextEdit getRewriteTextEdit(ICompilationUnit cu, ASTRewrite rewrite) throws CoreException {
		TextBuffer textBuffer= TextBuffer.create(cu.getBuffer().getContents());
		TextEdit resultingEdit= new MultiTextEdit();
		rewrite.rewriteNode(textBuffer, resultingEdit);
		if (fImportManager.hasImportEditFor(cu)) {
			ImportRewrite importRewrite= fImportManager.getImportRewrite(cu);
			resultingEdit.addChild(importRewrite.createEdit(textBuffer));
		}
		return resultingEdit;
	}

	private void modifyAccessesToMembersFromEnclosingInstance(TypeDeclaration typeDeclaration, ASTRewrite rewrite) {
		MemberAccessNodeCollector collector= new MemberAccessNodeCollector(getEnclosingType());
		typeDeclaration.accept(collector);
		modifyAccessToMethodsFromEnclosingInstance(rewrite, collector.getMethodInvocations(), typeDeclaration);
		modifyAccessToFieldsFromEnclosingInstance(rewrite, collector.getFieldAccesses(), typeDeclaration);
		modifyAccessToFieldsFromEnclosingInstance(rewrite, collector.getSimpleFieldNames(), typeDeclaration);
	}
	
	private void modifyAccessToFieldsFromEnclosingInstance(ASTRewrite rewrite, SimpleName[] simpleNames, TypeDeclaration inputType) {
		for (int i= 0; i < simpleNames.length; i++) {
			SimpleName simpleName= simpleNames[i];
			IBinding vb= simpleName.resolveBinding();
			if (vb == null)
				continue;
			Expression newExpression= createAccessExpressionToEnclosingInstanceFieldText(simpleName, vb, inputType);
			FieldAccess access= simpleName.getAST().newFieldAccess();
			access.setExpression(newExpression);
			access.setName(simpleName.getAST().newSimpleName(simpleName.getIdentifier()));
			rewrite.markAsReplaced(simpleName, access, null);
		}
	}

	private void modifyAccessToFieldsFromEnclosingInstance(ASTRewrite rewrite, FieldAccess[] fieldAccesses, TypeDeclaration inputType) {
		for (int i= 0; i < fieldAccesses.length; i++) {
			FieldAccess fieldAccess= fieldAccesses[i];
			Assert.isNotNull(fieldAccess.getExpression());
			if (! (fieldAccess.getExpression() instanceof ThisExpression) ||  (! (((ThisExpression)fieldAccess.getExpression()).getQualifier() != null)))
				continue;
					
			IVariableBinding vb= resolveFieldBinding(fieldAccess);
			if (vb == null)
				continue;
			Expression newExpression= createAccessExpressionToEnclosingInstanceFieldText(fieldAccess, vb, inputType);
			rewrite.markAsReplaced(fieldAccess.getExpression(), newExpression, null);
		}
	}

	private void modifyAccessToMethodsFromEnclosingInstance(ASTRewrite rewrite, MethodInvocation[] methodInvocations, TypeDeclaration inputType) {
		for (int i= 0; i < methodInvocations.length; i++) {
			MethodInvocation methodInvocation= methodInvocations[i];
			IMethodBinding mb= methodInvocation.resolveMethodBinding();
			if (mb == null)
				continue;
			Expression invocExpression= methodInvocation.getExpression();
			if (invocExpression == null){
				Expression newExpression= createAccessExpressionToEnclosingInstanceFieldText(methodInvocation, mb, inputType);
				methodInvocation.setExpression(newExpression);
				rewrite.markAsInserted(newExpression);
			} else {
				if (! (methodInvocation.getExpression() instanceof ThisExpression) || !(((ThisExpression)methodInvocation.getExpression()).getQualifier() != null))
					continue;
				Expression newExpression= createAccessExpressionToEnclosingInstanceFieldText(methodInvocation, mb, inputType);
				rewrite.markAsReplaced(invocExpression, newExpression, null);
			}
		}
	}	

	private static boolean isStatic(IBinding binding){
		return Modifier.isStatic(binding.getModifiers());
	}

	private Expression createAccessExpressionToEnclosingInstanceFieldText(ASTNode node, IBinding binding, TypeDeclaration inputType) {
		if (isStatic(binding))
			return getNameOfTypeOfEnclosingInstanceField(node.getAST());
		else if (isInTypeNestedInInputType(node, inputType))
			return createQualifiedReadAccessExpressionForEnclosingInstance(node.getAST());
		else
			return createReadAccessExpressionForEnclosingInstance(node.getAST());
	}

	private boolean isInTypeNestedInInputType(ASTNode node, TypeDeclaration inputType){
		return (isInAnonymousTypeInsideInputType(node, inputType) ||
				isInLocalTypeInsideInputType(node, inputType) ||
				isInNonStaticMemberTypeInsideInputType(node, inputType));
	}
	

	private boolean isInLocalTypeInsideInputType(ASTNode node, TypeDeclaration inputType) {
		TypeDeclarationStatement localType= (TypeDeclarationStatement)ASTNodes.getParent(node, TypeDeclarationStatement.class);
		return localType != null && ASTNodes.isParent(localType, inputType);
	}

	private boolean isInNonStaticMemberTypeInsideInputType(ASTNode node, TypeDeclaration inputType) {
		TypeDeclaration nested= (TypeDeclaration)ASTNodes.getParent(node, TypeDeclaration.class);
		return nested != null && 
				! inputType.equals(nested) && 
				! Modifier.isStatic(nested.getFlags()) &&
				ASTNodes.isParent(nested, inputType);
	}

	private boolean isInAnonymousTypeInsideInputType(ASTNode node, TypeDeclaration inputType) {
		AnonymousClassDeclaration anon= (AnonymousClassDeclaration)ASTNodes.getParent(node, AnonymousClassDeclaration.class);
		return anon != null && ASTNodes.isParent(anon, inputType);
	}

	private void modifyConstructors(TypeDeclaration td, ASTRewrite rewrite) throws CoreException {
		MethodDeclaration[] constructorNodes= getConstructorDeclarationNodes(td);
		for (int i= 0; i < constructorNodes.length; i++) {
			MethodDeclaration decl= constructorNodes[i];
			Assert.isTrue(decl.isConstructor());
			addParameterToConstructor(rewrite, decl);
			setEnclosingInstanceFieldInConstructor(rewrite, decl);
		}
	}

	private void setEnclosingInstanceFieldInConstructor(ASTRewrite rewrite, MethodDeclaration decl) throws JavaModelException {
		Block body= decl.getBody();
		List statements= body.statements();
		AST ast= decl.getAST();
		if (statements.isEmpty()){
			Assignment assignment= ast.newAssignment();
			assignment.setLeftHandSide(createReadAccessExpressionForEnclosingInstance(ast));
			assignment.setRightHandSide(createNameNodeForEnclosingInstanceConstructorParameter(ast));
			ExpressionStatement initialization= ast.newExpressionStatement(assignment);
			statements.add(0, initialization);
			rewrite.markAsInserted(initialization);
		} else {
			Statement first= (Statement)statements.get(0);
			if (first instanceof ConstructorInvocation){
				ConstructorInvocation ci= (ConstructorInvocation)first;
				Expression newArg= ast.newSimpleName(fEnclosingInstanceFieldName);
				ci.arguments().add(0, newArg);
				rewrite.markAsInserted(newArg);
			} else {
				Assignment assignment= ast.newAssignment();
				assignment.setLeftHandSide(createReadAccessExpressionForEnclosingInstance(ast));
				assignment.setRightHandSide(createNameNodeForEnclosingInstanceConstructorParameter(ast));
				ExpressionStatement initialization= ast.newExpressionStatement(assignment);
				statements.add(1, initialization);
				rewrite.markAsInserted(initialization);
			} 
		}
	}

	private void addParameterToConstructor(ASTRewrite rewrite, MethodDeclaration declaration) throws JavaModelException {
		AST ast= declaration.getAST();
		SingleVariableDeclaration param= ast.newSingleVariableDeclaration();
		Type paramType= getTypeOfEnclosingInstanceField(ast);
		SimpleName paramName= ast.newSimpleName(getNameForEnclosingInstanceConstructorParameter());
		param.setType(paramType);
		param.setName(paramName);
		declaration.parameters().add(0, param);
		rewrite.markAsInserted(param);
	}

	private void createConstructor(TypeDeclaration declaration, ASTRewrite rewrite) throws CoreException {
		BodyDeclaration newConst= (BodyDeclaration)rewrite.createPlaceholder(formatConstructorSource(getNewConstructorSource(), 0), ASTRewrite.METHOD_DECLARATION);
		declaration.bodyDeclarations().add(0, newConst);
		rewrite.markAsInserted(newConst);
	}

	private String getNewConstructorSource() throws CoreException {
		String lineDelimiter= getLineSeperator();
		String bodyStatement= createEnclosingInstanceInitialization();
		String constructorBody= CodeGeneration.getMethodBodyContent(fType.getCompilationUnit(), fType.getElementName(), fType.getElementName(), true, bodyStatement, lineDelimiter);
		if (constructorBody == null)
			constructorBody= ""; //$NON-NLS-1$
		return getNewConstructorComment() + fType.getElementName() +
				'(' + createDeclarationForEnclosingInstanceConstructorParameter() + "){" +  //$NON-NLS-1$
				lineDelimiter + constructorBody + lineDelimiter + '}';
	}

	private String getNewConstructorComment() throws CoreException {
		if (! fCodeGenerationSettings.createComments)
			return "";//$NON-NLS-1$
		String comment= CodeGeneration.getMethodComment(getInputTypeCu(), fType.getElementName(), fType.getElementName(), getNewConstructorParameterNames(), new String[0], null, null, getLineSeperator());
		if (comment == null)
			return ""; //$NON-NLS-1$
		return comment + getLineSeperator();
	}

	private String[] getNewConstructorParameterNames() {
		if (! fCreateInstanceField)
			return new String[0];
		return new String[]{getTypeOfEnclosingInstanceField()};
	}

	private void addEnclosingInstanceDeclaration(TypeDeclaration type, ASTRewrite rewrite){
		VariableDeclarationFragment fragment= type.getAST().newVariableDeclarationFragment();
		fragment.setName(type.getAST().newSimpleName(fEnclosingInstanceFieldName));
		FieldDeclaration newField= type.getAST().newFieldDeclaration(fragment);
		newField.setModifiers(getEnclosingInstanceAccessModifiers());
		newField.setType(ASTNodeFactory.newType(type.getAST(), getTypeOfEnclosingInstanceField()));
		type.bodyDeclarations().add(0, newField);
		rewrite.markAsInserted(newField);
	}

	private int getEnclosingInstanceAccessModifiers(){
		if (fMarkInstanceFieldAsFinal)
			return Modifier.PRIVATE | Modifier.FINAL;
		else 
			return Modifier.PRIVATE;
	}

	private void removeUnusedTypeModifiers(TypeDeclaration type, ASTRewrite rewrite) {
		int newModifiers= JdtFlags.clearFlag(Modifier.STATIC | Modifier.PROTECTED | Modifier.PRIVATE, type.getModifiers());
		rewrite.set(type, TypeDeclaration.MODIFIERS_PROPERTY, new Integer(newModifiers), null);
		
	}
	
	private void updateTypeReference(ASTNode node, ASTRewrite rewrite, ICompilationUnit cu) throws CoreException{
		ImportDeclaration enclosingImport= getEnclosingImportDeclaration(node);
		if (enclosingImport != null){
			updateReferenceInImport(enclosingImport, node, cu);
		} else {
			boolean updated= updateReference(node, rewrite);
			if (updated && ! getInputTypePackage().equals(cu.getParent()))
				fImportManager.addImportTo(getNewFullyQualifiedNameOfInputType(), cu);
		}
	}
	
	private void updateReferenceInImport(ImportDeclaration enclosingImport, ASTNode node, ICompilationUnit cu) throws CoreException {
		IBinding importBinding= enclosingImport.resolveBinding();
		if (!(importBinding instanceof ITypeBinding))
			return;
		//TODO see bug 36879
		fImportManager.removeImportTo(getSourceOfImport(enclosingImport, importBinding), cu);
		fImportManager.addImportTo(getSourceForModifiedImport(node, cu), cu);	
	}

	private String getSourceOfImport(ImportDeclaration enclosingImport, IBinding importBinding){
		String fullyQualifiedTypeName= MoveInnerToTopRefactoring.getFullyQualifiedImportName((ITypeBinding)importBinding);
		if (enclosingImport.isOnDemand())
			return fullyQualifiedTypeName +".*"; //$NON-NLS-1$
		else
			return fullyQualifiedTypeName;
	}

	private String getSourceForModifiedImport(ASTNode node, ICompilationUnit cu) throws JavaModelException {
		ImportDeclaration enclosingImport= getEnclosingImportDeclaration(node);
		int start= enclosingImport.getName().getStartPosition();
		int end= ASTNodes.getExclusiveEnd(enclosingImport);
		String rawImportSource= cu.getBuffer().getText(start, end - start);
		String newFullyQualifiedName= new StringBuffer(rawImportSource)
														.replace(0, ASTNodes.getExclusiveEnd(node)-start, getNewFullyQualifiedNameOfInputType())
														.toString();
		return newFullyQualifiedName.substring(0, newFullyQualifiedName.length() - 1);
	}
	
	private static ImportDeclaration getEnclosingImportDeclaration(ASTNode node){
		return (ImportDeclaration)ASTNodes.getParent(node, ImportDeclaration.class);
	}
	
	/*
	 * returns whether reference was updated 
	 */
	private boolean updateReference(ASTNode node, ASTRewrite rewrite) {
		if (node.getNodeType() == ASTNode.QUALIFIED_NAME)
			return updateNameReference((QualifiedName)node, rewrite);
		else if (node.getNodeType() == ASTNode.SIMPLE_TYPE)
			return updateNameReference(((SimpleType)node).getName(), rewrite);
		else
			return false;
	}

	private boolean updateNameReference(Name name, ASTRewrite rewrite) {
		if (name instanceof SimpleName)	
			return false;
		if (isFullyQualifiedName(name)){
			rewrite.markAsReplaced(name, name.getAST().newName(Strings.splitByToken(getNewFullyQualifiedNameOfInputType(), ".")), null); //$NON-NLS-1$
			return true;
		}
		rewrite.markAsReplaced(name, name.getAST().newSimpleName(fType.getElementName()), null);
		return true;
	}

	private boolean isFullyQualifiedName(Name name) {
		return ASTNodes.asString(name).equals(fType.getFullyQualifiedName('.'));
	}

	private String getNewFullyQualifiedNameOfInputType() {
		// TODO: Does not work for inner types, Use fType.getFullyQualifiedName('.') (MA)
		return fType.getPackageFragment().getElementName() + '.' + fType.getElementName();
	}

	private ICompilationUnit getInputTypeCu() {
		return WorkingCopyUtil.getWorkingCopyIfExists(fType.getCompilationUnit());
	}

	private Change createCompilationUnitForMovedType(IProgressMonitor pm) throws CoreException {
		ICompilationUnit newCuWC= null;
		try{
			newCuWC= WorkingCopyUtil.getNewWorkingCopy(getInputTypePackage(), getNameForNewCu());
			String source= createSourceForNewCu(newCuWC, pm);
			return new CreateTextFileChange(createPathForNewCu(), source, "java");	 //$NON-NLS-1$
		} finally{
			if (newCuWC != null)
				newCuWC.destroy();
		}
	}
	
	private String createSourceForNewCu(ICompilationUnit newCu, IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		// TODO: CodeGeneration.getCompilationUnitContent can return null
		newCu.getBuffer().setContents(CodeGeneration.getCompilationUnitContent(newCu, null, createTypeSource().toString(), getLineSeperator()));
		addImportsToNewCu(newCu, new SubProgressMonitor(pm, 1));
		pm.done();
		return newCu.getSource();
	}

	private void addImportsToNewCu(ICompilationUnit newCu, IProgressMonitor pm) throws CoreException, JavaModelException {
		ImportsStructure is= new ImportsStructure(newCu, fCodeGenerationSettings.importOrder, fCodeGenerationSettings.importThreshold, true);
		IType[] typesReferencedInInputType= ReferenceFinderUtil.getTypesReferencedIn(new IJavaElement[]{fType}, pm);
		for (int i= 0; i < typesReferencedInInputType.length; i++) {
			is.addImport(JavaModelUtil.getFullyQualifiedName(typesReferencedInInputType[i]));
		}
		is.create(false, pm);
	}

	private String createTypeSource() {
		return alignSourceBlock(fNewSourceOfInputType);
	}
	
	private String alignSourceBlock(String typeCodeBlock) {
		String[] lines= Strings.convertIntoLines(typeCodeBlock);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		return Strings.concatenate(lines, getLineSeperator());
	}

	private IPath createPathForNewCu() {
		return ResourceUtil.getFile(getInputTypeCu()).getFullPath().removeLastSegments(1).append(getNameForNewCu());
	}

	private String getNameForNewCu() {
		return fType.getElementName() + ".java"; //$NON-NLS-1$
	}

	private String getLineSeperator() {
		try {
			return StubUtility.getLineDelimiterUsed(fType);
		} catch (JavaModelException e) {
			return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private IFile[] getAllFilesToModify(){
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles(){
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}

	private void updateConstructorReference(ASTNode refNode, ASTRewrite rewrite, ICompilationUnit cu) throws CoreException {
		if (refNode instanceof SuperConstructorInvocation)
			updateConstructorReference((SuperConstructorInvocation)refNode, rewrite, cu);
		else if (refNode instanceof ClassInstanceCreation)
			updateConstructorReference((ClassInstanceCreation)refNode, rewrite, cu);
		else if (refNode.getParent() instanceof ClassInstanceCreation)
			updateConstructorReference((ClassInstanceCreation)refNode.getParent(), rewrite, cu);
	}
	
	private void updateConstructorReference(SuperConstructorInvocation sci, ASTRewrite rewrite, ICompilationUnit cu) throws CoreException{
		if (fCreateInstanceField)
			insertExpressionAsParameter(sci, rewrite, cu);
		if (sci.getExpression() != null)
			rewrite.markAsRemoved(sci.getExpression(), null);
	}

	private void updateConstructorReference(ClassInstanceCreation cic, ASTRewrite rewrite, ICompilationUnit cu) throws JavaModelException {
		if (fCreateInstanceField)
			insertExpressionAsParameter(cic, rewrite, cu);
		if (cic.getExpression() != null)
			rewrite.markAsRemoved(cic.getExpression(), null);
	}
	
	private MethodDeclaration[] getConstructorDeclarationNodes(TypeDeclaration declaration){
		MethodDeclaration[] methodDeclarations= declaration.getMethods();
		List result= new ArrayList(2);
		for (int i= 0; i < methodDeclarations.length; i++) {
			MethodDeclaration method= methodDeclarations[i];
			if (method.isConstructor())
				result.add(method);
		}
		return (MethodDeclaration[]) result.toArray(new MethodDeclaration[result.size()]);
	}
	
	private boolean insertExpressionAsParameter(ClassInstanceCreation cic, ASTRewrite rewrite, ICompilationUnit cu) throws JavaModelException{
		return addAsFirstArgument(rewrite, createEnclosingInstanceCreationString(cic, cu), cic.arguments());
	}

	private boolean insertExpressionAsParameter(SuperConstructorInvocation sci, ASTRewrite rewrite, ICompilationUnit cu) throws JavaModelException{
		return addAsFirstArgument(rewrite, createEnclosingInstanceCreationString(sci, cu), sci.arguments());
	}

	private static boolean addAsFirstArgument(ASTRewrite rewrite, String expression, List arguments) {
		if (expression == null)
			return false;
		Expression newArgument= (Expression)rewrite.createPlaceholder(expression, ASTRewrite.EXPRESSION);
		arguments.add(0, newArgument);
		rewrite.markAsInserted(newArgument);
		return true;
	}

	private String createEnclosingInstanceCreationString(ASTNode node, ICompilationUnit cu) throws JavaModelException {
		Assert.isTrue((node instanceof ClassInstanceCreation) || (node instanceof SuperConstructorInvocation));
		Expression expression;
		if (node instanceof ClassInstanceCreation)
			expression= ((ClassInstanceCreation)node).getExpression();
		else
			expression= ((SuperConstructorInvocation)node).getExpression();
			
		if (expression != null)
			return getExpressionString(expression, cu);
		else if (isInputTypeStatic())
			return null;	
		else if (isInsideSubclassOfDeclaringType(node))
			return THIS_KEYWORD;
		else if (isInsideInputType(node))
			return createReadAccessForEnclosingInstance();
		else if (isInsideTypeNestedInDeclaringType(node))
			return getEnclosingType().getElementName() + '.' + THIS_KEYWORD;
		return null;
	}
			
	private String getExpressionString(Expression expression, ICompilationUnit compilationUnit) throws JavaModelException {
		return compilationUnit.getBuffer().getText(expression.getStartPosition(), expression.getLength());
	}
	
	private boolean isInsideSubclassOfDeclaringType(ASTNode node) {
		Assert.isTrue((node instanceof ClassInstanceCreation) || (node instanceof SuperConstructorInvocation));
		TypeDeclaration typeDeclar= getInnerMostTypeDeclaration(node);
		Assert.isNotNull(typeDeclar);
		
		AnonymousClassDeclaration anon= (AnonymousClassDeclaration)ASTNodes.getParent(node, AnonymousClassDeclaration.class);
		boolean isAnonymous= anon != null && ASTNodes.isParent(anon, typeDeclar);
		if (isAnonymous)
			return isSubclassBindingOfEnclosingType(anon.resolveBinding());
		return isSubclassBindingOfEnclosingType(typeDeclar.resolveBinding());
	}
	
	private boolean isSubclassBindingOfEnclosingType(ITypeBinding binding){
		while(binding != null){
			if (isEnclosingTypeBinding(binding))
				return true;
			binding= binding.getSuperclass();	
		}	
		return false;
	}

	private boolean isInsideTypeNestedInDeclaringType(ASTNode node) {
		Assert.isTrue((node instanceof ClassInstanceCreation) || (node instanceof SuperConstructorInvocation));
		TypeDeclaration typeDeclar= getInnerMostTypeDeclaration(node);
		Assert.isNotNull(typeDeclar);
		ITypeBinding enclosing= typeDeclar.resolveBinding();
		while(enclosing != null){
			if (isEnclosingTypeBinding(enclosing))
				return true;
			enclosing= enclosing.getDeclaringClass();	
		}		
		return false;
	}

	private boolean isInsideInputType(ASTNode node) throws JavaModelException {
		Assert.isTrue((node instanceof ClassInstanceCreation) || (node instanceof SuperConstructorInvocation));
		ISourceRange range= fType.getSourceRange();
		return (node.getStartPosition() >= range.getOffset()
					&& ASTNodes.getExclusiveEnd(node) <= range.getOffset() + range.getLength());
	}

	private static TypeDeclaration getInnerMostTypeDeclaration(ASTNode node) {
		return (TypeDeclaration)ASTNodes.getParent(node, TypeDeclaration.class);
	}

	private boolean isEnclosingTypeBinding(ITypeBinding binding) {
		return isCorrespondingTypeBinding(binding, getEnclosingType());
	}
	
	private static boolean isCorrespondingTypeBinding(ITypeBinding binding, IType type) {
		if (binding == null)
			return false;
		return Bindings.getFullyQualifiedName(binding).equals(JavaElementUtil.createSignature(type));
	}	

	private Expression createQualifiedReadAccessExpressionForEnclosingInstance(AST ast) {
		ThisExpression thisE= ast.newThisExpression();
		thisE.setQualifier(ast.newName(new String[]{fType.getElementName()}));
		FieldAccess fa= ast.newFieldAccess();
		fa.setExpression(thisE);
		fa.setName(ast.newSimpleName(fEnclosingInstanceFieldName));
		return fa;
	}

	private Expression createReadAccessExpressionForEnclosingInstance(AST ast) {
		FieldAccess fa= ast.newFieldAccess();
		fa.setExpression(ast.newThisExpression());
		fa.setName(ast.newSimpleName(fEnclosingInstanceFieldName));
		return fa;
	}

	private String createReadAccessForEnclosingInstance() {
		return THIS_KEYWORD + '.' + fEnclosingInstanceFieldName;
	}

	private String createEnclosingInstanceInitialization() throws JavaModelException {
		return createReadAccessForEnclosingInstance() + '=' + getNameForEnclosingInstanceConstructorParameter() + ';';
	}

	private String getNameForEnclosingInstanceConstructorParameter() throws JavaModelException {
		IType enclosingType= getEnclosingType();
		String[] excludedNames= getParameterNamesOfAllConstructors(fType);
		String qualifiedTypeName= getTypeOfEnclosingInstanceField();
		String packageName= enclosingType.getPackageFragment().getElementName();
		String[] suggestedNames= NamingConventions.suggestArgumentNames(enclosingType.getJavaProject(), packageName, qualifiedTypeName, 0, excludedNames);
		if (suggestedNames.length > 0)
			return suggestedNames[0];
		return fEnclosingInstanceFieldName;
	}
	
	private Name createNameNodeForEnclosingInstanceConstructorParameter(AST ast) throws JavaModelException {
		return ast.newSimpleName(getNameForEnclosingInstanceConstructorParameter());
	}

	private static String[] getParameterNamesOfAllConstructors(IType type) throws JavaModelException {
		IMethod[] constructors= JavaElementUtil.getAllConstructors(type);
		Set result= new HashSet();
		for (int i= 0; i < constructors.length; i++) {
			result.addAll(Arrays.asList(constructors[i].getParameterNames()));
		}
		return (String[])result.toArray(new String[result.size()]);
	}

	private String createDeclarationForEnclosingInstanceConstructorParameter() throws JavaModelException {
		return getTypeOfEnclosingInstanceField() + ' ' + getNameForEnclosingInstanceConstructorParameter();
	}
	
	private Type getTypeOfEnclosingInstanceField(AST ast){
		return ast.newSimpleType(getNameOfTypeOfEnclosingInstanceField(ast));
	}

	private Name getNameOfTypeOfEnclosingInstanceField(AST ast){
		return ast.newName(Strings.splitByToken(getTypeOfEnclosingInstanceField(), ".")); //$NON-NLS-1$
	}
	
	private String getTypeOfEnclosingInstanceField() {
		return JavaModelUtil.getTypeQualifiedName(getEnclosingType());
	}
		
	private static ITypeBinding getDeclaringTypeBinding(MethodInvocation methodInvocation){
		IMethodBinding binding= methodInvocation.resolveMethodBinding();
		if (binding == null)
			return null;
		return binding.getDeclaringClass();
	}

	private static ITypeBinding getDeclaringTypeBinding(FieldAccess fieldAccess){
		IVariableBinding varBinding= resolveFieldBinding(fieldAccess);
		if (varBinding == null)
			return null;
		return varBinding.getDeclaringClass();
	}

	private static IVariableBinding resolveFieldBinding(FieldAccess fieldAccess) {
		return fieldAccess.resolveFieldBinding();
	}

	private String formatConstructorSource(String src, int indentationLevel){
		return CodeFormatterUtil.format(CodeFormatter.K_CLASS_BODY_DECLARATIONS, src, indentationLevel, null, getLineSeperator(), fType.getJavaProject());
	}

	private static String getFullyQualifiedImportName(ITypeBinding type) {
		if (type.isArray())
			return Bindings.getFullyQualifiedName(type.getElementType());
		else if (type.isAnonymous())
			return getFullyQualifiedImportName(type.getSuperclass());
		else
			return Bindings.getFullyQualifiedName(type);
	}
	
	private static class MemberAccessNodeCollector extends ASTVisitor{
		private final List fMethodAccesses= new ArrayList(0);
		private final List fFieldAccesses= new ArrayList(0);
		private final List fSimpleNames= new ArrayList(0);
		
		private final IType fType;
		MemberAccessNodeCollector(IType type){
			fType= type;
		}
		MethodInvocation[] getMethodInvocations(){
			return (MethodInvocation[]) fMethodAccesses.toArray(new MethodInvocation[fMethodAccesses.size()]);
		}
		FieldAccess[] getFieldAccesses(){
			return (FieldAccess[]) fFieldAccesses.toArray(new FieldAccess[fFieldAccesses.size()]);
		}
		SimpleName[] getSimpleFieldNames(){
			return (SimpleName[]) fSimpleNames.toArray(new SimpleName[fSimpleNames.size()]);
		}
		
		public boolean visit(MethodInvocation node) {
			ITypeBinding declaringClassBinding= MoveInnerToTopRefactoring.getDeclaringTypeBinding(node);
			if (declaringClassBinding != null && MoveInnerToTopRefactoring.isCorrespondingTypeBinding(declaringClassBinding, fType)) 
				fMethodAccesses.add(node);
			//method invocations can be nested in one another, we have to go on
			return super.visit(node);
		}
		
		public boolean visit(FieldAccess node) {
			ITypeBinding declaringClassBinding= MoveInnerToTopRefactoring.getDeclaringTypeBinding(node);
			if (declaringClassBinding != null && MoveInnerToTopRefactoring.isCorrespondingTypeBinding(declaringClassBinding, fType)) {
				fFieldAccesses.add(node);
				return false;
			}
			return super.visit(node);
		}

		public boolean visit(SimpleName node) {
			if (node.getParent() instanceof QualifiedName)
				return super.visit(node);
			IBinding binding= node.resolveBinding();
			if (binding instanceof IVariableBinding){
				IVariableBinding vb= (IVariableBinding)binding;
				if (vb.isField() && MoveInnerToTopRefactoring.isCorrespondingTypeBinding(vb.getDeclaringClass(), fType)) {
					fSimpleNames.add(node);
					return false;
				}
			}
			return super.visit(node);
		}
	}
}