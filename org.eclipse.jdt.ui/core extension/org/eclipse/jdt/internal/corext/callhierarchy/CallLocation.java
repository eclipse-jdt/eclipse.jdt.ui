/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *          (report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class CallLocation implements IAdaptable {
    public static final int UNKNOWN_LINE_NUMBER= -1;
    private IMember fCalledMember;
    private IMember fMember;
    private String fCallText;
    private int fEnd;
    private int fStart;
    private int fLineNumber;

    /**
     * @param method
     * @param cu
     * @param start
     * @param end
     */
    public CallLocation(IMember member, IMember calledMember, int start, int end) {
        this(member, calledMember, start, end, UNKNOWN_LINE_NUMBER);
    }

    /**
     * @param method
     * @param cu
     * @param start
     * @param end
     */
    public CallLocation(IMember member, IMember calledMember, int start, int end, int lineNumber) {
        this.fMember = member;
        this.fCalledMember = calledMember;
        this.fStart = start;
        this.fEnd = end;
        this.fLineNumber= lineNumber;
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

    public int getLineNumber() {
        if (fLineNumber == UNKNOWN_LINE_NUMBER) {
            CompilationUnit unit= CallHierarchy.getCompilationUnitNode(fMember, false);
            if (unit != null) {
                fLineNumber= unit.lineNumber(fStart);
            }
        }
        return fLineNumber;
    }
    
    public String toString() {
        return getCallText();
    }
    
    public String getCallText() {
        if (fCallText == null) {
            try {
				IOpenable openable= fMember.getOpenable();
				if (openable == null)
					return ""; //$NON-NLS-1$
				fCallText= openable.getBuffer().getText(fStart, (fEnd - fStart));
            } catch (JavaModelException e) {
                JavaPlugin.log(e);
                return "";    //$NON-NLS-1$
            }
        }
        return fCallText;
    }

    public Object getAdapter(Class adapter) {
        if (IJavaElement.class.isAssignableFrom(adapter)) {
            return getMember();
        }

        return null;
    }
}
