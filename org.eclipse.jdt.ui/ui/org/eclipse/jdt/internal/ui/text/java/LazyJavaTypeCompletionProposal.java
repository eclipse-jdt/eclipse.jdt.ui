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
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * If passed compilation unit is not null, the replacement string will be seen as a qualified type name.
  */
public class LazyJavaTypeCompletionProposal extends LazyJavaCompletionProposal {
	/** Triggers for types. Do not modify. */
	protected final static char[] TYPE_TRIGGERS= new char[] { '.', '\t', '[', '(', ' ' };

	protected final ICompilationUnit fCompilationUnit;

	/** The fully qualified type name. */
	private String fFullyQualifiedTypeName;


	public LazyJavaTypeCompletionProposal(CompletionProposal proposal, ICompilationUnit cu) {
		super(proposal);
		fCompilationUnit= cu;
		fFullyQualifiedTypeName= null;
	}
	
	
	protected final String getFullyQualifiedTypeName() {
		if (fFullyQualifiedTypeName == null) {
			fFullyQualifiedTypeName= String.valueOf(Signature.toCharArray(fProposal.getSignature()));
		}
		return fFullyQualifiedTypeName;
	}
	
	protected final String getUnqualifiedTypeName() {
		return Signature.getSimpleName(getFullyQualifiedTypeName());
	}

	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportsStructure impStructure) throws CoreException, BadLocationException {
		// avoid adding imports when inside imports container
		if (impStructure != null) {
			String replacementString= getReplacementString();
			String qualifiedType= getFullyQualifiedTypeName();
			if (qualifiedType.indexOf('.') != -1 && replacementString.startsWith(qualifiedType) && !replacementString.endsWith(String.valueOf(';'))) {
				IType[] types= impStructure.getCompilationUnit().getTypes();
				if (types.length > 0 && types[0].getSourceRange().getOffset() <= offset) {
					// ignore positions above type.
					setReplacementString(impStructure.addImport(getReplacementString()));
					return true;
				}
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see ICompletionProposalExtension#apply(IDocument, char, int)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		try {
			ImportsStructure impStructure= null;

			if (fCompilationUnit != null && allowAddingImports()) {
				IJavaProject project= fCompilationUnit.getJavaProject();
				String[] prefOrder= JavaPreferencesSettings.getImportOrderPreference(project);
				int threshold= JavaPreferencesSettings.getImportNumberThreshold(project);
				impStructure= new ImportsStructure(fCompilationUnit, prefOrder, threshold, true);
			}

			boolean importAdded= updateReplacementString(document, trigger, offset, impStructure);

			if (importAdded)
				setCursorPosition(getReplacementString().length());

			super.apply(document, trigger, offset);

			if (importAdded && impStructure != null) {
				int oldLen= document.getLength();
				impStructure.getResultingEdits(document, new NullProgressMonitor()).apply(document, TextEdit.UPDATE_REGIONS);
				setReplacementOffset(getReplacementOffset() + document.getLength() - oldLen);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}

	private boolean allowAddingImports() {
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		return preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_ADDIMPORT);
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
	 * see http://dev.eclipse.org/bugs/show_bug.cgi?id=39439
	 */
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		boolean isValid= super.validate(document, offset, event);
		if (isValid)
			return true;

		return
			startsWith(document, offset, getUnqualifiedTypeName()) ||
			startsWith(document, offset, getFullyQualifiedTypeName());
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.java.JavaCompletionProposal#getCompletionText()
	 */
	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return getUnqualifiedTypeName();
	}
	
	protected char[] computeTriggerCharacters() {
		return TYPE_TRIGGERS;
	}
	
	protected ProposalInfo computeProposalInfo() {
		if (fCompilationUnit != null) {
			IJavaProject project= fCompilationUnit.getJavaProject();
			if (project != null)
				return new TypeProposalInfo(project, fProposal);
		}
		return super.computeProposalInfo();
	}

	protected String computeSortString() {
		// try fast sort string to avoid display string creation
		return getUnqualifiedTypeName() + Character.MIN_VALUE + getFullyQualifiedTypeName();
	}
}
