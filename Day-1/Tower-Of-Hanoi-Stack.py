import time

class Stack:
    def __init__(self):
        self.items = []

    def push(self, item):
        self.items.append(item)

    def pop(self):
        if not self.is_empty():
            return self.items.pop()
        else:
            raise IndexError("Pop from empty stack")

    def peek(self):
        if not self.is_empty():
            return self.items[-1]
        else:
            raise IndexError("Peek from empty stack")

    def is_empty(self):
        return len(self.items) == 0

    def __len__(self):
        return len(self.items)

    def __str__(self):
        return ' '.join(str(item) for item in reversed(self.items))


class TowerOfHanoi:
    def __init__(self, num_disks):
        self.num_disks = num_disks
        self.source = Stack()
        self.auxiliary = Stack()
        self.destination = Stack()
        self._setup()

    def _setup(self):
        for disk in range(self.num_disks, 0, -1):
            self.source.push(f'disk {disk}')

    def _move_disks(self, n, source, destination, auxiliary):
        if n == 1:
            disk = source.pop()
            destination.push(disk)
            self._display()
            time.sleep(1)  # Pause for 1 seconds for better visibility
        else:
            self._move_disks(n - 1, source, auxiliary, destination)
            disk = source.pop()
            destination.push(disk)
            self._display()
            time.sleep(2)  # Pause for 2 seconds for better visibility
            self._move_disks(n - 1, auxiliary, destination, source)

    def solve(self):
        print("Starting the solution...")
        self._move_disks(self.num_disks, self.source, self.destination, self.auxiliary)

    def _display(self):
        print("Source:", self.source)
        print("Auxiliary:", self.auxiliary)
        print("Destination:", self.destination)
        print("-" * 30)


if __name__ == '__main__':
    num_disks = int(input("Enter the number of disks: "))
    tower = TowerOfHanoi(num_disks)
    print("Initial State:")
    tower._display()
    tower.solve()
    print("Final State:")
    tower._display()
