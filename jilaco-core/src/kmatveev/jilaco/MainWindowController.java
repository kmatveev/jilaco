package kmatveev.jilaco;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.Utilities;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.*;
import java.util.List;

import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.text.DefaultEditorKit.EndOfLineStringProperty;

public class MainWindowController {

    public static final String ENCODING = "encoding";
    private final AppListener listener;
    
    private final JFrame mainWindow;
    private final JTextPane interactionLog;
    private final JTabbedPane tabbedPane;
    
    private final List<JTextPane> editors = new ArrayList<>();
    private final List<Optional<File>> files = new ArrayList<>();
    private final List<Properties> fileProps = new ArrayList<>();
    private final Icon tabIcon = UIManager.getIcon("FileView.fileIcon");

    private Evaluator evaluator;
    
    private Color echoColor = Color.blue;
    private Color resultColor = Color.black;
    private Color errorColor = Color.decode("0xCD0000"); // Color.red;
    private Color outputColor = Color.decode("0x008000"); // Color.green
    public String defaultEncoding = "UTF-8";
    
    private final ExecuteCurrentLineAction executeCurrentLineAction;
    private final ExecuteSelectionAction executeSelectionAction;
    
    private Properties config;
    
    private float editorFontSize = 14.0f, interactionLogFontSize = 14.0f;
    private final MyCutAction cutAction;
    private final MyCopyAction copyAction;
    private final MyPasteAction pasteAction;

    private boolean useTabIcons = false;

    private final JPopupMenu editorPopupMenu = new JPopupMenu(), interactionLogPopupMenu = new JPopupMenu();

    private final BindingsWindowController bindingsWindowController;

    public MainWindowController(Evaluator evaluator, String title, Properties config, AppListener listener) {
        this.listener = listener;
        this.mainWindow = new JFrame(title);
        this.evaluator = evaluator;

        bindingsWindowController = new BindingsWindowController(this, evaluator);

        // tabbedPane = new JTabbedPane();
        // JComponent tabbedPaneComp = tabbedPane;
        JLayer<? extends JTabbedPane> layerOverTabbedPane = DraggableTabbedPaneFactory.createDraggableTabbedPane();
        tabbedPane = layerOverTabbedPane.getView();
        JComponent tabbedPaneComp = layerOverTabbedPane;
        
        this.config = config;

        fromConfig(config);

        tabbedPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));


        interactionLog = new JTextPane();
        interactionLog.setEditable(false);
        interactionLog.setAutoscrolls(true);
        interactionLog.setFont(interactionLog.getFont().deriveFont(interactionLogFontSize));
        
        JComponent interactionPanel = new JPanel();
        interactionPanel.setLayout(new BorderLayout());
        interactionPanel.add(new JScrollPane(interactionLog, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        JLabel interactionLogCaption = new JLabel("Interaction log:");
        interactionLogCaption.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        interactionLogCaption.setOpaque(true);
        interactionLogCaption.setBackground(UIManager.getColor("TextArea.selectionBackground"));
        interactionLogCaption.setForeground(UIManager.getColor("TextArea.selectionForeground"));
        interactionPanel.add(interactionLogCaption, BorderLayout.NORTH);
        interactionPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabbedPaneComp, interactionPanel);

        mainWindow.getContentPane().add(splitPane, BorderLayout.CENTER);
        
        
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem(new AbstractAction("New") {
            @Override
            public void actionPerformed(ActionEvent e) {
                addEditorTab(createEditor(), Optional.empty(), getDefaultFileProps());
            }
        }));
        fileMenu.add(new JMenuItem(new AbstractAction("Open") {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                JFileChooser chooser = new JFileChooser();
                if (chooser.showOpenDialog(mainWindow) == JFileChooser.APPROVE_OPTION) {

                    JTextPane editor = createEditor(true);
                    File file = chooser.getSelectedFile();
                    addEditorTab(editor, Optional.of(file), getDefaultFileProps(), file.getName());                    
                    
                    try {
                        FileInputStream in = new FileInputStream(file);
                        Reader fileReader = new BufferedReader(new InputStreamReader(in, defaultEncoding));
                        editor.read(fileReader, null); // this will set line-breaks document property, because document is empty
                        in.close();
                    } catch (FileNotFoundException ex) {
                        return;
                    } catch (UnsupportedEncodingException ex) {
                        throw new RuntimeException(ex);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    
                    tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
                    
                }
            }
        }));
        fileMenu.add(new JMenuItem(new AbstractAction("Save") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();

                int selectedIndex = tabbedPane.getSelectedIndex();
                Optional<File> associatedFile = files.get(selectedIndex);
                if (associatedFile.isPresent()) {
                    chooser.setSelectedFile(associatedFile.get());
                }
                
                if (chooser.showSaveDialog(mainWindow) == JFileChooser.APPROVE_OPTION) {

                    JTextPane editor = editors.get(selectedIndex);

                    File file = chooser.getSelectedFile();
                    try {
                        FileOutputStream out = new FileOutputStream(file);
                        Writer fileWriter = new BufferedWriter(new OutputStreamWriter(out, fileProps.get(selectedIndex).getProperty(ENCODING)));
                        editor.write(fileWriter);  // this is a correct way to save a document, with configured line breaks
                        fileWriter.flush();
                        out.flush();
                        out.close();
                    } catch (FileNotFoundException ex) {
                        throw new RuntimeException(ex);
                    } catch (UnsupportedEncodingException ex) {
                        throw new RuntimeException(ex);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    
                    tabbedPane.setTitleAt(selectedIndex, file.getName());
                    files.set(selectedIndex, Optional.of(file));
                }

            }
        }));
        fileMenu.add(new JMenuItem(new AbstractAction("Properties") {
            @Override
            public void actionPerformed(ActionEvent e) {
                new FilePropertiesDialogController(tabbedPane.getSelectedIndex()); 
            }
        }));
        fileMenu.add(new JMenuItem(new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // todo check if file need to be saved
                if (JOptionPane.showConfirmDialog(mainWindow, "Confirm close current tab", UIManager.getString("OptionPane.titleText"), YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    removeTab(MainWindowController.this.tabbedPane.getSelectedIndex());
                }
            }
        }));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(new AbstractAction("Exit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(mainWindow, "Confirm exit", UIManager.getString("OptionPane.titleText"), YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    mainWindow.setVisible(false);
                }
            }
        }));
        menuBar.add(fileMenu);
        
        JMenu editMenu = new JMenu("Edit");

        cutAction = new MyCutAction();
        tabbedPane.addChangeListener(cutAction);
        editMenu.add(new JMenuItem(cutAction));
        editorPopupMenu.add(new JMenuItem(cutAction));

        copyAction = new MyCopyAction();
        tabbedPane.addChangeListener(copyAction);
        editMenu.add(new JMenuItem(copyAction));
        editorPopupMenu.add(new JMenuItem(copyAction));

        pasteAction = new MyPasteAction();
        editMenu.add(new JMenuItem(pasteAction));
        editorPopupMenu.add(new JMenuItem(pasteAction));
        
        editMenu.addSeparator();

        editorPopupMenu.addSeparator();
                
        editMenu.add(new JMenuItem(new AbstractAction("Clear interaction log") {
            @Override
            public void actionPerformed(ActionEvent e) {
                interactionLog.setText("");
            }
        }));
        menuBar.add(editMenu);

        JMenu commandsMenu = new JMenu("Commands");

        executeCurrentLineAction = new ExecuteCurrentLineAction();
        tabbedPane.addChangeListener(executeCurrentLineAction);
        executeCurrentLineAction.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.CTRL_MASK));
        commandsMenu.add(new JMenuItem(executeCurrentLineAction));
        editorPopupMenu.add(new JMenuItem(executeCurrentLineAction));

        executeSelectionAction = new ExecuteSelectionAction();
        tabbedPane.addChangeListener(executeSelectionAction);
        executeSelectionAction.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
        commandsMenu.add(new JMenuItem(executeSelectionAction));
        editorPopupMenu.add(new JMenuItem(executeSelectionAction));
        
        commandsMenu.addSeparator();
        commandsMenu.add(new JMenuItem(new AbstractAction("Reset") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (JOptionPane.showConfirmDialog(mainWindow, "Confirm resetting interpreter", UIManager.getString("OptionPane.titleText"), YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    evaluator.reset();
                }
            }
        }));
        menuBar.add(commandsMenu);

        JMenu exploreMenu = new JMenu("Explore");
        exploreMenu.add(new JMenuItem(new AbstractAction("Bindings") {
            @Override
            public void actionPerformed(ActionEvent e) {
                bindingsWindowController.showBindingsWindow();
            }
        }));
        menuBar.add(exploreMenu);


        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.add(new JMenuItem(new AbstractAction("Settings") {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SettingsDialogController();
            }
        }));
        menuBar.add(toolsMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(new JMenuItem(new AbstractAction("About") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(mainWindow, "Jilaco by kmatveev");
            }
        }));
        menuBar.add(helpMenu);

        
        mainWindow.setJMenuBar(menuBar);

        mainWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // TODO check if files need saving
                super.windowClosing(e);
                listener.appExiting(makeConfig());
                System.exit(0);
            }

            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
            }
        });

        addEditorTab(createEditor(), Optional.empty(), getDefaultFileProps());        


        mainWindow.setSize(500, 400);
        splitPane.setDividerLocation(0.7);
        mainWindow.show();
        
    }

    private Properties getDefaultFileProps() {
        Properties props = new Properties();
        props.setProperty(ENCODING, defaultEncoding);
        return props;
    }

    static void configureLF() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");

        UIManager.getDefaults().put("TabbedPane.tabAreaInsets", new Insets(0,0,0,0));
        UIManager.getDefaults().put("TabbedPane.contentBorderInsets", new Insets(0,0,0,0));
        UIManager.getDefaults().put("TabbedPane.tabsOverlapBorder", true);
    }

    private void addEditorTab(JTextPane editor, Optional<File> file, Properties props) {
        addEditorTab(editor, file, props, "Untitled" + tabbedPane.getTabCount());
    }

    private void addEditorTab(JTextPane editor, Optional<File> file, Properties props, String title) {
        JScrollPane tabContents = new JScrollPane(editor, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        tabbedPane.addTab(title, useTabIcons ? tabIcon : null, tabContents);
        int tabIndex = tabbedPane.getTabCount() - 1; // maybe tabbedPane.indexOfComponent(tabContents) ?
        tabbedPane.setTabComponentAt(tabIndex, CloseButtonTabComponentFactory.createCloseButtonTabComponent(tabbedPane, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // todo check if file need to be saved
                if (JOptionPane.showConfirmDialog(mainWindow, "Confirm close tab", UIManager.getString("OptionPane.titleText"), YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    int tabIdx = tabbedPane.indexOfComponent(tabContents);  // maybe tabbedPane.indexOfTabComponent(e.getSource())
                    removeTab(tabIdx);
                }
            }

        }));
        editors.add(editor);
        files.add(file);
        fileProps.add(props);
    }

    private void removeTab(int selectedIndex) {
        tabbedPane.removeTabAt(selectedIndex);
        editors.remove(selectedIndex);
    }

    private JTextPane createEditor() {
        return createEditor(false);
    }

    private JTextPane createEditor(boolean forLoading) {
        
        if ((executeCurrentLineAction == null ) || (executeSelectionAction == null)) throw new IllegalStateException();
        CaretListener[] listeners = new CaretListener[] {executeCurrentLineAction, executeSelectionAction, cutAction, copyAction};
        
        JTextPane editor = new JTextPane();
        
        if (!forLoading) {
            // this property is not set by default on document, so system property is used when writing. We will set it explicitly
            editor.getDocument().putProperty(EndOfLineStringProperty, System.getProperty("line.separator"));
        }
        
        for (CaretListener listener : listeners) {
            editor.addCaretListener(listener);
        }
        
        editor.setFont(editor.getFont().deriveFont(editorFontSize));
        
        editor.setComponentPopupMenu(editorPopupMenu);

//        Keymap keymap = JTextComponent.addKeymap("jilako", editor.getKeymap());
//        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_MASK), new ExecuteCurrentLineAction(editor));
//        keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK), new ExecuteSelectionAction(editor));
//        editor.setKeymap(keymap);
        
        return editor;
    }

    void evalAndPrint(String rowText, Evaluator evaluator) {

        appendToInteractionLog(echoColor, ">" + rowText + "\n");
        
        String[] result = evaluator.eval(rowText);

        if (result[0] != null) {
            appendToInteractionLog(resultColor, result[0] + "\n");
        }
        
        if (result.length > 1 && result[1] != null) {
            appendToInteractionLog(errorColor, result[1] + "\n");
        }

        if (result.length > 2 && result[2] != null && result[2].length() > 0) {
            appendToInteractionLog(outputColor, result[2] + "\n");
        }
        
    }

    private void appendToInteractionLog(Color color, String str) {
        try {
            MutableAttributeSet attr = new SimpleAttributeSet();
            attr.addAttribute(StyleConstants.Foreground, color);
            interactionLog.getDocument().insertString(interactionLog.getDocument().getEndPosition().getOffset() - 1, str, attr);
            interactionLog.setCaretPosition(interactionLog.getDocument().getLength());
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public static interface Evaluator {
        // item 0 is result, item 1 is error, item 2 is output
        public String[] eval(String expression);
        public void reset();
        public Map<String, Object> getBindings();
    }

    private class MyCutAction extends DefaultEditorKit.CutAction implements ChangeListener, CaretListener {

        public MyCutAction() {
            this.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
            this.putValue(AbstractAction.NAME, "Cut");
        }

        @Override
        public void caretUpdate(CaretEvent e) {
            refreshEnabledState();
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            refreshEnabledState();
        }

        private void refreshEnabledState() {
            if (editors.size() > 0) {
                JTextPane editor = editors.get(tabbedPane.getSelectedIndex());
                setEnabled((editor.getSelectedText() != null) && (editor.getSelectedText().length() > 0));
            } else {
                setEnabled(false);
            }
        }
    }

    private class MyCopyAction extends DefaultEditorKit.CopyAction implements ChangeListener, CaretListener {
        public MyCopyAction() {
            this.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
            this.putValue(AbstractAction.NAME, "Copy");
        }

        @Override
        public void caretUpdate(CaretEvent e) {
            refreshEnabledState();
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            refreshEnabledState();
        }
        
        private void refreshEnabledState() {
            if (editors.size() > 0) {
                JTextPane editor = editors.get(tabbedPane.getSelectedIndex());
                setEnabled((editor.getSelectedText() != null) && (editor.getSelectedText().length() > 0));
            } else {
                setEnabled(false);
            }
        }
        
    }

    private class MyPasteAction extends DefaultEditorKit.PasteAction {
        public MyPasteAction() {
            this.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
            this.putValue(AbstractAction.NAME, "Paste");
        }
    }

    private class ExecuteCurrentLineAction extends AbstractAction implements ChangeListener, CaretListener {

        public ExecuteCurrentLineAction() {
            super("Execute current line");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextPane editor = editors.get(tabbedPane.getSelectedIndex());
            try {
                int caretPosition = editor.getCaretPosition();
                int rowStartPos = Utilities.getRowStart(editor, caretPosition);
                int rowEndPos = Utilities.getRowEnd(editor, caretPosition);
                String rowText = editor.getDocument().getText( rowStartPos, rowEndPos - rowStartPos);
                evalAndPrint(rowText, evaluator);
            } catch (BadLocationException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            
        }

        @Override
        public void caretUpdate(CaretEvent e) {
            
        }
    }

    private class ExecuteSelectionAction extends AbstractAction implements ChangeListener, CaretListener {
        
        public ExecuteSelectionAction() {
            super("Execute selection");

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextPane editor = editors.get(tabbedPane.getSelectedIndex());
            String expression = editor.getSelectedText();
            evalAndPrint(expression, evaluator);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            // this event is received after a different tab is selected
            refreshEnabledState();
        }

        @Override
        public void caretUpdate(CaretEvent e) {
            refreshEnabledState();
        }

        private void refreshEnabledState() {
            if (editors.size() > 0 && tabbedPane.getSelectedIndex() >= 0) {
                JTextPane editor = editors.get(tabbedPane.getSelectedIndex());
                setEnabled((editor.getSelectedText() != null) && (editor.getSelectedText().length() > 0));
            } else {
                setEnabled(false);
            }
        }

    }

    public static Properties loadProperties(String appName) {
        String localAppDataDir = System.getenv("LOCALAPPDATA");
        File myAppDir = new File(localAppDataDir + File.separator + ".jilaco");
        if (myAppDir.exists() && myAppDir.isDirectory()) {
            File propsFile = myAppDir.toPath().resolve(appName + ".properties").toFile();
            if (propsFile.exists()) {
                try {
                    Properties props = new Properties();
                    props.load(new FileInputStream(propsFile));
                    return props;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }

    private void fromConfig(Properties config) {
        echoColor = getColorOrDefault(config, "results.echoColor", echoColor);
        resultColor = getColorOrDefault(config,"results.resultColor", resultColor);
        errorColor = getColorOrDefault(config, "results.errorColor", errorColor);
        outputColor = getColorOrDefault(config, "results.outputColor", outputColor);
        if (config != null && config.getProperty("file.defaultEncoding") != null) {
            defaultEncoding = config.getProperty("file.defaultEncoding");
        }
        if (config != null && config.getProperty("editor.fontSize") != null) {
            editorFontSize = Integer.decode(config.getProperty("editor.fontSize")).floatValue();
        }
        if (config != null && config.getProperty("results.fontSize") != null) {
            interactionLogFontSize = Integer.decode(config.getProperty("results.fontSize")).floatValue();
        }
        if (config != null && config.getProperty("tabs.useIcons") != null) {
            useTabIcons = Boolean.valueOf(config.getProperty("tabs.useIcons"));
        }

        
    }
    
    public Properties makeConfig() {
        Properties props = new Properties();
        props.setProperty("results.echoColor", encodeColor(echoColor));
        props.setProperty("results.resultColor", encodeColor(resultColor));
        props.setProperty("results.errorColor", encodeColor(errorColor));
        props.setProperty("results.outputColor", encodeColor(outputColor));
        props.setProperty("file.defaultEncoding", defaultEncoding);
        props.setProperty("editor.fontSize", String.valueOf((int)editorFontSize));
        props.setProperty("results.fontSize", String.valueOf((int)editorFontSize));
        props.setProperty("tabs.useIcons", String.valueOf(useTabIcons));
        return props;
        
    }

    private static String encodeColor(Color color) {
        return "0x" + Integer.toHexString(color.getRGB() & 0xFFFFFF);
    }

    public static void storeProperties(Properties props, String appName) throws Exception {
        String localAppDataDir = System.getenv("LOCALAPPDATA");
        File myAppDir = new File(localAppDataDir + File.separator + ".jilaco");
        if (!myAppDir.exists() ) {
            myAppDir.mkdirs();
        }

        if (myAppDir.isDirectory()) {
            File propsFile = myAppDir.toPath().resolve(appName + ".properties").toFile();
            try {
                FileOutputStream out = new FileOutputStream(propsFile);
                props.store(out, "");
                out.flush();
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new Exception("File " + myAppDir.getAbsolutePath() + " already exists but it is not a directory!");
        }

    }
    

    public static Color getColorOrDefault(Properties config, String colorName, Color defaultVal) {
        if (config != null) {
            String color = config.getProperty(colorName);
            if (color != null) {
                return Color.decode(color);
            }
        }
        return defaultVal;
    }
    
    public static interface AppListener {
        public void appExiting(Properties props);
    }

    public void updateTabsIcons(boolean useTabIcons) {
        if (this.useTabIcons != useTabIcons) {
            this.useTabIcons = useTabIcons;
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                tabbedPane.setIconAt(i, useTabIcons ? tabIcon : null);
            }
        }
    }

    public void updateEditorFontSize(String newSize) {
        float ff = Float.parseFloat(newSize);
        if (ff != editorFontSize) {
            editorFontSize = ff;
            for (JTextPane editor : editors) {
                editor.setFont(editor.getFont().deriveFont(editorFontSize));
            }
        }
    }

    public void updateInteractionLogFontSize(String newSize) {
        float ff = Float.parseFloat(newSize);
        if (ff != interactionLogFontSize) {
            interactionLogFontSize = ff;
            interactionLog.setFont(interactionLog.getFont().deriveFont(interactionLogFontSize));
        }
    }
    
    
    private class SettingsDialogController {

        private final JTextField editorFontSizeField;
        private final JTextField interactionLogFontSizeField;
        private final JTextField echoColorField;
        private final JTextField resultColorField;
        private final JTextField errorColorField;
        private final JTextField outputColorField;
        private final JCheckBox useIconsInTabsCheckbox;

        public SettingsDialogController() {
            JDialog settingsDialog = new JDialog(mainWindow, "Settings", true);
            settingsDialog.getContentPane().setLayout(new GridBagLayout());
            
            JPanel settingsPanel = new JPanel();
            settingsPanel.setLayout(new GridBagLayout());

            Insets defaultInsets = new Insets(2, 5, 2, 5);
            
            // labels for setting names are right-aligned
            GridBagConstraints col1 = new GridBagConstraints();
            col1.fill = GridBagConstraints.NONE;
            col1.anchor = GridBagConstraints.EAST;
            col1.insets = defaultInsets;
            col1.weightx = 0;
            col1.weighty = 0;
            
            // edit fields for setting values are left-aligned
            GridBagConstraints col2 = new GridBagConstraints();
            col2.fill = GridBagConstraints.HORIZONTAL;
            col2.anchor = GridBagConstraints.NORTHWEST;
            col2.insets = defaultInsets;
            col2.weightx = 0;
            col2.weighty = 0;
            
            // empty space
            GridBagConstraints col3 = new GridBagConstraints();
            col3.gridx = 3;
            col3.fill = GridBagConstraints.HORIZONTAL;
            col3.anchor = GridBagConstraints.NORTHWEST;
            col3.insets = defaultInsets;
            col3.weightx = 1;
            col3.weighty = 0;
            
            GridBagByRowAdder adder = new GridBagByRowAdder(col1, col2, col3);

            useIconsInTabsCheckbox = new JCheckBox("Use icons in tabs", useTabIcons);
            adder.addSingleComponentWholeRow(settingsPanel, useIconsInTabsCheckbox, new Insets(0, 0, 0, 0));
            adder.addSingleComponentWholeRow(settingsPanel, new JSeparator(), new Insets(10, 5, 10, 5));

            editorFontSizeField = new JTextField(String.valueOf(editorFontSize), 15);
            adder.addRow(settingsPanel, new JLabel("Editor font size: "), editorFontSizeField, makeFiller());

            adder.addSingleComponentWholeRow(settingsPanel, new JSeparator(), new Insets(10, 5, 10, 5));

            interactionLogFontSizeField = new JTextField(String.valueOf(interactionLogFontSize), 15);
            adder.addRow(settingsPanel, new JLabel("Interaction log font size: "), interactionLogFontSizeField, makeFiller());
            echoColorField = new JTextField(encodeColor(echoColor));
            adder.addRow(settingsPanel, new JLabel("Echo color: "), echoColorField, makeFiller());
            resultColorField = new JTextField(encodeColor(resultColor));
            adder.addRow(settingsPanel, new JLabel("Result color: "), resultColorField, makeFiller());
            errorColorField = new JTextField(encodeColor(errorColor));
            adder.addRow(settingsPanel, new JLabel("Error color: "), errorColorField, makeFiller());
            outputColorField = new JTextField(encodeColor(outputColor));
            adder.addRow(settingsPanel, new JLabel("Output color: "), outputColorField, makeFiller());

            adder.addBottomFillerTo(settingsPanel);

            settingsDialog.getContentPane().add(settingsPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0 ));

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new FlowLayout());
            buttonsPanel.add(new JButton(new AbstractAction("OK") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Color echoClr;
                    try {
                        echoClr = Color.decode(echoColorField.getText());
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(settingsDialog, "Wrong value of " + "Echo color", "Verification error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    Color resultClr;
                    try {
                        resultClr = Color.decode(resultColorField.getText());
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(settingsDialog, "Wrong value of " + "Result color", "Verification error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    Color errorClr;
                    try {
                        errorClr = Color.decode(errorColorField.getText());
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(settingsDialog, "Wrong value of " + "Error color", "Verification error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    Color outputClr;
                    try {
                        outputClr = Color.decode(outputColorField.getText());
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(settingsDialog, "Wrong value of " + "Error color", "Verification error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    echoColor = echoClr;
                    resultColor = resultClr;
                    errorColor = errorClr;
                    outputColor = outputClr;

                    updateTabsIcons(useIconsInTabsCheckbox.isSelected());
                    
                    updateEditorFontSize(editorFontSizeField.getText());
                    updateInteractionLogFontSize(interactionLogFontSizeField.getText());
                    settingsDialog.hide();
                }
            }));
            buttonsPanel.add(new JButton(new AbstractAction("Cancel") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    settingsDialog.hide();
                }
            }));

            settingsDialog.getContentPane().add(buttonsPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0 ));
            
            settingsDialog.pack();
            settingsDialog.setLocation(mainWindow.getX() + 100, mainWindow.getY() + 100);
            settingsDialog.setResizable(false);
            settingsDialog.show();
            
            
        }
        
    }
    
    public static JComponent makeFiller() {
        JComponent filler = new JPanel();
        filler.setOpaque(false);
        return filler;
    }
    
    public class FilePropertiesDialogController {
        
        private JComboBox encodingField;
        private JComboBox lineSeparatorsField;
        
        public FilePropertiesDialogController(int idx) {
            JDialog filePropertiesDialog = new JDialog(mainWindow, "File properties", true);
            filePropertiesDialog.getContentPane().setLayout(new GridBagLayout());

            JPanel settingsPanel = new JPanel();
            settingsPanel.setLayout(new GridBagLayout());

            Insets defaultInsets = new Insets(2, 5, 2, 5);

            // labels for setting names are right-aligned
            GridBagConstraints col1 = new GridBagConstraints();
            col1.fill = GridBagConstraints.NONE;
            col1.anchor = GridBagConstraints.EAST;
            col1.insets = defaultInsets;
            col1.weightx = 0;
            col1.weighty = 0;

            // edit fields for setting values are left-aligned
            GridBagConstraints col2 = new GridBagConstraints();
            col2.fill = GridBagConstraints.HORIZONTAL;
            col2.anchor = GridBagConstraints.NORTHWEST;
            col2.insets = defaultInsets;
            col2.weightx = 0;
            col2.weighty = 0;

            // empty space
            GridBagConstraints col3 = new GridBagConstraints();
            col3.gridx = 3;
            col3.fill = GridBagConstraints.HORIZONTAL;
            col3.anchor = GridBagConstraints.NORTHWEST;
            col3.insets = defaultInsets;
            col3.weightx = 1;
            col3.weighty = 0;

            GridBagByRowAdder adder = new GridBagByRowAdder(col1, col2, col3);

            encodingField = new JComboBox(new String[] {"UTF-8", "UTF-16"});
            encodingField.setSelectedItem(fileProps.get(idx).getProperty(ENCODING));
            adder.addRow(settingsPanel, new JLabel("File encoding: "), encodingField, makeFiller());
            
            // this property will be set if document was read using doc.read() method
            Object eol = editors.get(idx).getDocument().getProperty(EndOfLineStringProperty);
            lineSeparatorsField = new JComboBox(new String[] {"CRLF", "LF"});
            lineSeparatorsField.setSelectedItem(eol.equals("\r\n") ? "CRLF" : "LF" );
            adder.addRow(settingsPanel, new JLabel("Line separators: "), lineSeparatorsField, makeFiller());

            adder.addBottomFillerTo(settingsPanel);

            filePropertiesDialog.getContentPane().add(settingsPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0 ));

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new FlowLayout());
            buttonsPanel.add(new JButton(new AbstractAction("OK") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    filePropertiesDialog.hide();
                    editors.get(idx).getDocument().putProperty(EndOfLineStringProperty, lineSeparatorsField.getSelectedItem().equals("CRLF") ? "\r\n" : "\n");
                    fileProps.get(idx).setProperty(ENCODING, String.valueOf(encodingField.getSelectedItem()));
                }
            }));
            buttonsPanel.add(new JButton(new AbstractAction("Cancel") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    filePropertiesDialog.hide();
                }
            }));

            filePropertiesDialog.getContentPane().add(buttonsPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0 ));

            filePropertiesDialog.pack();
            filePropertiesDialog.setLocation(mainWindow.getX() + 100, mainWindow.getY() + 100);
            filePropertiesDialog.setResizable(false);
            filePropertiesDialog.show();
            
        }
    }
    
}
