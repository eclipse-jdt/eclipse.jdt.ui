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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.participants.JavaProcessors;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IReferenceUpdating;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.SearchUtils;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

public abstract class RenameMethodProcessor extends JavaRenameProcessor implements IReferenceUpdating {
	
	private SearchResultGroup[] fOccurrences;
	private boolean fUpdateReferences;
	private IMethod fMethod;
	private Set/*<IMethod>*/ fMethodsToRename;
	private TextChangeManager fChangeManager;
	private WorkingCopyOwner fWorkingCopyOwner;
	
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.renameMethodProcessor"; //$NON-NLS-1$
	
	public RenameMethodProcessor(IMethod method) {
		initialize(method);
	}
	
	protected void initialize(IMethod method) {
		fMethod= method;
		setNewElementName(fMethod.getElementName());
		fUpdateReferences= true;
		fWorkingCopyOwner= new WorkingCopyOwner() {/*must subclass*/};
	}	
	
	protected void setData(RenameMethodProcessor other) {
		fUpdateReferences= other.fUpdateReferences;
		setNewElementName(other.getNewElementName());
	}
	
	//---- IRefactoringProcessor --------------------------------

	public String getIdentifier() {
		return IDENTIFIER;
	}
	
	public boolean isApplicable() throws CoreException {
		if (fMethod == null)
			return false;
		if (! Checks.isAvailable(fMethod))
			return false;
		if (fMethod.isConstructor())
			return false;
		if (isSpecialCase(fMethod))
			return false;
		return true;
	}
	
	public String getProcessorName() {
		return RefactoringCoreMessages.getFormattedString(
			"RenameMethodRefactoring.name", //$NON-NLS-1$
			new String[]{fMethod.getElementName(), getNewElementName()});
	}
	
	protected String[] getAffectedProjectNatures() throws CoreException {
		return JavaProcessors.computeAffectedNatures(fMethod);
	}

	public Object[] getElements() {
		return new Object[] {fMethod};
	}

	protected void loadDerivedParticipants(RefactoringStatus status, List result, String[] natures, SharableParticipants shared) throws CoreException {
		Set derived= new HashSet(fMethodsToRename);
		derived.remove(fMethod);
		loadDerivedParticipants(status, result, 
			derived.toArray(), 
			new RenameArguments(getNewElementName(), getUpdateReferences()), 
			null, natures, shared);
	}
	
	//---- IRenameProcessor -------------------------------------
	
	public final String getCurrentElementName(){
		return fMethod.getElementName();
	}
		
	public final RefactoringStatus checkNewElementName(String newName) {
		Assert.isNotNull(newName, "new name"); //$NON-NLS-1$
		RefactoringStatus result= Checks.checkMethodName(newName);
		if (Checks.isAlreadyNamed(fMethod, newName))
			result.addFatalError(RefactoringCoreMessages.getString("RenameMethodRefactoring.same_name")); //$NON-NLS-1$
		return result;
	}
	
	public Object getNewElement() {
		return fMethod.getDeclaringType().getMethod(getNewElementName(), fMethod.getParameterTypes());
	}
	
	public final IMethod getMethod() {
		return fMethod;
	}
	
	private void initializeMethodsToRename(IProgressMonitor pm) throws CoreException {
		fMethodsToRename= new HashSet(Arrays.asList(RippleMethodFinder2.getRelatedMethods(fMethod, pm, null)));
	}
	
	protected Set getMethodsToRename() {
		return fMethodsToRename;
	}
	
	//---- IReferenceUpdating -----------------------------------

	public boolean canEnableUpdateReferences() {
		return true;
	}

	public final void setUpdateReferences(boolean update) {
		fUpdateReferences= update;
	}	
	
	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}	
	
	//----------- preconditions ------------------

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		if (fMethod == null || ! fMethod.exists()){
			String message= RefactoringCoreMessages.getFormattedString("RenameMethodRefactoring.deleted", //$NON-NLS-1$
								fMethod.getCompilationUnit().getElementName());
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		
		RefactoringStatus result= Checks.checkAvailability(fMethod);
		if (result.hasFatalError())
				return result;
		result.merge(Checks.checkIfCuBroken(fMethod));
		if (JdtFlags.isNative(fMethod))
			result.addError(RefactoringCoreMessages.getString("RenameMethodRefactoring.no_native")); //$NON-NLS-1$
		return result;
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException {
		try{
			RefactoringStatus result= new RefactoringStatus();
			pm.beginTask("", 19); //$NON-NLS-1$
			// TODO workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=40367
			if (!Checks.isAvailable(fMethod)) {
				result.addFatalError(RefactoringCoreMessages.getString("RenameMethodProcessor.is_binary"), JavaStatusContext.create(fMethod)); //$NON-NLS-1$
				return result;
			}
			result.merge(Checks.checkIfCuBroken(fMethod));
			if (result.hasFatalError())
				return result;
			pm.setTaskName(RefactoringCoreMessages.getString("RenameMethodRefactoring.taskName.checkingPreconditions")); //$NON-NLS-1$
			result.merge(checkNewElementName(getNewElementName()));
			pm.worked(1);
			
			boolean mustAnalyzeShadowing;
			IMethod[] newNameMethods= searchForDeclarationsOfClashingMethods(new SubProgressMonitor(pm, 2));
			if (newNameMethods.length == 0) {
				mustAnalyzeShadowing= false;
				pm.worked(2);
			} else {
				IType[] outerTypes= searchForOuterTypesOfReferences(newNameMethods, new SubProgressMonitor(pm, 3));
				if (outerTypes.length > 0) {
					//There exists a reference to a clashing method, where the reference is in a nested type.
					//That nested type could be a type in a ripple method's hierarchy, which could
					//cause the reference to bind to the new ripple method instead of to
					//its old binding (a method of an enclosing scope).
					//-> Getting *more* references than before -> Semantics not preserved.
					//Example: RenamePrivateMethodTests#testFail6()
					//TODO: could pass declaringTypes to the RippleMethodFinder and check whether
					//a hierarchy contains one of outerTypes (or an outer type of an outerType, recursively).
					mustAnalyzeShadowing= true;
					
				} else {
					boolean hasOldRefsInInnerTypes= true;
						//TODO: to implement this optimization:
						//- move search for references to before this check.
						//- collect references in inner types.
						//- for each reference, check for all supertypes and their enclosing types
						//(recursively), whether they declare a rippleMethod
					if (hasOldRefsInInnerTypes) {
						//There exists a reference to a ripple method in a nested type
						//of a type in the hierarchy of any ripple method.
						//When that reference is renamed, and one of the supertypes of the
						//nested type declared a method matching the new name, then
						//the renamed reference will bind to the method in its supertype,
						//since inherited methods bind stronger than methods from enclosing scopes.
						//Getting *less* references than before -> Semantics not preserved.
						//Examples: RenamePrivateMethodTests#testFail2(), RenamePrivateMethodTests#testFail5()
						mustAnalyzeShadowing= true;
					} else {
						mustAnalyzeShadowing= false;
					}
				}
			}
			
			initializeMethodsToRename(new SubProgressMonitor(pm, 3));
			pm.setTaskName(RefactoringCoreMessages.getString("RenameMethodRefactoring.taskName.searchingForReferences")); //$NON-NLS-1$
			fOccurrences= getOccurrences(new SubProgressMonitor(pm, 4), result);	
			pm.setTaskName(RefactoringCoreMessages.getString("RenameMethodRefactoring.taskName.checkingPreconditions")); //$NON-NLS-1$
			
			if (fUpdateReferences)
				result.merge(checkRelatedMethods());
			pm.worked(1);
			
			result.merge(analyzeCompilationUnits()); //removes CUs with syntax errors
			pm.worked(1);
			
			if (result.hasFatalError())
				return result;
			
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 3), result);
			if (fUpdateReferences & mustAnalyzeShadowing)
				result.merge(analyzeRenameChanges(new SubProgressMonitor(pm, 1)));
			else
				pm.worked(1);
			
			ValidateEditChecker checker= (ValidateEditChecker)context.getChecker(ValidateEditChecker.class);
			checker.addFiles(getAllFilesToModify());
			return result;
		} finally{
			pm.done();
		}	
	}
	
	private IType[] searchForOuterTypesOfReferences(IMethod[] newNameMethods, IProgressMonitor pm) throws CoreException {
		final Set outerTypesOfReferences= new HashSet();
		SearchPattern pattern= RefactoringSearchEngine.createOrPattern(newNameMethods, IJavaSearchConstants.REFERENCES);
		IJavaSearchScope scope= createRefactoringScope(getMethod());
		SearchRequestor requestor= new SearchRequestor() {
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				IMember member= (IMember) match.getElement();
				IType declaring= member.getDeclaringType();
				if (declaring == null)
					return;
				IType outer= declaring.getDeclaringType();
				if (outer != null)
					outerTypesOfReferences.add(declaring);
			}
		};
		new SearchEngine().search(pattern, SearchUtils.getDefaultSearchParticipants(),
				scope, requestor, pm);
		return (IType[]) outerTypesOfReferences.toArray(new IType[outerTypesOfReferences.size()]);
	}

	private IMethod[] searchForDeclarationsOfClashingMethods(IProgressMonitor pm) throws CoreException {
		final List results= new ArrayList();
		SearchPattern pattern= createNewMethodPattern();
		IJavaSearchScope scope= RefactoringScopeFactory.create(getMethod().getJavaProject());
		SearchRequestor requestor= new SearchRequestor() {
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				results.add(match.getElement());
			}
		};
		new SearchEngine().search(pattern, SearchUtils.getDefaultSearchParticipants(), scope, requestor, pm);
		return (IMethod[]) results.toArray(new IMethod[results.size()]);
	}
	
	private SearchPattern createNewMethodPattern() throws JavaModelException {
		StringBuffer stringPattern= new StringBuffer(getNewElementName()).append('(');
		int paramCount= getMethod().getParameterNames().length;
		while (paramCount > 1) {
			stringPattern.append("*,"); //$NON-NLS-1$
			--paramCount;
		}
		if (paramCount > 0)
			stringPattern.append('*');
		stringPattern.append(')');
		
		return SearchPattern.createPattern(stringPattern.toString(), IJavaSearchConstants.METHOD,
				IJavaSearchConstants.DECLARATIONS, SearchUtils.GENERICS_AGNOSTIC_MATCH_RULE);
	}
	
	protected final IJavaSearchScope createRefactoringScope() throws CoreException {
		return createRefactoringScope(fMethod);
	}
	//TODO: shouldn't scope take all ripple methods into account?
	protected static final IJavaSearchScope createRefactoringScope(IMethod method) throws CoreException {
		return RefactoringScopeFactory.create(method);
	}
	
	/** */
	SearchPattern createOccurrenceSearchPattern(IProgressMonitor pm) {
		HashSet methods= new HashSet(fMethodsToRename);
		methods.add(fMethod);
		IMethod[] ms= (IMethod[]) methods.toArray(new IMethod[methods.size()]);
		pm.done();
		return RefactoringSearchEngine.createOrPattern(ms, IJavaSearchConstants.ALL_OCCURRENCES);
	}

	SearchResultGroup[] getOccurrences(){
		return fOccurrences;	
	}
	
	/*
	 * XXX made protected to allow overriding and working around bug 39700
	 */
	protected SearchResultGroup[] getOccurrences(IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		pm.beginTask("", 2);	 //$NON-NLS-1$
		SearchPattern pattern= createOccurrenceSearchPattern(new SubProgressMonitor(pm, 1));
		return RefactoringSearchEngine.search(pattern, createRefactoringScope(),
			new MethodOccurenceCollector(getMethod().getElementName()), new SubProgressMonitor(pm, 1), status);	
	}

	private static boolean isSpecialCase(IMethod method) throws CoreException {
		if (  method.getElementName().equals("toString") //$NON-NLS-1$
			&& (method.getNumberOfParameters() == 0)
			&& (method.getReturnType().equals("Ljava.lang.String;")  //$NON-NLS-1$
				|| method.getReturnType().equals("QString;") //$NON-NLS-1$
				|| method.getReturnType().equals("Qjava.lang.String;"))) //$NON-NLS-1$
			return true;		
		else return (method.isMainMethod());
	}
	
	private static RefactoringStatus checkIfConstructorName(IMethod method, String newName) {
		return Checks.checkIfConstructorName(method, newName, method.getDeclaringType().getElementName());
	}
	
	private RefactoringStatus checkRelatedMethods() throws CoreException { 
		RefactoringStatus result= new RefactoringStatus();
		for (Iterator iter= fMethodsToRename.iterator(); iter.hasNext(); ) {
			IMethod method= (IMethod)iter.next();
			
			result.merge(checkIfConstructorName(method, getNewElementName()));
			
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
	
	private IFile[] getAllFilesToModify() {
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus analyzeCompilationUnits() throws CoreException {
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
	
	private RefactoringStatus analyzeRenameChanges(IProgressMonitor pm) throws CoreException {
		ICompilationUnit[] newDeclarationWCs= null;
		try {
			pm.beginTask("", 4); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			ICompilationUnit[] declarationCUs= getDeclarationCUs();
			newDeclarationWCs= RenameAnalyzeUtil.createNewWorkingCopies(declarationCUs,
					fChangeManager, fWorkingCopyOwner, new SubProgressMonitor(pm, 1));
			
			IMethod[] newMethods= new IMethod[fMethodsToRename.size()];
			int i= 0;
			for (Iterator iter= fMethodsToRename.iterator(); iter.hasNext(); i++) {
				IMethod method= (IMethod) iter.next();
				ICompilationUnit newCu= RenameAnalyzeUtil.findWorkingCopyForCu(newDeclarationWCs, method.getCompilationUnit());
				newMethods[i]= getNewMethod(method, newCu);
			}
			
//			SearchResultGroup[] newOccurrences= findNewOccurrences(newMethods, newDeclarationWCs, new SubProgressMonitor(pm, 3));
			SearchResultGroup[] newOccurrences= batchFindNewOccurrences(newMethods, newDeclarationWCs, new SubProgressMonitor(pm, 3), result);
			
			result.merge(RenameAnalyzeUtil.analyzeRenameChanges2(fChangeManager, fOccurrences, newOccurrences, getNewElementName()));
			return result;
		} finally{
			pm.done();
			if (newDeclarationWCs != null){
				for (int i= 0; i < newDeclarationWCs.length; i++) {
					newDeclarationWCs[i].discardWorkingCopy();		
				}
			}	
		}
	}
	
	//Lower memory footprint than batchFindNewOccurrences. Not used because it is too slow.
	//Final solution is maybe to do searches in chunks of ~ 50 CUs.
//	private SearchResultGroup[] findNewOccurrences(IMethod[] newMethods, ICompilationUnit[] newDeclarationWCs, IProgressMonitor pm) throws CoreException {
//		pm.beginTask("", fOccurrences.length * 2); //$NON-NLS-1$
//		
//		SearchPattern refsPattern= RefactoringSearchEngine.createOrPattern(newMethods, IJavaSearchConstants.REFERENCES);
//		SearchParticipant[] searchParticipants= SearchUtils.getDefaultSearchParticipants();
//		IJavaSearchScope scope= RefactoringScopeFactory.create(newMethods);
//		MethodOccurenceCollector requestor= new MethodOccurenceCollector(getNewElementName());
//		SearchEngine searchEngine= new SearchEngine(fWorkingCopyOwner);
//		
//		//TODO: should process only references
//		for (int j= 0; j < fOccurrences.length; j++) { //should be getReferences()
//			//cut memory peak by holding only one reference CU at a time in memory
//			ICompilationUnit originalCu= fOccurrences[j].getCompilationUnit();
//			ICompilationUnit newWc= null;
//			try {
//				ICompilationUnit wc= RenameAnalyzeUtil.findWorkingCopyForCu(newDeclarationWCs, originalCu);
//				if (wc == null) {
//					newWc= RenameAnalyzeUtil.createNewWorkingCopy(originalCu, fChangeManager, fWorkingCopyOwner,
//							new SubProgressMonitor(pm, 1));
//				}
//				searchEngine.search(refsPattern, searchParticipants, scope,	requestor, new SubProgressMonitor(pm, 1));
//			} finally {
//				if (newWc != null)
//					newWc.discardWorkingCopy();
//			}
//		}
//		SearchResultGroup[] newResults= RefactoringSearchEngine.groupByResource(requestor.getResults());
//		pm.done();
//		return newResults;
//	}

	private SearchResultGroup[] batchFindNewOccurrences(IMethod[] newMethods, ICompilationUnit[] newDeclarationWCs, IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		
		SearchPattern refsPattern= RefactoringSearchEngine.createOrPattern(newMethods, IJavaSearchConstants.REFERENCES);
		SearchParticipant[] searchParticipants= SearchUtils.getDefaultSearchParticipants();
		IJavaSearchScope scope= RefactoringScopeFactory.create(newMethods);
		MethodOccurenceCollector requestor= new MethodOccurenceCollector(getNewElementName());
		SearchEngine searchEngine= new SearchEngine(fWorkingCopyOwner);
		
		ArrayList needWCs= new ArrayList();
		HashSet declaringCUs= new HashSet(newDeclarationWCs.length);
		for (int i= 0; i < newDeclarationWCs.length; i++)
			declaringCUs.add(newDeclarationWCs[i].getPrimary());
		for (int i= 0; i < fOccurrences.length; i++) {
			ICompilationUnit cu= fOccurrences[i].getCompilationUnit();
			if (! declaringCUs.contains(cu))
				needWCs.add(cu);
		}
		ICompilationUnit[] otherWCs= null;
		try {
			otherWCs= RenameAnalyzeUtil.createNewWorkingCopies(
					(ICompilationUnit[]) needWCs.toArray(new ICompilationUnit[needWCs.size()]),
					fChangeManager, fWorkingCopyOwner, new SubProgressMonitor(pm, 1));
			searchEngine.search(refsPattern, searchParticipants, scope,	requestor, new SubProgressMonitor(pm, 1));
		} finally {
			pm.done();
			if (otherWCs != null) {
				for (int i= 0; i < otherWCs.length; i++) {
					otherWCs[i].discardWorkingCopy();
				}
			}
		}
		SearchResultGroup[] newResults= RefactoringSearchEngine.groupByCu(requestor.getResults(), status);
		return newResults;
	}
	
	private ICompilationUnit[] getDeclarationCUs() {
		Set cus= new HashSet();
		for (Iterator iter= fMethodsToRename.iterator(); iter.hasNext();) {
			IMethod method= (IMethod) iter.next();
			cus.add(method.getCompilationUnit());
		}
		return (ICompilationUnit[]) cus.toArray(new ICompilationUnit[cus.size()]);
	}
	
	private IMethod getNewMethod(IMethod method, ICompilationUnit newWorkingCopyOfDeclaringCu) throws CoreException{
		IType type= method.getDeclaringType();
		IType typeWc= (IType) JavaModelUtil.findInCompilationUnit(newWorkingCopyOfDeclaringCu, type);
		if (typeWc == null)
			return null;
		
		String[] paramTypeSignatures= method.getParameterTypes();
		return typeWc.getMethod(getNewElementName(), paramTypeSignatures);
	}

	//-------
	private static IMethod[] classesDeclareMethodName(ITypeHierarchy hier, List classes, IMethod method, String newName)  throws CoreException {
		Set result= new HashSet();
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
					result.add(foundMethod);
				else if ((! isMethodPrivate) && (! JdtFlags.isPrivate(methods[j])))
					result.add(foundMethod);
			}
		}
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}

	final static IMethod[] hierarchyDeclaresMethodName(IProgressMonitor pm, IMethod method, String newName) throws CoreException {
		Set result= new HashSet();
		IType type= method.getDeclaringType();
		ITypeHierarchy hier= type.newTypeHierarchy(pm);
		IMethod foundMethod= Checks.findMethod(newName, method.getParameterTypes().length, false, type);
		if (foundMethod != null) 
			result.add(foundMethod);

		IMethod[] foundInHierarchyClasses= classesDeclareMethodName(hier, Arrays.asList(hier.getAllClasses()), method, newName);
		if (foundInHierarchyClasses != null)
			result.addAll(Arrays.asList(foundInHierarchyClasses));
		
		IType[] implementingClasses= hier.getImplementingClasses(type);	
		IMethod[] foundInImplementingClasses= classesDeclareMethodName(hier, Arrays.asList(implementingClasses), method, newName);
		if (foundInImplementingClasses != null)
			result.addAll(Arrays.asList(foundInImplementingClasses));
		return (IMethod[]) result.toArray(new IMethod[result.size()]);	
	}
				
	//-------- changes -----
	
	public final Change createChange(IProgressMonitor pm) throws CoreException {
		try{
			return new DynamicValidationStateChange(RefactoringCoreMessages.getString("Change.javaChanges"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally{
			pm.done();
		}	
	}
	
	private TextChangeManager createChangeManager(IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		TextChangeManager manager= new TextChangeManager(true);
		
		/* don't really want to add declaration and references separetely in this refactoring 
		* (declarations of methods are different than declarations of anything else)
		*/
		if (! fUpdateReferences)
			addDeclarationUpdate(manager); // TODO: only one declaration updated, not all of them
		else
			addOccurrences(manager, pm, status);	
		return manager;
	}
	
	void addOccurrences(TextChangeManager manager, IProgressMonitor pm, RefactoringStatus status) throws CoreException/*thrown in subtype*/{
		pm.beginTask("", fOccurrences.length);				 //$NON-NLS-1$
		for (int i= 0; i < fOccurrences.length; i++){
			ICompilationUnit cu= fOccurrences[i].getCompilationUnit();
			if (cu == null)	
				continue;
			
			TextChange textChange= manager.get(cu);
			SearchMatch[] results= fOccurrences[i].getSearchResults();
			for (int j= 0; j < results.length; j++){
				String editName= RefactoringCoreMessages.getString("RenameMethodRefactoring.update_occurrence"); //$NON-NLS-1$
				TextChangeCompatibility.addTextEdit(textChange, editName, createTextChange(results[j]));
			}
			pm.worked(1);
			if (pm.isCanceled())
				throw new OperationCanceledException();
		}
		pm.done();
	}
	
	private void addDeclarationUpdate(TextChangeManager manager) throws CoreException {
		ICompilationUnit cu= fMethod.getCompilationUnit();
		TextChange change= manager.get(cu);
		addDeclarationUpdate(change);
	}
	
	final void addDeclarationUpdate(TextChange change) throws CoreException {
		String editName= RefactoringCoreMessages.getString("RenameMethodRefactoring.update_declaration"); //$NON-NLS-1$
		ISourceRange nameRange= fMethod.getNameRange();
		ReplaceEdit replaceEdit= new ReplaceEdit(nameRange.getOffset(), nameRange.getLength(), getNewElementName());
		TextChangeCompatibility.addTextEdit(change, editName, replaceEdit);
	}
	
	final TextEdit createTextChange(SearchMatch searchResult) {
		return new ReplaceEdit(searchResult.getOffset(), searchResult.getLength(), getNewElementName());
	}
}	
