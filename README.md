<img src="https://avatars.githubusercontent.com/u/3723144?s=400&u=6c4b365e7feb0aab20383785d77ba27abf9f5bb8&v=4" title="tinystruct2.0" /> 

tinystruct
=========
[![Build Status](https://travis-ci.org/tinystruct/smalltalk.svg?branch=master)](https://travis-ci.org/m0ver/tinystruct2.0)

This is an example project based on tinystruct framework, it supports both C/S application and B/S web application development. 



To execute it in CLI mode
---
```tcsh
$ bin/dispatcher --version

  _/  '         _ _/  _     _ _/
  /  /  /) (/ _)  /  /  (/ (  /  0.8.8
           /
```

```tcsh
$ bin/dispatcher --help
Usage: bin/dispatcher COMMAND [OPTIONS]
Commands: 
        download        Download a resource from other servers
        exec            To execute native command(s)
        install         Install a package
        say             Output words
        set             Set system property
        sql-query       SQL query needs to be executed.
        update          Update for latest version

Options: 
        --help          Help command
        --import        Import application
        --logo          Print logo
        --settings      Print settings
        --version       Print version

Run 'bin/dispatcher COMMAND --help' for more information on a command.
	
$ bin/dispatcher say/"Praise to the Lord"
Praise to the Lord
$ bin/dispatcher say --words Hello --import tinystruct.examples.example
Hello
```
Configuration for ChatGPT
---
Add the below two lines to application.properties
```
chatGPT.api_endpoint=https://api.openai.com/v1/completions
chatGPT.api_key=sk-[secret key]
```
Run it in a servlet container
---
```tcsh
# ./mvnw compile
# bin/dispatcher start --import org.tinystruct.system.NettyHttpServer
```
Run it in docker container
---
```tcsh
# docker run -d -p 777:777 m0ver/smalltalk
```
Access
---
You can access the below URL after start the server:

* <a href="http://localhost:777/?q=talk">http://localhost:777/?q=talk </a><br />

If you want to talk with @ChatGPT, please type @ChatGPT in your topic of the conversation.

Demonstration
---
A demonstration for comet technology, without any websocket and support any web browser:
* <a href="https://tinystruct.herokuapp.com/?q=talk">https://tinystruct.herokuapp.com/?q=talk</a><br />

<img src="https://github.com/m0ver/tinystruct-examples/blob/master/example.png" title="smalltalk - tinystruct" height="300"/> <br />

Live Demo Site: 
* https://tinystruct.herokuapp.com/
* <a href="https://tinystruct.herokuapp.com/?q=say/Praise%20to%20the%20Lord!">https://tinystruct.herokuapp.com/?q=say/Praise%20to%20the%20Lord! </a><br />
* <a href="https://tinystruct.herokuapp.com/?q=praise">https://tinystruct.herokuapp.com/?q=praise</a><br />
* <a href="https://tinystruct.herokuapp.com/?q=youhappy">https://tinystruct.herokuapp.com/?q=youhappy</a><br />
* <a href="https://tinystruct.herokuapp.com/?q=say/%E4%BD%A0%E7%9F%A5%E9%81%93%E5%85%A8%E4%B8%96%E7%95%8C%E6%9C%80%E7%95%85%E9%94%80%E7%9A%84%E4%B9%A6%E6%98%AF%E5%93%AA%E4%B8%80%E6%9C%AC%E4%B9%A6%E5%90%97%EF%BC%9F">https://tinystruct.herokuapp.com/?q=say/%E4%BD%A0%E7%9F%A5%E9%81%93%E5%85%A8%E4%B8%96%E7%95%8C%E6%9C%80%E7%95%85%E9%94%80%E7%9A%84%E4%B9%A6%E6%98%AF%E5%93%AA%E4%B8%80%E6%9C%AC%E4%B9%A6%E5%90%97%EF%BC%9F</a>

Results in your browser should be:

<blockquote>
<h1>Praise to the Lord!</h1>
Praise to the Lord! 
<i>true</i>
<h1>你知道全世界最畅销的书是哪一本书吗？</h1>
</blockquote>

Explore it 
--
* Please read more example code in the project.
* Also please see this project: 
	https://github.com/m0ver/mobile1.0
	http://ingod.today


License
--

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


[![Bitdeli Badge](https://d2weczhvl823v0.cloudfront.net/m0ver/tinystruct2.0/trend.png)](https://bitdeli.com/free "Bitdeli Badge")
