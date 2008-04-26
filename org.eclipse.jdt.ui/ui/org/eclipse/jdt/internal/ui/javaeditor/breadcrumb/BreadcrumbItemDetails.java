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
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;


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
		layout.horizontalSpacing= 0;
		fDetailComposite.setLayout(layout);
		addElementListener(fDetailComposite);

		fImageComposite= new Composite(fDetailComposite, SWT.NONE);
		fImageComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		layout= new GridLayout(1, false);
		layout.marginHeight= 1;
		layout.marginWidth= 2;
		fImageComposite.setLayout(layout);
		fImageComposite.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				if (fHasFocus && !isTextVisible()) {
					e.gc.drawFocus(e.x, e.y, e.width, e.height);
				}
			}
		});
		installFocusComposite(fImageComposite);
		addElementListener(fImageComposite);

		fElementImage= new Label(fImageComposite, SWT.NONE);
		GridData layoutData= new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		fElementImage.setLayoutData(layoutData);
		addElementListener(fElementImage);

		fTextComposite= new Composite(fDetailComposite, SWT.NONE);
		fTextComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		layout= new GridLayout(1, false);
		layout.marginHeight= 2;
		layout.marginWidth= 2;
		fTextComposite.setLayout(layout);
		addElementListener(fTextComposite);
		fTextComposite.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				if (fHasFocus && isTextVisible()) {
					e.gc.drawFocus(e.x, e.y, e.width, e.height);
				}
			}
		});
		installFocusComposite(fTextComposite);
		addElementListener(fTextComposite);

		fElementText= new Label(fTextComposite, SWT.NONE);
		layoutData= new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
		fElementText.setLayoutData(layoutData);
		addElementListener(fElementText);
		
		fTextComposite.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			public void getName(AccessibleEvent e) {
				e.result= fElementText.getText();
			}
		});
		fImageComposite.getAccessible().addAccessibleListener(new AccessibleAdapter() {
			public void getName(AccessibleEvent e) {
				e.result= fElementText.getText();
			}
		});
		
		fDetailComposite.setTabList(new Control[] { fTextComposite });
	}
	
	/**
	 * @return true if this element has the keyboard focus.
	 */
	public boolean hasFocus() {
		return fHasFocus;
	}
	
	/**
	 * Set the tool tip to the given text.
	 * 
	 * @param text the tool tip
	 */
	public void setToolTip(String text) {
		if (isTextVisible()) {
			fElementText.getParent().setToolTipText(text);
			fElementText.setToolTipText(text);

			fElementImage.setToolTipText(text);
		} else {
			fElementText.getParent().setToolTipText(null);
			fElementText.setToolTipText(null);

			fElementImage.setToolTipText(text);
		}
	}

	/**
	 * Set the image to the given image.
	 * 
	 * @param image the image to use
	 */
	public void setImage(Image image) {
		fElementImage.setImage(image);
	}

	/**
	 * Set the text to the given text.
	 * 
	 * @param text the text to use
	 */
	public void setText(String text) {
		if (text != null) {
			fElementText.setText(text);
		} else {
			fElementText.setText(""); //$NON-NLS-1$
		}
	}

	/**
	 * @return current width of this element
	 */
	public int getWidth() {
		int result= 2;

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
		
		if (fTextVisible) {
			fDetailComposite.setTabList(new Control[] { fTextComposite });
		} else {
			fDetailComposite.setTabList(new Control[] { fImageComposite });
		}

		if (fHasFocus) {
			if (isTextVisible()) {
				fTextComposite.setFocus();
			} else {
				fImageComposite.setFocus();
			}
		}
		updateSelection();
	}

	/**
	 * Does this item show a text or only an image?
	 * 
	 * @return true if it shows a text and an image, false if it only shows the image
	 */
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
		
		GridData data= (GridData) fDetailComposite.getLayoutData();
		data.exclude= !visible;
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
		} else {
			foreground= null;
			background= null;
		}

		if (isTextVisible()) {
			fTextComposite.setBackground(background);
			fElementText.setBackground(background);
			fElementText.setForeground(foreground);

			fImageComposite.setBackground(null);
			fElementImage.setBackground(null);
		} else {
			fImageComposite.setBackground(background);
			fElementImage.setBackground(background);

			fTextComposite.setBackground(null);
			fElementText.setBackground(null);
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
		composite.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
					int index= fParent.getViewer().getIndexOfItem(fParent);
					if (e.detail == SWT.TRAVERSE_TAB_NEXT) {
						index++;
					} else {
						index--;
					}

					if (index > 0 && index < fParent.getViewer().getItemCount()) {
						fParent.getViewer().selectItem(fParent.getViewer().getItem(index));
					}

					e.doit= true;
				}
			}
		});
		composite.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				BreadcrumbViewer viewer= fParent.getViewer();
				
				switch (e.keyCode) {
					case SWT.ARROW_LEFT:
						if (fSelected) {
							viewer.doTraverse(false);
						} else {
							viewer.selectItem(fParent);
						}
						break;
					case SWT.ARROW_RIGHT:
						if (fSelected) {
							viewer.doTraverse(true);
						} else {
							viewer.selectItem(fParent);
						}
						break;
					case SWT.ARROW_DOWN:
						if (!fSelected) {
							viewer.selectItem(fParent);
						}
						openDropDown();
						break;
					case SWT.KEYPAD_ADD:
						if (!fSelected) {
							viewer.selectItem(fParent);
						}
						openDropDown();
						break;
					case SWT.CR:
						if (!fSelected) {
							viewer.selectItem(fParent);
						}
						viewer.fireOpen();
						break;
					default:
						if (e.character == ' ') {
							if (!fSelected) {
								viewer.selectItem(fParent);
							}
							openDropDown();
						}
						break;
				}
			}

			private void openDropDown() {
				BreadcrumbViewer viewer= fParent.getViewer();

				int index= viewer.getIndexOfItem(fParent);
				BreadcrumbItem parent= fParent.getViewer().getItem(index - 1);
				
				Shell shell= parent.getDropDownShell();
				if (shell == null) {
					parent.openDropDownMenu();
					shell= parent.getDropDownShell();
				}
				shell.setFocus();
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
			}

			public void mouseDown(MouseEvent e) {
				BreadcrumbViewer viewer= fParent.getViewer();
				viewer.selectItem(fParent);
				if (e.button == 1) {
					fParent.getViewer().fireDoubleClick();
				}
			}

			public void mouseUp(MouseEvent e) {
			}
		});
	}
}
