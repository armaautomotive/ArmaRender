#
# Run the ant build process
#
ant -buildfile Renderers.xml
ant -buildfile buoy.xml
ant -buildfile buoyx.xml
ant -buildfile SPManager.xml  # ???
#ant -buildfile HelpPlugin.xml
ant -buildfile Tools.xml
ant -buildfile Translators.xml
ant -buildfile ArmaRender.xml
