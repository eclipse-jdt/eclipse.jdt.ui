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

package org.eclipse.jdt.internal.ui.refactoring.contentassist;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommand;
import org.eclipse.ui.commands.ICommandManager;
import org.eclipse.ui.commands.IKeySequenceBinding;
import org.eclipse.ui.keys.CharacterKey;
import org.eclipse.ui.keys.KeySequence;
import org.eclipse.ui.keys.KeyStroke;
import org.eclipse.ui.keys.ModifierKey;
import org.eclipse.ui.keys.SWTKeySupport;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;

/**
 * @since 3.0
 */
public class ControlContentAssistHelper {
	
	/**
	 * @param combo the combo box to install ContentAssist
	 * @param processor the IContentAssistProcessor
	 * @return the installed ContentAssistant
	 */
	public static ContentAssistant createComboContentAssistant(final Combo combo, IContentAssistProcessor processor) {
		final ContentAssistant contentAssistant= createContentAssistant(combo, processor);
		contentAssistant.install(new ComboContentAssistSubjectAdapter(combo));
		return contentAssistant;
	}
	
	/**
	 * @param text the text field to install ContentAssist
	 * @param processor the IContentAssistProcessor
	 * @return the installed ContentAssistant
	 */
	public static ContentAssistant createTextContentAssistant(final Text text, IContentAssistProcessor processor) {
		final ContentAssistant contentAssistant= createContentAssistant(text, processor);
		contentAssistant.install(new TextContentAssistSubjectAdapter(text));
		return contentAssistant;
	}

	private static ContentAssistant createContentAssistant(final Control control, IContentAssistProcessor processor) {
		final ContentAssistant contentAssistant= new ContentAssistant();
					
		contentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		
		ContentAssistPreference.configure(contentAssistant, JavaPlugin.getDefault().getJavaTextTools().getPreferenceStore());
		contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		contentAssistant.setInformationControlCreator(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, SWT.NONE, new HTMLTextPresenter(true));
			}
		});
	
		control.addKeyListener(getContentAssistKeyAdapter(contentAssistant));
		hookForSmartCue(control);
		return contentAssistant;
	}

	private static KeyAdapter getContentAssistKeyAdapter(final ContentAssistant contentAssistant) {
		return new KeyAdapter() {
			KeySequence[] fKeySequences;
			
			private KeySequence[] getKeySequences() {
				if (fKeySequences == null) {
					ICommandManager cm = PlatformUI.getWorkbench().getCommandSupport().getCommandManager();
					ICommand command= cm.getCommand(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
					if (command.isDefined()) {
						List list= command.getKeySequenceBindings();
						if (!list.isEmpty()) {
							fKeySequences= new KeySequence[list.size()];
							for (int i= 0; i < fKeySequences.length; i++) {
								fKeySequences[i]= ((IKeySequenceBinding) list.get(i)).getKeySequence();
							}
							return fKeySequences;
						}		
					}
					// default is Ctrl+Space
					fKeySequences= new KeySequence[] { 
							KeySequence.getInstance(KeyStroke.getInstance(ModifierKey.CTRL, CharacterKey.SPACE))
					};
				}
				return fKeySequences;
			}
			
			public void keyPressed(KeyEvent e) {
				int accelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
				KeySequence keySequence = KeySequence.getInstance(SWTKeySupport.convertAcceleratorToKeyStroke(accelerator));
				KeySequence[] sequences= getKeySequences();
				
				for (int i= 0; i < sequences.length; i++) {
					// only works for single strokes (would need to hold KeyBindingState for multiple)
					// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=46662
					if (sequences[i].equals(keySequence)) {
						e.doit= false;
						String errorMessage= contentAssistant.showPossibleCompletions();
						if (errorMessage != null) {
							System.err.println(errorMessage);
						}
					}
				}
			}};
	}

	/**
	 * Installs a visual cue indicating availability of content assist on the given control.
	 */
	public static void hookForSmartCue(final Control control) {
		
		if (!(control instanceof Text) && !(control instanceof Combo))
			return;
		
		class Handler implements FocusListener, PaintListener {
			Image fBulb;
			ImageDescriptor fBulbID= ImageDescriptor.createFromFile(ControlContentAssistHelper.class, "bulb.gif"); //$NON-NLS-1$
			
			public void paintControl(PaintEvent e) {
				if (control.isDisposed())
					return;
				int dx, dy;
				if (control instanceof Combo || control instanceof Text) { //TODO: original was w/o || control instanceof Text
					if (SWT.getPlatform().equals("carbon"))	//$NON-NLS-1$
						dx= -9;
					else
						dx= -8;
					dy= 0;
				} else {
					dx= -5;
					dy= 3;
				}
				Point global= control.toDisplay(dx, dy);
				Point p= ((Control) e.widget).toControl(global);
				if (fBulb == null)
					fBulb= fBulbID.createImage();
				e.gc.drawImage(fBulb, p.x, p.y);
			}
			
			public void focusGained(FocusEvent e) {
				for (Control c= ((Control)e.widget).getParent(); c != null; c= c.getParent()) {
					c.addPaintListener(this);
					c.redraw();
				}
			}
			
			public void focusLost(FocusEvent e) {
				for (Control c= ((Control)e.widget).getParent(); c != null; c= c.getParent()) {
					c.removePaintListener(this);
					c.redraw();
				}
				if (fBulb != null) {
					fBulb.dispose();
					fBulb= null;
				}
			}
		}
		
		control.addFocusListener(new Handler());
	}
}
