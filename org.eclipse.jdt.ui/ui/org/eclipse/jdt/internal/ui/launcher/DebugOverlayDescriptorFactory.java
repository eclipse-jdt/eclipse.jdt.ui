/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */

package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jdt.debug.core.IJavaModifiers;

import org.eclipse.jdt.internal.ui.viewsupport.IOverlayDescriptor;
import org.eclipse.jdt.internal.ui.viewsupport.IOverlayDescriptorFactory;
import org.eclipse.jdt.internal.ui.viewsupport.JavaOverlayDescriptor;

/**
 * Creates IOverlayDescriptors for debugger java variables
 * @see org.eclipse.jdt.ui.viewsupport.IOverlayDescriptor.
 */
public class DebugOverlayDescriptorFactory implements IOverlayDescriptorFactory {
	public IOverlayDescriptor createDescriptor(String base, Object item) {
		if (item instanceof IAdaptable) {
			int flags= 0;
			IAdaptable element= (IAdaptable)item;
			IJavaModifiers javaProperties= (IJavaModifiers)element.getAdapter(IJavaModifiers.class);
			if (javaProperties != null) {
				if (javaProperties.isFinal()) {
					flags |= JavaOverlayDescriptor.FINAL;
				}
				if (javaProperties.isStatic()) {
					flags |= JavaOverlayDescriptor.STATIC;
				}
				return new JavaOverlayDescriptor(base, flags);
			}
		}
		return new JavaOverlayDescriptor(base, 0);

	}

}