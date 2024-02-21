/* Copyright (C) 2020-2021 by Jon Taylor
 *
 *
 */

package armarender;

import armarender.math.*;
import armarender.object.*;
import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.*;

// BDialog
public class InventoryManagement extends BDialog implements WindowListener
{
    private BFrame fr;
    Scene scene;
    private LayoutWindow window;
    BScrollPane listWrapper;
    
    private ValueField insertRowTextField, insertColumnTextField;
    BTree remoteFileList;
    
    
    public InventoryManagement(BFrame parent, LayoutWindow window, Scene theScene)
    {
      super(parent, "Inventory Management", true);
        //parent.addComponentListener(this);
        
      
      fr = parent;
        scene = theScene;
        this.window = window;
        FormContainer mainContent = new FormContainer(1, 3);
        FormContainer content = new FormContainer(9, 10);
        //FormContainer content = new FormContainer(new double [] {1}, new double [] {1, 0, 0, 0});
        setContent(BOutline.createEmptyBorder(mainContent, UIUtilities.getStandardDialogInsets()));
        mainContent.add( content, 0, 0 );
        remoteFileList = new BTree();
        
        //addNode(javax.swing.tree.TreePath parent, javax.swing.tree.MutableTreeNode node)
        TreePath tp = remoteFileList.getRootNode();
        remoteFileList.setRootNodeShown(false); // Have files appear as root nodes.
        remoteFileList.setPreferredVisibleRows(10);
        //remoteFileList.setCellRenderer(new FileTreeRenderer());
        
        //TreeCellRenderer renderer = remoteFileList.getCellRenderer();
        
        //tp.
        //DefaultMutableTreeNode x = new DefaultMutableTreeNode("X");
        //remoteFileList.addNode(tp, x);
        //remoteFileList.addNode(tp, new DefaultMutableTreeNode("Y"));
        
        remoteFileList.setMultipleSelectionEnabled(false);
        //remoteFileList.addEventLink(SelectionChangedEvent.class, this, "doSelectionChanged");
        //remoteFileList.addEventLink(MouseClickedEvent.class, this, "mouseClicked");
        
        //remoteFileList.add
        
        listWrapper = new BScrollPane(remoteFileList, BScrollPane.SCROLLBAR_AS_NEEDED, BScrollPane.SCROLLBAR_AS_NEEDED);
        listWrapper.setBackground(remoteFileList.getBackground());
        listWrapper.setForceWidth(true);
        listWrapper.setPreferredViewSize(new Dimension(650, 450));
        content.add(listWrapper, 0, 0);
        
        //content.add(insertRowTextField = new ValueField(1, ValueField.POSITIVE, 5), 1, 6);
        //content.add(Translate.button("Insert Row", this, "download"), 2, 6);
        //content.add(insertColumnTextField = new ValueField(1, ValueField.POSITIVE, 5), 3, 6);
        //content.add(Translate.button("Insert Column", this, "download"), 4, 6);
        
        //RowContainer choiceRow = new RowContainer();
        //content.add(choiceRow, 0, 1);
        
        RowContainer row = new RowContainer();
        row.add(Translate.button("Save", this, "save"));
        row.add(Translate.button("Cancel", this, "doCancel"));
        mainContent.add(row, 0, 1 );
        
        //loadFilesAvailable();
        
        //frame.add(scrollPane, BorderLayout.CENTER);
        
        //addWindowListener(this);
        SymComponent aSymComponent = new SymComponent();
        //this.addComponentListener(aSymComponent);
        
        addEventLink(WindowClosingEvent.class, this, "doClose");

        // Show the dialog.
        pack();
        UIUtilities.centerDialog(this, parent);
        setVisible(true);
    }
    
    
    public void doClose(){
        System.out.println("doClose");
        dispose();
    }
    
    public void WindowResizedEvent(WindowWidget source){
        System.out.println("WindowResizedEvent " + source);
        
    }
    //public void componentResized(ComponentEvent componentEvent) {
            // do stuff
     //   System.out.println("componentResized " );
    //}
    
    
    /**
     * download
     *
     */
    public void save(){
        
        
    }
    
    /**
     * doCancel
     *
     */
    private void doCancel()
    {
      dispose();
    }
    
    /*
    @Override
    public void componentResized(ComponentEvent e) {
        displayMessage(e.getComponent().getClass().getName() + " --- Resized ");
    }
    */
    
    class SymComponent extends java.awt.event.ComponentAdapter
        {
            public void componentResized(java.awt.event.ComponentEvent event)
            {
                Object object = event.getSource();
                //if (object == ChatFrame.this)
                //    ChatFrame_ComponentResized(event);
            }
        }
    public void ChatFrame_ComponentResized(java.awt.event.ComponentEvent event)
        {
            System.out.println("***");
            /*
            if (this.getSize().width < 200)
            {
                this.setSize(200, this.getSize().height);
            }
            if (this.getSize().height < 200)
            {
                this.setSize(this.getSize().width, 200);
            }
             */
        }
    
    
    public void windowDeactivated(WindowEvent e){
        
    }
    
    public void windowActivated(WindowEvent e){
        
    }
    
    public void windowDeiconified(WindowEvent e){
        
    }
    
    public void windowIconified(WindowEvent e) {
        
    }
    
    public void windowClosed(WindowEvent e){
        
    }
    
    public void windowClosing(WindowEvent e){
        
    }
    
    public void windowOpened(WindowEvent e){
        
    }
}
