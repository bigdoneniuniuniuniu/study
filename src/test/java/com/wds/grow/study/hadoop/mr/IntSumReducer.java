package com.wds.grow.study.hadoop.mr;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * 统计reducer
 * Created by wds on 2018/5/1.
 **/
public class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

    @Override
    protected void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
        int total = 0;
        for (IntWritable value : values){
            total += value.get();
        }
        context.write(key, new IntWritable(total));
    }
}
