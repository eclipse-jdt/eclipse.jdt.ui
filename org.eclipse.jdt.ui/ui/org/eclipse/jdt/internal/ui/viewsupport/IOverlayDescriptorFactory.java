/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;


/**
 * An IOverlayDescriptorFactory translates an element into an IOverlayDescriptor
 * that may be independent of the the element. This allows to reuse the overlays
 * for java members (public, static, etc) for IJavaElements and debugger stuff.
 */
public interface IOverlayDescriptorFactory {
	IOverlayDescriptor createDescriptor(String baseImageKey, Object element);
}