package org.eclipse.jdt.internal.ui.text.java.hover;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.preference.IPreferenceStore;import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.jface.util.IPropertyChangeListener;import org.eclipse.jface.util.PropertyChangeEvent;import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.debug.ui.IDebugUIConstants;import org.eclipse.jdt.internal.ui.IPreferencesConstants;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.text.JavaWordFinder;


/**
 * Caution: this implementation is a layer breaker and contains some "shortcuts"
 */
public class JavaTextHover implements ITextHover {
		
	class EditorWatcher implements IPartListener {
		
		/**
		 * @see IPartListener#partOpened(IWorkbenchPart)
		 */
		public void partOpened(IWorkbenchPart part) {
		}
		
		/**
		 * @see IPartListener#partDeactivated(IWorkbenchPart)
		 */
		public void partDeactivated(IWorkbenchPart part) {
		}
		
		/**
		 * @see IPartListener#partClosed(IWorkbenchPart)
		 */
		public void partClosed(IWorkbenchPart part) {
			if (part == fEditor) {
				fEditor.getSite().getWorkbenchWindow().getPartService().removePartListener(fPartListener);
				fPartListener= null;
				
				IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
				store.removePropertyChangeListener(fPropertyChangeListener);
				fPropertyChangeListener= null;
			}
		}
		
		/**
		 * @see IPartListener#partActivated(IWorkbenchPart)
		 */
		public void partActivated(IWorkbenchPart part) {
			update();
		}
		
		public void partBroughtToTop(IWorkbenchPart part) {
			update();
		}	
	};
	
	class PropertyChangeListener implements IPropertyChangeListener {
		/**
		 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
		 */
		public void propertyChange(PropertyChangeEvent event) {
			if (IPreferencesConstants.EDITOR_SHOW_HOVER.equals(event.getProperty())) {
				Object newValue= event.getNewValue();
				if (newValue instanceof Boolean)
					fEnabled= ((Boolean) newValue).booleanValue();
			}
		}
	};
	
	
	protected IEditorPart fEditor;
	protected IPartListener fPartListener;
	protected IPropertyChangeListener fPropertyChangeListener;
	
	protected String fCurrentPerspective;
	protected ITextHover[] fImplementations;
	protected boolean fEnabled;
	
	
	
	public JavaTextHover(IEditorPart editor) {
		
		fEditor= editor;
		
		if (fEditor != null) {
			
			fPartListener= new EditorWatcher();
			IWorkbenchWindow window= fEditor.getSite().getWorkbenchWindow();
			window.getPartService().addPartListener(fPartListener);
			
			fPropertyChangeListener= new PropertyChangeListener();
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			store.addPropertyChangeListener(fPropertyChangeListener);
			fEnabled= store.getBoolean(IPreferencesConstants.EDITOR_SHOW_HOVER);
			
			update();
		}
		
		
	}
	
	
	protected void update() {
		
		IWorkbenchWindow window= fEditor.getSite().getWorkbenchWindow();
		IWorkbenchPage page= window.getActivePage();
		if (page != null) {
			
			String newPerspective= page.getPerspective().getId();
			
			if (fCurrentPerspective == null || fCurrentPerspective != newPerspective) {
				fCurrentPerspective= newPerspective;
				if (IDebugUIConstants.ID_DEBUG_PERSPECTIVE.equals(fCurrentPerspective))
					fImplementations= new ITextHover[] { 
						new JavaDebugHover(fEditor) 
					};
				else
					fImplementations= new ITextHover[] {
						new JavaDebugHover(fEditor),
						new JavaTypeHover(fEditor)
					};
			}
		}
	}
			
	/**
	 * @see ITextHover#getHoverRegion(ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		
		if (!fEnabled)
			return null;
		
		if (textViewer != null)
			return JavaWordFinder.findWord(textViewer.getDocument(), offset);
		
		return null;
	}
		
	/**
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		if (fImplementations != null && fEnabled) {
			for (int i= 0; i < fImplementations.length; i++) {
				String s= fImplementations[i].getHoverInfo(textViewer, hoverRegion);
				if (s != null && s.trim().length() > 0)
					return s;
			}
		}
		return null;
	}
}