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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.ImportOrganizePreferencePage;

/**
 * If passed compilation unit is not null, the replacement string will be seen as a qualified type name.
  */
public class JavaTypeCompletionProposal extends JavaCompletionProposal {
	
	private ICompilationUnit fCompilationUnit; 

	public JavaTypeCompletionProposal(String replacementString, ICompilationUnit cu, int replacementOffset, int replacementLength, Image image, String displayString, int relevance) {
		super(replacementString, replacementOffset, replacementLength, image, displayString, relevance);
		fCompilationUnit= cu;
	}
	
	/**
	 * To be o
	 */
	protected String updateReplacementString(IDocument document, char trigger, int offset, ImportsStructure impStructure) throws CoreException, BadLocationException {
		if (impStructure != null) {
			IType[] types= impStructure.getCompilationUnit().getTypes();
			if (types.length > 0 && types[0].getSourceRange().getOffset() <= offset) {
				// ignore positions above type.
				return impStructure.addImport(getReplacementString());
			}
		}
		return null;
	}
	
	
	/* (non-Javadoc)
	 * @see ICompletionProposalExtension#apply(IDocument, char, int)
	 */
	public void apply(IDocument document, char trigger, int offset) {
		try {
			ImportsStructure impStructure= null;
			
			if (fCompilationUnit != null) {
				String[] prefOrder= ImportOrganizePreferencePage.getImportOrderPreference();
				int threshold= ImportOrganizePreferencePage.getImportNumberThreshold();					
				impStructure= new ImportsStructure(fCompilationUnit, prefOrder, threshold, true);
			}
			
			String replacementString= updateReplacementString(document, trigger, offset, impStructure);
			if (replacementString != null) {
				setReplacementString(replacementString);
				setCursorPosition(replacementString.length());
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
}
