package com.crm.test;

import java.util.Arrays;
import java.util.Scanner;

public class RevArrayWithZeroFixed {

	public static void main(String[] args) {
			
		demo();
	}
	
	static void demo() {
		Scanner sc = new Scanner(System.in);
		int size = sc.nextInt();
		
		int[] arr = new int[size];
		for(int i=0; i<size; i++) {
			arr[i] = sc.nextInt();
		}
		
		int left = 0;
		int right = size-1;
		
		
		while(left < right) {
			if(arr[left] == 0) 
				left++;
			else if(arr[right] == 0)
				right--;
			else {
				// swap
				int temp = arr[left];
				arr[left] = arr[right];
				arr[right] = temp;
			}
			
			left++;
			right--;
		}
		
		System.out.println(Arrays.toString(arr));
	}
}
