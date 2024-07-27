import time

def create_queue(size):
    return {
        'size': size,
        'front': -1,
        'rear': -1,
        'queue': [None] * size
    }

def is_full(queue):
    if (queue['rear'] + 1) % queue['size'] == queue['front']:
        print("Queue is full")
        return True
    else:
        print("Queue is not full")
        return False

def is_empty(queue):
    if queue['front'] == -1:
        print("Queue is empty")
        return True
    else:
        print("Queue is not empty")
        return False

def enqueue(queue, key):
    if is_full(queue):
        print("Queue overflow")
    else:
        if queue['front'] == -1:
            queue['front'] = 0
        queue['rear'] = (queue['rear'] + 1) % queue['size']
        queue['queue'][queue['rear']] = key
        animate_enqueue(queue, key)

def dequeue(queue):
    if is_empty(queue):
        print("Queue underflow")
        return None
    else:
        key = queue['queue'][queue['front']]
        animate_dequeue(queue, key)
        if queue['front'] == queue['rear']:
            queue['front'] = queue['rear'] = -1
        else:
            queue['front'] = (queue['front'] + 1) % queue['size']
        return key

def animate_enqueue(queue, key):
    print("\nEnqueuing element:", key)
    for i in range(queue['size']):
        if queue['queue'][i] is not None:
            print(f"[ {queue['queue'][i]} ]", end=" ")
        else:
            print("[   ]", end=" ")
        time.sleep(0.2)
    print("\nElement enqueued successfully\n")

def animate_dequeue(queue, key):
    print("\nDequeuing element:", key)
    for i in range(queue['size']):
        if i == queue['front']:
            print("[   ]", end=" ")
        else:
            print(f"[ {queue['queue'][i]} ]", end=" ")
        time.sleep(0.2)
    print(f"\nElement dequeued successfully\n")

def main():
    size = 10
    queue = create_queue(size)
    print("---------------------------QUEUE PROGRAM-----------------------------")
    print("\n||||||||||| You can enter only 10 elements in queue |||||||||||")
    print("\n********************************")
    print("MENU DRIVEN")
    print("********************************")
    print("1. CHECK QUEUE FULL OR NOT")
    print("2. CHECK QUEUE EMPTY OR NOT")
    print("3. ENQUEUE ELEMENT IN QUEUE")
    print("4. DEQUEUE ELEMENT FROM QUEUE")
    print("-------------Program ends when user enters -1--------------")

    choice = 0  # Initialize choice before entering the loop
    while choice != -1:
        print("----------------------------------------------")
        choice = int(input("Enter your choice: "))
        if choice == -1:
            break
        elif choice == 1:
            is_full(queue)
        elif choice == 2:
            is_empty(queue)
        elif choice == 3:
            key = int(input("Enter the value you want to enqueue into the queue: "))
            enqueue(queue, key)
        elif choice == 4:
            dequeued_element = dequeue(queue)
            if dequeued_element is not None:
                print(f"Element dequeued successfully and the dequeued element is {dequeued_element}")
        else:
            print("Enter a valid choice")

if __name__ == "__main__":
    main()
