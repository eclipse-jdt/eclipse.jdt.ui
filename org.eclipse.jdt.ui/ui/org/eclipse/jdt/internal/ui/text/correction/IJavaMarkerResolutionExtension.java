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
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.swt.graphics.Image;

/**
 *
 */
public interface IJavaMarkerResolutionExtension {

	/**
	 * Returns optional additional information about the resolution.
	 * The additional information will be presented to assist the user
	 * in deciding if the selected proposal is the desired choice.
	 *
	 * @return the additional information or <code>null</code>
	 */
	String getDescription();
	
	/**
	 * Returns the image to be displayed in the list of resolutions.
	 * The image would typically be shown to the left of the display string.
	 *
	 * @return the image to be shown or <code>null</code> if no image is desired
	 */
	Image getImage();
		
	/**
	 * Returns the relevance of this resolution.
	 * <p> 
	 * The relevance is used to determine if this proposal is more
	 * relevant than another proposal.</p>
	 * 
	 * @return the relevance of this completion proposal in the range of [0, 100]
	 */
	int getRelevance();
}
