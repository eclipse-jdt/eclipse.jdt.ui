package org.eclipse.jdt.internal.ui.launcher;import org.eclipse.core.runtime.CoreException;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.IVMInstallType;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.TableViewer;import org.eclipse.jface.viewers.TreeViewer;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Combo;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Event;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Listener;import org.eclipse.swt.widgets.Table;

public class VMSelector {
	private TreeViewer fVMTree;
	
	protected Control createContents(Composite ancestor) {		
		Composite parent= new Composite(ancestor, SWT.NULL);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		parent.setLayout(layout);

		fVMTree= new TreeViewer(parent);
		fVMTree.setLabelProvider(new VMLabelProvider());
		fVMTree.setContentProvider(new VMContentProvider());	
		fVMTree.setInput(JavaRuntime.getVMTypes());	
		fVMTree.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		return parent;
	}

		
	/**
	 * Must be called after createContents
	 */ 
	public void initFromProject(IJavaProject project) {
		IVMInstall vm= null;
		try {
			selectVM(JavaRuntime.getVM(project));
		} catch (CoreException e) {
		}
	}
	
	public void selectVM(IVMInstall vm) {
		if (vm != null)
			fVMTree.setSelection(new StructuredSelection(vm));
	}
	
	public IVMInstall getSelectedVM() {
		IStructuredSelection selection= (IStructuredSelection)fVMTree.getSelection();
		if (selection.isEmpty())
			return null;
		return (IVMInstall)selection.getFirstElement();
	}

}
