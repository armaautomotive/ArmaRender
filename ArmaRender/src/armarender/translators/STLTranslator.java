/*
   Copyright (C) 2022 by Jon Taylor
   Copyright (C) 2002-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender.translators;

import armarender.*;
import buoy.widget.*;

/** OBJTranslator is a Translator which imports and exports OBJ files. */

public class STLTranslator implements Translator
{
  public String getName()
  {
    return "Stereolithography (.stl)";
  }

  public boolean canImport()
  {
    return true;
  }
  
  public boolean canExport()
  {
    return true;
  }
  
  public void importFile(BFrame parent, Scene theScene)
  {
    STLImporter.importFile(parent, theScene);
  }
  
  public void exportFile(BFrame parent, Scene theScene)
  {
      STLExporter.exportFile(parent, theScene);
  }
}
