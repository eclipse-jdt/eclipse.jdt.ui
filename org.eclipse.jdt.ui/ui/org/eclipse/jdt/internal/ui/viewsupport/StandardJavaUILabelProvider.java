package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.AppearancePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;

/**
 * JavaUILabelProvider that respects settings from the Appearance preference page.
 * Triggers a viewer update when a preference changes.
 */
public class StandardJavaUILabelProvider extends JavaUILabelProvider implements IPropertyChangeListener {

	public final static int DEFAULT_TEXTFLAGS= JavaElementLabels.ROOT_VARIABLE | JavaElementLabels.M_PARAMETER_TYPES |  JavaElementLabels.M_APP_RETURNTYPE;
	public final static int DEFAULT_IMAGEFLAGS= JavaElementImageProvider.OVERLAY_ICONS;
	
	private int fTextFlagMask;
	private int fImageFlagMask;

	/**
	 * Constructor for StandardJavaUILabelProvider.
	 * @see JavaUILabelProvider#JavaUILabelProvider
	 */
	public StandardJavaUILabelProvider(int textFlags, int imageFlags, IAdornmentProvider[] adormentProviders) {
		super(textFlags, imageFlags, adormentProviders);
		initMasks();
		JavaPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
	}

	/**
	 * Creates a StandardJavaUILabelProvider with DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS
	 * and the ErrorTickAdornmentProvider.
	 */	
	public StandardJavaUILabelProvider() {
		this(DEFAULT_TEXTFLAGS, DEFAULT_IMAGEFLAGS, new IAdornmentProvider[] { new ErrorTickAdornmentProvider() });
	}
	
	private void initMasks() {
		fTextFlagMask= -1;
		if (!AppearancePreferencePage.showMethodReturnType()) {
			fTextFlagMask ^= JavaElementLabels.M_APP_RETURNTYPE;
		}
		if (!WorkInProgressPreferencePage.isCompressingPkgNameInPackagesView()) {
			fTextFlagMask ^= JavaElementLabels.P_COMPRESSED;
		}
		
		fImageFlagMask= -1;
	}

	/*
	 * @see IPropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String property= event.getProperty();
		if (property.equals(AppearancePreferencePage.PREF_METHOD_RETURNTYPE)
				|| property.equals(AppearancePreferencePage.PREF_OVERRIDE_INDICATOR)
				|| property.equals(WorkInProgressPreferencePage.PREF_PKG_NAME_PATTERN_FOR_PKG_VIEW)) {
			initMasks();
			LabelProviderChangedEvent lpEvent= new LabelProviderChangedEvent(this, null); // refresh all
			fireLabelProviderChanged(lpEvent);
		}		
	}

	/*
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
		JavaPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		super.dispose();
	}

	/*
	 * @see JavaUILabelProvider#getImageFlags()
	 */
	public int getImageFlags() {
		return super.getImageFlags() & fImageFlagMask;
	}

	/*
	 * @see JavaUILabelProvider#getTextFlags()
	 */
	public int getTextFlags() {
		return super.getTextFlags() & fTextFlagMask;
	}

}
