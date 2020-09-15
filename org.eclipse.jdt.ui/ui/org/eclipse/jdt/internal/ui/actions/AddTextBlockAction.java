/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.actions;


import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ITypeRoot;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;
import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.text.java.JavaMultiLineStringAutoIndentStrategy;

public class AddTextBlockAction extends SelectionDispatchAction {

	private CompilationUnitEditor fEditor;

	/**
	 * Creates a new <code>AddTextBlockAction</code>. The action requires that the selection
	 * provided by the site's selection provider is of type <code>
	 * org.eclipse.jface.viewers.IStructuredSelection</code>.
	 *
	 * @param site the site providing context information for this action
	 */
	private AddTextBlockAction(IWorkbenchSite site) {
		super(site);
		setText(ActionMessages.AddTextBlockAction_label);
		setDescription(ActionMessages.AddTextBlockAction_description);
		setToolTipText(ActionMessages.AddTextBlockAction_tooltip);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ADD_TEXTBLOCK_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 *
	 * @param editor the compilation unit editor
	 *
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public AddTextBlockAction(CompilationUnitEditor editor) {
		this(editor.getEditorSite());
		fEditor= editor;
		setEnabled(checkEnabledEditor());
	}

	private boolean checkEnabledEditor() {
		return fEditor != null && SelectionConverter.canOperateOn(fEditor);
	}

	//---- Structured Viewer -----------------------------------------------------------

	@Override
	public void selectionChanged(IStructuredSelection selection) {
		//do nothing
	}

	@Override
	public void run(IStructuredSelection selection) {
		//do nothing
	}

	//---- Java Editor --------------------------------------------------------------

	@Override
	public void selectionChanged(ITextSelection selection) {
		//do nothing
	}

	@Override
	public void run(ITextSelection selection) {
		IEditorInput input= fEditor.getEditorInput();
		IDocumentProvider docProvider= fEditor.getDocumentProvider();
		if (docProvider == null || input == null)
			return;

		IDocument document= docProvider.getDocument(input);
		if (document == null)
			return;

		IDocumentExtension3 docExtension= null;
		if (document instanceof IDocumentExtension3)
			docExtension= (IDocumentExtension3) document;
		else
			return;
		String indentStr= IndentAction.EMPTY_STR;

		IJavaProject javaProject= getProject();
		if (javaProject == null) {
			return;
		}
		try {
			indentStr= IndentAction.getIndentationAsPerTextBlockSettings(document, selection.getOffset(), javaProject);
		} catch (BadLocationException e) {
			return;
		}

		try {
			int selectionOffset= selection.getOffset();
			boolean setCaratPosition= selection.getLength() > 0 ? false : true;
			ITypedRegion partition= docExtension.getPartition(IJavaPartitions.JAVA_PARTITIONING, selectionOffset, false);
			if (!canAddTextBlock(partition, javaProject, selection)) {
				return;
			}
			DocumentCommand command= getDocumentCommand(document, javaProject, selection, indentStr);
			addTextBlock(document, command, setCaratPosition);
		} catch (BadLocationException | BadPartitioningException e) {
			//do nothing
		}
	}

	private DocumentCommand getDocumentCommand(IDocument document, IJavaProject javaProject, ITextSelection selection, String indentStr) {
		DocumentCommand cmd= new DocumentCommand() { //empty
		};

		if (selection.getLength() > 0) {
			try {
				indentStr= getLineIndentation(document, selection.getOffset());
			} catch (BadLocationException e) {
				//do nothing
			}
		}
		cmd.offset= selection.getOffset();
		cmd.length= selection.getLength();
		cmd.text= IndentAction.TEXT_BLOCK_STR + System.lineSeparator() + indentStr;
		cmd.doit= true;
		cmd.shiftsCaret= true;
		cmd.caretOffset= selection.getOffset() + selection.getLength() + cmd.text.length();
		cmd.text+= selection.getText();
		if (JavaMultiLineStringAutoIndentStrategy.isCloseStringsPreferenceSet(javaProject)) {
			cmd.text+= System.lineSeparator() + indentStr + IndentAction.TEXT_BLOCK_STR;
		}

		return cmd;
	}

	protected final void replace(IDocument document, int offset, int length, String string) throws BadLocationException {
		document.replace(offset, length, string);
	}

	private boolean canAddTextBlock(ITypedRegion partition, IJavaProject javaProject, ITextSelection selection) {
		if (fEditor == null) {
			return false;
		}
		if (!JavaModelUtil.is15OrHigher(javaProject)) {
			return false;
		}
		boolean addTextBlock= true;
		String partitionType= partition.getType();
		if (selection.getLength() == 0) {
			if (IJavaPartitions.JAVA_MULTI_LINE_STRING.equals(partitionType)
					|| IJavaPartitions.JAVA_STRING.equals(partitionType)
					|| IJavaPartitions.JAVA_DOC.equals(partitionType)
					|| IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(partitionType)
					|| IJavaPartitions.JAVA_SINGLE_LINE_COMMENT.equals(partitionType)
					|| IJavaPartitions.JAVA_CHARACTER.equals(partitionType)) {
				addTextBlock= false;
			}
		} else {
			if (IJavaPartitions.JAVA_MULTI_LINE_STRING.equals(partitionType)) {
				addTextBlock= false;
			} else if (IJavaPartitions.JAVA_STRING.equals(partitionType)
					|| IJavaPartitions.JAVA_DOC.equals(partitionType)
					|| IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(partitionType)
					|| IJavaPartitions.JAVA_SINGLE_LINE_COMMENT.equals(partitionType)
					|| IJavaPartitions.JAVA_CHARACTER.equals(partitionType)) {
				addTextBlock= false;
				if (partition.getOffset() == selection.getOffset() && partition.getLength() <= selection.getLength()) {
					addTextBlock= true;
				}
			}
		}
		return addTextBlock;
	}



	private void addTextBlock(IDocument document, DocumentCommand cmd, boolean setCaratPosition) {
		try {
			replace(document, cmd.offset, cmd.length, cmd.text);
			if (setCaratPosition) {
				selectAndReveal(cmd.caretOffset, 0);
			}
		} catch (BadLocationException e) {
			//do nothing
		}
	}

	protected String getLineIndentation(IDocument document, int offset) throws BadLocationException {
		// find start of line
		int adjustedOffset= (offset == document.getLength() ? offset - 1 : offset);
		IRegion line= document.getLineInformationOfOffset(adjustedOffset);
		int start= line.getOffset();

		// find white spaces
		int end= findEndOfWhiteSpace(document, start, offset);

		return document.get(start, end - start);
	}

	protected int findEndOfWhiteSpace(IDocument document, int offset, int end) throws BadLocationException {
		while (offset < end) {
			char c= document.getChar(offset);
			if (c != ' ' && c != '\t') {
				return offset;
			}
			offset++;
		}
		return end;
	}

	private IJavaProject getProject() {
		IJavaProject javaProject= null;
		if (fEditor != null) {
			ITypeRoot inputJavaElement= EditorUtility.getEditorInputJavaElement(fEditor, false);
			javaProject= inputJavaElement.getJavaProject();
		}
		return javaProject;
	}

	private void selectAndReveal(int newOffset, int newLength) {
		Assert.isTrue(newOffset >= 0);
		Assert.isTrue(newLength >= 0);
		ISourceViewer viewer= fEditor.getViewer();
		if (viewer != null) {
			viewer.setSelectedRange(newOffset, newLength);
		}
	}
}
