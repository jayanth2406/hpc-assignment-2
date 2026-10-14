import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class CorrectnessTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private interface ParallelAction { void run(int id) throws Exception; }
    private interface SetList {
        boolean insert(int key); boolean delete(int key); boolean contains(int key); boolean valid();
    }
    private static void parallel(final int workers,final ParallelAction action)throws Exception{
        final CountDownLatch ready=new CountDownLatch(workers),go=new CountDownLatch(1);
        final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        Thread[] ts=new Thread[workers];
        for(int i=0;i<workers;i++){
            final int id=i;
            ts[i]=new Thread(new Runnable(){public void run(){try{
                ready.countDown();go.await();action.run(id);
            }catch(Throwable ex){failure.compareAndSet(null,ex);}}},"correctness-worker-"+i);
        }
        for(Thread t:ts)t.start();
        ready.await();go.countDown();
        for(Thread t:ts)t.join();
        if(failure.get()!=null)throw new AssertionError("parallel correctness worker failed",failure.get());
    }
    private static void listTests(){
        for(Structures.Target t:new Structures.Target[]{new Structures.LazyListTarget(),new Structures.LockFreeListTarget()}){
            t.operation(Structures.INSERT,7);t.operation(Structures.INSERT,3);t.operation(Structures.INSERT,9);
            check(t.valid(),"list links after inserts");
            t.operation(Structures.DELETE,7);check(t.valid(),"list links after delete");
            t.operation(Structures.READ,3);t.operation(Structures.READ,7);
        }
        Structures.LazyListTarget l=new Structures.LazyListTarget();
        check(l.insert(4)&&!l.insert(4)&&l.contains(4)&&l.delete(4)&&!l.contains(4)&&l.valid(),"lazy list semantics");
        int low=Integer.MIN_VALUE+1,high=Integer.MAX_VALUE-1;
        check(l.insert(low)&&l.insert(high)&&l.contains(low)&&l.contains(high),"lazy list boundary keys");
        check(l.delete(low)&&l.delete(high)&&l.valid(),"lazy list boundary deletion");
        check(l.insert(Integer.MIN_VALUE)&&l.insert(Integer.MAX_VALUE),"lazy list accepts full int endpoints");
        check(l.contains(Integer.MIN_VALUE)&&l.contains(Integer.MAX_VALUE)&&l.valid(),"lazy list finds full int endpoints");
        check(l.delete(Integer.MIN_VALUE)&&l.delete(Integer.MAX_VALUE)&&!l.contains(Integer.MIN_VALUE)&&!l.contains(Integer.MAX_VALUE)&&l.valid(),"lazy list removes full int endpoints");
        Structures.LockFreeListTarget f=new Structures.LockFreeListTarget();
        check(f.insert(4)&&!f.insert(4)&&f.contains(4)&&f.delete(4)&&!f.contains(4)&&f.valid(),"lock-free list semantics");
        check(f.insert(low)&&f.insert(high)&&f.contains(low)&&f.contains(high),"lock-free list boundary keys");
        check(f.delete(low)&&f.delete(high)&&f.valid(),"lock-free list boundary deletion");
        check(f.insert(Integer.MIN_VALUE)&&f.insert(Integer.MAX_VALUE),"lock-free list accepts full int endpoints");
        check(f.contains(Integer.MIN_VALUE)&&f.contains(Integer.MAX_VALUE)&&f.valid(),"lock-free list finds full int endpoints");
        check(f.delete(Integer.MIN_VALUE)&&f.delete(Integer.MAX_VALUE)&&!f.contains(Integer.MIN_VALUE)&&!f.contains(Integer.MAX_VALUE)&&f.valid(),"lock-free list removes full int endpoints");
    }
    private static void stackTests(){
        Structures.Stack[] stacks={new Structures.LockStack(),new Structures.TreiberStack(),new Structures.EliminationStack()};
        for(Structures.Stack s:stacks){
            check(s.peek()==null&&s.pop()==null,"empty stack");
            s.push(11);s.push(22);check(s.peek()==22,"stack peek");
            check(s.pop()==22&&s.pop()==11&&s.pop()==null,"stack LIFO");
            check(s.valid(),"stack acyclic");
        }
    }
    private static void queueTests(){
        Structures.Queue[] queues={new Structures.LockQueue(),new Structures.MsQueue()};
        for(Structures.Queue q:queues){
            check(q.peek()==null&&q.dequeue()==null,"empty queue");
            q.enqueue(11);q.enqueue(22);check(q.peek()==11,"queue peek");
            check(q.dequeue()==11&&q.dequeue()==22&&q.dequeue()==null,"queue FIFO");
            check(q.valid(),"queue acyclic");
        }
    }
    private static void concurrentTarget(String q,String impl)throws Exception{
        final Structures.Target target=Structures.create(q,impl);target.prefill(100);
        final int workers=8,iterations=12000;
        final CountDownLatch ready=new CountDownLatch(workers),go=new CountDownLatch(1);
        final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        Thread[] ts=new Thread[workers];
        for(int t=0;t<workers;t++){
            final int id=t;
            ts[t]=new Thread(new Runnable(){public void run(){try{
                Random r=new Random(9001L+id);
                ready.countDown();go.await();
                for(int i=0;i<iterations;i++){
                    int k=101+r.nextInt(500);
                    int phase=i%4;
                    target.operation(phase==0?Structures.READ:(phase==1?Structures.INSERT:Structures.DELETE),k);
                }
            }catch(Throwable ex){failure.compareAndSet(null,ex);}}});
        }
        for(Thread t:ts)t.start();ready.await();go.countDown();for(Thread t:ts)t.join();
        if(failure.get()!=null)throw new AssertionError(q+"/"+impl+" concurrent failure",failure.get());
        check(target.valid(),q+"/"+impl+" structural invariant");
    }
    private static void concurrentListValues(final SetList list)throws Exception{
        final int workers=8,perWorker=100,duplicateKey=900001;
        final AtomicInteger duplicateInsertions=new AtomicInteger();
        parallel(workers,new ParallelAction(){public void run(int id){
            for(int i=0;i<perWorker;i++)check(list.insert(id*perWorker+i+1),"unique concurrent list insert");
            if(list.insert(duplicateKey))duplicateInsertions.incrementAndGet();
        }});
        check(duplicateInsertions.get()==1,"concurrent duplicate list insert");
        for(int k=1;k<=workers*perWorker;k++)check(list.contains(k),"concurrently inserted list key missing: "+k);
        check(list.contains(duplicateKey)&&!list.contains(duplicateKey+1),"list membership after concurrent inserts");
        check(list.valid(),"list links after concurrent inserts");
        parallel(workers,new ParallelAction(){public void run(int id){
            for(int i=0;i<perWorker;i++)check(list.delete(id*perWorker+i+1),"unique concurrent list delete");
        }});
        final AtomicInteger duplicateDeletions=new AtomicInteger();
        parallel(workers,new ParallelAction(){public void run(int id){
            if(list.delete(duplicateKey))duplicateDeletions.incrementAndGet();
        }});
        check(duplicateDeletions.get()==1,"concurrent duplicate list delete");
        for(int k=1;k<=workers*perWorker;k++)check(!list.contains(k),"deleted list key remains: "+k);
        check(!list.contains(duplicateKey)&&list.valid(),"list links after concurrent deletes");
    }
    private static void listValueTests()throws Exception{
        final Structures.LazyListTarget lazy=new Structures.LazyListTarget();
        concurrentListValues(new SetList(){public boolean insert(int k){return lazy.insert(k);}
            public boolean delete(int k){return lazy.delete(k);}public boolean contains(int k){return lazy.contains(k);}
            public boolean valid(){return lazy.valid();}});
        final Structures.LockFreeListTarget snapshot=new Structures.LockFreeListTarget();
        concurrentListValues(new SetList(){public boolean insert(int k){return snapshot.insert(k);}
            public boolean delete(int k){return snapshot.delete(k);}public boolean contains(int k){return snapshot.contains(k);}
            public boolean valid(){return snapshot.valid();}});
    }
    private static void stackValueTests(final Structures.Stack stack)throws Exception{
        final int workers=8,perWorker=100;
        if(stack instanceof Structures.EliminationStack)((Structures.EliminationStack)stack).configureThreads(workers);
        parallel(workers,new ParallelAction(){public void run(int id){
            for(int i=0;i<perWorker;i++)stack.push(id*perWorker+i);
        }});
        HashSet<Integer> values=new HashSet<Integer>();
        for(int i=0;i<workers*perWorker;i++){
            Integer v=stack.pop();check(v!=null,"concurrent stack lost a pushed value");
            check(v>=0&&v<workers*perWorker,"concurrent stack returned an invalid value");
            check(values.add(v),"concurrent stack returned a duplicate value");
        }
        check(stack.pop()==null&&stack.valid(),"stack not empty or invalid after concurrent drain");
    }
    private static void queueValueTests(final Structures.Queue queue)throws Exception{
        final int workers=8,perWorker=100;
        parallel(workers,new ParallelAction(){public void run(int id){
            for(int i=0;i<perWorker;i++)queue.enqueue(id*perWorker+i);
        }});
        int[] last=new int[workers];Arrays.fill(last,-1);
        HashSet<Integer> values=new HashSet<Integer>();
        for(int i=0;i<workers*perWorker;i++){
            Integer v=queue.dequeue();check(v!=null,"concurrent queue lost an enqueued value");
            check(v>=0&&v<workers*perWorker&&values.add(v),"concurrent queue returned an invalid or duplicate value");
            int producer=v/perWorker,sequence=v%perWorker;
            check(sequence>last[producer],"queue violated per-producer FIFO order");
            last[producer]=sequence;
        }
        check(queue.dequeue()==null&&queue.valid(),"queue not empty or invalid after concurrent drain");
    }
    private static void eliminationPairTest()throws Exception{
        final int producers=8,consumers=8,perProducer=100,total=producers*perProducer;
        final Structures.EliminationStack stack=new Structures.EliminationStack();
        stack.configureThreads(producers+consumers);
        final AtomicInteger popped=new AtomicInteger();
        final AtomicIntegerArray seen=new AtomicIntegerArray(total);
        parallel(producers+consumers,new ParallelAction(){public void run(int id)throws Exception{
            if(id<producers){
                for(int i=0;i<perProducer;i++)stack.push(id*perProducer+i);
                return;
            }
            long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(popped.get()<total){
                Integer value=stack.pop();
                if(value==null){
                    if(System.nanoTime()>=deadline)throw new AssertionError("elimination stack did not deliver all concurrent pushes");
                    Thread.yield();
                    continue;
                }
                check(value>=0&&value<total,"elimination stack returned an invalid value");
                check(seen.incrementAndGet(value)==1,"elimination stack duplicated a concurrent value");
                popped.incrementAndGet();
            }
        }});
        check(popped.get()==total,"elimination stack lost concurrent values");
        for(int i=0;i<total;i++)check(seen.get(i)==1,"elimination stack omitted value "+i);
        check(stack.pop()==null&&stack.valid(),"elimination stack not empty or slots not clear after paired stress");
    }
    private static void valuePreservationTests()throws Exception{
        stackValueTests(new Structures.LockStack());
        stackValueTests(new Structures.TreiberStack());
        stackValueTests(new Structures.EliminationStack());
        eliminationPairTest();
        queueValueTests(new Structures.LockQueue());
        queueValueTests(new Structures.MsQueue());
    }
    public static void main(String[] args)throws Exception{
        listTests();stackTests();queueTests();
        concurrentTarget("Q1","lazy");concurrentTarget("Q1","lockfree");
        concurrentTarget("Q2","lock");concurrentTarget("Q2","treiber");
        concurrentTarget("Q3","lock");concurrentTarget("Q3","ms");
        concurrentTarget("Q4","treiber");concurrentTarget("Q4","elimination");
        listValueTests();valuePreservationTests();
        System.out.println("PASS: sequential semantics, concurrent stress, value preservation, and structural invariants for all implementations.");
    }
}
