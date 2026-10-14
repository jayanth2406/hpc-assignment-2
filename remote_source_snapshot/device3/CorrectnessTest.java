import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class CorrectnessTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void listTests(){
        for(Structures.Target t:new Structures.Target[]{new Structures.LazyListTarget(),new Structures.LockFreeListTarget()}){
            t.operation(Structures.INSERT,7);t.operation(Structures.INSERT,3);t.operation(Structures.INSERT,9);
            check(t.valid(),"list links after inserts");
            t.operation(Structures.DELETE,7);check(t.valid(),"list links after delete");
            t.operation(Structures.READ,3);t.operation(Structures.READ,7);
        }
        Structures.LazyListTarget l=new Structures.LazyListTarget();
        check(l.insert(4)&&!l.insert(4)&&l.contains(4)&&l.delete(4)&&!l.contains(4)&&l.valid(),"lazy list semantics");
        Structures.LockFreeListTarget f=new Structures.LockFreeListTarget();
        check(f.insert(4)&&!f.insert(4)&&f.contains(4)&&f.delete(4)&&!f.contains(4)&&f.valid(),"lock-free list semantics");
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
    public static void main(String[] args)throws Exception{
        listTests();stackTests();queueTests();
        concurrentTarget("Q1","lazy");concurrentTarget("Q1","lockfree");
        concurrentTarget("Q2","lock");concurrentTarget("Q2","treiber");
        concurrentTarget("Q3","lock");concurrentTarget("Q3","ms");
        concurrentTarget("Q4","treiber");concurrentTarget("Q4","elimination");
        System.out.println("PASS: sequential semantics and concurrent structural stress for all implementations.");
    }
}
