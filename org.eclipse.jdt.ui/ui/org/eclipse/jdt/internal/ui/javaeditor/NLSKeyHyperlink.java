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
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.resources.IStorage;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.StringLiteral;

import org.eclipse.jdt.internal.corext.refactoring.nls.AccessorClassReference;
import org.eclipse.jdt.internal.corext.refactoring.nls.NLSHintHelper;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * NLS key hyperlink.
 * 
 * @since 3.1
 */
public class NLSKeyHyperlink implements IHyperlink {

	private IRegion fRegion;
	private AccessorClassReference fAccessorClassReference;
	private final Shell fShell;
	private final StringLiteral fKeyStringLiteral;

	
	/**
	 * Creates a new NLS key hyperlink.
	 * 
	 * @param region
	 * @param keyStringLiteral
	 * @param ref
	 * @param shell
	 */
	public NLSKeyHyperlink(IRegion region, StringLiteral keyStringLiteral, AccessorClassReference ref, Shell shell) {
		Assert.isNotNull(region);
		Assert.isNotNull(keyStringLiteral);
		Assert.isNotNull(ref);
		Assert.isNotNull(shell);
		
		fRegion= region;
		fKeyStringLiteral= keyStringLiteral;
		fAccessorClassReference= ref;
		fShell= shell;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#getHyperlinkRegion()
	 * @since 3.1
	 */
	public IRegion getHyperlinkRegion() {
		return fRegion;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#open()
	 * @since 3.1
	 */
	public void open() {
		IStorage propertiesFile= null;
		try {
			ITypeBinding typeBinding= fAccessorClassReference.getBinding();
			propertiesFile= NLSHintHelper.getResourceBundle(typeBinding.getJavaElement().getJavaProject(), typeBinding);
		} catch (JavaModelException e) {
			// Don't open the file
		}
		if (propertiesFile == null) {
			// Can't write to status line because it gets immediately cleared
			MessageDialog.openError(
					fShell,
					JavaEditorMessages.getString("Editor.OpenPropertiesFile.error.fileNotFound.dialogTitle"), //$NON-NLS-1$
					JavaEditorMessages.getString("Editor.OpenPropertiesFile.error.fileNotFound.dialogMessage")); //$NON-NLS-1$
			
			return;
		}
		
		IEditorPart editor;
		try {
			editor= EditorUtility.openInEditor(propertiesFile, true);
		} catch (PartInitException e) {
			handleOpenPropertiesFileFailed(propertiesFile);
			return;
		} catch (JavaModelException e) {
			handleOpenPropertiesFileFailed(propertiesFile);
			return;
		}
		
		// Reveal the key in the properties file
		if (editor instanceof ITextEditor) {
			IRegion region= null;
			// Find key in document
			IDocument document= JavaPlugin.getDefault().getPropertiesFileDocumentProvider().getDocument(editor.getEditorInput());
			if (document != null) {
				FindReplaceDocumentAdapter finder= new FindReplaceDocumentAdapter(document);
				try {
					region= finder.find(document.getLength() - 1, fKeyStringLiteral.getLiteralValue(), false, true, false, false);
				} catch (BadLocationException ex) {
				}
			}
			if (region != null)
				EditorUtility.revealInEditor(editor, region);
			else {
				EditorUtility.revealInEditor(editor, 0, 0);
				IEditorStatusLine statusLine= (IEditorStatusLine) editor.getAdapter(IEditorStatusLine.class);
				if (statusLine != null)
					statusLine.setMessage(true, JavaEditorMessages.getFormattedString("Editor.OpenPropertiesFile.error.keyNotFound", fKeyStringLiteral.getLiteralValue()), null); //$NON-NLS-1$
			}
		}
	}

	private void handleOpenPropertiesFileFailed(IStorage propertiesFile) {
		// Can't write to status line because it gets immediately cleared
		MessageDialog.openError(
				fShell,
				JavaEditorMessages.getString("Editor.OpenPropertiesFile.error.openEditor.dialogTitle"), //$NON-NLS-1$
				JavaEditorMessages.getFormattedString("Editor.OpenPropertiesFile.error.openEditor.dialogMessage", propertiesFile.getFullPath().toOSString())); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#getTypeLabel()
	 * @since 3.1
	 */
	public String getTypeLabel() {
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#getHyperlinkText()
	 * @since 3.1
	 */
	public String getHyperlinkText() {
		return null;
	}
}
