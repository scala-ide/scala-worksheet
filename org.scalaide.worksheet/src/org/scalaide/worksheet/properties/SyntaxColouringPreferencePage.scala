package org.scalaide.worksheet.properties

import scala.PartialFunction.condOpt
import org.scalaide.ui.internal.preferences.GridDataHelper.gridData
import org.scalaide.ui.syntax.ScalaSyntaxClass
import org.scalaide.util.internal.eclipse.EclipseUtils
import org.scalaide.util.internal.eclipse.SWTUtils.fnToDoubleClickListener
import org.scalaide.util.internal.eclipse.SWTUtils.fnToPropertyChangeListener
import org.scalaide.util.internal.eclipse.SWTUtils.fnToSelectionAdapter
import org.scalaide.util.internal.eclipse.SWTUtils.noArgFnToSelectionAdapter
import org.scalaide.util.internal.eclipse.SWTUtils.noArgFnToSelectionChangedListener

import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.BOOLEAN
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.OverlayKey
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.STRING
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages
import org.eclipse.jdt.internal.ui.preferences.ScrolledPageContent
import org.eclipse.jface.layout.PixelConverter
import org.eclipse.jface.preference.ColorSelector
import org.eclipse.jface.preference.PreferenceConverter
import org.eclipse.jface.preference.PreferencePage
import org.eclipse.jface.text.source.SourceViewer
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.jface.viewers.DoubleClickEvent
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.jface.viewers.StructuredSelection
import org.eclipse.jface.viewers.TreeViewer
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Link
import org.eclipse.swt.widgets.Scrollable
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.eclipse.ui.dialogs.PreferencesUtil
import org.scalaide.worksheet.WorksheetPlugin
import org.scalaide.worksheet.lexical.SyntaxClasses


/**
 * @see org.eclipse.jdt.internal.ui.preferences.JavaEditorColoringConfigurationBlock
 */
class SyntaxColouringPreferencePage extends PreferencePage with IWorkbenchPreferencePage {

  setPreferenceStore(WorksheetPlugin.plugin.getPreferenceStore)
  private val overlayStore = makeOverlayPreferenceStore

  private var foregroundColorEditorLabel: Label = _
  private var syntaxForegroundColorEditor: ColorSelector = _
  private var backgroundColorEditorLabel: Label = _
  private var syntaxBackgroundColorEditor: ColorSelector = _
  private var enabledCheckBox: Button = _
  private var backgroundColorEnabledCheckBox: Button = _
  private var foregroundColorButton: Button = _
  private var backgroundColorButton: Button = _
  private var boldCheckBox: Button = _
  private var italicCheckBox: Button = _
  private var underlineCheckBox: Button = _
  private var treeViewer: TreeViewer = _
  private var previewer: SourceViewer = _

  override def init(workbench: IWorkbench) {}

  override def createContents(parent: Composite): Control = {
    initializeDialogUnits(parent)

    val scrolled = new ScrolledPageContent(parent, SWT.H_SCROLL | SWT.V_SCROLL)
    scrolled.setExpandHorizontal(true)
    scrolled.setExpandVertical(true)

    val control = createSyntaxPage(scrolled)

    scrolled.setContent(control)
    val size = control.computeSize(SWT.DEFAULT, SWT.DEFAULT)
    scrolled.setMinSize(size.x, size.y)

    scrolled
  }

  import OverlayPreferenceStore._
  private def makeOverlayKeys(syntaxClass: ScalaSyntaxClass): List[OverlayKey] = {
    List(
      new OverlayKey(BOOLEAN, syntaxClass.enabledKey),
      new OverlayKey(STRING, syntaxClass.foregroundColourKey),
      new OverlayKey(STRING, syntaxClass.backgroundColourKey),
      new OverlayKey(BOOLEAN, syntaxClass.backgroundColourEnabledKey),
      new OverlayKey(BOOLEAN, syntaxClass.boldKey),
      new OverlayKey(BOOLEAN, syntaxClass.italicKey),
      new OverlayKey(BOOLEAN, syntaxClass.underlineKey))
  }

  def makeOverlayPreferenceStore = {
    val keys =
      SyntaxClasses.AllSyntaxClasses.flatMap(makeOverlayKeys)
    new OverlayPreferenceStore(getPreferenceStore, keys.toArray)
  }

  override def performOk() = {
    super.performOk()
    overlayStore.propagate()
    WorksheetPlugin.plugin.savePluginPreferences()
    true
  }

  override def dispose() {
    overlayStore.stop()
    foregroundColorEditorLabel.dispose()
    backgroundColorEditorLabel.dispose()
    enabledCheckBox.dispose()
    backgroundColorEnabledCheckBox.dispose()
    foregroundColorButton.dispose()
    backgroundColorButton.dispose()
    boldCheckBox.dispose()
    italicCheckBox.dispose()
    underlineCheckBox.dispose()
    super.dispose()
  }

  override def performDefaults() {
    super.performDefaults()
    overlayStore.loadDefaults()
    handleSyntaxColorListSelection()
  }

  def createTreeViewer(editorComposite: Composite) {
    val contentAndLabelProvider = new SyntaxColouringTreeContentAndLabelProvider
    treeViewer = new TreeViewer(editorComposite, SWT.SINGLE | SWT.BORDER)
    treeViewer.setContentProvider(contentAndLabelProvider)
    treeViewer.setLabelProvider(contentAndLabelProvider)

    // scrollbars and tree indentation guess
    val widthHint = SyntaxClasses.AllSyntaxClasses.map { syntaxClass => convertWidthInCharsToPixels(syntaxClass.displayName.length) }.max +
      Option(treeViewer.getControl.asInstanceOf[Scrollable].getVerticalBar).map { _.getSize.x * 3 }.getOrElse(0)

    treeViewer.getControl.setLayoutData(gridData(
      horizontalAlignment = SWT.BEGINNING,
      verticalAlignment = SWT.BEGINNING,
      grabExcessHorizontalSpace = false,
      grabExcessVerticalSpace = true,
      widthHint = widthHint,
      heightHint = convertHeightInCharsToPixels(11)))

    treeViewer.addDoubleClickListener { event: DoubleClickEvent =>
      val element = event.getSelection.asInstanceOf[IStructuredSelection].getFirstElement
      if (treeViewer.isExpandable(element))
        treeViewer.setExpandedState(element, !treeViewer.getExpandedState(element))
    }

    treeViewer.addSelectionChangedListener { () =>
      handleSyntaxColorListSelection()
    }

    treeViewer.setInput(new Object)
  }

  private def gridLayout(marginHeight: Int = 5, marginWidth: Int = 5, numColumns: Int = 1): GridLayout = {
    val layout = new GridLayout
    layout.marginHeight = marginHeight
    layout.marginWidth = marginWidth
    layout.numColumns = numColumns
    layout
  }

  def createSyntaxPage(parent: Composite): Control = {
    overlayStore.load()
    overlayStore.start()

    val outerComposite = new Composite(parent, SWT.NONE)
    outerComposite.setLayout(gridLayout(marginHeight = 0, marginWidth = 0))

    val link = new Link(outerComposite, SWT.NONE)
    link.setText(PreferencesMessages.JavaEditorColoringConfigurationBlock_link)
    link.addSelectionListener { e: SelectionEvent =>
      PreferencesUtil.createPreferenceDialogOn(parent.getShell, e.text, null, null)
    }
    link.setLayoutData(gridData(
      horizontalAlignment = SWT.FILL,
      verticalAlignment = SWT.BEGINNING,
      grabExcessHorizontalSpace = true,
      grabExcessVerticalSpace = false,
      widthHint = 150,
      horizontalSpan = 2))

    val filler = new Label(outerComposite, SWT.LEFT)
    filler.setLayoutData(gridData(
      horizontalAlignment = SWT.FILL,
      horizontalSpan = 1,
      heightHint = new PixelConverter(outerComposite).convertHeightInCharsToPixels(1) / 2))

    val elementLabel = new Label(outerComposite, SWT.LEFT)
    elementLabel.setText(PreferencesMessages.JavaEditorPreferencePage_coloring_element)
    elementLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL))

    val elementEditorComposite = new Composite(outerComposite, SWT.NONE)
    elementEditorComposite.setLayout(gridLayout(marginHeight = 0, marginWidth = 0, numColumns = 2))
    elementEditorComposite.setLayoutData(gridData(
      horizontalAlignment = SWT.FILL, verticalAlignment = SWT.BEGINNING, grabExcessHorizontalSpace = true,
      grabExcessVerticalSpace = false))

    createTreeViewer(elementEditorComposite)

    val stylesComposite = new Composite(elementEditorComposite, SWT.NONE)
    stylesComposite.setLayout(gridLayout(marginHeight = 0, marginWidth = 0, numColumns = 2))
    stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH))

    enabledCheckBox = new Button(stylesComposite, SWT.CHECK)
    enabledCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_enable)
    enabledCheckBox.setLayoutData(gridData(
      horizontalAlignment = GridData.BEGINNING, horizontalIndent = 0, horizontalSpan = 2))

    foregroundColorEditorLabel = new Label(stylesComposite, SWT.LEFT)
    foregroundColorEditorLabel.setText("Foreground:")

    foregroundColorEditorLabel.setLayoutData(gridData(horizontalAlignment = GridData.BEGINNING, horizontalIndent = 20))

    syntaxForegroundColorEditor = new ColorSelector(stylesComposite)
    foregroundColorButton = syntaxForegroundColorEditor.getButton
    foregroundColorButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING))

    backgroundColorEditorLabel = new Label(stylesComposite, SWT.LEFT)
    backgroundColorEditorLabel.setText("Background:")

    backgroundColorEditorLabel.setLayoutData(gridData(horizontalAlignment = GridData.BEGINNING, horizontalIndent = 20))

    syntaxBackgroundColorEditor = new ColorSelector(stylesComposite)
    backgroundColorButton = syntaxBackgroundColorEditor.getButton
    backgroundColorButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING))

    backgroundColorEnabledCheckBox = new Button(stylesComposite, SWT.CHECK)
    backgroundColorEnabledCheckBox.setText("Paint background")

    backgroundColorEnabledCheckBox.setLayoutData(gridData(
      horizontalAlignment = GridData.BEGINNING, horizontalIndent = 20, horizontalSpan = 2))

    boldCheckBox = new Button(stylesComposite, SWT.CHECK)
    boldCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_bold)

    boldCheckBox.setLayoutData(gridData(
      horizontalAlignment = GridData.BEGINNING, horizontalIndent = 20, horizontalSpan = 2))

    italicCheckBox = new Button(stylesComposite, SWT.CHECK)
    italicCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_italic)
    italicCheckBox.setLayoutData(gridData(
      horizontalAlignment = GridData.BEGINNING, horizontalIndent = 20, horizontalSpan = 2))

    underlineCheckBox = new Button(stylesComposite, SWT.CHECK)
    underlineCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_underline)
    underlineCheckBox.setLayoutData(
      gridData(horizontalAlignment = GridData.BEGINNING, horizontalIndent = 20, horizontalSpan = 2))

    val previewLabel = new Label(outerComposite, SWT.LEFT)
    previewLabel.setText(PreferencesMessages.JavaEditorPreferencePage_preview)
    previewLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL))

    previewer = createPreviewer(outerComposite)
    val previewerControl = previewer.getControl
    previewerControl.setLayoutData(gridData(
      horizontalAlignment = GridData.FILL,
      verticalAlignment = GridData.FILL,
      grabExcessHorizontalSpace = true,
      grabExcessVerticalSpace = true,
      widthHint = convertWidthInCharsToPixels(20),
      heightHint = convertHeightInCharsToPixels(12)))

    setUpSelectionListeners()

    treeViewer.setSelection(new StructuredSelection(SyntaxClasses.EvalResult.FirstLine))

    outerComposite.layout(false)
    outerComposite
  }

  private def setUpSelectionListeners() {
    enabledCheckBox.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass)
        overlayStore.setValue(syntaxClass.enabledKey, enabledCheckBox.getSelection)
    }
    foregroundColorButton.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass)
        PreferenceConverter.setValue(overlayStore, syntaxClass.foregroundColourKey, syntaxForegroundColorEditor.getColorValue)
    }
    backgroundColorButton.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass)
        PreferenceConverter.setValue(overlayStore, syntaxClass.backgroundColourKey, syntaxBackgroundColorEditor.getColorValue)
    }
    backgroundColorEnabledCheckBox.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass) {
        overlayStore.setValue(syntaxClass.backgroundColourEnabledKey, backgroundColorEnabledCheckBox.getSelection)
        backgroundColorButton.setEnabled(backgroundColorEnabledCheckBox.getSelection)
      }
    }
    boldCheckBox.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass)
        overlayStore.setValue(syntaxClass.boldKey, boldCheckBox.getSelection)
    }
    italicCheckBox.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass)
        overlayStore.setValue(syntaxClass.italicKey, italicCheckBox.getSelection)
    }
    underlineCheckBox.addSelectionListener { () =>
      for (syntaxClass <- selectedSyntaxClass)
        overlayStore.setValue(syntaxClass.underlineKey, underlineCheckBox.getSelection)
    }
  }

  private def createPreviewer(parent: Composite): SourceViewer = {
    val preview = new SyntaxColouringPreviewText
    PreviewerFactory.createPreviewer(parent, overlayStore, preview.previewText)
  }

  private def selectedSyntaxClass: Option[ScalaSyntaxClass] = condOpt(treeViewer.getSelection) {
    case EclipseUtils.SelectedItems(syntaxClass: ScalaSyntaxClass) => syntaxClass
  }

  private def massSetEnablement(enabled: Boolean) = {
    val widgets = List(enabledCheckBox, syntaxForegroundColorEditor.getButton, foregroundColorEditorLabel,
      syntaxBackgroundColorEditor.getButton, backgroundColorEditorLabel, backgroundColorEnabledCheckBox, boldCheckBox,
      italicCheckBox, underlineCheckBox)
    widgets foreach { _.setEnabled(enabled) }
  }

  private def handleSyntaxColorListSelection() = selectedSyntaxClass match {
    case None =>
      massSetEnablement(false)
    case Some(syntaxClass) =>
      import syntaxClass._
      import EclipseUtils._
      syntaxForegroundColorEditor.setColorValue(overlayStore getColor foregroundColourKey)
      syntaxBackgroundColorEditor.setColorValue(overlayStore getColor backgroundColourKey)
      val backgroundColorEnabled = overlayStore getBoolean backgroundColourEnabledKey
      backgroundColorEnabledCheckBox.setSelection(backgroundColorEnabled)
      enabledCheckBox.setSelection(overlayStore getBoolean enabledKey)
      boldCheckBox.setSelection(overlayStore getBoolean boldKey)
      italicCheckBox.setSelection(overlayStore getBoolean italicKey)
      underlineCheckBox.setSelection(overlayStore getBoolean underlineKey)

      massSetEnablement(true)
      enabledCheckBox.setEnabled(canBeDisabled)
      syntaxBackgroundColorEditor.getButton.setEnabled(backgroundColorEnabled)
  }

}
