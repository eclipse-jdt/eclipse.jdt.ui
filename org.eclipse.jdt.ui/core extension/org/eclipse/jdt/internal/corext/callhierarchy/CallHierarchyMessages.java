/*******************************************************************************
 * Copyright (c) 2000, orporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 * 			(report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class CallHierarchyMessages {

    private static final String BUNDLE_NAME= "org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchyMessages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE=
        ResourceBundle.getBundle(BUNDLE_NAME);

    private CallHierarchyMessages() {
    }

    /**
     * @param key
     * @return
     */
    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }
}
