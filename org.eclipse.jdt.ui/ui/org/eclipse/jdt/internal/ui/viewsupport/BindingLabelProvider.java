/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.core.manipulation.BindingLabelProviderCore;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Label provider to render bindings in viewers.
 *
 * @since 3.1
 */
public class BindingLabelProvider extends LabelProvider {


	private static int getAdornmentFlags(IBinding binding) {
		int adornments= 0;
		final int modifiers= binding.getModifiers();
		if (Modifier.isAbstract(modifiers))
			adornments|= JavaElementImageDescriptor.ABSTRACT;
		if (Modifier.isFinal(modifiers))
			adornments|= JavaElementImageDescriptor.FINAL;
		if (Modifier.isStatic(modifiers))
			adornments|= JavaElementImageDescriptor.STATIC;

		if (binding.isDeprecated())
			adornments|= JavaElementImageDescriptor.DEPRECATED;

		if (binding instanceof IMethodBinding) {
			if (((IMethodBinding) binding).isConstructor())
				adornments|= JavaElementImageDescriptor.CONSTRUCTOR;
			if (Modifier.isSynchronized(modifiers))
				adornments|= JavaElementImageDescriptor.SYNCHRONIZED;
			if (Modifier.isNative(modifiers))
				adornments|= JavaElementImageDescriptor.NATIVE;
			ITypeBinding type= ((IMethodBinding) binding).getDeclaringClass();
			if (type.isInterface() && !Modifier.isAbstract(modifiers) && !Modifier.isStatic(modifiers))
				adornments|= JavaElementImageDescriptor.DEFAULT_METHOD;
			if (((IMethodBinding) binding).getDefaultValue() != null)
				adornments|= JavaElementImageDescriptor.ANNOTATION_DEFAULT;
		}
		if (binding instanceof IVariableBinding && ((IVariableBinding) binding).isField()) {
			if (Modifier.isTransient(modifiers))
				adornments|= JavaElementImageDescriptor.TRANSIENT;
			if (Modifier.isVolatile(modifiers))
				adornments|= JavaElementImageDescriptor.VOLATILE;
		}
		return adornments;
	}

	private static ImageDescriptor getBaseImageDescriptor(IBinding binding, int flags) {
		if (binding instanceof ITypeBinding) {
			ITypeBinding typeBinding= (ITypeBinding) binding;
			if (typeBinding.isArray()) {
				typeBinding= typeBinding.getElementType();
			}
			if (typeBinding.isCapture()) {
				typeBinding.getWildcard();
			}
			return getTypeImageDescriptor(typeBinding.getDeclaringClass() != null, typeBinding, flags);
		} else if (binding instanceof IMethodBinding) {
			ITypeBinding type= ((IMethodBinding) binding).getDeclaringClass();
			int modifiers= binding.getModifiers();
			if (type.isEnum() && (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers) && !Modifier.isPrivate(modifiers)) && ((IMethodBinding) binding).isConstructor())
				return JavaPluginImages.DESC_MISC_PRIVATE;
			return getMethodImageDescriptor(binding.getModifiers());
		} else if (binding instanceof IVariableBinding)
			return getFieldImageDescriptor((IVariableBinding) binding);
		return JavaPluginImages.DESC_OBJS_UNKNOWN;
	}

	private static ImageDescriptor getClassImageDescriptor(int modifiers) {
		if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_OBJS_CLASS;
		else
			return JavaPluginImages.DESC_OBJS_CLASS_DEFAULT;
	}

	private static ImageDescriptor getFieldImageDescriptor(IVariableBinding binding) {
		final int modifiers= binding.getModifiers();
		if (Modifier.isPublic(modifiers) || binding.isEnumConstant())
			return JavaPluginImages.DESC_FIELD_PUBLIC;
		if (Modifier.isProtected(modifiers))
			return JavaPluginImages.DESC_FIELD_PROTECTED;
		if (Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_FIELD_PRIVATE;

		return JavaPluginImages.DESC_FIELD_DEFAULT;
	}

	private static ImageDescriptor getInnerClassImageDescriptor(int modifiers) {
		if (Modifier.isPublic(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_PUBLIC;
		else if (Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_PRIVATE;
		else if (Modifier.isProtected(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_PROTECTED;
		else
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_DEFAULT;
	}

	private static ImageDescriptor getInnerInterfaceImageDescriptor(int modifiers) {
		if (Modifier.isPublic(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PUBLIC;
		else if (Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PRIVATE;
		else if (Modifier.isProtected(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PROTECTED;
		else
			return JavaPluginImages.DESC_OBJS_INTERFACE_DEFAULT;
	}

	private static ImageDescriptor getInterfaceImageDescriptor(int modifiers) {
		if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_OBJS_INTERFACE;
		else
			return JavaPluginImages.DESC_OBJS_INTERFACE_DEFAULT;
	}

	private static ImageDescriptor getMethodImageDescriptor(int modifiers) {
		if (Modifier.isPublic(modifiers))
			return JavaPluginImages.DESC_MISC_PUBLIC;
		if (Modifier.isProtected(modifiers))
			return JavaPluginImages.DESC_MISC_PROTECTED;
		if (Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_MISC_PRIVATE;

		return JavaPluginImages.DESC_MISC_DEFAULT;
	}

	private static ImageDescriptor getTypeImageDescriptor(boolean inner, ITypeBinding binding, int flags) {
		if (binding.isEnum())
			return JavaPluginImages.DESC_OBJS_ENUM;
		else if (binding.isAnnotation())
			return JavaPluginImages.DESC_OBJS_ANNOTATION;
		else if (binding.isInterface()) {
			if ((flags & JavaElementImageProvider.LIGHT_TYPE_ICONS) != 0)
				return JavaPluginImages.DESC_OBJS_INTERFACEALT;
			if (inner)
				return getInnerInterfaceImageDescriptor(binding.getModifiers());
			return getInterfaceImageDescriptor(binding.getModifiers());
		} else if (binding.isRecord())
			return JavaPluginImages.DESC_OBJS_RECORD;
		else if (binding.isClass()) {
			if ((flags & JavaElementImageProvider.LIGHT_TYPE_ICONS) != 0)
				return JavaPluginImages.DESC_OBJS_CLASSALT;
			if (inner)
				return getInnerClassImageDescriptor(binding.getModifiers());
			return getClassImageDescriptor(binding.getModifiers());
		} else if (binding.isTypeVariable()) {
			return JavaPluginImages.DESC_OBJS_TYPEVARIABLE;
		}
		// primitive type, wildcard
		return null;
	}

	/**
	 * Returns the label for a Java element with the flags as defined by {@link JavaElementLabels}.
	 * @param binding The binding to render.
	 * @param flags The text flags as defined in {@link JavaElementLabels}
	 * @return the label of the binding
	 */
	public static String getBindingLabel(IBinding binding, long flags) {
		return BindingLabelProviderCore.getBindingLabel(binding, flags);
	}

	/**
	 * Returns the image descriptor for a binding with the flags as defined by {@link JavaElementImageProvider}.
	 * @param binding The binding to get the image for.
	 * @param imageFlags The image flags as defined in {@link JavaElementImageProvider}.
	 * @return the image of the binding or null if there is no image
	 */
	public static ImageDescriptor getBindingImageDescriptor(IBinding binding, int imageFlags) {
		ImageDescriptor baseImage= getBaseImageDescriptor(binding, imageFlags);
		if (baseImage != null) {
			int adornmentFlags= getAdornmentFlags(binding);
			Point size= ((imageFlags & JavaElementImageProvider.SMALL_ICONS) != 0) ? JavaElementImageProvider.SMALL_SIZE : JavaElementImageProvider.BIG_SIZE;
			return new JavaElementImageDescriptor(baseImage, adornmentFlags, size);
		}
		return null;
	}


	public static final long DEFAULT_TEXTFLAGS= JavaElementLabels.ALL_DEFAULT;
	public static final int DEFAULT_IMAGEFLAGS= JavaElementImageProvider.OVERLAY_ICONS;


	final private long fTextFlags;
	final private int fImageFlags;

	private ImageDescriptorRegistry fRegistry;

	/**
	 * Creates a new binding label provider with default text and image flags
	 */
	public BindingLabelProvider() {
		this(DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS);
	}

	/**
	 * @param textFlags Flags defined in {@link JavaElementLabels}.
	 * @param imageFlags Flags defined in {@link JavaElementImageProvider}.
	 */
	public BindingLabelProvider(final long textFlags, final int imageFlags) {
		fImageFlags= imageFlags;
		fTextFlags= textFlags;
		fRegistry= null;
	}

	/*
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	@Override
	public Image getImage(Object element) {
		if (element instanceof IBinding) {
			ImageDescriptor baseImage= getBindingImageDescriptor((IBinding) element, fImageFlags);
			if (baseImage != null) {
				return getRegistry().get(baseImage);
			}
		}
		return super.getImage(element);
	}

	private ImageDescriptorRegistry getRegistry() {
		if (fRegistry == null)
			fRegistry= JavaPlugin.getImageDescriptorRegistry();
		return fRegistry;
	}

	/*
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof IBinding) {
			return getBindingLabel((IBinding) element, fTextFlags);
		}
		return super.getText(element);
	}
}
