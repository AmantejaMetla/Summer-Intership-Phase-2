import time

def create_stack(size):
    return {
        'size': size,
        'top': -1,
        'stack': [None] * size
    }

def is_full(stack):
    if stack['top'] == stack['size'] - 1:
        print("Stack is full")
        return True
    else:
        print("Stack is not full")
        return False

def is_empty(stack):
    if stack['top'] == -1:
        print("Stack is empty")
        return True
    else:
        print("Stack is not empty")
        return False

def push(stack, key):
    if is_full(stack):
        print("Stack overflow")
    else:
        stack['top'] += 1
        stack['stack'][stack['top']] = key
        animate_push(stack, key)

def pop(stack):
    if is_empty(stack):
        print("Stack underflow")
        return None
    else:
        key = stack['stack'][stack['top']]
        animate_pop(stack, key)
        stack['top'] -= 1
        return key

def animate_push(stack, key):
    print("\nPushing element:", key)
    for i in range(stack['top'] + 1):
        if stack['stack'][i] is not None:
            print(f"[ {stack['stack'][i]} ]", end=" ")
        else:
            print("[   ]", end=" ")
        time.sleep(0.2)
    print("\nElement pushed into stack successfully\n")

def animate_pop(stack, key):
    print("\nPopping element:", key)
    for i in range(stack['top'] + 1):
        if i == stack['top']:
            print("[   ]", end=" ")
        else:
            print(f"[ {stack['stack'][i]} ]", end=" ")
        time.sleep(0.2)
    print(f"\nElement popped from stack successfully\n")

def main():
    size = 10
    stack = create_stack(size)
    print("---------------------------STACK PROGRAM-----------------------------")
    print("\n||||||||||| You can enter only 10 elements in stack |||||||||||")
    print("\n********************************")
    print("MENU DRIVEN")
    print("********************************")
    print("1. CHECK STACK FULL OR NOT")
    print("2. CHECK STACK EMPTY OR NOT")
    print("3. PUSH ELEMENT IN STACK")
    print("4. POP ELEMENT FROM STACK")
    print("-------------Program ends when user enters -1--------------")

    choice = 0  # Initialize choice before entering the loop
    while choice != -1:
        print("----------------------------------------------")
        choice = int(input("Enter your choice: "))
        if choice == -1:
            break
        elif choice == 1:
            is_full(stack)
        elif choice == 2:
            is_empty(stack)
        elif choice == 3:
            key = int(input("Enter the value you want to push into the stack: "))
            push(stack, key)
        elif choice == 4:
            popped_element = pop(stack)
            if popped_element is not None:
                print(f"Element popped successfully and the popped element is {popped_element}")
        else:
            print("Enter a valid choice")

if __name__ == "__main__":
    main()
