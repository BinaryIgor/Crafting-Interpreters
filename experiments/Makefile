# Variables
CC = gcc
CFLAGS = -Wall -g

# Target to build the final executable
linked_list: linked_list.o prepare_out
	$(CC) $(CFLAGS) -o out/$@ $<

# Target to compile main.o
linked_list.o: linked_list.c
	$(CC) $(CFLAGS) -c $<

# Clean up generated files
.PHONY: clean
clean:
	rm -f *.o linked_list

# prepare out dir
prepare_out:
	mkdir -p out

print: linked_list.c linked_list
	echo $@