package com.wds.grow.study.hadoop.mr;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * mr测试用例
 * Created by wds on 2018/5/1.
 **/
public class MrTest {

    private Configuration conf;

    @Before
    public void init(){
        conf = new Configuration();
        conf.set("fs.default.name","hdfs://localhost:9000");
        conf.set("mapred.job.tracker","http://127.0.0.1:54311");
    }

    /**
     * 单词统计
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    @Test
    public void wordCount() throws IOException, ClassNotFoundException, InterruptedException {
        Job job = Job.getInstance(conf);
        job.setJarByClass(MrTest.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setMapperClass(WordTokenizerMapper.class);
        job.setReducerClass(IntSumReducer.class);
        job.setCombinerClass(IntSumReducer.class);

        // wordSample.txt文件内容见testData/wordSample.txt
        FileInputFormat.setInputPaths(job, new Path("/mr/wordSample.txt"));
        FileOutputFormat.setOutputPath(job, new Path("/mr/wordCountResult.txt"));
        job.waitForCompletion(true);
    }
}
