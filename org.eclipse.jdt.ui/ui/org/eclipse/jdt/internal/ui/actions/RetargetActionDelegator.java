/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.actions.RetargetAction;

/**
 * A special delegate that creates a retragetable action.
 */
/* package */ abstract class RetargetActionDelegator implements IWorkbenchWindowActionDelegate {

	private IAction fMenuBarAction;
	private RetargetAction fTargetAction;
	private IWorkbenchWindow fWindow;

	public void dispose() {
		fWindow.getPartService().removePartListener(fTargetAction);
	}

	public void init(IWorkbenchWindow window) {
		fWindow= window;
		fTargetAction= createRetargetAction();
		window.getPartService().addPartListener(fTargetAction);
		fTargetAction.addPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (fMenuBarAction != null)
					propogateChange(event);
			}
		});
		IWorkbenchPart activePart= window.getPartService().getActivePart();
		if (activePart != null)
			fTargetAction.partActivated(activePart);
	}

	public void run(IAction action) {
		fTargetAction.run();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// action managed by the retargetable action has to deceide whether it
		// wants to listen to selection changes or not.
		if (fMenuBarAction == null) {
			fMenuBarAction= action;
			fMenuBarAction.setEnabled(fTargetAction.isEnabled());
		}
	}
	
	protected RetargetAction createRetargetAction() {
		return new RetargetAction(getId(), "label"); //$NON-NLS-1$
	}
	
	protected abstract String getId();
	
	protected void propogateChange(PropertyChangeEvent event) {
		String property= event.getProperty();
		if (IAction.ENABLED.equals(property)) {
			Boolean bool = (Boolean)event.getNewValue();
			fMenuBarAction.setEnabled(bool.booleanValue());
		} else if (IAction.TEXT.equals(property)) {
			String text= (String)event.getNewValue();
			fMenuBarAction.setText(text);
		} else if (IAction.TOOL_TIP_TEXT.equals(property)) {
			String text= (String)event.getNewValue();
			fMenuBarAction.setToolTipText(text);
		}  else if (IAction.DESCRIPTION.equals(property)) {
			String text= (String)event.getNewValue();
			fMenuBarAction.setDescription(text);
		} else if (IAction.IMAGE.equals(property)) {
			ImageDescriptor image= (ImageDescriptor)event.getNewValue();
			fMenuBarAction.setImageDescriptor(image);
		}
	}
}
