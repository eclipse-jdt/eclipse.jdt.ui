package org.eclipse.jdt.internal.corext.refactoring.structure;


import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.rename.RippleMethodFinder;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

class ReorderParametersRefactoring extends Refactoring {
	
	private IMethod fMethod;
	private IMethod[] fRippleMethods;
	
	private String[] fNewParameterNames;
	private String[] fOldParameterNames;
	
	private int[] fPermutation;
	private SearchResultGroup[] fOccurrences;
	
	public ReorderParametersRefactoring(IMethod method) {
		Assert.isNotNull(method);
		fMethod= method;
		setOldParameterNames();
		if (fOldParameterNames != null)
			setNewParameterOrder(fOldParameterNames, false);
	}
	
	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("ReorderParametersRefactoring.name"); //$NON-NLS-1$
	}
	
	public IMethod getMethod(){
		return fMethod;
	}
	
	public void setNewParameterOrder(String[] newParameterOrder) {
		setNewParameterOrder(newParameterOrder, true);
	}
	
	private void setNewParameterOrder(String[] newParameterOrder, boolean check) {
		if (check)
			checkParameterNames(newParameterOrder);
		fNewParameterNames= createCopy(newParameterOrder);
		fPermutation= createPermutation(fOldParameterNames, fNewParameterNames);
	}
	
	public String[] getNewParameterOrder() {
		return fNewParameterNames;
	}
	
	public int[] getParamaterPermutation() {
		return fPermutation;
	}
	
	public int getNewParameterPosition(String name){
		return getIndexOf(name, fNewParameterNames);
	}
	
	public boolean isInputSameAsInitial(){
		return isIdentityPermutation(fPermutation);
	}
	
	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("ReorderParametersRefactoring.checking_preconditions"), 2); //$NON-NLS-1$

			RefactoringStatus result= new RefactoringStatus();
			
			fRippleMethods= RippleMethodFinder.getRelatedMethods(fMethod, new SubProgressMonitor(pm, 1), null);
			fOccurrences= getOccurrences(new SubProgressMonitor(pm, 1));			
			fOccurrences= Checks.excludeCompilationUnits(fOccurrences, result);
			if (result.hasFatalError())
				return result;
				
			result.merge(Checks.checkCompileErrorsInAffectedFiles(fOccurrences));

			result.merge(checkNativeMethods());
			return result;
		} finally{
			pm.done();
		}	
	}
	
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(Checks.checkAvailability(fMethod));

		if (fOldParameterNames == null || fOldParameterNames.length < 2)
			result.addFatalError(RefactoringCoreMessages.getString("ReorderParametersRefactoring.too_few_parameters"));  //$NON-NLS-1$
		
		return result;
	}
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		IMethod orig= (IMethod)WorkingCopyUtil.getOriginal(fMethod);
		if (orig == null || ! orig.exists()){
			String message= RefactoringCoreMessages.getFormattedString("ReorderParametersRefactoring.method_deleted", fMethod.getCompilationUnit().getElementName());//$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}
		fMethod= orig;
		
		
		RefactoringStatus result= Checks.checkIfCuBroken(fMethod);
		if (result.hasFatalError())
			return result;

		if (! fMethod.isConstructor() && MethodChecks.isVirtual(fMethod)){
			result.merge(MethodChecks.checkIfComesFromInterface(getMethod(), new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;	
			
			result.merge(MethodChecks.checkIfOverridesAnother(getMethod(), new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;			
		} 
		if (! fMethod.isConstructor() && fMethod.getDeclaringType().isInterface()){
			result.merge(MethodChecks.checkIfOverridesAnother(getMethod(), new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;
		}
			
		return result;
	}

	private RefactoringStatus checkNativeMethods() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fRippleMethods.length; i++) {
			if (JdtFlags.isNative(fRippleMethods[i])){
				String message= RefactoringCoreMessages.getFormattedString("ReorderParametersRefactoring.native", //$NON-NLS-1$
					new String[]{JavaElementUtil.createMethodSignature(fRippleMethods[i]), JavaModelUtil.getFullyQualifiedName(fRippleMethods[i].getDeclaringType())});
				result.addError(message, JavaSourceContext.create(fRippleMethods[i]));			
			}								
		}
		return result;
	}

	private static String[] createCopy(String[] orig){
		if (orig == null)
			return null;
		String[] result= new String[orig.length];
		for (int i= 0; i < result.length; i++) {
			result[i]= orig[i];
		}
		return result;
	}
	
	private static int getIndexOf(int el, int[] array){
		for (int i= 0; i < array.length; i++){
			if (el == array[i])
				return i;
		}
		return -1;		
	}
	
	private static int getIndexOf(Object el, Object[] array){
		for (int i= 0; i < array.length; i++){
			if (el.equals(array[i]))
				return i;
		}
		Assert.isTrue(false, RefactoringCoreMessages.getString("ReorderParametersRefactoring.not_found")); //$NON-NLS-1$
		return -1;
	}

	private static int[] createPermutation(Object[] a1, Object[] a2){
		Assert.isTrue(a1.length == a2.length);
		int[] result= new int[a1.length];
		for (int i= 0; i < a1.length; i++)
			result[i]= getIndexOf(a2[i], a1);
		return result;	
	}
		
	/*
	 * permutations come in 2 flavors - one shows the new position of an element
	 * the other one points to the old element element in currect position 
	 * this method converts the latter to the former
	 * e.g. 2 0 1 -> 1 2 0
	 */
	private static int[] convertPermutation(int[] p){
		int[] result= new int[p.length];
		for (int i= 0; i < result.length; i++) {
			result[i]= getIndexOf(i, p);
		}
		return result;
	}

	private static boolean isIdentityPermutation(int[] perm){
		for (int i= 0; i< perm.length; i++){
			if (perm[i] != i)
				return false;
		}
		return true;
	}
	
	private void setOldParameterNames(){
		if (fMethod.isBinary()) 
			return;
		try{
			fOldParameterNames= fMethod.getParameterNames();
		} catch (JavaModelException e){
			//ignore
		}	
	}
	
	private void checkParameterNames(String[] newParameterNames) {
		try{
			Assert.isNotNull(fOldParameterNames);
			Assert.isTrue(newParameterNames.length > 1);
			Assert.isTrue(fOldParameterNames.length == newParameterNames.length);
			for (int i= 0; i < newParameterNames.length; i++){
				Assert.isNotNull(newParameterNames[i]);
			}	
			List names= Arrays.asList(fMethod.getParameterNames());
			for (int i= 0; i < newParameterNames.length; i++) {
				Assert.isTrue(names.contains(newParameterNames[i]));
			}
		} catch (JavaModelException e){
			//ignore
		}	
	}
	
	private IJavaSearchScope createRefactoringScope()  throws JavaModelException{
		if (fRippleMethods.length == 1)	
			return RefactoringScopeFactory.create(fRippleMethods[0]);
		return SearchEngine.createWorkspaceScope();
	}
	
	private SearchResultGroup[] getOccurrences(IProgressMonitor pm) throws JavaModelException{
		return RefactoringSearchEngine.search(pm, createRefactoringScope(), createSearchPattern());	
	}
	
	private ISearchPattern createSearchPattern() throws JavaModelException{
		ISearchPattern pattern= createSearchPattern(fRippleMethods[0]);
		for (int i= 1; i < fRippleMethods.length; i++) {
			pattern= SearchEngine.createOrSearchPattern(pattern, createSearchPattern(fRippleMethods[i]));
		}
		return pattern;
	}

	private ISearchPattern createSearchPattern(IMethod method) {
		return SearchEngine.createSearchPattern(method, IJavaSearchConstants.ALL_OCCURRENCES);
	}
	
	//-------------------------------------
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		TextChangeManager manager= new TextChangeManager();
		createChange(pm, manager);
		return new CompositeChange(RefactoringCoreMessages.getString("ReorderParametersRefactoring.changeName"), manager.getAllChanges()); //$NON-NLS-1$
	}
	
	public void createChange(IProgressMonitor pm, TextChangeManager manager) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("ReorderParametersRefactoring.preview"), fOccurrences.length); //$NON-NLS-1$
			for (int i= 0; i < fOccurrences.length ; i++){
				SearchResultGroup group= fOccurrences[i]; 
				List regionArrays= getSourceRangeArrays(group);
				for (Iterator iter= regionArrays.iterator(); iter.hasNext();) {
					ISourceRange[] sourceRanges= (ISourceRange[]) iter.next();
					Assert.isTrue(sourceRanges.length == fPermutation.length);
					ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getCompilatonUnit(group));
					if (cu == null)
						continue;
					MultiTextEdit edit= new PermuteSourceRangesTextEdit(sourceRanges, convertPermutation(fPermutation));	
					manager.get(cu).addTextEdit(RefactoringCoreMessages.getString("ReorderParametersRefactoring.editName"), edit);			 //$NON-NLS-1$
				}
				pm.worked(1);
			}
		} catch (CoreException e){
			throw new JavaModelException(e);
		}	finally{
			pm.done();
		}		
	}
	
	private static ICompilationUnit getCompilatonUnit(SearchResultGroup group){
		IJavaElement element= JavaCore.create(group.getResource());
		if (element instanceof ICompilationUnit)
			return (ICompilationUnit)element;
		return null;	
	}
	
	/**
	 * returns a List of ISourceRange[]
	 */
	private List getSourceRangeArrays(SearchResultGroup group) throws JavaModelException{
		return ReorderParameterMoveFinder.findParameterSourceRanges(group);
	}
}