package org.eclipse.jdt.internal.ui.text.java.hover;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;
import org.eclipse.jdt.internal.ui.text.HTMLPrinter;
import org.eclipse.jdt.internal.ui.text.JavaWordFinder;
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAccess;
import org.eclipse.jdt.internal.ui.viewsupport.JavaTextLabelProvider;


public class JavaTypeHover implements ITextHover {
	
	private IEditorPart fEditor;
	private JavaTextLabelProvider fTextRenderer;
	
	
	public JavaTypeHover(IEditorPart editor) {
		fEditor= editor;
		fTextRenderer= new JavaTextLabelProvider(JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION | JavaElementLabelProvider.SHOW_PARAMETERS);
	}
	
	private ICodeAssist getCodeAssist() {
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
	
	private String getInfoText(IMember member) {
		if (member.getElementType() != IJavaElement.TYPE) {
			StringBuffer buffer= new StringBuffer();
			buffer.append(fTextRenderer.getTextLabel(member.getDeclaringType()));
			buffer.append('.');
			buffer.append(fTextRenderer.getTextLabel(member));
			return buffer.toString();
		}
		
		return fTextRenderer.getTextLabel(member);
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
		ICodeAssist resolve= getCodeAssist();
		if (resolve != null) {
			try {
				
				IJavaElement[] result= resolve.codeSelect(hoverRegion.getOffset(), hoverRegion.getLength());
				
				if (result == null)
					return null;
				
				int nResults= result.length;	
				if (nResults == 0)
					return null;
					
				StringBuffer buffer= new StringBuffer();
				HTMLPrinter.addPageProlog(buffer);
				
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
						HTMLPrinter.addParagraph(buffer, JavaDocAccess.getJavaDoc(member));
					}
				}
				
				HTMLPrinter.addPageEpilog(buffer);
				
				return buffer.toString();
				
			} catch (JavaModelException x) {
				JavaPlugin.log(x.getStatus());
			}
		}
		
		return null;
	}	
}