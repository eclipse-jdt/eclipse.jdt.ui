/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

/**
 * If passed compilation unit is not null, the replacement string will be seen as a qualified type name.
  */
public class JavaTypeCompletionProposal extends JavaCompletionProposal {
		
	protected final ICompilationUnit fCompilationUnit; 
	/** The type name. */
	private final String fTypeName;
	/** The package name. */
	private final String fPackageName;
	/** The unqualified type name. */
	private final String fUnqualifiedTypeName;
	/** The fully qualified type name. */
	private final String fFullyQualifiedTypeName;
	
	private static String unqualify(String typeName) {
		if (typeName == null)
			return null;
			
		final int index= typeName.lastIndexOf('.');
		return index == -1 ? typeName : typeName.substring(index + 1);			
	}
	
	private static String qualify(String typeName, String packageName) {
		if (packageName == null)	
			return typeName;
			
		if (typeName == null)
			return null;
			
		if (packageName.length() == 0)
			return typeName;

		return packageName + "." + typeName;
	}

	public JavaTypeCompletionProposal(String replacementString, ICompilationUnit cu, int replacementOffset, int replacementLength, Image image, String displayString, int relevance) {
		this(replacementString, cu, replacementOffset, replacementLength, image, displayString, relevance, null, null);
	}

	public JavaTypeCompletionProposal(String replacementString, ICompilationUnit cu, int replacementOffset, int replacementLength, Image image, String displayString, int relevance,
		String typeName, String packageName)
	{
		super(replacementString, replacementOffset, replacementLength, image, displayString, relevance);
		fCompilationUnit= cu;
		fTypeName= typeName;
		fPackageName= packageName;
		fUnqualifiedTypeName= unqualify(typeName);
		fFullyQualifiedTypeName= qualify(typeName, packageName);
	}
	
	/**
	 * To be o
	 */
	protected boolean updateReplacementString(IDocument document, char trigger, int offset, ImportsStructure impStructure) throws CoreException, BadLocationException {
		if (impStructure != null) {
			IType[] types= impStructure.getCompilationUnit().getTypes();
			if (types.length > 0 && types[0].getSourceRange().getOffset() <= offset) {
				// ignore positions above type.
				setReplacementString(impStructure.addImport(getReplacementString()));
				return true;
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
			
			if (fCompilationUnit != null) {
				IPreferenceStore store= PreferenceConstants.getPreferenceStore();
				String[] prefOrder= JavaPreferencesSettings.getImportOrderPreference(store);
				int threshold= JavaPreferencesSettings.getImportNumberThreshold(store);					
				impStructure= new ImportsStructure(fCompilationUnit, prefOrder, threshold, true);
			}
			
			if (updateReplacementString(document, trigger, offset, impStructure)) {
				setCursorPosition(getReplacementString().length());
			}
			
			super.apply(document, trigger, offset);
			
			if (impStructure != null) {
				int oldLen= document.getLength();
				impStructure.create(false, null);
				setReplacementOffset(getReplacementOffset() + document.getLength() - oldLen);
			}
		} catch (CoreException e) {
			JavaPlugin.log(e);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
		}
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension#isValidFor(IDocument, int)
	 */
	public boolean isValidFor(IDocument document, int offset) {

		boolean isValid= super.isValidFor(document, offset);
		if (isValid)
			return true;

		return
			(fUnqualifiedTypeName != null && startsWith(document, offset, fUnqualifiedTypeName)) ||
			(fFullyQualifiedTypeName != null && startsWith(document, offset, fFullyQualifiedTypeName));
	}

}
