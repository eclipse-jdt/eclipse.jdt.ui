/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.text.java.hover;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Proxy for JavaEditorTextHovers.
 * 
 * @since 2.1
 */
public class JavaEditorTextHoverProxy extends AbstractJavaEditorTextHover {


	// XXX: Can be removed if we decide we don't want enabling and disabling of hovers
	class ListenerRemover implements IPartListener {

		void dispose() {
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			store.removePropertyChangeListener(fEnableStateUpdater);
			fEnableStateUpdater= null;
			stopListening();
			setEditor(null);
		}

		private void stopListening() {
			getEditor().getSite().getWorkbenchWindow().getPartService().removePartListener(this);
		}

		public void partClosed(IWorkbenchPart part) {
			if (part == getEditor())
				dispose();
		}

		public void partOpened(IWorkbenchPart part) {
		}
		
		public void partDeactivated(IWorkbenchPart part) {
		}
		
		public void partActivated(IWorkbenchPart part) {
		}

		public void partBroughtToTop(IWorkbenchPart part) {
		}	
	};


	// XXX: Can be removed if we decide we don't want enabling and disabling of hovers
	class EnableStateUpdater implements IPropertyChangeListener {
		/*
		 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (PreferenceConstants.EDITOR_SHOW_HOVER.equals(event.getProperty())) {
				Object newValue= event.getNewValue();
				if (newValue instanceof Boolean)
					fEnabled= ((Boolean) newValue).booleanValue();
			}
		}
	};
	

	private JavaEditorTextHoverDescriptor fHoverDescriptor;
	private IJavaEditorTextHover fHover;
	private boolean fEnabled;
	private IPropertyChangeListener fEnableStateUpdater;
	private ListenerRemover fListenerRemover;


	public JavaEditorTextHoverProxy(JavaEditorTextHoverDescriptor descriptor, IEditorPart editor) {
		fEnabled= JavaPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.EDITOR_SHOW_HOVER);
		fHoverDescriptor= descriptor;
		setEditor(editor);
	}

	/*
	 * @see IJavaEditorTextHover#setEditor(IEditorPart)
	 */
	public void setEditor(IEditorPart editor) {
		if (getEditor() != null && fListenerRemover != null)
			fListenerRemover.stopListening();
			
		super.setEditor(editor);

		if (fHover != null)
			fHover.setEditor(getEditor());

//		XXX: Can be removed if we decide we don't want enabling and disabling of hovers
//		if (fEditor != null) {
//			fListenerRemover= new ListenerRemover();
//			IWorkbenchWindow window= fEditor.getSite().getWorkbenchWindow();
//			window.getPartService().addPartListener(fListenerRemover);
//			
//			if (fEnableStateUpdater == null) {
//					fEnableStateUpdater= new EnableStateUpdater();
//					IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
//					store.addPropertyChangeListener(fEnableStateUpdater);
//			}
//		}
	}

	public boolean isEnabled() {
		return true;
	}

	/*
	 * @see ITextHover#getHoverRegion(ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		if (!isEnabled() || fHoverDescriptor == null)
			return null;

		if (isCreated() || createHover())
			return fHover.getHoverRegion(textViewer, offset);
		else
			return null;
	}
	
	/*
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		if (!isEnabled() || fHoverDescriptor == null)
			return null;

		if (isCreated() || createHover())
			return fHover.getHoverInfo(textViewer, hoverRegion);
		else
			return null;
	}

	public void dispose() {
		fListenerRemover.dispose();
		
	}
	
	private boolean isCreated() {
		return fHover != null;
	}

	private boolean createHover() {
		fHover= fHoverDescriptor.createTextHover();
		if (fHover != null)
			fHover.setEditor(getEditor());
		return isCreated();
	}
}