/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class CompareResultDialog extends Dialog {
    private static class CompareResultMergeViewer extends TextMergeViewer {
         private CompareResultMergeViewer(Composite parent, int style, CompareConfiguration configuration) {
             super(parent, style, configuration);
         }
        protected ITokenComparator createTokenComparator(String s) {
            return new CharacterComparator(s);
        }
        protected void configureTextViewer(TextViewer textViewer) {
            if (textViewer instanceof SourceViewer) {
                ((SourceViewer)textViewer).configure(new CompareResultViewerConfiguration());   
            }
        }
    }
    
    public static class CompareResultViewerConfiguration extends SourceViewerConfiguration {
        public class SimpleDamagerRepairer implements IPresentationDamager, IPresentationRepairer {
            private IDocument fDocument;

            public void setDocument(IDocument document) {
                fDocument= document;
            }

            public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent event, boolean changed) {
                return new Region(0, fDocument.getLength());
            }

            public void createPresentation(TextPresentation presentation, ITypedRegion damage) {
                int suffix= CompareResultDialog.fgThis.fSuffix;
                int prefix= CompareResultDialog.fgThis.fPrefix;
                TextAttribute attr= new TextAttribute(Display.getDefault().getSystemColor(SWT.COLOR_RED), null, SWT.BOLD);
                presentation.addStyleRange(new StyleRange(prefix, fDocument.getLength()-suffix-prefix, attr.getForeground(), attr.getBackground(), attr.getStyle()));
            }
        }
        
        public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
            PresentationReconciler reconciler= new PresentationReconciler();
            SimpleDamagerRepairer dr= new SimpleDamagerRepairer();
            reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
            reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
            return reconciler;
        }
    }
    
    private static class CharacterComparator implements ITokenComparator {
        private String fSource;
        CharacterComparator(String source) {
            fSource= source;
        }
        public int getTokenStart(int index) {
            return index;
        }
        public int getTokenLength(int index) {
            return 1;
        }
        public int getRangeCount() {
            return fSource.length();
        }
        public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
            CharacterComparator occ= (CharacterComparator) other;
            return fSource.charAt(thisIndex) == occ.fSource.charAt(otherIndex);
        }
        public boolean skipRangeComparison(int length, int maxLength, IRangeComparator other) {
            return false;
        }
    }
    
	private static class CompareElement implements ITypedElement, IStreamContentAccessor {
	    private InputStream fContent;
	    
	    public CompareElement(String content) {
	        fContent= createInputStream(content);
	    }
	    public String getName() {
	        return "<no name>"; //$NON-NLS-1$
	    }
	    public Image getImage() {
	        return null;
	    }
	    public String getType() {
	        return "txt"; //$NON-NLS-1$
	    }
	    public InputStream getContents() {
	        return fContent;
	    }
	    private static InputStream createInputStream(String s) {
		    try {
		        return new ByteArrayInputStream(s.getBytes(ResourcesPlugin.getEncoding()));
		    } catch (UnsupportedEncodingException e) {
		        return new ByteArrayInputStream(s.getBytes());
		    }
		}
	}

    private TextMergeViewer fViewer;
    private String fExpected;
    private String fActual;
    private String fTestName;
    
    /* workaround - to make prefix and suffix accessible to the CompareResultViewerConfiguration */
    private static CompareResultDialog fgThis;
    
    private int fPrefix;
    private int fSuffix;
    
    // dialog store id constants
    private final static String DIALOG_BOUNDS_KEY= "CompareResultDialogBounds"; //$NON-NLS-1$
    private static final String X= "x"; //$NON-NLS-1$
    private static final String Y= "y"; //$NON-NLS-1$
    private static final String WIDTH= "width"; //$NON-NLS-1$
    private static final String HEIGHT= "height"; //$NON-NLS-1$
    private IDialogSettings fSettings;
	protected Rectangle fNewBounds;
	
	public CompareResultDialog(Shell parentShell, TestRunInfo failure) {
		super(parentShell);
		fgThis= this;
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
        fTestName= failure.getTestName();
        fExpected= failure.getExpected();
        fActual= failure.getActual();
        computePrefixSuffix();
        fSettings= JUnitPlugin.getDefault().getDialogSettings();
	}
	
	protected Point getInitialSize() {
		int width= 0;
		int height= 0;
		
		final Shell s= getShell();
		if (s != null) {
			s.addControlListener(
					new ControlListener() {
						public void controlMoved(ControlEvent arg) {
							fNewBounds= s.getBounds();
						}
						public void controlResized(ControlEvent arg) {
							fNewBounds= s.getBounds();
						}
					}
			);
		}
		IDialogSettings bounds= fSettings.getSection(DIALOG_BOUNDS_KEY); 
		if (bounds == null) {
			return super.getInitialSize();
		}
		else {
			try {
				width= bounds.getInt(WIDTH);
			} catch (NumberFormatException e) {
				width= 400;
			}
			try {
				height= bounds.getInt(HEIGHT);
			} catch (NumberFormatException e) {
				height= 300;
			}
		}	
		return new Point(width, height);	
	}
	
	protected Point getInitialLocation(Point initialSize) {
		Point loc= super.getInitialLocation(initialSize);
		
		IDialogSettings bounds= fSettings.getSection(DIALOG_BOUNDS_KEY);
		if (bounds != null) {
			try {
				loc.x= bounds.getInt(X);
			} catch (NumberFormatException e) {
			}
			try {
				loc.y= bounds.getInt(Y);
			} catch (NumberFormatException e) {
			}
		}
		return loc;
	}
	
	public boolean close() {
		boolean closed= super.close();
		if (closed && fNewBounds != null)
			saveBounds(fNewBounds);
		return closed;
	}

	private void saveBounds(Rectangle bounds) {
		IDialogSettings dialogBounds= fSettings.getSection(DIALOG_BOUNDS_KEY);
		if (dialogBounds == null) {
			dialogBounds= new DialogSettings(DIALOG_BOUNDS_KEY);
			fSettings.addSection(dialogBounds);
		}
		dialogBounds.put(X, bounds.x);
		dialogBounds.put(Y, bounds.y);
		dialogBounds.put(WIDTH, bounds.width);
		dialogBounds.put(HEIGHT, bounds.height);
	}
	
	private void computePrefixSuffix() {
		int end= Math.min(fExpected.length(), fActual.length());
		int i= 0;
		for(; i < end; i++) 
			if (fExpected.charAt(i) != fActual.charAt(i))
				break;
		fPrefix= i;
		
		int j= fExpected.length()-1;
		int k= fActual.length()-1;
		int l= 0;
		for (; k >= fPrefix && j >= fPrefix; k--,j--) {
			if (fExpected.charAt(j) != fActual.charAt(k))
				break;
			l++;
		}
		fSuffix= l;
	}

    protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(JUnitMessages.getString("CompareResultDialog.title")); //$NON-NLS-1$
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, JUnitMessages.getString("CompareResultDialog.labelOK"), true); //$NON-NLS-1$
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite)super.createDialogArea(parent);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		composite.setLayout(layout);
		
		CompareViewerPane pane = new CompareViewerPane(composite, SWT.BORDER | SWT.FLAT);
		pane.setText(fTestName);
		GridData data= new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		data.widthHint= convertWidthInCharsToPixels(120);
		data.heightHint= convertHeightInCharsToPixels(13);
		pane.setLayoutData(data);
		
		Control previewer= createPreviewer(pane);
		pane.setContent(previewer);
		GridData gd= new GridData(GridData.FILL_BOTH);
		previewer.setLayoutData(gd);
		applyDialogFont(parent);
		return composite;
	}
	
	private Control createPreviewer(Composite parent) {
	    final CompareConfiguration compareConfiguration= new CompareConfiguration();
	    compareConfiguration.setLeftLabel(JUnitMessages.getString("CompareResultDialog.expectedLabel")); //$NON-NLS-1$
	    compareConfiguration.setLeftEditable(false);
	    compareConfiguration.setRightLabel(JUnitMessages.getString("CompareResultDialog.actualLabel"));	 //$NON-NLS-1$
	    compareConfiguration.setRightEditable(false);
	    
	    fViewer= new CompareResultMergeViewer(parent, SWT.NONE, compareConfiguration);
	    fViewer.setInput(new DiffNode( 
        new CompareElement(fExpected), 
        new CompareElement(fActual)));

	    Control control= fViewer.getControl();
	    control.addDisposeListener(new DisposeListener() {
	        public void widgetDisposed(DisposeEvent e) {
	            if (compareConfiguration != null)
	                compareConfiguration.dispose();
	        }
	    });
	    return  control;
	}	
}
