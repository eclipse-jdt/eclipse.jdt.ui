/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;


import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;

public class ResourceDeltaSpy implements IResourceChangeListener {
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta= event.getDelta();
		System.out.println("resource delta:");
		processDelta("", delta);
	}

	protected void processDelta(String indent, IResourceDelta delta) {
		System.out.println(indent+delta);
		IResourceDelta[] subdeltas= delta.getAffectedChildren();
		for (int i= 0; i < subdeltas.length; i++)
			processDelta(indent+"   ", subdeltas[i]);
	}
}