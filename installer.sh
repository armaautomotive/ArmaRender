#
# Generate an installer
# 
javapackager -deploy -native -outdir . -outfile ArmaDesignStudio -srcdir . -srcfiles ArmaDesignStudio.jar -appclass armadesignstudio.ArmaDesignStudio -name "ArmaDesignStudio_Installer"  -title "Arma Design Studio"
