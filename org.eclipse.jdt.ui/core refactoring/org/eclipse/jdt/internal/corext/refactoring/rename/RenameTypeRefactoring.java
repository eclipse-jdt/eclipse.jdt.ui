/*******************************************************************************
 * Copyright (c) 2000, 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Common Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/cpl-v10.
 * html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.RenameResourceChange;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IQualifiedNameUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ITextUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.QualifiedNameFinder;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class RenameTypeRefactoring extends Refactoring implements IRenameRefactoring, ITextUpdatingRefactoring, IReferenceUpdatingRefactoring, IQualifiedNameUpdatingRefactoring {
	
	private IType fType;
	private String fNewName;
	private SearchResultGroup[] fReferences;
	private TextChangeManager fChangeManager;
	private QualifiedNameFinder fQualifiedNameFinder;
	
	private boolean fUpdateReferences;
	
	private boolean fUpdateJavaDoc;
	private boolean fUpdateComments;
	private boolean fUpdateStrings;
	private boolean fUpdateQualifiedNames;
	private String fFilePatterns;

	public RenameTypeRefactoring(IType type) {
		fType= type;
		fNewName= type.getElementName();
		fUpdateReferences= true; //default is yes
		fUpdateJavaDoc= false;
		fUpdateComments= false;
		fUpdateStrings= false;
	}
	
	public Object getNewElement(){
		IPackageFragment parent= fType.getPackageFragment();
		ICompilationUnit cu;
		if (isPrimaryType())
			cu= parent.getCompilationUnit(fNewName + ".java"); //$NON-NLS-1$
		else
			cu= fType.getCompilationUnit();	
		return cu.getType(fNewName);
	}
	
	/*
	 * @see ITextUpdatingRefactoring#canEnableTextUpdating()
	 */
	public boolean canEnableTextUpdating() {
		return true;
	}
	
	/*
	 * @see ITextUpdatingRefactoring#getUpdateJavaDoc()
	 */
	public boolean getUpdateJavaDoc() {
		return fUpdateJavaDoc;
	}

	/*
	 * @see ITextUpdatingRefactoring#getUpdateComments()
	 */
	public boolean getUpdateComments() {
		return fUpdateComments;
	}

	/*
	 * @see ITextUpdatingRefactoring#getUpdateStrings()
	 */
	public boolean getUpdateStrings() {
		return fUpdateStrings;
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateJavaDoc(boolean)
	 */
	public void setUpdateJavaDoc(boolean update) {
		fUpdateJavaDoc= update;
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateComments(boolean)
	 */
	public void setUpdateComments(boolean update) {
		fUpdateComments= update;
	}

	/*
	 * @see ITextUpdatingRefactoring#setUpdateStrings(boolean)
	 */
	public void setUpdateStrings(boolean update) {
		fUpdateStrings= update;
	}
	
	/* non java-doc
	 * @see Refactoring#checkPreconditions(IProgressMonitor)
	 */
	public RefactoringStatus checkPreconditions(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= checkPreactivation();
		if (result.hasFatalError())
			return result;
		result.merge(super.checkPreconditions(pm));
		return result;
	}
	
	
	/* non java-doc
	 * @see IRenameRefactoring#getNewName
	 */
	public String getNewName(){
		return fNewName;
	}

	/* non java-doc
	 * @see IRenameRefactoring#setNewName
	 */
	public void setNewName(String newName){
		Assert.isNotNull(newName);
		fNewName= newName;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getCurrentName
	 */
	public String getCurrentName(){
		return fType.getElementName();
	}
	
	/* non java-doc
	 * @see IRefactoring#getName
	 */
	public String getName(){
		return RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.name",  //$NON-NLS-1$
														new String[]{JavaModelUtil.getFullyQualifiedName(fType), fNewName});
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#setUpdateReferences(boolean)
	 */	
	public void setUpdateReferences(boolean update){
		fUpdateReferences= update;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#canUpdateReferences()
	 */	
	public boolean canEnableUpdateReferences(){
		return true;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getUpdateReferences()
	 */	
	public boolean getUpdateReferences(){
		return fUpdateReferences;
	}

	/* non java-doc
	 * Method declared in IQualifiedNameUpdatingRefactoring
	 */	
	public boolean canEnableQualifiedNameUpdating() {
		return !fType.getPackageFragment().isDefaultPackage() && !(fType.getParent() instanceof IType);
	}
	
	/* non java-doc
	 * Method declared in IQualifiedNameUpdatingRefactoring
	 */	
	public boolean getUpdateQualifiedNames() {
		return fUpdateQualifiedNames;
	}
	
	/* non java-doc
	 * Method declared in IQualifiedNameUpdatingRefactoring
	 */	
	public void setUpdateQualifiedNames(boolean update) {
		fUpdateQualifiedNames= update;
	}
	
	/* non java-doc
	 * Method declared in IQualifiedNameUpdatingRefactoring
	 */	
	public String getFilePatterns() {
		return fFilePatterns;
	}
	
	/* non java-doc
	 * Method declared in IQualifiedNameUpdatingRefactoring
	 */	
	public void setFilePatterns(String patterns) {
		Assert.isNotNull(patterns);
		fFilePatterns= patterns;
	}
	
	//------------- Conditions -----------------
	
	/* non java-doc
	 * @see IPreactivatedRefactoring#checkPreactivation
	 */
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkAvailability(fType));
		if (isSpecialCase(fType))
			result.addFatalError(RefactoringCoreMessages.getString("RenameTypeRefactoring.special_case"));	 //$NON-NLS-1$
		return result;
	}
	
	/* non java-doc
	 * @see Refactoring#checkActivation
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		IType orig= (IType)WorkingCopyUtil.getOriginal(fType);
		if (orig == null || ! orig.exists()){
			String message= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.does_not_exist", //$NON-NLS-1$
						new String[]{JavaModelUtil.getFullyQualifiedName(fType), fType.getCompilationUnit().getElementName()});
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fType= orig;
		
		return Checks.checkIfCuBroken(fType);
	}

	/* non java-doc
	 * @see IRenameRefactoring#checkNewName
	 */	
	public RefactoringStatus checkNewName(String newName){
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		RefactoringStatus result= Checks.checkTypeName(newName);
		if (Checks.isAlreadyNamed(fType, newName))
			result.addFatalError(RefactoringCoreMessages.getString("RenameTypeRefactoring.choose_another_name"));	 //$NON-NLS-1$
		return result;
	}

	/* non java-doc
	 * @see Refactoring#checkInput
	 */		
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		Assert.isNotNull(fType, "type"); //$NON-NLS-1$
		Assert.isNotNull(fNewName, "newName"); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		try{
			pm.beginTask("", 120); //$NON-NLS-1$
			pm.setTaskName(RefactoringCoreMessages.getString("RenameTypeRefactoring.checking")); //$NON-NLS-1$
			result.merge(checkNewName(fNewName));
			if (result.hasFatalError())
				return result;
			result.merge(Checks.checkIfCuBroken(fType));
			if (result.hasFatalError())
				return result;
			pm.worked(2);
		
			result.merge(checkTypesInCompilationUnit());
			pm.worked(1);
		
			result.merge(checkForMethodsWithConstructorNames());
			pm.worked(1);
		
			result.merge(checkImportedTypes());	
			pm.worked(1);
		
			if (mustRenameCU())
				result.merge(Checks.checkCompilationUnitNewName(fType.getCompilationUnit(), fNewName));
			pm.worked(1);	
			
			if (isPrimaryType())
				result.merge(checkNewPathValidity());
			pm.worked(1);	
			
			result.merge(checkEnclosingTypes());
			pm.worked(1);	
			
			result.merge(checkEnclosedTypes());
			pm.worked(1);	
			
			result.merge(checkTypesInPackage());
			pm.worked(1);	
			
			result.merge(checkTypesImportedInCu());
			pm.worked(1);	
		
			result.merge(Checks.checkForNativeMethods(fType));
			pm.worked(1);	
		
			result.merge(Checks.checkForMainMethod(fType));
			pm.worked(1);	
			
			// before doing any expensive analysis
			if (result.hasFatalError())
				return result;
							
			result.merge(analyseEnclosedTypes());
			pm.worked(1);
			// before doing _the really_ expensive analysis
			if (result.hasFatalError())
				return result;
			
			fReferences= null;
			if (fUpdateReferences){
				pm.setTaskName(RefactoringCoreMessages.getString("RenameTypeRefactoring.searching"));	 //$NON-NLS-1$
				fReferences= getReferences(new SubProgressMonitor(pm, 35));
			} else
				pm.worked(35);

			pm.setTaskName(RefactoringCoreMessages.getString("RenameTypeRefactoring.checking")); //$NON-NLS-1$
			if (pm.isCanceled())
				throw new OperationCanceledException();
			
			if (fUpdateReferences)
				result.merge(analyzeAffectedCompilationUnits(new SubProgressMonitor(pm, 25)));
			else
				pm.worked(25);
			
			if (result.hasFatalError())
				return result;
			
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 35));

			if (fUpdateQualifiedNames)			
				computeQualifiedNameMatches(new SubProgressMonitor(pm, 10));
			else
				pm.worked(10);

			result.merge(validateModifiesFiles());
			return result;
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} finally {
			pm.done();
		}	
	}
	
	private RefactoringStatus checkNewPathValidity() throws JavaModelException{
		IContainer c= ResourceUtil.getResource(fType).getParent();
		
		String notRename= RefactoringCoreMessages.getString("RenameTypeRefactoring.will_not_rename"); //$NON-NLS-1$
		IStatus status= c.getWorkspace().validateName(fNewName, IResource.FILE);
		if (status.getSeverity() == IStatus.ERROR)
			return RefactoringStatus.createWarningStatus(status.getMessage() + ". " + notRename); //$NON-NLS-1$
		
		status= c.getWorkspace().validatePath(createNewPath(fNewName), IResource.FILE);
		if (status.getSeverity() == IStatus.ERROR)
			return RefactoringStatus.createWarningStatus(status.getMessage() + ". " + notRename); //$NON-NLS-1$

		return new RefactoringStatus();
	}
	
	private String createNewPath(String newName) throws JavaModelException{
		return ResourceUtil.getResource(fType).getFullPath().removeLastSegments(1).append(newName).toString();
	}
	
	private RefactoringStatus checkTypesImportedInCu() throws JavaModelException{
		IImportDeclaration imp= getImportedType(fType.getCompilationUnit(), fNewName);
		
		if (imp == null)
			return null;	
			
		String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.imported", //$NON-NLS-1$
											new Object[]{fNewName, ResourceUtil.getResource(fType).getFullPath()});
		IJavaElement grandParent= imp.getParent().getParent();
		if (grandParent instanceof ICompilationUnit)
			return RefactoringStatus.createErrorStatus(msg, JavaSourceContext.create(imp));

		return null;	
	}
	
	private RefactoringStatus checkTypesInPackage() throws JavaModelException{
		IType type= Checks.findTypeInPackage(fType.getPackageFragment(), fNewName);
		if (type == null || ! type.exists())
			return null;
		String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.exists", //$NON-NLS-1$
																	new String[]{fNewName, fType.getPackageFragment().getElementName()});
		return RefactoringStatus.createErrorStatus(msg, JavaSourceContext.create(type));
	}
	
	private RefactoringStatus checkEnclosedTypes() throws JavaModelException{
		IType enclosedType= findEnclosedType(fType, fNewName);
		if (enclosedType == null)
			return null;
		String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.encloses",  //$NON-NLS-1$
																		new String[]{JavaModelUtil.getFullyQualifiedName(fType), fNewName});
		return RefactoringStatus.createErrorStatus(msg, JavaSourceContext.create(enclosedType));
	}
	
	private RefactoringStatus checkEnclosingTypes() throws JavaModelException{
		IType enclosingType= findEnclosingType(fType, fNewName);
		if (enclosingType == null)
			return null;
			
		String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.enclosed",//$NON-NLS-1$
								new String[]{JavaModelUtil.getFullyQualifiedName(fType), fNewName});
		return RefactoringStatus.createErrorStatus(msg, JavaSourceContext.create(enclosingType));
	}
	
	private static IType findEnclosedType(IType type, String newName) throws JavaModelException{
		IType[] enclosedTypes= type.getTypes();
		for (int i= 0; i < enclosedTypes.length; i++){
			if (newName.equals(enclosedTypes[i].getElementName()) || findEnclosedType(enclosedTypes[i], newName) != null)
				return enclosedTypes[i];
		}
		return null;
	}
		
	private static IType findEnclosingType(IType type, String newName) {
		IType enclosing= type.getDeclaringType();
		while (enclosing != null){
			if (newName.equals(enclosing.getElementName()))
				return enclosing;
			else 
				enclosing= enclosing.getDeclaringType();	
		}
		return null;
	}
	
	private static IImportDeclaration getImportedType(ICompilationUnit cu, String typeName) throws JavaModelException{
		IImportDeclaration[] imports= cu.getImports();
		String dotTypeName= "." + typeName; //$NON-NLS-1$
		for (int i= 0; i < imports.length; i++){
			if (imports[i].getElementName().endsWith(dotTypeName))
				return imports[i];
		}
		return null;
	}
	
	private static boolean isSpecialCase(IType type) throws JavaModelException{
		return type.getPackageFragment().getElementName().equals("java.lang");	 //$NON-NLS-1$
	}
	
	private IJavaSearchScope createRefactoringScope() throws JavaModelException{
		return RefactoringScopeFactory.create(fType);
	}
	
	private ISearchPattern createSearchPattern() throws JavaModelException{
		return SearchEngine.createSearchPattern(fType, IJavaSearchConstants.REFERENCES);
	}
	
	private SearchResultGroup[] getReferences(IProgressMonitor pm) throws JavaModelException{
		return RefactoringSearchEngine.search(pm, createRefactoringScope(), createSearchPattern());
	}
	
	private RefactoringStatus checkForMethodsWithConstructorNames()  throws JavaModelException{
		IMethod[] methods= fType.getMethods();
		for (int i= 0; i < methods.length; i++){
			if (methods[i].isConstructor())
				continue;
			RefactoringStatus check= Checks.checkIfConstructorName(methods[i], methods[i].getElementName(), fNewName);	
			if (check != null)
				return check;
		}
		return null;
	}	
	
	private RefactoringStatus checkImportedTypes() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IImportDeclaration[] imports= fType.getCompilationUnit().getImports();	
		for (int i= 0; i < imports.length; i++)
			analyzeImportDeclaration(imports[i], result);
		return result;
	}
	
	private RefactoringStatus checkTypesInCompilationUnit() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		if (! Checks.isTopLevel(fType)){ //the other case checked in checkTypesInPackage
			IType siblingType= fType.getDeclaringType().getType(fNewName);
			if (siblingType.exists()){
				String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.member_type_exists", //$NON-NLS-1$
																		new String[]{fNewName, JavaModelUtil.getFullyQualifiedName(fType.getDeclaringType())});
				result.addError(msg, JavaSourceContext.create(siblingType));
			}
		}
		return result;
	}
	
	private RefactoringStatus analyseEnclosedTypes() throws JavaModelException{
		final ISourceRange typeRange= fType.getSourceRange();
		final RefactoringStatus result= new RefactoringStatus();
		CompilationUnit cuNode= AST.parseCompilationUnit(fType.getCompilationUnit(), false);
		cuNode.accept(new ASTVisitor(){
			public boolean visit(TypeDeclaration node){
				if (node.getStartPosition() <= typeRange.getOffset())
					return true;
				if (node.getStartPosition() > typeRange.getOffset() + typeRange.getLength())
					return true;
		
				if (fNewName.equals(node.getName().getIdentifier())){
					Context	context= JavaSourceContext.create(fType.getCompilationUnit(), node);
					String msg= null;;
					if (node.isLocalTypeDeclaration()){
						msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.local_type", //$NON-NLS-1$
									new String[]{JavaElementUtil.createSignature(fType), fNewName});
					}	
					else if (node.isMemberTypeDeclaration()){
						msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.member_type", //$NON-NLS-1$
								new String[]{JavaElementUtil.createSignature(fType), fNewName});
					}	
					if (msg != null)	
						result.addError(msg, context);
				}
		
				MethodDeclaration[] methods= node.getMethods();
				for (int i= 0; i < methods.length; i++) {
					if (Modifier.isNative(methods[i].getModifiers())){
						Context	context= JavaSourceContext.create(fType.getCompilationUnit(), methods[i]);
						String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.enclosed_type_native", node.getName().getIdentifier());//$NON-NLS-1$
						result.addWarning(msg, context); 
					}	
				}
				return true;
			}
		});
		return result;
	}
	
	private boolean mustRenameCU() throws JavaModelException{
		return Checks.isTopLevel(fType) && (JdtFlags.isPublic(fType));
	}
	
	private static ICompilationUnit getCompilationUnit(IImportDeclaration imp){
		return (ICompilationUnit)imp.getParent().getParent();
	}
	
	private static String getSimpleName(IImportDeclaration imp){
		String name= imp.getElementName();
		if (! imp.isOnDemand())
			return name;
		else
			return name.substring(0, name.length() - 2); //remove the '.*' suffix
	}
	
	private void analyzeImportedTypes(IType[] types, RefactoringStatus result, IImportDeclaration imp) throws JavaModelException{
		for (int i= 0; i < types.length; i++) {
			//could this be a problem (same package imports)?
			if (JdtFlags.isPublic(types[i]) && types[i].getElementName().equals(fNewName)){
				String msg= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.name_conflict1", //$NON-NLS-1$
																			new Object[]{JavaModelUtil.getFullyQualifiedName(types[i]), getFullPath(getCompilationUnit(imp))});
				result.addError(msg, JavaSourceContext.create(imp));
			}
		}
	}
	
	private static IJavaElement convertFromImportDeclaration(IImportDeclaration declaration) throws JavaModelException{
			if (declaration.isOnDemand()){ 
				String packageName= declaration.getElementName().substring(0, declaration.getElementName().length() - 2);
				return JavaModelUtil.findTypeContainer(declaration.getJavaProject(), packageName);
			} else 
				return JavaModelUtil.findTypeContainer(declaration.getJavaProject(), declaration.getElementName());
	}

	private void analyzeImportDeclaration(IImportDeclaration imp, RefactoringStatus result) throws JavaModelException{
		if (!imp.isOnDemand())
			return; //analyzed earlier
		
		IJavaElement imported= convertFromImportDeclaration(imp);
		if (imported == null)
			return;
			
		if (imported instanceof IPackageFragment){
			ICompilationUnit[] cus= ((IPackageFragment)imported).getCompilationUnits();
			for (int i= 0; i < cus.length; i++) {
				analyzeImportedTypes(cus[i].getTypes(), result, imp);
			}	
		} else {
			//cast safe: see JavaModelUtility.convertFromImportDeclaration
			analyzeImportedTypes(((IType)imported).getTypes(), result, imp);
		}
	}
	
	private IFile[] getAllFilesToModify() throws CoreException{
		List result= new ArrayList();
		result.addAll(Arrays.asList(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits())));
		if (fQualifiedNameFinder != null)
			result.addAll(Arrays.asList(fQualifiedNameFinder.getAllFiles()));
		return (IFile[]) result.toArray(new IFile[result.size()]);
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}
	
	/*
	 * Analyzes all compilation units in which type is referenced
	 */
	private RefactoringStatus analyzeAffectedCompilationUnits(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		fReferences= Checks.excludeCompilationUnits(fReferences, result);
		if (result.hasFatalError())
			return result;
			
		result.merge(Checks.checkCompileErrorsInAffectedFiles(fReferences));	
		
		pm.beginTask("", fReferences.length); //$NON-NLS-1$
		result.merge(checkConflictingTypes(pm));
		return result;
	}
	
	private RefactoringStatus checkConflictingTypes(IProgressMonitor pm) throws JavaModelException{
		IJavaSearchScope scope= RefactoringScopeFactory.create(fType);
		ISearchPattern pattern= SearchEngine.createSearchPattern(fNewName, IJavaSearchConstants.TYPE, IJavaSearchConstants.ALL_OCCURRENCES, true);
		ICompilationUnit[] cusWithReferencesToConflictingTypes= RefactoringSearchEngine.findAffectedCompilationUnits(pm, scope, pattern);
		if (cusWithReferencesToConflictingTypes.length == 0)
			return new RefactoringStatus();
		ICompilationUnit[] 	cusWithReferencesToRenamedType= getCus(fReferences);

		ICompilationUnit[] intersection= isIntersectionEmpty(cusWithReferencesToRenamedType, cusWithReferencesToConflictingTypes);
		if (intersection.length == 0)
			return new RefactoringStatus();
		
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < intersection.length; i++) {
			Context context= JavaSourceContext.create(intersection[i]);
			String message= RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.another_type", //$NON-NLS-1$
				new String[]{fNewName, intersection[i].getElementName()});
			result.addWarning(message, context);
		}	
		return result;
	}
	
	private static ICompilationUnit[] isIntersectionEmpty(ICompilationUnit[] a1, ICompilationUnit[] a2){
		Set set1= new HashSet(Arrays.asList(a1));
		Set set2= new HashSet(Arrays.asList(a2));
		set1.retainAll(set2);
		return (ICompilationUnit[]) set1.toArray(new ICompilationUnit[set1.size()]);
	}
	
	private static ICompilationUnit[] getCus(SearchResultGroup[] searchResultGroups){
		List cus= new ArrayList(searchResultGroups.length);
		for (int i= 0; i < searchResultGroups.length; i++) {
			ICompilationUnit cu= searchResultGroups[i].getCompilationUnit();
			if (cu != null)
				cus.add(cu);
		}
		return (ICompilationUnit[]) cus.toArray(new ICompilationUnit[cus.size()]);
	}
	
	private static String getFullPath(ICompilationUnit cu) throws JavaModelException{
		Assert.isTrue(cu.exists());
		return ResourceUtil.getResource(cu).getFullPath().toString();
	}

	/* 
	 * all the analysis that can be done with no AST walking
	 */
	private RefactoringStatus analyzeCompilationUnit(ICompilationUnit cu) throws JavaModelException{
		/* ICompilationUnit.getType expects simple name */
		String name= fNewName;
		if (fNewName.indexOf(".") != -1) //$NON-NLS-1$
			name= fNewName.substring(0, fNewName.indexOf(".")); //$NON-NLS-1$
		if (! cu.getType(name).exists())
			return null;
			
		return RefactoringStatus.createErrorStatus(RefactoringCoreMessages.getFormattedString("RenameTypeRefactoring.name_conflict2", //$NON-NLS-1$
																		new Object[]{fNewName, ResourceUtil.getResource(cu).getFullPath()}));
	}
	
	//------------- Changes ---------------
	
	/*
	 * non java-doc
	 * @see IRefactoring#createChange
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException{
		try{
			pm.beginTask(RefactoringCoreMessages.getString("RenameTypeRefactoring.creating_change"), 4); //$NON-NLS-1$
			CompositeChange builder= new CompositeChange();
			builder.addAll(fChangeManager.getAllChanges());
			if (fQualifiedNameFinder != null)
				builder.addAll(fQualifiedNameFinder.getAllChanges());
			if (willRenameCU())
				builder.add(new RenameResourceChange(ResourceUtil.getResource(fType), fNewName + ".java")); //$NON-NLS-1$
			pm.worked(1);	
			return builder;	
		} catch (CoreException e){
			throw new JavaModelException(e);
		} 
	}
	
	private boolean willRenameCU() throws JavaModelException{
		if (! isPrimaryType())
			return false;
		if (! checkNewPathValidity().isOK())
			return false;
		if (! Checks.checkCompilationUnitNewName(fType.getCompilationUnit(), fNewName).isOK())
			return false;
		return true;	
	}
	
	private boolean isPrimaryType(){
		return Checks.isTopLevel(fType) && hasSameNameAsCU();
	}
	
	private boolean hasSameNameAsCU() {
		return fType.getCompilationUnit().getElementName().equals(fType.getElementName() + ".java");//$NON-NLS-1$
	}
	
	private void addTextMatches(TextChangeManager manager, IProgressMonitor pm) throws JavaModelException{
		TextMatchFinder.findTextMatches(pm, createRefactoringScope(), this, manager);
	}
	
	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask("", 7); //$NON-NLS-1$
			TextChangeManager manager= new TextChangeManager();
					
			if (fUpdateReferences)
				addReferenceUpdates(manager, new SubProgressMonitor(pm, 3));
	
			pm.worked(1);
			
			addTypeDeclarationUpdate(manager);
			pm.worked(1);
			
			addConstructorRenames(manager);
			pm.worked(1);
			
			pm.subTask(RefactoringCoreMessages.getString("RenameTypeRefactoring.searching_text")); //$NON-NLS-1$
			addTextMatches(manager, new SubProgressMonitor(pm, 1));
			
			return manager;
		} finally{
			pm.done();
		}	
	}
	
	private void addTypeDeclarationUpdate(TextChangeManager manager) throws CoreException{
		String name= RefactoringCoreMessages.getString("RenameTypeRefactoring.update"); //$NON-NLS-1$
		int typeNameLength= fType.getElementName().length();
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fType.getCompilationUnit());
		manager.get(cu).addTextEdit(name, SimpleTextEdit.createReplace(fType.getNameRange().getOffset(), typeNameLength, fNewName));
	}
	
	private void addConstructorRenames(TextChangeManager manager) throws CoreException{
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fType.getCompilationUnit());
		IMethod[] methods= fType.getMethods();
		int typeNameLength= fType.getElementName().length();
		for (int i= 0; i < methods.length; i++){
			if (methods[i].isConstructor()) {
				/*
				 * constructor declarations cannot be fully qualified so we can use simple replace here
				 *
				 * if (methods[i].getNameRange() == null), then it's a binary file so it's wrong anyway 
				 * (checked as a precondition)
				 */				
				String name= RefactoringCoreMessages.getString("RenameTypeRefactoring.rename_constructor"); //$NON-NLS-1$
				manager.get(cu).addTextEdit(name, SimpleTextEdit.createReplace(methods[i].getNameRange().getOffset(), typeNameLength, fNewName));
			}
		}
	}
	
	private void addReferenceUpdates(TextChangeManager manager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", fReferences.length); //$NON-NLS-1$
		for (int i= 0; i < fReferences.length; i++){
			ICompilationUnit cu= fReferences[i].getCompilationUnit();
			if (cu == null)
				continue;
					
			ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
			String name= RefactoringCoreMessages.getString("RenameTypeRefactoring.update_reference"); //$NON-NLS-1$
			SearchResult[] results= fReferences[i].getSearchResults();

			for (int j= 0; j < results.length; j++){
				SearchResult searchResult= results[j];
				int offset= searchResult.getStart();
				int length= searchResult.getEnd() - searchResult.getStart();
				manager.get(wc).addTextEdit(name, new UpdateTypeReferenceEdit(offset, length, fNewName, fType.getElementName()));
			}
			pm.worked(1);
		}
	}
	
	private void computeQualifiedNameMatches(IProgressMonitor pm) throws JavaModelException {
		IPackageFragment fragment= fType.getPackageFragment();
		fQualifiedNameFinder= new QualifiedNameFinder();
		fQualifiedNameFinder.process(fType.getFullyQualifiedName(),  fragment.getElementName() + "." + fNewName, //$NON-NLS-1$
			fFilePatterns, fType.getJavaProject().getProject(), pm);
	}	
}
