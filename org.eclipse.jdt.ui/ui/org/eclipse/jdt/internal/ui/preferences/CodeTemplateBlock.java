package org.eclipse.jdt.internal.ui.preferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.corext.template.CodeTemplates;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.TemplateSet;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.PixelConverter;
import org.eclipse.jdt.internal.ui.viewsupport.ImageDescriptorRegistry;
import org.eclipse.jdt.internal.ui.viewsupport.JavaElementImageProvider;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;

/**
  */
public class CodeTemplateBlock {
	
	private class CodeTemplateAdapter implements ITreeListAdapter, IDialogFieldListener {

		private final Object[] NO_CHILDREN= new Object[0];
		private CodeTemplates fTemplates;

		public CodeTemplateAdapter(CodeTemplates templates) {
			fTemplates= templates;
		}		

		public void customButtonPressed(TreeListDialogField field, int index) {
			doButtonPressed(index, field.getSelectedElements());
		}
			
		public void selectionChanged(TreeListDialogField field) {
			List selected= field.getSelectedElements();
			field.enableButton(IDX_EDIT, canEdit(selected));
			field.enableButton(IDX_EXPORT, !selected.isEmpty());
			
			updateSourceViewerInput(selected);
		}

		public void doubleClicked(TreeListDialogField field) {
			List selected= field.getSelectedElements();
			if (canEdit(selected)) {
				doButtonPressed(IDX_EDIT, selected);
			}
		}

		public Object[] getChildren(TreeListDialogField field, Object element) {
			if (element == COMMENT_NODE || element == CODE_NODE) {
				return getTemplateOfCategory(element == COMMENT_NODE);
			}
			return NO_CHILDREN;
		}

		public Object getParent(TreeListDialogField field, Object element) {
			if (element instanceof Template) {
				Template template= (Template) element;
				if (template.getName().endsWith(CodeTemplates.COMMENT_SUFFIX)) {
					return COMMENT_NODE;
				}
				return CODE_NODE;
			}
			return null;
		}

		public boolean hasChildren(TreeListDialogField field, Object element) {
			return (element == COMMENT_NODE || element == CODE_NODE);
		}

		public void dialogFieldChanged(DialogField field) {
		}
	
	}
	
	private static class CodeTemplateLabelProvider extends LabelProvider {
	
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			if (element == COMMENT_NODE) {
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_JAVADOCTAG);
			}
			if (element == CODE_NODE) {
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_IMPCONT);
			}
			Template template = (Template) element;
			String name= template.getName();
			if (CodeTemplates.CATCHBLOCK.equals(name)) {
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
			} else if (CodeTemplates.METHODSTUB.equals(name)) {
				return JavaPluginImages.get(JavaPluginImages.IMG_MISC_DEFAULT);
			} else if (CodeTemplates.CONSTRUCTORSTUB.equals(name)) {
				ImageDescriptorRegistry registry= JavaPlugin.getImageDescriptorRegistry();
				return registry.get(new JavaElementImageDescriptor(JavaPluginImages.DESC_MISC_PUBLIC, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE));
			} else if (CodeTemplates.NEWTYPE.equals(name)) {
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CUNIT);
			} else if (CodeTemplates.TYPECOMMENT.equals(name)) {
				return JavaPluginImages.get(JavaPluginImages.IMG_OBJS_CLASS);
			} else if (CodeTemplates.METHODCOMMENT.equals(name)) {
				return JavaPluginImages.get(JavaPluginImages.IMG_MISC_PROTECTED);
			} else if (CodeTemplates.OVERRIDECOMMENT.equals(name)) {
				ImageDescriptorRegistry registry= JavaPlugin.getImageDescriptorRegistry();
				return registry.get(new JavaElementImageDescriptor(JavaPluginImages.DESC_MISC_PROTECTED, JavaElementImageDescriptor.OVERRIDES, JavaElementImageProvider.SMALL_SIZE));
			} else if (CodeTemplates.CONSTRUCTORCOMMENT.equals(name)) {
				ImageDescriptorRegistry registry= JavaPlugin.getImageDescriptorRegistry();
				return registry.get(new JavaElementImageDescriptor(JavaPluginImages.DESC_MISC_PUBLIC, JavaElementImageDescriptor.CONSTRUCTOR, JavaElementImageProvider.SMALL_SIZE));
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (element == COMMENT_NODE || element == CODE_NODE) {
				return (String) element;
			}
			Template template = (Template) element;
			String name= template.getName();
			if (CodeTemplates.CATCHBLOCK.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.catchblock.label"); //$NON-NLS-1$
			} else if (CodeTemplates.METHODSTUB.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.methodstub.label"); //$NON-NLS-1$
			} else if (CodeTemplates.CONSTRUCTORSTUB.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.constructorstub.label"); //$NON-NLS-1$
			} else if (CodeTemplates.NEWTYPE.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.newtype.label"); //$NON-NLS-1$
			} else if (CodeTemplates.TYPECOMMENT.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.typecomment.label"); //$NON-NLS-1$
			} else if (CodeTemplates.METHODCOMMENT.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.methodcomment.label"); //$NON-NLS-1$
			} else if (CodeTemplates.OVERRIDECOMMENT.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.overridecomment.label"); //$NON-NLS-1$
			} else if (CodeTemplates.CONSTRUCTORCOMMENT.equals(name)) {
				return PreferencesMessages.getString("CodeTemplateBlock.constructorcomment.label"); //$NON-NLS-1$
			}
			return template.getDescription();
		}

	}	
	
	private final static int IDX_EDIT= 0;
	private final static int IDX_IMPORT= 2;
	private final static int IDX_EXPORT= 3;
	private final static int IDX_EXPORTALL= 4;
	
	private final static Object COMMENT_NODE= PreferencesMessages.getString("CodeTemplateBlock.templates.comment.node"); //$NON-NLS-1$
	private final static Object CODE_NODE= PreferencesMessages.getString("CodeTemplateBlock.templates.code.node"); //$NON-NLS-1$
	
	private static final String PREF_JAVADOC_STUBS= PreferenceConstants.CODEGEN__JAVADOC_STUBS;
	
	private TreeListDialogField fCodeTemplateTree;
	
	private SelectionButtonDialogField fCreateJavaDocComments;
	
	protected CodeTemplates fTemplates;
	
	private PixelConverter fPixelConverter;
	private SourceViewer fPatternViewer;
	private Control fSWTWidget;

	public CodeTemplateBlock() {
		
		fTemplates= CodeTemplates.getInstance();
		
		CodeTemplateAdapter adapter= new CodeTemplateAdapter(fTemplates);

		String[] buttonLabels= new String[] { 
			/* IDX_EDIT*/ PreferencesMessages.getString("CodeTemplateBlock.templates.edit.button"),	//$NON-NLS-1$
			/* */ null,  
			/* IDX_IMPORT */ PreferencesMessages.getString("CodeTemplateBlock.templates.import.button"), //$NON-NLS-1$
			/* IDX_EXPORT */ PreferencesMessages.getString("CodeTemplateBlock.templates.export.button"), //$NON-NLS-1$
			/* IDX_EXPORTALL */ PreferencesMessages.getString("CodeTemplateBlock.templates.exportall.button") //$NON-NLS-1$

		};		
		fCodeTemplateTree= new TreeListDialogField(adapter, buttonLabels, new CodeTemplateLabelProvider());
		fCodeTemplateTree.setDialogFieldListener(adapter);
		fCodeTemplateTree.setLabelText(PreferencesMessages.getString("CodeTemplateBlock.templates.label")); //$NON-NLS-1$

		fCodeTemplateTree.enableButton(IDX_EXPORT, false);
		fCodeTemplateTree.enableButton(IDX_EDIT, false);
		
		fCodeTemplateTree.addElement(COMMENT_NODE);
		fCodeTemplateTree.addElement(CODE_NODE);
		
		fCreateJavaDocComments= new SelectionButtonDialogField(SWT.CHECK);
		fCreateJavaDocComments.setLabelText(PreferencesMessages.getString("CodeTemplateBlock.createcomment.label")); //$NON-NLS-1$
		fCreateJavaDocComments.setSelection(PreferenceConstants.getPreferenceStore().getBoolean(PREF_JAVADOC_STUBS));
		
		fCodeTemplateTree.selectFirstElement();	
	}
	
	protected Control createContents(Composite parent) {
		fPixelConverter=  new PixelConverter(parent);
		fSWTWidget= parent;
		
		Composite composite=  new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		//layout.marginHeight= 0;
		//layout.marginWidth= 0;
		layout.numColumns= 2;
		composite.setLayout(layout);
		
		fCreateJavaDocComments.doFillIntoGrid(composite, 2);
		
		fCodeTemplateTree.doFillIntoGrid(composite, 3);
		LayoutUtil.setHorizontalSpan(fCodeTemplateTree.getLabelControl(null), 2);
		LayoutUtil.setHorizontalGrabbing(fCodeTemplateTree.getTreeControl(null));
		
		fPatternViewer= createViewer(composite, 2);
		
		return composite;
	
	}
	
	private Shell getShell() {
		if (fSWTWidget != null) {
			return fSWTWidget.getShell();
		}
		return JavaPlugin.getActiveWorkbenchShell();			
	}	
	
	private SourceViewer createViewer(Composite parent, int nColumns) {
		Label label= new Label(parent, SWT.NONE);
		label.setText(PreferencesMessages.getString("CodeTemplateBlock.preview")); //$NON-NLS-1$
		GridData data= new GridData();
		data.horizontalSpan= nColumns;
		label.setLayoutData(data);
		
		SourceViewer viewer= new SourceViewer(parent, null, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		JavaTextTools tools= JavaPlugin.getDefault().getJavaTextTools();
		IDocument document= new Document();
		IDocumentPartitioner partitioner= tools.createDocumentPartitioner();
		document.setDocumentPartitioner(partitioner);
		partitioner.connect(document);		
		viewer.configure(new JavaSourceViewerConfiguration(tools, null));
		viewer.setEditable(false);
		viewer.setDocument(document);
		viewer.getTextWidget().setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		viewer.getTextWidget().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				doKeyInSourcePressed();
			}
		});
	
		Font font= JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT);
		viewer.getTextWidget().setFont(font);
		
		Control control= viewer.getControl();
		data= new GridData(GridData.FILL_BOTH);
		data.horizontalSpan= nColumns;
		data.heightHint= fPixelConverter.convertHeightInCharsToPixels(5);
		control.setLayoutData(data);
		
		return viewer;
	}
	
	protected Template[] getTemplateOfCategory(boolean isComment) {
		ArrayList res=  new ArrayList();
		Template[] templates= fTemplates.getTemplates();
		for (int i= 0; i < templates.length; i++) {
			Template curr= templates[i];
			if (isComment == curr.getName().endsWith(CodeTemplates.COMMENT_SUFFIX)) {
				res.add(curr);
			}
		}
		return (Template[]) res.toArray(new Template[res.size()]);
	}
	
	protected static boolean canEdit(List selected) {
		return selected.size() == 1 && (selected.get(0) instanceof Template);
	}	
	
	protected void updateSourceViewerInput(List selection) {
		if (fPatternViewer == null || fPatternViewer.getTextWidget().isDisposed()) {
			return;
		}
		if (selection.size() == 1 && selection.get(0) instanceof Template) {
			Template template= (Template) selection.get(0);
			fPatternViewer.getDocument().set(template.getPattern());
		} else {
			fPatternViewer.getDocument().set(""); //$NON-NLS-1$
		}		
	}
	
	protected void doKeyInSourcePressed() {
		List selected= fCodeTemplateTree.getSelectedElements();
		if (canEdit(selected)) {
			doButtonPressed(IDX_EDIT, selected);
		}
	}
	
	
	protected void doButtonPressed(int buttonIndex, List selected) {
		if (buttonIndex == IDX_EDIT) {
			edit((Template) selected.get(0));
		} else if (buttonIndex == IDX_EXPORT) {
			export(selected);
		} else if (buttonIndex == IDX_EXPORTALL) {
			exportAll();
		} else {
			import_();
		}
	}
	
	private void edit(Template template) {
		Template newTemplate= new Template(template);
		EditTemplateDialog dialog= new EditTemplateDialog(getShell(), newTemplate, true, false, new String[0]);
		if (dialog.open() == EditTemplateDialog.OK) {
			// changed
			template.setDescription(newTemplate.getDescription());
			template.setPattern(newTemplate.getPattern());
			fCodeTemplateTree.refresh(template);
			fCodeTemplateTree.selectElements(new StructuredSelection(template));
		}
	}
		
	private void import_() {
		FileDialog dialog= new FileDialog(getShell());
		dialog.setText(PreferencesMessages.getString("CodeTemplateBlock.import.title")); //$NON-NLS-1$
		dialog.setFilterExtensions(new String[] {PreferencesMessages.getString("CodeTemplateBlock.import.extension")}); //$NON-NLS-1$
		String path= dialog.open();
		if (path != null) {
			try {
				fTemplates.addFromFile(new File(path), false);
			} catch (CoreException e) {			
				openReadErrorDialog(e);
			}
			fCodeTemplateTree.refresh();
		}
	}
	
	private void exportAll() {
		export(fTemplates);	
	}

	private void export(List selected) {
		TemplateSet templateSet= new TemplateSet(fTemplates.getTemplateTag());
		for (int i= 0; i < selected.size(); i++) {
			Object curr= selected.get(i);
			if (curr instanceof Template) {
				addToTemplateSet(templateSet, (Template) curr);
			} else {
				Template[] templates= getTemplateOfCategory(curr == COMMENT_NODE);
				for (int k= 0; k < templates.length; k++) {
					addToTemplateSet(templateSet, templates[k]);
				}
			}
		}
		export(templateSet);
	}
	
	private void addToTemplateSet(TemplateSet set, Template template) {
		if (set.getFirstTemplate(template.getName()) == null) {
			set.add(template);
		}
	}
	
	private void export(TemplateSet templateSet) {
		FileDialog dialog= new FileDialog(getShell(), SWT.SAVE);
		dialog.setText(PreferencesMessages.getFormattedString("CodeTemplateBlock.export.title", String.valueOf(templateSet.getTemplates().length))); //$NON-NLS-1$
		dialog.setFilterExtensions(new String[] {PreferencesMessages.getString("CodeTemplateBlock.export.extension")}); //$NON-NLS-1$
		dialog.setFileName(PreferencesMessages.getString("CodeTemplateBlock.export.filename")); //$NON-NLS-1$
		String path= dialog.open();
		
		if (path == null)
			return;
		
		File file= new File(path);		

		if (!file.exists() || confirmOverwrite(file)) {
			try {
				templateSet.saveToFile(file);			
			} catch (CoreException e) {			
				JavaPlugin.log(e);
				openWriteErrorDialog(e);
			}		
		}
	}

	private boolean confirmOverwrite(File file) {
		return MessageDialog.openQuestion(getShell(),
			PreferencesMessages.getString("CodeTemplateBlock.export.exists.title"), //$NON-NLS-1$
			PreferencesMessages.getFormattedString("CodeTemplateBlock.export.exists.message", file.getAbsolutePath())); //$NON-NLS-1$
	}

	
	
	public void performDefaults() {
		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		fCreateJavaDocComments.setSelection(prefs.getDefaultBoolean(PREF_JAVADOC_STUBS));

		try {
			fTemplates.restoreDefaults();
		} catch (CoreException e) {
			JavaPlugin.log(e);
			openReadErrorDialog(e);
		}
		
		// refresh
		fCodeTemplateTree.refresh();		
	}
	
	public boolean performOk(boolean enabled) {
		IPreferenceStore prefs= PreferenceConstants.getPreferenceStore();
		prefs.setValue(PREF_JAVADOC_STUBS, fCreateJavaDocComments.isSelected());
		JavaPlugin.getDefault().savePluginPreferences();		
		return true;
	}
	
	public void performCancel() {
		try {
			fTemplates.reset();			
		} catch (CoreException e) {
			openReadErrorDialog(e);
		}
	}
	
	private void openReadErrorDialog(CoreException e) {
		String title= PreferencesMessages.getString("CodeTemplateBlock.error.read.title"); //$NON-NLS-1$
		String message= PreferencesMessages.getString("CodeTemplateBlock.error.read.message"); //$NON-NLS-1$
		ExceptionHandler.handle(e, getShell(), title, message);		
	}
	
	private void openWriteErrorDialog(CoreException e) {
		String title= PreferencesMessages.getString("CodeTemplateBlock.error.write.title"); //$NON-NLS-1$
		String message= PreferencesMessages.getString("CodeTemplateBlock.error.write.message"); //$NON-NLS-1$
		ExceptionHandler.handle(e, getShell(), title, message);	
	}	

}
