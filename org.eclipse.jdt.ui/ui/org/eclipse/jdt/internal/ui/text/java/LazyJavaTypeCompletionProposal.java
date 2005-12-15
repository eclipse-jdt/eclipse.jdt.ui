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
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * If passed compilation unit is not null, the replacement string will be seen as a qualified type name.
  */
public class LazyJavaTypeCompletionProposal extends LazyJavaCompletionProposal {
	/** Triggers for types. Do not modify. */
	protected static final char[] TYPE_TRIGGERS= new char[] { '.', '\t', '[', '(', ' ' };
	/** Triggers for types in javadoc. Do not modify. */
	protected static final char[] JDOC_TYPE_TRIGGERS= new char[] { '#', '}', ' ', '.' };

	/** The compilation unit, or <code>null</code> if none is available. */
	protected final ICompilationUnit fCompilationUnit;

	private String fQualifiedName;
	private String fSimpleName;
	private IType fType;
	private ImportsStructure fImportStructure;

	public LazyJavaTypeCompletionProposal(CompletionProposal proposal, JavaContentAssistInvocationContext context) {
		super(proposal, context);
		fCompilationUnit= context.getCompilationUnit();
		fQualifiedName= null;
	}
	
	/**
	 * Returns the java model type of this type proposal.
	 *
	 * @return the java mode type of this type proposal
	 * @throws JavaModelException
	 */
	protected final IType getProposedType() throws JavaModelException {
		if (fType == null && fCompilationUnit != null) {
			fType= fCompilationUnit.getJavaProject().findType(getQualifiedTypeName());
		}
		return fType;
	}

	protected final String getQualifiedTypeName() {
		if (fQualifiedName == null)
			fQualifiedName= String.valueOf(Signature.toCharArray(Signature.getTypeErasure(fProposal.getSignature())));
		return fQualifiedName;
	}
	
	protected final String getSimpleTypeName() {
		if (fSimpleName == null)
			fSimpleName= Signature.getSimpleName(getQualifiedTypeName());
		return fSimpleName;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#computeReplacementString()
	 */
	protected String computeReplacementString() {
		if (isImportCompletion())
			return super.computeReplacementString();
		
		// TODO fix
		 if (fProposal.getKind() == CompletionProposal.TYPE_REF &&  fInvocationContext.getCoreContext().isInJavadocText())
			 return getSimpleTypeName();
		
		fImportStructure= createImportStructure();
		if (fImportStructure != null) {
			return fImportStructure.addImport(getQualifiedTypeName());
		}
		
		if (fCompilationUnit != null) {
			if (ImportsStructure.isImplicitImport(Signature.getQualifier(getQualifiedTypeName()), fCompilationUnit))
				return getSimpleTypeName();
			else
				return getQualifiedTypeName();
		}
		
		return getQualifiedTypeName();
	}

	protected final boolean isImportCompletion() {
		char[] completion= fProposal.getCompletion();
		return completion.length > 0 && completion[completion.length - 1] == ';';
	}

	private ImportsStructure createImportStructure() {
		if (fCompilationUnit != null && allowAddingImports()) {
			IJavaProject project= fCompilationUnit.getJavaProject();
			String[] prefOrder= JavaPreferencesSettings.getImportOrderPreference(project);
			int threshold= JavaPreferencesSettings.getImportNumberThreshold(project);
			try {
				return new ImportsStructure(fCompilationUnit, prefOrder, threshold, true);
			} catch (CoreException x) {
				JavaPlugin.log(x);
			}
		}
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#apply(org.eclipse.jface.text.IDocument, char, int)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		try {
			super.apply(document, trigger, offset);

			if (fImportStructure != null && fImportStructure.hasChanges()) {
				int oldLen= document.getLength();
				fImportStructure.getResultingEdits(document, new NullProgressMonitor()).apply(document, TextEdit.UPDATE_REGIONS);
				setReplacementOffset(getReplacementOffset() + document.getLength() - oldLen);
			}
			
			if (trigger == '(' && autocloseBrackets()) {
				document.replace(getReplacementOffset() + getCursorPosition(), 0, ")"); //$NON-NLS-1$
				setUpLinkedMode(document, ')');
			}
			
			rememberSelection();
			
		} catch (CoreException e) {
			JavaPlugin.log(e);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}

	/**
	 * Remembers the selection in the content assist history.
	 * 
	 * @throws JavaModelException if anything goes wrong
	 * @since 3.2
	 */
	protected final void rememberSelection() throws JavaModelException {
		IType lhs= fInvocationContext.getExpectedType();
		IType rhs= getProposedType();
		if (lhs != null && rhs != null)
			JavaPlugin.getDefault().getContentAssistHistory().remember(lhs, rhs);
	}

	/**
	 * Returns <code>true</code> if imports may be added. The return value depends on the context
	 * and preferences only and does not take into account the contents of the compilation unit or
	 * the kind of proposal. Even if <code>true</code> is returned, there may be cases where no
	 * imports are added for the proposal. For example:
	 * <ul>
	 * <li>when completing within the import section</li>
	 * <li>when completing informal javadoc references (e.g. within <code>&lt;code&gt;</code>
	 * tags)</li>
	 * <li>when completing a type that conflicts with an existing import</li>
	 * <li>when completing an implicitly imported type (same package, <code>java.lang</code>
	 * types)</li>
	 * </ul>
	 * <p>
	 * The decision whether a qualified type or the simple type name should be inserted must take
	 * into account these different scenarios.
	 * </p>
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 * 
	 * @return <code>true</code> if imports may be added, <code>false</code> if not
	 */
	protected boolean allowAddingImports() {
		if (isInJavadoc()) {
			// TODO fix
//			if (!fContext.isInJavadocFormalReference())
//				return false;
			if (fProposal.getKind() == CompletionProposal.TYPE_REF &&  fInvocationContext.getCoreContext().isInJavadocText())
				return false;
			
			if (!isJavadocProcessingEnabled())
				return false;
		}
		
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		return preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_ADDIMPORT);
	}

	private boolean isJavadocProcessingEnabled() {
		IJavaProject project= fCompilationUnit.getJavaProject();
		boolean processJavadoc;
		if (project == null)
			processJavadoc= JavaCore.ENABLED.equals(JavaCore.getOption(JavaCore.COMPILER_DOC_COMMENT_SUPPORT));
		else
			processJavadoc= JavaCore.ENABLED.equals(project.getOption(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, true));
		return processJavadoc;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#isValidPrefix(java.lang.String)
	 */
	protected boolean isValidPrefix(String prefix) {
		return isPrefix(prefix, getSimpleTypeName()) || isPrefix(prefix, getQualifiedTypeName());
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal#getCompletionText()
	 */
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return getSimpleTypeName();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#computeTriggerCharacters()
	 */
	protected char[] computeTriggerCharacters() {
		return isInJavadoc() ? JDOC_TYPE_TRIGGERS : TYPE_TRIGGERS;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#computeProposalInfo()
	 */
	protected ProposalInfo computeProposalInfo() {
		if (fCompilationUnit != null) {
			IJavaProject project= fCompilationUnit.getJavaProject();
			if (project != null)
				return new TypeProposalInfo(project, fProposal);
		}
		return super.computeProposalInfo();
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#computeSortString()
	 */
	protected String computeSortString() {
		// try fast sort string to avoid display string creation
		return getSimpleTypeName() + Character.MIN_VALUE + getQualifiedTypeName();
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.LazyJavaCompletionProposal#computeRelevance()
	 */
	protected int computeRelevance() {
		return super.computeRelevance() + fInvocationContext.getRHSHistory().getRank(getQualifiedTypeName());
	}
}
