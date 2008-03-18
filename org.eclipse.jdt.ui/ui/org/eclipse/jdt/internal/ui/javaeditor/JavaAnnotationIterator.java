/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.Iterator;

import org.eclipse.jface.text.source.Annotation;


/**
 * Filters problems based on their types.
 */
public class JavaAnnotationIterator implements Iterator {

	private Iterator fIterator;
	private Annotation fNext;
	private boolean fSkipIrrelevants;
	private boolean fReturnAllAnnotations;

	/**
	 * Returns a new JavaAnnotationIterator.
	 * 
	 * Equivalent to <code>JavaAnnotationIterator(model, skipIrrelevants, false)</code>. 
	 * 
	 * @param parent the parent iterator to iterate over annotations
	 * @param skipIrrelevants whether to skip irrelevant annotations
	 */
	public JavaAnnotationIterator(Iterator parent, boolean skipIrrelevants) {
		this(parent, skipIrrelevants, false);
	}

	/**
	 * Returns a new JavaAnnotationIterator.
	 * @param parent the parent iterator to iterate over annotations
	 * @param skipIrrelevants whether to skip irrelevant annotations
	 * @param returnAllAnnotations Whether to return non IJavaAnnotations as well
	 */
	public JavaAnnotationIterator(Iterator parent, boolean skipIrrelevants, boolean returnAllAnnotations) {
		fReturnAllAnnotations= returnAllAnnotations;
		fIterator= parent;
		fSkipIrrelevants= skipIrrelevants;
		skip();
	}

	private void skip() {
		while (fIterator.hasNext()) {
			Annotation next= (Annotation) fIterator.next();
			if (next instanceof IJavaAnnotation) {
				if (fSkipIrrelevants) {
					if (!next.isMarkedDeleted()) {
						fNext= next;
						return;
					}
				} else {
					fNext= next;
					return;
				}
			} else if (fReturnAllAnnotations) {
				fNext= next;
				return;
			}
		}
		fNext= null;
	}

	/*
	 * @see Iterator#hasNext()
	 */
	public boolean hasNext() {
		return fNext != null;
	}

	/*
	 * @see Iterator#next()
	 */
	public Object next() {
		try {
			return fNext;
		} finally {
			skip();
		}
	}

	/*
	 * @see Iterator#remove()
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
