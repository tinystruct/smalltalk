
smalltalk
==
[![Build Status](https://travis-ci.org/tinystruct/smalltalk.svg?branch=master)](https://travis-ci.org/m0ver/tinystruct2.0)

smalltalk is an example project based on the tinystruct framework that supports both C/S application and B/S web application development. It allows you to interact with ChatGPT, a language model developed by OpenAI, through a command-line interface (CLI) or a web interface.

Installation
---
1. Download the project from GitHub by clicking the "Clone or download" button, then selecting "Download ZIP".
2. Extract the downloaded ZIP file to your local machine.
3. You will need a Java Development Kit (JDK) installed on your computer, as well as a Java development environment such as Eclipse or IntelliJ IDEA.
4. Import the extracted project into your Java development environment.
5. Go to src/main/resources/application.properties file and update the chatGPT.api_key with your own key.

Usage
---
You can run smalltalk in different ways:

CLI mode
1. Open a terminal and navigate to the project's root directory.
2. To execute it in CLI mode, run the following command:
```tcsh
$ bin/dispatcher --version
```
To see the available commands, run the following command:
```tcsh
$ bin/dispatcher --help
```
To interact with ChatGPT, use the say command, for example:
```tcsh
$ bin/dispatcher say/"What's your name?"
```
Web mode

1. Run the project in a servlet container or in a HTTP server:
2. To run it in a servlet container, you need to compile the project first:
```tcsh
# ./mvnw compile
```
then you can run it on tomcat server by running the following command:

```tcsh
# bin/dispatcher start --import org.tinystruct.system.TomcatServer --server-port 777
```
or run it on netty http server by running the following command:

```tcsh
# bin/dispatcher start --import org.tinystruct.system.NettyHttpServer --server-port 777
```
3. To run it in a Docker container, you can use the command below:

```tcsh
# docker run -d -p 777:777 -e "CHATGPT_API_KEY=[YOUR-CHATGPT-API-KEY]" m0ver/smalltalk
```
4. Access the application by navigating to http://localhost:777/?q=talk in your web browser
5. If you want to talk with ChatGPT, please type @ChatGPT in your topic of the conversation when you set up the topic.

Demonstration
---
A demonstration for the comet technology, without any websocket and support any web browser:

https://tinystruct.herokuapp.com/?q=talk

Troubleshooting
---
* If you encounter any problems during the installation or usage of the project, please check the project's documentation or build files for information about how to set up and run the project.
* If you still have problems, please open an issue on GitHub or contact the project maintainers for help.

Contribution
---
We welcome contributions to the smalltalk project. If you are interested in contributing, please read the CONTRIBUTING.md file for more information about the project's development process and coding standards.

Acknowledgements
---
smalltalk uses the OpenAI API to interact with the ChatGPT language model. We would like to thank OpenAI for providing this powerful tool to the community.

License
---

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
