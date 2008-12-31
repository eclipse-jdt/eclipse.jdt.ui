/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak <brockj@tpg.com.au> - [nls tooling] Properties file editor should have "toggle comment" action - https://bugs.eclipse.org/bugs/show_bug.cgi?id=192045
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.propertiesfileeditor;

import org.eclipse.swt.SWT;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IShowInTargetList;

import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import org.eclipse.ui.editors.text.ITextEditorHelpContextIds;
import org.eclipse.ui.editors.text.TextEditor;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jdt.ui.actions.JdtActionConstants;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ToggleCommentAction;


/**
 * Properties file editor.
 *
 * @since 3.1
 */
public class PropertiesFileEditor extends TextEditor {


	/** Open action. */
	protected OpenAction fOpenAction;


	/*
	 * @see org.eclipse.ui.editors.text.TextEditor#initializeEditor()
	 * @since 3.4
	 */
	protected void initializeEditor() {
		setDocumentProvider(JavaPlugin.getDefault().getPropertiesFileDocumentProvider());
		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		setPreferenceStore(store);
		JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
		setSourceViewerConfiguration(new PropertiesFileSourceViewerConfiguration(textTools.getColorManager(), store, this, IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING));
		setEditorContextMenuId("#TextEditorContext"); //$NON-NLS-1$
		setRulerContextMenuId("#TextRulerContext"); //$NON-NLS-1$
		setHelpContextId(ITextEditorHelpContextIds.TEXT_EDITOR);
		configureInsertMode(SMART_INSERT, false);
		setInsertMode(INSERT);
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#initializeKeyBindingScopes()
	 * @since 3.4
	 */
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "org.eclipse.jdt.ui.propertiesEditorScope" });  //$NON-NLS-1$
	}

	/*
	 * @see org.eclipse.ui.editors.text.TextEditor#createActions()
	 */
	protected void createActions() {
		super.createActions();

		IAction action= new ToggleCommentAction(PropertiesFileEditorMessages.getBundleForConstructedKeys(), "ToggleComment.", this); //$NON-NLS-1$
		action.setActionDefinitionId(IJavaEditorActionDefinitionIds.TOGGLE_COMMENT);
		setAction(IJavaEditorActionDefinitionIds.TOGGLE_COMMENT, action);
		markAsStateDependentAction(IJavaEditorActionDefinitionIds.TOGGLE_COMMENT, true);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(action, IJavaHelpContextIds.TOGGLE_COMMENT_ACTION);
		configureToggleCommentAction();

		fOpenAction= new OpenAction(this);
		fOpenAction.setActionDefinitionId(IJavaEditorActionDefinitionIds.OPEN_EDITOR);
		setAction(JdtActionConstants.OPEN, fOpenAction);
	}

	/**
	 * Configures the toggle comment action.
	 *
	 * @since 3.4
	 */
	private void configureToggleCommentAction() {
		IAction action= getAction(IJavaEditorActionDefinitionIds.TOGGLE_COMMENT);
		if (action instanceof ToggleCommentAction) {
			ISourceViewer sourceViewer= getSourceViewer();
			SourceViewerConfiguration configuration= getSourceViewerConfiguration();
			((ToggleCommentAction)action).configure(sourceViewer, configuration);
		}
	}

	/*
	 * @see AbstractTextEditor#handlePreferenceStoreChanged(PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

		try {

			ISourceViewer sourceViewer= getSourceViewer();
			if (sourceViewer == null)
				return;

			((PropertiesFileSourceViewerConfiguration) getSourceViewerConfiguration()).handlePropertyChangeEvent(event);

		} finally {
			super.handlePreferenceStoreChanged(event);
		}
	}

	/*
	 * @see AbstractTextEditor#affectsTextPresentation(PropertyChangeEvent)
	 */
	protected boolean affectsTextPresentation(PropertyChangeEvent event) {
		return ((PropertiesFileSourceViewerConfiguration)getSourceViewerConfiguration()).affectsTextPresentation(event) || super.affectsTextPresentation(event);
	}


	/*
	 * @see org.eclipse.ui.editors.text.TextEditor#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IShowInTargetList.class) {
			return new IShowInTargetList() {
				public String[] getShowInTargetIds() {
					return new String[] { JavaUI.ID_PACKAGES, IPageLayout.ID_RES_NAV };
				}

			};
		}
		return super.getAdapter(adapter);
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#getOrientation()
	 * @since 3.2
	 */
	public int getOrientation() {
		return SWT.LEFT_TO_RIGHT;	// properties editors are always left to right by default (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=110986)
	}

	/*
	 * @see org.eclipse.ui.texteditor.StatusTextEditor#updateStatusField(java.lang.String)
	 */
	protected void updateStatusField(String category) {
		super.updateStatusField(category);
		if (getEditorSite() != null) {
			getEditorSite().getActionBars().getStatusLineManager().setMessage(null);
			getEditorSite().getActionBars().getStatusLineManager().setErrorMessage(null);
		}
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#getSourceViewer()
	 */
	ISourceViewer internalGetSourceViewer() {
		return getSourceViewer();
	}

	/*
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#collectContextMenuPreferencePages()
	 * @since 3.1
	 */
	protected String[] collectContextMenuPreferencePages() {
		String[] ids= super.collectContextMenuPreferencePages();
		String[] more= new String[ids.length + 1];
		more[0]= "org.eclipse.jdt.ui.preferences.PropertiesFileEditorPreferencePage"; //$NON-NLS-1$
		System.arraycopy(ids, 0, more, 1, ids.length);
		return more;
	}

	/*
	 * @see org.eclipse.ui.editors.text.TextEditor#editorContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 * @since 3.4
	 */
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);

		addAction(menu, ITextEditorActionConstants.GROUP_EDIT, IJavaEditorActionDefinitionIds.TOGGLE_COMMENT);
	}
}
