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
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
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

public class PullUpRefactoring extends Refactoring {

	private IMember[] fElementsToPullUp;
	private IMethod[] fMethodsToDelete;
	private CodeGenerationSettings fPreferenceSettings;
	private static final String PREF_TAB_SIZE= "org.eclipse.jdt.core.formatter.tabulation.size";
	
	private IType fCachedSuperType;
	private ITypeHierarchy fCachedSuperTypeHierarchy;
	private IType fCachedDeclaringType;

	public PullUpRefactoring(IMember[] elements, CodeGenerationSettings preferenceSettings){
		Assert.isTrue(elements.length > 0);
		fElementsToPullUp= sortByOffset(elements);
		fMethodsToDelete= new IMethod[0];
		fPreferenceSettings= preferenceSettings;
	}
	
	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return "Pull Up";
	}
	
	/**
	 * Sets the methodsToDelete.
	 * @param methodsToDelete The methodsToDelete to set
	 * no validation is done on these methods - they will simply be removed. 
	 * it is the caller's resposibility to ensure that the selection makes sense from the behavior-preservation point of view.
	 */
	public void setMethodsToDelete(IMethod[] methodsToDelete) {
		Assert.isNotNull(methodsToDelete);
		fMethodsToDelete= getOriginals(methodsToDelete);
	}
	
	private IMember[] getMembersToDelete(IProgressMonitor pm){
		try{
			pm.beginTask("", 1);
			IMember[] matchingFields= getMembersOfType(getMatchingElements(new SubProgressMonitor(pm, 1)), IJavaElement.FIELD);
			return merge(getOriginals(matchingFields), fMethodsToDelete);
		} catch (JavaModelException e){
			//fallback
			return fMethodsToDelete;
		}	finally{
			pm.done();
		}
	}
	
	public IMember[] getElementsToPullUp() throws JavaModelException {
		return fElementsToPullUp;
	}
	
	public IType getDeclaringType(){
		if (fCachedDeclaringType != null)
			return fCachedDeclaringType;
		//all methods declared in same type - checked in precondition
		fCachedDeclaringType= fElementsToPullUp[0].getDeclaringType(); //index safe - checked in constructor
		return fCachedDeclaringType;
	}
	
	//XXX added for performance reasons
	public ITypeHierarchy getSuperTypeHierarchy(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 2);
			if (fCachedSuperTypeHierarchy != null)
				return fCachedSuperTypeHierarchy;
			fCachedSuperTypeHierarchy= getSuperType(new SubProgressMonitor(pm, 1)).newTypeHierarchy(new SubProgressMonitor(pm, 1));
			return fCachedSuperTypeHierarchy;
		} finally{
			pm.done();
		}	
	}
	
	public IType getSuperType(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 1);
			if (fCachedSuperType != null)
				return fCachedSuperType;
			IType declaringType= getDeclaringType();
			ITypeHierarchy st= declaringType.newSupertypeHierarchy(new SubProgressMonitor(pm, 1));
			fCachedSuperType= st.getSuperclass(declaringType);
			return fCachedSuperType;
		} finally{
			pm.done();
		}			
	}
	
	public IMember[] getMatchingElements(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 2);
			Set result= new HashSet();
			IType superType= getSuperType(new SubProgressMonitor(pm, 1));
			ITypeHierarchy hierarchy= getSuperTypeHierarchy(new SubProgressMonitor(pm, 1));
			IType[] subTypes= hierarchy.getAllSubtypes(superType);
			for (int i = 0; i < subTypes.length; i++) {
				result.addAll(getMatchingMembers(hierarchy, subTypes[i]));
			}
			return (IMember[]) result.toArray(new IMember[result.size()]);
		} finally{
			pm.done();
		}				
	}
	
	//Set of IMembers
	private Set getMatchingMembers(ITypeHierarchy hierarchy, IType type) throws JavaModelException {
		Set result= new HashSet();
		Map mapping= getMatchingMembersMapping(hierarchy, type);
		for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
			result.addAll((Set)mapping.get(iter.next()));
		}
		return result;
	}

	/*
	 * mapping: Map: IMember -> Set of IMembers (of the same type as key)
	 */	
	private static void addToMapping(Map mapping, IMember key, IMember matchingMethod){
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
	 * mapping: Map: IMember -> Set of IMembers (of the same type as key)
	 */
	private static void addToMapping(Map mapping, IMember key, Set matchingMembers){
		for (Iterator iter= matchingMembers.iterator(); iter.hasNext();) {
			IMember member= (IMember) iter.next();
			Assert.isTrue(key.getElementType() == member.getElementType());
			addToMapping(mapping, key, member);
		}
	}
	
	private Map getMatchingMembersMapping(ITypeHierarchy hierarchy, IType type) throws JavaModelException {
		Map result= new HashMap();//IMember -> Set of IMembers (of the same type as key)
		
		//current type
		for (int i = 0; i < fElementsToPullUp.length; i++) {
			if (fElementsToPullUp[i].getElementType() == IJavaElement.METHOD){
				IMethod method= (IMethod)fElementsToPullUp[i];
				IMethod found= findMethod(method, type.getMethods());
				if (found != null)
					addToMapping(result, method, found);
			} else if (fElementsToPullUp[i].getElementType() == IJavaElement.FIELD){
				IField field= (IField)fElementsToPullUp[i];
				IField found= type.getField(field.getElementName());
				if (found.exists())
					addToMapping(result, field, found);
			}
		}
		
		//subtypes
		IType[] subTypes= hierarchy.getAllSubtypes(type);
		for (int i = 0; i < subTypes.length; i++) {
			IType subType = subTypes[i];
			Map subTypeMapping= getMatchingMembersMapping(hierarchy, subType);
			for (Iterator iter= subTypeMapping.keySet().iterator(); iter.hasNext();) {
				IMember m= (IMember) iter.next();
				addToMapping(result, m, (Set)subTypeMapping.get(m));
			}
		}
		return result;		
	}
		
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm)	throws JavaModelException {
		try {
			RefactoringStatus result= new RefactoringStatus();
			pm.beginTask("", 3);
						
			result.merge(checkAllElements());
			pm.worked(1);
			if (result.hasFatalError())
				return result;
			
			if (! haveCommonDeclaringType())
				return RefactoringStatus.createFatalErrorStatus("All selected elements must be declared in the same type.");			
				
			pm.worked(1);
			if (result.hasFatalError())
				return result;			

			result.merge(checkDeclaringType());
			pm.worked(1);
			if (result.hasFatalError())
				return result;			

			if (getSuperType(new NullProgressMonitor()) == null)
				return RefactoringStatus.createFatalErrorStatus("Pull up not allowed.");
			if (getSuperType(new NullProgressMonitor()).isBinary())
				return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on elements declared in subtypes of binary types.");

			fElementsToPullUp= getOriginals(fElementsToPullUp);		
			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}
	
	private static IMethod[] getOriginals(IMethod[] methods){
		IMethod[] result= new IMethod[methods.length];
		for (int i= 0; i < methods.length; i++) {
			result[i]= (IMethod)WorkingCopyUtil.getOriginal(methods[i]);
		}
		return result;
	}
	
	private static IMember[] getOriginals(IMember[] members){
		IMember[] result= new IMember[members.length];
		for (int i= 0; i < members.length; i++) {
			result[i]= (IMember)WorkingCopyUtil.getOriginal(members[i]);
		}
		return result;
	}
	
	private static IMember[] merge(IMember[] a1, IMember[] a2){
		Set result= new HashSet(a1.length + a2.length);
		result.addAll(Arrays.asList(a1));
		result.addAll(Arrays.asList(a2));
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}
	
	private static IMember[] getMembersOfType(IMember[] members, int type){
		List list= Arrays.asList(JavaElementUtil.getElementsOfType(members, type));
		return (IMember[]) list.toArray(new IMember[list.size()]);
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
			result.merge(checkMembersInSubclasses(new SubProgressMonitor(pm, 1)));
			return result;
		} finally {
			pm.done();
		}	
	}

	//--- private helpers
	
	private RefactoringStatus checkAllElements() throws JavaModelException {
		//just 1 error message
		for (int i = 0; i < fElementsToPullUp.length; i++) {
			IMember member = fElementsToPullUp[i];

			if (member.getElementType() != IJavaElement.METHOD && 
				member.getElementType() != IJavaElement.FIELD)
					return RefactoringStatus.createFatalErrorStatus("Pull up is allowed only on fields and methods.");			
			if (! member.exists())
				return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on elements that do not exist.");			
	
			if (member.isBinary())
				return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on binary elements.");	

			if (member.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on read-only meelementsthods.");					

			if (! member.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on elements with unknown structure.");					

			if (Flags.isStatic(member.getFlags())) //for now
				return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on static elements.");
			
			if (member.getElementType() == IJavaElement.METHOD)
				return checkMethod((IMethod)member);
		}
		return null;
	}
	
	private static RefactoringStatus checkMethod(IMethod method) throws JavaModelException {
		if (method.isConstructor())
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on constructors.");			
			
		if (Flags.isAbstract(method.getFlags()))
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on abstract methods.");
			
 		if (Flags.isNative(method.getFlags())) //for now - move to input preconditions
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on abstract methods.");				

		return null;	
	}
	
	private RefactoringStatus checkDeclaringType() throws JavaModelException {
		IType declaringType= getDeclaringType();
				
		if (declaringType.isInterface()) //for now
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on interface methods.");
		
		if (declaringType.getFullyQualifiedName().equals("java.lang.Object"))
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on methods declared in java.lang.Object.");	

		if (declaringType.isBinary())
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on methods in subclasses of binary types.");	

		if (declaringType.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus("Pull up is not allowed on methods in subclasses of read-only types.");	
		
		return null;
	}
	
	private boolean haveCommonDeclaringType(){
		IType declaringType= fElementsToPullUp[0].getDeclaringType(); //index safe - checked in constructor
		for (int i= 0; i < fElementsToPullUp.length; i++) {
			if (! declaringType.equals(fElementsToPullUp[i].getDeclaringType()))
				return false;			
		}	
		return true;
	}
	
	private RefactoringStatus checkAccesses(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("Checking referenced elements", 3);
		result.merge(checkAccessedTypes(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedFields(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedMethods(new SubProgressMonitor(pm, 1)));
		return result;
	}
	
	private RefactoringStatus checkAccessedTypes(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= getTypesReferencedIn(fElementsToPullUp, pm);
		IType superType= getSuperType(new NullProgressMonitor());
		for (int i= 0; i < accessedTypes.length; i++) {
			IType iType= accessedTypes[i];
			if (! canBeAccessedFrom(iType, superType)){
				String msg= "Type '" + iType.getFullyQualifiedName() + "' referenced in one of the pulled elements is not accessible from type '" 
								+ superType.getFullyQualifiedName() + "'.";
				result.addError(msg, JavaSourceContext.create(iType));
			}	
		}
		return result;
	}
	
	private RefactoringStatus checkAccessedFields(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		
		List pulledUpList= Arrays.asList(fElementsToPullUp);
		List deletedList= Arrays.asList(getMembersToDelete(new NullProgressMonitor()));
		IField[] accessedFields= getFieldsReferencedIn(fElementsToPullUp, pm);
		
		IType superType= getSuperType(new NullProgressMonitor());
		for (int i= 0; i < accessedFields.length; i++) {
			IField iField= accessedFields[i];
			if (! canBeAccessedFrom(iField, superType) && ! pulledUpList.contains(iField) && !deletedList.contains(iField)){
				String msg= "Field '" + iField.getElementName() + "' referenced in one of the pulled elements is not accessible from type '" 
								+ superType.getFullyQualifiedName() + "'.";
				result.addError(msg, JavaSourceContext.create(iField));
			}	
		}
		return result;
	}

	private RefactoringStatus checkAccessedMethods(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		
		List pulledUpList= Arrays.asList(fElementsToPullUp);
		List deletedList= Arrays.asList(getMembersToDelete(new NullProgressMonitor()));
		IMethod[] accessedMethods= getMethodsReferencedIn(fElementsToPullUp, pm);
		
		IType superType= getSuperType(new NullProgressMonitor());
		for (int i= 0; i < accessedMethods.length; i++) {
			IMethod iMethod= accessedMethods[i];
			if (! canBeAccessedFrom(iMethod, superType) && ! pulledUpList.contains(iMethod) && !deletedList.contains(iMethod)){
				String msg= "Method '" + JavaElementUtil.createMethodSignature(iMethod) + "' referenced in one of the pulled elements is not accessible from type '" 
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
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fElementsToPullUp.length; i++) {
			if (fElementsToPullUp[i].getElementType() == IJavaElement.METHOD)
				checkMethodInSuperclass(getSuperType(new NullProgressMonitor()), result, (IMethod)fElementsToPullUp[i]);
			else 
				checkFieldInSuperclass(getSuperType(new NullProgressMonitor()), result, (IField)fElementsToPullUp[i]);
		}
		return result;	
	}

	private void checkMethodInSuperclass(IType superType, RefactoringStatus result, IMethod method) throws JavaModelException {
		IMethod[] superTypeMethods= superType.getMethods();
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
	
	private void checkFieldInSuperclass(IType superType, RefactoringStatus result, IField field) throws JavaModelException {
		IField superTypeField= superType.getField(field.getElementName());	
		if (! superTypeField.exists())
			return;
		result.addError("Field '" + field.getElementName() + "' already exists in superclass '" + superType.getFullyQualifiedName() 
									+ "', which will result in compile errors if you proceed.",
									createContext(Refactoring.getResource(superType), superTypeField.getSourceRange()));
	}
			
	private RefactoringStatus checkMembersInSubclasses(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		Set notDeletedMembers= getNotDeletedMembers();
		checkAccessModifiers(result, notDeletedMembers);
		checkMethodReturnTypes(pm, result, notDeletedMembers);
		checkFieldTypes(pm, result);
		return result;
	}

	private void checkMethodReturnTypes(IProgressMonitor pm, RefactoringStatus result, Set notDeletedMembers) throws JavaModelException {
		ITypeHierarchy hierarchy= getSuperTypeHierarchy(new SubProgressMonitor(pm, 1));
		Map mapping= getMatchingMembersMapping(hierarchy, hierarchy.getType());//IMember -> Set of IMembers
		for (int i= 0; i < fElementsToPullUp.length; i++) {
			if (fElementsToPullUp[i].getElementType() != IJavaElement.METHOD)
				continue;
			IMethod method= (IMethod)fElementsToPullUp[i];
			String returnType= getReturnTypeName(method);
			for (Iterator iter= ((Set)mapping.get(method)).iterator(); iter.hasNext();) {
				IMethod matchingMethod= (IMethod) iter.next();
				if (!notDeletedMembers.contains(matchingMethod))
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

	private void checkFieldTypes(IProgressMonitor pm, RefactoringStatus result) throws JavaModelException {
		ITypeHierarchy hierarchy= getSuperTypeHierarchy(new SubProgressMonitor(pm, 1));
		Map mapping= getMatchingMembersMapping(hierarchy, hierarchy.getType());//IMember -> Set of IMembers
		for (int i= 0; i < fElementsToPullUp.length; i++) {
			if (fElementsToPullUp[i].getElementType() != IJavaElement.FIELD)
				continue;
			IField field= (IField)fElementsToPullUp[i];
			String type= getTypeName(field);
			for (Iterator iter= ((Set)mapping.get(field)).iterator(); iter.hasNext();) {
				IField matchingField= (IField) iter.next();
				if (type.equals(getTypeName(matchingField)))
					continue;
				String msg= "Field '" + matchingField.getElementName() + "' declared in type'"
									 + matchingField.getDeclaringType().getFullyQualifiedName()
									 + "' has a different type than its pulled up counterpart." ;
				result.addError(msg, createContext(Refactoring.getResource(matchingField), matchingField.getSourceRange()));	
			}
		}
	}

	private void checkAccessModifiers(RefactoringStatus result, Set notDeletedMembers) throws JavaModelException {
		for (Iterator iter= notDeletedMembers.iterator(); iter.hasNext();) {
			IMember member= (IMember) iter.next();
			
			if (member.getElementType() == IJavaElement.METHOD)
				checkMethodAccessModifiers(result, ((IMethod) member));
		}
	}
	
	private void checkMethodAccessModifiers(RefactoringStatus result, IMethod method) throws JavaModelException {
		Context errorContext= createContext(method);
		
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

	private static String getReturnTypeName(IMethod method) throws JavaModelException {
		return Signature.toString(Signature.getReturnType(method.getSignature()).toString());
	}
	
	private static String getTypeName(IField field) throws JavaModelException {
		return Signature.toString(field.getTypeSignature());
	}

	private Set getNotDeletedMembers() throws JavaModelException {
		Set matchingSet= new HashSet();
		matchingSet.addAll(Arrays.asList(getMatchingElements(new NullProgressMonitor())));
		matchingSet.removeAll(Arrays.asList(getMembersToDelete(new NullProgressMonitor())));
		return matchingSet;
	}
	
	private static IMember[] sortByOffset(IMember[] methods){
		Comparator comparator= new Comparator(){
			public int compare(Object o1, Object o2){
				try{
					return ((ISourceReference)o2).getSourceRange().getOffset() - ((ISourceReference)o1).getSourceRange().getOffset();
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
	
	private static boolean isVisibilityLowerThanProtected(IMember member)throws JavaModelException {
		return ! (Flags.isPublic(member.getFlags()) || Flags.isProtected(member.getFlags()));
	}
	
	private static Context createContext(IMember member) throws JavaModelException {
		return createContext(Refactoring.getResource(member), member.getSourceRange());
	}
	
	private static Context createContext(IResource resource, ISourceRange range){
		Assert.isTrue(resource instanceof IFile);
		return JavaSourceContext.create((ICompilationUnit)JavaCore.create(resource), range);
	}

	
	//--- change creation
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 4);
			
			TextChangeManager manager= new TextChangeManager();
			
			addCopyMembersChange(manager);
			pm.worked(1);
			
			if (! getSuperType(new NullProgressMonitor()).getCompilationUnit().equals(getDeclaringType().getCompilationUnit()))
				addAddImportsChange(manager, new SubProgressMonitor(pm, 1));
			pm.worked(1);
			
			addDeleteMembersChange(manager);
			pm.worked(1);
			
			pm.worked(1);
			return new CompositeChange("Pull Up", manager.getAllChanges());
		} catch(CoreException e){
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}
	}
	
	private void addCopyMembersChange(TextChangeManager manager) throws CoreException {
		for (int i = fElementsToPullUp.length - 1; i >= 0; i--) { //backwards - to preserve method order
			addCopyChange(manager, fElementsToPullUp[i]);
		}
	}
	
	private void addCopyChange(TextChangeManager manager, IMember member) throws CoreException {
		String source= computeNewSource(member);
		String changeName= getCopyChangeName(member);
									
		if (needsToChangeVisibility(member))
			changeName += " (changing its visibility to '"+ "protected" + "')";
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getSuperType(new NullProgressMonitor()).getCompilationUnit());
		manager.get(cu).addTextEdit(changeName, createAddMemberEdit(source));
	}

	private String getCopyChangeName(IMember member) throws JavaModelException {
		if (member.getElementType() == IJavaElement.METHOD)
			return "copy method '" + JavaElementUtil.createMethodSignature((IMethod)member) 
										+ "' to type '" + getDeclaringType().getElementName() + "'";
		
		else return "copy field '" + member.getElementName() 
										+ "' to type '" + getDeclaringType().getElementName() + "'";
	}
		
	private TextEdit createAddMemberEdit(String methodSource) throws JavaModelException {
		int tabWidth=getTabWidth();
		IMethod sibling= getLastMethod(getSuperType(new NullProgressMonitor()));
		if (sibling != null)
			return new MemberEdit(sibling, MemberEdit.INSERT_AFTER, new String[]{ methodSource}, tabWidth);
		return
			new MemberEdit(getSuperType(new NullProgressMonitor()), MemberEdit.ADD_AT_END, new String[]{ methodSource}, tabWidth);
	}

	private void addDeleteMembersChange(TextChangeManager manager) throws CoreException {
		IMember[] membersToDelete= getMembersToDelete(new NullProgressMonitor());
		for (int i = 0; i < membersToDelete.length; i++) {
			String changeName= getDeleteChangeName(membersToDelete[i]);
			DeleteSourceReferenceEdit edit= new DeleteSourceReferenceEdit(membersToDelete[i], membersToDelete[i].getCompilationUnit());
			manager.get(WorkingCopyUtil.getWorkingCopyIfExists(membersToDelete[i].getCompilationUnit())).addTextEdit(changeName, edit);
		}
	}
	
	private static String getDeleteChangeName(IMember member) throws JavaModelException{
		if (member.getElementType() == IJavaElement.METHOD){
			IMethod method = (IMethod)member;
			return "Delete method '"
						+ JavaElementUtil.createMethodSignature(method)	 
						+ "' declared in type '" + method.getDeclaringType().getFullyQualifiedName() + "'";
		} else {
			return "Delete field '"
						+ member.getElementName()	 
						+ "' declared in type '" + member.getDeclaringType().getFullyQualifiedName() + "'";	
		}
	}
	
	private void addAddImportsChange(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getSuperType(new NullProgressMonitor()).getCompilationUnit());		
		IType[] referencedTypes= getTypesReferencedIn(fElementsToPullUp, pm);
		ImportEdit importEdit= new ImportEdit(cu, fPreferenceSettings);
		for (int i= 0; i < referencedTypes.length; i++) {
			IType iType= referencedTypes[i];
			importEdit.addImport(iType.getFullyQualifiedName());
		}
		manager.get(cu).addTextEdit("", importEdit);
	}

	private static int getTabWidth() {
		return Integer.parseInt((String)JavaCore.getOptions().get(PREF_TAB_SIZE));
	}
	
	private static boolean needsToChangeVisibility(IMember method) throws JavaModelException {
		return ! (Flags.isPublic(method.getFlags()) || Flags.isProtected(method.getFlags()));
	}
	
	private static String computeNewSource(IMember member) throws JavaModelException {
		String source;
		
		if (member.getElementType() == IJavaElement.METHOD)
			source= replaceSuperCalls((IMethod)member);
		else
			source= SourceReferenceSourceRangeComputer.computeSource(member);

		if (! needsToChangeVisibility(member))
			return source;
			
		if (Flags.isPrivate(member.getFlags()))
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
		
	//----- referenced types ----
		
	private static IType[] getTypesReferencedIn(IMember[] elements, IProgressMonitor pm) throws JavaModelException {
		List referencedTypes= new ArrayList();
		pm.beginTask("", elements.length);
		for (int i = 0; i < elements.length; i++) {
			referencedTypes.addAll(getTypesReferencedIn(elements[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (IType[]) referencedTypes.toArray(new IType[referencedTypes.size()]);
	}
	
	private static List getTypesReferencedIn(IMember element, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfReferencedTypes(ResourcesPlugin.getWorkspace(), element, collector);
		return extractElements(collector);
	}
	
	//----- referenced fields ----
	
	private static IField[] getFieldsReferencedIn(IMember[] elements, IProgressMonitor pm) throws JavaModelException {
		List referencedFields= new ArrayList();
		pm.beginTask("", elements.length);
		for (int i = 0; i < elements.length; i++) {
			referencedFields.addAll(getFieldsReferencedIn(elements[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (IField[]) referencedFields.toArray(new IField[referencedFields.size()]);
	}
	
	private static List getFieldsReferencedIn(IMember element, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfAccessedFields(ResourcesPlugin.getWorkspace(), element, collector);
		return extractElements(collector);
	}	
	
	//----- referenced methods ----
	
	private static IMethod[] getMethodsReferencedIn(IMember[] elements, IProgressMonitor pm) throws JavaModelException {
		List referencedFields= new ArrayList();
		pm.beginTask("", elements.length);
		for (int i = 0; i < elements.length; i++) {
			referencedFields.addAll(getMethodsReferencedIn(elements[i], new SubProgressMonitor(pm, 1)));
		}
		pm.done();
		return (IMethod[]) referencedFields.toArray(new IMethod[referencedFields.size()]);
	}
	
	private static List getMethodsReferencedIn(IMember element, IProgressMonitor pm) throws JavaModelException {
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().searchDeclarationsOfSentMessages(ResourcesPlugin.getWorkspace(), element, collector);
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

