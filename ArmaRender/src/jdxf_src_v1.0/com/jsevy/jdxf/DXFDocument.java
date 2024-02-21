/*
 * JDXF Library
 * 
 *   Copyright (C) 2018, Jonathan Sevy <jsevy@cs.drexel.edu>
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in all
 *   copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *   SOFTWARE.
 * 
 */

package com.jsevy.jdxf;



/**
 * Class representing a DXF document, which owns a DXFGraphics on which drawing commands can be made. The document's
 * toDXFString method can then be called to generate the DXF text corresponding to the drawing done on the Graphics object.
 * 
 * The typical workflow is as follows:
 * 
 * <pre>
 * 
 * // Create a DXF document and get its associated DXFGraphics instance
 * DXFDocument dxfDocument = new DXFDocument("Example"); 
 * DXFGraphics dxfGraphics = dxfDocument.getGraphics(); 
 * 
 * // Do drawing commands as on any other Graphics. If you have a paint(Graphics) method, 
 * // you can just use it with the DXFGraphics instance since it's a subclass of Graphics. 
 * graphics.setColor(Color.RED);  
 * graphics.setStroke(new BasicStroke(3));
 * graphics.drawLine(0, 0, 1000, 500); 
 * graphics.drawRect(1000, 500, 150, 150); 
 * graphics.drawRoundRect(20, 200, 130, 100, 20, 10); 
 * 
 * // Get the DXF output as a string - it's just text - and  save  in a file for use with a CAD package 
 * String stringOutput = dxfDocument.toDXFString(); 
 * String filePath = "path/to/file.dxf"; 
 * FileWriter fileWriter = new FileWriter(filePath); 
 * fileWriter.write(dxfText); 
 * fileWriter.flush(); 
 * fileWriter.close();
 * 
 * </pre>
 * 
 * @author jsevy
 *
 */
public class DXFDocument
{
    private DXFSection header = new DXFSection("HEADER");
    private DXFSection classes = new DXFSection("CLASSES");
    private DXFSection tables = new DXFSection("TABLES");
    private DXFSection blocks = new DXFSection("BLOCKS");
    private DXFSection entities = new DXFSection("ENTITIES");
    private DXFSection objects = new DXFSection("OBJECTS");
    
    private String documentComment = "";
    private DXFHeader acadHeader;
    
    private DXFGraphics graphics;
    
    
    /**
     * Create a new DXFDocument
     */
    public DXFDocument()
    {
        this("");
    }
    
    
    /**
     * Create a new DXF document with the specified comment in its header
     * 
     * @param documentComment  Comment for the document
     */
    public DXFDocument(String documentComment)
    {
        this.documentComment = documentComment;
        this.graphics = new DXFGraphics(this);
        
        // add stuff expected to be there by AutoCAD, even though this is not stuff required by the DXF standards...
        // HEADER needs to have AutoCAD version and max handle used
        generateAcadExtras();
    }
    
    
    /**
     * Add stuff that's not actually required in the DXF document - and not used here - but still
     * required to keep AutoCAD (at least the online viewer) happy.
     */
    private void generateAcadExtras()
    {
        // required header stuff - used AutoCad 2007 version
        acadHeader = new DXFHeader("AC1021");
        header.add(acadHeader);
        
        
        // Tables
        // required tables - sheesh! Many can be empty, but still have to be there. Crappy software!
        DXFTable viewportTable = new DXFTable("VPORT");
        tables.add(viewportTable);
        DXFTable linetypeTable = new DXFTable("LTYPE");
        tables.add(linetypeTable);
        DXFTable layerTable = new DXFTable("LAYER");
        tables.add(layerTable);
        DXFTable styleTable = new DXFTable("STYLE");
        tables.add(styleTable);
        DXFTable viewTable = new DXFTable("VIEW");
        tables.add(viewTable);
        DXFTable ucsTable = new DXFTable("UCS");
        tables.add(ucsTable);
        DXFTable appIDTable = new DXFTable("APPID");
        tables.add(appIDTable);
        DXFTable dimStyleTable = new DXFDimStyleTable("DIMSTYLE");
        tables.add(dimStyleTable);
        DXFTable blockRecordTable = new DXFTable("BLOCK_RECORD");
        tables.add(blockRecordTable);
        
        
        // viewport table requires *ACTIVE viewport entry; height can be anything?
        DXFViewport viewport = new DXFViewport("*ACTIVE", 1000);
        viewportTable.add(viewport);
        
        
        // linetype table requires two entries with names ByBlock and ByLayer
        DXFLinetype linetype = new DXFLinetype("ByBlock");
        linetypeTable.add(linetype);
        
        linetype = new DXFLinetype("ByLayer");
        linetypeTable.add(linetype);
        
        
        // layer table requires one layer; name could probably be anything, but we'll use "0"
        DXFLayer layer = new DXFLayer("0");
        layerTable.add(layer);
        
        // style, view, dimstyle and UCS tables can be empty
        
        // appid needs single entry for AutoCAD
        DXFAppID appID = new DXFAppID("ACAD");
        appIDTable.add(appID);
        
        // block record table needs two entries, *Model_Space and *Paper_Space
        DXFBlockRecord blockRecord = new DXFBlockRecord("*Model_Space");
        blockRecordTable.add(blockRecord);
        blockRecord = new DXFBlockRecord("*Paper_Space");
        blockRecordTable.add(blockRecord);
        
        
        // Blocks
        // blocks section needs two blocks, *Model_Space and *Paper_Space, with corresponding end-blocks
        DXFBlock block = new DXFBlock("*Model_Space");
        blocks.add(block);
        DXFBlockEnd endblock = new DXFBlockEnd(block);
        blocks.add(endblock);
        
        block = new DXFBlock("*Paper_Space");
        blocks.add(block);
        endblock = new DXFBlockEnd(block);
        blocks.add(endblock);
        
        
        // Objects
        // Objects section needs a single base dictionary with one empty entry dictionary, ACAD_GROUP - go figure...
        // Add base dictionary with no name or owner
        DXFDictionary dictionary = new DXFDictionary("", 0);
        objects.add(dictionary);
        
        // add the single child dictionary to the root dictionary
        DXFDictionary childDictionary = new DXFDictionary("ACAD_GROUP", dictionary.getHandle());
        dictionary.add(childDictionary);
        
    }


    /**
     * Get the DXFGraphics associated with this document for use with standard Graphics drawing operations to generate
     * a DXF text representation.
     * 
     * @return	The DXFGraphics associated with this document, on which standard Java Graphics drawing calls can be made
     */
    public DXFGraphics getGraphics()
    {
        return this.graphics;
    }
    
    
    /**
     * Return the DXF text associated with this DXF document. This includes the header,
     * classes, tables, blocks, entities and objects sections, populated with content
     * generated by graphics calls on the associated DXFGraphics.
     * 
     * @return The DXF text associated with this document.
     */
    public String toDXFString()
    {
        String returnString = new String();
        
        returnString += "999\n" + documentComment + "\n";
        
        returnString += header.toDXFString();
        returnString += classes.toDXFString();
        returnString += tables.toDXFString();
        returnString += blocks.toDXFString();
        returnString += entities.toDXFString();
        returnString += objects.toDXFString();
        
        // end-of-file marker
        returnString += "0\nEOF\n";
        
        return returnString;
    }
    
    
    /**
     * Utility method used by the associated DXFGraphics object.
     * 
     * @param table A DXFTable instance
     */
    public void addTable(DXFTable table)
    {
        tables.add(table);
    }
    
    
    /**
     * Utility method used by the associated DXFGraphics object.
     * 
     * @param entity A DXFEntity instance
     */
    public void addEntity(DXFEntity entity)
    {
        entities.add(entity);
    }
    
    
    /**
     * Utility method used by the associated DXFGraphics object.
     * 
     * @param style A DXFStyle instance
     * @return  the associated DXFStyle in the style table
     */
    public DXFStyle addStyle(DXFStyle style)
    {
        // first see if style already represented in tables; if so, return it
        for(int i = 0; i < tables.size(); i++)
        {
            DXFTable table = (DXFTable)tables.elementAt(i);
            if (table.name.equals("STYLE"))
            {
                // see if we have the style in the table
                int index = table.indexOf(style);
                if (index > 0)
                {
                    return (DXFStyle)table.elementAt(index);
                }
            }
        }
        
        
        // didn't find it; add to one of the STYLE tables, adding one if necessary, and return the style passed in
        DXFTable styleTable = null;
        for(int i = 0; i < tables.size(); i++)
        {
            DXFTable table = (DXFTable)tables.elementAt(i);
            if (table.name.equals("STYLE"))
            {
                styleTable = table;
                break;
            }
        }
        
        if (styleTable == null)
        {
            styleTable = new DXFTable("STYLE");
            tables.add(styleTable);
        }
        
        styleTable.add(style);
        
        return style;
    }
    
}