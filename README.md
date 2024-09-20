Arma Render 

Arma Render is a sandbox environment for developing test features.


Required Software Install: 
You will need to install Java JDK version 8 or higher and Apache Ant which is a build system.

Java: https://www.oracle.com/java/technologies/javase/jdk23-readme-downloads.html
Apache Ant: https://ant.apache.org/bindownload.cgi 
Git: May be installed on OSX. Or GitHub.com for Windows.


Project Checkout:

pull the project from: https://github.com/armaautomotive/ArmaRender



Run the project 

Windows:

> build.bat
> run.bat

Mac/Linux:

> ./buid.sh
> ./run.sh



A test file is located in the project named: mill_test.ads


Testing 3 axis milling:

open the test file mill_test.ads, click the menu item 'CAM' -> 'Export 3-Axis Top Down Layed Gcode'.
Check the 'Tool Path Markup' check box.
Set Accuracy to .1.
Set Drill Bit diameter to 0.125.

Click OK.



Testing the 5 axis demo:

open the test file mill_test.ads, click the menu item 'CAM' -> 'Export 5-Axis Finishing (Top Down) GCode'.
Check the 'Simulate Route' checkbox.
Set Accuracy to .1.
Set Drill Bit diameter to 0.125.

Click OK.

View exported GCode: /ArmaRender/mill_test/mill5axis/finishing_0.gcode

