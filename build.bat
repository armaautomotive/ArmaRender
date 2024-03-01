:: 
:: Run the ADS ant build process.
:: > build.bat - will compile and build the project jar file.
:: > build.bat exe - will compile and generate a windows exe binary.
::

::ant -buildfile buoy.xml
::ant -buildfile buoyx.xml
::ant -buildfile Renderers.xml
::ant -buildfile SPManager.xml  # ???
::ant -buildfile HelpPlugin.xml
::ant -buildfile Tools.xml
::ant -buildfile Translators.xml
ant -buildfile ArmaRender.xml %1
