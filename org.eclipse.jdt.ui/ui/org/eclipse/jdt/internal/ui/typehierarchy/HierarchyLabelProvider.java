/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelDecorator;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.OverrideIndicatorLabelDecorator;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * Label provider for the hierarchy viewers. Types in the hierarchy that are not belonging to the
 * input scope are rendered differntly.
  */
public class HierarchyLabelProvider extends AppearanceAwareLabelProvider {

	
	private static class HierarchyOverrideIndicatorLabelDecorator extends OverrideIndicatorLabelDecorator {
		
		private TypeHierarchyLifeCycle fHierarchy;
		
		public HierarchyOverrideIndicatorLabelDecorator(TypeHierarchyLifeCycle lifeCycle) {
			super(null);
			fHierarchy= lifeCycle;
		}
		
		/* (non-Javadoc)
		 * @see OverrideIndicatorLabelDecorator#getOverrideIndicators(IMethod)
		 */
		protected int getOverrideIndicators(IMethod method) throws JavaModelException {
			IType type= (IType) JavaModelUtil.toOriginal(method.getDeclaringType());
			ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
			if (hierarchy != null) {
				return findInHierarchy(type, hierarchy, method.getElementName(), method.getParameterTypes());
			}
			return 0;
		}
	}
	

	private static class FocusDescriptor extends CompositeImageDescriptor {
		private ImageDescriptor fBase;
		public FocusDescriptor(ImageDescriptor base) {
			fBase= base;
		}
		protected void drawCompositeImage(int width, int height) {
			drawImage(fBase.getImageData(), 0, 0);
			drawImage(JavaPluginImages.DESC_OVR_FOCUS.getImageData(), 0, 0);
		}
		protected Point getSize() {
			return JavaElementImageProvider.BIG_SIZE;
		}
		public int hashCode() {
			return fBase.hashCode();
		}
		public boolean equals(Object object) {
			return object != null && FocusDescriptor.class.equals(object.getClass()) && ((FocusDescriptor)object).fBase.equals(fBase);
		}		
	}

	private boolean fShowDefiningType;
	private TypeHierarchyLifeCycle fHierarchy;

	public HierarchyLabelProvider(TypeHierarchyLifeCycle lifeCycle) {
		super(DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS);
		fHierarchy= lifeCycle;
		fShowDefiningType= false;
		addLabelDecorator(new HierarchyOverrideIndicatorLabelDecorator(lifeCycle));
	}
	
	public void setShowDefiningType(boolean showDefiningType) {
		fShowDefiningType= showDefiningType;
	}
	
	public boolean isShowDefiningType() {
		return fShowDefiningType;
	}	
			
	private boolean isDifferentScope(IType type) {
		IJavaElement input= fHierarchy.getInputElement();
		if (input == null || input.getElementType() == IJavaElement.TYPE) {
			return false;
		}
			
		IJavaElement parent= type.getAncestor(input.getElementType());
		if (input.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
			if (parent == null || parent.getElementName().equals(input.getElementName())) {
				return false;
			}
		} else if (input.equals(parent)) {
			return false;
		}
		return true;
	}
	
	private IType getDefiningType(Object element) throws JavaModelException {
		int kind= ((IJavaElement) element).getElementType();
	
		if (kind != IJavaElement.METHOD && kind != IJavaElement.FIELD && kind != IJavaElement.INITIALIZER) {
			return null;
		}
		IType declaringType= (IType) JavaModelUtil.toOriginal(((IMember) element).getDeclaringType());
		if (kind != IJavaElement.METHOD) {
			return declaringType;
		}
		ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
		if (hierarchy == null) {
			return declaringType;
		}
		IMethod method= (IMethod) element;
		int flags= method.getFlags();
		if (Flags.isPrivate(flags) || Flags.isStatic(flags) || method.isConstructor()) {
			return declaringType;
		}
		IMethod res= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, declaringType, method.getElementName(), method.getParameterTypes(), false);
		if (res == null || method.equals(res)) {
			return declaringType;
		}
		return res.getDeclaringType();
	}

	/* (non-Javadoc)
	 * @see ILabelProvider#getText
	 */ 	
	public String getText(Object element) {
		String text= super.getText(element);
		if (fShowDefiningType) {
			try {
				IType type= getDefiningType(element);
				if (type != null) {
					StringBuffer buf= new StringBuffer(super.getText(type));
					buf.append(JavaElementLabels.CONCAT_STRING);
					buf.append(text);
					return buf.toString();			
				}
			} catch (JavaModelException e) {
			}
		}
		return text;
	}	
	
	
	/* (non-Javadoc)
	 * @see ILabelProvider#getImage
	 */ 
	public Image getImage(Object element) {
		Image result= null;
		if (element instanceof IType) {
			ImageDescriptor desc= getTypeImageDescriptor((IType) element);
			if (desc != null) {
				if (element.equals(fHierarchy.getInputElement())) {
					desc= new FocusDescriptor(desc);
				}
				result= JavaPlugin.getImageDescriptorRegistry().get(desc);
			}
		} else {
			result= fImageLabelProvider.getImageLabel(element, evaluateImageFlags(element));
		}

		if (fLabelDecorators != null && result != null) {
			for (int i= 0; i < fLabelDecorators.size(); i++) {
				ILabelDecorator decorator= (ILabelDecorator) fLabelDecorators.get(i);
				result= decorator.decorateImage(result, element);
			}
		}			
		return result;
	}

	private ImageDescriptor getTypeImageDescriptor(IType type) {
		ITypeHierarchy hierarchy= fHierarchy.getHierarchy();
		if (hierarchy == null) {
			return new JavaElementImageDescriptor(JavaPluginImages.DESC_OBJS_CLASS, 0, JavaElementImageProvider.BIG_SIZE);
		}
		
		IType originalType= (IType) JavaModelUtil.toOriginal(type);
		int flags= hierarchy.getCachedFlags(originalType);
		if (flags == -1) {
			return new JavaElementImageDescriptor(JavaPluginImages.DESC_OBJS_CLASS, 0, JavaElementImageProvider.BIG_SIZE);
		}
		
		boolean isInterface= Flags.isInterface(flags);
		boolean isInner= (type.getDeclaringType() != null);
		ImageDescriptor desc;
		if (isDifferentScope(type)) {
			desc= isInterface ? JavaPluginImages.DESC_OBJS_INTERFACEALT : JavaPluginImages.DESC_OBJS_CLASSALT;
		} else {
			desc= JavaElementImageProvider.getTypeImageDescriptor(isInterface, isInner, flags);
		}
		int adornmentFlags= 0;
		if (Flags.isFinal(flags)) {
			adornmentFlags |= JavaElementImageDescriptor.FINAL;
		}
		if (Flags.isAbstract(flags) && !isInterface) {
			adornmentFlags |= JavaElementImageDescriptor.ABSTRACT;
		}
		if (Flags.isStatic(flags)) {
			adornmentFlags |= JavaElementImageDescriptor.STATIC;
		}
		return new JavaElementImageDescriptor(desc, adornmentFlags, JavaElementImageProvider.BIG_SIZE);
	}

}
