/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * A panel with counters for the number of Runs, Errors and Failures.
 */
public class CounterPanel extends Composite {
	private Label fNumberOfErrors;
	private Label fNumberOfFailures;
	private Label fNumberOfRuns;
	private int fTotal;
	
	private final Image fErrorIcon= TestRunnerViewPart.createImage("ovr16/error_ovr.gif"); //$NON-NLS-1$
	private final Image fFailureIcon= TestRunnerViewPart.createImage("ovr16/failed_ovr.gif"); //$NON-NLS-1$
			
	public CounterPanel(Composite parent) {
		super(parent, SWT.WRAP);
		GridLayout gridLayout= new GridLayout();
		gridLayout.numColumns= 9;
		gridLayout.makeColumnsEqualWidth= false;
		gridLayout.marginWidth= 0;
		setLayout(gridLayout);
		
		fNumberOfRuns= createLabel(JUnitMessages.getString("CounterPanel.label.runs"), null, " 0/0  ");  //$NON-NLS-1$ //$NON-NLS-2$
		fNumberOfErrors= createLabel(JUnitMessages.getString("CounterPanel.label.errors"), fErrorIcon, " 0 "); //$NON-NLS-1$ //$NON-NLS-2$
		fNumberOfFailures= createLabel(JUnitMessages.getString("CounterPanel.label.failures"), fFailureIcon, " 0 "); //$NON-NLS-1$ //$NON-NLS-2$

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				disposeIcons();
			}
		});
	}
 
	void disposeIcons() {
		fErrorIcon.dispose();
		fFailureIcon.dispose();
	}

	private Label createLabel(String name, Image image, String init) {
		Label label= new Label(this, SWT.NONE);
		if (image != null) {
			image.setBackground(label.getBackground());
			label.setImage(image);
		}
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		
		label= new Label(this, SWT.NONE);
		label.setText(name);
		label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		label= new Label(this, SWT.NONE);
		label.setText(init);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING));
		return label;
	}

	public void reset() {
		fNumberOfErrors.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		fNumberOfFailures.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		fNumberOfErrors.setText(" 0 "); //$NON-NLS-1$
		fNumberOfFailures.setText(" 0 "); //$NON-NLS-1$
		String runString= JUnitMessages.getFormattedString("CounterPanel.runcount", new String[] { Integer.toString(0), Integer.toString(0) }); //$NON-NLS-1$
		fNumberOfRuns.setText(runString); 
		fTotal= 0;
	}
	
	public void setTotal(int value) {
		fTotal= value;
	}
	
	public int getTotal(){
		return fTotal;
	}
	
	public void setRunValue(int value) {
		String runString= JUnitMessages.getFormattedString("CounterPanel.runcount", new String[] { Integer.toString(value), Integer.toString(fTotal) }); //$NON-NLS-1$
		fNumberOfRuns.setText(runString);

		fNumberOfRuns.redraw();
		redraw();
	}
	
	public void setErrorValue(int value) {
		fNumberOfErrors.setText(Integer.toString(value));
		redraw();
	}
	
	public void setFailureValue(int value) {
		fNumberOfFailures.setText(Integer.toString(value));
		redraw();
	}
}