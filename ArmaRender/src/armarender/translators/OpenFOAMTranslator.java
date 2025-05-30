/* Copyright (C) 2024 by Jon Taylor

*/

package armarender.translators;

import armarender.*;
import buoy.widget.*;

/** OpenFOAMTranslator is a Translator which imports and exports OBJ files. */

public class OpenFOAMTranslator implements Translator
{
  public String getName()
  {
    return "OpenFoam (Project)";
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
    
  }
  
  public void exportFile(BFrame parent, Scene theScene)
  {
      OpenFOAMExporter.exportFile(parent, theScene);
  }
}
