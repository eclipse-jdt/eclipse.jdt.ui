package org.eclipse.jdt.internal.ui.text.java.hover;


import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;


/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

public class JavaTypeHover implements ITextHover {
	
		
	protected IEditorPart fEditor;
	
	
	public JavaTypeHover(IEditorPart editor) {
		fEditor= editor;
	}
			
	protected ICodeAssist getCodeAssist() {
		
		if (fEditor != null) {
		
			IEditorInput input= fEditor.getEditorInput();
			if (input instanceof ClassFileEditorInput) {
				ClassFileEditorInput cfeInput= (ClassFileEditorInput) input;
				return cfeInput.getClassFile();
			}
			
			IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
			return manager.getWorkingCopy(input);
		}
		
		return null;
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
		ICodeAssist resolve= getCodeAssist();
		if (resolve != null) {
		
			try {
				IJavaElement[] result= resolve.codeSelect(hoverRegion.getOffset(), hoverRegion.getLength());
				if (result != null && result.length > 0) {
					
					
					int flags= JavaElementLabelProvider.SHOW_CONTAINER | JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION;
					JavaElementLabelProvider renderer= new JavaElementLabelProvider(flags);
					StringBuffer buffer= new StringBuffer();
					for (int i= 0; i < result.length; i++) {
						if (IJavaElement.TYPE == result[i].getElementType()) { 
							if (i > 0) buffer.append('\n');
							buffer.append(renderer.getText(result[i]));
						}
					}
					
					return buffer.toString();
				}
			} catch (JavaModelException x) {
			}
		}
		
		return null;
	}
}