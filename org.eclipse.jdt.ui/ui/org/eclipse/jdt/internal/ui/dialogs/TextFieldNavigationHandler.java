/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.dialogs;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import org.eclipse.jdt.internal.ui.text.JavaWordIterator;

/**
 * This class is not to be used yet. We have to sort out some pending SWT bugs first.
 * The class will finally fix bug 64665 (JDT controls with symbol names should be camel-case aware [refactoring]).
 */
public class TextFieldNavigationHandler {
	
	public static void install(Text text) {
		new FocusHandler(new TextNavigable(text));
	}
	
	public static void install(Combo combo) {
		new FocusHandler(new ComboNavigable(combo));
	}
	
	private abstract static class Navigable {
		
		Point fLastSelection;
		int fCaretPosition;
		
		public abstract Control getControl();
		
		public abstract CharSequence getText();

		public abstract Point getSelection();

		public abstract int getCaretPosition();

		public abstract void setSelection(int start, int end);
		
		void selectionChanged() {
			Point selection= getSelection();
//			System.out.println("TextFieldNavigationHandler.selectionChanged():" + selection);
			if (selection.equals(fLastSelection)) {
				// leave caret position
			} else if (selection.x == selection.y) { //empty range
				fCaretPosition= selection.x;
			} else if (fLastSelection.y == selection.y) {
				fCaretPosition= selection.x; //same end -> assume caret at start
			} else {
				fCaretPosition= selection.y;
			}
			fLastSelection= selection;
		}
	}
	
	private static class TextNavigable extends Navigable {
		static final boolean BUG_106024_TEXT_SELECTION= true; //TODO: platform-dependent
		
		private final Text fText;
		
		public TextNavigable(Text text) {
			fText= text;
			//workaround for bug 106024 (Text#setSelection(int, int) does not handle start > end with SWT.SINGLE):
			if (BUG_106024_TEXT_SELECTION) {
				fLastSelection= getSelection();
				fCaretPosition= fLastSelection.y;
				fText.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent e) {
						selectionChanged();
					}
				});
				fText.addMouseListener(new MouseAdapter() {
					public void mouseUp(MouseEvent e) {
						selectionChanged();
					}
				});
			}
		}
		
		public Control getControl() {
			return fText;
		}

		public CharSequence getText() {
			return fText.getText();
		}

		public Point getSelection() {
			return fText.getSelection();
		}

		public int getCaretPosition() {
			if (BUG_106024_TEXT_SELECTION) {
				selectionChanged();
				return fCaretPosition;
			} else {
				return fText.getCaretPosition();
			}
		}

		public void setSelection(int start, int end) {
			fText.setSelection(start, end);
		}
	}
	
	private static class ComboNavigable extends Navigable {
		private final Combo fCombo;
		
		public ComboNavigable(Combo combo) {
			fCombo= combo;
			//workaround for bug xxx (no API):
			fLastSelection= getSelection();
			fCaretPosition= fLastSelection.y;
			fCombo.addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent e) {
					selectionChanged();
				}
			});
			fCombo.addMouseListener(new MouseAdapter() {
				public void mouseUp(MouseEvent e) {
					selectionChanged();
				}
			});
		}
		
		public Control getControl() {
			return fCombo;
		}

		public CharSequence getText() {
			return fCombo.getText();
		}
		
		public Point getSelection() {
			return fCombo.getSelection();
		}
		
		public int getCaretPosition() {
			selectionChanged();
			return fCaretPosition;
//			return fCombo.getCaretPosition(); //TODO: not available: bug 103630 (Add API: Combo#getCaretPosition())
		}
		
		public void setSelection(int start, int end) {
			fCombo.setSelection(new Point(start, end));
		}
	}
	
	private static class FocusHandler implements FocusListener {
	
		private final JavaWordIterator fIterator;
		private List fHandlerActivations;
		private IContextActivation fContextActivation;
		protected final Navigable fNavigable;
		
		private FocusHandler(Navigable navigable) {
			fIterator= new JavaWordIterator();
			fNavigable= navigable;
			
			Control control= navigable.getControl();
			control.addFocusListener(this);
			if (control.isFocusControl())
				activate();
			control.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					deactivate();
				}
			});
		}

		public void focusGained(FocusEvent e) {
			activate();
		}

		public void focusLost(FocusEvent e) {
			deactivate();
		}

		private void activate() {
			if (fContextActivation != null)
				return;
			IContextService contextService= (IContextService)PlatformUI.getWorkbench().getAdapter(IContextService.class);
			if (contextService == null)
				return;
//			fContextActivation= contextService.activateContext("org.eclipse.ui.textEditorScope"); // does not work, since not a dialog context?
			fContextActivation= contextService.activateContext(IContextService.CONTEXT_ID_WINDOW); // works, but why?
			
			if (fHandlerActivations != null)
				return;
			IHandlerService handlerService= (IHandlerService)PlatformUI.getWorkbench().getAdapter(IHandlerService.class);
			if (handlerService == null)
				return;
			fHandlerActivations= new ArrayList();
			
			//TODO: DELETE_PREVIOUS/NEXT_WORD
			fHandlerActivations.add(handlerService.activateHandler(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT, new AbstractHandler() {
				public Object execute(ExecutionEvent event) throws ExecutionException {
					fIterator.setText(fNavigable.getText());
					int caretPosition= fNavigable.getCaretPosition();
					int newCaret= fIterator.following(caretPosition);
					if (newCaret != BreakIterator.DONE) {
						Point selection= fNavigable.getSelection();
						if (caretPosition == selection.y)
							fNavigable.setSelection(selection.x, newCaret);
						else
							fNavigable.setSelection(selection.y, newCaret);
					}
					fIterator.setText(""); //$NON-NLS-1$
					return null;
				}
			}));
			fHandlerActivations.add(handlerService.activateHandler(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS, new AbstractHandler() {
				public Object execute(ExecutionEvent event) throws ExecutionException {
					fIterator.setText(fNavigable.getText());
					int caretPosition= fNavigable.getCaretPosition();
					int newCaret= fIterator.preceding(caretPosition);
					if (newCaret != BreakIterator.DONE) {
						Point selection= fNavigable.getSelection();
						if (caretPosition == selection.x)
							fNavigable.setSelection(selection.y, newCaret);
						else
							fNavigable.setSelection(selection.x, newCaret);
					}
					fIterator.setText(""); //$NON-NLS-1$
					return null;
				}
			}));
			fHandlerActivations.add(handlerService.activateHandler(ITextEditorActionDefinitionIds.WORD_NEXT, new AbstractHandler() {
				public Object execute(ExecutionEvent event) throws ExecutionException {
					fIterator.setText(fNavigable.getText());
					int caretPosition= fNavigable.getCaretPosition();
					int newCaret= fIterator.following(caretPosition);
					fNavigable.setSelection(newCaret, newCaret);
					fIterator.setText(""); //$NON-NLS-1$
					return null;
				}
			}));
			fHandlerActivations.add(handlerService.activateHandler(ITextEditorActionDefinitionIds.WORD_PREVIOUS, new AbstractHandler() {
				public Object execute(ExecutionEvent event) throws ExecutionException {
					fIterator.setText(fNavigable.getText());
					int caretPosition= fNavigable.getCaretPosition();
					int newCaret= fIterator.preceding(caretPosition);
					fNavigable.setSelection(newCaret, newCaret);
					fIterator.setText(""); //$NON-NLS-1$
					return null;
				}
			}));
			
		}
		
		private void deactivate() {
			if (fContextActivation == null)
				return;
			IContextService contextService= (IContextService)PlatformUI.getWorkbench().getAdapter(IContextService.class);
			if (contextService == null)
				return;
			contextService.deactivateContext(fContextActivation);
			fContextActivation= null;
			
			if (fHandlerActivations == null)
				return;
			IHandlerService handlerService= (IHandlerService)PlatformUI.getWorkbench().getAdapter(IHandlerService.class);
			if (handlerService == null)
				return;
			handlerService.deactivateHandlers(fHandlerActivations);
			fHandlerActivations= null;
		}
	}
}
