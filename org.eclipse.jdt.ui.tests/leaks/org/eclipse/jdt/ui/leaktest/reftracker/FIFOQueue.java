/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.leaktest.reftracker;

import java.util.Arrays;


public class FIFOQueue {

	private Object[] fStore;
	private int fReadIndex;
	private int fWriteIndex;

	public FIFOQueue(int initialSize) {
		fStore= new Object[initialSize];
		fReadIndex= 0;
		fWriteIndex= 0;
	}

	public void add(Object object) {
		int pos= fWriteIndex;
		int next= nextIndex(pos, fStore.length);
		if (next == fReadIndex) {
			pos= increaseCapacity();
			next= pos + 1;
		}
		fStore[pos]= object;
		fWriteIndex= next;
	}

	private int increaseCapacity() {
		Object[] oldStore= fStore;
		int oldLen= oldStore.length;
		Object[] newStore= new Object[oldLen * 2];
		int k= 0;
		for (int i= fReadIndex, end= fWriteIndex; i != end; i= nextIndex(i, oldLen), k++) {
			newStore[k]= oldStore[i];
			oldStore[i]= null;
		}
		fStore= newStore;
		fReadIndex= 0;
		fWriteIndex= k;
		return k;
	}

	public Object poll() {
		if (isEmpty()) {
			return null;
		}
		int index= fReadIndex;
		Object element= fStore[index];
		fStore[index]= null; // avoid unnecessary references
		fReadIndex= nextIndex(index, fStore.length);
		return element;
	}

	private static int nextIndex(int index, int max) {
		int next= index + 1;
		if (next == max) {
			return 0;
		}
		return next;
	}

	public boolean isEmpty() {
		return fReadIndex == fWriteIndex;
	}

	public int getSize() {
		if (fReadIndex <= fWriteIndex) {
			return fWriteIndex - fReadIndex;
		} else {
			return fStore.length - fReadIndex + fWriteIndex;
		}
	}

	public void clear() {
		Arrays.fill(fStore, null);
		fReadIndex= 0;
		fWriteIndex= 0;
	}
}
