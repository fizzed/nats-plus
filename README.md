Nats Plus by Fizzed
===================

Testing this library with other nats.java versions:

    mvn -Dnats.java.version=2.19.1 test
    mvn -Dnats.java.version=2.20.5-SNAPSHOT test












RabbitMQ Plus by Fizzed
============================================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.fizzed/rabbitmq-plus/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.fizzed/rabbitmq-plus)

[Fizzed, Inc.](http://fizzed.com) (Follow on Twitter: [@fizzed_inc](http://twitter.com/fizzed_inc))

## Overview

Utilities and framework integrations for Java and RabbitMQ. Includes an integration
of [RabbitMQ](https://www.rabbitmq.com/) with the [Ninja Framework](https://github.com/ninjaframework/ninja).

## Connection Pool and Other Helpers

### Setup

```xml
<dependency>
    <groupId>com.fizzed</groupId>
    <artifactId>rabbitmq-util</artifactId>
    <version>0.0.5</version>
</dependency>
```

Browse the utilities in https://github.com/fizzed/rabbitmq-plus/tree/master/rabbitmq-util/src/main/java/com/fizzed/rabbitmq/util


## Ninja Framework

Ninja Framework module for RabbitMQ. Will help provide connectivity to RabbitMQ,
a connection pool, and session providers.

### Setup

Add the rabbitmq-ninja-module dependency to your Maven pom.xml

```xml
<dependency>
    <groupId>com.fizzed</groupId>
    <artifactId>rabbitmq-ninja-module</artifactId>
    <version>0.0.5</version>
</dependency>
```

In your `conf/Module.java` file:

```java
package conf;

import com.fizzed.rabbitmq.ninja.NinjaRabbitModule;
import com.google.inject.AbstractModule;

public class Module extends AbstractModule {

    @Override
    protected void configure() {
        install(new NinjaRabbitModule());
    }

}
```

In your `conf/application.conf` file:

```java
#
# rabbitmq
#
rabbitmq.verify_queues = test.request
rabbitmq.url = amqp://localhost:5672
rabbitmq.user = root
rabbitmq.password = test
rabbitmq.pool.min_idle = 0
#rabbitmq.pool.max_idle = 0
rabbitmq.pool.evictable_idle_time_millis = 10000
```

## License

Copyright (C) 2020 Fizzed, Inc.

This work is licensed under the Apache License, Version 2.0. See LICENSE for details.
