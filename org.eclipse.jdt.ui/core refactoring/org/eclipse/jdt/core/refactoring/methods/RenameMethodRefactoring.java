/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.core.refactoring.methods;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IChange;
import org.eclipse.jdt.core.refactoring.RefactoringStatus;
import org.eclipse.jdt.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.core.refactoring.text.SimpleTextChange;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.core.refactoring.Assert;
import org.eclipse.jdt.internal.core.refactoring.Checks;
import org.eclipse.jdt.internal.core.refactoring.CompositeChange;
import org.eclipse.jdt.internal.core.refactoring.JavaModelUtility;
import org.eclipse.jdt.internal.core.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.core.refactoring.SearchResult;
import org.eclipse.jdt.internal.core.util.HackFinder;

/*
 * non java-doc
 * not API
 */
abstract class RenameMethodRefactoring extends MethodRefactoring implements IRenameRefactoring{
	
	private String fNewName;
	
	private List fOccurrences;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	public RenameMethodRefactoring(ITextBufferChangeCreator changeCreator, IJavaSearchScope scope, IMethod method, String newName) {
		super(scope, method);
		fTextBufferChangeCreator= changeCreator;
		Assert.isNotNull(fTextBufferChangeCreator, "change creator");
		Assert.isNotNull(newName, "new name");
		fNewName= newName;
	}
	
	public RenameMethodRefactoring(ITextBufferChangeCreator changeCreator, IMethod method) {
		super(method);
		fTextBufferChangeCreator= changeCreator;
		Assert.isNotNull(fTextBufferChangeCreator, "change creator");
	}

	/**
	 * @see IRenameRefactoring#setNewName
	 */
	public final void setNewName(String newName){
		Assert.isNotNull(newName);
		fNewName= newName;
	}
	
	/**
	 * @see IRenameRefactoring#getCurrentName
	 */
	public final String getCurrentName(){
		return getMethod().getElementName();
	}
		
	public final String getNewName(){
		return fNewName;
	}
		
	protected final ITextBufferChangeCreator getChangeCreator(){
		return fTextBufferChangeCreator;
	}
	
	/*package*/ final void setOccurrences(List Occurrences){
		fOccurrences= Occurrences;
	}
	
	/**
	 * @see IRefactoring#getName
	 */
	public String getName(){
		return "Rename method " + getMethod().getElementName() + " to: " + getNewName();
	}
	
	//----------- Conditions ------------------
		
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 3);
		result.merge(Checks.checkAffectedResourcesAvailability(getOccurrences(pm)));
		pm.worked(1);
		result.merge(checkNewName());
		pm.worked(1);
		result.merge(checkRelatedMethods(pm));
		pm.worked(1);
		pm.done();
		return result;
	}
	
	List getOccurrences(IProgressMonitor pm) throws JavaModelException{
		if (fOccurrences == null){
			if (pm == null)
				pm= new NullProgressMonitor();
			fOccurrences= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 3), getScope(), createSearchPattern(pm));	
		}	
		return fOccurrences;
	}
			
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAvailability(getMethod()));
		if (Flags.isNative(getMethod().getFlags()))
			result.addError("Renaming native methods can change the program's behavior");
		if (isSpecialCase(getMethod()))
			result.addError("This method is a special case - renaming might change program's behavior.");	
		return result;
	}
					
	private boolean isSpecialCase(IMethod method) throws JavaModelException{
		if (  method.getElementName().equals("toString")
			&& (method.getNumberOfParameters() == 0)
			&& (method.getReturnType().equals("Ljava.lang.String;") 
				|| method.getReturnType().equals("QString;")
				|| method.getReturnType().equals("Qjava.lang.String;")))
			return true;		
		else return (JavaModelUtility.isMainMethod(method));
	}
	
	private String computeErrorMessage(IMethod method, String suffix){
		String prefix= "Related method ";
		return prefix + " " + method.getElementName() + " (declared in " 
				 + method.getDeclaringType().getFullyQualifiedName() + ") " + suffix;
	}
	
	private RefactoringStatus checkRelatedMethods(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		Iterator methods = getMethodsToRename(getMethod(), pm).iterator();
		while (methods.hasNext()){
			IMethod method= (IMethod)methods.next();
			if (! method.exists()){
				result.addFatalError(computeErrorMessage(method, " does not exist in the model"));
				continue;
			}	
			if (method.isBinary())
				result.addFatalError(computeErrorMessage(method, " is binary"));
			if (method.isReadOnly())
				result.addFatalError(computeErrorMessage(method, " is read-only"));
			if (Flags.isNative(method.getFlags()))
				result.addError(computeErrorMessage(method, " is native. Refactoring can cause an UnasisfiedLinkError on run-time."));
			}
		return result;	
	}
	
	public final RefactoringStatus checkNewName() {
		Assert.isNotNull(getMethod(), "method");
		Assert.isNotNull(getNewName(), "new name");
		RefactoringStatus result= new RefactoringStatus();
		
		result.merge(Checks.checkMethodName(fNewName));
		
		if (Checks.isAlreadyNamed(getMethod(), getNewName()))
			result.addFatalError("Same name chosen");
		return result;
	}
			
	/*package*/ HashSet getMethodsToRename(IMethod method, IProgressMonitor pm) throws JavaModelException{
		//method has been added in the caller	
		return new HashSet(0);
	}
	
	/************ Changes ***************/
	
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("creating change", 25);
		CompositeChange builder= new CompositeChange();
		addOccurrences(pm, builder);
		pm.worked(5);
		pm.done();
		return builder;
	}
	
	void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws JavaModelException{
		for (Iterator iter= getOccurrences(null).iterator(); iter.hasNext();){
			List l= (List)iter.next();
			ITextBufferChange change= fTextBufferChangeCreator.create("Rename Method", (ICompilationUnit)JavaCore.create(((SearchResult)l.get(0)).getResource()));
			for (Iterator subIter= l.iterator(); subIter.hasNext();){
				change.addSimpleTextChange(createTextChange((SearchResult)subIter.next()));
			}
			builder.addChange(change);
			pm.worked(1);
		}
		HackFinder.fixMeSoon("maybe add dispose() method?");
		fOccurrences= null; //to prevent memory leak
	}
	
	protected SimpleReplaceTextChange createTextChange(SearchResult searchResult) {
		SimpleReplaceTextChange change= new SimpleReplaceTextChange("Method Occurrence Update", searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), fNewName) {
			protected SimpleTextChange[] adjust(ITextBuffer buffer) {
				String oldText= buffer.getContent(getOffset(), getLength());
				String oldMethodName= getMethod().getElementName();
				int leftBracketIndex= oldText.indexOf("(");
				if (leftBracketIndex != -1) {
					setLength(leftBracketIndex);
					oldText= oldText.substring(0, leftBracketIndex);
					int theDotIndex= oldText.lastIndexOf(".");
					if (theDotIndex == -1) {
						setText(getText() + oldText.substring(oldMethodName.length()));
					} else {
						String subText= oldText.substring(theDotIndex);
						int oldNameIndex= subText.indexOf(oldMethodName) + theDotIndex;
						String ending= oldText.substring(theDotIndex, oldNameIndex) + getText();
						oldText= oldText.substring(0, oldNameIndex + oldMethodName.length());
						setLength(oldNameIndex + oldMethodName.length());
						setText(oldText.substring(0, theDotIndex) + ending);
					}
				}
				return null;
			}
		};
		return change;
	}

	
	/*package*/ ISearchPattern createSearchPattern(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1);
		HashSet methods= methodsToRename(getMethod(), pm);
		Iterator iter= methods.iterator();
		ISearchPattern pattern= SearchEngine.createSearchPattern((IMethod)iter.next(), IJavaSearchConstants.ALL_OCCURRENCES);
		while (iter.hasNext()){
			ISearchPattern methodPattern= SearchEngine.createSearchPattern((IMethod)iter.next(), IJavaSearchConstants.ALL_OCCURRENCES);	
			pattern= SearchEngine.createOrSearchPattern(pattern, methodPattern);
		}
		pm.done();
		return pattern;
	}
	
	/*package*/ final HashSet methodsToRename(IMethod method, IProgressMonitor pm) throws JavaModelException{
		HashSet methods= new HashSet();
		methods.add(method);
		methods.addAll(getMethodsToRename(method, pm));
		return methods;
	}
	/**
	 * possible performance improvement
	 * needs rework
	 *
	 * it's too specific to have a home here - should be moved
	 */
	private boolean classesDeclareMethodName(ITypeHierarchy hier, List classes, IMethod method, String newName)  throws JavaModelException  {

		HackFinder.fixMeSoon(null);

		IType type= method.getDeclaringType();
		IType[] subtypesArr= hier.getAllSubtypes(type);
		int parameterCount= method.getParameterTypes().length;
		
		List subtypes= Arrays.asList(subtypesArr);
		
		Iterator classIter= classes.iterator();
		while (classIter.hasNext()) {
			IType clazz= (IType) classIter.next();
			IMethod[] methods= clazz.getMethods();
			boolean isSubclass= subtypes.contains(clazz);
			for (int j= 0; j < methods.length; j++) {
				if (null != Checks.findMethod(newName, parameterCount, false, new IMethod[] {methods[j]})) {
					if (isSubclass || type.equals(clazz))
						return true;
					if ((! Flags.isPrivate(method.getFlags())) && (! Flags.isPrivate(methods[j].getFlags())))
						return true;
				}
			}
		}
		return false;

	}
	
	/*package*/	boolean hierarchyDeclaresMethodName(IProgressMonitor pm, IMethod method, String newName) throws JavaModelException{
		IType type= method.getDeclaringType();
		ITypeHierarchy hier= type.newTypeHierarchy(pm);
		if (null != findMethod(newName, method.getParameterTypes().length, false, type)) {
			return true;
		}
		IType[] implementingClasses= hier.getImplementingClasses(type);
		return classesDeclareMethodName(hier, Arrays.asList(hier.getAllClasses()), method, newName) 
			|| classesDeclareMethodName(hier, Arrays.asList(implementingClasses), method, newName);
	}
	
	//-------------------------
			
	/**
	 * Finds a method in a type
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done
	 * @return The first found method or null, if nothing found
	 */
	static final IMethod findMethod(String name, int parameterCount, boolean isConstructor, IType type) throws JavaModelException {
		return Checks.findMethod(name, parameterCount, isConstructor, type.getMethods());
	}
	
	/**
	 * Finds a method in a type
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done
	 * @return The first found method or null, if nothing found
	 */
	static final IMethod findMethod(IMethod method, IType type) throws JavaModelException {
		return Checks.findMethod(method.getElementName(), method.getParameterTypes().length, method.isConstructor(), type.getMethods());
	}
	
	/**
	 * Finds a method in an array of methods
	 * This searches for a method with the same name and signature. Parameter types are only
	 * compared by the simple name, no resolving for the fully qualified type name is done
	 * @return The first found method or null, if nothing found
	 */
	static final IMethod findMethod(IMethod method, IMethod[] methods) throws JavaModelException {
		return Checks.findMethod(method.getElementName(), method.getParameterTypes().length, method.isConstructor(), methods);
	}
	
	
}	
