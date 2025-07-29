# TOMCAT 启动数据源自加密实施文档
 
## 1、先决条件

    1、JDK版本大于等于1.8
    2、TOMCAT大版本大于等于8（因为使用的是org.apache.tomcat.dbcp.dbcp2.BasicDataSourceFactory类，如果要低版本修改对应的源码即可）
    3、不适用于在server.xml中配置的数据源（官方也不推荐）

## 2、实施步骤

### 2.1 将代码打成两个jar包，并移入$TOMCAT_HOME/lib/下

> $TOMCAT_HOME=tomcat9版本安装目录

> tomcat-encryption.jar、tomcat-decryption.jar

> 将tomcat-encryption.jar、tomcat-decryption.jar、dom4j-2.0.2.jar放入$TOMCAT_HOME/lib/中
```
[root@localhost ~]#git clone https://github.com/lie-thorn/tomcat_datasource_auto_encryption.git
[root@localhost ~]#unzip tomcat_datasource_auto_encryption-master
[root@localhost ~]#cd tomcat_datasource_auto_encryption-master
[root@localhost tomcat_datasource_auto_encryption-master]#javac -cp dom4j-2.0.2.jar encryption/*.java -d .
[root@localhost tomcat_datasource_auto_encryption-master]#jar -cvfm tomcat-encryption.jar MANIFEST.MF com/tomcat/datasource/encryption/
[root@localhost tomcat_datasource_auto_encryption-master]#javac -cp $TOMCAT_HOME/lib/tomcat-dbcp.jar:tomcat-encryption.jar" decryption/*.java -d .
[root@localhost tomcat_datasource_auto_encryption-master]#jar -cvf tomcat-decryption.jar com/tomcat/datasource/decryption/
[root@localhost tomcat_datasource_auto_encryption-master]#cp *.jar $TOMCAT_HOME/lib/
```

### 2.2、修改原context.xml

自动加密由于涉及到对原配置文件的重写，所以需要将数据源配置从原配置文件中分离，保证重写后的配置文件不会影响到本身的其他配置。

> 将原context.xml


> 拆分为2个文件context.xml & datasource_test.xml 下划线后可自定义名称，推荐使用jndi name

```
[root@localhost tomcat_datasource_auto_encryption-master]#vim $TOMCAT_HOME/conf/context.xml
[root@localhost tomcat_datasource_auto_encryption-master]#vim $TOMCAT_HOME/conf/datasource_test.xml
```

#### context.xml ：
>添加了

```
<!DOCTYPE Context [<!ENTITY datasource_test SYSTEM "datasource_test.xml">]>
......

&datasource_test;

```

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Context [<!ENTITY datasource_test SYSTEM "datasource_test.xml">]>

<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!-- The contents of this file will be loaded for each web application -->
<Context>
&datasource_test;

    <!-- Default set of monitored resources. If one of these changes, the    -->
    <!-- web application will be reloaded.                                   -->
    <WatchedResource>WEB-INF/web.xml</WatchedResource>
    <WatchedResource>WEB-INF/tomcat-web.xml</WatchedResource>
    <WatchedResource>${catalina.base}/conf/web.xml</WatchedResource>

    <!-- Uncomment this to disable session persistence across Tomcat restarts -->
    <!--
    <Manager pathname="" />
    -->
</Context>
```

#### datasource_test.xml:
```
<?xml version="1.0" encoding="UTF-8"?>

<Resource name="jdbc/TestDB" auth="Container" type="javax.sql.DataSource" factory="com.tomcat.datasource.decryption.EncryptedTomcatJdbcDataSourceFactory" maxTotal="100" maxIdle="30" maxWaitMillis="10000" username="root" password="1qaz2wsx" driverClassName="com.mysql.jdbc.Driver" url="jdbc:mysql://192.168.1.1:1234/test"/>
```
<i>其中:</i>

"datasource_test" 和 "datasource_test.xml" 分别代表了原xml中引用的别名以及在原xml的同级目录中引用的外部xml文件名



### 2.3 对数据源文件进行加密
>也可以自行编写启动脚本，使启动tomcat时自动引用加密
```
[root@localhost tomcat_datasource_auto_encryption-master]#java -jar $TOMCAT_HOME/lib/tomcat-encryption.jar $TOMCAT_HOME/conf/datasource_test.xml
[root@localhost tomcat_datasource_auto_encryption-master]#cat $TOMCAT_HOME/conf/datasource_test.xml
```



## 3、添加多个数据源
> 修改context.xml

context.xml ：

<?xml version="1.0" encoding="UTF-8"?>

添加（保持<!ENTITY datasource_test SYSTEM "datasource_test.xml">格式，有几个加几个）
```
<!DOCTYPE Context [<!ENTITY datasource_test SYSTEM "datasource_test.xml"> <!ENTITY datasource_test01 SYSTEM "datasource_test01.xml">]>
......

&datasource_test;
&datasource_test01;（有几个加几个，名称对应）
</Context>
```
