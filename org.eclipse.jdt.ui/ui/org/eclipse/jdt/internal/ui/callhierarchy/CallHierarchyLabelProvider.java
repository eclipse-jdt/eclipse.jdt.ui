/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 * 			(report 36180: Callers/Callees view)
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import java.util.Collection;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;

import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;


class CallHierarchyLabelProvider extends AppearanceAwareLabelProvider {
	private static final long TEXTFLAGS= DEFAULT_TEXTFLAGS | JavaElementLabels.ALL_POST_QUALIFIED | JavaElementLabels.P_COMPRESSED;

	private static final int IMAGEFLAGS= DEFAULT_IMAGEFLAGS | JavaElementImageProvider.SMALL_ICONS;

	private ILabelDecorator fDecorator;

	CallHierarchyLabelProvider() {
		super(TEXTFLAGS, IMAGEFLAGS);
		fDecorator= new CallHierarchyLabelDecorator();
	}

	/*
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		Image result= null;
		if (element instanceof MethodWrapper) {
			MethodWrapper methodWrapper= (MethodWrapper)element;

			if (methodWrapper.getMember() != null) {
				result= fDecorator.decorateImage(super.getImage(methodWrapper.getMember()), methodWrapper);
			}
		} else if (isPendingUpdate(element)) {
			return null;
		} else {
			result= super.getImage(element);
		}

		return result;
	}

	/*
	 * @see ILabelProvider#getText(Object)
	 */
	public String getText(Object element) {
		if (element instanceof MethodWrapper && ((MethodWrapper)element).getMember() != null) {
			return getElementLabel((MethodWrapper)element);
		}
		return getSpecialLabel(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.viewsupport.JavaUILabelProvider#getStyledText(java.lang.Object)
	 */
	public StyledString getStyledText(Object element) {
		if (element instanceof MethodWrapper && ((MethodWrapper)element).getMember() != null) {
			MethodWrapper wrapper= (MethodWrapper)element;
			String decorated= getElementLabel(wrapper);
			StyledString text= super.getStyledText(wrapper.getMember());
			return StyledCellLabelProvider.styleDecoratedString(text, decorated, StyledString.COUNTER_STYLER);
		}
		return new StyledString(getSpecialLabel(element));
	}

	private String getSpecialLabel(Object element) {
		if (element instanceof MethodWrapper) {
			return CallHierarchyMessages.CallHierarchyLabelProvider_root;
		} else if (element == TreeTermination.SEARCH_CANCELED) {
			return CallHierarchyMessages.CallHierarchyLabelProvider_searchCanceled;
		} else if (isPendingUpdate(element)) {
			return CallHierarchyMessages.CallHierarchyLabelProvider_updatePending;
		}
		return CallHierarchyMessages.CallHierarchyLabelProvider_noMethodSelected;
	}

	private boolean isPendingUpdate(Object element) {
		return element instanceof IWorkbenchAdapter;
	}

	private String getElementLabel(MethodWrapper methodWrapper) {
		String label= super.getText(methodWrapper.getMember());

		Collection callLocations= methodWrapper.getMethodCall().getCallLocations();

		if ((callLocations != null) && (callLocations.size() > 1)) {
			return Messages.format(CallHierarchyMessages.CallHierarchyLabelProvider_matches, new String[] { label, String.valueOf(callLocations.size()) });
		}

		return label;
	}
}
