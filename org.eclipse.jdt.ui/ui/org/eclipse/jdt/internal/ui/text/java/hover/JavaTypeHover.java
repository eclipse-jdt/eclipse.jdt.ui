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

import java.io.Reader;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocAccess;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;
import org.eclipse.jdt.internal.ui.text.HTMLPrinter;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDoc2HTMLTextReader;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementLabels;



public class JavaTypeHover implements IJavaEditorTextHover {
	
	private IEditorPart fEditor;
	private IJavaEditorTextHover fProblemHover;
	
	private final int LABEL_FLAGS=  JavaElementLabels.ALL_FULLY_QUALIFIED
		| JavaElementLabels.M_PRE_RETURNTYPE | JavaElementLabels.M_PARAMETER_TYPES | JavaElementLabels.M_PARAMETER_NAMES | JavaElementLabels.M_EXCEPTIONS 
		| JavaElementLabels.F_PRE_TYPE_SIGNATURE;
	
	public JavaTypeHover() {
		fProblemHover= new JavaProblemHover();
	}
	
	/**
	 * @see IJavaEditorTextHover#setEditor(IEditorPart)
	 */
	public void setEditor(IEditorPart editor) {
		fEditor= editor;
		fProblemHover.setEditor(editor);
	}
	
	private ICodeAssist getCodeAssist() {
		if (fEditor != null) {
			IEditorInput input= fEditor.getEditorInput();
			if (input instanceof IClassFileEditorInput) {
				IClassFileEditorInput cfeInput= (IClassFileEditorInput) input;
				return cfeInput.getClassFile();
			}
			
			IWorkingCopyManager manager= JavaPlugin.getDefault().getWorkingCopyManager();				
			return manager.getWorkingCopy(input);
		}
		
		return null;
	}
	
	private String getInfoText(IMember member) {
		return JavaElementLabels.getElementLabel(member, LABEL_FLAGS);
	}
		
	/*
	 * @see ITextHover#getHoverRegion(ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return JavaWordFinder.findWord(textViewer.getDocument(), offset);
	}
	
	/*
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		
		String hoverInfo= fProblemHover.getHoverInfo(textViewer, hoverRegion);
		if (hoverInfo != null)
			return hoverInfo;
		
		ICodeAssist resolve= getCodeAssist();
		if (resolve != null) {
			try {
				
				IJavaElement[] result= null;
				
				synchronized (resolve) {
					result= resolve.codeSelect(hoverRegion.getOffset(), hoverRegion.getLength());
				}
				
				if (result == null)
					return null;
				
				int nResults= result.length;	
				if (nResults == 0)
					return null;
					
				StringBuffer buffer= new StringBuffer();
				
				if (nResults > 1) {
					
					for (int i= 0; i < result.length; i++) {
						HTMLPrinter.startBulletList(buffer);
						IJavaElement curr= result[i];
						if (curr instanceof IMember)
							HTMLPrinter.addBullet(buffer, getInfoText((IMember) curr));
						HTMLPrinter.endBulletList(buffer);
					}
					
				} else {
					
					IJavaElement curr= result[0];
					if (curr instanceof IMember) {
						IMember member= (IMember) curr;
						HTMLPrinter.addSmallHeader(buffer, getInfoText(member));
						Reader reader= JavaDocAccess.getJavaDoc(member, true);
						if (reader != null) {
							HTMLPrinter.addParagraph(buffer, new JavaDoc2HTMLTextReader(reader));
						}
					}
				}
				
				if (buffer.length() > 0) {
					HTMLPrinter.insertPageProlog(buffer, 0);
					HTMLPrinter.addPageEpilog(buffer);
					return buffer.toString();
				}
				
			} catch (JavaModelException x) {
				JavaPlugin.log(x.getStatus());
			}
		}
		
		return null;
	}	
}