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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IInitializer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
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
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
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
	 * Converts the given flags into the corresponding modifier keyword.
	 * 
	 * @param flags the flags to convert
	 * @return the corresponding modifier keyword
	 */
	private static ModifierKeyword flagToVisibility(final int flags) {
		if (Flags.isPublic(flags))
			return ModifierKeyword.PUBLIC_KEYWORD;
		else if (Flags.isProtected(flags))
			return ModifierKeyword.PROTECTED_KEYWORD;
		else if (Flags.isPrivate(flags))
			return ModifierKeyword.PRIVATE_KEYWORD;
		return null;
	}

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
	 * Does the specified modifier represent a lower visibility than the required threshold?
	 * 
	 * @param modifier the visibility modifier to test
	 * @param threshold the visibility threshold to compare with
	 * @return <code>true</code> if the visibility is lower than required, <code>false</code> otherwise
	 */
	public static boolean hasLowerVisibility(final int modifier, final int threshold) {
		if (Modifier.isPrivate(threshold))
			return false;
		else if (Modifier.isPublic(threshold))
			return !Modifier.isPublic(modifier);
		else if (Modifier.isProtected(threshold))
			return !Modifier.isProtected(threshold) && !Modifier.isPublic(threshold);
		else
			return Modifier.isPrivate(modifier);
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
		Assert.isTrue(threshold == null || isVisibilityKeyword(threshold));
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
	public static boolean hasLowerVisibility(final ModifierKeyword keyword, final ModifierKeyword threshold) {
		Assert.isTrue(keyword == null || isVisibilityKeyword(keyword));
		Assert.isTrue(threshold == null || isVisibilityKeyword(threshold));
		final int keywordFlag= keyword != null ? keyword.toFlagValue() : Modifier.NONE;
		final int thresholdFlag= threshold != null ? threshold.toFlagValue() : Modifier.NONE;
		return hasLowerVisibility(keywordFlag, thresholdFlag);
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

	/** The adjusted member visibility */
	private ModifierKeyword fAdjusted;

	/** The set of already handled elements */
	private Set fElements= new HashSet();

	/** The failure message severity */
	private int fFailureSeverity= RefactoringStatus.ERROR;

	/** Should getters be used to resolve visibility issues? */
	private boolean fGetters= true;

	/** The member causing the adjustment */
	private final IMember fMember;

	/** The original member visibility */
	private ModifierKeyword fOriginal= ModifierKeyword.PRIVATE_KEYWORD;

	/** The reference element */
	private final IJavaElement fReference;

	/** The ast rewrite to use for member visibility adjustments, or <code>null</code> to use a compilation unit rewrite */
	private ASTRewrite fRewrite= null;

	/** The map of compilation units to compilation unit rewrites */
	private Map fRewrites= new HashMap(3);

	/** The incoming search scope */
	private IJavaSearchScope fScope;

	/** Should setters be used to resolve visibility issues? */
	private boolean fSetters= true;

	/** The status of the visibility adjustment */
	private RefactoringStatus fStatus= new RefactoringStatus();

	/** The type hierarchy cache */
	private final Map fTypeHierarchies= new HashMap();

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
		Assert.isTrue(!(member instanceof IInitializer));
		Assert.isTrue(reference instanceof ICompilationUnit || reference instanceof IType || reference instanceof IPackageFragment);
		fScope= RefactoringScopeFactory.createReferencedScope(new IJavaElement[] { member}, IJavaSearchScope.REFERENCED_PROJECTS | IJavaSearchScope.SOURCES | IJavaSearchScope.APPLICATION_LIBRARIES);
		fReference= reference;
		fMember= member;
		try {
			fOriginal= flagToVisibility(member.getFlags());
		} catch (JavaModelException exception) {
			JavaPlugin.log(exception);
		}
		fAdjusted= fOriginal;
	}

	/**
	 * Adjusts the visibility of the specified search match found in a java element.
	 * 
	 * @param match the search match that has been found
	 * @throws JavaModelException if the java elements could not be accessed
	 */
	private void adjustIncomingVisibility(final SearchMatch match) throws JavaModelException {
		Assert.isNotNull(match);
		final Object element= match.getElement();
		if (element instanceof IMember) {
			final IMember member= (IMember) element;
			final ModifierKeyword threshold= computeIncomingVisibilityThreshold(member, fReference);
			if (hasLowerVisibility(fAdjusted, threshold))
				fAdjusted= threshold;
		}
	}

	/**
	 * Adjusts the visibility of the member based on the incoming references represented by the specified search result groups.
	 * 
	 * @param groups the search result groups representing the references
	 * @param monitor the progress monitor to use
	 * @throws JavaModelException if the java elements could not be accessed
	 */
	private void adjustIncomingVisibility(final SearchResultGroup[] groups, final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(groups);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", groups.length); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("JavaElementVisibilityProvider.checking")); //$NON-NLS-1$
			SearchMatch[] matches= null;
			SearchResultGroup group= null;
			for (int index= 0; index < groups.length; index++) {
				group= groups[index];
				matches= group.getSearchResults();
				for (int offset= 0; offset < matches.length; offset++)
					adjustIncomingVisibility(matches[offset]);
				monitor.worked(1);
			}
			if (hasLowerVisibility(fOriginal, fAdjusted)) {
				final CompilationUnitRewrite rewriter= getCompilationUnitRewrite(fMember.getCompilationUnit());
				final ASTRewrite rewrite= (fRewrite != null) ? fRewrite : rewriter.getASTRewrite();
				final BodyDeclaration declaration= ASTNodeSearchUtil.getBodyDeclarationNode(fMember, rewriter.getRoot());
				if (declaration != null) {
					ModifierRewrite.create(rewrite, declaration).setVisibility(fAdjusted != null ? fAdjusted.toFlagValue() : Modifier.NONE, null);
					fElements.add(fMember);
				}
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Adjusts the visibility of the referenced body declaration.
	 * 
	 * @param rewrite the compilation unit rewrite to use
	 * @param declaration the body declaration where to adjust the visibility
	 * @param threshold the visibility keyword representing the required visibility, or <code>null</code> for default visibility
	 * @param binding the binding of the body declaration
	 * @param template the message template to use
	 */
	private void adjustOutgoingVisibility(final CompilationUnitRewrite rewrite, final BodyDeclaration declaration, final ModifierKeyword threshold, final IBinding binding, final String template) {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(declaration);
		Assert.isTrue(threshold == null || isVisibilityKeyword(threshold));
		Assert.isNotNull(binding);
		if (hasLowerVisibility((List) declaration.getStructuralProperty(declaration.getModifiersProperty()), threshold)) {
			final RefactoringStatusEntry entry= new RefactoringStatusEntry(fVisibilitySeverity, RefactoringCoreMessages.getFormattedString(template, new String[] { BindingLabels.getFullyQualified(binding), getLabel(threshold)}), JavaStatusContext.create(rewrite.getCu(), declaration));
			if (fStatus.getEntries(fgComparator, entry).length == 0) {
				fStatus.addEntry(entry);
				ModifierRewrite.create(rewrite.getASTRewrite(), declaration).setVisibility(threshold != null ? threshold.toFlagValue() : Modifier.NONE, rewrite.createGroupDescription(RefactoringCoreMessages.getFormattedString("JavaElementVisibilityAdjustor.change_visibility", getLabel(threshold)))); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Adjusts the visibilities of the referenced element from the search match found in a compilation unit.
	 * 
	 * @param rewrite the compilation unit rewrite to use
	 * @param match the search match representing the element declaration
	 * @throws JavaModelException if the visibility could not be determined
	 */
	private void adjustOutgoingVisibility(final CompilationUnitRewrite rewrite, final SearchMatch match) throws JavaModelException {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(match);
		final Object element= match.getElement();
		if (element instanceof IMember && !fElements.contains(element)) {
			final ModifierKeyword threshold= computeOutgoingVisibilityThreshold(fReference, (IMember) element);
			final ASTNode node= ASTNodeSearchUtil.findNode(match, rewrite.getRoot());
			if (node instanceof MethodDeclaration) {
				final MethodDeclaration declaration= (MethodDeclaration) node;
				adjustOutgoingVisibility(rewrite, declaration, threshold, declaration.resolveBinding(), "JavaElementVisibilityAdjustor.change_visibility_method_warning"); //$NON-NLS-1$
				fElements.add(element);
			} else if (node instanceof SimpleName) {
				final ASTNode parent= node.getParent();
				if (parent instanceof VariableDeclarationFragment) {
					final VariableDeclarationFragment fragment= (VariableDeclarationFragment) parent;
					adjustOutgoingVisibility(rewrite, match, threshold, fragment, (FieldDeclaration) fragment.getParent());
				} else if (parent instanceof AbstractTypeDeclaration) {
					final AbstractTypeDeclaration declaration= (AbstractTypeDeclaration) parent;
					adjustOutgoingVisibility(rewrite, declaration, threshold, declaration.resolveBinding(), "JavaElementVisibilityAdjustor.change_visibility_type_warning"); //$NON-NLS-1$
					fElements.add(element);
				}
			}
		}
	}

	/**
	 * Adjusts the visibility of the referenced field from the search match found in a compilation unit.
	 * 
	 * @param rewrite the compilation unit rewrite to use
	 * @param match the search match representing the field declaration
	 * @param threshold the visibility threshold, or <code>null</code> for default visibility
	 * @param fragment the field declaration fragment
	 * @param declaration the field declaration
	 */
	private void adjustOutgoingVisibility(final CompilationUnitRewrite rewrite, final SearchMatch match, final ModifierKeyword threshold, final VariableDeclarationFragment fragment, final FieldDeclaration declaration) {
		Assert.isNotNull(rewrite);
		Assert.isNotNull(match);
		Assert.isTrue(threshold == null || isVisibilityKeyword(threshold));
		Assert.isNotNull(fragment);
		Assert.isNotNull(declaration);
		if (hasLowerVisibility((List) declaration.getStructuralProperty(declaration.getModifiersProperty()), threshold)) {
			final IField field= (IField) match.getElement();
			if (field != null) {
				if (fGetters) {
					try {
						final IMethod getter= GetterSetterUtil.getGetter(field);
						if (getter != null && getter.exists()) {
							final MethodDeclaration method= ASTNodeSearchUtil.getMethodDeclarationNode(getter, rewrite.getRoot());
							if (method != null) {
								adjustOutgoingVisibility(rewrite, method, threshold, method.resolveBinding(), "JavaElementVisibilityAdjustor.change_visibility_method_warning"); //$NON-NLS-1$
								fElements.add(getter);

								// TODO implement

								fElements.add(field);
							}
						}
					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
					}
				} else if (fSetters) {
					try {
						final IMethod setter= GetterSetterUtil.getSetter(field);
						if (setter != null && setter.exists()) {
							final MethodDeclaration method= ASTNodeSearchUtil.getMethodDeclarationNode(setter, rewrite.getRoot());
							if (method != null) {
								adjustOutgoingVisibility(rewrite, method, threshold, method.resolveBinding(), "JavaElementVisibilityAdjustor.change_visibility_method_warning"); //$NON-NLS-1$
								fElements.add(setter);

								// TODO implement

								fElements.add(field);
							}
						}
					} catch (JavaModelException exception) {
						JavaPlugin.log(exception);
					}
				}
				if (!fElements.contains(field)) {
					final RefactoringStatusEntry entry= new RefactoringStatusEntry(fVisibilitySeverity, RefactoringCoreMessages.getFormattedString("JavaElementVisibilityAdjustor.change_visibility_field_warning", new String[] { BindingLabels.getFullyQualified(fragment.resolveBinding()), getLabel(threshold)}), JavaStatusContext.create(rewrite.getCu(), fragment)); //$NON-NLS-1$
					if (fStatus.getEntries(fgComparator, entry).length == 0) {
						fStatus.addEntry(entry);
						if (declaration.fragments().size() == 1)
							ModifierRewrite.create(rewrite.getASTRewrite(), declaration).setVisibility(threshold != null ? threshold.toFlagValue() : Modifier.NONE, rewrite.createGroupDescription(RefactoringCoreMessages.getFormattedString("JavaElementVisibilityAdjustor.change_visibility", getLabel(threshold)))); //$NON-NLS-1$
						else {
							final VariableDeclarationFragment newFragment= rewrite.getASTRewrite().getAST().newVariableDeclarationFragment();
							newFragment.setName((SimpleName) ASTNode.copySubtree(rewrite.getASTRewrite().getAST(), fragment.getName()));
							final FieldDeclaration newDeclaration= rewrite.getASTRewrite().getAST().newFieldDeclaration(newFragment);
							newDeclaration.setType((Type) ASTNode.copySubtree(rewrite.getASTRewrite().getAST(), declaration.getType()));
							final AbstractTypeDeclaration type= (AbstractTypeDeclaration) declaration.getParent();
							rewrite.getASTRewrite().getListRewrite(type, type.getBodyDeclarationsProperty()).insertAfter(newDeclaration, declaration, null);
							rewrite.getASTRewrite().getListRewrite(declaration, FieldDeclaration.FRAGMENTS_PROPERTY).remove(fragment, rewrite.createGroupDescription(RefactoringCoreMessages.getFormattedString("JavaElementVisibilityAdjustor.change_visibility", getLabel(threshold)))); //$NON-NLS-1$
						}
					}
					fElements.add(field);
				}
			}
		}
	}

	/**
	 * Adjusts the visibilities of the outgoing references from the member represented by the specified search result groups.
	 * 
	 * @param groups the search result groups representing the references
	 * @param monitor the progress monitor to us
	 * @throws JavaModelException if the visibility could not be determined
	 */
	private void adjustOutgoingVisibility(final SearchResultGroup[] groups, final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(groups);
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", groups.length); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("JavaElementVisibilityProvider.checking")); //$NON-NLS-1$
			IJavaElement element= null;
			SearchMatch[] matches= null;
			SearchResultGroup group= null;
			CompilationUnitRewrite rewrite= null;
			for (int index= 0; index < groups.length; index++) {
				group= groups[index];
				element= JavaCore.create(group.getResource());
				if (element instanceof ICompilationUnit) {
					rewrite= getCompilationUnitRewrite((ICompilationUnit) element);
					matches= group.getSearchResults();
					for (int offset= 0; offset < matches.length; offset++)
						adjustOutgoingVisibility(rewrite, matches[offset]);
				} else if (element != null)
					fStatus.merge(RefactoringStatus.createStatus(fFailureSeverity, RefactoringCoreMessages.getFormattedString("JavaElementVisibilityAdjustor.binary.outgoing.project", new String[] { element.getJavaProject().getElementName(), getLabel(fMember)}), null, null, RefactoringStatusEntry.NO_CODE, null)); //$NON-NLS-1$
				else if (group.getResource() != null)
					fStatus.merge(RefactoringStatus.createStatus(fFailureSeverity, RefactoringCoreMessages.getFormattedString("JavaElementVisibilityAdjustor.binary.outgoing.resource", new String[] { group.getResource().getName(), getLabel(fMember)}), null, null, RefactoringStatusEntry.NO_CODE, null)); //$NON-NLS-1$
				monitor.worked(1);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Adjusts the visibilities of the referenced and referencing elements.
	 * 
	 * @param monitor the progress monitor to use
	 * @throws JavaModelException if an error occurs during search
	 */
	public final void adjustVisibility(final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(monitor);
		try {
			monitor.beginTask("", 4); //$NON-NLS-1$
			monitor.setTaskName(RefactoringCoreMessages.getString("JavaElementVisibilityProvider.checking")); //$NON-NLS-1$
			final RefactoringSearchEngine2 engine= new RefactoringSearchEngine2(SearchPattern.createPattern(fMember, IJavaSearchConstants.REFERENCES));
			engine.setScope(fScope);
			engine.setStatus(fStatus);
			engine.searchPattern(new SubProgressMonitor(monitor, 1));
			adjustIncomingVisibility((SearchResultGroup[]) engine.getResults(), new SubProgressMonitor(monitor, 1));
			engine.clearResults();
			engine.searchReferencedTypes(fMember, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			engine.searchReferencedFields(fMember, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			engine.searchReferencedMethods(fMember, new SubProgressMonitor(monitor, 1, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
			adjustOutgoingVisibility((SearchResultGroup[]) engine.getResults(), new SubProgressMonitor(monitor, 1));
		} finally {
			fTypeHierarchies.clear();
			monitor.done();
		}
	}

	/**
	 * Computes the visibility threshold for the referenced element.
	 * 
	 * @param referencing the referencing element
	 * @param referenced the referenced element
	 * @return the visibility keyword corresponding to the threshold, or <code>null</code> for default visibility
	 * @throws JavaModelException if the java elements could not be accessed
	 */
	private ModifierKeyword computeIncomingVisibilityThreshold(final IMember referencing, final IJavaElement referenced) throws JavaModelException {
		Assert.isTrue(referenced instanceof ICompilationUnit || referenced instanceof IType || referenced instanceof IPackageFragment);
		return computeOutgoingVisibilityThreshold(referenced, referencing instanceof IInitializer ? referencing.getDeclaringType() : referencing);
	}

	/**
	 * Computes the visibility threshold for the referenced element.
	 * 
	 * @param referencing the referencing element
	 * @param referenced the referenced element
	 * @return the visibility keyword corresponding to the threshold, or <code>null</code> for default visibility
	 * @throws JavaModelException if the java elements could not be accessed
	 */
	private ModifierKeyword computeOutgoingVisibilityThreshold(final IJavaElement referencing, final IMember referenced) throws JavaModelException {
		Assert.isTrue(referencing instanceof ICompilationUnit || referencing instanceof IType || referencing instanceof IPackageFragment);
		Assert.isTrue(referenced instanceof IType || referenced instanceof IField || referenced instanceof IMethod);
		final int referenceType= referencing.getElementType();
		final int referencedType= referenced.getElementType();
		ModifierKeyword keyword= ModifierKeyword.PUBLIC_KEYWORD;
		switch (referencedType) {
			case IJavaElement.TYPE:
				final IType typeReferenced= (IType) referenced;
				switch (referenceType) {
					case IJavaElement.COMPILATION_UNIT:
						final ICompilationUnit unit= (ICompilationUnit) referencing;
						if (typeReferenced.getCompilationUnit().equals(unit))
							keyword= ModifierKeyword.PRIVATE_KEYWORD;
						else if (typeReferenced.getCompilationUnit().getParent().equals(unit.getParent()))
							keyword= null;
						break;
					case IJavaElement.TYPE:
						final IType type= (IType) referencing;
						if (typeReferenced.getCompilationUnit().equals(type.getCompilationUnit()))
							keyword= ModifierKeyword.PRIVATE_KEYWORD;
						else if (typeReferenced.getCompilationUnit().getParent().equals(type.getCompilationUnit().getParent()))
							keyword= null;
						break;
					case IJavaElement.PACKAGE_FRAGMENT:
						final IPackageFragment fragment= (IPackageFragment) referencing;
						if (typeReferenced.getPackageFragment().equals(fragment))
							keyword= null;
						break;
					default:
						Assert.isTrue(false);
				}
				break;
			case IJavaElement.FIELD:
				final IField fieldReferenced= (IField) referenced;
				switch (referenceType) {
					case IJavaElement.COMPILATION_UNIT:
						final ICompilationUnit unit= (ICompilationUnit) referencing;
						if (fieldReferenced.getCompilationUnit().equals(unit))
							keyword= ModifierKeyword.PRIVATE_KEYWORD;
						else if (fieldReferenced.getCompilationUnit().getParent().equals(unit.getParent()))
							keyword= null;
						break;
					case IJavaElement.TYPE:
						final IType type= (IType) referencing;
						if (fieldReferenced.getDeclaringType().equals(type))
							keyword= ModifierKeyword.PRIVATE_KEYWORD;
						else if (fieldReferenced.getCompilationUnit().equals(type.getCompilationUnit()))
							keyword= ModifierKeyword.PRIVATE_KEYWORD;
						else if (fieldReferenced.getCompilationUnit().getParent().equals(type.getCompilationUnit().getParent()))
							keyword= null;
						break;
					case IJavaElement.PACKAGE_FRAGMENT:
						final IPackageFragment fragment= (IPackageFragment) referencing;
						if (fieldReferenced.getDeclaringType().getPackageFragment().equals(fragment))
							keyword= null;
						break;
					default:
						Assert.isTrue(false);
				}
				break;
			case IJavaElement.METHOD:
				final IMethod methodReferenced= (IMethod) referenced;
				switch (referenceType) {
					case IJavaElement.COMPILATION_UNIT:
						final ICompilationUnit unit= (ICompilationUnit) referencing;
						if (methodReferenced.getCompilationUnit().equals(unit))
							keyword= ModifierKeyword.PRIVATE_KEYWORD;
						else if (methodReferenced.getCompilationUnit().getParent().equals(unit.getParent()))
							keyword= null;
						break;
					case IJavaElement.TYPE:
						final IType type= (IType) referencing;
						if (methodReferenced.getDeclaringType().equals(type))
							keyword= ModifierKeyword.PRIVATE_KEYWORD;
						else if (methodReferenced.getCompilationUnit().equals(type.getCompilationUnit()))
							keyword= ModifierKeyword.PRIVATE_KEYWORD;
						else if (methodReferenced.getCompilationUnit().getParent().equals(type.getCompilationUnit().getParent()))
							keyword= null;
						break;
					case IJavaElement.PACKAGE_FRAGMENT:
						final IPackageFragment fragment= (IPackageFragment) referencing;
						if (methodReferenced.getDeclaringType().getPackageFragment().equals(fragment))
							keyword= null;
						break;
					default:
						Assert.isTrue(false);
				}
				break;
			default:
				Assert.isTrue(false);
		}
		return keyword;
	}

	/**
	 * Returns the set of elements already adjusted.
	 * 
	 * @return the set of already adjusted elements
	 */
	public final Set getAdjustedElements() {
		return fElements;
	}

	/**
	 * Returns the adjusted visibility of the member.
	 * 
	 * @return the adjusted visibility, or <code>null</code> for default visibility
	 */
	public final ModifierKeyword getAdjustedVisibility() {
		return fAdjusted;
	}

	/**
	 * Returns a compilation unit rewrite for the specified compilation unit.
	 * 
	 * @param unit the compilation unit to get the rewrite for
	 * @return the rewrite for the compilation unit
	 */
	private CompilationUnitRewrite getCompilationUnitRewrite(final ICompilationUnit unit) {
		Assert.isNotNull(unit);
		CompilationUnitRewrite rewrite= (CompilationUnitRewrite) fRewrites.get(unit);
		if (rewrite == null)
			rewrite= new CompilationUnitRewrite(unit);
		return rewrite;
	}

	/**
	 * Returns a cached type hierarchy for the specified type.
	 * 
	 * @param type the type to get the hierarchy for
	 * @param monitor the progress monitor to use
	 * @return the type hierarchy
	 * @throws JavaModelException if the type hierarchy could not be created
	 */
	private ITypeHierarchy getHierarchy(final IType type, final IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(type);
		ITypeHierarchy hierarchy= null;
		try {
			hierarchy= (ITypeHierarchy) fTypeHierarchies.get(type);
			if (hierarchy == null)
				hierarchy= type.newTypeHierarchy(monitor);
		} finally {
			monitor.done();
		}
		return hierarchy;
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
	 * Sets the set of elements considered to be already adjusted.
	 * <p>
	 * This method must be called before calling {@link JavaElementVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is an empty set.
	 * 
	 * @param elements the set of elements to set
	 */
	public final void setAdjustedElements(final Set elements) {
		Assert.isNotNull(elements);
		fElements= elements;
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
	 * Determines whether getters should be preferred to resolve visibility issues.
	 * <p>
	 * This method must be called before calling {@link JavaElementVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is to use getters where possible.
	 * 
	 * @param use
	 */
	public final void setGetters(final boolean use) {
		fGetters= use;
	}

	/**
	 * Sets the ast rewrite to use for member visibility adjustments.
	 * <p>
	 * This method must be called before calling {@link JavaElementVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is to use a compilation unit rewrite.
	 * 
	 * @param rewrite the ast rewrite to set
	 */
	public final void setRewrite(final ASTRewrite rewrite) {
		fRewrite= rewrite;
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
	 * Determines whether getters should be preferred to resolve visibility issues.
	 * <p>
	 * This method must be called before calling {@link JavaElementVisibilityAdjustor#adjustVisibility(IProgressMonitor)}. The default is to use setters where possible.
	 * 
	 * @param use
	 */
	public final void setSetters(final boolean use) {
		fSetters= use;
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
	 * Sets the severity of visibility messages.
	 * 
	 * @param severity the severity of visibility messages
	 */
	public final void setVisibilitySeverity(final int severity) {
		Assert.isTrue(isStatusSeverity(severity));
		fVisibilitySeverity= severity;
	}
}