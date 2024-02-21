::
:: Generate an installer
:: 
::javapackager -deploy -native exe -outdir . -outfile ArmaDesignStudio -srcdir . -srcfiles ArmaDesignStudio.jar -appclass armadesignstudio.ArmaDesignStudio -name "ArmaDesignStudio"  -title "Arma Design Studio"

:: --input installer/input
jpackage --name ArmaDesignStudio --input installer/  --main-jar ArmaDesignStudio.jar --main-class armadesignstudio.ArmaDesignStudio