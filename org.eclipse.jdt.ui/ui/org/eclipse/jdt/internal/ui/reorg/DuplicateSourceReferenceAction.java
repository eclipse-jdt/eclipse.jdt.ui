package org.eclipse.jdt.internal.ui.reorg;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.codemanipulation.MemberEdit;
import org.eclipse.jdt.internal.corext.codemanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.codemanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.codemanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.StructuredSelectionProvider;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

class DuplicateSourceReferenceAction extends SourceReferenceAction {
	
	private ISelectionProvider fSelectionProvider;
	
	protected DuplicateSourceReferenceAction(StructuredSelectionProvider provider, ISelectionProvider selectionProvider) {
		super("Dupl&icate", provider);
		Assert.isNotNull(selectionProvider);
		fSelectionProvider= selectionProvider;
	}

	/*
	 * @see Action#run
	 */
	public void run() {
		new BusyIndicator().showWhile(JavaPlugin.getActiveWorkbenchShell().getDisplay(), new Runnable() {
			public void run() {
				try {
					perform(getSelectedElements());
				} catch (CoreException e) {
					ExceptionHandler.handle(e, "Duplicate", "Unexpected exception. See log for details.");
				}
			}
		});
	}

	static void perform(ISourceReference[] elements) throws CoreException {
		ISourceReference[] childrenRemoved= SourceReferenceUtil.removeAllWithParentsSelected(elements);
		Map mapping= SourceReferenceUtil.groupByFile(childrenRemoved); //IFile -> List of ISourceReference (elements from that file)
		for (Iterator iter= mapping.keySet().iterator(); iter.hasNext();) {
			IFile file= (IFile)iter.next();
			List l= (List)mapping.get(file);
			ISourceReference[] refs= (ISourceReference[]) l.toArray(new ISourceReference[l.size()]);
			duplicate(file, refs);
		}
	}
	
	private static void duplicate(IFile file, ISourceReference[] elems) throws CoreException{
		TextBuffer tb= TextBuffer.acquire(file);
		TextBufferEditor tbe= new TextBufferEditor(tb);
		for (int i= 0; i < elems.length; i++) {
			tbe.add(createDuplicateEdit(elems[i]));
		}
		if (! tbe.canPerformEdits())
			return; ///XXX can i assert here?
		tbe.performEdits(new NullProgressMonitor());	
		TextBuffer.commitChanges(tb, false, new NullProgressMonitor());
		TextBuffer.release(tb);
	}
	
	private static TextEdit createDuplicateEdit(ISourceReference ref) throws JavaModelException{
		IJavaElement elements= (IJavaElement)ref;
		int tabWidth= CodeFormatterPreferencePage.getTabSize();
		String[] source= new String[]{SourceReferenceSourceRangeComputer.computeSource(ref)};
		return new MemberEdit(elements, MemberEdit.INSERT_AFTER, source, tabWidth);
	}
}

