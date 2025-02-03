/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	private final ITagHandler fTagHandler= new Jsp2JavaTagHandler();

	@Override
	public ITagHandler getHandler(String tag) {
		fTagHandler.reset(tag);
		return fTagHandler;
	}

	@Override
	public ITagHandler findHandler(String text) {
		return fTagHandler;
	}

}
