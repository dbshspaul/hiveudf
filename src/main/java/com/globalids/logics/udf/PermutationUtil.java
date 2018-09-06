package com.globalids.logics.udf;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by debasish paul on 05-09-2018.
 */
public class PermutationUtil {

    public static <T> Collection<List<T>> permutations(List<Collection<T>> collections) {
        if (collections == null || collections.isEmpty()) {
            return Collections.emptyList();
        } else {
            Collection<List<T>> res = new ArrayList<List<T>>();
            permutationsImpl(collections, res, 0, new ArrayList<T>());
            return res;
        }
    }

    private static <T> void permutationsImpl(List<Collection<T>> ori, Collection<List<T>> res, int d, ArrayList<T> current) {
        if (d == ori.size()) {
            res.add((ArrayList<T>) current.clone());
            return;
        }
        Collection<T> currentCollection = ori.get(d);
        if (currentCollection != null) {
            for (T element : currentCollection) {
                ArrayList<T> copy = (ArrayList<T>) current.clone();
                copy.add(element);
                permutationsImpl(ori, res, d + 1, copy);
            }
        }
    }

    public static void main(String[] args) {
        PermutationUtil util = new PermutationUtil();

//        Collection<List<String>> permutations = util.permutations(Arrays.asList(Arrays.asList("Guest Services Associate", "VIC/DSG-01"),
//                Arrays.asList("Texas", "TX"),
//                Arrays.asList("Hello", "World"),
//                Arrays.asList("")));

        List<String> data = new ArrayList<String>();
        data.add("Guest Services Associate");
        data.add("VIC/DSG-01");
        List<Collection<String>> collections = new ArrayList<Collection<String>>();
        collections.add(data);
        Collection<List<String>> permutations = util.permutations(collections);


        for (List<String> permutation : permutations) {
            System.out.println(permutation);
        }
    }
}
