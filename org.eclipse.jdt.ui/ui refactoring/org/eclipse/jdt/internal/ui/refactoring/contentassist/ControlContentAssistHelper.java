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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.DialogPage;
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
	
	public static ContentAssistant createComboContentAssistant(final Combo combo, IContentAssistProcessor processor, DialogPage dialogPage) {
		final ContentAssistant contentAssistant= createContentAssistant(combo, processor, dialogPage);
		contentAssistant.install(new ComboContentAssistSubjectAdapter(combo));
		return contentAssistant;
	}
	
	public static ContentAssistant createTextContentAssistant(final Text text, IContentAssistProcessor processor, DialogPage dialogPage) {
		final ContentAssistant contentAssistant= createContentAssistant(text, processor, dialogPage);
		contentAssistant.install(new TextContentAssistSubjectAdapter(text));
		return contentAssistant;
	}

	private static ContentAssistant createContentAssistant(final Control control, IContentAssistProcessor processor, DialogPage dialogPage) {
		final ContentAssistant contentAssistant= new ContentAssistant();
					
		contentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		
		ContentAssistPreference.configure(contentAssistant, JavaPlugin.getDefault().getJavaTextTools().getPreferenceStore());
		contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		contentAssistant.setInformationControlCreator(new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, SWT.NONE, new HTMLTextPresenter(true));
			}
		});
	
		control.addKeyListener(getContentAssistKeyAdapter(contentAssistant, dialogPage));
		return contentAssistant;
	}

	private static KeyAdapter getContentAssistKeyAdapter(final ContentAssistant contentAssistant, final DialogPage dialogPage) {
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
						if (errorMessage != null && dialogPage != null)
							dialogPage.setErrorMessage(errorMessage);
						return;
					}
				}
			}};
	}


}
