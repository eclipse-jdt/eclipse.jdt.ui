/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.Iterator;
import java.util.ResourceBundle;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerInfo;

import org.eclipse.core.resources.IMarker;

import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.eclipse.ui.texteditor.SelectMarkerRulerAction;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionSourceViewer;
import org.eclipse.jdt.internal.ui.text.correction.ProblemPosition;

/**
 * A special select marker ruler action which activates quick fix if clicked on a quick fixable
 * probelm.
 */
public class JavaSelectMarkerRulerAction extends SelectMarkerRulerAction {

	private ITextEditor fMyTextEditor;
	private Position fPosition;

	public JavaSelectMarkerRulerAction(ResourceBundle bundle, String prefix, ITextEditor editor, IVerticalRulerInfo ruler) {
		super(bundle, prefix, editor, ruler);
		fMyTextEditor= editor;
	}
	
	public void run() {
		superCall: {
			if (fPosition == null)
				break superCall;
			ITextOperationTarget operation= (ITextOperationTarget) fMyTextEditor.getAdapter(ITextOperationTarget.class);
			final int opCode= JavaCorrectionSourceViewer.CORRECTIONASSIST_PROPOSALS;
			if (operation == null || !operation.canDoOperation(opCode)) {
				break superCall;
			}
			fMyTextEditor.selectAndReveal(fPosition.getOffset(), fPosition.getLength());
			operation.doOperation(opCode);
			return;
		}
		super.run();
	}
	
	public void update() {
		// Begin Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20114
		if (!(fMyTextEditor instanceof ITextEditorExtension) || ((ITextEditorExtension)fMyTextEditor).isEditorInputReadOnly()) {
			fPosition= null;
			super.update();
			return;
		}
		// End Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20114
		fPosition= getProblemPosition();
		if (fPosition != null)
			setEnabled(true);
		else
			super.update();
	}
	
	private Position getProblemPosition() {
		AbstractMarkerAnnotationModel model= getAnnotationModel();
		IDocument document= getDocument();
		if (model == null)
			return null;
		Iterator iter= model.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annotation= (Annotation)iter.next();
			if (annotation instanceof IProblemAnnotation) {
				Position position= model.getPosition(annotation);
				if (includesRulerLine(position, document) && JavaCorrectionProcessor.hasCorrections(((IProblemAnnotation)annotation).getId()))
					return position;
			}
		}
		return null;
	}
}

