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

import java.util.Iterator;

import org.eclipse.swt.dnd.Clipboard;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtils;

public class CutAction extends SelectionDispatchAction{

	private CopyToClipboardAction fCopy;
	private DeleteAction fDelete;

	public CutAction(IWorkbenchSite site, Clipboard clipboard, SelectionDispatchAction pasteAction) {
		super(site);
		setText("Cu&t");
		fCopy= new CopyToClipboardAction(site, clipboard, pasteAction);
		fDelete= new DeleteAction(site);
		fDelete.setAskForConfirmation(false);

		ISharedImages workbenchImages= JavaPlugin.getDefault().getWorkbench().getSharedImages();
		setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT_DISABLED));
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
		setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT_HOVER));

		update(getSelection());
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.CUT_ACTION);
	}
	
	public void selectionChanged(IStructuredSelection selection) {
		/*
		 * cannot cut top-level types. this deletes the cu and then you cannot paste because the cu is gone. 
		 */
		if (! containsOnlyElementsInsideCompilationUnits(selection) || containsTopLevelTypes(selection)){
			setEnabled(false);
			return;
		}	
		fCopy.selectionChanged(selection);
		fDelete.selectionChanged(selection);
		setEnabled(fCopy.isEnabled() && fDelete.isEnabled());
	}

	private static boolean containsOnlyElementsInsideCompilationUnits(IStructuredSelection selection) {
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object object= iter.next();
			if (! (object instanceof IJavaElement && ReorgUtils.isInsideCompilationUnit((IJavaElement)object)))
				return false;
		}
		return true;
	}

	private static boolean containsTopLevelTypes(IStructuredSelection selection) {
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			if (isTopLevelType(iter.next()))
				return true;
		}
		return false;
	}

	private static boolean isTopLevelType(Object each) {
		return (each instanceof IType) && ((IType)each).getDeclaringType() == null;
	}

	public void run(IStructuredSelection selection) {
		selectionChanged(selection);
		if (isEnabled()) {
			fCopy.run(selection);
			fDelete.run(selection);
		}
	}
}