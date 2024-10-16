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
 * Just a special case of DXFObjects used for graphical entities.
 * 
 * @author jsevy
 *
 */
public class DXFEntity extends DXFDatabaseObject
{
        public DXFEntity()
        {
            super();
        }
        
        public String toDXFString()
        {
            // print out handle and superclass marker(s)
            String result = super.toDXFString();
            
            // print out base subclass marker for entities
            result += "100\nAcDbEntity\n";
            
            return result;
        }
        
        /* Should be overridden by classes that can be used as Hatch boundaries */
        public String getDXFHatchInfo()
        {
            return "";
        }
        
        public int getDXFLineWeight(float javaLineWidth)
        {
            // Java line width specified in pixels at 72 pixels/inch; DXF line width specified in 1/100 of mm, with restricted range
            // Range: 0, 5, 9, 13, 15, 18, 20, 25, 30, 35, 40, 50, 53, 60, 70, 80, 90, 100, 106, 120, 140, 158, 200, 211, -1 (by layer), -2 (by block), -3 (default)
            // so scale by (1 in/72 pixels) * (25.4 mm / 1 in) * 100, and map to nearest available weight
            double scale = 25.4 * 100 / 72;
            double lineWidthMMHundredths = javaLineWidth * scale;
            int[] lineWidthOptions = {0, 5, 9, 13, 15, 18, 20, 25, 30, 35, 40, 50, 53, 60, 70, 80, 90, 100, 106, 120, 140, 158, 200, 211};
            
            int i;
            for (i = 1; i < lineWidthOptions.length; i++)
            {
                if (lineWidthOptions[i] - lineWidthMMHundredths > 0)
                    break;
            }
            lineWidthMMHundredths = lineWidthOptions[i-1];
            
            return (int)lineWidthMMHundredths;
        }
}