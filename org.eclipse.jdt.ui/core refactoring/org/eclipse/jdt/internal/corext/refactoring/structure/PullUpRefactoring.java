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
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
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
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
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

	/**
	 * AST node visitor which performs the actual mapping.
	 */
	public static class PullUpAstNodeMapper extends TypeVariableMapper {

		/** Are we in an anonymous class declaration? */
		protected boolean fAnonymousClassDeclaration= false;

		/** The qualified type name where the super references are referring to */
		protected final String fQualifiedName;

		/** The source compilation unit rewrite to use */
		protected final CompilationUnitRewrite fSourceRewriter;

		/** The target compilation unit rewrite to use */
		protected final CompilationUnitRewrite fTargetRewriter;

		/** Are we in a type declaration statement? */
		protected boolean fTypeDeclarationStatement= false;

		/**
		 * Creates a new pull up ast node mapper.
		 * 
		 * @param sourceRewriter the source compilation unit rewrite to use
		 * @param targetRewriter the target compilation unit rewrite to use
		 * @param rewrite the AST rewrite to use
		 * @param type the super reference type
		 * @param mapping the type variable mapping
		 */
		public PullUpAstNodeMapper(final CompilationUnitRewrite sourceRewriter, final CompilationUnitRewrite targetRewriter, final ASTRewrite rewrite, final IType type, final TypeVariableMaplet[] mapping) {
			super(rewrite, mapping);
			Assert.isNotNull(rewrite);
			Assert.isNotNull(type);
			fSourceRewriter= sourceRewriter;
			fTargetRewriter= targetRewriter;
			fQualifiedName= JavaModelUtil.getFullyQualifiedName(type);
		}

		public final void endVisit(final AnonymousClassDeclaration node) {
			fAnonymousClassDeclaration= false;
			super.endVisit(node);
		}

		public final void endVisit(final TypeDeclarationStatement node) {
			fTypeDeclarationStatement= false;
			super.endVisit(node);
		}

		public final boolean visit(final AnonymousClassDeclaration node) {
			fAnonymousClassDeclaration= true;
			return super.visit(node);
		}

		public final boolean visit(final SuperFieldAccess node) {
			if (!fAnonymousClassDeclaration && !fTypeDeclarationStatement) {
				final AST ast= node.getAST();
				final FieldAccess access= ast.newFieldAccess();
				access.setExpression(ast.newThisExpression());
				access.setName(ast.newSimpleName(node.getName().getIdentifier()));
				fRewrite.replace(node, access, null);
				if (!fSourceRewriter.getCu().equals(fTargetRewriter.getCu()))
					fSourceRewriter.getImportRemover().registerRemovedNode(node);
				return true;
			}
			return false;
		}

		public final boolean visit(final SuperMethodInvocation node) {
			if (!fAnonymousClassDeclaration && !fTypeDeclarationStatement) {
				final IBinding name= node.getName().resolveBinding();
				if (name != null && name.getKind() == IBinding.METHOD) {
					final ITypeBinding type= ((IMethodBinding) name).getDeclaringClass();
					
					// TODO replace by comparison type.getJavaElement().equals(super_reference_type) (see 78087)
					
					if (type != null && !fQualifiedName.equals(Bindings.getFullyQualifiedName(type)))
						return true;
				}
				final AST ast= node.getAST();
				final ThisExpression expression= ast.newThisExpression();
				final MethodInvocation invocation= ast.newMethodInvocation();
				final SimpleName simple= ast.newSimpleName(node.getName().getIdentifier());
				invocation.setName(simple);
				invocation.setExpression(expression);
				final List arguments= (List) node.getStructuralProperty(SuperMethodInvocation.ARGUMENTS_PROPERTY);
				if (arguments != null && arguments.size() > 0) {
					final ListRewrite rewriter= fRewrite.getListRewrite(invocation, MethodInvocation.ARGUMENTS_PROPERTY);
					rewriter.insertLast(rewriter.createCopyTarget((ASTNode) arguments.get(0), (ASTNode) arguments.get(arguments.size() - 1)), null);
				}
				fRewrite.replace(node, invocation, null);
				if (!fSourceRewriter.getCu().equals(fTargetRewriter.getCu()))
					fSourceRewriter.getImportRemover().registerRemovedNode(node);
				return true;
			}
			return false;
		}

		public final boolean visit(final TypeDeclarationStatement node) {
			fTypeDeclarationStatement= true;
			return super.visit(node);
		}
	}

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
	
	public static boolean isAvailable(IMember[] members) throws JavaModelException {
		if (isOneTypeWithPullableMembers(members))
			return true;
		return members != null && members.length != 0 && areAllPullable(members) && haveCommonDeclaringType(members);
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
			return JavaElementUtil.merge(JavaElementUtil.merge(matchingFields, typesToDelete), fMethodsToDelete);
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
		IMember[] members= type.getFields();
		for (int i= 0; i < members.length; i++) {
			if (isPullable(members[i]))
				list.add(members[i]);
		}
		IMember[] members1= type.getMethods();
		for (int i= 0; i < members1.length; i++) {
			if (isPullable(members1[i]))
				list.add(members1[i]);
		}
		IMember[] members2= type.getTypes();
		for (int i= 0; i < members2.length; i++) {
			if (isPullable(members2[i]))
				list.add(members2[i]);
		}
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}

	private static boolean isPullable(IMember member) throws JavaModelException {
		if (member.getElementType() != IJavaElement.METHOD && member.getElementType() != IJavaElement.FIELD && member.getElementType() != IJavaElement.TYPE)
			return false;
		if (JdtFlags.isEnum(member))
			return false;
		if (!Checks.isAvailable(member))
			return false;
		if (member instanceof IType) {
			if (!JdtFlags.isStatic(member))
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
	
	public IType[] getPossibleTargetClasses(RefactoringStatus status, IProgressMonitor pm) throws JavaModelException {
		IType[] superClasses= getDeclaringType().newSupertypeHierarchy(pm).getAllSuperclasses(getDeclaringType());
		List superClassList= new ArrayList(superClasses.length);
		int binary= 0;
		for (int i= 0; i < superClasses.length; i++) {
			IType superclass= superClasses[i];
			if (superclass != null && superclass.exists() && ! superclass.isReadOnly() && ! superclass.isBinary() && ! "java.lang.Object".equals(superclass.getFullyQualifiedName())) { //$NON-NLS-1$
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
			Map matching= getMatchingMembersMappingFromTypeAndAllSubtypes(getTypeHierarchyOfTargetClass(pm), getTargetClass(), includeMethodsToDeclareAbstract);
			for (Iterator iter= matching.keySet().iterator(); iter.hasNext();) {
				IMember	key= (IMember) iter.next();
				Assert.isTrue(!key.getDeclaringType().equals(targetClass));
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
		IProgressMonitor pm1= new SubProgressMonitor(pm, 1);
		pm1.beginTask("", 2); //$NON-NLS-1$
		IMethod[] requiredMethods= ReferenceFinderUtil.getMethodsReferencedIn(new IJavaElement[]{member}, new SubProgressMonitor(pm1, 1));
		SubProgressMonitor pm2= new SubProgressMonitor(pm1, 1);
		pm2.beginTask("", requiredMethods.length); //$NON-NLS-1$
		boolean isStatic= JdtFlags.isStatic(member);
		for (int i= 0; i < requiredMethods.length; i++) {
			IMethod requiredMethod= requiredMethods[i];
			if (isStatic && ! JdtFlags.isStatic(requiredMethod))
				continue;
			if (isRequiredPullableMember(queue, requiredMethod) && ! isVirtualAccessibleFromTargetClass(requiredMethod, new SubProgressMonitor(pm2, 1)))
				queue.add(requiredMethod);
		}
		pm2.done();
		IField[] requiredFields= ReferenceFinderUtil.getFieldsReferencedIn(new IJavaElement[]{member}, new SubProgressMonitor(pm, 1));
		isStatic= JdtFlags.isStatic(member);
		for (int i= 0; i < requiredFields.length; i++) {
			IField requiredField= requiredFields[i];
			if (isStatic && ! JdtFlags.isStatic(requiredField))
				continue;			
			if (isRequiredPullableMember(queue, requiredField))
				queue.add(requiredField);
		}
		IType[] requiredTypes= ReferenceFinderUtil.getTypesReferencedIn(new IJavaElement[]{member}, new SubProgressMonitor(pm, 1));
		isStatic= JdtFlags.isStatic(member);
		for (int i= 0; i < requiredTypes.length; i++) {
			IType requiredType= requiredTypes[i];
			if (isStatic && ! JdtFlags.isStatic(requiredType))
				continue;			
			if (isRequiredPullableMember(queue, requiredType))
				queue.add(requiredType);
		}
		pm.done();
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
			Map map= getMatchingMembersMapping(subTypes[i]);
			mergeSetsForCommonKeys(result, map);
			putAllThatDoNotExistInResultYet(result, map);
		}
		if (includeMethodsToDeclareAbstract)
			return result;
			
		for (int i= 0; i < fMethodsToDeclareAbstract.length; i++) {
			if (result.containsKey(fMethodsToDeclareAbstract[i]))
				result.remove(fMethodsToDeclareAbstract[i]);
		}
		return result;
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
			if (! JdtFlags.isAbstract(getTargetClass()) && 
			   getAbstractMethodsAddedToTargetClass().length > 0)
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
						case 0:
							break;
						case 1:
							status.addError(RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Type_variable_not_available", new String[] { unmapped[0], declaring.getSuperclassName()}), JavaStatusContext.create(member)); //$NON-NLS-1$
							break;
						case 2:
							status.addError(RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Type_variable2_not_available", new String[] { unmapped[0], unmapped[1], declaring.getSuperclassName()}), JavaStatusContext.create(member)); //$NON-NLS-1$
							break;
						case 3:
							status.addError(RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Type_variable3_not_available", new String[] { unmapped[0], unmapped[1], unmapped[2], declaring.getSuperclassName()}), JavaStatusContext.create(member)); //$NON-NLS-1$
							break;
						default:
							status.addError(RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Type_variables_not_available", new String[] { declaring.getSuperclassName()}), JavaStatusContext.create(member)); //$NON-NLS-1$
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
			} else if (getSkippedSuperclasses(new NullProgressMonitor()).contains(field.getDeclaringType())){
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
			} else if (getSkippedSuperclasses(new NullProgressMonitor()).contains(method.getDeclaringType())){
				String[] keys= {createMethodLabel(method), createTypeLabel(targetClass)};
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.method_cannot_be_accessed", keys); //$NON-NLS-1$
				result.addError(message, JavaStatusContext.create(method));
			}	
		}
		pm.done();
		return result;
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
		for (Iterator iter= notDeletedMembers.iterator(); iter.hasNext();) {
			IMember member= (IMember) iter.next();
			if (getTargetClass().equals(member.getDeclaringType()))
				notDeletedMembersInTargetType.add(member);
			else
				notDeletedMembersInSubtypes.add(member);
		}
		checkMembersInDestinationType(result, notDeletedMembersInTargetType);
		checkAccessModifiers(result, notDeletedMembersInSubtypes);
		checkMethodReturnTypes(new SubProgressMonitor(pm, 1), result, notDeletedMembersInSubtypes);
		checkFieldTypes(new SubProgressMonitor(pm, 1), result);
		pm.done();
		return result;
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
		Map mapping= getMatchingMembersMappingFromTypeAndAllSubtypes(getTypeHierarchyOfTargetClass(pm), getTargetClass(), true);
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
		Map mapping= getMatchingMembersMappingFromTypeAndAllSubtypes(getTypeHierarchyOfTargetClass(pm), getTargetClass(), true);
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
	
	private void checkAccessModifiers(RefactoringStatus result, Set notDeletedMembersInSubtypes) throws JavaModelException {
		List toDeclareAbstract= Arrays.asList(fMethodsToDeclareAbstract);
		for (Iterator iter= notDeletedMembersInSubtypes.iterator(); iter.hasNext();) {
			IMember member= (IMember) iter.next();
			if (member.getElementType() == IJavaElement.METHOD && ! toDeclareAbstract.contains(member))
				checkMethodAccessModifiers(result, ((IMethod) member));
		}
	}
	
	private void checkMethodAccessModifiers(RefactoringStatus result, IMethod method) throws JavaModelException {
		if (! JdtFlags.isPublic(method) && ! JdtFlags.isProtected(method)){
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

	private TextChangeManager createChangeManager(IProgressMonitor monitor, RefactoringStatus status) throws CoreException {
		try {
			monitor.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.preview"), 4); //$NON-NLS-1$
			final CompilationUnitRewrite sourceRewriter= new CompilationUnitRewrite(getDeclaringType().getCompilationUnit());
			final CompilationUnitRewrite targetRewriter= new CompilationUnitRewrite(getTargetClass().getCompilationUnit());
			addImportsToCu(targetRewriter, getTypesThatNeedToBeImportedInTargetCu(new SubProgressMonitor(monitor, 1), sourceRewriter.getRoot()));
			final TextChangeManager manager= new TextChangeManager();
			final Map deleteMap= createMembersToDeleteMap(new SubProgressMonitor(monitor, 1));
			final Map effectedMap= createNonAbstractSubclassesMapping(new SubProgressMonitor(monitor, 1));
			final ICompilationUnit[] involvedUnits= getInvolvedCompilationUnits(new SubProgressMonitor(monitor, 1));
			final IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 1);
			subMonitor.beginTask("", involvedUnits.length * 6); //$NON-NLS-1$
			ICompilationUnit currentUnit= null;
			CompilationUnitRewrite unitRewriter= null;
			for (int index= 0; index < involvedUnits.length; index++) {
				currentUnit= involvedUnits[index];
				if (!(getDeclaringType().getCompilationUnit().equals(currentUnit) || getTargetClass().getCompilationUnit().equals(currentUnit) || deleteMap.containsKey(currentUnit) || effectedMap.containsKey(currentUnit))) {
					subMonitor.worked(6);
					continue;
				}
				if (currentUnit.equals(sourceRewriter.getCu()))
					unitRewriter= sourceRewriter;
				else if (currentUnit.equals(targetRewriter.getCu()))
					unitRewriter= targetRewriter;
				else
					unitRewriter= new CompilationUnitRewrite(currentUnit);
				if (deleteMap.containsKey(currentUnit))
					deleteDeclarationNodes(sourceRewriter, sourceRewriter.getCu().equals(targetRewriter.getCu()), unitRewriter, (List) deleteMap.get(currentUnit));
				if (currentUnit.equals(getTargetClass().getCompilationUnit())) {
					if (!JdtFlags.isAbstract(getTargetClass()) && getAbstractMethodsAddedToTargetClass().length > 0) {
						TypeDeclaration targetClass1= ASTNodeSearchUtil.getTypeDeclarationNode(getTargetClass(), unitRewriter.getRoot());
						ModifierRewrite.create(unitRewriter.getASTRewrite(), targetClass1).setModifiers(targetClass1.getModifiers() | Modifier.ABSTRACT, unitRewriter.createGroupDescription(RefactoringCoreMessages.getString("PullUpRefactoring.make_target_abstract"))); //$NON-NLS-1$
					}
					final TypeVariableMaplet[] mapping= TypeVariableUtil.subTypeToSuperType(getDeclaringType(), getTargetClass());
					copyMembersToTargetClass(sourceRewriter, unitRewriter, sourceRewriter.getRoot(), mapping, new SubProgressMonitor(subMonitor, 1), status);
					IProgressMonitor pm= new SubProgressMonitor(subMonitor, 1);
					TypeDeclaration targetClass= ASTNodeSearchUtil.getTypeDeclarationNode(getTargetClass(), unitRewriter.getRoot());
					pm.beginTask("", fMethodsToDeclareAbstract.length); //$NON-NLS-1$
					for (int i= 0; i < fMethodsToDeclareAbstract.length; i++)
						createAbstractMethodInTargetClass(fMethodsToDeclareAbstract[i], sourceRewriter.getRoot(), targetClass, mapping, unitRewriter, new SubProgressMonitor(pm, 1), status);
					pm.done();
				} else
					subMonitor.worked(2);
				if (currentUnit.equals(sourceRewriter.getCu()))
					increaseVisibilityOfMethodsDeclaredAbstract(unitRewriter.getASTRewrite(), unitRewriter.getRoot(), new SubProgressMonitor(subMonitor, 2), status);
				else
					subMonitor.worked(2);
				if (effectedMap.containsKey(currentUnit))
					addMethodStubsToNonAbstractSubclassesOfTargetClass((List) effectedMap.get(currentUnit), sourceRewriter.getRoot(), unitRewriter, new SubProgressMonitor(subMonitor, 2), status);
				manager.manage(currentUnit, unitRewriter.createChange());
				if (subMonitor.isCanceled())
					throw new OperationCanceledException();
			}
			subMonitor.done();
			return manager;
		} finally {
			monitor.done();
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

	private ICompilationUnit[] getInvolvedCompilationUnits(IProgressMonitor pm) throws JavaModelException {
		IType[] allSubtypes= getTypeHierarchyOfTargetClass(pm).getAllSubtypes(getTargetClass());
		Set result= new HashSet(allSubtypes.length);
		for (int i= 0; i < allSubtypes.length; i++) {
			result.add(allSubtypes[i].getCompilationUnit());
		}
		result.add(getTargetClass().getCompilationUnit());
		return (ICompilationUnit[]) result.toArray(new ICompilationUnit[result.size()]);
	}

	private void increaseVisibilityOfMethodsDeclaredAbstract(ASTRewrite rewrite, CompilationUnit cuNode, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		for (int i= 0; i < fMethodsToDeclareAbstract.length; i++) {
			IMethod method= fMethodsToDeclareAbstract[i];
			if (! needsToChangeVisibility(method, false, pm, status))
				continue;
			MethodDeclaration methodDeclaration= ASTNodeSearchUtil.getMethodDeclarationNode(method, cuNode);
			ModifierRewrite.create(rewrite, methodDeclaration).setModifiers(getModifiersWithUpdatedVisibility(method, methodDeclaration.getModifiers(), pm, false, status), null);
		}
	}

	private void createAbstractMethodInTargetClass(IMethod sourceMethod, CompilationUnit declaringCuNode, TypeDeclaration targetClass, TypeVariableMaplet[] mapping, CompilationUnitRewrite targetRewrite, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		MethodDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		AST ast= targetRewrite.getASTRewrite().getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		newMethod.setBody(null);
		newMethod.setConstructor(false);
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		newMethod.setJavadoc(null);
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getModifiersWithUpdatedVisibility(sourceMethod, Modifier.ABSTRACT | JdtFlags.clearFlag(Modifier.NATIVE | Modifier.FINAL, sourceMethod.getFlags()), pm, false, status)));
		newMethod.setName(((SimpleName)ASTNode.copySubtree(ast, oldMethod.getName())));
		copyReturnType(targetRewrite.getASTRewrite(), getDeclaringType().getCompilationUnit(), oldMethod, newMethod, mapping);
		copyParameters(targetRewrite.getASTRewrite(), getDeclaringType().getCompilationUnit(), oldMethod, newMethod, mapping);
		copyThrownExceptions(oldMethod, newMethod);
		targetRewrite.getASTRewrite().getListRewrite(targetClass, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, targetClass.bodyDeclarations()), targetRewrite.createGroupDescription(RefactoringCoreMessages.getString("PullUpRefactoring.add_abstract_method"))); //$NON-NLS-1$
	}

	private int getModifiersWithUpdatedVisibility(IMember member, int modifiers, IProgressMonitor pm, boolean considerReferences, RefactoringStatus status) throws JavaModelException {
		if (needsToChangeVisibility(member, considerReferences, pm, status))
			return JdtFlags.clearAccessModifiers(modifiers) | Modifier.PROTECTED;

		return modifiers;
	}
	
	//concreteSubclasses: List of IType
	private void addMethodStubsToNonAbstractSubclassesOfTargetClass(List concreteSubclasses, CompilationUnit declaringCuNode, CompilationUnitRewrite unitRewriter, IProgressMonitor monitor, RefactoringStatus status) throws CoreException {
		IType declaringType= getDeclaringType();
		IMethod[] methods= getAbstractMethodsAddedToTargetClass();
		IType[] typesToImport= getTypesReferencedInDeclarations(methods, declaringCuNode);
		monitor.beginTask("", concreteSubclasses.size()); //$NON-NLS-1$
		for (Iterator iter= concreteSubclasses.iterator(); iter.hasNext();) {
			IType clazz= (IType)iter.next();
			if (clazz.equals(declaringType))
				continue;
			boolean anyStubAdded= false;
			TypeDeclaration classToCreateStubIn= ASTNodeSearchUtil.getTypeDeclarationNode(clazz, unitRewriter.getRoot());
			ICompilationUnit cuToCreateStubIn= clazz.getCompilationUnit();
			IProgressMonitor subPm= new SubProgressMonitor(monitor, 1);
			subPm.beginTask("", methods.length); //$NON-NLS-1$
			for (int j= 0; j < methods.length; j++) {
				IMethod method= methods[j];
				if (null == JavaModelUtil.findMethod(method.getElementName(), method.getParameterTypes(), method.isConstructor(), clazz)){
					addMethodStubForAbstractMethod(method, declaringCuNode, classToCreateStubIn, cuToCreateStubIn, unitRewriter, new SubProgressMonitor(subPm, 1), status);
					anyStubAdded= true;
				}
			}
			subPm.done();
			if (anyStubAdded)
				addImportsToCu(unitRewriter, typesToImport);
		}
		monitor.done();
	}

	private void addMethodStubForAbstractMethod(IMethod sourceMethod, CompilationUnit declaringCuNode, TypeDeclaration typeToCreateStubIn, ICompilationUnit newCu, CompilationUnitRewrite rewriter, IProgressMonitor pm, RefactoringStatus status) throws CoreException {
		MethodDeclaration methodToCreateStubFor= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		final AST ast= rewriter.getRoot().getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		newMethod.setBody(getMethodStubBody(methodToCreateStubFor, ast));
		newMethod.setConstructor(false);
		newMethod.setExtraDimensions(methodToCreateStubFor.getExtraDimensions());
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getModifiersWithUpdatedVisibility(sourceMethod, JdtFlags.clearFlag(Modifier.NATIVE | Modifier.ABSTRACT, methodToCreateStubFor.getModifiers()), pm, false, status)));
		newMethod.setName(((SimpleName)ASTNode.copySubtree(ast, methodToCreateStubFor.getName())));
		TypeVariableMaplet[] mapping= TypeVariableUtil.composeMappings(TypeVariableUtil.subTypeToSuperType(getDeclaringType(), getTargetClass()), TypeVariableUtil.superTypeToInheritedType(getTargetClass(), Bindings.findType(typeToCreateStubIn.resolveBinding(), newCu.getJavaProject())));
		copyReturnType(rewriter.getASTRewrite(), getDeclaringType().getCompilationUnit(), methodToCreateStubFor, newMethod, mapping);
		copyParameters(rewriter.getASTRewrite(), getDeclaringType().getCompilationUnit(), methodToCreateStubFor, newMethod, mapping);
		copyThrownExceptions(methodToCreateStubFor, newMethod);
		newMethod.setJavadoc(createJavadocForStub(typeToCreateStubIn.getName().getIdentifier(), methodToCreateStubFor, newMethod, newCu, rewriter.getASTRewrite()));
		rewriter.getASTRewrite().getListRewrite(typeToCreateStubIn, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, typeToCreateStubIn.bodyDeclarations()), rewriter.createGroupDescription(RefactoringCoreMessages.getString("PullUpRefactoring.add_method_stub"))); //$NON-NLS-1$
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

	//ICompilationUnit -> List of IType
	private Map createNonAbstractSubclassesMapping(IProgressMonitor pm) throws JavaModelException{
		if (! (fCreateMethodStubs && getAbstractMethodsAddedToTargetClass().length > 0))
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

	private void copyMembersToTargetClass(CompilationUnitRewrite sourceRewrite, CompilationUnitRewrite targetRewrite, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		TypeDeclaration targetClass= ASTNodeSearchUtil.getTypeDeclarationNode(getTargetClass(), targetRewrite.getRoot());
		fMembersToMove= JavaElementUtil.sortByOffset(fMembersToMove); // preserve member order
		pm.beginTask("", fMembersToMove.length); //$NON-NLS-1$
		for (int i= 0; i < fMembersToMove.length; i++) {
			IMember member= fMembersToMove[i];
			if (member instanceof IField) {
				IField field= (IField)member;
				IProgressMonitor pm1= new SubProgressMonitor(pm, 1);
				FieldDeclaration newField= createNewFieldDeclarationNode(targetRewrite.getASTRewrite(), declaringCuNode, field, mapping, pm1, status, getModifiersWithUpdatedVisibility(field, field.getFlags(), pm1, true, status));
				targetRewrite.getASTRewrite().getListRewrite(targetClass, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(newField, ASTNodes.getInsertionIndex(newField, targetClass.bodyDeclarations()), targetRewrite.createGroupDescription(RefactoringCoreMessages.getString("HierarchyRefactoring.add_member"))); //$NON-NLS-1$
			} else if (member instanceof IMethod) {
				MethodDeclaration newMethod= createNewMethodDeclarationNode(sourceRewrite, targetRewrite, ((IMethod)member), declaringCuNode, mapping, new SubProgressMonitor(pm, 1), status);
				targetRewrite.getASTRewrite().getListRewrite(targetClass, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(newMethod, ASTNodes.getInsertionIndex(newMethod, targetClass.bodyDeclarations()), targetRewrite.createGroupDescription(RefactoringCoreMessages.getString("HierarchyRefactoring.add_member"))); //$NON-NLS-1$
			} else if (member instanceof IType) {
				BodyDeclaration newType= createNewTypeDeclarationNode(((IType)member), declaringCuNode, mapping, targetRewrite.getASTRewrite());
				targetRewrite.getASTRewrite().getListRewrite(targetClass, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAt(newType, ASTNodes.getInsertionIndex(newType, targetClass.bodyDeclarations()), targetRewrite.createGroupDescription(RefactoringCoreMessages.getString("HierarchyRefactoring.add_member"))); //$NON-NLS-1$
			} else 
				Assert.isTrue(false);
			pm.worked(1);
		}
		pm.done();
	}

	private BodyDeclaration createNewTypeDeclarationNode(IType type, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, ASTRewrite rewrite) throws JavaModelException {
		TypeDeclaration oldType= ASTNodeSearchUtil.getTypeDeclarationNode(type, declaringCuNode);
		ICompilationUnit declaringCu= getDeclaringType().getCompilationUnit();
		if (! JdtFlags.isPublic(type) && ! JdtFlags.isProtected(type)) {
			if (mapping.length > 0)
				return createPlaceholderForTypeDeclaration(oldType, declaringCu, mapping, rewrite, true);

			return createPlaceholderForProtectedTypeDeclaration(oldType, declaringCuNode, declaringCu, rewrite, true);
		}
		if (mapping.length > 0)
			return createPlaceholderForTypeDeclaration(oldType, declaringCu, mapping, rewrite, true);

		return createPlaceholderForTypeDeclaration(oldType, declaringCu, rewrite, true);
	}

	private MethodDeclaration createNewMethodDeclarationNode(CompilationUnitRewrite sourceRewrite, CompilationUnitRewrite targetRewrite, IMethod sourceMethod, CompilationUnit declaringCuNode, TypeVariableMaplet[] mapping, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		MethodDeclaration oldMethod= ASTNodeSearchUtil.getMethodDeclarationNode(sourceMethod, declaringCuNode);
		ASTRewrite rewrite= targetRewrite.getASTRewrite();
		AST ast= rewrite.getAST();
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		copyBodyOfPulledUpMethod(sourceRewrite, targetRewrite, sourceMethod, oldMethod, newMethod, mapping, pm);
		newMethod.setConstructor(oldMethod.isConstructor());
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		copyJavadocNode(rewrite, sourceMethod, oldMethod, newMethod);
		newMethod.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getModifiersWithUpdatedVisibility(sourceMethod, sourceMethod.getFlags(), pm, true, status)));
		newMethod.setName(((SimpleName)ASTNode.copySubtree(ast, oldMethod.getName())));
		copyReturnType(rewrite, getDeclaringType().getCompilationUnit(), oldMethod, newMethod, mapping);
		copyParameters(rewrite, getDeclaringType().getCompilationUnit(), oldMethod, newMethod, mapping);
		copyThrownExceptions(oldMethod, newMethod);
		return newMethod;
	}

	private void copyBodyOfPulledUpMethod(CompilationUnitRewrite sourceRewrite, CompilationUnitRewrite targetRewrite, IMethod method, MethodDeclaration oldMethod, MethodDeclaration newMethod, TypeVariableMaplet[] mapping, IProgressMonitor pm) throws JavaModelException {
		Block body= oldMethod.getBody();
		if (body == null) {
			newMethod.setBody(null);
			return;
		}
		try {
			final IDocument document= new Document(method.getCompilationUnit().getBuffer().getContents());
			final ASTRewrite rewrite= ASTRewrite.create(body.getAST());
			final ITrackedNodePosition position= rewrite.track(body);
			body.accept(new PullUpAstNodeMapper(sourceRewrite, targetRewrite, rewrite, getSuperclassOfDeclaringClass(pm), mapping));
			rewrite.rewriteAST(document, null).apply(document, TextEdit.NONE);
			String content= document.get(position.getStartPosition(), position.getLength());
			final String[] lines= Strings.convertIntoLines(content);
			Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
			content= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
			newMethod.setBody((Block) targetRewrite.getASTRewrite().createStringPlaceholder(content, ASTNode.BLOCK));
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
	}

	private boolean needsToChangeVisibility(IMember member, boolean considerReferences, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		if (JdtFlags.isPublic(member) || JdtFlags.isProtected(member))
			return false;
		if (! considerReferences)
			return true;
		return isReferencedBySomethingElseThanMembersToPull(member, pm, status);
	}

	private boolean isReferencedBySomethingElseThanMembersToPull(IMember member, IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		if (! fCachedMembersReferences.containsKey(member))
		fCachedMembersReferences.put(member, RefactoringSearchEngine.search(SearchPattern.createPattern(member, IJavaSearchConstants.REFERENCES), RefactoringScopeFactory.create(member), pm, status));
		SearchResultGroup[] references= (SearchResultGroup[])fCachedMembersReferences.get(member);
		if (references.length == 0)
			return false;
		if (references.length > 1)
			return true;
		ICompilationUnit referencingCu= references[0].getCompilationUnit();
		if (! getDeclaringType().getCompilationUnit().equals(referencingCu))
			return true;
		SearchMatch[] searchResults= references[0].getSearchResults();
		for (int i= 0; i < searchResults.length; i++) {
			if (! isWithinMemberToPullUp(searchResults[i]))
				return true;
		}
		return false;
	}
	
	private boolean isWithinMemberToPullUp(SearchMatch result) throws JavaModelException {
		int referenceStart= result.getOffset();
		for (int i= 0; i < fMembersToMove.length; i++) {
			ISourceRange range= fMembersToMove[i].getSourceRange();
			if (range.getOffset() <= referenceStart && range.getOffset() + range.getLength() >= referenceStart)
				return true;
		}
		return false;
	}

	private void addImportsToCu(final CompilationUnitRewrite targetRewrite, final IType[] types) {
		for (int index= 0; index < types.length; index++) {
			final String fullyQualifiedName= types[index].getFullyQualifiedName('.');
			targetRewrite.getImportRewrite().addImport(fullyQualifiedName);
			targetRewrite.getImportRemover().registerAddedImport(fullyQualifiedName);
		}
	}

	private IType[] getTypesThatNeedToBeImportedInTargetCu(IProgressMonitor pm, CompilationUnit declaringCuNode) throws JavaModelException {
		if (getTargetClass().getCompilationUnit().equals(getDeclaringType().getCompilationUnit()))
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