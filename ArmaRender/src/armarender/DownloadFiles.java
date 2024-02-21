/* Copyright (C) 2021 by Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE. See the GNU General Public License for more details. */

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

public class DownloadFiles extends BDialog
{
    private BFrame fr;
    Scene scene;
    private LayoutWindow window;
    
    //JTextField insertRowTextField = new JTextField(5);
    //JTextField insertColumnTextField = new JTextField(5);
    private ValueField insertRowTextField, insertColumnTextField;
    BTree remoteFileList;
    BScrollPane listWrapper;
    
    protected static final long SLEEP_TIME = 3 * 1000;
    
    public DownloadFiles(BFrame parent, LayoutWindow window, Scene theScene)
    {
      super(parent, "Download Files", true);
      
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
        remoteFileList.setCellRenderer(new FileTreeRenderer());
        
        //TreeCellRenderer renderer = remoteFileList.getCellRenderer();
        
        //tp.
        //DefaultMutableTreeNode x = new DefaultMutableTreeNode("X");
        //remoteFileList.addNode(tp, x);
        //remoteFileList.addNode(tp, new DefaultMutableTreeNode("Y"));
        
        remoteFileList.setMultipleSelectionEnabled(false);
        remoteFileList.addEventLink(SelectionChangedEvent.class, this, "doSelectionChanged");
        remoteFileList.addEventLink(MouseClickedEvent.class, this, "mouseClicked");
        
        //remoteFileList.add
        
        listWrapper = new BScrollPane(remoteFileList, BScrollPane.SCROLLBAR_AS_NEEDED, BScrollPane.SCROLLBAR_AS_NEEDED);
        listWrapper.setBackground(remoteFileList.getBackground());
        listWrapper.setForceWidth(true);
        listWrapper.setPreferredViewSize(new Dimension(400, 250));
        content.add(listWrapper, 0, 0);
        
        //content.add(insertRowTextField = new ValueField(1, ValueField.POSITIVE, 5), 1, 6);
        //content.add(Translate.button("Insert Row", this, "download"), 2, 6);
        //content.add(insertColumnTextField = new ValueField(1, ValueField.POSITIVE, 5), 3, 6);
        //content.add(Translate.button("Insert Column", this, "download"), 4, 6);
        
        //RowContainer choiceRow = new RowContainer();
        //content.add(choiceRow, 0, 1);
        
        RowContainer row = new RowContainer();
        row.add(Translate.button("Download", this, "download"));
        //row.add(Translate.button("ok", this, "download"));
        row.add(Translate.button("cancel", this, "doCancel"));
        mainContent.add(row, 0, 1 );
        
        loadFilesAvailable();

        // Show the dialog.
        pack();
        UIUtilities.centerDialog(this, parent);
        setVisible(true);
    }
    
    
    /**
     * loadFilesAvailable
     *
     * Description: Load files available from the server.
     */
    public void loadFilesAvailable(){
        try {
            String a = "http://arma.ac/download/files.xml";
            URLConnection connection = new URL(a).openConnection();
            connection.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            connection.connect();
            BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream(),
                    Charset.forName("UTF-8")));
            TreePath rootTp = remoteFileList.getRootNode();
            //StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                //sb.append(line);
                int start = line.indexOf("<file>");
                int end = line.indexOf("</file>");
                if(start != -1 && end != -1){
                    String fileName = line.substring(start + 6, end);
                    DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode(fileName);
                    TreePath tp = remoteFileList.addNode(rootTp, dmtn);
                    remoteFileList.makeNodeVisible(tp);
                }
            }
        } catch (Exception e){
            System.out.println("Error: " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * download
     *
     * Description: Download a file from the server with the game given on the selected list item.
     */
    public void download(){
        TreePath selectionPath = remoteFileList.getSelectedNode();
        if(selectionPath != null){
            Object selectedElement = selectionPath.getLastPathComponent();
            try {
                
                //String message = "Your file is being downloaded.";
                //JOptionPane.showMessageDialog(null, message, "Downloading: " + selectedElement, JOptionPane.INFORMATION_MESSAGE);
                
                remoteFileList.setEnabled(false);
                
                String fileName = new File(".").getCanonicalPath() +
                    FileSystems.getDefault().getSeparator() +
                    selectedElement;
                String urlPath = "http://arma.ac/download/" + selectedElement;
                byte[] buffer = new byte[8 * 1024];
                URLConnection connection = new URL(urlPath).openConnection();
                InputStream input = connection.getInputStream();
                try {
                  OutputStream output = new FileOutputStream(fileName);
                  try {
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                      output.write(buffer, 0, bytesRead);
                    }
                  } finally {
                    output.close();
                  }
                } finally {
                  input.close();
                }
                // Load downloaded file
                ArmaRender.openScene(new File(fileName), null);
            } catch(Exception e){
                System.out.println("Error: " + e);
                e.printStackTrace();
            }
            
            remoteFileList.setEnabled(true);
            dispose();
        }
    }
    
    /**
     *
     *
     */
    public void load(){
        
    }
    
    
    /**
     *
     *
     */
    public void doSelectionChanged()
    {
      TreePath selection = remoteFileList.getSelectedNode();
        
        
    }
    
    /**
     *
     *
     */
    public void mouseClicked(MouseClickedEvent ev)
    {
      if (ev.getClickCount() == 2)
      {
        //doEdit();
      }
      else if (ev.getClickCount() == 1)
      {
        //doSelectionChanged();
      }
    }
    
    
    /**
     *
     *
     */
    private void doCancel()
    {
      dispose();
    }
    
    
    /**
     * FileTreeRenderer Cell Class
     *
     */
    public class FileTreeRenderer extends DefaultTreeCellRenderer {
      private static final String SPAN_FORMAT = "<span style='color:%s;'>%s</span>";
      private final ImageIcon fileIcon;

      public FileTreeRenderer() {
          fileIcon = new ImageIcon(getClass().getResource("/armarender/Icons/ads.png"));
      }

      @Override
      public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                    boolean leaf, int row, boolean hasFocus) {
          super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
          DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
          Object userObject = node.getUserObject();
          
          this.setBackgroundSelectionColor(Color.LIGHT_GRAY);
          this.setBorderSelectionColor(Color.LIGHT_GRAY);
          
          if (userObject instanceof String) {
              String pp = (String) userObject;
              String text = String.format(SPAN_FORMAT, "black", pp);
              //text += " [" + String.format(SPAN_FORMAT, "orange", pp.()) + "]";
              this.setText("<html>" + text + "</html>");
              this.setIcon(fileIcon);
          }
          return this;
      }
    }
}


