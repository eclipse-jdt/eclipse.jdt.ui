package org.eclipse.jdt.internal.ui.javaeditor;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.util.Locale;
import java.util.ResourceBundle;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.texteditor.ResourceAction;


/**
 * Code Assist menu action.
 * Patches accelerator if on an italian locale.
 * http://dev.eclipse.org/bugs/show_bug.cgi?id=8652
 */
class CodeAssistAction extends ResourceAction {
	
	private IAction fAction;
	private String fDefaultText;
	private String fItalianAccelerator;
	
	private IPropertyChangeListener fListener= new IPropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent event) {
			update(event);
		}
	};
	
	/**
	 * Creates a new action. The action configures its initial visual 
	 * representation from the given resource bundle. If this action's
	 * wrapped action is set to <code>null</code> it also uses the 
	 * information in the resource bundle.
	 *
	 * @param bundle the resource bundle
	 * @param prefix a prefix to be prepended to the various resource keys
	 *   (described in <code>ResourceAction</code> constructor), or 
	 *   <code>null</code> if none
	 * @see ResourceAction#ResourceAction
	 */
	public CodeAssistAction(ResourceBundle bundle, String prefix) {
		super(bundle, prefix);
		fDefaultText= getText();
		
		String acceleratorKey= "italianAccelerator"; //$NON-NLS-1$
		if (prefix != null && prefix.length() > 0)
			acceleratorKey= prefix + acceleratorKey;
		fItalianAccelerator= getString(bundle, acceleratorKey, null);
	}
	
	/**
	 * Updates to the changes of the underlying action.
	 *
	 * @param event the change event describing the state change
	 */
	private void update(PropertyChangeEvent event) {
		if (ENABLED.equals(event.getProperty())) {
			Boolean bool= (Boolean) event.getNewValue();
			setEnabled(bool.booleanValue());
		} else if (TEXT.equals(event.getProperty()))
			setText((String) event.getNewValue());
		else if (TOOL_TIP_TEXT.equals(event.getProperty()))
			setToolTipText((String) event.getNewValue());
	}
	
	/**
	 * Sets the underlying action.
	 *
	 * @param action the underlying action
	 */
	public void setAction(IAction action) {
		
		if (fAction != null) {
			fAction.removePropertyChangeListener(fListener);
			fAction= null;
		}
		
		fAction= action;
		
		if (fAction == null) {
			
			setEnabled(false);
			setText(fDefaultText);
			setToolTipText(""); //$NON-NLS-1$
		
		} else {
						
			setEnabled(fAction.isEnabled());
			setText(fAction.getText());
			setToolTipText(fAction.getToolTipText());
			fAction.addPropertyChangeListener(fListener);
		}
	}

	/*
	 * @see Action#run()
	 */
	public void run() {
		if (fAction != null)
			fAction.run();
	}
	
	/*
	 * @see IAction#setText(String)
	 */
	public void setText(String text) {
		if (fItalianAccelerator != null && onItalianLocale())
			text= patchText(text);
		super.setText(text);
	}
	
	private boolean onItalianLocale() {
		Locale locale= Locale.getDefault();
		if (locale == null)
			return false;
		return "IT".equals(locale.getCountry()) && "it".equals(locale.getLanguage());
	}
	
	private String patchText(String text) {
		
		String accelerator= null;
		
		// find start of accelerator specification
		int index= text.lastIndexOf('\t');
		
		if (index == -1)
			index = text.lastIndexOf('@');
		
		if (index >= 0) {
			// patch text
			text= text.substring(0, index + 1) + fItalianAccelerator;
		}
				
		return text;
	}
}