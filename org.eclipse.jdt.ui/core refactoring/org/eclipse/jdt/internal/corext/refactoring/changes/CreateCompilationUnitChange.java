package org.eclipse.jdt.internal.corext.refactoring.changes;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.refactoring.NullChange;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.Change;
import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.Templates;
import org.eclipse.jdt.internal.corext.template.java.JavaContext;

/**
  */
public class CreateCompilationUnitChange extends Change {

	private String fName;
	private ICompilationUnit fCompilationUnit;
	
	private boolean fAddFileComment;
	private boolean fAddTypeComment;
	private boolean fIsClass;
	
	private IChange fUndoChange;
	
	private IType fCreatedType;
	
	public CreateCompilationUnitChange(ICompilationUnit cu, boolean isClass, boolean addFileComment, boolean addTypeComment) {
		fCompilationUnit= cu;
		fAddFileComment= addFileComment;
		fAddTypeComment= addTypeComment;
		fIsClass= isClass;
		
		fCreatedType= null;
	}

	/*
	 * @see IChange#perform(ChangeContext, IProgressMonitor)
	 */
	public void perform(ChangeContext context, IProgressMonitor pm) throws JavaModelException {
		pm.beginTask(RefactoringCoreMessages.getString("CreateCUChange.CreatingCU.operation"), 2); //$NON-NLS-1$
		try {
			if (!isActive() || fCompilationUnit.exists()) {
				fUndoChange= new NullChange();	
			} else {
				IPackageFragment pack= (IPackageFragment) fCompilationUnit.getParent();
				String lineDelimiter= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
				String packStatement= pack.isDefaultPackage() ? "" : "package " + pack.getElementName() + ";" + lineDelimiter + lineDelimiter; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								
				fCompilationUnit= pack.createCompilationUnit(fCompilationUnit.getElementName(), packStatement, false, new SubProgressMonitor(pm, 1));
				
				fUndoChange= new DeleteSourceManipulationChange(fCompilationUnit);
				
				if (fAddFileComment) {
					IBuffer buf= fCompilationUnit.getBuffer();
					buf.replace(0, 0, getTemplate("filecomment"));
					buf.save(null, false);
				}

				StringBuffer buf= new StringBuffer();
				if (fAddTypeComment) {
					buf.append(getTemplate("typecomment"));
				}
				createTypeStub(fCompilationUnit, fIsClass, buf);
				String formattedContent= StubUtility.codeFormat(buf.toString(), 0, lineDelimiter);
				
				fCreatedType= fCompilationUnit.createType(formattedContent, null, false, new SubProgressMonitor(pm, 1));
			}		
			setActive(false);
		} catch (CoreException e) {
			throw new JavaModelException(e);
		} finally {
			pm.done();
		}
	}

	protected void createTypeStub(ICompilationUnit cu, boolean isClass, StringBuffer buf) {
		buf.append("public ");
		if (isClass) {
			buf.append(" class ");
		} else {
			buf.append(" interface ");
		}
		buf.append(Signature.getQualifier(cu.getElementName()));
		buf.append(" {\n\n}\n");
	}
	
	protected String getTemplate(String name) throws CoreException {
		Template[] templates= Templates.getInstance().getTemplates(name);
		if (templates.length > 0) {
			String template= JavaContext.evaluateTemplate(templates[0], fCompilationUnit);
			if (template != null) {
				return template;
			}
		}
		return "";
	}
	
	
	/*
	 * @see IChange#getUndoChange()
	 */
	public IChange getUndoChange() {
		return fUndoChange;
	}

	/*
	 * @see IChange#getName()
	 */
	public String getName() {
		return RefactoringCoreMessages.getString("CreateCUChange.CreateCU.name");
	}

	/*
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedLanguageElement() {
		return fCreatedType;
	}
}
