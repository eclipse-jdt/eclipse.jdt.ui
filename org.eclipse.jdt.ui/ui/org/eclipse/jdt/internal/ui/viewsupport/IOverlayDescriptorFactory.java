package org.eclipse.jdt.internal.ui.viewsupport;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


/**
 * An IOverlayDescriptorFactory translates an element into an IOverlayDescriptor
 * that may be independent of the the element. This allows to reuse the overlays
 * for java members (public, static, etc) for IJavaElements and debugger stuff.
 */
public interface IOverlayDescriptorFactory {
	IOverlayDescriptor createDescriptor(String baseImageKey, Object element);
}