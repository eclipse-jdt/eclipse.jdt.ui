/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui.display;

import java.util.ResourceBundle;import org.eclipse.ui.IWorkbenchPart;import org.eclipse.ui.texteditor.ResourceAction;

/**
 * Clears the display.
 */
public class ClearDisplayAction extends ResourceAction {
	
	private IWorkbenchPart fWorkbenchPart;

	public ClearDisplayAction(ResourceBundle bundle, String prefix, IWorkbenchPart workbenchPart) {
		super(bundle, prefix);
		fWorkbenchPart= workbenchPart;
	}

	/**
	 * @see Action#run
	 */
	public void run() {
		Object value= fWorkbenchPart.getAdapter(IDataDisplay.class);
		if (value instanceof IDataDisplay) {
			IDataDisplay dataDisplay= (IDataDisplay) value;
			dataDisplay.clear();
		}
	}
}
