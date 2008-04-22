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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
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
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.jdt.ui.JavaElementComparator;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.SWTUtil;


/**
 * The part of the breadcrumb item with the drop down menu.
 * 
 * @since 3.4
 */
class BreadcrumbItemDropDown {

	private static final int DROP_DOWN_HIGHT= 300;
	private static final int DROP_DOWN_WIDTH= 500;

	private final BreadcrumbItem fParent;
	private final Composite fParentComposite;

	private boolean fMenuIsShown;
	private boolean fEnabled;
	private ToolBar fToolBar;
	private TreeViewer fDropDownViewer;
	private Shell fShell;

	public BreadcrumbItemDropDown(BreadcrumbItem parent, Composite composite) {
		fParent= parent;
		fParentComposite= composite;
		fMenuIsShown= false;
		fEnabled= true;
		
		fToolBar= new ToolBar(composite, SWT.FLAT);
		fToolBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		SWTUtil.setAccessibilityText(fToolBar, BreadcrumbMessages.BreadcrumbItemDropDown_showDropDownMenu_action_toolTip);
		ToolBarManager manager= new ToolBarManager(fToolBar);

		Action showDropDownMenuAction= new Action(null, SWT.NONE) {
			/* (non-Javadoc)
			 * @see org.eclipse.jface.action.Action#run()
			 */
			public void run() {
				showMenu();
			}
		};

		showDropDownMenuAction.setImageDescriptor(JavaPluginImages.DESC_ETOOL_ARROW_RIGHT);
		showDropDownMenuAction.setToolTipText(BreadcrumbMessages.BreadcrumbItemDropDown_showDropDownMenu_action_toolTip);
		manager.add(showDropDownMenuAction);

		manager.update(true);
	}

	/**
	 * Set whether the drop down menu is available.
	 * 
	 * @param enabled true if available
	 */
	public void setEnabled(boolean enabled) {
		fEnabled= enabled;

		fToolBar.setVisible(enabled);
	}
	
	/**
	 * Is the drop down menu open at the moment?
	 * 
	 * @return true if the menu is open
	 */
	public boolean isMenuShown() {
		return fMenuIsShown;
	}
	
	/**
	 * Get the shell used for the drop down menu if it is shown.
	 * 
	 * @return the drop down shell or <code>null</code>
	 */
	public Shell getDropDownShell() {
		if (!isMenuShown())
			return null;

		return fShell;
	}

	/**
	 * @return the selection provider of the drop down if {@link #isMenuShown()}, <code>null</code> otherwise
	 */
	public ISelectionProvider getDropDownSelectionProvider() {
		if (!fMenuIsShown)
			return null;
		
		return fDropDownViewer;
	}

	/**
	 * Opens the drop down menu.
	 */
	public void showMenu() {
		if (!fEnabled || fMenuIsShown)
			return;
		
		fMenuIsShown= true;
		
		fShell= new Shell(fToolBar.getShell(), SWT.RESIZE | SWT.TOOL | SWT.BORDER);
		GridLayout layout= new GridLayout(1, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		fShell.setLayout(layout);
		
		Composite composite= new Composite(fShell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout gridLayout= new GridLayout(1, false);
		gridLayout.marginHeight= 0;
		gridLayout.marginWidth= 0;
		composite.setLayout(gridLayout);
		
		fDropDownViewer= new TreeViewer(composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		
		final Tree tree= (Tree) fDropDownViewer.getControl();
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Object input= fParent.getData();
		fDropDownViewer.setContentProvider(fParent.getViewer().createDropDownContentProvider(input));
		fDropDownViewer.setLabelProvider(fParent.getViewer().createDropDownLabelProvider(input));
		fDropDownViewer.setComparator(new JavaElementComparator());
		fDropDownViewer.setInput(input);

		setShellBounds(fShell);

		fDropDownViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				ISelection selection= event.getSelection();
				if (!(selection instanceof IStructuredSelection))
					return;

				Object element= ((IStructuredSelection) selection).getFirstElement();
				if (element == null)
					return;
				
				fParent.getViewer().fireMenuSelection(element);
				if (fShell.isDisposed())
					return;

				if (fDropDownViewer.getExpandedState(element)) {
					fDropDownViewer.collapseToLevel(element, 1);
				} else {
					tree.setRedraw(false);
					try {
						fDropDownViewer.expandToLevel(element, 1);
						resizeShell(fShell);
					} finally {
						tree.setRedraw(true);
					}
				}
			}
		});

		tree.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {
				if (e.button != 1)
					return;

				Item item= tree.getItem(new Point(e.x, e.y));
				if (item == null)
					return;

				Object data= item.getData();
				if (data == null)
					return;
				
				fParent.getViewer().fireMenuSelection(data);
				if (fShell.isDisposed())
					return;

				if (fDropDownViewer.getExpandedState(data)) {
					fDropDownViewer.collapseToLevel(data, 1);
				} else {
					tree.setRedraw(false);
					try {
						fDropDownViewer.expandToLevel(data, 1);
						resizeShell(fShell);
					} finally {
						tree.setRedraw(true);
					}
				}
			}

			public void mouseDown(MouseEvent e) {
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});
		
		tree.addMouseMoveListener(new MouseMoveListener() {
			TreeItem fLastItem= null;

			public void mouseMove(MouseEvent e) {
				if (tree.equals(e.getSource())) {
					Object o= tree.getItem(new Point(e.x, e.y));
					if (o instanceof TreeItem) {
						TreeItem currentItem= (TreeItem) o;
						if (!o.equals(fLastItem)) {
							fLastItem= (TreeItem) o;
							tree.setSelection(new TreeItem[] { fLastItem });
						} else if (e.y < tree.getItemHeight() / 4) {
							// Scroll up
							if (currentItem.getParentItem() == null) {
								int index= tree.indexOf((TreeItem) o);
								if (index < 1)
									return;

								fLastItem= tree.getItem(index - 1);
								tree.setSelection(new TreeItem[] { fLastItem });
							} else {
								Point p= tree.toDisplay(e.x, e.y);
								Item item= fDropDownViewer.scrollUp(p.x, p.y);
								if (item instanceof TreeItem) {
									fLastItem= (TreeItem) item;
									tree.setSelection(new TreeItem[] { fLastItem });
								}
							}
						} else if (e.y > tree.getBounds().height - tree.getItemHeight() / 4) {
							// Scroll down
							if (currentItem.getParentItem() == null) {
								int index= tree.indexOf((TreeItem) o);
								if (index >= tree.getItemCount() - 1)
									return;

								fLastItem= tree.getItem(index + 1);
								tree.setSelection(new TreeItem[] { fLastItem });
							} else {
								Point p= tree.toDisplay(e.x, e.y);
								Item item= fDropDownViewer.scrollDown(p.x, p.y);
								if (item instanceof TreeItem) {
									fLastItem= (TreeItem) item;
									tree.setSelection(new TreeItem[] { fLastItem });
								}
							}
						}
					}
				}
			}
		});
		
		tree.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_UP) {
					TreeItem[] selection= tree.getSelection();
					if (selection.length != 1)
						return;

					int selectionIndex= tree.indexOf(selection[0]);
					if (selectionIndex != 0)
						return;
					
					fShell.close();
				}
			}

			public void keyReleased(KeyEvent e) {
			}
		});
		
		fDropDownViewer.addTreeListener(new ITreeViewerListener() {
			public void treeCollapsed(TreeExpansionEvent event) {
			}

			public void treeExpanded(TreeExpansionEvent event) {
				tree.setRedraw(false);
				fShell.getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (fShell.isDisposed())
							return;
						
						try {
							resizeShell(fShell);
						} finally {
							tree.setRedraw(true);
						}
					}
				});
			}
		});

		//process any pending focus events: 
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=217800
		while (tree.getDisplay().readAndDispatch()) {
		}
		
		int index= fParent.getViewer().getIndexOfItem(fParent);
		if (index < fParent.getViewer().getItemCount() - 1) {
			BreadcrumbItem childItem= fParent.getViewer().getItem(index + 1);
			Object child= childItem.getData();
			
			fDropDownViewer.setSelection(new StructuredSelection(child), true);

			TreeItem[] selection= tree.getSelection();
			if (selection.length > 0) {
				tree.setTopItem(selection[0]);
			}
		}

		fShell.open();
		installCloser(fShell);
		
		tree.setFocus();
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

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				Display.getDefault().removeFilter(SWT.FocusIn, focusListener);
			}
		});
		shell.addShellListener(new ShellListener() {
			public void shellActivated(ShellEvent e) {
			}

			public void shellClosed(ShellEvent e) {
				if (!fMenuIsShown)
					return;
				
				fMenuIsShown= false;
				fDropDownViewer= null;
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
	 * Calculates a useful size for the given shell.
	 * 
	 * @param shell the shell to calculate the size for.
	 */
	private void setShellBounds(Shell shell) {
		Rectangle rect= fParentComposite.getBounds();
		Rectangle toolbarBounds= fToolBar.getBounds();
		Point pt= new Point(toolbarBounds.x + toolbarBounds.width - 2, rect.y + rect.height);
		pt= fParentComposite.toDisplay(pt);

		shell.pack();
		Point size= shell.getSize();
		int height= Math.min(size.y, DROP_DOWN_HIGHT);
		int width= Math.max(Math.min(size.x, DROP_DOWN_WIDTH), 250);

		Rectangle monitor= getMonitor(shell.getDisplay(), pt).getClientArea();
		int overlap= (pt.x + width) - (monitor.x + monitor.width);
		if (overlap > 0)
			pt.x-= overlap;
		
		shell.setLocation(pt);
		shell.setSize(width, height);
	}
	
	/**
	 * Returns the monitor which contains the given point.
	 * 
	 * @param display the current display
	 * @param point a point in the result
	 * @return monitor containing <code>point</code>
	 */
	private Monitor getMonitor(Display display, Point point) {
		Monitor[] monitors= display.getMonitors();

		for (int i= 0; i < monitors.length; i++) {
			Monitor current= monitors[i];

			Rectangle clientArea= current.getClientArea();

			if (clientArea.contains(point))
				return current;
		}

		return monitors[0];
	}

	/**
	 * Set the size of the given shell such that more content
	 * can be shown. The shell size does not exceed {@link #DROP_DOWN_HIGHT}
	 * and {@link #DROP_DOWN_WIDTH}.
	 * 
	 * @param shell the shell to resize
	 */
	private void resizeShell(final Shell shell) {
		Point size= shell.getSize();
		int currentWidth= size.x;
		int currentHeight= size.y;

		if (currentHeight >= DROP_DOWN_HIGHT && currentWidth >= DROP_DOWN_WIDTH)
			return;

		Point preferedSize= shell.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

		int newWidth;
		if (currentWidth >= DROP_DOWN_WIDTH) {
			newWidth= currentWidth;
		} else {
			newWidth= Math.min(Math.max(preferedSize.x, currentWidth), DROP_DOWN_WIDTH);
		}
		int newHeight;
		if (currentHeight >= DROP_DOWN_HIGHT) {
			newHeight= currentHeight;
		} else {
			newHeight= Math.min(Math.max(preferedSize.y, currentHeight), DROP_DOWN_HIGHT);
		}

		if (newHeight != currentHeight || newWidth != currentWidth) {
			shell.setSize(newWidth, newHeight);
		}
	}

}
