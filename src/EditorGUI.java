import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class EditorGUI extends JFrame {

    private JTextArea textArea;
    private JTextField addWordField;
    private JButton addBtn;
    private JList<String> suggestionList;

    private EditorController controller = new EditorController();

    // Undo/Redo stacks
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();

    public EditorGUI() {
        setTitle("DS Text Editor with Incremental Autocomplete + Undo/Redo");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // TEXT AREA
        textArea = new JTextArea();
        textArea.setFont(new Font("Arial", Font.PLAIN, 18));
        add(new JScrollPane(textArea), BorderLayout.CENTER);

        // SUGGESTION LIST
        suggestionList = new JList<>();
        add(new JScrollPane(suggestionList), BorderLayout.EAST);

        // ADD WORD PANEL
        JPanel bottom = new JPanel(new FlowLayout());
        addWordField = new JTextField(20);
        addBtn = new JButton("Add Word");
        bottom.add(new JLabel("Add to Dictionary:"));
        bottom.add(addWordField);
        bottom.add(addBtn);
        add(bottom, BorderLayout.SOUTH);

        // ---------------- KEY LISTENER ----------------
        textArea.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                char ch = e.getKeyChar();

                // Save current text to undo stack
                undoStack.push(textArea.getText());
                redoStack.clear(); // clear redo after new action

                // Auto-add word on SPACE or ENTER
                if (ch == ' ' || ch == '\n') {
                    autoAddWord();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                updateSuggestions();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                // TAB inserts selected suggestion
                if (e.getKeyCode() == KeyEvent.VK_TAB && !suggestionList.isSelectionEmpty()) {
                    e.consume();
                    insertSelectedSuggestion();
                }

                // Undo Ctrl+Z
                if (e.getKeyCode() == KeyEvent.VK_Z && e.isControlDown()) {
                    undo();
                }

                // Redo Ctrl+Y
                if (e.getKeyCode() == KeyEvent.VK_Y && e.isControlDown()) {
                    redo();
                }
            }
        });

        // ADD WORD BUTTON
        addBtn.addActionListener(e -> {
            String w = addWordField.getText().trim().toLowerCase();
            if (w.isEmpty()) return;
            if (!controller.wordExists(w)) {
                controller.addWord(w);
                JOptionPane.showMessageDialog(this, "Added ✓");
            } else {
                JOptionPane.showMessageDialog(this, "Already exists");
            }
            addWordField.setText("");
        });

        setVisible(true);
    }

    // ---------------- AUTO ADD ----------------]
    private void autoAddWord() {
        String text = textArea.getText();
        if (text.isEmpty()) return;

        // Ignore trailing whitespace
        int end = text.length() - 1;
        while (end >= 0 && Character.isWhitespace(text.charAt(end))) end--;

        if (end < 0) return;

        // Find start of the last word
        int start = end;
        while (start >= 0 && Character.isLetter(text.charAt(start))) start--;

        String word = text.substring(start + 1, end + 1).toLowerCase();

        if (word.length() > 1 && word.matches("[a-zA-Z]+")) {
            controller.addWord(word);
            System.out.println("Added word: " + word); // debug
        }
    }


    // ---------------- UPDATE SUGGESTIONS ----------------
    private void updateSuggestions() {
        String text = textArea.getText();
        if (text.isEmpty()) {
            suggestionList.setListData(new String[0]);
            return;
        }

        // find last incomplete word
        int i = text.length() - 1;
        while (i >= 0 && !Character.isWhitespace(text.charAt(i))) i--;
        String currentWord = text.substring(i + 1);

        if (currentWord.isEmpty()) {
            suggestionList.setListData(new String[0]);
            return;
        }

        List<String> suggestions = controller.getSuggestions(currentWord);
        suggestionList.setListData(suggestions.toArray(new String[0]));
        if (!suggestions.isEmpty()) suggestionList.setSelectedIndex(0);
    }

    // ---------------- INSERT SELECTED SUGGESTION ----------------
    private void insertSelectedSuggestion() {
        String text = textArea.getText();
        int i = text.length() - 1;
        while (i >= 0 && !Character.isWhitespace(text.charAt(i))) i--;
        String chosen = suggestionList.getSelectedValue();
        if (chosen != null) {
            String newText = text.substring(0, i + 1) + chosen + " ";
            textArea.setText(newText);
            textArea.setCaretPosition(newText.length());
            updateSuggestions();
        }
    }

    // ---------------- UNDO ----------------
    private void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(textArea.getText());
            String prev = undoStack.pop();
            textArea.setText(prev);
        }
    }

    // ---------------- REDO ----------------
    private void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(textArea.getText());
            String next = redoStack.pop();
            textArea.setText(next);
        }
    }

    public static void main(String[] args) {
        new EditorGUI();
    }
}


class EditorController {

    private File dictionaryFile;

    public EditorController() {
        dictionaryFile = new File("dictionary.txt");

        try {
            if (!dictionaryFile.exists()) {
                dictionaryFile.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // READ all words
    private List<String> readAllWords() {
        List<String> words = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(dictionaryFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                words.add(line.trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return words;
    }

    // WRITE all words
    private void writeAllWords(List<String> words) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(dictionaryFile))) {
            for (String word : words) {
                pw.println(word);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // CHECK if word exists
    public boolean wordExists(String w) {
        List<String> words = readAllWords();
        return words.contains(w.toLowerCase());
    }

    // ADD word using Method 3
    public void addWord(String w) {
        w = w.toLowerCase();
        List<String> words = readAllWords();
        if (!words.contains(w)) {
            words.add(w);
            writeAllWords(words);
        }
    }

    // Get suggestions (basic autocomplete)
    public List<String> getSuggestions(String prefix) {
        prefix = prefix.toLowerCase();
        List<String> all = readAllWords();
        List<String> result = new ArrayList<>();

        for (String word : all) {
            if (word.startsWith(prefix)) {
                result.add(word);
            }
        }
        return result;
    }
}

