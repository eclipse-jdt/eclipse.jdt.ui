/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/ 
package org.eclipse.jdt.internal.ui.navigator;

import org.eclipse.jface.action.IMenuManager;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

import org.eclipse.jdt.ui.actions.RefactorActionGroup;

public class JavaRefactorActionProvider extends CommonActionProvider {
	  
	private RefactorActionGroup fRefactorGroup;
	
	private boolean fInViewPart = false; 
	
	public void fillActionBars(IActionBars actionBars) { 
		if (fInViewPart) {  
			fRefactorGroup.fillActionBars(actionBars);
			fRefactorGroup.retargetFileMenuActions(actionBars); 
		} 
	}
	

	public void fillContextMenu(IMenuManager menu) { 
		if (fInViewPart) {  
			fRefactorGroup.fillContextMenu(menu); 
		}
	}

	public void init(ICommonActionExtensionSite site) {

		ICommonViewerWorkbenchSite workbenchSite = null;
		if (site.getViewSite() instanceof ICommonViewerWorkbenchSite)
			workbenchSite = (ICommonViewerWorkbenchSite) site.getViewSite();
 
		if (workbenchSite != null) {
			if (workbenchSite.getPart() != null && workbenchSite.getPart() instanceof IViewPart) {
				IViewPart viewPart = (IViewPart) workbenchSite.getPart();
				  
				fRefactorGroup = new RefactorActionGroup(viewPart); 
				
				fInViewPart = true;
			} 
		} 
	}

	public void setContext(ActionContext context) {
		super.setContext(context);
		if (fInViewPart) {  
			fRefactorGroup.setContext(context); 
		}
	}

	public void restoreState(IMemento memento) { 
	} 

	public void saveState(IMemento aMemento) { 

	}

}
