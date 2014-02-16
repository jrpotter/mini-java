package tester;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/* Automated regression tester for Checkpoint 1 tests
 * Created by Max Beckman-Harned
 * Put your tests in "tests/pa1_tests" folder in your Eclipse workspace directory
 */
public class Checkpoint1 {
	
	static ExecutorService threadPool = Executors.newCachedThreadPool();

	public static void main(String[] args) throws IOException, InterruptedException {
		File testDir = new File(System.getProperty("java.class.path") + "/../tests/pa1_tests");
		System.out.println(testDir.getAbsolutePath());
		int failures = 0;
		for (File x : testDir.listFiles()) {
			int returnCode = runTest(x);
			if (x.getName().indexOf("pass") != -1) {
				if (returnCode == 0)
					System.out.println(x.getName() + " passed successfully!");
				else {
					failures++;
					System.err.println(x.getName()
							+ " failed but should have passed!");
				}
			} else {
				if (returnCode == 4)
					System.out.println(x.getName() + " failed successfully!");
				else {
					System.err.println(x.getName() + " did not fail properly!");
					failures++;
				}
			}
		}
		System.out.println(failures + " failures in all.");	
	}
	
	private static int runTest(File x) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder("java", "miniJava.Compiler", x.getPath()).directory(new File(System.getProperty("java.class.path")));
		Process p = pb.start();
		threadPool.execute(new ProcessOutputter(p.getInputStream(), false));
		p.waitFor();
		return p.exitValue();
	}
	
	static class ProcessOutputter implements Runnable {
		private Scanner processOutput;
		private boolean output;
		
		public ProcessOutputter(InputStream _processStream, boolean _output) {
			processOutput = new Scanner(_processStream);
			output = _output;
		}
		@Override
		public void run() {
			while(processOutput.hasNextLine()) {
				String line = processOutput.nextLine();
				if (output)
					System.out.println(line);
			}
		}
		
		
	}
}
