import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public final class HPC2 {
    private static final long DEFAULT_MILLIS=10_000L;
    private static final int TRACE_LENGTH=1024;
    private static final String[] MIX_NAMES={"0C-50I-50D","50C-25I-25D","100C-0I-0D"};
    private static final int[] FULL_THREADS={1,2,4,6,8,10,12,14,16};
    private static final String HEADER="question,implementation,n,workload,threads,repetition,elapsed_ns,operations,throughput_mops,verified,host";

    private static final class Trace {
        final byte[] ops; final int[] keys;
        Trace(byte[] o,int[] k){ops=o;keys=k;}
    }
    private static Trace makeTrace(int n,int workload,long seed){
        int[] counts;
        if(workload==0)counts=new int[]{0,512,512};
        else if(workload==1)counts=new int[]{512,256,256};
        else counts=new int[]{1024,0,0};
        byte[] ops=new byte[TRACE_LENGTH];int at=0;
        for(byte type=0;type<3;type++)for(int i=0;i<counts[type];i++)ops[at++]=type;
        Random r=new Random(seed);
        for(int i=ops.length-1;i>0;i--){int j=r.nextInt(i+1);byte t=ops[i];ops[i]=ops[j];ops[j]=t;}
        int[] keys=new int[TRACE_LENGTH];
        for(int i=0;i<keys.length;i++)keys[i]=ops[i]==0?1+r.nextInt(n):n+1+r.nextInt(n);
        return new Trace(ops,keys);
    }
    private static List<String> implementations(String q){
        if(q.equals("Q1"))return Arrays.asList("lazy","lockfree");
        if(q.equals("Q2"))return Arrays.asList("lock","treiber");
        if(q.equals("Q3"))return Arrays.asList("lock","ms");
        if(q.equals("Q4"))return Arrays.asList("treiber","elimination");
        throw new IllegalArgumentException("Question must be Q1, Q2, Q3, or Q4.");
    }
    private static void runOne(String q,String impl,int n,int workload,int threads,int rep,long millis,BufferedWriter out) throws Exception {
        Structures.Target target=Structures.create(q,impl);
        target.configureThreads(1);
        int prefill=n/2;
        target.prefill(prefill);
        Trace[] traces=new Trace[threads];
        for(int t=0;t<threads;t++){
            long seed=0x9e3779b97f4a7c15L ^ ((long)n<<20) ^ ((long)workload<<16) ^ ((long)threads<<8) ^ ((long)rep<<4) ^ t;
            traces[t]=makeTrace(n,workload,seed);
        }
        target.configureThreads(threads);
        CountDownLatch ready=new CountDownLatch(threads),gate=new CountDownLatch(1);
        long[] counts=new long[threads];
        AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        Thread[] workers=new Thread[threads];
        for(int t=0;t<threads;t++){
            final int id=t;final Trace trace=traces[t];
            workers[t]=new Thread(new Runnable(){public void run(){
                try{
                    ready.countDown();gate.await();
                    long count=0;int ix=0;
                    while(System.nanoTime()<deadlineRef[0]){
                        target.operation(trace.ops[ix],trace.keys[ix]);count++;
                        if(++ix==trace.ops.length)ix=0;
                    }
                    counts[id]=count;
                }catch(Throwable ex){failure.compareAndSet(null,ex);}
            }},"hpc2-worker-"+t);
        }
        for(Thread worker:workers)worker.start();
        ready.await();
        long start=System.nanoTime();
        deadlineRef[0]=start+TimeUnit.MILLISECONDS.toNanos(millis);
        gate.countDown();
        for(Thread worker:workers)worker.join();
        long elapsed=System.nanoTime()-start;
        Throwable problem=failure.get();
        if(problem!=null)throw new RuntimeException("Worker failed for "+q+"/"+impl,problem);
        boolean valid=target.valid();
        long operations=0;for(long count:counts)operations+=count;
        if(!valid)throw new IllegalStateException("Post-run structure validation failed for "+q+"/"+impl+" N="+n+" workload="+MIX_NAMES[workload]+" threads="+threads+" rep="+rep);
        double mops=(operations/(elapsed/1_000_000_000.0))/1_000_000.0;
        String host=System.getenv("HOSTNAME");if(host==null)host="unknown";
        out.write(String.format(Locale.US,"%s,%s,%d,%s,%d,%d,%d,%d,%.9f,true,%s%n",q,impl,n,MIX_NAMES[workload],threads,rep,elapsed,operations,mops,host));
        out.flush();
        System.out.printf(Locale.US,"%s,%s,N=%d,%s,T=%d,rep=%d,ops=%d,%.6f MOPS,verified=true%n",q,impl,n,MIX_NAMES[workload],threads,rep,operations,mops);
    }
    private static final long[] deadlineRef=new long[1];

    private static void benchmark(String q,Path output,long millis,int reps,boolean smoke) throws Exception {
        Files.createDirectories(output.toAbsolutePath().getParent());
        boolean exists=Files.exists(output)&&Files.size(output)>0;
        try(BufferedWriter writer=Files.newBufferedWriter(output,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND)){
            if(!exists){writer.write(HEADER);writer.newLine();writer.flush();}
            List<String> impls=implementations(q);
            int[] ns=smoke?new int[]{1000}:new int[]{1000,10000};
            int[] threadCounts=smoke?new int[]{1,4,8,16}:FULL_THREADS;
            int[] workloads=smoke?new int[]{0,1,2}:new int[]{0,1,2};
            int runReps=smoke?1:reps;
            long runMillis=smoke?Math.min(millis,250):millis;
            for(String impl:impls)for(int n:ns)for(int w:workloads)for(int t:threadCounts)for(int rep=1;rep<=runReps;rep++)
                runOne(q,impl,n,w,t,rep,runMillis,writer);
        }
    }
    public static void main(String[] args) throws Exception {
        if(args.length==0){usage();System.exit(2);}
        String mode=args[0].toUpperCase(Locale.ROOT);
        if(mode.equals("TEST")){CorrectnessTest.main(new String[0]);return;}
        boolean smoke=mode.equals("SMOKE");
        String q=smoke?(args.length>1?args[1].toUpperCase(Locale.ROOT):"Q1"):mode;
        if(!Arrays.asList("Q1","Q2","Q3","Q4").contains(q)){usage();System.exit(2);}
        Path output=Paths.get(args.length>(smoke?2:1)?args[smoke?2:1]:"results/"+q.toLowerCase(Locale.ROOT)+"_raw.csv");
        long millis=args.length>(smoke?3:2)?Long.parseLong(args[smoke?3:2]):DEFAULT_MILLIS;
        int reps=args.length>(smoke?4:3)?Integer.parseInt(args[smoke?4:3]):5;
        benchmark(q,output,millis,reps,smoke);
    }
    private static void usage(){
        System.err.println("Usage: java HPC2 Q1|Q2|Q3|Q4 [raw.csv] [duration_ms] [repetitions]");
        System.err.println("       java HPC2 SMOKE Q1|Q2|Q3|Q4 [raw.csv]");
        System.err.println("       java HPC2 TEST");
    }
}
