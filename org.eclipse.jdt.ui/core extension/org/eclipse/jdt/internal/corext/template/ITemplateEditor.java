/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template;

import org.eclipse.core.runtime.CoreException;

/**
 * A template editor implements an action to edit a template buffer in its context.
 */
public interface ITemplateEditor {

	/**
	 * Modifies a template buffer.
	 * 
	 * @param buffer the template buffer
	 * @param context the template context
	 * @throws CoreException if the buffer cannot be successfully modified
	 */
	void edit(TemplateBuffer buffer, TemplateContext context) throws CoreException;

}
