package app.display.dialogs.visual_editor.view.components;

import app.display.dialogs.visual_editor.recs.utils.HumanReadable;
import app.display.dialogs.visual_editor.recs.utils.Pair;
import app.display.dialogs.visual_editor.recs.utils.ReadableSymbol;
import app.display.dialogs.visual_editor.view.panels.IGraphPanel;
import main.grammar.Symbol;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public class AddLudemeWindow extends JPanel {

    JList list;
    DefaultListModel listModel;
    public JTextField searchField;
    JScrollPane scrollableList;

    IGraphPanel graphPanel;
    boolean connect;



    public AddLudemeWindow(List<Symbol> symbolList, IGraphPanel graphPanel, boolean connect){
        this.graphPanel = graphPanel;
        this.connect = connect;

        updateList(symbolList);

    }

    private void drawComponents(){
        removeAll();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        scrollableList.setPreferredSize(new Dimension(scrollableList.getPreferredSize().width, 150));
        searchField.setPreferredSize(new Dimension(scrollableList.getPreferredSize().width, searchField.getPreferredSize().height));


        add(searchField); add(scrollableList);

        repaint();


        setPreferredSize(new Dimension(getPreferredSize()));
        setSize(getPreferredSize());
        setVisible(false);


        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                JList theList = (JList) mouseEvent.getSource();
                int index = theList.locationToIndex(mouseEvent.getPoint());
                if (index >= 0) {
                    Object o = theList.getModel().getElementAt(index);
                    ReadableSymbol rs = (ReadableSymbol) o;
                    graphPanel.addNode(rs.getSymbol(), getLocation().x, getLocation().y, connect);
                    searchField.setText("");
                    scrollableList.getVerticalScrollBar().setValue(0);
                }
            }
        };
        list.addMouseListener(mouseListener);

    }

    public void updateList(List<Symbol> symbolList){

        searchField = new JTextField();

        // remove duplicates
        symbolList = symbolList.stream().distinct().collect(java.util.stream.Collectors.toList());

        // create readable strings
        List<ReadableSymbol> readableList = new ArrayList<>();
        for(int i = 0; i < symbolList.size(); i++) {
            readableList.add(new ReadableSymbol(symbolList.get(i)));
        }

        listModel = new DefaultListModel<Pair<Symbol,String>>();
        for (ReadableSymbol rs : readableList) {
            listModel.addElement(rs);
        }
        list = new JList(listModel);
        scrollableList = new JScrollPane(list);

        List<Symbol> ludemeList_copy = symbolList;

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listModel = new DefaultListModel<Symbol>();
                for(Symbol l : ludemeList_copy){
                    // TODO: Improve
                    if(l.name().contains(searchField.getText())){
                        listModel.addElement(l);
                    }
                }
                list.setModel(listModel);
                repaint();
            }
        });
        drawComponents();
    }

}