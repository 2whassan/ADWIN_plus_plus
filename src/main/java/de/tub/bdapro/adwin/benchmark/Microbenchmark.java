package de.tub.bdapro.adwin.benchmark;


import de.tub.bdapro.adwin.ADWINInterface;
import de.tub.bdapro.adwin.ADWINWrapper;
import de.tub.bdapro.adwin.ADWINWrapperOriginal;
import de.tub.bdapro.adwin.SnapshotThreadExecutorADWINWrapper;
import de.tub.bdapro.adwin.core.HalfCutCheckThreadExecutorADWINImpl;
import de.tub.bdapro.adwin.core.SequentialADWINImpl;
import de.tub.bdapro.adwin.core.histogram.Histogram;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

import java.util.Scanner;

/**
 * This is the benchmark for the evaluation of ADWIN.
 * We Insert 100 batches with 1 mio. elements with a constant value
 * This Micobenchmark makes use of our {@link DataGenerator},
 * which generates concepts drift according to the benchmark parameters.
 *
 * To execute the benchmark we define the following program parameter:
 *
 * AdwinType = ORIGINAL | SERIAL | HALFCUT | SNAPSHOT
 * ChangeType = ABRUPT | INCREMENTAL | GRADUAL | OUTLIER | CONSTANT
 * BatchSize = the number of elements in one batch
 * NumConstant = the number of constant elements between a concept drift
 * NumChange = the length of a concept drift
 * Delta = the delta parameter of ADWIN
 *
 *
 * For example:
 * Snapshot constant 1000000 10 10 0.001
 *
 */
@State(Scope.Benchmark)
public class Microbenchmark {

    private static ADWINInterface adwin;

    private int adwinCount;

    private DataGenerator dataGenerator;

    private double[] data;

    private int numInvocations;

    private int numTotalInvocations;

    private boolean warmup;







    // Dummy parameter values, because JMH requires default parameter values.
    // The actual parameter values are set in the main method.
    @Param({"SNAPSHOT"})
    public AdwinType adwinType;

    @Param({"0.01"})
    public double delta;

    @Param({"INCREMENTAL"})
    public DataGenerator.Type changeType;

    @Param({"1000"})
    public int numConstant;

    @Param({"200"})
    public int numChange;

    @Param({"20"})
    public int warmupIterations;

    @Param({"100"})
    public int measurementIterations;

    @Param({"100000"})
    public int batchSize;

    @Param({"10000"})
    public int min_lim;

    @Param({"10000"})
    public int safe_lim;

    @Param({"2000"})
    public int max_lim;

    @Param({"5"})
    public int theta;

    @Param({"5"})
    public int omega;


    @Setup( Level.Trial )
    public void setupTrial() throws Exception {
        data = new double[batchSize];
        warmup = true;
        adwinCount = 0;
        numTotalInvocations = 0;
    }

    @Setup( Level.Iteration )
    public void setupIteration() throws Exception {
        numInvocations = 0;

        //adwin =  new ADWINWrapperOriginal(1);

        adwin = new ADWINWrapper(1,Histogram.class, SequentialADWINImpl.class,15, 51, 60, 70000, 40000);

        //adwin = new ADWINWrapper(1, Histogram.class, HalfCutCheckThreadExecutorADWINImpl.class,5, 15, 20, 10, 0);

         //adwin = new SnapshotThreadExecutorADWINWrapper(1, Histogram.class, SequentialADWINImpl.class, 5, 20, 30, 10, 0);


        File file =
                new File("file");
        Scanner sc = new Scanner(file);

        int i = 0;
        data = new double[(int) file.length()];
        while (sc.hasNextLine()) {
            data[i] = Double.parseDouble(sc.nextLine());
            //System.out.println(data[i]);
            i++;
        }
    }

    @Benchmark()
    public boolean benchmarkAdwin() throws Exception {
        //setupIteration();
       // System.out.println("Item number "+numInvocations);
        System.out.println("Input data is "+data[numInvocations]);

        if (numInvocations % 100 ==0) {
            System.out.println("Current window size is "+adwin.getSize());
            File file =new File("file");
            FileWriter fw = new FileWriter(file,true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(bw);
            pw.println(adwin.getSize());
            pw.close();
        }

        return adwin.addElement(data[numInvocations++]);
    }


    @Benchmark()
    public void adaptiveWindowDrop(){
        adwin.adaptiveDrop();
    }

    @TearDown( Level.Iteration )
    public void teardownIteration() {
        System.out.print("<Number of Adwin cut checks performed: " + (adwin.getAdwinCount() - adwinCount) + "> ");
        adwinCount = adwin.getAdwinCount();
        numTotalInvocations += numInvocations;
        if (warmup && numTotalInvocations == warmupIterations * batchSize) {
            adwin.terminateAdwin();
            warmup = false;
            adwinCount = 0;
            numTotalInvocations = 0;
            adwin = null;
            dataGenerator = null;
        }
    }

    @TearDown( Level.Trial )
    public void teardownTrial() {
        adwin.terminateAdwin();
    }


    public static void main(String[] args) throws Exception {

        long startTime = System.currentTimeMillis();
        //addWinRunner();
        Microbenchmark mc = new Microbenchmark();
        //mc.dataGeneratorRunner();
        mc.setupIteration();

        boolean flag;
        int drift=0;
        PrintWriter og = new PrintWriter(new FileWriter("file"));
        for (int i = 1; i< 2000000; i++)
        {
            flag = mc.benchmarkAdwin();
            System.out.println(flag);
            og.println(flag);
            og.println(adwin.getMinLimit());

            if(flag){
                drift+=1;
            }
            mc.adaptiveWindowDrop();
            //System.out.println(adwin.getMinLimit());
            //System.out.println(adwin.minWindowMovingRate());
            //System.out.println(adwin.waitingAfterDriftElements());
        }
        og.close();
        mc.teardownTrial();
        System.out.println();
        System.out.println("Number of drifts is: " + drift);
        //mc.Generator();
        long endTime   = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        NumberFormat formatter = new DecimalFormat("#0.00000");
        System.out.print("Execution time is " + formatter.format((endTime - startTime) / 1000d) + " seconds");
    }


    private  double[] dataGeneratorRunner() throws IOException {
        this.dataGenerator = new DataGenerator(100000, DataGenerator.Type.ABRUPT,5000,DataGenerator.VarianceType.NOVARIANCE);
        double r[] = new double[2000000];
        PrintWriter pw = new PrintWriter(new FileWriter(""));
        for (int i = 1; i< 2000000; i++) {

            //System.out.println(dataGenerator.getNext());
            r[i] = dataGenerator.getNext();
            pw.println(r[i]);

        }
        pw.close();
        return r;

    }



    private static void addWinRunner() throws RunnerException {
        String argAdwinType = "SERIAL";
        String argChangeType = "INCREMENTAL";
        String argBatchSize = "3";
        String argNumConstant = "20";
        String argNumChange = "20";
        String argDelta = "1";

        String warmupIterations = "20";
        String measurementIterations = "100";

        Options opt = new OptionsBuilder()
                .include(Microbenchmark.class.getName())
                .mode(Mode.SingleShotTime)
                .timeUnit(TimeUnit.MILLISECONDS)
                .warmupIterations(Integer.valueOf(warmupIterations))
                .warmupBatchSize(Integer.valueOf(argBatchSize))
                .measurementIterations(Integer.valueOf(measurementIterations))
                .measurementBatchSize(Integer.valueOf(argBatchSize))
                .param("adwinType", argAdwinType)
                .param("delta", argDelta)
                .param("changeType", argChangeType)
                .param("numConstant", argNumConstant)
                .param("numChange", argNumChange)
                .param("warmupIterations", warmupIterations)
                .param("measurementIterations", measurementIterations)
                .param("batchSize", argBatchSize)
                .forks(1)
                .build();

        new Runner(opt).run();
    }





    private ADWINInterface newAdwin() throws Exception {
        switch (adwinType) {
            case ORIGINAL:
                return new ADWINWrapperOriginal(delta);
            case SERIAL:
                return new ADWINWrapper(delta,  Histogram.class, SequentialADWINImpl.class,safe_lim, min_lim, max_lim, theta, omega);
            case HALFCUT:
                return new ADWINWrapper(delta,  Histogram.class, HalfCutCheckThreadExecutorADWINImpl.class,safe_lim, min_lim, max_lim, theta, omega);
            case SNAPSHOT:
                return new SnapshotThreadExecutorADWINWrapper(delta, Histogram.class, SequentialADWINImpl.class,safe_lim, min_lim, max_lim, theta, omega);
        }
        throw new Exception("Unknown ADWIN type");
    }


    private DataGenerator newDatagenerator() {
        return new DataGenerator(numConstant, changeType, numChange, DataGenerator.VarianceType.NOVARIANCE);
    }


    public enum AdwinType { ORIGINAL, SERIAL, HALFCUT, SNAPSHOT }
}
