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

import org.eclipse.jface.viewers.LabelProvider;

class LocationLabelProvider extends LabelProvider {
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    public String getText(Object element) {
        if (element instanceof CallLocation) {
            CallLocation callLocation = (CallLocation) element;

            return removeWhitespaceOutsideStringLiterals(callLocation.toString());
        }

        return super.getText(element);
    }

    /**
     * @param string
     * @return String
     */
    private String removeWhitespaceOutsideStringLiterals(String s) {
        StringBuffer buf = new StringBuffer();
        boolean withinString = false;

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            if (ch == '"') {
                withinString = !withinString;
            }

            if (withinString) {
                buf.append(ch);
            } else if (Character.isWhitespace(ch)) {
                if ((buf.length() == 0) ||
                            !Character.isWhitespace(buf.charAt(buf.length() - 1))) {
                    if (ch != ' ') {
                        ch = ' ';
                    }

                    buf.append(ch);
                }
            } else {
                buf.append(ch);
            }
        }

        return buf.toString();
    }
}
