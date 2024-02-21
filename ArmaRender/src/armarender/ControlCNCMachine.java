/* Copyright (C) 2022,2023 by Jon Taylor

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import java.util.*;
import armarender.math.*;
import armarender.object.*;
import armarender.view.CanvasDrawer;
import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import buoy.widget.*;
import armarender.ui.*;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JFrame;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import javax.swing.JSplitPane;
import java.awt.GridLayout;
import java.awt.*;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.BorderFactory;
import javax.swing.plaf.basic.*;
import javax.swing.border.*;
import javax.swing.JScrollPane;
import java.awt.event.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.JComboBox;
import javax.swing.ImageIcon;
//import javax.comm.*; // RXTX may be too complicated, Use sockets communication with a server C# for serial communication.


public class ControlCNCMachine extends JFrame implements ActionListener {
    private boolean running = false;
    private boolean live = false;
    private JPanel centerPreviewPanel;
    private JTextArea textArea;
    private JScrollPane gCodeScrollPane;
    private JLabel connectionStatusLabel;
    private JButton loadFileButton;
    private JLabel gCodeFileLabel;
    private Highlighter.HighlightPainter painter;
    private int gCodeLineNumber = 0;
    private JButton stopButton;
    private JButton runButton;
    private JButton testButton;
    private JButton liveButton;
    private JProgressBar progressBar;
    private JLabel torchActiveLabel;
    private JTextField xSteps;
    private JTextField ySteps;
    private JTextField zSteps;
    private JTextField xPos;
    private JTextField yPos;
    private JTextField zPos;
    private JButton goToZeroButton;
    private JButton setZeroButton;
    //private double x;               // depricate
    //private double y;
    //private double z;
    private JButton moveXLeftButton;
    private JButton moveXRightButton;
    private JButton moveYLeftButton;
    private JButton moveYRightButton;
    private JButton moveZLeftButton;
    private JButton moveZRightButton;
    private JTextField inchesPerMinute;
    protected ViewerCanvas theView;
    private java.awt.Rectangle bounds;
    private Vec3 minPoint;
    private Vec3 maxPoint;
    private Vector<Vector<Vec3>> cutProfiles;
    private Vec3 cursorPoint = new Vec3(0,0,0);
    SocketCommunication socket;
    
    public class GCodeInstruction {
        public Vec3 point = null;
        public boolean torchEnable = false;
        public boolean torchDisable = false;
    }
    
    public ControlCNCMachine(){
        System.out.println("ControlCNCMachine");
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        
        this.setTitle("Control");
        
        
        socket = new SocketCommunication();
        socket.start();
        
        
        //x = 0;
        //y = 0;
        //z = 0;
        bounds = new Rectangle(0,0,0,0);
        minPoint = new Vec3(-4,-4,-4);
        maxPoint = new Vec3(4,4,4);
        cutProfiles = new Vector<Vector<Vec3>>();
        cursorPoint = new Vec3(0,0,0);
       
        //this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //

        //3. Create components and put them in the frame.
        //...create emptyLabel...
        JLabel emptyLabel = new JLabel("Image and Text", JLabel.CENTER);
        //Set the position of the text, relative to the icon:
        emptyLabel.setVerticalTextPosition(JLabel.BOTTOM);
        emptyLabel.setHorizontalTextPosition(JLabel.CENTER);
        
       
        //
        // File, run, stop
        //
        JPanel leftPanel = new JPanel();
        leftPanel.setSize(300, dim.height);
        leftPanel.setMinimumSize(new Dimension(300, dim.height));
        leftPanel.setPreferredSize(new Dimension(300, dim.height));
        leftPanel.setBackground(new Color(220,220,220));
        leftPanel.setLayout(null);
        
        int y = 20;
        int ySeperation = 44;
        
        // hardware
        String[] petStrings = { "Arma CNC Tube Notcher", "Arma CNC Tube Bender", "Arma 5Axis Machine" };
        JComboBox petList = new JComboBox(petStrings);
        petList.setBounds(20, y, 255, 33);
        petList.setPreferredSize(new Dimension(255, 33));
        //petList.setLayout(null);
        //petList.setSelectedIndex(4);
        petList.addActionListener(this);
        leftPanel.add(petList);
        
        y+= (ySeperation/2);
        connectionStatusLabel = new JLabel("Status: Not connected.");
        connectionStatusLabel.setBounds(20 + 3, y, 155,33);
        leftPanel.add(connectionStatusLabel);
        
        y+= ySeperation;
        gCodeFileLabel=new JLabel("Load GCode File");
        gCodeFileLabel.setBounds(20 + 3, y, 155,33);
        leftPanel.add(gCodeFileLabel);
        
        loadFileButton = new JButton("Load File");
        loadFileButton.setBounds(185, y, 90,33);
        loadFileButton.addActionListener( this );
        leftPanel.add(loadFileButton);
        
        y+= ySeperation;
        stopButton = new JButton("Stop");
        //stopButton.setBackground(Color.RED);
        //stopButton.setOpaque(true);
        stopButton.setForeground(Color.RED);
        //stopButton.setBorderPainted(false);
        stopButton.addActionListener( this );
        stopButton.setBounds(20, y, 255,33); // x,y,w,h
        leftPanel.add(stopButton);
        
        y+= ySeperation;
        testButton = new JButton("Test");
        //testButton.setForeground(Color.RED);
        testButton.addActionListener( this );
        testButton.setBounds(20, y, 121,33); // x,y,w,h
        leftPanel.add(testButton);
        
        liveButton = new JButton("Live");
        liveButton.setForeground(new Color(50, 50, 200));
        liveButton.addActionListener( this );
        liveButton.setBounds(154, y, 121,33); // x,y,w,h
        leftPanel.add(liveButton);
        
        y+= ySeperation;
        runButton = new JButton("Run");
        runButton.addActionListener( this );
        runButton.setBounds(20, y, 255,33); // x,y,w,h
        runButton.setForeground(new Color(50, 200, 50));
        //runButton.setBackground(Color.RED);
        //runButton.setOpaque(true);
        leftPanel.add(runButton);
        
        y+= ySeperation;
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setBounds(20, y, 255,33);
        leftPanel.add(progressBar);
        
        
        y+= ySeperation;
        torchActiveLabel = new JLabel("Torch Off");
        torchActiveLabel.setBounds(20, y, 255,33);
        leftPanel.add(torchActiveLabel);
        
        
        // Calibration
        y+= ySeperation* 2;
        JLabel xStepsLanel = new JLabel("X Steps");
        xStepsLanel.setBounds(20, y, 60,33);
        leftPanel.add(xStepsLanel);
        xSteps = new JTextField("100");
        xSteps.setBounds(80, y, 195,33);
        leftPanel.add(xSteps);
        
        y+= ySeperation;
        JLabel yStepsLanel = new JLabel("Y Steps");
        yStepsLanel.setBounds(20, y, 60,33);
        leftPanel.add(yStepsLanel);
        ySteps = new JTextField("100");
        ySteps.setBounds(80, y, 195,33);
        leftPanel.add(ySteps);
        
        y+= ySeperation;
        JLabel zStepsLanel = new JLabel("Z Steps");
        zStepsLanel.setBounds(20, y, 60,33);
        leftPanel.add(zStepsLanel);
        zSteps = new JTextField("100");
        zSteps.setBounds(80, y, 195,33);
        leftPanel.add(zSteps);
        
        
        
        
        centerPreviewPanel = new JPanel()
        {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int viewWidth = this.getWidth();
                int viewHeight = this.getHeight();
                double xScale = viewWidth / (maxPoint.x - minPoint.x) / 1.2;
                double yScale = viewHeight / (maxPoint.y - minPoint.y) / 1.2;
                double scale = Math.min(xScale, yScale);
                double xCentre = ((maxPoint.x - minPoint.x)/2) + minPoint.x;
                double yCentre = ((maxPoint.y - minPoint.y)/2) + minPoint.y;
                
                // Draw torch position
                int cursorX = viewWidth/2;
                int cursorY = viewHeight/2;
                if(cursorPoint != null ){
                    cursorX = (int)( (cursorPoint.x - xCentre ) * scale) + (viewWidth/2) ;
                    cursorY = (int)( (-cursorPoint.y - yCentre ) * scale) + (viewHeight/2) ;
                }
                g.setColor(new Color(180, 180, 180));
                g.drawLine(cursorX, 0, cursorX, viewHeight); // Cross vertical
                g.drawLine(0, cursorY, viewWidth, cursorY); // Cross horizontal
                //System.out.println("repaint cursor " + cursorX + " " + cursorY);
                
                Vec3 previousPoint = null;
                
                
                for(int i = 1; i < cutProfiles.size(); i++){
                    Vector<Vec3> currCutProfile = (Vector<Vec3>) cutProfiles.elementAt(i);
                    //System.out.println("  Profile ");
                    for(int j = 1; j < currCutProfile.size(); j++){
                        
                        Vec3 pointA = currCutProfile.elementAt(j-1);
                        Vec3 pointB = currCutProfile.elementAt(j);
                        //System.out.println("    - p " + point);
                        if(pointA != null && pointB != null){
                            // If applicable draw movement when torch is off.
                            if(previousPoint != null){
                                g.setColor(Color.YELLOW);
                                g.drawLine((int)( (previousPoint.x - xCentre ) * scale) + (viewWidth/2),
                                           (int)( (-previousPoint.y - yCentre) * scale) + (viewHeight/2),
                                           (int)( (pointA.x-xCentre) * scale) + (viewWidth/2),
                                           (int)( (-pointA.y-yCentre) * scale) + (viewHeight/2));
                            }
                            
                            // Draw line for torch cut
                            if(j < currCutProfile.size() - 1 ){
                                g.setColor(Color.RED);
                            } else {
                                g.setColor(Color.YELLOW);
                            }
                            g.drawLine((int)( (pointA.x - xCentre) * scale) + (viewWidth/2),
                                       (int)( (-pointA.y - yCentre) * scale) + (viewHeight/2),
                                       (int)( (pointB.x - xCentre) * scale) + (viewWidth/2),
                                       (int)( (-pointB.y - yCentre) * scale) + (viewHeight/2));
                            
                            // Draw first line, as torch was started from prev point
                            System.out.println(" j " + j + " size " + cutProfiles.size() );
                            if(j == 1 && cutProfiles.size() > 0){
                                Vector<Vec3> prevCutProfile = (Vector<Vec3>) cutProfiles.elementAt(i-1);
                                Vec3 pointZ = prevCutProfile.elementAt(prevCutProfile.size()-1);
                                
                                //System.out.println(" *** "  );
                                // Draw line for torch cut
                                g.setColor(Color.GREEN);
                                g.drawLine((int)( (pointZ.x - xCentre) * scale) + (viewWidth/2),
                                           (int)( (-pointZ.y - yCentre) * scale) + (viewHeight/2),
                                           (int)( (pointA.x - xCentre) * scale) + (viewWidth/2),
                                           (int)( (-pointA.y - yCentre) * scale) + (viewHeight/2));
                            }
                            
                            
                            previousPoint = pointB;
                        }
                        
                    }
                    
                }
                
            };
            
        };
           
        centerPreviewPanel.setSize(dim.width - 640, dim.height/2);
        centerPreviewPanel.setPreferredSize(new Dimension(dim.width - 640, dim.height/2));
        centerPreviewPanel.setBackground(new Color(50,50,50));
        
        /*
        Vec3 v[] = new Vec3[2]; v[0] = new Vec3(-1,-1,-1); v[1] = new Vec3(1, 1, 1);
        float s[] = new float[2]; s[0] = 1; s[1] = 1;
        Curve curve = new Curve(v, s, 1, false); // public Curve(Vec3 v[], float smoothness[], int smoothingMethod, boolean isClosed)
        ObjectInfo oi = new ObjectInfo(curve, new CoordinateSystem(), "TEST");
        
        BFrame bFrame = new BFrame();
        //bFrame.setBounds(new Rectange(0,0,300,300));
        EditingWindow ew = new CurveEditorWindow(null, "X", oi); // EditingWindow parent, String title, ObjectInfo obj
        MeshEditorWindow mew = new CurveEditorWindow(ew , "TEST", oi);
        // MeshEditController window, RowContainer p
        RowContainer p = new RowContainer();
        theView = new CurveViewer(mew, p); // CurveViewer-> ViewerCanvas -> CustomWidget
        
        theView.setMinimumSize(new java.awt.Dimension(400, 400));
        theView.setPreferredSize(new java.awt.Dimension(400, 400));
        System.out.println(" theView getPreferredSize " + theView.getPreferredSize() );
        //theView.setBounds(0, 0, 195, 200);
        
        System.out.println(" tjeView size " + theView.getBounds() );
        
        MoveViewTool metaTool = new MoveViewTool(ew);
        RotateViewTool altTool = new RotateViewTool(ew);
        theView.setMetaTool( metaTool );
        theView.setAltTool(altTool);
        
        theView.setTool(metaTool);
        
        
        // JPanel  CustomWidget  ->   theView is   buoy.widget
        // ****   want java.awt.Component
        // AWTWidget
        
        java.awt.Component comp = theView.getComponent();
        
        System.out.println("comp " + comp.getWidth() );
        //comp.setSize(400,400);
        //centerPanel.add(theView.getComponent());
        //centrePanel.add(new JButton("asd"));
        
        //ew is BFrame
        //centerPanel.add(ew.getComponent());
        */
        
        /*
        MeshViewer view = (MeshViewer) theView[i];
          view.setBackground(new Color(220, 220, 220)); // no effect
        view.setMetaTool(metaTool);
        view.setAltTool(altTool);
        view.setMeshVisible(true);
        */
        
        
        JPanel centerPanelText = new JPanel();
        centerPanelText.setLayout(new BorderLayout());
        centerPanelText.setSize(dim.width - 640, dim.height/2);
        centerPanelText.setPreferredSize(new Dimension(dim.width - 600, dim.height/2));
        centerPanelText.setBackground(new Color(220,220,220));
        
        JPanel rightPanel = new JPanel();
        rightPanel.setPreferredSize(new Dimension(300, dim.height));
        rightPanel.setSize(300, dim.height);
        rightPanel.setMinimumSize(new Dimension(300, dim.height));
        rightPanel.setBackground(new Color(220,220,220));
        rightPanel.setLayout(null);

        y = 20;
        
        
        JLabel xLanel = new JLabel("X");
        xLanel.setBounds(20, y, 40,33);
        rightPanel.add(xLanel);
        xPos = new JTextField(""+cursorPoint.x);
        xPos.setBounds(60, y, 215,33);
        xPos.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent e) {
            warn();
          }
          public void removeUpdate(DocumentEvent e) {
            warn();
          }
          public void insertUpdate(DocumentEvent e) {
            warn();
          }
          public void warn() {
             if(isNumeric(xPos.getText())){
                 cursorPoint.x = Double.parseDouble(xPos.getText());
                 centerPreviewPanel.repaint();
             }
          }
        });
        rightPanel.add(xPos);
        y+= ySeperation;
        
        JLabel yLanel = new JLabel("Y");
        yLanel.setBounds(20, y, 40,33);
        rightPanel.add(yLanel);
        yPos = new JTextField(""+cursorPoint.y);
        yPos.setBounds(60, y, 215,33);
        yPos.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent e) {
            warn();
          }
          public void removeUpdate(DocumentEvent e) {
            warn();
          }
          public void insertUpdate(DocumentEvent e) {
            warn();
          }
          public void warn() {
             if(isNumeric(yPos.getText())){
                 cursorPoint.y = Double.parseDouble(yPos.getText());
                 centerPreviewPanel.repaint();
             }
          }
        });
        rightPanel.add(yPos);
        y+= ySeperation;
        
        JLabel zLanel = new JLabel("Z");
        zLanel.setBounds(20, y, 40,33);
        rightPanel.add(zLanel);
        zPos = new JTextField(""+cursorPoint.z);
        zPos.setBounds(60, y, 215,33);
        zPos.getDocument().addDocumentListener(new DocumentListener() {
          public void changedUpdate(DocumentEvent e) {
            warn();
          }
          public void removeUpdate(DocumentEvent e) {
            warn();
          }
          public void insertUpdate(DocumentEvent e) {
            warn();
          }
          public void warn() {
             if(isNumeric(zPos.getText())){
                 cursorPoint.z = Double.parseDouble(zPos.getText());
                 //centerPreviewPanel.repaint();
             }
          }
        });
        rightPanel.add(zPos);
        
    
        y+= ySeperation;
        goToZeroButton = new JButton("Go To Zero");
        //testButton.setForeground(Color.RED);
        goToZeroButton.addActionListener( this );
        goToZeroButton.setBounds(20, y, 121,33); // x,y,w,h
        rightPanel.add(goToZeroButton);
        
        setZeroButton = new JButton("Set Zero");
        setZeroButton.setForeground(new Color(50, 50, 200));
        setZeroButton.addActionListener( this );
        setZeroButton.setBounds(154, y, 121,33); // x,y,w,h
        rightPanel.add(setZeroButton);
        
        y+= ySeperation;
        
        y+= ySeperation;
        JLabel IPMLanel = new JLabel("Inches per minute");
        IPMLanel.setBounds(20, y, 100,33);
        rightPanel.add(IPMLanel);
        inchesPerMinute = new JTextField("100");
        inchesPerMinute.setBounds(125, y, 150,33);
        rightPanel.add(inchesPerMinute);
        
        y+= ySeperation;
        
        moveXLeftButton = new JButton("X -");
        //moveXLeftButton.setForeground(Color.RED);
        moveXLeftButton.addActionListener( this );
        moveXLeftButton.setBounds(30, y + ySeperation, 45,45); // x,y,w,h
        rightPanel.add(moveXLeftButton);
        
        moveXRightButton = new JButton("X +");
        //moveXRightButton.setForeground(Color.RED);
        moveXRightButton.addActionListener( this );
        moveXRightButton.setBounds(120, y + ySeperation, 45,45); // x,y,w,h
        rightPanel.add(moveXRightButton);
        
        
        moveYLeftButton = new JButton("Y +");
        //moveYLeftButton.setForeground(Color.RED);
        moveYLeftButton.addActionListener( this );
        moveYLeftButton.setBounds(75, y , 45,45); // x,y,w,h
        rightPanel.add(moveYLeftButton);
        
        moveYRightButton = new JButton("Y -");
        //moveYRightButton.setForeground(Color.RED);
        moveYRightButton.addActionListener( this );
        moveYRightButton.setBounds(75, y + (ySeperation*2), 45,45); // x,y,w,h
        rightPanel.add(moveYRightButton);
        
        moveZLeftButton = new JButton("Z +");
        //moveZLeftButton.setForeground(Color.RED);
        moveZLeftButton.addActionListener( this );
        moveZLeftButton.setBounds(210, y , 45,45); // x,y,w,h
        rightPanel.add(moveZLeftButton);
        
        moveZRightButton = new JButton("Z -");
        //moveZRightButton.setForeground(Color.RED);
        moveZRightButton.addActionListener( this );
        moveZRightButton.setBounds(210, y + (ySeperation*2), 45,45); // x,y,w,h
        rightPanel.add(moveZRightButton);
        
        
        
        textArea = new JTextArea("");
        textArea.setFont(new Font("SansSerif", Font.PLAIN, 16));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setSize(600, 200); // centerPanel2.getWidth(), centerPanel2.getHeight());
        textArea.setMargin(new Insets(4,4,4,4));
        
        final DefaultCaret caret = new DefaultCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        textArea.setCaret(caret);
        
        
        textArea.addCaretListener(new CaretListener() {
            public void caretUpdate(CaretEvent e) {
                JTextArea editArea = (JTextArea)e.getSource();
                int linenum = 1;
                int columnnum = 1;
                try {
                    int caretpos = textArea.getCaretPosition();
                    linenum = textArea.getLineOfOffset(caretpos);
                    columnnum = caretpos - textArea.getLineStartOffset(linenum);

                    // Update line number and highlight.
                    gCodeLineNumber = linenum;
                    
                    highlightActiveGCodeFileLine();
                }
                catch(Exception ex) { }
            }
        });

        LineNumberingTextArea lineNumberingTextArea = new LineNumberingTextArea(textArea);
        lineNumberingTextArea.setFont(new Font("SansSerif", Font.PLAIN, 16));
        lineNumberingTextArea.setMargin(new Insets(4,6,4,6));
        lineNumberingTextArea.setForeground(new Color(200,200,200));
        lineNumberingTextArea.setBackground(new Color(255,255,255));
        
        
        gCodeScrollPane = new JScrollPane (textArea,
           JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        
        
        gCodeScrollPane.setRowHeaderView(lineNumberingTextArea);
        
        //centerPanelText.add(textArea);
        centerPanelText.add(gCodeScrollPane);
        
        textArea.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent documentEvent)
            {
                lineNumberingTextArea.updateLineNumbers();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent)
            {
                lineNumberingTextArea.updateLineNumbers();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent)
            {
                lineNumberingTextArea.updateLineNumbers();
            }
        });
        
        
        JSplitPane sp3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerPreviewPanel, centerPanelText);
        JSplitPane sp2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sp3, rightPanel);
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, sp2 ); // centerPanel
        
        flattenJSplitPane(sp3);
        flattenJSplitPane(sp2);
        flattenJSplitPane(sp);
        
        //sp.setDividerSize(0);
        sp.setDividerLocation(300);
        sp2.setDividerLocation(dim.width - 620 - 0);
        sp3.setDividerLocation(dim.height /2);
        this.add(sp, BorderLayout.CENTER);
        //this.add(leftPanel);
        //this.add(centerPanel);
        //this.add(rightPanel);
        
        //GridLayout experimentLayout = new GridLayout(0,2);
        
        /*
        c.weightx = 0.5;
            
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        this.add(leftPanel, c);
        
        
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 0;
        this.add(centerPanel, c);
        
        //button = new JButton("Button 3");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 2;
        c.gridy = 0;
        this.add(rightPanel, c);
        */
        
        ImageIcon iconImage = new ImageIcon(getClass().getResource("/armarender/Icons/favicon-32x32.png"));
        this.setIconImage(iconImage.getImage());
        
        
        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Dimension windowDim = new Dimension((screenBounds.width), (screenBounds.height));
        setBounds(new Rectangle((screenBounds.width-windowDim.width), (screenBounds.height-windowDim.height), windowDim.width, windowDim.height));
        
        this.setLocation(0,0); // dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);
        this.setPreferredSize(new Dimension(windowDim.width, windowDim.height));
        this.pack();
        
        //this.setLocation(0,0); // dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);
        //this.setPreferredSize(new Dimension(dim.width, dim.height));
        this.pack();

        //5. Show it.
        this.setVisible(true);
        

        //4. Size the frame.
        /*
        frame.setLocation(0,0); // dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);
        frame.setPreferredSize(new Dimension(dim.width, dim.height));
        frame.pack();

        //5. Show it.
        frame.setVisible(true);
        */
        
        
        //SimpleRead serialPort = new SimpleRead();
    }
    
    
    /**
     * readSerialDevices
     *
     * Description: 
     */
    public void readSerialDevices(){
        
    }
    
    
    /**
     * highlightActiveGCodeFileLine
     *
     * Description: highlight the active gcode line being processed.
     */
    public void highlightActiveGCodeFileLine(){
        try{
            int startIndex = textArea.getLineStartOffset(gCodeLineNumber);
            int endIndex = textArea.getLineEndOffset(gCodeLineNumber);
            painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(210,210,210));
            textArea.getHighlighter().removeAllHighlights();
            textArea.getHighlighter().addHighlight(startIndex, endIndex, painter);
            
            
            int progress = 1;
            if(textArea.getLineCount() > 0){
                progress = (int)(((double)gCodeLineNumber / (double)textArea.getLineCount()) * 100);
            }
            this.progressBar.setValue(progress);
            
            
            String cursorLine = textArea.getText().substring(startIndex, endIndex);
            //System.out.println(" cursor line: " + cursorLine);
            
            Vec3 updatedPoint = parseGCodeLine(cursorLine);
            if(updatedPoint != null){
                cursorPoint = updatedPoint;
                centerPreviewPanel.repaint();
                
                // Update position values.
                xPos.setText(""+cursorPoint.x);
                yPos.setText(""+cursorPoint.y);
                zPos.setText(""+cursorPoint.z);
            }
            
            if(cursorLine.toLowerCase().indexOf("m3") != -1 ||
               cursorLine.toLowerCase().indexOf("m4") != -1){ // Start cut profile
                //System.out.println("ON");
                torchActiveLabel.setText("Torch On");
            }
            if(cursorLine.toLowerCase().indexOf("m5") != -1){ // End cut profile
                torchActiveLabel.setText("Torch Off");
            }
            
        } catch (Exception e){
            
        }
    }
    
    
    /**
     * actionPerformed
     *
     * Description: handle button events.
     */
    public void actionPerformed( ActionEvent event )
    {
        System.out.println(""+ event.getSource().getClass().getName()  );
        if(event.getSource() == loadFileButton){
            //textArea.setText("GCode file");
            
            JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
            int returnValue = jfc.showOpenDialog(null);
            // int returnValue = jfc.showSaveDialog(null);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = jfc.getSelectedFile();
                //System.out.println("-> " + selectedFile.getAbsolutePath());
                try {
                    // Load file
                    String gCodeContent = readFile(selectedFile.getAbsolutePath(), Charset.defaultCharset());
                    textArea.setText(gCodeContent);
                    
                    // highlight first line
                    if(textArea.getText().length() > 1){
                        int startIndex = textArea.getLineStartOffset(gCodeLineNumber);
                        int endIndex = textArea.getLineEndOffset(gCodeLineNumber);
                        painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(210,210,210));
                        textArea.getHighlighter().addHighlight(startIndex, endIndex, painter);
                        
                        highlightActiveGCodeFileLine();
                    }
                    
                    // Scroll to top
                    gCodeScrollPane.getVerticalScrollBar().setValue(0);
                    textArea.setCaretPosition(0);
                    
                    // Set Label
                    String fileShort = selectedFile.getName();
                    gCodeFileLabel.setText(fileShort);
                    
                    // Process file
                    pricessGCodeFile(gCodeContent);
                } catch (Exception e){
                    
                }
            }
        }
        
        
        if(event.getSource() == stopButton){
            running = false;
            System.out.println("Stop");
        }
        
        if(event.getSource() == testButton){
            running = false;
            live = false;
        }
        
        if(event.getSource() == liveButton){
            running = false;
            live = true;
        }
        
        if(event.getSource() == runButton && running == true){
            running = true;
        }
        
        if(event.getSource() == goToZeroButton){
            //x = 0;
            //y = 0;
            //z = 0;
            cursorPoint.x = 0;
            cursorPoint.y = 0;
            cursorPoint.z = 0;
            xPos.setText("" + cursorPoint.x);
            yPos.setText("" + cursorPoint.y);
            zPos.setText("" + cursorPoint.z);
        }
        
        if(event.getSource() == setZeroButton){
            //x = 0;
            //y = 0;
            //z = 0;
            cursorPoint.x = 0;
            cursorPoint.y = 0;
            cursorPoint.z = 0;
            xPos.setText("" + cursorPoint.x);
            yPos.setText("" + cursorPoint.y);
            zPos.setText("" + cursorPoint.z);
        }
        
        
        if(event.getSource() == moveXLeftButton){
            //x -= 0.5;
            cursorPoint.x -= .25;
            xPos.setText("" + cursorPoint.x);
            
            // Update point
            //cursorPoint.x = x;
            centerPreviewPanel.repaint();
            
            socket.sendCommand("a 10");
        }
        if(event.getSource() == moveXRightButton){
            //x += 0.25;
            cursorPoint.x += .25;
            xPos.setText("" + cursorPoint.x);
            
            //cursorPoint.x = x;
            centerPreviewPanel.repaint();
            
            socket.sendCommand("s 10");
        }
        if(event.getSource() == moveYLeftButton){
            //y += 0.5;
            cursorPoint.y += .25;
            yPos.setText("" + cursorPoint.y);
            
            //cursorPoint.y = y;
            centerPreviewPanel.repaint();
        }
        if(event.getSource() == moveYRightButton){
            //y -= 0.5;
            cursorPoint.y -= .25;
            yPos.setText("" + cursorPoint.y);
            
            //cursorPoint.y = y;
            centerPreviewPanel.repaint();
        }
        if(event.getSource() == moveZLeftButton){
            //z += 0.5;
            cursorPoint.z += .25;
            zPos.setText("" + cursorPoint.z);
        }
        if(event.getSource() == moveZRightButton){
            //z -= 0.5;
            cursorPoint.z -= .25;
            zPos.setText("" + cursorPoint.z);
        }
    }
    
    
    /**
     * pricessGCodeFile
     *
     * Description:
     */
    public void pricessGCodeFile(String text){
        StringTokenizer st = new StringTokenizer(text, "\n");
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = Double.MIN_VALUE;
        
        int lineCount = 0;
        
        Vec3 previousVec = null;
        
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            
            System.out.println(line);
            
            Vec3 vec = parseGCodeLine(line);
            if(vec != null){
                if(vec.x < minX){
                    minX = vec.x;
                }
                if(vec.x > maxX){
                    maxX = vec.x;
                }
                if(vec.y < minY){
                    minY = vec.y;
                }
                if(vec.y > maxY){
                    maxY = vec.y;
                }
                if(vec.z < minZ){
                    minZ = vec.z;
                }
                if(vec.z > maxZ){
                    maxZ = vec.z;
                }
            }
            
            
            if(line.toLowerCase().indexOf("m3") != -1 ||
               line.toLowerCase().indexOf("m4") != -1){ // Start cut profile
                //int size = cutProfiles.size();
                Vector<Vec3> newCutProfile = new Vector<Vec3>();
                
                // if previous point add, to cut
                //if(previousVec != null){
                    //newCutProfile.addElement(previousVec);
                    //System.out.println("***" + previousVec);
                //}
                
                cutProfiles.addElement(newCutProfile);
                
                //torchActiveLabel.setText("Torch On");
            }
            
            if(line.toLowerCase().indexOf("m5") != -1){ // End cut profile
                //torchActiveLabel.setText("Torch Off");
            }
            
            if(cutProfiles.size() > 0 && vec != null){ // add vec point to latest cut profile
                Vector<Vec3> currCutProfile = (Vector<Vec3>)cutProfiles.elementAt(cutProfiles.size() - 1);
                currCutProfile.addElement(vec);
                cutProfiles.setElementAt(currCutProfile, cutProfiles.size() - 1);
            }
             
            //System.out.println(  "   - "   + " lineCount " + lineCount);
            lineCount++;
            previousVec = vec;
        }
        
        
        // Set bounds recatangle
        minPoint = new Vec3(minX, minY, minZ);
        maxPoint = new Vec3(maxX, maxY, maxZ);
        
        //System.out.println(" bounds " + minPoint + " " + maxPoint);
        
        /*
        for(int i = 0; i < cutProfiles.size(); i++){
            Vector<Vec3> currCutProfile = (Vector<Vec3>) cutProfiles.elementAt(i);
            System.out.println("  Profile ");
            for(int j = 0; j < currCutProfile.size(); j++){
                Vec3 point = currCutProfile.elementAt(j);
                System.out.println("    - p " + point);
            }
        }
         */
        centerPreviewPanel.repaint();
    }
    
    /**
     * parseGCodeLine
     *
     * Description: Parse a gcode line
     */
    public Vec3 parseGCodeLine(String line){
        double xValue = 0;
        double yValue = 0;
        double zValue = 0;
        boolean isPoint = false;
        try {
            // G1 X2.515 Y.82
            String xToken = new String(line);
            int xIndex = xToken.toLowerCase().indexOf("x");
            if(xIndex != -1){
                int xEnd = xToken.indexOf(" ", xIndex);
                if(xEnd != -1){
                    xToken = xToken.substring(xIndex + 1, xEnd);
                } else {
                    xToken = xToken.substring(xIndex + 1);
                }
                if(isNumeric(xToken)){
                    xValue = Double.parseDouble(xToken);
                    isPoint = true;
                }
            }
            String yToken = new String(line);
            int yIndex = yToken.toLowerCase().indexOf("y");
            if(yIndex != -1){
                int yEnd = yToken.indexOf(" ", yIndex);
                if(yEnd != -1){
                    yToken = yToken.substring(yIndex + 1, yEnd);
                } else {
                    yToken = yToken.substring(yIndex + 1);
                }
                if(isNumeric(yToken)){
                    yValue = Double.parseDouble(yToken);
                    isPoint = true;
                }
            }
            String zToken = new String(line);
            int zIndex = zToken.toLowerCase().indexOf("z");
            if(zIndex != -1){
                int zEnd = zToken.indexOf(" ", zIndex);
                if(zEnd != -1){
                    zToken = zToken.substring(zIndex + 1, zEnd);
                } else {
                    zToken = zToken.substring(zIndex + 1);
                }
                if(isNumeric(zToken)){
                    zValue = Double.parseDouble(zToken);
                    isPoint = true;
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        Vec3 result = null;
        if(isPoint){
            result = new Vec3(xValue, yValue, zValue);
        }
        return result;
    }
    
    
    public boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
    
    /**
     * readFile
     *
     * Description: Read file contents.
     */
    static String readFile(String path, Charset encoding) throws IOException
    {
      byte[] encoded = Files.readAllBytes(Paths.get(path));
      return new String(encoded, encoding);
    }
    
    /**
     * Makes a split pane invisible. Only contained components are shown.
     *
     * @param splitPane
     */
    public static void flattenJSplitPane(JSplitPane splitPane) {
        splitPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        BasicSplitPaneUI flatDividerSplitPaneUI = new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void setBorder(Border b) {
                    }
                };
            }
        };
        splitPane.setUI(flatDividerSplitPaneUI);
        splitPane.setBorder(null);
    }
    
    
    /**
     * LineNumberingTextArea
     *
     */
    public class LineNumberingTextArea extends JTextArea
    {
        private JTextArea textArea;

        public LineNumberingTextArea(JTextArea textArea)
        {
            this.textArea = textArea;
            setBackground(Color.LIGHT_GRAY);
            setEditable(false);
        }

        public void updateLineNumbers()
        {
            String lineNumbersText = getLineNumbersText();
            setText(lineNumbersText);
        }

        private String getLineNumbersText()
        {
            int caretPosition = textArea.getDocument().getLength();
            Element root = textArea.getDocument().getDefaultRootElement();
            StringBuilder lineNumbersTextBuilder = new StringBuilder();
            lineNumbersTextBuilder.append("1").append(System.lineSeparator());

            for (int elementIndex = 2; elementIndex < root.getElementIndex(caretPosition) + 2; elementIndex++)
            {
                lineNumbersTextBuilder.append(elementIndex).append(System.lineSeparator());
            }

            return lineNumbersTextBuilder.toString();
        }
    }
    
    
    
    
}
