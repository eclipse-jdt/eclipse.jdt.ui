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
package org.eclipse.jdt.internal.ui.text.link;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.ITextViewerExtension3;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;
import org.eclipse.jdt.internal.ui.text.link.contentassist.ContentAssistant2;

/**
 * The UI for linked mode. Detects events that influence behaviour of the
 * linked position UI and acts upon them.
 * 
 * @since 3.0
 */
public class LinkedUIControl {

	/* cycle constants */
	/**
	 * Constant indicating that this UI should never cycle from the last
	 * position to the first and vice versa.
	 */
	public static final int CYCLE_NEVER= 0;
	/**
	 * Constant indicating that this UI should always cycle from the last
	 * position to the first and vice versa.
	 */
	public static final int CYCLE_ALWAYS= 1;
	/**
	 * Constant indicating that this UI should cycle from the last position to
	 * the first and vice versa if its environment is not nested.
	 */
	public static final int CYCLE_WHEN_NO_PARENT= 2;

	/**
	 * Listens on a styled text for events before (Verify) and after (Modify)
	 * modifications. Used to update the caret after linked changes.
	 */
	private final class CaretListener implements ModifyListener, VerifyListener {
		/*
		 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
		 */
		public void modifyText(ModifyEvent e) {
			updateSelection(e);
		}

		/*
		 * @see org.eclipse.swt.events.VerifyListener#verifyText(org.eclipse.swt.events.VerifyEvent)
		 */
		public void verifyText(VerifyEvent e) {
			rememberSelection(e);
		}
	}

	/**
	 * A link target consists of a viewer and gets notified if the linked UI on
	 * it is being shown.
	 * 
	 * @since 3.0
	 */
	public static abstract class LinkedUITarget {
		/**
		 * Returns the viewer represented by this target, never <code>null</code>.
		 * 
		 * @return the viewer associated with this target.
		 */
		abstract ITextViewer getViewer();
		/**
		 * Called by the linked UI when this target is being shown. An
		 * implementation could for example ensure that the corresponding
		 * editor is showing.
		 */
		abstract void enter();
		
		/**
		 * The viewer's text widget is initialized when the UI first connects
		 * to the viewer and never changed thereafter. This is to keep the
		 * reference of the widget that we have registered our listeners with,
		 * as the viewer, when it gets disposed, does not remember it, resulting
		 * in a situation where we cannot uninstall the listeners and a memory leak.
		 */
		StyledText fWidget;
		
		/** The cached shell - same reason as fWidget. */
		Shell fShell;
	}

	private static final class EmptyTarget extends LinkedUITarget {

		private ITextViewer fTextViewer;

		public EmptyTarget(ITextViewer viewer) {
			Assert.isNotNull(viewer);
			fTextViewer= viewer;
		}
		/*
		 * @see org.eclipse.jdt.internal.ui.text.link2.LinkedUIControl.ILinkedUITarget#getViewer()
		 */
		public ITextViewer getViewer() {
			return fTextViewer;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.link2.LinkedUIControl.ILinkedUITarget#enter()
		 */
		public void enter() {
		}
	}

	/**
	 * An <code>ILinkedUITarget</code> with an associated editor, which is 
	 * brought to the top when a linked position in its viewer is jumped to.
	 * 
	 * @since 3.0
	 */
	public static class EditorTarget extends LinkedUITarget {

		/** The text viewer. */
		protected final ITextViewer fTextViewer;
		/** The editor displaying the viewer. */
		protected final ITextEditor fTextEditor;

		/**
		 * Creates a new instance.
		 * 
		 * @param viewer the viewer
		 * @param editor the editor displaying <code>viewer</code>, or <code>null</code>
		 */
		public EditorTarget(ITextViewer viewer, ITextEditor editor) {
			Assert.isNotNull(viewer);
			fTextViewer= viewer;
			fTextEditor= editor;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.link2.LinkedUIControl.ILinkedUITarget#getViewer()
		 */
		public ITextViewer getViewer() {
			return fTextViewer;
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.link2.LinkedUIControl.ILinkedUITarget#enter()
		 */
		public void enter() {
			if (fTextEditor != null)
				return;

			IWorkbenchPage page= fTextEditor.getEditorSite().getPage();
			if (page != null) {
				page.bringToTop(fTextEditor);
			}
			fTextEditor.setFocus();
		}

	}

	/**
	 * Listens for state changes in the model.
	 */
	private final class ExitListener implements ILinkedListener {
		/*
		 * @see org.eclipse.jdt.internal.ui.text.link2.LinkedEnvironment.ILinkedListener#left(org.eclipse.jdt.internal.ui.text.link2.LinkedEnvironment, int)
		 */
		public void left(LinkedEnvironment environment, int flags) {
			leave(ILinkedListener.EXIT_ALL | flags);
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.link2.LinkedEnvironment.ILinkedListener#suspend(org.eclipse.jdt.internal.ui.text.link2.LinkedEnvironment)
		 */
		public void suspend(LinkedEnvironment environment) {
			disconnect();
			redraw();
		}

		/*
		 * @see org.eclipse.jdt.internal.ui.text.link2.LinkedEnvironment.ILinkedListener#resume(org.eclipse.jdt.internal.ui.text.link2.LinkedEnvironment)
		 */
		public void resume(LinkedEnvironment environment, int flags) {
			if ((flags & ILinkedListener.EXIT_ALL) != 0) {
				leave(flags);
			} else {
				connect();
				if ((flags & ILinkedListener.SELECT) != 0)
					select();
				redraw();
			}
		}
	}

	/**
	 * Exit flags returned if a custom exit policy wants to exit linked mode.
	 */
	public static class ExitFlags {
		/** The flags to return in the <code>leave</code> method. */
		public int flags;
		/** The doit flag of the checked <code>VerifyKeyEvent</code>. */
		public boolean doit;
		/**
		 * Creates a new instance.
		 * 
		 * @param flags the exit flags
		 * @param doit the doit flag for the verify event
		 */
		public ExitFlags(int flags, boolean doit) {
			this.flags= flags;
			this.doit= doit;
		}
	}

	/**
	 * An exit policy can be registered by a caller to get custom exit
	 * behaviour.
	 */
	public interface IExitPolicy {
		/**
		 * Checks whether the linked mode should be left after receiving the
		 * given <code>VerifyEvent</code> and selection.
		 * 
		 * @param environment the linked environment
		 * @param event the verify event
		 * @param offset the offset of the current selection
		 * @param length the length of the current selection
		 * @return valid exit flags or <code>null</code> if no special action
		 *         should be taken
		 */
		ExitFlags doExit(LinkedEnvironment environment, VerifyEvent event, int offset, int length);
	}

	/**
	 * A NullObject implementation of <code>IExitPolicy</code>.
	 */
	private static class NullExitPolicy implements IExitPolicy {
		/*
		 * @see org.eclipse.jdt.internal.ui.text.link2.LinkedUIControl.IExitPolicy#doExit(org.eclipse.swt.events.VerifyEvent, int, int)
		 */
		public ExitFlags doExit(LinkedEnvironment environment, VerifyEvent event, int offset, int length) {
			return null;
		}
	}

	/**
	 * Listens for shell events and acts upon them.
	 */
	private class LinkedUICloser implements ShellListener {

		public void shellActivated(ShellEvent e) {
		}

		public void shellClosed(ShellEvent e) {
			leave(ILinkedListener.EXIT_ALL);
		}

		public void shellDeactivated(ShellEvent e) {
// 			TODO reenable after debugging 
//			if (true) return;
			
			// from LinkedPositionUI:
			
			// don't deactivate on focus lost, since the proposal popups may take focus
			// plus: it doesn't hurt if you can check with another window without losing linked mode
			// since there is no intrusive popup sticking out.
			
			// need to check first what happens on reentering based on an open action
			// Seems to be no problem
			
			// Better:
			// Check with content assistant and only leave if its not the proposal shell that took the 
			// focus away.
			
			StyledText text;
			Display display;

			if (fAssistant == null || fCurrentTarget == null || (text= fCurrentTarget.fWidget) == null 
					|| (display= text.getDisplay()) == null || display.isDisposed()) {
				leave(ILinkedListener.EXIT_ALL);
			} else {
				// Post in UI thread since the assistant popup will only get the focus after we lose it.
				display.asyncExec(new Runnable() {
					public void run() {
						if (fIsActive && (fAssistant == null || !fAssistant.hasFocus()))  {
							leave(ILinkedListener.EXIT_ALL);
						}
					}
				});
			}
		}

		public void shellDeiconified(ShellEvent e) {
		}

		public void shellIconified(ShellEvent e) {
			leave(ILinkedListener.EXIT_ALL);
		}

	}

	/**
	 * Listens for key events, checks the exit policy for custom exit
	 * strategies but defaults to handling Tab, Enter, and Escape.
	 */
	private class LinkedUIKeyListener implements VerifyKeyListener {

		public void verifyKey(VerifyEvent event) {

			if (!event.doit)
				return;

			Point selection= fCurrentTarget.getViewer().getSelectedRange();
			int offset= selection.x;
			int length= selection.y;

			// if the custom exit policy returns anything, use that
			ExitFlags exitFlags= fExitPolicy.doExit(fEnvironment, event, offset, length);
			if (exitFlags != null) {
				leave(exitFlags.flags);
				event.doit= exitFlags.doit;
				return;
			}

			// standard behaviour:
			// (Shift+)Tab:	jumps from position to position, depending on cycle mode
			// Enter:		accepts all entries and leaves all (possibly stacked) environments, the last sets the caret
			// Esc:			accepts all entries and leaves all (possibly stacked) environments, the caret stays
			// ? what do we do to leave one level of a cycling environment that is stacked?
			// -> This is only the case if the level was set up with forced cycling (CYCLE_ALWAYS), in which case
			// the caller is sure that one does not need by-level exiting.
			switch (event.character) {
				// [SHIFT-]TAB = hop between edit boxes
				case 0x09:
					if (!(fExitPosition != null && fExitPosition.includes(offset)) && !fEnvironment.anyPositionContains(offset)) {
						// outside any edit box -> leave (all? TODO should only leave the affected, level and forward to the next upper)
						leave(ILinkedListener.EXIT_ALL);
						break;
					} else {
						if (event.stateMask == SWT.SHIFT)
							previous();
						else
							next();
					}

					event.doit= false;
					break;

				// ENTER
				case 0x0A:
				// Ctrl+Enter on WinXP
				case 0x0D:
					if (fAssistant != null && fAssistant.wasProposalChosen()) {
						// don't exit as it was really just the proposal that was chosen
						next();
						event.doit= false;
						break;
					} else if (!(fExitPosition != null && fExitPosition.includes(offset)) && !fEnvironment.anyPositionContains(offset)) {
						// outside any edit box -> leave (all? TODO should only leave the affected, level and forward to the next upper)
						leave(ILinkedListener.EXIT_ALL);
						break;
					} else {
						// normal case: exit entire stack and put caret to final position
						leave(ILinkedListener.EXIT_ALL | ILinkedListener.UPDATE_CARET);
						event.doit= false;
						break;
					}

				// ESC
				case 0x1B:
					// exit entire stack and leave caret
					leave(ILinkedListener.EXIT_ALL);
					event.doit= false;
					break;

				default:
					if (event.character != 0) {
						if (!controlUndoBehavior(offset, length)) {
							leave(ILinkedListener.EXIT_ALL);
							break;
						}
					}
			}
		}

		private boolean controlUndoBehavior(int offset, int length) {
			LinkedPosition position= fEnvironment.findPosition(new LinkedPosition(fCurrentTarget.getViewer().getDocument(), offset, length, LinkedPositionGroup.NO_STOP));
			if (position != null) {

				ITextViewerExtension extension= (ITextViewerExtension) fCurrentTarget.getViewer();
				IRewriteTarget target= extension.getRewriteTarget();

				if (fPreviousPosition != null && !fPreviousPosition.equals(position))
					target.endCompoundChange();
				target.beginCompoundChange();
			}

			fPreviousPosition= position;
			return fPreviousPosition != null;
		}

	}

	/**
	 * Installed as post selection listener on the watched viewer. Updates the
	 * linked position after cursor movement, even to positions not in the
	 * iteration list.
	 */
	private class MySelectionListener implements ISelectionChangedListener {

		/*
		 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
		 */
		public void selectionChanged(SelectionChangedEvent event) {
			ISelection selection= event.getSelection();
			if (selection instanceof ITextSelection) {
				ITextSelection textsel= (ITextSelection) selection;
				if (event.getSelectionProvider() instanceof ITextViewer) {
					IDocument doc= ((ITextViewer) event.getSelectionProvider()).getDocument();
					if (doc != null) {
						int offset= textsel.getOffset();
						int length= textsel.getLength();
						if (offset >= 0 && length >= 0) {
							LinkedPosition find= new LinkedPosition(doc, offset, length, LinkedPositionGroup.NO_STOP);
							LinkedPosition pos= fEnvironment.findPosition(find);
							if (pos != null)
								switchPosition(pos, false);
						}
					}
				}
			}
		}

	}

	/** The current viewer. */
	private LinkedUITarget fCurrentTarget;
	/** The manager of the linked positions we provide a UI for. */
	private LinkedEnvironment fEnvironment;
	/** The set of viewers we manage. */
	private LinkedUITarget[] fTargets;
	/** The iterator over the tab stop positions. */
	private TabStopIterator fIterator;

	/* Our team of event listeners */
	/** The shell listener. */
	private LinkedUICloser fCloser= new LinkedUICloser();
	/** The key listener. */
	private LinkedUIKeyListener fKeyListener= new LinkedUIKeyListener();
	/** The linked listener. */
	private ILinkedListener fLinkedListener= new ExitListener();
	/** The selection listener. */
	private MySelectionListener fSelectionListener= new MySelectionListener();
	/** The styled text listener. */
	private CaretListener fCaretListener= new CaretListener();
	/** The last caret position, used by fCaretListener. */
	private final Position fCaretPosition= new Position(0, 0);

	/** The painter for the underline of the current position. */
	private LinkedUIPainter fPainter;

	/** The exit policy to control custom exit behaviour */
	private IExitPolicy fExitPolicy= new NullExitPolicy();

	/** The current frame position shown in the UI, or <code>null</code>. */
	private LinkedPosition fFramePosition;

	/** The last visisted position, used for undo / redo. */
	private LinkedPosition fPreviousPosition;

	/** The content assistant used to show proposals. */
	private ContentAssistant2 fAssistant;

	/** The exit position. */
	private LinkedPosition fExitPosition;

	/** State indicator to prevent multiple invocation of leave. */
	private boolean fIsActive= false;
	private IPositionUpdater fPositionUpdater= new DefaultPositionUpdater(getCategory());

	/**
	 * Creates a new UI on the given model (environment) and the set of
	 * viewers. The environment must provide a tab stop sequence with a
	 * non-empty list of tab stops.
	 * 
	 * @param environment the linked position model
	 * @param targets the non-empty list of targets upon which the linked ui
	 *        should act
	 */
	public LinkedUIControl(LinkedEnvironment environment, LinkedUITarget[] targets) {
		constructor(environment, targets);
	}

	/**
	 * Conveniance ctor for just one viewer.
	 * 
	 * @param environment the linked position model
	 * @param viewer the viewer upon which the linked ui
	 *        should act
	 */
	public LinkedUIControl(LinkedEnvironment environment, ITextViewer viewer) {
		constructor(environment, new LinkedUITarget[]{new EmptyTarget(viewer)});
	}

	/**
	 * Conveniance ctor for multiple viewers.
	 * 
	 * @param environment the linked position model
	 * @param viewers the non-empty list of viewers upon which the linked ui
	 *        should act
	 */
	public LinkedUIControl(LinkedEnvironment environment, ITextViewer[] viewers) {
		LinkedUITarget[] array= new LinkedUITarget[viewers.length];
		for (int i= 0; i < array.length; i++) {
			array[i]= new EmptyTarget(viewers[i]);
		}
		constructor(environment, array);
	}

	/**
	 * Conveniance ctor for one target.
	 * 
	 * @param environment the linked position model
	 * @param target the target upon which the linked ui
	 *        should act
	 */
	public LinkedUIControl(LinkedEnvironment environment, LinkedUITarget target) {
		constructor(environment, new LinkedUITarget[]{target});
	}

	/**
	 * This does the actual constructor work.
	 * 
	 * @param environment the linked position model
	 * @param targets the non-empty array of targets upon which the linked ui
	 *        should act
	 */
	private void constructor(LinkedEnvironment environment, LinkedUITarget[] targets) {
		Assert.isNotNull(environment);
		Assert.isNotNull(targets);
		Assert.isTrue(targets.length > 0);
		Assert.isTrue(environment.getTabStopSequence().size() > 0);

		fEnvironment= environment;
		fTargets= targets;
		fCurrentTarget= targets[0];
		fIterator= new TabStopIterator(fEnvironment.getTabStopSequence());
		fIterator.setCycling(!fEnvironment.isNested());
		fEnvironment.addLinkedListener(fLinkedListener);

		fPainter= new LinkedUIPainter(fCurrentTarget.getViewer());
		fPainter.setColor(getFrameColor());

		fAssistant= new ContentAssistant2();
		fAssistant.setDocumentPartitioning(IJavaPartitions.JAVA_PARTITIONING);

		fCaretPosition.delete();
	}

	/**
	 * Starts this UI on the first position.
	 */
	public void enter() {
		fIsActive= true;
		connect();
		next();
	}

	/**
	 * Sets an <code>IExitPolicy</code> to customize the exit behaviour of
	 * this linked UI.
	 * 
	 * @param policy the exit policy to use.
	 */
	public void setExitPolicy(IExitPolicy policy) {
		fExitPolicy= policy;
	}

	public void setExitPosition(LinkedUITarget target, int offset, int length, boolean isTabStop) throws BadLocationException {
		// remove any existing exit position
		if (fExitPosition != null) {
			fExitPosition.getDocument().removePosition(fExitPosition);
			fIterator.removePosition(fExitPosition);
			fExitPosition= null;
		}

		IDocument doc= target.getViewer().getDocument();
		if (doc == null)
			return;

		// if we are a tabstop, we are the last one -> MAX_VALUE
		fExitPosition= new LinkedPosition(doc, offset, length, isTabStop ? Integer.MAX_VALUE : LinkedPositionGroup.NO_STOP);
		doc.addPosition(fExitPosition); // gets removed in leave()
		if (isTabStop)
			fIterator.addPosition(fExitPosition);

	}

	/**
	 * Sets the exit position to move the caret to when linked mode is exited.
	 * 
	 * @param viewer the viewer where the exit position is located
	 * @param offset the offset of the exit position
	 * @param length the length of the exit position (in case there should be a
	 *        selection)
	 * @param isTabStop set to <code>true</code> if there should be a tab
	 *        stop a the exit position (important when cycling is on)
	 * @throws BadLocationException if the position is not valid in the
	 *         viewer's document
	 */
	public void setExitPosition(ITextViewer viewer, int offset, int length, boolean isTabStop) throws BadLocationException {
		setExitPosition(new EditorTarget(viewer, null), offset, length, isTabStop);
	}

	/**
	 * Sets the cycling mode to either of <code>CYCLING_ALWAYS</code>,
	 * <code>CYCLING_NEVER</code>, or <code>CYCLING_WHEN_NO_PARENT</code>,
	 * which is the default.
	 * 
	 * @param mode the new cycling mode.
	 */
	public void setCyclingMode(int mode) {
		if (mode == CYCLE_ALWAYS || mode == CYCLE_WHEN_NO_PARENT && !fEnvironment.isNested())
			fIterator.setCycling(true);
		else
			fIterator.setCycling(false);
	}

	void next() {
		if (fIterator.hasNext(fFramePosition)) {
			switchPosition(fIterator.next(fFramePosition), true);
			return;
		} else
			leave(ILinkedListener.UPDATE_CARET);
	}

	void previous() {
		if (fIterator.hasPrevious(fFramePosition)) {
			switchPosition(fIterator.previous(fFramePosition), true);
		} else
			// dont't update caret, but rather select the current frame
			leave(ILinkedListener.SELECT);
	}

	/** Trigger content assist on choice positions */
	private void triggerContentAssist() {
		if (fFramePosition instanceof ProposalPosition) {
			ProposalPosition pp= (ProposalPosition) fFramePosition;
			fAssistant.setCompletions(pp.getChoices());
			fAssistant.showPossibleCompletions();
		} else {
			fAssistant.setCompletions(new ICompletionProposal[0]);
			fAssistant.hidePossibleCompletions();
		}
	}

	private void switchPosition(LinkedPosition pos, boolean select) {
		Assert.isNotNull(pos);
		if (pos.equals(fFramePosition))
			return;
		
		// mark navigation history
		JavaPlugin.getActivePage().getNavigationHistory().markLocation(JavaPlugin.getActivePage().getActiveEditor());

		// undo
		ITextViewerExtension extension= (ITextViewerExtension) fCurrentTarget.getViewer();
		IRewriteTarget target= extension.getRewriteTarget();
		if (fFramePosition != null)
			target.endCompoundChange();

		redraw();
		IDocument oldDoc= fFramePosition == null ? null : fFramePosition.getDocument();
		IDocument newDoc= pos.getDocument();
		switchViewer(oldDoc, newDoc);
		fFramePosition= pos;

		if (fFramePosition != null)
			target.beginCompoundChange();

		fPainter.setPosition(fFramePosition);
		if (select)
			select();
		if (fFramePosition == fExitPosition && !fIterator.isCycling())
			leave(ILinkedListener.NONE);
		else {
			redraw();
		}
		triggerContentAssist();
	}

	private void switchViewer(IDocument oldDoc, IDocument newDoc) {
		if (oldDoc != newDoc) {
			LinkedUITarget target= null;
			for (int i= 0; i < fTargets.length; i++) {
				if (fTargets[i].getViewer().getDocument() == newDoc) {
					target= fTargets[i];
					break;
				}
			}
			if (target != fCurrentTarget) {
				disconnect();
				fCurrentTarget= target;
				target.enter();
				connect();
			}
		}
	}

	private void select() {
		ITextViewer viewer= fCurrentTarget.getViewer();
		if (!viewer.overlapsWithVisibleRegion(fFramePosition.offset, fFramePosition.length))
			viewer.resetVisibleRegion();
		viewer.revealRange(fFramePosition.offset, fFramePosition.length);
		viewer.setSelectedRange(fFramePosition.offset, fFramePosition.length);
	}

	private void redraw() {
		if (fFramePosition == null)
			return;

		IRegion widgetRange= asWidgetRange(fFramePosition);
		if (widgetRange == null) {
			leave(ILinkedListener.EXIT_ALL);
			return;
		}

		StyledText text= fCurrentTarget.fWidget;
		if (text != null && !text.isDisposed())
			text.redrawRange(widgetRange.getOffset(), widgetRange.getLength(), true);
	}

	private void connect() {
		Assert.isNotNull(fCurrentTarget);
		ITextViewer viewer= fCurrentTarget.getViewer();
		Assert.isNotNull(viewer);
		fCurrentTarget.fWidget= viewer.getTextWidget();
		if (fCurrentTarget.fWidget == null)
			leave(ILinkedListener.EXIT_ALL);

		((ITextViewerExtension) viewer).prependVerifyKeyListener(fKeyListener);
		((IPostSelectionProvider) viewer).addPostSelectionChangedListener(fSelectionListener);

		fPainter.setViewer(viewer);
		fCurrentTarget.fWidget.addPaintListener(fPainter);
		fCurrentTarget.fWidget.showSelection();
		fCurrentTarget.fWidget.addVerifyListener(fCaretListener);
		fCurrentTarget.fWidget.addModifyListener(fCaretListener);

		fCurrentTarget.fShell= fCurrentTarget.fWidget.getShell();
		if (fCurrentTarget.fShell == null)
			leave(ILinkedListener.EXIT_ALL);
		fCurrentTarget.fShell.addShellListener(fCloser);

		fAssistant.install(viewer);
	}

	private void disconnect() {
		Assert.isNotNull(fCurrentTarget);
		ITextViewer viewer= fCurrentTarget.getViewer();
		Assert.isNotNull(viewer);

		fAssistant.uninstall();

		StyledText text= fCurrentTarget.fWidget;
		fCurrentTarget.fWidget= null;
		
		Shell shell= fCurrentTarget.fShell;
		fCurrentTarget.fShell= null;
		if (shell != null)
			shell.removeShellListener(fCloser);
		
		if (text != null) {
			text.removeModifyListener(fCaretListener);
			text.removeVerifyListener(fCaretListener);
			text.removeVerifyKeyListener(fKeyListener);
			text.removePaintListener(fPainter);
		}

		((ITextViewerExtension) viewer).removeVerifyKeyListener(fKeyListener);
		((IPostSelectionProvider) viewer).removePostSelectionChangedListener(fSelectionListener);

		redraw();
	}

	void leave(int flags) {
		if (!fIsActive)
			return;
		fIsActive= false;

		ITextViewerExtension extension= (ITextViewerExtension) fCurrentTarget.getViewer();
		IRewriteTarget target= extension.getRewriteTarget();
		target.endCompoundChange();
		
//		// debug trace
//		JavaPlugin.log(new Status(IStatus.INFO, JavaPlugin.getPluginId(), IStatus.OK, "leaving linked mode", null));
		fPainter.dispose();
		disconnect();
		redraw();

		if (fExitPosition != null)
			fExitPosition.getDocument().removePosition(fExitPosition);

		if ((flags & ILinkedListener.UPDATE_CARET) != 0 && fExitPosition != null && fFramePosition != fExitPosition)
			switchPosition(fExitPosition, true);

		for (int i= 0; i < fTargets.length; i++) {
			IDocument doc= fTargets[i].getViewer().getDocument();
			if (doc != null) {
				doc.removePositionUpdater(fPositionUpdater);
				boolean uninstallCat= false;
				String[] cats= doc.getPositionCategories();
				for (int j= 0; j < cats.length; j++) {
					if (getCategory().equals(cats[j])) {
						uninstallCat= true;
						break;
					}
				}
				if (uninstallCat)
					try {
						doc.removePositionCategory(getCategory());
					} catch (BadPositionCategoryException e) {
						// ignore
					}
			}
		}

		fEnvironment.exit(flags);
	}

	private IRegion asWidgetRange(Position position) {
		ITextViewer viewer= fCurrentTarget.getViewer();
		if (viewer instanceof ITextViewerExtension3) {

			ITextViewerExtension3 extension= (ITextViewerExtension3) viewer;
			return extension.modelRange2WidgetRange(new Region(position.getOffset(), position.getLength()));

		} else {

			IRegion region= viewer.getVisibleRegion();
			if (includes(region, position))
				return new Region(position.getOffset() - region.getOffset(), position.getLength());
		}

		return null;
	}

	private static boolean includes(IRegion region, Position position) {
		return
			position.getOffset() >= region.getOffset() &&
			position.getOffset() + position.getLength() <= region.getOffset() + region.getLength();
	}

	private Color getFrameColor() {
		StyledText text= fCurrentTarget.getViewer().getTextWidget();
		if (text != null) {
			Display display= text.getDisplay();
			return createColor(JavaPlugin.getDefault().getPreferenceStore(), PreferenceConstants.EDITOR_LINKED_POSITION_COLOR, display);
		}
		return null;
	}

	/**
	 * Creates a color from the information stored in the given preference
	 * store. Returns <code>null</code> if there is no such information
	 * available.
	 */
	private Color createColor(IPreferenceStore store, String key, Display display) {

		RGB rgb= null;

		if (store != null && store.contains(key)) {

			if (store.isDefault(key))
				rgb= PreferenceConverter.getDefaultColor(store, key);
			else
				rgb= PreferenceConverter.getColor(store, key);

			if (rgb != null)
				return new Color(display, rgb);
		}

		return new Color(display, new RGB(100, 255, 100));
	}

	/**
	 * Returns the currently selected region or <code>null</code>.
	 * 
	 * @return the currently selected region or <code>null</code>
	 */
	public IRegion getSelectedRegion() {
		if (fFramePosition == null)
			if (fExitPosition != null)
				return new Region(fExitPosition.getOffset(), fExitPosition.getLength());
			else
				return null;
		else
			return new Region(fFramePosition.getOffset(), fFramePosition.getLength());
	}

	private void rememberSelection(VerifyEvent event) {
		// don't update other editor's carets
		if (event.getSource() != fCurrentTarget.fWidget)
			return;

		Point selection= fCurrentTarget.getViewer().getSelectedRange();
		fCaretPosition.offset= selection.x + selection.y;
		fCaretPosition.length= 0;
		fCaretPosition.isDeleted= false;
		try {
			IDocument document= fCurrentTarget.getViewer().getDocument();
			boolean installCat= true;
			String[] cats= document.getPositionCategories();
			for (int i= 0; i < cats.length; i++) {
				if (getCategory().equals(cats[i]))
					installCat= false;
			}
			if (installCat) {
				document.addPositionCategory(getCategory());
				document.addPositionUpdater(fPositionUpdater);
			}
			if (document.getPositions(getCategory()).length != 0)
				document.removePosition(getCategory(), fCaretPosition);
			document.addPosition(getCategory(), fCaretPosition);
		} catch (BadLocationException e) {
			// will not happen
			Assert.isTrue(false);
		} catch (BadPositionCategoryException e) {
			// will not happen
			Assert.isTrue(false);
		}
	}

	private void updateSelection(ModifyEvent event) {
		// don't set the caret if we've left already (we're still called as the listener
		// has just been removed) or the event does not happen on our current viewer
		if (!fIsActive || event.getSource() != fCurrentTarget.fWidget)
			return;

		if (!fCaretPosition.isDeleted())
			fCurrentTarget.getViewer().setSelectedRange(fCaretPosition.getOffset(), 0);

		fCaretPosition.isDeleted= true;
	}

	private String getCategory() {
		return toString();
	}

}
