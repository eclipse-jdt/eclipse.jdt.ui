/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.util;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElementDelta;

public class JavaElementDeltaSpy implements IElementChangedListener {
	public void elementChanged(ElementChangedEvent event) {
		IJavaElementDelta delta= event.getDelta();
		System.out.println("delta received: ");
		System.out.print(delta);
	}

	protected void processDelta(String indent, IJavaElementDelta delta) {
		System.out.println(indent + delta);
		IJavaElementDelta[] subdeltas= delta.getAffectedChildren();
		for (int i= 0; i < subdeltas.length; i++)
			processDelta("", subdeltas[i]);
	}

}