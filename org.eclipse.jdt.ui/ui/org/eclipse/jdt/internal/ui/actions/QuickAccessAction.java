/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommand;
import org.eclipse.ui.commands.ICommandManager;
import org.eclipse.ui.commands.IKeySequenceBinding;
import org.eclipse.ui.contexts.IWorkbenchContextSupport;
import org.eclipse.ui.internal.IPreferenceConstants;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.keys.KeyStroke;
import org.eclipse.ui.keys.SWTKeySupport;
import org.eclipse.ui.keys.SpecialKey;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;


public abstract class QuickAccessAction extends Action {
	
	private CompilationUnitEditor fEditor;
	private IWorkbenchPartSite fSite;
	
	private String fCommandId = null;
	private List fKeySequenceBindings = null;
	
	private IAction fAction;

	public QuickAccessAction(String commandID, CompilationUnitEditor editor) {
		fCommandId= commandID;
		fEditor= editor;
		fSite= editor.getSite();
	}

	public void run() {
		openDialog();
	}
	
	/*
	 * Open a dialog showing all views in the activation order
	 */
	private void openDialog() {
		final int MAX_ITEMS = 22;
		fAction= null;

		final Shell dialog = new Shell(fSite.getShell(), SWT.NO_FOCUS | SWT.ON_TOP);
		Display display = dialog.getDisplay();
		dialog.setLayout(new FillLayout());

		final Table table = new Table(dialog, SWT.SINGLE | SWT.FULL_SELECTION); // | SWT.NO_FOCUS | SWT.NO_REDRAW_RESIZE | SWT.NO_TRIM);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		TableColumn tc = new TableColumn(table, SWT.NO_FOCUS);
		tc.setResizable(false);
		tc.setText(getTableHeader());
		addItems(table);
		int tableItemCount = table.getItemCount();

		switch (tableItemCount) {
			case 0 :
				// do nothing;
				break;
			default :
				table.setSelection(0);
				break;
		}

		tc.pack();
		table.pack();
		Rectangle tableBounds = table.getBounds();
		tableBounds.height = Math.min(tableBounds.height, table.getItemHeight() * MAX_ITEMS);
		table.setBounds(tableBounds);
		dialog.pack();

		tc.setWidth(table.getClientArea().width);
		table.setFocus();
		table.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				// Do nothing
			}

			public void focusLost(FocusEvent e) {
				cancel(dialog);
			}
		});

		Point cursorLocation= fEditor.getViewer().getTextWidget().toDisplay(computeWordStart());
		Rectangle displayBounds= display.getBounds();
		Rectangle dialogBounds = dialog.getBounds();
		dialogBounds.height = dialogBounds.height + 3 - table.getHorizontalBar().getSize().y;

		dialogBounds.width= Math.min(dialogBounds.width, displayBounds.width);
		dialogBounds.height= Math.min(dialogBounds.height, displayBounds.height);
		
		dialogBounds.x = cursorLocation.x; // Math.max(cursorLocation.x - dialogBounds.width / 2, 0);
		dialogBounds.y = cursorLocation.y; // Math.max(cursorLocation.y - table.getItemHeight() / 2, 0);
		
		if (dialogBounds.x + dialogBounds.width > displayBounds.width)
			dialogBounds.x= displayBounds.width - dialogBounds.width;
		if (dialogBounds.y + dialogBounds.height > displayBounds.height)
			dialogBounds.y= displayBounds.height - dialogBounds.height; 
		
		
		dialog.setBounds(dialogBounds);

		// table.removeHelpListener(getHelpListener());
		table.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent event) {
				// Do nothing
			}
		});
		
		/* Fetch the key bindings for the forward and backward commands.  They will not
		 * change while the dialog is open, but the context will.  Bug 55581.
		 */
		final ICommandManager commandManager = PlatformUI.getWorkbench().getCommandSupport().getCommandManager();
		final ICommand command = commandManager.getCommand(fCommandId);
		if (command.isDefined()) {
			fKeySequenceBindings = command.getKeySequenceBindings();
		}

		final IWorkbenchContextSupport contextSupport = fSite.getWorkbenchWindow().getWorkbench().getContextSupport();
		try {
			dialog.open();
			addMouseListener(table, dialog);
			contextSupport.registerShell(dialog, IWorkbenchContextSupport.TYPE_NONE);
			addKeyListener(table, dialog);
			addTraverseListener(table);

			while (!dialog.isDisposed())
				if (!display.readAndDispatch())
					display.sleep();
		} finally {
			if (!dialog.isDisposed())
				cancel(dialog);
			contextSupport.unregisterShell(dialog);
			fKeySequenceBindings = null;
		}
		if (fAction != null) 
			fAction.run();
	}
	
	protected abstract void addItems(Table table);

	protected abstract String getTableHeader();
	
	private void addKeyListener(final Table table, final Shell dialog) {
		table.addKeyListener(new KeyListener() {
			private boolean firstKey = true;
			private boolean quickReleaseMode = false;

			public void keyPressed(KeyEvent e) {
				int keyCode = e.keyCode;
				char character = e.character;
				int accelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
				KeyStroke keyStroke = SWTKeySupport.convertAcceleratorToKeyStroke(accelerator);

				boolean acceleratorForward = false;
				boolean acceleratorBackward = false;

				if (fCommandId != null && fKeySequenceBindings != null) {
			        Iterator iterator = fKeySequenceBindings.iterator();
					outer: while (iterator.hasNext()) {
						IKeySequenceBinding keySequenceBinding= (IKeySequenceBinding)iterator.next();
						List keyStrokes = keySequenceBinding.getKeySequence().getKeyStrokes();
						if (keyStrokes.isEmpty())
							continue;
						KeyStroke lastKey= (KeyStroke)keyStrokes.get(keyStrokes.size() - 1);
						if (keyStroke.equals(lastKey)) {
							acceleratorForward= true;
							break;
						}
						Set pressedModifiers= keyStroke.getModifierKeys();
						Set expectedModifiers= lastKey.getModifierKeys();
						if (pressedModifiers.size() == expectedModifiers.size()) {
							for (Iterator iter= pressedModifiers.iterator(); iter.hasNext();) {
								if (!expectedModifiers.contains(iter.next()))
									break outer;
							}
							if (SpecialKey.ARROW_DOWN.equals(keyStroke.getNaturalKey())) {
								acceleratorForward= true;
								break;
							} else if (SpecialKey.ARROW_UP.equals(keyStroke.getNaturalKey())) {
								acceleratorBackward= true;
								break;
							}
						}
					}
				}

				if (character == SWT.CR || character == SWT.LF) {
					ok(dialog, table);
				} else if (acceleratorForward) {
					if (firstKey && e.stateMask != 0)
						quickReleaseMode = true;

					int index = table.getSelectionIndex();
					table.setSelection((index + 1) % table.getItemCount());
				} else if (acceleratorBackward) { 
					if (firstKey && e.stateMask != 0)
						quickReleaseMode = true;

					int index = table.getSelectionIndex();
					table.setSelection(index >= 1 ? index - 1 : table.getItemCount() - 1);
				} else if (
					keyCode != SWT.ALT
						&& keyCode != SWT.COMMAND
						&& keyCode != SWT.CTRL
						&& keyCode != SWT.SHIFT
						&& keyCode != SWT.ARROW_DOWN
						&& keyCode != SWT.ARROW_UP
						&& keyCode != SWT.ARROW_LEFT
						&& keyCode != SWT.ARROW_RIGHT)
					cancel(dialog);

				firstKey = false;
			}

			public void keyReleased(KeyEvent e) {
				int keyCode = e.keyCode;
				int stateMask = e.stateMask;
				//char character = e.character;
				//int accelerator = stateMask | (keyCode != 0 ? keyCode :
				// convertCharacter(character));

				//System.out.println("\nRELEASED");
				//printKeyEvent(e);
				//System.out.println("accelerat:\t" + accelerator + "\t (" +
				// KeySupport.formatStroke(Stroke.create(accelerator), true) +
				// ")");

				final IPreferenceStore store = WorkbenchPlugin.getDefault().getPreferenceStore();
				final boolean stickyCycle = store.getBoolean(IPreferenceConstants.STICKY_CYCLE);
				if ((!stickyCycle && (firstKey || quickReleaseMode)) && keyCode == stateMask)
					ok(dialog, table);
			}
		});
	}

	private void addMouseListener(final Table table, final Shell dialog) {
		table.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {
				ok(dialog, table);
			}

			public void mouseDown(MouseEvent e) {
				ok(dialog, table);
			}

			public void mouseUp(MouseEvent e) {
				ok(dialog, table);
			}
		});
	}
	
	private final void addTraverseListener(final Table table) {
		table.addTraverseListener(new TraverseListener() {
			public final void keyTraversed(final TraverseEvent event) {
				event.doit = false;
			}
		});
	}

	private void cancel(Shell dialog) {
		dialog.close();
	}

	private void ok(Shell dialog, final Table table) {
		TableItem[] items = table.getSelection();
		if (items != null && items.length == 1) {
			fAction= (IAction)items[0].getData();
		}
		dialog.close();
	}

	
	/**
	 * Determines graphical area covered by the given text region.
	 *
	 * @param region the region whose graphical extend must be computed
	 * @return the graphical extend of the given region
	 */
	private Point computeWordStart() {
		
		ITextSelection selection= (ITextSelection)fEditor.getSelectionProvider().getSelection();
		IRegion textRegion= JavaWordFinder.findWord(fEditor.getViewer().getDocument(), selection.getOffset());
				
		IRegion widgetRegion= modelRange2WidgetRange(textRegion);
		int start= widgetRegion.getOffset();
				
		StyledText styledText= fEditor.getViewer().getTextWidget();
		Point result= styledText.getLocationAtOffset(start);
		result.y+= styledText.getLineHeight();
		
		return result;
	}
	
	/**
	 * Translates a given region of the text viewer's document into
	 * the corresponding region of the viewer's widget.
	 * 
	 * @param region the document region
	 * @return the corresponding widget region
	 * @since 2.1
	 */
	private IRegion modelRange2WidgetRange(IRegion region) {
		ISourceViewer viewer= fEditor.getViewer();
		if (viewer instanceof ITextViewerExtension5) {
			ITextViewerExtension5 extension= (ITextViewerExtension5)viewer;
			return extension.modelRange2WidgetRange(region);
		}
		
		IRegion visibleRegion= viewer.getVisibleRegion();
		int start= region.getOffset() - visibleRegion.getOffset();
		int end= start + region.getLength();
		if (end > visibleRegion.getLength())
			end= visibleRegion.getLength();
			
		return new Region(start, end - start);
	}	
}
