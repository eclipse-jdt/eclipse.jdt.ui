/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class CallHierarchyMessages {
    private static final String RESOURCE_BUNDLE = "org.eclipse.jdt.internal.ui.callhierarchy.CallHierarchyMessages"; //$NON-NLS-1$
    private static ResourceBundle fgResourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE);

    private CallHierarchyMessages() {}

    /**
     * Gets a string from the resource bundle and formats it with the argument
     *
     * @param key   the string used to get the bundle value, must not be null
     */
    public static String getFormattedString(String key, Object arg) {
        String format = null;

        try {
            format = fgResourceBundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!"; //$NON-NLS-2$ //$NON-NLS-1$
        }

        if (arg == null) {
            arg = ""; //$NON-NLS-1$
        }

        return MessageFormat.format(format, new Object[] { arg });
    }

    /**
     * Gets a string from the resource bundle and formats it with arguments
     */
    public static String getFormattedString(String key, String[] args) {
        return MessageFormat.format(fgResourceBundle.getString(key), args);
    }

    public static String getString(String key) {
        try {
            return fgResourceBundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!"; //$NON-NLS-2$ //$NON-NLS-1$
        }
    }
}
