/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.workingsets;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PlatformUI;

public class Mementos {

    public static final String TAG_FACTORY_ID = "factoryID"; //$NON-NLS-1$
    public static final String TAG_ITEM = "item"; //$NON-NLS-1$

    public static void saveItem(IMemento memento, IAdaptable element) {
        IPersistableElement persistable= element.getAdapter(IPersistableElement.class);
        if (persistable != null) {
            memento.putString(
            	TAG_FACTORY_ID,
                persistable.getFactoryId());
            persistable.saveState(memento);
        }

    }

	public static IAdaptable restoreItem(IMemento memento) {
		return restoreItem(memento, TAG_FACTORY_ID);
	}

	public static IAdaptable restoreItem(IMemento memento, String factoryTag) {
		if (memento == null)
			return null;
	    String factoryID = memento.getString(factoryTag);
	    if (factoryID == null) return null;
	    IElementFactory factory = PlatformUI.getWorkbench().getElementFactory(factoryID);
	    if (factory == null) return null;
	    return factory.createElement(memento);
	}

	private Mementos() {
	}
}
