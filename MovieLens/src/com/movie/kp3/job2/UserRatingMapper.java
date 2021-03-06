package com.movie.kp3.job2;

import java.io.IOException;
import java.lang.reflect.Method;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

import com.movie.common.JoinWritable;


public class UserRatingMapper extends Mapper<Object, Text, Text, JoinWritable> {

	private String filename;
	private Text mMovieID = new Text();

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {

		FileSplit fileSplit = null;

		InputSplit split = context.getInputSplit();
		Class<? extends InputSplit> splitClass = split.getClass();

		if (splitClass.equals(FileSplit.class)) {
			fileSplit = (FileSplit) split;
		} else if (splitClass.getName().equals("org.apache.hadoop.mapreduce.lib.input.TaggedInputSplit")) {
			// begin reflection hackery...
			try {
				Method getInputSplitMethod = splitClass.getDeclaredMethod("getInputSplit");
				getInputSplitMethod.setAccessible(true);
				fileSplit = (FileSplit) getInputSplitMethod.invoke(split);
			} catch (Exception e) {
				// wrap and re-throw error
				throw new IOException(e);
			}
			filename = fileSplit.getPath().getName();
			// end reflection hackery
		}
	}

	@Override
	public void map(Object key, Text values, Context context) throws IOException, InterruptedException {
		String data = values.toString(); // AgeRange::Occupation::MovieId::Rating

		String[] field = data.split("::");
		if (field != null && field.length == 4) {

			mMovieID.set(field[2]);
			context.write(mMovieID, new JoinWritable(field[0] + "::" + field[1] + "::" + field[3], filename));
		}

	}

}
