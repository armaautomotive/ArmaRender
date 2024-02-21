/*
 * Based on OBJTranslator.java, which is Copyright (C) 2002-2004 by Peter Eastman


   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armadesignstudio.translators;

import armadesignstudio.*;
import buoy.widget.*;

/** ColladaTranslator is a Translator which exports Collada 1.4 .dae files. */

public class ColladaTranslator implements Translator
{
  public String getName()
  {
    return "Collada (.dae)";
  }

  public boolean canImport()
  {
    return false;
  }
  
  public boolean canExport()
  {
    return true;
  }
  
  public void importFile(BFrame parent, Scene scene)
  {
  }
  
  public void exportFile(BFrame parent, Scene theScene)
  {
    ColladaExporter.exportFile(parent, theScene);
  }
}
