@echo off
set "ROOT=%~dp0..\"
CALL %ROOT%\bin\dispatcher.cmd download --url https://www.7-zip.org/a/7zr.exe
CALL %ROOT%\bin\dispatcher.cmd download --url https://download.java.net/java/GA/jdk17/0d483333a00540d886896bac774ff48b/35/GPL/openjdk-17_windows-x64_bin.zip && a\7zr.exe X java\GA\jdk17\0d483333a00540d886896bac774ff48b\35\GPL\openjdk-17_windows-x64_bin.zip
setx JAVA_HOME "%ROOT%\jdk-17"
setx PATH %PATH%;"%ROOT%\jdk-17\bin"

echo OpenJDK17 has installed successfully.