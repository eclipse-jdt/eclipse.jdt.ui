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

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ComboContentAssistSubjectAdapter;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.contentassist.TextContentAssistSubjectAdapter;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommand;
import org.eclipse.ui.commands.ICommandManager;
import org.eclipse.ui.commands.IKeySequenceBinding;
import org.eclipse.ui.keys.KeySequence;
import org.eclipse.ui.keys.SWTKeySupport;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;

import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;

/**
 * @since 3.0
 */
public class ControlContentAssistHelper {
	
	/**
	 * @param combo the combo box to install ContentAssist
	 * @param processor the <code>IContentAssistProcessor</code>
	 * @return the installed ContentAssistant
	 */
	public static IContentAssistant createComboContentAssistant(final Combo combo, IContentAssistProcessor processor) {
		final ContentAssistant contentAssistant= createContentAssistant(combo, processor);
		contentAssistant.install(new ComboContentAssistSubjectAdapter(combo));
		return contentAssistant;
	}
	
	/**
	 * @param text the text field to install ContentAssist
	 * @param processor the <code>IContentAssistProcessor</code>
	 * @return the installed ContentAssistant
	 */
	public static IContentAssistant createTextContentAssistant(final Text text, IContentAssistProcessor processor) {
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
		return contentAssistant;
	}

	private static KeyAdapter getContentAssistKeyAdapter(final ContentAssistant contentAssistant) {
		class ContentAssistKeyAdapter extends KeyAdapter {
			KeySequence[] fContentAssistKeySequences;
			KeySequence[] fPrefixContentAssistKeySequences;

			private ContentAssistKeyAdapter() {
				fContentAssistKeySequences= getKeySequences(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
				fPrefixContentAssistKeySequences= getKeySequences(IJavaEditorActionDefinitionIds.CONTENT_ASSIST_COMPLETE_PREFIX);
			}

			private KeySequence[] getKeySequences(String commandId) {
				ICommandManager cm = PlatformUI.getWorkbench().getCommandSupport().getCommandManager();
				ICommand command= cm.getCommand(commandId);
				if (command.isDefined()) {
					List list= command.getKeySequenceBindings();
					if (!list.isEmpty()) {
						KeySequence[] result= new KeySequence[list.size()];
						for (int i= 0; i < result.length; i++) {
							result[i]= ((IKeySequenceBinding) list.get(i)).getKeySequence();
						}
						return result;
					}
				}
				return new KeySequence[0];
			}

			public void keyPressed(KeyEvent e) {
				int accelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
				KeySequence keySequence = KeySequence.getInstance(SWTKeySupport.convertAcceleratorToKeyStroke(accelerator));
				
				// XXX: this only works for single strokes (would need to hold KeyBindingState for multiple)
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=46662
				if (hasKeySequence(fContentAssistKeySequences, keySequence)) {
					String errorMessage= contentAssistant.showPossibleCompletions();
					if (errorMessage != null) {
						// XXX: better error display to come.
						// System.err.println(errorMessage);
					}
					e.doit= false;
					return;
				} else if (hasKeySequence(fPrefixContentAssistKeySequences, keySequence)) {
					String errorMessage= contentAssistant.completePrefix();
					if (errorMessage != null) {
						// XXX: better error display to come.
						// System.err.println(errorMessage);
					}
					e.doit= false;
					return;
				}
			}
			
			private boolean hasKeySequence(KeySequence[] registered, KeySequence pressed) {
				for (int i= 0; i < registered.length; i++)
					if (registered[i].equals(pressed))
						return true;
				return false;
			}
		}

		return new ContentAssistKeyAdapter();
	}
}
