/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

package org.eclipse.jface.text.source.translation;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.Assert;


/**
 * Default implementation of a tag handler factory
 *
 * @since 3.0
 */
public class TagHandlerFactory implements ITagHandlerFactory {

	private Map fHandlers;

	public void addTagHandler(String tag, ITagHandler handler)  {
		Assert.isNotNull(tag);
		Assert.isNotNull(handler);

		fHandlers.put(tag, handler);
	}

	public ITagHandler registerHandler(String tag)  {
		Assert.isNotNull(tag);
		return (ITagHandler)fHandlers.remove(tag);
	}


	/*
	 * @see org.eclipse.jface.text.source.ITagHandlerFactory#getHandler(java.lang.String)
	 */
	@Override
	public ITagHandler getHandler(String tag) {
		Assert.isNotNull(tag);

		return (ITagHandler)fHandlers.get(tag);
	}

	/*
	 * @see org.eclipse.jface.text.source.ITagHandlerFactory#findHandler(java.lang.String)
	 */
	@Override
	public ITagHandler findHandler(String text) {
		Assert.isNotNull(text);

		Iterator iter= fHandlers.values().iterator();
		while (iter.hasNext())  {
			ITagHandler handler= (ITagHandler)iter.next();
			if (handler.canHandleText(text))
				return handler;
		}
		return null;
	}
}
