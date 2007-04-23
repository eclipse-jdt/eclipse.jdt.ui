package org.eclipse.jdt.internal.ui.preferences.cleanup;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import org.eclipse.jface.action.MenuManager;

public class DialogBrowserConfigurationBlock {

	private Browser fBrowser;
	private Composite fParent;
	private boolean fEnabled;
	private String fText;
	
	public DialogBrowserConfigurationBlock() {
		fEnabled= true;
	}

	public Control createControl(Composite parent) {
		fParent= parent;
		
		fBrowser= new Browser(parent, SWT.FLAT | SWT.BORDER);
		
		//Disable context menu
		MenuManager menuManager= new MenuManager("#PopupMenu"); //$NON-NLS-1$
		Menu contextMenu= menuManager.createContextMenu(fBrowser);
		fBrowser.setMenu(contextMenu);
		
		final GridData data= new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		fBrowser.setLayoutData(data);
		
		return fBrowser;
	}

	public void setText(String text) {
		fText= text;
		fBrowser.setText(wrapStyle(text));
	}
	
	public void setEnabled(boolean enabled) {
		fEnabled= enabled;
		if (fText != null) {
			setText(fText);
		}
	}
	
    private String wrapStyle(String text) {
    	StringBuffer buf= new StringBuffer();
    	
    	buf.append("<html><head>"); //$NON-NLS-1$
    	buf.append("<style type=\"text/css\">"); //$NON-NLS-1$
    	buf.append("body {"); //$NON-NLS-1$
    	buf.append("margin-top:5px; margin-left:5px;"); //$NON-NLS-1$
    	
    	int shellStyle= fParent.getStyle();
    	boolean RTL= (shellStyle & SWT.RIGHT_TO_LEFT) != 0;
    	if (RTL)
    		buf.append("direction:rtl;\n"); //$NON-NLS-1$
    	
    	appendFont(buf, fParent.getFont().getFontData()[0]);
    	
    	buf.append("}"); //$NON-NLS-1$
    	buf.append("</style>"); //$NON-NLS-1$
    	buf.append("</head><body>"); //$NON-NLS-1$
    	
    	buf.append(text);
    	
    	buf.append("</body></html>"); //$NON-NLS-1$
    	return buf.toString();
    }

	private void appendFont(StringBuffer buf, FontData fontData) {
		boolean bold= (fontData.getStyle() & SWT.BOLD) != 0;
		boolean italic= (fontData.getStyle() & SWT.ITALIC) != 0;
		
		// See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=155993
		String size= Integer.toString(fontData.getHeight()) + ("carbon".equals(SWT.getPlatform()) ? "px" : "pt"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		buf.append("font-family:").append('\'').append(fontData.getName()).append('\'').append(",sans-serif;\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("font-size:").append(size).append(";\n"); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append("font-weight:").append(bold ? "bold" : "normal").append(";\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		buf.append("font-style:").append(italic ? "italic" : "normal").append(";\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		if (!fEnabled)
			buf.append("color:GrayText"); //$NON-NLS-1$
	}

}
