
# Apache Hadoop Learning
[TOC]

## 搭建
系统和软件 | 版本号 | 数量
----------- | --- ---- | ---
MacBook Pro | 10.13.3 mac OS | 1台
hadoop | 2.8.3 | 
jdk | 1.8.0_162

1. 用户/目录/事先约定
	* 已安装、配置好jdk
	* 用户使用hadoopuser
	* 下载文件包放置于/Users/hadoopuser/Downloads
	* hadoop_home的目录为/Users/hadoopuser/www
2. 下载hadoop,解压到约定目录
	 * brew install wget
	 * su hadoopuser
	 * cd /Users/hadoopuser/Downloads
	 * wget http://mirrors.shu.edu.cn/apache/hadoop/common/hadoop-2.8.3/hadoop-2.8.3.tar.gz
	 * tar zxvf hadoop-2.8.3.tar.gz -C /Users/hadoopuser/www/
3. 修改配置文件
	* mkdir /Users/hadoopuser/www/hadoop-2.8.3/tmp
	* cd /Users/hadoopuser/www/hadoop-2.8.3/etc/hadoop
	* 修改hadoop-env.sh

		```
		export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_162.jdk/Contents/Home
		export HADOOP_CONF_DIR=/Users/hadoopuser/www/hadoop-2.8.3/etc/hadoop
		```
	* 修改core-site.xml

		```
		<property>
      	  <name>fs.defaultFS</name>
          <value>hdfs://localhost:9000</value>
        </property>

  	    <!--用来指定hadoop运行时产生文件的存放目录-->
  	    <property>
           <name>hadoop.tmp.dir</name>
           <value>/Users/hadoopuser/www/hadoop-2.8.3/tmp</value>
  		</property>	
		```
	* 修改hdfs-site.xml

		```
		<property>
     		<!--伪分布式模式-->
     		<name>dfs.replication</name>
     		<value>1</value>
        </property>

 		<!--非root用户也可以写文件到hdfs-->
        <property>
        	<name>dfs.permissions</name>
     		<value>false</value>    
        </property>
		```
	* 修改mapred-site.xml

		```
		<property>
    		<!--指定mapreduce运行在yarn上-->
    		<name>mapreduce.framework.name</name>
    		<value>yarn</value>
        </property>
		```
	* 修改yarn-site.xml

		```
		<!-- Site specific YARN configuration properties -->
        <property>
    		<name>yarn.resourcemanager.hostname</name>
    		<value>localhost</value>
        </property>
        <property>
    		<name>yarn.nodemanager.aux-services</name>
    		<value>mapreduce_shuffle</value>
        </property>
		```
4. 配置hadoop全局环境变量
	* vim /etc/prifile
	* 修改profile

		```
		export HADOOP_HOME=/Users/hadoopuser/www/hadoop-2.8.3
		export PATH=$PATH:$HADOOP_HOME/bin
		```
	* source /etc/profile
5. 配置免密匙ssh localhost
	* ssh-keygen -t rsa
	* cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
	* cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
	* 用ssh localhost验证下
6. 格式化nameNode
	* hadoop namenode -format
6. 启动
	* cd /Users/hadoopuser/www/hadoop-2.8.3/sbin
	* ./start-all.sh
7. 验证 
	NameNode web管理端口：http://localhost:50070/

## shell命令学习
基本命令格式：hadoop fs -cmd < args >

命令行 | 作用
------------------ | ----------------
hadoop fs -ls #path | 列出路径指定的目录中的内容
hadoop fs -lsr #path | 递归显示路径的所有子目录项
hadoop fs -mkdir #path | hdfs中创建一个目录
hadoop fs -mv #src #dest | 把文件/目录从源移动到目的
hadoop fs -cp #src #test | 把文件/目录从源复制到目的
hadoop fs -rm #path | 删除文件/目录
hadoop fs -rmr #path | 递归删除文件/目录
hadoop fs -put #localSrc #dest | 本地文件复制到hdfs的目标文件
hadoop fs -get #src #localDest | 拷贝hdfs里的文件到本地
hadoop fs -cat #fileName | 显示文件内容




