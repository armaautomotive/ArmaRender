package buoyx.docking;

import buoy.widget.*;
import buoy.widget.BTabbedPane.TabPosition;
import buoy.internal.*;
import buoy.event.*;

import javax.swing.*;
import java.util.*;
import java.awt.*;

/**
 * A DockingContainer contains a single content Widget, plus any number of {@link DockableWidget DockableWidgets}.
 * The DockableWidgets are arranged along one edge of the content Widget, divided into a set of tabs.
 * The number of tabs, the set of DockableWidgets in each tab, and the order in which they appear are all
 * configurable.
 * <p>
 * Once you create one or more DockingContainers and add DockableWidgets to them, the user is free to rearrange
 * them by dragging them with the mouse.  This includes reordering the Widgets in a tab, moving them between
 * tabs, moving a Widget from one DockingContainer to a different DockingContainer in the same window, and
 * detaching a Widget so that it appears in a separate dialog.
 * <p>
 * Whenever the user performs a drag, a {@link DockingEvent} is dispatched to report the event.  When the
 * drag is from one DockingContainer to another one, both the source and target containers will dispatch
 * events.  For drags within a single container, only one event is generated.
 *
 * @author Peter Eastman
 */

public class DockingContainer extends WidgetContainer
{
  private Widget content;
  private ArrayList<ArrayList<DockableWidget>> childrenInTab;
  private TabPosition tabPosition;
  private BSplitPane splitPane;     // splits main content and docking containers
  private BTabbedPane tabs;         // right had side widgets.
  private boolean hideSingleTab;
  private int visibleDividerSize;

  /**
   * Create a DockingContainer with no children and tabs along its top edge.
   */

  public DockingContainer()
  {
    component = new WidgetContainerPanel(this);
    splitPane = new BSplitPane(BSplitPane.HORIZONTAL);
    ((JSplitPane) splitPane.getComponent()).setBorder(null);
    tabPosition = BTabbedPane.TOP;
    ((Container) getComponent()).add(splitPane.getComponent());
    setAsParent(splitPane);
    childrenInTab = new ArrayList<ArrayList<DockableWidget>>();
    visibleDividerSize = ((JSplitPane) splitPane.getComponent()).getDividerSize();
  }

  /**
   * Create a DockingContainer.
   *
   * @param content      the content Widget
   * @param tabPosition  this specifies which side of the content Widget the DockableWidgets will be
   *                     placed on
   */

  public DockingContainer(Widget content, TabPosition tabPosition)
  {
    this();
    setTabPosition(tabPosition);
    setContent(content);
  }

  /**
   * Get the content Widget.
   */

  public Widget getContent()
  {
    return content;
  }

  /**
   * Set the content Widget.
   */

  public void setContent(Widget widget)
  {
    if (widget != null && widget.getParent() != null)
      widget.getParent().remove(widget);
    if (content != null)
      splitPane.remove(content);
    content = widget;
    if (content != null)
    {
      splitPane.add(content, tabPosition == BTabbedPane.BOTTOM || tabPosition == BTabbedPane.RIGHT ? 0 : 1);
      setAsParent(content);
    }
  }

  /**
   * Add a DockableWidget to this container.  It will be placed in a new tab.
   */
  public void addDockableWidget(DockableWidget widget)
  {
    if (widget.getParent() != null)
      widget.getParent().remove(widget);
    ArrayList<DockableWidget> newTab = new ArrayList<DockableWidget>();
    newTab.add(widget);
    childrenInTab.add(newTab);
    rebuildContents(getDockedChildCount() > 1 && (content == null || !content.getBounds().equals(splitPane.getBounds())));
  }

  /**
   * Add a new DockableWidget to this container.
   *
   * @param widget      the Widget to add
   * @param tab         the index of the tab in which to place the Widget
   * @param indexInTab  the position within the tab at which the Widget should appear
   */
  public void addDockableWidget(DockableWidget widget, int tab, int indexInTab)
  {
    if (widget.getParent() != null)
      widget.getParent().remove(widget);
    if (tab == childrenInTab.size() && indexInTab == 0)
    {
      addDockableWidget(widget);
      return;
    }
    childrenInTab.get(tab).add(indexInTab, widget);
    rebuildContents(getDockedChildCount() > 1 && (content == null || !content.getBounds().equals(splitPane.getBounds())));
  }

  /**
   * Get the index of the tab in which a DockableWidget appears.  If it is not a child of this
   * container, -1 is returned.
   */

  public int getChildTabIndex(DockableWidget widget)
  {
    for (int i = 0; i < childrenInTab.size(); i++)
      if (childrenInTab.get(i).contains(widget))
        return i;
    return -1;
  }

  /**
   * Get the position within its tab at which a DockableWidget appears.  If it is not a child of this
   * container, -1 is returned.
   */

  public int getChildIndexInTab(DockableWidget widget)
  {
    for (ArrayList<DockableWidget> children : childrenInTab)
    {
      int index = children.indexOf(widget);
      if (index > -1)
        return index;
    }
    return -1;
  }

  /**
   * Get the number of tabs in this container.
   */

  public int getTabCount()
  {
    return childrenInTab.size();
  }

  /**
   * Get the number of DockableWidgets within a particular tab.
   */

  public int getTabChildCount(int tabIndex)
  {
    return childrenInTab.get(tabIndex).size();
  }

  /**
   * Get which side of the content Widget the DockableWidgets appear on.
   */

  public TabPosition getTabPosition()
  {
    return tabPosition;
  }

  /**
   * Set which side of the content Widget the DockableWidgets appear on.
   */

  public void setTabPosition(TabPosition position)
  {
    tabPosition = position;
    splitPane.setOrientation(tabPosition == BTabbedPane.TOP || tabPosition == BTabbedPane.BOTTOM ? BSplitPane.VERTICAL : BSplitPane.HORIZONTAL);
    if (content != null)
    {
      Widget theContent = content;
      remove(content);
      setContent(theContent);
    }
    rebuildContents(true);
  }

  /**
   * Get which tab is currently displayed.
   */

  public int getSelectedTab()
  {
    if (tabs == null)
      return (childrenInTab.size() == 0 ? -1 : 0);
    return tabs.getSelectedTab();
  }

  /**
   * Set which tab is currently displayed.
   */

  public void setSelectedTab(int index)
  {
    if (tabs != null)
      tabs.setSelectedTab(index);
  }

  /**
   * When all DockableWidgets are grouped into a single tab, it is possible to save space by hiding the tab.
   * This method gets whether that option is enabled.
   */

  public boolean getHideSingleTab()
  {
    return hideSingleTab;
  }

  /**
   * When all DockableWidgets are grouped into a single tab, it is possible to save space by hiding the tab.
   * This method sets whether that option is enabled.
   */

  public void setHideSingleTab(boolean hide)
  {
    hideSingleTab = hide;
    if (childrenInTab.size() == 1)
      rebuildContents(true);
  }

  /**
   * Reset the positions of the dividers within a single tab based on the preferred sizes of the
   * DockableWidgets it contains.
   */

  public void resetToPreferredSizes(int tabIndex)
  {
    Widget child;
    if (tabs == null)
    {
      int dockPosition = (tabPosition == BTabbedPane.BOTTOM || tabPosition == BTabbedPane.RIGHT ? 1 : 0);
      child = splitPane.getChild(dockPosition);
    }
    else
    {
      child = tabs.getChild(tabIndex);
    }
    if (child != null)
      recursivelyResetToPreferredSizes(child.getComponent());
  }

  private void recursivelyResetToPreferredSizes(Component c)
  {
    if (c instanceof SingleWidgetPanel)
      c = ((SingleWidgetPanel) c).getComponent(0);
    if (c instanceof JSplitPane)
    {
      JSplitPane js = (JSplitPane) c;
      js.resetToPreferredSizes();
      recursivelyResetToPreferredSizes(js.getTopComponent());
      recursivelyResetToPreferredSizes(js.getBottomComponent());
    }
  }

  Rectangle getTabBounds(int index)
  {
    if (tabs == null)
    {
      Rectangle bounds = getBounds();
      final int TAB_REGION_SIZE = 30;
      if (tabPosition == BTabbedPane.TOP)
        return new Rectangle(0, 0, bounds.width, TAB_REGION_SIZE);
      if (tabPosition == BTabbedPane.BOTTOM)
        return new Rectangle(0, bounds.height-TAB_REGION_SIZE, bounds.width, TAB_REGION_SIZE);
      if (tabPosition == BTabbedPane.LEFT)
        return new Rectangle(0, 0, TAB_REGION_SIZE, bounds.height);
      return new Rectangle(bounds.width-TAB_REGION_SIZE, 0, TAB_REGION_SIZE, bounds.height);
    }
      
    Rectangle tabBounds;
    if (index < getTabCount())
      tabBounds = ((JTabbedPane) tabs.getComponent()).getBoundsAt(index);
    else
    {
      int lastVisibleTab = getTabCount()-1;
      Rectangle lastTabBounds;
      do
      {
        lastTabBounds = ((JTabbedPane) tabs.getComponent()).getBoundsAt(lastVisibleTab);
        if (lastTabBounds == null)
          lastVisibleTab--;
        if (lastVisibleTab == -1)
          return null;
      } while (lastTabBounds == null);
      Rectangle bounds = tabs.getBounds();
      if (tabPosition == BTabbedPane.TOP || tabPosition == BTabbedPane.BOTTOM)
        tabBounds = new Rectangle(lastTabBounds.x+lastTabBounds.width, lastTabBounds.y,
            bounds.x+bounds.width-lastTabBounds.x-lastTabBounds.width, lastTabBounds.height);
      else
        tabBounds = new Rectangle(lastTabBounds.x, lastTabBounds.y+lastTabBounds.height,
            lastTabBounds.width, bounds.y+bounds.height-lastTabBounds.y-lastTabBounds.height);
    }
    if (tabBounds == null)
      return null;
    if (tabs != null)
    {
      Container parent = tabs.getComponent().getParent();
        
        //parent.setBackground(new Color(0,255, 0)); // nothing.
        
      while (parent != getComponent())
      {
        tabBounds.x += parent.getX();
        tabBounds.y += parent.getY();
        parent = parent.getParent();
      }
    }
    return tabBounds;
  }

    /**
     * rebuildContents
     *
     */
  private void rebuildContents(boolean preserveSize)
  {
    int mainPosition = (tabPosition == BTabbedPane.BOTTOM || tabPosition == BTabbedPane.RIGHT ? 0 : 1);
    BSplitPane.Orientation splitOrient = (tabPosition == BTabbedPane.LEFT || tabPosition == BTabbedPane.RIGHT ? BSplitPane.VERTICAL : BSplitPane.HORIZONTAL);
    splitPane.setResizeWeight(tabPosition == BTabbedPane.BOTTOM || tabPosition == BTabbedPane.RIGHT ? 1 : 0);
    int splitLocation = splitPane.getDividerLocation();

    // Mark every child as not having a parent, so they won't get confused when we add them again.

    for (Widget child : getChildren())
      if (child != content)
        removeAsParent(child);
    splitPane.remove(1-mainPosition);

    // Work out the contents for each tab.

    if (getDockedChildCount() == 0)
    {
      tabs = null;
      splitPane.remove(1-mainPosition);
      ((JSplitPane) splitPane.getComponent()).setDividerSize(0);
      return;
    }
    ((JSplitPane) splitPane.getComponent()).setDividerSize(visibleDividerSize); // center / right (horizontal) split pane
      
      
      //((JSplitPane) splitPane.getComponent()).setOpaque(true); // JDT    vert main container and right
      //((JSplitPane) splitPane.getComponent()).setBackground(new Color(105, 100, 255));
      
    Widget tabContents[] = new Widget [childrenInTab.size()];
      
      //System.out.println("tabContents.length: " +tabContents.length); // 1
      
    for (int i = 0; i < tabContents.length; i++)
    {
      ArrayList<DockableWidget> children = childrenInTab.get(i);
        //System.out.println("children.size(): " +children.size());  // 2, and 3
      if (children.size() == 1)
        tabContents[i] = children.get(0);
        
      else
      {
        BSplitPane split = new BSplitPane(splitOrient);
          
          //((JSplitPane) split.getComponent()).setOpaque(true); // JDT
          //((JSplitPane) split.getComponent()).setBackground(new Color(255, 100, 0)); // border around all right side region. (3 components)
          
          //JSplitPane getComponent()
          
        split.setResizeWeight(1.0/children.size());
        tabContents[i] = split;
        split.add(children.get(0), 0);
        for (int j = 1; j < children.size()-1; j++)
        {
          BSplitPane nextSplit = new BSplitPane(splitOrient);
          ((JSplitPane) nextSplit.getComponent()).setBorder(null);  // bottom two dock components
          nextSplit.setResizeWeight(1.0/(children.size()-j));
          split.add(nextSplit, 1);
          split = nextSplit;
          split.add(children.get(j), 0);
            
            ((JSplitPane) nextSplit.getComponent()).setOpaque(true); // JDT  NO Effect
            ((JSplitPane) nextSplit.getComponent()).setBackground(new Color(255, 100, 0)); // NO EFFECT
            
            
            //System.out.println("     j: " + j );
        }
        split.add(children.get(children.size()-1), 1);
      }
    }

    // Add the tabs.
      //System.out.println("tabContents.length: " + tabContents.length); // 1

    if (tabContents.length == 1 && hideSingleTab)
    {
      splitPane.add(tabContents[0], 1-mainPosition);
      tabs = null;
        
        //System.out.println(" --- tabContents.length == 1 ");
        
    }
    else if (tabContents.length > 0)
    {
      tabs = new BTabbedPane(tabPosition);
      DragManager manager = DragManager.getDragManager();
      tabs.addEventLink(MousePressedEvent.class, this, "mousePressedOnTab");
      tabs.addEventLink(MouseDraggedEvent.class, manager, "mouseDragged");
      tabs.addEventLink(MouseReleasedEvent.class, manager, "mouseReleased");
      for (int i = 0; i < tabContents.length; i++)
      {
        ArrayList<DockableWidget> children = childrenInTab.get(i);
        StringBuffer label = new StringBuffer();
        for (int j = 0; j < children.size(); j++)
        {
          if (j > 0)
            label.append(", ");
          label.append(children.get(j).getLabel());
        }
        tabs.add(tabContents[i], label.toString());
          
          //tabContents[i].setBackground(new Color(23, 230, 0)); //  no
      }
      splitPane.add(tabs, 1-mainPosition);
        
    }

      // ArrayList<ArrayList<DockableWidget>> childrenInTab
      for(int i = 0; i < childrenInTab.size(); i++){
          ArrayList<DockableWidget> curr = childrenInTab.get(i);
          for(int j = 0; j < curr.size(); j++){
              DockableWidget currWidget = curr.get(j);
           
              //System.out.println(" currWidget: " + currWidget.getLabel() + "  parent: " + currWidget.getParent() );
              
              if(currWidget.getParent() instanceof buoy.widget.BSplitPane){
                  //System.out.println(" YES ");
                  BSplitPane bSplitPane = (BSplitPane)currWidget.getParent();
                  
                  //JSplitPane jPane = bSplitPane.getComponent();
                  //jPane.setOpaque(true);
                  //jPane.setBackground(new Color(255, 255, 0));  // vertical
                  
                  //Component tempRight = jPane.getRightComponent(); // buoy.internal.SingleWidgetPanel
                  //System.out.println(" right " + tempRight.getClass().getName() );
                  
                  
                  //((JSplitPane) bSplitPane.getComponent()).setOpaque(true); // JDT  NO
                  //((JSplitPane) bSplitPane.getComponent()).setBackground(new Color(255, 255, 0));
              }
          }
      }
      
    // Reestablish that all the children belong to this container, not to the internal
    // BSplitPanes and BTabbedPane.

    for (ArrayList<DockableWidget> children : childrenInTab)
      for (DockableWidget widget : children)
        setAsParent(widget);
    if (preserveSize){
      splitPane.setDividerLocation(splitLocation);
    } else {
      splitPane.resetToPreferredSizes();
    }
    invalidateSize();
      
      /*
      Component tempA = ((JSplitPane) splitPane.getComponent()).getRightComponent(); // buoy.internal.SingleWidgetPanel
      System.out.println(" right " + tempA.getClass().getName() );
      
      Component tempTop = ((JSplitPane) splitPane.getComponent()).getTopComponent(); // buoy.internal.SingleWidgetPanel
      System.out.println(" tempTop " + tempTop.getClass().getName() );
      
      Component tempBot = ((JSplitPane) splitPane.getComponent()).getBottomComponent(); // buoy.internal.SingleWidgetPanel
      System.out.println(" tempBot " + tempBot.getClass().getName() );
       */
  }

  private void mousePressedOnTab(MousePressedEvent ev)
  {
    Point pos = ev.getPoint();
    JTabbedPane tp = (JTabbedPane) tabs.getComponent();
    for (int i = 0; i < getTabCount(); i++)
    {
      Rectangle bounds = tp.getBoundsAt(i);
      if (bounds.contains(pos))
      {
        bounds.x -= pos.x;
        bounds.y -= pos.y;
        DragManager.getDragManager().beginDraggingTab(this, i, bounds);
      }
    }
  }

  public int getChildCount()
  {
    int count = (content == null ? 0 : 1);
    for (ArrayList<DockableWidget> tab : childrenInTab)
      count += tab.size();
    return count;
  }

  private int getDockedChildCount()
  {
    return getChildCount()-(content == null ? 0 : 1);
  }

  public Collection<Widget> getChildren()
  {
    ArrayList<Widget> children = new ArrayList<Widget>(0);
    if (content != null)
      children.add(content);
    for (ArrayList<DockableWidget> thisTab : childrenInTab)
      children.addAll(thisTab);
    return children;
  }

  /**
   * Get a DockableWidget contained in this container.
   *
   * @param tab     the index of the tab in which the Widget appears
   * @param index   the index of the Widget within its tab
   */

  public DockableWidget getChild(int tab, int index)
  {
    return (DockableWidget) ((ArrayList) childrenInTab.get(tab)).get(index);
  }

  public void remove(Widget widget)
  {
    if (widget == content)
      setContent(null);
    else
    {
      for (int i = 0; i < childrenInTab.size(); i++)
      {
        ArrayList thisTab = (ArrayList) childrenInTab.get(i);
        int index = thisTab.indexOf(widget);
        if (index > -1)
        {
          thisTab.remove(index);
          removeAsParent(widget);
          if (thisTab.size() == 0)
            childrenInTab.remove(i);
          rebuildContents(getDockedChildCount() > 0);
          return;
        }
      }
    }
  }

  public void removeAll()
  {
    for (ArrayList<DockableWidget> thisTab : childrenInTab)
      for (DockableWidget widget : thisTab)
        removeAsParent(widget);
    childrenInTab.clear();
    setContent(null);

  }

  public void layoutChildren()
  {
    splitPane.getComponent().setBounds(getBounds());
    splitPane.layoutChildren();
  }

  public Dimension getMinimumSize()
  {
    return splitPane.getMinimumSize();
  }

  public Dimension getPreferredSize()
  {
    return splitPane.getPreferredSize();
  }

  /**
   * Get the BSplitPane which separates the content Widget from the DockableWidgets.
   */

  public BSplitPane getSplitPane()
  {
    return splitPane;
  }
}
