/* * (c) Copyright IBM Corp. 2000, 2001. * All Rights Reserved. */package org.eclipse.jdt.internal.ui.preferences;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.JavaUIMessages;import org.eclipse.jface.preference.IntegerFieldEditor;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Text;
public class MinMaxIntegerFieldEditor extends IntegerFieldEditor {

	private int fMinimumValue= Integer.MIN_VALUE;
	private int fMaximumValue= Integer.MAX_VALUE;
	
	/**
	 * Constructor for MinMaxIntegerFieldEditor
	 */
	public MinMaxIntegerFieldEditor(String preferenceName, String label, Composite parent) {
		super(preferenceName, label, parent);
	}
	
	public void setMaximumValue(int maximum) {
		fMaximumValue= maximum;
	}
	
	public void setMinimumValue(int minimum) {
		fMinimumValue= minimum;
	}
	
	
	protected boolean checkState() {
	
		Text text = getTextControl();
	
		if (text == null)
			return false;
	
		String numberString = text.getText();
		try {
			Long number = Long.valueOf(numberString);
			if (number.longValue() > fMaximumValue) {
				String message= JavaUIMessages.getFormattedString("MinMaxIntegerEditor.error.too_large", String.valueOf(fMaximumValue)); //$NON-NLS-1$
				showErrorMessage(message);
				return false;
			} else if (number.longValue() < fMinimumValue) {
				String message= JavaUIMessages.getFormattedString("MinMaxIntegerEditor.error.too_small", String.valueOf(fMinimumValue)); //$NON-NLS-1$
				showErrorMessage(message);
			} else {
				clearErrorMessage();
				return true;
			}
		} catch (NumberFormatException e1) {
			showErrorMessage();
		}
	
		return false;
	}
	
}
