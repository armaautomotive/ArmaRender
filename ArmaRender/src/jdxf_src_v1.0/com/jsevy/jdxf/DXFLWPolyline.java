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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Vector;



/**
 * Class representing a set of line segments defining a (possibly closed) polygon.
 * 
 * @author jsevy
 *
 */
public class DXFLWPolyline extends DXFEntity
{
    private int numVertices;
    private Vector<RealPoint> vertices;
    private boolean closed;
    private Color color;
    private BasicStroke stroke;
    
    
    
    /**
     * Create a set of line segments that connects the specified points, including a segment from the last 
     * to the first if closed is indicated.
     * 
     * @param numVertices 	The number of vertices specified in the vertex list
     * @param vertices		The vertices
     * @param closed		If true, adds a segment between the last and first points
     * @param graphics      The graphics object specifying parameters for this entity (color, thickness)
     */
    public DXFLWPolyline(int numVertices, Vector<RealPoint> vertices, boolean closed, Graphics2D graphics)
    {
        this.numVertices = numVertices;
        this.vertices = vertices;
        this.closed = closed;
        this.color = graphics.getColor();
        this.stroke = (BasicStroke)graphics.getStroke();
    }
    
    
    /**
     * Implementation of DXFObject interface method; creates DXF text representing the polyline.
     */
    public String toDXFString()
    {
        String result = "0\nLWPOLYLINE\n";
        
        // print out handle and superclass marker(s)
        result += super.toDXFString();
        
        // print out subclass marker
        result += "100\nAcDbPolyline\n";
        
        // include number of vertices
        result += "90\n" + numVertices + "\n";
        
        // indicate if closed
        if (closed)
        {
            result += "70\n1\n";
        }
        
        // include list of vertices
        for (int i = 0; i < vertices.size(); i++)
        {
            RealPoint point = vertices.elementAt(i);
            result += "10\n" + point.x + "\n";
            result += "20\n" + point.y + "\n";
            result += "30\n" + point.z + "\n";
        }
        
        // add thickness; specified in Java in pixels at 72 pixels/inch; needs to be in 1/100 of mm for DXF, and restricted range of values
        result += "370\n" + getDXFLineWeight(stroke.getLineWidth()) + "\n";
       
        // add color number
        result += "62\n" + DXFColor.getClosestDXFColor(color.getRGB()) + "\n";
               
        return result;
    }
}