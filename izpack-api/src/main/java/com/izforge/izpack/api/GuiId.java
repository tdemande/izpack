package com.izforge.izpack.api;

/**
 * Enum with identifier of frames
 */
public enum GuiId
{

    BUTTON_NEXT("nextButton"), BUTTON_HELP("HelpButton"), BUTTON_PREV("prevButton"), BUTTON_QUIT("quitButton"),
    BUTTON_LANG_OK("okButtonLang"), COMBO_BOX_LANG_FLAG("comboBox-lang-flag"), DIALOG_PICKER("dialogPicker"), LICENCE_NO_RADIO("LicenceNoRadio"), LICENCE_YES_RADIO("LicenceYesRadio"), FINISH_PANEL_AUTO_BUTTON("finishPanel_autoButton"), FINISH_PANEL_FILE_CHOOSER("finishPanelFileChooser"), LICENCE_TEXT_AREA("licenceTextArea"), INFO_PANEL_TEXT_AREA("InfoPanelTextArea"), SHORTCUT_CREATE_CHECK_BOX("shortcutCreateCheckBox"), HTML_INFO_PANEL_TEXT("htmlInfoPanelText"), SIMPLE_FINISH_LABEL("simpleFinishLabel"), FINISH_PANEL_LABEL("finishLabel"), HELLO_PANEL_LABEL("helloPanelLabel"), SIMPLE_FINISH_UNINSTALL_LABEL("simpleFinishUninstallLabel"),
    HELP_WINDOWS("helpWindows"), BUTTON_BROWSE("browseButton"), TARGET_PANEL_FILE_CHOOSER("targetPanelFileChooser");

    public String id;

    GuiId(String id)
    {
        this.id = id;
    }

    @Override
    public String toString()
    {
        return id;
    }

}
