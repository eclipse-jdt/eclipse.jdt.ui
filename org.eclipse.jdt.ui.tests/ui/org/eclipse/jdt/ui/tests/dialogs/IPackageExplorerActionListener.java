/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.ui.tests.dialogs;



/**
 * Interface for listeners of <code>PackageExplorerActionEvent</code>.
 */
public interface IPackageExplorerActionListener {
    
    /**
     * Handle the <code>PackageExplorerActionEvent</code> which is fired 
     * whenever the set of available actions changes.
     * 
     * @param event event to be processed
     * 
     * @see PackageExplorerActionEvent
     */
    public void handlePackageExplorerActionEvent(PackageExplorerActionEvent event);
    
}
