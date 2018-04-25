package com.wds.grow.study.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.URI;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * hdfs的api测试用例
 * Created by wds on 2018/4/12.
 **/
public class HdfsApiTest {

    private FileSystem fileSystem;

    // nameNode节点路径
    private String nameNodeUri = "hdfs://localhost:9000";

    // 访问用户
    private String user = "hadoopuser";

    @Before
    public void before() throws Exception {
        fileSystem = FileSystem.get(new URI(nameNodeUri), new Configuration(true), user);
    }

    /**
     * 创建文件夹
     */
    @Test
    public void mkdirs() throws Exception {
        boolean isSuccess = false;
        Path path = new Path("/hadoopLearning/test");
        if(!fileSystem.exists(path)){
            isSuccess = fileSystem.mkdirs(path);
        }
        assertEquals(true, isSuccess);
    }

    /***
     * 是否存在
     */
    @Test
    public void isExists() throws IOException {
        Path path = new Path("/hadoopLearning/test");
        assertEquals(true, fileSystem.exists(path));
    }

    /**
     * 上传本地文件(写入数据)
     * ps: FSDataOutputStream 只支持追加到文件末尾
     */
    @Test
    public void uploadFile() throws IOException {
        String fileUri = "/var/hive/warehouse/puck.db/bus/tmp_query_result/workflowId=1/batchId=1/data.txt";
        String content = String.format("Hi, %s.So nice day.", "wds");

        Path path = new Path(fileUri);
        FSDataOutputStream outputStream = fileSystem.create(path, true);
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        IOUtils.copyBytes(inputStream, outputStream, 1024, true);

        assertEquals(true, fileSystem.exists(path));
    }

    /**
     * 写入数据，回调
     * 触发一次progress（）：每次将64KB数据包写入datanode管线
     */
    @Test
    public void uploadFileWithProgress() throws IOException {
        String fileUri = "/hadoopLearning/test/test1.txt";
        String content = String.format("Hello world.Yeah, %s.So nice to meet u.", "wds");
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(content.getBytes()));

        Path path = new Path(fileUri);
        OutputStream outputStream = fileSystem.create(path, () -> System.out.print("."));
        IOUtils.copyBytes(inputStream, outputStream, 1024, true);

        assertEquals(true, fileSystem.exists(path));
    }

    /**
     * 下载数据（读取数据）
     * ps： FSDataInputStream 支持随机访问
     */
    @Test
    public void downloadFile() throws Exception {
        String fileUri = "/hadoopLearning/test/test.txt";
        Path path = new Path(fileUri);
        FSDataInputStream inputStream = null;

        try {
            if(fileSystem.exists(path)){
                inputStream = fileSystem.open(path);
                IOUtils.copyBytes(inputStream, System.out, 1024, false);
            }
        }finally {
            IOUtils.closeStream(inputStream);
        }
    }

    /**
     * 随机下载（读取）数据
     * @throws Exception
     */
    @Test
    public void downloadFileBySeek() throws Exception {
        String fileUri = "/hadoopLearning/test/test.txt";
        Path path = new Path(fileUri);
        FSDataInputStream inputStream = null;

        try {
            if(fileSystem.exists(path)){
                inputStream = fileSystem.open(path);
                IOUtils.copyBytes(inputStream, System.out, 1024, false);
                inputStream.seek(2);
                IOUtils.copyBytes(inputStream, System.out, 1024, false);
            }
        }finally {
            IOUtils.closeStream(inputStream);
        }
    }

    /**
     * 列举文件
     */
    @Test
    public void listFiles() throws IOException {
        String rootPath = "/";
        Path path = new Path(rootPath);

        RemoteIterator<LocatedFileStatus> fileStatuses = fileSystem.listFiles(path, true);
        while (fileStatuses.hasNext()){
            LocatedFileStatus status = fileStatuses.next();
            System.out.println(status);
        }
    }

    /**
     * 列举该路径下的文件、文件夹
     */
    @Test
    public void listDirsAndFiles() throws IOException {
        String uri = "/";
        Path path = new Path(uri);

        FileStatus[] files = fileSystem.listStatus(path);
        Arrays.stream(files).forEach(file -> {
            System.out.println(String.format("%s %s %s %s %d", file.getPath().toString(), file.getGroup(),
                    file.getOwner(), file.getPermission(), file.getModificationTime()));
        });
    }

    /**
     * 删除
     * ps：不存在返回false
     */
    @Test
    public void delete() throws IOException {
        String uri = "/test/test.txt";
        Path path = new Path(uri);

        // true:递归删除
        boolean isSuccess = fileSystem.delete(path, true);
        assertEquals(true, isSuccess);
    }
}
