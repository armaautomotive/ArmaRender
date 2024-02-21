/*  Copyright (C) 2023 by Jon Taylor
    
   TODO: randome pick image for splash.
 */

package armarender;

import armarender.*;
import armarender.animation.*;
import armarender.material.*;
import armarender.math.*;
import armarender.texture.*;
import armarender.ui.*;
import buoy.widget.*;
import java.io.*;
import javax.swing.WindowConstants;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import buoy.widget.*;
import buoy.xml.*;
import java.awt.FlowLayout;
import java.awt.*;
import javax.swing.*;
import java.awt.event.WindowEvent;
import javax.swing.border.EmptyBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

public class Splash extends JDialog // BDialog
{

    public class DrawingPanel extends JPanel {
        public DrawingPanel(){
            
            
            setSize(900, 167);
        }
        
        @Override
        public void paintComponent (Graphics g)
        {
            this.myPaint(g);
        }
        
        private void myPaint(Graphics g)
        {
            super.paintComponent(g);
            
            String botImage = "armarender/titleImages/footer.png";
            ImageIcon botIcon = new IconResource(botImage);
            g.drawImage(botIcon.getImage(), 0, 0, null);
            
            
            //IconResource ir = new IconResource(botImage);
            //String rn = ir.getResourceName();
            //System.out.println("  *** rn: " + rn );
            
            String version = ArmaRender.getVersion();
            
            int xOffset = 711;
            int yOffset = 95;
            
            if(version.length() == 7){
                xOffset -= 22;
            }
            
            String versionV = "armarender/titleImages/v.png";
            ImageIcon versionVIcon = new IconResource(versionV);
            g.drawImage(versionVIcon.getImage(), xOffset, yOffset + 9, null);
            xOffset += versionVIcon.getIconWidth() + 6;
            
            for(int i = 0; i < version.length(); i++){
                
                if(version.charAt(i) == '.'){
                    String versionZero = "armarender/titleImages/dot.png";
                    ImageIcon versionZeroIcon = new IconResource(versionZero);
                    g.drawImage(versionZeroIcon.getImage(), xOffset + 1, yOffset + 23, null);
                    
                    xOffset += versionZeroIcon.getIconWidth() + 6;
                }
                
                if(version.charAt(i) == '0'){
                    String versionZero = "armarender/titleImages/0.png";
                    ImageIcon versionZeroIcon = new IconResource(versionZero);
                    g.drawImage(versionZeroIcon.getImage(), xOffset, yOffset, null);
                    
                    xOffset += versionZeroIcon.getIconWidth() + 4;
                }
                
                if(version.charAt(i) == '1'){
                    String versionZero = "armarender/titleImages/1.png";
                    ImageIcon versionZeroIcon = new IconResource(versionZero);
                    g.drawImage(versionZeroIcon.getImage(), xOffset, yOffset, null);
                    
                    xOffset += versionZeroIcon.getIconWidth() + 4;
                }
                
                if(version.charAt(i) == '2'){
                    String versionZero = "armarender/titleImages/2.png";
                    ImageIcon versionZeroIcon = new IconResource(versionZero);
                    g.drawImage(versionZeroIcon.getImage(), xOffset, yOffset, null);
                    
                    xOffset += versionZeroIcon.getIconWidth() + 4;
                }
                
                if(version.charAt(i) == '3'){
                    String digitPath = "armarender/titleImages/3.png";
                    ImageIcon versionDigitIcon = new IconResource(digitPath);
                    g.drawImage(versionDigitIcon.getImage(), xOffset, yOffset, null);
                    xOffset += versionDigitIcon.getIconWidth() + 4;
                }
                
                if(version.charAt(i) == '4'){
                    String versionZero = "armarender/titleImages/4.png";
                    ImageIcon versionZeroIcon = new IconResource(versionZero);
                    g.drawImage(versionZeroIcon.getImage(), xOffset, yOffset, null);
                    xOffset += versionZeroIcon.getIconWidth() + 4;
                }
                
                if(version.charAt(i) == '5'){
                    String versionZero = "armarender/titleImages/5.png";
                    ImageIcon versionZeroIcon = new IconResource(versionZero);
                    g.drawImage(versionZeroIcon.getImage(), xOffset, yOffset, null);
                    xOffset += versionZeroIcon.getIconWidth() + 4;
                }
                
                if(version.charAt(i) == '6'){
                    String versionZero = "armarender/titleImages/6.png";
                    ImageIcon versionZeroIcon = new IconResource(versionZero);
                    g.drawImage(versionZeroIcon.getImage(), xOffset, yOffset, null);
                    xOffset += versionZeroIcon.getIconWidth() + 4;
                }
                
                if(version.charAt(i) == '7'){
                    String versionZero = "armarender/titleImages/7.png";
                    ImageIcon versionZeroIcon = new IconResource(versionZero);
                    g.drawImage(versionZeroIcon.getImage(), xOffset, yOffset, null);
                    xOffset += versionZeroIcon.getIconWidth() + 4;
                }
                
                if(version.charAt(i) == '8'){
                    String versionZero = "armarender/titleImages/8.png";
                    ImageIcon versionZeroIcon = new IconResource(versionZero);
                    g.drawImage(versionZeroIcon.getImage(), xOffset, yOffset, null);
                    xOffset += versionZeroIcon.getIconWidth() + 4;
                }
                
                if(version.charAt(i) == '9'){
                    String versionZero = "armarender/titleImages/9.png";
                    ImageIcon versionZeroIcon = new IconResource(versionZero);
                    g.drawImage(versionZeroIcon.getImage(), xOffset, yOffset, null);
                    xOffset += versionZeroIcon.getIconWidth() + 4;
                }

            }
            
            //g.setColor( Color.RED );
            //g.drawOval(30, 30, 20, 20); //  xy width height
            //g.setColor(Color.BLUE);
            //g.fillOval(5, 5, 5, 5);
        }
    }
    
    public Splash(){
        JDialog dialog = this;
        System.out.println("Splash 1");
        new Thread()
        {
            public void run() {
                try
                {
                    int width = 902;
                    int height = 425 + 167 + 2;
                    //System.out.println("1");
                    dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
                    
                    Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                    Dimension windowDim = new Dimension((width), (height));
                    dialog.setBounds(new Rectangle(((screenBounds.width/2) - (windowDim.width/2)),
                                            ((screenBounds.height/2)-(windowDim.height/2)), windowDim.width, windowDim.height));
                    
                    dialog.setPreferredSize(new Dimension(width, height));
                    dialog.setUndecorated(true);
                    dialog.setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
                    //setLayout(new BoxLayout(dialog, BoxLayout.Y_AXIS));
                      
                    dialog.getContentPane().setBackground(Color.WHITE);
                    
                    System.out.println("Splash 2");
                    
                    //FormContainer content = new FormContainer(1, 2); // 10
                    //setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
                    
                    //content.add(widthField = new ValueField(0.0, ValueField.NONE, 7), 1, 0);
                    String topImage = "armarender/titleImages/titleImage0.jpg"; //armadesignstudio/Icons/appIcon.png
                    String botImage = "armarender/titleImages/footer.png";
                    
                    
                    String versionZero = "armarender/titleImages/0.png";
                    
                    
                    //ImageIcon topIcon = new ImageIcon(ImageIO.read(getClass().getResourceAsStream(botImage)));
                    ImageIcon topIcon = new IconResource(topImage);
                    
                    JLabel topImageLabel = new JLabel(topIcon);
                    topImageLabel.setBorder(new EmptyBorder(0, 0, 0, 0));
                    
                    topImageLabel.addMouseListener(new MouseAdapter()
                    {
                        @Override
                        public void mouseClicked(MouseEvent e)
                        {
                            //statement
                            System.out.println("click");
                            close();
                        }
                    });
                    
                    dialog.add(topImageLabel);
                    
                    DrawingPanel bottomPanel = new DrawingPanel();
                    //bottomPanel.setSize(900, 167);
                    //bottomPanel.setMaximumSize(900, 167);
                    bottomPanel.setPreferredSize(new Dimension(900, 167));
                    bottomPanel.addMouseListener(new MouseAdapter()
                    {
                        @Override
                        public void mouseClicked(MouseEvent e)
                        {
                            //statement
                            System.out.println("click");
                            close();
                        }
                    });
                    
                    
                    ImageIcon botIcon = new IconResource(botImage);
                    JLabel botImageLabel = new JLabel(botIcon);
                    botImageLabel.addMouseListener(new MouseAdapter()
                    {
                        @Override
                        public void mouseClicked(MouseEvent e)
                        {
                            //statement
                            System.out.println("click");
                            close();
                        }
                    });
                    
                    //botImageLabel.setBackground(Color.GREEN);
                    //dialog.add(botImageLabel);
                    
                    dialog.add(bottomPanel);
                    
                    // Version
                    ImageIcon versionZeroIcon = new IconResource(versionZero);
                    
                    System.out.println("Splash 10");
                    
                    ((JPanel)getContentPane()).setBorder(BorderFactory.createLineBorder( Color.GRAY )); // border
                    
                    System.out.println("Splash 11");
                    
                    //System.out.println("2");
                    dialog.pack();
                    
                    System.out.println("Splash 12");
                    
                    //System.out.println("3");
                    setModal(true);  // breaks graalvm
                    
                    System.out.println("Splash 13");
                    
                    //System.out.println("4");
                    //setLocationByPlatform(true);
                    
                    dialog.setVisible(true); // ????
                    
                    System.out.println("Splash 100");
                    //System.out.println("5");
                    
                    //Thread.sleep(3000);
                    //System.out.println("GO");
                    //setVisible(false);
                    //dispose();
                    //dialog.close();
                    
                    //content.getComponent().pack();
                    //UIUtilities.centerDialog(this, window); //
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void close(){
        System.out.println("Splash close 1");
        setVisible(false);
        System.out.println("Splash close 2");
        dispose();
        System.out.println("Splash close 3");
    }
    
}
