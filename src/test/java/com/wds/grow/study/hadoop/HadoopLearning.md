
# Apache Hadoop Learning
[TOC]

## HDFS
### 简介
```
HDFS称为分布式文件系统（Hadoop Distributed Filesystem），有时也简称为DFS。
我们可以用以下几个key描述HDFS：
	- 超大文件
		GB、TB甚至PB级别的数据。
	- 流式数据访问
		数据集通常由数据源生成或从数据源复制而来的，然后在长时间在此数据集上进行各种数据分析。因此“一次写入，多次读取”是最高效的访问模式。
	- 要求低时间延迟数据访问的应用，不适合在HDFS上运行
	- 大量的小文件
	  namenode将文件系统的元数据存储在内存中，因此文件系统所能存储的文件总数受限于namenode的内存容量。
	- 文件写入只支持单个写入者，写操作总是以“只添加”方式在文件末尾写入数据
```

### 概念
1. 数据块
	```
		数据块也称为存储块，是主存储器与输入、输出设备之间进行传输的数据单位，是磁盘进行数据读/写的最小单位。
		HDFS也有块的概念，默认为128MB。HDFS上的文件也被划分为块大小的多个分块（chunk），作为独立的存储单元。抽象出这样的块会带来很多好处：一个文件的大小可以大雨网络中任意一个磁盘的容量，文件的所有块可以利用集群上的任意一个磁盘进行存储;块适用于数据备份，从而提高数据的容错能力，提高可用性。
	```
	
2. namenode 和 datenode
	```
		namenode(管理节点)：管理文件系统的命名空间，维护文件系统树及整棵树内所有的文件和目录，这些信息以命名空间镜像文件和编辑日志文件形式保存在磁盘。此外，还记录着每个文件中各个块所在的数据节点信息。
		datenode（工作节点）：根据需要存储并检索数据块（受客户端/namenode的调度），并定期向namenode汇报所存储的块的列表。
	```

3. 交互流程示意图
	* 客户端读取HDFS文件流程
		![客户端读取HDFS流程](https://raw.githubusercontent.com/wudongsen/study/master/src/test/docImages/客户端读取HDFS流程.png)
		- 步骤1:调用FileSystem的open()打开希望读取的文件。
		- 步骤2:DistributedFileSystem通过rpc调用namenode，确定文件起始块的位置。对于每个块，namenode返回存有该块副本的datanode的地址。此外，datanode根据它们与客户端的距离来排序。然后返回FSDataInputStream给客户端。
		- 步骤3:FSDataInputStream调用read()。
		- 步骤4:连接距离最近的文件中的第一个块所在的datanode，通过对数据流反复调用read()，将数据传输到客户端。
		- 步骤5:达到块的末端时，DFSInputStream关闭与该datanode的连接，然后寻找下一个块的最佳datanode。
		- 步骤6:客户端完成读取，close()。
		
	* 客户端写入HDFS流程图
		![客户端写入HDFS](https://raw.githubusercontent.com/wudongsen/study/master/src/test/docImages/客户端将数据写入HDFS.png)
		- 步骤1:DistributedFileSystem调用create()
		- 步骤2:DistributedFileSystem发起rpc调用，在确保该文件夹不存在且客户端有新建该文件夹的权限的一系列校验后，namenode会为创建新文件记录一天记录，并返回FSDataPutputStream对象。
		- 步骤3:客户端调用write()。
		- 步骤4：DFSOutputStream将它分成一个个数据包，写入数据队列。DataStreamer挑选出适合存储数据副本的一组datanode。这一组datanode构成一个管线。假设副本数为3，DataStreamer将数据包流式依次从第一个datanode传输到第三个datanode。
		- 步骤5:DFSOutputStream维护着确认队列，等收到管道中所有datanode的确认信息后，数据包才会从确认队列中删除。
		- 步骤6:完成数据写入后，close()。
		- 步骤7:告知namenode文件写入完成。
	
### 搭建
系统和软件 | 版本号 | 数量
----------- | ------- | ---
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

### shell命令学习
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


## 参考文献
> [1]Tom White.Hadoop权威指南[M].北京:清华大学出版社,第四版.









