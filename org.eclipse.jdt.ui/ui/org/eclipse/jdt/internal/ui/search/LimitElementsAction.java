/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.action.Action;


public class LimitElementsAction extends Action {
	private JavaSearchResultPage fPage;
	
	public LimitElementsAction(JavaSearchResultPage page) {
		super(SearchMessages.getString("LimitElementsAction.label"), Action.AS_CHECK_BOX); //$NON-NLS-1$
		fPage= page;
	}

	public void run() {
		fPage.enableLimit(!fPage.limitElements());
	}

}
