package org.eclipse.jdt.internal.ui.launcher;import org.eclipse.debug.core.model.IDebugTarget;import org.eclipse.debug.core.model.IProcess;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Label;import org.eclipse.ui.dialogs.PropertyPage;
public class ProcessPropertyPage extends PropertyPage {

	/**
	 * Constructor for ProcessPropertyPage
	 */
	public ProcessPropertyPage() {
		super();
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite ancestor) {
		Composite parent= new Composite(ancestor, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		parent.setLayout(layout);
		
		Label l1= new Label(parent, SWT.NULL);
		l1.setText("Command Line:");
		
		GridData gd= new GridData();
		gd.verticalAlignment= gd.BEGINNING;
		l1.setLayoutData(gd);
		
		Label l2= new Label(parent, SWT.WRAP | SWT.BORDER);
		gd= new GridData(gd.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(80);
		gd.heightHint= convertHeightInCharsToPixels(15);
		l2.setLayoutData(gd);
		
		initCommandLineLabel(l2);
		
		return parent;
	}
	
	private void initCommandLineLabel(Label l) {
		Object o= getElement();
		if (o instanceof IDebugTarget)
			o= ((IDebugTarget)o).getProcess();
		if (o instanceof IProcess) {
			IProcess process= (IProcess)o;
			l.setText(process.getAttribute(JavaLauncher.ATTR_CMDLINE));
		}
	}
}
