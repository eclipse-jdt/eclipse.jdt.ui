package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.AbstractOpenJavaElementAction;



/**
 * This action opens a java editor on the element represented by text selection of
 * the connected java source viewer.
 */
public class OpenOnSelectionAction extends AbstractOpenJavaElementAction {
		
	private String fDialogTitle;
	private String fDialogMessage;
	protected ITextEditor fEditor;
	
	
	/**
	 * Creates a new action with the given label and image.
	 */
	protected OpenOnSelectionAction() {
		super();
		
		setText(JavaEditorMessages.getString("OpenOnSelection.label")); //$NON-NLS-1$
		setToolTipText(JavaEditorMessages.getString("OpenOnSelection.tooltip")); //$NON-NLS-1$
		setDescription(JavaEditorMessages.getString("OpenOnSelection.description")); //$NON-NLS-1$
		setDialogTitle(JavaEditorMessages.getString("OpenOnSelection.dialog.title")); //$NON-NLS-1$
		setDialogMessage(JavaEditorMessages.getString("OpenOnSelection.dialog.message")); //$NON-NLS-1$
	}
	
	/**
	 * Creates a new action with the given image.
	 */
	public OpenOnSelectionAction(ImageDescriptor image) {
		this();
		setImageDescriptor(image);
	}
	
	protected void setDialogTitle(String title) {
		fDialogTitle= title;
	}
	
	protected void setDialogMessage(String message) {
		fDialogMessage= message;
	}
	
	public void setContentEditor(ITextEditor editor) {
		fEditor= editor;
		setEnabled(editor != null);
	}
	
	protected ICodeAssist getCodeAssist() {	
		IEditorInput input= fEditor.getEditorInput();
		if (input instanceof IClassFileEditorInput) {
			IClassFileEditorInput cfInput= (IClassFileEditorInput) input;
			return cfInput.getClassFile();
		}
		
		IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
		return manager.getWorkingCopy(input);
	}
	
	/**
	 * @see IAction#actionPerformed
	 */
	public void run() {
		
		ICodeAssist resolve= getCodeAssist();
		if (resolve != null && fEditor.getSelectionProvider() != null) {
			ITextSelection selection= (ITextSelection) fEditor.getSelectionProvider().getSelection();
			selection= expandToWord(selection);
			try {
				if (selection.getLength() > 0) {
					IJavaElement[] result= resolve.codeSelect(selection.getOffset(), selection.getLength());
					if (result != null && result.length > 0) {
						List filtered= filterResolveResults(result);
						IJavaElement selected= selectJavaElement(filtered, getShell(), fDialogTitle, fDialogMessage);
						if (selected != null) {
							open(selected);
							return;
						}
					}
				} else {
					openOnEmptySelection(selection);
					return;
				}
			} catch (JavaModelException x) {
				JavaPlugin.log(x.getStatus());
			} catch (PartInitException x) {
				JavaPlugin.log(x);
			}
		}
		
		getShell().getDisplay().beep();		
	}
	
	protected void openOnEmptySelection(ITextSelection selection) throws JavaModelException, PartInitException {
		getShell().getDisplay().beep();
	}
	
	protected Shell getShell() {
		return fEditor.getSite().getShell();
	}

	private ITextSelection expandToWord(ITextSelection selection) {
		IDocument document= fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
		if (document == null)
			return selection;
		
		String text= document.get();
		
		int offset= selection.getOffset();
		char c;

		while (offset >= 0) {
			c= text.charAt(offset);
			if (!Character.isJavaIdentifierPart(c))
				break;
			--offset;
		}

		int start= offset;

		offset= selection.getOffset() + selection.getLength();
		int length= text.length();

		while (offset < length) {
			c= text.charAt(offset);
			if (!Character.isJavaIdentifierPart(c))
				break;
			++offset;
		}
		
		int end= offset;

		if (start == end)
			return new TextSelection(document, start, 0);
		else
			return new TextSelection(document, start + 1, end - start - 1);
	}
}