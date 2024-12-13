#include <stdio.h>
#include <stdlib.h>

typedef struct Node
{
    int data;
    struct Node *next;
} Node;

int main()
{
    Node *first = (Node *)malloc(sizeof(Node));

    first->data = 10;

    Node *second = (Node *)malloc(sizeof(Node));

    second->data = 20;

    Node *third = (Node *)malloc(sizeof(Node));

    third->data = 30;

    first->next = second;
    second->next = third;
    third->next = NULL;

    printf("Linked List: \n");
    Node *temp = first;
    while (temp)
    {
        printf("%d\n", temp->data);
        temp = temp->next;
    }

    free(first);
    free(second);
    free(third);
    return 0;
}