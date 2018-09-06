package com.globalids.logics.udf;

import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDTF;
import org.apache.hadoop.hive.serde2.lazy.LazyArray;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by debasish paul on 04-09-2018.
 */
public class UDFArrayFirst extends GenericUDTF {

    public StructObjectInspector initialize(ObjectInspector[] args) throws UDFArgumentException {
        List<String> outFieldNames = new ArrayList<String>();
        List<ObjectInspector> outFieldOIs = new ArrayList<ObjectInspector>();

        if (args.length < 2) {
            throw new UDFArgumentException("Provide at least 2 args, 1st should be primitive type and other should be primitive type.");
        }
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                if (args[i].getCategory() != ObjectInspector.Category.LIST) {
                    throw new UDFArgumentException("All arguments except 1st must be an array type.");
                }
                outFieldNames.add("col" + i);
            } else {
                if (args[i].getCategory() != ObjectInspector.Category.PRIMITIVE) {
                    throw new UDFArgumentException("1st argument should be primitive type.");
                }
                outFieldNames.add("id");
            }
            outFieldOIs.add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);
        }
        return ObjectInspectorFactory.getStandardStructObjectInspector(outFieldNames, outFieldOIs);
    }

    public void process(Object[] objects) throws HiveException {
        List<List<String>> objects1 = processInputRecord(objects);
        for (Object objects2 : objects1) {
            forward(objects2);
        }
    }


    public void close() throws HiveException {

    }


    public List<List<String>> processInputRecord(Object[] objects) {

        List<List<String>> result = new ArrayList<List<String>>();
        List<String>[] columnWiseDataList = new List[objects.length-1];
        Collection<List<String>> shuffleData = new ArrayList<List<String>>();
        try {
            for (int i = 1; i < objects.length; i++) {
                List list = ((LazyArray) objects[i]).getList();
                List<String> stringList = new ArrayList<String>();
                for (Object o : list) {
                    stringList.add(o.toString());
                }
                columnWiseDataList[i-1] = stringList;
//                columnWiseDataList[i] = (List<String>) list.stream().map(item -> item.toString()).collect(Collectors.toList());
            }
            shuffleData = shuffleData(columnWiseDataList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*===================after shuffleData================================*/
        for (List<String> object : shuffleData) {
            List<String> row = new ArrayList<String>();
            row.add(objects[0].toString());
            for (int i = 0; i < object.size(); i++) {
                row.add(object.get(i));
            }
            result.add(row);
        }
        return result;
    }

    /**
     * this code is for java 8 streaming does not work with java 7
     * @param list
     * @return
     * @throws IOException
     */
//    private static Collection<List<String>> shuffleData(List<String>... list) throws IOException {
//        Stream<Collection<String>> inputs = Stream.of(list);
//        Stream<Collection<List<String>>> listified = inputs.filter(Objects::nonNull)
//                .filter(input -> !input.isEmpty())
//                .map(l -> l.stream()
//                        .map(o -> new ArrayList<>(Arrays.asList(o)))
//                        .collect(Collectors.toList()));
//
//        Collection<List<String>> combinations = listified.reduce((input1, input2) -> {
//            Collection<List<String>> merged = new ArrayList<>();
//            input1.forEach(permutation1 -> input2.forEach(permutation2 -> {
//                List<String> combination = new ArrayList<>();
//                combination.addAll(permutation1);
//                combination.addAll(permutation2);
//                merged.add(combination);
//            }));
//            return merged;
//        }).orElse(new HashSet<>());
//        return combinations;
//    }


    /**
     * java 7 compatible
     *
     * @param list
     * @return
     */
    private static Collection<List<String>> shuffleData(List<String>... list) {
        PermutationUtil util = new PermutationUtil();
        List<Collection<String>> collections = new ArrayList<Collection<String>>();
        for (List<String> stringList : list) {
            if (stringList!=null&&!stringList.isEmpty()) {
                collections.add(stringList);
            } else {
                collections.add(Arrays.asList(""));
            }
        }
        return util.permutations(collections);
    }
}