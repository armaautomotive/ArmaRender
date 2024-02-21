#AdsRenderSource
# requires: https://bell-sw.com/pages/downloads/native-image-kit/#nik-23-(jdk-21)

# Generate config files.
# java -agentlib:native-image-agent=config-output-dir=graal_conf -jar ArmaRender.jar

# /Library/Java/LibericaNativeImageKit/liberica-vm-full-23.1.1-openjdk21/Contents/Home/bin/java -agentlib:native-image-agent=config-merge-dir=graal_conf -jar ArmaRender.jar

# Build native
/Library/Java/LibericaNativeImageKit/liberica-vm-full-23.1.1-openjdk21/Contents/Home/bin/native-image \
 -Djava.awt.headless=false \
    -H:ReflectionConfigurationFiles=graal_conf/reflect-config.json \
    -H:ResourceConfigurationFiles=graal_conf/resource-config.json  \
    -H:DynamicProxyConfigurationFiles=graal_conf/proxy-config.json \
    -H:JNIConfigurationFiles=graal_conf/jni-config.json \
 -H:+UnlockExperimentalVMOptions \
 -H:+UnlockExperimentalVMOptions \
 -H:+UnlockExperimentalVMOptions \
 -H:+UnlockExperimentalVMOptions \
 --enable-url-protocols=http \
    -jar ArmaRender.jar \
    -H:Name=/Users/jon/NextCloud/ArmaAutomotive/AdsRenderSource/ArmaRender_Native

