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
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.eclipse.jface.bindings.keys.KeyLookupFactory;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IEditingSupport;
import org.eclipse.jface.text.IEditingSupportRegistry;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;

import org.eclipse.ui.forms.HyperlinkGroup;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;

public class RenameInformationPopup {
	
	private class FocusEditingSupport implements IEditingSupport {
		public boolean ownsFocusShell() {
			if (fPopup == null || fPopup.isDisposed())
				return false;
			Control focusControl= fPopup.getDisplay().getFocusControl();
			return focusControl != null && focusControl.getShell() == fPopup;
		}

		public boolean isOriginator(DocumentEvent event, IRegion subjectRegion) {
			return true;
		}
	}
	
	private class PopupVisibilityManager implements IPartListener2, ControlListener {
		public void start() {
			fEditor.getSite().getWorkbenchWindow().getPartService().addPartListener(this);
			fEditor.getViewer().getTextWidget().addControlListener(this);
			fEditor.getSite().getShell().addControlListener(this);
			fPopup.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					fEditor.getSite().getWorkbenchWindow().getPartService().removePartListener(PopupVisibilityManager.this);
					fEditor.getViewer().getTextWidget().removeControlListener(PopupVisibilityManager.this);
					fEditor.getSite().getShell().removeControlListener(PopupVisibilityManager.this);
					fPopup.removeDisposeListener(this);
				}
			});
		}
		
		public void stop() {
			fEditor.getSite().getWorkbenchWindow().getPartService().removePartListener(this);
		}
		
		public void partActivated(IWorkbenchPartReference partRef) {
			IWorkbenchPart fPart= fEditor.getEditorSite().getPart();
			if (fPopup != null && ! fPopup.isDisposed() && partRef.getPart(false) == fPart) {
				fPopup.setVisible(true);
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
			setPopupLocation(fEditor.getViewer());
		}

		public void controlResized(ControlEvent e) {
			setPopupLocation(fEditor.getViewer());
		}
	}

	private class PopupEnablementUpdater implements ISelectionChangedListener {
		private boolean fOldEnabled= true;
		
		public void selectionChanged(SelectionChangedEvent event) {
			boolean newEnabled= fRenameLinkedMode.isCaretInLinkedPosition();
			if (fOldEnabled == newEnabled)
				return;
			
			if (fPopup != null && !fPopup.isDisposed()) {
				for (Iterator iterator= fRefactorEntries.iterator(); iterator.hasNext();) {
					InfoEntry entry= (InfoEntry) iterator.next();
					entry.setEnabled(newEnabled);
				}
			}
			fOldEnabled= newEnabled;
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
			fLink= new DisableAwareHyperlink(table, SWT.NONE);
			fLink.setText(info);
			fLink.addHyperlinkListener(new HyperlinkAdapter() {
				public void linkActivated(HyperlinkEvent e) {
					//workaround for 157196: [Forms] Hyperlink listener notification throws AIOOBE when listener removed in callback
					e.display.asyncExec(runnable);
				}
			});
			hyperlinkGroup.add(fLink);
			
			fBindingLabel= new Label(table, SWT.NONE);
			fBindingLabel.setText(keybinding);
			addMoveSupport(fPopup, fBindingLabel);
		}
		
		public void setEnabled(boolean enabled) {
			fLink.setEnabled(enabled);
			fBindingLabel.setEnabled(enabled);
		}
		
		public Hyperlink getLink() {
			return fLink;
		}
	}
	
	private final CompilationUnitEditor fEditor;
	private final RenameLinkedMode fRenameLinkedMode;
	private final FocusEditingSupport fFocusEditingSupport;
	
	private Shell fPopup;
	private List/*<InfoEntry>*/ fRefactorEntries;
	
	
	public RenameInformationPopup(CompilationUnitEditor editor, RenameLinkedMode renameLinkedMode) {
		fEditor= editor;
		fRenameLinkedMode= renameLinkedMode;
		fFocusEditingSupport= new FocusEditingSupport();
	}

	public void open() {
		ISourceViewer viewer= fEditor.getViewer();
		
		if (viewer instanceof IEditingSupportRegistry) {
			IEditingSupportRegistry registry= (IEditingSupportRegistry) viewer;
			registry.register(fFocusEditingSupport);
		}
		
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
		setPopupLocation(viewer);
		
		addMoveSupport(fPopup, fPopup);
		new PopupVisibilityManager().start();
		fPopup.addShellListener(new ShellAdapter() {
			public void shellDeactivated(ShellEvent e) {
				final Shell editorShell= fEditor.getSite().getShell();
				display.asyncExec(new Runnable() {
					// post to UI thread since editor shell only gets activated after popup has lost focus
					public void run() {
						Shell activeShell= display.getActiveShell();
						if (activeShell != editorShell) {
//							fLinkedModeModel.exit(ILinkedModeListener.NONE);
							fRenameLinkedMode.cancel();
						}
					}
				});
			}
		});
		
		fPopup.setVisible(true);
		
		if (viewer instanceof IPostSelectionProvider) {
			final PopupEnablementUpdater updater= new PopupEnablementUpdater();
			final IPostSelectionProvider psp= (IPostSelectionProvider) viewer;
			psp.addPostSelectionChangedListener(updater);
			fPopup.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					psp.removePostSelectionChangedListener(updater);
				}
			});
		}
		
	}

	public void close() {
		if (fPopup != null) {
			if (!fPopup.isDisposed()) {
				fPopup.close();
			}
			fPopup= null;
		}
		ISourceViewer viewer= fEditor.getViewer();
		if (viewer instanceof IEditingSupportRegistry) {
			IEditingSupportRegistry registry= (IEditingSupportRegistry) viewer;
			registry.unregister(fFocusEditingSupport);
		}
		
	}
	
	private void setPopupLocation(ISourceViewer sourceViewer) {
		if (fPopup == null || fPopup.isDisposed())
			return;
		
		StyledText eWidget= sourceViewer.getTextWidget();
		Rectangle eBounds= eWidget.getClientArea();
		Point eLowerRight= eWidget.toDisplay(eBounds.x + eBounds.width, eBounds.y + eBounds.height);
		Point pSize= fPopup.getSize();
		/*
		 * possible improvement: try not to cover selection:
		 */
		fPopup.setLocation(eLowerRight.x - pSize.x - 5, eLowerRight.y - pSize.y - 5);
	}

	private static void addMoveSupport(final Shell shell, final Control control) {
		control.addMouseListener(new MouseAdapter() {
			private MouseMoveListener fMoveListener;

			public void mouseDown(final MouseEvent downEvent) {
				final Point location= shell.getLocation();
				fMoveListener= new MouseMoveListener() {
					public void mouseMove(MouseEvent moveEvent) {
						Point down= control.toDisplay(downEvent.x, downEvent.y);
						Point move= control.toDisplay(moveEvent.x, moveEvent.y);
						location.x= location.x + move.x - down.x;
						location.y= location.y + move.y - down.y;
						shell.setLocation(location);
					}
				};
				control.addMouseMoveListener(fMoveListener);
			}
			
			public void mouseUp(MouseEvent e) {
				control.removeMouseMoveListener(fMoveListener);
				fMoveListener= null;
			}
		});
	}
	
	private Control createTable(Composite parent) {
		final Display display= parent.getDisplay();
		Color editorForeground= fEditor.getViewer().getTextWidget().getForeground();
		
		Composite table= new Composite(parent, SWT.NONE);
		GridLayout tableLayout= new GridLayout(2, false);
		tableLayout.marginHeight= 5; 
		tableLayout.marginWidth= 5;
		tableLayout.horizontalSpacing= 10;
		table.setLayout(tableLayout);
		table.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, true));
		
		HyperlinkGroup refactorGroup= new HyperlinkGroup(display);
		refactorGroup.setForeground(editorForeground);
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
		refactorEntry.getLink().setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
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

		InfoEntry openDialogEntry= new InfoEntry(
				table,
				refactorGroup,
				ReorgMessages.RenameInformationPopup_open_dialog,
				new Runnable() {
					public void run() {
						fRenameLinkedMode.startFullDialog();
					}
				},
				getOpenDialogBinding());
		fRefactorEntries.add(openDialogEntry);
		
		HyperlinkGroup cancelGroup= new HyperlinkGroup(display);
		cancelGroup.setForeground(editorForeground);
		
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
		
		
		recursiveSetBackgroundColor(table, fEditor.getViewer().getTextWidget().getBackground());
		addMoveSupport(fPopup, table);
		
		Point size= table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		fPopup.setSize(size);
		
		return table;
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
}
