package org.eclipse.jdt.internal.ui.launcher;import org.eclipse.core.runtime.CoreException;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.launching.IVMInstall;import org.eclipse.jdt.launching.JavaRuntime;import org.eclipse.jface.viewers.ISelection;import org.eclipse.jface.viewers.ISelectionChangedListener;import org.eclipse.jface.viewers.IStructuredSelection;import org.eclipse.jface.viewers.StructuredSelection;import org.eclipse.jface.viewers.TreeViewer;import org.eclipse.swt.SWT;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;

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
		fVMTree.setInput(JavaRuntime.getVMInstallTypes());	
		fVMTree.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
		
		return parent;
	}

		
	/**
	 * Must be called after createContents
	 */ 
	public void initFromProject(IJavaProject project) {
		IVMInstall vm= null;
		try {
			selectVM(JavaRuntime.getVMInstall(project));
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
		Object o= selection.getFirstElement();
		if (o instanceof IVMInstall)
			return (IVMInstall)o;
		return null;
	}
	
	public void addSelectionChangedListener(ISelectionChangedListener l) {
		fVMTree.addSelectionChangedListener(l);
	}
	
	public void removeSelectionChangedListener(ISelectionChangedListener l) {
		fVMTree.removeSelectionChangedListener(l);
	}
	
	public boolean validateSelection(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection selection= (IStructuredSelection)sel;
			if (selection.size() == 0)
				return true;
			return selection.size() == 1 && selection.getFirstElement() instanceof IVMInstall;
		} else {
			return false;
		}
	}

}
