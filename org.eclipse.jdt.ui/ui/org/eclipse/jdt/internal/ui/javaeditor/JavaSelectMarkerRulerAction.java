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

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;
import org.eclipse.jface.text.source.IVerticalRulerInfo;

import org.eclipse.ui.editors.text.EditorsUI;

import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.AnnotationPreferenceLookup;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.SelectMarkerRulerAction;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.JavaCorrectionProcessor;
import org.eclipse.jdt.internal.ui.text.correction.QuickAssistLightBulbUpdater.AssistAnnotation;

/**
 * Java select marker ruler action.
 */
public class JavaSelectMarkerRulerAction extends SelectMarkerRulerAction {

	private ITextEditor fTextEditor;
	private Position fPosition;
	private Annotation fAnnotation;
	private AnnotationPreferenceLookup fAnnotationPreferenceLookup;
	private IPreferenceStore fStore;

	public JavaSelectMarkerRulerAction(ResourceBundle bundle, String prefix, ITextEditor editor, IVerticalRulerInfo ruler) {
		super(bundle, prefix, editor, ruler);
		fTextEditor= editor;
		
		fAnnotationPreferenceLookup= EditorsUI.getAnnotationPreferenceLookup();
		fStore= JavaPlugin.getDefault().getPreferenceStore();

		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.JAVA_SELECT_MARKER_RULER_ACTION);
	}
	
	public void run() {
		if (JavaPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_ANNOTATION_ROLL_OVER))
			return;

		if (fPosition != null) {
			if (fAnnotation instanceof OverrideIndicatorManager.OverrideIndicator) {
				((OverrideIndicatorManager.OverrideIndicator)fAnnotation).open();
				return;
			}
			
			ITextOperationTarget operation= (ITextOperationTarget) fTextEditor.getAdapter(ITextOperationTarget.class);
			final int opCode= CompilationUnitEditor.CORRECTIONASSIST_PROPOSALS;
			if (operation != null && operation.canDoOperation(opCode)) {
				fTextEditor.selectAndReveal(fPosition.getOffset(), fPosition.getLength());
				operation.doOperation(opCode);
				return;
			}
			return;
		}
		super.run();
	}
	
	public void update() {
		findJavaAnnotation();
		if (fPosition != null)
			setEnabled(true);
		else
			super.update();
	}
	
	private void findJavaAnnotation() {
		fPosition= null;
		fAnnotation= null;
		
		AbstractMarkerAnnotationModel model= getAnnotationModel();
		IAnnotationAccessExtension annotationAccess= getAnnotationAccessExtension();
		
		IDocument document= getDocument();
		if (model == null)
			return ;

		boolean hasAssistLightbulb= PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_QUICKASSIST_LIGHTBULB);
			
		Iterator iter= model.getAnnotationIterator();
		int layer= Integer.MIN_VALUE;
		
		while (iter.hasNext()) {
			Annotation annotation= (Annotation) iter.next();
			if (annotation.isMarkedDeleted())
				continue;
				
			if (annotationAccess != null && annotationAccess.getLayer(annotation) < layer)
				continue;

			Position position= model.getPosition(annotation);
			if (!includesRulerLine(position, document))
				continue;
			
			boolean isReadOnly= fTextEditor instanceof ITextEditorExtension && ((ITextEditorExtension)fTextEditor).isEditorInputReadOnly();
			if (!isReadOnly
					&& (
						((hasAssistLightbulb && annotation instanceof AssistAnnotation)
						|| (annotation instanceof IJavaAnnotation && JavaCorrectionProcessor.hasCorrections((IJavaAnnotation)annotation))))) {
				fPosition= position;
				fAnnotation= annotation;
				continue;
			} else {
				AnnotationPreference preference= fAnnotationPreferenceLookup.getAnnotationPreference(annotation);
				if (preference == null)
					continue;
				
				String key= preference.getVerticalRulerPreferenceKey();
				if (key == null)
					continue;
				
				if (fStore.getBoolean(key)) {
					fPosition= position;
					fAnnotation= annotation;
				}
			}
		}
	}
}

