/* Copyright (C) 1999-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.image.filter.*;
import armarender.material.*;
import armarender.object.*;
import armarender.procedural.*;
import armarender.texture.*;
import armarender.ui.*;
import buoy.widget.*;

import java.io.*;
import java.util.List;

/**
 * @deprecated This class is deprecated.  Use {@link ArmaRender} and {@link PluginRegistry} instead.
 */

public class ModellingApp
{
  public static final String MAJOR_VERSION = ArmaRender.getMajorVersion();
  public static final String VERSION = ArmaRender.getVersion();
  public static final double DIST_TO_SCREEN = Camera.DEFAULT_DISTANCE_TO_SCREEN;
  public static String currentDirectory;

  public static void main(String args[])
  {
    System.out.println("ModellingApp is deprecated.  Use armarender.ArmaRender.main() instead.");
    ArmaRender.main(args);
  }

  /** Get the complete version number of Art of Illusion. */

  public static String getVersion()
  {
    return ArmaRender.getVersion();
  }

  /** Get the major part of the version number of Art of Illusion. */

  public static String getMajorVersion()
  {
    return ArmaRender.getMajorVersion();
  }

  /** Get the application preferences object. */
  
  public static ApplicationPreferences getPreferences()
  {
    return ArmaRender.getPreferences();
  }

  /** Get a list of all available Renderers. */

  public static Renderer[] getRenderers()
  {
    return (Renderer[]) PluginRegistry.getPlugins(Renderer.class).toArray(new Renderer[0]);
  }
  
  /** Get a list of all installed Plugins. */

  public static Plugin[] getPlugins()
  {
    return (Plugin[]) PluginRegistry.getPlugins(Plugin.class).toArray(new Plugin[0]);
  }

  /** Get a list of all available Translators. */

  public static Translator[] getTranslators()
  {
    return (Translator[]) PluginRegistry.getPlugins(Translator.class).toArray(new Translator[0]);
  }
  
  /** Get a list of all available ModellingTools. */
  
  public static ModellingTool[] getModellingTools()
  {
    return (ModellingTool[]) PluginRegistry.getPlugins(ModellingTool.class).toArray(new ModellingTool[0]);
  }
  
  /** Get a list of all available Texture classes. */

  public static Class[] getTextureTypes()
  {
    List instances = PluginRegistry.getPlugins(Texture.class);
    Class classes[] = new Class[instances.size()];
    for (int i = 0; i < classes.length; i++)
      classes[i] = instances.get(i).getClass();
    return classes;
  }
  
  /** Get a list of all available Material classes. */

  public static Class[] getMaterialTypes()
  {
    List instances = PluginRegistry.getPlugins(Material.class);
    Class classes[] = new Class[instances.size()];
    for (int i = 0; i < classes.length; i++)
      classes[i] = instances.get(i).getClass();
    return classes;
  }
  
  /** Get a list of all available TextureMapping classes. */

  public static Class[] getTextureMappings()
  {
    List instances = PluginRegistry.getPlugins(TextureMapping.class);
    Class classes[] = new Class[instances.size()];
    for (int i = 0; i < classes.length; i++)
      classes[i] = instances.get(i).getClass();
    return classes;
  }
  
  /** Get a list of all available MaterialMapping classes. */

  public static Class[] getMaterialMappings()
  {
    List instances = PluginRegistry.getPlugins(MaterialMapping.class);
    Class classes[] = new Class[instances.size()];
    for (int i = 0; i < classes.length; i++)
      classes[i] = instances.get(i).getClass();
    return classes;
  }
  
  /** Get a list of all available ImageFilter classes. */

  public static Class[] getImageFilters()
  {
    List instances = PluginRegistry.getPlugins(ImageFilter.class);
    Class classes[] = new Class[instances.size()];
    for (int i = 0; i < classes.length; i++)
      classes[i] = instances.get(i).getClass();
    return classes;
  }

  /** Get a list of all plugin-defined procedural Module classes. */

  public static Class[] getModules()
  {
    List instances = PluginRegistry.getPlugins(armarender.procedural.Module.class);
    Class classes[] = new Class[instances.size()];
    for (int i = 0; i < classes.length; i++)
      classes[i] = instances.get(i).getClass();
    return classes;
  }
  
  /** Add a new Renderer the list of available ones. */
  
  public static void registerRenderer(Renderer o)
  {
    PluginRegistry.registerPlugin(o);
  }
  
  /** Add a new Translator the list of available ones. */
  
  public static void registerTranslator(Translator o)
  {
    PluginRegistry.registerPlugin(o);
  }
  
  /** Add a new ModellingTool the list of available ones. */
  
  public static void registerModellingTool(ModellingTool o)
  {
    PluginRegistry.registerPlugin(o);
  }
  
  /** Add a new Texture the list of available ones. */
  
  public static void registerTexture(Texture o)
  {
    PluginRegistry.registerPlugin(o);
  }
  
  /** Add a new Material the list of available ones. */
  
  public static void registerMaterial(Material o)
  {
    PluginRegistry.registerPlugin(o);
  }
  
  /** Add a new TextureMapping the list of available ones. */
  
  public static void registerTextureMapping(TextureMapping o)
  {
    PluginRegistry.registerPlugin(o);
  }
  
  /** Add a new MaterialMapping the list of available ones. */
  
  public static void registerMaterialMapping(MaterialMapping o)
  {
    PluginRegistry.registerPlugin(o);
  }
  
  /** Add a new Plugin the list of available ones. */
  
  public static void registerPlugin(Plugin o)
  {
    PluginRegistry.registerPlugin(o);
  }
  
  /** Add a new ImageFilter to the list of available ones. */
  
  public static void registerImageFilter(ImageFilter o)
  {
    PluginRegistry.registerPlugin(o);
  }
  
  /** Add a new Module to the list of available ones. */
  
  public static void registerModule( armarender.procedural.Module o)
  {
    PluginRegistry.registerPlugin(o);
  }
  
  /** Create a new Scene, and display it in a window. */
  
  public static void newWindow()
  {
    ArmaRender.newWindow();
  }
  
  /** Create a new window for editing the specified scene. */
  
  public static void newWindow(Scene theScene)
  {
    ArmaRender.newWindow(theScene);
  }
  
  /** Add a window to the list of open windows. */
  
  public static void addWindow(EditingWindow win)
  {
    ArmaRender.addWindow(win);
  }
  
  /** Close a window. */
  
  public static void closeWindow(EditingWindow win)
  {
    ArmaRender.closeWindow(win);
  }
  
  /** Get a list of all open windows. */
  
  public static EditingWindow[] getWindows()
  {
    return ArmaRender.getWindows();
  }
  
  /** Quit Art of Illusion. */
  
  public static void quit()
  {
    ArmaRender.quit();
  }
  
  /** Get a class specified by name.  This checks both the system classes, and all plugins.
      It also accounts for classes which changed packages in version 1.3. */
  
  public static Class getClass(String name) throws ClassNotFoundException
  {
    return ArmaRender.getClass(name);
  }
  
  /** This is a utility routine which loads a file from disk. */
  
  public static String loadFile(File f) throws IOException
  {
    return ArmaRender.loadFile(f);
  }
  
  /** Save a scene to a file.  This method returns true if the scene is successfully saved,
      false if an error occurs. */

  public static boolean saveScene(Scene sc, LayoutWindow fr)
  {
    return ArmaRender.saveScene(sc, fr);
  }
  
  /** Prompt the user to select a scene file, then open a new window containing it.  The BFrame is used for
      displaying dialogs. */
  
  public static void openScene(BFrame fr)
  {
    ArmaRender.openScene(fr);
  }
    
  /** Load a scene from a file, and open a new window containing it.  The BFrame is used for
      displaying dialogs. */
  
  public static void openScene(File f, BFrame fr)
  {
    ArmaRender.openScene(f, fr);
  }
  
  /** Copy a list of objects to the clipboard, so they can be pasted into either the same scene or a
      different one. */
  
  public static void copyToClipboard(ObjectInfo obj[], Scene scene)
  {
    ArmaRender.copyToClipboard(obj, scene);
  }
  
  /** Paste the contents of the clipboard into a window. */
  
  public static void pasteClipboard(LayoutWindow win)
  {
    ArmaRender.pasteClipboard(win);
  }
  
  /** Get the number of objects on the clipboard. */
  
  public static int getClipboardSize()
  {
    return ArmaRender.getClipboardSize();
  }
}
