package com.crm.test;

import java.util.Scanner;

class Node {

    int data;
    Node left;
    Node right;

   protected Node(int data) {
        this.data = data;
        this.left = null;
        this.right = null;
    }
}

public class BinaryTree {
    public static void main(String[] args) {
        Node root;
        System.out.println("Enter value for root");
        Scanner sc = new Scanner(System.in);
        int value = sc.nextInt();
        root = new Node(value);

        insert(root, sc);
    }

    static Node insert(Node node, Scanner sc) {
        if(node.data == -1)
            return null;

        // left
        System.out.println("Enter value for left of "+node.data);
        int value = sc.nextInt();
        node.left = new Node(value);
        insert(node.left, sc);

        // right
        System.out.println("Enter value for right of "+node.data);
        value = sc.nextInt();
        node.right = new Node(value);
        insert(node.right, sc);

        return node;
    }
}
