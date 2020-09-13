/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui;

import org.eclipse.jdt.ui.JavaUI;


public interface IUIConstants {

	String KEY_OK= JavaUI.ID_PLUGIN + ".ok.label"; //$NON-NLS-1$
	String KEY_CANCEL= JavaUI.ID_PLUGIN + ".cancel.label"; //$NON-NLS-1$

	String P_ICON_NAME= JavaUI.ID_PLUGIN + ".icon_name"; //$NON-NLS-1$

	String DIALOGSTORE_LASTEXTJAR= JavaUI.ID_PLUGIN + ".lastextjar"; //$NON-NLS-1$
	String DIALOGSTORE_LASTEXTJARFOLDER= JavaUI.ID_PLUGIN + ".lastextjarfolder"; //$NON-NLS-1$
	String DIALOGSTORE_LASTJARATTACH= JavaUI.ID_PLUGIN + ".lastjarattach"; //$NON-NLS-1$
	String DIALOGSTORE_LASTVARIABLE= JavaUI.ID_PLUGIN + ".lastvariable";	 //$NON-NLS-1$

	String DIALOGSTORE_TYPECOMMENT_DEPRECATED= JavaUI.ID_PLUGIN + ".typecomment.deprecated";	 //$NON-NLS-1$

}
