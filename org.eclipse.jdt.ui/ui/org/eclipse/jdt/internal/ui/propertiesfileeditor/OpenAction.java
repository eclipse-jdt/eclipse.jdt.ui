/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;

import org.eclipse.ui.IFileEditorInput;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

/**
 * This action opens a tool (internal editor or view or an external
 * application) for the element at the given location.
 * <p>
 * XXX:	This does not work for properties files coming from a JAR due to
 * 		missing J Core functionality. For details see:
 * 		https://bugs.eclipse.org/bugs/show_bug.cgi?id=22376
 * </p>
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 3.1
 */
public class OpenAction extends SelectionDispatchAction {


	private PropertiesFileEditor fEditor;


	/**
	 * Creates a new <code>OpenAction</code>.
	 *
	 * @param editor the Properties file editor which provides the context information for this action
	 */
	public OpenAction(PropertiesFileEditor editor) {
		super(editor.getEditorSite());
		fEditor= editor;
		setText(PropertiesFileEditorMessages.OpenAction_label);
		setToolTipText(PropertiesFileEditorMessages.OpenAction_tooltip);

		 // XXX: Must be removed once support for JARs is available (see class Javadoc for details).
		setEnabled(fEditor.getEditorInput() instanceof IFileEditorInput);
	}

	/*
	 * @see org.eclipse.jdt.ui.actions.SelectionDispatchAction#selectionChanged(org.eclipse.jface.text.ITextSelection)
	 */
	public void selectionChanged(ITextSelection selection) {
		setEnabled(checkEnabled(selection));
	}

	private boolean checkEnabled(ITextSelection selection) {
		if (selection == null || selection.isEmpty())
			return false;

		// XXX: Must be changed to IStorageEditorInput once support for JARs is available (see class Javadoc for details)
		return fEditor.getEditorInput() instanceof IFileEditorInput;
	}

	public void run(ITextSelection selection) {

		if (!checkEnabled(selection))
			return;

		IRegion region= new Region(selection.getOffset(), selection.getLength());
		PropertyKeyHyperlinkDetector detector= new PropertyKeyHyperlinkDetector();
		detector.setContext(fEditor);
		IHyperlink[]hyperlinks= detector.detectHyperlinks(fEditor.internalGetSourceViewer(), region, false);

		if (hyperlinks != null && hyperlinks.length == 1)
			hyperlinks[0].open();

	}
}
