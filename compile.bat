::Point javac to your ImageJ copy here with CLASSPATH
if "%JAVA_HOME%" == "" GOTO NOPATH
if "%ANT_HOME%" == "" GOTO NOPATH
@echo paths already set
GOTO YESPATH
:NOPATH
set JAVA_HOME=C:\Program Files\Java\jdk1.6.0_25
set ANT_HOME=D:\UserData\ratimo\Oma\ohjelmat\apache-ant-1.8.3
set PATH=.;%ANT_HOME%\bin;%PATH%
@echo Added path
:YESPATH
D:\UserData\ratimo\Oma\ohjelmat\apache-ant-1.8.3\bin\ant -buildfile buildFrameByFrame.xml
