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
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.Iterator;
import java.util.ResourceBundle;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.SelectMarkerRulerAction;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.text.correction.AssistContext;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;

/**
 * A special select marker ruler action which activates quick fix if clicked on a quick fixable problem.
 */
public class JavaSelectMarkerRulerAction extends SelectMarkerRulerAction {

	private ITextEditor fTextEditor;
	private Position fPosition;

	public JavaSelectMarkerRulerAction(ResourceBundle bundle, String prefix, ITextEditor editor, IVerticalRulerInfo ruler) {
		super(bundle, prefix, editor, ruler);
		fTextEditor= editor;
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.JAVA_SELECT_MARKER_RULER_ACTION);
	}
	
	public void run() {
		if (fPosition != null) {
			ITextOperationTarget operation= (ITextOperationTarget) fTextEditor.getAdapter(ITextOperationTarget.class);
			final int opCode= CompilationUnitEditor.CORRECTIONASSIST_PROPOSALS;
			if (operation != null && operation.canDoOperation(opCode)) {
				fTextEditor.selectAndReveal(fPosition.getOffset(), fPosition.getLength());
				operation.doOperation(opCode);
				return;
			}
		}
		super.run();
	}
	
	public void update() {
		// Begin Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20114
		if (!(fTextEditor instanceof ITextEditorExtension) || ((ITextEditorExtension) fTextEditor).isEditorInputReadOnly()) {
			fPosition= null;
			super.update();
			return;
		}
		// End Fix for http://dev.eclipse.org/bugs/show_bug.cgi?id=20114
		fPosition= getJavaAnnotationPosition();
		if (fPosition != null)
			setEnabled(true);
		else
			super.update();
	}
	
	private Position getJavaAnnotationPosition() {
		AbstractMarkerAnnotationModel model= getAnnotationModel();
		IDocument document= getDocument();
		if (model == null)
			return null;
		ICompilationUnit cu= getCompilationUnit();
		if (cu == null) {
			return null;
		}
			
		Iterator iter= model.getAnnotationIterator();
		while (iter.hasNext()) {
			Annotation annotation= (Annotation) iter.next();
			if (annotation instanceof IJavaAnnotation) {
				IJavaAnnotation javaAnnotation= (IJavaAnnotation)annotation;
				if (javaAnnotation.isRelevant()) {
					Position position= model.getPosition(annotation);
					if (includesRulerLine(position, document) && JavaCorrectionProcessor.hasCorrections(javaAnnotation))
						return position;
				}
			}
		}
		if (PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.APPEARANCE_QUICKASSIST_LIGHTBULB)) {
			ISelection sel= fTextEditor.getSelectionProvider().getSelection();
			if (sel instanceof ITextSelection) {
				ITextSelection selection= (ITextSelection) sel;
				Position position= new Position(selection.getOffset(), selection.getLength());
				if (!includesRulerLine(position, document)) {
					return null;
				}
				AssistContext context= new AssistContext(cu, position.getOffset(), position.getLength());
				if (JavaCorrectionProcessor.hasAssists(context)) {
					return position;
				}
			}
		}
		return null;
	}
	
	private ICompilationUnit getCompilationUnit() {
		IEditorInput input= fTextEditor.getEditorInput();
		if (input instanceof FileEditorInput) {
			return JavaCore.createCompilationUnitFrom(((FileEditorInput) input).getFile());
		}
		return null;
	}	
}

