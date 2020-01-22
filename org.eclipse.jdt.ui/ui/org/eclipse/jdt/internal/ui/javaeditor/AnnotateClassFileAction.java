/*******************************************************************************
 * Copyright (c) 2015 GK Software AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stephan Herrmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jface.action.Action;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;

/**
 * Action to add external null annotations from a ClassFileEditor.
 *
 * @see <a href="https://bugs.eclipse.org/458201">[null] Offer new command "Annotate" on
 *      ClassFileEditor</a>
 * @since 3.11
 */
public class AnnotateClassFileAction extends Action {

	private final ClassFileEditor fEditor;

	protected AnnotateClassFileAction(ClassFileEditor editor) {
		super(JavaEditorMessages.AnnotateClassFile_label);
		fEditor= editor;
	}

	@Override
	public void run() {
		ISourceViewer viewer= fEditor.getViewer();
		if (viewer instanceof SourceViewer) {
			SourceViewer sourceViewer= (SourceViewer) viewer;
			if (sourceViewer.canDoOperation(JavaSourceViewer.ANNOTATE_CLASS_FILE))
				sourceViewer.doOperation(JavaSourceViewer.ANNOTATE_CLASS_FILE);
		}
	}
}
