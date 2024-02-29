# ADS
# requires: https://bell-sw.com/pages/downloads/native-image-kit/#nik-23-(jdk-21)

# Generate config files.
# java -agentlib:native-image-agent=config-output-dir=graal_conf -jar ArmaDesignStudio.jar

# /Library/Java/LibericaNativeImageKit/liberica-vm-full-23.1.1-openjdk21/Contents/Home/bin/java -agentlib:native-image-agent=config-merge-dir=graal_conf -jar ArmaDesignStudio.jar

# Build native
#/Library/Java/JavaVirtualMachines/graalvm-jdk-21.0.1+12.1/Contents/Home/bin/native-image \

native-image \
 -Djava.awt.headless=false \
 -H:ReflectionConfigurationFiles=graal_conf_linux_x86/reflect-config.json \
 -H:ResourceConfigurationFiles=graal_conf_linux_x86/resource-config.json  \
 -H:DynamicProxyConfigurationFiles=graal_conf_linux_x86/proxy-config.json \
 -H:JNIConfigurationFiles=graal_conf_linux_x86/jni-config.json \
 -H:+UnlockExperimentalVMOptions \
 -H:+UnlockExperimentalVMOptions \
 -H:+UnlockExperimentalVMOptions \
 -H:+UnlockExperimentalVMOptions \
 --enable-url-protocols=http \
 -jar ArmaDesignStudio.jar \
 -H:Name=ArmaDesignStudio_linux_x86



