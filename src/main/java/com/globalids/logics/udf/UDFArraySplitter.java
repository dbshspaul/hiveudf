package com.globalids.logics.udf;

import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentLengthException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDTF;
import org.apache.hadoop.hive.serde2.lazy.LazyArray;
import org.apache.hadoop.hive.serde2.lazy.LazyString;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by debasish paul on 04-09-2018.
 */
public class UDFArraySplitter extends GenericUDTF {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");

    public StructObjectInspector initialize(ObjectInspector[] args) throws UDFArgumentException {
        List<String> outFieldNames = new ArrayList<String>();
        List<ObjectInspector> outFieldOIs = new ArrayList<ObjectInspector>();

        if (args.length < 2) {
            throw new UDFArgumentException("Provide at least 2 args, 1st should be primitive type and other should be array type.");
        }
        for (int i = 0; i < args.length; i++) {
            if (i > 1) {
                if (args[i].getCategory() != ObjectInspector.Category.LIST) {
                    throw new UDFArgumentTypeException(i, "All arguments except 1st must be an array type.");
                }
                outFieldNames.add("col" + i);
            } else if (i == 0) {
                if (args[i].getCategory() != ObjectInspector.Category.PRIMITIVE) {
                    throw new UDFArgumentTypeException(i, "1st argument should be primitive type. Provided " + args[i].getCategory());
                }
                outFieldNames.add("id");
            } else {
                outFieldNames.add("col" + i);
            }
            outFieldOIs.add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);
        }
        outFieldNames.add("timestamp");
        outFieldOIs.add(PrimitiveObjectInspectorFactory.javaTimestampObjectInspector);
        if (outFieldOIs.size() != outFieldNames.size()) {
            throw new UDFArgumentLengthException("Selected number of column and header must be same.");
        }
        return ObjectInspectorFactory.getStandardStructObjectInspector(outFieldNames, outFieldOIs);
    }

    public void process(Object[] objects) throws HiveException {
        List<List<Object>> objects1 = processInputRecord(objects);
        for (Object objects2 : objects1) {
            forward(objects2);
        }
    }


    public void close() throws HiveException {

    }


    public List<List<Object>> processInputRecord(Object[] objects) {

        List<List<Object>> result = new ArrayList<List<Object>>();
        int startValue = 1;
        if (objects[1] instanceof org.apache.hadoop.io.Text || objects[1] instanceof LazyString) {
            startValue = 2;
        }
        List<String>[] columnWiseDataList = new List[objects.length - startValue];
        Collection<List<String>> shuffleData = new ArrayList<List<String>>();
        try {
            for (int i = startValue; i < objects.length; i++) {
                List list = ((LazyArray) objects[i]).getList();
                List<String> stringList = new ArrayList<String>();
                for (Object o : list) {
                    stringList.add(o.toString());
                }
                columnWiseDataList[i - startValue] = stringList.isEmpty() ? Arrays.asList("") : stringList;
//                columnWiseDataList[i] = (List<String>) list.stream().map(item -> item.toString()).collect(Collectors.toList());
            }
//            shuffleData = shuffleData(columnWiseDataList);
            if ((objects[0] != null) && objects[0].toString().length() > 0) {
                shuffleData = shuffleData(columnWiseDataList);
            } else {
                System.out.println("Skipping shuffling for null value");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*===================after shuffleData================================*/
        for (List<String> object : shuffleData) {
            List<Object> row = new ArrayList<Object>();
            row.add(objects[0].toString());
            if (startValue == 2) {
                row.add(objects[1].toString());
            }
            for (int i = 0; i < object.size(); i++) {
                row.add(object.get(i));
            }
            row.add(new Timestamp(System.currentTimeMillis()));
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
            if (stringList != null && !stringList.isEmpty()) {
                collections.add(stringList);
            } else {
                collections.add(Arrays.asList(""));
            }
        }
        return util.permutations(collections);
    }
}