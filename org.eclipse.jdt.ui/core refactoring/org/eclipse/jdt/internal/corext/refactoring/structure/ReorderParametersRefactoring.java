package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
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
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;

public class ReorderParametersRefactoring extends Refactoring {
	
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
		return "Reorder Parameters";
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
	
	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("Checking preconditions", 2);

			RefactoringStatus result= new RefactoringStatus();
			
			if (isIdentityPermutation(fPermutation)){
				result.addFatalError("The order specified is the same as the current one.");
				return result;
			}
			
			fRippleMethods= RippleMethodFinder.getRelatedMethods(fMethod, new SubProgressMonitor(pm, 1));
			
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
		result.merge(checkAvailability(fMethod));

		if (fOldParameterNames == null || fOldParameterNames.length < 2)
			result.addFatalError("too few parameters"); 
		
		return result;
	}
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		IMethod orig= (IMethod)WorkingCopyUtil.getOriginal(fMethod);
		if (orig == null || ! orig.exists())
			return RefactoringStatus.createFatalErrorStatus("The selected method has been deleted from '" + fMethod.getCompilationUnit().getElementName()+ "'.");
		fMethod= orig;
		
		
		RefactoringStatus result= Checks.checkIfCuBroken(fMethod);
		if (result.hasFatalError())
			return result;

		//XX			
		if (MethodChecks.isVirtual(fMethod)){
			if (MethodChecks.overridesAnotherMethod(fMethod, new SubProgressMonitor(pm, 1)))
				result.addError("Method '" + JavaElementUtil.createMethodSignature(fMethod)+"' overrides another method. Perform this action in the most abstract type that declares it.");
			if (MethodChecks.isDeclaredInInterface(fMethod, new SubProgressMonitor(pm, 1)))	
				result.addError("Method '" + JavaElementUtil.createMethodSignature(fMethod)+"' is declared in an interface. Perform this action there.");
		}
		
		if (fMethod.getDeclaringType().isInterface()){
			if (MethodChecks.overridesAnotherMethod(fMethod, new SubProgressMonitor(pm, 1)))
				result.addError("Method '" + JavaElementUtil.createMethodSignature(fMethod)+"' overrides another method. Perform this action in the most abstract type that declares it.");
		}	
			
		return result;
	}

	private RefactoringStatus checkNativeMethods() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fRippleMethods.length; i++) {
			if (Flags.isNative(fRippleMethods[i].getFlags())){
				String msg= "Method " + JavaElementUtil.createMethodSignature(fRippleMethods[i]) 
									+ "' declared in type '" 
									+ fRippleMethods[i].getDeclaringType().getFullyQualifiedName() 
									+ "' is native.  Reordering parameters will cause UnsatisfiedLinkError on runtime if you do not update your native libraries.";
									
				result.addError(msg, JavaSourceContext.create(fRippleMethods[i]));			
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
		Assert.isTrue(false, "element not found");
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
		try{
			TextChangeManager manager= new TextChangeManager();
			pm.beginTask("preparing preview", fOccurrences.length);
			for (int i= 0; i < fOccurrences.length ; i++){
				SearchResultGroup group= fOccurrences[i]; 
				List regionArrays= getSourceRangeArrays(group);
				for (Iterator iter= regionArrays.iterator(); iter.hasNext();) {
					ISourceRange[] sourceRanges= (ISourceRange[]) iter.next();
					Assert.isTrue(sourceRanges.length == fPermutation.length);
					ICompilationUnit cu= getCompilatonUnit(group);
					if (cu == null)
						continue;
					MultiTextEdit edit= new PermuteSourceRangesTextEdit(sourceRanges, convertPermutation(fPermutation));	
					manager.get(cu).addTextEdit("reorder parameters", edit);			
				}
				pm.worked(1);
			}
			pm.done();
			return new CompositeChange("reorder parameters", manager.getAllChanges());
		} catch (CoreException e){
			throw new JavaModelException(e);
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