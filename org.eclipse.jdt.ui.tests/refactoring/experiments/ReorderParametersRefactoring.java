package experiments;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.methods.MethodRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;

public class ReorderParametersRefactoring extends MethodRefactoring {
	
	private String[] fNewParameterNames;
	private String[] fOldParameterNames;
	
	private int[] fPermutation;
	
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	private SearchResultGroup[] fOccurrences;
	
	public ReorderParametersRefactoring(ITextBufferChangeCreator changeCreator, IMethod method) {
		super(method);
		setOldParameterNames();
		fTextBufferChangeCreator= changeCreator;
	}

	public ReorderParametersRefactoring(ITextBufferChangeCreator changeCreator, IMethod method, String[] newParameterNames){
		this(changeCreator, method);
		fNewParameterNames= newParameterNames;
		fPermutation= createPermutation(fOldParameterNames, fNewParameterNames);
	}
	
	/**
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Reorder Parameters";
	}
	
	private void setOldParameterNames(){
		if (getMethod().isBinary()) 
			return;
		try{
			fOldParameterNames= getMethod().getParameterNames();
		} catch (JavaModelException e){
			//ignore
		}	
	}
	
	private void checkParameterNames(String[] newParameterNames) {
		Assert.isNotNull(fOldParameterNames, "names null");
		Assert.isTrue(newParameterNames.length > 1, "too few parameters");
		Assert.isTrue(fOldParameterNames.length == newParameterNames.length, "must be same number");
		for (int i= 0; i < newParameterNames.length; i++){
			Assert.isNotNull(newParameterNames[i], "name null:" + i);
		}	
		//XX should verify that names are the same
	}
	
	public void setNewParameterOrder(String[] newParameterOrder){
		checkParameterNames(newParameterOrder);
		fNewParameterNames= newParameterOrder;
		fPermutation= createPermutation(fOldParameterNames, fNewParameterNames);
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
			result[i]= getIndexOf(a1[i], a2);
		return result;	
	}
	
	private static boolean isNullPermutation(int[] perm){
		for (int i= 0; i< perm.length; i++){
			if (perm[i] != i)
				return false;
		}
		return true;
	}
	
	/**
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		//XXX should have initialize() method
		fOccurrences= getOccurrences(pm);

		RefactoringStatus result= new RefactoringStatus();
		if (isNullPermutation(fPermutation)){
			result.addFatalError("The order specified is the same as the current one.");
			return result;
		}	
		if (Flags.isNative(getMethod().getFlags()))
			result.addError("Reordering parameters in a native method will cause UnsatisfiedLinkError on runtime");
				
		result.merge(checkExpressionsInMethodInvocation());	
		return result;
	}
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAvailability(getMethod()));

		///XXX
		if (Flags.isPrivate(getMethod().getFlags()))
			result.addFatalError("only private for now");

		if (fOldParameterNames == null || fOldParameterNames.length < 2)
			result.addFatalError("too few parameters"); 
		
		return result;
	}	
	
	/**
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		return Checks.checkIfCuBroken(getMethod());
	}

	//XXX 
	private static ICompilationUnit getCompilationUnit(SearchResultGroup searchResults){
		return (ICompilationUnit)JavaCore.create(searchResults.getResource());
	}
	
	private RefactoringStatus checkExpressionsInMethodInvocation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fOccurrences.length; i++){
			SearchResultGroup group=fOccurrences[i]; 
			SearchResult[] searchResults= group.getSearchResults();
			result.merge(new ReorderParameterASTAnalyzer(searchResults).analyze(getCompilationUnit(group)));
		}
		return result;
	}
	
	//-------------------------------------
	/**
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		ITextBufferChange builder= fTextBufferChangeCreator.create("reorder parameters", getMethod().getCompilationUnit());
		List regionArrays= getRegionArrays();
		pm.beginTask("creating change", regionArrays.size());
		for (Iterator iter= regionArrays.iterator(); iter.hasNext() ;){
			TextRegion[] regions= (TextRegion[])iter.next();
			Assert.isTrue(regions.length == fPermutation.length, "wrong size of the regions array");
			for (int i= 0; i < regions.length; i++)
				builder.addMove("moving parameters", regions[i].getOffset(), regions[i].getLength(), regions[fPermutation[i]].getOffset());
			
			pm.worked(1);
		}
		pm.done();
		return builder;
	}

	/**
	 * returns a List of TextRegion[]
	 */
	private List getRegionArrays() throws JavaModelException{
		return new ReorderParameterMoveFinder().findParameterRegions(fOccurrences[0].getSearchResults(), getMethod().getCompilationUnit());
	}

	private IJavaSearchScope createRefactoringScope()  throws JavaModelException{
		return SearchEngine.createWorkspaceScope();
	}
		private SearchResultGroup[] getOccurrences(IProgressMonitor pm) throws JavaModelException{
		if (fOccurrences != null)
			return fOccurrences;
		if (pm == null)
			pm= new NullProgressMonitor();
		pm.beginTask("", 2);
		pm.subTask("creating pattern"); 
		ISearchPattern pattern= createSearchPattern(new SubProgressMonitor(pm, 1));
		pm.subTask("searching");
		fOccurrences= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), createRefactoringScope(), pattern);	
		pm.done();
		return fOccurrences;
	}
	
	private ISearchPattern createSearchPattern(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1); //$NON-NLS-1$
		ISearchPattern pattern= SearchEngine.createSearchPattern(getMethod(), IJavaSearchConstants.ALL_OCCURRENCES);
		pm.done();
		return pattern;
	}
}

