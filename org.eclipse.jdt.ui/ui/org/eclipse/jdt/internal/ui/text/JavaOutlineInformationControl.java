/*
 * Copyright (c) 2000, 2002 IBM Corp. and others..
 * All rights reserved. ? This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
?*
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package org.eclipse.jdt.internal.ui.text;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.OpenActionUtil;
import org.eclipse.jdt.internal.ui.util.StringMatcher;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;

/**
 * @author dmegert
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class JavaOutlineInformationControl implements IInformationControl, IInformationControlExtension, IInformationControlExtension2 {


	/**
	 * The NamePatternFilter selects the elements which
	 * match the given string patterns.
	 * <p>
	 * The following characters have special meaning:
	 *   ? => any character
	 *   * => any string
	 * </p>
	 *
	 * @since 2.0
	 */
	private static class NamePatternFilter extends ViewerFilter {
		private String fPattern;
		private StringMatcher fMatcher;
		private ILabelProvider fLabelProvider;
		private Viewer fViewer;

		private StringMatcher getMatcher() {
			return fMatcher;
		}


		/* (non-Javadoc)
		 * Method declared on ViewerFilter.
		 */
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (fMatcher == null)
				return true;

			ILabelProvider labelProvider= getLabelProvider(viewer);

			String matchName= null;
			if (labelProvider != null)
				matchName= ((ILabelProvider)labelProvider).getText(element);
			else if (element instanceof IJavaElement)
				matchName= ((IJavaElement) element).getElementName();

			if (matchName != null && fMatcher.match(matchName))
				return true;

			return hasUnfilteredChild(viewer, element);
		}

		private ILabelProvider getLabelProvider(Viewer viewer) {
			if (fViewer == viewer)
				return fLabelProvider;

			fLabelProvider= null;
			IBaseLabelProvider baseLabelProvider= null;
			if (viewer instanceof StructuredViewer)
				baseLabelProvider= ((StructuredViewer)viewer).getLabelProvider();

			if (baseLabelProvider instanceof ILabelProvider)
				fLabelProvider= (ILabelProvider)baseLabelProvider;

			return fLabelProvider;
		}

		private boolean hasUnfilteredChild(Viewer viewer, Object element) {
			IJavaElement[] children;
			if (element instanceof IParent) {
				try {
					children= ((IParent)element).getChildren();
				} catch (JavaModelException ex) {
					return false;
				}
				for (int i= 0; i < children.length; i++)
					if (select(viewer, element, children[i]))
						return true;
			}
			return false;
		}

		/**
		 * Sets the patterns to filter out for the receiver.
		 * <p>
		 * The following characters have special meaning:
		 *   ? => any character
		 *   * => any string
		 * </p>
		 */
		public void setPattern(String pattern) {
			fPattern= pattern;
			if (fPattern == null) {
				fMatcher= null;
				return;
			}
			boolean ignoreCase= pattern.toLowerCase().equals(pattern);
			fMatcher= new StringMatcher(pattern, ignoreCase, false);
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
	/** The control width constraint */
	private int fMaxWidth= -1;
	/** The control height constraint */
	private int fMaxHeight= -1;

	private StringMatcher fStringMatcher;


	/**
	 * Creates a tree information control with the given shell as parent. The given
	 * style is applied to the tree widget.
	 *
	 * @param parent the parent shell
	 * @param style the additional styles for the tree widget
	 */
	public JavaOutlineInformationControl(Shell parent, int style) {
		this(parent, SWT.NO_TRIM, style);
	}

	/**
	 * Creates a tree information control with the given shell as parent.
	 * No additional styles are applied.
	 *
	 * @param parent the parent shell
	 */
	public JavaOutlineInformationControl(Shell parent) {
		this(parent, SWT.NONE);
	}

	/**
	 * Creates a tree information control with the given shell as parent. The given
	 * styles are applied to the shell and the tree widget.
	 *
	 * @param parent the parent shell
	 * @param shellStyle the additional styles for the shell
	 * @param treeStyle the additional styles for the tree widget
	 */
	public JavaOutlineInformationControl(Shell parent, int shellStyle, int treeStyle) {
		fShell= new Shell(parent, SWT.NO_FOCUS | SWT.ON_TOP | shellStyle);
		Display display= fShell.getDisplay();
		fShell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));

		// Composite for filter text and tree
		fComposite= new Composite(fShell,SWT.RESIZE);
		GridLayout layout= new GridLayout(1, false);
		fComposite.setLayout(layout);
		fComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		createFilterText(fComposite);
		createTreeViewer(fComposite, treeStyle);

		int border= ((shellStyle & SWT.NO_TRIM) == 0) ? 0 : BORDER;
		fShell.setLayout(new BorderFillLayout(border));
		
		setInfoSystemColor();
		installFilter();
	}

	private void createTreeViewer(Composite parent, int style) {
		Tree tree= new Tree(parent, SWT.SINGLE | (style & ~SWT.MULTI));
		GridData data= new GridData(GridData.FILL_BOTH);
		tree.setLayoutData(data);

		fTreeViewer= new TreeViewer(tree);

		// Hide import declartions but show the container
		fTreeViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return !(element instanceof IImportDeclaration);
			}
		});

		fTreeViewer.setContentProvider(new StandardJavaElementContentProvider(true, true));
		fTreeViewer.setSorter(new JavaElementSorter());
		fTreeViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		
		ILabelProvider lprovider= new AppearanceAwareLabelProvider(
			AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS |  JavaElementLabels.F_APP_TYPE_SIGNATURE,
			AppearanceAwareLabelProvider.DEFAULT_IMAGEFLAGS
		);
		fTreeViewer.setLabelProvider(new DecoratingLabelProvider(lprovider, PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));

		fTreeViewer.getTree().addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e)  {
				if (e.character == 0x1B) // ESC
					dispose();
			}
			public void keyReleased(KeyEvent e) {
				// do nothing
			}
		});

		fTreeViewer.getTree().addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				// do nothing
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				gotoSelectedElement();
			}
		});
	}

	private Text createFilterText(Composite parent) {
		fFilterText= new Text(parent, SWT.FLAT);

		GridData data= new GridData();
		GC gc= new GC(parent);
		gc.setFont(parent.getFont());
		FontMetrics fontMetrics= gc.getFontMetrics();
		gc.dispose();

		data.heightHint= org.eclipse.jface.dialogs.Dialog.convertHeightInCharsToPixels(fontMetrics, 1);
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
			}
			public void keyReleased(KeyEvent e) {
				// do nothing
			}
		});

		// Horizonral separator line
		Label separator= new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.LINE_DOT);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return fFilterText;
	}
	
	private void setInfoSystemColor() {
		Display display= fShell.getDisplay();
		setForegroundColor(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		setBackgroundColor(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
	}

	private void installFilter() {
		final NamePatternFilter viewerFilter= new NamePatternFilter();
		fTreeViewer.addFilter(viewerFilter);
		fFilterText.setText(""); //$NON-NLS-1$

		fFilterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String pattern= fFilterText.getText();
				if (pattern != null) {
					int length= pattern.length();
					if (length == 0)
						pattern= null;
					else if (pattern.charAt(length -1 ) != '*')
						pattern= pattern + '*';
				} else
					pattern= null;
				viewerFilter.setPattern(pattern);
				fStringMatcher= viewerFilter.getMatcher();
				fTreeViewer.getControl().setRedraw(false);
				fTreeViewer.refresh();
				fTreeViewer.expandAll();
				selectFirstMatch();
				fTreeViewer.getControl().setRedraw(true);
			}
		});
	}

	private void gotoSelectedElement() {
		Object selectedElement= ((IStructuredSelection)fTreeViewer.getSelection()).getFirstElement();
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
	private void selectFirstMatch() {
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

	/*
	 * @see IInformationControl#setInformation(String)
	 */
	public void setInformation(String information) {
		// this method is ignored, see IInformationControlExtension2
	}
	
	/*
	 * @see IInformationControlExtension2#setInput(Object)
	 */
	public void setInput(Object information) {
		fFilterText.setText(""); //$NON-NLS-1$
		if (information == null || information instanceof String) {
			setInput(null);
			return;
		}
		IJavaElement je= (IJavaElement)information;
		IJavaElement sel= null;
		ICompilationUnit cu= (ICompilationUnit)je.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (cu != null)
			sel= cu;
		else
			sel= je.getAncestor(IJavaElement.CLASS_FILE);
		fTreeViewer.setInput(sel);
		fTreeViewer.setSelection(new StructuredSelection(information));
	}

	/*
	 * @see IInformationControl#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
			fShell.setVisible(visible);
	}

	/*
	 * @see IInformationControl#dispose()
	 */
	public void dispose() {
		if (fShell != null) {
			if (!fShell.isDisposed())
				fShell.dispose();
			fShell= null;
			fTreeViewer= null;
			fComposite= null;
			fFilterText= null;
		}
	}

	/* 
	 * @see org.eclipse.jface.text.IInformationControlExtension#hasContents()
	 */
	public boolean hasContents() {
		return fTreeViewer != null && fTreeViewer.getInput() != null;
	}

	/* 
	 * @see org.eclipse.jface.text.IInformationControl#setSizeConstraints(int, int)
	 */
	public void setSizeConstraints(int maxWidth, int maxHeight) {
		fMaxWidth= maxWidth;
		fMaxHeight= maxHeight;
	}

	/* 
	 * @see org.eclipse.jface.text.IInformationControl#computeSizeHint()
	 */
	public Point computeSizeHint() {
		return fShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	}

	/*
	 * @see IInformationControl#setLocation(Point)
	 */
	public void setLocation(Point location) {
		Rectangle trim= fShell.computeTrim(0, 0, 0, 0);
		Point textLocation= fComposite.getLocation();				
		location.x += trim.x - textLocation.x;		
		location.y += trim.y - textLocation.y;		
		fShell.setLocation(location);		
	}

	/*
	 * @see IInformationControl#setSize(int, int)
	 */
	public void setSize(int width, int height) {
		fShell.setSize(width, height);
	}

	/*
	 * @see IInformationControl#addDisposeListener(DisposeListener)
	 */
	public void addDisposeListener(DisposeListener listener) {
		fShell.addDisposeListener(listener);
	}

	/*
	 * @see IInformationControl#removeDisposeListener(DisposeListener)
	 */
	public void removeDisposeListener(DisposeListener listener) {
		fShell.removeDisposeListener(listener);
	}

	/*
	 * @see IInformationControl#setForegroundColor(Color)
	 */
	public void setForegroundColor(Color foreground) {
		fTreeViewer.getTree().setForeground(foreground);
		fFilterText.setForeground(foreground);
		fComposite.setForeground(foreground);
	}

	/*
	 * @see IInformationControl#setBackgroundColor(Color)
	 */
	public void setBackgroundColor(Color background) {
		fTreeViewer.getTree().setBackground(background);
		fFilterText.setBackground(background);
		fComposite.setBackground(background);
	}

	/*
	 * @see IInformationControl#isFocusControl()
	 */
	public boolean isFocusControl() {
		return fShell.isFocusControl();
	}

	/*
	 * @see IInformationControl#setFocus()
	 */
	public void setFocus() {
		fFilterText.setFocus();
	}

	/*
	 * @see IInformationControl#addFocusListener(FocusListener)
	 */
	public void addFocusListener(FocusListener listener) {
		fShell.addFocusListener(listener);
	}

	/*
	 * @see IInformationControl#removeFocusListener(FocusListener)
	 */
	public void removeFocusListener(FocusListener listener) {
		fShell.removeFocusListener(listener);
	}
}
