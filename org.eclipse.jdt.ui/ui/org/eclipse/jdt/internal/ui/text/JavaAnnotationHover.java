/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/
package org.eclipse.jdt.internal.ui.text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.IProblemAnnotation;


/**
 * Determines all markers for the given line and collects, concatenates, and formates
 * their messages.
 */
public class JavaAnnotationHover implements IAnnotationHover {
	
	/**
	 * Returns the distance to the ruler line. 
	 */
	protected int compareRulerLine(Position position, IDocument document, int line) {
		
		if (position.getOffset() > -1 && position.getLength() > -1) {
			try {
				int problemAnnotationLine= document.getLineOfOffset(position.getOffset());
				if (line == problemAnnotationLine)
					return 1;
				if (problemAnnotationLine <= line && line <= document.getLineOfOffset(position.getOffset() + position.getLength()))
					return 2;
			} catch (BadLocationException x) {
			}
		}
		
		return 0;
	}
	
	/**
	 * Selects a set of markers from the two lists. By default, it just returns
	 * the set of exact matches.
	 */
	protected List select(List exactMatch, List including) {
		return exactMatch;
	}
	
	/**
	 * Returns one marker which includes the ruler's line of activity.
	 */
	protected List getProblemAnnotationsForLine(ISourceViewer viewer, int line) {
		
		IDocument document= viewer.getDocument();
		IAnnotationModel model= viewer.getAnnotationModel();
		
		if (model == null)
			return null;
			
		List exact= new ArrayList();
		List including= new ArrayList();
		
		Iterator e= model.getAnnotationIterator();
		while (e.hasNext()) {
			Object o= e.next();
			if (o instanceof IProblemAnnotation) {
				IProblemAnnotation a= (IProblemAnnotation)o;
				switch (compareRulerLine(model.getPosition((Annotation)a), document, line)) {
					case 1:
						exact.add(a);
						break;
					case 2:
						including.add(a);
						break;
				}
			}
		}
		
		return select(exact, including);
	}

		
	/*
	 * @see IVerticalRulerHover#getHoverInfo(ISourceViewer, int)
	 */
	public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
		List problemAnnotations= getProblemAnnotationsForLine(sourceViewer, lineNumber);
		if (problemAnnotations != null) {
			
			if (problemAnnotations.size() == 1) {
				
				// optimization
				IProblemAnnotation problemAnnotation= (IProblemAnnotation)problemAnnotations.get(0);
				String message= problemAnnotation.getMessage();
				if (message != null && message.trim().length() > 0)
					return formatSingleMessage(message);
					
			} else {
					
				List messages= new ArrayList();
				
				Iterator e= problemAnnotations.iterator();
				while (e.hasNext()) {
					IProblemAnnotation problemAnnotation= (IProblemAnnotation)e.next();
					String message= problemAnnotation.getMessage();
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
}
