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

import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;
import org.eclipse.ui.texteditor.MarkerAnnotationPreferences;

/**
 * Default annotation access.
 * <p>
 * FIXME: the "extends" relationship needs to be inverted in the final solution.
 * </p>
 * 
 * @since 3.0
 */
public class DefaultAnnotationAccess extends DefaultMarkerAnnotationAccess {

	/**
	 * Creates a default annotation access for
	 * annotations that implement <code>IAnnotationExtension</code>.
	 */
	public DefaultAnnotationAccess() {
		super(new MarkerAnnotationPreferences());
	}

	/*
	 * @see org.eclipse.jface.text.source.IAnnotationAccess#getType(org.eclipse.jface.text.source.Annotation)
	 */
	public Object getType(Annotation annotation) {
		if (annotation instanceof IAnnotationExtension) {
			IAnnotationExtension extension= (IAnnotationExtension)annotation;
			return extension.getType();
		} else
			return super.getType(annotation);
	}
}
