/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;


import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Everything, even the kitchen sink.
 */
public class Utilities {
	
		/**
	 * Tries to convert the given object into an <code>IResource</code>. If the 
	 * object can't be converted into an <code>IResource</code> <code>null</code> 
	 * is returned.
	 */
	public static IResource convertToResource(Object o) {
		if (o instanceof IResource)
			return (IResource)o;
		if (o instanceof IAdaptable)
			return (IResource)((IAdaptable)o).getAdapter(IResource.class);
			
		return null;	
	}
	
	/**
	 * Returns the display of the given shell. If shell is <code>null</code>
	 * the method checks if there is a display associated with the current
	 * thread. If not, the default display is returned.
	 */
	public static Display getDisplay(Shell parent) {
		Display display;
		if (parent == null) {
			display= Display.getCurrent();
			if (display == null)
				display= Display.getDefault();
		} else {
			display= parent.getDisplay();
		}
		return display;		
	}
	
	/**
	 * Returns the shell for the given widget. If the widget doesn't represent
	 * a SWT object that manage a shell, <code>null</code> is returned.
	 */
	public static Shell getShell(Widget widget) {
		if (widget instanceof Control)
			return ((Control)widget).getShell();
		if (widget instanceof Caret)
			return ((Caret)widget).getParent().getShell();
		// XXX: Not present under Motif
		/*
		if (widget instanceof DragSource)
			return ((DragSource)widget).getControl().getShell();
		*/	
		if (widget instanceof DropTarget)
			return ((DropTarget)widget).getControl().getShell();
		if (widget instanceof Menu)
			return ((Menu)widget).getParent().getShell();
		if (widget instanceof ScrollBar)
			return ((ScrollBar)widget).getParent().getShell();
							
		return null;	
	}
		 
}