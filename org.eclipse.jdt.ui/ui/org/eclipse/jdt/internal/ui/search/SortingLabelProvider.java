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
package org.eclipse.jdt.internal.ui.search;

import java.text.MessageFormat;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.swt.graphics.Image;


public class SortingLabelProvider extends SearchLabelProvider implements IColorProvider {
	public static final int SHOW_ELEMENT_CONTAINER= 1; // default
	public static final int SHOW_CONTAINER_ELEMENT= 2;
	public static final int SHOW_PATH= 3;
	
	public SortingLabelProvider(JavaSearchResultPage page) {
		super(page, new AppearanceAwareLabelProvider());
	}	

	public Image getImage(Object element) {
		Image image= null;
		if (element instanceof IJavaElement || element instanceof IResource)
			image= getLabelProvider().getImage(element);
		if (image != null)
			return image;
		return getParticipantImage(element);
	}
		
	public final String getText(Object element) {
		int matchCount= fPage.getDisplayedMatchCount(element);
		String text= internalGetText(element);
		if (matchCount < 2) {
			String label= getSingularLabel(element);
			return MessageFormat.format(label, new String[] { text });
		}
		String label= getPluralLabel(element);
		return MessageFormat.format(label, new Object[] { text,  new Integer(matchCount) });
	}

	private String getSingularLabel(Object element) {
		if (hasPotentialMatches(element))
			return SearchMessages.getString("SortingLabelProvider.potential_singular"); //$NON-NLS-1$
		return SearchMessages.getString("SortingLabelProvider.exact_singular"); //$NON-NLS-1$
	}

	private String getPluralLabel(Object element) {
		if (hasPotentialMatches(element))
			return SearchMessages.getString("SortingLabelProvider.potential_plural"); //$NON-NLS-1$
		return SearchMessages.getString("SortingLabelProvider.exact_plural"); //$NON-NLS-1$
	}

	private String internalGetText(Object o) {
		if (o instanceof IImportDeclaration)
			o= ((IImportDeclaration)o).getParent().getParent();

		String text= getLabelProvider().getText(o);
		if (text != null && (!"".equals(text))) //$NON-NLS-1$
			return text;
		return getParticipantText(o);	
	}

	public void setOrder(int orderFlag) {
		int flags= AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS | JavaElementLabels.P_COMPRESSED;
		if (orderFlag == SHOW_ELEMENT_CONTAINER)
			flags |= JavaElementLabels.F_POST_QUALIFIED | JavaElementLabels.M_POST_QUALIFIED | JavaElementLabels.I_POST_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
							| JavaElementLabels.T_POST_QUALIFIED | JavaElementLabels.D_POST_QUALIFIED | JavaElementLabels.CF_POST_QUALIFIED  | JavaElementLabels.CU_POST_QUALIFIED;
			
		else if (orderFlag == SHOW_CONTAINER_ELEMENT)
			flags |= JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
				| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED;
		else if (orderFlag == SHOW_PATH) {
			flags |= JavaElementLabels.F_FULLY_QUALIFIED | JavaElementLabels.M_FULLY_QUALIFIED | JavaElementLabels.I_FULLY_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES
				| JavaElementLabels.T_FULLY_QUALIFIED | JavaElementLabels.D_QUALIFIED | JavaElementLabels.CF_QUALIFIED  | JavaElementLabels.CU_QUALIFIED;
			flags |= JavaElementLabels.PREPEND_ROOT_PATH;
		}
		getLabelProvider().setTextFlags(flags);
	}
}
