/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import org.eclipse.swt.dnd.Clipboard;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.corext.refactoring.reorg2.CopyToClipboardAction;
import org.eclipse.jdt.internal.corext.refactoring.reorg2.DeleteAction;
import org.eclipse.jdt.internal.corext.refactoring.reorg2.PasteAction;

public class ReorgActionFactory {

	private ReorgActionFactory(){
	}

	public static SelectionDispatchAction createCutAction(IWorkbenchSite site, Clipboard clipboard, SelectionDispatchAction pasteAction){
		String helpContextID= IJavaHelpContextIds.CUT_ACTION;
		SelectionDispatchAction a1= new CutSourceReferencesToClipboardAction(site, clipboard, pasteAction, helpContextID);

		ISharedImages workbenchImages= JavaPlugin.getDefault().getWorkbench().getSharedImages();
		a1.setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT_DISABLED));
		a1.setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
		a1.setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT_HOVER));
		return a1;
	}
	
	public static SelectionDispatchAction createCopyAction(IWorkbenchSite site, Clipboard clipboard, SelectionDispatchAction pasteAction){
		return new CopyToClipboardAction(site, clipboard, pasteAction);
	}
	
	public static SelectionDispatchAction createPasteAction(IWorkbenchSite site, Clipboard clipboard){
		return new PasteAction(site, clipboard);
	}
	
	public static SelectionDispatchAction createDeleteAction(IWorkbenchSite site){
		return new DeleteAction(site);
	}
}
