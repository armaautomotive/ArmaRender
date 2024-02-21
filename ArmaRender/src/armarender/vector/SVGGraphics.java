/*  SVGGraphics.java  */

package armarender.vector;

/*
 * SVGGraphics: a Graphics object that generates SVG output.
 *
 * Author: Nik Trevallyn-Jones, nik777@users.sourceforge.net
 * $Id: Exp $
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * with this program. If not, the license is available from the
 * GNU project, at http://www.gnu.org.
 */

/**
 *  A Graphics object that outputs SVG in response to drawing commands
 */

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.text.AttributedCharacterIterator;

public class SVGGraphics extends Graphics
{
    protected static final String HEADER =
	"<?xml version=\"1.0\" standalone=\"yes\"?>" +
	"\n<!DOCTYPE svg PUBLIC \"-//W3/DTD SVG 1.1//EN\"" +
	" \"http:////www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\" >";

    protected static final String LINE = "fill=\"none\" stroke=";
    protected static final String FILL = "stroke=\"none\" fill=";

    // *must* be a unique object, so we avoid string-pooling
    protected static final String CLEAR = new String(LINE);

    protected PrintWriter out;
    protected Color colour = Color.black;
    protected FontMetrics fontMetrics = null;
    protected Shape clip = null;

    protected String indent = "\n";
    
    protected int tx = 0;
    protected int ty = 0;
    protected int rx = 0;
    protected int ry = 0;
    protected int sx = 0;
    protected int sy = 0;
    protected int cx = 0;
    protected int cy = 0;
    protected int cw = 0;
    protected int ch = 0;

    protected boolean group = false;

    public SVGGraphics()
    {}

    public void dispose()
    { close(); }

    /**
     *  open this SVGGraphics object
     */
    public void open(OutputStream os, String title, int width, int height,
		     boolean standalone)
    {
	try {
	    out = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), false);
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}

	if (standalone) out.print(HEADER);

	out.print(indent);
	out.print("<svg version=\"1.1\"");
	if (standalone) out.print(" xmlns=\"http://www.w3.org/2000/svg\"");
	if (width > 0) {
	    out.print(" width=\"");
	    out.print(width);
	    out.print("\"");
	}
	if (height > 0) {
	    out.print(" height=\"");
	    out.print(height);
	    out.print("\"");
	}
	out.print(" >");
	tabRight();

	if (title != null) writeNotes(null, title);
    }

    /**
     *  close the output
     */
    public void close()
    {
	tabLeft();
	out.print(indent);
	out.print("</svg>\n");

	if (out.checkError())
	    System.out.println("SVGGraphics: error(s) writing output");

	out = null;
    }

    public void setPaintMode()
    {}

    public void translate(int x, int y)
    {
	tx += x;
	ty += y;
	writeGroup();
    }

    public void rotate(int x, int y)
    {
	rx += x;
	ry += y;
	writeGroup();
    }

    public void scale(int x, int y)
    {
	sx += x;
	sy += y;
	writeGroup();
    }

    public void clearRect(int x1, int y1, int x2, int y2)
    { writeRect(x1, y1, x2, y2, CLEAR); }

    public void clipRect(int x, int y, int width, int height)
    {
	cx = Math.max(cx, x);
	cy = Math.min(cy, y);
	cw = Math.min(cw, width);
	ch = Math.min(ch, height);
	writeGroup();
    }

    public void drawLine(int x1,int y1,int x2, int y2)
    {
	out.print(indent);
	out.print("<line");
	out.print(" x1=\"");
	out.print(x1);
	out.print("\" y1=\"");
	out.print(y1);
	out.print("\" x2=\"");
	out.print(x2);
	out.print("\" y2=\"");
	out.print(y2);
	out.print("\" stroke=");
	writeColour(colour);
	out.print("/>");
    }

    public void drawOval(int x, int y, int width, int height)
    {}

    public void drawRect(int x, int y, int width, int height)
    { writeRect(x, y, width, height, LINE); }

    public void fillOval(int x, int y, int width, int height)
    {}

    public void fillRect(int x, int y, int width, int height)
    { writeRect(x, y, width, height, FILL); }

    public void setClip(int x, int y, int width, int height)
    {
	cx = x;
	cy = y;
	cw = width;
	ch = height;
	writeGroup();
    }

    public boolean hitClip(int x, int y, int width, int height)
    {
	return (cx <= x && cy >= y && cx + cw >= x + width &&
		cy - ch <= y - height);
    }

    public void copyArea(int x, int y, int width, int height, int dx, int dy)
    {}

    public void drawArc(int x, int y, int width, int height, int startAngle,
			int arcAngle)
    {}

    public void drawRoundRect(int x, int y, int width, int height,
			      int arcWidth, int arcHeight)
    {}

    public void fillArc(int x, int y, int width, int height, int startAngle,
			int arcAngle)
    {}

    public void fillRoundRect(int x, int y, int width, int height,
			      int arcWidth, int arcHeight)
    {}

    public void draw3DRect(int x, int y, int width, int height, boolean raised)
    { drawRect(x, y, width, height); }

    public void fill3DRect(int x, int y, int width, int height, boolean raised)
    { fillRect(x, y, width, height); }

    public void drawBytes(byte[] bytes, int start, int len, int x, int y)
    {}

    public void drawChars(char[] chars, int start, int len, int x, int y)
    {}

    public void drawPolygon(int[] xpoints, int[] ypoints, int count)
    { writePolygon(xpoints, ypoints, count, LINE); }

    public void drawPolyline(int[] xpoints, int[] ypoints, int count)
    {}

    public void fillPolygon(int[] xpoints, int[] ypoints, int count)
    { writePolygon(xpoints, ypoints, count, null); }

    public Color getColor()
    { return colour; }

    public void setColor(Color colour)
    { this.colour = colour; }

    public void setXORMode(Color colour)
    { this.colour = colour; }

    public Font getFont()
    { return (fontMetrics != null ? fontMetrics.getFont() : null); }

    public void setFont(Font font)
    { /* fontMetrics = new FontMerics(font); */ }

    public FontMetrics getFontMetrics()
    { return fontMetrics; }

    public Graphics create()
    {
	throw new RuntimeException("Not yet implemented");

	/*
	Graphics result = new SVGGraphics();

	result.tx = tx;
	result.ty = ty;
	result.rx = rx;
	result.ry = ry;
	result.sx = sx;
	result.sy = sy;
	result.cx = cx;
	result.cy = cy;
	result.cw = cw;
	result.ch = ch;

	result.colour = colour;
	result.fontMetrics = fontMetrics;

	result.colourFront = colourFront;
	result.colourBack = colourBack;

	result.out = out;
	*/
    }

    public Graphics create(int x, int y, int width, int height)
    {
	throw new RuntimeException("Not yet implemented");

	/*
	Graphics result = create();
	result.tx += x;
	result.ty += y;
	*/
    }

    public void drawPolygon(Polygon p)
    {}

    public void fillPolygon(Polygon p)
    {}

    public Rectangle getClipBounds()
    {
	if (clip != null)
	    return clip.getBounds();
	else {
	    clip = new Rectangle(cx, cy, cw, ch);
	    return (Rectangle) clip;
	}
    }

    public Rectangle getClipRect()
    { return getClipBounds(); }

    public Shape getClip()
    { return clip; }

    public void setClip(Shape clip)
    {
	this.clip = clip;
	Rectangle bounds = clip.getBounds();

	cx = bounds.x;
	cy = bounds.y;
	cw = bounds.width;
	ch = bounds.height;
    }

    public String toString()
    { return "SVGGraphics"; }

    public void drawString(String text, int x, int y)
    {
	out.print(indent);
	out.print("<text ");
	out.print(" x=\"");
	out.print(x);
	out.print("\" y=\"");
	out.print(y);
	out.write("\" stroke=");
	writeColour(colour);
	out.print(">");
	out.print(tabRight(indent));
	out.print(text);
	out.print(indent);
	out.print("</text>");
    }

    public void drawString(AttributedCharacterIterator iter, int x, int y)
    { throw new RuntimeException("Not yet implemented"); }

    public FontMetrics getFontMetrics(Font font)
    { throw new RuntimeException("Not yet implemented"); }

    public Rectangle getClipBounds(Rectangle bounds)
    { throw new RuntimeException("Not yet implemented"); }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
			     int sx1, int sy1, int sx2, int sy2,
			     ImageObserver obs)
    { throw new RuntimeException("Not yet implemented"); }

    public boolean drawImage(Image img, int x, int y, int width, int height,
			     ImageObserver obs)
    { throw new RuntimeException("Not yet implemented"); }

    public boolean drawImage(Image img, int x, int y, ImageObserver obs)
    { throw new RuntimeException("Not yet implemented"); }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
			     int sx1, int sy1, int sx2, int sy2, Color bgcolor,
			     ImageObserver obs)
    { throw new RuntimeException("Not yet implemented"); }

    public boolean drawImage(Image img, int x, int y, int width, int height,
			     Color bgcolor, ImageObserver obs)
    { throw new RuntimeException("Not yet implemented"); }

    public boolean drawImage(Image img, int x, int y, Color bgcolor,
			     ImageObserver obs)
    { throw new RuntimeException("Not yet implemented"); }

    /**
     *  write notes elements
     */
    public void writeNotes(String desc, String title)
    {
	if (desc != null) {
	    out.print(indent);
	    out.print("<desc>");
	    out.print(tabRight(indent));
	    out.print(desc);
	    out.print(indent);
	    out.print("</desc>");
	}

	if (title != null) {
	    out.print(indent);
	    out.print("<title>\n\t");
	    out.print(tabRight(indent));
	    out.print(title);
	    out.print(indent);
	    out.print("</title>");
	}
    }

    /**
     *  write a rectangle
     */
    protected void writeRect(int x, int y, int width, int height, String opts)
    {
	out.print(indent);
	out.print("<rect");
	out.print(" x=\"");
	out.print(x);
	out.print("\" y=\"");
	out.print(y);
	out.print("\" width=\"");
	out.print(width);
	out.print("\" height=\"");
	out.print(height);
	out.print("\" ");
	if (opts != null) {
	    out.print(opts);
	    if (opts == LINE || opts == FILL) writeColour(colour);
	    else if (opts == CLEAR) out.print("\"black\"");
	}
	out.print(" />");
    }

    /**
     *  write a polygon
     */
    protected void writePolygon(int[] x, int[] y, int count, String opts)
    {
	int i=0;
	out.print(indent);
	out.print("<polygon");

	out.print(" points=\"");
	for (i = 0; i < count; i++) {
	    out.print(x[i]);
	    out.print(",");
	    out.print(y[i]);
	    if (i < count-1) out.print(" ");
	}
	out.print("\" ");

	if (opts != null) {
	    out.print(opts);
	    if (opts == LINE || opts == FILL) writeColour(colour);
	    else if (opts == CLEAR) out.print("\"black\"");
	}
	else {
	    out.write("stroke=");
	    writeColour(colour);
	    out.write(" fill=");
	    writeColour(colour);
	}
	out.print(" />");
    }

    /**
     *  change/set the group element to reflect the current settings
     */
    protected void writeGroup()
    {
	if (group) {
	    out.print(indent);
	    out.print("</g>");
	}

	// bitwise OR...
	if ((tx | ty | rx | ry | sx | sy | cx | cy | cw | ch) == 0) {
	    if (group) {
		tabLeft();
		group = false;
	    }
	    return;
	}

	if ((tx | ty | rx | ry | sx | sy) != 0) {
	    out.print(indent);
	    out.print("<g transform=\"");
	    if (tx != 0 || ty != 0) {
		out.print("translate(");
		out.print(tx);
		out.print(",");
		out.print(ty);
		out.print("),");
	    }
	    if (rx != 0 || ry != 0) {
		out.print("rotate(");
		out.print(rx);
		out.print(",");
		out.print(ry);
		out.print("),");
	    }
	    if (sx != 0 || sy != 0) {
		out.print("scale(");
		out.print(sx);
		out.print(",");
		out.print(sy);
		out.print(")");
	    }
	    out.print("\"");
	}
	    
	if ((cx | cy | cw | ch) != 0) {
	    out.print(" clip=\"");
	    out.print(cx);
	    out.print(",");
	    out.print(cy);
	    out.print(",");
	    out.print(cw);
	    out.print(",");
	    out.print(ch);
	    out.print(",");
	    out.print("\"");
	}
	out.print(" >");

	if (!group) {
	    tabRight();
	    group = true;
	}
    }

    protected void writeColour(Color c)
    {
	out.print("\"rgb(");
	out.print(c.getRed());
	out.print(",");
	out.print(c.getGreen());
	out.print(",");
	out.print(c.getBlue());
	out.print(")\"");
    }

    protected void tabRight()
    { indent = tabRight(indent); }

    protected void tabLeft()
    { indent = tabLeft(indent); }

    protected String tabRight(String pad)
    { return tab.substring(0, Math.min(pad.length() + 3, tab.length())); }

    protected String tabLeft(String pad)
    { return tab.substring(0, Math.max(pad.length() - 3, 3)); }

    private static final String tab = "\n                             ";
}
