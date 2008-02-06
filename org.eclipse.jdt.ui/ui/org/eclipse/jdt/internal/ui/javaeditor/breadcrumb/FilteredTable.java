/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.breadcrumb;

import java.util.HashSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.WorkbenchJob;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * A simple control that provides a text widget and a table viewer. The contents
 * of the text widget are used to drive a PatternFilter that is on the viewer.
 * 
 * @see PatternFilter
 * @see org.eclipse.ui.dialogs.FilteredTree
 * 
 * @since 3.4
 */
public class FilteredTable extends Composite {

	public static final class Direction {
		private Direction() {
		}
	}

	/**
	 * Walk the hierarchy up, from child to parent
	 */
	public static final Direction DIRECTION_UP= new Direction();
	
	/**
	 * Walk the hierarchy down, from parent to child 
	 */
	public static final Direction DIRECTION_DOWN= new Direction();

	public interface INavigateListener {
		void navigate(Direction direction);
	}

	/**
	 * The filter text widget to be used. This value may be
	 * <code>null</code> if there is no filter widget, or if the controls have
	 * not yet been created.
	 */
	private Text fFilterText;

	/**
	 * The viewer for the filtered table. This value should never be
	 * <code>null</code> after the widget creation methods are complete.
	 */
	private TableViewer fTableViewer;

	/**
	 * The Composite on which the filter controls are created. This is used to
	 * set the background color of the filter controls to match the surrounding
	 * controls.
	 */
	private Composite fFilterComposite;

	/**
	 * The pattern filter for the table. This value must not be <code>null</code>.
	 */
	private PatternFilter fPatternFilter;

	/**
	 * The text to initially show in the filter text control.
	 */
	private String fInitialText= ""; //$NON-NLS-1$

	/**
	 * The job used to refresh the table.
	 */
	private Job fRefreshJob;

	/**
	 * Whether or not to show the filter controls (text and clear button). The
	 * default is to show these controls. This can be overridden by providing a
	 * setting in the product configuration file. The setting to add to not show
	 * these controls is:
	 * 
	 * org.eclipse.ui/SHOW_FILTERED_TEXTS=false
	 */
	private boolean fShowFilterControls;

	private final ListenerList fNavigateListeners;

	private ToolBarManager fLeftToolBar;

	private ToolBarManager fRightToolBar;

	private final boolean fHasChild;
	private final boolean fHasParent;

	private Label fLeftToolBarSpacer;

	private Label fRightToolBarSpacer;

	/**
	 * Create a new instance of the receiver.
	 * 
	 * @param parent
	 *            the parent <code>Composite</code>
	 * @param tableStyle
	 *            the style bits for the <code>Table</code>
	 * @param filter
	 *            the filter to be used
	 * @param hasChild 
	 * 			  does this pop up have a child, if yes it is possible to navigate down
	 * @param hasParent
	 * 			  does this pop up have a parent, if yes it is possible to navigate up 
	 */
	public FilteredTable(Composite parent, int tableStyle, PatternFilter filter, boolean hasChild, boolean hasParent) {
		super(parent, SWT.NONE);

		fPatternFilter= filter;
		fHasChild= hasChild;
		fHasParent= hasParent;
		fShowFilterControls= PlatformUI.getPreferenceStore().getBoolean(IWorkbenchPreferenceConstants.SHOW_FILTERED_TEXTS);

		fNavigateListeners= new ListenerList();

		createControl(parent, tableStyle);
		createRefreshJob();

		setInitialText(BreadcrumbMessages.FilteredTable_initial_filter_text);
		setFont(parent.getFont());
	}

	/**
	 * Add a listener which will be informed about navigation
	 * 
	 * @param listener the listener to notify
	 */
	public void addNavigateListener(INavigateListener listener) {
		fNavigateListeners.add(listener);
	}

	/**
	 * Create the controls
	 * 
	 * @param parent
	 * @param tableStyle
	 */
	private void createControl(Composite parent, int tableStyle) {
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.verticalSpacing= 0;
		setLayout(layout);
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		if (fShowFilterControls) {
			fFilterComposite= new Composite(this, SWT.NONE);
			GridLayout filterLayout= new GridLayout(3, false);
			filterLayout.marginHeight= 0;
			filterLayout.marginWidth= 0;
			filterLayout.horizontalSpacing= 0;
			fFilterComposite.setLayout(filterLayout);
			fFilterComposite.setFont(parent.getFont());

			createFilterControls(fFilterComposite);
			fFilterComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		}

		Composite tableComposite= new Composite(this, SWT.NONE);
		GridLayout tableCompositeLayout= new GridLayout();
		tableCompositeLayout.marginHeight= 0;
		tableCompositeLayout.marginWidth= 0;
		tableComposite.setLayout(tableCompositeLayout);
		GridData data= new GridData(SWT.FILL, SWT.FILL, true, true);
		tableComposite.setLayoutData(data);
		createTableControl(tableComposite, tableStyle);
		
		parent.addFocusListener(new FocusListener() {

			public void focusGained(FocusEvent e) {
				fFilterText.setFocus();
			}

			public void focusLost(FocusEvent e) {
			}

		});
	}

	/**
	 * Create the filter controls. By default, a text and corresponding tool bar
	 * button that clears the contents of the text is created.
	 * 
	 * @param parent
	 *            parent <code>Composite</code> of the filter controls
	 * @return the <code>Composite</code> that contains the filter controls
	 */
	private Composite createFilterControls(Composite parent) {
		createLeftToolBar(parent);
		createFilterText(parent);
		createRightToolBar(parent);
		
		return parent;
	}

	private void createLeftToolBar(Composite parent) {
		if (fHasParent) {
			fLeftToolBar= new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
			fLeftToolBar.createControl(parent);
			fLeftToolBar.getControl().setBackground(parent.getBackground());
			
			IAction goLeft= new Action("", IAction.AS_PUSH_BUTTON) {//$NON-NLS-1$
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.jface.action.Action#run()
				 */
				public void run() {
					Object[] listeners= fNavigateListeners.getListeners();
					for (int i= 0; i < listeners.length; i++) {
						((INavigateListener) listeners[i]).navigate(DIRECTION_UP);
					}
				}
			};

			goLeft.setToolTipText(BreadcrumbMessages.FilteredTable_go_left_action_tooltip);
			goLeft.setImageDescriptor(JavaPluginImages.DESC_ETOOL_BREADCRUMB_GO_LEFT);

			fLeftToolBar.add(goLeft);
			fLeftToolBar.update(true);
		} else {
			fLeftToolBarSpacer= new Label(parent, SWT.NONE);
			GridData data= new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
			data.widthHint= 19;
			fLeftToolBarSpacer.setLayoutData(data);
		}
	}
	
	private void createRightToolBar(Composite parent) {
		if (fHasChild) {
			fRightToolBar= new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL);
			fRightToolBar.createControl(parent);
			fRightToolBar.getControl().setBackground(parent.getBackground());
			
			IAction goRight= new Action("", IAction.AS_PUSH_BUTTON) {//$NON-NLS-1$
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.jface.action.Action#run()
				 */
				public void run() {
					Object[] listeners= fNavigateListeners.getListeners();
					for (int i= 0; i < listeners.length; i++) {
						((INavigateListener) listeners[i]).navigate(DIRECTION_DOWN);
					}
				}
			};

			goRight.setToolTipText(BreadcrumbMessages.FilteredTable_go_right_action_tooltip);
			goRight.setImageDescriptor(JavaPluginImages.DESC_ETOOL_BREADCRUMB_GO_RIGHT);

			fRightToolBar.add(goRight);
			fRightToolBar.update(true);
		} else {
			fRightToolBarSpacer= new Label(parent, SWT.NONE);
			GridData data= new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
			data.widthHint= 19;
			fRightToolBarSpacer.setLayoutData(data);
		}
	}

	/**
	 * Creates and set up the table and table viewer. 
	 * 
	 * @param parent
	 *            parent <code>Composite</code>
	 * @param style
	 *            SWT style bits used to create the table
	 * @return the table
	 */
	private Control createTableControl(Composite parent, int style) {
		fTableViewer= new TableViewer(parent, style);
		GridData data= new GridData(SWT.FILL, SWT.FILL, true, true);
		fTableViewer.getControl().setLayoutData(data);
		fTableViewer.getControl().addDisposeListener(new DisposeListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
			 */
			public void widgetDisposed(DisposeEvent e) {
				fRefreshJob.cancel();
			}
		});
		fTableViewer.getControl().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_UP) {
					TableItem[] selection= fTableViewer.getTable().getSelection();
					if (selection.length == 1 && selection[0] == fTableViewer.getTable().getItem(0)) {
						fFilterText.setFocus();
						return;
					}
				} else if (e.keyCode == SWT.ARROW_RIGHT || e.keyCode == SWT.ARROW_LEFT) {
					Direction dir= e.keyCode == SWT.ARROW_LEFT ? DIRECTION_UP : DIRECTION_DOWN;
					
					Object[] listeners= fNavigateListeners.getListeners();
					for (int i= 0; i < listeners.length; i++) {
						((INavigateListener) listeners[i]).navigate(dir);
					}
				}

				e.doit= true;
				return;
			}
		});

		fTableViewer.addFilter(fPatternFilter);

		return fTableViewer.getControl();
	}

	/**
	 * Return the first item in the table that matches the filter pattern.
	 * 
	 * @param tableItems
	 * @return the first matching TableItem
	 */
	private TableItem getFirstMatchingItem(TableItem[] tableItems) {
		for (int i= 0; i < tableItems.length; i++) {
			if (fPatternFilter.select(fTableViewer, null, tableItems[i].getData())) {
				return tableItems[i];
			}
		}
		return null;
	}

	/**
	 * Create the refresh job for the receiver.
	 * 
	 */
	private void createRefreshJob() {
		fRefreshJob= new WorkbenchJob("Refresh Filter") {//$NON-NLS-1$
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
			 */
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (fTableViewer.getTable().isDisposed())
					return Status.CANCEL_STATUS;

				String text= getFilterString();
				if (text == null) {
					return Status.OK_STATUS;
				}

				boolean initial= fInitialText != null && fInitialText.equals(text);
				if (initial) {
					fPatternFilter.setPattern(null);
				} else {
					fPatternFilter.setPattern(text);
				}

				try {
					fTableViewer.getTable().setRedraw(false);

					HashSet existing= new HashSet();
					TableItem[] items= fTableViewer.getTable().getItems();
					for (int i= 0; i < items.length; i++) {
						while (getDisplay().readAndDispatch()) {
							//allow to cancel this job if user types in filter field
							//see textChanged()
						}

						if (monitor.isCanceled() || fTableViewer.getTable().isDisposed())
							return Status.CANCEL_STATUS;

						if (!fPatternFilter.select(fTableViewer, null, items[i].getData())) {
							fTableViewer.remove(items[i].getData());
						} else {
							existing.add(items[i].getData());
						}
					}

					Object[] elements= ((IStructuredContentProvider) fTableViewer.getContentProvider()).getElements(fTableViewer.getInput());
					for (int i= 0; i < elements.length; i++) {
						while (getDisplay().readAndDispatch()) {
							//allow to cancel this job if user types in filter field
							//see textChanged()
						}

						if (monitor.isCanceled() || fTableViewer.getTable().isDisposed())
							return Status.CANCEL_STATUS;

						if (fPatternFilter.select(fTableViewer, null, elements[i]) && !existing.contains(elements[i])) {
							fTableViewer.add(elements[i]);
						}
					}
				} finally {
					if (!fTableViewer.getTable().isDisposed())
						fTableViewer.getTable().setRedraw(true);
				}

				return Status.OK_STATUS;
			}
		};
		fRefreshJob.setSystem(true);
	}

	/**
	 * Creates the filter text and adds listeners. This method calls
	 * 
	 * @param parent
	 *            <code>Composite</code> of the filter text
	 */
	private void createFilterText(Composite parent) {
		fFilterText= new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.CANCEL);

		fFilterText.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.accessibility.AccessibleListener#getName(org.eclipse.swt.accessibility.AccessibleEvent)
			 */
			public void getName(AccessibleEvent e) {
				String filterTextString= fFilterText.getText();
				if (filterTextString.length() == 0 || filterTextString.equals(fInitialText)) {
					e.result= fInitialText;
				} else {
					e.result= Messages.format(BreadcrumbMessages.FilteredTable_accessible_listener_text, new String[] { filterTextString, String.valueOf(getViewer().getTable().getItemCount()) });
				}
			}
		});

		fFilterText.addFocusListener(new FocusAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
			 */
			public void focusGained(FocusEvent e) {
				/*
				 * Running in an asyncExec because the selectAll() does not
				 * appear to work when using mouse to give focus to text.
				 */
				Display display= fFilterText.getDisplay();
				display.asyncExec(new Runnable() {
					public void run() {
						if (!fFilterText.isDisposed()) {
							if (getInitialText().equals(fFilterText.getText().trim())) {
								fFilterText.selectAll();
							}
						}
					}
				});
			}
		});

		fFilterText.addKeyListener(new KeyAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.KeyAdapter#keyReleased(org.eclipse.swt.events.KeyEvent)
			 */
			public void keyPressed(KeyEvent e) {
				// on a CR we want to transfer focus to the list
				boolean hasItems= getViewer().getTable().getItemCount() > 0;
				if (hasItems && e.keyCode == SWT.ARROW_DOWN) {
					fTableViewer.getTable().setFocus();

					if (getViewer().getTable().getSelectionCount() == 0) {
						TableItem item= getViewer().getTable().getItem(0);
						getViewer().getTable().setSelection(item);
						ISelection sel= getViewer().getSelection();
						getViewer().setSelection(sel, true);
					}
				} else if (e.keyCode == SWT.ARROW_UP) {
					e.doit= false;
				} else if (e.character == SWT.CR) {
					return;
				}
			}
		});

		// enter key set focus to table
		fFilterText.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN) {
					e.doit= false;
					if (getViewer().getTable().getItemCount() == 0) {
						Display.getCurrent().beep();
					} else {
						// if the initial filter text hasn't changed, do not try
						// to match
						boolean hasFocus= getViewer().getTable().setFocus();
						boolean textChanged= !getInitialText().equals(fFilterText.getText().trim());
						if (hasFocus && textChanged && fFilterText.getText().trim().length() > 0) {
							TableItem item= getFirstMatchingItem(getViewer().getTable().getItems());
							if (item != null) {
								getViewer().getTable().setSelection(item);
								ISelection sel= getViewer().getSelection();
								getViewer().setSelection(sel, true);
							}
						}
					}
				}
			}
		});

		fFilterText.addModifyListener(new ModifyListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
			 */
			public void modifyText(ModifyEvent e) {
				textChanged();
			}
		});

		// if we're using a field with built in cancel we need to listen for
		// default selection changes (which tell us the cancel button has been
		// pressed)
		if ((fFilterText.getStyle() & SWT.CANCEL) != 0) {
			fFilterText.addSelectionListener(new SelectionAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				public void widgetDefaultSelected(SelectionEvent e) {
					if (e.detail == SWT.CANCEL)
						clearText();
				}
			});
		}

		GridData gridData= new GridData(SWT.FILL, SWT.CENTER, true, false);

		fFilterText.setLayoutData(gridData);
		fFilterText.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
	}

	/**
	 * Update the receiver after the text has changed.
	 */
	private void textChanged() {
		if (fFilterText.getText().equals(fInitialText)) {
			fFilterText.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
		} else {
			fFilterText.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
		}
		
		// cancel currently running job first, to prevent unnecessary redraw
		fRefreshJob.cancel();
		fRefreshJob.schedule(200);
	}

	/**
	 * Set the background for the widgets that support the filter text area.
	 * 
	 * @param background
	 *            background <code>Color</code> to set
	 */
	public void setBackground(Color background) {
		super.setBackground(background);
		if (fFilterComposite != null) {
			fFilterComposite.setBackground(background);
		}
		if (fRightToolBar != null && fRightToolBar.getControl() != null) {
			fRightToolBar.getControl().setBackground(background);
		}
		if (fLeftToolBar != null && fLeftToolBar.getControl() != null) {
			fLeftToolBar.getControl().setBackground(background);
		}
		if (fLeftToolBarSpacer != null) {
			fLeftToolBarSpacer.setBackground(background);
		}
		if (fRightToolBarSpacer != null) {
			fRightToolBarSpacer.setBackground(background);
		}
	}

	/**
	 * Clears the text in the filter text widget. Also removes the optional
	 * additional filter that is provided via addFilter(ViewerFilter).
	 */
	private void clearText() {
		setFilterText(""); //$NON-NLS-1$
		textChanged();
	}

	/**
	 * Set the text in the filter control.
	 * 
	 * @param string
	 */
	private void setFilterText(String string) {
		if (fFilterText != null) {
			fFilterText.setText(string);
			selectAll();
		}
	}

	/**
	 * Returns the pattern filter used by this table.
	 * 
	 * @return The pattern filter; never <code>null</code>.
	 */
	public final PatternFilter getPatternFilter() {
		return fPatternFilter;
	}

	/**
	 * Get the table viewer of the receiver.
	 * 
	 * @return the table viewer
	 */
	public TableViewer getViewer() {
		return fTableViewer;
	}

	/**
	 * Get the filter text for the receiver, if it was created. Otherwise return
	 * <code>null</code>.
	 * 
	 * @return the filter Text, or null if it was not created
	 */
	public Text getFilterControl() {
		return fFilterText;
	}

	/**
	 * Convenience method to return the text of the filter control. If the text
	 * widget is not created, then null is returned.
	 * 
	 * @return String in the text, or null if the text does not exist
	 */
	private String getFilterString() {
		return fFilterText != null ? fFilterText.getText() : null;
	}

	/**
	 * Set the text that will be shown until the first focus. A default value is
	 * provided, so this method only need be called if overriding the default
	 * initial text is desired.
	 * 
	 * @param text
	 *            initial text to appear in text field
	 */
	public void setInitialText(String text) {
		fInitialText= text;
		setFilterText(fInitialText);
		textChanged();
	}

	/**
	 * Select all text in the filter text field.
	 */
	private void selectAll() {
		if (fFilterText != null) {
			fFilterText.selectAll();
		}
	}

	/**
	 * Get the initial text for the receiver.
	 * 
	 * @return String
	 */
	private String getInitialText() {
		return fInitialText;
	}

}
