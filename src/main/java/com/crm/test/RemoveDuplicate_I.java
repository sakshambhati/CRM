package com.crm.test;

import java.util.Arrays;

public class RemoveDuplicate_I {

    public static void main(String[] args) {
        demo();
    }

    static void demo() {
        int[] nums = {1,1,1,2,3,3,3,4};
        int left=0, right =left+1;

        while (right < nums.length) {
            if(nums[left] != nums[right]) {
                left++;
                nums[left] = nums[right];
            }
            right++;
        }
        System.out.println(Arrays.toString(nums));
        System.out.println(left+1);
    }
}
