/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.core.refactoring.methods;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
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
import org.eclipse.jdt.internal.core.refactoring.base.IChange;
import org.eclipse.jdt.internal.core.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.core.refactoring.tagging.IPreactivatedRefactoring;
import org.eclipse.jdt.internal.core.refactoring.tagging.IRenameRefactoring;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBuffer;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.internal.core.refactoring.text.SimpleTextChange;
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
import org.eclipse.jdt.internal.core.refactoring.RefactoringCoreMessages;

/*
 * non java-doc
 * not API
 */
abstract class RenameMethodRefactoring extends MethodRefactoring implements IRenameRefactoring, IPreactivatedRefactoring{
	
	private String fNewName;
	private List fOccurrences;
	private ITextBufferChangeCreator fTextBufferChangeCreator;
	
	public RenameMethodRefactoring(ITextBufferChangeCreator changeCreator, IMethod method) {
		super(method);
		fTextBufferChangeCreator= changeCreator;
		Assert.isNotNull(fTextBufferChangeCreator, "change creator"); //$NON-NLS-1$
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
		return RefactoringCoreMessages.getFormattedString("RenameMethodRefactoring.name", //$NON-NLS-1$
															new String[]{getMethod().getElementName(), getNewName()});
	}
	
	//----------- Conditions ------------------
		
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 6); //$NON-NLS-1$
		result.merge(Checks.checkIfCuBroken(getMethod()));
		if (result.hasFatalError())
			return result;
		result.merge(Checks.checkAffectedResourcesAvailability(getOccurrences(new SubProgressMonitor(pm, 4))));
		pm.subTask(RefactoringCoreMessages.getString("RenameMethodRefactoring.checking_name")); //$NON-NLS-1$
		result.merge(checkNewName());
		pm.worked(1);
		pm.subTask(RefactoringCoreMessages.getString("RenameMethodRefactoring.analyzing_hierarchy")); //$NON-NLS-1$
		result.merge(checkRelatedMethods(new SubProgressMonitor(pm, 1)));
		pm.done();
		return result;
	}
	
	private IJavaSearchScope createRefactoringScope() throws JavaModelException{
		if (Flags.isPrivate(getMethod().getFlags()))
			return SearchEngine.createJavaSearchScope(new IResource[]{getResource(getMethod())});
		else
			return SearchEngine.createWorkspaceScope();	
	}
	
	List getOccurrences(IProgressMonitor pm) throws JavaModelException{
		if (fOccurrences == null){
			if (pm == null)
				pm= new NullProgressMonitor();
			pm.beginTask("", 2);	 //$NON-NLS-1$
			pm.subTask(RefactoringCoreMessages.getString("RenameMethodRefactoring.creating_pattern")); //$NON-NLS-1$
			ISearchPattern pattern= createSearchPattern(new SubProgressMonitor(pm, 1));
			pm.subTask(RefactoringCoreMessages.getString("RenameMethodRefactoring.searching")); //$NON-NLS-1$
			fOccurrences= RefactoringSearchEngine.search(new SubProgressMonitor(pm, 1), createRefactoringScope(), pattern);	
			pm.done();
		}
		return fOccurrences;
	}
			
	public RefactoringStatus checkPreactivation() throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAvailability(getMethod()));
		if (isSpecialCase(getMethod()))
			result.addError(RefactoringCoreMessages.getString("RenameMethodRefactoring.special_case")); //$NON-NLS-1$
		if (getMethod().isConstructor())
			result.addFatalError(RefactoringCoreMessages.getString("RenameMethodRefactoring.no_constructors"));	 //$NON-NLS-1$
		return result;
	}
	
	public final RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= Checks.checkIfCuBroken(getMethod());
		if (Flags.isNative(getMethod().getFlags()))
			result.addError(RefactoringCoreMessages.getString("RenameMethodRefactoring.no_native")); //$NON-NLS-1$
		return result;
	}
					
	private boolean isSpecialCase(IMethod method) throws JavaModelException{
		if (  method.getElementName().equals("toString") //$NON-NLS-1$
			&& (method.getNumberOfParameters() == 0)
			&& (method.getReturnType().equals("Ljava.lang.String;")  //$NON-NLS-1$
				|| method.getReturnType().equals("QString;") //$NON-NLS-1$
				|| method.getReturnType().equals("Qjava.lang.String;"))) //$NON-NLS-1$
			return true;		
		else return (JavaModelUtility.isMainMethod(method));
	}
	
	private String computeErrorMessage(IMethod method, String key){
		return RefactoringCoreMessages.getFormattedString(
			key,
			new String[]{method.getElementName(), method.getDeclaringType().getFullyQualifiedName()});
	}
	
	private RefactoringStatus checkIfConstructorName(IMethod method){
		return Checks.checkIfConstructorName(method, fNewName, method.getDeclaringType().getElementName());
	}
	
	private RefactoringStatus checkRelatedMethods(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		Iterator methods = getMethodsToRename(getMethod(), pm).iterator();
		while (methods.hasNext()){
			IMethod method= (IMethod)methods.next();
			
			result.merge(checkIfConstructorName(method));
			
			if (! method.exists()){
				result.addFatalError(computeErrorMessage(method, "RenameMethodRefactoring.not_in_mode")); //$NON-NLS-1$
				continue;
			}	
			if (method.isBinary())
				result.addFatalError(computeErrorMessage(method, "RenameMethodRefactoring.no_binary")); //$NON-NLS-1$
			if (method.isReadOnly())
				result.addFatalError(computeErrorMessage(method, "RenameMethodRefactoring.no_read_only")); //$NON-NLS-1$
			if (Flags.isNative(method.getFlags()))
				result.addError(computeErrorMessage(method, "RenameMethodRefactoring.no_native_1")); //$NON-NLS-1$
			}
		return result;	
	}
	
	public final RefactoringStatus checkNewName() {
		Assert.isNotNull(getMethod(), "method"); //$NON-NLS-1$
		Assert.isNotNull(getNewName(), "new name"); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		
		result.merge(Checks.checkMethodName(fNewName));
		
		result.merge(checkIfConstructorName(getMethod()));
					
		if (Checks.isAlreadyNamed(getMethod(), getNewName()))
			result.addFatalError(RefactoringCoreMessages.getString("RenameMethodRefactoring.same_name")); //$NON-NLS-1$
		return result;
	}
			
	/*package*/ HashSet getMethodsToRename(IMethod method, IProgressMonitor pm) throws JavaModelException{
		//method has been added in the caller	
		pm.done();
		return new HashSet(0);
	}
	
	/************ Changes ***************/
	
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.getString("RenameMethodRefactoring.creating_change"), 25); //$NON-NLS-1$
		CompositeChange builder= new CompositeChange();
		addOccurrences(pm, builder);
		pm.worked(5);
		pm.done();
		return builder;
	}
	
	void addOccurrences(IProgressMonitor pm, CompositeChange builder) throws JavaModelException{
		for (Iterator iter= getOccurrences(null).iterator(); iter.hasNext();){
			List l= (List)iter.next();
			ITextBufferChange change= fTextBufferChangeCreator.create(RefactoringCoreMessages.getString("RenameMethodRefactoring.update_references"), (ICompilationUnit)JavaCore.create(((SearchResult)l.get(0)).getResource())); //$NON-NLS-1$
			for (Iterator subIter= l.iterator(); subIter.hasNext();){
				change.addSimpleTextChange(createTextChange((SearchResult)subIter.next()));
			}
			builder.addChange(change);
			pm.worked(1);
		}
		fOccurrences= null; //to prevent memory leak
	}
	
	protected SimpleReplaceTextChange createTextChange(SearchResult searchResult) {
		SimpleReplaceTextChange change= new SimpleReplaceTextChange(RefactoringCoreMessages.getString("RenameMethodRefactoring.update_reference"), searchResult.getStart(), searchResult.getEnd() - searchResult.getStart(), fNewName) { //$NON-NLS-1$
			protected SimpleTextChange[] adjust(ITextBuffer buffer) {
				String oldText= buffer.getContent(getOffset(), getLength());
				String oldMethodName= getMethod().getElementName();
				int leftBracketIndex= oldText.indexOf("("); //$NON-NLS-1$
				if (leftBracketIndex != -1) {
					setLength(leftBracketIndex);
					oldText= oldText.substring(0, leftBracketIndex);
					int theDotIndex= oldText.lastIndexOf("."); //$NON-NLS-1$
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
		pm.beginTask("", 4); //$NON-NLS-1$
		HashSet methods= methodsToRename(getMethod(), new SubProgressMonitor(pm, 3));
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
		pm.beginTask("", 3); //$NON-NLS-1$
		methods.add(method);
		pm.worked(1);
		methods.addAll(getMethodsToRename(method, new SubProgressMonitor(pm, 2)));
		pm.done();
		return methods;
	}
	
	/* XXX
	 * possible performance improvement
	 * needs rework
	 */
	private boolean classesDeclareMethodName(ITypeHierarchy hier, List classes, IMethod method, String newName)  throws JavaModelException  {
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
