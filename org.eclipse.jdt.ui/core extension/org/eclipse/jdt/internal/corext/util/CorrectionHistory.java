/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import org.w3c.dom.Element;

/**
 * Containing command ids as objects and keys
 */
public class CorrectionHistory extends History {

	private static final String NODE_ID= "commandId"; //$NON-NLS-1$
	
	private static CorrectionHistory fgInstance;
	
	public static CorrectionHistory getDefault() {
		if (fgInstance == null)
			fgInstance= new CorrectionHistory();
		
		return fgInstance;
	}
	
	protected CorrectionHistory() {
		super("CorrectionHistory.xml"); //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	protected void setAttributes(Object object, Element element) {
		String s= (String)object;
		element.setAttribute(NODE_ID, s);
	}

	/**
	 * {@inheritDoc}
	 */
	protected Object createFromElement(Element element) {
		return element.getAttribute(NODE_ID);
	}

	/**
	 * {@inheritDoc}
	 */
	protected Object getKey(Object object) {
		return object;
	}

}
