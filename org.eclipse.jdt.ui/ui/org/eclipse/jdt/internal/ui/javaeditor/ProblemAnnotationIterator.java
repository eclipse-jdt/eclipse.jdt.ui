package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.Iterator;
import org.eclipse.jface.text.source.IAnnotationModel;


/**
 * Filters problems based on their types.
 */
public class ProblemAnnotationIterator implements Iterator {
			
	private Iterator fIterator;
	private IProblemAnnotation fNext;
	private boolean fSkipIrrelevants;
	
	public ProblemAnnotationIterator(IAnnotationModel model, boolean skipIrrelevants) {
		fIterator= model.getAnnotationIterator();
		fSkipIrrelevants= skipIrrelevants;
		skip();
	}
	
	private void skip() {
		while (fIterator.hasNext()) {
			Object next= fIterator.next();
			if (next instanceof IProblemAnnotation) {
				IProblemAnnotation a= (IProblemAnnotation) next;
				if (fSkipIrrelevants) {
					if (a.isRelevant()) {
						fNext= a;
						return;
					}
				} else {
					fNext= a;
					return;
				}
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
