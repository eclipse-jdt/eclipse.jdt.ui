package org.eclipse.jdt.internal.corext.refactoring.reorg;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.refactoring.text.ITextBuffer;
import org.eclipse.jdt.internal.corext.refactoring.text.SimpleReplaceTextChange;
import org.eclipse.jdt.internal.corext.refactoring.text.SimpleTextChange;
import org.eclipse.jdt.internal.corext.refactoring.util.TextBufferChangeManager;

public class AddImportUtil {
	
	//no instances
	private AddImportUtil(){
	}
	
	public static boolean addImport(TextBufferChangeManager changeManager, IType type, ICompilationUnit cu) throws JavaModelException {
		if (cu.getImport(type.getFullyQualifiedName()).exists())
			return false;
		if (cu.getImport(type.getPackageFragment().getElementName() + ".*").exists()) //$NON-NLS-1$
			return false;	
		
		changeManager.addSimpleTextChange(cu, addImport(type.getFullyQualifiedName() + ";", cu));
		return true;	
	}
	
	public static boolean addImport(TextBufferChangeManager changeManager, IPackageFragment pack, ICompilationUnit cu) throws JavaModelException {
		if (cu.getImport(pack.getElementName() + ".*").exists()) //$NON-NLS-1$
			return false;
					
		changeManager.addSimpleTextChange(cu, addImport(pack.getElementName() + ".*;", cu));
		return true;
	}
	
	private static SimpleReplaceTextChange addImport(final String importString, ICompilationUnit cu)throws JavaModelException {
		return new SimpleReplaceTextChange("add import", 1 + computeImportInsertionOffset(cu)){
			public SimpleTextChange[] adjust(ITextBuffer buffer) {
				String lineDelimiter= buffer.getLineDelimiter(buffer.getLineOfOffset(getOffset()));
				setText(lineDelimiter + "import " + importString);//$NON-NLS-2$ 
				return null;
			}
		};
	}
	
	private static int computeImportInsertionOffset(ICompilationUnit cu) throws JavaModelException {
		IImportContainer importContainer= cu.getImportContainer();
		if (importContainer.exists()){
			ISourceRange sr= importContainer.getSourceRange();
			return sr.getOffset() + sr.getLength() - 1;
		}
		
		IPackageDeclaration declars[]= cu.getPackageDeclarations();
		if (declars.length == 0)
			return 0;
		ISourceRange sr= declars[declars.length - 1].getSourceRange();
		return sr.getOffset() + sr.getLength() - 1;
	}
	
}

