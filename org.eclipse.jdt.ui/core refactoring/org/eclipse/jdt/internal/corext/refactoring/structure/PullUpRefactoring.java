/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

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
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.ui.text.correction.ASTResolving;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Binding2JavaModel;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;


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
	
	private final ASTNodeMappingManager fAstManager;
	private final ASTRewriteManager fRewriteManager;
	private final ImportEditManager fImportEditManager;
	private final CodeGenerationSettings fPreferenceSettings;

	//caches
	private IType fCachedDeclaringType;
	private IType[] fTypesReferencedInPulledUpMembers;
	private ITypeHierarchy fCachedTargetClassHierarchy;
	private Set fCachedSkippedSuperclasses;

	public PullUpRefactoring(IMember[] elements, CodeGenerationSettings preferenceSettings){
		Assert.isTrue(elements.length > 0);
		Assert.isNotNull(preferenceSettings);
		fMembersToPullUp= (IMember[])SourceReferenceUtil.sortByOffset(elements);
		fMethodsToDelete= new IMethod[0];
		fMethodsToDeclareAbstract= new IMethod[0];
		fPreferenceSettings= preferenceSettings;
		fAstManager= new ASTNodeMappingManager();
		fRewriteManager= new ASTRewriteManager(fAstManager);
		fImportEditManager= new ImportEditManager(preferenceSettings);
		fCreateMethodStubs= true;
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
		fCachedDeclaringType= (IType)WorkingCopyUtil.getOriginal(fMembersToPullUp[0].getDeclaringType()); //index safe - checked in constructor
		return fCachedDeclaringType;
	}
	
	public IMember[] getPullableMembersOfDeclaringType() {
		try{
			List list= new ArrayList(3);
			addAllPullable(getDeclaringType().getFields(), list);
			addAllPullable(getDeclaringType().getMethods(), list);
			addAllPullable(getDeclaringType().getTypes(), list);
			return (IMember[]) list.toArray(new IMember[list.size()]);
		} catch (JavaModelException e){
			return new IMember[0];
		}	
	}
	
	private void addAllPullable(IMember[] members, List list) throws JavaModelException{
		for (int i= 0; i < members.length; i++) {
			if (isPullable(members[i]))
				list.add(members[i]);
		}	
	}
	private static boolean isPullable(IMember member) throws JavaModelException {
		return checkElement(member).isOK();
	}
	
	public ITypeHierarchy getTypeHierarchyOfTargetClass(IProgressMonitor pm) throws JavaModelException {
		try{
			if (fCachedTargetClassHierarchy != null)
				return fCachedTargetClassHierarchy;
			fCachedTargetClassHierarchy= getTargetClass().newTypeHierarchy(pm);
			return fCachedTargetClassHierarchy;
		} finally{
			pm.done();
		}	
	}
	
	public IType[] getPossibleTargetClasses(IProgressMonitor pm) throws JavaModelException {
		IType[] superClasses= getDeclaringType().newSupertypeHierarchy(pm).getAllSuperclasses(getDeclaringType());
		List superClassList= new ArrayList(superClasses.length);	
		for (int i= 0; i < superClasses.length; i++) {
			if (isPossibleTargetClass(superClasses[i]))
				superClassList.add(superClasses[i]);
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
	
	public IMember[] getMatchingElements(IProgressMonitor pm, boolean includeMethodsToDeclareAbstract) throws JavaModelException {
		try{	
			Set result= new HashSet();
			IType targetClass= getTargetClass();
			Map matching= getMatchingMemberMatching(pm, includeMethodsToDeclareAbstract);
			for (Iterator iter= matching.keySet().iterator(); iter.hasNext();) {
				IMember	key= (IMember) iter.next();
				if (key.getDeclaringType().equals(targetClass))
					iter.remove();
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
				IField found= type.getField(type.getElementName());
				if (found.exists())
					addToMapping(result, type, found);
			} else
				Assert.isTrue(false);
		}
		
		return result;		
	}
	
	/* non java-doc
	 * @see Refactoring#checkPreconditions(IProgressMonitor)
	 */
	public RefactoringStatus checkPreconditions(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= checkPreactivation();
		if (result.hasFatalError())
			return result;
		result.merge(super.checkPreconditions(pm));
		return result;
	}
	
	public RefactoringStatus checkPreactivation() throws JavaModelException{
					
		RefactoringStatus precheck= precheckAllElements(); //this just checks basic things like being binary etc.
		if (precheck.hasFatalError())
			return precheck;

		if (! haveCommonDeclaringType())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.same_declaring_type"));			 //$NON-NLS-1$

		return new RefactoringStatus();
	}
		
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm)	throws JavaModelException {
		try {
			pm.beginTask("", 3); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();

			result.merge(checkAllElements());
			if (result.hasFatalError())
				return result;

			fMembersToPullUp= WorkingCopyUtil.getOriginals(fMembersToPullUp);
						
			result.merge(checkDeclaringType(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;			
			result.merge(checkIfMembersExist());
			if (result.hasFatalError())
				return result;			
			result.merge(checkMultiDeclarationOfFields());//XXX should enable this case
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

	private RefactoringStatus checkMultiDeclarationOfFields() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMembersToPullUp.length; i++) {
			IMember member= fMembersToPullUp[i];
			if (member.getElementType() != IJavaElement.FIELD)
				continue;
			IField field= (IField)member;	
			if (isPartOfMultiDeclaration(field)){
				Context context= JavaSourceContext.create(member);
				String[] keys= {createFieldLabel(field)};
				String msg= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.multi_declaration", keys); //$NON-NLS-1$
				result.addFatalError(msg, context);
			}
		}
		return result;
	}

	private boolean isPartOfMultiDeclaration(IField iField) throws JavaModelException {
		Selection selection= Selection.createFromStartLength(iField.getNameRange().getOffset(), iField.getNameRange().getLength());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(selection, true);
		fAstManager.getAST(iField.getCompilationUnit()).accept(analyzer);
		ASTNode selected= analyzer.getFirstSelectedNode();
		if (selected == null)
			selected= analyzer.getLastCoveringNode();
		if (selected == null)
			return false;
		FieldDeclaration fieldDeclaration= (FieldDeclaration)ASTNodes.getParent(selected, FieldDeclaration.class);	
		if (fieldDeclaration == null)
			return false;
		return fieldDeclaration.fragments().size() != 1;	
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
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.preview"), 7); //$NON-NLS-1$
			clearManagers();

			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkFinalFields(new SubProgressMonitor(pm, 1)));
			result.merge(checkAccesses(new SubProgressMonitor(pm, 1)));
			result.merge(checkMembersInDestinationType());
			pm.worked(1);
			result.merge(checkMembersInSubclasses(new SubProgressMonitor(pm, 1)));
			result.merge(checkIfSkippingOverElements(new SubProgressMonitor(pm, 1)));
			if (shouldMakeTargetClassAbstract())
				result.merge(checkCallsToTargetClassConstructors(new SubProgressMonitor(pm, 1)));
			else	
				pm.worked(1);
			if (result.hasFatalError())
				return result;
			
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} catch (JavaModelException e){
			throw e;
		} catch (CoreException e){	
			throw new JavaModelException(e);
		} finally {
			pm.done();
		}	
	}
	private RefactoringStatus checkMembersInDestinationType() throws JavaModelException {
		IMember[] membersToBeCreatedInTargetClass = getMembersToBeCreatedInTargetClass();
		List newMembersInDesitnationType= new ArrayList(membersToBeCreatedInTargetClass.length);
		newMembersInDesitnationType.addAll(Arrays.asList(membersToBeCreatedInTargetClass));
		newMembersInDesitnationType.removeAll(Arrays.asList(fMethodsToDelete));
		IMember[] members= (IMember[]) newMembersInDesitnationType.toArray(new IMember[newMembersInDesitnationType.size()]);
		return MemberCheckUtil.checkMembersInDestinationType(members, getTargetClass());
	}
	
	private void clearManagers() {
		fAstManager.clear();
		fImportEditManager.clear();
		fRewriteManager.clear();
	}
	
	private RefactoringStatus checkCallsToTargetClassConstructors(IProgressMonitor pm) throws JavaModelException {
		ASTNode[] refNodes= ConstructorReferenceFinder.getConstructorReferenceNodes(getTargetClass(), fAstManager, pm);
		RefactoringStatus result= new RefactoringStatus();
		String[] keys= {createTypeLabel(getTargetClass())};
		String msg= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.gets_instantiated", keys); //$NON-NLS-1$
		for (int i= 0; i < refNodes.length; i++) {
			ASTNode node= refNodes[i];
			if (node instanceof ClassInstanceCreation){
				Context context= JavaSourceContext.create(fAstManager.getCompilationUnit(node), node);
				result.addError(msg, context);
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
		Context context= JavaSourceContext.create(typeInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}

	private RefactoringStatus checkIfFieldDeclaredIn(IField iField, IType type) {
		IField fieldInType= type.getField(iField.getElementName());
		if (! fieldInType.exists())
			return null;
		String[] keys= {createFieldLabel(fieldInType), createTypeLabel(type)};
		String msg= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Field_declared_in_class", keys); //$NON-NLS-1$
		Context context= JavaSourceContext.create(fieldInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}
	
	private RefactoringStatus checkIfMethodDeclaredIn(IMethod iMethod, IType type) throws JavaModelException {
		IMethod methodInType= JavaModelUtil.findMethod(iMethod.getElementName(), iMethod.getParameterTypes(), iMethod.isConstructor(), type);
		if (methodInType == null || ! methodInType.exists())
			return null;
		String[] keys= {createMethodLabel(methodInType), createTypeLabel(type)};
		String msg= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.methodis_declared_in_class", keys); //$NON-NLS-1$
		Context context= JavaSourceContext.create(methodInType);
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

	private RefactoringStatus precheckAllElements() throws JavaModelException {
		//just 1 error message
		for (int i = 0; i < fMembersToPullUp.length; i++) {
			RefactoringStatus status= precheckElement(fMembersToPullUp[i]);
			if (! status.isOK())
				return status;
		}
		return new RefactoringStatus();
	}

	private RefactoringStatus checkAllElements() throws JavaModelException {
		//just 1 error message
		for (int i = 0; i < fMembersToPullUp.length; i++) {
			RefactoringStatus status= checkElement(fMembersToPullUp[i]);
			if (! status.isOK())
				return status;
		}
		return new RefactoringStatus();
	}

	private static RefactoringStatus precheckElement(IMember member) throws JavaModelException{
		if (member.getElementType() != IJavaElement.METHOD && 
			member.getElementType() != IJavaElement.FIELD &&
			member.getElementType() != IJavaElement.TYPE)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.only_fields_and_methods"));			 //$NON-NLS-1$
				
		if (! member.exists())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.elements_do_not_exist"));			 //$NON-NLS-1$
	
		if (member.isBinary())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_binary_elements"));	 //$NON-NLS-1$

		if (member.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_read_only_elements"));					 //$NON-NLS-1$

		if (! member.isStructureKnown())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_unknown_structure"));					 //$NON-NLS-1$

		return new RefactoringStatus();	
	}
		
	private static RefactoringStatus checkElement(IMember member) throws JavaModelException{
		RefactoringStatus precheck= precheckElement(member);
		if (precheck.hasFatalError())
			return precheck;
		
		if (member instanceof IType){
			if (! JdtFlags.isStatic(member))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.only_static_types")); //$NON-NLS-1$
		}
		if (member instanceof IMethod ){
			IMethod method= (IMethod) member;
			if (method.isConstructor())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_constructors"));			 //$NON-NLS-1$
			
			if (JdtFlags.isNative(method)) //for now - move to input preconditions
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_native_methods"));				 //$NON-NLS-1$
		}
		return new RefactoringStatus();	
	}
	
	private RefactoringStatus checkDeclaringType(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 3); //$NON-NLS-1$
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
		if (getPossibleTargetClasses(pm).length == 0)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.not_this_type"));	 //$NON-NLS-1$
		return new RefactoringStatus();
	}
	
	private boolean haveCommonDeclaringType(){
		IType declaringType= fMembersToPullUp[0].getDeclaringType(); //index safe - checked in constructor
		if (declaringType == null)
			return false;
		for (int i= 0; i < fMembersToPullUp.length; i++) {
			if (! declaringType.equals(fMembersToPullUp[i].getDeclaringType()))
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
				Context context= JavaSourceContext.create(member);
				result.addWarning(RefactoringCoreMessages.getString("PullUpRefactoring.final_fields"), context); //$NON-NLS-1$
			}
			pm.worked(1);
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
		for (int i= 0; i < accessedTypes.length; i++) {
			IType iType= accessedTypes[i];
			if (! iType.exists())
				continue;
			
			if (! canBeAccessedFrom(iType, targetClass)){
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.type_not_accessible", //$NON-NLS-1$
					new String[]{createTypeLabel(iType), createTypeLabel(targetClass)});
				result.addError(message, JavaSourceContext.create(iType));
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
		for (int i= 0; i < accessedFields.length; i++) {
			IField field= accessedFields[i];
			if (! field.exists())
				continue;
			
			boolean isAccessible= 	pulledUpList.contains(field) ||
									deletedList.contains(field) ||
									canBeAccessedFrom(field, targetClass);
			if (! isAccessible){
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.field_not_accessible", //$NON-NLS-1$
					new String[]{createFieldLabel(field), createTypeLabel(targetClass)});
				result.addError(message, JavaSourceContext.create(field));
			} else if (isDeclaredInSkippedSuperclass(field, new NullProgressMonitor())){
				String[] keys= {createFieldLabel(field), createTypeLabel(targetClass)};
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.field_cannot_be_accessed", keys); //$NON-NLS-1$
				result.addError(message, JavaSourceContext.create(field));
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
		for (int i= 0; i < accessedMethods.length; i++) {
			IMethod method= accessedMethods[i];
			if (! method.exists())
				continue;
			boolean isAccessible= 	pulledUpList.contains(method) || 
									deletedList.contains(method) ||
									declaredAbstractList.contains(method) ||
									canBeAccessedFrom(method, targetClass);
			if (! isAccessible){
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.method_not_accessible", //$NON-NLS-1$
					new String[]{createMethodLabel(method), createTypeLabel(targetClass)});
				result.addError(message, JavaSourceContext.create(method));
			} else if (isDeclaredInSkippedSuperclass(method, new NullProgressMonitor())){
				String[] keys= {createMethodLabel(method), createTypeLabel(targetClass)};
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.method_cannot_be_accessed", keys); //$NON-NLS-1$
				result.addError(message, JavaSourceContext.create(method));
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
			if (fCachedSkippedSuperclasses != null)
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
	
	private boolean canBeAccessedFrom(IMember member, IType newHome) throws JavaModelException{
		Assert.isTrue(!(member instanceof IInitializer));
		if (! member.exists())
			return false;
			
		if (newHome.equals(member.getDeclaringType()))
			return true;
			
		if (newHome.equals(member))
			return true;	
		
		if (JdtFlags.isPrivate(member))
			return false;
			
		if (member.getDeclaringType() == null){ //top level
			if (! (member instanceof IType))
				return false;

			if (JdtFlags.isPublic(member))
				return true;
			
			if (! JdtFlags.isPackageVisible(member))
				return false;
			
			return ((IType)member).getPackageFragment().equals(newHome.getPackageFragment());		
		} else {
			IType enclosingType= member.getDeclaringType();
			
			if (! canBeAccessedFrom(enclosingType, newHome))
				return false;

			if (enclosingType.equals(getDeclaringType())) //cannot reach down the hierachy
				return false;
			
			if (JdtFlags.isPublic(member))
				return true;
		 
			//FIX ME: protected and default treated the same
			return (enclosingType.getPackageFragment().equals(newHome.getPackageFragment()));		
		}
	}
	
	private RefactoringStatus checkMembersInSubclasses(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 3); //$NON-NLS-1$
		Set notDeletedMembers= getNotDeletedMembers(new SubProgressMonitor(pm, 1));
		checkAccessModifiers(result, notDeletedMembers);
		checkMethodReturnTypes(new SubProgressMonitor(pm, 1), result, notDeletedMembers);
		checkFieldTypes(new SubProgressMonitor(pm, 1), result);
		pm.done();
		return result;
	}

	private void checkMethodReturnTypes(IProgressMonitor pm, RefactoringStatus result, Set notDeletedMembers) throws JavaModelException {
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
				if (!notDeletedMembers.contains(matchingMethod))
					continue;
				if (returnType.equals(getReturnTypeName(matchingMethod)))
					continue;
				String[] keys= {createMethodLabel(matchingMethod), createTypeLabel(matchingMethod.getDeclaringType())};	
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.different_method_return_type", //$NON-NLS-1$
					keys);
				Context context= JavaSourceContext.create(matchingMethod.getCompilationUnit(), matchingMethod.getNameRange());
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
				Context context= JavaSourceContext.create(matchingField.getCompilationUnit(), matchingField.getSourceRange());					 
				result.addError(message, context);	
			}
		}
	}

	private Map getMatchingMemberMatching(IProgressMonitor pm, boolean includeMethodsToDeclareAbstract) throws JavaModelException {
		return getMatchingMembersMappingFromTypeAndAllSubtypes(getTypeHierarchyOfTargetClass(pm), getTargetClass(), includeMethodsToDeclareAbstract);
	}

	private void checkAccessModifiers(RefactoringStatus result, Set notDeletedMembers) throws JavaModelException {
		List toDeclareAbstract= Arrays.asList(fMethodsToDeclareAbstract);
		for (Iterator iter= notDeletedMembers.iterator(); iter.hasNext();) {
			IMember member= (IMember) iter.next();
			
			if (member.getElementType() == IJavaElement.METHOD && ! toDeclareAbstract.contains(member))
				checkMethodAccessModifiers(result, ((IMethod) member));
		}
	}
	
	private void checkMethodAccessModifiers(RefactoringStatus result, IMethod method) throws JavaModelException {
		Context errorContext= JavaSourceContext.create(method);
		
		if (JdtFlags.isStatic(method)){
				String[] keys= {createMethodLabel(method), createTypeLabel(method.getDeclaringType())};
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.static_method", //$NON-NLS-1$
					keys);
				result.addError(message, errorContext);
		 } 
		 if (isVisibilityLowerThanProtected(method)){
		 	String[] keys= {createMethodLabel(method), createTypeLabel(method.getDeclaringType())};
		 	String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.lower_visibility", //$NON-NLS-1$
		 		keys);
			result.addError(message, errorContext);	
		} 
	}

	private static String getReturnTypeName(IMethod method) throws JavaModelException {
		return Signature.toString(Signature.getReturnType(method.getSignature()).toString());
	}
	
	private static String getTypeName(IField field) throws JavaModelException {
		return Signature.toString(field.getTypeSignature());
	}

	private Set getNotDeletedMembers(IProgressMonitor pm) throws JavaModelException {
		Set matchingSet= new HashSet();
		pm.beginTask("", 2); //$NON-NLS-1$
		matchingSet.addAll(Arrays.asList(getMatchingElements(new SubProgressMonitor(pm, 1), true)));
		matchingSet.removeAll(Arrays.asList(getMembersToDelete(new SubProgressMonitor(pm, 1))));
		pm.done();
		return matchingSet;
	}
	
	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}
	
	private static boolean isVisibilityLowerThanProtected(IMember member)throws JavaModelException {
		return ! JdtFlags.isPublic(member) && ! JdtFlags.isProtected(member);
	}
	
	//--- change creation
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			return new CompositeChange(RefactoringCoreMessages.getString("PullUpRefactoring.Pull_Up"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally{
			pm.done();
			clearManagers();
		}
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.preview"), 4); //$NON-NLS-1$

			copyMembersToTargetClass(new SubProgressMonitor(pm, 1));

			if (needsAddingImportsToTargetCu())
				addImportsToTargetCu(new SubProgressMonitor(pm, 1));
			else	
				pm.worked(1);

			deleteMembers(new SubProgressMonitor(pm, 1));
	
			if (shouldMakeTargetClassAbstract())
				makeTargetClassAbstract();
	
			if (shouldAddMethodStubsToConcreteSubclassesOfTargetClass())
				addMethodStubsToNonAbstractSubclassesOfTargetClass(new SubProgressMonitor(pm, 1));
			else	
				pm.worked(1);
	
			createAbstractMethodsInTargetClass();
			increaseVisibilityOfMethodsDeclaredAbstract();

			TextChangeManager manager= new TextChangeManager();
			fillWithRewriteEdits(manager);
			return manager;
		} finally{
			pm.done();
		}
	}
	
	private void increaseVisibilityOfMethodsDeclaredAbstract() throws JavaModelException {
		ASTRewrite rewrite= fRewriteManager.getRewrite(getDeclaringCu());
		AST ast= getAST(rewrite);
		for (int i= 0; i < fMethodsToDeclareAbstract.length; i++) {
			IMethod method= fMethodsToDeclareAbstract[i];
			if (! needsToChangeVisibility(method))
				continue;
			MethodDeclaration methodDeclaration= getMethodDeclarationNode(method);
			MethodDeclaration newMethod= ast.newMethodDeclaration();
			newMethod.setExtraDimensions(methodDeclaration.getExtraDimensions());
			newMethod.setConstructor(methodDeclaration.isConstructor());
			newMethod.setModifiers(getModifiersWithUpdatedVisibility(method, methodDeclaration.getModifiers()));
			rewrite.markAsModified(methodDeclaration, newMethod);
		}
	}

	private void createAbstractMethodsInTargetClass() throws JavaModelException {
		TypeDeclaration targetClass= getTypeDeclarationNode(getTargetClass());
		ASTRewrite rewrite= getRewriteFor(targetClass);
		for (int i= 0; i < fMethodsToDeclareAbstract.length; i++) {
			IMethod method= fMethodsToDeclareAbstract[i];
			createAbstractMethodInTargetClass(method, rewrite, targetClass);
		}
	}
	
	private void createAbstractMethodInTargetClass(IMethod method, ASTRewrite rewrite, TypeDeclaration targetClass) throws JavaModelException {
		MethodDeclaration methodDeclaration= getMethodDeclarationNode(method);
		AST ast= getAST(rewrite);
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		newMethod.setBody(null);
		newMethod.setConstructor(false);
		newMethod.setExtraDimensions(methodDeclaration.getExtraDimensions());
		newMethod.setJavadoc(null);
		newMethod.setModifiers(createModifiersForAbstractDeclaration(method));
		newMethod.setName(createCopyOfSimpleName(methodDeclaration.getName(), ast));
		copyReturnType(rewrite, methodDeclaration, newMethod);		
		copyParameters(rewrite, methodDeclaration, newMethod);
		copyThrownExceptions(methodDeclaration, newMethod);
		targetClass.bodyDeclarations().add(newMethod);
		rewrite.markAsInserted(newMethod);
	}

	private int createModifiersForAbstractDeclaration(IMethod method) throws JavaModelException {
		MethodDeclaration methodDeclaration= getMethodDeclarationNode(method);
		int modifiers= Modifier.ABSTRACT | ASTNodes.clearFlag(Modifier.NATIVE, ASTNodes.clearFlag(Modifier.FINAL, methodDeclaration.getModifiers()));
		return getModifiersWithUpdatedVisibility(method, modifiers);
	}
	
	private int getModifiersWithUpdatedVisibility(IMember member, int modifiers) throws JavaModelException{
		if (needsToChangeVisibility(member))
			return ASTNodes.clearAccessModifiers(modifiers) | Modifier.PROTECTED;
		else
			return modifiers;	
	}
	
	private void addMethodStubsToNonAbstractSubclassesOfTargetClass(IProgressMonitor pm) throws CoreException {
		IType[] concreteSubclasses= getNonAbstractSubclassesOfTargetClass(pm);
		IType declaringType= getDeclaringType();
		IMethod[] methods= getAbstractMethodsAddedToTargetClass();
		IType[] typesToImport= getTypesReferencedInDeclarations(methods);
		for (int i= 0; i < concreteSubclasses.length; i++) {
			IType clazz= concreteSubclasses[i];
			if (clazz.equals(declaringType))
				continue;
			boolean anyStubAdded= false;
			for (int j= 0; j < methods.length; j++) {
				IMethod method= methods[j];
				if (null == JavaModelUtil.findMethod(method.getElementName(), method.getParameterTypes(), method.isConstructor(), clazz)){
					addStub(clazz, method);
					anyStubAdded= true;
				}
			}
			if (anyStubAdded)
				addImports(typesToImport, WorkingCopyUtil.getWorkingCopyIfExists(clazz.getCompilationUnit()));
		}
	}

	private void addStub(IType type, IMethod method) throws CoreException {
		TypeDeclaration typeDeclaration= getTypeDeclarationNode(type);
		MethodDeclaration methodDeclaration= getMethodDeclarationNode(method);
		ASTRewrite rewrite= getRewriteFor(typeDeclaration);
		AST ast= getAST(rewrite);
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		newMethod.setBody(getMethodStubBody(methodDeclaration, ast));
		newMethod.setConstructor(false);
		newMethod.setExtraDimensions(methodDeclaration.getExtraDimensions());
		newMethod.setModifiers(createModifiersForMethodStubs(method));
		newMethod.setName(createCopyOfSimpleName(methodDeclaration.getName(), ast));
		copyReturnType(rewrite, methodDeclaration, newMethod);		
		copyParameters(rewrite, methodDeclaration, newMethod);
		copyThrownExceptions(methodDeclaration, newMethod);
		newMethod.setJavadoc(createJavadocForStub(method, typeDeclaration.getName().getIdentifier(), newMethod, rewrite));
		typeDeclaration.bodyDeclarations().add(newMethod);
		rewrite.markAsInserted(newMethod);
	}

	private Block getMethodStubBody(MethodDeclaration method, AST ast) {
		Block body= ast.newBlock();
		Type returnType= method.getReturnType();
		ITypeBinding typeBinding= returnType.resolveBinding();
		if (typeBinding == null)
			return body;
		Type returnTypeInOurAST= ASTResolving.getTypeFromTypeBinding(ast, typeBinding);
		//cannot pass return type here - because it's from another ast
		Expression expression= ASTResolving.getInitExpression(returnTypeInOurAST, method.getExtraDimensions());
		if (expression != null) {
			ReturnStatement returnStatement= ast.newReturnStatement();
			returnStatement.setExpression(expression);
			body.statements().add(returnStatement);
		}
		return body;
	}

	private Javadoc createJavadocForStub(IMethod method, String enclosingTypeName, MethodDeclaration newMethodNode, ASTRewrite rewrite) throws CoreException {
		if (! fPreferenceSettings.createComments)
			return null;
		MethodDeclaration methodDeclaration= getMethodDeclarationNode(method);
		IMethodBinding binding= methodDeclaration.resolveBinding();
		if (binding == null)
			return null;
		ITypeBinding[] params= binding.getParameterTypes();	
		String fullTypeName= JavaModelUtil.getFullyQualifiedName(getTargetClass());
		String[] fullParamNames= new String[params.length];
		for (int i= 0; i < fullParamNames.length; i++) {
			fullParamNames[i]= Bindings.getFullyQualifiedName(params[i]);
		}
		ICompilationUnit cu= fAstManager.getCompilationUnit(rewrite.getRootNode());
		String comment= StubUtility.getMethodComment(cu, enclosingTypeName, newMethodNode, true, false, fullTypeName, fullParamNames, String.valueOf('\n'));
		return (Javadoc) rewrite.createPlaceholder(comment, ASTRewrite.JAVADOC);
	}

	private int createModifiersForMethodStubs(IMethod method) throws JavaModelException {
		MethodDeclaration methodDeclaration= getMethodDeclarationNode(method);
		int modifiers= ASTNodes.clearFlag(Modifier.NATIVE, ASTNodes.clearFlag(Modifier.ABSTRACT, methodDeclaration.getModifiers()));
		return getModifiersWithUpdatedVisibility(method, modifiers);
	}

	private IType[] getNonAbstractSubclassesOfTargetClass(IProgressMonitor pm) throws JavaModelException {
		ITypeHierarchy hierarchy= getTypeHierarchyOfTargetClass(pm);
		Set nonAbstractSubclasses= getNonAbstractSubclasses(hierarchy, getTargetClass());
		return (IType[]) nonAbstractSubclasses.toArray(new IType[nonAbstractSubclasses.size()]);
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
		return fCreateMethodStubs && (getAbstractMethodsAddedToTargetClass().length > 0);
	}

	private void makeTargetClassAbstract() throws JavaModelException {
		TypeDeclaration targetClass= getTypeDeclarationNode(getTargetClass());
		TypeDeclaration modifiedNode= targetClass.getAST().newTypeDeclaration();
		modifiedNode.setInterface(targetClass.isInterface());
		modifiedNode.setModifiers(targetClass.getModifiers() | Modifier.ABSTRACT);
		ASTRewrite rewrite= getRewriteFor(getTypeDeclarationNode(getTargetClass()));
		rewrite.markAsModified(targetClass, modifiedNode);
	}
	
	private ASTRewrite getRewriteFor(ASTNode node) {
		return fRewriteManager.getRewrite(ASTNodeMappingManager.getCompilationUnitNode(node));
	}

	private boolean shouldMakeTargetClassAbstract() throws JavaModelException {
		if (JdtFlags.isAbstract(getTargetClass()))
			return false;
		return getAbstractMethodsAddedToTargetClass().length > 0;	
	}
	
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

	private void copyMembersToTargetClass(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", fMembersToPullUp.length); //$NON-NLS-1$
		for (int i = fMembersToPullUp.length - 1; i >= 0; i--) { //backwards - to preserve method order
			if (fMembersToPullUp[i] instanceof IField)
				copyFieldToTargetClass((IField)fMembersToPullUp[i]);
			else if (fMembersToPullUp[i] instanceof IMethod)
				copyMethodToTargetClass((IMethod)fMembersToPullUp[i], new SubProgressMonitor(pm, 1));
			else if (fMembersToPullUp[i] instanceof IType)
				copyTypeToTargetClass((IType)fMembersToPullUp[i]);
			else 
				Assert.isTrue(false);
		}
		pm.done();
	}
	
	private void copyTypeToTargetClass(IType type) throws JavaModelException {
		ASTRewrite rewrite= fRewriteManager.getRewrite(getTargetCu());
		TypeDeclaration targetClass= getTypeDeclarationNode(getTargetClass());
		BodyDeclaration newType= createNewTypeDeclarationNode(type, rewrite);
		rewrite.markAsInserted(newType);
		targetClass.bodyDeclarations().add(newType);
	}

	private BodyDeclaration createNewTypeDeclarationNode(IType type, ASTRewrite rewrite) throws JavaModelException {
		TypeDeclaration oldType= getTypeDeclarationNode(type);
		return (BodyDeclaration)createPlaceholderForBodyDeclaration(oldType, rewrite, true);
	}

	private void copyMethodToTargetClass(IMethod method, IProgressMonitor pm) throws JavaModelException {
		ASTRewrite targetRewrite= fRewriteManager.getRewrite(getTargetCu());
		TypeDeclaration targetClass= getTypeDeclarationNode(getTargetClass());
		MethodDeclaration newMethod= createNewMethodDeclarationNode(method, targetRewrite, pm);
		targetRewrite.markAsInserted(newMethod);
		targetClass.bodyDeclarations().add(newMethod);
	}
	
	private MethodDeclaration createNewMethodDeclarationNode(IMethod method, ASTRewrite targetRewrite, IProgressMonitor pm) throws JavaModelException {
		MethodDeclaration oldMethod= getMethodDeclarationNode(method);
		AST ast= getAST(targetRewrite);
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		copyBodyOfPulledUpMethod(targetRewrite, method, oldMethod, newMethod, pm);
		newMethod.setConstructor(oldMethod.isConstructor());
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		copyJavadocNode(ast, oldMethod, newMethod);
		newMethod.setModifiers(getNewModifiers(method));
		newMethod.setName(createCopyOfSimpleName(oldMethod.getName(), ast));
		copyReturnType(targetRewrite, oldMethod, newMethod);
		copyParameters(targetRewrite, oldMethod, newMethod);
		copyThrownExceptions(oldMethod, newMethod);
		return newMethod;
	}

	private void copyBodyOfPulledUpMethod(ASTRewrite targetRewrite, IMethod method, MethodDeclaration oldMethod, MethodDeclaration newMethod, IProgressMonitor pm) throws JavaModelException {
		if (oldMethod.getBody() == null){
			newMethod.setBody(null);
			return;
		}
		Block oldBody= oldMethod.getBody();
		ISourceRange[] superRefOffsert= SourceRange.reverseSortByOffset(findSuperReferenceRanges(method, getSuperclassOfDeclaringClass(pm)));
		
		String oldBodySource= getBufferText(oldBody);
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
		Block newBody= (Block)targetRewrite.createPlaceholder(newBodySource, ASTRewrite.BLOCK);
		newMethod.setBody(newBody);
	}
	
	private static ISourceRange[] findSuperReferenceRanges(IMethod method, IType superType) throws JavaModelException{
		Assert.isNotNull(method);
		if (JdtFlags.isStatic(method))
			return new ISourceRange[0];
		SuperReferenceFinderVisitor visitor= new SuperReferenceFinderVisitor(method, superType);
		AST.parseCompilationUnit(method.getCompilationUnit(), true).accept(visitor);
		return visitor.getSuperReferenceRanges();
	}
	
	private void copyThrownExceptions(MethodDeclaration oldMethod, MethodDeclaration newMethod) {
		AST ast= newMethod.getAST();
		for (int i= 0, n= oldMethod.thrownExceptions().size(); i < n; i++) {
			Name oldExceptionName= (Name)oldMethod.thrownExceptions().get(i);
			newMethod.thrownExceptions().add(i, createCopyOfName(oldExceptionName, ast));
		}
	}
	
	private static Name createCopyOfName(Name name, AST ast){
		if (name.isSimpleName())
			return createCopyOfSimpleName((SimpleName)name, ast);
		else 
			return ast.newName(ASTNodes.getIdentifiers((QualifiedName)name));
	}
	
	private static SimpleName createCopyOfSimpleName(SimpleName simpleName, AST ast){
		return ast.newSimpleName(simpleName.getIdentifier());
	}
	
	private void copyParameters(ASTRewrite targetRewrite, MethodDeclaration oldMethod, MethodDeclaration newMethod) throws JavaModelException {
		for (int i= 0, n= oldMethod.parameters().size(); i < n; i++) {
			SingleVariableDeclaration oldParam= (SingleVariableDeclaration)oldMethod.parameters().get(i);
			SingleVariableDeclaration newParam= createPlaceholderForSingleVariableDeclaration(oldParam, targetRewrite);
			newMethod.parameters().add(i, newParam);
		}
	}
	
	private void copyReturnType(ASTRewrite targetRewrite, MethodDeclaration oldMethod, MethodDeclaration newMethod) throws JavaModelException {
		Type newReturnType= createPlaceholderForType(oldMethod.getReturnType(), targetRewrite);
		newMethod.setReturnType(newReturnType);
	}

	private static void copyJavadocNode(AST ast, BodyDeclaration oldDeclaration, BodyDeclaration newDeclaration) {
		if (oldDeclaration.getJavadoc() == null)
			return;
		Javadoc newJavadoc= ast.newJavadoc();
		newJavadoc.setComment(oldDeclaration.getJavadoc().getComment());
		newDeclaration.setJavadoc(newJavadoc);
	}
		
	private void copyFieldToTargetClass(IField field) throws JavaModelException {
		ASTRewrite rewrite= fRewriteManager.getRewrite(getTargetCu());
		TypeDeclaration targetClass= getTypeDeclarationNode(getTargetClass());
		FieldDeclaration newField= createNewFieldDeclarationNode(field, rewrite);
		rewrite.markAsInserted(newField);
		targetClass.bodyDeclarations().add(newField);
	}
	
	private FieldDeclaration createNewFieldDeclarationNode(IField field, ASTRewrite rewrite) throws JavaModelException {
		AST ast= getAST(rewrite);
		VariableDeclarationFragment oldFieldFragment= getFieldDeclarationFragmentNode(field);
		VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
		newFragment.setExtraDimensions(oldFieldFragment.getExtraDimensions());
		if (oldFieldFragment.getInitializer() != null){
			Expression newInitializer= createPlaceholderForExpression(oldFieldFragment.getInitializer(), rewrite);
			newFragment.setInitializer(newInitializer);
		}	
		newFragment.setName(createCopyOfSimpleName(oldFieldFragment.getName(), ast));
		FieldDeclaration newField= ast.newFieldDeclaration(newFragment);
		FieldDeclaration oldField= getFieldDeclarationNode(field);
		copyJavadocNode(ast, oldField, newField);
		newField.setModifiers(getNewModifiers(field));
		
		Type newType= createPlaceholderForType(oldField.getType(), rewrite);
		newField.setType(newType);
		return newField;
	}
	
	private static boolean needsToChangeVisibility(IMember method) throws JavaModelException {
		return ! JdtFlags.isPublic(method) && ! JdtFlags.isProtected(method);
	}

	private int getNewModifiers(IMember member) throws JavaModelException {
		return getModifiersWithUpdatedVisibility(member, getModifiers(member));
	}
	
	private int getModifiers(IMember member) throws JavaModelException{
		if (member instanceof IField)
			return getFieldDeclarationNode((IField)member).getModifiers();	
		else if (member instanceof IMethod)
			return getMethodDeclarationNode((IMethod)member).getModifiers();	
		else if (member instanceof IType)
			return getTypeDeclarationNode((IType)member).getModifiers();	
		Assert.isTrue(false);	
		return Modifier.NONE;
	}
	
	private void addImportsToTargetCu(IProgressMonitor pm) throws CoreException {
		addImports(getTypesThatNeedToBeImportedInTargetCu(pm), getTargetWorkingCopy());
	}

	private void addImports(IType[] typesToImport, ICompilationUnit cu) throws JavaModelException {
		for (int i= 0; i < typesToImport.length; i++) {
			fImportEditManager.addImportTo(typesToImport[i], cu);
		}
	}

	private void deleteMembers(IProgressMonitor pm) throws JavaModelException {
		IMember[] membersToDelete= getMembersToDelete(pm);
		for (int i = 0; i < membersToDelete.length; i++) {
			if (membersToDelete[i] instanceof IField)
				deleteField((IField)membersToDelete[i]);
			else if (membersToDelete[i] instanceof IMethod)
				deleteMethod((IMethod)membersToDelete[i]);
			else if (membersToDelete[i] instanceof IType)
				deleteType((IType)membersToDelete[i]);
			else Assert.isTrue(false);	
		}
	}

	private void deleteType(IType type) throws JavaModelException {
		TypeDeclaration td= getTypeDeclarationNode(type);
		fRewriteManager.getRewrite(getWorkingCopy(type)).markAsRemoved(td);
	}

	private void deleteField(IField field) throws JavaModelException{
		FieldDeclaration fd= getFieldDeclarationNode(field);
		fRewriteManager.getRewrite(getWorkingCopy(field)).markAsRemoved(fd);
	}

	private void deleteMethod(IMethod method) throws JavaModelException{
		MethodDeclaration md= getMethodDeclarationNode(method);
		fRewriteManager.getRewrite(getWorkingCopy(method)).markAsRemoved(md);
	}

	private void fillWithRewriteEdits(TextChangeManager manager) throws JavaModelException, CoreException {
		CompilationUnit[] cuNodes= fRewriteManager.getAllCompilationUnitNodes();
		for (int i= 0; i < cuNodes.length; i++) {
			CompilationUnit cuNode= cuNodes[i];
			ASTRewrite rewrite= fRewriteManager.getRewrite(cuNode);
			TextBuffer textBuffer= TextBuffer.create(fAstManager.getCompilationUnit(cuNode).getBuffer().getContents());
			MultiTextEdit resultingEdits= new MultiTextEdit();
			rewrite.rewriteNode(textBuffer, resultingEdits, null);
			ICompilationUnit cu= fAstManager.getCompilationUnit(cuNode);
			TextChange textChange= manager.get(cu);
			if (fImportEditManager.hasImportEditFor(cu))
				resultingEdits.add(fImportEditManager.getImportEdit(cu));
			textChange.addTextEdit(RefactoringCoreMessages.getString("PullUpRefactoring.pull_up_class_members"), resultingEdits); //$NON-NLS-1$
			rewrite.removeModifications();
		}
	}

	private ICompilationUnit getWorkingCopy(IMember member){
		return WorkingCopyUtil.getWorkingCopyIfExists(member.getCompilationUnit());
	}
	
	private boolean needsAddingImportsToTargetCu() throws JavaModelException {
		return ! getTargetCu().equals(getDeclaringCu());
	}
	
	private ICompilationUnit getDeclaringCu() {
		return getDeclaringType().getCompilationUnit();
	}

	private ICompilationUnit getTargetWorkingCopy() {
		return WorkingCopyUtil.getWorkingCopyIfExists(getTargetCu());
	}

	private ICompilationUnit getTargetCu() {
		return getTargetClass().getCompilationUnit();
	}
	
	private IType[] getTypesThatNeedToBeImportedInTargetCu(IProgressMonitor pm) throws JavaModelException {
		IType[] typesInPulledUpMembers= getTypeReferencedInPulledUpMembers(pm);
		IType[] typesInMethodsDeclaredAbstract= getTypesReferencedInDeclarations(fMethodsToDeclareAbstract);
		List result= new ArrayList(typesInMethodsDeclaredAbstract.length + typesInPulledUpMembers.length);
		result.addAll(Arrays.asList(typesInMethodsDeclaredAbstract));
		result.addAll(Arrays.asList(typesInPulledUpMembers));
		return (IType[]) result.toArray(new IType[result.size()]);
	}
	
	private IType[] getTypeReferencedInPulledUpMembers(IProgressMonitor pm) throws JavaModelException {
		if (fTypesReferencedInPulledUpMembers == null)
			fTypesReferencedInPulledUpMembers= ReferenceFinderUtil.getTypesReferencedIn(fMembersToPullUp, pm);
		return fTypesReferencedInPulledUpMembers;
	}
	
	private IType[] getTypesReferencedInDeclarations(IMethod[] methods) throws JavaModelException{
		ITypeBinding[] referencedTypesBindings= ReferenceFinderUtil.getTypesReferencedInDeclarations(methods, fAstManager);
		List types= new ArrayList(referencedTypesBindings.length);
		IJavaProject proj= getDeclaringType().getJavaProject();
		for (int i= 0; i < referencedTypesBindings.length; i++) {
			ITypeBinding typeBinding= referencedTypesBindings[i];
			if (typeBinding == null)
				continue;
			if (typeBinding.isArray())
				typeBinding= typeBinding.getElementType();
			IType type= Binding2JavaModel.find(typeBinding, proj);
			if (type != null)
				types.add(type);
		}
		return (IType[]) types.toArray(new IType[types.size()]);
	}
	
	private static AST getAST(ASTRewrite rewrite){
		return rewrite.getRootNode().getAST();
	}
	
	private String getBufferText(ASTNode node) throws JavaModelException{
		return fAstManager.getCompilationUnit(node).getBuffer().getText(node.getStartPosition(), node.getLength());
	}

	private String getNewText(ASTNode astNode, ASTRewrite rewrite, boolean removeIndentation) throws JavaModelException {
		String bufferText= getBufferText(astNode);
		if (removeIndentation)
			return getUnindentedText(bufferText, rewrite);
		else
			return bufferText;
	}
	
	private String getUnindentedText(String bufferText, ASTRewrite rewrite) throws JavaModelException {
		String[] lines= Strings.convertIntoLines(bufferText);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		return Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(fAstManager.getCompilationUnit(rewrite.getRootNode())));
	}
			
	//-----declaration node finders -----
	
	private FieldDeclaration getFieldDeclarationNode(IField field) throws JavaModelException {
		return ASTNodeSearchUtil.getFieldDeclarationNode(field, fAstManager);
	}

	private VariableDeclarationFragment getFieldDeclarationFragmentNode(IField field) throws JavaModelException {
		return ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, fAstManager);
	}

	private TypeDeclaration getTypeDeclarationNode(IType type) throws JavaModelException {
		return ASTNodeSearchUtil.getTypeDeclarationNode(type, fAstManager);
	}
	
	private MethodDeclaration getMethodDeclarationNode(IMethod method) throws JavaModelException {
		return ASTNodeSearchUtil.getMethodDeclarationNode(method, fAstManager);
	}
	
	//---- placeholder creators ----

	private Expression createPlaceholderForExpression(Expression expression, ASTRewrite rewrite) throws JavaModelException{
		return (Expression)rewrite.createPlaceholder(getBufferText(expression), ASTRewrite.EXPRESSION);
	}
			
	private SingleVariableDeclaration createPlaceholderForSingleVariableDeclaration(SingleVariableDeclaration declaration, ASTRewrite rewrite) throws JavaModelException{
		return (SingleVariableDeclaration)rewrite.createPlaceholder(getBufferText(declaration), ASTRewrite.SINGLEVAR_DECLARATION);
	}
	
	private Type createPlaceholderForType(Type type, ASTRewrite rewrite) throws JavaModelException{
		return (Type)rewrite.createPlaceholder(getBufferText(type), ASTRewrite.TYPE);
	}

	private BodyDeclaration createPlaceholderForBodyDeclaration(BodyDeclaration bodyDeclaration, ASTRewrite rewrite, boolean removeIndentation) throws JavaModelException{
		String newBufferText= getNewText(bodyDeclaration, rewrite, removeIndentation);
		return (BodyDeclaration)rewrite.createPlaceholder(newBufferText, ASTRewrite.BODY_DECLARATION);
	}
}

