/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tracker;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener2;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.eclipse.jface.bindings.keys.KeyLookupFactory;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Geometry;

import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.IViewportListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.part.PageBook;

import org.eclipse.ui.forms.HyperlinkGroup;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

public class RenameInformationPopup {
	
	private class PopupVisibilityManager implements IPartListener2, ControlListener, MouseListener, KeyListener, ITextListener, IViewportListener {
		private boolean fAlwaysEnabled= true;
		
		public void start() {
			fEditor.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
			final ISourceViewer viewer= fEditor.getViewer();
			final StyledText textWidget= viewer.getTextWidget();
			textWidget.addControlListener(this);
			textWidget.addMouseListener(this);
			textWidget.addKeyListener(this);
			fEditor.getSite().getShell().addControlListener(this);
			viewer.addTextListener(this);
			viewer.addViewportListener(this);
			fPopup.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					fEditor.getSite().getWorkbenchWindow().getPartService().removePartListener(PopupVisibilityManager.this);
					if (! textWidget.isDisposed()) {
						textWidget.removeControlListener(PopupVisibilityManager.this);
						textWidget.removeMouseListener(PopupVisibilityManager.this);
						textWidget.removeKeyListener(PopupVisibilityManager.this);
					}
					fEditor.getSite().getShell().removeControlListener(PopupVisibilityManager.this);
					viewer.removeTextListener(PopupVisibilityManager.this);
					viewer.removeViewportListener(PopupVisibilityManager.this);
					if (fMenuImage != null)
						fMenuImage.dispose();
					if (fMinimizedMenuManager != null)
						fMinimizedMenuManager.dispose();
					if (fTableMenuManager != null)
						fTableMenuManager.dispose();
					
					// workaround for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=173438 :
//					fRenameLinkedMode.cancel();
					fEditor.getSite().getShell().getDisplay().asyncExec(new Runnable() {
						public void run() {
							fRenameLinkedMode.cancel();
						}
					});
				}
			});
		}
		
		public void partActivated(IWorkbenchPartReference partRef) {
			IWorkbenchPart fPart= fEditor.getEditorSite().getPart();
			if (partRef.getPart(false) == fPart) {
				updateVisibility();
			}
		}

		public void partBroughtToTop(IWorkbenchPartReference partRef) {
		}

		public void partClosed(IWorkbenchPartReference partRef) {
		}

		public void partDeactivated(IWorkbenchPartReference partRef) {
			IWorkbenchPart fPart= fEditor.getEditorSite().getPart();
			if (fPopup != null && ! fPopup.isDisposed() && partRef.getPart(false) == fPart) {
				fPopup.setVisible(false);
			}
		}

		public void partHidden(IWorkbenchPartReference partRef) {
		}

		public void partInputChanged(IWorkbenchPartReference partRef) {
		}

		public void partOpened(IWorkbenchPartReference partRef) {
		}

		public void partVisible(IWorkbenchPartReference partRef) {
		}


		public void controlMoved(ControlEvent e) {
			updatePopupLocation(true);
			updateVisibility(); //only for hiding outside editor area
		}

		public void controlResized(ControlEvent e) {
			updatePopupLocation(true);
			updateVisibility(); //only for hiding outside editor area
		}
		
		
		private void updateVisibility() {
			if (fPopup != null && !fPopup.isDisposed()) {
				boolean visible= false;
				if (fRenameLinkedMode.isCaretInLinkedPosition()) {
					StyledText textWidget= fEditor.getViewer().getTextWidget();
					Rectangle eArea= Geometry.toDisplay(textWidget, textWidget.getClientArea());
					Rectangle pBounds= fPopup.getBounds();
					if (eArea.intersects(pBounds)) {
						visible= true;
					}
				}
				fPopup.setVisible(visible);
			}
		}

		public void mouseDoubleClick(MouseEvent e) {
		}

		public void mouseDown(MouseEvent e) {
		}

		public void mouseUp(MouseEvent e) {
			updatePopupLocation(false);
			updateVisibility();
		}

		public void keyPressed(KeyEvent e) {
			updatePopupLocation(false);
			updateVisibility();
		}

		public void keyReleased(KeyEvent e) {
		}
		

		public void textChanged(TextEvent event) {
			updateEnablement();
			updatePopupLocation(false);
			updateVisibility(); //only for hiding outside editor area
		}
		
		private void updateEnablement() {
			if (fPopup != null && !fPopup.isDisposed()) {
				if (fAlwaysEnabled) {
					if (fRenameLinkedMode.isOriginalName()) {
						return;
					} else {
						fAlwaysEnabled= false;
					}
				}
				boolean enabled= fRenameLinkedMode.isEnabled();
				for (Iterator iterator= fRefactorEntries.iterator(); iterator.hasNext();) {
					InfoEntry entry= (InfoEntry) iterator.next();
					entry.setEnabled(enabled);
				}
			}
		}
		
		public void viewportChanged(int verticalOffset) {
			updatePopupLocation(true);
			updateVisibility(); //only for hiding outside editor area
		}
	}

	private static class DisableAwareHyperlink extends Hyperlink {
		//workaround for 169859: Hyperlink widget should be rendered gray when disabled
		
		public DisableAwareHyperlink(Composite parent, int style) {
			super(parent, style);
		}
		
		public Color getForeground() {
			if (getEnabled())
				return super.getForeground();
			else
				return getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
		}
	}
	
	private class InfoEntry {
		private final Hyperlink fLink;
		private final Label fBindingLabel;

		public InfoEntry(Composite table, HyperlinkGroup hyperlinkGroup, String info, final Runnable runnable, String keybinding) {
			final Display display= table.getDisplay();
			fLink= new DisableAwareHyperlink(table, SWT.NONE);
			fLink.setText(info);
			fLink.setForeground(hyperlinkGroup.getForeground());
			fLink.addHyperlinkListener(new HyperlinkAdapter() {
				public void linkActivated(HyperlinkEvent e) {
					//workaround for 157196: [Forms] Hyperlink listener notification throws AIOOBE when listener removed in callback
					display.asyncExec(runnable);
				}
			});
			hyperlinkGroup.add(fLink);
			
			if (keybinding != null) {
				fBindingLabel= new Label(table, SWT.NONE);
				fBindingLabel.setText(keybinding);
				addMoveSupport(fPopup, fBindingLabel);
			} else {
				fBindingLabel= null;
			}
		}
		
		public void setEnabled(boolean enabled) {
			fLink.setEnabled(enabled);
			if (fBindingLabel != null)
				fBindingLabel.setEnabled(enabled);
		}
		
		public Hyperlink getLink() {
			return fLink;
		}
	}
	
	private static final String DIALOG_SETTINGS_SECTION= "RenameInformationPopup"; //$NON-NLS-1$
	private static final String SNAP_POSITION_KEY= "snap_position"; //$NON-NLS-1$
	private static final String SNAP_POSITION_MINIMIZED_KEY= "snap_position_minimized"; //$NON-NLS-1$
	private static final String IS_MINIMIZED_KEY= "is_minimized"; //$NON-NLS-1$

	private static final int SNAP_POSITION_UNDER_RIGHT_FIELD= 0;
	private static final int SNAP_POSITION_OVER_RIGHT_FIELD= 1;
	private static final int SNAP_POSITION_UNDER_LEFT_FIELD= 2;
	private static final int SNAP_POSITION_OVER_LEFT_FIELD= 3;
	private static final int SNAP_POSITION_LOWER_RIGHT= 4;
	
	private final CompilationUnitEditor fEditor;
	private final RenameLinkedMode fRenameLinkedMode;
	
	private int fSnapPosition;
	private boolean fIsMinimized;
	private Shell fPopup;
	private List/*<InfoEntry>*/ fRefactorEntries;
	private Image fMenuImage;
	private MenuManager fMinimizedMenuManager;
	private MenuManager fTableMenuManager;
	private boolean fIsMenuUp= false;
	
	public RenameInformationPopup(CompilationUnitEditor editor, RenameLinkedMode renameLinkedMode) {
		fEditor= editor;
		fRenameLinkedMode= renameLinkedMode;
		fIsMinimized= getDialogSettings().getBoolean(IS_MINIMIZED_KEY);
		restoreSnapPosition();
	}

	private void restoreSnapPosition() {
		IDialogSettings settings= getDialogSettings();
		if (fIsMinimized) {
			try {
				fSnapPosition= settings.getInt(SNAP_POSITION_MINIMIZED_KEY);
			} catch (NumberFormatException e) {
				fSnapPosition= SNAP_POSITION_LOWER_RIGHT;
			}
		} else {
			try {
				fSnapPosition= settings.getInt(SNAP_POSITION_KEY);
			} catch (NumberFormatException e) {
				fSnapPosition= SNAP_POSITION_UNDER_LEFT_FIELD;
			}
		}
	}

	private IDialogSettings getDialogSettings() {
		return JavaPlugin.getDefault().getDialogSettingsSection(DIALOG_SETTINGS_SECTION);
	}

	public void open() {
		ISourceViewer viewer= fEditor.getViewer();
		
		Shell workbenchShell= fEditor.getSite().getShell();
		final Display display= workbenchShell.getDisplay();
		
		fPopup= new Shell(workbenchShell, SWT.ON_TOP | SWT.NO_TRIM);
		GridLayout shellLayout= new GridLayout();
		shellLayout.marginWidth= 1;
		shellLayout.marginHeight= 1;
		fPopup.setLayout(shellLayout);
		fPopup.setBackground(viewer.getTextWidget().getForeground());
		
		createTable(fPopup);
		
		fPopup.pack();
		updatePopupLocation(true);
		
		addMoveSupport(fPopup, fPopup);
		new PopupVisibilityManager().start();
		
		// Leave linked mode when popup loses focus
		// (except when focus goes back to workbench window or menu is open):
		fPopup.addShellListener(new ShellAdapter() {
			public void shellDeactivated(ShellEvent e) {
				if (fIsMenuUp)
					return;
				
				final Shell editorShell= fEditor.getSite().getShell();
				display.asyncExec(new Runnable() {
					// post to UI thread since editor shell only gets activated after popup has lost focus
					public void run() {
						Shell activeShell= display.getActiveShell();
						if (activeShell != editorShell) {
							fRenameLinkedMode.cancel();
						}
					}
				});
			}
		});
		
//		fPopup.moveBelow(null); // make sure hovers are on top of the info popup
// XXX workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=170774
		fPopup.moveBelow(workbenchShell.getShells()[0]);
		
		fPopup.setVisible(true);
	}

	public void close() {
		if (fPopup != null) {
			if (!fPopup.isDisposed()) {
				fPopup.close();
			}
			fPopup= null;
		}
	}
	
	public Shell getShell() {
		return fPopup;
	}
	
	private void updatePopupLocation(boolean force) {
		if (! force && fSnapPosition == SNAP_POSITION_LOWER_RIGHT)
			return;
		
		Point loc= computePopupLocation(fSnapPosition);
		if (loc != null) {
			fPopup.setLocation(loc);
			// XXX workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=170774
			fPopup.moveBelow(fEditor.getSite().getShell().getShells()[0]);
		}
	}

	/**
	 * @returns the location in display coordinates or <code>null</code> iff not visible
	 */
	private Point computePopupLocation(int snapPosition) {
		if (fPopup == null || fPopup.isDisposed())
			return null;
		
		switch (snapPosition) {
			case SNAP_POSITION_LOWER_RIGHT:
			{
				StyledText eWidget= fEditor.getViewer().getTextWidget();
				Rectangle eBounds= eWidget.getClientArea();
				Point eLowerRight= eWidget.toDisplay(eBounds.x + eBounds.width, eBounds.y + eBounds.height);
				Point pSize= fPopup.getSize();
				return new Point(eLowerRight.x - pSize.x - 5, eLowerRight.y - pSize.y - 5);
			}
			
			case SNAP_POSITION_UNDER_RIGHT_FIELD:
			case SNAP_POSITION_OVER_RIGHT_FIELD:
			{
				LinkedPosition position= fRenameLinkedMode.getCurrentLinkedPosition();
				if (position == null)
					return null;
				ISourceViewer viewer= fEditor.getViewer();
				ITextViewerExtension5 viewer5= (ITextViewerExtension5) viewer;
				int widgetOffset= viewer5.modelOffset2WidgetOffset(position.offset + position.length);
				
				StyledText textWidget= viewer.getTextWidget();
				Point pos= textWidget.getLocationAtOffset(widgetOffset);
				Point pSize= fPopup.getSize();
				if (snapPosition == SNAP_POSITION_OVER_RIGHT_FIELD) {
					pos.y-= pSize.y;
				} else {
					pos.y+= textWidget.getLineHeight(widgetOffset);
				}
				pos.x+= 2;
				Point dPos= textWidget.toDisplay(pos);
				Rectangle displayBounds= textWidget.getDisplay().getClientArea();
				Rectangle dPopupRect= Geometry.createRectangle(dPos, pSize);
				Geometry.moveInside(dPopupRect, displayBounds);
				return new Point(dPopupRect.x, dPopupRect.y);
			}
			
			case SNAP_POSITION_UNDER_LEFT_FIELD:
			case SNAP_POSITION_OVER_LEFT_FIELD:
			default: // same as SNAP_POSITION_UNDER_LEFT_FIELD
			{
				LinkedPosition position= fRenameLinkedMode.getCurrentLinkedPosition();
				if (position == null)
					return null;
				ISourceViewer viewer= fEditor.getViewer();
				ITextViewerExtension5 viewer5= (ITextViewerExtension5) viewer;
				int widgetOffset= viewer5.modelOffset2WidgetOffset(position.offset/* + position.length*/);
				
				StyledText textWidget= viewer.getTextWidget();
				Point pos= textWidget.getLocationAtOffset(widgetOffset);
				Point pSize= fPopup.getSize();
				if (snapPosition == SNAP_POSITION_OVER_LEFT_FIELD) {
					pos.y-= pSize.y;
				} else {
					pos.y+= textWidget.getLineHeight(widgetOffset);
				}
				Point dPos= textWidget.toDisplay(pos);
				Rectangle displayBounds= textWidget.getDisplay().getClientArea();
				Rectangle dPopupRect= Geometry.createRectangle(dPos, pSize);
				Geometry.moveInside(dPopupRect, displayBounds);
				return new Point(dPopupRect.x, dPopupRect.y);
			}
			
		}
	}

	private void addMoveSupport(final Shell popupShell, final Control movedControl) {
		movedControl.addMouseListener(new MouseAdapter() {

			public void mouseDown(final MouseEvent downEvent) {
				if (downEvent.button != 1) {
					return;
				}
				
				final Point POPUP_SOURCE= popupShell.getLocation();
				final StyledText textWidget= fEditor.getViewer().getTextWidget();
				Point pSize= fPopup.getSize();
				int originalSnapPosition= fSnapPosition;
				
				/*
				 * Feature in Tracker: it is not possible to directly control the feedback,
				 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=121300
				 * and https://bugs.eclipse.org/bugs/show_bug.cgi?id=121298#c1 .
				 * 
				 * Workaround is to have an offscreen rectangle for tracking mouse movement
				 * and a manually updated rectangle for the actual drop target.
				 */
				final Tracker tracker= new Tracker(textWidget, SWT.NONE);
				
				final Point[] LOCATIONS= {
						textWidget.toControl(computePopupLocation(SNAP_POSITION_UNDER_RIGHT_FIELD)),
						textWidget.toControl(computePopupLocation(SNAP_POSITION_OVER_RIGHT_FIELD)),
						textWidget.toControl(computePopupLocation(SNAP_POSITION_UNDER_LEFT_FIELD)),
						textWidget.toControl(computePopupLocation(SNAP_POSITION_OVER_LEFT_FIELD)),
						textWidget.toControl(computePopupLocation(SNAP_POSITION_LOWER_RIGHT))
				};
				
				final Rectangle[] DROP_TARGETS= new Rectangle[LOCATIONS.length];
				for (int i= 0; i < LOCATIONS.length; i++) {
					DROP_TARGETS[i]= Geometry.createRectangle(LOCATIONS[i], pSize);
				}
				final Rectangle MOUSE_MOVE_SOURCE= new Rectangle(1000000, 0, 0, 0);
				tracker.setRectangles(new Rectangle[] { MOUSE_MOVE_SOURCE, DROP_TARGETS[fSnapPosition] });
				tracker.setStippled(true);
				
				ControlListener moveListener= new ControlAdapter() {
					/*
					 * @see org.eclipse.swt.events.ControlAdapter#controlMoved(org.eclipse.swt.events.ControlEvent)
					 */
					public void controlMoved(ControlEvent moveEvent) {
						Rectangle[] currentRects= tracker.getRectangles();
						final Rectangle mouseMoveCurrent= currentRects[0];
						Point popupLoc= new Point(
								POPUP_SOURCE.x + mouseMoveCurrent.x - MOUSE_MOVE_SOURCE.x,
								POPUP_SOURCE.y + mouseMoveCurrent.y - MOUSE_MOVE_SOURCE.y);
						
						popupShell.setLocation(popupLoc);
						
						Point ePopupLoc= textWidget.toControl(popupLoc);
						int minDist= Integer.MAX_VALUE;
						for (int snapPos= 0; snapPos < DROP_TARGETS.length; snapPos++) {
							int dist= Geometry.distanceSquared(ePopupLoc, LOCATIONS[snapPos]);
							if (dist < minDist) {
								minDist= dist;
								fSnapPosition= snapPos;
								currentRects[1]= DROP_TARGETS[snapPos];
							}
						}
						tracker.setRectangles(currentRects);
					}
				};
				tracker.addControlListener(moveListener);
				boolean committed= tracker.open();
				tracker.close();
				tracker.dispose();
				if (committed) {
					getDialogSettings().put(fIsMinimized ? SNAP_POSITION_MINIMIZED_KEY : SNAP_POSITION_KEY, fSnapPosition);
				} else {
					fSnapPosition= originalSnapPosition;
				}
				updatePopupLocation(true);
				fEditor.getSite().getShell().setActive();
			}
		});
	}
	
	private Control createTable(Composite parent) {
		final Display display= parent.getDisplay();
		Color foreground= display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		Color background= display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		
		final PageBook book= new PageBook(parent, SWT.NONE);
		final Composite table= new Composite(book, SWT.NONE);
		final Composite minimized= new Composite(book, SWT.NONE);
		
		GridLayout tableLayout= new GridLayout(2, false);
		tableLayout.marginHeight= 4; 
		tableLayout.marginWidth= 4;
		tableLayout.horizontalSpacing= 10;
		tableLayout.verticalSpacing= 2;
		table.setLayout(tableLayout);
		
		HyperlinkGroup refactorGroup= new HyperlinkGroup(display);
		refactorGroup.setForeground(foreground);
		refactorGroup.setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_HOVER);
		fRefactorEntries= new ArrayList();
		
		InfoEntry refactorEntry= new InfoEntry(
				table,
				refactorGroup,
				ReorgMessages.RenameInformationPopup_refactor_rename,
				new Runnable() {
					public void run() {
						fRenameLinkedMode.doRename(false);
					}
				},
				KeyStroke.getInstance(KeyLookupFactory.getDefault().formalKeyLookup(IKeyLookup.CR_NAME)).format());
		refactorEntry.getLink().setFont(JFaceResources.getFontRegistry().getBold("")); //$NON-NLS-1$ // bold OS font
		fRefactorEntries.add(refactorEntry);
		
		InfoEntry previewEntry= new InfoEntry(
				table,
				refactorGroup,
				ReorgMessages.RenameInformationPopup_preview,
				new Runnable() {
					public void run() {
						fRenameLinkedMode.doRename(true);
					}
				},
				KeyStroke.getInstance(SWT.CTRL, KeyLookupFactory.getDefault().formalKeyLookup(IKeyLookup.CR_NAME)).format());
		fRefactorEntries.add(previewEntry);

		new InfoEntry(
				table,
				refactorGroup,
				ReorgMessages.RenameInformationPopup_open_dialog,
				new Runnable() {
					public void run() {
						fRenameLinkedMode.startFullDialog();
					}
				},
				getOpenDialogBinding());
		
		HyperlinkGroup cancelGroup= new HyperlinkGroup(display);
		cancelGroup.setForeground(foreground);
		cancelGroup.setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_HOVER);
		
		new InfoEntry(
				table,
				cancelGroup,
				ReorgMessages.RenameInformationPopup_leave,
				new Runnable() {
					public void run() {
						fRenameLinkedMode.cancel();
					}
				},
				KeyStroke.getInstance(KeyLookupFactory.getDefault().formalKeyLookup(IKeyLookup.ESC_NAME)).format());
		
		addMoveSupport(fPopup, table);
		
		ToolBar tableToolBar= addViewMenu(book, table, minimized, true);
		GridData gridData= new GridData();
		gridData.exclude= true;
		tableToolBar.setLayoutData(gridData);
		Point tableSize= table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		// having the button overlap the keybinding for refactor is OK, since Ctrl+Enter is always wider than Enter...
		tableToolBar.setLocation(tableSize.x - tableToolBar.getSize().x, 0);
		
		// minimized:
		GridLayout minimizedLayout= new GridLayout(2, false);
		minimizedLayout.marginWidth= minimizedLayout.marginHeight= 0;
		minimizedLayout.horizontalSpacing= 0;
		minimized.setLayout(minimizedLayout);
		
		Composite minimizedBorder= new Composite(minimized, SWT.NONE);
		GridLayout minimizedBorderLayout= new GridLayout(1, false);
		minimizedBorderLayout.marginHeight= tableLayout.marginHeight; 
		minimizedBorderLayout.marginWidth= tableLayout.marginWidth;
		minimizedBorder.setLayout(minimizedBorderLayout);
		
		InfoEntry minRefactorEntry= new InfoEntry(
				minimizedBorder,
				refactorGroup,
				ReorgMessages.RenameInformationPopup_refactor_rename,
				new Runnable() {
					public void run() {
						fRenameLinkedMode.doRename(false);
					}
				},
				null);
		minRefactorEntry.getLink().setFont(JFaceResources.getFontRegistry().getBold("")); //$NON-NLS-1$ // bold OS font
		fRefactorEntries.add(minRefactorEntry);
		
		addMoveSupport(fPopup, minimized);
		addMoveSupport(fPopup, minimizedBorder);
		
		addViewMenu(book, table, minimized, false);
		
		recursiveSetBackgroundColor(book, background);
		book.showPage(fIsMinimized ? minimized : table);
		return table;
	}

	private ToolBar addViewMenu(final PageBook book, final Composite table, final Composite minimized, final boolean addToTable) {
		final ToolBar toolBar= new ToolBar(addToTable ? table : minimized, SWT.FLAT);
		final ToolItem menuButton = new ToolItem(toolBar, SWT.PUSH, 0);
		fMenuImage= JavaPluginImages.DESC_ELCL_VIEW_MENU.createImage();
		menuButton.setImage(fMenuImage);
		menuButton.setToolTipText(ReorgMessages.RenameInformationPopup_menu);
		toolBar.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				showMenu(book, table, minimized, addToTable, toolBar);
			}
		});
		menuButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showMenu(book, table, minimized, addToTable, toolBar);
			}
		});
		toolBar.pack();
		return toolBar;
	}

	private void showMenu(PageBook book, Composite table, Composite minimized, boolean addToTable, ToolBar toolBar) {
		MenuManager menuManager;
		if (addToTable) {
			if (fTableMenuManager == null)
				fTableMenuManager= createMenuManager(book, table, minimized);
			menuManager= fTableMenuManager;
		} else {
			if (fMinimizedMenuManager == null)
				fMinimizedMenuManager= createMenuManager(book, table, minimized);
			menuManager= fMinimizedMenuManager;
		}
		Menu menu= menuManager.createContextMenu(toolBar);
		menu.setLocation(toolBar.toDisplay(0, toolBar.getSize().y));
		fIsMenuUp= true;
		menu.setVisible(true);
	}

	private MenuManager createMenuManager(final PageBook book, final Composite table, final Composite minimized) {
		MenuManager menuManager= new MenuManager();
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener2() {
			public void menuAboutToHide(IMenuManager manager) {
				fIsMenuUp= false;
			}
			public void menuAboutToShow(IMenuManager manager) {
				IAction action= new Action(
						fIsMinimized ? ReorgMessages.RenameInformationPopup_restore : ReorgMessages.RenameInformationPopup_minimize,
						SWT.PUSH) {
					public void run() {
						book.showPage(fIsMinimized ? table : minimized);
						fPopup.pack();
						fIsMinimized= ! fIsMinimized;
						getDialogSettings().put(IS_MINIMIZED_KEY, fIsMinimized); 
						restoreSnapPosition();
						updatePopupLocation(true);
						fEditor.getSite().getShell().setActive();
					}
				};
				manager.add(action);
				
				manager.add(new Separator());
				addMoveMenuItem(manager, SNAP_POSITION_UNDER_LEFT_FIELD, ReorgMessages.RenameInformationPopup_snap_under_left);
				addMoveMenuItem(manager, SNAP_POSITION_UNDER_RIGHT_FIELD, ReorgMessages.RenameInformationPopup_snap_under_right);
				addMoveMenuItem(manager, SNAP_POSITION_OVER_LEFT_FIELD, ReorgMessages.RenameInformationPopup_snap_over_left);
				addMoveMenuItem(manager, SNAP_POSITION_OVER_RIGHT_FIELD, ReorgMessages.RenameInformationPopup_snap_over_right);
				addMoveMenuItem(manager, SNAP_POSITION_LOWER_RIGHT, ReorgMessages.RenameInformationPopup_snap_bottom_right);
			}
		});
		return menuManager;
	}

	private void addMoveMenuItem(IMenuManager manager, final int snapPosition, String text) {
		IAction action= new Action(text, IAction.AS_RADIO_BUTTON) {
			public void run() {
				fSnapPosition= snapPosition;
				getDialogSettings().put(fIsMinimized ? SNAP_POSITION_MINIMIZED_KEY : SNAP_POSITION_KEY, fSnapPosition);
				updatePopupLocation(true);
				fEditor.getSite().getShell().setActive();
			}
		};
		action.setChecked(fSnapPosition == snapPosition);
		manager.add(action);
	}
	
	private static String getOpenDialogBinding() {
		IBindingService bindingService= (IBindingService)PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		if (bindingService == null)
			return ""; //$NON-NLS-1$
		String binding= bindingService.getBestActiveBindingFormattedFor(IJavaEditorActionDefinitionIds.RENAME_ELEMENT);
		return binding == null ? "" : binding; //$NON-NLS-1$
	}
	
	private static void recursiveSetBackgroundColor(Control control, Color color) {
		control.setBackground(color);
		if (control instanceof Composite) {
			Control[] children= ((Composite) control).getChildren();
			for (int i= 0; i < children.length; i++) {
				recursiveSetBackgroundColor(children[i], color);
			}
		}
	}

	public boolean ownsFocusShell() {
		if (fIsMenuUp)
			return true;
		if (fPopup == null || fPopup.isDisposed())
			return false;
		Shell activeShell= fPopup.getDisplay().getActiveShell();
		if (fPopup == activeShell)
			return true;
		return false;
	}
}
