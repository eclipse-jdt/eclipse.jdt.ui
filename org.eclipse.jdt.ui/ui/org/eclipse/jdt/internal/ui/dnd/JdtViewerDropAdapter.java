package org.eclipse.jdt.internal.ui.dnd;
/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.StructuredViewer;

/**
 * An drag and drop adapter to be used together with structured viewers.
 * The adpater delegates the <code>dragEnter</code>, <code>dragOperationChanged
 * </code>, <code>dragOver</code> and <code>dropAccept</code> method to the <
 * code>validateDrop</code> method. Furthermore it adds location feedback.
 */
public class JdtViewerDropAdapter implements DropTargetListener {

	/**
	 * Constant describing the position of the cursor relative 
	 * to the target object.  This means the mouse is positioned
	 * slightly before the target.
	 */
	protected static final int LOCATION_BEFORE= 1;
	
	/**
	 * Constant describing the position of the cursor relative 
	 * to the target object.  This means the mouse is positioned
	 * slightly after the target.
	 */
	protected static final int LOCATION_AFTER= 2;
	
	/**
	 * Constant describing the position of the cursor relative 
	 * to the target object.  This means the mouse is positioned
	 * directly on the target.
	 */
	protected static final int LOCATION_ON= 3;
	
	/**
	 * Constant describing the position of the cursor relative 
	 * to the target object.  This means the mouse is not positioned
	 * over or near any valid target.
	 */
	protected static final int LOCATION_NONE= 4;
	
	/**
	 * The threshold used to determine if the mouse is before or after
	 * a item.
	 */
	private static final int LOCATION_EPSILON= 5; 
	
	/**
	 * The threshold used to determine if the mouse is near the border
	 * and scrollinh should occur.
	 */
	private static final int SCROLL_EPSILON= 20;
	
	/**
	 * Style to enable location feedback.
	 */
	public static final int INSERTION_FEEDBACK= 1 << 1; 

	private int fStyle;
	private StructuredViewer fViewer;
	private int fRequestedOperation;
	private int fLastOperation;
	private long fLastScroll;
	protected int fLocation;
	protected Object fTarget;

	public JdtViewerDropAdapter(StructuredViewer viewer, int style) {
		fViewer= viewer;
		Assert.isNotNull(fViewer);
		fStyle= style;
		fLastOperation= -1;
	}

	/**
	 * Returns the viewer this adapter is working on.
	 */
	protected StructuredViewer getViewer() {
		return fViewer;
	} 
	
	//---- Hooks to override -----------------------------------------------------
	
	/**
	 * The actual drop has occurred. Calls <code>drop(Object target, DropTargetEvent event)
	 * </code>.
	 * @see DropTargetListener.drop(DropTargetEvent)
	 */	 
	public void drop(DropTargetEvent event) {
		drop(fTarget, event);
	}
	
	/**
	 * The actual drop has occurred.
	 * @param target the drop target in form of a domain element.
	 * @param event the drop traget event
	 */	 
	public void drop(Object target, DropTargetEvent event) {
	}
	
	/**
	 * Validates if the drop is valid. The method calls <code>validateDrop
	 * (Object target, DropTargetEvent event). Implementors can alter the 
	 * <code>currentDataType</code> field and the <code>detail</code> field 
	 * to give feedback about drop acceptence.
	 */
	public void validateDrop(DropTargetEvent event) {
		validateDrop(fTarget, event, fRequestedOperation);
	}
	
	/**
	 * Validates if the drop on the current target is valid. The method
	 * can alter the <code>currentDataType</code> field and the <code>
	 * detail</code> field to give feedback about drop acceptence.
	 * @param target the drop target in form of a domain element.
	 * @param event the drop traget event
	 * @param operation the operation requested by the user.
	 */
	public void validateDrop(Object target, DropTargetEvent event, int operation) {
	}
	
	public void dragEnter(DropTargetEvent event) {
		fLastScroll= System.currentTimeMillis();
		dragOperationChanged(event);
	}
	
	public void dragLeave(DropTargetEvent event) {
		fTarget= null;
		fLocation= LOCATION_NONE;
		fLastScroll= 0;
	}
	
	public void dragOperationChanged(DropTargetEvent event) {
		fRequestedOperation= event.detail;
		fTarget= computeTarget(event);
		fLocation= computeLocation(event);
		validateDrop(event);
		fLastOperation= event.detail;
		computeFeedback(event);
	}
	
	public void dragOver(DropTargetEvent event) {
		Object oldTarget= fTarget;
		fTarget= scrollIfNeeded(event.x, event.y);
		// didn't scroll. So use normal target.
		if (fTarget == null) 
			fTarget= computeTarget(event);
		
		//set the location feedback
		int oldLocation= fLocation;
		fLocation= computeLocation(event);
		if (oldLocation != fLocation || oldTarget != fTarget || fLastOperation != event.detail) {
			validateDrop(event);
			fLastOperation= event.detail;
		} else {
			event.detail= fLastOperation;
		}
		computeFeedback(event);
	}
	
	public void dropAccept(DropTargetEvent event) {
		fTarget= computeTarget(event);
		validateDrop(event);
		fLastOperation= event.detail;
	}
	
	/**
	 * Returns the data hold by <code>event.item</code>. Inside a viewer
	 * this corresponds to the items data model element.
	 */
	protected Object computeTarget(DropTargetEvent event) {
		return event.item == null ? null : event.item.getData();
	}
	
	/**
	 * Returns the position of the given coordinates relative to the given target.
	 * The position is determined to be before, after, or on the item, based on 
	 * some threshold value. The return value is one of the LOCATION_* constants 
	 * defined in this class.
	 */
	final protected int computeLocation(DropTargetEvent event) {
		if (!(event.item instanceof Item)) {
			return LOCATION_NONE;
		}
		
		Item item= (Item) event.item;
		Point coordinates= new Point(event.x, event.y);
		coordinates= fViewer.getControl().toControl(coordinates);
		Rectangle bounds= getBounds(item);
		if (bounds == null) {
			return LOCATION_NONE;
		}
		if ((coordinates.y - bounds.y) < LOCATION_EPSILON) {
			return LOCATION_BEFORE;
		}
		if ((bounds.y + bounds.height - coordinates.y) < LOCATION_EPSILON) {
			return LOCATION_AFTER;
		}
		return LOCATION_ON;
	}

	/**
	 * Returns the bounds of the given item, or <code>null</code> if it is not a 
	 * valid type of item.
	 */
	private Rectangle getBounds(Item item) {
		if (item instanceof TreeItem) {
			return ((TreeItem) item).getBounds();
		}
		if (item instanceof TableItem) {
			return ((TableItem) item).getBounds(0);
		}
		return null;
	}

	/**
	 * Scrolls the widget if the given coordinates are within epsilon
	 * of the widget borders.  If scolling occurs, the viewer selection
	 * is set to be the newly revealed widget.  Returns true if scrolling
	 * actually, occurred, and false otherwise.
	 */
	protected Object scrollIfNeeded(int x, int y) {
		long time= System.currentTimeMillis();
		if (time - fLastScroll < 500)
			return null;
		
		fLastScroll= time;	
		Control control= fViewer.getControl();
		Point point= control.toControl(new Point(x, y));
		Rectangle bounds= control.getBounds();
		Item item= null;
		if (point.y < SCROLL_EPSILON) {
			item= fViewer.scrollUp(x, y);
		} else {
			if ((bounds.height - bounds.y - point.y) < SCROLL_EPSILON) {
				item= fViewer.scrollDown(x, y);
			}
		}
		
		if (item != null)
			return item.getData();
			
		return null;
	}
	
	/**
	 * Sets the drag under feedback corresponding to the value of <code>fLocation</code>
	 * and the <code>INSERTION_FEEDBACK</code> style bit.
	 */
	protected void computeFeedback(DropTargetEvent event) {
		int old= event.feedback;
		boolean insertionFeedback= (fStyle & INSERTION_FEEDBACK) != 0;
		
		if (!insertionFeedback && fLocation != LOCATION_NONE) {
			event.feedback= DND.FEEDBACK_SELECT;
		} else {
			if (fLocation == LOCATION_BEFORE) {
				event.feedback= DND.FEEDBACK_INSERT_BEFORE;
			} else if (fLocation == LOCATION_AFTER) {
				event.feedback= DND.FEEDBACK_INSERT_AFTER;
			}
		}
	}
	
	/**
	 * Sets the dop operation to </code>DROP_NODE<code>.
	 */
	protected void clearDropOperation(DropTargetEvent event) {
		event.detail= DND.DROP_NONE;
	}
	
	/**
	 * Returns the requested drop operation.
	 */
	protected int getRequestedOperation() {
		return fRequestedOperation;
	} 
}