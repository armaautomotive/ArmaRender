/* Copyright (C) 2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.image;

import armarender.*;
import armarender.math.*;
import java.io.*;

/** This class generates .hdr image files. */

public class HDREncoder
{
  /** Write out the data for an image to a stream. */

  public static void writeImage(ComplexImage img, OutputStream out) throws IOException
  {
    int rows = img.getHeight(), cols = img.getWidth();
    RGBColor color = new RGBColor();
    PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
    boolean hasAlpha = img.hasFloatData(ComplexImage.ALPHA);

    pw.print("#?RADIANCE\n");
    pw.print("# Generated by Arma Render "+ArmaRender.getVersion()+"\n");
    pw.print("FORMAT=32-bit_rle_rgbe\n");
    pw.print("\n");
    pw.print("-Y "+rows+" +X "+cols+"\n");
    pw.flush();
    for (int i = 0; i < rows; i++)
      for (int j = 0; j < cols; j++)
        {
          float red = img.getPixelComponent(j, i, ComplexImage.RED);
          float green = img.getPixelComponent(j, i, ComplexImage.GREEN);
          float blue = img.getPixelComponent(j, i, ComplexImage.BLUE);
          if (hasAlpha)
            {
              float alpha = img.getPixelComponent(j, i, ComplexImage.ALPHA);
              red *= alpha;
              green *= alpha;
              blue *= alpha;
            }
          color.setRGB(red, green, blue);
          int ergb = color.getERGB();
          out.write((ergb>>16)&0xFF);
		  out.write((ergb>>8)&0xFF);
		  out.write(ergb&0xFF);
		  out.write((ergb>>24)&0xFF);
        }
    out.flush();
  }
}
