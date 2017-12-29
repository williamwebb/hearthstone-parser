hearthstone-parser | [![Release](https://jitpack.io/v/io.williamwebb/hearthstone-parser.svg)](https://jitpack.io/#io.williamwebb/hearthstone-parser) [![CircleCI](https://circleci.com/gh/williamwebb/hearthstone-parser.svg?style=svg)](https://circleci.com/gh/williamwebb/hearthstone-parser)
============

hearthstone log parser

Usage
=====

```
val parser = HearthstoneParser(logReader, pathToLogs, cardDb)

parser.power.subscribe {
    // power events
}

parser.arena.subscribe {
    // arena events
}

parser.loadingscreen.subscribe {
    // loadingscreen events
}

parser.start() // start parsing
parser.stop() // stop parsing
```

Installation
=====
```
// Add the JitPack repo
repositories {
    maven { url 'https://jitpack.io' }
}

// Add dependency
dependencies {
    compile 'io.williamwebb:hearthstone-parser:<version>'
}
```
License
-------

    Copyright 2017 William Webb

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
