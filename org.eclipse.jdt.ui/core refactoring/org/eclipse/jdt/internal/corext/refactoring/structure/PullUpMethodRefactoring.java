package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.compiler.parser.InvalidInputException;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalSymbols;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultCollector;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusEntry.Context;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceSourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class PullUpMethodRefactoring extends Refactoring {

	private IMethod[] fMethodsToPullUp;
	private IMethod[] fMethodsToDelete;
	private CodeGenerationSettings fPreferenceSettings;
	private static final String PREF_TAB_SIZE= "org.eclipse.jdt.core.formatter.tabulation.size";

	public PullUpMethodRefactoring(IMethod[] methods, CodeGenerationSettings preferenceSettings){
		Assert.isTrue(methods.length > 0);
		fMethodsToPullUp= sortByOffset(methods);
		fMethodsToDelete= new IMethod[0];
		fPreferenceSettings= preferenceSettings;
	}
	
	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Pull Up Methods";
	}
	
	/**
	 * Sets the methodsToDelete.
	 * @param methodsToDelete The methodsToDelete to set
	 * no validation is done on these methods - they will simply be removed. 
	 * it is the caller's resposibility to ensure that the selection makes sense from the behavior-preservation point of view.
	 */
	public void setMethodsToDelete(IMethod[] methodsToDelete) {
		Assert.isNotNull(methodsToDelete);
		fMethodsToDelete = getOriginalMethods(methodsToDelete);
	}
	
	public IMethod[] getMethodsToPullUp()throws JavaModelException {
		return fMethodsToPullUp;
	}
	
	public IMethod[] getMatchingMethods() throws JavaModelException {
		Set result= new HashSet();
		//XXX pm
		IType superType= getSuperType();
		ITypeHierarchy hierarchy= superType.newTypeHierarchy(new NullProgressMonitor());
		IType[] subTypes= hierarchy.getAllSubtypes(superType);
		for (int i = 0; i < subTypes.length; i++) {
			result.addAll(getMatchingMethods(hierarchy, subTypes[i]));
		}
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}
	
	private Set getMatchingMethods(ITypeHierarchy hierarchy, IType type) throws JavaModelException {
		Set result= new HashSet();
		Map mapping= getMatchingMethodsMapping(hierarchy, type);
		for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
			result.addAll((Set)mapping.get(iter.next()));
		}
		return result;
	}
	
	private static void addToMapping(Map mapping, IMethod key, IMethod matchingMethod){
		Set matchingSet;
		if (mapping.containsKey(key)){
			matchingSet= (Set)mapping.get(key);
		}else{
			matchingSet= new HashSet();
			mapping.put(key, matchingSet);
		}
		matchingSet.add(matchingMethod);
	}
	
	/*
	 * mapping: Map: IMethod -> Set of IMethods
	 */
	private static void addToMapping(Map mapping, IMethod key, Set matchingMethods){
		for (Iterator iter= matchingMethods.iterator(); iter.hasNext();) {
			IMethod method= (IMethod) iter.next();
			addToMapping(mapping, key, method);
		}
	}
	
	private Map getMatchingMethodsMapping(ITypeHierarchy hierarchy, IType type) throws JavaModelException {
		Map result= new HashMap();//IMethod -> Set of IMethods
		
		//current type
		for (int i = 0; i < fMethodsToPullUp.length; i++) {
			IMethod found= findMethod(fMethodsToPullUp[i], type.getMethods());
			if (found != null)
				addToMapping(result, fMethodsToPullUp[i], found);
		}
		
		//subtypes
		IType[] subTypes= hierarchy.getAllSubtypes(type);
		for (int i = 0; i < subTypes.length; i++) {
			IType subType = subTypes[i];
			Map subTypeMapping= getMatchingMethodsMapping(hierarchy, subType);
			for (Iterator iter= subTypeMapping.keySet().iterator(); iter.hasNext();) {
				IMethod m= (IMethod) iter.next();
				addToMapping(result, m, (Set)subTypeMapping.get(m));
			}
		}
		return result;		
	}
	
	private static IMethod[] sortByOffset(IMethod[] methods){
		Comparator comparator= new Comparator(){
			public int compare(Object o1, Object o2){
				try{
					return ((IMethod)o2).getSourceRange().getOffset() - ((IMethod)o1).getSourceRange().getOffset();
				} catch (JavaModelException e){
					return o2.hashCode() - o1.hashCode();
				}	
			}
		};
		Arrays.sort(methods, comparator);
		return methods;
	}
	
	
	/**
	 * Finds a method in a list of methods.
	 * @return The found method or null, if nothing found
	 */
	private static IMethod findMethod(IMethod method, IMethod[] allMethods) throws JavaModelException {
		String name= method.getElementName();
		String[] paramTypes= method.getParameterTypes();
		boolean isConstructor= method.isConstructor();

		for (int i= 0; i < allMethods.length; i++) {
			if (JavaModelUtil.isSameMethodSignature(name, paramTypes, isConstructor, allMethods[i]))
				return allMethods[i];
		}
		return null;
	}
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm)	throws JavaModelException {
		try {
			RefactoringStatus result= new RefactoringStatus();
			pm.beginTask("", 3);
						
			result.merge(checkAllMethods());
			pm.worked(1);
			if (result.hasFatalError())
				return result;
			
			result.merge(checkCommonSuperType());
			pm.worked(1);
			if (result.hasFatalError())
				return result;			

			result.merge(checkDeclaringType());
			pm.worked(1);
			if (result.hasFatalError())
				return result;			

			if (getSuperType().isBinary())
				return RefactoringStatus.createFatalErrorStatus("Pull up method is not allowed on methods declared in subtypes binary types.");

			fMethodsToPullUp= getOriginalMethods(fMethodsToPullUp);		
			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}
	
	private static IMethod[] getOriginalMethods(IMethod[] methods){
		IMethod[] result= new IMethod[methods.length];
		for (int i= 0; i < methods.length; i++) {
			result[i]= (IMethod)WorkingCopyUtil.getOriginal(methods[i]);
		}
		return result;
	}

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 3);
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkAccesses(new SubProgressMonitor(pm, 1)));
			result.merge(checkSuperclass());
			pm.worked(1);
			result.merge(checkMethodsInSubclasses(new SubProgressMonitor(pm, 1)));
			return result;
		} finally {
			pm.done();
		}	
	}

	//--- private helpers
	
	private RefactoringStatus checkAllMethods() throws JavaModelException {
		//just 1 error message
		for (int i = 0; i < fMethodsToPullUp.length; i++) {
			IMethod method = fMethodsToPullUp[i];
			if (! method.exists())
				return RefactoringStatus.createFatalErrorStatus("Pull up method is not allowed on methods that do not exist.");			

			if (method.isBinary())
				return RefactoringStatus.createFatalErrorStatus("Pull up method is not allowed on binary methods.");	

			if (method.isConstructor())
				return RefactoringStatus.createFatalErrorStatus("Pull up method is not allowed on constructors.");	

			if (method.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus("Pull up method is not allowed on read-only methods.");					

			if (! method.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus("Pull up method is not allowed on methods with unknown structure.");					

			if (Flags.isStatic(method.getFlags())) //for now
				return RefactoringStatus.createFatalErrorStatus("Pull up method is not allowed on static methods.");
			
			if (Flags.isAbstract(method.getFlags()))
				return RefactoringStatus.createFatalErrorStatus("Pull up method is not allowed on abstract methods.");
				
 			if (Flags.isNative(method.getFlags())) //for now - move to input preconditions
				return RefactoringStatus.createFatalErrorStatus("Pull up method is not allowed on abstract methods.");				
		}
		return null;
	}
	
	private RefactoringStatus checkDeclaringType() throws JavaModelException {
		IType declaringType= getDeclaringType();
				
		if (declaringType.isInterface()) //for now
			return RefactoringStatus.createFatalErrorStatus("Pull up method is not allowed on interface methods.");
		
		if (declaringType.getFullyQualifiedName().equals("java.lang.Object"))
			return RefactoringStatus.createFatalErrorStatus("Pull up method is not allowed on methods declared in java.lang.Object.");	

		if (declaringType.isBinary())
			return RefactoringStatus.createFatalErrorStatus("Pull up method is not allowed on methods in subclasses of binary types.");	

		if (declaringType.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus("Pull up method is not allowed on methods in subclasses of read-only types.");	
		
		return null;
	}
	
	private RefactoringStatus checkCommonSuperType(){
		IType superType= fMethodsToPullUp[0].getDeclaringType(); //index safe - checked in constructor
		for (int i = 0; i < fMethodsToPullUp.length; i++) {
			if (! superType.equals(fMethodsToPullUp[i].getDeclaringType()))
				return RefactoringStatus.createFatalErrorStatus("All selected methods must be declared in the same type.");			
		}	
		return null;
	}
	
	private RefactoringStatus checkAccesses(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("Cheching referenced elements", 3);
		result.merge(checkAccessedTypes(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedFields(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedMethods(new SubProgressMonitor(pm, 1)));
		return result;
	}
	
	private RefactoringStatus checkAccessedTypes(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= getTypesReferencedInMethods(fMethodsToPullUp, pm);
		IType superType= getSuperType();
		for (int i= 0; i < accessedTypes.length; i++) {
			IType iType= accessedTypes[i];
			if (! canBeAccessedFrom(iType, superType)){
				String msg= "Type '" + iType.getFullyQualifiedName() + "' referenced in one of the pulled methods is not accessible from type '" 
								+ superType.getFullyQualifiedName() + "'.";
				result.addError(msg, JavaSourceContext.create(iType));
			}	
		}
		return result;
	}
	
	private RefactoringStatus checkAccessedFields(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IField[] accessedFields= getFieldsReferencedInMethods(fMethodsToPullUp, pm);
		IType superType= getSuperType();
		for (int i= 0; i < accessedFields.length; i++) {
			IField iField= accessedFields[i];
			if (! canBeAccessedFrom(iField, superType)){
				String msg= "Field '" + iField.getElementName() + "' referenced in one of the pulled methods is not accessible from type '" 
								+ superType.getFullyQualifiedName() + "'.";
				result.addError(msg, JavaSourceContext.create(iField));
			}	
		}
		return result;
	}

	private RefactoringStatus checkAccessedMethods(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		
		List pulledUpList= Arrays.asList(fMethodsToPullUp);
		List deletedList= Arrays.asList(fMethodsToDelete);
		
		IMethod[] accessedMethods= getMethodsReferencedInMethods(fMethodsToPullUp, pm);
		IType superType= getSuperType();
		for (int i= 0; i < accessedMethods.length; i++) {
			IMethod iMethod= accessedMethods[i];
			if (! canBeAccessedFrom(iMethod, superType) && ! pulledUpList.contains(iMethod) && !deletedList.contains(iMethod)){
				String msg= "Method '" + JavaElementUtil.createMethodSignature(iMethod) + "' referenced in one of the pulled methods is not accessible from type '" 
								+ superType.getFullyQualifiedName() + "'.";
				result.addError(msg, JavaSourceContext.create(iMethod));
			}	
		}
		return result;
	}
	
	private boolean canBeAccessedFrom(IMember member, IType newHome) throws JavaModelException{
		Assert.isTrue(!(member instanceof IInitializer));
		if (newHome.equals(member.getDeclaringType()))
			return true;
			
		if (newHome.equals(member))
			return true;	
		
		if (! member.exists())
			return false;
			
		if (Flags.isPrivate(member.getFlags()))
			return false;
			
		if (member.getDeclaringType() == null){ //top level
			if (! (member instanceof IType))
				return false;

			if (Flags.isPublic(member.getFlags()))
				return true;
				
			//FIX ME: protected and default treated the same
			return ((IType)member).getPackageFragment().equals(newHome.getPackageFragment());		
		}	

		 if (! canBeAccessedFrom(member.getDeclaringType(), newHome))
		 	return false;

		if (member.getDeclaringType().equals(getDeclaringType())) //XXX
			return false;
			
		if (Flags.isPublic(member.getFlags()))
			return true;
		 
		//FIX ME: protected and default treated the same
		return (member.getDeclaringType().getPackageFragment().equals(newHome.getPackageFragment()));		
	}
	
	private RefactoringStatus checkSuperclass() throws JavaModelException {	
		IType superType= getSuperType();
		IMethod[] superTypeMethods= superType.getMethods();
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMethodsToPullUp.length; i++) {
			IMethod method= fMethodsToPullUp[i];
			IMethod found= findMethod(method, superTypeMethods);
			if (found != null){
				result.addError("Method '" + method.getElementName() + "' (with the same signature) already exists in superclass '" + superType.getFullyQualifiedName() 
											+ "', which will result in compile errors if you proceed.",
											createContext(Refactoring.getResource(superType), found.getSourceRange()));
			} else {
				IMethod similar= Checks.findMethod(method, superType);
				if (similar != null)
					result.addWarning("Method '" + method.getElementName() + "' (with the same number of parameters) already exists in type '" 
														+ superType.getFullyQualifiedName() + "'",
														createContext(Refactoring.getResource(superType), similar.getSourceRange()));
			}	
		}
		return result;	
	}
	
	private RefactoringStatus checkMethodsInSubclasses(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		Set notDeletedMethods= getNotDeletedMethods();
		checkAccessModifiers(result, notDeletedMethods);
		checkReturnTypes(pm, result, notDeletedMethods);
		return result;
	}

	private void checkReturnTypes(IProgressMonitor pm, RefactoringStatus result, Set notDeletedMethods) throws JavaModelException {
		ITypeHierarchy hierarchy= getSuperType().newTypeHierarchy(new SubProgressMonitor(pm, 1));
		Map mapping= getMatchingMethodsMapping(hierarchy, hierarchy.getType());//IMethod -> Set of IMethods
		for (int i= 0; i < fMethodsToPullUp.length; i++) {
			String returnType= getReturnTypeName(fMethodsToPullUp[i]);
			for (Iterator iter= ((Set)mapping.get(fMethodsToPullUp[i])).iterator(); iter.hasNext();) {
				IMethod matchingMethod= (IMethod) iter.next();
				if (!notDeletedMethods.contains(matchingMethod))
					continue;
				if (returnType.equals(getReturnTypeName(matchingMethod)))
					continue;
				String msg= "Method '" + JavaElementUtil.createMethodSignature(matchingMethod) + "' declared in type'"
									 + matchingMethod.getDeclaringType().getFullyQualifiedName()
									 + "' has a different return type than its pulled up counterpart, which will result in compile errors if you proceed." ;
				result.addError(msg, createContext(Refactoring.getResource(matchingMethod), matchingMethod.getNameRange()));	
			}
		}
	}

	private void checkAccessModifiers(RefactoringStatus result, Set notDeletedMethods) throws JavaModelException {
		for (Iterator iter= notDeletedMethods.iterator(); iter.hasNext();) {
			IMethod method= (IMethod) iter.next();
			Context errorContext= createContext(Refactoring.getResource(method), method.getSourceRange());
			if (Flags.isStatic(method.getFlags())){
					String msg= "Method '" + JavaElementUtil.createMethodSignature(method) + "' declared in type '" 
										 + method.getDeclaringType().getFullyQualifiedName()
										 + "' is 'static', which will result in compile errors if you proceed." ;
					result.addError(msg, errorContext);
			 } 
			 if (isVisibilityLowerThanProtected(method)){
				String msg= "Method '" + JavaElementUtil.createMethodSignature(method)+ "' declared in type '" 
									 + method.getDeclaringType().getFullyQualifiedName()
									 + "' has visibility lower than 'protected', which will result in compile errors if you proceed." ;
				result.addError(msg, errorContext);	
			} 
		}
	}

	private String getReturnTypeName(IMethod method) throws JavaModelException {
		return Signature.toString(Signature.getReturnType(method.getSignature()).toString());
	}

	private Set getNotDeletedMethods() throws JavaModelException {
		Set matchingSet= new HashSet();
		matchingSet.addAll(Arrays.asList(getMatchingMethods()));
		matchingSet.removeAll(Arrays.asList(fMethodsToDelete));
		return matchingSet;
	}
	
	private static boolean isVisibilityLowerThanProtected(IMember member)throws JavaModelException {
		return ! (Flags.isPublic(member.getFlags()) || Flags.isProtected(member.getFlags()));
	}
	
	private static Context createContext(IResource resource, ISourceRange range){
		Assert.isTrue(resource instanceof IFile);
		return JavaSourceContext.create((ICompilationUnit)JavaCore.create(resource), range);
	}
	
	public IType getDeclaringType(){
		//all methods declared in same type - checked in precondition
		return fMethodsToPullUp[0].getDeclaringType(); //index safe - checked in constructor
	}
	
	public IType getSuperType() throws JavaModelException {
		IType declaringType= getDeclaringType();
		//FIX ME pm
		ITypeHierarchy st= declaringType.newSupertypeHierarchy(new NullProgressMonitor());
		return st.getSuperclass(declaringType);
	}
		
	//--- change creation
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 4);
			CompositeChange builder= new CompositeChange("Pull up methods");
			TextChangeManager manager= new TextChangeManager();
			
			addCopyMethodsChange(manager);
			pm.worked(1);
			
			if (! getSuperType().getCompilationUnit().equals(getDeclaringType().getCompilationUnit()))
				addAddImportsChange(manager, new SubProgressMonitor(pm, 1));
			pm.worked(1);
			
			addDeleteMethodsChange(manager);
			pm.worked(1);
			
			//putting it together
			builder.addAll(manager.getAllChanges());
			pm.worked(1);
			return builder;
		} catch(CoreException e){
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}
	}
	
	private void addCopyMethodsChange(TextChangeManager manager) throws CoreException {
		for (int i = fMethodsToPullUp.length - 1; i >= 0; i--) { //backwards - to preserve method order
			addCopyMethodChange(manager, fMethodsToPullUp[i]);
		}
	}
	
	private void addCopyMethodChange(TextChangeManager manager, IMethod method) throws CoreException {
		String methodSource= computeNewMethodSource(method);
		String changeName= "copy method '" + JavaElementUtil.createMethodSignature(method) 
										+ "' to type '" + getDeclaringType().getElementName() + "'";
		if (needsToChangeVisibility(method))
			changeName += " (changing its visibility to '"+ "protected" + "')";
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getSuperType().getCompilationUnit());
		manager.get(cu).addTextEdit(changeName, createAddMemberEdit(methodSource));
	}

	private TextEdit createAddMemberEdit(String methodSource) throws JavaModelException {
		int tabWidth=getTabWidth();
		IMethod sibling= getLastMethod(getSuperType());
		if (sibling != null)
			return new MemberEdit(sibling, MemberEdit.INSERT_AFTER, new String[]{ methodSource}, tabWidth);
		return
			new MemberEdit(getSuperType(), MemberEdit.ADD_AT_END, new String[]{ methodSource}, tabWidth);
	}

	private static int getTabWidth() {
		return Integer.parseInt((String)JavaCore.getOptions().get(PREF_TAB_SIZE));
	}
	
	private static boolean needsToChangeVisibility(IMethod method) throws JavaModelException {
		return ! (Flags.isPublic(method.getFlags()) || Flags.isProtected(method.getFlags()));
	}
	
	private static String computeNewMethodSource(IMethod method) throws JavaModelException {
		String source= replaceSuperCalls(method);

		if (! needsToChangeVisibility(method))
			return source;
			
		if (Flags.isPrivate(method.getFlags()))
			return substitutePrivateWithProtected(source);
		return "protected " + removeLeadingWhiteSpaces(source);
	}

	private static String replaceSuperCalls(IMethod method) throws JavaModelException {
		ISourceRange[] superRefOffsert= reverseSortByOffset(SuperReferenceFinder.findSuperReferenceRanges(method));
		
		StringBuffer source= new StringBuffer(SourceReferenceSourceRangeComputer.computeSource(method));
		ISourceRange originalMethodRange= SourceReferenceSourceRangeComputer.computeSourceRange(method, method.getCompilationUnit());
		
		for (int i= 0; i < superRefOffsert.length; i++) {
			int start= superRefOffsert[i].getOffset() - originalMethodRange.getOffset();
			int end= start + superRefOffsert[i].getLength();
			source.replace(start, end, "this");
		}
		return source.toString();
	}
	
	private static String removeLeadingWhiteSpaces(String s){
		if ("".equals(s))
			return "";

		if ("".equals(s.trim()))	
			return "";
			
		int i= 0;
		while (i < s.length() && Character.isWhitespace(s.charAt(i))){
			i++;
		}
		if (i == s.length())
			return "";
		return s.substring(i);
	}
	
	private static ISourceRange[] reverseSortByOffset(ISourceRange[] refs){
		Comparator comparator= new Comparator(){
			public int compare(Object o1, Object o2){
				return ((ISourceRange)o2).getOffset() - ((ISourceRange)o1).getOffset();
			}
		};
		Arrays.sort(refs, comparator);
		return refs;
	}
	
	private static String substitutePrivateWithProtected(String methodSource) throws JavaModelException {
		Scanner scanner= new Scanner();
		scanner.setSourceBuffer(methodSource.toCharArray());
		int offset= 0;
		int token= 0;
		try {
			while((token= scanner.getNextToken()) != TerminalSymbols.TokenNameEOF) {
				if (token == TerminalSymbols.TokenNameprivate) {
					offset= scanner.startPosition;
					break;
				}
			}
		} catch (InvalidInputException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.INVALID_CONTENTS);
		}
		//must do it this way - unicode
		int length= scanner.getCurrentTokenSource().length;
		return  new StringBuffer(methodSource).delete(offset, offset + length).insert(offset, "protected").toString();
	}
	
	private static IMethod getLastMethod(IType type) throws JavaModelException {
		if (type == null)
			return null;
		IMethod[] methods= type.getMethods();
		if (methods.length == 0)
			return null;
		return methods[methods.length - 1];	
	}
	
	private void addDeleteMethodsChange(TextChangeManager manager) throws CoreException {
		for (int i = 0; i < fMethodsToDelete.length; i++) {
			IMethod method = fMethodsToDelete[i];
			String name= "Delete method '"
								+ JavaElementUtil.createMethodSignature(method)	 
								+ "' declared in type '" + method.getDeclaringType().getFullyQualifiedName() + "'";
			DeleteSourceReferenceEdit edit= new DeleteSourceReferenceEdit(method, method.getCompilationUnit());
			manager.get(WorkingCopyUtil.getWorkingCopyIfExists(method.getCompilationUnit())).addTextEdit(name, edit);
		}
	}
	
	private void addAddImportsChange(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getSuperType().getCompilationUnit());		
		IType[] referencedTypes= getTypesReferencedInMethods(fMethodsToPullUp, pm);
		ImportEdit importEdit= new ImportEdit(cu, getPreferenceSetting());
		for (int i= 0; i < referencedTypes.length; i++) {
			IType iType= referencedTypes[i];
			importEdit.addImport(iType.getFullyQualifiedName());
		}
		manager.get(cu).addTextEdit("", importEdit);
	}
	
	private CodeGenerationSettings getPreferenceSetting(){
		return fPreferenceSettings;
	}

	//----- referenced types ----
		
	private static IType[] getTypesReferencedInMethods(IMethod[] methods, IProgressMonitor pm) throws JavaModelException {
		List referencedTypes= new ArrayList();
		pm.beginTask("", methods.length);
		for (int i = 0; i < methods.length; i++) {
			referencedTypes.addAll(getTypesReferencedInMethod(methods[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (IType[]) referencedTypes.toArray(new IType[referencedTypes.size()]);
	}
	
	private static List getTypesReferencedInMethod(IMethod method, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfReferencedTypes(ResourcesPlugin.getWorkspace(), method, collector);
		return extractElements(collector);
	}
	
	//----- referenced fields ----
	
	private static IField[] getFieldsReferencedInMethods(IMethod[] methods, IProgressMonitor pm) throws JavaModelException {
		
		List referencedFields= new ArrayList();
		pm.beginTask("", methods.length);
		for (int i = 0; i < methods.length; i++) {
			referencedFields.addAll(getFieldsReferencedInMethod(methods[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (IField[]) referencedFields.toArray(new IField[referencedFields.size()]);
	}
	
	private static List getFieldsReferencedInMethod(IMethod method, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfAccessedFields(ResourcesPlugin.getWorkspace(), method, collector);
		return extractElements(collector);
	}	
	
	//----- referenced methods ----
	
	private static IMethod[] getMethodsReferencedInMethods(IMethod[] methods, IProgressMonitor pm) throws JavaModelException {
		List referencedFields= new ArrayList();
		pm.beginTask("", methods.length);
		for (int i = 0; i < methods.length; i++) {
			referencedFields.addAll(getMethodsReferencedInMethod(methods[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (IMethod[]) referencedFields.toArray(new IMethod[referencedFields.size()]);
	}
	
	private static List getMethodsReferencedInMethod(IMethod method, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfSentMessages(ResourcesPlugin.getWorkspace(), method, collector);
		return extractElements(collector);
	}	
	
	private static List extractElements(SearchResultCollector collector){
		List elements= new ArrayList(collector.getResults().size());
		for (Iterator iter = collector.getResults().iterator(); iter.hasNext();) {
			elements.add(((SearchResult) iter.next()).getEnclosingElement());
		}
		return elements;		
	}
}

