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
 * Class representing a general B-spline
 * 
 * @author jsevy
 *
 */
public class DXFSpline extends DXFEntity
{
    private int degree;
    private Vector<SplineControlPoint> expandedControlPoints;
    private boolean closed;
    private Color color;
    private BasicStroke stroke;
    
    
    /**
     * Create a spline of specified degree for the specified control points.
     * 
     * @param degree		Degree of the piecewise-polynomial spline segments
     * @param controlPoints	Locations and weights of the control points for the spline
     * @param throughEndpoints		If true, the spline will be forced to pass through the endpoints by setting the end
     * 						control point multiplicities to degree + 1
     * @param graphics      The graphics object specifying parameters for this entity (color, thickness)
     */
    public DXFSpline(int degree, Vector<SplineControlPoint> controlPoints, boolean throughEndpoints, Graphics2D graphics)
    {
        
        // if pass through endpoints, set multiplicities of first and last control points to degree + 1
        if (throughEndpoints)
        {
        	controlPoints.elementAt(0).multiplicity = degree + 1;
        	controlPoints.elementAt(controlPoints.size()-1).multiplicity = degree + 1;
        }
    	
    	this.degree = degree;
        this.expandedControlPoints = createExpandedPointVector(controlPoints);
        this.closed = false;
        this.color = graphics.getColor();
        this.stroke = (BasicStroke)graphics.getStroke();
    }
    
    
    
    /**
     * Implementation of DXFObject interface method; creates DXF text representing the spline.
     */
    public String toDXFString()
    {
        String result = "0\nSPLINE\n";
        
        // print out handle and superclass marker(s)
        result += super.toDXFString();
        
        // print out subclass marker
        result += "100\nAcDbSpline\n";
        
        // include degree of spline
        result += "71\n" + degree + "\n";
        
        // include number of control points
        result += "73\n" + expandedControlPoints.size() + "\n";
        
        // indicate if closed
        if (closed)
        {
            result += "70\n1\n";
        }
        
        // include expanded list of control points, with multiplicities for control points; 
        // knots are just evenly spaced integer values, augmented at end with n+1 points to make AutoCAD happy
        
        // knots first (since there are more of them than control points)
        for (int i = 0; i < expandedControlPoints.size() + degree + 1; i++)
        {
        	result += "40\n" + i + "\n";
        }
        
        // now control points and weights
        for (int i = 0; i < expandedControlPoints.size(); i++)
        {
            SplineControlPoint point = expandedControlPoints.elementAt(i);
            result += "10\n" + point.x + "\n";
            result += "20\n" + point.y + "\n";
            result += "30\n" + point.z + "\n";
            result += "41\n1\n";                // all weights 1; multiplicities already accounted for
        }
        
        // add thickness; specified in Java in pixels at 72 pixels/inch; needs to be in 1/100 of mm for DXF, and restricted range of values
        result += "370\n" + getDXFLineWeight(stroke.getLineWidth()) + "\n";
       
        // add color number
        result += "62\n" + DXFColor.getClosestDXFColor(color.getRGB()) + "\n";
               
        return result;
    }
    
    
    public String getDXFHatchInfo()
    {
        // spline
        String result = "72\n" + "4" + "\n";
        
        // degree
        result += "94\n" + degree + "\n";
        
        // not rational
        result += "73\n" + "0" + "\n";
        
        // not periodic
        result += "74\n" + "0" + "\n";
        
        // include number of knots
        result += "95\n" + (expandedControlPoints.size() + degree + 1) + "\n";
        
        // include number of control points
        result += "96\n" + expandedControlPoints.size() + "\n";
        
        // knots first (since there are more of them than control points)
        for (int i = 0; i < expandedControlPoints.size() + degree + 1; i++)
        {
            result += "40\n" + i + "\n";
        }
        
        // now control points and weights
        for (int i = 0; i < expandedControlPoints.size(); i++)
        {
            SplineControlPoint point = expandedControlPoints.elementAt(i);
            result += "10\n" + point.x + "\n";
            result += "20\n" + point.y + "\n";
            // all weights 1; multiplicities already accounted for
            //result += "42\n1\n";                
        }
               
        return result;
    }
    
    
    /**
     * Create vector of control points with points multiply represented according to their multiplicities,
     * and appropriate multiplicity at endpoints to pass through these.
     */
    private Vector<SplineControlPoint> createExpandedPointVector(Vector<SplineControlPoint> controlPoints)
    {

    	int index = 0;
        
        Vector<SplineControlPoint> expandedPoints = new Vector<SplineControlPoint>();
        
        if (controlPoints.size() != 0)
        {
            
	        for (int j = 0; j < controlPoints.size(); j++)
	        {
	            SplineControlPoint controlPoint = controlPoints.elementAt(j);
	            controlPoint.expandedIndex = index;
	            
	            for (int i = 0; i < controlPoint.multiplicity; i++)
	            {
	                expandedPoints.add(controlPoint);
	                index++;
	            }
	        }
        }
        
        return expandedPoints;
        
    }
}