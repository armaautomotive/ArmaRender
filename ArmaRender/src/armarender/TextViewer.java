/* Copyright (C) 2020-2023 by Jon Taylor
     
    */


package armarender;

import armarender.animation.*;
import armarender.animation.distortion.*;
import armarender.image.*;
import armarender.math.*;
import armarender.object.*;
import armarender.script.*;
import armarender.texture.*;
import armarender.ui.*;
import armarender.keystroke.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.prefs.*;
import java.util.*;
import java.util.List;
import buoyx.docking.*;
import javax.swing.text.*;
import javax.swing.*;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import armarender.object.TriangleMesh.*;


/** The LayoutWindow class represents the main window for creating and laying out scenes. */

public class TextViewer extends JFrame implements ActionListener
{
    // JFrame
    static JFrame f;
  
    // JButton
    static JButton b;
  
    // label to display text
    static JLabel l;
  
    // text area
    static JTextArea jt;
    
    public TextViewer(){
        // create a new frame to store text field and button
        f = new JFrame("Information");
        f.setAlwaysOnTop( true );
  
        // create a label to display text
        l = new JLabel("nothing entered");
  
        // create a new button
        b = new JButton("submit");
  
        // create a object of the text class
        //text te = new text();
  
        // addActionListener to button
        b.addActionListener(this);
  
        // create a text area, specifying the rows and columns
        jt = new JTextArea(28, 40); // row column
        //jt
        
        JPanel p = new JPanel(new BorderLayout());
        
        JScrollPane scroll = new JScrollPane (jt, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        p.add(scroll);
        //frame.setVisible (true);
        
        
        // add the text area and button to panel
        p.add(scroll);
        //p.add(b);
        //p.add(l);
  
        f.add(p);
        // set the size of frame
        f.setSize(500, 500);
        
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - f.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - f.getHeight()) / 2);
        f.setLocation(x, y);
  
        f.show();
        
    }
    
    public void setText(String text){
        this.jt.setText(text);
    }
    
    public void display(){
        
    }
    
    // if the button is pressed
    public void actionPerformed(ActionEvent e)
    {
        String s = e.getActionCommand();
        if (s.equals("submit")) {
            // set the text of the label to the text of the field
            l.setText(jt.getText());
        }
    }

    
    public void toFront() {
      super.setVisible(true);
      int state = super.getExtendedState();
      state &= ~JFrame.ICONIFIED;
      super.setExtendedState(state);
      super.setAlwaysOnTop(true);
      super.toFront();
      super.requestFocus();
      super.setAlwaysOnTop(false);
    }
}
