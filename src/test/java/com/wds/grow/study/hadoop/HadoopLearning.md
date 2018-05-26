# Apache Hadoop Learning
[TOC]

## HDFS
### 简介
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

### 概念
1. 数据块
	
	数据块也称为存储块，是主存储器与输入、输出设备之间进行传输的数据单位，是磁盘进行数据读/写的最小单位。
	HDFS也有块的概念，默认为128MB。HDFS上的文件也被划分为块大小的多个分块（chunk），作为独立的存储单元。抽象出这样的块会带来很多好处：一个文件的大小可以大雨网络中任意一个磁盘的容量，文件的所有块可以利用集群上的任意一个磁盘进行存储;块适用于数据备份，从而提高数据的容错能力，提高可用性。
	
	
2. namenode 和 datenode
	
	namenode(管理节点)：管理文件系统的命名空间，维护文件系统树及整棵树内所有的文件和目录，这些信息以命名空间镜像文件和编辑日志文件形式保存在磁盘。此外，还记录着每个文件中各个块所在的数据节点信息。
	datenode（工作节点）：根据需要存储并检索数据块（受客户端/namenode的调度），并定期向namenode汇报所存储的块的列表。
	

### 交互流程示意图
1. 客户端读取HDFS文件流程
		![客户端读取HDFS流程](https://raw.githubusercontent.com/wudongsen/study/master/src/test/docImages/客户端读取HDFS流程.png)
	- 步骤1:调用FileSystem的open()打开希望读取的文件。
	- 步骤2:DistributedFileSystem通过rpc调用namenode，确定文件起始块的位置。对于每个块，namenode返回存有该块副本的datanode的地址。此外，datanode根据它们与客户端的距离来排序。然后返回FSDataInputStream给客户端。
	- 步骤3:FSDataInputStream调用read()。
	- 步骤4:连接距离最近的文件中的第一个块所在的datanode，通过对数据流反复调用read()，将数据传输到客户端。
	- 步骤5:达到块的末端时，DFSInputStream关闭与该datanode的连接，然后寻找下一个块的最佳datanode。
	- 步骤6:客户端完成读取，close()。
		
2. 客户端写入HDFS流程图
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
	* hadoop_home的目录为/Users/hadoopuser/www/hadoop-2.8.3
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
  		
  		<property>
            <name>hadoop.proxyuser.hadoopuser.hosts</name>
            <value>*</value>
        </property>
        <property>
            <name>hadoop.proxyuser.hadoopuser.groups</name>
            <value>*</value>
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
    			<name>yarn.nodemanager.hostname</name>
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

---

## MapReduce
### 简介
### 设计

### 举个栗子🌰
1. example  
  给定一个名称为data.txt的文档，想要统计出这份文档的每个单词的数量。文档数据内容为：  
    tom animal
    wds man
    peiqi animal
  那么运用MapReduce对其进行实现的过程为：
    1. input:输入该文档
        tom animal
        wds man
        peiqi animal
    2. split
        split-0:tom animal
        split-0:wds man
        split-1:peiqi animal
    3. map:将内容转换为所需要的key value
        split-0:
            tom 1
            animali 1
            wds 1
            man 1
        split-1:
            peiqi 1
            animal 1
    4. shuffle: 将key相同的派发到一起
        tom:1
        animali:1,1
        wds:1
        man:1
        peiqi:1
    5.reduce:将相同key结果进行统计
        tom:1
        animali:2
        wds:1
        man:1
        peiqi:1
    6.output
2. 配图  
    ![MapReduce-wordCount示意图.png](https://raw.githubusercontent.com/wudongsen/study/master/src/test/docImages/MapReduce-wordCount示意图.png)
3. 代码  
    [MapReduce-wordCount代码demo](https://github.com/wudongsen/study/blob/master/src/test/java/com/wds/grow/study/hadoop/mr/MrTest.java)


---

## yarn

## hive
### 简介  
hive是基于hadoop之上的数据仓库。主要提供了以下功能：  
  1. 用来对数据进行提取/转化/加载（ETL）
  2. 定义简单语言hql，可以在hdfs上进行类sql的curd等操作。屏蔽了编写mr的复杂过程，使得懂sql语言的人就能上手。
  3. hive的hql可以通过用户自定义函数（UDF）、自定义聚合（UDAFs）、自定义表功能（UDTFs）进行拓展。
  4. hive适用于传统的数据仓库任务，不是为联机事物处理（OLTP[^OLTP]）而设计的。
  
### 概念  
1. 托管表、外部表  
    hive创建的表称为托管表，hive可以控制其数据的生命周期，对数据拥有所有权。  
    外部表不需要将数据复制到hive中，一旦关联上数据格式和数据位置，hive就能访问外部数据。  
    
    &nbsp; | 托管表 | 外部表
    ---|---|---
    创建 | 将数据移到数据仓库指向的路径 | 记录数据所在位置、数据格式
    删除 | 表的元数据和数据会被一起删除 | 只删除元数据

2. 分区(partition)、分桶(bucket)  
    hive可以把表组织成分区，这是根据partition column的值对表进行粗略划分的一种机制。这样做的目的是：避免全表扫描，加快数据分片(slice)的查询速度。  
    一个表可以以多个维度来进行分区。例如：可以对日志表进行日期维度的划分，还能进一步在日期维度的基础上进行日志级别维度的划分。这样的划分并不会影响跨多个分区的范围查询。  

    桶是通过对指定列进行哈希计算来实现的，通过哈希值将一个列名下的数据切分为一组桶。  
    分桶就是对列值进行hash，并用hash值%桶的个数的一个过程，哈希函数的选择依赖于桶操作所针对的列的数据类型。这其实是对数据进行采样的一个过程，并不能保证数据均匀地分布在每个桶中。除了数据采样，桶操作也可以用来实现高效的Map端连接操作（map-side-join）。   
    但当某个分区中的文件数量过于庞大甚至可能导致文件系统崩溃时，我们就可以进一步把分区中的数据进一步拆分成桶。  
    ps：建桶之前，需要设置hive.enforce.bucketing属性为true。
    
3. 存储格式  
    * 行格式 
        行格式指定的是一行中的字段应该如何存储。  
        默认分隔符：  
        
        值 | 作用
        ---|---
        \n | 行分隔符
        ^A(Ctrl+A) 八进制编码表示为\001 | 列分隔符
        ^B(Ctrl+B) 八进制编码表示为\002 | 用于分隔ARRAY或者STRUCT中的元素，或用于MAP中键-值对之间的分隔。
        ^C(Ctrl+C) 八进制编码表示为\003 | 用于MAP中键和值之间的分隔。
    * 文件格式  
    
        存储格式 | 存储方式 | 优点 | 缺点
        ---|---|---|---
        TEXTFILE | 行式存储 | 可读性好，易查看 | 数据不做压缩，磁盘开销、数据解析开销大；可结合Gzip、Bzip2压缩，但这种方式不会对数据进行切分（split）操作，无法并行执行。
        SEQUENCEFILE | 二进制文件，数据以<key,value>的形式序列化到文件中 | 使用方便、可分割、可压缩。 | 
        RCFILE | 列式存储；数据按照行分块，每个块中按照列存储 | 多种压缩方式；有很高的压缩比；文件可切分；提供多种索引；支持复杂的数据结构；适合统计查询 | 不适合增、删、改等事务操作 
        
        存储方式 | 优点 | 缺点
        ---|---|---
        行式存储 | 数据存在一起；update、insert容易 | 只想查某几列，该行所有数据也会被读取
        列式存储 | query时只有涉及到的列会被读取；任何列都可作为索引；适合聚集运算；有利于高度压缩 | insert、update麻烦

        ![列式存储-行式存储.png](https://raw.githubusercontent.com/wudongsen/study/master/src/test/docImages/列式存储-行式存储示意图.png)  
        
4. UDF
    

### 架构
### 执行流程
### 远程模式搭建
系统和软件 | 版本号 | 数量
----------- | ------- | ---
MacBook Pro | 10.13.3 mac OS | 1台
hadoop | 2.8.3 | 
jdk | 1.8.0_162 | 
mysql | 5.6.39 | 
hive | 2.3.3 | 

1. 用户/目录/事先约定
	* 已安装好jdk、mysql、hadoop环境
	* 用户使用hadoopuser
	* 下载文件包放置于/Users/hadoopuser/Downloads
	* hive_home的目录为/Users/hadoopuser/www/hive-2.3.3
2. 下载hive、mysql驱动包,解压到约定目录,并把mysql驱动包添加到hive的lib目录下
	* su hadoopuser
	* cd /Users/hadoopuser/Downloads
	* wget http://mirrors.hust.edu.cn/apache/hive/hive-2.3.3/apache-hive-2.3.3-bin.tar.gz
	* wget https://cdn.mysql.com//Downloads/Connector-J/mysql-connector-java-5.1.46.tar.gz
	* tar zxvf apache-hive-2.3.3-bin.tar.gz -C /Users/hadoopuser/www/
	* tar zxvf mysql-connector-java-5.1.46.tar
	* cd mysql-connector-java-5.1.46
	* cp mysql-connector-java-5.1.46-bin.jar /Users/hadoopuser/www/hive-2.3.3/lib/
3. 更改hive-site.xml配置文件
	* mkdir mkdir /Users/hadoopuser/www/hive-2.3.3/temp（创建临时文件存放路径）
	* cd /Users/hadoopuser/www/hive-2.3.3/conf
	* cp hive-default.xml.template hive-site.xml
	* vim hive-site.xml
		* 把${system:java.io.tmpdir}全部替换成/Users/hadoopuser/www/hive-2.3.3/temp
		* 把{system:user.name}全部换成{user.name}
	* 配置数据库

		```
			<name>javax.jdo.option.ConnectionURL</name>
    		<value>jdbc:mysql://127.0.0.1:3306/hive?createDatabaseIfNotExist=true</value>
    		
    		<name>javax.jdo.option.ConnectionDriverName</name>
			<value>com.mysql.jdbc.Driver</value>
			
			<name>javax.jdo.option.ConnectionUserName</name>
 			<value>hive</value>
 			
			<name>javax.jdo.option.ConnectionPassword</name>
			<value>hive</value>
		```			
4. 创建数据库用户、数据库
	* /usr/local/mysql/bin
	* ./mysql -h127.0.0.1 -P3306 -uroot -p
	* create database hive;
	* grant all on hive.* to hive@'%'  identified by 'hive';
	* grant all on hive.* to hive@'localhost'  identified by 'hive';
	* flush privileges;
5. 初始化metadata
	* ./schematool -initSchema -dbType mysql --verbose
	* 在mysql上查看是否成功
6. 启动hive
	* ./hive --service hiveserver2 &

### hive命令
1. DDL(data definition language)操作
	* 建表  
  CREATE [EXTERNAL] TABLE [IF NOT EXISTS] table_name  
  [(col_name data_type [COMMENT col_comment], ...)]  
  [COMMENT table_comment]  
  [PARTITIONED BY (col_name data_type [COMMENT col_comment], ...)]   
  [CLUSTERED BY (col_name, col_name, ...)   
  [SORTED BY (col_name [ASC|DESC], ...)] INTO num_buckets BUCKETS]  
  [ROW FORMAT row_format]  
  [STORED AS file_format]  
  [LOCATION hdfs_path]
  		- EXTERNAL：可以让用户创建一个外部表，在建表的同时指定一个指向实际数据的路径（LOCATION）
  		- ROW FORMAT：
  			
  			DELIMITED [FIELDS TERMINATED BY char] [COLLECTION ITEMS TERMINATED BY char][MAP KEYS TERMINATED BY char] [LINES TERMINATED BY char]| SERDE serde_name [WITH SERDEPROPERTIES (property_name=property_value, property_name=property_value, ...)]
  		- PARTITIONED BY：指定分区信息
  		- CLUSTERED BY：针对某一列分桶，采用对列值进行哈希，然后除以桶的个数求余的方式决定该条记录存放在哪个桶当中。更细粒度地划分数据。
  		- STORED AS SEQUENCEFILE | TEXTFILE| RCFILE。如果文件数据是纯文本，可以使用TEXTFILE。如果数据需要压缩，使用SEQUENCEFILE。
  	case：
  	```
  		CREATE TABLE par(userid BIGINT, time INT, page_url STRING,referrer_url STRING, ip STRING COMMENT 'IP Address of the User')  
  		COMMENT 'This is the page view table'  
  		PARTITIONED BY(dt STRING) CLUSTERED BY(userid)   
  		SORTED BY(time) INTO 32 BUCKETS   
  		ROW FORMAT DELIMITED FIELDS TERMINATED BY ' '  LINES TERMINATED BY '\n' STORED AS TEXTFILE;
  	```
	* 查看数据库
	    ```
	    show databases;
	    ```
	* 查看具体数据库
	    ```
		describe database default;
		```
	* 查看创建数据库sql语句
	    ```
		show create database default;
		```
	* 查看表详细信息
	    ```
		desc par;
        describe extended par;
 		describe formatted par;
 		```
	* 删除表
	    ```
		drop table par;
		```
	* 清空表数据
	    ```
		truncate table par;
		```
2. DML(data mainpulation language)操作
	* 加载本地文件
	    ```
		load data local inpath  
		'/Users/hadoopuser/www/hive-2.3.3/bin/1.txt' into table  
		default.par PARTITION (dt='2008-08-15');
		```
	* insert  
	    (1)单表：insert (overwrite|into) table `tb_name1` select * from `tb_name2` where_statement;  
        overwrite:重写;into:append。
	    ```
	    insert into test values(1,'22',3);
	    ```  
	    (2)多表:FROM `tb_name1` t INSERT OVERWRITE TABLE `tb_name2` PARTITION(dt='2008-06-08') SELECT *
3. DQL(data query language)操作  
    * SELECT [ALL | DISTINCT] 字段名, 字段名, …   
      FROM 表名  
      [WHERE 条件]  
      [GROUP BY 列名 [HAVING 条件]]  
      [ CLUSTER BY 列名 | [DISTRIBUTE BY 列名] [SORT BY | ORDER BY 列名]]  
      [LIMIT 数字]
## 参考文献
> [1]Tom White.Hadoop权威指南[M].北京:清华大学出版社,第四版.


[^OLTP]: OLTP(On-Line Transaction Processing)：联机处理事务过程，也称为实时系统，是传统的关系型数据库的主要应用，例如：银行存取一笔款。主要有以下特点：响应速度快；传输数据量不大，但并发高；具备事务的ACID特性。
