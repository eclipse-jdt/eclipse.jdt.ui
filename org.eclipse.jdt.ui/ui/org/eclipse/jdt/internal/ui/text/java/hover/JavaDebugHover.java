package org.eclipse.jdt.internal.ui.text.java.hover;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.internal.ui.text.JavaWordFinder;
import org.eclipse.jdt.internal.ui.util.JdtPortingFinder;


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
		
		org.eclipse.jdt.internal.ui.util.JdtPortingFinder.toBeDone("reactivate");
		
//		DebugPlugin debugPlugin= DebugPlugin.getDefault();
//		if (debugPlugin == null)
//			return null;
//			
//		ILaunchManager launchManager= debugPlugin.getLaunchManager();
//		if (launchManager == null)
//			return null;
//			
//		List targets= launchManager.getDebugTargets();
//		if (targets != null && !targets.isEmpty()) {
//			
//			try {
//				
//				String variableName= textViewer.getDocument().get(hoverRegion.getOffset(), hoverRegion.getLength());
//				
//				boolean first= true;
//				StringBuffer buffer= new StringBuffer();
//				for (int i= 0; i < targets.size(); i++) {
//					IDebugTarget target= (IDebugTarget) targets.get(i);
//					IJavaDebugTarget javaTarget = (IJavaDebugTarget)target.getAdapter(IJavaDebugTarget.class);
//					if (javaTarget != null) {
//						IVariable variable= javaTarget.findVariable(variableName);
//						if (variable != null) {
//							if (!first)
//								buffer.append('\n');
//							first= false;
//							buffer.append(variable.getValue().getValueString(true));
//						}
//					}
//				}
//				
//				if (buffer.length() > 0)
//					return buffer.toString();
//			
//			} catch (BadLocationException x) {
//			}
//		}

		return null;
	}
}