package org.eclipse.jdt.internal.ui.text.java.hover;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.IOException;import java.io.Reader;import org.eclipse.swt.graphics.GC;import org.eclipse.swt.graphics.Rectangle;import org.eclipse.swt.widgets.Display;import org.eclipse.jface.text.IRegion;import org.eclipse.jface.text.ITextHover;import org.eclipse.jface.text.ITextViewer;import org.eclipse.ui.IEditorInput;import org.eclipse.ui.IEditorPart;import org.eclipse.jdt.core.ICodeAssist;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IMember;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.ui.IWorkingCopyManager;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;import org.eclipse.jdt.internal.ui.text.JavaWordFinder;import org.eclipse.jdt.internal.ui.text.LineBreakingReader;import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAccess;import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocTextReader;import org.eclipse.jdt.internal.ui.viewsupport.JavaTextLabelProvider;

public class JavaTypeHover implements ITextHover {
	
	private static final int NUMBER_OF_JAVADOC_LINES= 12;
	
	private IEditorPart fEditor;
	private JavaTextLabelProvider fTextRenderer;
	
	
	public JavaTypeHover(IEditorPart editor) {
		fEditor= editor;
		fTextRenderer= new JavaTextLabelProvider(JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION | JavaElementLabelProvider.SHOW_PARAMETERS);
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
			String lineDelim= System.getProperty("line.separator", "\n"); //$NON-NLS-2$ //$NON-NLS-1$
			try {
				IJavaElement[] result= resolve.codeSelect(hoverRegion.getOffset(), hoverRegion.getLength());
				int nResults= result.length;
				if (result != null && nResults > 0) {
					StringBuffer buffer= new StringBuffer();
					if (nResults > 1) {
						for (int i= 0; i < result.length; i++) {
							IJavaElement curr= result[i];
							if (curr instanceof IMember) {
								String infoText= getInfoText((IMember)curr);
								if (buffer.length() > 0) {
									buffer.append(lineDelim);
								}								
								buffer.append(infoText);
							}
						}
					} else {
						IJavaElement curr= result[0];
						if (curr instanceof IMember) {
							IMember member= (IMember)curr;
							String infoText= getInfoText(member);
							buffer.append(infoText);							
							String jdocText= getJavaDocText(member, lineDelim);
							if (jdocText != null) {
								buffer.append(lineDelim);
								buffer.append(jdocText);
							}							
						}
					}
					return buffer.toString();
				}
			} catch (JavaModelException x) {
				JavaPlugin.log(x.getStatus());
			}
		}
		
		return null;
	}
		
	private String getInfoText(IMember member) {
		StringBuffer buf= new StringBuffer();
		if (member.getElementType() != IJavaElement.TYPE) {
			buf.append(fTextRenderer.getTextLabel(member.getDeclaringType()));
			buf.append('.');
		}
		buf.append(fTextRenderer.getTextLabel(member));
		return buf.toString();
	}		
	
	
	private String getJavaDocText(IMember member, String lineDelim) throws JavaModelException {
		Reader rd= JavaDocAccess.getJavaDoc(member);
		if (rd != null) {
			JavaDocTextReader textReader= new JavaDocTextReader(rd);
			
			Display display= fEditor.getSite().getShell().getDisplay();
			GC gc= new GC(display);
			try {
				StringBuffer buf= new StringBuffer();
				int maxNumberOfLines= NUMBER_OF_JAVADOC_LINES;
				
				LineBreakingReader reader= new LineBreakingReader(textReader, gc, getHoverWidth(display));
				String line= reader.readLine();
				while (maxNumberOfLines > 0 && line != null) {
					if (buf.length() != 0) {
						buf.append(lineDelim);
					}
					buf.append(' '); // add one space indent
					buf.append(line);
					line= reader.readLine();
					maxNumberOfLines--;
				}
				if (line != null) {
					buf.append(lineDelim);
					buf.append(JavaHoverMessages.getString("TypeHover.more_to_come")); //$NON-NLS-1$
				}
				return buf.toString();
			} catch (IOException e) {
				JavaPlugin.log(e);
			} finally {
				gc.dispose();
			}
		}
		return null;
	}
	
	private int getHoverWidth(Display display) {
		Rectangle displayBounds= display.getClientArea();
		int hoverWidth= displayBounds.width - (display.getCursorLocation().x - displayBounds.x);
		hoverWidth-= 5; // add some space to the border
		if (hoverWidth < 200) {
			hoverWidth= 200;
		}
		return hoverWidth;
	}
	
	


}