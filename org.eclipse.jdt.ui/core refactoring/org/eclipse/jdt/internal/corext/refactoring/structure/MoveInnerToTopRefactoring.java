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

import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
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
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.rename.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavadocUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IRefactoringStatusEntryComparator;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;
import org.eclipse.ltk.core.refactoring.TextChange;

public class MoveInnerToTopRefactoring extends Refactoring{

	private final CodeGenerationSettings fCodeGenerationSettings;
	private IType fType;
	private TextChangeManager fChangeManager;
	private String fEnclosingInstanceFieldName;
	private boolean fMarkInstanceFieldAsFinal;
	private boolean fCreateInstanceField;
	private String fNewSourceOfInputType;
	private String fNameForEnclosingInstanceConstructorParameter;
	private CompilationUnitRewrite fSourceRewrite;
	private boolean fIsInstanceFieldCreationPossible;
	private boolean fIsInstanceFieldCreationMandatory;
	
	private MoveInnerToTopRefactoring(IType type, CodeGenerationSettings codeGenerationSettings) throws JavaModelException{
		Assert.isNotNull(type);
		Assert.isNotNull(codeGenerationSettings);
		fType= type;
		fCodeGenerationSettings= codeGenerationSettings;
		fMarkInstanceFieldAsFinal= true; //default
	}

	public static MoveInnerToTopRefactoring create(IType type, CodeGenerationSettings codeGenerationSettings) throws JavaModelException{
		if (! isAvailable(type))
			return null;
		return new MoveInnerToTopRefactoring(type, codeGenerationSettings);
	}
	
	public static boolean isAvailable(IType type) throws JavaModelException{
		return Checks.isAvailable(type) && ! Checks.isTopLevel(type) && ! Checks.isInsideLocalType(type);
	}
	
	public boolean isInstanceFieldMarkedFinal(){
		return fMarkInstanceFieldAsFinal;
	}
	
	public void setMarkInstanceFieldAsFinal(boolean mark){
		fMarkInstanceFieldAsFinal= mark;
	}

	public boolean isCreatingInstanceFieldPossible(){
		return fIsInstanceFieldCreationPossible;
	}

	public boolean isCreatingInstanceFieldMandatory(){
		return fIsInstanceFieldCreationMandatory;
	}
	
	public boolean getCreateInstanceField(){
		return fCreateInstanceField;
	}
	
	public void setCreateInstanceField(boolean create){
		Assert.isTrue(fIsInstanceFieldCreationPossible);
		Assert.isTrue(! fIsInstanceFieldCreationMandatory);
		fCreateInstanceField= create;
	}
	
	private String getInitialNameForEnclosingInstanceField() {
		IType enclosingType= fType.getDeclaringType();
		if (enclosingType == null)
			return ""; //$NON-NLS-1$
		String[] suggestedNames= NamingConventions.suggestFieldNames(enclosingType.getJavaProject(), enclosingType.getPackageFragment().getElementName(), JavaModelUtil.getTypeQualifiedName(fType.getDeclaringType()), 0, getEnclosingInstanceAccessModifiers(), getFieldNames(fType));
		if (suggestedNames.length > 0)
			return suggestedNames[0];
		String name= enclosingType.getElementName();
		if (name.equals("")) //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}
	
	private static String[] getFieldNames(IType type) {
		try {
			IField[] fields = type.getFields();
			List result= new ArrayList(fields.length);
			for (int i = 0; i < fields.length; i++) {
				result.add(fields[i].getElementName());
			}
			return (String[]) result.toArray(new String[result.size()]);
		} catch (JavaModelException e) {
			return null;
		}
	}

	public IType getInputType(){
		return fType;
	}
	
	public String getEnclosingInstanceName(){
		return fEnclosingInstanceFieldName;
	}
	
	public RefactoringStatus checkEnclosingInstanceName(String name){
		if (! fCreateInstanceField)
			return new RefactoringStatus();
		RefactoringStatus result= Checks.checkFieldName(name);
		if (! Checks.startsWithLowerCase(name))
			result.addWarning(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.names_start_lowercase"));  //$NON-NLS-1$
			
		if (fType.getField(name).exists()){
			Object[] keys= new String[]{name, fType.getElementName()};
			String msg= RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.already_declared", keys); //$NON-NLS-1$
			result.addError(msg, JavaStatusContext.create(fType.getField(name)));
		}	
		return result;	
	}
	
	public void setEnclosingInstanceName(String name){
		Assert.isNotNull(name);
		fEnclosingInstanceFieldName= name;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkActivation(IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {

		fEnclosingInstanceFieldName= getInitialNameForEnclosingInstanceField();
		fSourceRewrite= new CompilationUnitRewrite(fType.getCompilationUnit());
		fIsInstanceFieldCreationPossible= !JdtFlags.isStatic(fType);
		fIsInstanceFieldCreationMandatory= fIsInstanceFieldCreationPossible && isInstanceFieldCreationMandatory();
		fCreateInstanceField= fIsInstanceFieldCreationMandatory;

		IType orig= (IType)WorkingCopyUtil.getOriginal(fType);
		if (orig == null || ! orig.exists()){
			
			String message= RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.deleted", new String[]{fType.getCompilationUnit().getElementName()}); //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(message);
		}	
		fType= orig;
		
		return Checks.checkIfCuBroken(fType);
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		pm.beginTask("", 2);//$NON-NLS-1$
		try {
			RefactoringStatus result= new RefactoringStatus();		

			if (JdtFlags.isStatic(fType))
				result.merge(checkEnclosingInstanceName(fEnclosingInstanceFieldName));
			
			if (fType.getPackageFragment().getCompilationUnit((fType.getElementName() + ".java")).exists()){ //$NON-NLS-1$
				String message= RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.compilation_Unit_exists", new String[]{(fType.getElementName() + ".java"), fType.getPackageFragment().getElementName()}); //$NON-NLS-1$ //$NON-NLS-2$
				result.addFatalError(message);
			}	
			result.merge(checkEnclosingInstanceName(fEnclosingInstanceFieldName));
			result.merge(Checks.checkCompilationUnitName((fType.getElementName() + ".java"))); //$NON-NLS-1$
			result.merge(checkConstructorParameterNames());
			result.merge(checkTypeNameInPackage());
			fChangeManager= createChangeManager(new SubProgressMonitor(pm, 1), result);
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()), getValidationContext()));
			return result;
		} finally {
			pm.done();
		}	
	}

	private RefactoringStatus checkTypeNameInPackage() throws JavaModelException {
		IType type= Checks.findTypeInPackage(fType.getPackageFragment(), fType.getElementName());
		if (type == null || ! type.exists())
			return null;
		String message= RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.type_exists", new String[]{fType.getElementName(), fType.getPackageFragment().getElementName()}); //$NON-NLS-1$
		return RefactoringStatus.createErrorStatus(message);
	}

	private RefactoringStatus checkConstructorParameterNames(){
		RefactoringStatus result= new RefactoringStatus();
		CompilationUnit cuNode= new RefactoringASTParser(AST.JLS3).parse(fType.getCompilationUnit(), false);
		TypeDeclaration type= findTypeDeclaration(fType, cuNode);
		MethodDeclaration[] nodes= getConstructorDeclarationNodes(type);
		for (int i= 0; i < nodes.length; i++) {
			MethodDeclaration constructor= nodes[i];
			for (Iterator iter= constructor.parameters().iterator(); iter.hasNext();) {
				SingleVariableDeclaration param= (SingleVariableDeclaration) iter.next();
				if (fEnclosingInstanceFieldName.equals(param.getName().getIdentifier())){
					String msg= RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.name_used", new String[]{param.getName().getIdentifier(), fType.getElementName()}); //$NON-NLS-1$
					result.addError(msg, JavaStatusContext.create(fType.getCompilationUnit(), param));
				}
			}
		}
		return result;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.name"); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(IProgressMonitor)
	 */
	public Change createChange(final IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.creating_change"), 1); //$NON-NLS-1$
		final DynamicValidationStateChange result= new DynamicValidationStateChange(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.move_to_Top")); //$NON-NLS-1$
		result.addAll(fChangeManager.getAllChanges());
		result.add(createCompilationUnitForMovedType(new SubProgressMonitor(monitor, 1)));
		return result;
	}

	private TextChangeManager createChangeManager(final IProgressMonitor monitor, final RefactoringStatus status) throws CoreException {
		monitor.beginTask(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.creating_preview"), 2); //$NON-NLS-1$
		final TextChangeManager manager= new TextChangeManager();
		final Map map= new HashMap();
		addTypeParameters(fSourceRewrite.getRoot(), fType, map);
		final ITypeBinding[] parameters= new ITypeBinding[map.values().size()];
		map.values().toArray(parameters);
		final Map typeReferences= createTypeReferencesMapping(new SubProgressMonitor(monitor, 1), status); //Map<ICompilationUnit, SearchMatch[]>
		Map constructorReferences= null; //Map<ICompilationUnit, SearchMatch[]>
		if (JdtFlags.isStatic(fType))
			constructorReferences= new HashMap(0);
		else
			constructorReferences= createConstructorReferencesMapping(new SubProgressMonitor(monitor, 1), status);
		monitor.worked(1);
		for (final Iterator iterator= getMergedSet(typeReferences.keySet(), constructorReferences.keySet()).iterator(); iterator.hasNext();) {
			final ICompilationUnit unit= (ICompilationUnit) iterator.next();
			final CompilationUnitRewrite targetRewrite= getCompilationUnitRewrite(unit);
			createCompilationUnitRewrite(parameters, targetRewrite, typeReferences, constructorReferences, fType.getCompilationUnit(), unit, false, status);
			if (unit.equals(fType.getCompilationUnit())) {
				fNewSourceOfInputType= createNewSource(targetRewrite, unit);
				targetRewrite.clearASTAndImportRewrites();
				createCompilationUnitRewrite(parameters, targetRewrite, typeReferences, constructorReferences, fType.getCompilationUnit(), unit, true, status);
			}
			manager.manage(unit, targetRewrite.createChange());
			if (monitor.isCanceled())
				throw new OperationCanceledException();
		}
		monitor.done();
		return manager;
	}

	private static void addTypeParameters(final CompilationUnit declaring, final IType type, final Map map) throws JavaModelException {
		Assert.isNotNull(declaring);
		Assert.isNotNull(type);
		Assert.isNotNull(map);
		final TypeDeclaration declaration= ASTNodeSearchUtil.getTypeDeclarationNode(type, declaring);
		ITypeBinding binding= null;
		TypeParameter parameter= null;
		for (final Iterator iterator= declaration.typeParameters().iterator(); iterator.hasNext();) {
			parameter= (TypeParameter) iterator.next();
			binding= (ITypeBinding) parameter.resolveBinding();
			if (binding != null && !map.containsKey(binding.getKey()))
				map.put(binding.getKey(), binding);
		}
		if (!Flags.isStatic(type.getFlags()) && type.getDeclaringType() != null)
			addTypeParameters(declaring, type.getDeclaringType(), map);
	}

	private String createNewSource(final CompilationUnitRewrite targetRewrite, final ICompilationUnit unit) throws CoreException, JavaModelException {
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(unit);
		TextChange change= targetRewrite.createChange();
		if (change == null)
			change= new CompilationUnitChange("", unit); //$NON-NLS-1$
		final String source= change.getPreviewContent(new NullProgressMonitor());
		final ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(source.toCharArray());
		final TypeDeclaration declaration= findTypeDeclaration(fType, (CompilationUnit) parser.createAST(null));
		return source.substring(declaration.getStartPosition(), ASTNodes.getExclusiveEnd(declaration));
	}

	private CompilationUnitRewrite getCompilationUnitRewrite(final ICompilationUnit unit) {
		Assert.isNotNull(unit);
		if (unit.equals(fType.getCompilationUnit()))
			return fSourceRewrite;
		return new CompilationUnitRewrite(unit);
	}

	private void createCompilationUnitRewrite(final ITypeBinding[] parameters, final CompilationUnitRewrite targetRewrite, final Map typeReferences, final Map constructorReferences, final ICompilationUnit sourceUnit, final ICompilationUnit targetUnit, final boolean remove, final RefactoringStatus status) throws CoreException {
		Assert.isNotNull(parameters);
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(typeReferences);
		Assert.isNotNull(constructorReferences);
		Assert.isNotNull(sourceUnit);
		Assert.isNotNull(targetUnit);
		final CompilationUnit processedNode= targetRewrite.getRoot();
		final ASTRewrite rewrite= targetRewrite.getASTRewrite();
		if (targetUnit.equals(sourceUnit)) {
			final TypeDeclaration declaration= findTypeDeclaration(fType, processedNode);
			final TextEditGroup qualifierGroup= fSourceRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.change_qualifier")); //$NON-NLS-1$
			if (!remove && !JdtFlags.isStatic(fType) && fCreateInstanceField) {
				if (JavaElementUtil.getAllConstructors(fType).length == 0)
					createConstructor(declaration, rewrite);
				else
					modifyConstructors(declaration, rewrite);
				addInheritedTypeQualifications(declaration, targetRewrite, qualifierGroup);
				addEnclosingInstanceTypeParameters(parameters, declaration, rewrite);
				addEnclosingInstanceDeclaration(declaration, rewrite);
			}
			modifyAccessToEnclosingInstance(targetRewrite, declaration, status);
			final ITypeBinding binding= declaration.resolveBinding();
			if (binding != null) {
				modifyInterfaceMemberModifiers(binding);
				modifyEnclosingClassModifiers(status, binding, fSourceRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.change_visibility"))); //$NON-NLS-1$
				final ITypeBinding declaring= binding.getDeclaringClass();
				if (declaring != null)
					declaration.accept(new TypeReferenceQualifier(binding, null));
				declaration.accept(new TypeVisibilityModifier(status, binding, fSourceRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.change_visibility")))); //$NON-NLS-1$
			}
			final TextEditGroup groupMove= targetRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.change_label")); //$NON-NLS-1$
			if (remove) {
				rewrite.remove(declaration, groupMove);
				targetRewrite.getImportRemover().registerRemovedNode(declaration);
			} else
				ModifierRewrite.create(rewrite, declaration).setModifiers(JdtFlags.clearFlag(Modifier.STATIC | Modifier.PROTECTED | Modifier.PRIVATE, declaration.getModifiers()), groupMove);
		}
		ASTNode[] references= getReferenceNodesIn(processedNode, typeReferences, targetUnit);
		for (int index= 0; index < references.length; index++)
			updateTypeReference(parameters, references[index], targetRewrite, targetUnit);
		references= getReferenceNodesIn(processedNode, constructorReferences, targetUnit);
		for (int index= 0; index < references.length; index++)
			updateConstructorReference(references[index], targetRewrite, targetUnit);
	}

	private void modifyInterfaceMemberModifiers(final ITypeBinding binding) {
		Assert.isNotNull(binding);
		ITypeBinding declaring= binding.getDeclaringClass();
		while (declaring != null && !declaring.isInterface()) {
			declaring= declaring.getDeclaringClass();
		}
		if (declaring != null) {
			final ASTNode node= ASTNodes.findDeclaration(binding, fSourceRewrite.getRoot());
			if (node instanceof TypeDeclaration) {
				final TypeDeclaration declaration= (TypeDeclaration) node;
				ModifierRewrite.create(fSourceRewrite.getASTRewrite(), declaration).setVisibility(Modifier.PUBLIC, null);
			}
		}
	}

	private void modifyAccessToEnclosingInstance(final CompilationUnitRewrite targetRewrite, final TypeDeclaration declaration, final RefactoringStatus status) {
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(declaration);
		final Set handledMethods= new HashSet();
		final Set handledFields= new HashSet();
		final MemberAccessNodeCollector collector= new MemberAccessNodeCollector(fType.getDeclaringType());
		declaration.accept(collector);
		modifyAccessToMethodsFromEnclosingInstance(targetRewrite, handledMethods, collector.getMethodInvocations(), declaration, status);
		modifyAccessToFieldsFromEnclosingInstance(targetRewrite, handledFields, collector.getFieldAccesses(), declaration, status);
		modifyAccessToFieldsFromEnclosingInstance(targetRewrite, handledFields, collector.getSimpleFieldNames(), declaration, status);
	}

	private void modifyEnclosingClassModifiers(final RefactoringStatus status, final ITypeBinding binding, final TextEditGroup group) {
		final ITypeBinding declaring= binding.getDeclaringClass();
		if (declaring != null && !declaring.isInterface() && Modifier.isStatic(binding.getModifiers()) && Modifier.isPrivate(binding.getModifiers())) {
			final ASTNode node= ASTNodes.findDeclaration(binding, fSourceRewrite.getRoot());
			if (node instanceof TypeDeclaration) {
				final TypeDeclaration declaration= (TypeDeclaration) node;
				ModifierRewrite.create(fSourceRewrite.getASTRewrite(), declaration).setModifiers(0, Modifier.PRIVATE, group);
				final RefactoringStatusEntry entry= new RefactoringStatusEntry(RefactoringStatus.WARNING, RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.change_visibility_type_warning", new String[] { Bindings.asString(binding)}), JavaStatusContext.create(fSourceRewrite.getCu(), node)); //$NON-NLS-1$
				if (!containsStatusEntry(status, entry))
					status.addEntry(entry);
			}
		}
		if (declaring != null)
			modifyEnclosingClassModifiers(status, declaring, group);
	}

	private static boolean containsStatusEntry(final RefactoringStatus status, final RefactoringStatusEntry other) {
		return status.getEntries(new IRefactoringStatusEntryComparator() {

			public final int compare(final RefactoringStatusEntry entry1, final RefactoringStatusEntry entry2) {
				return entry1.getMessage().compareTo(entry2.getMessage());
			}
		}, other).length > 0;
	}

	private void addInheritedTypeQualifications(final TypeDeclaration declaration, final CompilationUnitRewrite targetRewrite, final TextEditGroup group) {
		Assert.isNotNull(declaration);
		Assert.isNotNull(targetRewrite);
		final CompilationUnit unit= (CompilationUnit) declaration.getRoot();
		final ITypeBinding binding= declaration.resolveBinding();
		if (binding != null) {
			Type type= declaration.getSuperclassType();
			if (type != null && unit.findDeclaringNode(binding) != null)
				addTypeQualification(type, targetRewrite, group);

			for (final Iterator iterator= declaration.superInterfaceTypes().iterator(); iterator.hasNext();) {
				type= (Type) iterator.next();
				if (unit.findDeclaringNode(type.resolveBinding()) != null)
					addTypeQualification(type, targetRewrite, group);
			}
		}
	}

	private void addTypeQualification(final Type type, final CompilationUnitRewrite targetRewrite, final TextEditGroup group) {
		Assert.isNotNull(type);
		Assert.isNotNull(targetRewrite);
		final ITypeBinding binding= type.resolveBinding();
		if (binding != null) {
			final ITypeBinding declaring= binding.getDeclaringClass();
			if (declaring != null) {
				if (type instanceof SimpleType) {
					final SimpleType simpleType= (SimpleType) type;
					addSimpleTypeQualification(targetRewrite, declaring, simpleType, group);
				} else if (type instanceof ParameterizedType) {
					final ParameterizedType parameterizedType= (ParameterizedType) type;
					final Type rawType= parameterizedType.getType();
					if (rawType instanceof SimpleType)
						addSimpleTypeQualification(targetRewrite, declaring, (SimpleType) rawType, group);
				}
			}
		}
	}

	private void addSimpleTypeQualification(final CompilationUnitRewrite targetRewrite, final ITypeBinding declaring, final SimpleType simpleType, final TextEditGroup group) {
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(declaring);
		Assert.isNotNull(simpleType);
		final AST ast= targetRewrite.getRoot().getAST();
		if (!(simpleType.getName() instanceof QualifiedName)) {
			targetRewrite.getASTRewrite().replace(simpleType, ast.newQualifiedType(ASTNodeFactory.newType(ast, declaring, false), ast.newSimpleName(simpleType.getName().getFullyQualifiedName())), group);
			targetRewrite.getImportRemover().registerRemovedNode(simpleType);
		}
	}

	private boolean isInstanceFieldCreationMandatory() {
		final MemberAccessNodeCollector collector= new MemberAccessNodeCollector(fType.getDeclaringType());
		findTypeDeclaration(fType, fSourceRewrite.getRoot()).accept(collector);
		return containsNonStatic(collector.getFieldAccesses()) || containsNonStatic(collector.getMethodInvocations()) || containsNonStatic(collector.getSimpleFieldNames());
	}

	private static boolean containsNonStatic(SimpleName[] fieldNames) {
		for (int i= 0; i < fieldNames.length; i++) {
			if (! isStaticFieldName(fieldNames[i]))
				return true;
		}
		return false;
	}

	private static boolean containsNonStatic(MethodInvocation[] invocations) {
		for (int i= 0; i < invocations.length; i++) {
			if (! isStatic(invocations[i]))
				return true;
		}
		return false;
	}

	private static boolean containsNonStatic(FieldAccess[] accesses) {
		for (int i= 0; i < accesses.length; i++) {
			if (! isStatic(accesses[i]))
				return true;
		}
		return false;
	}

	private static boolean isStatic(MethodInvocation invocation) {
		IMethodBinding methodBinding= invocation.resolveMethodBinding();
		if (methodBinding == null)
			return false;
		return JdtFlags.isStatic(methodBinding);
	}

	private static boolean isStatic(FieldAccess access) {
		IVariableBinding fieldBinding= access.resolveFieldBinding();
		if (fieldBinding == null)
			return false;
		return JdtFlags.isStatic(fieldBinding);
	}

	private static boolean isStaticFieldName(SimpleName name) {
		IBinding binding= name.resolveBinding();
		if (! (binding instanceof IVariableBinding))
			return false;
		IVariableBinding variableBinding= (IVariableBinding)binding;
		if (! variableBinding.isField())
			return false;
		return JdtFlags.isStatic(variableBinding);			
	}

	private static TypeDeclaration findTypeDeclaration(IType type, CompilationUnit cuNode) {
		List types= getDeclaringTypes(type);
		types.add(type);
		TypeDeclaration[] declarations= (TypeDeclaration[]) cuNode.types().toArray(new TypeDeclaration[cuNode.types().size()]);
		TypeDeclaration td= null;
		for (Iterator iter= types.iterator(); iter.hasNext();) {
			IType enclosing= (IType) iter.next();
			td= findTypeDeclaration(enclosing, declarations);
			Assert.isNotNull(td);
			declarations= td.getTypes();
		}
		Assert.isNotNull(td);
		return td;
	}

	private static TypeDeclaration findTypeDeclaration(IType enclosing, TypeDeclaration[] declarations) {
		String typeName= enclosing.getElementName();
		for (int i= 0; i < declarations.length; i++) {
			TypeDeclaration declaration= declarations[i];
			if (declaration.getName().getIdentifier().equals(typeName))
				return declaration;
		}
		return null;
	}

	//List of ITypes
	private static List getDeclaringTypes(IType type) {
		IType declaringType= type.getDeclaringType();
		if (declaringType == null)
			return new ArrayList(0);
		List result= getDeclaringTypes(declaringType);
		result.add(declaringType);
		return result;
	}
	
	private static Set getMergedSet(Set s1, Set s2){
		Set result= new HashSet();
		result.addAll(s1);
		result.addAll(s2);
		return result;
	}
	
	//Map<ICompilationUnit, SearchMatch[]>
	private Map createTypeReferencesMapping(IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		SearchResultGroup[] groups= RefactoringSearchEngine.search(SearchPattern.createPattern(fType, IJavaSearchConstants.ALL_OCCURRENCES), RefactoringScopeFactory.create(fType), pm, status);
		Map result= new HashMap();
		for (int i= 0; i < groups.length; i++) {
			SearchResultGroup group= groups[i];
			ICompilationUnit cu= group.getCompilationUnit();
			if (cu == null)
				continue;
			result.put(cu, group.getSearchResults());
		}
		return result;
	}

	//Map<ICompilationUnit, SearchMatch[]>
	private Map createConstructorReferencesMapping(IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		SearchResultGroup[] groups= ConstructorReferenceFinder.getConstructorReferences(fType, pm, status);
		Map result= new HashMap();
		for (int i= 0; i < groups.length; i++) {
			SearchResultGroup group= groups[i];
			ICompilationUnit cu= group.getCompilationUnit();
			if (cu == null)
				continue;
			result.put(cu, group.getSearchResults());
		}
		return result;
	}

	private static ASTNode[] getReferenceNodesIn(CompilationUnit cuNode, Map references, ICompilationUnit cu){
		SearchMatch[] results= (SearchMatch[])references.get(cu);
		if (results == null)
			return new ASTNode[0];
		return ASTNodeSearchUtil.getAstNodes(results, cuNode);
	}

	private void modifyAccessToFieldsFromEnclosingInstance(CompilationUnitRewrite targetRewrite, Set handledFields, SimpleName[] simpleNames, TypeDeclaration inputType, RefactoringStatus status) {
		IBinding binding= null;
		SimpleName simpleName= null;
		IVariableBinding variable= null;
		for (int index= 0; index < simpleNames.length; index++) {
			simpleName= simpleNames[index];
			binding= simpleName.resolveBinding();
			if (binding != null && binding instanceof IVariableBinding) {
				variable= (IVariableBinding) binding;
				modifyFieldVisibility(targetRewrite, handledFields, variable, status);
				final FieldAccess access= simpleName.getAST().newFieldAccess();
				access.setExpression(createAccessExpressionToEnclosingInstanceFieldText(simpleName, variable, inputType));
				access.setName(simpleName.getAST().newSimpleName(simpleName.getIdentifier()));
				targetRewrite.getASTRewrite().replace(simpleName, access, null);
				targetRewrite.getImportRemover().registerRemovedNode(simpleName);
			}
		}
	}

	private void modifyFieldVisibility(CompilationUnitRewrite targetRewrite, Set handledFields, IVariableBinding variable, RefactoringStatus status) {
		if (!handledFields.contains(variable.getKey()) && variable.isField() && Modifier.isPrivate(variable.getModifiers())) {
			final ASTNode node= fSourceRewrite.getRoot().findDeclaringNode(variable);
			if (node instanceof VariableDeclarationFragment) {
				final VariableDeclarationFragment fragment= (VariableDeclarationFragment) node;
				final FieldDeclaration declaration= (FieldDeclaration) fragment.getParent();
				final ASTRewrite rewrite= targetRewrite.getASTRewrite();
				if (declaration.fragments().size() <= 1)
					ModifierRewrite.create(rewrite, declaration).setModifiers(0, Modifier.PRIVATE, targetRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.change_visibility"))); //$NON-NLS-1$
				else {
					final AST ast= declaration.getAST();
					final Type newType= (Type) ASTNode.copySubtree(ast, declaration.getType());
					final VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
					newFragment.setName(ast.newSimpleName(variable.getName()));
					final FieldDeclaration newDeclaration= ast.newFieldDeclaration(newFragment);
					newDeclaration.setType(newType);
					rewrite.getListRewrite(declaration.getParent(), TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertAfter(newDeclaration, declaration, null);
					rewrite.getListRewrite(declaration, FieldDeclaration.FRAGMENTS_PROPERTY).remove(fragment, targetRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.change_visibility"))); //$NON-NLS-1$
				}
				final RefactoringStatusEntry entry= new RefactoringStatusEntry(RefactoringStatus.WARNING, RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.change_visibility_field_warning", new String[] {Bindings.asString(variable)}), JavaStatusContext.create(fSourceRewrite.getCu(), node)); //$NON-NLS-1$
				if (!containsStatusEntry(status, entry))
					status.addEntry(entry);
			}
		}
	}

	private void modifyAccessToFieldsFromEnclosingInstance(CompilationUnitRewrite targetRewrite, Set handledFields, FieldAccess[] fieldAccesses, TypeDeclaration inputType, RefactoringStatus status) {
		FieldAccess access= null;
		for (int index= 0; index < fieldAccesses.length; index++) {
			access= fieldAccesses[index];
			Assert.isNotNull(access.getExpression());
			if (!(access.getExpression() instanceof ThisExpression) || (!(((ThisExpression) access.getExpression()).getQualifier() != null)))
				continue;

			final IVariableBinding binding= access.resolveFieldBinding();
			if (binding != null) {
				modifyFieldVisibility(targetRewrite, handledFields, binding, status);
				targetRewrite.getASTRewrite().replace(access.getExpression(), createAccessExpressionToEnclosingInstanceFieldText(access, binding, inputType), null);
				targetRewrite.getImportRemover().registerRemovedNode(access.getExpression());
			}
		}
	}

	private void modifyAccessToMethodsFromEnclosingInstance(CompilationUnitRewrite targetRewrite, Set handledMethods, MethodInvocation[] methodInvocations, TypeDeclaration inputType, RefactoringStatus status) {
		IMethodBinding binding= null;
		MethodInvocation invocation= null;
		for (int index= 0; index < methodInvocations.length; index++) {
			invocation= methodInvocations[index];
			binding= invocation.resolveMethodBinding();
			if (binding != null) {
				final ASTNode node= fSourceRewrite.getRoot().findDeclaringNode(binding);
				modifyMethodVisibility(handledMethods, node, status);
				final Expression target= invocation.getExpression();
				if (target == null) {
					final Expression expression= createAccessExpressionToEnclosingInstanceFieldText(invocation, binding, inputType);
					targetRewrite.getASTRewrite().set(invocation, MethodInvocation.EXPRESSION_PROPERTY, expression, null);
				} else {
					if (!(invocation.getExpression() instanceof ThisExpression) || !(((ThisExpression) invocation.getExpression()).getQualifier() != null))
						continue;
					targetRewrite.getASTRewrite().replace(target, createAccessExpressionToEnclosingInstanceFieldText(invocation, binding, inputType), null);
					targetRewrite.getImportRemover().registerRemovedNode(target);
				}
			}
		}
	}

	private void modifyMethodVisibility(final Set handledMethods, final ASTNode node, final RefactoringStatus status) {
		if (!handledMethods.contains(node) && node instanceof MethodDeclaration) {
			handledMethods.add(node);
			final MethodDeclaration declaration= (MethodDeclaration) node;
			if (Modifier.isPrivate(declaration.getModifiers())) {
				ModifierRewrite.create(fSourceRewrite.getASTRewrite(), declaration).setModifiers(0, Modifier.PRIVATE, fSourceRewrite.createGroupDescription(RefactoringCoreMessages.getString("MoveInnerToTopRefactoring.change_visibility"))); //$NON-NLS-1$
				final IMethodBinding binding= declaration.resolveBinding();
				if (binding != null) {
					final RefactoringStatusEntry entry= new RefactoringStatusEntry(RefactoringStatus.WARNING, RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.change_visibility_method_warning", new String[] { Bindings.asString(binding)}), JavaStatusContext.create(fSourceRewrite.getCu(), declaration)); //$NON-NLS-1$
					if (!containsStatusEntry(status, entry))
						status.addEntry(entry);
				}
			}
		}
	}

	private Expression createAccessExpressionToEnclosingInstanceFieldText(ASTNode node, IBinding binding, TypeDeclaration inputType) {
		if (Modifier.isStatic(binding.getModifiers()))
			return node.getAST().newName(Strings.splitByToken(JavaModelUtil.getTypeQualifiedName(fType.getDeclaringType()), ".")); //$NON-NLS-1$
		else if ((isInAnonymousTypeInsideInputType(node, inputType) || isInLocalTypeInsideInputType(node, inputType) || isInNonStaticMemberTypeInsideInputType(node, inputType)))
			return createQualifiedReadAccessExpressionForEnclosingInstance(node.getAST());
		else
			return createReadAccessExpressionForEnclosingInstance(node.getAST());
	}

	private boolean isInLocalTypeInsideInputType(ASTNode node, TypeDeclaration inputType) {
		TypeDeclarationStatement localType= (TypeDeclarationStatement) ASTNodes.getParent(node, TypeDeclarationStatement.class);
		return localType != null && ASTNodes.isParent(localType, inputType);
	}

	private boolean isInNonStaticMemberTypeInsideInputType(ASTNode node, TypeDeclaration inputType) {
		TypeDeclaration nested= (TypeDeclaration) ASTNodes.getParent(node, TypeDeclaration.class);
		return nested != null && !inputType.equals(nested) && !Modifier.isStatic(nested.getFlags()) && ASTNodes.isParent(nested, inputType);
	}

	private boolean isInAnonymousTypeInsideInputType(ASTNode node, TypeDeclaration inputType) {
		AnonymousClassDeclaration anon= (AnonymousClassDeclaration) ASTNodes.getParent(node, AnonymousClassDeclaration.class);
		return anon != null && ASTNodes.isParent(anon, inputType);
	}

	private void modifyConstructors(TypeDeclaration td, ASTRewrite rewrite) throws CoreException {
		MethodDeclaration[] constructorNodes= getConstructorDeclarationNodes(td);
		for (int i= 0; i < constructorNodes.length; i++) {
			MethodDeclaration decl= constructorNodes[i];
			Assert.isTrue(decl.isConstructor());
			addParameterToConstructor(rewrite, decl);
			setEnclosingInstanceFieldInConstructor(rewrite, decl);
		}
	}

	private void setEnclosingInstanceFieldInConstructor(ASTRewrite rewrite, MethodDeclaration decl) throws JavaModelException {
		final AST ast= decl.getAST();
		final Block body= decl.getBody();
		final List statements= body.statements();
		if (statements.isEmpty()) {
			final Assignment assignment= ast.newAssignment();
			assignment.setLeftHandSide(createReadAccessExpressionForEnclosingInstance(ast));
			assignment.setRightHandSide(ast.newSimpleName(getNameForEnclosingInstanceConstructorParameter()));
			rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY).insertFirst(ast.newExpressionStatement(assignment), null);
		} else {
			final Statement first= (Statement) statements.get(0);
			if (first instanceof ConstructorInvocation) {
				rewrite.getListRewrite(first, ConstructorInvocation.ARGUMENTS_PROPERTY).insertFirst(ast.newSimpleName(fEnclosingInstanceFieldName), null);
			} else {
				int index= 0;
				if (first instanceof SuperConstructorInvocation)
					index++;
				final Assignment assignment= ast.newAssignment();
				assignment.setLeftHandSide(createReadAccessExpressionForEnclosingInstance(ast));
				assignment.setRightHandSide(ast.newSimpleName(getNameForEnclosingInstanceConstructorParameter()));
				rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY).insertAt(ast.newExpressionStatement(assignment), index, null);
			}
		}
	}

	private void addParameterToConstructor(final ASTRewrite rewrite, final MethodDeclaration declaration) throws JavaModelException {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(declaration);
		final AST ast= declaration.getAST();
		final String name= getNameForEnclosingInstanceConstructorParameter();
		final SingleVariableDeclaration variable= ast.newSingleVariableDeclaration();
		variable.setType(createEnclosingType(ast));
		variable.setName(ast.newSimpleName(name));
		rewrite.getListRewrite(declaration, MethodDeclaration.PARAMETERS_PROPERTY).insertFirst(variable, null);
		JavadocUtil.addParamJavadoc(name, declaration, rewrite, fType.getJavaProject(), null);
	}

	private Type createEnclosingType(final AST ast) throws JavaModelException {
		Assert.isNotNull(ast);
		final ITypeParameter[] parameters= fType.getDeclaringType().getTypeParameters();
		final Type type= ASTNodeFactory.newType(ast, JavaModelUtil.getTypeQualifiedName(fType.getDeclaringType()));
		if (parameters.length > 0) {
			final ParameterizedType parameterized= ast.newParameterizedType(type);
			for (int index= 0; index < parameters.length; index++)
				parameterized.typeArguments().add(ast.newSimpleType(ast.newSimpleName(parameters[index].getElementName())));
			return parameterized;
		}
		return type;
	}

	private void createConstructor(final TypeDeclaration declaration, final ASTRewrite rewrite) throws CoreException {
		Assert.isNotNull(declaration);
		Assert.isNotNull(rewrite);
		final AST ast= declaration.getAST();
		final MethodDeclaration constructor= ast.newMethodDeclaration();
		constructor.setConstructor(true);
		constructor.setName(ast.newSimpleName(declaration.getName().getIdentifier()));
		final String comment= CodeGeneration.getMethodComment(fType.getCompilationUnit(), fType.getElementName(), fType.getElementName(), getNewConstructorParameterNames(), new String[0], null, null, getLineSeperator());
		if (comment != null && comment.length() > 0) {
			final Javadoc doc= (Javadoc) rewrite.createStringPlaceholder(comment, ASTNode.JAVADOC);
			constructor.setJavadoc(doc);
		}
		if (fCreateInstanceField) {
			final SingleVariableDeclaration variable= ast.newSingleVariableDeclaration();
			final String name= getNameForEnclosingInstanceConstructorParameter();
			variable.setName(ast.newSimpleName(name));
			variable.setType(createEnclosingType(ast));
			constructor.parameters().add(variable);
			final Block body= ast.newBlock();
			final Assignment assignment= ast.newAssignment();
			if (fCodeGenerationSettings.useKeywordThis || fEnclosingInstanceFieldName.equals(fNameForEnclosingInstanceConstructorParameter)) {
				final FieldAccess access= ast.newFieldAccess();
				access.setExpression(ast.newThisExpression());
				access.setName(ast.newSimpleName(fEnclosingInstanceFieldName));
				assignment.setLeftHandSide(access);
			} else
				assignment.setLeftHandSide(ast.newSimpleName(fEnclosingInstanceFieldName));
			assignment.setRightHandSide(ast.newSimpleName(name));
			final Statement statement= ast.newExpressionStatement(assignment);
			body.statements().add(statement);
			constructor.setBody(body);
		} else
			constructor.setBody(ast.newBlock());
		rewrite.getListRewrite(declaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertFirst(constructor, null);
	}

	private String[] getNewConstructorParameterNames() throws JavaModelException {
		if (!fCreateInstanceField)
			return new String[0];
		return new String[] { getNameForEnclosingInstanceConstructorParameter()};
	}

	private void addEnclosingInstanceDeclaration(final TypeDeclaration declaration, final ASTRewrite rewrite) throws CoreException {
		Assert.isNotNull(declaration);
		Assert.isNotNull(rewrite);
		final AST ast= declaration.getAST();
		final VariableDeclarationFragment fragment= ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName(fEnclosingInstanceFieldName));		
		final FieldDeclaration newField= ast.newFieldDeclaration(fragment);
		newField.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getEnclosingInstanceAccessModifiers()));
		newField.setType(createEnclosingType(ast));
		final String comment= CodeGeneration.getFieldComment(fType.getCompilationUnit(), declaration.getName().getIdentifier(), fEnclosingInstanceFieldName, getLineSeperator());
		if (comment != null && comment.length() > 0) {
			final Javadoc doc= (Javadoc) rewrite.createStringPlaceholder(comment, ASTNode.JAVADOC);
			newField.setJavadoc(doc);
		}
		rewrite.getListRewrite(declaration, TypeDeclaration.BODY_DECLARATIONS_PROPERTY).insertFirst(newField, null);
	}

	private void addEnclosingInstanceTypeParameters(final ITypeBinding[] parameters, final TypeDeclaration declaration, final ASTRewrite rewrite) {
		Assert.isNotNull(parameters);
		Assert.isNotNull(declaration);
		Assert.isNotNull(rewrite);
		final List existing= declaration.typeParameters();
		final Set names= new HashSet();
		TypeParameter parameter= null;
		for (final Iterator iterator= existing.iterator(); iterator.hasNext();) {
			parameter= (TypeParameter) iterator.next();
			names.add(parameter.getName().getIdentifier());
		}
		final ListRewrite rewriter= rewrite.getListRewrite(declaration, TypeDeclaration.TYPE_PARAMETERS_PROPERTY);
		String name= null;
		for (int index= 0; index < parameters.length; index++) {
			name= parameters[index].getName();
			if (!names.contains(name)) {
				parameter= declaration.getAST().newTypeParameter();
				parameter.setName(declaration.getAST().newSimpleName(name));
				rewriter.insertLast(parameter, null);
			}
		}
	}

	private int getEnclosingInstanceAccessModifiers(){
		if (fMarkInstanceFieldAsFinal)
			return Modifier.PRIVATE | Modifier.FINAL;
		else 
			return Modifier.PRIVATE;
	}

	private void updateTypeReference(ITypeBinding[] parameters, ASTNode node, CompilationUnitRewrite rewrite, ICompilationUnit cu) throws CoreException {
		ImportDeclaration enclosingImport= (ImportDeclaration) ASTNodes.getParent(node, ImportDeclaration.class);
		if (enclosingImport != null) 
			updateReferenceInImport(enclosingImport, node, rewrite);
		 else {
			boolean updated= updateReference(parameters, node, rewrite);
			if (updated && !fType.getPackageFragment().equals(cu.getParent())) {
				final String name= fType.getPackageFragment().getElementName() + '.' + fType.getElementName();
				rewrite.getImportRemover().registerAddedImport(name);
				rewrite.getImportRewrite().addImport(name);
			}
		}
	}

	private void updateReferenceInImport(ImportDeclaration enclosingImport, ASTNode node, CompilationUnitRewrite rewrite) throws CoreException {
		final IBinding binding= enclosingImport.resolveBinding();
		if (binding instanceof ITypeBinding) {
			final ITypeBinding type= (ITypeBinding) binding;
			rewrite.getImportRewrite().removeImport(type);
		}
	}

	private boolean updateReference(ITypeBinding[] parameters, ASTNode node, CompilationUnitRewrite rewrite) {
		if (node.getNodeType() == ASTNode.QUALIFIED_NAME)
			return updateNameReference(parameters, (QualifiedName) node, rewrite);
		else if (node.getNodeType() == ASTNode.SIMPLE_TYPE)
			return updateNameReference(parameters, ((SimpleType) node).getName(), rewrite);
		else
			return false;
	}

	private boolean updateNameReference(ITypeBinding[] parameters, Name name, CompilationUnitRewrite targetRewrite) {
		if (name instanceof SimpleName)	
			return false;
		if (ASTNodes.asString(name).equals(fType.getFullyQualifiedName('.'))){
			targetRewrite.getASTRewrite().replace(name, name.getAST().newName(Strings.splitByToken((fType.getPackageFragment().getElementName() + '.' + fType.getElementName()), ".")), null); //$NON-NLS-1$
			targetRewrite.getImportRemover().registerRemovedNode(name);
			return true;
		}
		targetRewrite.getASTRewrite().replace(name, name.getAST().newSimpleName(fType.getElementName()), null);
		targetRewrite.getImportRemover().registerRemovedNode(name);
		return true;
	}

	private Change createCompilationUnitForMovedType(IProgressMonitor pm) throws CoreException {
		ICompilationUnit newCuWC= null;
		try {
			newCuWC= WorkingCopyUtil.getNewWorkingCopy(fType.getPackageFragment(), (fType.getElementName() + ".java")); //$NON-NLS-1$
			String source= createSourceForNewCu(newCuWC, pm);
			return new CreateTextFileChange(ResourceUtil.getFile(fType.getCompilationUnit()).getFullPath().removeLastSegments(1).append((fType.getElementName() + ".java")), source, null, "java"); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (newCuWC != null)
				newCuWC.discardWorkingCopy();
		}
	}

	private String createSourceForNewCu(final ICompilationUnit unit, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(unit);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 2); //$NON-NLS-1$
			final String separator= getLineSeperator();
			final String block= getAlignedSourceBlock(fNewSourceOfInputType);
			String content= CodeGeneration.getCompilationUnitContent(unit, null, block, separator);
			if (content == null || block.startsWith("/*") || block.startsWith("//")) {  //$NON-NLS-1$//$NON-NLS-2$
				final StringBuffer buffer= new StringBuffer();
				if (!fType.getPackageFragment().isDefaultPackage()) {
					buffer.append("package ").append(fType.getPackageFragment().getElementName()).append(';'); //$NON-NLS-1$
				}
				buffer.append(separator).append(separator);
				buffer.append(block);
				content= buffer.toString();
			}
			unit.getBuffer().setContents(content);
			addImportsToTargetUnit(unit, new SubProgressMonitor(monitor, 1));
		} finally {
			monitor.done();
		}
		return unit.getSource();
	}

	private void addImportsToTargetUnit(final ICompilationUnit targetUnit, final IProgressMonitor monitor) throws CoreException, JavaModelException {
		final ImportsStructure structure= new ImportsStructure(targetUnit, fCodeGenerationSettings.importOrder, fCodeGenerationSettings.importThreshold, true);
		final IType[] references= ReferenceFinderUtil.getTypesReferencedIn(new IJavaElement[] { fType}, monitor);
		IType type= null;
		for (int index= 0; index < references.length; index++) {
			type= references[index];
			if (isParent(fType, type))
				continue;
			else if (type.isBinary() || !type.getCompilationUnit().equals(fSourceRewrite.getCu())) {
				IType declaring= type.getDeclaringType();
				while (declaring != null) {
					type= declaring;
					declaring= declaring.getDeclaringType();
				}
			}
			structure.addImport(JavaModelUtil.getFullyQualifiedName(type));
		}
		structure.create(false, monitor);
	}

	private boolean isParent(IType parent, IType child) {
		Assert.isNotNull(parent);
		Assert.isNotNull(child);
		if (parent.equals(child))
			return true;
		final IType declaring= child.getDeclaringType();
		if (declaring != null)
			return isParent(parent, declaring);
		return false;
	}

	private String getAlignedSourceBlock(final String block) {
		Assert.isNotNull(block);
		final String[] lines= Strings.convertIntoLines(block);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		return Strings.concatenate(lines, getLineSeperator());
	}

	private String getLineSeperator() {
		try {
			return StubUtility.getLineDelimiterUsed(fType);
		} catch (JavaModelException e) {
			return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void updateConstructorReference(ASTNode reference, CompilationUnitRewrite targetRewrite, ICompilationUnit cu) throws CoreException {
		if (reference instanceof SuperConstructorInvocation)
			updateConstructorReference((SuperConstructorInvocation) reference, targetRewrite, cu);
		else if (reference instanceof ClassInstanceCreation)
			updateConstructorReference((ClassInstanceCreation) reference, targetRewrite, cu);
		else if (reference.getParent() instanceof ClassInstanceCreation)
			updateConstructorReference((ClassInstanceCreation) reference.getParent(), targetRewrite, cu);
	}

	private void updateConstructorReference(final SuperConstructorInvocation invocation, final CompilationUnitRewrite targetRewrite, final ICompilationUnit unit) throws CoreException {
		Assert.isNotNull(invocation);
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(unit);
		final ASTRewrite rewrite= targetRewrite.getASTRewrite();
		if (fCreateInstanceField)
			insertExpressionAsParameter(invocation, rewrite, unit);
		final Expression expression= invocation.getExpression();
		if (expression != null) {
			rewrite.remove(expression, null);
			targetRewrite.getImportRemover().registerRemovedNode(expression);
		}
	}

	private void updateConstructorReference(final ClassInstanceCreation creation, final CompilationUnitRewrite targetRewrite, final ICompilationUnit unit) throws JavaModelException {
		Assert.isNotNull(creation);
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(unit);
		final ASTRewrite rewrite= targetRewrite.getASTRewrite();
		if (fCreateInstanceField)
			insertExpressionAsParameter(creation, rewrite, unit);
		final Expression expression= creation.getExpression();
		if (expression != null) {
			rewrite.remove(expression, null);
			targetRewrite.getImportRemover().registerRemovedNode(expression);
		}
	}

	private MethodDeclaration[] getConstructorDeclarationNodes(TypeDeclaration declaration){
		MethodDeclaration[] methodDeclarations= declaration.getMethods();
		List result= new ArrayList(2);
		for (int i= 0; i < methodDeclarations.length; i++) {
			MethodDeclaration method= methodDeclarations[i];
			if (method.isConstructor())
				result.add(method);
		}
		return (MethodDeclaration[]) result.toArray(new MethodDeclaration[result.size()]);
	}

	private boolean insertExpressionAsParameter(ClassInstanceCreation cic, ASTRewrite rewrite, ICompilationUnit cu) throws JavaModelException{
		final Expression expression= createEnclosingInstanceCreationString(cic, cu);
		if (expression == null)
			return false;
		rewrite.getListRewrite(cic, ClassInstanceCreation.ARGUMENTS_PROPERTY).insertFirst(expression, null);
		return true;
	}

	private boolean insertExpressionAsParameter(SuperConstructorInvocation sci, ASTRewrite rewrite, ICompilationUnit cu) throws JavaModelException{
		final Expression expression= createEnclosingInstanceCreationString(sci, cu);
		if (expression == null)
			return false;
		rewrite.getListRewrite(sci, SuperConstructorInvocation.ARGUMENTS_PROPERTY).insertFirst(expression, null);
		return true;
	}

	private Expression createEnclosingInstanceCreationString(final ASTNode node, final ICompilationUnit cu) throws JavaModelException {
		Assert.isTrue((node instanceof ClassInstanceCreation) || (node instanceof SuperConstructorInvocation));
		Assert.isNotNull(cu);
		Expression expression= null;
		if (node instanceof ClassInstanceCreation)
			expression= ((ClassInstanceCreation) node).getExpression();
		else
			expression= ((SuperConstructorInvocation) node).getExpression();
		final AST ast= node.getAST();
		if (expression != null)
			return expression;
		else if (JdtFlags.isStatic(fType))
			return null;
		else if (isInsideSubclassOfDeclaringType(node))
			return ast.newThisExpression();
		else if (isInsideInputType(node)) {
			if (fCodeGenerationSettings.useKeywordThis || fEnclosingInstanceFieldName.equals(fNameForEnclosingInstanceConstructorParameter)) {
				final FieldAccess access= ast.newFieldAccess();
				access.setExpression(ast.newThisExpression());
				access.setName(ast.newSimpleName(fEnclosingInstanceFieldName));
				return access;
			} else
				return ast.newSimpleName(fEnclosingInstanceFieldName);
		} else if (isInsideTypeNestedInDeclaringType(node)) {
			final ThisExpression qualified= ast.newThisExpression();
			qualified.setQualifier(ast.newSimpleName(fType.getDeclaringType().getElementName()));
			return qualified;
		}
		return null;
	}

	private boolean isInsideSubclassOfDeclaringType(ASTNode node) {
		Assert.isTrue((node instanceof ClassInstanceCreation) || (node instanceof SuperConstructorInvocation));
		TypeDeclaration type= (TypeDeclaration)ASTNodes.getParent(node, TypeDeclaration.class);
		Assert.isNotNull(type);

		AnonymousClassDeclaration declaration= (AnonymousClassDeclaration) ASTNodes.getParent(node, AnonymousClassDeclaration.class);
		boolean isAnonymous= declaration != null && ASTNodes.isParent(declaration, type);
		if (isAnonymous)
			return isSubclassBindingOfEnclosingType(declaration.resolveBinding());
		return isSubclassBindingOfEnclosingType(type.resolveBinding());
	}

	private boolean isSubclassBindingOfEnclosingType(ITypeBinding binding) {
		while (binding != null) {
			if (isCorrespondingTypeBinding(binding, fType.getDeclaringType()))
				return true;
			binding= binding.getSuperclass();
		}
		return false;
	}

	private boolean isInsideTypeNestedInDeclaringType(ASTNode node) {
		Assert.isTrue((node instanceof ClassInstanceCreation) || (node instanceof SuperConstructorInvocation));
		TypeDeclaration typeDeclar= (TypeDeclaration)ASTNodes.getParent(node, TypeDeclaration.class);
		Assert.isNotNull(typeDeclar);
		ITypeBinding enclosing= typeDeclar.resolveBinding();
		while(enclosing != null){
			if (isCorrespondingTypeBinding(enclosing, fType.getDeclaringType()))
				return true;
			enclosing= enclosing.getDeclaringClass();	
		}		
		return false;
	}

	private boolean isInsideInputType(ASTNode node) throws JavaModelException {
		Assert.isTrue((node instanceof ClassInstanceCreation) || (node instanceof SuperConstructorInvocation));
		ISourceRange range= fType.getSourceRange();
		return (node.getStartPosition() >= range.getOffset() && ASTNodes.getExclusiveEnd(node) <= range.getOffset() + range.getLength());
	}

	private static boolean isCorrespondingTypeBinding(ITypeBinding binding, IType type) {
		if (binding == null)
			return false;
		return Bindings.getFullyQualifiedName(binding).equals(JavaElementUtil.createSignature(type));
	}	

	private Expression createQualifiedReadAccessExpressionForEnclosingInstance(AST ast) {
		ThisExpression thisE= ast.newThisExpression();
		thisE.setQualifier(ast.newName(new String[]{fType.getElementName()}));
		FieldAccess fa= ast.newFieldAccess();
		fa.setExpression(thisE);
		fa.setName(ast.newSimpleName(fEnclosingInstanceFieldName));
		return fa;
	}

	private Expression createReadAccessExpressionForEnclosingInstance(AST ast) {
		if (fCodeGenerationSettings.useKeywordThis || fEnclosingInstanceFieldName.equals(fNameForEnclosingInstanceConstructorParameter)) {
			FieldAccess fa= ast.newFieldAccess();
			fa.setExpression(ast.newThisExpression());
			fa.setName(ast.newSimpleName(fEnclosingInstanceFieldName));
			return fa;
		}
		return ast.newSimpleName(fEnclosingInstanceFieldName);
	}

	private String getNameForEnclosingInstanceConstructorParameter() throws JavaModelException {
		if (fNameForEnclosingInstanceConstructorParameter != null)
			return fNameForEnclosingInstanceConstructorParameter;
		
		IType enclosingType= fType.getDeclaringType();
		String[] suggestedNames= NamingConventions.suggestArgumentNames(enclosingType.getJavaProject(), enclosingType.getPackageFragment().getElementName(), JavaModelUtil.getTypeQualifiedName(fType.getDeclaringType()), 0, getParameterNamesOfAllConstructors(fType));
		if (suggestedNames.length > 0)
			fNameForEnclosingInstanceConstructorParameter= suggestedNames[0];
		else
			fNameForEnclosingInstanceConstructorParameter= fEnclosingInstanceFieldName;
		return fNameForEnclosingInstanceConstructorParameter;
	}
	
	private static String[] getParameterNamesOfAllConstructors(IType type) throws JavaModelException {
		IMethod[] constructors= JavaElementUtil.getAllConstructors(type);
		Set result= new HashSet();
		for (int i= 0; i < constructors.length; i++) {
			result.addAll(Arrays.asList(constructors[i].getParameterNames()));
		}
		return (String[])result.toArray(new String[result.size()]);
	}

	private static ITypeBinding getDeclaringTypeBinding(MethodInvocation methodInvocation) {
		IMethodBinding binding= methodInvocation.resolveMethodBinding();
		if (binding == null)
			return null;
		return binding.getDeclaringClass();
	}

	private static ITypeBinding getDeclaringTypeBinding(FieldAccess fieldAccess){
		IVariableBinding varBinding= fieldAccess.resolveFieldBinding();
		if (varBinding == null)
			return null;
		return varBinding.getDeclaringClass();
	}

	private static class MemberAccessNodeCollector extends ASTVisitor{
		private final List fMethodAccesses= new ArrayList(0);
		private final List fFieldAccesses= new ArrayList(0);
		private final List fSimpleNames= new ArrayList(0);
		private final IType fType;
		
		MemberAccessNodeCollector(IType type){
			fType= type;
		}
		MethodInvocation[] getMethodInvocations(){
			return (MethodInvocation[]) fMethodAccesses.toArray(new MethodInvocation[fMethodAccesses.size()]);
		}
		FieldAccess[] getFieldAccesses(){
			return (FieldAccess[]) fFieldAccesses.toArray(new FieldAccess[fFieldAccesses.size()]);
		}
		SimpleName[] getSimpleFieldNames(){
			return (SimpleName[]) fSimpleNames.toArray(new SimpleName[fSimpleNames.size()]);
		}
		
		public boolean visit(MethodInvocation node) {
			ITypeBinding declaringClassBinding= MoveInnerToTopRefactoring.getDeclaringTypeBinding(node);
			if (declaringClassBinding != null && MoveInnerToTopRefactoring.isCorrespondingTypeBinding(declaringClassBinding, fType)) 
				fMethodAccesses.add(node);
			//method invocations can be nested in one another, we have to go on
			return super.visit(node);
		}
		
		public boolean visit(FieldAccess node) {
			ITypeBinding declaringClassBinding= MoveInnerToTopRefactoring.getDeclaringTypeBinding(node);
			if (declaringClassBinding != null && MoveInnerToTopRefactoring.isCorrespondingTypeBinding(declaringClassBinding, fType)) {
				fFieldAccesses.add(node);
				return false;
			}
			return super.visit(node);
		}

		public boolean visit(SimpleName node) {
			if (node.getParent() instanceof QualifiedName)
				return super.visit(node);
			IBinding binding= node.resolveBinding();
			if (binding instanceof IVariableBinding) {
				IVariableBinding vb= (IVariableBinding) binding;
				if (vb.isField() && MoveInnerToTopRefactoring.isCorrespondingTypeBinding(vb.getDeclaringClass(), fType)) {
					fSimpleNames.add(node);
					return false;
				}
			}
			return super.visit(node);
		}
	}


	private class TypeReferenceQualifier extends ASTVisitor {

		private final ITypeBinding fTypeBinding;
		private final TextEditGroup fGroup;

		public TypeReferenceQualifier(final ITypeBinding type, final TextEditGroup group) {
			Assert.isNotNull(type);
			Assert.isNotNull(type.getDeclaringClass());
			fTypeBinding= type;
			fGroup= group;
		}

		public boolean visit(final ClassInstanceCreation node) {
			Assert.isNotNull(node);
			if (fCreateInstanceField) {
				final AST ast= node.getAST();
				final Type type= node.getType();
				final ITypeBinding binding= type.resolveBinding();
				if (binding != null && binding.getDeclaringClass() != null && !Bindings.equals(binding, fTypeBinding) && fSourceRewrite.getRoot().findDeclaringNode(binding) != null) {
					if (!Modifier.isStatic(binding.getModifiers())) {
						Expression expression= null;
						if (fCodeGenerationSettings.useKeywordThis || fEnclosingInstanceFieldName.equals(fNameForEnclosingInstanceConstructorParameter)) {
							final FieldAccess access= ast.newFieldAccess();
							access.setExpression(ast.newThisExpression());
							access.setName(ast.newSimpleName(fEnclosingInstanceFieldName));
							expression= access;
						} else
							expression= ast.newSimpleName(fEnclosingInstanceFieldName);
						if (node.getExpression() != null)
							fSourceRewrite.getImportRemover().registerRemovedNode(node.getExpression());
						fSourceRewrite.getASTRewrite().set(node, ClassInstanceCreation.EXPRESSION_PROPERTY, expression, fGroup);
					} else
						addTypeQualification(type, fSourceRewrite, fGroup);
				}
			}
			return true;
		}

		public boolean visit(final SimpleType node) {
			Assert.isNotNull(node);
			if (!(node.getParent() instanceof ClassInstanceCreation)) {
				final ITypeBinding binding= node.resolveBinding();
				if (binding != null) {
					final ITypeBinding declaring= binding.getDeclaringClass();
					if (declaring != null && !Bindings.equals(declaring, fTypeBinding.getDeclaringClass()) && !Bindings.equals(binding, fTypeBinding) && fSourceRewrite.getRoot().findDeclaringNode(binding) != null && Modifier.isStatic(binding.getModifiers()))
						addTypeQualification(node, fSourceRewrite, fGroup);
				}
			}
			return super.visit(node);
		}

		public boolean visit(final QualifiedType node) {
			Assert.isNotNull(node);
			return false;
		}
	}

	private class TypeVisibilityModifier extends ASTVisitor {

		private final Set handledTypes= new HashSet();

		private final ITypeBinding fBinding;

		private final TextEditGroup fGroup;

		private final RefactoringStatus fStatus;

		public TypeVisibilityModifier(final RefactoringStatus status, final ITypeBinding type, final TextEditGroup group) {
			Assert.isNotNull(status);
			Assert.isNotNull(type);
			Assert.isNotNull(type.getDeclaringClass());
			fStatus= status;
			fBinding= type;
			fGroup= group;
		}

		public boolean visit(final QualifiedType node) {
			visitType(node);
			return false;
		}

		public boolean visit(final SimpleType node) {
			visitType(node);
			return false;
		}

		private void visitType(final Type type) {
			Assert.isNotNull(type);
			final ITypeBinding binding= type.resolveBinding();
			if (binding != null && !handledTypes.contains(binding.getKey()) && !Bindings.equals(fBinding, binding) && Modifier.isPrivate(binding.getModifiers())) {
				final ASTNode node= fSourceRewrite.getRoot().findDeclaringNode(binding);
				if (node instanceof TypeDeclaration) {
					final TypeDeclaration declaration= (TypeDeclaration) node;
					ModifierRewrite.create(fSourceRewrite.getASTRewrite(), declaration).setModifiers(0, Modifier.PRIVATE, fGroup);
					final RefactoringStatusEntry entry= new RefactoringStatusEntry(RefactoringStatus.WARNING, RefactoringCoreMessages.getFormattedString("MoveInnerToTopRefactoring.change_visibility_type_warning", new String[] { Bindings.asString(binding)}), JavaStatusContext.create(fSourceRewrite.getCu(), node)); //$NON-NLS-1$
					if (!containsStatusEntry(fStatus, entry))
						fStatus.addEntry(entry);
				}
			}
		}
	}
}