package org.eclipse.jdt.internal.corext.refactoring.structure;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.CompositeChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine;
import org.eclipse.jdt.internal.corext.refactoring.SearchResult;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultCollector;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaSourceContext;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatus;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceSourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JdtFlags;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.refactoring.util.WorkingCopyUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

public class MoveMembersRefactoring extends Refactoring {
	
	private final CodeGenerationSettings fPreferenceSettings;
	private IMember[] fMembers;
	private IType fDestinationType;
	private String fDestinationTypeName;
	private TextChangeManager fChangeManager;
	
	private Map fImportEdits;

	public MoveMembersRefactoring(IMember[] elements, CodeGenerationSettings preferenceSettings){
		Assert.isNotNull(elements);
		Assert.isNotNull(preferenceSettings);
		fMembers= (IMember[])SourceReferenceUtil.sortByOffset(elements);
		fPreferenceSettings= preferenceSettings;
		fImportEdits= new HashMap();
	}
	
	/*
	 * @see IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("MoveMembersRefactoring.Move_Members"); //$NON-NLS-1$
	}

	public IType getDestinationType() {
		return fDestinationType;
	}

	public void setDestinationTypeFullyQualifiedName(String fullyQualifiedTypeName) throws JavaModelException {
		Assert.isNotNull(fullyQualifiedTypeName);
		fDestinationType= resolveType(fullyQualifiedTypeName);
		fDestinationTypeName= fullyQualifiedTypeName;
	}
	
	public IMember[] getMovedMembers(){
		return fMembers;
	}
	
	private IType resolveType(String fullyQualifiedTypeName) throws JavaModelException{
		return getDeclaringType().getJavaProject().findType(fullyQualifiedTypeName);
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
			
		result.merge(checkAllElements());
		if (result.hasFatalError())
			return result;
			
		if (! haveCommonDeclaringType())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.same_type"));			 //$NON-NLS-1$
		
		return new RefactoringStatus();
	}
	
	/*
	 * @see Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkActivation(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 1); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			
			result.merge(checkDeclaringType());
			pm.worked(1);
			if (result.hasFatalError())
				return result;			
			
			fMembers= getOriginals(fMembers);
			return new RefactoringStatus();
		} finally{
			pm.done();
		}	
	}

	/*
	 * @see Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkInput(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask(RefactoringCoreMessages.getString("MoveMembersRefactoring.Checking_preconditions"), 5); //$NON-NLS-1$
			
			RefactoringStatus result= new RefactoringStatus();	
			
			result.merge(checkDestinationType());			
			if (result.hasFatalError())
				return result;
						
			result.merge(MemberCheckUtil.checkMembersInDestinationType(fMembers, fDestinationType));	
			if (result.hasFatalError())
				return result;
			
			result.merge(checkAccessedMembersAvailability(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;

			result.merge(checkMovedMembersAvailability(new SubProgressMonitor(pm, 1)));
			if (result.hasFatalError())
				return result;
			
			result.merge(checkNativeMovedMethods(new SubProgressMonitor(pm, 1)));
			
			if (result.hasFatalError())
				return result;
			
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} catch (CoreException e){
			throw new JavaModelException(e);	
		} finally{
			pm.done();
		}	
	}
	
	private RefactoringStatus checkDestinationType() throws JavaModelException {			
		if (fDestinationType == null){
			String message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.not_found", fDestinationTypeName);//$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		
		if (fDestinationType.equals(getDeclaringType())){
			String message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.same", //$NON-NLS-1$
				JavaElementUtil.createSignature(fDestinationType));
			return RefactoringStatus.createFatalErrorStatus(message);	
		}	
		
		if (! fDestinationType.exists()){
			String message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.not_exist", //$NON-NLS-1$
				JavaElementUtil.createSignature(fDestinationType));
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
			
		if (fDestinationType.isBinary()){
			String message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.dest_binary", //$NON-NLS-1$
				JavaElementUtil.createSignature(fDestinationType));
			return RefactoringStatus.createFatalErrorStatus(message);
		}	

		if (fDestinationType.isInterface() && ! getDeclaringType().isInterface()){
			String message= RefactoringCoreMessages.getString("MoveMembersRefactoring.interface_fields"); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	

		if (! fDestinationType.isInterface() && getDeclaringType().isInterface()){
			String message= RefactoringCoreMessages.getString("MoveMembersRefactoring.interface_members"); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	

		RefactoringStatus result= new RefactoringStatus();				
		
		if (! canDeclareStaticMembers(fDestinationType)){
			String message= RefactoringCoreMessages.getString("MoveMembersRefactoring.static_declaration"); //$NON-NLS-1$
			result.addError(message);
		}	
				
		return result;	
	}
	
	private RefactoringStatus checkNativeMovedMethods(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", fMembers.length); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMembers.length; i++) {
			if (fMembers[i].getElementType() != IJavaElement.METHOD)
				continue;
			if (! JdtFlags.isNative(fMembers[i]))
				continue;
			String message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.native", //$NON-NLS-1$
				JavaElementUtil.createMethodSignature((IMethod)fMembers[i]));
			result.addWarning(message, JavaSourceContext.create(fMembers[i]));
			pm.worked(1);
		}
		pm.done();
		return result;		
	}
	
	private RefactoringStatus checkMovedMembersAvailability(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", fMembers.length); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		for (int i= 0; i < fMembers.length; i++) {
			//XXX issues too many warnings - should check references to moved members
			if (! isVisibleFrom(fMembers[i], fMembers[i].getDeclaringType(), fDestinationType)){
				String message= createNonAccessibleMemberMessage(fMembers[i], fMembers[i].getDeclaringType(), true);
				result.addWarning(message, JavaSourceContext.create(fMembers[i]));
			}	
			pm.worked(1);
		}
		pm.done();
		return result;
	}
	
	private static String createNonAccessibleMemberMessage(IMember member, IType type, boolean moved) throws JavaModelException{
		switch (member.getElementType()){
			case IJavaElement.FIELD: {
				String message;
				if (moved)
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.moved_field", //$NON-NLS-1$ 
								new String[]{JavaElementUtil.createFieldSignature((IField)member), 
									 createAccessModifierString(member),
								 	JavaModelUtil.getFullyQualifiedName(type)});
				else
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.accessed_field", //$NON-NLS-1$
								new String[]{JavaElementUtil.createFieldSignature((IField)member), 
									 createAccessModifierString(member),
								 	JavaModelUtil.getFullyQualifiedName(type)});
				return message;
			}			
			case IJavaElement.METHOD: {
				String message;
				if (moved)
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.moved_method", //$NON-NLS-1$
									new String[]{JavaElementUtil.createMethodSignature((IMethod)member),
								 	createAccessModifierString(member),
								 	JavaModelUtil.getFullyQualifiedName(type)});
				else				 
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.accessed_method", //$NON-NLS-1$
									new String[]{JavaElementUtil.createMethodSignature((IMethod)member),
								 	createAccessModifierString(member),
								 	JavaModelUtil.getFullyQualifiedName(type)});
								 
				return message;		
			}			
			case IJavaElement.TYPE:{
				String message;
				if (moved)
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.moved_type", //$NON-NLS-1$
						new String[]{JavaModelUtil.getFullyQualifiedName(((IType)member)), 
									createAccessModifierString(member),
									JavaModelUtil.getFullyQualifiedName(type)});
				else
					message= RefactoringCoreMessages.getFormattedString("MoveMembersRefactoring.accessed_type", //$NON-NLS-1$
						new String[]{JavaModelUtil.getFullyQualifiedName(((IType)member)), 
									createAccessModifierString(member),
									JavaModelUtil.getFullyQualifiedName(type)});
				return message;
			}			
			default:
				Assert.isTrue(false);
				return null;
		}
	}
	
	private static String createAccessModifierString(IMember member) throws JavaModelException{
		if (JdtFlags.isPublic(member))
			return RefactoringCoreMessages.getString("MoveMembersRefactoring.public"); //$NON-NLS-1$
		else if (JdtFlags.isProtected(member))
			return RefactoringCoreMessages.getString("MoveMembersRefactoring.protected"); //$NON-NLS-1$
		else if (JdtFlags.isPrivate(member))
			return RefactoringCoreMessages.getString("MoveMembersRefactoring.private"); //$NON-NLS-1$
		else	
			return RefactoringCoreMessages.getString("MoveMembersRefactoring.package-visible"); //$NON-NLS-1$
	}
	
	private RefactoringStatus checkAccessedMembersAvailability(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 3); //$NON-NLS-1$
		RefactoringStatus result= new RefactoringStatus();
		result.merge(checkAccessedMethodsAvailability(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedFieldsAvailability(new SubProgressMonitor(pm, 1)));
		result.merge(checkAccessedTypesAvailability(new SubProgressMonitor(pm, 1)));
		pm.done();
		return result;
	}

	private RefactoringStatus checkAccessedMethodsAvailability(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(fMembers, pm);
		List movedElementList= Arrays.asList(fMembers);
		for (int i= 0; i < accessedMethods.length; i++) {
			if (movedElementList.contains(accessedMethods[i]))
				continue;
			if (! JdtFlags.isStatic(accessedMethods[i])) //safely ignore non-static 
				continue;
			if (! isVisibleFrom(accessedMethods[i], fDestinationType, accessedMethods[i].getDeclaringType())){
				String msg= createNonAccessibleMemberMessage(accessedMethods[i], fDestinationType, false);
				result.addWarning(msg, JavaSourceContext.create(accessedMethods[i]));
			}	
		}
		return result;
	}
	
	private RefactoringStatus checkAccessedTypesAvailability(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= ReferenceFinderUtil.getTypesReferencedIn(fMembers, pm);
		List movedElementList= Arrays.asList(fMembers);
		for (int i= 0; i < accessedTypes.length; i++) {
			if (movedElementList.contains(accessedTypes[i]))
				continue;
			if (! JdtFlags.isStatic(accessedTypes[i])) //safely ignore non-static 
				continue;
			if (! isVisibleFrom(accessedTypes[i], fDestinationType, accessedTypes[i].getDeclaringType())){
				String msg= createNonAccessibleMemberMessage(accessedTypes[i], fDestinationType, false);
				result.addWarning(msg, JavaSourceContext.create(accessedTypes[i]));
			}	
		}
		return result;
	}
	
	private RefactoringStatus checkAccessedFieldsAvailability(IProgressMonitor pm) throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(fMembers, pm);
		List movedElementList= Arrays.asList(fMembers);
		for (int i= 0; i < accessedFields.length; i++) {
			if (movedElementList.contains(accessedFields[i]))
				continue;
			if (! JdtFlags.isStatic(accessedFields[i])) //safely ignore non-static 
				continue;
			if (! isVisibleFrom(accessedFields[i], fDestinationType, accessedFields[i].getDeclaringType())){
				String msg= createNonAccessibleMemberMessage(accessedFields[i], fDestinationType, false);
				result.addWarning(msg, JavaSourceContext.create(accessedFields[i]));
			}	
		}
		return result;
	}
	
	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}
	
	private static boolean isVisibleFrom(IMember member, IType accessingType, IType newMemberDeclaringType) throws JavaModelException{
		if (JdtFlags.isPrivate(member))
			return newMemberDeclaringType.equals(accessingType); //roughly
		if (JdtFlags.isPublic(member)){
			if (JdtFlags.isPublic(newMemberDeclaringType)) //roughly
				return true;
			return accessingType.getPackageFragment().equals(newMemberDeclaringType.getPackageFragment());  //roughly
		}	
		if (JdtFlags.isProtected(member)){ //FIX ME
			if (JdtFlags.isPublic(newMemberDeclaringType))
				return true;
			return accessingType.getPackageFragment().equals(newMemberDeclaringType.getPackageFragment());
		}	
		else	
		    //default visibility
			return accessingType.getPackageFragment().equals(newMemberDeclaringType.getPackageFragment());  //roughly
	}
	
	private static boolean canDeclareStaticMembers(IType type) throws JavaModelException {
		return (JdtFlags.isStatic(type)) || (type.getDeclaringType() == null);
	}

	private RefactoringStatus checkAllElements() throws JavaModelException{
		//just 1 error message
		for (int i = 0; i < fMembers.length; i++) {
			IMember member = fMembers[i];

			if (member.getElementType() != IJavaElement.METHOD && 
				member.getElementType() != IJavaElement.FIELD)
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.fields_methods")); //$NON-NLS-1$
			if (! member.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.exist")); //$NON-NLS-1$
	
			if (member.isBinary())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.binary_elements"));	 //$NON-NLS-1$

			if (member.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.read_only_elements")); //$NON-NLS-1$

			if (! member.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.structure")); //$NON-NLS-1$

			if (member.getElementType() == IJavaElement.METHOD && member.getDeclaringType().isInterface())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.interface_methods")); //$NON-NLS-1$
				
			if (member.getElementType() == IJavaElement.METHOD && ! JdtFlags.isStatic(member))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.static_methods")); //$NON-NLS-1$

			if (! member.getDeclaringType().isInterface() && ! JdtFlags.isStatic(member))
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.static_elements")); //$NON-NLS-1$
			
			if (member.getElementType() == IJavaElement.METHOD && ((IMethod)member).isConstructor())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.constructors"));	//$NON-NLS-1$
		}
		return null;
	}
	
	private RefactoringStatus checkDeclaringType() throws JavaModelException{
		IType declaringType= getDeclaringType();
				
		if (JavaModelUtil.getFullyQualifiedName(declaringType).equals("java.lang.Object")) //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.Object"));	 //$NON-NLS-1$

		if (declaringType.isBinary())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.binary"));	 //$NON-NLS-1$

		if (declaringType.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("MoveMembersRefactoring.read_only"));	 //$NON-NLS-1$
		
		return null;
	}
	
	public IType getDeclaringType(){
		//all methods declared in same type - checked in precondition
		return  fMembers[0].getDeclaringType(); //index safe - checked in constructor
	}
	
	private boolean haveCommonDeclaringType(){
		IType declaringType= fMembers[0].getDeclaringType(); //index safe - checked in constructor
		for (int i= 0; i < fMembers.length; i++) {
			if (! declaringType.equals(fMembers[i].getDeclaringType()))
				return false;			
		}	
		return true;
	}
	
	private static IMember[] getOriginals(IMember[] members){
		IMember[] result= new IMember[members.length];
		for (int i= 0; i < members.length; i++) {
			result[i]= (IMember)WorkingCopyUtil.getOriginal(members[i]);
		}
		return result;
	}
	
	//------------------------
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			return new CompositeChange(RefactoringCoreMessages.getString("MoveMembersRefactoring.move_members"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally{
			pm.done();
		}	
	}
	
	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask(RefactoringCoreMessages.getString("MoveMembersRefactoring.analyzing"), 6); //$NON-NLS-1$
			TextChangeManager manager= new TextChangeManager();
			addCopyMembersChange(new SubProgressMonitor(pm, 1), manager);
			
			if (destinationCUNeedsAddedImports())
				addImportsToDestinationCu(new SubProgressMonitor(pm, 1));
				
			if (sourceCUNeedsAddedImports())
				addImportsToSourceCu();
			pm.worked(1);	
			
			addDeleteMembersChange(new SubProgressMonitor(pm, 1), manager);

			addModifyReferencesToMovedMembers(new SubProgressMonitor(pm, 1), manager);

			addImports(manager);
			
			return manager;
		} finally{
			pm.done();
		}	
	}
	
	private void addImports(TextChangeManager manager) throws CoreException{
		for (Iterator iter= fImportEdits.keySet().iterator(); iter.hasNext();) {
			ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists((ICompilationUnit) iter.next());
			ImportEdit edit= (ImportEdit)fImportEdits.get(cu);
			if (edit != null && ! edit.isEmpty())
				manager.get(cu).addTextEdit(RefactoringCoreMessages.getString("MoveMembersRefactoring.update_imports"), edit); //$NON-NLS-1$
		}
	}

	private boolean destinationCUNeedsAddedImports() {
		return ! getDeclaringType().getCompilationUnit().equals(fDestinationType.getCompilationUnit());
	}
	
	private boolean sourceCUNeedsAddedImports() {
		return ! getDeclaringType().getCompilationUnit().equals(fDestinationType.getCompilationUnit());
	}

	private void addCopyMembersChange(IProgressMonitor pm, TextChangeManager manager) throws CoreException {
		pm.beginTask("", fMembers.length);	 //$NON-NLS-1$
		for (int i = fMembers.length - 1; i >= 0; i--) { //backwards - to preserve order
			addCopyMemberChange(manager, fMembers[i], new SubProgressMonitor(pm, 1));
		}
		pm.done();
	}
	
	private void addCopyMemberChange(TextChangeManager manager, IMember member, IProgressMonitor pm) throws CoreException {
		String source= computeNewSource(member, pm);
		String changeName= RefactoringCoreMessages.getString("MoveMembersRefactoring.Copy") + member.getElementName();								 //$NON-NLS-1$
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fDestinationType.getCompilationUnit());
		manager.get(cu).addTextEdit(changeName, createAddMemberEdit(source, member.getElementType()));
	}
	
	private String computeNewSource(IMember member, IProgressMonitor pm) throws JavaModelException {
		String originalSource= SourceReferenceSourceRangeComputer.computeSource(member);
		StringBuffer modifiedSource= new StringBuffer(originalSource);
		
		//ISourceRange -> String (new source)
		Map accessModifications= getStaticMemberAccessesInMovedMember(member, pm);
		ISourceRange[] ranges= (ISourceRange[]) accessModifications.keySet().toArray(new ISourceRange[accessModifications.keySet().size()]);
		ISourceRange[] sortedRanges= SourceRange.reverseSortByOffset(ranges);
		
		ISourceRange originalRange= SourceReferenceSourceRangeComputer.computeSourceRange(member, member.getCompilationUnit().getSource());
		
		for (int i= 0; i < sortedRanges.length; i++) {
			int start= sortedRanges[i].getOffset() - originalRange.getOffset();
			int end= start + sortedRanges[i].getLength();
			modifiedSource.replace(start, end, (String)accessModifications.get(sortedRanges[i]));
		}
		return modifiedSource.toString();
	}
	
	private Map getStaticMemberAccessesInMovedMember(IMember member, IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("", 3); //$NON-NLS-1$
		Map resultMap= new HashMap();
		resultMap.putAll(getFieldAccessModificationsInMovedMember(member, new SubProgressMonitor(pm, 1)));
		resultMap.putAll(getMethodSendsInMovedMember(member, new SubProgressMonitor(pm, 1)));
		resultMap.putAll(getTypeReferencesInMovedMember(member, new SubProgressMonitor(pm, 1)));
		pm.done();
		return resultMap;
	}
	
	private TextEdit createAddMemberEdit(String source, int memberType) throws JavaModelException {
		IMember sibling= getLastMember(fDestinationType, memberType);
		if (sibling != null)
			return new MemberEdit(sibling, MemberEdit.INSERT_AFTER, new String[]{ source}, getTabWidth());
		return new MemberEdit(fDestinationType, MemberEdit.ADD_AT_END, new String[]{ source}, getTabWidth());
	}
	
	private void addDeleteMembersChange(IProgressMonitor pm, TextChangeManager manager) throws CoreException {
		pm.beginTask("", fMembers.length); //$NON-NLS-1$
		for (int i = 0; i < fMembers.length; i++) {
			String changeName= RefactoringCoreMessages.getString("MoveMembersRefactoring.delete") + fMembers[i].getElementName(); //$NON-NLS-1$
			DeleteSourceReferenceEdit edit= new DeleteSourceReferenceEdit(fMembers[i], fMembers[i].getCompilationUnit());
			ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fMembers[i].getCompilationUnit());
			manager.get(cu).addTextEdit(changeName, edit);
			pm.worked(1);
		}
		pm.done();
	}
	
	private ImportEdit getImportEdit(ICompilationUnit cu){
		if (fImportEdits.containsKey(cu))
			return (ImportEdit)fImportEdits.get(cu);
		
		ImportEdit edit= new ImportEdit(cu, fPreferenceSettings);	
		fImportEdits.put(cu, edit);	
		return edit;
	}
	
	private void addImportsToDestinationCu(IProgressMonitor pm) throws CoreException {
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(fDestinationType.getCompilationUnit());		
		IType[] referencedTypes= ReferenceFinderUtil.getTypesReferencedIn(fMembers, pm);
		for (int i= 0; i < referencedTypes.length; i++) {
			addImportTo(referencedTypes[i], cu);
		}
	}
	
	private void addImportsToSourceCu() throws CoreException {
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getDeclaringType().getCompilationUnit());		
		addImportTo(fDestinationType, cu);
	}
	
	private void addModifyReferencesToMovedMembers(IProgressMonitor pm, TextChangeManager manager) throws CoreException {
		pm.beginTask("", fMembers.length); //$NON-NLS-1$
		for (int i= 0; i < fMembers.length; i++) {
			addModifyReferencesToMovedMember(fMembers[i], new SubProgressMonitor(pm, 1), manager);	
		}
		pm.done();
	}
	
	private void addModifyReferencesToMovedMember(IMember member, IProgressMonitor pm, TextChangeManager manager) throws CoreException {
		pm.beginTask("", 2); //$NON-NLS-1$
		
		SearchResultGroup[] results= findReferencesToMember(member, new SubProgressMonitor(pm, 1));
		
		for (int i= 0; i < results.length; i++) {
			SearchResultGroup searchResultGroup= results[i];
			IJavaElement referencingElement= JavaCore.create(searchResultGroup.getResource());
			if (referencingElement == null || referencingElement.getElementType() != IJavaElement.COMPILATION_UNIT)
				continue;
			ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists((ICompilationUnit)referencingElement);
			SearchResultGroup modifiedGroup= searchResultGroup;
	
			if (ResourceUtil.getResource(getDeclaringType()).equals(ResourceUtil.getResource(cu)))
				modifiedGroup= removeReferencesEnclosedIn(fMembers, searchResultGroup);
	
			modifyReferencesToMovedMember(member, manager, modifiedGroup, cu);
			addImportTo(fDestinationType, cu);
		}
		pm.done();
	}
	
	private static SearchResultGroup removeReferencesEnclosedIn(IJavaElement[] elements, SearchResultGroup group){
		List elementList= Arrays.asList(elements);
		
		List searchResultList= new ArrayList(group.getSearchResults().length);
		SearchResult[] searchResults= group.getSearchResults();
		for (int i= 0; i < searchResults.length; i++) {
			if (! elementList.contains(searchResults[i].getEnclosingElement()))
				searchResultList.add(searchResults[i]);
		}
		SearchResult[] searchResultArray= (SearchResult[]) searchResultList.toArray(new SearchResult[searchResultList.size()]);
		return new SearchResultGroup(group.getResource(), searchResultArray);
	}

	private void modifyReferencesToMovedMember(IMember member, TextChangeManager manager, SearchResultGroup searchResultGroup, ICompilationUnit cu) throws JavaModelException, CoreException {
		ISourceRange[] ranges= findMemberReferences(member.getElementType(), searchResultGroup);
		String text= fDestinationType.getElementName() + "." + member.getElementName(); //$NON-NLS-1$
		for (int i= 0; i < ranges.length; i++) {
			ISourceRange iSourceRange= ranges[i];
			TextEdit edit= SimpleTextEdit.createReplace(iSourceRange.getOffset(), iSourceRange.getLength(), text);
			manager.get(cu).addTextEdit(RefactoringCoreMessages.getString("MoveMembersRefactoring.convert"), edit); //$NON-NLS-1$
		}	
	}

	private static ISourceRange[] findMemberReferences(int memberType, SearchResultGroup searchResultGroup) throws JavaModelException {
		Assert.isTrue(memberType == IJavaElement.METHOD || memberType == IJavaElement.FIELD);
		if (memberType == IJavaElement.METHOD)
			return MethodInvocationFinder.findMessageSendRanges(searchResultGroup);
		else
			return FieldReferenceFinder.findFieldReferenceRanges(searchResultGroup);
	}

	private static SearchResultGroup[] findReferencesToMember(IMember member, IProgressMonitor pm) throws JavaModelException {
		ISearchPattern pattern= SearchEngine.createSearchPattern(member, IJavaSearchConstants.REFERENCES);
		IJavaSearchScope scope= RefactoringScopeFactory.create(member);
		return RefactoringSearchEngine.search(pm, scope, pattern);
	}
	
	//ISourceRange -> String (new source)
	private Map getFieldAccessModificationsInMovedMember(IMember member, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		Map result= new HashMap();
		IField[] fields= ReferenceFinderUtil.getFieldsReferencedIn(new IMember[]{member}, new SubProgressMonitor(pm, 1));
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getDeclaringType().getCompilationUnit());
		IMember[] interestingFields= getMembersThatNeedReferenceConversion(fields);
		for (int i= 0; i < interestingFields.length; i++) {
			IField field= (IField)interestingFields[i];
			//XX side effect
			addImportTo(field.getDeclaringType(), cu);
			String newSource= field.getDeclaringType().getElementName() + "." + field.getElementName(); //$NON-NLS-1$
			SearchResult[] searchResults= findReferencesInMember(member, field, new SubProgressMonitor(pm, 1));
			ISourceRange[] ranges= FieldReferenceFinder.findFieldReferenceRanges(searchResults, cu);
			putAllToMap(result, newSource, ranges);		
		}
		return result;
	}

	//ISourceRange -> String (new source)
	private Map getMethodSendsInMovedMember(IMember member, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		Map result= new HashMap();
		IMethod[] methods= ReferenceFinderUtil.getMethodsReferencedIn(new IMember[]{member}, new SubProgressMonitor(pm, 1));
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getDeclaringType().getCompilationUnit());
		IMember[] interestingMethods= getMembersThatNeedReferenceConversion(methods);
		for (int i= 0; i < interestingMethods.length; i++) {
			IMethod method= (IMethod)interestingMethods[i];
			//XX side effect
			addImportTo(method.getDeclaringType(), cu);
			String newSource= method.getDeclaringType().getElementName() + "." + method.getElementName(); //$NON-NLS-1$
			SearchResult[] searchResults= findReferencesInMember(member, method, new SubProgressMonitor(pm, 1));
			ISourceRange[] ranges= MethodInvocationFinder.findMessageSendRanges(searchResults, cu);
			putAllToMap(result, newSource, ranges);
		}
		return result;
	}
	
	//ISourceRange -> String (new source)
	private Map getTypeReferencesInMovedMember(IMember member, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2); //$NON-NLS-1$
		Map result= new HashMap();
		IType[] types= ReferenceFinderUtil.getTypesReferencedIn(new IMember[]{member}, new SubProgressMonitor(pm, 1));
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getDeclaringType().getCompilationUnit());
		IMember[] interestingMethods= getMembersThatNeedReferenceConversion(types);
		for (int i= 0; i < interestingMethods.length; i++) {
			IType type= (IType)interestingMethods[i];
			//XX side effect
			addImportTo(type.getDeclaringType(), cu);
			String newSource= type.getDeclaringType().getElementName() + "." + type.getElementName(); //$NON-NLS-1$
			SearchResult[] searchResults= findReferencesInMember(member, type, new SubProgressMonitor(pm, 1));
			ISourceRange[] ranges= TypeReferenceFinder.findTypeReferenceRanges(searchResults, cu);
			putAllToMap(result, newSource, ranges);
		}
		return result;
	}
	
	private void addImportTo(IType type, ICompilationUnit cu){
		getImportEdit(cu).addImport(JavaModelUtil.getFullyQualifiedName(type));
	}

	private static void putAllToMap(Map result, String newSource, ISourceRange[] ranges) {
		for (int j= 0; j < ranges.length; j++) {
			result.put(ranges[j], newSource);
		}
	}
	
	private static SearchResult[] findReferencesInMember(IMember scopeMember, IMember referenceMember, IProgressMonitor pm) throws JavaModelException {
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[]{scopeMember});
		SearchResultCollector collector= new SearchResultCollector(pm);
		new SearchEngine().search(ResourcesPlugin.getWorkspace(), referenceMember, IJavaSearchConstants.REFERENCES, scope, collector);
		List results= collector.getResults();
		SearchResult[] searchResults= (SearchResult[]) results.toArray(new SearchResult[results.size()]);
		return searchResults;
	}
		
	private IMember[] getMembersThatNeedReferenceConversion(IMember[] members) throws JavaModelException{
		Set memberSet= new HashSet(); //using set to remove dups
		for (int i= 0; i < members.length; i++) {
			if (willNeedToConvertReferenceTo(members[i]))
				memberSet.add(members[i]);
		}
		return (IMember[]) memberSet.toArray(new IMember[memberSet.size()]);
	}
	
	private boolean willNeedToConvertReferenceTo(IMember member) throws JavaModelException{
		if (! member.exists())
			return false;
		List memberList= Arrays.asList(fMembers);
		if (memberList.contains(member))
			return false;
		if (! JdtFlags.isStatic(member)) //convert all static references
			return false;
		return true;		
	}
	
	//--- helpers

	private static int getTabWidth() {
		return Integer.parseInt((String)JavaCore.getOptions().get(JavaCore.FORMATTER_TAB_SIZE));
	}
	
	private static IMember getLastMember(IType type, int elementType) throws JavaModelException {
		if (elementType == IJavaElement.METHOD)
			return getLastMethod(type);
		if (elementType == IJavaElement.FIELD)
			return getLastField(type);
		Assert.isTrue(false);
		return null;	
	}
	
	private static IMethod getLastMethod(IType type) throws JavaModelException {
		if (type == null)
			return null;
		IMethod[] methods= type.getMethods();
		if (methods.length == 0)
			return null;
		return methods[methods.length - 1];	
	}
	
	private static IField getLastField(IType type) throws JavaModelException {
		if (type == null)
			return null;
		IField[] fields= type.getFields();
		if (fields.length == 0)
			return null;
		return fields[fields.length - 1];	
	}
}
