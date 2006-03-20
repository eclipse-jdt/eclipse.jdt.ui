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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.formatter.CodeFormatter;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

import org.eclipse.jdt.ui.CodeGeneration;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Refactoring processor for the extract supertype refactoring.
 * 
 * @since 3.2
 */
public final class ExtractSupertypeProcessor extends PullUpRefactoringProcessor {

	/** The extract supertype group category set */
	private static final GroupCategorySet SET_EXTRACT_SUPERTYPE= new GroupCategorySet(new GroupCategory("org.eclipse.jdt.internal.corext.extractSupertype", //$NON-NLS-1$
			RefactoringCoreMessages.ExtractSupertypeProcessor_category_name, RefactoringCoreMessages.ExtractSupertypeProcessor_category_description));

	/** The possible extract supertype candidates, or the empty array */
	private IType[] fPossibleCandidates= {};

	/** The name of the extracted type */
	private String fTypeName= "Test";

	/** The types where to extract the supertype */
	private IType[] fTypesToExtract= {};

	/** The working copies (working copy owner is <code>fOwner</code>) */
	private Set fWorkingCopies= new HashSet(8);

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
	 * {@inheritDoc}
	 */
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor, final CheckConditionsContext context) throws CoreException, OperationCanceledException {
		final RefactoringStatus status= new RefactoringStatus();
		try {
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.ExtractSupertypeProcessor_checking);
			status.merge(Checks.checkCompilationUnitNewName(getDeclaringType().getCompilationUnit(), fTypeName));
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
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		// TODO: implement
		return super.createChange(monitor);
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
			ICompilationUnit copy= null;
			try {
				final IType declaring= getDeclaringType();
				final CompilationUnitRewrite declaringRewrite= new CompilationUnitRewrite(declaring.getCompilationUnit());
				final AbstractTypeDeclaration subDeclaration= ASTNodeSearchUtil.getAbstractTypeDeclarationNode(declaring, declaringRewrite.getRoot());
				if (subDeclaration != null) {
					copy= declaring.getPackageFragment().getCompilationUnit(JavaModelUtil.getRenamedCUName(declaring.getCompilationUnit(), fTypeName)).getWorkingCopy(fOwner, null, new SubProgressMonitor(monitor, 10));
					final String source= createTypeSource(copy, destinationType, subDeclaration, status, new SubProgressMonitor(monitor, 10));
					if (source != null) {
						copy.getBuffer().setContents(source);
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
	protected final RefactoringStatus createWorkingCopyLayer(final IProgressMonitor monitor) {
		Assert.isNotNull(monitor);
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
			monitor.done();
		}
		return status;
	}

	/**
	 * Returns the possible candidates where a supertype can be extracted.
	 * <p>
	 * This includes the declaring type.
	 * </p>
	 * 
	 * @return the array of candidates, or the empty array
	 */
	public IType[] getPossibleCandidates(final IProgressMonitor monitor) {
		Assert.isNotNull(monitor);
		if (fPossibleCandidates == null || fPossibleCandidates.length == 0) {
			final IType declaring= getDeclaringType();
			if (declaring != null) {
				try {
					monitor.beginTask(RefactoringCoreMessages.ExtractSupertypeProcessor_computing_possible_types, 10);
					final IType superType= getDeclaringSuperTypeHierarchy(new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).getSuperclass(declaring);
					if (superType != null) {
						fPossibleCandidates= superType.newTypeHierarchy(fOwner, new SubProgressMonitor(monitor, 9, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL)).getSubtypes(superType);
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
	 * Returns the types to extract.
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
		// TODO: implement
		return super.initialize(arguments);
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