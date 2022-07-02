package app.display.dialogs.visual_editor.view.panels.menus;

import app.display.dialogs.visual_editor.handler.EditorMenuBarHandler;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EditorMenuBar extends JMenuBar {

    public EditorMenuBar() {
        JMenu file = new JMenu("File"); // operations with file being edited
        // adding file menu items
        addJMenuItem(file, "New", null);
        addJMenuItem(file, "Open...", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EditorMenuBarHandler.openDescriptionFile();
            }
        });
        addJMenuItem(file, "Open recent", null);
        addJMenuItem(file, "Close file", null);
        addJMenuItem(file, "Save", null);
        addJMenuItem(file, "Save as...", null);
        addJMenuItem(file, "Compile", null);
        addJMenuItem(file, "Exit", null);

        JMenu settings = new JMenu("Settings"); // adjust editor settings e.g. font size, colors ect.
        // adding settings menu items
        addJMenuItem(settings, "Open settings...", null);

        JMenu about = new JMenu("About"); // read about the editor: documentation, research report, DLP
        // adding about menu items
        addJMenuItem(about, "Open documentation", null);
        addJMenuItem(about, "Learn more about the editor", null); // opens research paper
        addJMenuItem(about, "Learn more about DLP", null);

        add(file);
        add(new EditMenu(this));
        add(settings);
        add(about);
    }

    public void addJMenuItem(JMenu menu, String itemName, ActionListener actionListener)
    {
        JMenuItem jMenuItem = new JMenuItem(itemName);
        jMenuItem.addActionListener(actionListener);
        menu.add(jMenuItem);
    }
}
