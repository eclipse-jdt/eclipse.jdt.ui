/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.IWorkingCopy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdatingRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public abstract class RenameMethodRefactoring extends Refactoring implements IRenameRefactoring, IReferenceUpdatingRefactoring{
	
	private String fNewName;
	private SearchResultGroup[] fOccurrences;
	private boolean fUpdateReferences;
	private IMethod fMethod;
	private TextChangeManager fChangeManager;
	private ICompilationUnit[] fNewWorkingCopies;
	
	RenameMethodRefactoring(IMethod method) {
		Assert.isNotNull(method);
		fMethod= method;
		fNewName= method.getElementName();
		fUpdateReferences= true;
	}
		
	/**
	 * Factory method to create appropriate instances
	 */
	public static RenameMethodRefactoring createInstance(IMethod method) throws JavaModelException{
		 if (JdtFlags.isPrivate(method))
		 	return new RenamePrivateMethodRefactoring(method);
		 else if (JdtFlags.isStatic(method))
		 	return new RenameStaticMethodRefactoring(method);
		 else if (method.getDeclaringType().isClass())	
		 	return new RenameVirtualMethodRefactoring(method);
		 else 	
		 	return new RenameMethodInInterfaceRefactoring(method);	
	}
	
	public static RenameMethodRefactoring createInstance(IMethod method, RenameMethodRefactoring other) throws JavaModelException {
		RenameMethodRefactoring result= createInstance(method);
		result.setData(other);
		return result;
	}
	
	protected void setData(RenameMethodRefactoring other) {
		fUpdateReferences= other.fUpdateReferences;
		fNewName= other.fNewName;
	}
	
	public Object getNewElement(){
		return fMethod.getDeclaringType().getMethod(fNewName, fMethod.getParameterTypes());
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
	 * @see IRenameRefactoring#setNewName
	 */
	public final void setNewName(String newName){
		Assert.isNotNull(newName);
		fNewName= newName;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#getCurrentName
	 */
	public final String getCurrentName(){
		return fMethod.getElementName();
	}
		
	/* non java-doc
	 * @see IRenameRefactoring#getNewName
	 */		
	public final String getNewName(){
		return fNewName;
	}
		
	/* non java-doc
	 * @see IRefactoring#getName
	 */
	public String getName(){
		return RefactoringCoreMessages.getFormattedString("RenameMethodRefactoring.name", //$NON-NLS-1$
															new String[]{fMethod.getElementName(), getNewName()});
	}
	
	public final IMethod getMethod(){
		return fMethod;
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#canUpdateReferences()
	 */
	public boolean canEnableUpdateReferences() {
		return true;
	}

	/* non java-doc
	 * @see IRenameRefactoring#setUpdateReferences(boolean)
	 */
	public final void setUpdateReferences(boolean update) {
		fUpdateReferences= update;
	}	
	
	/* non java-doc
	 * @see IRenameRefactoring#getUpdateReferences()
	 */	
	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}	
	
	//----------- preconditions ------------------
	
	/*
	 * non java-doc
	 * @see IPreactivatedRefactoring#checkPreactivation
	 */
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkAvailability(fMethod));
		if (isSpecialCase(fMethod))
			result.addError(RefactoringCoreMessages.getString("RenameMethodRefactoring.special_case")); //$NON-NLS-1$
		if (fMethod.isConstructor())
			result.addFatalError(RefactoringCoreMessages.getString("RenameMethodRefactoring.no_constructors"));	 //$NON-NLS-1$
		return result;
	}

	/*
	 * non java-doc
	 * @see Refactoring#checkActivation
	 */		
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		IMethod orig= getOriginalMethod(fMethod);
		if (orig == null || ! orig.exists()){
			String message= RefactoringCoreMessages.getFormattedString("RenameMethodRefactoring.deleted", //$NON-NLS-1$
								fMethod.getCompilationUnit().getElementName());
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fMethod= orig;
		
		RefactoringStatus result= Checks.checkIfCuBroken(fMethod);
		if (JdtFlags.isNative(fMethod))
			result.addError(RefactoringCoreMessages.getString("RenameMethodRefactoring.no_native")); //$NON-NLS-1$
		return result;
	}

	private static IMethod getOriginalMethod(IMethod method) throws JavaModelException {
		return (IMethod)WorkingCopyUtil.getOriginal(method);
	}
	
	/* non java-doc
	 * @see IRenameRefactoring#checkNewName
	 */
	public final RefactoringStatus checkNewName(String newName) {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		RefactoringStatus result= Checks.checkMethodName(newName);
		if (Checks.isAlreadyNamed(fMethod, newName))
			result.addFatalError(RefactoringCoreMessages.getString("RenameMethodRefactoring.same_name")); //$NON-NLS-1$
		return result;
	}
	
	/*
	 * non java-doc
	 * @see Refactoring#checkInput
	 */	
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		try{
			RefactoringStatus result= new RefactoringStatus();
			pm.beginTask("", 4); //$NON-NLS-1$
			result.merge(Checks.checkIfCuBroken(fMethod));
			if (result.hasFatalError())
				return result;
			pm.setTaskName(RefactoringCoreMessages.getString("RenameMethodRefactoring.taskName.checkingPreconditions")); //$NON-NLS-1$
			result.merge(checkNewName(fNewName));
			pm.worked(1);
			
			pm.setTaskName(RefactoringCoreMessages.getString("RenameMethodRefactoring.taskName.searchingForReferences")); //$NON-NLS-1$
			fOccurrences= getOccurrences(new SubProgressMonitor(pm, 4));	
			pm.setTaskName(RefactoringCoreMessages.getString("RenameMethodRefactoring.taskName.checkingPreconditions")); //$NON-NLS-1$
			
			if (fUpdateReferences)
				result.merge(checkRelatedMethods(new SubProgressMonitor(pm, 1)));
			else
				pm.worked(1);
			
			if (fUpdateReferences)
				result.merge(analyzeCompilationUnits());	
			pm.worked(1);
			
			if (result.hasFatalError())
				return result;
			
			if (fUpdateReferences)
				result.merge(analyzeRenameChanges(new SubProgressMonitor(pm, 1)));
				
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 3));
			result.merge(validateModifiesFiles());
			return result;
		} catch (CoreException e){	
			throw new JavaModelException(e);	
		} finally{
			pm.done();
		}	
	}
	
	private IJavaSearchScope createRefactoringScope() throws JavaModelException{
		return RefactoringScopeFactory.create(fMethod);
	}
	
	ISearchPattern createSearchPattern(IProgressMonitor pm) throws JavaModelException{
		return createSearchPattern(pm, fMethod, null);
	}
	
	private static ISearchPattern createSearchPattern(IProgressMonitor pm, IMethod method, IWorkingCopy[] workingCopies) throws JavaModelException{
		pm.beginTask("", 4); //$NON-NLS-1$
		Set methods= methodsToRename(method, new SubProgressMonitor(pm, 3), workingCopies);
		IMethod[] ms= (IMethod[]) methods.toArray(new IMethod[methods.size()]);
		pm.done();
		return RefactoringSearchEngine.createSearchPattern(ms, IJavaSearchConstants.ALL_OCCURRENCES);
	}
	
	static Set getMethodsToRename(IMethod method, IProgressMonitor pm, IWorkingCopy[] workingCopies) throws JavaModelException{
		return new HashSet(Arrays.asList(RippleMethodFinder.getRelatedMethods(method, pm, workingCopies)));
	}
	
	private static Set methodsToRename(IMethod method, IProgressMonitor pm, IWorkingCopy[] workingCopies) throws JavaModelException{
		HashSet methods= new HashSet();
		pm.beginTask("", 1); //$NON-NLS-1$
		methods.add(method);
		methods.addAll(getMethodsToRename(method, new SubProgressMonitor(pm, 1), workingCopies));
		pm.done();
		return methods;
	}
	
	SearchResultGroup[] getOccurrences(){
		return fOccurrences;	
	}
	
	private SearchResultGroup[] getOccurrences(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 2);	 //$NON-NLS-1$
		ISearchPattern pattern= createSearchPattern(new SubProgressMonitor(pm, 1));
		return RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), createRefactoringScope(), pattern);	
	}

	private static boolean isSpecialCase(IMethod method) throws JavaModelException{
		if (  method.getElementName().equals("toString") //$NON-NLS-1$
			&& (method.getNumberOfParameters() == 0)
			&& (method.getReturnType().equals("Ljava.lang.String;")  //$NON-NLS-1$
				|| method.getReturnType().equals("QString;") //$NON-NLS-1$
				|| method.getReturnType().equals("Qjava.lang.String;"))) //$NON-NLS-1$
			return true;		
		else return (method.isMainMethod());
	}
	
	private static RefactoringStatus checkIfConstructorName(IMethod method, String newName){
		return Checks.checkIfConstructorName(method, newName, method.getDeclaringType().getElementName());
	}
	
	private RefactoringStatus checkRelatedMethods(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator iter= getMethodsToRename(fMethod, pm, null).iterator(); iter.hasNext(); ){
			IMethod method= (IMethod)iter.next();
			
			result.merge(checkIfConstructorName(method, fNewName));
			
			String[] msgData= new String[]{method.getElementName(), JavaModelUtil.getFullyQualifiedName(method.getDeclaringType())};
			if (! method.exists()){
				result.addFatalError(RefactoringCoreMessages.getFormattedString("RenameMethodRefactoring.not_in_model", msgData)); //$NON-NLS-1$ 
				continue;
			}
			if (method.isBinary())
				result.addFatalError(RefactoringCoreMessages.getFormattedString("RenameMethodRefactoring.no_binary", msgData)); //$NON-NLS-1$
			if (method.isReadOnly())
				result.addFatalError(RefactoringCoreMessages.getFormattedString("RenameMethodRefactoring.no_read_only", msgData));//$NON-NLS-1$
			if (JdtFlags.isNative(method))
				result.addError(RefactoringCoreMessages.getFormattedString("RenameMethodRefactoring.no_native_1", msgData));//$NON-NLS-1$
		}
		return result;	
	}
	
	private IFile[] getAllFilesToModify() throws JavaModelException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}
	
	private RefactoringStatus analyzeCompilationUnits() throws JavaModelException{
		if (fOccurrences.length == 0)
			return null;
			
		RefactoringStatus result= new RefactoringStatus();
		fOccurrences= Checks.excludeCompilationUnits(fOccurrences, result);
		if (result.hasFatalError())
			return result;
		
		result.merge(Checks.checkCompileErrorsInAffectedFiles(fOccurrences));	
			
		return result;
	}
	
	//-------
	private RefactoringStatus analyzeRenameChanges(IProgressMonitor pm) throws JavaModelException{
		try {
			pm.beginTask("", 3); //$NON-NLS-1$
			
			TextChangeManager manager= createChangeManager(new SubProgressMonitor(pm, 1));
			SearchResultGroup[] oldOccurrences= getOldOccurrences(new SubProgressMonitor(pm, 1));
			SearchResultGroup[] newOccurrences= getNewOccurrences(new SubProgressMonitor(pm, 1), manager);
			RefactoringStatus result= RenameAnalyzeUtil.analyzeRenameChanges(manager, oldOccurrences, newOccurrences);
			return result;
		} catch(CoreException e) {
			throw new JavaModelException(e);
		} finally{
			pm.done();
			if (fNewWorkingCopies != null){
				for (int i= 0; i < fNewWorkingCopies.length; i++) {
					fNewWorkingCopies[i].destroy();		
				}
			}	
		}
	}

	private SearchResultGroup[] getOldOccurrences(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		ISearchPattern oldPattern= createSearchPattern(new SubProgressMonitor(pm, 1));
		return RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), createRefactoringScope(), oldPattern);
	}
	
	private SearchResultGroup[] getNewOccurrences(IProgressMonitor pm, TextChangeManager manager) throws CoreException {
		pm.beginTask("", 3); //$NON-NLS-1$
		try{
			ICompilationUnit[] compilationUnitsToModify= manager.getAllCompilationUnits();
			fNewWorkingCopies= RenameAnalyzeUtil.getNewWorkingCopies(compilationUnitsToModify, manager, new SubProgressMonitor(pm, 1));
			
			ICompilationUnit declaringCuWorkingCopy= RenameAnalyzeUtil.findWorkingCopyForCu(fNewWorkingCopies, fMethod.getCompilationUnit());
			if (declaringCuWorkingCopy == null)
				return new SearchResultGroup[0];
			
			IMethod method= getNewMethod(declaringCuWorkingCopy);
			if (method == null || ! method.exists())
				return new SearchResultGroup[0];
			
			ISearchPattern newPattern= createSearchPattern(new SubProgressMonitor(pm, 1), method, fNewWorkingCopies);
			return RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), createRefactoringScope(), newPattern, fNewWorkingCopies);
		} finally{
			pm.done();
		}	
	}
	
	private IMethod getNewMethod(ICompilationUnit newWorkingCopyOfDeclaringCu) throws JavaModelException{
		IType[] allNewTypes= newWorkingCopyOfDeclaringCu.getAllTypes();
		String fullyTypeName= fMethod.getDeclaringType().getFullyQualifiedName();
		String[] paramTypeSignatures= fMethod.getParameterTypes();
		for (int i= 0; i < allNewTypes.length; i++) {
			if (allNewTypes[i].getFullyQualifiedName().equals(fullyTypeName))
				return allNewTypes[i].getMethod(fNewName, paramTypeSignatures);
		}
		return null;
	}

	//-------
	/**
	 * @return the found method or <code>null</code>
	 */
	private IMethod classesDeclareMethodName(ITypeHierarchy hier, List classes, IMethod method, String newName)  throws JavaModelException  {
		IType type= method.getDeclaringType();
		List subtypes= Arrays.asList(hier.getAllSubtypes(type));
		
		int parameterCount= method.getParameterTypes().length;
		boolean isMethodPrivate= JdtFlags.isPrivate(method);
		
		for (Iterator iter= classes.iterator(); iter.hasNext(); ){
			IType clazz= (IType) iter.next();
			IMethod[] methods= clazz.getMethods();
			boolean isSubclass= subtypes.contains(clazz);
			for (int j= 0; j < methods.length; j++) {
				IMethod foundMethod= Checks.findMethod(newName, parameterCount, false, new IMethod[] {methods[j]});
				if (foundMethod == null)
					continue;
				if (isSubclass || type.equals(clazz))
					return foundMethod;
				if ((! isMethodPrivate) && (! JdtFlags.isPrivate(methods[j])))
					return foundMethod;
			}
		}
		return null;
	}

	/**
	 * @return the found method or <code>null</code>
	 */	
	final IMethod hierarchyDeclaresMethodName(IProgressMonitor pm, IMethod method, String newName) throws JavaModelException{
		IType type= method.getDeclaringType();
		ITypeHierarchy hier= type.newTypeHierarchy(pm);
		IMethod foundMethod= Checks.findMethod(newName, method.getParameterTypes().length, false, type);
		if (foundMethod != null) 
			return foundMethod;

		IMethod foundInHierarchyClasses= classesDeclareMethodName(hier, Arrays.asList(hier.getAllClasses()), method, newName);
		if (foundInHierarchyClasses != null)
			return foundInHierarchyClasses;
		
		IType[] implementingClasses= hier.getImplementingClasses(type);	
		IMethod foundInImplementingClasses= classesDeclareMethodName(hier, Arrays.asList(implementingClasses), method, newName);
		if (foundInImplementingClasses != null)
			return foundInImplementingClasses;
		return null;	
	}
				
	//-------- changes -----
	
	public final IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			return new CompositeChange(RefactoringCoreMessages.getString("RenameMethodRefactoring.rename"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally{
			pm.done();
		}	
	}
	
	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		TextChangeManager manager= new TextChangeManager();
		
		/* don't really want to add declaration and references separetely in this refactoring 
		* (declarations of methods are different than declarations of anything else)
		*/
		if (! fUpdateReferences)
			addDeclarationUpdate(manager);
		else
			addOccurrences(manager, pm);	
		return manager;
	}
	
	void addOccurrences(TextChangeManager manager, IProgressMonitor pm) throws CoreException{
		pm.beginTask("", fOccurrences.length);				 //$NON-NLS-1$
		for (int i= 0; i < fOccurrences.length; i++){
			ICompilationUnit cu= fOccurrences[i].getCompilationUnit();
			if (cu == null)	
				continue;
			ICompilationUnit wc= WorkingCopyUtil.getWorkingCopyIfExists(cu);
			SearchResult[] results= fOccurrences[i].getSearchResults();
			for (int j= 0; j < results.length; j++){
				String editName= RefactoringCoreMessages.getString("RenameMethodRefactoring.update_occurrence"); //$NON-NLS-1$
				manager.get(wc).addTextEdit(editName, createTextChange(results[j]));
			}
			pm.worked(1);
		}		
	}
	
	private void addDeclarationUpdate(TextChangeManager manager) throws CoreException{
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fMethod.getCompilationUnit());
		TextChange change= manager.get(cu);
		addDeclarationUpdate(change);
	}
	
	final void addDeclarationUpdate(TextChange change) throws JavaModelException{
		change.addTextEdit(RefactoringCoreMessages.getString("RenameMethodRefactoring.update_declaration"), SimpleTextEdit.createReplace(fMethod.getNameRange().getOffset(), fMethod.getNameRange().getLength(), fNewName));  //$NON-NLS-1$
	}
	
	final TextEdit createTextChange(SearchResult searchResult) {
		return new UpdateMethodReferenceEdit(searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), fNewName, fMethod.getElementName());		
	}
}	
