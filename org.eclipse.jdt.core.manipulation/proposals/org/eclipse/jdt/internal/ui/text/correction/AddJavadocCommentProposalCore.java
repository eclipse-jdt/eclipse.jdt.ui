/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - add module-info support
 *     Microsoft Corporation - read formatting options from the compilation unit
 *     Red Hat Inc. - body copied from JavadocTagsSubProcessor
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.manipulation.CUCorrectionProposalCore;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;

import org.eclipse.jdt.internal.ui.JavaUIStatus;

public class AddJavadocCommentProposalCore extends CUCorrectionProposalCore {
 	private final int fInsertPosition;
	private final String fComment;

	public AddJavadocCommentProposalCore(String name, ICompilationUnit cu, int relevance, int insertPosition, String comment) {
		super(name, cu, relevance);
		fInsertPosition= insertPosition;
		fComment= comment;
	}

	@Override
	public void addEdits(IDocument document, TextEdit rootEdit) throws CoreException {
		try {
			String lineDelimiter= TextUtilities.getDefaultLineDelimiter(document);
			final ICompilationUnit unit= getCompilationUnit();
			IRegion region= document.getLineInformationOfOffset(fInsertPosition);

			String lineContent= document.get(region.getOffset(), region.getLength());
			String indentString= Strings.getIndentString(lineContent, unit);
			String str= Strings.changeIndent(fComment, 0, unit, indentString, lineDelimiter);
			InsertEdit edit= new InsertEdit(fInsertPosition, str);
			rootEdit.addChild(edit);
			if (fComment.charAt(fComment.length() - 1) != '\n') {
				rootEdit.addChild(new InsertEdit(fInsertPosition, lineDelimiter));
				rootEdit.addChild(new InsertEdit(fInsertPosition, indentString));
			}
		} catch (BadLocationException e) {
			throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
		}
	}
}