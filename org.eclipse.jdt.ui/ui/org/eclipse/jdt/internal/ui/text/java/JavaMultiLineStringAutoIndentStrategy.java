/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java;

import java.util.StringTokenizer;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.formatter.IndentManipulation;

import org.eclipse.jdt.internal.core.manipulation.util.Strings;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.actions.IndentAction;
import org.eclipse.jdt.internal.ui.text.correction.PreviewFeaturesSubProcessor;

public class JavaMultiLineStringAutoIndentStrategy extends JavaStringAutoIndentStrategy {
	
	public JavaMultiLineStringAutoIndentStrategy(String partitioning, IJavaProject project) {
		super(partitioning, project);
	}

	private void javaMultiLineStringIndentAfterNewLine(IDocument document, DocumentCommand command) throws BadLocationException {

		ITypedRegion partition= TextUtilities.getPartition(document, fPartitioning, command.offset, true);
		int offset= partition.getOffset();
		int length= partition.getLength();

		if (command.offset == offset + length && document.getChar(offset + length - 1) == '\"')
			return;

		String indentation= getLineIndentation(document, command.offset);
		String delimiter= TextUtilities.getDefaultLineDelimiter(document);

		IRegion line= document.getLineInformationOfOffset(offset);
		String fullStr= document.get(line.getOffset(), line.getLength()).trim();
		String fullTextBlockText= document.get(offset, length).trim();
		boolean hasTextBlockEnded= PreviewFeaturesSubProcessor.isPreviewFeatureEnabled(fProject) && fullTextBlockText.endsWith(IndentAction.TEXT_BLOCK_STR);
		boolean isTextBlock= PreviewFeaturesSubProcessor.isPreviewFeatureEnabled(fProject) && fullStr.endsWith(IndentAction.TEXT_BLOCK_STR);
		boolean isLineDelimiter= isLineDelimiter(document, command.text);
		if (isEditorWrapStrings() && isLineDelimiter && isTextBlock) {
			if (isTextBlock) {
				indentation= getIndentation(document, command.offset);
				if (hasTextBlockEnded) {
					command.text= command.text + indentation;
				} else {
					command.text= command.text + indentation;
					if (isCloseStringsPreferenceSet()) {
						command.caretOffset= command.offset + command.text.length();
						command.shiftsCaret= false;
						command.text= command.text + System.lineSeparator() + getIndentation(document, offset) + IndentAction.TEXT_BLOCK_STR;
					}
				}
			} else {
				command.text= command.text + indentation;
			}
		} else if (command.text.length() > 1 && !isLineDelimiter && isEditorEscapeStrings()) {
			command.text= getModifiedText(command.text, indentation, delimiter, isEditorEscapeStringsNonAscii());
		}
	}

	private String getIndentation(IDocument document, int offset) throws BadLocationException {
		IRegion line= document.getLineInformationOfOffset(offset);
		String fullStrNoTrim= document.get(line.getOffset(), line.getLength());
		String indentation= getLineIndentation(document, offset);
		int startIndex= fullStrNoTrim.lastIndexOf(IndentAction.TEXT_BLOCK_STR);
		if (fullStrNoTrim.endsWith(IndentAction.TEXT_BLOCK_STR) && startIndex != -1) {
			int length= IndentAction.measureLengthInSpaces(fullStrNoTrim.substring(0, startIndex), CodeFormatterUtil.getTabWidth(fProject));
			String str= IndentAction.EMPTY_STR;
			for (int i= 0; i < length; i++) {
				str+= IndentAction.SPACE_STR;
			}
			int units= Strings.computeIndentUnits(str, fProject);
			String newStr= CodeFormatterUtil.createIndentString(units, fProject);
			int newLength= IndentManipulation.measureIndentInSpaces(newStr, CodeFormatterUtil.getTabWidth(fProject));
			if (newLength < length) {
				for (int i= newLength; i < length; i++) {
					newStr+= IndentAction.SPACE_STR;
				}
			}
			indentation= newStr;
		}
		return indentation;
	}

	/**
	 * The input string will contain line delimiter.
	 *
	 * @param inputString the given input string
	 * @param indentation the indentation
	 * @param delimiter the line delimiter
	 * @return the display string
	 */
	@Override
	protected String displayString(String inputString, String indentation, String delimiter, boolean escapeNonAscii) {

		int length= inputString.length();
		StringBuilder buffer= new StringBuilder(length);
		StringTokenizer tokenizer= new StringTokenizer(inputString, "\n\r", true); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String token= tokenizer.nextToken();
			if (token.equals("\r")) { //$NON-NLS-1$
				buffer.append('\r');
				if (tokenizer.hasMoreTokens()) {
					token= tokenizer.nextToken();
					if (token.equals("\n")) { //$NON-NLS-1$
						buffer.append('\n');
						continue;
					}
				} else {
					continue;
				}
			} else if (token.equals("\n")) { //$NON-NLS-1$
				buffer.append('\n');
				continue;
			}

			StringBuilder tokenBuffer= new StringBuilder();
			for (int i= 0; i < token.length(); i++) {
				char c= token.charAt(i);
				switch (c) {
					default:
						if (escapeNonAscii && (c < 0x20 || c >= 0x80)) {
							String hex= "0123456789ABCDEF"; //$NON-NLS-1$
							tokenBuffer.append('\\');
							tokenBuffer.append('u');
							tokenBuffer.append(hex.charAt((c >> 12) & 0xF));
							tokenBuffer.append(hex.charAt((c >> 8) & 0xF));
							tokenBuffer.append(hex.charAt((c >> 4) & 0xF));
							tokenBuffer.append(hex.charAt(c & 0xF));
						} else {
							tokenBuffer.append(c);
						}
				}
			}
			buffer.append(tokenBuffer);
		}
		return buffer.toString();
	}

	/*
	 * @see org.eclipse.jface.text.IAutoIndentStrategy#customizeDocumentCommand(IDocument, DocumentCommand)
	 */
	@Override
	public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
		try {
			if (command.text == null)
				return;
			if (isSmartMode()) {
				javaMultiLineStringIndentAfterNewLine(document, command);
			}
		} catch (BadLocationException e) {
		}
	}

	private boolean isCloseStringsPreferenceSet() {
		boolean isSet= false;
		if (fProject != null) {
			IPreferenceStore store= PreferenceConstants.getPreferenceStore();
			if (store != null) {
				isSet= store.getBoolean(PreferenceConstants.EDITOR_CLOSE_STRINGS);
			}
		}
		return isSet;
	}
}
