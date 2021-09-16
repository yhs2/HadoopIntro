
import java.io.IOException;
import java.util.StringTokenizer;

import org.json.JSONObject;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class RedditAverage extends Configured implements Tool {

	public static class TokenizerMapper
	extends Mapper<LongWritable, Text, Text, LongPairWritable >{
	
		private final static LongPairWritable pair = new LongPairWritable();
		private Text word = new Text();
		
		@Override
		public void map(LongWritable key, Text value, Context context
				) throws IOException, InterruptedException {
			
			
			
			JSONObject record = new JSONObject(value.toString());
			String userSubreddit = record.getString("subreddit");
			Long score = record.getLong("score");
			word.set(userSubreddit);
			pair.set(1, score);
			context.write(word, pair);
			
		}
	}
	
	public static class DoubleReducer
	extends Reducer<Text, LongPairWritable, Text, DoubleWritable> {
		private final static DoubleWritable a = new DoubleWritable();
		@Override
		public void reduce(Text key, Iterable<LongPairWritable> values,
				Context context
				) throws IOException, InterruptedException {
			int sum = 0;
			long score = 0;
			for (LongPairWritable val : values) {
				sum += val.get_0();
				score += val.get_1();
			}
			a.set((double) score / sum);
			context.write(key, a);
		}
	}
	
	public static class Combiner
	extends Reducer<Text, LongPairWritable, Text, LongPairWritable> {
		private final static LongPairWritable pair = new LongPairWritable();
		@Override
		public void reduce(Text key, Iterable<LongPairWritable> values,
				Context context
				) throws IOException, InterruptedException {
			Long sum = (long) 0;
			Long score = (long) 0;
			for (LongPairWritable val : values) {
				sum += val.get_0();
				score += val.get_1();
			}
			
			pair.set(sum, score);
			
			context.write(key, pair);
		}
	}
	

	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new RedditAverage(), args);
		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		Configuration conf = this.getConf();
		Job job = Job.getInstance(conf, "RedditAverage");
		job.setJarByClass(RedditAverage.class);

		job.setInputFormatClass(TextInputFormat.class);

		job.setMapperClass(TokenizerMapper.class);
		job.setCombinerClass(Combiner.class);
		job.setReducerClass(DoubleReducer.class);
		
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(LongPairWritable.class);
		
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(DoubleWritable.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		TextInputFormat.addInputPath(job, new Path(args[0]));
		TextOutputFormat.setOutputPath(job, new Path(args[1]));

		return job.waitForCompletion(true) ? 0 : 1;
	}
}
