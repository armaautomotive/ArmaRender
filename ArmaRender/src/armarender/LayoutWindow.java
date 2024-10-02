/* Copyright (C) 2020-2023 by Jon Taylor
   Copyright (C) 1999-2013 by Peter Eastman
     
   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package armarender;

import armarender.animation.*;
import armarender.animation.distortion.*;
import armarender.image.*;
import armarender.math.*;
import armarender.object.*;
import armarender.script.*;
import armarender.texture.*;
import armarender.ui.*;
import armarender.keystroke.*;
import armarender.fea.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.prefs.*;
import java.util.*;
import java.util.List;
import buoyx.docking.*; // ddragable window.
import javax.swing.text.*;
import javax.swing.*;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import armarender.object.TriangleMesh.*;
import buoy.internal.WidgetContainerPanel;
import javax.swing.border.Border;

/** The LayoutWindow class represents the main window for creating and laying out scenes. */

public class LayoutWindow extends BFrame implements EditingWindow, PopupMenuManager
{
  SceneViewer theView[];
  BorderContainer viewPanel[];
  FormContainer viewsContainer;
  FormContainer centerContainer;
    
    FormContainer rightContainer; // work in progress. replace draggable frames.
    
  private DockingContainer dock[];
  BScrollPane itemTreeScroller;
  ColumnContainer itemScrollerToolsPane; // ColumnContainer itemScrollerToolsPane BSplitPane
    
  BScrollPane itemSelectionScroller;
  Score theScore;
  ToolPalette tools;
  ToolPalette viewTools;
  ToolPalette perspectiveTools;
    
    ToolPalette contextTools;
    
  BLabel helpText;
  TreeList itemTree;
  TreeList itemSelectionTree; // JDT
  Scene theScene;
  BMenuBar menubar;
  BMenu fileMenu, recentFilesMenu, editMenu, objectMenu, createMenu, toolsMenu, scriptMenu;
  BMenu animationMenu, editKeyframeMenu,sceneMenu;
  BMenu solidsMenu;
  BMenu layoutMenu; // JDT
  BMenu feaMenu; // JDT
  BMenu cfdMenu; // JDT
    BMenu helpMenu;
    BMenu examplesMenu;
  BMenu addTrackMenu, positionTrackMenu, rotationTrackMenu, distortionMenu;
  BMenu layoutModelView, layoutLayView, gCodeScaleMenu, exportGCode, exportLayoutDXF, exportObj, layoutHide, exportGCodeMesh; // JDT
  BMenuItem fileMenuItem[], editMenuItem[], objectMenuItem[], toolsMenuItem[];
  BMenuItem animationMenuItem[], sceneMenuItem[], popupMenuItem[];
  BMenuItem solidsMenuItem[];
  BMenuItem solidsResolvedViewMenuItem;
  BMenuItem layoutMenuItem[]; // JDT
  BMenuItem feaMenuItem[]; // JDT
  BMenuItem cfdMenuItem[]; // JDT
  BCheckBoxMenuItem displayItem[];
  BCheckBoxMenuItem objectDisplayItem[];
  BPopupMenu popupMenu;
  UndoStack undoStack;
  int numViewsShown, currentView;
  private boolean modified, sceneChangePending;
  private KeyEventPostProcessor keyEventHandler;
  private SceneChangedEvent sceneChangedEvent;
  private List<ModellingTool> modellingTools;
  protected Preferences preferences;
  boolean layoutModelingView = true;
  //Analytics analytics;
  PairDistanceAlign pairDistanceAlignDialog;
  JDialog infoGraphicDialog = null;
    
    boolean rotateScene = false;
    Thread rotateSceneThread = null;
    
    EditingTool defaultTool;
    MirrorMesh mirrorMesh;
    MirrorCurve mirrorCurve;
    //NotchIntersections notchIntersections;
    
    JButton mostRecentFile;
    JButton secondMostRecentFile;
    JButton materialButton;
    JButton unitsButton;
    
    ScaleSettings scaleSettings;
    
    Cloth cloth = null;
    ArcTubeSoftBody softBody = null;
    boolean displayContouredHighlight = true;
    
    //CreateFolderTool createFolderTool = null;
    
    int theme = 0; // 0 = light, 1 = dark
    BooleanSceneProcessor bsp;

  /** Create a new LayoutWindow for editing a Scene.  Usually, you will not use this constructor directly.
      Instead, call ModellingApp.newWindow(Scene s). */

  public LayoutWindow(Scene s)
  {
    super(s.getName() == null ? "Untitled" : s.getName());
    theScene = s;
      
      //theScene.setLayoutWindow(this);
      
      // Set static scene variable so mirror functions can access scene objects and layoutwindow elements.
      mirrorMesh = new MirrorMesh();
      //mirrorMesh.setScene(s);
      mirrorMesh.setLayoutWindow(this);
      mirrorCurve = new MirrorCurve();
      mirrorCurve.setLayoutWindow(this);
      //notchIntersections = new NotchIntersections(s, this);
    
      try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (ClassNotFoundException ex) {
      } catch (InstantiationException ex) {
      } catch (IllegalAccessException ex) {
      } catch (UnsupportedLookAndFeelException ex) {
      }

      //UIManager.put("MenuBar.background", Color.RED);   // not work
      //UIManager.put("Menu.background", Color.GREEN);    // Menu items with children.
      //UIManager.put("MenuItem.background", Color.MAGENTA); // works
      
    helpText = new BLabel();
      //helpText.setForeground(Color.WHITE);
      //helpText.setBackground(new Color(255, 255, 255));
      
    theScore = new Score(this);
    undoStack = new UndoStack();
    sceneChangedEvent = new SceneChangedEvent(this);
    createItemList();
      
    createItemSelectionList(); // JDT

    // Create the four SceneViewer panels.

    theView = new SceneViewer [4];
    viewPanel = new BorderContainer [4];
    RowContainer row;
    Object listen = new Object() {
      void processEvent(MousePressedEvent ev)
      {
        setCurrentView((ViewerCanvas) ev.getWidget());
      }
    };
    Object keyListener = new Object() {
      public void processEvent(KeyPressedEvent ev)
      {
        handleKeyEvent(ev);
      }
    };
      
    //
    for (int i = 0; i < 4; i++)
    {
      viewPanel[i] = new BorderContainer() {
        public Dimension getPreferredSize()
        {
          return new Dimension(0, 0);
        }
        public Dimension getMinimumSize()
        {
          return new Dimension(0, 0);
        }
      };
      viewPanel[i].add(row = new RowContainer(), BorderContainer.NORTH); // ??? Why is RowContainer add to viewPanel[i]
      viewPanel[i].add(theView[i] = new SceneViewer(i, theScene, (RowContainer)row, (EditingWindow)this), BorderContainer.CENTER); // pass in 'i'
      theView[i].setGrid(theScene.getGridSpacing(), theScene.getGridSubdivisions(), theScene.getShowGrid(), theScene.getSnapToGrid());
      theView[i].addEventLink(MousePressedEvent.class, listen);
      theView[i].addEventLink(KeyPressedEvent.class, keyListener);
      theView[i].setPopupMenuManager(this);
    }
      
    // Set the default view orientation.
    theView[1].setOrientation(2);
    theView[2].setOrientation(4);
    theView[3].setOrientation(6);
    theView[3].setPerspective(true);
      
    //theView[1].setBackground(new Color(255, 0, 0)); // no effect
    //  viewPanel[0].setBackground(new Color(255, 0, 0)); 
      
    theView[currentView].setDrawFocus(true);
    viewsContainer = new FormContainer(new double [] {1, 1}, new double [] {1, 1});
    viewsContainer.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    viewsContainer.add(viewPanel[0], 0, 0);
    viewsContainer.add(viewPanel[1], 1, 0);
    viewsContainer.add(viewPanel[2], 0, 1);
    viewsContainer.add(viewPanel[3], 1, 1);
    centerContainer = new FormContainer(new double [] {0.0, 1.0}, new double [] {0.0, 1.0, 0.0, 0.0});
    centerContainer.setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.BOTH, null, null));
    centerContainer.add(viewsContainer, 1, 0, 1, 3);
    
    centerContainer.add(helpText, 0, 3, 2, 1);    // Lower text status message
      
      // No longer want docable containers
      
    dock = new DockingContainer [4];
    dock[0] = new DockingContainer(centerContainer, BTabbedPane.LEFT);
    dock[1] = new DockingContainer(dock[0], BTabbedPane.RIGHT);
    dock[2] = new DockingContainer(dock[1], BTabbedPane.BOTTOM);
    dock[3] = new DockingContainer(dock[2], BTabbedPane.TOP);
    setContent(dock[3]);
      
    for (int i = 0; i < dock.length; i++)
    {
      dock[i].setHideSingleTab(true); // Tab labels not needed.
      dock[i].addEventLink(DockingEvent.class, this, "dockableWidgetMoved");
      BSplitPane split = dock[i].getSplitPane();
        
        //((JSplitPane) split.getComponent()).setOpaque(true); // JDT
        //((JSplitPane) split.getComponent()).setBackground(new Color(255, 100, 0)); // Vertical
        
      split.setContinuousLayout(true);
      split.setOneTouchExpandable(true);
      BTabbedPane.TabPosition pos = dock[i].getTabPosition();
      split.setResizeWeight(pos == BTabbedPane.TOP || pos == BTabbedPane.LEFT ? 1.0 : 0.0);
      split.addEventLink(ValueChangedEvent.class, this, "updateMenus");
      split.addEventLink(ValueChangedEvent.class, this, "updateMenus");
    }
    ObjectPropertiesPanel propertiesPanel = new ObjectPropertiesPanel(this);
    BScrollPane propertiesScroller = new BScrollPane(propertiesPanel, BScrollPane.SCROLLBAR_NEVER, BScrollPane.SCROLLBAR_AS_NEEDED);
    propertiesScroller.getVerticalScrollBar().setUnitIncrement(10);
    propertiesScroller.setBackground(ThemeManager.getAppBackgroundColor());
    
      getDockingContainer(BTabbedPane.RIGHT).addDockableWidget(new DefaultDockableWidget(itemTreeScroller, Translate.text("Objects")), 0, 0);
      //getDockingContainer(BTabbedPane.RIGHT).addDockableWidget(new DefaultDockableWidget(itemScrollerToolsPane, Translate.text("Objects")), 0, 0);
      
      DefaultDockableWidget sectionDDW = new DefaultDockableWidget(itemSelectionScroller, Translate.text("Selection"));
      getDockingContainer(BTabbedPane.RIGHT).addDockableWidget(sectionDDW, 0, 1); // Object Picker List
    
      getDockingContainer(BTabbedPane.RIGHT).addDockableWidget(new DefaultDockableWidget(propertiesScroller, Translate.text("Properties")), 0, 2);
      
      // temp
      //for (int i = 1; i == 1 && i < dock.length; i++)
      //{
      //DockingContainer dc = getDockingContainer(BTabbedPane.RIGHT);
      //Collection<Widget> children =
      
          //BSplitPane split = dock[1].getSplitPane();
          //((JSplitPane) split.getComponent()).setOpaque(true); // JDT
          //((JSplitPane) split.getComponent()).setBackground(new Color(255, 100, 0)); // border around all right side region. (3 components)
      //}
      //sectionDDW.
      
      
    // Disable animation tools
    //getDockingContainer(BTabbedPane.BOTTOM).addDockableWidget(new DefaultDockableWidget(theScore, Translate.text("Score")));

      // ContextTools
      //contextTools = new ToolPalette(1, 7); // 2, 7   3, 7
      //contextTools.setBackground(new Color(220, 220, 220)); // only corners
      //contextTools.getComponent().setPreferredSize(new Dimension(44, 44));
      
    // Build the tool palette.
    tools = new ToolPalette(2, 7); // 2, 7   3, 7
    tools.setBackground(new Color(220, 220, 220)); // only corners
      // Set border to 0
      //tools.setBor
      
    EditingTool metaTool, altTool, compoundTool;
      
      
    tools.addTool(defaultTool = new MoveObjectTool(this));              // move, select by smallest
    tools.addTool( new MoveClosestObjectTool(this));
      
    tools.addTool(metaTool = new MoveViewTool(this));
    tools.addTool(altTool = new RotateViewTool(this));
      
    tools.addTool(new RotateObjectTool(this));
    tools.addTool(new ScaleObjectTool(this));
      
    tools.addTool(compoundTool = new MoveScaleRotateObjectTool(this)); // TBD
    
    tools.addTool(new CreateCubeTool(this));
    tools.addTool(new CreateSphereTool(this));
    tools.addTool(new CreateCylinderTool(this));
    tools.addTool(new CreateSplineMeshTool(this));
    tools.addTool(new CreatePolygonTool(this));
    
    tools.addTool(new CreateCurveTool(this));
    tools.addTool(new CreateSupportCurveTool(this));
    tools.addTool(new CreateArcTool(this));
    tools.addTool(new CreateMeshVoidCurveTool(this));
      
    tools.addTool(new CreateCameraTool(this));
    tools.addTool(new CreateLightTool(this));
    
    // Dimension Tool
    // Grid Tool
    // Snap To Tool
    tools.addTool(new CreateDimensionLinearTool(this));
    tools.addTool(new CreateDimensionTool(this));
    //tools.addTool(new CreateSnapToTool(this));
    tools.addTool(new CreateLabelTool(this));
    
    //tools.addTool(new CreateForceTool(this)); // Work in progress
    //tools.addTool(new CreateFixedTool(this)); // Work in progress
     
    //tools.addTool(new CreateVoidTool(this));
      
    //tools.addTool(new CreateAlignLeftTool(this));
    //tools.addTool(new CreateAlignRightTool(this));
    //tools.addTool(new CreateAlignUpTool(this));
    //tools.addTool(new CreateAlignDownTool(this));
      
    tools.addTool(new CreateMirrorPlaneTool(this)); // Mirror plane replicates arcObjects and mesh objects.
    //tools.addTool(new CreateBlankTool(this));         // Blank, evens out odd number of tools.
    //tools.addTool(new CreateExportRegionTool(this)); // feature not yet implemented.
      
    //
    tools.addTool(new CreateFiveAxisPathTool(this));
    
      
    //tools.addTool(createFolderTool = new CreateFolderTool(this));
    //tools.addTool(new CreateBlankTool(this));         // Blank, evens out odd number of tools.
      
    if (ArmaRender.getPreferences().getUseCompoundMeshTool())
      defaultTool = compoundTool;
    tools.setDefaultTool(defaultTool);
    tools.selectTool(defaultTool);
    for (int i = 0; i < theView.length; i++)
    {
      theView[i].setMetaTool(metaTool);
      theView[i].setAltTool(altTool);
    }
      
    // Fill in the left hand panel.
    centerContainer.add(tools, 0, 0);
      
      //centerContainer.add(contextTools, 1, 0);
      
      
      // Container for view direction and perspective at bottom of tools pane.
      FormContainer bottomViewContainer;
      bottomViewContainer = new FormContainer(new double [] {0.0, 1.0}, new double [] {0.0, 1.0, 0.0, 0.0});
      bottomViewContainer.setDefaultLayout(new LayoutInfo(LayoutInfo.SOUTH, LayoutInfo.BOTH, null, null));
      bottomViewContainer.setBackground(new Color(220, 220, 220)); // space between the tool bar pane.
      
      // View direction
      viewTools = new ToolPalette(1, 1);
      viewTools.setPreferredSize(new Dimension(96, 96));
      viewTools.setMaximumSize(new java.awt.Dimension (96, 96)); // keeps size constrained so it aligns to the bottom.
      viewTools.setBackground(new Color(220, 220, 220)); // only corners
      ViewTool viewTool = new ViewTool(this);
      viewTools.addTool(viewTool);
      bottomViewContainer.add(viewTools, 0, 1);
      
      // Perspective
      perspectiveTools = new ToolPalette(2, 2);
      perspectiveTools.setPreferredSize(new Dimension(96, 96));
      perspectiveTools.setBackground(new Color(220, 220, 220));
      perspectiveTools.addTool(new ParallelViewTool(this));
      perspectiveTools.addTool(new PerspectiveViewTool(this));
      perspectiveTools.addTool(new SinglePaneTool(this));
      perspectiveTools.addTool(new MultiPaneTool(this));
      bottomViewContainer.add(perspectiveTools, 0, 2);
      
      centerContainer.add(bottomViewContainer, 0, 1);   // add view direction and perspective to bottom of tool pallete.
      
      
    // Set colors
    //centerContainer.getViewport().setBackground(Color.RED);
    //centerContainer.setBackground(new Color(23, 23, 23)); // no effect
    //centerContainer.setBackground(new Color(220, 220, 220));  //
      
    propertiesScroller.setBackground(new Color(220, 220, 220)); // background of property container
    propertiesPanel.setBackground(new Color(220, 220, 220));
      
      
    // Build the menubar.

    menubar = new BMenuBar();
      //menubar.setBackground(Color.red);
      
    setMenuBar(menubar);
    createFileMenu();
    createEditMenu();
    createSceneMenu();
    createObjectMenu();
    createAnimationMenu();
    createToolsMenu();
    createPopupMenu();
      
      
      
      menubar.getComponent().setPreferredSize(new Dimension(1000, 26)); // Set menu bar height to be 26 pixels high.
      
      //
      // menubar Buttons
      //
      JMenuBar jBar = menubar.getComponent();
      jBar.setOpaque(true);
      jBar.setBackground(new Color((float)0.97, (float)0.97, (float)0.97));
      jBar.add(Box.createGlue());
      
      scaleSettings = new ScaleSettings(this);
      
      
      
      // Recent File
      String recentFileName = RecentFiles.getRecentFile(0);
      if(recentFileName != null){
          String fileSeparator = File.separator;
          int recentFileNameStartIndex = recentFileName.lastIndexOf(fileSeparator);
          if( recentFileNameStartIndex != -1 ){
              recentFileName = recentFileName.substring(recentFileNameStartIndex + 1);
          }
          LayoutWindow lw = this;
          AbstractAction actionMostRecentFile = new AbstractAction(recentFileName) {
               public void actionPerformed(ActionEvent evt) {
                   try {
                       String recentFileName = RecentFiles.getRecentFile(0);
                       File file = new File(recentFileName);
                       ArmaRender.openScene(file, lw);
                   } catch (Exception e){
                   }
               }
           };
          mostRecentFile = new JButton(actionMostRecentFile);
          mostRecentFile.setOpaque(true);
          mostRecentFile.setBackground(new Color((float)0.97, (float)0.97, (float)0.97));
          mostRecentFile.setFocusPainted(false);
          mostRecentFile.setRolloverEnabled(false);
          mostRecentFile.setContentAreaFilled(false);
          Font font = mostRecentFile.getFont();
          font = new Font(font.getName(), font.getStyle(),  12);
          mostRecentFile.setFont(font);
          mostRecentFile.setBorder(new RoundedBorder(4, new Color((float)0.95, (float)0.58, (float)0.0)));
          if(theScene.getName() == null && recentFileName.length() > 0){    // Only show if no file loaded and a previous file exists.
              jBar.add(mostRecentFile, BorderLayout.EAST);
          }
      }
      
      
      // Second Most Recent File
      String secondRecentFileName = RecentFiles.getRecentFile(1);
      if(secondRecentFileName != null){
          String fileSeparator = File.separator;
          int secondRecentFileNameStartIndex = secondRecentFileName.lastIndexOf(fileSeparator);
          if( secondRecentFileNameStartIndex != -1 ){
              secondRecentFileName = secondRecentFileName.substring(secondRecentFileNameStartIndex + 1);
          }
          LayoutWindow lw = this;
          AbstractAction actionSecondMostRecentFile = new AbstractAction(secondRecentFileName) {
               public void actionPerformed(ActionEvent evt) {
                   try {
                       String recentFileName = RecentFiles.getRecentFile(1);
                       File file = new File(recentFileName);
                       ArmaRender.openScene(file, lw);
                   } catch (Exception e){
                   }
               }
           };
          secondMostRecentFile = new JButton(actionSecondMostRecentFile);
          secondMostRecentFile.setOpaque(true);
          secondMostRecentFile.setBackground(new Color((float)0.97, (float)0.97, (float)0.97));
          secondMostRecentFile.setMargin(new Insets(0, 0, 0, 0));
          secondMostRecentFile.setContentAreaFilled(false);
          secondMostRecentFile.setFocusPainted(false);
          
          Font font = mostRecentFile.getFont();
          font = secondMostRecentFile.getFont();
          secondMostRecentFile.setFont(new Font(font.getName(), font.getStyle(),  12));
          secondMostRecentFile.setBorder(new RoundedBorder(4, new Color((float)0.95, (float)0.58, (float)0.0)));
          if(theScene.getName() == null && secondRecentFileName.length() > 0){    // Only show if no file loaded and a previous file exists.
              jBar.add(secondMostRecentFile, BorderLayout.EAST);
          }
      }
      
      // Material Button
      LayoutWindow lw = this;
      AbstractAction actionMaterial = new AbstractAction("Material: -") {
           public void actionPerformed(ActionEvent evt) {
               PhysicalMaterial mats = new PhysicalMaterial(lw, lw, lw.getScene());
               mats.display();
           }
       };
      materialButton = new JButton(actionMaterial);
      materialButton.setBorder(new RoundedBorder(4, new Color((float)0.8, (float)0.8, (float)0.8)));
      materialButton.setBackground(new Color((float)0.97, (float)0.97, (float)0.97));
      materialButton.setOpaque(true);
      materialButton.setContentAreaFilled(false);
      materialButton.setFocusPainted(false);
      jBar.add(materialButton, BorderLayout.EAST);
      
      
      // Units Button
      AbstractAction actionScaleUnits = new AbstractAction("Units: " + scaleSettings.getLabel()) {
           public void actionPerformed(ActionEvent evt) {
               scaleSettings.display();
               unitsButton.setText("Units: " + scaleSettings.getLabel());
           }
       };
      unitsButton = new JButton(actionScaleUnits);
      unitsButton.setBackground(new Color((float)0.97, (float)0.97, (float)0.97));
      unitsButton.setOpaque(true);
      
      unitsButton.setContentAreaFilled(false);
      
      unitsButton.setBorder(new RoundedBorder(4, new Color((float)0.8, (float)0.8, (float)0.8)));
      jBar.add(unitsButton, BorderLayout.EAST);
      
      /*
      // Fabricate Button
      AbstractAction actionFabricate = new AbstractAction("Fabricate") {
           public void actionPerformed(ActionEvent evt) {
               System.out.println("Fabricate");
               Fabrication fab = new Fabrication(lw);
               
               //
           }
       };
      JButton fabricateButton = new JButton(actionFabricate);
      fabricateButton.setBackground(new Color((float)0.97, (float)0.97, (float)0.97));
      fabricateButton.setOpaque(true);
      fabricateButton.setContentAreaFilled(false);
      fabricateButton.setFocusPainted(false);
      fabricateButton.setBorder(new RoundedBorder(4, new Color((float)0.26, (float)0.5, (float)0.94)));
      jBar.add(fabricateButton, BorderLayout.EAST);
        */
      
      
      
    // createSolidsMenu(); // JDT depricated

    createLayoutMenu(); // JDT Layout modifications
    createFEAMenu(); // JDT
    //createCFDMenu(); // JDT
      
      createHelpMenu();
      createExamplesMenu();

    preferences = Preferences.userNodeForPackage(getClass()).node("LayoutWindow");
    loadPreferences();
    numViewsShown = (numViewsShown == 1 ? 4 : 1);
    toggleViewsCommand();
    keyEventHandler = new KeyEventPostProcessor()
    {
      public boolean postProcessKeyEvent(KeyEvent e)
      {
        if (e.getID() != KeyEvent.KEY_PRESSED || e.isConsumed())
          return false;
        KeyPressedEvent press = new KeyPressedEvent(LayoutWindow.this, e.getWhen(), e.getModifiersEx(), e.getKeyCode());
        handleKeyEvent(press);
        return (press.isConsumed());
      }
    };
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(keyEventHandler);
    addEventLink(WindowActivatedEvent.class, this, "updateMenus");
    addEventLink(WindowClosingEvent.class, new Object() {
      void processEvent()
      {
        ArmaRender.closeWindow(LayoutWindow.this);
      }
    });
    itemTree.setPopupMenuManager(this);
    itemTree.setEditingWindow(this);
    UIUtilities.applyDefaultFont(getContent());
    //UIUtilities.applyDefaultBackground(centerContainer);
      
    itemTreeScroller.setBackground(Color.white);    // scene object list
    itemSelectionScroller.setBackground(Color.white);
    
    //if (ArmaRender.APP_ICON != null)
    //  setIcon(ArmaRender.APP_ICON);
      
    ImageIcon iconImage = new ImageIcon(getClass().getResource("/armarender/Icons/favicon-32x32.png"));
    this.setIcon(iconImage);
      
    Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    setBounds(screenBounds);
    tools.requestFocus();
    setTime(theScene.getTime());
      
      bsp = new BooleanSceneProcessor(this);
      
      
      //analytics = new Analytics();
      //analytics.start();
      
      //CloudServer cloudServer = new CloudServer(this);
  }

  /** Load all the preferences into memory. */

  protected void loadPreferences()
  {
    boolean lastShowAxes = preferences.getBoolean("showAxes", false);
    numViewsShown = preferences.getInt("numViews", 4);
    byte lastRenderMode[] = preferences.getByteArray("displayMode", new byte[] {ViewerCanvas.RENDER_SMOOTH, ViewerCanvas.RENDER_SMOOTH, ViewerCanvas.RENDER_SMOOTH, ViewerCanvas.RENDER_SMOOTH});
    for (int i = 0; i < theView.length; i++)
    {
      theView[i].setShowAxes(lastShowAxes);
      theView[i].setRenderMode((int) lastRenderMode[i]);
    }
  }

  /** Save user settings that should be persistent between sessions. */

  protected void savePreferences()
  {
    preferences.putBoolean("showAxes", theView[currentView].getShowAxes());
    preferences.putInt("numViews", numViewsShown);
    preferences.putByteArray("displayMode", new byte[] {(byte) theView[0].getRenderMode(), (byte) theView[1].getRenderMode(), (byte) theView[2].getRenderMode(), (byte) theView[3].getRenderMode()});
  }

  private void handleKeyEvent(KeyPressedEvent e)
  {
    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    if (manager.getFocusedWindow() != getComponent() || manager.getFocusOwner() instanceof JTextComponent)
      return;
      
      if(currentView == -1){
          System.out.println("No view active. ");
      }
      if(currentView > -1){
          tools.getSelectedTool().keyPressed(e, theView[currentView]);
      }
      
    if (!e.isConsumed())
      KeystrokeManager.executeKeystrokes(e, this);
  }

  /** Create the TreeList containing all the objects in the scene. */

  private void createItemList()
  {
    itemTree = new TreeList(this);
    itemTree.setPreferredSize(new Dimension(130, 300)); // TODO: calculate height preference based on window height.
    itemTree.addEventLink(TreeList.ElementMovedEvent.class, theScore, "rebuildList");
    itemTree.addEventLink(TreeList.ElementDoubleClickedEvent.class, this, "editObjectCommand");
    itemTree.setUpdateEnabled(false);
    for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        if (info.getParent() == null)
          itemTree.addElement(new ObjectTreeElement(info, itemTree));
      }
    itemTree.setUpdateEnabled(true);
    itemTreeScroller = new BScrollPane(itemTree) {
      public Dimension getMinimumSize()
      {
        return new Dimension(0, 0);
      }
    };
    itemTreeScroller.setForceWidth(true);
    itemTreeScroller.setForceHeight(true);
    itemTreeScroller.getVerticalScrollBar().setUnitIncrement(10);
    itemTree.addEventLink(SelectionChangedEvent.class, this, "treeSelectionChanged");
    
      //new AutoScroller(itemTreeScroller, 0, 10); // Not sure what this does
      
      
      //
      // Folder Icon Tools. (Would prefer this to be at the bottom but its difficult.)
      //
      RowContainer folderIconPanel = new RowContainer(); // BorderContainer
      folderIconPanel.setBackground(Color.white);
      
      Icon folderIcon = ThemeManager.getIcon("folder");
      BButton folderButton = new BButton(folderIcon); // Component is JButton
      folderButton.getComponent().setBorder( BorderFactory.createEmptyBorder(2, 18, 2, 6) ); // int top, int left, int bottom, int right
      //((JButton)folderButton.getComponent()).setTooltip("Create Folder");
                    
      folderButton.setBackground(Color.WHITE);
      
      folderButton.getComponent().setOpaque(true);
      folderButton.getComponent().setFocusPainted(false);
      folderButton.getComponent().setRolloverEnabled(false);
      folderButton.getComponent().setContentAreaFilled(false);
      //folderButton.getComponent().
      folderButton.addEventLink(CommandEvent.class, this, "addFolderCommand");
      folderIconPanel.add(folderButton);
      
      Icon addFolderIcon = ThemeManager.getIcon("folder_union");
      BButton addButton = new BButton(addFolderIcon);
      //addButton.getComponent().setBorder(emptyBorder);
      addButton.getComponent().setOpaque(true);
      addButton.getComponent().setFocusPainted(false);
      addButton.getComponent().setRolloverEnabled(false);
      addButton.getComponent().setContentAreaFilled(false);
      addButton.addEventLink(CommandEvent.class, this, "unionFolderCommand");
      addButton.getComponent().setBorder( BorderFactory.createEmptyBorder(2, 6, 2, 6) ); // int top, int left, int bottom, int right
      folderIconPanel.add(addButton);
      
      Icon subtractFolderIcon = ThemeManager.getIcon("folder_subtract");
      BButton subtractButton = new BButton(subtractFolderIcon);
      subtractButton.getComponent().setOpaque(true);
      subtractButton.getComponent().setFocusPainted(false);
      subtractButton.getComponent().setRolloverEnabled(false);
      subtractButton.getComponent().setContentAreaFilled(false);
      subtractButton.addEventLink(CommandEvent.class, this, "subtractFolderCommand");
      //subtractButton.getComponent().setBorder(emptyBorder);
      subtractButton.getComponent().setBorder( BorderFactory.createEmptyBorder(2, 6, 2, 6) ); // int top, int left, int bottom, int right
      folderIconPanel.add(subtractButton);
      
      Icon intersectionFolderIcon = ThemeManager.getIcon("folder_intersection");
      BButton intersectionButton = new BButton(intersectionFolderIcon);
      intersectionButton.getComponent().setOpaque(true);
      intersectionButton.getComponent().setFocusPainted(false);
      intersectionButton.getComponent().setRolloverEnabled(false);
      intersectionButton.getComponent().setContentAreaFilled(false);
      intersectionButton.addEventLink(CommandEvent.class, this, "intersectionFolderCommand");
      //subtractButton.getComponent().setBorder(emptyBorder);
      intersectionButton.getComponent().setBorder( BorderFactory.createEmptyBorder(2, 6, 2, 6) ); // int top, int left, int bottom, int right
      folderIconPanel.add(intersectionButton);
      
      
      
      //itemTreeScroller.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.BOTH, null, null));
      itemTreeScroller.getComponent().setMaximumSize(new Dimension(10000, 10000));
      
      itemScrollerToolsPane = new ColumnContainer();
      //itemScrollerToolsPane.add(folderIconPanel);
      //itemScrollerToolsPane.add(itemTreeScroller);
      //itemScrollerToolsPane.add(folderIconPanel);
      //TreeListTools tlt = new TreeListTools();
      //itemScrollerToolsPane.add(tlt);
      //itemScrollerToolsPane.add(wcp);
      
      itemTreeScroller.setColHeader(folderIconPanel); // Add item tree controls to scoler header
      
      
      JButton cornerButton = new JButton("#");
      //panel.setCorner(JScrollPane.UPPER_RIGHT_CORNER, cornerButton);
      //itemTreeScroller.setCorner(cornerButton.getComponent());
      
      
      //itemTreeScroller.setBackground(Color.BLUE); // NO
      //itemScrollerToolsPane.setBackground(Color.BLUE); // NO
      
      //RowContainer blankPanel = new RowContainer(); // BorderContainer
      //blankPanel.setBackground(Color.red);
      //blankPanel.getComponent().setPreferredSize(new Dimension(10, 10));
      //itemTreeScroller.setRowHeader( blankPanel );
      
      /*BButton testButton = new BButton("Test");
      testButton.setBackground(Color.BLUE);
      testButton.getComponent().setMaximumSize(new Dimension(1000, 30));
      //itemScrollerToolsPane.add(testButton);*/
      
      //itemTreeScroller.setColFooter(testButton); // Not implemented correctly
      
      //itemScrollerToolsPane.setDefaultLayout(new LayoutInfo(LayoutInfo.WEST, LayoutInfo.BOTH, null, null));
      
      //itemScrollerToolsPane.setBackground(Color.RED);
      //itemScrollerToolsPane.layoutChildren();
      
    //itemScrollerToolsPane = new BSplitPane(BSplitPane.VERTICAL, itemTreeScroller, folderIconPanel);
  }
    
    /**
     * addFolderCommand
     *
     */
    public void addFolderCommand(){
        String name = "Folder";
        
        Folder folderObj = new Folder();
        CoordinateSystem coords;
        coords = new CoordinateSystem();
        
        ObjectInfo info = new ObjectInfo(folderObj, coords, name);
        //info.addTrack(new PositionTrack(info), 0);
        //info.addTrack(new RotationTrack(info), 1);
        
        int sel[] = getSelectedIndices();
        ObjectInfo existingSelectedInfo = null;
        if(sel.length == 1){
            existingSelectedInfo = ((LayoutWindow) this).getScene().getObject( sel[0] );
            if(existingSelectedInfo != null
               //&&
               //( existingSelectedInfo.getObject() instanceof Mesh ||
               // existingSelectedInfo.getObject() instanceof Curve)
               ){
                existingSelectedInfo.addChild(info, 0);
                info.setParent(existingSelectedInfo);
            }
        }
        
        UndoRecord undo = new UndoRecord(this, false);
        
        this.addObject(info, undo);
        undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
        this.setUndoRecord(undo);
        ((LayoutWindow) this).setSelection(((LayoutWindow) this).getScene().getNumObjects()-1);
        ((LayoutWindow) this).rebuildItemList(); // redraw item list.
        //points = null;
        this.updateImage();
        
        Sound.playSound("success.wav");
    }
    
    public void unionFolderCommand(){
        //Sound.playSound("deny.wav");
        String name = "Union";
        
        Folder folderObj = new Folder();
        folderObj.setAction(Folder.UNION);
        CoordinateSystem coords;
        coords = new CoordinateSystem();
        
        ObjectInfo info = new ObjectInfo(folderObj, coords, name);
        //info.addTrack(new PositionTrack(info), 0);
        //info.addTrack(new RotationTrack(info), 1);
        
        int sel[] = getSelectedIndices();
        ObjectInfo existingSelectedInfo = null;
        if(sel.length == 1){
            existingSelectedInfo = ((LayoutWindow) this).getScene().getObject( sel[0] );
            if(existingSelectedInfo != null
               //&&
               //( existingSelectedInfo.getObject() instanceof Mesh ||
               // existingSelectedInfo.getObject() instanceof Curve)
               ){
                existingSelectedInfo.addChild(info, 0);
                info.setParent(existingSelectedInfo);
            }
        }
        UndoRecord undo = new UndoRecord(this, false);
        
        this.addObject(info, undo);
        undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
        this.setUndoRecord(undo);
        ((LayoutWindow) this).setSelection(((LayoutWindow) this).getScene().getNumObjects()-1);
        ((LayoutWindow) this).rebuildItemList(); // redraw item list.
        //points = null;
        
        // Expand parent list tree to show new folder
        TreeElement treeElements [] =  itemTree.getElements();
        for(int i = 0; i < treeElements.length; i++){
            TreeElement te = treeElements[i];
            Object ob = te.getObject();
            ObjectInfo currInfo = (ObjectInfo)te.getObject();
            if(currInfo == info && existingSelectedInfo != null){ // existingSelectedInfo
                itemTree.expandToShowObject(te);
            }
        }
        
        this.updateImage();
        
        Sound.playSound("success.wav");
    }
    
    public void subtractFolderCommand(){
        //Sound.playSound("deny.wav");
        String name = "Subtract";
        
        Folder folderObj = new Folder();
        folderObj.setAction(Folder.SUBTRACT);
        CoordinateSystem coords;
        coords = new CoordinateSystem();
        
        ObjectInfo info = new ObjectInfo(folderObj, coords, name);
        //info.addTrack(new PositionTrack(info), 0);
        //info.addTrack(new RotationTrack(info), 1);
        
        int sel[] = getSelectedIndices();
        ObjectInfo existingSelectedInfo = null;
        if(sel.length == 1){
            existingSelectedInfo = ((LayoutWindow)this).getScene().getObject( sel[0] );
            if(existingSelectedInfo != null
               //&&
               //( existingSelectedInfo.getObject() instanceof Mesh ||
               // existingSelectedInfo.getObject() instanceof Curve)
               ){
                existingSelectedInfo.addChild(info, 0);
                info.setParent(existingSelectedInfo);
            }
        }
        UndoRecord undo = new UndoRecord(this, false);
        
        this.addObject(info, undo);
        undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
        this.setUndoRecord(undo);
        ((LayoutWindow) this).setSelection(((LayoutWindow) this).getScene().getNumObjects()-1);
        ((LayoutWindow) this).rebuildItemList(); // redraw item list.
        
        // Expand parent list tree to show new folder
        TreeElement treeElements [] =  itemTree.getElements();
        for(int i = 0; i < treeElements.length; i++){
            TreeElement te = treeElements[i];
            Object ob = te.getObject();
            ObjectInfo currInfo = (ObjectInfo)te.getObject();
            if(currInfo == info && existingSelectedInfo != null){ // existingSelectedInfo
                itemTree.expandToShowObject(te);
            }
        }
        
        //points = null;
        this.updateImage();
        
        Sound.playSound("success.wav");
    }
    
    public void intersectionFolderCommand(){
        //Sound.playSound("deny.wav");
        String name = "Intersection";
        
        Folder folderObj = new Folder();
        folderObj.setAction(Folder.INTERSECTION);
        CoordinateSystem coords;
        coords = new CoordinateSystem();
        
        ObjectInfo info = new ObjectInfo(folderObj, coords, name);
        //info.addTrack(new PositionTrack(info), 0);
        //info.addTrack(new RotationTrack(info), 1);
        
        int sel[] = getSelectedIndices();
        ObjectInfo existingSelectedInfo = null;
        if(sel.length == 1){
            existingSelectedInfo = ((LayoutWindow) this).getScene().getObject( sel[0] );
            if(existingSelectedInfo != null
               //&&
               //( existingSelectedInfo.getObject() instanceof Mesh ||
               // existingSelectedInfo.getObject() instanceof Curve)
               ){
                existingSelectedInfo.addChild(info, 0);
                info.setParent(existingSelectedInfo);
            }
        }
        UndoRecord undo = new UndoRecord(this, false);
        
        this.addObject(info, undo);
        undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
        this.setUndoRecord(undo);
        ((LayoutWindow) this).setSelection(((LayoutWindow) this).getScene().getNumObjects()-1);
        ((LayoutWindow) this).rebuildItemList(); // redraw item list.
        //points = null;
        
        // Expand parent list tree to show new folder
        TreeElement treeElements [] =  itemTree.getElements();
        for(int i = 0; i < treeElements.length; i++){
            TreeElement te = treeElements[i];
            Object ob = te.getObject();
            ObjectInfo currInfo = (ObjectInfo)te.getObject();
            if(currInfo == info && existingSelectedInfo != null){ // existingSelectedInfo
                itemTree.expandToShowObject(te);
            }
        }
        
        this.updateImage();
        
        Sound.playSound("success.wav");
    }
    
    /**
     * createItemSelectionList
     *
     * Description: Select one of many possible objects.
     */
    private void createItemSelectionList()
    {
        itemSelectionTree = new TreeList(this);
        itemSelectionTree.setPreferredSize(new Dimension(130, 60)); // 130, 300
        itemSelectionTree.addEventLink(TreeList.ElementMovedEvent.class, theScore, "rebuildList");
        itemSelectionTree.addEventLink(TreeList.ElementDoubleClickedEvent.class, this, "editObjectCommand");
        itemSelectionTree.setUpdateEnabled(false);
        /*
        for (int i = 0; i < theScene.getNumObjects(); i++)
        {
            ObjectInfo info = theScene.getObject(i);
            if (info.getParent() == null)
                //itemSelectionTree.addElement(new ObjectTreeElement(info, itemTree));
        }
        */
        itemSelectionTree.setUpdateEnabled(true);
        
        
        itemSelectionScroller = new BScrollPane(itemSelectionTree) {
            public Dimension getMinimumSize()
            {
                return new Dimension(0, 0);
            }
            
            public Dimension getPreferredSize()
            {
                return new Dimension(40, 15);
            }
        };
        
        
        itemSelectionScroller.setForceWidth(true);
        itemSelectionScroller.setForceHeight(true);
        itemSelectionScroller.getVerticalScrollBar().setUnitIncrement(10);
        
        itemSelectionTree.addEventLink(SelectionChangedEvent.class, this, "treeSelectionPickerChanged");
        //new AutoScroller(itemTreeScroller, 0, 10);
    }
    
    

  /** Rebuild the TreeList of objects, attempting as much as possible to preserve its
      current state. */

  public void rebuildItemList()
  {
    boolean expanded[] = new boolean [theScene.getNumObjects()],
            selected[] = new boolean [theScene.getNumObjects()];

    for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        TreeElement el = itemTree.findElement(info);
        if (el == null)
          continue;
        expanded[i] = el.isExpanded(); // index out of bounds from CreateMirrorPlaneTool.
        selected[i] = el.isSelected();
      }
    itemTree.setUpdateEnabled(false);
    itemTree.removeAllElements();
    for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        if (info.getParent() == null)
          itemTree.addElement(new ObjectTreeElement(info, itemTree));
      }
    for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        TreeElement el = itemTree.findElement(info);
        if (el == null)
          continue;
        el.setExpanded(expanded[i]);
        el.setSelected(selected[i]);
      }
    itemTree.setUpdateEnabled(true);
    theScore.rebuildList();
  }

  /** This is called whenever the user moves a DockableWidget.  It saves the current configuration
      to the preferences. */

  private void dockableWidgetMoved()
  {
    StringBuffer config = new StringBuffer();
    for (int i = 0; i < dock.length; i++)
    {
      for (int j = 0; j < dock[i].getTabCount(); j++)
      {
        for (int k = 0; k < dock[i].getTabChildCount(j); k++)
        {
          DockableWidget w = dock[i].getChild(j, k);
          config.append(w.getContent().getClass().getName());
          config.append('\t');
          config.append(w.getLabel());
          config.append('\n');
        }
        config.append('\n');
      }
      config.append("-\n");
    }
    Preferences prefs = Preferences.userNodeForPackage(getClass()).node("LayoutWindow");
    prefs.put("dockingConfiguration", config.toString());
  }

  /** This is called when the window is first created.  It attempts to arrange the DockableWidgets
      however they were last arranged by the user. */

  void arrangeDockableWidgets()
  {
    // Look up how they were last arranged.

    Preferences prefs = Preferences.userNodeForPackage(getClass()).node("LayoutWindow");
    String config = prefs.get("dockingConfiguration", null);
    if (config == null)
      return;

    // Make a table of all DockableWidgets.

    HashMap<String, DockableWidget> widgets = new HashMap<String, DockableWidget>();
    for (int i = 0; i < dock.length; i++)
    {
      for (Widget next : dock[i].getChildren())
      {
        if (next instanceof DockableWidget)
        {
          DockableWidget w = (DockableWidget) next;
          widgets.put(w.getContent().getClass().getName()+'\t'+w.getLabel(), w);
        }
      }
    }

    // Rearrange them.

    String lines[] = config.split("\n");
    int container = 0, tab = 0, index = 0;
    for (int i = 0; i < lines.length; i++)
    {
      if (lines[i].length() == 0)
      {
        tab++;
        index = 0;
      }
      else if ("-".equals(lines[i]))
      {
        container++;
        tab = 0;
        index = 0;
      }
      else
      {
        DockableWidget w = widgets.get(lines[i]);
        if (w != null)
        {
          dock[container].addDockableWidget(w, tab, index++);
          widgets.remove(lines[i]);
        }
      }
    }
    setScoreVisible(false);
  }

  private void createFileMenu()
  {
    BMenuItem item;
    BMenu importMenu, exportMenu;
    List<Translator> trans = PluginRegistry.getPlugins(Translator.class);

    fileMenu = Translate.menu("file");
      //fileMenu.setBackground(new Color(0, 0, 0));
      
    menubar.add(fileMenu);
    importMenu = Translate.menu("import");
    exportMenu = Translate.menu("export");
    fileMenuItem = new BMenuItem [1];
    
      fileMenu.add(Translate.menuItem("Account", this, "accountSettingsCommand"));
      fileMenu.addSeparator();
      
      fileMenu.add(Translate.menuItem("new", this, "actionPerformed"));
    fileMenu.add(Translate.menuItem("open", this, "actionPerformed"));
    fileMenu.add(recentFilesMenu = Translate.menu("openRecent"));
    RecentFiles.createMenu(recentFilesMenu);
    fileMenu.add(Translate.menuItem("close", this, "actionPerformed"));
    fileMenu.addSeparator();
    
      fileMenu.add(Translate.menuItem("Download Files", this, "downloadFilesCommand"));
      fileMenu.addSeparator();
      
      fileMenu.add(Translate.menuItem("Inventory Management", this, "inventoryManagementCommand"));
      
      // Backup tool
      BMenuItem fileBackupMenuItem = Translate.menuItem("Backups", this, "backupCommand");
      fileBackupMenuItem.setName("file_backup");   // Name is used by event handler for ID
      fileBackupMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      fileBackupMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      fileMenu.add(fileBackupMenuItem);
      
      fileMenu.addSeparator();
      
    Collections.sort(trans, new Comparator<Translator>() {
      public int compare(Translator o1, Translator o2)
      {
        return o1.getName().compareTo(o2.getName());
      }
    });
    for (int i = 0; i < trans.size(); i++)
      {
        if (trans.get(i).canImport())
          {
            importMenu.add(item = new BMenuItem(trans.get(i).getName()));
            item.setActionCommand("import");
            item.addEventLink(CommandEvent.class, this, "actionPerformed");
          }
        if (trans.get(i).canExport())
          {
            exportMenu.add(item = new BMenuItem(trans.get(i).getName()));
            item.setActionCommand("export");
            item.addEventLink(CommandEvent.class, this, "actionPerformed");
          }
      }
    // Export to DXF
    exportMenu.add(Translate.menuItem("AutoDesk AutoCAD DXF (.dxf)", this, "exportDXF"));
    // csv export - todo move to module
    exportMenu.add(Translate.menuItem("Curve Points (.csv)", this, "exportObjectCSV"));
      
    exportMenu.add(Translate.menuItem("Stereolithography (.stl)", this, "exportSTL"));
    exportMenu.add(Translate.menuItem("Initial Graphics Exchange Specification (.iges)", this, "exportIGES"));
      
    // BeamNG Drive
    exportMenu.add(Translate.menuItem("BeamNG Drive", this, "exportBeamNGDrive"));
      
      // PDF
      //exportMenu.add(Translate.menuItem("PDF Drawings", this, "exportArcTubeBendPDF"));
      BMenuItem fileExportArcTubeBendPDFMenuItem = Translate.menuItem("PDF Drawings", this, "exportArcTubeBendPDF");
      fileExportArcTubeBendPDFMenuItem.setName("cam_exportArcTubeBendPDF");   // Name is used by event handler for ID
      fileExportArcTubeBendPDFMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      fileExportArcTubeBendPDFMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      exportMenu.add(fileExportArcTubeBendPDFMenuItem);
      
      
      
    if (importMenu.getChildCount() > 0)
      fileMenu.add(importMenu);
    if (exportMenu.getChildCount() > 0)
      fileMenu.add(exportMenu);
    fileMenu.add(Translate.menuItem("linkExternal", this, "linkExternalCommand"));
    fileMenu.addSeparator();
    fileMenu.add(fileMenuItem[0] = Translate.menuItem("save", this, "saveCommand"));
    fileMenu.add(Translate.menuItem("saveas", this, "saveAsCommand"));
    fileMenu.addSeparator();
    fileMenu.add(Translate.menuItem("quit", this, "actionPerformed"));
      
      
  }

  private void createEditMenu()
  {
    editMenu = Translate.menu("edit");
    menubar.add(editMenu);
    editMenuItem = new BMenuItem [10];
    editMenu.add(editMenuItem[0] = Translate.menuItem("undo", this, "undoCommand"));
    editMenu.add(editMenuItem[1] = Translate.menuItem("redo", this, "redoCommand"));
    editMenu.addSeparator();
    editMenu.add(editMenuItem[2] = Translate.menuItem("cut", this, "cutCommand"));
    editMenu.add(editMenuItem[3] = Translate.menuItem("copy", this, "copyCommand"));
    editMenu.add(editMenuItem[4] = Translate.menuItem("paste", this, "pasteCommand"));
      editMenu.add(editMenuItem[5] = Translate.menuItem("Paste as child", this, "pasteAsChildCommand"));
      
    editMenu.add(editMenuItem[6] = Translate.menuItem("clear", this, "clearCommand"));
    editMenu.add(editMenuItem[7] = Translate.menuItem("selectChildren", this, "actionPerformed"));
    editMenu.add(Translate.menuItem("selectAll", this, "selectAllCommand"));
      
      editMenu.add(Translate.menuItem("selectAllCurves", this, "selectAllCurvesCommand"));
      
    editMenu.addSeparator();
    editMenu.add(editMenuItem[8] = Translate.menuItem("duplicate", this, "duplicateCommand"));
    editMenu.add(editMenuItem[9] = Translate.menuItem("sever", this, "severCommand"));
    editMenu.addSeparator();
    editMenu.add(Translate.menuItem("preferences", this, "preferencesCommand"));
  }

    /**
     * createObjectMenu
     *
     */
  private void createObjectMenu()
  {
    BMenu objectDisplayMenu;
      
    objectMenu = Translate.menu("object");
    menubar.add(objectMenu);
    objectMenuItem = new BMenuItem [14]; // 12
 
    objectMenu.add(objectMenuItem[0] = Translate.menuItem("editObject", this, "editObjectCommand"));
      
      //objectMenu.add(Translate.menuItem("numericEditObject", this, "numericEditObjectCommand"));
      
    objectMenu.add(objectMenuItem[1] = Translate.menuItem("objectLayout", this, "objectLayoutCommand"));
    objectMenu.add(objectMenuItem[2] = Translate.menuItem("transformObject", this, "transformObjectCommand"));
    objectMenu.add(objectMenuItem[3] = Translate.menuItem("alignObjects", this, "alignObjectsCommand"));
    objectMenu.add(objectMenuItem[4] = Translate.menuItem("setTextureAndMaterial", this, "setTextureCommand"));
    objectMenu.add(objectMenuItem[5] = Translate.menuItem("renameObject", this, "renameObjectCommand"));
    objectMenu.add(objectMenuItem[6] = Translate.menuItem("convertToTriangle", this, "convertToTriangleCommand"));
    objectMenu.add(objectMenuItem[7] = Translate.menuItem("convertToActor", this, "convertToActorCommand"));
    objectMenu.addSeparator();

    objectMenu.add(Translate.menuItem("Auto Align Selection to Scene", this, "autoAlignTransformCommand"));
      
      objectMenu.add(Translate.menuItem("Zero Transform", this, "zeroTransformCommand"));
      
    //objectMenu.addSeparator();
    //objectMenu.add(Translate.menuItem("Join Object Vertices", this, "joinObjectVerticesCommand")); // depricate
    //objectMenu.add(Translate.menuItem("Connect Curves", this, "connectCurvesCommand")); // move to tools menu
      
      
      objectMenu.addSeparator();
      // Numeric Tube Editor
      //objectMenu.add(Translate.menuItem("numericEditObject", this, "numericEditObjectCommand"));
      
      
    objectMenu.addSeparator();
    objectMenu.add(Translate.menuItem("Find Object", this, "findObjectCommand"));
    objectMenu.addSeparator();

    objectMenu.add(Translate.menuItem("Set Object Group", this, "setObjectGroupCommand"));
      
    objectMenu.addSeparator();
    objectMenu.add(Translate.menuItem("Get Object Info", this, "getObjectInfo") );
      
    objectMenu.addSeparator();
      
      // Set object display mode
      objectMenu.add(objectDisplayMenu = Translate.menu("displayMode"));
      objectDisplayItem = new BCheckBoxMenuItem [6];
      //displayMenu.add(displayItem[0] = Translate.checkboxMenuItem("wireframeDisplay", this, "displayModeCommand", theView[0].getRenderMode() == ViewerCanvas.RENDER_WIREFRAME));
      objectDisplayMenu.add(objectDisplayItem[0] = Translate.checkboxMenuItem("default", this, "objectDisplayModeCommand", false));
      objectDisplayMenu.add(objectDisplayItem[1] = Translate.checkboxMenuItem("wireframeDisplay", this, "objectDisplayModeCommand", false));
      objectDisplayMenu.add(objectDisplayItem[2] = Translate.checkboxMenuItem("transparentDisplay", this, "objectDisplayModeCommand", false));
      //objectDisplayMenu.add(objectDisplayItem[3] = Translate.checkboxMenuItem("renderedDisplay", this, "objectDisplayModeCommand", false));
      
      
    objectMenu.addSeparator();
    objectMenu.add(objectMenuItem[8] = Translate.menuItem("hideSelection", this, "actionPerformed"));
    objectMenu.add(objectMenuItem[9] = Translate.menuItem("showSelection", this, "actionPerformed"));
    objectMenu.add(Translate.menuItem("showAll", this, "actionPerformed"));
      
      objectMenu.add(objectMenuItem[10] = Translate.menuItem("hideChildrenSelection", this, "actionPerformed")); // hideChildren
      objectMenu.add(objectMenuItem[11] = Translate.menuItem("showChildrenSelection", this, "actionPerformed")); // show children
      
    objectMenu.addSeparator();
    objectMenu.add(objectMenuItem[12] = Translate.menuItem("lockSelection", this, "actionPerformed"));   // 10 -> 12
    objectMenu.add(objectMenuItem[13] = Translate.menuItem("unlockSelection", this, "actionPerformed")); // 11 -> 13
    objectMenu.add(Translate.menuItem("unlockAll", this, "actionPerformed"));
    objectMenu.addSeparator();
    objectMenu.add(createMenu = Translate.menu("createPrimitive"));
    createMenu.add(Translate.menuItem("cube", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("sphere", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("cylinder", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("cone", this, "createObjectCommand"));
    createMenu.addSeparator();
    createMenu.add(Translate.menuItem("pointLight", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("directionalLight", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("spotLight", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("proceduralPointLight", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("proceduralDirectionalLight", this, "createObjectCommand"));
    createMenu.addSeparator();
    createMenu.add(Translate.menuItem("camera", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("referenceImage", this, "createObjectCommand"));
    createMenu.add(Translate.menuItem("null", this, "createObjectCommand"));
      
      objectMenu.addSeparator();
      objectMenu.add(Translate.menuItem("Fix Corrupted Objects", this, "fixCorruptionCommand") );
      
      // Regenerate mirror objects
      // TODO: Some
      
      
  }

  private void createToolsMenu()
  {
    modellingTools = PluginRegistry.getPlugins(ModellingTool.class);
    Collections.sort(modellingTools, new Comparator<ModellingTool>() {
      public int compare(ModellingTool o1, ModellingTool o2)
      {
        return (o1.getName().compareTo(o2.getName()));
      }
    });
    toolsMenu = Translate.menu("tools");
    menubar.add(toolsMenu);
    toolsMenuItem = new BMenuItem [modellingTools.size()];
    for (int i = 0; i < modellingTools.size(); i++)
      {
        BMenuItem item = new BMenuItem(modellingTools.get(i).getName());
          
          //item.setName("tools_modellingToolCommand");
          
        toolsMenu.add(item);
        item.setActionCommand("modellingTool");
        item.addEventLink(CommandEvent.class, this, "modellingToolCommand");
        toolsMenuItem[i] = item;
      }
      
      toolsMenu.addSeparator();
      
      // Arange Menu
      BMenu toolsArangeMenu;
      toolsMenu.add(toolsArangeMenu = Translate.menu("Arange"));
      
      // Space Equally
      BMenuItem toolsArangeSpaceEquallyMenuItem = Translate.menuItem("Space Selection Equaly", this, "spaceEquallyCommand");
      toolsArangeSpaceEquallyMenuItem.setName("tools_spaceEquallyCommand"); // Name is used by event handler for ID
      toolsArangeSpaceEquallyMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsArangeSpaceEquallyMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //toolsMenu.add(toolsArangeSpaceEquallyMenuItem);
      toolsArangeMenu.add(toolsArangeSpaceEquallyMenuItem);
      
      
      toolsMenu.addSeparator();

      BMenu toolsSplinesMenu;
      toolsMenu.add(toolsSplinesMenu = Translate.menu("Curve"));
      
      BMenu toolsMeshMenu;
      toolsMenu.add(toolsMeshMenu = Translate.menu("Mesh"));
      
      BMenu toolsTubeMenu;
      toolsMenu.add(toolsTubeMenu = Translate.menu("Tube"));
      
      // Resize Mesh
      BMenuItem toolsResizeMeshMenuItem = Translate.menuItem("Resize Mesh", this, "resizeMeshCommand");
      toolsResizeMeshMenuItem.setName("tools_resizeMeshCommand"); // Name is used by event handler for ID
      toolsResizeMeshMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsResizeMeshMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //toolsMenu.add(toolsResizeMeshMenuItem);
      toolsMeshMenu.add(toolsResizeMeshMenuItem);
      
      
      //Conform mesh to child curves
      BMenuItem toolsConformMeshMenuItem = Translate.menuItem("Conform Mesh to Curves", this, "conformMeshToCurvesCommand");
      toolsConformMeshMenuItem.setName("tools_conformMeshToCurvesCommand"); // Name is used by event handler for ID
      toolsConformMeshMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsConformMeshMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //toolsMenu.add(toolsConformMeshMenuItem);
      toolsMeshMenu.add(toolsConformMeshMenuItem);
      
      //toolsMenu.add(Translate.menuItem("Conform Curve to Curves", this, "conformCurveToCurvesCommand"));
      BMenuItem toolsConformCurveMenuItem = Translate.menuItem("Conform Curve to Curves", this, "conformCurveToCurvesCommand");
      toolsConformCurveMenuItem.setName("tools_conformCurveToCurvesCommand"); // Name is used by event handler for ID
      toolsConformCurveMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsConformCurveMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //toolsMenu.add(toolsConformCurveMenuItem);
      toolsSplinesMenu.add(toolsConformCurveMenuItem);
      
      //toolsMenu.addSeparator();
      
      
      
      
    //toolsMenu.add(Translate.menuItem("Join Multiple Splines", this, "joinMultipleSplines"));
      BMenuItem toolsJoinSplinesMenuItem = Translate.menuItem("Join Multiple Splines", this, "joinMultipleSplines");
      toolsJoinSplinesMenuItem.setName("tools_joinMultipleSplines"); // Name is used by event handler for ID
      toolsJoinSplinesMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsJoinSplinesMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //toolsMenu.add(toolsJoinSplinesMenuItem);
      toolsSplinesMenu.add(toolsJoinSplinesMenuItem);
      
    //toolsMenu.add(Translate.menuItem("Straighten Spline (XY Plane)", this, "straightenSpline"));
      BMenuItem toolsStraightenSplineMenuItem = Translate.menuItem("Straighten Spline (XY Plane)", this, "straightenSpline");
      toolsStraightenSplineMenuItem.setName("tools_straightenSpline"); // Name is used by event handler for ID
      toolsStraightenSplineMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsStraightenSplineMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //toolsMenu.add(toolsStraightenSplineMenuItem);
      toolsSplinesMenu.add(toolsStraightenSplineMenuItem);
    
      //toolsMenu.add(Translate.menuItem("Get Spline Length", this, "getSplineLength"));
      BMenuItem toolsSplineLengthMenuItem = Translate.menuItem("Get Spline Length", this, "getSplineLength");
      toolsSplineLengthMenuItem.setName("tools_getSplineLength"); // Name is used by event handler for ID
      toolsSplineLengthMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsSplineLengthMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //toolsMenu.add(toolsSplineLengthMenuItem);
      toolsSplinesMenu.add(toolsSplineLengthMenuItem);
      
      //toolsMenu.add(Translate.menuItem("Get Curve from Tube", this, "getCurveFromTube"));
      BMenuItem toolsTubeCurveMenuItem = Translate.menuItem("Get Curve from Tube Mesh", this, "getCurveFromTubeMesh");
      toolsTubeCurveMenuItem.setName("tools_getCurveFromTube"); // Name is used by event handler for ID
      toolsTubeCurveMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsTubeCurveMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //toolsMenu.add(toolsTubeCurveMenuItem);
      toolsSplinesMenu.add(toolsTubeCurveMenuItem);
      
      //
      // Conform Curve to Mesh Object
      BMenuItem toolsConformCurveToMeshMenuItem = Translate.menuItem("Conform Curve to Mesh Object", this, "conformCurveToMesh");
      toolsConformCurveToMeshMenuItem.setName("tools_conformCurveToMesh"); // Name is used by event handler for ID
      toolsConformCurveToMeshMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsConformCurveToMeshMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //toolsMenu.add(toolsConformCurveToMeshMenuItem);
      toolsSplinesMenu.add(toolsConformCurveToMeshMenuItem);
      
      
      // toolsMenu.add(Translate.menuItem("Get Tube from Curve", this, "getTubeFromCurve")); // replaces with arc version
      //toolsMenu.add(Translate.menuItem("Get Tube from Arc", this, "getTubeFromArc"));
      //BMenuItem toolsTubeFromArcMenuItem = Translate.menuItem("Get Tube from Arc", this, "getTubeFromArc");
      //toolsTubeFromArcMenuItem.setName("tools_getTubeFromArc"); // Name is used by event handler for ID
      //toolsTubeFromArcMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      //toolsTubeFromArcMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //toolsTubeMenu.add(toolsTubeFromArcMenuItem);
      
      //toolsMenu.add(Translate.menuItem("Combined Tube Length", this, "getCombinedTubeLength"));
      BMenuItem toolsTubeLengthMenuItem = Translate.menuItem("Get Combined Tube Length", this, "getCombinedTubeLength");
      toolsTubeLengthMenuItem.setName("tools_getCombinedTubeLength"); // Name is used by event handler for ID
      toolsTubeLengthMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsTubeLengthMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //toolsTubeMenu.add(toolsTubeLengthMenuItem);
      
      BMenuItem toolsTubeGetTubeFromGeometryMenuItem = Translate.menuItem("Get Arc Tube from Mesh", this, "getArcFromTubeMesh");
      toolsTubeGetTubeFromGeometryMenuItem.setName("tools_getArcFromTubeMesh"); // Name is used by event handler for ID
      toolsTubeGetTubeFromGeometryMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsTubeGetTubeFromGeometryMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      toolsTubeMenu.add(toolsTubeGetTubeFromGeometryMenuItem);
      
      BMenuItem extractArcTubeGeometryMenuItem = Translate.menuItem("Get Mesh from Arc Tube", this, "extractArcTubeGeometryCommand");
      extractArcTubeGeometryMenuItem.setName("tools_extractArcTubeGeometryCommand"); // Name is used by event handler for ID
      extractArcTubeGeometryMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      extractArcTubeGeometryMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //toolsMenu.add(extractArcTubeGeometryMenuItem);
      toolsTubeMenu.add(extractArcTubeGeometryMenuItem);
      
      // Snap tube to adjacent centers
      BMenuItem snapTubeToAdjacentCentersMenuItem = Translate.menuItem("Snap Tube to Adjacent Centers", this, "snapTubeToAdjacentCentersCommand");
      snapTubeToAdjacentCentersMenuItem.setName("tools_snapTubeToAdjacentCentersCommand"); // Name is used by event handler for ID
      snapTubeToAdjacentCentersMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      snapTubeToAdjacentCentersMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      toolsTubeMenu.add(snapTubeToAdjacentCentersMenuItem);
      
      
      // toolsTubeMenu
      
    toolsMenu.addSeparator();
    // Tube / CAM Notching functions
      
      
      
      // Add bend markers
      //BMenuItem toolsAddBendMarkersMenuItem = Translate.menuItem("Add Bend Markers", this, "addBendMarkersCommand");
      //toolsAddBendMarkersMenuItem.setName("tools_addBendMarkersCommand"); // Name is used by event handler for ID
      //toolsAddBendMarkersMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      //toolsAddBendMarkersMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //tubeNotchMenu.add(toolsAddBendMarkersMenuItem);
      
      
      
      //
      
      
      
      
    toolsMenu.addSeparator();
      
      //toolsMenu.add(Translate.menuItem("Connect Curves", this, "connectCurvesCommand"));
      BMenuItem toolsConnectCurvesMenuItem = Translate.menuItem("Connect Curves", this, "connectCurvesCommand");
      toolsConnectCurvesMenuItem.setName("tools_connectCurvesCommand"); // Name is used by event handler for ID
      toolsConnectCurvesMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsConnectCurvesMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      toolsMenu.add(toolsConnectCurvesMenuItem);
      //curveMeshingMenu.add(toolsConnectCurvesMenuItem);
      
      //toolsMenu.add(Translate.menuItem("Mesh from Connected Curves (Beta)", this, "connectedCurvesToMeshCommand"));
      
      //toolsMenu.add(Translate.menuItem("Mesh (quad) from Connected Curves", this, "connectedCurvesToQuadMeshCommand"));
      BMenuItem toolsCurvesToMeshMenuItem = Translate.menuItem("Mesh (quad) from Connected Curves", this, "connectedCurvesToQuadMeshCommand");
      toolsCurvesToMeshMenuItem.setName("tools_connectedCurvesToQuadMeshCommand"); // Name is used by event handler for ID
      toolsCurvesToMeshMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsCurvesToMeshMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      toolsMenu.add(toolsCurvesToMeshMenuItem);
      
      toolsMenu.addSeparator();
      
    // Auto Skin
    // TODO
    // new SkinDialog(window, curves);
    //toolsMenu.add(Translate.menuItem("Auto Skin", this, "autoSkin"));
      BMenuItem toolsAutoSkinMenuItem = Translate.menuItem("Auto Skin", this, "autoSkin");
      toolsAutoSkinMenuItem.setName("tools_autoSkin"); // Name is used by event handler for ID
      toolsAutoSkinMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsAutoSkinMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      toolsMenu.add(toolsAutoSkinMenuItem);
      
      
      //toolsMenu.add(Translate.menuItem("Auto Skin by Voids", this, "autoSkinByVoids"));
      BMenuItem toolsAutoSkinByVoidsMenuItem = Translate.menuItem("Auto Skin by Voids", this, "autoSkinByVoids");
      toolsAutoSkinByVoidsMenuItem.setName("tools_autoSkinByVoids"); // Name is used by event handler for ID
      toolsAutoSkinByVoidsMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsAutoSkinByVoidsMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      toolsMenu.add(toolsAutoSkinByVoidsMenuItem);
      
      
      //toolsMenu.add(Translate.menuItem("Spline Grid Skin", this, "splineGridSkin"));
      BMenuItem toolsSplineGridSkinMenuItem = Translate.menuItem("Spline Grid Skin", this, "splineGridSkin");
      toolsSplineGridSkinMenuItem.setName("tools_splineGridSkin"); // Name is used by event handler for ID
      toolsSplineGridSkinMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsSplineGridSkinMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      toolsMenu.add(toolsSplineGridSkinMenuItem);
      
      
      //toolsMenu.addSeparator();
      // TODO: ***
      // Link Mesh Vertex to Closest Point on Curve
      
      
      
      
      //BMenu curveMeshingMenu;
      //toolsMenu.add(curveMeshingMenu = Translate.menu("Tube Notching"));
      
      
      /*
       // Commented out until implemented
      BMenuItem toolsJoinCurvesMenuItem = Translate.menuItem("Join Curves (Alters)", this, "joinCurvesCommand");
      toolsJoinCurvesMenuItem.setName("tools_joinCurvesCommand"); // Name is used by event handler for ID
      toolsJoinCurvesMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsJoinCurvesMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      toolsMenu.add(toolsJoinCurvesMenuItem);
      //curveMeshingMenu.add(toolsJoinCurvesMenuItem);
      */
      
      //toolsMenu.add(Translate.menuItem("Move and Connect Curves", this, "moveConnectCurvesCommand"));
      
      BMenuItem toolsMeshEdgeCurvesMenuItem = Translate.menuItem("Mesh Edge Curves", this, "meshEdgeCurvesCommand");
      toolsMeshEdgeCurvesMenuItem.setName("tools_meshEdgeCurvesCommand"); // Name is used by event handler for ID
      toolsMeshEdgeCurvesMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toolsMeshEdgeCurvesMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //toolsMenu.add(toolsMeshEdgeCurvesMenuItem);
      toolsMeshMenu.add(toolsMeshEdgeCurvesMenuItem);
      
      
      
      
      
      //toolsMenu.add(Translate.menuItem("Mesh (quad) from Connected Curves 2", this, "connectedCurvesToQuadMeshCommand2"));
      //toolsMenu.add(Translate.menuItem("Mesh (quad) from Connected Curves (Debug)", this, "connectedCurvesToQuadMeshCommandDebug"));
      //toolsMenu.add(Translate.menuItem("Mesh (quad) from Connected Curves High Detail", this, "connectedCurvesToQuadMeshHDCommand"));
      
    //toolsMenu.addSeparator();
    //toolsMenu.add(Translate.menuItem("createScriptObject", this, "createScriptObjectCommand"));
    //toolsMenu.add(Translate.menuItem("editScript", this, "actionPerformed"));
    //toolsMenu.add(scriptMenu = Translate.menu("scripts"));
    //rebuildScriptsMenu();
      
      toolsMenu.addSeparator();
      
      // Pair Distance Align
      toolsMenu.add(Translate.menuItem("Pair Distance Align", this, "pairDistanceAlign"));
      
      
      toolsMenu.add(Translate.menuItem("Regenerate Boolean Scene", this, "booleanSceneCommand"));
  }

  /** Rebuild the list of tool scripts in the Tools menu.  This should be called whenever a
      script has been added to or deleted from the Scripts/Tools directory on disk. */

  public void rebuildScriptsMenu()
  {
    scriptMenu.removeAll();
    addScriptsToMenu(scriptMenu, new File(ArmaRender.TOOL_SCRIPT_DIRECTORY));
  }

  private void addScriptsToMenu(BMenu menu, File dir)
  {
    String files[] = dir.list();
    if (files == null)
      return;
    Arrays.sort(files, Collator.getInstance(Translate.getLocale()));
    for (String file : files)
    {
      File f = new File(dir, file);
      if (f.isDirectory())
      {
        BMenu m = new BMenu(file);
        menu.add(m);
        addScriptsToMenu(m, f);
      }
      else
      {
        try
        {
          ScriptRunner.getLanguageForFilename(file);
          BMenuItem item = new BMenuItem(file.substring(0, file.lastIndexOf('.')));
          item.setActionCommand(f.getAbsolutePath());
          item.addEventLink(CommandEvent.class, this, "executeScriptCommand");
          menu.add(item);
        }
        catch (IllegalArgumentException ex)
        {
          // This file isn't a known scripting language.
        }
      }
    }
  }

  private void createAnimationMenu()
  {
    animationMenu = Translate.menu("animation");
    menubar.add(animationMenu);
    animationMenuItem = new BMenuItem [13];
    animationMenu.add(addTrackMenu = Translate.menu("addTrack"));
    addTrackMenu.add(positionTrackMenu = Translate.menu("positionTrack"));
    positionTrackMenu.add(Translate.menuItem("xyzOneTrack", this, "actionPerformed"));
    positionTrackMenu.add(Translate.menuItem("xyzThreeTracks", this, "actionPerformed"));
    positionTrackMenu.add(Translate.menuItem("proceduralTrack", this, "actionPerformed"));
    addTrackMenu.add(rotationTrackMenu = Translate.menu("rotationTrack"));
    rotationTrackMenu.add(Translate.menuItem("xyzOneTrack", this, "actionPerformed"));
    rotationTrackMenu.add(Translate.menuItem("xyzThreeTracks", this, "actionPerformed"));
    rotationTrackMenu.add(Translate.menuItem("quaternionTrack", this, "actionPerformed"));
    rotationTrackMenu.add(Translate.menuItem("proceduralTrack", this, "actionPerformed"));
    addTrackMenu.add(Translate.menuItem("poseTrack", this, "actionPerformed"));
    addTrackMenu.add(distortionMenu = Translate.menu("distortionTrack"));
    distortionMenu.add(Translate.menuItem("bendDistortion", this, "actionPerformed"));
    distortionMenu.add(Translate.menuItem("customDistortion", this, "actionPerformed"));
    distortionMenu.add(Translate.menuItem("scaleDistortion", this, "actionPerformed"));
    distortionMenu.add(Translate.menuItem("shatterDistortion", this, "actionPerformed"));
    distortionMenu.add(Translate.menuItem("twistDistortion", this, "actionPerformed"));
    distortionMenu.addSeparator();
    distortionMenu.add(Translate.menuItem("IKTrack", this, "actionPerformed"));
    distortionMenu.add(Translate.menuItem("skeletonShapeTrack", this, "actionPerformed"));
    addTrackMenu.add(Translate.menuItem("constraintTrack", this, "actionPerformed"));
    addTrackMenu.add(Translate.menuItem("visibilityTrack", this, "actionPerformed"));
    addTrackMenu.add(Translate.menuItem("textureTrack", this, "actionPerformed"));
    animationMenu.add(animationMenuItem[0] = Translate.menuItem("editTrack", theScore, "editSelectedTrack"));
    animationMenu.add(animationMenuItem[1] = Translate.menuItem("duplicateTracks", theScore, "duplicateSelectedTracks"));
    animationMenu.add(animationMenuItem[2] = Translate.menuItem("deleteTracks", theScore, "deleteSelectedTracks"));
    animationMenu.add(animationMenuItem[3] = Translate.menuItem("selectAllTracks", theScore, "selectAllTracks"));
    animationMenu.add(animationMenuItem[4] = Translate.menuItem("enableTracks", this, "actionPerformed"));
    animationMenu.add(animationMenuItem[5] = Translate.menuItem("disableTracks", this, "actionPerformed"));
    animationMenu.addSeparator();
    animationMenu.add(animationMenuItem[6] = Translate.menuItem("keyframe", theScore, "keyframeSelectedTracks"));
    animationMenu.add(animationMenuItem[7] = Translate.menuItem("keyframeModified", theScore, "keyframeModifiedTracks"));
    animationMenu.add(animationMenuItem[8] = Translate.menuItem("editKeyframe", theScore, "editSelectedKeyframe"));
    animationMenu.add(animationMenuItem[9] = Translate.menuItem("deleteSelectedKeyframes", theScore, "deleteSelectedKeyframes"));
    animationMenu.add(editKeyframeMenu = Translate.menu("bulkEditKeyframes"));
    editKeyframeMenu.add(Translate.menuItem("moveKeyframes", this, "actionPerformed"));
    editKeyframeMenu.add(Translate.menuItem("copyKeyframes", this, "actionPerformed"));
    editKeyframeMenu.add(Translate.menuItem("rescaleKeyframes", this, "actionPerformed"));
    editKeyframeMenu.add(Translate.menuItem("loopKeyframes", this, "actionPerformed"));
    editKeyframeMenu.add(Translate.menuItem("deleteKeyframes", this, "actionPerformed"));
    animationMenu.add(animationMenuItem[10] = Translate.menuItem("pathFromCurve", this, "actionPerformed"));
    animationMenu.add(animationMenuItem[11] = Translate.menuItem("bindToParent", this, "bindToParentCommand"));
    animationMenu.addSeparator();
    animationMenu.add(animationMenuItem[12] = Translate.menuItem("showScore", this, "actionPerformed"));
    animationMenu.add(Translate.menuItem("previewAnimation", this, "actionPerformed"));
    animationMenu.addSeparator();
    animationMenu.add(Translate.menuItem("forwardFrame", this, "actionPerformed"));
    animationMenu.add(Translate.menuItem("backFrame", this, "actionPerformed"));
    animationMenu.add(Translate.menuItem("jumpToTime", this, "jumpToTimeCommand"));
  }

  private void createSceneMenu()
  {
    BMenu displayMenu;

    sceneMenu = Translate.menu("scene");
    menubar.add(sceneMenu);
    sceneMenuItem = new BMenuItem [5];
    sceneMenu.add(Translate.menuItem("renderScene", this, "renderCommand"));
    sceneMenu.add(Translate.menuItem("renderImmediately", this, "actionPerformed"));
    sceneMenu.add(Translate.menuItem("industrialDesignDrawing", this, "industrialDesignDrawingCommand"));
      
    sceneMenu.addSeparator();
      
    sceneMenu.add(Translate.menuItem("Set Scale", this, "setGCodeExportScale"));
      
    sceneMenu.addSeparator();
    sceneMenu.add(displayMenu = Translate.menu("displayMode"));
    displayItem = new BCheckBoxMenuItem [6];
    displayMenu.add(displayItem[0] = Translate.checkboxMenuItem("wireframeDisplay", this, "displayModeCommand", theView[0].getRenderMode() == ViewerCanvas.RENDER_WIREFRAME));
    displayMenu.add(displayItem[1] = Translate.checkboxMenuItem("shadedDisplay", this, "displayModeCommand", theView[0].getRenderMode() == ViewerCanvas.RENDER_FLAT));
    displayMenu.add(displayItem[2] = Translate.checkboxMenuItem("smoothDisplay", this, "displayModeCommand", theView[0].getRenderMode() == ViewerCanvas.RENDER_SMOOTH));
    displayMenu.add(displayItem[3] = Translate.checkboxMenuItem("texturedDisplay", this, "displayModeCommand", theView[0].getRenderMode() == ViewerCanvas.RENDER_TEXTURED));
    displayMenu.add(displayItem[4] = Translate.checkboxMenuItem("transparentDisplay", this, "displayModeCommand", theView[0].getRenderMode() == ViewerCanvas.RENDER_TEXTURED));
    displayMenu.add(displayItem[5] = Translate.checkboxMenuItem("renderedDisplay", this, "displayModeCommand", theView[0].getRenderMode() == ViewerCanvas.RENDER_RENDERED));
    sceneMenu.add(sceneMenuItem[0] = Translate.menuItem("fourViews", this, "toggleViewsCommand"));
    sceneMenu.add(sceneMenuItem[1] = Translate.menuItem("hideObjectList", this, "actionPerformed"));
    sceneMenu.add(Translate.menuItem("grid", this, "setGridCommand"));
    sceneMenu.add(sceneMenuItem[2] = Translate.menuItem("showCoordinateAxes", this, "actionPerformed"));
    sceneMenu.add(sceneMenuItem[3] = Translate.menuItem("showTemplate", this, "actionPerformed"));
    sceneMenu.add(Translate.menuItem("setTemplate", this, "setTemplateCommand"));
    sceneMenu.add(Translate.menuItem("Toggle Background Gradient", this, "toggleBackgroundGradient"));
      
      // Theme -> Light / Dark
      
      
      //sceneMenu.add(Translate.menuItem("Toggle Selection Highlight", this, "toggleSelectionHighlight"));
      BMenuItem toggleSelectionHighlightMenuItem = Translate.menuItem("Toggle Selection Highlight", this, "toggleSelectionHighlight");
      toggleSelectionHighlightMenuItem.setName("scene_SelectionOutline");   // Name is used by event handler for ID
      toggleSelectionHighlightMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      toggleSelectionHighlightMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      sceneMenu.add(toggleSelectionHighlightMenuItem);
      
    sceneMenu.addSeparator();
    sceneMenu.add(sceneMenuItem[4] = Translate.menuItem("frameSelection", this, "actionPerformed"));
    sceneMenu.add(Translate.menuItem("frameScene", this, "actionPerformed"));
    sceneMenu.addSeparator();
    sceneMenu.add(Translate.menuItem("textures", this, "texturesCommand"));
    sceneMenu.add(Translate.menuItem("images", this, "actionPerformed"));
    sceneMenu.add(Translate.menuItem("environment", this, "environmentCommand")); // JDT Possibly Disable.
      
      sceneMenu.addSeparator();
      sceneMenu.add(Translate.menuItem("Rotate Scene", this, "rotateSceneCommand"));
  }

  /** Create the popup menu. */

  private void createPopupMenu()
  {
    popupMenu = new BPopupMenu();
    popupMenuItem = new BMenuItem [21]; // 16
    popupMenu.add(popupMenuItem[0] = Translate.menuItem("editObject", this, "editObjectCommand", null));
    popupMenu.add(popupMenuItem[1] = Translate.menuItem("objectLayout", this, "objectLayoutCommand", null));
    popupMenu.add(popupMenuItem[2] = Translate.menuItem("setTextureAndMaterial", this, "setTextureCommand", null));
    popupMenu.add(popupMenuItem[3] = Translate.menuItem("renameObject", this, "renameObjectCommand", null));
    popupMenu.add(popupMenuItem[4] = Translate.menuItem("convertToTriangle", this, "convertToTriangleCommand", null));
    popupMenu.add(popupMenuItem[5] = Translate.menuItem("selectChildren", this, "actionPerformed", null));
      
      // resize mesh
      
    popupMenu.addSeparator();
    popupMenu.add(popupMenuItem[6] = Translate.menuItem("hideSelection", this, "actionPerformed", null));
    popupMenu.add(popupMenuItem[7] = Translate.menuItem("showSelection", this, "actionPerformed", null));
      
      popupMenu.add(popupMenuItem[8] = Translate.menuItem("hideChildrenSelection", this, "actionPerformed")); // hideChildren
      popupMenu.add(popupMenuItem[9] = Translate.menuItem("showChildrenSelection", this, "actionPerformed")); // show children
      
    popupMenu.addSeparator();
    popupMenu.add(popupMenuItem[10] = Translate.menuItem("lockSelection", this, "actionPerformed"));
    popupMenu.add(popupMenuItem[11] = Translate.menuItem("unlockSelection", this, "actionPerformed"));
    popupMenu.addSeparator();
    popupMenu.add(popupMenuItem[12] = Translate.menuItem("cut", this, "cutCommand", null));
    popupMenu.add(popupMenuItem[13] = Translate.menuItem("copy", this, "copyCommand", null));
    popupMenu.add(popupMenuItem[14] = Translate.menuItem("paste", this, "pasteCommand", null));
      popupMenu.add(popupMenuItem[15] = Translate.menuItem("Paste as child", this, "pasteAsChildCommand"));
    popupMenu.add(popupMenuItem[16] = Translate.menuItem("clear", this, "clearCommand", null));
    popupMenu.addSeparator();
    popupMenu.add(popupMenuItem[17] = Translate.menuItem("zoomToObject", this, "zoomToObject", null));
      popupMenu.add(popupMenuItem[18] = Translate.menuItem("Show In Object List", this, "showInList", null));
      
      
    popupMenu.add(popupMenuItem[19] = Translate.menuItem("alignObjectToCollided", this, "alignObjectToCollided", null));
      
      popupMenu.addSeparator();
      popupMenu.add(popupMenuItem[20] = Translate.menuItem("getDebugInfo", this, "getDebugInfoCommand", null));
      
  }
    
    /**
     * createSolidsMenu
     *
     * Description: Solids modelling.
     */
    private void createSolidsMenu(){
        solidsMenu = Translate.menu("Solids");
        menubar.add(solidsMenu);
        //solidsMenuItem = new BMenuItem [13];
        
        solidsResolvedViewMenuItem = Translate.menuItem("Resolved View", this, "setSolidsResolvedView");
        solidsResolvedViewMenuItem.setName("solids_setSolidsResolvedView");   // Name is used by event handler for ID
        solidsResolvedViewMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
        solidsResolvedViewMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
        solidsMenu.add(solidsResolvedViewMenuItem);
        
        //solidsMenu.add(Translate.menuItem("Edit View", this, "setSolidsEditView"));
        BMenuItem solidsEditViewMenuItem = Translate.menuItem("Edit View", this, "setSolidsEditView");
        solidsEditViewMenuItem.setName("solids_setSolidsEditView");   // Name is used by event handler for ID
        solidsEditViewMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
        solidsEditViewMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
        solidsMenu.add(solidsEditViewMenuItem);
        
        
        solidsMenu.addSeparator();
        solidsMenu.add(Translate.menuItem("Add to Union", this, "solidsAddUnion"));
        solidsMenu.add(Translate.menuItem("Subtract from", this, "solidsSubtract"));
    }
    
    /**
     * mouseEnteredEvent
     *  Description:
     */
    public void mouseEnteredEvent(MouseEnteredEvent ev){
        // solidsResolvedViewMenuItem
        java.lang.Object source = ev.getSource();
        if(source instanceof BMenuItem){ // && ((BMenuItem)source) == solidsResolvedViewMenuItem
            
            BMenuItem menuItem = ((BMenuItem)source);
            String nameID = menuItem.getName();
            //System.out.println(" menu name: " + nameID);
            
            int offset = 0;
            
            // Offset more on submenus.
            WidgetContainer parent = menuItem.getParent();
            if( parent != null){
                WidgetContainer parent2 = parent.getParent();
                if(parent2 != null){
                    //System.out.println("       Parent2: " + parent2.getClass().getName() + " name: " + parent2.getName()  );
                    //System.out.println("       Parent2: " );
                    WidgetContainer parent3 = parent2.getParent();
                    if(parent3 != null){
                        //System.out.println("          Parent3: " + parent3.getClass().getName() + " name: " + parent3.getName()  );
                        //System.out.println("       Parent3: " );
                        if(parent3 instanceof BMenuBar){
                            offset = 150;
                        }
                    }
                }
            }
            
            
            // If an info graphic image exists load the dialog.
            String text = menuItem.getText();
            //System.out.println(" text " + text);
            
            if(infoGraphicDialog == null){
                infoGraphicDialog = new JDialog();
                infoGraphicDialog.setLayout(new FlowLayout());
                infoGraphicDialog.setUndecorated(true);
                infoGraphicDialog.setType(Window.Type.POPUP);
                infoGraphicDialog.setFocusableWindowState(false);
                infoGraphicDialog.setPreferredSize(new Dimension(400, 433));
            }
            
            
            ImageIcon titleImage = new ImageIcon(getClass().getResource("/armarender/infographic/title_400.png"));
            JLabel label = new JLabel(titleImage);
            label.setVerticalAlignment(JLabel.TOP);
            label.setVerticalTextPosition(JLabel.TOP);
            infoGraphicDialog.add(label);
            
            if(getClass().getResource("/armarender/infographic/"+nameID+".png") != null){
            ImageIcon illustrationImage = new ImageIcon(getClass().getResource("/armarender/infographic/"+nameID+".png"));
            if(illustrationImage != null){
            JLabel illustrationLabel = new JLabel(illustrationImage);
            //illustrationLabel.setVerticalAlignment(JLabel.TOP);
            //illustrationLabel.setVerticalTextPosition(JLabel.TOP);
            infoGraphicDialog.add(illustrationLabel);
            }
            }
            
            //JTextField descriptionField = new JTextField(20);
            //descriptionField.setText("Description.");
            //infoGraphicDialog.add(descriptionField);
            
    
            double x = 0;
            double y = 0;
            int width = 0;
            
            java.awt.Rectangle windowBoundsRect = this.getBounds();
            java.awt.Rectangle menuItemBoundsRect = menuItem.getBounds();
            java.awt.Rectangle parentBoundsRect = menuItem.getParent().getBounds();
            java.awt.Rectangle parent2BoundsRect = menuItem.getParent().getParent().getBounds();
            
            x = windowBoundsRect.getX() + menuItemBoundsRect.getX() + parentBoundsRect.getX() + parent2BoundsRect.getX();
            y = windowBoundsRect.getY() + 24 + 4; // menuItemBoundsRect.getY() + parentBoundsRect.getY() +
            width = (int)menuItemBoundsRect.getWidth();
            
            //System.out.println("   boundsRect " + x + " " + y  );
            
            infoGraphicDialog.setLocation((int)x + width + 150 + offset, (int)y);
            
            infoGraphicDialog.pack();
            infoGraphicDialog.setVisible(true);
        }
    }
    
    /**
     * mouseExitedEvent
     */
    public void mouseExitedEvent(MouseExitedEvent ev){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
    }
    
    
    public void setSolidsResolvedView(){
     
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
    }
    
    public void setSolidsEditView(){
        
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
    }
    
    /**
     * solidsAddUnion
     *
     */
    public void solidsAddUnion(){
        
        // selected
        Object selectedObjects[] = itemTree.getSelectedObjects();
        if(selectedObjects.length > 0){
            ObjectInfo selectedInfo = (ObjectInfo) selectedObjects[0];
            
            Scene theScene = getScene();
            ObjectInfo objInfo = new ObjectInfo(new Union(), new CoordinateSystem(), "Union");
            UndoRecord undo = new UndoRecord(this, false);
            int sel[] = getSelectedIndices();
            
            selectedInfo.addChild(objInfo, 0);
            objInfo.setParent(selectedInfo);
            
            addObject(objInfo, undo);
            
            undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
            setUndoRecord(undo);
            setSelection(theScene.getNumObjects()-1);
            
            // refresh menu
            
            
        } else {
            // Error
        }
    }
    
    public void solidsSubtract(){
        
    }

  // JDT
  private void createLayoutMenu(){
      layoutMenu = Translate.menu("CAM");
	  menubar.add(layoutMenu);

	  layoutMenuItem = new BMenuItem [13];
	  //layoutMenu.add(layoutModelView = Translate.menu("Modling View", this, "setModelingView"));
	  //layoutMenu.add(Translate.menuItem("Modeling View", this, "setModelingView"));
      BMenuItem camModelingViewMenuItem = Translate.menuItem("Modeling View", this, "setModelingView");
      camModelingViewMenuItem.setName("cam_setModelingView");   // Name is used by event handler for ID
      camModelingViewMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      camModelingViewMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //layoutMenu.add(camModelingViewMenuItem);
      
	  //layoutMenu.add(layoutLayView = Translate.menu("Layout View"));
	  //layoutMenu.add(Translate.menuItem("Layout View", this, "setLayoutView"));
      BMenuItem camLayoutViewMenuItem = Translate.menuItem("Layout View", this, "setLayoutView");
      camLayoutViewMenuItem.setName("cam_setLayoutView");   // Name is used by event handler for ID
      camLayoutViewMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      camLayoutViewMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //layoutMenu.add(camLayoutViewMenuItem);
      
      // Auto Layout Plate
      BMenuItem camAutoLayoutPlateMenuItem = Translate.menuItem("Auto Layout Plate", this, "autoLayoutPlate");
      camAutoLayoutPlateMenuItem.setName("cam_autoLayoutPlate");   // Name is used by event handler for ID
      camAutoLayoutPlateMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      camAutoLayoutPlateMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //layoutMenu.add(camAutoLayoutPlateMenuItem);
      
      //layoutMenu.addSeparator();
      //layoutMenu.add(Translate.menuItem("Reset Object Layout", this, "resetLayoutView"));
      
      //layoutMenu.add(Translate.menuItem("Tube Layout View", this, "setTubeLayoutView"));
      //layoutMenu.add(Translate.menuItem("DEBUG", this, "debug"));
      
	  //layoutMenu.addSeparator();
	  //layoutMenu.add(Translate.menuItem("Enable Poly", this, "setGCodeEnablePoly"));
	  //layoutMenu.add(Translate.menuItem("Disable Poly", this, "setGCodeDisablePoly"));
      //layoutMenu.addSeparator();
	  //layoutMenu.add(Translate.menuItem("Set Selected Poly Order", this, "setGCodePolyOrder"));
      
      
      //layoutMenu.add(Translate.menuItem("Set Order by Size", this, "setGCodePolyOrdersBySize"));
      BMenuItem camOrderBySizeMenuItem = Translate.menuItem("Set Order by Size", this, "setGCodePolyOrdersBySize");
      camOrderBySizeMenuItem.setName("cam_setGCodePolyOrdersBySize");   // Name is used by event handler for ID
      camOrderBySizeMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      camOrderBySizeMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //layoutMenu.add(camOrderBySizeMenuItem);
      
      
	  //layoutMenu.add(Translate.menuItem("Set Selected Poly Depth", this, "setGCodePolyDepth"));
      //layoutMenu.add(Translate.menuItem("Set Selected Point Offset", this, "setGCodePointOffset"));
      //layoutMenu.add(Translate.menuItem("Reverse Selected Order", this, "setGCodeReverseOrder"));
	  //layoutMenu.addSeparator();
	  layoutMenu.add(Translate.menuItem("Set Scale", this, "setGCodeExportScale"));
	  layoutMenu.addSeparator();
	  // exportGCode
	  //layoutMenu.add(Translate.menuItem("Export Poly Table GCode (Children Grouped)", this, "exportGroupGCode"));
      //layoutMenu.add(Translate.menuItem("Export Poly Table GCode (All)", this, "exportAllGCode"));
      
      
      //BMenuItem camExportTubeGCodeMenuItem = Translate.menuItem("Export Tube Notch (X) GCode", this, "exportTubeGCode");
      //camExportTubeGCodeMenuItem.setName("cam_exportTubeGCode");   // Name is used by event handler for ID
      //camExportTubeGCodeMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      //camExportTubeGCodeMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //layoutMenu.add(camExportTubeGCodeMenuItem);
      
      //layoutMenu.add(Translate.menuItem("Export Tube Bend (X) GCode", this, "exportTubeBendGCode"));
      
      
      // Export
      //layoutMenu.add(Translate.menuItem("Export Arc Tube Notch GCode", this, "exportArcTubeNotchGCode"));
      BMenuItem camExportArcTubeNotchGCodeMenuItem = Translate.menuItem("Export Arc Tube Notch GCode", this, "exportArcTubeNotchGCode");
      camExportArcTubeNotchGCodeMenuItem.setName("cam_exportArcTubeNotchGCode");   // Name is used by event handler for ID
      camExportArcTubeNotchGCodeMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      camExportArcTubeNotchGCodeMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //layoutMenu.add(camExportArcTubeNotchGCodeMenuItem);
      
      //BMenuItem camExportArcTubeNotchGCodeMenuItem2 = Translate.menuItem("Export Arc Tube Notch GCode 2", this, "exportArcTubeNotchGCode2");
      //camExportArcTubeNotchGCodeMenuItem2.setName("cam_exportArcTubeGCode");   // Name is used by event handler for ID
      //camExportArcTubeNotchGCodeMenuItem2.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      //camExportArcTubeNotchGCodeMenuItem2.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //layoutMenu.add(camExportArcTubeNotchGCodeMenuItem2);
      
      
      
      //layoutMenu.add(Translate.menuItem("Export Arc Tube Bend GCode", this, "exportArcTubeBendGCode"));
      BMenuItem camExportArcTubeBendGCodeMenuItem = Translate.menuItem("Export Arc Tube Bend GCode", this, "exportArcTubeBendGCode");
      camExportArcTubeBendGCodeMenuItem.setName("cam_exportArcTubeBendGCode");   // Name is used by event handler for ID
      camExportArcTubeBendGCodeMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      camExportArcTubeBendGCodeMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //layoutMenu.add(camExportArcTubeBendGCodeMenuItem);
      
      
      //
      BMenuItem camExportArcTubeNotchPDFMenuItem = Translate.menuItem("Export Arc Tube Notch PDF", this, "exportArcTubeNotchPDF");
      camExportArcTubeNotchPDFMenuItem.setName("cam_exportArcTubeNotchPDF");   // Name is used by event handler for ID
      camExportArcTubeNotchPDFMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      camExportArcTubeNotchPDFMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //layoutMenu.add(camExportArcTubeNotchPDFMenuItem);
      
      
      BMenuItem camExportArcTubeBendPDFMenuItem = Translate.menuItem("Export Arc Tube Bend PDF", this, "exportArcTubeBendPDF");
      camExportArcTubeBendPDFMenuItem.setName("cam_exportArcTubeBendPDF");   // Name is used by event handler for ID
      camExportArcTubeBendPDFMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      camExportArcTubeBendPDFMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //layoutMenu.add(camExportArcTubeBendPDFMenuItem);
      
      // Plate PDF
      BMenuItem camExportPlatePDFMenuItem = Translate.menuItem("Export Plate PDF", this, "exportPlatePDF");
      camExportPlatePDFMenuItem.setName("cam_exportPlatePDF");   // Name is used by event handler for ID
      camExportPlatePDFMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      camExportPlatePDFMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //layoutMenu.add(camExportPlatePDFMenuItem);
      

     
      
      
      
      layoutMenu.add(Translate.menuItem("Export 3 Axis Voids (Top Down) Mesh GCode", this, "export3dGCode2"));
      //layoutMenu.add(Translate.menuItem("Export DXF", this, "exportLayoutDXF"));
      //layoutMenu.add(Translate.menuItem("Export OBJ", this, "exportOBJ"));
      
      layoutMenu.addSeparator();
      
      // Nesting Tube


	//layoutMenu.add(Translate.menuItem("Export 3 Axis (Top Down) Mesh GCode", this, "export3dGCode"));
      BMenuItem camMillGCodeMenuItem = Translate.menuItem("Export 3-Axis (Top Down, Layered) GCode", this, "export3dGCode"); // Sliced
      camMillGCodeMenuItem.setName("cam_export3dGCode"); // Name is used by event handler for ID
      camMillGCodeMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
      camMillGCodeMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
      //layoutMenu.add(camMillGCodeMenuItem);
      layoutMenu.add(camMillGCodeMenuItem);
      
      
      //layoutMenu.add(Translate.menuItem("Export 3 Axis Voids (Top Down) Mesh GCode", this, "export3dGCode2"));
      layoutMenu.add(Translate.menuItem("Export 3-Axis Voids (Top Down, Layered) GCode", this, "export3dGCode2"));
      
      // TODO:
      // add Roughing
      
      layoutMenu.add(Translate.menuItem("Export 5-Axis Roughing (Top Down) GCode", this, "printFiveAxisRoughingGCode")); // printFiveAxisGCode
      
      layoutMenu.add(Translate.menuItem("Export 5-Axis Finishing (Top Down) GCode", this, "printFiveAxisFinishingGCode")); // printFiveAxisGCode
      

      
      
      layoutMenu.addSeparator();
      //layoutMenu.add(Translate.menuItem("3D Print GCode", this, "print3DGCode"));
      
      
      
      //BMenu fiveAxisMenu;
      //layoutMenu.add(fiveAxisMenu = Translate.menu("fiveAxisMenu"));
      //fiveAxisMenu.add(Translate.menuItem("5 Axis 3D GCode", this, "printFiveAxisGCode")); // printFiveAxisGCode
      
      
      //layoutMenu.add(Translate.menuItem("Export GCode Mesh", this, "exportGCodeMesh"));
      
      
      layoutMenu.addSeparator();
      
      layoutMenu.add(Translate.menuItem("Control CNC Machine", this, "controlCNCMachine"));
      
  }
   
    /**
    * createFEAMenu
    *
    */ 
    private void createFEAMenu(){
        feaMenu = Translate.menu("FEA");
        menubar.add(feaMenu);
        feaMenuItem = new BMenuItem [13];
        
        //feaMenu.add(Translate.menuItem("Run Simulation", this, "runCrashSimulation"));
        BMenuItem feaRunSimMenuItem = Translate.menuItem("Run Simulation", this, "runCrashSimulation");
        feaRunSimMenuItem.setName("fea_runCrashSimulation"); // Name is used by event handler for ID
        feaRunSimMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
        feaRunSimMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
        feaMenu.add(feaRunSimMenuItem);
        
        
        feaMenu.addSeparator(); 
        feaMenu.add(Translate.menuItem("Set Object Structure", this, "setObjectStructure"));
        feaMenu.addSeparator();
        feaMenu.add(Translate.menuItem("Select Structure Objects", this, "copyStructureObjects"));
        feaMenu.add(Translate.menuItem("Generate Structure Mesh", this, "generateStructureMesh"));
        feaMenu.addSeparator();
        
        
        //feaMenu.add(Translate.menuItem("Even Mesh Subdivision", this, "evenMesh"));
        BMenuItem feaMeshSubdivideMenuItem = Translate.menuItem("Even Mesh Subdivision", this, "evenMesh");
        feaMeshSubdivideMenuItem.setName("cam_evenMesh"); // Name is used by event handler for ID
        feaMeshSubdivideMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
        feaMeshSubdivideMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
        feaMenu.add(feaMeshSubdivideMenuItem);
        // ***
        
        //feaMenu.addSeparator();
        //feaMenu.add(Translate.menuItem("BeamNG Join Selected Nodes", this, "beamNGCreateBeam"));
        //feaMenu.add(Translate.menuItem("Export BeamNG Vehicle", this, "exportBeamNGVehicle"));
        
        feaMenu.addSeparator();
        // Cloth
        BMenuItem feaClothSimulationMenuItem = Translate.menuItem("Cloth Simulation", this, "clothSimulation");
        feaClothSimulationMenuItem.setName("fea_clothSimulation"); // Name is used by event handler for ID
        feaClothSimulationMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
        feaClothSimulationMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
        feaMenu.add(feaClothSimulationMenuItem);
        
        BMenuItem arcTubeSoftBodyMenuItem = Translate.menuItem("Arc Tube Soft Body", this, "arcTubeSoftBody");
        feaMenu.add(arcTubeSoftBodyMenuItem);
        
    }
    
    
    private void createCFDMenu(){
        cfdMenu = Translate.menu("CFD");
        menubar.add(cfdMenu);
        
        cfdMenuItem = new BMenuItem [13];
        
        BMenuItem cfdRunMenuItem = Translate.menuItem("Run", this, "runCFD");
        cfdRunMenuItem.setName("cfd_run"); // Name is used by event handler for ID
        cfdRunMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
        cfdRunMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
        cfdMenu.add(cfdRunMenuItem);
        
        //cfdMenu.add(Translate.menuItem("Run", this, "runCFD"));
        
        
        cfdMenu.add(Translate.menuItem("Stop", this, "stopCFD"));
        cfdMenu.add(Translate.menuItem("Calibrate", this, "calibrateCFD"));
        
        cfdMenu.addSeparator();
        
        cfdMenu.add(Translate.menuItem("Frontal Area", this, "frontalArea"));
        //cfdMenu.add(Translate.menuItem("Copy Structure Objects", this, "copyStructureObjects"));
        //cfdMenu.add(Translate.menuItem("Generate Structure Mesh", this, "generateStructureMesh"));
        //cfdMenu.addSeparator();
        //cfdMenu.add(Translate.menuItem("Even Mesh", this, "evenMesh"));
        
        //cfdMenu.addSeparator();
        //cfdMenu.add(Translate.menuItem("BeamNG Join Selected Nodes", this, "beamNGCreateBeam"));
        //cfdMenu.add(Translate.menuItem("Export BeamNG Vehicle", this, "exportBeamNGVehicle"));
    }


  /** Display the popup menu. */

  public void showPopupMenu(Widget w, int x, int y)
  {
    Object sel[] = itemTree.getSelectedObjects();
    boolean canConvert, canSetTexture, canHide, canShow, canLock, canUnlock, hasChildren;
      boolean canShowChildren, canHideChildren;
    ObjectInfo info;
    Object3D obj;

    canConvert = canSetTexture = (sel.length > 0);
    canHide = canShow = canLock = canUnlock = hasChildren = false;
      canShowChildren = canHideChildren = false;
    for (int i = 0; i < sel.length; i++)
      {
        info = (ObjectInfo) sel[i];
        obj = info.getObject();
        if (obj.canConvertToTriangleMesh() == Object3D.CANT_CONVERT)
          canConvert = false;
        if (!obj.canSetTexture())
          canSetTexture = false;
        if (info.getChildren().length > 0)
          hasChildren = true;
        if (info.isVisible())
          canHide = true;
        else
          canShow = true;
        if (info.isLocked())
          canUnlock = true;
        else
          canLock = true;
          
        if(info.isChildrenHiddenWhenHidden()){
            canHideChildren = true;
        } else {
            canShowChildren = true;
        }
      }
    if (sel.length == 0)
      {
        for (int i = 0; i < popupMenuItem.length; i++)
          popupMenuItem[i].setEnabled(false);
      }
    else
      {
        obj = ((ObjectInfo) sel[0]).getObject();
        popupMenuItem[0].setEnabled(sel.length == 1 && obj.isEditable()); // Edit Object
        popupMenuItem[1].setEnabled(true); // Object Layout
        popupMenuItem[2].setEnabled(canSetTexture); // Set Texture
        popupMenuItem[3].setEnabled(sel.length == 1); // Rename Object
        popupMenuItem[4].setEnabled(canConvert); // Convert to Triangle Mesh
        popupMenuItem[5].setEnabled(sel.length == 1 && hasChildren); // Select Children
        popupMenuItem[6].setEnabled(canHide); // Hide Selection
        popupMenuItem[7].setEnabled(canShow); // Show Selection
          
          popupMenuItem[8].setEnabled(canShowChildren); // Show Selection Children
          popupMenuItem[9].setEnabled(canHideChildren); // Hide Selection Children
          
        popupMenuItem[10].setEnabled(canLock); // Lock Selection
        popupMenuItem[11].setEnabled(canUnlock); // Unlock Selection
        popupMenuItem[12].setEnabled(sel.length > 0); // Cut
        popupMenuItem[13].setEnabled(sel.length > 0); // Copy
        popupMenuItem[14].setEnabled(sel.length > 0); // Clear
      }
    popupMenuItem[12].setEnabled(ArmaRender.getClipboardSize() > 0); // Paste
    popupMenu.show(w, x, y);
  }

  /** Get the File menu. */

  public BMenu getFileMenu()
  {
    return fileMenu;
  }

  /** Get the Edit menu. */

  public BMenu getEditMenu()
  {
    return editMenu;
  }

  /** Get the Scene menu. */

  public BMenu getSceneMenu()
  {
    return sceneMenu;
  }

  /** Get the Object menu. */

  public BMenu getObjectMenu()
  {
    return objectMenu;
  }

  /** Get the Animation menu. */

  public BMenu getAnimationMenu()
  {
    return animationMenu;
  }

  /** Get the Tools menu. */

  public BMenu getToolsMenu()
  {
    return toolsMenu;
  }

  /** Get the popup menu. */

  public BPopupMenu getPopupMenu()
  {
    return popupMenu;
  }

  /** Get the DockingContainer which holds DockableWidgets on one side of the window. */

  public DockingContainer getDockingContainer(BTabbedPane.TabPosition position)
  {
    for (int i = 0; i < dock.length; i++)
      if (dock[i].getTabPosition() == position)
        return dock[i];
    return null; // should be impossible
  }

  /** Set the wait cursor on everything in this window. */

  public void setWaitCursor()
  {
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
  }

  /** Remove the wait cursor from everything in this window. */

  public void clearWaitCursor()
  {
    setCursor(Cursor.getDefaultCursor());
  }

  public Dimension getMinimumSize()
  {
    return new Dimension(100, 100);
  }

  /* EditingWindow methods. */

  /** This method is called to close the window.  If the Scene has been modified, it first
      gives the user a chance to save the Scene, or to cancel.  If the user cancels it, the
      method returns false.  Otherwise, it closes the window and returns true. */

  public boolean confirmClose()
  {
    if (modified)
    {
      String name = theScene.getName();
      if (name == null)
        name = "Untitled";
      BStandardDialog dlg = new BStandardDialog("", Translate.text("checkSaveChanges", name), BStandardDialog.QUESTION);
      String options[] = new String [] {Translate.text("button.save"), Translate.text("button.dontSave"), Translate.text("button.cancel")};
      int choice = dlg.showOptionDialog(this, options, options[0]);
      if (choice == 0)
      {
        saveCommand();
        if (modified)
          return false;
      }
      if (choice == 2)
        return false;
    }
    dispose();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(keyEventHandler);
    return true;
  }

  /** Set the selected EditingTool for this window. */

  public void setTool(EditingTool tool)
  {
    for (int i = 0; i < theView.length; i++)
      theView[i].setTool(tool);
  }

  /** Set the help text displayed at the bottom of the window. */

  public void setHelpText(String text)
  {
    helpText.setText(text);
  }

  /** Get the Frame corresponding to this window.  (Because LayoutWindow is a Frame,
      it simply returns itself.) */

  public BFrame getFrame()
  {
    return this;
  }

  /** Update the images displayed in all of the viewport. */

  public void updateImage()
  {
    if (numViewsShown == 1)
    {
      theView[currentView].copyOrientationFromCamera();
      theView[currentView].repaint();
    }
    else
      for (int i = 0; i < numViewsShown; i++)
      {
        theView[i].copyOrientationFromCamera();
        theView[i].repaint();
      }
  }
    
    // JDT
    public void updateTree(){
        rebuildItemList();
        //itemTree.repaint();
    }

  /**
   *
   * Update the state of all menu items.
   *
   */
  public void updateMenus()
  {
    Object sel[] = itemTree.getSelectedObjects();
    int numSelObjects = sel.length;
    Track selTrack[] = theScore.getSelectedTracks();
    int numSelTracks = selTrack.length;
    int numSelKeyframes = theScore.getSelectedKeyframes().length;
    ViewerCanvas view = theView[currentView];
    boolean canConvert, canSetTexture;
    boolean curve, noncurve, enable, disable, hasChildren, hasParent;
    ObjectInfo info;
    Object3D obj;
    int i;
    canConvert = canSetTexture = (numSelObjects > 0);
    curve = noncurve = enable = disable = hasChildren = hasParent = false;
    for (i = 0; i < numSelObjects; i++)
    {
      info = (ObjectInfo) sel[i];
      obj = info.getObject();
      if (obj instanceof Curve && !(obj instanceof Tube))
        curve = true;
      else
        noncurve = true;
      if (obj.canConvertToTriangleMesh() == Object3D.CANT_CONVERT)
        canConvert = false;
      if (!obj.canSetTexture())
        canSetTexture = false;
      if (info.getChildren().length > 0)
        hasChildren = true;
      if (info.getParent() != null)
        hasParent = true;
    }
    for (i = 0; i < numSelTracks; i++)
    {
      if (selTrack[i].isEnabled())
        disable = true;
      else
        enable = true;
    }

    fileMenuItem[0].setEnabled(modified);
    editMenuItem[0].setEnabled(undoStack.canUndo()); // Undo
    editMenuItem[1].setEnabled(undoStack.canRedo()); // Redo
    editMenuItem[2].setEnabled(numSelObjects > 0); // Cut
    editMenuItem[3].setEnabled(numSelObjects > 0); // Copy
    editMenuItem[4].setEnabled(ArmaRender.getClipboardSize() > 0); // Paste
    editMenuItem[5].setEnabled(numSelObjects > 0); // Clear
    editMenuItem[6].setEnabled(hasChildren); // Select Children
    editMenuItem[7].setEnabled(numSelObjects > 0); // Make Live Duplicates
    editMenuItem[8].setEnabled(numSelObjects > 0); // Sever Duplicates
    if (numSelObjects == 0)
    {
      for (i = 0; i < objectMenuItem.length; i++)
        objectMenuItem[i].setEnabled(false);
    }
    else
    {
      obj = ((ObjectInfo) sel[0]).getObject();
      objectMenuItem[0].setEnabled(numSelObjects == 1 && obj.isEditable()); // Edit Object
      objectMenuItem[1].setEnabled(true); // Object Layout
      objectMenuItem[2].setEnabled(true); // Transform Object
      objectMenuItem[3].setEnabled(numSelObjects > 0); // Align Objects
      objectMenuItem[4].setEnabled(canSetTexture); // Set Texture
      objectMenuItem[5].setEnabled(sel.length == 1); // Rename Object
      objectMenuItem[6].setEnabled(canConvert && sel.length == 1); // Convert to Triangle Mesh
      objectMenuItem[7].setEnabled(sel.length == 1 && ((ObjectInfo) sel[0]).getObject().canConvertToActor()); // Convert to Actor
      objectMenuItem[8].setEnabled(true); // Hide Selection
      objectMenuItem[9].setEnabled(true); // Show Selection
        objectMenuItem[10].setEnabled(true); // hide children
        objectMenuItem[11].setEnabled(true); // show children
      objectMenuItem[12].setEnabled(true); // Lock Selection
      objectMenuItem[13].setEnabled(true); // Unlock Selection
    }
    /*
    animationMenuItem[0].setEnabled(numSelTracks == 1); // Edit Track
    animationMenuItem[1].setEnabled(numSelTracks > 0); // Duplicate Tracks
    animationMenuItem[2].setEnabled(numSelTracks > 0); // Delete Tracks
    animationMenuItem[3].setEnabled(numSelObjects > 0); // Select All Tracks
    animationMenuItem[4].setEnabled(enable); // Enable Tracks
    animationMenuItem[5].setEnabled(disable); // Disable Tracks
    animationMenuItem[6].setEnabled(numSelTracks > 0); // Keyframe Selected Tracks
    animationMenuItem[7].setEnabled(numSelObjects > 0); // Keyframe Modified Tracks
    animationMenuItem[8].setEnabled(numSelKeyframes == 1); // Edit Keyframe
    animationMenuItem[9].setEnabled(numSelKeyframes > 0); // Delete Selected Keyframes
    animationMenuItem[10].setEnabled(curve && noncurve); // Set Path From Curve
    animationMenuItem[11].setEnabled(hasParent); // Bind to Parent Skeleton
    animationMenuItem[12].setText(Translate.text(theScore.getBounds().height == 0 || theScore.getBounds().width == 0 ? "menu.showScore" : "menu.hideScore"));
    addTrackMenu.setEnabled(numSelObjects > 0);
    */
      
    //distortionMenu.setEnabled(sel.length > 0);
    sceneMenuItem[1].setText(Translate.text(itemTreeScroller.getBounds().width == 0 || itemTreeScroller.getBounds().height == 0 ? "menu.showObjectList" : "menu.hideObjectList"));
    sceneMenuItem[2].setText(Translate.text(view.getShowAxes() ? "menu.hideCoordinateAxes" : "menu.showCoordinateAxes"));
    sceneMenuItem[3].setEnabled(view.getTemplateImage() != null); // Show template
    sceneMenuItem[3].setText(Translate.text(view.getTemplateShown() ? "menu.hideTemplate" : "menu.showTemplate"));
    sceneMenuItem[4].setEnabled(sel.length > 0); // Frame Selection With Camera
    displayItem[0].setState(view.getRenderMode() == ViewerCanvas.RENDER_WIREFRAME);
    displayItem[1].setState(view.getRenderMode() == ViewerCanvas.RENDER_FLAT);
    displayItem[2].setState(view.getRenderMode() == ViewerCanvas.RENDER_SMOOTH);
    displayItem[3].setState(view.getRenderMode() == ViewerCanvas.RENDER_TEXTURED);
    displayItem[4].setState(view.getRenderMode() == ViewerCanvas.RENDER_TRANSPARENT);
    displayItem[5].setState(view.getRenderMode() == ViewerCanvas.RENDER_RENDERED);
  }

  /** Set the UndoRecord which will be executed if the user chooses Undo from the Edit menu. */

  public void setUndoRecord(UndoRecord command)
  {
    undoStack.addRecord(command);
    boolean modified = false;
    for (int c : command.getCommands())
      if (c != UndoRecord.SET_SCENE_SELECTION)
        modified = true;
    if (modified)
      setModified();
    else
      dispatchSceneChangedEvent();
    updateMenus();
  }

  /** Set whether the scene has been modified since it was last saved. */

  public void setModified()
  {
    modified = true;
    for (ViewerCanvas view : theView)
      view.viewChanged(false);
    dispatchSceneChangedEvent();
  }

  /** Determine whether the scene has been modified since it was last saved. */

  public boolean isModified()
  {
    return modified;
  }

  /** Cause a SceneChangedEvent to be dispatched to this window's listeners. */

  private void dispatchSceneChangedEvent()
  {
    if (sceneChangePending)
      return; // There's already a Runnable on the event queue waiting to dispatch a SceneChangedEvent.
    sceneChangePending = true;
    EventQueue.invokeLater(new Runnable()
    {
      public void run()
      {
        sceneChangePending = false;
        dispatchEvent(sceneChangedEvent);
      }
    });
  }
    
    // JDT
    public int addObjectL(Object3D obj){
        return theScene.addObjectI(new ObjectInfo(obj, new CoordinateSystem(), ""), null);
    }

    public void removeObjectL(Object3D obj){
        // ObjectInfo info = theScene.getObject(which);
        
        //FluidPointObject fluidPoint = (FluidPointObject)obj;
      
        //theScene.removeObject(fluidPoint.getId(), null);
        theScene.removeObjectL( obj );
    }
    
  /** Add a new object to the scene.  If undo is not null,
      appropriate commands will be added to it to undo this operation. */

  public void addObject(Object3D obj, CoordinateSystem coords, String name, UndoRecord undo)
  {
    addObject(new ObjectInfo(obj, coords, name), undo);
  }

  /** Add a new object to the scene.  If undo is not null,
      appropriate commands will be added to it to undo this operation. */

  public void addObject(ObjectInfo info, UndoRecord undo)
  {
    theScene.addObject(info, undo);
    itemTree.addElement(new ObjectTreeElement(info, itemTree));
    for (int i = 0; i < theView.length ; i++)
      theView[i].rebuildCameraList();
    theScore.rebuildList();
  }

  /** Add a new object to the scene.  If undo is not null,
      appropriate commands will be added to it to undo this operation. */

  public void addObject(ObjectInfo info, int index, UndoRecord undo)
  {
    theScene.addObject(info, index, undo);
    itemTree.addElement(new ObjectTreeElement(info, itemTree), index);
    for (int i = 0; i < theView.length ; i++)
      theView[i].rebuildCameraList();
    theScore.rebuildList();
  }

  /** Remove an object from the scene.  If undo is not null,
      appropriate commands will be added to it to undo this operation. */

  public void removeObject(int which, UndoRecord undo)
  {
    ObjectInfo info = theScene.getObject(which);
    ObjectInfo parent = info.getParent();
    int childIndex = -1;
    if (parent != null)
      for (int i = 0; i < parent.getChildren().length; i++)
        if (parent.getChildren()[i] == info)
          childIndex = i;
    itemTree.removeObject(info);
    if (childIndex > -1 && info.getParent() == null)
      undo.addCommandAtBeginning(UndoRecord.ADD_TO_GROUP, new Object [] {parent, info, new Integer(childIndex)});
    theScene.removeObject(which, undo);
    for (int i = 0; i < theView.length ; i++)
    {
      if (theView[i].getBoundCamera() == info)
        theView[i].setOrientation(ViewerCanvas.VIEW_OTHER);
      theView[i].rebuildCameraList();
    }
    theScore.rebuildList();
  }

  /** Set the name of an object in the scene. */

  public void setObjectName(int which, String name)
  {
    theScene.getObject(which).setName(name);
    itemTree.repaint();
    for (int i = 0; i < theView.length ; i++)
      theView[i].rebuildCameraList();
    theScore.rebuildList();
  }

  /** Set the time which is currently being displayed. */

  public void setTime(double time)
  {
    theScene.setTime(time);
    theScore.setTime(time);
    theScore.repaint();
    itemTree.repaint();
    for (SceneViewer view : theView)
      view.viewChanged(false);
    updateImage();
    dispatchSceneChangedEvent();
  }

  /** Get the Scene associated with this window. */

  public Scene getScene()
  {
    return theScene;
  }

  /** Get the ViewerCanvas which currently has focus. */

  public ViewerCanvas getView()
  {
    return theView[currentView];
  }

  /** Get all ViewerCanvases contained in this window. */

  public ViewerCanvas[] getAllViews()
  {
    return (ViewerCanvas[]) theView.clone();
  }

  /**
   * Set which ViewerCanvas has focus.
   *
   * @param view  the ViewerCanvas which should become the currently focused view.  If this
   * is not one of the views belonging to this window, this method does nothing.
   */

  public void setCurrentView(ViewerCanvas view)
  {
    for (int i = 0; i < theView.length; i++)
      if (currentView != i && view == theView[i])
      {
        theView[currentView].setDrawFocus(false);
        theView[i].setDrawFocus(true);
        displayItem[0].setState(theView[i].getRenderMode() == ViewerCanvas.RENDER_WIREFRAME);
        displayItem[1].setState(theView[i].getRenderMode() == ViewerCanvas.RENDER_FLAT);
        displayItem[2].setState(theView[i].getRenderMode() == ViewerCanvas.RENDER_SMOOTH);
        displayItem[3].setState(theView[i].getRenderMode() == ViewerCanvas.RENDER_TEXTURED);
        displayItem[4].setState(theView[i].getRenderMode() == ViewerCanvas.RENDER_TRANSPARENT);
        displayItem[5].setState(theView[i].getRenderMode() == ViewerCanvas.RENDER_RENDERED);
        currentView = i;
        updateImage();
        updateMenus();
      }
  }

  /** Get the Score for this window. */

  public Score getScore()
  {
    return theScore;
  }

  /** Get the ToolPalette for this window. */

  public ToolPalette getToolPalette()
  {
    return tools;
  }

  /** Set whether a DockableWidget contained in this window is visible. */

  private void setDockableWidgetVisible(DockableWidget widget, boolean visible)
  {
      if(widget == null){
          System.out.println("ERROR: setDockableWidgetVisible()");
          return;
      }
      
    DockingContainer parent = (DockingContainer) widget.getParent();
    BTabbedPane.TabPosition pos = parent.getTabPosition();
    BSplitPane split = parent.getSplitPane();
    if (visible)
      split.resetToPreferredSizes();
    else
      split.setDividerLocation(pos == BTabbedPane.TOP || pos == BTabbedPane.LEFT ? 0.0 : 1.0);
    updateMenus();
  }

  /** Set whether the object list should be displayed. */

  public void setObjectListVisible(boolean visible)
  {
    setDockableWidgetVisible((DockableWidget) itemTreeScroller.getParent(), visible);
  }

  /** Set whether the score should be displayed. */

  public void setScoreVisible(boolean visible)
  {
    setDockableWidgetVisible((DockableWidget) theScore.getParent(), visible);
  }

  /** Set whether the window is split into four views. */

  public void setSplitView(boolean split)
  {
    if ((numViewsShown == 1) == split)
      toggleViewsCommand();
      
      
  }

  /** Get whether the window is split into four views. */

  public boolean getSplitView()
  {
    return (numViewsShown > 1);
  }

  /** This is called when the selection in the object tree changes. */

  private void treeSelectionChanged()
  {
    Object sel[] = itemTree.getSelectedObjects();
    int which[] = new int [sel.length];

    for (int i = 0; i < sel.length; i++)
      which[i] = theScene.indexOf((ObjectInfo) sel[i]);
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_SCENE_SELECTION, new Object [] {getSelectedIndices()}));
    setSelection(which);
    updateImage();
  }
    
    
    private void treeSelectionPickerChanged(){
        Object sel[] = itemSelectionTree.getSelectedObjects();
        int which[] = new int [sel.length];
        for (int i = 0; i < sel.length; i++){
            which[i] = theScene.indexOf((ObjectInfo) sel[i]);
        }
        setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_SCENE_SELECTION, new Object [] {getSelectedIndices()}));
        setSelection(which);
        updateImage();
    }

    /**
     * displayModeCommand
     *
     * Description: Set viewport display.
     */
  private void displayModeCommand(CommandEvent ev)
  {
    Widget source = ev.getWidget();
    if (source == displayItem[0])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_WIREFRAME);
    else if (source == displayItem[1])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_FLAT);
    else if (source == displayItem[2])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_SMOOTH);
    else if (source == displayItem[3])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_TEXTURED);
    else if (source == displayItem[4])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_TRANSPARENT);
    else if (source == displayItem[5])
      theView[currentView].setRenderMode(ViewerCanvas.RENDER_RENDERED);
    for (int i = 0; i < displayItem.length; i++)
      displayItem[i].setState(source == displayItem[i]);
    savePreferences();
  }
    
    /**
     * objectDisplayModeCommand
     *
     * Description: Set an objects display mode that overrides the scene display mode.
     */
    private void objectDisplayModeCommand(CommandEvent ev){
        //System.out.println("objectDisplayModeCommand ");
        Widget source = ev.getWidget();
        //System.out.println("source " + source);
        
        // Get Selected objects.
        for (int index : theScene.getSelection()){
            ObjectInfo oi = theScene.getObject(index);
            
            
            if(source == objectDisplayItem[0]){
                oi.setDisplayModeOverride(0);
            }
            if(source == objectDisplayItem[1]){
                oi.setDisplayModeOverride(ViewerCanvas.RENDER_WIREFRAME + 1);
            }
            if(source == objectDisplayItem[2]){
                oi.setDisplayModeOverride(ViewerCanvas.RENDER_TRANSPARENT + 1);
            }
            if(source == objectDisplayItem[3]){
                oi.setDisplayModeOverride(ViewerCanvas.RENDER_RENDERED + 1);
            }
        }
        
        // redraw scene.
        updateImage();
        updateMenus();
    }

  /** Get a list of the indices of all selected objects. */

  public int[] getSelectedIndices()
  {
    return theScene.getSelection();
  }

  /** Get a collection of all selected objects. */

  public Collection<ObjectInfo> getSelectedObjects()
  {
    ArrayList<ObjectInfo> objects = new ArrayList<ObjectInfo>();
    for (int index : theScene.getSelection())
      objects.add(theScene.getObject(index));
    return objects;
  }

  /** Determine whether an object is selected. */

  public boolean isObjectSelected(ObjectInfo info)
  {
    return info.selected;
  }

  /** Determine whether an object is selected. */

  public boolean isObjectSelected(int index)
  {
    return theScene.getObject(index).selected;
  }

  /** Get the indices of all objects which are either selected, or are children of
      selected objects. */

  public int[] getSelectionWithChildren()
  {
    return theScene.getSelectionWithChildren();
  }

  /** Set a single object in the scene to be selected. */

  public void setSelection(int which)
  {
    itemTree.setUpdateEnabled(false);
    clearSelection();
    theScene.setSelection(which);
    itemTree.setSelected(theScene.getObject(which), true);
    itemTree.setUpdateEnabled(true);
    theScore.rebuildList();
    updateMenus();
  }

  /** Set the list of objects in the scene which should be selected. */

  public void setSelection(int which[])
  {
    itemTree.setUpdateEnabled(false);
    clearSelection();
    theScene.setSelection(which);
    for (int i = 0; i < which.length; i++)
      itemTree.setSelected(theScene.getObject(which[i]), true);
    itemTree.setUpdateEnabled(true);
    theScore.rebuildList();
    updateMenus();
      
      
      // If selection.size() == 2 - pair distance align dialog
      /*
      int sel[] = theScene.getSelection();
      if(sel.length == 2){
          ObjectInfo a = theScene.getObject(sel[0]);
          ObjectInfo b = theScene.getObject(sel[1]);
          if(pairDistanceAlignDialog == null){
              pairDistanceAlignDialog = new PairDistanceAlign(   this, a, b);
          } else {
              pairDistanceAlignDialog.setObjects(a, b);
              pairDistanceAlignDialog.setVisible(true);
          }
      } else if(pairDistanceAlignDialog != null) {
          pairDistanceAlignDialog.deselectObject();
          pairDistanceAlignDialog.setVisible(false);
          pairDistanceAlignDialog.dispose();
          pairDistanceAlignDialog = null;
      }
    */
      //System.out.println("setSelection");
  }

  /**
   * Set an object to be selected.
   *
   */
  public void addToSelection(int which)
  {
    theScene.addToSelection(which);
    itemTree.setSelected(theScene.getObject(which), true);
    theScore.rebuildList();
    updateMenus();
      
      // If selection.size() == 2 - pair distance align dialog
      /*
      int sel[] = theScene.getSelection();
      if(sel.length == 2){
          ObjectInfo a = theScene.getObject(sel[0]);
          ObjectInfo b = theScene.getObject(sel[1]);
          
          if(pairDistanceAlignDialog == null){
              pairDistanceAlignDialog = new PairDistanceAlign(   this, a, b);
          } else {
              pairDistanceAlignDialog.setObjects(a, b);
              pairDistanceAlignDialog.setVisible(true);
          }
      } else if(pairDistanceAlignDialog != null) {
          pairDistanceAlignDialog.deselectObject();
          pairDistanceAlignDialog.setVisible(false);
          pairDistanceAlignDialog.dispose();
          pairDistanceAlignDialog = null;
          
      }
      System.out.println("addToSelection");
        */
  }
    
    
    /**
     * addToSelectionFast
     *
     * Description: Add Item to selection but don't refresh any control UI as we will be adding more to the selection first.
     */
    public void addToSelectionFast(int which){
        theScene.addToSelection(which);
    }
    
    /**
     * refreshItemTreeSelection
     *
     * Description: Used to update the item tree list once after adding many selection items for performance.
     */
    public void refreshItemTreeSelection(){
        int [] sel = theScene.getSelection();
        for(int i = 0; i < sel.length; i++){
            int curr = sel[i];
            itemTree.setSelectedFast(theScene.getObject(curr), true);
        }
        itemTree.buildState();
        itemTree.repaint();
        updateMenus();
    }

  /** Deselect all objects. */

  public void clearSelection()
  {
    theScene.clearSelection();
    itemTree.deselectAll();
    theScore.rebuildList();
    updateMenus();
      
      /*
      if(pairDistanceAlignDialog != null) {
          pairDistanceAlignDialog.deselectObject();
          pairDistanceAlignDialog.setVisible(false);
          pairDistanceAlignDialog.dispose();
          pairDistanceAlignDialog = null;
      }
       */
  }

  /** Deselect a single object. */

  public void removeFromSelection(int which)
  {
    theScene.removeFromSelection(which);
    itemTree.setSelected(theScene.getObject(which), false);
    theScore.rebuildList();
    updateMenus();
  }
    
    /**
     * add objects that have been clicked on the screen to the picker list.
     */
  public void addToSelectionPickerList(int which)
  {
      itemSelectionTree.addElement(new ObjectTreeElement(theScene.getObject(which), itemSelectionTree));
      itemSelectionTree.repaint();
  }
    
  public void clearSelectionPickerList(){
    itemSelectionTree.removeAllElements();
    itemSelectionTree.repaint();
  }

  private void zoomToObject(){
	System.out.println("Zoom to object function not implemented. ");
  }
    
    /**
     * showInList
     *
     * Description: Show a selected object in the list by expanding parents and scrolling.
     */
    private void showInList(){
        System.out.println("showInList to object. ");
        Collection<ObjectInfo> selected = getSelectedObjects();
        Object sel[] = selected.toArray();
        for(int i = 0; i == 0 && i < sel.length; i++){ // only one
            ObjectInfo info = (ObjectInfo)sel[i];
            //System.out.println("Object " + info.getName());
            
            // Find object in tree list and select it.
            //itemTree.deselectAll();
            //itemTree.setSelected( info , true); // theScene.getObject(which[i])
            
            Object [] treeSelection = itemTree.getSelectedObjects();
            for(int e = 0; e == 0 && e < treeSelection.length; e++){
                
                Object el = (Object)treeSelection[e];  // NO
                itemTree.expandToShowObject(el);
            }
            
            
            // TODO: Scroll to.
            
            
        }
    }
    
    /**
     * alignObjectToCollided
     * Description: Quickly align an object to
     */
  private void alignObjectToCollided(){
      System.out.println("alignObjectToCollided to object function not implemented. ");
      
      
      
  }
    
    /**
     * getDebugInfoCommand
     *
     * Description: Print selected object debug info.
     */
    public void getDebugInfoCommand(){
        System.out.println("getDebugInfoCommand to object function not implemented. ");
        // ***
        
        //
        Collection<ObjectInfo> selected = getSelectedObjects();
        Object sel[] = selected.toArray();
        for(int i = 0; i < sel.length; i++){
            ObjectInfo info = (ObjectInfo)sel[i];
            
            System.out.println("" + info.getName());
            Vector<String> debugInfo = info.getDebugInfo();
            for(int d = 0; d < debugInfo.size(); d++){
                System.out.println(" " + debugInfo.elementAt(d));
            }
            
        }
        
    }
    

    /**
     * actionPerformed
     *
     * Description: Object menu.
     */
  private void actionPerformed(CommandEvent e)
  {
    String command = e.getActionCommand();
    Widget src = e.getWidget();
    Widget menu = (src instanceof MenuWidget ? src.getParent() : null);

    setWaitCursor();
    if (menu == fileMenu)
      {
        savePreferences();
        if (command.equals("new"))
          ArmaRender.newWindow();
        else if (command.equals("open"))
          ArmaRender.openScene(this);
        else if (command.equals("close"))
          ArmaRender.closeWindow(this);
        else if (command.equals("quit"))
          ArmaRender.quit();
      }
    else if (command.equals("import"))
     importCommand(((BMenuItem) e.getWidget()).getText());
    else if (command.equals("export"))
      exportCommand(((BMenuItem) e.getWidget()).getText());
    else if (menu == editMenu)
      {
        if (command.equals("selectChildren"))
          {
            setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_SCENE_SELECTION, new Object [] {getSelectedIndices()}));
            setSelection(getSelectionWithChildren());
            updateImage();
          }
      }
    else if (menu == objectMenu)
      {
        if (command.equals("hideSelection"))
          setObjectVisibility(false, true);
        else if (command.equals("showSelection"))
          setObjectVisibility(true, true);
        else if (command.equals("showAll"))
          setObjectVisibility(true, false);
        
        else if(command.equals("hideChildrenSelection"))
            setChildrenObjectVisibility(false, true);
          
        else if(command.equals("showChildrenSelection"))
            setChildrenObjectVisibility(true, true);
          
        else if (command.equals("lockSelection"))
          setObjectsLocked(true, true);
        else if (command.equals("unlockSelection"))
          setObjectsLocked(false, true);
        else if (command.equals("unlockAll"))
          setObjectsLocked(false, false);
      }
    else if (menu == toolsMenu)
      {
        if (command.equals("editScript"))
          new ExecuteScriptWindow(this);
      }
    else if (menu == animationMenu || menu == theScore.getPopupMenu())
      {
        if (command.equals("showScore"))
          setScoreVisible(theScore.getBounds().height == 0 || theScore.getBounds().width == 0);
        else if (command.equals("previewAnimation"))
          new AnimationPreviewer(this);
        else if (command.equals("forwardFrame"))
          {
            double t = theScene.getTime() + 1.0/theScene.getFramesPerSecond();
            setTime(t);
          }
        else if (command.equals("backFrame"))
          {
            double t = theScene.getTime() - 1.0/theScene.getFramesPerSecond();
            setTime(t);
          }
        else if (command.equals("enableTracks"))
          theScore.setTracksEnabled(true);
        else if (command.equals("disableTracks"))
          theScore.setTracksEnabled(false);
        else if (command.equals("pathFromCurve"))
          new PathFromCurveDialog(this, itemTree.getSelectedObjects());
        else if (command.equals("bindToParent"))
          bindToParentCommand();
      }
    else if (menu == editKeyframeMenu)
      {
        if (command.equals("moveKeyframes"))
          new EditKeyframesDialog(this, EditKeyframesDialog.MOVE);
        else if (command.equals("copyKeyframes"))
          new EditKeyframesDialog(this, EditKeyframesDialog.COPY);
        else if (command.equals("rescaleKeyframes"))
          new EditKeyframesDialog(this, EditKeyframesDialog.RESCALE);
        else if (command.equals("loopKeyframes"))
          new EditKeyframesDialog(this, EditKeyframesDialog.LOOP);
        else if (command.equals("deleteKeyframes"))
          new EditKeyframesDialog(this, EditKeyframesDialog.DELETE);
      }
    else if (menu == addTrackMenu)
      {
        if (command.equals("poseTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), PoseTrack.class, null, true);
        else if (command.equals("constraintTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), ConstraintTrack.class, null, true);
        else if (command.equals("visibilityTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), VisibilityTrack.class, null, true);
        else if (command.equals("textureTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), TextureTrack.class, null, true);
      }
    else if (menu == positionTrackMenu)
      {
        if (command.equals("xyzOneTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), PositionTrack.class, null, true);
        else if (command.equals("xyzThreeTracks"))
          {
            theScore.addTrack(itemTree.getSelectedObjects(), PositionTrack.class, new Object [] {"Z Position", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE}, true);
            theScore.addTrack(itemTree.getSelectedObjects(), PositionTrack.class, new Object [] {"Y Position", Boolean.FALSE, Boolean.TRUE, Boolean.FALSE}, false);
            theScore.addTrack(itemTree.getSelectedObjects(), PositionTrack.class, new Object [] {"X Position", Boolean.TRUE, Boolean.FALSE, Boolean.FALSE}, false);
          }
        else if (command.equals("proceduralTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), ProceduralPositionTrack.class, null, true);
      }
    else if (menu == rotationTrackMenu)
      {
        if (command.equals("xyzOneTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), RotationTrack.class, new Object [] {"Rotation", Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE}, true);
        else if (command.equals("xyzThreeTracks"))
          {
            theScore.addTrack(itemTree.getSelectedObjects(), RotationTrack.class, new Object [] {"Z Rotation", Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE}, true);
            theScore.addTrack(itemTree.getSelectedObjects(), RotationTrack.class, new Object [] {"Y Rotation", Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE}, false);
            theScore.addTrack(itemTree.getSelectedObjects(), RotationTrack.class, new Object [] {"X Rotation", Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE}, false);
          }
        else if (command.equals("quaternionTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), RotationTrack.class, new Object [] {"Rotation", Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE}, true);
        else if (command.equals("proceduralTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), ProceduralRotationTrack.class, null, true);
      }
    else if (menu == distortionMenu)
      {
        if (command.equals("bendDistortion"))
          theScore.addTrack(itemTree.getSelectedObjects(), BendTrack.class, null, true);
        else if (command.equals("customDistortion"))
          theScore.addTrack(itemTree.getSelectedObjects(), CustomDistortionTrack.class, null, true);
        else if (command.equals("scaleDistortion"))
          theScore.addTrack(itemTree.getSelectedObjects(), ScaleTrack.class, null, true);
        else if (command.equals("shatterDistortion"))
          theScore.addTrack(itemTree.getSelectedObjects(), ShatterTrack.class, null, true);
        else if (command.equals("twistDistortion"))
          theScore.addTrack(itemTree.getSelectedObjects(), TwistTrack.class, null, true);
        else if (command.equals("IKTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), IKTrack.class, null, true);
        else if (command.equals("skeletonShapeTrack"))
          theScore.addTrack(itemTree.getSelectedObjects(), SkeletonShapeTrack.class, null, true);
      }
    else if (menu == sceneMenu)
      {
        if (command.equals("renderScene"))
          new RenderSetupDialog(this, theScene);
        else if (command.equals("renderImmediately"))
          RenderSetupDialog.renderImmediately(this, theScene);
        else if (command.equals("hideObjectList"))
          setObjectListVisible(itemTreeScroller.getBounds().width == 0 || itemTreeScroller.getBounds().height == 0);
        else if (command.equals("showCoordinateAxes"))
          {
            boolean wasShown = theView[currentView].getShowAxes();
            for (int i = 0; i < theView.length; i++)
              theView[i].setShowAxes(!wasShown);
            savePreferences();
            updateImage();
            updateMenus();
          }
        else if (command.equals("showTemplate"))
          {
            boolean wasShown = theView[currentView].getTemplateShown();
            theView[currentView].setShowTemplate(!wasShown);
            updateImage();
            updateMenus();
          }
        else if (command.equals("frameSelection"))
          frameWithCameraCommand(true);
        else if (command.equals("frameScene"))
          frameWithCameraCommand(false);
        else if (command.equals("images"))
          new ImagesDialog(this, theScene, null);
      }
    else if (menu == popupMenu)
      {
        if (command.equals("selectChildren"))
          {
            setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_SCENE_SELECTION, new Object [] {getSelectedIndices()}));
            setSelection(getSelectionWithChildren());
            updateImage();
          }
        else if (command.equals("hideSelection"))
          setObjectVisibility(false, true);
        else if (command.equals("showSelection"))
          setObjectVisibility(true, true);
          
        else if(command.equals("hideChildrenSelection"))
            setChildrenObjectVisibility(false, true);
          
        else if(command.equals("showChildrenSelection"))
            setChildrenObjectVisibility(true, true);
          
        else if (command.equals("lockSelection"))
          setObjectsLocked(true, true);
        else if (command.equals("unlockSelection"))
          setObjectsLocked(false, true);
      }
    clearWaitCursor();
  }
    
    /**
     * downloadFilesCommand
     */
    void downloadFilesCommand(){
        //System.out.println("downloadFilesCommand" );
        DownloadFiles df = new DownloadFiles(this, this, getScene());
    }
    
    
    public void inventoryManagementCommand(){
        InventoryManagement im = new InventoryManagement(this, this, getScene());
        
    }
    
    
    public void backupCommand(){
        // TODO...
    }

  void importCommand(String format)
  {
    List<Translator> trans = PluginRegistry.getPlugins(Translator.class);
    for (int i = 0; i < trans.size(); i++)
      if (trans.get(i).canImport() && format.equals(trans.get(i).getName()))
        {
          trans.get(i).importFile(this, theScene);
          return;
        }
  }

  void exportCommand(String format)
  {
    List<Translator> trans = PluginRegistry.getPlugins(Translator.class);
      for (int i = 0; i < trans.size(); i++){
          System.out.println("  export: " + trans.get(i).getName());
          if (trans.get(i).canExport() && format.equals(trans.get(i).getName()))
          {
            trans.get(i).exportFile(this, theScene);
            return;
          }
      }
  }

  public void linkExternalCommand()
  {
    BFileChooser fc = new BFileChooser(BFileChooser.OPEN_FILE, Translate.text("externalObject.selectScene"));
    if (!fc.showDialog(this))
      return;
    ExternalObject obj = new ExternalObject(fc.getSelectedFile(), "");
    ObjectInfo info = new ObjectInfo(obj, new CoordinateSystem(), "External Object");
    if (obj.getTexture() == null)
      obj.setTexture(getScene().getDefaultTexture(), getScene().getDefaultTexture().getDefaultMapping(obj));
    UndoRecord undo = new UndoRecord(this, false);
    int sel[] = getSelectedIndices();
    addObject(info, undo);
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
    setUndoRecord(undo);
    setSelection(theScene.getNumObjects()-1);
    editObjectCommand();
  }

  void modellingToolCommand(CommandEvent ev)
  {
    Widget item = ev.getWidget();
    for (int i = 0; i < toolsMenuItem.length; i++)
      if (toolsMenuItem[i] == item)
        modellingTools.get(i).commandSelected(this);
  }

  public void saveCommand()
  {
    if (theScene.getName() == null)
      saveAsCommand();
    else
      modified = !ArmaRender.saveScene(theScene, this);
  }

  public void saveAsCommand()
  {
    BFileChooser fc = new BFileChooser(BFileChooser.SAVE_FILE, Translate.text("saveScene"));
    if (theScene.getName() == null){
      //fc.setSelectedFile(new File("Untitled.aoi"));
      fc.setSelectedFile(new File("Untitled.ads")); 
    } else
      fc.setSelectedFile(new File(theScene.getName()));
    if (theScene.getDirectory() != null)
      fc.setDirectory(new File(theScene.getDirectory()));
    else if (ArmaRender.getCurrentDirectory() != null)
      fc.setDirectory(new File(ArmaRender.getCurrentDirectory()));
    if (!fc.showDialog(this))
      return;
    String name = fc.getSelectedFile().getName();
    if (!name.toLowerCase().endsWith(".ads"))
      name = name+".ads";
    File file = new File(fc.getDirectory(), name);
    if (file.isFile())
    {
      String options[] = new String [] {Translate.text("Yes"), Translate.text("No")};
      int choice = new BStandardDialog("", Translate.text("overwriteFile", name), BStandardDialog.QUESTION).showOptionDialog(this, options, options[1]);
      if (choice == 1)
        return;
    }
    theScene.setName(name);
    theScene.setDirectory(fc.getDirectory().getAbsolutePath());
    setTitle(name);
    modified = !ArmaRender.saveScene(theScene, this);
  }

  public void undoCommand()
  {
    undoStack.executeUndo();
    for (ViewerCanvas view : theView)
      view.viewChanged(false);
    rebuildItemList();
    updateImage();
    updateMenus();
  }

  public void redoCommand()
  {
    undoStack.executeRedo();
    for (ViewerCanvas view : theView)
      view.viewChanged(false);
    rebuildItemList();
    updateImage();
    updateMenus();
  }

  public void cutCommand()
  {
    copyCommand();
    clearCommand();
  }

  public void copyCommand()
  {
    int sel[] = getSelectionWithChildren();
    if (sel.length == 0)
      return;
    ObjectInfo copy[] = new ObjectInfo [sel.length];
    for (int i = 0; i < sel.length; i++)
      copy[i] = theScene.getObject(sel[i]);
    copy = ObjectInfo.duplicateAll(copy);
    ArmaRender.copyToClipboard(copy, theScene);
    updateMenus();
  }

    /**
     * pasteCommand
     *
     * Description: Paste a clipboard object into the scene.
     *
     */
  public void pasteCommand()
  {
    int which[] = new int [ArmaRender.getClipboardSize()], num = theScene.getNumObjects();
    for (int i = 0; i < which.length; i++)
      which[i] = num+i;
    ArmaRender.pasteClipboard(this, false);
    setSelection(which);
    rebuildItemList();
    updateImage();
  }
    
    /**
     * pasteAsChildCommand
     *
     * Description: Past copied object as child of selected object.
     */
    public void pasteAsChildCommand()
    {
      int which[] = new int [ArmaRender.getClipboardSize()], num = theScene.getNumObjects();
      for (int i = 0; i < which.length; i++)
        which[i] = num+i;
      ArmaRender.pasteClipboard(this, true /*as child*/);
      setSelection(which);
      rebuildItemList();
      updateImage();
    }

    /**
     * clearCommand
     *
     * Description:
     */
  public void clearCommand()
  {
    Object sel[] = itemTree.getSelectedObjects();
    int selIndex[] = getSelectedIndices();
    boolean any;
    int i;

    if (sel.length == 0)
      return;
    clearSelection();
    UndoRecord undo = new UndoRecord(this, false);

    // First remove any selected objects.

    for (i = sel.length-1; i >= 0; i--)
      {
        ObjectInfo info = (ObjectInfo) sel[i];
        int index = theScene.indexOf(info);
        removeObject(index, undo);
      }

    // Now remove any objects whose parents were just deleted.

    do
    {
      any = false;
      for (i = 0; i < theScene.getNumObjects(); i++)
        {
          ObjectInfo info = theScene.getObject(i);
          if (info.getParent() != null && theScene.indexOf(info.getParent()) == -1)
            {
              removeObject(i, undo);
              i--;
              any = true;
            }
        }
    } while (any);
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {selIndex});
    setUndoRecord(undo);
    updateMenus();
    updateImage();
  }

  public void selectAllCommand()
  {
    int i, which[] = new int [theScene.getNumObjects()];

    for (i = 0; i < which.length; i++)
      which[i] = i;
    setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_SCENE_SELECTION, new Object [] {getSelectedIndices()}));
    setSelection(which);
    updateImage();
  }
   
    /**
     * selectAllCurvesCommand
     * Select all scene curves and arc objects.
     */
    public void selectAllCurvesCommand()
    {
        Vector selectionIds = new Vector();
        for (int i = 0; i < theScene.getNumObjects(); i++){
            ObjectInfo info = theScene.getObject(i);
            if(info.getObject() instanceof Curve || info.getObject() instanceof ArcObject){
                selectionIds.addElement(i);
            }
        }
        int which[] = new int [selectionIds.size()];
        for(int i = 0; i < selectionIds.size(); i++){
            int currId = (int)selectionIds.elementAt(i);
            which[i] = currId;
        }
        setUndoRecord(new UndoRecord(this, false, UndoRecord.SET_SCENE_SELECTION, new Object [] {getSelectedIndices()}));
        setSelection(which);
        updateImage();
    }
    

  public void preferencesCommand()
  {
    Renderer previewRenderer = ArmaRender.getPreferences().getObjectPreviewRenderer();
    new PreferencesWindow(this);
    if (previewRenderer != ArmaRender.getPreferences().getObjectPreviewRenderer())
    {
      previewRenderer.cancelRendering(theScene);
      for (ViewerCanvas view : theView)
        view.viewChanged(false);
      updateImage();
    }
  }

  public void duplicateCommand()
  {
    Object sel[] = itemTree.getSelectedObjects();
    int i, which[] = new int [sel.length], num = theScene.getNumObjects();

    UndoRecord undo = new UndoRecord(this, false);
    int selected[] = getSelectedIndices();
    for (i = 0; i < sel.length; i++)
      {
        addObject(((ObjectInfo) sel[i]).duplicate(), undo);
        which[i] = num + i;
      }
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {selected});
    setSelection(which);
    setUndoRecord(undo);
    updateImage();
  }

  public void severCommand()
  {
    Object sel[] = itemTree.getSelectedObjects();
    ObjectInfo info;
    int i;

    UndoRecord undo = new UndoRecord(this, false);
    for (i = 0; i < sel.length; i++)
      {
        info = (ObjectInfo) sel[i];
        undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
        info.setObject(info.object.duplicate());
      }
    setUndoRecord(undo);
  }

    
    public void autoAlignTransformCommand(){
        // Not implemented
    
    }
    
    /**
     * zeroTransformCommand
     *
     * Description: zero
     */
    public void zeroTransformCommand(){
        System.out.println("Zero Transform.");
        ZeroTransform zero = new ZeroTransform();
        zero.zero(theScene);
    }
    
    /**
     * joinObjectVerticesCommand
     *
     * Description: Join two selected object verticies using a JoinPointObject.
     *
     * Depricate: Use Connect Points instead. This function is not reliable or fully implemented.
     */
    public void joinObjectVerticesCommand(){
        PointJoinObject createPointJoin = theScene.getCreatePointJoinObject();
        createPointJoin.setScene(theScene);
        
        //System.out.println(" *** JOIN *** ");
        System.out.println("    A: " + createPointJoin.objectA + " point: " + createPointJoin.objectAPoint + " " );
        System.out.println("    B: " + createPointJoin.objectB + " point: " + createPointJoin.objectBPoint + " " );
        
        if(createPointJoin.objectA <= 0 || createPointJoin.objectB <= 0){
            // exit
            System.out.println("No points selected.");
        }
        
        Vec3 v[] = new Vec3[2];
        v[0] = new Vec3(0.0, 0.0, 0.0);
        v[1] = new Vec3(0.0, 0.0, 0.0);
        
        int count = theScene.getNumObjects();
        for(int i = 0; i < count; i++){
            ObjectInfo obj = theScene.getObject(i);
            if( obj.getId() == createPointJoin.objectA ){
                //System.out.println(" FOUND A " + obj.getName());
                Mesh o3d = (Mesh)obj.getObject();
                MeshVertex[] verts = o3d.getVertices();
                if(createPointJoin.objectAPoint < verts.length){
                    MeshVertex vm = verts[createPointJoin.objectAPoint];
                    Vec3 vec = vm.r;
                    v[0] = new Vec3(vec.x, vec.y, vec.z);
                }
            }
            if( obj.getId() == createPointJoin.objectB ){
                //System.out.println(" FOUND B " + obj.getName());
                Mesh o3d = (Mesh)obj.getObject();
                MeshVertex[] verts = o3d.getVertices();
                if(createPointJoin.objectAPoint < verts.length){
                    MeshVertex vm = verts[createPointJoin.objectBPoint];
                    Vec3 vec = vm.r;
                    v[1] = new Vec3(vec.x, vec.y, vec.z);
                }
            }
        }
        
        //System.out.println("    A: " + v[0].x + " " + v[0].y  + " " + v[0].z );
        //System.out.println("    B: " + v[1].x + " " + v[1].y  + " " + v[1].z );
        createPointJoin.setVertex(v);
        
        
        // Save pointJoin object to project file.
        
        // copy AddToScene function from DimensionTool
        
        int counter = 0;
        
        CoordinateSystem coords;
        Vec3 vertex[], orig, ydir, zdir;
        orig = new Vec3();
        
        ViewerCanvas view = theView[currentView];
        EditingWindow theWindow = null;
        
        UndoRecord undo = new UndoRecord(theWindow, false);
        
        Camera cam = view.getCamera();
        ydir = cam.getViewToWorld().timesDirection(Vec3.vy());
        zdir = cam.getViewToWorld().timesDirection(new Vec3(0.0, 0.0, -1.0));
        coords = new CoordinateSystem(orig, zdir, ydir);
        
        ObjectInfo info = new ObjectInfo(createPointJoin, coords, "PointJoin "+(counter++));
        info.addTrack(new PositionTrack(info), 0);
        info.addTrack(new RotationTrack(info), 1);
        
        ((LayoutWindow)this).addObject(info, undo);
        
        // Reset the new pointjoin object
        PointJoinObject resetPointJoin = new PointJoinObject();
        theScene.setPointJoinObject(resetPointJoin);
        
        //((LayoutWindow) theWindow).setSelection(theWindow.getScene().getNumObjects()-1);
        
        // Reload screen
        // TODO
    }
    
    
    /**
     * joinCurvesCommand
     *
     * Derscription: move curves such that the verts use the same coordinates
     *  Modify geometry such that the closest points
     */
    public void joinCurvesCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        System.out.println("joinCurvesCommand " );
        int [] selection = theScene.getSelection();
        if(selection.length < 2){
            System.out.println("Error: Select two curves ");
        }
        // 1) Get the selected curves.
        ObjectInfo dominantCurve = null;
        ObjectInfo supportCurve = null;
        for(int i = 0; i < selection.length; i++){
            ObjectInfo obj = theScene.getObject(selection[i]);
            Object co = (Object)obj.getObject();
            if((co instanceof Curve) == true){
                if(((Curve)co).isSupportMode()){
                    supportCurve = obj;
                } else {
                    dominantCurve = obj;
                }
            }
        }
        
        Object aCo = (Object)dominantCurve.getObject();
        Object bCo = (Object)supportCurve.getObject();
        
        if(dominantCurve != null && supportCurve != null){
            
            // 3) find closest joining point
            
            // Transform points of  curves with object matrix.
            CoordinateSystem c;
            c = dominantCurve.getCoords().duplicate();
            Mat4 aMat4 = c.duplicate().fromLocal();
            MeshVertex aDomv[] = ((Mesh)aCo).getVertices();
            Vec3 aTranslatedPoints[] = new Vec3[aDomv.length];
            for(int i = 0; i < aDomv.length; i++){
                Vec3 point = ((MeshVertex)aDomv[i]).r;
                aMat4.transform(point);
                //subv[i].r = point;
                aTranslatedPoints[i] = point;
            }
            
            c = supportCurve.getCoords().duplicate();
            Mat4 bMat4 = c.duplicate().fromLocal();
            MeshVertex bDomv[] = ((Mesh)bCo).getVertices();
            Vec3 bTranslatedPoints[] = new Vec3[bDomv.length];
            for(int i = 0; i < bDomv.length; i++){
                Vec3 point = ((MeshVertex)bDomv[i]).r;
                bMat4.transform(point);
                //subv[i].r = point;
                bTranslatedPoints[i] = point;
            }
            
            double closestDistance = 99999;
            int closestA = -1;
            int closestB = -1;
            Vec3 closestAVec = null;
            Vec3 closestBVec = null;
            for(int a = 0; a < aTranslatedPoints.length; a++){
                Vec3 aVec = (Vec3)aTranslatedPoints[a];
                for(int b = 0; b < bTranslatedPoints.length; b++){
                    Vec3 bVec = (Vec3)bTranslatedPoints[b];
                    double distance = aVec.distance(bVec);
                    if(distance < closestDistance){
                        closestDistance = distance;
                        closestA = a;
                        closestB = b;
                        closestAVec = aVec;
                        closestBVec = bVec;
                    }
                }
            }
            
            System.out.println(" closest a " + closestA + " " + closestB + " "   );
            
            // Move Support point
            if(closestA > -1 && closestB > -1){
                
                bTranslatedPoints[closestB] = closestAVec;
                bDomv[closestB].r = closestAVec;
                //bCo.setVertices();
                
                ((Mesh)bCo).setVertexPositions(bTranslatedPoints);
                supportCurve.setObject((Object3D)bCo);
                
                updateImage();
            }
            
            
        }
        
        // 4) Which point to move? both or one? Move the support curve, leave dominant curve.
        
    }
    
    
    /**
     * meshEdgeCurvesCommand
     * Description: Generate curves from mesh edges
     *  TODO: Possibly move to another location.
     */
    public void meshEdgeCurvesCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
       
        Vector<ObjectInfo> objects = theScene.getObjects();
        for(int a = 0; a < objects.size(); a++){
            ObjectInfo obj = (ObjectInfo)objects.elementAt(a);
            Object co = (Object)obj.getObject();
            if((co instanceof SplineMesh) == true && obj.isVisible() && obj.isHiddenByParent() == false){
                //
                SplineMesh mesh = (SplineMesh) obj.getObject(); // Object3D
                int u = mesh.getUSize();
                int v = mesh.getVSize();
                
                //System.out.println("obj mesh " + obj.getName() + " u: " + u + " v: " + v );
                // u 4 horizon    v 6 vert
               
                // get points
                Vec3 vert[] = mesh.getVertexPositions();
                //CoordinateSystem c;
                CoordinateSystem cs = ((ObjectInfo)obj).getCoords();
                Vec3 origin = cs.getOrigin();
                
                // Delete existing edge curves.
                ObjectInfo replaceTop = null;
                ObjectInfo replaceBottom = null;
                ObjectInfo replaceLeft = null;
                ObjectInfo replaceRight = null;
                ObjectInfo[] children = obj.getChildren();
                for(int i = 0; i < children.length; i++){
                    ObjectInfo child = (ObjectInfo)children[i];
                    String top = obj.getName() + " - edge top";
                    String bottom = obj.getName() + " - edge bottom";
                    String left = obj.getName() + " - edge left";
                    String right = obj.getName() + " - edge right";
                    if( child.getName().trim().equals(top.trim())){
                        replaceTop = child;
                    }
                    if(child.getName().trim().equals(bottom.trim())){
                        replaceBottom = child;
                    }
                    if(child.getName().trim().equals(left.trim())){
                        replaceLeft = child;
                    }
                    if(child.getName().trim().equals(right.trim())){
                        replaceRight = child;
                    }
                }
                
                Mat4 aMat4 = cs.duplicate().fromLocal();
               
                //
                // Top row
                //
                Vector topRowPoints = new Vector();
                for(int i = 0; i < u; i++){
                    int index = i + (u * 0) ; // u=horizon
                    //System.out.println("index: " + index);
                    Vec3 currVec = new Vec3((Vec3)vert[ index]);
                    aMat4.transform(currVec);
                    //System.out.println(" - " + currVec );
                    topRowPoints.addElement(new Vec3(currVec));
                }
                // Add curve from points
                Curve curve = Curve.getCurveFromPoints(topRowPoints);
                if(replaceTop == null){
                    ObjectInfo topEdgeCurveInfo = new ObjectInfo(curve, new CoordinateSystem(), obj.getName() + " - edge top");
                    replaceTop = topEdgeCurveInfo;
                    obj.addChild(replaceTop, 0);
                    replaceTop.setParent(obj);
                    theScene.addObject(replaceTop, null);
                    theScene.objectModified(obj.getObject());
                } else {
                    replaceTop.setObject(curve);
                    theScene.objectModified(replaceTop.getObject());
                }
                
                //
                // Bottom row
                //
                Vector bottomRowPoints = new Vector();
                for(int i = 0; i < u; i++){
                    int index = i + (u * (v-1)); // u=horizon
                    //System.out.println("index: " + index);
                    Vec3 currVec = new Vec3((Vec3)vert[index]);
                    aMat4.transform(currVec);
                    //System.out.println(" - " + currVec );
                    bottomRowPoints.addElement(new Vec3(currVec));
                }
                // Add curve from points
                Curve bottomCurve = Curve.getCurveFromPoints(bottomRowPoints);
                if(replaceBottom == null){
                    ObjectInfo bottomEdgeCurveInfo = new ObjectInfo(bottomCurve, new CoordinateSystem(), obj.getName() + " - edge bottom");
                    replaceBottom = bottomEdgeCurveInfo;
                    obj.addChild(replaceBottom, 0);
                    replaceBottom.setParent(obj);
                    theScene.addObject(replaceBottom, null);
                    theScene.objectModified(obj.getObject());
                } else {
                    replaceBottom.setObject(bottomCurve);
                    theScene.objectModified(replaceBottom.getObject());
                }
                
                //
                // Left edge
                //
                Vector leftEdgePoints = new Vector();
                for(int i = 0; i < v; i++){
                    int index = 0 + (u * (i));
                    //System.out.println("index: " + index);
                    Vec3 currVec = new Vec3((Vec3)vert[index]);
                    aMat4.transform(currVec);
                    //System.out.println(" - " + currVec );
                    leftEdgePoints.addElement(new Vec3(currVec));
                }
                // Add curve from points
                Curve leftCurve = Curve.getCurveFromPoints(leftEdgePoints);
                if(replaceLeft == null){
                    ObjectInfo leftEdgeCurveInfo = new ObjectInfo(leftCurve, new CoordinateSystem(), obj.getName() + " - edge left");
                    replaceLeft = leftEdgeCurveInfo;
                    obj.addChild(replaceLeft, 0);
                    replaceLeft.setParent(obj);
                    theScene.addObject(replaceLeft, null);
                    theScene.objectModified(obj.getObject());
                } else {
                    replaceLeft.setObject(leftCurve);
                    theScene.objectModified(replaceLeft.getObject());
                }
                
                //
                // right edge
                //
                Vector rightEdgePoints = new Vector();
                for(int i = 0; i < v; i++){ // i is row
                    int index = (u-1) + (u * i); // u=horizon   v=vert
                    Vec3 currVec = new Vec3((Vec3)vert[index]);
                    aMat4.transform(currVec);
                    //System.out.println(" - " + currVec );
                    rightEdgePoints.addElement(new Vec3(currVec));
                }
                // Add curve from points
                Curve rightCurve = Curve.getCurveFromPoints(rightEdgePoints);
                if(replaceRight == null){
                    ObjectInfo rightEdgeCurveInfo = new ObjectInfo(rightCurve, new CoordinateSystem(), obj.getName() + " - edge right");
                    replaceRight = rightEdgeCurveInfo;
                    obj.addChild(replaceRight, 0);
                    replaceRight.setParent(obj);
                    theScene.addObject(replaceRight, null);
                    theScene.objectModified(obj.getObject());
                } else {
                    replaceRight.setObject(rightCurve);
                    theScene.objectModified(replaceRight.getObject());
                }
                
                obj.clearCachedMeshes();
                rebuildItemList();
            }
        }
        
        // Update scene and object tree list.
        this.updateImage();
        this.itemTree.repaint();
    }
    
    /**
     * connectCurvesCommand
     *
     * Description: Connect points between two curves.
     */
    public void connectCurvesCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        int [] selection = theScene.getSelection();
        PointJoinObject createPointJoin = new PointJoinObject(); // theScene.getCreatePointJoinObject();
        createPointJoin.setScene(theScene);
        if(selection.length < 2){
            System.out.println("Error: Select two curves ");
        }
        
        for(int i = 0; i < selection.length; i++){
            ObjectInfo obj = theScene.getObject(selection[i]);
            
            // TODO: ensure the selected objects are curves.
            
            System.out.println(" obj: " + obj.getName() );
            
            if(i == 0){
                System.out.println(" A "  );
                createPointJoin.objectA = obj.getId();
            }
            if(i == 1){
                System.out.println(" B "  );
                createPointJoin.objectB = obj.getId();
            }
        }
        
        if(selection.length >= 2){
            Vec3 v[] = new Vec3[2];
            v[0] = new Vec3(1.0, 0.0, 0.0);
            v[1] = new Vec3(1.0, 0.0, 0.0);
            createPointJoin.setVertex(v);
            
            createPointJoin.updateLocation(); // set join markup based on the curves.
            
            CoordinateSystem coords;
            Vec3 vertex[], orig, ydir, zdir;
            orig = new Vec3();
            
            ViewerCanvas view = theView[currentView];
            EditingWindow theWindow = null;
            
            UndoRecord undo = new UndoRecord(theWindow, false);
            
            Camera cam = new Camera();
            ydir = cam.getViewToWorld().timesDirection(Vec3.vy());
            zdir = cam.getViewToWorld().timesDirection(new Vec3(0.0, 0.0, 1.0));
            coords = new CoordinateSystem(orig, zdir, ydir);
            
            int counter = 0;
            
            ObjectInfo info = new ObjectInfo(createPointJoin, coords, "CurveJoin "+(counter++));
            info.addTrack(new PositionTrack(info), 0);
            info.addTrack(new RotationTrack(info), 1);
            
            ((LayoutWindow)this).addObject(info, undo);
        
            updateImage();
        }
    }

  /**
  * findObjectCommand
  *
  * Description: find objects by name
  */
  public void findObjectCommand(){
    // Get search string.
    String x = "";
    String search = JOptionPane.showInputDialog("Seach for Object name ", x);
    if(search != null){
      itemSelectionTree.removeAllElements();

      // Search through model for matches.
      int count = theScene.getNumObjects();
      for(int i = 0; i < count; i++){
        ObjectInfo obj = theScene.getObject(i);
        if( obj.getName().contains(search) ){
          System.out.println(" FOUND " + obj.getName());
          // add results to selection list

          itemSelectionTree.addElement(new ObjectTreeElement(obj, itemTree));
        }      
      }  
    }
  }

    /**
     * editObjectCommand
     * - obj.edit(this)
     *
     * Description: tell selected object to load editor for particular type.
     */
  public void editObjectCommand()
  {
    int sel[] = getSelectedIndices();
    final Object3D obj;

    if (sel.length != 1)
      return;
    obj = theScene.getObject(sel[0]).getObject();
    if (obj.isEditable())
      {
        final UndoRecord undo = new UndoRecord(this, false, UndoRecord.COPY_OBJECT, new Object [] {obj, obj.duplicate()});
        
          //obj.setLayoutWindow((Object)this); // used by editor windows to access the scene and windows.
          
          obj.edit(this, theScene.getObject(sel[0]), new Runnable() {
          public void run()
          {
            setUndoRecord(undo);
            theScene.objectModified(obj);
              
              // If object is cube and part of a boolean structure, update the generated geometry.
              if(obj instanceof Cube || obj instanceof Cylinder || obj instanceof Sphere){
                  // If parent is Boolean folder
                  ObjectInfo clickedObject = theScene.getObject(sel[0]);
                  ObjectInfo parent = clickedObject.getParent();
                  if(parent != null){
                      Object3D parentObj = parent.getObject();
                      if(parentObj instanceof Folder){
                          Folder f = (Folder)parentObj;
                          if(f.getAction() == Folder.UNION || f.getAction() == Folder.SUBTRACT || f.getAction() == Folder.INTERSECTION){
                              ObjectInfo rootObject = parent.getParent();
                              if(rootObject != null){
                                  updateBooleanObject( rootObject ); // regenerate boolean objects affected by this change.
                              }
                          }
                      }
                  }
              }
              
            updateImage();
            updateMenus();
          }
        } );
      }
  }
    
    /**
     * numericEditObjectCommand
     *
     * Description: Numeric object editor.
     */
    public void numericEditObjectCommand(){
        // TODO:
        
        Object selectedObjects[] = itemTree.getSelectedObjects();
        if(selectedObjects.length > 0){
            ObjectInfo selectedInfo = (ObjectInfo) selectedObjects[0];
        
            //NumericObjectEditor numericEditor = new NumericObjectEditor(this, selectedInfo);
        }
        
    }

  public void objectLayoutCommand()
  {
    int i, sel[] = getSelectedIndices();
    TransformDialog dlg;
    ObjectInfo obj[] = new ObjectInfo [sel.length];
    Vec3 orig, size;
    double angles[], values[];

    if (sel.length == 0)
      return;
    UndoRecord undo = new UndoRecord(this, false);
    for (i = 0; i < sel.length; i++)
      {
        obj[i] = theScene.getObject(sel[i]);
        undo.addCommand(UndoRecord.COPY_OBJECT, new Object [] {obj[i].getObject(), obj[i].getObject().duplicate()});
        undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {obj[i].getCoords(), obj[i].getCoords().duplicate()});
      }
    if (sel.length == 1)
    {
		System.out.println(" translate ");

      orig = obj[0].getCoords().getOrigin();
      angles = obj[0].getCoords().getRotationAngles();
      size = obj[0].getObject().getBounds().getSize();
      dlg = new TransformDialog(this, Translate.text("objectLayoutTitle", theScene.getObject(sel[0]).getName()),
          new double [] {orig.x, orig.y, orig.z, angles[0], angles[1], angles[2],
          size.x, size.y, size.z}, false, false);
      if (!dlg.clickedOk())
        return;
      values = dlg.getValues();
      if (!Double.isNaN(values[0]))
        orig.x = values[0];
      if (!Double.isNaN(values[1]))
        orig.y = values[1];
      if (!Double.isNaN(values[2]))
        orig.z = values[2];
      if (!Double.isNaN(values[3]))
        angles[0] = values[3];
      if (!Double.isNaN(values[4]))
        angles[1] = values[4];
      if (!Double.isNaN(values[5]))
        angles[2] = values[5];
      if (!Double.isNaN(values[6]))
        size.x = values[6];
      if (!Double.isNaN(values[7]))
        size.y = values[7];
      if (!Double.isNaN(values[8]))
        size.z = values[8];
      obj[0].getCoords().setOrigin(orig);
      obj[0].getCoords().setOrientation(angles[0], angles[1], angles[2]);
      obj[0].getObject().setSize(size.x, size.y, size.z);
      theScene.objectModified(obj[0].getObject());
      obj[0].getObject().sceneChanged(obj[0], theScene);
      theScene.applyTracksAfterModification(Collections.singleton(obj[0]));


      if(obj[0].getLayoutView() == false){
	  	// Save
	  	System.out.println(" translate save ");
	  }

    }
    else
    {
		System.out.println(" translate multiple ");

      dlg = new TransformDialog(this, Translate.text("objectLayoutTitleMultiple"), false, false);
      if (!dlg.clickedOk())
        return;
      values = dlg.getValues();
      for (i = 0; i < sel.length; i++)
      {
        orig = obj[i].getCoords().getOrigin();
        angles = obj[i].getCoords().getRotationAngles();
        size = obj[i].getObject().getBounds().getSize();
        if (!Double.isNaN(values[0]))
          orig.x = values[0];
        if (!Double.isNaN(values[1]))
          orig.y = values[1];
        if (!Double.isNaN(values[2]))
          orig.z = values[2];
        if (!Double.isNaN(values[3]))
          angles[0] = values[3];
        if (!Double.isNaN(values[4]))
          angles[1] = values[4];
        if (!Double.isNaN(values[5]))
          angles[2] = values[5];
        if (!Double.isNaN(values[6]))
          size.x = values[6];
        if (!Double.isNaN(values[7]))
          size.y = values[7];
        if (!Double.isNaN(values[8]))
          size.z = values[8];
        obj[i].getCoords().setOrigin(orig);
        obj[i].getCoords().setOrientation(angles[0], angles[1], angles[2]);
        obj[i].getObject().setSize(size.x, size.y, size.z);
      }
      ArrayList<ObjectInfo> modified = new ArrayList<ObjectInfo>();
      for (int index : sel)
        modified.add(theScene.getObject(index));
      theScene.applyTracksAfterModification(modified);
    }
    setUndoRecord(undo);
    updateImage();
  }

  public void transformObjectCommand()
  {
	  System.out.println("transformObjectCommand()");
	LayoutModeling layout = new LayoutModeling();
	//layout.setBaseDir(theScene.getDirectory() + System.getProperty("file.separator") + theScene.getName() + "_layout_data" );

    int i, sel[] = getSelectedIndices();
    TransformDialog dlg;
    ObjectInfo info;
    Object3D obj;
    CoordinateSystem coords;
    Vec3 orig, size, center;
    double values[];
    Mat4 m;

    if (sel.length == 0)
      return;
    if (sel.length == 1)
      dlg = new TransformDialog(this, Translate.text("transformObjectTitle", theScene.getObject(sel[0]).getName()),
                new double [] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0}, true, true);
    else
      dlg = new TransformDialog(this, Translate.text("transformObjectTitleMultiple"),
                new double [] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0}, true, true);
    if (!dlg.clickedOk())
      return;
    values = dlg.getValues();

    // Find the center of all selected objects.

    BoundingBox bounds = null;
    for (i = 0; i < sel.length; i++)
    {
      info = theScene.getObject(sel[i]);
      if (bounds == null)
        bounds = info.getBounds().transformAndOutset(info.getCoords().fromLocal());
      else
        bounds = bounds.merge(info.getBounds().transformAndOutset(info.getCoords().fromLocal()));
    }
    center = bounds.getCenter();
    if (dlg.applyToChildren())
      sel = getSelectionWithChildren();

    // Determine the rotation matrix.

    m = Mat4.identity();
    if (!Double.isNaN(values[3]))
      m = m.times(Mat4.xrotation(values[3]*Math.PI/180.0));
    if (!Double.isNaN(values[4]))
      m = m.times(Mat4.yrotation(values[4]*Math.PI/180.0));
    if (!Double.isNaN(values[5]))
      m = m.times(Mat4.zrotation(values[5]*Math.PI/180.0));
    UndoRecord undo = new UndoRecord(this, false);
    HashSet<Object3D> scaledObjects = new HashSet<Object3D>();
    for (i = 0; i < sel.length; i++)
    {
      info = theScene.getObject(sel[i]);
      obj = info.getObject();
      coords = info.getCoords();

      // JDT
      if(info.getLayoutView() == false){
		coords = layout.getCoords(info); // Read cutting coord from file
	  }

      if (!scaledObjects.contains(obj))
        undo.addCommand(UndoRecord.COPY_OBJECT, new Object [] {obj, obj.duplicate()});
      undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {coords, coords.duplicate()});
      orig = coords.getOrigin();
        
        if(obj.getBounds() == null){    // Boolean objects have no bounds.
            //System.out.println("Error: " + info.getName() + " bounds is null. " );
            // Todo: these objects should return this value.
            size = new Vec3(1,1,1);
        } else {
            size = obj.getBounds().getSize();
        }
      if (!Double.isNaN(values[0]))
        orig.x += values[0];
      if (!Double.isNaN(values[1]))
        orig.y += values[1];
      if (!Double.isNaN(values[2]))
        orig.z += values[2];
      if (!Double.isNaN(values[6]))
        size.x *= values[6];
      if (!Double.isNaN(values[7]))
        size.y *= values[7];
      if (!Double.isNaN(values[8]))
        size.z *= values[8];
      if (dlg.useSelectionCenter())
      {
        Vec3 neworig = orig.minus(center);
        if (!Double.isNaN(values[6]))
          neworig.x *= values[6];
        if (!Double.isNaN(values[7]))
          neworig.y *= values[7];
        if (!Double.isNaN(values[8]))
          neworig.z *= values[8];
        coords.setOrigin(neworig);
        coords.transformCoordinates(m);
        coords.setOrigin(coords.getOrigin().plus(center));
      }
      else
      {
        coords.setOrigin(orig);
        coords.transformAxes(m);
      }
      if (!scaledObjects.contains(obj))
      {
        obj.setSize(size.x, size.y, size.z);
        scaledObjects.add(obj);
      }

      // JDT
		if(info.getLayoutView() == false){
			layout.saveLayout(info, coords);
			info.resetLayoutCoords(coords);
		}
        if(info.getTubeLayoutView() == true){
            layout.saveLayout(info, coords);
            info.resetLayoutCoords(coords);
        }
    }

    for (i = 0; i < sel.length; i++)
    {
      info = theScene.getObject(sel[i]);
      theScene.objectModified(info.getObject());
    }
    ArrayList<ObjectInfo> modified = new ArrayList<ObjectInfo>();
    for (int index : sel)
      modified.add(theScene.getObject(index));
    theScene.applyTracksAfterModification(modified);
    setUndoRecord(undo);
    updateImage();
  }

  public void alignObjectsCommand()
  {
    int i, sel[] = getSelectedIndices();
    ComponentsDialog dlg;
    ObjectInfo info;
    CoordinateSystem coords;
    Vec3 alignTo, orig, center;
    BComboBox xchoice, ychoice, zchoice;
    RowContainer px = new RowContainer(), py = new RowContainer(), pz = new RowContainer();
    ValueField vfx, vfy, vfz;
    BoundingBox bounds;

    if (sel.length == 0)
      return;
    px.add(xchoice = new BComboBox(new String [] {
      Translate.text("doNotAlign"),
      Translate.text("Right"),
      Translate.text("Center"),
      Translate.text("Left"),
      Translate.text("Origin")
    }));
    px.add(Translate.label("alignTo"));
    px.add(vfx = new ValueField(Double.NaN, ValueField.NONE, 5));
    py.add(ychoice = new BComboBox(new String [] {
      Translate.text("doNotAlign"),
      Translate.text("Top"),
      Translate.text("Center"),
      Translate.text("Bottom"),
      Translate.text("Origin")
    }));
    py.add(Translate.label("alignTo"));
    py.add(vfy = new ValueField(Double.NaN, ValueField.NONE, 5));
    pz.add(zchoice = new BComboBox(new String [] {
      Translate.text("doNotAlign"),
      Translate.text("Front"),
      Translate.text("Center"),
      Translate.text("Back"),
      Translate.text("Origin")
    }));
    pz.add(Translate.label("alignTo"));
    pz.add(vfz = new ValueField(Double.NaN, ValueField.NONE, 5));
    dlg = new ComponentsDialog(this, Translate.text("alignObjectsTitle"),
                new Widget [] {px, py, pz}, new String [] {"X", "Y", "Z"});
    if (!dlg.clickedOk())
      return;
    UndoRecord undo = new UndoRecord(this, false);

    // Determine the position to align the objects to.

    alignTo = new Vec3();
    for (i = 0; i < sel.length; i++)
    {
      info = theScene.getObject(sel[i]);
      coords = info.getCoords();
      bounds = info.getBounds();
      bounds = bounds.transformAndOutset(coords.fromLocal());
      center = bounds.getCenter();
      orig = coords.getOrigin();
      if (!Double.isNaN(vfx.getValue()))
        alignTo.x += vfx.getValue();
      else if (xchoice.getSelectedIndex() == 1)
        alignTo.x += bounds.maxx;
      else if (xchoice.getSelectedIndex() == 2)
        alignTo.x += center.x;
      else if (xchoice.getSelectedIndex() == 3)
        alignTo.x += bounds.minx;
      else if (xchoice.getSelectedIndex() == 4)
        alignTo.x += orig.x;
      if (!Double.isNaN(vfy.getValue()))
        alignTo.y += vfy.getValue();
      else if (ychoice.getSelectedIndex() == 1)
        alignTo.y += bounds.maxy;
      else if (ychoice.getSelectedIndex() == 2)
        alignTo.y += center.y;
      else if (ychoice.getSelectedIndex() == 3)
        alignTo.y += bounds.miny;
      else if (ychoice.getSelectedIndex() == 4)
        alignTo.y += orig.y;
      if (!Double.isNaN(vfz.getValue()))
        alignTo.z += vfz.getValue();
      else if (zchoice.getSelectedIndex() == 1)
        alignTo.z += bounds.maxz;
      else if (zchoice.getSelectedIndex() == 2)
        alignTo.z += center.z;
      else if (zchoice.getSelectedIndex() == 3)
        alignTo.z += bounds.minz;
      else if (zchoice.getSelectedIndex() == 4)
        alignTo.z += orig.z;
    }
    alignTo.scale(1.0/sel.length);

    // Now transform all of the objects.

    for (i = 0; i < sel.length; i++)
    {
      info = theScene.getObject(sel[i]);
      coords = info.getCoords();
      bounds = info.getBounds();
      bounds = bounds.transformAndOutset(coords.fromLocal());
      center = bounds.getCenter();
      orig = coords.getOrigin();
      undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {coords, coords.duplicate()});
      if (xchoice.getSelectedIndex() == 1)
        orig.x += alignTo.x-bounds.maxx;
      else if (xchoice.getSelectedIndex() == 2)
        orig.x += alignTo.x-center.x;
      else if (xchoice.getSelectedIndex() == 3)
        orig.x += alignTo.x-bounds.minx;
      else if (xchoice.getSelectedIndex() == 4)
        orig.x += alignTo.x-orig.x;
      if (ychoice.getSelectedIndex() == 1)
        orig.y += alignTo.y-bounds.maxy;
      else if (ychoice.getSelectedIndex() == 2)
        orig.y += alignTo.y-center.y;
      else if (ychoice.getSelectedIndex() == 3)
        orig.y += alignTo.y-bounds.miny;
      else if (ychoice.getSelectedIndex() == 4)
        orig.y += alignTo.y-orig.y;
      if (zchoice.getSelectedIndex() == 1)
        orig.z += alignTo.z-bounds.maxz;
      else if (zchoice.getSelectedIndex() == 2)
        orig.z += alignTo.z-center.z;
      else if (zchoice.getSelectedIndex() == 3)
        orig.z += alignTo.z-bounds.minz;
      else if (zchoice.getSelectedIndex() == 4)
        orig.z += alignTo.z-orig.z;
      coords.setOrigin(orig);
    }
    ArrayList<ObjectInfo> modified = new ArrayList<ObjectInfo>();
    for (int index : sel)
      modified.add(theScene.getObject(index));
    theScene.applyTracksAfterModification(modified);
    setUndoRecord(undo);
    updateImage();
  }

  public void setTextureCommand()
  {
    int sel[] = getSelectedIndices(), i, count = 0;
    ObjectInfo obj[];

    for (i = 0; i < sel.length; i++)
      if (theScene.getObject(sel[i]).getObject().canSetTexture())
        count++;
    if (count == 0)
      return;
    obj = new ObjectInfo [count];
    for (i = 0; i < sel.length; i++)
      if (theScene.getObject(sel[i]).getObject().canSetTexture())
        obj[i] = theScene.getObject(sel[i]);
    new ObjectTextureDialog(this, obj);
    for (i = 0; i < sel.length; i++)
      theScene.objectModified(theScene.getObject(sel[i]).getObject());
    modified = true;
    updateImage();
  }

  public void renameObjectCommand()
  {
    int sel[] = getSelectedIndices();
    ObjectInfo info;

    if (sel.length != 1)
      return;
    info = theScene.getObject(sel[0]);
    BStandardDialog dlg = new BStandardDialog("", Translate.text("renameObjectTitle"), BStandardDialog.PLAIN);
    String val = dlg.showInputDialog(this, null, info.getName());
    if (val == null)
      return;
    setUndoRecord(new UndoRecord(this, false, UndoRecord.RENAME_OBJECT, new Object [] {new Integer(sel[0]), info.getName()}));
    setObjectName(sel[0], val);
  }

  public void convertToTriangleCommand()
  {
    int sel[] = getSelectedIndices();
    Object3D obj, mesh;
    ObjectInfo info;

    if (sel.length != 1)
      return;
    info = theScene.getObject(sel[0]);
    obj = info.getObject();
    if (obj.canConvertToTriangleMesh() == Object3D.CANT_CONVERT)
      return;

    // If the object has a Pose track, all Pose keyframes will need to be deleted.

    boolean confirmed = false, hasPose = false;
    for (int i = 0; i < info.getTracks().length; i++)
      if (info.getTracks()[i] instanceof PoseTrack)
      {
        hasPose = true;
        if (!confirmed && !info.getTracks()[i].isNullTrack())
        {
          BStandardDialog dlg = new BStandardDialog("", Translate.text("convertLosesPosesWarning", info.getName()), BStandardDialog.QUESTION);
          String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
          if (dlg.showOptionDialog(this, options, options[0]) == 1)
            return;
          confirmed = true;
        }
        if (info.getTracks()[i].getTimecourse() != null)
          info.getTracks()[i].getTimecourse().removeAllTimepoints();
        info.setPose(null);
      }
    if (confirmed)
      theScore.repaintAll();
    UndoRecord undo = new UndoRecord(this, false, UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
    if (obj.canConvertToTriangleMesh() == Object3D.EXACTLY)
    {
      if (!confirmed)
      {
        BStandardDialog dlg = new BStandardDialog("", Translate.text("confirmConvertToTriangle", info.getName()), BStandardDialog.QUESTION);
        String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
        if (dlg.showOptionDialog(this, options, options[0]) == 1)
          return;
      }
      mesh = obj.convertToTriangleMesh(0.0);
    }
    else
    {
      ValueField errorField = new ValueField(0.1, ValueField.POSITIVE);
      ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("selectToleranceForMesh"),
          new Widget [] {errorField}, new String [] {Translate.text("maxError")});
      if (!dlg.clickedOk())
        return;
      mesh = obj.convertToTriangleMesh(errorField.getValue());
    }
    if (mesh == null)
    {
      new BStandardDialog("", Translate.text("cannotTriangulate"), BStandardDialog.ERROR).showMessageDialog(this);
      return;
    }
    if (hasPose)
      mesh = mesh.getPosableObject();
    if (mesh.getTexture() == null)
    {
      Texture tex = theScene.getDefaultTexture();
      mesh.setTexture(tex, tex.getDefaultMapping(mesh));
    }
    theScene.replaceObject(obj, mesh, undo);
    setUndoRecord(undo);
    updateImage();
    updateMenus();
  }

  public void convertToActorCommand()
  {
    int sel[] = getSelectedIndices();
    Object3D obj;
    ObjectInfo info;

    if (sel.length != 1)
      return;
    info = theScene.getObject(sel[0]);
    obj = info.getObject();
    Object3D posable = obj.getPosableObject();
    if (posable == null)
      return;
    BStandardDialog dlg = new BStandardDialog("", UIUtilities.breakString(Translate.text("confirmConvertToActor", info.getName())), BStandardDialog.QUESTION);
    String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
    if (dlg.showOptionDialog(this, options, options[0]) == 1)
      return;
    UndoRecord undo = new UndoRecord(this, false, UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
    theScene.replaceObject(obj, posable, undo);
    setUndoRecord(undo);
    updateImage();
    updateMenus();
  }

  // JDT Get object details.
  public void getObjectInfo(){
      if(theScene != null){
            theScene.getObjectInfo();
      }
  }
    
    public void fixCorruptionCommand(){
        
        
    }
    
    /**
     * toggleBackgroundGradient
     *
     * Description: Tell each view to toggle the background gradient.
     */
    public void toggleBackgroundGradient(){
        for(int i = 0; i < 4; i++){
            SceneViewer v = theView[i];
            v.toggleBackgroundGradient();
        }
        setModified();
        updateImage();
    }
    
    /**
     * toggleSelectionHighlight
     *
     * Description: Toggle contour selection highlight.
     */
    public void toggleSelectionHighlight(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        
        // Toggle display mode
        displayContouredHighlight = !displayContouredHighlight;
        
        // Update canvas attribute.
        for (SceneViewer view : theView){
            view.drawContouredHighlight( displayContouredHighlight );
        }
    }
    

  public void autoSkin(){
      if(theScene != null){
         theScene.autoSkin(this);
      }
  }
    
    public void autoSkinByVoids(){
        if(theScene != null){
           theScene.autoSkinByVoids(this);
        }
    }
    
    public void splineGridSkin(){
        if(theScene != null){
           theScene.splineGridSkin(this);
        }
    }
    
    
    public void connectedCurvesToMeshCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
           theScene.connectedCurvesToMesh(this);
        }
    }
    
    public void connectedCurvesToQuadMeshCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            //theScene.removeSplineMesh();
           theScene.connectedCurvesToQuadMesh(this, false, 0, 0);
        }
    }
    
    public void connectedCurvesToQuadMeshCommand2(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            //theScene.removeSplineMesh();
           theScene.connectedCurvesToQuadMesh(this, false, 0, 1);
        }
    }
    
    public void connectedCurvesToQuadMeshCommandDebug(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            //theScene.removeSplineMesh();
           theScene.connectedCurvesToQuadMesh(this, true, 0, 0);
        }
    }
    
    public void connectedCurvesToQuadMeshHDCommand(){
        if(theScene != null){
            //theScene.removeSplineMesh();
           theScene.connectedCurvesToQuadMesh(this, false, 2, 0);
        }
    }
    
    
    /**
     * resizeMeshCommand
     *
     * Description: Resize mesh.
     */
    public void resizeMeshCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //System.out.println("resizeMeshCommand ");
        
        ResizeSplineMesh resizeDialog = new ResizeSplineMesh(this, this, getScene());
        
        // Get selected
        /*
        int selection [] = theScene.getSelection();
        if(selection.length >= 1){
            ObjectInfo objectInfo = theScene.getObject(selection[0]);
            System.out.println(" -> " + objectInfo.getName());
            Object co = (Object)objectInfo.getObject();
            // armarender.object.SplineMesh
            //System.out.println("class " + co.getClass().getName());
            if( co instanceof armarender.object.Mesh ){ // armarender.object.SplineMesh
                System.out.println(" MESH ");
                SplineMesh sMesh = (SplineMesh)co;
                
                int width = sMesh.getUSize();
                int height = sMesh.getVSize();
                System.out.println(" width " + width + " " + height);
                //MeshVertex vm = sMesh.getVertex(0, 0);
                //Vec3 normalVec = vm.r;
                //System.out.println(" normal  " + normalVec.x + " " + normalVec.y + " " + normalVec.z + " " );
                  //return vertex[u+usize*v];
            }
        }
         */
    }
    
    /**
     * conformMeshToCurvesCommand
     * Description: Modify mesh to conform to child curve object profiles.
     */
    public void conformMeshToCurvesCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        
        ConformMesh conform = new ConformMesh(this);
        conform.conformMeshToCurves(getScene());
    }
    
    public void conformCurveToCurvesCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        
        ConformMesh conform = new ConformMesh(this);
        conform.conformCurveToCurves(getScene());
    }
    
    
    
    /**
     * pairDistanceAlign
     *
     */
    public void pairDistanceAlign(){
        if(theScene != null){
            //
            int selection [] = theScene.getSelection();
            if(selection.length == 2){
                ObjectInfo a = theScene.getObject(selection[0]);
                ObjectInfo b = theScene.getObject(selection[1]);
                
                if(pairDistanceAlignDialog == null){
                    pairDistanceAlignDialog = new PairDistanceAlign(this, a, b);
                } else {
                    pairDistanceAlignDialog.setObjects(a, b);
                }
            } else {
                
                // LOG
                Sound.playSound("deny.wav");
                
                System.out.println("Error: Select two objects to align."  );
                
                JOptionPane.showMessageDialog(null, "The selected two objects to align.",  "Error" , JOptionPane.ERROR_MESSAGE);
            }
            
            
            //info = this.getObject(sel[0]);
            
        }
    }
    
    /**
     * Temp until we can get BSP running automatically when required.
     */
    public void booleanSceneCommand(){ // ObjectInfo selectedObject
        //BooleanSceneProcessor bsp = new BooleanSceneProcessor();
        bsp.go(this);
    }
    
    /**
     * updateBooleanObject
     * Description: regenerate a selected boolean object.
     */
    public void updateBooleanObject(ObjectInfo selectedObject){
        //BooleanSceneProcessor bsp = new BooleanSceneProcessor();
        //bsp.go(this);
        //bsp.process(selectedObject, selectedObject, null, this);
        bsp.processObjectInThread(selectedObject);
    }
    

    public void joinMultipleSplines(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            theScene.joinMultipleSplines(this);
        }
    }

    public void straightenSpline(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            theScene.straightenSpline(this);
        }
    }
    
    public void getSplineLength(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            theScene.getSplineLength(this);
        }
    }
    
    public void spaceEquallyCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        System.out.println("spaceEquallyCommand: Not implemented");
    }
    
    /**
     * getCurveFromTubeMesh
     *
     * Description: Get curve from tube geometry.
     */
    public void getCurveFromTubeMesh(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        TubeCurve tubeCurve = new TubeCurve(this);
        tubeCurve.getCurveFromTubeMesh(theScene);
    }
    
    
    public void getArcFromTubeMesh(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        TubeCurve tubeCurve = new TubeCurve(this);
        tubeCurve.getArcFromTubeMesh(theScene);
    }
    
    public void getTubeFromCurve(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        TubeCurve tubeCurve = new TubeCurve(this);
        tubeCurve.getTubeFromCurve(theScene);
    }
    
    public void getTubeFromArc(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        TubeCurve tubeCurve = new TubeCurve(this);
        tubeCurve.getTubeFromArc(theScene);
    }
    
    public void getCombinedTubeLength(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //
        System.out.println("CombinedTubeLength: Not implemented. ");
    }
    
    
    public void conformCurveToMesh(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //
        //System.out.println("conformCurveToMesh: Not implemented. ");
        ConformMesh cm = new ConformMesh(this);
        cm.conformCurveToMesh(theScene, null);
    }
    
    /**
     * notchTubeIntersectionsInnerCommand
     *
     */
    public void notchTubeIntersectionsInnerCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //NotchIntersections notch = new NotchIntersections(theScene, this);
        //notch.notchIntersections(theScene, false, false); // pass scene objects , angled, outer
    }
    
    public void notchTubeIntersectionsOuterCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //NotchIntersections notch = new NotchIntersections(theScene, this);
        //notch.notchIntersections(theScene , false, true); // pass scene objects   , angled, outer
    }
    
    public void notchTubeIntersectionsAngledCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //NotchIntersections notch = new NotchIntersections(theScene, this);
        //notch.notchIntersections(theScene, true, false); // pass scene objects , angled, outer
    }
    
    public void notchTubeEndsCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //NotchIntersections notch = new NotchIntersections(theScene, this);
        //notch.notchEnds( theScene, false ); // pass scene objects
    }
    
    public void notchTubeEndsOuterCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //NotchIntersections notch = new NotchIntersections(theScene, this);
        //notch.notchEnds( theScene, true ); // pass scene objects
    }
    
    public void addTabsToNotchCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //NotchIntersections notch = new NotchIntersections(theScene, this);
        //notch.addTabsToNotch( theScene , -1); // pass scene objects
    }
    
    public void addTabsToNotchAdvancedCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //NotchIntersections notch = new NotchIntersections(theScene, this);
        //notch.addTabsToNotchAdvanced( theScene ); // pass scene objects
    }
    
    
    public void mergeIntersectingNotchesCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //NotchIntersections notch = new NotchIntersections(theScene, this);
        //notch.mergeIntersectingNotches( theScene );
    }
    
    public void addReferenceMarkerCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //NotchIntersections notch = new NotchIntersections(theScene, this);
        //notch.addReferenceMarker(theScene); // pass scene objects
    }
    
    /**
     * addNotchesToTubeGeometryCommand
     *
     */
    public void addNotchesToTubeGeometryCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //NotchIntersections notch = new NotchIntersections(theScene, this);
        //notch.addNotchesFromGeometry2(theScene);
    }
    
    public void compensateNotchToolPathCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //NotchIntersections notch = new NotchIntersections(theScene, this);
        //notch.compensateNotchToolPath(theScene);
    }
    
    public void dashSelectedNotchGeometryCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //NotchIntersections notch = new NotchIntersections(theScene, this);
        //notch.dashSelectedNotchGeometry(theScene);
    }

    public void perferateTrianglesCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //Perferate perferate = new Perferate(theScene, this);
        //perferate.perferateTriangles( theScene ); // pass scene objects
    }
    
    public void perferateSquaresCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //Perferate perferate = new Perferate(theScene, this);
        //perferate.perferateSquares( theScene ); // pass scene objects
    }
    
    public void perferateDiamondsCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //Perferate perferate = new Perferate(theScene, this);
        //perferate.perferateDiamonds( theScene ); // pass scene objects
    }
    
    public void rotateNotchNintyCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //ArcTubeUtils arcTubeUtils = new ArcTubeUtils(this);
        //arcTubeUtils.rotateNotchNinty(null);
    }
    
    public void addBendMarkersCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        
        // Implement
    }
    
    
    /**
     * extractArcTubeGeometryCommand
     *
     * Description: Generate arc tube geometry. Adds cylendars and scehers to the scene where arcs reside.
     *
     */
    public void extractArcTubeGeometryCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        
        Thread thread = new Thread(){
            boolean running = true;
            public void run(){
                final JDialog progressDialog = new JDialog(); //  parentFrame , "Progress Dialog", true ); // parentFrame , "Progress Dialog", true); // Frame owner
                JProgressBar dpb = new JProgressBar(0, 100);
                progressDialog.add(BorderLayout.CENTER, dpb);
                JLabel progressLabel = new JLabel("Extracting Geometry...");
                progressDialog.add(BorderLayout.NORTH, progressLabel);
                progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                progressDialog.setSize(300, 75);
                progressDialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                // progressDialog.setLocationRelativeTo(parentFrame);
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                progressDialog.setLocation((int)(screenSize.getWidth() / 2) - (300/2), (int) ((screenSize.getHeight()/(float)2) - ((float)75/(float)2.0)));
                progressDialog.addWindowListener(new WindowAdapter()
                {
                    public void windowClosed(WindowEvent e)
                    {
                        System.out.println("jdialog window closed event received");
                        running = false;
                    }
                    public void windowClosing(WindowEvent e)
                    {
                        System.out.println("jdialog window closing event received");
                        running = false;
                    }
                });
                progressDialog.setVisible(true);
                
                Vector<ObjectInfo> sceneObjects = theScene.getObjects();
                int currProgress = 0;
                for(int o = 0; o < sceneObjects.size() && running; o++){
                    ObjectInfo info = sceneObjects.elementAt(o);
                    Object co = (Object)info.getObject();
                    if(co instanceof armarender.object.ArcObject &&
                       info.isVisible() &&
                       info.isChildrenHiddenWhenHidden() == false
                    ){
                        ArcObject arc = (ArcObject)co;
                        //System.out.println("Auto Notch Arc. ");
                        Vector<ObjectInfo> segments = arc.getTubeGeometry(info, true); //
                        
                        //currProgress++;
                        int progress = (int) (((float)(o) / (float)(sceneObjects.size())) * (float)100);
                        dpb.setValue(progress);
                        //System.out.println("progress: " + progress);
                        
                        for(int i = 0; i < segments.size(); i++){
                            ObjectInfo segOI = (ObjectInfo)segments.elementAt(i);
                            theScene.addObject(segOI, null);
                        }
                        
                        /*
                         // Remove spheres, they should be in ArcObject if needed.
                        // Add spheres to connections between segments
                        LayoutModeling layout = new LayoutModeling();
                        Mesh mesh = (Mesh)info.getObject(); // Object3D ;
                        Vec3 [] verts = mesh.getVertexPositions();
                        CoordinateSystem c;
                        c = layout.getCoords(info);
                        Mat4 mat4 = c.duplicate().fromLocal();
                        for(int i = 1; i < verts.length - 1; i++){
                            Vec3 vec1 = verts[i];
                            mat4.transform(vec1);
                            double radius = arc.getTubeDiameter() / 2;
                            radius *= 1.1;
                            Sphere sphere = new Sphere(radius, radius, radius);
                            CoordinateSystem sphereCS = new CoordinateSystem();
                            sphereCS.setOrigin( new Vec3( vec1 ) );
                            ObjectInfo sphereOI = new ObjectInfo(sphere, sphereCS, "sphere");
                            theScene.addObject(sphereOI, null);
                        }
                        */
                        
                        
                    } // Object is arc and visible
                } // o objects in scene
                
                // Update scene
                updateImage();
                updateTree();
                
                // Done with progress dialog.
                progressDialog.setVisible(false);
                
            } // thread run
        }; // thread
        thread.start();
    }
    
    
    /**
     * snapTubeToAdjacentCentersCommand
     *
     * Description: Snap selected tube objects to connect ends to adjacent arc tubes.
     */
    public void snapTubeToAdjacentCentersCommand(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        
        // Get selected arc info.
        int sel[] = getSelectedIndices();
        for (int i = 0; i < sel.length; i++)
        {
            ObjectInfo info = theScene.getObject(sel[i]);
            Object co = (Object)info.getObject();
            if(co instanceof armarender.object.ArcObject){
                ArcObject arc = (ArcObject)co;
                
                
                System.out.println("snap " + info.getName() );
                
                // Get first and last points.
                MeshVertex[] verts = arc.getVertices();
                if(verts.length > 1){
                    
                    Vec3 start = verts[0].r;
                    
                    
                }
                
                
                
                // Find adjacent tubes.
                Vector<ObjectInfo> sceneObjects = theScene.getObjects();
                int currProgress = 0;
                for(int o = 0; o < sceneObjects.size(); o++){
                    ObjectInfo adjacentInfo = sceneObjects.elementAt(o);
                    Object adjacentCo = (Object)adjacentInfo.getObject();
                    if(adjacentCo instanceof armarender.object.ArcObject &&
                       info.isVisible() &&
                       info.isChildrenHiddenWhenHidden() == false
                       ){
                        ArcObject adjacentArc = (ArcObject)adjacentCo;
                        //System.out.println("Auto Notch Arc. ");
                        Vector<ObjectInfo> segments = adjacentArc.getTubeGeometry(adjacentInfo, true); //
                        
                        
                        
                        
                    }
                }
            }
            
        }
    
    }
    
    
  private void setObjectVisibility(boolean visible, boolean selectionOnly)
  {
    UndoRecord undo = new UndoRecord(this, false);
    if (selectionOnly)
    {
      int sel[] = getSelectedIndices();
      for (int i = 0; i < sel.length; i++)
      {
        ObjectInfo info = theScene.getObject(sel[i]);
        undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
        info.setVisible(visible);
          
          mirrorMesh.updateVisibility(info);
      }
    }
    else
      for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
        info.setVisible(visible);
          
          mirrorMesh.updateVisibility(info);
      }
    setUndoRecord(undo);
    updateImage();
    itemTree.repaint();
  }
    
    /**
     * setChildrenObjectVisibility
     *
     * TODO
     */
    private void setChildrenObjectVisibility(boolean visible, boolean selectionOnly)
    {
      UndoRecord undo = new UndoRecord(this, false);
      //if (selectionOnly)
      //{
        int sel[] = getSelectedIndices();
        for (int i = 0; i < sel.length; i++)
        {
          ObjectInfo info = theScene.getObject(sel[i]);
          undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
            //info.setVisible(visible);
            info.setChildrenHiddenWhenHidden(visible);
            
            ObjectInfo[] children = info.getChildren();
            for(int j = 0; j < children.length; j++){
                ObjectInfo child = children[j];
                //child.setVisible(visible);
                child.setChildrenHiddenWhenHidden(visible);
            }
            
            mirrorMesh.updateVisibility(info);
        }
      /*
      }
      else
        for (int i = 0; i < theScene.getNumObjects(); i++)
        {
          ObjectInfo info = theScene.getObject(i);
          undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
          info.setVisible(visible);
        }
      */
      //setUndoRecord(undo);
      updateImage();
      itemTree.repaint();
    }

  private void setObjectsLocked(boolean locked, boolean selectionOnly)
  {
    UndoRecord undo = new UndoRecord(this, false);
    if (selectionOnly)
    {
      int sel[] = getSelectedIndices();
      for (int i = 0; i < sel.length; i++)
      {
        ObjectInfo info = theScene.getObject(sel[i]);
        undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
        info.setLocked(locked);
      }
    }
    else
      for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
        info.setLocked(locked);
      }
    setUndoRecord(undo);
    updateImage();
    itemTree.repaint();
  }


  void createObjectCommand(CommandEvent ev)
  {
    String type = ev.getActionCommand();
    Object3D obj;
    String name;

    if ("cube".equals(type))
    {
      obj = new Cube(1.0, 1.0, 1.0);
      name = "Cube "+(CreateCubeTool.counter++);
    }
    else if ("sphere".equals(type))
    {
      obj = new Sphere(0.5, 0.5, 0.5);
      name = "Sphere "+(CreateSphereTool.counter++);
    }
    else if ("cylinder".equals(type))
    {
      obj = new Cylinder(1.0, 0.5, 0.5, 1.0);
      name = "Cylinder "+(CreateCylinderTool.counter++);
    }
    else if ("cone".equals(type))
    {
      obj = new Cylinder(1.0, 0.5, 0.5, 0.0);
      name = "Cone "+(CreateCylinderTool.counter++);
    }
    else if ("pointLight".equals(type))
    {
      obj = new PointLight(new RGBColor(1.0f, 1.0f, 1.0f), 1.0f, 0.1);
      name = "Light "+(CreateLightTool.counter++);
    }
    else if ("directionalLight".equals(type))
    {
      obj = new DirectionalLight(new RGBColor(1.0f, 1.0f, 1.0f), 1.0f);
      name = "Light "+(CreateLightTool.counter++);
    }
    else if ("spotLight".equals(type))
    {
      obj = new SpotLight(new RGBColor(1.0f, 1.0f, 1.0f), 1.0f, 20.0, 0.0, 0.1);
      name = "Light "+(CreateLightTool.counter++);
    }
    else if ("proceduralPointLight".equals(type))
    {
      obj = new ProceduralPointLight(0.1);
      name = "Light "+(CreateLightTool.counter++);
    }
    else if ("proceduralDirectionalLight".equals(type))
    {
      obj = new ProceduralDirectionalLight(1.0);
      name = "Light "+(CreateLightTool.counter++);
    }
    else if ("camera".equals(type))
    {
      obj = new SceneCamera();
      name = "Camera "+(CreateCameraTool.counter++);
    }
    else if ("referenceImage".equals(type))
    {
      BFileChooser fc = new ImageFileChooser(Translate.text("selectReferenceImage"));
      if (!fc.showDialog(this))
        return;
      File f = fc.getSelectedFile();
      Image image = new ImageIcon(f.getAbsolutePath()).getImage();
      if (image == null || image.getWidth(null) <= 0 || image.getHeight(null) <= 0)
      {
        new BStandardDialog("", UIUtilities.breakString(Translate.text("errorLoadingImage", f.getName())), BStandardDialog.ERROR).showMessageDialog(this);
        return;
      }
      obj = new ReferenceImage(image);
      name = f.getName();
      if (name.lastIndexOf('.') > -1)
        name = name.substring(0, name.lastIndexOf('.'));
    }
    else
    {
      obj = new NullObject();
      name = "Null";
    }
    CoordinateSystem coords = new CoordinateSystem(new Vec3(), Vec3.vz(), Vec3.vy());
    ObjectInfo info = new ObjectInfo(obj, coords, name);
    if (obj.canSetTexture())
      info.setTexture(theScene.getDefaultTexture(), theScene.getDefaultTexture().getDefaultMapping(obj));
    Vec3 orig = coords.getOrigin();
    double angles[] = coords.getRotationAngles();
    Vec3 size = info.getBounds().getSize();
    TransformDialog dlg = new TransformDialog(this, Translate.text("objectLayoutTitle", name),
        new double [] {orig.x, orig.y, orig.z, angles[0], angles[1], angles[2],
        size.x, size.y, size.z}, false, false);
    if (!dlg.clickedOk())
      return;
    double values[] = dlg.getValues();
    if (!Double.isNaN(values[0]))
      orig.x = values[0];
    if (!Double.isNaN(values[1]))
      orig.y = values[1];
    if (!Double.isNaN(values[2]))
      orig.z = values[2];
    if (!Double.isNaN(values[3]))
      angles[0] = values[3];
    if (!Double.isNaN(values[4]))
      angles[1] = values[4];
    if (!Double.isNaN(values[5]))
      angles[2] = values[5];
    if (!Double.isNaN(values[6]))
      size.x = values[6];
    if (!Double.isNaN(values[7]))
      size.y = values[7];
    if (!Double.isNaN(values[8]))
      size.z = values[8];
    coords.setOrigin(orig);
    coords.setOrientation(angles[0], angles[1], angles[2]);
    obj.setSize(size.x, size.y, size.z);
    info.clearCachedMeshes();
    info.addTrack(new PositionTrack(info), 0);
    info.addTrack(new RotationTrack(info), 1);
    UndoRecord undo = new UndoRecord(this, false);
    int sel[] = getSelectedIndices();
    addObject(info, undo);
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
    setSelection(theScene.getNumObjects()-1);
    setUndoRecord(undo);
    updateImage();
  }

  public void createScriptObjectCommand()
  {
    // Prompt the user to select a name and, optionally, a predefined script.

    BTextField nameField = new BTextField(Translate.text("Script"));
    BComboBox scriptChoice = new BComboBox();
    scriptChoice.add(Translate.text("newScript"));
    String files[] = new File(ArmaRender.OBJECT_SCRIPT_DIRECTORY).list();
    ArrayList<String> scriptNames = new ArrayList<String>();
    if (files != null)
      for (String file : files)
      {
        try
        {
          ScriptRunner.getLanguageForFilename(file);
          scriptChoice.add(file.substring(0, file.lastIndexOf(".")));
          scriptNames.add(file);
        }
        catch (IllegalArgumentException ex)
        {
          // This file isn't a known scripting language.
        }
      }
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("newScriptedObject"),
      new Widget [] {nameField, scriptChoice}, new String [] {Translate.text("Name"), Translate.text("Script")});
    if (!dlg.clickedOk())
      return;

    // If they are using a predefined script, load it.

    String scriptText = "";
    String language = ScriptRunner.LANGUAGES[0];
    if (scriptChoice.getSelectedIndex() > 0)
    {
      try
      {
        File f = new File(ArmaRender.OBJECT_SCRIPT_DIRECTORY, scriptNames.get(scriptChoice.getSelectedIndex()-1));
        scriptText = ArmaRender.loadFile(f);
        language = ScriptRunner.getLanguageForFilename(f.getName());
      }
      catch (IOException ex)
      {
        new BStandardDialog("", new String [] {Translate.text("errorReadingScript"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(this);
        return;
      }
    }
    ScriptedObject obj = new ScriptedObject(scriptText, language);
    ObjectInfo info = new ObjectInfo(obj, new CoordinateSystem(), nameField.getText());
    UndoRecord undo = new UndoRecord(this, false);
    int sel[] = getSelectedIndices();
    addObject(info, undo);
    undo.addCommand(UndoRecord.SET_SCENE_SELECTION, new Object [] {sel});
    setSelection(theScene.getNumObjects()-1);
    setUndoRecord(undo);
    updateImage();
    editObjectCommand();
  }

  public void jumpToTimeCommand()
  {
    ValueField timeField = new ValueField(theScene.getTime(), ValueField.NONE);
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("jumpToTimeTitle"),
      new Widget [] {timeField}, new String [] {Translate.text("Time")});

    if (!dlg.clickedOk())
      return;
    double t = timeField.getValue();
    double fps = theScene.getFramesPerSecond();
    t = Math.round(t*fps)/(double) fps;
    setTime(t);
  }

  public void bindToParentCommand()
  {
    BStandardDialog dlg = new BStandardDialog("", UIUtilities.breakString(Translate.text("confirmBindParent")), BStandardDialog.QUESTION);
    String options[] = new String [] {Translate.text("button.ok"), Translate.text("button.cancel")};
    if (dlg.showOptionDialog(this, options, options[0]) == 1)
      return;
    int sel[] = getSelectedIndices();

    UndoRecord undo = new UndoRecord(this, false);
    for (int i = 0; i < sel.length; i++)
    {
      ObjectInfo info = theScene.getObject(sel[i]);
      if (info.getParent() == null)
        continue;
      Skeleton s = info.getParent().getSkeleton();
      ObjectRef relObj = new ObjectRef(info.getParent());
      if (s != null)
      {
        double nearest = Double.MAX_VALUE;
        Joint jt[] = s.getJoints();
        Vec3 pos = info.getCoords().getOrigin();
        for (int j = 0; j < jt.length; j++)
        {
          ObjectRef r = new ObjectRef(info.getParent(), jt[j]);
          double dist = r.getCoords().getOrigin().distance2(pos);
          if (dist < nearest)
          {
            relObj = r;
            nearest = dist;
          }
        }
      }
      undo.addCommand(UndoRecord.COPY_OBJECT_INFO, new Object [] {info, info.duplicate()});
      PositionTrack pt = new PositionTrack(info);
      pt.setCoordsObject(relObj);
      info.addTrack(pt, 0);
      pt.setKeyframe(theScene.getTime(), theScene);
      RotationTrack rt = new RotationTrack(info);
      rt.setCoordsObject(relObj);
      info.addTrack(rt, 1);
      rt.setKeyframe(theScene.getTime(), theScene);
    }
    setUndoRecord(undo);
    theScore.rebuildList();
    theScore.repaint();
  }

  public void renderCommand()
  {
    new RenderSetupDialog(this, theScene);
  }

  public void toggleViewsCommand()
  {
    if (numViewsShown == 4)
    {
      numViewsShown = 1;
      viewsContainer.setColumnWeight(0, (currentView == 0 || currentView == 2) ? 1 : 0);
      viewsContainer.setColumnWeight(1, (currentView == 1 || currentView == 3) ? 1 : 0);
      viewsContainer.setRowWeight(0, (currentView == 0 || currentView == 1) ? 1 : 0);
      viewsContainer.setRowWeight(1, (currentView == 2 || currentView == 3) ? 1 : 0);
      sceneMenuItem[0].setText(Translate.text("menu.fourViews"));
    }
    else
    {
      numViewsShown = 4;
      viewsContainer.setColumnWeight(0, 1);
      viewsContainer.setColumnWeight(1, 1);
      viewsContainer.setRowWeight(0, 1);
      viewsContainer.setRowWeight(1, 1);
      sceneMenuItem[0].setText(Translate.text("menu.oneView"));
    }
    viewsContainer.layoutChildren();
    savePreferences();
    updateImage();
    viewPanel[currentView].requestFocus();
      
      // Windows need to be updated with the active tool
      setTool(tools.getTool(tools.getSelection()));
  }

  public void setTemplateCommand()
  {
    BFileChooser fc = new ImageFileChooser(Translate.text("selectTemplateImage"));
    if (!fc.showDialog(this))
      return;
    File f = fc.getSelectedFile();
    try
    {
      theView[currentView].setTemplateImage(f);
    }
    catch (InterruptedException ex)
    {
      new BStandardDialog("", UIUtilities.breakString(Translate.text("errorLoadingImage", f.getName())), BStandardDialog.ERROR).showMessageDialog(this);
    }
    theView[currentView].setShowTemplate(true);
    updateImage();
    updateMenus();
  }

  public void setGridCommand()
  {
    ValueField spaceField = new ValueField(theScene.getGridSpacing(), ValueField.POSITIVE);
    ValueField divField = new ValueField(theScene.getGridSubdivisions(), ValueField.POSITIVE+ValueField.INTEGER);
    BCheckBox showBox = new BCheckBox(Translate.text("showGrid"), theScene.getShowGrid());
    BCheckBox snapBox = new BCheckBox(Translate.text("snapToGrid"), theScene.getSnapToGrid());
    ComponentsDialog dlg = new ComponentsDialog(this, Translate.text("gridTitle"),
                new Widget [] {spaceField, divField, showBox, snapBox},
                new String [] {Translate.text("gridSpacing"), Translate.text("snapToSubdivisions"), null, null});
    if (!dlg.clickedOk())
      return;
    theScene.setGridSpacing(spaceField.getValue());
    theScene.setGridSubdivisions((int) divField.getValue());
    theScene.setShowGrid(showBox.getState());
    theScene.setSnapToGrid(snapBox.getState());
    for (int i = 0; i < theView.length; i++)
      theView[i].setGrid(theScene.getGridSpacing(), theScene.getGridSubdivisions(), theScene.getShowGrid(), theScene.getSnapToGrid());
    updateImage();
  }

  public void frameWithCameraCommand(boolean selectionOnly)
  {
    int sel[] = getSelectionWithChildren();
    BoundingBox bb = null;

    if (selectionOnly)
      for (int i = 0; i < sel.length; i++)
      {
        ObjectInfo info = theScene.getObject(sel[i]);
        BoundingBox bounds = info.getBounds().transformAndOutset(info.getCoords().fromLocal());
        if (bb == null)
          bb = bounds;
        else
          bb = bb.merge(bounds);
      }
    else
      for (int i = 0; i < theScene.getNumObjects(); i++)
      {
        ObjectInfo info = theScene.getObject(i);
        BoundingBox bounds = info.getBounds().transformAndOutset(info.getCoords().fromLocal());
        if (bb == null)
          bb = bounds;
        else
          bb = bb.merge(bounds);
      }
    if (bb == null)
      return;
    if (numViewsShown == 1)
      theView[currentView].frameBox(bb);
    else
      for (int i = 0; i < theView.length; i++)
        theView[i].frameBox(bb);
    updateImage();
  }

  public void texturesCommand()
  {
    theScene.showTexturesDialog(this);
  }

    /**
     *
     */
  public void environmentCommand()
  {
    final RGBColor ambColor = theScene.getAmbientColor(), envColor = theScene.getEnvironmentColor(), fogColor = theScene.getFogColor();
    final RGBColor oldAmbColor = ambColor.duplicate(), oldEnvColor = envColor.duplicate(), oldFogColor = fogColor.duplicate();
    final Widget ambPatch = ambColor.getSample(50, 30), envPatch = envColor.getSample(50, 30), fogPatch = fogColor.getSample(50, 30);
    final BCheckBox fogBox = new BCheckBox("Environment Fog", theScene.getFogState());
    final ValueField fogField = new ValueField(theScene.getFogDistance(), ValueField.POSITIVE);
    final OverlayContainer envPanel = new OverlayContainer();
    final BComboBox envChoice;
    final BButton envButton = new BButton(Translate.text("Choose")+":");
    final BLabel envLabel = new BLabel();
    final Sphere envSphere = new Sphere(1.0, 1.0, 1.0);
    final ObjectInfo envInfo = new ObjectInfo(envSphere, new CoordinateSystem(), "Environment");

    envChoice = new BComboBox(new String [] {
      Translate.text("solidColor"),
      Translate.text("textureDiffuse"),
      Translate.text("textureEmissive")
    });
    envChoice.setSelectedIndex(theScene.getEnvironmentMode());
    RowContainer row = new RowContainer();
    row.add(envButton);
    row.add(envLabel);
    envPanel.add(envPatch, 0);
    envPanel.add(row, 1);
    if (theScene.getEnvironmentMode() == Scene.ENVIRON_SOLID)
      envPanel.setVisibleChild(0);
    else
      envPanel.setVisibleChild(1);
    envInfo.setTexture(theScene.getEnvironmentTexture(), theScene.getEnvironmentMapping());
    envSphere.setParameterValues(theScene.getEnvironmentParameterValues());
    envLabel.setText(envSphere.getTexture().getName());
    envChoice.addEventLink(ValueChangedEvent.class, new Object()
    {
      void processEvent()
      {
        if (envChoice.getSelectedIndex() == Scene.ENVIRON_SOLID)
          envPanel.setVisibleChild(0);
        else
          envPanel.setVisibleChild(1);
        envPanel.getParent().layoutChildren();
      }
    });
    final Runnable envTextureCallback = new Runnable() {
      public void run()
      {
        envLabel.setText(envSphere.getTexture().getName());
        envPanel.getParent().layoutChildren();
      }
    };
    envButton.addEventLink(CommandEvent.class, new Object()
    {
      void processEvent()
      {
        envPanel.getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ObjectTextureDialog otd = new ObjectTextureDialog(LayoutWindow.this, new ObjectInfo [] {envInfo}, true, false);
        otd.setCallback(envTextureCallback);
        envPanel.getParent().setCursor(Cursor.getDefaultCursor());
      }
    });
    ambPatch.addEventLink(MouseClickedEvent.class, new Object()
    {
      void processEvent()
      {
        new ColorChooser(LayoutWindow.this, Translate.text("ambientColor"), ambColor);
        ambPatch.setBackground(ambColor.getColor());
        ambPatch.repaint();
      }
    });
    envPatch.addEventLink(MouseClickedEvent.class, new Object()
    {
      void processEvent()
      {
        new ColorChooser(LayoutWindow.this, Translate.text("environmentColor"), envColor);
        envPatch.setBackground(envColor.getColor());
        envPatch.repaint();
      }
    });
    fogPatch.addEventLink(MouseClickedEvent.class, new Object()
    {
      void processEvent()
      {
        new ColorChooser(LayoutWindow.this, Translate.text("fogColor"), fogColor);
        fogPatch.setBackground(fogColor.getColor());
        fogPatch.repaint();
      }
    });
    Runnable okCallback = new Runnable() {
      public void run()
      {
        theScene.setFog(fogBox.getState(), fogField.getValue());
        theScene.setEnvironmentMode(envChoice.getSelectedIndex());
        theScene.setEnvironmentTexture(envSphere.getTexture());
        theScene.setEnvironmentMapping(envSphere.getTextureMapping());
        theScene.setEnvironmentParameterValues(envSphere.getParameterValues());
        setModified();
      }
    };
    Runnable cancelCallback = new Runnable() {
      public void run()
      {
        ambColor.copy(oldAmbColor);
        envColor.copy(oldEnvColor);
        fogColor.copy(oldFogColor);
      }
    };
    new ComponentsDialog(LayoutWindow.this, Translate.text("environmentTitle"),
        new Widget [] {ambPatch, envChoice, envPanel, fogBox, fogPatch, fogField},
        new String [] {Translate.text("ambientColor"), Translate.text("environment"), "", "", Translate.text("fogColor"), Translate.text("fogDistance")},
        okCallback, cancelCallback);
  }
    
    
    public void stopRotateScene(){
        rotateScene = false;
    }
    
    /**
     * rotateSceneCommand
     *
     * Description: Start a thread that rotates the scene or around a selected object while there is no user input to the scene.
     */
  private void rotateSceneCommand(){
      if(rotateScene){
          rotateScene = false;
          return;
      }
      
      rotateSceneThread = new Thread(){
          
          public void run(){
              
              while (rotateScene){
                  try {
                      Thread.sleep(250);
                      //Thread.sleep(25);
                  } catch (Exception e){
                      
                  }
      
                    double DRAG_SCALE = 0.01;
                    CoordinateSystem oldCoords;

                    ViewerCanvas view = getView();
                    Camera cam = view.getCamera();
                    oldCoords = cam.getCameraCoordinates().duplicate();
                    Mat4 viewToWorld;
                    viewToWorld = cam.getViewToWorld();
                    Vec3 rotationCenter;
                    rotationCenter = view.getRotationCenter();
                    if (rotationCenter == null)
                    rotationCenter = view.getDefaultRotationCenter();


                    Vec3 cameray = viewToWorld.timesDirection(Vec3.vy());
                    Vec3 cameraz = viewToWorld.timesDirection(Vec3.vz());
                    Vec3 vertical = cameray.times(cameray.y).plus(cameraz.times(cameraz.y));
                    if (vertical.length2() < 1e-5)
                    vertical = cameray;
                    else
                    vertical.normalize();

                    // Compute the rotation matrix.

                    //Point dragPoint = e.getPoint();
                    int dx = 2; //  dragPoint.x-clickPoint.x;
                    int dy = 0; //  dragPoint.y-clickPoint.y;
                    Mat4 rotation;

                    //if (controlDown)
                    //  rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vz()), -dx*DRAG_SCALE);
                    //else if (e.isShiftDown())
                    //  {
                    //    if (Math.abs(dx) > Math.abs(dy))
                    //      rotation = Mat4.axisRotation(vertical, -dx*DRAG_SCALE);
                    //    else
                    //      rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vx()), dy*DRAG_SCALE);
                    //  }
                    //else
                    //  {
                      rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vx()), dy*DRAG_SCALE);
                      rotation = Mat4.axisRotation(vertical, -dx*DRAG_SCALE).times(rotation);
                    //  }
                            if (!rotation.equals(Mat4.identity()))
                    {
                      CoordinateSystem c = oldCoords.duplicate();
                      c.transformCoordinates(Mat4.translation(-rotationCenter.x, -rotationCenter.y, -rotationCenter.z));
                      c.transformCoordinates(rotation);
                      c.transformCoordinates(Mat4.translation(rotationCenter.x, rotationCenter.y, rotationCenter.z));
                      view.getCamera().setCameraCoordinates(c);
                      view.viewChanged(false);
                      view.repaint();
                    }
                  
              }
          }
      };

      rotateScene = true;
      rotateSceneThread.start();
  }

  private void executeScriptCommand(CommandEvent ev)
  {
    executeScript(new File(ev.getActionCommand()));
  }

  /** Execute the tool script contained in a file, passing a reference to this window in its "window" variable. */

  public void executeScript(File f)
  {
    // Read the script from the file.

    String scriptText = null;
    String language = null;
    try
    {
      language = ScriptRunner.getLanguageForFilename(f.getName());
      scriptText = ArmaRender.loadFile(f);
    }
    catch (IOException ex)
    {
      new BStandardDialog("", new String [] {Translate.text("errorReadingScript"), ex.getMessage() == null ? "" : ex.getMessage()}, BStandardDialog.ERROR).showMessageDialog(this);
      return;
    }
    try
    {
      ToolScript script = ScriptRunner.parseToolScript(language, scriptText);
      script.execute(this);
    }
    catch (Exception e)
    {
      ScriptRunner.displayError(language, e);
    }
    updateImage();
    dispatchSceneChangedEvent(); // To be safe, since we can't rely on scripts to set undo records or call setModified().
  }


    /**
     * setModelingView
     *
     * Description: Set selected object view corrdinate mode to default modeling.
     */
    public void setModelingView(){
        hideInfographic();
        layoutModelingView = true;
        if(theScene != null){
            theScene.setLayoutViewModeling();
            itemTree.repaint(); // reload tree
            updateImage();
        }
    }
    
    /**
     * setLayoutView
     *
     * Description: Set selected object view corrdinate mode to alternate file based coords.
     *
     *    LayoutWindow.setLayoutView()
     *    Scene.setLayoutViewCutting()
     *    Scene.setChildObjectViewMode(obj, false)
     *    ObjectInfo.setLayoutView(layout)
     *    LayoutModeling.saveLayout(obj, coords)
     */
    public void setLayoutView(){
        hideInfographic();
        layoutModelingView = false;
        if(theScene != null){
            theScene.setLayoutViewCutting();
            itemTree.repaint(); // reload tree
            updateImage();
        }
    }
    
    public void resetLayoutView(){
        hideInfographic();
        //layoutModelingView = false;
        if(theScene != null){
            theScene.resetLayoutView();
            itemTree.repaint(); // reload tree
            updateImage();
        }
    }
    
    public void autoLayoutPlate(){
        hideInfographic();
        AutoLayoutPlate autoLayoutPlate = new AutoLayoutPlate();
        autoLayoutPlate.layout(this);
    }
    
    public void tubeNesting(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //Nesting nesting = new Nesting(this);
        //nesting.prompt();
    }
    
    // DEPRICATE
    public void setTubeLayoutView(){
        hideInfographic();
        //System.out.println("setTubeLayoutView  ********* ");
        layoutModelingView = false;
        if(theScene != null){
            theScene.setLayoutViewTube();
            itemTree.repaint(); // reload tree
            updateImage();
        }
    }
    
    public void debug(){
        if(theScene != null){
            theScene.debug();
        }
    }
    
    /**
     * print3DGCode
     *
     * Description:
     */
    public void print3DGCode(){
        hideInfographic();
        if(theScene != null){
            Print3D print3D = new Print3D();
            print3D.setScene(theScene);
            print3D.setObjects(theScene.getObjects());
            print3D.setLayoutWindow(this);
            if(print3D.getUserInput()){
                print3D.start();
            }
        }
    }
    
    /**
     * printFiveAxisGCode
     *
     * Description:
     */
    public void printFiveAxisGCode(){
        hideInfographic();
        
        System.out.println("Five Axis. ");
        FiveAxis fiveAxis = new FiveAxis();
        fiveAxis.setObjects(theScene.getObjects());
        fiveAxis.setScene(theScene);
        fiveAxis.setLayoutWindow(this);
       
        // Dialog
        if(fiveAxis.getUserInput()){
            //fiveAxis.exportGCode();
            fiveAxis.start();
        }
    }
    
    
    /**
     * controlCNCMachine
     * 
     */
    public void controlCNCMachine(){
        ControlCNCMachine control = new ControlCNCMachine();
    }

    /**
     * Table group by children
     */
    public void exportGroupGCode(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            //	theScene.exportGroupGCode();
            //ExportPolyTableGCode exportGcode = new ExportPolyTableGCode();
            //exportGcode.exportGroupGCode(theScene.getObjects(), theScene.getDirectory(), theScene.getName());
            //exportGcode.exportGroupGCode(theScene);
        }
    }
    
    /**
     * exportAllGCode
     */
    public void exportAllGCode(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            //theScene.exportGCode(true);
            //ExportPolyTableGCode exportGcode = new ExportPolyTableGCode();
            //exportGcode.exportAllGCode(theScene.getObjects(), theScene.getDirectory(), theScene.getName());
            //exportGcode.exportAllGCode(theScene);
        }
    }
    
    public void exportTubeGCode(){
        //System.out.println("exportTubeGCode");
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            //ExportTubeNotchGCode exportTubeGCode = new ExportTubeNotchGCode(this, false);
            //exportTubeGCode.exportTubeGCode(theScene);
            //theScene.exportTubeGCode();
        }
    }
    
    /**
     * exportTubeBendGCode
     *
     * Description: bend gcode from curve object profile. Only works on XY plane.
     */
    public void exportTubeBendGCode(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            //theScene.exportTubeBendGCode();
        }
    }
    
    public void exportArcTubeNotchGCode(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //ExportTubeNotchGCode exportTubeGCode = new ExportTubeNotchGCode(this, true);
    }
    
    public void exportArcTubeNotchGCode2(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        //ExportTubeNotchGCode exportTubeGCode = new ExportTubeNotchGCode(this, true);
    }
    
    /**
     * exportArcTubeBendGCode
     *
     * Description: generate gcode 
     */
    public void exportArcTubeBendGCode(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            //ExportTubeBendGCode arcBend = new ExportTubeBendGCode(this);
            //arcBend.exportArcTubeBendGCode();
        }
    }
    
    public void exportArcTubeBendPDF(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            //ExportTubeBendPDF arcBend = new ExportTubeBendPDF(this); // theScene
            //arcBend.exportArcTubeBendPDF();
        }
    }
    
    public void exportArcTubeNotchPDF(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            //ExportTubeNotchPDF arcNotch = new ExportTubeNotchPDF(this); // theScene
            //arcNotch.exportArcTubeNotchPDF();
        }
    }
    
    public void exportPlatePDF(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            //ExportPlatePDF platePDF = new ExportPlatePDF(this); // theScene
            //platePDF.exportPlatePDF();
        }
    }
    
    public void export3dGCode(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            theScene.export3dCode(this);
        }
    }
    
    public void export3dGCode2(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        if(theScene != null){
            theScene.export3dCode2(this);
        }
    }
    
    public void exportGCodeMesh(){
        System.out.println("exportGCodeMesh  ********* ");
        
        // theScene
        // private Vector<ObjectInfo> objects;
        
        if(theScene != null){
            theScene.exportGCodeMesh();
        }
    }
    
    public void exportLayoutDXF(){
        System.out.println("exportLayoutDXF  ********* ");
        
        // theScene
        // private Vector<ObjectInfo> objects;
        
        if(theScene != null){
            theScene.exportLayoutDXF();
        }
    }
    
    public void exportOBJ(){
        if(theScene != null){
            theScene.exportOBJ();
        }
    }

    /**
     * setGCodeExportScale
     *
     * Description: Prompt user for scene scale factor that adjusts the unit values.
     */
  public void setGCodeExportScale(){
      ScaleSettings scaleSettings = new ScaleSettings(this);
      scaleSettings.display();
      /*
	try {
		double scale = theScene.getScale();
        
		// Prompt for new value
		//JFrame myFrame = null;
		
		String answer = JOptionPane.showInputDialog("Enter an export scale factor",  Double.toString(scale));
        //String answer = JOptionPane.showMessageDialog(null, "Enter an export scale factor", "Scale", Double.toString(scale), JOptionPane.INFORMATION_MESSAGE);

        //String answer = JOptionPane.showInputDialog(null,
        //                                                  "Enter an export scale factor",
        //                                                  "Scale",
        //                                                  JOptionPane.INFORMATION_MESSAGE );
        //JOptionPane.showMessageDialog(null,"Ok Done");
        
		System.out.println("New GCode export scale: " + answer);

		// Save new value
		if(answer != null){
			scale = Double.parseDouble(answer);
            theScene.setScale(scale);
		}
	} catch (Exception e){
		System.out.println("Error " + e);
		e.printStackTrace();
	}
      */
  }


  // object property cut order.
  public void setGCodePolyOrder(){
	if(theScene != null){
		theScene.setGCodePolyOrder();
	}
  }
    
    /**
     *
     */
    public void setGCodePolyOrdersBySize(){
        if(theScene != null){
            theScene.setGCodePolyOrdersBySize();
        }
    }
    
  public void setGCodePointOffset(){
    if(theScene != null){
      theScene.setGCodePointOffset();
    }
  }

  public void setGCodePolyDepth(){
  	if(theScene != null){
  		theScene.setGCodePolyDepth();
  	}
  }

  public void setGCodeEnablePoly(){
	  if(theScene != null){
	  		theScene.gcodeEnablePoly();
	  }

  }

  public void setGCodeDisablePoly(){
	  if(theScene != null){
		theScene.gcodeDisablePoly();
	  }

  }
    
    public void setGCodeReverseOrder(){ 
        if(theScene != null){
            theScene.setGCodePolyReverseOrder();
        }
    }

    // ************************
    public void setObjectStructure(){
        if(theScene != null){
            theScene.setObjectStructure();
        }
    }

    public void runCrashSimulation(){
        if(theScene != null){
            theScene.runCrashSimulation(this);
        } 

        //CrashSimulation crash = new CrashSimulation(this);
        //if (!crash.clickedOk())
        //  return;

    }
    
    public void copyStructureObjects(){
        if(theScene != null){
            theScene.copyStructureObjects(this);
        }
    }
    
    public void generateStructureMesh(){
        if(theScene != null){
            theScene.generateStructureMesh(this);
        }
    }
    
    public void evenMesh(){
        if(theScene != null){
            theScene.evenMesh(this);
        }
    }
    
    
    public void clothSimulation(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        
        if(cloth == null){
            cloth = new Cloth(this);
        } else {
            // If running
            if(cloth.isRunning()){
                cloth.stop();
            } else {
                cloth.start();
            }
        }
        //cloth
    }
    
    
    public void arcTubeSoftBody(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
        
        // If main window is full screen, then resize to make room for routing dialog.
        Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Rectangle appBounds = getBounds();
        //System.out.println(" screenBounds " + screenBounds + "  appBounds: " + appBounds );
        if( screenBounds.getWidth() == appBounds.getWidth() ){
            Rectangle resizeBounds = new Rectangle((int)appBounds.getX(), (int)appBounds.getY(), (int)appBounds.getWidth() - 300, (int)appBounds.getHeight() ); //   appBounds;
            //resizeBounds.grow(0, -300 );
            setBounds(resizeBounds);
            //System.out.println("resize");
        }
        
        
        softBody = new ArcTubeSoftBody(this);
        softBody.setPrevBounds(appBounds);
    }
    
    public void beamNGCreateBeam(){
        //System.out.println("LayoutWindow - ExportBeamNGDrive");
        //if(theScene != null){
            //theScene.beamNGCreateBeam(this);
            //ExportBeamNGDrive export = new ExportBeamNGDrive(this);
            
        //}
    }
    
    public void exportBeamNGVehicle(){
        if(theScene != null){
            if(infoGraphicDialog != null){
                infoGraphicDialog.hide();
                infoGraphicDialog = null;
            }
            theScene.exportBeamNGVehicle(this);
        }
    }
    
    public void runCFD(){
        if(theScene != null){
            if(infoGraphicDialog != null){
                infoGraphicDialog.hide();
                infoGraphicDialog = null;
            }
            theScene.runCFD(this);
        }
    }
    
    public void stopCFD(){
        if(theScene != null){
            theScene.stopCFD(this);
        }
    }
    
    public void calibrateCFD(){
        if(theScene != null){
            theScene.calibrateCFD(this);
        }
    }
    
    public void frontalArea(){
        if(theScene != null){
            theScene.frontalArea(this);
        }
    }
    
    public void exportDXF(){
        if(theScene != null){
            theScene.exportDXF();
        } 
    }
    
    public void accountSettingsCommand(){
        //if(theScene != null){
        //}
        // Get file path
        Account account = new Account();
        account.displayForm();
    }
    

    public void exportObjectCSV(){
        if(theScene != null){
            theScene.exportObjectCSV(this);
        }
    }
    
    public void exportBeamNGDrive(){
        System.out.println("LayoutWindow - ExportBeamNGDrive");
        ExportBeamNGDrive export = new ExportBeamNGDrive(this);
    }
    
    public void setObjectGroupCommand(){
        if(theScene != null){
            theScene.setObjectGroup();
        }
    }
    
    
    public void exportSTL(){
        if(theScene != null){
            theScene.exportSTL();
        }
    }
    
    public void exportIGES(){
        ExportIGES iges = new ExportIGES();
        
        iges.export(theScene);
    }
    
    public void hideInfographic(){
        if(infoGraphicDialog != null){
            infoGraphicDialog.hide();
            infoGraphicDialog = null;
        }
    }
    
    
    public EditingTool getDefaultTool(){
        return defaultTool;
    }
    
    
    public void industrialDesignDrawingCommand(){
        IndustrialDesignDrawing idd = new IndustrialDesignDrawing(this);
    }
    
    
    public int getCurrentView(){
        return currentView;
    }
    
    /**
     * getCurrentViewCoords
     *
     * Description:
     */
    public CoordinateSystem getCurrentViewCoords(){
        CoordinateSystem cs = new CoordinateSystem();
        if(currentView > -1){
            SceneViewer sv = theView[currentView];
            CoordinateSystem coords = sv.getCamera().getCameraCoordinates();
            cs = coords;
        }
        return cs;
    }
    
    public double getCurrentViewScale(){
        double scale = 100;
        if(currentView > -1){
            SceneViewer sv = theView[currentView];
            scale = sv.getScale();
        }
        return scale;
    }
    
    public boolean isCurrentViewPerspective(){
        boolean perspective = false;
        if(currentView > -1){
            SceneViewer sv = theView[currentView];
            perspective = sv.isPerspective();
        }
        return perspective;
    }
    
    /**
     * setMaterialSelectionIndicator
     *
     * Description: The scene selction has changed and a material selection needs to be updated in the UI.
     *
     * @param: Material id index.
     */
    public void setMaterialSelectionIndicator(int matId){
        if(matId < 0){
            materialButton.setText("Material: ---");
        } else {
            // Look up Material name
            PhysicalMaterial pm = new PhysicalMaterial(this, this, theScene);
            pm.loadMaterials();
            PhysicalMaterial.MaterialContainer mc = pm.getMaterialById(matId);
            if(mc != null){
                materialButton.setText("Material: " + mc.name);
            }
        }
    }
    
    /**
     * createHelpMenu
     *
     * Description:
     */
    private void createHelpMenu(){
        helpMenu = Translate.menu("Help");
        menubar.add(helpMenu);
        
      
        
        BMenuItem helpAboutMenuItem = Translate.menuItem("About Arma Design Studio", this, "helpAbout");
        //helpAboutMenuItem.setName(null); // Name is used by event handler for ID
        helpAboutMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
        helpAboutMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
        helpMenu.add(helpAboutMenuItem);
        
        
        BMenuItem soundMenuItem = Translate.menuItem("Sound Test", this, "soundTest");
        //helpAboutMenuItem.setName(null); // Name is used by event handler for ID
        soundMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
        soundMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
        helpMenu.add(soundMenuItem);
        
        
    }
    
    /**
     * helpAbout
     *
     */
    public void helpAbout(){
        Splash splash = new Splash();
    }
    
    public void soundTest(){
        Sound.playSound("success.wav");
    }
    
    private void createExamplesMenu(){
        examplesMenu = Translate.menu("Examples");
        menubar.add(examplesMenu);
        
        BMenuItem examplesRunMenuItem = Translate.menuItem("Examples Run", this, "runExamplesCommand");
        //examplesRunMenuItem.setName(null); // Name is used by event handler for ID
        //examplesRunMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
        //examplesRunMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
        examplesMenu.add(examplesRunMenuItem);
        
        
        
        
        BMenuItem examples2RunMenuItem = Translate.menuItem("Collision Dist", this, "runExamples2Command");
        //examplesRunMenuItem.setName(null); // Name is used by event handler for ID
        //examplesRunMenuItem.addEventLink(MouseEnteredEvent.class, this, "mouseEnteredEvent");
        //examplesRunMenuItem.addEventLink(MouseExitedEvent.class, this, "mouseExitedEvent");
        examplesMenu.add(examples2RunMenuItem);
        
        
        BMenuItem constructRouterGeometryMenuItem = Translate.menuItem("Construct Router Geometry", this, "constructRouterGeometryCommand");
        examplesMenu.add(constructRouterGeometryMenuItem);
        
        
        BMenuItem threePlusTwoXFourMenuItem = Translate.menuItem("3+2 x 4 directions", this, "threePlusTwoXFourCommand");
        examplesMenu.add(threePlusTwoXFourMenuItem);
        
        
    }
    
    
    public void runExamplesCommand(){
        Examples ex = new Examples();
        ex.demo(this);
    }
    
    public void runExamples2Command(){
        Examples ex = new Examples();
        ex.intersectDistanceDemo(this);
    }
    
    public void constructRouterGeometryCommand(){
        Examples ex = new Examples();
        ex.constructRouterGeometry(this);
    }
    
    public void threePlusTwoXFourCommand(){
        Examples ex = new Examples();
        ex.threePlusTwoXFour(this);
    }

/**
     * printFiveAxisGCode
     *
     * Description: Load Prompt and generate router toolpath based on scene.
     */
    public void printFiveAxisRoughingGCode(){
        hideInfographic();
        
        //System.out.println("Five Axis. ");
        FiveAxisRoughing fiveAxis = new FiveAxisRoughing();
        fiveAxis.setObjects(theScene.getObjects());
        fiveAxis.setScene(theScene);
        fiveAxis.setLayoutWindow(this);
       
        // Dialog
        if(fiveAxis.getUserInput()){
            //fiveAxis.exportGCode();
            fiveAxis.start();
        }
    }
    
    /**
     * printFiveAxisGCode
     *
     * Description: Load Prompt and generate router toolpath based on scene.
     */
    public void printFiveAxisFinishingGCode(){
        hideInfographic();
        
        //System.out.println("Five Axis. ");
        FiveAxisFinishing fiveAxis = new FiveAxisFinishing();
        fiveAxis.setObjects(theScene.getObjects());
        fiveAxis.setScene(theScene);
        fiveAxis.setLayoutWindow(this);
       
        // Dialog
        if(fiveAxis.getUserInput()){
            //fiveAxis.exportGCode();
            fiveAxis.start();
        }
    }

// faster - experimental
    public void addObjects(Vector<ObjectInfo> infos, UndoRecord undo){
        theScene.addObjects(infos, undo);
        //for(int i = 0; i < infos.size(); i++){
        //    ObjectInfo info = infos.elementAt(i);
            //itemTree.addElement(new ObjectTreeElement(info, itemTree)); // performance is bad
        //}
        for (int i = 0; i < theView.length ; i++)
          theView[i].rebuildCameraList();
        theScore.rebuildList();
        
        rebuildItemList(); // updates the object tree once after all the objects are added.
        // itemTree.repaint();
    }

}
