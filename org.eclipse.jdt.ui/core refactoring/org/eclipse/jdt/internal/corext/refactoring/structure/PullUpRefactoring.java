package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.text.MessageFormat;
import java.util.Arrays;
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
import org.eclipse.jdt.core.IJavaModelStatusConstants;
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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
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
import org.eclipse.jdt.internal.corext.refactoring.reorg.DeleteSourceReferenceEdit;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

public class PullUpRefactoring extends Refactoring {

	private final CodeGenerationSettings fPreferenceSettings;
	private IMember[] fElementsToPullUp;
	private IMethod[] fMethodsToDelete;
	private TextChangeManager fChangeManager;

	//caches
	private IType fCachedSuperType;
	private ITypeHierarchy fCachedSuperTypeHierarchy;
	private IType fCachedDeclaringType;
	private IType[] fTypesReferencedInMovedMembers;

	public PullUpRefactoring(IMember[] elements, CodeGenerationSettings preferenceSettings){
		Assert.isTrue(elements.length > 0);
		Assert.isNotNull(preferenceSettings);
		fElementsToPullUp= (IMember[])SourceReferenceUtil.sortByOffset(elements);
		fMethodsToDelete= new IMethod[0];
		fPreferenceSettings= preferenceSettings;
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
	
	private IMember[] getMembersToDelete(IProgressMonitor pm){
		try{
			pm.beginTask("", 1); //$NON-NLS-1$
			IMember[] matchingFields= getMembersOfType(getMatchingElements(new SubProgressMonitor(pm, 1)), IJavaElement.FIELD);
			return merge(getOriginals(matchingFields), fMethodsToDelete);
		} catch (JavaModelException e){
			//fallback
			return fMethodsToDelete;
		}	finally{
			pm.done();
		}
	}
	
	public IMember[] getElementsToPullUp() {
		return fElementsToPullUp;
	}
	
	public IType getDeclaringType(){
		if (fCachedDeclaringType != null)
			return fCachedDeclaringType;
		//all members declared in same type - checked in precondition
		fCachedDeclaringType= fElementsToPullUp[0].getDeclaringType(); //index safe - checked in constructor
		return fCachedDeclaringType;
	}
	
	//XXX added for performance reasons
	public ITypeHierarchy getSuperTypeHierarchy(IProgressMonitor pm) throws JavaModelException {
		try{
			pm.beginTask("", 2); //$NON-NLS-1$
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
			pm.beginTask("", 1); //$NON-NLS-1$
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
			pm.beginTask("", 2); //$NON-NLS-1$
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
				IMethod found= MemberCheckUtil.findMethod(method, type.getMethods());
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
			fElementsToPullUp= getOriginals(fElementsToPullUp);
						
			result.merge(checkDeclaringType(new SubProgressMonitor(pm, 1)));
			pm.worked(1);
			if (result.hasFatalError())
				return result;			

			if (getSuperType(new SubProgressMonitor(pm, 1)) == null)
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.not_allowed")); //$NON-NLS-1$
			if (getSuperType(new SubProgressMonitor(pm, 1)).isBinary())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.subtypes_of_binary_types")); //$NON-NLS-1$

			for (int i= 0; i < fElementsToPullUp.length; i++) {
				IMember orig= fElementsToPullUp[i];
				if (orig == null || ! orig.exists()){
					String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.does_not_exist", orig.getElementName());//$NON-NLS-1$
					result.addFatalError(message);
				}	
			}
			result.merge(checkMultiDeclarationOfFields());
			if (result.hasFatalError())
				return result;			
			return new RefactoringStatus();
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus checkMultiDeclarationOfFields() throws JavaModelException {
		RefactoringStatus result= new RefactoringStatus();
		ASTNodeMappingManager astManager= new ASTNodeMappingManager();
		for (int i= 0; i < fElementsToPullUp.length; i++) {
			IMember member= fElementsToPullUp[i];
			if (member.getElementType() != IJavaElement.FIELD)
				continue;
			if (isPartOfMultiDeclaration((IField)member, astManager)){
				Context context= JavaSourceContext.create(member);
				String pattern= "Field ''{0}'' is declared in a multi declaration. Pulling up is currently not supported.";
				String msg= MessageFormat.format(pattern, new String[]{member.getElementName()});
				result.addFatalError(msg, context);
			}
		}
		return result;
	}

	private boolean isPartOfMultiDeclaration(IField iField, ASTNodeMappingManager manager) throws JavaModelException {
		Selection selection= Selection.createFromStartLength(iField.getNameRange().getOffset(), iField.getNameRange().getLength());
		SelectionAnalyzer analyzer= new SelectionAnalyzer(selection, true);
		manager.getAST(iField.getCompilationUnit()).accept(analyzer);
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
			pm.beginTask("", 5); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkFinalFields(new SubProgressMonitor(pm, 1)));
			result.merge(checkAccesses(new SubProgressMonitor(pm, 1)));
			result.merge(MemberCheckUtil.checkMembersInDestinationType(fElementsToPullUp, getSuperType(new SubProgressMonitor(pm, 1))));
			pm.worked(1);
			result.merge(checkMembersInSubclasses(new SubProgressMonitor(pm, 1)));
			
			if (result.hasFatalError())
				return result;
			
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1));
			result.merge(validateModifiesFiles());
			return result;
		} catch (CoreException e){	
			throw new JavaModelException(e);
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
					return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.only_fields_and_methods"));			 //$NON-NLS-1$
			if (! member.exists())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.elements_do_not_exist"));			 //$NON-NLS-1$
	
			if (member.isBinary())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_binary_elements"));	 //$NON-NLS-1$

			if (member.isReadOnly())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_read_only_elements"));					 //$NON-NLS-1$

			if (! member.isStructureKnown())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_unknown_structure"));					 //$NON-NLS-1$

			if (JdtFlags.isStatic(member)) //for now
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_static_elements")); //$NON-NLS-1$
			
			if (member.getElementType() == IJavaElement.METHOD){
				RefactoringStatus substatus= checkMethod((IMethod)member);
				if (substatus != null && ! substatus.isOK())
					return substatus;	
			}	
		}
		return null;
	}
	
	private static RefactoringStatus checkMethod(IMethod method) throws JavaModelException {
		if (method.isConstructor())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_constructors"));			 //$NON-NLS-1$
			
		if (JdtFlags.isAbstract(method))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_abstract_methods")); //$NON-NLS-1$
			
 		if (JdtFlags.isNative(method)) //for now - move to input preconditions
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_native_methods"));				 //$NON-NLS-1$

		return null;	
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
	
		if (getSuperType(new SubProgressMonitor(pm, 1)) == null)	
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.not_this_type"));	 //$NON-NLS-1$
			
		if (getSuperType(new SubProgressMonitor(pm, 1)).isBinary())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_subclasses_of_binary_types"));	 //$NON-NLS-1$

		if (getSuperType(new SubProgressMonitor(pm, 1)).isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_subclasses_of_read_only_types"));	 //$NON-NLS-1$
		
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
	
	private RefactoringStatus checkFinalFields(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", fElementsToPullUp.length); //$NON-NLS-1$
		for (int i= 0; i < fElementsToPullUp.length; i++) {
			if (fElementsToPullUp[i].getElementType() == IJavaElement.FIELD && JdtFlags.isFinal(fElementsToPullUp[i])){
				Context context= JavaSourceContext.create(fElementsToPullUp[i]);
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
		return result;
	}
	
	private RefactoringStatus checkAccessedTypes(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		IType[] accessedTypes= getTypeReferencedInMovedMembers(pm);
		IType superType= getSuperType(new NullProgressMonitor());
		for (int i= 0; i < accessedTypes.length; i++) {
			IType iType= accessedTypes[i];
			if (! iType.exists())
				continue;
			
			if (! canBeAccessedFrom(iType, superType)){
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.type_not_accessible", //$NON-NLS-1$
					new String[]{JavaModelUtil.getFullyQualifiedName(iType), JavaModelUtil.getFullyQualifiedName(superType)});
				result.addError(message, JavaSourceContext.create(iType));
			}	
		}
		return result;
	}
	
	private RefactoringStatus checkAccessedFields(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		
		List pulledUpList= Arrays.asList(fElementsToPullUp);
		List deletedList= Arrays.asList(getMembersToDelete(new NullProgressMonitor()));
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(fElementsToPullUp, pm);
		
		IType superType= getSuperType(new NullProgressMonitor());
		for (int i= 0; i < accessedFields.length; i++) {
			IField iField= accessedFields[i];
			if (! iField.exists())
				continue;
			
			if (! canBeAccessedFrom(iField, superType) && ! pulledUpList.contains(iField) && !deletedList.contains(iField)){
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.field_not_accessible", //$NON-NLS-1$
					new String[]{iField.getElementName(), JavaModelUtil.getFullyQualifiedName(superType)});
				result.addError(message, JavaSourceContext.create(iField));
			}	
		}
		return result;
	}

	private RefactoringStatus checkAccessedMethods(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		
		List pulledUpList= Arrays.asList(fElementsToPullUp);
		List deletedList= Arrays.asList(getMembersToDelete(new NullProgressMonitor()));
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(fElementsToPullUp, pm);
		
		IType superType= getSuperType(new NullProgressMonitor());
		for (int i= 0; i < accessedMethods.length; i++) {
			IMethod iMethod= accessedMethods[i];
			if (! iMethod.exists())
				continue;
			if (! canBeAccessedFrom(iMethod, superType) && ! pulledUpList.contains(iMethod) && !deletedList.contains(iMethod)){
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.method_not_accessible", //$NON-NLS-1$
					new String[]{JavaElementUtil.createMethodSignature(iMethod), JavaModelUtil.getFullyQualifiedName(superType)});
				result.addError(message, JavaSourceContext.create(iMethod));
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
			
		if (JdtFlags.isPrivate(member))
			return false;
			
		if (member.getDeclaringType() == null){ //top level
			if (! (member instanceof IType))
				return false;

			if (JdtFlags.isPublic(member))
				return true;
				
			//FIX ME: protected and default treated the same
			return ((IType)member).getPackageFragment().equals(newHome.getPackageFragment());		
		}	

		 if (! canBeAccessedFrom(member.getDeclaringType(), newHome))
		 	return false;

		if (member.getDeclaringType().equals(getDeclaringType())) //XXX
			return false;
			
		if (JdtFlags.isPublic(member))
			return true;
		 
		//FIX ME: protected and default treated the same
		return (member.getDeclaringType().getPackageFragment().equals(newHome.getPackageFragment()));		
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
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.different_method_return_type", //$NON-NLS-1$
					new String[]{JavaElementUtil.createMethodSignature(matchingMethod),
								JavaModelUtil.getFullyQualifiedName(matchingMethod.getDeclaringType())});
				Context context= JavaSourceContext.create(matchingMethod.getCompilationUnit(), matchingMethod.getNameRange());
				result.addError(message, context);	
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
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.different_field_type", //$NON-NLS-1$
					new String[]{matchingField.getElementName(), JavaModelUtil.getFullyQualifiedName(matchingField.getDeclaringType())});
				Context context= JavaSourceContext.create(matchingField.getCompilationUnit(), matchingField.getSourceRange());					 
				result.addError(message, context);	
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
		Context errorContext= JavaSourceContext.create(method);
		
		if (JdtFlags.isStatic(method)){
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.static_method", //$NON-NLS-1$
					new String[]{JavaElementUtil.createMethodSignature(method), JavaModelUtil.getFullyQualifiedName(method.getDeclaringType())});
				result.addError(message, errorContext);
		 } 
		 if (isVisibilityLowerThanProtected(method)){
		 	String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.lower_visibility", //$NON-NLS-1$
		 		new String[]{JavaElementUtil.createMethodSignature(method), JavaModelUtil.getFullyQualifiedName(method.getDeclaringType())});
			result.addError(message, errorContext);	
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
	
	private IFile[] getAllFilesToModify() throws CoreException{
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}
	
	private RefactoringStatus validateModifiesFiles() throws CoreException{
		return Checks.validateModifiesFiles(getAllFilesToModify());
	}
	
	private static boolean isVisibilityLowerThanProtected(IMember member)throws JavaModelException {
		return ! (JdtFlags.isPublic(member) || JdtFlags.isProtected(member));
	}
	
	//--- change creation
	
	/*
	 * @see IRefactoring#createChange(IProgressMonitor)
	 */
	public IChange createChange(IProgressMonitor pm) throws JavaModelException {
		try{
			fChangeManager= createChangeManager(pm);
			return new CompositeChange(RefactoringCoreMessages.getString("PullUpRefactoring.Pull_Up"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} catch(CoreException e){
			throw new JavaModelException(e);
		} finally{
			pm.done();
		}
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.preview"), 3); //$NON-NLS-1$
			
			TextChangeManager manager= new TextChangeManager();
			
			addCopyMembersChange(manager);
			pm.worked(1);
			
			if (needsAddingImports())
				addAddImportsChange(manager, new SubProgressMonitor(pm, 1));
			else	
				pm.worked(1);
			
			addDeleteMembersChange(manager);
			pm.worked(1);
			
			return manager;
		} finally{
			pm.done();
		}
	}

	private boolean needsAddingImports() throws JavaModelException {
		return ! getSuperType(new NullProgressMonitor()).getCompilationUnit().equals(getDeclaringType().getCompilationUnit());
	}
	
	private void addCopyMembersChange(TextChangeManager manager) throws CoreException {
		for (int i = fElementsToPullUp.length - 1; i >= 0; i--) { //backwards - to preserve method order
			addCopyChange(manager, fElementsToPullUp[i]);
		}
	}
	
	private void addCopyChange(TextChangeManager manager, IMember member) throws CoreException {
		String source= computeNewSource(member);
		String changeName= getCopyChangeName(member);
									
		if (needsToChangeVisibility(member)){
			changeName += RefactoringCoreMessages.getFormattedString("PullUpRefactoring.changing_visibility_to", //$NON-NLS-1$
					"protected"); //$NON-NLS-1$
		}	
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getSuperType(new NullProgressMonitor()).getCompilationUnit());
		manager.get(cu).addTextEdit(changeName, createAddMemberEdit(source));
	}

	private String getCopyChangeName(IMember member) throws JavaModelException {
		if (member.getElementType() == IJavaElement.METHOD){
			String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.copy_method", //$NON-NLS-1$
				new String[]{JavaElementUtil.createMethodSignature((IMethod)member), JavaModelUtil.getFullyQualifiedName(getDeclaringType())});
			return message;
		} else {
			String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.copy_field", //$NON-NLS-1$
				new String[]{member.getElementName(), JavaModelUtil.getFullyQualifiedName(getDeclaringType())});
		 	return message;
		}
	}
		
	private TextEdit createAddMemberEdit(String methodSource) throws JavaModelException {
		IMethod sibling= getLastMethod(getSuperType(new NullProgressMonitor()));
		String[] sourceLines= Strings.removeTrailingEmptyLines(Strings.convertIntoLines(methodSource));
		if (sibling != null)
			return new MemberEdit(sibling, MemberEdit.INSERT_AFTER, sourceLines, CodeFormatterUtil.getTabWidth());
		return
			new MemberEdit(getSuperType(new NullProgressMonitor()), MemberEdit.ADD_AT_END, sourceLines, CodeFormatterUtil.getTabWidth());
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
			String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Delete_method", //$NON-NLS-1$
				new String[]{JavaElementUtil.createMethodSignature(method), JavaModelUtil.getFullyQualifiedName(method.getDeclaringType())});
			return message;
		} else {
			String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.Delete_field", //$NON-NLS-1$
				new String[]{member.getElementName(), JavaModelUtil.getFullyQualifiedName(member.getDeclaringType())});
			return message;	
		}
	}
	
	private void addAddImportsChange(TextChangeManager manager, IProgressMonitor pm) throws CoreException {
		ICompilationUnit cu= WorkingCopyUtil.getWorkingCopyIfExists(getSuperType(new NullProgressMonitor()).getCompilationUnit());		
		IType[] referencedTypes= getTypeReferencedInMovedMembers(pm);
		ImportEditManager importEditManager= new ImportEditManager(fPreferenceSettings);
		for (int i= 0; i < referencedTypes.length; i++) {
			importEditManager.addImportTo(referencedTypes[i], cu);
		}
		importEditManager.fill(manager);
	}
	
	private IType[] getTypeReferencedInMovedMembers(IProgressMonitor pm) throws JavaModelException {
		if (fTypesReferencedInMovedMembers == null)
			fTypesReferencedInMovedMembers= ReferenceFinderUtil.getTypesReferencedIn(fElementsToPullUp, pm);
		return fTypesReferencedInMovedMembers;
	}

	private static boolean needsToChangeVisibility(IMember method) throws JavaModelException {
		return ! (JdtFlags.isPublic(method) || JdtFlags.isProtected(method));
	}
	
	private String computeNewSource(IMember member) throws JavaModelException {
		String source;
		
		if (member.getElementType() == IJavaElement.METHOD)
			source= replaceSuperCalls((IMethod)member);
		else
			source= SourceRangeComputer.computeSource(member);

		if (! needsToChangeVisibility(member))
			return source;
			
		if (JdtFlags.isPrivate(member))
			return substitutePrivateWithProtected(source);
		
		return addProtectedBeforeFirstToken(source);		
	}
	
	private static String addProtectedBeforeFirstToken(String source){
		int firstTokenStart= findFirstTokenOffset(source);
		StringBuffer buff= new StringBuffer();
		buff.append(source.substring(0, firstTokenStart));
		buff.append("protected "); //$NON-NLS-1$
		buff.append(source.substring(firstTokenStart));
		return buff.toString();
	}

	private static int findFirstTokenOffset(String source){
		try {
			IScanner scanner= ToolFactory.createScanner(false, false, false, false);
			scanner.setSource(source.toCharArray());
			scanner.getNextToken();
			return scanner.getCurrentTokenStartPosition();
		} catch (InvalidInputException e) {
			return 0;
		}
	}
	
	private String replaceSuperCalls(IMethod method) throws JavaModelException {
		ISourceRange[] superRefOffsert= SourceRange.reverseSortByOffset(SuperReferenceFinder.findSuperReferenceRanges(method, getSuperType(new NullProgressMonitor())));
		
		StringBuffer source= new StringBuffer(SourceRangeComputer.computeSource(method));
		ISourceRange originalMethodRange= SourceRangeComputer.computeSourceRange(method, method.getCompilationUnit().getSource());
		
		for (int i= 0; i < superRefOffsert.length; i++) {
			int start= superRefOffsert[i].getOffset() - originalMethodRange.getOffset();
			int end= start + superRefOffsert[i].getLength();
			source.replace(start, end, "this"); //$NON-NLS-1$
		}
		return source.toString();
	}
	
	private static String removeLeadingWhiteSpaces(String s){
		if ("".equals(s)) //$NON-NLS-1$
			return ""; //$NON-NLS-1$

		if ("".equals(s.trim()))	 //$NON-NLS-1$
			return ""; //$NON-NLS-1$
			
		int i= 0;
		while (i < s.length() && Character.isWhitespace(s.charAt(i))){
			i++;
		}
		if (i == s.length())
			return ""; //$NON-NLS-1$
		return s.substring(i);
	}
	
	private static String substitutePrivateWithProtected(String methodSource) throws JavaModelException {
		IScanner scanner= ToolFactory.createScanner(false, false, false, false);
		scanner.setSource(methodSource.toCharArray());
		int offset= 0;
		int token= 0;
		try {
			while((token= scanner.getNextToken()) != ITerminalSymbols.TokenNameEOF) {
				if (token == ITerminalSymbols.TokenNameprivate) {
					offset= scanner.getCurrentTokenStartPosition();
					break;
				}
			}
		} catch (InvalidInputException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.INVALID_CONTENTS);
		}
		//must do it this way - unicode
		int length= scanner.getCurrentTokenSource().length;
		return  new StringBuffer(methodSource).delete(offset, offset + length).insert(offset, "protected").toString(); //$NON-NLS-1$
	}
	
	private static IMethod getLastMethod(IType type) throws JavaModelException {
		if (type == null)
			return null;
		IMethod[] methods= type.getMethods();
		if (methods.length == 0)
			return null;
		return methods[methods.length - 1];	
	}
}

