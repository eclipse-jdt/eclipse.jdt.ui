/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.ResourceBundle;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextUtilities;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension3;
import org.eclipse.ui.texteditor.TextEditorAction;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.JavaHeuristicScanner;
import org.eclipse.jdt.internal.ui.text.JavaIndenter;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAutoIndentStrategy;


/**
 * Indents a line or range of lines in a Java document to its correct position. No complete
 * AST must be present, the indentation is computed using heuristics. The algorith used is fast for
 * single lines, but does not store any information and therefore not so efficient for large line
 * ranges.
 * 
 * @see org.eclipse.jdt.internal.ui.text.JavaHeuristicScanner
 * @see org.eclipse.jdt.internal.ui.text.JavaIndenter
 * @since 3.0
 */
public class IndentAction extends TextEditorAction {
	
	/** The caret offset after an indent operation. */
	private int fCaretOffset;
	
	/**
	 * Creates a new instance.
	 * 
	 * @param bundle the resource bundle
	 * @param prefix the prefix to use for keys in <code>bundle</code>
	 * @param editor the text editor
	 */
	public IndentAction(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
	}
	
	/*
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		// update has been called by the framework
		if (!isEnabled() || !validateEdit())
			return;
		
		ITextSelection selection= getSelection();
		final IDocument document= getDocument();
		
		if (document != null) {
			
			final int offset= selection.getOffset();
			final int length= selection.getLength();
			final Position end= new Position(offset + length);
			final int firstLine, nLines;
			
			try {
				document.addPosition(end);
				firstLine= document.getLineOfOffset(offset);
				// check for marginal (zero-length) lines
				int minusOne= length == 0 ? 0 : 1;
				nLines= document.getLineOfOffset(offset + length - minusOne) - firstLine + 1;
			} catch (BadLocationException e) {
				// will only happen on concurrent modification
				JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, null, e));
				return;
			}
			
			Runnable runnable= new Runnable() {
				public void run() {
					IRewriteTarget target= (IRewriteTarget)getTextEditor().getAdapter(IRewriteTarget.class);
					if (target != null) {
						target.beginCompoundChange();
						target.setRedraw(false);
					}
					
					try {
						JavaHeuristicScanner scanner= new JavaHeuristicScanner(document);
						JavaIndenter indenter= new JavaIndenter(document, scanner);
						boolean hasChanged= false;
						for (int i= 0; i < nLines; i++) {
							hasChanged |= indentLine(document, firstLine + i, indenter, scanner);
						}
						
						// update caret position: move to new position when indenting just one line
						// keep selection when indenting multiple
						int newOffset= fCaretOffset;
						int newLength= 0;
						if (nLines > 1) {
							newOffset= offset;
							newLength= end.getOffset() - offset;
						}
						
						Assert.isTrue(newLength >= 0);
						Assert.isTrue(newOffset >= 0);
						
						// always reset the selection if anything was replaced
						if (hasChanged || newOffset != offset || newLength != length)
							// TODO: be less intrusive than selectAndReveal
							getTextEditor().selectAndReveal(newOffset, newLength);
						
						document.removePosition(end);
					} catch (BadLocationException e) {
						// will only happen on concurrent modification
						JavaPlugin.log(new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.OK, null, e));
						
					} finally {
						
						if (target != null) {
							target.endCompoundChange();
							target.setRedraw(true);
						}
					}
				}
			};
			
			if (nLines > 50) {
				Display display= getTextEditor().getEditorSite().getWorkbenchWindow().getShell().getDisplay();
				BusyIndicator.showWhile(display, runnable);
			} else
				runnable.run();
			
		}
	}
	
	/**
	 * Indents a single line using the java heuristic scanner. Javadoc and multiline comments are 
	 * indented as specified by the <code>JavaDocAutoIndentStrategy</code>.
	 * 
	 * @param document the document
	 * @param line the line to be indented
	 * @param indenter the java indenter
	 * @param scanner the heuristic scanner
	 * @return <code>true</code> if <code>document</code> was modified, <code>false</code> otherwise
	 * @throws BadLocationException if the document got changed concurrently 
	 */
	private boolean indentLine(IDocument document, int line, JavaIndenter indenter, JavaHeuristicScanner scanner) throws BadLocationException {
		IRegion currentLine= document.getLineInformation(line);
		int offset= currentLine.getOffset();
		
		String indent= null;
		if (offset < document.getLength()) {
			ITypedRegion partition= TextUtilities.getPartition(document, IJavaPartitions.JAVA_PARTITIONING, offset);
			String type= partition.getType();
			if (partition.getOffset() < offset
					&& type.equals(IJavaPartitions.JAVA_DOC)
					|| type.equals(IJavaPartitions.JAVA_MULTI_LINE_COMMENT)) {
				
				// TODO this is a hack
				// what I want to do
//				new JavaDocAutoIndentStrategy().indentLineAtOffset(document, offset);
//				return;
				
				IRegion previousLine= document.getLineInformation(line - 1);
				
				DocumentCommand command= new DocumentCommand() {
				};
				command.text= "\n"; //$NON-NLS-1$
				command.offset= previousLine.getOffset() + previousLine.getLength();
				new JavaDocAutoIndentStrategy(IJavaPartitions.JAVA_PARTITIONING).customizeDocumentCommand(document, command);
				int i= command.text.indexOf('*');
				if (i != -1)
					indent= command.text.substring(1, i);
				else
					indent= command.text.substring(1);
			}
		} 
		
		// standard java indentation
		if (indent == null)
			indent= indenter.computeIndentation(offset);
		
		// default is no indentation
		if (indent == null)
			indent= new String();
		
		// change document:
		// get current white space
		int lineLength= currentLine.getLength();
		int end= scanner.findNonWhitespaceForwardInAnyPartition(offset, offset + lineLength);
		int length;
		if (end == JavaHeuristicScanner.NOT_FOUND)
			length= lineLength;
		else
			length= end - offset;
		
		fCaretOffset= offset + indent.length();
		
		// only change the document if it is a real change
		if (!indent.equals(document.get(offset, length))) {
			document.replace(offset, length, indent);
			return true;
		} else
			return false;
	}
	
	/**
	 * Returns the editor's selection provider.
	 * 
	 * @return the editor's selection provider or <code>null</code>
	 */
	private ISelectionProvider getSelectionProvider() {
		ITextEditor editor= getTextEditor();
		if (editor != null) {
			return editor.getSelectionProvider();
		}
		return null;
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.IUpdate#update()
	 */
	public void update() {
		super.update();
		
		if (isEnabled())
			setEnabled(canModifyEditor() && !getSelection().isEmpty());
	}
	
	/**
	 * Enablement when tab key is pressed - the current selection has to be empty and in the
	 * whitespace in the beginning of a line.
	 */
	protected void updateForTab() {
		super.update();
		
		if (isEnabled())
			setEnabled(isSmartMode() && isValidSelection());
	}
	
	/**
	 * Returns if the current selection is valid, i.e. whether it is empty and the caret in the 
	 * whitespace at the start of a line, or covers multiple lines.
	 * 
	 * @return <code>true</code> if the selection is valid for an indent operation
	 */
	private boolean isValidSelection() {
		ITextSelection selection= getSelection();
		if (selection.isEmpty())
			return false;
		
		int offset= selection.getOffset();
		int length= selection.getLength();
		
		IDocument document= getDocument();
		if (document == null)
			return false;
		
		try {
			IRegion firstLine= document.getLineInformationOfOffset(offset);
			int lineOffset= firstLine.getOffset();
			
			// either the selection has to be empty and the caret in the WS at the line start
			// or the selection has to extend over multiple lines
			if (length == 0)
				return document.get(lineOffset, offset - lineOffset).trim().length() == 0;
			else
//				return lineOffset + firstLine.getLength() < offset + length;
				return false; // only enable for empty selections for now
			
		} catch (BadLocationException e) {
		}
		
		return false;
	}
	
	/**
	 * Returns the smart preference state.
	 * 
	 * @return <code>true</code> if smart mode is on, <code>false</code> otherwise
	 */
	private boolean isSmartMode() {
		ITextEditor editor= getTextEditor();
		
		if (editor instanceof ITextEditorExtension3)
			return ((ITextEditorExtension3) editor).getInsertMode() == ITextEditorExtension3.SMART_INSERT;
		
		return false;
	}
	
	/**
	 * Returns the document currently displayed in the editor, or <code>null</code> if none can be 
	 * obtained.
	 * 
	 * @return the current document or <code>null</code>
	 */
	private IDocument getDocument() {
		
		ITextEditor editor= getTextEditor();
		if (editor != null) {
			
			IDocumentProvider provider= editor.getDocumentProvider();
			IEditorInput input= editor.getEditorInput();
			if (provider != null && input != null)
				return provider.getDocument(input);
			
		}
		return null;
	}
	
	/**
	 * Returns the selection on the editor or an invalid selection if none can be obtained. Returns
	 * never <code>null</code>.
	 * 
	 * @return the current selection, never <code>null</code>
	 */
	private ITextSelection getSelection() {
		ISelectionProvider provider= getSelectionProvider();
		if (provider != null) {
			
			ISelection selection= provider.getSelection();
			if (selection instanceof ITextSelection)
				return (ITextSelection) selection;
		}
		
		// null object
		return TextSelection.emptySelection();
	}
	
}