/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.util.SelectionUtil;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

class SelectionProviderAdapter implements ISelectionProvider {
    private ISelectionProvider fProvider;
    private ISelectionChangedListener fListener;

    SelectionProviderAdapter(ISelectionProvider provider) {
        this.fProvider= provider;
    }

    public void addSelectionChangedListener(final ISelectionChangedListener listener) {
        fListener=
            new ISelectionChangedListener() {
                public void selectionChanged(SelectionChangedEvent event) {
                    listener.selectionChanged(new SelectionChangedEvent(fProvider, convertSelection(event.getSelection())));
                }
            };
        fProvider.addSelectionChangedListener(fListener);
    }

    public ISelection getSelection() {
        return convertSelection(fProvider.getSelection());
    }

    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        fProvider.removeSelectionChangedListener(fListener);
    }

    public void setSelection(ISelection selection) {
    }
    
    private ISelection convertSelection(ISelection selection) {
        Object element= SelectionUtil.getSingleElement(selection);
        if (element instanceof MethodWrapper) {
            MethodWrapper methodWrapper= (MethodWrapper) element;
            if (methodWrapper != null) {
                    IJavaElement je= methodWrapper.getMember();
                    if (je != null)
                        return new StructuredSelection(je);
            }
        }
        return StructuredSelection.EMPTY;       
    }
}