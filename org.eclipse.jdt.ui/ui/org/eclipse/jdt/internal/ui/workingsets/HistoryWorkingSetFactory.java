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
package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class HistoryWorkingSetFactory implements IElementFactory {

	/**
	 * {@inheritDoc}
	 */
	public IAdaptable createElement(IMemento memento) {
		HistoryWorkingSet result= new HistoryWorkingSet(memento);
		return result;
	}

}
