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

package org.eclipse.jface.text.source;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * Interface for marker and temporary annotations.
 * 
 * @see org.eclipse.core.resources.IMarker
 * @see org.eclipse.jface.text.source.Annotation
 * @since 3.0
 */
public interface IAnnotationExtension {

	/**
	 * Returns the type of the given annotation.
	 * 
	 * @return the type of the given annotation or <code>null</code> if it has none.
	 */
	Object getType();

	/**
	 * Returns whether the given annotation is temporary rather than persistent.
	 * 
	 * @return <code>true</code> if the annotation is temporary,
	 * 	<code>false</code> otherwise
	 */
	boolean isTemporary();

	/**
	 * Returns the message of this annotation.
	 * 
	 * @return the message of this annotation
	 */
	String getMessage();

	/**
	 * Returns an image for this annotation.
	 * 
	 * @param display the display for which the image is requested
	 * @return the image for this annotation
	 */
	Image getImage(Display display);
}
