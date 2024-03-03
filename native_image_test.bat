::AdsRenderSource
:: requires: https://bell-sw.com/pages/downloads/native-image-kit/#nik-23-(jdk-21)
:: Generate config files.
:: java -agentlib:native-image-agent=config-output-dir=graal_conf -jar ArmaRender.jar
:: /Library/Java/LibericaNativeImageKit/liberica-vm-full-23.1.1-openjdk21/Contents/Home/bin/java -agentlib:native-image-agent=config-merge-dir=graal_conf -jar ArmaRender.jar
:: Build native


native-image -Djava.awt.headless=false ^
 -H:ReflectionConfigurationFiles=graal_config_test/reflect-config.json ^
 -H:ResourceConfigurationFiles=graal_config_test/resource-config.json  ^
 -H:DynamicProxyConfigurationFiles=graal_config_test/proxy-config.json ^
 -H:JNIConfigurationFiles=graal_config_test/jni-config.json ^
 --enable-url-protocols=http ^
 -jar Test.jar ^
 -H:Name=Test_Native

