/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.examples.jspeditor;

import org.eclipse.jface.text.source.IAnnotationAccess;

import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.editors.text.TextEditor;

import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;


/**
 * A simple JSP Editor.
 * 
 * @since 3.0
 */
public class JspEditor extends TextEditor {

	/**
	 * Creates a new JSP editor.
	 */
	public JspEditor() {
		super();
		setSourceViewerConfiguration(new JspSourceViewerConfiguration(this));
		setDocumentProvider(new FileDocumentProvider());
		
		
		/*
		 * FIXME:
		 * This would be the right thing to do. Currently
		 * we share the preferences with the text editor.
		 */
//		setPreferenceStore(JspUIPlugin.getDefault().getPreferenceStore());
	}

	/*
	 * @see TextEditor#createAnnotationAccess()
	 */
	protected IAnnotationAccess createAnnotationAccess() {
		return new DefaultMarkerAnnotationAccess();
	}
}
