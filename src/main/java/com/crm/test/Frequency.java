package com.crm.test;

import java.util.HashMap;
import java.util.Scanner;

public class Frequency {

	public static void main(String[] args) {
	
		demo();
	}
	
	static void demo() {
		
		// first
		Scanner sc = new Scanner(System.in);
		int size1 = sc.nextInt();
		
		int[] arr1 = new int[size1];
		for(int i=0; i<size1; i++) {
			arr1[i] = sc.nextInt();
		}
		
		// second
		Scanner sc2 = new Scanner(System.in);
		int size2 = sc.nextInt();
		
		int[] arr2 = new int[size2];
		for(int i=0; i<size2; i++) {
			arr2[i] = sc.nextInt();
		}
		
		HashMap<Integer, Integer> hash = new HashMap<>();
		
		// first
		for(int num: arr1) {
			if(hash.containsKey(num)) {
				hash.put(num, hash.get(num)+1);
			} else {
				hash.put(num, 1);
			}
		}
		
		int maxValue = 0;
		int maxOccurence = -1;
		for(int num: arr2) {
			if(hash.containsKey(num) && hash.get(num) > maxValue) {
				maxValue = hash.get(num);
				maxOccurence = num;
			}
		}
		
		System.out.println(maxOccurence);
	}
}
