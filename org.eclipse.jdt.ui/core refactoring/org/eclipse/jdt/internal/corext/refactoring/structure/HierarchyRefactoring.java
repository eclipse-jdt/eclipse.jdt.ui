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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.reorg.SourceReferenceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.TextChangeManager;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.corext.util.WorkingCopyUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.ltk.core.refactoring.TextChange;

/**
 * Partial implementation of a refactoring executed on type hierarchies.
 */
public abstract class HierarchyRefactoring extends Refactoring {

	/**
	 * AST node visitor which performs the actual mapping.
	 */
	public static final class MethodMapper extends TypeMapper {

		/** Are we in an anonymous class declaration? */
		protected boolean fAnonymousClassDeclaration= false;

		/** The qualified type name where the super references are referring to */
		protected final String fQualifiedName;

		/** Are we in a type declaration statement? */
		protected boolean fTypeDeclarationStatement= false;

		/**
		 * Creates a new method mapper.
		 * 
		 * @param rewrite
		 *        The AST rewrite to use
		 * @param type
		 *        the super reference type
		 * @param mapping
		 *        the type variable mapping
		 */
		public MethodMapper(final ASTRewrite rewrite, final IType type, final TypeVariableMaplet[] mapping) {
			super(rewrite, mapping);
			Assert.isNotNull(type);
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
				return true;
			}
			return false;
		}

		public final boolean visit(final SuperMethodInvocation node) {
			if (!fAnonymousClassDeclaration && !fTypeDeclarationStatement) {
				final IBinding name= node.getName().resolveBinding();
				if (name != null && name.getKind() == IBinding.METHOD) {
					final ITypeBinding type= ((IMethodBinding) name).getDeclaringClass();
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
					final ListRewrite rewrite= fRewrite.getListRewrite(invocation, MethodInvocation.ARGUMENTS_PROPERTY);
					rewrite.insertLast(rewrite.createCopyTarget((ASTNode) arguments.get(0), (ASTNode) arguments.get(arguments.size() - 1)), null);
				}
				fRewrite.replace(node, invocation, null);
				return true;
			}
			return false;
		}

		public final boolean visit(final TypeDeclarationStatement node) {
			fTypeDeclarationStatement= true;
			return super.visit(node);
		}
	}

	/**
	 * AST node visitor which performs the actual mapping.
	 */
	public static class TypeMapper extends ASTVisitor {

		/** The type variable mapping to use */
		protected final TypeVariableMaplet[] fMapping;

		/** The AST rewrite to use */
		protected final ASTRewrite fRewrite;

		/**
		 * Creates a new type mapper.
		 * 
		 * @param rewrite
		 *        The AST rewrite to use
		 * @param mapping
		 *        The type variable mapping to use
		 */
		public TypeMapper(final ASTRewrite rewrite, final TypeVariableMaplet[] mapping) {
			Assert.isNotNull(rewrite);
			Assert.isNotNull(mapping);
			fRewrite= rewrite;
			fMapping= mapping;
		}

		public final boolean visit(final SimpleName node) {
			final ITypeBinding binding= node.resolveTypeBinding();
			if (binding != null && binding.isTypeVariable()) {
				String name= null;
				for (int index= 0; index < fMapping.length; index++) {
					name= binding.getName();
					if (fMapping[index].getSourceName().equals(name) && node.getIdentifier().equals(name))
						fRewrite.set(node, SimpleName.IDENTIFIER_PROPERTY, fMapping[index].getTargetName(), null);
				}
			}
			return true;
		}
	}

	protected static boolean areAllFragmentsDeleted(final FieldDeclaration declaration, final List declarationNodes) {
		for (final Iterator iterator= declaration.fragments().iterator(); iterator.hasNext();) {
			if (!declarationNodes.contains(iterator.next()))
				return false;
		}
		return true;
	}

	protected static RefactoringStatus checkCallsToClassConstructors(final IType type, final IProgressMonitor monitor) throws JavaModelException {
		final RefactoringStatus result= new RefactoringStatus();
		final SearchResultGroup[] groups= ConstructorReferenceFinder.getConstructorReferences(type, monitor, result);
		final String message= RefactoringCoreMessages.getFormattedString("HierarchyRefactoring.gets_instantiated", new Object[] { createTypeLabel(type)}); //$NON-NLS-1$

		ICompilationUnit unit= null;
		for (int index= 0; index < groups.length; index++) {
			unit= groups[index].getCompilationUnit();
			if (unit != null) {
				final CompilationUnit cuNode= new RefactoringASTParser(AST.JLS3).parse(unit, false);
				final ASTNode[] references= ASTNodeSearchUtil.getAstNodes(groups[index].getSearchResults(), cuNode);
				ASTNode node= null;
				for (int offset= 0; offset < references.length; offset++) {
					node= references[offset];
					if ((node instanceof ClassInstanceCreation) || ConstructorReferenceFinder.isImplicitConstructorReferenceNodeInClassCreations(node)) {
						RefactoringStatusContext context= JavaStatusContext.create(unit, node);
						result.addError(message, context);
					}
				}
			}
		}
		monitor.done();
		return result;
	}

	protected static void copyJavadocNode(final ASTRewrite rewrite, final IMember member, final BodyDeclaration oldDeclaration, final BodyDeclaration newDeclaration) throws JavaModelException {
		final Javadoc doc= oldDeclaration.getJavadoc();
		if (doc != null)
			newDeclaration.setJavadoc((Javadoc) ASTNode.copySubtree(rewrite.getAST(), doc));
	}

	protected static String createFieldLabel(final IField field) {
		return field.getElementName();
	}

	protected static String createLabel(final IMember member) {
		if (member instanceof IType)
			return createTypeLabel((IType) member);
		else if (member instanceof IMethod)
			return createMethodLabel((IMethod) member);
		else if (member instanceof IField)
			return createFieldLabel((IField) member);
		else if (member instanceof IInitializer)
			return RefactoringCoreMessages.getString("HierarchyRefactoring.initializer"); //$NON-NLS-1$
		Assert.isTrue(false);
		return null;
	}

	protected static String createMethodLabel(final IMethod method) {
		return JavaElementUtil.createMethodSignature(method);
	}

	protected static Expression createPlaceholderForExpression(final Expression expression, final ICompilationUnit declaringCu, final ASTRewrite rewrite) throws JavaModelException {
		return (Expression) rewrite.createStringPlaceholder(getBufferText(expression, declaringCu), ASTNode.METHOD_INVOCATION);
	}

	protected static Expression createPlaceholderForExpression(final Expression expression, final ICompilationUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite) throws JavaModelException {
		Expression result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(expression.getAST());
			final ITrackedNodePosition position= rewriter.track(expression);
			expression.accept(new TypeMapper(rewriter, mapping));
			rewriter.rewriteAST(document, null).apply(document, TextEdit.NONE);
			result= (Expression) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.METHOD_INVOCATION);
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		return result;
	}

	protected static BodyDeclaration createPlaceholderForProtectedTypeDeclaration(final BodyDeclaration bodyDeclaration, final CompilationUnit declaringCuNode, final ICompilationUnit declaringCu, final ASTRewrite rewrite, final boolean removeIndentation) throws JavaModelException {
		String text= null;
		try {
			final ASTRewrite rewriter= ASTRewrite.create(bodyDeclaration.getAST());
			ModifierRewrite.create(rewriter, bodyDeclaration).setVisibility(Modifier.PROTECTED, null);
			final ITrackedNodePosition position= rewriter.track(bodyDeclaration);
			final IDocument document= new Document(getBufferText(declaringCuNode, declaringCu));
			rewriter.rewriteAST(document, null).apply(document, TextEdit.UPDATE_REGIONS);
			text= document.get(position.getStartPosition(), position.getLength());
		} catch (BadLocationException exception) {
			text= getNewText(bodyDeclaration, declaringCu, removeIndentation);
		}
		return (BodyDeclaration) rewrite.createStringPlaceholder(text, ASTNode.TYPE_DECLARATION);
	}

	protected static BodyDeclaration createPlaceholderForProtectedTypeDeclaration(final BodyDeclaration bodyDeclaration, final CompilationUnit declaringCuNode, final ICompilationUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite, final boolean removeIndentation) throws JavaModelException {
		BodyDeclaration result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(bodyDeclaration.getAST());
			final ITrackedNodePosition position= rewriter.track(bodyDeclaration);
			bodyDeclaration.accept(new TypeMapper(rewriter, mapping) {

				public final boolean visit(final TypeDeclaration node) {
					ModifierRewrite.create(fRewrite, bodyDeclaration).setVisibility(Modifier.PROTECTED, null);
					return true;
				}
			});
			rewriter.rewriteAST(document, null).apply(document, TextEdit.NONE);
			result= (BodyDeclaration) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.TYPE_DECLARATION);
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		return result;
	}

	protected static SingleVariableDeclaration createPlaceholderForSingleVariableDeclaration(final SingleVariableDeclaration declaration, final ICompilationUnit declaringCu, final ASTRewrite rewrite) throws JavaModelException {
		return (SingleVariableDeclaration) rewrite.createStringPlaceholder(getBufferText(declaration, declaringCu), ASTNode.SINGLE_VARIABLE_DECLARATION);
	}

	protected static SingleVariableDeclaration createPlaceholderForSingleVariableDeclaration(final SingleVariableDeclaration declaration, final ICompilationUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite) throws JavaModelException {
		SingleVariableDeclaration result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(declaration.getAST());
			final ITrackedNodePosition position= rewriter.track(declaration);
			declaration.accept(new TypeMapper(rewriter, mapping));
			rewriter.rewriteAST(document, null).apply(document, TextEdit.NONE);
			result= (SingleVariableDeclaration) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.SINGLE_VARIABLE_DECLARATION);
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		return result;
	}

	protected static Type createPlaceholderForType(final Type type, final ICompilationUnit declaringCu, final ASTRewrite rewrite) throws JavaModelException {
		return (Type) rewrite.createStringPlaceholder(getBufferText(type, declaringCu), ASTNode.SIMPLE_TYPE);
	}

	protected static Type createPlaceholderForType(final Type type, final ICompilationUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite) throws JavaModelException {
		Type result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(type.getAST());
			final ITrackedNodePosition position= rewriter.track(type);
			type.accept(new TypeMapper(rewriter, mapping));
			rewriter.rewriteAST(document, null).apply(document, TextEdit.NONE);
			result= (Type) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.SIMPLE_TYPE);
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		return result;
	}

	protected static BodyDeclaration createPlaceholderForTypeDeclaration(final BodyDeclaration bodyDeclaration, final ICompilationUnit declaringCu, final ASTRewrite rewrite, final boolean removeIndentation) throws JavaModelException {
		return (BodyDeclaration) rewrite.createStringPlaceholder(getNewText(bodyDeclaration, declaringCu, removeIndentation), ASTNode.TYPE_DECLARATION);
	}

	protected static BodyDeclaration createPlaceholderForTypeDeclaration(final BodyDeclaration bodyDeclaration, final ICompilationUnit declaringCu, final TypeVariableMaplet[] mapping, final ASTRewrite rewrite, final boolean removeIndentation) throws JavaModelException {
		BodyDeclaration result= null;
		try {
			final IDocument document= new Document(declaringCu.getBuffer().getContents());
			final ASTRewrite rewriter= ASTRewrite.create(bodyDeclaration.getAST());
			final ITrackedNodePosition position= rewriter.track(bodyDeclaration);
			bodyDeclaration.accept(new TypeMapper(rewriter, mapping));
			rewriter.rewriteAST(document, null).apply(document, TextEdit.NONE);
			result= (BodyDeclaration) rewrite.createStringPlaceholder(document.get(position.getStartPosition(), position.getLength()), ASTNode.TYPE_DECLARATION);
		} catch (MalformedTreeException exception) {
			JavaPlugin.log(exception);
		} catch (BadLocationException exception) {
			JavaPlugin.log(exception);
		}
		return result;
	}

	protected static String createTypeLabel(final IType type) {
		return JavaModelUtil.getFullyQualifiedName(type);
	}

	protected static void deleteDeclarationNodes(final CompilationUnit cuNode, final ASTRewrite rewrite, final List members) throws JavaModelException {
		final List declarationNodes= getDeclarationNodes(cuNode, members);
		for (final Iterator iterator= declarationNodes.iterator(); iterator.hasNext();) {
			final ASTNode node= (ASTNode) iterator.next();
			if (node instanceof VariableDeclarationFragment) {
				if (node.getParent() instanceof FieldDeclaration) {
					final FieldDeclaration declaration= (FieldDeclaration) node.getParent();
					if (areAllFragmentsDeleted(declaration, declarationNodes))
						rewrite.remove(declaration, null);
					else
						rewrite.remove(node, null);
				}
			} else {
				rewrite.remove(node, null);
			}
		}
	}

	protected static String getBufferText(final ASTNode node, final ICompilationUnit declaringCu) throws JavaModelException {
		return declaringCu.getBuffer().getText(node.getStartPosition(), node.getLength());
	}

	protected static List getDeclarationNodes(final CompilationUnit cuNode, final List members) throws JavaModelException {
		final List result= new ArrayList(members.size());
		for (final Iterator iterator= members.iterator(); iterator.hasNext();) {
			final IMember member= (IMember) iterator.next();
			ASTNode node= null;
			if (member instanceof IField)
				node= ASTNodeSearchUtil.getFieldDeclarationFragmentNode((IField) member, cuNode);
			else if (member instanceof IType)
				node= ASTNodeSearchUtil.getTypeDeclarationNode((IType) member, cuNode);
			else if (member instanceof IMethod)
				node= ASTNodeSearchUtil.getMethodDeclarationNode((IMethod) member, cuNode);
			if (node != null)
				result.add(node);
		}
		return result;
	}

	protected static String getNewText(final ASTNode node, final ICompilationUnit declaringCu, final boolean removeIndentation) throws JavaModelException {
		final String result= getBufferText(node, declaringCu);
		if (removeIndentation)
			return getUnindentedText(result, declaringCu);

		return result;
	}

	protected static String getReturnTypeName(final IMethod method) throws JavaModelException {
		return Signature.toString(Signature.getReturnType(method.getSignature()).toString());
	}

	protected static IType getSingleTopLevelType(final IMember[] members) {
		if (members != null && members.length == 1 && Checks.isTopLevelType(members[0]))
			return (IType) members[0];
		return null;
	}

	protected static String getTypeName(final IField field) throws JavaModelException {
		return Signature.toString(field.getTypeSignature());
	}

	protected static String getUnindentedText(final String text, final ICompilationUnit declaringCu) throws JavaModelException {
		final String[] lines= Strings.convertIntoLines(text);
		Strings.trimIndentation(lines, CodeFormatterUtil.getTabWidth(), false);
		return Strings.concatenate(lines, StubUtility.getLineDelimiterUsed(declaringCu));
	}

	protected static boolean haveCommonDeclaringType(final IMember[] members) {
		if (members.length == 0)
			return false;
		final IType type= members[0].getDeclaringType();
		if (type == null)
			return false;
		for (int index= 0; index < members.length; index++) {
			if (!type.equals(members[index].getDeclaringType()))
				return false;
		}
		return true;
	}

	protected IType fCachedDeclaringType;

	protected IType[] fCachedReferencedTypes;

	protected TextChangeManager fChangeManager;

	protected final CodeGenerationSettings fCodeGenerationSettings;

	protected final ImportRewriteManager fImportManager;

	protected IMember[] fMembersToMove;

	protected HierarchyRefactoring(final IMember[] members, final CodeGenerationSettings settings) {
		Assert.isNotNull(members);
		Assert.isNotNull(settings);

		fMembersToMove= (IMember[]) SourceReferenceUtil.sortByOffset(members);
		fCodeGenerationSettings= settings;
		fImportManager= new ImportRewriteManager();
	}

	protected void addTextEditFromRewrite(final TextChangeManager manager, final ICompilationUnit cu, final ASTRewrite rewrite) throws CoreException {
		final IDocument document= new Document(cu.getBuffer().getContents());
		final TextEdit result= rewrite.rewriteAST(document, null);
		final TextChange change= manager.get(cu);
		if (fImportManager.hasImportEditFor(cu))
			result.addChild(fImportManager.getImportRewrite(cu).createEdit(document));
		TextChangeCompatibility.addTextEdit(change, getName(), result);
	}

	protected boolean canBeAccessedFrom(final IMember member, final IType targetType, final ITypeHierarchy targetTypeHierarchy) throws JavaModelException {
		Assert.isTrue(!(member instanceof IInitializer));

		if (!member.exists())
			return false;

		if (targetType.equals(member.getDeclaringType()))
			return true;

		if (targetType.equals(member))
			return true;

		if (JdtFlags.isPrivate(member))
			return false;

		if (member.getDeclaringType() == null) {

			if (!(member instanceof IType))
				return false;

			if (JdtFlags.isPublic(member))
				return true;

			if (!JdtFlags.isPackageVisible(member))
				return false;

			if (JavaModelUtil.isSamePackage(((IType) member).getPackageFragment(), targetType.getPackageFragment()))
				return true;

			return targetTypeHierarchy.contains(member.getDeclaringType());
		}

		final IType declaringType= member.getDeclaringType();

		if (!canBeAccessedFrom(declaringType, targetType, targetTypeHierarchy))
			return false;

		if (declaringType.equals(getDeclaringType()))
			return false;

		if (JdtFlags.isPublic(member))
			return true;

		if (JavaModelUtil.isSamePackage(declaringType.getPackageFragment(), targetType.getPackageFragment()))
			return true;

		return JdtFlags.isProtected(member) && targetTypeHierarchy.contains(declaringType);
	}

	protected RefactoringStatus checkDeclaringType(final IProgressMonitor monitor) throws JavaModelException {
		final IType type= getDeclaringType();

		if (type.isInterface())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("HierarchyRefactoring.interface_members")); //$NON-NLS-1$

		if (type.isBinary())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("HierarchyRefactoring.members_of_binary")); //$NON-NLS-1$

		if (type.isReadOnly())
			return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.getString("HierarchyRefactoring.members_of_read-only")); //$NON-NLS-1$

		return new RefactoringStatus();
	}

	protected RefactoringStatus checkIfMembersExist() {
		RefactoringStatus result= new RefactoringStatus();
		IMember member= null;
		for (int index= 0; index < fMembersToMove.length; index++) {
			member= fMembersToMove[index];
			if (member == null || !member.exists())
				result.addFatalError(RefactoringCoreMessages.getString("HierarchyRefactoring.does_not_exist")); //$NON-NLS-1$
		}
		return result;
	}

	protected void clearCaches() {
		fCachedReferencedTypes= null;
		fImportManager.clear();
	}

	protected IFile[] getAllFilesToModify() {
		return ResourceUtil.getFiles(fChangeManager.getAllCompilationUnits());
	}

	protected ICompilationUnit getDeclaringCU() {
		return getDeclaringType().getCompilationUnit();
	}

	public IType getDeclaringType() {
		if (fCachedDeclaringType != null)
			return fCachedDeclaringType;

		Assert.isTrue(fMembersToMove.length > 0);
		fCachedDeclaringType= (IType) WorkingCopyUtil.getOriginal(fMembersToMove[0].getDeclaringType());

		return fCachedDeclaringType;
	}

	public IMember[] getMembersToMove() {
		return fMembersToMove;
	}

	protected IType[] getTypesReferencedInMovedMembers(final IProgressMonitor monitor) throws JavaModelException {
		if (fCachedReferencedTypes == null) {
			final IType[] types= ReferenceFinderUtil.getTypesReferencedIn(fMembersToMove, monitor);
			final List result= new ArrayList(types.length);
			final List members= Arrays.asList(fMembersToMove);
			for (int index= 0; index < types.length; index++) {
				if (!members.contains(types[index]) && types[index] != getDeclaringType())
					result.add(types[index]);
			}
			fCachedReferencedTypes= new IType[result.size()];
			result.toArray(fCachedReferencedTypes);
		}
		return fCachedReferencedTypes;
	}

	protected RefactoringStatus validateModifiesFiles() {
		return Checks.validateModifiesFiles(getAllFilesToModify(), getValidationContext());
	}
}
