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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
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
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.dialogs.Dialog;
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

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommand;
import org.eclipse.ui.commands.ICommandManager;
import org.eclipse.ui.commands.IKeySequenceBinding;
import org.eclipse.ui.keys.KeySequence;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IParent;

import org.eclipse.jdt.internal.ui.JavaPlugin;
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



	/** Border thickness in pixels. */
	private static final int BORDER= 1;
	/** Right margin in pixels. */
	private static final int RIGHT_MARGIN= 3;
	/** The control's shell */
	private Shell fShell;
	/** The composite */
	Composite fComposite;
	/** The control's text widget */
	private Text fFilterText;
	/** The control's tree widget */
	private TreeViewer fTreeViewer;
	/** The control's width constraint */
	//private int fMaxWidthInChars= -1;
	/** The control's height constraint */
	//private int fMaxHeightInChars= -1;
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
		}
		
		fShell= new Shell(parent, shellStyle);
		Display display= fShell.getDisplay();
		fShell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));

		// Composite for filter text and tree
		
		fComposite= new Composite(fShell,SWT.RESIZE);
		GridLayout layout= new GridLayout(1, false);
		fComposite.setLayout(layout);
		fComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		fFilterText= createFilterText(fComposite);
		fTreeViewer= createTreeViewer(fComposite, treeStyle);
		
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
		
		fComposite.setTabList(new Control[] {fFilterText, fTreeViewer.getTree()});
		
		setInfoSystemColor();
		installFilter();
		
		addDisposeListener(this);
		fShell.addListener(SWT.Deactivate, new Listener() {
			/**
			 * {@inheritDoc}
			 */
			public void handleEvent(Event event) {
				dispose();
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

	protected TreeViewer getTreeViewer() {
		return fTreeViewer;
	}

	protected Text getFilterText() {
		return fFilterText;
	}
	
	protected Text createFilterText(Composite parent) {
		fFilterText= new Text(parent, SWT.FLAT);

		GridData data= new GridData();
		GC gc= new GC(parent);
		gc.setFont(parent.getFont());
		FontMetrics fontMetrics= gc.getFontMetrics();
		gc.dispose();

		data.heightHint= Dialog.convertHeightInCharsToPixels(fontMetrics, 1);
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.BEGINNING;
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

		// Horizontal separator line
		Label separator= new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.LINE_DOT);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return fFilterText;
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

//		fStatusField= new Button(parent, SWT.CENTER | SWT.FLAT);
//		fStatusField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//		fStatusField.setText(getStatusFieldText());
//		Font font= fStatusField.getFont();
//		Display display= parent.getDisplay();
//		FontData[] fontDatas= font.getFontData();
//		for (int i= 0; i < fontDatas.length; i++)
//			fontDatas[i].setHeight(fontDatas[i].getHeight() * 9 / 10);
//		fStatusTextFont= new Font(display, fontDatas);
//		fStatusField.setFont(fStatusTextFont);
//		
//		getFilterText().addFocusListener(new FocusAdapter() {
//			/**
//			 * {@inheritDoc}
//			 */
//			public void focusGained(FocusEvent e) {
//				fFocusWidget= getFilterText();
//			}
//		});
//		fTreeViewer.getTree().addFocusListener(new FocusAdapter() {
//			/**
//			 * {@inheritDoc}
//			 */
//			public void focusGained(FocusEvent e) {
//				fFocusWidget= fTreeViewer.getTree();
//			}
//		});
//		
//		fStatusField.addSelectionListener(new SelectionAdapter() {
//			/**
//			 * {@inheritDoc}
//			 */
//			public void widgetSelected(SelectionEvent e) {
//				handleStatusFieldClicked();
//				if (fFocusWidget != null)
//					fFocusWidget.setFocus();
//				else
//					getFilterText().setFocus();
//			}
//		});
//
//		// Regarding the color see bug 41128
//		fStatusField.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
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
	
	/**
	 * {@inheritDoc}
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
		if (fStatusField != null) {
			fStatusField.setBackground(background);
			fStatusField.getParent().setBackground(background);
		}
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
		return null;
	}
}
