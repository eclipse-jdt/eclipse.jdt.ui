package org.eclipse.jdt.internal.ui.reorg;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.Assert;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;

class PasteInCompilationUnitEdit extends SimpleTextEdit {

	private String fSource;
	private int fType;
	private ICompilationUnit fCu;
	
	protected PasteInCompilationUnitEdit(String source, int type, ICompilationUnit cu) {
		Assert.isNotNull(source);
		fSource= source;
		
		Assert.isTrue(type == IJavaElement.PACKAGE_DECLARATION 
						  || type == IJavaElement.IMPORT_CONTAINER
						  || type == IJavaElement.IMPORT_DECLARATION
						  || type == IJavaElement.TYPE);
		fType= type;				  
		
		Assert.isTrue(cu.exists());
		fCu= cu;
	}

	/*
	 * @see TextEdit#copy()
	 */
	public TextEdit copy() {
		return new PasteInCompilationUnitEdit(fSource, fType, fCu);
	}

	/* non Java-doc
	 * @see TextEdit#connect
	 */
	public void connect(TextBufferEditor editor) throws CoreException {	
		setText(fSource);
		setTextRange(new TextRange(computeOffset(), 0));
		super.connect(editor);
	}
	
	private int computeOffset() throws JavaModelException{
		switch(fType){
			case IJavaElement.PACKAGE_DECLARATION:
				return computeOffsetForPackageDeclaration();	
			case IJavaElement.IMPORT_CONTAINER:
				return computeOffsetForImportContainer();
			case IJavaElement.IMPORT_DECLARATION:
				return computeOffsetForImportContainer();// i think it's the same
			case IJavaElement.TYPE:
				return computeOffsetForType();
			default:
				Assert.isTrue(false);
				return -1; //to make the compiler happy
		}
	}
	
	private int computeOffsetForPackageDeclaration() throws JavaModelException{
		//insert before the first one or at the top if none
		IPackageDeclaration[] declarations= fCu.getPackageDeclarations();
		if (declarations.length == 0)
			return 0;
		return declarations[0].getSourceRange().getOffset();	
	}
	
	private int computeOffsetForImportContainer() throws JavaModelException{
		//try prepending the existing one
		IImportContainer container= fCu.getImportContainer();
		if (container.exists())
			return container.getSourceRange().getOffset();

		//try putting after the package declaration
		IPackageDeclaration[] declarations= fCu.getPackageDeclarations();
		if (declarations.length != 0)
			return declarations[declarations.length - 1].getSourceRange().getOffset() 
			      + declarations[declarations.length - 1].getSourceRange().getLength();
		
		//put at the beginning
		return 0;
	}
	
	private int computeOffsetForType()  throws JavaModelException{
		//try prepending to existing types
		IType[] types= fCu.getTypes();
		if (types.length != 0)
			return types[0].getSourceRange().getOffset();
		
		//put at the end
		return fCu.getSourceRange().getLength();	
	}
}

