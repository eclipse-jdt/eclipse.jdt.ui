/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.text.MessageFormat;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.*;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;

import org.eclipse.ui.texteditor.IUpdate;


public abstract class JavaHistoryAction extends Action implements ISelectionChangedListener, IUpdate {
	
	protected ResourceBundle fBundle;
	protected String fTitle;
	private String fLabel;
	private String fLabelWithArg;
	protected ISelectionProvider fSelectionProvider;


	public JavaHistoryAction(ISelectionProvider sp, String bundleName) {
		
		fBundle= ResourceBundle.getBundle(bundleName);
		fTitle= getResourceString("title", "title");
		fLabel= getResourceString("actionLabel", "actionLabel");
		fLabelWithArg= getResourceString("actionLabelWithArg", fLabel);
				
		fSelectionProvider= sp;
		
		update();
	}
	
	protected String getResourceString(String key, String dfltValue) {
		
		if (fBundle != null) {
			try {
				return fBundle.getString(key);
			} catch (MissingResourceException x) {
			}
		}
		return dfltValue;
	}		
		
	protected String getResourceString(String key) {
		return getResourceString(key, key);
	}		
		
	/**
	 * @see IUpdate#update
	 */
	public void update() {
		updateLabel(fSelectionProvider.getSelection());
	}
	
	/**
	 * @see ISelectionAction#selectionChanged
	 */	
	public final void selectionChanged(SelectionChangedEvent e) {
		updateLabel(e.getSelection());
	}
		
	IMember getEditionElement(ISelection selection) {
		
		if (selection instanceof IStructuredSelection) {
			Object[] o= ((IStructuredSelection)selection).toArray();
			if (o != null && o.length == 1 && o[0] instanceof IMember) {
				IMember m= (IMember) o[0];
				if (!m.isBinary() && JavaStructureCreator.hasEdition((IJavaElement) o[0]))
					return m;
			}
		}
		return null;
	}
	
	void updateLabel(ISelection selection) {
		String name= getLabelName(selection);
		if (name != null) {
			setText(MessageFormat.format(fLabelWithArg, new String[] { getLabelName(selection) }));
			setEnabled(true);
		} else {
			setText(MessageFormat.format(fLabel, new String[0]));
			setEnabled(false);
		}
	}
	
	protected String getLabelName(ISelection selection) {
		return null;
	}
}