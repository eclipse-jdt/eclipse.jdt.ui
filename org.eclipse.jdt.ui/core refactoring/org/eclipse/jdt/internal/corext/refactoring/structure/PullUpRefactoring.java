package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.text.MessageFormat;
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
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.ASTRewrite;
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
	private static class SuperReferenceFinder {

		//no instances
		private SuperReferenceFinder(){
		}
	
		public static ISourceRange[] findSuperReferenceRanges(IMethod method, IType superType) throws JavaModelException{
			Assert.isNotNull(method);
			SuperReferenceFinderVisitor visitor= new SuperReferenceFinderVisitor(method, superType);
			AST.parseCompilationUnit(method.getCompilationUnit(), true).accept(visitor);
			return visitor.getSuperReferenceRanges();
		}

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
	}

	private final CodeGenerationSettings fPreferenceSettings;
	private IMember[] fElementsToPullUp;
	private IMethod[] fMethodsToDelete;
	private TextChangeManager fChangeManager;
	private IType fTargetType;
	
	private final ASTNodeMappingManager fAstManager;
	private final ASTRewriteManager fRewriteManager;
	private final ImportEditManager fImportEditManager;

	//caches
	private IType fCachedDeclaringType;
	private IType[] fTypesReferencedInMovedMembers;
	private ITypeHierarchy fCachedTargetClassHierarchy;
	private Set fCachedSkippedSuperclasses;

	public PullUpRefactoring(IMember[] elements, CodeGenerationSettings preferenceSettings){
		Assert.isTrue(elements.length > 0);
		Assert.isNotNull(preferenceSettings);
		fElementsToPullUp= (IMember[])SourceReferenceUtil.sortByOffset(elements);
		fMethodsToDelete= new IMethod[0];
		fPreferenceSettings= preferenceSettings;
		fAstManager= new ASTNodeMappingManager();
		fRewriteManager= new ASTRewriteManager(fAstManager);
		fImportEditManager= new ImportEditManager(preferenceSettings);
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
	public void setElementsToPullUp(IMember[] elements){
		Assert.isNotNull(elements);
		fElementsToPullUp= (IMember[])SourceReferenceUtil.sortByOffset(elements);
		fElementsToPullUp= getOriginals(fElementsToPullUp);
	}
	
	private IMember[] getMembersToDelete(IProgressMonitor pm){
		try{
			IMember[] matchingFields= getMembersOfType(getMatchingElements(pm), IJavaElement.FIELD);
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
	
	public IMember[] getPullableMembersOfDeclaringType() {
		try{
			List list= new ArrayList(3);
			addAllPullable(getDeclaringType().getFields(), list);
			addAllPullable(getDeclaringType().getMethods(), list);
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
	private boolean isPullable(IMember member) throws JavaModelException {
		return checkElement(member) == null;
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
		return clazz != null && clazz.exists() && ! clazz.isReadOnly() && ! clazz.isBinary() && ! "java.lang.Object".equals(clazz.getFullyQualifiedName());
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
	
	public IMember[] getMatchingElements(IProgressMonitor pm) throws JavaModelException {
		try{	
			Set result= new HashSet();
			IType targetClass= getTargetClass();
			ITypeHierarchy hierarchy= getTypeHierarchyOfTargetClass(pm);
			IType[] subTypes= hierarchy.getAllSubtypes(targetClass);
			for (int i = 0; i < subTypes.length; i++) {
				result.addAll(getMatchingMembers(hierarchy, subTypes[i]));
			}
			return (IMember[]) result.toArray(new IMember[result.size()]);
		} finally{
			pm.done();
		}				
	}
	
	public IMember[] getRequiredPullableMembers(IProgressMonitor pm) throws JavaModelException{
		pm.beginTask("Calculating required members", fElementsToPullUp.length);//not true, but not easy to give anything better
		List queue= new ArrayList(fElementsToPullUp.length);
		queue.addAll(Arrays.asList(fElementsToPullUp));
		int i= 0;
		IMember current;
		do{
			current= (IMember)queue.get(i);
			addAllRequiredPullableMembers(queue, current, new SubProgressMonitor(pm, 1));
			i++;
			if (queue.size() == i)
				current= null;
		} while(current != null);
		return (IMember[]) queue.toArray(new IMember[queue.size()]);
	}
	
	private void addAllRequiredPullableMembers(List queue, IMember member, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2);
		addAllRequiredPullableMethods(queue, member, new SubProgressMonitor(pm, 1));
		addAllRequiredPullableFields(queue, member, new SubProgressMonitor(pm, 1));
		pm.done();
	}
	
	private void addAllRequiredPullableFields(List queue, IMember member, IProgressMonitor pm) throws JavaModelException {
		IField[] requiredFields= ReferenceFinderUtil.getFieldsReferencedIn(new IJavaElement[]{member}, pm);
		for (int i= 0; i < requiredFields.length; i++) {
			IField field= requiredFields[i];
			if (isRequiredPullableMember(queue, field))
				queue.add(field);
		}
	}
	
	private void addAllRequiredPullableMethods(List queue, IMember member, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", 2);
		IMethod[] requiredMethods= ReferenceFinderUtil.getMethodsReferencedIn(new IJavaElement[]{member}, new SubProgressMonitor(pm, 1));
		SubProgressMonitor sPm= new SubProgressMonitor(pm, 1);
		sPm.beginTask("", requiredMethods.length);
		for (int i= 0; i < requiredMethods.length; i++) {
			IMethod method= requiredMethods[i];
			if (isRequiredPullableMember(queue, method) && ! isVirtualAccessibleFromTargetClass(method, new SubProgressMonitor(sPm, 1)))
				queue.add(method);
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
		return member.getDeclaringType().equals(getDeclaringType()) && ! queue.contains(member) && isPullable(member);
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
	
	private Map getMatchingMembersMappingFromTypeAndAllSubtypes(ITypeHierarchy hierarchy, IType type) throws JavaModelException {
		Map result= new HashMap(); //IMember -> Set of IMembers (of the same type as key)
		result.putAll(getMatchingMembersMapping(hierarchy, type));
		IType[]  subTypes= hierarchy.getAllSubtypes(type);
		for (int i = 0; i < subTypes.length; i++) {
			mergeSets(result, getMatchingMembersMapping(hierarchy, subTypes[i]));
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
			IField field= (IField)member;	
			if (isPartOfMultiDeclaration(field, astManager)){
				Context context= JavaSourceContext.create(member);
				String pattern= "Field ''{0}'' is declared in a multi declaration. Pulling up is currently not supported.";
				String msg= MessageFormat.format(pattern, new String[]{createFieldLabel(field)});
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
			pm.beginTask("", 6); //$NON-NLS-1$
			RefactoringStatus result= new RefactoringStatus();
			result.merge(checkFinalFields(new SubProgressMonitor(pm, 1)));
			result.merge(checkAccesses(new SubProgressMonitor(pm, 1)));
			result.merge(MemberCheckUtil.checkMembersInDestinationType(fElementsToPullUp, getTargetClass()));
			pm.worked(1);
			result.merge(checkMembersInSubclasses(new SubProgressMonitor(pm, 1)));
			result.merge(checkIfSkippingOverElements(new SubProgressMonitor(pm, 1)));
			
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
	
	private RefactoringStatus checkIfSkippingOverElements(IProgressMonitor pm) throws JavaModelException {
		try{
			Set skippedSuperclassSet= getSkippedSuperclasses();
			IType[] skippedSuperclasses= (IType[]) skippedSuperclassSet.toArray(new IType[skippedSuperclassSet.size()]);
			RefactoringStatus result= new RefactoringStatus();
			for (int i= 0; i < fElementsToPullUp.length; i++) {
				IMember element= fElementsToPullUp[i];
				for (int j= 0; j < skippedSuperclasses.length; j++) {
					IType type= skippedSuperclasses[j];
					result.merge(checkIfDeclaredIn(element, type));
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
		else return null;			
	}
	
	private RefactoringStatus checkIfFieldDeclaredIn(IField iField, IType type) {
		IField fieldInType= type.getField(iField.getElementName());
		if (! fieldInType.exists())
			return null;
		String pattern= "Field ''{0}'' is declared in class ''{1}''. Pulling it up may result in changed program semantics.";
		String msg= MessageFormat.format(pattern, new Object[]{createFieldLabel(fieldInType), JavaModelUtil.getFullyQualifiedName(type)});
		Context context= JavaSourceContext.create(fieldInType);
		return RefactoringStatus.createWarningStatus(msg, context);
	}
	
	private RefactoringStatus checkIfMethodDeclaredIn(IMethod iMethod, IType type) throws JavaModelException {
		IMethod methodInType= JavaModelUtil.findMethod(iMethod.getElementName(), iMethod.getParameterTypes(), iMethod.isConstructor(), type);
		if (methodInType == null || ! methodInType.exists())
			return null;
		String pattern= "Method ''{0}'' is declared in class ''{1}''. Pulling it up may result in changed program semantics.";
		String msg= MessageFormat.format(pattern, new Object[]{createMethodLabel(methodInType), createTypeLabel(type)});
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

	private RefactoringStatus checkAllElements() throws JavaModelException {
		//just 1 error message
		for (int i = 0; i < fElementsToPullUp.length; i++) {
			RefactoringStatus status= checkElement(fElementsToPullUp[i]);
			if (status != null)
				return status;
		}
		return null;
	}
	
	//returns null if ok
	private RefactoringStatus checkElement(IMember member) throws JavaModelException{
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

		if (JdtFlags.isStatic(member))
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.no_static_elements")); //$NON-NLS-1$
			
		if (member.getElementType() == IJavaElement.METHOD){
			RefactoringStatus substatus= checkMethod((IMethod)member);
			if (substatus != null && ! substatus.isOK())
				return substatus;	
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
		
		return checkSuperclassesOfDeclaringClass(pm);	
	}
	
	private RefactoringStatus checkSuperclassesOfDeclaringClass(IProgressMonitor pm) throws JavaModelException{
		if (getPossibleTargetClasses(pm).length == 0)
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("PullUpRefactoring.not_this_type"));	 //$NON-NLS-1$
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
		return result;
	}
	
	private RefactoringStatus checkAccessedFields(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		
		List pulledUpList= Arrays.asList(fElementsToPullUp);
		List deletedList= Arrays.asList(getMembersToDelete(new NullProgressMonitor()));
		IField[] accessedFields= ReferenceFinderUtil.getFieldsReferencedIn(fElementsToPullUp, pm);
		
		IType targetClass= getTargetClass();
		for (int i= 0; i < accessedFields.length; i++) {
			IField field= accessedFields[i];
			if (! field.exists())
				continue;
			
			if (! canBeAccessedFrom(field, targetClass) && ! pulledUpList.contains(field) && !deletedList.contains(field)){
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.field_not_accessible", //$NON-NLS-1$
					new String[]{createFieldLabel(field), createTypeLabel(targetClass)});
				result.addError(message, JavaSourceContext.create(field));
			} else if (isDeclaredInSkippedSuperclass(field)){
				String pattern= "Field ''{0}'' cannot be accessed from ''{1}''";
				Object[] keys= {createFieldLabel(field), createTypeLabel(targetClass)};
				String message= MessageFormat.format(pattern, keys);
				result.addError(message, JavaSourceContext.create(field));
			}
		}
		return result;
	}

	private RefactoringStatus checkAccessedMethods(IProgressMonitor pm) throws JavaModelException{
		RefactoringStatus result= new RefactoringStatus();
		
		List pulledUpList= Arrays.asList(fElementsToPullUp);
		List deletedList= Arrays.asList(getMembersToDelete(new NullProgressMonitor()));
		IMethod[] accessedMethods= ReferenceFinderUtil.getMethodsReferencedIn(fElementsToPullUp, pm);
		
		IType targetClass= getTargetClass();
		for (int i= 0; i < accessedMethods.length; i++) {
			IMethod method= accessedMethods[i];
			if (! method.exists())
				continue;
			if (! canBeAccessedFrom(method, targetClass) && ! pulledUpList.contains(method) && ! deletedList.contains(method)){
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.method_not_accessible", //$NON-NLS-1$
					new String[]{createMethodLabel(method), createTypeLabel(targetClass)});
				result.addError(message, JavaSourceContext.create(method));
			} else if (isDeclaredInSkippedSuperclass(method)){
				String pattern= "Method ''{0}'' cannot be accessed from ''{1}''";
				Object[] keys= {createMethodLabel(method), createTypeLabel(targetClass)};
				String message= MessageFormat.format(pattern, keys);
				result.addError(message, JavaSourceContext.create(method));
			}	
		}
		return result;
	}
	
	private boolean isDeclaredInSkippedSuperclass(IMember member) throws JavaModelException {
		return getSkippedSuperclasses().contains(member.getDeclaringType());
	}
	
	//skipped superclasses are those declared in the hierarchy between the declaring type of the selected members 
	//and the target type
	private Set getSkippedSuperclasses() throws JavaModelException {
		if (fCachedSkippedSuperclasses != null)
			return fCachedSkippedSuperclasses;
		ITypeHierarchy hierarchy= getTypeHierarchyOfTargetClass(new NullProgressMonitor());//XXX	
		fCachedSkippedSuperclasses= new HashSet(2);
		IType current= hierarchy.getSuperclass(getDeclaringType());
		while(current != null && ! current.equals(getTargetClass())){
			fCachedSkippedSuperclasses.add(current);
			current= hierarchy.getSuperclass(current);
		}
		return fCachedSkippedSuperclasses;
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
		Map mapping= getMatchingMemberMatching(pm);
		for (int i= 0; i < fElementsToPullUp.length; i++) {
			if (fElementsToPullUp[i].getElementType() != IJavaElement.METHOD)
				continue;
			IMethod method= (IMethod)fElementsToPullUp[i];
			String returnType= getReturnTypeName(method);
			Assert.isTrue(mapping.containsKey(method));
			for (Iterator iter= ((Set)mapping.get(method)).iterator(); iter.hasNext();) {
				IMethod matchingMethod= (IMethod) iter.next();
				if (!notDeletedMembers.contains(matchingMethod))
					continue;
				if (returnType.equals(getReturnTypeName(matchingMethod)))
					continue;
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.different_method_return_type", //$NON-NLS-1$
					new String[]{createMethodLabel(matchingMethod),
								createTypeLabel(matchingMethod.getDeclaringType())});
				Context context= JavaSourceContext.create(matchingMethod.getCompilationUnit(), matchingMethod.getNameRange());
				result.addError(message, context);	
			}
		}
	}

	private void checkFieldTypes(IProgressMonitor pm, RefactoringStatus result) throws JavaModelException {
		Map mapping= getMatchingMemberMatching(pm);
		for (int i= 0; i < fElementsToPullUp.length; i++) {
			if (fElementsToPullUp[i].getElementType() != IJavaElement.FIELD)
				continue;
			IField field= (IField)fElementsToPullUp[i];
			String type= getTypeName(field);
			Assert.isTrue(mapping.containsKey(field));
			for (Iterator iter= ((Set)mapping.get(field)).iterator(); iter.hasNext();) {
				IField matchingField= (IField) iter.next();
				if (type.equals(getTypeName(matchingField)))
					continue;
				String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.different_field_type", //$NON-NLS-1$
					new String[]{createFieldLabel(matchingField), createTypeLabel(matchingField.getDeclaringType())});
				Context context= JavaSourceContext.create(matchingField.getCompilationUnit(), matchingField.getSourceRange());					 
				result.addError(message, context);	
			}
		}
	}

	private Map getMatchingMemberMatching(IProgressMonitor pm) throws JavaModelException {
		return getMatchingMembersMappingFromTypeAndAllSubtypes(getTypeHierarchyOfTargetClass(pm), getTargetClass());
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
					new String[]{createMethodLabel(method), createTypeLabel(method.getDeclaringType())});
				result.addError(message, errorContext);
		 } 
		 if (isVisibilityLowerThanProtected(method)){
		 	String message= RefactoringCoreMessages.getFormattedString("PullUpRefactoring.lower_visibility", //$NON-NLS-1$
		 		new String[]{createMethodLabel(method), createTypeLabel(method.getDeclaringType())});
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
			return new CompositeChange(RefactoringCoreMessages.getString("PullUpRefactoring.Pull_Up"), fChangeManager.getAllChanges()); //$NON-NLS-1$
		} finally{
			pm.done();
		}
	}

	private TextChangeManager createChangeManager(IProgressMonitor pm) throws CoreException{
		try{
			pm.beginTask(RefactoringCoreMessages.getString("PullUpRefactoring.preview"), 2); //$NON-NLS-1$
			copyMembers(new SubProgressMonitor(pm, 1));
			if (needsAddingImports())
				addImports(new SubProgressMonitor(pm, 1));
			else	
				pm.worked(1);
			deleteMembers();

			TextChangeManager manager= new TextChangeManager();
			fillWithRewriteEdits(manager);
			return manager;
		} finally{
			pm.done();
		}
	}
	
	private void copyMembers(IProgressMonitor pm) throws JavaModelException {
		pm.beginTask("", fElementsToPullUp.length);
		for (int i = fElementsToPullUp.length - 1; i >= 0; i--) { //backwards - to preserve method order
			if (fElementsToPullUp[i] instanceof IField)
				copyField((IField)fElementsToPullUp[i], new SubProgressMonitor(pm, 1));
			else if (fElementsToPullUp[i] instanceof IMethod)
				copyMethod((IMethod)fElementsToPullUp[i], new SubProgressMonitor(pm, 1));
		}
		pm.done();
	}
	
	private IType getSuperclassOfDeclaringClass(IProgressMonitor pm) throws JavaModelException {
		IType declaringType= getDeclaringType();
		return declaringType.newSupertypeHierarchy(pm).getSuperclass(declaringType);
	}

	private void copyMethod(IMethod method, IProgressMonitor pm) throws JavaModelException {
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
		copyBody(targetRewrite, method, oldMethod, newMethod, pm);
		newMethod.setConstructor(oldMethod.isConstructor());
		newMethod.setExtraDimensions(oldMethod.getExtraDimensions());
		copyJavadocNode(ast, oldMethod, newMethod);
		newMethod.setModifiers(getNewModifiers(method));
		newMethod.setName(ast.newSimpleName(oldMethod.getName().getIdentifier()));
		copyReturnType(targetRewrite, method, oldMethod, newMethod);
		copyParameters(targetRewrite, method, oldMethod, newMethod);
		copyThrownExceptions(ast, oldMethod, newMethod);
		return newMethod;
	}

	private void copyBody(ASTRewrite targetRewrite, IMethod method, MethodDeclaration oldMethod, MethodDeclaration newMethod, IProgressMonitor pm) throws JavaModelException {
		if (oldMethod.getBody() == null)
			return;
		Block oldBody= oldMethod.getBody();
		ISourceRange[] superRefOffsert= SourceRange.reverseSortByOffset(SuperReferenceFinder.findSuperReferenceRanges(method, getSuperclassOfDeclaringClass(pm)));
		
		String oldBodySource= getBufferText(oldBody);
		StringBuffer newBodyCodeBuff= new StringBuffer(oldBodySource);
		for (int i= 0; i < superRefOffsert.length; i++) {
			ISourceRange range= superRefOffsert[i];
			int start= range.getOffset() - oldBody.getStartPosition();
			int end= start + range.getLength();
			newBodyCodeBuff.replace(start, end, "this");
		}
		String newBodySource= newBodyCodeBuff.toString();
		String[] lines= Strings.convertIntoLines(newBodySource);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		newBodySource= Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(method));
		Block newBody= (Block)targetRewrite.createPlaceholder(newBodySource, ASTRewrite.BLOCK);
		newMethod.setBody(newBody);
	}
	
	private void copyThrownExceptions(AST ast, MethodDeclaration oldMethod, MethodDeclaration newMethod) {
		for (int i= 0, n= oldMethod.thrownExceptions().size(); i < n; i++) {
			Name oldException= (Name)oldMethod.thrownExceptions().get(i);
			if (oldException.isSimpleName()){
				Name newException= ast.newSimpleName(((SimpleName)oldException).getIdentifier());
				newMethod.thrownExceptions().add(i, newException);
			}	else {
				String[] identifiers= getIdentifiers((QualifiedName)oldException);
				Name newException= ast.newName(identifiers);
				newMethod.thrownExceptions().add(i, newException);
			}
		}
	}
	
	private void copyParameters(ASTRewrite targetRewrite, IMethod method, MethodDeclaration oldMethod, MethodDeclaration newMethod) throws JavaModelException {
		for (int i= 0, n= oldMethod.parameters().size(); i < n; i++) {
			SingleVariableDeclaration oldParam= (SingleVariableDeclaration)oldMethod.parameters().get(i);
			SingleVariableDeclaration newParam= createPlaceholderForSingleVariableDeclaration(oldParam, targetRewrite);
			newMethod.parameters().add(i, newParam);
		}
	}
	
	private void copyReturnType(ASTRewrite targetRewrite, IMethod method, MethodDeclaration oldMethod, MethodDeclaration newMethod) throws JavaModelException {
		Type newReturnType= createPlaceholderForType(oldMethod.getReturnType(), targetRewrite);
		newMethod.setReturnType(newReturnType);
	}

	private void copyJavadocNode(AST ast, BodyDeclaration oldDeclaration, BodyDeclaration newDeclaration) {
		if (oldDeclaration.getJavadoc() == null)
			return;
		Javadoc newJavadoc= ast.newJavadoc();
		newJavadoc.setComment(oldDeclaration.getJavadoc().getComment());
		newDeclaration.setJavadoc(newJavadoc);
	}
	
	private static String[] getIdentifiers(QualifiedName qualifiedName) {
		List result= getIdentifierList(qualifiedName);
		return (String[]) result.toArray(new String[result.size()]);
	}
	
	private static List getIdentifierList(Name name) {
		if (name.isSimpleName()){
			List l= new ArrayList(1);
			l.add(((SimpleName)name).getIdentifier());
			return l;
		}	else {
			List l= getIdentifierList(((QualifiedName)name).getQualifier());
			l.add(((QualifiedName)name).getName().getIdentifier());
			return l;
		}
	}
	
	private void copyField(IField field, IProgressMonitor pm) throws JavaModelException {
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
		newFragment.setName(ast.newSimpleName(oldFieldFragment.getName().getIdentifier()));
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
		if (needsToChangeVisibility(member))
			return clearAccessModifiers(getModifiers(member)) | Modifier.PROTECTED;
		else
			return getModifiers(member);
	}
	
	private int getModifiers(IMember member) throws JavaModelException{
		if (member instanceof IField)
			return getFieldDeclarationNode((IField)member).getModifiers();	
		else if (member instanceof IMethod)
			return getMethodDeclarationNode((IMethod)member).getModifiers();	
		Assert.isTrue(false);	
		return Modifier.NONE;
	}
	
	private static int clearAccessModifiers(int flags) {
		return clearFlag(clearFlag(clearFlag(flags, Modifier.PRIVATE), Modifier.PUBLIC), Modifier.PROTECTED);
	}
	
	private static int clearFlag(int flags, int flag){
		return flags & ~ flag;
	}

	private void addImports(IProgressMonitor pm) throws CoreException {
		IType[] referencedTypes= getTypeReferencedInMovedMembers(pm);
		ICompilationUnit cu= getTargetWorkingCopy();		
		for (int i= 0; i < referencedTypes.length; i++) {
			fImportEditManager.addImportTo(referencedTypes[i], cu);
		}
		pm.done();
	}

	private void deleteMembers() throws JavaModelException {
		IMember[] membersToDelete= getMembersToDelete(new NullProgressMonitor());
		for (int i = 0; i < membersToDelete.length; i++) {
			if (membersToDelete[i] instanceof IField)
				deleteField((IField)membersToDelete[i]);
			else if (membersToDelete[i] instanceof IMethod)
				deleteMethod((IMethod)membersToDelete[i]);
			else Assert.isTrue(false);	
		}
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
			textChange.addTextEdit("Pull up class member(s)", resultingEdits);
			rewrite.removeModifications();
		}
	}

	private ICompilationUnit getWorkingCopy(IMember member){
		return WorkingCopyUtil.getWorkingCopyIfExists(member.getCompilationUnit());
	}
	
	private boolean needsAddingImports() throws JavaModelException {
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
	
	private IType[] getTypeReferencedInMovedMembers(IProgressMonitor pm) throws JavaModelException {
		if (fTypesReferencedInMovedMembers == null)
			fTypesReferencedInMovedMembers= ReferenceFinderUtil.getTypesReferencedIn(fElementsToPullUp, pm);
		return fTypesReferencedInMovedMembers;
	}

	private static AST getAST(ASTRewrite rewrite){
		return rewrite.getRootNode().getAST();
	}
	
	private String getBufferText(ASTNode node) throws JavaModelException{
		return fAstManager.getCompilationUnit(node).getBuffer().getText(node.getStartPosition(), node.getLength());
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
}

