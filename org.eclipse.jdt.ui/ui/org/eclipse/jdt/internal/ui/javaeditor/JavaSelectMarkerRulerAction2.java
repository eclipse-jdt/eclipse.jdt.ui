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

import java.util.ResourceBundle;

import org.eclipse.jface.action.IAction;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationEvent;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.ui.texteditor.SelectMarkerRulerAction2;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.QuickAssistLightBulbUpdater.AssistAnnotation;
import org.eclipse.jdt.internal.ui.text.java.hover.JavaExpandHover;

/**
 * A special select marker ruler action which activates quick fix if clicked on a quick fixable problem.
 */
public class JavaSelectMarkerRulerAction2 extends SelectMarkerRulerAction2 {

	public JavaSelectMarkerRulerAction2(ResourceBundle bundle, String prefix, ITextEditor editor) {
		super(bundle, prefix, editor);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.JAVA_SELECT_MARKER_RULER_ACTION);
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.IAnnotationListener#annotationDefaultSelected(org.eclipse.ui.texteditor.AnnotationEvent)
	 */
	public void annotationDefaultSelected(AnnotationEvent event) {
		Annotation a= event.getAnnotation();
		IAnnotationModel model= getAnnotationModel();
		Position position= model.getPosition(a);
		
		if (isBreakpoint(a))
			triggerAction(ITextEditorActionConstants.RULER_DOUBLE_CLICK);
		
		if (position == null)
			return;
		
		if (isQuickFixTarget(a)) {
			ITextOperationTarget operation= (ITextOperationTarget) getTextEditor().getAdapter(ITextOperationTarget.class);
			final int opCode= CompilationUnitEditor.CORRECTIONASSIST_PROPOSALS;
			if (operation != null && operation.canDoOperation(opCode)) {
				getTextEditor().selectAndReveal(position.getOffset(), position.getLength());
				operation.doOperation(opCode);
				return;
			}
		}
		
		// default:
		super.annotationDefaultSelected(event);
	}

	/**
	 * @param ma
	 * @return
	 */
	private boolean isBreakpoint(Annotation a) {
		return a.getType().equals("org.eclipse.debug.core.breakpoint") || a.getType().equals(JavaExpandHover.NO_BREAKPOINT_ANNOTATION); //$NON-NLS-1$
	}

	private boolean isQuickFixTarget(Annotation a) {
		return a instanceof IJavaAnnotation && JavaCorrectionProcessor.hasCorrections((IJavaAnnotation) a) || a instanceof AssistAnnotation;	
	}

	private void triggerAction(String actionID) {
		IAction action= getTextEditor().getAction(actionID);
		if (action != null) {
			if (action instanceof IUpdate)
				((IUpdate) action).update();
			// hack to propagate line change
			if (action instanceof ISelectionListener) {
				((ISelectionListener)action).selectionChanged(null, null);
			}
			if (action.isEnabled())
				action.run();
		}
	}

}

