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

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.IImportsStructure;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
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

	private ICompilationUnit fCompilationUnit;
	private boolean fIsClass;
	private String[] fSuperTypes;
	
	private CodeGenerationSettings fCodeGenerationSettings;
	
	private IType fCreatedType;
	private IChange fUndoChange;
	
	/**
	 * Creates a compilation with a primary type.
	 * @param cu A handle of the compilation unit to create.
	 * @param isClass If set a class will be created. If not, an interface.
	 * @param superTypes The super types of the created type, or <code>null</code> to not add super types.
	 * If the type is a class, then the first entry is the super class, the rest the extended interfaces.
	 * @param settings Code generation settings to be used.
	 */
	public CreateCompilationUnitChange(ICompilationUnit cu, boolean isClass, String[] superTypes, CodeGenerationSettings settings) {
		fCompilationUnit= cu;
		fCodeGenerationSettings= settings;
		fIsClass= isClass;
		fSuperTypes= superTypes;
		
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
				
				if (fCodeGenerationSettings.createFileComments) {
					IBuffer buf= fCompilationUnit.getBuffer();
					buf.replace(0, 0, getTemplate("filecomment", 0)); //$NON-NLS-1$
					buf.save(null, false);
				}

				StringBuffer buf= new StringBuffer();
				if (fCodeGenerationSettings.createComments) {
					buf.append(getTemplate("typecomment", 0)); //$NON-NLS-1$
				}
				ImportsStructure imports= new ImportsStructure(fCompilationUnit, fCodeGenerationSettings.importOrder,  fCodeGenerationSettings.importThreshold, false);
				createTypeStub(fCompilationUnit, fIsClass, fSuperTypes, imports, buf);
				imports.create(true, null);
								
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

	protected void createTypeStub(ICompilationUnit cu, boolean isClass, String[] superTypes, IImportsStructure imports, StringBuffer buf) {
		buf.append("public "); //$NON-NLS-1$
		if (isClass) {
			buf.append(" class "); //$NON-NLS-1$
		} else {
			buf.append(" interface "); //$NON-NLS-1$
		}
		buf.append(Signature.getQualifier(cu.getElementName()));
		
		if (superTypes != null && superTypes.length > 0) {
			int k= 0;
			if (isClass) {
				String superClass= superTypes[0];
				if (!superClass.equals("java.lang.Object")) { //$NON-NLS-1$
					buf.append(" extends "); //$NON-NLS-1$
					buf.append(imports.addImport(superClass));
				}
				if (superTypes.length > 1) {
					buf.append(" implements "); //$NON-NLS-1$
				}				
				k++;
			} else {
				buf.append(" extends "); //$NON-NLS-1$
			}
			boolean commaNeeded= false;
			while (k < superTypes.length) {
				if (commaNeeded) {
					buf.append(", "); //$NON-NLS-1$
				}
				buf.append(imports.addImport(superTypes[k++]));
				commaNeeded= true;
			}
		}
		buf.append(" {\n\n}\n"); //$NON-NLS-1$
	}
	
	protected String getTemplate(String name, int pos) throws CoreException {
		Template[] templates= Templates.getInstance().getTemplates(name);
		if (templates.length > 0) {
			String template= JavaContext.evaluateTemplate(templates[0], fCompilationUnit, pos);
			if (template != null) {
				return template;
			}
		}
		return ""; //$NON-NLS-1$
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
		return RefactoringCoreMessages.getString("CreateCUChange.CreateCU.name"); //$NON-NLS-1$
	}

	/*
	 * @see IChange#getModifiedLanguageElement()
	 */
	public Object getModifiedLanguageElement() {
		return fCreatedType;
	}
}
