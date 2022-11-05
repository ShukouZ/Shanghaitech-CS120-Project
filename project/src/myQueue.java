import java.util.Arrays;

public class myQueue {
    private int size;
    private float queue[];
    private int head;
    private int tail;

    myQueue(int _size){
        size = _size;
        queue = new float[size];
        Arrays.fill(queue, 0);
        head = 0;
        tail = size-1;
    }

    public void add(float element){
        queue[head] = element;
        tail = head;
        head += 1;
        if(head == size){
            head = 0;
        }
    }

    public void clear(){
        Arrays.fill(queue, 0);
        head = 0;
        tail = size-1;
    }

    public float dot_product(float[] preamble){
        int idx;
        float sum = 0.0f;
        for(int i=0; i<size; i++){
            idx = (i+head) % size;
            sum += queue[idx] * preamble[i];
        }
        return sum;
    }

}
