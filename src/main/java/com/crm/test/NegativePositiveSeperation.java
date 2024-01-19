package com.crm.test;

public class NegativePositiveSeperation {
    public static void main(String[] args) {
        test();
    }

    static void test() {

                int[] array = {-3, -5, 2, 3, 1, -6, 7, -5};

                rearrangeArray(array);

                // Print the rearranged array
                for (int num : array) {
                    System.out.print(num + " ");
                }
            }

    public static void rearrangeArray(int[] array) {
        int negativeIndex = 0;

        for (int i = 0; i < array.length; i++) {
            if (array[i] < 0) {
                // Swap the current element with the first negative element
                int temp = array[i];
                array[i] = array[negativeIndex];
                array[negativeIndex] = temp;
                negativeIndex++;
            }
        }
    }

}
