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
package org.eclipse.jdt.internal.ui.text;

import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tracker;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.IInformationControlExtension3;

import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommand;
import org.eclipse.ui.commands.ICommandManager;
import org.eclipse.ui.commands.IKeySequenceBinding;
import org.eclipse.ui.keys.KeySequence;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IParent;

import org.eclipse.jdt.ui.actions.CustomFiltersActionGroup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.util.StringMatcher;

/**
 * Abstract class for Show hierarchy in light-weight controls.
 * 
 * @since 2.1
 */
public abstract class AbstractInformationControl implements IInformationControl, IInformationControlExtension, IInformationControlExtension2, IInformationControlExtension3, DisposeListener {


	
	/**
	 * The NamePatternFilter selects the elements which
	 * match the given string patterns.
	 *
	 * @since 2.0
	 */
	protected class NamePatternFilter extends ViewerFilter {
		
		public NamePatternFilter() {
		}
		
		/* (non-Javadoc)
		 * Method declared on ViewerFilter.
		 */
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			StringMatcher matcher= getMatcher();
			if (matcher == null || !(viewer instanceof TreeViewer))
				return true;
			TreeViewer treeViewer= (TreeViewer) viewer;
	
			String matchName= ((ILabelProvider) treeViewer.getLabelProvider()).getText(element);
			if (matchName != null && matcher.match(matchName))
				return true;
	
			return hasUnfilteredChild(treeViewer, element);
		}
	
		private boolean hasUnfilteredChild(TreeViewer viewer, Object element) {
			if (element instanceof IParent) {
				Object[] children=  ((ITreeContentProvider) viewer.getContentProvider()).getChildren(element);
				for (int i= 0; i < children.length; i++)
					if (select(viewer, element, children[i]))
						return true;
			}
			return false;
		}
	}


	private static class BorderFillLayout extends Layout {

		/** The border widths. */
		final int fBorderSize;

		/**
		 * Creates a fill layout with a border.
		 */
		public BorderFillLayout(int borderSize) {
			if (borderSize < 0)
				throw new IllegalArgumentException();
			fBorderSize= borderSize;
		}

		/**
		 * Returns the border size.
		 */
		public int getBorderSize() {
			return fBorderSize;
		}

		/*
		 * @see org.eclipse.swt.widgets.Layout#computeSize(org.eclipse.swt.widgets.Composite, int, int, boolean)
		 */
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {

			Control[] children= composite.getChildren();
			Point minSize= new Point(0, 0);

			if (children != null) {
				for (int i= 0; i < children.length; i++) {
					Point size= children[i].computeSize(wHint, hHint, flushCache);
					minSize.x= Math.max(minSize.x, size.x);
					minSize.y= Math.max(minSize.y, size.y);
				}
			}

			minSize.x += fBorderSize * 2 + RIGHT_MARGIN;
			minSize.y += fBorderSize * 2;

			return minSize;
		}
		/*
		 * @see org.eclipse.swt.widgets.Layout#layout(org.eclipse.swt.widgets.Composite, boolean)
		 */
		protected void layout(Composite composite, boolean flushCache) {

			Control[] children= composite.getChildren();
			Point minSize= new Point(composite.getClientArea().width, composite.getClientArea().height);

			if (children != null) {
				for (int i= 0; i < children.length; i++) {
					Control child= children[i];
					child.setSize(minSize.x - fBorderSize * 2, minSize.y - fBorderSize * 2);
					child.setLocation(fBorderSize, fBorderSize);
				}
			}
		}
	}
	
	
	/**
	 * The view menu's Remember Size and Location action.
	 * 
	 * @since 3.0
	 */
	private class RememberBoundsAction extends Action {
		
		RememberBoundsAction() {
			super(TextMessages.getString("AbstractInformationControl.viewMenu.remember.label"), IAction.AS_CHECK_BOX); //$NON-NLS-1$
			setChecked(!getDialogSettings().getBoolean(STORE_DISABLE_RESTORE_LOCATION));
		}
		
		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			IDialogSettings settings= getDialogSettings();
			
			boolean newValue= !isChecked();

			// store new value
			settings.put(STORE_DISABLE_RESTORE_LOCATION, newValue);
			settings.put(STORE_DISABLE_RESTORE_SIZE, newValue);
			
			fIsDecativateListenerActive= true;
		}
	}

	/**
	 * The view menu's Resize action.
	 * 
	 * @since 3.0
	 */
	private class ResizeAction extends Action {
		
		ResizeAction() {
			super(TextMessages.getString("AbstractInformationControl.viewMenu.resize.label"), IAction.AS_PUSH_BUTTON); //$NON-NLS-1$
		}
		
		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			Tracker tracker= new Tracker(fShell.getDisplay(), SWT.RESIZE);
			tracker.setStippled(true);
			Rectangle[] r= new Rectangle[] { getFilterText().getShell().getBounds() };
			tracker.setRectangles(r);
			if (tracker.open())
				fShell.setBounds(tracker.getRectangles()[0]);
		}
	}

	/**
	 * The view menu's Move action.
	 * 
	 * @since 3.0
	 */
	private class MoveAction extends Action {
		
		MoveAction() {
			super(TextMessages.getString("AbstractInformationControl.viewMenu.move.label"), IAction.AS_PUSH_BUTTON); //$NON-NLS-1$
		}
		
		/*
		 * @see org.eclipse.jface.action.Action#run()
		 */
		public void run() {
			Tracker tracker= new Tracker(fShell.getDisplay(), SWT.NONE);
			tracker.setStippled(true);
			Rectangle[] r= new Rectangle[] { getFilterText().getShell().getBounds() };
			tracker.setRectangles(r);
			if (tracker.open())
				fShell.setBounds(tracker.getRectangles()[0]);
		}
	}

	
	/** Border thickness in pixels. */
	private static final int BORDER= 1;
	/** Right margin in pixels. */
	private static final int RIGHT_MARGIN= 3;
	/**
	 * Dialog constants telling whether this control can be resized or move.
	 * @since 3.0
	 */
	private static final String STORE_DISABLE_RESTORE_SIZE= "DISABLE_RESTORE_SIZE"; //$NON-NLS-1$
	private static final String STORE_DISABLE_RESTORE_LOCATION= "DISABLE_RESTORE_LOCATION"; //$NON-NLS-1$
	
	/** The control's shell */
	private Shell fShell;
	/** The composite */
	Composite fComposite;
	/** The control's text widget */
	private Text fFilterText;
	/** The control's tree widget */
	private TreeViewer fTreeViewer;
	/** The current string matcher */
	private StringMatcher fStringMatcher;
	private ICommand fInvokingCommand;
	private Label fStatusField;
	private Font fStatusTextFont;
	private KeySequence[] fInvokingCommandKeySequences;
	/**
	 * Remembers the bounds for this information control.
	 * @since 3.0
	 */
	private Rectangle fBounds;
	private Rectangle fTrim;

	/**
	 * Fields for view menu support.
	 * @since 3.0
	 */
	private Button fViewMenuButton;
	private ToolBar fToolBar;
	private Composite fViewMenuButtonComposite;
	private MenuManager fViewMenuManager;
	private IKeyBindingService fKeyBindingService;
	private String[] fKeyBindingScopes;
	private IAction fShowViewMenuAction;
	
	private Listener fDeactivateListener;
	private boolean fIsDecativateListenerActive= false;
	private CustomFiltersActionGroup fCustomFiltersActionGroup;

	
	/**
	 * Creates a tree information control with the given shell as parent. The given
	 * styles are applied to the shell and the tree widget.
	 *
	 * @param parent the parent shell
	 * @param shellStyle the additional styles for the shell
	 * @param treeStyle the additional styles for the tree widget
	 * @param invokingCommandId the id of the command that invoked this control or <code>null</code>
	 * @param showStatusField <code>true</code> iff the control has a status field at the bottom
	 */
	public AbstractInformationControl(Shell parent, int shellStyle, int treeStyle, String invokingCommandId, boolean showStatusField) {
		if (invokingCommandId != null) {
			ICommandManager commandManager= PlatformUI.getWorkbench().getCommandSupport().getCommandManager();
			fInvokingCommand= commandManager.getCommand(invokingCommandId);
			if (fInvokingCommand != null && !fInvokingCommand.isDefined())
				fInvokingCommand= null;
			else
				// Pre-fetch key sequence - do not change because scope will change later.
				getInvokingCommandKeySequences();
		}
		
		fShell= new Shell(parent, shellStyle);
		Display display= fShell.getDisplay();
		fShell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));

		// Composite for filter text and tree
		
		fComposite= new Composite(fShell,SWT.RESIZE);
		GridLayout layout= new GridLayout(1, false);
		fComposite.setLayout(layout);
		fComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		

		fViewMenuButtonComposite= new Composite(fComposite, SWT.NONE);
		layout= new GridLayout(2, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		fViewMenuButtonComposite.setLayout(layout);
		fViewMenuButtonComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		if (hasHeader()) {
			createHeader(fViewMenuButtonComposite);
			fFilterText= createFilterText(fComposite);
		} else {
			fFilterText= createFilterText(fViewMenuButtonComposite);
		}
		
		createViewMenu(fViewMenuButtonComposite);
		
		createHorizontalSeparator(fComposite);
		
		fTreeViewer= createTreeViewer(fComposite, treeStyle);
		
		fCustomFiltersActionGroup= new CustomFiltersActionGroup(getId(), fTreeViewer);
		
		if (showStatusField)
			createStatusField(fComposite);
		
		final Tree tree= fTreeViewer.getTree();
		tree.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e)  {
				if (e.character == 0x1B) // ESC
					dispose();
			}
			public void keyReleased(KeyEvent e) {
				// do nothing
			}
		});

		tree.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				gotoSelectedElement();
			}
		});

		tree.addMouseMoveListener(new MouseMoveListener()	 {
			TreeItem fLastItem= null;
			public void mouseMove(MouseEvent e) {
				if (tree.equals(e.getSource())) {
					Object o= tree.getItem(new Point(e.x, e.y));
					if (o instanceof TreeItem) {
						if (!o.equals(fLastItem)) {
							fLastItem= (TreeItem)o;
							tree.setSelection(new TreeItem[] { fLastItem });
						} else if (e.y < tree.getItemHeight() / 4) {
							// Scroll up
							Point p= tree.toDisplay(e.x, e.y);
							Item item= fTreeViewer.scrollUp(p.x, p.y);
							if (item instanceof TreeItem) {
								fLastItem= (TreeItem)item;
								tree.setSelection(new TreeItem[] { fLastItem });
							}								
						} else if (e.y > tree.getBounds().height - tree.getItemHeight() / 4) {
							// Scroll down
							Point p= tree.toDisplay(e.x, e.y);
							Item item= fTreeViewer.scrollDown(p.x, p.y);
							if (item instanceof TreeItem) {
								fLastItem= (TreeItem)item;
								tree.setSelection(new TreeItem[] { fLastItem });
							}								
						}
					}
				}
			}
		});

		tree.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {

				if (tree.getSelectionCount() < 1)
					return;
				
				if (e.button != 1)
					return;

				if (tree.equals(e.getSource())) {
					Object o= tree.getItem(new Point(e.x, e.y));
					TreeItem selection= tree.getSelection()[0];
					if (selection.equals(o))
						gotoSelectedElement();
				}
			}
		});

		int border= ((shellStyle & SWT.NO_TRIM) == 0) ? 0 : BORDER;
		fShell.setLayout(new BorderFillLayout(border));
		
		if (hasHeader()) {
			fComposite.setTabList(new Control[] {fFilterText, fTreeViewer.getTree()});
		} else {
			fViewMenuButtonComposite.setTabList(new Control[] {fFilterText});
			fComposite.setTabList(new Control[] {fViewMenuButtonComposite, fTreeViewer.getTree()});
		}
		
		setInfoSystemColor();
		installFilter();
		
		addDisposeListener(this);
		fDeactivateListener= new Listener() {
			/*
			 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			 */
			public void handleEvent(Event event) {
				if (fIsDecativateListenerActive)
					dispose();
			}
		};
		fShell.addListener(SWT.Deactivate, fDeactivateListener);
		fIsDecativateListenerActive= true;
		fShell.addShellListener(new ShellAdapter() {
			/*
			 * @see org.eclipse.swt.events.ShellAdapter#shellActivated(org.eclipse.swt.events.ShellEvent)
			 */
			public void shellActivated(ShellEvent e) {
				if (e.widget == fShell && fShell.getShells().length == 0)
					fIsDecativateListenerActive= true;
			}
		});
		
		fShell.addControlListener(new ControlAdapter() {
			/**
			 * {@inheritDoc}
			 */
			public void controlMoved(ControlEvent e) {
				fBounds= fShell.getBounds();
				if (fTrim != null) {
					Point location= fComposite.getLocation();
					fBounds.x= fBounds.x - fTrim.x + location.x;		
					fBounds.y= fBounds.y - fTrim.y + location.y;
				}
				
			}
			
			/**
			 * {@inheritDoc}
			 */
			public void controlResized(ControlEvent e) {
				fBounds= fShell.getBounds();
				if (fTrim != null) {
					Point location= fComposite.getLocation();
					fBounds.x= fBounds.x - fTrim.x + location.x;		
					fBounds.y= fBounds.y - fTrim.y + location.y;
			}
			}
		});
	}
	
	/**
	 * Creates a tree information control with the given shell as parent. The given
	 * styles are applied to the shell and the tree widget.
	 *
	 * @param parent the parent shell
	 * @param shellStyle the additional styles for the shell
	 * @param treeStyle the additional styles for the tree widget
	 */
	public AbstractInformationControl(Shell parent, int shellStyle, int treeStyle) {
		this(parent, shellStyle, treeStyle, null, false);
	}

	protected abstract TreeViewer createTreeViewer(Composite parent, int style);

	/**
	 * Returns the name of the dialog settings section.
	 * 
	 * @return
	 */
	protected abstract String getId();
	
	protected TreeViewer getTreeViewer() {
		return fTreeViewer;
	}

	protected boolean hasHeader() {
		// default is to have no header
		return false;
	}
	
	/**
	 * Creates a header for this information control.
	 * <p>
	 * Note: The header is only be created if {@link #hasHeader()} returns <code>true</code>. 
	 * </p>
	 * 
	 * @param parent
	 */
	protected void createHeader(Composite parent) {
		// default is to have no header
	}
	
	protected Text getFilterText() {
		return fFilterText;
	}
	
	protected Text createFilterText(Composite parent) {
		fFilterText= new Text(parent, SWT.FLAT);

		GridData data= new GridData(GridData.FILL_HORIZONTAL);
		GC gc= new GC(parent);
		gc.setFont(parent.getFont());
		FontMetrics fontMetrics= gc.getFontMetrics();
		gc.dispose();

		data.heightHint= Dialog.convertHeightInCharsToPixels(fontMetrics, 1);
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.CENTER;
		fFilterText.setLayoutData(data);
		
		fFilterText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == 0x0D) // return
					gotoSelectedElement();
				if (e.keyCode == SWT.ARROW_DOWN)
					fTreeViewer.getTree().setFocus();
				if (e.keyCode == SWT.ARROW_UP)
					fTreeViewer.getTree().setFocus();
				if (e.character == 0x1B) // ESC
					dispose();
			}
			public void keyReleased(KeyEvent e) {
				// do nothing
			}
		});

		return fFilterText;
	}
	
	protected void createHorizontalSeparator(Composite parent) {
		Label separator= new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.LINE_DOT);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	
	private void createViewMenu(Composite toolbar) {
		fToolBar= new ToolBar(toolbar, SWT.FLAT);
		ToolItem viewMenuButton= new ToolItem(fToolBar, SWT.PUSH, 0);
		
		GridData data= new GridData();
		data.horizontalAlignment= GridData.END;
		data.verticalAlignment= GridData.BEGINNING;
		fToolBar.setLayoutData(data);

		Image hoverImage= JavaPluginImages.get(JavaPluginImages.IMG_VIEW_MENU);
		viewMenuButton.setImage(hoverImage);
		viewMenuButton.setToolTipText(TextMessages.getString("AbstractInformationControl.viewMenu.toolTipText")); //$NON-NLS-1$
		viewMenuButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showViewMenu();
			}
		});
		
		// Key binding service
		IWorkbenchPart part= JavaPlugin.getActivePage().getActivePart();
		IWorkbenchPartSite site= part.getSite();
		fKeyBindingService=  site.getKeyBindingService();

		// Remember current scope and then remove it.
		fKeyBindingScopes= fKeyBindingService.getScopes();
		fKeyBindingService.setScopes(new String[] {});
		
		// Register shell with key binding support
		IWorkbench workbench= PlatformUI.getWorkbench();
		workbench.getCommandSupport().registerForKeyBindings(fShell, false);
		
		// Register action with key binding service
		fShowViewMenuAction= new Action("showViewMenu") { //$NON-NLS-1$
			/*
			 * @see org.eclipse.jface.action.Action#run()
			 */
			public void run() {
				showViewMenu();
			}
		};
		fShowViewMenuAction.setActionDefinitionId("org.eclipse.ui.window.showViewMenu"); //$NON-NLS-1$
		fKeyBindingService.registerAction(fShowViewMenuAction);
	}

	private MenuManager getViewMenuManager() {
		if (fViewMenuManager == null) {
			fViewMenuManager= new MenuManager();
			fillViewMenu(fViewMenuManager);
		}
		return fViewMenuManager;
	}
	
	private void showViewMenu( ) {
		fIsDecativateListenerActive= false;
		
		Menu aMenu = getViewMenuManager().createContextMenu(fShell);
		
		Rectangle bounds = fToolBar.getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = fShell.toDisplay(topLeft);
		aMenu.setLocation(topLeft.x, topLeft.y);

		aMenu.setVisible(true);
	}
	
	private void createStatusField(Composite parent) {
		
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout(1, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		// Horizontal separator line
		Label separator= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.LINE_DOT);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Status field label
		fStatusField= new Label(parent, SWT.RIGHT);
		fStatusField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fStatusField.setText(getStatusFieldText());
		Font font= fStatusField.getFont();
		Display display= parent.getDisplay();
		FontData[] fontDatas= font.getFontData();
		for (int i= 0; i < fontDatas.length; i++)
			fontDatas[i].setHeight(fontDatas[i].getHeight() * 9 / 10);
		fStatusTextFont= new Font(display, fontDatas);
		fStatusField.setFont(fStatusTextFont);

		fStatusField.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
	}
	
	protected void updateStatusFieldText() {
		if (fStatusField != null)
			fStatusField.setText(getStatusFieldText());
	}
	
	/**
	 * Handles click in status field.
	 * <p>
	 * Default does nothing.
	 * </p> 
	 */
	protected void handleStatusFieldClicked() {
	}

	protected String getStatusFieldText() {
		return ""; //$NON-NLS-1$
	}
	
	private void setInfoSystemColor() {
		Display display= fShell.getDisplay();
		setForegroundColor(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		setBackgroundColor(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
	}
	
	private void installFilter() {
		fFilterText.setText(""); //$NON-NLS-1$

		fFilterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String text= ((Text) e.widget).getText();
				int length= text.length();
				if (length > 0 && text.charAt(length -1 ) != '*') {
					text= text + '*';
				}				
				setMatcherString(text);
			}
		});
	}
	
	/**
	 * The string matcher has been modified. The default implementation
	 * refreshes the view and selects the first matched element
	 */
	protected void stringMatcherUpdated() {
		// refresh viewer to re-filter
		fTreeViewer.getControl().setRedraw(false);
		fTreeViewer.refresh();
		fTreeViewer.expandAll();
		selectFirstMatch();
		fTreeViewer.getControl().setRedraw(true);
	}
	
	/**
	 * Sets the patterns to filter out for the receiver.
	 * <p>
	 * The following characters have special meaning:
	 *   ? => any character
	 *   * => any string
	 * </p>
	 */
	protected void setMatcherString(String pattern) {
		if (pattern.length() == 0) {
			fStringMatcher= null;
		} else {
			boolean ignoreCase= pattern.toLowerCase().equals(pattern);
			fStringMatcher= new StringMatcher(pattern, ignoreCase, false);
		}
		stringMatcherUpdated();
	}
	
	protected StringMatcher getMatcher() {
		return fStringMatcher;
	}
	
	/**
	 * Implementers can modify
	 */
	protected Object getSelectedElement() {
		if (fTreeViewer == null)
			return null;
		
		return ((IStructuredSelection) fTreeViewer.getSelection()).getFirstElement();
	}

	private void gotoSelectedElement() {
		Object selectedElement= getSelectedElement();
		if (selectedElement != null) {
			try {
				dispose();
				OpenActionUtil.open(selectedElement, true);
			} catch (CoreException ex) {
				JavaPlugin.log(ex);
			}
		}
	}

	/**
	 * Selects the first element in the tree which
	 * matches the current filter pattern.
	 */
	protected void selectFirstMatch() {
		Tree tree= fTreeViewer.getTree();
		Object element= findElement(tree.getItems());
		if (element != null)
			fTreeViewer.setSelection(new StructuredSelection(element), true);
		else
			fTreeViewer.setSelection(StructuredSelection.EMPTY);
	}

	private IJavaElement findElement(TreeItem[] items) {
		ILabelProvider labelProvider= (ILabelProvider)fTreeViewer.getLabelProvider();
		for (int i= 0; i < items.length; i++) {
			IJavaElement element= (IJavaElement)items[i].getData();
			if (fStringMatcher == null)
				return element;
			
			if (element != null) {
				String label= labelProvider.getText(element);
				if (fStringMatcher.match(label))
					return element;
			}

			element= findElement(items[i].getItems());
			if (element != null)
				return element;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setInformation(String information) {
		// this method is ignored, see IInformationControlExtension2
	}
	
	/**
	 * {@inheritDoc}
	 */
	public abstract void setInput(Object information);
	
	/**
	 * Fills the view menu.
	 * Clients can extend or override.
	 * 
	 * @param viewMenu the menu manager that manages the menu
	 * @since 3.0
	 */
	protected void fillViewMenu(IMenuManager viewMenu) {
		viewMenu.add(new GroupMarker("SystemMenuStart")); //$NON-NLS-1$
		viewMenu.add(new MoveAction());
		viewMenu.add(new ResizeAction());
		viewMenu.add(new RememberBoundsAction());
		viewMenu.add(new Separator("SystemMenuEnd")); //$NON-NLS-1$
		
		fCustomFiltersActionGroup.fillViewMenu(viewMenu);
	}
	
	protected void inputChanged(Object newInput, Object newSelection) {
		fFilterText.setText(""); //$NON-NLS-1$
		fTreeViewer.setInput(newInput);
		if (newSelection != null) {
			fTreeViewer.setSelection(new StructuredSelection(newSelection));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setVisible(boolean visible) {
		if (visible || fIsDecativateListenerActive)
			fShell.setVisible(visible);
	}

	/**
	 * {@inheritDoc}
	 */
	public final void dispose() {
		if (fShell != null && !fShell.isDisposed())
			fShell.dispose();
		else
			widgetDisposed(null);
	}
	
	/**
	 * {@inheritDoc}
	 * @param event can be null
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 */
	public void widgetDisposed(DisposeEvent event) {
		if (fStatusTextFont != null && !fStatusTextFont.isDisposed())
			fStatusTextFont.dispose();
		
		fShell= null;
		fTreeViewer= null;
		fComposite= null;
		fFilterText= null;
		fStatusTextFont= null;
		
		// Unregister Show View Menu action
		fKeyBindingService.registerAction(fShowViewMenuAction);
		
		// Restore editor's key binding scope
		if (fKeyBindingScopes != null) {
			fKeyBindingService.setScopes(fKeyBindingScopes);
			fKeyBindingScopes= null;
			fKeyBindingService= null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasContents() {
		return fTreeViewer != null && fTreeViewer.getInput() != null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setSizeConstraints(int maxWidth, int maxHeight) {
		if (maxWidth > -1 && maxHeight > -1) {
			GridData gd= new GridData(GridData.FILL_BOTH);
			if (maxWidth > -1)
				gd.widthHint= maxWidth; 
			if (maxHeight > -1)
				gd.heightHint= maxHeight;
			
			fShell.setLayoutData(gd);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Point computeSizeHint() {
		return fShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	}

	/**
	 * {@inheritDoc}
	 * @since 3.0
	 */
	public Rectangle getBounds() {
		return fBounds;
	}
	
	/*
	 * @see org.eclipse.jface.text.IInformationControlExtension3#restoresLocation()
	 */
	public boolean restoresLocation() {
		return !getDialogSettings().getBoolean(STORE_DISABLE_RESTORE_LOCATION);
	}
	
	/*
	 * @see org.eclipse.jface.text.IInformationControlExtension3#restoresSize()
	 */
	public boolean restoresSize() {
		return !getDialogSettings().getBoolean(STORE_DISABLE_RESTORE_SIZE);
	}

	/*
	 * @see org.eclipse.jface.text.IInformationControlExtension3#computeTrim()
	 */
	public Rectangle computeTrim() {
		if (fTrim != null)
			return fTrim;
		return new Rectangle(0, 0, 0, 0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setLocation(Point location) {
		fTrim= fShell.computeTrim(0, 0, 0, 0);
		Point compositeLocation= fComposite.getLocation();
		location.x += fTrim.x - compositeLocation.x;		
		location.y += fTrim.y - compositeLocation.y;		
		fShell.setLocation(location);		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void setSize(int width, int height) {
		fShell.setSize(width, height);
	}

	/**
	 * {@inheritDoc}
	 */
	public void addDisposeListener(DisposeListener listener) {
		fShell.addDisposeListener(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeDisposeListener(DisposeListener listener) {
		fShell.removeDisposeListener(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setForegroundColor(Color foreground) {
		fTreeViewer.getTree().setForeground(foreground);
		fFilterText.setForeground(foreground);
		fComposite.setForeground(foreground);
		fViewMenuButtonComposite.setForeground(foreground);
		if (fStatusField != null)
			fStatusField.getParent().setForeground(foreground);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setBackgroundColor(Color background) {
		fTreeViewer.getTree().setBackground(background);
		fFilterText.setBackground(background);
		fComposite.setBackground(background);
		fViewMenuButtonComposite.setBackground(background);
		if (fStatusField != null) {
			fStatusField.setBackground(background);
			fStatusField.getParent().setBackground(background);
		}
		if (fViewMenuButton != null)
			fViewMenuButton.setBackground(background);
		if (fToolBar != null)
			fToolBar.setBackground(background);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isFocusControl() {
		return fTreeViewer.getControl().isFocusControl() || fFilterText.isFocusControl();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setFocus() {
		fShell.forceFocus();
		fFilterText.setFocus();
	}

	/**
	 * {@inheritDoc}
	 */
	public void addFocusListener(FocusListener listener) {
		fShell.addFocusListener(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeFocusListener(FocusListener listener) {
		fShell.removeFocusListener(listener);
	}
	
	final protected ICommand getInvokingCommand() {
		return fInvokingCommand;
	}
	
	final protected KeySequence[] getInvokingCommandKeySequences() {
		if (fInvokingCommandKeySequences == null) {
			if (getInvokingCommand() != null) {
				List list= getInvokingCommand().getKeySequenceBindings();
				if (!list.isEmpty()) {
					fInvokingCommandKeySequences= new KeySequence[list.size()];
					for (int i= 0; i < fInvokingCommandKeySequences.length; i++) {
						fInvokingCommandKeySequences[i]= ((IKeySequenceBinding) list.get(i)).getKeySequence();
					}
					return fInvokingCommandKeySequences;
				}		
			}
		}
		return fInvokingCommandKeySequences;
	}
	
	protected IDialogSettings getDialogSettings() {
		String sectionName= getId();
		
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings().getSection(sectionName);
		if (settings == null)
			settings= JavaPlugin.getDefault().getDialogSettings().addNewSection(sectionName);
		
		return settings;
	}
}
