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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

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
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
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
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CastExpression;
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
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ASTNodeDeleteUtil;
import org.eclipse.jdt.internal.corext.refactoring.typeconstraints.CompilationUnitRange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringFileBuffers;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContext;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.ProjectTemplateStore;

/**
 * Refactoring processor to extract interfaces.
 */
public final class ExtractInterfaceProcessor extends RefactoringProcessor {

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
				return JdtFlags.isPublic(member) && JdtFlags.isStatic(member) && JdtFlags.isFinal(member);
			default:
				return false;
		}
	}

	/**
	 * Sorts the given members by source offset.
	 * 
	 * @param members the members to sort
	 */
	protected static void sortByOffset(final IMember[] members) {
		Assert.isNotNull(members);
		Arrays.sort(members, new Comparator() {

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
	}

	/** Should extracted methods be declared as abstract? */
	private boolean fAbstract= true;

	/** Should override annotations be generated? */
	private boolean fAnnotations= false;

	/** The text change manager */
	private TextChangeManager fChangeManager= null;

	/** Should comments be generated? */
	private boolean fComments= true;

	/** The interface name */
	private String fInterfaceName;

	/** The source of the new interface */
	private String fInterfaceSource= null;

	/** The members to extract */
	private IMember[] fMembers= null;

	/** The working copy owner */
	private final WorkingCopyOwner fOwner= new RefactoringWorkingCopyOwner();

	/** Should extracted methods be declared as public? */
	private boolean fPublic= true;

	/** Should occurrences of the type be replaced by the interface? */
	private boolean fReplace= false;

	/** The code generation settings */
	private final CodeGenerationSettings fSettings;

	/** The static bindings to import */
	private final Set fStaticBindings= new HashSet();

	/** The type where to extract the interface */
	private final IType fType;

	/** The type bindings to import */
	private final Set fTypeBindings= new HashSet();

	/**
	 * Creates a new extract interface processor.
	 * 
	 * @param type The type where to extract the interface
	 * @param settings The code generation settings
	 */
	public ExtractInterfaceProcessor(final IType type, final CodeGenerationSettings settings) {
		Assert.isNotNull(type);
		Assert.isNotNull(settings);
		fType= type;
		fSettings= settings;
		fInterfaceName= fType.getElementName();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext)
	 */
	public final RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		Assert.isNotNull(context);
		final RefactoringStatus status= new RefactoringStatus();
		fChangeManager= new TextChangeManager();
		try {
			monitor.beginTask("", 4); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.checking")); //$NON-NLS-1$
			status.merge(Checks.checkIfCuBroken(fType));
			if (!status.hasError()) {
				if (fType.isBinary() || fType.isReadOnly() || !fType.exists())
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.no_binary"), JavaStatusContext.create(fType))); //$NON-NLS-1$
				else if (fType.isAnonymous())
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.no_anonymous"), JavaStatusContext.create(fType))); //$NON-NLS-1$
				else if (fType.isAnnotation())
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.no_annotation"), JavaStatusContext.create(fType))); //$NON-NLS-1$
				else {
					status.merge(checkInterfaceType());
					if (!status.hasFatalError()) {
						status.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[] { fType.getCompilationUnit()}), null));
						monitor.worked(1);
						if (!status.hasFatalError())
							fChangeManager= createChangeManager(status, new SubProgressMonitor(monitor, 1));
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
			monitor.setTaskName(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.checking")); //$NON-NLS-1$
			status.merge(Checks.checkIfCuBroken(fType));
			if (!status.hasError()) {
				if (Checks.isException(fType, new SubProgressMonitor(monitor, 1)))
					status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.no_throwable"))); //$NON-NLS-1$
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Checks whether the interface name is valid.
	 * 
	 * @param name the name to check
	 * @return the status of the condition checking
	 */
	public final RefactoringStatus checkInterfaceName(final String name) {
		Assert.isNotNull(name);
		try {
			final RefactoringStatus result= Checks.checkTypeName(name);
			if (result.hasFatalError())
				return result;
			result.merge(Checks.checkCompilationUnitName(name + ".java")); //$NON-NLS-1$
			if (result.hasFatalError())
				return result;
			final String path= fInterfaceName + ".java"; //$NON-NLS-1$
			final IPackageFragment fragment= fType.getPackageFragment();
			if (fragment.getCompilationUnit(path).exists()) {
				result.addFatalError(RefactoringCoreMessages.getFormattedString("ExtractInterfaceProcessor.existing_compilation_unit", new String[] { path, fragment.getElementName()})); //$NON-NLS-1$
				return result;
			}
			result.merge(checkInterfaceType());
			return result;
		} catch (JavaModelException exception) {
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.internal_error")); //$NON-NLS-1$
		}
	}

	/**
	 * Checks whether the interface type clashes with existing types.
	 * 
	 * @return the status of the condition checking
	 * @throws JavaModelException if an error occurs
	 */
	protected RefactoringStatus checkInterfaceType() throws JavaModelException {
		final IPackageFragment fragment= fType.getPackageFragment();
		final IType type= Checks.findTypeInPackage(fragment, fInterfaceName);
		if (type != null && type.exists()) {
			if (fragment.isDefaultPackage())
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getFormattedString("ExtractInterfaceProcessor.existing_default_type", new String[] { fInterfaceName})); //$NON-NLS-1$
			else
				return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getFormattedString("ExtractInterfaceProcessor.existing_type", new String[] { fInterfaceName, fragment.getElementName()})); //$NON-NLS-1$
		}
		return new RefactoringStatus();
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 6); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.creating")); //$NON-NLS-1$
			final DynamicValidationStateChange change= new DynamicValidationStateChange(RefactoringCoreMessages.getString("ExtractInterfaceRefactoring.name"), fChangeManager.getAllChanges()); //$NON-NLS-1$
			final IFile file= ResourceUtil.getFile(fType.getCompilationUnit());
			if (fInterfaceSource != null && fInterfaceSource.length() > 0)
				change.add(new CreateTextFileChange(file.getFullPath().removeLastSegments(1).append(fInterfaceName + ".java"), fInterfaceSource, file.getCharset(false), "java")); //$NON-NLS-1$ //$NON-NLS-2$
			return change;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the text change manager for this processor.
	 * 
	 * @param status the refactoring status
	 * @param monitor the progress monitor to display progress
	 * @return the created text change manager
	 * @throws JavaModelException if the method declaration could not be found
	 * @throws CoreException if the changes could not be generated
	 */
	protected TextChangeManager createChangeManager(final RefactoringStatus status, final IProgressMonitor monitor) throws JavaModelException, CoreException {
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 4); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.creating")); //$NON-NLS-1$
			fInterfaceSource= null;
			final TextChangeManager manager= new TextChangeManager();
			final CompilationUnitRewrite sourceRewrite= new CompilationUnitRewrite(fType.getCompilationUnit());
			final AbstractTypeDeclaration declaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(fType, sourceRewrite.getRoot());
			if (declaration != null) {
				createTypeSignature(sourceRewrite, declaration, status, new SubProgressMonitor(monitor, 1));
				final IField[] fields= getExtractedFields(fType.getCompilationUnit());
				if (fields.length > 0)
					ASTNodeDeleteUtil.markAsDeleted(fields, sourceRewrite, null);
				if (fType.isInterface()) {
					final IMethod[] methods= getExtractedMethods(fType.getCompilationUnit());
					if (methods.length > 0)
						ASTNodeDeleteUtil.markAsDeleted(methods, sourceRewrite, null);
				}
				ICompilationUnit copy= null;
				try {
					copy= WorkingCopyUtil.getNewWorkingCopy(fType.getPackageFragment(), fInterfaceName + ".java", fOwner, new SubProgressMonitor(monitor, 1)); //$NON-NLS-1$
					fInterfaceSource= createInterfaceSource(copy, sourceRewrite, declaration, status, new SubProgressMonitor(monitor, 1));
					final Set replacements= new HashSet();
					if (fReplace)
						createOccurrencesReplacements(manager, copy, sourceRewrite, declaration, replacements, status, new SubProgressMonitor(monitor, 1));
					createMethodComments(sourceRewrite, replacements);
					manager.manage(fType.getCompilationUnit(), sourceRewrite.createChange());
				} finally {
					if (copy != null)
						copy.discardWorkingCopy();
				}
			}
			return manager;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates a target field declaration.
	 * 
	 * @param sourceRewrite the source compilation unit rewrite
	 * @param targetRewrite the target rewrite
	 * @param targetDeclaration the target interface declaration
	 * @param fragment the source variable declaration fragment
	 * @throws CoreException if a buffer could not be retrieved
	 */
	protected void createFieldDeclaration(final CompilationUnitRewrite sourceRewrite, final ASTRewrite targetRewrite, final AbstractTypeDeclaration targetDeclaration, final VariableDeclarationFragment fragment) throws CoreException {
		Assert.isNotNull(targetDeclaration);
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(fragment);
		final FieldDeclaration field= (FieldDeclaration) fragment.getParent();
		ImportRewriteUtil.collectImports(fType.getJavaProject(), field, fTypeBindings, fStaticBindings, false);
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
	 * Creates the declaration of the new interface, excluding any comments or package declaration.
	 * 
	 * @param sourceRewrite the source compilation unit rewrite
	 * @param sourceDeclaration the type declaration of the source type
	 * @param buffer the string buffer containing the declaration
	 * @param status the refactoring status
	 * @param monitor the progress monitor to use
	 * @throws CoreException if an error occurs
	 */
	protected void createInterfaceDeclaration(final CompilationUnitRewrite sourceRewrite, final AbstractTypeDeclaration sourceDeclaration, final StringBuffer buffer, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(sourceDeclaration);
		Assert.isNotNull(buffer);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		monitor.beginTask("", 1); //$NON-NLS-1$
		monitor.setTaskName(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.creating")); //$NON-NLS-1$
		final String delimiter= getLineDelimiter();
		if (JdtFlags.isPublic(fType)) {
			buffer.append(JdtFlags.VISIBILITY_STRING_PUBLIC);
			buffer.append(" "); //$NON-NLS-1$
		}
		buffer.append("interface "); //$NON-NLS-1$
		buffer.append(fInterfaceName);
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
		final TextEdit edit= targetRewrite.rewriteAST(document, fType.getJavaProject().getOptions(true));
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
	 * Creates the necessary imports for the extracted interface.
	 * 
	 * @param unit the working copy of the new interface
	 * @return the generated import declaration
	 * @throws CoreException if the imports could not be generated
	 */
	protected String createInterfaceImports(final ICompilationUnit unit) throws CoreException {
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
			rewrite.createEdit(document).apply(document);
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		} catch (CoreException exception) {
			JavaPlugin.log(exception);
		}
		return document.get();
	}

	/**
	 * Creates the source for the new compilation unit containing the extracted interface.
	 * 
	 * @param copy the working copy of the new interface
	 * @param sourceRewrite the source compilation unit rewrite
	 * @param declaration the type declaration
	 * @param status the refactoring status
	 * @param monitor the progress monitor to display progress
	 * @return the source of the new compilation unit, or <code>null</code>
	 * @throws CoreException if an error occurs
	 */
	protected String createInterfaceSource(final ICompilationUnit copy, final CompilationUnitRewrite sourceRewrite, final AbstractTypeDeclaration declaration, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(copy);
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(declaration);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		String source= null;
		try {
			monitor.beginTask("", 2); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.creating")); //$NON-NLS-1$
			final String delimiter= getLineDelimiter();
			String comment= null;
			if (fSettings.createComments) {
				final ITypeParameter[] parameters= fType.getTypeParameters();
				final String[] names= new String[parameters.length];
				for (int index= 0; index < parameters.length; index++)
					names[index]= parameters[index].getElementName();
				comment= CodeGeneration.getTypeComment(copy, fType.getTypeQualifiedName('.'), names, delimiter);
			}
			final StringBuffer buffer= new StringBuffer(64);
			createInterfaceDeclaration(sourceRewrite, declaration, buffer, status, new SubProgressMonitor(monitor, 1));
			final String imports= createInterfaceImports(copy);
			source= createInterfaceTemplate(copy, imports, comment, buffer.toString());
			if (source == null) {
				if (!fType.getPackageFragment().isDefaultPackage()) {
					if (imports.length() > 0)
						buffer.insert(0, imports);
					buffer.insert(0, "package " + fType.getPackageFragment().getElementName() + ";"); //$NON-NLS-1$//$NON-NLS-2$
				}
				source= buffer.toString();
			}
		} finally {
			monitor.done();
		}
		return source;
	}

	/**
	 * Creates the interface template based on the code generation settings.
	 * 
	 * @param unit the working copy for the new interface
	 * @param imports the generated imports declaration
	 * @param comment the type comment
	 * @param content the type content
	 * @return a template for the interface, or <code>null</code>
	 * @throws CoreException if the template could not be evaluated
	 */
	protected String createInterfaceTemplate(final ICompilationUnit unit, final String imports, final String comment, final String content) throws CoreException {
		Assert.isNotNull(unit);
		Assert.isNotNull(imports);
		Assert.isNotNull(content);
		final IPackageFragment fragment= (IPackageFragment) unit.getParent();
		final Template template= new ProjectTemplateStore(unit.getJavaProject().getProject()).findTemplateById(CodeTemplateContextType.NEWTYPE_ID);
		if (template != null) {
			final CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeId(), unit.getJavaProject(), getLineDelimiter());
			context.setCompilationUnitVariables(unit);
			final StringBuffer buffer= new StringBuffer();
			final String delimiter= getLineDelimiter();
			if (!fragment.isDefaultPackage()) {
				buffer.append("package " + fragment.getElementName() + ";"); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append(delimiter);
				buffer.append(delimiter);
			}
			if (imports.length() > 0)
				buffer.append(imports);
			context.setVariable(CodeTemplateContextType.PACKAGE_DECLARATION, buffer.toString());
			context.setVariable(CodeTemplateContextType.TYPE_COMMENT, comment != null ? comment : ""); //$NON-NLS-1$
			context.setVariable(CodeTemplateContextType.TYPE_DECLARATION, content);
			context.setVariable(CodeTemplateContextType.TYPENAME, Signature.getQualifier(unit.getElementName()));
			return StubUtility.evaluateTemplate(context, template);
		}
		return null;
	}

	/**
	 * Creates the declarations of the new interface members.
	 * 
	 * @param sourceRewrite the source compilation unit rewrite
	 * @param targetRewrite the target rewrite
	 * @param targetDeclaration the target interface declaration
	 * @throws CoreException if a buffer could not be retrieved
	 */
	protected void createMemberDeclarations(final CompilationUnitRewrite sourceRewrite, final ASTRewrite targetRewrite, final AbstractTypeDeclaration targetDeclaration) throws CoreException {
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(targetDeclaration);
		sortByOffset(fMembers);
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
	protected void createMethodComment(final ASTRewrite rewrite, final MethodDeclaration declaration, final Set replacements, final boolean javadoc) throws CoreException {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(declaration);
		Assert.isNotNull(replacements);
		final IMethodBinding binding= declaration.resolveBinding();
		if (binding != null) {
			IVariableBinding variable= null;
			SingleVariableDeclaration argument= null;
			final IPackageFragment fragment= fType.getPackageFragment();
			final String string= fragment.isDefaultPackage() ? fInterfaceName : fragment.getElementName() + "." + fInterfaceName; //$NON-NLS-1$
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
			final String comment= StubUtility.getMethodComment(fType.getCompilationUnit(), fType.getElementName(), declaration, true, false, string, names, getLineDelimiter()); //$NON-NLS-1$
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
	protected void createMethodComments(final CompilationUnitRewrite sourceRewrite, final Set replacements) throws CoreException {
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(replacements);
		if (fMembers.length > 0) {
			final IJavaProject project= fType.getJavaProject();
			final boolean annotations= project.getOption(JavaCore.COMPILER_COMPLIANCE, true).equals(JavaCore.VERSION_1_5) && project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true).equals(JavaCore.VERSION_1_5);
			final boolean javadoc= project.getOption(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, true).equals(JavaCore.ENABLED);
			IMember member= null;
			for (int index= 0; index < fMembers.length; index++) {
				member= fMembers[index];
				if (member instanceof IMethod) {
					final ASTRewrite rewrite= sourceRewrite.getASTRewrite();
					final MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) member, sourceRewrite.getRoot());
					if (fAnnotations && annotations) {
						final Annotation marker= rewrite.getAST().newMarkerAnnotation();
						marker.setTypeName(rewrite.getAST().newSimpleName("Override")); //$NON-NLS-1$
						rewrite.getListRewrite(declaration, MethodDeclaration.MODIFIERS2_PROPERTY).insertFirst(marker, null);
					}
					if (fComments)
						createMethodComment(rewrite, declaration, replacements, javadoc);
				}
			}
		}
	}

	/**
	 * Creates a target method declaration.
	 * 
	 * @param sourceRewrite the source compilation unit rewrite
	 * @param targetRewrite the target rewrite
	 * @param targetDeclaration the target interface declaration
	 * @param declaration the source method declaration
	 * @throws CoreException if a buffer could not be retrieved
	 */
	protected void createMethodDeclaration(final CompilationUnitRewrite sourceRewrite, final ASTRewrite targetRewrite, final AbstractTypeDeclaration targetDeclaration, final MethodDeclaration declaration) throws CoreException {
		Assert.isNotNull(targetDeclaration);
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(declaration);
		ImportRewriteUtil.collectImports(fType.getJavaProject(), declaration, fTypeBindings, fStaticBindings, true);
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
	 * Creates a new occurrence replacement.
	 * 
	 * @param range the compilation unit range
	 * @param rewrite the ast rewrite to use
	 * @param target the compilation unit node of the ast to rewrite
	 * @param source the compilation unit node of the working copy ast
	 * @param replacements the set of variable binding keys of formal parameters which must be replaced
	 * @param group the text edit group to use
	 */
	protected void createOccurrenceReplacement(final CompilationUnitRange range, final ASTRewrite rewrite, final CompilationUnit target, final CompilationUnit source, final Set replacements, final TextEditGroup group) {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(target);
		Assert.isNotNull(source);
		Assert.isNotNull(range);
		Assert.isNotNull(replacements);
		ASTNode node= null;
		IBinding binding= null;
		node= NodeFinder.perform(source, range.getSourceRange());
		if (node != null) {
			node= ASTNodes.getNormalizedNode(node).getParent();
			if (node instanceof VariableDeclaration) {
				binding= ((VariableDeclaration) node).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof SingleVariableDeclaration) {
					rewrite.replace(((SingleVariableDeclaration) node).getType(), target.getAST().newSimpleType(target.getAST().newSimpleName(fInterfaceName)), group);
					if (node.getParent() instanceof MethodDeclaration) {
						binding= ((VariableDeclaration) node).resolveBinding();
						if (binding != null)
							replacements.add(binding.getKey());
					}
				}
			} else if (node instanceof VariableDeclarationStatement) {
				binding= ((VariableDeclaration) ((VariableDeclarationStatement) node).fragments().get(0)).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof VariableDeclarationFragment)
					rewrite.replace(((VariableDeclarationStatement) node.getParent()).getType(), target.getAST().newSimpleType(target.getAST().newSimpleName(fInterfaceName)), group);
			} else if (node instanceof MethodDeclaration) {
				binding= ((MethodDeclaration) node).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof MethodDeclaration)
					rewrite.replace(((MethodDeclaration) node).getReturnType2(), target.getAST().newSimpleType(target.getAST().newSimpleName(fInterfaceName)), group);
			} else if (node instanceof FieldDeclaration) {
				binding= ((VariableDeclaration) ((FieldDeclaration) node).fragments().get(0)).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof VariableDeclarationFragment) {
					node= node.getParent();
					if (node instanceof FieldDeclaration)
						rewrite.replace(((FieldDeclaration) node).getType(), target.getAST().newSimpleType(target.getAST().newSimpleName(fInterfaceName)), group);
				}
			} else if (node instanceof ArrayType) {
				final ASTNode type= node;
				while (node != null && !(node instanceof MethodDeclaration) && !(node instanceof VariableDeclarationFragment))
					node= node.getParent();
				final int delta= node.getStartPosition() + node.getLength() - type.getStartPosition();
				if (node instanceof MethodDeclaration)
					binding= ((MethodDeclaration) node).resolveBinding();
				else if (node instanceof VariableDeclarationFragment)
					binding= ((VariableDeclarationFragment) node).resolveBinding();
				if (binding != null) {
					node= target.findDeclaringNode(binding.getKey());
					if (node instanceof MethodDeclaration || node instanceof VariableDeclarationFragment) {
						node= NodeFinder.perform(target, node.getStartPosition() + node.getLength() - delta, 0);
						if (node instanceof SimpleName)
							rewrite.replace(node, target.getAST().newSimpleName(fInterfaceName), group);
					}
				}
			} else if (node instanceof QualifiedName) {
				final ASTNode name= node;
				while (node != null && !(node instanceof MethodDeclaration) && !(node instanceof VariableDeclarationFragment))
					node= node.getParent();
				final int delta= node.getStartPosition() + node.getLength() - name.getStartPosition();
				if (node instanceof MethodDeclaration)
					binding= ((MethodDeclaration) node).resolveBinding();
				else if (node instanceof VariableDeclarationFragment)
					binding= ((VariableDeclarationFragment) node).resolveBinding();
				if (binding != null) {
					node= target.findDeclaringNode(binding.getKey());
					if (node instanceof SimpleName || node instanceof MethodDeclaration || node instanceof VariableDeclarationFragment) {
						node= NodeFinder.perform(target, node.getStartPosition() + node.getLength() - delta, 0);
						if (node instanceof SimpleName)
							rewrite.replace(node, target.getAST().newSimpleName(fInterfaceName), group);
					}
				}
			} else if (node instanceof CastExpression) {
				final ASTNode expression= node;
				while (node != null && !(node instanceof MethodDeclaration))
					node= node.getParent();
				final int delta= node.getStartPosition() + node.getLength() - expression.getStartPosition();
				binding= ((MethodDeclaration) node).resolveBinding();
				node= target.findDeclaringNode(binding.getKey());
				if (node instanceof MethodDeclaration) {
					node= NodeFinder.perform(target, node.getStartPosition() + node.getLength() - delta, 0);
					if (node instanceof CastExpression)
						rewrite.replace(((CastExpression) node).getType(), target.getAST().newSimpleType(target.getAST().newSimpleName(fInterfaceName)), group);
				}
			}
		}
	}

	/**
	 * Creates the necessary replace edits to replace all occurrences of the new type with the interface.
	 * 
	 * @param manager the text change manager
	 * @param extracted the working copy of the new interface
	 * @param sourceRewrite the compilation unit source rewrite
	 * @param declaration the type declaration
	 * @param replacements the set of variable binding keys of formal parameters which must be replaced
	 * @param status the refactoring status
	 * @param monitor the progress monitor to display progress
	 * @throws CoreException if an error occurs
	 */
	protected void createOccurrencesReplacements(final TextChangeManager manager, final ICompilationUnit extracted, final CompilationUnitRewrite sourceRewrite, final AbstractTypeDeclaration declaration, final Set replacements, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(manager);
		Assert.isNotNull(extracted);
		Assert.isNotNull(sourceRewrite);
		Assert.isNotNull(declaration);
		Assert.isNotNull(replacements);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 5); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.creating")); //$NON-NLS-1$
			ICompilationUnit type= null;
			try {
				type= WorkingCopyUtil.getNewWorkingCopy(fType.getCompilationUnit(), fOwner, new SubProgressMonitor(monitor, 1));
				final ITextFileBuffer buffer= RefactoringFileBuffers.acquire(fType.getCompilationUnit());
				final ASTRewrite rewrite= sourceRewrite.getASTRewrite();
				try {
					final IDocument document= new Document(buffer.getDocument().get());
					try {
						rewrite.rewriteAST(document, fType.getJavaProject().getOptions(true)).apply(document, TextEdit.UPDATE_REGIONS);
					} catch (MalformedTreeException exception) {
						JavaPlugin.log(exception);
					} catch (BadLocationException exception) {
						JavaPlugin.log(exception);
					}
					type.getBuffer().setContents(document.get());
				} finally {
					RefactoringFileBuffers.release(fType.getCompilationUnit());
				}
				synchronized (type) {
					type.reconcile(ICompilationUnit.NO_AST, false, null, new SubProgressMonitor(monitor, 1));
				}
				if (fInterfaceSource != null) {
					extracted.getBuffer().setContents(fInterfaceSource);
					synchronized (extracted) {
						extracted.reconcile(ICompilationUnit.NO_AST, false, null, new SubProgressMonitor(monitor, 1));
					}
					final CompilationUnitRange[] ranges= ExtractInterfaceUtil.updateReferences(manager, (IType) JavaModelUtil.findInCompilationUnit(type, fType), extracted.getType(fInterfaceName), fOwner, false, new SubProgressMonitor(monitor, 9), status);
					if (!status.hasFatalError()) {
						final RefactoringASTParser parser= new RefactoringASTParser(AST.JLS3);
						final CompilationUnit source= parser.parse(type, true, new SubProgressMonitor(monitor, 1));
						final CompilationUnit target= parser.parse(extracted, true, new SubProgressMonitor(monitor, 1));
						final ASTRewrite rewriter= ASTRewrite.create(target.getAST());
						final TextEditGroup group= sourceRewrite.createGroupDescription(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.update_reference")); //$NON-NLS-1$
						for (int index= 0; index < ranges.length; index++) {
							final CompilationUnitRange range= ranges[index];
							if (range.getCompilationUnit().equals(type))
								createOccurrenceReplacement(range, sourceRewrite.getASTRewrite(), sourceRewrite.getRoot(), source, replacements, group);
							else if (range.getCompilationUnit().equals(extracted))
								createOccurrenceReplacement(range, rewriter, target, target, replacements, group);
						}
						final IDocument document= new Document(fInterfaceSource);
						try {
							rewriter.rewriteAST(document, fType.getJavaProject().getOptions(true)).apply(document, TextEdit.UPDATE_REGIONS);
						} catch (MalformedTreeException exception) {
							JavaPlugin.log(exception);
						} catch (IllegalArgumentException exception) {
							JavaPlugin.log(exception);
						} catch (BadLocationException exception) {
							JavaPlugin.log(exception);
						}
						fInterfaceSource= document.get();
						manager.remove(extracted);
						manager.remove(type);
					}
				}
			} finally {
				if (type != null)
					type.discardWorkingCopy();
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the type parameters of the new interface.
	 * 
	 * @param targetRewrite the target compilation unit rewrite
	 * @param sourceDeclaration the type declaration of the source type
	 * @param targetDeclaration the type declaration of the target type
	 */
	protected void createTypeParameters(final ASTRewrite targetRewrite, final AbstractTypeDeclaration sourceDeclaration, final AbstractTypeDeclaration targetDeclaration) {
		Assert.isNotNull(targetRewrite);
		Assert.isNotNull(sourceDeclaration);
		Assert.isNotNull(targetDeclaration);
		if (sourceDeclaration instanceof TypeDeclaration) {
			TypeParameter parameter= null;
			final ListRewrite rewrite= targetRewrite.getListRewrite(targetDeclaration, TypeDeclaration.TYPE_PARAMETERS_PROPERTY);
			for (final Iterator iterator= ((TypeDeclaration) sourceDeclaration).typeParameters().iterator(); iterator.hasNext();) {
				parameter= (TypeParameter) iterator.next();
				rewrite.insertLast(ASTNode.copySubtree(targetRewrite.getAST(), parameter), null);
				ImportRewriteUtil.collectImports(fType.getJavaProject(), sourceDeclaration, fTypeBindings, fStaticBindings, false);
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
	protected void createTypeSignature(final CompilationUnitRewrite rewrite, final AbstractTypeDeclaration declaration, final RefactoringStatus status, final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(declaration);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		final AST ast= declaration.getAST();
		final ITypeParameter[] parameters= fType.getTypeParameters();
		Type type= ast.newSimpleType(ast.newSimpleName(fInterfaceName));
		if (parameters.length > 0) {
			final ParameterizedType parameterized= ast.newParameterizedType(type);
			for (int index= 0; index < parameters.length; index++)
				parameterized.typeArguments().add(ast.newSimpleType(ast.newSimpleName(parameters[index].getElementName())));
			type= parameterized;
		}
		final TextEditGroup group= rewrite.createGroupDescription(RefactoringCoreMessages.getString("ExtractInterfaceProcessor.change_signature")); //$NON-NLS-1$
		if (declaration instanceof TypeDeclaration)
			rewrite.getASTRewrite().getListRewrite(declaration, TypeDeclaration.SUPER_INTERFACE_TYPES_PROPERTY).insertLast(type, group);
		else if (declaration instanceof EnumDeclaration)
			rewrite.getASTRewrite().getListRewrite(declaration, EnumDeclaration.SUPER_INTERFACE_TYPES_PROPERTY).insertLast(type, group);
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
		return new Object[] { fType};
	}

	/**
	 * Returns the list of extractable members from the type.
	 * 
	 * @return the list of extractable members
	 * @throws JavaModelException if an error occurs
	 */
	public final IMember[] getExtractableMembers() throws JavaModelException {
		final List list= new ArrayList();
		IJavaElement[] children= fType.getChildren();
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
	protected IField[] getExtractedFields(final ICompilationUnit unit) {
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
	protected IMethod[] getExtractedMethods(final ICompilationUnit unit) {
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

	/**
	 * Returns the new interface name.
	 * 
	 * @return the new interface name
	 */
	public final String getInterfaceName() {
		return fInterfaceName;
	}

	/**
	 * Returns the line delimiter to be used for code generation.
	 * 
	 * @return the line delimiter to be used
	 */
	protected String getLineDelimiter() {
		try {
			return StubUtility.getLineDelimiterUsed(fType);
		} catch (JavaModelException exception) {
			return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#getProcessorName()
	 */
	public final String getProcessorName() {
		return RefactoringCoreMessages.getFormattedString("ExtractInterfaceProcessor.name", new String[] { fType.getElementName()}); //$NON-NLS-1$
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
		return fType;
	}

	/**
	 * Should override annotations be generated?
	 * 
	 * @return <code>true</code> if annotations should be generated, <code>false</code> otherwise
	 */
	public final boolean isAnnotations() {
		return fAnnotations;
	}

	/*
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#isApplicable()
	 */
	public final boolean isApplicable() throws CoreException {
		return Checks.isAvailable(fType) && !fType.isBinary() && !fType.isReadOnly() && !fType.isAnnotation();
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
	 * @see org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor#loadParticipants(org.eclipse.ltk.core.refactoring.RefactoringStatus, org.eclipse.ltk.core.refactoring.participants.SharableParticipants)
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
	protected String normalizeText(final String code) throws JavaModelException {
		Assert.isNotNull(code);
		final String[] lines= Strings.convertIntoLines(code);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(fType.getJavaProject()), false);
		return Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(fType));
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
	 * Determines whether override annotations should be generated.
	 * 
	 * @param annotations <code>true</code> to generate override annotations, <code>false</code> otherwise
	 */
	public final void setAnnotations(final boolean annotations) {
		fAnnotations= annotations;
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
	 * Sets the new interface name.
	 * 
	 * @param name the new interface name
	 */
	public final void setInterfaceName(final String name) {
		Assert.isNotNull(name);
		fInterfaceName= name;
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
}
