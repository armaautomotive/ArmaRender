/* Copyright (C) 2023 by Jon Taylor

 This program is free software; you can redistribute it and/or modify it under the
 terms of the GNU General Public License as published by the Free Software
 Foundation; either version 2 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 
 */

package armarender;

import armarender.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.util.*;
import javax.swing.JComboBox;
import armarender.object.*;
import armarender.math.*;
import armarender.*;
import javax.swing.*; // For JOptionPane
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

public class ScaleSettings {
    LayoutWindow window = null;
    String[] unitStrings = {"mm", "cm", "meters", "inches", "feet"};
    String[] unitSymbols = {"mm", "cm", "m", "\"", "'" };
    
    /**
     * ScaleSettings
     * Description:
     */
    public ScaleSettings(LayoutWindow layout) {
        this.window = layout;
    }
    
    /**
     * display
     *
     */
    public void display(){
        double scale = window.getScene().getScale();
        int units = window.getScene().getUnits();
        int exportUnits = window.getScene().getExportUnits();
        
        JPanel panel = new JPanel();
        //panel.setBackground(new Color(0, 0, 0));
        panel.setSize(new Dimension(100, 100));
        panel.setLayout(null);
        
        int yPos = 0;
        
        JLabel scaleLabel = new JLabel("Scale Factor");
        //scaleLabel.setForeground(new Color(255, 255, 0));
        scaleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        scaleLabel.setFont(new Font("Arial", Font.BOLD, 11));
        scaleLabel.setBounds(0, yPos, 130, 40); // x, y, width, height
        panel.add(scaleLabel);
        
        JTextField scaleField = new JTextField(new String(scale+""));
        scaleField.setBounds(130, yPos, 130, 40); // x, y, width, height
        panel.add(scaleField);
        //scaleField.getDocument().addDocumentListener(myListener);
        
        yPos += 40;
        
        JLabel unitsLabel = new JLabel("Design Units");
        //unitsLabel.setForeground(new Color(255, 255, 0));
        unitsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        unitsLabel.setFont(new Font("Arial", Font.BOLD, 11));
        unitsLabel.setBounds(0, yPos, 130, 40); // x, y, width, height
        panel.add(unitsLabel);
        
        //JTextField scaleField = new JTextField(new String(scale+""));
        //scaleField.setBounds(130, yPos, 130, 40); // x, y, width, height
        //panel.add(scaleField);
        
        //Create the combo box, select item at index 4.
        //Indices start at 0, so 4 specifies the pig.
        JComboBox unitList = new JComboBox(unitStrings);
        
        unitList.setSelectedIndex(units);
        //unitList.addActionListener(this);
        unitList.setBounds(130, yPos, 130, 40); // x, y, width, height
        panel.add(unitList);
        
        
        yPos += 40;
        
        JLabel exportUnitsLabel = new JLabel("Export Units");
        //exportUnitsLabel.setForeground(new Color(255, 255, 0));
        exportUnitsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        exportUnitsLabel.setFont(new Font("Arial", Font.BOLD, 11));
        exportUnitsLabel.setBounds(0, yPos, 130, 40); // x, y, width, height
        panel.add(exportUnitsLabel);
        
        JComboBox exportUnitList = new JComboBox(unitStrings);
        
        exportUnitList.setSelectedIndex(exportUnits); //
        //unitList.addActionListener(this);
        exportUnitList.setBounds(130, yPos, 130, 40); // x, y, width, height
        panel.add(exportUnitList);
        
        
        UIManager.put("OptionPane.minimumSize",new Dimension(400, 200));
        
        ImageIcon icon = new ImageIcon(getClass().getResource("/armarender/Icons/favicon-32x32.png"));
        
        int result = JOptionPane.showConfirmDialog(null, panel, "Scale Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.NO_OPTION, icon);
        if (result == JOptionPane.OK_OPTION) {
            scale = Double.parseDouble(scaleField.getText());
            window.getScene().setScale(scale);
            units = unitList.getSelectedIndex();
            window.getScene().setUnits(units);
            exportUnits = exportUnitList.getSelectedIndex();
            window.getScene().setExportUnits(exportUnits);
        }
    }
    
    /**
     *
     */
    public String getSymbol(){
        String symbol = "";
        int units = window.getScene().getUnits();
        if(units < unitSymbols.length){
            symbol = unitSymbols[units];
        }
        return symbol;
    }
    
    public String getLabel(){
        String label = "";
        int units = window.getScene().getUnits();
        if(units < unitStrings.length){
            label = unitStrings[units];
        }
        return label;
    }
    
    public String getExportSymbol(){
        String symbol = "";
        int units = window.getScene().getExportUnits();
        if(units < unitSymbols.length){
            symbol = unitSymbols[units];
        }
        return symbol;
    }
    
    public String getExportLabel(){
        String label = "";
        int units = window.getScene().getExportUnits();
        if(units < unitStrings.length){
            label = unitStrings[units];
        }
        return label;
    }
    
    
    /**
     * getInchScaleValue
     *
     * Description: Return a scale value to transform scene dimensions into inches. Used for fabrication with parts in inches.
     */
    public double getInchScaleValue(){
        double scale = 1.0;
        String symbol = getSymbol();
        
        // Millimeters
        if(symbol.equals("mm")){
            scale = 25.39;
        }
        
        // Centimeters
        if(symbol.equals("cm")){
            scale = 2.539;
        }
        
        // Meters
        if(symbol.equals("m")){
            scale = 0.025399986284007;
        }
        
        // Feet
        if(symbol.equals("'")){
            scale = 0.08333333;
        }
        
        // Default Inches doesn't change.
        return scale;
    }
    
    
    /**
     * getExportScaleValue
     *
     * Description: Get scale multiple to export in CNC machine units not modeling units.
     *
     * @return: scale factor.
     */
    public double getExportScaleValue(){
        double exportScale = 1.0;
        double sceneScale = getInchScaleValue();
        
        // Scene inch -> inch =   1 -> 1
        // Scene MM -> inch   =   45 -> 1.75           1.75/45   .0388  (exp / scene)
        // Scene Inch -> mm =      1.75 -> 45          45/1.75   25.7
        // Scene Merter -> inch  =  0.0254 -> 1        1/.025   40
        
        String exportSymbol = getExportSymbol();
        
        // Millimeters
        if(exportSymbol.equals("mm")){
            exportScale = 25.39;
        }
        
        // Centimeters
        if(exportSymbol.equals("cm")){
            exportScale = 2.539;
        }
        
        // Meters
        if(exportSymbol.equals("m")){
            exportScale = 0.025399986284007;
        }
        
        // Feet
        if(exportSymbol.equals("'")){
            exportScale = 0.08333333;
        }
        
        // sceneScale
        exportScale = exportScale / sceneScale; // export divided by scene
        
        //System.out.println("getExportScaleValue: " + exportScale );
        
        return exportScale;
    }
    
    /**
     * getExportInchScaleValue
     *
     * Description: 
     */
    public double getExportInchScaleValue(){
        double exportScale = 1.0;
        String exportSymbol = getExportSymbol();
        
        // Millimeters
        if(exportSymbol.equals("mm")){
            exportScale = 25.39;
        }
        
        // Centimeters
        if(exportSymbol.equals("cm")){
            exportScale = 2.539;
        }
        
        // Meters
        if(exportSymbol.equals("m")){
            exportScale = 0.025399986284007;
        }
        
        // Feet
        if(exportSymbol.equals("'")){
            exportScale = 0.08333333;
        }
        
        return exportScale;
    }
}



