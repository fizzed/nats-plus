# Nats Plus by Fizzed

[![Maven Central](https://img.shields.io/maven-central/v/com.fizzed/nats-plus?style=flat-square)](https://mvnrepository.com/artifact/com.fizzed/nats-plus)

## Automated Testing

The following Java versions and platforms are tested using GitHub workflows:

[![Java 11](https://img.shields.io/github/actions/workflow/status/fizzed/nats-plus/java11.yaml?branch=master&label=Java%2011&style=flat-square)](https://github.com/fizzed/nats-plus/actions/workflows/java11.yaml)
[![Java 17](https://img.shields.io/github/actions/workflow/status/fizzed/nats-plus/java17.yaml?branch=master&label=Java%2017&style=flat-square)](https://github.com/fizzed/nats-plus/actions/workflows/java17.yaml)
[![Java 21](https://img.shields.io/github/actions/workflow/status/fizzed/nats-plus/java21.yaml?branch=master&label=Java%2021&style=flat-square)](https://github.com/fizzed/nats-plus/actions/workflows/java21.yaml)

[![Linux x64](https://img.shields.io/github/actions/workflow/status/fizzed/nats-plus/java8.yaml?branch=master&label=Linux%20x64&style=flat-square)](https://github.com/fizzed/nats-plus/actions/workflows/java8.yaml)
[![MacOS arm64](https://img.shields.io/github/actions/workflow/status/fizzed/nats-plus/macos-arm64.yaml?branch=master&label=MacOS%20arm64&style=flat-square)](https://github.com/fizzed/nats-plus/actions/workflows/macos-arm64.yaml)
[![Windows x64](https://img.shields.io/github/actions/workflow/status/fizzed/nats-plus/windows-x64.yaml?branch=master&label=Windows%20x64&style=flat-square)](https://github.com/fizzed/nats-plus/actions/workflows/windows-x64.yaml)

The following platforms are tested using the [Fizzed, Inc.](http://fizzed.com) build system:

[![Linux arm64](https://img.shields.io/badge/Linux%20arm64-passing-green)](buildx-results.txt)
[![Linux MUSL x64](https://img.shields.io/badge/Linux%20MUSL%20x64-passing-green)](buildx-results.txt)
[![MacOS x64](https://img.shields.io/badge/MacOS%20x64-passing-green)](buildx-results.txt)
[![Windows arm64](https://img.shields.io/badge/Windows%20arm64-passing-green)](buildx-results.txt)
[![FreeBSD x64](https://img.shields.io/badge/FreeBSD%20x64-passing-green)](buildx-results.txt)

## Overview

Utilities and framework integrations for Java 11+ and NATS.io -- includes an integration of [NATS](https://nats.io/) with the [Ninja Framework](https://github.com/ninjaframework/ninja).

## Utilities

```xml
<dependency>
    <groupId>com.fizzed</groupId>
    <artifactId>nats-core</artifactId>
    <version>VERSION-HERE</version>
</dependency>
```

Browse the utilities in https://github.com/fizzed/nats-plus/tree/master/nats-core/src/main/java/com/fizzed/nats/core

## Ninja Framework

Ninja Framework module for NATS. Will help provide a connection, etc.

Add the nats-ninja-module dependency to your Maven pom.xml

```xml
<dependency>
    <groupId>com.fizzed</groupId>
    <artifactId>nats-ninja-module</artifactId>
    <version>VERSION-HERE</version>
</dependency>
```

In your `conf/Module.java` file:

```java
package conf;

import com.fizzed.nats.ninja.NinjaNatsModule;
import com.google.inject.AbstractModule;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        install(new NinjaNatsModule());
    }

}
```

In your `conf/application.conf` file:

```conf
#
# nats
#
nats.url = nats://localhost:14222
nats.username = root
nats.password = test
nats.connection_name = nats-demo
```

## Testing

Testing this library with other nats.java versions:

    mvn -Dnats.java.version=2.19.1 test
    mvn -Dnats.java.version=2.20.5-SNAPSHOT test

## License

Copyright (C) 2025 Fizzed, Inc.

This work is licensed under the Apache License, Version 2.0. See LICENSE for details.
