/*  VectorRenderer.java  */

/* Copyright (C) 2001,2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details.
*/

package armarender.vector;

import armarender.*;
import armarender.image.*;
import armarender.object.*;
import buoy.widget.*;
import java.util.*;

/** This is a wrapper around the Vector renderer, so that the whole class does not need to
    be loaded at startup. */

public class VectorRenderer implements Renderer
{
  VectorRender vector;
  
  public VectorRenderer()
  {
      vector = new VectorRender();
  }

  public String getName()
  {
    return "VectorRender";
  }

  public void renderScene(Scene theScene, Camera theCamera, RenderListener rl, SceneCamera sceneCamera)
  {
    vector.renderScene(theScene, theCamera, rl, sceneCamera);
  }

  public void cancelRendering(Scene sc)
  {
    vector.cancelRendering(sc);
  }

  public Widget getConfigPanel()
  {
    return vector.getConfigPanel();
  }

  public boolean recordConfiguration()
  {
    return vector.recordConfiguration();
  }

  public void configurePreview()
  {
    vector.configurePreview();
  }
  
  public Map getConfiguration()
  {
    return vector.getConfiguration();
  }

  public void setConfiguration(String property, Object value)
  {
    vector.setConfiguration(property, value);
  }
}
