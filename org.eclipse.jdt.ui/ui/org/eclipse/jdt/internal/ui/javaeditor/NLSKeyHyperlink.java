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

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.resources.IStorage;

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
import org.eclipse.jdt.internal.ui.propertiesfileeditor.PropertyKeyHyperlinkDetector;


/**
 * NLS key hyperlink.
 * 
 * @since 3.1
 */
public class NLSKeyHyperlink implements IHyperlink {

	private IRegion fRegion;
	private AccessorClassReference fAccessorClassReference;
	private IEditorPart fEditor;
	private final StringLiteral fKeyStringLiteral;

	
	/**
	 * Creates a new NLS key hyperlink.
	 * 
	 * @param region
	 * @param keyStringLiteral
	 * @param ref
	 * @param editor the editor which contains the hyperlink
	 */
	public NLSKeyHyperlink(IRegion region, StringLiteral keyStringLiteral, AccessorClassReference ref, IEditorPart editor) {
		Assert.isNotNull(region);
		Assert.isNotNull(keyStringLiteral);
		Assert.isNotNull(ref);
		Assert.isNotNull(editor);
		
		fRegion= region;
		fKeyStringLiteral= keyStringLiteral;
		fAccessorClassReference= ref;
		fEditor= editor;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#getHyperlinkRegion()
	 */
	public IRegion getHyperlinkRegion() {
		return fRegion;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#open()
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
			showErrorInStatusLine(fEditor, JavaEditorMessages.getString("Editor.OpenPropertiesFile.error.fileNotFound.dialogMessage")); //$NON-NLS-1$
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
			boolean found= false;
			
			// Find key in document
			IDocument document= JavaPlugin.getDefault().getPropertiesFileDocumentProvider().getDocument(editor.getEditorInput());
			if (document != null) {
				FindReplaceDocumentAdapter finder= new FindReplaceDocumentAdapter(document);
				PropertyKeyHyperlinkDetector detector= new PropertyKeyHyperlinkDetector((ITextEditor)editor);
				int offset= document.getLength() - 1;
				String keyName= fKeyStringLiteral.getLiteralValue();
				try {
					while (!found && offset >= 0) {
						region= finder.find(offset, keyName, false, true, false, false);
						if (region == null)
							offset= -1;
						else {
							// test whether it's the key
							IHyperlink[] hyperlinks= detector.detectHyperlinks(null, region, false);
							if (hyperlinks != null) {
								for (int i= 0; i < hyperlinks.length; i++) {
									IRegion hyperlinkRegion= hyperlinks[i].getHyperlinkRegion();
									found= keyName.equals(document.get(hyperlinkRegion.getOffset(), hyperlinkRegion.getLength()));
								}
							}
							// Prevent endless loop (panic code, shouldn't be needed)
							if (offset == region.getOffset())
								offset= -1;
							else
								offset= region.getOffset();
						}
					}
				} catch (BadLocationException ex) {
				}
			}
			if (found)
				EditorUtility.revealInEditor(editor, region);
			else {
				EditorUtility.revealInEditor(editor, 0, 0);
				showErrorInStatusLine(editor, JavaEditorMessages.getFormattedString("Editor.OpenPropertiesFile.error.keyNotFound", fKeyStringLiteral.getLiteralValue())); //$NON-NLS-1$
			}
		}
	}
	
	private void showErrorInStatusLine(IEditorPart editor, final String message) {
		final Display display= fEditor.getSite().getShell().getDisplay();
		display.beep();
		final IEditorStatusLine statusLine= (IEditorStatusLine)editor.getAdapter(IEditorStatusLine.class);
		if (statusLine != null) {
			display.asyncExec(new Runnable() {
				/*
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					statusLine.setMessage(true, message, null);
				}
			});
		}
	}

	private void handleOpenPropertiesFileFailed(IStorage propertiesFile) {
		showErrorInStatusLine(fEditor, JavaEditorMessages.getFormattedString("Editor.OpenPropertiesFile.error.openEditor.dialogMessage", propertiesFile.getFullPath().toOSString())); //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#getTypeLabel()
	 */
	public String getTypeLabel() {
		return null;
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlink#getHyperlinkText()
	 */
	public String getHyperlinkText() {
		return null;
	}
}
