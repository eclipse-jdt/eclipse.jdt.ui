/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.dialogs;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

public class SourceActionLabelProvider extends LabelProvider {

	static int computeJavaAdornmentFlags(IBinding binding, int flags) {
		int adornments= 0;
		if (binding instanceof IMethodBinding && ((IMethodBinding) binding).isConstructor())
			adornments|= JavaElementImageDescriptor.CONSTRUCTOR;

		int modifiers= binding.getModifiers();
		if (Modifier.isAbstract(modifiers))
			adornments|= JavaElementImageDescriptor.ABSTRACT;
		if (Modifier.isFinal(modifiers))
			adornments|= JavaElementImageDescriptor.FINAL;
		if (Modifier.isSynchronized(modifiers))
			adornments|= JavaElementImageDescriptor.SYNCHRONIZED;
		if (Modifier.isStatic(modifiers))
			adornments|= JavaElementImageDescriptor.STATIC;
		if (binding.isDeprecated())
			adornments|= JavaElementImageDescriptor.DEPRECATED;
		return adornments;
	}

	static ImageDescriptor getBaseImageDescriptor(IBinding binding, int flags) {
		if (binding instanceof ITypeBinding)
			return getTypeImageDescriptor((ITypeBinding) binding, flags);
		else if (binding instanceof IMethodBinding) {
			ITypeBinding type= ((IMethodBinding) binding).getDeclaringClass();
			int modifiers= binding.getModifiers();
			if (type.isEnum() && (!Modifier.isPublic(modifiers) && !Modifier.isProtected(modifiers) && !Modifier.isPrivate(modifiers)) && ((IMethodBinding) binding).isConstructor())
				return JavaPluginImages.DESC_MISC_PRIVATE;
			return getMethodImageDescriptor(binding.getModifiers());
		} else if (binding instanceof IVariableBinding)
			return getFieldImageDescriptor((IVariableBinding) binding);
		return JavaPluginImages.DESC_OBJS_UNKNOWN;
	}

	static ImageDescriptor getClassImageDescriptor(int modifiers) {
		if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_OBJS_CLASS;
		else
			return JavaPluginImages.DESC_OBJS_CLASS_DEFAULT;
	}

	static ImageDescriptor getFieldImageDescriptor(IVariableBinding binding) {
		int modifiers= binding.getModifiers();
		if (Modifier.isPublic(modifiers) || binding.isEnumConstant())
			return JavaPluginImages.DESC_FIELD_PUBLIC;
		if (Modifier.isProtected(modifiers))
			return JavaPluginImages.DESC_FIELD_PROTECTED;
		if (Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_FIELD_PRIVATE;

		return JavaPluginImages.DESC_FIELD_DEFAULT;
	}

	static void getFieldLabel(IVariableBinding binding, int flags, StringBuffer buffer) {
		if (getFlag(flags, JavaElementLabels.F_PRE_TYPE_SIGNATURE) && !binding.isEnumConstant()) {
			buffer.append(binding.getType().getName());
			buffer.append(' ');
		}
		// qualification
		if (getFlag(flags, JavaElementLabels.F_FULLY_QUALIFIED)) {
			getTypeLabel(binding.getDeclaringClass(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & JavaElementLabels.P_COMPRESSED), buffer);
			buffer.append('.');
		}
		buffer.append(binding.getName());
		if (getFlag(flags, JavaElementLabels.F_APP_TYPE_SIGNATURE) && !binding.isEnumConstant()) {
			buffer.append(JavaElementLabels.DECL_STRING);
			buffer.append(binding.getType().getName());
		}
		// post qualification
		if (getFlag(flags, JavaElementLabels.F_POST_QUALIFIED)) {
			buffer.append(JavaElementLabels.CONCAT_STRING);
			getTypeLabel(binding.getDeclaringClass(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & JavaElementLabels.P_COMPRESSED), buffer);
		}
	}

	static boolean getFlag(long flags, long flag) {
		return (flags & flag) != 0;
	}

	static ImageDescriptor getInnerClassImageDescriptor(int modifiers) {
		if (Modifier.isPublic(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_PUBLIC;
		else if (Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_PRIVATE;
		else if (Modifier.isProtected(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_PROTECTED;
		else
			return JavaPluginImages.DESC_OBJS_INNER_CLASS_DEFAULT;
	}

	static ImageDescriptor getInnerInterfaceImageDescriptor(int modifiers) {
		if (Modifier.isPublic(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PUBLIC;
		else if (Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PRIVATE;
		else if (Modifier.isProtected(modifiers))
			return JavaPluginImages.DESC_OBJS_INNER_INTERFACE_PROTECTED;
		else
			return JavaPluginImages.DESC_OBJS_INTERFACE_DEFAULT;
	}

	static ImageDescriptor getInterfaceImageDescriptor(int modifiers) {
		if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_OBJS_INTERFACE;
		else
			return JavaPluginImages.DESC_OBJS_INTERFACE_DEFAULT;
	}

	static ImageDescriptor getMethodImageDescriptor(int modifiers) {
		if (Modifier.isPublic(modifiers))
			return JavaPluginImages.DESC_MISC_PUBLIC;
		if (Modifier.isProtected(modifiers))
			return JavaPluginImages.DESC_MISC_PROTECTED;
		if (Modifier.isPrivate(modifiers))
			return JavaPluginImages.DESC_MISC_PRIVATE;

		return JavaPluginImages.DESC_MISC_DEFAULT;
	}

	static void getMethodLabel(IMethodBinding binding, long flags, StringBuffer buffer) {
		// return type
		if (getFlag(flags, JavaElementLabels.M_PRE_TYPE_PARAMETERS)) {
			ITypeBinding[] parameters= binding.getTypeParameters();
			if (parameters.length > 0) {
				buffer.append('<');
				for (int i= 0; i < parameters.length; i++) {
					if (i > 0) {
						buffer.append(JavaElementLabels.COMMA_STRING);
					}
					buffer.append(parameters[i].getName());
				}
				buffer.append('>');
				buffer.append(' ');
			}
		}
		// return type
		if (getFlag(flags, JavaElementLabels.M_PRE_RETURNTYPE) && !binding.isConstructor()) {
			buffer.append(binding.getReturnType().getName());
			buffer.append(' ');
		}
		// qualification
		if (getFlag(flags, JavaElementLabels.M_FULLY_QUALIFIED)) {
			getTypeLabel(binding.getDeclaringClass(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & JavaElementLabels.P_COMPRESSED), buffer);
			buffer.append('.');
		}
		buffer.append(binding.getName());
		// parameters
		buffer.append('(');
		if (getFlag(flags, JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES)) {
			ITypeBinding[] arguments= getFlag(flags, JavaElementLabels.M_PARAMETER_TYPES) ? binding.getParameterTypes() : null;
			if (arguments != null) {
				for (int index= 0; index < arguments.length; index++) {
					if (index > 0) {
						buffer.append(JavaElementLabels.COMMA_STRING); //$NON-NLS-1$
					}
					if (arguments != null) {
						if (binding.isVarargs() && (index == arguments.length - 1)) {
							int dimension= arguments[index].getDimensions() - 1;
							if (dimension >= 0)
								buffer.append(arguments[index].getElementType().getName());
							else
								buffer.append(arguments[index].getName());
							for (int offset= 0; offset < dimension; offset++) {
								buffer.append("[]"); //$NON-NLS-1$
							}
							buffer.append("..."); //$NON-NLS-1$
						} else {
							buffer.append(arguments[index].getName());
						}
					}
				}
			}
		} else {
			if (binding.getParameterTypes().length > 0) {
				buffer.append("..."); //$NON-NLS-1$
			}
		}
		buffer.append(')');
		if (getFlag(flags, JavaElementLabels.M_EXCEPTIONS)) {
			ITypeBinding[] exceptions= binding.getExceptionTypes();
			if (exceptions.length > 0) {
				buffer.append(" throws "); //$NON-NLS-1$
				for (int index= 0; index < exceptions.length; index++) {
					if (index > 0) {
						buffer.append(JavaElementLabels.COMMA_STRING);
					}
					buffer.append(exceptions[index].getName());
				}
			}
		}
		if (getFlag(flags, JavaElementLabels.M_APP_TYPE_PARAMETERS)) {
			ITypeBinding[] parameters= binding.getTypeParameters();
			if (parameters.length > 0) {
				buffer.append(' ');
				buffer.append('<');
				for (int index= 0; index < parameters.length; index++) {
					if (index > 0) {
						buffer.append(JavaElementLabels.COMMA_STRING);
					}
					buffer.append(parameters[index].getName());
				}
				buffer.append('>');
			}
		}
		if (getFlag(flags, JavaElementLabels.M_APP_RETURNTYPE) && !binding.isConstructor()) {
			buffer.append(JavaElementLabels.DECL_STRING);
			buffer.append(binding.getReturnType().getName());
		}
		// post qualification
		if (getFlag(flags, JavaElementLabels.M_POST_QUALIFIED)) {
			buffer.append(JavaElementLabels.CONCAT_STRING);
			getTypeLabel(binding.getDeclaringClass(), JavaElementLabels.T_FULLY_QUALIFIED | (flags & JavaElementLabels.P_COMPRESSED), buffer);
		}
	}

	static ImageDescriptor getTypeImageDescriptor(boolean isInner, ITypeBinding binding, int flags) {
		if (binding.isEnum()) {
			return JavaPluginImages.DESC_OBJS_ENUM;
		} else if (binding.isAnnotation()) {
			return JavaPluginImages.DESC_OBJS_ANNOTATION;
		} else if (binding.isInterface()) {
			if ((flags & JavaElementImageProvider.LIGHT_TYPE_ICONS) != 0) {
				return JavaPluginImages.DESC_OBJS_INTERFACEALT;
			}
			if (isInner) {
				return getInnerInterfaceImageDescriptor(binding.getModifiers());
			}
			return getInterfaceImageDescriptor(binding.getModifiers());
		} else {
			if ((flags & JavaElementImageProvider.LIGHT_TYPE_ICONS) != 0) {
				return JavaPluginImages.DESC_OBJS_CLASSALT;
			}
			if (isInner) {
				return getInnerClassImageDescriptor(binding.getModifiers());
			}
			return getClassImageDescriptor(binding.getModifiers());
		}
	}

	static ImageDescriptor getTypeImageDescriptor(ITypeBinding binding, int flags) {
		return getTypeImageDescriptor(binding.getDeclaringClass() != null, binding, flags);
	}

	static void getTypeLabel(ITypeBinding binding, long flags, StringBuffer buffer) {
		String name= binding.getName();
		if (name.length() == 0) { // anonymous
			if (binding.isEnum()) {
				name= "{...}"; //$NON-NLS-1$
			} else {
				ITypeBinding ancestor= binding.getSuperclass();
				if (ancestor != null) {
					name= JavaUIMessages.getFormattedString("JavaElementLabels.anonym_type", ancestor.getName()); //$NON-NLS-1$
				}
			}
			if (name == null || name.length() == 0)
				name= JavaUIMessages.getString("JavaElementLabels.anonym"); //$NON-NLS-1$
		}
		buffer.append(name);
	}

	private ImageDescriptorRegistry fRegistry= null;

	/*
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof IBinding) {
			IBinding binding= (IBinding) element;
			int flags= JavaElementLabelProvider.SHOW_DEFAULT;
			return getImageLabel(new JavaElementImageDescriptor(getBaseImageDescriptor(binding, flags), computeJavaAdornmentFlags(binding, flags), ((flags & JavaElementImageProvider.SMALL_ICONS) != 0) ? JavaElementImageProvider.SMALL_SIZE : JavaElementImageProvider.BIG_SIZE));
		}
		return null;
	}

	Image getImageLabel(ImageDescriptor descriptor) {
		if (descriptor == null)
			return null;
		return getRegistry().get(descriptor);
	}

	ImageDescriptorRegistry getRegistry() {
		if (fRegistry == null) {
			fRegistry= JavaPlugin.getImageDescriptorRegistry();
		}
		return fRegistry;
	}

	/*
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof ITypeBinding) {
			StringBuffer buffer= new StringBuffer();
			getTypeLabel(((ITypeBinding) element), 0, buffer);
			return buffer.toString();
		} else if (element instanceof IMethodBinding) {
			StringBuffer buffer= new StringBuffer();
			getMethodLabel(((IMethodBinding) element), JavaElementLabels.M_PARAMETER_TYPES, buffer);
			return buffer.toString();
		} else if (element instanceof IVariableBinding) {
			StringBuffer buffer= new StringBuffer();
			getFieldLabel(((IVariableBinding) element), 0, buffer);
			return buffer.toString();
		}
		return null;
	}
}