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
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.ui.IMarkerResolution2;

/**
 *
 */
public interface IJavaMarkerResolutionExtension extends IMarkerResolution2 {
		
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
