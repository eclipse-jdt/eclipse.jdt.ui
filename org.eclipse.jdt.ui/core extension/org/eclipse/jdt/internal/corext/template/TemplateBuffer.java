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
package org.eclipse.jdt.internal.corext.template;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * A template buffer is a container for a string and variables.
 */
public final class TemplateBuffer {
	
	/** The string of the template buffer */
	private String fString;
	/** The variable positions of the template buffer */
	private TemplatePosition[] fVariables;
	
	/**
	 * Creates a template buffer.
	 * 
	 * @param string the string
	 * @param variables the variable positions
	 * @throws CoreException for illegal variable positions
	 */
    public TemplateBuffer(String string, TemplatePosition[] variables) throws CoreException {
		setContent(string, variables);
    }

	/**
	 * Sets the content of the template buffer.
	 * 
	 * @param string the string
	 * @param variables the variable positions
	 * @throws CoreException for illegal variable positions
	 */
	public final void setContent(String string, TemplatePosition[] variables) throws CoreException {
		Assert.isNotNull(string);
		Assert.isNotNull(variables);

		// XXX assert non-overlapping variable properties

		fString= string;
		fVariables= variables;
	}

	/**
	 * Returns the string of the template buffer.
	 */
	public final String getString() {
		return fString;
	}
	
	/**
	 * Returns the variable positions of the template buffer.
	 */
	public final TemplatePosition[] getVariables() {
		return fVariables;
	}

}
