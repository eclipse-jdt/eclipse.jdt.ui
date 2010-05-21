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

package org.eclipse.jdt.internal.ui.examples.jspeditor;

import org.eclipse.jface.text.source.translation.ITagHandler;
import org.eclipse.jface.text.source.translation.ITagHandlerFactory;

/**
 * First cut of JSP to Java tag handler factory.
 * Only one handler is used to do the job.
 * 
 * @since 3.0
 */
public class Jsp2JavaTagHandlerFactory implements ITagHandlerFactory {
	
	private ITagHandler fTagHandler= new Jsp2JavaTagHandler();

	/*
	 * @see org.eclipse.jface.text.source.IHandlerFactory#getHandler(java.lang.String)
	 */
	public ITagHandler getHandler(String tag) {
		fTagHandler.reset(tag);
		return fTagHandler;
	}

	/*
	 * @see org.eclipse.jface.text.source.IHandlerFactory#findHandler(java.lang.String)
	 */
	public ITagHandler findHandler(String text) {
		return fTagHandler;
	}

}
