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

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

class CallLocation implements IAdaptable {
    private IMember fCalledMember;
    private IMember fMember;
    private String fCallText;
    private int fEnd;
    private int fStart;

    /**
     * @param method
     * @param cu
     * @param start
     * @param end
     */
    public CallLocation(IMember member, IMember calledMember, int start, int end) {
        this.fMember = member;
        this.fCalledMember = calledMember;
        this.fStart = start;
        this.fEnd = end;

        fCallText = initializeCallText();
    }

    /**
     * @return IMethod
     */
    public IMember getCalledMember() {
        return fCalledMember;
    }

    /**
     *
     */
    public int getEnd() {
        return fEnd;
    }

    public IMember getMember() {
        return fMember;
    }

    /**
     *
     */
    public int getStart() {
        return fStart;
    }

    public String toString() {
        return fCallText;
    }

    private String initializeCallText() {
        try {
            ICompilationUnit compilationUnit = fMember.getCompilationUnit();

            if ((fMember != null) && (compilationUnit != null)) {
                IBuffer buffer = compilationUnit.getBuffer();

                return buffer.getText(fStart, (fEnd - fStart));
            } else {
                return fMember.getOpenable().getBuffer().getText(fStart, (fEnd - fStart));
            }
        } catch (JavaModelException e) {
            Utility.logError("CallLocation::toString: Error creating text", e);

            return "- error -";
        }
    }
    
    public Object getAdapter(Class adapter) {
        if (IJavaElement.class.isAssignableFrom(adapter)) {
            return getMember();
        }
        return null;
    }
}
