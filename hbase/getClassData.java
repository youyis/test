package com.jd.hbase;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableReducer;

public class getClassData {
	public static class Map extends TableMapper<Text, Text> {		
		private String columnCluster = "";
		private String columnName = "";

		public void setup(Context context) throws IOException,
				InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			columnCluster = conf.get("column.cluster");
			columnName = conf.get("column.name");
		}		
		@Override
		public void map(ImmutableBytesWritable row, Result value, Context context)
				throws IOException, InterruptedException {
			String line = new String(value.getValue(columnCluster.getBytes(), columnName.getBytes()));
			context.write(new Text(row.get()), new Text(line));
		}
	}

	public static class Reduce extends
			TableReducer<Text, Text, ImmutableBytesWritable> {
		private String columnCluster = "";
		private String columnName = "";

		public void setup(Context context) throws IOException,
				InterruptedException {
			super.setup(context);
			Configuration conf = context.getConfiguration();
			columnCluster = conf.get("column.cluster");
			columnName = conf.get("column.name");
		}

		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			String k = key.toString();
			String value = values.iterator().next().toString();

			Put putRow = new Put(k.getBytes());
			putRow.add(columnCluster.getBytes(), columnName.getBytes(),
					value.getBytes());

			try {

				context.write(new ImmutableBytesWritable(key.getBytes()),
						putRow);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws Exception {

		Configuration conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.quorum", "BJYZ-Hbase-odpts-44147.jd.local");
		conf.set("hbase.client.retries.number", "1");
		conf.set("zookeeper.znode.parent", "/hbase");

		conf.set("column.cluster", args[1]);
		conf.set("column.name", args[2]);
		Job job = Job.getInstance(conf, "hbaseToHdfs");

		job.setJarByClass(getClassData.class);
	//	job.setMapperClass(Map.class);
	//	job.setReducerClass(Reduce.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		
		Scan scan = new Scan();
		scan.setCaching(1000);
		scan.setCacheBlocks(false);

		TableMapReduceUtil.initTableMapperJob("item_features", scan, Map.class,Text.class, Text.class, job);
		
//		TableMapReduceUtil.initTableReducerJob("item_features", Reduce.class,job);

	//	FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[0]));
	//	job.setNumReduceTasks(Integer.parseInt(args[3]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}

}
