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
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;


/**
 * A panel with counters for the number of Runs, Errors and Failures.
 */
public class CounterPanel extends Composite {
	private Label fNumberOfErrors;
	private Label fNumberOfFailures;
	private Label fNumberOfRuns;
	private int fTotal;
	private final Image fErrorIcon= TestRunnerViewPart.createImage("icons/error.gif", getClass());
	private final Image fFailureIcon= TestRunnerViewPart.createImage("icons/failure.gif", getClass());
	private final Image fRunIcon= TestRunnerViewPart.createImage("icons/run_exc.gif", getClass());
			
	public CounterPanel(Composite parent) {
		super(parent, SWT.WRAP);
		GridLayout gridLayout= new GridLayout();
		gridLayout.numColumns= 9;
		gridLayout.makeColumnsEqualWidth= false;
		gridLayout.marginWidth= 0;
		setLayout(gridLayout);
		
		fNumberOfRuns= createLabel("Runs: ", fRunIcon, " 0/0  ");
		fNumberOfErrors= createLabel("Errors: ", fErrorIcon, " 0 ");
		fNumberOfFailures= createLabel("Failures: ", fFailureIcon, " 0 ");

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				disposeIcons();
			}
		});
	}

	void disposeIcons() {
		fErrorIcon.dispose();
		fFailureIcon.dispose();
		fRunIcon.dispose();
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

	protected void reset() {
		fNumberOfErrors.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		fNumberOfFailures.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
		fNumberOfErrors.setText(" 0 ");
		fNumberOfFailures.setText(" 0 ");
		fNumberOfRuns.setText(" 0/0  ");
		fTotal= 0;
	}
	
	protected void setTotal(int value) {
		fTotal= value;
	}
	
	protected int getTotal(){
		return fTotal;
	}
	
	protected void setRunValue(int value) {
		fNumberOfRuns.setText(Integer.toString(value) + "/" + fTotal);
		fNumberOfRuns.redraw();
		redraw();
	}
	
	protected void setErrorValue(int value) {
		fNumberOfErrors.setText(Integer.toString(value));
		redraw();
	}
	
	protected void setFailureValue(int value) {
		fNumberOfFailures.setText(Integer.toString(value));
		redraw();
	}
}