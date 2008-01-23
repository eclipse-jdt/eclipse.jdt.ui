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
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;


/**
 * The label and icon part of the breadcrumb item.
 * 
 * @since 3.4
 */
class BreadcrumbItemDetails {

	private final Label fElementImage;
	private final Label fElementText;
	private final Composite fDetailComposite;
	private final BreadcrumbItem fParent;

	private boolean fTextVisible;
	private boolean fSelected;
	private boolean fHasFocus;

	private Composite fTextComposite;
	private Composite fImageComposite;

	public BreadcrumbItemDetails(BreadcrumbItem parent, Composite parentContainer) {
		fParent= parent;
		fTextVisible= true;

		fDetailComposite= new Composite(parentContainer, SWT.NONE);
		fDetailComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		GridLayout layout= new GridLayout(2, false);
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		fDetailComposite.setLayout(layout);
		fDetailComposite.setBackground(parentContainer.getBackground());
		addElementListener(fDetailComposite);

		fImageComposite= new Composite(fDetailComposite, SWT.NONE);
		fImageComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		layout= new GridLayout(1, false);
		layout.marginHeight= 2;
		layout.marginWidth= 2;
		fImageComposite.setLayout(layout);
		fImageComposite.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				if (fHasFocus && !isTextVisible()) {
					e.gc.drawFocus(e.x, e.y, e.width, e.height);
				}
			}
		});
		fImageComposite.setBackground(parentContainer.getBackground());
		installFocusComposite(fImageComposite);

		fElementImage= new Label(fImageComposite, SWT.NONE);
		GridData layoutData= new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		fElementImage.setLayoutData(layoutData);
		fElementImage.setBackground(parentContainer.getBackground());
		addElementListener(fElementImage);

		fTextComposite= new Composite(fDetailComposite, SWT.NONE);
		fTextComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		layout= new GridLayout(1, false);
		layout.marginHeight= 2;
		layout.marginWidth= 2;
		fTextComposite.setLayout(layout);
		fTextComposite.setBackground(parentContainer.getBackground());
		addElementListener(fTextComposite);
		fTextComposite.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				if (fHasFocus && isTextVisible()) {
					e.gc.drawFocus(e.x, e.y, e.width, e.height);
				}
			}
		});
		installFocusComposite(fTextComposite);

		fElementText= new Label(fTextComposite, SWT.NONE);
		layoutData= new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		fElementText.setLayoutData(layoutData);
		fElementText.setBackground(parentContainer.getBackground());
		addElementListener(fElementText);
	}
	
	/**
	 * @return true if this element has the keyboard focus.
	 */
	public boolean hasFocus() {
		return fHasFocus;
	}

	/**
	 * Set values to display to the given values
	 * 
	 * @param text label text
	 * @param image label image
	 * @param toolTipText tool tip text
	 */
	public void setContent(String text, Image image, String toolTipText) {
		if (text != null) {
			fElementText.setText(text);
			fParent.setText(text);
		} else {
			fElementText.setText(""); //$NON-NLS-1$
			fParent.setText(""); //$NON-NLS-1$
		}
		fElementImage.setImage(image);
		fParent.setImage(image);

		if (isTextVisible()) {
			fElementText.getParent().setToolTipText(toolTipText);
			fElementText.setToolTipText(toolTipText);

			fElementImage.setToolTipText(toolTipText);
		} else {
			fElementText.getParent().setToolTipText(null);
			fElementText.setToolTipText(null);

			fElementImage.setToolTipText(toolTipText);
		}
	}

	/**
	 * @return current width of this element
	 */
	public int getWidth() {
		int result= 8;

		if (fElementImage.getImage() != null)
			result+= fElementImage.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;

		if (fTextComposite.isVisible() && fElementText.getText().length() > 0)
			result+= fElementText.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;

		return result;
	}

	public void setTextVisible(boolean enabled) {
		if (fTextVisible == enabled)
			return;

		fTextVisible= enabled;

		GridData data= (GridData) fTextComposite.getLayoutData();
		data.exclude= !enabled;
		fTextComposite.setVisible(enabled);

		if (fHasFocus) {
			if (isTextVisible()) {
				fTextComposite.setFocus();
			} else {
				fImageComposite.setFocus();
			}
		}
		updateSelection();
	}

	public boolean isTextVisible() {
		return fTextVisible;
	}

	/**
	 * Sets whether details should be shown
	 * 
	 * @param visible true if details should be shown
	 */
	public void setVisible(boolean visible) {
		fDetailComposite.setVisible(visible);
	}

	public void setSelected(boolean selected) {
		if (selected == fSelected)
			return;

		fSelected= selected;
		if (!fSelected)
			fHasFocus= false;

		updateSelection();
	}

	public void setFocus(boolean enabled) {
		if (enabled == fHasFocus)
			return;

		fHasFocus= enabled;
		if (fHasFocus) {
			if (isTextVisible()) {
				fTextComposite.setFocus();
			} else {
				fImageComposite.setFocus();
			}
		}
		updateSelection();
	}

	private void updateSelection() {
		Color background;
		Color foreground;

		if (fSelected && fHasFocus) {
			background= Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
			foreground= Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
		} else if (fSelected) {
			foreground= null;
			background= null;
		} else {
			foreground= null;
			background= fDetailComposite.getBackground();
		}

		if (isTextVisible()) {
			fTextComposite.setBackground(background);
			fElementText.setBackground(background);
			fElementText.setForeground(foreground);

			fImageComposite.setBackground(fDetailComposite.getBackground());
			fElementImage.setBackground(fDetailComposite.getBackground());
		} else {
			fImageComposite.setBackground(background);
			fElementImage.setBackground(background);

			fTextComposite.setBackground(fDetailComposite.getBackground());
			fElementText.setBackground(fDetailComposite.getBackground());
			fElementText.setForeground(null);
		}

		fTextComposite.redraw();
		fImageComposite.redraw();
	}

	/**
	 * Install focus and key listeners to the given composite
	 * 
	 * @param composite the composite which may get focus
	 */
	private void installFocusComposite(Composite composite) {
		composite.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				switch (e.keyCode) {
					case SWT.ARROW_LEFT:
						if (fSelected) {
							fParent.getTree().doTraverse(false);
						} else {
							fParent.getTree().selectItem(fParent);
						}
						break;
					case SWT.ARROW_RIGHT:
						if (fSelected) {
							fParent.getTree().doTraverse(true);
						} else {
							fParent.getTree().selectItem(fParent);
						}
						break;
					case SWT.ARROW_DOWN:
						if (!fSelected) {
							fParent.getTree().selectItem(fParent);
						}
						fParent.openDropDownMenu(null);
						break;
					case SWT.KEYPAD_ADD:
						if (!fSelected) {
							fParent.getTree().selectItem(fParent);
						}
						fParent.openDropDownMenu(null);
						break;
					case SWT.CR:
						if (!fSelected) {
							fParent.getTree().selectItem(fParent);
						}
						fParent.getTree().fireOpen();
						break;
					default:
						if ((e.stateMask == SWT.NONE || e.stateMask == SWT.SHIFT) && Character.isLetterOrDigit(e.character)) {
							String filterText= new String(new char[] { e.character });
							if (!fSelected) {
								fParent.getTree().selectItem(fParent);
							}
							fParent.openDropDownMenu(filterText);
						} else if (e.character == '\t' && (e.stateMask & SWT.CTRL) != 0) {
							if ((e.stateMask & SWT.SHIFT) != 0) {
								fParent.getTree().getControl().getParent().traverse(SWT.TRAVERSE_TAB_NEXT);
							} else {
								BreadcrumbViewer viewer= fParent.getTree();
								if (viewer.getRoot() == fParent.getData()) {
									viewer.selectItem(viewer.getItem(fParent.getTree().getItemCount() - 1));
								} else {
									viewer.selectItem(viewer.getItem(0));
								}
							}
						}
						break;
				}
			}

			public void keyReleased(KeyEvent e) {
			}
		});

		composite.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				if (!fHasFocus) {
					fHasFocus= true;
					updateSelection();
				}
			}

			public void focusLost(FocusEvent e) {
				if (fHasFocus) {
					fHasFocus= false;
					updateSelection();
				}
			}
		});
	}
	
	/**
	 * Add mouse listeners to the given control
	 * 
	 * @param control the control to which may be clicked 
	 */
	private void addElementListener(Control control) {
		control.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {
				fParent.getTree().fireDoubleClick();
			}

			public void mouseDown(MouseEvent e) {
				fParent.getTree().selectItem(fParent);
			}

			public void mouseUp(MouseEvent e) {
			}
		});
	}
}
