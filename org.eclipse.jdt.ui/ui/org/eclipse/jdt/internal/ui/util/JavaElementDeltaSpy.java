/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElementDelta;

public class JavaElementDeltaSpy implements IElementChangedListener {
	public void elementChanged(ElementChangedEvent event) {
		IJavaElementDelta delta= event.getDelta();
		System.out.println("delta received: "); //$NON-NLS-1$
		System.out.print(delta);
	}

	protected void processDelta(String indent, IJavaElementDelta delta) {
		System.out.println(indent + delta);
		IJavaElementDelta[] subdeltas= delta.getAffectedChildren();
		for (int i= 0; i < subdeltas.length; i++)
			processDelta("", subdeltas[i]); //$NON-NLS-1$
	}

}