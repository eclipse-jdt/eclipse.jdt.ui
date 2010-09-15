/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledString;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaElementLabels;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;

/**
 * Action used for the type hierarchy forward / backward buttons
 */
public class HistoryAction extends Action {

	private TypeHierarchyViewPart fViewPart;
	private IJavaElement[] fElements;

	public HistoryAction(TypeHierarchyViewPart viewPart, IJavaElement[] elements) {
        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
		fViewPart= viewPart;
		fElements= elements;

		long flags= JavaElementLabels.ALL_POST_QUALIFIED | JavaElementLabels.ALL_DEFAULT;
		String elementName= concatenateElementsNames(elements, flags);
		setText(elementName);
		setImageDescriptor(getImageDescriptor(elements[0]));

		setDescription(Messages.format(TypeHierarchyMessages.HistoryAction_description, elementName));
		setToolTipText(Messages.format(TypeHierarchyMessages.HistoryAction_tooltip, elementName));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.HISTORY_ACTION);
	}

	/**
	 * Concatenates and returns the names of all the java elements into a single string.
	 * 
	 * @param elements the java elements
	 * @param flags the rendering flags
	 * @return the concatenated string of names of all the java elements
	 * @since 3.7
	 */
	protected static String concatenateElementsNames(IJavaElement[] elements, long flags) {
		String result= ""; //$NON-NLS-1$
		if (elements != null && elements.length > 0) {
			int min= Math.min(2, elements.length);
			for (int i= 0; i < min; i++) {
				String elementName= JavaElementLabels.getElementLabel(elements[i], flags);
				if (i > 0)
					result= Messages.format(TypeHierarchyMessages.HistoryAction_javaElementConcatenation, new String[] { result, elementName });
				else
					result= elementName;
			}
			if (elements.length > 2)
				result= Messages.format(TypeHierarchyMessages.HistoryAction_javaElementConcatenationWithEllipsis, new String[] { result });
		}
		return result;
	}

	private ImageDescriptor getImageDescriptor(IJavaElement elem) {
		JavaElementImageProvider imageProvider= new JavaElementImageProvider();
		ImageDescriptor desc= imageProvider.getBaseImageDescriptor(elem, 0);
		imageProvider.dispose();
		return desc;
	}

	/*
	 * @see Action#run()
	 */
	public void run() {
		fViewPart.gotoHistoryEntry(fElements);
	}

	/**
	 * Fetches the label for the java element.
	 * 
	 * @param element the java element
	 * @return the label for the java element
	 * @since 3.7
	 */
	static StyledString getSingleElementLabel(IJavaElement element) {
		return JavaElementLabels.getStyledElementLabel(element, JavaElementLabels.ALL_POST_QUALIFIED | JavaElementLabels.COLORIZE);
	}

	/**
	 * Fetches the label for all the java elements.
	 * 
	 * @param elements the java elements
	 * @return the label for all the java elements
	 * @since 3.7
	 */
	static String getElementLabel(IJavaElement[] elements) {
		switch (elements.length) {
			case 0:
				Assert.isTrue(false);
				return null;

			case 1:
				return JavaElementLabels.getElementLabel(elements[0], JavaElementLabels.ALL_POST_QUALIFIED);

			case 2:
				return Messages.format(TypeHierarchyMessages.HistoryAction_inputElements_2,
						new String[] { getShortLabel(elements[0]), getShortLabel(elements[1]) });

			case 3:
				return Messages.format(TypeHierarchyMessages.HistoryAction_inputElements_3,
						new String[] { getShortLabel(elements[0]), getShortLabel(elements[1]), getShortLabel(elements[2]) });

			default:
				return Messages.format(TypeHierarchyMessages.HistoryAction_inputElements_more,
						new String[] { getShortLabel(elements[0]), getShortLabel(elements[1]), getShortLabel(elements[2]) });
		}
	}

	/**
	 * Fetches the short label for the java element.
	 * 
	 * @param element the java element
	 * @return the short label for the java element
	 * @since 3.7
	 */
	private static String getShortLabel(IJavaElement element) {
		return JavaElementLabels.getElementLabel(element, 0L);
	}

}
