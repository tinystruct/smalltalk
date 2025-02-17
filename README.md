
smalltalk
==
smalltalk is a tinystruct-based project that provides instant messaging functionality, It allows users to send text and share images, documents, and other content. 
Also, It allows you to interact with ChatGPT which is a language model developed by OpenAI through a command-line interface (CLI) or a web interface.

[![Star History Chart](https://api.star-history.com/svg?repos=tinystruct/smalltalk&type=Date)](https://star-history.com/#tinystruct/smalltalk&Date)

Installation
---
1. Download the project from GitHub by clicking the "Clone or download" button, then selecting "Download ZIP".
2. Extract the downloaded ZIP file to your local machine.
3. If you used to use git, then you should execute the following command to instead of above steps:
```bash
git clone https://github.com/tinystruct/smalltalk.git 
```  
4. You will need to follow this [tutorial](https://openjdk.org/install/) to install the Java Development Kit (JDK 11+) on your computer first. If you choose to download and install it manually, please check it in this [OpenJDK Archive](https://jdk.java.net/archive/). And Java development environment such as Eclipse or IntelliJ IDEA is just better to have, not required.

If your current envirionment is using JDK 8, you can execute the below command to upgrade it quickly.
```
bin/openjdk-upgrade
```
5. Import the extracted / cloned project into your Java development environment.
6. Go to `src/main/resources/application.properties` file and update the `openai.api_key` with your own key or set the environment variable `OPENAI_API_KEY` with your own key.
7. Here is the last step for installation:
```tcsh
./mvnw compile
```

Usage
---
You can run smalltalk in different ways:

CLI mode
1. Open a terminal and navigate to the project's root directory.
2. To execute it in CLI mode, run the following command:
```tcsh
bin/dispatcher --version
```
To see the available commands, run the following command:
```tcsh
bin/dispatcher --help
```
To interact with ChatGPT, use the chat command, for example:
```tcsh
bin/dispatcher chat
```
![CLI](https://github.com/tinystruct/smalltalk/assets/3631818/b49bab05-0135-4383-b252-0ca9c011f6e8)

Web mode

1. Run the project in a servlet container or in a HTTP server:
2. To run it in a servlet container, you need to compile the project first:

then you can run it on tomcat server by running the following command:

```tcsh
sudo bin/dispatcher start --import org.tinystruct.system.TomcatServer --server-port 777
```
or run it on netty http server by running the following command:

```tcsh
sudo bin/dispatcher start --import org.tinystruct.system.NettyHttpServer --server-port 777
```
3. To run it in a Docker container, you can use the command below:

```tcsh
docker run -d -p 777:777 -e "OPENAI_API_KEY=[YOUR-OPENAI-API-KEY]" -e "STABILITY_API_KEY=[YOUR-STABILITY-API-KEY]" m0ver/smalltalk
```
4. Access the application by navigating to http://localhost:777/?q=talk in your web browser
5. If you want to talk with ChatGPT, please type @ChatGPT in your topic of the conversation when you set up the topic.

![Web](https://github.com/tinystruct/smalltalk/assets/3631818/32e50145-a5be-41d6-9cea-5b25e76e9f1b)

<img src="https://github.com/user-attachments/assets/32721443-b680-481b-b5ed-ae3c7e4c6908" width=500 />

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

