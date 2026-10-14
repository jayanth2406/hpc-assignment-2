import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ThreadLocalRandom;

final class Structures {
    static final byte READ=0, INSERT=1, DELETE=2;
    interface Target {
        void prefill(int count);
        void operation(byte op, int value);
        default void configureThreads(int workers) {}
        boolean valid();
    }
    static Target create(String question, String implementation) {
        switch (question.toUpperCase()) {
            case "Q1":
                return implementation.equalsIgnoreCase("lazy") ? new LazyListTarget() : new LockFreeListTarget();
            case "Q2":
                return implementation.equalsIgnoreCase("lock") ? new LockStackTarget() : new TreiberStackTarget();
            case "Q3":
                return implementation.equalsIgnoreCase("lock") ? new LockQueueTarget() : new MsQueueTarget();
            case "Q4":
                return implementation.equalsIgnoreCase("treiber") ? new TreiberStackTarget() : new EliminationStackTarget();
            default: throw new IllegalArgumentException("Unknown question: "+question);
        }
    }

    static final class LazyListTarget implements Target {
        private static final class Node {
            final long key; volatile Node next, prev; volatile boolean marked;
            final ReentrantLock lock=new ReentrantLock();
            Node(long key){this.key=key;}
        }
        // Keep sentinels outside the int domain so every int is a valid key.
        private final Node head=new Node(Long.MIN_VALUE), tail=new Node(Long.MAX_VALUE);
        LazyListTarget(){head.next=tail;tail.prev=head;}
        private Node[] window(int key) {
            Node pred=head, curr=pred.next;
            while(curr!=tail && curr.key<key){pred=curr;curr=curr.next;}
            return new Node[]{pred,curr};
        }
        private boolean validWindow(Node p,Node c){return !p.marked&&!c.marked&&p.next==c&&c.prev==p;}
        boolean insert(int key) {
            for(;;){
                Node[] w=window(key);Node p=w[0],c=w[1];
                p.lock.lock();c.lock.lock();
                try{
                    if(!validWindow(p,c))continue;
                    if(c.key==key)return false;
                    Node n=new Node(key);n.prev=p;n.next=c;p.next=n;c.prev=n;return true;
                }finally{c.lock.unlock();p.lock.unlock();}
            }
        }
        boolean delete(int key) {
            for(;;){
                Node[] w=window(key);Node p=w[0],c=w[1];
                p.lock.lock();c.lock.lock();
                try{
                    if(!validWindow(p,c))continue;
                    if(c.key!=key)return false;
                    c.marked=true;Node s=c.next;p.next=s;s.prev=p;return true;
                }finally{c.lock.unlock();p.lock.unlock();}
            }
        }
        boolean contains(int key) {
            for(;;){
                Node[] w=window(key);Node p=w[0],c=w[1];
                p.lock.lock();c.lock.lock();
                try{if(!validWindow(p,c))continue;return c.key==key&&!c.marked;}
                finally{c.lock.unlock();p.lock.unlock();}
            }
        }
        public void prefill(int n){for(int i=1;i<=n;i++)insert(i);}
        public void operation(byte op,int key){if(op==READ)contains(key);else if(op==INSERT)insert(key);else delete(key);}
        public boolean valid(){
            Node p=head,c=head.next;long last=Long.MIN_VALUE;int steps=0;
            while(c!=tail){if(c==null||c.key<=last||c.marked||c.prev!=p||++steps>20_000_000)return false;last=c.key;p=c;c=c.next;}
            return tail.prev==p&&p.next==tail;
        }
    }

    /**
     * Lock-free ordered doubly linked set using immutable snapshots.
     * Each update builds a complete new doubly linked version and publishes it
     * with one CAS. Readers traverse a stable version without locks.
     * This favors simple, auditable correctness over update efficiency (O(N)).
     */
    static final class LockFreeListTarget implements Target {
        private static final class Node {
            final long key;
            Node prev, next;
            Node(long key){this.key=key;}
        }
        private static final class State {
            final Node head, tail;
            final int size;
            State(Node h,Node t,int n){head=h;tail=t;size=n;}
        }
        private final AtomicReference<State> state =
                new AtomicReference<State>(build(new int[0]));
        private static State build(int[] keys){
            Node[] nodes=new Node[keys.length+2];
            nodes[0]=new Node(Long.MIN_VALUE);
            for(int i=0;i<keys.length;i++)nodes[i+1]=new Node(keys[i]);
            nodes[nodes.length-1]=new Node(Long.MAX_VALUE);
            for(int i=0;i<nodes.length;i++){
                nodes[i].prev=i==0?null:nodes[i-1];
                nodes[i].next=i+1==nodes.length?null:nodes[i+1];
            }
            return new State(nodes[0],nodes[nodes.length-1],keys.length);
        }
        boolean insert(int key){
            for(;;){
                State before=state.get();
                int pos=0;Node c=before.head.next;
                while(c!=before.tail&&c.key<key){pos++;c=c.next;}
                if(c!=before.tail&&c.key==key)return false;
                int[] keys=new int[before.size+1];int j=0;boolean put=false;
                for(c=before.head.next;c!=before.tail;c=c.next){
                    if(!put&&j==pos){keys[j++]=key;put=true;}
                    keys[j++]=(int)c.key;
                }
                if(!put)keys[j]=key;
                State after=build(keys);
                if(state.compareAndSet(before,after))return true;
            }
        }
        boolean delete(int key){
            for(;;){
                State before=state.get();
                int pos=0;Node c=before.head.next;
                while(c!=before.tail&&c.key<key){pos++;c=c.next;}
                if(c==before.tail||c.key!=key)return false;
                int[] keys=new int[before.size-1];int j=0;
                for(c=before.head.next;c!=before.tail;c=c.next)
                    if(c.key!=key)keys[j++]=(int)c.key;
                State after=build(keys);
                if(state.compareAndSet(before,after))return true;
            }
        }
        boolean contains(int key){
            State snapshot=state.get();
            for(Node c=snapshot.head.next;c!=snapshot.tail&&c.key<=key;c=c.next)
                if(c.key==key)return true;
            return false;
        }
        public void prefill(int n){
            int[] keys=new int[n];for(int i=0;i<n;i++)keys[i]=i+1;
            state.set(build(keys));
        }
        public void operation(byte op,int key){
            if(op==READ)contains(key);else if(op==INSERT)insert(key);else delete(key);
        }
        public boolean valid(){
            State s=state.get();int count=0;long last=Long.MIN_VALUE;Node prev=null;
            for(Node c=s.head;c!=null;c=c.next){
                if(c.prev!=prev)return false;
                if(prev!=null&&prev.next!=c)return false;
                if(c!=s.head&&c!=s.tail){
                    if(c.key<=last)return false;last=c.key;count++;
                }
                prev=c;
                if(count>s.size)return false;
            }
            if(prev!=s.tail||count!=s.size||s.head.prev!=null||s.tail.next!=null)return false;
            count=0;Node next=null;
            for(Node c=s.tail;c!=null;c=c.prev){
                if(c.next!=next)return false;
                if(c!=s.head&&c!=s.tail)count++;
                next=c;
                if(count>s.size)return false;
            }
            return next==s.head&&count==s.size;
        }
    }

    interface Stack {void push(int x);Integer pop();Integer peek();boolean valid();default void configureThreads(int workers) {}}
    static final class Link {final int value;final Link next;Link(int v,Link n){value=v;next=n;}}
    static boolean acyclic(Link p){Link s=p,f=p;while(f!=null&&f.next!=null){s=s.next;f=f.next.next;if(s==f)return false;}return true;}
    static final class LockStack implements Stack {
        private final ReentrantLock lock=new ReentrantLock();private Link top;
        public void push(int x){lock.lock();try{top=new Link(x,top);}finally{lock.unlock();}}
        public Integer pop(){lock.lock();try{if(top==null)return null;int v=top.value;top=top.next;return v;}finally{lock.unlock();}}
        public Integer peek(){lock.lock();try{return top==null?null:top.value;}finally{lock.unlock();}}
        public boolean valid(){return acyclic(top);}
    }
    static final class TreiberStack implements Stack {
        private final AtomicReference<Link> top=new AtomicReference<Link>();
        public void push(int x){for(;;){Link old=top.get();if(top.compareAndSet(old,new Link(x,old)))return;}}
        public Integer pop(){for(;;){Link old=top.get();if(old==null)return null;if(top.compareAndSet(old,old.next))return old.value;}}
        public Integer peek(){Link p=top.get();return p==null?null:p.value;}
        public boolean valid(){return acyclic(top.get());}
    }
    static final class EliminationStack implements Stack {
        private static final int WAITING=0,MATCHED=1,CANCELLED=2;
        private static final class Offer {final int value;final AtomicInteger state=new AtomicInteger(WAITING);Offer(int v){value=v;}}
        private final TreiberStack central=new TreiberStack();
        private final AtomicReferenceArray<Offer> slots=new AtomicReferenceArray<Offer>(16);
        private volatile int workerCount=16;
        public void configureThreads(int workers){workerCount=workers;}
        public void push(int x){
            if(eliminatePush(x))return;central.push(x);
        }
        private boolean eliminatePush(int x){
            if(workerCount<=1)return false;
            int range=Math.min(slots.length(),Math.max(2,workerCount*2));
            for(int probe=0;probe<4;probe++){
                int i=ThreadLocalRandom.current().nextInt(range);Offer o=new Offer(x);
                if(!slots.compareAndSet(i,null,o))continue;
                long deadline=System.nanoTime()+2_000L;
                while(System.nanoTime()<deadline){
                    int s=o.state.get();if(s==MATCHED){slots.compareAndSet(i,o,null);return true;}if(s!=WAITING)break;
                }
                if(o.state.compareAndSet(WAITING,CANCELLED)){slots.compareAndSet(i,o,null);}
                else if(o.state.get()==MATCHED){slots.compareAndSet(i,o,null);return true;}
                else slots.compareAndSet(i,o,null);
            }
            return false;
        }
        public Integer pop(){
            if(workerCount<=1)return central.pop();
            int range=Math.min(slots.length(),Math.max(2,workerCount*2));
            int first=ThreadLocalRandom.current().nextInt(range);
            for(int probe=0;probe<range;probe++){
                int i=(first+probe)%range;Offer o=slots.get(i);
                if(o==null)continue;
                if(o.state.compareAndSet(WAITING,MATCHED)){slots.compareAndSet(i,o,null);return o.value;}
                if(o.state.get()!=WAITING)slots.compareAndSet(i,o,null);
            }
            return central.pop();
        }
        public Integer peek(){return central.peek();}
        public boolean valid(){
            for(int i=0;i<slots.length();i++)if(slots.get(i)!=null)return false;
            return central.valid();
        }
    }
    abstract static class StackTarget implements Target {
        abstract Stack stack();
        public void prefill(int n){for(int i=1;i<=n;i++)stack().push(i);}
        public void operation(byte op,int key){if(op==READ)stack().peek();else if(op==INSERT)stack().push(key);else stack().pop();}
        public boolean valid(){return stack().valid();}
    }
    static final class LockStackTarget extends StackTarget {private final LockStack s=new LockStack();Stack stack(){return s;}}
    static final class TreiberStackTarget extends StackTarget {private final TreiberStack s=new TreiberStack();Stack stack(){return s;}}
    static final class EliminationStackTarget extends StackTarget {private final EliminationStack s=new EliminationStack();Stack stack(){return s;}public void configureThreads(int workers){s.configureThreads(workers);}}

    interface Queue {void enqueue(int x);Integer dequeue();Integer peek();boolean valid();}
    static final class QNode {final int value;volatile QNode next;QNode(int v){value=v;}}
    static final class LockQueue implements Queue {
        private final ReentrantLock lock=new ReentrantLock();private QNode head,tail;
        public void enqueue(int x){lock.lock();try{QNode n=new QNode(x);if(tail==null)head=n;else tail.next=n;tail=n;}finally{lock.unlock();}}
        public Integer dequeue(){lock.lock();try{if(head==null)return null;int v=head.value;head=head.next;if(head==null)tail=null;return v;}finally{lock.unlock();}}
        public Integer peek(){lock.lock();try{return head==null?null:head.value;}finally{lock.unlock();}}
        public boolean valid(){QNode slow=head,fast=head;while(fast!=null&&fast.next!=null){slow=slow.next;fast=fast.next.next;if(slow==fast)return false;}return (head==null)==(tail==null)&&(tail==null||tail.next==null);}
    }
    static final class ANode {
        final int value;final AtomicReference<ANode> next=new AtomicReference<ANode>();
        ANode(int v){value=v;}
    }
    static final class MsQueue implements Queue {
        private final AtomicReference<ANode> head,tail;
        MsQueue(){ANode d=new ANode(0);head=new AtomicReference<ANode>(d);tail=new AtomicReference<ANode>(d);}
        public void enqueue(int x){
            ANode n=new ANode(x);
            for(;;){ANode t=tail.get(),s=t.next.get();if(t==tail.get()){
                if(s==null){if(t.next.compareAndSet(null,n)){tail.compareAndSet(t,n);return;}}
                else tail.compareAndSet(t,s);
            }}
        }
        public Integer dequeue(){
            for(;;){ANode h=head.get(),t=tail.get(),s=h.next.get();if(h==head.get()){
                if(h==t){if(s==null)return null;tail.compareAndSet(t,s);}
                else if(head.compareAndSet(h,s))return s.value;
            }}
        }
        public Integer peek(){
            for(;;){ANode h=head.get(),s=h.next.get();if(h==head.get())return s==null?null:s.value;}
        }
        public boolean valid(){
            ANode h=head.get(),slow=h,fast=h;
            while(fast!=null&&fast.next.get()!=null){slow=slow.next.get();fast=fast.next.get().next.get();if(slow==fast)return false;}
            ANode t=tail.get();while(t.next.get()!=null){ANode n=t.next.get();tail.compareAndSet(t,n);t=n;}
            return head.get()!=null&&tail.get()!=null;
        }
    }
    static final class LockQueueTarget implements Target {
        private final LockQueue q=new LockQueue();
        public void prefill(int n){for(int i=1;i<=n;i++)q.enqueue(i);}
        public void operation(byte op,int key){if(op==READ)q.peek();else if(op==INSERT)q.enqueue(key);else q.dequeue();}
        public boolean valid(){return q.valid();}
    }
    static final class MsQueueTarget implements Target {
        private final MsQueue q=new MsQueue();
        public void prefill(int n){for(int i=1;i<=n;i++)q.enqueue(i);}
        public void operation(byte op,int key){if(op==READ)q.peek();else if(op==INSERT)q.enqueue(key);else q.dequeue();}
        public boolean valid(){return q.valid();}
    }
}
