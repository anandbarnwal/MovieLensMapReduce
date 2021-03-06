package com.movie.common;

import java.io.IOException;
import java.lang.reflect.Method;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Mapper;

import org.apache.hadoop.mapreduce.lib.input.FileSplit;

/**
 * @author mehtab khan
 * @description RatingMapper class to process the Ratings data file.
 */
public class RatingMapper extends Mapper<LongWritable, Text, Text, JoinWritable> {

	// Create string attributes to hold the file name.
	private String mFilename;
	private Text mMovieId = new Text();

	@Override
	protected void setup(Mapper<LongWritable, Text, Text, JoinWritable>.Context context)
			throws IOException, InterruptedException {
		// Taking multiple input file so here we need to handle TaggedInputSplit class
		// otherwise we will get ClassCastException
		FileSplit fileSplit = null;
		InputSplit split = context.getInputSplit();
		Class<? extends InputSplit> splitClass = split.getClass();
		if (splitClass.equals(FileSplit.class)) {
			fileSplit = (FileSplit) split;
		} else if (splitClass.getName().equals("org.apache.hadoop.mapreduce.lib.input.TaggedInputSplit")) {
			// begin reflection hackery
			try {
				Method getInputSplitMethod = splitClass.getDeclaredMethod("getInputSplit");
				getInputSplitMethod.setAccessible(true);
				fileSplit = (FileSplit) getInputSplitMethod.invoke(split);
			} catch (Exception e) {
				// wrap and re-throw error
				throw new IOException(e);
			}
			mFilename = fileSplit.getPath().getName();
			// end reflection hackery
		}
	}

	@Override
	protected void map(LongWritable key, Text values, Mapper<LongWritable, Text, Text, JoinWritable>.Context context)
			throws IOException, InterruptedException {
		String data = values.toString(); // UserID::MovieID::Rating::Timestamp

		String[] field = data.split("::");
		// handling the bad records and also taking care of userId
		if (null != field && field.length == 4 && field[0].length() > 0) {
			mMovieId.set(field[1]);
			context.write(mMovieId, new JoinWritable(field[2], mFilename)); // movieId and rating with filename
		}
	}

}
