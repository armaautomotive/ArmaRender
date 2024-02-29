:: 
:: Run the ADS ant build process.
:: > build.bat - will compile and build the project jar file.
:: > build.bat exe - will compile and generate a windows exe binary.
::

#ant -buildfile buoy.xml

ant -buildfile buoyx.xml


ant -buildfile ArmaRender.xml %1
