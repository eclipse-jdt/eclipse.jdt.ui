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
package org.eclipse.jdt.internal.ui.text.link;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * @since 3.0
 */
final class LinkedPositionAnnotations extends AnnotationModel {
	
	/* annotation types */
	private static final String TARGET_ANNOTATION_TYPE= "org.eclipse.jdt.ui.link.target"; //$NON-NLS-1$
	private static final String SLAVE_ANNOTATION_TYPE= "org.eclipse.jdt.ui.link.slave"; //$NON-NLS-1$
	private static final String FOCUS_ANNOTATION_TYPE= "org.eclipse.jdt.ui.link.master"; //$NON-NLS-1$
	
	/* configuration */
	final static boolean markTargets= true;
	final static boolean markSlaves= true;
	final static boolean markFocus= true;
	final static boolean markFirstTarget= true;
	
	private Annotation fFocusAnnotation= null;
	private final Map fGroupAnnotations= new HashMap();
	private final Map fTargetAnnotations= new HashMap();
	private Position[] fTargets= new Position[0];

	/**
	 * Sets the position that should be highlighted as the focus position, i.e. 
	 * as the position whose changes are propagated to all its linked positions
	 * by the linked environment.
	 * 
	 * @param position the new focus position, or <code>null</code> if no focus is set.
	 */
	private void setFocusPosition(Position position) throws BadLocationException {
		if (markFocus && getPosition(fFocusAnnotation) != position) {
			removeAnnotation(fFocusAnnotation, false);
			if (position != null) {
				fFocusAnnotation= new Annotation(FOCUS_ANNOTATION_TYPE, false, ""); //$NON-NLS-1$
				addAnnotation(fFocusAnnotation, position, false);
			} else
				fFocusAnnotation= null;
		}
	}
	
	/**
	 * Sets the positions that should be highlighted as the slave positions, i.e. 
	 * as the positions that are linked to the focus position.
	 * 
	 * @param positions the new slave positions, or <code>null</code> if no slave positions are to be set
	 */
	private void setGroupPositions(List positions) throws BadLocationException {
		if (!markSlaves)
			return;
		
		// remove all positions which are already there
		// algo: toRemove contains all mappings at first, but all that are in 
		// positions get removed -> toRemove contains the difference set of previsous - new
		// toAdd are the new positions, which don't exist in previous = new - previous
		List toRemove= new ArrayList(fGroupAnnotations.values());
		Map toAdd= new HashMap();
		if (positions != null) {
			for (Iterator iter= positions.iterator(); iter.hasNext();) {
				Position p= (Position) iter.next();
				if (fGroupAnnotations.containsKey(p)) {
					toRemove.remove(fGroupAnnotations.get(p));
				} else {
					Annotation a= new Annotation(SLAVE_ANNOTATION_TYPE, false, ""); //$NON-NLS-1$
					toAdd.put(a, p);
					fGroupAnnotations.put(p, a);
				}
			}
		}
		fGroupAnnotations.values().removeAll(toRemove);

		replaceAnnotations((Annotation[]) toRemove.toArray(new Annotation[0]), toAdd, false);
	}
	
	/**
	 * Sets the positions that should be highlighted as the target positions, i.e. 
	 * as the positions that can be jumped to in a linked set up.
	 * 
	 * @param positions the new target positions, or <code>null</code> if no target positions are to be set
	 */
	private void setTargetPositions(List positions) throws BadLocationException {
		if (!markTargets)
			return;
		
		// remove all positions which are already there
		// algo: toRemove contains all mappings at first, but all that are in 
		// positions get removed -> toRemove contains the difference set of previsous - new
		// toAdd are the new positions, which don't exist in previous = new - previous
		List toRemove= new ArrayList(fTargetAnnotations.values());
		Map toAdd= new HashMap();
		if (positions != null) {
			for (Iterator iter= positions.iterator(); iter.hasNext();) {
				Position p= (Position) iter.next();
				if (fTargetAnnotations.containsKey(p)) {
					toRemove.remove(fTargetAnnotations.get(p));
				} else {
					Annotation a= new Annotation(TARGET_ANNOTATION_TYPE, false, ""); //$NON-NLS-1$
					toAdd.put(a, p);
					fTargetAnnotations.put(p, a);
				}
			}
		}
		fTargetAnnotations.values().removeAll(toRemove);

		replaceAnnotations((Annotation[]) toRemove.toArray(new Annotation[0]), toAdd, false);
	}
	
	/**
	 * Switches the focus position to <code>position</code> given the
	 * <code>LinkedEnvironment env</code>. The slave positions for <code>position</code>
	 * is extracted from the environment and set accordingly, the target positions
	 * are updated as well.
	 *  
	 * @param env
	 * @param position
	 */
	public void switchToPosition(LinkedEnvironment env, LinkedPosition position) {
		if (fDocument == null || position != null && getPosition(fFocusAnnotation) == position)
			return;
		
		
		LinkedPositionGroup linkedGroup= null;
		if (position != null)
			linkedGroup= env.getGroupForPosition(position);
		
		List targets= new ArrayList();
		targets.addAll(Arrays.asList(fTargets));

		List group;
		if (linkedGroup != null)
			group= new ArrayList(Arrays.asList(linkedGroup.getPositions()));
		else
			group= new ArrayList();
		
		if (position == null || !fDocument.equals(position.getDocument()))
			// position is not valid if not in this document
			position= null;
		
		group.removeAll(targets);
		targets.remove(position);
		group.remove(position);
		prune(targets);
		prune(group);
		
		try {
			setFocusPosition(position);
			setGroupPositions(group);
			setTargetPositions(targets);
		} catch (BadLocationException e) {
			// log and ignore
			JavaPlugin.log(e);
		}
		fireModelChanged();
		
	}
	
	/**
	 * Prune <code>list</code> of all <code>LinkedPosition</code>s that 
	 * do not belong to this model's <code>IDocument</code>.
	 * 
	 * @param list the list of positions to prune
	 */
	private void prune(List list) {
		for (Iterator iter= list.iterator(); iter.hasNext();) {
			LinkedPosition pos= (LinkedPosition) iter.next();
			if (!pos.getDocument().equals(fDocument))
				iter.remove();
		}
	}

	/**
	 * Sets the target positions
	 * @param positions
	 */
	public void setTargets(Position[] positions) {
		fTargets= positions;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.AnnotationModel#connect(org.eclipse.jface.text.IDocument)
	 */
	public void connect(IDocument document) {
		fDocument= document;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.AnnotationModel#disconnect(org.eclipse.jface.text.IDocument)
	 */
	public void disconnect(IDocument document) {
		fDocument= null;
	}

	/*
	 * @see org.eclipse.jface.text.source.AnnotationModel#addPosition(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.Position)
	 */
	protected void addPosition(IDocument document, Position position) {
		// don't to anything as our positions are managed by custom
		// position updaters
	}
	
	/*
	 * @see org.eclipse.jface.text.source.AnnotationModel#removePosition(org.eclipse.jface.text.IDocument, org.eclipse.jface.text.Position)
	 */
	protected void removePosition(IDocument document, Position pos) {
		// don't to anything as our positions are managed by custom
		// position updaters
	}
	
	/*
	 * @see org.eclipse.jface.text.source.AnnotationModel#fireModelChanged()
	 */
	public void fireModelChanged() {
		super.fireModelChanged();
	}
	
}
