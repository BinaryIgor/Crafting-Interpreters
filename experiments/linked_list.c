#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct Node
{
    char *data;
    struct Node *next;
} Node;

int main()
{
    Node *first = (Node *)malloc(sizeof(Node));

    first->data = "data-1";

    Node *second = (Node *)malloc(sizeof(Node));

    second->data = "data-2";

    Node *third = (Node *)malloc(sizeof(Node));

    third->data = "data-3";

    first->next = second;
    second->next = third;
    third->next = NULL;

    printf("Linked List: \n");
    Node *temp = first;
    while (temp)
    {
        printf("%s, %lu\n", temp->data, strlen(temp->data));
        temp = temp->next;
    }

    free(first);
    free(second);
    free(third);
    return 0;
}