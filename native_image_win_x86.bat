:: ADS
:: requires: https://bell-sw.com/pages/downloads/native-image-kit/#nik-23-(jdk-21)
::

native-image ^
 -Djava.awt.headless=false ^
 -H:ConfigurationFileDirectories=graal_conf_win_x86 ^
 --strict-image-heap -march=native --no-fallback ^
 --enable-url-protocols=http ^
 -jar ArmaRender.jar ^
 -H:Name=ArmaRender_Win_x86


