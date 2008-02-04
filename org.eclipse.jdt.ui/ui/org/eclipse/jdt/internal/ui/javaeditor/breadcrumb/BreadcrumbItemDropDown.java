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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.ui.JavaElementComparator;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.FilteredTable.Direction;
import org.eclipse.jdt.internal.ui.javaeditor.breadcrumb.FilteredTable.INavigateListener;
import org.eclipse.jdt.internal.ui.viewsupport.ColoredViewersManager;


/**
 * The part of the breadcrumb item with the drop down menu.
 * 
 * @since 3.4
 */
class BreadcrumbItemDropDown {

	private static final int DROP_DOWN_HIGHT= 300;
	private static final int DROP_DOWN_WIDTH= 500;

	private final Label fArrow;
	private final BreadcrumbItem fParent;
	
	private ITreeContentProvider fContentProvider;
	private ILabelProvider fLabelProvider;

	private boolean fMenuIsShown;
	private boolean fEnabled;

	public BreadcrumbItemDropDown(BreadcrumbItem parent, Composite composite) {
		fParent= parent;
		fMenuIsShown= false;
		fEnabled= true;

		fArrow= new Label(composite, SWT.NONE);
		GridData layoutData= new GridData(SWT.END, SWT.CENTER, false, false);
		fArrow.setLayoutData(layoutData);
		fArrow.setImage(JavaPluginImages.get(JavaPluginImages.IMG_ETOOL_ARROW_RIGHT));
		fArrow.setBackground(composite.getBackground());

		fArrow.addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(MouseEvent e) {
				fArrow.setImage(JavaPluginImages.get(JavaPluginImages.IMG_ETOOL_ARROW_DOWN));
			}

			public void mouseExit(MouseEvent e) {
				if (!fMenuIsShown) {
					fArrow.setImage(JavaPluginImages.get(JavaPluginImages.IMG_ETOOL_ARROW_RIGHT));
				}
			}

			public void mouseHover(MouseEvent e) {
			}
		});

		fArrow.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {
			}

			public void mouseDown(MouseEvent e) {
				showMenu(null, false);
			}

			public void mouseUp(MouseEvent e) {
			}
		});
	}
	
	public void setContentProvider(ITreeContentProvider contentProvider) {
		fContentProvider= contentProvider;
	}

	public void setLabelProvider(ILabelProvider labelProvider) {
		fLabelProvider= labelProvider;
	}

	/**
	 * Set whether the drop down menu is available.
	 * 
	 * @param enabled true if available
	 */
	public void setEnabled(boolean enabled) {
		fEnabled= enabled;
		if (enabled) {
			fArrow.setEnabled(true);
			fArrow.setVisible(true);
		} else {
			fArrow.setEnabled(false);
			fArrow.setVisible(false);
		}
	}
	
	/**
	 * Opens the drop down menu. Initialize the filter with
	 * the given text, or <code>null</code> if the default
	 * filter text should be used.
	 * 
	 * @param filterText the text to filter for or <code>null</code>
	 * @param selectItem true to select the item in the drop down, false to select the filter text 
	 */
	public void showMenu(String filterText, boolean selectItem) {
		if (!fEnabled)
			return;
		
		fMenuIsShown= true;
		
		final Shell shell= new Shell(fArrow.getShell(), SWT.RESIZE | SWT.TOOL | SWT.BORDER);
		GridLayout layout= new GridLayout(1, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		shell.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		shell.setLayout(layout);
		installCloser(shell);
		
		Composite composite= new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gridLayout= new GridLayout(1, false);
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		composite.setLayout(gridLayout);
		
		int parentIndex= fParent.getViewer().getIndexOfItem(fParent);
		final FilteredTable filteredTable= new FilteredTable(composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL, new PatternFilter(), parentIndex < fParent.getViewer().getItemCount() - 1, parentIndex > 0);
		if (filterText != null) {
			filteredTable.getFilterControl().setText(filterText);
			filteredTable.getFilterControl().setSelection(filterText.length());
		}
		filteredTable.setBackground(shell.getBackground());

		final TableViewer viewer= filteredTable.getViewer();
		ColoredViewersManager.install(viewer);
		final Table table= (Table) viewer.getControl();
		
		filteredTable.addNavigateListener(new INavigateListener() {
			public void navigate(Direction direction) {
				if (direction == FilteredTable.DIRECTION_UP) {
					int index= fParent.getViewer().getIndexOfItem(fParent);
					if (index - 1 >= 0) {
						shell.close();
						
						BreadcrumbItem parent= fParent.getViewer().getItem(index - 1);
						fParent.getViewer().selectItem(parent);
						parent.openDropDownMenu(null, true);
					}
				} else if (direction == FilteredTable.DIRECTION_DOWN) {
					int index= fParent.getViewer().getIndexOfItem(fParent);
					if (index != -1 && index + 1 < fParent.getViewer().getItemCount()) {
						shell.close();
						
						BreadcrumbItem child= fParent.getViewer().getItem(index + 1);
						fParent.getViewer().selectItem(child);
						child.openDropDownMenu(null, true);
					}
				}
			}
		});

		viewer.setContentProvider(new ITreeContentProvider() {  
			public void dispose() {
				//do not dispose this, still needed
			}

			public void inputChanged(Viewer viewer1, Object oldInput, Object newInput) {
				fContentProvider.inputChanged(viewer1, oldInput, newInput);
			}

			public Object[] getElements(Object inputElement) {
				return fContentProvider.getElements(inputElement);
			}

			public Object[] getChildren(Object parentElement) {
				if (parentElement != viewer.getInput())
					return new Object[0];

				return getElements(parentElement);
			}

			public Object getParent(Object element) {
				return viewer.getInput();
			}

			public boolean hasChildren(Object element) {
				return false;
			}
		});
		viewer.setLabelProvider(fLabelProvider);
		viewer.setComparator(new JavaElementComparator());
		viewer.setInput(fParent.getInput());

		setShellBounds(shell);

		viewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				ISelection selection= event.getSelection();
				if (!(selection instanceof IStructuredSelection))
					return;

				Object element= ((IStructuredSelection) selection).getFirstElement();
				if (element == null)
					return;
				
				shell.close();
				fParent.getViewer().fireMenuSelection(element);
			}
		});

		table.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {
				if (e.button != 1)
					return;

				Item item= table.getItem(new Point(e.x, e.y));
				if (item == null)
					return;

				Object data= item.getData();
				if (data == null)
					return;

				shell.close();
				fParent.getViewer().fireMenuSelection(data);
			}

			public void mouseDown(MouseEvent e) {
			}

			public void mouseUp(MouseEvent e) {
			}
		});

		shell.open();

		Object child= fParent.getData();
		if (child != null)
			viewer.setSelection(new StructuredSelection(child), true);
		
		if (selectItem) {
			viewer.getTable().setFocus();
		} else {
			filteredTable.getFilterControl().setFocus();
		}
	}

	/**
	 * The closer closes the given shell when the focus is lost.
	 * 
	 * @param shell the shell to install the closer to
	 */
	private void installCloser(final Shell shell) {
		final Listener focusListener= new Listener() {
			public void handleEvent(Event event) {
				Widget focusElement= event.widget;
				if (!(focusElement instanceof Control)) {
					shell.close();
					return;
				}
				Control control= (Control) focusElement;
				while (control != null) {
					if (control == shell)
						return;

					control= control.getParent();
				}

				shell.close();
			}
		};
		Display.getDefault().addFilter(SWT.FocusIn, focusListener);

		shell.addShellListener(new ShellListener() {
			public void shellActivated(ShellEvent e) {
			}

			public void shellClosed(ShellEvent e) {
				Display.getDefault().removeFilter(SWT.FocusIn, focusListener);
				fArrow.setImage(JavaPluginImages.get(JavaPluginImages.IMG_ETOOL_ARROW_RIGHT));
				fParent.getViewer().setFocus();
				fMenuIsShown= false;
			}

			public void shellDeactivated(ShellEvent e) {
			}

			public void shellDeiconified(ShellEvent e) {
			}

			public void shellIconified(ShellEvent e) {
			}
		});
	}

	/**
	 * Calculates a useful size for the given shell
	 * 
	 * @param shell the shell to calculate the size for.
	 */
	private void setShellBounds(Shell shell) {
		Rectangle rect= fArrow.getBounds();
		Point pt= new Point(rect.x + rect.width - 2, rect.y + rect.height + 5);
		pt= fArrow.getParent().toDisplay(pt);
		shell.setLocation(pt.x, pt.y);
		fArrow.setImage(JavaPluginImages.get(JavaPluginImages.IMG_ETOOL_ARROW_DOWN));

		shell.pack();
		Point size= shell.getSize();
		int height= Math.min(size.y, DROP_DOWN_HIGHT);
		int width= Math.max(Math.min(size.x, DROP_DOWN_WIDTH), 250);

		shell.setSize(width, height);
		shell.layout(true, true);
	}

	public boolean isMenuShown() {
		return fMenuIsShown;
	}

}
