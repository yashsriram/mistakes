import Abstract.LeftOvers.LeftOvers;
import Abstract.SInequality;
import Jama.Matrix;
import PartEstimates.PartEstimate;
import PartEstimates.PartEstimatesTable;
import Physical.Shipment;
import com.opencsv.CSVReader;

import java.io.*;
import java.util.*;

/**
 * Main program
 * todo : handle typos
 */
public class Main {


    private static Map<String, Shipment> readNewShipments() throws IOException {
        final String boxes_path = "../db/raw/new/boxes.csv";
        final String part_clone_count_maps_path = "../db/raw/new/part_clone_count_maps.csv";

        Map<String, Shipment> shipments = new HashMap<>();

        CSVReader boxes_reader = new CSVReader(new FileReader(boxes_path));
        // Ignore CSV heading
        String[] box_fields;
        // Reads boxes
        while ((box_fields = boxes_reader.readNext()) != null) {

            // Format
            // 1 -> order id
            // 2 -> shipment id
            // 10 -> l
            // 11 -> b
            // 12 -> h
            // 13 -> w

            // Assert
            // l, b, h > 0
            // w > 0
            shipments.put(
                    box_fields[2],
                    new Shipment(
                            box_fields[1],
                            box_fields[2],
                            Double.parseDouble(box_fields[10]),
                            Double.parseDouble(box_fields[11]),
                            Double.parseDouble(box_fields[12]),
                            Double.parseDouble((box_fields[13]))
                    ));
        }
        //  System.out.println("Read boxes finished");

        // Clear the file
        PrintWriter boxes_writer = new PrintWriter(boxes_path);
        boxes_writer.write("");
        boxes_writer.close();

        CSVReader pc_map_reader = new CSVReader(new FileReader(part_clone_count_maps_path));
        // Ignore CSV heading
        String[] pc_map_fields;
        // Reads part clone_count maps
        while ((pc_map_fields = pc_map_reader.readNext()) != null) {

            // Format
            // 0 -> shipment id
            // 1 -> id
            // 6 -> quantity
            String shipment_id = pc_map_fields[0];
            String sku = pc_map_fields[1];
            int quantity = Integer.parseInt(pc_map_fields[6]);

            // Assert
            // quantity > 0
            Shipment shipment = shipments.get(shipment_id);
            if (shipment == null) {
                //  System.out.println("Warning : Unknown box found " + shipment_id);
            } else {
                shipment.addPart(sku, quantity);
            }
        }
        //  System.out.println("Read part clone count maps finished");

        // Clear the file
        PrintWriter pc_map_writer = new PrintWriter(part_clone_count_maps_path);
        pc_map_writer.write("");
        pc_map_writer.close();

        // Validate Bad Cases ///////////////////////////////////////////////////////////////////////////////////

        Vector<String> empty_boxes = new Vector<>();

        // Clean data
        for (Map.Entry<String, Shipment> shipment : shipments.entrySet()) {

            // Keep track of Empty Shipments
            if (shipment.getValue().part_clone_count_map.size() == 0) {
                empty_boxes.add(shipment.getKey());
                //  System.out.println("Warning : Empty box found " + shipment.getKey());
            }
        }

        // Removing Empty box
        for (String empty_box : empty_boxes) {
            shipments.remove(empty_box);
            //  System.out.println("Note : Empty box ignored " + empty_box);
        }

        return shipments;
    }

    private static Vector<SInequality> getAllSInequalities() throws IOException {
        Vector<SInequality> all = new Vector<>();

        Vector<SInequality> old = LeftOvers.getAll();
        all.addAll(old);

        Map<String, Shipment> shipments = Main.readNewShipments();
        for (Map.Entry<String, Shipment> id_shipment_map : shipments.entrySet()) {
            SInequality sInequality = id_shipment_map.getValue().extractSInequality();
            all.add(sInequality);
        }

        return all;
    }

    private static Map<Integer, Vector<SInequality>> extractAllFullSets() throws IOException {
        Map<Integer, Vector<SInequality>> full_sets = new HashMap<>();

        Vector<SInequality> all = Main.getAllSInequalities();
        for (SInequality sInequality : all) {

            int cardinality = sInequality.getCardinality();

            if (full_sets.containsKey(cardinality)) {
                full_sets.get(cardinality).add(sInequality);
            }
            else {
                Vector<SInequality> full_set = new Vector<>();
                full_set.add(sInequality);
                full_sets.put(cardinality, full_set);
            }

        }

        return full_sets;
    }

    private static Map<Set<String>, Vector<SInequality>> extractAllSimilarSets() throws IOException {
        Map<Set<String>, Vector<SInequality>> similar_sets = new HashMap<>();

        Map<Integer, Vector<SInequality>> full_sets = Main.extractAllFullSets();
        for (Map.Entry<Integer, Vector<SInequality>> full_set : full_sets.entrySet()) {
            for (SInequality sInequality : full_set.getValue()) {

                Set<String> signature = sInequality.getSignature();

                if (similar_sets.containsKey(signature)) {
                    similar_sets.get(signature).add(sInequality);
                }
                else {
                    Vector<SInequality> similar_set = new Vector<>();
                    similar_set.add(sInequality);
                    similar_sets.put(signature, similar_set);
                }

            }
        }

        Vector<Set<String>> unsolvable_similar_sets = new Vector<>();

        for (Map.Entry<Set<String>, Vector<SInequality>> similar_set : similar_sets.entrySet()) {

            if (similar_set.getKey().size() > similar_set.getValue().size()) {
                unsolvable_similar_sets.add(similar_set.getKey());
                //  System.out.println("Note : Unsolvable similar set found " + similar_set.getKey());
            }
        }

        for (Set<String> unsolvable_similar_set : unsolvable_similar_sets) {

            LeftOvers.add(similar_sets.get(unsolvable_similar_set));

            similar_sets.remove(unsolvable_similar_set);
            //  System.out.println("Note : Unsolvable similar set removed " + unsolvable_similar_set);
        }

        return similar_sets;
    }

    private static int no_squares_formed_in_this_similar = 0;

    private static int squares_per_similar_limit = 100;

    private static void extractSquaresFromSimilar(Vector<SInequality> similar_set,
                                                  int cardinality, Vector<SInequality> buffer,
                                                  int buffer_i,
                                                  int input_i,
                                                  Vector<Vector<SInequality>> square_sets) {
        if (Main.no_squares_formed_in_this_similar >= squares_per_similar_limit) {
            return;
        }

        if (buffer_i == cardinality) {
            Vector<SInequality> square = new Vector<>(buffer);
            square_sets.add(square);
            Main.no_squares_formed_in_this_similar++;
            return;
        }

        if (input_i >= similar_set.size())
            return;

        buffer.set(buffer_i, similar_set.get(input_i));

        Main.extractSquaresFromSimilar(similar_set, cardinality, buffer, buffer_i + 1, input_i + 1, square_sets);

        Main.extractSquaresFromSimilar(similar_set, cardinality, buffer, buffer_i, input_i + 1, square_sets);

    }

    private static Map<Set<String>, Vector<Vector<SInequality>>> extractAllSquareSets() throws IOException {
        Map<Set<String>, Vector<Vector<SInequality>>> square_sets = new HashMap<>();

        Map<Set<String>, Vector<SInequality>> similar_sets = Main.extractAllSimilarSets();

        for (Map.Entry<Set<String>, Vector<SInequality>> similar_set_m : similar_sets.entrySet()) {

            Vector<SInequality> similar_set = similar_set_m.getValue();
            int cardinality = similar_set_m.getKey().size();

            Vector<SInequality> buffer = new Vector<>();
            for (int i = 0; i < cardinality; i++) {
                buffer.add(null);
            }
            Vector<Vector<SInequality>> square_sets_from_this = new Vector<>();

            Main.no_squares_formed_in_this_similar = 0;
            Main.extractSquaresFromSimilar(similar_set, cardinality, buffer, 0, 0, square_sets_from_this);

            square_sets.put(similar_set_m.getKey(), square_sets_from_this);

        }

        return square_sets;
    }

    private static void pushNewEstimates(Vector<String> ids, double[][] new_estimates) {
        for (int i = 0; i < ids.size(); i++) {
            PartEstimate ith = new PartEstimate(ids.get(i),
                    new_estimates[i][0],
                    new_estimates[i][1],
                    new_estimates[i][2],
                    new_estimates[i][3]
            );
            PartEstimatesTable.pushEstimate(ith);
        }
    }

    private static void estimateFromSquareSet(Vector<SInequality> square_set) {

        double[][] a = new double[square_set.size()][square_set.size()];
        double[][] b = new double[square_set.size()][4];

        for (int i = 0; i < square_set.size(); i++) {
            SInequality sInequality = square_set.get(i);
            a[i] = sInequality.getCoefficientRow();
            b[i] = sInequality.getConstantRow();
        }

        Matrix A = new Matrix(a);
        Matrix B = new Matrix(b);

        if (A.det() != 0) {
            Matrix X = A.solve(B);
            double[][] estimates = X.getArray();

            boolean has_non_positive_dimension = false;

            for (double[] estimate : estimates) {
                for (double dimension : estimate) {
                    if (dimension <= 0) {
                        has_non_positive_dimension = true;
                        break;
                    }
                }
                if (has_non_positive_dimension) break;
            }

            if (has_non_positive_dimension) {
                // todo : specially solve these
            }
            else {
                Main.pushNewEstimates(square_set.get(0).getVariableRow(), estimates);
            }

        }
        else {
            //  System.out.println("Note : Singular matrix found");
        }

    }

    public static void main(String[] args) throws IOException {

        Map<Set<String>, Vector<Vector<SInequality>>> square_sets = Main.extractAllSquareSets();

        for (Map.Entry<Set<String>, Vector<Vector<SInequality>>> square_sets_from_this : square_sets.entrySet()) {
            for (Vector<SInequality> square_set : square_sets_from_this.getValue()) {
                Main.estimateFromSquareSet(square_set);
            }
        }

        LeftOvers.saveAllLeftOvers();
        PartEstimatesTable.saveAllEstimates();
    }

}