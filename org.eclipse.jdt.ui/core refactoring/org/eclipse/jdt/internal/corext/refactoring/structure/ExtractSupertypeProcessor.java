/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.osgi.util.NLS;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringArguments;
import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Refactoring processor for the extract supertype refactoring.
 * 
 * @since 3.2
 */
public final class ExtractSupertypeProcessor extends PullUpRefactoringProcessor {

	/** The types attribute */
	private static final String ATTRIBUTE_TYPES= "types"; //$NON-NLS-1$

	/** The id of the refactoring */
	private static final String ID_EXTRACT_SUPERTYPE= "org.eclipse.jdt.ui.extract.supertype"; //$NON-NLS-1$

	/** The extract supertype group category set */
	private static final GroupCategorySet SET_EXTRACT_SUPERTYPE= new GroupCategorySet(new GroupCategory("org.eclipse.jdt.internal.corext.extractSupertype", //$NON-NLS-1$
			RefactoringCoreMessages.ExtractSupertypeProcessor_category_name, RefactoringCoreMessages.ExtractSupertypeProcessor_category_description));

	/** The possible extract supertype candidates, or the empty array */
	private IType[] fPossibleCandidates= {};

	/** The source of the supertype */
	private String fSuperSource;

	/** The name of the extracted type */
	private String fTypeName= ""; //$NON-NLS-1$

	/** The types where to extract the supertype */
	private IType[] fTypesToExtract= {};

	/** The working copies (working copy owner is <code>fOwner</code>) */
	private Set fWorkingCopies= new HashSet(8);

	/** Have the working copies already been created? */
	private boolean fWorkingCopiesCreated= false;

	/**
	 * Creates a new extract supertype refactoring processor.
	 * 
	 * @param members
	 *            the members to extract, or <code>null</code> if invoked by
	 *            scripting
	 * @param settings
	 *            the code generation settings, or <code>null</code> if
	 *            invoked by scripting
	 */
	public ExtractSupertypeProcessor(final IMember[] members, final CodeGenerationSettings settings) {
		super(members, settings);
	}

	/**
	 * {@inheritDoc}
	 */
	protected final RefactoringStatus checkDeclaringSuperTypes(final IProgressMonitor monitor) throws JavaModelException {
		return new RefactoringStatus();
	}

	/**
	 * Checks whether the compilation unit to be extracted is valid.
	 * 
	 * @return a status describing the outcome of the
	 */
	public RefactoringStatus checkExtractedCompilationUnit() {
		final RefactoringStatus status= new RefactoringStatus();
		final ICompilationUnit cu= getDeclaringType().getCompilationUnit();
		if (fTypeName == null || "".equals(fTypeName)) //$NON-NLS-1$
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.Checks_Choose_name);
		status.merge(Checks.checkCompilationUnitName(JavaModelUtil.getRenamedCUName(cu, fTypeName)));
		if (status.hasFatalError())
			return status;
		status.merge(Checks.checkCompilationUnitNewName(cu, fTypeName));
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context) throws CoreException, OperationCanceledException {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractSupertypeProcessor_checking);
			status.merge(checkExtractedCompilationUnit());
			if (status.hasFatalError())
				return status;
			return super.checkFinalConditions(new SubProgressMonitor(monitor, 1), context);
		} finally {
			monitor.done();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected final void clearCaches() {
		super.clearCaches();
		try {
			for (final Iterator iterator= fWorkingCopies.iterator(); iterator.hasNext();) {
				final ICompilationUnit unit= (ICompilationUnit) iterator.next();
				try {
					unit.discardWorkingCopy();
				} catch (JavaModelException exception) {
					JavaPlugin.log(exception);
				}
			}
		} finally {
			fWorkingCopies.clear();
			fWorkingCopiesCreated= false;
		}
	}

	/**
	 * Computes the destination type based on the new name.
	 * 
	 * @return the destination type
	 */
	public IType computeDestinationType(final String name) {
		if (name != null && !name.equals("")) {//$NON-NLS-1$
			final IType declaring= getDeclaringType();
			final IPackageFragment fragment= declaring.getPackageFragment();
			final ICompilationUnit unit= fragment.getCompilationUnit(JavaModelUtil.getRenamedCUName(declaring.getCompilationUnit(), name));
			return unit.getType(name);
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		try {
			final CompositeChange change= new DynamicValidationStateChange(RefactoringCoreMessages.ExtractSupertypeProcessor_extract_supertype, fChangeManager.getAllChanges()) {

				public final ChangeDescriptor getDescriptor() {
					final Map arguments= new HashMap();
					String project= null;
					final IType declaring= getDeclaringType();
					final IJavaProject javaProject= declaring.getJavaProject();
					if (javaProject != null)
						project= javaProject.getElementName();
					int flags= JavaRefactoringDescriptor.JAR_IMPORTABLE | JavaRefactoringDescriptor.JAR_REFACTORABLE | RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE;
					try {
						if (declaring.isLocal() || declaring.isAnonymous())
							flags|= JavaRefactoringDescriptor.JAR_SOURCE_ATTACHMENT;
					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
					}
					final JavaRefactoringDescriptor descriptor= new JavaRefactoringDescriptor(ID_EXTRACT_SUPERTYPE, project, NLS.bind(RefactoringCoreMessages.ExtractSupertypeProcessor_descriptor_description, JavaElementLabels.getElementLabel(fDestinationType, JavaElementLabels.ALL_DEFAULT), JavaElementLabels.getElementLabel(fDeclaringType, JavaElementLabels.ALL_FULLY_QUALIFIED)), getComment(), arguments, flags);
					arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_INPUT, descriptor.elementToHandle(fDestinationType));
					arguments.put(ATTRIBUTE_REPLACE, Boolean.valueOf(fReplace).toString());
					arguments.put(ATTRIBUTE_INSTANCEOF, Boolean.valueOf(fInstanceOf).toString());
					if (fDeclaringType != null)
						arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + 1, descriptor.elementToHandle(fDeclaringType));
					arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_NAME, fTypeName);
					arguments.put(ATTRIBUTE_PULL, new Integer(fMembersToMove.length).toString());
					for (int offset= 0; offset < fMembersToMove.length; offset++)
						arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + 2), descriptor.elementToHandle(fMembersToMove[offset]));
					arguments.put(ATTRIBUTE_DELETE, new Integer(fDeletedMethods.length).toString());
					for (int offset= 0; offset < fDeletedMethods.length; offset++)
						arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fMembersToMove.length + 2), descriptor.elementToHandle(fDeletedMethods[offset]));
					arguments.put(ATTRIBUTE_ABSTRACT, new Integer(fAbstractMethods.length).toString());
					for (int offset= 0; offset < fAbstractMethods.length; offset++)
						arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fMembersToMove.length + fDeletedMethods.length + 2), descriptor.elementToHandle(fAbstractMethods[offset]));
					arguments.put(ATTRIBUTE_TYPES, new Integer(fTypesToExtract.length).toString());
					for (int offset= 0; offset < fTypesToExtract.length; offset++)
						arguments.put(JavaRefactoringDescriptor.ATTRIBUTE_ELEMENT + (offset + fMembersToMove.length + fDeletedMethods.length + fAbstractMethods.length + 2), descriptor.elementToHandle(fTypesToExtract[offset]));
					arguments.put(ATTRIBUTE_STUBS, Boolean.valueOf(fCreateMethodStubs).toString());
					return new RefactoringChangeDescriptor(descriptor);
				}
			};
			final IType declaring= getDeclaringType();
			final IFile file= ResourceUtil.getFile(declaring.getCompilationUnit());
			if (fSuperSource != null && fSuperSource.length() > 0)
				change.add(new CreateTextFileChange(file.getFullPath().removeLastSegments(1).append(JavaModelUtil.getRenamedCUName(declaring.getCompilationUnit(), fTypeName)), fSuperSource, file.getCharset(false), "java")); //$NON-NLS-1$
			return change;
		} finally {
			monitor.done();
			clearCaches();
		}
	}

	/**
	 * Creates the new extracted supertype.
	 * 
	 * @param destinationType
	 *            the destination type, or <code>null</code> if no destination
	 *            type is available
	 * @param monitor
	 *            the progress monitor
	 * @return a status describing the outcome of the operation
	 * @throws CoreException
	 *             if an error occurs
	 */
	protected final RefactoringStatus createExtractedSuperType(final IType destinationType, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(monitor);
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask(RefactoringCoreMessages.ExtractSupertypeProcessor_checking, 20);
			fSuperSource= null;
			ICompilationUnit copy= null;
			try {
				final IType declaring= getDeclaringType();
				final CompilationUnitRewrite declaringRewrite= new CompilationUnitRewrite(declaring.getCompilationUnit());
				final AbstractTypeDeclaration subDeclaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(declaring, declaringRewrite.getRoot());
				if (subDeclaration != null) {
					copy= declaring.getPackageFragment().getCompilationUnit(JavaModelUtil.getRenamedCUName(declaring.getCompilationUnit(), fTypeName)).getWorkingCopy(fOwner, null, new SubProgressMonitor(monitor, 10));
					fSuperSource= createTypeSource(copy, destinationType, subDeclaration, status, new SubProgressMonitor(monitor, 10));
					if (fSuperSource != null) {
						copy.getBuffer().setContents(fSuperSource);
						JavaModelUtil.reconcile(copy);
					}
				}
			} finally {
				fWorkingCopies.add(copy);
			}
		} finally {
			monitor.done();
		}
		return status;
	}

	/**
	 * Creates the declaration of the new supertype, excluding any comments or
	 * package declaration.
	 * 
	 * @param copy
	 *            the working copy of the new supertype
	 * @param destinationType
	 *            the destination type, or <code>null</code> if no destination
	 *            type is available
	 * @param buffer
	 *            the string buffer containing the declaration
	 * @param status
	 *            the refactoring status
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             if an error occurs
	 */
	protected final void createTypeDeclaration(final ICompilationUnit copy, final IType destinationType, final AbstractTypeDeclaration subDeclaration, final StringBuffer buffer, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(copy);
		Assert.isNotNull(subDeclaration);
		Assert.isNotNull(buffer);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractSupertypeProcessor_checking);
			final String delimiter= StubUtility.getLineDelimiterUsed(copy.getJavaProject());
			buffer.append(JdtFlags.VISIBILITY_STRING_PUBLIC);
			buffer.append(" "); //$NON-NLS-1$
			buffer.append("class "); //$NON-NLS-1$
			buffer.append(fTypeName);
			buffer.append(" {"); //$NON-NLS-1$
			buffer.append(delimiter);
			buffer.append(delimiter);
			buffer.append('}');
			final IDocument document= new Document(buffer.toString());
			final ASTParser parser= ASTParser.newParser(AST.JLS3);
			parser.setSource(document.get().toCharArray());
			final CompilationUnit unit= (CompilationUnit) parser.createAST(new SubProgressMonitor(monitor, 1));
			final ASTRewrite targetRewrite= ASTRewrite.create(unit.getAST());
			createTypeParameters(targetRewrite, destinationType, subDeclaration, (AbstractTypeDeclaration) unit.types().get(0));
			final TextEdit edit= targetRewrite.rewriteAST(document, copy.getJavaProject().getOptions(true));
			try {
				edit.apply(document, TextEdit.UPDATE_REGIONS);
			} catch (MalformedTreeException exception) {
				JavaPlugin.log(exception);
			} catch (BadLocationException exception) {
				JavaPlugin.log(exception);
			}
			buffer.setLength(0);
			buffer.append(document.get());
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates a new type signature of a subtype.
	 * 
	 * @param subRewrite
	 *            the compilation unit rewrite of a subtype
	 * @param declaration
	 *            the type declaration of a subtype
	 * @param superName
	 *            the supertype name
	 * @param status
	 *            the refactoring status
	 * @param monitor
	 *            the progress monitor to use
	 * @throws JavaModelException
	 *             if the type parameters cannot be retrieved
	 */
	protected final void createTypeSignature(final CompilationUnitRewrite subRewrite, final AbstractTypeDeclaration declaration, final String superName, final RefactoringStatus status, final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(subRewrite);
		Assert.isNotNull(declaration);
		Assert.isNotNull(superName);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask(RefactoringCoreMessages.ExtractSupertypeProcessor_checking, 10);
			final AST ast= declaration.getAST();
			Type type= ast.newSimpleType(ast.newSimpleName(superName));
			final IType declaringSuperType= getDeclaringSuperTypeHierarchy(new SubProgressMonitor(monitor, 10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).getSuperclass(getDeclaringType());
			if (declaringSuperType != null) {
				final ITypeParameter[] parameters= declaringSuperType.getTypeParameters();
				if (parameters.length > 0) {
					final ParameterizedType parameterized= ast.newParameterizedType(type);
					for (int index= 0; index < parameters.length; index++)
						parameterized.typeArguments().add(ast.newSimpleType(ast.newSimpleName(parameters[index].getElementName())));
					type= parameterized;
				}
			}
			final ASTRewrite rewriter= subRewrite.getASTRewrite();
			if (declaration instanceof TypeDeclaration) {
				final TypeDeclaration extended= (TypeDeclaration) declaration;
				final Type superClass= extended.getSuperclassType();
				if (superClass != null)
					rewriter.replace(superClass, type, subRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.ExtractSupertypeProcessor_add_supertype, SET_EXTRACT_SUPERTYPE));
				else
					rewriter.set(extended, TypeDeclaration.SUPERCLASS_TYPE_PROPERTY, type, subRewrite.createCategorizedGroupDescription(RefactoringCoreMessages.ExtractSupertypeProcessor_add_supertype, SET_EXTRACT_SUPERTYPE));
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Creates the source for the new compilation unit containing the supertype.
	 * 
	 * @param copy
	 *            the working copy of the new supertype
	 * @param destinationType
	 *            the destination type, or <code>null</code> if no destination
	 *            type is available
	 * @param subDeclaration
	 *            the declaration of the subtype
	 * @param status
	 *            the refactoring status
	 * @param monitor
	 *            the progress monitor to display progress
	 * @return the source of the new compilation unit, or <code>null</code>
	 * @throws CoreException
	 *             if an error occurs
	 */
	protected final String createTypeSource(final ICompilationUnit copy, final IType destinationType, final AbstractTypeDeclaration subDeclaration, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		Assert.isNotNull(copy);
		Assert.isNotNull(subDeclaration);
		Assert.isNotNull(status);
		Assert.isNotNull(monitor);
		String source= null;
		try {
			monitor.beginTask("", 2); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractSupertypeProcessor_checking);
			final IType declaring= getDeclaringType();
			final String delimiter= StubUtility.getLineDelimiterUsed(copy.getJavaProject());
			String typeComment= null;
			String fileComment= null;
			if (fSettings.createComments) {
				final ITypeParameter[] parameters= declaring.getTypeParameters();
				final String[] names= new String[parameters.length];
				for (int index= 0; index < parameters.length; index++)
					names[index]= parameters[index].getElementName();
				typeComment= CodeGeneration.getTypeComment(copy, fTypeName, names, delimiter);
				fileComment= CodeGeneration.getFileComment(copy, delimiter);
			}
			final StringBuffer buffer= new StringBuffer(64);
			createTypeDeclaration(copy, destinationType, subDeclaration, buffer, status, new SubProgressMonitor(monitor, 1));
			final String imports= createTypeImports(copy, monitor);
			source= createTypeTemplate(copy, imports, fileComment, typeComment, buffer.toString());
			if (source == null) {
				if (!declaring.getPackageFragment().isDefaultPackage()) {
					if (imports.length() > 0)
						buffer.insert(0, imports);
					buffer.insert(0, "package " + declaring.getPackageFragment().getElementName() + ";"); //$NON-NLS-1$//$NON-NLS-2$
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
	 * {@inheritDoc}
	 */
	public final RefactoringStatus createWorkingCopyLayer(final IProgressMonitor monitor) {
		Assert.isNotNull(monitor);
		if (!fWorkingCopiesCreated) {
			final RefactoringStatus status= new RefactoringStatus();
			try {
				monitor.beginTask(RefactoringCoreMessages.ExtractSupertypeProcessor_checking, 20);
				final IType declaring= getDeclaringType();
				status.merge(createExtractedSuperType(getDeclaringSuperTypeHierarchy(new SubProgressMonitor(monitor, 10)).getSuperclass(declaring), new SubProgressMonitor(monitor, 10)));
				if (status.hasFatalError())
					return status;
				final Set units= new HashSet();
				for (int index= 0; index < fTypesToExtract.length; index++) {
					units.add(fTypesToExtract[index].getCompilationUnit());
				}
				units.add(declaring.getCompilationUnit());
				final Map projects= new HashMap();
				Collection collection= null;
				IJavaProject project= null;
				ICompilationUnit current= null;
				for (final Iterator iterator= units.iterator(); iterator.hasNext();) {
					current= (ICompilationUnit) iterator.next();
					project= current.getJavaProject();
					collection= (Collection) projects.get(project);
					if (collection == null) {
						collection= new ArrayList();
						projects.put(project, collection);
					}
					collection.add(current);
				}

				// TODO: implement

			} catch (CoreException exception) {
				JavaPlugin.log(exception);
				status.merge(RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.ExtractSupertypeProcessor_unexpected_exception_on_layer));
			} finally {
				fWorkingCopiesCreated= true;
				monitor.done();
			}
			return status;
		}
		return new RefactoringStatus();
	}

	/**
	 * {@inheritDoc}
	 */
	public IType[] getCandidateTypes(final RefactoringStatus status, final IProgressMonitor monitor) {
		Assert.isNotNull(monitor);
		if (fPossibleCandidates == null || fPossibleCandidates.length == 0) {
			final IType declaring= getDeclaringType();
			if (declaring != null) {
				try {
					monitor.beginTask(RefactoringCoreMessages.ExtractSupertypeProcessor_computing_possible_types, 10);
					final IType superType= getDeclaringSuperTypeHierarchy(new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).getSuperclass(declaring);
					if (superType != null) {
						fPossibleCandidates= superType.newTypeHierarchy(fOwner, new SubProgressMonitor(monitor, 9, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).getSubtypes(superType);
						final LinkedList list= new LinkedList(Arrays.asList(fPossibleCandidates));
						for (final Iterator iterator= list.iterator(); iterator.hasNext();) {
							final IType type= (IType) iterator.next();
							if (type.isReadOnly() || type.isBinary() || type.isAnonymous() || !type.isClass())
								iterator.remove();
						}
						fPossibleCandidates= (IType[]) list.toArray(new IType[list.size()]);
					}
				} catch (JavaModelException exception) {
					JavaPlugin.log(exception);
				} finally {
					monitor.done();
				}
			}
		}
		return fPossibleCandidates;
	}

	/**
	 * Returns the type name.
	 * 
	 * @return the type name
	 */
	public String getTypeName() {
		return fTypeName;
	}

	/**
	 * Returns the types to extract. The declaring type may or may not be
	 * contained in the result.
	 * 
	 * @return the types to extract
	 */
	public IType[] getTypesToExtract() {
		return fTypesToExtract;
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		if (arguments instanceof JavaRefactoringArguments) {
			final JavaRefactoringArguments extended= (JavaRefactoringArguments) arguments;

			// TODO: implement
		} else
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.InitializableRefactoring_inacceptable_arguments);
		return new RefactoringStatus();
	}

	/**
	 * {@inheritDoc}
	 */
	protected void rewriteTypeOccurrences(final TextChangeManager manager, final CompilationUnitRewrite sourceRewrite, final ICompilationUnit copy, final Set replacements, final RefactoringStatus status, final IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 20); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractSupertypeProcessor_checking);
			try {
				final IType declaring= getDeclaringType();
				final ICompilationUnit destinationUnit= getDestinationType().getCompilationUnit();
				final IJavaProject project= declaring.getJavaProject();
				final ASTParser parser= ASTParser.newParser(AST.JLS3);
				parser.setWorkingCopyOwner(fOwner);
				parser.setResolveBindings(true);
				parser.setProject(project);
				parser.setCompilerOptions(RefactoringASTParser.getCompilerOptions(project));
				parser.createASTs(new ICompilationUnit[] { copy}, new String[0], new ASTRequestor() {

					public final void acceptAST(final ICompilationUnit unit, final CompilationUnit node) {
						try {
							final IType subType= (IType) JavaModelUtil.findInCompilationUnit(unit, declaring);
							final AbstractTypeDeclaration subDeclaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(subType, node);
							if (subDeclaration != null) {
								final ITypeBinding subBinding= subDeclaration.resolveBinding();
								if (subBinding != null) {
									String name= null;
									ITypeBinding superBinding= null;
									final ITypeBinding[] superBindings= Bindings.getAllSuperTypes(subBinding);
									for (int index= 0; index < superBindings.length; index++) {
										name= superBindings[index].getName();
										if (name.startsWith(fDestinationType.getElementName()))
											superBinding= superBindings[index];
									}
									if (superBinding != null) {
										solveSuperTypeConstraints(unit, node, subType, subBinding, superBinding, new SubProgressMonitor(monitor, 14), status);
										if (!status.hasFatalError())
											rewriteTypeOccurrences(manager, this, sourceRewrite, unit, node, replacements, status, new SubProgressMonitor(monitor, 3));
										if (manager.containsChangesIn(destinationUnit)) {
											final TextEdit edit= manager.get(destinationUnit).getEdit();
											if (edit != null) {
												final IDocument document= new Document(destinationUnit.getBuffer().getContents());
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
												manager.remove(destinationUnit);
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

			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Sets the type name.
	 * 
	 * @param name
	 *            the type name
	 */
	public void setTypeName(final String name) {
		Assert.isNotNull(name);
		fTypeName= name;
	}

	/**
	 * Sets the types to extract. Must be a subset of
	 * <code>getPossibleCandidates()</code>. If the declaring type is not
	 * contained, it will automatically be added.
	 * 
	 * @param types
	 *            the types to extract
	 */
	public void setTypesToExtract(final IType[] types) {
		Assert.isNotNull(types);
		fTypesToExtract= types;
	}
}