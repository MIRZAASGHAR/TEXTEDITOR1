import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class TextEditorDSProject {
    // ---------- Char linked list (editor) ----------
    static class CharNode {
        char c;
        CharNode prev, next;
        CharNode(char c){ this.c = c; }
    }

    static class EditorBuffer {
        CharNode head, tail; // sentinel-free but can be null
        CharNode cursor; // cursor is position *after* this node; cursor == null means at start
        int length = 0;

        // cursor at start (before first char)
        EditorBuffer(){ head = tail = null; cursor = null; length = 0; }

        // insert char at cursor (i.e. after cursor node)
        CharNode insertChar(char ch) {
            CharNode n = new CharNode(ch);
            if (head == null) { // empty
                head = tail = n;
                n.prev = n.next = null;
                cursor = n; // cursor ends after inserted char
            } else if (cursor == null) { // insert at head
                n.next = head; n.prev = null;
                head.prev = n; head = n;
                cursor = n;
            } else { // insert after cursor
                CharNode after = cursor.next;
                cursor.next = n;
                n.prev = cursor;
                n.next = after;
                if (after != null) after.prev = n;
                else tail = n;
                cursor = n;
            }
            length++;
            return cursor;
        }

        // delete char before cursor (backspace). returns deleted char or 0 if none
        char deleteBeforeCursor() {
            if (cursor == null) { // nothing before start
                return 0;
            }
            CharNode toDel = cursor;
            char ch = toDel.c;
            CharNode before = toDel.prev;
            CharNode after = toDel.next;
            if (before != null) before.next = after;
            else head = after;
            if (after != null) after.prev = before;
            else tail = before;
            cursor = before;
            length--;
            return ch;
        }

        // move cursor left (one position), return true if moved
        boolean moveLeft() {
            if (cursor == null) return false;
            cursor = cursor.prev;
            return true;
        }

        // move cursor right (one position), return true if moved
        boolean moveRight() {
            if (cursor == null) {
                if (head != null) { cursor = head; return true; }
                return false;
            }
            if (cursor.next != null) { cursor = cursor.next; return true; }
            return false;
        }

        // get string representation and cursor index
        String getText() {
            StringBuilder sb = new StringBuilder();
            CharNode n = head;
            while (n != null) {
                sb.append(n.c);
                n = n.next;
            }
            return sb.toString();
        }

        int getCursorIndex() {
            int idx = 0;
            CharNode n = head;
            while (n != null && n != cursor) {
                idx++;
                n = n.next;
            }
            if (cursor == null) return 0;
            return idx + 1; // cursor is after the node
        }

        // get word before cursor (for autocomplete prefix)
        String getWordPrefixBeforeCursor() {
            StringBuilder sb = new StringBuilder();
            CharNode n = cursor;
            if (n == null) n = head; // if cursor at start and head exists, there's no prefix
            else n = cursor;
            // Move left collecting letters until non-letter or start
            CharNode walker = (cursor == null) ? null : cursor;
            // walker points to node before cursor (cursor itself is last typed char)
            if (walker == null) return "";
            // Collect backwards
            while (walker != null && Character.isLetter(walker.c)) {
                sb.append(walker.c);
                walker = walker.prev;
            }
            return sb.reverse().toString().toLowerCase();
        }
    }

    // ---------- Autocomplete Trie (custom) ----------
    static class TrieNode {
        // children for a-z and space and apostrophe:
        // index 0-25 -> 'a'..'z', 26 -> space, 27 -> apostrophe
        TrieNode[] child = new TrieNode[28];
        boolean isWord = false;
        // store word end for output convenience
        TrieNode(){}
    }

    static class Autocomplete {
        TrieNode root = new TrieNode();

        static int idxOf(char ch){
            if (ch >= 'a' && ch <= 'z') return ch - 'a';
            if (ch == ' ') return 26;
            if (ch == '\'') return 27;
            return -1;
        }

        void addWord(String w){
            if (w == null || w.length() == 0) return;
            String s = w.toLowerCase();
            TrieNode cur = root;
            for (int i=0;i<s.length();i++){
                int id = idxOf(s.charAt(i));
                if (id < 0) continue; // skip unexpected chars
                if (cur.child[id] == null) cur.child[id] = new TrieNode();
                cur = cur.child[id];
            }
            cur.isWord = true;
        }

        // gather up to k suggestions (lexicographic by traversal)
        void collectWords(TrieNode node, StringBuilder prefix, SimpleStringList out, int k) {
            if (out.size() >= k) return;
            if (node.isWord) out.add(prefix.toString());
            for (int i=0;i<28;i++){
                if (node.child[i] != null) {
                    char ch = (i < 26) ? (char)('a' + i) : (i==26 ? ' ' : '\'');
                    prefix.append(ch);
                    collectWords(node.child[i], prefix, out, k);
                    prefix.setLength(prefix.length()-1);
                    if (out.size() >= k) return;
                }
            }
        }

        SimpleStringList suggest(String prefix, int k) {
            SimpleStringList out = new SimpleStringList();
            TrieNode cur = root;
            String s = prefix.toLowerCase();
            for (int i=0;i<s.length();i++){
                int id = idxOf(s.charAt(i));
                if (id < 0) return out;
                if (cur.child[id] == null) return out;
                cur = cur.child[id];
            }
            StringBuilder sb = new StringBuilder(prefix);
            collectWords(cur, sb, out, k);
            return out;
        }
    }

    // ---------- Simple string list (dynamic array) ----------
    static class SimpleStringList {
        String[] arr; int size;
        SimpleStringList(){ arr = new String[4]; size = 0; }
        void add(String s){
            if (size == arr.length){
                String[] n = new String[arr.length*2];
                for (int i=0;i<arr.length;i++) n[i]=arr[i];
                arr = n;
            }
            arr[size++] = s;
        }
        int size(){ return size; }
        String get(int i){ return arr[i]; }
    }

    // ---------- Simple Stack via linked nodes ----------
    static class StackNode {
        Operation op;
        StackNode next;
        StackNode(Operation op){ this.op = op; }
    }
    static class SimpleStack {
        StackNode top;
        void push(Operation op){ StackNode n = new StackNode(op); n.next = top; top = n; }
        Operation pop(){
            if (top == null) return null;
            Operation op = top.op;
            top = top.next;
            return op;
        }
        boolean isEmpty(){ return top == null; }
        void clear(){ top = null; }
    }

    // ---------- Operation definitions for Undo/Redo ----------
    static abstract class Operation {
        abstract void apply(EditorBuffer buf, Autocomplete ac, SimpleStack pushToUndoOrRedo);
        abstract Operation inverse();
    }

    static class InsertOp extends Operation {
        char ch;
        InsertOp(char ch){ this.ch = ch; }
        void apply(EditorBuffer buf, Autocomplete ac, SimpleStack pushToUndoOrRedo){
            buf.insertChar(ch);
        }
        Operation inverse(){ return new DeleteOp(); }
    }

    static class DeleteOp extends Operation {
        char deletedChar; // filled when applied
        DeleteOp(){}
        void apply(EditorBuffer buf, Autocomplete ac, SimpleStack pushToUndoOrRedo){
            deletedChar = buf.deleteBeforeCursor();
        }
        Operation inverse(){ return new InsertOp(deletedChar); }
    }

    static class MoveLeftOp extends Operation {
        boolean moved;
        MoveLeftOp(){}
        void apply(EditorBuffer buf, Autocomplete ac, SimpleStack pushToUndoOrRedo){
            moved = buf.moveLeft();
        }
        Operation inverse(){ return moved ? new MoveRightOp() : new NoOp(); }
    }
    static class MoveRightOp extends Operation {
        boolean moved;
        MoveRightOp(){}
        void apply(EditorBuffer buf, Autocomplete ac, SimpleStack pushToUndoOrRedo){
            moved = buf.moveRight();
        }
        Operation inverse(){ return moved ? new MoveLeftOp() : new NoOp(); }
    }
    static class NoOp extends Operation {
        void apply(EditorBuffer buf, Autocomplete ac, SimpleStack pushToUndoOrRedo){}
        Operation inverse(){ return new NoOp(); }
    }

    // ---------- Editor controller tying buffer, autocomplete, undo/redo ----------
    static class EditorController {
        EditorBuffer buf = new EditorBuffer();
        Autocomplete ac = new Autocomplete();
        SimpleStack undo = new SimpleStack();
        SimpleStack redo = new SimpleStack();

        // typed char
        void typeChar(char c) {
            InsertOp op = new InsertOp(c);
            op.apply(buf, ac, null);
            undo.push(op.inverse()); // inverse to undo the insert is delete
            redo.clear();
        }

        // backspace
        void backspace() {
            DeleteOp op = new DeleteOp();
            op.apply(buf, ac, null);
            // if nothing deleted, do not push
            if (op.deletedChar != 0) {
                undo.push(op.inverse());
                redo.clear();
            }
        }

        void moveLeft() {
            MoveLeftOp op = new MoveLeftOp();
            op.apply(buf, ac, null);
            undo.push(op.inverse());
            redo.clear();
        }

        void moveRight() {
            MoveRightOp op = new MoveRightOp();
            op.apply(buf, ac, null);
            undo.push(op.inverse());
            redo.clear();
        }

        void undoOne() {
            Operation inv = undo.pop();
            if (inv == null) return;
            // apply inv, but push its inverse to redo
            inv.apply(buf, ac, null);
            redo.push(inv.inverse());
        }

        void redoOne() {
            Operation op = redo.pop();
            if (op == null) return;
            op.apply(buf, ac, null);
            undo.push(op.inverse());
        }

        // add word to dictionary used by autocomplete (manual population)
        void addDictionaryWord(String w){ ac.addWord(w); }

        SimpleStringList suggestions(int k) {
            String prefix = buf.getWordPrefixBeforeCursor();
            if (prefix.length() == 0) return new SimpleStringList();
            return ac.suggest(prefix, k);
        }

        void printState() {
            String text = buf.getText();
            int cursorIdx = buf.getCursorIndex();
            // display cursor with a pipe |
            String display = text.substring(0, Math.max(0, Math.min(cursorIdx, text.length())))
                    + "|"
                    + text.substring(Math.max(0, Math.min(cursorIdx, text.length())));
            System.out.println("Text: " + display);
        }
    }

    // ---------- Demo / simple CLI ----------
    public static void main(String[] args) throws IOException {
        EditorController ed = new EditorController();

        // Populate sample dictionary
        String[] sampleWords = {"hello","help","hell","helium","hero","heron","heap","happy","hack","java","javascript","jar","join","jog","world","word","work","wonder"};
        for (int i=0;i<sampleWords.length;i++) ed.addDictionaryWord(sampleWords[i]);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Simple TextEditor (type single chars). Commands: LEFT RIGHT BACKSPACE UNDO REDO SUG K (k suggestions) QUIT");
        ed.printState();
        while (true) {
            System.out.print("> ");
            String line = br.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.equalsIgnoreCase("QUIT")) break;
            if (line.equalsIgnoreCase("LEFT")) { ed.moveLeft(); ed.printState(); continue; }
            if (line.equalsIgnoreCase("RIGHT")) { ed.moveRight(); ed.printState(); continue; }
            if (line.equalsIgnoreCase("BACKSPACE")) { ed.backspace(); ed.printState(); continue; }
            if (line.equalsIgnoreCase("UNDO")) { ed.undoOne(); ed.printState(); continue; }
            if (line.equalsIgnoreCase("REDO")) { ed.redoOne(); ed.printState(); continue; }
            if (line.startsWith("SUG")) {
                String[] parts = line.split("\\s+");
                int k = 5;
                if (parts.length >= 2) {
                    try { k = Integer.parseInt(parts[1]); } catch(Exception e) {}
                }
                SimpleStringList s = ed.suggestions(k);
                System.out.println("Suggestions for prefix: ");
                for (int i=0;i<s.size();i++) System.out.println("  " + s.get(i));
                continue;
            }
            // type every char from the input line
            for (int i=0;i<line.length();i++){
                char ch = line.charAt(i);
                // we only accept printable chars
                ed.typeChar(ch);
            }
            ed.printState();
        }
        System.out.println("bye");
    }
}

