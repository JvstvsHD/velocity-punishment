# Velocity Punishment
Velocity punishment is a punishment plugin designed for
[velocity](https://velocitypowered.com).
The plugin is in the development stage at the moment. There will be a downloadable jar file
soon.

## API
This plugin also contains an API which partially exist yet.

### Installation
Replace ```{version}``` with the current version, e.g. 1.0.0.
#### Gradle (kotlin)
```kotlin
repositories {
    mavenCentral()    
}

depenencies {
    implementation("de.jvstvshd.punishment:velocitypunishment:{version}")
}
```

#### Gradle (groovy)
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'de.jvstvshd.punishment:velocitypunishment:{version}'
}
```
#### Maven
```xml
<dependencies>
    <dependency>
        <groupId>de.jvstvshd.punishment</groupId>
        <artifactId>velocitypunishment</artifactId>
        <version>{version}</version>
    </dependency>
</dependencies>
```