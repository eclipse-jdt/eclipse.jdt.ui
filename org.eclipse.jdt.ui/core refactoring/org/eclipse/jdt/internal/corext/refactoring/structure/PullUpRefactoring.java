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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.dom.OldASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;


public class PullUpRefactoring extends Refactoring {
	private static class SuperReferenceFinderVisitor extends ASTVisitor{
		
		private Collection fFoundRanges;
		private int fMethodSourceStart;
		private int fMethodSourceEnd;
		private String fMethodSource;
		private String fSuperTypeName;
		
		SuperReferenceFinderVisitor(IMethod method, IType superType) throws JavaModelException{
			fFoundRanges= new ArrayList(0);
			fMethodSourceStart= method.getSourceRange().getOffset();
			fMethodSourceEnd= method.getSourceRange().getOffset() + method.getSourceRange().getLength();	
			fMethodSource= method.getSource();
			fSuperTypeName= JavaModelUtil.getFullyQualifiedName(superType);
		}

		ISourceRange[] getSuperReferenceRanges(){
			return (ISourceRange[]) fFoundRanges.toArray(new ISourceRange[fFoundRanges.size()]);
		}
		
		private boolean withinMethod(ASTNode node){
			return (node.getStartPosition() >= fMethodSourceStart) && (node.getStartPosition() <= fMethodSourceEnd);
		}
		
		private ISourceRange getSuperRange(String scanSource){
			IScanner scanner= ToolFactory.createScanner(false, false, false, false);
			scanner.setSource(scanSource.toCharArray());
			try {
				int token = scanner.getNextToken();
				while (token != ITerminalSymbols.TokenNameEOF) {
					switch (token) {
						case ITerminalSymbols.TokenNamesuper :
							int start= scanner.getCurrentTokenEndPosition() + 1 - scanner.getCurrentTokenSource().length;
							int end= scanner.getCurrentTokenEndPosition() + 1;
							return new SourceRange(start, end - start);
					}
					token = scanner.getNextToken();
				}
			} catch(InvalidInputException e) {
				return new SourceRange(0, 0); //FIX ME
			}
			return new SourceRange(0, 0);//FIX ME
		}

		private String getSource(int start, int end){
			return fMethodSource.substring(start - fMethodSourceStart, end - fMethodSourceStart);
		}
		
		private String getScanSource(SuperMethodInvocation node){
			return getSource(getScanSourceOffset(node), node.getName().getStartPosition());
		}
		
		private String getScanSource(SuperFieldAccess node){
			return getSource(getScanSourceOffset(node), node.getName().getStartPosition());
		}
		
		private static int getScanSourceOffset(SuperMethodInvocation node){
			if (node.getQualifier() == null)
				return node.getStartPosition();
			else
				return node.getQualifier().getStartPosition() + node.getQualifier().getLength();			
		}
		
		private static int getScanSourceOffset(SuperFieldAccess node){
			if (node.getQualifier() == null)
				return node.getStartPosition();
			else
				return node.getQualifier().getStartPosition() + node.getQualifier().getLength();			
		}
		
		//---- visit methods ------------------
		
		public boolean visit(SuperFieldAccess node) {
			if (! withinMethod(node))
				return true;
			
			ISourceRange superRange= getSuperRange(getScanSource(node));
			fFoundRanges.add(new SourceRange(superRange.getOffset() + getScanSourceOffset(node), superRange.getLength()));
			return true;
		}

		public boolean visit(SuperMethodInvocation node) {
			if (! withinMethod(node))
				return true;
			
			IBinding nameBinding= node.getName().resolveBinding();
			if (nameBinding != null && nameBinding.getKind() == IBinding.METHOD){
				ITypeBinding declaringType= ((IMethodBinding)nameBinding).getDeclaringClass();
				if (declaringType != null && ! fSuperTypeName.equals(Bindings.getFullyQualifiedName(declaringType)))
					return true;
			}
			ISourceRange superRange= getSuperRange(getScanSource(node));
			fFoundRanges.add(new SourceRange(superRange.getOffset() + getScanSourceOffset(node), superRange.getLength()));
			return true;
		}
		
		//- stop nodes ---
		
		public boolean visit(TypeDeclarationStatement node) {
			if (withinMethod(node))
				return false;
			return true;
		}

		public boolean visit(AnonymousClassDeclaration node) {
			if (withinMethod(node))
				return false;
			return true;
		}
	}
	
	private IMember[] 	fMembersToPullUp;
	private IMethod[] 	fMethodsToDeclareAbstract;
	private IMethod[] 	fMethodsToDelete;
	private TextChangeManager fChangeManager;
	private IType fTargetType;
	private boolean fCreateMethodStubs;
	
	private final ImportRewriteManager fImportManager;
	private final CodeGenerationSettings fPreferenceSettings;

	//caches
	private IType fCachedDeclaringType;
	private IType[] fCachedTypesReferencedInPulledUpMembers;
	private ITypeHierarchy fCachedTargetClassHierarchy;
	private Set fCachedSkippedSuperclasses;//Set<IType>
	private final Map fCachedMembersReferences; //Map<IMember, SearchResultGroup[]>

	private PullUpRefactoring(IMember[] elements, CodeGenerationSettings preferenceSettings){
		Assert.isNotNull(elements);
		Assert.isNotNull(preferenceSettings);
		fMembersToPullUp= elements;
		fMethodsToDelete= new IMethod[0];
		fMethodsToDeclareAbstract= new IMethod[0];
		fPreferenceSettings= preferenceSettings;
		fImportManager= new ImportRewriteManager(preferenceSettings);
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
	
	private static IType getSingleTopLevelType(IMember[] members) {
		if (members != null && members.length == 1 && Checks.isTopLevelType(members[0]))
			return (IType)members[0];
		return null;
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
	 * it is the caller's resposibility to ensure that the selection makes sense from the behavior-preservation point of view.
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
	public void setMembersToPullUp(IMember[] elements){
		Assert.isNotNull(elements);
		fMembersToPullUp= (IMember[])SourceReferenceUtil.sortByOffset(elements);
		fMembersToPullUp= WorkingCopyUtil.getOriginals(fMembersToPullUp);
	}
	
	private IMember[] getMembersToDelete(IProgressMonitor pm) throws JavaModelException{
		try{
			IMember[] typesToDelete= WorkingCopyUtil.getOriginals(getMembersOfType(fMembersToPullUp, IJavaElement.TYPE));
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

	public IMember[] getMembersToPullUp() {
		return fMembersToPullUp;
	}
	
	public IType getDeclaringType(){
		if (fCachedDeclaringType != null)
			return fCachedDeclaringType;
		//all members declared in same type - checked in precondition
		Assert.isTrue(fMembersToPullUp.length > 0);//ensured by constructor
		fCachedDeclaringType= (IType)WorkingCopyUtil.getOriginal(fMembersToPullUp[0].getDeclaringType()); 
		return fCachedDeclaringType;
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
		if (superClasses.length == binary) {
			status.addFatalError(RefactoringCoreMessages.getString("PullUPRefactoring.no_all_binary")); //$NON-NLS-1$
		}
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
	
			fMembersToPullUp= WorkingCopyUtil.getOriginals(fMembersToPullUp);
						
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
	
	private RefactoringStatus checkIfMembersExist() {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMembersToPullUp.length; i++) {
			IMember orig= fMembersToPullUp[i];
			if (orig == null || ! orig.exists()){
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.does_not_exist", orig.getElementName());//$NON-NLS-1$
				result.addFatalError(message);
			}	
		}
		return result;
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
			result.merge(checkFinalFields(new SubProgressMonitor(pm, 1)));
			result.merge(checkAccesses(new SubProgressMonitor(pm, 1)));
			result.merge(checkMembersInTypeAndAllSubtypes(new SubProgressMonitor(pm, 2)));
			result.merge(checkIfSkippingOverElements(new SubProgressMonitor(pm, 1)));
			if (pm.isCanceled())
				throw new OperationCanceledException();
			if (shouldMakeTargetClassAbstract())
				result.merge(checkCallsToTargetClassConstructors(new SubProgressMonitor(pm, 1)));
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
	
	private void clearCaches() {
		fCachedTypesReferencedInPulledUpMembers= null;
		fImportManager.clear();
		fCachedMembersReferences.clear();
		fCachedTargetClassHierarchy= null;
	}
	
	private RefactoringStatus checkCallsToTargetClassConstructors(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		SearchResultGroup[] groups= ConstructorReferenceFinder.getConstructorReferences(getTargetClass(), pm, result);
		String[] keys= {createTypeLabel(getTargetClass())};
		String msg= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.gets_instantiated", keys); //$NON-NLS-1$
		for (int i= 0; i < groups.length; i++) {
			ICompilationUnit cu= groups[i].getCompilationUnit();
			if (cu == null)
				continue;
			CompilationUnit cuNode= new RefactoringASTParser(AST.JLS3).parse(cu, false);
			ASTNode[] refNodes= ASTNodeSearchUtil.getAstNodes(groups[i].getSearchResults(), cuNode);
			for (int j= 0; j < refNodes.length; j++) {
				ASTNode node= refNodes[j];
				if ((node instanceof ClassInstanceCreation) || ConstructorReferenceFinder.isImplicitConstructorReferenceNodeInClassCreations(node)){
					RefactoringStatusContext context= JavaStatusContext.create(cu, node);
					result.addError(msg, context);
				}
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkIfSkippingOverElements(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 1); //$NON-NLS-1$
		try{
			Set skippedSuperclassSet= getSkippedSuperclasses(new SubProgressMonitor(pm, 1));
			IType[] skippedSuperclasses= (IType[]) skippedSuperclassSet.toArray(new IType[skippedSuperclassSet.size()]);
			RefactoringStatus result= new RefactoringStatus();
			for (int i= 0; i < fMembersToPullUp.length; i++) {
				IMember element= fMembersToPullUp[i];
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

	private static String createTypeLabel(IType type){
		return JavaModelUtil.getFullyQualifiedName(type);
	}

	private static String createFieldLabel(IField field){
		return field.getElementName();
	}

	private static String createMethodLabel(IMethod method){
		return JavaElementUtil.createMethodSignature(method);
	}

	private static boolean areAllPullable(IMember[] members) throws JavaModelException {
		for (int i = 0; i < members.length; i++) {
			if (! isPullable(members[i]))
				return false;
		}
		return true;
	}

	private RefactoringStatus checkDeclaringType(IProgressMonitor pm) throws JavaModelException {
		IType declaringType= getDeclaringType();
				
		if (declaringType.isInterface()) //for now
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_interface_members")); //$NON-NLS-1$
		
		if (JavaModelUtil.getFullyQualifiedName(declaringType).equals("java.lang.Object")) //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_java.lang.Object"));	 //$NON-NLS-1$
	
		if (declaringType.isBinary())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_binary_types"));	 //$NON-NLS-1$
	
		if (declaringType.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_read_only_types"));	 //$NON-NLS-1$
		
		return checkSuperclassesOfDeclaringClass(pm);	
	}
	
	private RefactoringStatus checkSuperclassesOfDeclaringClass(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		if (getPossibleTargetClasses(result, pm).length == 0 && !result.hasFatalError())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.not_this_type"));	 //$NON-NLS-1$
		return result;
	}
	
	private static boolean haveCommonDeclaringType(IMember[] members){
		if (members.length == 0)
			return false;
		IType declaringType= members[0].getDeclaringType();
		if (declaringType == null)
			return false;
		for (int i= 0; i < members.length; i++) {
			if (! declaringType.equals(members[i].getDeclaringType()))
				return false;			
		}	
		return true;
	}
	
	private RefactoringStatus checkFinalFields(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", fMembersToPullUp.length); //$NON-NLS-1$
		for (int i= 0; i < fMembersToPullUp.length; i++) {
			IMember member= fMembersToPullUp[i];
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
		IType[] accessedTypes= getTypeReferencedInPulledUpMembers(pm);
		IType targetClass= getTargetClass();
		ITypeHierarchy targetSupertypes= targetClass.newSupertypeHierarchy(null);
		for (int i= 0; i < accessedTypes.length; i++) {
			IType iType= accessedTypes[i];
			if (! iType.exists())
				continue;
			
			if (! canBeAccessedFrom(iType, targetClass, targetSupertypes)){
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
		
		List pulledUpList= Arrays.asList(fMembersToPullUp);
		List deletedList= Arrays.asList(getMembersToDelete(new SubProgressMonitor(pm, 1)));
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(fMembersToPullUp, new SubProgressMonitor(pm, 1));
		
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
		
		List pulledUpList= Arrays.asList(fMembersToPullUp);
		List declaredAbstractList= Arrays.asList(fMethodsToDeclareAbstract);
		List deletedList= Arrays.asList(getMembersToDelete(new SubProgressMonitor(pm, 1)));
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(fMembersToPullUp, new SubProgressMonitor(pm, 1));
		
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
	
	/**
	 * @param member a called member
	 * @param newHome the new home of the member call
	 * @param newHomeSupertypes supertype hierarchy of newHome
	 * @return <code>true</code> iff member is visible from <code>newHome</code> 
	 * @throws JavaModelException
	 */
	private boolean canBeAccessedFrom(IMember member, IType newHome, ITypeHierarchy newHomeSupertypes) throws JavaModelException {
		Assert.isTrue(!(member instanceof IInitializer));
		if (! member.exists())
			return false;
			
		if (newHome.equals(member.getDeclaringType()))
			return true;
			
		if (newHome.equals(member))
			return true;	
		
		if (JdtFlags.isPrivate(member))
			return false;
			
		if (member.getDeclaringType() == null){ //top level -> end of recursion
			if (! (member instanceof IType))
				return false;

			if (JdtFlags.isPublic(member))
				return true;
			
			if (! JdtFlags.isPackageVisible(member))
				return false;
			
			if (JavaModelUtil.isSamePackage(((IType) member).getPackageFragment(), newHome.getPackageFragment()))
				return true;
			
			return newHomeSupertypes.contains(member.getDeclaringType());
		} else {
			IType enclosingType= member.getDeclaringType();
			
			if (! canBeAccessedFrom(enclosingType, newHome, newHomeSupertypes)) // recursive
				return false;

			if (enclosingType.equals(getDeclaringType())) //cannot reach down the hierachy
				return false;
			
			if (JdtFlags.isPublic(member))
				return true;
		 
			if (JavaModelUtil.isSamePackage(enclosingType.getPackageFragment(), newHome.getPackageFragment()))
				return true;
			
			return JdtFlags.isProtected(member) && newHomeSupertypes.contains(enclosingType);
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
		for (int i= 0; i < fMembersToPullUp.length; i++) {
			if (fMembersToPullUp[i].getElementType() != IJavaElement.FIELD)
				continue;
			IField field= (IField)fMembersToPullUp[i];
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

	private static String getReturnTypeName(IMethod method) throws JavaModelException {
		return Signature.toString(Signature.getReturnType(method.getSignature()).toString());
	}
	
	private static String getTypeName(IField field) throws JavaModelException {
		return Signature.toString(field.getTypeSignature());
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
	
	private IFile[] getAllFilesToModify(){
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles(){
		return Checks.validateModifiesFiles(getAllFilesToModify(), getValidationContext());
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
				OldASTRewrite rewrite= new OldASTRewrite(cuNode);
				if (membersToDeleteMap.containsKey(cu)){
					List members= (List) membersToDeleteMap.get(cu);
					deleteDeclarationNodes(cuNode, rewrite, members);
				}
				if (cu.equals(targetCu)){ 
					if (shouldMakeTargetClassAbstract())
						makeTargetClassAbstract(rewrite, cuNode);
					copyMembersToTargetClass(declaringCuNode, cuNode, rewrite, new SubProgressMonitor(subPm, 1), status);
					createAbstractMethodsInTargetClass(cuNode, declaringCuNode, rewrite, new SubProgressMonitor(subPm, 1), status);
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
	
	/*
	 * members: List<IMember>
	 */
	private static void deleteDeclarationNodes(CompilationUnit cuNode, OldASTRewrite rewrite, List members) throws JavaModelException {
		List declarationNodes= getDeclarationNodes(cuNode, members);
		for (Iterator iter= declarationNodes.iterator(); iter.hasNext();) {
			ASTNode node= (ASTNode) iter.next();
			if (node instanceof VariableDeclarationFragment){
				if (node.getParent() instanceof FieldDeclaration){
					FieldDeclaration fd= (FieldDeclaration)node.getParent();
					if (areAllFragmentsDeleted(fd, declarationNodes))
						rewrite.remove(fd, null);
					else
						rewrite.remove(node, null);
				}
			} else {
				rewrite.remove(node, null);
			}
		}
	}
	
	private static boolean areAllFragmentsDeleted(FieldDeclaration fd, List declarationNodes) {
		for (Iterator iter= fd.fragments().iterator(); iter.hasNext();) {
			if (! declarationNodes.contains(iter.next()))
				return false;
		}
		return true;
	}

	/*
	 * return List<ASTNode>
	 * members List<IMember>
	 */
	private static List getDeclarationNodes(CompilationUnit cuNode, List members) throws JavaModelException {
		List result= new ArrayList(members.size());
		for (Iterator iter= members.iterator(); iter.hasNext(); ) {
			IMember member= (IMember)iter.next();
			ASTNode declarationNode= null;
			if (member instanceof IField)
				declarationNode= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField)member, cuNode);
			else if (member instanceof IType)
				declarationNode= ASTNodeSearchUtil.getTypeDeclarationNode((IType)member, cuNode);
			else if (member instanceof IMethod)
				declarationNode= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod)member, cuNode);
			if (declarationNode != null)
				result.add(declarationNode);
		}
		return result;
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

	private void addTextEditFromRewrite(TextChangeManager manager, ICompilationUnit cu, OldASTRewrite rewrite) throws CoreException {
		TextBuffer textBuffer= TextBuffer.create(cu.getBuffer().getContents());
		TextEdit resultingEdits= new MultiTextEdit();
		rewrite.rewriteNode(textBuffer, resultingEdits);

		TextChange textChange= manager.get(cu);
		if (fImportManager.hasImportEditFor(cu))
			resultingEdits.addChild(fImportManager.getImportRewrite(cu).createEdit(textBuffer.getDocument()));
		TextChangeCompatibility.addTextEdit(textChange, RefactoringCoreMessages.getString("PullUpRefactoring.42"), resultingEdits); //$NON-NLS-1$
		rewrite.removeModifications();
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

	private void increaseVisibilityOfMethodsDeclaredAbstract(OldASTRewrite rewrite, CompilationUnit cuNode, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		for (int i= 0; i < fMethodsToDeclareAbstract.length; i++) {
			IMethod method= fMethodsToDeclareAbstract[i];
			if (! needsToChangeVisibility(method, false, pm, status))
				continue;
			MethodDeclaration methodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(method, cuNode);//getMethodDeclarationNode(method);
		
			int newModifiers= getModifiersWithUpdatedVisibility(method, methodDeclaration.getModifiers(), pm, false, status);
			ModifierRewrite.create(rewrite, methodDeclaration).setModifiers(newModifiers, null);
		}
	}

	private void createAbstractMethodsInTargetClass(CompilationUnit targetCuNode, CompilationUnit declaringCuNode, OldASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		TypeDeclaration targetClass= ASTNodeSearchUtil.getTypeDeclarationNode(getTargetClass(), targetCuNode);
		pm.beginTask("", fMethodsToDeclareAbstract.length); //$NON-NLS-1$
		for (int i= 0; i < fMethodsToDeclareAbstract.length; i++) {
			createAbstractMethodInTargetClass(fMethodsToDeclareAbstract[i], declaringCuNode, rewrite, targetClass, new SubProgressMonitor(pm, 1), status);
		}
		pm.done();
	}
	
	private void createAbstractMethodInTargetClass(IMethod sourceMethod, CompilationUnit declaringCuNode, OldASTRewrite rewrite, TypeDeclaration targetClass, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		MethodDeclaration methodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		AST ast= rewrite.getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		newMethod.setBody(null);
		newMethod.setConstructor(false);
		newMethod.setExtraDimensions(methodDeclaration.getExtraDimensions());
		newMethod.setJavadoc(null);
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, createModifiersForAbstractDeclaration(sourceMethod, pm, status)));
		newMethod.setName(createCopyOfSimpleName(methodDeclaration.getName(), ast));
		copyReturnType(rewrite, getDeclaringCU(), methodDeclaration, newMethod);		
		copyParameters(rewrite, getDeclaringCU(), methodDeclaration, newMethod);
		copyThrownExceptions(methodDeclaration, newMethod);
		targetClass.bodyDeclarations().add(newMethod);
		rewrite.markAsInserted(newMethod);
	}

	private int createModifiersForAbstractDeclaration(IMethod method, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		int modifiers= Modifier.ABSTRACT | JdtFlags.clearFlag(Modifier.NATIVE | Modifier.FINAL, method.getFlags());
		return getModifiersWithUpdatedVisibility(method, modifiers, pm, false, status);
	}
	
	private int getModifiersWithUpdatedVisibility(IMember member, int modifiers, IProgressMonitor pm, boolean considerReferences, RefactoringStatus status) throws JavaModelException{
		if (needsToChangeVisibility(member, considerReferences, pm, status))
			return JdtFlags.clearAccessModifiers(modifiers) | Modifier.PROTECTED;
		else
			return modifiers;	
	}
	
	//concreteSubclasses: List of IType
	private void addMethodStubsToNonAbstractSubclassesOfTargetClass(List concreteSubclasses, CompilationUnit cuNode, CompilationUnit declaringCuNode, OldASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws CoreException {
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

	private void addStub(IMethod sourceMethod, CompilationUnit declaringCuNode, TypeDeclaration typeToCreateStubIn, ICompilationUnit newCu, OldASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		MethodDeclaration methodToCreateStubFor= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		AST ast= rewrite.getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		newMethod.setBody(getMethodStubBody(methodToCreateStubFor, ast));
		newMethod.setConstructor(false);
		newMethod.setExtraDimensions(methodToCreateStubFor.getExtraDimensions());
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, createModifiersForMethodStubs(sourceMethod, methodToCreateStubFor, pm, status)));
		newMethod.setName(createCopyOfSimpleName(methodToCreateStubFor.getName(), ast));
		copyReturnType(rewrite, getDeclaringCU(), methodToCreateStubFor, newMethod);		
		copyParameters(rewrite, getDeclaringCU(), methodToCreateStubFor, newMethod);
		copyThrownExceptions(methodToCreateStubFor, newMethod);
		newMethod.setJavadoc(createJavadocForStub(typeToCreateStubIn.getName().getIdentifier(), methodToCreateStubFor, newMethod, newCu, rewrite));
		typeToCreateStubIn.bodyDeclarations().add(newMethod);
		rewrite.markAsInserted(newMethod);
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

	private Javadoc createJavadocForStub(String enclosingTypeName, MethodDeclaration oldMethod, MethodDeclaration newMethodNode, ICompilationUnit cu, OldASTRewrite rewrite) throws CoreException {
		if (! fPreferenceSettings.createComments)
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

	private void makeTargetClassAbstract(OldASTRewrite rewrite, CompilationUnit cuNode) throws JavaModelException {
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
		List result= new ArrayList(fMembersToPullUp.length + fMethodsToDeclareAbstract.length);
		result.addAll(Arrays.asList(fMembersToPullUp));
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
		List result= new ArrayList(fMembersToPullUp.length);
		for (int i= 0; i < fMembersToPullUp.length; i++) {
			IMember member= fMembersToPullUp[i];
			if (member instanceof IMethod && JdtFlags.isAbstract(member))
				result.add(member);
		}
		return (IMethod[]) result.toArray(new IMethod[result.size()]);
	}

	private IType getSuperclassOfDeclaringClass(IProgressMonitor pm) throws JavaModelException {
		IType declaringType= getDeclaringType();
		return declaringType.newSupertypeHierarchy(pm).getSuperclass(declaringType);
	}

	private void copyMembersToTargetClass(CompilationUnit declaringCuNode, CompilationUnit targetCuNode, OldASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		TypeDeclaration targetClass= ASTNodeSearchUtil.getTypeDeclarationNode(getTargetClass(), targetCuNode);
		fMembersToPullUp= JavaElementUtil.sortByOffset(fMembersToPullUp); // preserve member order
		pm.beginTask("", fMembersToPullUp.length); //$NON-NLS-1$
		for (int i= 0; i < fMembersToPullUp.length; i++) {
			IMember member= fMembersToPullUp[i];
			if (member instanceof IField)
				copyFieldToTargetClass((IField)member, declaringCuNode, targetClass, rewrite, new SubProgressMonitor(pm, 1), status);
			else if (member instanceof IMethod)
				copyMethodToTargetClass((IMethod)member, declaringCuNode, targetClass, rewrite, new SubProgressMonitor(pm, 1), status);
			else if (member instanceof IType)
				copyTypeToTargetClass((IType)member, declaringCuNode, targetClass, rewrite);
			else 
				Assert.isTrue(false);
			pm.worked(1);
		}
		pm.done();
	}
	
	private void copyTypeToTargetClass(IType type, CompilationUnit declaringCuNode, TypeDeclaration targetClass, OldASTRewrite rewrite) throws JavaModelException {
		BodyDeclaration newType= createNewTypeDeclarationNode(type, declaringCuNode, rewrite);
		rewrite.markAsInserted(newType);
		int insertionIndex= ASTNodes.getInsertionIndex(newType, targetClass.bodyDeclarations());
		targetClass.bodyDeclarations().add(insertionIndex, newType);
	}

	private BodyDeclaration createNewTypeDeclarationNode(IType type, CompilationUnit declaringCuNode, OldASTRewrite rewrite) throws JavaModelException {
		TypeDeclaration oldType= ASTNodeSearchUtil.getTypeDeclarationNode(type, declaringCuNode);
		return createPlaceholderForTypeDeclaration(oldType, getDeclaringCU(), rewrite, true);
	}

	private void copyMethodToTargetClass(IMethod method, CompilationUnit declaringCuNode, TypeDeclaration targetClass, OldASTRewrite targetRewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		MethodDeclaration newMethod= createNewMethodDeclarationNode(method, declaringCuNode, targetRewrite, pm, status);
		targetRewrite.markAsInserted(newMethod);
		int insertionIndex= ASTNodes.getInsertionIndex(newMethod, targetClass.bodyDeclarations());
		targetClass.bodyDeclarations().add(insertionIndex, newMethod);
	}

	private MethodDeclaration createNewMethodDeclarationNode(IMethod sourceMethod, CompilationUnit declaringCuNode, OldASTRewrite targetRewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		MethodDeclaration sourceMethodNode= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		AST ast= targetRewrite.getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		copyBodyOfPulledUpMethod(targetRewrite, sourceMethod, sourceMethodNode, newMethod, pm);
		newMethod.setConstructor(sourceMethodNode.isConstructor());
		newMethod.setExtraDimensions(sourceMethodNode.getExtraDimensions());
		copyJavadocNode(targetRewrite, sourceMethod, sourceMethodNode, newMethod);
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getNewModifiers(sourceMethod, true, pm, status)));
		newMethod.setName(createCopyOfSimpleName(sourceMethodNode.getName(), ast));
		copyReturnType(targetRewrite, getDeclaringCU(), sourceMethodNode, newMethod);
		copyParameters(targetRewrite, getDeclaringCU(), sourceMethodNode, newMethod);
		copyThrownExceptions(sourceMethodNode, newMethod);
		return newMethod;
	}

	private void copyBodyOfPulledUpMethod(OldASTRewrite targetRewrite, IMethod method, MethodDeclaration oldMethod, MethodDeclaration newMethod, IProgressMonitor pm) throws JavaModelException {
		if (oldMethod.getBody() == null){
			newMethod.setBody(null);
			return;
		}
		Block oldBody= oldMethod.getBody();
		ISourceRange[] superRefOffsert= SourceRange.reverseSortByOffset(findSuperReferenceRanges(method, getSuperclassOfDeclaringClass(pm)));
		
		String oldBodySource= getBufferText(oldBody, getDeclaringCU());
		StringBuffer newBodyCodeBuff= new StringBuffer(oldBodySource);
		for (int i= 0; i < superRefOffsert.length; i++) {
			ISourceRange range= superRefOffsert[i];
			int start= range.getOffset() - oldBody.getStartPosition();
			int end= start + range.getLength();
			newBodyCodeBuff.replace(start, end, "this"); //$NON-NLS-1$
		}
		String newBodySource= newBodyCodeBuff.toString();
		String[] lines= Strings.convertIntoLines(newBodySource);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		newBodySource= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
		Block newBody= (Block)targetRewrite.createStringPlaceholder(newBodySource, ASTNode.BLOCK);
		newMethod.setBody(newBody);
	}
	
	private static ISourceRange[] findSuperReferenceRanges(IMethod method, IType superType) throws JavaModelException{
		Assert.isNotNull(method);
		if (JdtFlags.isStatic(method))
			return new ISourceRange[0];
		SuperReferenceFinderVisitor visitor= new SuperReferenceFinderVisitor(method, superType);
		new RefactoringASTParser(AST.JLS3).parse(method.getCompilationUnit(), true).accept(visitor);
		return visitor.getSuperReferenceRanges();
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
	
	private static void copyParameters(OldASTRewrite targetRewrite, ICompilationUnit cu, MethodDeclaration oldMethod, MethodDeclaration newMethod) throws JavaModelException {
		for (int i= 0, n= oldMethod.parameters().size(); i < n; i++) {
			SingleVariableDeclaration oldParam= (SingleVariableDeclaration)oldMethod.parameters().get(i);
			SingleVariableDeclaration newParam= createPlaceholderForSingleVariableDeclaration(oldParam, cu, targetRewrite);
			newMethod.parameters().add(i, newParam);
		}
	}
	
	private static void copyReturnType(OldASTRewrite targetRewrite, ICompilationUnit sourceCu, MethodDeclaration oldMethod, MethodDeclaration newMethod) throws JavaModelException {
		Type newReturnType= createPlaceholderForType(oldMethod.getReturnType2(), sourceCu, targetRewrite);
		newMethod.setReturnType2(newReturnType);
	}
	
	private static void copyJavadocNode(OldASTRewrite rewrite, IMember member, BodyDeclaration oldDeclaration, BodyDeclaration newDeclaration) throws JavaModelException {
		Javadoc oldJavaDoc= oldDeclaration.getJavadoc();
		if (oldJavaDoc == null)
			return;
		Javadoc newJavaDoc= (Javadoc)ASTNode.copySubtree(rewrite.getAST(), oldJavaDoc);
		newDeclaration.setJavadoc(newJavaDoc);
		/*
		String source= oldJavaDoc.getComment();
		String[] lines= Strings.convertIntoLines(source);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		source= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(member));
		Javadoc newJavaDoc= (Javadoc)rewrite.createStringPlaceholder(source, ASTNode.JAVADOC);
		*/
	}

	private void copyFieldToTargetClass(IField field, CompilationUnit declaringCuNode, TypeDeclaration targetClass, OldASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		FieldDeclaration newField= createNewFieldDeclarationNode(field, declaringCuNode, rewrite, pm, status);
		rewrite.markAsInserted(newField);
		int insertionIndex= ASTNodes.getInsertionIndex(newField, targetClass.bodyDeclarations());
		targetClass.bodyDeclarations().add(insertionIndex, newField);
	}

	private FieldDeclaration createNewFieldDeclarationNode(IField field, CompilationUnit declaringCuNode, OldASTRewrite rewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		AST ast= rewrite.getAST();
		VariableDeclarationFragment oldFieldFragment= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, declaringCuNode);
		VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
		newFragment.setExtraDimensions(oldFieldFragment.getExtraDimensions());
		if (oldFieldFragment.getInitializer() != null){
			Expression newInitializer= createPlaceholderForExpression(oldFieldFragment.getInitializer(), field.getCompilationUnit(), rewrite);
			newFragment.setInitializer(newInitializer);
		}	
		newFragment.setName(createCopyOfSimpleName(oldFieldFragment.getName(), ast));
		FieldDeclaration newField= ast.newFieldDeclaration(newFragment);
		FieldDeclaration oldField= ASTNodeSearchUtil.getFieldDeclarationNode(field, declaringCuNode);
		copyJavadocNode(rewrite, field, oldField, newField);
		newField.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getNewModifiers(field, true, pm, status)));
		
		Type newType= createPlaceholderForType(oldField.getType(), field.getCompilationUnit(), rewrite);
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
		for (int i= 0; i < fMembersToPullUp.length; i++) {
			if (liesWithin(fMembersToPullUp[i].getSourceRange(), referenceStart))
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

	private ICompilationUnit getDeclaringCU() {
		return getDeclaringType().getCompilationUnit();
	}

	private ICompilationUnit getTargetCU() {
		return getTargetClass().getCompilationUnit();
	}

	private IType[] getTypesThatNeedToBeImportedInTargetCu(IProgressMonitor pm, CompilationUnit declaringCuNode) throws JavaModelException {
		if (getTargetCU().equals(getDeclaringCU()))
			return new IType[0];
		IType[] typesInPulledUpMembers= getTypeReferencedInPulledUpMembers(pm);
		IType[] typesInMethodsDeclaredAbstract= getTypesReferencedInDeclarations(fMethodsToDeclareAbstract, declaringCuNode);
		List result= new ArrayList(typesInMethodsDeclaredAbstract.length + typesInPulledUpMembers.length);
		result.addAll(Arrays.asList(typesInMethodsDeclaredAbstract));
		result.addAll(Arrays.asList(typesInPulledUpMembers));
		return (IType[]) result.toArray(new IType[result.size()]);
	}
	
	private IType[] getTypeReferencedInPulledUpMembers(IProgressMonitor pm) throws JavaModelException {
		if (fCachedTypesReferencedInPulledUpMembers == null)
			fCachedTypesReferencedInPulledUpMembers= ReferenceFinderUtil.getTypesReferencedIn(fMembersToPullUp, pm);
		return fCachedTypesReferencedInPulledUpMembers;
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

	private static String getBufferText(ASTNode node, ICompilationUnit cu) throws JavaModelException{
		return cu.getBuffer().getText(node.getStartPosition(), node.getLength());
	}

	private static String getNewText(ASTNode astNode, ICompilationUnit cu, boolean removeIndentation) throws JavaModelException {
		String bufferText= getBufferText(astNode, cu);
		if (removeIndentation)
			return getUnindentedText(bufferText, cu);
		else
			return bufferText;
	}
	
	private static String getUnindentedText(String bufferText, ICompilationUnit cu) throws JavaModelException {
		String[] lines= Strings.convertIntoLines(bufferText);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		return Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(cu));
	}
			
	//---- placeholder creators ----

	private static Expression createPlaceholderForExpression(Expression expression, ICompilationUnit cu, OldASTRewrite rewrite) throws JavaModelException{
		return (Expression)rewrite.createStringPlaceholder(getBufferText(expression, cu), ASTNode.METHOD_INVOCATION);
	}
			
	private static SingleVariableDeclaration createPlaceholderForSingleVariableDeclaration(SingleVariableDeclaration declaration, ICompilationUnit cu, OldASTRewrite rewrite) throws JavaModelException{
		return (SingleVariableDeclaration)rewrite.createStringPlaceholder(getBufferText(declaration, cu), ASTNode.SINGLE_VARIABLE_DECLARATION);
	}
	
	private static Type createPlaceholderForType(Type type, ICompilationUnit cu, OldASTRewrite rewrite) throws JavaModelException{
		return (Type)rewrite.createStringPlaceholder(getBufferText(type, cu), ASTNode.SIMPLE_TYPE);
	}

	private static BodyDeclaration createPlaceholderForTypeDeclaration(BodyDeclaration bodyDeclaration, ICompilationUnit cu, OldASTRewrite rewrite, boolean removeIndentation) throws JavaModelException{
		String newBufferText= getNewText(bodyDeclaration, cu, removeIndentation);
		return (BodyDeclaration)rewrite.createStringPlaceholder(newBufferText, ASTNode.TYPE_DECLARATION);
	}
}
