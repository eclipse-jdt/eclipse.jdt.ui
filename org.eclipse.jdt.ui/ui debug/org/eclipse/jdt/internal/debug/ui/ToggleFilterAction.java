/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.debug.ui;

import org.eclipse.debug.ui.IDebugViewAdapter;import org.eclipse.jface.action.Action;import org.eclipse.jface.action.IAction;import org.eclipse.jface.viewers.*;import org.eclipse.swt.custom.BusyIndicator;import org.eclipse.ui.IViewActionDelegate;import org.eclipse.ui.IViewPart;

/**
 * A generic Toggle filter action, meant to be subclassed to provide
 * a specific filter.
 */
public abstract class ToggleFilterAction extends Action implements IViewActionDelegate {

	/**
	 * The viewer that this action works for
	 */
	StructuredViewer fViewer;

	protected static final String fgShow= ".label.show";
	protected static final String fgHide= ".label.hide";

	/**
	 * @see IViewActionDelegate
	 */
	public void init(IViewPart view) {
		IDebugViewAdapter adapter= (IDebugViewAdapter) view.getAdapter(IDebugViewAdapter.class);
		if (adapter != null) {
			fViewer= adapter.getViewer();
		}
	}

	/**
	 * Returns the appropriate tool tip text depending on
	 * the state of the action.
	 */
	protected String getToolTipText(boolean on) {
		return on ? DebugUIUtils.getResourceString(getPrefix() + fgShow) : DebugUIUtils.getResourceString(getPrefix() + fgHide);
	}

	/**
	 * Adds or removes the viewer filter depending
	 * on the value of the parameter.
	 */
	protected void valueChanged(final boolean on) {
		BusyIndicator.showWhile(fViewer.getControl().getDisplay(), new Runnable() {
			public void run() {
				if (on) {
					ViewerFilter filter= getViewerFilter();
					ViewerFilter[] filters= fViewer.getFilters();
					boolean alreadyAdded= false;
					for (int i= 0; i < filters.length; i++) {
						ViewerFilter addedFilter= filters[i];
						if (addedFilter.equals(filter)) {
							alreadyAdded= true;
							break;
						}
					}
					if (!alreadyAdded) {
						fViewer.addFilter(filter);
					}
					
				} else {
					fViewer.removeFilter(getViewerFilter());
				}
				setToolTipText(getToolTipText(on));									
			}
		});

	}

	/**
	 * Returns the <code>ViewerFilter</code> that this action
	 * will add/remove from the viewer, or <code>null</code>
	 * if no filter is involved.
	 */
	protected abstract ViewerFilter getViewerFilter();

	/**
	 * @see IViewActionDelegate
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * @see IViewActionDelegate
	 */
	public void run(IAction action) {
		valueChanged(action.isChecked());
		String label= getToolTipText(action.isChecked());
		action.setToolTipText(label);
		action.setText(label);
	}

	/**
	 * Returns resource bundle prefix for "button_id". The ".label.hide" and ".label.show"
	 * suffixes will be applied to get tool tip text and labels.
	 */
	protected abstract String getPrefix();

	/**
	 * @see IAction
	 */
	public void run() {
	}

}
