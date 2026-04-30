import javax.swing.*;
import javax.swing.text.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MiniWordPad extends JFrame {

    // Core Components 
    private final JTextPane textPane;
    private final StyledDocument doc;
    private final JComboBox<String> fontSizeBox;
    private final JComboBox<String> fontFamilyBox;
    private final JLabel wordCountLabel;
    private final JLabel statusLabel;
    private File currentFile = null;
    private boolean isModified = false;

    // Color Palette
    private static final Color CLR_BG       = new Color(28, 28, 35);
    private static final Color CLR_TOOLBAR  = new Color(38, 38, 48);
    private static final Color CLR_MENUBAR  = new Color(22, 22, 30);
    private static final Color CLR_EDITOR   = new Color(245, 245, 248);
    private static final Color CLR_ACCENT   = new Color(99, 179, 237);
    private static final Color CLR_ACCENT2  = new Color(154, 117, 234);
    private static final Color CLR_FG_LIGHT = new Color(210, 210, 220);
    private static final Color CLR_FG_MUTED = new Color(130, 130, 150);
    private static final Color CLR_BTN_HOVER= new Color(55, 55, 70);
    private static final Color CLR_STATUS   = new Color(18, 18, 25);

    public MiniWordPad() {
        super("Mini WordPad (Java Swing)");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(1000, 720);
        setMinimumSize(new Dimension(700, 500));
        setLocationRelativeTo(null);

        //  Editor 
        textPane = new JTextPane();
        doc = textPane.getStyledDocument();
        textPane.setFont(new Font("Georgia", Font.PLAIN, 14));
        textPane.setBackground(CLR_EDITOR);
        textPane.setForeground(new Color(30, 30, 40));
        textPane.setCaretColor(new Color(99, 179, 237));
        textPane.setMargin(new Insets(16, 20, 16, 20));
        textPane.setSelectionColor(new Color(99, 179, 237, 80));

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CLR_EDITOR);

        // Toolbar pieces 
        String[] fontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        fontFamilyBox = new JComboBox<>(fontFamilies);
        fontFamilyBox.setSelectedItem("Georgia");
        fontFamilyBox.setMaximumSize(new Dimension(170, 28));
        fontFamilyBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        String[] sizes = {"8","9","10","11","12","14","16","18","20","22","24","28","32","36","48","72"};
        fontSizeBox = new JComboBox<>(sizes);
        fontSizeBox.setSelectedItem("14");
        fontSizeBox.setMaximumSize(new Dimension(60, 28));
        fontSizeBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        wordCountLabel = new JLabel("Words: 0");
        wordCountLabel.setForeground(CLR_FG_MUTED);
        wordCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(CLR_FG_MUTED);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        // Assemble UI 
        setJMenuBar(buildMenuBar());
        add(buildToolBar(), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        //  Listeners 
        wireListeners();

        //  Window close guard
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { exitApp(); }
        });

        // Look & Feel tweaks 
        getContentPane().setBackground(CLR_BG);
        setVisible(true);
    }

    //  MENU BAR

    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.setBackground(CLR_MENUBAR);
        mb.setForeground(Color.WHITE);
        mb.setOpaque(true);      

        mb.setBorder(BorderFactory.createMatteBorder(0,0,1,0, new Color(50,50,65)));

        mb.add(buildMenu("File",
            menuItem("New",           "ctrl N",        e -> fileNew()),
            menuItem("Open...",       "ctrl O",        e -> fileOpen()),
            menuItem("Save",          "ctrl S",        e -> fileSave()),
            menuItem("Save As...",    "ctrl shift S",  e -> fileSaveAs()),
            null,
            menuItem("Exit",          "alt F4",        e -> exitApp())
        ));

        mb.add(buildMenu("Edit",
            menuItem("Cut",           "ctrl X",   e -> textPane.cut()),
            menuItem("Copy",          "ctrl C",   e -> textPane.copy()),
            menuItem("Paste",         "ctrl V",   e -> textPane.paste()),
            null,
            menuItem("Select All",    "ctrl A",   e -> textPane.selectAll()),
            null,
            menuItem("Find & Replace...", "ctrl H", e -> showFindReplace())
        ));

        mb.add(buildMenu("Format",
            menuItem("Text Color...",       null, e -> changeTextColor()),
            menuItem("Background Color...", null, e -> changeBackgroundColor()),
            null,
            menuItem("Bold",      "ctrl B",  e -> toggleBold()),
            menuItem("Italic",    "ctrl I",  e -> toggleItalic()),
            menuItem("Underline", "ctrl U",  e -> toggleUnderline()),
            null,
            menuItem("UPPERCASE",           null, e -> convertCase(true)),
            menuItem("lowercase",           null, e -> convertCase(false))
        ));

        mb.add(buildMenu("Insert",
            menuItem("Date & Time", "F5", e -> insertDateTime()),
            menuItem("Word Count",  null, e -> showWordCount())
        ));

        mb.add(buildMenu("Help",
            menuItem("About Mini WordPad", null, e -> showAbout())
        ));

        return mb;
    }

    private JMenu buildMenu(String title, Object... items) {
        JMenu menu = new JMenu(title);
          menu.setOpaque(false);                
          menu.setBackground(null);      
          menu.setForeground(Color.BLACK);  

        menu.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        menu.getPopupMenu().setBackground(CLR_TOOLBAR);
        menu.getPopupMenu().setBorder(BorderFactory.createLineBorder(new Color(60,60,80)));
        for (Object item : items) {
            if (item == null) menu.addSeparator();
            else menu.add((JMenuItem) item);
        }
        return menu;
    }

    private JMenuItem menuItem(String text, String accel, ActionListener al) {
        JMenuItem mi = new JMenuItem(text);
        mi.setForeground(CLR_FG_LIGHT);
        mi.setBackground(CLR_TOOLBAR);
        mi.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        if (accel != null) mi.setAccelerator(KeyStroke.getKeyStroke(accel));
        mi.addActionListener(al);
        mi.setBorder(BorderFactory.createEmptyBorder(4,8,4,24));
        return mi;
    }

    //  TOOLBAR

    private JPanel buildToolBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 5));
        bar.setBackground(CLR_TOOLBAR);
        bar.setBorder(BorderFactory.createMatteBorder(0,0,1,0, new Color(50,50,65)));

        // File group
        bar.add(toolBtn("New",   "New",   e -> fileNew()));
        bar.add(toolBtn("Open",  "Open",  e -> fileOpen()));
        bar.add(toolBtn("Save",  "Save",  e -> fileSave()));
        bar.add(toolSep());

        // Edit group
        bar.add(toolBtn("Cut",   "Cut",   e -> textPane.cut()));
        bar.add(toolBtn("Copy",  "Copy",  e -> textPane.copy()));
        bar.add(toolBtn("Paste", "Paste", e -> textPane.paste()));
        bar.add(toolSep());

        // Format group — bold/italic/underline labels
        JButton btnBold = toolBtn("Bold",      "B",  e -> toggleBold());
        btnBold.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JButton btnItalic = toolBtn("Italic",  "I",  e -> toggleItalic());
        btnItalic.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        JButton btnUnder = toolBtn("Underline","U",  e -> toggleUnderline());
        bar.add(btnBold); bar.add(btnItalic); bar.add(btnUnder);
        bar.add(toolSep());

        // Font family
        bar.add(styledLabel("Font:"));
        bar.add(fontFamilyBox);
        bar.add(Box.createHorizontalStrut(4));

        // Font size
        bar.add(styledLabel("Size:"));
        bar.add(fontSizeBox);
        bar.add(toolSep());

        // Color
        bar.add(toolBtn("Text Color",       "A",  e -> changeTextColor()));
        bar.add(toolBtn("Background Color", "BG", e -> changeBackgroundColor()));
        bar.add(toolSep());

        // Special features
        bar.add(toolBtn("Find & Replace", "Find",  e -> showFindReplace()));
        bar.add(toolBtn("Insert Date/Time","Date", e -> insertDateTime()));
        bar.add(toolBtn("Word Count",     "Words", e -> showWordCount()));

        return bar;
    }
private JButton toolBtn(String tooltip, String label, ActionListener al) {
    JButton btn = new JButton(label);
    btn.setToolTipText(tooltip);

    btn.setFocusPainted(false);

    // BUTTON VISIBILITY
    btn.setBorderPainted(false);
    btn.setOpaque(true);
    btn.setBackground(CLR_TOOLBAR);
    btn.setForeground(Color.WHITE);

    btn.setFont(new Font("Segoe UI", Font.BOLD, 12)); // 👈 bold for clear text
    btn.setPreferredSize(new Dimension(70, 28));      // 👈 size thoda bada

    btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    btn.addActionListener(al);

    // Hover effect

    btn.addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent e) { btn.setBackground(CLR_BTN_HOVER); }
        public void mouseExited(MouseEvent e)  { btn.setBackground(CLR_TOOLBAR); }
    });

    return btn;
}

    private JSeparator toolSep() {
        JSeparator s = new JSeparator(SwingConstants.VERTICAL);
        s.setPreferredSize(new Dimension(1, 22));
        s.setForeground(new Color(60, 60, 80));
        return s;
    }

    private JLabel styledLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(CLR_FG_MUTED);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return lbl;
    }

    //  STATUS BAR

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(CLR_STATUS);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1,0,0,0, new Color(50,50,65)),
            BorderFactory.createEmptyBorder(3,10,3,10)
        ));
        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(wordCountLabel, BorderLayout.EAST);
        return bar;
    }

    //  LISTENERS

    private void wireListeners() {
        // Track modifications + live word count
        doc.addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { onChange(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { onChange(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange(); }
            private void onChange() {
                isModified = true;
                updateWordCount();
                updateTitle();
            }
        });

        fontFamilyBox.addActionListener(e -> applyFontFamily());
        fontSizeBox.addActionListener(e -> applyFontSize());
    }

    //  FILE OPERATIONS

    private void fileNew() {
        if (!confirmDiscard()) return;
        textPane.setText("");
        currentFile = null;
        isModified = false;
        updateTitle();
        setStatus("New document created.");
    }

    private void fileOpen() {
        if (!confirmDiscard()) return;
        JFileChooser fc = new JFileChooser(System.getProperty("user.home"));
        fc.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fc.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(currentFile))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
                textPane.setText(sb.toString());
                isModified = false;
                updateTitle();
                setStatus("Opened: " + currentFile.getName());
            } catch (IOException ex) {
                showError("Could not open file:\n" + ex.getMessage());
            }
        }
    }

    private void fileSave() {
        if (currentFile == null) { fileSaveAs(); return; }
        writeToFile(currentFile);
    }

    private void fileSaveAs() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.home"));
        fc.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
        if (currentFile != null) fc.setSelectedFile(currentFile);
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".txt"))
                f = new File(f.getAbsolutePath() + ".txt");
            currentFile = f;
            writeToFile(currentFile);
        }
    }

    private void writeToFile(File f) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            bw.write(textPane.getText());
            isModified = false;
            updateTitle();
            setStatus("Saved: " + f.getName());
        } catch (IOException ex) {
            showError("Could not save file:\n" + ex.getMessage());
        }
    }

    //  FORMATTING

    private void changeTextColor() {
        Color c = JColorChooser.showDialog(this, "Choose Text Color", Color.BLACK);
        if (c != null) applyAttribute(StyleConstants::setForeground, c);
    }

    private void changeBackgroundColor() {
        Color c = JColorChooser.showDialog(this, "Choose Background Color", textPane.getBackground());
        if (c != null) textPane.setBackground(c);
    }

    private void toggleBold() {
        boolean bold = StyleConstants.isBold(doc.getCharacterElement(textPane.getCaretPosition()).getAttributes());
        applyAttribute(StyleConstants::setBold, !bold);
    }

    private void toggleItalic() {
        boolean italic = StyleConstants.isItalic(doc.getCharacterElement(textPane.getCaretPosition()).getAttributes());
        applyAttribute(StyleConstants::setItalic, !italic);
    }

    private void toggleUnderline() {
        boolean under = StyleConstants.isUnderline(doc.getCharacterElement(textPane.getCaretPosition()).getAttributes());
        applyAttribute(StyleConstants::setUnderline, !under);
    }

    @FunctionalInterface interface AttributeSetter<T> { void set(MutableAttributeSet a, T v); }

    private <T> void applyAttribute(AttributeSetter<T> setter, T value) {
        int start = textPane.getSelectionStart();
        int end   = textPane.getSelectionEnd();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        setter.set(attrs, value);
        if (start == end) textPane.setCharacterAttributes(attrs, false);
        else doc.setCharacterAttributes(start, end - start, attrs, false);
    }

    private void applyFontFamily() {
        String family = (String) fontFamilyBox.getSelectedItem();
        if (family != null) applyAttribute(StyleConstants::setFontFamily, family);
    }

    private void applyFontSize() {
        try {
            int size = Integer.parseInt((String) fontSizeBox.getSelectedItem());
            applyAttribute(StyleConstants::setFontSize, size);
        } catch (NumberFormatException ignored) {}
    }

    private void convertCase(boolean toUpper) {
        int start = textPane.getSelectionStart();
        int end   = textPane.getSelectionEnd();
        if (start == end) { showError("Please select text first."); return; }
        String converted = toUpper
            ? textPane.getSelectedText().toUpperCase()
            : textPane.getSelectedText().toLowerCase();
        textPane.replaceSelection(converted);
        textPane.setSelectionStart(start);
        textPane.setSelectionEnd(start + converted.length());
        setStatus(toUpper ? "Converted to UPPERCASE" : "Converted to lowercase");
    }

    //  CUSTOM FEATURE — FIND & REPLACE DIALOG  (the "Small Challenge" feature)

    private void showFindReplace() {
        JDialog dialog = new JDialog(this, "Find & Replace", false);
        dialog.setSize(440, 220);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(CLR_TOOLBAR);

        JTextField findField    = dialogField();
        JTextField replaceField = dialogField();

        JButton btnFind    = dialogBtn("Find",   CLR_ACCENT);
        JButton btnReplace = dialogBtn("Replace",     CLR_ACCENT2);
        JButton btnReplAll = dialogBtn("Replace All", new Color(80, 160, 100));
        JButton btnClose   = dialogBtn("Close",       new Color(90, 90, 110));

        // Layout

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(CLR_TOOLBAR);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(7,10,7,10);
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx=0; gc.gridy=0; gc.weightx=0.25;
        grid.add(makeLabel("Find:"), gc);
        gc.gridx=1; gc.weightx=1.0;
        grid.add(findField, gc);

        gc.gridx=0; gc.gridy=1; gc.weightx=0.25;
        grid.add(makeLabel("Replace:"), gc);
        gc.gridx=1; gc.weightx=1.0;
        grid.add(replaceField, gc);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        btns.setBackground(CLR_TOOLBAR);
        btns.add(btnFind); btns.add(btnReplace); btns.add(btnReplAll); btns.add(btnClose);

        dialog.setLayout(new BorderLayout());
        dialog.add(grid, BorderLayout.CENTER);
        dialog.add(btns, BorderLayout.SOUTH);

        // Actions 

        btnFind.addActionListener(e -> {
            String needle = findField.getText();
            if (needle.isEmpty()) return;
            String hay = textPane.getText();
            int from = textPane.getSelectionEnd();
            int idx  = hay.indexOf(needle, from);
            if (idx < 0) idx = hay.indexOf(needle);   // wrap around
            if (idx >= 0) {
                textPane.setSelectionStart(idx);
                textPane.setSelectionEnd(idx + needle.length());
                textPane.requestFocus();
            } else {
                JOptionPane.showMessageDialog(dialog,
                    "\"" + needle + "\" not found.", "Find",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnReplace.addActionListener(e -> {
            String sel = textPane.getSelectedText();
            if (sel != null && sel.equals(findField.getText()))
                textPane.replaceSelection(replaceField.getText());
            btnFind.doClick();
        });

        btnReplAll.addActionListener(e -> {
            String needle = findField.getText();
            String repl   = replaceField.getText();
            if (needle.isEmpty()) return;
            String text = textPane.getText();
            int count = 0, idx;
            while ((idx = text.indexOf(needle)) >= 0) {
                text = text.substring(0, idx) + repl + text.substring(idx + needle.length());
                count++;
            }
            textPane.setText(text);
            setStatus(count + " replacement(s) made.");
            JOptionPane.showMessageDialog(dialog, count + " replacement(s) made.",
                "Replace All", JOptionPane.INFORMATION_MESSAGE);
        });

        btnClose.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private JTextField dialogField() {
        JTextField f = new JTextField(22);
        f.setBackground(new Color(48, 48, 62));
        f.setForeground(CLR_FG_LIGHT);
        f.setCaretColor(CLR_ACCENT);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70,70,90)),
            BorderFactory.createEmptyBorder(5,8,5,8)
        ));
        return f;
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(CLR_FG_LIGHT);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return l;
    }

    private JButton dialogBtn(String text, Color accent) {
        JButton btn = new JButton(text);
        btn.setOpaque(true);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(accent);
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createEmptyBorder(6,14,6,14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(accent.brighter()); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(accent); }
        });
        return btn;
    }

    //  INSERT DATE/TIME

    private void insertDateTime() {
        String dt = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy  HH:mm:ss"));
        try {
            doc.insertString(textPane.getCaretPosition(), dt, null);
            setStatus("Date & time inserted.");
        } catch (BadLocationException ex) {
            showError(ex.getMessage());
        }
    }

    //  WORD COUNT

    private void showWordCount() {
        int[] c = getCounts();
        JOptionPane.showMessageDialog(this,
            "<html><div style='font-family:Segoe UI;font-size:13px;padding:8px'>" +
            "<b>Document Statistics</b><br><br>" +
            "Words: &nbsp;&nbsp;&nbsp;&nbsp;<b>" + c[0] + "</b><br>" +
            "Characters: <b>" + c[1] + "</b><br>" +
            "Lines: &nbsp;&nbsp;&nbsp;&nbsp;<b>" + c[2] + "</b>" +
            "</div></html>",
            "Word Count", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateWordCount() {
        int[] c = getCounts();
        wordCountLabel.setText("Words: " + c[0] + "  |  Chars: " + c[1]);
    }

    private int[] getCounts() {
        String text = textPane.getText().trim();
        int words = text.isEmpty() ? 0 : text.split("\\s+").length;
        int chars  = textPane.getText().length();
        int lines  = textPane.getText().isEmpty() ? 1
            : textPane.getText().split("\n", -1).length;
        return new int[]{words, chars, lines};
    }

    //  HELPERS
    private void updateTitle() {
        String name = currentFile != null ? currentFile.getName() : "Untitled";
        setTitle("Mini WordPad (Java Swing) - " + name + (isModified ? " *" : ""));
    }

    private void setStatus(String msg) { statusLabel.setText(msg); }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private boolean confirmDiscard() {
        if (!isModified) return true;
        int r = JOptionPane.showConfirmDialog(this,
            "You have unsaved changes. Discard them?",
            "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (r == JOptionPane.YES_OPTION) return true;
        if (r == JOptionPane.NO_OPTION)  { fileSave(); return !isModified; }
        return false;
    }

    private void exitApp() { if (confirmDiscard()) System.exit(0); }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
            "<html><div style='font-family:Segoe UI;font-size:13px;text-align:center;padding:10px'>" +
            "<b style='font-size:16px'>Mini WordPad</b><br><br>" +
            "Advanced Java Programming Project<br>" +
            "Built with Java Swing<br><br>" +
            "<i>File I/O  |  Cut/Copy/Paste<br>" +
            "Rich Text Formatting  |  Find &amp; Replace<br>" +
            "Word Count  |  Date &amp; Time Insert</i>" +
            "</div></html>",
            "About Mini WordPad", JOptionPane.INFORMATION_MESSAGE);
    }

    //  ENTRY POINT

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(MiniWordPad::new);
    }
}