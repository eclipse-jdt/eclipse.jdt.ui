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
package org.eclipse.jdt.internal.ui.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.ISourceViewerExtension2;
import org.eclipse.jface.text.source.projection.AnnotationBag;

import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AnnotationPreference;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;


/**
 * Determines all markers for the given line and collects, concatenates, and formates
 * their messages.
 */
public class JavaAnnotationHover implements IAnnotationHover {

	private static class JavaAnnotationHoverType {
	}
	
	public static final JavaAnnotationHoverType OVERVIEW_RULER_HOVER= new JavaAnnotationHoverType();
	public static final JavaAnnotationHoverType TEXT_RULER_HOVER= new JavaAnnotationHoverType();
	public static final JavaAnnotationHoverType VERTICAL_RULER_HOVER= new JavaAnnotationHoverType();
	
	private IPreferenceStore fStore= JavaPlugin.getDefault().getCombinedPreferenceStore();
	private JavaAnnotationHoverType fType;
	
	public JavaAnnotationHover(JavaAnnotationHoverType type) {
		Assert.isTrue(OVERVIEW_RULER_HOVER.equals(type) || TEXT_RULER_HOVER.equals(type) || VERTICAL_RULER_HOVER.equals(type));
		fType= type;
	}
	
	private boolean isRulerLine(Position position, IDocument document, int line) {
		if (position.getOffset() > -1 && position.getLength() > -1) {
			try {
				return line == document.getLineOfOffset(position.getOffset());
			} catch (BadLocationException x) {
			}
		}
		return false;
	}
			
	private IAnnotationModel getAnnotationModel(ISourceViewer viewer) {
		if (viewer instanceof ISourceViewerExtension2) {
			ISourceViewerExtension2 extension= (ISourceViewerExtension2) viewer;
			return extension.getVisualAnnotationModel();
		}
		return viewer.getAnnotationModel();
	}
	
	private boolean isDuplicateJavaAnnotation(Map messagesAtPosition, Position position, String message) {
		if (messagesAtPosition.containsKey(position)) {
			Object value= messagesAtPosition.get(position);
			if (message.equals(value))
				return true;

			if (value instanceof List) {
				List messages= (List)value;
				if  (messages.contains(message))
					return true;
				else
					messages.add(message);
			} else {
				ArrayList messages= new ArrayList();
				messages.add(value);
				messages.add(message);
				messagesAtPosition.put(position, messages);
			}
		} else
			messagesAtPosition.put(position, message);
		return false;
	}
	
	private boolean includeAnnotation(Annotation annotation, Position position, HashMap messagesAtPosition) {
		AnnotationPreference preference= getAnnotationPreference(annotation);
		if (preference == null)
			return false;
		
		if (OVERVIEW_RULER_HOVER.equals(fType)) {				
			String key= preference.getOverviewRulerPreferenceKey();
			if (key == null || !fStore.getBoolean(key))
				return false;
		} else if (TEXT_RULER_HOVER.equals(fType)) {
			String key= preference.getTextPreferenceKey();
			if (key != null) {
				if (!fStore.getBoolean(key))
					return false;
			} else {
				key= preference.getHighlightPreferenceKey();
				if (key == null || !fStore.getBoolean(key))
					return false;
			}
		} else if (VERTICAL_RULER_HOVER.equals(fType)) {
			String key= preference.getVerticalRulerPreferenceKey();
			// backward compatibility
			if (key != null && !fStore.getBoolean(key))
				return false;
		}
		
		String text= annotation.getText();
		return (text != null && !isDuplicateJavaAnnotation(messagesAtPosition, position, text));
	}
	
	private List getJavaAnnotationsForLine(ISourceViewer viewer, int line) {
		IAnnotationModel model= getAnnotationModel(viewer);
		if (model == null)
			return null;
			
		IDocument document= viewer.getDocument();
		List javaAnnotations= new ArrayList();
		HashMap messagesAtPosition= new HashMap();
		Iterator iterator= model.getAnnotationIterator();
		
		while (iterator.hasNext()) {
			Annotation annotation= (Annotation) iterator.next();
						
			Position position= model.getPosition(annotation);
			if (position == null)
				continue;
			
			if (!isRulerLine(position, document, line))
				continue;
			
			if (annotation instanceof AnnotationBag) {
				AnnotationBag bag= (AnnotationBag) annotation;
				Iterator e= bag.iterator();
				while (e.hasNext()) {
					annotation= (Annotation) e.next();
					position= model.getPosition(annotation);
					if (position != null && includeAnnotation(annotation, position, messagesAtPosition))
						javaAnnotations.add(annotation);
				}
				continue;
			} 
			
			if (includeAnnotation(annotation, position, messagesAtPosition))
				javaAnnotations.add(annotation);
		}
		
		return javaAnnotations;
	}
		
	/*
	 * @see IVerticalRulerHover#getHoverInfo(ISourceViewer, int)
	 */
	public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
		List javaAnnotations= getJavaAnnotationsForLine(sourceViewer, lineNumber);
		if (javaAnnotations != null) {
			
			if (javaAnnotations.size() == 1) {
				
				// optimization
				Annotation annotation= (Annotation) javaAnnotations.get(0);
				String message= annotation.getText();
				if (message != null && message.trim().length() > 0)
					return formatSingleMessage(message);
					
			} else {
					
				List messages= new ArrayList();
				
				Iterator e= javaAnnotations.iterator();
				while (e.hasNext()) {
					Annotation annotation= (Annotation) e.next();
					String message= annotation.getText();
					if (message != null && message.trim().length() > 0)
						messages.add(message.trim());
				}
				
				if (messages.size() == 1)
					return formatSingleMessage((String) messages.get(0));
					
				if (messages.size() > 1)
					return formatMultipleMessages(messages);
			}
		}
		return null;
	}
		
	/*
	 * Formats a message as HTML text.
	 */
	private String formatSingleMessage(String message) {
		StringBuffer buffer= new StringBuffer();
		HTMLPrinter.addPageProlog(buffer);
		HTMLPrinter.addParagraph(buffer, HTMLPrinter.convertToHTMLContent(message));
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}
	
	/*
	 * Formats several message as HTML text.
	 */
	private String formatMultipleMessages(List messages) {
		StringBuffer buffer= new StringBuffer();
		HTMLPrinter.addPageProlog(buffer);
		HTMLPrinter.addParagraph(buffer, HTMLPrinter.convertToHTMLContent(JavaUIMessages.getString("JavaAnnotationHover.multipleMarkersAtThisLine"))); //$NON-NLS-1$
		
		HTMLPrinter.startBulletList(buffer);
		Iterator e= messages.iterator();
		while (e.hasNext())
			HTMLPrinter.addBullet(buffer, HTMLPrinter.convertToHTMLContent((String) e.next()));
		HTMLPrinter.endBulletList(buffer);	
		
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}

	/**
	 * Returns the annotation preference for the given annotation.
	 * 
	 * @param annotation the annotation
	 * @return the annotation preference or <code>null</code> if none
	 */	
	private AnnotationPreference getAnnotationPreference(Annotation annotation) {
		return EditorsUI.getAnnotationPreferenceLookup().getAnnotationPreference(annotation);
	}
}
