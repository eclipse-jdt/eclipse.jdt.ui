/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Context;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
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

public class PushDownRefactoring extends Refactoring {
	
	public static class MemberActionInfo{

		private final IMember fMember;
		private int fAction;
		public static final int NO_ACTION= 0;
		public static final int PUSH_DOWN_ACTION= 1;
		public static final int PUSH_ABSTRACT_ACTION= 2;
		private MemberActionInfo(IMember member, int action){
			assertValidAction(action);
			Assert.isTrue(member instanceof IField || member instanceof IMethod);
			fMember= member;
			fAction= action;
		}
		
		public void setAction(int action){
			assertValidAction(action);
			if (isFieldInfo())
				Assert.isTrue(action != PUSH_ABSTRACT_ACTION);
			fAction= action;	
		}
	
		public int[] getAvailableActions(){
			if (isFieldInfo())
				return new int[]{NO_ACTION, PUSH_DOWN_ACTION};
			else
				return new int[]{NO_ACTION, PUSH_DOWN_ACTION, PUSH_ABSTRACT_ACTION};
		}
		
		public void assertValidAction(int action) {
			Assert.isTrue(	action == PUSH_ABSTRACT_ACTION || 
							action == NO_ACTION ||
							action == PUSH_DOWN_ACTION);
		}
		
		public static MemberActionInfo create(IMember member) {
			return new MemberActionInfo(member, NO_ACTION);
		}

		public boolean isToBeDeletedFromDeclaringClass() {
			return fAction == PUSH_DOWN_ACTION;
		}

		public boolean isNewMethodToBeDeclaredAbstract() throws JavaModelException {
			return 
				! isFieldInfo() &&
				! JdtFlags.isAbstract(fMember) &&
				fAction == PUSH_ABSTRACT_ACTION;
		}		

		public boolean isToBeCreatedInSubclassesOfDeclaringClass() {
			return fAction != NO_ACTION;
		}

		public IMember getMember() {
			return fMember;
		}
		
		public int getAction(){
			return fAction;
		}

		public boolean isFieldInfo() {
			return fMember instanceof IField;
		}

		public int getNewModifiersForCopyInSubclass(int oldModifiers) throws JavaModelException {
			if (isFieldInfo())
				return oldModifiers;
			if (isToBeDeletedFromDeclaringClass())
				return oldModifiers;
			int modifiers= oldModifiers;	
			if (isNewMethodToBeDeclaredAbstract()){
				if (! JdtFlags.isPublic(fMember))
					modifiers= Modifier.PROTECTED | ASTNodes.clearAccessModifiers(modifiers);
			}
			return modifiers;
		}
		
		public int getNewModifiersForOriginal(int oldModifiers) throws JavaModelException{
			if (isFieldInfo())
				return oldModifiers;
			if (isToBeDeletedFromDeclaringClass())
				return oldModifiers;
			int modifiers= oldModifiers;	
			if (isNewMethodToBeDeclaredAbstract()){
				modifiers= ASTNodes.clearFlag(Modifier.FINAL, ASTNodes.clearFlag(Modifier.NATIVE, oldModifiers));
				modifiers |= Modifier.ABSTRACT;

				if (! JdtFlags.isPublic(fMember))
					modifiers= Modifier.PROTECTED | ASTNodes.clearAccessModifiers(modifiers);
			}
			return modifiers;
		}

		public boolean copyJavadocToCopiesInSubclasses() {
			return isToBeDeletedFromDeclaringClass();
		}
		

		public static IMember[] getMembers(MemberActionInfo[] infos){
			IMember[] result= new IMember[infos.length];
			for (int i= 0; i < result.length; i++) {
				result[i]= infos[i].getMember();
			}
			return result;
		}
	}
	
	private MemberActionInfo[] fMemberInfos;
	private IMember[] fSelectedMembers;
	private TextChangeManager fChangeManager;
	
	private final ASTNodeMappingManager fAstManager;
	private final ASTRewriteManager fRewriteManager;
	private final ImportEditManager fImportEditManager;
	private final CodeGenerationSettings fPreferenceSettings;

	//caches
	private IType fCachedDeclaringClass;
	private ITypeHierarchy fCachedClassHierarchy;
	
	private IType[] fTypesReferencedInPushedDownMembers;
	public PushDownRefactoring(IMember[] members, CodeGenerationSettings preferenceSettings){
		Assert.isTrue(members.length > 0);
		Assert.isNotNull(preferenceSettings);
		fSelectedMembers= (IMember[])SourceReferenceUtil.sortByOffset(members);
		fPreferenceSettings= preferenceSettings;
		fAstManager= new ASTNodeMappingManager();
		fRewriteManager= new ASTRewriteManager(fAstManager);
		fImportEditManager= new ImportEditManager(preferenceSettings);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return "Push Down";
	}

	public IType getDeclaringClass(){
		if (fCachedDeclaringClass != null)
			return fCachedDeclaringClass;
		//all members declared in same type - checked in precondition
		fCachedDeclaringClass= (IType)WorkingCopyUtil.getOriginal(fSelectedMembers[0].getDeclaringType()); //index safe - checked in constructor
		return fCachedDeclaringClass;
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
		RefactoringStatus result= new RefactoringStatus();
					
		result.merge(checkAllSelectedMembers());
		if (result.hasFatalError())
			return result;
		
		if (! haveCommonDeclaringType())
			return RefactoringStatus.createFatalErrorStatus("Selected elements do not have a common delcaring type");

		return new RefactoringStatus();
	}

	private boolean haveCommonDeclaringType() {
		IType declaringType= fSelectedMembers[0].getDeclaringType(); //index safe - checked in constructor
		if (declaringType == null)
			return false;
		for (int i= 0; i < fSelectedMembers.length; i++) {
			if (! declaringType.equals(fSelectedMembers[i].getDeclaringType()))
				return false;
		}
		return true;
	}

	private RefactoringStatus checkAllSelectedMembers() throws JavaModelException {
		for (int i = 0; i < fSelectedMembers.length; i++) {
			RefactoringStatus status= checkElement(fSelectedMembers[i]);
			if (! status.isOK())
				return status;
		}
		return new RefactoringStatus();
	}
	
	private static RefactoringStatus checkElement(IMember member) throws JavaModelException{
		if (member.getElementType() != IJavaElement.METHOD && 
			member.getElementType() != IJavaElement.FIELD)
				return RefactoringStatus.createFatalErrorStatus("Only fields and methods can be pushed down");
		if (! member.exists())
			return RefactoringStatus.createFatalErrorStatus("One or more of the elements does not exist");
	
		if (member.isBinary())
			return RefactoringStatus.createFatalErrorStatus("Binary members cannot be pulled down");

		if (member.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus("Read only elements cannot be pulled down");

		if (! member.isStructureKnown())
			return RefactoringStatus.createFatalErrorStatus("Elements with unknown structure cannot be pulled down");

		if (JdtFlags.isStatic(member))
			return RefactoringStatus.createFatalErrorStatus("Static members cannot be pulled down");
			
		if (member.getElementType() == IJavaElement.METHOD){
			IMethod method= (IMethod) member;
			if (method.isConstructor())
				return RefactoringStatus.createFatalErrorStatus("Constructors cannot be pulled down");
			
			if (JdtFlags.isNative(method))
				return RefactoringStatus.createFatalErrorStatus("Native methods cannot be pulled down");
		}
		return new RefactoringStatus();	
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try {
			pm.beginTask("", 2); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			fSelectedMembers= WorkingCopyUtil.getOriginals(fSelectedMembers);
			
			result.merge(checkPossibleSubclasses(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;						
			result.merge(checkDeclaringType());
			if (result.hasFatalError())
				return result;			
			result.merge(checkIfMembersExist());
			if (result.hasFatalError())
				return result;			
			
			fMemberInfos= createInfosForAllPushableFieldsAndMethods(getDeclaringClass());
			setInfoAction(MemberActionInfo.PUSH_DOWN_ACTION, fSelectedMembers);
			return result;	
		} finally {
			pm.done();
		}
	}
	
	private void setInfoAction(int action, IMember[] members) {
		List list= Arrays.asList(members);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (list.contains(info.getMember()))
				info.setAction(action);
		}
	}

	private RefactoringStatus checkPossibleSubclasses(IProgressMonitor pm) throws JavaModelException {
		IType[] modifiableSubclasses= getDestinationClassesForNonAbstractMembers(pm);
		if (modifiableSubclasses.length == 0){
			String pattern= "Class ''{0}'' does not have any modifiable non-anonymous subclasses";
			String msg= MessageFormat.format(pattern, new String[]{createTypeLabel(getDeclaringClass())});
			return RefactoringStatus.createFatalErrorStatus(msg);
		}
		return new RefactoringStatus();
	}

	//XXX copied form Pull UP
	private RefactoringStatus checkIfMembersExist() {
		for (int i= 0; i < fSelectedMembers.length; i++) {
			IMember orig= fSelectedMembers[i];
			if (orig == null || ! orig.exists()){
				String message= "One more more of the selected members does not exist in the saved version of the file";
				return RefactoringStatus.createFatalErrorStatus(message);
			}	
		}
		return new RefactoringStatus();
	}

	//XXX copied form Pull UP
	private RefactoringStatus checkDeclaringType() throws JavaModelException {
		IType declaringType= getDeclaringClass();
				
		if (declaringType.isInterface()) //for now
			return RefactoringStatus.createFatalErrorStatus("Pushing down interface members is not supported");
		
		if (declaringType.isBinary())
			return RefactoringStatus.createFatalErrorStatus("Pushing down members declared in binary types is not supported");

		if (declaringType.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus("Pushing down members declared in read-only types is not supported");
		
		return new RefactoringStatus();
	}

	private static MemberActionInfo[] createInfosForAllPushableFieldsAndMethods(IType type) throws JavaModelException {
		IMethod[] methods= type.getMethods();
		IField[] fields= type.getFields();
		List result= new ArrayList(methods.length + fields.length);
		for (int i= 0; i < methods.length; i++) {
			if (isPushable(methods[i]))
				result.add(MemberActionInfo.create(methods[i]));
		}
		for (int i= 0; i < fields.length; i++) {
			if (isPushable(fields[i]))
				result.add(MemberActionInfo.create(fields[i]));
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private static boolean isPushable(IMember member) throws JavaModelException {
		return checkElement(member).isOK();
	}

	public MemberActionInfo[] getMemberActionInfos(){
		return fMemberInfos;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 4); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			
			result.merge(checkMembersInDestinationClasses(new SubProgressMonitor(pm, 1)));
			result.merge(checkAccesses(new SubProgressMonitor(pm, 1)));
			if (shouldMakeDeclaringClassAbstract())
				result.merge(checkCallsToDeclaringClassConstructors(new SubProgressMonitor(pm, 1)));
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

	private RefactoringStatus checkAccesses(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("Checking referenced elements", 3);
		IType[] subclasses= getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(pm, 1));
		result.merge(checkAccessedTypes(subclasses, new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedFields(subclasses, new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedMethods(subclasses, new SubProgressMonitor(pm, 1)));
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkAccessedTypes(IType[] subclasses, IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= getTypeReferencedInPushedDownMembers(pm);
		for (int i= 0; i < subclasses.length; i++) {
			IType targetClass= subclasses[i];
			for (int j= 0; j < accessedTypes.length; j++) {
				IType type= accessedTypes[j];
				if (! canBeAccessedFrom(type, targetClass)){
					String pattern= "Type ''{0}'' referenced in one of the pushed elements is not accessible from type ''{1}''";
					String message= MessageFormat.format(pattern, new String[]{createTypeLabel(type), createTypeLabel(targetClass)});
					result.addError(message, JavaSourceContext.create(type));
				}	
			}
		}
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkAccessedFields(IType[] subclasses, IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= getMembersToPushDown();
		List pushedDownList= Arrays.asList(membersToPushDown);
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(membersToPushDown, pm);
		for (int i= 0; i < subclasses.length; i++) {	
			IType targetClass= subclasses[i];
			for (int j= 0; j < accessedFields.length; j++) {
				IField field= accessedFields[j];
				boolean isAccessible= 	pushedDownList.contains(field) || 
										canBeAccessedFrom(field, targetClass);
				if (! isAccessible){
					String pattern= "Field ''{0}'' referenced in one of the pulled elements is not accessible from type ''{1}''";
					String message= MessageFormat.format(pattern,  new String[]{createFieldLabel(field), createTypeLabel(targetClass)});
					result.addError(message, JavaSourceContext.create(field));
				} 
			}
		}
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccessedMethods(IType[] subclasses, IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= getMembersToPushDown();
		List pushedDownList= Arrays.asList(membersToPushDown);
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(membersToPushDown, pm);
		for (int i= 0; i < subclasses.length; i++) {
			IType targetClass= subclasses[i];
			for (int j= 0; j < accessedMethods.length; j++) {
				IMethod method= accessedMethods[j];
				boolean isAccessible= canBeAccessedFrom(method, targetClass) ||
									   pushedDownList.contains(method);
				if (! isAccessible){
					String pattern= "Method ''{0}'' referenced in one of the pulled elements is not accessible from type ''{1}''";
					String message= MessageFormat.format(pattern,  new String[]{createMethodLabel(method), createTypeLabel(targetClass)});
					result.addError(message, JavaSourceContext.create(method));
				}
			}
		}
		pm.done();
		return result;
	}
	
	//almost copied from pull up
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

			boolean samePackage= enclosingType.getPackageFragment().equals(newHome.getPackageFragment());
			if (samePackage)
				return true; //private checked before

			if (enclosingType.equals(getDeclaringClass()))
				return JdtFlags.isPublic(member) || JdtFlags.isProtected(member);
			else
				return JdtFlags.isPublic(member);
		}
	}

	private IType[] getTypeReferencedInPushedDownMembers(IProgressMonitor pm) throws JavaModelException {
		if (fTypesReferencedInPushedDownMembers == null)
			fTypesReferencedInPushedDownMembers= ReferenceFinderUtil.getTypesReferencedIn(getMembersToPushDown(), pm);
		return fTypesReferencedInPushedDownMembers;
	}

	private IMember[] getMembersToPushDown() {
		return MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
	}

	//copied from pull up
	private RefactoringStatus checkCallsToDeclaringClassConstructors(IProgressMonitor pm) throws JavaModelException {
		ASTNode[] refNodes= ConstructorReferenceFinder.getConstructorReferenceNodes(getDeclaringClass(), fAstManager, pm);
		RefactoringStatus result= new RefactoringStatus();
		String pattern= "Class ''{0}'' cannot be made abstract because it gets instantiated";
		String msg= MessageFormat.format(pattern, new Object[]{createTypeLabel(getDeclaringClass())});
		for (int i= 0; i < refNodes.length; i++) {
			ASTNode node= refNodes[i];
			if (node instanceof ClassInstanceCreation){
				Context context= JavaSourceContext.create(fAstManager.getCompilationUnit(node), node);
				result.addError(msg, context);
			}
		}
		return result;
	}
	
	private RefactoringStatus checkMembersInDestinationClasses(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2);
		RefactoringStatus result= new RefactoringStatus();
		IMember[] membersToPushDown= getMembersToPushDown();

		IType[] destinationClassesForNonAbstract= getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(pm, 1));
		result.merge(checkNonAbstractMembersInDestinationClasses(membersToPushDown, destinationClassesForNonAbstract));
		
		IType[] destinationClassesForAbstract= getDestinationClassesForAbstractMembers(new SubProgressMonitor(pm, 1));
		result.merge(checkAbstractMembersInDestinationClasses(membersToPushDown, destinationClassesForAbstract));
		pm.done();
		return result;
	}
	
	private RefactoringStatus checkAbstractMembersInDestinationClasses(IMember[] membersToPushDown, IType[] destinationClassesForAbstract) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMember[] abstractMembersToPushDown= getAbstractMembers(membersToPushDown);
		for (int i= 0; i < destinationClassesForAbstract.length; i++) {
			result.merge(MemberCheckUtil.checkMembersInDestinationType(abstractMembersToPushDown, destinationClassesForAbstract[i]));
		}
		return result;
	}
	
	private RefactoringStatus checkNonAbstractMembersInDestinationClasses(IMember[] membersToPushDown, IType[] destinationClassesForNonAbstract) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMember[] nonAbstractMembersToPushDown= getNonAbstractMembers(membersToPushDown);
		for (int i= 0; i < destinationClassesForNonAbstract.length; i++) {
			result.merge(MemberCheckUtil.checkMembersInDestinationType(nonAbstractMembersToPushDown, destinationClassesForNonAbstract[i]));
		}
		return result;
	}
	
	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}

	//--  change creation -------
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			return new CompositeChange("Push down", fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally{
			pm.done();
		}
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask("Creating preview...", 4);

			deleteMembersFromDeclaringClass();
			pm.worked(1);
			
			makeMethodsAbstractInDeclaringClass();
			pm.worked(1);

			if (shouldMakeDeclaringClassAbstract())
				makeDeclaringClassAbstract();
			pm.worked(1);
	
			createMembersInSubclasses(new SubProgressMonitor(pm, 1));
			
			addImportsToSubclasses(new SubProgressMonitor(pm, 1));
			
			TextChangeManager manager= new TextChangeManager();
			fillWithRewriteEdits(manager);
			return manager;
		} finally{
			pm.done();
		}
	}

	private void addImportsToSubclasses(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 4);
		IMember[] allMembers= MemberActionInfo.getMembers(getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass());
		IMember[] nonAbstractMembers= getNonAbstractMembers(allMembers);
		IType[] destForNonAbstract= getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(pm, 1));
		addImportsToTypesReferencedInMembers(nonAbstractMembers, destForNonAbstract, new SubProgressMonitor(pm, 1));

		IMember[] abstractMembers= getAbstractMembers(allMembers);
		IType[] destForAbstract= getDestinationClassesForAbstractMembers(new SubProgressMonitor(pm, 1));
		addImportsToTypesReferencedInMembers(abstractMembers, destForAbstract, new SubProgressMonitor(pm, 1));
		pm.done();
	}
	
	private void addImportsToTypesReferencedInMembers(IMember[] members, IType[] destinationClasses, IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 1);
		IType[] typesReferenced= getTypesReferencedIn(members, pm);
		for (int i= 0; i < destinationClasses.length; i++) {
			ICompilationUnit cu= getWorkingCopyOfCu(destinationClasses[i]);
			for (int j= 0; j < typesReferenced.length; j++) {
				fImportEditManager.addImportTo(typesReferenced[j], cu);
			}
		}
		pm.done();
	}
	
	private IType[] getTypesReferencedIn(IMember[] members, IProgressMonitor pm) throws JavaModelException {
		return ReferenceFinderUtil.getTypesReferencedIn(members, pm);
	}

	private void deleteMembersFromDeclaringClass() throws JavaModelException {
		IMember[] toDelete= getMembersToDelete();
		for (int i= 0; i < toDelete.length; i++) {
			if (toDelete[i] instanceof IField)
				deleteFieldFromDeclaringClass((IField)toDelete[i]);
			else if (toDelete[i] instanceof IMethod)
				deleteMethodFromDeclaringClass((IMethod)toDelete[i]);
		}
	}

	private IMember[] getMembersToDelete() {
		List result= new ArrayList(fMemberInfos.length);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (info.isToBeDeletedFromDeclaringClass())
				result.add(info.getMember());
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	private void deleteFieldFromDeclaringClass(IField field) throws JavaModelException{
		FieldDeclaration fd= getFieldDeclarationNode(field);
		if (fd.fragments().size() == 1){
			fRewriteManager.getRewrite(getWorkingCopyOfCu(field)).markAsRemoved(fd);
		} else {	
			VariableDeclarationFragment fragment= getFieldDeclarationFragmentNode(field);
			fRewriteManager.getRewrite(getWorkingCopyOfCu(field)).markAsRemoved(fragment);
		}
	}

	private void deleteMethodFromDeclaringClass(IMethod method) throws JavaModelException{
		MethodDeclaration md= getMethodDeclarationNode(method);
		fRewriteManager.getRewrite(getWorkingCopyOfCu(method)).markAsRemoved(md);
	}

	private void createMembersInSubclasses(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2);
		MemberActionInfo[] allMembers= getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass();
		IType[] destinationsForNonAbstract= getDestinationClassesForNonAbstractMembers(new SubProgressMonitor(pm, 1));
		IType[] destinationsForAbstract= getDestinationClassesForAbstractMembers(new SubProgressMonitor(pm, 1));
		for (int i= 0; i < allMembers.length; i++) {
			MemberActionInfo info= allMembers[i];
			if (JdtFlags.isAbstract(info.getMember()))
				createInAll(info, destinationsForAbstract);
			else
				createInAll(info, destinationsForNonAbstract);
		}
		pm.done();
	}

	private void createInAll(MemberActionInfo info, IType[] types) throws JavaModelException{
		for (int j= 0; j < types.length; j++) {
			IType subclass= types[j];
			TypeDeclaration typeDeclaration= getTypeDeclarationNode(subclass);
			create(info, typeDeclaration);
		}
	}
	
	private void create(MemberActionInfo info, TypeDeclaration typeDeclaration) throws JavaModelException {
		if (info.isFieldInfo())
			createField(info, typeDeclaration);
		else
			createMethod(info, typeDeclaration);
	}

	private void createMethod(MemberActionInfo info, TypeDeclaration typeDeclaration) throws JavaModelException {
		ASTRewrite rewrite= getRewriteFor(typeDeclaration);
		MethodDeclaration newMethod= createNewMethodDeclarationNode(info, rewrite);
		rewrite.markAsInserted(newMethod);
		typeDeclaration.bodyDeclarations().add(newMethod);
	}

	private MethodDeclaration createNewMethodDeclarationNode(MemberActionInfo info, ASTRewrite rewrite) throws JavaModelException {
		Assert.isTrue(! info.isFieldInfo());
		IMethod method= (IMethod)info.getMember();
		MethodDeclaration oldMethod= getMethodDeclarationNode(method);
		AST ast= getAST(rewrite);
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		copyBodyOfPushedDownMethod(rewrite, method, oldMethod, newMethod);
		newMethod.setConstructor(oldMethod.isConstructor());
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		if (info.copyJavadocToCopiesInSubclasses())
			copyJavadocNode(rewrite, method, oldMethod, newMethod);
		newMethod.setModifiers(getNewModifiersForCopiedMethod(info, oldMethod));
		newMethod.setName(ast.newSimpleName(oldMethod.getName().getIdentifier()));
		copyReturnType(rewrite, oldMethod, newMethod);
		copyParameters(rewrite, oldMethod, newMethod);
		copyThrownExceptions(oldMethod, newMethod);
		return newMethod;
	}

	private void copyBodyOfPushedDownMethod(ASTRewrite targetRewrite, IMethod method, MethodDeclaration oldMethod, MethodDeclaration newMethod) throws JavaModelException {
		if (oldMethod.getBody() == null){
			newMethod.setBody(null);
			return;
		}
		Block oldBody= oldMethod.getBody();
		String oldBodySource= getBufferText(oldBody);
		String[] lines= Strings.convertIntoLines(oldBodySource);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		oldBodySource= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
		Block newBody= (Block)targetRewrite.createPlaceholder(oldBodySource, ASTRewrite.BLOCK);
		newMethod.setBody(newBody);
	}

	private int getNewModifiersForCopiedMethod(MemberActionInfo info, MethodDeclaration oldMethod) throws JavaModelException {
		return info.getNewModifiersForCopyInSubclass(oldMethod.getModifiers());
	}

	//copied from Pull up
	private void copyThrownExceptions(MethodDeclaration oldMethod, MethodDeclaration newMethod) {
		AST ast= newMethod.getAST();
		for (int i= 0, n= oldMethod.thrownExceptions().size(); i < n; i++) {
			Name oldException= (Name)oldMethod.thrownExceptions().get(i);
			if (oldException.isSimpleName()){
				Name newException= ast.newSimpleName(((SimpleName)oldException).getIdentifier());
				newMethod.thrownExceptions().add(i, newException);
			}	else {
				String[] identifiers= ASTNodes.getIdentifiers((QualifiedName)oldException);
				Name newException= ast.newName(identifiers);
				newMethod.thrownExceptions().add(i, newException);
			}
		}
	}
	
	//copied from Pull up
	private void copyParameters(ASTRewrite targetRewrite, MethodDeclaration oldMethod, MethodDeclaration newMethod) throws JavaModelException {
		for (int i= 0, n= oldMethod.parameters().size(); i < n; i++) {
			SingleVariableDeclaration oldParam= (SingleVariableDeclaration)oldMethod.parameters().get(i);
			SingleVariableDeclaration newParam= createPlaceholderForSingleVariableDeclaration(oldParam, targetRewrite);
			newMethod.parameters().add(i, newParam);
		}
	}
	
	//copied from Pull up
	private void copyReturnType(ASTRewrite targetRewrite, MethodDeclaration oldMethod, MethodDeclaration newMethod) throws JavaModelException {
		Type newReturnType= createPlaceholderForType(oldMethod.getReturnType(), targetRewrite);
		newMethod.setReturnType(newReturnType);
	}

	private void createField(MemberActionInfo info, TypeDeclaration typeDeclaration) throws JavaModelException {
		ASTRewrite rewrite= getRewriteFor(typeDeclaration);
		FieldDeclaration newField= createNewFieldDeclarationNode(info,rewrite);
		rewrite.markAsInserted(newField);
		typeDeclaration.bodyDeclarations().add(newField);
	}
	
	private FieldDeclaration createNewFieldDeclarationNode(MemberActionInfo info, ASTRewrite rewrite) throws JavaModelException {
		Assert.isTrue(info.isFieldInfo());
		IField field= (IField)info.getMember();
		AST ast= getAST(rewrite);
		VariableDeclarationFragment oldFieldFragment= getFieldDeclarationFragmentNode(field);
		VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
		newFragment.setExtraDimensions(oldFieldFragment.getExtraDimensions());
		if (oldFieldFragment.getInitializer() != null){
			Expression newInitializer= createPlaceholderForExpression(oldFieldFragment.getInitializer(), rewrite);
			newFragment.setInitializer(newInitializer);
		}	
		newFragment.setName(ast.newSimpleName(oldFieldFragment.getName().getIdentifier()));
		FieldDeclaration newField= ast.newFieldDeclaration(newFragment);
		FieldDeclaration oldField= getFieldDeclarationNode(field);
		if (info.copyJavadocToCopiesInSubclasses())
			copyJavadocNode(rewrite, field, oldField, newField);
		newField.setModifiers(getNewModifiersForCopiedField(info, oldField));
		
		Type newType= createPlaceholderForType(oldField.getType(), rewrite);
		newField.setType(newType);
		return newField;
	}
	
	private int getNewModifiersForCopiedField(MemberActionInfo info, FieldDeclaration oldField) throws JavaModelException {
		return info.getNewModifiersForCopyInSubclass(oldField.getModifiers());
	}

	private static void copyJavadocNode(ASTRewrite rewrite, IMember member, BodyDeclaration oldDeclaration, BodyDeclaration newDeclaration) throws JavaModelException {
		Javadoc oldJavaDoc= oldDeclaration.getJavadoc();
		if (oldJavaDoc == null)
			return;
		String source= oldJavaDoc.getComment();
		String[] lines= Strings.convertIntoLines(source);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		source= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(member));
		Javadoc newJavaDoc= (Javadoc)rewrite.createPlaceholder(source, ASTRewrite.JAVADOC);
		newDeclaration.setJavadoc(newJavaDoc);
	}
	
	private static IType[] toTypeArray(IMember[] member){
		List list= Arrays.asList(member);
		return (IType[]) list.toArray(new IType[list.size()]);
	}
	
	private static IMember[] getNonAbstractMembers(IMember[] members) throws JavaModelException{
		List list= new ArrayList(); //Arrays.asList does not support removing
		list.addAll(Arrays.asList(members));
		list.removeAll(Arrays.asList(getAbstractMembers(members)));
		return (IMember[]) list.toArray(new IMember[list.size()]);
	}
	
	private static IMember[] getAbstractMembers(IMember[] members) throws JavaModelException{
		List result= new ArrayList(members.length);
		for (int i= 0; i < members.length; i++) {
			IMember member= members[i];
			if (JdtFlags.isAbstract(member))
				result.add(member);
		}
		return (IMember[]) result.toArray(new IMember[result.size()]);
	}

	private IType[] getDestinationClassesForAbstractMembers(IProgressMonitor pm) throws JavaModelException {
		return toTypeArray(getAbstractMembers(getDestinationClassesForNonAbstractMembers(pm)));
	}
	
	private IType[] getDestinationClassesForNonAbstractMembers(IProgressMonitor pm) throws JavaModelException {
		IType[] allDirectSubclasses= getAllDirectSubclassesOfDeclaringClass(pm);
		List result= new ArrayList(allDirectSubclasses.length);
		for (int i= 0; i < allDirectSubclasses.length; i++) {
			IType subclass= allDirectSubclasses[i];
			if (isModifiable(subclass))
				result.add(subclass);
		}
		return (IType[]) result.toArray(new IType[result.size()]);
	}

	private static boolean isModifiable(IType clazz) throws JavaModelException {
		if (! clazz.exists())
			return false;
		if (clazz.isBinary())
			return false;
		if (clazz.isReadOnly())
			return false;
		if (clazz.getCompilationUnit() == null)
			return false;
		if (! clazz.isStructureKnown())
			return false;
		return true;
	}

	private IType[] getAllDirectSubclassesOfDeclaringClass(IProgressMonitor pm) throws JavaModelException {
		return getHierarchyOfDeclaringClass(pm).getSubclasses(getDeclaringClass());
	}

	public ITypeHierarchy getHierarchyOfDeclaringClass(IProgressMonitor pm) throws JavaModelException {
		try{
			if (fCachedClassHierarchy != null)
				return fCachedClassHierarchy;
			fCachedClassHierarchy= getDeclaringClass().newTypeHierarchy(pm);
			return fCachedClassHierarchy;
		} finally{
			pm.done();
		}	
	}
	
	private MemberActionInfo[] getInfosForMembersToBeCreatedInSubclassesOfDeclaringClass() {
		List result= new ArrayList(fMemberInfos.length);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (info.isToBeCreatedInSubclassesOfDeclaringClass())
				result.add(info);
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private void makeDeclaringClassAbstract() throws JavaModelException {
		TypeDeclaration declaration= getTypeDeclarationNode(getDeclaringClass());
		ASTRewrite rewrite= getRewriteFor(declaration);
		AST ast= getAST(rewrite);
		TypeDeclaration newType= ast.newTypeDeclaration();
		newType.setInterface(declaration.isInterface());
		newType.setModifiers(createNewModifiersForMakingDeclaringClassAbstract(declaration));
		rewrite.markAsModified(declaration, newType);
	}

	private int createNewModifiersForMakingDeclaringClassAbstract(TypeDeclaration declaration) {
		int initialModifiers= declaration.getModifiers();
		return Modifier.ABSTRACT | initialModifiers;
	}

	private boolean shouldMakeDeclaringClassAbstract() throws JavaModelException {
		return ! JdtFlags.isAbstract(getDeclaringClass()) && 
				getInfosForNewMethodsToBeDeclaredAbstract().length != 0;
	}

	private void makeMethodsAbstractInDeclaringClass() throws JavaModelException {
		MemberActionInfo[] methods= getInfosForNewMethodsToBeDeclaredAbstract();
		for (int i= 0; i < methods.length; i++) {
			declareAbstract(methods[i]);
		}
	}

	private MemberActionInfo[] getInfosForNewMethodsToBeDeclaredAbstract() throws JavaModelException {
		List result= new ArrayList(fMemberInfos.length);
		for (int i= 0; i < fMemberInfos.length; i++) {
			MemberActionInfo info= fMemberInfos[i];
			if (info.isNewMethodToBeDeclaredAbstract())
				result.add(info);
		}
		return (MemberActionInfo[]) result.toArray(new MemberActionInfo[result.size()]);
	}

	private void declareAbstract(MemberActionInfo info) throws JavaModelException {
		Assert.isTrue(! info.isFieldInfo());
		IMethod method= (IMethod)info.getMember();
		if (JdtFlags.isAbstract(method))
			return;
		MethodDeclaration declaration= getMethodDeclarationNode(method);
		ASTRewrite rewrite= getRewriteFor(declaration);
		AST ast= getAST(rewrite);
		MethodDeclaration newMethod= ast.newMethodDeclaration();
		rewrite.markAsRemoved(declaration.getBody());
		newMethod.setModifiers(createModifiersForMethodMadeAbstract(info, declaration.getModifiers()));
		newMethod.setExtraDimensions(declaration.getExtraDimensions());
		newMethod.setConstructor(declaration.isConstructor());
		rewrite.markAsModified(declaration, newMethod);
	}

	private int createModifiersForMethodMadeAbstract(MemberActionInfo info, int oldModifiers) throws JavaModelException {
		return info.getNewModifiersForOriginal(oldModifiers);
	}

	//XXX copied from pull up
	private void fillWithRewriteEdits(TextChangeManager manager) throws CoreException {
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
			textChange.addTextEdit("Push down class member(s)", resultingEdits);
			rewrite.removeModifications();
		}
	}

	private ICompilationUnit getWorkingCopyOfCu(IMember member){
		return WorkingCopyUtil.getWorkingCopyIfExists(member.getCompilationUnit());
	}
	
	private ASTRewrite getRewriteFor(ASTNode node) {
		return fRewriteManager.getRewrite(ASTNodeMappingManager.getCompilationUnitNode(node));
	}

	private static AST getAST(ASTRewrite rewrite){
		return rewrite.getRootNode().getAST();
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

	//--
	private static String createTypeLabel(IType type){
		return JavaModelUtil.getFullyQualifiedName(type);
	}

	private static String createFieldLabel(IField field){
		return field.getElementName();
	}

	private static String createMethodLabel(IMethod method){
		return JavaElementUtil.createMethodSignature(method);
	}
	
	private String getBufferText(ASTNode node) throws JavaModelException{
		return fAstManager.getCompilationUnit(node).getBuffer().getText(node.getStartPosition(), node.getLength());
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
}
