/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.filebuffers.ITextFileBuffer;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.templates.Template;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ASTNodeDeleteUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsModel;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsSolver;
import org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types.TType;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ISourceConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.ITypeConstraintVariable;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContext;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.ProjectTemplateStore;

/**
 * Refactoring processor to extract interfaces.
 */
public final class ExtractInterfaceProcessor extends SuperTypeRefactoringProcessor {

	/** The identifier of this processor */
	public static final String IDENTIFIER= "org.eclipse.jdt.ui.extractInterfaceProcessor"; //$NON-NLS-1$

	/**
	 * Is the specified member extractable from the type?
	 * 
	 * @param member the member to test
	 * @return <code>true</code> if the member is extractable, <code>false</code> otherwise
	 * @throws JavaModelException if an error occurs
	 */
	protected static boolean isExtractableMember(final IMember member) throws JavaModelException {
		Assert.isNotNull(member);
		switch (member.getElementType()) {
			case IJavaElement.METHOD:
				return JdtFlags.isPublic(member) && !JdtFlags.isStatic(member) && !((IMethod) member).isConstructor();
			case IJavaElement.FIELD:
				return JdtFlags.isPublic(member) && JdtFlags.isStatic(member) && JdtFlags.isFinal(member) && !JdtFlags.isEnum(member);
			default:
				return false;
		}
	}

	/** Should extracted methods be declared as abstract? */
	private boolean fAbstract= true;

	/** The text change manager */
	private TextChangeManager fChangeManager= null;

	/** Should comments be generated? */
	private boolean fComments= true;

	/** The members to extract */
	private IMember[] fMembers= null;

	/** Should extracted methods be declared as public? */
	private boolean fPublic= true;

	/** Should occurrences of the type be replaced by the supertype? */
	private boolean fReplace= false;

	/** The code generation settings */
	private final CodeGenerationSettings fSettings;

	/** The static bindings to import */
	private final Set fStaticBindings= new HashSet();

	/** The subtype where to extract the supertype */
	private final IType fSubType;

	/** The subtype name */
	private String fSuperName;

	/** The source of the new supertype */
	private String fSuperSource= null;

	/** The type bindings to import */
	private final Set fTypeBindings= new HashSet();

	/**
	 * Creates a new extract interface processor.
	 * 
	 * @param type The type where to extract the supertype
	 * @param settings The code generation settings
	 */
	public ExtractInterfaceProcessor(final IType type, final CodeGenerationSettings settings) {
		Assert.isNotNull(type);
		Assert.isNotNull(settings);
		fSubType= type;
		fSettings= settings;
		fSuperName= fSubType.getElementName();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor,org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext)
	 */
	public final RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(context);
		final RefactoringStatus status= new RefactoringStatus();
		fChangeManager= new TextChangeManager();
		try {
			monitor.beginTask("", 4); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_checking);
			status.merge(Checks.checkIfCuBroken(fSubType));
			if (!status.hasError()) {
				if (fSubType.isBinary() || fSubType.isReadOnly() || !fSubType.exists())
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_no_binary, JavaStatusContext.create(fSubType)));
				else if (fSubType.isAnonymous())
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_no_anonymous, JavaStatusContext.create(fSubType)));
				else if (fSubType.isAnnotation())
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_no_annotation, JavaStatusContext.create(fSubType)));
				else {
					status.merge(checkSuperType());
					if (!status.hasFatalError()) {
						monitor.worked(1);
						if (!status.hasFatalError()) {
							fChangeManager= createChangeManager(new SubProgressMonitor(monitor, 1), status);
							if (!status.hasFatalError()) {
								final RefactoringStatus validation= Checks.validateModifiesFiles(ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits()), getRefactoring().getValidationContext());
								if (!validation.isOK())
									return validation;
							}
						}
					}
				}
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final RefactoringStatus checkInitialConditions(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_checking);
			status.merge(Checks.checkIfCuBroken(fSubType));
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Checks whether the supertype clashes with existing types.
	 * 
	 * @return the status of the condition checking
	 * @throws JavaModelException if an error occurs
	 */
	protected final RefactoringStatus checkSuperType() throws JavaModelException {
		final IPackageFragment fragment= fSubType.getPackageFragment();
		final IType type= Checks.findTypeInPackage(fragment, fSuperName);
		if (type != null && type.exists()) {
			if (fragment.isDefaultPackage())
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.ExtractInterfaceProcessor_existing_default_type, new String[] { fSuperName}));
			else
				return RefactoringStatus.createFatalErrorStatus(Messages.format(RefactoringCoreMessages.ExtractInterfaceProcessor_existing_type, new String[] { fSuperName, fragment.getElementName()}));
		}
		return new RefactoringStatus();
	}

	/**
	 * Checks whether the type name is valid.
	 * 
	 * @param name the name to check
	 * @return the status of the condition checking
	 */
	public final RefactoringStatus checkTypeName(final String name) {
		Assert.isNotNull(name);
		try {
			final RefactoringStatus result= Checks.checkTypeName(name);
			if (result.hasFatalError())
				return result;
			result.merge(Checks.checkCompilationUnitName(name + ".java")); //$NON-NLS-1$
			if (result.hasFatalError())
				return result;
			final String path= fSuperName + ".java"; //$NON-NLS-1$
			final IPackageFragment fragment= fSubType.getPackageFragment();
			if (fragment.getCompilationUnit(path).exists()) {
				result.addFatalError(Messages.format(RefactoringCoreMessages.ExtractInterfaceProcessor_existing_compilation_unit, new String[] { path, fragment.getElementName()}));
				return result;
			}
			result.merge(checkSuperType());
			return result;
		} catch (JavaModelException exception) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error);
		}
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 6); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
			final DynamicValidationStateChange change= new DynamicValidationStateChange(RefactoringCoreMessages.ExtractInterfaceRefactoring_name, fChangeManager.getAllChanges());
			final IFile file= ResourceUtil.getFile(fSubType.getCompilationUnit());
			if (fSuperSource != null && fSuperSource.length() > 0)
				change.add(new CreateTextFileChange(file.getFullPath().removeLastSegments(1).append(fSuperName + ".java"), fSuperSource, file.getCharset(false), "java")); //$NON-NLS-1$ //$NON-NLS-2$
			return change;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the text change manager for this processor.
	 * 
	 * @param monitor the progress monitor to display progress
	 * @param status the refactoring status
	 * @return the created text change manager
	 * @throws JavaModelException if the method declaration could not be found
	 * @throws CoreException if the changes could not be generated
	 */
	protected final TextChangeManager createChangeManager(final IProgressMonitor monitor, final RefactoringStatus status) throws JavaModelException, CoreException {
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 4); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
			fSuperSource= null;
			final TextChangeManager manager= new TextChangeManager();
			final CompilationUnitRewrite sourceRewrite= new CompilationUnitRewrite(fSubType.getCompilationUnit());
			final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(fSubType, sourceRewrite.getRoot());
			if (declaration != null) {
				createTypeSignature(sourceRewrite, declaration, status, new SubProgressMonitor(monitor, 1));
				final IField[] fields= getExtractedFields(fSubType.getCompilationUnit());
				if (fields.length > 0)
					ASTNodeDeleteUtil.markAsDeleted(fields, sourceRewrite, null);
				if (fSubType.isInterface()) {
					final IMethod[] methods= getExtractedMethods(fSubType.getCompilationUnit());
					if (methods.length > 0)
						ASTNodeDeleteUtil.markAsDeleted(methods, sourceRewrite, null);
				}
				ICompilationUnit superUnit= null;
				try {
					superUnit= WorkingCopyUtil.getNewWorkingCopy(fSubType.getPackageFragment(), fSuperName + ".java", fOwner, new SubProgressMonitor(monitor, 1)); //$NON-NLS-1$
					fSuperSource= createTypeSource(superUnit, sourceRewrite, declaration, status, new SubProgressMonitor(monitor, 1));
					if (fSuperSource != null) {
						superUnit.getBuffer().setContents(fSuperSource);
						JavaModelUtil.reconcile(superUnit);
					}
					final Set replacements= new HashSet();
					if (fReplace)
						rewriteTypeOccurrences(manager, sourceRewrite, superUnit, replacements, status, new SubProgressMonitor(monitor, 1));
					createMethodComments(sourceRewrite, replacements);
					manager.manage(fSubType.getCompilationUnit(), sourceRewrite.createChange());
				} finally {
					if (superUnit != null)
						superUnit.discardWorkingCopy();
				}
			}
			return manager;
		} finally {
			monitor.done();
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor#createContraintSolver(org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeConstraintsModel)
	 */
	protected final SuperTypeConstraintsSolver createContraintSolver(final SuperTypeConstraintsModel model) {
		return new ExtractInterfaceConstraintsSolver(model, fSuperName);
	}

	/**
	 * Creates a target field declaration.
	 * 
	 * @param sourceRewrite the source compilation unit rewrite
	 * @param targetRewrite the target rewrite
	 * @param targetDeclaration the target type declaration
	 * @param fragment the source variable declaration fragment
	 * @throws CoreException if a buffer could not be retrieved
	 */
	protected final void createFieldDeclaration(final CompilationUnitRewrite sourceRewrite, final ASTRewrite targetRewrite, final AbstractTypeDeclaration targetDeclaration, final VariableDeclarationFragment fragment) throws CoreException {
		Assert.isNotNull(targetDeclaration);
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(fragment);
		final FieldDeclaration field= (FieldDeclaration) fragment.getParent();
		ImportRewriteUtil.collectImports(fSubType.getJavaProject(), field, fTypeBindings, fStaticBindings, false);
		final ASTRewrite rewrite= ASTRewrite.create(field.getAST());
		final ITrackedNodePosition position= rewrite.track(field);
		final ListRewrite rewriter= rewrite.getListRewrite(field, FieldDeclaration.FRAGMENTS_PROPERTY);
		VariableDeclarationFragment current= null;
		for (final Iterator iterator= field.fragments().iterator(); iterator.hasNext();) {
			current= (VariableDeclarationFragment) iterator.next();
			if (!current.getName().getIdentifier().equals(fragment.getName().getIdentifier()))
				rewriter.remove(current, null);
		}
		final ICompilationUnit unit= sourceRewrite.getCu();
		final ITextFileBuffer buffer= RefactoringFileBuffers.acquire(unit);
		try {
			final IDocument document= new Document(buffer.getDocument().get());
			try {
				rewrite.rewriteAST(document, unit.getJavaProject().getOptions(true)).apply(document, TextEdit.UPDATE_REGIONS);
				targetRewrite.getListRewrite(targetDeclaration, targetDeclaration.getBodyDeclarationsProperty()).insertFirst(targetRewrite.createStringPlaceholder(normalizeText(document.get(position.getStartPosition(), position.getLength())), ASTNode.FIELD_DECLARATION), null);
			} catch (MalformedTreeException exception) {
				JavaPlugin.log(exception);
			} catch (BadLocationException exception) {
				JavaPlugin.log(exception);
			}
		} finally {
			RefactoringFileBuffers.release(unit);
		}
	}

	/**
	 * Creates the declarations of the new supertype members.
	 * 
	 * @param sourceRewrite the source compilation unit rewrite
	 * @param targetRewrite the target rewrite
	 * @param targetDeclaration the target type declaration
	 * @throws CoreException if a buffer could not be retrieved
	 */
	protected final void createMemberDeclarations(final CompilationUnitRewrite sourceRewrite, final ASTRewrite targetRewrite, final AbstractTypeDeclaration targetDeclaration) throws CoreException {
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(targetDeclaration);
		Arrays.sort(fMembers, new Comparator() {

			public final int compare(final Object first, final Object second) {
				Assert.isNotNull(first);
				Assert.isNotNull(second);
				final ISourceReference predecessor= (ISourceReference) first;
				final ISourceReference successor= (ISourceReference) second;
				try {
					return predecessor.getSourceRange().getOffset() - successor.getSourceRange().getOffset();
				} catch (JavaModelException exception) {
					return first.hashCode() - second.hashCode();
				}
			}
		});
		fTypeBindings.clear();
		fStaticBindings.clear();
		if (fMembers.length > 0) {
			IMember member= null;
			for (int index= fMembers.length - 1; index >= 0; index--) {
				member= fMembers[index];
				if (member instanceof IField) {
					createFieldDeclaration(sourceRewrite, targetRewrite, targetDeclaration, ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) member, sourceRewrite.getRoot()));
				} else if (member instanceof IMethod) {
					createMethodDeclaration(sourceRewrite, targetRewrite, targetDeclaration, ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) member, sourceRewrite.getRoot()));
				}
			}
		}
	}

	/**
	 * Creates the method comment for the specified declaration.
	 * 
	 * @param rewrite the ast rewrite
	 * @param declaration the method declaration
	 * @param replacements the set of variable binding keys of formal parameters which must be replaced
	 * @param javadoc <code>true</code> if javadoc comments are processed, <code>false</code> otherwise
	 * @throws CoreException if an error occurs
	 */
	protected final void createMethodComment(final ASTRewrite rewrite, final MethodDeclaration declaration, final Set replacements, final boolean javadoc) throws CoreException {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(declaration);
		Assert.isNotNull(replacements);
		final IMethodBinding binding= declaration.resolveBinding();
		if (binding != null) {
			IVariableBinding variable= null;
			SingleVariableDeclaration argument= null;
			final IPackageFragment fragment= fSubType.getPackageFragment();
			final String string= fragment.isDefaultPackage() ? fSuperName : fragment.getElementName() + "." + fSuperName; //$NON-NLS-1$
			final ITypeBinding[] bindings= binding.getParameterTypes();
			final String[] names= new String[bindings.length];
			for (int offset= 0; offset < names.length; offset++) {
				argument= (SingleVariableDeclaration) declaration.parameters().get(offset);
				variable= argument.resolveBinding();
				if (variable != null) {
					if (replacements.contains(variable.getKey()))
						names[offset]= string;
					else {
						if (binding.isVarargs() && bindings[offset].isArray() && offset == names.length - 1)
							names[offset]= Bindings.getFullyQualifiedName(bindings[offset].getElementType());
						else
							names[offset]= Bindings.getFullyQualifiedName(bindings[offset]);
					}
				}
			}
			final String comment= StubUtility.getMethodComment(fSubType.getCompilationUnit(), fSubType.getElementName(), declaration, true, false, string, names, StubUtility.getLineDelimiterUsed(fSubType.getJavaProject())); //$NON-NLS-1$
			if (comment != null) {
				if (declaration.getJavadoc() != null) {
					rewrite.replace(declaration.getJavadoc(), rewrite.createStringPlaceholder(comment, ASTNode.JAVADOC), null);
				} else if (javadoc) {
					rewrite.set(declaration, MethodDeclaration.JAVADOC_PROPERTY, rewrite.createStringPlaceholder(comment, ASTNode.JAVADOC), null);
				}
			}
		}
	}

	/**
	 * Creates the method annotations and comments of the extracted methods in the source type.
	 * 
	 * @param sourceRewrite the source compilation unit rewrite
	 * @param replacements the set of variable binding keys of formal parameters which must be replaced
	 * @throws CoreException if an error occurs
	 */
	protected final void createMethodComments(final CompilationUnitRewrite sourceRewrite, final Set replacements) throws CoreException {
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(replacements);
		if (fComments && fMembers.length > 0) {
			final IJavaProject project= fSubType.getJavaProject();
			final boolean javadoc= project.getOption(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, true).equals(JavaCore.ENABLED);
			IMember member= null;
			for (int index= 0; index < fMembers.length; index++) {
				member= fMembers[index];
				if (member instanceof IMethod)
					createMethodComment(sourceRewrite.getASTRewrite(), ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) member, sourceRewrite.getRoot()), replacements, javadoc);
			}
		}
	}

	/**
	 * Creates a target method declaration.
	 * 
	 * @param sourceRewrite the source compilation unit rewrite
	 * @param targetRewrite the target rewrite
	 * @param targetDeclaration the target type declaration
	 * @param declaration the source method declaration
	 * @throws CoreException if a buffer could not be retrieved
	 */
	protected final void createMethodDeclaration(final CompilationUnitRewrite sourceRewrite, final ASTRewrite targetRewrite, final AbstractTypeDeclaration targetDeclaration, final MethodDeclaration declaration) throws CoreException {
		Assert.isNotNull(targetDeclaration);
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(declaration);
		ImportRewriteUtil.collectImports(fSubType.getJavaProject(), declaration, fTypeBindings, fStaticBindings, true);
		final ASTRewrite rewrite= ASTRewrite.create(declaration.getAST());
		final ITrackedNodePosition position= rewrite.track(declaration);
		if (declaration.getBody() != null)
			rewrite.remove(declaration.getBody(), null);
		final ListRewrite modifiers= rewrite.getListRewrite(declaration, declaration.getModifiersProperty());
		boolean publicFound= false;
		boolean abstractFound= false;
		Modifier modifier= null;
		IExtendedModifier extended= null;
		for (final Iterator iterator= declaration.modifiers().iterator(); iterator.hasNext();) {
			extended= (IExtendedModifier) iterator.next();
			if (!extended.isAnnotation()) {
				modifier= (Modifier) extended;
				if (fPublic && modifier.getKeyword().equals(Modifier.ModifierKeyword.PUBLIC_KEYWORD)) {
					publicFound= true;
					continue;
				}
				if (fAbstract && modifier.getKeyword().equals(Modifier.ModifierKeyword.ABSTRACT_KEYWORD)) {
					abstractFound= true;
					continue;
				}
				modifiers.remove(modifier, null);
			}
		}
		final ModifierRewrite rewriter= ModifierRewrite.create(rewrite, declaration);
		if (fPublic && !publicFound)
			rewriter.setVisibility(Modifier.PUBLIC, null);
		if (fAbstract && !abstractFound)
			rewriter.setModifiers(Modifier.ABSTRACT, 0, null);
		final ICompilationUnit unit= sourceRewrite.getCu();
		final ITextFileBuffer buffer= RefactoringFileBuffers.acquire(unit);
		try {
			final IDocument document= new Document(buffer.getDocument().get());
			try {
				rewrite.rewriteAST(document, unit.getJavaProject().getOptions(true)).apply(document, TextEdit.UPDATE_REGIONS);
				targetRewrite.getListRewrite(targetDeclaration, targetDeclaration.getBodyDeclarationsProperty()).insertFirst(targetRewrite.createStringPlaceholder(normalizeText(document.get(position.getStartPosition(), position.getLength())), ASTNode.METHOD_DECLARATION), null);
			} catch (MalformedTreeException exception) {
				JavaPlugin.log(exception);
			} catch (BadLocationException exception) {
				JavaPlugin.log(exception);
			}
		} finally {
			RefactoringFileBuffers.release(unit);
		}
	}

	/**
	 * Creates the declaration of the new supertype, excluding any comments or package declaration.
	 * 
	 * @param sourceRewrite the source compilation unit rewrite
	 * @param sourceDeclaration the type declaration of the source type
	 * @param buffer the string buffer containing the declaration
	 * @param status the refactoring status
	 * @param monitor the progress monitor to use
	 * @throws CoreException if an error occurs
	 */
	protected final void createTypeDeclaration(final CompilationUnitRewrite sourceRewrite, final AbstractTypeDeclaration sourceDeclaration, final StringBuffer buffer, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(sourceDeclaration);
		Assert.isNotNull(buffer);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		monitor.beginTask("", 1); //$NON-NLS-1$
		monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
		final String delimiter= StubUtility.getLineDelimiterUsed(fSubType.getJavaProject());
		if (JdtFlags.isPublic(fSubType)) {
			buffer.append(JdtFlags.VISIBILITY_STRING_PUBLIC);
			buffer.append(" "); //$NON-NLS-1$
		}
		buffer.append("interface "); //$NON-NLS-1$
		buffer.append(fSuperName);
		buffer.append(" {"); //$NON-NLS-1$
		buffer.append(delimiter);
		buffer.append(delimiter);
		buffer.append('}');
		final IDocument document= new Document(buffer.toString());
		final ASTParser parser= ASTParser.newParser(AST.JLS3);
		parser.setSource(document.get().toCharArray());
		final CompilationUnit unit= (CompilationUnit) parser.createAST(new SubProgressMonitor(monitor, 1));
		final ASTRewrite targetRewrite= ASTRewrite.create(unit.getAST());
		final AbstractTypeDeclaration targetDeclaration= (AbstractTypeDeclaration) unit.types().get(0);
		createTypeParameters(targetRewrite, sourceDeclaration, targetDeclaration);
		createMemberDeclarations(sourceRewrite, targetRewrite, targetDeclaration);
		final TextEdit edit= targetRewrite.rewriteAST(document, fSubType.getJavaProject().getOptions(true));
		try {
			edit.apply(document, TextEdit.UPDATE_REGIONS);
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		buffer.setLength(0);
		buffer.append(document.get());
	}

	/**
	 * Creates the necessary imports for the extracted supertype.
	 * 
	 * @param unit the working copy of the new supertype
	 * @param monitor the progress monitor to use
	 * @return the generated import declaration
	 * @throws CoreException if the imports could not be generated
	 */
	protected final String createTypeImports(final ICompilationUnit unit, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(unit);
		final ImportRewrite rewrite= new ImportRewrite(unit);
		ITypeBinding type= null;
		for (final Iterator iterator= fTypeBindings.iterator(); iterator.hasNext();) {
			type= (ITypeBinding) iterator.next();
			if (type.isTypeVariable()) {
				final ITypeBinding[] bounds= type.getTypeBounds();
				for (int index= 0; index < bounds.length; index++)
					rewrite.addImport(bounds[index]);
			}
			rewrite.addImport(type);
		}
		IBinding binding= null;
		for (final Iterator iterator= fStaticBindings.iterator(); iterator.hasNext();) {
			binding= (IBinding) iterator.next();
			rewrite.addStaticImport(binding);
		}
		final IDocument document= new Document();
		try {
			rewrite.createEdit(document, new SubProgressMonitor(monitor, 1)).apply(document);
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		} catch (CoreException exception) {
			JavaPlugin.log(exception);
		}
		fTypeBindings.clear();
		fStaticBindings.clear();
		return document.get();
	}

	/**
	 * Creates the type parameters of the new supertype.
	 * 
	 * @param targetRewrite the target compilation unit rewrite
	 * @param sourceDeclaration the type declaration of the source type
	 * @param targetDeclaration the type declaration of the target type
	 */
	protected final void createTypeParameters(final ASTRewrite targetRewrite, final AbstractTypeDeclaration sourceDeclaration, final AbstractTypeDeclaration targetDeclaration) {
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(sourceDeclaration);
		Assert.isNotNull(targetDeclaration);
		if (sourceDeclaration instanceof TypeDeclaration) {
			TypeParameter parameter= null;
			final ListRewrite rewrite= targetRewrite.getListRewrite(targetDeclaration, TypeDeclaration.TYPE_PARAMETERS_PROPERTY);
			for (final Iterator iterator= ((TypeDeclaration) sourceDeclaration).typeParameters().iterator(); iterator.hasNext();) {
				parameter= (TypeParameter) iterator.next();
				rewrite.insertLast(ASTNode.copySubtree(targetRewrite.getAST(), parameter), null);
				ImportRewriteUtil.collectImports(fSubType.getJavaProject(), sourceDeclaration, fTypeBindings, fStaticBindings, false);
			}
		}
	}

	/**
	 * Creates the new signature of the source type.
	 * 
	 * @param rewrite the source compilation unit rewrite
	 * @param declaration the type declaration
	 * @param status the refactoring status
	 * @param monitor the progress monitor to use
	 * @throws JavaModelException if the type parameters cannot be retrieved
	 */
	protected final void createTypeSignature(final CompilationUnitRewrite rewrite, final AbstractTypeDeclaration declaration, final RefactoringStatus status, final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(declaration);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		final AST ast= declaration.getAST();
		final ITypeParameter[] parameters= fSubType.getTypeParameters();
		Type type= ast.newSimpleType(ast.newSimpleName(fSuperName));
		if (parameters.length > 0) {
			final ParameterizedType parameterized= ast.newParameterizedType(type);
			for (int index= 0; index < parameters.length; index++)
				parameterized.typeArguments().add(ast.newSimpleType(ast.newSimpleName(parameters[index].getElementName())));
			type= parameterized;
		}
		final ASTRewrite rewriter= rewrite.getASTRewrite();
		if (declaration instanceof TypeDeclaration)
			rewriter.getListRewrite(declaration, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY).insertLast(type, null);
		else if (declaration instanceof EnumDeclaration)
			rewriter.getListRewrite(declaration, EnumDeclaration.SUPER_INTERFACE_TYPES_PROPERTY).insertLast(type, null);
	}

	/**
	 * Creates the source for the new compilation unit containing the supertype.
	 * 
	 * @param copy the working copy of the new supertype
	 * @param sourceRewrite the source compilation unit rewrite
	 * @param declaration the type declaration
	 * @param status the refactoring status
	 * @param monitor the progress monitor to display progress
	 * @return the source of the new compilation unit, or <code>null</code>
	 * @throws CoreException if an error occurs
	 */
	protected final String createTypeSource(final ICompilationUnit copy, final CompilationUnitRewrite sourceRewrite, final AbstractTypeDeclaration declaration, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(copy);
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(declaration);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		String source= null;
		try {
			monitor.beginTask("", 2); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
			final String delimiter= StubUtility.getLineDelimiterUsed(fSubType.getJavaProject());
			String typeComment= null;
			String fileComment= null;
			if (fSettings.createComments) {
				final ITypeParameter[] parameters= fSubType.getTypeParameters();
				final String[] names= new String[parameters.length];
				for (int index= 0; index < parameters.length; index++)
					names[index]= parameters[index].getElementName();
				typeComment= CodeGeneration.getTypeComment(copy, fSubType.getTypeQualifiedName('.'), names, delimiter);
				fileComment= CodeGeneration.getFileComment(copy, delimiter);
			}
			final StringBuffer buffer= new StringBuffer(64);
			createTypeDeclaration(sourceRewrite, declaration, buffer, status, new SubProgressMonitor(monitor, 1));
			final String imports= createTypeImports(copy, monitor);
			source= createTypeTemplate(copy, imports, fileComment, typeComment, buffer.toString());
			if (source == null) {
				if (!fSubType.getPackageFragment().isDefaultPackage()) {
					if (imports.length() > 0)
						buffer.insert(0, imports);
					buffer.insert(0, "package " + fSubType.getPackageFragment().getElementName() + ";"); //$NON-NLS-1$//$NON-NLS-2$
				}
				source= buffer.toString();
			}
			final IDocument document= new Document(source);
			final TextEdit edit= CodeFormatterUtil.format2(CodeFormatter.K_COMPILATION_UNIT, source, 0, delimiter, copy.getJavaProject().getOptions(true));
			if (edit != null) {
				try {
					edit.apply(document, TextEdit.UPDATE_REGIONS);
				} catch (MalformedTreeException exception) {
					JavaPlugin.log(exception);
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
				} catch (BadLocationException exception) {
					JavaPlugin.log(exception);
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
				}
				source= document.get();
			}
		} finally {
			monitor.done();
		}
		return source;
	}

	/**
	 * Creates the type template based on the code generation settings.
	 * 
	 * @param unit the working copy for the new supertype
	 * @param imports the generated imports declaration
	 * @param fileComment the file comment
	 * @param comment the type comment
	 * @param content the type content
	 * @return a template for the supertype, or <code>null</code>
	 * @throws CoreException if the template could not be evaluated
	 */
	protected final String createTypeTemplate(final ICompilationUnit unit, final String imports, String fileComment, final String comment, final String content) throws CoreException {
		Assert.isNotNull(unit);
		Assert.isNotNull(imports);
		Assert.isNotNull(content);
		final IPackageFragment fragment= (IPackageFragment) unit.getParent();
		final Template template= new ProjectTemplateStore(unit.getJavaProject().getProject()).findTemplateById(CodeTemplateContextType.NEWTYPE_ID);
		if (template != null) {
			final CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), unit.getJavaProject(), StubUtility.getLineDelimiterUsed(fSubType.getJavaProject()));
			context.setCompilationUnitVariables(unit);
			final StringBuffer buffer= new StringBuffer();
			final String delimiter= StubUtility.getLineDelimiterUsed(fSubType.getJavaProject());
			if (!fragment.isDefaultPackage()) {
				buffer.append("package " + fragment.getElementName() + ";"); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append(delimiter);
				buffer.append(delimiter);
			}
			if (imports.length() > 0)
				buffer.append(imports);

			context.setVariable(CodeTemplateContextType.PACKAGE_DECLARATION, buffer.toString());
			context.setVariable(CodeTemplateContextType.FILE_COMMENT, fileComment != null ? fileComment : ""); //$NON-NLS-1$
			context.setVariable(CodeTemplateContextType.TYPE_COMMENT, comment != null ? comment : ""); //$NON-NLS-1$
			context.setVariable(CodeTemplateContextType.TYPE_DECLARATION, content);
			context.setVariable(CodeTemplateContextType.TYPENAME, Signature.getQualifier(unit.getElementName()));
			return StubUtility.evaluateTemplate(context, template);
		}
		return null;
	}

	/**
	 * Should extracted methods be declared as abstract?
	 * 
	 * @return <code>true</code> if the should be declared as abstract, <code>false</code> otherwise
	 */
	public final boolean getAbstract() {
		return fAbstract;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getElements()
	 */
	public final Object[] getElements() {
		return new Object[] { fSubType};
	}

	/**
	 * Returns the list of extractable members from the type.
	 * 
	 * @return the list of extractable members
	 * @throws JavaModelException if an error occurs
	 */
	public final IMember[] getExtractableMembers() throws JavaModelException {
		final List list= new ArrayList();
		IJavaElement[] children= fSubType.getChildren();
		for (int index= 0; index < children.length; index++) {
			if (children[index] instanceof IMember && isExtractableMember((IMember) children[index]))
				list.add(children[index]);
		}
		final IMember[] members= new IMember[list.size()];
		list.toArray(members);
		return members;
	}

	/**
	 * Returns the extracted fields from the compilation unit.
	 * 
	 * @param unit the compilation unit
	 * @return the extracted fields
	 */
	protected final IField[] getExtractedFields(final ICompilationUnit unit) {
		Assert.isNotNull(unit);
		final List list= new ArrayList();
		for (int index= 0; index < fMembers.length; index++) {
			if (fMembers[index] instanceof IField) {
				final IJavaElement element= JavaModelUtil.findInCompilationUnit(unit, fMembers[index]);
				if (element instanceof IField)
					list.add(element);
			}
		}
		final IField[] fields= new IField[list.size()];
		list.toArray(fields);
		return fields;
	}

	/**
	 * Returns the extracted methods from the compilation unit.
	 * 
	 * @param unit the compilation unit
	 * @return the extracted methods
	 */
	protected final IMethod[] getExtractedMethods(final ICompilationUnit unit) {
		Assert.isNotNull(unit);
		final List list= new ArrayList();
		for (int index= 0; index < fMembers.length; index++) {
			if (fMembers[index] instanceof IMethod) {
				final IJavaElement element= JavaModelUtil.findInCompilationUnit(unit, fMembers[index]);
				if (element instanceof IMethod)
					list.add(element);
			}
		}
		final IMethod[] methods= new IMethod[list.size()];
		list.toArray(methods);
		return methods;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getIdentifier()
	 */
	public final String getIdentifier() {
		return IDENTIFIER;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getProcessorName()
	 */
	public final String getProcessorName() {
		return Messages.format(RefactoringCoreMessages.ExtractInterfaceProcessor_name, new String[] { fSubType.getElementName()});
	}

	/**
	 * Should extracted methods be declared as public?
	 * 
	 * @return <code>true</code> if the should be declared as public, <code>false</code> otherwise
	 */
	public final boolean getPublic() {
		return fPublic;
	}

	/**
	 * Returns the type where to extract an interface.
	 * 
	 * @return the type where to extract an interface
	 */
	public final IType getType() {
		return fSubType;
	}

	/**
	 * Returns the new interface name.
	 * 
	 * @return the new interface name
	 */
	public final String getTypeName() {
		return fSuperName;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#isApplicable()
	 */
	public final boolean isApplicable() throws CoreException {
		return Checks.isAvailable(fSubType) && !fSubType.isBinary() && !fSubType.isReadOnly() && !fSubType.isAnnotation() && !fSubType.isAnonymous();
	}

	/**
	 * Should comments be generated?
	 * 
	 * @return <code>true</code> if comments should be generated, <code>false</code> otherwise
	 */
	public final boolean isComments() {
		return fComments;
	}

	/**
	 * Should occurrences of the type be replaced by the interface?
	 * 
	 * @return <code>true</code> if the should be replaced, <code>false</code> otherwise
	 */
	public final boolean isReplace() {
		return fReplace;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#loadParticipants(org.eclipse.ltk.core.refactoring.RefactoringStatus,org.eclipse.ltk.core.refactoring.participants.SharableParticipants)
	 */
	public final RefactoringParticipant[] loadParticipants(final RefactoringStatus status, final SharableParticipants sharedParticipants) throws CoreException {
		return new RefactoringParticipant[0];
	}

	/**
	 * Normalizes the indentation of the specified text.
	 * 
	 * @param code the text to normalize
	 * @return the normalized text
	 * @throws JavaModelException if an error occurs
	 */
	protected final String normalizeText(final String code) throws JavaModelException {
		Assert.isNotNull(code);
		final String[] lines= Strings.convertIntoLines(code);
		final IJavaProject project= fSubType.getJavaProject();
		Strings.trimIndentation(lines, project, false);
		return Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(project));
	}

	/*
	 * @see org.eclipse.jdt.internal.corext.refactoring.structure.constraints.SuperTypeRefactoringProcessor#rewriteTypeOccurrences(org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager, org.eclipse.jdt.core.dom.ASTRequestor, org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite, org.eclipse.jdt.core.ICompilationUnit, org.eclipse.jdt.core.dom.CompilationUnit, java.util.Set)
	 */
	protected final void rewriteTypeOccurrences(final TextChangeManager manager, final ASTRequestor requestor, final CompilationUnitRewrite rewrite, final ICompilationUnit unit, final CompilationUnit node, final Set replacements) throws CoreException {
		CompilationUnitRewrite currentRewrite= null;
		final boolean isSubUnit= rewrite.getCu().equals(unit.getPrimary());
		if (isSubUnit)
			currentRewrite= rewrite;
		else
			currentRewrite= new CompilationUnitRewrite(unit, node);
		final Collection collection= (Collection) fTypeOccurrences.get(unit);
		if (collection != null && !collection.isEmpty()) {
			TType estimate= null;
			ISourceConstraintVariable variable= null;
			ITypeConstraintVariable constraint= null;
			for (final Iterator iterator= collection.iterator(); iterator.hasNext();) {
				variable= (ISourceConstraintVariable) iterator.next();
				if (variable instanceof ITypeConstraintVariable) {
					constraint= (ITypeConstraintVariable) variable;
					estimate= (TType) constraint.getData(SuperTypeConstraintsSolver.DATA_TYPE_ESTIMATE);
					if (estimate != null) {
						final CompilationUnitRange range= constraint.getRange();
						if (isSubUnit)
							rewriteTypeOccurrence(range, estimate, requestor, currentRewrite, node, replacements, currentRewrite.createGroupDescription(RefactoringCoreMessages.SuperTypeRefactoringProcessor_update_type_occurrence));
						else {
							final ASTNode result= NodeFinder.perform(node, range.getSourceRange());
							if (result != null)
								rewriteTypeOccurrence(estimate, currentRewrite, result, currentRewrite.createGroupDescription(RefactoringCoreMessages.SuperTypeRefactoringProcessor_update_type_occurrence));
						}
					}
				}
			}
		}
		if (!isSubUnit) {
			final TextChange change= currentRewrite.createChange();
			if (change != null)
				manager.manage(unit, change);
		}
	}

	/**
	 * Creates the necessary text edits to replace the subtype occurrences by a supertype.
	 * 
	 * @param manager the text change manager
	 * @param sourceRewrite the compilation unit of the subtype (not in working copy mode)
	 * @param superUnit the compilation unit of the supertype (in working copy mode)
	 * @param replacements the set of variable binding keys of formal parameters which must be replaced
	 * @param status the refactoring status
	 * @param monitor the progress monitor to display progress
	 * @throws CoreException if an error occurs
	 */
	protected final void rewriteTypeOccurrences(final TextChangeManager manager, final CompilationUnitRewrite sourceRewrite, final ICompilationUnit superUnit, final Set replacements, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(manager);
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(superUnit);
		Assert.isNotNull(replacements);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 4); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractInterfaceProcessor_creating);
			ICompilationUnit subUnit= null;
			try {
				subUnit= WorkingCopyUtil.getNewWorkingCopy(fSubType.getCompilationUnit(), fOwner, new SubProgressMonitor(monitor, 1));
				final ITextFileBuffer buffer= RefactoringFileBuffers.acquire(fSubType.getCompilationUnit());
				final ASTRewrite rewrite= sourceRewrite.getASTRewrite();
				try {
					final IDocument document= new Document(buffer.getDocument().get());
					try {
						rewrite.rewriteAST(document, fSubType.getJavaProject().getOptions(true)).apply(document, TextEdit.UPDATE_REGIONS);
					} catch (MalformedTreeException exception) {
						JavaPlugin.log(exception);
					} catch (BadLocationException exception) {
						JavaPlugin.log(exception);
					}
					subUnit.getBuffer().setContents(document.get());
				} finally {
					RefactoringFileBuffers.release(fSubType.getCompilationUnit());
				}
				JavaModelUtil.reconcile(subUnit);
				final IJavaProject project= subUnit.getJavaProject();
				final ASTParser parser= ASTParser.newParser(AST.JLS3);
				parser.setWorkingCopyOwner(fOwner);
				parser.setResolveBindings(true);
				parser.setProject(project);
				parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
				parser.createASTs(new ICompilationUnit[] { subUnit}, new String[0], new ASTRequestor() {

					public final void acceptAST(final ICompilationUnit unit, final CompilationUnit node) {
						try {
							final IType subType= (IType) JavaModelUtil.findInCompilationUnit(unit, fSubType);
							final AbstractTypeDeclaration subDeclaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(subType, node);
							if (subDeclaration != null) {
								final ITypeBinding subBinding= subDeclaration.resolveBinding();
								if (subBinding != null) {
									String name= null;
									ITypeBinding superBinding= null;
									final ITypeBinding[] superBindings= subBinding.getInterfaces();
									for (int index= 0; index < superBindings.length; index++) {
										name= superBindings[index].getName();
										if (name.startsWith(fSuperName) && superBindings[index].getTypeArguments().length == subBinding.getTypeParameters().length)
											superBinding= superBindings[index];
									}
									if (superBinding != null) {
										solveSuperTypeConstraints(unit, node, subType, subBinding, superBinding, new SubProgressMonitor(monitor, 1), status);
										if (!status.hasFatalError()) {
											rewriteTypeOccurrences(manager, this, sourceRewrite, unit, node, replacements, status, new SubProgressMonitor(monitor, 1));
											if (manager.containsChangesIn(superUnit)) {
												final TextEdit edit= manager.get(superUnit).getEdit();
												if (edit != null) {
													final IDocument document= new Document(superUnit.getBuffer().getContents());
													try {
														edit.apply(document, TextEdit.UPDATE_REGIONS);
													} catch (MalformedTreeException exception) {
														JavaPlugin.log(exception);
														status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
													} catch (BadLocationException exception) {
														JavaPlugin.log(exception);
														status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
													}
													fSuperSource= document.get();
													manager.remove(superUnit);
												}
											}
										}
									}
								}
							}
						} catch (JavaModelException exception) {
							JavaPlugin.log(exception);
							status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractInterfaceProcessor_internal_error));
						}
					}

					public final void acceptBinding(final String key, final IBinding binding) {
						// Do nothing
					}
				}, new SubProgressMonitor(monitor, 1));
			} finally {
				if (subUnit != null)
					subUnit.discardWorkingCopy();
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Determines whether extracted methods should be declared as abstract.
	 * 
	 * @param declare <code>true</code> to declare them public, <code>false</code> otherwise
	 */
	public final void setAbstract(final boolean declare) {
		fAbstract= declare;
	}

	/**
	 * Determines whether comments should be generated.
	 * 
	 * @param comments <code>true</code> to generate comments, <code>false</code> otherwise
	 */
	public final void setComments(final boolean comments) {
		fComments= comments;
	}

	/**
	 * Sets the members to be extracted.
	 * 
	 * @param members the members to be extracted
	 * @throws JavaModelException if an error occurs
	 */
	public final void setExtractedMembers(final IMember[] members) throws JavaModelException {
		fMembers= members;
	}

	/**
	 * Determines whether extracted methods should be declared as public.
	 * 
	 * @param declare <code>true</code> to declare them public, <code>false</code> otherwise
	 */
	public final void setPublic(final boolean declare) {
		fPublic= declare;
	}

	/**
	 * Determines whether occurrences of the type should be replaced by the interface.
	 * 
	 * @param replace <code>true</code> to replace occurrences where possible, <code>false</code> otherwise
	 */
	public final void setReplace(final boolean replace) {
		fReplace= replace;
	}

	/**
	 * Sets the new interface name.
	 * 
	 * @param name the new interface name
	 */
	public final void setTypeName(final String name) {
		Assert.isNotNull(name);
		fSuperName= name;
	}
}