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

package org.eclipse.jdt.internal.ui.text.java.hover;

import java.util.Iterator;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.AnnotationPreference;
import org.eclipse.ui.texteditor.IAnnotationExtension;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.IJavaAnnotation;
import org.eclipse.jdt.internal.ui.javaeditor.JavaAnnotationIterator;
import org.eclipse.jdt.internal.ui.text.HTMLPrinter;


public class AnnotationHover extends AbstractJavaEditorTextHover {

	private MarkerAnnotationPreferences fMarkerAnnotationPreferences= new MarkerAnnotationPreferences();
	private IPreferenceStore fStore= JavaPlugin.getDefault().getPreferenceStore();

	/*
	 * Formats a message as HTML text.
	 */
	private String formatMessage(String message) {
		StringBuffer buffer= new StringBuffer();
		HTMLPrinter.addPageProlog(buffer);
		HTMLPrinter.addParagraph(buffer, HTMLPrinter.convertToHTMLContent(message));
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}
	
	/*
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		
		if (getEditor() == null)
			return null;
			
		IDocumentProvider provider= JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
		IAnnotationModel model= provider.getAnnotationModel(getEditor().getEditorInput());
		
		if (model != null) {
			Iterator e= new JavaAnnotationIterator(model, true);
			while (e.hasNext()) {
				Annotation a= (Annotation) e.next();

				if (a instanceof IAnnotationExtension) {
					AnnotationPreference preference= getAnnotationPreference((IAnnotationExtension)a);
					if (preference == null || !fStore.getBoolean(preference.getTextPreferenceKey()))
						continue;
				}

				Position p= model.getPosition(a);
				if (p != null && p.overlapsWith(hoverRegion.getOffset(), hoverRegion.getLength())) {
					String msg= ((IJavaAnnotation) a).getMessage();
					if (msg != null && msg.trim().length() > 0)
						return formatMessage(msg);
				}
			}
		}
		
		return null;
	}
	
	/*
	 * @see IJavaEditorTextHover#setEditor(IEditorPart)
	 */
	public void setEditor(IEditorPart editor) {
		if (editor instanceof CompilationUnitEditor)
			super.setEditor(editor);
		else
			super.setEditor(null);
	}

	/**
	 * Returns the annotation preference for the given marker.
	 * 
	 * @param marker
	 * @return the annotation preference or <code>null</code> if none
	 */	
	private AnnotationPreference getAnnotationPreference(IAnnotationExtension annotation) {
		String markerType= annotation.getMarkerType();
		int severity= annotation.getSeverity();		
		Iterator e= fMarkerAnnotationPreferences.getAnnotationPreferences().iterator();
		while (e.hasNext()) {
			AnnotationPreference info= (AnnotationPreference) e.next();
			if (info.getMarkerType().equals(markerType) && severity == info.getSeverity())
				return info;
			}
		return null;
	}

	static boolean isJavaProblemHover(String id) {
		return PreferenceConstants.ID_PROBLEM_HOVER.equals(id);
	}
}
