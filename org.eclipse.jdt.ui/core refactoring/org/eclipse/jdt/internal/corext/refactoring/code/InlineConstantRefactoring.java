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
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.TargetSourceRangeComputer;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.Corext;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.HierarchicalASTVisitor;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.dom.fragments.ASTFragmentFactory;
import org.eclipse.jdt.internal.corext.dom.fragments.IExpressionFragment;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.IRefactoringSearchRequestor;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Strings;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class InlineConstantRefactoring extends Refactoring {

	private static class InlineTargetCompilationUnit {

		private static class InitializerTraversal extends HierarchicalASTVisitor {
		
			private static boolean areInSameClassOrInterface(ASTNode one, ASTNode other) {
				ASTNode onesContainer= getContainingClassOrInterfaceDeclaration(one);
				ASTNode othersContainer= getContainingClassOrInterfaceDeclaration(other);
		
				if (onesContainer == null || othersContainer == null)
					return false;
		
				ITypeBinding onesContainerBinding= getTypeBindingForClassOrInterfaceDeclaration(onesContainer);
				ITypeBinding othersContainerBinding= getTypeBindingForClassOrInterfaceDeclaration(othersContainer);
		
				Assert.isNotNull(onesContainerBinding);
				Assert.isNotNull(othersContainerBinding);
		
				String onesKey= onesContainerBinding.getKey();
				String othersKey= othersContainerBinding.getKey();
		
				if (onesKey == null || othersKey == null)
					return false;
		
				return onesKey.equals(othersKey);
			}
		
			private static boolean isStaticAccess(SimpleName memberName) {
				IBinding binding= memberName.resolveBinding();
				Assert.isTrue(binding instanceof IVariableBinding || binding instanceof IMethodBinding || binding instanceof ITypeBinding);
		
				if (binding instanceof ITypeBinding)
					return true;
		
				if (binding instanceof IVariableBinding)
					return ((IVariableBinding) binding).isField();
		
				int modifiers= binding.getModifiers();
				return Modifier.isStatic(modifiers);
			}
		
			private static ASTNode getContainingClassOrInterfaceDeclaration(ASTNode node) {
				while (node != null && !(node instanceof AbstractTypeDeclaration) && !(node instanceof AnonymousClassDeclaration)) {
					node= node.getParent();
				}
				return node;
			}
		
			private static ITypeBinding getTypeBindingForClassOrInterfaceDeclaration(ASTNode declaration) {
				if (declaration instanceof AnonymousClassDeclaration)
					return ((AnonymousClassDeclaration) declaration).resolveBinding();
		
				if (declaration instanceof AbstractTypeDeclaration)
					return ((AbstractTypeDeclaration) declaration).resolveBinding();
		
				Assert.isTrue(false);
				return null;
			}
		
			private final Expression fInitializer;
			private ASTRewrite fInitializerRewrite;
		
			// cache:
			private Set fNamesDeclaredLocallyAtNewLocation;
		
			private final Expression fNewLocation;
			private final CompilationUnitRewrite fNewLocationCuRewrite;
		
			public InitializerTraversal(Expression initializer, Expression newLocation, CompilationUnitRewrite newLocationCuRewrite) {
				fInitializer= initializer;
				fInitializerRewrite= ASTRewrite.create(initializer.getAST());
				fNewLocation= newLocation;
				fNewLocationCuRewrite= newLocationCuRewrite;
		
				perform(initializer);
			}
		
			/**
			 * @param scope not a TypeDeclaration
			 * @return Set containing Strings representing simple names
			 */
			private Set getLocallyDeclaredNames(BodyDeclaration scope) {
				Assert.isTrue(!(scope instanceof AbstractTypeDeclaration));
		
				final Set result= new HashSet();
		
				if (scope instanceof FieldDeclaration)
					return result;
		
				scope.accept(new HierarchicalASTVisitor() {
		
					public boolean visit(AbstractTypeDeclaration node) {
						Assert.isTrue(node.getParent() instanceof TypeDeclarationStatement);
		
						result.add(node.getName().getIdentifier());
						return false;
					}
		
					public boolean visit(AnonymousClassDeclaration anonDecl) {
						return false;
					}
		
					public boolean visit(VariableDeclaration varDecl) {
						result.add(varDecl.getName().getIdentifier());
						return false;
					}
				});
				return result;
			}
		
			private Set getNamesDeclaredLocallyAtNewLocation() {
				if (fNamesDeclaredLocallyAtNewLocation != null)
					return fNamesDeclaredLocallyAtNewLocation;
		
				BodyDeclaration enclosingBodyDecl= (BodyDeclaration) ASTNodes.getParent(fNewLocation, BodyDeclaration.class);
				Assert.isTrue(!(enclosingBodyDecl instanceof AbstractTypeDeclaration));
		
				return fNamesDeclaredLocallyAtNewLocation= getLocallyDeclaredNames(enclosingBodyDecl);
			}
		
			public ASTRewrite getInitializerRewrite() {
				return fInitializerRewrite;
			}
		
			private boolean mayBeShadowedByLocalDeclaration(SimpleName memberName) {
				return getNamesDeclaredLocallyAtNewLocation().contains(memberName.getIdentifier());
			}
		
			private void perform(Expression initializer) {
				initializer.accept(this);
			}
		
			private void qualifyMemberName(SimpleName memberName) {
				if (isStaticAccess(memberName))
					qualifyToTopLevelClass(memberName);
			}
		
			private void qualifyToTopLevelClass(SimpleName toQualify) {
				ITypeBinding declaringClass= getDeclaringClassBinding(toQualify);
				if (declaringClass == null)
					return;
				
				Type newQualification= fNewLocationCuRewrite.getImportRewrite().addImport(declaringClass, fInitializerRewrite.getAST());
				fNewLocationCuRewrite.getImportRemover().registerAddedImports(newQualification);
				
				SimpleName newToQualify= (SimpleName) fInitializerRewrite.createMoveTarget(toQualify);
				Type newType= fInitializerRewrite.getAST().newQualifiedType(newQualification, newToQualify);
				fInitializerRewrite.replace(toQualify, newType, null);
			}
			
			private static ITypeBinding getDeclaringClassBinding(SimpleName memberName) {
		
				IBinding binding= memberName.resolveBinding();
				if (binding instanceof IMethodBinding)
					return ((IMethodBinding) binding).getDeclaringClass();
		
				if (binding instanceof IVariableBinding)
					return ((IVariableBinding) binding).getDeclaringClass();
		
				if (binding instanceof ITypeBinding)
					return ((ITypeBinding) binding).getDeclaringClass();
		
				Assert.isTrue(false);
				return null;
		
			}
		
			private void qualifyUnqualifiedMemberNameIfNecessary(SimpleName memberName) {
				if (shouldQualify(memberName))
					qualifyMemberName(memberName);
			}
		
			private boolean shouldQualify(SimpleName memberName) {
				if (!areInSameClassOrInterface(fInitializer, fNewLocation))
					return true;
		
				return mayBeShadowedByLocalDeclaration(memberName);
			}
		
			public boolean visit(FieldAccess fieldAccess) {
				fieldAccess.getExpression().accept(this);
				return false;
			}
		
			public boolean visit(MethodInvocation invocation) {
				if (invocation.getExpression() == null)
					qualifyUnqualifiedMemberNameIfNecessary(invocation.getName());
				else
					invocation.getExpression().accept(this);
		
				for (Iterator it= invocation.arguments().iterator(); it.hasNext();)
					((Expression) it.next()).accept(this);
		
				return false;
			}
		
			public boolean visit(Name name) {
				SimpleName leftmost= getLeftmost(name);
		
				IBinding leftmostBinding= leftmost.resolveBinding();
				if (leftmostBinding instanceof IVariableBinding || leftmostBinding instanceof IMethodBinding || leftmostBinding instanceof ITypeBinding)
					qualifyUnqualifiedMemberNameIfNecessary(leftmost);
		
				if (leftmostBinding instanceof ITypeBinding)
					fNewLocationCuRewrite.getImportRewrite().addImport((ITypeBinding) leftmostBinding);
				
				return false;
			}
		}

		private final Expression fInitializer;
		private final ICompilationUnit fInitializerUnit;

		/** The references in this compilation unit, represented as AST Nodes in the parsed representation of the compilation unit */
		private final Expression[] fReferences;
		private VariableDeclarationFragment fDeclarationToRemove;
		private final CompilationUnitRewrite fCuRewrite;
		
		private InlineTargetCompilationUnit(CompilationUnitRewrite cuRewrite, Name singleReference, InlineConstantRefactoring refactoring) throws JavaModelException {
			fInitializer= refactoring.getInitializer();
			fInitializerUnit= refactoring.getDeclaringCompilationUnit();
			
			fCuRewrite= cuRewrite;
			if (refactoring.getRemoveDeclaration() && cuRewrite.getCu().equals(fInitializerUnit))
				fDeclarationToRemove= refactoring.getDeclaration();
			
			fReferences= new Expression[] { getQualifiedReference(singleReference)};
		}

		private InlineTargetCompilationUnit(CompilationUnitRewrite cuRewrite, SearchMatch[] references, InlineConstantRefactoring refactoring) throws JavaModelException {
			fInitializer= refactoring.getInitializer();
			fInitializerUnit= refactoring.getDeclaringCompilationUnit();
			
			fCuRewrite= cuRewrite;
			if (refactoring.getRemoveDeclaration() && cuRewrite.getCu().equals(fInitializerUnit))
				fDeclarationToRemove= refactoring.getDeclaration();
			
			fReferences= new Expression[references.length];
			CompilationUnit cuNode= fCuRewrite.getRoot();
			for (int i= 0; i < references.length; i++) {
				ASTNode node= NodeFinder.perform(cuNode, references[i].getOffset(), references[i].getLength());
				Assert.isTrue(node instanceof Name);
				fReferences[i]= getQualifiedReference((Name) node);
			}
		}

		private static Expression getQualifiedReference(Name fieldName) {
			if (doesParentQualify(fieldName))
				return (Expression) fieldName.getParent();

			return fieldName;
		}

		private static boolean doesParentQualify(Name fieldName) {
			ASTNode parent= fieldName.getParent();
			Assert.isNotNull(parent);

			if (parent instanceof FieldAccess && ((FieldAccess) parent).getName() == fieldName)
				return true;

			if (parent instanceof QualifiedName && ((QualifiedName) parent).getName() == fieldName)
				return true;

			if (parent instanceof MethodInvocation && ((MethodInvocation) parent).getName() == fieldName)
				return true;

			return false;
		}

		public CompilationUnitChange getChange() throws CoreException {
			for (int i= 0; i < fReferences.length; i++)
				inlineReference(fReferences[i]);
			
			removeConstantDeclarationIfNecessary();
			
			fCuRewrite.getASTRewrite().setTargetSourceRangeComputer(new TargetSourceRangeComputer() {
				public SourceRange computeSourceRange(ASTNode node) {
					return new TargetSourceRangeComputer.SourceRange(node.getStartPosition(), node.getLength());
				}
			});
			return fCuRewrite.createChange();
		}

		private void inlineReference(Expression reference) throws CoreException {
			ASTNode importDecl= ASTNodes.getParent(reference, ImportDeclaration.class);
			if (importDecl != null)
				return; // don't inline into static imports

			String modifiedInitializer= prepareInitializerForLocation(reference);
			if (modifiedInitializer == null)
				return;

			TextEditGroup msg= fCuRewrite.createGroupDescription(RefactoringCoreMessages.getString("InlineConstantRefactoring.Inline")); //$NON-NLS-1$
			Expression newReference= (Expression) fCuRewrite.getASTRewrite().createStringPlaceholder(modifiedInitializer, reference.getNodeType());
			if (shouldParenthesizeSubstitute(fInitializer, reference)) {
				ParenthesizedExpression parenthesized= fCuRewrite.getAST().newParenthesizedExpression();
				parenthesized.setExpression(newReference);
				newReference= parenthesized;
			}
			fCuRewrite.getASTRewrite().replace(reference, newReference, msg);
		}

		private String prepareInitializerForLocation(Expression location) throws CoreException {
			InitializerTraversal traversal= new InitializerTraversal(fInitializer, location, fCuRewrite);
			ASTRewrite initializerRewrite= traversal.getInitializerRewrite();
			IDocument document= new Document(fInitializerUnit.getBuffer().getContents()); // could reuse document when generating and applying undo edits
			
			final RangeMarker marker= new RangeMarker(fInitializer.getStartPosition(), fInitializer.getLength());
			TextEdit[] rewriteEdits= initializerRewrite.rewriteAST(document, fInitializerUnit.getJavaProject().getOptions(true)).removeChildren();
			marker.addChildren(rewriteEdits);
			try {
				marker.apply(document, TextEdit.UPDATE_REGIONS);
				String rewrittenInitializer= document.get(marker.getOffset(), marker.getLength());
				int width= CodeFormatterUtil.getTabWidth(fCuRewrite.getCu().getJavaProject());
				IRegion region= document.getLineInformation(document.getLineOfOffset(marker.getOffset()));
				int oldIndent= Strings.computeIndent(document.get(region.getOffset(), region.getLength()), width);
				return Strings.changeIndent(rewrittenInitializer, oldIndent, width, "", StubUtility.getLineDelimiterFor(document)); //$NON-NLS-1$
			} catch (MalformedTreeException e) {
				JavaPlugin.log(e);
			} catch (BadLocationException e) {
				JavaPlugin.log(e);
			}
			return fInitializerUnit.getBuffer().getText(fInitializer.getStartPosition(), fInitializer.getLength());
		}

		private static boolean shouldParenthesizeSubstitute(Expression substitute, Expression location) {
			if (substitute instanceof Assignment) // for esthetic reasons
				return true;
			else
				return ASTNodes.substituteMustBeParenthesized(substitute, location);
		}
		
		private void removeConstantDeclarationIfNecessary() throws CoreException {
			if (fDeclarationToRemove == null)
				return;
			
			FieldDeclaration parentDeclaration= (FieldDeclaration) fDeclarationToRemove.getParent();
			ASTNode toRemove;
			if (parentDeclaration.fragments().size() == 1)
				toRemove= parentDeclaration;
			else
				toRemove= fDeclarationToRemove;

			TextEditGroup msg= fCuRewrite.createGroupDescription(RefactoringCoreMessages.getString("InlineConstantRefactoring.remove_declaration")); //$NON-NLS-1$
			fCuRewrite.getASTRewrite().remove(toRemove, msg);
			fCuRewrite.getImportRemover().registerRemovedNode(toRemove);
		}
	}

	// ---- End InlineTargetCompilationUnit ----------------------------------------------------------------------------------------------

	private static SimpleName getLeftmost(Name name) {
		if (name instanceof SimpleName)
			return (SimpleName) name;

		return getLeftmost(((QualifiedName) name).getQualifier());
	}

	private final int fSelectionStart;
	private final int fSelectionLength;
	
	private final ICompilationUnit fSelectionCu;
	private CompilationUnitRewrite fSelectionCuRewrite;
	private Name fSelectedConstantName;
	
	IField fField;
	private CompilationUnitRewrite fDeclarationCuRewrite;
	private VariableDeclarationFragment fDeclaration;
	private boolean fDeclarationSelected;
	private boolean fDeclarationSelectedChecked= false;
	private boolean fInitializerAllStaticFinal;
	private boolean fInitializerChecked= false;

	private boolean fRemoveDeclaration= false;
	private boolean fReplaceAllReferences= true;

	private CompilationUnitChange[] fChanges;
	
	private InlineConstantRefactoring(ICompilationUnit cu, int selectionStart, int selectionLength) {
		Assert.isTrue(selectionStart >= 0);
		Assert.isTrue(selectionLength >= 0);
		Assert.isTrue(cu.exists());
		fSelectionCu= cu;
		fSelectionStart= selectionStart;
		fSelectionLength= selectionLength;
		fSelectionCuRewrite= new CompilationUnitRewrite(fSelectionCu);
		fSelectedConstantName= findConstantNameNode();
	}

	private Name findConstantNameNode() {
		ASTNode node= NodeFinder.perform(fSelectionCuRewrite.getRoot(), fSelectionStart, fSelectionLength);
		if (node == null)
			return null;
		if (node instanceof FieldAccess)
			node= ((FieldAccess) node).getName();
		if (node.getParent() instanceof EnumConstantDeclaration)
			return null;
		if (!(node instanceof Name))
			return null;
		Name name= (Name) node;
		IBinding binding= name.resolveBinding();
		if (!(binding instanceof IVariableBinding))
			return null;
		IVariableBinding variableBinding= (IVariableBinding) binding;
		if (!variableBinding.isField() || variableBinding.isEnumConstant())
			return null;
		int modifiers= binding.getModifiers();
		if (! (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)))
			return null;

		return name;
	}

	public static InlineConstantRefactoring create(ICompilationUnit cu, int selectionStart, int selectionLength) {
		InlineConstantRefactoring ref= new InlineConstantRefactoring(cu, selectionStart, selectionLength);
		if (ref.checkStaticFinalConstantNameSelected().hasFatalError())
			return null;
		return ref;
	}

	private RefactoringStatus checkStaticFinalConstantNameSelected() {
		if (fSelectedConstantName == null)
			return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("InlineConstantRefactoring.static_final_field"), null, Corext.getPluginId(), RefactoringStatusCodes.NOT_STATIC_FINAL_SELECTED, null); //$NON-NLS-1$

		return new RefactoringStatus();
	}

	public static boolean isAvailable(IField field) throws JavaModelException {
		return Checks.isAvailable(field) && JdtFlags.isStatic(field) && JdtFlags.isFinal(field) && !JdtFlags.isEnum(field);
	}

	public String getName() {
		return RefactoringCoreMessages.getString("InlineConstantRefactoring.name"); //$NON-NLS-1$
	}

	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask("", 3); //$NON-NLS-1$

			if (!fSelectionCu.isStructureKnown())
				return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("InlineConstantRefactoring.syntax_errors"), null, Corext.getPluginId(), RefactoringStatusCodes.SYNTAX_ERRORS, null); //$NON-NLS-1$

			RefactoringStatus result= checkStaticFinalConstantNameSelected();
			if (result.hasFatalError())
				return result;
			
			result.merge(findField());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			result.merge(findDeclaration());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			result.merge(checkInitializer());
			if (result.hasFatalError())
				return result;
			pm.worked(1);

			return result;
			
		} finally {
			pm.done();
		}
	}

	private RefactoringStatus findField() throws JavaModelException {
		fField= Bindings.findField((IVariableBinding) fSelectedConstantName.resolveBinding(), fSelectionCu.getJavaProject());
		if (fField != null && ! fField.exists())
			return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("InlineConstantRefactoring.local_anonymous_unsupported"), null, Corext.getPluginId(), RefactoringStatusCodes.LOCAL_AND_ANONYMOUS_NOT_SUPPORTED, null); //$NON-NLS-1$
		
		return null;
	}

	private RefactoringStatus findDeclaration() throws JavaModelException {
		fDeclarationSelectedChecked= true;
		fDeclarationSelected= false;
		ASTNode parent= fSelectedConstantName.getParent();
		if (parent instanceof VariableDeclarationFragment) {
			VariableDeclarationFragment parentDeclaration= (VariableDeclarationFragment) parent;
			if (parentDeclaration.getName() == fSelectedConstantName) {
				fDeclarationSelected= true;
				fDeclarationCuRewrite= fSelectionCuRewrite;
				fDeclaration= (VariableDeclarationFragment) fSelectedConstantName.getParent();
				return null;
			}
		}
		
		VariableDeclarationFragment declaration= (VariableDeclarationFragment) fSelectionCuRewrite.getRoot().findDeclaringNode(fSelectedConstantName.resolveBinding());
		if (declaration != null) {
			fDeclarationCuRewrite= fSelectionCuRewrite;
			fDeclaration= declaration;
			return null;
		}

		if (fField.getCompilationUnit() == null)
			return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("InlineConstantRefactoring.binary_file"), null, Corext.getPluginId(), RefactoringStatusCodes.DECLARED_IN_CLASSFILE, null); //$NON-NLS-1$
		
		fDeclarationCuRewrite= new CompilationUnitRewrite(fField.getCompilationUnit());
		fDeclaration= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(fField, fDeclarationCuRewrite.getRoot());
		return null;
	}

	private RefactoringStatus checkInitializer() {
		Expression initializer= getInitializer();
		if (initializer == null)
			return RefactoringStatus.createStatus(RefactoringStatus.FATAL, RefactoringCoreMessages.getString("InlineConstantRefactoring.blank_finals"), null, Corext.getPluginId(), RefactoringStatusCodes.CANNOT_INLINE_BLANK_FINAL, null); //$NON-NLS-1$

		fInitializerAllStaticFinal= ConstantChecks.isStaticFinalConstant((IExpressionFragment) ASTFragmentFactory.createFragmentForFullSubtree(initializer));
		fInitializerChecked= true;
		return new RefactoringStatus();
	}

	private VariableDeclarationFragment getDeclaration() throws JavaModelException {
		return fDeclaration;
	}

	private Expression getInitializer() {
		return fDeclaration.getInitializer();
	}
	
	private ICompilationUnit getDeclaringCompilationUnit() throws JavaModelException {
		return fField.getCompilationUnit();
	}

	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result= new RefactoringStatus();
		pm.beginTask("", 3); //$NON-NLS-1$
		
		try {
			List/*<CompilationUnitChange>*/changes= new ArrayList();

			// prepareTargets:
			if (getReplaceAllReferences()) {
				SearchResultGroup[] searchResultGroups= findReferences(pm, result);
				for (int i= 0; i < searchResultGroups.length; i++) {
					if (pm.isCanceled())
						throw new OperationCanceledException();
					SearchResultGroup group= searchResultGroups[i];
					ICompilationUnit cu= group.getCompilationUnit();

					SearchMatch[] searchResults= group.getSearchResults();
					InlineTargetCompilationUnit targetCompilationUnit= new InlineTargetCompilationUnit(
							getCuRewrite(cu), searchResults, this);
					changes.add(targetCompilationUnit.getChange());
				}

			} else {
				Assert.isTrue(! isDeclarationSelected());
				InlineTargetCompilationUnit targetForOnlySelectedReference= new InlineTargetCompilationUnit(
						fSelectionCuRewrite, fSelectedConstantName, this);
				changes.add(targetForOnlySelectedReference.getChange());
			}

			if (result.hasFatalError())
				return result;

			if (getRemoveDeclaration()) {
				boolean declarationRemoved= false;
				for (Iterator iter= changes.iterator(); iter.hasNext();) {
					CompilationUnitChange change= (CompilationUnitChange) iter.next();
					if (change.getCompilationUnit().equals(fDeclarationCuRewrite.getCu())) {
						declarationRemoved= true;
						break;
					}
				}
				if (! declarationRemoved) {
					InlineTargetCompilationUnit targetForDeclaration= new InlineTargetCompilationUnit(fDeclarationCuRewrite, new SearchMatch[0], this);
					CompilationUnitChange change= targetForDeclaration.getChange();
					if (change != null)
						changes.add(change);
				}
			}

			ICompilationUnit[] cus= new ICompilationUnit[changes.size()];
			for (int i= 0; i < changes.size(); i++) {
				CompilationUnitChange change= (CompilationUnitChange) changes.get(i);
				cus[i]= change.getCompilationUnit();
			}
			result.merge(Checks.validateModifiesFiles(ResourceUtil.getFiles(cus), getValidationContext()));

			pm.worked(1);

			fChanges= (CompilationUnitChange[]) changes.toArray(new CompilationUnitChange[changes.size()]);

			return result;
			
		} finally {
			fSelectionCuRewrite= null;
			fSelectedConstantName= null;
			fDeclarationCuRewrite= null;
			fDeclaration= null;
			pm.done();
		}
	}

	private CompilationUnitRewrite getCuRewrite(ICompilationUnit cu) {
		CompilationUnitRewrite cuRewrite;
		if (cu.equals(fSelectionCu))
			cuRewrite= fSelectionCuRewrite;
		else if (cu.equals(fField.getCompilationUnit()))
			cuRewrite= fDeclarationCuRewrite;
		else
			cuRewrite= new CompilationUnitRewrite(cu);
		return cuRewrite;
	}

	private SearchResultGroup[] findReferences(IProgressMonitor pm, RefactoringStatus status) throws JavaModelException {
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(fField, IJavaSearchConstants.REFERENCES));
		engine.setFiltering(true, true);
		engine.setScope(RefactoringScopeFactory.create(fField));
		engine.setStatus(status);
		engine.setRequestor(new IRefactoringSearchRequestor() {
			public boolean acceptSearchMatch(SearchMatch match) {
				return ! match.isInsideDocComment();
			}
		});
		engine.searchPattern(new SubProgressMonitor(pm, 1));
		return (SearchResultGroup[]) engine.getResults();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(RefactoringCoreMessages.getString("InlineConstantRefactoring.preview"), 2); //$NON-NLS-1$
			final DynamicValidationStateChange result= new DynamicValidationStateChange(RefactoringCoreMessages.getString("InlineConstantRefactoring.inline")); //$NON-NLS-1$
			result.addAll(fChanges);
			return result;
		} finally {
			pm.done();
			fChanges= null;
		}
	}
	

	private void checkInvariant() {
		if (isDeclarationSelected())
			Assert.isTrue(fReplaceAllReferences);
		if (fRemoveDeclaration)
			Assert.isTrue(fReplaceAllReferences);
	}

	public boolean getRemoveDeclaration() {
		checkInvariant();
		return fRemoveDeclaration;
	}

	public boolean getReplaceAllReferences() {
		checkInvariant();
		return fReplaceAllReferences;
	}

	public boolean isDeclarationSelected() {
		Assert.isTrue(fDeclarationSelectedChecked);
		return fDeclarationSelected;
	}

	public boolean isInitializerAllStaticFinal() {
		Assert.isTrue(fInitializerChecked);
		return fInitializerAllStaticFinal;
	}

	public void setRemoveDeclaration(boolean removeDeclaration) {
		fRemoveDeclaration= removeDeclaration;
		checkInvariant();
	}

	public void setReplaceAllReferences(boolean replaceAllReferences) {
		fReplaceAllReferences= replaceAllReferences;
		checkInvariant();
	}
}