package org.eclipse.jdt.internal.ui.launcher;import org.eclipse.debug.core.model.IDebugTarget;import org.eclipse.debug.core.model.IProcess;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jdt.ui.JavaUI;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Text;import org.eclipse.ui.dialogs.PropertyPage;
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
		
		Text l2= new Text(parent, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.READ_ONLY);		gd= new GridData(gd.FILL_HORIZONTAL);
		gd.widthHint= convertWidthInCharsToPixels(80);
		gd.heightHint= convertHeightInCharsToPixels(15);
		l2.setLayoutData(gd);
		
		initCommandLineLabel(l2);
		
		return parent;
	}
	
	private void initCommandLineLabel(Text l) {
		Object o= getElement();
		if (o instanceof IDebugTarget)
			o= ((IDebugTarget)o).getProcess();
		if (o instanceof IProcess) {
			IProcess process= (IProcess)o;			String cmdLine= process.getAttribute(JavaRuntime.ATTR_CMDLINE);			if (cmdLine != null)
				l.setText(cmdLine);
		}
	}
}
