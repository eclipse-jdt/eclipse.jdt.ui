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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jdt.ui.ProblemsLabelDecorator;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.search.ui.ISearchResultViewEntry;
import org.eclipse.search.ui.SearchUI;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

public class JavaSearchResultLabelProvider extends LabelProvider {
	public static final int SHOW_ELEMENT_CONTAINER= 1;
	public static final int SHOW_CONTAINER_ELEMENT= 2;
	public static final int SHOW_PATH= 3;
	
	public static final String POTENTIAL_MATCH= SearchMessages.getString("JavaSearchResultLabelProvider.potentialMatch"); //$NON-NLS-1$

	private AppearanceAwareLabelProvider fLabelProvider;
	private ILabelDecorator fDecorator;
	
	// Cache
	private IMarker fLastMarker;
	private IJavaElement fLastJavaElement;

	public JavaSearchResultLabelProvider() {
		fDecorator= PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
		fLabelProvider= 
			new AppearanceAwareLabelProvider(
				AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS  | JavaElementLabels.P_COMPRESSED,
				AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS);
		fLabelProvider.addLabelDecorator(new ProblemsLabelDecorator(null));
	}	

	public String getText(Object o) {
		fLastMarker= null; // reset cache
		IJavaElement javaElement= getJavaElement(o); // sets fLastMarker as side effect
		boolean isPotentialMatch= fLastMarker != null && fLastMarker.getAttribute(SearchUI.POTENTIAL_MATCH, false);
		if (javaElement == null) {
			if (fLastMarker != null) {
				if (isPotentialMatch) 
					return super.getText(fLastMarker.getResource()) + POTENTIAL_MATCH;
				else
					return super.getText(fLastMarker.getResource());
			}
			else
				return ""; //$NON-NLS-1$
		}

		if (javaElement instanceof IImportDeclaration)
			javaElement= ((IImportDeclaration)javaElement).getParent().getParent();

		String text= ""; //$NON-NLS-1$
		if (isPotentialMatch) 
			text= fLabelProvider.getText(javaElement) + POTENTIAL_MATCH;
		else
			text= fLabelProvider.getText(javaElement);

		if (fDecorator != null) {
			String decoratedText= fDecorator.decorateText(text, javaElement);
			if (decoratedText != null)
				return decoratedText;
		}
		return text;
	}

	public Image getImage(Object o) {
		IJavaElement javaElement= getJavaElement(o);

		Image image= fLabelProvider.getImage(o);
		if (image == null)
			return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_UNKNOWN);
		
		if (fDecorator != null && javaElement != null) {
			Image decoratedImage= fDecorator.decorateImage(image, javaElement);
			if (decoratedImage != null)
				return decoratedImage;
		}
		return image;
	}

	public void setAppearance(int appearanceFlag) {
		int flags= AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED;
		if (appearanceFlag == SHOW_ELEMENT_CONTAINER)
			flags |= JavaElementLabels.F_POST_QUALIFIED | JavaElementLabels.M_POST_QUALIFIED | JavaElementLabels.I_POST_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
							| JavaElementLabels.T_POST_QUALIFIED | JavaElementLabels.D_POST_QUALIFIED | JavaElementLabels.CF_POST_QUALIFIED  | JavaElementLabels.CU_POST_QUALIFIED;
			
		else if (appearanceFlag == SHOW_CONTAINER_ELEMENT)
			flags |= JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
				| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED;
		else if (appearanceFlag == SHOW_PATH) {
			flags |= JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
				| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED;
			flags |= JavaElementLabels.PREPEND_ROOT_PATH;
		}
		fLabelProvider.setTextFlags(flags);
	}


	private IJavaElement getJavaElement(Object o) {
		if (o instanceof IJavaElement)
			return (IJavaElement)o;

		IMarker marker= getMarker(o);
		if (marker == null)
			return null;

		return getJavaElement(marker, (ISearchResultViewEntry)o);
	}

	protected IMarker getMarker(Object o) {
		if (!(o instanceof ISearchResultViewEntry))
			return null;
		return ((ISearchResultViewEntry)o).getSelectedMarker();
	}
	
	private IJavaElement getJavaElement(IMarker marker, ISearchResultViewEntry entry) {
		if (fLastMarker != marker) {
			boolean canUseGroupByKey= !marker.getAttribute(SearchUI.POTENTIAL_MATCH, false)
								&& !marker.getAttribute(IJavaSearchUIConstants.ATT_IS_WORKING_COPY, false)
								&& !marker.getAttribute(IJavaSearchUIConstants.ATT_JE_HANDLE_ID_CHANGED, false);

			if (canUseGroupByKey && entry.getGroupByKey() instanceof IJavaElement)
				fLastJavaElement= (IJavaElement)entry.getGroupByKey();
			else
				fLastJavaElement= SearchUtil.getJavaElement(marker);
			fLastMarker= marker;
		}
		return fLastJavaElement;
	}

	public void addListener(ILabelProviderListener listener) {
		super.addListener(listener);
		fLabelProvider.addListener(listener);
	}

	public void dispose() {
		super.dispose();
		fLabelProvider.dispose();
	}

	public boolean isLabelProperty(Object element, String property) {
		return fLabelProvider.isLabelProperty(element, property);
	}

	public void removeListener(ILabelProviderListener listener) {
		super.removeListener(listener);
		fLabelProvider.removeListener(listener);
	}
}
