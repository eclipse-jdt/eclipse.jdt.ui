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
package org.eclipse.jdt.internal.ui.workingsets.dyn;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.internal.IWorkbenchConstants;

/**
 * A DynamicWorkingSetFactory is used to recreate a persisted DynamicWorkingSet 
 * object.
 */
public class DynamicWorkingSetFactory implements IElementFactory {
    public IAdaptable createElement(IMemento memento) {
        String workingSetName = memento.getString(IWorkbenchConstants.TAG_NAME);
        String workingSetEditPageId = memento.getString(IWorkbenchConstants.TAG_EDIT_PAGE_ID);

        if (workingSetName == null)
            return null;

        DynamicWorkingSet workingSet = new DynamicWorkingSet(workingSetName, memento);
        workingSet.setId(workingSetEditPageId);
        return workingSet;
    }
}