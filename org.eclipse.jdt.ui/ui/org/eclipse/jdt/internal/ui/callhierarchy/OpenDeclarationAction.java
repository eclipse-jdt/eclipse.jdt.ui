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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.actions.OpenAction;

import org.eclipse.jdt.internal.ui.util.SelectionUtil;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;

class OpenDeclarationAction extends OpenAction {
    public OpenDeclarationAction(IWorkbenchSite site) {
        super(site);
    }

    public boolean canActionBeAdded() {
        Object element = SelectionUtil.getSingleElement(getSelection());

        IMethod method = null;
        
        if (element instanceof IMethod) {
            method = (IMethod) element;
        } else if (element instanceof IAdaptable) {
            method = (IMethod) ((IAdaptable) element).getAdapter(IMethod.class);
        }
        
        if (method != null) {
            return true;
        }

        return false;
    }

    public ISelection getSelection() {
        ISelection selection= getSelectionProvider().getSelection();
        
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection= (IStructuredSelection) selection;
            List javaElements= new ArrayList();
            for (Iterator iter= structuredSelection.iterator(); iter.hasNext();) {
                Object element= iter.next();
                if (element instanceof MethodWrapper) {
                    javaElements.add(((MethodWrapper)element).getMember());
                }
            }
            return new StructuredSelection(javaElements);
        }
        return selection; 
    }

    public Object getElementToOpen(Object object) throws JavaModelException {
        if (object instanceof MethodWrapper) {
            return ((MethodWrapper) object).getMember();
        }
        return object;
    }   
}