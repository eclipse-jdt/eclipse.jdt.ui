package org.eclipse.jdt.internal.ui.text.java.hover;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.List;import org.eclipse.debug.core.DebugException;import org.eclipse.debug.core.DebugPlugin;import org.eclipse.debug.core.ILaunchManager;import org.eclipse.debug.core.model.IDebugTarget;import org.eclipse.debug.core.model.IVariable;import org.eclipse.jface.text.BadLocationException;import org.eclipse.jface.text.IRegion;import org.eclipse.jface.text.ITextHover;import org.eclipse.jface.text.ITextViewer;import org.eclipse.ui.IEditorPart;import org.eclipse.jdt.debug.core.IJavaDebugTarget;import org.eclipse.jdt.internal.ui.text.JavaWordFinder;


public class JavaDebugHover implements ITextHover {
		
	
	protected IEditorPart fEditor;
	
	
	public JavaDebugHover(IEditorPart editor) {
		fEditor= editor;
	}
		
	/**
	 * @see ITextHover#getHoverRegion(ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return JavaWordFinder.findWord(textViewer.getDocument(), offset);
	}
		
	/**
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
				
		DebugPlugin debugPlugin= DebugPlugin.getDefault();
		if (debugPlugin == null)
			return null;
			
		ILaunchManager launchManager= debugPlugin.getLaunchManager();
		if (launchManager == null)
			return null;
			
		IDebugTarget[] targets= launchManager.getDebugTargets();
		if (targets != null && targets.length > 0) {
			
			try {
				
				String variableName= textViewer.getDocument().get(hoverRegion.getOffset(), hoverRegion.getLength());
				
				boolean first= true;
				StringBuffer buffer= new StringBuffer();
				for (int i= 0; i < targets.length; i++) {
					IJavaDebugTarget javaTarget = (IJavaDebugTarget) targets[i].getAdapter(IJavaDebugTarget.class);
					if (javaTarget != null) {
						try {
							IVariable variable= javaTarget.findVariable(variableName);
							if (variable != null) {
								if (!first)
									buffer.append('\n');
								first= false;
								buffer.append(variable.getValue().getValueString());
							}
						} catch (DebugException x) {
						}
					}
				}
				
				if (buffer.length() > 0)
					return buffer.toString();
			
			} catch (BadLocationException x) {
			}
		}

		return null;
	}
}