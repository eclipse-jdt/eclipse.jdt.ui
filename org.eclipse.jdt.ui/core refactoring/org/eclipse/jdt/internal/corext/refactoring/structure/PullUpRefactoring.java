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
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;


public class PullUpRefactoring extends HierarchyRefactoring {

	private IMethod[] 	fMethodsToDeclareAbstract;
	private IMethod[] 	fMethodsToDelete;
	private IType fTargetType;
	private boolean fCreateMethodStubs;
	
	//caches
	private ITypeHierarchy fCachedTargetClassHierarchy;
	private Set fCachedSkippedSuperclasses;//Set<IType>
	private final Map fCachedMembersReferences; //Map<IMember, SearchResultGroup[]>

	private PullUpRefactoring(IMember[] elements, CodeGenerationSettings preferenceSettings){
		super(elements, preferenceSettings);

		fMethodsToDelete= new IMethod[0];
		fMethodsToDeclareAbstract= new IMethod[0];
		fCreateMethodStubs= true;
		fCachedMembersReferences= new HashMap(2);
	}
	
	public static PullUpRefactoring create(IMember[] members, CodeGenerationSettings preferenceSettings) throws JavaModelException{
		if (! isAvailable(members))
			return null;
		if (isOneTypeWithPullableMembers(members)) {
			PullUpRefactoring result= new PullUpRefactoring(new IMember[0], preferenceSettings);
			result.fCachedDeclaringType= getSingleTopLevelType(members);
			return result;
		}
		return new PullUpRefactoring(members, preferenceSettings);
	}
	
	public static boolean isAvailable(IMember[] members) throws JavaModelException{
		if (isOneTypeWithPullableMembers(members))
			return true;
		return 	members != null && 
			   	members.length != 0 &&
				areAllPullable(members) &&
				haveCommonDeclaringType(members);
	}
	
	private static boolean isOneTypeWithPullableMembers(IMember[] members) throws JavaModelException {
		IType singleTopLevelType= getSingleTopLevelType(members);
		return (singleTopLevelType != null && getPullableMembers(singleTopLevelType).length != 0);
	}

	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("PullUpRefactoring.Pull_Up"); //$NON-NLS-1$
	}
	
	/**
	 * Sets the methodsToDelete.
	 * @param methodsToDelete The methodsToDelete to set
	 * no validation is done on these methods - they will simply be removed. 
	 * it is the caller's responsibility to ensure that the selection makes sense from the behavior-preservation point of view.
	 */
	public void setMethodsToDelete(IMethod[] methodsToDelete) {
		Assert.isNotNull(methodsToDelete);
		fMethodsToDelete= getOriginals(methodsToDelete);
	}

	/*
	* no validation is done here - the members must be a subset of those
	* returned by the call to getPullableMembersOfDeclaringType
	*/
	public void setMethodsToDeclareAbstract(IMethod[] methods) {
		Assert.isNotNull(methods);		
		fMethodsToDeclareAbstract= getOriginals(methods);
	}

	/*
	* no validation is done here - the members must be a subset of those
	* returned by the call to getPullableMembersOfDeclaringType
	*/
	public void setMembersToMove(IMember[] elements){
		Assert.isNotNull(elements);
		fMembersToMove= (IMember[])SourceReferenceUtil.sortByOffset(elements);
		fMembersToMove= WorkingCopyUtil.getOriginals(fMembersToMove);
	}
	
	private IMember[] getMembersToDelete(IProgressMonitor pm) throws JavaModelException{
		try{
			IMember[] typesToDelete= WorkingCopyUtil.getOriginals(getMembersOfType(fMembersToMove, IJavaElement.TYPE));
			IMember[] matchingElements= getMatchingElements(pm, false);
			IMember[] matchingFields= WorkingCopyUtil.getOriginals(getMembersOfType(matchingElements, IJavaElement.FIELD));
			return merge(matchingFields, typesToDelete, fMethodsToDelete);
		} finally{
			pm.done();
		}
	}

	public void setCreateMethodStubs(boolean create) {
		fCreateMethodStubs= create;
	}
	
	public boolean getCreateMethodStubs() {
		return fCreateMethodStubs;
	}

	public IMember[] getPullableMembersOfDeclaringType() {
		try{
			return getPullableMembers(getDeclaringType());
		} catch (JavaModelException e){
			return new IMember[0];
		}	
	}
	
	private static IMember[] getPullableMembers(IType type) throws JavaModelException {
		List list= new ArrayList(3);
		addAllPullable(type.getFields(), list);
		addAllPullable(type.getMethods(), list);
		addAllPullable(type.getTypes(), list);
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}

	private static void addAllPullable(IMember[] members, List list) throws JavaModelException{
		for (int i= 0; i < members.length; i++) {
			if (isPullable(members[i]))
				list.add(members[i]);
		}	
	}
	private static boolean isPullable(IMember member) throws JavaModelException {
		if (member.getElementType() != IJavaElement.METHOD && 
			member.getElementType() != IJavaElement.FIELD &&
			member.getElementType() != IJavaElement.TYPE)
				return false;
		
		if (! Checks.isAvailable(member))
			return false;
	
		if (member instanceof IType){
			if (! JdtFlags.isStatic(member))
				return false;
		}
		if (member instanceof IMethod ){
			IMethod method= (IMethod) member;
			if (method.isConstructor())
				return false;
			
			if (JdtFlags.isNative(method)) //for now - move to input preconditions
				return false;
		}

		return true;	
	}
	
	public ITypeHierarchy getTypeHierarchyOfTargetClass(IProgressMonitor pm) throws JavaModelException {
		try{
			if (fCachedTargetClassHierarchy != null && fCachedTargetClassHierarchy.getType().equals(getTargetClass()))
				return fCachedTargetClassHierarchy;
			fCachedTargetClassHierarchy= getTargetClass().newTypeHierarchy(pm);
			return fCachedTargetClassHierarchy;
		} finally{
			pm.done();
		}	
	}
	
	public IType[] getPossibleTargetClasses(IProgressMonitor pm) throws JavaModelException {
		return getPossibleTargetClasses(new RefactoringStatus(), pm);
	}
	
	private IType[] getPossibleTargetClasses(RefactoringStatus status, IProgressMonitor pm) throws JavaModelException {
		IType[] superClasses= getDeclaringType().newSupertypeHierarchy(pm).getAllSuperclasses(getDeclaringType());
		List superClassList= new ArrayList(superClasses.length);
		int binary= 0;
		for (int i= 0; i < superClasses.length; i++) {
			IType superclass= superClasses[i];
			if (isPossibleTargetClass(superclass)) {
				superClassList.add(superclass);
			} else {
				if (superclass != null && superclass.isBinary()) {
					binary++;
				}
			}
		}
		if (superClasses.length == 1 && superClasses[0].getFullyQualifiedName().equals("java.lang.Object")) //$NON-NLS-1$
			status.addFatalError(RefactoringCoreMessages.getString("PullUPRefactoring.not_java_lang_object")); //$NON-NLS-1$
		else if (superClasses.length == binary)
			status.addFatalError(RefactoringCoreMessages.getString("PullUPRefactoring.no_all_binary")); //$NON-NLS-1$

		Collections.reverse(superClassList);
		return (IType[]) superClassList.toArray(new IType[superClassList.size()]);
	}
	
	private boolean isPossibleTargetClass(IType clazz) {
		return clazz != null && clazz.exists() && ! clazz.isReadOnly() && ! clazz.isBinary() && ! "java.lang.Object".equals(clazz.getFullyQualifiedName()); //$NON-NLS-1$
	}
	
	public IType getTargetClass(){
		return fTargetType;
	}
	
	public void setTargetClass(IType targetType){
		Assert.isNotNull(targetType);
		if (! targetType.equals(fTargetType))
			fCachedTargetClassHierarchy= null;
		fTargetType= targetType;
	}
	
	/* @return matching elements of in targetClass and subtypes of targetClass */
	public IMember[] getMatchingElements(IProgressMonitor pm, boolean includeMethodsToDeclareAbstract) throws JavaModelException {
		try{	
			Set result= new HashSet();
			IType targetClass= getTargetClass();
			Map matching= getMatchingMemberMatching(pm, includeMethodsToDeclareAbstract);
			for (Iterator iter= matching.keySet().iterator(); iter.hasNext();) {
				IMember	key= (IMember) iter.next();
				if (key.getDeclaringType().equals(targetClass))
					iter.remove(); //TODO: can remove this, since line is never reached (asserted by construction of keySet)
				else	
					result.addAll((Set)matching.get(key));
			}
			return (IMember[]) result.toArray(new IMember[result.size()]);
		} finally{
			pm.done();
		}				
	}
	
	public IMember[] getAdditionalRequiredMembersToPullUp(IProgressMonitor pm) throws JavaModelException{
		IMember[] members= getMembersToBeCreatedInTargetClass();
		pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.calculating_required"), members.length);//not true, but not easy to give anything better //$NON-NLS-1$
		List queue= new ArrayList(members.length);
		queue.addAll(Arrays.asList(members));
		if (queue.isEmpty())
			return new IMember[0];
		int i= 0;
		IMember current;
		do{
			current= (IMember)queue.get(i);
			addAllRequiredPullableMembers(queue, current, new SubProgressMonitor(pm, 1));
			i++;
			if (queue.size() == i)
				current= null;
		} while(current != null);
		queue.removeAll(Arrays.asList(members));//report only additional
		return (IMember[]) queue.toArray(new IMember[queue.size()]);
	}
	
	private void addAllRequiredPullableMembers(List queue, IMember member, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 3); //$NON-NLS-1$
		addAllRequiredPullableMethods(queue, member, new SubProgressMonitor(pm, 1));
		addAllRequiredPullableFields(queue, member, new SubProgressMonitor(pm, 1));
		addAllRequiredPullableTypes(queue, member, new SubProgressMonitor(pm, 1));
		pm.done();
	}
	
	private void addAllRequiredPullableTypes(List queue, IMember member, IProgressMonitor pm) throws JavaModelException {
		IType[] requiredTypes= ReferenceFinderUtil.getTypesReferencedIn(new IJavaElement[]{member}, pm);
		boolean isStatic= JdtFlags.isStatic(member);
		for (int i= 0; i < requiredTypes.length; i++) {
			IType requiredType= requiredTypes[i];
			if (isStatic && ! JdtFlags.isStatic(requiredType))
				continue;			
			if (isRequiredPullableMember(queue, requiredType))
				queue.add(requiredType);
		}
	}

	private void addAllRequiredPullableFields(List queue, IMember member, IProgressMonitor pm) throws JavaModelException {
		IField[] requiredFields= ReferenceFinderUtil.getFieldsReferencedIn(new IJavaElement[]{member}, pm);
		boolean isStatic= JdtFlags.isStatic(member);
		for (int i= 0; i < requiredFields.length; i++) {
			IField requiredField= requiredFields[i];
			if (isStatic && ! JdtFlags.isStatic(requiredField))
				continue;			
			if (isRequiredPullableMember(queue, requiredField))
				queue.add(requiredField);
		}
	}
	
	private void addAllRequiredPullableMethods(List queue, IMember member, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		IMethod[] requiredMethods= ReferenceFinderUtil.getMethodsReferencedIn(new IJavaElement[]{member}, new SubProgressMonitor(pm, 1));
		SubProgressMonitor sPm= new SubProgressMonitor(pm, 1);
		sPm.beginTask("", requiredMethods.length); //$NON-NLS-1$
		boolean isStatic= JdtFlags.isStatic(member);
		for (int i= 0; i < requiredMethods.length; i++) {
			IMethod requiredMethod= requiredMethods[i];
			if (isStatic && ! JdtFlags.isStatic(requiredMethod))
				continue;
			if (isRequiredPullableMember(queue, requiredMethod) && ! isVirtualAccessibleFromTargetClass(requiredMethod, new SubProgressMonitor(sPm, 1)))
				queue.add(requiredMethod);
		}
		sPm.done();
	}
	
	private boolean isVirtualAccessibleFromTargetClass(IMethod method, IProgressMonitor pm) throws JavaModelException {
		try{
			return MethodChecks.isVirtual(method) && isDeclaredInTargetClassOrItsSuperclass(method, pm);
		} finally {
			pm.done();
		}
	}
	
	private boolean isDeclaredInTargetClassOrItsSuperclass(IMethod method, IProgressMonitor pm) throws JavaModelException {
		try{
			boolean isConstructor= false;
			String[] paramTypes= method.getParameterTypes();
			String name= method.getElementName();
			IType targetClass= getTargetClass();
			ITypeHierarchy hierarchy= getTypeHierarchyOfTargetClass(pm);
			IMethod first= JavaModelUtil.findMethod(name, paramTypes, isConstructor, targetClass);
			if (first != null && MethodChecks.isVirtual(first))
				return true;
			IMethod found= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, targetClass, name, paramTypes, isConstructor);
			return found != null && MethodChecks.isVirtual(found);
		} finally{
			pm.done();
		}	
	}
	
	private boolean isRequiredPullableMember(List queue, IMember member) throws JavaModelException {
		if (member.getDeclaringType() == null) //not a member
			return false; 
		return member.getDeclaringType().equals(getDeclaringType()) && ! queue.contains(member) && isPullable(member);
	}
		
	/*
	 * mapping: Map: IMember -> Set of IMembers (of the same type as key)
	 */	
	private static void addToMapping(Map mapping, IMember key, IMember matchingMember){
		Set matchingSet;
		if (mapping.containsKey(key)){
			matchingSet= (Set)mapping.get(key);
		}else{
			matchingSet= new HashSet();
			mapping.put(key, matchingSet);
		}
		Assert.isTrue(! matchingSet.contains(matchingMember));
		matchingSet.add(matchingMember);
	}
	
	private Map getMatchingMembersMappingFromTypeAndAllSubtypes(ITypeHierarchy hierarchy, IType type, boolean includeMethodsToDeclareAbstract) throws JavaModelException {
		Map result= new HashMap(); //IMember -> Set of IMembers (of the same type as key)
		result.putAll(getMatchingMembersMapping(type));
		IType[]  subTypes= hierarchy.getAllSubtypes(type);
		for (int i = 0; i < subTypes.length; i++) {
			mergeSets(result, getMatchingMembersMapping(subTypes[i]));
		}
		if (includeMethodsToDeclareAbstract)
			return result;
			
		for (int i= 0; i < fMethodsToDeclareAbstract.length; i++) {
			if (result.containsKey(fMethodsToDeclareAbstract[i]))
				result.remove(fMethodsToDeclareAbstract[i]);
		}
		return result;
	}
	
	/*
	 * result: IMember -> Set 
	 * map: IMember -> Set
	 * this method merges sets for common keys and adds entries to result for
	 * those keys that exist only in map
	 */
	private static void mergeSets(Map result, Map map) {
		mergeSetsForCommonKeys(result, map);
		putAllThatDoNotExistInResultYet(result, map);
	}
	
	private static void mergeSetsForCommonKeys(Map result, Map map) {
		for (Iterator iter= result.keySet().iterator(); iter.hasNext();) {
			IMember key= (IMember) iter.next();
			if (map.containsKey(key)){
				Set resultSet= (Set)result.get(key);
				Set mapSet= (Set)map.get(key);
				resultSet.addAll(mapSet);
			}
		}
	}
	
	private static void putAllThatDoNotExistInResultYet(Map result, Map map) {
		for (Iterator iter= map.keySet().iterator(); iter.hasNext();) {
			IMember key= (IMember) iter.next();
			if (! result.containsKey(key)){
				Set mapSet= (Set)map.get(key);
				Set resultSet= new HashSet(mapSet);
				result.put(key, resultSet);
			}
		}
	}
	
	private Map getMatchingMembersMapping(IType analyzedType) throws JavaModelException {
		Map result= new HashMap();//IMember -> Set of IMembers (of the same type as key)
		IMember[] members= getMembersToBeCreatedInTargetClass();
		for (int i = 0; i < members.length; i++) {
			IMember member= members[i];
			if (member instanceof IMethod){
				IMethod method= (IMethod)member;
				IMethod found= MemberCheckUtil.findMethod(method, analyzedType.getMethods());
				if (found != null)
					addToMapping(result, method, found);
			} else if (member instanceof IField){
				IField field= (IField)member;
				IField found= analyzedType.getField(field.getElementName());
				if (found.exists())
					addToMapping(result, field, found);
			} else if (member instanceof IType){
				IType type= (IType)member;
				IType found= analyzedType.getType(type.getElementName());
				if (found.exists())
					addToMapping(result, type, found);
			} else
				Assert.isTrue(false);
		}
		
		return result;		
	}
		
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)	throws CoreException {
		try {
			pm.beginTask("", 3); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
	
			fMembersToMove= WorkingCopyUtil.getOriginals(fMembersToMove);
						
			result.merge(checkDeclaringType(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;			
			result.merge(checkIfMembersExist());
			if (result.hasFatalError())
				return result;			
			return result;			
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

	private static IMember[] merge(IMember[] a1, IMember[] a2, IMember[] a3){
		return JavaElementUtil.merge(JavaElementUtil.merge(a1, a2), a3);
	}
	
	private static IMember[] getMembersOfType(IMember[] members, int type){
		List list= Arrays.asList(JavaElementUtil.getElementsOfType(members, type));
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.preview"), 7); //$NON-NLS-1$
			clearCaches();

			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkGenericDeclaringType(new SubProgressMonitor(pm, 1)));
			result.merge(checkFinalFields(new SubProgressMonitor(pm, 1)));
			result.merge(checkAccesses(new SubProgressMonitor(pm, 1)));
			result.merge(checkMembersInTypeAndAllSubtypes(new SubProgressMonitor(pm, 2)));
			result.merge(checkIfSkippingOverElements(new SubProgressMonitor(pm, 1)));
			if (pm.isCanceled())
				throw new OperationCanceledException();
			if (shouldMakeTargetClassAbstract())
				result.merge(checkCallsToClassConstructors(getTargetClass(), new SubProgressMonitor(pm, 1)));
			else
				pm.worked(1);
			if (result.hasFatalError())
				return result;

			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1), result);
			result.merge(validateModifiesFiles());
			return result;
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkGenericDeclaringType(final SubProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(monitor);

		RefactoringStatus status= new RefactoringStatus();
		try {
			final IMember[] pullables= getMembersToMove();
			monitor.beginTask("", pullables.length); //$NON-NLS-1$

			final IType declaring= getDeclaringType();
			final ITypeParameter[] parameters= declaring.getTypeParameters();
			if (parameters.length > 0) {
				final TypeVariableMaplet[] mapping= TypeVariableUtil.subTypeToInheritedType(declaring);
				IMember member= null;
				int length= 0;
				for (int index= 0; index < pullables.length; index++) {
					member= pullables[index];
					final String[] unmapped= TypeVariableUtil.getUnmappedVariables(mapping, declaring, member);
					length= unmapped.length;
					switch (length) {
						case 1:
							status.addError(RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Type_variable_not_available", new String[] { unmapped[0], declaring.getSuperclassName()}), JavaStatusContext.create(member)); //$NON-NLS-1$
							break;
						case 2:
							status.addError(RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Type_variable2_not_available", new String[] { unmapped[0], unmapped[1], declaring.getSuperclassName()}), JavaStatusContext.create(member)); //$NON-NLS-1$
							break;
						case 3:
							status.addError(RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Type_variable3_not_available", new String[] { unmapped[0], unmapped[1], unmapped[2], declaring.getSuperclassName()}), JavaStatusContext.create(member)); //$NON-NLS-1$
					}
					monitor.worked(1);
					if (monitor.isCanceled())
						throw new OperationCanceledException();
				}
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	protected void clearCaches() {
		super.clearCaches();
		fCachedMembersReferences.clear();
		fCachedTargetClassHierarchy= null;
	}

	private RefactoringStatus checkIfSkippingOverElements(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			Set skippedSuperclassSet= getSkippedSuperclasses(new SubProgressMonitor(pm, 1));
			IType[] skippedSuperclasses= (IType[]) skippedSuperclassSet.toArray(new IType[skippedSuperclassSet.size()]);
			RefactoringStatus result= new RefactoringStatus();
			for (int i= 0; i < fMembersToMove.length; i++) {
				IMember element= fMembersToMove[i];
				for (int j= 0; j < skippedSuperclasses.length; j++) {
					result.merge(checkIfDeclaredIn(element, skippedSuperclasses[j]));
				}
			}
			return result;
		} finally{
			pm.done();
		}	
	}
	
	private RefactoringStatus checkIfDeclaredIn(IMember element, IType type) throws JavaModelException {
		if (element instanceof IMethod)
			return checkIfMethodDeclaredIn((IMethod)element, type);
		else if (element instanceof IField)
			return checkIfFieldDeclaredIn((IField)element, type);
		else if (element instanceof IType)
			return checkIfTypeDeclaredIn((IType)element, type);
		Assert.isTrue(false);	
		return null;			
	}
	
	private RefactoringStatus checkIfTypeDeclaredIn(IType iType, IType type) {
		IType typeInType= type.getType(iType.getElementName());
		if (! typeInType.exists())
			return null;
		String[] keys= {createTypeLabel(typeInType), createTypeLabel(type)};
		String msg= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Type_declared_in_class", keys); //$NON-NLS-1$
		RefactoringStatusContext context= JavaStatusContext.create(typeInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	private RefactoringStatus checkIfFieldDeclaredIn(IField iField, IType type) {
		IField fieldInType= type.getField(iField.getElementName());
		if (! fieldInType.exists())
			return null;
		String[] keys= {createFieldLabel(fieldInType), createTypeLabel(type)};
		String msg= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Field_declared_in_class", keys); //$NON-NLS-1$
		RefactoringStatusContext context= JavaStatusContext.create(fieldInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}
	
	private RefactoringStatus checkIfMethodDeclaredIn(IMethod iMethod, IType type) throws JavaModelException {
		IMethod methodInType= JavaModelUtil.findMethod(iMethod.getElementName(), iMethod.getParameterTypes(), iMethod.isConstructor(), type);
		if (methodInType == null || ! methodInType.exists())
			return null;
		String[] keys= {createMethodLabel(methodInType), createTypeLabel(type)};
		String msg= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.methodis_declared_in_class", keys); //$NON-NLS-1$
		RefactoringStatusContext context= JavaStatusContext.create(methodInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	private static boolean areAllPullable(IMember[] members) throws JavaModelException {
		for (int i = 0; i < members.length; i++) {
			if (! isPullable(members[i]))
				return false;
		}
		return true;
	}

	protected RefactoringStatus checkDeclaringType(IProgressMonitor monitor) throws JavaModelException {
		final RefactoringStatus status= super.checkDeclaringType(monitor);
		if (JavaModelUtil.getFullyQualifiedName(getDeclaringType()).equals("java.lang.Object")) //$NON-NLS-1$
			status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_java.lang.Object"))); //$NON-NLS-1$
		status.merge(checkSuperclassesOfDeclaringClass(monitor));
		return status;
	}

	private RefactoringStatus checkSuperclassesOfDeclaringClass(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		if (getPossibleTargetClasses(result, pm).length == 0 && !result.hasFatalError())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.not_this_type"));	 //$NON-NLS-1$
		return result;
	}

	private RefactoringStatus checkFinalFields(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", fMembersToMove.length); //$NON-NLS-1$
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			if (member.getElementType() == IJavaElement.FIELD && ! JdtFlags.isStatic(member) && JdtFlags.isFinal(member)){
				RefactoringStatusContext context= JavaStatusContext.create(member);
				result.addWarning(RefactoringCoreMessages.getString("PullUpRefactoring.final_fields"), context); //$NON-NLS-1$
			}
			pm.worked(1);
			if (pm.isCanceled())
				throw new OperationCanceledException();
		}
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkAccesses(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.checking_referenced_elements"), 3); //$NON-NLS-1$
		result.merge(checkAccessedTypes(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedFields(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedMethods(new SubProgressMonitor(pm, 1)));
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkAccessedTypes(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= getTypesReferencedInMovedMembers(pm);
		IType targetClass= getTargetClass();
		ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
		List pulledUpList= Arrays.asList(fMembersToMove);
		for (int i= 0; i < accessedTypes.length; i++) {
			IType iType= accessedTypes[i];
			if (! iType.exists())
				continue;

			if (!canBeAccessedFrom(iType, targetClass, targetSupertypes) && !pulledUpList.contains(iType)) {
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.type_not_accessible", //$NON-NLS-1$
					new String[]{createTypeLabel(iType), createTypeLabel(targetClass)});
				result.addError(message, JavaStatusContext.create(iType));
			}	
		}
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkAccessedFields(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 2); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		
		List pulledUpList= Arrays.asList(fMembersToMove);
		List deletedList= Arrays.asList(getMembersToDelete(new SubProgressMonitor(pm, 1)));
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(fMembersToMove, new SubProgressMonitor(pm, 1));
		
		IType targetClass= getTargetClass();
		ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
		for (int i= 0; i < accessedFields.length; i++) {
			IField field= accessedFields[i];
			if (! field.exists())
				continue;
			
			boolean isAccessible= 	pulledUpList.contains(field) ||
									deletedList.contains(field) ||
									canBeAccessedFrom(field, targetClass, targetSupertypes);
			if (! isAccessible){
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.field_not_accessible", //$NON-NLS-1$
					new String[]{createFieldLabel(field), createTypeLabel(targetClass)});
				result.addError(message, JavaStatusContext.create(field));
			} else if (isDeclaredInSkippedSuperclass(field, new NullProgressMonitor())){
				String[] keys= {createFieldLabel(field), createTypeLabel(targetClass)};
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.field_cannot_be_accessed", keys); //$NON-NLS-1$
				result.addError(message, JavaStatusContext.create(field));
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccessedMethods(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 2); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		
		List pulledUpList= Arrays.asList(fMembersToMove);
		List declaredAbstractList= Arrays.asList(fMethodsToDeclareAbstract);
		List deletedList= Arrays.asList(getMembersToDelete(new SubProgressMonitor(pm, 1)));
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(fMembersToMove, new SubProgressMonitor(pm, 1));
		
		IType targetClass= getTargetClass();
		ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
		for (int i= 0; i < accessedMethods.length; i++) {
			IMethod method= accessedMethods[i];
			if (! method.exists())
				continue;
			boolean isAccessible= 	pulledUpList.contains(method) || 
									deletedList.contains(method) ||
									declaredAbstractList.contains(method) ||
									canBeAccessedFrom(method, targetClass, targetSupertypes);
			if (! isAccessible){
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.method_not_accessible", //$NON-NLS-1$
					new String[]{createMethodLabel(method), createTypeLabel(targetClass)});
				result.addError(message, JavaStatusContext.create(method));
			} else if (isDeclaredInSkippedSuperclass(method, new NullProgressMonitor())){
				String[] keys= {createMethodLabel(method), createTypeLabel(targetClass)};
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.method_cannot_be_accessed", keys); //$NON-NLS-1$
				result.addError(message, JavaStatusContext.create(method));
			}	
		}
		pm.done();
		return result;
	}
	
	private boolean isDeclaredInSkippedSuperclass(IMember member, IProgressMonitor pm) throws JavaModelException {
		return getSkippedSuperclasses(pm).contains(member.getDeclaringType());
	}
	
	//skipped superclasses are those declared in the hierarchy between the declaring type of the selected members 
	//and the target type
	private Set getSkippedSuperclasses(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try {
			if (fCachedSkippedSuperclasses != null && getTypeHierarchyOfTargetClass(new SubProgressMonitor(pm, 1)).getType().equals(getTargetClass()))
				return fCachedSkippedSuperclasses;
			ITypeHierarchy hierarchy= getTypeHierarchyOfTargetClass(new SubProgressMonitor(pm, 1));
			fCachedSkippedSuperclasses= new HashSet(2);
			IType current= hierarchy.getSuperclass(getDeclaringType());
			while(current != null && ! current.equals(getTargetClass())){
				fCachedSkippedSuperclasses.add(current);
				current= hierarchy.getSuperclass(current);
			}
			return fCachedSkippedSuperclasses;
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkMembersInTypeAndAllSubtypes(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 3); //$NON-NLS-1$
		Set notDeletedMembers= getNotDeletedMembers(new SubProgressMonitor(pm, 1));
		Set notDeletedMembersInTargetType= new HashSet();
		Set notDeletedMembersInSubtypes= new HashSet();
		splitNotDeletedMembers(notDeletedMembers, notDeletedMembersInTargetType, notDeletedMembersInSubtypes);
		checkMembersInDestinationType(result, notDeletedMembersInTargetType);
		checkAccessModifiers(result, notDeletedMembersInSubtypes);
		checkMethodReturnTypes(new SubProgressMonitor(pm, 1), result, notDeletedMembersInSubtypes);
		checkFieldTypes(new SubProgressMonitor(pm, 1), result);
		pm.done();
		return result;
	}

	private void splitNotDeletedMembers(Set originals, Set inTargetType, Set inSubTypes) {
		for (Iterator iter= originals.iterator(); iter.hasNext();) {
			IMember member= (IMember) iter.next();
			if (getTargetClass().equals(member.getDeclaringType()))
				inTargetType.add(member);
			else
				inSubTypes.add(member);
		}
	}

	private void checkMembersInDestinationType(RefactoringStatus result, Set notDeletedMembersInTargetType) throws JavaModelException {
		IMember[] membersToBeCreatedInTargetClass= getMembersToBeCreatedInTargetClass();
		List newMembersInDestinationType= new ArrayList(membersToBeCreatedInTargetClass.length);
		newMembersInDestinationType.addAll(Arrays.asList(membersToBeCreatedInTargetClass));
		newMembersInDestinationType.addAll(notDeletedMembersInTargetType);
		newMembersInDestinationType.removeAll(Arrays.asList(fMethodsToDelete));
		IMember[] members= (IMember[]) newMembersInDestinationType.toArray(new IMember[newMembersInDestinationType.size()]);
		result.merge(MemberCheckUtil.checkMembersInDestinationType(members, getTargetClass()));
	}
	
	private void checkMethodReturnTypes(IProgressMonitor pm, RefactoringStatus result, Set notDeletedMembersInSubtypes) throws JavaModelException {
		Map mapping= getMatchingMemberMatching(pm, true);
		IMember[] members= getMembersToBeCreatedInTargetClass();
		for (int i= 0; i < members.length; i++) {
			if (members[i].getElementType() != IJavaElement.METHOD)
				continue;
			IMethod method= (IMethod)members[i];
			String returnType= getReturnTypeName(method);
			Assert.isTrue(mapping.containsKey(method));
			for (Iterator iter= ((Set)mapping.get(method)).iterator(); iter.hasNext();) {
				IMethod matchingMethod= (IMethod) iter.next();
				if (method.equals(matchingMethod))
					continue;
				if (!notDeletedMembersInSubtypes.contains(matchingMethod))
					continue;
				if (returnType.equals(getReturnTypeName(matchingMethod)))
					continue;
				String[] keys= {createMethodLabel(matchingMethod), createTypeLabel(matchingMethod.getDeclaringType())};	
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.different_method_return_type", //$NON-NLS-1$
					keys);
				RefactoringStatusContext context= JavaStatusContext.create(matchingMethod.getCompilationUnit(), matchingMethod.getNameRange());
				result.addError(message, context);	
			}
		}

	}

	private void checkFieldTypes(IProgressMonitor pm, RefactoringStatus result) throws JavaModelException {
		Map mapping= getMatchingMemberMatching(pm, true);
		for (int i= 0; i < fMembersToMove.length; i++) {
			if (fMembersToMove[i].getElementType() != IJavaElement.FIELD)
				continue;
			IField field= (IField)fMembersToMove[i];
			String type= getTypeName(field);
			Assert.isTrue(mapping.containsKey(field));
			for (Iterator iter= ((Set)mapping.get(field)).iterator(); iter.hasNext();) {
				IField matchingField= (IField) iter.next();
				if (field.equals(matchingField))
					continue;
				if (type.equals(getTypeName(matchingField)))
					continue;
				String[] keys= {createFieldLabel(matchingField), createTypeLabel(matchingField.getDeclaringType())};
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.different_field_type", //$NON-NLS-1$
					keys);
				RefactoringStatusContext context= JavaStatusContext.create(matchingField.getCompilationUnit(), matchingField.getSourceRange());					 
				result.addError(message, context);	
			}
		}
	}
	
	/* @return Map<IMember memberToBeCreatedInTargetClass, Set<IMember memberInTargetClassOrSubtypeOfTC> > */
	private Map getMatchingMemberMatching(IProgressMonitor pm, boolean includeMethodsToDeclareAbstract) throws JavaModelException {
		//TODO: This is the top method of this whole "matching members" mess.
		// Need to clean up the whole caller hierarchy and ensure that everything
		// is calculated correctly and only once, and that names make sense.
		return getMatchingMembersMappingFromTypeAndAllSubtypes(getTypeHierarchyOfTargetClass(pm), getTargetClass(), includeMethodsToDeclareAbstract);
	}

	private void checkAccessModifiers(RefactoringStatus result, Set notDeletedMembersInSubtypes) throws JavaModelException {
		List toDeclareAbstract= Arrays.asList(fMethodsToDeclareAbstract);
		for (Iterator iter= notDeletedMembersInSubtypes.iterator(); iter.hasNext();) {
			IMember member= (IMember) iter.next();
			if (member.getElementType() == IJavaElement.METHOD && ! toDeclareAbstract.contains(member))
				checkMethodAccessModifiers(result, ((IMethod) member));
		}
	}
	
	private void checkMethodAccessModifiers(RefactoringStatus result, IMethod method) throws JavaModelException {
		if (isVisibilityLowerThanProtected(method)){
		 	String[] keys= {createMethodLabel(method), createTypeLabel(method.getDeclaringType())};
		 	String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.lower_visibility", //$NON-NLS-1$
		 		keys);
			RefactoringStatusContext errorContext= JavaStatusContext.create(method);
			result.addError(message, errorContext);	
		}
	}

	/* @return not deleted matching members of membersToBeCreatedInTargetClass */
	private Set getNotDeletedMembers(IProgressMonitor pm) throws JavaModelException {
		Set matchingSet= new HashSet();
		pm.beginTask("", 2); //$NON-NLS-1$
		matchingSet.addAll(Arrays.asList(getMatchingElements(new SubProgressMonitor(pm, 1), true)));
		matchingSet.removeAll(Arrays.asList(getMembersToDelete(new SubProgressMonitor(pm, 1))));
		pm.done();
		return matchingSet;
	}
	
	private static boolean isVisibilityLowerThanProtected(IMember member)throws JavaModelException {
		return ! JdtFlags.isPublic(member) && ! JdtFlags.isProtected(member);
	}
	
	//--- change creation
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			return new DynamicValidationStateChange(RefactoringCoreMessages.getString("PullUpRefactoring.Pull_Up"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally {
			pm.done();
			clearCaches();
		}
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm, RefactoringStatus status) throws CoreException{
		try{
			pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.preview"), 4); //$NON-NLS-1$

			ICompilationUnit declaringCu= getDeclaringCU();
			CompilationUnit declaringCuNode= new RefactoringASTParser(AST.JLS3).parse(declaringCu, true);			

			addImportsToTargetCu(new SubProgressMonitor(pm, 1), declaringCuNode);

			TextChangeManager manager= new TextChangeManager();

			//ICompilationUnit -> List of IMember
			Map membersToDeleteMap= createMembersToDeleteMap(new SubProgressMonitor(pm, 1));

			//ICompilationUnit -> List of IType
			Map nonAbstractSubclassesMap= createNonAbstractSubclassesMapping(new SubProgressMonitor(pm, 1));

			ICompilationUnit[] cus= getCompilationUnitsOfSubclassesOfTargetClass(new SubProgressMonitor(pm, 1));
			IProgressMonitor subPm= new SubProgressMonitor(pm, 1);
			subPm.beginTask("", cus.length*6); //$NON-NLS-1$
			ICompilationUnit targetCu= getTargetCU();
			for (int i= 0; i < cus.length; i++) {
				ICompilationUnit cu= cus[i];
				if (! needsRewriting(cu, membersToDeleteMap, nonAbstractSubclassesMap)) {
					subPm.worked(6);
					continue;
				}	
				CompilationUnit cuNode= cu.equals(declaringCu) ? declaringCuNode : new RefactoringASTParser(AST.JLS3).parse(cu, true);
				ASTRewrite rewrite= ASTRewrite.create(cuNode.getAST());
				if (membersToDeleteMap.containsKey(cu)){
					List members= (List) membersToDeleteMap.get(cu);
					deleteDeclarationNodes(cuNode, rewrite, members);
				}
				if (cu.equals(targetCu)){ 
					if (shouldMakeTargetClassAbstract())
						makeTargetClassAbstract(rewrite, cuNode);
					TypeVariableMaplet[] mapping= TypeVariableUtil.subTypeToSuperType(getDeclaringType(), getTargetClass());
					copyMembersToTargetClass(declaringCuNode, cuNode, mapping, rewrite, new SubProgressMonitor(subPm, 1), status);
					createAbstractMethodsInTargetClass(cuNode, declaringCuNode, mapping, rewrite, new SubProgressMonitor(subPm, 1), status);
				} else{
					subPm.worked(2);
				}					
				if (cu.equals(declaringCu)){
					increaseVisibilityOfMethodsDeclaredAbstract(rewrite, cuNode, new SubProgressMonitor(subPm, 2), status);
				} else{
					subPm.worked(2);
				}					
				if (nonAbstractSubclassesMap.containsKey(cu)){
					List subclasses= (List)nonAbstractSubclassesMap.get(cu);
					addMethodStubsToNonAbstractSubclassesOfTargetClass(subclasses, cuNode, declaringCuNode, rewrite, new SubProgressMonitor(subPm, 2), status);
				}
				addTextEditFromRewrite(manager, cu, rewrite);
				if (subPm.isCanceled())
					throw new OperationCanceledException();
			}
			subPm.done();
			return manager;
		} finally{
			pm.done();
		}
	}

	//ICompilationUnit -> List of IMember
	private Map createMembersToDeleteMap(IProgressMonitor pm) throws JavaModelException {
		IMember[] membersToDelete= getMembersToDelete(pm);
		Map result= new HashMap();
		for (int i= 0; i < membersToDelete.length; i++) {
			IMember member= membersToDelete[i];
			ICompilationUnit cu= member.getCompilationUnit();
			if (! result.containsKey(cu))
				result.put(cu, new ArrayList(1));
			((List)result.get(cu)).add(member);
		}
		return result;
	}

	private boolean needsRewriting(ICompilationUnit unit, Map membersToDeleteMap, Map nonAbstractSubclassesMap) {
		return 	getDeclaringCU().equals(unit) ||
		 	   	getTargetCU().equals(unit) ||
				membersToDeleteMap.containsKey(unit) ||
				nonAbstractSubclassesMap.containsKey(unit);
	}

	private ICompilationUnit[] getCompilationUnitsOfSubclassesOfTargetClass(IProgressMonitor pm) throws JavaModelException {
		IType[] allSubtypes= getTypeHierarchyOfTargetClass(pm).getAllSubtypes(getTargetClass());
		Set result= new HashSet(allSubtypes.length);
		for (int i= 0; i < allSubtypes.length; i++) {
			result.add(allSubtypes[i].getCompilationUnit());
		}
		result.add(getTargetCU());
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}

	private void increaseVisibilityOfMethodsDeclaredAbstract(ASTRewrite rewrite, CompilationUnit cuNode, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		for (int i= 0; i < fMethodsToDeclareAbstract.length; i++) {
			IMethod method= fMethodsToDeclareAbstract[i];
			if (! needsToChangeVisibility(method, false, pm, status))
				continue;
			MethodDeclaration methodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(method, cuNode);//getMethodDeclarationNode(method);
		
			int newModifiers= getModifiersWithUpdatedVisibility(method, methodDeclaration.getModifiers(), pm, false, status);
			ModifierRewrite.create(rewrite, methodDeclaration).setModifiers(newModifiers, null);
		}
	}

	private void createAbstractMethodsInTargetClass(CompilationUnit targetCuNode, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, ASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		TypeDeclaration targetClass= ASTNodeSearchUtil.getTypeDeclarationNode(getTargetClass(), targetCuNode);
		pm.beginTask("", fMethodsToDeclareAbstract.length); //$NON-NLS-1$
		for (int i= 0; i < fMethodsToDeclareAbstract.length; i++) {
			createAbstractMethodInTargetClass(fMethodsToDeclareAbstract[i], declaringCuNode, targetClass, mapping, rewrite, new SubProgressMonitor(pm, 1), status);
		}
		pm.done();
	}

	private void createAbstractMethodInTargetClass(IMethod sourceMethod, CompilationUnit declaringCuNode, TypeDeclaration targetClass, TypeVariableMaplet[] mapping, ASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		MethodDeclaration methodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		AST ast= rewrite.getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		newMethod.setBody(null);
		newMethod.setConstructor(false);
		newMethod.setExtraDimensions(methodDeclaration.getExtraDimensions());
		newMethod.setJavadoc(null);
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, createModifiersForAbstractDeclaration(sourceMethod, pm, status)));
		newMethod.setName(createCopyOfSimpleName(methodDeclaration.getName(), ast));
		copyReturnType(rewrite, getDeclaringCU(), methodDeclaration, newMethod, mapping);
		copyParameters(rewrite, getDeclaringCU(), methodDeclaration, newMethod, mapping);
		copyThrownExceptions(methodDeclaration, newMethod);
		rewrite.getListRewrite(targetClass, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, targetClass.bodyDeclarations()), null);
	}

	private int createModifiersForAbstractDeclaration(IMethod method, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		int modifiers= Modifier.ABSTRACT | JdtFlags.clearFlag(Modifier.NATIVE | Modifier.FINAL, method.getFlags());
		return getModifiersWithUpdatedVisibility(method, modifiers, pm, false, status);
	}

	private int getModifiersWithUpdatedVisibility(IMember member, int modifiers, IProgressMonitor pm, boolean considerReferences, RefactoringStatus status) throws JavaModelException {
		if (needsToChangeVisibility(member, considerReferences, pm, status))
			return JdtFlags.clearAccessModifiers(modifiers) | Modifier.PROTECTED;

		return modifiers;
	}
	
	//concreteSubclasses: List of IType
	private void addMethodStubsToNonAbstractSubclassesOfTargetClass(List concreteSubclasses, CompilationUnit cuNode, CompilationUnit declaringCuNode, ASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		IType declaringType= getDeclaringType();
		IMethod[] methods= getAbstractMethodsAddedToTargetClass();
		IType[] typesToImport= getTypesReferencedInDeclarations(methods, declaringCuNode);
		pm.beginTask("", concreteSubclasses.size()); //$NON-NLS-1$
		for (Iterator iter= concreteSubclasses.iterator(); iter.hasNext();) {
			IType clazz= (IType)iter.next();
			if (clazz.equals(declaringType))
				continue;
			boolean anyStubAdded= false;
			TypeDeclaration classToCreateStubIn= ASTNodeSearchUtil.getTypeDeclarationNode(clazz, cuNode);
			ICompilationUnit cuToCreateStubIn= clazz.getCompilationUnit();
			IProgressMonitor subPm= new SubProgressMonitor(pm, 1);
			subPm.beginTask("", methods.length); //$NON-NLS-1$
			for (int j= 0; j < methods.length; j++) {
				IMethod method= methods[j];
				if (null == JavaModelUtil.findMethod(method.getElementName(), method.getParameterTypes(), method.isConstructor(), clazz)){
					addStub(method, declaringCuNode, classToCreateStubIn, cuToCreateStubIn, rewrite, new SubProgressMonitor(subPm, 1), status);
					anyStubAdded= true;
				}
			}
			subPm.done();
			if (anyStubAdded)
				addImports(typesToImport, clazz.getCompilationUnit());
		}
		pm.done();
	}

	private void addStub(IMethod sourceMethod, CompilationUnit declaringCuNode, TypeDeclaration typeToCreateStubIn, ICompilationUnit newCu, ASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		MethodDeclaration methodToCreateStubFor= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		AST ast= rewrite.getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		newMethod.setBody(getMethodStubBody(methodToCreateStubFor, ast));
		newMethod.setConstructor(false);
		newMethod.setExtraDimensions(methodToCreateStubFor.getExtraDimensions());
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, createModifiersForMethodStubs(sourceMethod, methodToCreateStubFor, pm, status)));
		newMethod.setName(createCopyOfSimpleName(methodToCreateStubFor.getName(), ast));
		TypeVariableMaplet[] mapping= TypeVariableUtil.composeMappings(TypeVariableUtil.subTypeToSuperType(getDeclaringType(), getTargetClass()), TypeVariableUtil.superTypeToInheritedType(getTargetClass(), Bindings.findType(typeToCreateStubIn.resolveBinding(), newCu.getJavaProject())));
		copyReturnType(rewrite, getDeclaringCU(), methodToCreateStubFor, newMethod, mapping);
		copyParameters(rewrite, getDeclaringCU(), methodToCreateStubFor, newMethod, mapping);
		copyThrownExceptions(methodToCreateStubFor, newMethod);
		newMethod.setJavadoc(createJavadocForStub(typeToCreateStubIn.getName().getIdentifier(), methodToCreateStubFor, newMethod, newCu, rewrite));
		rewrite.getListRewrite(typeToCreateStubIn, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, typeToCreateStubIn.bodyDeclarations()), null);
	}

	private static Block getMethodStubBody(MethodDeclaration method, AST ast) {
		Block body= ast.newBlock();
		Type returnType= method.getReturnType2();

		Expression expression= ASTNodeFactory.newDefaultExpression(ast, returnType, method.getExtraDimensions());
		if (expression != null) {
			ReturnStatement returnStatement= ast.newReturnStatement();
			returnStatement.setExpression(expression);
			body.statements().add(returnStatement);
		}
		return body;
	}

	private Javadoc createJavadocForStub(String enclosingTypeName, MethodDeclaration oldMethod, MethodDeclaration newMethodNode, ICompilationUnit cu, ASTRewrite rewrite) throws CoreException {
		if (! fCodeGenerationSettings.createComments)
			return null;
		IMethodBinding binding= oldMethod.resolveBinding();
		if (binding == null)
			return null;
		ITypeBinding[] params= binding.getParameterTypes();	
		String fullTypeName= JavaModelUtil.getFullyQualifiedName(getTargetClass());
		String[] fullParamNames= new String[params.length];
		for (int i= 0; i < fullParamNames.length; i++) {
			fullParamNames[i]= Bindings.getFullyQualifiedName(params[i]);
		}
		String comment= StubUtility.getMethodComment(cu, enclosingTypeName, newMethodNode, true, false, fullTypeName, fullParamNames, String.valueOf('\n'));
		return (Javadoc) rewrite.createStringPlaceholder(comment, ASTNode.JAVADOC);
	}

	private int createModifiersForMethodStubs(IMethod method, MethodDeclaration methodDeclaration, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		int modifiers= JdtFlags.clearFlag(Modifier.NATIVE | Modifier.ABSTRACT, methodDeclaration.getModifiers());
		return getModifiersWithUpdatedVisibility(method, modifiers, pm, false, status);
	}

	//ICompilationUnit -> List of IType
	private Map createNonAbstractSubclassesMapping(IProgressMonitor pm) throws JavaModelException{
		if (! shouldAddMethodStubsToConcreteSubclassesOfTargetClass())
			return new HashMap(0);
		Set nonAbstractSubclasses= getNonAbstractSubclasses(getTypeHierarchyOfTargetClass(pm), getTargetClass());
		Map result= new HashMap();
		for (Iterator iter= nonAbstractSubclasses.iterator(); iter.hasNext();) {
			IType type= (IType)iter.next();
			ICompilationUnit cu= type.getCompilationUnit();
			if (! result.containsKey(cu))
				result.put(cu, new ArrayList(1));
			((List)result.get(cu)).add(type);
		}
		return result;
	}

	//Set of IType
	private static Set getNonAbstractSubclasses(ITypeHierarchy hierarchy, IType clazz) throws JavaModelException {
		IType[] subclasses= hierarchy.getSubclasses(clazz);
		Set result= new HashSet();
		for (int i= 0; i < subclasses.length; i++) {
			IType subclass= subclasses[i];
			if (JdtFlags.isAbstract(subclass))
				result.addAll(getNonAbstractSubclasses(hierarchy, subclass));
			else
				result.add(subclass);	
		}
		return result;
	}

	private boolean shouldAddMethodStubsToConcreteSubclassesOfTargetClass() throws JavaModelException {
		return fCreateMethodStubs && getAbstractMethodsAddedToTargetClass().length > 0;
	}

	private void makeTargetClassAbstract(ASTRewrite rewrite, CompilationUnit cuNode) throws JavaModelException {
		TypeDeclaration targetClass= ASTNodeSearchUtil.getTypeDeclarationNode(getTargetClass(), cuNode);
	
		int newModifiers= targetClass.getModifiers() | Modifier.ABSTRACT;
		ModifierRewrite.create(rewrite, targetClass).setModifiers(newModifiers, null);
	}

	private boolean shouldMakeTargetClassAbstract() throws JavaModelException {
		return ! JdtFlags.isAbstract(getTargetClass()) && 
			   getAbstractMethodsAddedToTargetClass().length > 0;	
	}
	
	/* @return the members to be pulled up (*not* the pulled-up members!) */
	private IMember[] getMembersToBeCreatedInTargetClass(){
		List result= new ArrayList(fMembersToMove.length + fMethodsToDeclareAbstract.length);
		result.addAll(Arrays.asList(fMembersToMove));
		result.addAll(Arrays.asList(fMethodsToDeclareAbstract));
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}
	
	private IMethod[] getAbstractMethodsAddedToTargetClass() throws JavaModelException{
		IMethod[] toDeclareAbstract= fMethodsToDeclareAbstract;
		IMethod[] abstractPulledUp= getAbstractMethodsToPullUp();
		List result= new ArrayList(toDeclareAbstract.length + abstractPulledUp.length);
		result.addAll(Arrays.asList(toDeclareAbstract));
		result.addAll(Arrays.asList(abstractPulledUp));
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}

	private IMethod[] getAbstractMethodsToPullUp() throws JavaModelException {
		List result= new ArrayList(fMembersToMove.length);
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			if (member instanceof IMethod && JdtFlags.isAbstract(member))
				result.add(member);
		}
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}

	private IType getSuperclassOfDeclaringClass(IProgressMonitor pm) throws JavaModelException {
		IType declaringType= getDeclaringType();
		return declaringType.newSupertypeHierarchy(pm).getSuperclass(declaringType);
	}

	private void copyMembersToTargetClass(CompilationUnit declaringCuNode, CompilationUnit targetCuNode, TypeVariableMaplet[] mapping, ASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		TypeDeclaration targetClass= ASTNodeSearchUtil.getTypeDeclarationNode(getTargetClass(), targetCuNode);
		fMembersToMove= JavaElementUtil.sortByOffset(fMembersToMove); // preserve member order
		pm.beginTask("", fMembersToMove.length); //$NON-NLS-1$
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			if (member instanceof IField)
				copyFieldToTargetClass((IField)member, declaringCuNode, targetClass, mapping, rewrite, new SubProgressMonitor(pm, 1), status);
			else if (member instanceof IMethod)
				copyMethodToTargetClass((IMethod)member, declaringCuNode, targetClass, mapping, rewrite, new SubProgressMonitor(pm, 1), status);
			else if (member instanceof IType)
				copyTypeToTargetClass((IType)member, declaringCuNode, targetClass, mapping, rewrite);
			else 
				Assert.isTrue(false);
			pm.worked(1);
		}
		pm.done();
	}

	private void copyTypeToTargetClass(IType type, CompilationUnit declaringCuNode, TypeDeclaration targetClass, TypeVariableMaplet[] mapping, ASTRewrite rewrite) throws JavaModelException {
		BodyDeclaration newType= createNewTypeDeclarationNode(type, declaringCuNode, mapping, rewrite);
		rewrite.getListRewrite(targetClass, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(newType, ASTNodes.getInsertionIndex(newType, targetClass.bodyDeclarations()), null);
	}

	private BodyDeclaration createNewTypeDeclarationNode(IType type, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, ASTRewrite rewrite) throws JavaModelException {
		TypeDeclaration oldType= ASTNodeSearchUtil.getTypeDeclarationNode(type, declaringCuNode);
		ICompilationUnit declaringCu= getDeclaringCU();
		if (isVisibilityLowerThanProtected(type)) {
			if (mapping.length > 0)
				return createPlaceholderForTypeDeclaration(oldType, declaringCu, mapping, rewrite, true);

			return createPlaceholderForProtectedTypeDeclaration(oldType, declaringCuNode, declaringCu, rewrite, true);
		}
		if (mapping.length > 0)
			return createPlaceholderForTypeDeclaration(oldType, declaringCu, mapping, rewrite, true);

		return createPlaceholderForTypeDeclaration(oldType, declaringCu, rewrite, true);
	}

	private void copyMethodToTargetClass(IMethod method, CompilationUnit declaringCuNode, TypeDeclaration targetClass, TypeVariableMaplet[] mapping, ASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		MethodDeclaration newMethod= createNewMethodDeclarationNode(method, declaringCuNode, mapping, rewrite, pm, status);
		rewrite.getListRewrite(targetClass, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, targetClass.bodyDeclarations()), null);
	}

	private MethodDeclaration createNewMethodDeclarationNode(IMethod sourceMethod, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, ASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		MethodDeclaration sourceMethodNode= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		AST ast= rewrite.getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		copyBodyOfPulledUpMethod(rewrite, sourceMethod, sourceMethodNode, newMethod, mapping, pm);
		newMethod.setConstructor(sourceMethodNode.isConstructor());
		newMethod.setExtraDimensions(sourceMethodNode.getExtraDimensions());
		copyJavadocNode(rewrite, sourceMethod, sourceMethodNode, newMethod);
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getNewModifiers(sourceMethod, true, pm, status)));
		newMethod.setName(createCopyOfSimpleName(sourceMethodNode.getName(), ast));
		copyReturnType(rewrite, getDeclaringCU(), sourceMethodNode, newMethod, mapping);
		copyParameters(rewrite, getDeclaringCU(), sourceMethodNode, newMethod, mapping);
		copyThrownExceptions(sourceMethodNode, newMethod);
		return newMethod;
	}

	private void copyBodyOfPulledUpMethod(ASTRewrite targetRewrite, IMethod method, MethodDeclaration oldMethod, MethodDeclaration newMethod, TypeVariableMaplet[] mapping, IProgressMonitor pm) throws JavaModelException {
		Block body= oldMethod.getBody();
		if (body == null) {
			newMethod.setBody(null);
			return;
		}
		try {
			final IDocument document= new Document(method.getCompilationUnit().getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(body.getAST());
			final ITrackedNodePosition position= rewriter.track(body);
			body.accept(new MethodMapper(rewriter, getSuperclassOfDeclaringClass(pm), mapping));
			rewriter.rewriteAST(document, null).apply(document, TextEdit.NONE);
			String content= document.get(position.getStartPosition(), position.getLength());
			String[] lines= Strings.convertIntoLines(content);
			Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
			content= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
			newMethod.setBody((Block) targetRewrite.createStringPlaceholder(content, ASTNode.BLOCK));
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
	}

	private static void copyThrownExceptions(MethodDeclaration oldMethod, MethodDeclaration newMethod) {
		AST ast= newMethod.getAST();
		for (int i= 0, n= oldMethod.thrownExceptions().size(); i < n; i++) {
			Name oldExceptionName= (Name)oldMethod.thrownExceptions().get(i);
			newMethod.thrownExceptions().add(i, createCopyOfName(oldExceptionName, ast));
		}
	}
	
	private static Name createCopyOfName(Name name, AST ast){
		return (Name)ASTNode.copySubtree(ast, name);
	}
	
	private static SimpleName createCopyOfSimpleName(SimpleName simpleName, AST ast){
		return (SimpleName)ASTNode.copySubtree(ast, simpleName);
	}

	// TODO factor out
	private void copyParameters(ASTRewrite targetRewrite, ICompilationUnit cu, MethodDeclaration oldMethod, MethodDeclaration newMethod, TypeVariableMaplet[] mapping) throws JavaModelException {
		for (int i= 0, n= oldMethod.parameters().size(); i < n; i++) {
			SingleVariableDeclaration newParam= null;
			SingleVariableDeclaration oldParam= (SingleVariableDeclaration) oldMethod.parameters().get(i);
			if (mapping.length > 0)
				newParam= createPlaceholderForSingleVariableDeclaration(oldParam, cu, mapping, targetRewrite);
			else
				newParam= createPlaceholderForSingleVariableDeclaration(oldParam, cu, targetRewrite);
			newMethod.parameters().add(i, newParam);
		}
	}

	// TODO factor out
	private void copyReturnType(ASTRewrite targetRewrite, ICompilationUnit sourceCu, MethodDeclaration oldMethod, MethodDeclaration newMethod, TypeVariableMaplet[] mapping) throws JavaModelException {
		Type newReturnType= null;
		Type oldReturnType= oldMethod.getReturnType2();
		if (mapping.length > 0)
			newReturnType= createPlaceholderForType(oldReturnType, sourceCu, mapping, targetRewrite);
		else
			newReturnType= createPlaceholderForType(oldReturnType, sourceCu, targetRewrite);
		newMethod.setReturnType2(newReturnType);
	}

	// TODO factor out
	private void copyFieldToTargetClass(IField field, CompilationUnit declaringCuNode, TypeDeclaration targetClass, TypeVariableMaplet[] mapping, ASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		FieldDeclaration newField= createNewFieldDeclarationNode(field, declaringCuNode, mapping, rewrite, pm, status);
		rewrite.getListRewrite(targetClass, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(newField, ASTNodes.getInsertionIndex(newField, targetClass.bodyDeclarations()), null);
	}

	// TODO factor out
	private FieldDeclaration createNewFieldDeclarationNode(IField field, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, ASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		AST ast= rewrite.getAST();
		VariableDeclarationFragment oldFieldFragment= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, declaringCuNode);
		VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
		newFragment.setExtraDimensions(oldFieldFragment.getExtraDimensions());
		Expression initializer= oldFieldFragment.getInitializer();
		if (initializer != null) {
			Expression newInitializer= null;
			if (mapping.length > 0)
				newInitializer= createPlaceholderForExpression(initializer, field.getCompilationUnit(), mapping, rewrite);
			else
				newInitializer= createPlaceholderForExpression(initializer, field.getCompilationUnit(), rewrite);
			newFragment.setInitializer(newInitializer);
		}	
		newFragment.setName(createCopyOfSimpleName(oldFieldFragment.getName(), ast));
		FieldDeclaration newField= ast.newFieldDeclaration(newFragment);
		FieldDeclaration oldField= ASTNodeSearchUtil.getFieldDeclarationNode(field, declaringCuNode);
		copyJavadocNode(rewrite, field, oldField, newField);
		newField.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getNewModifiers(field, true, pm, status)));

		Type oldType= oldField.getType();
		ICompilationUnit cu= field.getCompilationUnit();
		Type newType= null;
		if (mapping.length > 0) {
			newType= createPlaceholderForType(oldType, cu, mapping, rewrite);
		} else
			newType= createPlaceholderForType(oldType, cu, rewrite);
		newField.setType(newType);
		return newField;
	}

	private boolean needsToChangeVisibility(IMember member, boolean considerReferences, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		if (JdtFlags.isPublic(member) || JdtFlags.isProtected(member))
			return false;
		if (! considerReferences)
			return true;
		return isReferencedBySomethingElseThanMembersToPull(member, pm, status);
	}

	private boolean isReferencedBySomethingElseThanMembersToPull(IMember member, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		SearchResultGroup[] references= getReferences(member, pm, status);
		if (references.length == 0)
			return false;
		if (references.length > 1)
			return true;
		ICompilationUnit referencingCu= references[0].getCompilationUnit();
		if (! getDeclaringCU().equals(referencingCu))
			return true;
		SearchMatch[] searchResults= references[0].getSearchResults();
		for (int i= 0; i < searchResults.length; i++) {
			if (! isWithinMemberToPullUp(searchResults[i]))
				return true;
		}
		return false;
	}
	
	private SearchResultGroup[] getReferences(IMember member, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		if (! fCachedMembersReferences.containsKey(member)) {
			IJavaSearchScope scope= RefactoringScopeFactory.create(member);
			SearchPattern pattern= SearchPattern.createPattern(member, IJavaSearchConstants.REFERENCES);
			fCachedMembersReferences.put(member, RefactoringSearchEngine.search(pattern, scope, pm, status));
		}
		return (SearchResultGroup[])fCachedMembersReferences.get(member);
	}

	private boolean isWithinMemberToPullUp(SearchMatch result) throws JavaModelException {
		int referenceStart= result.getOffset();
		for (int i= 0; i < fMembersToMove.length; i++) {
			if (liesWithin(fMembersToMove[i].getSourceRange(), referenceStart))
				return true;
		}
		return false;
	}

	private static boolean liesWithin(ISourceRange range, int offset) {
		return range.getOffset() <= offset && range.getOffset() + range.getLength() >= offset;
	}

	private int getNewModifiers(IMember member, boolean considerReferences, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		return getModifiersWithUpdatedVisibility(member, member.getFlags(), pm, considerReferences, status);
	}
	
	private void addImportsToTargetCu(IProgressMonitor pm, CompilationUnit declaringCuNode) throws CoreException {
		addImports(getTypesThatNeedToBeImportedInTargetCu(pm, declaringCuNode), getTargetCU());
	}

	private void addImports(IType[] typesToImport, ICompilationUnit cu) throws CoreException {
		for (int i= 0; i < typesToImport.length; i++) {
			fImportManager.addImportTo(typesToImport[i], cu);
		}
	}

	private ICompilationUnit getTargetCU() {
		return getTargetClass().getCompilationUnit();
	}

	private IType[] getTypesThatNeedToBeImportedInTargetCu(IProgressMonitor pm, CompilationUnit declaringCuNode) throws JavaModelException {
		if (getTargetCU().equals(getDeclaringCU()))
			return new IType[0];
		IType[] typesInPulledUpMembers= getTypesReferencedInMovedMembers(pm);
		IType[] typesInMethodsDeclaredAbstract= getTypesReferencedInDeclarations(fMethodsToDeclareAbstract, declaringCuNode);
		List result= new ArrayList(typesInMethodsDeclaredAbstract.length + typesInPulledUpMembers.length);
		result.addAll(Arrays.asList(typesInMethodsDeclaredAbstract));
		result.addAll(Arrays.asList(typesInPulledUpMembers));
		return (IType[]) result.toArray(new IType[result.size()]);
	}
	
	private IType[] getTypesReferencedInDeclarations(IMethod[] methods, CompilationUnit declaringCuNode) throws JavaModelException{
		MethodDeclaration[] methodDeclarations= getMethodDeclarations(methods, declaringCuNode);
		ITypeBinding[] referencedTypesBindings= ReferenceFinderUtil.getTypesReferencedInDeclarations(methodDeclarations);
		List types= new ArrayList(referencedTypesBindings.length);
		IJavaProject proj= getDeclaringType().getJavaProject();
		for (int i= 0; i < referencedTypesBindings.length; i++) {
			ITypeBinding typeBinding= referencedTypesBindings[i];
			if (typeBinding == null)
				continue;
			if (typeBinding.isArray())
				typeBinding= typeBinding.getElementType();
			IType type= Bindings.findType(typeBinding, proj);
			if (type != null)
				types.add(type);
		}
		return (IType[]) types.toArray(new IType[types.size()]);
	}
	
	private static MethodDeclaration[] getMethodDeclarations(IMethod[] methods, CompilationUnit cuNode) throws JavaModelException {
		List result= new ArrayList(methods.length);
		for (int i= 0; i < methods.length; i++) {
			result.add(ASTNodeSearchUtil.getMethodDeclarationNode(methods[i], cuNode));
		}
		return (MethodDeclaration[]) result.toArray(new MethodDeclaration[result.size()]);
	}
}