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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchPattern;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.GetterSetterUtil;
import org.eclipse.jdt.internal.corext.dom.ModifierRewrite;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringScopeFactory;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringSearchEngine2;
import org.eclipse.jdt.internal.corext.refactoring.SearchResultGroup;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabels;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

import org.eclipse.ltk.core.refactoring.IRefactoringStatusEntryComparator;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusEntry;

/**
 * Helper class to adjust the visibilities of java elements which reference a specified member.
 */
public final class JavaElementVisibilityAdjustor {

	/** Default implementation */
	private static class RefactoringStatusEntryComparator implements IRefactoringStatusEntryComparator {

		public final int compare(final RefactoringStatusEntry first, final RefactoringStatusEntry second) {
			return first.getMessage().compareTo(second.getMessage());
		}
	}

	/** The refactoring status entry comparator */
	private static final IRefactoringStatusEntryComparator fgComparator= new RefactoringStatusEntryComparator();

	/**
	 * Returns the label for the specified java element.
	 * 
	 * @param element the element to get the label for
	 * @return the label for the element
	 */
	private static String getLabel(final IJavaElement element) {
		Assert.isNotNull(element);
		return JavaElementLabels.getElementLabel(element, JavaElementLabels.ALL_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES);
	}

	/**
	 * Returns the label for the specified visibility keyword.
	 * 
	 * @param keyword the keyword to get the label for
	 * @return the label for the keyword
	 */
	private static String getLabel(final ModifierKeyword keyword) {
		Assert.isTrue(isVisibilityKeyword(keyword));
		if (keyword == null)
			return RefactoringCoreMessages.getString("JavaElementVisibilityAdjustor.change_visibility_default"); //$NON-NLS-1$
		else if (ModifierKeyword.PUBLIC_KEYWORD.equals(keyword))
			return RefactoringCoreMessages.getString("JavaElementVisibilityAdjustor.change_visibility_public"); //$NON-NLS-1$
		else if (ModifierKeyword.PROTECTED_KEYWORD.equals(keyword))
			return RefactoringCoreMessages.getString("JavaElementVisibilityAdjustor.change_visibility_protected"); //$NON-NLS-1$
		else
			return RefactoringCoreMessages.getString("JavaElementVisibilityAdjustor.change_visibility_private"); //$NON-NLS-1$
	}

	/**
	 * Do the specified modifiers represent a lower visibility than the required threshold?
	 * 
	 * @param modifiers the modifier list to test
	 * @param threshold the visibility threshold keyword to compare with, or <code>null</code> to compare with default visibility
	 * @return <code>true</code> if the visibility is lower than required, <code>false</code> otherwise
	 */
	private static boolean hasLowerVisibility(final List modifiers, final ModifierKeyword threshold) {
		Assert.isNotNull(modifiers);
		Assert.isTrue(isVisibilityKeyword(threshold));
		Modifier modifier= null;
		IExtendedModifier extended= null;
		for (final Iterator iterator= modifiers.iterator(); iterator.hasNext();) {
			extended= (IExtendedModifier) iterator.next();
			if (extended.isModifier()) {
				modifier= (Modifier) extended;
				if (isVisibilityKeyword(modifier.getKeyword()))
					return hasLowerVisibility(modifier.getKeyword(), threshold);
			}
		}
		return hasLowerVisibility((ModifierKeyword) null, threshold);
	}

	/**
	 * Does the specified modifier keyword represent a lower visibility than the required threshold?
	 * 
	 * @param keyword the visibility keyword to test, or <code>null</code> for default visibility
	 * @param threshold the visibility threshold keyword to compare with, or <code>null</code> to compare with default visibility
	 * @return <code>true</code> if the visibility is lower than required, <code>false</code> otherwise
	 */
	private static boolean hasLowerVisibility(final ModifierKeyword keyword, final ModifierKeyword threshold) {
		Assert.isTrue(isVisibilityKeyword(keyword));
		Assert.isTrue(isVisibilityKeyword(threshold));
		final int keywordFlag= keyword != null ? keyword.toFlagValue() : Modifier.NONE;
		final int thresholdFlag= threshold != null ? threshold.toFlagValue() : Modifier.NONE;
		if (Modifier.isPrivate(thresholdFlag))
			return false;
		else if (Modifier.isPublic(thresholdFlag))
			return !Modifier.isPublic(keywordFlag);
		else if (Modifier.isProtected(thresholdFlag))
			return !Modifier.isProtected(thresholdFlag) && !Modifier.isPublic(thresholdFlag);
		else
			return Modifier.isPrivate(keywordFlag);
	}

	/**
	 * Is the specified severity a refactoring status severity?
	 * 
	 * @param severity the severity to test
	 * @return <code>true</code> if it is a refactoring status severity, <code>false</code> otherwise
	 */
	private static boolean isStatusSeverity(final int severity) {
		return severity == RefactoringStatus.ERROR || severity == RefactoringStatus.FATAL || severity == RefactoringStatus.INFO || severity == RefactoringStatus.OK || severity == RefactoringStatus.WARNING;
	}

	/**
	 * Is the specified modifier keyword a visibility keyword?
	 * 
	 * @param keyword the keyword to test
	 * @return <code>true</code> if it is a visibility keyword, <code>false</code> otherwise
	 */
	private static boolean isVisibilityKeyword(final ModifierKeyword keyword) {
		return keyword == null || ModifierKeyword.PUBLIC_KEYWORD.equals(keyword) || ModifierKeyword.PROTECTED_KEYWORD.equals(keyword) || ModifierKeyword.PRIVATE_KEYWORD.equals(keyword);
	}

	/** The failure message severity */
	private int fFailureSeverity= RefactoringStatus.ERROR;

	/** The member causing the adjustment */
	private final IMember fMember;

	/** The reference element */
	private final IJavaElement fReference;

	/** The map of compilation units to compilation unit rewrites */
	private Map fRewrites= new HashMap(3);

	/** The incoming search scope */
	private IJavaSearchScope fScope;

	/** The status of the visibility adjustment */
	private RefactoringStatus fStatus= new RefactoringStatus();

	/** Should getters be used to resolve visibility issues? */
	private boolean fUseGetters= true;

	/** Should setters be used to resolve visibility issues? */
	private boolean fUseSetters= true;

	/** The visibility message severity */
	private int fVisibilitySeverity= RefactoringStatus.WARNING;

	/**
	 * Creates a new java element visibility adjustor.
	 * 
	 * @param reference the reference element used to compute the visibility
	 * @param member the member which causes the visibility changes
	 */
	public JavaElementVisibilityAdjustor(final IJavaElement reference, final IMember member) {
		Assert.isNotNull(reference);
		Assert.isNotNull(member);
		fScope= RefactoringScopeFactory.createReferencedScope(new IJavaElement[] { member}, IJavaSearchScope.REFERENCED_PROJECTS | IJavaSearchScope.SOURCES | IJavaSearchScope.APPLICATION_LIBRARIES);
		fReference= reference;
		fMember= member;
	}

	/**
	 * Adjusts the visibility of the specified search match found in a class file.
	 * 
	 * @param file the class file where the search match was found
	 * @param match the search match that has been found
	 */
	private void adjustClassFileVisibility(final IClassFile file, final SearchMatch match) {
		Assert.isNotNull(file);
		Assert.isNotNull(match);

		// TODO implement
	}

	/**
	 * Adjusts the visibility of the specified search match found in a compilation unit.
	 * 
	 * @param unit the compilation unit where the search match was found
	 * @param match the search match that has been found
	 */
	private void adjustCompilationUnitVisibility(final ICompilationUnit unit, final SearchMatch match) {
		Assert.isNotNull(unit);
		Assert.isNotNull(match);

		// TODO implement
	}

	/**
	 * Adjusts the visibility of the member based on the incoming references represented by the specified search result groups.
	 * 
	 * @param groups the search result groups representing the references
	 */
	private void adjustIncomingVisibility(final SearchResultGroup[] groups) {
		Assert.isNotNull(groups);
		IJavaElement element= null;
		SearchMatch[] matches= null;
		SearchResultGroup group= null;
		for (int index= 0; index < groups.length; index++) {
			group= groups[index];
			element= JavaCore.create(group.getResource());
			if (element instanceof ICompilationUnit) {
				matches= group.getSearchResults();
				for (int offset= 0; offset < matches.length; offset++)
					adjustCompilationUnitVisibility((ICompilationUnit) element, matches[offset]);
			} else if (element instanceof IPackageFragmentRoot) {
				matches= group.getSearchResults();
				for (int offset= 0; offset < matches.length; offset++)
					adjustPackageFragmentVisibility((IPackageFragmentRoot) element, matches[offset]);
			} else if (element instanceof IClassFile) {
				matches= group.getSearchResults();
				for (int offset= 0; offset < matches.length; offset++)
					adjustClassFileVisibility((IClassFile) element, matches[offset]);
			} else
				Assert.isTrue(false);
		}
	}

	/**
	 * Adjusts the visibility of the referenced body declaration.
	 * 
	 * @param rewrite the compilation unit rewrite to use
	 * @param declaration the body declaration where to adjust the visibility
	 * @param threshold the visibility keyword representing the required visibility
	 * @param binding the binding of the body declaration
	 * @param template the message template to use
	 * @return <code>true</code> if the visibility is adjusted, <code>false</code> otherwise
	 */
	private boolean adjustOutgoingVisibility(final CompilationUnitRewrite rewrite, final BodyDeclaration declaration, final ModifierKeyword threshold, final IBinding binding, final String template) {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(declaration);
		Assert.isNotNull(threshold);
		Assert.isNotNull(binding);
		if (hasLowerVisibility((List) declaration.getStructuralProperty(declaration.getModifiersProperty()), threshold)) {
			final RefactoringStatusEntry entry= new RefactoringStatusEntry(fVisibilitySeverity, RefactoringCoreMessages.getFormattedString(template, new String[] { BindingLabels.getFullyQualified(binding), getLabel(threshold)}), JavaStatusContext.create(rewrite.getCu(), declaration));
			if (fStatus.getEntries(fgComparator, entry).length == 0) {
				fStatus.addEntry(entry);
				ModifierRewrite.create(rewrite.getASTRewrite(), declaration).setVisibility(threshold.toFlagValue(), rewrite.createGroupDescription(RefactoringCoreMessages.getFormattedString("JavaElementVisibilityAdjustor.change_visibility", getLabel(threshold)))); //$NON-NLS-1$
			}
			return true;
		}
		return false;
	}

	/**
	 * Adjusts the visibilities of the referenced element from the search match found in a compilation unit.
	 * 
	 * @param rewrite the compilation unit rewrite to use
	 * @param match the search match representing the element declaration
	 * @param threshold the visibility keyword representing the required visibility
	 */
	private void adjustOutgoingVisibility(final CompilationUnitRewrite rewrite, final SearchMatch match, final ModifierKeyword threshold) {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(match);
		Assert.isTrue(isVisibilityKeyword(threshold));
		final AST ast= rewrite.getASTRewrite().getAST();
		final ASTNode node= ASTNodeSearchUtil.findNode(match, rewrite.getRoot());
		if (node instanceof MethodDeclaration) {
			final MethodDeclaration declaration= (MethodDeclaration) node;
			adjustOutgoingVisibility(rewrite, declaration, threshold, declaration.resolveBinding(), "JavaElementVisibilityAdjustor.change_visibility_method_warning"); //$NON-NLS-1$
		} else if (node instanceof SimpleName) {
			boolean resolved= false;
			final ASTNode parent= node.getParent();
			if (fUseGetters) {
				final IField field= (IField) match.getElement();
				if (field != null) {
					try {
						final IMethod getter= GetterSetterUtil.getGetter(field);
						if (getter != null) {
							final MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(getter, rewrite.getRoot());
							if (declaration != null) {
								adjustOutgoingVisibility(rewrite, declaration, threshold, declaration.resolveBinding(), "JavaElementVisibilityAdjustor.change_visibility_method_warning"); //$NON-NLS-1$

								// TODO implement

								resolved= false;
							}
						}
					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
					}
				}
			} else if (fUseSetters) {
				final IField field= (IField) match.getElement();
				if (field != null) {
					try {
						final IMethod setter= GetterSetterUtil.getSetter(field);
						if (setter != null) {
							final MethodDeclaration declaration= ASTNodeSearchUtil.getMethodDeclarationNode(setter, rewrite.getRoot());
							if (declaration != null) {
								adjustOutgoingVisibility(rewrite, declaration, threshold, declaration.resolveBinding(), "JavaElementVisibilityAdjustor.change_visibility_method_warning"); //$NON-NLS-1$

								// TODO implement

								resolved= false;
							}
						}
					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
					}
				}
			}
			if (!resolved) {
				if (parent instanceof VariableDeclarationFragment) {
					final VariableDeclarationFragment fragment= (VariableDeclarationFragment) parent;
					final FieldDeclaration declaration= (FieldDeclaration) fragment.getParent();
					final RefactoringStatusEntry entry= new RefactoringStatusEntry(fVisibilitySeverity, RefactoringCoreMessages.getFormattedString("JavaElementVisibilityAdjustor.change_visibility_field_warning", new String[] { BindingLabels.getFullyQualified(fragment.resolveBinding()), getLabel(threshold)}), JavaStatusContext.create(rewrite.getCu(), fragment)); //$NON-NLS-1$
					if (fStatus.getEntries(fgComparator, entry).length == 0) {
						fStatus.addEntry(entry);
						if (declaration.fragments().size() == 1)
							ModifierRewrite.create(rewrite.getASTRewrite(), declaration).setVisibility(threshold.toFlagValue(), rewrite.createGroupDescription(RefactoringCoreMessages.getFormattedString("JavaElementVisibilityAdjustor.change_visibility", getLabel(threshold)))); //$NON-NLS-1$
						else {
							final VariableDeclarationFragment newFragment= ast.newVariableDeclarationFragment();
							newFragment.setName((SimpleName) ASTNode.copySubtree(ast, fragment.getName()));
							final FieldDeclaration newDeclaration= ast.newFieldDeclaration(newFragment);
							newDeclaration.setType((Type) ASTNode.copySubtree(ast, declaration.getType()));
							final AbstractTypeDeclaration type= (AbstractTypeDeclaration) declaration.getParent();
							rewrite.getASTRewrite().getListRewrite(type, type.getBodyDeclarationsProperty()).insertAfter(newDeclaration, declaration, null);
							rewrite.getASTRewrite().getListRewrite(declaration, FieldDeclaration.FRAGMENTS_PROPERTY).remove(fragment, rewrite.createGroupDescription(RefactoringCoreMessages.getFormattedString("JavaElementVisibilityAdjustor.change_visibility", getLabel(threshold)))); //$NON-NLS-1$
						}
					}
				} else if (parent instanceof AbstractTypeDeclaration) {
					final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) parent;
					adjustOutgoingVisibility(rewrite, declaration, threshold, declaration.resolveBinding(), "JavaElementVisibilityAdjustor.change_visibility_type_warning"); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Adjusts the visibilities of the outgoing references from the member represented by the specified search result groups.
	 * 
	 * @param groups the search result groups representing the references
	 */
	private void adjustOutgoingVisibility(final SearchResultGroup[] groups) {
		Assert.isNotNull(groups);

		ModifierKeyword threshold= ModifierKeyword.PUBLIC_KEYWORD;

		IJavaElement element= null;
		SearchMatch[] matches= null;
		SearchResultGroup group= null;
		CompilationUnitRewrite rewrite= null;
		for (int index= 0; index < groups.length; index++) {
			group= groups[index];
			element= JavaCore.create(group.getResource());
			if (element instanceof ICompilationUnit) {
				rewrite= getRewrite((ICompilationUnit) element);
				matches= group.getSearchResults();
				for (int offset= 0; offset < matches.length; offset++)
					adjustOutgoingVisibility(rewrite, matches[offset], threshold);
			} else if (element != null)
				fStatus.merge(RefactoringStatus.createStatus(fFailureSeverity, RefactoringCoreMessages.getFormattedString("JavaElementVisibilityAdjustor.binary.outgoing.project", new String[] { element.getJavaProject().getElementName(), getLabel(fMember)}), null, null, RefactoringStatusEntry.NO_CODE, null)); //$NON-NLS-1$
			else if (group.getResource() != null)
				fStatus.merge(RefactoringStatus.createStatus(fFailureSeverity, RefactoringCoreMessages.getFormattedString("JavaElementVisibilityAdjustor.binary.outgoing.resource", new String[] { group.getResource().getName(), getLabel(fMember)}), null, null, RefactoringStatusEntry.NO_CODE, null)); //$NON-NLS-1$
		}
	}

	/**
	 * Adjusts the visibility of the specified search match found in a package fragment root.
	 * 
	 * @param root the package fragment root where the search match was found
	 * @param match the search match that has been found
	 */
	private void adjustPackageFragmentVisibility(final IPackageFragmentRoot root, final SearchMatch match) {
		Assert.isNotNull(root);
		Assert.isNotNull(match);

		// TODO implement
	}

	/**
	 * Adjusts the visibilities of the referenced and referencing elements.
	 * 
	 * @param monitor the progress monitor, or <code>null</code>
	 * @throws JavaModelException if an error occurs during search
	 */
	public final void adjustVisibility(final IProgressMonitor monitor) throws JavaModelException {
		final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(fMember, IJavaSearchConstants.REFERENCES));
		engine.setScope(fScope);
		engine.setStatus(fStatus);
		engine.searchPattern(monitor);
		adjustIncomingVisibility((SearchResultGroup[]) engine.getResults());
		engine.clearResults();
		engine.searchReferencedTypes(fMember, monitor);
		engine.searchReferencedFields(fMember, monitor);
		engine.searchReferencedMethods(fMember, monitor);
		adjustOutgoingVisibility((SearchResultGroup[]) engine.getResults());
	}

	/**
	 * Returns a compilation unit rewrite for the specified compilation unit.
	 * 
	 * @param unit the compilation unit to get the rewrite for
	 * @return the rewrite for the compilation unit
	 */
	private CompilationUnitRewrite getRewrite(final ICompilationUnit unit) {
		Assert.isNotNull(unit);
		CompilationUnitRewrite rewrite= (CompilationUnitRewrite) fRewrites.get(unit);
		if (rewrite == null)
			rewrite= new CompilationUnitRewrite(unit);
		return rewrite;
	}

	/**
	 * Returns the compilation unit rewrites used by this adjustor.
	 * 
	 * @return the compilation unit rewrites
	 */
	public final Map getRewrites() {
		return fRewrites;
	}

	/**
	 * Returns the status of the visibility adjustment.
	 * 
	 * @return the status
	 */
	public final RefactoringStatus getStatus() {
		return fStatus;
	}

	/**
	 * Sets the severity of failure messages.
	 * 
	 * @param severity the severity of failure messages
	 */
	public final void setFailureSeverity(final int severity) {
		Assert.isTrue(isStatusSeverity(severity));
		fFailureSeverity= severity;
	}

	/**
	 * Sets the compilation unit rewrites used by this adjustor.
	 * <p>
	 * This method must be called before calling {@link JavaElementVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is to use no existing rewrites.
	 * 
	 * @param rewrites the map of compilation units to compilation unit rewrites to set
	 */
	public final void setRewrites(final Map rewrites) {
		Assert.isNotNull(rewrites);
		fRewrites= rewrites;
	}

	/**
	 * Sets the incoming search scope used by this adjustor.
	 * <p>
	 * This method must be called before calling {@link JavaElementVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is the whole workspace as scope.
	 * 
	 * @param scope the search scope to set
	 */
	public final void setScope(final IJavaSearchScope scope) {
		Assert.isNotNull(scope);
		fScope= scope;
	}

	/**
	 * Sets the refactoring status used by this adjustor.
	 * <p>
	 * This method must be called before calling {@link JavaElementVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is a fresh status with status {@link RefactoringStatus#OK}.
	 * 
	 * @param status the refactoring status to set
	 */
	public final void setStatus(final RefactoringStatus status) {
		Assert.isNotNull(status);
		fStatus= status;
	}

	/**
	 * Determines whether getters should be preferred to resolve visibility issues.
	 * <p>
	 * This method must be called before calling {@link JavaElementVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is to use getters where possible.
	 * 
	 * @param use
	 */
	public final void setUseGetters(final boolean use) {
		fUseGetters= use;
	}

	/**
	 * Determines whether getters should be preferred to resolve visibility issues.
	 * <p>
	 * This method must be called before calling {@link JavaElementVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is to use setters where possible.
	 * 
	 * @param use
	 */
	public final void setUseSetters(final boolean use) {
		fUseSetters= use;
	}

	/**
	 * Sets the severity of visibility messages.
	 * 
	 * @param severity the severity of visibility messages
	 */
	public final void setVisibilitySeverity(final int severity) {
		Assert.isTrue(isStatusSeverity(severity));
		fVisibilitySeverity= severity;
	}
}