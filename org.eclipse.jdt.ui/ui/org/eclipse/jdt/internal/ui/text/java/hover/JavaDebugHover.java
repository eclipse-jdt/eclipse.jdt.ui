package org.eclipse.jdt.internal.ui.text.java.hover;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaValue;

import org.eclipse.jdt.internal.ui.text.JavaWordFinder;


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
								appendVariable(buffer, variable);
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

	private static String getTypeName(IVariable variable) throws DebugException {
		IValue value= variable.getValue();

		if (value instanceof IJavaValue)
			return ((IJavaValue) value).getJavaType().getName();			

		return value.getReferenceTypeName();
	}

	private static void appendVariable(StringBuffer buffer, IVariable variable) throws DebugException {

		buffer.append(variable.getName());
		buffer.append(" ="); //$NON-NLS-1$
		
		String type= getTypeName(variable);
		String value= variable.getValue().getValueString().trim();
		
		if (type.equals("java.lang.String")) { //$NON-NLS-1$
			buffer.append(" \""); //$NON-NLS-1$
			buffer.append(value);
			buffer.append('"');

		} else if (type.equals("boolean")) { //$NON-NLS-1$
			buffer.append(' ');
			buffer.append(value);

		} else {
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(type);
			buffer.append(") "); //$NON-NLS-1$
			buffer.append(value);			
		}		
	}
}