package org.eclipse.jdt.internal.ui.text.java.hover;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

import java.io.BufferedReader;import java.io.IOException;import java.io.Reader;import org.eclipse.swt.custom.StyledText;import org.eclipse.swt.graphics.GC;import org.eclipse.swt.graphics.Point;import org.eclipse.swt.graphics.Rectangle;import org.eclipse.jface.text.IRegion;import org.eclipse.jface.text.ITextHover;import org.eclipse.jface.text.ITextViewer;import org.eclipse.ui.IEditorInput;import org.eclipse.ui.IEditorPart;import org.eclipse.jdt.core.ICodeAssist;import org.eclipse.jdt.core.IJavaElement;import org.eclipse.jdt.core.IMember;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.ui.IWorkingCopyManager;import org.eclipse.jdt.ui.JavaElementLabelProvider;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;import org.eclipse.jdt.internal.ui.text.JavaWordFinder;import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAccess;import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocTextReader;

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
			String lineDelim= System.getProperty("line.separator", "\n");
			try {
				IJavaElement[] result= resolve.codeSelect(hoverRegion.getOffset(), hoverRegion.getLength());
				int nResults= result.length;
				if (result != null && nResults > 0) {
					int hoverWidth= getMaxUsableWidth(textViewer, hoverRegion) - 40;
					if (hoverWidth < 200) {
						hoverWidth= 200;
					}
					GC gc= new GC(textViewer.getTextWidget().getDisplay());
					
					int flags= JavaElementLabelProvider.SHOW_CONTAINER | JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION;
					JavaElementLabelProvider renderer= new JavaElementLabelProvider(flags);
					StringBuffer buffer= new StringBuffer();
					if (nResults > 1) {
						for (int i= 0; i < result.length; i++) {
							IJavaElement curr= result[i];
							if (curr instanceof IMember) {
								String infoText= renderer.getText(curr);
								if (buffer.length() > 0) {
									buffer.append(lineDelim);
								}								
								buffer.append(infoText);
							}
						}
					} else {
						IJavaElement curr= result[0];
						if (curr instanceof IMember) {
							Reader rd= JavaDocAccess.getJavaDoc((IMember)curr);
							if (rd != null) {
								String jdocText= getFormattedText(rd, gc, hoverWidth, 10);
								buffer.append(jdocText);
								buffer.append(lineDelim);
							}
							String infoText= renderer.getText(curr);
							buffer.append(infoText);
						}
					}
					renderer.dispose();
					return buffer.toString();
				}
			} catch (JavaModelException x) {
				JavaPlugin.log(x.getStatus());
			}
		}
		
		return null;
	}
	
	private static String getFormattedText(Reader reader, GC gc, int maxWidth, int maxNumberOfLines) {
		try {
			int whiteSpaceWidth= gc.getCharWidth(' ');
			String lineDelim= System.getProperty("line.separator", "\n");
			
			StringBuffer buf= new StringBuffer();
			BufferedReader bufReader= new BufferedReader(new JavaDocTextReader(reader));

			String line= bufReader.readLine();
			while (line != null && maxNumberOfLines > 0) {
				if (buf.length() != 0) {
					buf.append(lineDelim);
				}
				int lineLen= gc.textExtent(line).x;
				if (lineLen < maxWidth) {
					buf.append(line);
					maxNumberOfLines--;
				} else {
					int idx= 0;
					while (maxNumberOfLines > 0 && idx < line.length()) {
						if (idx != 0) {
							buf.append(lineDelim);
						}
						int breakIdx= findBreakIndex(gc, line, idx, maxWidth, whiteSpaceWidth);
						buf.append(line.substring(idx, breakIdx));
						maxNumberOfLines--;
						idx= findWordBegin(line, breakIdx);
					}					
				}
				line= bufReader.readLine();
			}
			if (line != null) {
				buf.append(lineDelim);
				buf.append("...");
			}		
			return buf.toString();
			
		} catch (IOException e) {
			JavaPlugin.log(e);
		}
		return null;
	}

	private static int findWordBegin(String line, int idx) {
		while (idx < line.length() && Character.isWhitespace(line.charAt(idx))) {
			idx++;
		}
		return idx;
	}
	
	private static int findBreakIndex(GC gc, String line, int currIndex, int maxWidth, int whiteSpaceWidth) {
		int currWidth= 0;
		int lineLength= line.length();

		while (currIndex < lineLength) {
			char ch= line.charAt(currIndex);
			int nextWidth;
			int nextIndex;
			if (Character.isWhitespace(ch)) {
				nextWidth= currWidth + whiteSpaceWidth;
				nextIndex= currIndex + 1;
			} else {
				nextIndex= currIndex + 1;
				while (nextIndex < lineLength && !Character.isWhitespace(line.charAt(nextIndex))) {
					nextIndex++;
				}
				String word= line.substring(currIndex, nextIndex);
				nextWidth= gc.textExtent(word).x + currWidth;
			}
			if (nextWidth > maxWidth) {
				return currIndex;
			}
			currWidth= nextWidth;
			currIndex= nextIndex;
		}
		return currIndex;
	}
			
	private int getMaxUsableWidth(ITextViewer textViewer, IRegion hoverRegion) {
		StyledText textWidget= textViewer.getTextWidget();
		Point currRelPos= textWidget.getLocationAtOffset(hoverRegion.getOffset());
		Point currAbsPos= textWidget.toDisplay(currRelPos);
		
		Rectangle displayBounds= textWidget.getDisplay().getBounds();
		return displayBounds.width - (currAbsPos.x - displayBounds.x); 
	}
	

}