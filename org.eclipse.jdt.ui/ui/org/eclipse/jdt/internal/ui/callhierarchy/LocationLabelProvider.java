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

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

import org.eclipse.jdt.internal.corext.callhierarchy.CallLocation;

class LocationLabelProvider extends LabelProvider {
    
    LocationLabelProvider() {
    }
            
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    public String getText(Object element) {
        String text= ""; //$NON-NLS-1$        
        if (element instanceof CallLocation) {
            text= removeWhitespaceOutsideStringLiterals((CallLocation) element);
        }

        return text;
    }

    public Image getImage(Object element) {
        return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_SEARCH_OCCURRENCE);
    }
    
    /**
     * @param string
     * @return String
     */
    private String removeWhitespaceOutsideStringLiterals(CallLocation callLocation) {
        StringBuffer buf = new StringBuffer();
        boolean withinString = false;

        String s= callLocation.toString();
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
